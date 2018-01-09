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
import static org.junit.Assert.assertTrue;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED_MARKEDFOREXEC;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTING;
import static org.knime.core.node.workflow.InternalNodeContainerState.IDLE;
import static org.knime.core.node.workflow.InternalNodeContainerState.UNCONFIGURED_MARKEDFOREXEC;

import java.util.concurrent.locks.ReentrantLock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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
    private NodeID m_subnodeOutput_12_13;
    private NodeID m_dataGenInner_12_14;
    private NodeID m_blockInner_12_10;

    @Before
    public void setUp() throws Exception {
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
        m_subnodeOutput_12_13 = new NodeID(subnodeWFM, 13);
        m_blockInner_12_10 = new NodeID(subnodeWFM, 10);
        m_dataGenInner_12_14 = new NodeID(subnodeWFM, 14);
    }

    @Test
    public void testInit() throws Exception {
        checkStateOfMany(CONFIGURED, m_dataGen1, m_javaEditInput2);
        checkStateOfMany(IDLE, m_subnode12, m_javaEditOutput6, m_javaEditInner_12_9, m_blockOuter8, m_blockInner_12_10);
    }

    @Test
    public void testExecuteAllAndResetStart() throws Exception {
        executeAllAndWait();
        checkStateOfMany(EXECUTED, m_dataGen1, m_javaEditInput2, m_subnode12,
            m_javaEditOutput6, m_javaEditInner_12_9, m_blockOuter8, m_blockInner_12_10);
        reset(m_javaEditInput2);
        checkState(m_javaEditInput2, CONFIGURED);
        checkStateOfMany(IDLE, m_subnode12, m_blockInner_12_10, m_blockOuter8, m_dataGenInner_12_14);
    }

    @Test
    public void testExecuteOneEndAndReset() throws Exception {
        executeAndWait(m_tableView7);
        checkStateOfMany(EXECUTED, m_subnode12, m_tableView7, m_blockOuter8, m_blockInner_12_10, m_subnode12);
        checkState(m_javaEditOutput6, CONFIGURED);
    }

    @Test
    public void testBlockInnerAndExecuteEnd() throws Exception {
        WorkflowManager m = getManager();
        ReentrantLock innerLock = BlockingRepository.get(INNER_LOCK_ID);
        ReentrantLock outerLock = BlockingRepository.get(OUTER_LOCK_ID);
        innerLock.lock();
        outerLock.lock();
        try {
            m.executeUpToHere(m_tableView7);
            InternalNodeContainerState subnodeNodeState = findNodeContainer(m_subnode12).getInternalState();
            assertTrue("should be in-execution: " + subnodeNodeState, subnodeNodeState.isExecutionInProgress());

            final SubNodeContainer subNC = m.getNodeContainer(m_subnode12, SubNodeContainer.class, true);
            final NodeContainer snOutputNode = findNodeContainer(m_subnodeOutput_12_13);

            waitWhile(snOutputNode, new Hold() {
                @Override
                protected boolean shouldHold() {
                    return !snOutputNode.getNodeContainerState().isExecuted();
                }
            });
            checkState(snOutputNode, EXECUTED);
            checkState(m_subnode12, EXECUTING);

            checkState(m_dataGen1, EXECUTED);
            checkState(m_tableView7, CONFIGURED_MARKEDFOREXEC, UNCONFIGURED_MARKEDFOREXEC);

            InternalNodeContainerState blockerNodeState = findNodeContainer(m_blockInner_12_10).getInternalState();
            assertTrue("should be in-execution: " + blockerNodeState, blockerNodeState.isExecutionInProgress());

            innerLock.unlock();
            waitWhileNodeInExecution(m_subnode12);

            checkState(m_subnode12, EXECUTED);

            checkState(m_tableView7, CONFIGURED_MARKEDFOREXEC, UNCONFIGURED_MARKEDFOREXEC);

            assertFalse(m.canAddConnection(m_dataGen1, 2, m_blockOuter8, 1));
            assertFalse(subNC.canResetContainedNodes());
            assertFalse(subNC.getWorkflowManager().canResetNode(m_blockInner_12_10));
            assertFalse(subNC.getWorkflowManager().canRemoveConnection(findInConnection(m_blockInner_12_10, 1)));

            assertNotNull(subNC.getOutPort(2).getPortObject());
            assertFalse(m.canRemoveConnection(m.getIncomingConnectionFor(m_tableView7, 1)));

            outerLock.unlock();
            waitWhileInExecution();

            checkStateOfMany(EXECUTED, m_tableView7, m_subnode12);

            reset(m_dataGen1);
            checkStateOfMany(IDLE, m_subnode12, m_blockInner_12_10, m_blockOuter8, m_dataGenInner_12_14);
        } finally {
            if (innerLock.isHeldByCurrentThread()) {
                innerLock.unlock();
            }
            if (outerLock.isHeldByCurrentThread()) {
                outerLock.unlock();
            }
        }
    }

    @Test
    public void testShutdownBeforeExecute() throws Exception {
        long time = System.currentTimeMillis();
        WorkflowManager m = getManager();
        m.shutdown();
        time = System.currentTimeMillis() - time;
        assertTrue(String.format("Tests on workflow took too long (%d ms but limit at %d)", time, MAX_TIME_MS), time <= MAX_TIME_MS);
    }

    /** {@inheritDoc} */
    @Override
    @After
    public void tearDown() throws Exception {
        BlockingRepository.remove(INNER_LOCK_ID);
        BlockingRepository.remove(OUTER_LOCK_ID);
        super.tearDown();
    }

}
