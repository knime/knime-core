/*
 * ------------------------------------------------------------------------
 *
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
 *   4 Jul 2022 (jasper): created
 */
package org.knime.core.node.exec;

import java.net.URL;

import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.workflow.AbstractNodeExecutionJobManager;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeExecutionJob;
import org.knime.core.util.ThreadPool;

/**
 * This class is parent to the default job managers and manages Job <-> ThreadPool association.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz
 */
public abstract class AbstractThreadNodeExecutionJobManager extends AbstractNodeExecutionJobManager {

    private final ThreadPool m_pool;

    /**
     * Create a new instance using the default global thread pool
     */
    protected AbstractThreadNodeExecutionJobManager() {
        this(KNIMEConstants.GLOBAL_THREAD_POOL);
    }

    private AbstractThreadNodeExecutionJobManager(final ThreadPool pool) {
        if (pool == null) {
            throw new IllegalArgumentException("thread pool must not be null");
        }
        m_pool = pool;
    }

    /**
     * Create a {@link LocalNodeExecutionJob} that wraps the execution of the NC using the provided data
     *
     * @param nc The {@link NodeContainer} to execute
     * @param data The data at the input ports of the executed node
     * @return The created {@link LocalNodeExecutionJob}
     */
    protected abstract LocalNodeExecutionJob createJob(final NodeContainer nc, final PortObject[] data);

    /** {@inheritDoc} */
    @Override
    public NodeExecutionJob submitJob(final NodeContainer nc, final PortObject[] data) {
        var job = createJob(nc, data);
        var future = m_pool.enqueue(job);
        job.setFuture(future);
        return job;
    }

    /**
     * Utility method to determine whether the job manager corresponds to the default job manager and can therefore be
     * omitted when saving the node to disk
     *
     * @return {@code false}, if the job manager needs to be saved with its node, {@code true} otherwise
     */
    public abstract boolean isDefault();

    /** {@inheritDoc} */
    @Override
    public URL getIcon() {
        return null;
    }

}
