/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.workflow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.locks.ReentrantLock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.testing.node.blocking.BlockingRepository;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class TestCanXYZResponeTimeBug3285 extends WorkflowTestCase {

    /** timeout for tests. */
    //  On Windows: Tests on workflow took too long (542 ms but limit at 500)"
    private static final long MAX_TIME_MS = 800L;
    private static final String LOCK_ID = "myLock";
    private NodeID m_dataGen1;
    private NodeID m_firstSplitter2;
    private NodeID m_failInExec99;
    private NodeID m_lockProgrammatically100;
    private NodeID m_lastJoin97;
    private NodeID m_tableView98;

    @Before
    public void setUp() throws Exception {
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

    @Test
    public void testCanXYZ() throws Exception {
        checkState(m_dataGen1, InternalNodeContainerState.CONFIGURED);
        checkState(m_tableView98, InternalNodeContainerState.CONFIGURED);
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

    @Test
    public void testCanXYZAfterExecute() throws Exception {
        executeAllAndWait();
        checkState(m_dataGen1, InternalNodeContainerState.EXECUTED);
        checkState(m_tableView98, InternalNodeContainerState.EXECUTED);
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

    @Test
    public void testShutdownAfterExecute() throws Exception {
        executeAllAndWait();
        checkState(m_dataGen1, InternalNodeContainerState.EXECUTED);
        checkState(m_tableView98, InternalNodeContainerState.EXECUTED);
        long time = System.currentTimeMillis();
        WorkflowManager m = getManager();
        m.shutdown();
        time = System.currentTimeMillis() - time;
        assertTrue(String.format("Tests on workflow took too long (%d ms but limit at %d)", time, MAX_TIME_MS), time <= MAX_TIME_MS);
    }

    @Test
    public void testShutdownBeforeExecute() throws Exception {
        long time = System.currentTimeMillis();
        WorkflowManager m = getManager();
        m.shutdown();
        time = System.currentTimeMillis() - time;
        assertTrue(String.format("Tests on workflow took too long (%d ms but limit at %d)", time, MAX_TIME_MS), time <= MAX_TIME_MS);
    }

    @Test
    public void testCanXYZWhileStartIsExecuting() throws Exception {
        deleteConnection(m_firstSplitter2, 1);
        WorkflowManager m = getManager();
        m.addConnection(m_dataGen1, 1, m_lockProgrammatically100, 1);
        m.addConnection(m_lockProgrammatically100, 1, m_firstSplitter2, 1);
        checkState(m_dataGen1, InternalNodeContainerState.CONFIGURED);
        checkState(m_tableView98, InternalNodeContainerState.CONFIGURED);
        ReentrantLock execLock = BlockingRepository.get(LOCK_ID);
        execLock.lock();
        try {
            m.executeUpToHere(m_tableView98);
            // can't tell about the data generator, but the remaining three
            // should be in some executing state
            checkState(m_tableView98, InternalNodeContainerState.CONFIGURED_MARKEDFOREXEC);
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

    @Test
    public void testCanXYZAfterStartHasFailed() throws Exception {
        deleteConnection(m_firstSplitter2, 1);
        WorkflowManager m = getManager();
        m.addConnection(m_dataGen1, 1, m_failInExec99, 1);
        m.addConnection(m_failInExec99, 1, m_firstSplitter2, 1);
        checkState(m_dataGen1, InternalNodeContainerState.CONFIGURED);
        checkState(m_tableView98, InternalNodeContainerState.CONFIGURED);
        long time = System.currentTimeMillis();
        m.executeUpToHere(m_tableView98);
        waitWhileNodeInExecution(m_tableView98);
        waitWhileNodeInExecution(m_failInExec99);
        checkState(m_tableView98, InternalNodeContainerState.CONFIGURED);
        checkState(m_dataGen1, InternalNodeContainerState.EXECUTED);
        time = System.currentTimeMillis() - time;
        assertTrue(String.format("Tests on workflow took too long (%d ms but limit at %d)", time, MAX_TIME_MS), time <= MAX_TIME_MS);
    }

    /** {@inheritDoc} */
    @Override
    @After
    public void tearDown() throws Exception {
        BlockingRepository.remove(LOCK_ID);
        super.tearDown();
    }

}
