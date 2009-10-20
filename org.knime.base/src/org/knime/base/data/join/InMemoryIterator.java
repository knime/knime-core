/* Created on Jul 6, 2006 2:46:47 PM by thor
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
