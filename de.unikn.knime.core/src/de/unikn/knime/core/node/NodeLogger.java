/*
 * @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * --------------------------------------------------------------------- *
 *   This source code, its documentation and all appendant files         *
 *   are protected by copyright law. All rights reserved.                *
 *                                                                       *
 *   Copyright, 2003 - 2006                                              *
 *   Universitaet Konstanz, Germany.                                     *
 *   Lehrstuhl fuer Angewandte Informatik                                *
 *   Prof. Dr. Michael R. Berthold                                       *
 *                                                                       *
 *   You may not modify, publish, transmit, transfer or sell, reproduce, *
 *   create derivative works from, distribute, perform, display, or in   *
 *   any way exploit any of the content, in whole or in part, except as  *
 *   otherwise expressly permitted in writing by the copyright owner.    *
 * --------------------------------------------------------------------- *
 */
package de.unikn.knime.core.node;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.TTCCLayout;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.varia.LevelRangeFilter;
import org.apache.log4j.varia.NullAppender;

/**
 * The general logger used to write info, warnings, errors, debugging,
 * and assert messages and exceptions into the Log4J logger. By default, the
 * <code>System.out</code> stream and <i>knime.log</i> (created in the
 * system's temp directory) are appended to the logger.
 * 
 * @author Thomas Gabriel, Konstanz University
 */
public final class NodeLogger {

    /** The message levels. */
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

    /** The KNIME log file name. */
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
    
    private static final ConsoleAppender SERR_APPENDER;
    private static final ConsoleAppender SOUT_APPENDER;
    private static final Appender FILE_APPENDER;

    /**
     * Init Log4J logger and append <code>System.out</code> stream and
     * <i>knime.log</i> file.
     */
    static {
        // init root logger
        Logger root = Logger.getRootLogger();
        root.setLevel(Level.ALL);
        // add System.out
        SOUT_APPENDER = new ConsoleAppender(new PatternLayout(
                "%-5p\t %c{1}\t %m\n"), "System.out");
        SOUT_APPENDER.setImmediateFlush(true);
        LevelRangeFilter filter = new LevelRangeFilter();
        filter.setLevelMin(Level.DEBUG);
        filter.setLevelMax(Level.FATAL);
        SOUT_APPENDER.addFilter(filter);
        // sout.setThreshold(Level.INFO);
        root.addAppender(SOUT_APPENDER);
        // add System.err
        SERR_APPENDER = new ConsoleAppender(new PatternLayout(
                "%-5p\t %t : %c\t %m\n"), "System.err");
        SERR_APPENDER.setImmediateFlush(true);
        SERR_APPENDER.setThreshold(Level.ERROR);
        root.addAppender(SERR_APPENDER);
        FileAppender tempFileAppender;
        try {
            // get user home
            String tmpDir = KNIMEConstants.KNIME_HOME_DIR + File.separator;
            // check if home/.knime exists
            File tempDir = new File(tmpDir);
            if (!tempDir.exists()) {
                tempDir.mkdir();
            }
            // check old for old knime log files
            File oldLog = new File(tmpDir + LOG_FILE);
            if (oldLog.exists()) {
                oldLog.renameTo(new File(tmpDir + LOG_FILE + ".bak"));
                oldLog.delete();
            }
            // add knime.log file appender
            tempFileAppender = new FileAppender(new TTCCLayout(
                    "yy.MM.dd HH:mm:ss"), tmpDir + LOG_FILE);
            // WriterAppender file =
            // new DailyRollingFileAppender(
            // new TTCCLayout("yy.MM.dd HH:mm:ss"),
            // tmpDir + LOG_FILE, "yyMMdd");
            tempFileAppender.setName(LOG_FILE);
            tempFileAppender.setImmediateFlush(true);
            tempFileAppender.setThreshold(Level.ALL);
            root.addAppender(tempFileAppender);
            // write start logging message
            startMessage();
        } catch (IOException ioe) {
            // write start logging message
            startMessage();
            // print error
            NodeLogger logger = getLogger(NodeLogger.class);
            logger.error(
                    "Could not create temp-file: "
                            + KNIMEConstants.KNIME_HOME_DIR + File.separator
                            + LOG_FILE, ioe);
            tempFileAppender = null;
        }
        if (tempFileAppender == null) {
            FILE_APPENDER = new NullAppender();
        } else {
            FILE_APPENDER = tempFileAppender;
        }
    }

