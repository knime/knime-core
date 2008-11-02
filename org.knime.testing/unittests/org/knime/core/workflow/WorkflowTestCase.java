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
 *   01.11.2008 (wiswedel): created
 */
package org.knime.core.workflow;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import junit.framework.TestCase;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.NodeContainer.State;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class WorkflowTestCase extends TestCase {
    
    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());
    
    private WorkflowManager m_manager;
    private WorkflowListener m_workflowListener;
    private ReentrantLock m_lock;
    private Condition m_workflowStableCondition;
    
    /**
     * 
     */
    public WorkflowTestCase() {
        m_lock = new ReentrantLock();
        m_workflowStableCondition = m_lock.newCondition();
        m_workflowListener = new WorkflowListener() {
            @Override
            public void workflowChanged(WorkflowEvent event) {
                m_lock.lock();
                try {
                    m_logger.info("Sending signal: " + event);
                    m_workflowStableCondition.signalAll();
                } finally {
                    m_lock.unlock();
                }
            }
        };
    }
    
    /**
     * @param manager the manager to set
     */
    protected void setManager(WorkflowManager manager) {
        if (m_manager != null) {
            m_manager.removeListener(m_workflowListener);
        }
        m_manager = manager;
        if (m_manager != null) {
            m_manager.addListener(m_workflowListener);
        }
    }
    
    /**
     * @return the manager
     */
    protected WorkflowManager getManager() {
        return m_manager;
    }
    
    protected void lock() {
        m_lock.lock();
    }
    
    protected void unlock() {
        m_lock.unlock();
    }
    
    protected void checkState(final NodeID id, 
            final State expected) throws Exception {
        if (m_manager == null) {
            throw new NullPointerException("WorkflowManager not set.");
        }
        NodeContainer nc = m_manager.getNodeContainer(id);
        State actual = nc.getState();
        if (!actual.equals(expected)) {
            String error = "node " + nc.getNameWithID() + " has wrong state; "
            + "expected " + expected + ", actual " + actual + " (dump follows)";
            m_logger.info("Test failed: " + error);
            String toString = m_manager.printNodeSummary(m_manager.getID(), 0);
            BufferedReader r = new BufferedReader(new StringReader(toString));
            String line;
            while ((line = r.readLine()) != null) {
                m_logger.info(line);
            }
            r.close();
            fail(error);
        }
    }
    
    protected void executeAndWait(final NodeID... ids) 
        throws Exception {
        m_lock.lock();
        try {
            getManager().executeUpToHere(ids);
            System.out.println(getManager());
            waitWhileInExecution();
        } finally {
            m_lock.unlock();
        }
    }
    
    protected void waitWhileInExecution() throws InterruptedException {
        waitWhileNodeInExecution(m_manager);
    }

    protected void waitWhileNodeInExecution(final NodeID node) 
        throws InterruptedException {
        waitWhileNodeInExecution(m_manager.getNodeContainer(node));
    }
    
    protected void waitWhileNodeInExecution(final NodeContainer node) 
    throws InterruptedException {
        if (!m_lock.isHeldByCurrentThread()) {
            throw new IllegalStateException(
                    "Ill-posed test case; thread must own lock");
        }
        System.out.println("node " + node.getNameWithID() + " is " + node.getState());
        while (node.getState().executionInProgress()) {
            System.out.println("node " + node.getNameWithID() + " is " + node.getState());
            m_workflowStableCondition.await();
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (m_manager != null) {
            WorkflowManager.ROOT.removeProject(m_manager.getID());
            setManager(null);
        }
    }
    
}
