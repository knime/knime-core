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
 * ---------------------------------------------------------------------
 *
 * History
 *   19.06.2013 (thor): created
 */
package org.knime.core.node.workflow;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.SwingWorker;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.NodeContext.ContextObjectSupplier;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.FileUtil;
import org.knime.core.util.SwingWorkerWithContext;
import org.knime.core.util.ThreadUtils;
import org.knime.core.util.ThreadUtils.CallableWithContext;
import org.knime.core.util.ThreadUtils.RunnableWithContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Testcases for {@link NodeContext}.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
public class NodeContextTest {
    private static WorkflowManager wfm;

    private static ExecutorService executorService;

    /**
     * Unpack and load testflow.
     *
     * @throws Exception if an error occurs
     */
    @BeforeClass
    public static void setupClass() throws Exception {
        File workflowZip = findInPlugin("/files/NodeContextTestflow.zip");
        File tmpDir = FileUtil.createTempDir("NodeContextTest");
        tmpDir.deleteOnExit();
        FileUtil.unzip(workflowZip, tmpDir);
        File workflowDir = tmpDir.listFiles()[0];
        WorkflowLoadResult res =
                WorkflowManager.loadProject(workflowDir, new ExecutionMonitor(), new WorkflowLoadHelper(workflowDir));
        if (res.hasErrors()) {
            throw new IOException("Error while loading workflow: " + res.getMessage());
        }
        wfm = res.getWorkflowManager();
        executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Shuts the executor service down.
     */
    @AfterClass
    public static void teardownClass() {
        wfm.getParent().removeProject(wfm.getID());
        executorService.shutdown();
    }

    /**
     * Check that the node context stack is empty before each test method.
     */
    @Before
    public void checkForEmptyContextStackBefore() {
        NodeContext currentContext = NodeContext.getContext();

        // this cleans up if a test method accidently leaves a filled stack so that the next method has an empty stack
        while (NodeContext.getContext() != null) {
            NodeContext.removeLastContext();
        }

        assertThat("Node context stack not empty before test", currentContext, is(nullValue()));
    }

    /**
     * Check that the node context stack is empty after each test method.
     */
    @After
    public void checkForEmptyContextStackAfter() {
        assertThat("Node context stack not empty after test", NodeContext.getContext(), is(nullValue()));
    }


    /**
     * Tests basic operations on the context stack.
     */
    @Test
    public void testStack() {
        List<NodeContainer> containers = new ArrayList<NodeContainer>(wfm.getNodeContainers());

        NodeContext.pushContext(containers.get(0)); // Table Creator
        NodeContext.pushContext(containers.get(1)); // Shuffle
        NodeContext.pushContext(containers.get(2)); // Metanode

        assertThat("Unexpected node container on top of context stack", NodeContext.getContext().getNodeContainer(),
                   is(containers.get(2)));
        assertThat("Unexpected workflow manager", NodeContext.getContext().getWorkflowManager(), is(wfm));

        NodeContext.removeLastContext();
        assertThat("Unexpected node container on top of context stack", NodeContext.getContext().getNodeContainer(),
                   is(containers.get(1)));
        assertThat("Unexpected workflow manager", NodeContext.getContext().getWorkflowManager(), is(wfm));

        NodeContext.removeLastContext();
        assertThat("Unexpected node container on top of context stack", NodeContext.getContext().getNodeContainer(),
                   is(containers.get(0)));
        assertThat("Unexpected workflow manager", NodeContext.getContext().getWorkflowManager(), is(wfm));

        NodeContext.removeLastContext();
        assertThat("Node context stack was not empty", NodeContext.getContext(), is(nullValue()));

        try {
            NodeContext.removeLastContext();
            fail("Expected IllegalStateException upon removeLastContext and empty stack");
        } catch (IllegalStateException ex) {
            // OK
        }
    }

    /**
     * Test if the node context handles metanodes properly
     */
    @Test
    public void testStackWithMetanode() {
        List<NodeContainer> containers = new ArrayList<NodeContainer>(wfm.getNodeContainers());

        WorkflowManager metaNode = (WorkflowManager)containers.get(2); // this is the metanode
        containers = new ArrayList<NodeContainer>(metaNode.getNodeContainers());
        NodeContext.pushContext(containers.get(0)); // Shuffle in metanode
        assertThat("Unexpected node container on top of context stack", NodeContext.getContext().getNodeContainer(),
                   is(containers.get(0)));
        assertThat("Unexpected workflow manager", NodeContext.getContext().getWorkflowManager(), is(wfm));
        NodeContext.removeLastContext();
    }

    /**
     * Tests the node context with other objects than {@link NodeContainer} or {@link WorkflowManager}.
     */
    @Test
    public void testStackWithObject() {
        String contextObj = "ctx obj";
        NodeContext.pushContext(contextObj);
        assertThat("Empty optional expected due to missing context object supplier",
            NodeContext.getContext().getContextObjectForClass(String.class), is(Optional.empty()));
        ContextObjectSupplier supplier = new ContextObjectSupplier() {
            @Override
            public <C> Optional<C> getObjOfClass(final Class<C> contextObjClass, final Object srcObj) {
                return Optional.of((C)srcObj);
            }
        };
        NodeContext.addContextObjectSupplier(supplier);
        try {
            assertThat("Unexpected context object", NodeContext.getContext().getContextObjectForClass(String.class).get(),
                is(contextObj));
        } finally {
            NodeContext.removeContextObjectSupplier(supplier);
        }
        NodeContext.removeLastContext();
    }

    /**
     * Test handling an empty context.
     */
    @Test
    public void testEmptyContext() {
        NodeContext.pushContext((NodeContext)null);
        assertThat("Unexpected node context", NodeContext.getContext(), is(nullValue()));
        NodeContext.removeLastContext(); // must not throw an exception
    }

    /**
     * Test pushing an existing context.
     */
    @Test
    public void testPushContext() {
        List<NodeContainer> containers = new ArrayList<NodeContainer>(wfm.getNodeContainers());

        NodeContext.pushContext(containers.get(0)); // Table Creator
        NodeContext.pushContext(NodeContext.getContext()); // Table Creator
        assertThat("Unexpected node container on top of context stack", NodeContext.getContext().getNodeContainer(),
                   is(containers.get(0)));
        NodeContext.removeLastContext();
        assertThat("Unexpected node container on top of context stack", NodeContext.getContext().getNodeContainer(),
                   is(containers.get(0)));
        NodeContext.removeLastContext();
    }

    /**
     * Test context transfer to thread pool jobs.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testContextInThreadpool() throws Exception {
        List<NodeContainer> containers = new ArrayList<NodeContainer>(wfm.getNodeContainers());

        NodeContext.pushContext(containers.get(0)); // Table Creator

        Callable<NodeContext> callable = new Callable<NodeContext>() {
            @Override
            public NodeContext call() throws Exception {
                return NodeContext.getContext();
            }
        };

        NodeContext contextInPool = KNIMEConstants.GLOBAL_THREAD_POOL.submit(callable).get();
        assertThat("Context transfer to pool thread via submit(Callable) does not work", contextInPool,
                   is(NodeContext.getContext()));

        NodeContext.pushContext(containers.get(1)); // Shuffle
        contextInPool = KNIMEConstants.GLOBAL_THREAD_POOL.enqueue(callable).get();
        assertThat("Context transfer to pool thread via enqueue(Callable) does not work", contextInPool,
                   is(NodeContext.getContext()));

        final AtomicReference<NodeContext> ref = new AtomicReference<NodeContext>();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ref.set(NodeContext.getContext());
            }
        };

        NodeContext.pushContext(containers.get(2)); // Metanode
        KNIMEConstants.GLOBAL_THREAD_POOL.submit(runnable).get();
        assertThat("Context transfer to pool thread via submit(Runnable) does not work", ref.get(),
                   is(NodeContext.getContext()));

        NodeContext.pushContext(containers.get(0)); // File Reader
        KNIMEConstants.GLOBAL_THREAD_POOL.enqueue(runnable).get();
        assertThat("Context transfer to pool thread via enqueue(Runnable) does not work", ref.get(),
                   is(NodeContext.getContext()));

        NodeContext.removeLastContext();
        NodeContext.removeLastContext();
        NodeContext.removeLastContext();
        NodeContext.removeLastContext();
    }

    /**
     * Tests if a node context is transfered into the AWT Event thread via {@link ViewUtils}.
     *
     * @throws InterruptedException if an error occurs
     */
    @Test
    public void testContextInAWTEventQueue() throws InterruptedException {
        List<NodeContainer> containers = new ArrayList<NodeContainer>(wfm.getNodeContainers());

        NodeContext.pushContext(containers.get(0)); // Table Creator
        final AtomicReference<NodeContext> ref = new AtomicReference<NodeContext>();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ref.set(NodeContext.getContext());
            }
        };

