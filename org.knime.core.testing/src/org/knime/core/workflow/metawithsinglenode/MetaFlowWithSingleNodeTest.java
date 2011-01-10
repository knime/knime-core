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
package org.knime.core.workflow.metawithsinglenode;

import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.workflow.WorkflowTestCase;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class MetaFlowWithSingleNodeTest extends WorkflowTestCase {
    
    private NodeID m_dataGen;
    private NodeID m_colFilterInMeta;
    private NodeID m_meta;
    private NodeID m_tblView;
    
    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        NodeID baseID = loadAndSetWorkflow();
        m_dataGen = new NodeID(baseID, 1);
        m_meta = new NodeID(baseID, 2);
        m_colFilterInMeta = new NodeID(m_meta, 1);
        m_tblView = new NodeID(baseID, 3);
    }
    
    public void testExecuteOneByOne() throws Exception {
        checkState(m_dataGen, State.CONFIGURED);
        checkState(m_colFilterInMeta, State.CONFIGURED);
        checkMetaOutState(m_meta, 0, State.CONFIGURED);
        checkState(m_tblView, State.CONFIGURED);
        
        executeAndWait(m_dataGen);
        checkState(m_dataGen, State.EXECUTED);
        checkState(m_colFilterInMeta, State.CONFIGURED);
        checkMetaOutState(m_meta, 0, State.CONFIGURED);
        checkState(m_tblView, State.CONFIGURED);
        
        executeAndWait(m_colFilterInMeta);
        checkState(m_dataGen, State.EXECUTED);
        checkState(m_colFilterInMeta, State.EXECUTED);
        checkMetaOutState(m_meta, 0, State.EXECUTED);
        checkState(m_tblView, State.CONFIGURED);

        executeAndWait(m_tblView);
        checkState(m_dataGen, State.EXECUTED);
        checkState(m_colFilterInMeta, State.EXECUTED);
        checkMetaOutState(m_meta, 0, State.EXECUTED);
        checkState(m_tblView, State.EXECUTED);
        
        // state may not have propagated to workflow
        waitWhileInExecution();
        checkState(m_meta, State.EXECUTED);
        
    }
    
    public void testExecuteLast() throws Exception {
        executeAndWait(m_tblView);
        checkState(m_dataGen, State.EXECUTED);
        checkState(m_colFilterInMeta, State.EXECUTED);
        checkMetaOutState(m_meta, 0, State.EXECUTED);
        checkState(m_tblView, State.EXECUTED);
    }
    
    public void testExecuteInMeta() throws Exception {
        executeAndWait(m_colFilterInMeta);
        checkState(m_dataGen, State.EXECUTED);
        checkState(m_colFilterInMeta, State.EXECUTED);
        checkMetaOutState(m_meta, 0, State.EXECUTED);
        checkState(m_tblView, State.CONFIGURED);
    }
    
    public void testExecuteAll() throws Exception {
        getManager().executeAllAndWaitUntilDone();
        checkState(m_dataGen, State.EXECUTED);
        checkState(m_colFilterInMeta, State.EXECUTED);
        checkMetaOutState(m_meta, 0, State.EXECUTED);
        checkState(m_tblView, State.EXECUTED);
    }
    
    public void testRandomExecuteAndReset() throws Exception {
        executeAndWait(m_colFilterInMeta);
        checkState(m_dataGen, State.EXECUTED);
        checkState(m_colFilterInMeta, State.EXECUTED);
        checkMetaOutState(m_meta, 0, State.EXECUTED);
        checkState(m_tblView, State.CONFIGURED);
        
        assertTrue(getManager().canResetNode(m_meta));
        getManager().resetAndConfigureNode(m_meta);
        checkState(m_dataGen, State.EXECUTED);
        checkState(m_colFilterInMeta, State.CONFIGURED);
        checkMetaOutState(m_meta, 0, State.CONFIGURED);
        checkState(m_tblView, State.CONFIGURED);
        
        executeAndWait(m_tblView);
        checkState(m_tblView, State.EXECUTED);
        
        for (int i = 0; i < 10; i++) {
            getManager().resetAndConfigureNode(m_dataGen);
            checkState(m_dataGen, State.CONFIGURED);
            checkState(m_colFilterInMeta, State.CONFIGURED);
            checkMetaOutState(m_meta, 0, State.CONFIGURED);
            checkState(m_tblView, State.CONFIGURED);
            
            executeAndWait(m_tblView);
            checkState(m_dataGen, State.EXECUTED);
            checkState(m_colFilterInMeta, State.EXECUTED);
            checkMetaOutState(m_meta, 0, State.EXECUTED);
            checkState(m_tblView, State.EXECUTED);
        }
    }
    
    public void testDeleteOuterConnectionTryExecuteInsertAgain() 
        throws Exception {
        WorkflowManager m = getManager();
        ConnectionContainer c = findInConnection(m_meta, 0);
        assertNotNull(c);
        
        assertTrue(m.canRemoveConnection(c));
        m.removeConnection(c);
        assertNull(findInConnection(m_meta, 0));
        
        checkState(m_colFilterInMeta, State.IDLE);
        checkMetaOutState(m_meta, 0, State.IDLE);
        assertFalse(findParent(
                m_colFilterInMeta).canExecuteNode(m_colFilterInMeta));
        assertFalse(m.canExecuteNode(m_tblView));
        assertFalse(m.canExecuteNode(m_meta));
        
        executeAndWait(m_colFilterInMeta);
        checkState(m_meta, State.IDLE);
        
        m.addConnection(c.getSource(), c.getSourcePort(), 
                c.getDest(), c.getDestPort());
        checkState(m_colFilterInMeta, State.CONFIGURED);
        checkMetaOutState(m_meta, 0, State.CONFIGURED);
        checkState(m_tblView, State.CONFIGURED);
        
        executeAndWait(m_tblView);
        checkState(m_tblView, State.EXECUTED);
        checkMetaOutState(m_meta, 0, State.EXECUTED);
    }
    
    public void testDeleteInnerConnectionTryExecuteInsertAgain() 
    throws Exception {
        WorkflowManager m = getManager();
        WorkflowManager meta = findParent(m_colFilterInMeta);
        ConnectionContainer c = findInConnection(m_colFilterInMeta, 0);
        assertNotNull(c);
        
        assertTrue(meta.canRemoveConnection(c));
        meta.removeConnection(c);
        assertNull(findInConnection(m_colFilterInMeta, 0));
        
        checkState(m_colFilterInMeta, State.IDLE);
        checkMetaOutState(m_meta, 0, State.IDLE);
        assertFalse(m.canExecuteNode(m_colFilterInMeta));
        assertFalse(m.canExecuteNode(m_tblView));
        assertFalse(m.canExecuteNode(m_meta));
        
        executeAndWait(m_colFilterInMeta);
        checkState(m_meta, State.IDLE);
        
        meta.addConnection(c.getSource(), c.getSourcePort(), 
                c.getDest(), c.getDestPort());
        checkState(m_colFilterInMeta, State.CONFIGURED);
        checkMetaOutState(m_meta, 0, State.CONFIGURED);
        checkState(m_tblView, State.CONFIGURED);
        
        executeAndWait(m_tblView);
        checkState(m_tblView, State.EXECUTED);
        checkMetaOutState(m_meta, 0, State.EXECUTED);
    }
    
    public void testExecuteDeleteOuterConnection() throws Exception {
        WorkflowManager m = getManager();
        WorkflowManager meta = findParent(m_colFilterInMeta);
        executeAndWait(m_tblView);
        ConnectionContainer connection = findInConnection(m_meta, 0);
        assertTrue(m.canRemoveConnection(connection));
        m.removeConnection(connection);
        
        checkState(m_colFilterInMeta, State.IDLE);
        checkMetaOutState(m_meta, 0, State.IDLE);
        assertFalse(meta.canExecuteNode(m_colFilterInMeta));
        assertFalse(m.canExecuteNode(m_tblView));
    }
    
    public void testExecuteDeleteInnerConnection() throws Exception {
        WorkflowManager m = getManager();
        WorkflowManager meta = findParent(m_colFilterInMeta);
        executeAndWait(m_tblView);
        ConnectionContainer connection = findInConnection(m_colFilterInMeta, 0);
        assertTrue(meta.canRemoveConnection(connection));
        meta.removeConnection(connection);
        
        checkState(m_colFilterInMeta, State.IDLE);
        checkMetaOutState(m_meta, 0, State.IDLE);
        assertFalse(meta.canExecuteNode(m_colFilterInMeta));
        assertFalse(m.canExecuteNode(m_tblView));
    }
    
    public void testExecuteDeleteNode() throws Exception {
        WorkflowManager m = getManager();
        WorkflowManager meta = findParent(m_colFilterInMeta);
        executeAndWait(m_tblView);
        assertTrue(meta.canRemoveNode(m_colFilterInMeta));
        meta.removeNode(m_colFilterInMeta);
        
        checkState(m_tblView, State.IDLE);
        assertFalse(m.canExecuteNode(m_tblView));
    }
}
