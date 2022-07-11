package org.knime.core.node.workflow;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;

public class TestUpdateTemplateByVersion extends WorkflowTestCase {

	private NodeID m_olderVersionComponent;
	private NodeID m_newerVersionComponent;
	private NodeID m_tableDiff;

	@Before
	public void setUp() throws Exception {
		NodeID baseID = loadAndSetWorkflow(new File(getDefaultWorkflowDirectory(), "MainWorkflow"));
		m_olderVersionComponent = new NodeID(baseID, 5);
		m_newerVersionComponent = new NodeID(baseID, 6);
		m_tableDiff = new NodeID(baseID, 4);
	}

	@Test
	public void testUpdateOlderAndNewerVersionOfComponent() throws Exception {
		executeAndWait(m_tableDiff);
		checkState(m_tableDiff, InternalNodeContainerState.EXECUTED); 
		var loadHelper = new WorkflowLoadHelper(true, getManager().getContext());

		assertTrue("Expected meta node update available",
				getManager().checkUpdateMetaNodeLink(m_olderVersionComponent, loadHelper));
		getManager().updateMetaNodeLink(m_olderVersionComponent, new ExecutionMonitor(), loadHelper);
		assertTrue("Expected meta node update available",
				getManager().checkUpdateMetaNodeLink(m_newerVersionComponent, loadHelper));
		getManager().updateMetaNodeLink(m_newerVersionComponent, new ExecutionMonitor(), loadHelper);

		executeAndWait(m_tableDiff);
		checkState(m_tableDiff, InternalNodeContainerState.EXECUTED); 
	}
}
