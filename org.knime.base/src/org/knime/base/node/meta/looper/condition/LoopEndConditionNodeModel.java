/* ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 *
 * History
 *   02.09.2008 (thor): created
 */
package org.knime.base.node.meta.looper.condition;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;

import org.knime.base.data.append.column.AppendedColumnRow;
import org.knime.base.node.meta.looper.condition.LoopEndConditionSettings.Operator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.LoopStartNode;
import org.knime.core.node.workflow.LoopStartNodeTerminator;
import org.knime.core.node.workflow.ScopeVariable;
import org.knime.core.node.workflow.ScopeVariable.Type;

/**
 * This class is the model for the condition loop tail node. It checks the user
 * condition in each iteration and decides if the loop should be finished.
 * Meanwhile it collects all rows from each iteration.s
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class LoopEndConditionNodeModel extends NodeModel implements
        LoopEndNode {
    private final LoopEndConditionSettings m_settings =
            new LoopEndConditionSettings();

    private BufferedDataContainer m_collectContainer;

    private BufferedDataContainer m_variableContainer;

    private static DataTableSpec createSpec1(final DataTableSpec inSpec) {
        DataColumnSpecCreator crea =
                new DataColumnSpecCreator(DataTableSpec.getUniqueColumnName(
                        inSpec, "Iteration"), IntCell.TYPE);
        DataTableSpec newSpec = new DataTableSpec(crea.createSpec());

        return new DataTableSpec(inSpec, newSpec);
    }

    private DataTableSpec createSpec2() {
        DataType type;
        if (m_settings.variableType() == Type.DOUBLE) {
            type = DoubleCell.TYPE;
        } else if (m_settings.variableType() == Type.INTEGER) {
            type = IntCell.TYPE;
        } else {
            type = StringCell.TYPE;
        }

        DataColumnSpecCreator crea =
                new DataColumnSpecCreator("Variable value", type);
        return new DataTableSpec(crea.createSpec());
    }

    /**
     * Creates a new node model.
     */
    public LoopEndConditionNodeModel() {
        super(1, 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_settings.variableName() == null) {
            throw new InvalidSettingsException("No variable selected");
        }
        if (m_settings.value() == null) {
            throw new InvalidSettingsException(
                    "No value for the condition entered");
        }

        try {
            if (m_settings.variableType() == ScopeVariable.Type.INTEGER) {
                peekScopeVariableInt(m_settings.variableName());
                try {
                    Integer.parseInt(m_settings.value());
                } catch (NumberFormatException ex) {
                    throw new InvalidSettingsException("Given value '"
                            + m_settings.value() + "' is not an integer");
                }
            } else if (m_settings.variableType() == ScopeVariable.Type.DOUBLE) {
                peekScopeVariableDouble(m_settings.variableName());
                try {
                    Double.parseDouble(m_settings.value());
                } catch (NumberFormatException ex) {
                    throw new InvalidSettingsException("Given value '"
                            + m_settings.value() + "' is not an number");
                }
            } else {
                peekScopeVariableString(m_settings.variableName());
            }
        } catch (NoSuchElementException ex) {
            throw new InvalidSettingsException("No variable named '"
                    + m_settings.variableName() + "' of type "
                    + m_settings.variableType().toString().toLowerCase()
                    + " found");
        }

        return new DataTableSpec[]{createSpec1(inSpecs[0]), createSpec2()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        int count = peekScopeVariableInt("currentIteration");
        exec.setMessage("Iteration " + count);
        DataTableSpec spec1 = createSpec1(inData[0].getDataTableSpec());
        if (m_collectContainer == null) {
            assert m_variableContainer == null;
            // first time we are getting to this: open container
            m_collectContainer = exec.createDataContainer(spec1);
            m_variableContainer = exec.createDataContainer(createSpec2());
        } else if (!spec1.equalStructure(m_collectContainer.getTableSpec())) {
            DataTableSpec predSpec = m_collectContainer.getTableSpec();
            StringBuilder error = new StringBuilder(
                    "Input table's structure differs from reference " 
                    + "(first iteration) table: ");
            if (spec1.getNumColumns() != predSpec.getNumColumns()) {
                error.append("different column counts ");
                error.append(spec1.getNumColumns());
                error.append(" vs. ").append(predSpec.getNumColumns());
            } else {
                for (int i = 0; i < spec1.getNumColumns(); i++) {
                    DataColumnSpec inCol = spec1.getColumnSpec(i);
                    DataColumnSpec predCol = predSpec.getColumnSpec(i);
                    if (!inCol.equalStructure(predCol)) {
                      error.append("Column ").append(i).append(" [");
                      error.append(inCol).append("] vs. [");
                      error.append(predCol).append("]");
                    }
                }
            }
            throw new IllegalArgumentException(error.toString());
        }
        
        RowKey rk = new RowKey("Iteration " + count);
        if (m_settings.variableType() == Type.DOUBLE) {
            m_variableContainer.addRowToTable(new DefaultRow(rk,
                    new DoubleCell(peekScopeVariableDouble(m_settings
                            .variableName()))));
        } else if (m_settings.variableType() == Type.INTEGER) {
            m_variableContainer.addRowToTable(new DefaultRow(rk, new IntCell(
                    peekScopeVariableInt(m_settings.variableName()))));
        } else {
            m_variableContainer.addRowToTable(new DefaultRow(rk,
                    new StringCell(peekScopeVariableString(m_settings
                            .variableName()))));
        }

        LoopStartNode lsn = getLoopStartNode();

        boolean stop = checkCondition()
            || ((lsn instanceof LoopStartNodeTerminator)
                    && ((LoopStartNodeTerminator) lsn).terminateLoop());

        if ((m_settings.addLastRows() && !m_settings.addLastRowsOnly())
                || ((stop == m_settings.addLastRows()) && (stop == m_settings
                        .addLastRowsOnly()))) {
            exec.setMessage("Collecting rows from current iteration");
            int k = 0;
            final double max = inData[0].getRowCount();
            IntCell currIterCell = new IntCell(count);
            for (DataRow row : inData[0]) {
                exec.checkCanceled();
                if (k++ % 10 == 0) {
                    exec.setProgress(k / max);
                }
                AppendedColumnRow newRow =
                        new AppendedColumnRow(new DefaultRow(new RowKey(row
                                .getKey()
                                + "#" + count), row), currIterCell);
                m_collectContainer.addRowToTable(newRow);
            }
        }

        if (stop) {
            m_collectContainer.close();
            m_variableContainer.close();
            return new BufferedDataTable[]{m_collectContainer.getTable(),
                    m_variableContainer.getTable()};
        } else {
            continueLoop();
            return new BufferedDataTable[2];
        }
    }

    private boolean checkCondition() {
        if (m_settings.variableType() == Type.INTEGER) {
            int v = peekScopeVariableInt(m_settings.variableName());
            if (m_settings.operator() == Operator.EQ) {
                return v == Integer.parseInt(m_settings.value());
            } else if (m_settings.operator() == Operator.NE) {
                return v != Integer.parseInt(m_settings.value());
            } else if (m_settings.operator() == Operator.LT) {
                return v < Integer.parseInt(m_settings.value());
            } else if (m_settings.operator() == Operator.LE) {
                return v <= Integer.parseInt(m_settings.value());
            } else if (m_settings.operator() == Operator.GE) {
                return v >= Integer.parseInt(m_settings.value());
            } else if (m_settings.operator() == Operator.GT) {
                return v > Integer.parseInt(m_settings.value());
            }
        } else if (m_settings.variableType() == Type.DOUBLE) {
            double v = peekScopeVariableDouble(m_settings.variableName());
            if (m_settings.operator() == Operator.EQ) {
                return v == Double.parseDouble(m_settings.value());
            } else if (m_settings.operator() == Operator.NE) {
                return v != Double.parseDouble(m_settings.value());
            } else if (m_settings.operator() == Operator.LT) {
                return v < Double.parseDouble(m_settings.value());
            } else if (m_settings.operator() == Operator.LE) {
                return v <= Double.parseDouble(m_settings.value());
            } else if (m_settings.operator() == Operator.GE) {
                return v >= Double.parseDouble(m_settings.value());
            } else if (m_settings.operator() == Operator.GT) {
                return v > Double.parseDouble(m_settings.value());
            }
        } else {
            String s = peekScopeVariableString(m_settings.variableName());
            if (m_settings.operator() == Operator.EQ) {
                return s.equals(m_settings.value());
            } else if (m_settings.operator() == Operator.NE) {
                return !s.equals(m_settings.value());
            }
        }
        throw new IllegalStateException("Why are we here?");
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
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_variableContainer = null;
        m_collectContainer = null;
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
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        LoopEndConditionSettings s = new LoopEndConditionSettings();
        s.loadSettings(settings);

        if (s.operator() == null) {
            throw new InvalidSettingsException(
                    "No comparison operator selected");
        }
        if ((s.variableType() != Type.STRING) && ((s.value() == null)
                || (s.value().length() < 1))) {
            throw new InvalidSettingsException("No comparison value given");
        }
    }
}
