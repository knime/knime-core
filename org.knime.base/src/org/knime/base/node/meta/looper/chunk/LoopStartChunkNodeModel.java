/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *   02.09.2008 (thor): created
 */
package org.knime.base.node.meta.looper.chunk;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.LoopStartNodeTerminator;

/**
 * Loop start node that outputs a set of rows at a time. Used to implement
 * a streaming (or chunking approach) where only a set of rows is processed at
 * a time
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class LoopStartChunkNodeModel extends NodeModel implements
        LoopStartNodeTerminator {

    private LoopStartChunkConfiguration m_config;

    // loop invariants
    private BufferedDataTable m_table;
    private CloseableRowIterator m_iterator;

    // loop variants
    private int m_iteration;

    /**
     * Creates a new model.
     */
    public LoopStartChunkNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_config == null) {
            m_config = new LoopStartChunkConfiguration();
            setWarningMessage("Using default: " + m_config);
        }
        assert m_iteration == 0;
        pushFlowVariableInt("currentIteration", m_iteration);
        pushFlowVariableInt("maxIterations", 0);
        return inSpecs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable table = inData[0];
        int rowCount = table.getRowCount();

        int totalChunkCount;
        int nrRowsPerIteration;
        switch (m_config.getMode()) {
        case NrOfChunks:
            totalChunkCount = Math.min(m_config.getNrOfChunks(), rowCount);
            nrRowsPerIteration = (int)Math.ceil(
                    rowCount / (double)totalChunkCount);
            break;
        case RowsPerChunk:
            nrRowsPerIteration = m_config.getNrRowsPerChunk();
            totalChunkCount = (int)Math.ceil(
                    rowCount / (double)nrRowsPerIteration);
            break;
        default:
            throw new Exception("Unsupported mode: " + m_config.getMode());
        }

        if (m_iteration == 0) {
            assert getLoopEndNode() == null : "1st iteration but end node set";
            m_table = table;
            m_iterator = table.iterator();
        } else {
            assert getLoopEndNode() != null : "No end node set";
            assert table == m_table : "Input tables differ between iterations";
        }

        BufferedDataContainer cont = exec.createDataContainer(table.getSpec());
        for (int i = 0; i < nrRowsPerIteration && m_iterator.hasNext(); i++) {
            cont.addRowToTable(m_iterator.next());
        }
        cont.close();
        pushFlowVariableInt("currentIteration", m_iteration);
        pushFlowVariableInt("maxIterations", totalChunkCount);
        m_iteration++;
        return new BufferedDataTable[] {cont.getTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_iteration = 0;
        if (m_iterator != null) {
            m_iterator.close();
        }
        m_iterator = null;
        m_table = null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean terminateLoop() {
        boolean continueLoop = m_iterator == null || m_iterator.hasNext();
        return !continueLoop;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_config != null) {
            m_config.saveSettingsTo(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        new LoopStartChunkConfiguration().loadSettingsInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        LoopStartChunkConfiguration config = new LoopStartChunkConfiguration();
        config.loadSettingsInModel(settings);
        m_config = config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals to load
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals to save
    }
}
