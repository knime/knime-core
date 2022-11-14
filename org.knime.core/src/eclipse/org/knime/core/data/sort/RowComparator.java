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
import java.util.LinkedHashMap;
import java.util.OptionalInt;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingDataCellComparator;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.node.util.CheckUtils;

/**
 * The RowComparator is used to compare two DataRows. It implements the Comparator-interface, so we can use the
 * Arrays.sort method to sort an array of DataRows.
 *
 * @since 4.1 made public for the use in the TopK Selector node
 */
public final class RowComparator implements Comparator<DataRow> {

    /**
     * The comparators for the columns (including row key represented by {@link OptionalInt#empty()})
     * in the order they should be compared in together with the column index in the row.
     *
     * Currently, we do not support to compare the same column multiple times since the GUI does not need that option
     * and well-used workarounds (such as computed columns) exist.
     */
    private final LinkedHashMap<OptionalInt, Comparator<?>> m_colComparators;

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
        for (int i = 0; i < indices.length; i++) {
            final int index = indices[i];
            final boolean ascending = sortAscending[i];
            if (isRowKey(index)) {
                rc.thenComparingRowKey(ascending, false);
            } else {
                rc.thenComparingColumn(index, ascending, false, sortMissingsToEnd);
            }
        }
        m_colComparators = rc.build().m_colComparators;
    }

    private static boolean isRowKey(final int index) {
        return index == -1;
    }

    /**
     * Builder to construct a row comparator.
     *
     * @author "Manuel Hotz &lt;manuel.hotz@knime.com&gt;"
     */
    public static final class RowComparatorBuilder {

        private final LinkedHashMap<OptionalInt, Comparator<?>> m_columnComparators = new LinkedHashMap<>();
        private final DataTableSpec m_spec;

        private RowComparatorBuilder(final DataTableSpec spec) {
            m_spec = spec;
        }

        /**
         * Adds the given column to the columns compared by the row comparator using the column type's comparator.
         * @param columnIndex column to compare
         * @param ascending ascending sort order
         * @param alphaNum alphanumeric instead of lexicographic order for string-compatible types, if {@code true}
         *          the given column has to actually be string-compatible
         * @param missingsLast {@code true} if missing cells should be sorted to end,
         *          {@code false} otherwise
         * @return the builder instance
         */
        public RowComparatorBuilder thenComparingColumn(final int columnIndex, final boolean ascending,
            final boolean alphaNum, final boolean missingsLast) {
            final var type = m_spec.getColumnSpec(columnIndex).getType();
            Comparator<DataCell> cellComp;
            if (alphaNum) {
                // compares column on string representation in alphanumerical order
                CheckUtils.checkState(type.isCompatible(StringValue.class), "Only string-compatible columns can "
                        + "be sorted alphanumerically. Column at position %d is not string-compatible.", columnIndex);
                cellComp = Comparator.comparing(dc -> CheckUtils.checkStateType(dc, StringValue.class,
                    "Comparing non-string compatible column").getStringValue(),
                    new AlphanumericComparator(Comparator.naturalOrder()));
            } else {
                cellComp = type.getComparator();
            }
            if (!ascending) {
                cellComp = cellComp.reversed();
            }
            if (missingsLast) {
                cellComp = new MissingDataCellComparator<>(cellComp, true);
            }
            return thenComparingColumn(columnIndex, cellComp);
        }

        /**
         * Adds the given column to the columns compared by the row comparator using the given cell comparator.
         * @param columnIndex column to compare
         * @param cellComp comparator to use for comparison of the column values
         * @return the builder instance
         */
        public RowComparatorBuilder thenComparingColumn(final int columnIndex, final Comparator<DataCell> cellComp) {
            final var colIdx = OptionalInt.of(columnIndex);
            if (m_columnComparators.containsKey(colIdx)) {
                throw new IllegalArgumentException(String.format("Row comparator already contains column #%d",
                    columnIndex));
            }
            m_columnComparators.put(colIdx, cellComp);
            return this;
        }

        /**
         * Adds the row key to the row comparator using the natural order.
         * @param ascending ascending sort order if {@code true}, descending if {@code false}
         * @param alphaNum use alphanumerical comparisons instead of lexicographical
         * @return the builder instance
         */
        public RowComparatorBuilder thenComparingRowKey(final boolean ascending, final boolean alphaNum) {
            Comparator<String> comp = Comparator.naturalOrder();
            if (alphaNum) {
                comp = new AlphanumericComparator(comp);
            }
            if (!ascending) {
                comp = comp.reversed();
            }
            return thenComparingRowKey(comp);
        }

        /**
         * Adds the row key to the row comparator using the given string comparator.
         * @param comp string comparator to use
         * @return the builder instance
         */
        public RowComparatorBuilder thenComparingRowKey(final Comparator<String> comp) {
            final var rowKeyIdx = OptionalInt.empty();
            if (m_columnComparators.containsKey(rowKeyIdx)) {
                throw new IllegalArgumentException("Row comparator already contains row key");
            }
            m_columnComparators.put(rowKeyIdx, Comparator.comparing(RowKey::getString, comp));
            return this;
        }

        /**
         * Builds the configured row comparator.
         * @return row comparator
         */
        public RowComparator build() {
            return new RowComparator(m_columnComparators);
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
    private RowComparator(final LinkedHashMap<OptionalInt, Comparator<?>> columns) { // NOSONAR no suitable interface
        m_colComparators = columns;
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

        // find first non-zero comparison result in order
        return m_colComparators.entrySet().stream()
                .mapToInt(e -> {
                    final var col = e.getKey();
                    final var comp = e.getValue();
                    return comparePos(dr1, dr2, col, comp);
                })
                // find earliest column that differs to avoid unnecessary comparisons
                .filter(cmp -> cmp != 0)
                .findFirst()
                // all cells in the DataRow have the same value
                .orElse(0);
    }

    private static int comparePos(final DataRow dr1, final DataRow dr2, final OptionalInt pos,
        final Comparator<?> comp) {
        if (pos.isEmpty()) {
            @SuppressWarnings("unchecked")
            final var rowKeyComparator = (Comparator<RowKey>) comp;
            return rowKeyComparator.compare(dr1.getKey(), dr2.getKey());
        }
        @SuppressWarnings("unchecked")
        final var cellComparator = (Comparator<DataCell>) comp;
        final var col = pos.getAsInt();
        final var c1 = dr1.getCell(col);
        final var c2 = dr2.getCell(col);
        return cellComparator.compare(c1, c2);
    }
}