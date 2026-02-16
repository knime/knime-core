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
 *   Feb 14, 2026 (Paul Bärnreuther): created
 */
package org.knime.core.node.workflow;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.workflow.virtual.subnode.VirtualSubNodeInputNodeFactory;

/**
 * Test for {@link NativeNodeContainer#getFlowObjectStackForDialog()} with
 * VirtualSubNodeInput nodes. Verifies that VirtualSubNodeInput nodes use the
 * parent SubNodeContainer's FlowObjectStack when opening dialogs, rather than
 * their own empty stack.
 *
 * @author Paul Bärnreuther
 */
public class EnhUIEXT3328_VirtualSubNodeInputFlowStack extends WorkflowTestCase {

    // Nodes for component 1
    private NodeID m_snc1;
    private NodeID m_virtualSubNodeInput1;

    @BeforeEach
    public void setUp() throws Exception {
        final var baseID = loadAndSetWorkflow();
        m_snc1 = new NodeID(baseID, 4); // SubNodeContainer
        m_virtualSubNodeInput1 = new NodeID(new NodeID(m_snc1, 0), 1); // VirtualSubNodeInput inside the component
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        closeWorkflow();
    }

    /**
     * Tests that VirtualSubNodeInput nodes use the parent SubNodeContainer's
     * FlowObjectStack for dialogs. This is necessary because the
     * VirtualSubNodeInput dialog needs to show flow variables that are available in
     * the component's scope (from outside the component), not the node's own empty
     * flow stack.
     */
    @Test
    public void testVirtualSubNodeInputUsesParentFlowStackForDialog() {
        final var snc = (SubNodeContainer) getManager().getNodeContainer(m_snc1);
        final var componentWfm = snc.getWorkflowManager();

        // Find the VirtualSubNodeInput node inside the component
        final var virtualSubNodeInput = (NativeNodeContainer) componentWfm.getNodeContainer(m_virtualSubNodeInput1);

        // Verify it's actually a VirtualSubNodeInput
        assertTrue(VirtualSubNodeInputNodeFactory.class.equals(virtualSubNodeInput.getNode().getFactory().getClass()),
                "Node should be a VirtualSubNodeInput");

        final var parentFlowStack = snc.getFlowObjectStack();
        final var nodeOwnFlowStack = virtualSubNodeInput.getFlowObjectStack();
        final var dialogFlowStack = virtualSubNodeInput.getFlowObjectStackForDialog();

        // VirtualSubNodeInput's own flow stack should be different from parent's
        // (it's empty or contains only what's inside the component)
        assertNotSame(nodeOwnFlowStack, parentFlowStack, "Node's own flow stack should not be the same as parent's");

        // Dialog flow stack should be the parent's flow stack
        assertSame(parentFlowStack, dialogFlowStack,
                "Dialog flow stack should be the parent SubNodeContainer's flow stack");

        // Verify parent flow stack has workflow variables
        assertFalse(parentFlowStack.getAllAvailableFlowVariables().isEmpty(),
                "Parent flow stack should have variables");
    }

    /**
     * Tests that regular nodes (not VirtualSubNodeInput) use their own
     * FlowObjectStack for dialogs.
     */
    @Test
    public void testRegularNodeUsesOwnFlowStackForDialog() {
        // Node 0 is a regular node (TableCreator) that creates flow variables
        final var regularNode = (NativeNodeContainer) getManager()
                .getNodeContainer(new NodeID(getManager().getID(), 1));

        final var nodeFlowStack = regularNode.getFlowObjectStack();
        final var dialogFlowStack = regularNode.getFlowObjectStackForDialog();

        assertSame(nodeFlowStack, dialogFlowStack, "Regular node's dialog flow stack should be its own flow stack");
    }
}
