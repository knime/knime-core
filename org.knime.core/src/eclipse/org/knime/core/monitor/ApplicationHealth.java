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
 *   Jan 16, 2025 (wiswedel): created
 */
package org.knime.core.monitor;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.knime.core.data.DataTable;
import org.knime.core.data.util.memory.InstanceCounter;
import org.knime.core.internal.ApplicationHealthInternal;
import org.knime.core.internal.ApplicationHealthInternal.LoadAvgIntervals;
import org.knime.core.internal.ApplicationHealthInternal.QueueLengthAvgIntervals;
import org.knime.core.monitor.beans.ApplicationHealthMetric;
import org.knime.core.monitor.beans.Counter;
import org.knime.core.monitor.beans.CounterMXBean;
import org.knime.core.monitor.beans.CountersMXBean;
import org.knime.core.monitor.beans.DataTableCountsMXBean;
import org.knime.core.monitor.beans.GlobalPoolMXBean;
import org.knime.core.monitor.beans.InstanceCountersMXBean;
import org.knime.core.monitor.beans.NodeStates;
import org.knime.core.monitor.beans.NodeStatesMXBean;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowDataRepository;

/**
 * Utility class centralizing metrics that can be monitored, e.g. in metrics end points etc. While the class is public,
 * its use is limited to KNIME internals such as health checkers in hub executors etc.
 *
 * @noreference This class is not intended to be referenced by clients.
 * @author Bernd Wiswedel
 * @since 5.5
 */
