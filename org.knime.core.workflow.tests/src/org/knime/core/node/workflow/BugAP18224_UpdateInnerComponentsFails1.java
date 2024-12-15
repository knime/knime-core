MISSINGpackage org.knime.core.node.workflow;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.ExecutionMonitor;

/**
 * Tests the new update components functionality of update errors being masked
 * when a parent update exists. In the workflow "mainWorkflow" one component
 * instance of B contains a non-existent inner component A, but it will be found
 * once B is updated properly.
 * 
 * As an indicator, the updated inner component a returns a single row String
 * value with "updated", the non-updated version would return an empty table.
 * Following that, in
 * {@link BugAP18224_UpdateInnerComponentsFails1#testUpdatingFixesInnerComponentsError()},
 * the first checkState checks for non-equal inputs, the second one for equal
 * inputs.
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 */
public class BugAP18224_UpdateInnerComponentsFails1 extends WorkflowTestCase {

	private NodeID m_olderVersionComponent, m_newerVersionComponent, m_tableDiff;

	/**
	 * Initialize all node IDs
	 * 
	 * @throws Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
		final var workspaceDir = getDefaultWorkflowDirectory();
		var baseId = loadAndSetWorkflowInWorkspace(new File(workspaceDir, "mainWorkflow"), workspaceDir);
		m_olderVersionComponent = new NodeID(baseId, 1);
		m_newerVersionComponent = new NodeID(baseId, 4);
		m_tableDiff = new NodeID(baseId, 3);
	}

	@Test
	public void testUpdatingFixesInnerComponentsError() throws Exception {
		executeAndWait(m_tableDiff);
		checkState(m_tableDiff, InternalNodeContainerState.CONFIGURED); // fails
		var loadHelper = new WorkflowLoadHelper(true, getManager().getContextV2());

		assertTrue(getManager().checkUpdateMetaNodeLink(m_olderVersionComponent, loadHelper),
				"Expected meta node update available");
		getManager().updateMetaNodeLink(m_olderVersionComponent, new ExecutionMonitor(), loadHelper);
		assertFalse(getManager().checkUpdateMetaNodeLink(m_newerVersionComponent, loadHelper),
				"Expected no meta node update to be available");

		executeAndWait(m_tableDiff);
		checkState(m_tableDiff, InternalNodeContainerState.EXECUTED); // runs successful
	}
}