        ViewUtils.invokeAndWaitInEDT(runnable);
        assertThat("Context transfer to AWT Event thread via invokeAndWaitInEDT does not work", ref.get(),
                   is(NodeContext.getContext()));

        final ReentrantLock lock = new ReentrantLock();
        final Condition cond = lock.newCondition();
        runnable = new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    ref.set(NodeContext.getContext());
                    cond.signalAll();
                } finally {
                    lock.unlock();
                }
            }
        };
        lock.lock();
        try {
            NodeContext.pushContext(containers.get(1)); // Shuffle
            ViewUtils.invokeLaterInEDT(runnable);
            cond.await();
        } finally {
            lock.unlock();
        }
        assertThat("Context transfer to AWT Event thread via invokeLaterInEDT does not work", ref.get(),
                   is(NodeContext.getContext()));

        lock.lock();
        try {
            NodeContext.pushContext(containers.get(2)); // Metanode
            ViewUtils.runOrInvokeLaterInEDT(runnable);
            cond.await();
        } finally {
            lock.unlock();
        }
        assertThat("Context transfer to AWT Event thread via runOrInvokeLaterInEDT does not work", ref.get(),
                   is(NodeContext.getContext()));

        NodeContext.removeLastContext();
        NodeContext.removeLastContext();
        NodeContext.removeLastContext();
    }

    /**
     * Checks if {@link ThreadUtils#runnableWithContext(Runnable)} works as expected.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testRunnableWithContext() throws Exception {
        List<NodeContainer> containers = new ArrayList<NodeContainer>(wfm.getNodeContainers());

        NodeContext.pushContext(containers.get(0)); // Table Creator
        final AtomicReference<NodeContext> ref = new AtomicReference<NodeContext>();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ref.set(NodeContext.getContext());
            }
        };

        Thread t = new Thread(ThreadUtils.runnableWithContext(runnable));
        t.start();
        t.join();

        assertThat("Context inheritance to runnable does now work properly", ref.get(), is(NodeContext.getContext()));
        NodeContext.removeLastContext();

        Runnable runnableWithContext = new RunnableWithContext() {
            @Override
            protected void runWithContext() {
                // do nothing
            }
        };

        assertThat("RunnableWithContext wrapped again by runnableWithContext",
                   ThreadUtils.runnableWithContext(runnableWithContext), is(sameInstance(runnableWithContext)));

        assertThat("Runnable wrapped by runnableWithContext although not context is available",
                   ThreadUtils.runnableWithContext(runnable), is(sameInstance(runnable)));
    }

    /**
     * Checks if {@link ThreadUtils#callableWithContext(Callable)} works as expected.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testCallableWithContext() throws Exception {
        List<NodeContainer> containers = new ArrayList<NodeContainer>(wfm.getNodeContainers());

        NodeContext.pushContext(containers.get(0)); // Table Creator
        Callable<NodeContext> callable = new Callable<NodeContext>() {
            @Override
            public NodeContext call() {
                return NodeContext.getContext();
            }
        };

        Future<NodeContext> future = executorService.submit(ThreadUtils.callableWithContext(callable));

        assertThat("Context inheritance to callable does now work properly", future.get(), is(NodeContext.getContext()));
        NodeContext.removeLastContext();

        Callable<Object> callableWithContext = new CallableWithContext<Object>() {
            @Override
            protected Object callWithContext() {
                return null;
            }
        };

        assertThat("CallableWithContext wrapped again by callableWithContext",
                   ThreadUtils.callableWithContext(callableWithContext), is(sameInstance(callableWithContext)));

        assertThat("Callable wrapped by callableWithContext although not context is available",
                   ThreadUtils.callableWithContext(callable), is(sameInstance(callable)));
    }

    /**
     * Checks if {@link ThreadUtils#threadWithContext(Runnable)} and
     * {@link ThreadUtils#threadWithContext(Runnable, String)} work as expected.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testThreadWithContext() throws Exception {
        List<NodeContainer> containers = new ArrayList<NodeContainer>(wfm.getNodeContainers());

        NodeContext.pushContext(containers.get(0)); // Table Creator
        final AtomicReference<NodeContext> ref = new AtomicReference<NodeContext>();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ref.set(NodeContext.getContext());
            }
        };

        Thread t = ThreadUtils.threadWithContext(runnable);
        t.start();
        t.join();

        assertThat("Context inheritance via threadWithContext does now work properly", ref.get(),
                   is(NodeContext.getContext()));

        ref.set(null);
        t = ThreadUtils.threadWithContext(runnable, "Thread's name");
        t.start();
        t.join();

        assertThat("Context inheritance via threadWithContext does now work properly", ref.get(),
                   is(NodeContext.getContext()));
        NodeContext.removeLastContext();
    }

    /**
     * Checks if {@link ThreadUtils#executorWithContext(java.util.concurrent.Executor)} works as expected.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testExecutorWithContext() throws Exception {
        List<NodeContainer> containers = new ArrayList<NodeContainer>(wfm.getNodeContainers());

        NodeContext.pushContext(containers.get(0)); // Table Creator
        final AtomicReference<NodeContext> ref = new AtomicReference<NodeContext>();
        final ReentrantLock lock = new ReentrantLock();
        final Condition condition = lock.newCondition();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ref.set(NodeContext.getContext());
                lock.lock();
                try {
                    condition.signalAll();
                } finally {
                    lock.unlock();
                }
            }
        };

        Executor executor = ThreadUtils.executorWithContext(executorService);
        lock.lock();
        try {
            executor.execute(runnable);
            condition.await();
        } finally {
            lock.unlock();
        }

        assertThat("Context inheritance via wrapped Executor does now work properly", ref.get(),
                   is(NodeContext.getContext()));
        NodeContext.removeLastContext();
    }

    /**
     * Checks if {@link ThreadUtils#executorServiceWithContext(ExecutorService)} works as expected.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testExecutorServiceWithContext() throws Exception {
        List<NodeContainer> containers = new ArrayList<NodeContainer>(wfm.getNodeContainers());

        NodeContext.pushContext(containers.get(0)); // Table Creator
        final AtomicReference<NodeContext> ref = new AtomicReference<NodeContext>();

        final ReentrantLock lock = new ReentrantLock();
        final Condition condition = lock.newCondition();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ref.set(NodeContext.getContext());
                lock.lock();
                try {
                    condition.signalAll();
                } finally {
                    lock.unlock();
                }
            }
        };

        ExecutorService executor = ThreadUtils.executorServiceWithContext(executorService);
        lock.lock();
        try {
            executor.execute(runnable);
            condition.await();
        } finally {
            lock.unlock();
        }

        assertThat("Context inheritance via wrapped ExecutorService.execute does now work properly", ref.get(),
                   is(NodeContext.getContext()));

        runnable = new Runnable() {
            @Override
            public void run() {
                ref.set(NodeContext.getContext());
            }
        };

        executor.submit(runnable).get();
        assertThat("Context inheritance via wrapped ExecutorService.submit(Runnable) does now work properly",
                   ref.get(), is(NodeContext.getContext()));

        executor.submit(runnable, new Integer(1)).get();
        assertThat("Context inheritance via wrapped ExecutorService.submit(Runnable) does now work properly",
                   ref.get(), is(NodeContext.getContext()));

        Callable<NodeContext> callable = new Callable<NodeContext>() {
            @Override
            public NodeContext call() {
                return NodeContext.getContext();
            }
        };

        Future<NodeContext> future = executor.submit(callable);
        assertThat("Context inheritance via wrapped ExecutorService.submit(Callable) does now work properly",
                   future.get(), is(NodeContext.getContext()));

        future = executor.invokeAll(Collections.singletonList(callable)).get(0);
        assertThat("Context inheritance via wrapped ExecutorService.invokeAll does now work properly", future.get(),
                   is(NodeContext.getContext()));

        future = executor.invokeAll(Collections.singletonList(callable), 1, TimeUnit.SECONDS).get(0);
        assertThat("Context inheritance via wrapped ExecutorService.invokeAll does now work properly", future.get(),
                   is(NodeContext.getContext()));

        NodeContext ctx = executor.invokeAny(Collections.singletonList(callable));
        assertThat("Context inheritance via wrapped ExecutorService.invokeAny does now work properly", ctx,
                   is(NodeContext.getContext()));

        ctx = executor.invokeAny(Collections.singletonList(callable), 1, TimeUnit.SECONDS);
        assertThat("Context inheritance via wrapped ExecutorService.invokeAny does now work properly", ctx,
                   is(NodeContext.getContext()));

        NodeContext.removeLastContext();
    }

    /**
     * Tests if {@link SwingWorkerWithContext} works as expected.
     *
     * @throws Exception if something goes wrong
     */
    @Test
    public void testSwingWorkerWithContext() throws Exception {
        List<NodeContainer> containers = new ArrayList<NodeContainer>(wfm.getNodeContainers());

        NodeContext.pushContext(containers.get(0)); // Table Creator
        final AtomicReference<NodeContext> refInBackground = new AtomicReference<NodeContext>();
        final AtomicReference<NodeContext> refInDone = new AtomicReference<NodeContext>();
        final AtomicReference<NodeContext> refInProcess = new AtomicReference<NodeContext>();
        final ReentrantLock lock = new ReentrantLock();
        final Condition condition = lock.newCondition();

        SwingWorker<NodeContext, NodeContext> worker = new SwingWorkerWithContext<NodeContext, NodeContext>() {
            @Override
            protected NodeContext doInBackgroundWithContext() throws Exception {
                refInBackground.set(NodeContext.getContext());
                publish(NodeContext.getContext());
                return NodeContext.getContext();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void doneWithContext() {
                lock.lock();
                try {
                    refInDone.set(NodeContext.getContext());
                    condition.signalAll();
                } finally {
                    lock.unlock();
                }

            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void processWithContext(final List<NodeContext> chunks) {
                refInProcess.set(NodeContext.getContext());
            }
        };

        lock.lock();
        try {
            worker.execute();
            condition.await();
        } finally {
            lock.unlock();
        }

        assertThat("Wrong node context in doInBackgroundWithContext", refInBackground.get(),
                   is(sameInstance(NodeContext.getContext())));
        assertThat("Wrong node context in doneWithContext", refInDone.get(),
                   is(sameInstance(NodeContext.getContext())));
        assertThat("Wrong node context in processWithContext", refInProcess.get(),
                   is(sameInstance(NodeContext.getContext())));
        NodeContext.removeLastContext();
    }

    private static File findInPlugin(final String name) throws IOException {
        Bundle thisBundle = FrameworkUtil.getBundle(BatchExecutorTestcase.class);
        URL url = FileLocator.find(thisBundle, new Path(name), null);
        if (url == null) {
            throw new FileNotFoundException(thisBundle.getLocation() + name);
        }
        return new File(FileLocator.toFileURL(url).getPath());
    }
}
