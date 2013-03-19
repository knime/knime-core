package org.knime.core.data.util.memory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryPoolMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;

import org.knime.core.node.NodeLogger;

/**
 * No public API yet.
 *
 * @author dietzc
 */
public class MemoryWarningSystem {

        private static final NodeLogger LOGGER = NodeLogger
                        .getLogger(MemoryWarningSystem.class);

        private final Collection<Listener> listeners = new ArrayList<Listener>();

        private static long m_maxMem;

        private static final MemoryPoolMXBean m_memPool = findTenuredGenPool();

        public interface Listener {
                public void memoryUsageLow(long usedMemory, long maxMemory);
        }

        public MemoryWarningSystem() {

                m_maxMem = computeMaxMem();

                MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
                NotificationEmitter emitter = (NotificationEmitter) mbean;

                emitter.addNotificationListener(new NotificationListener() {
                        @Override
                        public void handleNotification(final Notification n,
                                        final Object hb) {
                                if (n.getType()
                                                .equals(MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED)) {

                                        for (Listener listener : listeners) {
                                                listener.memoryUsageLow(
                                                                computeUsedMem(),
                                                                m_maxMem);
                                        }
                                }
                        }
                }, null, null);
        }

        public boolean addListener(final Listener listener) {
                return listeners.add(listener);
        }

        public boolean removeListener(final Listener listener) {
                return listeners.remove(listener);
        }

        public static void setPercentageUsageThreshold(final double percentage) {
                if (percentage <= 0.0 || percentage > 1.0) {
                        throw new IllegalArgumentException(
                                        "Percentage not in range");
                }
                long warningThreshold = (long) (computeMaxMem() * percentage);

                System.out.println("THRESHOLD: " + warningThreshold
                                / (1024.0d * 1024.0d));
                m_memPool.setUsageThreshold(warningThreshold);
        }

        public static long computeUsedMem() {
                return null != m_memPool ? m_memPool.getUsage().getUsed()
                                : Runtime.getRuntime().totalMemory()
                                                - Runtime.getRuntime()
                                                                .freeMemory();
        }

        private static long computeMaxMem() {
                // Compute the threshold in bytes
                long maxMem = null != m_memPool ? m_memPool.getUsage().getMax()
                                : Runtime.getRuntime().maxMemory();

                // Workaround for a bug in G1 garbage collector:
                // http://bugs.sun.com/view_bug.do?bug_id=6880903
                List<String> jvmArgs = ManagementFactory.getRuntimeMXBean()
                                .getInputArguments();
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
                                        } else if (arg.toLowerCase().endsWith(
                                                        "m")) {
                                                factorPresent = true;
                                                factor = 1000000;
                                        } else if (arg.toLowerCase().endsWith(
                                                        "g")) {
                                                factorPresent = true;
                                                factor = 1000000000;
                                        }
                                        if (factorPresent) {
                                                maxMem = Integer.parseInt(arg
                                                                .substring(4,
                                                                                arg.length() - 1))
                                                                * factor;
                                        } else {
                                                maxMem = Integer.parseInt(arg
                                                                .substring(4));
                                        }
                                        break;
                                }
                        }
                        if (!xmxArgSet) {
                                LOGGER.error("Please, set -Xmx jvm argument "
                                                + "due to a bug in G1GC. Otherwise, memory "
                                                + "intensive nodes might not work correctly.");
                        }
                }

                return maxMem;
        }

        /**
         * Tenured Space Pool can be determined by it being of type HEAP and by
         * it being possible to set the usage threshold.
         */
        private static MemoryPoolMXBean findTenuredGenPool() {
                for (MemoryPoolMXBean pool : ManagementFactory
                                .getMemoryPoolMXBeans()) {

                        List<String> asList = Arrays.asList("Tenured Gen",
                                        "PS Old Gen");

                        // I don't know whether this approach is better, or
                        // whether
                        // we should rather check for the pool name
                        // "Tenured Gen"?
                        if (asList.contains(pool.getName())
                                        && pool.isUsageThresholdSupported()) {
                                return pool;
                        }
                }
                throw new AssertionError("Could not find tenured space");
        }

        // /**
        // * @return
        // */
        // public synchronized static long availableMem() {
        // return m_maxMem - computeUsedMem();
        // }
}