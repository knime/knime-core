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
 *   Oct 25, 2023 (leonard.woerteler): created
 */
package org.knime.core.data.sort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DefaultRow;

/**
 * Tests the <i>k</i>-way merge iterator.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
class KWayMergeIteratorTest {

    @Test
    void testKZero() {
        final Comparator<DataRow> rowComp = Comparator.comparing(r -> r.getKey().getString());
        @SuppressWarnings("resource")
        final var empty = new KWayMergeIterator(rowComp, new CloseableRowIterator[0]);
        assertThrows(NoSuchElementException.class, empty::next);
        assertFalse(empty.hasNext());
    }

    @Test
    void testKOneNull() {
        final Comparator<DataRow> rowComp = Comparator.comparing(r -> r.getKey().getString());
        @SuppressWarnings("resource")
        final var empty = new KWayMergeIterator(rowComp, new CloseableRowIterator[] { null });
        assertThrows(NoSuchElementException.class, empty::next);
        assertFalse(empty.hasNext());
    }

    @SuppressWarnings("resource")
    @Test
    void testKOne() {
        final Comparator<DataRow> rowComp = Comparator.comparing(r -> r.getKey().getString());
        String[] keys = { "A", "B", "C" };
        CloseableRowIterator rowIter = CloseableRowIterator.wrap(Arrays.stream(RowKey.toRowKeys(keys)) //
                .map(k -> new DefaultRow(k, new DataCell[0])) //
                .iterator());
        final var mergeIter = new KWayMergeIterator(rowComp, new CloseableRowIterator[] { rowIter });
        assertTrue(mergeIter.hasNext());
        assertEquals("A", mergeIter.next().getKey().getString());
        assertTrue(mergeIter.hasNext());
        assertEquals("B", mergeIter.next().getKey().getString());
        assertTrue(mergeIter.hasNext());
        assertEquals("C", mergeIter.next().getKey().getString());
        assertFalse(mergeIter.hasNext());
        assertThrows(NoSuchElementException.class, mergeIter::next);
    }

    @SuppressWarnings("resource")
    @Test
    void testKTen() {
        final Comparator<DataRow> rowComp = Comparator.comparing(r -> r.getKey().getString());
        final String[][] keys = {
            /* 0 */ { },
            /* 1 */ { "A1", "C1" },
            /* 2 */ { "C2", "D2", "E2", "F2" },
            /* 3 */ { "D3a", "D3b", "E3a", "E3b" },
            /* 4 */ { "G4" },
            /* 5 */ { "B5a", "B5b", "B5c", "B5d" },
            /* 6 */ { "A6" },
            /* 7 */ { "C7a", "C7b", "D7a", "D7b" },
            /* 8 */ { },
            /* 9 */ { "E9", "F9a", "F9b" },
        };

        final var closeOrder = new ArrayList<>();
        CloseableRowIterator[] rowIters = new CloseableRowIterator[keys.length];
        for (var i = 0; i < keys.length; i++) {
            final int iterNo = i;
            final var iter = Arrays.stream(keys[iterNo]).map(k -> new DefaultRow(k, new DataCell[0])).iterator();
            rowIters[i] = CloseableRowIterator.from(iter, () -> closeOrder.add(iterNo));
        }

        final var sorted = Arrays.stream(keys).flatMap(Arrays::stream).sorted().collect(Collectors.toList());

        final var mergeIter = new KWayMergeIterator(rowComp, rowIters);
        final var mergedKeys = new ArrayList<>();
        mergeIter.forEachRemaining(row -> mergedKeys.add(row.getKey().getString()));
        assertEquals(sorted, mergedKeys, "Unexpected sort order");
        assertEquals(Arrays.asList(0, 8, 6, 5, 1, 7, 3, 2, 9, 4), closeOrder, "Unexpected order of `close()` calls");

        // test `close()`
        final var closeOrder2 = new ArrayList<>();
        CloseableRowIterator[] rowIters2 = new CloseableRowIterator[keys.length];
        for (var i = 0; i < keys.length; i++) {
            final int iterNo = i;
            final var iter = Arrays.stream(keys[iterNo]).map(k -> new DefaultRow(k, new DataCell[0])).iterator();
            rowIters2[i] = CloseableRowIterator.from(iter, () -> closeOrder2.add(iterNo));
        }
        final var mergeIter2 = new KWayMergeIterator(rowComp, rowIters2);
        assertEquals(Arrays.asList(0, 8), closeOrder2, "The two empty iterators are not closed immediately");
        assertEquals("A1", mergeIter2.next().getKey().getString());
        assertEquals("A6", mergeIter2.next().getKey().getString());
        assertEquals(Arrays.asList(0, 8, 6), closeOrder2, "Iterator 6 should be closed after being drained");
        mergeIter2.close();
        assertEquals(Arrays.asList(0, 8, 6, 1, 2, 3, 4, 5, 7, 9), closeOrder2,
            "All non-drained iterators should be closed in order");
    }
}
