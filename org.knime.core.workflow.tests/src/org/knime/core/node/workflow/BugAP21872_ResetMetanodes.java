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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.core.node.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BugAP21872_ResetMetanodes extends WorkflowTestCase {

    private NodeID m_sourceNode;

    private NodeID m_throughSucessor;

    private NodeID m_viaSuccessor;

    private NodeID m_metaNode;

    private NodeID m_baseId;

    @BeforeEach
    public void setUp() throws Exception {
        m_baseId = loadAndSetWorkflow();
        m_sourceNode = new NodeID(m_baseId, 1);
        m_metaNode = new NodeID(m_baseId, 6);

        // successor of source via connections "through" the metanode
        m_throughSucessor = new NodeID(m_baseId, 4);
        // successor of source "via" metanode (disregarding the workflow inside the metanode, as if the metanode were
        //  a simple single node.
        m_viaSuccessor = new NodeID(m_baseId, 5);
        executeAllAndWait();

        getManager().resetAndConfigureNode(m_sourceNode, true);
    }

    @Test
    public void testSuccessorsAreReset() throws Exception {
        assertConfigured(m_throughSucessor, m_viaSuccessor);
    }

    /**
     * Nothing should have changed here with this ticket
     */
    @Test
    public void testAllInsideMetanodeAreReset() throws Exception {
        var metaNodeWfm = (WorkflowManager)getManager().getNodeContainer(m_metaNode);
        var connectedToWfInAndOut = metaNodeWfm.getNodeContainer(m_metaNode.createChild(2));
        var connectedtoWfOutWithSuccessorOutside = metaNodeWfm.getNodeContainer(m_metaNode.createChild(3));
        var connectedtoWfOutWithoutSuccessorOutside = metaNodeWfm.getNodeContainer(m_metaNode.createChild(4));
        assertConfigured(connectedToWfInAndOut, connectedtoWfOutWithSuccessorOutside,
            connectedtoWfOutWithoutSuccessorOutside);
    }

    @Test
    public void checkMetaOutState() throws Exception {
        checkMetaOutState(m_metaNode, 0, InternalNodeContainerState.CONFIGURED);
        checkMetaOutState(m_metaNode, 1, InternalNodeContainerState.CONFIGURED);
        checkMetaOutState(m_metaNode, 2, InternalNodeContainerState.CONFIGURED);
    }

    private void assertConfigured(final NodeID... nodes) throws Exception {
        for (NodeID node : nodes) {
            checkState(node, InternalNodeContainerState.CONFIGURED);
        }
    }

    private void assertConfigured(final NodeContainer... nodes) throws Exception {
        for (NodeContainer node : nodes) {
            checkState(node, InternalNodeContainerState.CONFIGURED);
        }
    }

}