MISSINGpackage org.knime.core.node.workflow;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;
import static org.knime.core.node.workflow.InternalNodeContainerState.IDLE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 */
public class BugAP21669_ComponentInInactiveBranch extends WorkflowTestCase {

	private NodeID m_branchInverter_2;
	private NodeID m_failInExecution_12;

	// inner component
	private NodeID m_outerComponentID_7;
	private NodeID m_fileReader_7_9; 
	private NodeID m_failInExecution_7_12;
	private NodeID m_branchInverter_7_6;

	// inner most component
	private NodeID m_innerComponentID_7_10_7;
	private NodeID m_fileReader_7_10_7_4; 

	/**
	 * Initialize all node IDs
	 * 
	 * @throws Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
		final NodeID workflowID = loadAndSetWorkflow();
		m_branchInverter_2 = workflowID.createChild(2);
		m_failInExecution_12 = workflowID.createChild(12);
		m_outerComponentID_7 = workflowID.createChild(7);
		final NodeID outerComponentWFMID = m_outerComponentID_7.createChild(0);
		m_branchInverter_7_6 = outerComponentWFMID.createChild(6);
		m_fileReader_7_9 = outerComponentWFMID.createChild(9);
		m_failInExecution_7_12 = outerComponentWFMID.createChild(12);
		m_innerComponentID_7_10_7 = outerComponentWFMID.createChild(10).createChild(0).createChild(7);
		final NodeID innerComponentWFMID = m_innerComponentID_7_10_7.createChild(0);
		m_fileReader_7_10_7_4 = innerComponentWFMID.createChild(4);
	}

	@Test
	public void testAll() throws Exception {
		final NodeID[] downstreamNodeIDs = { m_failInExecution_12, m_outerComponentID_7, m_fileReader_7_9,
				m_failInExecution_7_12, m_branchInverter_7_6, m_innerComponentID_7_10_7, m_fileReader_7_10_7_4 };
		checkState(m_branchInverter_2, CONFIGURED);
		for (NodeID id : downstreamNodeIDs) {
			System.out.println(id);
			checkState(id, IDLE);
		}
		checkStateOfMany(IDLE, downstreamNodeIDs); // all component nodes idle

		executeAndWait(m_branchInverter_2); 
		// output is populated but inactive - configure storm is propagated downstream 
		checkStateOfMany(CONFIGURED, downstreamNodeIDs);

		for (NodeID id : downstreamNodeIDs) {
			assertThat(String.format("Node %s is inactive", id), findNodeContainer(id).isInactive());
		}

		executeAllAndWait();
		checkStateOfMany(EXECUTED, downstreamNodeIDs); // all component nodes executed

		for (NodeID id : downstreamNodeIDs) {
			assertThat(String.format("Node %s is inactive", id), findNodeContainer(id).isInactive());
		}


	}
}