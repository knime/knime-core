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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.apache.commons.io.output.NullWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.jupiter.api.Test;
import org.knime.core.data.container.BufferedContainerTable;
import org.knime.core.data.container.DataContainerTest;
import org.knime.core.monitor.ApplicationHealth.LoadAverages;
import org.knime.core.monitor.beans.CounterMXBean;
import org.knime.core.monitor.beans.CountersMXBean;
import org.knime.core.monitor.beans.DataTableCountsMXBean;
import org.knime.core.monitor.beans.GlobalPoolMXBean;
import org.knime.core.monitor.beans.InstanceCountersMXBean;
import org.knime.core.monitor.beans.NodeStatesMXBean;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;
import org.knime.core.node.workflow.WorkflowDataRepositoryTest;

/**
 * Tests for {@link ApplicationHealth}.
 *
 * @author wiswedel
 */
final class ApplicationHealthTest {

    @SuppressWarnings("static-method")
    @Test
    final void testInstanceCounters() throws IntrospectionException, InstanceNotFoundException,
        MalformedObjectNameException, ReflectionException, AttributeNotFoundException, MBeanException {
        try (final var app = new ApplicationHealth()) {
            assertFalse(ApplicationHealth.getInstanceCounters().isEmpty(), "instance counter list should not be empty");

            // and now via JMX
            final var server = ManagementFactory.getPlatformMBeanServer();
            final var name = new ObjectName("org.knime.core:type=Memory,name=ObjectInstances");
            final var info = server.getMBeanInfo(name);
            for (final var attr : info.getAttributes()) {
                final var attrName = attr.getName();
                final var attrValue = server.getAttribute(name, attrName);
                NodeLogger.getLogger(ApplicationHealthTest.class).info(attrName + ": " + attrValue);
            }
            final var data = (TabularData)server.getAttribute(name, "InstanceCounters");
            assertFalse(data.isEmpty(), "Instance counters should not be empty");

            // test that we have exactly the known instance counters
            final var knownInstanceCounters = ApplicationHealth.getInstanceCounters() //
                .stream().map(i -> i.getName()).collect(Collectors.toSet());
            data.values().forEach(row -> {
                // Our Map<String, Long> is mapped to TabularData by JMX
                final var cd = (CompositeData)row;
                assertEquals(2, cd.values().size(), "TabularData row contains two columns");
                final var counterName = (String)cd.get("key");
                final var counterValue = (Long)cd.get("value");
                assertTrue(knownInstanceCounters.remove(counterName), "Unknown counter: " + counterName);
                assertTrue(counterValue >= 0, "Counter value should be non-negative");
            });
            assertTrue(knownInstanceCounters.isEmpty(),
                "Some known instance counters are missing: " + String.join(", ", knownInstanceCounters));
        }
    }

