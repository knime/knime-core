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
 *   9 Jan 2024 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.node.logging;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.varia.LevelMatchFilter;
import org.apache.log4j.varia.LevelRangeFilter;
import org.apache.log4j.varia.NullAppender;
import org.apache.log4j.xml.DOMConfigurator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;
import org.knime.core.node.logging.LogBuffer.BufferedLogMessage;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.EclipseUtil;
import org.knime.core.util.FileUtil;
import org.knime.core.util.Pair;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;

/**
 * The KNIME logging lifecycle starts in "uninitialized" state and can transition once to the "initialized" state.
 *
 * <dl>
 *  <dt>Uninitialized</dt>
 *  <dd>All logged messages get buffered in a circular buffer; a transition to "initialized" is possible</dd>
 *  <dt>Initialized</dt>
 *  <dd>Logged messages are immediately forwarded to the underlying logging framework; no transition to another state
 *      is possible; it is possible to forward the buffered log messages once (i.e. drain the buffer contents once)</dd>
 * </dl>
 *
 * <p>
 * Such a two-phase logging lifecycle is useful in the following (currently in place) scenario:
 * <br>
 * The logging in KNIME can be configured with a file residing in the user's workspace (aka "instance location").
 * As a consequence, the underlying logging framework cannot be configured until the user chooses the workspace location
 * (e.g. interactively through a prompt).
 * Hence, we buffer all log messages (up to a configurable number to conserve memory somewhat) until the location is
 * chosen and the underlying logging framework was configured.
 * Then we drain the buffer and forward the messages to the underlying logging framework.
 *
 * @noreference This class is not intended to be referenced by clients.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @since 5.3
 */
public final class KNIMELogger {

    // shared buffer for all instances used before logging is initialized
    private static final LogBuffer BUFFER = new LogBuffer(1024);

    private static volatile boolean isInitialized;

    static boolean isInstanceLocationSet() {
        final var loc = Platform.getInstanceLocation();
        if (loc != null) {
            return loc.isSet();
        }
        return false;
    }

    /**
     * Initializes the logging framework in a safe way, i.e. only after the instance location has been set.
     */
    public static void safeInitializeLogging() {
        final var logBufferedMessages = true;
        if (isInstanceLocationSet()) {
            initializeLogging(logBufferedMessages);
        } else {
            try {
                FrameworkUtil.getBundle(KNIMELogger.class).getBundleContext().addServiceListener(event -> {
                    if (event.getType() == ServiceEvent.MODIFIED) {
                        initializeLogging(logBufferedMessages);
                    }
                }, Location.INSTANCE_FILTER);
            } catch (final InvalidSyntaxException ex) {
                NodeLogger.getLogger(KNIMELogger.class.getName()).coding(
                    "Unable to initialize logging via service listener due to invalid filter", ex);
            }
        }
    }

    /**
     * The prefix when logging coding messages (they end up on 'error' but are prefixed by this value).
     */
    private static final String CODING_PROBLEM_PREFIX = "CODING PROBLEM\t";

    /** Map of additionally added writers: Writer -> Appender. */
    private static final Map<Writer, WriterAppender> WRITERS = new HashMap<>();

    /**
     * Keeps set of <code>DelegatingLogger</code> elements by class name as key.
     * */
    private static final ConcurrentHashMap<String, KNIMELogger> LOGGERS = new ConcurrentHashMap<>();

    /** As per log4j-3.xml we only log 'knime' log out -- the loggers with these prefixes are the parents of all
     * 'knime' logger. List is amended by 'NodeLogger' from other packages (e.g. partner extensions), see AP-12238 */
    private static final List<String> KNOWN_LOGGER_PREFIXES = new ArrayList<>(Arrays.asList("com.knime", "org.knime"));

    /**
     * Creates a logger for the given name.
     * @param s name to log under
     * @return logger for the given name
     */
    public static KNIMELogger getLogger(final String s) {
        return LOGGERS.computeIfAbsent(s, k -> {
            final var logger = new KNIMELogger(s);
            updateKnownPrefixesAndLoggerLevel(s);
            return logger;
        });
    }

