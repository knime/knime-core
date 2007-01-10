/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * --------------------------------------------------------------------- *
 * History
 *   21.06.06 (bw & po): reviewed
 */
package org.knime.core.data.def;

import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;


/**
 * Default row for {@link DataCell}s which keeps a row identifier
 * and an array of {@link DataCell} objects.
 * 
 * @author ohl, University of Konstanz
 */
public class DefaultRow implements DataRow {

    /**
     * Keeps the row key.
     */
    private final RowKey m_rowKey;

    /**
     * Stores content of the row.
     */
    private final DataCell[] m_row;

    /**
     * Inits a new <code>DefaultRow</code> object by row key and an array of
     * {@link DataCell}s. The content of the argument array is copied.
     * 
     * @param rowKey A {@link RowKey} containing a row Id.
     * @param row An array containing the actual data of this row.
     * @throws IllegalArgumentException if the <code>rowKey</code> or
     *             <code>row</code> or one of its cell is <code>null</code>.
     */
    public DefaultRow(final RowKey rowKey, final DataCell... row) {
        // check row key
        if (rowKey == null) {
            throw new NullPointerException("Row ID must not be null!");
        }

        // check row array
        if (row == null) {
            throw new NullPointerException("Row array must not be null!");
        }

        m_row = new DataCell[row.length];

        // check row elements and copy them into our private array
        for (int i = 0; i < row.length; i++) {
            if (row[i] == null) {
                throw new NullPointerException("Cell at index " + i
                        + " is null!");
            }
            m_row[i] = row[i];
        }

        m_rowKey = rowKey;
    }

    /**
     * Inits a new <code>DefaultRow</code> object by row id and an array of
     * <code>DataCell</code>s. The content of the argument array is copied.
     * 
     * @param rowId A {@link DataCell} containing a row Id.
     * @param row An array containing the actual data of this row.
     * @throws NullPointerException if the specified rowID is <code>null</code>
     * @throws NullPointerException if <code>row</code> or one of its
     *             cells is <code>null</code>.
     */
    public DefaultRow(final DataCell rowId, final DataCell... row) {
        this(new RowKey(rowId), row);
    }

    /**
     * Inits a new <code>DefaultRow</code> object by row key and an array of
     * <code>double</code> values.
     * @param rowKey A {@link RowKey} containing a unique row Id.
     * @param row An array containing the actual data of this row.
     * @throws NullPointerException if the <code>rowKey</code> or
     *             <code>row</code> is <code>null</code>.
     */
    public DefaultRow(final RowKey rowKey, final double... row) {

        // check row key
        if (rowKey == null) {
            throw new NullPointerException("Row id must not be null!");
        }
        // check row array
        if (row == null) {
            throw new NullPointerException("Value array must not be null!");
        }
        m_rowKey = rowKey;
        // init row with cells
        m_row = new DataCell[row.length];
        // of all values in row array
        for (int i = 0; i < m_row.length; i++) {
            m_row[i] = new DoubleCell(row[i]);
        }
    }

    /**
     * Inits a new <code>DefaultRow</code> object by row ID and an array of
     * <code>double</code> values.
     * 
     * @param rowId to be wrapped in a {@link RowKey} object
     * @param row the values in the row
     * @throws NullPointerException As soon as the other constructor does.
     * @see #DefaultRow(RowKey, double[])
     */
    public DefaultRow(final DataCell rowId, final double... row) {
        this(new RowKey(rowId), row);
    }

    /**
     * Inits a new <code>DefaultRow</code> object by row key and an array of
     * <code>int</code> values.
     * 
     * Checks if none of the arguments is <code>null</code>.
     * 
     * @param rowId a {@link DataCell} containing a unique row Id.
     * @param row an array containing the actual data of this row.
     * @throws NullPointerException if the <code>rowKey</code> or
     *             <code>row</code> is <code>null</code>.
     */
    public DefaultRow(final DataCell rowId, final int... row) {
        // check row key
        if (rowId == null) {
            throw new NullPointerException("Row id must not be null!");
        }
        // check row array
        if (row == null) {
            throw new NullPointerException("Row array must not be null!");
        }
        // create new RowKey where all properties are set to default values.
        m_rowKey = new RowKey(rowId);
        // init row with cells
        m_row = new DataCell[row.length];
        // of all values in the row array
        for (int i = 0; i < m_row.length; i++) {
            m_row[i] = new IntCell(row[i]);
        }
    }

    /**
     * Inits a new <code>DefaultRow</code> object by row id and an array of
     * {@link String} values.
     * 
     * @param rowId A {@link DataCell} containing a unique row Id.
     * @param row An array containing the actual data of this row.
     * @throws NullPointerException if the <code>rowId</code> or
     *             <code>row</code> or one of its strings is <code>null</code>.
     */
    public DefaultRow(final DataCell rowId, final String... row) {
        // check row key
        if (rowId == null) {
            throw new NullPointerException("Row id must not be null!");
        }
        // check row array
        if (row == null) {
            throw new NullPointerException("Row array must not be null!");
        }
        // check row elements and copy them into our private array
        m_row = new DataCell[row.length];
        for (int i = 0; i < row.length; i++) {
            if (row[i] == null) {
                throw new NullPointerException("String at index " + i 
                        + " is null!");
            }
            m_row[i] = new StringCell(row[i]);
        }
        // create new RowKey where all properties are set to default values.
        m_rowKey = new RowKey(rowId);
    }

    /**
     * Creates an new row, using the data of the specified row, and overwrites
     * the row key with the given new one.
     * 
     * @param key The new row's key.
     * @param row The row to copy.
     * @throws IllegalArgumentException If the key or one of the row's cells
     *             is <code>null</code>.
     * @throws NullPointerException If the row is <code>null</code>.
     */
    public DefaultRow(final RowKey key, final DataRow row) {
        if (key == null) {
            throw new IllegalArgumentException("Row ID can't be null.");
        }
        m_rowKey = key;
        m_row = new DataCell[row.getNumCells()];
        for (int i = 0; i < m_row.length; i++) {
            m_row[i] = row.getCell(i);
            if (m_row[i] == null) {
                throw new IllegalArgumentException(
                        "A row element can't be null.");
            }
        }
    }

    /**
     * @see org.knime.core.data.DataRow#getNumCells()
     */
    public final int getNumCells() {
        return m_row.length;
    }

    /**
     * @see org.knime.core.data.DataRow#getKey()
     */
    public final RowKey getKey() {
        return m_rowKey;
    }

    /**
     * @see org.knime.core.data.DataRow#getCell(int)
     */
    public final DataCell getCell(final int index) {
        return m_row[index];
    }

    /**
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<DataCell> iterator() {
        return new DefaultCellIterator(this);
    }

    /**
     * Get a string representing this row, i.e. "rowkey: (cell1, ..., celln)"
     * 
     * @return key + values of this row in a string
     */
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer(getKey().toString());
        buffer.append(": (");
        for (int i = 0; i < getNumCells(); i++) {
            buffer.append(getCell(i).toString());
            // separate by ", "
            if (i != getNumCells() - 1) {
                buffer.append(", ");
            }
        }
        buffer.append(")");
        return buffer.toString();
    }

    /**
     * A row is equal to another one if the key and all cells are equal.
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DataRow)) {
            return false;
        }
        DataRow otherRow = (DataRow)obj;
        if (otherRow.getNumCells() != getNumCells()
                || !otherRow.getKey().equals(getKey())) {
            return false;
        }
        for (int i = 0; i < getNumCells(); i++) {
            if (!otherRow.getCell(i).equals(getCell(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return getKey().hashCode();
    }
}