    @SuppressWarnings("static-method")
    @Test
    final void testDataTableCounts() throws IntrospectionException, InstanceNotFoundException,
        MalformedObjectNameException, ReflectionException, AttributeNotFoundException, MBeanException {
        // currently hard-coding data tables names in map, so should always be non-empty
        assertFalse(ApplicationHealth.getDataTableCounts().isEmpty(), "data tables counts map should not be empty");

        final var repository = WorkflowDataRepositoryTest.createWorkflowDataRepository();
        @SuppressWarnings("resource")
        final var table = DataContainerTest.generateSmallSizedTable();
        final var count = ApplicationHealth.getDataTableCountFor(BufferedContainerTable.class.getName());
        repository.addTable(table.getTableId(), table);

        try (final var app = new ApplicationHealth()) {
            assertEquals(1, ApplicationHealth.getDataTableCountFor(BufferedContainerTable.class.getName()) - count, //
                "one buffered container table should be counted");

            // and now via JMX
            final var server = ManagementFactory.getPlatformMBeanServer();
            final var name = new ObjectName("org.knime.core:type=Memory,name=DataTablesRepository");
            final var info = server.getMBeanInfo(name);
            for (final var attr : info.getAttributes()) {
                final var attrName = attr.getName();
                final var attrValue = server.getAttribute(name, attrName);
                NodeLogger.getLogger(ApplicationHealthTest.class).info(attrName + ": " + attrValue);
            }
            final var data = (TabularData)server.getAttribute(name, "DataTableCounts");
            assertFalse(data.isEmpty(), "DataTable counts should not be empty");

            // test that we have exactly the known data table counts
            final var knownDataTableCounts =
                ApplicationHealth.getDataTableCounts().keySet().stream().collect(Collectors.toSet());
            data.values().forEach(row -> {
                // our Map<String, Long> is mapped to TabularData by JMX
                final var cd = (CompositeData)row;
                assertEquals(2, cd.values().size(), "TabularData row contains two columns");
                final var countName = (String)cd.get("key");
                final var countValue = (Long)cd.get("value");
                assertTrue(knownDataTableCounts.remove(countName), "Unknown count: " + countName);
                assertTrue(countValue >= 0, "Count value should be non-negative");
            });
            assertTrue(knownDataTableCounts.isEmpty(),
                "Some known DataTable counts are missing: " + String.join(", ", knownDataTableCounts));
        }

        table.close();
    }

    @SuppressWarnings("static-method")
    @Test
    final void testNodeStates() throws MalformedObjectNameException, InstanceNotFoundException,
        AttributeNotFoundException, ReflectionException, MBeanException {
        try (final var app = new ApplicationHealth()) {
            assertEquals(0, ApplicationHealth.getNodeStateExecutedCount(), "No executed nodes");
            assertEquals(0, ApplicationHealth.getNodeStateExecutingCount(), "No executing nodes");
            assertEquals(0, ApplicationHealth.getNodeStateOtherCount(), "No nodes in \"other\" state");

            // and now via JMX
            final var server = ManagementFactory.getPlatformMBeanServer();
            final var name = new ObjectName("org.knime.core:type=Execution,name=NodeStates");
            final CompositeData attr = (CompositeData)server.getAttribute(name, "NodeStates");
            assertEquals(0, attr.get("executed"), "No executed nodes");
            assertEquals(0, attr.get("executing"), "No executing nodes");
            assertEquals(0, attr.get("other"), "No nodes in \"other\" state");
        }
    }

    @SuppressWarnings("static-method")
    @Test
    final void testThreadPoolLoadAverages() throws MalformedObjectNameException, InstanceNotFoundException,
        AttributeNotFoundException, ReflectionException, MBeanException {
        try (final var app = new ApplicationHealth()) {
            assertTrue(ApplicationHealth.getGlobalThreadPoolLoadAverages().avg1Min() >= 0.0,
                "Reports load average >= 0.0");

            // and now via JMX
            final var server = ManagementFactory.getPlatformMBeanServer();
            final var name = new ObjectName("org.knime.core:type=Execution,name=GlobalPool");
            final var load = fromCompositeData((CompositeData)server.getAttribute(name, "AverageLoad"));
            assertTrue(load.avg1Min() >= 0.0, "Reports load average >= 0.0");
        }
    }

    @SuppressWarnings("static-method")
    @Test
    final void testQueuedAverages() throws MalformedObjectNameException, InstanceNotFoundException,
        AttributeNotFoundException, ReflectionException, MBeanException {
        try (final var app = new ApplicationHealth()) {
            assertTrue(ApplicationHealth.getGlobalThreadPoolQueuedAverages().avg1Min() >= 0.0,
                "Reports queue length average >= 0.0");

            // and now via JMX
            final var server = ManagementFactory.getPlatformMBeanServer();
            final var name = new ObjectName("org.knime.core:type=Execution,name=GlobalPool");
            final var queue = fromCompositeData((CompositeData)server.getAttribute(name, "AverageQueueLength"));
            assertTrue(queue.avg1Min() >= 0.0, "Reports queue length average >= 0.0");
        }
    }

