/*
 * ------------------------------------------------------------------------
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;
import static org.knime.core.node.workflow.InternalNodeContainerState.IDLE;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.util.FileUtil;

/**
 * Compares output of a streamed subnode to non-streamed version. Then saves, loads, checks again.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class TestSubnode_StreamingPortObject extends WorkflowTestCase {

    private NodeID m_streamedSubnode_8;
    private NodeID m_nonStreamedMetanode_10;
    private NodeID m_tableViewInSubNode_8_8;
    private File m_tmpWorkflowDir;

    @Before
    public void setUp() throws Exception {
        m_tmpWorkflowDir = FileUtil.createTempDir(getClass().getSimpleName() + "-tempTestInstance");
        FileUtil.copyDir(getDefaultWorkflowDirectory(), m_tmpWorkflowDir);
        loadFlow();
    }

    private WorkflowManager loadFlow() throws Exception {
        NodeID baseID = loadAndSetWorkflow(m_tmpWorkflowDir);
        m_streamedSubnode_8 = baseID.createChild(8);
        m_nonStreamedMetanode_10 = baseID.createChild(10);
        m_tableViewInSubNode_8_8 = m_streamedSubnode_8.createChild(0).createChild(8);
        return getManager();
    }

    /** Just run and compare.
     * @throws Exception ...*/
    @Test
    public void testExecuteAndCompare() throws Exception {
        WorkflowManager manager = getManager();
        checkState(manager, IDLE);
        checkState(m_streamedSubnode_8, IDLE);

        executeAllAndWait();
        checkState(manager, EXECUTED);
        checkState(m_streamedSubnode_8, EXECUTED);
        checkTableView();
    }

    /** Run, save all, close, load, reset meta node, re-execute all,
     * make sure the subnode output is loaded correctly. */
    @Test
    public void testExecSaveLoadCheck() throws Exception {
        WorkflowManager manager = getManager();
        executeAllAndWait();
        manager.save(m_tmpWorkflowDir, new ExecutionMonitor(), true);

        closeWorkflow();
        assertNull(getManager());
        manager = loadFlow();
        checkState(m_streamedSubnode_8, EXECUTED);
        checkTableView();
        manager.resetAndConfigureNode(m_nonStreamedMetanode_10);
        manager.executeAllAndWaitUntilDone();
        checkState(manager, EXECUTED);
        checkTableView();
    }


    private void checkTableView() throws Exception {
        NativeNodeContainer tableViewNNC = (NativeNodeContainer)findNodeContainer(m_tableViewInSubNode_8_8);
        checkState(tableViewNNC, EXECUTED);
        BufferedDataTable table = (BufferedDataTable)tableViewNNC.getNode().getInternalHeldPortObjects()[0];
        assertNotNull(table);
        try (CloseableRowIterator it = table.iterator()) {
            assertTrue(it.hasNext());
            assertNotNull(it.next());
        }
    }

    /** {@inheritDoc} */
    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        if (!FileUtil.deleteRecursively(m_tmpWorkflowDir)) {
            getLogger().errorWithFormat("Could not fully delete the temporary workflow dir \"%s\" " +
            		"- directory does %sstill exists", m_tmpWorkflowDir.getAbsolutePath(),
            		m_tmpWorkflowDir.exists() ? " " : "not ");
        }
    }

}
