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
 *   07.06.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.editor2.actions.job;

import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.NodeProgressMonitor;
import de.unikn.knime.core.node.workflow.NodeContainer;
import de.unikn.knime.core.node.workflow.WorkflowEvent;
import de.unikn.knime.core.node.workflow.WorkflowListener;
import de.unikn.knime.core.node.workflow.WorkflowManager;
import de.unikn.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * This implements an Eclipse job for managing (sub) jobs that execute node(s)
 * inside a <code>WorkflowManager</code>.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NodeExecutionManagerJob extends Job implements WorkflowListener {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(NodeExecutionManagerJob.class);

    private WorkflowManager m_manager;

    private NodeContainerEditPart[] m_parts;

    private final ArrayList<Job> m_scheduledJobs = new ArrayList<Job>();

    /**
     * @param manager The manager that hosts the nodes
     * @param nodeParts Controller edit parts that manage the nodes that should
     *            be executed.
     */
    public NodeExecutionManagerJob(final WorkflowManager manager,
            final NodeContainerEditPart[] nodeParts) {
        super("Execution Manager");
        m_manager = manager;
        m_parts = nodeParts;
    }

    /**
     * @see org.eclipse.core.runtime.jobs.Job
     *      #run(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected IStatus run(final IProgressMonitor monitor) {
        // state to return to user (OK, cancel)
        IStatus state = Status.OK_STATUS;

        try {
            // we must register on the manager to get lifecycle events
            m_manager.addListener(this);
            
            // list to remember the sub jobs
            m_scheduledJobs.clear();    
    
            for (int i = 0; i < m_parts.length; i++) {
                // This is the node up to which the flow should be executed.
                NodeContainer container = m_parts[i].getNodeContainer();
                // check if the part is already locked execution
                if (m_parts[i].isLocked()) {
                    LOGGER.warn("Node is (already?) locked, skipping...");
                    continue;
                }
    
                // create a monitor object
                LOGGER.info("executing up to node #" + container.getID());
                try {
                    m_manager.executeUpToNode(container.getID(), false);
                } catch (IllegalArgumentException ex) {
                    // ignore it
                    // if the user selects more nodes in the workflow he
                    // can selected "red" nodes and try to execute them
                    // this fails of course
                }
            }
    
            m_manager.waitUntilFinished();
            
            // We must wait for all jobs to end
            for (int i = 0; i < m_scheduledJobs.size(); i++) {
                Job job = m_scheduledJobs.get(i);
                LOGGER.debug("Waiting for remaining job to finish: "
                        + job.getName());
                try {
                    job.join();
                } catch (InterruptedException e) {
                    LOGGER.error("Could not join job: " + job.getName(), e);
                }
            }
        } finally {    
            LOGGER.info("Execution finished");
            m_manager.removeListener(this);    
        }

        return state;
    }

    /**
     * Creates new jobs if there's another node ready to be executed or set the
     * 'finished' flag.
     * 
     * @see de.unikn.knime.core.node.workflow.WorkflowListener
     *      #workflowChanged(de.unikn.knime.core.node.workflow.WorkflowEvent)
     */
    public void workflowChanged(final WorkflowEvent event) {
        if (event instanceof WorkflowEvent.NodeStarted) {
            NodeContainer nc = (NodeContainer) event.getOldValue();
            NodeProgressMonitor pm = (NodeProgressMonitor) event.getNewValue();
            
            DummyNodeJob job = new DummyNodeJob(nc.getNameWithID(), pm, nc);
            job.schedule();
        }
    }
}
