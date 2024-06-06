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
 */
package org.knime.core.data.container.filter;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.UnmaterializedCell.UnmaterializedDataCellException;
import org.knime.core.data.container.storage.AbstractTableStoreReader;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.table.row.Selection;
import org.knime.core.table.row.Selection.RowRangeSelection;

/**
 * A class for specifying filters over {@link BufferedDataTable BufferedDataTables}. The filter may restrict which rows
 * and columns to materialize by an {@link AbstractTableStoreReader}. The filter will leave the {@link DataTableSpec}
 * and order of rows of the table unchanged, but might reduce the amount of {@link DataRow DataRows} retrieved by a
 * filtered iterator obtained via {@link BufferedDataTable#filter(TableFilter)}.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @since 4.0
 */
public final class TableFilter {

    // Additional filter options are planned for the future, see AP-11805
    private TableFilter(final Optional<Set<Integer>> columnIndices, final Optional<Long> fromRowIndex,
        final Optional<Long> toRowIndex) {
        m_columnIndices = columnIndices;
        m_fromRowIndex = fromRowIndex;
        m_toRowIndex = toRowIndex;
    }

    private final Optional<Set<Integer>> m_columnIndices;

    private final Optional<Long> m_fromRowIndex;

    private final Optional<Long> m_toRowIndex;

    /**
     * A method that can be used to obtain the indices of columns that should be materialized. The returned
     * {@link Optional} will be empty if all indices are to be materialized.
     *
     * @return an optional set of indices for the columns which are to be materialized
     */
    public Optional<Set<Integer>> getMaterializeColumnIndices() {
        return m_columnIndices;
    }

    /**
     * A method that can be used to obtain the lowest index of rows to keep. The returned {@link Optional} will be empty
     * if no lower bound for row indices is set.
     *
     * @return an optional minimum index of to-be-kept rows
     */
    public Optional<Long> getFromRowIndex() {
        return m_fromRowIndex;
    }

    /**
     * A method that can be used to obtain the highest index of rows to keep. The returned {@link Optional} will be
     * empty if no upper bound for row indices is set.
     *
     * @return an optional maximum index of to-be-kept rows
     */
    public Optional<Long> getToRowIndex() {
        return m_toRowIndex;
    }

    /**
     * Validates this {@link TableFilter} against a {@link DataTableSpec}.
     *
     * @param spec the spec to validate against
     * @param size the size of the table that is to be filtered
     * @throws IndexOutOfBoundsException when any index is out of bounds
     * @throws IllegalArgumentException when any argument is invalid
     */
    public void validate(final DataTableSpec spec, final long size) {
        if (/*(m_fromRowIndex.isPresent() && m_fromRowIndex.get() >= size)
                || */(m_toRowIndex.isPresent() && m_toRowIndex.get() >= size)) {
            throw new IndexOutOfBoundsException("Row index must be smaller than " + size);
        }

        if (m_columnIndices.isPresent()) {
            spec.verifyIndices(m_columnIndices.get().stream().mapToInt(i -> i).toArray());
        }
    }

    /**
     * Static factory method for creating a {@link TableFilter} that only materializes columns with a certain index.
     * {@link DataCell DataCells} accessed in unmaterialized columns might lead to an
     * {@link UnmaterializedDataCellException} being thrown.
     *
     * @param indices the indices of columns to materialize
     * @return a new table filter
     */
    public static TableFilter materializeCols(final int... indices) {
        return (new Builder()).withMaterializeColumnIndices(indices).build();
    }

    /**
     * Static factory method for creating a {@link TableFilter} that only materializes columns with a certain name.
     * {@link DataCell DataCells} accessed in unmaterialized columns might lead to an
     * {@link UnmaterializedDataCellException} being thrown.
     *
     * @param spec the data table spec of the table to filter
     * @param columnNames the names of columns to materialize
     * @return a new table filter
     */
    public static TableFilter materializeCols(final DataTableSpec spec, final String... columnNames) {
        return (new Builder()).withMaterializeColumnIndices(spec.columnsToIndices(columnNames)).build();
    }

    /**
     * Static factory method for creating a {@link TableFilter} that retains only rows from a certain index onward. The
     * index of the first {@link DataRow} in a table is 0.
     *
     * @param index the row index from which to filter
     * @return a new table filter
     */
    public static TableFilter filterRowsFromIndex(final long index) {
        return (new Builder()).withFromRowIndex(index).build();
    }

    /**
     * Static factory method for creating a {@link TableFilter} that retains only rows up to a certain index. The index
     * of the first {@link DataRow} in a table is 0.
     *
     * @param index the row index to which to filter
     * @return a new table filter
     */
    public static TableFilter filterRowsToIndex(final long index) {
        return (new Builder()).withToRowIndex(index).build();
    }

