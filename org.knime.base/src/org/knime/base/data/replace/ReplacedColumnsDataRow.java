/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.base.data.replace;

import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultCellIterator;


/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class ReplacedColumnsDataRow implements DataRow {
    private final DataRow m_row;

    private final int[] m_columns;

    private final DataCell[] m_newCells;

    /**
     * Creates a new replaced column row.
     * 
     * @param row the row to replace one or more columns in
     * @param newCells the new cells
     * @param columns at positions
     * @throws IndexOutOfBoundsException if one of the column indices is not
     *             inside the row
     * @throws NullPointerException if the replace cell is <code>null</code>
     */
    public ReplacedColumnsDataRow(final DataRow row, final DataCell[] newCells,
            final int[] columns) {
        for (int column : columns) {
            if (column < 0 || column >= row.getNumCells()) {
                throw new IndexOutOfBoundsException("Index invalid: " + column);
            }
        }
        for (DataCell newCell : newCells) {
            if (newCell == null) {
                throw new NullPointerException("New cell must not be null");
            }
        }
        m_row = row;
        m_columns = columns;
        m_newCells = newCells;
    }

    /**
     * Convenience constructor that replaces one cell only. This constructor
     * calls:
     * 
     * <pre>
     * this(row, new DataCell[]{newCell}, new int[]{column});
     * </pre>.
     * 
     * @param row the row to replace one column in
     * @param newCell the new cell
     * @param column the column to be replaced
     */
    public ReplacedColumnsDataRow(final DataRow row, final DataCell newCell,
            final int column) {
        this(row, new DataCell[]{newCell}, new int[]{column});
    }

    /**
     * {@inheritDoc}
     */
    public int getNumCells() {
        return m_row.getNumCells();
    }

    /**
     * {@inheritDoc}
     */
    public RowKey getKey() {
        return m_row.getKey();
    }

    /**
     * {@inheritDoc}
     */
    public DataCell getCell(final int index) {
        for (int i = 0; i < m_columns.length; i++) {
            if (index == m_columns[i]) {
                return m_newCells[i];
            }
        }
        return m_row.getCell(index);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<DataCell> iterator() {
        return new DefaultCellIterator(this);
    }
}
