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

import java.io.File;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.util.FileUtil;

/**
 * Tests Save As.
 * @author wiswedel, University of Konstanz
 */
public class Bug5405_WorkflowLocationAfterSaveAs extends WorkflowTestCase {

    private NodeID m_fileReader1;
    private NodeID m_fileReader2;
    private NodeID m_diffChecker3;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_fileReader1 = new NodeID(baseID, 1);
        m_fileReader2 = new NodeID(baseID, 2);
        m_diffChecker3 = new NodeID(baseID, 3);
    }

    /** Basic tests that execution works and the NC dir and workflow context folder are the same. */
    @Test
    public void testExecAfterLoad() throws Exception {
        WorkflowManager manager = getManager();
        ContainerTable fileReaderTable = getExecuteFileReaderTable();
        // tables are not extracted to workflow temp space after load (lazy init)
        Assert.assertFalse(fileReaderTable.isOpen());
        checkState(m_fileReader2, InternalNodeContainerState.EXECUTED);
        checkStateOfMany(InternalNodeContainerState.CONFIGURED, m_fileReader1, m_diffChecker3);
        Assert.assertNotNull(manager.getContext());
        Assert.assertEquals(manager.getNodeContainerDirectory().getFile(), manager.getContext().getCurrentLocation());
        executeAndWait(m_diffChecker3);
        Assert.assertTrue(fileReaderTable.isOpen());
        checkStateOfMany(InternalNodeContainerState.EXECUTED, m_fileReader2, m_fileReader1, m_diffChecker3);
    }

    /** Loads the workflow, saves it to new location, then executes. */
    @Test
    public void testExecAfterSaveAs() throws Exception {
        WorkflowManager manager = getManager();
        ContainerTable fileReaderTable = getExecuteFileReaderTable();
        // tables are not extracted to workflow temp space after load (lazy init)
        Assert.assertFalse(fileReaderTable.isOpen());
        Assert.assertNotNull(manager.getContext());
        Assert.assertEquals(manager.getNodeContainerDirectory().getFile(), manager.getContext().getCurrentLocation());
        File saveAsFolder = FileUtil.createTempDir(getClass().getName());
        saveAsFolder.delete();

        WorkflowContext.Factory fac = manager.getContext().createCopy().setCurrentLocation(saveAsFolder);
        manager.saveAs(fac.createContext(), new ExecutionMonitor());
        Assert.assertEquals(saveAsFolder, manager.getNodeContainerDirectory().getFile());
        Assert.assertEquals(saveAsFolder, manager.getContext().getCurrentLocation());

        // if this fails (= assertion thrown) this means the workflow format has changed and all nodes are dirty
        // when save-as is called.
        Assert.assertFalse(fileReaderTable.isOpen());

        executeAndWait(m_diffChecker3);
        checkStateOfMany(InternalNodeContainerState.EXECUTED, m_fileReader2, m_fileReader1, m_diffChecker3);
        Assert.assertTrue(fileReaderTable.isOpen());
    }

    private ContainerTable getExecuteFileReaderTable() {
        WorkflowManager manager = getManager();
        final WorkflowDataRepository dataRepository = manager.getWorkflowDataRepository();
        Map<Integer, ContainerTable> globalTableRepository = dataRepository.getGlobalTableRepository();
        Assert.assertEquals(1, globalTableRepository.size());
        return globalTableRepository.values().iterator().next();
    }

}
