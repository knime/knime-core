/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
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
package org.knime.core.node.workflow.simpleLoop;

import java.util.Map;

import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowTestCase;
import org.knime.testing.node.executioncount.ExecutionCountNodeModel;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class SimpleLoopTest extends WorkflowTestCase {

    private NodeID m_loopStart;
    private NodeID m_loopEnd;
    private NodeID m_counterInLoop;
    private NodeID m_counterOutSourceLoop;
    private NodeID m_counterOutSinkLoop;
    private NodeID m_tblView;

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        NodeID baseID = loadAndSetWorkflow();
        m_loopStart = new NodeID(baseID, 2);
        m_loopEnd = new NodeID(baseID, 3);
        m_counterInLoop = new NodeID(baseID, 4);
        m_counterOutSourceLoop = new NodeID(baseID, 8);
        m_counterOutSinkLoop = new NodeID(baseID, 9);
        m_tblView = new NodeID(baseID, 5);
    }

    public void testExecuteFlow() throws Exception {
        checkState(m_loopStart, State.CONFIGURED);
        checkState(m_tblView, State.CONFIGURED);
        executeAndWait(m_loopEnd);
        waitWhileInExecution();
        checkState(m_loopEnd, State.EXECUTED);
        Map<NodeID, ExecutionCountNodeModel> counterNodes =
            getManager().findNodes(ExecutionCountNodeModel.class, true);
        int inCount = counterNodes.get(m_counterInLoop).getCounter();

        checkState(m_counterInLoop, State.EXECUTED);
        assertEquals("Expected 10 executions of node in loop", 10, inCount);

        int outCount = counterNodes.get(m_counterOutSourceLoop).getCounter();
        checkState(m_counterOutSourceLoop, State.EXECUTED);
        assertEquals(
                "Expected one execution of source nodes in loop", 1, outCount);

        int outCountSink = counterNodes.get(m_counterOutSinkLoop).getCounter();
        checkState(m_counterOutSinkLoop, State.EXECUTED);
        assertEquals("Expected 10 executions of sink nodes in loop",
                10, outCountSink);

        executeAndWait(m_tblView);
        checkState(m_tblView, State.EXECUTED);

        getManager().resetAndConfigureAll();
        assertEquals(getNrTablesInGlobalRepository(), 0);
    }

    /** {@inheritDoc} */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
