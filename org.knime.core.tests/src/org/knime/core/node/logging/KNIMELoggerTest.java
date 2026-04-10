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
 *   18 Apr 2024 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.node.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.output.NullWriter;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;
import org.knime.core.util.EclipseUtil;
import org.knime.core.util.Pair;

/**
 * Tests for the KNIME logger. Currently only appender level setting.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
class KNIMELoggerTest {

    private static final String LOGGER_NAME = KNIMELoggerTest.class.getName();

    private static final String APPENDER_NAME = LOGGER_NAME + "_appender";

    private static NullWriter WRITER;

    private LogInterceptor m_logStack;

    @BeforeAll
    static void setup() {
        WRITER = new NullWriter();
        KNIMELogger.initializeLogging(false);
    }

    @AfterAll
    static void teardown() {
        WRITER.close();
        WRITER = null;
    }

    @BeforeEach
    void setupEach() {
        m_logStack = new LogInterceptor(LOGGER_NAME);
        // Initially, the writer/appender accepts log statements of any level since its accepted range is [ALL, OFF].
        KNIMELogger.addWriter(APPENDER_NAME, WRITER, m_logStack, LEVEL.ALL, LEVEL.OFF);
    }

    @AfterEach
    void removeWriter() {
        KNIMELogger.removeWriter(WRITER);
    }

    /**
     * Test for raising then lowering the minimum appender level.
     */
    @Test
    void testConfigureMinimumAppenderLevel() {

        final var logger = NodeLogger.getLogger(LOGGER_NAME);
        final var firstMsg = "DEBUG msg";
        logger.debug(firstMsg);

        m_logStack.assertLastLogMessageEquals(Level.DEBUG, firstMsg);
        KNIMELogger.modifyAppenderLevelRange(APPENDER_NAME, (min, max) -> Pair.create(LEVEL.INFO, max));
        assertEquals(Pair.create(LEVEL.INFO, LEVEL.OFF), KNIMELogger.getAppenderLevelRange(APPENDER_NAME),
            "Appender level range should have a raised lower bound");

        logger.debug("another DEBUG msg"); // should get ignored

        // stack should still contain the first message as last message
        m_logStack.assertLastLogMessageEquals(Level.DEBUG, firstMsg);

        // decreasing the minimum level again should let DEBUG through
        final var thirdMsg = "third DEBUG msg";
        KNIMELogger.modifyAppenderLevelRange(APPENDER_NAME, (min, max) -> Pair.create(LEVEL.DEBUG, max));
        assertEquals(Pair.create(LEVEL.DEBUG, LEVEL.OFF), KNIMELogger.getAppenderLevelRange(APPENDER_NAME),
            "Appender level range should have a lower lower bound again");

        logger.debug(thirdMsg);
        m_logStack.assertLastLogMessageEquals(Level.DEBUG, thirdMsg);
        m_logStack.assertFirstLogMessageEquals(Level.DEBUG, firstMsg);
    }

    /**
     * Test for lowering then raising the maximum appender level.
     */
    @Test
    void testConfigureMaximumAppenderLevel() {
        final var logger = NodeLogger.getLogger(LOGGER_NAME);
        final var firstMsg = "ERROR msg";
        logger.error(firstMsg);

        m_logStack.assertLastLogMessageEquals(Level.ERROR, firstMsg);
        KNIMELogger.modifyAppenderLevelRange(APPENDER_NAME, (min, max) -> Pair.create(min, LEVEL.WARN));
        assertEquals(Pair.create(LEVEL.ALL, LEVEL.WARN), KNIMELogger.getAppenderLevelRange(APPENDER_NAME),
            "Appender level range should have a lowered upper bound");

        logger.error("another ERROR msg"); // should get ignored

        // stack should still contain the first message
        m_logStack.assertLastLogMessageEquals(Level.ERROR, firstMsg);

        // raising the maximum level again should let ERROR through
        final var thirdMsg = "third ERROR msg";
        KNIMELogger.modifyAppenderLevelRange(APPENDER_NAME, (min, max) -> Pair.create(min, LEVEL.ERROR));
        assertEquals(Pair.create(LEVEL.ALL, LEVEL.ERROR), KNIMELogger.getAppenderLevelRange(APPENDER_NAME),
            "Appender level range should have a raised upper bound again");

        logger.error(thirdMsg);
        m_logStack.assertLastLogMessageEquals(Level.ERROR, thirdMsg);
        m_logStack.assertFirstLogMessageEquals(Level.ERROR, firstMsg);
    }

    @Test
    void testLogRejectsNullLevelForObjectMessage() {
        final var logger = KNIMELogger.getLogger(LOGGER_NAME);
        assertThrows(NullPointerException.class, () -> logger.log(null, "message", true, null));
    }

    @Test
    void testLogRejectsNullLevelForSupplierMessage() {
        final var logger = KNIMELogger.getLogger(LOGGER_NAME);
        assertThrows(NullPointerException.class, () -> logger.log(null, () -> "message", true, null));
    }

    @Test
    void testFailsafeDrainSkipsEmptyBuffer() {
        final var out = new ByteArrayOutputStream();
        final var router = new StartupLogRouter(2, new PrintStream(out), Level.DEBUG, createFailsafeLayout());
        router.activateFailsafeAndDrain();
        assertEquals("", out.toString(StandardCharsets.UTF_8), "Expected no failsafe output for empty buffer");
    }

    @Test
    void testFailsafeDrainSkipsMessagesBelowMinLevel() {
        final var out = new ByteArrayOutputStream();
        final var router = new StartupLogRouter(2, new PrintStream(out), Level.INFO, createFailsafeLayout());
        router.route(() -> false, Level.DEBUG, LOGGER_NAME, "debug message", null);

        router.activateFailsafeAndDrain();
        assertEquals("", out.toString(StandardCharsets.UTF_8),
            "Expected no failsafe output if all messages are below the minimum level");
    }

    @Test
    void testFailsafeDrainEmitsEvictionNoticeAndMatchingMessages() {
        final var out = new ByteArrayOutputStream();
        final var router = new StartupLogRouter(2, new PrintStream(out), Level.INFO, createFailsafeLayout());
        router.route(() -> false, Level.ERROR, LOGGER_NAME, "first", null);
        router.route(() -> false, Level.INFO, LOGGER_NAME, "second", null);
        router.route(() -> false, Level.WARN, LOGGER_NAME, "third", null);

        router.activateFailsafeAndDrain();
        final var dump = out.toString(StandardCharsets.UTF_8);

        assertTrue(dump.contains("3 messages were buffered."),
            "Expected failsafe dump header");
        assertTrue(dump.contains("evicted from buffer in total"), "Expected eviction notice");
        assertTrue(dump.contains("second"), "Expected INFO message in dump");
        assertTrue(dump.contains("third"), "Expected WARN message in dump");
        assertFalse(dump.contains("first"), "Expected evicted message to be absent");
        assertTrue(dump.contains("End of KNIME startup failsafe logging dump."),
            "Expected failsafe dump footer");
    }

    @Test
    void testFailsafeEmitSupplierDoesNotEvaluateBelowMinLevel() {
        final var evaluated = new AtomicBoolean();
        final var out = new ByteArrayOutputStream();
        final var router = new StartupLogRouter(2, new PrintStream(out), Level.INFO, createFailsafeLayout());
        router.activateFailsafeAndDrain();

        router.route(() -> false, Level.DEBUG, LOGGER_NAME, () -> {
                evaluated.set(true);
                return "message";
            }, null);

        assertFalse(evaluated.get(), "Supplier should not be evaluated below the minimum failsafe level");
        assertEquals("", out.toString(StandardCharsets.UTF_8), "Expected no failsafe output below minimum level");
    }

    @Test
    void testFailsafeEmitPrintsThrowableIfLayoutIgnoresIt() {
        final var out = new ByteArrayOutputStream();
        final var ex = new IllegalStateException("boom");
        final var router = new StartupLogRouter(2, new PrintStream(out), Level.DEBUG, createThrowableIgnoringLayout());
        router.activateFailsafeAndDrain();

        router.route(() -> false, Level.ERROR, LOGGER_NAME, "message", ex);

        final var emitted = out.toString(StandardCharsets.UTF_8);
        assertTrue(emitted.contains("message"), "Expected formatted message output");
        assertTrue(emitted.contains("IllegalStateException: boom"), "Expected throwable stack trace output");
    }

    @Test
    void testDrainToLoggerWithEmptyBufferDoesNotInvokeConsumer() {
        final var router = new StartupLogRouter(2);
        final var called = new AtomicBoolean();
        router.drainToLogger(m -> called.set(true));
        assertFalse(called.get(), "Consumer should not be called when draining an empty buffer");
    }

    @Test
    void testRouteReturnsFalseAfterLoggerDrain() {
        final var router = new StartupLogRouter(2);
        router.route(() -> false, Level.DEBUG, LOGGER_NAME, "before drain", null);
        router.drainToLogger(m -> { /* discard */ });
        assertFalse(router.route(() -> false, Level.DEBUG, LOGGER_NAME, "after drain", null),
            "Expected route() to return false (unhandled) after buffer has been drained to logger");
    }

    @Test
    void testFailsafeEmitSupplierAboveMinLevelEmitsMessage() {
        final var evaluated = new AtomicBoolean();
        final var out = new ByteArrayOutputStream();
        final var router = new StartupLogRouter(2, new PrintStream(out), Level.DEBUG, createFailsafeLayout());
        router.activateFailsafeAndDrain();

        router.route(() -> false, Level.INFO, LOGGER_NAME, () -> {
                evaluated.set(true);
                return "supplier message";
            }, null);

        assertTrue(evaluated.get(), "Supplier should be evaluated at or above the minimum failsafe level");
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("supplier message"),
            "Expected supplier message in failsafe output");
    }

    @Test
    void testFailsafeDrainEmitsHeaderLazilyOnFirstQualifyingMessage() {
        final var out = new ByteArrayOutputStream();
        // buffer size > number of messages, so no eviction
        final var router = new StartupLogRouter(4, new PrintStream(out), Level.INFO, createFailsafeLayout());
        router.route(() -> false, Level.INFO, LOGGER_NAME, "first", null);
        router.route(() -> false, Level.INFO, LOGGER_NAME, "second", null);

        router.activateFailsafeAndDrain();
        final var dump = out.toString(StandardCharsets.UTF_8);

        assertTrue(dump.contains("2 messages were buffered."), "Expected header in dump");
        assertFalse(dump.contains("evicted"), "Expected no eviction notice without overflow");
        assertTrue(dump.contains("first"), "Expected first message in dump");
        assertTrue(dump.contains("second"), "Expected second message in dump");
    }

    @Test
    void testResolveFailsafeTargetOffDisablesFailsafe() {
        assertNull(StartupLogRouter.resolveFailsafeTarget("off", EclipseUtil.Application.AP),
            "Expected null for \"off\" (AP)");
        assertNull(StartupLogRouter.resolveFailsafeTarget("OFF", EclipseUtil.Application.EXECUTOR),
            "Expected null for \"OFF\" (EXECUTOR)");
        assertNull(StartupLogRouter.resolveFailsafeTarget("Off", EclipseUtil.Application.UNKNOWN),
            "Expected null for \"Off\" (UNKNOWN)");
    }

    @Test
    void testResolveFailsafeTargetStderrWritesToStderr() {
        assertEquals(System.err, StartupLogRouter.resolveFailsafeTarget("stderr", EclipseUtil.Application.UNKNOWN),
            "Expected System.err for \"stderr\"");
        assertEquals(System.err, StartupLogRouter.resolveFailsafeTarget("STDERR", EclipseUtil.Application.UNKNOWN),
            "Expected System.err for \"STDERR\" (case-insensitive)");
    }

    @Test
    void testResolveFailsafeTargetStdoutWritesToStdout() {
        assertEquals(System.out, StartupLogRouter.resolveFailsafeTarget("stdout", EclipseUtil.Application.UNKNOWN),
            "Expected System.out for \"stdout\"");
        assertEquals(System.out, StartupLogRouter.resolveFailsafeTarget("any-other-value", EclipseUtil.Application.UNKNOWN),
            "Expected System.out for any non-special non-blank value");
    }

    @Test
    void testResolveFailsafeTargetNullEnablesDefaultByApplication() {
        // enabled by default for AP and EXECUTOR
        assertEquals(System.out, StartupLogRouter.resolveFailsafeTarget(null, EclipseUtil.Application.AP),
            "Expected System.out by default for AP");
        assertEquals(System.out, StartupLogRouter.resolveFailsafeTarget(null, EclipseUtil.Application.EXECUTOR),
            "Expected System.out by default for EXECUTOR");
        // disabled by default for all other applications
        assertNull(StartupLogRouter.resolveFailsafeTarget(null, EclipseUtil.Application.UNKNOWN),
            "Expected null (disabled) by default for UNKNOWN");
    }

    @Test
    void testResolveFailsafeTargetBlankEnablesDefaultByApplication() {
        // blank is treated the same as unset: enabled for AP and EXECUTOR, disabled for others
        assertEquals(System.out, StartupLogRouter.resolveFailsafeTarget("", EclipseUtil.Application.AP),
            "Expected System.out for blank value with AP");
        assertEquals(System.out, StartupLogRouter.resolveFailsafeTarget("  ", EclipseUtil.Application.EXECUTOR),
            "Expected System.out for whitespace-only value with EXECUTOR");
        assertNull(StartupLogRouter.resolveFailsafeTarget("", EclipseUtil.Application.UNKNOWN),
            "Expected null (disabled) for blank value with UNKNOWN");
    }

    @Test
    void testFailsafeDisabledProducesNoOutput() {
        final var router = new StartupLogRouter(2, null, Level.DEBUG, createFailsafeLayout());
        router.route(() -> false, Level.ERROR, LOGGER_NAME, "message", null);
        router.activateFailsafeAndDrain(); // no-op: failsafe is disabled
        // message is still in the buffer since the disabled failsafe did not drain it
        final var wasDrained = new AtomicBoolean();
        router.drainToLogger(m -> wasDrained.set(true));
        assertTrue(wasDrained.get(), "Expected message to stay buffered until explicitly drained to the logger");
    }

    @Test
    void testFailsafeDisabledDoesNotDrainBuffer() {
        final var router = new StartupLogRouter(2, null, Level.DEBUG, createFailsafeLayout());
        router.route(() -> false, Level.WARN, LOGGER_NAME, "first", null);
        router.activateFailsafeAndDrain(); // no-op when disabled
        // buffer is still in BUFFER state, so further route() calls still buffer
        assertTrue(router.route(() -> false, Level.ERROR, LOGGER_NAME, "second", null),
            "Expected route() to still buffer messages after disabled failsafe activation");
    }

    private static Layout createFailsafeLayout() {
        final var layout = new org.knime.core.node.NodeLoggerPatternLayout();
        layout.setConversionPattern("%d{ISO8601} : %-5p : %t : %c{1} : %m%n");
        layout.activateOptions();
        return layout;
    }

    private static Layout createThrowableIgnoringLayout() {
        return new Layout() {
            @Override
            public void activateOptions() {
            }

            @Override
            public String format(final org.apache.log4j.spi.LoggingEvent event) {
                return event.getMessage() + System.lineSeparator();
            }

            @Override
            public boolean ignoresThrowable() {
                return true;
            }
        };
    }
}
