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
 *   3 Apr 2020 ("Marc Bux, KNIME GmbH, Berlin, Germany"): created
 */
package org.knime.core.data.container;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Test;
import org.knime.core.data.DataRow;
import org.knime.core.data.util.memory.MemoryAlertSystem;
import org.knime.core.data.util.memory.MemoryAlertSystemTest;
import org.knime.core.node.KNIMEConstants;

/**
 * Test cases for the class {@link Buffer}.
 *
 * @author "Marc Bux, KNIME GmbH, Berlin, Germany"
 */
public class BufferTest {

    /**
     * Test that even medium-sized tables (larger then the container's maximum number of cells) are kept in memory. Also
     * test that once the table has been evicted from memory, it is read back into memory on next iteration.
     *
     * @throws InterruptedException thrown when the thread is unexpectedly interrupted during sleep.
     */
    @Test(timeout = 2000)
    public void testMediumSizedTables() throws InterruptedException {
        // invoke GC to free up memory
        MemoryAlertSystemTest.forceGC();

        // generate a medium-sized table and check that it is held in memory
        final Buffer buffer = generateMediumSizedTable();
        Assert.assertTrue("Recently generated medium-sized table not held in memory.", buffer.isHeldInMemory());

        // generate more medium-sized tables that should eventually evict the first table from the LRU cache
        for (int i = 0; i < BufferSettings.getDefault().getLRUCacheSize(); i++) {
            generateMediumSizedTable();
        }

        // once evicted from the LRU cache, the table should only be weakly referenced and can be garbage-collected
        while (buffer.isHeldInMemory()) {
            // invoke garbage collection and hope that it collects weakly referenced tables
            MemoryAlertSystemTest.forceGC();
        }

        // we should check that now that the table is no longer held in memory, it has actually been written to a file
        Assert.assertTrue("Medium-sized table dropped from memory but not written to disk.", buffer.isFlushedToDisk());

        // finally, we iterate over the table and make sure that it has been read back into memory
        try (final CloseableRowIterator it = buffer.iterator();) {
            while (it.hasNext()) {
                it.next();
            }
        }
        Assert.assertTrue("Medium-sized table not read back into memory from disk.", buffer.isHeldInMemory());
        Assert.assertTrue("Previously flushed medium-sized table not flushed any more.", buffer.isFlushedToDisk());
    }

    /**
     * Tests that when a memory alert is thrown while an in-memory table is being iterated over, that table is
     * garbage-collected and iteration continues by reading the flushed table from disk.
     *
     * @throws InterruptedException thrown when the thread is unexpectedly interrupted during sleep
     */
    @Test(timeout = 2000)
    public void testTableDroppedWhileIteratedOver() throws InterruptedException {
        // generate a table, put it into the cache and wait for it being flushed to disk
        final Buffer table = generateMediumSizedTable();
        while (!table.isFlushedToDisk()) {
            Thread.sleep(10);
        }

        final DataRow refRow = table.iterator().next();
        try (final CloseableRowIterator iterator = table.iterator()) {

            // send a memory alert and wait for the table to be garbage-collected
            BufferTest.waitForBufferToBeCollected(table);

            // check that despite being garbage collected, the table can still be iterated over
            final DataRow row = iterator.next();
            Assert.assertEquals("Row key in row ", row.getKey(), refRow.getKey());
            for (int j = 0; j < refRow.getNumCells(); j++) {
                Assert.assertEquals("Cell " + j + " in Row ", refRow.getCell(j), row.getCell(j));
            }
        }
    }

    /**
     * Generate a small-sized table. Medium-sized means smaller than a container's maximum number of cells.
     *
     * @return a small-sized table
     */
    static Buffer generateSmallSizedTable() {
        return DataContainerTest.generateSmallSizedTable().getBuffer();
    }

    /**
     * Generate a medium-sized table. Medium-sized means larger than a container's maximum number of cells, but smaller
     * than Java heap space.
     *
     * @return a medium-sized table
     */
    static Buffer generateMediumSizedTable() {
        return DataContainerTest.generateMediumSizedTable().getBuffer();
    }

    /**
     * Wait for all asynchronous disk write threads to terminate.
     *
     * @param buffer the to-be-flushed buffer
     *
     * @throws InterruptedException thrown when the thread is unexpectedly interrupted during sleep
     */
    static void waitForBufferToBeFlushed(final Buffer buffer) throws InterruptedException {
        Future<?> voidTask = KNIMEConstants.IO_EXECUTOR.submit(() -> {
        });
        try {
            voidTask.get();
        } catch (ExecutionException e) {
            // the void task should not be able to throw an ExecutionExecption
        }
        Assert.assertTrue("Buffer has not been flushed to disk.", buffer.isFlushedToDisk());
    }

    /**
     * Send a memory alert and wait for the table to be garbage-collected.
     *
     * @param buffer the to-be-garbage-collected buffer
     *
     * @throws InterruptedException thrown when the thread is unexpectedly interrupted during sleep
     */
    static void waitForBufferToBeCollected(final Buffer buffer) throws InterruptedException {
        MemoryAlertSystem.getInstanceUncollected().sendMemoryAlert();
        MemoryAlertSystemTest.forceGC();
        while (buffer.isHeldInMemory()) {
            MemoryAlertSystemTest.forceGC();
        }
    }

}
