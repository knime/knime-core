/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Mar 13, 2014 ("Patrick Winter"): created
 */
package org.knime.core.node.workflow;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.port.MetaPortInfo;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;

/**
 *
 * @author "Patrick Winter", University of Konstanz
 */
public class Bug4890_reconfigureSubNode extends WorkflowTestCase {

    private NodeID m_subNode;
    private NodeID m_varSource;
    private NodeID m_varTarget;
    private NodeID m_diffChecker1;
    private NodeID m_diffChecker2;
    private NodeID m_diffChecker3;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_subNode = new NodeID(baseID, 16);
        m_varSource = new NodeID(baseID, 11);
        m_varTarget = new NodeID(baseID, 12);
        m_diffChecker1 = new NodeID(baseID, 5);
        m_diffChecker2 = new NodeID(baseID, 6);
        m_diffChecker3 = new NodeID(baseID, 9);
    }

    /**
     * @throws Exception If the test fails
     */
    @Test
    public void testExecuteFlow() throws Exception {
        executeAllAndWait();
        checkState(m_diffChecker1, InternalNodeContainerState.EXECUTED);
        checkState(m_diffChecker2, InternalNodeContainerState.EXECUTED);
        // Switch input ports
        reset(m_subNode);
        MetaPortInfo[] inPorts = getManager().getSubnodeInputPortInfo(m_subNode);
        inPorts[1] = MetaPortInfo.builder(inPorts[1]).setNewIndex(2).build();
        inPorts[2] = MetaPortInfo.builder(inPorts[2]).setNewIndex(1).build();
        getManager().changeSubNodeInputPorts(m_subNode, new MetaPortInfo[]{inPorts[0], inPorts[2], inPorts[1]});
        executeAllAndWait();
        checkState(m_diffChecker1, InternalNodeContainerState.EXECUTED);
        checkState(m_diffChecker2, InternalNodeContainerState.EXECUTED);
        // Switch output ports
        reset(m_subNode);
        MetaPortInfo[] outPorts = getManager().getSubnodeOutputPortInfo(m_subNode);
        outPorts[1] = MetaPortInfo.builder(outPorts[1]).setNewIndex(2).build();
        outPorts[2] = MetaPortInfo.builder(outPorts[2]).setNewIndex(1).build();
        getManager().changeSubNodeOutputPorts(m_subNode, new MetaPortInfo[]{outPorts[0], outPorts[2], outPorts[1]});
        executeAllAndWait();
        checkState(m_diffChecker1, InternalNodeContainerState.EXECUTED);
        checkState(m_diffChecker2, InternalNodeContainerState.EXECUTED);
        // Remove all connections, change ports to variable ports and connect var nodes
        SubNodeContainer subNode = (SubNodeContainer)getManager().getNodeContainer(m_subNode);
        WorkflowManager subWorkflow = subNode.getWorkflowManager();
        List<ConnectionContainer> connections = new ArrayList<ConnectionContainer>();
        connections.addAll(getManager().getWorkflow().getConnectionsByDest(m_subNode));
        connections.addAll(getManager().getWorkflow().getConnectionsBySource(m_subNode));
        for (ConnectionContainer con : connections) {
            getManager().removeConnection(con);
        }
        List<NodeContainer> nodes = new ArrayList<NodeContainer>();
        nodes.addAll(subWorkflow.getNodeContainers());
        for (NodeContainer node : nodes) {
            NodeID id = node.getID();
            if (!id.equals(subNode.getVirtualInNodeID()) && !id.equals(subNode.getVirtualOutNodeID())) {
                subWorkflow.removeNode(id);
            }
        }
        MetaPortInfo[] ports = new MetaPortInfo[2];
        ports[0] = MetaPortInfo.builder().setPortType(FlowVariablePortObject.TYPE).setOldIndex(0).build();
        ports[1] = MetaPortInfo.builder().setPortType(FlowVariablePortObject.TYPE).setOldIndex(1).build();
        getManager().changeSubNodeInputPorts(m_subNode, ports);
        getManager().changeSubNodeOutputPorts(m_subNode, ports);
        getManager().addConnection(m_varSource, 1, m_subNode, 1);
        getManager().addConnection(m_subNode, 1, m_varTarget, 1);
        subWorkflow.addConnection(subNode.getVirtualInNodeID(), 1, subNode.getVirtualOutNodeID(), 1);
        executeAllAndWait();
        checkState(m_diffChecker3, InternalNodeContainerState.EXECUTED);
    }

}
