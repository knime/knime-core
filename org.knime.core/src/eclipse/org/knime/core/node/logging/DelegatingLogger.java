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
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.function.Supplier;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.RendererSupport;
import org.apache.log4j.varia.LevelRangeFilter;
import org.knime.core.eclipseUtil.OSGIHelper;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.NodeContextInformation;
import org.knime.core.node.NodeLoggerPatternLayout;
import org.knime.core.node.logging.LogBuffer.BufferedLogMessage;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.contextv2.JobExecutorInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2.ExecutorType;
import org.knime.core.util.LogfileAppender;
import org.knime.core.util.User;

/**
 * Wrapper for the {@link Logger log4j Logger} instances.
 *
 * Its static state holds configuration for the logging behavior, instances of the class wrap log4j instances and
 * delegate log statements.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
final class DelegatingLogger {

    /** Default log file appender. */
    private static Appender logFileAppender;

    /**
     * Maximum number of chars (10000) printed on <code>System.out</code> and
     * <code>System.err</code>.
     */
    private static final int MAX_CHARS = 10000;

    /**
     * Layout for "per-workflow" logging. Is overwritten when initializing logging based on a configuration, if a global
     * logfile appender is configured. In this case, its layout overwrites this fallback layout.
     */
    static Layout workflowDirLogfileLayout = new PatternLayout("%-5p\t %-30c{1}\t %." + MAX_CHARS + "m\n");

    static boolean logInWFDir;

    /**
     * Optional filter to use when per-workflow logging is active and should not wrap global logfile filter.
     */
    static LevelRangeFilter levelFilter;

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
        if (m_logger.isEnabledFor(bufMsg.level())) {
            final var logEvent = new LoggingEvent(m_logger.getName(), m_logger,
                bufMsg.instant().toEpochMilli(), bufMsg.level(),  bufMsg.message(), bufMsg.cause());
            m_logger.callAppenders(logEvent);
        }
    }

    void log(final Level level, final Supplier<Object> supplier, final boolean omitContext, final Throwable cause) {
        if (!m_logger.isEnabledFor(level)) {
            return;
        }
        m_logger.log(level, toKNIMELogMessage(m_logger, omitContext, supplier.get()), cause);
    }

    void log(final Level level, final Object o, final boolean omitContext, final Throwable cause) {
        if (!m_logger.isEnabledFor(level)) {
            return;
        }
        m_logger.log(level, toKNIMELogMessage(m_logger, omitContext, o), cause);
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
     * @param omitContext flag to omit the node context
     * @return a KNIMELogMessage that not only contains the log message but also the information about the workflow
     * and node that belong to the log message if applicable
     */
    private static Object toKNIMELogMessage(final Logger logger, final boolean omitContext, final Object message) {
        // context information not needed by global settings
        if (!logNodeId && !logInWFDir && !logWFDir && !logJobId) {
            return message;
        }
        NodeContextInformation nodeContext = null;
        File workflowDir = null;
        UUID jobID = null;
        final var context = omitContext ? null : NodeContext.getContext();
        if (context != null) {
            if (logNodeId) {
                //retrieve and store the node id only if the user has requested to log it
                final var nodeContainer = context.getNodeContainer();
                if (nodeContainer != null) {
                    nodeContext = new NodeContextInformation(nodeContainer.getID(), nodeContainer.getName());
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
        return new KNIMELogMessage(logger, nodeContext, workflowDir, jobID,  message);
    }

    /**
     * Creates a new, non-activated, {@link LogfileAppender logfile appender} for the given workflow directory for use
     * in "per-workflow logging". Call {@link FileAppender#activateOptions()} in order to activate the appender.
     *
     * @param appenderName name for the appender, usually the path to the workflow directory
     * @param workflowDir workflow directory to create knime.log file in
     * @return log file appender for the given workflow directory
     */
    static LogfileAppender createWorkflowLogAppender(final String appenderName, final Path workflowDirPath) {
        //use the KNIME specific LogfileAppender that moves larger log files into a separate zip file
        //and that implements equals and hash code to ensure that two LogfileAppender
        //with the same name are considered equal to prevent duplicate appender registration
        final var workflowDir = workflowDirPath.toFile();
        final var fileAppender = new LogfileAppender(workflowDir);
        fileAppender.setLayout(workflowDirLogfileLayout);
        fileAppender.setName(appenderName);

        // set up filter chain that first removes messages that do not belong into the specific workflow dir
        // appender and then asks a level range filter (the global logfile filter or a local one)
        final var workflowDirFilter = new Filter() {
            @Override
            public int decide(final LoggingEvent event) {
                // DENY cuts off filtering early and not consult the next filter
                if (!(logInWFDir && event.getMessage() instanceof KNIMELogMessage kmsg)) {
                    // if workflow logs are disabled, we don't need to check the rest
                    // we only log our custom KNIMELogMessage-wrapped messages
                    return Filter.DENY;
                }
                // no workflow dir means it's a "global" message
                final var isGlobal = kmsg.workflowDir() == null;
                if (isGlobal) {
                    return logGlobalInWFDir ? Filter.NEUTRAL : Filter.DENY;
                }
                // some "local" message
                // now comes the more expensive test
                return workflowDir.equals(kmsg.workflowDir()) ? Filter.NEUTRAL : Filter.DENY;
            }
        };
        // this builds a filter chain (equivalent to calling `setNext` on `workflowDirFilter` and then adding
        // that one to the appender, or wrapping the level range filter in the `workflowDirFilter`)
        fileAppender.addFilter(workflowDirFilter);
        // avoid adding `null` filter, because after that no filter can be added anymore...
        if (levelFilter != null) {
            fileAppender.addFilter(levelFilter);
        } else if (logFileAppender.getFilter() != null) {
            fileAppender.addFilter(logFileAppender.getFilter());
        }
        return fileAppender;
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
