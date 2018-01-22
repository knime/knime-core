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
 *   22 Feb 2018 (albrecht): created
 */
package org.knime.core.wizard;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.js.core.JSONViewResponse;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Container class encapsulating a view response for a specific node inside a combined view.
 *
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 * @since 3.6
 */
@JsonAutoDetect
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public class SubnodeViewResponse extends JSONViewResponse<SubnodeViewRequest> {

    private String m_nodeID;

    private static final String CFG_JSON_RESPONSE = "jsonResponse";
    private String m_jsonResponse;

    /**
     * Creates a new response object for subnode views.
     *
     * @param request
     * @param nodeID the node id of the view the response is for
     * @param jsonResponse the JSON serialized response for the specific view
     */
    public SubnodeViewResponse(final SubnodeViewRequest request, final String nodeID,
        final String jsonResponse) {
        super(request);
        m_nodeID = nodeID;
        m_jsonResponse = jsonResponse;
    }

    /**
     * @return the nodeID
     */
    public String getNodeID() {
        return m_nodeID;
    }

    /**
     * @return the jsonResponse
     */
    @JsonRawValue
    public String getJsonResponse() {
        return m_jsonResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveToNodeSettings(final NodeSettingsWO settings) {
        super.saveToNodeSettings(settings);
        settings.addString(SubnodeViewRequest.CFG_NODE_ID, m_nodeID);
        settings.addString(CFG_JSON_RESPONSE, m_jsonResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadFromNodeSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadFromNodeSettings(settings);
        m_nodeID = settings.getString(SubnodeViewRequest.CFG_NODE_ID);
        m_jsonResponse = settings.getString(CFG_JSON_RESPONSE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        SubnodeViewResponse other = (SubnodeViewResponse)obj;
        return new EqualsBuilder()
                .appendSuper(super.equals(obj))
                .append(m_nodeID, other.m_nodeID)
                .append(m_jsonResponse, other.m_jsonResponse)
                .isEquals();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(m_nodeID)
                .append(m_jsonResponse)
                .toHashCode();
    }

}
