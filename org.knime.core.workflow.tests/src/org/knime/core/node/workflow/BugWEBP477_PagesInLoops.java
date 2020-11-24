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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;

/**
 * Test for bug WEBP-477 where
 * {@link WizardExecutionController#hasCurrentWizardPage()} returned
 * <code>true</code> even though the contained nodes weren't executed, yet,
 * which led to missing content on the respective webportal page.
 * 
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class BugWEBP477_PagesInLoops extends WorkflowTestCase {

	private WorkflowManager m_wfm;

	@Test
	public void testNodesInComponentsAreExecutedIfWizardPageIsAvailable() throws Exception {
		NodeID wfId = loadAndSetWorkflow();
		m_wfm = getManager();
		NodeContainer nc1 = m_wfm.findNodeContainer(NodeIDSuffix.fromString("9:0:7:0:3").prependParent(wfId));
		NodeContainer nc2 = m_wfm.findNodeContainer(NodeIDSuffix.fromString("9:0:7:0:6").prependParent(wfId));
		NodeContainer nc3 = m_wfm.findNodeContainer(NodeIDSuffix.fromString("9:0:8:0:3").prependParent(wfId));
		NodeContainer nc4 = m_wfm.findNodeContainer(NodeIDSuffix.fromString("9:0:8:0:9").prependParent(wfId));
		WizardExecutionController wec = m_wfm.setAndGetWizardExecutionController();
		assertFalse(wec.hasCurrentWizardPage());
		wec.stepFirst();
		await().atMost(2, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
			assertTrue(wec.hasCurrentWizardPage());
		});
		wec.loadValuesIntoCurrentPage(wizardPageInput());
		wec.stepNext();
		await().atMost(2, TimeUnit.SECONDS).pollInterval(10, TimeUnit.MILLISECONDS).untilAsserted(() -> {
			assertTrue(wec.hasCurrentWizardPage());
		});
		assertTrue("Node execution not finished, yet", nc1.getNodeContainerState().isExecuted());
		assertTrue("Node execution not finished, yet", nc2.getNodeContainerState().isExecuted());
		assertTrue("Node execution not finished, yet", nc3.getNodeContainerState().isExecuted());
		assertTrue("Node execution not finished, yet", nc4.getNodeContainerState().isExecuted());
	}

	@After
	public void cancelExecution() {
		m_wfm.cancelExecution();
	}

	private static Map<String, String> wizardPageInput() {
		Map<String, String> viewValues = new HashMap<>();
		viewValues.put("9:0:7:0:3",
				"{\"@class\":\"org.knime.js.base.node.viz.generic3.GenericJSViewValue\",\"settings\":null,\"flowVariables\":{}}");
		viewValues.put("9:0:7:0:6",
				"{\"@class\":\"org.knime.js.base.node.viz.pagedTable.PagedTableViewValue\",\"currentPage\":0,\"pageSize\":0,\"hideUnselected\":false,\"publishSelection\":true,\"subscribeSelection\":true,\"publishFilter\":true,\"subscribeFilter\":true,\"selection\":null,\"selectAll\":false,\"selectAllIndeterminate\":false,\"filterString\":null,\"columnFilterStrings\":null,\"currentOrder\":[]}");
		viewValues.put("9:0:8:0:9",
				"{\"@class\":\"org.knime.js.base.node.base.input.bool.BooleanNodeValue\",\"boolean\":false}");
		viewValues.put("9:0:8:0:3",
				"{\"@class\":\"org.knime.js.base.node.viz.generic3.GenericJSViewValue\",\"settings\":null,\"flowVariables\":{}}");
		return viewValues;
	}

}
