/* Created on Apr 20, 2006 12:13:46 PM by thor
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   Apr 20, 2006 (thor): created
 */
package de.unikn.knime.core.util;

import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

/**
 * Testcase for the thread pool.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class ThreadPoolTest extends TestCase {
    /** A counter for the Testers. */
    static int s_count = 0;
    /** Counter for running threads. */
    final AtomicInteger m_running = new AtomicInteger(0);
    /** Counter for finished threads. */
    final AtomicInteger m_finished = new AtomicInteger(0);
    
    private class Tester implements Runnable {
        private final String m_name = "Tester " + s_count++;
        private final ThreadPool m_pool;
        
        public Tester(final ThreadPool pool) {
            m_pool = pool;
        }
        
        public void run() {
            m_running.incrementAndGet();
            System.out.println("[ " + m_name + " ] starting from pool " + m_pool);
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
        ThreadPool root = ThreadPool.getRootPool(3);
        final int loops = 200;
        
        for (int i = 1; i <= loops; i++) {
            root.submit(new Tester(root));
            System.out.println("Submitted task " + i + ", " + m_running.get() + " running threads");
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
            System.out.println("Submitted task " + i + ", " + m_running.get() + " running threads");
            if (Math.random() > 0.95) {
                root.setMaxThreads(root.getMaxThreads() + 1);
            } else if (Math.random() > 0.98) {
                root.setMaxThreads(root.getMaxThreads() - 1);
            }
        }
        root.waitForTermination();
        assertEquals(0, m_running.get());
        assertEquals(loops, m_finished.get());

        root.shutdown();
    }


    /**
     * Tests the sub pools.
     * @throws InterruptedException if the thread is interrupted
     */
    public void testSubPools() throws InterruptedException {
        ThreadPool root = ThreadPool.getRootPool(20);
        ThreadPool[] pools = new ThreadPool[4];
        
        pools[0] = root;
        pools[1] = root.createSubPool(20);
        pools[2] = root.createSubPool(5);
        pools[3] = pools[1].createSubPool(10);
        
        final int loops = 200;
        
        for (int i = 1; i <= loops; i++) {
            final int k = (int) (Math.random() * pools.length);
            
            pools[k].submit(new Tester(pools[k]));
            System.out.println("Submitted task " + i + ", " + m_running.get() + " running threads");
            if (i % 30 == 0) {
                pools[k].setMaxThreads(pools[k].getMaxThreads() + 1);
            }
            
            for (int m = 0; m < pools.length; m++) {
                assertTrue(pools[m].getRunningThreads() <= pools[m].getMaxThreads());
            }
        }
        
        root.waitForTermination();
        assertEquals(0, m_running.get());
        assertEquals(loops, m_finished.get());
    }

}
