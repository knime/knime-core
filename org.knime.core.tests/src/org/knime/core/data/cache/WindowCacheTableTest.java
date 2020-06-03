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
 *   25 Feb 2019 (albrecht): created
 */
package org.knime.core.data.cache;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DirectAccessTable.UnknownRowCountException;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DefaultRowIterator;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

import junit.framework.TestCase;

/**
 *
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 */
public class WindowCacheTableTest extends TestCase {

    private static final int NUM_ROWS = 600;
    private static final int CUSTOM_CACHE_SIZE = 300;
    private static final int CUSTOM_LOOK_AHEAD_SIZE = 20;

    private static final DataTableSpec TABLE_SPEC = new DataTableSpec(
        new DataColumnSpecCreator("col1", StringCell.TYPE).createSpec(),
        new DataColumnSpecCreator("col2", IntCell.TYPE).createSpec(),
        new DataColumnSpecCreator("col3", DoubleCell.TYPE).createSpec()
    );

    private static final DataContainer CONT = new DataContainer(TABLE_SPEC);

    static {
        for (int row = 0; row < NUM_ROWS; row++) {
            DataRow data = new DefaultRow("r" + row, Arrays.asList(
                new StringCell("Some content " + Integer.toString(row)),
                new IntCell(row),
                new DoubleCell(row + 0.4)));
            CONT.addRowToTable(data);
        }
        CONT.close();
    }

    /**
     * Tests the initialization of the cache with no data
     * @throws UnknownRowCountException
     * @throws CanceledExecutionException
     */
    @Test
    public void testWindowCacheWithNull()
        throws UnknownRowCountException, CanceledExecutionException {
        WindowCacheTable cache = new WindowCacheTable(null);
        assertFalse("Cache should have no data", cache.hasData());
        assertTrue("Cache should have row count", cache.hasRowCount());
        assertEquals("Cache should have no rows", 0, cache.getRowCount());
        assertEquals("Cache should have no columns",0, cache.getColumnCount());
        assertNull("Table should be null", cache.getDataTable());
        assertNull("Table spec should be null", cache.getDataTableSpec());
        assertNull("Table name should be null", cache.getTableName());
        try {
            cache.getRows(0, 1, new ExecutionMonitor());
            fail("Should not be able to access an cache without data");
        } catch (IndexOutOfBoundsException e) { /* expected */ }
    }

    /**
     * Tests initialization of cache with an empty table (no rows)
     * @throws UnknownRowCountException
     * @throws CanceledExecutionException
     */
    @Test
    public void testWindowCacheWithEmptyTable() throws UnknownRowCountException, CanceledExecutionException {
        DataContainer emptyContainer = new DataContainer(TABLE_SPEC);
        emptyContainer.close();
        WindowCacheTable cache = new WindowCacheTable(emptyContainer.getTable());
        assertTrue("Cache should have data", cache.hasData());
        assertTrue("Cache should have row count", cache.hasRowCount());
        assertEquals("Cache should have no rows", 0, cache.getRowCount());
        assertEquals("Col count of table should equal col count of cache", TABLE_SPEC.getNumColumns(),
            cache.getColumnCount());
        assertEquals("Tables should be equal", emptyContainer.getTable(), cache.getDataTable());
        assertEquals("Table specs should be equal", emptyContainer.getTableSpec(), cache.getDataTableSpec());
        assertEquals("Table names should be equal", emptyContainer.getTableSpec().getName(), cache.getTableName());
        try {
            cache.getRows(0, 1, new ExecutionMonitor());
            fail("Should not be able to access an cache without data");
        } catch (IndexOutOfBoundsException e) { /* expected */ }
    }

    /**
     * Tests the initialization of the cache with a data table
     * @throws UnknownRowCountException
     */
    @Test
    public void testWindowCacheWithDataTable() throws UnknownRowCountException {
        WindowCacheTable cache = new WindowCacheTable(CONT.getTable());
        checkDefaultTable(cache);
    }

