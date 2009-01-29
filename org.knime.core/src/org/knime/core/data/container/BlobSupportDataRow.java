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
 * History
 *   Dec 15, 2006 (wiswedel): created
 */
package org.knime.core.data.container;

import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultCellIterator;

/**
 * Special row implementation that supports to access the wrapper cells of
 * {@link BlobDataCell}. Dealing with the wrapper cells 
 * ({@link BlobWrapperDataCell}) gives the benefit that blobs are not read
 * from the file when passed from one place to another (they will be read
 * on access).
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class BlobSupportDataRow implements DataRow {
    
    private final RowKey m_key;
    private final DataCell[] m_cells;

    /**
     * @param key Row key
     * @param cells cell array.
     */
    BlobSupportDataRow(final RowKey key, final DataCell[] cells) {
        m_key = key;
        m_cells = cells;
    }

    /**
     * If the cell at index is a blob wrapper cell, it will fetch the content
     * and return it.
     * {@inheritDoc}
     */
    public DataCell getCell(final int index) {
        DataCell c = m_cells[index];
        if (c instanceof BlobWrapperDataCell) {
            return ((BlobWrapperDataCell)c).getCell();
        }
        return c;
    }
    
    /** Returns the cell at given index. Returns the wrapper cell (if any).
     * @param index Cell index.
     * @return Raw cell.
     */
    public DataCell getRawCell(final int index) {
        return m_cells[index];
    }

    /**
     * {@inheritDoc}
     */
    public RowKey getKey() {
        return m_key;
    }

    /**
     * {@inheritDoc}
     */
    public int getNumCells() {
        return m_cells.length;
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

}
