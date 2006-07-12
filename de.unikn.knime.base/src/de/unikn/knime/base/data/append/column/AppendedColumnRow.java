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
 */
package de.unikn.knime.base.data.append.column;

import java.util.Iterator;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.RowKey;
import de.unikn.knime.core.data.def.DefaultCellIterator;

/**
 * <code>DataRow</code> that is extended by one or more cells.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class AppendedColumnRow implements DataRow {
    
    private final DataRow m_baseRow;
    private final DataCell[] m_appendCell;

    /**
     * Creates new Row with <code>baseRow</code> providing the first cells
     * and <code>appendCell</code> as last cells.
     * @param baseRow Containing the first cells
     * @param appendCell The last cells (to be appended).
     */
    public AppendedColumnRow(
            final DataRow baseRow, final DataCell... appendCell) {
        m_baseRow = baseRow;
        m_appendCell = appendCell;
    }

    /**
     * @see de.unikn.knime.core.data.DataRow#getNumCells()
     */
    public int getNumCells() {
        return m_baseRow.getNumCells() + m_appendCell.length;
    }

    /**
     * @see de.unikn.knime.core.data.DataRow#getKey()
     */
    public RowKey getKey() {
        return m_baseRow.getKey();
    }

    /**
     * @see de.unikn.knime.core.data.DataRow#getCell(int)
     */
    public DataCell getCell(final int index) {
        if (index < 0 || index >= getNumCells()) {
            throw new IndexOutOfBoundsException("Index " + index + " out of " 
                    + "range [" + 0 + ", " + (getNumCells() - 1) + "]"); 
        }
        if (index < m_baseRow.getNumCells()) {
            return m_baseRow.getCell(index);
        }
        return m_appendCell[index - m_baseRow.getNumCells()];
    }
    
    /**
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<DataCell> iterator() {
        return new DefaultCellIterator(this);
    }

}
