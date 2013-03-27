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
 *   19.06.2012 (wiswedel): created
 */
package org.knime.core.node.workflow.testFailSideBranchInLoop;

import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowTestCase;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class FailSideBranchInLoop extends WorkflowTestCase {

    private NodeID m_dataGen2;
    private NodeID m_loopStart3;
    private NodeID m_csvWriterInLoop13;
    private NodeID m_loopEnd4;

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        NodeID baseID = loadAndSetWorkflow();
        m_dataGen2 = new NodeID(baseID, 2);
        m_loopStart3 = new NodeID(baseID, 3);
        m_csvWriterInLoop13 = new NodeID(baseID, 13);
        m_loopEnd4 = new NodeID(baseID, 4);
    }

    public void disabledBug3292testExecuteFlowWithUnconfiguredCSVWriter() throws Exception {
        checkState(m_dataGen2, State.CONFIGURED);
        checkState(m_loopEnd4, State.CONFIGURED);
        checkState(m_csvWriterInLoop13, State.IDLE);
        getManager().executeUpToHere(m_loopEnd4);
        final NodeContainer loopEndNC = getManager().getNodeContainer(m_loopEnd4);
        waitWhile(loopEndNC, new Hold() {

            @Override
            protected boolean shouldHold() {
                return loopEndNC.getState().executionInProgress();
            }
            @Override
            protected int getSecondsToWaitAtMost() {
                return 2;
            }
        });
        checkState(m_loopEnd4, State.CONFIGURED);
        checkState(m_loopStart3, State.EXECUTED);
    }

    public void testExecuteFlowNoCSVWriter() throws Exception {
        checkState(m_dataGen2, State.CONFIGURED);
        checkState(m_loopEnd4, State.CONFIGURED);
        deleteConnection(m_csvWriterInLoop13, 1);
        checkState(m_csvWriterInLoop13, State.IDLE);
        executeAllAndWait();
        checkState(m_loopEnd4, State.EXECUTED);
        checkState(m_loopStart3, State.EXECUTED);
    }

}
