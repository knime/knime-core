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
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Implements a sophisticated thread pool.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class ThreadPool {

    /** Feature flag / system property to disable "slot reacquisition" after {@link #runInvisible(Callable)}.
     * Added in 5.5.2 and 5.8.0 as part of AP-25062.
     * @deprecated to be removed again once problem is understood/solved.
     */
    @Deprecated(since = "5.8.0", forRemoval = true)
    private static final String SYSPROP_DISABLE_INVISIBLE_SYNC = "org.knime.threadpool.disableInvisibleSync"; // NOSONAR

    private static final boolean DISABLE_INVISIBLE_SYNC;

    static {
        DISABLE_INVISIBLE_SYNC = Boolean.getBoolean(SYSPROP_DISABLE_INVISIBLE_SYNC); // NOSONAR
        if (DISABLE_INVISIBLE_SYNC) {
            NodeLogger.getLogger(ThreadPool.class).infoWithFormat(
                "ThreadPool invisible synchronization is disabled via system property: %s",
                SYSPROP_DISABLE_INVISIBLE_SYNC); // NOSONAR
        }
    }

    private class MyFuture<T> extends FutureTask<T> {
        private final CountDownLatch m_startWaiter = new CountDownLatch(1);
        private final ClassLoader m_contextClassloader = Thread.currentThread().getContextClassLoader();

        /**
         * @see FutureTask#FutureTask(Callable)
         */
        public MyFuture(final Callable<T> callable) {
            super(ThreadUtils.callableWithContext(callable, false));
        }

        /**
         * Creates a <tt>FutureTask</tt> that will upon running, execute the
         * given <tt>Runnable</tt>, and arrange that <tt>get</tt> will return
         * the given result on successful completion.
         *
         * @param runnable the runnable task
         * @param result the result to return on successful completion. If you
         *            don't need a particular result, consider using
         *            constructions of the form: <tt>Future&lt;?&gt; f =
         *            new FutureTask&lt;Object&gt;(runnable, null)</tt>
         * @throws NullPointerException if runnable is null
         */
        public MyFuture(final Runnable runnable, final T result) {
            super(ThreadUtils.runnableWithContext(runnable, false), result);
        }

        /**
         * Returns the pool in which this future has been created.
         *
         * @return a thread pool
         */
        public ThreadPool getPool() {
            return ThreadPool.this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            m_startWaiter.countDown();
            // set context classloader of thread that created this task
            ClassLoader previousContextClassloader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(m_contextClassloader);
            try {
                super.run();
            } finally {
                Thread.currentThread().setContextClassLoader(previousContextClassloader);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean cancel(final boolean mayInterruptIfRunning) {
            boolean b = super.cancel(mayInterruptIfRunning);
            if (b) {
                m_startWaiter.countDown();
            }
            return b;
        }

        /**
         * Waits until this future has started its execution. Returns
         * immediately if the future is already running or even finished.
         *
         * @throws InterruptedException if the current thread is interrupted
         *             while waiting
         */
        public void waitUntilStarted() throws InterruptedException {
            m_startWaiter.await();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public T get() throws InterruptedException, ExecutionException {
            if (Thread.currentThread() instanceof Worker w) {
                w.m_startedFrom.m_invisibleThreads.incrementAndGet();
                try {
                    checkQueue();
                    return super.get();
                } finally {
                    w.m_startedFrom.m_invisibleThreads.decrementAndGet();
                }
            } else {
                return super.get();
            }
        }

        void checkException() throws InterruptedException, ExecutionException {
            super.get();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public T get(final long timeout, final TimeUnit unit)
                throws InterruptedException, ExecutionException,
                TimeoutException {
            if (Thread.currentThread() instanceof Worker w) {
                w.m_startedFrom.m_invisibleThreads.incrementAndGet();
                try {
                    checkQueue();
                    return super.get(timeout, unit);
                } finally {
                    w.m_startedFrom.m_invisibleThreads.decrementAndGet();
                }
            } else {
                return super.get(timeout, unit);
            }
        }
    }

    private static int workerCounter;

    private static final class Worker extends Thread {
        private final Object m_lock = new Object();

        private MyFuture<?> m_runnable;

        private ThreadPool m_startedFrom;

        private boolean m_stopped;

        /** Set (and unset) by {@link ThreadPool#runInvisible(Callable)}, remembers if this worker is currently
         * executing tasks invisibly. */
        private boolean m_isCurrentlyInvisible;

        // set context class loader after each runnable#run -- we had problems with some cxf web service client that
        // hijacked the current thread and subsequent runnables were using some URL class loader set by cxf
        private final ClassLoader m_contextClassLoaderAtInit;

        /**
         * Creates a new worker (internal code, use {@link #startNewWorker()} instead).
         */
        private Worker() {
            super("KNIME-Worker-" + workerCounter++);
            setPriority(Thread.MIN_PRIORITY + 2);
            setDaemon(true);
            m_contextClassLoaderAtInit = getContextClassLoader();
        }

        /** @return new worker with an exception handler. */
        static Worker startNewWorker() {
            final Worker w = new Worker();
            w.setUncaughtExceptionHandler((t, e) -> NodeLogger.getLogger(ThreadPool.class)
                .error("An uncaught exception occurred in a worker thread.", e));
            w.start();
            return w;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            while (!isInterrupted()) {
                ThreadPool startedFrom;
                synchronized (m_lock) {
                    if (m_runnable == null) {
                        try {
                            m_lock.wait(90 * 1000);
                            if (m_runnable == null) {
                                // then the timeout has occurred
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
                        m_runnable.checkException();
                    } catch (InterruptedException ex) {
                        NodeLogger.getLogger(ThreadPool.class).debug("Thread was interrupted");
                    } catch (CancellationException ex) {
                        NodeLogger.getLogger(ThreadPool.class).debug("Future was canceled");
                    } catch (ExecutionException ex) {
                        if (!(ex.getCause() instanceof CanceledExecutionException)) {
                            // canceled execution exception is fine and will not be reported
                            NodeLogger.getLogger(ThreadPool.class).error(
                                "An exception occurred while executing a runnable.", ex.getCause());
                        }
                    } catch (Exception ex) { // dead code as the #run method is from a FutureTask, catching Throwable
                        // prevent the worker from being terminated
                        NodeLogger.getLogger(ThreadPool.class).error("An exception occurred while executing "
                                + "a runnable.", ex);
                    } finally {
                        setContextClassLoader(m_contextClassLoaderAtInit);
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
         *         <code>false</code> if not because the thread has already died
         */
        public boolean wakeup(final MyFuture<?> r, final ThreadPool pool) {
            synchronized (m_lock) {
                if (m_stopped || !isAlive()) {
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

    private final AtomicInteger m_maxThreads = new AtomicInteger();

    private final AtomicInteger m_invisibleThreads = new AtomicInteger();

    private final AtomicInteger m_pendingJobs = new AtomicInteger();

    private final ThreadPool m_parent;

    private final Queue<MyFuture<?>> m_queuedFutures;

    private final Set<Worker> m_runningWorkers = new HashSet<Worker>();

    /**
     * Creates a new ThreadPool with a maximum number of threads.
     *
     * @param maxThreads the maximum number of threads
     */
    public ThreadPool(final int maxThreads) {
        if (maxThreads < 1) {
            throw new IllegalArgumentException("Thread count must be > 0");
        }
        m_maxThreads.set(maxThreads);
        m_parent = null;
        m_queuedFutures = new LinkedList<MyFuture<?>>();
        m_availableWorkers = new ConcurrentLinkedQueue<>();
        ThreadUtils.cleaner().register(this, new FinalizeRunnable(m_availableWorkers)); // NOSONAR (no fully init'ed)
    }

    /**
     * Creates a new sub pool.
     *
     * @param maxThreads the maximum number of threads in the pool
     * @param parent the parent pool
     */
    protected ThreadPool(final int maxThreads, final ThreadPool parent) {
        if (maxThreads < 1) {
            throw new IllegalArgumentException("Thread count must be > 0");
        }
        m_parent = parent;
        m_maxThreads.set(maxThreads);
        m_queuedFutures = m_parent.m_queuedFutures;
        m_availableWorkers = null;
    }

    private boolean checkQueue() {
        synchronized (m_runningWorkers) {
            // some workers might be waiting to become visible again
            m_runningWorkers.notifyAll();
        }

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
     * Creates a sub pool that shares the threads with this (parent) pool.
     *
     * @return a thread pool
     */
    public ThreadPool createSubPool() {
        return new ThreadPool(m_maxThreads.get(), this);
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
     * @param task the task to submit
     * @param <T> any result type
     * @return a Future representing pending completion of the task
     * @throws NullPointerException if <code>task</code> null
     *
     * @see #submit(Callable)
     */
    public <T> Future<T> enqueue(final Callable<T> task) {
        if (task == null) {
            throw new IllegalArgumentException("Task must not be null");
        }

        MyFuture<T> ftask = new MyFuture<T>(task);

        synchronized (m_queuedFutures) {
            incrementPendingJobs();
            if (wakeupWorker(ftask, this) == null) {
                m_queuedFutures.add(ftask);
            }
        }

        return ftask;
    }

    private void incrementPendingJobs() {
        m_pendingJobs.incrementAndGet();
        if (m_parent != null) {
            m_parent.incrementPendingJobs();
        }
    }

    private void decrementPendingJobs() {
        if (m_parent != null) {
            m_parent.decrementPendingJobs();
        }
        if (m_pendingJobs.decrementAndGet() == 0) {
            synchronized (m_pendingJobs) {
                m_pendingJobs.notifyAll();
            }
        }
    }

    /**
     * Submits a Runnable task for execution and returns a Future representing
     * that task. The method immediately returns and puts the runnable into a
     * queue.
     *
     * @param r the task to submit
     * @return a Future representing pending completion of the task, and whose
     *         <tt>get()</tt> method will return <tt>null</tt> upon completion.
     * @throws NullPointerException if <code>task</code> null
     * @see #submit(Runnable)
     */
    public Future<?> enqueue(final Runnable r) {
        MyFuture<?> ftask = new MyFuture<Object>(r, null);

        synchronized (m_queuedFutures) {
            incrementPendingJobs();
            if (wakeupWorker(ftask, this) == null) {
                m_queuedFutures.add(ftask);
            }
        }

        return ftask;
    }

    /**
     * Tries to submits a value-returning task for immediate execution and
     * returns a Future representing the pending results of the task if a thread
     * is free. If no thread is currently available <code>null</code> is
     * returned and the task is not queued.
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
     * @return a Future representing pending completion of the task or
     *         <code>null</code> if no thread was available
     * @throws NullPointerException if <code>task</code> null
     *
     * @see #submit(Callable)
     */
    public <T> Future<T> trySubmit(final Callable<T> t) {
        MyFuture<T> ftask = new MyFuture<T>(t);

        synchronized (m_queuedFutures) {
            incrementPendingJobs();
            if (wakeupWorker(ftask, this) == null) {
                decrementPendingJobs();
                return null;
            }
        }

        return ftask;
    }

    /**
     * Tries to submits a Runnable task for immediate execution and returns a
     * Future representing the task if a thread is free. If no thread is
     * currently available <code>null</code> is returned and the task is not
     * queued.
     *
     * @param r the task to submit
     * @return a Future representing pending completion of the task, and whose
     *         <tt>get()</tt> method will return <tt>null</tt> upon completion.
     * @throws NullPointerException if <code>task</code> null
     * @see #submit(Runnable)
     */
    public Future<?> trySubmit(final Runnable r) {
        MyFuture<?> ftask = new MyFuture<Object>(r, null);

        synchronized (m_queuedFutures) {
            incrementPendingJobs();
            if (wakeupWorker(ftask, this) == null) {
                decrementPendingJobs();
                return null;
            }
        }

        return ftask;
    }

    private Worker wakeupWorker(final MyFuture<?> task, final ThreadPool pool) {
        synchronized (m_runningWorkers) {
            if (hasFreeWorkerSlot()) {
                Worker w;
                if (m_parent == null) {
                    w = m_availableWorkers.poll();
                    while ((w == null) || !w.wakeup(task, pool)) {
                        w = Worker.startNewWorker();
                    }
                } else {
                    w = m_parent.wakeupWorker(task, pool);
                }

                if (w != null) {
                    m_runningWorkers.add(w);
                }
                return w;
            }
            return null;
        }
    }

    /**
     * Returns the maximum number of threads in the pool.
     *
     * @return the maximum thread number
     */
    public int getMaxThreads() {
        return m_maxThreads.get();
    }

    /**
     * Returns an estimate for the number of currently running threads in this pool and its sub
     * pools.
     *
     * @return the number of running threads
     */
    public int getRunningThreads() {
        // no synchronization b/c it's only an estimate
        // we exclude invisible threads because we assume they're not doing compute-heavy tasks
        return m_runningWorkers.size() - m_invisibleThreads.get();
    }

    /**
     * @return the invisibleThreads
     * @since 5.8
     */
    int getInvisibleThreads() {
        return m_invisibleThreads.get();
    }

    private boolean hasFreeWorkerSlot() {
        synchronized (m_runningWorkers) {
            return m_runningWorkers.size() - m_invisibleThreads.get() < m_maxThreads.get();
        }
    }

    /**
     * Returns an estimate for the number of queued jobs.
     *
     * @return the number of queued jobs
     * @since 5.5
     */
    public int getQueueSize() {
        return m_queuedFutures.size();
    }

    /**
     * Executes the runnable in the current thread. If the current thread is
     * taken out of this pool or any ancestor pool the number of invisible
     * threads is increased, so that it is not counted and one additional thread
     * is allowed to run.
     * After the runnable is finished, the thread is being accounted for again.
     *
     * <p><b>Note:</b> This method is intended for <em>blocking operations</em> or threads
     * that submit other tasks.</p>
     *
     * @param <T> Type of the argument (result type)
     * @param r A callable, which will be executed by the thread invoking this
     *            method.
     *
     * @return T The result of the callable.
     * @throws IllegalThreadStateException if the current thread is not taken
     *             out of a thread pool
     * @throws ExecutionException if the callable could not be executed for some
     *             reason
     * @throws InterruptedException If either the current thread is interrupted, either when interrupted
     *             while <code>r.call()</code> is executed or when reacquiring a "visible" thread slot.
     */
    public <T> T runInvisible(final Callable<T> r) throws ExecutionException, InterruptedException {
        if (!(Thread.currentThread() instanceof Worker thisWorker)) {
            throw new IllegalThreadStateException("The current thread is not "
                    + "taken out of a thread pool");
        }

        boolean b;
        synchronized (m_runningWorkers) {
            b = m_runningWorkers.contains(thisWorker);
        }

        if (!b) {
            synchronized (thisWorker.m_startedFrom.m_runningWorkers) {
                b =
                        thisWorker.m_startedFrom.m_runningWorkers
                                .contains(thisWorker);
            }

            if (!b) {
                throw new IllegalThreadStateException("The current thread is "
                        + "not taken out of this thread pool");
            }
            return thisWorker.m_startedFrom.runInvisible(r);
        } else if (thisWorker.m_isCurrentlyInvisible) {
            try {
                return r.call();
            } catch (final InterruptedException e) {
                throw e;
            } catch (final Exception ex) {
                throw new ExecutionException(ex);
            }
        } else {
            m_invisibleThreads.incrementAndGet();
            checkQueue();
            var interruptInCallable = false;
            thisWorker.m_isCurrentlyInvisible = true;
            try {
                return r.call();
            } catch (final InterruptedException e) {
                interruptInCallable = true;
                throw e;
            } catch (final Exception ex) {
                throw new ExecutionException(ex);
            } finally {
                reacquireSlot(interruptInCallable);
                thisWorker.m_isCurrentlyInvisible = false;
            }
        }
    }

    private void reacquireSlot(final boolean interruptInCallable) throws InterruptedException {
        synchronized (m_runningWorkers) {
            try {

                // discussed as part of AP-24224: we are more picky to not oversubscribe the thread pool after
                // leaving 'runInvisible' but some nodes secretly call it during configuration (which is when the
                // workflow is locked) -- special case here but log
                final boolean isLockedWorkflow = NodeContext.getContextOptional() //
                    .flatMap(ctx -> ctx.getContextObjectForClass(WorkflowManager.class)) //
                    .map(WorkflowManager::isLockedByCurrentThread).orElse(Boolean.FALSE);
                NodeLogger.getLogger(ThreadPool.class).assertLog(!isLockedWorkflow,
                    "ThreadPool.runInvisible() called while workflow is locked");

                // no need to re-acquire a slot if we want to pass on the callable interrupt
                if (!interruptInCallable && !isLockedWorkflow && !DISABLE_INVISIBLE_SYNC) {
                    // reacquire a "slot" among visible workers
                    // Getting interrupted here can trigger a special case (see below in finally block)
                    while (!hasFreeWorkerSlot()) {
                        m_runningWorkers.wait();
                    }
                }
            } finally {
                /* One interesting special case:
                 * When we were interrupted while waiting for a slot again in the loop above, we decrement the
                 * "invisible" counter as if we would have acquired a slot. In any case, control is passed back
                 * to the calling method. This means that the caller can opt to swallow the interrupted
                 * exception in order to keep the thread running and thus keeping the pool oversubscribed.
                 * Once the current task is finished, the thread will be put back into the available workers
                 * but only actually used if the pool is not saturated anymore. This is exactly the behavior
                 * as before the "re-acquire slot after call" change in AP-24224.
                 */
                m_invisibleThreads.decrementAndGet();
            }
        }
    }

    /**
     * Sets the maximum number of threads in the pool. If the new value is
     * smaller than the old value running surplus threads will not be
     * interrupted. If the new value is bigger than the old one, waiting jobs
     * will be started immediately.
     *
     * @param newValue the new maximum thread number
     */
    public void setMaxThreads(final int newValue) {
        if (newValue < 0) {
            throw new IllegalArgumentException("Thread count must be >= 0");
        }
        if ((m_parent == null) && (newValue < m_maxThreads.get())) {
            for (int i = (m_maxThreads.get() - newValue); i >= 0; i--) {
                Worker w = m_availableWorkers.poll();
                if (w != null) {
                    w.interrupt();
                }
            }
        }
        m_maxThreads.set(newValue);
        checkQueue();
    }

    /**
     * Shuts the pool down, still running threads are not interrupted.
     */
    public void shutdown() {
        synchronized (m_queuedFutures) {
            Iterator<MyFuture<?>> it = m_queuedFutures.iterator();
            while (it.hasNext()) {
                MyFuture<?> future = it.next();
                if (future.getPool() == this) {
                    decrementPendingJobs();
                    future.cancel(true);
                    it.remove();
                }
            }
        }
        setMaxThreads(0);
    }


    /**
     * Interrupts all running jobs.
     */
    public void interruptAll() {
        for (Worker w : m_runningWorkers) {
            w.interrupt();
        }
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
     * @throws InterruptedException if the thread is interrupted
     *
     * @see #enqueue(Callable)
     */
    public <T> Future<T> submit(final Callable<T> task)
            throws InterruptedException {
        MyFuture<T> ftask = (MyFuture<T>)enqueue(task);
        ftask.waitUntilStarted();
        return ftask;
    }

    /**
     * Submits a Runnable task for execution and returns a Future representing
     * that task. The method blocks until a free thread is available.
     *
     * @param task the task to submit
     * @return a Future representing pending completion of the task, and whose
     *         <tt>get()</tt> method will return <tt>null</tt> upon completion.
     * @throws InterruptedException if the thread is interrupted
     * @see #enqueue(Runnable)
     */
    public Future<?> submit(final Runnable task) throws InterruptedException {
        MyFuture<?> ftask = (MyFuture<?>)enqueue(task);
        ftask.waitUntilStarted();
        return ftask;
    }

    /**
     * Waits until all jobs in this pool and its sub pools have been finished.
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public void waitForTermination() throws InterruptedException {
        synchronized (m_pendingJobs) {
            ThreadPool currentPool = currentPool();
            if (currentPool != null) {
                currentPool.m_invisibleThreads.incrementAndGet();
            }
            try {
                checkQueue();
                while (m_pendingJobs.get() != 0) {
                    m_pendingJobs.wait();
                }
            } finally {
                if (currentPool != null) {
                    currentPool.m_invisibleThreads.decrementAndGet();
                }
            }
        }
    }

    /**
     * This method is called every time a worker has finished.
     *
     * @param w the finished worker
     */
    protected void workerFinished(final Worker w) {
        if (m_pendingJobs.decrementAndGet() == 0) {
            synchronized (m_pendingJobs) {
                m_pendingJobs.notifyAll();
            }
        }

        if (m_parent != null) {
            synchronized (m_runningWorkers) {
                m_runningWorkers.remove(w);
                m_runningWorkers.notifyAll();
            }
            m_parent.workerFinished(w);
        } else { // this is the root pool
            synchronized (m_runningWorkers) {
                m_runningWorkers.remove(w);
                m_availableWorkers.add(w);
                m_runningWorkers.notifyAll();
            }
            checkQueue();
        }
    }

    /**
     * Cleans up the pool. This method is called when the pool is collected by the GC (ported from a former
     * {@link #finalize()} method.)
     */
    private static class FinalizeRunnable implements Runnable {

        private final Queue<Worker> m_availableWorkersInFinalize;

        FinalizeRunnable(final Queue<Worker> availableWorkersInFinalize) {
            m_availableWorkersInFinalize = availableWorkersInFinalize;
        }

        @Override
        public void run() {
            while (true) {
                Worker w = m_availableWorkersInFinalize.poll();
                if (w == null) {
                    break;
                }
                w.interrupt();
            }
        }
    }

    /**
     * If the current thread is taken out of a thread pool, this method will
     * return the thread pool. Otherwise it will return <code>null</code>.
     *
     * @return a thread pool or <code>null</code>
     */
    public static ThreadPool currentPool() {
        if (Thread.currentThread() instanceof Worker worker) {
            return worker.m_startedFrom;
        } else {
            return null;
        }
    }
}
