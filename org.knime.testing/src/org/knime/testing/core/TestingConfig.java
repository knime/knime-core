/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   21.12.2006 (ohl): created
 */
package org.knime.testing.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.varia.LevelRangeFilter;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeStatus;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Holds the config of a test case. That is, the settings that should be applied
 * before execution, the expected/required messages, and the list of nodes not
 * to be executed. It is also registered as an Appender with the logger and
 * listens to all messages logged by the test case. It provides methods to check
 * the workflow against these settings and to decide whether it was run
 * successfully or not.
 * 
 * @author ohl, University of Konstanz
 */
public class TestingConfig extends AppenderSkeleton {
    /*
     * if this pattern occurs in a value of an option, it's replaced with the
     * directory of the options file
     */
    private static final String LOCATION = "$$location$$";

    private static final String LOCATIONURL = "$$locationURL$$";

    /**
     * The message inserted if the test fails due to error messages (analyzers
     * will parse for it).
     */
    public static final String ERR_FAIL_MSG = "Got ERRORs during run";

    /**
     * The message inserted if the test fails due to exceptions (analyzers will
     * parse for it).
     */
    public static final String EXCEPT_FAIL_MSG = "Got EXCEPTIONs during run";

    private List<String> m_owners;

    private Collection<String> m_requiredErrors;

    private Collection<String> m_requiredWarnings;

    private Collection<String> m_requiredInfos;

    private Collection<String> m_requiredDebugs;

    private Collection<Integer> m_requiredUnexecutedNodes;

    private Map<Integer, String> m_warningStatus;

    private Map<Integer, String> m_errorStatus;

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(TestingConfig.class);

    private int m_maxlines;

    private LinkedList<String> m_unexpectedErrors;

    private LinkedList<String> m_exceptions;

    /**
     * Constructor. Adds itself as appender to the root logger.
     * 
     * @param maxLines the last maxLines messages will be stored.
     */
    public TestingConfig(final int maxLines) {

        m_requiredErrors = new LinkedList<String>();
        m_requiredWarnings = new LinkedList<String>();
        m_requiredInfos = new LinkedList<String>();
        m_requiredDebugs = new LinkedList<String>();
        m_requiredUnexecutedNodes = new LinkedList<Integer>();
        m_errorStatus = new HashMap<Integer, String>();
        m_warningStatus = new HashMap<Integer, String>();
        m_owners = null;

        m_maxlines = maxLines;
        m_unexpectedErrors = new LinkedList<String>();
        m_exceptions = new LinkedList<String>();

        Logger root = Logger.getRootLogger();
        LevelRangeFilter filter = new LevelRangeFilter();
        filter.setLevelMin(Level.DEBUG);
        filter.setLevelMax(Level.FATAL);
        this.addFilter(filter);
        root.addAppender(this);
    }

    /**
     * Reads the owners of the test from the specified owner file.
     * 
     * @param ownerFile the file containing the owners of the test case. Each
     *            line of the file must contain one email address.
     * @return the number of owners read (lines in the file)
     * @throws IOException if an I/O Error occurred.
     */
    public int setOwners(final File ownerFile) throws IOException {
        int count = 0;
        if (ownerFile.exists()) {
            m_owners = new LinkedList<String>();
            FileReader fileR = new FileReader(ownerFile);
            BufferedReader r = new BufferedReader(fileR);
            String line = null;
            while ((line = r.readLine()) != null) {
                if (line.trim().length() > 0) {
                    count++;
                    m_owners.add(line.trim());
                }
            }
        }
        return count;
    }

    /**
     * Returns the comma separated list of owners.
     * 
     * @return the comma separated list of owners of this test case, or null if
     *         no owner is set.
     */
    public String getOwners() {
        if (m_owners == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (String o : m_owners) {
            if (!first) {
                result.append(";");
            }
            first = false;
            result.append(o);
        }
        return result.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void append(final LoggingEvent aEvent) {

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
                 * ..." followed by a line starting with "<TAB>at ...blah...:<linenumber>)"
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
                                if (m_exceptions.size() > m_maxlines) {
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
                if (m_unexpectedErrors.size() > m_maxlines) {
                    m_unexpectedErrors.removeFirst();
                }
            }
        }
    }

    /**
     * Disconnects this appender from the root logger.
     */
    public void disconnect() {
        Category.getRoot().removeAppender(this);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        m_exceptions.clear();
        m_unexpectedErrors.clear();
    }

    /**
     * does not require layout.
     * 
     * @see org.apache.log4j.AppenderSkeleton#requiresLayout()
     */
    @Override
    public boolean requiresLayout() {
        return false;
    }

