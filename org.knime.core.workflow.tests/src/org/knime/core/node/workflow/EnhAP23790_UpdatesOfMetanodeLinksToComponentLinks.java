/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.core.node.workflow;

import static org.junit.Assert.assertTrue;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.ExecutionMonitor;

/**
 * Tests the bug described in AP-23790: shared metanodes get converted to components and
 * their connections need to be correctly adjusted. 
 * 
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
public class EnhAP23790_UpdatesOfMetanodeLinksToComponentLinks extends WorkflowTestCase { //NOSONAR

	private NodeID m_variableCreator_4;
	private NodeID m_sharedTemplate_3;
	private NodeID m_metanodeDiffChecker_6;
	private NodeID m_metanodeDiffChecker_10;
	private NodeID m_componentDiffChecker_12;
	private NodeID m_componentDiffChecker_15;
	
	@Before
	public void beforeEach() throws Exception {
		NodeID baseID = loadAndSetWorkflow();
		m_variableCreator_4 = baseID.createChild(4);
		m_sharedTemplate_3 = baseID.createChild(3);
		m_metanodeDiffChecker_6 = baseID.createChild(6);
		m_metanodeDiffChecker_10 = baseID.createChild(10);
		m_componentDiffChecker_12 = baseID.createChild(12);
		m_componentDiffChecker_15 = baseID.createChild(15);
	}

	@Test
	public void testUnchanged() throws Exception {
		checkState(m_variableCreator_4, CONFIGURED);
		executeAllAndWait();
		checkState(m_metanodeDiffChecker_6, EXECUTED);       
		checkState(m_metanodeDiffChecker_10, EXECUTED);
		checkState(m_componentDiffChecker_12, CONFIGURED); // failed
		checkState(m_componentDiffChecker_15, CONFIGURED); // failed
	}
	
	@Test
	public void testAfterUpdate() throws Exception {
		checkState(m_variableCreator_4, CONFIGURED);
		final WorkflowManager manager = getManager();
		final WorkflowLoadHelper lH = new WorkflowLoadHelper(true, manager.getContextV2());
		assertTrue(manager.checkUpdateMetaNodeLink(m_sharedTemplate_3, lH));
		manager.updateMetaNodeLink(m_sharedTemplate_3, new ExecutionMonitor(), lH);
		manager.getNodeContainer(m_sharedTemplate_3, SubNodeContainer.class, true); // check type
		executeAllAndWait();
		checkState(m_metanodeDiffChecker_6, CONFIGURED); // failed      
		checkState(m_metanodeDiffChecker_10, CONFIGURED); // failed
		checkState(m_componentDiffChecker_12, EXECUTED);
		checkState(m_componentDiffChecker_15, EXECUTED);
	}
}
