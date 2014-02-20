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
 *   19.06.2012 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.io.File;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class Bug4352_FileStoreCountInLoopJoin extends WorkflowTestCase {

    private NodeID m_testFS1;
    private NodeID m_createFS9;
    private NodeID m_createFS7;
    private NodeID m_loopStart6;

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // will save the workflow in one of the test ...don't write SVN folder
        NodeID baseID = loadAndSetWorkflow();
        m_testFS1 = new NodeID(baseID, 1);
        m_createFS9 = new NodeID(baseID, 9);
        m_createFS7 = new NodeID(baseID, 7);
        m_loopStart6 = new NodeID(baseID, 6);
    }

    public void testExecuteAllAndCountFileStores() throws Exception {
        checkState(m_testFS1, InternalNodeContainerState.IDLE);
        assertEquals(0, getWriteFileStoreHandlers().size());
        executeAllAndWait();
        checkState(m_testFS1, InternalNodeContainerState.EXECUTED);
        checkState(m_createFS7, InternalNodeContainerState.EXECUTED);
        checkState(m_createFS9, InternalNodeContainerState.EXECUTED);

        File startFSDir = getFileStoresDirectory(m_loopStart6);
        assertEquals(6 + 6 + 2, countFilesInDirectory(startFSDir));

        // there should be other nodes having a file store w/ directory
        for (SingleNodeContainer snc : iterateSNCs(getManager(), true)) {
            final NodeID id = snc.getID();
            if (!id.equals(m_loopStart6)) {
                File fsDir = getFileStoresDirectory(id);
                assertNull(fsDir);
            }
        }

    }

}
