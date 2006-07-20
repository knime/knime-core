/* Created on Jun 20, 2006 1:28:07 PM by thor
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 */
package de.unikn.knime.workbench.editor2.actions.job;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import de.unikn.knime.core.node.NodeProgressListener;
import de.unikn.knime.core.node.NodeProgressMonitor;
import de.unikn.knime.core.node.workflow.NodeContainer;
import de.unikn.knime.core.node.workflow.WorkflowManager;

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
     * @param manager the workflow manage responsole for the node
     * @param node the node that should be monitored
     */
    public ProgressMonitorJob(final String name,
            final NodeProgressMonitor monitor, final WorkflowManager manager,
            final NodeContainer node, final String initMessage) {

        super(name);
        m_stateMessage = initMessage;
        m_nodeMonitor = monitor;
        m_wfm = manager;
        m_node = node;
        m_currentProgressMessage = "";
        setPriority(LONG);
    }

    /**
     * @see org.eclipse.core.runtime.jobs.Job
     *      #run(org.eclipse.core.runtime.IProgressMonitor)
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
     * @see de.unikn.knime.core.node.NodeProgressListener
     *      #progressChanged(double, java.lang.String)
     */
    public synchronized void progressChanged(final double progress,
            final String message) {

        String tmpMessage = message;

        int newWorked = (int)Math.round(Math.max(0, Math.min(progress * 100,
                100)));
        boolean change = newWorked > m_currentWorked
                || !m_currentProgressMessage.equals(tmpMessage);

        if (change) {
            // TODO this does not work if we don't know if progress is provided
            int worked = newWorked - m_currentWorked;
            if (worked > 0) { // only update if something changed
                m_eclipseMonitor.worked(worked);
                m_currentWorked = newWorked;
            }

            if (!m_currentProgressMessage.equals(tmpMessage)) {

                if (message == null) {

                    tmpMessage = m_stateMessage;
                } else {

                    tmpMessage = m_stateMessage + " - " + message;
                }

                m_eclipseMonitor.subTask(tmpMessage);
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
