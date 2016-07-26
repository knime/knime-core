/*
 * ------------------------------------------------------------------------
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
 */
package org.knime.core.node.tableview;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.knime.core.node.util.CheckUtils;

/**
 * Position information when searching occurrences in the entire table.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
final class FindPosition {

    static final int HEADER = -1;

    private final SearchOptions m_searchOptions;

    private final int m_rowCount;

    private final int m_columnCountInclRowIDCol;

    private int m_searchRowInclColNameRow;

    private int m_searchRowInclColNameRowMark;

    private int m_searchColumnInclRowIDCol;

    private int m_searchColumnInclRowIDColMark;

    /**
     * Create new position object.
     *
     * @param rowCount The total number of rows in the table
     * @param columnCount The total number of columns in the table.
     * @param searchOptions What to search
     */
    FindPosition(final int rowCount, final int columnCount, final SearchOptions searchOptions) {
        CheckUtils.checkArgument(rowCount >= 0, "Row Count < 0: %d", rowCount);
        CheckUtils.checkArgument(columnCount >= 0, "Column Count < 0: %d", columnCount);

        m_searchOptions = CheckUtils.checkArgumentNotNull(searchOptions);

        m_rowCount = rowCount;
        m_searchRowInclColNameRow = 0;
        m_searchRowInclColNameRowMark = m_searchRowInclColNameRow;

        m_columnCountInclRowIDCol = columnCount + 1;
        m_searchColumnInclRowIDCol = 0;
        m_searchColumnInclRowIDColMark = 0;
    }

    /** @return Current location row. */
    int getSearchRow() {
        return m_searchRowInclColNameRow - 1;
    }

    /** @return Current location column (in this class -1). */
    int getSearchColumn() {
        return m_searchColumnInclRowIDCol - 1;
    }

    /**
     * Pushes the search position to its next location.
     *
     * @return true if it advanced to the next position, false if it starts from top.
     */
    boolean next() {
        final int oldSearchRow = m_searchRowInclColNameRow;
        final int oldSearchCol = m_searchColumnInclRowIDCol;
        if (oldSearchRow == 0) {
            if (m_searchOptions.isSearchColumnName() && m_columnCountInclRowIDCol > 1) {
                m_searchColumnInclRowIDCol = nextColumn();
                if (m_searchColumnInclRowIDCol > oldSearchCol) {
                    return true;
                }
            } else {
                assert m_searchColumnInclRowIDCol == 0 : "Expected to have cursor in top left corner";
            }
        } else if (m_searchOptions.isSearchData() && m_columnCountInclRowIDCol > 1) {
            m_searchColumnInclRowIDCol = nextColumn();
            if (m_searchColumnInclRowIDCol > oldSearchCol) {
                return true;
            }
        }
        if (m_searchOptions.isSearchRowID() || (m_searchOptions.isSearchData() && m_columnCountInclRowIDCol > 1)) {
            m_searchRowInclColNameRow = nextRow();
            if (m_searchRowInclColNameRow > oldSearchRow) {
                m_searchColumnInclRowIDCol = m_searchOptions.isSearchRowID() ? 0 : 1;
                return true;
            } else {
                m_searchColumnInclRowIDCol = 0; // reset to top left corner
                return false;
            }
        }
        return false;
    }

    private int nextColumn() {
        int nextColumn = (m_searchColumnInclRowIDCol + 1) % (m_columnCountInclRowIDCol);
        return nextColumn;
    }

    private int nextRow() {
        return (m_searchRowInclColNameRow + 1) % (m_rowCount + 1);
    }

    /** Reset position to row 0. */
    void reset() {
        m_searchRowInclColNameRow = 0;
        m_searchColumnInclRowIDCol = 0;
    }

    /**
     * @return the searchOptions
     */
    SearchOptions getSearchOptions() {
        return m_searchOptions;
    }

    /** Set a mark (memorize last search hit location). */
    void mark() {
        m_searchRowInclColNameRowMark = m_searchRowInclColNameRow;
        m_searchColumnInclRowIDColMark = m_searchColumnInclRowIDCol;
    }

    /** @return true if we reached the mark (again). */
    boolean reachedMark() {
        return m_searchRowInclColNameRowMark == m_searchRowInclColNameRow
                && m_searchColumnInclRowIDColMark == m_searchColumnInclRowIDCol;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("Current Row: ");
        b.append(getSearchRow()).append(", Current Col: ");
        int col = getSearchColumn();
        if (col < 0) {
            b.append("<row ID column>");
        } else {
            b.append(col);
        }
        return b.toString();
    }

    static final class SearchOptions {

        private final boolean m_isSearchRowID;
        private final boolean m_isSearchColumnName;
        private final boolean m_isSearchData;

        /**
         * @param isSearchRowID Whether to search row ID column.
         * @param isSearchColumnName Whether to search column names.
         * @param isSearchData Whether to search the data.
         */
        SearchOptions(final boolean isSearchRowID, final boolean isSearchColumnName, final boolean isSearchData) {
            CheckUtils.checkArgument(isSearchRowID || isSearchColumnName || isSearchData, "No field set");
            m_isSearchRowID = isSearchRowID;
            m_isSearchColumnName = isSearchColumnName;
            m_isSearchData = isSearchData;
        }


        /** @return the isSearchRowID */
        boolean isSearchRowID() {
            return m_isSearchRowID;
        }

        /** @return the isSearchColumnName */
        boolean isSearchColumnName() {
            return m_isSearchColumnName;
        }

        /** @return the isSearchData */
        boolean isSearchData() {
            return m_isSearchData;
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(final Object obj) {
            return EqualsBuilder.reflectionEquals(this, obj, true);
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this, true);
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
        }


    }

}
