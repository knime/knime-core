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
 *   Oct 11, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.node.context.ports.impl;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortGroupConfiguration;
import org.knime.core.node.port.PortType;

/**
 * A fixed port group, i.e., a port group whose ports cannot be changed.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class DefaultFixedPortGroup implements PortGroupConfiguration {

    private final PortType[] m_fixedPortTypes;

    private final boolean m_definesInputPorts;

    private final boolean m_definesOutputPorts;

    /**
     * Constructor.
     *
     * @param fixedPortTypes the fixed ports
     * @param definesInputPorts flag indicating whether this port group defines input ports
     * @param definesOutputPorts flag indicating whether this port group defines output ports
     */
    public DefaultFixedPortGroup(final PortType[] fixedPortTypes, final boolean definesInputPorts,
        final boolean definesOutputPorts) {
        m_fixedPortTypes = fixedPortTypes;
        m_definesInputPorts = definesInputPorts;
        m_definesOutputPorts = definesOutputPorts;
    }

    @Override
    public DefaultFixedPortGroup copy() {
        return new DefaultFixedPortGroup(m_fixedPortTypes.clone(), m_definesInputPorts, m_definesOutputPorts);
    }

    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        // nothing to do
    }

    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        // nothing to do
    }

    @Override
    public boolean definesInputPorts() {
        return m_definesInputPorts;
    }

    @Override
    public boolean definesOutputPorts() {
        return m_definesOutputPorts;
    }

    @Override
    public PortType[] getInputPorts() {
        if (definesInputPorts()) {
            return m_fixedPortTypes;
        }
        throw UNSUPPORTED_INPUT_OPERATION;
    }

    @Override
    public PortType[] getOutputPorts() {
        if (definesOutputPorts()) {
            return m_fixedPortTypes;
        }
        throw UNSUPPORTED_OUTPUT_OPERATION;
    }

}
