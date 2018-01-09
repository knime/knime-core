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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;

import java.io.File;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.util.FileUtil;
import org.knime.testing.node.blocking.BlockingRepository;

/** Tests fix for bug 4107: "Lazy" FileStoreCells are not properly saved/copied in Parallel Chunk Loops.
 * It executes the workflow half-way (partial loop), then saves, continues execution and then saves/loads. Without
 * the fix the workflow could not be executed further after loading.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class Bug4107_FileStoresInLoopsAfterSave extends WorkflowTestCase {

    private static final String LOCK_ID = "in_loop_block";
    private NodeID m_testFileStore_19;
    private NodeID m_loopEnd_22;
    private NodeID m_block_23;
    private NodeID m_innerLoopStart_21;
    private File m_tmpWorkflowDir;

    @Before
    public void setUp() throws Exception {
        // the id is used here and in the workflow (part of the settings)
        BlockingRepository.put(LOCK_ID, new ReentrantLock());
        m_tmpWorkflowDir = FileUtil.createTempDir(getClass().getSimpleName() + "-tempTestInstance");
        FileUtil.copyDir(getDefaultWorkflowDirectory(), m_tmpWorkflowDir);
        loadFlow();
    }

    private WorkflowManager loadFlow() throws Exception {
        NodeID baseID = loadAndSetWorkflow(m_tmpWorkflowDir);
        m_testFileStore_19 = new NodeID(baseID, 19);
        m_innerLoopStart_21 = new NodeID(baseID, 21);
        m_loopEnd_22 = new NodeID(baseID, 22);
        m_block_23 = new NodeID(baseID, 23);
        return getManager();
    }

    @Test
    public void testMain() throws Exception {
        WorkflowManager m = getManager();
        checkState(m, CONFIGURED);
        checkState(m_block_23, CONFIGURED);
        checkState(m_loopEnd_22, CONFIGURED);
        checkState(m_testFileStore_19, CONFIGURED);
        ReentrantLock execLock = BlockingRepository.get(LOCK_ID);
        execLock.lock();
        try {
            m.executeUpToHere(m_testFileStore_19);
            waitWhileNodeInExecution(m_innerLoopStart_21);
            checkState(m_testFileStore_19, InternalNodeContainerState.CONFIGURED_MARKEDFOREXEC);
            m.save(m_tmpWorkflowDir, new ExecutionMonitor(), true);
            assertTrue(m.isDirty()); // still executing
        } finally {
            execLock.unlock();
        }
        waitWhileNodeInExecution(m_testFileStore_19);
        checkState(m, EXECUTED);
        m.save(m_tmpWorkflowDir, new ExecutionMonitor(), true);
        assertFalse(m.isDirty());
        closeWorkflow();
        m = loadFlow();
        checkState(m_testFileStore_19, EXECUTED);
        reset(m_testFileStore_19);
        executeAllAndWait();
        NodeMessage nodeMessage = m.getNodeContainer(m_testFileStore_19).getNodeMessage();
        assertEquals("Unexpected message: " + nodeMessage, NodeMessage.Type.RESET, nodeMessage.getMessageType());
        checkState(m, EXECUTED);
    }

    /** {@inheritDoc} */
    @Override
    @After
    public void tearDown() throws Exception {
        BlockingRepository.remove(LOCK_ID);
        super.tearDown();
        if (!FileUtil.deleteRecursively(m_tmpWorkflowDir)) {
            getLogger().errorWithFormat("Could not fully delete the temporary workflow dir \"%s\" " +
            		"- directory does %sstill exists", m_tmpWorkflowDir.getAbsolutePath(),
            		m_tmpWorkflowDir.exists() ? " " : "not ");
        }
    }

}
