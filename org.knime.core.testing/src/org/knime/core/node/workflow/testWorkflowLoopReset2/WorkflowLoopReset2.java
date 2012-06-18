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
package org.knime.core.node.workflow.testWorkflowLoopReset2;

import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowTestCase;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class WorkflowLoopReset2 extends WorkflowTestCase {

    private NodeID m_dataGen1;
    private NodeID m_loopStartInMeta2;
    private NodeID m_loopEndInMeta3;
    private NodeID m_tableView3;

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        NodeID baseID = loadAndSetWorkflow();
        m_dataGen1 = new NodeID(baseID, 1);
        NodeID metaID = new NodeID(baseID, 2);
        m_loopStartInMeta2 = new NodeID(metaID, 2);
        m_loopEndInMeta3 = new NodeID(metaID, 3);
        m_tableView3 = new NodeID(baseID, 3);
    }

    public void testLoopEndReset() throws Exception {
        executeAllAndWait();
        checkState(m_tableView3, State.EXECUTED);
        reset(m_loopEndInMeta3);
        checkState(m_tableView3, State.CONFIGURED);
        checkState(m_loopEndInMeta3, State.CONFIGURED);
        checkState(m_loopStartInMeta2, State.CONFIGURED);
        checkState(m_dataGen1, State.EXECUTED);
    }

    public void testDataGenReset() throws Exception {
        executeAllAndWait();
        checkState(m_tableView3, State.EXECUTED);
        reset(m_dataGen1);
        checkState(m_tableView3, State.CONFIGURED);
        checkState(m_loopEndInMeta3, State.CONFIGURED);
        checkState(m_loopStartInMeta2, State.CONFIGURED);
        checkState(m_dataGen1, State.CONFIGURED);
    }

}
