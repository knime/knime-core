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

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;
import org.slf4j.helpers.MessageFormatter;

/**
 * A copy of org.slf4j.impl.Log4jLoggerAdapter.class, adjusted to delegate to {@link NodeLogger}. Additional references
 * to the source are provided in this package's description or the accompanying LICENSE.txt.
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
public final class NodeLoggerLoggerAdapter extends AbstractLogger {

    private static final long serialVersionUID = 6182834493563598289L;

    final transient NodeLogger m_nodeLogger;

    // WARN: Log4jLoggerAdapter constructor should have only package access so
    // that
    // only Log4jLoggerFactory be able to create one.
    NodeLoggerLoggerAdapter(final String nameArg, final NodeLogger logger) {
        this.m_nodeLogger = logger;
        this.name = nameArg;
    }

    /**
     * Is this logger instance enabled for the TRACE level?
     *
     * @return True if this Logger is enabled for level TRACE, false otherwise.
     */
    @Override
    public boolean isTraceEnabled() {
        return m_nodeLogger.isDebugEnabled();
    }

    @Override
    public boolean isTraceEnabled(final Marker marker) {
        return isTraceEnabled();
    }

    @Override
    public boolean isDebugEnabled() {
        return m_nodeLogger.isDebugEnabled();
    }

    @Override
    public boolean isDebugEnabled(final Marker marker) {
        return isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return m_nodeLogger.isInfoEnabled();
    }

    @Override
    public boolean isInfoEnabled(final Marker marker) {
        return isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return m_nodeLogger.isEnabledFor(LEVEL.WARN);
    }

    @Override
    public boolean isWarnEnabled(final Marker marker) {
        return isWarnEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return m_nodeLogger.isEnabledFor(LEVEL.ERROR);
    }

    @Override
    public boolean isErrorEnabled(final Marker marker) {
        return isErrorEnabled();
    }

    @Override
    protected void handleNormalizedLoggingCall(final Level level, final Marker marker, final String messagePattern,
        final Object[] arguments, final Throwable throwable) {
        // simple SLF4J formatting
        final String formatted = ArrayUtils.isNotEmpty(arguments) ?
            MessageFormatter.basicArrayFormat(messagePattern, arguments) : messagePattern;
        switch (level) {
            case TRACE:
                m_nodeLogger.debug(formatted, throwable);
                break;
            case DEBUG:
                m_nodeLogger.debug(formatted, throwable);
                break;
            case INFO:
                m_nodeLogger.info(formatted, throwable);
                break;
            case WARN:
                m_nodeLogger.warn(formatted, throwable);
                break;
            case ERROR:
                m_nodeLogger.error(formatted, throwable);
                break;
            default:
                throw new IllegalStateException("Unexpected SLF4J level: " + level);
        }
    }

    @Override
    protected String getFullyQualifiedCallerName() {
        return this.name; // this method is not actually used within KNIME AP
    }
}
