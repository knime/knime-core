/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
