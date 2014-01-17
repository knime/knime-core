/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by 
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

import org.knime.core.data.util.memory.MemoryObjectTracker;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class Bug4385_FileStoresWithLowMemory extends WorkflowTestCase {

    private NodeID m_dataGenStart1;
    private NodeID m_testAfterLoop2;
    private NodeID m_testSingle8;

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        NodeID baseID = loadAndSetWorkflow();
        m_dataGenStart1 = new NodeID(baseID, 1);
        m_testAfterLoop2 = new NodeID(baseID, 2);
        m_testSingle8 = new NodeID(baseID, 8);
    }

    public void testExecuteFlow() throws Exception {
        checkState(InternalNodeContainerState.CONFIGURED);
        executeAllAndWait();
        for (NodeContainer nc : getManager().getNodeContainers()) {
            assertEquals("Node " + nc.getNameWithID() + ": " + nc.getNodeMessage(), 
                NodeMessage.Type.RESET, nc.getNodeMessage().getMessageType());
        }
        MemoryObjectTracker.getInstance().simulateMemoryAlert();
        checkState(InternalNodeContainerState.EXECUTED);
        reset(m_testAfterLoop2, m_testSingle8);
        executeAndWait(m_testAfterLoop2, m_testSingle8);
        checkState(InternalNodeContainerState.EXECUTED);
        // interesting part (where the error used to occur) happens in shutdown.
        // it threw an exception as documented in the bug report
    }
    
    @Override
    protected void tearDown() throws Exception {
        // m_manger is shut down, which caused exceptions 
        super.tearDown();
    }
    
    private void checkState(final InternalNodeContainerState state) throws Exception {
        checkState(m_dataGenStart1, state);
        checkState(m_testAfterLoop2, state);
        checkState(m_testSingle8, state);
    }

}
