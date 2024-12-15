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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTING;

import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.testing.node.blocking.BlockingRepository;
import org.knime.testing.node.blocking.BlockingRepository.LockedMethod;

/** 
 * Tests changes made as part of AP-20402: Subnodes Output nodes can only turn executed if all contained nodes are
 * executed. 
 * 
 * @author Bernd Wiswedel, KNIME
 */
public class EnhAP20402_SubnodeEndWaitingForNodes extends WorkflowTestCase {

    private NodeID m_subnode_7;
    private NodeID m_datagen_7_1;
    private NodeID m_fail_7_2;
    private NodeID m_chunkend_7_4;
    private NodeID m_block_7_6;
    private NodeID m_output_7_8;

    private static final String BLOCK_LOCK_ID = "enh-20402-lock";

    @BeforeEach
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_subnode_7 = baseID.createChild(7);
        final var subnodeWFM = baseID.createChild(7).createChild(0);
        m_datagen_7_1 = subnodeWFM.createChild(1);
        m_fail_7_2 = subnodeWFM.createChild(2);
        m_chunkend_7_4 = subnodeWFM.createChild(4);
        m_block_7_6 = subnodeWFM.createChild(6);
        m_output_7_8 = subnodeWFM.createChild(8);
        BlockingRepository.put(BLOCK_LOCK_ID, LockedMethod.EXECUTE, new ReentrantLock());
    }

    @Test
    public void testRegularExecute() throws Exception {
    	removeFailNodeAndExecute();
    	checkState(getManager(), EXECUTED);
    	checkStateOfMany(EXECUTED, m_subnode_7, m_output_7_8);
    }

    @Test
    public void testWorkflowModificationAfterExecute_NodeAdded() throws Exception {
    	final var subnodeWFM = removeFailNodeAndExecute();
    	checkState(getManager(), EXECUTED);
    	subnodeWFM.copyFromAndPasteHere(subnodeWFM, WorkflowCopyContent.builder().setNodeIDs(m_datagen_7_1).build());
    	checkState(m_subnode_7, CONFIGURED); // immediate
    	// state transition may happen asynchronously (which is OK as the subnode itself is reset
    	waitWhile(m_output_7_8, out -> out.getInternalState() != CONFIGURED, 5);
    	checkState(m_output_7_8, CONFIGURED);  
    }

	@Test
    public void testSimpleFailExecute() throws Exception {
    	final var subnodeWFM = getSubnodeWFM();
    	executeAllAndWait();
    	checkState(getManager(), CONFIGURED);
    	checkStateOfMany(EXECUTED, m_datagen_7_1, m_chunkend_7_4, m_block_7_6);
    	checkStateOfMany(CONFIGURED, m_subnode_7, m_output_7_8, m_fail_7_2);
    	final var subnodeMessage = getManager().getNodeContainer(m_subnode_7).getNodeMessage().toStringWithDetails();
    	final var outNodeMessage = subnodeWFM.getNodeContainer(m_output_7_8).getNodeMessage().toStringWithDetails();
		assertThat("Fail message on subnode", subnodeMessage,
				containsString("Contains one node with execution failure (Fail in execution #2)"));
		assertThat("Fail message on subnode output", outNodeMessage,
				containsString("Contains one node with execution failure (Fail in execution #2)"));
    }

    /** Simulates execute on subnode itself. Blocks execution, asserts state of output node. */
    @Test
    public void testBlockWhileExecuting_TriggeredFromOutside() throws Exception {
    	innerTestBlockWhileExecuting(m_subnode_7);
    }

    /** Simulates execute on output node itself - expects execution to be pulled (even if not connected). */
    @Test
    public void testBlockWhileExecuting_TriggeredFromInside() throws Exception {
    	innerTestBlockWhileExecuting(m_output_7_8); // new in AP-20402: execute on output node should start all nodes
    }

	/**
	 * Implementation of {@link #testBlockWhileExecuting_TriggeredFromInside()} and
	 * {@link #testBlockWhileExecuting_TriggeredFromOutside()}
	 */
    private void innerTestBlockWhileExecuting(final NodeID nodeToStart) throws Exception {
    	final var subnodeWFM = getSubnodeWFM();
    	subnodeWFM.removeNode(m_fail_7_2);
    	final var lock = BlockingRepository.get(BLOCK_LOCK_ID, LockedMethod.EXECUTE).orElseThrow();
    	lock.lockInterruptibly();
    	try {
    		executeDontWait(nodeToStart); // either the subnode itself (outside) or the output node (inside)
    		waitWhile(m_chunkend_7_4, nc -> !nc.getInternalState().isExecuted(), /* seconds to wait */ 5);
    		checkState(m_chunkend_7_4, EXECUTED);
    		assertThat("State of blocking node is executing",
    				subnodeWFM.getNodeContainer(m_block_7_6).getInternalState().isExecutionInProgress(), is(true));
    		assertThat("State of end node is executing",
    				subnodeWFM.getNodeContainer(m_output_7_8).getInternalState().isExecutionInProgress(), is(true));
    	} finally {
    		lock.unlock();
    	}
    	waitWhileInExecution();
    	checkStateOfMany(EXECUTED, m_block_7_6, m_subnode_7, m_output_7_8);
    	checkState(getManager(), EXECUTED);
    }

    /**
     * Test nodes added while in execution (should fail entire execution).
     */
    @Test
    public void testNodesAddedWhileInExecution_NodesConfigured() throws Exception {
    	final var subnodeWFM = getSubnodeWFM();
    	subnodeWFM.removeNode(m_fail_7_2);
    	final var lock = BlockingRepository.get(BLOCK_LOCK_ID, LockedMethod.EXECUTE).orElseThrow();
    	lock.lockInterruptibly();
    	final NodeID pastedNodeID;
    	try {
    		executeDontWait(m_output_7_8);
    		waitWhile(m_chunkend_7_4, nc -> !nc.getInternalState().isExecuted(), /* seconds to wait */ 5);
    		checkState(m_chunkend_7_4, EXECUTED);
    		waitWhile(m_output_7_8, nc -> nc.getInternalState() != EXECUTING, 5);
    		assertThat("State of blocking node is executing",
    				subnodeWFM.getNodeContainer(m_block_7_6).getInternalState().isExecutionInProgress(), is(true));
    		assertThat("State of end node is executing",
    				subnodeWFM.getNodeContainer(m_output_7_8).getInternalState().isExecutionInProgress(), is(true));
    		// now insert a new node (whose not been executed/executing)
			final var pastedContent = subnodeWFM.copyFromAndPasteHere(subnodeWFM,
					WorkflowCopyContent.builder().setNodeIDs(m_datagen_7_1).build());
			pastedNodeID = pastedContent.getNodeIDs()[0];
			checkState(pastedNodeID, CONFIGURED);
    	} finally {
    		lock.unlock();
    	}
    	waitWhileInExecution();
    	checkState(m_block_7_6, EXECUTED);
    	checkState(m_output_7_8, CONFIGURED);
    	checkStateOfMany(CONFIGURED, pastedNodeID, m_subnode_7);
		final var outNodeMessage = subnodeWFM.getNodeContainer(m_output_7_8).getNodeMessage().toStringWithDetails();
		assertThat("Fail message on subnode", outNodeMessage, containsString("were not executed"));
    }

    /**
     * Test nodes added while in execution (should fail entire execution).
     */
    @Test
    public void testNodesAddedWhileInExecution_NodesExecuting() throws Exception {
    	final var subnodeWFM = getSubnodeWFM();
    	subnodeWFM.removeNode(m_fail_7_2);
    	final var lock = BlockingRepository.get(BLOCK_LOCK_ID, LockedMethod.EXECUTE).orElseThrow();
    	lock.lockInterruptibly();
    	final NodeID pastedNodeID;
    	try {
    		executeDontWait(m_output_7_8);
    		waitWhile(m_chunkend_7_4, nc -> !nc.getInternalState().isExecuted(), /* seconds to wait */ 5);
    		checkState(m_chunkend_7_4, EXECUTED);
    		waitWhile(m_output_7_8, nc -> nc.getInternalState() != EXECUTING, 5);
    		assertThat("State of blocking node is executing",
    				subnodeWFM.getNodeContainer(m_block_7_6).getInternalState().isExecutionInProgress(), is(true));
    		assertThat("State of end node is executing",
    				subnodeWFM.getNodeContainer(m_output_7_8).getInternalState().isExecutionInProgress(), is(true));
    		// now insert a new node and execute it (different to test method above)
    		final var pastedContent = subnodeWFM.copyFromAndPasteHere(subnodeWFM,
    				WorkflowCopyContent.builder().setNodeIDs(m_datagen_7_1).build());
    		pastedNodeID = pastedContent.getNodeIDs()[0];
    		executeDontWait(pastedNodeID);
    	} finally {
    		lock.unlock();
    	}
    	waitWhileInExecution();
    	checkStateOfMany(EXECUTED, pastedNodeID, m_subnode_7, m_output_7_8, m_block_7_6);
    }

	private WorkflowManager getSubnodeWFM() {
		final var subnode = getManager().getNodeContainer(m_subnode_7, SubNodeContainer.class, true);
    	return subnode.getWorkflowManager();
	}

    /** Remove "Fail in Execution" node, run the entire rest (all executed afterwards), return the inner WFM. */
	private WorkflowManager removeFailNodeAndExecute() throws Exception {
		final var subnodeWFM = getSubnodeWFM();
		subnodeWFM.removeNode(m_fail_7_2); // because it would fail the execution
		executeAllAndWait();
		return subnodeWFM;
	}

	@Override
    public void tearDown() throws Exception {
    	super.tearDown();
    	BlockingRepository.removeAll(BLOCK_LOCK_ID);
    }

}