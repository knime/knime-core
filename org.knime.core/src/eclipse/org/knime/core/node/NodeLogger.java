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
package org.knime.core.node;

import java.io.Writer;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.helpers.LogLog;
import org.knime.core.node.logging.KNIMELogger;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.util.Pair;

/**
 * The general logger used to write info, warnings, errors , debugging, assert
 * messages, exceptions, and coding problems into the internal Log4J logger. The
 * loggers are configured by the <code>log4j.properties</code> file in the
 * root of the core package. The configuration can be overridden by specifying a
 * file in <code>-Dlog4j.configuration</code> (this is the standard log4j
 * behaviour). Furthermore, it is possible to add and remove additional writers
 * to this logger. Note, calling {@link #setLevel(LEVEL)} does only effect
 * the minimum logging level of the default loggers. All other writers' levels
 * have to be set before hand.
 *
 * @author Thomas Gabriel, Tobias Koetter, KNIME.com
 */
public final class NodeLogger {

    // functionality lives in the logging package now, this is a very shallow facade

    /** The logging levels. */
    public enum LEVEL {
        /** includes debug and more critical messages. */
        DEBUG,
        /** includes infos and more critical messages. */
        INFO,
        /** includes warnings and more critical messages. */
        WARN,
        /** includes error and more critical messages. */
        ERROR,
        /** includes fatal and more critical messages. */
        FATAL,
        /** includes all messages. */
        ALL,
        /** includes no messages.
         * @since 5.3
         * */
        /* Added to make the Log4j level of the same name, which can be set in the XML config, visible in the
         * preferences page. */
        OFF
    }

    /**
     * Name of the default appender to System.out.
     *
     * @since 2.8
     */
    public static final String STDOUT_APPENDER = "stdout";

    /**
     * Name of the default appender to System.err.
     *
     * @since 2.8
     */
    public static final String STDERR_APPENDER = "stderr";

    /**
     * Name of the default appender to the log file.
     *
     * @since 2.8
     */
    public static final String LOGFILE_APPENDER = "logfile";

    /**
     * Name of the default appender to the KNIME console.
     *
     * @since 2.12
     */
    public static final String KNIME_CONSOLE_APPENDER = "knimeConsole";

    /** The default log file name, <i>knime.log</i>. */
    public static final String LOG_FILE = "knime.log";


    /**
     * Initializes logger only if the instance location (i.e. workspace) is already set.
     */
    static {
        // this ensures that code which uses the node logger but does not explicitly initialize logging still works
        // as before
        KNIMELogger.safeInitializeLogging();
    }

    private final KNIMELogger m_logger;

    private NodeLogger(final String name) {
        m_logger = KNIMELogger.getLogger(name);
    }

    /**
     * Creates a new <code>NodeLogger</code> for the given Class.
     *
     * @param c The logger's Class.
     * @return A new logger for this Class.
     */
    public static NodeLogger getLogger(final Class<?> c) {
        return getLogger(c.getName());
    }

    /**
     * Creates a new <code>NodeLogger</code> for the given name.
     *
     * @param s The logger's String.
     * @return A new logger for the given name.
     */
    public static NodeLogger getLogger(final String s) {
        return new NodeLogger(s);
    }

    /**
     * Write warning message into this logger.
     *
     * @param o The object to print.
     */
    public void warn(final Object o) {
        log(Level.WARN, o, null);
    }

    /**
     * Write warning message into this logger.
     *
     * @param supplier Supplier for the object to print.
     * @since 5.2
     */
    public void warn(final Supplier<Object> supplier) {
        log(Level.WARN, supplier, null);
    }

    /**
     * Write debugging message into this logger.
     *
     * @param o The object to print.
     */
    public void debug(final Object o) {
        log(Level.DEBUG, o, null);
    }

    /**
     * Write debugging message into this logger.
     *
     * @param supplier Supplier for the object to print.
     * @since 5.2
     */
    public void debug(final Supplier<Object> supplier) {
        log(Level.DEBUG, supplier, null);
    }


    /**
     * Write debugging message into this logger. The message is logged without a node context. This method should only
     * be used when you know that there is no node context available.
     *
     * @param o The object to print.
     * @since 3.1
     */
    public void debugWithoutContext(final Object o) {
        log(Level.DEBUG, o, null, false);
    }


    /**
     * Write debugging message into this logger. The message is logged without a node context. This method should only
     * be used when you know that there is no node context available.
     *
     * @param supplier Supplier for the object to print.
     * @since 5.2
     */
    public void debugWithoutContext(final Supplier<Object> supplier) {
        log(Level.DEBUG, supplier, null, false);
    }


