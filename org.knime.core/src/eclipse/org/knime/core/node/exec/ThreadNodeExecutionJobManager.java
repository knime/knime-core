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
 *   Oct 10, 2008 (wiswedel): created
 */
package org.knime.core.node.exec;

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
 * @author wiswedel, University of Konstanz
 */
public class ThreadNodeExecutionJobManager extends AbstractNodeExecutionJobManager {

    public static final ThreadNodeExecutionJobManager INSTANCE =
            new ThreadNodeExecutionJobManager();

    private final ThreadNodeExecutionSettings m_settings;

    private final ThreadPool m_pool;

    public ThreadNodeExecutionJobManager() {
        this(KNIMEConstants.GLOBAL_THREAD_POOL, new ThreadNodeExecutionSettings());
    }

    public ThreadNodeExecutionJobManager(final ThreadPool pool, final ThreadNodeExecutionSettings settings) {
        if (pool == null) {
            throw new NullPointerException("arg must not be null");
        }
        m_settings = settings;
        m_pool = pool;
    }

    /** {@inheritDoc} */
    @Override
    public NodeExecutionJob submitJob(final NodeContainer nc, final PortObject[] data) {
        if (!(nc instanceof SingleNodeContainer)) {
            throw new IllegalStateException(getClass().getSimpleName()
                    + " is not able to execute a metanode: " + nc.getNameWithID());
        }
        LocalNodeExecutionJob job = new LocalNodeExecutionJob((SingleNodeContainer)nc, data, m_settings);
        Future<?> future = m_pool.enqueue(job);
        job.setFuture(future);
        return job;
    }

    /** {@inheritDoc} */
    @Override
    public String getID() {
        return ThreadNodeExecutionJobManagerFactory.INSTANCE.getID();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Threaded Job Manager";
    }

    /** {@inheritDoc} */
    @Override
    public URL getIcon() {
        return null;
    }

    @Override
    public NodeExecutionJobManagerPanel getSettingsPanelComponent(final SplitType nodeSplitType) {
        return new ThreadNodeExecutionJobManagerPanel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }
}
