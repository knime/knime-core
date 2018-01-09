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
 */
package org.knime.core.node.workflow;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests execution of subnodes and nodes in subnodes that live in a loop that was loaded in a partially executed state.
 * https://bugs.knime.org/AP-7585
 */
public class BugAP7585_SubnodeInRestoredLoop extends WorkflowTestCase {

    private NodeID m_chunkLoopStart_2;
    private NodeID m_rowFilter_3;
    private NodeID m_subnode_6;
    private NodeID m_subnode_9;
    private NodeID m_subnodeRowfilter_9_7;
    private NodeID m_subnodeRowfilter_9_8;
    private NodeID m_loopEnd_10;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_chunkLoopStart_2 = baseID.createChild(2);
        m_rowFilter_3 = baseID.createChild(3);
        m_subnode_6 = baseID.createChild(6);
        m_subnode_9 = baseID.createChild(9);
        NodeID subnode9WFM = m_subnode_9.createChild(0);
        m_subnodeRowfilter_9_7 = subnode9WFM.createChild(7);
        m_subnodeRowfilter_9_8 = subnode9WFM.createChild(8);
        m_loopEnd_10 = baseID.createChild(10);
    }

    /** Individually run nodes and check their expected message. Includes nodes within a subnode. */
    @Test
    public void testRunIndividualWithRestoredContext() throws Exception {
        WorkflowManager manager = getManager();
        checkState(manager, CONFIGURED);
        checkStateOfMany(CONFIGURED, m_rowFilter_3, m_subnode_6, m_subnodeRowfilter_9_8, m_loopEnd_10);
        checkStateOfMany(EXECUTED, m_chunkLoopStart_2, m_subnodeRowfilter_9_7);
        for (NodeID id: Arrays.asList(m_rowFilter_3, m_subnode_6, m_subnodeRowfilter_9_8)) {
            executeAndWait(id);
            checkState(id, CONFIGURED);
            NodeMessage nodeMessage = getManager().findNodeContainer(id).getNodeMessage();
            Assert.assertThat("Expected error", nodeMessage.getMessageType(), is(NodeMessage.Type.ERROR));
            Assert.assertThat("Expected error about restored loop",
                nodeMessage.getMessage(), containsString("restored"));
        }
    }

    /** Runs the entire workflow, checks errors on the top-level only. */
    @Test
    public void testRunAllWithRestoredContext() throws Exception {
        executeAllAndWait();
        for (NodeID id: Arrays.asList(m_rowFilter_3, m_subnode_6, m_subnode_9)) {
            checkState(id, CONFIGURED);
            NodeMessage nodeMessage = getManager().findNodeContainer(id).getNodeMessage();
            Assert.assertThat("Expected error", nodeMessage.getMessageType(), is(NodeMessage.Type.ERROR));
            Assert.assertThat("Expected error about restored loop",
                nodeMessage.getMessage(), containsString("restored"));
        }
    }

    /** Runs the entire workflow, checks completion. */
    @Test
    public void testRunAllAfterReset() throws Exception {
        reset(m_chunkLoopStart_2);
        executeAllAndWait();
        checkState(getManager(), EXECUTED);
    }

}
