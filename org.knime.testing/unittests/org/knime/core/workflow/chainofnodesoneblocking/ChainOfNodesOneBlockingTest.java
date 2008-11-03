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
package org.knime.core.workflow.chainofnodesoneblocking;

import java.util.concurrent.locks.ReentrantLock;

import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.workflow.WorkflowTestCase;
import org.knime.testing.node.blocking.BlockingRepository;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class ChainOfNodesOneBlockingTest extends WorkflowTestCase {
    
    private static final String LOCK_ID = "chainofnodesoneblocking";
    private NodeID m_dataGen;
    private NodeID m_blocker;
    private NodeID m_colFilter;
    private NodeID m_tblView;
    
    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // the id is used here and in the workflow (part of the settings)
        BlockingRepository.put(LOCK_ID, new ReentrantLock());
        NodeID baseID = loadAndSetWorkflow();
        m_dataGen = new NodeID(baseID, 1);
        m_blocker = new NodeID(baseID, 2);
        m_colFilter = new NodeID(baseID, 3);
        m_tblView = new NodeID(baseID, 4);
    }
    
    public void testExecuteNoBlocking() throws Exception {
        checkState(m_dataGen, State.CONFIGURED);
        checkState(m_blocker, State.CONFIGURED);
        checkState(m_colFilter, State.CONFIGURED);
        checkState(m_tblView, State.CONFIGURED);
        
        executeAndWait(m_tblView);
        checkState(m_tblView, State.EXECUTED);
        checkState(getManager(), State.EXECUTED);
    }
    
    public void testBlockingStates() throws Exception {
        WorkflowManager m = getManager();
        ReentrantLock lock = BlockingRepository.get(LOCK_ID);
        lock.lock();
        try {
            m.executeUpToHere(m_tblView);
            // can't tell about the data generator, but the remaining three
            // should be in some executing state
            checkState(m_blocker, State.MARKEDFOREXEC, State.EXECUTING);
            checkState(m_colFilter, State.MARKEDFOREXEC);
            checkState(m_tblView, State.MARKEDFOREXEC);
            // TODO enable later
//            assertTrue(m.getState().executionInProgress());
        } finally {
            lock.unlock();
        }
        // give the workflow manager time to wrap up
        executeAndWait(m_tblView);
    }
    
    /** {@inheritDoc} */
    @Override
    protected void tearDown() throws Exception {
        BlockingRepository.remove(LOCK_ID);
        super.tearDown();
    }
    
}
