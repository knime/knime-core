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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayDeque;
import java.util.Deque;

import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Test log interceptor recording added messages on a stack for later assertion of top/bottom message. Only intercepts
 * logging events emitted by the logger specified in the constructor.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
class LogInterceptor extends Layout {

    record LogMsg(Level level, String msg) {
    }

    private final Deque<LogMsg> m_stack = new ArrayDeque<>();

    private final String m_loggerName;

    /**
     * Create a new log event interceptor.
     *
     * @param loggerName logger name whose {@link LoggingEvent}s should be intercepted and added to the stack
     */
    LogInterceptor(final String loggerName) {
        m_loggerName = loggerName;
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

        if (m_loggerName.equals(event.getLoggerName())) {
            m_stack.add(new LogMsg(level, msg));
        }

        return String.format("%s: %s", level, msg);
    }

    private void expectLogMessageEquals(final boolean first, final Level level, final String msg) {
        assertThat(m_stack.isEmpty()) //
            .as(() -> "Expected log stack to contain at least one log message, but it is empty") //
            .isFalse();
        final var log = first ? m_stack.peekFirst() : m_stack.peekLast();
        final var s = first ? "first" : "last";
        assertThat(msg) //
            .as(() -> "Unexpected %s log message".formatted(s)) //
            .isEqualTo(log.msg);
        assertThat(level) //
            .as(() -> "Unexpected level for %s log message".formatted(s)) //
            .isEqualTo(log.level);
    }

    void assertLastLogMessageEquals(final Level level, final String expected) {
        expectLogMessageEquals(false, level, expected);
    }

    void assertFirstLogMessageEquals(final Level level, final String expected) {
        expectLogMessageEquals(true, level, expected);
    }

    boolean isEmpty() {
        return m_stack.isEmpty();
    }
}
