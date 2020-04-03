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

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.knime.core.data.util.memory.MemoryAlertSystem;
import org.knime.core.data.util.memory.MemoryAlertSystemTest;

/**
 * Test cases for the class {@link BufferResource}.
 *
 * @author "Marc Bux, KNIME GmbH, Berlin, Germany"
 */
public class BufferResourceTest {

    static {
        // init the BufferCache such that it does not interfere with our measured number of memory alert listeners
        DataContainerTest.generateSmallSizedTable();
    }

    /**
     * Tests that when a small buffer is created, a MemoryAlertListener of type BufferFlusher is registered, which is
     * then unregistered when the Buffer is cleared.
     *
     * @throws InterruptedException thrown when the thread is unexpectedly interrupted during sleep
     */
    @Test(timeout = 5000)
    public void testNoDanglingBufferFlusherOnClear() throws InterruptedException {
        // invoke GC to free up memory
        MemoryAlertSystemTest.forceGC();
        final long numberOfListenersAtOnset = MemoryAlertSystem.getNumberOfListeners();

        // create a small-sized table, which registers a MemoryAlertListener of type BufferFlusher
        final Buffer smallSizedTable = BufferTest.generateSmallSizedTable();
        checkNumberOfRegisteredMemoryAlertListeners(numberOfListenersAtOnset + 1); // BufferFlusher

        // clear the small-sized table, which should unregister the BufferFlusher
        smallSizedTable.clear();
        checkNumberOfRegisteredMemoryAlertListeners(numberOfListenersAtOnset);
    }

    /**
     * Tests that when small or medium-sized buffers are iterated back into memory and these iterators are closed, no
     * memory alert listeners remain unregistered.
     *
     * @throws InterruptedException thrown when the thread is unexpectedly interrupted during sleep
     */
    @Test(timeout = 5000)
    public void testNoDanglingBackIntoMemoryIteratorDropperOnIteratorClose() throws InterruptedException {
        // invoke GC to free up memory
        MemoryAlertSystemTest.forceGC();
        final long numberOfListenersAtOnset = MemoryAlertSystem.getNumberOfListeners();

        final Buffer smallSizedTable = BufferTest.generateSmallSizedTable();
        final Buffer mediumSizedTable = BufferTest.generateMediumSizedTable();
        checkNumberOfOpenResources(smallSizedTable, 0);
        checkNumberOfOpenResources(mediumSizedTable, 0);
        checkNumberOfRegisteredMemoryAlertListeners(numberOfListenersAtOnset + 1); // BufferFlusher

        // send memory alert and wait for these tables to be collected
        BufferTest.waitForBufferToBeCollected(smallSizedTable);
        BufferTest.waitForBufferToBeCollected(mediumSizedTable);

        // the BufferFlusher associated with the smallSizedTable should have been unregistered
        checkNumberOfOpenResources(smallSizedTable, 0);
        checkNumberOfOpenResources(mediumSizedTable, 0);
        checkNumberOfRegisteredMemoryAlertListeners(numberOfListenersAtOnset);

        // create iterators, which should iterate the table back into memory, which, in turn, should lead to two
        // MemoryAlertListeners of type BackIntoMemoryIteratorDropper being registered
        try (final CloseableRowIterator smallIt = smallSizedTable.iterator();
                final CloseableRowIterator mediumIt = mediumSizedTable.iterator()) {
            checkNumberOfOpenResources(smallSizedTable, 2); // TSCloseableRowIterator, BackIntoMemoryIteratorDropper
            checkNumberOfOpenResources(mediumSizedTable, 2); // TSCloseableRowIterator, BackIntoMemoryIteratorDropper
            checkNumberOfRegisteredMemoryAlertListeners(numberOfListenersAtOnset + 2);
        }

        // once the iterators have been closed, its underlying memory alert listeners should have also been unregistered
        checkNumberOfOpenResources(smallSizedTable, 1);
        checkNumberOfOpenResources(mediumSizedTable, 1);
        checkNumberOfRegisteredMemoryAlertListeners(numberOfListenersAtOnset);
    }

