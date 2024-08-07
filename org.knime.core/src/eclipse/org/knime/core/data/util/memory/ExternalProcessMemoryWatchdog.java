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
package org.knime.core.data.util.memory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.LongConsumer;

import org.apache.commons.lang3.SystemUtils;
import org.knime.core.data.TableBackend;
import org.knime.core.data.TableBackendRegistry;
import org.knime.core.node.NodeLogger;

import gnu.trove.map.hash.TObjectLongHashMap;
import gnu.trove.set.hash.TLongHashSet;

/**
 * Watchdog that tracks the memory usage of external processes. If the total memory usage of all external processes
 * surpasses a threshold, the process with the highest memory usage gets killed forcibly. The watchdog uses the
 * proportional set size (PSS) of the processes and their subprocesses to determine their memory usage.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Berlin, Germany
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @since 5.4
 */
public final class ExternalProcessMemoryWatchdog {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ExternalProcessMemoryWatchdog.class);

    /** The polling interval for the watchdog in milliseconds. */
    private static final long POLLING_INTERVAL_MS = Long.getLong("knime.externalprocesswatchdog.pollinginterval", 250);

    /**
     * The maximum memory that external processes are allowed to allocate. <code>-1</code> means no limit and disables
     * the watchdog.
     */
    private static final long MAX_MEMORY_KBYTES = getMaxMemoryKBytes();

    /**
     * How much memory we reserve for the OS and other processes running on the container
     */
    private static final long CONTAINER_RESERVED_MEMORY_KBYTES =
        Long.getLong("knime.externalprocesswatchdog.containerreservedkbytes", 128);

    private static long getMaxMemoryKBytes() {
        var requestedContainerSizeBytes = System.getenv("CONTAINER_MEMORY_REQUESTS");
        if (requestedContainerSizeBytes == null) {
            return getMaxMemoryKBytesFromSysProperty();
        }

        try {
            long requestedContainerSizeKBytes = Long.valueOf(requestedContainerSizeBytes) >> 10;
            LOGGER.info("Watchdog config: CONTAINER_MEMORY_REQUESTS = " + requestedContainerSizeKBytes + " KB");
            long jvmMemoryKBytes = Runtime.getRuntime().maxMemory() >> 10;
            LOGGER.info("Watchdog config: JVM Memory = " + jvmMemoryKBytes + " KB");

            long tableBackendOffHeapKBytes = TableBackendRegistry.getInstance().getTableBackends().stream() //
                .mapToLong(TableBackend::getReservedOffHeapBytes) //
                .sum() //
                >> 10; // turn Bytes to KBytes

            LOGGER.info("Watchdog config: TableBackend OffHeap = " + tableBackendOffHeapKBytes + " KB");

            long memoryLimitKBytes = requestedContainerSizeKBytes //
                - jvmMemoryKBytes //
                - tableBackendOffHeapKBytes //
                - CONTAINER_RESERVED_MEMORY_KBYTES;

            LOGGER.info("KNIME External Process Watchdog memory limit configured based on environment variable "
                + "CONTAINER_MEMORY_REQUESTS propery to " + memoryLimitKBytes + "kb");
            return memoryLimitKBytes;
        } catch (NumberFormatException e) {
            LOGGER.warn("Could not parse value of environment variable 'CONTAINER_MEMORY_REQUESTS' ("
                + requestedContainerSizeBytes + ")");
            return getMaxMemoryKBytesFromSysProperty();
        }
    }

    private static long getMaxMemoryKBytesFromSysProperty() {
        var max = Long.getLong("knime.externalprocesswatchdog.maxmemory");
        if (max != null) {
            LOGGER.info("KNIME External Process Watchdog memory limit configured via system propery to " + max);
            return max;
        }

        LOGGER.info("KNIME External Process Watchdog memory limit not configured");
        return -1;
    }

    private static final ExternalProcessMemoryWatchdog INSTANCE = new ExternalProcessMemoryWatchdog();

    /**
     * @return the singleton instance of the watchdog
     */
    public static ExternalProcessMemoryWatchdog getInstance() {
        return INSTANCE;
    }

    //#region API

    /**
     * Start tracking the given external process. If the total memory usage of external processes surpasses a threshold,
     * the process that uses most memory gets killed forcibly ({@link ProcessHandle#destroyForcibly()}). The
     * <code>killCallback</code> is called for this process.
     * <P>
     * Note that the memory usage of the process and all subprocesses is tracked.
     *
     * @param process a handle for the process
     * @param killCallback a callback that gets called before a process is killed by the watchdog. The argument of the
     *            callback is the current memory usage of the process in kilo-bytes. The callback must not block. The
     *            killing of the process cannot be prevented by freeing up memory. The callback must only be used to
     *            record the reason why the process was killed.
     */
    public void trackProcess(final ProcessHandle process, final LongConsumer killCallback) {
        if (m_watchdogRunning) {
            var existingkillCallback = m_processesToKillCallbacks.putIfAbsent(process, killCallback);
            if (existingkillCallback != null) {
                throw new IllegalArgumentException("The process " + process + " is already being tracked.");
            }
        }
    }

    //#endregion

    //#region INSTANCE

    private ConcurrentMap<ProcessHandle, LongConsumer> m_processesToKillCallbacks = new ConcurrentHashMap<>();

    private final boolean m_watchdogRunning;

    private ExternalProcessMemoryWatchdog() {
        // We only track memory usage on Linux systems that support PSS measurements
        if (PSSUtil.supportsPSS() && MAX_MEMORY_KBYTES >= 0) {
            var timer = new Timer("KNIME External Process Watchdog", true); // Daemon thread
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    updateMemoryUsage();
                }
            }, 0, POLLING_INTERVAL_MS);
            m_watchdogRunning = true;
        } else if (MAX_MEMORY_KBYTES < 0) {
            LOGGER.info("External process memory watchdog is disabled, because the memory limit is set to " + MAX_MEMORY_KBYTES);
            m_watchdogRunning = false;
        } else if (SystemUtils.IS_OS_LINUX) {
            LOGGER.warn(
                "PSS measurements are not supported on this system. The external process memory watchdog is disabled.");
            m_watchdogRunning = false;
        } else {
            m_watchdogRunning = false;
        }
    }

    private void updateMemoryUsage() {
        var memoryState = collectMemoryUsageState();

        // Check if the total memory usage surpasses the threshold
        if (memoryState.getTotalMemoryUsage() > MAX_MEMORY_KBYTES) {
            killProcessWithHighestMemoryUsage(memoryState);
        }
    }

    /** Collect the memory usage of all tracked external processes. Removes dead processes from the tracking. */
    private ExternalProcessMemoryState collectMemoryUsageState() {
        var memoryState = new ExternalProcessMemoryState();

        var iterator = m_processesToKillCallbacks.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var process = entry.getKey();

            // If the process is dead we do not need to track it anymore - remove it from the map
            if (!process.isAlive()) {
                iterator.remove();
                continue;
            }

            // Collect the current memory usage of the process
            memoryState.put(process, getMemoryUsage(process.pid()));
        }

        return memoryState;
    }

    /** Kill the process with the highest memory usage. */
    private void killProcessWithHighestMemoryUsage(final ExternalProcessMemoryState memoryState) {
        var processToKill = memoryState.getProcessWithMaxMemoryUsage();

        // Call the kill callback
        var killCallback = m_processesToKillCallbacks.remove(processToKill);
        if (killCallback != null) {
            try {
                killCallback.accept(memoryState.getMemoryUsage(processToKill));
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

    /**
     * Get the memory usage of the given process and all its subprocesses in kb. Uses the PSS of the individual
     * processes.
     *
     * @param process the process handle
     * @return proportional set size (PSS) of this process and all its subprocesses in kb or 0 if the memory usage could
     *         not be determined
     */
    private static long getMemoryUsage(final long pid) {
        var childrenPids = getChildren(pid);
        var childrenMem = 0;
        for (var i = 0; i < childrenPids.length; i++) {
            childrenMem += getMemoryUsage(childrenPids[i]);
        }
        try {
            return childrenMem + PSSUtil.getPSS(pid);
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

    //#region ExternalProcessMemoryState

    /** Utility class to store and collect the memory usage of external processes. Not thread-safe! */
    private static final class ExternalProcessMemoryState {

        private final TObjectLongHashMap<ProcessHandle> m_processToMemoryUsage;

        private long m_totalMemoryUsage;

        private long m_maxMemoryUsage;

        private ProcessHandle m_maxMemoryUsageProcess;

        public ExternalProcessMemoryState() {
            m_processToMemoryUsage = new TObjectLongHashMap<>();
            m_totalMemoryUsage = 0;
            m_maxMemoryUsage = -1;
        }

        /**
         * Collect the memory usage of the given process.
         *
         * @param process the process
         * @param memoryUsage the memory usage
         */
        public void put(final ProcessHandle process, final long memoryUsage) {
            m_processToMemoryUsage.put(process, memoryUsage);

            // Update the total memory usage
            m_totalMemoryUsage += memoryUsage;

            // Update the max memory usage
            if (memoryUsage > m_maxMemoryUsage) {
                m_maxMemoryUsage = memoryUsage;
                m_maxMemoryUsageProcess = process;
            }
        }

        /**
         * @param process the process
         * @return the memory usage of the given process
         */
        public long getMemoryUsage(final ProcessHandle process) {
            return m_processToMemoryUsage.get(process);
        }

        /** @return the total memory usage of all processes */
        public long getTotalMemoryUsage() {
            return m_totalMemoryUsage;
        }

        /** @return the process with the highest memory usage */
        public ProcessHandle getProcessWithMaxMemoryUsage() {
            return m_maxMemoryUsageProcess;
        }
    }

    //#endregion
}
