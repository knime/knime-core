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
 *   10 Dec 2019 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.node.workflow.capture;

import java.util.List;
import java.util.Set;

import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @since 4.1 // TODO replace with 4.2 since tag
 */
public class WorkflowFragment {

    private final WorkflowManager m_wfm;

    private final Set<NodeIDSuffix> m_portObjectReferenceReaderNodes;

    private List<Port> m_inputPorts;

    private List<Port> m_outputPorts;

    /**
     * Creates a new instance.
     *
     * @param wfm the workflow manager representing the workflow fragment
     * @param inputPorts workflow fragment's input ports
     * @param outputPorts workflow fragment's output ports
     * @param portObjectReferenceReaderNodes relative node ids of nodes that reference port objects in another workflow
     */
    public WorkflowFragment(final WorkflowManager wfm, final List<Port> inputPorts, final List<Port> outputPorts,
        final Set<NodeIDSuffix> portObjectReferenceReaderNodes) {
        m_wfm = wfm;
        m_inputPorts = inputPorts;
        m_outputPorts = outputPorts;
        m_portObjectReferenceReaderNodes = portObjectReferenceReaderNodes;
    }

    /**
     * @return the workflow manager representing the fragment
     */
    public WorkflowManager getWorkflow() {
        return m_wfm;
    }

    /**
     * @return relative node ids of nodes that reference port objects in another workflow (TODO revisit)
     */
    public Set<NodeIDSuffix> getPortObjectReferenceReaderNodes() {
        return m_portObjectReferenceReaderNodes;
    }

    /**
     * @return workflow fragment's input ports
     */
    public List<Port> getInputPorts() {
        return m_inputPorts;
    }

    /**
     * @return workflow fragment's output ports
     */
    public List<Port> getOutputPorts() {
        return m_outputPorts;
    }

    /**
     * Determines type of a given port.
     *
     * @param p
     * @return type of the given port
     */
    public PortType getInPortType(final Port p) {
        return m_wfm.getNodeContainer(p.getNodeID()).getInPort(p.getIndex()).getPortType();
    }

    /**
     * Determines type of a given port.
     *
     * @param p
     * @return type of the given port
     */
    public PortType getOutPortType(final Port p) {
        return m_wfm.getNodeContainer(p.getNodeID()).getOutPort(p.getIndex()).getPortType();
    }

    /**
     * References/marks ports in the workflow fragment by node id and index.
     */
    public static class Port {
        private NodeID m_nodeId;

        private int m_idx;

        /**
         * Creates an new port marker.
         *
         * @param nodeId the node's id
         * @param idx port index
         */
        public Port(final NodeID nodeId, final int idx) {
            m_nodeId = nodeId;
            m_idx = idx;
        }

        /**
         * @return node id
         */
        public NodeID getNodeID() {
            return m_nodeId;
        }

        /**
         * @return port index
         */
        public int getIndex() {
            return m_idx;
        }
    }
}
