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
 *   17.03.2015 (thor): created
 */
package org.knime.core.data.util.memory;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.lang.ref.SoftReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.NodeLogger;

/**
 * Testcase for {@link MemoryAlertSystem}.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
public class MemoryAlertSystemTest {
    private MemoryAlertSystem m_memSystem;

    /**
     * Checks that enough memory is available before each test.
     *
     * @throws Exception if an error occurs
     */
    @Before
    public void checkAvailableMemory() throws Exception {
        forceGC();
        for (int i = 0; i < 10 && MemoryAlertSystem.getInstance().isMemoryLow(); i++) {
            forceGC();
            Thread.sleep(1000);
        }
        assertThat("Cannot test because memory usage is already above threshold: " + MemoryAlertSystem.getUsage(),
            MemoryAlertSystem.getInstance().isMemoryLow(), is(false));
        m_memSystem = MemoryAlertSystem.getInstance();
        NodeLogger.getLogger(getClass()).debug(
            "Memory usage: " + MemoryAlertSystem.getUsedMemory() + "/" + MemoryAlertSystem.getMaximumMemory() + " => "
                + MemoryAlertSystem.getUsage());
        forceGC();
    }

    /**
     * Checks whether {@link MemoryAlertSystem#sleepWhileLow(double, long)} works as expected.
     *
     * @throws Exception if an error occurs
     */
    @Test(timeout = 20000)
    public void testSleepWhileLow() throws Exception {
        int reserveSize = (int)(MemoryAlertSystem.DEFAULT_USAGE_THRESHOLD
            * (MemoryAlertSystem.getMaximumMemory() - MemoryAlertSystem.getUsedMemory())) + (32 << 20);

        // we should return immediately because enough memory is available
        boolean memoryAvailable = m_memSystem.sleepWhileLow(MemoryAlertSystem.DEFAULT_USAGE_THRESHOLD, 1000);
        assertThat("Was sleeping although memory is below threshold: " + MemoryAlertSystem.getUsage(), memoryAvailable,
            is(true));

        // allocate memory
        final AtomicReference<byte[]> buffer = new AtomicReference<byte[]>(new byte[reserveSize]);
        // force buffer into tenured space
        forceGC();

        Thread.sleep(1000);
        // we should return after 1 seconds
        memoryAvailable = m_memSystem.sleepWhileLow(MemoryAlertSystem.DEFAULT_USAGE_THRESHOLD, 1000);
        assertThat("Was not sleeping although memory usage is above threshold: " + MemoryAlertSystem.getUsage(),
            memoryAvailable, is(false));

        Thread clearThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    buffer.set(null);
                    NodeLogger.getLogger(getClass()).debug("Cleared buffer, memory should be freed now");
                    forceGC();
                } catch (Exception ex) {
                    // ignore
                }
            }
        });
        clearThread.start();

        NodeLogger.getLogger(getClass()).debug(
            "Going to sleep, memory usage is " + MemoryAlertSystem.getUsage());
        m_memSystem.sleepWhileLow(MemoryAlertSystem.DEFAULT_USAGE_THRESHOLD, 25000);
        // should return quite fast and not time out the test method
        clearThread.interrupt();
        clearThread.join();
    }

    /**
     * Checks whether listeners are notified correctly.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testListener() throws Exception {
        int reserveSize = (int)(MemoryAlertSystem.DEFAULT_USAGE_THRESHOLD
            * (MemoryAlertSystem.getMaximumMemory() - MemoryAlertSystem.getUsedMemory()));

        final AtomicBoolean listenerCalled = new AtomicBoolean();
        MemoryAlertListener listener = new MemoryAlertListener() {
            @Override
            protected boolean memoryAlert(final MemoryAlert alert) {
                NodeLogger.getLogger(MemoryAlertSystemTest.class).debug(
                    "Memory listener called, current usage is " + MemoryAlertSystem.getUsage());
                listenerCalled.set(true);
                return false;
            }
        };

        m_memSystem.addListener(listener);
        try {
            forceGC();
            Thread.sleep(1000);
            assertThat("Alert listener called although usage is below threshold: " + MemoryAlertSystem.getUsage(),
                listenerCalled.get(), is(false));

            @SuppressWarnings("unused")
            byte[] buf = new byte[reserveSize];
            forceGC();
            Thread.sleep(2000);
            assertThat("Alert listener not called although usage is above threshold: " + MemoryAlertSystem.getUsage(),
                listenerCalled.get(), is(true));
        } finally {
            m_memSystem.removeListener(listener);
        }
    }

    /**
     * Checks whether listeners are removed automatically.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testAutoRemoveListener() throws Exception {
        int reserveSize = (int)(MemoryAlertSystem.DEFAULT_USAGE_THRESHOLD
            * (MemoryAlertSystem.getMaximumMemory() - MemoryAlertSystem.getUsedMemory()));

        final AtomicBoolean listenerCalled = new AtomicBoolean();
        MemoryAlertListener listener = new MemoryAlertListener() {
            @Override
            protected boolean memoryAlert(final MemoryAlert alert) {
                listenerCalled.set(true);
                return true;
            }
        };

        m_memSystem.addListener(listener);
        try {
            forceGC();
            Thread.sleep(1000);
            assertThat("Alert listener called although usage is below threshold: " + MemoryAlertSystem.getUsage(),
                listenerCalled.get(), is(false));

            @SuppressWarnings("unused")
            byte[] buf = new byte[reserveSize];
            forceGC();
            Thread.sleep(1000);
            assertThat("Alert listener not called although usage is above threshold: " + MemoryAlertSystem.getUsage(),
                listenerCalled.getAndSet(false), is(true));

            boolean removed = m_memSystem.removeListener(listener);
            assertThat("Listener was not removed automatically", removed, is(false));
        } finally {
            m_memSystem.removeListener(listener);
        }
    }

    /**
     * Forces a GC run. By using soft reference {@link System#gc()} is called until the soft reference has been cleared.
     *
     * @throws InterruptedException if the thread is interrupted
     */
    public static void forceGC() throws InterruptedException {
        Object obj1 = new Object();
        SoftReference<Object> ref1 = new SoftReference<>(obj1);
        Object obj2 = new Object();
        SoftReference<Object> ref2 = new SoftReference<>(obj2);
        obj1 = null;
        int max = 10;
        while ((ref1.get() != null) && (max-- > 0) && !Thread.currentThread().isInterrupted()) {
            System.gc();
            Thread.sleep(50);
        }

        obj2 = null;
        max = 10;
        while ((ref2.get() != null) && (max-- > 0) && !Thread.currentThread().isInterrupted()) {
            System.gc();
            Thread.sleep(50);
        }

        NodeLogger.getLogger(MemoryAlertSystemTest.class).debug(
            "Called System.gc, memory usage is now " + MemoryAlertSystem.getUsage());
    }
}