    /**
     * Log the given object at the given level.
     *
     * @param level level to log at
     * @param o object to log
     * @param cause nullable cause
     */
    private void log(final Level level, final Object o, final Throwable cause) {
        log(level, o, cause, true);
    }

    /**
     * Log the given object at the given level.
     *
     * @param level level to log at
     * @param supplier supplier for object to log
     * @param cause nullable cause
     */
    private void log(final Level level, final Supplier<Object> supplier, final Throwable cause) {
        log(level, supplier, cause, true);
    }

    /**
     * Log the given object at the given level.
     *
     * @param level level to log at
     * @param logObject object to log
     * @param cause nullable cause
     * @param considerWFDirAppenders whether to consider setting up workflow dir appenders
     */
    private void log(final Level level, final Object o, final Throwable cause, final boolean considerWFDirAppenders) {
        m_logger.log(level, o, cause, considerWFDirAppenders);
    }

    private void log(final Level level, final Supplier<Object> supplier, final Throwable cause,
        final boolean considerWFDirAppenders) {
        m_logger.log(level, supplier, cause, considerWFDirAppenders);
    }

    private void logCoding(final Object o, final Throwable cause, final boolean considerWFDirAppenders) {
        m_logger.logCoding(o, cause, considerWFDirAppenders);
    }

    private void logCoding(final Supplier<Object> supplier, final Throwable cause,
        final boolean considerWFDirAppenders) {
        m_logger.logCoding(supplier, cause, considerWFDirAppenders);
    }

    /**
     * Write info message into this logger.
     *
     * @param o The object to print.
     */
    public void info(final Object o) {
        log(Level.INFO, o, null);
    }

    /**
     * Write info message into this logger.
     *
     * @param supplier Supplier for the object to print.
     * @since 5.2
     */
    public void info(final Supplier<Object> supplier) {
        log(Level.INFO, supplier, null);
    }

    /**
     * Write error message into the logger.
     *
     * @param o The object to print.
     */
    public void error(final Object o) {
        log(Level.ERROR, o, null);
    }

    /**
     * Write error message into the logger.
     *
     * @param supplier Supplier for the object to print.
     * @since 5.2
     */
    public void error(final Supplier<Object> supplier) {
        log(Level.ERROR, supplier, null);
    }

    /**
     * Write fatal error message into the logger.
     *
     * @param o The object to print.
     */
    public void fatal(final Object o) {
        log(Level.FATAL, o, null);
    }

    /**
     * Write fatal error message into the logger.
     *
     * @param supplier Supplier for the object to print.
     * @since 5.2
     */
    public void fatal(final Supplier<Object> supplier) {
        log(Level.FATAL, supplier, null);
    }

    /**
     * Write warning message and throwable into this logger.
     *
     * @param o The object to print.
     * @param t The exception to log at debug level, including its stack trace.
     */
    public void warn(final Object o, final Throwable t) {
        log(Level.WARN, o, t);
    }

    /**
     * Write warning message and throwable into this logger.
     *
     * @param supplier Supplier for the object to print.
     * @param t The exception to log at debug level, including its stack trace.
     * @since 5.2
     */
    public void warn(final Supplier<Object> supplier, final Throwable t) {
        log(Level.WARN, supplier, t);
    }

    /**
     * Write debugging message and throwable into this logger.
     *
     * @param o The object to print.
     * @param t The exception to log, including its stack trace.
     */
    public void debug(final Object o, final Throwable t) {
        log(Level.DEBUG, o, t);
    }

    /**
     * Write debugging message and throwable into this logger.
     *
     * @param supplier Supplier for the object to print.
     * @param t The exception to log, including its stack trace.
     * @since 5.2
     */
    public void debug(final Supplier<Object> supplier, final Throwable t) {
        log(Level.DEBUG, supplier, t);
    }

    /**
     * Write info message and throwable into this logger.
     *
     * @param o The object to print.
     * @param t The exception to log at debug level, including its stack trace.
     */
    public void info(final Object o, final Throwable t) {
        log(Level.INFO, o, t);
    }

    /**
     * Write info message and throwable into this logger.
     *
     * @param supplier Supplier for the object to print.
     * @param t The exception to log at debug level, including its stack trace.
     * @since 5.2
     */
    public void info(final Supplier<Object> supplier, final Throwable t) {
        log(Level.INFO, supplier, t);
    }

