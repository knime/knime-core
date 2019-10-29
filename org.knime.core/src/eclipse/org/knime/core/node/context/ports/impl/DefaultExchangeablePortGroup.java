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
 *   Oct 10, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.node.context.ports.impl;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.ExchangeablePortGroup;
import org.knime.core.node.port.PortType;

/**
 * Implementation of an exchangeable port group.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class DefaultExchangeablePortGroup implements ExchangeablePortGroup {

    private PortType m_curType;

    private final PortType[] m_supportedTypes;

    private final boolean m_definesInputPorts;

    private final boolean m_definesOutputPorts;

    /**
     * Constructor.
     *
     * @param defaultType the default port type
     * @param supportedTypes the supported port types has to include the default type
     * @param definesInputPorts flag indicating whether this port group defines input ports
     * @param definesOutputPorts flag indicating whether this port group defines output ports
     */
    public DefaultExchangeablePortGroup(final PortType defaultType, final PortType[] supportedTypes,
        final boolean definesInputPorts, final boolean definesOutputPorts) {
        m_curType = defaultType;
        m_supportedTypes = supportedTypes;
        m_definesInputPorts = definesInputPorts;
        m_definesOutputPorts = definesOutputPorts;
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
    public DefaultExchangeablePortGroup copy() {
        return new DefaultExchangeablePortGroup(m_curType, m_supportedTypes, m_definesInputPorts, m_definesOutputPorts);
    }

    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_curType.save(settings);
    }

    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        setSelectedPortType(PortType.load(settings));
    }

    @Override
    public PortType getSelectedPortType() {
        return m_curType;
    }

    @Override
    public PortType[] getSupportedPortTypes() {
        return m_supportedTypes;
    }

    @Override
    public void setSelectedPortType(final PortType pType) {
        m_curType = pType;
    }

}
