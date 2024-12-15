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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.shared.workflow.def.ConnectionDef;
import org.knime.shared.workflow.def.NativeNodeDef;
import org.knime.shared.workflow.def.WorkflowDef;
import org.knime.shared.workflow.def.impl.ConnectionDefBuilder;
import org.knime.shared.workflow.def.impl.ConnectionUISettingsDefBuilder;
import org.knime.shared.workflow.def.impl.DefaultConnectionDef;
import org.knime.shared.workflow.storage.util.PasswordRedactor;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Test workflow structure: table creators #1 and #2, joiner #3 connects on
 * outports 2 and 3, table writers #4 and #5.
 * 
 * <pre>
       Table Creator #1 -\        /- #4 Table Writer
                         #3 Joiner
       Table Creator #2 -/        \- #5 Table Writer
 * </pre>
 * 
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class EnhAP18826_WorkflowCopyContentToDef extends WorkflowTestCase { //NOSONAR

	private WorkflowManager m_wfm;

	private NodeID m_joinerID, m_writerID;

	private WorkflowAnnotationID m_sourceAnnotationID;

	@BeforeEach
	public void beforeEach() throws Exception {
		NodeID baseID = loadAndSetWorkflow();
		m_wfm = getManager();
		m_joinerID = new NodeID(baseID, 3);
		m_writerID = new NodeID(baseID, 5);

		// only one annotation in the workflow
		m_sourceAnnotationID = m_wfm.getWorkflowAnnotationIDs().iterator().next();
	}

	/**
	 * Test copying the joiner node (#3) without dangling connections.
	 */
	@Test
	public void copySingleNode() throws Exception {
		final WorkflowCopyContent spec = WorkflowCopyContent.builder()//
			.setNodeIDs(m_joinerID)//
			.setIncludeInOutConnections(false)//
			.build();

		WorkflowDef workflow = m_wfm.copyToDef(spec, PasswordRedactor.asNull()).getPayload();

		// copied one node
		assertThat(workflow.getNodes().size(), is(1));

		// joiner with id = 3
		NativeNodeDef joinerNode = (NativeNodeDef) workflow.getNodes().get("3");
		assertThat(joinerNode.getNodeName(), is("Joiner"));
		assertThat(joinerNode.getId(), is(3));

		// and no connections
		assertThat(workflow.getConnections().size(), is(0));
	}	


	/**
	 * Test copying the joiner node (#3) with connections to non-included nodes.
	 */
	@Test
	public void copySingleNodeWithPartialConections() throws Exception {
		final WorkflowCopyContent spec = WorkflowCopyContent.builder()//
			.setNodeIDs(m_joinerID)//
			.setIncludeInOutConnections(true)//
			.build();

		var workflow = m_wfm.copyToDef(spec, PasswordRedactor.asNull()).getPayload();

		// and four connections
		assertThat(workflow.getConnections().size(), is(4));

		// somehow doesn't match
		final DefaultConnectionDef conn23 = new ConnectionDefBuilder().setDeletable(true).setSourceID(2).setSourcePort(1).setDestID(3).setDestPort(2).setUiSettings(new ConnectionUISettingsDefBuilder().build()).build();
		assertThat(workflow.getConnections(), hasItem(conn23));
	}

	/**
	 * Test copying the joiner node and one downstream table writer (ID 5).
	 */
	@Test
	public void copyTwoNodes() throws Exception {
		final WorkflowCopyContent spec = WorkflowCopyContent.builder()//
			.setNodeIDs(m_joinerID, m_writerID)//
			.setIncludeInOutConnections(false)//
			.build();

		final var workflow = m_wfm.copyToDef(spec, PasswordRedactor.asNull()).getPayload();

		// both nodes
		assertThat(workflow.getNodes().size(), is(2)); 

		// only one connection
		final DefaultConnectionDef conn35 = new ConnectionDefBuilder().setDeletable(true).setSourceID(3).setSourcePort(3).setDestID(5).setDestPort(1).setUiSettings(new ConnectionUISettingsDefBuilder().build()).build();
		assertThat(workflow.getConnections(), contains(conn35));
	}

	/**
	 * Test copying the joiner node (#3) and one downstream table writer (ID 5) with connections to non-included nodes.
	 * This yields the same set of connections (all) as when only copying the joiner with dangling connections.
	 */
	@Test
	public void copyTwoNodesWithPartialConections() throws Exception {
		final WorkflowCopyContent joinerWithDanglingSpec = WorkflowCopyContent.builder()//
				.setNodeIDs(m_joinerID)//
				.setIncludeInOutConnections(true)//
				.build();

		final WorkflowCopyContent twoWithDanglingSpec = WorkflowCopyContent.builder()//
				.setNodeIDs(m_joinerID, m_writerID)//
				.setIncludeInOutConnections(true)//
				.build();

		var joinerWithDangling = m_wfm.copyToDef(joinerWithDanglingSpec, PasswordRedactor.asNull()).getPayload();
		var twoWithDangling = m_wfm.copyToDef(twoWithDanglingSpec, PasswordRedactor.asNull()).getPayload();

		final List<ConnectionDef> connections1 = joinerWithDangling.getConnections();
		final List<ConnectionDef> connections2 = twoWithDangling.getConnections();
		assertTrue(connections1.containsAll(connections2) && connections2.containsAll(connections1));
	}

	@Test
	public void copyAnnotation() throws JsonProcessingException {
		final WorkflowCopyContent spec = WorkflowCopyContent.builder()//
			.setAnnotationIDs(m_sourceAnnotationID)
			.build();

		WorkflowDef workflow = m_wfm.copyToDef(spec, PasswordRedactor.asNull()).getPayload();

		assertEquals("Source", workflow.getAnnotations().values().stream().findAny().get().getText());
	}

}