    /**
     * Tests that when small or medium-sized buffers are iterated back into memory and the buffers cleared, no memory
     * alert listeners remain unregistered.
     *
     * @throws InterruptedException thrown when the thread is unexpectedly interrupted during sleep
     */
    @Test(timeout = 5000)
    public void testNoDanglingBackIntoMemoryIteratorDropperOnClear() throws InterruptedException {
        // invoke GC to free up memory
        MemoryAlertSystemTest.forceGC();
        final long numberOfListenersAtOnset = MemoryAlertSystem.getNumberOfListeners();

        final Buffer smallSizedTable = BufferTest.generateSmallSizedTable();
        final Buffer mediumSizedTable = BufferTest.generateMediumSizedTable();
        checkNumberOfOpenResources(smallSizedTable, 0);
        checkNumberOfOpenResources(mediumSizedTable, 0);
        checkNumberOfRegisteredMemoryAlertListeners(numberOfListenersAtOnset + 1); // BufferFlusher

        // send memory alert and wait for these tables to be collected
        BufferTest.waitForBufferToBeCollected(smallSizedTable);
        BufferTest.waitForBufferToBeCollected(mediumSizedTable);

        // the BufferFlusher associated with the smallSizedTable should have been unregistered
        checkNumberOfOpenResources(smallSizedTable, 0);
        checkNumberOfOpenResources(mediumSizedTable, 0);
        checkNumberOfRegisteredMemoryAlertListeners(numberOfListenersAtOnset);

        // create iterators, which should iterate the table back into memory, which, in turn, should lead to two
        // MemoryAlertListeners of type BackIntoMemoryIteratorDropper being registered
        try (final CloseableRowIterator smallIt = smallSizedTable.iterator();
                final CloseableRowIterator mediumIt = mediumSizedTable.iterator()) {
            checkNumberOfOpenResources(smallSizedTable, 2); // TSCloseableRowIterator, BackIntoMemoryIteratorDropper
            checkNumberOfOpenResources(mediumSizedTable, 2); // TSCloseableRowIterator, BackIntoMemoryIteratorDropper
            checkNumberOfRegisteredMemoryAlertListeners(numberOfListenersAtOnset + 2);

            // clear the buffers and make sure that its underlying memory alert listeners have also been unregistered
            smallSizedTable.clear();
            mediumSizedTable.clear();
            checkNumberOfOpenResources(smallSizedTable, 0);
            checkNumberOfOpenResources(mediumSizedTable, 0);
            checkNumberOfRegisteredMemoryAlertListeners(numberOfListenersAtOnset);
        }
    }

    /**
     * Tests that when small or medium-sized buffers are iterated back into memory and these iterators are
     * garbage-collected, no memory alert listeners remain unregistered.
     *
     * @throws InterruptedException thrown when the thread is unexpectedly interrupted during sleep
     */
    @SuppressWarnings("resource") // we explicitly want to test that we catch resource leaks
    @Test(timeout = 5000)
    public void testNoDanglingBackIntoMemoryIteratorDropperOnIteratorFinalize() throws InterruptedException {
        // invoke GC to free up memory
        MemoryAlertSystemTest.forceGC();
        final long numberOfListenersAtOnset = MemoryAlertSystem.getNumberOfListeners();

        final Buffer smallSizedTable = BufferTest.generateSmallSizedTable();
        final Buffer mediumSizedTable = BufferTest.generateMediumSizedTable();
        checkNumberOfOpenResources(smallSizedTable, 0);
        checkNumberOfOpenResources(mediumSizedTable, 0);
        checkNumberOfRegisteredMemoryAlertListeners(numberOfListenersAtOnset + 1); // BufferFlusher

        // send memory alert and wait for these tables to be collected
        BufferTest.waitForBufferToBeCollected(smallSizedTable);
        BufferTest.waitForBufferToBeCollected(mediumSizedTable);

        // the BufferFlusher associated with the smallSizedTable should have been unregistered
        checkNumberOfOpenResources(smallSizedTable, 0);
        checkNumberOfOpenResources(mediumSizedTable, 0);
        checkNumberOfRegisteredMemoryAlertListeners(numberOfListenersAtOnset);

        // create iterators, which should iterate the table back into memory, which, in turn, should lead to two
        // MemoryAlertListeners of type BackIntoMemoryIteratorDropper being registered
        CloseableRowIterator smallIt = smallSizedTable.iterator();
        CloseableRowIterator mediumIt = mediumSizedTable.iterator();
        checkNumberOfOpenResources(smallSizedTable, 2); // TSCloseableRowIterator, BackIntoMemoryIteratorDropper
        checkNumberOfOpenResources(mediumSizedTable, 2); // TSCloseableRowIterator, BackIntoMemoryIteratorDropper
        checkNumberOfRegisteredMemoryAlertListeners(numberOfListenersAtOnset + 2);

        // do not clear buffers; do not close iterators; wait for iterators to be garbage-collected
        final WeakReference<?>[] refs =
            Stream.of(smallIt, mediumIt).map(WeakReference::new).toArray(WeakReference<?>[]::new);
        smallIt = null;
        mediumIt = null;
        while (Arrays.stream(refs).map(WeakReference::get).anyMatch(Objects::nonNull)) {
            MemoryAlertSystemTest.forceGC();
        }

        // when the iterators were garbage-collected, their held resources became stale and were released
        checkNumberOfOpenResources(smallSizedTable, 0);
        checkNumberOfOpenResources(mediumSizedTable, 0);
        checkNumberOfRegisteredMemoryAlertListeners(numberOfListenersAtOnset);
    }

