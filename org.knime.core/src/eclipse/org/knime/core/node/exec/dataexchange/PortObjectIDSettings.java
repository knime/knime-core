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
 *   Oct 27, 2008 (wiswedel): created
 */
package org.knime.core.node.exec.dataexchange;

import java.net.URI;
import java.net.URISyntaxException;
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
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.VariableType.CredentialsType;

/**
 * Settings helper that reads/writes the port object ID that is used by the {@link PortObjectRepository}.
 *
 * @author Bernd Wiswedel, University of Konstanz
 * @noreference This class is not intended to be referenced by clients.
 * @since 3.1
 */
public final class PortObjectIDSettings {

    private ReferenceType m_refType;

    // repo ref type details
    private Integer m_id;

    // node ref type details
    private NodeIDSuffix m_nodeIDSuffix;
    private int m_portIdx;

    // file ref type details
    private URI m_uri;
    private boolean m_isTable;

    private List<FlowVariable> m_flowVariables;
    private boolean m_copyData;
    private CredentialsProvider m_credentialsProvider;

    /**
     * Type that determines how the port object is referenced.
     */
    // TODO we might decide to delete this when implementing AP-13335
    public static enum ReferenceType {
            /**
             * Reference via the {@link PortObjectRepository} and an ID.
             */
            REPOSITORY,
            /**
             * Reference to a node (node ID suffix and port idx) in the same workflow.
             */
            NODE,
            /**
             * Reference to a file given by a URI.
             */
            FILE;
    }

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
        m_refType = ReferenceType.valueOf(settings.getString("referenceType", ReferenceType.REPOSITORY.toString()));
        switch (m_refType) {
            case REPOSITORY:
                m_id = settings.getInt("portobject_ID");
                break;
            case NODE:
                try {
                    m_nodeIDSuffix = NodeIDSuffix.fromString(settings.getString("node_ID_suffix"));
                } catch (IllegalArgumentException ex1) {
                    throw new InvalidSettingsException(ex1);
                }
                m_portIdx = settings.getInt("port_idx");
                CheckUtils.checkSetting(m_portIdx >= 0, "Port index must be >= 0: %d", m_portIdx);
                break;
            case FILE:
                try {
                    m_uri = new URI(settings.getString("uri"));
                } catch (URISyntaxException ex) {
                    throw new InvalidSettingsException(ex);
                }
                m_isTable = settings.getBoolean("is_table");
                break;
        }
        m_copyData = settings.getBoolean("copyData");
        m_flowVariables = new ArrayList<FlowVariable>();
        // added for cluster version 1.0.2
        if (settings.containsKey("flowVariables")) {
            NodeSettingsRO sub = settings.getNodeSettings("flowVariables");
            for (String key : sub.keySet()) {
                final NodeSettingsRO child = sub.getNodeSettings(key);

                final String name = child.getString("name");
                final String typeS = child.getString("class");
                if (typeS == null || name == null) {
                    throw new InvalidSettingsException("name or type is null");
                }

                final FlowVariable v;
                /* Flow variables of types Credentials and FSConnection are handled separately, using the
                 * credentialsProvider and fsConnectionProvider members, respectively. */
                if (typeS.equals(CredentialsType.INSTANCE.getIdentifier())) {
                    CheckUtils.checkState(m_credentialsProvider != null, "No credentials provider set");
                    final ICredentials credentials = m_credentialsProvider.get(name);
                    v = CredentialsStore.newCredentialsFlowVariable(credentials.getName(), credentials.getLogin(),
                        credentials.getPassword(), false, false);
                } else {
                    v = FlowVariable.load(child);
                }

                m_flowVariables.add(v);
            }
        }
    }

    /** Saves the current settings to a NodeSettings object.
     * @param settings To write to. */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString("referenceType", m_refType.toString());
        switch (m_refType) {
            case REPOSITORY:
                if (m_id != null) {
                    settings.addInt("portobject_ID", m_id);
                }
                break;
            case NODE:
                settings.addString("node_ID_suffix", m_nodeIDSuffix.toString());
                settings.addInt("port_idx", m_portIdx);
                break;
            case FILE:
                settings.addString("uri", m_uri.toString());
                settings.addBoolean("is_table", m_isTable);
                break;
        }
        settings.addBoolean("copyData", m_copyData);
        NodeSettingsWO sub = settings.addNodeSettings("flowVariables");
        int index = 0;
        for (FlowVariable fv : getFlowVariables()) {
            NodeSettingsWO child = sub.addNodeSettings("flowVar_" + (index++));
            fv.save(child);
        }
    }

    /**
     * Sets the infos required if a port object of another node (in the same workflow) is referenced.
     *
     * @param nodeIDSuffix the 'relative' node id to reference
     * @param portIdx the port index
     */
    public void setNodeReference(final NodeIDSuffix nodeIDSuffix, final int portIdx) {
        m_refType = ReferenceType.NODE;
        m_nodeIDSuffix = nodeIDSuffix;
        m_portIdx = portIdx;
    }

    /**
     * Sets the info required if a port object file is referenced.
     *
     * @param uri the file URI, possibly a knime-url
     * @param isTable whether the reference port object is a data table or not
     */
    public void setFileReference(final URI uri, final boolean isTable) {
        m_refType = ReferenceType.FILE;
        m_uri = uri;
        m_isTable = isTable;
    }

    /**
     * @return the type of reference
     */
    public ReferenceType getReferenceType() {
        return m_refType;
    }

    /**
     * @return the node ID suffix if reference type {@link ReferenceType#NODE}, otherwise possibly <code>null</code>
     */
    public NodeIDSuffix getNodeIDSuffix() {
        return m_nodeIDSuffix;
    }

    /**
     * @return the port index if reference type is {@link ReferenceType#NODE}, otherwise possibly <code>null</code>
     */
    public int getPortIdx() {
        return m_portIdx;
    }

    /**
     * @return the file uri if reference type is {@link ReferenceType#FILE}, otherwise possibly <code>null</code>
     */
    public URI getUri() {
        return m_uri;
    }

    /**
     * @return whether the referenced file represents a table. NOTE: Only of meaningful, if reference type is
     *         {@link ReferenceType#FILE}!
     */
    public boolean isTable() {
        return m_isTable;
    }

    /**
     * Get the currently set ID or null if none have been set. Only meaningful if reference type is
     * {@link ReferenceType#REPOSITORY}.
     *
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
        m_refType = ReferenceType.REPOSITORY;
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
