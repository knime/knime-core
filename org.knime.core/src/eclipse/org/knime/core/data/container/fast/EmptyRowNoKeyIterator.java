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

/**
 *
 * @author dietzc
 */
class EmptyRowNoKeyIterator extends CloseableRowIterator {

    private final DataRow m_rowInstance;

    private final long m_size;

    private long m_index;

    public EmptyRowNoKeyIterator(final int numCells, final long size) {
        m_size = size;
        m_rowInstance = new CompletelyUnmaterializedRow(numCells);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        // No op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return m_index < m_size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataRow next() {
        m_index++;
        return m_rowInstance;
    }

    static class CompletelyUnmaterializedRow implements DataRow {

        private static final UnmaterializedCell INSTANCE = UnmaterializedCell.getInstance();

        private final int m_numCells;

        CompletelyUnmaterializedRow(final int numCells) {
            m_numCells = numCells;
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
            throw new IllegalStateException("RowKey requested but not available!");
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
