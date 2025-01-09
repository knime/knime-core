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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.ExchangeablePortGroup;
import org.knime.core.node.context.ports.ExtendablePortGroup;
import org.knime.core.node.context.ports.ModifiablePortsConfiguration;
import org.knime.core.node.context.ports.PortGroupConfiguration;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortType;

/**
 * Class allowing to change input/output ports of a node.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class DefaultModifiablePortsConfiguration extends DefaultPortsConfiguration
    implements ModifiablePortsConfiguration {

    /**
     * Contains the identifiers of the port groups that cannot be modified via the user interface.
     *
     * Never null. Never contains null elements.
     */
    final Set<String> m_nonInteractivePortGroups;

    /**
     * Constructor.
     *
     * @param portGroups the port groups
     */
    public DefaultModifiablePortsConfiguration(final Map<String, PortGroupConfiguration> portGroups) {
        super(portGroups);
        m_nonInteractivePortGroups = Set.of();
    }

    /**
     * Constructor.
     * 
     * @param portGroups the port groups
     * @param nonInteractivePortGroups the identifiers (keys in portGroups map) of port groups that shall not be
     *            modifiable via the user interface. If null, all port groups are interactive. Null values are ignored.
     */
    public DefaultModifiablePortsConfiguration(final Map<String, PortGroupConfiguration> portGroups,
        final Set<String> nonInteractivePortGroups) {
        super(portGroups);
        // make sure the given set is not null and that it does not contain null elements
        m_nonInteractivePortGroups = nonInteractivePortGroups == null ? Set.of()
            : nonInteractivePortGroups.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public ModifiablePortsConfiguration copy() {
        final Map<String, PortGroupConfiguration> map = m_portGroups.entrySet().stream()//
            .collect(Collectors.toMap(//
                Map.Entry::getKey, //
                e -> e.getValue().copy(), //
                (u, v) -> {
                    throw new IllegalStateException(String.format("Duplicate key %s", u));
                }, //
                LinkedHashMap::new));
        map.values().stream()//
            .filter(TypeBoundExtendablePortGroup.class::isInstance)//
            .map(v -> (TypeBoundExtendablePortGroup)v)//
            .forEach(v -> v.setLookupTable(map));
        return new DefaultModifiablePortsConfiguration(map, Set.copyOf(m_nonInteractivePortGroups));
    }

    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_portGroups.forEach((s, c) -> c.saveSettingsTo(settings.addNodeSettings(s)));
    }

    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        for (var entry : m_portGroups.entrySet()) {
            if (settings.containsKey(entry.getKey())) {
                entry.getValue().loadSettingsFrom(settings.getNodeSettings(entry.getKey()));
            }
        }
    }

    @Override
    public Map<String, ExchangeablePortGroup> getExchangeablePorts() {
        return getPorts(ExchangeablePortGroup.class);
    }

    @Override
    public Map<String, ExtendablePortGroup> getExtendablePorts() {
        return getPorts(ExtendablePortGroup.class);
    }

    private <P extends PortGroupConfiguration> Map<String, P> getPorts(final Class<P> cls) {
        return m_portGroups.entrySet().stream()//
            .filter(e -> cls.isInstance(e.getValue()))//
            .collect(Collectors.toMap(//
                e -> e.getKey(), //
                e -> (cls.cast(e.getValue())), //
                (u, v) -> {
                    throw new IllegalStateException(String.format("Duplicate key %s", u));
                }, //
                LinkedHashMap::new)//
            );
    }

    @Override
    public PortGroupConfiguration getGroup(final String grpName) {
        if (m_portGroups.containsKey(grpName)) {
            return m_portGroups.get(grpName);
        }
        throw new NoSuchElementException("There is no group called \'" + grpName + "\'");
    }

    @Override
    public Map<Integer, Integer> mapInputPorts(final PortsConfiguration otherConfig) {
        return mapPorts(otherConfig, PortsConfiguration::getInputPorts, PortsConfiguration::getInputPortLocation);
    }

    @Override
    public Map<Integer, Integer> mapOutputPorts(final PortsConfiguration otherConfig) {
        return mapPorts(otherConfig, PortsConfiguration::getOutputPorts, PortsConfiguration::getOutputPortLocation);
    }

    /**
     * Map ports from this node to another node
     * 
     * @param otherConfig configuration of the other node
     * @param portAccess Obtain list of ports, see {@link PortsConfiguration#getInputPorts()}
     * @param locAccess Obtain list of port locations, see {@link PortsConfiguration#getInputPortLocation()}
     * @return Either identity, last port "removed" (i.e. mapped to -1) or entire port groups "removed". Indices are
     *         counting across the total number of ports, including the implicit flow variable port, if present.
     */
    @SuppressWarnings("java:S134") // deep nesting -- accepted, this is legacy code
    private Map<Integer, Integer> mapPorts(final PortsConfiguration otherConfig,
        final Function<PortsConfiguration, PortType[]> portAccess,
        final Function<PortsConfiguration, Map<String, int[]>> locAccess) {
        if (!isCompatible(otherConfig)) {
            throw new IllegalArgumentException("The port configurations are incompatible");
        }
        final var offset = 1; // to work around flow variable port
        final var portMapping = new HashMap<Integer, Integer>();
        final var ports = portAccess.apply(this);
        final var otherPorts = portAccess.apply(otherConfig);
        final var otherPortGroups = locAccess.apply(otherConfig);
        for (var portGroup : locAccess.apply(this).entrySet()) { // for each port group
            final var portGroupId = portGroup.getKey();
            final var portGroupLocations = portGroup.getValue(); // range of indices belonging to that port group
            if (!otherPortGroups.containsKey(portGroupId)) {
                // port group id/name no longer present in other -> set all indices as removed
                Arrays.stream(portGroupLocations).forEach(idx -> portMapping.put(idx + offset, -1));
            } else {
                final var otherPortGrpLoc = otherPortGroups.get(portGroupId);
                final var otherPortGrpLocLength = otherPortGrpLoc.length;
                for (var i = 0; i < portGroupLocations.length; i++) {
                    // this assumes only the last port in a group can be removed
                    if (i < otherPortGrpLocLength
                        && ports[portGroupLocations[i]].equals(otherPorts[otherPortGrpLoc[i]])) {
                        // identity
                        portMapping.put(portGroupLocations[i] + offset, otherPortGrpLoc[i] + offset);
                    } else { // i >= otherPortGrpLocLength, i.e. port has been removed in other
                        // has been removed
                        portMapping.put(portGroupLocations[i] + offset, -1);
                    }
                }
            }
        }

        // add additional entry for flow variable port (always implicitly present on native nodes).
        portMapping.put(0, 0);

        return portMapping;
    }

    /**
     * @param otherConfig
     */
    private boolean isCompatible(final PortsConfiguration otherConfig) {
        return getPortGroupNames().equals(otherConfig.getPortGroupNames());
    }

    @Override
    public boolean isInteractive(final String groupIdentifier) {
        return !m_nonInteractivePortGroups.contains(groupIdentifier);
    }
}
