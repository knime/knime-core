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

import java.io.File;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.RendererSupport;
import org.knime.core.eclipseUtil.OSGIHelper;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLoggerPatternLayout;
import org.knime.core.node.logging.LogBuffer.BufferedLogMessage;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowEvent.Type;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.contextv2.JobExecutorInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2.ExecutorType;
import org.knime.core.util.LogfileAppender;
import org.knime.core.util.User;

/**
 * Logger that delegates to an underlying logging framework.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class DelegatingLogger {

    private static final Map<String, Appender> WF_APPENDER = new HashMap<>();

    /**
     * Listens to workflow changes e.g. when a workflow is closed to unregister all related workflow directory logger.
     * */
    private MyWorkflowListener m_listener;

    /** Default log file appender. */
    private static Appender logFileAppender;

    /**
     * Maximum number of chars (10000) printed on <code>System.out</code> and
     * <code>System.err</code>.
     */
    private static final int MAX_CHARS = 10000;

    static Layout workflowDirLogfileLayout = new PatternLayout("%-5p\t %-30c{1}\t %." + MAX_CHARS + "m\n");

    static boolean logInWFDir;

    static boolean logGlobalInWFDir;

    static boolean logNodeId;

    private static boolean logWFDir;

    private static boolean logJobId;

    private final Logger m_logger;

    DelegatingLogger(final Logger logger) {
        m_logger = logger;
    }

    DelegatingLogger(final String s) {
        this(Logger.getLogger(s));
    }

    static void setLogFileAppender(final Appender appender) {
        logFileAppender = appender;
    }

    static Appender getLogFileAppender() {
        return logFileAppender;
    }

    void log(final BufferedLogMessage bufMsg) {
        // basically what `Logger#log(...)` would do, but we need to construct the LogEvent ourselves
        // to insert the timestamp at log-buffer-time not at the current time.
        // this way, anyone using a pattern which prints the log timestamp will see messages "out of order"
        // but with a correct, non-confusing timestamp.

        // we don't use `getLoggerInternal` since buffered log messages are from when no workflow dir was available
        if (m_logger.isEnabledFor(bufMsg.level())) {
            final var logEvent = new LoggingEvent(m_logger.getName(), m_logger,
                bufMsg.instant().toEpochMilli(), bufMsg.level(),  bufMsg.message(), bufMsg.cause());
            m_logger.callAppenders(logEvent);
        }
    }

    public void log(final Level level, final Supplier<Object> supplier, final Throwable cause,
        final boolean considerWFDirAppenders) {
        final var internalLogger = getLoggerInternal(considerWFDirAppenders);
        if (internalLogger.isEnabledFor(level)) {
            internalLogger.log(level, toKNIMELogMessage(internalLogger, supplier.get(),
                considerWFDirAppenders), cause);
        }
    }

    public void log(final Level level, final Object o, final Throwable cause, final boolean considerWFDirAppenders) {
        final var internalLogger = getLoggerInternal(considerWFDirAppenders);
        internalLogger.log(level, toKNIMELogMessage(internalLogger, o, considerWFDirAppenders), cause);
    }

    /**
     * Use this method whenever you want to log a message. It ensures that the right logger is used and that all
     * required appenders are added to it e.g. workflow directory appender (if node context is considered).
     *
     * @param considerWFDirAppenders set to {@code false} in order to ignore {@link #LOG_IN_WF_DIR} and not set up
     *     workflow dir appenders on this call
     *
     * @return the correct logger to use and ensures that any workflow relative log file appenders are registered
     * properly
     */
    private Logger getLoggerInternal(final boolean considerWFDirAppenders) {
        if (considerWFDirAppenders && logInWFDir) {
            addWorkflowDirAppender();
        }
        return m_logger;
    }

    /**
     * @param layout checks if any of the KNIME specific flags e.g. node id is set in the layout pattern and ensures
     * that the corresponding boolean flag is enabled.
     */
    static void checkLayoutFlags(final Layout layout) {
        if (layout instanceof PatternLayout pl) {
            final String conversionPattern = pl.getConversionPattern();
            //enable the node id logging if one of the appender contains the node id or node name pattern
            logNodeId |= conversionPattern.contains("%" + NodeLoggerPatternLayout.NODE_ID);
            logNodeId |= conversionPattern.contains("%" + NodeLoggerPatternLayout.NODE_NAME);
            logNodeId |= conversionPattern.contains("%" + NodeLoggerPatternLayout.QUALIFIER);
            if (logNodeId) {
                LogLog.debug("Node id logging enabled due to pattern layout");
            }
            //enable the workflow logging if one of the appender contains the workflow pattern
            logWFDir |= conversionPattern.contains("%" + NodeLoggerPatternLayout.WORKFLOW_DIR);
            if (logWFDir) {
                LogLog.debug("Workflow directory logging enabled due to pattern layout");
            }
            //enable the job id logging if one of the appender contains the job id pattern
            logJobId |= conversionPattern.contains("%" + NodeLoggerPatternLayout.JOB_ID);
            if (logJobId) {
                LogLog.debug("Job id logging enabled due to pattern layout");
            }
        }
    }

    /**
     * @param message the logging message
     * @return a KNIMELogMessage that not only contains the log message but also the information about the workflow
     * and node that belong to the log message if applicable
     */
    private static Object toKNIMELogMessage(final Logger logger, final Object message,
        final boolean withNodeContext) {
        if (!logNodeId && !logInWFDir && !logWFDir && !logJobId) {
            return message;
        }
        NodeID nodeID = null;
        String nodeName = null;
        File workflowDir = null;
        UUID jobID = null;
        if (withNodeContext) {
            final var context = NodeContext.getContext();
            if (context != null) {
                if (logNodeId) {
                    //retrieve and store the node id only if the user has requested to log it
                    final var nodeContainer = context.getNodeContainer();
                    if (nodeContainer != null) {
                        nodeID = nodeContainer.getID();
                        nodeName = nodeContainer.getName();
                    }
                }
                if (logInWFDir || logWFDir || logJobId) {
                    final var workflowManager = context.getWorkflowManager();
                    if (workflowManager != null) {
                        final var wc = workflowManager.getContextV2();
                        if (wc != null) {
                            workflowDir = wc.getExecutorInfo().getLocalWorkflowPath().toFile();
                            jobID = wc.getExecutorType() != ExecutorType.ANALYTICS_PLATFORM
                                    ? ((JobExecutorInfo)wc.getExecutorInfo()).getJobId() : null;
                        }
                    }
                }
            }
        }
        return new KNIMELogMessage(logger, nodeID, nodeName, workflowDir, jobID,  message);
    }

    /**
     * Adds a new workflow dir appender based on the current node context if it is available.
     */
    private void addWorkflowDirAppender() {
        final var context = NodeContext.getContext();
        if (context != null) {
            final var workflowManager = context.getWorkflowManager();
            if (workflowManager != null) {
                final var workflowContext = workflowManager.getContextV2();
                if (workflowContext != null) {
                    addWorkflowDirAppender(workflowContext.getExecutorInfo().getLocalWorkflowPath().toFile());
                }
            }
        }
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
        final var logger = m_logger;
        final String workflowDirPath = workflowDir.getPath();
        if (workflowDirPath == null) {
            return;
        }
        var wfAppender = WF_APPENDER.get(workflowDirPath);
        if (wfAppender != null) {
            logger.addAppender(wfAppender);
        } else {
            //we do the getAppender twice to prevent the synchronize block on subsequent calls!!!
            synchronized (WF_APPENDER) {
                //we need a synchronize block otherwise we might create a second appender that opens a file handle
                //which never get closed and thus the copying of a full log file to the zip file fails
                wfAppender = WF_APPENDER.get(workflowDirPath);
                if (wfAppender == null) {
                    //use the KNIME specific LogfileAppender that moves larger log files into a separate zip file
                    //and that implements equals and hash code to ensure that two LogfileAppender
                    //with the same name are considered equal to prevent duplicate appender registration
                    final var fileAppender = new LogfileAppender(workflowDir);
                    fileAppender.setLayout(workflowDirLogfileLayout);
                    fileAppender.setName(workflowDirPath);
                    final var mainFilter = logFileAppender.getFilter();
                    fileAppender.addFilter(new Filter() {
                        @Override
                        public int decide(final LoggingEvent event) {
                            final Object msg = event.getMessage();
                            if (msg instanceof KNIMELogMessage kmsg) {
                                final File msgDir = kmsg.workflowDir(); //can be null
                                if ((logGlobalInWFDir && msgDir == null) || logInWFDir && workflowDir.equals(msgDir)) {
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
    private static void removeWorkflowDirAppender(final File workflowDir) {
        if (workflowDir == null) {
            //if the workflowDir is null we do not need to remove the extra log appender
            return;
        }
        final String workflowDirPath = workflowDir.getPath();
        if (workflowDirPath != null) {
            synchronized (WF_APPENDER) {
                final var appender = WF_APPENDER.remove(workflowDirPath);
                if (appender != null) {
                    appender.close();
                    //Remove the appender from all open node loggers
                    @SuppressWarnings("unchecked")
                    final Enumeration<Logger> allLoggers =
                            Logger.getRootLogger().getLoggerRepository().getCurrentLoggers();
                    while (allLoggers.hasMoreElements()) {
                        allLoggers.nextElement().removeAppender(appender);
                    }
                }
            }
        }
    }

    /**
     * Listener that calls {@link NodeLogger#removeWorkflowDirAppender(File)} on workflow closing to
     * remove all workflow relative log file appender.
     * @author Tobias Koetter, KNIME.com
     */
    private static class MyWorkflowListener implements WorkflowListener {
        @Override
        public void workflowChanged(final WorkflowEvent event) {
            if (event != null && Type.NODE_REMOVED == event.getType()) {
                final Object val = event.getOldValue();
                if (val instanceof WorkflowManager wm) {
                    final ReferencedFile workflowWorkingDir = wm.getWorkingDir();
                    if (workflowWorkingDir != null) {
                        removeWorkflowDirAppender(workflowWorkingDir.getFile());
                    }
                }
            }
        }
    }

    static void setWorkflowDirLogfileLayout(final Layout layout) {
        DelegatingLogger.workflowDirLogfileLayout = layout;
        checkLayoutFlags(layout);
    }

    /** Write start logging message to info logger of this class. */
    static void startMessage() {
        final var l = NodeLogger.getLogger(NodeLogger.class);
        l.info("#########################################################################################");
        l.info("#                                                                                       #");
        l.info("# "
                + ("Welcome to KNIME Analytics Platform v" + KNIMEConstants.VERSION + " (Build "
                        + KNIMEConstants.BUILD_DATE
                        + ")                                          ").substring(0, 85) + " #");
        l.info("# Based on Eclipse, http://www.eclipse.org                                              #");
        l.info("#                                                                                       #");
        l.info("#########################################################################################");
        l.info("#                                                                                       #");
        copyrightMessage();
        l.info("#                                                                                       #");
        l.info("#########################################################################################");
        if (logFileAppender instanceof LogfileAppender lA) {
            l.info("# For more details see the KNIME log file:                                              #");
            l.info("# " + lA.getFile());
            l.info("#---------------------------------------------------------------------------------------#");
        }
        // format following Date#toString (which was used here before)
        final var fmt = DateTimeFormatter.ofPattern("EEE LLL dd HH:mm:ss zzz yyyy");
        l.info("# logging date=" + fmt.format(ZonedDateTime.now()));
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
        } catch (Exception ex) { // NOSONAR
            l.info("# username=<unknown>");
        }
        l.info("# max mem=" + Runtime.getRuntime().maxMemory() / (1024 * 1024) + "MB");
        l.info("# application=" + OSGIHelper.getApplicationName());
        l.info("# KNID=" + KNIMEConstants.getKNID());
        l.info("#########################################################################################");
    }

    /** Write copyright message. */
    private static void copyrightMessage() {
        final var l = NodeLogger.getLogger(NodeLogger.class);
        l.info("# Copyright by KNIME AG, Zurich, Switzerland and others.                                #");
        l.info("# Website: http://www.knime.com                                                         #");
        l.info("# E-mail: contact@knime.com                                                             #");
    }

    private static String getHostname() {
        final var hostName = KNIMEConstants.getHostname();
        return hostName != null ? hostName : "<unknown host>";
    }

    Level getLevel() {
        return m_logger.getLevel();
    }

    boolean isEnabledFor(final Level level) {
        return m_logger.isEnabledFor(level);
    }

    static String renderMessage(final Logger logger, final KNIMELogMessage kmsg) {
        final var message = kmsg.msg();
        if (message == null) {
            return "";
        }
        if (message instanceof String stringMessage) {
            return stringMessage;
        }
        final var repository = logger.getLoggerRepository();
        if (repository instanceof RendererSupport rs) {
            return rs.getRendererMap().findAndRender(message);
        }
        return message.toString();
    }

}
