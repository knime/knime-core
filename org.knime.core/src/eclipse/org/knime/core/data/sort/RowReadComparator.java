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
 *   Mar 20, 2024 (leonard.woerteler): created
 */
package org.knime.core.data.sort;

import java.util.BitSet;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.UnaryOperator;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparatorDelegator;
import org.knime.core.data.MissingValueHandling;
import org.knime.core.data.v2.RowRead;
import org.knime.core.node.util.CheckUtils;

/**
 * Comparator for table rows represented as {@link RowRead}s, used for efficient sorting of large tables.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 * @since 5.3
 */
public final class RowReadComparator implements Comparator<RowRead> { // NOSONAR

    @SuppressWarnings("unchecked")
    private abstract static class ComparatorBuilder<SELF, BASE> { // NOSONAR

        protected final DataColumnSpec m_columnSpec;
        private final Comparator<BASE> m_defaultBaseComparator;

        protected Comparator<BASE> m_baseComparator;

        protected boolean m_descending;


        ComparatorBuilder(final DataColumnSpec columnSpec, final Comparator<BASE> baseComparator) {
            m_columnSpec = columnSpec;
            m_defaultBaseComparator = baseComparator;
            m_baseComparator = baseComparator;
        }

        /**
         * Enable descending sort order.
         * @return builder with descending sort order
         */
        public SELF withDescendingSortOrder() {
            return withDescendingSortOrder(true);
        }

        public SELF withDefaultValueComparator() {
            m_baseComparator = m_defaultBaseComparator;
            return (SELF)this;
        }

        /**
         * Configure descending sort order.
         * @param descending {@code true} to enable descending sort order, {@code false} otherwise
         * @return builder with descending sort order configured
         */
        public SELF withDescendingSortOrder(final boolean descending) {
            m_descending = descending;
            return (SELF)this;
        }

        abstract Builder build(final Builder builder);
    }

    /**
     * Builder to configure a column comparator.
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    public static final class ColumnComparatorBuilder extends ComparatorBuilder<ColumnComparatorBuilder, DataValue> {
        private final int m_index;
        private boolean m_missingsLast;

        ColumnComparatorBuilder(final int columnIndex, final DataColumnSpec columnSpec) {
            super(columnSpec, new DataValueComparatorDelegator<>(columnSpec.getType().getComparator()));
            CheckUtils.checkArgument(columnIndex >= 0, "Expected non-negative column index.");
            m_index = columnIndex;
        }

        /**
         * Sets a custom comparator for the values of this column.
         *
         * @param <T> type of the values in the column
         * @param valueComparator custom comparator for the values in the column
         * @param valueClass class object of the values expected to be in the column
         * @return this builder
         */
        @SuppressWarnings("unchecked")
        public <T extends DataValue> ColumnComparatorBuilder withValueComparator(final Comparator<T> valueComparator,
                final Class<T> valueClass) {
            if (m_columnSpec != null && !m_columnSpec.getType().isCompatible(valueClass)) {
                throw new IllegalStateException(
                    "Value comparator for values of type %s is incompatible with column '%s' of type %s." //
                        .formatted(valueClass.getSimpleName(), m_columnSpec.getName(), m_columnSpec.getType()));
            }
            m_baseComparator = (Comparator<DataValue>)valueComparator;
            return this;
        }

        /**
         * Enable sorting of missing cells to end of table.
         * @return builder which sorts missing cells to the end of the table
         */
        public ColumnComparatorBuilder withMissingsLast() {
            return withMissingsLast(true);
        }

        /**
         * Configure handling of missing cells.
         * @param missingsLast {@code true} to sort missing cells to the end of the table, {@code false} otherwise
         * @return builder configured to sort missing cells to end of table
         */
        public ColumnComparatorBuilder withMissingsLast(final boolean missingsLast) {
            m_missingsLast = missingsLast;
            return this;
        }

