/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
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
 *   Apr 11, 2014 (wiswedel): created
 */
package org.knime.core.node.workflow.virtual.subnode;

import java.util.Collections;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.variable.FlowVariableFilterConfiguration;
import org.knime.core.node.workflow.FlowVariable;

/**
 * Configuration to virtual sub node output node. Currently only hosts the variables to pass in.
 * <p>No API.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class VirtualSubNodeOutputConfiguration {

    /**
     * Default name for ports, '?' will be replaced with the ports number (starting by 1).
     */
    private static final String DEFAULT_PORT_NAME = "Port ?";

    /**
     * Default description for ports, '?' will be replaced with the ports number (starting by 1).
     */
    private static final String DEFAULT_PORT_DESCRIPTION = "";

    private int m_numberOfPorts;

    private FlowVariableFilterConfiguration m_filterConfiguration;
    private String m_flowVariablePrefix;

    private String[] m_portNames;
    private String[] m_portDescriptions;

    /**
     * @param numberOfPorts The number of in ports of this virtual out node
     */
    public VirtualSubNodeOutputConfiguration(final int numberOfPorts) {
        m_numberOfPorts = numberOfPorts;
    }

    /** @return the filterConfiguration (possibly null if not assigned). */
    FlowVariableFilterConfiguration getFilterConfiguration() {
        return m_filterConfiguration;
    }

    /** @return create empty configuration. */
    static FlowVariableFilterConfiguration createFilterConfiguration() {
        return new FlowVariableFilterConfiguration("variable-filter");
    }

    /** @param f the filterConfiguration to set */
    void setFilterConfiguration(final FlowVariableFilterConfiguration f) {
        m_filterConfiguration = f;
    }

    /** @return the flowVariablePrefix or null */
    String getFlowVariablePrefix() {
        return m_flowVariablePrefix;
    }

    /** @param flowVariablePrefix the flowVariablePrefix to set (or null). */
    void setFlowVariablePrefix(final String flowVariablePrefix) {
        m_flowVariablePrefix = flowVariablePrefix;
    }

    /**
     * @return Array of port names.
     */
    String[] getPortNames() {
        return m_portNames;
    }

    /**
     * @param portNames Array of port names.
     */
    void setPortNames(final String[] portNames) {
        m_portNames = portNames;
    }

    /**
     * @return Array of port descriptions.
     */
    String[] getPortDescriptions() {
        return m_portDescriptions;
    }

    /**
     * @param portDescriptions Array of port descriptions.
     */
    void setPortDescriptions(final String[] portDescriptions) {
        m_portDescriptions = portDescriptions;
    }

    /** Save current config to argument.
     * @param settings ...
     */
    void saveConfiguration(final NodeSettingsWO settings) {
        if (m_filterConfiguration != null) {
            m_filterConfiguration.saveConfiguration(settings);
            settings.addString("variable-prefix", m_flowVariablePrefix);
        }
        settings.addStringArray("port-names", m_portNames);
        settings.addStringArray("port-descriptions", m_portDescriptions);
    }

    /** Load routine to be called from dialog.
     * @param settings ...
     * @param variableMap all available flow variables
     */
    void loadConfigurationInDialog(final NodeSettingsRO settings, final Map<String, FlowVariable> variableMap) {
        m_filterConfiguration = createFilterConfiguration();
        m_filterConfiguration.loadConfigurationInDialog(settings, variableMap);
        m_flowVariablePrefix = settings.getString("variable-prefix", null);
        m_portNames = correctedPortNames(settings.getStringArray("port-names", new String[0]), m_numberOfPorts);
        m_portDescriptions =
            correctedPortDescriptions(settings.getStringArray("port-descriptions", new String[0]), m_numberOfPorts);
    }

    /** Load routine for model implementation.
     * @param settings ...
     * @throws InvalidSettingsException if incomplete or invalid.
     */
    void loadConfigurationInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_filterConfiguration = createFilterConfiguration();
        m_filterConfiguration.loadConfigurationInModel(settings);
        m_flowVariablePrefix = settings.getString("variable-prefix");
        m_portNames = correctedPortNames(settings.getStringArray("port-names"), m_numberOfPorts);
        m_portDescriptions = correctedPortDescriptions(settings.getStringArray("port-descriptions"), m_numberOfPorts);
    }

    private static String[] correctedPortNames(final String[] portNames, final int numberOfPorts) {
        String[] correctedPortNames = new String[numberOfPorts];
        for (int i = 0; i < correctedPortNames.length; i++) {
            String name = i < portNames.length ? portNames[i] : DEFAULT_PORT_NAME.replace("?", "" + (i + 1));
            correctedPortNames[i] = name;
        }
        return correctedPortNames;
    }

    private static String[] correctedPortDescriptions(final String[] portDescriptions, final int numberOfPorts) {
        String[] correctedPortDescriptions = new String[numberOfPorts];
        for (int i = 0; i < correctedPortDescriptions.length; i++) {
            String description =
                i < portDescriptions.length ? portDescriptions[i] : DEFAULT_PORT_DESCRIPTION.replace("?", "" + (i + 1));
            correctedPortDescriptions[i] = description;
        }
        return correctedPortDescriptions;
    }

    /** @param numberOfPorts The number of output ports of this virtual in node
     * @return New default configuration, used during auto-guessing. */
    static VirtualSubNodeOutputConfiguration newDefault(final int numberOfPorts) {
        VirtualSubNodeOutputConfiguration r = new VirtualSubNodeOutputConfiguration(numberOfPorts);
        r.m_flowVariablePrefix = null;
        r.m_filterConfiguration = createFilterConfiguration();
        r.m_filterConfiguration.loadDefaults(Collections.<String, FlowVariable>emptyMap(), false);
        r.m_portNames = correctedPortNames(new String[0], numberOfPorts);
        r.m_portDescriptions = correctedPortDescriptions(new String[0], numberOfPorts);
        return r;
    }

}
