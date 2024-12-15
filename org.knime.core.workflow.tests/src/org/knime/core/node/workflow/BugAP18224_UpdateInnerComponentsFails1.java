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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.core.node.workflow;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.ExecutionMonitor;

/**
 * Tests the new update components functionality of update errors being masked
 * when a parent update exists. In the workflow "mainWorkflow" one component
 * instance of B contains a non-existent inner component A, but it will be found
 * once B is updated properly.
 * 
 * As an indicator, the updated inner component a returns a single row String
 * value with "updated", the non-updated version would return an empty table.
 * Following that, in
 * {@link BugAP18224_UpdateInnerComponentsFails1#testUpdatingFixesInnerComponentsError()},
 * the first checkState checks for non-equal inputs, the second one for equal
 * inputs.
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 */
public class BugAP18224_UpdateInnerComponentsFails1 extends WorkflowTestCase {

	private NodeID m_olderVersionComponent, m_newerVersionComponent, m_tableDiff;

	/**
	 * Initialize all node IDs
	 * 
	 * @throws Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
		final var workspaceDir = getDefaultWorkflowDirectory();
		var baseId = loadAndSetWorkflowInWorkspace(new File(workspaceDir, "mainWorkflow"), workspaceDir);
		m_olderVersionComponent = new NodeID(baseId, 1);
		m_newerVersionComponent = new NodeID(baseId, 4);
		m_tableDiff = new NodeID(baseId, 3);
	}

	@Test
	public void testUpdatingFixesInnerComponentsError() throws Exception {
		executeAndWait(m_tableDiff);
		checkState(m_tableDiff, InternalNodeContainerState.CONFIGURED); // fails
		var loadHelper = new WorkflowLoadHelper(true, getManager().getContextV2());

		assertTrue(getManager().checkUpdateMetaNodeLink(m_olderVersionComponent, loadHelper),
				"Expected meta node update available");
		getManager().updateMetaNodeLink(m_olderVersionComponent, new ExecutionMonitor(), loadHelper);
		assertFalse(getManager().checkUpdateMetaNodeLink(m_newerVersionComponent, loadHelper),
				"Expected no meta node update to be available");

		executeAndWait(m_tableDiff);
		checkState(m_tableDiff, InternalNodeContainerState.EXECUTED); // runs successful
	}
}