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
 *   24 Jun 2025 (manuelhotz): created
 */
package org.knime.core.node.logging;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.varia.NullAppender;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.KNIMELogMessage;
import org.knime.core.node.NodeLoggerConfig;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.LogfileAppender;

/**
 * Resource to unregister workflow log (and some static settings for visibility).
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings({"java:S1104", "java:S1444"})
public final class WorkflowLogCloseable implements AutoCloseable {

    /**
     * Reference counting for workflow log appender cleanup.
     */
    private static final Map<String, Integer> APPENDER_COUNTS = new ConcurrentHashMap<>();


    /* ======= Fields moved here for visibility to this class and from NodeLogger (but not exported). */
    /**
     * Maximum number of chars (10000) printed on <code>System.out</code> and
     * <code>System.err</code>.
     */
    private static final int MAX_CHARS = 10000;

    /**
     * Logfile layout for the workflow dir.
     */
    public static Layout workflowDirLogfileLayout = new PatternLayout("%-5p\t %-30c{1}\t %." + MAX_CHARS + "m\n");

    /** Default log file appender, public for access from {@link NodeLoggerConfig},
     * in this class to access its filter. */
    public static Appender logFileAppender;

    /**
     * Whether to log in the workflow directory or not.
     */
    public static boolean logInWfDir;

    /**
     * Whether to log messages without associated directory (i.e. "global" messages) into workflow logs.
     */
    public static boolean logGlobalInWfDir;

    /**
     * Include node id in log message.
     */
    public static boolean logNodeID;

    /**
     * Include workflow dir in log message.
     */
    public static boolean logWfDir;

    /**
     * Include job ID (Hub/Server only) in log message.
     */
    public static boolean logJobID;
    /* ======= End of moved fields. */


    private final WorkflowContextV2 m_workflowContext;

    WorkflowLogCloseable(final WorkflowContextV2 workflowContext) {
        m_workflowContext = workflowContext;
    }

    /**
     * Initializes the global logfile appender.
     *
     * @param isLoggingEnabled whether logging is enabled or not
     */
    // in this class to configure
    public static void initializeGlobalLogfileAppender(final boolean isLoggingEnabled) {
        if (logFileAppender != null) {
            // only once
            return;
        }
        if (isLoggingEnabled) {
            final var rootLogger = Logger.getRootLogger();
            final var a = rootLogger.getAppender(NodeLogger.LOGFILE_APPENDER);
            if (a != null) {
                logFileAppender = a;
                WorkflowLogCloseable.workflowDirLogfileLayout = a.getLayout();
                checkLayoutFlags(WorkflowLogCloseable.workflowDirLogfileLayout);
                return;
            } else {
                rootLogger.warn("Could not find '" + NodeLogger.LOGFILE_APPENDER + "' appender");
            }
        }
        logFileAppender = new NullAppender();
    }

    /**
     * Checks the layout for custom KNIME patterns and enables their usage if present.
     *
     * @param layout checks if any of the KNIME specific flags e.g. node id is set in the layout pattern and ensures
     * that the corresponding boolean flag is enabled.
     */
    public static void checkLayoutFlags(final Layout layout) {
        if (layout instanceof PatternLayout pl) {
            final String conversionPattern = pl.getConversionPattern();
            //enable the node id logging if one of the appender contains the node id or node name pattern
            WorkflowLogCloseable.logNodeID |= conversionPattern.contains("%" + NodeLoggerPatternConstants.NODE_ID);
            WorkflowLogCloseable.logNodeID |= conversionPattern.contains("%" + NodeLoggerPatternConstants.NODE_NAME);
            WorkflowLogCloseable.logNodeID |= conversionPattern.contains("%" + NodeLoggerPatternConstants.QUALIFIER);
            if (WorkflowLogCloseable.logNodeID) {
                LogLog.debug("Node id logging enabled due to pattern layout");
            }
            //enable the workflow logging if one of the appender contains the workflow pattern
            WorkflowLogCloseable.logWfDir |= conversionPattern.contains(
                "%" + NodeLoggerPatternConstants.WORKFLOW_DIR);
            if (WorkflowLogCloseable.logWfDir) {
                LogLog.debug("Workflow directory logging enabled due to pattern layout");
            }
            //enable the job id logging if one of the appender contains the job id pattern
            WorkflowLogCloseable.logJobID |= conversionPattern.contains("%" + NodeLoggerPatternConstants.JOB_ID);
            if (WorkflowLogCloseable.logJobID) {
                LogLog.debug("Job id logging enabled due to pattern layout");
            }
        }
    }

    @Override
    public void close() {
        unregisterFromWorkflowLog(m_workflowContext);
    }

    /**
     * Registers the workflow, identified by its given context, for a "workflow log" and returns a handle to
     * unregister it.
     *
     * For example, called by {@link WorkflowManager} when a workflow project is opened.
     *
     * @param workflowContext {@code null}able context of the workflow
     * @return close handle to unregister from workflow logging again
     */
    public static WorkflowLogCloseable registerForWorkflowLog(final WorkflowContextV2 workflowContext) {
        final var path = workflowContext != null ? workflowContext.getExecutorInfo().getLocalWorkflowPath() : null;
        if (addWorkflowLogAppender(path)) {
            return new WorkflowLogCloseable(workflowContext);
        } else {
            return new WorkflowLogCloseable(null);
        }
    }

    /**
     * Adds a new workflow directory logger for the given workflow directory if it doesn't exists yet.
     * @param workflowDir the directory of the workflow that should be logged to
     */
    private static boolean addWorkflowLogAppender(final Path workflowDir) {
        if (workflowDir == null) {
            NodeLogger.getLogger(NodeLogger.class)
                .debug("Skipping registration of workflow log appender, because workflow directory is not set");
            return false;
        }
        final var appenderName = workflowDir.toString();
        // if it is already registered, we don't make the effort to register it again, we just need to count
        // so we properly clean it up later
        final var countNow = APPENDER_COUNTS.merge(appenderName, 1, Integer::sum);
        NodeLogger.getLogger(NodeLogger.class)
            .debug("Incrementing count for workflow log appender: \"%s\"".formatted(appenderName));
        if (countNow > 1) {
            // we return true, because we need to schedule a decrement of the counter later
            // but someone else already registered it
            return true;
        }
        final var rootLogger = Logger.getRootLogger();
        synchronized (rootLogger) {
            NodeLogger.getLogger(NodeLogger.class).debug(
                "Registering workflow log appender for directory under name: \"%s\"".formatted(appenderName));
            final var fileAppender = createWorkflowLogAppender(appenderName, workflowDir);
            rootLogger.addAppender(fileAppender);
            return true;
        }
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
        // appender and then asks a level range filter (the global logfile filter)
        final var mainFilter = logFileAppender.getFilter();
        final var workflowDirFilter = new Filter() {
            @Override
            public int decide(final LoggingEvent event) {
                // DENY cuts off filtering early and not consult the next filter
                if (!(logInWfDir && event.getMessage() instanceof KNIMELogMessage kmsg)) {
                    // if workflow logs are disabled, we don't need to check the rest
                    // we only log our custom KNIMELogMessage-wrapped messages
                    return Filter.DENY;
                }
                // no workflow dir means it's a "global" message, otherwise it's for some workflow
                final var messageWfDir = kmsg.getWorkflowDir();
                if (!(messageWfDir == null ? logGlobalInWfDir : workflowDir.equals(messageWfDir))) {
                    return Filter.DENY;
                }
                return mainFilter == null ? Filter.NEUTRAL : mainFilter.decide(event);
            }
        };
        fileAppender.addFilter(workflowDirFilter);
        return fileAppender;
    }

    /**
     * Unregisters the workflow, identified by its given context, from "workflow logging".
     *
     * <i>Called by {@link WorkflowManager} when a project is closed.</i>
     *
     * @param workflowContext {@code null}able context of the workflow
     */
    @SuppressWarnings("null") // CheckUtils.checkState asserts not null
    private static void unregisterFromWorkflowLog(final WorkflowContextV2 workflowContext) {
        if (workflowContext == null) {
            return;
        }
        final var workflowDir = workflowContext.getExecutorInfo().getLocalWorkflowPath();
        final var appenderName = workflowDir.toString();
        final var countNow = APPENDER_COUNTS.merge(appenderName, -1, Integer::sum);
        final var remaining = countNow == 0 ? "none" : countNow;
        NodeLogger.getLogger(NodeLogger.class)
            .debug("Decremented count for workflow log appender (%s remaining): \"%s\"".formatted(remaining,
                appenderName));
        if (countNow > 0) {
            // others still need it
            return;
        }
        // last cow closes the gate
        final var rootLogger = Logger.getRootLogger();
        synchronized (rootLogger) {
            final var appender = rootLogger.getAppender(appenderName);
            CheckUtils.checkState(appender != null,
                "Expected workflow log appender \"%s\" not found anymore".formatted(appenderName));
            // remove, then close to avoid any potential for usage of the closed appender
            rootLogger.removeAppender(appenderName);
            appender.close();
            NodeLogger.getLogger(NodeLogger.class).debug(
                "Removed and closed workflow log appender named: \"%s\"".formatted(appenderName));
        }
    }
}

