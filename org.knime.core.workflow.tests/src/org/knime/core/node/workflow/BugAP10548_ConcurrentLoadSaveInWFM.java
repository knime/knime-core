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

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.testing.node.blocking.BlockingRepository;
import org.knime.testing.node.blocking.BlockingRepository.LockedMethod;

/** Tests basic functionality of the "Block Programmatically (XYZ)" nodes. 
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class BugAP10548_ConcurrentLoadSaveInWFM extends WorkflowTestCase {

    private static final String LOCK_INSTANCE_1 = "AP-10548-instance1";
    private static final String LOCK_INSTANCE_2 = "AP-10548-instance2";
    private static final String LOCK_INSTANCE_3 = "AP-10548-instance3";

    private WorkflowManager m_instance1WFM;
    private WorkflowManager m_instance2WFM;
    private WorkflowManager m_instance3WFM;

    private File m_instance2CopyDir;

    @BeforeEach
    public void addLocksToRepoBeforeTest() {
        BlockingRepository.put(LOCK_INSTANCE_1, LockedMethod.CONFIGURE, new ReentrantLock());
        BlockingRepository.put(LOCK_INSTANCE_2, LockedMethod.CONFIGURE, new ReentrantLock());
        BlockingRepository.put(LOCK_INSTANCE_3, LockedMethod.CONFIGURE, new ReentrantLock());
        BlockingRepository.put(LOCK_INSTANCE_2, LockedMethod.SAVE_INTERNALS, new ReentrantLock());
    }

    @Test
    public void testLoadAllConcurrently() throws Exception {
        ReentrantLock instance1Lock = getLock(LOCK_INSTANCE_1, LockedMethod.CONFIGURE);
        ReentrantLock instance2Lock = getLock(LOCK_INSTANCE_2, LockedMethod.CONFIGURE);
        ReentrantLock instance3Lock = getLock(LOCK_INSTANCE_3, LockedMethod.CONFIGURE);
        instance1Lock.lock();
        instance2Lock.lock();
        instance3Lock.lock();
        boolean instance1IsLocked = true;
        boolean instance2IsLocked = true;
        boolean instance3IsLocked = true;
        Future<WorkflowLoadResult> instance1Future;
        Future<WorkflowLoadResult> instance2Future;
        Future<WorkflowLoadResult> instance3Future;
        try {
            // this may yield thread exhaustion -- if it does the testing environment needs to be changed
            instance1Future = KNIMEConstants.GLOBAL_THREAD_POOL.submit(() -> {
                WorkflowLoadResult res = loadWorkflow(getWorkflowDirFor(1), new ExecutionMonitor());
                m_instance1WFM = res.getWorkflowManager();
                return res;
            });
            instance2Future = KNIMEConstants.GLOBAL_THREAD_POOL.submit(() -> {
                WorkflowLoadResult res = loadWorkflow(getWorkflowDirFor(2), new ExecutionMonitor());
                m_instance2WFM = res.getWorkflowManager();
                return res;
            });
            instance3Future = KNIMEConstants.GLOBAL_THREAD_POOL.submit(() -> {
                WorkflowLoadResult res = loadWorkflow(getWorkflowDirFor(3), new ExecutionMonitor());
                m_instance3WFM = res.getWorkflowManager();
                return res;
            });
            // this would usually take << 1s. When this workflow is run stand-alone (from eclipse, only this test)
            // it would take longer due to loading of the node repository
            for (int wait = 10, i = 0; i < 30 * 1000; i += wait) {
                if (instance1Lock.hasQueuedThreads() && instance2Lock.hasQueuedThreads()
                        && instance3Lock.hasQueuedThreads()) {
                    break;
                }
                Thread.sleep(wait); // NOSONAR
            }
            assertThat("Load lock of instance1 has queued threads", instance1Lock.hasQueuedThreads(), is(true));
            assertThat("Load lock of instance2 has queued threads", instance2Lock.hasQueuedThreads(), is(true));
            assertThat("Load lock of instance3 has queued threads", instance3Lock.hasQueuedThreads(), is(true));

            // release 2
            instance2Lock.unlock();
            instance2IsLocked = false;
            final WorkflowLoadResult instance2LoadResult = instance2Future.get(20, TimeUnit.SECONDS);
            assertThat("Workflow instance2 load status", instance2LoadResult.getType(), is(LoadResultEntryType.Ok));
            assertThat("Workflow for instance2 is present", instance2Future.get(), is(notNullValue()));
            assertThat("Workflow loading for instance1 is done", instance1Future.isDone(), is(false));
            assertThat("Workflow loading for instance3 is done", instance3Future.isDone(), is(false));

            // release 3
            instance3Lock.unlock();
            instance3IsLocked = false;
            final WorkflowLoadResult instance3LoadResult = instance3Future.get(20, TimeUnit.SECONDS);
            assertThat("Workflow instance3 load status", instance3LoadResult.getType(), is(LoadResultEntryType.Ok));
            assertThat("Workflow loading for instance1 is done", instance1Future.isDone(), is(false));
            assertThat("Workflow loading for instance3 is done", instance3Future.isDone(), is(true));

            // release 1
            instance1Lock.unlock();
            instance1IsLocked = false;
            final WorkflowLoadResult instance1LoadResult = instance1Future.get(20, TimeUnit.SECONDS);
            assertThat("Workflow instance1 load status", instance1LoadResult.getType(), is(LoadResultEntryType.Ok));
            assertThat("Workflow for instance1 is present", instance1Future.get(), is(notNullValue()));
        } finally {
            if (instance1IsLocked) {
                instance1Lock.unlock();
            }
            if (instance2IsLocked) {
                instance2Lock.unlock();
            }
            if (instance3IsLocked) {
                instance3Lock.unlock();
            }
        }
        // wait for completion, so that #tearDown can clean up
        instance1Future.get();
        instance2Future.get();
        instance3Future.get();
    }

    @Test
    public void testLoadWhileSaving() throws Exception {
        ReentrantLock instance2Lock = getLock(LOCK_INSTANCE_2, LockedMethod.SAVE_INTERNALS);

        final File workflowDirForInstance2 = getWorkflowDirFor(2);
        m_instance2CopyDir = new File(FileUtils.getTempDirectory(), workflowDirForInstance2.getName());
        FileUtils.copyDirectory(workflowDirForInstance2, m_instance2CopyDir);
        m_instance2WFM = loadWorkflow(m_instance2CopyDir, new ExecutionMonitor()).getWorkflowManager();
        checkState(m_instance2WFM, CONFIGURED);
        m_instance2WFM.executeAllAndWaitUntilDone();
        checkState(m_instance2WFM, EXECUTED);

        Future<WorkflowLoadResult> instance2Future;

        instance2Lock.lock();
        try {
            instance2Future = KNIMEConstants.GLOBAL_THREAD_POOL.submit(() -> {
                m_instance2WFM.save(m_instance2CopyDir, new ExecutionMonitor(), true);
                return null;
            });
            m_instance1WFM = loadWorkflow(getWorkflowDirFor(1), new ExecutionMonitor()).getWorkflowManager();
            m_instance3WFM = loadWorkflow(getWorkflowDirFor(3), new ExecutionMonitor()).getWorkflowManager();
            m_instance1WFM.executeAllAndWaitUntilDone();
            m_instance3WFM.executeAllAndWaitUntilDone();
            assertThat("Status of workflow instance 1", m_instance1WFM.getInternalState(), is(EXECUTED));
            assertThat("Status of workflow instance 3", m_instance3WFM.getInternalState(), is(EXECUTED));

            assertThat("Workflow instance2 is locked (currently being saved)", 
                    m_instance2WFM.getReentrantLockInstance().isLocked(), is(true));

        } finally {
            instance2Lock.unlock();
        }
        // wait for completion, so that #tearDown can clean up
        instance2Future.get();
        assertThat("Workflow instance2 is locked (currently being saved)", 
                m_instance2WFM.getReentrantLockInstance().isLocked(), is(false));
    }

    private File getWorkflowDirFor(final int index) throws Exception {
        return new File(getDefaultWorkflowDirectory(), "bugAP10548_ConcurrentLoadSaveInWFM_instance" + index);
    }

    private static ReentrantLock getLock(String key, LockedMethod method) {
        return BlockingRepository.get(key, method)
                .orElseThrow(() -> new IllegalArgumentException("No lock for ID " + key));
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        BlockingRepository.removeAll(LOCK_INSTANCE_1);
        BlockingRepository.removeAll(LOCK_INSTANCE_2);
        BlockingRepository.removeAll(LOCK_INSTANCE_3);
        for (WorkflowManager wfm : Arrays.asList(m_instance1WFM, m_instance2WFM, m_instance3WFM)) {
            if (wfm != null) {
                wfm.getParent().removeProject(wfm.getID());
            }
        }
        FileUtils.deleteQuietly(m_instance2CopyDir);
        super.tearDown();
    }

}