/*
 * ------------------------------------------------------------------ *
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
 */
package org.knime.core.data.util;

import java.util.Iterator;

/**
 * Synchronized (thread-safe) wrapper for an {@link Iterator}. This class
 * does not implement the <code>Iterator</code> interface as it does not
 * follow the usual <code>hasNext()</code>, <code>next()</code> procedure
 * (which is not a atomic sequence). Instead, this class should be used as
 * follows:
 * <pre>
 * final SynchronizedIterator&lt;T&gt; it = 
 *      new SynchronizedIterator&lt;T&gt;(wrappedIterator);
 *      
 * // this is usually executed in different threads
 * T next;
 * while ((next = it.next()) != null) {
 * }
 * </pre>
 * Alternatively, this class provides a method {@link #nextWithIndex()}, 
 * which wraps the iteration element and its index in the iteration loop
 * in a {@link ElementAndIndex}. 
 * 
 * @param <T> The elements contained in the Iterable.
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class SynchronizedIterator<T> {
    
    private final Iterator<T> m_it;
    private int m_currentIndex;

    /** Creates new iterator from a given (unsynchronized) iterator.
     * @param it The iterator instance to wrap.
     * @throws NullPointerException If the argument is null.
     */
    public SynchronizedIterator(final Iterator<T> it) {
        if (it == null) {
            throw new NullPointerException("Iterator is null");
        }
        m_it = it;
        m_currentIndex = -1;
    }
    
    /** Creates new iterator from an Iterable.
     * @param iterable Argument iterable.
     * @throws NullPointerException If the argument is null.
     */
    public SynchronizedIterator(final Iterable<T> iterable) {
        this(iterable.iterator());
    }
    
    /** @return the next element in the iteration or <code>null</code> if
     * no more are available. */ 
    public synchronized T next() {
        if (!m_it.hasNext()) {
            return null;
        }
        T next = m_it.next();
        m_currentIndex += 1;
        return next;
    }
    
    /** Get the next element along with its index in the iteration (starting 
     * with 0 for the first element) or <code>null</code> if no more elements
     * are available.
     * @return the next element with index
     */
    public synchronized ElementAndIndex<T> nextWithIndex() {
        T next = next();
        return next == null 
        ? null : new ElementAndIndex<T>(next, m_currentIndex); 
    }
    
    /** Class combining an iteration element with its index in the iteration
     * loop.
     * @param <T> The element class to wrap */
    public static final class ElementAndIndex<T> {
        private final T m_element;
        private final int m_number;
        
        private ElementAndIndex(final T row, final int number) {
            m_element = row;
            m_number = number;
        }
        
        /** @return the element */
        public T get() {
            return m_element;
        }
        
        /** @return the index of the elemtent in the iteration process */
        public int getIndex() {
            return m_number;
        }
    }
}
