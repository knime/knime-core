/*
 * ------------------------------------------------------------------------
 *
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
 *   Oct 27, 2008 (wiswedel): created
 */
package org.knime.core.node.exec.dataexchange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.CredentialsStore;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.ICredentials;

/**
 * Settings helper that reads/writes the port object ID that is used by the {@link PortObjectRepository}.
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @noreference This class is not intended to be referenced by clients.
 * @since 3.1
 */
public final class PortObjectIDSettings {

    private Integer m_id;
    private List<FlowVariable> m_flowVariables;
    private boolean m_copyData;
    private CredentialsProvider m_credentialsProvider;

    /** Constructor, which sets a null ID (no id). */
    public PortObjectIDSettings() {
        // empty
    }

    /** Loads the settings from a NodeSettings object.
     * @param settings to load from.
     * @throws InvalidSettingsException If no settings present or invalid.
     */
    public void loadSettings(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        m_id = settings.getInt("portobject_ID");
        m_copyData = settings.getBoolean("copyData");
        m_flowVariables = new ArrayList<FlowVariable>();
        // added for cluster version 1.0.2
        if (settings.containsKey("flowVariables")) {
            NodeSettingsRO sub = settings.getNodeSettings("flowVariables");
            for (String key : sub.keySet()) {
                NodeSettingsRO child = sub.getNodeSettings(key);

                // code copied from (package scope) load method in FlowVariable
                String name = child.getString("name");
                String typeS = child.getString("class");
                if (typeS == null || name == null) {
                    throw new InvalidSettingsException("name or type is null");
                }
                FlowVariable.Type varType;
                try {
                    varType = FlowVariable.Type.valueOf(typeS);
                } catch (final IllegalArgumentException e) {
                    throw new InvalidSettingsException("invalid type " + typeS);
                }
                FlowVariable v;
                switch (varType) {
                case DOUBLE:
                    v = new FlowVariable(name, child.getDouble("value"));
                    break;
                case INTEGER:
                    v = new FlowVariable(name, child.getInt("value"));
                    break;
                case STRING:
                    v = new FlowVariable(name, child.getString("value"));
                    break;
                case CREDENTIALS:
                    CheckUtils.checkState(m_credentialsProvider != null, "No credentials provider set");
                    ICredentials credentials = m_credentialsProvider.get(child.getString("value"));
                    v = CredentialsStore.newCredentialsFlowVariable(
                        credentials.getName(), credentials.getLogin(), credentials.getPassword(), false, false);
                    break;
                default:
                    throw new InvalidSettingsException(
                            "Unknown type " + varType);
                }
                m_flowVariables.add(v);
            }
        }
    }

    /** Saves the current settings to a NodeSettings object.
     * @param settings To write to. */
    public void saveSettings(final NodeSettingsWO settings) {
        if (m_id != null) {
            settings.addInt("portobject_ID", m_id);
        }
        settings.addBoolean("copyData", m_copyData);
        NodeSettingsWO sub = settings.addNodeSettings("flowVariables");
        int index = 0;
        for (FlowVariable fv : getFlowVariables()) {
            NodeSettingsWO child = sub.addNodeSettings("flowVar_" + (index++));
            // copied from (package scope) save method in FlowVariable
            child.addString("name", fv.getName());
            child.addString("class", fv.getType().name());
            switch (fv.getType()) {
            case INTEGER:
                child.addInt("value", fv.getIntValue());
                break;
            case DOUBLE:
                child.addDouble("value", fv.getDoubleValue());
                break;
            case STRING:
                child.addString("value", fv.getStringValue());
                break;
            case CREDENTIALS:
                child.addString("value", fv.getName());
                break;
            default:
                assert false : "Unknown variable type: " + fv.getType();
            }
        }
    }

    /**
     * Get the currently set ID or null if none have been set.
     * @return the id
     */
    public Integer getId() {
        return m_id;
    }

    /**
     * Set new ID for the port object, setting null invalidates the settings.
     * @param id the id to set
     */
    public void setId(final Integer id) {
        m_id = id;
    }

    /** Set list of flow variables to be exposed by the node.
     * @param flowVariables the flowVariables to set
     */
    public void setFlowVariables(final List<FlowVariable> flowVariables) {
        m_flowVariables = flowVariables;
    }

    /** List of flow variables to be exposed, never null.
     * @return the flowVariables
     */
    public List<FlowVariable> getFlowVariables() {
        if (m_flowVariables == null) {
            return Collections.emptyList();
        }
        return m_flowVariables;
    }

    /** @param copyData the copyData to set */
    public void setCopyData(final boolean copyData) {
        m_copyData = copyData;
    }

    /** @return the copyData */
    public boolean isCopyData() {
        return m_copyData;
    }

    /**
     * Sets the credentials provider to read the credentials from (in case there are credentials flow variables to be loaded).
     * Only required for loading the settings.
     *
     * @param cp the credentials provider
     */
    public void setCredentialsProvider(final CredentialsProvider cp) {
        m_credentialsProvider = cp;
    }

}
