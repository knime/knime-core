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
 * History
 *   Jun 27, 2020 (benlaney): created
 */
package org.knime.core.node.workflow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.FileUtil;

/**
 * Tests {@link SubNodeContainer} behavior after JSONLayoutStringProviders were
 * added in v4.2.0.
 *
 * @author benlaney
 */
public class BugWEBP409_AddDefaultLayout extends WorkflowTestCase {

	private File m_workflowDir;

	private NodeID m_subNode1; // saved component with empty layout
	private NodeID m_subNode2; // saved component with layout

	/**
	 * Creates and copies the workflow into a temporary directory.
	 *
	 * @throws Exception
	 */
	@Before
	public void setup() throws Exception {
		m_workflowDir = FileUtil.createTempDir(getClass().getSimpleName());
		FileUtil.copyDir(getDefaultWorkflowDirectory(), m_workflowDir);
		initWorkflowFromTemp();
	}

	private void initWorkflowFromTemp() throws Exception {
		WorkflowLoadResult loadResult = loadWorkflow(m_workflowDir, new ExecutionMonitor());
		setManager(loadResult.getWorkflowManager());
		NodeID baseID = getManager().getID();
		m_subNode1 = new NodeID(baseID, 3);
		m_subNode2 = new NodeID(baseID, 4);
	}

	/**
	 * Main test for SubNode layout behavior after default layouts were added. Runs
	 * sub-test methods sequentially to simulate the life-cycle of a workflow.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDefaultSubNodeContainerLayout() throws Exception {
		testLoadingSavedLayouts();
		testDefaultLayoutCreatingSubNode();
		testSuccessfulExecution();
		testSaveAndLoadLayoutVersion();
	}

	/**
	 * Tests loading a workflow with components saved without default layouts.
	 * 
	 * @throws Exception
	 */
	public void testLoadingSavedLayouts() throws Exception {
		final WorkflowManager wfm = getManager();
		checkState(m_subNode1, InternalNodeContainerState.CONFIGURED);
		checkState(m_subNode2, InternalNodeContainerState.IDLE);
		SubNodeContainer container1 = (SubNodeContainer) findNodeContainer(m_subNode1);
		SubNodeContainer container2 = (SubNodeContainer) findNodeContainer(m_subNode2);
		assertNotNull(container1);
		assertNotNull(container2);
		assertTrue("Problem detecting saved, empty layouts",
				container1.getSubnodeLayoutStringProvider().isEmptyLayout());
		assertFalse("Problem detecting saved layouts", container2.getSubnodeLayoutStringProvider().isEmptyLayout());
		assertFalse("Missing layout version caused dirty workflow", wfm.isDirty());
	}

	/**
	 * Tests creating a new component after default layout behavior was added.
	 *
	 * @throws Exception
	 */
	public void testDefaultLayoutCreatingSubNode() throws Exception {
		final WorkflowManager wfm = getManager();
		SubNodeContainer container1 = (SubNodeContainer) findNodeContainer(m_subNode1);
		assertNotNull(container1);
		NodeID m_subNodeNew = new NodeID(wfm.getID(), 5);
		SubNodeContainer newComponent = SubNodeContainer.newSubNodeContainerFromMetaNodeContent(wfm, m_subNodeNew,
				container1.getWorkflowManager(), "Test_Node");
		assertNotNull(newComponent);
		assertTrue("Problem creating new component layouts layouts", newComponent.getSubnodeLayoutStringProvider()
				.checkOriginalContains("{\"parentLayoutLegacyMode\":false}"));
	}

	/**
	 * Test successful execution of loaded workflow.
	 * 
	 * @throws Exception
	 */
	public void testSuccessfulExecution() throws Exception {
		executeAllAndWait();
		checkStateOfMany(InternalNodeContainerState.EXECUTED, m_subNode1, m_subNode2);
	}

	/**
	 * Test saving and loading the workflow successfully.
	 *
	 * @throws Exception
	 */
	public void testSaveAndLoadLayoutVersion() throws Exception {
		WorkflowManager wfm = getManager();
		wfm.save(m_workflowDir, new ExecutionMonitor(), true);
		closeWorkflow();
		initWorkflowFromTemp();

		checkStateOfMany(InternalNodeContainerState.EXECUTED, m_subNode1, m_subNode2);
		SubNodeContainer container1 = (SubNodeContainer) findNodeContainer(m_subNode1);
		SubNodeContainer container2 = (SubNodeContainer) findNodeContainer(m_subNode2);
		assertNotNull(container1);
		assertNotNull(container2);
		assertTrue("Problem detecting saved, empty layouts",
				container1.getSubnodeLayoutStringProvider().isEmptyLayout());
		assertFalse("Problem detecting saved layouts", container2.getSubnodeLayoutStringProvider().isEmptyLayout());
		assertFalse("Missing layout version caused dirty workflow", wfm.isDirty());
	}
}
