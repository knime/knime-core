/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2013
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
 *   22.01.2009 (meinl): created
 */
package org.knime.base.node.meta.looper;

import java.io.File;
import java.io.IOException;

import org.knime.base.data.append.column.AppendedColumnRow;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
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
import org.knime.core.node.workflow.LoopStartNodeTerminator;

/**
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class LoopEnd2NodeModel extends NodeModel implements LoopEndNode {

    private BufferedDataContainer[] m_resultContainer;

    private int m_count;

    private final LoopEndNodeSettings m_settings = new LoopEndNodeSettings();

    /**
     * Creates a new model.
     */
    public LoopEnd2NodeModel() {
        super(2, 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{createSpec(inSpecs[0]),
                createSpec(inSpecs[1])};
    }

    private DataTableSpec createSpec(final DataTableSpec inSpec) {
        if (m_settings.addIterationColumn()) {
            DataColumnSpecCreator crea =
                    new DataColumnSpecCreator(
                            DataTableSpec.getUniqueColumnName(inSpec,
                                    "Iteration"), IntCell.TYPE);
            DataTableSpec newSpec = new DataTableSpec(crea.createSpec());

            return new DataTableSpec(inSpec, newSpec);
        } else {
            return inSpec;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        if (!(this.getLoopStartNode() instanceof LoopStartNodeTerminator)) {
            throw new IllegalStateException("Loop end is not connected"
                   + " to matching/corresponding loop start node. You"
                   + "are trying to create an infinite loop!");
        }
        if (m_resultContainer == null) {
            // first time we are getting to this: open container
            m_resultContainer = new BufferedDataContainer[] {
                    exec.createDataContainer(createSpec(inData[0]
                            .getDataTableSpec())),
                    exec.createDataContainer(createSpec(inData[1]
                            .getDataTableSpec()))
            };
        }

        if (m_settings.addIterationColumn()) {
            final IntCell currIterCell = new IntCell(m_count);
            for (DataRow row : inData[0]) {
                AppendedColumnRow newRow = new AppendedColumnRow(
                       createNewRow(row), currIterCell);
                m_resultContainer[0].addRowToTable(newRow);
            }
            for (DataRow row : inData[1]) {
                AppendedColumnRow newRow = new AppendedColumnRow(
                        createNewRow(row), currIterCell);
                m_resultContainer[1].addRowToTable(newRow);
            }
        } else {
            for (DataRow row : inData[0]) {
                m_resultContainer[0].addRowToTable(createNewRow(row));
            }
            for (DataRow row : inData[1]) {
                m_resultContainer[1].addRowToTable(createNewRow(row));
            }
        }

        final boolean terminateLoop =
            ((LoopStartNodeTerminator)this.getLoopStartNode()).terminateLoop();
        if (terminateLoop) {
            // this was the last iteration - close container and continue
            m_resultContainer[0].close();
            m_resultContainer[1].close();
            final BufferedDataTable[] outTables =
                {m_resultContainer[0].getTable(),
                    m_resultContainer[1].getTable()};
            m_resultContainer = null;
            m_count = 0;

            return outTables;
        } else {
            continueLoop();
            m_count++;
            return new BufferedDataTable[2];
        }
    }

    private DataRow createNewRow(final DataRow row) {
        RowKey newKey;
        if (m_settings.uniqueRowIDs()) {
            newKey = new RowKey(row.getKey() + "#" + m_count);
        } else {
            newKey = row.getKey();
        }
        return new DefaultRow(newKey, row);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // empty
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
        m_resultContainer = null;
        m_count = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // empty
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
        LoopEndNodeSettings s = new LoopEndNodeSettings();
        s.loadSettings(settings);
    }
}
