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
 *   19.06.2012 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.io.File;

import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.FileUtil;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class TestFileStoreCountsInLoops extends WorkflowTestCase {

    private NodeID m_dataGen1;
    private NodeID m_loopStartOuter15;
    private NodeID m_loopEndOuter32;
    private NodeID m_testFS31;
    private NodeID m_testFS47;
    private NodeID m_testFS88;
    private NodeID m_createFS8;
    private NodeID m_createFS89;
    private NodeID m_meta86;
    private File m_workflowDirTemp;

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        File workflowDirSVN = getDefaultWorkflowDirectory();
        // will save the workflow in one of the test ...don't write SVN folder
        m_workflowDirTemp = FileUtil.createTempDir(workflowDirSVN.getName());
        FileUtil.copyDir(workflowDirSVN, m_workflowDirTemp);
        initFlow();
    }

    /**
     * @throws Exception */
    private void initFlow() throws Exception {
        NodeID baseID = loadAndSetWorkflow(m_workflowDirTemp);
        m_dataGen1 = new NodeID(baseID, 1);
        m_loopStartOuter15 = new NodeID(baseID, 15);
        m_loopEndOuter32 = new NodeID(baseID, 32);
        m_testFS31 = new NodeID(baseID, 31);
        m_testFS47 = new NodeID(baseID, 47);
        m_testFS88 = new NodeID(baseID, 88);
        m_createFS8 = new NodeID(baseID, 8);
        m_createFS89 = new NodeID(baseID, 89);
        m_meta86 = new NodeID(baseID, 86);
    }

    public void testExecuteAllAndCountFileStores() throws Exception {
        checkState(m_dataGen1, InternalNodeContainerState.CONFIGURED);
        assertEquals(0, getWriteFileStoreHandlers().size());
        executeAllAndWait();
        checkState(m_testFS47, InternalNodeContainerState.EXECUTED);
        checkState(m_testFS88, InternalNodeContainerState.IDLE); // still unconnected
        checkState(m_createFS89, InternalNodeContainerState.EXECUTED);

        File startFSDir = getFileStoresDirectory(m_loopStartOuter15);
        assertEquals(100, countFilesInDirectory(startFSDir));

        // there should be other nodes having a file store w/ directory
        for (SingleNodeContainer snc : iterateSNCs(getManager(), true)) {
            final NodeID id = snc.getID();
            if (!id.equals(m_loopStartOuter15)) {
                File fsDir = getFileStoresDirectory(id);
                assertNull(fsDir);
            }
        }

        checkFileStoreAccessibility(m_testFS47);
        checkFileStoreAccessibility(m_testFS31);
        checkFileStoreAccessibility(m_createFS8);
        checkFileStoreAccessibility(m_createFS89);
        checkFileStoreAccessibility(m_meta86);

        // reset any node in body will reset entire loop
        getManager().resetAndConfigureNode(m_testFS31);
        checkState(m_loopStartOuter15, InternalNodeContainerState.CONFIGURED);

        assertFalse("Directory must have been deleted: "
                + startFSDir.getAbsolutePath(), startFSDir.exists());
    }

    public void testExecuteAllAndCountFileStoresWithUnconnectedNestedLoop() throws Exception {
        deleteConnection(m_loopEndOuter32, 0);
        deleteConnection(m_testFS31, 0);
        testExecuteAllAndCountFileStores();
    }

    public void testFileStoresAfterSaveAndReload() throws Exception {
        executeAllAndWait();
        WorkflowManager mgr = getManager();
        mgr.save(m_workflowDirTemp, new ExecutionMonitor(), true);
        mgr.getParent().removeNode(mgr.getID());
        initFlow();
        mgr = getManager(); // new instance

        checkState(m_loopStartOuter15, InternalNodeContainerState.EXECUTED);
        checkState(m_loopEndOuter32, InternalNodeContainerState.EXECUTED);
        checkState(m_testFS47, InternalNodeContainerState.EXECUTED);
        checkState(m_testFS88, InternalNodeContainerState.IDLE); // unconnected

        File startFSDir = getFileStoresDirectory(m_loopStartOuter15);
        assertNull(startFSDir); // not extracted yet.
        checkFileStoreAccessibility(m_testFS47);

        startFSDir = getFileStoresDirectory(m_loopStartOuter15);
        assertNotNull(startFSDir);
        assertEquals(100, countFilesInDirectory(startFSDir));

        mgr.addConnection(m_testFS31, 1, m_testFS88, 1);
        executeAndWait(m_testFS88);
        checkState(m_testFS88, InternalNodeContainerState.CONFIGURED); // failure
        final NodeMessage msg = mgr.getNodeContainer(m_testFS88).getNodeMessage();
        assertEquals(NodeMessage.Type.ERROR, msg.getMessageType());
        assertTrue(msg.getMessage().contains("was restored"));

        assertNotNull(startFSDir);
        assertEquals(100, countFilesInDirectory(startFSDir));

    }

    private void checkFileStoreAccessibility(final NodeID... ncs) throws Exception {
        WorkflowManager mgr = getManager();
        checkState(m_testFS47, InternalNodeContainerState.EXECUTED);

        for (NodeID id : ncs) {
            boolean isUpstreamMeta = mgr.getNodeContainer(id) instanceof WorkflowManager;
            mgr.addConnection(id, isUpstreamMeta ? 0 : 1, m_testFS88, 1);
            executeAndWait(m_testFS88);
            checkState(m_testFS88, InternalNodeContainerState.EXECUTED);
            deleteConnection(m_testFS88, 1);
        }
    }


    /** {@inheritDoc} */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (m_workflowDirTemp != null && m_workflowDirTemp.isDirectory()) {
            FileUtil.deleteRecursively(m_workflowDirTemp);
            m_workflowDirTemp = null;
        }
    }

}
