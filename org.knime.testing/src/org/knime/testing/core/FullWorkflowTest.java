/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
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
 *   17.05.2006 (Fabian Dill): created
 */
package org.knime.testing.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.varia.LevelRangeFilter;
import org.junit.After;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.KNIMETimer;
import org.knime.testing.node.config.TestConfigNodeModel;
import org.knime.testing.node.config.TestConfigSettings;

// TODO: check, that the number of correct results corresponds to the number
// of out-ports of the node under test
// Solve the question about model and data in/out

/**
 *
 * @since 2.6
 */
public class FullWorkflowTest extends TestCase implements WorkflowTest {
    /**
     * Factory for full workflow tests.
     *
     * @since 2.6
     */
    public static final WorkflowTestFactory factory =
            new WorkflowTestFactory() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public WorkflowTest createTestcase(final File workflowDir,
                        final File saveLocation) {
                    return new FullWorkflowTest(workflowDir, saveLocation);
                }
            };

    private final class MsgPattern {
        private static final String REGEXPRPATTERN = "_!_";

        private final String m_msg;

        private final String m_patternPart1;

        private final Pattern m_regExpr;

        private final String m_patternPart3;

        /**
         * Creates a new pattern for a message. In the simple case this is the
         * expected message. But it could also be a message containing a regular
         * expression. The equals method pays attention to it then.<br />
         * The part in the pattern that starts with _!_ and ends with _!_ will
         * be considered a regular expression. If a pattern consists of
         * PART1_!_RegExpr_!_PART3 then the message to compare against it will
         * be split in three parts: Part 1 from the beginning of the message of
         * length of PART1, part 2 from the end of the message - of length of
         * PART3, and part 3, the middle part which must match the RegExpr part
         * of the pattern.
         *
         * @see #equals(Object)
         *
         * @param msg the expected message (pattern). Can't be null.
         */
        public MsgPattern(final String msg) {

            m_msg = msg;

            // figure out if the pattern contains a regular expression
            int regExStartIdx = m_msg.indexOf(REGEXPRPATTERN);
            int regExEndIdx = m_msg.lastIndexOf(REGEXPRPATTERN);

            if (regExStartIdx + REGEXPRPATTERN.length() < regExEndIdx) {
                // seems it does
                String part1 = m_msg.substring(0, regExStartIdx);
                String regExprPart =
                        m_msg.substring(
                                regExStartIdx + REGEXPRPATTERN.length(),
                                regExEndIdx);
                String part3 =
                        m_msg.substring(regExEndIdx + REGEXPRPATTERN.length());
                Pattern p;
                try {
                    int compileFlags = Pattern.DOTALL | Pattern.UNICODE_CASE;
                    p = Pattern.compile(regExprPart, compileFlags);
                } catch (PatternSyntaxException pse) {
                    logger.error("Pattern to compare message to contains "
                            + "invalid regular expression: \"" + regExprPart
                            + "\" (full pattern: " + m_msg
                            + "). Using regular string equals.");
                    part1 = null;
                    p = null;
                    part3 = null;
                }

                m_patternPart1 = part1;
                m_regExpr = p;
                m_patternPart3 = part3;

            } else {
                // no regular expression
                m_regExpr = null;
                m_patternPart1 = null;
                m_patternPart3 = null;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            throw new UnsupportedOperationException(
                    "Don't call equal. Use matches.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return super.hashCode();
        }

        /**
         * Compares the actual message with the pattern.
         *
         * @param actualMsg the message to test.
         * @return true if the message matches the pattern, false otherwise.
         */
        public boolean matches(final String actualMsg) {
            if (m_regExpr == null) {
                // no regular expression - do a normal string equals
                return m_msg.equals(actualMsg); // (m_msg can't be null)
            }

            if (actualMsg == null) {
                return false;
            }

            // we must compare those three parts

            if (actualMsg.length() < (m_patternPart1.length() + m_patternPart3
                    .length())) {
                return false;
            }
            if (!actualMsg.startsWith(m_patternPart1)) {
                return false;
            }
            if (!actualMsg.endsWith(m_patternPart3)) {
                return false;
            }
            String msgRegExpr =
                    actualMsg.substring(m_patternPart1.length(),
                            actualMsg.length() - m_patternPart3.length());
            Matcher matcher = m_regExpr.matcher(msgRegExpr);
            return matcher.matches();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return m_msg;
        }
    }

    private static class Option {
        private final int[] m_nodeIDs;

        private final String m_name;

        private final String m_value;

        private final String m_type;

        /**
         * Create new <code>Option</code>.
         *
         * @param nodeIDs node IDs, mostly one element, more for nested flows
         * @param name name
         * @param value value
         * @param type type
         */
        Option(final int[] nodeIDs, final String name, final String value,
                final String type) {
            m_nodeIDs = nodeIDs;
            m_name = name;
            m_value = value;
            m_type = type;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("Node#");
            boolean first = true;
            for (int id : m_nodeIDs) {
                if (!first) {
                    result.append(".");
                }
                result.append(id);
                first = false;
            }
            result.append(": ");
            result.append(m_name);
            result.append(" = ");
            result.append(m_value);
            return result.toString();
        }
    }

    /*
     * If this pattern occurs in a value of an option, it's replaced with the
     * directory of the options file. This only works with the old file-based
     * configuration since it is not really needed any more because we have drop
     * folders now.
     */
    private static final String LOCATION = "$$location$$";

    private static final String LOCATIONURL = "$$locationURL$$";

    private static final String OPTIONS_FILE = "workflow_options";

    private static final String STATUS_FILE = "node_status";

    /**
     * name of the file containing the testowner's email address.
     */
    private static final String OWNER_FILE = "owner";

    /**
     * The message inserted if the test fails (analyzers parse for it).
     */
    public static final String FAIL_MSG = " -> failing test!";

    /**
     * The message inserted if the test succeeded (analyzers parse for it).
     */
    public static final String SUCCESS_MSG = "Test succeeded.";

    private final List<String> m_owners = new ArrayList<String>();

    private final Collection<MsgPattern> m_requiredErrors =
            new ArrayList<MsgPattern>();

    private final Collection<MsgPattern> m_requiredWarnings =
            new ArrayList<MsgPattern>();

    private final Collection<MsgPattern> m_requiredInfos =
            new ArrayList<MsgPattern>();

    private final Collection<MsgPattern> m_requiredDebugs =
            new ArrayList<MsgPattern>();

    // node IDs are strings like "0:1:4" - but only the ID without prefix!
    private final Set<NodeID> m_requiredUnexecutedNodes = new HashSet<NodeID>();

    // file readers loaded in as executed nodes are expected to have a warning
    // (file <blah> not accessible anymore). This stores their ID (like "0:1:3")
    // without prefix
    private final Set<NodeID> m_preExecutedNodes = new HashSet<NodeID>();

    // maps node IDs (strings like "0:1:4" w/o prefix) to messages
    private final Map<NodeID, MsgPattern> m_warningStatus =
            new HashMap<NodeID, MsgPattern>();

    // maps node IDs (strings like "0:1:4" w/o prefix) to messages
    private final Map<NodeID, MsgPattern> m_errorStatus =
            new HashMap<NodeID, MsgPattern>();

    private final LinkedList<String> m_unexpectedErrors =
            new LinkedList<String>();

    private final LinkedList<String> m_exceptions = new LinkedList<String>();

    private final AppenderSkeleton m_customAppender = new AppenderSkeleton() {
        {
            LevelRangeFilter filter = new LevelRangeFilter();
            filter.setLevelMin(Level.DEBUG);
            filter.setLevelMax(Level.FATAL);
            addFilter(filter);
        }

        @Override
        public boolean requiresLayout() {
            return false;
        }

        @Override
        public void close() {
            m_exceptions.clear();
            m_unexpectedErrors.clear();
        }

        @Override
        protected void append(final LoggingEvent event) {
            FullWorkflowTest.this.appendLogMessage(event);
        }
    };

    /**
     * These guys own all tests without owner file. They will be notified about
     * their failure due to missing owners.
     */
    public final static String REGRESSIONS_OWNER = "peter.ohl@uni-konstanz.de";

    private static final NodeLogger logger = NodeLogger
            .getLogger(FullWorkflowTest.class);

    private static final int MAX_LINES = 100;

    /**
     * The maximum runtime for a single testcase in seconds. After the timeout
     * the workflow will be canceled.
     */
    public static final int TIMEOUT = 300;

    private File m_knimeWorkFlow;

    private WorkflowManager m_manager;

    // if not null, the executed workflow si saved here.
    private final File m_saveLoc;

    /**
     *
     * @param workflowFile the workflow dir
     * @param saveLoc the dir to save the flow into after execution, or null.
     */
    public FullWorkflowTest(final File workflowFile, final File saveLoc) {
        m_knimeWorkFlow = workflowFile;
        m_saveLoc = saveLoc;
        this.setName(workflowFile.getParentFile().getName());
    }

    private void appendLogMessage(final LoggingEvent aEvent) {
        String msg = aEvent.getRenderedMessage();
        Level msgLevel = aEvent.getLevel();

        // try recognizing stacktraces in warning messages.
        if ((msgLevel == Level.ERROR) || (msgLevel == Level.WARN)
                || (msgLevel == Level.FATAL)) {
            if (msg != null) {
                boolean exceptionStartLine = false;
                String lines[] = msg.replace("\r", "").split("\n");

                /*
                 * An Exception starts with a line containing "....Exception:
                 * ..." followed by a line starting with "<TAB>at
                 * ...blah...:<linenumber>)"
                 */
                for (String line : lines) {
                    if (line.indexOf("Exception: ") > 0) {
                        exceptionStartLine = true;
                    } else {
                        if (exceptionStartLine == true) {
                            // if the previous line started an exception dump
                            // this should be the first line of the stackstrace
                            if (line.matches("^\\tat .*\\(.*\\)$")) {
                                m_exceptions.add(lines[0]);
                                // make sure list won't get too long
                                if (m_exceptions.size() > MAX_LINES) {
                                    m_exceptions.removeFirst();
                                }
                            }
                        }
                        exceptionStartLine = false;
                    }
                }
            }
        }

        // first check if it is a required error/warning/info/debug message
        boolean isRequired = registerRequiredMessage(msg, msgLevel);

        if (!isRequired) {
            // an unintended error: add it to the list of unexpected errors
            if ((msgLevel == Level.ERROR) || (msgLevel == Level.FATAL)) {
                m_unexpectedErrors.add(msg);
                // make sure the list won't get too long
                if (m_unexpectedErrors.size() > MAX_LINES) {
                    m_unexpectedErrors.removeFirst();
                }
            }
        } else {
            String dbgmsg = "Required msg. ('" + msg + "')";
            logger.debug(dbgmsg);
        }
    }

    /**
     * Reads in each line of the options file, parses it, replaces $$location$$
     * in each value with the location of the options file and applies the
     * changes to the workflow settings. If the options file doesn't exist it
     * does nothing.
     *
     * @param optionsFile the file containing the lines with the settings to
     *            apply
     * @param wfm the workflow manager holding the workflow
     * @throws InvalidSettingsException if the options file contains incorrect
     *             settings
     * @throws FileNotFoundException if the options file does not exist nothing
     *             happens.
     * @throws IOException if an I/O Error occurs
     */
    private void applySettings(final File optionsFile)
            throws FileNotFoundException, InvalidSettingsException, IOException {
        if (!optionsFile.exists()) {
            return;
        }

        BufferedReader optfile =
                new BufferedReader(new FileReader(optionsFile));

        logger.debug("Applying new settings to workflow ----------------"
                + "------------------");

        String location = optionsFile.getParent();
        if (location == null) {
            throw new FileNotFoundException(
                    "Couldn't determine location of options file.");
        }
        location = location.replace('\\', '/');
        String locURL = new File(location).toURI().toString();
        if (locURL.endsWith("/")) {
            locURL = locURL.substring(0, locURL.length() - 1);
        }
        List<Option> options = new LinkedList<Option>();

        String line;
        while ((line = optfile.readLine()) != null) {
            try {
                String[] parts = line.split("\\,");
                String[] nodeIDPath = parts[0].split("/");
                int[] nodeIDs = new int[nodeIDPath.length];
                for (int i = 0; i < nodeIDs.length; i++) {
                    nodeIDs[i] = Integer.parseInt(nodeIDPath[i]);
                }
                String optionName = parts[1];
                String value = parts[2];
                String type = parts[3];

                // locationURL is part of location! Make sure to test it first!
                if (value.contains(LOCATIONURL)) {
                    value = value.replace(LOCATIONURL, locURL);
                } else if (value.contains(LOCATION)) {
                    value = value.replace(LOCATION, location);
                }
                options.add(new Option(nodeIDs, optionName, value, type));
            } catch (ArrayIndexOutOfBoundsException aobe) {
                throw new InvalidSettingsException("Invalid options line, "
                        + "in File '" + optionsFile + "', line: " + line);
            }
        }
        if (options.size() <= 0) {
            throw new InvalidSettingsException(
                    "Options file with no settings to change. "
                            + "Please delete the file or edit it (in the "
                            + "workflow dir file '" + optionsFile.getName()
                            + "').");
        }

        applySettingsModifications(options);
    }

    private void applySettingsModifications(final List<Option> options)
            throws InvalidSettingsException {
        for (Option o : options) {
            int[] idPath = o.m_nodeIDs;
            NodeID subID = new NodeID(m_manager.getID(), idPath[0]);
            NodeContainer cont = m_manager.getNodeContainer(subID);
            WorkflowManager subWM = m_manager;
            for (int i = 1; i < idPath.length; i++) {
                if (cont instanceof WorkflowManager) {
                    subWM = (WorkflowManager)cont;
                    subID = new NodeID(subID, idPath[i]);
                    cont = subWM.getNodeContainer(subID);
                } else {
                    cont = null;
                    break;
                }
            }
            if (cont == null) {
                logger.error("Can't modify settings: No node with id "
                        + Arrays.toString(idPath) + " found.");
            } else {
                NodeSettings settings = new NodeSettings("something");
                subWM.saveNodeSettings(cont.getID(), settings);
                NodeSettings model = settings.getNodeSettings(Node.CFG_MODEL);
                String[] splitName = o.m_name.split("/");
                String optName = splitName[splitName.length - 1];
                String[] pathElements = new String[splitName.length - 1];
                System.arraycopy(splitName, 0, pathElements, 0,
                        pathElements.length);
                for (String s : pathElements) {
                    model = model.getNodeSettings(s);
                }

                if ("int".equals(o.m_type)) {
                    model.addInt(optName, Integer.parseInt(o.m_value));
                } else if ("short".equals(o.m_type)) {
                    model.addShort(optName, Short.parseShort(o.m_value));
                } else if ("byte".equals(o.m_type)) {
                    model.addByte(optName, Byte.parseByte(o.m_value));
                } else if ("boolean".equals(o.m_type)) {
                    model.addBoolean(optName, Boolean.parseBoolean(o.m_value));
                } else if ("char".equals(o.m_type)) {
                    model.addChar(optName, o.m_value.charAt(0));
                } else if ("float".equals(o.m_type)
                        || ("double".equals(o.m_type))) {
                    model.addDouble(optName, Double.parseDouble(o.m_value));
                } else if ("String".equals(o.m_type)) {
                    model.addString(optName, o.m_value);
                } else if ("StringCell".equals(o.m_type)) {
                    model.addDataCell(optName, new StringCell(o.m_value));
                } else if ("DoubleCell".equals(o.m_type)) {
                    double d = Double.parseDouble(o.m_value);
                    model.addDataCell(optName, new DoubleCell(d));
                } else if ("IntCell".equals(o.m_type)) {
                    int i = Integer.parseInt(o.m_value);
                    model.addDataCell(optName, new IntCell(i));
                } else {
                    throw new IllegalArgumentException("Unknown option type '"
                            + o.m_type + "'");
                }
                subWM.loadNodeSettings(cont.getID(), settings);

                logger.info("Applied settings change: " + o);
            }
        }

    }

    /**
     * Causes the test to fail, if unexpected error messages or exceptions where
     * caught by the appender.
     */
    private void checkMessages() {
        StringBuilder buf = new StringBuilder();

        boolean unexpectedErrors = false;

        if (m_unexpectedErrors.size() > 0) {
            for (String msg : m_unexpectedErrors) {
                logger.info("Got error: " + msg);
                buf.append("\t * Got error: ").append(msg).append('\n');
            }
            logger.error("Unexpected error messages during test run.");
            unexpectedErrors = true;
        }
        if (m_exceptions.size() > 0) {
            for (String e : m_exceptions) {
                logger.info("Got exception: " + e);
                buf.append("\t * Got exception: ").append(e).append('\n');
            }
            logger.error("Exceptions during test run.");
            unexpectedErrors = true;
        }

        boolean missingMessages = false;

        if (m_requiredDebugs.size() > 0) {
            missingMessages = true;
            for (MsgPattern msg : m_requiredDebugs) {
                logger.info("Missing DEBUG msg: \"" + msg + "\"");
                buf.append("\t * Missing DEBUG msg: ").append(msg).append('\n');
            }
            logger.error("Missing required DEBUG messages in the test output");
        }
        if (m_requiredInfos.size() > 0) {
            missingMessages = true;
            for (MsgPattern msg : m_requiredInfos) {
                logger.info("Missing INFO msg: \"" + msg + "\"");
                buf.append("\t * Missing INFO msg: ").append(msg).append('\n');
            }
            logger.error("Missing required INFO messages in the test output");
        }
        if (m_requiredWarnings.size() > 0) {
            missingMessages = true;
            for (MsgPattern msg : m_requiredWarnings) {
                logger.info("Missing WARNING msg: \"" + msg + "\"");
                buf.append("\t * Missing WARNING msg: ").append(msg)
                        .append('\n');
            }
            logger.error("Missing required WARNING messages in the "
                    + "test output");
        }
        if (m_requiredErrors.size() > 0) {
            missingMessages = true;
            for (MsgPattern msg : m_requiredErrors) {
                logger.info("Missing ERROR msg: \"" + msg + "\"");
                buf.append("\t * Missing ERROR msg: ").append(msg).append('\n');
            }
            logger.error("Missing required ERROR messages in the test output");
        }

        // this method must cause the test to fail, as previous checks just
        // log error messages if something was fishy
        if (unexpectedErrors && missingMessages) {
            // that's the message log file analyzers pick up
            logger.fatal("Unexpected and missing messages" + FAIL_MSG);

            Assert.fail("Failing due to unexpected errors "
                    + "and missing messages in the log file.\n" + buf);
        }
        if (unexpectedErrors) {
            logger.fatal("Unexpected error messages" + FAIL_MSG);
            Assert.fail("Failing due to unexpected errors "
                    + "in the log file.\n" + buf);
        }
        if (missingMessages) {
            logger.fatal("Missing required messages" + FAIL_MSG);
            Assert.fail("Failing due to missing messages in the log file.\n"
                    + buf);
        }

        logger.info(SUCCESS_MSG);
    }

    /**
     * Checks if all nodes are executed - or if a config file existed, that
     * defines nodes that are not supposed to execute, it makes sure these are
     * not executed. If the check fails, it logs an error message and tells the
     * JUnit framework.
     *
     * @param manager the workflow manager holding the workflow to check.
     */
    private void checkNodeExecution(final WorkflowManager manager) {
        for (NodeContainer node : manager.getNodeContainers()) {
            State status = node.getState();

            if (!status.equals(State.EXECUTED)
                    && !m_requiredUnexecutedNodes.contains(node.getID())) {

                // not executed but supposed to be

                String msg =
                        "Node " + node.getNameWithID() + " is not executed.";
                if (node.getNodeMessage() != null) {
                    msg +=
                            " (node's status message: "
                                    + node.getNodeMessage().getMessageType()
                                    + ": " + node.getNodeMessage().getMessage()
                                    + ")";
                }
                // make sure to log an error - during wrapUp the test fails then
                logger.error(msg);
            } else if (status.equals(State.EXECUTED)
                    && m_requiredUnexecutedNodes.contains(node.getID())) {

                // executed but shouldn't be

                String msg =
                        "Execution of node " + node.getNameWithID()
                                + " should have failed (as specified in the "
                                + " testflow settings)";
                // make sure to log an error - during wrapUp the test fails then
                logger.error(msg);
            } else {

                // executed state as expected

                logger.debug("Node '" + node.getNameWithID() + "' is"
                        + (status.equals(State.EXECUTED) ? " " : " not ")
                        + "executed - which is good.");
            }
            if (node instanceof WorkflowManager) {
                checkNodeExecution((WorkflowManager)node);
            }
        }
    }

    /**
     * makes sure all nodes have a node status corresponding to the node status
     * file. If a node has an unexpected status or not the expected one, an
     * error is logged and the test fails.
     */
    private void checkNodeStatus(final WorkflowManager manager) {
        for (NodeContainer node : manager.getNodeContainers()) {
            NodeMessage status = node.getNodeMessage();
            if (status != null
                    && status.getMessageType().equals(NodeMessage.Type.ERROR)) {

                MsgPattern expMsg = m_errorStatus.get(node.getID());
                if (expMsg == null) {
                    // node was not expected to finish with an error status
                    if (!m_preExecutedNodes.contains(node.getID())) {
                        // complain only if the node was not loaded in executed
                        String msg =
                                "Node '" + node.getNameWithID() + "' has an "
                                        + "unexpected error status (message: "
                                        + status.getMessage() + ")";
                        // make sure to log an error
                        // during wrapUp the test fails then
                        logger.error(msg);
                    } else {
                        String msg =
                                "Node '" + node.getNameWithID() + "' has an "
                                        + "unexpected error status (message: "
                                        + status.getMessage()
                                        + ") - ignored as it was not "
                                        + "executed during the test";
                        logger.debug(msg);
                    }
                } else {
                    // make sure the error message is as expected.
                    if (!expMsg.matches(status.getMessage())) {
                        String msg =
                                "Node '" + node.getNameWithID()
                                        + "' has the expected status - but "
                                        + "the message is not the "
                                        + "expected one (node status msg: "
                                        + status.getMessage() + ")";
                        // make sure to log an error
                        // during wrapUp the test fails then
                        logger.error(msg);
                    } else {
                        Iterator<MsgPattern> iter = m_requiredErrors.iterator();
                        while (iter.hasNext()) {
                            MsgPattern p = iter.next();
                            if (p.matches(status.getMessage())) {
                                iter.remove();
                                break;
                            }
                        }

                        logger.debug("Node '" + node.getNameWithID()
                                + "' finished"
                                + " with an error status - which is good.");
                    }
                }

            } else if (status != null
                    && status.getMessageType().equals(NodeMessage.Type.WARNING)) {

                MsgPattern expMsg = m_warningStatus.get(node.getID());
                if (expMsg == null) {
                    // Node was not supposed to finish with a warning status
                    if (!m_preExecutedNodes.contains(node.getID())) {
                        // Complain only if node was not loaded in executed
                        String msg =
                                "Node '"
                                        + node.getNameWithID()
                                        + "' has an "
                                        + "unexpected warning status (message: "
                                        + status.getMessage() + ")";
                        // log an error. During wrapUp the test fails then
                        logger.error(msg);
                    } else {
                        String msg =
                                "Node '" + node.getNameWithID()
                                        + "' has an unexpected warning status."
                                        + " Ignored as it was not executed "
                                        + "during the test";
                        logger.debug(msg);
                    }
                } else {
                    // make sure the warning message is as expected.
                    if (!expMsg.matches(status.getMessage())) {
                        String msg =
                                "Node '"
                                        + node.getNameWithID()
                                        + "' has the"
                                        + " expected status - but the message is not the "
                                        + "expected one (node status msg: "
                                        + status.getMessage() + ")";
                        // make sure to log an error
                        // during wrapUp the test fails then
                        logger.error(msg);
                    } else {
                        Iterator<MsgPattern> iter = m_requiredWarnings.iterator();
                        while (iter.hasNext()) {
                            MsgPattern p = iter.next();
                            if (p.matches(status.getMessage())) {
                                iter.remove();
                                break;
                            }
                        }

                        logger.debug("Node '" + node.getNameWithID()
                                + "' finished"
                                + " with a warning status - which is good.");
                    }
                    // Pre-Executed nodes with warning status don't create
                    // log entries - except for reader nodes. Remove the
                    // message from the list of required messages!
                    if (m_preExecutedNodes.contains(node.getID())) {
                        String nodeWarnMsg = status.getMessage();
                        Iterator<MsgPattern> warns =
                                m_requiredWarnings.iterator();
                        while (warns.hasNext()) {
                            MsgPattern warn = warns.next();
                            if (warn.matches(nodeWarnMsg)) {
                                logger.debug("Removing warn message from required "
                                        + "warn messages (it is a pre-executed "
                                        + "node ("
                                        + node.getNameWithID()
                                        + ") with declared status) Msg:"
                                        + nodeWarnMsg);
                                warns.remove();
                                break;
                            }
                        }
                    }
                }

            } else {
                // no or unknown status

                MsgPattern expMsg = m_warningStatus.get(node.getID());
                if (expMsg != null) {
                    String msg =
                            "Node '"
                                    + node.getNameWithID()
                                    + "' is supposed to have a warning status, "
                                    + "but it has not. (Expected warning msg: "
                                    + expMsg + ")";
                    // make sure to log an error
                    // during wrapUp the test fails then
                    logger.error(msg);
                }

                expMsg = m_errorStatus.get(node.getID().toString());
                if (expMsg != null) {
                    String msg =
                            "Node '" + node.getNameWithID()
                                    + "' is supposed to have an error status, "
                                    + "but it has not. (Expected error msg: "
                                    + expMsg + ")";
                    // make sure to log an error
                    // during wrapUp the test fails then
                    logger.error(msg);
                }
            }
            if (node instanceof WorkflowManager) {
                checkNodeStatus((WorkflowManager)node);
            }
        }
    }

    private void dumpToLogError(final String s) {
        try {
            BufferedReader reader = new BufferedReader(new StringReader(s));
            String line;
            while ((line = reader.readLine()) != null) {
                logger.error(line);
            }
            reader.close();
        } catch (IOException ioe) {
            logger.debug("unable to log string", ioe);
        }
    }

    /**
     * From a line of the log file it extract the part that is the interesting
     * message. That is the part behind the first colon, dismissing the date,
     * time, level, and class info. It is important to extract the message that
     * was sent to the logger from the line.
     *
     * @param line to extract the message from
     * @return the interesting message part of the specified line, or null, if
     *         the line has an unexpected format.
     */
    private MsgPattern extractMessagePart(final String line) {

        String[] splits = line.split(" : ", 2);
        if (splits.length != 2) {
            return null;
        }
        return new MsgPattern(splits[1]);
    }

    private SingleNodeContainer findConfigNode() {
        for (NodeContainer cont : m_manager.getNodeContainers()) {
            if (cont instanceof SingleNodeContainer) {
                if (((SingleNodeContainer)cont).getNodeModel() instanceof TestConfigNodeModel) {
                    return (SingleNodeContainer)cont;
                }
            }
        }
        return null;
    }

    /**
     * Builds a node id using the current workflow manager as prefix and the given partial node id
     * as suffix.
     *
     * @param nodeId a node id relative to the current workflow manager
     * @return a complete node id including the current workflow manager's id
     */
    private NodeID createCompleteNodeID(final String nodeId) {
        NodeID id = m_manager.getID();
        for (String s : nodeId.split(":")) {
            id = new NodeID(id, Integer.parseInt(s));
        }
        return id;
    }

    /**
     * Parses one line from the status file assuming it specifies a required
     * error/warning/info or debug message.
     *
     * @param line the line to be parsed
     */
    private void parseMessageLine(final String line)
            throws InvalidSettingsException {

        Collection<MsgPattern> msgList = null;
        String infoMsg = "<unknownType>";
        if (line.toUpperCase().startsWith("ERROR")) {
            msgList = m_requiredErrors;
            infoMsg = "ERROR";
        } else if (line.toUpperCase().startsWith("WARN")) {
            msgList = m_requiredWarnings;
            infoMsg = "WARNING";
        } else if (line.toUpperCase().startsWith("INFO")) {
            msgList = m_requiredInfos;
            infoMsg = "INFO";
        } else if (line.toUpperCase().startsWith("DEBUG")) {
            msgList = m_requiredDebugs;
            infoMsg = "DEBUG";
        } else {
            // ignore this line
            return;
        }

        MsgPattern msg = extractMessagePart(line);
        if (msg == null) {
            throw new InvalidSettingsException("Invalid line in status file "
                    + "(missing ':'): " + line);
        }

        msgList.add(msg);
        logger.debug("Expecting " + infoMsg + " message during test: " + msg);
    }

    /**
     * Parses and applies one line from the status file that contains the
     * post-execute status of a node.
     *
     * @param line the line specifying the status of the node.
     * @throws InvalidSettingsException if the line is invalid
     */
    private void parseNodeStatusLine(final String line)
            throws InvalidSettingsException {

        String[] splits = line.split(",", 2);
        if (splits.length < 2) {
            throw new InvalidSettingsException("Invalid line in status file "
                    + "(specify node id and status):" + line);
        }

        NodeID nodeID = createCompleteNodeID(splits[0]);

        String msg = null;

        // see if we got a required error/warning status
        if (splits.length == 2) {
            String[] stats = splits[1].split(" ", 2);
            if (stats.length != 2) {
                throw new InvalidSettingsException(
                        "Invalid line in status file "
                                + "(invalid err/warn status): " + line);
            }
            MsgPattern statMsg = extractMessagePart(stats[1]);
            if (statMsg == null) {
                throw new InvalidSettingsException(
                        "Invalid line in status file "
                                + "(invalid err status msg): " + line);
            }

            if (stats[0].toUpperCase().startsWith("ERR")) {
                /*
                 * if a node is supposed to have an error status then it is
                 * supposed to fail during execution. Add the failure messages
                 * to the required messages, add the node id to the not-executed
                 * ids.
                 */
                // remember the nodes that must not execute
                if (m_requiredUnexecutedNodes.add(nodeID)) {
                    msg = "Node #" + nodeID + " must not be executed";
                }

                // now store the expected status message
                m_errorStatus.put(nodeID, statMsg);
                if (msg == null) {
                    msg = "Node #" + nodeID;
                }
                msg += " should have an error status '" + statMsg + "'";

                /*
                 * an error status also creates an error message in the log file
                 */
                // add message of the error status to the list of expected msgs
                m_requiredErrors.add(statMsg);
            } else if (stats[0].toUpperCase().startsWith("WARN")) {
                m_warningStatus.put(nodeID, statMsg);
                msg =
                        "Node #" + nodeID + " should have a warning status '"
                                + statMsg + "'";

                /*
                 * a warning status also creates a warn message in the log file
                 * lets make this required.
                 */
                // add message of the warn status to the list of expected msgs
                m_requiredWarnings.add(statMsg);
            } else {
                throw new InvalidSettingsException(
                        "Invalid line in status file "
                                + "(invalid err/warn status): " + line);
            }
        }

        logger.debug(msg);
    }

    private void readConfigFromFiles(final File workflowDir)
            throws IOException, InvalidSettingsException {
        readOwners(new File(workflowDir, OWNER_FILE));
        readNodeStatusFile(new File(workflowDir, STATUS_FILE));
    }

    private void readConfigFromNode(final SingleNodeContainer configNode)
            throws InvalidSettingsException {
        NodeSettings s = new NodeSettings("");
        configNode.getNode().saveSettingsTo(s);
        TestConfigSettings settings = new TestConfigSettings();
        settings.loadSettings(s.getNodeSettings(Node.CFG_MODEL));

        for (String owner : settings.owner().split(",")) {
            m_owners.add(owner);
        }

        for (String error : settings.requiredLogErrors()) {
            m_requiredErrors.add(new MsgPattern(error));
        }

        for (String warning : settings.requiredLogWarnings()) {
            m_requiredWarnings.add(new MsgPattern(warning));
        }

        for (String info : settings.requiredLogInfos()) {
            m_requiredInfos.add(new MsgPattern(info));
        }

        for (String id : settings.failingNodes()) {
            m_requiredUnexecutedNodes.add(createCompleteNodeID(id));
        }

        for (Map.Entry<String, String> e : settings.requiredNodeErrors()
                .entrySet()) {
            NodeID nodeId = createCompleteNodeID(e.getKey());
            try {
                m_manager.findNodeContainer(nodeId);
                MsgPattern msg = new MsgPattern(e.getValue());
                m_errorStatus.put(nodeId, msg);
                // error status on node also creates an error in the log
                m_requiredErrors.add(msg);
            } catch (IllegalArgumentException ex) {
                logger.warn("No node with id '" + e.getKey() + "' found in "
                        + "workflow, but we have a configuration for it. "
                        + "Ignoring the configuration.");
            }
        }

        for (Map.Entry<String, String> e : settings.requiredNodeWarnings()
                .entrySet()) {
            NodeID nodeId = createCompleteNodeID(e.getKey());
            try {
                m_manager.findNodeContainer(nodeId);
                MsgPattern msg = new MsgPattern(e.getValue());
                m_warningStatus.put(nodeId, msg);
                // warning status on node also creates a warning in the log
                m_requiredWarnings.add(msg);
            } catch (IllegalArgumentException ex) {
                logger.warn("No node with id '" + e.getKey() + "' found in "
                        + "workflow, but we have a configuration for it. "
                        + "Ignoring the configuration.");
            }
        }
    }

    /**
     * Reads in the file that contains the nodes that should show a certain
     * status after execution.
     *
     * @param statusFile the file containing the lines defining the node status
     *            after execution. If it doesn't exist, the method returns
     *            without error or without doing anything.
     * @throws FileNotFoundException if the file opening fails
     * @throws IOException if the file reading fails
     * @throws InvalidSettingsException if the settings in the file are
     *             incorrect
     */
    private void readNodeStatusFile(final File statusFile)
            throws FileNotFoundException, IOException, InvalidSettingsException {
        if (!statusFile.exists()) {
            return;
        }

        BufferedReader statusReader =
                new BufferedReader(new FileReader(statusFile));

        logger.debug("Reading configuration file for node status / messages");
        String line;
        while ((line = statusReader.readLine()) != null) {

            if (line.isEmpty()) {
                // ignore empty lines
                continue;
            }
            if ((line.toUpperCase().startsWith("ERROR"))
                    || (line.toUpperCase().startsWith("WARN"))
                    || (line.toUpperCase().startsWith("INFO"))
                    || (line.toUpperCase().startsWith("DEBUG"))) {
                // line specifies certain message that should appear in the log
                parseMessageLine(line);
            } else {
                // line specifies a node status plus message
                parseNodeStatusLine(line);
            }

        }

        statusReader.close();
    }

    /**
     * Reads the owners of the test from the specified owner file. A
     * non-existing file is ignored.
     *
     * @param ownerFile the file containing the owners of the test case. Each
     *            line of the file must contain one email address.
     * @throws IOException if an I/O error occurrs
     */
    private void readOwners(final File ownerFile) throws IOException {
        if (ownerFile.exists()) {
            BufferedReader r = new BufferedReader(new FileReader(ownerFile));
            String line = null;
            while ((line = r.readLine()) != null) {
                if (line.trim().length() > 0) {
                    m_owners.add(line.trim());
                }
            }
            r.close();
        }
    }

    /**
     * Searches the nodes of the WFM for executed nodes. Their warning status
     * will later on not cause the test case to fail then. That is to make test
     * creation (or my life) easier. The status of these nodes doesn't appear in
     * the log file - but if specified in the node_status file it will become a
     * required message. (Except for the reader nodes that re-create the status
     * each time they are loaded...). Also, reader always create warnings in a
     * test (because the test file read doesn't exists after the test gets
     * checked in) - and the test creators will always forget to specify their
     * warning status.
     *
     * @param wfm the workflow to examine
     */
    private void registerExecutedNodes(final WorkflowManager manager) {
        for (NodeContainer nc : manager.getNodeContainers()) {
            if (nc instanceof WorkflowManager) {
                registerExecutedNodes((WorkflowManager)nc);
            }

            if (!nc.getState().equals(State.EXECUTED)) {
                continue;
            }

            if (nc.getNodeMessage() == null) {
                continue;
            }
            String status = nc.getNodeMessage().getMessageType().name();
            m_preExecutedNodes.add(nc.getID());
            logger.debug("Registering node " + nc.getNameWithID()
                    + " as pre-executed with a " + status + " status");
        }
    }

    /**
     * Tests if the passed msg from the logger is a required message, i.e. was
     * specified in the node status file. It also remembers that this message
     * appeared (for the checkMessages method at the end).
     *
     * @param msg the message passed to the logger
     * @param msgLevel msg level
     * @return true, if the msg is a required message
     */
    private boolean registerRequiredMessage(final String msg,
            final Level msgLevel) {
        Collection<MsgPattern> msgList = null;
        if (msg != null) {
            if (msgLevel == Level.ERROR) {
                msgList = m_requiredErrors;
            } else if (msgLevel == Level.WARN) {
                msgList = m_requiredWarnings;
            } else if (msgLevel == Level.INFO) {
                msgList = m_requiredInfos;
            } else if (msgLevel == Level.DEBUG) {
                msgList = m_requiredDebugs;
            }
        }

        // now check if its contained the corresponding list
        boolean expected = false;
        if (msgList != null) {
            Iterator<MsgPattern> iter = msgList.iterator();
            while (iter.hasNext()) {
                MsgPattern p = iter.next();
                if (p.matches(msg)) {
                    iter.remove();
                    expected = true;
                    break;
                }
            }
        }
        return expected;
    }

    @Override
    public void runTest() {
        final Set<AbstractNodeView<? extends NodeModel>> allViews =
                new HashSet<AbstractNodeView<? extends NodeModel>>();

        /*
         * disabling node views to workaround problems with
         * SyntheticImageGenerator:
         * http://bugs.sun.com/bugdatabase/view_bug.do;jsessionid
         * =d544ef8157d74565d9f7aee881430?bug_id=6967484
         * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6419354
         *
         * See also KNIME bug 2562: Enable NodeViews in test cases (disabled due
         * to java bug in SyntheticImageGenerator)
         */
        for (NodeContainer nodeCont : m_manager.getNodeContainers()) {
            for (int i = 0; i < nodeCont.getNrViews(); i++) {
                logger.debug("opening view nr. " + i + " for node "
                        + nodeCont.getName());
                final AbstractNodeView<? extends NodeModel> view =
                        nodeCont.getView(i);
                final int index = i;
                // store the view in order to close is after the test finishes
                allViews.add(view);
                // open it now.
                ViewUtils.invokeAndWaitInEDT(new Runnable() {
                    /** {@inheritDoc} */
                    @Override
                    public void run() {
                        Node.invokeOpenView(view, "View #" + index);
                    }
                });
            }
        }
        TimerTask timeout = new TimerTask() {
            @Override
            public void run() {
                logger.error("Workflow is running longer than " + TIMEOUT
                        + " seconds, dumping status, followed by cancel:");
                String status =
                        m_manager.printNodeSummary(m_manager.getID(), 0);
                logger.error("------- Status before Cancel (Start) ----------");
                dumpToLogError(status);
                logger.error("------- Status before Cancel (End) ------------");
                logger.error("Now calling cancel on workflow parent...");
                try {
                    m_manager.getParent().cancelExecution(m_manager);
                } catch (Exception e) {
                    logger.error("Failed to cancel workflow, giving up", e);
                    return;
                }
                logger.error("Workflow canceled after " + TIMEOUT
                        + " seconds, status after cancel follows: ");
                status = m_manager.printNodeSummary(m_manager.getID(), 0);
                logger.error("------- Status after Cancel (Start) ----------");
                dumpToLogError(status);
                logger.error("------- Status after Cancel (End) ------------");
            }
        };
        try {
            try {
                KNIMETimer.getInstance().schedule(timeout, TIMEOUT * 1000);

                // execute all nodes.
                logger.info("Executing workflow ----------------------");

                m_manager.executeAllAndWaitUntilDone();

                timeout.cancel();
            } catch (Throwable t) {
                String msg = t.getMessage();
                logger.error("Caught a " + t.getClass().getSimpleName()
                        + " during workflow execution:"
                        + (msg == null ? "<no details>" : msg), t);
                throw t;
            }
            try {
                // evaluate the results
                logger.info("Analyzing executed workflow ----------------------");
                /*
                 * 1) make sure all nodes are executed (or nodes not supposed to
                 * execute are not executed).
                 */
                checkNodeExecution(m_manager);

                /*
                 * 2) check the status (warning and/or error) of the nodes.
                 */
                checkNodeStatus(m_manager);

                /*
                 * the above checks only write errors into the log file - thus
                 * the next step decides whether the test fails or succeeds:
                 *
                 * 3) make sure all expected/required messages appeared and no
                 * unexpected error message showed up. (We do that always - thus
                 * we let it fall through finally.)
                 */
            } catch (Throwable t) {
                String msg = t.getMessage();
                logger.error("Caught a " + t.getClass().getSimpleName()
                        + " during workflow analysis:"
                        + (msg == null ? "<no details>" : msg), t);
                throw t;
            }
        } catch (Throwable t) {
            // message was printed already
        } finally {
            timeout.cancel();

            // always close these views.
            for (AbstractNodeView<? extends NodeModel> v : allViews) {
                Node.invokeCloseView(v);
            }

            wrapUp();
        }
    }

    private void saveWorkflow() {
        logger.info("Saving workflow ------------------------------------");
        boolean doSave = true;
        if (!m_saveLoc.exists()) {
            if (!m_saveLoc.mkdirs()) {
                logger.info("Unable to create dir :" + m_saveLoc
                        + ". Not saving executed workflow.");
                doSave = false;
            }
        } else if (!m_saveLoc.isDirectory()) {
            logger.info("Can only save to a directory. This is none :"
                    + m_saveLoc + ". Not saving executed workflow.");
            doSave = false;
        }
        if (doSave) {
            try {
                m_manager.save(m_saveLoc, new ExecutionMonitor(), true);
                logger.info("Saved workflow to " + m_saveLoc.getAbsolutePath());
                logger.info("Done saving workflow ---------------------------");
            } catch (Throwable t) {
                logger.error("Caught throwable during workflow saving!");
                logger.info(t.getMessage(), t);
                logger.info("No success saving workflow ---------------------");
            }
        } else {
            logger.info("Didn't save workflow -------------------------------");
        }
    }

    /**
     * Starts the workflow with the given settings file.
     *
     * @throws IOException
     * @throws CanceledExecutionException
     * @throws InvalidSettingsException
     *
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    public void setUp() throws InvalidSettingsException,
            CanceledExecutionException, IOException {

        logger.info("<Start> Test='"
                + m_knimeWorkFlow.getParentFile().getName()
                + "' --------------------------------------------------------");
        logger.debug("Workflow location: " + m_knimeWorkFlow.getParent()
                + " -------------------------");
        logger.debug("Loading workflow ----------------------------"
                + "--------------");

        Logger.getRootLogger().addAppender(m_customAppender);
        try {
            WorkflowLoadResult loadRes =
                    WorkflowManager
                            .loadProject(m_knimeWorkFlow.getParentFile(),
                                    new ExecutionMonitor(),
                                    WorkflowLoadHelper.INSTANCE);
            boolean mustReportErrors;
            switch (loadRes.getType()) {
                case Ok:
                case Warning:
                    mustReportErrors = false;
                    break;
                case DataLoadError:
                    mustReportErrors = loadRes.getGUIMustReportDataLoadErrors();
                    break;
                default:
                    mustReportErrors = true;
            }
            if (mustReportErrors) {
                logger.error(loadRes.getFilteredError("",
                        LoadResultEntryType.Warning));
            }
            m_manager = loadRes.getWorkflowManager();
            logger.debug("Workflow loaded ----------------------------"
                    + "--------------");

            SingleNodeContainer configNode = findConfigNode();
            if (configNode != null) {
                readConfigFromNode(configNode);
            } else {
                readConfigFromFiles(m_knimeWorkFlow.getParentFile());
            }

            // be sure to always add an owner to the log file
            if ((m_owners != null) && (m_owners.size() > 0)) {
                String s = m_owners.toString();
                logger.info("TestOwners=" + s.substring(1, s.length() - 1));
            } else {
                logger.info("TestOwners=" + REGRESSIONS_OWNER);
                String msg = "No owner set in test '"
                        + m_knimeWorkFlow.getParentFile().getName()
                        + "'. Please create an owner file in the test directory"
                        + " or add a Testflow Configuration node to the "
                        + "workflow.";
                logger.error(msg);
                wrapUp();
                fail(msg);
            }

            // remember the executed nodes (before executing) to not
            // complain about their warning status later.
            registerExecutedNodes(m_manager);

            // construct a list of options (i.e. settings to change in the flow)
            File optionsFile =
                    new File(m_knimeWorkFlow.getParentFile(), OPTIONS_FILE);
            applySettings(optionsFile);
        } catch (AssertionError err) {
            throw err;
        } catch (IOException ex) {
            String msg = ex.getMessage();
            logger.error("I/O Error during workflow loading:"
                    + (msg == null ? "<no details>" : msg));
            wrapUp();
            fail(msg);
        } catch (InvalidSettingsException ise) {
            String msg = ise.getMessage();
            logger.error("Invalid settings: "
                    + (msg == null ? "<no details>" : msg));
            wrapUp();
            fail(msg);
        } catch (Throwable t) {
            String msg = t.getMessage();
            logger.error("Caught a throwable during test setup: "
                    + t.getClass().getSimpleName() + ", msg: "
                    + (msg == null ? "<no details>" : msg), t);
            wrapUp();
            fail(msg);
        }
    }

    @Override
    @After
    public void tearDown() throws Exception {
        m_manager.shutdown();
        m_manager.getParent().removeNode(m_manager.getID());
        // throw the workflow away so that the GC can do its work
        m_manager = null;
    }

    /**
     * wraps up execution and lets the test fail if (unexpected) error messages
     * occurred.
     */
    private void wrapUp() {
        if (m_saveLoc != null) {
            saveWorkflow();
        }
        try {
            // disconnect the appender to not catch any message anymore
            Logger.getRootLogger().removeAppender(m_customAppender);
            logger.info("Result ---------------------------------------");
            checkMessages();
        } finally {
            m_customAppender.close();
            logger.info("<End> Test='"
                    + m_knimeWorkFlow.getParentFile().getName()
                    + "' ----------------------------------------------------");
        }
    }
}
