/*
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   Nov 6, 2006 (wiswedel): created
 */
package org.knime.base.node.meta.xvalidation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
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
import org.knime.core.node.workflow.ScopeVariable;

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
        new DataTableSpec(
                new DataColumnSpecCreator("Total mean squarred error",
                        DoubleCell.TYPE).createSpec(),
                new DataColumnSpecCreator("Mean squarred error per row",
                        DoubleCell.TYPE).createSpec(),
                new DataColumnSpecCreator("Size of Test Set",
                        IntCell.TYPE).createSpec());


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
        DataTableSpec in = inSpecs[0];
        if ((m_settings.targetColumn() == null)
                && (m_settings.predictionColumn() == null)) {
            // try to guess columns
            for (int i = in.getNumColumns() - 1; i >= 0; i--) {
                DataColumnSpec c = in.getColumnSpec(i);
                if (c.getType().isCompatible(StringValue.class)) {
                    if (m_settings.predictionColumn() == null) {
                        m_settings.predictionColumn(c.getName());
                    } else {
                        assert m_settings.targetColumn() == null;
                        m_settings.targetColumn(c.getName());
                        break; // both columns assigned
                    }
                }
            }
            if (m_settings.targetColumn() == null) {
                throw new InvalidSettingsException(
                        "Invalid input: Need at least two string columns.");
            }
            setWarningMessage("Auto configuration: Using \""
                    + m_settings.targetColumn() + "\" as target and \""
                    + m_settings.predictionColumn() + "\" as prediction");
        }

        int targetColIndex = in.findColumnIndex(m_settings.targetColumn());
        if (targetColIndex < 0) {
            throw new InvalidSettingsException("No such column: "
                    + m_settings.targetColumn());
        }

        int predictColIndex = in.findColumnIndex(m_settings.predictionColumn());
        if (predictColIndex < 0) {
            throw new InvalidSettingsException("No such column: "
                    + m_settings.predictionColumn());
        }

        DataType commonType = DataType.getCommonSuperType(
                in.getColumnSpec(targetColIndex).getType(),
                in.getColumnSpec(predictColIndex).getType());
        if ((commonType.getValueClasses().size() == 1)
                && (commonType.getValueClasses().get(0) == DataValue.class)) {
            throw new InvalidSettingsException("Target and prediction column "
                    + "are not of compatible type");
        }

        if (in.getColumnSpec(targetColIndex).getType()
                .isCompatible(DoubleValue.class)) {
            return new DataTableSpec[]{in, NUMERIC_STATISTICS_SPEC};
        } else {
            return new DataTableSpec[]{in, NOMINAL_STATISTICS_SPEC};
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
        ScopeVariable countVar = peekScopeVariable("currentIteration");
        int count = countVar.getIntValue();
        ScopeVariable maxCountVar = peekScopeVariable("maxIterations");
        int maxCount = maxCountVar.getIntValue();

        if (count == 1) {
            m_predictionTable =
                    exec.createDataContainer(inData[0].getDataTableSpec());
        }
        final BufferedDataTable in = inData[0];

        final int rowCount = in.getRowCount();
        final int targetColIndex =
                in.getDataTableSpec()
                        .findColumnIndex(m_settings.targetColumn());
        final int predictColIndex =
                in.getDataTableSpec().findColumnIndex(
                        m_settings.predictionColumn());

        final boolean numericMode = in.getDataTableSpec()
                .getColumnSpec(predictColIndex).getType()
                .isCompatible(DoubleValue.class);

        ExecutionMonitor subExec =
            exec.createSubProgress(count == maxCount ? 0.9 : 1);
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

            DataRow stats = new DefaultRow(
                            new RowKey("fold " + m_foldStatistics.size()),
                            new DoubleCell(errorSum),
                            new DoubleCell(errorSum / rowCount),
                            new IntCell(rowCount));
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

            DataRow stats = new DefaultRow(
                            new RowKey("fold " + m_foldStatistics.size()),
                            new DoubleCell(100.0 * incorrect / rowCount),
                            new IntCell(rowCount), new IntCell(incorrect));
            m_foldStatistics.add(stats);
        }



        if (count < maxCount) {
            continueLoop();
            return new BufferedDataTable[2];
        } else {
            BufferedDataContainer cont =
                    exec.createDataContainer(numericMode
                            ? NUMERIC_STATISTICS_SPEC
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
