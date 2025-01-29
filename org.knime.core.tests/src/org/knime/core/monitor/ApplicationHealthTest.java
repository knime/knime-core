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
 *   Jan 17, 2025 (wiswedel): created
 */
package org.knime.core.monitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.commons.io.output.NullWriter;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.jupiter.api.Test;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;

/**
 * Tests for {@link ApplicationHealth}.
 *
 * @author wiswedel
 */
final class ApplicationHealthTest {

    @SuppressWarnings("static-method")
    @Test
    final void testInstanceCounters() {
        assertFalse(ApplicationHealth.getInstanceCounters().isEmpty(), "instance counter list should not be empty");
    }

    @SuppressWarnings("static-method")
    @Test
    final void testThreadPoolLoadAverages() {
        assertTrue(ApplicationHealth.getGlobalThreadPoolLoadAverages().avg1Min() >= 0.0, "Reports load average >= 0.0");
    }

    @SuppressWarnings("static-method")
    @Test
    final void testQueuedAverages() {
        assertTrue(ApplicationHealth.getGlobalThreadPoolQueuedAverages().avg1Min() >= 0.0,
            "Reports queue length average >= 0.0");
    }

    /**
     * Tests that registration of ApplicationHealth MXBeans worked.
     *
     * @throws IntrospectionException some MXBean exception
     * @throws InstanceNotFoundException some MXBean exception
     * @throws MalformedObjectNameException some MXBean exception
     * @throws ReflectionException some MXBean exception
     */
    @Test
    @SuppressWarnings("static-method")
    final void testMXBeanRegistrations()
        throws IntrospectionException, InstanceNotFoundException, MalformedObjectNameException, ReflectionException {
        try (final var app = new ApplicationHealth()) {

            final var nodeStatesMXBean = ManagementFactory.getPlatformMBeanServer()
                .getMBeanInfo(new ObjectName("org.knime.core:type=Execution,name=NodeStates"));
            final var nodeStatesDesc = nodeStatesMXBean.getDescriptor();
            assertEquals("org.knime.core.monitor.beans.CountersMXBean",
                nodeStatesDesc.getFieldValue("interfaceClassName"), "NodeStates should be CountersMXBean");
            assertEquals("true", nodeStatesDesc.getFieldValue("mxbean"), "Should be an MXBean");

            final var globalPoolMXBean = ManagementFactory.getPlatformMBeanServer()
                .getMBeanInfo(new ObjectName("org.knime.core:type=Execution,name=GlobalPool"));
            final var poolMXBean = globalPoolMXBean.getDescriptor();
            assertEquals("org.knime.core.monitor.beans.GlobalPoolMXBean",
                poolMXBean.getFieldValue("interfaceClassName"), "GlobalPool should be GlobalPoolMXBean");
            assertEquals("true", poolMXBean.getFieldValue("mxbean"), "Should be an MXBean");

            final var instancesMXBean = ManagementFactory.getPlatformMBeanServer()
                .getMBeanInfo(new ObjectName("org.knime.core:type=Memory,name=ObjectInstances"));
            final var instancesDesc = instancesMXBean.getDescriptor();
            assertEquals("org.knime.core.monitor.beans.CountersMXBean",
                instancesDesc.getFieldValue("interfaceClassName"), "Instances should be CountersMXBean");
            assertEquals("true", instancesDesc.getFieldValue("mxbean"), "Should be an MXBean");

            // check that "CODING" message is logged to std out when opening app health twice
            final var startMsg = "EXPECTED LOG START";
            final var endMsg = "EXPECTED LOG END";
            final AtomicBoolean enabled = new AtomicBoolean(false);
            final AtomicInteger numBeans = new AtomicInteger(0);
            final var logStack = new LogInterceptor(ApplicationHealth.class.getName(), log -> {
                if (log.msg.contains(startMsg)) {
                    enabled.set(true);
                } else if (log.msg.contains(endMsg)) {
                    enabled.set(false);
                }

                if (!enabled.get()) {
                    return;
                }
                if (log.level == Level.ERROR && log.msg.startsWith("CODING PROBLEM")
                    && log.msg.contains("Failed to register")) {
                    numBeans.incrementAndGet();
                }
            });
            NodeLogger.addWriter(NullWriter.INSTANCE, logStack, LEVEL.ERROR, LEVEL.OFF);
            NodeLogger.getLogger(ApplicationHealth.class.getName()).error(startMsg);
            try (final var app2 = new ApplicationHealth()) {
                // should work, but print CODING error
                assertTrue(true);
            }
            NodeLogger.getLogger(ApplicationHealth.class.getName()).error(endMsg);
            NodeLogger.removeWriter(NullWriter.INSTANCE);
            // GlobalPool, ObjectInstances, NodeStates
            assertTrue(numBeans.get() == 3, "Expected 3 CODING error messages, got " + numBeans.get());
        }
    }

    private record LogMsg(Level level, String msg) {
    }

    private static final class LogInterceptor extends Layout {

        private final String m_loggerName;

        private final Consumer<LogMsg> m_logConsumer;

        LogInterceptor(final String loggerName, final Consumer<LogMsg> logConsumer) {
            m_loggerName = loggerName;
            m_logConsumer = logConsumer;
        }

        @Override
        public String format(final LoggingEvent event) {
            final var level = event.getLevel();
            final var message = event.getMessage();

            if (m_loggerName.equals(event.getLoggerName()) && message != null) {
                final var msg = event.getMessage().toString();
                m_logConsumer.accept(new LogMsg(level, msg));
            }

            return String.format("%s: %s", level, message);
        }

        @Override
        public void activateOptions() {
            // no-op
        }

        @Override
        public boolean ignoresThrowable() {
            return false;
        }

    }
}