    /**
     * Creates a logger for the given class.
     * @param clazz class under whose name to log
     * @return logger for given class
     */
    public static KNIMELogger getLogger(final Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    private static void updateKnownPrefixesAndLoggerLevel(final String s) {
        synchronized(KNOWN_LOGGER_PREFIXES) {
            if (KNOWN_LOGGER_PREFIXES.stream().noneMatch(s::startsWith)) {
                // s = foo.bar.blah.ClassName
                // pkgName = foo.bar.blah
                final var pkgName = StringUtils.substringBeforeLast(s, ".");
                if (StringUtils.isNotBlank(pkgName)) {
                    // remove foo.bar.blah.baz (subpackage)
                    KNOWN_LOGGER_PREFIXES.removeIf(prefix -> prefix.startsWith(pkgName));
                    KNOWN_LOGGER_PREFIXES.add(pkgName);
                    updateLog4JKNIMELoggerLevel();
                }
            }
        }
    }

    static boolean isInitialized() {
        return isInitialized;
    }

    static synchronized void initializeLogging(final boolean logBufferedLogMessages) {
        if (isInitialized()) {
            return;
        }
        final var isInstanceLocationSet = isInstanceLocationSet();

        initLogging();
        isInitialized = true;
        // now that the logging framework is initialized, we need to create the actual loggers for all
        // of the currently existing KNIMELoggers
        // Note on concurrency: while we go through this map, any newly created KNIMELoggers will already have the
        //                      wrapped logger initialized, since the flag above is already set
        LOGGERS.forEachValue(Integer.MAX_VALUE, KNIMELogger::initializeInternalLogger);

        // now that the flag is set, we can also use logging internally since no message will be buffered
        if (!isInstanceLocationSet && isInstanceLocationSet()) {
            NodeLogger.getLogger(NodeLogger.class).info(() ->
                    """
                        Initialization of logging did set the instance location, possibly to an unexpected default
                        location: "%s"
                    """.formatted(Platform.getInstanceLocation().getURL()));
        }
        DelegatingLogger.startMessage();
        if (logBufferedLogMessages) {
            logBufferedMessages();
        }
    }

    private void initializeInternalLogger() {
        if (m_logger == null) {
            m_logger = new DelegatingLogger(m_name);
        }
    }

    private static void initLogging() {
        // we only guard the private method against repeated init requests
        CheckUtils.checkState(!isInitialized(), "Logging was already initialized");
        if (!Boolean.getBoolean(KNIMEConstants.PROPERTY_DISABLE_LOG4J_CONFIG)) {
            initLog4J();
            // init root logger
            final var root = Logger.getRootLogger();
            final var appender = root.getAppender(NodeLogger.LOGFILE_APPENDER);
            if (appender != null) {
                DelegatingLogger.setLogFileAppender(appender);
                final var layout = appender.getLayout();
                DelegatingLogger.setWorkflowDirLogfileLayout(layout);
            } else {
                root.warn("Could not find '" + NodeLogger.LOGFILE_APPENDER + "' appender");
                DelegatingLogger.setLogFileAppender(new NullAppender());
            }
        } else {
            DelegatingLogger.setLogFileAppender(new NullAppender());
        }
    }

    private static void initLog4J() {
        final String file = System.getProperty("log4j.configuration");
        if (file == null) {
            try {
                String latestLog4jConfig = getLatestLog4jConfig();
                assert (NodeLogger.class.getClassLoader().getResourceAsStream(
                        latestLog4jConfig) != null) : "latest log4j-configuration "
                            + " could not be found";
                final var knimeDir = new File(KNIMEConstants.getKNIMEHomeDir());
                //we use log4j3 as log file name starting with KNIME 2.12 which introduced the new
                //org.knime.core.node.NodeLoggerPatternLayout class. This way older versions of KNIME can also open
                //workflows created with >2.12 since they simply ignore the new log file
                var log4j = new File(knimeDir, "log4j3.xml");

                final var legacyFile = new File(knimeDir, "log4j-1.1.0.xml");
                if (legacyFile.exists() && !legacyFile.renameTo(log4j)) {
                    System.err.println("There are two log4j configuration files"
                            + " in your KNIME home directory ('"
                            + knimeDir.getAbsolutePath()
                            + " ') - or this directory is write-protected.");
                    System.err.println("The 'log4j.xml' is the one actually used."
                            + " Merge changes you may have made"
                            + " to 'log4j-1.1.0.xml' and remove"
                            + " 'log4j-1.1.0.xml' to get rid of this message.");
                }
                if (!log4j.exists()) {
                    //this might be a workspace created prior KNIME 2.12 which introduced the new
                    //org.knime.core.node.NodeLoggerPatternLayout class check that it is the default version which
                    //we can safely overwrite
                    final var log4jOld = new File(knimeDir, "log4j.xml");
                    if (!log4jOld.exists()) {
                        //this is a new workspace so simply use the new log file
                        copyCurrentLog4j(log4j, latestLog4jConfig);
                    } else if (checkPreviousLog4j(log4jOld, latestLog4jConfig)) {
                        //this is an old workspace <KNIME 2.12 with a default log file so delete the old log file
                        copyCurrentLog4j(log4j, latestLog4jConfig);
                        Files.delete(log4jOld.toPath());
                    } else {
                        //this is an old workspace with an adapted log4j file which we should continue to use
                        final var templateFile = new File(knimeDir, "log4j3.xml_template");
                        if (!templateFile.exists()) {
                            //create a template file which contains the new logging settings
                            copyCurrentLog4j(templateFile, latestLog4jConfig);
                        }
                        log4j = log4jOld;
                    }
                } else if (checkPreviousLog4j(log4j, latestLog4jConfig)) {
                    copyCurrentLog4j(log4j, latestLog4jConfig);
                }
                DOMConfigurator.configure(log4j.getAbsolutePath());
            } catch (final IOException e) {
                // since configuration of the logging framework failed, we use the logging framework's internal logger
                LogLog.error(e.getMessage(), e);
            }
        } else {
            if (file.endsWith(".xml")) {
                DOMConfigurator.configure(file);
            } else {
                PropertyConfigurator.configure(file);
            }
        }
        // use unguarded private method here since we did not yet set the "initialized" flag, so a check would fail
        updateLog4JKNIMELoggerLevelInternal();
    }

    /** Adjusts log level of 'knime' loggers so that it matches the minimum level of all registered appenders.
     * Called after initialization and after the log level is changed for individual appenders.
     *
     * <p>
     * Logger must be in <b>initialized</b> state.
     */
    static void updateLog4JKNIMELoggerLevel() {
        checkInitializedState();
        updateLog4JKNIMELoggerLevelInternal();
    }

    private static void updateLog4JKNIMELoggerLevelInternal() {
        final var rootLogger = LogManager.getRootLogger();
        var minimumLevel = rootLogger.getLevel(); // by default this is 'ERROR' but may be changed in log4j.xml
        for (@SuppressWarnings("unchecked")
        Enumeration<Appender> appenderEnum = rootLogger.getAllAppenders(); appenderEnum.hasMoreElements();) {
            final var next = appenderEnum.nextElement();
            for (var filter = next.getFilter(); filter != null; filter = filter.getNext()) {
                Level l = null;
                if (filter instanceof LevelMatchFilter lmf) {
                    l = OptionConverter.toLevel(lmf.getLevelToMatch(), Level.FATAL);
                } else if (filter instanceof LevelRangeFilter lrf) {
                    l = lrf.getLevelMin();
                }
                if (l != null && minimumLevel.isGreaterOrEqual(l)) {
                    minimumLevel = l;
                }
            }
        }
        final var minimumLevelFinal = minimumLevel;
        synchronized (LOGGERS) {
            KNOWN_LOGGER_PREFIXES.stream().map(LogManager::getLogger).forEach(l -> l.setLevel(minimumLevelFinal));
        }
    }

    private static void copyCurrentLog4j(final File dest, final String latestLog4jConfig) throws IOException {
        try (final var in = NodeLogger.class.getClassLoader().getResourceAsStream(latestLog4jConfig);
                final var out = new FileOutputStream(dest)){
            if (in == null) {
                throw new IOException("Latest log4j-config '"
                        + latestLog4jConfig + "' not found");
            }
            FileUtil.copy(in, out);
        }
    }

    private static String getLatestLog4jConfig() throws IOException {
        final var cl = NodeLogger.class.getClassLoader();
        for (var i = 1; i < Integer.MAX_VALUE; i++) {
            String file = "log4j/log4j-" + i + ".xml";
            try (final InputStream in = cl.getResourceAsStream(file)) {
                if (in == null) {
                    return "log4j/log4j-" + (i - 1) + ".xml";
                }
            }
        }
        // should not happen since log4j-0.xml must exist
        return null;
    }

    /**
     * Checks if any of the previous shipped log4j-XMLs matches the current one
     * the user has in its local KNIME directory.
     *
     * @param current the user's current file
     * @param latestLog4jConfig the latest log4j template file
     * @return <code>true</code> if it matches, <code>false</code> otherwise
     * @throws IOException if an I/O error occurs
     */
    private static boolean checkPreviousLog4j(final File current, final String latestLog4jConfig)
            throws IOException {
        try (final var reader = new FileInputStream(current)) {
            final var currentContents = new byte[(int)current.length()];
            reader.read(currentContents);
            final var cl = NodeLogger.class.getClassLoader();
            for (var k = 0; k < Integer.MAX_VALUE; k++) {
                String file = "log4j/log4j-" + k + ".xml";
                if (file.equals(latestLog4jConfig)) {
                    break;
                }
                // compare the two files
                try (
                        final var currentReader =
                                new BufferedReader(new InputStreamReader(new ByteArrayInputStream(currentContents)));
                        final var existingReader =
                                new BufferedReader(new InputStreamReader(cl.getResourceAsStream(file)))) {
                    var match = true;
                    String line1 = null;
                    String line2 = null;
                    while (((line1 = currentReader.readLine()) != null)
                            && ((line2 = existingReader.readLine()) != null)) {
                        if (!line1.equals(line2)) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private static void checkInitializedState() {
        CheckUtils.checkState(isInitialized(), "Logging not yet initialized");
    }

    /**
     * @return <code>true</code> if assertions are on or run from the SDK.
     */
    private static boolean isToLogCodingMessages() {
        return KNIMEConstants.ASSERTIONS_ENABLED || EclipseUtil.isRunFromSDK();
    }

    /**
     * Allows to enable/disable logging in the workflow directory. If enabled log messages that belong to workflow
     * are logged into a log file within the workflow directory itself in addition to the global KNIME log file.
     *
     * @param enable <code>true</code> if workflow relative logging should be enabled
     */
    public static void setLogInWorkflowDir(final boolean enable) {
        DelegatingLogger.logInWFDir = enable;
    }

    /**
     * Allows to enable/disable logging of global messages e.g. message that are not related to a workflow into the
     * workflow directory log file.
     *
     * @param enable <code>true</code> if logging of global messages in workflow directory log file should be enabled
     */
    public static void setLogGlobalInWorkflowDir(final boolean enable) {
        DelegatingLogger.logGlobalInWFDir = enable;
    }

    /**
     * Allows to enable/disable node id logging. If enabled the node id information is added to the log events.
     * This method should only be called to globally disable the node id logging since the flag is enabled
     * automatically if one of the log file appender has a log layout that contains the node id pattern.
     *
     * @param enable <code>false</code> to disable that the node id is added to log events
     * @since 2.12
     */
    public static void setLogNodeId(final boolean enable) {
        DelegatingLogger.logNodeId  = enable;
    }

    /**
     * Adds a new {@link java.io.Writer} with the given level to this logger.
     *
     * <p>
     * Logger must be in <b>initialized</b> state.
     *
     * @param writer The writer to add.
     * @param minLevel The minimum level to output.
     * @param maxLevel The maximum level to output.
     * @see #addWriter(Writer, Layout, LEVEL, LEVEL)
     * @deprecated use {@link #addWriter(Writer, Layout, LEVEL, LEVEL)}
     */
    @Deprecated
    // this method is only here to bridge the deprecated method in NodeLogger#addWriter(writer, minLevel, maxLevel)
    // with the field in DelegatingLogger
    public static void addWriter(final Writer writer, final LEVEL minLevel, final LEVEL maxLevel) {
        addWriter(writer, DelegatingLogger.workflowDirLogfileLayout, minLevel, maxLevel);
    }

    /**
     * Adds a new {@link java.io.Writer} with the given level to this logger.
     *
     * <p>
     * Logger must be in <b>initialized</b> state.
     *
     * @param writer The writer to add.
     * @param layout the log file layout to use
     * @param minLevel The minimum level to output.
     * @param maxLevel The maximum level to output.
     */
    public static void addWriter(final Writer writer, final Layout layout, final LEVEL minLevel, final LEVEL maxLevel) {
        checkInitializedState();
        final var appender = new WriterAppender(layout, writer);
        appender.setImmediateFlush(true);
        final var filter = new LevelRangeFilter();
        filter.setLevelMin(translateKnimeToLog4JLevel(minLevel));
        filter.setLevelMax(translateKnimeToLog4JLevel(maxLevel));
        appender.addFilter(filter);

        // remove the writer first if existent
        synchronized (WRITERS) {
            if (WRITERS.containsKey(writer)) {
                Appender a = WRITERS.get(writer);
                Logger.getRootLogger().removeAppender(a);
                WRITERS.remove(writer);
            }
            // register new appender
            WRITERS.put(writer, appender);
        }
        Logger.getRootLogger().addAppender(appender);
        DelegatingLogger.checkLayoutFlags(layout);
        updateLog4JKNIMELoggerLevel();
    }

    /**
     * Adds a new {@link java.io.Writer} with the given level to this logger.
     *
     * <p>
     * Logger must be in <b>initialized</b> state.
     *
     * @param writer The writer to add.
     * @param minLevel The minimum level to output.
     * @param maxLevel The maximum level to output.
     */
    public static void addKNIMEConsoleWriter(final Writer writer, final LEVEL minLevel, final LEVEL maxLevel) {
        checkInitializedState();
        final var appender = Logger.getRootLogger().getAppender(NodeLogger.KNIME_CONSOLE_APPENDER);
        final Layout layout;
        if (appender != null) {
            layout = appender.getLayout();
            DelegatingLogger.checkLayoutFlags(layout);
        } else {
            layout = DelegatingLogger.workflowDirLogfileLayout;
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
     * Removes the previously added {@link java.io.Writer} from the logger.
     *
     * <p>
     * Logger must be in <b>initialized</b> state.
     *
     * @param writer The Writer to remove.
     */
    public static void removeWriter(final Writer writer) {
        checkInitializedState();
        synchronized (WRITERS) {
            final var appender = WRITERS.get(writer);
            if (appender != null && appender != DelegatingLogger.getLogFileAppender()) {
                Logger.getRootLogger().removeAppender(appender);
                WRITERS.remove(writer);
            } else {
                NodeLogger.getLogger(NodeLogger.class).warn("Could not delete writer: " + writer);
            }
        }
        updateLog4JKNIMELoggerLevel();
    }

    /**
     * Returns the minimum logging level.
     *
     * <p>
     * Logger must be in <b>initialized</b> state.
     *
     * @return minimum logging level
     */
    public LEVEL getLevel() {
        return translateLog4JToKnimeLevel(getInternalLevel());
    }

    private Level getInternalLevel() {
        checkInitializedState();
        return m_logger.getLevel();
    }

    /**
     * Sets an new minimum logging level for all internal appenders, that are, log file, and <code>System.out</code> and
     * <code>System.err</code> appender. The maximum logging level stays <code>LEVEL.ALL</code> for all appenders.
     *
     * <p>
     * Logger must be in <b>initialized</b> state.
     *
     * @param level new minimum logging level
     * @deprecated use {@link #setAppenderLevelRange(String, LEVEL, LEVEL)} instead for more fine-grained control
     */
    @Deprecated
    public static void setLevel(final LEVEL level) {
        checkInitializedState();
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
     * Checks whether the logger is enabled for the given level.
     *
     * <p>
     * Logger must be in <b>initialized</b> state.
     *
     * @param level level to check
     * @return {@code true} if the logger is enabled for the given level
     */
    public boolean isEnabledFor(final Level level) {
        checkInitializedState();
        return m_logger.isEnabledFor(level);
    }

    /**
     * Checks whether the logger is enabled for the given level.
     *
     * <p>
     * Logger must be in <b>initialized</b> state.
     *
     * @param level level to check
     * @return {@code true} if the logger is enabled for the given level
     */
    public boolean isEnabledFor(final LEVEL level) {
        checkInitializedState();
        return m_logger.isEnabledFor(translateKnimeToLog4JLevel(level));
    }

    /**
     * Returns the minimum and maximum log level for a given appender name.
     * If the log level range has not been specified or logging not yet been initialized, returns null.
     *
     * @param appenderName Name of the appender.
     * @return Pair of (minLogLevel, maxLogLevel).
     */
    public static Pair<LEVEL, LEVEL> getAppenderLevelRange(final String appenderName) {
        // checkInitializedState(); makes "HeadlessPreferencesInitializer" fail
        if (!isInitialized()) {
            return null;
        }
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
     * <p>
     * Logger must be in <b>initialized</b> state.
     *
     * @param appenderName the name of the appender
     * @param min the minimum logging level
     * @param max the maximum logging level
     * @throws NoSuchElementException if the given appender does not exist
     */
    public static void setAppenderLevelRange(final String appenderName, final LEVEL min, final LEVEL max)
        throws NoSuchElementException {
        checkInitializedState();
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
            var levelFilter = new LevelRangeFilter();
            levelFilter.setLevelMin(translateKnimeToLog4JLevel(min));
            levelFilter.setLevelMax(translateKnimeToLog4JLevel(max));
            appender.addFilter(levelFilter);
        } else {
            // modify existing level range filter
            ((LevelRangeFilter)filter).setLevelMin(translateKnimeToLog4JLevel(min));
            ((LevelRangeFilter)filter).setLevelMax(translateKnimeToLog4JLevel(max));
        }
        updateLog4JKNIMELoggerLevel();
    }

    /**
     * Translates this logging <code>LEVEL</code> into Log4J logging levels.
     *
     * @param level the <code>LEVEL</code> to translate
     * @return the Log4J logging level
     */
    private static Level translateKnimeToLog4JLevel(final LEVEL level) {
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
     *
     * @param level the Level to translate
     * @return this logging LEVEL
     */
    private static LEVEL translateLog4JToKnimeLevel(final Level level) {
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


    // cached non-buffering logger once available (after logging was initialized)
    private DelegatingLogger m_logger;

    private final String m_name;

    private KNIMELogger(final String name) {
        m_name = name;
        if(isInitialized()) {
            m_logger = new DelegatingLogger(m_name);
        }
    }

    /**
     * Logs any buffered messages. Will throw an exception if still in uninitialized state.
     */
    static void logBufferedMessages() {
        checkInitializedState();
        synchronized(BUFFER) {
            BUFFER.drainTo(bufferedMessage -> getLogger(bufferedMessage.name()).log(bufferedMessage));
        }
    }

    private void log(final BufferedLogMessage bufferedMessage) {
        m_logger.log(bufferedMessage);
    }

    /**
     * Log the message from the supplier at the specified level.
     * The message supplier may or may not get invoked immediately (or at all).
     *
     * @param level level to log under
     * @param messageSupplier supplier for the message
     * @param cause optional cause for the log message
     * @param considerWFDirAppenders whether or not to consider workflow directory appenders
     */
    public void log(final Level level, final Supplier<Object> messageSupplier,
            final Throwable cause, final boolean considerWFDirAppenders) {
        // we double-check to avoid expensive locking
        if (!isInitialized()) {
            synchronized(BUFFER) {
                // we need to check again now that we have the lock to see if someone else has initialized it in the
                // meantime
                if (!isInitialized()) {
                    BUFFER.log(level, m_name, messageSupplier.get(), cause);
                    return;
                }
            }
        }
        m_logger.log(level, messageSupplier, cause, considerWFDirAppenders);
    }

    /**
     * Log the message at the specified level.
     *
     * @param level level to log under
     * @param message message to log
     * @param cause optional cause for the log message
     * @param considerWFDirAppenders whether or not to consider workflow directory appenders
     */
    public void log(final Level level, final Object message, final Throwable cause,
        final boolean considerWFDirAppenders) {
        // we double-check to avoid expensive locking
        if (!isInitialized()) {
            synchronized(BUFFER) {
                if (!isInitialized()) {
                    BUFFER.log(level, m_name, message, cause);
                    return;
                }
            }
        }
        m_logger.log(level, message, cause, considerWFDirAppenders);
    }


    /**
     * Log the message from the supplier at the specified level as a "coding message"
     * with a special prefix and the error level.
     * The message supplier may or may not get invoked immediately (or at all).
     *
     * @param messageSupplier supplier for the message
     * @param cause optional cause for the log message
     * @param considerWFDirAppenders whether or not to consider workflow directory appenders
     */
    public void logCoding(final Supplier<Object> messageSupplier, final Throwable cause,
        final boolean considerWFDirAppenders) {
        if (isToLogCodingMessages()) {
            log(Level.ERROR, () -> CODING_PROBLEM_PREFIX + messageSupplier.get(), cause, considerWFDirAppenders);
        }
    }

    /**
     * Log the message at the specified level as a "coding message" with a special
     * prefix and the error level.
     *
     * @param message message to log
     * @param cause optional cause for the log message
     * @param considerWFDirAppenders whether or not to consider workflow directory appenders
     */
    public void logCoding(final Object message, final Throwable cause,
        final boolean considerWFDirAppenders) {
        if (isToLogCodingMessages()) {
            log(Level.ERROR, CODING_PROBLEM_PREFIX + message, cause, considerWFDirAppenders);
        }
    }
}
