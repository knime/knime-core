package org.knime.core.node.workflow;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;

/**
 * Tests the new update components functionality of update errors being masked
 * when a parent update exists. In the workflow "mainWorkflow" one component
 * instance of B contains a non-existent inner component A, and for this test
 * it won't be found upon the outer component's update, expecting an IOException.
 * 
 * As an indicator, the updated inner component a returns a single row String
 * value with "updated", the non-updated version would return an empty table.
 * Following that, in
 * {@link BugAP18224_UpdateInnerComponentsFails2#testUpdatingFixesInnerComponentsError()},
 * the first checkState checks for non-equal inputs, the second one for equal
 * inputs.
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 */
public class BugAP18224_UpdateInnerComponentsFails2 extends WorkflowTestCase {

	private NodeID m_olderVersionComponent, m_newerVersionComponent, m_tableDiff;

	/**
	 * Initialize all node IDs
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		final var workspaceDir = getDefaultWorkflowDirectory();
		var baseId = loadAndSetWorkflowInWorkspace(new File(workspaceDir, "mainWorkflow"), workspaceDir);
		m_olderVersionComponent = new NodeID(baseId, 1);
		m_newerVersionComponent = new NodeID(baseId, 4);
		m_tableDiff = new NodeID(baseId, 3);
	}

	@Test
	public void testUpdatingInnerComponentsErrorPersists() throws Exception {
		executeAndWait(m_tableDiff);
		checkState(m_tableDiff, InternalNodeContainerState.CONFIGURED); // definitely fails
		var loadHelper = new WorkflowLoadHelper(true, getManager().getContextV2());

		assertTrue("Expected meta node update available",
				getManager().checkUpdateMetaNodeLink(m_olderVersionComponent, loadHelper));
		getManager().updateMetaNodeLink(m_olderVersionComponent, new ExecutionMonitor(), loadHelper);
		assertThrows("Expect an IOException to be thrown because node template is non-existent", IOException.class,
				() -> getManager().checkUpdateMetaNodeLink(m_newerVersionComponent, loadHelper));

		executeAndWait(m_tableDiff);
		checkState(m_tableDiff, InternalNodeContainerState.EXECUTED);
	}
}
