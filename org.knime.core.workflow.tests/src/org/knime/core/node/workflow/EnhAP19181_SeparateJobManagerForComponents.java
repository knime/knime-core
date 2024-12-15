MISSINGpackage org.knime.core.node.workflow;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.RETURNS_MOCKS;

import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.exec.AbstractThreadNodeExecutionJobManager;
import org.knime.core.node.exec.ThreadComponentExecutionJobManager;
import org.knime.core.node.exec.ThreadComponentExecutionJobManagerFactory;
import org.knime.core.node.exec.ThreadNodeExecutionJobManagerFactory;
import org.knime.shared.workflow.def.WorkflowDef;
import org.knime.shared.workflow.storage.util.PasswordRedactor;

/**
 * See AP-19181. Tests whether the new default job manager works properly.
 * 
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
public class EnhAP19181_SeparateJobManagerForComponents extends WorkflowTestCase {

	private WorkflowManager m_wfm;

	private NodeContainer m_nativeNode, m_defaultComp, m_oldComp, m_inheritComp;
	private SubNodeContainer m_nonDefaultComp;

	/**
	 * Set up the variables for nodes
	 * 
	 * @throws Exception
	 */
	@BeforeEach
	public void beforeEach() throws Exception {
		NodeID baseID = loadAndSetWorkflow();
		m_wfm = getManager();
		m_nativeNode = m_wfm.getNodeContainer(new NodeID(baseID, 1));
		m_defaultComp = m_wfm.getNodeContainer(new NodeID(baseID, 4));
		m_nonDefaultComp = (SubNodeContainer) m_wfm.getNodeContainer(new NodeID(baseID, 5));
		final NodeID inheritComponentId = m_nonDefaultComp.getID().createChild(0).createChild(9);
		m_inheritComp = m_nonDefaultComp.getWorkflowManager().getNodeContainer(inheritComponentId);
		m_oldComp = m_wfm.getNodeContainer(new NodeID(baseID, 6));
	}

	/**
	 * Test whether the job managers have been loaded correctly
	 */
	@Test
	public void testLoadJobManagers() {
		assertThat(m_nativeNode.getJobManager(), is(IsNull.nullValue()));
		assertThat(m_defaultComp.getJobManager(), is(IsNull.nullValue()));

		final var nonStandardManager = (ThreadComponentExecutionJobManager) m_nonDefaultComp.getJobManager();
		assertThat(nonStandardManager.getID(), is(ThreadComponentExecutionJobManagerFactory.INSTANCE.getID()));
		assertThat(nonStandardManager.isCancelOnFailure(), is(true));

		// (get JobManager) the component is configured with <<default>> as job manager,
		// meaning null is set as job manager
		assertThat((m_inheritComp).getJobManager(), is(IsNull.nullValue()));
	}

	/**
	 * {@link NodeContainer#findJobManager()} resolves null job managers by using
	 * the job manager of the first ancestor node container with a non-null job
	 * manager.
	 */
	@Test
	public void testFindJobManager() {
		assertThat(m_nativeNode.findJobManager().getID(), is(ThreadNodeExecutionJobManagerFactory.INSTANCE.getID()));

		assertThat(m_defaultComp.findJobManager().getID(),
				is(ThreadNodeExecutionJobManagerFactory.INSTANCE.getID()));

		// (findJobManager) the component is configured with <<default>> as job manager, it will inherit
		// from the root workflow
		final var nonStandardManager = (ThreadComponentExecutionJobManager) m_nonDefaultComp.findJobManager();
		assertThat(nonStandardManager.getID(), is(ThreadComponentExecutionJobManagerFactory.INSTANCE.getID()));
		assertThat(nonStandardManager.isCancelOnFailure(), is(true));

		final var inheritedManager = (ThreadComponentExecutionJobManager) m_inheritComp.findJobManager();
		assertThat(inheritedManager.getID(), is(ThreadComponentExecutionJobManagerFactory.INSTANCE.getID()));
		assertThat(inheritedManager.isCancelOnFailure(), is(true));
	}

	/**
	 * Test whether the old job manager is still able to execute components
	 * (sometimes in old workflows, that job manager is saved alongside a
	 * node/component even though it's the default...)
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBackwardsCompatibility() throws Exception {
		assertThat(m_oldComp.getJobManager().getID(), is(ThreadNodeExecutionJobManagerFactory.INSTANCE.getID()));
		executeAllAndWait();
	}

}