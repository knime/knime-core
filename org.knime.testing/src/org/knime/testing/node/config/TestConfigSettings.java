/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 *   07.09.2011 (meinl): created
 */
package org.knime.testing.node.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * This class holds the settings for the testflow configuration node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class TestConfigSettings {
    private String m_owner;

    private Map<String, String> m_requiredNodeErrors =
            new HashMap<String, String>();

    private Map<String, String> m_requiredNodeWarnings =
            new HashMap<String, String>();

    private List<String> m_requiredLogErrors = new ArrayList<String>();

    private List<String> m_requiredLogWarnings = new ArrayList<String>();

    private List<String> m_requiredLogInfos = new ArrayList<String>();

    private Set<String> m_failingNodes = new HashSet<String>();

    private static final String[] EMPTY = new String[0];

    /**
     * Returns the email address of the workflow owner. Multiple addresses are
     * separated by comma.
     *
     * @return an e-mail address
     */
    public String owner() {
        return m_owner;
    }

    /**
     * Sets the email address of the workflow owner. Multiple addresses can be
     * separated by comma.
     *
     * @param address an e-mail address
     */
    public void owner(final String address) {
        m_owner = address;
    }

    /**
     * Returns a list of required error messages in the log file.
     *
     * @return a list with error messages
     */
    public List<String> requiredLogErrors() {
        return Collections.unmodifiableList(m_requiredLogErrors);
    }

    /**
     * Returns a list of required warning messages in the log file.
     *
     * @return a list with warning messages
     */
    public List<String> requiredLogWarnings() {
        return Collections.unmodifiableList(m_requiredLogWarnings);
    }

    /**
     * Returns a list of required info messages in the log file.
     *
     * @return a list with info messages
     */
    public List<String> requiredLogInfos() {
        return Collections.unmodifiableList(m_requiredLogInfos);
    }

    /**
     * Returns a set with the IDs of the nodes that must fail during the test.
     *
     * @return a set with node IDs <b>without</b> the root prefix.
     */
    public Set<String> failingNodes() {
        return Collections.unmodifiableSet(m_failingNodes);
    }

    /**
     * Returns a map with required error messages for individual nodes. The
     * map's keys are the node's ID (without the root prefix), the values are
     * the messages.
     *
     * @return a map with required error messages
     */
    public Map<String, String> requiredNodeErrors() {
        return Collections.unmodifiableMap(m_requiredNodeErrors);
    }

    /**
     * Returns a map with required warning messages for individual nodes. The
     * map's keys are the node's ID (without the root prefix), the values are
     * the messages.
     *
     * @return a map with required warning messages
     */
    public Map<String, String> requiredNodeWarnings() {
        return Collections.unmodifiableMap(m_requiredNodeWarnings);
    }

    /**
     * Sets the required error messages in the log file.
     *
     * @param col a collection with error messages
     */
    public void requiredLogErrors(final Collection<String> col) {
        m_requiredLogErrors.clear();
        m_requiredLogErrors.addAll(col);
    }

    /**
     * Sets the required warning messages in the log file.
     *
     * @param col a collection with warning messages
     */
    public void requiredLogWarnings(final Collection<String> col) {
        m_requiredLogWarnings.clear();
        m_requiredLogWarnings.addAll(col);
    }

    /**
     * Sets the required info messages in the log file.
     *
     * @param col a collection with info messages
     */
    public void requiredLogInfos(final Collection<String> col) {
        m_requiredLogInfos.clear();
        m_requiredLogInfos.addAll(col);
    }

    /**
     * Adds a node that must fail during the test. The node's ID must be given
     * without the root prefix.
     *
     * @param nodeID the node's id without the root prefix
     */
    public void addFailingNode(final String nodeID) {
        m_failingNodes.add(nodeID);
    }

    /**
     * Removes a node that must fail during the test. The node's ID must be
     * given without the root prefix.
     *
     * @param nodeID the node's id without the root prefix
     */
    public void removeFailingNode(final String nodeID) {
        m_failingNodes.remove(nodeID);
    }

    /**
     * Sets a required warning message on a node.
     *
     * @param nodeID the node's ID without the root prefix
     * @param message the expected message
     */
    public void setRequiredNodeWarning(final String nodeID, final String message) {
        if (message == null) {
            m_requiredNodeWarnings.remove(nodeID);
        } else {
            m_requiredNodeWarnings.put(nodeID, message);
        }
    }

    /**
     * Sets a required error message on a node.
     *
     * @param nodeID the node's ID without the root prefix
     * @param message the expected message
     */
    public void setRequiredNodeError(final String nodeID, final String message) {
        if (message == null) {
            m_requiredNodeErrors.remove(nodeID);
        } else {
            m_requiredNodeErrors.put(nodeID, message);
        }
    }

    /**
     * Loads the settings from the given settings object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if an expected setting is missing
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_owner = settings.getString("owner");

        String[] temp = settings.getStringArray("requiredLogErrors");
        m_requiredLogErrors.clear();
        for (String s : temp) {
            m_requiredLogErrors.add(s);
        }

        temp = settings.getStringArray("requiredLogWarnings");
        m_requiredLogWarnings.clear();
        for (String s : temp) {
            m_requiredLogWarnings.add(s);
        }

        temp = settings.getStringArray("requiredLogInfos");
        m_requiredLogInfos.clear();
        for (String s : temp) {
            m_requiredLogInfos.add(s);
        }

        temp = settings.getStringArray("failingNodes");
        m_failingNodes.clear();
        for (String s : temp) {
            m_failingNodes.add(s);
        }

        m_requiredNodeWarnings.clear();
        NodeSettingsRO subs = settings.getNodeSettings("requiredNodeWarnings");
        int count = subs.getInt("count", 0);
        for (int i = 0; i < count; i++) {
            String nodeID = subs.getString("nodeID_" + i, null);
            String message = subs.getString("message_" + i, null);
            if ((nodeID != null) && (message != null)) {
                m_requiredNodeWarnings.put(nodeID, message);
            }
        }

        m_requiredNodeErrors.clear();
        subs = settings.getNodeSettings("requiredNodeErrors");
        count = subs.getInt("count", 0);
        for (int i = 0; i < count; i++) {
            String nodeID = subs.getString("nodeID_" + i, null);
            String message = subs.getString("message_" + i, null);
            if ((nodeID != null) && (message != null)) {
                m_requiredNodeErrors.put(nodeID, message);
            }
        }
    }

    /**
     * Loads the settings from the given settings object using default values
     * for missing settings.
     *
     * @param settings a node settings object
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_owner = settings.getString("owner", null);
        if (m_owner == null) {
            m_owner = System.getProperty("user.name") + "@inf.uni-konstanz.de";
        }

        String[] temp = settings.getStringArray("requiredLogErrors", EMPTY);
        m_requiredLogErrors.clear();
        for (String s : temp) {
            m_requiredLogErrors.add(s);
        }

        temp = settings.getStringArray("requiredLogWarnings", EMPTY);
        m_requiredLogWarnings.clear();
        for (String s : temp) {
            m_requiredLogWarnings.add(s);
        }

        temp = settings.getStringArray("requiredLogInfos", EMPTY);
        m_requiredLogInfos.clear();
        for (String s : temp) {
            m_requiredLogInfos.add(s);
        }

        temp = settings.getStringArray("failingNodes", EMPTY);
        m_failingNodes.clear();
        for (String s : temp) {
            m_failingNodes.add(s);
        }

        m_requiredNodeWarnings.clear();
        try {
            NodeSettingsRO subs =
                    settings.getNodeSettings("requiredNodeWarnings");
            int count = subs.getInt("count", 0);
            for (int i = 0; i < count; i++) {
                String nodeID = subs.getString("nodeID_" + i, null);
                String message = subs.getString("message_" + i, null);
                if ((nodeID != null) && (message != null)) {
                    m_requiredNodeWarnings.put(nodeID, message);
                }
            }
        } catch (InvalidSettingsException ex) {
        }

        m_requiredNodeErrors.clear();
        try {
            NodeSettingsRO subs =
                    settings.getNodeSettings("requiredNodeErrors");
            int count = subs.getInt("count", 0);
            for (int i = 0; i < count; i++) {
                String nodeID = subs.getString("nodeID_" + i, null);
                String message = subs.getString("message_" + i, null);
                if ((nodeID != null) && (message != null)) {
                    m_requiredNodeErrors.put(nodeID, message);
                }
            }
        } catch (InvalidSettingsException ex) {
        }
    }

    /**
     * Saves the settings into the given settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString("owner", m_owner);
        settings.addStringArray("requiredLogErrors",
                m_requiredLogErrors.toArray(EMPTY));
        settings.addStringArray("requiredLogWarnings",
                m_requiredLogWarnings.toArray(EMPTY));
        settings.addStringArray("requiredLogInfos",
                m_requiredLogInfos.toArray(EMPTY));
        settings.addStringArray("failingNodes", m_failingNodes.toArray(EMPTY));

        NodeSettingsWO subs = settings.addNodeSettings("requiredNodeWarnings");
        int i = 0;
        for (Map.Entry<String, String> e : m_requiredNodeWarnings.entrySet()) {
            subs.addString("nodeID_" + i, e.getKey());
            subs.addString("message_" + i, e.getValue());
            i++;
        }
        subs.addInt("count", i);

        subs = settings.addNodeSettings("requiredNodeErrors");
        i = 0;
        for (Map.Entry<String, String> e : m_requiredNodeErrors.entrySet()) {
            subs.addString("nodeID_" + i, e.getKey());
            subs.addString("message_" + i, e.getValue());
            i++;
        }
        subs.addInt("count", i);
    }

    /**
     * Returns an node id suffix, which is essentially the given node container's id minus the given workflow
     * manager's id.
     *
     * @param root the workflow manager which is the current root for nodes
     * @param cont a node container
     * @return a node id suffix
     */
    public static String getNodeIDWithoutRootPrefix(final WorkflowManager root, final NodeContainer cont) {
        int prefixLength = root.getID().toString().length();

        String nodeId = cont.getID().toString();
        return nodeId.substring(prefixLength + 1);
    }

}