    private static LoadAverages fromCompositeData(final CompositeData cd) {
        return new LoadAverages((double)cd.get("avg1Min"), (double)cd.get("avg5Min"), (double)cd.get("avg15Min"));
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

            final var beans = new ArrayList<>();
            beans.add(assertMXBeanRegistered("org.knime.core:type=Execution,name=NodeStates", NodeStatesMXBean.class));
            beans.add(assertMXBeanRegistered("org.knime.core:type=Execution,name=GlobalPool", GlobalPoolMXBean.class));
            beans.add(assertMXBeanRegistered("org.knime.core:type=Memory,name=ObjectInstances",
                InstanceCountersMXBean.class));
            beans.add(assertMXBeanRegistered("org.knime.core:type=Memory,name=DataTablesRepository",
                DataTableCountsMXBean.class));
            if (ProcessStateUtil.supportsPSS()) {
                beans.add(assertMXBeanRegistered("org.knime.core:type=Memory,name=ExternalProcessesPss",
                    CountersMXBean.class));
            }
            if (ProcessStateUtil.supportsRSS()) {
                beans.add(assertMXBeanRegistered("org.knime.core:type=Memory,name=KNIMErss", CounterMXBean.class));
            }

            // check that "CODING" message is logged when opening app health twice
            final AtomicBoolean enabled = new AtomicBoolean(false);
            final AtomicInteger numBeans = new AtomicInteger(0);
            final List<String> msgs = new ArrayList<>();
            final var logStack = new LogInterceptor(ApplicationHealth.class.getName(), log -> {
                if (skip(enabled, log.msg)) {
                    return;
                }
                if (log.level == Level.ERROR && log.msg.startsWith("CODING PROBLEM")
                    && log.msg.contains("Failed to register")) {
                    numBeans.incrementAndGet();
                    msgs.add(StringUtils.truncate(log.msg, 100) + "[...]");
                }
            });
            try {
                NodeLogger.addWriter(NullWriter.INSTANCE, logStack, LEVEL.ERROR, LEVEL.OFF);
                NodeLogger.getLogger(ApplicationHealth.class.getName()).error(START_MSG_MARKER);
                try (final var app2 = new ApplicationHealth()) {
                    // should work, but print CODING error
                    assertTrue(true);
                }
                NodeLogger.getLogger(ApplicationHealth.class.getName()).error(END_MSG_MARKER);
            } finally {
                NodeLogger.removeWriter(NullWriter.INSTANCE);
            }

            // GlobalPool, ObjectInstances, NodeStates (and PSS & RSS on linux)
            assertEquals(beans.size(), numBeans.get(), "Expected %d CODING error messages, got %d:%n%s"
                .formatted(beans.size(), numBeans.get(), String.join(",\n", msgs)));
        }
    }

    private static final String START_MSG_MARKER = "EXPECTED LOG START";

    private static final String END_MSG_MARKER = "EXPECTED LOG END";

    private static boolean skip(final AtomicBoolean enabled, final String msg) {
        if (msg.contains(START_MSG_MARKER)) {
            enabled.set(true);
        } else if (msg.contains(END_MSG_MARKER)) {
            enabled.set(false);
        }
        return !enabled.get();
    }

    private static MBeanInfo assertMXBeanRegistered(final String beanName, final Class<?> beanType)
        throws ReflectionException, IntrospectionException, InstanceNotFoundException, MalformedObjectNameException {
        final var name = new ObjectName(beanName);
        final var nodeStatesMXBean = ManagementFactory.getPlatformMBeanServer().getMBeanInfo(name);
        final var nodeStatesDesc = nodeStatesMXBean.getDescriptor();
        assertEquals(beanType.getCanonicalName(), nodeStatesDesc.getFieldValue("interfaceClassName"),
            "NodeStates should be %s".formatted(beanType.getSimpleName()));
        assertEquals("true", nodeStatesDesc.getFieldValue("mxbean"), "Should be an MXBean");
        return nodeStatesMXBean;
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
