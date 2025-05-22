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
 *   3 May 2024 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.node.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.RootLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;
import org.knime.core.node.logging.KNIMELogger;


/**
 * Test for "per-workflow logfile detached from global logfile" (AP-22429).
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
public final class EnhAP22429_WorkflowLoggingTest extends WorkflowTestCase {

    private static final String LOGGER_NAME = EnhAP22429_WorkflowLoggingTest.class.getName();

    private static final String TEST_MSG = "Global DEBUG message from WorkflowTestCase";

    @BeforeEach
    public void clearLog() throws Exception {
        final var log = getWorkflowLog();
        Files.deleteIfExists(log);
    }

    @Test
    public void testPerWorkflowLogging() throws Exception {
        final var logger = NodeLogger.getLogger(LOGGER_NAME);
        logger.debug("GLOBAL message before workflow loading");

        // enable per-workflow logs with detached levels _before_ creating it (i.e. by loading the workflow)
        // otherwise we would follow the "global" logfile filter (levels)
        NodeLogger.logInWorkflowDir(LEVEL.DEBUG, LEVEL.FATAL);

        final var workflowLogFile = getWorkflowLog();
        assertFalse(Files.exists(workflowLogFile),
                "No workflow logfile should exist yet at \"%s\"".formatted(workflowLogFile));

        // we know the workflow log appender will be called like the workflow directory (parent folder of the knime.log)
        final var appenderName = workflowLogFile.getParent().toString();
        // the appender gets set up when loading the workflow
        final var baseID = loadAndSetWorkflow();

        final var hasReceivedMessages = new AtomicBoolean();
        final var hasReceivedGlobalTestMsg = new AtomicBoolean();

        assertNotNull(Logger.getRootLogger().getAppender(NodeLogger.LOGFILE_APPENDER),
                "Missing main %s appender".formatted(NodeLogger.LOG_FILE));

        // create overlapping registration for the same workflow log (needs reference counting to be correct)
        try (final var secondRegistration = KNIMELogger.registerForWorkflowLog(getManager().getContextV2())) {

            // the workflow contains a Java Snippet node that prints a single debug log statement
            final var javaSnippetNode = baseID.createChild(1);
            checkState(javaSnippetNode, InternalNodeContainerState.CONFIGURED);

            final var appender = RootLogger.getRootLogger().getAppender(appenderName);
            assertNotNull(appender, "Workflow log appender should have been created under name \"%s\""
                    .formatted(workflowLogFile.toString()));
            appender.addFilter(new Filter() {
                @Override
                public int decide(final LoggingEvent event) {
                    hasReceivedMessages.set(true);
                    if (TEST_MSG.equals(event.getRenderedMessage())) {
                        hasReceivedGlobalTestMsg.set(true);
                    }
                    return Filter.NEUTRAL;
                }
            });

            // briefly enable global msg to workflow log routing
            NodeLogger.logGlobalMsgsInWfDir(true);
            NodeLogger.getLogger(LOGGER_NAME).debug(TEST_MSG);
            NodeLogger.logGlobalMsgsInWfDir(false);

            // log statements from the Java Snippet node have a node context and should go into the workflow log
            executeAllAndWait();
            checkState(javaSnippetNode, InternalNodeContainerState.EXECUTED);

            // close workflow before exiting try-with-resource to ensure that reference counting of workflow log 
            // appender works
            closeWorkflow();
        }

        assertTrue(hasReceivedMessages.get(), "Expected dummy appender to have received messages");
        assertTrue(hasReceivedGlobalTestMsg.get(),
                "Expected dummy appender to have received the global DEBUG test message");

        assertTrue(Files.exists(workflowLogFile),
                "Workflow logfile should now exist at \"%s\"".formatted(workflowLogFile));
        assertTrue(Files.lines(workflowLogFile).count() > 0,
                "Logfile should now contain some content at \"%s\"".formatted(workflowLogFile));
        assertTrue(Files.lines(workflowLogFile) //
                .anyMatch(logLine -> logLine.contains("DEBUG message from JavaSnippet")),
                "DEBUG message from JavaSnippet is present");

        // appender should now be removed and closed
        // it should also not be available via the log4j classes anymore
        final var expectedNullAppender = RootLogger.getRootLogger().getAppender(appenderName);
        assertNull(expectedNullAppender, "Workflow log appender should have been removed after workflow close");
        // TODO assert fileappender closed by appending to closed appender and seeing "log4j:ERROR" pop up on stdout
    }

    private final Path getWorkflowLog() throws Exception {
        final var wfDir = getDefaultWorkflowDirectory();
        return Path.of(wfDir.getPath()).resolve("knime.log");
    }

}