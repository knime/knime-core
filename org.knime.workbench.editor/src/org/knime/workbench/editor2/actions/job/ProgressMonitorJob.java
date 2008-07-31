/* Created on Jun 20, 2006 1:28:07 PM by thor
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
 */
package org.knime.workbench.editor2.actions.job;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.knime.core.node.NodeProgressMonitor;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeProgressEvent;
import org.knime.core.node.workflow.NodeProgressListener;
import org.knime.core.node.workflow.WorkflowManager;


/**
 * This class is a dummy node job whose only responsibility is to show a
 * progress bar for the running (or waiting) node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class ProgressMonitorJob extends Job implements NodeProgressListener {
    private final NodeProgressMonitor m_nodeMonitor;

    private final WorkflowManager m_wfm;

    private final NodeContainer m_node;

    private IProgressMonitor m_eclipseMonitor;

    private int m_currentWorked;

    private String m_currentProgressMessage;

    private String m_stateMessage;

    private volatile boolean m_finished;

    /**
     * Creates a new dummy node job.
     *
     * @param name the job's name
     * @param monitor the progress monitor to listen to
     * @param wfm the workflow manage responsole for the node
     * @param node the node that should be monitored
     * @param initMessage an initial messge for the progress bar
     */
    public ProgressMonitorJob(final String name,
            final NodeProgressMonitor monitor, final WorkflowManager wfm,
            final NodeContainer node, final String initMessage) {

        super(name);
        m_stateMessage = initMessage;
        m_nodeMonitor = monitor;
        m_wfm = wfm;
        m_node = node;
        m_currentProgressMessage = "";
        setPriority(LONG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IStatus run(final IProgressMonitor monitor) {
        m_eclipseMonitor = monitor;
        m_currentWorked = 0;
        m_nodeMonitor.addProgressListener(this);
        // Start new task (100 %, must be converted from internal double scale)
        // TODO use IProgressMonitor.UNKNOWN if unknown !!!

        // TODO workaround for bug #239. The very last progress (remainder 100)
        // is not shown in the progress bar in eclipse. If we set it to 101
        // here, it will be shown with little space to the right.
        m_eclipseMonitor.beginTask("", 101);
        m_eclipseMonitor.subTask(m_stateMessage);

        try {
            while (!m_finished) {
                if (m_eclipseMonitor.isCanceled()) {
                    m_wfm.cancelExecution(m_node);
                }
                try {
                    Thread.sleep(250);
                } catch (InterruptedException ex) {
                    // nothing to do here
                }
            }
        } finally {
            m_nodeMonitor.removeProgressListener(this);
        }

        return Status.OK_STATUS;
    }

    /**
     * Updates UI after progress has changed.
     *
     * @see org.knime.core.node.NodeProgressListener
     *      #progressChanged(NodeProgressEvent)
     */
    public synchronized void progressChanged(final NodeProgressEvent pe) {

        String message = pe.getNodeProgress().getMessage();

        int newWorked = m_currentWorked;
        if (pe.getNodeProgress().getProgress() >= 0) {
            double progress = pe.getNodeProgress().getProgress().doubleValue();
            newWorked = (int)Math.round(Math.max(0, Math.min(progress * 100,
                100)));
        }

        boolean change = newWorked > m_currentWorked
                || !m_currentProgressMessage.equals(message);

        if (change) {
            // TODO this does not work if we don't know if progress is provided
            int worked = newWorked - m_currentWorked;
            if (worked > 0) { // only update if something changed
                m_eclipseMonitor.worked(worked);
                m_currentWorked = newWorked;
            }

            if (!m_currentProgressMessage.equals(message)) {
                if (message == null) {
                    message = m_stateMessage;
                } else {
                    message = m_stateMessage + " - " + message;
                }

                m_eclipseMonitor.subTask(message);
            }

            m_currentProgressMessage = message == null ? "" : m_stateMessage
                    + " - " + message;
        }
    }

    /**
     * Finishes the job.
     */
    public void finish() {
        m_finished = true;
    }

    /**
     * Sets a new state message.
     *
     * @param stateMessage the state message to set
     */
    public void setStateMessage(final String stateMessage) {
        m_stateMessage = stateMessage;
    }
}
