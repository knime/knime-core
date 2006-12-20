/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.base.data.join;

import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultCellIterator;


/**
 * Row that concatenates two given rows.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class JoinedRow implements DataRow {
    
    /** Underlying left row. */
    private final DataRow m_left;
    /** And its right counterpart. */
    private final DataRow m_right;
    
    /**
     * Creates new row based on two given rows. 
     * @param left The left row providing the head cells
     * @param right The right row providing the tail cells 
     * @throws NullPointerException If either argument is null
     * @throws IllegalArgumentException If row key's ids aren't equal.
     */
    public JoinedRow(final DataRow left, final DataRow right) {
        DataCell lId = left.getKey().getId();
        DataCell rId = right.getKey().getId(); 
        if (!lId.equals(rId)) {
            throw new IllegalArgumentException("Key of rows do not match: \"" 
                    + lId + "\" vs. \"" + rId + "\"");
        }
        m_left = left;
        m_right = right;
    }

    /**
     * @see org.knime.core.data.DataRow#getNumCells()
     */
    public int getNumCells() {
        return m_left.getNumCells() + m_right.getNumCells();
    }

    /**
     * Returns the key from the left row that was passed in the constructor.
     * @see org.knime.core.data.DataRow#getKey()
     */
    public RowKey getKey() {
        return m_left.getKey();
    }

    /**
     * @see org.knime.core.data.DataRow#getCell(int)
     */
    public DataCell getCell(final int index) {
        final int leftCellCount = m_left.getNumCells();
        // I guess both implementation will IndexOutOfBounds if out of range,
        // and so do we.
        if (index < leftCellCount) {
            return m_left.getCell(index);
        } else {
            return m_right.getCell(index - leftCellCount);
        }
    }
    
    /**
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<DataCell> iterator() {
        return new DefaultCellIterator(this);
    }

}
