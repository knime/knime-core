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
 * Configuration to virtual sub node output node. Currently only hosts the variables to pass in.
 * <p>No API.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
final class VirtualSubNodeOutputConfiguration extends AbstractVirtualSubNodeConfiguration {

    /**
     * added in 5.1.1, see AP-20843: in order go guarantee backward compatibility with 5.0/4.7 (and before) we allow the
     * component output node to execute before other nodes get executed. This behavior was previously "accidently
     * desired" in case the component output is part of a loop body and loop end awaits execution of body.
     *
     * (N.B. The component itself is executed only when _all_ nodes contained in it are executed -- unchanged behavior
     * since v1 of components.)
     *
     * Option is hidden (not user configurable) and defaults to <code>false</code> for components created in 5.1.1+.
     *
     * @since 5.1.1
     */
    // default for new components (created with 5.1.1+) is false
    private boolean m_allowOutputNodeToCompleteBeforeContent;

    /**
     * @param numberOfPorts The number of out ports of this virtual out node
     */
    public VirtualSubNodeOutputConfiguration(final int numberOfPorts) {
        super(numberOfPorts);
    }

    /**
     * @return the allowOutputNodeToCompleteBeforeContent, see field description for details.
     */
    boolean isAllowOutputNodeToCompleteBeforeContent() {
        return m_allowOutputNodeToCompleteBeforeContent;
    }

    @Override
    void saveConfiguration(final NodeSettingsWO settings) {
        super.saveConfiguration(settings);
        settings.addBoolean("allowOutputNodeToCompleteBeforeContent", m_allowOutputNodeToCompleteBeforeContent);
    }

    @Override
    void loadConfigurationInDialog(final NodeSettingsRO settings, final Map<String, FlowVariable> variableMap) {
        super.loadConfigurationInDialog(settings, variableMap);
        m_allowOutputNodeToCompleteBeforeContent = settings.getBoolean("allowOutputNodeToCompleteBeforeContent", false);
    }

    @Override
    void loadConfigurationInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadConfigurationInModel(settings);
        // 4.7 components didn't have the flag so their value needs to be true
        m_allowOutputNodeToCompleteBeforeContent = settings.getBoolean("allowOutputNodeToCompleteBeforeContent", true);
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