    /**
     * Static factory method for creating a {@link TableFilter} that retains only rows within a certain index range. The
     * index of the first {@link DataRow} in a table is 0.
     *
     * @param fromIndex the row index from which to filter
     * @param toIndex the row index to which to filter
     * @return a new table filter
     */
    public static TableFilter filterRangeOfRows(final long fromIndex, final long toIndex) {
        return (new Builder()).withFromRowIndex(fromIndex).withToRowIndex(toIndex).build();
    }

    /**
     * Create a new {@link TableFilter} corresponding to the given {@link Selection}.
     *
     * @param selection the selected columns and row range
     * @param table The table in order to extract/bound table dimensions. Unlike this {@code TableFilter} a
     *            {@link Selection} allows row limits larger than the number of rows in the table and this argument is
     *            used to constraint.
     * @return a new {@link TableFilter}
     * @noreference This method is not intended to be referenced by clients.
     */
    public static TableFilter fromSelection(final Selection selection, final BufferedDataTable table) {
        final Builder builder = new TableFilter.Builder();
        if (!selection.columns().allSelected()) {
            builder.withMaterializeColumnIndices(IntStream.of(selection.columns().getSelected())//
                .filter(i -> i > 0)// the row key column is always included and therefore not subject to TableFilter
                .map(i -> i - 1)// adjust for the row key column
                .toArray());
        }
        final RowRangeSelection rowSel = selection.rows();
        if (!rowSel.allSelected()) { // implies fromIndex >= 0
            final var toIndexExcl = Math.min(rowSel.toIndex(), table.size());
            builder.withFromRowIndex(rowSel.fromIndex()).withToRowIndex(toIndexExcl - 1);
        }
        return builder.build();
    }

    /**
     * Implementation of the builder design pattern for the {@link TableFilter} class.
     */
    public final static class Builder {

        private Optional<Set<Integer>> m_columnIndices;

        private Optional<Long> m_fromRowIndex;

        private Optional<Long> m_toRowIndex;

        /**
         * Constructs a new builder.
         */
        public Builder() {
            m_columnIndices = Optional.empty();
            m_fromRowIndex = Optional.empty();
            m_toRowIndex = Optional.empty();
        }

        /**
         * Constructs a new builder with parameters copied over from an existing {@link TableFilter}.
         *
         * @param filter the filter from which to copy parameters
         */
        public Builder(final TableFilter filter) {
            m_columnIndices = filter.getMaterializeColumnIndices();
            m_fromRowIndex = filter.getFromRowIndex();
            m_toRowIndex = filter.getToRowIndex();
        }

        /**
         * Configure the builder to provide {@link TableFilter TableFilters} that only materialize columns with a
         * certain index. {@link DataCell DataCells} accessed in unmaterialized columns might lead to an
         * {@link UnmaterializedDataCellException} being thrown.
         *
         * @param indices the indices of columns to materialize
         * @return the same builder with updated parameters
         * @throws IllegalArgumentException when there are duplicates among given indices
         */
        public Builder withMaterializeColumnIndices(final int... indices) {
            CheckUtils.checkArgumentNotNull(indices);
            final Set<Integer> indicesSet = new HashSet<>();
            for (int index : indices) {
                if (!indicesSet.add(index)) {
                    throw new IllegalArgumentException("Duplicate index: " + index);
                }
            }
            m_columnIndices = Optional.of(indicesSet);
            return this;
        }

        /**
         * Configure the builder to provide {@link TableFilter TableFilters} that retain only rows from a certain index
         * onward. The index of the first {@link DataRow} in a table is 0.
         *
         * @param index the row index from which to filter
         * @return the same builder with updated parameters
         * @throws IndexOutOfBoundsException when any index is out of bounds
         */
        public Builder withFromRowIndex(final long index) {
            if (index < 0) {
                throw new IndexOutOfBoundsException("Row index must be at least 0.");
            }
            if (m_toRowIndex.isPresent()) {
                CheckUtils.checkArgument(index <= m_toRowIndex.get(),
                    "Row index to filter from cannot be higher than row index to filter to.");
            }
            m_fromRowIndex = Optional.of(index);
            return this;
        }

        /**
         * Configure the builder to provide {@link TableFilter TableFilters} that retain only rows up to a certain
         * index. The index of the first {@link DataRow} in a table is 0.
         *
         * @param index the row index to which to filter
         * @return the same builder with updated parameters
         * @throws IndexOutOfBoundsException when any index is out of bounds
         */
        public Builder withToRowIndex(final long index) {
            if (index < 0) {
                throw new IndexOutOfBoundsException("Row index must be at least 0.");
            }
            if (m_fromRowIndex.isPresent()) {
                CheckUtils.checkArgument(m_fromRowIndex.get() <= index,
                    "Row index to filter from cannot be higher than row index to filter to.");
            }
            m_toRowIndex = Optional.of(index);
            return this;
        }

        /**
         * Builds a new table filter with the paramaters configured in this builder.
         *
         * @return a new table filter
         */
        public TableFilter build() {
            return new TableFilter(m_columnIndices, m_fromRowIndex, m_toRowIndex);
        }

    }

}
