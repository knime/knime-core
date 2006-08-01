/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 */
package de.unikn.knime.base.data.append.row;

import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultCellIterator;


/**
 * A row that takes a base row and re-sorts the cells in it according to and
 * <code>int[]</code> parameter passed in the constructor.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class ResortedCellsRow implements DataRow {

    private final DataRow m_row;

    private final int[] m_sort;

    /**
     * Creates new row with <code>row</code> as underlying base row and
     * <code>sort</code> the new sorting scheme. That is the old
     * <code>i</code>-th entry becomes entry number <code>sort[i]</code>.
     * 
     * @param row the base row
     * @param sort the re-sorting
     * @throws IllegalArgumentException if the lengths of arrays don't match
     * @throws NullPointerException if either argument is <code>null</code>
     */
    protected ResortedCellsRow(final DataRow row, final int[] sort) {
        if (row.getNumCells() != sort.length) {
            throw new IllegalArgumentException("Length don't match.");
        }
        m_row = row;
        m_sort = sort;
    }

    /**
     * @see org.knime.core.data.DataRow#getNumCells()
     */
    public int getNumCells() {
        return m_row.getNumCells();
    }

    /**
     * @see org.knime.core.data.DataRow#getKey()
     */
    public RowKey getKey() {
        return m_row.getKey();
    }

    /**
     * @see org.knime.core.data.DataRow#getCell(int)
     */
    public DataCell getCell(final int index) {
        return m_row.getCell(m_sort[index]);
    }

    /**
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<DataCell> iterator() {
        return new DefaultCellIterator(this);
    }
}
