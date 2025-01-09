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
 *   Oct 8, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.node.context.ports;

import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;

import org.knime.core.node.context.DeepCopy;
import org.knime.core.node.context.NodeSettingsPersistable;

/**
 * Interface defining a node's port configuration.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @noreference This interface is not intended to be referenced by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ModifiablePortsConfiguration
    extends PortsConfiguration, DeepCopy<ModifiablePortsConfiguration>, NodeSettingsPersistable {

    /**
     * Returns a map of all exchangeable ports.
     *
     * @return list of all exchangeable ports
     */
    Map<String, ExchangeablePortGroup> getExchangeablePorts();

    /**
     * Returns a map of all extendable ports.
     *
     * @return list of all extendable ports
     */
    Map<String, ExtendablePortGroup> getExtendablePorts();

    /**
     * Returns the port group configuration for the given group name
     *
     * @param grpName the group name
     * @return the port group configuration for the given group name
     * @throws NoSuchElementException If there is no configuration for the given group name
     */
    PortGroupConfiguration getGroup(final String grpName);

    /**
     * Maps the input ports of this instance onto the input ports of the other config.
     *
     * @param otherConfig the config to map onto
     * @return mapping from input port indices to the input port indices of the other config
     * @throws IllegalArgumentException If the configs specify different port configurations
     */
    Map<Integer, Integer> mapInputPorts(final PortsConfiguration otherConfig);

    /**
     * Maps the output ports of this instance onto the output ports of the other config. Note that the port indices
     * start with 1 and not 0. Ports that went missing have to map to -1.
     *
     * @param otherConfig the config to map onto
     * @return mapping from output port indices to the output port indices of the other config
     * @throws IllegalArgumentException If the configs specify different port configurations
     */
    Map<Integer, Integer> mapOutputPorts(final PortsConfiguration otherConfig);

    /**
     * Whether the port group can be modified through the user interface. For instance, the Call Workflow Service node
     * and the Workflow Executor node adjust their input and output ports according to the callee's workflow parameters.
     * The user is not allowed to configure the input and output ports manually in order to make sure they are always in
     * sync with the callee workflow.
     * 
     * @param groupIdentifier name of the port group to test
     * @return true if the port group can be modified by the user via the user interface. False if the port group can
     *         only programmatically be modified.
     */
    default boolean isInteractive(final String groupIdentifier) {
        return true;
    }

    /**
     * returned array does not include position for implicit flow variable port
     * 
     * @param isInputSide Whether to consider input or output ports
     * @return Port group names/IDs repeated such that lookup at index {@code i} yields the port group name/ID that the
     *         {@code i}-th port belongs to. Does not include position for implicit flow variable port.
     */
    default String[] getPortGroupsPerIndex(final boolean isInputSide) {
        var locations = isInputSide ? this.getInputPortLocation() : this.getOutputPortLocation();
        var totalNumberOfPorts = isInputSide ? (this.getInputPorts().length) : (this.getOutputPorts().length);
        var portGroupNamePerIndex = new String[totalNumberOfPorts];
        locations.forEach((portGroupName, indicesInPortGroup) -> Arrays.stream(indicesInPortGroup)
            .forEach(i -> portGroupNamePerIndex[i] = portGroupName));
        return portGroupNamePerIndex;
    }

    /**
     * @see ModifiablePortsConfiguration#getPortIndexWithinGroup(String[], int)
     */
    @SuppressWarnings({"java:S1176", "MissingJavadoc", "javadoc"}) // don't repeat javadoc for override
    default int getPortIndexWithinGroup(final int totalPortIndex, final boolean isInputSide) {
        var portIndexToPortGroupMap = getPortGroupsPerIndex(isInputSide);
        return getPortIndexWithinGroup(portIndexToPortGroupMap, totalPortIndex);
    }

    /**
     * Map a total port index (i.e. counting over all ports of the node) to the index within its port group.
     * <p>
     * For example, consider port groups [[p0, p1, p2], [p3, p4, p5]]. Then getPortIndexWithinGroup(4) = 1
     * 
     * @param portIndexToPortGroupMap see {@link ModifiablePortsConfiguration#getPortGroupsPerIndex(boolean)}
     * @param totalPortIndex index over all ports on this side, including implicit flow variable port
     * @return The index of that port within its port group
     */
    static int getPortIndexWithinGroup(final String[] portIndexToPortGroupMap, final int totalPortIndex) {
        var portGroupName = portIndexToPortGroupMap[totalPortIndex - 1];
        var portIndexWithinGroup = 0;
        var previousPortGroupName = portGroupName;
        while (totalPortIndex - 1 - portIndexWithinGroup > 0 && portGroupName.equals(previousPortGroupName)) {
            previousPortGroupName = portIndexToPortGroupMap[totalPortIndex - 2 - portIndexWithinGroup];
            if (previousPortGroupName.equals(portGroupName)) {
                portIndexWithinGroup++;
            }
        }
        return portIndexWithinGroup;
    }

}
