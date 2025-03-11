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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;
import static org.knime.core.node.workflow.InternalNodeContainerState.IDLE;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.util.FileUtil;

/** Tests fix for bug 6339: "Loop End (Column Append)" may generate too long file path:
 * workflow can't be saved - problem on some Windows systems
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class Bug6339_LoopEndColAppend_PathTooLong extends WorkflowTestCase {

    private NodeID m_dataGen1;
    private NodeID m_loopEnd_5;
    private NodeID m_javaSnippet_7;
    private File m_tmpWorkflowDir;

    @BeforeEach
    public void setUp() throws Exception {
        m_tmpWorkflowDir = FileUtil.createTempDir(getClass().getSimpleName() + "-tempTestInstance");
        FileUtil.copyDir(getDefaultWorkflowDirectory(), m_tmpWorkflowDir);
        loadFlow();
    }

    private WorkflowManager loadFlow() throws Exception {
        NodeID baseID = loadAndSetWorkflow(m_tmpWorkflowDir);
        m_dataGen1 = new NodeID(baseID, 1);
        m_loopEnd_5 = new NodeID(baseID, 5);
        m_javaSnippet_7 = new NodeID(baseID, 7);
        return getManager();
    }

    /** Load workflow, execute, check, save, close, reopen, check, reset, execute, check. */
    @Test
    public void testMain() throws Exception {
        WorkflowManager m = getManager();
        checkState(m, IDLE);
        checkState(m_javaSnippet_7, IDLE);
        checkState(m_dataGen1, CONFIGURED);
        executeAndWait(m_loopEnd_5);
        m.save(m_tmpWorkflowDir, new ExecutionMonitor(), true);
        executeAllAndWait();
        checkState(getManager(), EXECUTED);

        m.save(m_tmpWorkflowDir, new ExecutionMonitor(), true);
        closeWorkflow();
        assertNull(getManager());
        loadAndSetWorkflow(m_tmpWorkflowDir);
        m = getManager();

        checkState(m, EXECUTED);
        reset(m_javaSnippet_7);
        checkState(m_javaSnippet_7, CONFIGURED);
        executeAllAndWait();
        checkState(m, EXECUTED);
    }

    /** {@inheritDoc} */
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        if (!FileUtil.deleteRecursively(m_tmpWorkflowDir)) {
            getLogger().errorWithFormat("Could not fully delete the temporary workflow dir \"%s\" " +
            		"- directory does %sstill exists", m_tmpWorkflowDir.getAbsolutePath(),
            		m_tmpWorkflowDir.exists() ? " " : "not ");
        }
    }

}