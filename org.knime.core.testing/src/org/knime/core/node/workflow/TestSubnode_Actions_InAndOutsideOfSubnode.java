/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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

import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED_MARKEDFOREXEC;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;
import static org.knime.core.node.workflow.InternalNodeContainerState.IDLE;
import static org.knime.core.node.workflow.InternalNodeContainerState.UNCONFIGURED_MARKEDFOREXEC;

import java.util.concurrent.locks.ReentrantLock;

import org.knime.testing.node.blocking.BlockingRepository;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class TestSubnode_Actions_InAndOutsideOfSubnode extends WorkflowTestCase {

    /**  */
    private static final long MAX_TIME_MS = 250L;
    private static final String INNER_LOCK_ID = "inner_lock";
    private static final String OUTER_LOCK_ID = "outer_lock";
    private NodeID m_dataGen1;
    private NodeID m_javaEditInput2;
    private NodeID m_javaEditOutput6;
    private NodeID m_tableView7;
    private NodeID m_blockOuter8;
    private NodeID m_subnode12;
    private NodeID m_javaEditInner_12_9;
    private NodeID m_subnodeInput_12_12;
    private NodeID m_subnodeOutput_12_13;
    private NodeID m_blockInner_12_10;

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // the id is used here and in the workflow (part of the settings)
        BlockingRepository.put(INNER_LOCK_ID, new ReentrantLock());
        BlockingRepository.put(OUTER_LOCK_ID, new ReentrantLock());
        NodeID baseID = loadAndSetWorkflow();
        m_dataGen1 = new NodeID(baseID, 1);
        m_javaEditInput2 = new NodeID(baseID, 2);
        m_javaEditOutput6 = new NodeID(baseID, 6);
        m_tableView7 = new NodeID(baseID, 7);
        m_blockOuter8 = new NodeID(baseID, 8);
        m_subnode12 = new NodeID(baseID, 12);
        NodeID subnodeWFM = new NodeID(m_subnode12, 0);
        m_javaEditInner_12_9 = new NodeID(subnodeWFM, 9);
        m_subnodeInput_12_12 = new NodeID(subnodeWFM, 12);
        m_subnodeOutput_12_13 = new NodeID(subnodeWFM, 13);
        m_blockInner_12_10 = new NodeID(subnodeWFM, 10);
    }

    public void testInit() throws Exception {
        checkStateOfMany(CONFIGURED, m_dataGen1, m_javaEditInput2);
        checkStateOfMany(IDLE, m_subnode12, m_javaEditOutput6, m_javaEditInner_12_9, m_blockOuter8, m_blockInner_12_10);
    }

    public void testExecuteAllAndResetStart() throws Exception {
        executeAllAndWait();
        checkStateOfMany(EXECUTED, m_dataGen1, m_javaEditInput2, m_subnode12,
            m_javaEditOutput6, m_javaEditInner_12_9, m_blockOuter8, m_blockInner_12_10);
        reset(m_javaEditInput2);
        checkState(m_javaEditInput2, CONFIGURED);
        checkStateOfMany(IDLE, m_subnode12, m_blockInner_12_10, m_blockOuter8);
    }

    public void testExecuteOneEndAndReset() throws Exception {
        executeAndWait(m_tableView7);
        checkStateOfMany(EXECUTED, m_subnode12, m_tableView7, m_blockOuter8, m_blockInner_12_10, m_subnode12);
        checkState(m_javaEditOutput6, CONFIGURED);
    }

    public void testBlockInnerAndExecuteEnd() throws Exception {
        WorkflowManager m = getManager();
        ReentrantLock execLock = BlockingRepository.get(INNER_LOCK_ID);
        execLock.lock();
        try {
            m.executeUpToHere(m_tableView7);
            InternalNodeContainerState subnodeOutputNodeState = findNodeContainer(m_subnodeOutput_12_13).getInternalState();
            assertTrue("should be in-execution: " + subnodeOutputNodeState, subnodeOutputNodeState.isExecutionInProgress());

            waitWhileNodeInExecution(m_subnodeOutput_12_13);
            checkState(m_tableView7, CONFIGURED_MARKEDFOREXEC, UNCONFIGURED_MARKEDFOREXEC);
            checkState(m_subnodeOutput_12_13, EXECUTED);
            InternalNodeContainerState blockerNodeState = findNodeContainer(m_blockInner_12_10).getInternalState();
            assertTrue("should be in-execution: " + blockerNodeState, blockerNodeState.isExecutionInProgress());
            checkState(m_dataGen1, EXECUTED);
            assertFalse(m.canAddConnection(m_dataGen1, 2, m_blockOuter8, 1));

            assertFalse(((SubNodeContainer)findNodeContainer(m_subnode12)).canResetContainedNodes());
            final NodeContainer subNC = m.getNodeContainer(m_subnode12);
            assertNull(subNC.getOutPort(2).getPortObject());
            assertFalse(m.canRemoveConnection(m.getIncomingConnectionFor(m_tableView7, 1)));
            m.cancelExecution(subNC);
            checkStateOfMany(CONFIGURED, m_tableView7, m_blockInner_12_10, m_subnode12);
            m.executeUpToHere(m_tableView7);
        } finally {
            execLock.unlock();
        }
        waitWhileInExecution();
        checkState(m_tableView7, EXECUTED);
    }

    public void testShutdownBeforeExecute() throws Exception {
        long time = System.currentTimeMillis();
        WorkflowManager m = getManager();
        m.shutdown();
        time = System.currentTimeMillis() - time;
        assertTrue(String.format("Tests on workflow took too long (%d ms but limit at %d)", time, MAX_TIME_MS), time <= MAX_TIME_MS);
    }

    public void testCanXYZWhileStartIsExecuting() throws Exception {
        deleteConnection(m_javaEditInput2, 1);
        WorkflowManager m = getManager();
        m.addConnection(m_dataGen1, 1, m_javaEditOutput6, 1);
        m.addConnection(m_javaEditOutput6, 1, m_javaEditInput2, 1);
        checkState(m_dataGen1, InternalNodeContainerState.CONFIGURED);
        checkState(m_tableView7, InternalNodeContainerState.CONFIGURED);
        ReentrantLock execLock = BlockingRepository.get(INNER_LOCK_ID);
        execLock.lock();
        try {
            m.executeUpToHere(m_tableView7);
            // can't tell about the data generator, but the remaining three
            // should be in some executing state
            checkState(m_tableView7, InternalNodeContainerState.CONFIGURED_MARKEDFOREXEC);
            long time = System.currentTimeMillis();
            assertFalse(m.canAddConnection(m_dataGen1, 1, m_tableView7, 1));
            assertFalse(m.canExecuteNode(m_dataGen1));
            assertFalse(m.canExecuteNode(m_tableView7));
            assertFalse(m.canResetNode(m_dataGen1));
            assertFalse(m.canResetNode(m_tableView7));
            assertFalse(m.canRemoveConnection(m.getIncomingConnectionFor(m_tableView7, 1)));
            assertFalse(m.canRemoveConnection(m.getIncomingConnectionFor(m_javaEditInput2, 1)));
            assertFalse(m.canExecuteNode(m_tableView7));
            assertNotNull(m.canCollapseNodesIntoMetaNode(new NodeID[] {m_dataGen1, m_javaEditInput2}));
            assertFalse(m.canRemoveNode(m_dataGen1));
            assertFalse(m.canRemoveNode(m_javaEditInput2));
            m.cancelExecution(m.getNodeContainer(m_tableView7));
            m.cancelExecution(m.getNodeContainer(m_javaEditInput2));
            time = System.currentTimeMillis() - time;
            assertTrue(String.format("Tests on workflow took too long (%d ms but limit at %d)", time, MAX_TIME_MS), time <= MAX_TIME_MS);
        } finally {
            execLock.unlock();
        }
        waitWhileInExecution();
    }

    public void testCanXYZAfterStartHasFailed() throws Exception {
        deleteConnection(m_javaEditInput2, 1);
        WorkflowManager m = getManager();
        m.addConnection(m_dataGen1, 1, m_subnode12, 1);
        m.addConnection(m_subnode12, 1, m_javaEditInput2, 1);
        checkState(m_dataGen1, InternalNodeContainerState.CONFIGURED);
        checkState(m_tableView7, InternalNodeContainerState.CONFIGURED);
        long time = System.currentTimeMillis();
        m.executeUpToHere(m_tableView7);
        waitWhileNodeInExecution(m_tableView7);
        waitWhileNodeInExecution(m_subnode12);
        checkState(m_tableView7, InternalNodeContainerState.CONFIGURED);
        checkState(m_dataGen1, InternalNodeContainerState.EXECUTED);
        time = System.currentTimeMillis() - time;
        assertTrue(String.format("Tests on workflow took too long (%d ms but limit at %d)", time, MAX_TIME_MS), time <= MAX_TIME_MS);
    }

    /** {@inheritDoc} */
    @Override
    protected void tearDown() throws Exception {
        BlockingRepository.remove(INNER_LOCK_ID);
        BlockingRepository.remove(OUTER_LOCK_ID);
        super.tearDown();
    }

}
