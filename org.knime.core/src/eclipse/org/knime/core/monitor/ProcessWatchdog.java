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
 *   Jul 23, 2024 (benjamin): created
 */
package org.knime.core.monitor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

import org.apache.commons.lang3.SystemUtils;
import org.knime.core.node.NodeLogger;

import gnu.trove.set.hash.TLongHashSet;

/**
 * Watchdog that tracks the memory usage of KNIME AP and its external processes. If the total memory usage surpasses a
 * threshold, the external process with the highest memory usage gets killed forcibly. The watchdog uses the
 * proportional set size (PSS) of the processes and their subprocesses to determine their memory usage.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @since 5.5
 */
public final class ProcessWatchdog {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ProcessWatchdog.class);

    private static final boolean ENABLE_TIME_TRACKER_FOR_DEBUGGING =
        Boolean.getBoolean("knime.processwatchdog.timetracker");

    /** The polling interval for the watchdog in milliseconds. */
    private static final long POLLING_INTERVAL_MS = Long.getLong("knime.processwatchdog.pollinginterval", 250);

    static final ProcessHandle KNIME_PROCESS_HANDLE = ProcessHandle.current();

    /**
     * The maximum memory that KNIME AP and external processes are allowed to allocate. <code>-1</code> means no limit
     * and disables the watchdog.
     */
    private static final long MAX_MEMORY_KBYTES = getMaxMemoryKBytes();

    /**
     * How much memory we reserve for the OS and other processes running on the container
     */
    private static final long CONTAINER_RESERVED_MEMORY_KBYTES =
        Long.getLong("knime.processwatchdog.containerreservedkbytes", 128);

    private static long getMaxMemoryKBytes() {
        var max = Long.getLong("knime.processwatchdog.maxmemory");
        if (max != null) {
            LOGGER.info("KNIME Process Watchdog memory limit configured via system property to " + max);
            return max;
        }

        var requestedContainerSizeBytes = System.getenv("CONTAINER_MEMORY_REQUESTS");
        if (requestedContainerSizeBytes != null) {
            try {
                return (Long.valueOf(requestedContainerSizeBytes) >> 10) - CONTAINER_RESERVED_MEMORY_KBYTES;
            } catch (NumberFormatException e) {
                LOGGER.warn("Could not parse value of environment variable 'CONTAINER_MEMORY_REQUESTS' ("
                    + requestedContainerSizeBytes + ")");
            }
        }

        LOGGER.info("KNIME Process Watchdog memory limit not configured");
        return -1;
    }

    private static final ProcessWatchdog INSTANCE = new ProcessWatchdog();

    /**
     * @return the singleton instance of the watchdog
     */
    public static ProcessWatchdog getInstance() {
        return INSTANCE;
    }

    // #region monitoring

    static final AtomicLong KNIME_PROCESS_RSS = new AtomicLong(0);

    static final ConcurrentMap<ExternalProcessType, Long> EXTERNAL_PROCESS_PSS =
        new ConcurrentHashMap<>(ExternalProcessType.values().length);

    // #endregion

    //#region API

    /**
     * Start tracking the given external process. If the total memory usage of external processes surpasses a threshold,
     * the process that uses most memory gets killed forcibly ({@link ProcessHandle#destroyForcibly()}). The
     * <code>killCallback</code> is called for this process.
     * <P>
     * Note that the memory usage of the process and all subprocesses is tracked.
     *
     * @param process a handle for the process
     * @param type the type of the external process
     * @param killCallback a callback that gets called before a process is killed by the watchdog. The argument of the
     *            callback is the current memory usage of the process in kilo-bytes. The callback must not block. The
     *            killing of the process cannot be prevented by freeing up memory. The callback must only be used to
     *            record the reason why the process was killed.
     */
    public void trackProcess(final ProcessHandle process, final ExternalProcessType type,
        final LongConsumer killCallback) {
        if (m_watchdogRunning) {
            var existingkillCallback =
                m_trackedProcesses.putIfAbsent(process, new TypeAndKillCallback(type, killCallback));
            if (existingkillCallback != null) {
                throw new IllegalArgumentException("The process " + process + " is already being tracked.");
            }
        }
    }

    /**
     * @since 5.5
     * @param resourceAlertListener A resource alert listener to call when the system is low on resources
     */
    public SafeCloseable registerResourceAlertListener(final ResourceAlertListener resourceAlertListener) {
        m_resourceAlertListeners.put(resourceAlertListener, resourceAlertListener);
        return () -> m_resourceAlertListeners.remove(resourceAlertListener);
    }

    /**
     * @since 5.5
     * @return Whether node executions should be cancelled
     */
    public boolean shouldNodeExecutionsBeCancelled() {
        return m_shouldNodeExecutionsBeCancelled;
    }

    /**
     * @since 5.5
     */
    public interface ResourceAlertListener {
        void alert(Message message);
    }

    //#endregion

    //#region INSTANCE

    private ConcurrentMap<ResourceAlertListener, ResourceAlertListener> m_resourceAlertListeners =
        new ConcurrentHashMap<>();

    private boolean m_shouldNodeExecutionsBeCancelled;

    private record TypeAndKillCallback(ExternalProcessType type, LongConsumer killCallback) {
    }

    private ConcurrentMap<ProcessHandle, TypeAndKillCallback> m_trackedProcesses = new ConcurrentHashMap<>();

    private final boolean m_watchdogRunning;

    /** Set to false if we have warned once, and the memory usage did not drop below the limit */
    private boolean m_shouldWarnAboutKnimeProcessMemory = true;

    private ProcessWatchdog() {

        // We only track memory usage on Linux systems that support PSS measurements
        if (ProcessStateUtil.supportsPSS() && ProcessStateUtil.supportsRSS() && MAX_MEMORY_KBYTES >= 0) {

            // Record the time it takes to update the memory usage if enabled
            TimeTracker timeTracker;
            if (ENABLE_TIME_TRACKER_FOR_DEBUGGING) {
                timeTracker = new TimeTracker();
            } else {
                timeTracker = null;
            }

            var timer = new Timer("KNIME External Process Watchdog", true); // Daemon thread
            timer.scheduleAtFixedRate(new TimerTask() {
                @SuppressWarnings("null") // timeTracker is only null if ENABLE_TIME_TRACKER_FOR_DEBUGGING is false
                @Override
                public void run() {
                    var startTime = System.nanoTime();
                    updateMemoryUsage();
                    if (ENABLE_TIME_TRACKER_FOR_DEBUGGING) {
                        timeTracker.addTime(System.nanoTime() - startTime);
                    }
                }
            }, 0, POLLING_INTERVAL_MS);
            m_watchdogRunning = true;

        } else if (MAX_MEMORY_KBYTES < 0) {
            LOGGER.info("External process memory watchdog is disabled, because the memory limit is set to "
                + MAX_MEMORY_KBYTES);
            m_watchdogRunning = false;
        } else if (SystemUtils.IS_OS_LINUX) {
            LOGGER.warn("PSS or RSS measurements are not supported on this system. "
                + "The external process memory watchdog is disabled.");
            m_watchdogRunning = false;
        } else {
            m_watchdogRunning = false;
        }
    }

    private void updateMemoryUsage() {
        var externalProcessMonitorUpdate = new HashMap<ExternalProcessType, Long>(ExternalProcessType.values().length);
        long totalExternalProcessPss = 0;
        long maxPss = -1;
        ProcessHandle maxPssUsageProcess = null;

        // External process memory usage
        var iterator = m_trackedProcesses.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var process = entry.getKey();
            var type = entry.getValue().type();

            // If the process is dead we do not need to track it anymore - remove it from the map
            if (!process.isAlive()) {
                iterator.remove();
                continue;
            }

            /* Collect the current memory usage of the process using PSS (Proportional Set Size).
             *
             * PSS is more accurate than RSS (Resident Set Size) when there are subprocesses because it properly
             * accounts for shared memory. Simply adding the RSS of each subprocess can significantly overestimate total
             * memory (especially if they share large regions), while using only the parent process’s RSS underestimates
             * usage if memory is heavily allocated in a subprocess.
             *
             * Although measuring PSS can be more runtime-intensive, the overhead is acceptable here because each
             * subprocess typically only lives for the duration of a single task. Moreover, in the Python kernel queue
             * scenario, new processes are initially very small, which helps keep the cost of collecting PSS minimal.
             */
            var pss = getPSSOfProcessTree(process.pid());
            totalExternalProcessPss += pss;

            // Remember the process with the highest memory - this will be killed if we have memory pressure
            if (pss > maxPss) {
                maxPss = pss;
                maxPssUsageProcess = process;
            }

            // Update the monitoring value for this process type
            externalProcessMonitorUpdate.merge(type, pss * 1024, Long::sum);
        }
        EXTERNAL_PROCESS_PSS.putAll(externalProcessMonitorUpdate);

        /* Collect KNIME AP process memory usage
         *
         * Note that we use the RSS of the KNIME AP process here. This is less accurate than the PSS but is faster to
         * obtain. For the KNIME AP process this should be sufficient because we do not share large memory regions with
         * other processes. We do not sum up the memory usage of all subprocesses of KNIME AP because they are tracked
         * individually by the watchdog.
         */
        var knimeRss = getKnimeRSS();
        KNIME_PROCESS_RSS.set(knimeRss * 1024);

        // Kill the larges external process if the accumulated memory usage is above the threshold
        if (totalExternalProcessPss + knimeRss > MAX_MEMORY_KBYTES && maxPssUsageProcess != null) {
            killProcessWithHighestMemoryUsage(maxPssUsageProcess, maxPss);
        }

        handleKnimeProcessMemory(knimeRss);
    }

    private void handleKnimeProcessMemory(final long knimeMemory) {
        // Warn if the memory usage of the JVM process itself is close to the limit
        if (knimeMemory > MAX_MEMORY_KBYTES * 0.80) {
            if (m_shouldWarnAboutKnimeProcessMemory) {
                m_shouldWarnAboutKnimeProcessMemory = false;

                LOGGER.warn("KNIME AP process is using " + knimeMemory + "KB of the available " + MAX_MEMORY_KBYTES
                    + "KB in the container");
            }
        } else {
            m_shouldWarnAboutKnimeProcessMemory = true;
        }

        if (knimeMemory > MAX_MEMORY_KBYTES * 0.90) {
            LOGGER.warn("KNIME AP process is using " + knimeMemory + "KB of the available "
                + MAX_MEMORY_KBYTES + "KB in the container. Cancelling node executions");
            cancelRunningNodes();
            m_shouldNodeExecutionsBeCancelled = true;
        } else {
            m_shouldNodeExecutionsBeCancelled = false;
        }
    }

    private void cancelRunningNodes() {
        // TODO: messaging that this node got cancelled.
        m_resourceAlertListeners.keySet()
            .forEach(t -> t.alert(Message.fromSummaryWithResolution("Execution cancelled due to low system resources",
                "Provide more resources to this execution context")));
    }

    /** Kill the process with the highest memory usage and call the kill callback */
    private void killProcessWithHighestMemoryUsage(final ProcessHandle processToKill, final long memoryUsage) {
        // Call the kill callback
        var killCallback =
            Optional.ofNullable(m_trackedProcesses.remove(processToKill)).map(TypeAndKillCallback::killCallback);
        if (killCallback.isPresent()) {
            try {
                killCallback.get().accept(memoryUsage);
            } catch (Throwable ex) { // NOSONAR: We want to make sure the callback cannot crash the watchdog
                LOGGER.error("Error in kill callback for process " + processToKill + ".", ex);
            }
        } else {
            // NB: This should never happen
            LOGGER.error("No kill callback found for process " + processToKill + ". Killing it without callback.");
        }

        // Kill the process
        processToKill.destroyForcibly();
    }

    //#endregion

    //#region STATIC UTILS

    /** Get the memory usage of the KNIME AP process in kb. Uses the RSS of the process. */
    private static long getKnimeRSS() {
        try {
            return ProcessStateUtil.getRSS(KNIME_PROCESS_HANDLE.pid());
        } catch (IOException e) {
            LOGGER.warn("Could not obtain memory usage of KNIME AP process", e);
            return 0;
        }
    }

    /**
     * Get the memory usage of the given process and all its subprocesses in kb. Uses the PSS of the individual
     * processes.
     *
     * @param process the process handle
     * @return proportional set size (PSS) of this process and all its subprocesses in kb or 0 if the memory usage could
     *         not be determined
     */
    private static long getPSSOfProcessTree(final long pid) {
        var childrenPids = getChildren(pid);
        var childrenMem = 0;
        for (var i = 0; i < childrenPids.length; i++) {
            childrenMem += getPSSOfProcessTree(childrenPids[i]);
        }
        try {
            return childrenMem + ProcessStateUtil.getPSS(pid);
        } catch (IOException e) {
            LOGGER.info("Failed to get memory usage of external process with pid " + pid + ".", e);
            return 0;
        }
    }

    /**
     * Get the pids of the direct children of the process with the given pid. Uses the /proc[pid]/task/[tid]/children
     * files to determine the children quickly and without building up a process tree or all processes.
     * <P>
     * Note that when this method returns, the list can be out-dated already.
     *
     * @param pid the pid of the process
     * @return a list of the pids of the direct children of the process
     * @see <a href="https://www.baeldung.com/linux/get-process-child-processes#using-the-proc-file-system">Baeldung -
     *      Getting a Process&#39; Child Processes</a>
     */
    private static long[] getChildren(final long pid) {
        var children = new TLongHashSet();
        try (var taskDirStream = Files.newDirectoryStream(Paths.get("/proc", String.valueOf(pid), "task"))) {
            for (var tidPath : taskDirStream) {
                addChildrenPids(tidPath, children);
            }
        } catch (IOException ex) {
            LOGGER.warn("Failed to get children of external process with pid " + pid + ".", ex);
        }

        return children.toArray();
    }

    /**
     * Parses the file /proc/[pid]/task/[tid]/children to get the children of a process and adds them to the given
     * children set.
     *
     * @param tidPath the path to the task directory of the process
     * @param children the set to add the children to
     * @see <a href=
     *      "https://docs.kernel.org/filesystems/proc.html#proc-pid-task-tid-children-information-about-task-children"
     *      >Linux /proc filesystem documentation</a>
     */
    private static void addChildrenPids(final Path tidPath, final TLongHashSet children) {
        var childrenPath = tidPath.resolve("children");

        final String childrenContent;
        try {
            // NB: We do not check for file existence but just try to read it and handle the exception
            childrenContent = Files.readString(childrenPath, StandardCharsets.US_ASCII).trim();
        } catch (IOException e) {
            LOGGER.debug("Failed to read children file", e);
            return;
        }

        if (!childrenContent.isEmpty()) {
            for (var childPid : childrenContent.split(" ")) {
                try {
                    children.add(Long.parseLong(childPid.trim()));
                } catch (final NumberFormatException ex) {
                    LOGGER.warn("Failed to parse subprocess pid from line '" + childPid + "'. Ignoring subprocess.",
                        ex);
                }
            }
        }
    }

    //#endregion

    //#region TimeTracker

    private static final class TimeTracker {

        // every 10 seconds
        private static final int LOGGING_INTERVAL = 10_000;

        private long m_numCalls;

        private long m_totalMeasuredTime;

        public TimeTracker() {
            var runtimeLogger = new Timer("KNIME Watchdog Runtime Logger", true);
            runtimeLogger.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (m_numCalls > 0) {
                        LOGGER.warnWithFormat("Avg measured time of Watchdog: %.3f ms",
                            m_totalMeasuredTime / m_numCalls / 1e6);
                    }
                }
            }, 0, LOGGING_INTERVAL);
        }

        public void addTime(final long time) {
            m_numCalls++;
            m_totalMeasuredTime += time;
        }
    }

    //#endregion
}
