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
import java.util.Map.Entry;
import java.util.NoSuchElementException;
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
     * Constructor.
     *
     * @param portGroups the port groups
     */
    public DefaultModifiablePortsConfiguration(final Map<String, PortGroupConfiguration> portGroups) {
        super(portGroups);
    }

    @Override
    public ModifiablePortsConfiguration copy() {
        return new DefaultModifiablePortsConfiguration(m_portGroups.entrySet().stream()//
            .collect(Collectors.toMap(//
                e -> e.getKey(), //
                e -> e.getValue().copy(), //
                (u, v) -> {
                    throw new IllegalStateException(String.format("Duplicate key %s", u));
                }, //
                LinkedHashMap::new)));
    }

    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_portGroups.forEach((s, c) -> c.saveSettingsTo(settings.addNodeSettings(s)));
    }

    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        for (Entry<String, PortGroupConfiguration> entry : m_portGroups.entrySet()) {
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

    private Map<Integer, Integer> mapPorts(final PortsConfiguration otherConfig,
        final Function<PortsConfiguration, PortType[]> portAccess,
        final Function<PortsConfiguration, Map<String, int[]>> locAccess) {
        if (!isComplatible(otherConfig)) {
            throw new IllegalArgumentException("The port configurations are incompatible");
        }
        final int offset = 1;
        final HashMap<Integer, Integer> portMapping = new HashMap<>();
        final PortType[] ports = portAccess.apply(this);
        final PortType[] otherPorts = portAccess.apply(otherConfig);
        final Map<String, int[]> otherPortLoc = locAccess.apply(otherConfig);
        for (Entry<String, int[]> entry : locAccess.apply(this).entrySet()) {
            if (otherPortLoc.containsKey(entry.getKey())) {
                final int[] portGrpLoc = entry.getValue();
                final int[] otherPortGrpLoc = otherPortLoc.get(entry.getKey());
                final int otherPortGrpLocLength = otherPortGrpLoc.length;
                for (int i = 0; i < portGrpLoc.length; i++) {
                    // we don't supported changing of ports within a group
                    if (i < otherPortGrpLocLength && ports[portGrpLoc[i]].equals(otherPorts[otherPortGrpLoc[i]])) {
                        portMapping.put(portGrpLoc[i] + offset, otherPortGrpLoc[i] + offset);
                    } else {
                        portMapping.put(portGrpLoc[i] + offset, -1);
                    }
                }
            } else {
                Arrays.stream(entry.getValue()).forEach(idx -> portMapping.put(idx + offset, -1));
            }
        }

        // add additional entry for flow variable connections
        portMapping.put(0, 0);

        return portMapping;
    }

    /**
     * @param otherConfig
     */
    private boolean isComplatible(final PortsConfiguration otherConfig) {
        return getPortGroupNames().equals(otherConfig.getPortGroupNames());
    }
}
