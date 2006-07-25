/*
 * --------------------------------------------------------------------- *
 *   This source code, its documentation and all appendant files         *
 *   are protected by copyright law. All rights reserved.                *
 *                                                                       *
 *   Copyright, 2003 - 2006                                              *
 *   Universitaet Konstanz, Germany.                                     *
 *   Lehrstuhl fuer Angewandte Informatik                                *
 *   Prof. Dr. Michael R. Berthold                                       *
 *                                                                       *
 *   You may not modify, publish, transmit, transfer or sell, reproduce, *
 *   create derivative works from, distribute, perform, display, or in   *
 *   any way exploit any of the content, in whole or in part, except as  *
 *   otherwise expressly permitted in writing by the copyright owner.    *
 * --------------------------------------------------------------------- *
 */
package de.unikn.knime.base.data.filter.column;

import java.util.Iterator;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.RowKey;
import de.unikn.knime.core.data.def.DefaultCellIterator;

/**
 * Filter {@link DataRow} which extracts particular cells (columns) from an
 * underlying row.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class FilterColumnRow implements DataRow {

    /**
     * Underlying row.
     */
    private final DataRow m_row;

    /**
     * Array of column indices.
     */
    private final int[] m_columns;

    /**
     * Inits a new filter column {@link DataRow} with the underling row and an
     * array of indices into this row.
     * 
     * @param row the underlying {@link DataRow}
     * @param columns the array of column indices to keep
     */
    FilterColumnRow(final DataRow row, final int[] columns) {
        m_row = row;
        m_columns = columns;
    }

    /**
     * @see de.unikn.knime.core.data.DataRow#getNumCells()
     */
    public int getNumCells() {
        return m_columns.length;
    }

    /**
     * @see de.unikn.knime.core.data.DataRow#getKey()
     */
    public RowKey getKey() {
        return m_row.getKey();
    }

    /**
     * Returns the data cell at the given <code>index</code>.
     * 
     * @param index the column index inside the row
     * @return the data cell for index
     * @throws ArrayIndexOutOfBoundsException if the <code>index</code> is out
     *             of range
     */
    public DataCell getCell(final int index) {
        return m_row.getCell(m_columns[index]);
    }

    /**
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<DataCell> iterator() {
        return new DefaultCellIterator(this);
    }
}
