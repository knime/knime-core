/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 *
 * History
 *   Apr 20, 2006 (meinl): created
 */
package org.knime.core.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

/**
 * Testcase for the thread pool.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class ThreadPoolTest extends TestCase {
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
            System.out.println("[ " + m_name + " ] starting from pool "
                    + m_pool);
            try {
                ack(3, 10);
                Thread.sleep((int) (Math.random() * 300));
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            System.out.println("[ " + m_name + " ] finished");
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
        final int loops = 200;

        for (int i = 1; i <= loops; i++) {
            root.submit(new Tester(root));
            System.out.println("Submitted task " + i + ", " + m_running.get()
                    + " running threads");
            if (i % 30 == 0) {
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
            System.out.println("Submitted task " + i + ", " + m_running.get()
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
        final int loops = 200;

        final Callable<?> submitter = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                for (int i = 1; i <= loops; i++) {
                    try {
                        root.submit(new Tester(root));
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    System.out.println("Submitted task " + i + ", "
                            + m_running.get() + " running threads");
                    if (i % 30 == 0) {
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
                }
            }
        };

        root.submit(main);

        root.waitForTermination();
        assertEquals(0, m_running.get());
        assertEquals(loops, m_finished.get());
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

        final int loops = 200;

        for (int i = 1; i <= loops; i++) {
            final int k = (int) (Math.random() * pools.length);

            pools[k].submit(new Tester(pools[k]));
            System.out.println("Submitted task " + i + ", " + m_running.get()
                    + " running threads");
            if (i % 30 == 0) {
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
        final int loops = 200;

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
                    System.out.println("Submitted task " + i + ", "
                            + m_running.get() + " running threads");
                    assertTrue(root.getRunningThreads()
                            <= root.getMaxThreads());
                }
                return null;
            }
        };

        Runnable main = new Runnable() {
            @Override
            public void run() {
                System.out.println("Running invisible");
                try {
                    sub2.runInvisible(submitter);
                    System.out.println("Finished invisible");
                } catch (ExecutionException ex) {
                    ex.printStackTrace();
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
        final int loops = 200;

        for (int i = 1; i <= loops; i++) {
            if (Math.random() > 0.4) {
                root.enqueue(new Tester(root));
            } else {
                root.submit(new Tester(root));
            }
            System.out.println("Submitted task " + i + ", " + m_running.get()
                    + " running threads, " + root.getQueueSize()
                    + " queued jobs");
            if (i % 30 == 0) {
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

            System.out.println("Submitted task " + i + ", " + m_running.get()
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

        final int loops = 200;

        for (int i = 1; i <= loops; i++) {
            final int k = (int) (Math.random() * pools.length);

            if (Math.random() > 0.4) {
                pools[k].enqueue(new Tester(pools[k]));
            } else {
                pools[k].submit(new Tester(pools[k]));
            }
            System.out.println("Submitted task " + i + ", " + m_running.get()
                    + " running threads, " + root.getQueueSize()
                    + " queued jobs");
            if (i % 30 == 0) {
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
}
