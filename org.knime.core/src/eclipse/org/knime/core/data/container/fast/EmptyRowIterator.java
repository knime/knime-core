/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ---------------------------------------------------------------------
 *
 * History
 *   May 3, 2020 (dietzc): created
 */
package org.knime.core.data.container.fast;

import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.UnmaterializedCell;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.table.ReadTable;
import org.knime.core.data.table.row.RowReadCursor;
import org.knime.core.data.table.value.StringReadValue;

/**
 *
 * @author dietzc
 */
class EmptyRowIterator extends CloseableRowIterator {

    private final int m_numCells;

    private final RowReadCursor m_cursor;

    private final StringReadValue m_rowKeyValue;

    public EmptyRowIterator(final ReadTable table, final int numCells) {
        m_numCells = numCells;
        m_cursor = table.newCursor();
        m_rowKeyValue = m_cursor.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        try {
            m_cursor.close();
        } catch (Exception ex) {
            // TODO
            throw new RuntimeException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return m_cursor.canFwd();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataRow next() {
        return new CompletelyUnmaterializedRow(m_rowKeyValue, m_numCells);
    }

    static class CompletelyUnmaterializedRow implements DataRow {

        private static final UnmaterializedCell INSTANCE = UnmaterializedCell.getInstance();

        private final StringReadValue m_rowKeyValue;

        private final int m_numCells;

        CompletelyUnmaterializedRow(final StringReadValue rowKeyValue, final int numCells) {
            m_numCells = numCells;
            m_rowKeyValue = rowKeyValue;
        }

        // TODO in case of filtering, over what columns do I actually iterate?
        @Override
        public Iterator<DataCell> iterator() {
            return new Iterator<DataCell>() {

                int i = 0;

                @Override
                public boolean hasNext() {
                    return i < m_numCells;
                }

                @Override
                public DataCell next() {
                    i++;
                    return INSTANCE;
                }
            };
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getNumCells() {
            return m_numCells;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public RowKey getKey() {
            return new RowKey(m_rowKeyValue.getStringValue());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell getCell(final int index) {
            return INSTANCE;
        }
    }

}
