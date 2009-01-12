/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   Oct 10, 2008 (wiswedel): created
 */
package org.knime.core.node.exec;

import java.net.URL;
import java.util.concurrent.Future;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.workflow.NodeExecutionJob;
import org.knime.core.node.workflow.NodeExecutionJobManager;
import org.knime.core.node.workflow.NodeExecutionJobManagerPanel;
import org.knime.core.node.workflow.NodeExecutionJobReconnectException;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.util.ThreadPool;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class ThreadNodeExecutionJobManager implements NodeExecutionJobManager {

    public static final ThreadNodeExecutionJobManager INSTANCE =
            new ThreadNodeExecutionJobManager();

    private final ThreadPool m_pool;

    public ThreadNodeExecutionJobManager() {
        this(KNIMEConstants.GLOBAL_THREAD_POOL);
    }

    public ThreadNodeExecutionJobManager(final ThreadPool pool) {
        if (pool == null) {
            throw new NullPointerException("arg must not be null");
        }
        m_pool = pool;
    }

    /** {@inheritDoc} */
    @Override
    public NodeExecutionJob submitJob(final SingleNodeContainer snc,
            final PortObject[] data, final ExecutionContext exec) {
        LocalNodeExecutionJob job = new LocalNodeExecutionJob(snc, data, exec);
        Future<?> future = m_pool.enqueue(job);
        job.setFuture(future);
        return job;
    }

    /**
     * {@inheritDoc}
     */
    public String getID() {
        return ThreadNodeExecutionJobManagerFactory.INSTANCE.getID();
    }

    /**
     * {@inheritDoc}
     */
    public NodeExecutionJobManagerPanel getSettingsPanelComponent() {
        // no settings to adjust for this job manager
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Threaded Job Manager";
    }

    /**
     * {@inheritDoc}
     */
    public boolean canDisconnect(final NodeExecutionJob job) {
        // threaded jobs can disconnect - but not reconnect...
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public NodeExecutionJob loadFromReconnectSettings(final NodeSettingsRO settings,
            final PortObject[] inports, final SingleNodeContainer snc, final ExecutionContext exec)
            throws InvalidSettingsException, NodeExecutionJobReconnectException {
        throw new NodeExecutionJobReconnectException(
                "Threaded jobs can't be reconnected");
    }
    
    /** {@inheritDoc} */
    @Override
    public void disconnect(final NodeExecutionJob job) {
        throw new IllegalStateException(
                "Can't disconnect from thread pool execution.");
    }

    /**
     * {@inheritDoc}
     */
    public void saveReconnectSettings(final NodeExecutionJob job,
            final NodeSettingsWO settings) {
        assert false : "Don't save threaded job info! Can't reconnect.";
    }

    /** {@inheritDoc} */
    @Override
    public void save(final NodeSettingsWO settings) {
    }

    /** {@inheritDoc} */
    @Override
    public void load(final NodeSettingsRO settings)
    throws InvalidSettingsException {
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public URL getIcon() {
        return null;
    }

}
