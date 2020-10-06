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

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the correctness of the alternative way to determine node properties
 * that depend on other nodes in the workflow graph.
 * 
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class EnhNXT264_AlternativeDeterminationOfDependentNodeProperties extends WorkflowTestCase {

	WorkflowManager m_wfm = getManager();

	@Before
	public void loadWorklfow() throws Exception {
		loadAndSetWorkflow();
		m_wfm = getManager();
	}

	/**
	 * Mainly tests the correctness of the dependent node properties as determined
	 * in the alternative way. It is done by comparing the results of the
	 * {@link WorkflowManager#canExecuteNode(NodeID)} and
	 * {@link WorkflowManager#canResetNode(NodeID)} obtained in the 'classic' way to
	 * the results as obtained by the alternative way (where the dependent node
	 * properties are determined in one rush and cached).
	 *
	 * @throws Exception
	 */
	@Test
	public void testCorrectnessOfDependentNodeProperties() throws Exception {
		NodeID parentId = m_wfm.getID();
		WorkflowManager metanode_209 = (WorkflowManager) m_wfm.getNodeContainer(parentId.createChild(209));
		WorkflowManager component_214 = ((SubNodeContainer) m_wfm.getNodeContainer(parentId.createChild(214)))
				.getWorkflowManager();
		WorkflowManager component_215 = ((SubNodeContainer) m_wfm.getNodeContainer(parentId.createChild(215)))
				.getWorkflowManager();
		WorkflowManager component_212 = ((SubNodeContainer) m_wfm.getNodeContainer(parentId.createChild(212)))
				.getWorkflowManager();
		NodeID wait_216 = parentId.createChild(216);
		NodeID wait_203 = parentId.createChild(203);
		NodeID wait_195 = parentId.createChild(195);

		checkCanExecuteAndCanResetFlagsForAllNodes(m_wfm);
		checkCanExecuteAndCanResetFlagsForAllNodes(metanode_209);
		// test disabled, see comment above
		// checkCanExecuteAndCanResetFlagsForAllNodes(metanode_219);
		checkCanExecuteAndCanResetFlagsForAllNodes(component_214);
		checkCanExecuteAndCanResetFlagsForAllNodes(component_215);
		checkCanExecuteAndCanResetFlagsForAllNodes(component_212);

		// execute 'Wait ...' nodes
		m_wfm.executeUpToHere(wait_203, wait_195);
		await().atMost(5, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
			assertTrue(m_wfm.getNodeContainerState().isExecutionInProgress());
		});
		checkCanExecuteAndCanResetFlagsForAllNodes(m_wfm);
		checkCanExecuteAndCanResetFlagsForAllNodes(metanode_209);
		checkCanExecuteAndCanResetFlagsForAllNodes(component_214);

		// cancel 'Wait...' node again
		m_wfm.cancelExecution(m_wfm.getNodeContainer(wait_216));
		await().atMost(5, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
			assertTrue(m_wfm.getNodeContainer(wait_216).getNodeContainerState().isConfigured());
		});
		checkCanExecuteAndCanResetFlagsForAllNodes(m_wfm);
		checkCanExecuteAndCanResetFlagsForAllNodes(metanode_209);
		checkCanExecuteAndCanResetFlagsForAllNodes(component_214);

		// add a connection within the component to make sure that the
		// 'hasExecutablePredeccessor' property
		// of the contained nodes changes
		component_212.addConnection(component_212.getID().createChild(212), 1, component_212.getID().createChild(214),
				1);
		checkCanExecuteAndCanResetFlagsForAllNodes(component_212);

		// Tests that only a certain branches within a metanode are regarded as
		// 'executable'
		// Note: here is a discrepancy with the classic implementation where all
		// predecessor of a metanode are taken into account at once which is not 100% correct
		NodeID metanode_219 = parentId.createChild(219);
		DependentNodeProperties props = ((WorkflowManager) m_wfm.getNodeContainer(metanode_219))
				.determineDependentNodeProperties();
		assertFalse(props.canExecuteNode(metanode_219.createChild(196)));
		assertTrue(props.canExecuteNode(metanode_219.createChild(218)));
		
		// test exception
		assertThrows(NoSuchElementException.class, () -> props.canExecuteNode(parentId));
		assertThrows(NoSuchElementException.class, () -> props.canResetNode(parentId));

	}

	@After
	public void cancelWorkflow() {
		m_wfm.cancelExecution();
	}

	private void checkCanExecuteAndCanResetFlagsForAllNodes(WorkflowManager wfm) {
		List<NodeID> nodes = wfm.getNodeContainers().stream().map(nc -> nc.getID()).collect(Collectors.toList());

		List<Boolean> canExecute = nodes.stream().map(wfm::canExecuteNode).collect(Collectors.toList());
		List<Boolean> canReset = nodes.stream().map(wfm::canResetNode).collect(Collectors.toList());

		DependentNodeProperties props = wfm.determineDependentNodeProperties();

		for (int i = 0; i < nodes.size(); i++) {
			NodeID id = nodes.get(i);
			assertThat("'canExecute' flag differs for node " + id, props.canExecuteNode(id), is(canExecute.get(i)));
			assertThat("'canReset' flag differs for node " + id, props.canResetNode(id), is(canReset.get(i)));
		}
	}
}
