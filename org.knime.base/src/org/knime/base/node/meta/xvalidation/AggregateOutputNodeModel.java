/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *   Nov 6, 2006 (wiswedel): created
 */
package org.knime.base.node.meta.xvalidation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
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

/**
 * This models aggregates the result from each of the cross validation loops. It
 * will only work together with predecessing {@link XValidatePartitionModel}.
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @author Thorsten Meinl, University of Konstanz
 */
public class AggregateOutputNodeModel extends NodeModel implements LoopEndNode {
    private static final DataTableSpec NOMINAL_STATISTICS_SPEC =
            new DataTableSpec(new DataColumnSpecCreator("Error in %",
                    DoubleCell.TYPE).createSpec(), new DataColumnSpecCreator(
                    "Size of Test Set", IntCell.TYPE).createSpec(),
                    new DataColumnSpecCreator("Error Count", IntCell.TYPE)
                            .createSpec());

    private static final DataTableSpec NUMERIC_STATISTICS_SPEC =
            new DataTableSpec(new DataColumnSpecCreator(
                    "Total mean squarred error", DoubleCell.TYPE).createSpec(),
                    new DataColumnSpecCreator("Mean squarred error per row",
                            DoubleCell.TYPE).createSpec(),
                    new DataColumnSpecCreator("Size of Test Set", IntCell.TYPE)
                            .createSpec());

    private final AggregateSettings m_settings = new AggregateSettings();

    private final ArrayList<DataRow> m_foldStatistics =
            new ArrayList<DataRow>();

    private BufferedDataContainer m_predictionTable;