    /**
     * Write error message and throwable into the logger.
     *
     * @param o The object to print.
     * @param t The exception to log at debug level, including its stack trace.
     */
    public void error(final Object o, final Throwable t) {
        log(Level.ERROR, o, t);
    }

    /**
     * Write error message and throwable into the logger.
     *
     * @param supplier Supplier for the object to print.
     * @param t The exception to log at debug level, including its stack trace.
     * @since 5.2
     */
    public void error(final Supplier<Object> supplier, final Throwable t) {
        log(Level.ERROR, supplier, t);
    }

    /**
     * Check assert and write into logger if failed.
     *
     * @param b The expression to check.
     * @param m Print this message if failed.
     */
    public void assertLog(final boolean b, final String m) {
        if (KNIMEConstants.ASSERTIONS_ENABLED && !b) {
            log(Level.ERROR, "ASSERT " + m, new AssertionError(m));
        }
    }

    /**
     * Check assertions on/off and write debug message into logger.
     *
     * @param b The expression to check.
     * @param m Print this message if failed.
     * @param e AssertionError which as been fired.
     */
    public void assertLog(final boolean b, final String m,
            final AssertionError e) {
        if (KNIMEConstants.ASSERTIONS_ENABLED) {
            if (!b) {
                log(Level.ERROR, "ASSERT " + m, null);
            }
            // for stacktrace
            if (!b && e != null) {
                log(Level.ERROR, "ASSERT\t " + m, e);
            }
        }
    }

    /**
     * Writes CODING PROBLEM plus this message into this logger as error. The event is only logged if assertions are
     * enabled or KNIME is run from within the SDK.
     *
     * @param o the message to print
     */
    public void coding(final Object o) {
        logCoding(o, null, true);
    }

    /**
     * Writes CODING PROBLEM plus this message into this logger as error. The event is only logged if assertions are
     * enabled or KNIME is run from within the SDK.
     *
     * @param supplier Supplier for the object to print.
     * @since 5.2
     */
    public void coding(final Supplier<Object> supplier) {
        logCoding(supplier.get(), null, true);
    }

    /**
     * Writes <i>CODING PROBLEM</i> plus this message, as well as the the message of the throwable into this logger as
     * error and debug. The event is only logged if assertions are enabled or KNIME is run from within the SDK.
     *
     * @param o the message to print
     * @param t the exception to log at debug level, including its stack trace
     */
    public void coding(final Object o, final Throwable t) {
        logCoding(o, t, true);
    }

    /**
     * Writes <i>CODING PROBLEM</i> plus this message, as well as the the message of the throwable into this logger as
     * error and debug. The event is only logged if assertions are enabled or KNIME is run from within the SDK.
     *
     * @param supplier Supplier for the object to print.
     * @param t the exception to log at debug level, including its stack trace
     * @since 5.2
     */
    public void coding(final Supplier<Object> supplier, final Throwable t) {
        logCoding(supplier.get(), t, true);
    }

    /**
     * Write coding message into this logger. The message is logged without a node context. This method should only
     * be used when you know that there is no node context available.
     *
     * @param o The object to print.
     * @since 4.3
     */
    public void codingWithoutContext(final Object o) {
        logCoding(o, null, false);
    }

    /**
     * Write coding message into this logger. The message is logged without a node context. This method should only
     * be used when you know that there is no node context available.
     *
     * @param supplier Supplier for the object to print.
     * @since 5.2
     */
    public void codingWithoutContext(final Supplier<Object> supplier) {
        logCoding(supplier, null, false);
    }

    /**
     * Write fatal error message and throwable into the logger.
     *
     * @param o The object to print.
     * @param t The exception to log at debug level, including its stack trace.
     */
    public void fatal(final Object o, final Throwable t) {
        log(Level.FATAL, o, t);
    }

    /**
     * Write fatal error message and throwable into the logger.
     *
     * @param supplier Supplier for the object to print.
     * @param t The exception to log at debug level, including its stack trace.
     * @since 5.2
     */
    public void fatal(final Supplier<Object> supplier, final Throwable t) {
        log(Level.FATAL, supplier, t);
    }

    /**
     * Write warning message, created by {@link String#format(String, Object...)} into this logger. The String is only
     * formatted if actually necessary.
     *
     * @param format the format for printing
     * @param args the arguments for {@link String#format(String, Object...)}
     * @throws NullPointerException if format argument is <code>null</code>
     * @since 2.10
     */
    public void warnWithFormat(final String format, final Object... args) {
        log(Level.WARN, (Supplier<Object>)() -> String.format(format, args), null);
    }

