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
 *   Nov 11, 2022 (manuelhotz): refactored with builder pattern
 */
package org.knime.core.data.sort;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.UnaryOperator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.sort.RowReadComparator.SortKeyColumns;
import org.knime.core.data.v2.RowRead;

/**
 * The RowComparator is used to compare two DataRows. It implements the Comparator-interface, so we can use the
 * Arrays.sort method to sort an array of DataRows.
 *
 * @since 4.1 made public for the use in the TopK Selector node
 */
public final class RowComparator implements Comparator<DataRow> {//NOSONAR

    private final RowReadComparator m_delegate;

    /**
     * @param indices Array of sort column indices (-1 indicates the RowKey).
     * @param sortAscending Sort order.
     * @param sortMissingsToEnd Missing at bottom.
     * @param spec The spec to the table.
     *
     * @deprecated Use {@link RowComparatorBuilder} instead.
     */
    @Deprecated(since = "4.7.0")
    public RowComparator(final int[] indices, final boolean[] sortAscending, final boolean sortMissingsToEnd,
        final DataTableSpec spec) {
        final var rc = RowComparator.on(spec);
        for (var i = 0; i < indices.length; i++) {
            final int index = indices[i];
            final boolean descending = !sortAscending[i];
            if (isRowKey(index)) {
                rc.thenComparingRowKey(k -> k
                    .withDescendingSortOrder(descending));
            } else {
                rc.thenComparingColumn(index, c -> c
                    .withDescendingSortOrder(descending)
                    .withMissingsLast(sortMissingsToEnd));
            }
        }
        m_delegate = rc.build().m_delegate;
    }

    /**
     * Adapts this comparator so it can be used to compare {@link RowRead}s.
     *
     * @return adapted comparator
     * @since 5.3
     */
    public RowReadComparator toRowReadComparator() {
        return m_delegate;
    }

    private static boolean isRowKey(final int index) {
        return index == -1;
    }

    /**
     * Builder to configure a column comparator.
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    public static final class ColumnComparatorBuilder {

        private final RowReadComparator.ColumnComparatorBuilder m_delegateBuilder;

        private ColumnComparatorBuilder(final int columnIndex,
            final RowReadComparator.ColumnComparatorBuilder delegateBuilder) {
            if (columnIndex < 0) {
                throw new IllegalArgumentException("Expected non-negative column index.");
            }
            m_delegateBuilder = delegateBuilder;
        }

        /**
         * Enable alphanumeric comparison. Make sure that the data type is string-compatible, otherwise an
         * exception is thrown.
         * @return builder with alphanumeric comparison enabled
         */
        public ColumnComparatorBuilder withAlphanumericComparison() {
            return withAlphanumericComparison(true);
        }

        /**
         * Configure alphanumeric comparison. Make sure that the data type is string-compatible, otherwise an
         * exception is thrown.
         * @param alphanum {@code true} if enabled, {@code false} otherwise
         * @return builder with alphanumeric comparison configured or exception if column datatype is not
         *           string-compatible
         */
        public ColumnComparatorBuilder withAlphanumericComparison(final boolean alphanum) { // NOSONAR
            if (alphanum) {
                final Comparator<StringValue> alphaNumComparator = Comparator.comparing(
                    RowComparator::extractStringFromValue, AlphanumericComparator.NATURAL_ORDER);
                m_delegateBuilder.withValueComparator(alphaNumComparator, StringValue.class);
            } else {
                m_delegateBuilder.withDefaultValueComparator();
            }
            return this;
        }

        /**
         * Enable descending sort order.
         * @return builder with descending sort order
         */
        public ColumnComparatorBuilder withDescendingSortOrder() {
            m_delegateBuilder.withDescendingSortOrder();
            return this;
        }

        /**
         * Configure descending sort order.
         * @param descending {@code true} to enable descending sort order, {@code false} otherwise
         * @return builder with descending sort order configured
         */
        public ColumnComparatorBuilder withDescendingSortOrder(final boolean descending) {
            m_delegateBuilder.withDescendingSortOrder(descending);
            return this;
        }

        /**
         * Enable sorting of missing cells to end of table.
         * @return builder which sorts missing cells to the end of the table
         */
        public ColumnComparatorBuilder withMissingsLast() {
            m_delegateBuilder.withMissingsLast();
            return this;
        }

