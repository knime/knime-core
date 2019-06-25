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
 *   Jun 14, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.node.workflow;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.workflow.NodeMessage.Type;

/**
 * Simple test ensuring that after executing a component containing nodes with unconnected inputs a proper error message
 * is given to the user.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public class TestSubnode_ErrorMessages extends WorkflowTestCase {

    private NodeID m_oneUnconnectedOutputPort;

    private NodeID m_twoUnconnectedOneConnectedOutputPort;

    private NodeID m_threeUnconnectedOutputPorts;

    private NodeID m_oneUnconnectedInternalNode;

    private NodeID m_threeUnconnectedOneConnectedInternalNode;

    private NodeID m_failingInnerMetaNode;

    private NodeID m_failingMetaNode;

    private NodeID m_insideMetaNodeFailingComponent;

    private NodeID m_allFine;

    /**
     * Setup the workflow and initialize the node ids.
     *
     * @throws Exception - If something goes wrong while loading the workflow
     */
    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_oneUnconnectedOutputPort = new NodeID(baseID, 7);
        m_twoUnconnectedOneConnectedOutputPort = new NodeID(baseID, 10);
        m_threeUnconnectedOutputPorts = new NodeID(baseID, 11);
        m_oneUnconnectedInternalNode = new NodeID(baseID, 9);
        m_threeUnconnectedOneConnectedInternalNode = new NodeID(baseID, 8);
        m_failingInnerMetaNode = new NodeID(baseID, 17);
        m_failingMetaNode = new NodeID(getManager()
            .getNodeContainer(m_failingInnerMetaNode, SubNodeContainer.class, true).getWorkflowManager().getID(), 12);
        m_insideMetaNodeFailingComponent =
            new NodeID(getManager().getNodeContainer(m_failingInnerMetaNode, SubNodeContainer.class, true)
                .getWorkflowManager().getNodeContainer(m_failingMetaNode, WorkflowManager.class, true).getID(), 11);

        m_allFine = new NodeID(baseID, 15);
    }

    /**
     * Test that all messages are correct and all nodes have the proper execution status.
     *
     * @throws Exception - If something goes wrong during execution or state checks
     */
    @Test
    public void testMessages() throws Exception {
//        executeAllAndWait();
//
//        // ensure that unconnected subnodecontainers failed
//        ensureSubnodeContainerFailed(m_oneUnconnectedOutputPort, "Unconnected input port(s) for nodes:\n" + //
//            "\t- Component Output 0:7:0:9");
//        ensureSubnodeContainerFailed(m_twoUnconnectedOneConnectedOutputPort, "Unconnected input port(s) for nodes:" + //
//            "\n\t- Component Output 0:10:0:10");
//        ensureSubnodeContainerFailed(m_threeUnconnectedOutputPorts, "Unconnected input port(s) for nodes:\n" + //
//            "\t- Component Output 0:11:0:10");
//        ensureSubnodeContainerFailed(m_oneUnconnectedInternalNode, "Unconnected input port(s) for nodes:\n" + //
//            "\t- Column Filter 0:9:0:4");
//        ensureSubnodeContainerFailed(m_threeUnconnectedOneConnectedInternalNode,
//            "Unconnected input port(s) for nodes:\n" + //
//                "\t- Column Filter 0:8:0:3\n" + //
//                "\t- Column Filter 0:8:0:4\n" + //
//                "\t- Column Filter 0:8:0:10");
//
//        ensureSubnodeContainerFailed(m_failingInnerMetaNode, "Unconnected input port(s) for nodes:\n" + //
//            "\t- Component Output 0:17:0:10\n\n" + //
//            "Error during execution:\n" + //
//            "\t- Metanode 0:17:0:12");
//
//        checkState(m_failingMetaNode, InternalNodeContainerState.IDLE);
//
//        ensureSubnodeContainerFailed(
//            getManager().getNodeContainer(m_failingInnerMetaNode, SubNodeContainer.class, true).getWorkflowManager()
//                .getNodeContainer(m_failingMetaNode, WorkflowManager.class, true),
//            m_insideMetaNodeFailingComponent, "Unconnected input port(s) for nodes:\n" + //
//                "\t- Column Filter 0:17:0:12:11:0:9\n" + //
//                "\t- Component Output 0:17:0:12:11:0:13\n\n" + //
//                "Error during execution:\n" + //
//                "\t- Row Filter 0:17:0:12:11:0:7 : No row filter specified");
//
//        // ensure that the properly configured subnodecontainer was executed without any errors
//        checkState(m_allFine, InternalNodeContainerState.EXECUTED);
    }

    private void ensureSubnodeContainerFailed(final NodeID subNodeContainerId, final String msg) throws Exception {
        ensureSubnodeContainerFailed(getManager(), subNodeContainerId, msg);
    }

    private void ensureSubnodeContainerFailed(final WorkflowManager manager, final NodeID subNodeContainerId,
        final String msg) throws Exception {
        final SubNodeContainer subNC = manager.getNodeContainer(subNodeContainerId, SubNodeContainer.class, true);
        checkState(subNC, InternalNodeContainerState.IDLE);
        assertEquals(Type.ERROR, subNC.getNodeMessage().getMessageType());
        assertEquals(msg, subNC.getNodeMessage().getMessage());
    }

}