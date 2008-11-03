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
package org.knime.core.workflow.simplechainofnodes;

import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.workflow.WorkflowTestCase;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class ChainOfNodesTest extends WorkflowTestCase {
    
    private NodeID m_dataGen;
    private NodeID m_colFilter;
    private NodeID m_rowFilter;
    private NodeID m_tblView;
    
    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        NodeID baseID = loadAndSetWorkflow();
        m_dataGen = new NodeID(baseID, 1);
        m_colFilter = new NodeID(baseID, 2);
        m_rowFilter = new NodeID(baseID, 3);
        m_tblView = new NodeID(baseID, 4);
    }
    
    public void testExecuteOneByOne() throws Exception {
        checkState(m_dataGen, State.CONFIGURED);
        checkState(m_colFilter, State.CONFIGURED);
        checkState(m_rowFilter, State.CONFIGURED);
        checkState(m_tblView, State.CONFIGURED);
        
        executeAndWait(m_dataGen);
        checkState(m_dataGen, State.EXECUTED);
        checkState(m_colFilter, State.CONFIGURED);
        checkState(m_rowFilter, State.CONFIGURED);
        checkState(m_tblView, State.CONFIGURED);
        
        executeAndWait(m_colFilter);
        checkState(m_dataGen, State.EXECUTED);
        checkState(m_colFilter, State.EXECUTED);
        checkState(m_rowFilter, State.CONFIGURED);
        checkState(m_tblView, State.CONFIGURED);

        executeAndWait(m_rowFilter);
        checkState(m_dataGen, State.EXECUTED);
        checkState(m_colFilter, State.EXECUTED);
        checkState(m_rowFilter, State.EXECUTED);
        checkState(m_tblView, State.CONFIGURED);
        
        executeAndWait(m_tblView);
        checkState(m_dataGen, State.EXECUTED);
        checkState(m_colFilter, State.EXECUTED);
        checkState(m_rowFilter, State.EXECUTED);
        checkState(m_tblView, State.EXECUTED);
    }

}
