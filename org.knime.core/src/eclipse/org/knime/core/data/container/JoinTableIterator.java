/*
 * ------------------------------------------------------------------------
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
        // the single & is on purpose because we want to call hasNext on both iterators, so that they can close
        // resources if the end of the stream has been reached; see AP-8055
        return m_itReference.hasNext() & m_itAppended.hasNext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataRow next() {
        DataRow ref = m_itReference.next();
        DataRow app = m_itAppended.next();
        return createOutputRow(ref, app, m_map, m_flags);
    }

    /** Assembles the output row. Used by {@link #next()}.
     * @param ref the original input row.
     * @param app the row with the new columns.
     * @param map The map containing which column maps to which index
     * @param flags which column from which row.
     * @return the assembled output data (including data reshuffling). */
    static final DataRow createOutputRow(final DataRow ref, final DataRow app,
            final int[] map, final boolean[] flags) {
        DataCell[] cells = new DataCell[map.length];
        int sanityCount = 0;
        for (int i = 0; i < cells.length; i++) {
            if (flags[i]) {
                cells[i] = getUnwrappedCell(ref, map[i]);
            } else {
                cells[i] = getUnwrappedCell(app, map[i]);
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
        if (row instanceof PartlyMaterializedBlobSupportRow) {
            return ((PartlyMaterializedBlobSupportRow)row).getRawCellUnsafe(i);
        } else if (row instanceof BlobSupportDataRow) {
            return ((BlobSupportDataRow)row).getRawCell(i);
        }
        return row.getCell(i);
    }

}
