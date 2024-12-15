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

import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.exec.ThreadComponentExecutionJobManager;
import org.knime.core.node.exec.ThreadComponentExecutionJobManagerFactory;
import org.knime.core.node.exec.ThreadNodeExecutionJobManagerFactory;

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