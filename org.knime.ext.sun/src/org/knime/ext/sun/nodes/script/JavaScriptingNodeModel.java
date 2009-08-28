/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 *
 */
package org.knime.ext.sun.nodes.script;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.TimestampCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.ext.sun.nodes.script.expression.Expression;

/**
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class JavaScriptingNodeModel extends NodeModel {

    private final JavaScriptingSettings m_settings;

    /** The compiled version is stored because it is expensive to create it. Do
     * not rely on its existence! */
    private Expression m_compiledExpression = null;

   /** The input table spec at the time the above expression was compiled. */
    private DataTableSpec m_inputSpec = null;
    
    /** The current row count or -1 if not in execute(). */
    private int m_rowCount = -1;

    /** One input, one output. */
    public JavaScriptingNodeModel() {
        super(1, 1);
        m_settings = new JavaScriptingSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        new JavaScriptingSettings().loadSettingsInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // after we got a new expression delete the compiled version of it.
        m_compiledExpression = null;
        m_inputSpec = null;
        m_settings.loadSettingsInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec inSpec = inData[0].getDataTableSpec();
        ColumnRearranger c = createColumnRearranger(inSpec);
        m_rowCount = inData[0].getRowCount();
        try {
            BufferedDataTable o = exec.createColumnRearrangeTable(
                    inData[0], c, exec);
            return new BufferedDataTable[]{o};
        } finally {
            m_rowCount = -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_rowCount = -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        ColumnRearranger c = createColumnRearranger(inSpecs[0]);
        return new DataTableSpec[]{c.createSpec()};
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec spec) 
        throws InvalidSettingsException {
        if (m_settings.getExpression() == null) {
            throw new InvalidSettingsException("No expression has been set.");
        }
        boolean isReplace = m_settings.isReplace();
        String colName = m_settings.getColName();
        try {
            if ((m_compiledExpression == null)
                    || (!m_inputSpec.equalStructure(spec))) {
                // if the spec changes, we need to re-compile the expression
                m_compiledExpression = Expression.compile(m_settings, spec);
                m_inputSpec = spec;
            }
            assert m_inputSpec != null;
            ColumnCalculator cc = new ColumnCalculator(this, getNewColSpec());
            ColumnRearranger result = new ColumnRearranger(spec);
            if (isReplace) {
                result.replace(cc, colName);
            } else {
                if (spec.containsName(colName)) {
                    throw new InvalidSettingsException(
                            "Can't create new column \"" + colName 
                            + "\" as input spec already contains such column");
                }
                result.append(cc);
            }
            return result;
        } catch (Exception e) {
            throw new InvalidSettingsException(e.getMessage(), e);
        }
    }
    
    /** Reads a variable from this node model. Calls for instance
     * {@link #peekFlowVariableDouble(String)}.
     * @param name The name of variable.
     * @param type Type of variable.
     * @return The value
     */
    Object readVariable(final String name, final Class<?> type) {
        if (Integer.class.equals(type)) {
            return peekFlowVariableInt(name);
        } else if (Double.class.equals(type)) {
            return peekFlowVariableDouble(name);
        } else if (String.class.equals(type)) {
            return peekFlowVariableString(name);
        } else {
            throw new RuntimeException("Invalid variable class: " + type);
        }
    }
    
    /**
     * @return the returnType
     */
    Class<?> getReturnType() {
        return m_settings.getReturnType();
    }
    
    /**
     * @return true if the return value of the expression represents an array.
     */
    boolean isArrayReturn() {
        return m_settings.isArrayReturn();
    }

    /**
     * @return the compiledExpression
     */
    Expression getCompiledExpression() {
        return m_compiledExpression;
    }
    
    /**
     * @return the inputSpec
     */
    DataTableSpec getInputSpec() {
        return m_inputSpec;
    }
    
    /** @return the row count of the BDT being processed, otherwise -1. */
    int getRowCount() {
        return m_rowCount;
    }
    
    private DataColumnSpec getNewColSpec() throws InvalidSettingsException {
        Class<?> returnType = m_settings.getReturnType();
        String colName = m_settings.getColName();
        DataType cellReturnType;
        if (returnType.equals(Integer.class)) {
            cellReturnType = IntCell.TYPE;
        } else if (returnType.equals(Double.class)) {
            cellReturnType = DoubleCell.TYPE;
        } else if (returnType.equals(Date.class)) {
            cellReturnType = TimestampCell.TYPE;
        } else if (returnType.equals(String.class)) {
            cellReturnType = StringCell.TYPE;
        } else {
            throw new InvalidSettingsException("Illegal return type: "
                    + returnType.getName());
        }
        DataType type = !m_settings.isArrayReturn() ? cellReturnType 
                : DataType.getType(ListCell.class, cellReturnType);
        return new DataColumnSpecCreator(colName, type).createSpec();
    }

}
