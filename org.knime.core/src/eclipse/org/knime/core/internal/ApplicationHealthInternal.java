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
package org.knime.core.internal;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.knime.core.data.util.memory.InstanceCounter;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.LoadTracker;

/**
 * Bundle-internal fields that need to be public so that they can be modified from code inside org.knime.core.
 *
 * <p>
 * The fields below are defined here to centralize the definition of all relevant counters. This is necessary
 * because the metrics endpoint (Prometheus) needs to be aware of the complete set of metrics upfront. Only this
 * initial set defines the available metrics.
 *
 * @author Bernd Wiswedel
 * @since 5.5
 */
public final class ApplicationHealthInternal {
    /**
     * Counter of native nodes currently in EXECUTING state (as per <code>InternalNodeContainerState#EXECUTING</code>).
     */
    public static final AtomicInteger NODESTATE_EXECUTING_COUNTER = new AtomicInteger();

    /**
     * Counter of native nodes currently in EXECUTED state (as per <code>InternalNodeContainerState#EXECUTED</code>).
     */
    public static final AtomicInteger NODESTATE_EXECUTED_COUNTER = new AtomicInteger();

    /**
     * Counter of native nodes currently in not executed and not executing state, includes nodes that are
     * queued or post processing.
     */
    public static final AtomicInteger NODESTATE_OTHER = new AtomicInteger();

    /**
     * Collection of instance counters that are considered interesting or relevant. Content may change between releases.
     */
    public static final List<InstanceCounter<?>> INSTANCE_COUNTERS = List.of( //
        NativeNodeContainer.INSTANCE_COUNTER, //
        NodeModel.INSTANCE_COUNTER, //
        WorkflowManager.PROJECT_COUNTER, //
        WorkflowManager.NO_PROJECT_COUNTER);

    /**
     * The intervals tracked in the load tracker observing the global thread pool load.
     */
    @SuppressWarnings("javadoc")
    public enum LoadAvgIntervals {
        ONE_MIN, FIVE_MIN, FIFTEEN_MIN
    }

    /**
     * The load tracker observing {@link KNIMEConstants#GLOBAL_THREAD_POOL}.
     */
    public static final LoadTracker<LoadAvgIntervals> GLOBAL_THREAD_POOL_LOAD_TRACKER =
            LoadTracker.<LoadAvgIntervals> builder(Duration.ofSeconds(5),
                KNIMEConstants.GLOBAL_THREAD_POOL::getRunningThreads) //
            .addInterval(LoadAvgIntervals.ONE_MIN, Duration.ofMinutes(1)) //
            .addInterval(LoadAvgIntervals.FIVE_MIN, Duration.ofMinutes(5)) //
            .addInterval(LoadAvgIntervals.FIFTEEN_MIN, Duration.ofMinutes(15)) //
            .setIgnoreCloseInvocation(false) //
            .start();

    private ApplicationHealthInternal() {
        // no op
    }

}
