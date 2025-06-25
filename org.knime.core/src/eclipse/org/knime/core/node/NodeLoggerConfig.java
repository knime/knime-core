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
 *   May 25, 2023 (Leon Wenzler, KNIME AG, Konstanz, Germany): created
 */
package org.knime.core.node;

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.varia.LevelRangeFilter;
import org.knime.core.node.NodeLogger.LEVEL;
import org.knime.core.node.logging.WorkflowLogCloseable;
import org.knime.core.util.Pair;

/**
 * All configuration-relevant NodeLogger functionality. Includes adding and removing writers,
 * getting and setting log level ranges, and translating between KNIME and Log4J Level enums.
 *
 * @author Leon Wenzler, KNIME AG, Konstanz, Germany
 * @since 5.1
 */
public final class NodeLoggerConfig {

    // -- WRITER MANAGMENT --

    /** Map of additionally added writers: Writer -> Appender. */
    private static final Map<Writer, WriterAppender> WRITER = new HashMap<>();

    /**
     * Adds a new {@link java.io.Writer} with the given level to this logger.
     *
     * @param writer The writer to add.
     * @param minLevel The minimum level to output.
     * @param maxLevel The maximum level to output.
     */
    public static void addKNIMEConsoleWriter(final Writer writer, final LEVEL minLevel, final LEVEL maxLevel) {
        final var appender = Logger.getRootLogger().getAppender(NodeLogger.KNIME_CONSOLE_APPENDER);
        final Layout layout;
        if (appender != null) {
            layout = appender.getLayout();
            WorkflowLogCloseable.checkLayoutFlags(layout);
        } else {
            layout = WorkflowLogCloseable.workflowDirLogfileLayout;
        }
        // no stack traces in KNIME's console view:
        // a custom layout that pretends Throwable information is baked into the log message
        final Layout suppressThrowableLayout = new Layout() {

            @Override
            public void activateOptions() {
                layout.activateOptions();
            }

            @Override
            public String format(final LoggingEvent event) {
                return layout.format(event);
            }

            @Override
            public boolean ignoresThrowable() {
                // PatternLayout returns true (which makes the appender to log the Throwable)
                return false;
            }

        };
        addWriter(writer, suppressThrowableLayout, minLevel, maxLevel);
    }

    /**
     * Adds a new {@link java.io.Writer} with the given level to this logger.
     *
     * @param writer The writer to add.
     * @param layout the log file layout to use
     * @param minLevel The minimum level to output.
     * @param maxLevel The maximum level to output.
     */
    public static void addWriter(final Writer writer, final Layout layout, final LEVEL minLevel, final LEVEL maxLevel) {
        final var appender = new WriterAppender(layout, writer);
        appender.setImmediateFlush(true);
        final var filter = new LevelRangeFilter();
        filter.setLevelMin(translateKnimeToLog4JLevel(minLevel));
        filter.setLevelMax(translateKnimeToLog4JLevel(maxLevel));
        appender.addFilter(filter);

        // remove the writer first if existent
        synchronized (WRITER) {
            if (WRITER.containsKey(writer)) {
                Appender a = WRITER.get(writer);
                Logger.getRootLogger().removeAppender(a);
                WRITER.remove(writer);
            }
            // register new appender
            WRITER.put(writer, appender);
        }
        Logger.getRootLogger().addAppender(appender);
        WorkflowLogCloseable.checkLayoutFlags(layout);
        NodeLogger.updateLog4JKNIMELoggerLevel();
    }

    /**
     * Removes the previously added {@link java.io.Writer} from the logger.
     *
     * @param writer The Writer to remove.
     */
    public static void removeWriter(final Writer writer) {
        synchronized (WRITER) {
            final var appender = WRITER.get(writer);
            if (appender != null) {
                if (appender != WorkflowLogCloseable.logFileAppender) {
                    Logger.getRootLogger().removeAppender(appender);
                    WRITER.remove(writer);
                }
            } else {
                NodeLogger.getLogger(NodeLogger.class).warn("Could not delete writer: " + writer);
            }
        }
        NodeLogger.updateLog4JKNIMELoggerLevel();
    }

    // -- GLOBAL LOG LEVEL GETTER AND SETTER --