    /**
     * Write debug message, created by {@link String#format(String, Object...)} into this logger. The String is only
     * formatted if actually necessary.
     *
     * @param format the format for printing
     * @param args the arguments for {@link String#format(String, Object...)}
     * @throws NullPointerException if format argument is <code>null</code>
     * @since 2.10
     */
    public void debugWithFormat(final String format, final Object... args) {
        log(Level.DEBUG, (Supplier<Object>)() -> String.format(format, args), null);
    }

    /**
     * Write info message, created by {@link String#format(String, Object...)} into this logger. The String is only
     * formatted if actually necessary.
     *
     * @param format the format for printing
     * @param args the arguments for {@link String#format(String, Object...)}
     * @throws NullPointerException if format argument is <code>null</code>
     * @since 2.10
     */
    public void infoWithFormat(final String format, final Object... args) {
        log(Level.INFO, (Supplier<Object>)() -> String.format(format, args), null);
    }

    /**
     * Write error message, created by {@link String#format(String, Object...)} into this logger. The String is only
     * formatted if actually necessary.
     *
     * @param format the format for printing
     * @param args the arguments for {@link String#format(String, Object...)}
     * @throws NullPointerException if format argument is <code>null</code>
     * @since 2.10
     */
    public void errorWithFormat(final String format, final Object... args) {
        log(Level.ERROR, (Supplier<Object>)() -> String.format(format, args), null);
    }

    /**
     * Write fatal message, created by {@link String#format(String, Object...)} into this logger. The String is only
     * formatted if actually necessary.
     *
     * @param format the format for printing
     * @param args the arguments for {@link String#format(String, Object...)}
     * @throws NullPointerException if format argument is <code>null</code>
     * @since 2.10
     */
    public void fatalWithFormat(final String format, final Object... args) {
        log(Level.FATAL, (Supplier<Object>)() -> String.format(format, args), null);
    }

    /**
     * Write coding message, created by {@link String#format(String, Object...)} into this logger. The String is only
     * formatted if actually necessary.
     *
     * @param format the format for printing
     * @param args the arguments for {@link String#format(String, Object...)}
     * @throws NullPointerException if format argument is <code>null</code>
     * @since 2.10
     */
    public void codingWithFormat(final String format, final Object... args) {
        logCoding((Supplier<Object>)() -> String.format(format, args), null, true);
    }

    /**
     * Adds a new {@link java.io.Writer} with the given level to this logger.
     *
     * @param writer The writer to add.
     * @param minLevel The minimum level to output.
     * @param maxLevel The maximum level to output.
     * @see #addWriter(Writer, Layout, LEVEL, LEVEL)
     */
    @Deprecated
    public static void addWriter(final Writer writer,
            final LEVEL minLevel, final LEVEL maxLevel) {
        KNIMELogger.addWriter(writer, minLevel, maxLevel);
    }


    /**
     * Adds a new {@link java.io.Writer} with the given level to this logger.
     *
     * @param writer The writer to add.
     * @param minLevel The minimum level to output.
     * @param maxLevel The maximum level to output.
     * @since 2.12
     */
    public static void addKNIMEConsoleWriter(final Writer writer, final LEVEL minLevel, final LEVEL maxLevel) {
        KNIMELogger.addKNIMEConsoleWriter(writer, minLevel, maxLevel);
    }

    /**
     * Adds a new {@link java.io.Writer} with the given level to this logger.
     *
     * @param writer The writer to add.
     * @param layout the log file layout to use
     * @param minLevel The minimum level to output.
     * @param maxLevel The maximum level to output.
     * @since 2.12
     */
    public static void addWriter(final Writer writer, final Layout layout,
            final LEVEL minLevel, final LEVEL maxLevel) {
        KNIMELogger.addWriter(writer, layout, minLevel, maxLevel);
    }


    /**
     * Removes the previously added {@link java.io.Writer} from the logger.
     *
     * @param writer The Writer to remove.
     */
    public static void removeWriter(final Writer writer) {
        KNIMELogger.removeWriter(writer);
    }

    /**
     * @param level minimum log level
     * @see #setLevel(NodeLogger.LEVEL)
     */
    @Deprecated
    public static void setLevelIntern(final LEVEL level) {
        KNIMELogger.setLevel(level);
    }

    /**
     * Sets an new minimum logging level for all internal appenders (log file and <code>System.out</code>).
     * The maximum logging level will be set to <code>LEVEL.FATAL</code> for all appenders.
     *
     * @param level new minimum logging level
     * @deprecated use {@link #setAppenderLevelRange(String, LEVEL, LEVEL)} instead for more fine-grained control
     */
    @Deprecated
    public static void setLevel(final LEVEL level) {
        KNIMELogger.setLevel(level);
    }


