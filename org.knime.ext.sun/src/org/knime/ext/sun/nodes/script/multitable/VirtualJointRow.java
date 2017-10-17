/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   19 Jan 2017 (stefano): created
 */
package org.knime.ext.sun.nodes.script.multitable;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;

/**
 * Virtual row that contains two rows from different tables and makes them transparently accessible as one row.
 *
 * @author Stefano Woerner
 */
public class VirtualJointRow implements DataRow {

    private DataRow m_leftRow;
    private DataRow m_rightRow;
    private int m_numCells;
    private int m_leftNumCells;

    /**
     * Separates the two row IDs.
     */
    public static final String ROWID_SEPARATOR = "_";

    /**
     * Creates a new instance of <code>VirtualJointRow</code>.
     *
     * @param leftRow the row from the left table
     * @param rightRow the row from the right table
     */
    public VirtualJointRow(final DataRow leftRow, final DataRow rightRow) {
        m_leftRow = leftRow;
        m_rightRow = rightRow;
        m_leftNumCells = leftRow.getNumCells();
        m_numCells = m_leftNumCells + rightRow.getNumCells();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<DataCell> iterator() {
        return new JointRowCellIterator(m_leftRow.iterator(), m_rightRow.iterator());
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
        return new RowKey(m_leftRow.getKey().toString() + ROWID_SEPARATOR + m_rightRow.getKey().toString());
    }

    /**
     * @return the row key of the left row
     */
    public RowKey getLeftKey() {
        return m_leftRow.getKey();
    }

    /**
     * @return the row key of the right row
     */
    public RowKey getRightKey() {
        return m_rightRow.getKey();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell getCell(final int index) {
        if (index < m_leftNumCells) {
            return m_leftRow.getCell(index);
        } else {
            return m_rightRow.getCell(index - m_leftNumCells);
        }
    }

    /**
     * Joint iterator for joint row.
     *
     * @author Stefano Woerner
     */
    public class JointRowCellIterator implements Iterator<DataCell> {

        private Iterator<DataCell> m_leftIterator;
        private Iterator<DataCell> m_rightIterator;

        /**
         * Initialize with two iterators.
         *
         * @param leftIterator iterator of left row
         * @param rightIterator iterator of right row
         */
        public JointRowCellIterator(final Iterator<DataCell> leftIterator, final Iterator<DataCell> rightIterator) {
            this.m_leftIterator = leftIterator;
            this.m_rightIterator = rightIterator;
        }

        @Override
        public boolean hasNext() {
            return m_leftIterator.hasNext() || m_rightIterator.hasNext();
        }

        @Override
        public DataCell next() {
            if (m_leftIterator.hasNext()) {
                return m_leftIterator.next();
            } else if (m_rightIterator.hasNext()) {
                return m_rightIterator.next();
            } else {
                throw new NoSuchElementException("Iterator is at end.");
            }
        }

        /**
         * Throws {@link UnsupportedOperationException} as removal of datacells from
         * a row is not permitted.
         *
         * @see java.util.Iterator#remove()
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException(
                    "Removing cells is not allowed.");
        }
    }

}
