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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.FileUtil;

/**
 * Tests that a "failing" recursive loop workflow can properly be saved and restored.
 * https://knime-com.atlassian.net/browse/AP-13748
 * 
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
public class BugAP13748_RecursiveLoopErrorCausesSaveFailure extends WorkflowTestCase {

	@TempDir
	public File m_folder;

	private NodeID m_recursiveLoopEnd_3;
	private NodeID m_recursiveLoopEnd_8;
	private NodeID m_recursiveLoopEnd_12;

	@BeforeEach
	public void setUp() throws Exception {
		File defaultWorkflowDirectory = getDefaultWorkflowDirectory();
		File workflowDir = new File(m_folder, defaultWorkflowDirectory.getName());
		// copy the workflow because it gets saved as part of the test...
		FileUtil.copyDir(defaultWorkflowDirectory, workflowDir);
		loadWorkflowAndAssign(workflowDir);
		checkStateOfMany(InternalNodeContainerState.CONFIGURED, getAllNodes());
	}

	@Test
	public void testExecuteSaveRestore() throws Exception {
		executeAllAndWait();
		NodeID[] allNodesButEndNodes = 
				ArrayUtils.removeElements(getAllNodes(), m_recursiveLoopEnd_3, m_recursiveLoopEnd_8);
		checkStateOfMany(InternalNodeContainerState.EXECUTED, allNodesButEndNodes);
		checkStateOfMany(InternalNodeContainerState.CONFIGURED, m_recursiveLoopEnd_3, m_recursiveLoopEnd_8);
		WorkflowManager manager = getManager();
		assertThat("Loop End 3 message type",
				manager.getNodeContainer(m_recursiveLoopEnd_3).getNodeMessage().getMessageType(),
				is(NodeMessage.Type.ERROR));
		assertThat("Loop End 8 message type",
				manager.getNodeContainer(m_recursiveLoopEnd_8).getNodeMessage().getMessageType(),
				is(NodeMessage.Type.ERROR));
		ReferencedFile workflowDir = manager.getWorkingDir();
		manager.save(workflowDir.getFile(), new ExecutionMonitor(), true);
		closeWorkflow();
		assertThat("WorkflowManager instance", getManager(), is((WorkflowManager)null));

		WorkflowLoadResult loadResult = loadWorkflowAndAssign(workflowDir.getFile());
		manager = getManager();
		checkStateOfMany(InternalNodeContainerState.EXECUTED, allNodesButEndNodes);
		checkStateOfMany(InternalNodeContainerState.CONFIGURED, m_recursiveLoopEnd_3, m_recursiveLoopEnd_8);
		// error message is not persisted
		assertThat("Loop End 3 message type",
				manager.getNodeContainer(m_recursiveLoopEnd_3).getNodeMessage().getMessageType(),
				is(NodeMessage.Type.RESET));
		assertThat("LoadResult has errors", loadResult.hasErrors(), is(false));
		assertThat("LoadResult has warnings", loadResult.hasWarningEntries(), is(false));

	}

	private WorkflowLoadResult loadWorkflowAndAssign(File workflowDir) throws Exception {
		WorkflowLoadResult loadResult = loadWorkflow(workflowDir, new ExecutionMonitor());
		setManager(loadResult.getWorkflowManager());
		NodeID wfId = getManager().getID();
		m_recursiveLoopEnd_3 = wfId.createChild(3);
		m_recursiveLoopEnd_8 = wfId.createChild(8);
		m_recursiveLoopEnd_12 = wfId.createChild(12);
		return loadResult;
	}

	/**
	 * @return
	 */
	private NodeID[] getAllNodes() {
		return getManager().getNodeContainers().stream().map(NodeContainer::getID).toArray(NodeID[]::new);
	}


}