    /**
     * Tests that when a medium-sized buffer is iterated back into memory, the file input stream is closed when the
     * buffer is cleared.
     *
     * @throws InterruptedException thrown when the thread is unexpectedly interrupted during sleep
     */
    @Test(timeout = 5000)
    public void testNoDanglingBackIntoMemoryIteratorOnClear() throws InterruptedException {
        // invoke GC to free up memory
        MemoryAlertSystemTest.forceGC();

        final Buffer mediumSizedTable = BufferTest.generateMediumSizedTable();
        checkNumberOfOpenResources(mediumSizedTable, 0);
        checkNumberOfOpenInputStreams(mediumSizedTable, 0);

        // send memory alert and wait for table to be collected
        BufferTest.waitForBufferToBeCollected(mediumSizedTable);
        checkNumberOfOpenResources(mediumSizedTable, 0);
        checkNumberOfOpenInputStreams(mediumSizedTable, 0);

        // create two iterators, which should iterate the table back into memory using a shared input stream
        try (final CloseableRowIterator it1 = mediumSizedTable.iterator();
                final CloseableRowIterator it2 = mediumSizedTable.iterator()) {
            checkNumberOfOpenResources(mediumSizedTable, 3); // TSCloseableRowIterator, 2 BackIntoMemoryIteratorDropper
            checkNumberOfOpenInputStreams(mediumSizedTable, 1);

            // clear the buffer and make sure that its underlying TSCloseableRowIterator was also closed
            mediumSizedTable.clear();
            checkNumberOfOpenResources(mediumSizedTable, 0);
            checkNumberOfOpenInputStreams(mediumSizedTable, 0);
        }
    }

