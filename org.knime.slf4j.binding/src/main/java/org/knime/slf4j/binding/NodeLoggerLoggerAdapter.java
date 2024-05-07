/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.slf4j.binding;

import java.io.Serializable;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;
import org.slf4j.Marker;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.spi.LocationAwareLogger;

/**
 * A copy of org.slf4j.impl.Log4jLoggerAdapter.class, adjusted to delegate to {@link NodeLogger}. Additional references
 * to the source are provided in this package's description or the accompanying LICENSE.txt.
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
public final class NodeLoggerLoggerAdapter extends MarkerIgnoringBase implements
LocationAwareLogger, Serializable {

    private static final long serialVersionUID = 6182834493563598289L;

    final transient NodeLogger m_nodeLogger;

    // WARN: Log4jLoggerAdapter constructor should have only package access so
    // that
    // only Log4jLoggerFactory be able to create one.
    NodeLoggerLoggerAdapter(final String name, final NodeLogger logger) {
        this.m_nodeLogger = logger;
        this.name = name;
    }

    /**
     * Is this logger instance enabled for the TRACE level?
     *
     * @return True if this Logger is enabled for level TRACE, false otherwise.
     */
    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    /**
     * Log a message object at level TRACE.
     *
     * @param msg
     *          - the message object to be logged
     */
    @Override
    public void trace(final String msg) {
        debug(msg);
    }

    /**
     * Log a message at level TRACE according to the specified format and
     * argument.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for level TRACE.
     * </p>
     *
     * @param format
     *          the format string
     * @param arg
     *          the argument
     */
    @Override
    public void trace(final String format, final Object arg) {
        if (isDebugEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            m_nodeLogger.debug(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level TRACE according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the TRACE level.
     * </p>
     *
     * @param format
     *          the format string
     * @param arg1
     *          the first argument
     * @param arg2
     *          the second argument
     */
    @Override
    public void trace(final String format, final Object arg1, final Object arg2) {
        if (isDebugEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
            m_nodeLogger.debug(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level TRACE according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the TRACE level.
     * </p>
     *
     * @param format
     *          the format string
     * @param arguments
     *          an array of arguments
     */
    @Override
    public void trace(final String format, final Object... arguments) {
        if (isDebugEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
            m_nodeLogger.debug(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at level TRACE with an accompanying message.
     *
     * @param msg
     *          the message accompanying the exception
     * @param t
     *          the exception (throwable) to log
     */
    @Override
    public void trace(final String msg, final Throwable t) {
        m_nodeLogger.debug(msg, t);
    }

    /**
     * Is this logger instance enabled for the DEBUG level?
     *
     * @return True if this Logger is enabled for level DEBUG, false otherwise.
     */
    @Override
    public boolean isDebugEnabled() {
        return m_nodeLogger.isDebugEnabled();
    }

    /**
     * Log a message object at level DEBUG.
     *
     * @param msg
     *          - the message object to be logged
     */
    @Override
    public void debug(final String msg) {
        m_nodeLogger.debug(msg);
    }

    /**
     * Log a message at level DEBUG according to the specified format and
     * argument.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for level DEBUG.
     * </p>
     *
     * @param format
     *          the format string
     * @param arg
     *          the argument
     */
    @Override
    public void debug(final String format, final Object arg) {
        if (m_nodeLogger.isDebugEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            m_nodeLogger.debug(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level DEBUG according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the DEBUG level.
     * </p>
     *
     * @param format
     *          the format string
     * @param arg1
     *          the first argument
     * @param arg2
     *          the second argument
     */
    @Override
    public void debug(final String format, final Object arg1, final Object arg2) {
        if (m_nodeLogger.isDebugEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
            m_nodeLogger.debug(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level DEBUG according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the DEBUG level.
     * </p>
     *
     * @param format
     *          the format string
     * @param arguments an array of arguments
     */
    @Override
    public void debug(final String format, final Object... arguments) {
        if (m_nodeLogger.isDebugEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
            m_nodeLogger.debug(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at level DEBUG with an accompanying message.
     *
     * @param msg
     *          the message accompanying the exception
     * @param t
     *          the exception (throwable) to log
     */
    @Override
    public void debug(final String msg, final Throwable t) {
        m_nodeLogger.debug(msg, t);
    }

    /**
     * Is this logger instance enabled for the INFO level?
     *
     * @return True if this Logger is enabled for the INFO level, false otherwise.
     */
    @Override
    public boolean isInfoEnabled() {
        return m_nodeLogger.isInfoEnabled();
    }

    /**
     * Log a message object at the INFO level.
     *
     * @param msg
     *          - the message object to be logged
     */
    @Override
    public void info(final String msg) {
        m_nodeLogger.info(msg, null);
    }

    /**
     * Log a message at level INFO according to the specified format and argument.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the INFO level.
     * </p>
     *
     * @param format
     *          the format string
     * @param arg
     *          the argument
     */
    @Override
    public void info(final String format, final Object arg) {
        if (m_nodeLogger.isInfoEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            m_nodeLogger.info(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at the INFO level according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the INFO level.
     * </p>
     *
     * @param format
     *          the format string
     * @param arg1
     *          the first argument
     * @param arg2
     *          the second argument
     */
    @Override
    public void info(final String format, final Object arg1, final Object arg2) {
        if (m_nodeLogger.isInfoEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
            m_nodeLogger.info(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level INFO according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the INFO level.
     * </p>
     *
     * @param format
     *          the format string
     * @param argArray
     *          an array of arguments
     */
    @Override
    public void info(final String format, final Object... argArray) {
        if (m_nodeLogger.isInfoEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
            m_nodeLogger.info(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at the INFO level with an accompanying
     * message.
     *
     * @param msg
     *          the message accompanying the exception
     * @param t
     *          the exception (throwable) to log
     */
    @Override
    public void info(final String msg, final Throwable t) {
        m_nodeLogger.info(msg, t);
    }

    /**
     * Is this logger instance enabled for the WARN level?
     *
     * @return True if this Logger is enabled for the WARN level, false otherwise.
     */
    @Override
    public boolean isWarnEnabled() {
        return m_nodeLogger.isEnabledFor(LEVEL.WARN);
    }

    /**
     * Log a message object at the WARN level.
     *
     * @param msg
     *          - the message object to be logged
     */
    @Override
    public void warn(final String msg) {
        m_nodeLogger.warn(msg, null);
    }

    /**
     * Log a message at the WARN level according to the specified format and
     * argument.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the WARN level.
     * </p>
     *
     * @param format
     *          the format string
     * @param arg
     *          the argument
     */
    @Override
    public void warn(final String format, final Object arg) {
        if (isWarnEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            m_nodeLogger.warn(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at the WARN level according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the WARN level.
     * </p>
     *
     * @param format
     *          the format string
     * @param arg1
     *          the first argument
     * @param arg2
     *          the second argument
     */
    @Override
    public void warn(final String format, final Object arg1, final Object arg2) {
        if (isWarnEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
            m_nodeLogger.warn(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level WARN according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the WARN level.
     * </p>
     *
     * @param format
     *          the format string
     * @param argArray
     *          an array of arguments
     */
    @Override
    public void warn(final String format, final Object... argArray) {
        if (isWarnEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
            m_nodeLogger.warn(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at the WARN level with an accompanying
     * message.
     *
     * @param msg
     *          the message accompanying the exception
     * @param t
     *          the exception (throwable) to log
     */
    @Override
    public void warn(final String msg, final Throwable t) {
        m_nodeLogger.warn(msg, t);
    }

    /**
     * Is this logger instance enabled for level ERROR?
     *
     * @return True if this Logger is enabled for level ERROR, false otherwise.
     */
    @Override
    public boolean isErrorEnabled() {
        return m_nodeLogger.isEnabledFor(LEVEL.ERROR);
    }

    /**
     * Log a message object at the ERROR level.
     *
     * @param msg
     *          - the message object to be logged
     */
    @Override
    public void error(final String msg) {
        m_nodeLogger.error(msg, null);
    }

    /**
     * Log a message at the ERROR level according to the specified format and
     * argument.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the ERROR level.
     * </p>
     *
     * @param format
     *          the format string
     * @param arg
     *          the argument
     */
    @Override
    public void error(final String format, final Object arg) {
        if (isErrorEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            m_nodeLogger.error(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at the ERROR level according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the ERROR level.
     * </p>
     *
     * @param format
     *          the format string
     * @param arg1
     *          the first argument
     * @param arg2
     *          the second argument
     */
    @Override
    public void error(final String format, final Object arg1, final Object arg2) {
        if (isErrorEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
            m_nodeLogger.error(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level ERROR according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the ERROR level.
     * </p>
     *
     * @param format
     *          the format string
     * @param argArray
     *          an array of arguments
     */
    @Override
    public void error(final String format, final Object... argArray) {
        if (isErrorEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
            m_nodeLogger.error(ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at the ERROR level with an accompanying
     * message.
     *
     * @param msg
     *          the message accompanying the exception
     * @param t
     *          the exception (throwable) to log
     */
    @Override
    public void error(final String msg, final Throwable t) {
        m_nodeLogger.error(msg, t);
    }

    @Override
    public void log(final Marker marker, final String callerFQCN, final int level, final String msg,
        final Object[] argArray, final Throwable t) {
        switch (level) {
            case LocationAwareLogger.TRACE_INT:
                m_nodeLogger.debug(msg, t);
                break;
            case LocationAwareLogger.DEBUG_INT:
                m_nodeLogger.debug(msg, t);
                break;
            case LocationAwareLogger.INFO_INT:
                m_nodeLogger.info(msg, t);
                break;
            case LocationAwareLogger.WARN_INT:
                m_nodeLogger.warn(msg, t);
                break;
            case LocationAwareLogger.ERROR_INT:
                m_nodeLogger.error(msg, t);
                break;
            default:
                throw new IllegalStateException("Level number " + level
                    + " is not recognized.");
        }
    }

}
