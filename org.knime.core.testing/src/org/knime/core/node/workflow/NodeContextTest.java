/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   19.06.2013 (thor): created
 */
package org.knime.core.node.workflow;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.FileUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Testcases for {@link NodeContext}.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
public class NodeContextTest {
    private static WorkflowManager wfm;

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
    }

    /**
     * Check that the node context stack is empty before each test method.
     */
    @Before
    public void checkForEmptyContextStackBefore() {
        NodeContext currentContext = NodeContext.getContext();

        // this is just to clean up if a test method leaves a filled stack so that the next method has an empty stack
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
        List<NodeContainer> containers = new ArrayList<>(wfm.getNodeContainers());

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
        List<NodeContainer> containers = new ArrayList<>(wfm.getNodeContainers());

        WorkflowManager metaNode = (WorkflowManager)containers.get(2); // this is the metanode
        containers = new ArrayList<>(metaNode.getNodeContainers());
        NodeContext.pushContext(containers.get(0)); // Shuffle in metanode
        assertThat("Unexpected node container on top of context stack", NodeContext.getContext().getNodeContainer(),
                   is(containers.get(0)));
        assertThat("Unexpected workflow manager", NodeContext.getContext().getWorkflowManager(), is(wfm));
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
        List<NodeContainer> containers = new ArrayList<>(wfm.getNodeContainers());

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
     * Test context inheritance to new threads.
     *
     * @throws InterruptedException if an error occurs
     */
    @Test
    public void testContextInheritance() throws InterruptedException {
        final AtomicReference<NodeContext> threadContext = new AtomicReference<NodeContext>();
        List<NodeContainer> containers = new ArrayList<>(wfm.getNodeContainers());

        NodeContext.pushContext(containers.get(0)); // Table Creator
        Thread t = new Thread() {
            @Override
            public void run() {
                threadContext.set(NodeContext.getContext());
            }
        };
        t.start();
        t.join();

        assertThat("Context inheritance to new thread does now work properly", threadContext.get(),
                   is(NodeContext.getContext()));
        NodeContext.removeLastContext();
    }

    /**
     * Test context transfer to thread pool jobs.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testContextInThreadpool() throws Exception {
        List<NodeContainer> containers = new ArrayList<>(wfm.getNodeContainers());

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
        List<NodeContainer> containers = new ArrayList<>(wfm.getNodeContainers());

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

    private static File findInPlugin(final String name) throws IOException {
        Bundle thisBundle = FrameworkUtil.getBundle(BatchExecutorTestcase.class);
        URL url = FileLocator.find(thisBundle, new Path(name), null);
        if (url == null) {
            throw new FileNotFoundException(thisBundle.getLocation() + name);
        }
        return new File(FileLocator.toFileURL(url).getPath());
    }
}