    /**
     * Tests the initialization of the cache with filter columns set
     * @throws UnknownRowCountException
     */
    @Test
    public void testWindowCacheWithFilterColumns() throws UnknownRowCountException {
        WindowCacheTable cache = new WindowCacheTable(CONT.getTable(), "col1");
        checkDefaultTable(cache);
        cache = new WindowCacheTable(CONT.getTable(), new String[0]);
        checkDefaultTable(cache);
        cache = new WindowCacheTable(CONT.getTable(), (String[])null);
        checkDefaultTable(cache);
    }

    private static void checkDefaultTable(final WindowCacheTable cache) throws UnknownRowCountException {
        assertEquals("Cache size should be default", WindowCacheTable.DEFAULT_CACHE_SIZE, cache.getCacheSize());
        assertEquals("Look ahead size should be default", WindowCacheTable.DEFAULT_LOOK_AHEAD,
            cache.getLookAheadSize());
        assertTrue("Cache should have data", cache.hasData());
        assertTrue("Cache should have row count", cache.hasRowCount());
        assertEquals("Row count of table should equal row count of cache", NUM_ROWS, cache.getRowCount());
        assertEquals("Col count of table should equal col count of cache", TABLE_SPEC.getNumColumns(),
            cache.getColumnCount());
        assertEquals("Tables should be equal", CONT.getTable(), cache.getDataTable());
        assertEquals("Table specs should be equal", CONT.getTableSpec(), cache.getDataTableSpec());
        assertEquals("Table names should be equal", CONT.getTableSpec().getName(), cache.getTableName());
        try {
            cache.getRows(0, 1, new ExecutionMonitor());
        } catch (Exception e) {
            fail("Rows should be accessible");
        }
    }

    /**
     * Tests the fetching of rows with and without execution monitors
     */
    @Test
    public void testExecutionMonitorBehaviour() {
        WindowCacheTable cache = new WindowCacheTable(CONT.getTable());
        ExecutionMonitor monitor = new ExecutionMonitor();

        // test regular behavior
        try {
            cache.getRows(0, 1, monitor);
        } catch (Exception e) {
            fail("Rows should be accessible with default execution monitor");
        }
        assertEquals("Monitor should have progress of 100% after fetching rows", 1.0,
            monitor.getProgressMonitor().getProgress());

        // test cancelled execution
        monitor.getProgressMonitor().setExecuteCanceled();
        try {
            cache.getRows(cache.getLookAheadSize() + 1, 1, monitor);
            fail("Execution should be cancelled");
        } catch (CanceledExecutionException e) { /* expected */ }

        // test without monitor
        try {
            cache.getRows(cache.getLookAheadSize() * 2 + 1, 1, null);
        } catch (Exception e) {
            fail("Rows should be accessible without an execution monitor");
        }
    }

    /**
     * Tests setting the cache size to a custom level
     */
    @Test
    public void testSetCacheSize() {
        WindowCacheTable cache = new WindowCacheTable(CONT.getTable());
        assertEquals("Cache size should be default", WindowCacheTable.DEFAULT_CACHE_SIZE, cache.getCacheSize());

        cache.setCacheSize(CUSTOM_CACHE_SIZE);
        assertEquals("Cache size should be custom value", CUSTOM_CACHE_SIZE, cache.getCacheSize());
        assertEquals("Look ahead size should be default", WindowCacheTable.DEFAULT_LOOK_AHEAD,
            cache.getLookAheadSize());

        cache.setCacheSize(WindowCacheTable.DEFAULT_LOOK_AHEAD * 2 - 1);
        assertEquals("Cache size should be minimum of 2 * look ahead size", WindowCacheTable.DEFAULT_LOOK_AHEAD * 2,
            cache.getCacheSize());

        try {
            cache.setCacheSize(0);
            fail("Should not be able to set cache size to 0");
        } catch (IllegalArgumentException e) { /* expected */ }
        try {
            cache.setCacheSize(-1);
            fail("Should not be able to set cache size to negative value");
        } catch (IllegalArgumentException e) { /* expected */ }
    }