        @Override
        Builder build(final Builder builder) {
            Comparator<DataValue> valueComp = m_baseComparator;
            // the condition is such that missing cells never get sorted to the top of a table if sorted in DESC order
            Comparator<RowRead> columnComp = MissingValueHandling.compareWithMissing(m_index, valueComp,
                m_missingsLast && !m_descending);
            if (m_descending) {
                columnComp = columnComp.reversed();
            }
            return builder.thenComparingColumn(m_index, columnComp);
        }
    }

    /**
     * Builder to configure the row key comparator. There can be at most one, adding a second one will lead to a
     * runtime exception.
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    public static final class RowKeyComparatorBuilder extends ComparatorBuilder<RowKeyComparatorBuilder, String> {

        private RowKeyComparatorBuilder() {
            super(null, Comparator.naturalOrder());
        }

        /**
         * Configures the way in which row keys are being compared.
         *
         * @param valueComparator comparator on the string values of the row keys
         * @return this builder
         */
        public RowKeyComparatorBuilder withComparator(final Comparator<String> valueComparator) {
            m_baseComparator = valueComparator;
            return this;
        }

        @Override
        Builder build(final Builder builder) {
            Comparator<String> comp = m_baseComparator;
            if (m_descending) {
                comp = comp.reversed();
            }
            return builder.thenComparingRowKey(comp);
        }
    }

    /**
     * Builder to construct a row comparator.
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    public static final class Builder {

        private final LinkedHashMap<OptionalInt, Comparator<RowRead>> m_columnComparators = new LinkedHashMap<>();

        private final DataTableSpec m_spec;

        private Builder(final DataTableSpec spec) {
            m_spec = spec;
        }

        /**
         * Adds the given column to the columns compared by the row comparator using the column type's comparator.
         * @param columnIndex column to compare
         * @param comp operator to configure the builder for a column comparator
         * @return the row comparator builder instance
         */
        public Builder thenComparingColumn(final int columnIndex,
            final UnaryOperator<ColumnComparatorBuilder> comp) {
            return comp.apply(new ColumnComparatorBuilder(columnIndex, m_spec.getColumnSpec(columnIndex))).build(this);
        }

        /**
         * Adds the given column to the columns compared by the row comparator using the given cell comparator.
         * @param columnIndex column to compare
         * @param cellComp comparator to use for comparison of the column values
         * @return the row comparator builder instance
         */
        Builder thenComparingColumn(final int columnIndex, final Comparator<RowRead> cellComp) {
            final var colIdx = OptionalInt.of(columnIndex);
            if (m_columnComparators.containsKey(colIdx)) {
                throw new IllegalArgumentException(String.format("Row comparator already contains column #%d",
                    columnIndex));
            }
            m_columnComparators.put(colIdx, cellComp);
            return this;
        }

        /**
         * Adds the row key to the builder using the given configuration.
         * @param comp configuration builder for row key comparator
         * @return the row comparator builder instance
         */
        public Builder thenComparingRowKey(final UnaryOperator<RowKeyComparatorBuilder> comp) {
            return comp.apply(new RowKeyComparatorBuilder()).build(this);
        }

        /**
         * Adds the row key to the row comparator using the given string comparator.
         * @param comp string comparator to use
         * @return the row comparator builder instance
         */
        public Builder thenComparingRowKey(final Comparator<String> comp) {
            final var rowKeyIdx = OptionalInt.empty();
            if (m_columnComparators.containsKey(rowKeyIdx)) {
                throw new IllegalArgumentException("Row comparator already contains row key");
            }
            m_columnComparators.put(rowKeyIdx,
                (r1, r2) ->  comp.compare(r1.getRowKey().getString(), r2.getRowKey().getString()));
            return this;
        }

        /**
         * Builds the configured row comparator.
         * @return row comparator
         */
        public RowReadComparator build() {
            var comparesRowKey = false;
            final var columnIndexes = new BitSet();
            for (final var optCol : m_columnComparators.keySet()) {
                if (optCol.isPresent()) {
                    columnIndexes.set(optCol.getAsInt());
                } else {
                    comparesRowKey = true;
                }
            }
            final var sortKey = new SortKeyColumns(columnIndexes, comparesRowKey);
            @SuppressWarnings("unchecked")
            final Comparator<RowRead>[] comparators =
                    (Comparator<RowRead>[])m_columnComparators.values().toArray(Comparator<?>[]::new);
            return new RowReadComparator(sortKey, comparators);
        }
    }

    /**
     * Adapts the given comparator on {@link DataRow}s to be usable as a {@link RowReadComparator}.
     * This may require materializing all rows of the table into in-memory data rows many times, which is inefficient.
     *
     * @param comparator comparator on data rows
     * @return adapted comparator accepting {@link RowRead}s
     */
    public static RowReadComparator fromRowComparator(final Comparator<DataRow> comparator) {
        return comparator instanceof RowComparator rowComparator ? rowComparator.toRowReadComparator()
            : new RowReadComparator(null, Comparator.comparing(RowRead::materializeDataRow, comparator));
    }

    /**
     * Adapts the given comparator on {@link RowRead}s to be usable as a {@link RowReadComparator}.
     *
     * @param comparator comparator on row reads
     * @return adapted comparator
     */
    public static RowReadComparator fromRowReadComparator(final Comparator<RowRead> comparator) {
        return comparator instanceof RowReadComparator rowReadComparator ? rowReadComparator
            : new RowReadComparator(null, comparator);
    }

    /**
     * Creates a comparator builder on the columns of the given data table.
     * @param spec data table
     * @return builder to configure row comparator
     */
    public static Builder on(final DataTableSpec spec) {
        return new Builder(spec);
    }

    private final SortKeyColumns m_sortKeyCols;
    private final Comparator<RowRead>[] m_comparators;

    @SafeVarargs
    private RowReadComparator(final SortKeyColumns sortKeyCols, final Comparator<RowRead>... comparators) {
        m_comparators = comparators.clone();
        m_sortKeyCols = sortKeyCols;
    }

    /**
     * Description of the components of each row which are potentially being accessed by a comparator.
     *
     * @param columnIndexes indexes of columns that are accessed by the comparator
     * @param comparesRowKey whether the comparator accesses the row key
     */
    public record SortKeyColumns(BitSet columnIndexes, boolean comparesRowKey) {

        /**
         * @param numColumns number of columns the rows compared by the comparator have
         * @return {@link SortKeyColumns} instance claiming that all columns and the row key are all
         * (potentially) being used
         */
        public static SortKeyColumns all(final int numColumns) {
            final var allCols = new BitSet();
            allCols.set(0, numColumns);
            return new SortKeyColumns(allCols, true);
        }
    }

    /**
     * @return record describing whether or not each columns and the row key are used by this comparator,
     * {@link Optional#empty()} if unknown
     * @since 5.3
     */
    public Optional<SortKeyColumns> getSortKeyColumns() {
        return Optional.ofNullable(m_sortKeyCols);
    }

    @Override
    public int compare(final RowRead dr1, final RowRead dr2) {
        if (dr1 == dr2) {
            return 0;
        }
        if (dr1 == null) {
            return 1;
        }
        if (dr2 == null) {
            return -1;
        }
        assert dr1.getNumColumns() == dr2.getNumColumns() : String.format( // NOSONAR run time performance
            "The rows %s and %s don't contain the same number of cells.", dr1.getRowKey(), dr2.getRowKey()); //

        for (var comparator : m_comparators) {
            var comparison = comparator.compare(dr1, dr2);
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }
}
