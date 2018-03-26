/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   11.01.2018 (David Kolb): created
 */
package org.knime.core.node.util.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import javax.swing.AbstractListModel;

/**
 * {@link ArrayList} implementation of an {@link AbstractListModel}.
 *
 * @param <E> the type of the elements of this model
 *
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 * @since 3.6
 */
public class ArrayListModel<E> extends AbstractListModel<E> implements Iterable<E> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private ArrayList<E> m_delegate = new ArrayList<>();

    /**
     * Trims the capacity of this list to be the list's current size.
     *
     * @see ArrayList#trimToSize()
     */
    public void trimToSize() {
        m_delegate.trimToSize();
    }

    /**
     * Increases the capacity of this list, if necessary, to ensure that it can hold at least the number of components
     * specified by the minimum capacity argument.
     *
     * @param minCapacity the desired minimum capacity
     * @see ArrayList#ensureCapacity(int)
     */
    public void ensureCapacity(final int minCapacity) {
        m_delegate.ensureCapacity(minCapacity);
    }

    /**
     * Returns the number of components in this list.
     *
     * @return the number of components in this list
     * @see ArrayList#size()
     */
    public int size() {
        return m_delegate.size();
    }

    /**
     * Tests whether this list has any components.
     *
     * @return <code>true</code> if and only if this list has no components, that is, its size is zero;
     *         <code>false</code> otherwise
     * @see ArrayList#isEmpty()
     */
    public boolean isEmpty() {
        return m_delegate.isEmpty();
    }

    /**
     * Tests whether the specified object is a component in this list.
     *
     * @param elem an object
     * @return <code>true</code> if the specified object is the same as a component in this list
     * @see ArrayList#contains(Object)
     */
    public boolean contains(final Object elem) {
        return m_delegate.contains(elem);
    }

    /**
     * Searches for the first occurrence of <code>elem</code>.
     *
     * @param elem an object
     * @return the index of the first occurrence of the argument in this list; returns <code>-1</code> if the object is
     *         not found
     * @see ArrayList#indexOf(Object)
     */
    public int indexOf(final Object elem) {
        return m_delegate.indexOf(elem);
    }

    /**
     * Returns the index of the last occurrence of <code>elem</code>.
     *
     * @param elem the desired component
     * @return the index of the last occurrence of <code>elem</code> in the list; returns <code>-1</code> if the object
     *         is not found
     * @see ArrayList#lastIndexOf(Object)
     */
    public int lastIndexOf(final Object elem) {
        return m_delegate.lastIndexOf(elem);
    }

    /**
     * Adds the specified component to the end of this list.
     *
     * @param element the component to be added
     * @see ArrayList#add(Object)
     */
    public void add(final E element) {
        int index = m_delegate.size();
        m_delegate.add(element);
        fireIntervalAdded(this, index, index);
    }

    /**
     * Inserts the specified element at the specified position in this list.
     *
     * @param index index at which the specified element is to be inserted
     * @param element element to be inserted
     */
    public void add(final int index, final E element) {
        m_delegate.add(index, element);
        fireIntervalAdded(this, index, index);
    }

    /**
     * Removes the first (lowest-indexed) occurrence of the argument from this list.
     *
     * @param obj the component to be removed
     * @return <code>true</code> if the argument was a component of this list; <code>false</code> otherwise
     * @see ArrayList#remove(Object)
     */
    public boolean remove(final Object obj) {
        int index = indexOf(obj);
        boolean rv = m_delegate.remove(obj);
        if (index >= 0) {
            fireIntervalRemoved(this, index, index);
        }
        return rv;
    }

    /**
     * Removes the element at the specified position in this list. Returns the element that was removed from the list.
     *
     * @param index the index of the element to removed
     * @return the element previously at the specified position
     */
    public E remove(final int index) {
        E rv = m_delegate.remove(index);
        fireIntervalRemoved(this, index, index);
        return rv;
    }

    /**
     * Returns a string that displays and identifies this object's properties.
     *
     * @return a String representation of this object
     */
    @Override
    public String toString() {
        return m_delegate.toString();
    }

    /**
     * Returns an array containing all of the elements in this list in the correct order.
     *
     * @return an array containing the elements of the list
     * @see ArrayList#toArray()
     */
    public Object[] toArray() {
        return m_delegate.toArray();
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index index of element to return
     * @return the element at the specified index
     */
    public E get(final int index) {
        return m_delegate.get(index);
    }

    /**
     * Removes all of the elements from this list.
     */
    public void clear() {
        int index1 = m_delegate.size() - 1;
        m_delegate.clear();
        if (index1 >= 0) {
            fireIntervalRemoved(this, 0, index1);
        }
    }

    /**
     * Deletes the components at the specified range of indexes. The removal is inclusive, so specifying a range of
     * (1,5) removes the component at index 1 and the component at index 5, as well as all components in between.
     * <p>
     * Throws an <code>ArrayIndexOutOfBoundsException</code> if the index was invalid. Throws an
     * <code>IllegalArgumentException</code> if <code>fromIndex &gt; toIndex</code>.
     *
     * @param fromIndex the index of the lower end of the range
     * @param toIndex the index of the upper end of the range
     * @see #remove(int)
     */
    public void removeRange(final int fromIndex, final int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("fromIndex must be <= toIndex");
        }
        for (int i = toIndex; i >= fromIndex; i--) {
            m_delegate.remove(i);
        }
        fireIntervalRemoved(this, fromIndex, toIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSize() {
        return size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public E getElementAt(final int index) {
        return get(index);
    }

    /**
     * @param c collection containing elements to be added to this list
     * @see ArrayList#addAll(Collection)
     */
    public void addAll(final Collection<E> c) {
        if(c.isEmpty()){
            return;
        }
        int start = m_delegate.size();
        m_delegate.addAll(c);
        fireIntervalAdded(this, start, m_delegate.size() - 1);
    }

    /**
     * @param index index at which to insert the first element from the specified collection
     * @param c collection containing elements to be added to this list
     * @see ArrayList#addAll(int, Collection)
     */
    public void addAll(final int index, final Collection<E> c) {
        if(c.isEmpty()){
            return;
        }
        m_delegate.addAll(index, c);
        fireIntervalAdded(this, index, c.size() - 1);
    }

    /**
     * @param c collection containing elements to be added to this list
     * @return true if this list changed as a result of the call
     * @see ArrayList#removeAll(Collection)
     */
    public boolean removeAll(final Collection<E> c) {
        if(c.isEmpty()){
            return false;
        }
        int index = m_delegate.size() - 1;
        boolean changed = m_delegate.removeAll(c);
        fireIntervalRemoved(this, 0, index);
        return changed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<E> iterator() {
        return m_delegate.iterator();
    }

    /**
     * Get all elements in this model as an {@link Collections#unmodifiableList(java.util.List) unmodifiableList}.
     *
     * @return All elements in this list model.
     */
    public Collection<?> getAllElements() {
        return Collections.unmodifiableList(m_delegate);
    }
}
