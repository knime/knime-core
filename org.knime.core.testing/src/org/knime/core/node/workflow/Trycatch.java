/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2013
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not+ modify, publish, transmit, transfer or sell, reproduce,
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

import java.awt.Window.Type;
import java.util.NoSuchElementException;

import junit.framework.Assert;

import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;

/**
 *
 * @author M. Berthold, University of Konstanz
 */
public class Trycatch extends WorkflowTestCase {

    private NodeID m_try;
    private NodeID m_catch;
    private NodeID m_if;
    private NodeID m_endif;
    private NodeID m_endloop;
    private NodeID m_finalnode;

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        NodeID baseID = loadAndSetWorkflow();
        m_try = new NodeID(baseID, 10);
        m_catch = new NodeID(baseID, 8);
        m_if = new NodeID(baseID, 15);
        m_endif = new NodeID(baseID, 16);
        m_endloop = new NodeID(baseID, 13);
        m_finalnode = new NodeID(baseID, 7);
    }

    public void testExecuted() throws Exception {
    	executeAllAndWait();
    	// check node states - inactive interna/active end.
        checkState(m_try, InternalNodeContainerState.EXECUTED);
        checkState(m_catch, InternalNodeContainerState.EXECUTED);
        checkState(m_if, InternalNodeContainerState.EXECUTED);
        assertTrue(((SingleNodeContainer)(getManager().getNodeContainer(m_if))).isInactive());
        checkState(m_endif, InternalNodeContainerState.EXECUTED);
        assertTrue(((SingleNodeContainer)(getManager().getNodeContainer(m_endif))).isInactive());
        checkState(m_endloop, InternalNodeContainerState.EXECUTED);
        assertTrue(((SingleNodeContainer)(getManager().getNodeContainer(m_endloop))).isInactive());
        checkState(m_catch, InternalNodeContainerState.EXECUTED);
        assertFalse(((SingleNodeContainer)(getManager().getNodeContainer(m_catch))).isInactive());
        // check variable in scope and hiding outside
        SingleNodeContainer endifSNC = (SingleNodeContainer)(getManager().getNodeContainer(m_endif));
        try {
        	endifSNC.getNode().getNodeModel().peekFlowVariableString("innerScopeVariable");
        } catch (NoSuchElementException nsee) {
        	Assert.fail("Variable does not exist but should!");
        };
        SingleNodeContainer finalnodeSNC = (SingleNodeContainer)(getManager().getNodeContainer(m_finalnode));
        try {
        	finalnodeSNC.getNode().getNodeModel().peekFlowVariableString("innerScopeVariable");
        	Assert.fail("Variable exists but shouldn't!");
        } catch (NoSuchElementException nsee) {};
    }

}
