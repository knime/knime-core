/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * ---------------------------------------------------------------------
 *
 * History
 *   13.02.2008 (thor): created
 */
package org.knime.base.node.meta.looper;

import java.io.File;
import java.io.IOException;

import org.knime.base.data.append.column.AppendedColumnRow;
import org.knime.core.data.DataColumnSpec;
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
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.LoopStartNodeTerminator;

/**
 * This model is the tail node of a for loop.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class LoopEndNodeModel extends NodeModel implements LoopEndNode {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(LoopEndNodeModel.class);

    private long m_startTime;

    private BufferedDataContainer m_resultContainer;

    private int m_count;

    private final LoopEndNodeSettings m_settings = new LoopEndNodeSettings();

    /**
     * Creates a new model.
     */
    public LoopEndNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{createSpec(inSpecs[0])};
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
            throw new IllegalStateException("Loop End is not connected"
                    + " to matching/corresponding Loop Start node. You"
                    + " are trying to create an infinite loop!");
        }
        BufferedDataTable in = inData[0];
        DataTableSpec amendedSpec = createSpec(in.getDataTableSpec());
        if (m_resultContainer == null) {
            // first time we are getting to this: open container
            m_startTime = System.currentTimeMillis();
            m_resultContainer = exec.createDataContainer(amendedSpec);
        } else if (!amendedSpec
                .equalStructure(m_resultContainer.getTableSpec())) {
            DataTableSpec predSpec = m_resultContainer.getTableSpec();
            StringBuilder error =
                    new StringBuilder(
                            "Input table's structure differs from reference "
                                    + "(first iteration) table: ");
            if (amendedSpec.getNumColumns() != predSpec.getNumColumns()) {
                error.append("different column counts ");
                error.append(amendedSpec.getNumColumns());
                error.append(" vs. ").append(predSpec.getNumColumns());
            } else {
                for (int i = 0; i < amendedSpec.getNumColumns(); i++) {
                    DataColumnSpec inCol = amendedSpec.getColumnSpec(i);
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

        if (m_settings.addIterationColumn()) {
            IntCell currIterCell = new IntCell(m_count);
            for (DataRow row : in) {
                AppendedColumnRow newRow = new AppendedColumnRow(
                        createNewRow(row), currIterCell);
                m_resultContainer.addRowToTable(newRow);
            }
        } else {
            for (DataRow row : in) {
                m_resultContainer.addRowToTable(createNewRow(row));
            }
        }

        boolean terminateLoop =
                ((LoopStartNodeTerminator)this.getLoopStartNode())
                        .terminateLoop();
        if (terminateLoop) {
            // this was the last iteration - close container and continue
            m_resultContainer.close();
            BufferedDataTable outTable = m_resultContainer.getTable();
            m_resultContainer.close();
            m_resultContainer = null;
            m_count = 0;
            LOGGER.debug("Total loop execution time: "
                    + (System.currentTimeMillis() - m_startTime) + "ms");
            m_startTime = 0;
            return new BufferedDataTable[]{outTable};
        } else {
            continueLoop();
            m_count++;
            return new BufferedDataTable[1];
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
        m_startTime = 0;
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
