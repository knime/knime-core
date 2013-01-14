/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * --------------------------------------------------------------------- *
 *
 * History
 *   15.05.2006(sieb, ohl): reviewed
 */
package org.knime.core.node;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.varia.LevelRangeFilter;
import org.apache.log4j.varia.NullAppender;
import org.apache.log4j.xml.DOMConfigurator;
import org.knime.core.eclipseUtil.OSGIHelper;
import org.knime.core.util.FileUtil;
import org.knime.core.util.LogfileAppender;
import org.knime.core.util.User;

/**
 * The general logger used to write info, warnings, errors , debugging, assert
 * messages, exceptions, and coding problems into the internal Log4J logger. The
 * loggers are configured by the <code>log4j.properties</code> file in the
 * root of the core package. The configuration can be overridden by specifying a
 * file in <code>-Dlog4j.configuration</code> (this is the standard log4j
 * behaviour). Furthermore, it is possible to add and remove additional writers
 * to this logger. Note, calling {@link #setLevelIntern(LEVEL)} does only effect
 * the minimum logging level of the default loggers. All other writers' levels
 * have to be set before hand.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public final class NodeLogger {
    /** The logging levels. */
    public static enum LEVEL {
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
        ALL
    }

    /** The default log file name, <i>knime.log</i>. */
    public static final String LOG_FILE = "knime.log";

    /** Keeps set of <code>NodeLogger</code> elements by class name as key. */
    private static final HashMap<String, NodeLogger> LOGGERS =
            new HashMap<String, NodeLogger>();

    /** Map of additionally added writers: Writer -> Appender. */
    private static final HashMap<Writer, WriterAppender> WRITER =
            new HashMap<Writer, WriterAppender>();

    /**
     * Maximum number of chars (10000) printed on <code>System.out</code> and
     * <code>System.err</code>.
     */
    private static final int MAX_CHARS = 10000;

    /** <code>System.out</code> log appender. */
    private static final Appender SOUT_APPENDER;

    /** Default log file appender. */
    private static final Appender FILE_APPENDER;

    /**
     * Inits Log4J logger and appends <code>System.out</code>,
     * <code>System.err</code>, and <i>knime.log</i> to it.
     */
    static {
        if (!Boolean.getBoolean(KNIMEConstants.PROPERTY_DISABLE_LOG4J_CONFIG)) {
            try {
                initLog4J();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            // init root logger
            Logger root = Logger.getRootLogger();
            Appender a = root.getAppender("stderr");
            a = root.getAppender("stdout");
            if (a != null) {
                SOUT_APPENDER = a;
            } else {
                root.warn("Could not find 'stdout' appender");
                SOUT_APPENDER = new NullAppender();
            }

            a = root.getAppender("logfile");
            if (a != null) {
                FILE_APPENDER = a;
            } else {
                root.warn("Could not find 'logfile' appender");
                FILE_APPENDER = new NullAppender();
            }
        } else {
            SOUT_APPENDER = new NullAppender();
            FILE_APPENDER = new NullAppender();
        }

        startMessage();
    }


    private static void initLog4J() throws IOException {
        final String file = System.getProperty("log4j.configuration");
        if (file == null) {
            String latestLog4jConfig = getLatestLog4jConfig();
            assert (NodeLogger.class.getClassLoader().getResourceAsStream(
                    latestLog4jConfig) != null) : "latest log4j-configuration "
                        + " could not be found";
            File knimeDir = new File(KNIMEConstants.getKNIMEHomeDir());
            File log4j = new File(knimeDir, "log4j.xml");

            File legacyFile = new File(knimeDir, "log4j-1.1.0.xml");
            if (legacyFile.exists()) {
                if (!legacyFile.renameTo(log4j)) {
                    System.err.println("There are two log4j configuration files"
                            + " in your KNIME home directory ('"
                            + knimeDir.getAbsolutePath()
                            + " ') - or this directory is write-protected.");
                    System.err.println("The 'log4j.xml' is the one actually used."
                            + " Merge changes you may have made"
                            + " to 'log4j-1.1.0.xml' and remove"
                            + " 'log4j-1.1.0.xml' to get rid of this message.");
                }
            }
            if (!log4j.exists() || checkPreviousLog4j(log4j, latestLog4jConfig)) {
                copyCurrentLog4j(log4j, latestLog4jConfig);
            }
            DOMConfigurator.configure(log4j.toURI().toURL());
        } else {
            if (file.endsWith(".xml")) {
                DOMConfigurator.configure(file);
            } else {
                PropertyConfigurator.configure(file);
            }
        }
    }

    private static void copyCurrentLog4j(final File dest, final String latestLog4jConfig) throws IOException {
        InputStream in =
                NodeLogger.class.getClassLoader().getResourceAsStream(
                        latestLog4jConfig);
        if (in == null) {
            throw new IOException("Latest log4j-config '"
                    + latestLog4jConfig + "' not found");
        }
        FileOutputStream out = new FileOutputStream(dest);
        FileUtil.copy(in, out);
        in.close();
        out.close();
    }

    private static String getLatestLog4jConfig() throws IOException {
        ClassLoader cl = NodeLogger.class.getClassLoader();

        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            String file = "log4j/log4j-" + i + ".xml";
            InputStream in = cl.getResourceAsStream(file);
            if (in == null) {
                return "log4j/log4j-" + (i - 1) + ".xml";
            }
            in.close();
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
        FileInputStream reader = new FileInputStream(current);
        byte[] currentContents = new byte[(int)current.length()];
        reader.read(currentContents);
        reader.close();

        ClassLoader cl = NodeLogger.class.getClassLoader();

        for (int k = 0; k < Integer.MAX_VALUE; k++) {
            String file = "log4j/log4j-" + k + ".xml";
            if (latestLog4jConfig.equals(file)) {
                break;
            }

            // compare the two files
            InputStream in = new BufferedInputStream(cl.getResourceAsStream(file));
            int i = 0;
            boolean match = true;
            while (true) {
                byte b = (byte) in.read();
                if ((i >= currentContents.length) && (b == -1)) {
                    break;
                }

                if (i >= currentContents.length) {
                    match = false;
                    break;
                }

                if (b == -1) {
                    match = false;
                    break;
                }

                if (currentContents[i] != b) {
                    match = false;
                    break;
                }
                i++;
            }
            in.close();
            if (match) {
                return true;
            }
        }
        return false;
    }

    /** Write start logging message to info logger of this class. */
    private static void startMessage() {
        NodeLogger l = getLogger(NodeLogger.class);
        l.info("#############################################################");
        l.info("#                                                           #");
        l.info("# "
                + ("Welcome to KNIME v" + KNIMEConstants.VERSION + " (Build "
                        + KNIMEConstants.BUILD_DATE
                        + ")                        ").substring(0, 62) + " #");
        l.info("# the Konstanz Information Miner                            #");
        l.info("# Based on Eclipse, www.eclipse.org                         #");
        l.info("# Uses: Java, GEF, Log4J                                    #");
        l.info("#                                                           #");
        l.info("#############################################################");
        l.info("#                                                           #");
        copyrightMessage();
        l.info("#                                                           #");
        l.info("#############################################################");
        if (FILE_APPENDER instanceof LogfileAppender) {
            l.info("# For more details see the KNIME log file:"
                    + "                  #");
            l.info("# " + ((LogfileAppender)FILE_APPENDER).getFile());
            l.info("#-----------------------------------------------"
                    + "------------#");
        }

        l.info("# logging date=" + new Date());
        l.info("# java.version=" + System.getProperty("java.version"));
        l.info("# java.vm.version=" + System.getProperty("java.vm.version"));
        l.info("# java.vendor=" + System.getProperty("java.vendor"));
        l.info("# os.name=" + System.getProperty("os.name"));
        l.info("# os.arch=" + System.getProperty("os.arch"));
        l.info("# number of CPUs="
                        + Runtime.getRuntime().availableProcessors());
        l.info("# assertions=" + (KNIMEConstants.ASSERTIONS_ENABLED
                ? "on" : "off"));
        l.info("# host=" + getHostname());
        try {
            l.info("# username=" + User.getUsername());
        } catch (Exception ex) {
            l.info("# username=<unknown>");
        }
        l.info("# max mem=" + Runtime.getRuntime().maxMemory() / (1024 * 1024)
                + "MB");
        l.info("# application=" + OSGIHelper.getApplicationName());
        l.info("#############################################################");
    }

    /** Write copyright message. */
    private static void copyrightMessage() {
        NodeLogger l = getLogger(NodeLogger.class);
        l.info("# Copyright, 2003 - 2013                                    #");
        l.info("# University of Konstanz, Germany.                          #");
        l.info("# Chair for Bioinformatics and Information Mining           #");
        l.info("# Prof. Dr. Michael R. Berthold                             #");
        l.info("# and KNIME GmbH, Konstanz, Germany                         #");
        l.info("# website: http://www.knime.org                             #");
        l.info("# email: contact@knime.org                                  #");
    }

    /** The Log4J logger to which all messages are logged. */
    private final Logger m_logger;

    /**
     * Hidden default constructor, logger created by
     * <code>java.lang.Class</code>.
     *
     * @param c The logger created by Class name.
     */
    private NodeLogger(final Class<?> c) {
        m_logger = Logger.getLogger(c);
    }

    /**
     * Hidden default constructor, logger created by just a name.
     *
     * @param s The name of the logger.
     */
    private NodeLogger(final String s) {
        m_logger = Logger.getLogger(s);
    }

    /**
     * Creates a new <code>NodeLogger</code> for the given Class.
     *
     * @param c The logger's Class.
     * @return A new logger for this Class.
     */
    public static NodeLogger getLogger(final Class<?> c) {
        String s = c.getName();
        if (LOGGERS.containsKey(s)) {
            return LOGGERS.get(s);
        } else {
            NodeLogger logger = new NodeLogger(c);
            LOGGERS.put(s, logger);
            return logger;
        }
    }

    /**
     * Creates a new <code>NodeLogger</code> for the given name.
     *
     * @param s The logger's String.
     * @return A new logger for the given name.
     */
    public static NodeLogger getLogger(final String s) {
        if (LOGGERS.containsKey(s)) {
            return LOGGERS.get(s);
        } else {
            NodeLogger logger = new NodeLogger(s);
            LOGGERS.put(s, logger);
            return logger;
        }
    }

    /**
     * Write warning message into this logger.
     *
     * @param o The object to print.
     */
    public void warn(final Object o) {
        m_logger.warn(o);
    }

    /**
     * Write debugging message into this logger.
     *
     * @param o The object to print.
     */
    public void debug(final Object o) {
        m_logger.debug(o);
    }

    /**
     * Write info message into this logger.
     *
     * @param o The object to print.
     */
    public void info(final Object o) {
        m_logger.info(o);
    }

    /**
     * Write error message into the logger.
     *
     * @param o The object to print.
     */
    public void error(final Object o) {
        m_logger.error(o);
    }

    /**
     * Write fatal error message into the logger.
     *
     * @param o The object to print.
     */
    public void fatal(final Object o) {
        m_logger.fatal(o);
    }

    /**
     * Write warning message and throwable into this logger.
     *
     * @param o The object to print.
     * @param t The exception to log at debug level, including its stack trace.
     */
    public void warn(final Object o, final Throwable t) {
        this.warn(o);
        if (t != null) {
            this.debug(o, t);
        }
    }

    /**
     * Write debugging message and throwable into this logger.
     *
     * @param o The object to print.
     * @param t The exception to log, including its stack trace.
     */
    public void debug(final Object o, final Throwable t) {
        m_logger.debug(o, t);
    }

    /**
     * Write info message and throwable into this logger.
     *
     * @param o The object to print.
     * @param t The exception to log at debug level, including its stack trace.
     */
    public void info(final Object o, final Throwable t) {
        this.info(o);
        if (t != null) {
            this.debug(o, t);
        }
    }

    /**
     * Write error message and throwable into the logger.
     *
     * @param o The object to print.
     * @param t The exception to log at debug level, including its stack trace.
     */
    public void error(final Object o, final Throwable t) {
        this.error(o);
        if (t != null) {
            this.debug(o, t);
        }
    }

    /**
     * Check assert and write into logger if failed.
     *
     * @param b The expression to check.
     * @param m Print this message if failed.
     */
    public void assertLog(final boolean b, final String m) {
        if (KNIMEConstants.ASSERTIONS_ENABLED) {
            m_logger.assertLog(b, "ASSERT " + m);
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
            m_logger.assertLog(b, "ASSERT " + m);
            // for stacktrace
            if (!b & e != null) {
                m_logger.debug("ASSERT\t " + m, e);
            }
        }
    }

    /**
     * Writes CODING PROBLEM plus this message into this logger as error.
     *
     * @param o The message to print.
     */
    public void coding(final Object o) {
        m_logger.error("CODING PROBLEM\t" + o);
    }

    /**
     * Writes <i>CODING PROBLEM</i> plus this message, as well as the the
     * message of the throwable into this logger as error and debug.
     *
     * @param o The message to print.
     * @param t The exception to log at debug level, including its stack trace.
     */
    public void coding(final Object o, final Throwable t) {
        this.coding(o);
        if (t != null) {
            this.debug(o, t);
        }
    }

    /**
     * Write fatal error message and throwable into the logger.
     *
     * @param o The object to print.
     * @param t The exception to log at debug level, including its stack trace.
     */
    public void fatal(final Object o, final Throwable t) {
        this.fatal(o);
        if (t != null) {
            this.debug(o, t);
        }
    }

    /**
     * Adds a new {@link java.io.Writer} with the given level to this logger.
     *
     * @param writer The writer to add.
     * @param minLevel The minimum level to output.
     * @param maxLevel The maximum level to output.
     */
    public static final void addWriter(final Writer writer,
            final LEVEL minLevel, final LEVEL maxLevel) {
        // remove the writer first if existent
        if (WRITER.containsKey(writer)) {
            Appender a = WRITER.get(writer);
            Logger.getRootLogger().removeAppender(a);
            WRITER.remove(writer);
        }
        // register new appender
        WriterAppender app =
                new WriterAppender(new PatternLayout("%-5p\t %c{1}\t %."
                        + MAX_CHARS + "m\n"), writer);
        app.setImmediateFlush(true);
        LevelRangeFilter filter = new LevelRangeFilter();
        filter.setLevelMin(transLEVEL(minLevel));
        filter.setLevelMax(transLEVEL(maxLevel));
        app.addFilter(filter);
        Logger.getRootLogger().addAppender(app);
        WRITER.put(writer, app);
    }

    /**
     * Removes the previously added {@link java.io.Writer} from the logger.
     *
     * @param writer The Writer to remove.
     */
    public static final void removeWriter(final Writer writer) {
        Appender o = WRITER.get(writer);
        if (o != null) {
            if (o != FILE_APPENDER) {
                Logger.getRootLogger().removeAppender(o);
            }
        } else {
            getLogger(NodeLogger.class).warn(
                    "Could not delete writer: " + writer);
        }
    }

    /**
     * @param level minimum log level
     * @see #setLevel(NodeLogger.LEVEL)
     */
    @Deprecated
    public static void setLevelIntern(final LEVEL level) {
        setLevel(level);
    }

    /**
     * Sets an new minimum logging level for all internal appenders, that are,
     * log file, and <code>System.out</code> and <code>System.err</code>
     * appender. The maximum logging level stays <code>LEVEL.ALL</code> for
     * all appenders.
     *
     * @param level new minimum logging level
     */
    public static void setLevel(final LEVEL level) {
        getLogger(NodeLogger.class).info(
                "Changing logging level to " + level.toString());
        LevelRangeFilter filter = new LevelRangeFilter();
        filter.setLevelMin(transLEVEL(level));
        filter.setLevelMax(transLEVEL(LEVEL.FATAL));
        FILE_APPENDER.clearFilters();
        // SERR_APPENDER.clearFilters();
        SOUT_APPENDER.clearFilters();
        FILE_APPENDER.addFilter(filter);
        // SERR_APPENDER.addFilter(filter);
        SOUT_APPENDER.addFilter(filter);
    }

    /**
     * Returns the minimum logging retrieved from the underlying Log4J logger.
     *
     * @return minimum logging level
     */
    public LEVEL getLevel() {
        return transLevel(m_logger.getLevel());
    }

    /**
     * Checks if debug logging level is enabled.
     *
     * @return <code>true</code> if debug logging level is enabled, otherwise
     *         <code>false</code>
     */
    public boolean isDebugEnabled() {
        return m_logger.isDebugEnabled();
    }

    /**
     * Checks if info logging level is enabled.
     *
     * @return <code>true</code> if info logging level is enabled, otherwise
     *         <code>false</code>
     */
    public boolean isInfoEnabled() {
        return m_logger.isInfoEnabled();
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
        return m_logger.isEnabledFor(transLEVEL(level));
    }

    /**
     * Translates this logging <code>LEVEL</code> into Log4J logging levels.
     *
     * @param level the <code>LEVEL</code> to translate
     * @return the Log4J logging level
     */
    private static Level transLEVEL(final LEVEL level) {
        switch (level) {
        case DEBUG:
            return Level.DEBUG;
        case INFO:
            return Level.INFO;
        case WARN:
            return Level.WARN;
        case ERROR:
            return Level.ERROR;
        case FATAL:
            return Level.FATAL;
        default:
            return Level.ALL;
        }
    }

    /**
     * Translates Log4J logging level into this <code>LEVEL</code>.
     *
     * @param level the Level to translate
     * @return this logging LEVEL
     */
    private static LEVEL transLevel(final Level level) {
        if (level == Level.DEBUG) {
            return LEVEL.DEBUG;
        } else if (level == Level.INFO) {
            return LEVEL.INFO;
        } else if (level == Level.WARN) {
            return LEVEL.WARN;
        } else if (level == Level.ERROR) {
            return LEVEL.ERROR;
        } else if (level == Level.FATAL) {
            return LEVEL.FATAL;
        } else {
            return LEVEL.ALL;
        }
    }

    private static String getHostname() {
        try {
            InetAddress localMachine = InetAddress.getLocalHost();
            return localMachine.getHostName();
        } catch (Exception uhe) {
            return "<unknown host>";
        }
    }
}
