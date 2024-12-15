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

import java.io.File;
import java.io.IOException;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.workflow.WorkflowPersistor.MetaNodeLinkUpdateResult;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.FileUtil;

/**
 * Makes sure that component projects which have 'example' flow variables stored
 * can be loaded and saved properly.
 * 
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class BugAP15980_ProblemSavingComponentProjectWithFlowVariables extends WorkflowTestCase {

	@TempDir
	private File m_componentDir;
	private SubNodeContainer m_componentProject;
	private NodeID m_node_1230;

	/**
	 * Copies the component into a temporary location.
	 * 
	 * @throws Exception
	 * @throws IOException
	 */
	@BeforeEach
	public void setup() throws IOException, Exception {
		FileUtil.copyDir(getDefaultWorkflowDirectory(), m_componentDir);
		initComponentFromTemp();
	}

	private void initComponentFromTemp() throws Exception {
		WorkflowLoadHelper loadHelper = new WorkflowLoadHelper(true, true,
				WorkflowContextV2.forTemporaryWorkflow(m_componentDir.toPath(), null));
		MetaNodeLinkUpdateResult loadResult = loadComponent(m_componentDir, new ExecutionMonitor(), loadHelper);
		m_componentProject = (SubNodeContainer) loadResult.getLoadedInstance();
		NodeID baseID = m_componentProject.getWorkflowManager().getID();
		m_node_1230 = new NodeID(baseID, 1230);
	}

	/**
	 * Executes the entire component project (which makes sure that the 'example'
	 * flow variables are loaded as expected) and saves it (proofing that the actual
	 * problem has been fixed - because saving didn't work).
	 */
	@Test
	public void testExecuteAndSaveComponent() throws Exception {
		WorkflowManager wfm = m_componentProject.getWorkflowManager();
		wfm.executeAllAndWaitUntilDone();
		assertThat("Not all nodes are executed", wfm.getNodeContainer(m_node_1230).getNodeContainerState().isExecuted(),
				is(true));

		m_componentProject.saveAsTemplate(m_componentDir, new ExecutionMonitor(), null);
	}

}