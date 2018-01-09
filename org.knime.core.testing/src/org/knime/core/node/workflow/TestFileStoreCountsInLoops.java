/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;
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

    @Before
    public void setUp() throws Exception {
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

    @Test
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

        System.gc(); // we have seen problems on windows where the folder still exists, hopefully this will help
        assertFalse("Directory must have been deleted: " + startFSDir.getAbsolutePath()
                + " found " + (startFSDir.exists() ? countFilesInDirectory(startFSDir) : "-1")
                + " files", startFSDir.exists());
    }

    @Test
    public void testExecuteAllAndCountFileStoresWithUnconnectedNestedLoop() throws Exception {
        deleteConnection(m_loopEndOuter32, 0);
        deleteConnection(m_testFS31, 0);
        testExecuteAllAndCountFileStores();
    }

    @Test
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
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        if (m_workflowDirTemp != null && m_workflowDirTemp.isDirectory()) {
            FileUtil.deleteRecursively(m_workflowDirTemp);
            m_workflowDirTemp = null;
        }
    }

}
