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
 *   20 Dec 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.node.logging;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

import org.apache.commons.io.output.NullWriter;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.eclipse.core.runtime.Platform;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;
import org.knime.core.node.logging.LogBuffer.BufferedLogMessage;

/**
 * Tests that the node logger can be used prior to setting the workspace (instance location) and still
 * all log messages end up in the configured writer.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
class NodeLoggerInitializationTest {

    private static NullWriter WRITER;

    @BeforeAll
    static void setup() {
        WRITER = new NullWriter();
    }

    @AfterAll
    static void teardown() {
        WRITER.close();
        WRITER = null;
    }

    @Test
    void testBufferDraining() {
        final var buffer = new LogBuffer(3);
        final var msgs = new String[] { "First", "Second", "Third" };
        for (final var msg : msgs) {
            buffer.log(Level.DEBUG, "testing", msg, null);
        }
        final var buffered = new ArrayList<BufferedLogMessage>();
        buffer.drainTo(buffered::add);
        final var bufferedMessages = buffered.stream().map(BufferedLogMessage::message)
                .toArray(String[]::new);
        assertArrayEquals(msgs, bufferedMessages, "Unexpected messages received while draining buffer");
    }

    @Test
    void testNoBuffering() {
        // initializing the object instance is automatic
        final var loggerName = "org.knime.bufferedloggertest";
        final var logStack = new LogStack(loggerName);
        KNIMELogger.addWriter(WRITER, logStack, LEVEL.DEBUG, LEVEL.FATAL);
        // add twice to test removal logic
        KNIMELogger.addWriter(WRITER, logStack, LEVEL.DEBUG, LEVEL.FATAL);

        final var testLogger = NodeLogger.getLogger(loggerName);
        final var beforeMsg = "Before setting instance location";
        testLogger.warn(beforeMsg);

        // should be a no-op
        KNIMELogger.initializeLogging(false);

        assertFalse(logStack.m_stack.isEmpty(), "Expected at least one log message");
        final var middleMsg = "Middle message";
        testLogger.info(middleMsg);
        assertFalse(logStack.m_stack.isEmpty(), "Expected some log messages");

        // this should have no effect, since we should have never buffered
        KNIMELogger.logBufferedMessages();

        logStack.assertFirstLogMessageEquals(Level.WARN, beforeMsg);
        logStack.assertLastLogMessageEquals(Level.INFO, middleMsg);

        final var afterMsg = "After setting instance location";
        testLogger.info(afterMsg);
        logStack.assertLastLogMessageEquals(Level.INFO, afterMsg);
    }

    @Test
    @Disabled
    void testLogBeforeInstanceLocationSet() {
        Assumptions.assumeFalse(Platform.getInstanceLocation().isSet(),
            """
                Test is skipped since instance location must not already be set for the test to make sense.
                Possible reasons (multiple may apply):
                - JUnit Plug-in Test not run in headless mode
                - Workspace set in Run Configuration

                If you want to run the test (locally), make sure the instance location is not set when running the test.
            """);

        final var loggerName = "org.knime.bufferedloggertest";
        final var testLogger = NodeLogger.getLogger(loggerName);
        final var beforeMsg = "Before setting instance location";
        testLogger.info(beforeMsg);
        // assertFalse(KNIMELogging.isInitialized(), "Logger should not yet be initialized");

        // don't force default location to be set and instead let logging initialization set it implicitly
        KNIMELogger.initializeLogging(false);
        assertTrue(Platform.getInstanceLocation().isSet(), "Instance location should have been set implicitly");
        // install our writer in between initialization and buffer draining
        final var logStack = new LogStack(loggerName);
        KNIMELogger.addWriter(WRITER, logStack, LEVEL.DEBUG, LEVEL.FATAL);

        assertTrue(logStack.m_stack.isEmpty(), "Unexpected log messages; expected none");
        final var middleMsg = "Middle message";
        testLogger.info(middleMsg);
        assertFalse(logStack.m_stack.isEmpty(), "No log messages; expected some");

        KNIMELogger.logBufferedMessages();
        assertFalse(logStack.m_stack.isEmpty(), "No log messages; expected some");

        logStack.assertFirstLogMessageEquals(Level.INFO, middleMsg);
        logStack.assertLastLogMessageEquals(Level.INFO, beforeMsg);

        final var afterMsg = "After setting instance location";
        testLogger.info(afterMsg);
        logStack.assertLastLogMessageEquals(Level.INFO, afterMsg);
    }

    private record LogMsg(Level level, String msg) {}

    private final class LogStack extends Layout {

        private final Deque<LogMsg> m_stack = new ArrayDeque<>();

        private final String m_filter;

        LogStack(final String filter) {
            m_filter = filter;
        }

        @Override
        public void activateOptions() {
            // no op
        }

        @Override
        public boolean ignoresThrowable() {
            return false;
        }

        @Override
        public String format(final LoggingEvent event) {
            final var level = event.getLevel();
            final var msg = event.getMessage().toString();

            if (m_filter.equals(event.getLoggerName())) {
                m_stack.add(new LogMsg(level, msg));
            }

            return String.format("%s: %s", level, msg);
        }

        private void expectLogMessageEquals(final boolean first, final Level level, final String msg) {
            final var log = first ? m_stack.peekFirst() : m_stack.peekLast();
            final var s = first ? "first" : "last";
            assertEquals(msg, log.msg, "Unexpected %s log message".formatted(s));
            assertEquals(level, log.level, "Unexpected level for %s log message".formatted(s));
        }

        public void assertLastLogMessageEquals(final Level level, final String expected) {
            expectLogMessageEquals(false, level, expected);
        }

        public void assertFirstLogMessageEquals(final Level level, final String expected) {
            expectLogMessageEquals(true, level, expected);
        }
    }

}
