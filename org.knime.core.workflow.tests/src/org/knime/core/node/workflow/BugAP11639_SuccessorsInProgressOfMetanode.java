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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests correct estimation of successors in progress of nodes contained in a
 * metanode and component; especially where a one branch in the metanode has
 * executing successors and the other not.
 * 
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class BugAP11639_SuccessorsInProgressOfMetanode extends WorkflowTestCase {

	private WorkflowManager m_wfm;

	private WorkflowManager m_metanode_8;
	private WorkflowManager m_component_10;

	private NodeID m_datagen_8_1;
	private NodeID m_datagen_8_5;
	private NodeID m_datagen_10_0_1;
	private NodeID m_datagen_10_0_5;
	private NodeID m_colfilter_8_6;
	private NodeID m_colfilter_15_7_6;
	private NodeID m_colfilter_15_8_6;
	private NodeID m_colfilter_20_7_6;
	private NodeID m_colfilter_20_8_6;

	@BeforeEach
	public void setupAndExecute() throws Exception {
		NodeID wfId = loadAndSetWorkflow();
		m_wfm = getManager();
		m_metanode_8 = (WorkflowManager) m_wfm.getNodeContainer(new NodeID(wfId, 8));
		m_component_10 = ((SubNodeContainer) m_wfm.getNodeContainer(new NodeID(wfId, 10))).getWorkflowManager();
		m_datagen_8_1 = createNodeID(wfId, 8, 1);
		m_datagen_8_5 = createNodeID(wfId, 8, 5);
		m_colfilter_8_6 = createNodeID(wfId, 8, 6);
		m_datagen_10_0_1 = createNodeID(wfId, 10, 0, 1);
		m_datagen_10_0_5 = createNodeID(wfId, 10, 0, 5);
		m_colfilter_15_7_6 = createNodeID(wfId, 15, 7, 6);
		m_colfilter_15_8_6 = createNodeID(wfId, 15, 8, 6);
		m_colfilter_20_7_6 = createNodeID(wfId, 20, 7, 6);
		m_colfilter_20_8_6 = createNodeID(wfId, 20, 8, 6);
		m_wfm.executeAll();
		Awaitility.await().atMost(20, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
			assertTrue(m_metanode_8.getNodeContainerState().isExecuted());
			assertTrue(m_component_10.getNodeContainerState().isExecuted());
		});
	}

	@Test
	public void testSuccessorsInProgressOfMetanode() throws Exception {
		try (WorkflowLock lock = m_wfm.lock()) {
			assertTrue(m_metanode_8.hasSuccessorInProgress(m_datagen_8_1), "successors in progress expected");
			assertFalse(m_metanode_8.hasSuccessorInProgress(m_datagen_8_5), "no successors in progress expected");

			assertFalse(m_component_10.hasSuccessorInProgress(m_datagen_10_0_1), "successors in progress expected");
			assertFalse(m_component_10.hasSuccessorInProgress(m_datagen_10_0_5), "successors in progress expected");
		}
	}

	@Test
	public void testCanResetNodesInMetanodeWithSuccessorsInProgress() {
		assertFalse(m_metanode_8.canResetNode(m_datagen_8_1));
		assertTrue(m_metanode_8.canResetNode(m_datagen_8_5));

		assertFalse(m_component_10.canResetNode(m_datagen_10_0_1));
		assertFalse(m_component_10.canResetNode(m_datagen_10_0_5));

		// bug AP-14915
		assertFalse(m_wfm.findNodeContainer(m_colfilter_15_8_6).getParent().canResetNode(m_colfilter_15_8_6));
		assertTrue(m_wfm.findNodeContainer(m_colfilter_15_7_6).getParent().canResetNode(m_colfilter_15_7_6));
		assertFalse(m_wfm.findNodeContainer(m_colfilter_20_8_6).getParent().canResetNode(m_colfilter_20_8_6));
		assertTrue(m_wfm.findNodeContainer(m_colfilter_20_7_6).getParent().canResetNode(m_colfilter_20_7_6));

	}

	@Test
	public void testCanRemoveOrAddConnectionInMetanodeWithSuccessorsInProgress() {
		// check can remove
		assertFalse(m_metanode_8
				.canRemoveConnection(m_metanode_8.getOutgoingConnectionsFor(m_datagen_8_1, 1).iterator().next()));
		assertTrue(m_metanode_8
				.canRemoveConnection(m_metanode_8.getOutgoingConnectionsFor(m_datagen_8_5, 1).iterator().next()));
		assertFalse(m_component_10
				.canRemoveConnection(m_component_10.getOutgoingConnectionsFor(m_datagen_10_0_1, 1).iterator().next()));
		assertFalse(m_component_10
				.canRemoveConnection(m_component_10.getOutgoingConnectionsFor(m_datagen_10_0_5, 1).iterator().next()));

		// remove a connection where possible
		m_metanode_8.removeConnection(m_metanode_8.getOutgoingConnectionsFor(m_datagen_8_5, 1).iterator().next());

		// check can add connection
		assertTrue(m_metanode_8.canAddConnection(m_datagen_8_5, 1, m_colfilter_8_6, 1));
	}

	@AfterEach
	public void cancelWorkflow() {
		m_wfm.cancelExecution();
	}

	private NodeID createNodeID(NodeID root, int... ids) {
		NodeID res = root;
		for (int i : ids) {
			res = new NodeID(res, i);
		}
		return res;
	}

}