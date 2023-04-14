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
 *   14 Nov 2022 (manuelhotz): created
 */
package org.knime.core.data.sort;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.filestore.internal.NotInWorkflowDataRepository;
import org.knime.core.data.sort.RowComparator.RowComparatorBuilder;
import org.knime.core.data.util.memory.MemoryAlertSystemTest;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.virtual.parchunk.VirtualParallelizedChunkPortObjectInNodeFactory;

/**
 * Tests the row comparator used for sorting rows in data tables.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
public class RowComparatorTest {

    private BufferedDataTable m_table;
    private ExecutionContext m_exec;
    private DataTableSpec m_spec;

    /**
     * Create a sample table containing some data.
     * @throws InterruptedException if thread is interrupted
     */
    @Before
    public void setUp() throws InterruptedException {
        @SuppressWarnings({"unchecked", "rawtypes"})
        final NodeFactory<NodeModel> dummyFactory =
            (NodeFactory) new VirtualParallelizedChunkPortObjectInNodeFactory(new PortType[0]);
        m_exec = new ExecutionContext(new DefaultNodeProgressMonitor(), new Node(dummyFactory),
                SingleNodeContainer.MemoryPolicy.CacheInMemory, NotInWorkflowDataRepository.newInstance());
        m_spec = new DataTableSpec(
            new DataColumnSpecCreator("MyDouble", DoubleCell.TYPE).createSpec(),
            new DataColumnSpecCreator("MyInt", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("MyString", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("RowKeyLeadingZeros", StringCell.TYPE).createSpec()
            );
        final var container = m_exec.createDataContainer(m_spec);
        // 11 entries so we get "Row10" as row id naturally
        IntStream.range(0, 11).forEach(
            // leading zeros with 2 digits for easy lexicographic proxy comparison of an alphanumeric ordering on RowKey
            i -> container.addRowToTable(createRow(i, 1.0 * i, i, String.valueOf(i), String.format("%02d", i))));
        // add two rows containing missing values
        IntStream.range(11, 13).forEach(
            i -> container.addRowToTable(createRow(i, null, null, String.valueOf(i), String.format("%02d", i)))
                );
        container.close();
        m_table = container.getTable();
        MemoryAlertSystemTest.forceGC();
    }


    /**
     * Tests ascending and descending sorting of the sample table by row key.
     * @throws CanceledExecutionException shuffling canceled
     */
    @Test
    public void testCompareRowKeys() throws CanceledExecutionException {
        for (final boolean ascending : new boolean[] { true, false }) {
            try (final var shuffler = new ClosableShuffler(m_table, m_exec, 42)) {
                final var shuffledTable = shuffler.getShuffled();

                final var rc = RowComparator.on(m_spec).thenComparingRowKey(
                    rk -> rk.withDescendingSortOrder(!ascending)).build();
                final var sorter = new BufferedDataTableSorter(shuffledTable, rc);
                final var sorted = sorter.sort(m_exec);

                final var rowKeyComparator = Comparator.comparing(RowKey::getString, Comparator.naturalOrder());

                try (final var it = sorted.iterator()) {
                    RowKey last = null;
                    while (it.hasNext()) {
                        final var next = it.next();
                        final var rkv = next.getKey();
                        if (last != null) {
                            final var cmp = rowKeyComparator.compare(rkv, last);
                            assertTrue(ascending ? cmp > 0 : cmp < 0);
                        }
                        last = rkv;
                    }
                }
            }
        }
    }

    /**
     * Tests alphanumeric sort order of row keys by sorting the sample table.
     * @throws CanceledExecutionException shuffling canceled
     */
    @Test
    public void testCompareRowKeysAlphanumericAscending() throws CanceledExecutionException {
        try (final var shuffler = new ClosableShuffler(m_table, m_exec, 42)) {
            final var shuffledTable = shuffler.getShuffled();

            final var comp = RowComparator.on(m_spec).thenComparingRowKey(
                rk -> rk.withAlphanumericComparison()).build();
            final var sorter = new BufferedDataTableSorter(shuffledTable, comp);
            final var sorted = sorter.sort(m_exec);
            final var lex = Comparator.comparing(StringCell::getStringValue, Comparator.naturalOrder());
            try (final var it = sorted.iterator()) {
                StringCell last = null;
                while (it.hasNext()) {
                    final var next = it.next();
                    final var rowKeyLeadingZeros = CheckUtils.checkCast(next.getCell(3), StringCell.class,
                        IllegalStateException::new, "Expected string in last column of test data.");
                    if (last != null) {
                        assertTrue(lex.compare(rowKeyLeadingZeros, last) > 0);
                    }
                    last = rowKeyLeadingZeros;
                }
            }
        }
    }

    /**
     * It should not be possible to add the row key twice to the comparator.
     */
    @Test
    public void testRowKeyTwiceThrows() {
        final RowComparatorBuilder cmp = assertDoesNotThrow(
            () -> RowComparator.on(m_spec).thenComparingRowKey(rk -> rk.withDescendingSortOrder()));
        assertThrows(IllegalArgumentException.class,
            () -> cmp.thenComparingRowKey(rk -> rk.withDescendingSortOrder()));
    }

    /**
     * Tests sorting of a column in ascending and descending order.
     * @throws CanceledExecutionException if shuffling is canceled
     */
    @Test
    public void testCompareColumn() throws CanceledExecutionException {
        for (final boolean ascending : new boolean[] { true, false }) {
            try (final var shuffler = new ClosableShuffler(m_table, m_exec, 42)) {
                final var shuffledTable = shuffler.getShuffled();

                final var rc = RowComparator.on(m_spec).thenComparingColumn(1,
                    col -> col.withDescendingSortOrder(!ascending)).build();
                final var sorter = new BufferedDataTableSorter(shuffledTable, rc);
                final var sorted = sorter.sort(m_exec);

                final var intComparator = Comparator.comparing(IntCell::getIntValue, Comparator.naturalOrder());

                try (final var it = sorted.iterator()) {
                    IntCell last = null;
                    while (it.hasNext()) {
                        final var next = it.next();
                        final var cell = next.getCell(1);
                        if (cell.isMissing()) {

                        } else {
                            final IntCell ic = (IntCell) cell;
                            if (last != null) {
                                final var cmp = intComparator.compare(ic, last);
                                assertTrue(ascending ? cmp > 0 : cmp < 0);
                            }
                            last = ic;
                        }
                    }
                }
            }
        }
    }

    /**
     * Tests alphanumeric sorting of a column.
     * @throws CanceledExecutionException if shuffling is canceled
     */
    @Test
    public void testCompareColumnAlphanumericAscending() throws CanceledExecutionException {
        try (final var shuffler = new ClosableShuffler(m_table, m_exec, 42)) {
            final var shuffledTable = shuffler.getShuffled();

            // column at index 3 is string rep of row key value with leading zeros, thus can be compared
            // lexicographically easily
            final var comp = RowComparator.on(m_spec)
                    .thenComparingColumn(3, col -> col.withAlphanumericComparison()).build();
            final var sorter = new BufferedDataTableSorter(shuffledTable, comp);
            final var sorted = sorter.sort(m_exec);
            final var lex = Comparator.comparing(StringCell::getStringValue, Comparator.naturalOrder());
            try (final var it = sorted.iterator()) {
                StringCell last = null;
                while (it.hasNext()) {
                    final var next = it.next();
                    final var rowKeyLeadingZeros = CheckUtils.checkCast(next.getCell(3), StringCell.class,
                        IllegalStateException::new, "Expected string in last column of test data.");
                    if (last != null) {
                        assertTrue(lex.compare(rowKeyLeadingZeros, last) > 0);
                    }
                    last = rowKeyLeadingZeros;
                }
            }
        }
    }

    /**
     * Tests that the same column cannot be added twice.
     */
    @Test
    public void testColumnTwiceThrows() {
        final RowComparatorBuilder comp = assertDoesNotThrow(
            () -> RowComparator.on(m_spec).thenComparingColumn(0, c -> c));
        assertThrows(IllegalArgumentException.class,
            () -> comp.thenComparingColumn(0, c -> c));
    }

    /**
     * Tests that alphanumeric comparison is only available for string-compatible types.
     */
    @Test
    public void testIncompatibleThrows() {
        // Double is not string-compatible
        final var comp = RowComparator.on(m_spec);
        assertThrows(IllegalStateException.class, () -> comp.thenComparingColumn(0,
            c -> c.withAlphanumericComparison()));
    }

    /**
     * Exercises the lazy evaluation of column comparators. Does not really test anything new and mainly increases
     * the code coverage.
     */
    @Test
    public void testComparatorShortcuts() {
        // increase code coverage
        final var row1 = createRow(0L, 0d, 0, "First", "A");
        final var row2 = createRow(1L, 0d, 1, "Second", "B");
        final var cb = RowComparator.on(m_spec);
        IntStream.range(0, 3).forEach(i -> cb.thenComparingColumn(i, c -> c));
        final var comp = cb.build();

        assertTrue(comp.compare(row1, row2) < 0);
    }

    /**
     * Tests that all four cases of (ASC, DESC) x (missings last, missings first) work (and are possible).
     */
    @Test
    public void testMissingsOrder() {
        final var spec = new DataTableSpec(new DataColumnSpecCreator("MyDouble", DoubleCell.TYPE).createSpec());
        final var row0 = new DefaultRow(RowKey.createRowKey(0L), new DoubleCell(0));
        final var row1 = new DefaultRow(RowKey.createRowKey(1L), new DoubleCell(1));
        final var rowMissingCell = new DefaultRow(RowKey.createRowKey(2L), DataType.getMissingCell());

        for (final var ascending : new boolean[] { false, true }) {
            for (final var missingsLast : new boolean[] { false, true } ) {

                final var order = ascending ? "ASC" : "DESC";
                final var miss = missingsLast ? "MISS_LAST" : "MISS_FIRST";

                final var comp = RowComparator.on(spec).thenComparingColumn(0,
                            c -> c.withDescendingSortOrder(!ascending).withMissingsLast(missingsLast)).build();

                final var normalRows = comp.compare(row0, row1);
                final var expectedNormalRows = ascending ? -1 : 1;
                assertEquals(expectedNormalRows, normalRows,
                    String.format("Non-missing rows have wrong sort order for %s with %s", order, miss));

                // regardless of sort order, when missings are last the "outer" comparator should put them always
                // as largest, when they are first, they should be compared as smallest
                final var expectedMissing = missingsLast ? 1 : -1;
                for (final var r : new DefaultRow[] { row0, row1 }) {
                    assertEquals(expectedMissing, comp.compare(rowMissingCell, r),
                        String.format("Unexpected order for missing vs. non-missing and %s with %s.", order, miss));
                }
            }
        }
    }

    /**
     * Tests that it is possible to make missings order dependent on non-missing sort order.
     */
    @Test
    public void testMissingsSmallestLargest() {
        final var spec = new DataTableSpec(new DataColumnSpecCreator("MyDouble", DoubleCell.TYPE).createSpec());
        final var row0 = new DefaultRow(RowKey.createRowKey(0L), new DoubleCell(0));
        final var row1 = new DefaultRow(RowKey.createRowKey(1L), new DoubleCell(1));
        final var rowMissingCell = new DefaultRow(RowKey.createRowKey(2L), DataType.getMissingCell());

        for (final var descending : new boolean[] { true, false }) {
            for (final var missingsLargest : new boolean[] { true, false } ) {

                final var order = descending ? "DESC" : "ASC";
                final var miss = missingsLargest ? "MISS_LARGEST" : "MISS_SMALLEST";

                final boolean missLast = descending ^ missingsLargest;
                final var comp = RowComparator.on(spec).thenComparingColumn(0,
                            c -> c.withDescendingSortOrder(descending).withMissingsLast(missLast))
                        .build();

                final var normalRows = comp.compare(row0, row1);
                final var expectedNormalRows = !descending ? -1 : 1;
                assertEquals(expectedNormalRows, normalRows,
                    String.format("Non-missing rows have wrong sort order for %s with %s", order, miss));

                final var expectedMissing = !missLast ? -1 : 1;
                for (final var r : new DefaultRow[] { row0, row1 }) {
                    assertEquals(expectedMissing, comp.compare(rowMissingCell, r),
                        String.format("Unexpected order for missing vs. non-missing and %s with %s.", order, miss));
                }
            }
        }
    }

    /**
     * Tests the comparison of identical and null data rows.
     */
    @Test
    public void testCompareIdentity() {
        final var spec = new DataTableSpec(new DataColumnSpecCreator("MyDouble", DoubleCell.TYPE).createSpec());
        final var row = new DefaultRow(RowKey.createRowKey(0L), new DoubleCell(0));
        final var comp = RowComparator.on(spec).thenComparingColumn(0, c -> c.withDescendingSortOrder()).build();
        assertEquals(0, comp.compare(row, row), "Unexpected inequality of same row.");
        assertTrue(comp.compare(row, null) < 0, "Unexpected order of non-missing and missing row.");
        assertTrue(comp.compare(null, row) > 0, "Unexpected order of missing and non-missing row.");
    }

    /**
     * Tests that the old constructor-based API agrees with the new builder API, except in the case
     * which is now possible with the new API.
     * In the old version it was not possible to sort descending but with missing values on top.
     *
     * @throws CanceledExecutionException if shuffle is canceled
     */
    @Test
    public void testOldVsNewAPI() throws CanceledExecutionException {

        final var spec = new DataTableSpec(new DataColumnSpecCreator("MyInt", IntCell.TYPE).createSpec());
        final var container = m_exec.createDataContainer(spec);
        // 2 rows with values and 2 with missing cells
        IntStream.range(0, 4).forEach(
            i -> container.addRowToTable(new DefaultRow(RowKey.createRowKey((long)i), i <= 1 ? new IntCell(i) : DataType.getMissingCell())));
        container.close();
        final var table = container.getTable();

        for (final var missingsLast : new boolean[] { true, false } ) {
            for (final var descending : new boolean[] { true, false } ) {
                try (final var shuffler = new ClosableShuffler(table, m_exec, 42)) {
                    final var shuffledTable = shuffler.getShuffled();

                    final var newComp = RowComparator.on(m_spec) //
                            .thenComparingColumn(0, //
                                c -> c.withDescendingSortOrder(descending) //
                                      .withMissingsLast(missingsLast))//
                            .build();
                    final var sorter = new BufferedDataTableSorter(shuffledTable, newComp);
                    final var sorted = sorter.sort(m_exec);
                    // assert correct order of rows
                    // should be two blocks of 2 rows each: [non-missing] and [missing]
                    // DESC and missings on top should not be possible with old comparator based on the constructor
                    assertBlockOrder(sorted, descending, missingsLast);


                    @SuppressWarnings("deprecation")
                    final var oldComp = new RowComparator(
                        new int[] { 0 },
                        new boolean[] { !descending },
                        missingsLast,
                    m_spec);
                    final var sorterOld = new BufferedDataTableSorter(shuffledTable, oldComp);
                    final var sortedOld = sorterOld.sort(m_exec);
                    // old comparator constructor will not result in the case DESC-MissOnTop, therefore we expect
                    // the missing block at a different position in one case
                    assertBlockOrder(sortedOld, descending, missingsLast || descending);


                    // same as above for the explicit constructor
                    final var sorterOldConstr = new BufferedDataTableSorter(shuffledTable,
                        Arrays.asList("MyInt"),
                        new boolean[] { !descending },
                        missingsLast);
                    final var sortedOldConstr = sorterOldConstr.sort(m_exec);
                    // old sorter constructor will not result in the case DESC-MissOnTop, therefore we expect
                    // the missing block at a different position in one case
                    assertBlockOrder(sortedOldConstr, descending, missingsLast || descending);

                }
            }
        }
    }

    private static void assertBlockOrder(final BufferedDataTable sorted, final boolean descending,
            final boolean missingsLast) {
        final var rowKeys = RowComparator.on(sorted.getSpec()).thenComparingRowKey().build();
        try (final var it = sorted.iterator()) {
            final DataRow[] rows = new DataRow[] { it.next(), it.next(), it.next(), it.next() };
            final var blocks = new int[][] { new int[] { 0, 1 }, new int[] { 2, 3 } };

            final int[] missingBlock = blocks[missingsLast ? 1 : 0];
            final int[] nonMissingBlock = blocks[missingsLast ? 0 : 1];

            // assert correct order of blocks
            DataRow prev = null;
            for (final int nonI : nonMissingBlock) {
                final var next = rows[nonI];
                assertFalse(next.getCell(0).isMissing(),
                    String.format("Unexpected missing cell in row %d", nonI));
                if (prev != null) {
                    assertEquals(rowKeys.compare(prev, next), descending ? 1 : -1,
                            String.format("Unexpected order of row keys (%s, %s) for %s sort order.",
                                prev.getKey(), next.getKey(), descending ? "DESC" : "ASC"));
                }
                prev = next;
            }
            prev = null;
            for (final int missI : missingBlock) {
                final var next = rows[missI];
                assertTrue(next.getCell(0).isMissing(),
                    String.format("Unexpected non-missing cell in row %d", missI));
                if (prev != null) {
                    // our single-cell rows with missings should not be re-ordered by stable sorting algorithm since
                    // all missings are equal
                    assertEquals(rowKeys.compare(prev, next), -1,
                            String.format("Unexpected order of row keys (%s, %s) for %s sort order.",
                                prev.getKey(), next.getKey(), descending ? "DESC" : "ASC"));
                }
                prev = next;
            }
        }
    }

    /**
     * Create a new test row based on the passed data. If any argument is null, the row will have a MissingCell in its
     * place.
     *
     * @param rowKey Row ID for the row
     * @param d double value for first cell
     * @param i int value for second cell
     * @param s string value for third cell
     * @param s2 string value for fourth cell
     * @return the created row (rowKey, d, i, s, s2)
     */
    private static DataRow createRow(final long rowKey, final Double d, final Integer i, final String s, final String s2) {
        return new DefaultRow(RowKey.createRowKey(rowKey),
            d != null ? new DoubleCell(d) : DataType.getMissingCell(),
            i != null ? new IntCell(i) : DataType.getMissingCell(),
            s != null ? new StringCell(s) : DataType.getMissingCell(),
            s2 != null ? new StringCell(s2) : DataType.getMissingCell());
    }
}
