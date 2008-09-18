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
 *   Sept 17 2008 (mb): created (from wiswedel's TableToVariableNode)
 */
package org.knime.base.node.flowvariable.variableloophead;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.GenericNodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.workflow.LoopStartNode;

/** Start of loop: pushes variables in input datatable columns
 * onto stack, taking the values from one row per iteration.
 * 
 * @author M. Berthold, University of Konstanz
 */
public class VariableLoopStartNodeModel extends GenericNodeModel
implements LoopStartNode {

    // remember which iteration we are in:
    private int m_currentIteration = -1;
    private int m_maxNrIterations = -1;
    
    /** One input, one output.
     */
    protected VariableLoopStartNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE},
                new PortType[]{FlowVariablePortObject.TYPE});
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        pushVariables((DataTableSpec)inSpecs[0], null);
        return new PortObjectSpec[]{FlowVariablePortObjectSpec.INSTANCE};
    }
    
    /** Pushes the variable as given by the row argument onto the stack. 
     * @param variablesSpec The spec (for names and types)
     * @param currentVariables The values of the variables.
     */
    protected void pushVariables(final DataTableSpec variablesSpec,
            final DataRow currentVariables) {
        int colCount = variablesSpec.getNumColumns();
        DataCell[] cells = new DataCell[variablesSpec.getNumColumns()];
        for (int i = 0; i < cells.length; i++) {
            DataColumnSpec c = variablesSpec.getColumnSpec(i);
            if (c.getType().isCompatible(IntValue.class)) {
                cells[i] = new IntCell(0);
            } else if (c.getType().isCompatible(DoubleValue.class)) {
                cells[i] = new DoubleCell(0);
            } else {
                cells[i] = new StringCell("");
            }
        }
        DataRow defaultRow = new DefaultRow("Default Vars", cells);
        // column names starting with "knime." are uniquified as they represent
        // global constants. 
        HashSet<String> variableNames = new HashSet<String>();
        for (int i = 0; i < colCount; i++) {
            DataColumnSpec spec = variablesSpec.getColumnSpec(i);
            DataType type = spec.getType();
            String name = spec.getName();
            if (name.equals("knime.")) {
                name = "column_" + i;
            } else if (name.startsWith("knime.")) {
                name = name.substring("knime.".length());
            }
            int uniquifier = 1;
            String basename = name;
            while (!variableNames.add(name)) {
                name = basename + "(#" + (uniquifier++) + ")";
            }
            DataCell cell = currentVariables == null 
                ? defaultRow.getCell(i) : currentVariables.getCell(i);
            if (cell.isMissing()) {
                throw new IllegalArgumentException("Missing Values not"
                    + " allowed for Variable Values!");
            }
            if (type.isCompatible(IntValue.class)) {
                pushScopeVariableInt(name, ((IntValue)cell).getIntValue());
            } else if (type.isCompatible(DoubleValue.class)) {
                pushScopeVariableDouble(
                        name, ((DoubleValue)cell).getDoubleValue());
            } else if (type.isCompatible(StringValue.class)) {
                pushScopeVariableString(
                        name, ((StringValue)cell).getStringValue());
            }
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inPOs,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable inData = (BufferedDataTable)inPOs[0];
        if (m_currentIteration == -1) {
            // first time we see this, initalize counters:
            m_currentIteration = 0;
            m_maxNrIterations = inData.getRowCount();
        } else {
            if (m_currentIteration > m_maxNrIterations) {
                throw new IOException("Loop did not terminate correctly.");
            }
        }
        // ok, not nice: iterate over table until current row is reached
        int i = 0;
        DataRow row = null;
        for (DataRow r : inData) {
            i++;
            row = r;
            if (i > m_currentIteration) {
                break;
            }
        }
        if (row == null) {
            throw new Exception("Not enough rows in input table (odd)!");
        }
        // put values for variables on stack, based on current row
        pushVariables(inData.getDataTableSpec(), row);
        // and add information about loop progress
        pushScopeVariableInt("NrIterations", m_maxNrIterations);
        pushScopeVariableInt("currentIteration", m_currentIteration);
        return new PortObject[]{new FlowVariablePortObject()};
    }
    
    /** {@inheritDoc} */
    @Override
    protected void reset() {
        m_currentIteration = -1;
        m_maxNrIterations = -1;
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
