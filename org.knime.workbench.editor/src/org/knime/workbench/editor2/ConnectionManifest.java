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
 *   Apr 5, 2018 (loki): created
 */
package org.knime.workbench.editor2;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;

/**
 * The manifest is used in two different semantic ways in this codebase, depending on how it is constructed; it is
 * either a manifest of potential connections (like for the node in drag), or a manifest of existing connections (like
 * for a potential hit target node.)
 *
 * This class began life as an inner class of NodeSupplantDragListener but is required in both that class and the
 * SupplantationCommand class in seperate package, which is the reason for this class' placement in this package.
 *
 * @author loki der quaeler
 */
public class ConnectionManifest {
    private final HashMap<PortType, TreeSet<Integer>> m_portTypeCountMap;

    /**
     * The instance constructor used when creating an instance based on the potential connections of a node.
     *
     * @param node the node to be inspected for its in and out ports
     * @param wm the <code>WorkflowManager</code> in which the node sits
     * @param input whether we are interested in the inports (if this is <code>true</code>) or the outports
     */
    public ConnectionManifest(final NodeContainerUI node, final WorkflowManager wm, final boolean input) {
        final int portStartIndex = (node instanceof WorkflowManagerUI) ? 0 : 1;
        final int portCount = input ? node.getNrInPorts() : node.getNrOutPorts();

        m_portTypeCountMap = new HashMap<>();

        for (int i = portStartIndex; i < portCount; i++) {
            final PortType portType = input ? node.getInPort(i).getPortType() : node.getOutPort(i).getPortType();

            tallyForPort(portType, i);
        }
    }

    /**
     * The instance constructor used when creating an instance based on existing connections of a node.
     *
     * @param node the node to which the connections are attached
     * @param wm the <code>WorkflowManager</code> in which the node sits
     * @param existingConnections the set of existing connects attached to the node
     * @param input whether we are interested in the inports (if this is <code>true</code>) or the outports
     */
    public ConnectionManifest(final NodeContainerUI node, final WorkflowManager wm,
        final Set<ConnectionContainer> existingConnections, final boolean input) {
        m_portTypeCountMap = new HashMap<>();

        for (ConnectionContainer cc : existingConnections) {
            final PortType portType;
            final int port;

            if (input) {
                port = cc.getDestPort();
                portType = node.getInPort(port).getPortType();
            } else {
                port = cc.getSourcePort();
                portType = node.getOutPort(port).getPortType();
            }

            tallyForPort(portType, port);
        }
    }

    // Used only by the clone() operation; produces a clone which clones the value collection instances
    private ConnectionManifest(final ConnectionManifest original) {
        m_portTypeCountMap = new HashMap<>();

        for (Map.Entry<PortType, TreeSet<Integer>> me : original.getCountEntries()) {
            final TreeSet<Integer> newSet = new TreeSet<>();

            newSet.addAll(me.getValue());
            m_portTypeCountMap.put(me.getKey(), newSet);
        }
    }

    private void tallyForPort(final PortType portType, final int port) {
        TreeSet<Integer> portSet = m_portTypeCountMap.computeIfAbsent(portType, k -> new TreeSet<>());

        portSet.add(new Integer(port));
    }

    // Clearly not thread-safe
    private Set<Map.Entry<PortType, TreeSet<Integer>>> getCountEntries() {
        return m_portTypeCountMap.entrySet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionManifest clone() {
        return new ConnectionManifest(this);
    }

    /**
     * This determines which this instance is a compatible superset of the argument instance.
     *
     * This method is not thread-safe.
     *
     * @param cm an instance of ConnectionManifest for comparison
     * @return <code>true</code> if this instance is a port-compatible super set of the argument instance
     */
    public boolean isCompatibleSuperSetOf(final ConnectionManifest cm) {
        for (Map.Entry<PortType, TreeSet<Integer>> me : cm.getCountEntries()) {
            final Set<Integer> portSet = m_portTypeCountMap.get(me.getKey());

            if ((portSet == null) || (portSet.size() < me.getValue().size())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Used to inquire as to whether this instance has support for the specified port type.
     *
     * This method is not thread-safe.
     *
     * @param portType the <code>PortType</code> instance for which we're querying
     * @return <code>true</code> if this manifest ever supported the port type
     */
    public boolean describesSupportForPortType(final PortType portType) {
        return m_portTypeCountMap.containsKey(portType);
    }

    /**
     * Takes the next available port, possibly making it not returnable on subsequent calls, for the given port type.
     *
     * This method is not thread-safe.
     *
     * @param portType the type of port desired
     * @param makeUnavailable if this is <code>true</code>, then the port being returned will not be vended again
     * @return the lowest available port for the given portType or -1 if none exists
     */
    public int consumePortForPortType(final PortType portType, final boolean makeUnavailable) {
        final TreeSet<Integer> portSet = m_portTypeCountMap.get(portType);

        if ((portSet != null) && (!portSet.isEmpty())) {
            final Integer port = makeUnavailable ? portSet.pollFirst() : portSet.first();

            return port.intValue();
        }

        return -1;
    }
}
