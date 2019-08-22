/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Aug 22, 2019 (hornm): created
 */
package org.knime.core.node.workflow;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.FileUtil;

/**
 * Tests the execution of streamed components and the availability of the output data on re-load of the workflow.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class BugAP9284_RestoreFileStoreCellsStreaming extends WorkflowTestCase {

    private File m_workflowDir;

    private NodeID m_metanode_26;

    private NodeID m_loopEnd_30;

    private NodeID m_testFileStoreColumn_19;

    private NodeID m_testFileStoreColumn_31;

    private NodeID m_componentOutput_26_0_19;

    private NodeID m_createFileStoreColumn_26_0_20;

    private NodeID m_chunkLoopStart_28;

    @Before
    public void setup() throws Exception {
        m_workflowDir = FileUtil.createTempDir(getClass().getSimpleName());
        FileUtil.copyDir(getDefaultWorkflowDirectory(), m_workflowDir);
        initWorkflowFromTemp();
    }

    private WorkflowLoadResult initWorkflowFromTemp() throws Exception {
        WorkflowLoadResult loadResult = loadWorkflow(m_workflowDir, new ExecutionMonitor());
        setManager(loadResult.getWorkflowManager());
        NodeID baseID = getManager().getID();
        m_metanode_26 = new NodeID(baseID, 26);
        m_loopEnd_30 = new NodeID(baseID, 30);
        m_testFileStoreColumn_19 = new NodeID(baseID, 19);
        m_testFileStoreColumn_31 = new NodeID(baseID, 31);
        m_chunkLoopStart_28 = new NodeID(baseID, 28);
        m_componentOutput_26_0_19 = NodeID.fromString(baseID + ":26:0:19");
        m_createFileStoreColumn_26_0_20 = NodeID.fromString(baseID + ":26:0:20");
        return loadResult;
    }

    @Test
    public void testPartialExecuteSaveLoad() throws Exception {
        //partially execute workflow, save, check, close
        checkState(m_chunkLoopStart_28, InternalNodeContainerState.CONFIGURED);
        executeAndWait(m_metanode_26, m_loopEnd_30);
        checkState(m_metanode_26, InternalNodeContainerState.EXECUTED);
        checkState(m_loopEnd_30, InternalNodeContainerState.EXECUTED);
        getManager().save(m_workflowDir, new ExecutionMonitor(), true);
        countFileStoreFiles();
        closeWorkflow();

        //reload workflow and execute the rest
        initWorkflowFromTemp();
        checkState(m_metanode_26, InternalNodeContainerState.EXECUTED);
        checkState(m_loopEnd_30, InternalNodeContainerState.EXECUTED);
        //execute the rest
        executeAllAndWait();
        checkState(m_testFileStoreColumn_19, InternalNodeContainerState.EXECUTED);
        checkState(m_testFileStoreColumn_31, InternalNodeContainerState.EXECUTED);
        countFileStoreFiles();
    }

    private void countFileStoreFiles() throws Exception {
        //make sure that all files stores are stored into the component output filestore folder
        File fileStoresDirectory = getFileStoresDirectory(m_componentOutput_26_0_19);
        assertNotNull("no file store directory for component output", fileStoresDirectory);
        assertThat("unexpected number of files in component output filestore folder",
            countFilesInDirectory(fileStoresDirectory), is(16));
        //check that all the other nodes in a streamed component don't store any file stores
        assertNull(getFileStoresDirectory(m_createFileStoreColumn_26_0_20));
        //make sure that all files stores are stored into the component output filestore folder
        assertThat("unexpected number of files in loop start filestore folder",
            countFilesInDirectory(getFileStoresDirectory(m_chunkLoopStart_28)), is(16));
    }
}
