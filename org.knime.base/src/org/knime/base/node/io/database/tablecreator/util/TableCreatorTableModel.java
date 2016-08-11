/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * History
 *   Dec 3, 2015 (budiyanto): created
 */
package org.knime.base.node.io.database.tablecreator.util;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 *
 * @author Budi Yanto, KNIME.com
 */
abstract class TableCreatorTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private final String[] m_colNames;

    private final Class<?>[] m_classes;

    private final List<RowElement> m_elements;

    /**
     * Creates a new instance of TableCreatorTableModel
     *
     * @param colNames column names of the table model
     * @param colClasses column classes of the table model
     * @param elements underlying data structure to hold the table model data
     */
    TableCreatorTableModel(final String[] colNames, final Class<?>[] colClasses, final List<RowElement> elements) {
        if (colNames == null) {
            throw new NullPointerException("colNames must not be null");
        }

        if (colClasses == null) {
            throw new NullPointerException("colClasses must not be null");
        }

        if (colNames.length != colClasses.length) {
            throw new IllegalArgumentException("Number of column names and classes should be equal");
        }

        m_colNames = colNames;
        m_classes = colClasses;
        m_elements = elements;
    }

    /**
     * Removes all elements from the table model
     */
    void removeAll() {
        m_elements.clear();
        fireTableDataChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCount() {
        return m_elements.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getColumnCount() {
        return m_classes.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        return getValueAtRow(rowIndex, columnIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
        if (rowIndex < 0 || rowIndex >= m_elements.size()) {
            // this might happen if the user removes a row that he also
            // edited e.g. changed the name of the result column
            fireTableDataChanged();
            return;
        }
        setValue(aValue, rowIndex, columnIndex);
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getColumnClass(final int columnIndex) {
        return m_classes[columnIndex];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnName(final int column) {
        return m_colNames[column];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
        return isEditable(rowIndex, columnIndex);
    }

    /**
     * Removes elements at the given indices
     *
     * @param idxs indices of the elements to remove
     */
    protected void remove(final int... idxs) {
        if (idxs == null || idxs.length < 1) {
            return;
        }
        final List<RowElement> rows2remove = new LinkedList<>();
        for (final int idx : idxs) {
            rows2remove.add(m_elements.get(idx));
        }
        remove(rows2remove);
    }

    /**
     * Removes the given elements from the table model
     *
     * @param elements elements to remove
     */
    protected void remove(final Collection<RowElement> elements) {
        m_elements.removeAll(elements);
        fireTableDataChanged();
    }

    /**
     * Adds the given elements to the table model
     *
     * @param elements elements to add
     */
    protected void add(final List<RowElement> elements) {
        if (elements == null || elements.isEmpty()) {
            return;
        }
        if (m_elements.addAll(elements)) {
            // notify the rest if the rows have changed
            fireTableDataChanged();
        }
    }

    /**
     * Adds the given element to the table model
     *
     * @param element element to add
     */
    protected void add(final RowElement element) {
        if (element == null) {
            return;
        }
        int index = m_elements.size();
        if (m_elements.add(element)) {
            fireTableRowsInserted(index, index);
        }
    }

    /**
     * Updates the element at the given index
     *
     * @param rowIdx index of the element to update
     * @param row element used to update
     */
    protected void updateElement(final int rowIdx, final RowElement row) {
        m_elements.set(rowIdx, row);
        fireTableRowsUpdated(rowIdx, rowIdx);
    }

    /**
     * Returns all elements of the table model
     *
     * @return all elements of the table model
     */
    protected List<RowElement> getElements() {
        return Collections.unmodifiableList(m_elements);
    }

    /**
     * Returns element at the specified index
     *
     * @param row index of the element to return
     * @return element at the specified index
     */
    protected RowElement getElement(final int row) {
        if (row < 0 || getRowCount() <= row) {
            throw new IllegalArgumentException("Invalid row index: " + row);
        }
        return m_elements.get(row);
    }

    /**
     * Returns true if the table model is empty, otherwise returns false
     *
     * @return true if the table model is empty, otherwise false
     */
    protected boolean isEmpty() {
        return m_elements.isEmpty();
    }

    /**
     * Returns true if the cell at the specified row index and column index is editable, otherwise returns false
     *
     * @param rowIndex row index of the cell
     * @param columnIndex column index of the cell
     * @return true if the cell at the specified row index and column index is editable, otherwise false
     */
    protected abstract boolean isEditable(final int rowIndex, final int columnIndex);

    /**
     * Returns the value of the cell at the specified row index and column index
     *
     * @param rowIndex row index of the cell
     * @param columnIndex column index of the cell
     * @return the value of the cell at the specified row index and column index
     */
    protected abstract Object getValueAtRow(final int rowIndex, final int columnIndex);

    /**
     * Sets the value of the cell at the specified row index and column index
     *
     * @param aValue a new value to set
     * @param rowIndex row index of the cell
     * @param columnIndex column index of the cell
     */
    protected abstract void setValue(final Object aValue, final int rowIndex, final int columnIndex);

}
