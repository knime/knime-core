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
package org.knime.core.workflow.metaWithIndependentThroughConns;

import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.workflow.WorkflowTestCase;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class MetaWithIndependentThroughConnsTest extends WorkflowTestCase {

    private NodeID m_topSource;
    private NodeID m_bottomSource;
    private NodeID m_topSink;
    private NodeID m_bottomSink;
    private NodeID m_metaWithOnlyThrough;

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        NodeID baseID = loadAndSetWorkflow();
        m_topSource = new NodeID(baseID, 1);
        m_bottomSource = new NodeID(baseID, 2);
        m_metaWithOnlyThrough = new NodeID(baseID, 3);
        m_topSink = new NodeID(baseID, 4);
        m_bottomSink = new NodeID(baseID, 5);
    }

    public void testStateOfMeta() throws Exception {
        checkState(m_topSource, State.CONFIGURED);
        checkState(m_bottomSource, State.CONFIGURED);
        checkState(m_metaWithOnlyThrough, State.CONFIGURED);
        executeAllAndWait();
        checkState(m_topSource, State.EXECUTED);
        checkState(m_metaWithOnlyThrough, State.EXECUTED);

        // reset one sink -- no change
        getManager().resetAndConfigureNode(m_topSink);
        checkState(m_metaWithOnlyThrough, State.EXECUTED);

        // reset one source -- meta node reset
        getManager().resetAndConfigureNode(m_topSource);
        checkState(m_metaWithOnlyThrough, State.CONFIGURED);
        // unconnected through connection -- no change
        checkState(m_bottomSink, State.EXECUTED);

        executeAllAndWait();
        checkState(m_metaWithOnlyThrough, State.EXECUTED);
        getManager().resetAll();
        checkState(m_metaWithOnlyThrough, State.CONFIGURED);
    }

    public void testPullExecutionFromSink() throws Exception {
        checkState(m_topSource, State.CONFIGURED);
        executeAndWait(m_topSink);
        checkState(m_topSink, State.EXECUTED);
        checkState(m_bottomSource, State.CONFIGURED);
        checkState(m_bottomSink, State.CONFIGURED);

        getManager().resetAndConfigureNode(m_topSource);
        checkState(m_metaWithOnlyThrough, State.CONFIGURED);
    }

    public void testExecuteAllThenDeleteOneSourceConnection() throws Exception {
        executeAllAndWait();
        checkState(m_topSink, State.EXECUTED);
        checkState(m_bottomSink, State.EXECUTED);
        // remove top connection
        getManager().removeConnection(getManager().getIncomingConnectionFor(
                m_metaWithOnlyThrough, 0));
        checkState(m_topSource, State.EXECUTED);
        checkState(m_topSink, State.IDLE);

        checkState(m_bottomSink, State.EXECUTED);
    }

    public void testExecuteAllThenResetOneSource() throws Exception {
        executeAllAndWait();
        checkState(m_topSink, State.EXECUTED);
        checkState(m_bottomSink, State.EXECUTED);

        getManager().resetAndConfigureNode(m_bottomSource);
        checkState(m_topSource, State.EXECUTED);
        checkState(m_topSink, State.EXECUTED);

        checkState(m_bottomSink, State.CONFIGURED);
    }

    public void testInsertConnection() throws Exception {

        // top input deleted
        getManager().removeConnection(getManager().getIncomingConnectionFor(
                m_metaWithOnlyThrough, 0));

        executeAllAndWait();
        checkState(m_topSink, State.IDLE);
        checkState(m_bottomSource, State.EXECUTED);
        checkState(m_bottomSink, State.EXECUTED);

        getManager().addConnection(m_bottomSource, 0,m_metaWithOnlyThrough, 0);

        checkState(m_topSource, State.EXECUTED);
        checkState(m_bottomSource, State.EXECUTED);
        checkState(m_bottomSink, State.CONFIGURED);
        checkState(m_topSink, State.EXECUTED);

    }

}