    /**
     * Sets an new minimum logging level for all internal appenders, that are, log file, and <code>System.out</code> and
     * <code>System.err</code> appender. The maximum logging level stays <code>LEVEL.ALL</code> for all appenders.
     *
     * @param level new minimum logging level
     * @deprecated user {@link #setAppenderLevelRange(String, LEVEL, LEVEL)} instead for more fine-grained control
     */
    @Deprecated
    public static void setLevel(final LEVEL level) {
        NodeLogger.getLogger(NodeLogger.class).info("Changing logging level to " + level.toString());
        try {
            setAppenderLevelRange(NodeLogger.STDOUT_APPENDER, level, LEVEL.FATAL);
        } catch (NoSuchElementException ex) { // NOSONAR
            // ignore it
        }
        try {
            setAppenderLevelRange(NodeLogger.LOGFILE_APPENDER, level, LEVEL.FATAL);
        } catch (NoSuchElementException ex) { // NOSONAR
            // ignore it
        }
    }

    /**
     * Returns the minimum and maximum log level for a given appender name.
     * If the log level range has not been specified, returns null.
     *
     * @param appenderName Name of the appender.
     * @return Pair of (minLogLevel, maxLogLevel).
     */
    public static Pair<LEVEL, LEVEL> getAppenderLevelRange(final String appenderName) {
        final var appender = Logger.getRootLogger().getAppender(appenderName);
        if (appender == null) {
            return null;
        }
        var filter = appender.getFilter();
        while (filter != null) {
            if (filter instanceof LevelRangeFilter rangeFilter) {
                return Pair.create(//
                    translateLog4JToKnimeLevel(rangeFilter.getLevelMin()), //
                    translateLog4JToKnimeLevel(rangeFilter.getLevelMax()));
            }
            filter = filter.getNext();
        }
        return null;
    }

    /**
     * Sets a level range filter on the given appender.
     *
     * @param appenderName the name of the appender
     * @param min the minimum logging level
     * @param max the maximum logging level
     * @throws NoSuchElementException if the given appender does not exist
     */
    public static void setAppenderLevelRange(final String appenderName, final LEVEL min, final LEVEL max)
        throws NoSuchElementException {
        final var root = Logger.getRootLogger();
        final var appender = root.getAppender(appenderName);
        if (appender == null) {
            throw new NoSuchElementException("Appender '" + appenderName + "' does not exist");
        }

        var filter = appender.getFilter();
        while ((filter != null) && !(filter instanceof LevelRangeFilter)) {
            filter = filter.getNext();
        }
        if (filter == null) {
            // add a new level range filter
            LevelRangeFilter levelFilter = new LevelRangeFilter();
            levelFilter.setLevelMin(translateKnimeToLog4JLevel(min));
            levelFilter.setLevelMax(translateKnimeToLog4JLevel(max));
            appender.addFilter(levelFilter);
        } else {
            // modify existing level range filter
            ((LevelRangeFilter)filter).setLevelMin(translateKnimeToLog4JLevel(min));
            ((LevelRangeFilter)filter).setLevelMax(translateKnimeToLog4JLevel(max));
        }
        NodeLogger.updateLog4JKNIMELoggerLevel();
    }

    // -- LOG LEVEL TRANSLATION --

    /**
     * Translates this logging <code>LEVEL</code> into Log4J logging levels.
     * Package-scope for access by {@link NodeLogger}.
     *
     * @param level the <code>LEVEL</code> to translate
     * @return the Log4J logging level
     */
    static Level translateKnimeToLog4JLevel(final LEVEL level) {
        return switch (level) {
            case DEBUG -> Level.DEBUG;
            case INFO -> Level.INFO;
            case WARN -> Level.WARN;
            case ERROR -> Level.ERROR;
            case FATAL -> Level.FATAL;
            default -> Level.ALL;
        };
    }

    /**
     * Translates Log4J logging level into this <code>LEVEL</code>.
     * Package-scope for access by {@link NodeLogger}.
     *
     * @param level the Level to translate
     * @return this logging LEVEL
     */
    static LEVEL translateLog4JToKnimeLevel(final Level level) {
        // A null level defaults to log level ALL.
        return switch (Objects.requireNonNullElse(level, Level.ALL).toInt()) {
            case Priority.DEBUG_INT -> LEVEL.DEBUG;
            case Priority.INFO_INT -> LEVEL.INFO;
            case Priority.WARN_INT -> LEVEL.WARN;
            case Priority.ERROR_INT -> LEVEL.ERROR;
            case Priority.FATAL_INT -> LEVEL.FATAL;
            default -> LEVEL.ALL;
        };
    }

    /**
     * Hides the constructor.
     */
    private NodeLoggerConfig() {
    }
}
