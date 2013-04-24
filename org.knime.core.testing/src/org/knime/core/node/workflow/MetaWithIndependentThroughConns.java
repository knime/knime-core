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

import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class MetaWithIndependentThroughConns extends WorkflowTestCase {

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
        checkState(m_topSource, InternalNodeContainerState.CONFIGURED);
        checkState(m_bottomSource, InternalNodeContainerState.CONFIGURED);
        checkState(m_metaWithOnlyThrough, InternalNodeContainerState.CONFIGURED);
        executeAllAndWait();
        checkState(m_topSource, InternalNodeContainerState.EXECUTED);
        checkState(m_metaWithOnlyThrough, InternalNodeContainerState.EXECUTED);

        // reset one sink -- no change
        getManager().resetAndConfigureNode(m_topSink);
        checkState(m_metaWithOnlyThrough, InternalNodeContainerState.EXECUTED);

        // reset one source -- meta node reset
        getManager().resetAndConfigureNode(m_topSource);
        checkState(m_metaWithOnlyThrough, InternalNodeContainerState.CONFIGURED);
        // unconnected through connection -- no change
        checkState(m_bottomSink, InternalNodeContainerState.EXECUTED);

        executeAllAndWait();
        checkState(m_metaWithOnlyThrough, InternalNodeContainerState.EXECUTED);
        getManager().getParent().resetAndConfigureNode(getManager().getID());
        checkState(m_metaWithOnlyThrough, InternalNodeContainerState.CONFIGURED);
    }

    public void testPullExecutionFromSink() throws Exception {
        checkState(m_topSource, InternalNodeContainerState.CONFIGURED);
        executeAndWait(m_topSink);
        checkState(m_topSink, InternalNodeContainerState.EXECUTED);
        checkState(m_bottomSource, InternalNodeContainerState.CONFIGURED);
        checkState(m_bottomSink, InternalNodeContainerState.CONFIGURED);

        getManager().resetAndConfigureNode(m_topSource);
        checkState(m_metaWithOnlyThrough, InternalNodeContainerState.CONFIGURED);
    }

    public void testExecuteAllThenDeleteOneSourceConnection() throws Exception {
        executeAllAndWait();
        checkState(m_topSink, InternalNodeContainerState.EXECUTED);
        checkState(m_bottomSink, InternalNodeContainerState.EXECUTED);
        // remove top connection
        getManager().removeConnection(getManager().getIncomingConnectionFor(
                m_metaWithOnlyThrough, 0));
        checkState(m_topSource, InternalNodeContainerState.EXECUTED);
        checkState(m_topSink, InternalNodeContainerState.IDLE);

        checkState(m_bottomSink, InternalNodeContainerState.EXECUTED);
    }

    public void testExecuteAllThenDeleteThroughConnection() throws Exception {
        executeAllAndWait();
        checkState(m_topSink, InternalNodeContainerState.EXECUTED);
        checkState(m_bottomSink, InternalNodeContainerState.EXECUTED);
        // remove top through connection
        WorkflowManager internalWFM = (WorkflowManager)(getManager()
                             .getNodeContainer(m_metaWithOnlyThrough));
        internalWFM.removeConnection(internalWFM.getIncomingConnectionFor(
                m_metaWithOnlyThrough, 0));
        checkState(m_topSource, InternalNodeContainerState.EXECUTED);
        checkState(m_bottomSource, InternalNodeContainerState.EXECUTED);
        checkState(m_topSink, InternalNodeContainerState.IDLE);
        checkState(m_bottomSink, InternalNodeContainerState.EXECUTED);
    }

    public void testExecuteAllThenResetOneSource() throws Exception {
        executeAllAndWait();
        checkState(m_topSink, InternalNodeContainerState.EXECUTED);
        checkState(m_bottomSink, InternalNodeContainerState.EXECUTED);

        getManager().resetAndConfigureNode(m_bottomSource);
        checkState(m_topSource, InternalNodeContainerState.EXECUTED);
        checkState(m_topSink, InternalNodeContainerState.EXECUTED);

        checkState(m_bottomSink, InternalNodeContainerState.CONFIGURED);
    }

    public void testInsertConnection() throws Exception {

        // top input deleted
        getManager().removeConnection(getManager().getIncomingConnectionFor(
                m_metaWithOnlyThrough, 0));

        executeAllAndWait();
        checkState(m_topSink, InternalNodeContainerState.IDLE);
        checkState(m_bottomSource, InternalNodeContainerState.EXECUTED);
        checkState(m_bottomSink, InternalNodeContainerState.EXECUTED);

        getManager().addConnection(m_bottomSource, 1, m_metaWithOnlyThrough, 0);

        checkState(m_topSource, InternalNodeContainerState.EXECUTED);
        checkState(m_bottomSource, InternalNodeContainerState.EXECUTED);
        checkState(m_bottomSink, InternalNodeContainerState.EXECUTED);
        checkState(m_topSink, InternalNodeContainerState.CONFIGURED);

        executeAllAndWait();
        checkState(m_topSink, InternalNodeContainerState.EXECUTED);

    }

}
