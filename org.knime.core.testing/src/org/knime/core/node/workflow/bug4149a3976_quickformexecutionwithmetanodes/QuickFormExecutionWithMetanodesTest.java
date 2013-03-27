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
package org.knime.core.node.workflow.bug4149a3976_quickformexecutionwithmetanodes;

import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowTestCase;
import org.knime.core.quickform.in.QuickFormInputNode;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class QuickFormExecutionWithMetanodesTest extends WorkflowTestCase {

    private NodeID m_end1;
    private NodeID m_end2;
    private NodeID m_end3;

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        NodeID baseID = loadAndSetWorkflow();
        m_end1 = new NodeID(baseID, 12);
        m_end2 = new NodeID(baseID, 11);
        m_end3 = new NodeID(baseID, 13);
    }

    public void testExecuteFlow() throws Exception {
    	getManager().stepExecutionUpToNodeType(QuickFormInputNode.class, QuickFormInputNode.NOT_HIDDEN_FILTER);
    	waitWhileInExecution();
        checkState(m_end1, State.EXECUTED);
        checkState(m_end2, State.EXECUTED);
        checkState(m_end3, State.EXECUTED);
    }

    /** {@inheritDoc} */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
