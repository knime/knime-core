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
 */
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
                if (n.getType().equals(MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED)) {
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

        m_memPool.setUsageThreshold(warningThreshold);
    }

    private long computeUsedMem() {
        return null != m_memPool ? m_memPool.getUsage().getUsed() : Runtime.getRuntime().totalMemory()
                - Runtime.getRuntime().freeMemory();
    }

    private long computeMaxMem() {
        // Compute the threshold in bytes
        long maxMem = null != m_memPool ? m_memPool.getUsage().getMax() : Runtime.getRuntime().maxMemory();

        // Workaround for a bug in G1 garbage collector:
        // http://bugs.sun.com/view_bug.do?bug_id=6880903
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        if (jvmArgs.contains("-XX:+UseG1GC")) {
            boolean xmxArgSet = false;
            for (String arg : jvmArgs) {
                if (arg.startsWith("-Xmx")) {
                    xmxArgSet = true;
                    boolean factorPresent = false;
                    int factor = -1;
                    if (arg.toLowerCase().endsWith("k")) {
                        factorPresent = true;
                        factor = 1000;
                    } else if (arg.toLowerCase().endsWith("m")) {
                        factorPresent = true;
                        factor = 1000000;
                    } else if (arg.toLowerCase().endsWith("g")) {
                        factorPresent = true;
                        factor = 1000000000;
                    }
                    if (factorPresent) {
                        maxMem = Integer.parseInt(arg.substring(4, arg.length() - 1)) * factor;
                    } else {
                        maxMem = Integer.parseInt(arg.substring(4));
                    }
                    break;
                }
            }
            if (!xmxArgSet) {
                LOGGER.error("Please, set -Xmx jvm argument " + "due to a bug in G1GC. Otherwise, memory "
                        + "intensive nodes might not work correctly.");
            }
        }

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