/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   Oct 14, 2015 (hornm): created
 */
package org.knime.testing.streaming.testexecutor;

import java.net.URL;
import java.util.concurrent.Future;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.workflow.AbstractNodeExecutionJobManager;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainer.NodeContainerSettings.SplitType;
import org.knime.core.node.workflow.NodeExecutionJob;
import org.knime.core.node.workflow.NodeExecutionJobManagerPanel;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.util.ThreadPool;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class StreamingTestNodeExecutionJobManager extends AbstractNodeExecutionJobManager {

    public static final StreamingTestNodeExecutionJobManager INSTANCE = new StreamingTestNodeExecutionJobManager();

    private final ThreadPool m_pool;

    private int m_numChunks = StreamingTestJobMgrSettingsPanel.DEFAULT_NUM_CHUNKS;

    public StreamingTestNodeExecutionJobManager() {
        this(KNIMEConstants.GLOBAL_THREAD_POOL);
    }

    public StreamingTestNodeExecutionJobManager(final ThreadPool pool) {
        if (pool == null) {
            throw new NullPointerException("arg must not be null");
        }
        m_pool = pool;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeExecutionJob submitJob(final NodeContainer nc, final PortObject[] data) {
        if (!(nc instanceof SingleNodeContainer)) {
            throw new IllegalStateException(getClass().getSimpleName()
                    + " is not able to execute a meta node: " + nc.getNameWithID());
        }
        StreamingTestNodeExecutionJob job = new StreamingTestNodeExecutionJob(nc, data, m_numChunks);
        Future<?> future = m_pool.enqueue(job);
        job.setFuture(future);
        return job;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load(final NodeSettingsRO settings) throws InvalidSettingsException {
        try {
            m_numChunks = settings.getInt(StreamingTestJobMgrSettingsPanel.CFG_NUM_CHUNKS);
        } catch (InvalidSettingsException e) {
            //use default value
            m_numChunks = StreamingTestJobMgrSettingsPanel.DEFAULT_NUM_CHUNKS;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final NodeSettingsWO settings) {
        settings.addInt(StreamingTestJobMgrSettingsPanel.CFG_NUM_CHUNKS, m_numChunks);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeExecutionJobManagerPanel getSettingsPanelComponent(final SplitType nodeSplitType) {
        return new StreamingTestJobMgrSettingsPanel(nodeSplitType);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getID() {
        return StreamingTestNodeExecutionJobManagerFactory.ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URL getIcon() {
        return getClass().getResource("icons/streaming_test.png");
    }

}
