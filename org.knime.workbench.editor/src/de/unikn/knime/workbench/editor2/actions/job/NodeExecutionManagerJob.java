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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.workflow.NodeContainer;
import de.unikn.knime.core.node.workflow.WorkflowManager;
import de.unikn.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * This implements an Eclipse job for managing (sub) jobs that execute node(s)
 * inside a <code>WorkflowManager</code>.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NodeExecutionManagerJob extends Job {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(NodeExecutionManagerJob.class);

    private WorkflowManager m_manager;

    private NodeContainerEditPart[] m_parts;

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
     * Creates a job that executes all nodes.
     * 
     * @param manager The manager that hosts the nodes
     */
    public NodeExecutionManagerJob(final WorkflowManager manager) {
        super("Execution Manager");
        m_manager = manager;
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
            // if there are no specified parts, execute all nodes
            if (m_parts == null) {
                LOGGER.debug("Execute all nodes.");
                m_manager.executeAll(false);
            } else {

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
            }

            m_manager.waitUntilFinished();

        } finally {
            LOGGER.info("Execution finished");
        }

        return state;
    }
}
