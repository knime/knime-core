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
 *   Oct 12, 2020 (wiswedel): created
 */
package org.knime.slf4j.binding;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.Writer;
import java.util.Objects;

import org.apache.commons.io.output.NullWriter;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;

/**
 * A junit test rule that inspects if a provided log message is actually received.
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
final class ExpectedLogMessage implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private final Writer m_writer;

    private String m_expectedMessage;
    private LEVEL m_expectedLevel;
    private Throwable m_expectedThrowable;

    private String m_firstSeenLogMessage;
    private LEVEL m_firstSeenLogMessageType;
    private Throwable m_firstSeenLoggedThrowable;

    private ExpectedLogMessage() {
        m_writer = new NullWriter();
        NodeLogger.addWriter(m_writer, new LayoutExtension(), LEVEL.DEBUG, LEVEL.FATAL);
    }

    void expect(final LEVEL level, final String message) {
        expect(level, message, null);
    }

    void expectNone() {
    }

    void expect(final LEVEL level, final String message, final Throwable throwable) {
        m_expectedLevel = Objects.requireNonNull(level);
        m_expectedMessage = Objects.requireNonNull(message);
        m_expectedThrowable = throwable;
    }

    static ExpectedLogMessage newInstance() {
        return new ExpectedLogMessage();
    }

    @Override
    public void beforeEach(final ExtensionContext context) throws Exception {
        // setup log capture
        NodeLogger.addWriter(m_writer, new LayoutExtension(), LEVEL.DEBUG, LEVEL.FATAL);
    }

    @Override
    public void afterEach(final ExtensionContext context) throws Exception {
        try {
            // verify expectations
            if (m_expectedMessage == null && m_firstSeenLogMessage != null) {
                throw new AssertionError("Expected no log, but saw: " + m_firstSeenLogMessage);
            }
            assertThat("Type of message", m_firstSeenLogMessageType, is(m_expectedLevel));
            assertThat("Message", m_firstSeenLogMessage, is(m_expectedMessage));
            assertThat("Message Throwable", m_firstSeenLoggedThrowable, is(m_expectedThrowable));
        } finally {
            NodeLogger.removeWriter(m_writer);
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext pc, ExtensionContext ec) {
        return pc.getParameter().getType() == ExpectedLogMessage.class;
    }

    @Override
    public Object resolveParameter(ParameterContext pc, ExtensionContext ec) {
        return this;
    }

    private final class LayoutExtension extends Layout {

        @Override
        public void activateOptions() {
            // no op
        }

        @Override
        public boolean ignoresThrowable() {
            return false;
        }

        @Override
        public String format(final LoggingEvent log) {
            m_firstSeenLogMessageType = toNodeLoggerLEVEL(log);
            m_firstSeenLogMessage = Objects.toString(log.getMessage());
            m_firstSeenLoggedThrowable =
                log.getThrowableInformation() != null ? log.getThrowableInformation().getThrowable() : null;
            return String.format("%s: %s", m_firstSeenLogMessageType, log.getMessage());
        }

        private LEVEL toNodeLoggerLEVEL(final LoggingEvent log) {
            LEVEL nodeLoggerLevel;
            switch (log.getLevel().toInt()) {
                case Level.TRACE_INT:
                    nodeLoggerLevel = LEVEL.DEBUG;
                    break;
                case Priority.DEBUG_INT:
                    nodeLoggerLevel = LEVEL.DEBUG;
                    break;
                case Priority.INFO_INT:
                    nodeLoggerLevel = LEVEL.INFO;
                    break;
                case Priority.WARN_INT:
                    nodeLoggerLevel = LEVEL.WARN;
                    break;
                case Priority.ERROR_INT:
                    nodeLoggerLevel = LEVEL.ERROR;
                    break;
                case Priority.FATAL_INT:
                    nodeLoggerLevel = LEVEL.FATAL;
                    break;
                default:
                    throw new IllegalStateException("unsupported level :" + log.getLevel());
            }
            return nodeLoggerLevel;
        }
    }
}