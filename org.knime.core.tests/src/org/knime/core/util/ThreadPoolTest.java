/*
 * ------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   Apr 20, 2006 (meinl): created
 */
package org.knime.core.util;

import static org.junit.Assert.assertThat;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.osgi.internal.framework.ContextFinder;
import org.hamcrest.core.Is;
import org.knime.core.node.NodeLogger;

import junit.framework.TestCase;

/**
 * Testcase for the thread pool.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class ThreadPoolTest extends TestCase {
    private static final int LOOPS = 20;
    private static final int LOOPS_MODULO = LOOPS / 5;
    /** A counter for the Testers. */
    private static int count = 0;
    /** Counter for running threads. */
    private final AtomicInteger m_running = new AtomicInteger(0);
    /** Counter for finished threads. */
    private final AtomicInteger m_finished = new AtomicInteger(0);

    private class Tester implements Runnable {
        private final String m_name = "Tester " + count++;
        private final ThreadPool m_pool;

        /**
         * Creates a new tester.
         *
         * @param pool a thread pool
         */
        public Tester(final ThreadPool pool) {
            m_pool = pool;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            m_running.incrementAndGet();
            NodeLogger.getLogger(ThreadPoolTest.class).info("[ " + m_name + " ] starting from pool " + m_pool);
            try {
                ack(3, 10);
                Thread.sleep((int) (Math.random() * 300));
            } catch (InterruptedException ex) {
                NodeLogger.getLogger(ThreadPoolTest.class).warn(ex.getMessage(), ex);
            }
            NodeLogger.getLogger(ThreadPoolTest.class).info("[ " + m_name + " ] finished");
            m_running.decrementAndGet();
            m_finished.incrementAndGet();
        }

        // Guess what it does ;-)
        private int ack(final int n, final int m) {
            if (n == 0) {
                return m + 1;
            } else if (m == 0) {
                return ack(n - 1, 1);
            } else {
                return ack(n - 1, ack(n, m - 1));
            }
        }

    }

    /**
     * Tests the root pool.
     * @throws InterruptedException if the thread is interrupted
     */
    public void testRootPool() throws InterruptedException {
        ThreadPool root = new ThreadPool(3);
        final int loops = LOOPS;

        for (int i = 1; i <= loops; i++) {
            root.submit(new Tester(root));
            NodeLogger.getLogger(ThreadPoolTest.class).info("Submitted task " + i + ", " + m_running.get()
                                                                    + " running threads");
            if (i % LOOPS_MODULO == 0) {
                root.setMaxThreads(root.getMaxThreads() + 1);
            }

            assertTrue(root.getRunningThreads() <= root.getMaxThreads());
        }

        root.waitForTermination();
        assertEquals(0, m_running.get());
        assertEquals(loops, m_finished.get());


        m_running.set(0);
        m_finished.set(0);
        root.setMaxThreads(3);
        for (int i = 1; i <= loops; i++) {
            root.submit(new Tester(root));
            NodeLogger.getLogger(ThreadPoolTest.class).info("Submitted task " + i + ", " + m_running.get()
                                                                    + " running threads");
            if (Math.random() > 0.95) {
                root.setMaxThreads(root.getMaxThreads() + 1);
            } else if ((root.getMaxThreads() > 1) && (Math.random() > 0.98)) {
                root.setMaxThreads(root.getMaxThreads() - 1);
            }
        }
        root.waitForTermination();
        assertEquals(0, m_running.get());
        assertEquals(loops, m_finished.get());

        root.shutdown();
    }


    /**
     * Tests if invisible threads work with the root pool.
     *
     * @throws InterruptedException if the thread is interrupted
     */
    public void testRootInvisible() throws InterruptedException {
        final ThreadPool root = new ThreadPool(3);
        final int loops = LOOPS;

        final Callable<?> submitter = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                for (int i = 1; i <= loops; i++) {
                    try {
                        root.submit(new Tester(root));
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    NodeLogger.getLogger(ThreadPoolTest.class).info("Submitted task " + i + ", " + m_running.get()
                                                                            + " running threads");
                    if (i % LOOPS_MODULO == 0) {
                        root.setMaxThreads(root.getMaxThreads() + 1);
                    }

                    assertTrue(m_running.get() <= root.getMaxThreads());
                    assertTrue(root.getRunningThreads()
                            <= root.getMaxThreads());
                }
                return null;
            }
        };

        Runnable main = new Runnable() {
            @Override
            public void run() {
                try {
                    root.runInvisible(submitter);
                } catch (ExecutionException ex) {
                    ex.printStackTrace();
                } catch (final InterruptedException e) {
                    return;
                }
            }
        };

        root.submit(main);

        root.waitForTermination();
        assertEquals(0, m_running.get());
        assertEquals(loops, m_finished.get());
    }

    /**
     * Tests that the invisible method blocks at the end to reacquire a free slot.
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    public void testInvisibleBlocks() throws InterruptedException, ExecutionException, TimeoutException {
        final var poolSize = 2;
        final var pool = new ThreadPool(poolSize);

        // three tasks for two workers, all blocking at their condition
        final var task1 = submitTask(pool, false);
        final var task2 = submitTask(pool, true);
        final var task3 = submitTask(pool, false);
        Thread.sleep(100);
        assertEquals("Unexpected number of running workers", poolSize, pool.getRunningThreads());
        assertEquals("Expected task 3 to be queued", 1, pool.getQueueSize());
        // task 2 gets invisible, blocking on next condition
        try {
            task2.lock.lock();
            task2.conditions[0].signal();
        } finally {
            task2.lock.unlock();
        }
        Thread.sleep(100);
        // third task gets scheduled on additional worker
        assertEquals("Unexpected number of running workers", poolSize, pool.getRunningThreads());
        assertEquals("Expected no task to be queued", 0, pool.getQueueSize());
        // invisible task finishes and wants to re-acquire slot
        try {
            task2.lock.lock();
            task2.conditions[1].signal();
        } finally {
            task2.lock.unlock();
        }
        Thread.sleep(100);
        assertFalse("Expected task 2 to not be finished", task2.task.isDone() || task2.task.isCancelled());
        // worker with task 1 or 3 finishes and releases slot
        try {
            task1.lock.lock();
            task1.conditions[0].signal();
        } finally {
            task1.lock.unlock();
        }
        // invisible task can reacquire slot and finish (return value)
        assertTrue("Expected task2 to finish", task2.task.get(2, TimeUnit.SECONDS));
        try {
            task3.lock.lock();
            task3.conditions[0].signal();
        } finally {
            task3.lock.unlock();
        }

        task1.task.cancel(true);
        task3.task.cancel(true);

        pool.waitForTermination();
        pool.shutdown();
    }

    private record Task(Future<Boolean> task, ReentrantLock lock, Condition[] conditions) {}

    /**
     * Submits a task to the pool that blocks on the returned conditions in order, the second one being in an invisible
     * portion.
     *
     * @param pool pool to submit to
     * @return condition where the task blocks
     * @throws InterruptedException
     */
    private static Task submitTask(final ThreadPool pool, final boolean invisible) throws InterruptedException {
        final var lock = new ReentrantLock();
        final var taskProceed = lock.newCondition();
        final var taskInvis = lock.newCondition();
        final var task = new Callable<Boolean> () {
            @Override
            public Boolean call() throws Exception {
                lock.lock();
                try {
                    taskProceed.await();
                    if (invisible) {
                        pool.runInvisible(() -> {
                            taskInvis.await();
                            return true;
                        });
                    }
                } finally {
                    lock.unlock();
                }
                return true;
            }
        };
        final var result = pool.enqueue(task);
        return new Task(result, lock, invisible ? new Condition[]{taskProceed, taskInvis} : new Condition[]{taskProceed});
    }

    /**
     * Tests the sub pools.
     * @throws InterruptedException if the thread is interrupted
     */
    public void testSubPools() throws InterruptedException {
        ThreadPool root = new ThreadPool(20);
        ThreadPool[] pools = new ThreadPool[4];

        pools[0] = root;
        pools[1] = root.createSubPool(20);
        pools[2] = root.createSubPool(5);
        pools[3] = pools[1].createSubPool(10);

        final int loops = LOOPS;

        for (int i = 1; i <= loops; i++) {
            final int k = (int) (Math.random() * pools.length);

            pools[k].submit(new Tester(pools[k]));
            NodeLogger.getLogger(ThreadPoolTest.class).info("Submitted task " + i + ", " + m_running.get()
                                                                    + " running threads");
            if (i % LOOPS_MODULO == 0) {
                pools[k].setMaxThreads(pools[k].getMaxThreads() + 1);
            }

            for (int m = 0; m < pools.length; m++) {
                assertTrue(pools[m].getRunningThreads()
                        <= pools[m].getMaxThreads());
            }
        }

        root.waitForTermination();
        assertEquals(0, m_running.get());
        assertEquals(loops, m_finished.get());
    }


    /**
     * Tests if invisible threads work with sub pools.
     *
     * @throws InterruptedException if the thread is interrupted
     */
    public void testSubInvisible() throws InterruptedException {
        final ThreadPool root = new ThreadPool(10);
        final ThreadPool sub1 = root.createSubPool(6);
        final ThreadPool sub2 = root.createSubPool(6);
        final int loops = LOOPS;

        final Callable<?> submitter = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                for (int i = 1; i <= loops; i++) {
                    if (Math.random() > 0.5) {
                        try {
                            sub1.submit(new Tester(sub1));
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        try {
                            sub2.submit(new Tester(sub2));
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                    NodeLogger.getLogger(ThreadPoolTest.class).info("Submitted task " + i + ", " + m_running.get()
                                                                            + " running threads");
                    assertTrue(root.getRunningThreads()
                            <= root.getMaxThreads());
                }
                return null;
            }
        };

        Runnable main = new Runnable() {
            @Override
            public void run() {
                NodeLogger.getLogger(ThreadPoolTest.class).info("Running invisible");
                try {
                    sub2.runInvisible(submitter);
                    NodeLogger.getLogger(ThreadPoolTest.class).info("Finished invisible");
                } catch (ExecutionException ex) {
                    ex.printStackTrace();
                } catch (final InterruptedException e) {
                    return;
                }
            }
        };

        sub1.submit(main);

        root.waitForTermination();
        assertEquals(0, m_running.get());
        assertEquals(loops, m_finished.get());
    }


    /**
     * Tests if the root pool handles submit and enqueue correctly.
     * @throws InterruptedException if the thread is interrupted
     */
    public void testRootEnqueue() throws InterruptedException {
        ThreadPool root = new ThreadPool(3);
        final int loops = LOOPS;

        for (int i = 1; i <= loops; i++) {
            if (Math.random() > 0.4) {
                root.enqueue(new Tester(root));
            } else {
                root.submit(new Tester(root));
            }
            NodeLogger.getLogger(ThreadPoolTest.class).info("Submitted task " + i + ", " + m_running.get()
                                                                    + " running threads, " + root.getQueueSize()
                                                                    + " queued jobs");
            if (i % LOOPS_MODULO == 0) {
                root.setMaxThreads(root.getMaxThreads() + 1);
            }

            assertTrue(m_running.get() <= root.getMaxThreads());
            assertTrue(root.getRunningThreads() <= root.getMaxThreads());
        }

        root.waitForTermination();
        assertEquals(0, m_running.get());
        assertEquals(loops, m_finished.get());


        m_running.set(0);
        m_finished.set(0);
        root.setMaxThreads(3);
        for (int i = 1; i <= loops; i++) {
            if (Math.random() > 0.4) {
                root.enqueue(new Tester(root));
            } else {
                root.submit(new Tester(root));
            }

            NodeLogger.getLogger(ThreadPoolTest.class).info("Submitted task " + i + ", " + m_running.get()
                                                                    + " running threads, " + root.getQueueSize()
                                                                    + " queued jobs");
            if (Math.random() > 0.95) {
                root.setMaxThreads(root.getMaxThreads() + 1);
            } else if ((root.getMaxThreads() > 1) && (Math.random() > 0.98)) {
                root.setMaxThreads(root.getMaxThreads() - 1);
            }
        }
        root.waitForTermination();
        assertEquals(0, m_running.get());
        assertEquals(loops, m_finished.get());

        root.shutdown();
    }


    /**
     * Tests if sub pools handle submit and enqueue correctly.
     * @throws InterruptedException if the thread is interrupted
     */
    public void testSubEnqueue() throws InterruptedException {
        ThreadPool root = new ThreadPool(20);
        ThreadPool[] pools = new ThreadPool[4];

        pools[0] = root;
        pools[1] = root.createSubPool(20);
        pools[2] = root.createSubPool(5);
        pools[3] = pools[1].createSubPool(10);

        final int loops = LOOPS;

        for (int i = 1; i <= loops; i++) {
            final int k = (int) (Math.random() * pools.length);

            if (Math.random() > 0.4) {
                pools[k].enqueue(new Tester(pools[k]));
            } else {
                pools[k].submit(new Tester(pools[k]));
            }
            NodeLogger.getLogger(ThreadPoolTest.class).info("Submitted task " + i + ", " + m_running.get()
                                                                    + " running threads, " + root.getQueueSize()
                                                                    + " queued jobs");
            if (i % LOOPS_MODULO == 0) {
                pools[k].setMaxThreads(pools[k].getMaxThreads() + 1);
            }

            for (int m = 0; m < pools.length; m++) {
                assertTrue(pools[m].getRunningThreads()
                        <= pools[m].getMaxThreads());
            }
            assertTrue(m_running.get() <= root.getMaxThreads());
        }

        root.waitForTermination();
        assertEquals(0, m_running.get());
        assertEquals(loops, m_finished.get());
    }

    /**
     * Checks if the context classloader is set correctly for the threads in the pool.
     *
     * @throws Exception if an error occurs
     */
    public void testContextClassloader() throws Exception {
        ThreadPool root = new ThreadPool(1);

        Callable<ClassLoader> callable = new Callable<ClassLoader>() {
            @Override
            public ClassLoader call() throws Exception {
                return Thread.currentThread().getContextClassLoader();
            }
        };

        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader outerCl = new ContextFinder(cl, null);
            Thread.currentThread().setContextClassLoader(outerCl);
            Future<ClassLoader> future = root.enqueue(callable);
            ClassLoader innerCl = future.get();
            assertThat("Unexpected classloader in thread pool", innerCl, Is.is(outerCl));
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }
}
