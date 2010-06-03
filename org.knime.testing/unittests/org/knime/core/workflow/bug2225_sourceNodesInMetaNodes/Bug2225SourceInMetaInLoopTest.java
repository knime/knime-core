/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
package org.knime.core.workflow.bug2225_sourceNodesInMetaNodes;

import java.util.Map;

import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.workflow.WorkflowTestCase;
import org.knime.testing.node.executioncount.ExecutionCountNodeModel;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class Bug2225SourceInMetaInLoopTest extends WorkflowTestCase {

    private NodeID m_loopStart;
    private NodeID m_loopEnd;
    private NodeID m_counterInLoop;
    private NodeID m_counterOutLoop;
    private NodeID m_tblView;

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        NodeID baseID = loadAndSetWorkflow();
        m_loopStart = new NodeID(baseID, 2);
        m_loopEnd = new NodeID(baseID, 3);
        NodeID meta = new NodeID(baseID, 5);
        m_counterInLoop = new NodeID(meta, 2);
        m_counterOutLoop = new NodeID(meta, 4);
        m_tblView = new NodeID(baseID, 6);
    }

    public void testExecuteFlow() throws Exception {
        checkState(m_loopStart, State.CONFIGURED);
        checkState(m_tblView, State.CONFIGURED);
        executeAndWait(m_loopEnd);
        waitWhileInExecution();
        System.out.println(getManager().printNodeSummary(getManager().getID(), 0));
        checkState(m_loopEnd, State.EXECUTED);
        Map<NodeID, ExecutionCountNodeModel> counterNodes =
            getManager().findNodes(ExecutionCountNodeModel.class, true);
        int inCount = counterNodes.get(m_counterInLoop).getCounter();

        checkState(m_counterInLoop, State.EXECUTED);
        assert inCount == 1 : "Expected one execution of source node: " + inCount;
        int outCount = counterNodes.get(m_counterOutLoop).getCounter();

        checkState(m_counterOutLoop, State.CONFIGURED);
        assert outCount == 0 : "Expected no execution of unconnected node: "
            + outCount;

        executeAndWait(m_tblView);
        checkState(m_tblView, State.EXECUTED);

        outCount = counterNodes.get(m_counterOutLoop).getCounter();
        checkState(m_counterOutLoop, State.EXECUTED);
        assert outCount == 1 : "Expected no execution of unconnected node: "
            + outCount;

    }

    /** {@inheritDoc} */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
