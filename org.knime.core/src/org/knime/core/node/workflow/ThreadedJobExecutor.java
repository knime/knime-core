/*
 * ------------------------------------------------------------------ *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Apr 16, 2007 (mb): created
 */
package org.knime.core.node.workflow;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.knime.core.node.NodeLogger;

/** Provide a fixed pool of threads to run jobs in.
 * 
 * @author B. Wiswedel & M. Berthold, University of Konstanz
 */
public class ThreadedJobExecutor implements JobExecutor {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(ThreadedJobExecutor.class);

    private class Worker extends Thread {
        private JobRunnable m_myRunnable;
        private boolean commitSuicide = false;

        Worker(final String name) {
            super(name);
            m_myRunnable = null;
        }
        
        synchronized boolean hasJob() {
            return m_myRunnable != null;
        }

        synchronized void setNewJob(final JobRunnable jr) {
            m_myRunnable = jr;
            this.notifyAll();
        }
        
        @Override
        public void run() {
            while (true) {
                synchronized(this) {
                    if (m_myRunnable == null) {
                        try {
                            wait();
                        } catch (InterruptedException ie) {
                            assert false;
                        }
                    }
                    if (commitSuicide) {
                        return;
                    }
                }
                m_myRunnable.run();  // MUST be unsynchronized (other threads
                                     // may be submitted inside this call!)
                m_myRunnable = null;
                ThreadedJobExecutor.this.newWorkerAvailable(this);
                if (commitSuicide) {
                    return;
                }
            }
        }
    }
    
    private Worker[] m_worker;
    private Queue<JobRunnable> m_jobQueue =
        new ConcurrentLinkedQueue<JobRunnable>();

    ThreadedJobExecutor(final int maxNrThreads) {
        m_worker = new Worker[maxNrThreads];
        for (int i = 0; i < m_worker.length; i++) {
            m_worker[i] = null;
        }
    }
    
    synchronized void newJobAvailable(final JobRunnable r) {
        m_jobQueue.add(r);
        for (int i = 0; i < m_worker.length; i++) {
            if (m_worker[i] == null) {
                m_worker[i] = new Worker("KNIME-Worker (" + i + ")");
                m_worker[i].setUncaughtExceptionHandler(
                        new UncaughtExceptionHandler() {
                    /** {@inheritDoc} */
                    public void uncaughtException(
                            final Thread t, final Throwable e) {
                        LOGGER.error(t.getName() + " terminated with uncaught "
                                + e.getClass().getSimpleName(), e);
                    }
                    
                });
                m_worker[i].start();
                m_worker[i].setNewJob(m_jobQueue.remove());
                return;
            }
            Worker w = m_worker[i];
            if (!w.hasJob()) {
                m_worker[i].setNewJob(m_jobQueue.remove());
                return;
            }
        }
    }
    
    private synchronized void newWorkerAvailable(final Worker w) {
        boolean queueEmpty = m_jobQueue.isEmpty();
        if (!queueEmpty) {
            w.setNewJob(m_jobQueue.remove());
        } else {
            for (int i = 0; i < m_worker.length; i++) {
                if (m_worker[i] != null) {
                    if (m_worker[i].hasJob()) {
                        return;
                    }
                }
            }
            // all jobs idle: kill them
            for (int i = 0; i < m_worker.length; i++) {
                if (m_worker[i] != null) {
                    m_worker[i].commitSuicide = true;
                    m_worker[i].setNewJob(null);
                    m_worker[i] = null;
                }
            }
        }
    }
    
    public synchronized void cancelJob(JobID id) {
        // try to find the job in the queued ones
        for (Iterator<JobRunnable> it = m_jobQueue.iterator(); it.hasNext();) {
            JobRunnable r = it.next();
            if (r.getJobID().equals(id)) {
                it.remove();
            }
        }
        // otherwise try to find out if one of the workers is working on this ID
        for (int i = 0; i < m_worker.length; i++) {
            if (m_worker[i] != null) {
                if (m_worker[i].hasJob()) {
                    if (m_worker[i].m_myRunnable.getJobID().equals(id)) {
                        m_worker[i].m_myRunnable.triggerCancel();
                    }
                }
            }
        }
    }

    public synchronized JobID submitJob(JobRunnable r) {
        JobID id = JobID.createNewID();
        r.setJobID(id);
        newJobAvailable(r);
        return id;
    }
    
   
}
