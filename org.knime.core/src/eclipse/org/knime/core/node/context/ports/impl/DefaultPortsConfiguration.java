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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.core.node.context.ports.PortGroupConfiguration;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortType;

/**
 * Class allowing to access the user defined node ports.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
class DefaultPortsConfiguration implements PortsConfiguration {

    /** Map storing the port groups. */
    protected final Map<String, PortGroupConfiguration> m_portGroups;

    /**
     * Constructor.
     *
     * @param portGroups the port groups map
     */
    protected DefaultPortsConfiguration(final Map<String, PortGroupConfiguration> portGroups) {
        m_portGroups = portGroups;
    }

    @Override
    public PortType[] getInputPorts() {
        return getPorts(PortGroupConfiguration::definesInputPorts, PortGroupConfiguration::getInputPorts);
    }

    @Override
    public PortType[] getOutputPorts() {
        return getPorts(PortGroupConfiguration::definesOutputPorts, PortGroupConfiguration::getOutputPorts);
    }

    private PortType[] getPorts(final Predicate<PortGroupConfiguration> portGroupFilter,
        final Function<PortGroupConfiguration, PortType[]> getPortsInGroup) {
        return m_portGroups.values().stream()//
            .filter(portGroupFilter)//
            .map(getPortsInGroup)//
            .flatMap(Stream::of)//
            .toArray(PortType[]::new);
    }

    @Override
    public List<String> getPortGroupNames() {
        return new ArrayList<>(m_portGroups.keySet());
    }

    @Override
    public Map<String, int[]> getInputPortLocation() {
        return getPortLocation(PortGroupConfiguration::definesInputPorts, pGrpCfg -> pGrpCfg.getInputPorts().length);
    }

    @Override
    public Map<String, int[]> getOutputPortLocation() {
        return getPortLocation(PortGroupConfiguration::definesOutputPorts, pGrpCfg -> pGrpCfg.getOutputPorts().length);

    }

    @SuppressWarnings("java:S4276") // more specialised functional interface possible
    private Map<String, int[]> getPortLocation(final Predicate<PortGroupConfiguration> selectPortConfig,
        final Function<PortGroupConfiguration, Integer> getNrPorts) {
        final Map<String, int[]> portLocation = new LinkedHashMap<>();
        final var position = new AtomicInteger();
        m_portGroups.entrySet().stream()//
            .filter(entry -> selectPortConfig.test(entry.getValue()))//
            .forEachOrdered(entry -> {
                var portGroupId = entry.getKey();
                var portGroupConfig = entry.getValue();
                final int nrPortsInGroup = getNrPorts.apply(portGroupConfig);
                if (nrPortsInGroup != 0) {
                    portLocation.put(portGroupId, IntStream.range(position.get() + 0, position.get() + nrPortsInGroup).toArray());
                }
                position.addAndGet(nrPortsInGroup);
            });//
        return portLocation;
    }

}
