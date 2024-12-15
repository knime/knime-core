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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTING;
import static org.knime.core.node.workflow.InternalNodeContainerState.IDLE;

import java.io.File;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.FileUtil;
import org.knime.testing.node.blocking.BlockingRepository;
import org.knime.testing.node.blocking.BlockingRepository.LockedMethod;

/** Tests basic functionality of the "Block Programmatically (XYZ)" nodes. 
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class TestBlockingNode extends WorkflowTestCase {

    private static final String LOCK_TABLE_NODE_NAME = "block_table_node";
    private static final String LOCK_VAR_NODE_NAME = "block_variable_node";
	private File m_workflowDir;
	private NodeID m_tableCreator_2;
    private NodeID m_tableView_5;
    private NodeID m_blockTable_3;
    private NodeID m_blockVariable_4;
    private NodeID m_mergeVariable_6;

    @BeforeEach
    public void setUp() throws Exception {
        m_workflowDir = FileUtil.createTempDir(getClass().getSimpleName());
        FileUtil.copyDir(getDefaultWorkflowDirectory(), m_workflowDir);
        initWorkflowFromTemp();
    }

    private WorkflowLoadResult initWorkflowFromTemp() throws Exception {
        WorkflowLoadResult loadResult = loadWorkflow(m_workflowDir, new ExecutionMonitor());
        setManager(loadResult.getWorkflowManager());
        NodeID baseID = getManager().getID();
        m_tableCreator_2 = baseID.createChild(2);
        m_tableView_5 = baseID.createChild(5);
        m_blockTable_3 = baseID.createChild(3);
        m_blockVariable_4 = baseID.createChild(4);
        m_mergeVariable_6 = baseID.createChild(6);
        BlockingRepository.put(LOCK_TABLE_NODE_NAME, LockedMethod.EXECUTE, new ReentrantLock());
        BlockingRepository.put(LOCK_TABLE_NODE_NAME, LockedMethod.CONFIGURE, new ReentrantLock());
        BlockingRepository.put(LOCK_TABLE_NODE_NAME, LockedMethod.SAVE_INTERNALS, new ReentrantLock());
        BlockingRepository.put(LOCK_VAR_NODE_NAME, LockedMethod.EXECUTE, new ReentrantLock());
        BlockingRepository.put(LOCK_VAR_NODE_NAME, LockedMethod.CONFIGURE, new ReentrantLock());
        BlockingRepository.put(LOCK_VAR_NODE_NAME, LockedMethod.SAVE_INTERNALS, new ReentrantLock());
        return loadResult;
    }

    /** Load workflow, expect all nodes to be 'yellow', then run to completion. */
    @Test
    public void testLoadRunAllGreen() throws Exception {
        checkStateOfMany(InternalNodeContainerState.CONFIGURED, m_tableView_5, m_mergeVariable_6);
        checkState(getManager(), InternalNodeContainerState.CONFIGURED);
        executeAllAndWait();
        checkState(getManager(), InternalNodeContainerState.EXECUTED);
    }

    /** Check whether execution can be paused by acquiring lock. */
    @Test
    public void testBlockExecution() throws Exception {
        ReentrantLock execTableLock = getLock(LOCK_TABLE_NODE_NAME, LockedMethod.EXECUTE);
        ReentrantLock execVarLock = getLock(LOCK_VAR_NODE_NAME, LockedMethod.EXECUTE);
        execTableLock.lock();
        execVarLock.lock();
        try {
        	getManager().executeAll();
			waitWhile(m_blockVariable_4, nc -> !nc.getInternalState().equals(EXECUTING), -1);
			waitWhile(m_blockTable_3, nc -> !nc.getInternalState().equals(EXECUTING), -1);
			Thread.sleep(100); // NOSONAR // give some time to reach the user code in BlockingNodeModel
			assertThat("Var lock has waiting threads", execVarLock.hasQueuedThreads(), is(true));
			assertThat("Table lock has waiting threads", execTableLock.hasQueuedThreads(), is(true));
			Thread.sleep(400); // NOSONAR
			checkStateOfMany(EXECUTING, m_blockTable_3, m_blockVariable_4);
        } finally {
        	execTableLock.unlock();
        	execVarLock.unlock();
        }
        waitWhileInExecution();
        checkState(getManager(), EXECUTED);
    }

    /** Check whether configuration can be paused by acquiring lock. */
    @Test
    public void testBlockConfigureTableNode() throws Exception {
    	ReentrantLock configureTableLock = getLock(LOCK_TABLE_NODE_NAME, LockedMethod.CONFIGURE);
    	ConnectionContainer toBlockTableConnection = findInConnection(m_blockTable_3, 1);
    	assertThat("Input connection to block node", toBlockTableConnection, is(notNullValue()));
    	getManager().removeConnection(toBlockTableConnection);
    	checkState(m_blockTable_3, IDLE);
    	Future<?> addConnectionFuture;
    	configureTableLock.lock();
    	try {
    		addConnectionFuture = KNIMEConstants.GLOBAL_THREAD_POOL.submit( //
    				() -> getManager().addConnection(m_tableCreator_2, 1, m_blockTable_3, 1));
    		long timeoutMS = 200;
    		try {
    			addConnectionFuture.get(timeoutMS, TimeUnit.MILLISECONDS);
    			org.junit.jupiter.api.Assertions.fail("Adding a connection in a separate thread should not complete, #configure is locked.");
    		} catch (TimeoutException e) {
    			// expected, adding a connection will configure the node but configure is blocked
    		}
    		assertThat("Table lock has waiting threads", configureTableLock.hasQueuedThreads(), is(true));
    	} finally {
    		configureTableLock.unlock();
    	}
    	addConnectionFuture.get();
    	checkState(m_blockTable_3, CONFIGURED);
    }


    /** Check wheter execution can be paused by acquiring lock. */
    @Test
    public void testBlockConfigureVariableNode() throws Exception {
    	ReentrantLock configureVariableLock = getLock(LOCK_VAR_NODE_NAME, LockedMethod.CONFIGURE);
    	Future<?> addConnectionFuture;
    	configureVariableLock.lock();
    	try {
    		addConnectionFuture = KNIMEConstants.GLOBAL_THREAD_POOL.submit( //
    				() -> getManager().addConnection(m_tableCreator_2, 0, m_blockVariable_4, 1));
    		long timeoutMS = 200;
    		try {
    			addConnectionFuture.get(timeoutMS, TimeUnit.MILLISECONDS);
    			org.junit.jupiter.api.Assertions.fail("Adding a connection in a separate thread should not complete, #configure is locked.");
    		} catch (TimeoutException e) {
    			// expected, adding a connection will configure the node but configure is blocked
    		}
    		assertThat("Table lock has waiting threads", configureVariableLock.hasQueuedThreads(), is(true));
    	} finally {
    		configureVariableLock.unlock();
    	}
    	addConnectionFuture.get();
    	checkState(m_blockVariable_4, CONFIGURED);
    	ConnectionContainer toBlockVariableConnection = findInConnection(m_blockVariable_4, 1);
    	assertThat("Input connection to block variable node", toBlockVariableConnection, is(notNullValue()));
    }



    /** Check whether saving the workflow can be locked. */
	@Test
	public void testBlockSaveInternals() throws Exception {
	    ReentrantLock saveTableLock = getLock(LOCK_TABLE_NODE_NAME, LockedMethod.SAVE_INTERNALS);
	    ReentrantLock saveVarLock = getLock(LOCK_VAR_NODE_NAME, LockedMethod.SAVE_INTERNALS);
	    executeAndWait(m_tableCreator_2);
	    checkStateOfMany(CONFIGURED, m_blockTable_3, m_blockVariable_4);
	    // save partially executed workflow (the table creator creates a 1-cell table, whose saving might be expensive)
	    getManager().save(m_workflowDir, new ExecutionMonitor(), true); 
	    executeAllAndWait();
	    checkState(getManager(), EXECUTED);
	    assertThat("Workflow dirty state", getManager().isDirty(), is(true));
    	Future<?> saveFuture;
	    saveTableLock.lock();
	    saveVarLock.lock();
	    try {
    		saveFuture = KNIMEConstants.GLOBAL_THREAD_POOL.submit(() -> {
    			getManager().save(m_workflowDir, new ExecutionMonitor(), true);
    			return null;
    		});
    		long timeoutMS = 500;
    		try {
    			saveFuture.get(timeoutMS, TimeUnit.MILLISECONDS);
    			org.junit.jupiter.api.Assertions.fail("Saving workflow asynchronously expected to not complete (node should be locked)");
    		} catch (TimeoutException e) {
    			// expected, adding a connection will configure the node but configure is blocked
    		}
    		assertThat("One of the locks has waiting threads", 
    				saveTableLock.hasQueuedThreads() || saveVarLock.hasQueuedThreads(), is(true));
	    } finally {
	    	saveTableLock.unlock();
	    	saveVarLock.unlock();
	    }
	    saveFuture.get();
	    assertThat("Workflow dirty state", getManager().isDirty(), is(false));
	}

	/**
	 * @return
	 */
	private static ReentrantLock getLock(String key, LockedMethod method) {
		return BlockingRepository.get(key, method)
				.orElseThrow(() -> new IllegalArgumentException("No lock for ID " + key));
	}

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        BlockingRepository.removeAll(LOCK_TABLE_NODE_NAME);
        BlockingRepository.removeAll(LOCK_VAR_NODE_NAME);
        FileUtil.deleteRecursively(m_workflowDir);
    }

}