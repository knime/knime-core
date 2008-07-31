/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   Jun 22, 2006 (wiswedel): created
 */
package org.knime.core.data.container;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;

/**
 * Internal iterator class that concatenates two rows. The iterator assumes
 * that both underlying iterators return the row keys in the same order. No
 * check is done.
 * @author wiswedel, University of Konstanz
 */
class JoinTableIterator extends CloseableRowIterator {

    private final CloseableRowIterator m_itReference;
    private final CloseableRowIterator m_itAppended;
    private final int[] m_map;
    private final boolean[] m_flags;
    
    /**
     * Creates new iterator based on two iterators.
     * @param itReference The reference iterator, providing the keys, e.g.
     * @param itAppended The row to be appended.
     * @param map The internal map which columns are contributed from what 
     *         iterator
     * @param flags The flags from which row to use.
     */
    JoinTableIterator(final CloseableRowIterator itReference, 
            final CloseableRowIterator itAppended, final int[] map, 
            final boolean[] flags) {
        m_itReference = itReference;
        m_itAppended = itAppended;
        m_map = map;
        m_flags = flags;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return m_itReference.hasNext();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public DataRow next() {
        DataRow ref = m_itReference.next();
        DataRow app = m_itAppended.next();
        DataCell[] cells = new DataCell[m_map.length];
        int sanityCount = 0;
        for (int i = 0; i < cells.length; i++) {
            if (m_flags[i]) {
                cells[i] = getUnwrappedCell(ref, m_map[i]);
            } else {
                cells[i] = getUnwrappedCell(app, m_map[i]);
                sanityCount++;
            }
        }
        if (sanityCount != app.getNumCells()) {
            throw new IllegalStateException("Additional cells as read " 
                    + "from file do not have the right count: " + sanityCount 
                    + " vs. " + app.getNumCells());
        }
        return new BlobSupportDataRow(ref.getKey(), cells);
    }
    
    /** {@inheritDoc} */
    @Override
    public void close() {
        m_itAppended.close();
        m_itReference.close();
    }
    
    private static DataCell getUnwrappedCell(final DataRow row, final int i) {
        return row instanceof BlobSupportDataRow 
            ? ((BlobSupportDataRow)row).getRawCell(i) : row.getCell(i);
    }

}
