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



/**
 *
 * @author wiswedel, University of Konstanz
 */
public class Bug4409_inactiveInnerLoop extends WorkflowTestCase {

    private NodeID m_outerLoopStart_137;
    private NodeID m_outerLoopEnd_138;
    private NodeID m_innerLoopStart_767;
    private NodeID m_innerLoopEnd_780;
    private NodeID m_innerJavaSnippet_782;

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        NodeID baseID = loadAndSetWorkflow();
        m_outerLoopStart_137 = new NodeID(baseID, 137);
        m_outerLoopEnd_138 = new NodeID(baseID, 138);
        m_innerLoopStart_767 = new NodeID(baseID, 767);
        m_innerLoopEnd_780 = new NodeID(baseID, 780);
        m_innerJavaSnippet_782 = new NodeID(baseID, 782);
    }
    
    public void testExecuteAll() throws Exception {
        checkState(m_outerLoopEnd_138, InternalNodeContainerState.CONFIGURED);
        executeAllAndWait();
        checkState(m_outerLoopEnd_138, InternalNodeContainerState.EXECUTED);
    }

    public void testExecuteAllThenReset() throws Exception {
        executeAllAndWait();
        reset(m_innerLoopStart_767); // propagates to outer loop
        checkState(m_outerLoopStart_137, InternalNodeContainerState.CONFIGURED);
        checkState(m_innerLoopStart_767, InternalNodeContainerState.CONFIGURED);
    }
    
    public void testPartialExecuteThenResetInnerLoop() throws Exception {
        executeAndWait(m_innerLoopEnd_780);
        checkState(m_innerLoopEnd_780, InternalNodeContainerState.EXECUTED);
        checkState(m_innerLoopStart_767, InternalNodeContainerState.EXECUTED);
        reset(m_innerLoopEnd_780);
        checkState(m_innerLoopEnd_780, InternalNodeContainerState.CONFIGURED);
        // this currently doesn't work: if you reset an inactive loop end 
        // it doesn't reset its loop start - not sure if it should?
        // checkState(m_innerLoopStart_767, InternalNodeContainerState.CONFIGURED);
        
        // execute all again.
        executeAllAndWait();
        checkState(m_outerLoopEnd_138, InternalNodeContainerState.EXECUTED);
    }
    
}
