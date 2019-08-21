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
 *   Aug 16, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.data.sort;

import java.util.Comparator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValueComparator;

/**
 * The RowComparator is used to compare two DataRows. It implements the Comparator-interface, so we can use the
 * Arrays.sort method to sort an array of DataRows.
 *
 * @since 4.1 made public for the use in the Element Selector node
 */
public final class RowComparator implements Comparator<DataRow> {

    /**
     * The included column indices.
     */
    private final int[] m_indices;

    /**
     * The comparators for the different columns (value in array is null if sorted according to row key). Fetched at
     * constructor time to reduce number of DataType accesses during compare() call
     */
    private final DataValueComparator[] m_colComparators;

    /**
     * Array containing information about the sort order for each column. true: ascending false: descending
     */
    private final boolean[] m_sortAscending;

    /**
     * Missing vals always at end (if not then they just smaller than any non-missing).
     */
    private final boolean m_sortMissingsToEnd;

    /**
     * @param indices Array of sort column indices (-1 indicates the RowKey).
     * @param sortAscending Sort order.
     * @param sortMissingsToEnd Missing at bottom.
     * @param spec The spec to the table.
     */
    public RowComparator(final int[] indices, final boolean[] sortAscending, final boolean sortMissingsToEnd,
        final DataTableSpec spec) {
        m_indices = indices;
        m_colComparators = new DataValueComparator[indices.length];
        for (int i = 0; i < m_indices.length; i++) {
            // only if the cell is in the includeList
            // -1 is RowKey!
            if (m_indices[i] == -1) {
                m_colComparators[i] = null;
            } else {
                m_colComparators[i] = spec.getColumnSpec(m_indices[i]).getType().getComparator();
            }
        }
        m_sortAscending = sortAscending;
        m_sortMissingsToEnd = sortMissingsToEnd;
    }

    /** {@inheritDoc} */
    @Override
    public int compare(final DataRow dr1, final DataRow dr2) {

        if (dr1 == dr2) {
            return 0;
        }
        if (dr1 == null) {
            return 1;
        }
        if (dr2 == null) {
            return -1;
        }

        assert dr1.getNumCells() == dr2.getNumCells() : String.format(
            "The rows %s and %s don't contain the same number of cells.", dr1.getKey(), dr2.getKey());

        for (int i = 0; i < m_indices.length; i++) {

            // only if the cell is in the includeList
            // -1 is RowKey!
            final int cellComparison = compareIndex(dr1, dr2, i);
            if (cellComparison != 0) {
                return (m_sortAscending[i] ? cellComparison : -cellComparison);
            }
        }
        return 0; // all cells in the DataRow have the same value
    }

    private int compareIndex(final DataRow dr1, final DataRow dr2, final int i) {
        if (isRowKey(m_indices[i])) {
            return compareRowKeys(dr1, dr2);
        } else {
            return compareCells(dr1, dr2, i);
        }
    }

    private int compareCells(final DataRow dr1, final DataRow dr2, final int i) {
        int cellComparison;
        final DataCell c1 = dr1.getCell(m_indices[i]);
        final DataCell c2 = dr2.getCell(m_indices[i]);
        final boolean c1Missing = c1.isMissing();
        final boolean c2Missing = c2.isMissing();
        if (m_sortMissingsToEnd && (c1Missing || c2Missing)) {
            return sortMissingsToEnd(i, c1Missing, c2Missing);
        } else {
            final DataValueComparator comp = m_colComparators[i];
            cellComparison = comp.compare(c1, c2);
        }
        return cellComparison;
    }

    private int sortMissingsToEnd(final int i, final boolean c1Missing, final boolean c2Missing) {
        int cellComparison;
        if (c1Missing && c2Missing) {
            cellComparison = 0;
        } else if (c1Missing) {
            cellComparison = m_sortAscending[i] ? +1 : -1;
        } else { // c2.isMissing()
            cellComparison = m_sortAscending[i] ? -1 : +1;
        }
        return cellComparison;
    }

    private static int compareRowKeys(final DataRow dr1, final DataRow dr2) {
        int cellComparison;
        final String k1 = dr1.getKey().getString();
        final String k2 = dr2.getKey().getString();
        cellComparison = k1.compareTo(k2);
        return cellComparison;
    }

    private static boolean isRowKey(final int index) {
        return index == -1;
    }
}