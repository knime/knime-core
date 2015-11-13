/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.InetAddress;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.RendererSupport;
import org.apache.log4j.varia.LevelRangeFilter;
import org.apache.log4j.varia.NullAppender;
import org.apache.log4j.xml.DOMConfigurator;
import org.knime.core.eclipseUtil.OSGIHelper;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowEvent.Type;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.EclipseUtil;
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
 * to this logger. Note, calling {@link #setLevel(LEVEL)} does only effect
 * the minimum logging level of the default loggers. All other writers' levels
 * have to be set before hand.
 *
 * @author Thomas Gabriel, Tobias Koetter, KNIME.com
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

    /**
     * Class that encapsulates all information of a log message in KNIME such as the {@link NodeID} and
     * workflow directory if the message can be assigned to them.
     * @author Tobias Koetter, KNIME.com
     */
    class KNIMELogMessage {

        private final File m_workflowDir;
        private Object m_msg;
        private NodeID m_nodeID;
        private String m_nodeName;

        /**
         * @param nodeID the NodeID if available (can be <code>null</code>)
         * @param nodeName the name of the node if available (can be <code>null</code>)
         * @param workflowDir the workflow location if available (can be <code>null</code>)
         * @param msg the logging message
         */
        private KNIMELogMessage(final NodeID nodeID, final String nodeName, final File workflowDir,
            final Object msg) {
            m_nodeID = nodeID;
            m_nodeName = nodeName;
            m_workflowDir = workflowDir;
            m_msg = msg;
        }

        /**
         * @return the workflowDir
         */
        File getWorkflowDir() {
            return m_workflowDir;
        }

        /**
         * @return the nodeID
         */
        NodeID getNodeID() {
            return m_nodeID;
        }

        /**
         * @return the nodeName
         */
        String getNodeName() {
            return m_nodeName;
        }

        /**
         * @return the msg
         */
        Object getMsg() {
            return m_msg;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            if (m_msg == null) {
                return "";
            }
            String renderedMessage;
            if (m_msg instanceof String) {
                renderedMessage = (String)m_msg;
            } else {
                LoggerRepository repository = m_logger.getLoggerRepository();
                if (repository instanceof RendererSupport) {
                    RendererSupport rs = (RendererSupport)repository;
                    renderedMessage = rs.getRendererMap().findAndRender(m_msg);
                } else {
                    renderedMessage = m_msg.toString();
                }
            }
            return renderedMessage;
        }
    }

    /**
     * Listener that calls {@link NodeLogger#removeWorkflowDirAppender(File)} on workflow closing to
     * remove all workflow relative log file appender.
     * @author Tobias Koetter, KNIME.com
     */
    private class MyWorkflowListener implements WorkflowListener {
        /**{@inheritDoc}*/
        @Override
        public void workflowChanged(final WorkflowEvent event) {
            if (event != null && Type.NODE_REMOVED.equals(event.getType())) {
                final Object val = event.getOldValue();
                if (val instanceof WorkflowManager) {
                    final WorkflowManager wm = (WorkflowManager)val;
                    final ReferencedFile workflowWorkingDir = wm.getWorkingDir();
                    if (workflowWorkingDir != null) {
                        removeWorkflowDirAppender(workflowWorkingDir.getFile());
                    }
                }
            }
        }
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

    /** Keeps set of <code>NodeLogger</code> elements by class name as key. */
    private static final Map<String, NodeLogger> LOGGERS =
            new HashMap<String, NodeLogger>();

    /** Map of additionally added writers: Writer -> Appender. */
    private static final Map<Writer, WriterAppender> WRITER =
            new HashMap<Writer, WriterAppender>();

    private static final Map<String, Appender> WF_APPENDER = new HashMap<>();

    /**
     * Maximum number of chars (10000) printed on <code>System.out</code> and
     * <code>System.err</code>.
     */
    private static final int MAX_CHARS = 10000;

    /** Default log file appender. */
    private static final Appender LOG_FILE_APPENDER;

    private static boolean LOG_IN_WF_DIR = false;

    private static boolean LOG_GLOBAL_IN_WF_DIR = false;

    private static boolean LOG_NODE_ID = false;

    private static boolean LOG_WF_DIR = false;

    private static Layout WF_DIR_LOG_FILE_LAYOUT = new PatternLayout("%-5p\t %-30c{1}\t %." + MAX_CHARS + "m\n");

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
            Appender a = root.getAppender(LOGFILE_APPENDER);
            if (a != null) {
                LOG_FILE_APPENDER = a;
                WF_DIR_LOG_FILE_LAYOUT = a.getLayout();
                checkLayoutFlags(WF_DIR_LOG_FILE_LAYOUT);
            } else {
                root.warn("Could not find '" + LOGFILE_APPENDER + "' appender");
                LOG_FILE_APPENDER = new NullAppender();
            }
        } else {
            LOG_FILE_APPENDER = new NullAppender();
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
            //we use log4j3 as log file name starting with KNIME 2.12 which introduced the new
            //org.knime.core.node.NodeLoggerPatternLayout class. This way older versions of KNIME can also open
            //workflows created with >2.12 since they simply ignore the new log file
            File log4j = new File(knimeDir, "log4j3.xml");

            File legacyFile = new File(knimeDir, "log4j-1.1.0.xml");
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
                final File log4jOld = new File(knimeDir, "log4j.xml");
                if (!log4jOld.exists()) {
                    //this is a new workspace so simply use the new log file
                    copyCurrentLog4j(log4j, latestLog4jConfig);
                } else if (checkPreviousLog4j(log4jOld, latestLog4jConfig)) {
                    //this is an old workspace <KNIME 2.12 with a default log file so delete the old log file
                    copyCurrentLog4j(log4j, latestLog4jConfig);
                    log4jOld.delete();
                } else {
                    //this is an old workspace with an adapted log4j file which we should continue to use
                    final File templateFile = new File(knimeDir, "log4j3.xml_template");
                    if (!templateFile.exists()) {
                        //create a template file which contains the new logging settings
                        copyCurrentLog4j(templateFile, latestLog4jConfig);
                    }
                    log4j = log4jOld;
                }
            } else if (checkPreviousLog4j(log4j, latestLog4jConfig)) {
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
        try (final InputStream in = NodeLogger.class.getClassLoader().getResourceAsStream(latestLog4jConfig);
                final FileOutputStream out = new FileOutputStream(dest);){
            if (in == null) {
                throw new IOException("Latest log4j-config '"
                        + latestLog4jConfig + "' not found");
            }
            FileUtil.copy(in, out);
        }
    }

    private static String getLatestLog4jConfig() throws IOException {
        ClassLoader cl = NodeLogger.class.getClassLoader();

        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            String file = "log4j/log4j-" + i + ".xml";
            try (final InputStream in = cl.getResourceAsStream(file);) {
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
        try (final FileInputStream reader = new FileInputStream(current);) {
            final byte[] currentContents = new byte[(int)current.length()];
            reader.read(currentContents);
            final ClassLoader cl = NodeLogger.class.getClassLoader();
            for (int k = 0; k < Integer.MAX_VALUE; k++) {
                String file = "log4j/log4j-" + k + ".xml";
                if (latestLog4jConfig.equals(file)) {
                    break;
                }
                // compare the two files
                try (
                        final BufferedReader currentReader =
                                new BufferedReader(new InputStreamReader(new ByteArrayInputStream(currentContents)));
                        final BufferedReader existingReader =
                                new BufferedReader(new InputStreamReader(cl.getResourceAsStream(file)));) {
                    boolean match = true;
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

    /** Write start logging message to info logger of this class. */
    private static void startMessage() {
        NodeLogger l = getLogger(NodeLogger.class);
        l.info("#########################################################################################");
        l.info("#                                                                                       #");
        l.info("# "
                + ("Welcome to the KNIME Analytics Platform v" + KNIMEConstants.VERSION + " (Build "
                        + KNIMEConstants.BUILD_DATE
                        + ")                                          ").substring(0, 85) + " #");
        l.info("# Based on Eclipse, http://www.eclipse.org                                              #");
        l.info("#                                                                                       #");
        l.info("#########################################################################################");
        l.info("#                                                                                       #");
        copyrightMessage();
        l.info("#                                                                                       #");
        l.info("#########################################################################################");
        if (LOG_FILE_APPENDER instanceof LogfileAppender) {
            l.info("# For more details see the KNIME log file:                                              #");
            l.info("# " + ((LogfileAppender)LOG_FILE_APPENDER).getFile());
            l.info("#---------------------------------------------------------------------------------------#");
        }

        l.info("# logging date=" + new Date());
        l.info("# java.version=" + System.getProperty("java.version"));
        l.info("# java.vm.version=" + System.getProperty("java.vm.version"));
        l.info("# java.vendor=" + System.getProperty("java.vendor"));
        l.info("# os.name=" + System.getProperty("os.name"));
        l.info("# os.arch=" + System.getProperty("os.arch"));
        l.info("# number of CPUs=" + Runtime.getRuntime().availableProcessors());
        l.info("# assertions=" + (KNIMEConstants.ASSERTIONS_ENABLED ? "on" : "off"));
        l.info("# host=" + getHostname());
        try {
            l.info("# username=" + User.getUsername());
        } catch (Exception ex) {
            l.info("# username=<unknown>");
        }
        l.info("# max mem=" + Runtime.getRuntime().maxMemory() / (1024 * 1024) + "MB");
        l.info("# application=" + OSGIHelper.getApplicationName());
        l.info("# ID=" + KNIMEConstants.getKNIMEInstanceID());
        l.info("#########################################################################################");
    }

    /** Write copyright message. */
    private static void copyrightMessage() {
        NodeLogger l = getLogger(NodeLogger.class);
        l.info("# Copyright by KNIME GmbH, Konstanz, Germany and others.                                #");
        l.info("# Website: http://www.knime.org                                                         #");
        l.info("# E-mail: contact@knime.org                                                             #");
    }

    /** The Log4J logger to which all messages are logged. Do not access it directly if you want to log a message
     * but use the {@link #getLoggerInternal()} method instead.*/
    private final Logger m_logger;

    /**Listens to workflow changes e.g. when a workflow is closed to unregister all related workflow directory logger.*/
    private MyWorkflowListener m_listener;

    /**
     * Hidden default constructor, logger created by
     * <code>java.lang.Class</code>.
     *
     * @param c The logger created by Class name.
     */
    private NodeLogger(final Class<?> c) {
        this(Logger.getLogger(c));
    }

    /**
     * Hidden default constructor, logger created by just a name.
     *
     * @param s The name of the logger.
     */
    private NodeLogger(final String s) {
        this(Logger.getLogger(s));
    }

    /**
     * Hidden constructor that should be used by all other constructors to assign the logger
     * @param logger
     */
    private NodeLogger(final Logger logger) {
        m_logger = logger;
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
        synchronized (LOGGERS) {
            if (LOGGERS.containsKey(s)) {
                return LOGGERS.get(s);
            } else {
                NodeLogger logger = new NodeLogger(s);
                LOGGERS.put(s, logger);
                return logger;
            }
        }
    }

    /**
     * Write warning message into this logger.
     *
     * @param o The object to print.
     */
    public void warn(final Object o) {
        getLoggerInternal().warn(getLogObject(o));
    }

    /**
     * Write debugging message into this logger.
     *
     * @param o The object to print.
     */
    public void debug(final Object o) {
        getLoggerInternal().debug(getLogObject(o));
    }


    /**
     * Write debugging message into this logger. The message is logged without a node context. This method should only
     * be used when you know that there is no node context available.
     *
     * @param o The object to print.
     * @since 3.1
     */
    public void debugWithoutContext(final Object o) {
        m_logger.debug(o);
    }


    /**
     * @param layout checks if any of the KNIME specific flags e.g. node id is set in the layout pattern and ensures
     * that the corresponding boolean flag is enabled.
     */
    private static void checkLayoutFlags(final Layout layout) {
        if (layout instanceof PatternLayout) {
            final PatternLayout pl = (PatternLayout)layout;
            final String conversionPattern = pl.getConversionPattern();
            //enable the node id logging if one of the appender contains the node id or node name pattern
            LOG_NODE_ID |= conversionPattern.contains("%" + NodeLoggerPatternLayout.NODE_ID);
            LOG_NODE_ID |= conversionPattern.contains("%" + NodeLoggerPatternLayout.NODE_NAME);
            LOG_NODE_ID |= conversionPattern.contains("%" + NodeLoggerPatternLayout.QUALIFIER);
            if (LOG_NODE_ID) {
                LogLog.debug("Node id logging enabled due to pattern layout");
            }
            //enable the workflow logging if one of the appender contains the workflow pattern
            LOG_WF_DIR |= conversionPattern.contains("%" + NodeLoggerPatternLayout.WORKFLOW_DIR);
            if (LOG_WF_DIR) {
                LogLog.debug("Workflow directory logging enabled due to pattern layout");
            }
        }
    }

    /**
     * @param message the logging message
     * @return a KNIMELogMessage that not only contains the log message but also the information about the workflow
     * and node that belong to the log message if applicable
     */
    private Object getLogObject(final Object message) {
        if (!LOG_NODE_ID && !LOG_IN_WF_DIR && !LOG_WF_DIR) {
            return message;
        }
        final NodeContext context = NodeContext.getContext();
        NodeID nodeID = null;
        String nodeName = null;
        File workflowDir = null;
        if (context != null) {
            if (LOG_NODE_ID) {
                //retrieve and store the node id only if the user has requested to log it
                final NodeContainer nodeContainer = context.getNodeContainer();
                if (nodeContainer != null) {
                    nodeID = nodeContainer.getID();
                    nodeName = nodeContainer.getName();
                }
            }
            if (LOG_IN_WF_DIR || LOG_WF_DIR) {
                final WorkflowManager workflowManager = context.getWorkflowManager();
                if (workflowManager != null) {
                    final WorkflowContext workflowContext = workflowManager.getContext();
                    if (workflowContext != null) {
                        workflowDir = workflowContext.getCurrentLocation();
                    }
                }
            }
        }
        return new KNIMELogMessage(nodeID, nodeName, workflowDir, message);
    }

    /**
     * Use this method whenever you want to log a message. It ensures that the right logger is used and that all
     * required appenders are added to it e.g. workflow directory appender.
     * @return the correct logger to use and ensures that any workflow relative log file appenders are registered
     * properly
     */
    private Logger getLoggerInternal() {
        if (LOG_IN_WF_DIR) {
            final NodeContext context = NodeContext.getContext();
            if (context != null) {
                final WorkflowManager workflowManager = context.getWorkflowManager();
                if (workflowManager != null) {
                    final WorkflowContext workflowContext = workflowManager.getContext();
                    if (workflowContext != null) {
                        addWorkflowDirAppender(workflowContext.getCurrentLocation());
                    }
                }
            }
        }
        return m_logger;
    }

    /**
     * Adds a new workflow directory logger for the given workflow directory if it doesn't exists yet.
     * @param workflowDir the directory of the workflow that should be logged to
     */
    private void addWorkflowDirAppender(final File workflowDir) {
        if (workflowDir == null) {
            //if the workflowDir is null we do not need to append an extra log appender
            return;
        }
        //in this method we have to use the logger directly to prevent a deadlock!!!
        final Logger logger = m_logger;
        final String workflowDirPath = workflowDir.getPath();
        if (workflowDirPath == null) {
            return;
        }
        Appender wfAppender = WF_APPENDER.get(workflowDirPath);
        if (wfAppender != null) {
            logger.addAppender(wfAppender);
        } else {
            //we do the getAppender twice to prevent the synchronize block on subsequent calls!!!
            synchronized (WF_APPENDER) {
                //we need a synchronize block otherwise we might create a second appender that opens a file handle
                //which never get closed and thus the copying of a full log file to the zip file fails
                wfAppender = WF_APPENDER.get(workflowDirPath);
                if (wfAppender == null) {
                    //use the KNIME specific LogfielAppender that moves larger log files into a separate zip file
                    //and that implements equals and hash code to ensure that two LogfileAppender
                    //with the same name are considered equal to prevent duplicate appender registration
                    final FileAppender fileAppender = new LogfileAppender(workflowDir);
                    fileAppender.setLayout(WF_DIR_LOG_FILE_LAYOUT);
                    fileAppender.setName(workflowDirPath);
                    final Filter mainFilter = LOG_FILE_APPENDER.getFilter();
                    fileAppender.addFilter(new Filter() {
                        @Override
                        public int decide(final LoggingEvent event) {
                            final Object msg = event.getMessage();
                            if (msg instanceof KNIMELogMessage) {
                                final KNIMELogMessage kmsg = (KNIMELogMessage)msg;
                                final File msgDir = kmsg.getWorkflowDir(); //can be null
                                if ((LOG_GLOBAL_IN_WF_DIR && msgDir == null)
                                        || LOG_IN_WF_DIR && workflowDir.equals(msgDir)) {
                                    //return only neutral to let the log level based filters decide if we log this event
                                    if (mainFilter != null) {
                                        return mainFilter.decide(event);
                                    }
                                    return Filter.NEUTRAL;
                                }
                            }
                            return Filter.DENY;
                        }
                    });
                    //we have to call this function to activate the writer!!!
                    fileAppender.activateOptions();
                    logger.addAppender(fileAppender);
                    WF_APPENDER.put(workflowDirPath, fileAppender);
                    if (m_listener == null) {
                        m_listener = new MyWorkflowListener();
                        WorkflowManager.ROOT.addListener(m_listener);
                    }
                }
            }
        }
    }

    /**
     * Removes any extra workflow directory appender if it exists.
     * @param workflowDir the directory of the workflow that should no longer be logged
     */
    private void removeWorkflowDirAppender(final File workflowDir) {
        if (workflowDir == null) {
            //if the workflowDir is null we do not need to remove the extra log appender
            return;
        }
        final String workflowDirPath = workflowDir.getPath();
        if (workflowDirPath != null) {
            synchronized (WF_APPENDER) {
                final Appender appender = WF_APPENDER.remove(workflowDirPath);
                if (appender != null) {
                    appender.close();
                    //Remove the appender from all open node loggers
                    @SuppressWarnings("unchecked")
                    final Enumeration<Logger> allLoggers = Logger.getRootLogger().getLoggerRepository().getCurrentLoggers();
                    while (allLoggers.hasMoreElements()) {
                        allLoggers.nextElement().removeAppender(appender);
                    }
                }
            }
        }
    }

    /**
     * Write info message into this logger.
     *
     * @param o The object to print.
     */
    public void info(final Object o) {
        getLoggerInternal().info(getLogObject(o));
    }

    /**
     * Write error message into the logger.
     *
     * @param o The object to print.
     */
    public void error(final Object o) {
        getLoggerInternal().error(getLogObject(o));
    }

    /**
     * Write fatal error message into the logger.
     *
     * @param o The object to print.
     */
    public void fatal(final Object o) {
        getLoggerInternal().fatal(getLogObject(o));
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
        getLoggerInternal().debug(getLogObject(o), t);
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
        if (KNIMEConstants.ASSERTIONS_ENABLED && !b) {
            getLoggerInternal().error("ASSERT " + m, new AssertionError(m));
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
            getLoggerInternal().assertLog(b, "ASSERT " + m);
            // for stacktrace
            if (!b & e != null) {
                getLoggerInternal().debug("ASSERT\t " + m, e);
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
        if (KNIMEConstants.ASSERTIONS_ENABLED || EclipseUtil.isRunFromSDK()) {
            getLoggerInternal().error("CODING PROBLEM\t" + o);
        }
    }

    /**
     * Writes <i>CODING PROBLEM</i> plus this message, as well as the the message of the throwable into this logger as
     * error and debug. The event is only logged if assertions are enabled or KNIME is run from within the SDK.
     *
     * @param o the message to print
     * @param t the exception to log at debug level, including its stack trace
     */
    public void coding(final Object o, final Throwable t) {
        if (KNIMEConstants.ASSERTIONS_ENABLED || EclipseUtil.isRunFromSDK()) {
            this.coding(o);
            if (t != null) {
                this.debug(o, t);
            }
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
     * Write warning message, created by {@link String#format(String, Object...)} into this logger. The String is only
     * formatted if actually necessary.
     *
     * @param format the format for printing
     * @param args the arguments for {@link String#format(String, Object...)}
     * @throws NullPointerException if format argument is <code>null</code>
     * @since 2.10
     */
    public void warnWithFormat(final String format, final Object... args) {
        if (isEnabledFor(LEVEL.WARN)) {
            this.warn(String.format(format, args));
        }
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
        if (isEnabledFor(LEVEL.DEBUG)) {
            this.debug(String.format(format, args));
        }
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
        if (isEnabledFor(LEVEL.INFO)) {
            this.info(String.format(format, args));
        }
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
        if (isEnabledFor(LEVEL.ERROR)) {
            this.error(String.format(format, args));
        }
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
        if (isEnabledFor(LEVEL.FATAL)) {
            this.fatal(String.format(format, args));
        }
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
        if (KNIMEConstants.ASSERTIONS_ENABLED || EclipseUtil.isRunFromSDK()) {
            coding(String.format(format, args));
        }
    }

    /**
     * Adds a new {@link java.io.Writer} with the given level to this logger.
     *
     * @param writer The writer to add.
     * @param minLevel The minimum level to output.
     * @param maxLevel The maximum level to output.
     */
    public static void addWriter(final Writer writer,
            final LEVEL minLevel, final LEVEL maxLevel) {
        addWriter(writer, WF_DIR_LOG_FILE_LAYOUT, minLevel, maxLevel);
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
        final Appender a = Logger.getRootLogger().getAppender(KNIME_CONSOLE_APPENDER);
        final Layout layout;
        if (a != null) {
            layout = a.getLayout();
            checkLayoutFlags(layout);
        } else {
            layout = WF_DIR_LOG_FILE_LAYOUT;
        }
        addWriter(writer, layout, minLevel, maxLevel);
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
        WriterAppender app = new WriterAppender(layout, writer);
        app.setImmediateFlush(true);
        LevelRangeFilter filter = new LevelRangeFilter();
        filter.setLevelMin(transLEVEL(minLevel));
        filter.setLevelMax(transLEVEL(maxLevel));
        app.addFilter(filter);

        // remove the writer first if existent
        synchronized (WRITER) {
            if (WRITER.containsKey(writer)) {
                Appender a = WRITER.get(writer);
                Logger.getRootLogger().removeAppender(a);
                WRITER.remove(writer);
            }
            // register new appender
            WRITER.put(writer, app);
        }
        Logger.getRootLogger().addAppender(app);
        checkLayoutFlags(layout);
    }

    /**
     * Removes the previously added {@link java.io.Writer} from the logger.
     *
     * @param writer The Writer to remove.
     */
    public static void removeWriter(final Writer writer) {
        synchronized (WRITER) {
            Appender o = WRITER.get(writer);
            if (o != null) {
                if (o != LOG_FILE_APPENDER) {
                    Logger.getRootLogger().removeAppender(o);
                    WRITER.remove(writer);
                }
            } else {
                getLogger(NodeLogger.class).warn(
                        "Could not delete writer: " + writer);
            }
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
     * @deprecated user {@link #setAppenderLevelRange(String, LEVEL, LEVEL)} instead for more fine-grained control
     */
    @Deprecated
    public static void setLevel(final LEVEL level) {
        getLogger(NodeLogger.class).info(
                "Changing logging level to " + level.toString());
        try {
            setAppenderLevelRange(STDOUT_APPENDER, level, LEVEL.FATAL);
        } catch (NoSuchElementException ex) {
            // ignore it
        }
        try {
            setAppenderLevelRange(LOGFILE_APPENDER, level, LEVEL.FATAL);
        } catch (NoSuchElementException ex) {
            // ignore it
        }
    }



    /**
     * Returns the minimum logging retrieved from the underlying Log4J logger.
     *
     * @return minimum logging level
     */
    public LEVEL getLevel() {
        return transLevel(getLoggerInternal().getLevel());
    }

    /**
     * Checks if debug logging level is enabled.
     *
     * @return <code>true</code> if debug logging level is enabled, otherwise
     *         <code>false</code>
     */
    public boolean isDebugEnabled() {
        return getLoggerInternal().isDebugEnabled();
    }

    /**
     * Checks if info logging level is enabled.
     *
     * @return <code>true</code> if info logging level is enabled, otherwise
     *         <code>false</code>
     */
    public boolean isInfoEnabled() {
        return getLoggerInternal().isInfoEnabled();
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
        return getLoggerInternal().isEnabledFor(transLEVEL(level));
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

    /**
     * Sets a level range filter on the given appender.
     *
     * @param appenderName the name of the appender
     * @param min the minimum logging level
     * @param max the maximum logging level
     * @throws NoSuchElementException if the given appender does not exist
     * @since 2.8
     */
    public static void setAppenderLevelRange(final String appenderName, final LEVEL min, final LEVEL max)
            throws NoSuchElementException {
        Logger root = Logger.getRootLogger();
        Appender appender = root.getAppender(appenderName);
        if (appender == null) {
            throw new NoSuchElementException("Appender '" + appenderName + "' does not exist");
        }

        Filter filter = appender.getFilter();
        while ((filter != null) && !(filter instanceof LevelRangeFilter)) {
            filter = filter.getNext();
        }
        if (filter == null) {
            // add a new level range filter
            LevelRangeFilter levelFilter = new LevelRangeFilter();
            levelFilter.setLevelMin(transLEVEL(min));
            levelFilter.setLevelMax(transLEVEL(max));
            appender.addFilter(levelFilter);
        } else {
            // modify existing level range filter
            ((LevelRangeFilter) filter).setLevelMin(transLEVEL(min));
            ((LevelRangeFilter) filter).setLevelMax(transLEVEL(max));
        }
    }

    /**
     * Allows to enable/disable logging in the workflow directory. If enabled log messages that belong to workflow
     * are logged into a log file within the workflow directory itself in addition to the global KNIME log file.
     *
     * @param enable <code>true</code> if workflow relative logging should be enabled
     * @since 2.12
     */
    public static void logInWorkflowDir(final boolean enable) {
        LOG_IN_WF_DIR = enable;
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
        LOG_GLOBAL_IN_WF_DIR = enable;
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
        LOG_NODE_ID  = enable;
        LogLog.debug("Node ID logging set to: " + enable);
    }
}
