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
 *   14 Feb 2019 (Marc): created
 */
package org.knime.core.data.container;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.util.memory.MemoryAlertSystemTest;
import org.knime.core.util.Pair;

/**
 * Test cases for the class <code>BufferCache</code>.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public class BufferCacheTest {

    private static int OFFSET = 0;

    /**
     * Tests the basic functionality (put, invalidate, contains, get) for the cache if tables aren't cleared for garbage
     * collection.
     */
    @Test
    public void testHardRefCache() {
        // put k tables into cache
        final int k = BufferSettings.getDefault().getLRUCacheSize();
        final List<Pair<Buffer, List<BlobSupportDataRow>>> tables = generateKTables(k, false);
        final BufferCache cache = new BufferCache();
        addTablesToCache(tables, cache, false);

        // invalidate every other table
        for (int i = 0; i < k; i++) {
            if (i % 2 == 0) {
                cache.invalidate(tables.get(i).getFirst());
            }
        }

        // check if get and contains methods respect invalidation of tables
        for (int i = 0; i < k; i++) {
            final Buffer buffer = tables.get(i).getFirst();
            if (i % 2 == 0) {
                Assert.assertEquals("Invalidated table still in cache.", cache.get(buffer), Optional.empty());
                Assert.assertFalse("Invalidated table still in cache.", cache.contains(buffer));
            } else {
                Assert.assertNotEquals("Table no longer in cache.", cache.get(buffer), Optional.empty());
                Assert.assertTrue("Invalidated table still in cache.", cache.contains(buffer));
            }
        }
    }

    /**
     * Tests that when tables are cleared for garbage collection, only least-recently-used ("hot") tables are kept in
     * memory subsequent to garbage collection.
     *
     * @throws InterruptedException thrown when the thread is unexpectedly interrupted during sleep.
     */
    @Test
    public void testSoftRefCache() throws InterruptedException {
        // generate k-1 hot tables and k cold tables where k equals the LRU cache size -1
        List<Pair<Buffer, List<BlobSupportDataRow>>> hotTables =
            generateKTables(BufferSettings.getDefault().getLRUCacheSize() - 1, true);
        List<Pair<Buffer, List<BlobSupportDataRow>>> coldTables =
            generateKTables(BufferSettings.getDefault().getLRUCacheSize(), true);

        // put all hot tables into the cache
        final BufferCache cache = new BufferCache();
        addTablesToCache(hotTables, cache, true);

        // then, put all cold tables into the cache
        for (Pair<Buffer, List<BlobSupportDataRow>> coldTable : coldTables) {
            final Buffer buffer = coldTable.getFirst();
            cache.put(buffer, coldTable.getSecond());
            cache.clearForGarbageCollection(buffer);
            // for each cold table put into the cache, access each hot table once (to keep tables hot)
            getTablesFromCache(hotTables, cache);
        }
        // if LRU strategy was implemented correctly, all k-1 hot tables should have remained in the LRU cache
        // now add one additional table to push the last cold table out of LRU
        addTablesToCache(generateKTables(1, true), cache, true);

        // drop hard references on lists but keep hard references on buffers (we don't want the buffers to be GCed)
        final List<Pair<Buffer, WeakReference<List<BlobSupportDataRow>>>> weakenedHotTables = weaken(hotTables);
        final List<Pair<Buffer, WeakReference<List<BlobSupportDataRow>>>> weakenedColdTables = weaken(coldTables);

        hotTables = null;
        coldTables = null;

        // invoke garbage collection
        MemoryAlertSystemTest.forceGC();

        // check that all hot tables are still in the cache (due to them being softly referenced)
        for (Pair<Buffer, WeakReference<List<BlobSupportDataRow>>> weakenedHotTable : weakenedHotTables) {
            final Buffer buffer = weakenedHotTable.getFirst();
            final List<BlobSupportDataRow> list = weakenedHotTable.getSecond().get();
            final Optional<List<BlobSupportDataRow>> listFromCache = cache.get(buffer);
            Assert.assertNotNull("Reference to list has been dropped unexpectedly.", list);
            Assert.assertTrue("List could not be retrieved from cache.", listFromCache.isPresent());
            Assert.assertEquals("List retrieved from cache differs from list put into cache.", list,
                listFromCache.get());
        }

        // check that all cold tables have been dropped (due to them being only weakly referenced)
        for (Pair<Buffer, WeakReference<List<BlobSupportDataRow>>> weakenedColdTable : weakenedColdTables) {
            Assert.assertNull("Reference to list has not been dropped as instructed.",
                weakenedColdTable.getSecond().get());
        }
    }

    private static List<Pair<Buffer, List<BlobSupportDataRow>>> generateKTables(final int k,
        final boolean flushToDisk) {

        final List<List<BlobSupportDataRow>> lists = IntStream.range(OFFSET + 0, OFFSET + k)
            .mapToObj(i -> new BlobSupportDataRow(RowKey.createRowKey((long)i),
                new DataCell[]{new IntCell(i), new StringCell(Integer.toString(i)), new LongCell(i),
                    new DoubleCell(i + .5), i % 2 == 1 ? BooleanCell.TRUE : BooleanCell.FALSE}))
            .map(Collections::singletonList).collect(Collectors.toList());

        final DataTableSpec spec = new DataTableSpec(new DataColumnSpecCreator("int", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("string", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("long", LongCell.TYPE).createSpec(),
            new DataColumnSpecCreator("double", DoubleCell.TYPE).createSpec(),
            new DataColumnSpecCreator("boolean", BooleanCell.TYPE).createSpec());

        final List<Pair<Buffer, List<BlobSupportDataRow>>> result = new ArrayList<>();
        for (List<BlobSupportDataRow> list : lists) {
            final DataContainer container = new DataContainer(spec, true, flushToDisk ? 0 : Integer.MAX_VALUE);
            for (BlobSupportDataRow row : list) {
                container.addRowToTable(row);
            }
            container.close();
            final Buffer buffer = container.getBufferedTable().getBuffer();
            result.add(new Pair<Buffer, List<BlobSupportDataRow>>(buffer, list));
        }

        OFFSET += k;

        return result;
    }

    private static List<Pair<Buffer, WeakReference<List<BlobSupportDataRow>>>>
        weaken(final List<Pair<Buffer, List<BlobSupportDataRow>>> tables) {
        return tables.stream().map(p -> new Pair<Buffer, WeakReference<List<BlobSupportDataRow>>>(p.getFirst(),
            new WeakReference<List<BlobSupportDataRow>>(p.getSecond()))).collect(Collectors.toList());
    }

    private static void addTablesToCache(final List<Pair<Buffer, List<BlobSupportDataRow>>> tables,
        final BufferCache cache, final boolean clearForGC) {
        for (Pair<Buffer, List<BlobSupportDataRow>> table : tables) {
            final Buffer buffer = table.getFirst();
            cache.put(buffer, table.getSecond());
            if (clearForGC) {
                cache.clearForGarbageCollection(buffer);
            }
        }
    }

    private static void getTablesFromCache(final List<Pair<Buffer, List<BlobSupportDataRow>>> tables,
        final BufferCache cache) {
        for (Pair<Buffer, List<BlobSupportDataRow>> table : tables) {
            cache.get(table.getFirst());
        }
    }

}
