/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Apr 28, 2008 (wiswedel): created
 */
package org.knime.base.node.meta.looper.variable;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.StringValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.LoopStartNode;

/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class IterateVariablesLoopHeadNodeModel extends NodeModel implements
        LoopStartNode {

    private int m_iteration;
    private int m_maxIterations;
    private DataTableSpec m_variablesSpec;
    private RowIterator m_variablesIterator;
    private DataRow m_currentVariables;
    
    /** Two inputs, one output..  */
    public IterateVariablesLoopHeadNodeModel() {
        super(2, 1);
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        m_variablesSpec = inSpecs[1];
        pushVariables();
        return new DataTableSpec[]{inSpecs[0]};
    }
    
    private void pushVariables() {
        int colCount = 0;
        if (m_variablesSpec != null) {
            colCount = m_variablesSpec.getNumColumns();
        }
        for (int i = 0; i < colCount; i++) {
            DataColumnSpec spec = m_variablesSpec.getColumnSpec(i);
            DataType type = spec.getType();
            String name = spec.getName();
            DataCell cell = m_currentVariables == null 
                ? null : m_currentVariables.getCell(i);
            if (type.isCompatible(IntValue.class)) {
                if (cell == null) {
                    pushScopeVariableInt(name, 0);
                } else if (!cell.isMissing()) {
                    pushScopeVariableInt(name, ((IntValue)cell).getIntValue());
                }
            } else if (type.isCompatible(DoubleValue.class)) {
                if (cell == null) {
                    pushScopeVariableDouble(name, 0.0);
                } else if (!cell.isMissing()) {
                    pushScopeVariableDouble(
                            name, ((DoubleValue)cell).getDoubleValue());
                }
            } else if (type.isCompatible(StringValue.class)) {
                if (cell == null) {
                    pushScopeVariableString(name, "");
                } else if (!cell.isMissing()) {
                    pushScopeVariableString(
                            name, ((StringValue)cell).getStringValue());
                }
            }
        }
        pushScopeVariableInt("currentIteration", m_iteration);
        pushScopeVariableInt("maxIterations", m_maxIterations);
    }
    
    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable variables = inData[1];
        if (m_variablesSpec != null) {
            assert m_variablesSpec.equalStructure(variables.getDataTableSpec())
                : "Spec in loop iterations don't match";
            m_variablesSpec = variables.getDataTableSpec();
        }
        m_maxIterations = variables.getRowCount();
        if (m_variablesIterator == null) {
            m_variablesIterator = variables.iterator();
        }
        if (!m_variablesIterator.hasNext()) {
            throw new Exception("No more iterations (variables table has "
                    + variables.getRowCount() + " variable sets, i.e. rows)");
        }
        m_currentVariables = m_variablesIterator.next();
        pushVariables();
        m_iteration += 1;
        return new BufferedDataTable[]{inData[0]};
    }
    
    /** {@inheritDoc} */
    @Override
    protected void reset() {
        m_variablesSpec = null;
        m_variablesIterator = null;
        m_currentVariables = null;
        m_iteration = 0;
        m_maxIterations = 0;
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(
            final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(
            final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

}
