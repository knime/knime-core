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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.ExtendablePortGroup;
import org.knime.core.node.context.ports.PortGroupConfiguration;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.CheckUtils;

/**
 * Implementation of an bound extendible port group.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 */
public final class TypeBoundExtendablePortGroup implements ExtendablePortGroup {

    private static final PortType[] EMPTY_TYPES = new PortType[0];

    private final PortType[] m_supportedTypes;

    private final List<PortType> m_configuredTypes;

    private final boolean m_definesInputPorts;

    private final boolean m_definesOutputPorts;

    private final int m_maxAdditionalPorts;

    private String m_boundTo;

    private int m_fixedPortNumber;

    private int m_lastConfiguredPortNumber;

    private Map<String, PortGroupConfiguration> m_lookupTable;

    /**
     * Constructor.
     *
     * @param boundTo the name of the port group this port group is bound to
     * @param lookupTable the map used to search for the bound port group
     * @param fixedPortNumber the amount of fixed ports that should be created if there is a type
     * @param defaultPortNumber the amount of default ports, that should be created if there is a type for the first
     *            time
     * @param definesInputPorts flag indicating whether this port group defines input ports
     * @param definesOutputPorts flag indicating whether this port group defines output ports
     */
    public TypeBoundExtendablePortGroup(final String boundTo, final Map<String, PortGroupConfiguration> lookupTable,
        final int fixedPortNumber, final int defaultPortNumber, final boolean definesInputPorts,
        final boolean definesOutputPorts) {
        this(boundTo, lookupTable, fixedPortNumber, defaultPortNumber, definesInputPorts, definesOutputPorts,
            Integer.MAX_VALUE - fixedPortNumber);
    }

    /**
     * Constructor.
     *
     * @param boundTo the name of the port group this port group is bound to
     * @param lookupTable the map used to search for the bound port group
     * @param fixedPortNumber the amount of fixed ports that should be created if there is a type
     * @param defaultPortNumber the amount of default ports, that should be created if there is a type for the first
     *            time
     * @param definesInputPorts flag indicating whether this port group defines input ports
     * @param definesOutputPorts flag indicating whether this port group defines output ports
     * @param maxAdditionalPorts the maximum number of additional ports
     */
    public TypeBoundExtendablePortGroup(final String boundTo, final Map<String, PortGroupConfiguration> lookupTable,
        final int fixedPortNumber, final int defaultPortNumber, final boolean definesInputPorts,
        final boolean definesOutputPorts, final int maxAdditionalPorts) {
        m_supportedTypes = new PortType[1];
        m_definesInputPorts = definesInputPorts;
        m_definesOutputPorts = definesOutputPorts;
        m_maxAdditionalPorts = maxAdditionalPorts + fixedPortNumber;
        m_configuredTypes = new ArrayList<>();
        m_boundTo = boundTo;
        m_lookupTable = lookupTable;
        m_fixedPortNumber = fixedPortNumber;
        m_lastConfiguredPortNumber = fixedPortNumber + defaultPortNumber;
    }

    private TypeBoundExtendablePortGroup(final String boundTo, final Map<String, PortGroupConfiguration> lookupTable, //NOSONAR: copy constructor
        final int fixedPortNumber, final int lastConfiguredPortNumber, final PortType[] supportedTypes,
        final List<PortType> configuredPorts, final boolean definesInputPorts, final boolean definesOutputPorts,
        final int maxAdditionalPorts) {
        m_supportedTypes = supportedTypes;
        m_definesInputPorts = definesInputPorts;
        m_definesOutputPorts = definesOutputPorts;
        m_maxAdditionalPorts = maxAdditionalPorts;
        m_configuredTypes = configuredPorts;
        m_boundTo = boundTo;
        m_lookupTable = lookupTable;
        m_fixedPortNumber = fixedPortNumber;
        m_lastConfiguredPortNumber = lastConfiguredPortNumber;
    }

