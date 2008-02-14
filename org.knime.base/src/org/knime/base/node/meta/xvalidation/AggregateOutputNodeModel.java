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

/**
 * This models aggregates the result from each of the cross validation loops. It
 * will only work together with predecessing {@link XValidatePartitionModel}.
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @author Thorsten Meinl, University of Konstanz
 */
public class AggregateOutputNodeModel extends NodeModel {
    private static final DataTableSpec STATISTICS_SPEC =
            new DataTableSpec(new DataColumnSpecCreator("Error in %",
                    DoubleCell.TYPE).createSpec(), new DataColumnSpecCreator(
                    "Size of Test Set", IntCell.TYPE).createSpec(),
                    new DataColumnSpecCreator("Error Count", IntCell.TYPE)
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
        return new DataTableSpec[]{in, STATISTICS_SPEC};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        final XValLoopContext ctx = peekScopeContext(XValLoopContext.class);

        if (ctx == null) {
            throw new IllegalStateException("No cross validation loop context "
                    + "found");
        }

        if (ctx.currentIteration() == 1) {
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
        ExecutionMonitor subExec =
            exec.createSubProgress(ctx.finished() ? 0.9 : 1);
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
            subExec.setProgress(r / (double)rowCount, "Calculating output " + r
                    + "/" + rowCount + " (\"" + key + "\")");
            subExec.checkCanceled();
        }

        DataRow stats =
                new DefaultRow(new RowKey("fold " + m_foldStatistics.size()),
                        new DoubleCell(100.0 * incorrect / rowCount),
                        new IntCell(rowCount), new IntCell(incorrect));
        m_foldStatistics.add(stats);

        if (!ctx.finished()) {
            continueLoop(ctx);
            return new BufferedDataTable[2];
        } else {
            popScopeContext(XValLoopContext.class);

            BufferedDataContainer cont =
                    exec.createDataContainer(STATISTICS_SPEC);
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
