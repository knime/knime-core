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
 * ---------------------------------------------------------------------
 *
 * History
 *   07.08.2013 (thor): created
 */
package org.knime.testing.core.ng;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.testing.node.config.TestConfigNodeModel;
import org.knime.testing.node.config.TestConfigSettings;

/**
 * Configuration for a testflow. The configuration is either read from old-style configuration files in the workflows
 * directory for from a testflow configuration node inside the workflow.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
class TestflowConfiguration {
    /**
     * The default maximum number of rows (currently {@value}) that are hilited during a workflow test.
     */
    public static final int DEFAULT_MAX_HILITE_ROWS = 2500;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(TestflowConfiguration.class);

    private static final String REGEX_PATTERN = "_!_";

    private final WorkflowManager m_manager;

    private int m_timeout = TestrunConfiguration.DEFAULT_TIMEOUT;

    private final List<String> m_owners = new ArrayList<String>();

    private final Set<NodeID> m_requiredUnexecutedNodes = new HashSet<NodeID>();

    private final Collection<Pattern> m_requiredErrors = new ArrayList<Pattern>();

    private final Collection<Pattern> m_requiredWarnings = new ArrayList<Pattern>();

    private final Collection<Pattern> m_requiredInfos = new ArrayList<Pattern>();

    private final Map<NodeID, Pattern> m_nodeWarningMessages = new HashMap<NodeID, Pattern>();

    private final Map<NodeID, Pattern> m_nodeErrorMessages = new HashMap<NodeID, Pattern>();

    private final Collection<Pattern> m_requiredDebugs = new ArrayList<Pattern>();

    private int m_maxHiliteRows = DEFAULT_MAX_HILITE_ROWS;

    /**
     * Creates a new testflow configuration. The configuration is read from the testflow configuration node inside the
     * workflow. If no such node exists, configuration files are read. If no files exists, a default configuration is
     * used.
     *
     * @param workflowManager the manager of the test workflow
     * @throws InvalidSettingsException if the testflow configuration node's settings cannot be read
     * @throws IOException if the configuration files cannot be read
     */
    public TestflowConfiguration(final WorkflowManager workflowManager) throws InvalidSettingsException, IOException {
        m_manager = workflowManager;

        boolean loaded = false;
        for (NodeContainer cont : m_manager.getNodeContainers()) {
            if ((cont instanceof NativeNodeContainer)
                    && (((NativeNodeContainer)cont).getNodeModel() instanceof TestConfigNodeModel)) {
                load((NativeNodeContainer)cont);
                loaded = true;
            }
        }

        if (!loaded) {
            load(workflowManager.getContext().getCurrentLocation());
        }
    }

    private void load(final NativeNodeContainer configNode) throws InvalidSettingsException {
        NodeSettings s = new NodeSettings("");
        NodeContext.pushContext(configNode);
        try {
            configNode.getNode().saveModelSettingsTo(s);
        } finally {
            NodeContext.removeLastContext();
        }
        TestConfigSettings settings = new TestConfigSettings();
        settings.loadSettings(s);

        for (String owner : settings.owner().split(",")) {
            m_owners.add(owner);
        }

        for (String error : settings.requiredLogErrors()) {
            m_requiredErrors.add(createPatternFromMessage(error));
        }

        for (String warning : settings.requiredLogWarnings()) {
            m_requiredWarnings.add(createPatternFromMessage(warning));
        }

        for (String info : settings.requiredLogInfos()) {
            m_requiredInfos.add(createPatternFromMessage(info));
        }

        for (String id : settings.failingNodes()) {
            m_requiredUnexecutedNodes.add(createCompleteNodeID(id));
        }

        for (Map.Entry<String, String> e : settings.requiredNodeErrors().entrySet()) {
            NodeID nodeId = createCompleteNodeID(e.getKey());
            try {
                NodeContainer cont = m_manager.findNodeContainer(nodeId);
                Pattern pattern = createPatternFromMessage(e.getValue());
                m_nodeErrorMessages.put(nodeId, pattern);
                if (!cont.getNodeContainerState().isExecuted() && !(cont instanceof WorkflowManager)) {
                    // error status on node also creates an error in the log if the node is not already executed
                    // and if it's not a workflow manager
                    m_requiredErrors.add(pattern);
                }
            } catch (IllegalArgumentException ex) {
                LOGGER.warn("No node with id '" + e.getKey() + "' found in "
                        + "workflow, but we have a configuration for it. Ignoring the configuration.");
            }
        }

        for (Map.Entry<String, String> e : settings.requiredNodeWarnings().entrySet()) {
            NodeID nodeId = createCompleteNodeID(e.getKey());
            try {
                NodeContainer cont = m_manager.findNodeContainer(nodeId);
                Pattern pattern = createPatternFromMessage(e.getValue());
                m_nodeWarningMessages.put(nodeId, pattern);
                if (!cont.getNodeContainerState().isExecuted() && !(cont instanceof WorkflowManager)) {
                    // warning status on node also creates an error in the log if the node is not already executed
                    // and if it's not a workflow manager
                    m_requiredWarnings.add(pattern);
                }
            } catch (IllegalArgumentException ex) {
                LOGGER.warn("No node with id '" + e.getKey() + "' found in "
                        + "workflow, but we have a configuration for it. Ignoring the configuration.");
            }
        }

        m_timeout = settings.timeout();
        m_maxHiliteRows = settings.maxHiliteRows();
    }

    /**
     * Reads the configuration from the old-style files.
     *
     * @param workflowDir the workflow directory
     * @throws IOException if the files cannot be read
     * @throws InvalidSettingsException if the settings in the files are incorrect
     */
    private void load(final File workflowDir) throws IOException, InvalidSettingsException {
        readOwnersFromFile(new File(workflowDir, "owner"));
        readNodeStatusFromFile(new File(workflowDir, "node_status"));
    }

    /**
     * Reads in the file that contains the nodes that should show a certain status after execution.
     *
     * @param statusFile the file containing the lines defining the node status after execution. If it doesn't exist,
     *            the method returns without error or without doing anything.
     * @throws IOException if the file reading fails
     * @throws InvalidSettingsException if the settings in the file are incorrect
     */
    private void readNodeStatusFromFile(final File statusFile) throws IOException, InvalidSettingsException {
        if (!statusFile.exists()) {
            return;
        }

        BufferedReader statusReader =
                new BufferedReader(new InputStreamReader(new FileInputStream(statusFile), Charset.forName("UTF-8")));

        LOGGER.debug("Reading configuration file '" + statusFile + "' for node status / messages");
        String line;
        while ((line = statusReader.readLine()) != null) {
            if (line.isEmpty()) {
                // ignore empty lines
                continue;
            }
            String uppercaseLine = line.toUpperCase();
            if (uppercaseLine.startsWith("ERROR") || uppercaseLine.startsWith("WARN")
                    || uppercaseLine.startsWith("INFO") || uppercaseLine.startsWith("DEBUG")) {
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
     * Parses one line from the status file assuming it specifies a required error/warning/info or debug message.
     *
     * @param line the line to be parsed
     * @throws InvalidSettingsException if the settings in the file are incorrect
     */
    private void parseMessageLine(final String line) throws InvalidSettingsException {
        Collection<Pattern> msgList = null;
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

        Pattern pattern = extractMessagePart(line);
        if (pattern == null) {
            throw new InvalidSettingsException("Invalid line in status file (missing ':'): " + line);
        }

        msgList.add(pattern);
        LOGGER.debug("Expecting " + infoMsg + " message during test: " + pattern);
    }

    /**
     * Parses and applies one line from the status file that contains the post-execute status of a node.
     *
     * @param line the line specifying the status of the node.
     * @throws InvalidSettingsException if the line is invalid
     */
    private void parseNodeStatusLine(final String line) throws InvalidSettingsException {
        String[] splits = line.split(",", 2);
        if (splits.length < 2) {
            throw new InvalidSettingsException("Invalid line in status file (specify node id and status): " + line);
        }

        NodeID nodeID = createCompleteNodeID(splits[0]);

        String msg = null;

        // see if we got a required error/warning status
        if (splits.length == 2) {
            String[] stats = splits[1].split(" ", 2);
            if (stats.length != 2) {
                throw new InvalidSettingsException("Invalid line in status file (invalid err/warn status): " + line);
            }
            Pattern pattern = extractMessagePart(stats[1]);
            if (pattern == null) {
                throw new InvalidSettingsException("Invalid line in status file (invalid err status msg): " + line);
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
                m_nodeErrorMessages.put(nodeID, pattern);
                if (msg == null) {
                    msg = "Node #" + nodeID;
                }
                msg += " should have an error status '" + pattern + "'";

                /*
                 * an error status also creates an error message in the log file
                 */
                // add message of the error status to the list of expected msgs
                m_requiredErrors.add(pattern);
            } else if (stats[0].toUpperCase().startsWith("WARN")) {
                m_nodeWarningMessages.put(nodeID, pattern);
                msg = "Node #" + nodeID + " should have a warning status '" + pattern + "'";

                /*
                 * a warning status also creates a warn message in the log file
                 * lets make this required.
                 */
                // add message of the warn status to the list of expected msgs
                m_requiredWarnings.add(pattern);
            } else {
                throw new InvalidSettingsException("Invalid line in status file " + "(invalid err/warn status): "
                        + line);
            }
        }

        LOGGER.debug(msg);
    }

    /**
     * From a line of the log file it extracts the part that is the interesting message. That is the part behind the
     * first colon, dismissing the date, time, level, and class info. It is important to extract the message that was
     * sent to the logger from the line.
     *
     * @param line to extract the message from
     * @return the interesting message part of the specified line, or null, if the line has an unexpected format.
     */
    private Pattern extractMessagePart(final String line) {
        String[] splits = line.split(" : ", 2);
        if (splits.length != 2) {
            return null;
        }
        return createPatternFromMessage(splits[1]);
    }

    /**
     * Reads the owners of the test from the specified owner file. A non-existing file is ignored.
     *
     * @param ownerFile the file containing the owners of the test case. Each line of the file must contain one email
     *            address.
     * @throws IOException if an I/O error occurs
     */
    private void readOwnersFromFile(final File ownerFile) throws IOException {
        if (ownerFile.exists()) {
            BufferedReader r =
                    new BufferedReader(new InputStreamReader(new FileInputStream(ownerFile), Charset.forName("UTF-8")));
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
     * Builds a node id using the current workflow manager as prefix and the given partial node id as suffix.
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
     * Returns the workflow timeout in seconds. After the timeout the execution of the workflow is canceled. The default
     * timeout is {@link TestrunConfiguration#DEFAULT_TIMEOUT}.
     *
     * @return the timeout in seconds.
     */
    public int getTimeout() {
        return m_timeout;
    }

    /**
     * Returns whether the node with the given ID is supposed to fail during execution of the workflow.
     *
     * @param nodeId the node's full ID (including the workflow manager's prefix)
     * @return <code>true</code> if the node must fail, <code>false</code> otherwise
     */
    public boolean nodeMustFail(final NodeID nodeId) {
        return m_requiredUnexecutedNodes.contains(nodeId);
    }

    /**
     * Returns the pattern for the expected error message for the given node.
     *
     * @param nodeId the node's full ID (including the workflow manager's prefix)
     * @return a pattern specifying the expected error message
     */
    public Pattern getNodeErrorMessage(final NodeID nodeId) {
        return m_nodeErrorMessages.get(nodeId);
    }

    /**
     * Returns the pattern for the expected warning message for the given node.
     *
     * @param nodeId the node's full ID (including the workflow manager's prefix)
     * @return a pattern specifying the expected warning message
     */
    public Pattern getNodeWarningMessage(final NodeID nodeId) {
        return m_nodeWarningMessages.get(nodeId);
    }

    /**
     * Returns a collection of expected log error messages.
     *
     * @return a collection of patterns specifying the expected messages
     */
    public Collection<Pattern> getRequiredErrors() {
        return m_requiredErrors;
    }

    /**
     * Returns a collection of expected log warning messages.
     *
     * @return a collection of patterns specifying the expected messages
     */
    public Collection<Pattern> getRequiredWarnings() {
        return m_requiredWarnings;
    }

    /**
     * Returns a collection of expected log info messages.
     *
     * @return a collection of patterns specifying the expected messages
     */
    public Collection<Pattern> getRequiredInfos() {
        return m_requiredInfos;
    }

    /**
     * Returns a collection of expected log debug messages.
     *
     * @return a collection of patterns specifying the expected messages
     */
    public Collection<Pattern> getRequiredDebugs() {
        return m_requiredDebugs;
    }

    /**
     * Returns the maximum number of rows that are hilit during the hilite test.
     *
     * @return the maximum number of rows
     */
    public int getMaxHiliteRows() {
        return m_maxHiliteRows;
    }


    private static Pattern createPatternFromMessage(String message) {
        int index = message.indexOf(REGEX_PATTERN);

        if (index < 0) {
            return Pattern.compile(Pattern.quote(message));
        } else {
            StringBuilder patternString = new StringBuilder();

            while (index >= 0) {
                // non-regex part
                patternString.append("\\Q").append(message, 0, index).append("\\E");

                // regex pattern starts
                message = message.substring(index + REGEX_PATTERN.length());
                index = message.indexOf(REGEX_PATTERN);
                if (index >= 0) {
                    patternString.append(message.substring(0, index));
                } else {
                    patternString.append(message);
                }

                // regex pattern ends
                message = message.substring(index + REGEX_PATTERN.length());
                index = message.indexOf(REGEX_PATTERN);
            }
            patternString.append("\\Q").append(message).append("\\E");

            return Pattern.compile(patternString.toString(), Pattern.DOTALL);
        }
    }

    /**
     * Returns a human-readable string for the given pattern. If the pattern does not contain any regular expression
     * parts, the plain string is returned, otherwise the full pattern including all meta-characters is returned.
     *
     * @param pattern any pattern
     * @return a string for the pattern
     */
    public static String patternToString(final Pattern pattern) {
        String s = pattern.pattern();

        if (s.startsWith("\\Q")) {
            if ((s.indexOf("\\Q", "\\Q".length()) >= 0) || !s.endsWith("\\E")) {
                // we have a real regex part in the message => output complete pattern
                return s;
            } else {
                return s.substring("\\Q".length(), s.length() - "\\E".length());
            }
        } else {
            // strange, not quoted at all
            return s;
        }
    }

}
