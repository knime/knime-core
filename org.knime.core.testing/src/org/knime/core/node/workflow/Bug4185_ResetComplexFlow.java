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

import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;
import static org.knime.core.node.workflow.InternalNodeContainerState.IDLE;
/**
 *
 * @author wiswedel, University of Konstanz
 */
public class Bug4185_ResetComplexFlow extends WorkflowTestCase {

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
        checkState(getManager(), EXECUTED);
        getManager().getParent().resetAndConfigureNode(getManager().getID());
        checkState(getManager(), CONFIGURED);
        checkState(m_javaEditStart, CONFIGURED);
        checkState(m_javaEditEnd, CONFIGURED);
    }

    public void testExecuteAllResetStart() throws Exception {
        executeAllAndWait();
        checkState(getManager(), EXECUTED);
        getManager().resetAndConfigureNode(m_javaEditStart);
        checkState(getManager(), CONFIGURED);
        checkState(m_javaEditStart, CONFIGURED);
        checkState(m_javaEditEnd, CONFIGURED);
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
            checkState(getManager(), EXECUTED);
        }
        deleteConnection(m_metaMiddle, 0);
        checkState(getManager(), IDLE);
        checkState(m_javaEditStart, CONFIGURED, EXECUTED);
        checkState(m_javaEditEnd, IDLE);
    }
    

}