    /**
     * reads in each line of the options file, parses it, replaces $$location$$
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
    public void applySettings(final File optionsFile, final WorkflowManager wfm)
            throws FileNotFoundException, InvalidSettingsException, IOException {

        if (!optionsFile.exists()) {
            return;
        }

        BufferedReader optfile =
                new BufferedReader(new FileReader(optionsFile));

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

        applySettingsModifications(options, wfm);

    }

    private void applySettingsModifications(final List<Option> options,
            final WorkflowManager wfm) throws InvalidSettingsException {

        for (Option o : options) {
            int[] idPath = o.m_nodeIDs;
            NodeContainer cont = wfm.getNodeContainerById(idPath[0]);
            for (int i = 1; i < idPath.length; i++) {
                cont =
                        cont.getEmbeddedWorkflowManager().getNodeContainerById(
                                idPath[i]);
            }
            if (cont == null) {
                LOGGER.error("Can't modify settings: No node with id "
                        + Arrays.toString(idPath) + " found.");
            } else {
                NodeSettings settings = new NodeSettings("something");
                cont.saveSettings(settings);
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
                cont.loadSettings(settings);

                LOGGER.info("Applied settings change: " + o);
            }
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

    /**
     * Reads in the file that contains the nodes that should show a certain
     * status after execution.
     * 
     * @param statusFile the file containing the lines defining the node status
     *            after execution. If it doesn't exist, the method returns
     *            without error or without doing anything.
     * @param wfm manager holding the flow whose nodes are specified in the file
     * @throws FileNotFoundException if the file opening fails
     * @throws IOException if the file reading fails
     * @throws InvalidSettingsException if the settings in the file are
     *             incorrect
     */
    public void readNodeStatusFile(final File statusFile,
            final WorkflowManager wfm) throws FileNotFoundException,
            IOException, InvalidSettingsException {

        if (!statusFile.exists()) {
            return;
        }

        BufferedReader statusReader =
                new BufferedReader(new FileReader(statusFile));

        String line;
        while ((line = statusReader.readLine()) != null) {

            if ((line.toUpperCase().startsWith("ERROR"))
                    || (line.toUpperCase().startsWith("WARNING"))
                    || (line.toUpperCase().startsWith("INFO"))
                    || (line.toUpperCase().startsWith("DEBUG"))) {
                parseMessageLine(line);
            } else {
                parseNodeStatusLine(line, wfm);
            }

        }

        statusReader.close();

    }

    /**
     * Parses one line from the status file assuming it specifies a required
     * error/warning/info or debug message.
     * 
     * @param line the line to be parsed
     */
    private void parseMessageLine(final String line)
            throws InvalidSettingsException {

        Collection<String> msgList = null;
        if (line.toUpperCase().startsWith("ERROR")) {
            msgList = m_requiredErrors;
        } else if (line.toUpperCase().startsWith("WARNING")) {
            msgList = m_requiredWarnings;
        } else if (line.toUpperCase().startsWith("INFO")) {
            msgList = m_requiredInfos;
        } else if (line.toUpperCase().startsWith("DEBUG")) {
            msgList = m_requiredDebugs;
        } else {
            // call this method only in one of these four cases
            assert false;
        }

        String msg = extractMessagePart(line);
        if (msg == null) {
            throw new InvalidSettingsException("Invalid line in status file "
                    + "(Missing ':'):" + line);
        }

        msgList.add(msg);

    }

