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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
     * The message inserted if the test fails due to excpetions (analyzers will
     * parse for it).
     */
    public static final String EXCEPT_FAIL_MSG = "Got EXCEPTIONs during run";

    private List<String> m_owners;

    private Collection<String> m_requiredErrors;

    private Collection<String> m_requiredWarnings;

    private Collection<String> m_requiredInfos;

    private Collection<String> m_requiredDebugs;

    private Collection<Integer> m_requiredUnexecutedNodes;

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

        m_requiredErrors = null;
        m_requiredWarnings = null;
        m_requiredInfos = null;
        m_requiredDebugs = null;
        m_requiredUnexecutedNodes = null;
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
     * Set a list of error messages all incoming error messages are compared
     * against. You can always get the count of still "missing" error messages.
     * 
     * @param errMsgs the error messages that should appear.
     */
    public void setRequiredErrorMessages(final Collection<String> errMsgs) {
        m_requiredErrors = new LinkedList<String>(errMsgs);
    }

    /**
     * Returns the list of error messages that have not occurred so far.
     * 
     * @return the list of error messages that have not occurred so far.
     */
    public Collection<String> getMissingErrorMessages() {
        if (m_requiredErrors != null) {
            return Collections.unmodifiableCollection(m_requiredErrors);
        } else {
            return new LinkedList<String>();
        }
    }

    /**
     * Set a list of warning messages all incoming warnings messages are
     * compared against. You can always get the count of still "missing" warning
     * messages.
     * 
     * @param warnMsgs the warning messages you want to appear.
     */
    public void setRequiredWarningMessages(final Collection<String> warnMsgs) {
        m_requiredWarnings = new LinkedList<String>(warnMsgs);
    }

    /**
     * Returns the list of warning messages that have not been seen so far.
     * 
     * @return the list of warning messages that have not been seen so far.
     */
    public Collection<String> getMissingWarningMessages() {
        if (m_requiredWarnings != null) {
            return Collections.unmodifiableCollection(m_requiredWarnings);
        } else {
            return new LinkedList<String>();
        }
    }

    /**
     * Set a list of info messages all incoming info messages are compared
     * against. You can always get the count of still "missing" info messages.
     * 
     * @param infoMsgs the info messages that should appear.
     */
    public void setRequiredInfoMessages(final Collection<String> infoMsgs) {
        m_requiredInfos = new LinkedList<String>(infoMsgs);
    }

    /**
     * Returns the list of info messages that have not occurred so far.
     * 
     * @return the list of info messages that have not occurred so far.
     */
    public Collection<String> getMissingInfoMessages() {
        if (m_requiredInfos != null) {
            return Collections.unmodifiableCollection(m_requiredInfos);
        } else {
            return new LinkedList<String>();
        }

    }

    /**
     * Set a list of debug messages all incoming debug messages are compared
     * against. You can always get the count of still "missing" debug messages.
     * 
     * @param debugMsgs the debug messages that should appear.
     */
    public void setRequiredDebugMessages(final Collection<String> debugMsgs) {
        m_requiredDebugs = new LinkedList<String>(debugMsgs);
    }

    /**
     * Returns the list of debug messages that have not occurred so far.
     * 
     * @return the list of debug messages that have not occurred so far.
     */
    public Collection<String> getMissingDebugMessages() {
        if (m_requiredDebugs != null) {
            return Collections.unmodifiableCollection(m_requiredDebugs);
        } else {
            return new LinkedList<String>();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void append(final LoggingEvent aEvent) {

        String msg = aEvent.getRenderedMessage();
        Level msgLevel = aEvent.getLevel();

        // try recognizing Stacktraces in warning messages.
        if ((msgLevel == Level.ERROR) || (msgLevel == Level.WARN)
                || (msgLevel == Level.FATAL)) {
            if (msg != null) {
                boolean exceptionStartLine = false;
                String lines[] = msg.replace("\r", "").split("\n");

                /*
                 * An Exception starts with a line containing "....Exception:
                 * ..." followed by a line starting with "TABat ...blah...:<linenumber>)"
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
        boolean expected = false;
        Collection<String> msgList = null;
        if (msg != null) {
            if ((msgLevel == Level.ERROR) && (m_requiredErrors != null)) {
                msgList = m_requiredErrors;
            } else if ((msgLevel == Level.WARN) && (m_requiredWarnings != null)) {
                msgList = m_requiredWarnings;
            } else if ((msgLevel == Level.INFO) && (m_requiredInfos != null)) {
                msgList = m_requiredInfos;
            } else if ((msgLevel == Level.DEBUG) && (m_requiredDebugs != null)) {
                msgList = m_requiredDebugs;
            }
        }
        // no check if its contained the corresponding list
        if (msgList != null) {
            for (String required : msgList) {
                if (msg.equals(required)) {
                    msgList.remove(required);
                    expected = true;
                    break;
                }
            }
        }

        if (!expected) {
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
     * @return the error messages received so far.
     */
    public String[] getReceivedMessages() {
        return m_unexpectedErrors
                .toArray(new String[m_unexpectedErrors.size()]);
    }

    /**
     * @return the number of error messages received so far.
     */
    public int getMessageCount() {
        return m_unexpectedErrors.size();
    }

    /**
     * @return the first lines of the stacktraces recieved.
     */
    public String[] getExceptions() {
        return m_exceptions.toArray(new String[m_exceptions.size()]);
    }

    /**
     * @return the number or stacktraces in error or warning messages received
     *         so far.
     */
    public int getExceptionsCount() {
        return m_exceptions.size();
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
    public void applyChanges(final File optionsFile, final WorkflowManager wfm)
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
            LOGGER
                    .debug(node.getName() + " isExecuted == "
                            + node.isExecuted());

            NodeStatus status = node.getStatus();

            if (!node.isExecuted()
                    && !m_requiredUnexecutedNodes.contains(node.getID())) {
                String msg = "\nNode " + node.getName() + " is not executed\n ";
                if (status != null) {
                    msg += status.getMessage();
                }
                // make sure to log the reason for failure.
                LOGGER.error(msg);
                Assert.fail(msg);
            } else {
                if (status != null && (status instanceof NodeStatus.Error)) {
                    String msg =
                            "\nNode " + node.getName()
                                    + " executed with errors: \n ";
                    msg += status.getMessage();
                    // make sure to log the reason for failure.
                    LOGGER.error(msg);
                    Assert.fail(msg);
                }
            }
        }

    }

    /**
     * Causes the test to fail, if unexpected error messages or exceptions where
     * caught by the appender.
     */
    public void checkMessages() {

        boolean testFails = false;

        if (getMessageCount() > 0) {
            String[] errMsgs = getReceivedMessages();
            for (String msg : errMsgs) {
                LOGGER.error("Got error: " + msg);
            }
            LOGGER.error(ERR_FAIL_MSG + " -> FAILING! Check the log file.");
            testFails = true;
        }
        if (getExceptionsCount() > 0) {
            String[] excMsgs = getExceptions();
            for (String e : excMsgs) {
                LOGGER.error("Got exception: " + e);
            }
            LOGGER.error(EXCEPT_FAIL_MSG + " -> FAILING! "
                    + "Check the log file.");
            testFails = true;
        }
        if (testFails) {
            Assert.fail("Failing due to errors or exceptions in the log file.");
        }

    }
}
