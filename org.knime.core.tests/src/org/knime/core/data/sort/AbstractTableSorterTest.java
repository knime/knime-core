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
 *   Oct 27, 2023 (leonard.woerteler): created
 */
package org.knime.core.data.sort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.Pair;
import org.knime.testing.core.ExecutionContextExtension;
import org.knime.testing.util.TableTestUtil;

/**
 * Tests for the merge phase of {@link AbstractTableSorter}.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("javadoc")
class AbstractTableSorterTest {
    private static final DataTableSpec SPEC = new TableTestUtil.SpecBuilder() //
            .addColumn("strCol", StringCell.TYPE) //
            .addColumn("dblCol", DoubleCell.TYPE) //
            .build();

    private static final RowComparator COMP = RowComparator.on(SPEC) //
            .thenComparingColumn(1, DoubleCell.TYPE.getComparator()) //
            .thenComparingColumn(0, StringCell.TYPE.getComparator()) //
            .build();

    @RegisterExtension
    static ExecutionContextExtension executionContextExtension = ExecutionContextExtension.create();

    /** Merges three chunks into a materialized iterator with {@code k=3}. */
    @Test
    void testMergePhaseSmall(final ExecutionContext exec) throws CanceledExecutionException {
        final var n = 2_000;
        final var randomData = createChunks(exec, n, 3, 456456456L);
        final var tables = randomData.getFirst();
        final var doublesSorted = randomData.getSecond();

        final var sorter = new AbstractTableSorter(n, SPEC, COMP) {};
        sorter.setMaxOpenContainers(3);
        sorter.setSortInMemory(false);

        final var ioHandler = BufferedDataTableSorter.createTableIOHandler(exec);
        try (final var mergePhase = sorter.createMergePhase(ioHandler, new ArrayDeque<>(tables), n)) {
            assertEquals(n, mergePhase.getNumRows());
            assertEquals(0, mergePhase.computeNumLevels(false));
            assertEquals(1, mergePhase.computeNumLevels(true));

            try (final var iter = mergePhase.mergeIntoMaterializedIterator(exec)) {
                checkResult(iter, doublesSorted);
            }
        }
    }

    /** Merges 16 chunks into a non-materialized iterator with {@code k=3}. */
    @Test
    void testMergePhase(final ExecutionContext exec) throws CanceledExecutionException {
        final var n = 2_000;

        // `k=16` means that we will get five merges of 3 chunks each in the first round, with the last chunk being
        // copied over into the next round. The second round will contain two merges of three chunks each, and the third
        // one will be a binary merge.
        final var randomData = createChunks(exec, n, 16, 133771984L);
        final var tables = randomData.getFirst();
        final var doublesSorted = randomData.getSecond();

        final var sorter = new AbstractTableSorter(n, SPEC, COMP) {};
        sorter.setMaxOpenContainers(3);
        sorter.setSortInMemory(false);

        final var ioHandler = BufferedDataTableSorter.createTableIOHandler(exec);
        try (final var mergePhase = sorter.createMergePhase(ioHandler, new ArrayDeque<>(tables), n)) {
            assertEquals(n, mergePhase.getNumRows());
            assertEquals(2, mergePhase.computeNumLevels(false));
            assertEquals(3, mergePhase.computeNumLevels(true));

            try (final var iter = mergePhase.mergeIntoIterator(exec)) {
                checkResult(iter, doublesSorted);
            }
        }
    }

    /**
     * Creates a specified number of rows, distributed over a given number of chunks. Each row has one duplicate.
     *
     * @param exec execution context
     * @param n number of rows
     * @param k number of chunks
     * @param seed seed for the RNG
     * @return pair containing the chunks and an array of {@code n/2} (sorted) values contained in the second column
     */
    private static Pair<List<BufferedDataTable>, double[]> createChunks(final ExecutionContext exec, final int n,
            final int k, final long seed) {
        final List<BufferedDataContainer> containers = IntStream.range(0, k) //
                .mapToObj(i -> exec.createDataContainer(SPEC)) //
                .toList();

        final Random rng = new Random(seed);
        final var doublesSorted = rng.doubles(n / 2).sorted().toArray();
        for (var i = 0; i < n / 2; i++) {
            final var cells =
                    new DataCell[] { TableTestUtil.cellify("foo_" + i), TableTestUtil.cellify(doublesSorted[i]) };
            final int[] cids = IntStream.range(0, 2).map(j -> rng.nextInt(containers.size())).sorted().toArray();
            for (int j = 0; j < 2; j++) {
                final var container = containers.get(cids[j]);
                container.addRowToTable(new DefaultRow(RowKey.createRowKey(2L * i + j), cells));
            }
        }
        containers.stream().forEach(BufferedDataContainer::close);
        final var tables = containers.stream().map(BufferedDataContainer::getTable).toList();
        return Pair.create(tables, doublesSorted);
    }

    /**
     * Checks that the merged iterator contains the correct rows in the correct order and that the sort is stable.
     *
     * @param iter merged iterator
     * @param doublesSorted ground truth for the second column
     */
    private static void checkResult(final CloseableRowIterator iter, final double[] doublesSorted) {
        final var n = 2 * doublesSorted.length;
        final var rowKeyComp = RowComparator.on(SPEC) //
                .thenComparingRowKey(new AlphanumericComparator(Comparator.naturalOrder())) //
                .build();

        var seen = 0;
        DataRow prev = null;
        while (iter.hasNext()) {
            final var row = iter.next();
            assertEquals(doublesSorted[seen / 2], ((DoubleValue)row.getCell(1)).getDoubleValue());
            if (seen % 2 == 1) {
                // check for stable sorting
                assertEquals(row.getCell(0), Objects.requireNonNull(prev).getCell(0));
                assertTrue(rowKeyComp.compare(prev, row) < 0, "Expected stable sort order");
            }
            seen++;
            prev = row;
        }
        assertEquals(n, seen);
    }
}
