/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Sep 5, 2011 (wiswedel): created
 */
package org.knime.core.node.tableview;

/** Represents sort order in a table (multiple columns).
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class TableSortOrder {

    /** Sorting associated with individual column. */
    enum  TableSortKey {
        /** No sorting. */
        NONE,
        /** Column is primary, sorting is descending. */
        PRIMARY_DESCENDING,
        /** Column is primary, sorting is ascending. */
        PRIMARY_ASCENDING,
        /** Column is secondary, sorting is descending. */
        SECONDARY_DESCENDING,
        /** Column is secondary, sorting is ascending. */
        SECONDARY_ASCENDING;
    }

    /** Indicates that no secondary sorting is used. */
    private static final int COLIDX_NONE = -2;
    /** Indicates that the soring is on the rowID column. */
    private static final int COLIDX_ROWKEY = -1;

    private final int m_colIndexPrimary;
    private final int m_colIndexSecondary;
    private final boolean m_primarySortIsAscending;
    private final boolean m_secondarySortIsAscending;

    /** Inits new sorting on a primary column.
     * @param colIndexPrimary The column. */
    TableSortOrder(final int colIndexPrimary) {
        // create object only if there is some sorting
        if (colIndexPrimary < COLIDX_ROWKEY) {
            throw new IllegalArgumentException(
                    "Invalid column index for sorting: " + colIndexPrimary);
        }
        m_colIndexPrimary = colIndexPrimary;
        m_primarySortIsAscending = false;
        m_colIndexSecondary = COLIDX_NONE;
        m_secondarySortIsAscending = false;
    }

    /**
     * @param colIndexPrimary
     * @param colIndexSecondary
     * @param primarySortIsAscending
     * @param secondarySortIsAscending */
    private TableSortOrder(final int colIndexPrimary,
            final boolean primarySortIsAscending,
            final int colIndexSecondary,
            final boolean secondarySortIsAscending) {
        m_colIndexPrimary = colIndexPrimary;
        m_colIndexSecondary = colIndexSecondary;
        m_primarySortIsAscending = primarySortIsAscending;
        m_secondarySortIsAscending = secondarySortIsAscending;
    }

    /** For a given query column return the sort key.
     * @param colIdx The column in the model.
     * @return The sort key. */
    TableSortKey getSortKeyForColumn(final int colIdx) {
        if (colIdx == m_colIndexPrimary) {
            return m_primarySortIsAscending ? TableSortKey.PRIMARY_ASCENDING
                    : TableSortKey.PRIMARY_DESCENDING;
        } else if (colIdx == m_colIndexSecondary) {
            return m_secondarySortIsAscending ? TableSortKey.SECONDARY_ASCENDING
                    : TableSortKey.SECONDARY_DESCENDING;
        } else {
            return TableSortKey.NONE;
        }
    }

    /** Get indices of sort columns in array, passed to sorter.
     * @return Sort indices. */
    int[] getSortColumnIndices() {
        if (m_colIndexSecondary == COLIDX_NONE) {
            return new int[] {m_colIndexPrimary};
        } else {
            return new int[] {m_colIndexPrimary, m_colIndexSecondary};
        }
    }

    /** @return Sort order, passed to sorter. */
    boolean[] getSortColumnOrder() {
        if (m_colIndexSecondary == COLIDX_NONE) {
            return new boolean[] {m_primarySortIsAscending};
        } else {
            return new boolean[] {
                    m_primarySortIsAscending, m_secondarySortIsAscending};
        }
    }

    /** Calculate next sort order (what happens if the user clicks the arg
     * column). A return value of null indicates a reset (e.g. three clicks
     * on a column).
     * @param colIdx column clicked.
     * @return The next sorting.  */
    TableSortOrder nextSortOrder(final int colIdx) {
        if (colIdx < COLIDX_ROWKEY) {
            throw new IllegalArgumentException(
                    "Invalid sort column index: " + colIdx);
        }
        if (colIdx == m_colIndexPrimary) {
            if (!m_primarySortIsAscending) {
                // flip sorting on primary column
                return new TableSortOrder(m_colIndexPrimary, true,
                        m_colIndexSecondary, m_secondarySortIsAscending);
            } else {
                // restore natural order on (old) primary; make secondary
                // to primary unless there is no secondary
                if (m_colIndexSecondary != COLIDX_NONE) {
                    int colIndexPrimary = m_colIndexSecondary;
                    boolean primarySortIsAscending = m_secondarySortIsAscending;
                    int colIndexSecondary = COLIDX_NONE;
                    boolean secondarySortIsAscending = false;
                    return new TableSortOrder(
                            colIndexPrimary, primarySortIsAscending,
                            colIndexSecondary, secondarySortIsAscending);
                } else {
                    return null; // indicate natural ordering
                }
            }
        } else if (colIdx == m_colIndexSecondary) {
            // simply make secondary to primary, leave sort order as-is
            return new TableSortOrder(
                    m_colIndexSecondary, m_secondarySortIsAscending,
                    m_colIndexPrimary, m_primarySortIsAscending);
        } else {
            // make new column primary, discard (old) secondary
            return new TableSortOrder(colIdx, false,
                    m_colIndexPrimary, m_primarySortIsAscending);
        }
    }
}