        /**
         * Configure handling of missing cells.
         * @param missingsLast {@code true} to sort missing cells to the end of the table, {@code false} otherwise
         * @return builder configured to sort missing cells to end of table
         */
        public ColumnComparatorBuilder withMissingsLast(final boolean missingsLast) {
            m_delegateBuilder.withMissingsLast(missingsLast);
            return this;
        }
    }

    /**
     * Builder to configure the row key comparator. There can be at most one, adding a second one will lead to a
     * runtime exception.
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    public static final class RowKeyComparatorBuilder {

        private final RowReadComparator.RowKeyComparatorBuilder m_delegateBuilder;

        private RowKeyComparatorBuilder(final RowReadComparator.RowKeyComparatorBuilder delegateBuilder) {
            m_delegateBuilder = delegateBuilder;
        }

        /**
         * Enable alphanumeric comparison.
         * @return builder with alphanumeric comparison enabled
         */
        public RowKeyComparatorBuilder withAlphanumericComparison() {
            m_delegateBuilder.withComparator(AlphanumericComparator.NATURAL_ORDER);
            return this;
        }

        /**
         * Configure alphanumeric comparison.
         * @param alphanum {@code true} if enabled, {@code false} otherwise
         * @return builder with alphanumeric comparison configured
         */
        public RowKeyComparatorBuilder withAlphanumericComparison(final boolean alphanum) { // NOSONAR
            if (alphanum) {
                return withAlphanumericComparison();
            } else {
                m_delegateBuilder.withDefaultValueComparator();
                return this;
            }
        }

        /**
         * Enable descending sort order.
         * @return builder with descending sort order
         */
        public RowKeyComparatorBuilder withDescendingSortOrder() {
            m_delegateBuilder.withDescendingSortOrder();
            return this;
        }

        /**
         * Configure descending sort order.
         * @param descending {@code true} to enable descending sort order, {@code false} otherwise
         * @return builder with descending sort order configured
         */
        public RowKeyComparatorBuilder withDescendingSortOrder(final boolean descending) {
            m_delegateBuilder.withDescendingSortOrder(descending);
            return this;
        }
    }

    /**
     * Builder to construct a row comparator.
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    public static final class RowComparatorBuilder {

        private final RowReadComparator.Builder m_delegateBuilder;

        private RowComparatorBuilder(final RowReadComparator.Builder builder) {
            m_delegateBuilder = builder;
        }

        /**
         * Adds the given column to the columns compared by the row comparator using the column type's comparator.
         * @param columnIndex column to compare
         * @param comp operator to configure the builder for a column comparator
         * @return the row comparator builder instance
         */
        public RowComparatorBuilder thenComparingColumn(final int columnIndex,
            final UnaryOperator<ColumnComparatorBuilder> comp) {
            m_delegateBuilder.thenComparingColumn(columnIndex, colBuilder -> {
                comp.apply(new ColumnComparatorBuilder(columnIndex, colBuilder));
                return colBuilder;
            });
            return this;
        }

        /**
         * Adds the given column to the columns compared by the row comparator using the given cell comparator.
         * @param columnIndex column to compare
         * @param cellComp comparator to use for comparison of the column values
         * @return the row comparator builder instance
         */
        public RowComparatorBuilder thenComparingColumn(final int columnIndex, final Comparator<DataCell> cellComp) {
            m_delegateBuilder.thenComparingColumn(columnIndex,
                Comparator.comparing(read -> materializeDataCell(read, columnIndex), cellComp));
            return this;
        }

        private static DataCell materializeDataCell(final RowRead read, final int columnIndex) {
            return read.isMissing(columnIndex) ? DataType.getMissingCell()
                : read.getValue(columnIndex).materializeDataCell();
        }

        /**
         * Adds the row key to the builder using the given configuration.
         * @param comp configuration builder for row key comparator
         * @return the row comparator builder instance
         */
        public RowComparatorBuilder thenComparingRowKey(final UnaryOperator<RowKeyComparatorBuilder> comp) {
            m_delegateBuilder.thenComparingRowKey(keyCompBuilder -> {
                comp.apply(new RowKeyComparatorBuilder(keyCompBuilder));
                return keyCompBuilder;
            });
            return this;
        }

        /**
         * Adds the row key to the row comparator using its natural order.
         * @return the row comparator builder instance
         */
        public RowComparatorBuilder thenComparingRowKey() {
            m_delegateBuilder.thenComparingRowKey(Comparator.naturalOrder());
            return this;
        }

        /**
         * Adds the row key to the row comparator using the given string comparator.
         * @param comp string comparator to use
         * @return the row comparator builder instance
         */
        public RowComparatorBuilder thenComparingRowKey(final Comparator<String> comp) {
            m_delegateBuilder.thenComparingRowKey(comp);
            return this;
        }

        /**
         * Builds the configured row comparator.
         * @return row comparator
         */
        public RowComparator build() {
            return new RowComparator(m_delegateBuilder.build());
        }
    }

    /**
     * Adapts the given comparator on {@link RowRead}s to be usable as a {@link RowComparator}.
     *
     * @param comparator comparator on row reads
     * @return adapted comparator accepting {@link DataRow}s
     * @since 5.3
     */
    public static RowComparator fromRowReadComparator(final Comparator<RowRead> comparator) {
        return new RowComparator(RowReadComparator.fromRowReadComparator(comparator));
    }

    /**
     * Creates a comparator builder on the columns of the given data table.
     * @param spec data table
     * @return builder to configure row comparator
     */
    public static RowComparatorBuilder on(final DataTableSpec spec) {
        return new RowComparatorBuilder(RowReadComparator.on(spec));
    }

    private RowComparator(final RowReadComparator delegate) {
        m_delegate = delegate;
    }

    private static String extractStringFromValue(final DataValue dataValue) {
        if (dataValue instanceof StringValue str) {
            return str.getStringValue();
        }
        final var dataCell = dataValue.materializeDataCell();
        throw new IllegalStateException("Unexpected value of type %s in String-compatible column: %s" //
            .formatted(dataCell.getType(), dataCell));
    }

    /**
     * @return record describing whether or not each columns and the row key are used by this comparator,
     * {@link Optional#empty()} if unknown
     * @since 5.3
     */
    public Optional<SortKeyColumns> getSortKeyColumns() {
        return m_delegate.getSortKeyColumns();
    }

    @Override
    public int compare(final DataRow dr1, final DataRow dr2) {
        return m_delegate.compare(RowRead.from(dr1), RowRead.from(dr2));
    }
}
