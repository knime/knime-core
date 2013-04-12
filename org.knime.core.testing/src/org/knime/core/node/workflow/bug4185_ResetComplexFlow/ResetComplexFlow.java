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
package org.knime.core.node.workflow.bug4185_ResetComplexFlow;

import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowTestCase;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class ResetComplexFlow extends WorkflowTestCase {

    private NodeID m_javaEditStart;
    private NodeID m_javaEditEnd;
    private NodeID m_metaMiddle;

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        NodeID baseID = loadAndSetWorkflow();
        m_javaEditStart = new NodeID(baseID, 610);
        m_javaEditEnd = new NodeID(baseID, 705);
        m_metaMiddle = new NodeID(baseID, 704);
    }

    public void testExecuteAllAndReset() throws Exception {
        executeAllAndWait();
        checkState(getManager(), State.EXECUTED);
        getManager().getParent().resetAndConfigureNode(getManager().getID());
        checkState(getManager(), State.CONFIGURED);
        checkState(m_javaEditStart, State.CONFIGURED);
        checkState(m_javaEditEnd, State.CONFIGURED);
    }

    public void testExecuteAllResetStart() throws Exception {
        executeAllAndWait();
        checkState(getManager(), State.EXECUTED);
        getManager().resetAndConfigureNode(m_javaEditStart);
        checkState(getManager(), State.CONFIGURED);
        checkState(m_javaEditStart, State.CONFIGURED);
        checkState(m_javaEditEnd, State.CONFIGURED);
    }
    
    public void testDeleteConnectionToMetaExecuteFirst() throws Exception {
        internalTestDeleteConnectionToMeta(true);
    }
        
    public void testDeleteConnectionToMetaDontExecute() throws Exception {
        internalTestDeleteConnectionToMeta(false);
    }
    
    private void internalTestDeleteConnectionToMeta(final boolean executeFirst) throws Exception {
        if (executeFirst) {
            executeAllAndWait();
            checkState(getManager(), State.EXECUTED);
        }
        deleteConnection(m_metaMiddle, 0);
        checkState(getManager(), State.IDLE);
        checkState(m_javaEditStart, State.CONFIGURED, State.EXECUTED);
        checkState(m_javaEditEnd, State.IDLE);
    }
    

}
