/* Created on Apr 20, 2006 9:08:05 AM by thor
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

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public abstract class ThreadPool  {
    /** A counter for workers for naming them. */
    static int s_workerCount = 0;
    /**
     * A simple worker thread that waits for {@link Runnable}s and executes them.
     * 
     * @author Thorsten Meinl, University of Konstanz
     */
    protected class Worker extends Thread {
        private Runnable m_runnable;
        private final Object m_myLock = new Object();
        private ThreadPool m_startedFrom;
        
        /**
         * Creates a new worker.
         */
        public Worker() {
            super("Pool-Worker-" + s_workerCount++);
            setPriority(NORM_PRIORITY - 1);
        }
        /** 
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            while (!isInterrupted()) {
                synchronized (m_myLock) {
                    if (m_runnable == null) {
                        try {
                            m_myLock.wait();
                        } catch (InterruptedException ex) {
                            return;
                        }
                    }
                    
                    m_runnable.run();
                    
                    m_startedFrom.workerDone(this);
                    m_runnable = null;
                }
            }
        }
        
        /**
         * Sets the runnable for this (sleeping) workers and awakes it. This method waits until the worker has finished
         * the previous task if it is currently executing one.
         * 
         * @param r the Runnable to run
         * @param startedFrom the pool from which the worker is taken from
         */
        public void setRunnable(final Runnable r, final ThreadPool startedFrom) {
            synchronized (m_myLock) {
                m_runnable = r;
                m_startedFrom = startedFrom;
                m_myLock.notifyAll();
            }
        }
    }
    
    
    /** A Map of workers currently running in this pool. Keys and values are the same objects. */
    protected final Set<Worker> m_runningWorkers = new HashSet<Worker>();
    /** The maximum allowed number of running workers. */
    protected int m_maxWorkers;
    /** A global lock to synchronize access to the running workers. */
    protected final ReentrantLock m_lock = new ReentrantLock();
    /** A notifier that is signalled if a worker finishes. */
    protected final Condition m_workerFinished = m_lock.newCondition();
    
    
    /**
     * Creates a new ThreadPool with a maximum number of threads.
     * @param maxThreads the maximum number of threads
     */
    public ThreadPool(final int maxThreads) {
        m_maxWorkers = maxThreads;
    }


    /**
     * Submits a Runnable task for execution and returns a Future representing that task. The method blocks until a
     * free thread is available.
     *
     * @param task the task to submit
     * @return a Future representing pending completion of the task, and whose <tt>get()</tt> method will return
     * <tt>null</tt> upon completion.
     * @throws NullPointerException if task null
     */
    public Future<?> submit(final Runnable task) {
        if (task == null) { throw new NullPointerException(); }
        FutureTask<Object> ftask = new FutureTask<Object>(task, null);
        execute(ftask);
        return ftask;
    }

    
    /**
     * Submits a Runnable task for execution and returns a Future representing that task that will upon completion
     * return the given result. The method blocks until a free thread is available.
     *
     * @param task the task to submit
     * @param result the result to return
     * @param <T> any result type
     * @return a Future representing pending completion of the task, and whose <tt>get()</tt> method will return the
     * given result upon completion.
     * @throws NullPointerException if task null     
     */
    public <T> Future<T> submit(final Runnable task, final T result) {
        if (task == null) { throw new NullPointerException(); }
        FutureTask<T> ftask = new FutureTask<T>(task, result);
        execute(ftask);
        return ftask;
    }

    
    /**
     * Submits a value-returning task for execution and returns a Future representing the pending results of the task.
     * The method blocks until a free thread is available. 
     *
     * <p>
     * If you would like to immediately block waiting
     * for a task, you can use constructions of the form
     * <tt>result = exec.submit(aCallable).get();</tt>
     *
     * <p> Note: The {@link Executors} class includes a set of methods
     * that can convert some other common closure-like objects,
     * for example, {@link java.security.PrivilegedAction} to
     * {@link Callable} form so they can be submitted.
     *
     * @param task the task to submit
     * @param <T> any result type
     * @return a Future representing pending completion of the task
     * @throws NullPointerException if task null
     */
    public <T> Future<T> submit(final Callable<T> task) {
        if (task == null) { throw new NullPointerException(); }
        FutureTask<T> ftask = new FutureTask<T>(task);
        execute(ftask);
        return ftask;
    }
    
    /**
     * Executes the runnable in a free thread. The method blocks until a thread is available.
     * 
     * @param r a runnable.
     */
    private void execute(final Runnable r) {
        Worker w = getWorker();
        if (w != null) {
            w.setRunnable(r, this);
        }
    }
    

    /**
     * Returns the maximum number of threads in the pool.
     * @return the maximum thread number
     */
    public int getMaxThreads() { return m_maxWorkers; }
    
    
    /**
     * Sets the maximum number of threads in the pool. If the new value is smaller than the old value running
     * surplus threads will not be interrupted.
     * @param newValue the new maximum thread number
     */
    public void setMaxThreads(final int newValue) {
        m_maxWorkers = newValue;
    }

    /**
     * Returns the number of currently running threads in this pool and its sub pools.
     * @return the number of running threads
     */
    public int getRunningThreads() {
        return m_runningWorkers.size();
    }
    
    
    /**
     * Waits until all jobs in this pool and its sub pools have been finished.
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public void waitForTermination() throws InterruptedException {
        ReentrantLock lock = m_lock;
        lock.lock();
        
        try {
            while (m_runningWorkers.size() > 0) {
                m_workerFinished.await();
            }
        } finally {
            lock.unlock();
        }
    }
    
    
    /**
     * Shuts the pool down, still running threads are not interrupted.
     */
    public void shutdown() {
        setMaxThreads(0);
    }

    
    /** 
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        shutdown();
        super.finalize();
    }


    /**
     * This method is called every time a worker has finished.
     * @param w the finished worker
     */
    abstract void workerDone(final Worker w);
    
    
    /**
     * Tries to get a worker from the pool. This method blocks until a free worker is available.
     * @return a free worker
     */
    protected abstract Worker getWorker();
    
    
    /**
     * Creates a sub pool that shares the workers with this (parent) pool. The maximum number of threads in this and
     * all its sub pools does not exceed the maximum thread number for this pool, even if a sub pool is created with
     * a higher thread count.
     * 
     * @param maxThreads the maximum number of threads in the sub pool
     * @return a thread pool
     */
    public abstract ThreadPool createSubPool(final int maxThreads);
    
    
    /**
     * Returns a root pool that is the top of a hierarchy of thread pools.
     * 
     * @param maxThreads the maximum number of threads in the pool (and all its sub pools) 
     * @return a new thread pool
     */
    public static ThreadPool getRootPool(final int maxThreads) {
        return new RootPool(maxThreads);
    }
    
    
    private static class RootPool extends ThreadPool {
        private final Queue<Worker> m_availableWorkers = new ConcurrentLinkedQueue<Worker>(); 
       
        /**
         * Creates a new root pool.
         * @param maxThreads the maximum number of threads
         */
        public RootPool(final int maxThreads) {
            super(maxThreads);
        }

        /**
         * @see de.unikn.knime.dev.node.parallel.ThreadPool#getWorker()
         */
        @Override
        protected Worker getWorker() {
            Worker w = null;
            ReentrantLock lock = m_lock;
            lock.lock();            
            
            try {
                do {
                    if (m_runningWorkers.size() < m_maxWorkers) {
                        if ((w = m_availableWorkers.poll()) == null) {
                            w = new Worker();
                            w.start();
                        }
                    } else {
                        try {
                            m_workerFinished.await();
                        } catch (InterruptedException ex) {
                            return null;
                        }
                    }
                } while (w == null);
                
                m_runningWorkers.add(w);
            } finally {
                lock.unlock();
            }
            
            return w;
        }

        /** 
         * @see de.unikn.knime.dev.node.parallel.ThreadPool#setMaxThreads(int)
         */
        @Override
        public void setMaxThreads(final int newValue) {
            if (newValue < m_maxWorkers) {
                ReentrantLock lock = m_lock;
                lock.lock();
                try {
                    for (int i = (m_maxWorkers - newValue); (i >= 0) && m_availableWorkers.size() > 0; i--) {
                        Worker w = m_availableWorkers.remove();
                        w.interrupt();
                    }
                } finally {
                    lock.unlock();
                }
            }
            
            super.setMaxThreads(newValue);
        }
        
        
        /** 
         * @see de.unikn.knime.dev.node.parallel.ThreadPool
         * #workerDone(de.unikn.knime.dev.node.parallel.ThreadPool.Worker)
         */
        @Override
        void workerDone(final Worker w) {
            ReentrantLock lock = m_lock;
            lock.lock();
            try {
                m_runningWorkers.remove(w);                

                if (m_availableWorkers.size() + m_runningWorkers.size() < m_maxWorkers) {
                    m_availableWorkers.add(w);
                } else {
                    w.interrupt();
                }
                
                m_workerFinished.signalAll();
            } finally {
                lock.unlock();
            }
        }

        /** 
         * @see de.unikn.knime.dev.node.parallel.ThreadPool#createSubPool(int)
         */
        @Override
        public ThreadPool createSubPool(final int maxThreads) {
            return new SubPool(maxThreads, this);
        }
    }
    
    
    private static class SubPool extends ThreadPool {
        private final ThreadPool m_parent;
        
        /**
         * Creates a new sub pool.
         * 
         * @param maxThreads the maximum number of threads in the pool
         * @param parent the parent pool
         */
        public SubPool(final int maxThreads, final ThreadPool parent) {
            super(maxThreads);
            m_parent = parent;
        }

        /**
         * @see de.unikn.knime.dev.node.parallel.ThreadPool#getWorker()
         */
        @Override
        protected Worker getWorker() {
            Worker w = null;
            ReentrantLock lock = m_lock;
            lock.lock();            
            
            try {
                do {
                    if (m_runningWorkers.size() < m_maxWorkers) {
                        w = m_parent.getWorker();
                    } else {
                        try {
                            m_workerFinished.await();
                        } catch (InterruptedException ex) {
                            return null;
                        }
                    }
                } while (w == null);
                
                m_runningWorkers.add(w);
            } finally {
                lock.unlock();
            }
            
            return w;
        }

        
        /**
         * @see de.unikn.knime.dev.node.parallel.ThreadPool#createSubPool(int)
         */
        @Override
        public ThreadPool createSubPool(final int maxThreads) {
            return new SubPool(maxThreads, this);
        }

        
        /** 
         * @see de.unikn.knime.dev.node.parallel.ThreadPool
         * #workerDone(de.unikn.knime.dev.node.parallel.ThreadPool.Worker)
         */
        @Override
        void workerDone(final Worker w) {
            ReentrantLock lock = m_lock;
            lock.lock();
            try {
                m_runningWorkers.remove(w);
                m_parent.workerDone(w);
                m_workerFinished.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }
}
