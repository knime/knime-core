/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   Dec 15, 2006 (wiswedel): created
 */
package org.knime.core.data.container;

import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultCellIterator;

class BlobSupportDataRow implements DataRow {
    
    private final RowKey m_key;
    private final DataCell[] m_cells;

    BlobSupportDataRow(final RowKey key, final DataCell[] cells) {
        m_key = key;
        m_cells = cells;
    }

    /**
     * @see org.knime.core.data.DataRow#getCell(int)
     */
    public DataCell getCell(final int index) {
        DataCell c = m_cells[index];
        if (c instanceof BlobWrapperDataCell) {
            return ((BlobWrapperDataCell)c).getCell();
        }
        return c;
    }
    
    DataCell getRawCell(final int index) {
        return m_cells[index];
    }

    /**
     * @see DataRow#getKey()
     */
    public RowKey getKey() {
        return m_key;
    }

    /**
     * @see DataRow#getNumCells()
     */
    public int getNumCells() {
        return m_cells.length;
    }

    /**
     * @see Iterable#iterator()
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

}
