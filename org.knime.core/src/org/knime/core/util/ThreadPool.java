/*
 * ------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   Apr 25, 2006 (meinl): created
 *   11.05.2006 (wiswedel, ohl) reviewed
 */
package org.knime.core.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.knime.core.node.NodeLogger;

/**
 * Implements a sophisticated thread pool.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class ThreadPool {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(ThreadPool.class);

    private class MyFuture<T> extends FutureTask<T> {

        /**
         * @see FutureTask#FutureTask(Callable)
         */
        public MyFuture(final Callable<T> callable) {
            super(callable);
        }

        /**
         * Creates a <tt>FutureTask</tt> that will upon running, execute the
         * given <tt>Runnable</tt>, and arrange that <tt>get</tt> will
         * return the given result on successful completion.
         *
         * @param runnable the runnable task
         * @param result the result to return on successful completion. If you
         *            don't need a particular result, consider using
         *            constructions of the form: <tt>Future&lt;?&gt; f =
         *            new FutureTask&lt;Object&gt;(runnable, null)</tt>
         * @throws NullPointerException if runnable is null
         */
        public MyFuture(final Runnable runnable, final T result) {
            super(runnable, result);
        }

        /**
         * Returns the pool in which this future has been created.
         *
         * @return a thread pool
         */
        public ThreadPool getPool() {
            return ThreadPool.this;
        }
    }

    private static int workerCounter;

    private class Worker extends Thread {
        private final Object m_lock = new Object();

        private Runnable m_runnable;

        private ThreadPool m_startedFrom;

        private boolean m_stopped;

        /**
         * Creates a new worker.
         */
        public Worker() {
            super("KNIME-Worker-" + workerCounter++);
            setPriority(Thread.MIN_PRIORITY + 2);
            setDaemon(true);
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                ThreadPool startedFrom;
                synchronized (m_lock) {
                    if (m_runnable == null) {
                        try {
                            m_lock.wait(90 * 1000);
                            if (m_runnable == null) {
                                // then the timeout has occured
                                // and we end the thread
                                m_stopped = true;
                                return;
                            }
                        } catch (InterruptedException ex) {
                            return;
                        }
                    }
                    startedFrom = m_startedFrom;

                    try {
                        m_runnable.run();
                    } catch (Exception ex) {
                        // prevent the worker from being terminated
                        LOGGER.error("An exception occurred while executing "
                                + "a runnable.", ex);
                    }
                    m_runnable = null;
                }
                startedFrom.workerFinished(this);
            }
        }

        /**
         * Sets the runnable for this (sleeping) worker and awakes it. This
         * method waits until the worker has finished the previous task if it is
         * currently executing one.
         *
         * @param r the Runnable to run
         * @param pool the pool from which the worker is taken from
         * @return <code>true</code> if the worker has been woken up,
         * <code>false</code> if not because the thread has already died
         */
        public boolean wakeup(final Runnable r, final ThreadPool pool) {
            synchronized (m_lock) {
                if (m_stopped) {
                    return false;
                }
                m_runnable = r;
                m_startedFrom = pool;
                m_lock.notifyAll();
            }
            return true;
        }
    }

    private final Queue<Worker> m_availableWorkers;

    private volatile int m_maxThreads, m_invisibleThreads;

    private final ThreadPool m_parent;

    private final Queue<MyFuture<?>> m_queuedFutures;

    private final Set<Worker> m_runningWorkers = new HashSet<Worker>();

    /**
     * Creates a new ThreadPool with a maximum number of threads.
     *
     * @param maxThreads the maximum number of threads
     */
    public ThreadPool(final int maxThreads) {
        m_maxThreads = maxThreads;
        m_parent = null;
        m_queuedFutures = new LinkedList<MyFuture<?>>();
        m_availableWorkers = new ConcurrentLinkedQueue<Worker>();
    }

    /**
     * Creates a new sub pool.
     *
     * @param maxThreads the maximum number of threads in the pool
     * @param parent the parent pool
     */
    protected ThreadPool(final int maxThreads, final ThreadPool parent) {
        m_parent = parent;
        m_maxThreads = maxThreads;
        m_queuedFutures = m_parent.m_queuedFutures;
        m_availableWorkers = null;
    }

    private boolean checkQueue() {
        synchronized (m_queuedFutures) {
            for (Iterator<MyFuture<?>> it = m_queuedFutures.iterator(); it
                    .hasNext();) {
                MyFuture<?> f = it.next();
                if (f.isCancelled()) {
                    it.remove();
                } else {
                    ThreadPool pool = f.getPool();
                    if (pool.wakeupWorker(f, pool) != null) {
                        it.remove();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Creates a sub pool that shares the threads with this (parent) pool. The
     * maximum number of threads in this and all its sub pools does not exceed
     * the maximum thread number for this pool, even if a sub pool is created
     * with a higher thread count.
     *
     * @param maxThreads the maximum number of threads in the sub pool
     * @return a thread pool
     */
    public ThreadPool createSubPool(final int maxThreads) {
        return new ThreadPool(maxThreads, this);
    }

    /**
     * Submits a value-returning task for execution and returns a Future
     * representing the pending results of the task. The method immediately
     * returns and puts the runnable into a queue.
     *
     * <p>
     * If you would like to immediately block waiting for a task, you can use
     * constructions of the form <tt>result = exec.submit(aCallable).get();</tt>
     *
     * <p>
     * Note: The {@link java.util.concurrent.Executors} class includes a set of
     * methods that can convert some other common closure-like objects, for
     * example, {@link java.security.PrivilegedAction} to {@link Callable} form
     * so they can be submitted.
     *
     * @param t the task to submit
     * @param <T> any result type
     * @return a Future representing pending completion of the task
     * @throws NullPointerException if <code>task</code> null
     *
     * @see #submit(Callable)
     */
    public <T> Future<T> enqueue(final Callable<T> t) {
        MyFuture<T> ftask = new MyFuture<T>(t);

        synchronized (m_queuedFutures) {
            if (wakeupWorker(ftask, this) == null) {
                m_queuedFutures.add(ftask);
            }
        }

        return ftask;
    }

    /**
     * Submits a Runnable task for execution and returns a Future representing
     * that task. The method immediately returns and puts the runnable into a
     * queue.
     *
     * @param r the task to submit
     * @return a Future representing pending completion of the task, and whose
     *         <tt>get()</tt> method will return <tt>null</tt> upon
     *         completion.
     * @throws NullPointerException if <code>task</code> null
     * @see #submit(Runnable)
     */
    public Future<?> enqueue(final Runnable r) {
        MyFuture<?> ftask = new MyFuture<Object>(r, null);

        synchronized (m_queuedFutures) {
            if (wakeupWorker(ftask, this) == null) {
                m_queuedFutures.add(ftask);
            }
        }

        return ftask;
    }

    private Worker wakeupWorker(final Runnable task, final ThreadPool pool) {
        synchronized (m_runningWorkers) {
            if (m_runningWorkers.size() - m_invisibleThreads < m_maxThreads) {
                Worker w;
                if (m_parent == null) {
                    w = m_availableWorkers.poll();
                    while ((w == null) || !w.wakeup(task, pool)) {
                        w = new Worker();
                        w.start();
                    }
                } else {
                    w = m_parent.wakeupWorker(task, pool);
                }

                if (w != null) {
                    m_runningWorkers.add(w);
                }
                return w;
            } else {
                return null;
            }
        }
    }

    /**
     * Returns the maximum number of threads in the pool.
     *
     * @return the maximum thread number
     */
    public int getMaxThreads() {
        return m_maxThreads;
    }

    /**
     * Returns the number of currently running threads in this pool and its sub
     * pools.
     *
     * @return the number of running threads
     */
    public int getRunningThreads() {
        return m_runningWorkers.size() - m_invisibleThreads;
    }

    /**
     * Executes the runnable in the current thread. If the current thread is
     * taken out of this pool or any ancestor pool the number of invisible
     * threads is increased, so that it is not counted and one additional thread
     * is allowed to run. This method should only be used if the Runnable does
     * nothing more than submitting jobs.
     *
     * @param r a Runnable to execute
     * @throws IllegalThreadStateException if the current thread is not taken
     *             out of a thread pool
     */
    public void runInvisible(final Runnable r) {
        if (!(Thread.currentThread() instanceof Worker)) {
            throw new IllegalThreadStateException("The current thread is not"
                    + "taken out of a thread pool");
        }

        Worker thisWorker = (Worker)Thread.currentThread();
        if (!m_runningWorkers.contains(thisWorker)) {
            if (
              !thisWorker.m_startedFrom.m_runningWorkers.contains(thisWorker)) {
                throw new IllegalThreadStateException("The current thread is "
                        + "not taken out of this thread pool");
            }
            thisWorker.m_startedFrom.runInvisible(r);
        } else {
            if (!m_runningWorkers.contains(thisWorker)) {
                throw new IllegalThreadStateException("The current thread is "
                        + "not taken out of this thread pool");
            }
            m_invisibleThreads++;
            checkQueue();

            try {
                r.run();
            } finally {
                m_invisibleThreads--;
            }
        }

    }

    /**
     * Sets the maximum number of threads in the pool. If the new value is
     * smaller than the old value running surplus threads will not be
     * interrupted.
     *
     * @param newValue the new maximum thread number
     */
    public void setMaxThreads(final int newValue) {
        if (m_parent == null) {
            if (newValue < m_maxThreads) {
                for (int i = (m_maxThreads - newValue); i >= 0; i--) {
                    Worker w = m_availableWorkers.poll();
                    if (w != null) {
                        w.interrupt();
                    }
                }
            }
        }
        m_maxThreads = newValue;
    }

    /**
     * Shuts the pool down, still running threads are not interrupted.
     */
    public void shutdown() {
        setMaxThreads(0);
    }

    /**
     * Submits a value-returning task for execution and returns a Future
     * representing the pending results of the task. The method blocks until a
     * thread is available.
     *
     * <p>
     * If you would like to immediately block waiting for a task, you can use
     * constructions of the form <tt>result = exec.submit(aCallable).get();</tt>
     *
     * <p>
     * Note: The {@link java.util.concurrent.Executors} class includes a set of
     * methods that can convert some other common closure-like objects, for
     * example, {@link java.security.PrivilegedAction} to {@link Callable} form
     * so they can be submitted.
     *
     * @param task the task to submit
     * @param <T> any result type
     * @return a Future representing pending completion of the task
     * @throws NullPointerException if <code>task</code> null
     * @throws InterruptedException if the thread is interrupted
     *
     * @see #enqueue(Callable)
     */
    public <T> Future<T> submit(final Callable<T> task)
            throws InterruptedException {
        if (task == null) {
            throw new NullPointerException();
        }

        final Object lock = new Object();
        final Callable<T> t = new Callable<T>() {
            public T call() throws Exception {
                synchronized (lock) {
                    lock.notifyAll();
                }
                return task.call();
            }
        };

        synchronized (lock) {
            Future<T> ftask = enqueue(t);
            lock.wait();
            return ftask;
        }
    }

    /**
     * Submits a Runnable task for execution and returns a Future representing
     * that task. The method blocks until a free thread is available.
     *
     * @param task the task to submit
     * @return a Future representing pending completion of the task, and whose
     *         <tt>get()</tt> method will return <tt>null</tt> upon
     *         completion.
     * @throws NullPointerException if <code>task</code> null
     * @throws InterruptedException if the thread is interrupted
     * @see #enqueue(Runnable)
     */
    public Future<?> submit(final Runnable task) throws InterruptedException {
        if (task == null) {
            throw new NullPointerException();
        }

        final Object lock = new Object();
        final Runnable r = new Runnable() {
            public void run() {
                synchronized (lock) {
                    lock.notifyAll();
                }
                task.run();
            }
        };

        synchronized (lock) {
            Future<?> ftask = enqueue(r);
            lock.wait();
            return ftask;
        }
    }

    /**
     * Waits until all jobs in this pool and its sub pools have been finished.
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public void waitForTermination() throws InterruptedException {
        synchronized (m_runningWorkers) {
            while (!m_runningWorkers.isEmpty()) {
                m_runningWorkers.wait();
            }
        }
    }

    /**
     * This method is called every time a worker has finished.
     *
     * @param w the finished worker
     */
    protected void workerFinished(final Worker w) {
        if (m_parent != null) {
            synchronized (m_runningWorkers) {
                m_runningWorkers.remove(w);
            }

            m_parent.workerFinished(w);
        } else { // this is the root pool
            synchronized (m_runningWorkers) {
                m_runningWorkers.remove(w);
                m_availableWorkers.add(w);
            }
            if (checkQueue()) {
                return;
            }
        }

        synchronized (m_runningWorkers) {
            m_runningWorkers.notifyAll();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void finalize() throws Throwable {
        if (m_availableWorkers != null) {
            while (true) {
                Worker w = m_availableWorkers.poll();
                if (w == null) {
                    break;
                }
                w.interrupt();
            }
        }
        super.finalize();
    }

    /**
     * Returns the size of the future queue.
     *
     * @return the queue size
     */
    int getQueueSize() {
        return m_queuedFutures.size();
    }

    /**
     * If the current thread is taken out of a thread pool, this method will
     * return the thread pool. Otherwise it will return <code>null</code>.
     *
     * @return a thread pool or <code>null</code>
     */
    public static ThreadPool currentPool() {
        if (Thread.currentThread() instanceof Worker) {
            return ((Worker)Thread.currentThread()).m_startedFrom;
        } else {
            return null;
        }
    }
}