    /**
     * Tests setting the look ahead size to a custom value
     */
    @Test
    public void testSetLookAheadSize() {
        WindowCacheTable cache = new WindowCacheTable(CONT.getTable());
        assertEquals("Look ahead size should be default", WindowCacheTable.DEFAULT_LOOK_AHEAD,
            cache.getLookAheadSize());

        cache.setLookAheadSize(CUSTOM_LOOK_AHEAD_SIZE);
        assertEquals("Look ahead size should be custom value", CUSTOM_LOOK_AHEAD_SIZE, cache.getLookAheadSize());
        assertEquals("Cache size should be default", WindowCacheTable.DEFAULT_CACHE_SIZE, cache.getCacheSize());

        cache.setLookAheadSize(cache.getCacheSize() / 2 + 1);
        assertEquals("Look ahead should be a maximum of cache size / 2", cache.getCacheSize() / 2,
            cache.getLookAheadSize());

        try {
            cache.setLookAheadSize(0);
            fail("Should not be able to set look ahead size to 0");
        } catch (IllegalArgumentException e) { /* expected */ }
        try {
            cache.setLookAheadSize(-1);
            fail("Should not be able to set look ahead size to negative value");
        } catch (IllegalArgumentException e) { /* expected */ }
    }

    /**
     * Tests setting the row count to the cache from the outside.
     *
     * @throws UnknownRowCountException
     * @throws CanceledExecutionException
     */
    @Test
    public void testSetRowCount() throws UnknownRowCountException, CanceledExecutionException {
        RestrictedAccessTable restrictedTable = new RestrictedAccessTable(CONT.getTable(), new boolean[] {true, true});
        WindowCacheTable cache = new WindowCacheTable(restrictedTable);
        assertFalse("Cache should have no row count", cache.hasRowCount());
        try {
            cache.getRowCount();
            fail("Row count should not be accessible");
        } catch (UnknownRowCountException e) { /* expected */ }

        // set temporary row count
        cache.setRowCount(NUM_ROWS / 2 - 1, false);
        assertFalse("Cache should have no row count", cache.hasRowCount());
        try {
            cache.getRowCount();
            fail("Non final row count should not be accessible");
        } catch (UnknownRowCountException e) { /* expected */ }
        try {
            cache.getRows(NUM_ROWS / 2 + 1, 1, new ExecutionMonitor());
        } catch (IndexOutOfBoundsException e) {
            fail("Should not throw exception because row count was not final");
        }

        // set final row count
        cache.setRowCount(NUM_ROWS / 2 + cache.getLookAheadSize() + 3, true);
        assertTrue("Cache should have row count", cache.hasRowCount());
        cache.getRowCount(); // should not fail on final count
        try {
            cache.getRows(NUM_ROWS / 2 + cache.getLookAheadSize() + 4, 1, new ExecutionMonitor());
            fail("Should not be able to access rows outside of row count anymore");
        } catch (IndexOutOfBoundsException e) { /* expected */ }

        // set smaller row count than before
        cache.setRowCount(NUM_ROWS / 2, true);
        assertThat("Row count should not be set to smaller value", cache.getRowCount() > NUM_ROWS / 2);
    }

