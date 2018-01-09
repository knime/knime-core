/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
import java.util.Optional;
import java.util.function.Function;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
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

    private static final NodeLogger LOGGER = NodeLogger.getLogger(LoopEndNodeModel.class);

    private long m_startTime;

    //overall row count
    private long m_count = 0;

    //current iteration
    private int m_iteration = 0;

    /* Helper factory to collect the intermediate tables and create
     * the final concatenated table. */
    private ConcatenateTableFactory m_tableFactory;

    private final LoopEndNodeSettings m_settings = new LoopEndNodeSettings();


    /** Creates a new model. */
    public LoopEndNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_settings.ignoreEmptyTables() || m_settings.tolerateColumnTypes() || m_settings.tolerateChangingTableSpecs()) {
            return new DataTableSpec[]{null};
        } else {
            return new DataTableSpec[]{ConcatenateTableFactory.createSpec(inSpecs[0], m_settings.addIterationColumn(), false)};
        }
    }


    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        if (!(this.getLoopStartNode() instanceof LoopStartNodeTerminator)) {
            throw new IllegalStateException("Loop End is not connected"
                    + " to matching/corresponding Loop Start node. You"
                    + " are trying to create an infinite loop!");
        }

        if(m_tableFactory == null) {
            //first time we get here: create table factory
            Optional<Function<RowKey, RowKey>> rowKeyFunc;
            switch(m_settings.rowKeyPolicy()) {
                case APPEND_SUFFIX:
                    rowKeyFunc = Optional.of(k -> {return new RowKey(k.toString() + "#" + (m_iteration));});
                    break;
                case GENERATE_NEW:
                    rowKeyFunc = Optional.of(k -> {return new RowKey("Row" + (m_count++));});
                    break;
                case UNMODIFIED:
                default:
                    rowKeyFunc = Optional.empty();
            }
            m_tableFactory = new ConcatenateTableFactory(m_settings.ignoreEmptyTables(),
                m_settings.tolerateColumnTypes(), m_settings.addIterationColumn(), m_settings.tolerateChangingTableSpecs(), rowKeyFunc);
            m_startTime = System.currentTimeMillis();
        }

        m_tableFactory.addTable(inData[0], exec);

        boolean terminateLoop = ((LoopStartNodeTerminator)this.getLoopStartNode()).terminateLoop();
        if (terminateLoop) {
            LOGGER.debug("Total loop execution time: " + (System.currentTimeMillis() - m_startTime) + "ms");
            m_startTime = 0;
            m_iteration = 0;
            m_count = 0;
            return new BufferedDataTable[]{m_tableFactory.createTable(exec)};
        } else {
            m_iteration++;
            continueLoop();
            return new BufferedDataTable[1];
        }
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
        m_startTime = 0;
        m_tableFactory = null;
        m_count = 0;
        m_iteration = 0;
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
        AbstractLoopEndNodeSettings s = new LoopEndNodeSettings();
        s.loadSettings(settings);
    }
}
