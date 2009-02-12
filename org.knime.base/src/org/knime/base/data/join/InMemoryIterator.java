/* Created on Jul 6, 2006 2:46:47 PM by thor
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
package org.knime.base.data.join;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.knime.core.data.DataRow;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.node.BufferedDataTable;


/**
 * This iterator joins two tables (given by a
 * {@link JoinedTable}) in memory and is therfore
 * magnituded faster than the standard joiner. However, it needs more memory,
 * depending on the number and size of the data rows in the smaller table.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
class InMemoryIterator extends RowIterator {
    private final Map<RowKey, DataRow> m_smallerTableMap;

    private final RowIterator m_biggerTableIterator;

    private DataRow[] m_nextRow;

    private final boolean m_inverted;

    /**
     * Creates a new iterator for an in memory join of two tables.
     * 
     * @param table the JoinedTable
     */
    public InMemoryIterator(final JoinedTable table) {
        final int left;
        final int right;
        if (table.getLeftTable() instanceof BufferedDataTable) {
            left = ((BufferedDataTable)table.getLeftTable()).getRowCount();
        } else {
            left = -1;
        }
        if (table.getRightTable() instanceof BufferedDataTable) {
            right = ((BufferedDataTable)table.getRightTable()).getRowCount();
        } else {
            right = -1;
        }
        if (left > right) {
            m_smallerTableMap = new HashMap<RowKey, DataRow>();
            for (DataRow row : table.getRightTable()) {
                m_smallerTableMap.put(row.getKey(), row);
            }
            m_biggerTableIterator = table.getLeftTable().iterator();
            m_inverted = false;
        } else {
            m_smallerTableMap = new HashMap<RowKey, DataRow>();
            for (DataRow row : table.getLeftTable()) {
                m_smallerTableMap.put(row.getKey(), row);
            }
            m_biggerTableIterator = table.getRightTable().iterator();
            m_inverted = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return (m_nextRow != null) || (getNextMatch() != null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataRow next() {
        if (m_nextRow == null) {
            if (getNextMatch() == null) {
                throw new NoSuchElementException("No more rows");
            }
        }

        DataRow row;
        if (m_inverted) {
            row = new JoinedRow(m_nextRow[1], m_nextRow[0]);
        } else {
            row = new JoinedRow(m_nextRow[0], m_nextRow[1]);
        }
        m_nextRow = null;
        return row;
    }

    private DataRow[] getNextMatch() {
        m_nextRow = null;
        if (!m_biggerTableIterator.hasNext()) {
            return null;
        }

        while (m_biggerTableIterator.hasNext()) {
            DataRow rowA = m_biggerTableIterator.next();
            DataRow rowB = m_smallerTableMap.get(rowA.getKey());
            if (rowB != null) {
                m_nextRow = new DataRow[]{rowA, rowB};
                break;
            }
        }

        return m_nextRow;
    }
}
