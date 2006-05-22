/* 
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

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

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
    /**
     * This class is a checker for started KNIME nodes. It frequently checks
     * if a cancel request has been issued by the user and either really cancels
     * it if the job is already running or just removes it from the job queue.
     * 
     * @author Thorsten Meinl, University of Konstanz
     */
    private static class CancelChecker extends Thread {
        private final Map<Future<?>, SingleNodeJob> m_nodeMap =
            new ConcurrentHashMap<Future<?>, SingleNodeJob>();
        
        /**
         * Creates a new CancelChecker and starts it in a separate thread.
         */
        public CancelChecker() {
            super("KNIME Cancel Checker");
            setDaemon(true);
            start();
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                try {
                    Thread.sleep(500);
                    
                    Iterator<Map.Entry<Future<?>, SingleNodeJob>> it = 
                        m_nodeMap.entrySet().iterator();
                    
                    while (it.hasNext()) {
                        Map.Entry<Future<?>, SingleNodeJob> e = it.next();
                        SingleNodeJob job = e.getValue(); 
                        if (job.m_monitor.isCanceled()) {
                            e.getKey().cancel(false);
                            job.m_manager.cancelExecutionAfterNode(
                                    job.m_container.getID());
                            it.remove();
                        } else if (e.getKey().isDone()) {
                            it.remove();
                        }
                    }
                } catch (InterruptedException ex) { /* empty */ }
            }            
        }
        
        /**
         * Adds a job that the cancel checker should watch for.
         * @param job the job
         * @param future the future for the job
         */
        public void addJob(final SingleNodeJob job, final Future<?> future) {
            m_nodeMap.put(future, job);
        }
    }
    
    private static final CancelChecker CANCEL_CHECKER = new CancelChecker();
    
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
    @Override
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
        Future<?> future = m_container.startExecution(nodeProgress);
        CANCEL_CHECKER.addJob(this, future);        
        
        // dummy progress changed
        progressChanged(0.00, "running...");
        try {
            future.get();
        } catch (Exception ex) {
            // do nothing
        }

        nodeProgress.removeProgressListener(this);

        monitor.done();
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