    /**
     * Returns the minimum logging retrieved from the underlying Log4J logger.
     *
     * @return minimum logging level
     */
    public LEVEL getLevel() {
        return m_logger.getLevel();
    }

    /**
     * Checks if debug logging level is enabled.
     *
     * @return <code>true</code> if debug logging level is enabled, otherwise
     *         <code>false</code>
     */
    public boolean isDebugEnabled() {
        return m_logger.isEnabledFor(Level.DEBUG);
    }

    /**
     * Checks if info logging level is enabled.
     *
     * @return <code>true</code> if info logging level is enabled, otherwise
     *         <code>false</code>
     */
    public boolean isInfoEnabled() {
        return m_logger.isEnabledFor(Level.INFO);
    }

    /**
     * Returns <code>true</code> if the underlying Log4J logger is enabled for
     * the given <code>level</code>.
     *
     * @param level to test logging enabled
     * @return <code>true</code> if logging is enabled, otherwise
     *         <code>false</code>
     */
    public boolean isEnabledFor(final LEVEL level) {
        return m_logger.isEnabledFor(level);
    }

    /**
     * Sets a level range filter on the given appender.
     *
     * @param appenderName the name of the appender
     * @param min the minimum logging level
     * @param max the maximum logging level
     * @throws NoSuchElementException if the given appender does not exist
     * @since 2.8
     * @deprecated use {@link NodeLoggerConfig#modifyAppenderLevelRange(String, BiFunction)} for more fine-graned
     *     control about existing minimum and maximum values
     */
    @Deprecated(forRemoval = true)
    public static void setAppenderLevelRange(final String appenderName, final LEVEL min, final LEVEL max)
            throws NoSuchElementException {
        KNIMELogger.modifyAppenderLevelRange(appenderName, (oldMin, oldMax) -> Pair.create(min, max));
    }

    /**
     * Allows to enable/disable logging in the workflow directory. If enabled log messages that belong to workflow
     * are logged into a log file within the workflow directory itself in addition to the global KNIME log file.
     *
     * @param enable <code>true</code> if workflow relative logging should be enabled
     * @since 2.12
     */
    public static void logInWorkflowDir(final boolean enable) {
        KNIMELogger.setLogInWorkflowDir(enable);
        LogLog.debug("Workflow directory logging set to: " + enable);
    }

    /**
     * Allows to enable/disable logging of global messages e.g. message that are not related to a workflow into the
     * workflow directory log file.
     *
     * @param enable <code>true</code> if workflow relative logging should be enabled
     * @since 2.12
     */
    public static void logGlobalMsgsInWfDir(final boolean enable) {
        KNIMELogger.setLogGlobalInWorkflowDir(enable);
        LogLog.debug("Workflow directory global message logging set to: " + enable);
    }

    /**
     * Allows to enable/disable node id logging. If enabled the node id information is added to the log events.
     * This method should only be called to globally disable the node id logging since the flag is enabled
     * automatically if one of the log file appender has a log layout that contains the node id pattern.
     *
     * @param enable <code>false</code> if workflow relative logging should be globally disabled
     * @since 2.12
     */
    public static void logNodeId(final boolean enable) {
        KNIMELogger.setLogNodeId(enable);
        LogLog.debug("Node ID logging set to: " + enable);
    }

    /**
     * Information from the node context – if available – at the time the message was logged.
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     *
     * @param nodeID non-{@code null} node id
     * @param nodeName non-{@code null} node name
     *
     * @since 5.3
     */
    public record NodeContextInformation(NodeID nodeID, String nodeName) {

        /**
         * Creates a new log message node context information object.
         *
         * @param nodeID non-{@code null} node id
         * @param nodeName non-{@code null} node name
         */
        public NodeContextInformation {
            Objects.requireNonNull(nodeID);
            Objects.requireNonNull(nodeName);
        }
    }

    /**
     * Tries to retrieve node context information from the given log message object.
     *
     * @apiNote This method is public for testing purposes only.
     *
     * @param msg log message object to retrieve info from
     * @return node context information if available, otherwise {@link Optional#empty()}
     *
     * @since 5.3
     */
    // we have this method in order to avoid exporting the whole logging package
    public static Optional<NodeContextInformation> getNodeContext(final Object msg) {
        return KNIMELogger.getNodeContext(msg);
    }
}
