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
 *   11.03.2015 (thor): created
 */
package org.knime.core.data.util.memory;

import static java.lang.management.MemoryNotificationInfo.MEMORY_COLLECTION_THRESHOLD_EXCEEDED;
import static java.lang.management.MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @since 2.12
 */
public final class MemoryAlertSystem {
    /**
     * Interface for a simple stateful object that indicates if action because of low memory should be taken.
     *
     * @since 3.0
     * @noimplement This interface is not intended to be implemented by clients.
     * @noextend This interface is not intended to be extended by clients.
     */
    @FunctionalInterface
    public interface MemoryActionIndicator { // NOSONAR equivalent to `BooleanSupplier`, but already API
        /**
         * Returns <code>true</code> if an action because of a low memory condition is required. Subsequent calls will
         * return <code>false</code> until the next GC events, because it is assumed that the action freed memory.
         *
         * @return <code>true</code> if an action is required, <code>false</code> if no action is required
         */
        boolean lowMemoryActionRequired();
    }

    private final class MemoryActionIndicatorImpl implements MemoryActionIndicator {
        private long m_lastCheckTimestamp;

        @Override
        public boolean lowMemoryActionRequired() {
            if (m_lastCheckTimestamp < m_lastGcTimestamp.get()) {
                m_lastCheckTimestamp = m_lastGcTimestamp.get();
                return isMemoryLow();
            } else {
                return false;
            }
        }
    }


    private static final MemoryPoolMXBean OLD_GEN_POOL = findTenuredGenPool();

    /**
     * The threshold of collected old generation heap space usage that triggers a memory event. The threshold is set to
     * 90% of the total memory minus 128MB.
     */
    public static final double DEFAULT_USAGE_THRESHOLD = 0.9 - ((128 << 20) / (double) getMaximumMemory());

    /**
     * Instance that emits memory alerts when collected old generation heap space (i.e., space after the last
     * full garbage collection) is nearing exhaustion.
     */
    private static final MemoryAlertSystem INSTANCE = new MemoryAlertSystem(DEFAULT_USAGE_THRESHOLD, true);

    private static final boolean IS_G1 = ManagementFactory.getGarbageCollectorMXBeans().stream()
            .map(GarbageCollectorMXBean::getName).anyMatch(n -> n.equals("G1 Old Generation"));

