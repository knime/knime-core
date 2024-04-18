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

import org.apache.commons.io.output.NullWriter;
import org.apache.log4j.Level;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;
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
}
