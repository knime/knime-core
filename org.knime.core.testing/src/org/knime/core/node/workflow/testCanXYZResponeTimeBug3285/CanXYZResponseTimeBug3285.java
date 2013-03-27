/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2013
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
package org.knime.core.node.workflow.testCanXYZResponeTimeBug3285;

import java.util.concurrent.locks.ReentrantLock;

import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowTestCase;
import org.knime.testing.node.blocking.BlockingRepository;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class CanXYZResponseTimeBug3285 extends WorkflowTestCase {

    /**  */
    private static final long MAX_TIME_MS = 250L;
    private static final String LOCK_ID = "myLock";
    private NodeID m_dataGen1;
    private NodeID m_firstSplitter2;
    private NodeID m_failInExec99;
    private NodeID m_lockProgrammatically100;
    private NodeID m_lastJoin97;
    private NodeID m_tableView98;

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // the id is used here and in the workflow (part of the settings)
        BlockingRepository.put(LOCK_ID, new ReentrantLock());
        NodeID baseID = loadAndSetWorkflow();
        m_dataGen1 = new NodeID(baseID, 1);
        m_firstSplitter2 = new NodeID(baseID, 2);
        m_failInExec99 = new NodeID(baseID, 99);
        m_lockProgrammatically100 = new NodeID(baseID, 100);
        m_lastJoin97 = new NodeID(baseID, 97);
        m_tableView98 = new NodeID(baseID, 98);
    }

    public void testCanXYZ() throws Exception {
        checkState(m_dataGen1, State.CONFIGURED);
        checkState(m_tableView98, State.CONFIGURED);
        long time = System.currentTimeMillis();
        WorkflowManager m = getManager();
        assertTrue(m.canAddConnection(m_dataGen1, 1, m_tableView98, 1));
        assertTrue(m.canExecuteNode(m_dataGen1));
        assertTrue(m.canExecuteNode(m_tableView98));
        assertFalse(m.canResetNode(m_dataGen1));
        assertFalse(m.canResetNode(m_tableView98));
        assertTrue(m.canRemoveConnection(m.getIncomingConnectionFor(m_tableView98, 1)));
        assertTrue(m.canRemoveConnection(m.getIncomingConnectionFor(m_firstSplitter2, 1)));
        assertTrue(m.canExecuteNode(m_tableView98));
        assertNull(m.canCollapseNodesIntoMetaNode(new NodeID[] {m_dataGen1, m_firstSplitter2}));
        assertTrue(m.canRemoveNode(m_dataGen1));
        assertTrue(m.canRemoveNode(m_firstSplitter2));
        assertTrue(m.canSetJobManager(m_dataGen1));
        time = System.currentTimeMillis() - time;
        assertTrue(String.format("Tests on workflow took too long (%d ms but limit at %d)", time, MAX_TIME_MS), time <= MAX_TIME_MS);
    }

    public void testCanXYZAfterExecute() throws Exception {
        executeAllAndWait();
        checkState(m_dataGen1, State.EXECUTED);
        checkState(m_tableView98, State.EXECUTED);
        long time = System.currentTimeMillis();
        WorkflowManager m = getManager();
        assertTrue(m.canAddConnection(m_dataGen1, 1, m_tableView98, 1));
        assertFalse(m.canExecuteNode(m_dataGen1));
        assertFalse(m.canExecuteNode(m_tableView98));
        assertTrue(m.canResetNode(m_dataGen1));
        assertTrue(m.canResetNode(m_tableView98));
        assertTrue(m.canRemoveConnection(m.getIncomingConnectionFor(m_tableView98, 1)));
        assertTrue(m.canRemoveConnection(m.getIncomingConnectionFor(m_firstSplitter2, 1)));
        assertFalse(m.canExecuteNode(m_tableView98));
        assertNotNull(m.canCollapseNodesIntoMetaNode(new NodeID[] {m_dataGen1, m_firstSplitter2}));
        assertTrue(m.canRemoveNode(m_dataGen1));
        assertTrue(m.canRemoveNode(m_firstSplitter2));
        assertTrue(m.canSetJobManager(m_dataGen1));
        time = System.currentTimeMillis() - time;
        assertTrue(String.format("Tests on workflow took too long (%d ms but limit at %d)", time, MAX_TIME_MS), time <= MAX_TIME_MS);
    }

    public void testShutdownAfterExecute() throws Exception {
        executeAllAndWait();
        checkState(m_dataGen1, State.EXECUTED);
        checkState(m_tableView98, State.EXECUTED);
        long time = System.currentTimeMillis();
        WorkflowManager m = getManager();
        m.shutdown();
        time = System.currentTimeMillis() - time;
        assertTrue(String.format("Tests on workflow took too long (%d ms but limit at %d)", time, MAX_TIME_MS), time <= MAX_TIME_MS);
    }

    public void testShutdownBeforeExecute() throws Exception {
        long time = System.currentTimeMillis();
        WorkflowManager m = getManager();
        m.shutdown();
        time = System.currentTimeMillis() - time;
        assertTrue(String.format("Tests on workflow took too long (%d ms but limit at %d)", time, MAX_TIME_MS), time <= MAX_TIME_MS);
    }

    public void testCanXYZWhileStartIsExecuting() throws Exception {
        deleteConnection(m_firstSplitter2, 1);
        WorkflowManager m = getManager();
        m.addConnection(m_dataGen1, 1, m_lockProgrammatically100, 1);
        m.addConnection(m_lockProgrammatically100, 1, m_firstSplitter2, 1);
        checkState(m_dataGen1, State.CONFIGURED);
        checkState(m_tableView98, State.CONFIGURED);
        ReentrantLock execLock = BlockingRepository.get(LOCK_ID);
        execLock.lock();
        try {
            m.executeUpToHere(m_tableView98);
            // can't tell about the data generator, but the remaining three
            // should be in some executing state
            checkState(m_tableView98, State.MARKEDFOREXEC);
            long time = System.currentTimeMillis();
            assertFalse(m.canAddConnection(m_dataGen1, 1, m_tableView98, 1));
            assertFalse(m.canExecuteNode(m_dataGen1));
            assertFalse(m.canExecuteNode(m_tableView98));
            assertFalse(m.canResetNode(m_dataGen1));
            assertFalse(m.canResetNode(m_tableView98));
            assertFalse(m.canRemoveConnection(m.getIncomingConnectionFor(m_tableView98, 1)));
            assertFalse(m.canRemoveConnection(m.getIncomingConnectionFor(m_firstSplitter2, 1)));
            assertFalse(m.canExecuteNode(m_tableView98));
            assertNotNull(m.canCollapseNodesIntoMetaNode(new NodeID[] {m_dataGen1, m_firstSplitter2}));
            assertFalse(m.canRemoveNode(m_dataGen1));
            assertFalse(m.canRemoveNode(m_firstSplitter2));
            m.cancelExecution(m.getNodeContainer(m_tableView98));
            m.cancelExecution(m.getNodeContainer(m_firstSplitter2));
            time = System.currentTimeMillis() - time;
            assertTrue(String.format("Tests on workflow took too long (%d ms but limit at %d)", time, MAX_TIME_MS), time <= MAX_TIME_MS);
        } finally {
            execLock.unlock();
        }
        waitWhileInExecution();
    }

    public void testCanXYZAfterStartHasFailed() throws Exception {
        deleteConnection(m_firstSplitter2, 1);
        WorkflowManager m = getManager();
        m.addConnection(m_dataGen1, 1, m_failInExec99, 1);
        m.addConnection(m_failInExec99, 1, m_firstSplitter2, 1);
        checkState(m_dataGen1, State.CONFIGURED);
        checkState(m_tableView98, State.CONFIGURED);
        long time = System.currentTimeMillis();
        m.executeUpToHere(m_tableView98);
        waitWhileNodeInExecution(m_tableView98);
        waitWhileNodeInExecution(m_failInExec99);
        checkState(m_tableView98, State.CONFIGURED);
        checkState(m_dataGen1, State.EXECUTED);
        time = System.currentTimeMillis() - time;
        assertTrue(String.format("Tests on workflow took too long (%d ms but limit at %d)", time, MAX_TIME_MS), time <= MAX_TIME_MS);
    }

    /** {@inheritDoc} */
    @Override
    protected void tearDown() throws Exception {
        BlockingRepository.remove(LOCK_ID);
        super.tearDown();
    }

}
