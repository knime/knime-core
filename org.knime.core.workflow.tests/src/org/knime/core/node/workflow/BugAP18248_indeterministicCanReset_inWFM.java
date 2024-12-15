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
import static org.hamcrest.Matchers.is;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTING;
import static org.knime.core.node.workflow.InternalNodeContainerState.IDLE;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.testing.node.blocking.BlockingRepository;
import org.knime.testing.node.blocking.BlockingRepository.LockedMethod;

/**
 * Validates that {@link WorkflowManager#canPerformReset()} works correctly as per 
 * https://knime-com.atlassian.net/browse/AP-18248
 * 
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
public class BugAP18248_indeterministicCanReset_inWFM extends WorkflowTestCase { // NOSONAR name is "discouraged"

    private static final String LOCK_COMP1_BLOCK_FIRST = "comp_1_block_first";
    private static final String LOCK_COMP2_BLOCK_LAST = "comp_2_block_last";

    private NodeID m_varCreator_1;
    private NodeID m_component1_3;
    private NodeID m_component2_5;
    private NodeID m_component1_block_2;
    private NodeID m_component1_varToTbl_5;
    private NodeID m_component2_block_7;
    private NodeID m_component2_varToTbl_4;

    @BeforeEach
    public void setUp() throws Exception {
        // the id is used here and in the workflow (part of the settings)
        BlockingRepository.put(LOCK_COMP1_BLOCK_FIRST, LockedMethod.EXECUTE, new ReentrantLock());
        BlockingRepository.put(LOCK_COMP2_BLOCK_LAST, LockedMethod.EXECUTE, new ReentrantLock());
        NodeID baseID = loadAndSetWorkflow();
        m_varCreator_1 = baseID.createChild(1);
        m_component1_3 = baseID.createChild(3);
        m_component2_5 = baseID.createChild(5);
        m_component1_block_2 = m_component1_3.createChild(0).createChild(2);
        m_component1_varToTbl_5 = m_component1_3.createChild(0).createChild(5);
        m_component2_block_7 = m_component2_5.createChild(0).createChild(7);
        m_component2_varToTbl_4 = m_component2_5.createChild(0).createChild(4);
    }

	/**
	 * Verify the workflow is in the right state and that the nodes are still
	 * returned in the expected order (other tests rely on that to properly verify
	 * the change).
	 */
    @Test
    public void testInitialSetup() throws Exception {
        WorkflowManager m = getManager();
        checkState(m_varCreator_1, CONFIGURED);
        checkStateOfMany(IDLE, m_component1_3, m_component2_5);

        var subNode1 = m.getNodeContainer(m_component1_3, SubNodeContainer.class, true);
        var listOfNodesInSubNode1 = new ArrayList<>(subNode1.getWorkflowManager().getWorkflow().getNodeIDs());
        listOfNodesInSubNode1.remove(subNode1.getVirtualInNodeID());
        listOfNodesInSubNode1.remove(subNode1.getVirtualOutNodeID());
		assertThat("Wrong content or wrong order", listOfNodesInSubNode1,
				is(List.of(m_component1_block_2, m_component1_varToTbl_5)));

		var subNode2 = m.getNodeContainer(m_component2_5, SubNodeContainer.class, true);
		var listOfNodesInSubNode2 = new ArrayList<>(subNode2.getWorkflowManager().getWorkflow().getNodeIDs());
		listOfNodesInSubNode2.remove(subNode2.getVirtualInNodeID());
		listOfNodesInSubNode2.remove(subNode2.getVirtualOutNodeID());
		assertThat("Wrong content or wrong order", listOfNodesInSubNode2,
				is(List.of(m_component2_varToTbl_4, m_component2_block_7)));
    }

    /** Tests the behavior when first an 'executing' node is returned when checking for reset. */
    @Test
    public void testCanResetComponent1() throws Exception {
        WorkflowManager m = getManager();
        executeAndWait(m_varCreator_1);
    	WorkflowManager component1Mgr = 
    			m.getNodeContainer(m_component1_3, SubNodeContainer.class, true).getWorkflowManager();
        checkState(m_component1_3, CONFIGURED);
        assertThat("Component resetable state when all reset", m.canResetNode(m_component1_3), is(false));
        assertThat("Workflow resetable state when all reset", component1Mgr.canResetAll(), is(false));
        ReentrantLock execLock = BlockingRepository.getNonNull(LOCK_COMP1_BLOCK_FIRST, LockedMethod.EXECUTE);
        execLock.lock();
        try {
        	executeAndWait(m_component1_varToTbl_5);
        	checkState(m_component1_varToTbl_5, EXECUTED);
        	assertThat("Component resetable state when one is executed", m.canResetNode(m_component1_3), is(true));
        	assertThat("Workflow resetable state when all reset", component1Mgr.canResetAll(), is(true));

        	executeDontWait(m_component1_block_2);
        	waitWhile(m_component1_block_2, nc -> nc.getInternalState() != EXECUTING, -1);
        	assertThat("Component resetable state while executing", m.canResetNode(m_component1_3), is(false));
        	assertThat("Workflow resetable state when all reset", component1Mgr.canResetAll(), is(false));
        	checkState(m_component1_block_2, EXECUTING);
        } finally {
            execLock.unlock();
        }
        waitWhileInExecution();
        checkState(m_component1_block_2, EXECUTED);
        executeAndWait(m_component1_3);
        checkState(m_component1_3, EXECUTED);
        assertThat("Component resetable state after execution", m.canResetNode(m_component1_3), is(true));
        assertThat("Workflow restable state after execution", component1Mgr.canResetAll(), is(true));
    }

    /** Tests the behavior when first an 'executed' node is returned when checking for reset. */
    @Test
    public void testCanResetComponent2() throws Exception {
    	WorkflowManager m = getManager();
    	WorkflowManager component2Mgr = 
    			m.getNodeContainer(m_component2_5, SubNodeContainer.class, true).getWorkflowManager();
    	executeAndWait(m_varCreator_1);
    	checkState(m_component2_5, CONFIGURED);
    	assertThat("Component resetable state when all reset", m.canResetNode(m_component2_5), is(false));
    	assertThat("Workflow resetable state when all reset", component2Mgr.canResetAll(), is(false));
    	ReentrantLock execLock = BlockingRepository.getNonNull(LOCK_COMP2_BLOCK_LAST, LockedMethod.EXECUTE);
    	execLock.lock();
    	try {
    		executeAndWait(m_component2_varToTbl_4);
    		checkState(m_component2_varToTbl_4, EXECUTED);
    		assertThat("Component resetable state when one is executed", m.canResetNode(m_component2_5), is(true));
    		assertThat("Workflow resetable state when one is executed", component2Mgr.canResetAll(), is(true));

    		executeDontWait(m_component2_block_7);
        	waitWhile(m_component2_block_7, nc -> nc.getInternalState() != EXECUTING, -1);
    		assertThat("Component resetable state while executing", m.canResetNode(m_component2_5), is(false));
    		assertThat("Workflow resetable state when one is executed", component2Mgr.canResetAll(), is(false));
    		checkState(m_component2_block_7, EXECUTING);
    	} finally {
    		execLock.unlock();
    	}
    	waitWhileInExecution();
    	checkState(m_component2_block_7, EXECUTED);
    	executeAndWait(m_component2_5);
    	checkState(m_component2_5, EXECUTED);
    	assertThat("Component restable state after execution", m.canResetNode(m_component2_5), is(true));
    	assertThat("Workflow restable state after execution", component2Mgr.canResetAll(), is(true));
    }

    /** {@inheritDoc} */
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        BlockingRepository.remove(LOCK_COMP1_BLOCK_FIRST, LockedMethod.EXECUTE);
        BlockingRepository.remove(LOCK_COMP2_BLOCK_LAST, LockedMethod.EXECUTE);
        super.tearDown();
    }

}