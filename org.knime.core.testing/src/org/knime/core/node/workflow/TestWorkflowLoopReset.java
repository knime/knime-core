/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2013
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 *
 * History
 *   01.11.2008 (wiswedel): created
 */
package org.knime.core.node.workflow;

import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;

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

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
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
