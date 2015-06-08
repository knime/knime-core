/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.data.util.memory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryPoolMXBean;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;

import org.knime.core.node.NodeLogger;

/**
 * No public API yet.
 *
 * @author dietzc
 * @deprecated used {@link MemoryAlertSystem} instead
 */
@Deprecated
public final class MemoryWarningSystem {

    private static MemoryWarningSystem m_instance = null;

    /* Standard Logger */
    private final NodeLogger LOGGER = NodeLogger.getLogger(MemoryWarningSystem.class);

    /**
     * Listener to be notified on warning events
     */
    public interface MemoryWarningListener {
        /**
         * @param usedMemory
         * @param maxMemory
         */
        public void memoryUsageLow(long usedMemory, long maxMemory);
    }

    private final Set<MemoryWarningListener> listeners = new HashSet<MemoryWarningListener>();

    private final MemoryPoolMXBean m_memPool = findTenuredGenPool();

    /**
     * Singleton here?
     */
    private MemoryWarningSystem() {

        final long maxMem = computeMaxMem();

        MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
        NotificationEmitter emitter = (NotificationEmitter)mbean;

        emitter.addNotificationListener(new NotificationListener() {
            @Override
            public void handleNotification(final Notification n, final Object hb) {
                if (n.getType().equals(MemoryNotificationInfo.MEMORY_COLLECTION_THRESHOLD_EXCEEDED)) {
                    long computeUsedMem = computeUsedMem();
                    synchronized (m_instance) {
                        for (MemoryWarningListener listener : listeners) {
                            listener.memoryUsageLow(computeUsedMem, maxMem);
                        }
                    }
                }
            }
        }, null, null);
    }

    /**
     * Register Listener
     *
     * @param listener
     * @return
     */
    public synchronized boolean registerListener(final MemoryWarningListener listener) {
        return listeners.add(listener);
    }

    /**
     * Remove registered Listener
     *
     * @param listener
     * @return
     */
    public synchronized boolean removeListener(final MemoryWarningListener listener) {
        return listeners.remove(listener);
    }

    /**
     * Set percentage level of the amount of memory in tenured space which may be set before a memory warning event is
     * thrown
     *
     * @param percentage
     */
    public void setPercentageUsageThreshold(final double percentage) {
        if (percentage <= 0.0 || percentage > 1.0) {
            throw new IllegalArgumentException("Percentage not in range");
        }

        long warningThreshold = (long)(computeMaxMem() * percentage);

        m_memPool.setCollectionUsageThreshold(warningThreshold);
    }

    private long computeUsedMem() {
        return null != m_memPool ? m_memPool.getUsage().getUsed() : Runtime.getRuntime().totalMemory()
                - Runtime.getRuntime().freeMemory();
    }

    private long computeMaxMem() {
        // Compute the threshold in bytes
        long maxMem = null != m_memPool ? m_memPool.getUsage().getMax() : Runtime.getRuntime().maxMemory();

        return maxMem;
    }

    /**
     * Tenured Space Pool can be determined by it being of type HEAP and by it being possible to set the usage
     * threshold.
     */
    private MemoryPoolMXBean findTenuredGenPool() {
        List<String> asList = Arrays.asList("Tenured Gen", "PS Old Gen", "CMS Old Gen", "G1 Old Gen");

        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            // I don't know whether this approach is better, or
            // whether
            // we should rather check for the pool name
            // "Tenured Gen"?
            if (asList.contains(pool.getName()) && pool.isUsageThresholdSupported()) {
                return pool;
            }
        }
        throw new AssertionError("Could not find tenured space");
    }

    /**
     * Singleton on MemoryObjectTracker
     */
    public static MemoryWarningSystem getInstance() {
        if (m_instance == null) {
            m_instance = new MemoryWarningSystem();
        }
        return m_instance;
    }

}