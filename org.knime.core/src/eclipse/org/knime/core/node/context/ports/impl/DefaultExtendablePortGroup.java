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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.IntStream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.ExtendablePortGroup;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;

/**
 * Implementation of an extendable port group.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class DefaultExtendablePortGroup implements ExtendablePortGroup {

    private final PortType[] m_fixedTypes;

    private final PortType[] m_supportedTypes;

    private final List<PortType> m_configuredTypes;

    private final boolean m_definesInputPorts;

    private final boolean m_definesOutputPorts;

    private final int m_maxAdditionalPorts;

    /**
     * Constructor.
     *
     * @param fixedPortTypes the fixed port types
     * @param supportedTypes the supported port types
     * @param definesInputPorts flag indicating whether this port group defines input ports
     * @param definesOutputPorts flag indicating whether this port group defines output ports
     */
    public DefaultExtendablePortGroup(final PortType[] fixedPortTypes, final PortType[] supportedTypes,
        final boolean definesInputPorts, final boolean definesOutputPorts) {
        this(fixedPortTypes, supportedTypes, definesInputPorts, definesOutputPorts, Integer.MAX_VALUE);
    }

    /**
     * Constructor.
     *
     * @param fixedPortTypes the fixed port types
     * @param supportedTypes the supported port types
     * @param definesInputPorts flag indicating whether this port group defines input ports
     * @param definesOutputPorts flag indicating whether this port group defines output ports
     * @param maxAdditionalPorts the maximum number of additional ports
     */
    public DefaultExtendablePortGroup(final PortType[] fixedPortTypes, final PortType[] supportedTypes,
        final boolean definesInputPorts, final boolean definesOutputPorts, final int maxAdditionalPorts) {
        m_fixedTypes = fixedPortTypes;
        m_supportedTypes = supportedTypes;
        m_configuredTypes = new ArrayList<>();
        m_definesInputPorts = definesInputPorts;
        m_definesOutputPorts = definesOutputPorts;
        m_maxAdditionalPorts = maxAdditionalPorts;
    }

    private DefaultExtendablePortGroup(final PortType[] fixedPortTypes, final PortType[] supportedTypes,
        final List<PortType> configuredPorts, final boolean definesInputPorts, final boolean definesOutputPorts,
        final int maxAdditionalPorts) {
        m_fixedTypes = fixedPortTypes;
        m_supportedTypes = supportedTypes;
        m_configuredTypes = configuredPorts;
        m_definesInputPorts = definesInputPorts;
        m_definesOutputPorts = definesOutputPorts;
        m_maxAdditionalPorts = maxAdditionalPorts;
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
    public DefaultExtendablePortGroup copy() {
        return new DefaultExtendablePortGroup(m_fixedTypes.clone(), m_supportedTypes.clone(),
            new ArrayList<PortType>(m_configuredTypes), m_definesInputPorts, m_definesOutputPorts,
            m_maxAdditionalPorts);
    }

    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        IntStream.range(0, m_configuredTypes.size())//
            .forEach(idx -> m_configuredTypes.get(idx).save(settings.addNodeSettings("port_" + idx)));
    }

    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_configuredTypes.clear();
        @SuppressWarnings("unchecked")
        Enumeration<NodeSettingsRO> children = (Enumeration<NodeSettingsRO>)settings.children();
        while (children.hasMoreElements()) {
            m_configuredTypes.add(PortType.load(children.nextElement()));
        }
    }

    @Override
    public PortType[] getFixedPorts() {
        return m_fixedTypes;
    }

    @Override
    public PortType[] getConfiguredPorts() {
        return m_configuredTypes.toArray(new PortType[0]);
    }

    @Override
    public boolean canAddPort() {
        return m_maxAdditionalPorts > getConfiguredPorts().length;
    }

    @Override
    public boolean hasConfiguredPorts() {
        return !m_configuredTypes.isEmpty();
    }

    @Override
    public void addPort(final PortType pType) {
        m_configuredTypes.add(pType);
    }

    @Override
    public PortType removeLastPort() {
        return m_configuredTypes.remove(m_configuredTypes.size() - 1);
    }

    @Override
    public PortType removePort(final int portIndex) {
        CheckUtils.checkArgument(portIndex >= m_fixedTypes.length,
            "Port at index %s cannot be removed. Index is within fixed port types. ".formatted(portIndex));
        var totalNumberOfPorts = m_configuredTypes.size() + m_fixedTypes.length;
        CheckUtils.checkArgument(portIndex < totalNumberOfPorts,
            "Port at index %s cannot be removed. There are only %s ports in total.".formatted(portIndex,
                totalNumberOfPorts));
        return m_configuredTypes.remove(portIndex - m_fixedTypes.length);
    }

    @Override
    public PortType[] getSupportedPortTypes() {
        return m_supportedTypes;
    }

}
