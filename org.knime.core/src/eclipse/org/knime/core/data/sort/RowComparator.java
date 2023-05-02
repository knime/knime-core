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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.UnaryOperator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingDataCellComparator;
import org.knime.core.data.StringValue;
import org.knime.core.node.util.CheckUtils;

/**
 * The RowComparator is used to compare two DataRows. It implements the Comparator-interface, so we can use the
 * Arrays.sort method to sort an array of DataRows.
 *
 * @since 4.1 made public for the use in the TopK Selector node
 */
public final class RowComparator implements Comparator<DataRow> {//NOSONAR

    private final List<Comparator<DataRow>> m_comparators;

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
                // legacy behavior: if "sortMissingsToEnd" is false, the comparator would sort missing cells based on
                //                  the column sort order (ASC/DESC) and _not_ always to the start. this means that the
                //                  combination (DESC, missings at the start) is not possible to achieve with this.
                // In order to not break this legacy behavior, we make the actual argument to the comparator builder
                // dependent on the sort order again.
                rc.thenComparingColumn(index, c -> c
                    .withDescendingSortOrder(descending)
                    .withMissingsLast(sortMissingsToEnd || descending));
            }
        }
        m_comparators = rc.build().m_comparators;
    }

    private static boolean isRowKey(final int index) {
        return index == -1;
    }

    /**
     * <p>Builder to configure a column comparator. The comparator can be configured to always sort missing values
     * to the end of the table, regardless of the configured sort order for non-missing values.
     * <p>
     * The default is to always sort them to the start.
     * <p>
     * If you want to configure the comparator such that it always compares missing values as smallest/largest
     * relative to the sort order and not using the absolute position (missingsLast),
     * you can adapt the {@code missingsLast} condition based on your non-missing value sort order.
     * <p>
     * <i>For example:</i>
     * <pre><code>
     *     final boolean missingAsLargest = ...;
     *     final boolean descending = ...;
     *     RowComparator.on(spec).thenComparingColumn(columnIndex,
     *         c.withDescendingSortOrder(descending)
     *          .withMissingsLast(descending ^ missingsAsLargest));
     * </code></pre>
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    public static final class ColumnComparatorBuilder {

        private final int m_index;
        private final DataType m_type;

        private boolean m_alphanum;
        private boolean m_descending;
        private boolean m_missingsLast;

        private ColumnComparatorBuilder(final int columnIndex, final DataType type) {
            if (columnIndex < 0) {
                throw new IllegalArgumentException("Expected non-negative column index.");
            }
            m_type = type;
            m_index = columnIndex;
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
        public ColumnComparatorBuilder withAlphanumericComparison(final boolean alphanum) {
            if (alphanum) {
                CheckUtils.checkState(m_type.isCompatible(StringValue.class), "Only string-compatible columns can"
                    + " be sorted alphanumerically. Column at %d of type %s is not string-compatible.", m_index,
                    m_type);
            }
            m_alphanum = alphanum;
            return this;
        }

        /**
         * Enable descending sort order for non-missing values.
         * @return builder with descending sort order
         */
        public ColumnComparatorBuilder withDescendingSortOrder() {
            return withDescendingSortOrder(true);
        }

        /**
         * Configure descending sort order for non-missing values.
         * @param descending {@code true} to enable descending sort order, {@code false} otherwise
         * @return builder with descending sort order configured
         */
        public ColumnComparatorBuilder withDescendingSortOrder(final boolean descending) {
            m_descending = descending;
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
         * @param missingsLast {@code true} to sort missing cells to the end of the table, {@code false} to sort them to
         *          the start of the table
         * @return builder configured to sort missing cells to end of table
         */
        public ColumnComparatorBuilder withMissingsLast(final boolean missingsLast) {
            m_missingsLast = missingsLast;
            return this;
        }

        private RowComparatorBuilder build(final RowComparatorBuilder rowComparatorBuilder) {
            // build comparator from "inside"
            Comparator<DataCell> cellComp;
            if (this.m_alphanum) {
                // compares column on string representation in alphanumerical order
                cellComp = Comparator.comparing(
                    dc -> CheckUtils.checkCast(dc, StringValue.class, IllegalStateException::new,
                        "Comparing non-string compatible column").getStringValue(),
                    new AlphanumericComparator(Comparator.naturalOrder()));
            } else {
                cellComp = m_type.getComparator();
            }
            if (m_descending) {
                cellComp = cellComp.reversed();
            }
            return rowComparatorBuilder.thenComparingColumn(m_index,
                new MissingDataCellComparator<>(cellComp, m_missingsLast));
        }
    }

    /**
     * Builder to configure the row key comparator. There can be at most one, adding a second one will lead to a
     * runtime exception.
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    public static final class RowKeyComparatorBuilder {

        private boolean m_alphanum;
        private boolean m_descending;

        private RowKeyComparatorBuilder() {
        }

        /**
         * Enable alphanumeric comparison.
         * @return builder with alphanumeric comparison enabled
         */
        public RowKeyComparatorBuilder withAlphanumericComparison() {
            return withAlphanumericComparison(true);
        }

        /**
         * Configure alphanumeric comparison.
         * @param alphanum {@code true} if enabled, {@code false} otherwise
         * @return builder with alphanumeric comparison configured
         */
        public RowKeyComparatorBuilder withAlphanumericComparison(final boolean alphanum) {
            m_alphanum = alphanum;
            return this;
        }

        /**
         * Enable descending sort order.
         * @return builder with descending sort order
         */
        public RowKeyComparatorBuilder withDescendingSortOrder() {
            return withDescendingSortOrder(true);
        }

        /**
         * Configure descending sort order.
         * @param descending {@code true} to enable descending sort order, {@code false} otherwise
         * @return builder with descending sort order configured
         */
        public RowKeyComparatorBuilder withDescendingSortOrder(final boolean descending) {
            m_descending = descending;
            return this;
        }

        private RowComparatorBuilder build(final RowComparatorBuilder rowComparatorBuilder) {
            Comparator<String> comp = Comparator.naturalOrder();
            if (m_alphanum) {
                // since row keys are compared as strings, they are always string-compatible
                comp = new AlphanumericComparator(comp);
            }
            if (m_descending) {
                comp = comp.reversed();
            }
            return rowComparatorBuilder.thenComparingRowKey(comp);
        }
    }

    private static final class ColumnComparator implements Comparator<DataRow> {//NOSONAR

        private final int m_colIdx;

        private final Comparator<DataCell> m_cellComparator;

        ColumnComparator(final int colIdx, final Comparator<DataCell> cellComparator) {
            m_colIdx = colIdx;
            m_cellComparator = cellComparator;
        }

        @Override
        public int compare(final DataRow o1, final DataRow o2) {
            return m_cellComparator.compare(o1.getCell(m_colIdx), o2.getCell(m_colIdx));
        }

    }

    private static final class RowKeyComparator implements Comparator<DataRow> {//NOSONAR

        private final Comparator<String> m_rowKeyComparator;

        RowKeyComparator(final Comparator<String> rowKeyComparator) {
            m_rowKeyComparator = rowKeyComparator;
        }

        @Override
        public int compare(final DataRow o1, final DataRow o2) {
            return m_rowKeyComparator.compare(o1.getKey().getString(), o2.getKey().getString());
        }

    }

    /**
     * Builder to construct a row comparator.
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    public static final class RowComparatorBuilder {

        private final LinkedHashMap<OptionalInt, Comparator<DataRow>> m_columnComparators = new LinkedHashMap<>();

        private final DataTableSpec m_spec;

        private RowComparatorBuilder(final DataTableSpec spec) {
            m_spec = spec;
        }

        /**
         * Adds the given column to the columns compared by the row comparator using the column type's comparator.
         * @param columnIndex column to compare
         * @param comp operator to configure the builder for a column comparator
         * @return the row comparator builder instance
         */
        public RowComparatorBuilder thenComparingColumn(final int columnIndex,
            final UnaryOperator<ColumnComparatorBuilder> comp) {
            return comp.apply(new ColumnComparatorBuilder(columnIndex, m_spec.getColumnSpec(columnIndex).getType()))
                    .build(this);
        }

        /**
         * Adds the given column to the columns compared by the row comparator using the given cell comparator.
         * @param columnIndex column to compare
         * @param cellComp comparator to use for comparison of the column values
         * @return the row comparator builder instance
         */
        public RowComparatorBuilder thenComparingColumn(final int columnIndex, final Comparator<DataCell> cellComp) {
            final var colIdx = OptionalInt.of(columnIndex);
            if (m_columnComparators.containsKey(colIdx)) {
                throw new IllegalArgumentException(String.format("Row comparator already contains column #%d",
                    columnIndex));
            }
            m_columnComparators.put(colIdx, new ColumnComparator(columnIndex, cellComp));
            return this;
        }

        /**
         * Adds the row key to the builder using the given configuration.
         * @param comp configuration builder for row key comparator
         * @return the row comparator builder instance
         */
        public RowComparatorBuilder thenComparingRowKey(final UnaryOperator<RowKeyComparatorBuilder> comp) {
            return comp.apply(new RowKeyComparatorBuilder()).build(this);
        }

        /**
         * Adds the row key to the row comparator using its natural order.
         * @return the row comparator builder instance
         */
        public RowComparatorBuilder thenComparingRowKey() {
            return new RowKeyComparatorBuilder().build(this);
        }

        /**
         * Adds the row key to the row comparator using the given string comparator.
         * @param comp string comparator to use
         * @return the row comparator builder instance
         */
        public RowComparatorBuilder thenComparingRowKey(final Comparator<String> comp) {
            final var rowKeyIdx = OptionalInt.empty();
            if (m_columnComparators.containsKey(rowKeyIdx)) {
                throw new IllegalArgumentException("Row comparator already contains row key");
            }
            m_columnComparators.put(rowKeyIdx, new RowKeyComparator(comp));
            return this;
        }

        /**
         * Builds the configured row comparator.
         * @return row comparator
         */
        public RowComparator build() {
            return new RowComparator(new ArrayList<>(m_columnComparators.values()));
        }
    }

    /**
     * Creates a comparator builder on the columns of the given data table.
     * @param spec data table
     * @return builder to configure row comparator
     */
    public static RowComparatorBuilder on(final DataTableSpec spec) {
        return new RowComparatorBuilder(spec);
    }

    /**
     * Compare rows based on the given comparators.
     * @param columns column comparators to compare
     */
    private RowComparator(final List<Comparator<DataRow>> comparators) {
        m_comparators = comparators;
    }

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
        assert dr1.getNumCells() == dr2.getNumCells() : String.format( // NOSONAR run time performance
            "The rows %s and %s don't contain the same number of cells.", dr1.getKey(), dr2.getKey()); //

        for (var comparator : m_comparators) {
            var comparison = comparator.compare(dr1, dr2);
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }
}
