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
 *   09.05.2005 (mb): created
 *   12.01.2006 (mb): clean up for code review
 */
package de.unikn.knime.core.node.workflow;

import de.unikn.knime.core.node.DefaultNodeProgressMonitor;

/** Convenience Class that supports Execution of a Workflow stored within
 * a <code>WorkflowManager</code>.
 * Currently it simply starts a new Thread for every executable Node and
 * returns when the Workflow is done.
 * 
 * @author M. Berthold, University of Konstanz
 */
public class WorkflowExecutor implements WorkflowListener {

    private final WorkflowManager m_flowMgr;
    private final Object m_executionInProgress = new Object();
    
    /** Create executor class and register as listener for events.
     * 
     * @param mgr WorkflowManager to be executed
     */
    public WorkflowExecutor(final WorkflowManager mgr) {
        m_flowMgr = mgr;
        m_flowMgr.addListener(this);
    }

    /** Execute all nodes in workflow - return when all nodes
     * are executed (or at least Workflow claims to be done).
     */
    public void executeAll() {
        m_flowMgr.prepareForExecAllNodes();
        executeWorkflow();
    }
    
    /** Execute all nodes in workflow leading to a certain node.
     * Return when all nodes are executed (or at least Workflow
     * claims to be done).
     * 
     * @param nodeID id of node to be executed.
     */
    public void executeUpToNode(final int nodeID) {
        m_flowMgr.prepareForExecUpToNode(nodeID);
        executeWorkflow();
    }
    
    /* start execution of an already prepare workflow (i.e. nodes that are
     * to be executed are marked (prepared...) accordingly).
     */    
    private void executeWorkflow() {        
        synchronized (m_executionInProgress) {
            createThreads();
            try {
                m_executionInProgress.wait();
            } catch (InterruptedException ie) {
                assert false;  // shouldn't happen
            }
        } 
    }
    
    /* create threads for all currently executable nodes
     */
    private void createThreads() {
        NodeContainer nextNode = m_flowMgr.getNextExecutableNode();
        while (nextNode != null) {
            nextNode.startExecution(new DefaultNodeProgressMonitor());
            nextNode = m_flowMgr.getNextExecutableNode();
        }
    }
    
    /** Create new threads when workflow has changed, stop execution when
     * workflow is done.
     * 
     * @param event the WorkflowEvent to be handled.
     */
    public void workflowChanged(final WorkflowEvent event) {
        if (event.getEventType() == WorkflowEvent.EXEC_POOL_CHANGED) {
            createThreads();
        }
        if (event.getEventType() == WorkflowEvent.EXEC_POOL_DONE) {
            synchronized (m_executionInProgress) {
                m_executionInProgress.notifyAll();
            } 
        }
    }

}
