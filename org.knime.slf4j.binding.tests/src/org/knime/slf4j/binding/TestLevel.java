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
 *   Oct 13, 2020 (wiswedel): created
 */
package org.knime.slf4j.binding;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;
import org.slf4j.Logger;

/** Abstracts methods on different log levels. */
enum TestLevel {

    TEST_TRACE(//
        Logger::trace, // (these all mean different methods -- Ctrl+click to resolve)
        Logger::trace, //
        Logger::trace, //
        Logger::trace, //
        Logger::trace, //
        LEVEL.DEBUG),

    TEST_DEBUG(//
        Logger::debug, //
        Logger::debug, //
        Logger::debug, //
        Logger::debug, //
        Logger::debug, //
        LEVEL.DEBUG),

    TEST_INFO(//
        Logger::info, //
        Logger::info, //
        Logger::info, //
        Logger::info, //
        Logger::info, //
        LEVEL.INFO),

    TEST_WARN(//
        Logger::warn, //
        Logger::warn, //
        Logger::warn, //
        Logger::warn, //
        Logger::warn, //
        LEVEL.WARN),

    TEST_ERROR(//
        Logger::error, //
        Logger::error, //
        Logger::error, //
        Logger::error, //
        Logger::error, //
        LEVEL.ERROR);

    private final MessageOnlyMethod m_messageOnlyMethod;
    private final MessageOneArgMethod m_messageOneArgMethod;
    private final MessageTwoArgsMethod m_messageTwoArgsMethod;
    private final MessageArrayArgMethod m_messageArrayArgMethod;
    private final MessageAndExceptionMethod m_messageAndThrowableMethod;
    private final LEVEL m_nodeLoggerLevel;

    private TestLevel( //
        final MessageOnlyMethod messageOnlyMethod, //
        final MessageOneArgMethod messageOneArgMethod, //
        final MessageTwoArgsMethod messageTwoArgsMethod, //
        final MessageArrayArgMethod messageArrayArgMethod, //
        final MessageAndExceptionMethod messageAndThrowableMethod, //
        final NodeLogger.LEVEL nodeLoggerLevel) {
        m_messageOnlyMethod = messageOnlyMethod;
        m_messageAndThrowableMethod = messageAndThrowableMethod;
        m_messageOneArgMethod = messageOneArgMethod;
        m_messageTwoArgsMethod = messageTwoArgsMethod;
        m_messageArrayArgMethod = messageArrayArgMethod;
        m_nodeLoggerLevel = nodeLoggerLevel;
    }

    MessageOnlyMethod messageOnly() {
        return m_messageOnlyMethod;
    }

    MessageOneArgMethod messageOneArg() {
        return m_messageOneArgMethod;
    }

    MessageTwoArgsMethod messageTwoArgs() {
        return m_messageTwoArgsMethod;
    }

    MessageArrayArgMethod messageArrayArg() {
        return m_messageArrayArgMethod;
    }

    MessageAndExceptionMethod messageAndException() {
        return m_messageAndThrowableMethod;
    }

    LEVEL getNodeLoggerLevel() {
        return m_nodeLoggerLevel;
    }

    @FunctionalInterface
    interface MessageOnlyMethod { // NOSONAR
        void logMessage(final Logger logger, final String message);
    }

    @FunctionalInterface
    interface MessageOneArgMethod {
        void logMessage(final Logger logger, final String message, final String arg);
    }

    @FunctionalInterface
    interface MessageTwoArgsMethod {
        void logMessage(final Logger logger, final String message, final String arg1, String arg2);
    }

    @FunctionalInterface
    interface MessageArrayArgMethod {
        void logMessage(final Logger logger, final String message, String[] args);
    }

    @FunctionalInterface
    interface MessageAndExceptionMethod {
        void logMessage(final Logger logger, final String message, final Throwable throwable);
    }
}
