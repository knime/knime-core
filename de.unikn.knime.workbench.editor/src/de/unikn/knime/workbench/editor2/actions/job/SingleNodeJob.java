/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
 * History
 *   09.06.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.editor2.actions.job;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import de.unikn.knime.core.node.DefaultNodeProgressMonitor;
import de.unikn.knime.core.node.NodeProgressListener;
import de.unikn.knime.core.node.workflow.NodeContainer;
import de.unikn.knime.core.node.workflow.WorkflowManager;

/**
 * Job for executing a single node in a Workflow.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class SingleNodeJob extends Job implements NodeProgressListener {

    private NodeContainer m_container;

    private int m_currentWorked;

    private IProgressMonitor m_monitor;

    private WorkflowManager m_manager;

    private String m_currentProgressMessage;

    /**
     * @param manager The workflow manager
     * @param container The node container to execute
     * @param parentMonitor parent monitor
     * @param ticks amount of ticks that this sub-job should take
     */
    public SingleNodeJob(final WorkflowManager manager,
            final NodeContainer container,
            final IProgressMonitor parentMonitor, final int ticks) {
        super("Executing " + container.getNodeNameWithID());

        // m_monitor = parentMonitor;
        m_container = container;
        m_manager = manager;
        setProgressGroup(parentMonitor, ticks);
    }

    /**
     * Executes the node container and waits for it to finish.
     * 
     * @see org.eclipse.core.runtime.jobs.Job
     *      #run(org.eclipse.core.runtime.IProgressMonitor)
     */
    protected IStatus run(final IProgressMonitor monitor) {
        DefaultNodeProgressMonitor nodeProgress = null;
        m_monitor = monitor;

        // Start new task (100 %, must be converted from internal double scale)
        // TODO use IProgressMonitor.UNKNOWN if unknown !!!
        
        // TODO workaround for bug #239. The very last progress (remainder 100)
        // is not shown in the progress bar in eclipse. If we set it to 101
        // here, it will be shown with little space to the right.
        m_monitor.beginTask(getName(), 101);

        // make internal progress monitor
        nodeProgress = new DefaultNodeProgressMonitor();

        // we want to react on progress changes
        nodeProgress.addProgressListener(this);

        // we start with 0%
        m_currentWorked = 0;
        m_currentProgressMessage = "";

        // fire it up !
        m_container.startExecution(nodeProgress);

        // dummy progress changed
        progressChanged(0.00, "running...");

        // wait until this one has finished
        while (m_container.isExecuting()) {

            // user canceled?
            if (monitor.isCanceled()) {
                m_manager.cancelExecutionAfterNode(m_container.getID());
                // nodeProgress.setExecuteCanceled();
                // we can't return here, but have to wait until the node
                // (hopefully) notifies the container about its finish.
            }

            // wait a bit...
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        nodeProgress.removeProgressListener(this);

        return Status.OK_STATUS;
        // we handle this, always be happy
        // m_container.executionSucceeded()
        // ? Status.OK_STATUS
        // : new JobStatus(IStatus.ERROR, this, "Execution failed: "
        // + m_container.getNode().getNodeName());

    }

    /**
     * Updates UI after progress has changed.
     * 
     * @see de.unikn.knime.core.node.NodeProgressListener
     *      #progressChanged(double, java.lang.String)
     */
    public synchronized void progressChanged(final double progress,
            final String message) {

        int newWorked = (int)Math.round(
                Math.max(0, Math.min(progress * 100, 100)));
        boolean change = newWorked > m_currentWorked 
            || !m_currentProgressMessage.equals(message);
        
        if (change) {
            // TODO this does not work if we don't know if progress is provided
            int worked = newWorked - m_currentWorked; 
            if (worked > 0) { // only update if something changed
                m_monitor.worked(worked);
                m_currentWorked = newWorked;
            }

            if (!m_currentProgressMessage.equals(message)) {
                m_monitor.subTask(message == null ? "" : message);
            }

            m_currentProgressMessage = message == null ? "" : message;

        }

    }
}
