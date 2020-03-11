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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * One subnode with variable-defining nodes that are exported to the outside. One branch can be switched inactive,
 * whereby the value is then not expected to be seen downstream.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class TestSubnode_VariableScopeDuringExecution extends WorkflowTestCase {

    private NodeID m_subNode6;
    private NodeID m_javaEdit_out_1;
    private NodeID m_javaEdit_out_7;
    private NodeID m_javaEdit_out_8;
    private NodeID m_javaEdit_in_5;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_subNode6 = new NodeID(baseID, 6);
        m_javaEdit_out_1 = new NodeID(baseID, 1);
        m_javaEdit_out_8 = new NodeID(baseID, 8);
        m_javaEdit_out_7 = new NodeID(baseID, 7);
        NodeID subnodeWFM = new NodeID(m_subNode6, 0);
        m_javaEdit_in_5 = new NodeID(subnodeWFM, 5);
    }

    private String getVariableValue(final String varName) throws Exception {
        final NativeNodeContainer nnc = (NativeNodeContainer)getManager().getNodeContainer(m_javaEdit_out_7);
        FlowObjectStack flowObjectStack = nnc.getOutPort(0).getFlowObjectStack();
        FlowVariable variable = flowObjectStack.getAvailableFlowVariables().get(varName);
        return variable != null ? variable.getStringValue() : null;
    }

    /** Just loading and making sure all is green with expected output.
     * @throws Exception ... */
    @Test
    public void testVariablesAfterLoad() throws Exception {
        checkState(m_javaEdit_out_7, InternalNodeContainerState.IDLE);
        checkState(m_javaEdit_out_8, InternalNodeContainerState.IDLE);
//        SubNodeContainer subNC = getManager().getNodeContainer(m_subNode6, SubNodeContainer.class, true);

        // until we know better the Subnode is not configurable if the input is not populated
        checkState(m_subNode6, InternalNodeContainerState.IDLE);

        Assert.assertFalse(((NativeNodeContainer)findNodeContainer(m_javaEdit_out_8)).isInactive());
        Assert.assertEquals("outside-variable", getVariableValue("outside-variable"));
        // subnode not configured - no variables populated
        Assert.assertEquals(null, getVariableValue("inside-variable-active"));
        Assert.assertEquals(null, getVariableValue("inside-variable-inactive"));

    }

    /** Execute predecessor of subnode will cause configuration of subnode. Ideally this would also populate
     * flow variables down stream but currently it doesn't. Problem is that upon execution some nodes turn
     * inactive and the downstream nodes would still see the variables produced during configured.
     * @throws Exception ... */
    @Test
    public void testVariablesAfterExecuteUpstream() throws Exception {
        executeAndWait(m_javaEdit_out_1);

        checkState(m_javaEdit_out_7, InternalNodeContainerState.CONFIGURED);
        checkState(m_javaEdit_out_8, InternalNodeContainerState.CONFIGURED);
        checkState(m_subNode6, InternalNodeContainerState.CONFIGURED);

        Assert.assertFalse(((NativeNodeContainer)findNodeContainer(m_javaEdit_out_8)).isInactive());
        // flow variables are not propagated during #configure of subnode
        // expected output is different once we fix that...
        Assert.assertEquals("outside-variable", getVariableValue("outside-variable"));
        Assert.assertEquals(null, getVariableValue("inside-variable-active"));
        Assert.assertEquals(null, getVariableValue("inside-variable-inactive"));
    }

    /** Fully execute workflow. It should populate variables and set the node at the first subnode output inactive.
     * @throws Exception ... */
    @Test
    public void testVariablesAfterFullExecute() throws Exception {
        executeAllAndWait();

        Assert.assertTrue(((NativeNodeContainer)findNodeContainer(m_javaEdit_out_8)).isInactive());
        checkState(m_javaEdit_out_7, InternalNodeContainerState.EXECUTED);
        checkState(m_javaEdit_out_8, InternalNodeContainerState.EXECUTED);
        checkState(m_subNode6, InternalNodeContainerState.EXECUTED);
        // modifying node is now inactive - unmodified value is expected
        Assert.assertEquals("outside-variable", getVariableValue("outside-variable"));
        Assert.assertEquals("inside-variable-active", getVariableValue("inside-variable-active"));
        Assert.assertEquals(null, getVariableValue("inside-variable-inactive"));
    }

    /** Fully execute workflow, then re-wire to disable the inactive switch
     * @throws Exception ... */
    @Test
    public void testVariablesAfterFullExecuteThenRewire() throws Exception {
        executeAllAndWait();
        deleteConnection(m_javaEdit_in_5, 1);
        checkState(m_javaEdit_out_7, InternalNodeContainerState.CONFIGURED);
        checkState(m_javaEdit_out_8, InternalNodeContainerState.CONFIGURED);
        checkState(m_subNode6, InternalNodeContainerState.CONFIGURED);

        executeAllAndWait();
        Assert.assertFalse(((NativeNodeContainer)findNodeContainer(m_javaEdit_out_8)).isInactive());
        checkState(m_javaEdit_out_7, InternalNodeContainerState.EXECUTED);
        checkState(m_javaEdit_out_8, InternalNodeContainerState.EXECUTED);
        checkState(m_subNode6, InternalNodeContainerState.EXECUTED);
        // modifying node is now inactive - unmodified value is expected
        Assert.assertEquals("outside-variable-modified", getVariableValue("outside-variable"));
        Assert.assertEquals("inside-variable-active", getVariableValue("inside-variable-active"));
        Assert.assertEquals("inside-variable-inactive", getVariableValue("inside-variable-inactive"));
    }

//    /** Full execute of workflow, then reset start node. This should bring the workflow into the state
//     * after loading. Currently we do not reset the flow var stack during 'reset' why this test is disabled
//     * @throws Exception ... */
//    public void testVariablesAfterFullExecuteAndReset() throws Exception {
//        executeAllAndWait();
//        reset(m_javaEdit_out_1); // fall back to where it was after load
//
//        Assert.assertFalse(((NativeNodeContainer)findNodeContainer(m_javaEdit_out_8)).isInactive());
//
//        // this currently fails as a "reset" does not reset the flow variable stack.
//        Assert.assertEquals("outside-variable", getVariableValue("outside-variable"));
//        Assert.assertEquals(null, getVariableValue("inside-variable-active"));
//        Assert.assertEquals(null, getVariableValue("inside-variable-inactive"));
//    }

}
