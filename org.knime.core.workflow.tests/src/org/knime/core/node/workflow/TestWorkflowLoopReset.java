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

import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.NodeSettings;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class TestWorkflowLoopReset extends WorkflowTestCase {

    private NodeID m_tableCreateNode1;
    private NodeID m_loopStartNode2;
    private NodeID m_loopEndNode4;
    private NodeID m_groupByInLoopNode3;
    private NodeID m_metaInLoopNode8;
    private NodeID m_tableCreateNode6;
    private NodeID m_javaEditInMetaThroughNode8_7;
    private NodeID m_javaEditInMetaNewSourceNode8_8;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_tableCreateNode1 = new NodeID(baseID, 1);
        m_loopStartNode2 = new NodeID(baseID, 2);
        m_loopEndNode4 = new NodeID(baseID, 4);
        m_groupByInLoopNode3 = new NodeID(baseID, 3);
        m_metaInLoopNode8 = new NodeID(baseID, 8);
        m_tableCreateNode6 = new NodeID(baseID, 6);
        m_javaEditInMetaThroughNode8_7 = new NodeID(m_metaInLoopNode8, 7);
        m_javaEditInMetaNewSourceNode8_8 = new NodeID(m_metaInLoopNode8, 8);
    }

    @Test
    public void testUpStreamOfLoopReset() throws Exception {
        executeAllAndWait();
        checkState(m_loopEndNode4, InternalNodeContainerState.EXECUTED);
        reset(m_tableCreateNode1);
        checkState(m_tableCreateNode1, InternalNodeContainerState.CONFIGURED);
        checkState(m_loopEndNode4, InternalNodeContainerState.CONFIGURED);
        checkState(m_groupByInLoopNode3, InternalNodeContainerState.CONFIGURED);
        checkState(m_tableCreateNode6, InternalNodeContainerState.EXECUTED);
        checkState(m_javaEditInMetaThroughNode8_7, InternalNodeContainerState.CONFIGURED);
        checkState(m_javaEditInMetaNewSourceNode8_8, InternalNodeContainerState.EXECUTED);
    }

    @Test
    public void testNodeInLoopReset() throws Exception {
        executeAllAndWait();
        reset(m_groupByInLoopNode3);
        checkState(m_groupByInLoopNode3, InternalNodeContainerState.CONFIGURED);
        checkState(m_tableCreateNode1, InternalNodeContainerState.EXECUTED);
        checkState(m_loopStartNode2, InternalNodeContainerState.CONFIGURED);
        checkState(m_loopEndNode4, InternalNodeContainerState.CONFIGURED);
        checkState(m_tableCreateNode6, InternalNodeContainerState.EXECUTED);
        checkState(m_javaEditInMetaThroughNode8_7, InternalNodeContainerState.CONFIGURED);
        checkState(m_javaEditInMetaNewSourceNode8_8, InternalNodeContainerState.EXECUTED);
    }

    @Test
    public void testSourceNodeInLoopReset() throws Exception {
        executeAllAndWait();
        reset(m_tableCreateNode6);
        checkState(m_groupByInLoopNode3, InternalNodeContainerState.CONFIGURED);
        checkState(m_tableCreateNode1, InternalNodeContainerState.EXECUTED);
        checkState(m_loopStartNode2, InternalNodeContainerState.CONFIGURED);
        checkState(m_loopEndNode4, InternalNodeContainerState.CONFIGURED);
        checkState(m_tableCreateNode6, InternalNodeContainerState.CONFIGURED);
        checkState(m_javaEditInMetaThroughNode8_7, InternalNodeContainerState.CONFIGURED);
        checkState(m_javaEditInMetaNewSourceNode8_8, InternalNodeContainerState.EXECUTED);
    }


    /** disabled, see bug 3246. */
    @Test
    public void testNodeInMetaNodeInLoopReset() throws Exception {
        executeAllAndWait();
        reset(m_javaEditInMetaThroughNode8_7);
        checkState(m_javaEditInMetaThroughNode8_7, InternalNodeContainerState.CONFIGURED);
        checkState(m_tableCreateNode1, InternalNodeContainerState.EXECUTED);
        checkState(m_loopStartNode2, InternalNodeContainerState.CONFIGURED);
        checkState(m_loopEndNode4, InternalNodeContainerState.CONFIGURED);
        checkState(m_tableCreateNode6, InternalNodeContainerState.EXECUTED);
        checkState(m_javaEditInMetaNewSourceNode8_8, InternalNodeContainerState.EXECUTED);
    }

    /** disabled, see bug 3246. */
    @Test
    public void testSourceNodeInMetaNodeInLoopReset() throws Exception {
        executeAllAndWait();
        reset(m_javaEditInMetaNewSourceNode8_8);
        checkState(m_javaEditInMetaNewSourceNode8_8, InternalNodeContainerState.CONFIGURED);
        checkState(m_javaEditInMetaThroughNode8_7, InternalNodeContainerState.CONFIGURED);
        checkState(m_tableCreateNode1, InternalNodeContainerState.EXECUTED);
        checkState(m_loopStartNode2, InternalNodeContainerState.CONFIGURED);
        checkState(m_loopEndNode4, InternalNodeContainerState.CONFIGURED);
        checkState(m_tableCreateNode6, InternalNodeContainerState.EXECUTED);
    }

    @Test
    public void testDeleteConnectionInLoop() throws Exception {
        executeAllAndWait();
        deleteConnection(m_groupByInLoopNode3, 1);
        checkState(m_javaEditInMetaNewSourceNode8_8, InternalNodeContainerState.EXECUTED);
        checkState(m_javaEditInMetaThroughNode8_7, InternalNodeContainerState.CONFIGURED);
        checkState(m_tableCreateNode1, InternalNodeContainerState.EXECUTED);
        checkState(m_loopStartNode2, InternalNodeContainerState.CONFIGURED);
        checkState(m_loopEndNode4, InternalNodeContainerState.IDLE);
        checkState(m_groupByInLoopNode3, InternalNodeContainerState.IDLE);
    }

    /** disabled, see bug 3246. */
    @Test
    public void testDeleteConnectionInMetaNodeInLoop() throws Exception {
        executeAllAndWait();
        deleteConnection(m_javaEditInMetaThroughNode8_7, 1);
        checkState(m_javaEditInMetaNewSourceNode8_8, InternalNodeContainerState.EXECUTED);
        checkState(m_javaEditInMetaThroughNode8_7, InternalNodeContainerState.IDLE);
        checkState(m_tableCreateNode1, InternalNodeContainerState.EXECUTED);
        checkState(m_loopStartNode2, InternalNodeContainerState.CONFIGURED);
        checkState(m_loopEndNode4, InternalNodeContainerState.IDLE);
        checkState(m_groupByInLoopNode3, InternalNodeContainerState.CONFIGURED);
    }

    /** disabled, see bug 3246. */
    @Test
    public void testDeleteConnectionInMetaNodeInIndirectLoop() throws Exception {
        executeAllAndWait();
        deleteConnection(m_javaEditInMetaThroughNode8_7, 0);
        checkState(m_javaEditInMetaNewSourceNode8_8, InternalNodeContainerState.EXECUTED);
        checkState(m_javaEditInMetaThroughNode8_7, InternalNodeContainerState.CONFIGURED);
        checkState(m_tableCreateNode1, InternalNodeContainerState.EXECUTED);
        checkState(m_loopStartNode2, InternalNodeContainerState.CONFIGURED);
        checkState(m_loopEndNode4, InternalNodeContainerState.CONFIGURED);
        checkState(m_groupByInLoopNode3, InternalNodeContainerState.CONFIGURED);
    }

    @Test
    public void testAddConnectionInExecutedLoop() throws Exception {
        deleteConnection(m_groupByInLoopNode3, 0);
        executeAllAndWait();
        checkState(m_loopEndNode4, InternalNodeContainerState.EXECUTED);
        // the same connection deleted above
        getManager().addConnection(m_tableCreateNode6,
                0, m_groupByInLoopNode3, 0);
        checkState(m_javaEditInMetaNewSourceNode8_8, InternalNodeContainerState.EXECUTED);
        checkState(m_javaEditInMetaThroughNode8_7, InternalNodeContainerState.CONFIGURED);
        checkState(m_tableCreateNode1, InternalNodeContainerState.EXECUTED);
        checkState(m_loopStartNode2, InternalNodeContainerState.CONFIGURED);
        checkState(m_loopEndNode4, InternalNodeContainerState.CONFIGURED);
        checkState(m_groupByInLoopNode3, InternalNodeContainerState.CONFIGURED);
    }

    @Test
    public void testChangeConfigInExecutedLoop() throws Exception {
        NodeSettings s = new NodeSettings("groupByModel");
        WorkflowManager m = getManager();
        m.saveNodeSettings(m_groupByInLoopNode3, s);
        executeAllAndWait();
        m.loadNodeSettings(m_groupByInLoopNode3, s);
        checkState(m_groupByInLoopNode3, InternalNodeContainerState.CONFIGURED);
        checkState(m_loopEndNode4, InternalNodeContainerState.CONFIGURED);
        checkState(m_loopStartNode2, InternalNodeContainerState.CONFIGURED);
    }

}
