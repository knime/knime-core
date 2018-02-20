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
 *   Jan 30, 2018 (jschweig): created
 */
package org.knime.core.node.util.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.table.AbstractTableModel;

/**
 * {@link ArrayList} implementation of an {@link AbstractTableModel} with only one column
 *
 * @param <E> the type of the elements of this model
 *
 * @author Johannes Schweig, KNIME GmbH, Konstanz, Germany
 * @since 3.6
 */
@SuppressWarnings("serial")
class NameFilterTableModel<E> extends AbstractTableModel implements Iterable<E>{

    private ArrayList<E> m_delegate = new ArrayList<E>();

    /**
     * Tests whether this list has any components.
     *
     * @return <code>true</code> if and only if this list has no components, that is, its size is zero;
     *         <code>false</code> otherwise
     * @see ArrayList#isEmpty()
     */
    boolean isEmpty () {
        return m_delegate.isEmpty();
    }

    /**
     * Tests whether the specified object is a component in this list.
     *
     * @param obj an object
     * @return <code>true</code> if the specified object is the same as a component in this list
     * @see ArrayList#contains(Object)
     */
    boolean contains(final Object obj) {
        return m_delegate.contains(obj);
    }

    /**
     * Removes the first (lowest-indexed) occurrence of the argument from this list.
     *
     * @param obj the component to be removed
     * @return <code>true</code> if the argument was a component of this list; <code>false</code> otherwise
     * @see ArrayList#remove(Object)
     */
    boolean remove (final Object obj) {
        boolean rv = m_delegate.remove(obj);
        fireTableDataChanged();
        return rv;
    }

    /**
     * Removes the element at the specified position in this list. Returns the element that was removed from the list.
     *
     * @param index the index of the element to removed
     * @return the element previously at the specified position
     */
    E remove (final int index) {
        E rv = m_delegate.remove(index);
        fireTableRowsDeleted(index, index);
        return rv;
    }

    /**
     * Removes all of the elements from this list.
     */
    void clear() {
        int size = getSize();
        m_delegate.clear();
        if (size > 0) {
            fireTableRowsDeleted(0, size-1);
        }
    }

    /**
     * @param c collection containing elements to be added to this list
     * @return true if this list changed as a result of the call
     * @see ArrayList#removeAll(Collection)
     */
    boolean removeAll (final Collection<E> c){
        m_delegate.removeAll(c);
        fireTableDataChanged();
        return true;
    }

    /**
     * Adds the specified component to the end of this list.
     *
     * @param element the component to be added
     * @see ArrayList#add(Object)
     */
    void add (final E element){
        int index = getSize();
        m_delegate.add(element);
        fireTableRowsInserted(index, index);
    }

    /**
     * @param c collection containing elements to be added to this list
     * @see ArrayList#addAll(Collection)
     */
    void addAll (final Collection<E> c){
        if (c.isEmpty()) {
            return;
        }
        int index = getSize();
        m_delegate.addAll(c);
        fireTableRowsInserted(index, index + c.size() - 1);
    }

    /**
     * Returns the number of components in this list.
     *
     * @return the number of components in this list
     * @see ArrayList#size()
     */
    int getSize(){
        return getRowCount();
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index index of element to return
     * @return the element at the specified index
     */
    Object getElementAt(final int index) {
        return getValueAt(index, 0);
    }

   /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCount() {
        return m_delegate.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getColumnCount() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        return m_delegate.get(rowIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnName (final int column) {
        return "column";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<E> iterator() {
        return m_delegate.iterator();
    }
}