    /**
     * The threshold of current old generation heap space usage that triggers a memory event. It should not be higher
     * than the {@link #DEFAULT_USAGE_THRESHOLD}. It should also be above the initiating heap occupancy percent (IHOP)
     * of G1 (and other concurrent GCs), which is set to 0.45 by deault. If this threshold were to be set below the IHOP
     * percentage, we can end up in a state of near-permanent memory alert, since the garbage collector won't bother
     * collecting garbage when heap occupancy is below IHOP. If this threshold were to be set close to the IHOP
     * percentage, the uncollected memory alert system will emit an alert around the time the first (concurrent) garbage
     * collection run is triggered, which is probably too early. For more information, see the comments in AP-12939.
     */
    private static final double DEFAULT_USAGE_THRESHOLD_UNCOLLECTED;
    static {
        double ihop = .45d;
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (arg.startsWith("-XX:InitiatingHeapOccupancyPercent=")) {
                try {
                    ihop = Integer.parseInt(arg.substring(35)) / 100d;
                } catch (NumberFormatException e) {
                }
            }
        }
        DEFAULT_USAGE_THRESHOLD_UNCOLLECTED = Math.min(DEFAULT_USAGE_THRESHOLD, (DEFAULT_USAGE_THRESHOLD + ihop) / 2);
    }

    /**
     * Instance that emits memory alerts when current old generation heap space exceeds the threshold at which garbage
     * collection is triggered. Currently only supported when the G1 garbage collector is enabled (see
     * {@link #getInstanceUncollected()}).
     */
    private static final MemoryAlertSystem INSTANCE_UNCOLLECTED =
        new MemoryAlertSystem(DEFAULT_USAGE_THRESHOLD_UNCOLLECTED, false);

    /* Standard Logger */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(MemoryAlertSystem.class);

    /**
     * The time (in seconds) that has to pass at least in between heap size checks.
     */
    private static final int CHECK_HEAP_SIZE_INTERVAL = 5;

    private final Map<MemoryAlertListener, Object> m_listeners = new ConcurrentHashMap<>();

    /** Dummy object for the listeners map. */
    private static final Object DUMMY = new Object();

    private final MemoryPoolMXBean m_memPool = OLD_GEN_POOL;

    private final ReentrantLock m_aboveThresholdLock = new ReentrantLock();

    private final Condition m_aboveThresholdEvent = m_aboveThresholdLock.newCondition();

    private final AtomicBoolean m_lowMemory = new AtomicBoolean();

    private final AtomicLong m_lastEventTimestamp = new AtomicLong();

    private final AtomicLong m_lastGcTimestamp = new AtomicLong();

    private final double m_usageThreshold;

    private final boolean m_checkCollectedMemory;

    private long m_timeOfLastCheck = System.currentTimeMillis();

    /**
     * Creates a new memory alert system. <b>In almost all cases you should use the instance via {@link #getInstance()}
     * or {@link #getInstanceUncollected()} instead of creating your own instance.</b>
     *
     * @param usageThreshold the threshold above which a low memory condition will be reported; a value between 0 and 1
     * @param checkCollectedMemory whether only collected memory (i.e., memory after the last full GC) or current memory
     *            is to be considered when determining a low memory condition
     * @noreference This constructor is not intended to be referenced by clients. Only used in test cases.
     */
    private MemoryAlertSystem(final double usageThreshold, final boolean checkCollectedMemory) {
        m_usageThreshold = usageThreshold;
        m_checkCollectedMemory = checkCollectedMemory;
        setFractionUsageThreshold(usageThreshold);

        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            for (String memPoolName : gcBean.getMemoryPoolNames()) {
                if (memPoolName.equals(m_memPool.getName())) {
                    ((NotificationEmitter)gcBean).addNotificationListener(new NotificationListener() {
                        @Override
                        public void handleNotification(final Notification notification, final Object handback) {
                            gcEvent(notification);
                        }
                    }, null, null);
                    break;
                }
            }
        }

        NotificationEmitter memoryBean = (NotificationEmitter)ManagementFactory.getMemoryMXBean();
        memoryBean.addNotificationListener(new NotificationListener() {
            @Override
            public void handleNotification(final Notification notification, final Object handback) {
                if (notification.getType().equals(MEMORY_COLLECTION_THRESHOLD_EXCEEDED)) {
                    LOGGER.debugWithFormat("Memory collection threshold of %.0f%% exceeded after GC",
                        m_usageThreshold * 100.0);
                    usageThresholdEvent(notification);
                } else if (!m_checkCollectedMemory && notification.getType().equals(MEMORY_THRESHOLD_EXCEEDED)) {
                    LOGGER.debugWithFormat("Memory threshold of %.0f%% exceeded", m_usageThreshold * 100.0);
                    usageThresholdEvent(notification);
                }
            }
        }, null, null);

        startNotificationThread();
    }

    private void usageThresholdEvent(final Notification not) {
        long prev, next;
        do {
            prev = m_lastEventTimestamp.get();
            next = Math.max(prev, not.getTimeStamp());
        } while (!m_lastEventTimestamp.compareAndSet(prev, next));

        if (prev < not.getTimeStamp()) {
            m_lowMemory.set(true);
            sendMemoryAlert();
        }
    }

    private void gcEvent(final Notification not) {
        // Only reset the low memory flag if the last (memory) event was earlier than this event.
        // the GC event and the Mem event have the same timestamp in case the threshold is exceeded.

        m_lastGcTimestamp.set(not.getTimeStamp());
        long prev, next;
        do {
            prev = m_lastEventTimestamp.get();
            next = Math.max(prev, not.getTimeStamp());
        } while (!m_lastEventTimestamp.compareAndSet(prev, next));

        if (prev < not.getTimeStamp()) {
            final MemoryUsage collectionUsage = m_memPool.getCollectionUsage();
            final double used = collectionUsage.getUsed();
            final long max = collectionUsage.getMax();
            final double currentUsage = used / max;
            if (currentUsage < m_usageThreshold) {
                m_lowMemory.set(false);
            }
            m_timeOfLastCheck = System.currentTimeMillis();
        }
    }

    /**
     * Send a memory alert events to all registered listeners. Should only be used for testing purposes.
     */
    public void sendMemoryAlert() {
        m_aboveThresholdLock.lock();
        try {
            m_aboveThresholdEvent.signalAll();
        } finally {
            m_aboveThresholdLock.unlock();
        }
    }

    /**
     * Registers a new listener.
     *
     * @param listener the listener
     * @return <code>true</code> if the listener was added, <code>false</code> otherwise
     */
    public boolean addListener(final MemoryAlertListener listener) {
        return m_listeners.put(listener, DUMMY) == null;
    }

    /**
     * Removes a registered listener,
     *
     * @param listener a listener
     * @return <code>true</code> if the listener was removed, <code>false</code> otherwise
     */
    public boolean removeListener(final MemoryAlertListener listener) {
        return m_listeners.remove(listener) == DUMMY;
    }

    /**
     * Set percentage level of the amount of memory in tenured space which may be set before a memory warning event is
     * thrown. <b>Note that this changes a global value, therefore use with care!</b>
     *
     * @param percentage the fraction of used memory after garbage collection, a value between 0 and 1
     * @noreference This method is not intended to be referenced by clients.
     */
    public void setFractionUsageThreshold(final double percentage) {
        if (percentage <= 0.0 || percentage > 1.0) {
            throw new IllegalArgumentException("Percentage not in range");
        }

        long warningThreshold = (long)(getMaximumMemory() * percentage);
        m_memPool.setCollectionUsageThreshold(warningThreshold);
        if (!m_checkCollectedMemory) {
            m_memPool.setUsageThreshold(warningThreshold);
        }
    }

    private void startNotificationThread() {
        // run in separate thread so that it can be debugged and we don't mess around with system threads
        final Thread t = new Thread() {
            @Override
            public void run() {
                while (!isInterrupted()) {
                    m_aboveThresholdLock.lock();
                    try {
                        m_aboveThresholdEvent.await();
                    } catch (InterruptedException ex) {
                        break;
                    } finally {
                        m_aboveThresholdLock.unlock();
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
    public static long getUsedMemory() {
        return OLD_GEN_POOL.getUsage().getUsed();
    }

    /**
     * Returns the maximum amount of memory available in the old generation memory pool.
     *
     * @return the available memory in bytes
     */
    public static long getMaximumMemory() {
        return OLD_GEN_POOL.getUsage().getMax();
    }

    /**
     * Returns the current memory usage as a fraction between 0 and 1.
     *
     * @return the current memory usage
     */
    public static double getUsage() {
        return getUsedMemory() / (double)getMaximumMemory();
    }

    private void notifyListeners() {
        final MemoryAlert alert = new MemoryAlert(getUsedMemory(), getMaximumMemory());
        final int initialSize = m_listeners.size();

        LOGGER.debugWithFormat("%d listeners will be notified.", initialSize);
        int removeCount = 0;
        for (final Iterator<MemoryAlertListener> it = m_listeners.keySet().iterator(); it.hasNext();) {
            final MemoryAlertListener listener = it.next();
            NodeContext.pushContext(listener.getNodeContext());
            try {
                if (listener.memoryAlert(alert)) {
                    removeCount++;
                    it.remove();
                }
            } catch (Exception ex) {
                LOGGER.errorWithFormat("Error while notifying memory alert listener %s: %s", listener, ex.getMessage(),
                    ex);
            } finally {
                NodeContext.removeLastContext();
            }
        }

        LOGGER.debugWithFormat("%d/%d listeners have been removed, %d are remaining.", removeCount, initialSize,
            m_listeners.size());
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
     * Returns if the memory usage is above the configured threshold. The flag will be cleared when the next garbage
     * collector run can free enough memory.
     *
     * @return <code>true</code> if memory is low, <code>false</code> otherwise
     */
    public boolean isMemoryLow() {
        final long time = System.currentTimeMillis();

        /**
         * We have to occasionally check tenured gen heap space manually, since some GCs (like G1) avoid full GCs and,
         * thus, will only very rarely (if ever) emit a GC event notification that would be caught by the
         * MemoryAlertSystem. See AP-11858 for details.
         */
        if (m_lowMemory.get() && (time - m_timeOfLastCheck) / 1000 >= CHECK_HEAP_SIZE_INTERVAL) {
            // based on the Javadoc, invoking getUsage() is expected to be a relatively quick operation
            final MemoryUsage usage = m_memPool.getUsage();
            final double used = usage.getUsed();
            final long max = usage.getMax();
            final double currentUsage = used / max;

            if (currentUsage < m_usageThreshold) {
                m_lowMemory.set(false);
            }
            m_timeOfLastCheck = time;
        }

        return m_lowMemory.get();
    }

    /**
     * Creates a new memory action indicator.
     *
     * @return a new indicator
     * @since 3.0
     */
    public MemoryActionIndicator newIndicator() {
        return new MemoryActionIndicatorImpl();
    }

    /**
     * Instance of {@link MemoryAlertSystem} that emits memory alerts when collected old generation heap space (i.e.,
     * space after the last full garbage collection) is nearing exhaustion.
     *
     * @return the instance for collected old generation heap space
     */
    public static MemoryAlertSystem getInstance() {
        return INSTANCE;
    }

    /**
     * Instance of {@link MemoryAlertSystem} that emits memory alerts when current old generation heap space exceeds the
     * threshold at which garbage collection is triggered. Currently only supported when the G1 garbage collector is
     * enabled. Will return {@link #getInstance()} when another garbage collector is enabled.
     *
     * @return the instance for current old generation heap space
     * @since 4.0
     */
    public static MemoryAlertSystem getInstanceUncollected() {
        return IS_G1 ? INSTANCE_UNCOLLECTED : getInstance();
    }

    /**
     * Method for obtaining the number of currently registered listeners. For testing purposes only.
     *
     * @return the number of currently registered listeners
     * @since 4.2
     * @noreference This method is not intended to be referenced by clients.
     */
    public static long getNumberOfListeners() {
        return (long)INSTANCE.m_listeners.size() + INSTANCE_UNCOLLECTED.m_listeners.size();
    }
}