    /** Write start logging message. */  
    private static void startMessage() {
        NodeLogger l = getLogger(NodeLogger.class);
        l.info("#############################################################");
        l.info("#                                                           #");
        l.info("# KNIME Logging " + new Date());
        l.info("#                                                           #");
        l.info("#############################################################");
        l.info("#                                                           #");
        copyrightMessage();
        l.info("#                                                           #");
        l.info("#############################################################");
        l.info("# For more details see:                                     #");
        l.info("# " + KNIMEConstants.KNIME_HOME_DIR + File.separator
                        + LOG_FILE);
        l.info("#-----------------------------------------------------------#");
        // logger.info("user.name=" + System.getProperty("user.name"));
        l.info("# java.version=" + System.getProperty("java.version"));
        l.info("# java.vm.version=" + System.getProperty("java.vm.version"));
        l.info("# os.name=" + System.getProperty("os.name"));
        l.info("# assertions=" + (ASSERT ? "on" : "off"));
        l.info("#############################################################");
    }

    /** Write copyright message. */
    private static void copyrightMessage() {
        NodeLogger l = getLogger(NodeLogger.class);
        l.info("# Copyright, 2003 - 2006                                    #");
        l.info("# Konstanz University, Germany.                             #");
        l.info("# Chair for Bioinformatics and Information Mining           #");
        l.info("# Prof. Dr. Michael R. Berthold                             #");
    }

    /** The Log4J logger assigned to this NodeLogger. */
    private final Logger m_logger;

    /** Don't log the following types. */
    private static final String[] DONT_LOG = new String[]{"joelib",
            "org.openscience"};
    static {
        for (int i = 0; i < DONT_LOG.length; i++) {
            Logger log = Logger.getLogger(DONT_LOG[i]);
            log.setLevel(Level.OFF);
        }
    }

    /**
     * Hidden default constructor; logs the given Class.
     * 
     * @param c The Class to log.
     */
    private NodeLogger(final Class c) {
        m_logger = Logger.getLogger(c);
    }

    /**
     * Hidden default constructor; logs the given class by name.
     * 
     * @param s The String to log.
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
    public static NodeLogger getLogger(final Class c) {
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
     * Creates a new <code>NodeLogger</code> for the given Class.
     * 
     * @param s The logger's String.
     * @return A new logger of this type.
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
     * @param o The message to print.
     */
    public void coding(final Object o) {
        m_logger.error("CODING PROBLEM\t" + o);
    }
    
    /**
     * Writes CODING PROBLEM plus this message, as well as the the message of
     * the throwable into this logger as error and debug.
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
                "%-5p\t %c{1}\t %m\n"), writer);
        app.setImmediateFlush(true);
        LevelRangeFilter filter = new LevelRangeFilter();
        filter.setLevelMin(getLevel(minLevel));
        filter.setLevelMax(getLevel(maxLevel));
        app.addFilter(filter);
        Logger.getRootLogger().addAppender(app);
        WRITER.put(writer, app);
    }

    /**
     * Removes the previously added <code>java.io.Writer</code> from the logger.
     * 
     * @param writer The Writer to remove.
     */
    public static final void removeWriter(final Writer writer) {
        Appender o = WRITER.get(writer);
        if (o != null) {
            if (o != FILE_APPENDER 
                    && o != SERR_APPENDER && o != SOUT_APPENDER) {
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
     * @param level The new minimum logging level.
     */
    public static void setLevelIntern(final LEVEL level) {
        getLogger(NodeLogger.class).info(
                "Changing logging level to " + level.toString());
        LevelRangeFilter filter = new LevelRangeFilter();
        filter.setLevelMin(getLevel(level));
        filter.setLevelMax(getLevel(LEVEL.FATAL));
        FILE_APPENDER.clearFilters();
        SERR_APPENDER.clearFilters();
        SOUT_APPENDER.clearFilters();
        FILE_APPENDER.addFilter(filter);
        SERR_APPENDER.addFilter(filter);
        SOUT_APPENDER.addFilter(filter);
    }

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
