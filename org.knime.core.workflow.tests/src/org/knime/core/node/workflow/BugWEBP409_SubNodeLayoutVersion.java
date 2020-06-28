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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.util.FileUtil;
import org.knime.core.util.LoadVersion;

/**
 * Tests to load and save workflows created before layout versions were
 * introduced.
 *
 * @author benlaney
 */
public class BugWEBP409_SubNodeLayoutVersion extends WorkflowTestCase {

	private File m_workflowDir;

	private NodeID m_subNode1;
	private NodeID m_subNode2;

	private LoadVersion m_initialWorkflowVersion;
	private String m_initialLayoutString;

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
		m_initialWorkflowVersion = LoadVersion.V4010;
	}

	private void initWorkflowFromTemp() throws Exception {
		WorkflowLoadResult loadResult = loadWorkflow(m_workflowDir, new ExecutionMonitor());
		setManager(loadResult.getWorkflowManager());
		NodeID baseID = getManager().getID();
		m_subNode1 = new NodeID(baseID, 3);
		m_subNode2 = new NodeID(baseID, 4);
	}

	/**
	 * Main test method to run the test suite in order. Tests loading, executing,
	 * saving and reloading SubNodes from older workflows to catch any dirty
	 * workflows or compatibility exceptions which may be thrown.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSubNodeLayoutVersions() throws Exception {
		testLoadWithoutLayoutVersion();
		testSuccessfulExecution();
		testSaveAndLoadLayoutVersion();
	}

	/**
	 * Ensure that workflows created before layout versions were introduced can be
	 * properly loaded without being dirtied.
	 * 
	 * @throws Exception
	 */
	private void testLoadWithoutLayoutVersion() throws Exception {
		final WorkflowManager wfm = getManager();
		checkState(m_subNode1, InternalNodeContainerState.CONFIGURED);
		checkState(m_subNode2, InternalNodeContainerState.IDLE);
		SubNodeContainer container1 = (SubNodeContainer) findNodeContainer(m_subNode1);
		SubNodeContainer container2 = (SubNodeContainer) findNodeContainer(m_subNode2);
		assertNotNull(container1);
		assertNotNull(container2);
		assertEquals("Problem initializing layout version", container1.getLayoutVersion(),
				m_initialWorkflowVersion.getVersionString());
		assertEquals("Problem initializing layout version", container2.getLayoutVersion(),
				m_initialWorkflowVersion.getVersionString());
		assertTrue(container1.getLayoutJSONString().isEmpty());
		assertFalse(container2.getLayoutJSONString().isEmpty());
		assertFalse("Missing layout version caused dirty workflow", wfm.isDirty());
	}

	/**
	 * Test that a SubNode component with a saved before layout versioning and with
	 * a layout will be updated when it's executed.
	 * 
	 * @throws Exception
	 */
	private void testSuccessfulExecution() throws Exception {
		SubNodeContainer container2 = (SubNodeContainer) findNodeContainer(m_subNode2);
		m_initialLayoutString = container2.getLayoutJSONString();
		executeAllAndWait();
		checkStateOfMany(InternalNodeContainerState.EXECUTED, m_subNode1, m_subNode2);
	}

	/**
	 * Ensure workflows can be saved with layout versions and loaded again.
	 * Additionally, layout versions should conform to a similar and comparable
	 * schema as the {@link LoadVersion} system used to track workflow modification.
	 *
	 * @throws Exception
	 */
	private void testSaveAndLoadLayoutVersion() throws Exception {
		WorkflowManager wfm = getManager();
		wfm.save(m_workflowDir, new ExecutionMonitor(), true);
		closeWorkflow();
		initWorkflowFromTemp();
		wfm = getManager(); // update manager after save, close and reload
		assertTrue("Workflow not saved properly", m_initialWorkflowVersion.isOlderThan(wfm.getLoadVersion()));
		checkStateOfMany(InternalNodeContainerState.EXECUTED, m_subNode1, m_subNode2);
		SubNodeContainer container1 = (SubNodeContainer) findNodeContainer(m_subNode1);
		SubNodeContainer container2 = (SubNodeContainer) findNodeContainer(m_subNode2);
		String layoutVersion1 = container1.getLayoutVersion();
		String layoutVersion2 = container1.getLayoutVersion();
		assertEquals("Layout version saved inconsistently", layoutVersion1, layoutVersion2);
		assertEquals("Layout version not saved correctly", layoutVersion1, m_initialWorkflowVersion.getVersionString());
		assertEquals("Layout version not saved correctly", layoutVersion1, m_initialWorkflowVersion.getVersionString());
		Optional<LoadVersion> mappedLoadVersion1 = LoadVersion.get(layoutVersion1);
		Optional<LoadVersion> mappedLoadVersion2 = LoadVersion.get(layoutVersion1);
		assertTrue("Layout version cannot be mapped to load version", mappedLoadVersion1.isPresent());
		assertTrue("Layout version cannot be mapped load version", mappedLoadVersion2.isPresent());
		assertTrue("Layout version does not conform to schema",
				mappedLoadVersion1.get().isOlderThan(wfm.getLoadVersion()));
		assertTrue("Layout version does not conform to schema",
				mappedLoadVersion2.get().isOlderThan(wfm.getLoadVersion()));
		assertTrue("Default layout incorrectly saved", container1.getLayoutJSONString().isEmpty());
		assertFalse("Saved layout incorrectly deleted", container2.getLayoutJSONString().isEmpty());
		assertEquals("Layout version incorrectly saved as updated", container2.getLayoutJSONString(),
				m_initialLayoutString);
		assertFalse("Reloading versions layouts caused dirty workflow", wfm.isDirty());
	}
}
