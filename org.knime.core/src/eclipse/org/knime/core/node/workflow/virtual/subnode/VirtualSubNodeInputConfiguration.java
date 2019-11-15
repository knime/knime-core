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
 *   Apr 11, 2014 (wiswedel): created
 */
package org.knime.core.node.workflow.virtual.subnode;

import java.util.Collections;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Configuration to virtual sub node input node. Currently only hosts the variables to pass in.
 * <p>No API.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
final class VirtualSubNodeInputConfiguration extends AbstractVirtualSubNodeConfiguration {
    private String m_subNodeDescription;

    /**
     * @param numberOfPorts The number of out ports of this virtual in node
     */
    public VirtualSubNodeInputConfiguration(final int numberOfPorts) {
        super(numberOfPorts);
    }

    /**
     * @return The sub node description.
     * @deprecated component metadata should be used
     */
    @Deprecated
    String getSubNodeDescription() {
        return m_subNodeDescription;
    }

    /**
     * @param subNodeDescription The sub node description.
     * @deprecated component metadata should be used
     */
    @Deprecated
    void setSubNodeDescription(final String subNodeDescription) {
        m_subNodeDescription = subNodeDescription;
    }

    /**
     * Save current config to argument.
     * @param settings ...
     */
    @Override
    void saveConfiguration(final NodeSettingsWO settings) {
        super.saveConfiguration(settings);
        settings.addString("sub-node-description", m_subNodeDescription);
    }

    /**
     * Load routine to be called from dialog.
     * @param settings ...
     * @param variableMap all available flow variables
     */
    @Override
    void loadConfigurationInDialog(final NodeSettingsRO settings, final Map<String, FlowVariable> variableMap) {
        super.loadConfigurationInDialog(settings, variableMap);
        m_subNodeDescription = settings.getString("sub-node-description", "");
    }

    /**
     * Load routine for model implementation.
     * @param settings ...
     * @throws InvalidSettingsException if incomplete or invalid.
     */
    @Override
    void loadConfigurationInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadConfigurationInModel(settings);
        m_subNodeDescription = settings.getString("sub-node-description");
    }

    /** @param numberOfPorts The number of output ports of this virtual in node
     * @return New default configuration, used during auto-guessing. */
    static VirtualSubNodeInputConfiguration newDefault(final int numberOfPorts) {
        VirtualSubNodeInputConfiguration r = new VirtualSubNodeInputConfiguration(numberOfPorts);
        r.m_flowVariablePrefix = null;
        r.m_filterConfiguration = createFilterConfiguration();
        r.m_filterConfiguration.loadDefaults(Collections.<String, FlowVariable>emptyMap(), false);
        r.m_subNodeDescription = "";
        r.m_portNames = correctedPortNames(new String[0], numberOfPorts);
        r.m_portDescriptions = correctedPortDescriptions(new String[0], numberOfPorts);
        return r;
    }
}
