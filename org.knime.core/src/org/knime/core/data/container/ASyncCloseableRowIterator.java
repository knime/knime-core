/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   Feb 16, 2009 (wiswedel): created
 */
package org.knime.core.data.container;

import java.lang.ref.WeakReference;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.knime.core.data.DataRow;
import org.knime.core.node.NodeLogger;

/**
 * An asynchronous iterator wrapping an underlying argument iterator. It uses
 * a blocking queue to read the rows in background.
 * @author Bernd Wiswedel, University of Konstanz
 */
final class ASyncCloseableRowIterator extends CloseableRowIterator {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(ASyncCloseableRowIterator.class);
    
    /** The queue with the next rows to return. It's parameterized with Object 
     * rather than DataRow because the asynchronous task will put a generic 
     * object into the queue in order to signal the iteration and. So this 
     * queue will -- except for the last element -- contain DataRows. */
    private final LinkedBlockingQueue<Object> m_queue;
    
    /** The future representing the background reading task. */
    private final Future<?> m_readInBackgroundTask;
    
    /** The iterator to wrap. */
    private final CloseableRowIterator m_it;
    
    /** A pointer to any throwable that was thrown during read (kept as field
     * because the reading will happen asynchronously. */
    private final AtomicReference<Throwable> m_readThrowable;
    
    /** The very next data row to return. Kept here as fixed reference because 
     * the queue may not have been filled (hard to tell with the iterator is at
     * end if queue is empty. */
    private DataRow m_next;
    
    /** Creates new asynchronous iterator, which wraps the argument iterator.
     * @param it The iterator to wrap.
     * @throws NullPointerException If the argument is null. */
    ASyncCloseableRowIterator(final CloseableRowIterator it) {
        if (it == null) {
            throw new NullPointerException();
        }
        m_it = it;
        m_queue = new LinkedBlockingQueue<Object>(
                DataContainer.ASYNC_CACHE_SIZE);
        m_readThrowable = new AtomicReference<Throwable>();
        boolean hasRows = m_it.hasNext();
        m_readInBackgroundTask = DataContainer.ASYNC_EXECUTORS.submit(
                new ASyncReadRunnable(this));
        // read very first row, if there is any.
        if (hasRows) {
            try {
                m_next = (DataRow)m_queue.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(
                        "Reading from iterator was interrupted", e);
            }
        } else {
            m_next = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        // this will eventually also bring this iterator to the end
        m_it.close();
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasNext() {
        return m_next != null;
    }

    /** {@inheritDoc} */
    @Override
    public DataRow next() {
        if (m_next == null) {
            throw new NoSuchElementException("Iterator at end");
        }
        DataRow result = m_next;
        Object next = internalNext();
        // the very last element in the queue will be a generic object,
        // which indicates that the underlying iterator has no more elements
        if (next instanceof DataRow) {
            m_next = (DataRow)next;
        } else {
            m_next = null;
        }
        return result;
    }
    
    /** Actively retrieves the next object from the queue. It also handles cases
     * where the background reading task has been canceled.
     * @return the next object from the queue
     * @throws RuntimeException if interrupted or other exceptions were thrown.
     */
    private Object internalNext() {
        Throwable t = m_readThrowable.get();
        if (t != null) {
            throw new RuntimeException("Errors reading from iterator", t);
        }
        Object obj;
        try {
            while ((obj = m_queue.poll(5, TimeUnit.SECONDS)) == null) {
                if (m_readInBackgroundTask.isDone()) {
                    t = m_readThrowable.get();
                    StringBuilder error = new StringBuilder(
                            "Reading in background task has unexpectedly died");
                    if (t != null) {
                        error.append(" (caused by \"");
                        error.append(t.getClass().getName()).append("\")");
                    }
                    throw new RuntimeException(error.toString(), t);
                }
            }
            return obj;
        } catch (InterruptedException ie) {
            throw new RuntimeException("Reading table has been interrupted");
        }
    }
    
    private static final class ASyncReadRunnable implements Runnable {
        
        private final WeakReference<ASyncCloseableRowIterator> m_outerRef;
        
        /**
         * Creates new runnable that add the rows from the underlying iterator
         * to the queue. It will also handle cases where the iterator (the outer
         * class object) is garbage collected in order to release the executing
         * thread.
         * @param outer The outer class object. It will be held in a weak 
         * reference 
         */
        ASyncReadRunnable(final ASyncCloseableRowIterator outer) {
            m_outerRef = new WeakReference<ASyncCloseableRowIterator>(outer);
        }
        
        // two fields for reporting caching statistics
        private int m_counter = 0;
        private int m_cacheSize = 0;
        
        /** {@inheritDoc} */
        @Override
        public void run() {
            ASyncCloseableRowIterator outer = m_outerRef.get();
            if (outer == null) {
                return;
            }
            final BlockingQueue<Object> queue = outer.m_queue;
            final CloseableRowIterator it = outer.m_it;
            final AtomicReference<Throwable> readThrowable = 
                outer.m_readThrowable;
            outer = null; // allow garbage collection
            while (it.hasNext()) {
                try {
                    if (!offer(it.next(), queue)) {
                        return;
                    }
                    m_counter++;
                    m_cacheSize += queue.size();
                } catch (InterruptedException e) {
                    LOGGER.info("Reading from iterator was interrupted", e);
                    break;
                } catch (Throwable e) {
                    readThrowable.set(e);
                    break;
                }
            }
            LOGGER.debug("Average size of asynchronous read cache: " 
                    + m_cacheSize / (double)m_counter + " (for " 
                    + m_counter + " rows)");
            // indicate the iteration end. 
            try {
                if (!offer(new Object(), queue)) {
                    return;
                }
            } catch (InterruptedException e) {
                LOGGER.info("Reading from iterator was interrupted", e);
            }
        }
        
        /** Puts the argument object into the queue, waiting for space to 
         * become available. It also checks regularly whether the outer 
         * reference has been cleared (meaning the iterator was gc'ed).
         * @param o the object to add
         * @param queue the queue to add to
         * @return if that was successful. If false is returned, the iterator
         * was garbage collected
         * @throws InterruptedException if the thread was interrupted.
         */
        private boolean offer(final Object o, 
                final BlockingQueue<Object> queue) throws InterruptedException {
            while (true) {
                if (queue.offer(o, 1, TimeUnit.MINUTES)) {
                    return true;
                } else {
                    if (m_outerRef.get() == null) {
                        return false;
                    }
                }
            }
        }
        
    }
    
}
