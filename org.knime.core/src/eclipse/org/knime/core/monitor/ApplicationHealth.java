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

import java.util.List;

import org.knime.core.data.util.memory.InstanceCounter;
import org.knime.core.internal.ApplicationHealthInternal;
import org.knime.core.internal.ApplicationHealthInternal.LoadAvgIntervals;

/**
 * Utility class centralizing metrics that can be monitored, e.g. in metrics end points etc. While the class is public,
 * its use is limited to KNIME internals such as health checkers in hub executors etc.
 *
 * @noreference This class is not intended to be referenced by clients.
 * @author Bernd Wiswedel
 * @since 5.5
 */
public final class ApplicationHealth {

    private ApplicationHealth() {
        // utility class
    }

    /**
     * InstanceCounter for various classes used within the framework, currently nodes and workflow projects.
     * Content might change between releases.
     *
     * @return That non-modifiable list.
     */
    public static List<InstanceCounter<?>> getInstanceCounters() { // NOSONAR (generic wildcards)
        return ApplicationHealthInternal.INSTANCE_COUNTERS;
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
     * Return type of {@link #getGlobalThreadPoolLoadAverages()}.
     * @param avg1Min 1min average load
     * @param avg5Min 5min average load
     * @param avg15Min 15min average load
     */
    public record GlobalPoolLoadAverages(double avg1Min, double avg5Min, double avg15Min) {}

    /**
     * The "load" average (estimate) of the global KNIME thread pool (as per
     * {@link org.knime.core.node.KNIMEConstants#GLOBAL_THREAD_POOL}). That is, the number of concurrent jobs,
     * usually node executions, that are running averaged over intervals of 1, 5, and 15min.
     * @return The load averages at the current time.
     */
    public static GlobalPoolLoadAverages getGlobalThreadPoolLoadAverages() {
        return new GlobalPoolLoadAverages(
            ApplicationHealthInternal.GLOBAL_THREAD_POOL_LOAD_TRACKER.getLoadAverage(LoadAvgIntervals.ONE_MIN),
            ApplicationHealthInternal.GLOBAL_THREAD_POOL_LOAD_TRACKER.getLoadAverage(LoadAvgIntervals.FIVE_MIN),
            ApplicationHealthInternal.GLOBAL_THREAD_POOL_LOAD_TRACKER.getLoadAverage(LoadAvgIntervals.FIFTEEN_MIN));
    }

}
