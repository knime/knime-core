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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.knime.core.node.workflow.BugWEBP803_OnlyResetNodesToBeReexecuted.createNodeID;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.wizard.page.WizardPage;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;

/**
 * Test for the (partial) single page re-execution of pages in a workflow which
 * is in wizard execution.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class EnhWEBP674_SinglePageExecution extends WorkflowTestCase {

	private SubNodeContainer m_page1;
	private SubNodeContainer m_page2;

	@BeforeEach
	public void loadWorklfowAndExecuteToFirstPage() throws Exception {
		loadAndSetWorkflow();
		WorkflowManager wfm = getManager();
		m_page1 = (SubNodeContainer) wfm.getNodeContainer(wfm.getID().createChild(7));
		m_page2 = (SubNodeContainer) wfm.getNodeContainer(wfm.getID().createChild(6));

		WizardExecutionController wec = wfm.setAndGetWizardExecutionController();

		IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> wec.reexecuteSinglePage((NodeID)null, Collections.emptyMap()));
		assertThat(exception.getMessage(), is("No current wizard page"));

		// advance to first page
		wec.stepFirst();
		waitForPage(m_page1);
		assertThat(wec.hasCurrentWizardPage(), is(true));
	}

	/**
	 * Tests general partial re-execution of a wizard page.
	 */
	@Test
	public void testReexecuteSinglePage() {
		WizardExecutionController wec = getManager().setAndGetWizardExecutionController();
		reexecuteSinglePageTillExecuted(wec);

		WizardPage pageContent = wec.getCurrentWizardPage();
		assertThat(pageContent.getPageMap().size(), is(4));
		// make sure that sec and wec return the same wizard page
		assertThat(wec.getCurrentWizardPage().getPageMap(), is(pageContent.getPageMap()));
	}

	/**
	 * Check single page re-execution with the current page still containing
	 * executing nodes.
	 */
	@Test
	public void testReexecuteSinglePageAndGetPageWithStillExecutingNodes() {
		WizardExecutionController wec = getManager().setAndGetWizardExecutionController();
		reexecuteSinglePageAndKeepExecuting(wec);
		WizardPage pageContent = wec.getCurrentWizardPage();
		assertThat(pageContent.getPageMap().size(), is(4));
		assertTrue(pageContent.getPageMap().get(createNodeIDSuffix(7, 0, 3)).getNodeContainerState()
				.isWaitingToBeExecuted());
	}

	/**
	 * Try to re-execute single page, load new values and step next while the
	 * single-page re-execution of the current page is still in progress.
	 */
	@Test
	public void testReexecuteSinglePageLoadValuesAndStepNextWhileReexcutionInProgress() {
		WorkflowManager wfm = getManager();
		WizardExecutionController wec = wfm.setAndGetWizardExecutionController();
		NodeID projectId = wfm.getID();
		reexecuteSinglePageAndKeepExecuting(wec);

		IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> wec.reexecuteSinglePage(createNodeID(projectId, 7), createWizardPageInput(0)));
		assertThat(exception.getMessage(), is("Action not allowed. Single page re-execution is in progress."));
		exception = assertThrows(IllegalStateException.class,
				() -> wec.loadValuesIntoCurrentPage(createWizardPageInput(0)));
		assertThat(exception.getMessage(), is("Action not allowed. Single page re-execution is in progress."));
		exception = assertThrows(IllegalStateException.class, () -> wec.stepNext());
		assertThat(exception.getMessage(), is("Action not allowed. Single page re-execution is in progress."));
	}

	/**
	 * Check reset to previous page while in single page re-execution.
	 */
	@Test
	public void testStepBackWhileInSinglePageReexecution() {
		WorkflowManager wfm = getManager();
		WizardExecutionController wec = wfm.setAndGetWizardExecutionController();
		NodeID projectId = wfm.getID();

		// step to second page
		wec.loadValuesIntoCurrentPage(createWizardPageInput(0));
		wec.stepNext();
		waitForPage(m_page2);

		// re-execute second page
		wec.reexecuteSinglePage(createNodeID(projectId, 6, 0, 6), Collections.emptyMap());
		Awaitility.await().atMost(5, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).until(() -> {
			WizardPage pc = wec.getCurrentWizardPage();
			return pc.getPageMap().keySet().contains(createNodeIDSuffix(6, 0, 5));
		});

		// step back and check page content
		wfm.cancelExecution();
		waitForSinglePageNotExecutingAnymore(wec);
		wec.stepBack();
		WizardPage pageContent = wec.getCurrentWizardPage();
		assertTrue(pageContent.getPageMap().containsKey(createNodeIDSuffix(7, 0, 3)));
		assertThat(pageContent.getPageMap().size(), is(4));
	}

	private void reexecuteSinglePageAndKeepExecuting(WizardExecutionController wec) {
		wec.reexecuteSinglePage(createNodeID(getManager().getID(), 7, 0, 7), createWizardPageInput(400000));
		assertThat(wec.hasCurrentWizardPage(), is(true));
		Awaitility.await().atMost(5, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
			WizardPage pc = wec.getCurrentWizardPage();
			assertThat(pc.getPageMap().size(), is(4));
		});
	}

	private void reexecuteSinglePageTillExecuted(WizardExecutionController wec) {
		wec.reexecuteSinglePage(createNodeID(getManager().getID(), 7, 0, 7),
				createWizardPageInput(0));
		waitForPage(m_page1);
	}

	/**
	 * @param intInput the intInput controls the num seconds of a 'Wait ...' node
	 */
	private static Map<String, String> createWizardPageInput(int intInput) {
		Map<String, String> res = new HashMap<>();
		res.put("7:0:7", "{\"@class\":\"org.knime.js.base.node.base.input.integer.IntegerNodeValue\",\"integer\":"
				+ intInput + "}");
		res.put("7:0:3",
				"{\"@class\":\"org.knime.js.base.node.viz.pagedTable.PagedTableViewValue\",\"publishSelection\":true,\"subscribeSelection\":true,\"publishFilter\":true,\"subscribeFilter\":true,\"pageSize\":0,\"currentPage\":0,\"hideUnselected\":false,\"selection\":null,\"selectAll\":false,\"selectAllIndeterminate\":false,\"filterString\":null,\"columnFilterStrings\":null,\"currentOrder\":[]}");
		res.put("7:0:2",
				"{\"@class\":\"org.knime.js.base.node.base.input.string.StringNodeValue\",\"string\":\"test\"}}");
		return res;
	}

	private static void waitForPage(SubNodeContainer page) {
		Awaitility.await().atMost(5, TimeUnit.SECONDS).pollInterval(10, TimeUnit.MILLISECONDS)
				.until(() -> page.getNodeContainerState().isExecuted());
	}

	private static void waitForSinglePageNotExecutingAnymore(WizardExecutionController wec) {
		Awaitility.await().atMost(5, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).until(
				() -> !wec.getSinglePageExecutionState().map(NodeContainerState::isExecutionInProgress).orElse(false));
	}

	private static NodeIDSuffix createNodeIDSuffix(int... ids) {
		return new NodeIDSuffix(ids);
	}

	@Override
	@AfterEach
	public void tearDown() throws Exception {
		getManager().cancelExecution();
		super.tearDown();
	}

}