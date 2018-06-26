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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.util.FileUtil;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class TestLoadAndExecManySimultaneously extends WorkflowTestCase {

    private ExecutorService m_executorService;
    private OneInstanceWorkflowTest[] m_instances;
    private static final int NR_CONCURRENT = 20;

    @Before
    public void setUp() throws Exception {
        m_executorService = Executors.newFixedThreadPool(NR_CONCURRENT);
        m_instances = new OneInstanceWorkflowTest[NR_CONCURRENT];
        for (int i = 0; i < NR_CONCURRENT; i++) {
            m_instances[i] = new OneInstanceWorkflowTest(i);
        }
    }

    @Test
    public void testConcurrency() throws Exception {
        final CyclicBarrier barrier = new CyclicBarrier(NR_CONCURRENT);
        Future<Void>[] futures = new Future[NR_CONCURRENT];
        final AtomicBoolean isDone = new AtomicBoolean();
        for (int i = 0; i < NR_CONCURRENT; i++) {
            final int index = i;
            futures[i] = m_executorService.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    try {
                        OneInstanceWorkflowTest oneInstance = m_instances[index];
                        oneInstance.prepareWorkflowDirectory();
                        barrier.await(10, TimeUnit.SECONDS);
                        try {
                            oneInstance.loadWorkflow(InternalNodeContainerState.CONFIGURED);
                        } catch (RuntimeException e) {
                            e.printStackTrace();
                            throw e;
                        }
                        barrier.await(120, TimeUnit.SECONDS);
                        oneInstance.executeWorkflow();
                        barrier.await(10, TimeUnit.SECONDS);
                        oneInstance.saveWorkflow();
                        barrier.await(10, TimeUnit.SECONDS);
                        oneInstance.discardWorkflow();
                        barrier.await(10, TimeUnit.SECONDS);
                        oneInstance.loadWorkflow(InternalNodeContainerState.EXECUTED);
                        barrier.await(10, TimeUnit.SECONDS);
                        oneInstance.discardWorkflow();
                        return null;
                    } finally {
                        isDone.set(true);
                    }
                }
                private void await(final int secondsTimeOut)
                        throws InterruptedException, BrokenBarrierException, TimeoutException {
                    barrier.await(secondsTimeOut, TimeUnit.SECONDS);
                }
            });
        }
        ExecutionException brokenBarrierExceptionWrapper = null;
        for (int i = 0; i < NR_CONCURRENT; i++) {
            try {
                futures[i].get();
            } catch (ExecutionException e) {
                if (e.getCause() instanceof BrokenBarrierException) {
                    if (brokenBarrierExceptionWrapper == null) {
                        brokenBarrierExceptionWrapper = e;
                    }
                } else {
                    throw e;
                }
            }
        }
        if (brokenBarrierExceptionWrapper != null) {
            throw brokenBarrierExceptionWrapper;
        }
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        m_executorService.shutdown();
        for (int i = 0; i < m_instances.length; i++) {
            m_instances[i].shutDown();
        }
    }

    class OneInstanceWorkflowTest {

        private NodeContainer m_fileReader;
        private NodeContainer m_diffChecker;
        private File m_instanceDir;
        private final int m_index;
        private WorkflowManager m_loadWorkflow;

        public OneInstanceWorkflowTest(final int index) {
            m_index = index;
        }

        private void prepareWorkflowDirectory() throws Exception {
            File dir = TestLoadAndExecManySimultaneously.this.getDefaultWorkflowDirectory();
            m_instanceDir = FileUtil.createTempDir(
                TestLoadAndExecManySimultaneously.class.getSimpleName() + "-instance-" + m_index);
            FileUtil.copyDir(dir, m_instanceDir);
        }

        private void loadWorkflow(final InternalNodeContainerState exectedState) throws Exception {
            m_loadWorkflow = TestLoadAndExecManySimultaneously.this.loadWorkflow(
                m_instanceDir, new ExecutionMonitor()).getWorkflowManager();
            m_fileReader = m_loadWorkflow.getNodeContainer(new NodeID(m_loadWorkflow.getID(), 1));
            m_diffChecker = m_loadWorkflow.getNodeContainer(new NodeID(m_loadWorkflow.getID(), 5));
            checkState(m_fileReader, exectedState);
            checkState(m_diffChecker, exectedState);
        }

        private void executeWorkflow() throws Exception {
            m_loadWorkflow.executeAll();
            waitWhileNodeInExecution(m_loadWorkflow);
            assertTrue(m_loadWorkflow.isDirty());
            checkState(m_fileReader, InternalNodeContainerState.EXECUTED);
            checkState(m_diffChecker, InternalNodeContainerState.EXECUTED);
        }

        private void saveWorkflow() throws Exception {
            m_loadWorkflow.save(m_instanceDir, new ExecutionMonitor(), true);
        }

        private void discardWorkflow() throws Exception {
            if (m_loadWorkflow != null) {
                m_loadWorkflow.shutdown();
                m_loadWorkflow.getParent().removeNode(m_loadWorkflow.getID());
                m_loadWorkflow = null;
            }
        }

        private void shutDown() throws Exception {
            discardWorkflow();
            FileUtil.deleteRecursively(m_instanceDir);
        }

    }

}
