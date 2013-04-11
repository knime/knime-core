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
package org.knime.core.node.workflow.bug4182_quickformexecutionwithmetanodes2;

import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowTestCase;
import org.knime.core.quickform.in.QuickFormInputNode;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class QuickFormExecutionWithMetanodesTest2 extends WorkflowTestCase {

    private NodeID m_tableViewEnd;

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        NodeID baseID = loadAndSetWorkflow();
        m_tableViewEnd = new NodeID(baseID, 5);
    }

    public void testExecuteFlow() throws Exception {
        checkState(m_tableViewEnd, State.CONFIGURED);
    	getManager().stepExecutionUpToNodeType(QuickFormInputNode.class, QuickFormInputNode.NOT_HIDDEN_FILTER);
    	waitWhileInExecution();
    	checkState(m_tableViewEnd, State.EXECUTED);
    }

}
