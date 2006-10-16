/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 * 
 * History
 *   15.05.2006(sieb, ohl): reviewed 
 */
package org.knime.core.node;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.HashMap;

import javax.xml.parsers.FactoryConfigurationError;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.varia.LevelRangeFilter;
import org.apache.log4j.varia.NullAppender;
import org.apache.log4j.xml.DOMConfigurator;
import org.knime.core.util.LogfileAppender;

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

    /** Assertions are on or off. */
    private static final boolean ASSERT;
    static {
        boolean flag;
        try {
            assert false;
            flag = false;
        } catch (AssertionError ae) {
            flag = true;
        }
        ASSERT = flag;
    }

    /** Keeps set of <code>NodeLogger</code> elements by classname as key. */
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
        // check if KNIME log4j config exisist in users home
        File knimeDir = new File(KNIMEConstants.getKNIMEHomeDir());
        File log4j = new File(knimeDir, "log4j-1.1.0.xml");
        if (!log4j.exists()) {
            InputStream in = NodeLogger.class.getClassLoader()
                    .getResourceAsStream("log4j-1.1.0.xml");
            if (in != null) {
                byte[] buf = new byte[4096];
                try {
                    OutputStream out = new FileOutputStream(log4j);
                    int count = 0;
                    while ((count = in.read(buf)) > 0) {
                        out.write(buf, 0, count);
                    }
                    in.close();
                    out.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        if (System.getProperty("log4j.configuration") == null) {
            try {
                DOMConfigurator.configure(log4j.toURL());
            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            } catch (FactoryConfigurationError ex) {
                ex.printStackTrace();
            }
        }

        // init root logger
        Logger root = Logger.getRootLogger();
        Appender a = root.getAppender("stderr");
        a = root.getAppender("stdout");
        SOUT_APPENDER = (a != null) ? a : new NullAppender();
        a = root.getAppender("knimelog");
        FILE_APPENDER = (a != null) ? a : new NullAppender();

        startMessage();
    }

    /** Write start logging message to info logger of this class. */
    private static void startMessage() {
        NodeLogger l = getLogger(NodeLogger.class);
        l.info("#############################################################");
        l.info("#                                                           #");
        l.info("# Welcome to KNIME v1.1.0 (Build August 16, 2006)           #");
        l.info("# the Konstanz Information Miner                            #");
        l.info("# Based on Eclipse 3.2, www.eclipse.org                     #");
        l.info("# Uses: Java5, GEF, Log4J, Weka, JFreeChart                 #");
        l.info("#                                                           #");
        l.info("#############################################################");
        l.info("#                                                           #");
        copyrightMessage();
        l.info("#                                                           #");
        l.info("#############################################################");
        if (FILE_APPENDER instanceof LogfileAppender) {
            l.info("# For more details see:" 
                    + "                                     #");
            l.info("# " + ((LogfileAppender)FILE_APPENDER).getFile());
            l.info("#-----------------------------------------------" 
                    + "------------#");
        }

        l.info("# logging date=" + new Date());
        l.info("# java.version=" + System.getProperty("java.version"));
        l.info("# java.vm.version=" + System.getProperty("java.vm.version"));
        l.info("# java.vendor=" + System.getProperty("java.vendor"));
        l.info("# os.name=" + System.getProperty("os.name"));
        l.info("# number of CPUs=" 
                + Runtime.getRuntime().availableProcessors());
        l.info("# assertions=" + (ASSERT ? "on" : "off"));
        l.info("#############################################################");
    }

    /** Write copyright message. */
    private static void copyrightMessage() {
        NodeLogger l = getLogger(NodeLogger.class);
        l.info("# Copyright, 2003 - 2006                                    #");
        l.info("# University of Konstanz, Germany.                          #");
        l.info("# Chair for Bioinformatics and Information Mining           #");
        l.info("# Prof. Dr. Michael R. Berthold                             #");
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
     * @param t The exception to log, including its stack trace.
     */
    public void warn(final Object o, final Throwable t) {
        this.warn(o);
        this.debug(o, t);
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
     * @param t The exception to log, including its stack trace.
     */
    public void info(final Object o, final Throwable t) {
        this.info(o);
        this.debug(o, t);
    }

    /**
     * Write error message and throwable into the logger.
     * 
     * @param o The object to print.
     * @param t The exception to log, including its stack trace.
     */
    public void error(final Object o, final Throwable t) {
        this.error(o);
        this.error(t.getMessage());
        this.debug(o, t);
    }

    /**
     * Check assert and write into logger if failed.
     * 
     * @param b The expression to check.
     * @param m Print this message if failed.
     */
    public void assertLog(final boolean b, final String m) {
        if (ASSERT) {
            m_logger.assertLog(b, "ASSERT " + m);
        } else {
            // assertions are off, but write to knime.log anyway
            m_logger.debug("ASSERT\t " + m);
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
        if (ASSERT) {
            m_logger.assertLog(b, "ASSERT " + m + " " + e.getMessage());
            m_logger.debug("ASSERT\t " + m, e);
        } else {
            // assertions are off, but write to knime.log anyway
            m_logger.debug("ASSERT\t " + m, e);
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
     * Writes CODING PROBLEM plus this message, as well as the the message of
     * the throwable into this logger as error and debug.
     * 
     * @param o The message to print.
     * @param t The throwable's message to print.
     */
    public void coding(final Object o, final Throwable t) {
        this.coding(o);
        this.coding(t.getMessage());
        this.debug(o, t);
    }

    /**
     * Write fatal error message and throwable into the logger.
     * 
     * @param o The object to print.
     * @param t The exception to log, including its stack trace.
     */
    public void fatal(final Object o, final Throwable t) {
        this.fatal(o);
        this.fatal(t.getMessage());
        this.debug(o, t);
    }

    /**
     * Adds a new <code>java.io.Writer</code> with the given level to this
     * logger.
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
        WriterAppender app = new WriterAppender(new PatternLayout(
                "%-5p\t %c{1}\t %." + MAX_CHARS + "m\n"), writer);
        app.setImmediateFlush(true);
        LevelRangeFilter filter = new LevelRangeFilter();
        filter.setLevelMin(getLevel(minLevel));
        filter.setLevelMax(getLevel(maxLevel));
        app.addFilter(filter);
        Logger.getRootLogger().addAppender(app);
        WRITER.put(writer, app);
    }

    /**
     * Removes the previously added <code>java.io.Writer</code> from the
     * logger.
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
     * Sets an new minimum logging level for all internal appenders, that are,
     * log file, and System.out and System.err appender. The maximum loggings
     * stays LEVEL.ALL for all appenders.
     * 
     * @param level The new minimum logging level.
     */
    public static void setLevelIntern(final LEVEL level) {
        getLogger(NodeLogger.class).info(
                "Changing logging level to " + level.toString());
        LevelRangeFilter filter = new LevelRangeFilter();
        filter.setLevelMin(getLevel(level));
        filter.setLevelMax(getLevel(LEVEL.FATAL));
        FILE_APPENDER.clearFilters();
        // SERR_APPENDER.clearFilters();
        SOUT_APPENDER.clearFilters();
        FILE_APPENDER.addFilter(filter);
        // SERR_APPENDER.addFilter(filter);
        SOUT_APPENDER.addFilter(filter);
    }

    /**
     * Translates this loging levels into Log4J logging levels.
     * 
     * @param level The level to translate.
     * @return The Log4J logging level.
     */
    private static Level getLevel(final LEVEL level) {
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
        case ALL:
            return Level.ALL;
        default:
            return Level.ALL;
        }
    }

}
