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
 *   02.03.2015 (thor): created
 */
package org.knime.core.util;

import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.knime.core.node.NodeLogger;

/**
 * This class tries to detect deadlocks in GUI threads. If a potential deadlock is detected, a full stack trace is
 * logged. Subclasses need to implement {@link #enqueue(Runnable)} and dispatch the runnable to the corresponding
 * thread.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @author Luke Bullard
 * @since 2.12
 */
public abstract class GUIDeadlockDetector {
    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    // The threshold must be at least twice the check interval!
    /** The threshold for detecting slow-downs. **/
    private static final Integer THRESHOLD = Integer.getInteger("knime.deadlockdetector.threshold", 21000);

    /** The interval for thread dumps. **/
    private static final Integer CHECK_INTERVAL = Integer.getInteger("knime.deadlockdetector.check.interval", 10000);

    private static final Boolean DONT_WATCH = Boolean.getBoolean("knime.deadlockdetector.off");

    private final ScheduledExecutorService m_executor = Executors.newScheduledThreadPool(1);

    /** Last time an event was dispatched. **/
    private final AtomicLong m_lastDispatched = new AtomicLong(System.currentTimeMillis());

    /** Last time an event completed. **/
    private final AtomicLong m_lastProcessed = new AtomicLong(System.currentTimeMillis());

    private final AtomicBoolean m_deadlockDetected = new AtomicBoolean();

    /**
     * Creates a new deadlock detector.
     */
    protected GUIDeadlockDetector() {
        if (!DONT_WATCH && !isInDebug()) {
            m_executor.scheduleAtFixedRate(new SubmitTask(), 0, CHECK_INTERVAL, TimeUnit.MILLISECONDS);
            m_executor.scheduleAtFixedRate(new CheckTask(), 1000, CHECK_INTERVAL, TimeUnit.MILLISECONDS);
        }
    }

    /** @return true if the VM runs in debug mode -- don't enable deadlock detector then. */
    private static final boolean isInDebug() {
        return ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("-agentlib:jdwp");
    }

    /**
     * Enqueues the given runnable in the event dispatch thread.
     *
     * @param r a runnable
     */
    protected abstract void enqueue(Runnable r);

    /**
     * Returns the event dispatch thread's name, e.g. "SWT display thread" or "AWT event dispatch thread".
     *
     * @return a thread name
     */
    protected abstract String getThreadName();

    private final class SubmitTask implements Runnable {
        @Override
        public void run() {
            m_lastDispatched.set(System.currentTimeMillis());
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    m_lastProcessed.set(System.currentTimeMillis());
                }
            };
            enqueue(r);
        }
    }

    private final class CheckTask implements Runnable {
        @Override
        public void run() {
            if (m_lastDispatched.get() - m_lastProcessed.get() < THRESHOLD) {
                // If the difference between dispatch and processing is within tolerance, do nothing.
                m_deadlockDetected.set(false);
            } else if (!m_deadlockDetected.getAndSet(true)) {
                String stacktrace = ThreadUtils.getJVMStacktraces();
                m_logger.debug("Potential deadlock in " + getThreadName()
                    + " detected. Full thread dump will follow as debug output.");
                m_logger.debug(stacktrace);
            }
        }
    }

}
