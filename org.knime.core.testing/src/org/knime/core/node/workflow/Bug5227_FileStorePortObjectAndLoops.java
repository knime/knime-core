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
 * Tests the handling of file store port objects, which are collected/created in loop end nodes.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class Bug5227_FileStorePortObjectAndLoops extends WorkflowTestCase {

    private NodeID m_dataGen1;
    private NodeID m_loopStart3;
    private NodeID m_createFS2;
    private NodeID m_loopEndFS4;
    private NodeID m_fsPortToCell5;
    private NodeID m_testFS6;

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // will save the workflow in one of the test ...don't write SVN folder
        NodeID baseID = loadAndSetWorkflow();
        m_dataGen1 = new NodeID(baseID, 1);
        m_createFS2 = new NodeID(baseID, 2);
        m_loopStart3 = new NodeID(baseID, 3);
        m_loopEndFS4 = new NodeID(baseID, 4);
        m_fsPortToCell5 = new NodeID(baseID, 5);
        m_testFS6 = new NodeID(baseID, 6);
    }

    public void testStatusAndCountAfterLoad() throws Exception {
        checkState(m_loopStart3, InternalNodeContainerState.EXECUTED);
        checkState(m_loopEndFS4, InternalNodeContainerState.EXECUTED);
        checkState(m_fsPortToCell5, InternalNodeContainerState.EXECUTED);
        checkState(m_testFS6, InternalNodeContainerState.CONFIGURED);
        File startFSDir = getFileStoresDirectory(m_loopStart3);
        assertEquals(10 + (5 - 1) * 5, countFilesInDirectory(startFSDir));
        // there should be no other nodes having a file store w/ directory
        for (SingleNodeContainer snc : iterateSNCs(getManager(), true)) {
            final NodeID id = snc.getID();
            if (!id.equals(m_loopStart3)) {
                File fsDir = getFileStoresDirectory(id);
                assertNull(fsDir);
            }
        }
    }

    public void testExecuteAllAndCountFileStores() throws Exception {
        checkState(m_testFS6, InternalNodeContainerState.CONFIGURED);
        assertEquals(1, getWriteFileStoreHandlers().size());
        executeAllAndWait();
        checkState(m_testFS6, InternalNodeContainerState.EXECUTED);
        checkState(getManager(), InternalNodeContainerState.EXECUTED);
    }

    public void testReExecuteAll() throws Exception {
        reset(m_dataGen1);
        executeAllAndWait();
        checkState(getManager(), InternalNodeContainerState.EXECUTED);
        File startFSDir = getFileStoresDirectory(m_loopStart3);
        assertEquals(10 + (5 - 1) * 5, countFilesInDirectory(startFSDir));
    }

    public void testReExecuteTestNodeAfterLoad() throws Exception {
        reset(m_testFS6);
        checkState(m_testFS6, InternalNodeContainerState.CONFIGURED);
        executeAndWait(m_testFS6);
        checkState(m_testFS6, InternalNodeContainerState.EXECUTED);
    }

}
