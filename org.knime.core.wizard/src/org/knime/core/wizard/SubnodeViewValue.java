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
 *   25 Apr 2017 (albrecht): created
 */
package org.knime.core.wizard;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.js.core.JSONViewContent;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * View value for combined subnode view, contains of map of contained view values
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 */
public class SubnodeViewValue extends JSONViewContent {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(SubnodeViewValue.class);

    private Map<String, String> m_viewValues = new HashMap<String, String>();

    /**
     * @return the viewValues
     */
    @JsonAnyGetter
    public Map<String, String> getViewValues() {
        return m_viewValues;
    }

    /**
     * @param viewValues the viewValues to set
     */
    public void setViewValues(final Map<String, String> viewValues) {
        m_viewValues = viewValues;
    }

    /**
     * @param key
     * @param value
     */
    @JsonAnySetter
    public void addViewValue(final String key, final Object value) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            m_viewValues.put(key, mapper.writeValueAsString(value));
        } catch (JsonProcessingException e) { /* do nothing */ }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveToNodeSettings(final NodeSettingsWO settings) { /* nothing to save */ }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadFromNodeSettings(final NodeSettingsRO settings) throws InvalidSettingsException { /* nothing to load */ }

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
        SubnodeViewValue other = (SubnodeViewValue)obj;
        if (!m_viewValues.keySet().equals(other.m_viewValues.keySet())) {
            return false;
        }
        EqualsBuilder builder = new EqualsBuilder();
        ObjectMapper mapper = new ObjectMapper();
        for (String key : m_viewValues.keySet()) {
            try {
                // try deserializing and comparing generic JSON objects
                JsonNode first = mapper.readTree(m_viewValues.get(key));
                JsonNode second = mapper.readTree(other.m_viewValues.get(key));
                // the following would be better but concrete view classes might not be visible here
                /*JSONViewContent first = mapper.readValue(m_viewValues.get(key), JSONViewContent.class);
                JSONViewContent second = mapper.readValue(other.m_viewValues.get(key), JSONViewContent.class);*/
                builder.append(first, second);
            } catch (Exception e) {
                LOGGER.debug("Can't compare JsonNode in #equals", e);
                //compare strings on exception
                builder.append(m_viewValues.get(key), other.m_viewValues.get(key));
            }
        }
        return builder.isEquals();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(m_viewValues)
                .toHashCode();
    }

}