    /**
     * Tests that when a medium-sized buffer is iterated back into memory and these iterators are garbage-collected, the
     * file input stream is closed.
     *
     * @throws InterruptedException thrown when the thread is unexpectedly interrupted during sleep
     */
    @Test(timeout = 5000)
    @SuppressWarnings("resource") // we explicitly want to test that we catch resource leaks
    public void testNoDanglingBackIntoMemoryIteratorOnIteratorFinalize() throws InterruptedException {
        // invoke GC to free up memory
        MemoryAlertSystemTest.forceGC();

        final Buffer mediumSizedTable = BufferTest.generateMediumSizedTable();
        checkNumberOfOpenResources(mediumSizedTable, 0);
        checkNumberOfOpenInputStreams(mediumSizedTable, 0);

        // send memory alert and wait for table to be collected
        BufferTest.waitForBufferToBeCollected(mediumSizedTable);
        checkNumberOfOpenResources(mediumSizedTable, 0);
        checkNumberOfOpenInputStreams(mediumSizedTable, 0);

        // create two iterators, which should iterate the table back into memory using a shared input stream
        CloseableRowIterator it1 = mediumSizedTable.iterator();
        CloseableRowIterator it2 = mediumSizedTable.iterator();
        checkNumberOfOpenResources(mediumSizedTable, 3); // TSCloseableRowIterator, 2 BackIntoMemoryIteratorDropper
        checkNumberOfOpenInputStreams(mediumSizedTable, 1);

        // do not clear buffer; do not close iterators; wait for the iterators to be garbage-collected
        final WeakReference<?>[] refs = Stream.of(it1, it2).map(WeakReference::new).toArray(WeakReference<?>[]::new);
        it1 = null;
        it2 = null;
        while (Arrays.stream(refs).map(WeakReference::get).anyMatch(Objects::nonNull)) {
            MemoryAlertSystemTest.forceGC();
        }

        // when the iterators were garbage-collected, their held resources became stale and were released
        checkNumberOfOpenResources(mediumSizedTable, 0);
        checkNumberOfOpenInputStreams(mediumSizedTable, 0);
    }

    /**
     * Tests that when a FromListFallBackFromFileIterator falls back to iterate from file and that iterator is closed,
     * the file input stream is closed as well.
     *
     * @throws InterruptedException thrown when the thread is unexpectedly interrupted during sleep
     */
    @Test(timeout = 5000)
    public void testNoDanglingFallBackFromFileIteratorOnIteratorClose() throws InterruptedException {
        // invoke GC to free up memory
        MemoryAlertSystemTest.forceGC();

        // create buffer
        final Buffer mediumSizedTable = BufferTest.generateMediumSizedTable();
        checkNumberOfOpenResources(mediumSizedTable, 0);
        checkNumberOfOpenInputStreams(mediumSizedTable, 0);

        // wait for buffer to be flushed to disk
        BufferTest.waitForBufferToBeFlushed(mediumSizedTable);
        checkNumberOfOpenResources(mediumSizedTable, 0);
        checkNumberOfOpenInputStreams(mediumSizedTable, 0);

        // create iterator and iterate for a bit, then send a memory alert and force a garbage collection
        try (final CloseableRowIterator it = mediumSizedTable.iterator()) {
            it.next();
            BufferTest.waitForBufferToBeCollected(mediumSizedTable);
            checkNumberOfOpenResources(mediumSizedTable, 0);
            checkNumberOfOpenInputStreams(mediumSizedTable, 0);

            // GC of in-memory table should have iterator fall back to reading from disk on the next iteration
            it.next();
            checkNumberOfOpenResources(mediumSizedTable, 1); // TSCloseableRowIterator
            checkNumberOfOpenInputStreams(mediumSizedTable, 1);
        }

        // once the iterator has been closed, its underlying TSCloseableRowIterator should have also been closed
        checkNumberOfOpenResources(mediumSizedTable, 0);
        checkNumberOfOpenInputStreams(mediumSizedTable, 0);
    }

    /**
     * Tests that when a FromListFallBackFromFileIterator falls back to iterate from file and the owning Buffer is
     * cleared, the file input stream is closed as well.
     *
     * @throws InterruptedException thrown when the thread is unexpectedly interrupted during sleep
     */
    @Test(timeout = 5000)
    public void testNoDanglingFallBackFromFileIteratorOnBufferClear() throws InterruptedException {
        // invoke GC to free up memory
        MemoryAlertSystemTest.forceGC();

        // create buffer
        final Buffer mediumSizedTable = BufferTest.generateMediumSizedTable();
        checkNumberOfOpenResources(mediumSizedTable, 0);
        checkNumberOfOpenInputStreams(mediumSizedTable, 0);

        // wait for buffer to be flushed to disk
        BufferTest.waitForBufferToBeFlushed(mediumSizedTable);
        checkNumberOfOpenResources(mediumSizedTable, 0);
        checkNumberOfOpenInputStreams(mediumSizedTable, 0);

        // create iterator and iterate for a bit, then send a memory alert and force a garbage collection
        try (final CloseableRowIterator it = mediumSizedTable.iterator()) {
            it.next();
            BufferTest.waitForBufferToBeCollected(mediumSizedTable);
            checkNumberOfOpenInputStreams(mediumSizedTable, 0);
            checkNumberOfOpenResources(mediumSizedTable, 0);

            // GC of in-memory table should have iterator fall back to reading from disk on the next iteration
            it.next();
            checkNumberOfOpenInputStreams(mediumSizedTable, 1); // TSCloseableRowIterator
            checkNumberOfOpenResources(mediumSizedTable, 1);

            // clear the buffer and make sure that its underlying TSCloseableRowIterator was also closed
            mediumSizedTable.clear();
            checkNumberOfOpenInputStreams(mediumSizedTable, 0);
            checkNumberOfOpenResources(mediumSizedTable, 0);
        }
    }

