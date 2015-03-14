/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   11.03.2015 (thor): created
 */
package org.knime.core.data.util.memory;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContext;

/**
 * Implementation of a simple memory warning system. You can either register a listener via
 * {@link #addListener(MemoryAlertListener)} that get notified if free memory gets low. This is useful for cases where
 * memory is kept in some kind of cache. Or you can check {@link #isMemoryLow()} while processing data.
 *
 * @author Christian Dietz, University of Konstanz
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.12
 */
public final class MemoryAlertSystem {
    /**
     * The default threshold of used memory that triggers a memory event.
     */
    public static final double DEFAULT_USAGE_THRESHOLD = 0.7;

    private static final MemoryAlertSystem INSTANCE = new MemoryAlertSystem(DEFAULT_USAGE_THRESHOLD);

    /* Standard Logger */
    private final NodeLogger LOGGER = NodeLogger.getLogger(MemoryAlertSystem.class);

    private final Collection<MemoryAlertListener> m_listeners = new ArrayList<>();

    private final MemoryPoolMXBean m_memPool = findTenuredGenPool();

    private final Lock m_notificationLock = new ReentrantLock();

    private final Condition m_notificationReceived = m_notificationLock.newCondition();

    private boolean m_lowMemory;

    private final AtomicLong m_lastEventTimestamp = new AtomicLong();

    /**
     * Creates a new memory alert system. <b>In almost all cases you should use the singleton instance via
     * {@link #getInstance()} instead of creating your own instance.</b>
     *
     * @param usageThreshold the threshold above which a low memory condition will be reported; a value between 0 and 1
     * @noreference This constructor is not intended to be referenced by clients. Only used in test cases.
     */
    public MemoryAlertSystem(final double usageThreshold) {
        setFractionUsageThreshold(usageThreshold);
        NotificationEmitter memoryBean = (NotificationEmitter)ManagementFactory.getMemoryMXBean();

        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (Stream.of(gcBean.getMemoryPoolNames()).filter(x -> m_memPool.getName().equals(x)).findAny().isPresent()) {
                ((NotificationEmitter)gcBean).addNotificationListener((not, obj) -> {
                    // Only reset the low memory flag if the last (memory) event was earlier than this event.
                    // the GC event and the Mem event have the same timestamp in case the threshold is exceeded.
                    if (m_lastEventTimestamp.getAndSet(not.getTimeStamp()) < not.getTimeStamp()) {
                        LOGGER.debug("Memory usage below threshold after GC run");
                        m_lowMemory = false;
                    }
                }, null, null);
                break;
            }
        }

        memoryBean.addNotificationListener(new NotificationListener() {
            @Override
            public void handleNotification(final Notification not, final Object hb) {
                if (not.getType().equals(MemoryNotificationInfo.MEMORY_COLLECTION_THRESHOLD_EXCEEDED)) {
                    LOGGER.debug("Memory collection threshold exceeded event received");
                    m_lastEventTimestamp.set(not.getTimeStamp());
                    m_lowMemory = true;
                    sendMemoryAlert();
                }
            }
        }, null, null);

        startNotificationThread();
    }

    /**
     * Send a memory alert events to all registered listeners. Should only be used for testing purposes.
     */
    public void sendMemoryAlert() {
        m_notificationLock.lock();
        try {
            m_notificationReceived.signalAll();
        } finally {
            m_notificationLock.unlock();
        }
    }

    /**
     * Registers a new listener.
     *
     * @param listener the listener
     * @return <code>true</code> if the listener was added, <code>false</code> otherwise
     */
    public boolean addListener(final MemoryAlertListener listener) {
        synchronized (m_listeners) {
            return m_listeners.add(listener);
        }
    }

    /**
     * Removes a registered listener,
     *
     * @param listener a listener
     * @return <code>true</code> if the listener was removed, <code>false</code> otherwise
     */
    public boolean removeListener(final MemoryAlertListener listener) {
        synchronized (m_listeners) {
            return m_listeners.remove(listener);
        }
    }

    /**
     * Set percentage level of the amount of memory in tenured space which may be set before a memory warning event is
     * thrown. Note that this changes a global value, therefore use with care!
     *
     * @param percentage the fraction of used memory after garbage collection, a value between 0 and 1
     */
    private void setFractionUsageThreshold(final double percentage) {
        if (percentage <= 0.0 || percentage > 1.0) {
            throw new IllegalArgumentException("Percentage not in range");
        }

        long warningThreshold = (long)(getMaximumMemory() * percentage);
        m_memPool.setCollectionUsageThreshold(warningThreshold);
    }

    private void startNotificationThread() {
        // run in separate thread so that it can be debugged and we don't mess around with system threads
        final Thread t = new Thread() {
            @Override
            public void run() {
                while (!isInterrupted()) {
                    m_notificationLock.lock();
                    try {
                        m_notificationReceived.await();
                    } catch (InterruptedException ex) {
                        break;
                    } finally {
                        m_notificationLock.unlock();
                    }
                    notifyListeners();
                }
            }
        };
        t.setName("KNIME Memory Alert Distributor");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Returns the currently used amount of memory in the old generation memory pool.
     *
     * @return the used memory in bytes
     */
    public long getUsedMemory() {
        return m_memPool.getUsage().getUsed();
    }

    /**
     * Returns the maximum amount of memory available in the old generation memory pool.
     *
     * @return the available memory in bytes
     */
    public long getMaximumMemory() {
        return m_memPool.getUsage().getMax();
    }

    private void notifyListeners() {
        MemoryAlert alert = new MemoryAlert(getUsedMemory(), getMaximumMemory());

        synchronized (m_listeners) {
            int initialSize = m_listeners.size();
            int removeCount = 0;
            for (Iterator<MemoryAlertListener> it = m_listeners.iterator(); it.hasNext();) {
                MemoryAlertListener listener = it.next();
                NodeContext.pushContext(listener.getNodeContext());
                try {
                    if (listener.memoryAlert(alert)) {
                        removeCount++;
                        it.remove();
                    }
                } catch (Exception ex) {
                    LOGGER
                        .error("Error while notifying memory alert listener " + listener + ": " + ex.getMessage(), ex);
                } finally {
                    NodeContext.removeLastContext();
                }
            }

            LOGGER.debug(removeCount + "/" + initialSize + " listeners have been removed,  " + m_listeners.size()
                + " are remaining");
        }
    }

    /**
     * Tenured Space Pool can be determined by it being of type HEAP and by it being possible to set the usage
     * threshold.
     */
    private static MemoryPoolMXBean findTenuredGenPool() {
        List<String> asList = Arrays.asList("Tenured Gen", "PS Old Gen", "CMS Old Gen", "G1 Old Gen");

        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            // I don't know whether this approach is better, or
            // whether
            // we should rather check for the pool name
            // "Tenured Gen"?
            if (pool.getType().equals(MemoryType.HEAP) && pool.isUsageThresholdSupported()
                && asList.contains(pool.getName())) {
                return pool;
            }
        }
        throw new AssertionError("Could not find tenured space");
    }

    /**
     * Returns if the memory usage is above the configured threshold. The flaf will be cleared when the next garbage
     * collector run can free enough memory.
     *
     * @return <code>true</code> if memory is low, <code>false</code> otherwise
     */
    public boolean isMemoryLow() {
        return m_lowMemory;
    }

    /**
     * Singleton instance of {@link MemoryAlertSystem}.
     *
     * @return the singleton instance
     */
    public static MemoryAlertSystem getInstance() {
        return INSTANCE;
    }
}
