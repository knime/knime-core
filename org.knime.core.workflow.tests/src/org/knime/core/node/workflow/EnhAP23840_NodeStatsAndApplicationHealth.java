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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;

import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.monitor.ApplicationHealth;
import org.knime.testing.node.blocking.BlockingRepository;
import org.knime.testing.node.blocking.BlockingRepository.LockedMethod;

/** 
 * Tests metrics added as part of AP-23840: Node stats and application health. 
 * 
 * @author Bernd Wiswedel, KNIME
 */
public class EnhAP23840_NodeStatsAndApplicationHealth extends WorkflowTestCase {

    private NodeID m_tableCreate_1;
    private NodeID m_block_2;
    private NodeID m_block_3;
    private NodeID m_tableView_4;
    
    private static final String BLOCK_LOCK_ID_2 = "AP-23840_Block_2";
    private static final String BLOCK_LOCK_ID_3 = "AP-23840_Block_3";

    @BeforeEach
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_tableCreate_1 = baseID.createChild(1);
        m_block_2 = baseID.createChild(2);
        m_block_3 = baseID.createChild(3);
        m_tableView_4 = baseID.createChild(4);
        BlockingRepository.put(BLOCK_LOCK_ID_2, LockedMethod.EXECUTE, new ReentrantLock());
        BlockingRepository.put(BLOCK_LOCK_ID_3, LockedMethod.EXECUTE, new ReentrantLock());
    }

    @Test
    public void testStateCounts() throws Exception {
    	checkState(m_tableCreate_1, CONFIGURED);

    	final int nrOfNodesInExecutedState = ApplicationHealth.getNodeStateExecutedCount();
    	final int nrOfNodesInSomeState = ApplicationHealth.getNodeStateOtherCount();
    	
    	// no concurrent tests are run
    	assertThat("Number of nodes currently executing", ApplicationHealth.getNodeStateExecutingCount(), is(0));
    	
    	assertThat("Number of nodes in some state", nrOfNodesInSomeState, greaterThanOrEqualTo(5));
    	
    	executeAndWait(m_tableCreate_1);
    	checkState(m_tableCreate_1, EXECUTED);
		assertThat("Number of nodes currently executed after single node exec",
				ApplicationHealth.getNodeStateExecutedCount(), is(nrOfNodesInExecutedState + 1));
    	
		assertThat("Number of nodes in some state", ApplicationHealth.getNodeStateOtherCount(),
				is(nrOfNodesInSomeState - 1));
    	
    	reset(m_tableCreate_1);
		assertThat("Number of nodes currently executed", ApplicationHealth.getNodeStateExecutedCount(),
				is(nrOfNodesInExecutedState));
    	
    	final var lock2 = BlockingRepository.get(BLOCK_LOCK_ID_2, LockedMethod.EXECUTE).orElseThrow();
    	final var lock3 = BlockingRepository.get(BLOCK_LOCK_ID_3, LockedMethod.EXECUTE).orElseThrow();
    	lock2.lockInterruptibly();
    	try {
    		lock3.lockInterruptibly();
    		try {
    			getManager().executeAll();
    			waitWhile(m_block_2, n -> n.getInternalState() != InternalNodeContainerState.EXECUTING, /* second */ 5);
    			waitWhile(m_block_3, n -> n.getInternalState() != InternalNodeContainerState.EXECUTING, /* second */ 5);
    			// downstream nodes are queued but don't count as executing
    			assertThat("Number of nodes executing", ApplicationHealth.getNodeStateExecutingCount(), is(2));
    		} finally {
    			lock3.unlock();
    		}
    		waitWhileNodeInExecution(m_tableView_4); // downstream node of block_3
    		
    		// independent branch still executing
    		assertThat("Number of nodes executing", ApplicationHealth.getNodeStateExecutingCount(), is(1));
    	} finally {
    		lock2.unlock();
    	}
    	waitWhileInExecution();
    	assertThat("Number of nodes executing", ApplicationHealth.getNodeStateExecutingCount(), is(0));
    	checkState(getManager(), EXECUTED);
    }
    
	@AfterEach
	@Override
    public void tearDown() throws Exception {
    	super.tearDown();
    	BlockingRepository.removeAll(BLOCK_LOCK_ID_2);
    	BlockingRepository.removeAll(BLOCK_LOCK_ID_3);
    }

}
