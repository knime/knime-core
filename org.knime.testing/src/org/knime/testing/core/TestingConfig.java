/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.NodeContainer.State;

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
     * The message inserted if the test fails (analyzers parse for it).
     */
    public static final String FAIL_MSG = " -> failing test!";

    /**
     * The message inserted if the test succeeded (analyzers parse for it).
     */
    public static final String SUCCESS_MSG = "Test succeeded.";

    private List<String> m_owners;

    private Collection<MsgPattern> m_requiredErrors;

    private Collection<MsgPattern> m_requiredWarnings;

    private Collection<MsgPattern> m_requiredInfos;

    private Collection<MsgPattern> m_requiredDebugs;

    // node IDs are strings like "0:1:4" - but only the ID without prefix!
    private Set<String> m_requiredUnexecutedNodes;

    // file readers loaded in as executed nodes are expected to have a warning
    // (file <blah> not accessible anymore). This store their ID (like "0:1:3")
    // without prefix
    private Set<String> m_executedFileReaders;

    // used to identify the file reader node by name
    private static final String FILEREADER_NAME = "File Reader";

    // the test case we get the messages from
    private final KnimeTestCase m_testCase;

    // maps node IDs (strings like "0:1:4" w/o prefix) to messages
    private Map<String, MsgPattern> m_warningStatus;

    // maps node IDs (strings like "0:1:4" w/o prefix) to messages
    private Map<String, MsgPattern> m_errorStatus;

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(TestingConfig.class);

    private int m_maxlines;

    private LinkedList<String> m_unexpectedErrors;

    private LinkedList<String> m_exceptions;

    /**
     * Constructor. Adds itself as appender to the root logger.
     *
     * @param test the test case we get messages from.
     * @param maxLines the last maxLines messages will be stored.
     */
    public TestingConfig(final KnimeTestCase test, final int maxLines) {

        m_testCase = test;

        m_requiredErrors = new LinkedList<MsgPattern>();
        m_requiredWarnings = new LinkedList<MsgPattern>();
        m_requiredInfos = new LinkedList<MsgPattern>();
        m_requiredDebugs = new LinkedList<MsgPattern>();
        m_requiredUnexecutedNodes = new HashSet<String>();
        m_errorStatus = new HashMap<String, MsgPattern>();
        m_warningStatus = new HashMap<String, MsgPattern>();
        m_executedFileReaders = new HashSet<String>();

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
            // lets ignore message from the file reader during workflow loading
            // when the node is executed and warning that the file isn't
            // accessible anymore.
            if (!isFileReaderMessageWhileLoading(aEvent)) {
                // an unintended error: add it to the list of unexpected errors
                if ((msgLevel == Level.ERROR) || (msgLevel == Level.FATAL)) {

                    m_unexpectedErrors.add(msg);

                    // make sure the list won't get too long
                    if (m_unexpectedErrors.size() > m_maxlines) {
                        m_unexpectedErrors.removeFirst();
                    }
                }
            } else {
                LOGGER.debug("Ignoring warning message of executed "
                        + "file reader during workflow loading");
            }
        } else {
            String dbgmsg = "Required msg. ('" + msg + "')";
            LOGGER.debug(dbgmsg);
        }
    }

    private boolean isFileReaderMessageWhileLoading(final LoggingEvent logMsg) {

        if (logMsg.getLevel() != Level.WARN) {
            // only warning message are ignored
            return false;
        }
        if (!m_testCase.isCurrentlyLoading()) {
            // only while the workflow is loading msg are ignored
            return false;
        }

        if (!logMsg.getLoggerName().equals(FileReader.class.getName())
                && !logMsg.getLoggerName().equals(FILEREADER_NAME)) {
            // only file reader messages are ignored
            return false;
        }

        return equalsFileNotAccessibleAnymoreMessage(logMsg
                .getRenderedMessage());
    }

    /**
     * @param String the message to check
     * @return true, if the message looks like file readers &quot;The file
     *         '&lt;blah&gt;' can't be accessed anymore!&quot;
     *
     */
    private boolean equalsFileNotAccessibleAnymoreMessage(final String msg) {
        if (msg != null && msg.startsWith("The file '")
                && msg.endsWith("' can't be accessed anymore!")) {
            return true;
        }
        return false;
    }

    /**
     * Searches the nodes of the WFM for executed file readers. Their warning
     * status will later on not cause the test case to fail then. That is
     * because ALL executed file readers warn in a regression test framework
     * about missing files. To avoid having all test creators add this warning
     * status to the test's node_status file, we crick our neck to ignore the
     * status later.
     *
     * @param wfm the workflow to examine
     */
    void registerExecutedFileReader(final WorkflowManager wfm) {
        String prefix = wfm.getID().toString() + ":";
        for (NodeContainer nc : wfm.getNodeContainers()) {

            if (!nc.getState().equals(State.EXECUTED)) {
                continue;
            }
            if (!nc.getName().equals(FILEREADER_NAME)) {
                continue;
            }
            NodeMessage msg = nc.getNodeMessage();
            if (msg == null
                    || !msg.getMessageType().equals(NodeMessage.Type.WARNING)) {
                continue;
            }
            if (!equalsFileNotAccessibleAnymoreMessage(msg.getMessage())) {
                continue;
            }

            m_executedFileReaders.add(shortID(nc, prefix));
            LOGGER.debug("Registering File Reader Node " + nc.getID()
                    + " as executed node with file-not-accessible-warning");
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

        LOGGER.debug("Applying new settings to workflow ----------------"
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

        applySettingsModifications(options, wfm);

    }

    private void applySettingsModifications(final List<Option> options,
            final WorkflowManager wfm) throws InvalidSettingsException {

        for (Option o : options) {
            int[] idPath = o.m_nodeIDs;
            NodeID subID = new NodeID(wfm.getID(), idPath[0]);
            NodeContainer cont = wfm.getNodeContainer(subID);
            WorkflowManager subWM = wfm;
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
                LOGGER.error("Can't modify settings: No node with id "
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
     * @throws FileNotFoundException if the file opening fails
     * @throws IOException if the file reading fails
     * @throws InvalidSettingsException if the settings in the file are
     *             incorrect
     */
    public void readNodeStatusFile(final File statusFile)
            throws FileNotFoundException, IOException, InvalidSettingsException {

        if (!statusFile.exists()) {
            return;
        }

        BufferedReader statusReader =
                new BufferedReader(new FileReader(statusFile));

        LOGGER.debug("Reading configuration file for node status / messages");
        String line;
        while ((line = statusReader.readLine()) != null) {

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
            // call this method only in one of these four cases
            assert false;
        }

        MsgPattern msg = extractMessagePart(line);
        if (msg == null) {
            throw new InvalidSettingsException("Invalid line in status file "
                    + "(missing ':'): " + line);
        }

        msgList.add(msg);
        LOGGER.debug("Expecting " + infoMsg + " message during test: " + msg);

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

        String nodeID = splits[0];

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
                if (msg == null) {
                    msg = "Node #" + nodeID;
                }
                msg += " should have a warning status '" + statMsg + "'";

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

        LOGGER.debug(msg);
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
        Collection<NodeContainer> nodes = manager.getNodeContainers();

        for (NodeContainer node : nodes) {

            State status = node.getState();
            String nodeSimpleID = Integer.toString(node.getID().getIndex());

            if (!status.equals(State.EXECUTED)
                    && !m_requiredUnexecutedNodes.contains(nodeSimpleID)) {

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
                LOGGER.error(msg);
            } else if (status.equals(State.EXECUTED)
                    && m_requiredUnexecutedNodes.contains(nodeSimpleID)) {

                // executed but shouldn't be

                String msg =
                        "Node " + node.getNameWithID()
                                + " is executed eventhough it shouldn't "
                                + "(as specified in the node status file)";
                // make sure to log an error - during wrapUp the test fails then
                LOGGER.error(msg);
            } else {

                // executed state as expected

                LOGGER.debug("Node '" + node.getNameWithID() + "' is"
                        + (status.equals(State.EXECUTED) ? " " : " not ")
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
        Collection<NodeContainer> nodes = wfm.getNodeContainers();
        String idPrefix = wfm.getID().toString() + ":";

        for (NodeContainer node : nodes) {

            NodeMessage status = node.getNodeMessage();
            if (status != null
                    && status.getMessageType().equals(NodeMessage.Type.ERROR)) {

                MsgPattern expMsg = m_errorStatus.get(shortID(node, idPrefix));
                if (expMsg == null) {
                    // node was not expected to finish with an error status
                    String msg =
                            "Node '" + node.getNameWithID() + "' has an "
                                    + "unexpected error status (message: "
                                    + status.getMessage() + ")";
                    // make sure to log an error
                    // during wrapUp the test fails then
                    LOGGER.error(msg);
                } else {
                    // make sure the error message is as expected.
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
                        LOGGER.error(msg);
                    } else {
                        LOGGER.debug("Node '" + node.getNameWithID()
                                + "' finished"
                                + " with an error status - which is good.");
                    }
                }

            } else if (status != null
                    && status.getMessageType().equals(NodeMessage.Type.WARNING)) {

                MsgPattern expMsg =
                        m_warningStatus.get(shortID(node, idPrefix));
                if (expMsg == null) {
                    // node was not expected to finish with a warning status -
                    // unless it was an executed file reader. That's okay.
                    if (!m_executedFileReaders
                            .contains(shortID(node, idPrefix))) {
                        String msg =
                                "Node '"
                                        + node.getNameWithID()
                                        + "' has an "
                                        + "unexpected warning status (message: "
                                        + status.getMessage() + ")";
                        // make sure to log an error
                        // during wrapUp the test fails then
                        LOGGER.error(msg);
                    } else {
                        String msg =
                            "Node '"
                                    + node.getNameWithID()
                                    + "' has an "
                                    + "unexpected warning status. Ignored as"
                                    + " it is an executed file reader.";
                        LOGGER.debug(msg);
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
                        LOGGER.error(msg);
                    } else {
                        LOGGER.debug("Node '" + node.getNameWithID()
                                + "' finished"
                                + " with a warning status - which is good.");
                    }
                }

            } else {
                // no or unknown status

                MsgPattern expMsg =
                        m_warningStatus.get(shortID(node, idPrefix));
                if (expMsg != null) {
                    String msg =
                            "Node '"
                                    + node.getNameWithID()
                                    + "' is supposed to have a warning status, "
                                    + "but it has not. (Expected warning msg: "
                                    + expMsg + ")";
                    // make sure to log an error
                    // during wrapUp the test fails then
                    LOGGER.error(msg);
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
                    LOGGER.error(msg);
                }

            }

        }

    }

    /**
     * Cuts off the prefix from the node container's ID.
     *
     * @param nc the node to create the short ID for
     * @param prefix the prefix cut off the node's ID
     * @return the node's ID w/o prefix or the full ID if the ID was not
     *         prefixed with the prefix
     */
    private String shortID(final NodeContainer nc, final String prefix) {
        String nodeID = nc.getID().toString();
        if (nodeID.startsWith(prefix)) {
            return nodeID.substring(prefix.length());
        } else {
            return nodeID;
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
                LOGGER.info("Got error: " + msg);
            }
            LOGGER.error("Unexpected error messages during test run.");
            unexpectedErrors = true;
        }
        if (m_exceptions.size() > 0) {
            Collection<String> excMsgs = m_exceptions;
            for (String e : excMsgs) {
                LOGGER.info("Got exception: " + e);
            }
            LOGGER.error("Exceptions during test run.");
            unexpectedErrors = true;
        }

        boolean missingMessages = false;

        if (m_requiredDebugs.size() > 0) {
            missingMessages = true;
            for (MsgPattern msg : m_requiredDebugs) {
                LOGGER.info("Missing DEBUG msg: \"" + msg + "\"");
            }
            LOGGER.error("Missing required DEBUG messages in the test output");
        }
        if (m_requiredInfos.size() > 0) {
            missingMessages = true;
            for (MsgPattern msg : m_requiredInfos) {
                LOGGER.info("Missing INFO msg: \"" + msg + "\"");
            }
            LOGGER.error("Missing required INFO messages in the test output");
        }
        if (m_requiredWarnings.size() > 0) {
            missingMessages = true;
            for (MsgPattern msg : m_requiredWarnings) {
                LOGGER.info("Missing WARNING msg: \"" + msg + "\"");
            }
            LOGGER.error("Missing required WARNING messages in the "
                    + "test output");
        }
        if (m_requiredErrors.size() > 0) {
            missingMessages = true;
            for (MsgPattern msg : m_requiredErrors) {
                LOGGER.info("Missing ERROR msg: \"" + msg + "\"");
            }
            LOGGER.error("Missing required ERROR messages in the test output");
        }

        // this method must cause the test to fail, as previous checks just
        // log error messages if something was fishy
        if (unexpectedErrors && missingMessages) {
            // that's the message log file analyzers pick up
            LOGGER.fatal("Unexpected and missing messages" + FAIL_MSG);
            Assert.fail("Failing due to unexpected errors "
                    + "and missing messages in the log file.");
        }
        if (unexpectedErrors) {
            LOGGER.fatal("Unexpected error messages" + FAIL_MSG);
            Assert.fail("Failing due to unexpected errors "
                    + "in the log file.");
        }
        if (missingMessages) {
            LOGGER.fatal("Missing required messages" + FAIL_MSG);
            Assert.fail("Failing due to missing messages in the log file.");
        }

        LOGGER.info(SUCCESS_MSG);

    }

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
                    LOGGER.error("Pattern to compare message to contains "
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
         * Returns the stored message pattern.
         *
         * @return the stored message pattern.
         */
        public String getPattern() {
            return m_msg;
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
                    actualMsg.substring(m_patternPart1.length(), actualMsg
                            .length()
                            - m_patternPart3.length());
            Matcher matcher = m_regExpr.matcher(msgRegExpr);
            return matcher.matches();

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
        public String toString() {
            return m_msg;
        }
    }
}
