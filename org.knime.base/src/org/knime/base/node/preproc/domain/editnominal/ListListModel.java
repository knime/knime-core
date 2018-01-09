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
 * ---------------------------------------------------------------------
 *
 * Created on 15.10.2013 by NanoHome
 */
package org.knime.base.node.preproc.domain.editnominal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.swing.AbstractListModel;
import javax.swing.ListModel;

/**
 * A {@link ListModel} and {@link List} implementation backed by an {@link ArrayList}. This enables foreach loops, java
 * sorting capabilities as {@link Collections#sort(List)} and all other power of the java collection API direct on this
 * model. The iterator returned by {@link #iterator()} does not support modifications on the list. The implementation is
 * not <code>Thread Safe</code>.
 *
 * @author Marcel Hanser
 * @param <E> the type
 */
public class ListListModel<E> extends AbstractListModel implements List<E> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private List<E> m_delegate;

    /**
     * Constructor.
     */
    public ListListModel() {
        super();
        m_delegate = new ArrayList<E>();
    }

    /**
     * Constructor. see {@link ArrayList#ArrayList(int)}.
     *
     * @param initialCapacity see {@link ArrayList#ArrayList(int)}
     */
    public ListListModel(final int initialCapacity) {
        super();
        m_delegate = new ArrayList<E>(initialCapacity);
    }

    @Override
    public boolean remove(final Object o) {
        int indexOf = m_delegate.indexOf(o);
        if (indexOf >= 0) {
            m_delegate.remove(indexOf);
            fireIntervalRemoved(this, indexOf, indexOf);
            return true;
        }
        return false;
    }

    @Override
    public E set(final int index, final E element) {
        E set = m_delegate.set(index, element);
        if (set != null) {
            fireContentsChanged(this, index, index);
        }
        return set;
    }

    @Override
    public boolean add(final E e) {
        add(size(), e);
        return true;
    }

    @Override
    public void add(final int index, final E element) {
        m_delegate.add(index, element);
        fireIntervalAdded(this, index, index);
    }

    @Override
    public E remove(final int index) {
        E remove = m_delegate.remove(index);
        if (remove != null) {
            fireIntervalRemoved(this, index, index);
        }
        return remove;
    }

    @Override
    public boolean addAll(final Collection<? extends E> c) {
        return addAll(size(), c);
    }

    @Override
    public boolean addAll(final int index, final Collection<? extends E> c) {
        boolean addAll = m_delegate.addAll(index, c);
        if (addAll) {
            fireIntervalAdded(this, index, index + c.size() - 1);
        }
        return addAll;
    }

    @Override
    public void clear() {
        int index1 = m_delegate.size() - 1;
        m_delegate.clear();
        if (index1 >= 0) {
            fireIntervalRemoved(this, 0, index1);
        }
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        boolean toReturn = false;
        for (Object o : c) {
            toReturn |= remove(o);
        }
        return toReturn;
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        throw new UnsupportedOperationException("retainAll not supported.");
    }

    // READ only methods
    @Override
    public boolean equals(final Object o) {
        return m_delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return m_delegate.hashCode();
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        return m_delegate.containsAll(c);
    }

    @Override
    public int getSize() {
        return m_delegate.size();
    }

    @Override
    public E getElementAt(final int index) {
        return m_delegate.get(index);
    }

    @Override
    public int size() {
        return m_delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return m_delegate.isEmpty();
    }

    @Override
    public boolean contains(final Object o) {
        return m_delegate.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return new ImmutableIterator(m_delegate.iterator());
    }

    @Override
    public Object[] toArray() {
        return m_delegate.toArray();
    }

    @Override
    public <T> T[] toArray(final T[] a) {
        return m_delegate.toArray(a);
    }

    @Override
    public E get(final int index) {
        return m_delegate.get(index);
    }

    @Override
    public int indexOf(final Object o) {
        return m_delegate.indexOf(o);
    }

    @Override
    public int lastIndexOf(final Object o) {
        return m_delegate.lastIndexOf(o);
    }

    @Override
    public ListIterator<E> listIterator() {
        return new ListIteratorImpl(m_delegate.listIterator());
    }

    @Override
    public ListIterator<E> listIterator(final int index) {
        return new ListIteratorImpl(m_delegate.listIterator(index));
    }

    @Override
    public List<E> subList(final int fromIndex, final int toIndex) {
        return m_delegate.subList(fromIndex, toIndex);
    }

    private class ImmutableIterator implements Iterator<E> {
        private final Iterator<E> m_itDelegate;

        /**
         * @param delegate
         */
        protected ImmutableIterator(final Iterator<E> delegate) {
            super();
            this.m_itDelegate = delegate;
        }

        @Override
        public boolean hasNext() {
            return m_itDelegate.hasNext();
        }

        @Override
        public E next() {
            return m_itDelegate.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove not supported");
        }

    }

    private class ListIteratorImpl extends ImmutableIterator implements ListIterator<E> {
        private final ListIterator<E> m_itDelegate;

        /**
         * @param delegate
         */
        protected ListIteratorImpl(final ListIterator<E> delegate) {
            super(delegate);
            this.m_itDelegate = delegate;
        }

        @Override
        public boolean hasPrevious() {
            return m_itDelegate.hasPrevious();
        }

        @Override
        public E previous() {
            return m_itDelegate.previous();
        }

        @Override
        public int nextIndex() {
            return m_itDelegate.nextIndex();
        }

        @Override
        public int previousIndex() {
            return m_itDelegate.previousIndex();
        }

        @Override
        public void set(final E e) {
            m_itDelegate.set(e);
            fireContentsChanged(ListListModel.this, previousIndex() + 1, previousIndex() + 1);
        }

        @Override
        public void add(final E e) {
            m_itDelegate.add(e);
            fireIntervalAdded(ListListModel.this, previousIndex() + 1, previousIndex() + 1);
        }
    }

}
