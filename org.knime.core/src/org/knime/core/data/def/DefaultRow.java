/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 * --------------------------------------------------------------------- *
 * History
 *   21.06.06 (bw & po): reviewed
 */
package org.knime.core.data.def;

import java.util.Iterator;
import java.util.List;

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
     * @param rowId A {@link String} containing a row Id.
     * @param row An array containing the actual data of this row.
     * @throws NullPointerException if the specified rowID is <code>null</code>
     * @throws NullPointerException if <code>row</code> or one of its
     *             cells is <code>null</code>.
     */
    public DefaultRow(final String rowId, final DataCell... row) {
        this(new RowKey(rowId), row);
    }
    
    /**
     * Inits a new <code>DefaultRow</code> object by row id and an array of
     * <code>DataCell</code>s. The content of the argument array is copied.
     * 
     * @param rowId a {@link String} containing a row Id
     * @param row a list containing the actual data of this row
     * @throws NullPointerException if the specified rowID is <code>null</code>
     * @throws NullPointerException if <code>row</code> or one of its
     *             cells is <code>null</code>.
     */
    public DefaultRow(final String rowId, final List<DataCell> row) {
        this(rowId, row.toArray(new DataCell[row.size()]));
    }

    /**
     * Inits a new <code>DefaultRow</code> object by row id and an array of
     * <code>DataCell</code>s. The content of the argument array is copied.
     * 
     * @param rowKey a {@link RowKey} containing a unique row Id
     * @param row a list containing the actual data of this row
     * @throws NullPointerException if the specified row key is
     *             <code>null</code>
     * @throws NullPointerException if <code>row</code> or one of its
     *             cells is <code>null</code>.
     */
    public DefaultRow(final RowKey rowKey, final List<DataCell> row) {
        this(rowKey, row.toArray(new DataCell[row.size()]));
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
    public DefaultRow(final String rowId, final double... row) {
        this(new RowKey(rowId), row);
    }
    
    /**
     * Inits a new <code>DefaultRow</code> object by row id and an array of
     * <code>int</code> values.
     * 
     * Checks if none of the arguments is <code>null</code>.
     * 
     * @param rowId a {@link String} containing a unique row Id.
     * @param row an array containing the actual data of this row.
     * @throws NullPointerException if the <code>rowKey</code> or
     *             <code>row</code> is <code>null</code>.
     */
    public DefaultRow(final String rowId, final int... row) {
        this(new RowKey(rowId), row);
    }
    
    /**
     * Inits a new <code>DefaultRow</code> object by row key and an array of
     * <code>int</code> values.
     * 
     * Checks if none of the arguments is <code>null</code>.
     * 
     * @param rowKey a {@link RowKey} containing a unique row Id.
     * @param row an array containing the actual data of this row.
     * @throws NullPointerException if the <code>rowKey</code> or
     *             <code>row</code> is <code>null</code>.
     */
    public DefaultRow(final RowKey rowKey, final int... row) {
        // check row key
        if (rowKey == null) {
            throw new NullPointerException("Row id must not be null!");
        }
        // check row array
        if (row == null) {
            throw new NullPointerException("Row array must not be null!");
        }
        // create new RowKey where all properties are set to default values.
        m_rowKey = rowKey;
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
     * @param rowId A {@link String} containing a unique row Id.
     * @param row An array containing the actual data of this row.
     * @throws NullPointerException if the <code>rowId</code> or
     *             <code>row</code> or one of its strings is <code>null</code>.
     */
    public DefaultRow(final String rowId, final String... row) {
        this(new RowKey(rowId), row);
    }

    /**
     * Inits a new <code>DefaultRow</code> object by row key and an array of
     * {@link String} values.
     * 
     * @param rowKey a {@link RowKey} containing a unique row Id
     * @param row an array containing the actual data of this row
     * @throws NullPointerException if the <code>rowId</code> or
     *             <code>row</code> or one of its strings is <code>null</code>.
     */
    public DefaultRow(final RowKey rowKey, final String... row) {
        // check row key
        if (rowKey == null) {
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
        m_rowKey = rowKey;
    }
    
    /**
     * Creates an new row, using the data of the specified row, and overwrites
     * the row key with the given new one.
     * 
     * @param rowId the row id to create the new row key.
     * @param row The row to copy.
     * @throws IllegalArgumentException If the key or one of the row's cells
     *             is <code>null</code>.
     * @throws NullPointerException If the row is <code>null</code>.
     */
    public DefaultRow(final String rowId, final DataRow row) {
        this(new RowKey(rowId), row);
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
     * {@inheritDoc}
     */
    public final int getNumCells() {
        return m_row.length;
    }

    /**
     * {@inheritDoc}
     */
    public final RowKey getKey() {
        return m_rowKey;
    }

    /**
     * {@inheritDoc}
     */
    public final DataCell getCell(final int index) {
        return m_row[index];
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getKey().hashCode();
    }
}
