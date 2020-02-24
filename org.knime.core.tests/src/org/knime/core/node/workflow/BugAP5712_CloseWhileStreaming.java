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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTINGREMOTELY;
import static org.knime.core.node.workflow.InternalNodeContainerState.IDLE;
import static org.knime.core.node.workflow.InternalNodeContainerState.UNCONFIGURED_MARKEDFOREXEC;

import java.io.File;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.FileUtil;
import org.knime.testing.node.blocking.BlockingRepository;

/**
 * Run a streaming executor and while it's running save &amp; close. See AP-5712.
 * @author wiswedel, University of Konstanz
 */
public class BugAP5712_CloseWhileStreaming extends WorkflowTestCase {

    private static final String LOCK_ID = "bug_ap_5712";

    private File m_workflowDir;
    private NodeID m_tableView_4;
    private NodeID m_streamSubnode_5;

    @Before
    public void setUp() throws Exception {
        BlockingRepository.put(LOCK_ID, new ReentrantLock());
        m_workflowDir = FileUtil.createTempDir(getClass().getSimpleName());
        FileUtil.copyDir(getDefaultWorkflowDirectory(), m_workflowDir);
        initWorkflowFromTemp();
    }

    private WorkflowLoadResult initWorkflowFromTemp() throws Exception {
        // will save the workflow in one of the test ...don't write SVN folder
        WorkflowLoadResult loadResult = loadWorkflow(m_workflowDir, new ExecutionMonitor());
        setManager(loadResult.getWorkflowManager());
        NodeID baseID = getManager().getID();
        m_tableView_4 = new NodeID(baseID, 4);
        m_streamSubnode_5 = new NodeID(baseID, 5);
        return loadResult;
    }

    @Test(timeout = 5000L)
    public void testRunToCompletion() throws Exception {
        WorkflowManager manager = getManager();
        checkState(manager, IDLE);
        // can't check classes here, unfortunately
        assertThat("Expected streaming executor on component", findNodeContainer(m_streamSubnode_5)
            .getJobManager().getClass().toString().toLowerCase(), containsString("stream"));
        ReentrantLock execLock = BlockingRepository.get(LOCK_ID);
        execLock.lock();
        try {
            manager.executeAll();
            int waitCount = 0;
            while (!execLock.hasQueuedThreads() && waitCount < 10) {
                waitCount += 1;
                Thread.sleep(100);
            }
            assertThat("Streaming executor hasn't been progressed to 'blocking' node", execLock.hasQueuedThreads(),
                is(Boolean.TRUE));
        } finally {
            execLock.unlock();
        }
        waitWhileInExecution();
        checkState(manager, EXECUTED);
    }

    @Test(timeout = 5000L)
    public void testSaveLoadWhileExecuting() throws Exception {
        WorkflowManager manager = getManager();
        ReentrantLock execLock = BlockingRepository.get(LOCK_ID);
        execLock.lock();
        try {
            manager.executeAll();
            final NodeContainer streamNC = findNodeContainer(m_streamSubnode_5);
            waitWhile(streamNC, new Hold() {
                @Override
                protected boolean shouldHold() {
                    return !streamNC.getInternalState().isExecutingRemotely();
                }
            });
            checkState(m_streamSubnode_5, EXECUTINGREMOTELY);
            checkState(m_tableView_4, UNCONFIGURED_MARKEDFOREXEC);
            manager.save(m_workflowDir, new ExecutionMonitor(), true);
            assertTrue(manager.canCancelAll());
            getLogger().error("Canceling...");
            manager.getParent().cancelExecution(manager);
            waitWhileInExecution();
        } finally {
            execLock.unlock();
        }
        closeWorkflow();
        assertNull(getManager());
        WorkflowLoadResult loadResult = initWorkflowFromTemp();
        assertFalse("should not have errors", loadResult.hasErrors());
        // when saving the workflow while executing we can't determine the correct 'safe' state of all nodes
        // in the component - most of them are configured but downstream of the transpose they are idle;
        // the framework saves them as IDLE and the load-routines will update the state ... and warn.
        assertTrue("should have warnings on state - unpredictable during #save", loadResult.hasWarningEntries());
        manager = getManager();
    }

    /** {@inheritDoc} */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        BlockingRepository.remove(LOCK_ID);
        FileUtil.deleteRecursively(m_workflowDir);
    }

}