    /**
     * From a line of the node status file or a log file it extract the part
     * that is the interesting message. That is the part behind the first colon,
     * dismissing the date, time, level, and class info. It is important to
     * extract the message that was sent to the logger from the line.
     * 
     * @param line to extract the message from
     * @return the interesting message part of the specified line, or null, if
     *         the line has an unexpected format.
     */
    private String extractMessagePart(final String line) {

        String[] splits = line.split(" : ", 2);
        if (splits.length != 2) {
            return null;
        }
        return splits[1];
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
        Collection<String> msgList = null;
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
            if (msgList.contains(msg)) {
                // remove the expected msg - at the end we test, if
                // required messages did not appear (are still in this list)
                // and fail if so.
                msgList.remove(msg);
                expected = true;
            }
        }
        return expected;
    }

    /**
     * Parses and applies one line from the status file that contains the
     * post-execute status of a node.
     * 
     * @param line the line specifying the status of the node.
     * @param wfm the flow the line refers to. NodeID are checked against this.
     * @throws InvalidSettingsException if the line is invalid
     */
    private void parseNodeStatusLine(final String line,
            final WorkflowManager wfm) throws InvalidSettingsException {

        String[] splits = line.split(",", 3);
        // make sure we have at least a node id and exec status (0/1)
        if (splits.length < 2) {
            throw new InvalidSettingsException("Invalid line in status file "
                    + "(specify exec status {0|1}):" + line);
        }
        if (!"1".equals(splits[1]) && !"0".equals(splits[1])) {
            throw new InvalidSettingsException("Invalid line in status file "
                    + "(invalid exec status {0|1}):" + line);
        }

        // try parsing the node id
        Integer nodeID = null;
        try {
            nodeID = Integer.parseInt(splits[0]);
        } catch (NumberFormatException nfe) {
            throw new InvalidSettingsException(
                    "Invalid line in status file (invalid node ID): " + line);
        }
        if (wfm.getNodeContainerById(nodeID) == null) {
            throw new InvalidSettingsException(
                    "Invalid line in status file (not existing node ID): "
                            + line);
        }

        boolean mustExec = splits[1].equals("1");
        if (!mustExec) {
            // remember the nodes that must not execute
            m_requiredUnexecutedNodes.add(nodeID);
        }

        // see if we got a required error/warning status
        if (splits.length == 3) {
            String[] stats = splits[2].split(":", 2);
            if (stats.length != 2) {
                throw new InvalidSettingsException(
                        "Invalid line in status file "
                                + "(invalid err/warn status): " + line);
            }

            if (stats[0].toUpperCase().startsWith("ERR")) {
                m_errorStatus.put(nodeID, stats[1]);
            } else if (stats[0].toUpperCase().startsWith("WARN")) {
                m_warningStatus.put(nodeID, stats[1]);
            } else {
                throw new InvalidSettingsException(
                        "Invalid line in status file "
                                + "(invalid err/warn status): " + line);
            }
        }

    }

    /**
     * Checks if all nodes are executed - or if a config file existed, that
     * defines nodes that are not supposed to execute, it makes sure these are
     * not executed. If the check fails, it logs an error message and tells the
     * JUnit framework.
     * 
     * @param manager the workflow manager holding the workflow to check.
     */
    public void checkNodeExecution(final WorkflowManager manager) {
        Collection<NodeContainer> nodes = manager.getNodes();

        for (NodeContainer node : nodes) {

            NodeStatus status = node.getStatus();

            if (!node.isExecuted()
                    && !m_requiredUnexecutedNodes.contains(node.getID())) {

                // not executed but supposed to be

                String msg = "Node " + node.getName() + " is not executed.";
                if (status != null) {
                    msg += " (status message: " + status.getMessage() + ")";
                }
                // make sure to log an error - during wrapUp the test fails then
                LOGGER.error(msg);
            } else if (node.isExecuted()
                    && m_requiredUnexecutedNodes.contains(node.getID())) {

                // executed but shouldn't be

                String msg =
                        "Node " + node.getName()
                                + " is executed eventhough it shouldn't. "
                                + "(as specified in the node status file)";
                // make sure to log an error - during wrapUp the test fails then
                LOGGER.error(msg);
            } else {

                // executed state as expected

                LOGGER.debug("Node '" + node.getName() + "' is"
                        + (node.isExecuted() ? " " : " not ")
                        + "executed - which is good.");
            }

        }
    }

    /**
     * makes sure all nodes have a node status corresponding to the node status
     * file. If a node has an unexpected status or not the expected one, an
     * error is logged and the test fails.
     * 
     * @param wfm the manager holding the flow to check.
     */
    public void checkNodeStatus(final WorkflowManager wfm) {
        Collection<NodeContainer> nodes = wfm.getNodes();

        for (NodeContainer node : nodes) {

            NodeStatus status = node.getStatus();

            if (status instanceof NodeStatus.Error) {

                String expMsg = m_errorStatus.get(node.getID());
                if (expMsg == null) {
                    // node was not expected to finish with an error status
                    String msg =
                            "Node '" + node.getName() + "' has an "
                                    + "unexpected error status (message: "
                                    + status.getMessage() + ")";
                    // make sure to log an error
                    // during wrapUp the test fails then
                    LOGGER.error(msg);
                } else {
                    // make sure the error message is as expected.
                    if (!expMsg.equals(status.getMessage())) {
                        String msg =
                                "Node '"
                                        + node.getName()
                                        + "' has the"
                                        + " expected status - but the message is not the "
                                        + "expected one (node status msg: "
                                        + status.getMessage() + ")";
                        // make sure to log an error
                        // during wrapUp the test fails then
                        LOGGER.error(msg);
                    } else {
                        LOGGER.debug("Node '" + node.getName() + "' finished"
                                + " with an error status - which is good.");
                    }
                }

            } else if (status instanceof NodeStatus.Warning) {

                String expMsg = m_warningStatus.get(node.getID());
                if (expMsg == null) {
                    // node was not expected to finish with a warning status
                    String msg =
                            "Node '" + node.getName() + "' has an "
                                    + "unexpected warning status (message: "
                                    + status.getMessage() + ")";
                    // make sure to log an error
                    // during wrapUp the test fails then
                    LOGGER.error(msg);
                } else {
                    // make sure the warning message is as expected.
                    if (!expMsg.equals(status.getMessage())) {
                        String msg =
                                "Node '"
                                        + node.getName()
                                        + "' has the"
                                        + " expected status - but the message is not the "
                                        + "expected one (node status msg: "
                                        + status.getMessage() + ")";
                        // make sure to log an error
                        // during wrapUp the test fails then
                        LOGGER.error(msg);
                    } else {
                        LOGGER.debug("Node '" + node.getName() + "' finished"
                                + " with a warning status - which is good.");
                    }
                }

            } else {
                // no or unknown status

                String expMsg = m_warningStatus.get(node.getID());
                if (expMsg != null) {
                    String msg =
                            "Node '"
                                    + node.getName()
                                    + "' is supposed to have a warning status, "
                                    + "but it has not. (Expected warning msg: "
                                    + expMsg + ")";
                    // make sure to log an error
                    // during wrapUp the test fails then
                    LOGGER.error(msg);
                }

                expMsg = m_errorStatus.get(node.getID());
                if (expMsg != null) {
                    String msg =
                            "Node '" + node.getName()
                                    + "' is supposed to have an error status, "
                                    + "but it has not. (Expected error msg: "
                                    + expMsg + ")";
                    // make sure to log an error
                    // during wrapUp the test fails then
                    LOGGER.error(msg);
                }

            }

        }

    }

    /**
     * Causes the test to fail, if unexpected error messages or exceptions where
     * caught by the appender.
     */
    public void checkMessages() {

        boolean unexpectedErrors = false;

        if (m_unexpectedErrors.size() > 0) {
            Collection<String> errMsgs = m_unexpectedErrors;
            for (String msg : errMsgs) {
                LOGGER.error("Got error: " + msg);
            }
            LOGGER.error(ERR_FAIL_MSG + " -> FAILING! Check the log file.");
            unexpectedErrors = true;
        }
        if (m_exceptions.size() > 0) {
            Collection<String> excMsgs = m_exceptions;
            for (String e : excMsgs) {
                LOGGER.error("Got exception: " + e);
            }
            LOGGER.error(EXCEPT_FAIL_MSG + " -> FAILING! "
                    + "Check the log file.");
            unexpectedErrors = true;
        }

        boolean missingMessages = false;

        if (m_requiredDebugs.size() > 0) {
            missingMessages = true;
            LOGGER.error("Missing required DEBUG messages!");
            LOGGER.info("Check node status file in workflow directory");
            for (String msg : m_requiredDebugs) {
                LOGGER.info("Missing DEBUG msg: " + msg);
            }
        }
        if (m_requiredInfos.size() > 0) {
            missingMessages = true;
            LOGGER.error("Missing required INFO messages!");
            LOGGER.info("Check node status file in workflow directory");
            for (String msg : m_requiredInfos) {
                LOGGER.info("Missing INFO msg: " + msg);
            }
        }
        if (m_requiredWarnings.size() > 0) {
            missingMessages = true;
            LOGGER.error("Missing required WARNING messages!");
            LOGGER.info("Check node status file in workflow directory");
            for (String msg : m_requiredWarnings) {
                LOGGER.info("Missing WARNING msg: " + msg);
            }
        }
        if (m_requiredErrors.size() > 0) {
            missingMessages = true;
            LOGGER.error("Missing required ERROR messages!");
            LOGGER.info("Check node status file in workflow directory");
            for (String msg : m_requiredErrors) {
                LOGGER.info("Missing ERROR msg: " + msg);
            }
        }

        // this method must cause the test to fail, as previous checks just
        // log error messages if something was fishy
        if (unexpectedErrors && missingMessages) {
            Assert.fail("Failing due to unexpected errors "
                    + "and missing messages in the log file.");
        }
        if (unexpectedErrors) {
            Assert.fail("Failing due to unexpected errors "
                    + "in the log file.");
        }
        if (missingMessages) {
            Assert.fail("Failing due to missing messages in the log file.");
        }

    }
}