    /**
     * Tests accessing rows in the cache
     * @throws IndexOutOfBoundsException
     * @throws CanceledExecutionException
     */
    @Test
    public void testGetRows() throws IndexOutOfBoundsException, CanceledExecutionException {
        WindowCacheTable cache = new WindowCacheTable(CONT.getTable());
        cache.setCacheSize(CUSTOM_CACHE_SIZE);
        cache.setLookAheadSize(CUSTOM_LOOK_AHEAD_SIZE);
        assertEquals("Cache size should be custom value", CUSTOM_CACHE_SIZE, cache.getCacheSize());
        assertEquals("Look ahead size should be custom value", CUSTOM_LOOK_AHEAD_SIZE, cache.getLookAheadSize());

        // get first row
        int rowsToFetch = 1;
        int offset = 0;
        List<DataRow> rows = cache.getRows(offset, rowsToFetch, new ExecutionMonitor());
        assertEquals("Number of rows returned should be number of rows requested", rowsToFetch, rows.size());
        checkRowEquals(CONT.getTable().iterator().next(), rows.get(0));

        // get chunk
        rowsToFetch = 10;
        rows = cache.getRows(offset, rowsToFetch, new ExecutionMonitor());
        assertEquals("Number of rows returned should be number of rows requested", rowsToFetch, rows.size());
        checkRowEquals(CONT.getTable().iterator().next(), rows.get(0));

        // get chunk with offset
        offset = NUM_ROWS / 2;
        rows = cache.getRows(offset, rowsToFetch, new ExecutionMonitor());
        assertEquals("Number of rows returned should be number of rows requested", rowsToFetch, rows.size());
        RowIterator iterator = CONT.getTable().iterator();
        for (int i = 0; i < offset; i++) {
            iterator.next();
        }
        checkRowEquals(iterator.next(), rows.get(0));

        // get last row
        offset = NUM_ROWS - 1;
        rowsToFetch = 1;

        // get illegal row
        offset = -1;
        try {
            cache.getRows(offset, rowsToFetch, new ExecutionMonitor());
            fail("Should not be able to access rows outside of range");
        } catch (IndexOutOfBoundsException e) { /* expected */ }
        offset = NUM_ROWS;
        try {
            cache.getRows(offset, rowsToFetch, new ExecutionMonitor());
            fail("Should not be able to access rows outside of range");
        } catch (IndexOutOfBoundsException e) { /* expected */ }

        // get no window
        offset = 0;
        rowsToFetch = 0;
        rows = cache.getRows(offset, rowsToFetch, new ExecutionMonitor());
        assertEquals("Number of rows returned should be number of rows requested", rowsToFetch, rows.size());

        // get illegal window
        rowsToFetch = -1;
        try {
            cache.getRows(offset, rowsToFetch, new ExecutionMonitor());
            fail("Should not be able to request window with negative size");
        } catch (IndexOutOfBoundsException e) { /* expected */ }
        rowsToFetch = CUSTOM_CACHE_SIZE - CUSTOM_LOOK_AHEAD_SIZE + 1;
        try {
            cache.getRows(offset, rowsToFetch, new ExecutionMonitor());
            fail("Should not be able to request window larger than possible look back cache size");
        } catch (IndexOutOfBoundsException e) { /* expected */ }

        // get window at end of table
        offset = NUM_ROWS - 5;
        rowsToFetch = 10;
        rows = cache.getRows(offset, rowsToFetch, new ExecutionMonitor());
        assertEquals("Number of rows returned should be number of rows left in table", 5, rows.size());
        iterator = CONT.getTable().iterator();
        for (int i = 0; i < offset; i++) {
            iterator.next();
        }
        checkRowEquals(iterator.next(), rows.get(0));
    }

    private void checkRowEquals(final DataRow expected, final DataRow actual) {
        assertEquals("Rowkeys not identical", expected.getKey(), actual.getKey());
        for (int j = 0; j < expected.getNumCells(); j++) {
            assertEquals("Cells not identical", expected.getCell(j), actual.getCell(j));
        }
    }