public final class ApplicationHealth implements AutoCloseable {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ApplicationHealth.class);

    private final List<ObjectName> m_registeredBeans = new ArrayList<>();

    /**
     * Constructs a new instance, setting up relevant metrics frameworks (e.g. MXBeans).
     */
    public ApplicationHealth() {

        final var metrics = new HashMap<>(Map.of( //
            "org.knime.core:type=Execution,name=GlobalPool", //
            new GlobalPoolMXBean() {
                @Override
                public LoadAverages getAverageQueueLength() {
                    return ApplicationHealth.getGlobalThreadPoolQueuedAverages();
                }

                @Override
                public LoadAverages getAverageLoad() {
                    return ApplicationHealth.getGlobalThreadPoolLoadAverages();
                }
            }, //
            "org.knime.core:type=Memory,name=ObjectInstances", //
            (InstanceCountersMXBean)() -> ApplicationHealth.getInstanceCounters().stream() //
                .collect(Collectors.toMap(InstanceCounter::getName, InstanceCounter::get)), //
            "org.knime.core:type=Memory,name=DataTablesRepository", //
            (DataTableCountsMXBean)ApplicationHealth::getDataTableCounts, //
            "org.knime.core:type=Execution,name=NodeStates", //
            (NodeStatesMXBean)() -> new NodeStates(ApplicationHealth.getNodeStateExecutedCount(),
                ApplicationHealth.getNodeStateExecutingCount(), ApplicationHealth.getNodeStateOtherCount())));

        if (ProcessStateUtil.supportsRSS()) {
            metrics.put("org.knime.core:type=Memory,name=KNIMErss",
                (CounterMXBean)() -> new Counter("rssBytes", ApplicationHealth.getKnimeProcessRssBytes()));
        }
        if (ProcessStateUtil.supportsPSS()) {
            metrics.put("org.knime.core:type=Memory,name=ExternalProcessesPss",
                (CountersMXBean)() -> Arrays.stream(ExternalProcessType.values()) //
                    .map(t -> new Counter(t.name(), ApplicationHealth.getExternalProcessesPssBytes(t))) //
                    .toList());
        }

        final var mbs = ManagementFactory.getPlatformMBeanServer();
        metrics.entrySet().stream() //
            .map(b -> tryRegisterAsBean(mbs, b.getKey(), b.getValue())) //
            .forEach(t -> t.ifPresent(m_registeredBeans::add));
    }

    /**
     * Unregisters metrics at relevant metrics frameworks.
     */
    @Override
    public void close() {
        final var mbs = ManagementFactory.getPlatformMBeanServer();
        m_registeredBeans.forEach(t -> {
            try {
                mbs.unregisterMBean(t);
            } catch (MBeanRegistrationException | InstanceNotFoundException ex) {
                LOGGER.debug(() -> "Failed to unregister bean for %s, ignoring...".formatted(t.getCanonicalName()), ex);
            }
        });
    }

    private static Optional<ObjectName> tryRegisterAsBean(final MBeanServer mbs, final String name,
        final ApplicationHealthMetric obj) {
        try {
            final var objectName = new ObjectName(name);
            LOGGER.debug("Registering JMX bean for %s".formatted(name));
            mbs.registerMBean(obj, objectName);
            return Optional.of(objectName);
        } catch (MBeanRegistrationException | NotCompliantMBeanException | MalformedObjectNameException e) {
            LOGGER.error(() -> "Failed to register \"%s\" with JMX.".formatted(name)
                + " You can ignore this error, but relevant metrics could be unavailable.", e);
            return Optional.empty();
        } catch (final InstanceAlreadyExistsException e) {
            LOGGER.coding(() -> "Failed to register \"%s\" with JMX, because it is already registered under that name."
                .formatted(name) + " You can ignore this warning.", e);
            return Optional.empty();
        }
    }

    /**
     * InstanceCounter for various classes used within the framework, currently nodes and workflow projects. Content
     * might change between releases.
     *
     * @return That non-modifiable list.
     */
    public static List<InstanceCounter<?>> getInstanceCounters() { // NOSONAR (generic wildcards)
        return ApplicationHealthInternal.INSTANCE_COUNTERS;
    }

    /**
     * Counts for {@link DataTable} classes used within all instances of {@link WorkflowDataRepository}.
     *
     * @return That non-modifiable map.
     * @since 5.8
     */
    public static Map<String, Long> getDataTableCounts() {
        return WorkflowDataRepository.takeDataTableCountsSnapshot();
    }

    /**
     * Count for the {@link DataTable} with the given class name within all {@link WorkflowDataRepository}s.
     *
     * @param name class name of the {@link DataTable}.
     * @return Snapshot of the count for the specified data table name.
     * @since 5.8
     */
    public static long getDataTableCountFor(final String name) {
        return WorkflowDataRepository.takeDataTableCountSnapshotFor(name);
    }

    /**
     * An estimate for the number of native nodes currently in executing state (truly executing, not just waiting to be
     * executed).
     *
     * @return That number
     */
    public static int getNodeStateExecutingCount() {
        return ApplicationHealthInternal.NODESTATE_EXECUTING_COUNTER.get();
    }

    /**
     * An estimate for the number of native nodes currently in executed state.
     *
     * @return That number
     */
    public static int getNodeStateExecutedCount() {
        return ApplicationHealthInternal.NODESTATE_EXECUTED_COUNTER.get();
    }

    /**
     * An estimate for the number of native nodes currently in other states, e.g. queued, post-processing etc.
     *
     * @return That number
     */
    public static int getNodeStateOtherCount() {
        return ApplicationHealthInternal.NODESTATE_OTHER.get();
    }

    /**
     * Return type of load average methods.
     *
     * @param avg1Min 1min average load
     * @param avg5Min 5min average load
     * @param avg15Min 15min average load
     */
    public record LoadAverages(double avg1Min, double avg5Min, double avg15Min) {}

    /**
     * The "load" average (estimate) of the global KNIME thread pool (as per
     * {@link org.knime.core.node.KNIMEConstants#GLOBAL_THREAD_POOL}). That is, the number of concurrent jobs, usually
     * node executions, that are running averaged over intervals of 1, 5, and 15min.
     *
     * @return The load averages at the current time.
     */
    public static LoadAverages getGlobalThreadPoolLoadAverages() {
        return new LoadAverages(
            ApplicationHealthInternal.GLOBAL_THREAD_POOL_LOAD_TRACKER.getLoadAverage(LoadAvgIntervals.ONE_MIN),
            ApplicationHealthInternal.GLOBAL_THREAD_POOL_LOAD_TRACKER.getLoadAverage(LoadAvgIntervals.FIVE_MIN),
            ApplicationHealthInternal.GLOBAL_THREAD_POOL_LOAD_TRACKER.getLoadAverage(LoadAvgIntervals.FIFTEEN_MIN));
    }

    /**
     * Estimates for the number of queued jobs in the global KNIME thread pool averaged over intervals of 1, 5, and
     * 15min.
     *
     * @return average number of queued jobs at the current time
     */
    public static LoadAverages getGlobalThreadPoolQueuedAverages() {
        return new LoadAverages(
            ApplicationHealthInternal.QUEUE_LENGTH_LOAD_TRACKER.getLoadAverage(QueueLengthAvgIntervals.ONE_MIN),
            ApplicationHealthInternal.QUEUE_LENGTH_LOAD_TRACKER.getLoadAverage(QueueLengthAvgIntervals.FIVE_MIN),
            ApplicationHealthInternal.QUEUE_LENGTH_LOAD_TRACKER.getLoadAverage(QueueLengthAvgIntervals.FIFTEEN_MIN));
    }

    /**
     * @return the approximate current RSS (resident set size) of the KNIME process in bytes
     */
    public static long getKnimeProcessRssBytes() {
        return ProcessWatchdog.KNIME_PROCESS_RSS.get();
    }

    /**
     * @param type the type of the external process
     * @return the current PSS (proportional set size) of all external processes of the given type (and their children)
     *         in bytes, or -1 if no such process is running or PSS is not supported on the current platform
     */
    public static long getExternalProcessesPssBytes(final ExternalProcessType type) {
        return ProcessWatchdog.EXTERNAL_PROCESS_PSS.getOrDefault(type, -1L);
    }
}
