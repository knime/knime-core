/*
 * ------------------------------------------------------------------------
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
 * Created on 25.11.2013 by thor
 */
package org.knime.core.util;

import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.knime.core.api.node.workflow.NodeProgress;
import org.knime.core.api.node.workflow.NodeProgressEvent;
import org.knime.core.api.node.workflow.NodeProgressListener;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeProgressMonitor;
import org.knime.core.node.workflow.NodeID;

/**
 * Adapter that converts an {@link IProgressMonitor} into a {@link NodeProgressMonitor}.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.9
 */
public class ProgressMonitorAdapter implements NodeProgressMonitor {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ProgressMonitorAdapter.class);

    private final CopyOnWriteArrayList<NodeProgressListener> m_listeners;

    private final IProgressMonitor m_monitor;

    private int m_worked; // progress reported so far

    private String m_message;

    /**
     * Creates a new adapter.
     *
     * @param monitor the underlying progress monitor
     */
    public ProgressMonitorAdapter(final IProgressMonitor monitor) {
        m_listeners = new CopyOnWriteArrayList<NodeProgressListener>();
        m_monitor = monitor;
        m_worked = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkCanceled() throws CanceledExecutionException {
        if (m_monitor.isCanceled()) {
            throw new CanceledExecutionException("Execution canceled");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProgress(final double progress) {
        int newWork = (int)Math.floor(progress * 100.0);
        if (newWork > m_worked) {
            m_monitor.worked(newWork - m_worked); // report only the increment
            m_worked = newWork;
            fireProgressChanged();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Double getProgress() {
        return m_worked / 100.0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProgress(final double progress, final String message) {
        setMessage(message);
        setProgress(progress);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMessage(final String message) {
        if (m_message == null) {
            if (message == null) {
                return; // no change
            }
        }
        if (m_message == null || !m_message.equals(message)) {
            m_message = message;
            m_monitor.subTask(message);
            fireProgressChanged();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProgress(final String message) {
        setMessage(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessage() {
        return m_message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setExecuteCanceled() {
        m_monitor.setCanceled(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        // can't reset the progress
    }

    private void fireProgressChanged() {
        NodeProgress pe = new NodeProgress(getProgress(), getMessage());
        for (NodeProgressListener l : m_listeners) {
            try {
                // we can't provide a useful node id here (copied from DefaultNodeProgressMonitor!)
                l.progressChanged(new NodeProgressEvent(new NodeID(0), pe));
            } catch (Throwable t) {
                LOGGER.error("Exception while notifying listeners", t);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addProgressListener(final NodeProgressListener l) {
        if ((l != null) && !m_listeners.contains(l)) {
            m_listeners.add(l);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeProgressListener(final NodeProgressListener l) {
        if (l != null) {
            m_listeners.remove(l);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAllProgressListener() {
        m_listeners.clear();
    }

}