    /**
     * Tests the caching strategy (sliding window with look ahead)
     * @throws IndexOutOfBoundsException
     * @throws CanceledExecutionException
     */
    @Test
    public void testCachingStrategy() throws IndexOutOfBoundsException, CanceledExecutionException {
        final int customCacheSize = 2 * CUSTOM_LOOK_AHEAD_SIZE;
        // these flags keep track when the iterator of the table may be accessed (will be changed in this method, first
        // flag) and when the iterator is indeed being accessed (set in the iterator below to true, reset here,
        // second flag)
        final boolean[] flags = new boolean[] {true, false};
        RestrictedAccessTable restrictedTable = new RestrictedAccessTable(CONT.getTable(), flags);
        WindowCacheTable cache = new WindowCacheTable(restrictedTable);

        cache.setLookAheadSize(CUSTOM_LOOK_AHEAD_SIZE);
        cache.setCacheSize(customCacheSize);
        assertEquals("Cache size should be custom value", customCacheSize, cache.getCacheSize());
        assertEquals("Look ahead size should be custom value", CUSTOM_LOOK_AHEAD_SIZE, cache.getLookAheadSize());

        assertTrue(flags[1]);  // init of table uses iterator
        flags[1] = false;
        flags[0] = true;       // allow table access

        cache.getRows(0, 1, new ExecutionMonitor()); // get first row, iterator is used
        assertTrue(flags[1]); // is true when iterator has indeed been used

        // simulate access - for first look ahead size of rows
        // iterator access
        flags[0] = false;
        flags[1] = false;
        for (int i = 0; i < CUSTOM_LOOK_AHEAD_SIZE; i++) { // access rows 0 - 19
            cache.getRows(i, 1, new ExecutionMonitor()); // will throw exception when iterator is accessed
        }
        cache.getRows(0, CUSTOM_LOOK_AHEAD_SIZE, new ExecutionMonitor()); // access whole window
        assertFalse(flags[0]);
        assertFalse(flags[1]);

        flags[0] = true;         // row after "look ahead": update cache!
        cache.getRows(CUSTOM_LOOK_AHEAD_SIZE, 1, new ExecutionMonitor()); // now in cache: row 1 - 40 (release row 0)
        assertTrue(flags[1]);    // iterator has been used

        flags[0] = false;
        flags[1] = false;

        // cache is full
        for (int i = 1; i < customCacheSize; i++) {
            cache.getRows(i, 1, new ExecutionMonitor()); // access look back and look ahead rows in cache
        }
        cache.getRows(1, customCacheSize - CUSTOM_LOOK_AHEAD_SIZE, new ExecutionMonitor()); // access largest window
        assertFalse(flags[0]);
        assertFalse(flags[1]);

        // Simulate arbitrary jumping (20 different positions) in the table and check the cache.
        // The rows in the cache are: [row + look ahead - cache size + 1 : row + look ahead - 1]
        final Random rand = new Random();
        for (int i = 0; i < 20; i++) {
            int row = rand.nextInt(NUM_ROWS); // draw some row to access
            flags[0] = true;
            flags[1] = false;
            cache.getRows(row, 1, new ExecutionMonitor());
            if (!flags[1]) {  // row was (by chance) in cache - no check
                continue;     // cache not changed, we may continue
            }
            flags[0] = false; // disallow access
            flags[1] = false;
            int firstRow = Math.max(0, row + CUSTOM_LOOK_AHEAD_SIZE - customCacheSize + 1);
            int lastRow = Math.min(row + CUSTOM_LOOK_AHEAD_SIZE, NUM_ROWS);
            for (int r = firstRow; r < lastRow; r++) {
                cache.getRows(r, 1, new ExecutionMonitor());
            }
            cache.getRows(firstRow, Math.max(1, (lastRow - firstRow) / 2), new ExecutionMonitor()); // access to window
            assertFalse(flags[0]);
            assertFalse(flags[1]);
        }
    }

    /**
     * Wrapper table for an arbitrary {@link DataTable}, which is accessed via a {@link RestrictedAccessIterator}.
     */
    private static class RestrictedAccessTable implements DataTable {

        private final DataTable m_table;
        private final boolean[] m_flags;

        private RestrictedAccessTable(final DataTable table, final boolean[] flags) {
            m_table = table;
            m_flags = flags;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataTableSpec getDataTableSpec() {
            return m_table.getDataTableSpec();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public RowIterator iterator() {
            return new RestrictedAccessIterator(m_table, m_flags);
        }

    }

    /**
     * Iterator that throws exception when <code>next()</code> method is called
     * at an inappropriate time.
     */
    private static class RestrictedAccessIterator extends DefaultRowIterator {

        // flags passed in constructor
        private final boolean[] m_flags;

        /**
         * Constructs new Iterator based on <code>table</code> and the access
         * policy encoded in <code>flags</code>.
         *
         * @param table to iterate over.
         * @param flags two-dimensional array, first flag is set remotely and is
         *            <code>true</code> when <code>next()</code> may be
         *            called. Second flag is set to <code>true</code> by the
         *            <code>next()</code> method when it is called.
         */
        private RestrictedAccessIterator(final Iterable<DataRow> table, final boolean[] flags) {
            super(table);
            m_flags = flags;
        }


        /**
         * Pushes iterator forward. If first flag is set to <code>true</code> or
         * throws an exception when it is set to <code>false</code>
         *
         * @return next row in the table
         * @see RowIterator#next()
         * @throws IllegalStateException if iterator is disabled
         */
        @Override
        public DataRow next() {
            if (!m_flags[0]) {
                throw new IllegalStateException(
                        "Iterator should not have been called at the current "
                        + "state, all rows are supposedly cached");
            }
            m_flags[1] = true;
            return super.next();
        }
    }

}