    @Override
    public TypeBoundExtendablePortGroup copy() {
        return new TypeBoundExtendablePortGroup(m_boundTo, m_lookupTable, m_fixedPortNumber, m_lastConfiguredPortNumber,
            m_supportedTypes.clone(), new ArrayList<PortType>(m_configuredTypes), m_definesInputPorts,
            m_definesOutputPorts, m_maxAdditionalPorts);
    }

    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString("boundTo", m_boundTo);
        settings.addInt("fixedPortNumber", m_fixedPortNumber);
        settings.addInt("lastConfiguredPortNumber", m_lastConfiguredPortNumber);
        settings.addInt("configuredPortNumber", m_configuredTypes.size());
        if (m_supportedTypes[0] != null) {
            m_supportedTypes[0].save(settings.addNodeSettings("portType"));
        }
    }

    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_configuredTypes.clear();
        m_boundTo = settings.getString("boundTo");
        m_fixedPortNumber = settings.getInt("fixedPortNumber");
        m_lastConfiguredPortNumber = settings.getInt("lastConfiguredPortNumber");
        final var noPorts = settings.getInt("configuredPortNumber");
        if (settings.containsKey("portType")) {
            final var portType = PortType.load(settings.getNodeSettings("portType"));
            m_supportedTypes[0] = portType;
            Stream.generate(() -> portType)//
                .limit(noPorts)//
                .forEach(m_configuredTypes::add); //NOSONAR: we want to modify an existing list
        } else {
            m_supportedTypes[0] = null;
        }
    }

    @Override
    public PortType[] getConfiguredPorts() {
        mayClear();
        return m_configuredTypes.toArray(new PortType[0]);
    }

    @Override
    public boolean canAddPort() {
        return getBoundType().isPresent() && getConfiguredPorts().length < m_maxAdditionalPorts - m_fixedPortNumber;
    }

    @Override
    public boolean hasConfiguredPorts() {
        mayClear();
        return m_configuredTypes.size() - m_fixedPortNumber > 0;
    }

    @Override
    public void addPort(final PortType pType) {
        m_configuredTypes.add(m_supportedTypes[0]);
    }

    @Override
    public PortType[] getSupportedPortTypes() {
        mayClear();
        if (m_supportedTypes[0] == null) {
            return EMPTY_TYPES;
        } else {
            return m_supportedTypes;
        }
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
    public PortType[] getFixedPorts() {
        return EMPTY_TYPES; // we never have actual fixed ports, only emulated ones
    }

    @Override
    public PortType removeLastPort() {
        return m_configuredTypes.remove(m_configuredTypes.size() - 1);
    }

    @Override
    public PortType removePort(final int portIndex) {
        CheckUtils.checkArgument(portIndex >= m_fixedPortNumber,
            "Port at index %s cannot be removed. Index is within fixed port types. ".formatted(portIndex));
        var totalNumberOfPorts = m_configuredTypes.size() + m_fixedPortNumber;
        CheckUtils.checkArgument(portIndex < totalNumberOfPorts,
            "Port at index %s cannot be removed. There are only %s ports in total.".formatted(portIndex,
                totalNumberOfPorts));
        return m_configuredTypes.remove(portIndex - m_fixedPortNumber);
    }

    void setLookupTable(final Map<String, PortGroupConfiguration> lookupTable) {
        m_lookupTable = lookupTable;
    }

    private void mayClear() {
        final var boundType = getBoundType();
        if (boundType.map(t -> !t.equals(m_supportedTypes[0])).orElse(Boolean.TRUE)) {
            m_supportedTypes[0] = boundType.orElse(null);
            m_configuredTypes.clear();
            if (boundType.isPresent()) {
                Stream.generate(boundType::get)//
                    .limit(m_lastConfiguredPortNumber)//
                    .forEach(m_configuredTypes::add); //NOSONAR: we want to modify an existing list
            }
        } else if (boundType.isPresent()) {
            m_lastConfiguredPortNumber = m_configuredTypes.size();
        }
    }

    private Optional<PortType> getBoundType() {
        final var bound = m_lookupTable.get(m_boundTo);
        if (bound == null) {
            return Optional.empty();
        }
        if (bound.definesInputPorts()) {
            return Arrays.stream(bound.getInputPorts()).findFirst();
        }
        if (bound.definesOutputPorts()) {
            return Arrays.stream(bound.getOutputPorts()).findFirst();
        }
        return Optional.empty();
    }

}