    /**
     * Tests that when a FromListFallBackFromFileIterator falls back to iterate from file and that iterator is
     * garbage-collected, the file input stream is closed as well.
     *
     * Tests that a FromListFallBackFromFileIterator falls back to iterate from file and that input stream is closed
     * when the owning uncleared Buffer is finalized.
     *
     * @throws InterruptedException thrown when the thread is unexpectedly interrupted during sleep
     */
    @Test(timeout = 5000)
    @SuppressWarnings("resource") // we explicitly want to test that we catch resource leaks
    public void testNoDanglingFallBackFromFileIteratorOnIteratorFinalize() throws InterruptedException {
        // invoke GC to free up memory
        MemoryAlertSystemTest.forceGC();

        // create buffer
        final Buffer mediumSizedTable = BufferTest.generateMediumSizedTable();
        checkNumberOfOpenResources(mediumSizedTable, 0);
        checkNumberOfOpenInputStreams(mediumSizedTable, 0);

        // wait for buffer to be flushed to disk
        BufferTest.waitForBufferToBeFlushed(mediumSizedTable);
        checkNumberOfOpenResources(mediumSizedTable, 0);
        checkNumberOfOpenInputStreams(mediumSizedTable, 0);

        // create iterator and iterate for a bit, then send a memory alert and force a garbage collection
        CloseableRowIterator it = mediumSizedTable.iterator();
        it.next();
        BufferTest.waitForBufferToBeCollected(mediumSizedTable);
        checkNumberOfOpenResources(mediumSizedTable, 0);
        checkNumberOfOpenInputStreams(mediumSizedTable, 0);

        // GC of in-memory table should have iterator fall back to reading from disk on the next iteration
        it.next();
        checkNumberOfOpenResources(mediumSizedTable, 1); // TSCloseableRowIterator
        checkNumberOfOpenInputStreams(mediumSizedTable, 1);

        // do not clear buffer; do not close iterator; wait for the iterator to be garbage collected
        final WeakReference<CloseableRowIterator> ref = new WeakReference<>(it);
        it = null;
        while (ref.get() != null) {
            MemoryAlertSystemTest.forceGC();
        }

        // when the iterator was garbage-collected, the TSCloseableRowIterator should have also been closed
        checkNumberOfOpenResources(mediumSizedTable, 0);
        checkNumberOfOpenInputStreams(mediumSizedTable, 0);
    }

    private static void checkNumberOfOpenResources(final Buffer buffer, final int expected) {
        final int actual = buffer.getNrOpenResources();
        Assert.assertEquals(String
            .format("Number of open resources owned by iterators on buffer should be %d but is %d.", expected, actual),
            expected, actual);
    }

    private static void checkNumberOfRegisteredMemoryAlertListeners(final long expected) {
        final long actual = MemoryAlertSystem.getNumberOfListeners();
        Assert.assertEquals(
            String.format("Number of registered memory alert listeners should be %d but is %d.", expected, actual),
            expected, actual);
    }

    private static void checkNumberOfOpenInputStreams(final Buffer buffer, final int expected) {
        final int actual = buffer.getNrOpenInputStreams();
        Assert.assertEquals(
            String.format("Number of open input streams on buffer should be %d but is %d.", expected, actual), expected,
            actual);
    }

}