    /**
     * Create a new model for the aggregation node.
     */
    public AggregateOutputNodeModel() {
        super(1, 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec inSpec = inSpecs[0];
        if ((m_settings.targetColumn() == null)
                && (m_settings.predictionColumn() == null)) {
            List<DataColumnSpec> colSpecs = new ArrayList<DataColumnSpec>();
            for (int i = 0; i < inSpec.getNumColumns(); i++) {
                colSpecs.add(inSpec.getColumnSpec(i));
            }

            outer: for (int i = colSpecs.size() - 1; i >= 0; i--) {
                for (int j = i - 1; j >= 0; j--) {
                    DataType commonType =
                            DataType.getCommonSuperType(colSpecs.get(i)
                                    .getType(), colSpecs.get(j).getType());
                    if ((commonType.getValueClasses().size() > 1)
                            || (commonType.getValueClasses().get(0) != DataValue.class)) {
                        m_settings.predictionColumn(colSpecs.get(i).getName());
                        m_settings.targetColumn(colSpecs.get(j).getName());
                        setWarningMessage("Selected columns '"
                                + m_settings.predictionColumn() + "' and '"
                                + m_settings.targetColumn() + "' as prediction"
                                + " and target columns");
                        break outer;
                    }
                }
            }
        }

        int targetColIndex = inSpec.findColumnIndex(m_settings.targetColumn());
        if (targetColIndex < 0) {
            throw new InvalidSettingsException("No such column: "
                    + m_settings.targetColumn());
        }

        int predictColIndex =
                inSpec.findColumnIndex(m_settings.predictionColumn());
        if (predictColIndex < 0) {
            throw new InvalidSettingsException("No such column: "
                    + m_settings.predictionColumn());
        }

        DataType commonType =
                DataType.getCommonSuperType(inSpec
                        .getColumnSpec(targetColIndex).getType(), inSpec
                        .getColumnSpec(predictColIndex).getType());
        if ((commonType.getValueClasses().size() == 1)
                && (commonType.getValueClasses().get(0) == DataValue.class)) {
            throw new InvalidSettingsException("Target and prediction column "
                    + "are not of compatible type");
        }

        if (inSpec.getColumnSpec(targetColIndex).getType().isCompatible(
                DoubleValue.class)) {
            return new DataTableSpec[]{inSpec, NUMERIC_STATISTICS_SPEC};
        } else {
            return new DataTableSpec[]{inSpec, NOMINAL_STATISTICS_SPEC};
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        // retrieve variables from the stack which the head of this
        // loop hopefully put there:
        int count;
        int maxCount;
        try {
            count = peekFlowVariableInt("currentIteration");
            maxCount = peekFlowVariableInt("maxIterations");
        } catch (NoSuchElementException e) {
            throw new Exception("No matching Loop Start node!", e);
        }
        if (count < 0 || count >= maxCount) {
            throw new Exception("Conflicting loop variables, count is " + count
                    + " and max count is " + maxCount);
        }
        final BufferedDataTable in = inData[0];
        final DataTableSpec inSpec = in.getDataTableSpec();
        if (count == 0) {
            m_predictionTable = exec.createDataContainer(in.getDataTableSpec());
        } else if (m_predictionTable == null) {
            throw new Exception(
                    "Loop Head claims this is NOT the first iteration"
                    + " but the tail believes it is?!");
        } else {
            if (!inSpec.equalStructure(m_predictionTable.getTableSpec())) {
                DataTableSpec predSpec = m_predictionTable.getTableSpec();
                StringBuilder error = new StringBuilder(
                        "Input table's structure differs from reference " 
                        + "(first iteration) table: ");
                if (inSpec.getNumColumns() != predSpec.getNumColumns()) {
                    error.append("different column counts ");
                    error.append(inSpec.getNumColumns());
                    error.append(" vs. ").append(predSpec.getNumColumns());
                } else {
                    for (int i = 0; i < inSpec.getNumColumns(); i++) {
                        DataColumnSpec inCol = inSpec.getColumnSpec(i);
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
        }


        final int rowCount = in.getRowCount();
        final int targetColIndex =
                in.getDataTableSpec()
                        .findColumnIndex(m_settings.targetColumn());
        final int predictColIndex =
                in.getDataTableSpec().findColumnIndex(
                        m_settings.predictionColumn());

        final boolean numericMode =
                in.getDataTableSpec().getColumnSpec(predictColIndex).getType()
                        .isCompatible(DoubleValue.class);

        ExecutionMonitor subExec =
                exec.createSubProgress(count == maxCount - 1 ? 0.9 : 1);
        if (numericMode) {
            double errorSum = 0;
            int r = 0;
            for (DataRow row : in) {
                RowKey key = row.getKey();
                DoubleValue target = (DoubleValue)row.getCell(targetColIndex);
                DoubleValue predict = (DoubleValue)row.getCell(predictColIndex);
                double d = (target.getDoubleValue() - predict.getDoubleValue());
                errorSum += d * d;
                r++;

                m_predictionTable.addRowToTable(row);
                subExec.setProgress(r / (double)rowCount, "Calculating output "
                        + r + "/" + rowCount + " (\"" + key + "\")");
                subExec.checkCanceled();
            }

            errorSum = Math.sqrt(errorSum);

            DataRow stats =
                    new DefaultRow(
                            new RowKey("fold " + m_foldStatistics.size()),
                            new DoubleCell(errorSum), new DoubleCell(errorSum
                                    / rowCount), new IntCell(rowCount));
            m_foldStatistics.add(stats);
        } else {
            int correct = 0;
            int incorrect = 0;
            int r = 0;
            for (DataRow row : in) {
                RowKey key = row.getKey();
                DataCell target = row.getCell(targetColIndex);
                DataCell predict = row.getCell(predictColIndex);
                if (target.equals(predict)) {
                    correct++;
                } else {
                    incorrect++;
                }
                r++;

                m_predictionTable.addRowToTable(row);
                subExec.setProgress(r / (double)rowCount, "Calculating output "
                        + r + "/" + rowCount + " (\"" + key + "\")");
                subExec.checkCanceled();
            }

            DataRow stats =
                    new DefaultRow(
                            new RowKey("fold " + m_foldStatistics.size()),
                            new DoubleCell(100.0 * incorrect / rowCount),
                            new IntCell(rowCount), new IntCell(incorrect));
            m_foldStatistics.add(stats);
        }

        if (count < maxCount - 1) {
            continueLoop();
            return new BufferedDataTable[2];
        } else {
            BufferedDataContainer cont =
                    exec
                            .createDataContainer(numericMode ? NUMERIC_STATISTICS_SPEC
                                    : NOMINAL_STATISTICS_SPEC);
            for (DataRow row : m_foldStatistics) {
                cont.addRowToTable(row);
            }
            cont.close();

            m_predictionTable.close();
            return new BufferedDataTable[]{m_predictionTable.getTable(),
                    cont.getTable()};
        }
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
        m_foldStatistics.clear();
        m_predictionTable = null;
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
        new AggregateSettings().loadSettings(settings);
    }
}
