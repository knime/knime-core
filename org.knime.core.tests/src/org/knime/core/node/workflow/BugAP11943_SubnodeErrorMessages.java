/*
 * ------------------------------------------------------------------------
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
 */
package org.knime.core.node.workflow;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.IDLE;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class BugAP11943_SubnodeErrorMessages extends WorkflowTestCase {

    private NodeID m_dataGenerator_1;
    private NodeID m_subnode_4;
    private NodeID m_subnode_6;
    private NodeID m_subnode_8;
    private NodeID m_subnode_9;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_dataGenerator_1 = baseID.createChild(1);
        m_subnode_4 = baseID.createChild(4);
        m_subnode_6 = baseID.createChild(6);
        m_subnode_8 = baseID.createChild(8);
        m_subnode_9 = baseID.createChild(9);
    }

    /** Subnode contains node that fails during execution. Regular failure case. */
    @Test
    public void testRegularNodeFailureMessage() throws Exception {
        checkState(m_dataGenerator_1, CONFIGURED); // no input data to subnode -- expected to be red (until we fix that)
        NodeContainer subContainer = findNodeContainer(m_subnode_4);
        checkState(subContainer, IDLE);
        assertThat("Message type when no input is available",
            subContainer.getNodeMessage().getMessageType(), is(NodeMessage.Type.WARNING));
        assertThat("Message text when no input is available",
            subContainer.getNodeMessage().getMessage(), containsString("does not have input data"));
        executeAndWait(m_subnode_4);
        checkState(subContainer, CONFIGURED);
        assertThat("Message type after attempted execute",
            subContainer.getNodeMessage().getMessageType(), is(NodeMessage.Type.ERROR));
        assertThat("Message text when no input is available",
            subContainer.getNodeMessage().getMessage(), containsString("fails on each execution"));
    }

    /** Subnode contains unconnected nodes. */
    @Test
    public void testUnconnectedNodeFailureMessage() throws Exception {
        NodeContainer subContainer = findNodeContainer(m_subnode_6);
        assertThat("Message type when no input is available",
            subContainer.getNodeMessage().getMessageType(), is(NodeMessage.Type.WARNING));
        assertThat("Message text when no input is available",
            subContainer.getNodeMessage().getMessage(), containsString("unconnected nodes"));
        executeAndWait(m_subnode_6);
        checkState(subContainer, IDLE);
        assertThat("Message type after attempted execute",
            subContainer.getNodeMessage().getMessageType(), is(NodeMessage.Type.ERROR));
        assertThat("Message text when no input is available",
            subContainer.getNodeMessage().getMessage(), containsString("unconnected nodes"));
    }

    /** Subnode contains metanode with unconnected node. */
    @Test
    public void testUnconnectedNodeInMetanodeFailureMessage() throws Exception {
        NodeContainer subContainer = findNodeContainer(m_subnode_8);
        assertThat("Message type when no input is available",
            subContainer.getNodeMessage().getMessageType(), is(NodeMessage.Type.WARNING));
        assertThat("Message text when no input is available",
            subContainer.getNodeMessage().getMessage(), containsString("unconnected node"));
        executeAndWait(m_subnode_8);
        checkState(subContainer, IDLE);
        assertThat("Message type after attempted execute",
            subContainer.getNodeMessage().getMessageType(), is(NodeMessage.Type.ERROR));
        assertThat("Message text when no input is available",
            subContainer.getNodeMessage().getMessage(), containsString("unconnected node"));
    }

    /** Subnode contains node that hasn't been configured and fails during #configure.
     * Expected to see that message on attempted execution. */
    @Test
    public void testUnconfiguredNodesMessage() throws Exception {
        NodeContainer subContainer = findNodeContainer(m_subnode_9);
        assertThat("Message type when no input is available",
            subContainer.getNodeMessage().getMessageType(), is(NodeMessage.Type.WARNING));
        assertThat("Message text when no input is available",
            subContainer.getNodeMessage().getMessage(), containsString("does not have input data"));
        executeAndWait(m_subnode_9);
        checkState(subContainer, IDLE);
        assertThat("Message type after attempted execute",
            subContainer.getNodeMessage().getMessageType(), is(NodeMessage.Type.ERROR));
        assertThat("Message text when no input is available",
            subContainer.getNodeMessage().getMessage(), containsString("Validation failed")); // has a lot more text
    }

    /** {@inheritDoc} */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

}
