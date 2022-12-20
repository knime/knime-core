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
 *   14 Nov 2022 ("Manuel Hotz &lt;manuel.hotz@knime.com&gt;"): created
 */
package org.knime.core.data.sort;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * @author "Manuel Hotz &lt;manuel.hotz@knime.com&gt;"
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
            i -> container.addRowToTable(createRow(i, i, i, String.valueOf(i), String.format("%02d", i))));
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
                        final IntCell ic = (IntCell) next.getCell(1);
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
        final var row1 = createRow(0L, 0, 0, "First", "A");
        final var row2 = createRow(1L, 0, 1, "Second", "B");
        final var cb = RowComparator.on(m_spec);
        IntStream.range(0, 3).forEach(i -> cb.thenComparingColumn(i, c -> c));
        final var comp = cb.build();

        assertTrue(comp.compare(row1, row2) < 0);
    }

    /**
     * Tests that missing cells are always sorted to end of list if configured, else according to type comparator.
     */
    @Test
    public void testMissingsOrder() {
        for (final var descending : new boolean[] { false, true }) {
            final var alphanum = false;
            final var spec = new DataTableSpec(new DataColumnSpecCreator("MyDouble", DoubleCell.TYPE).createSpec());

            final var row = new DefaultRow(RowKey.createRowKey(0L), new DoubleCell(0));
            final var missing = new DefaultRow(RowKey.createRowKey(1L), DataType.getMissingCell());

            // if the flag is not set, choose order based on type's comparator
            final var missingsDefault = RowComparator.on(spec).thenComparingColumn(0,
                c -> c.withDescendingSortOrder(descending).withAlphanumericComparison(alphanum)).build();
            final var md = missingsDefault.compare(missing, row);
            assertTrue(!descending ? md < 0 : md > 0);

            // if flag is set, hard-code missings at the end of the list
            final var missingsLast = RowComparator.on(spec).thenComparingColumn(0,
                c -> c.withDescendingSortOrder(descending).withAlphanumericComparison(alphanum).withMissingsLast())
                    .build();
            final var lst = missingsLast.compare(missing, row);
            assertTrue(lst > 0);
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
        assertEquals(0, comp.compare(row, row));
        assertTrue(comp.compare(row, null) < 0);
        assertTrue(comp.compare(null, row) > 0);
    }

    /**
     * Tests that the old constructor-based API agrees with the new builder API.
     * @throws CanceledExecutionException if shuffle is canceled
     */
    @Test
    public void testOldVsNewAPI() throws CanceledExecutionException {

        try (final var shuffler = new ClosableShuffler(m_table, m_exec, 42)) {
            final var shuffledTable = shuffler.getShuffled();

            final var newComp = RowComparator.on(m_spec)
                    .thenComparingRowKey(k -> k)
                    .thenComparingColumn(0, c -> c.withDescendingSortOrder().withMissingsLast())
                    .thenComparingColumn(1, c -> c.withMissingsLast())
                    .build();

            @SuppressWarnings("deprecation")
            final var oldComp = new RowComparator(
                new int[] { -1, 0 , 1},
                new boolean[] {true, false, true},
                false,
            m_spec);

            final var sorter = new BufferedDataTableSorter(shuffledTable, new Comparator<DataRow> () {

                @Override
                public int compare(final DataRow o1, final DataRow o2) {
                    final var nc = newComp.compare(o1, o2);
                    final var oc = oldComp.compare(o1, o2);
                    assertEquals(nc, oc, "Old constructor should result in same comparator as new API");
                    return nc;
                }

            });
            final var sorted = sorter.sort(m_exec);

            final var rowKeyComparator = Comparator.comparing(RowKey::getString, Comparator.naturalOrder());

            try (final var it = sorted.iterator()) {
                RowKey last = null;
                while (it.hasNext()) {
                    final var next = it.next();
                    final var rkv = next.getKey();
                    if (last != null) {
                        final var cmp = rowKeyComparator.compare(rkv, last);
                        assertTrue(cmp > 0);
                    }
                    last = rkv;
                }
            }
        }
    }


    private static DataRow createRow(final long rowKey, final double d, final int i, final String s, final String s2) {
        return new DefaultRow(RowKey.createRowKey(rowKey), new DoubleCell(d), new IntCell(i),
            s != null ? new StringCell(s) : DataType.getMissingCell(),
            s2 != null ? new StringCell(s2) : DataType.getMissingCell());
    }
}
