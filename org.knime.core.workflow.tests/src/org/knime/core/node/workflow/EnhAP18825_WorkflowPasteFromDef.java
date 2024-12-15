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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.shared.workflow.storage.util.PasswordRedactor;

/**
 * 
 * @author Dionysios Stolis, KNIME GmbH, Berlin, Germany
 *
 */
public class EnhAP18825_WorkflowPasteFromDef extends WorkflowTestCase {

	private WorkflowManager m_wfm;

	private NodeID m_tableCreator, m_metanode, m_concatenate, m_component, m_lockedMetanode;

	private WorkflowAnnotationID m_sourceAnnotationID;

	@BeforeEach
	public void beforeEach() throws Exception {
		NodeID baseID = loadAndSetWorkflow();
		m_wfm = getManager();
		m_tableCreator = new NodeID(baseID, 10);
		m_metanode = new NodeID(baseID, 12);
		m_concatenate = new NodeID(baseID, 11);
		m_component = new NodeID(baseID, 14);
		m_lockedMetanode = new NodeID(baseID, 8);

		// only one annotation in the workflow
		m_sourceAnnotationID = m_wfm.getWorkflowAnnotationIDs().iterator().next();
	}

	@Test
	public void pasteNativeNode() throws Exception {
		final WorkflowCopyContent spec = WorkflowCopyContent.builder()//
				.setNodeIDs(m_tableCreator)//
				.setIncludeInOutConnections(false)//
				.build();

		var workflowDef = m_wfm.copyToDef(spec, PasswordRedactor.asNull());

		var pasted = m_wfm.paste(workflowDef);

		assertThat(spec.getAnnotationIDs().length, is(pasted.getAnnotationIDs().length));
		assertThat(spec.getNodeIDs().length, is(pasted.getNodeIDs().length));
		assertThat(spec.getNodeIDs()[0].getPrefix(), is(pasted.getNodeIDs()[0].getPrefix()));
	}

	@Test
	public void pasteTwoNativeNodesWithPartialConnections() throws Exception {
		final WorkflowCopyContent spec = WorkflowCopyContent.builder()//
				.setNodeIDs(m_tableCreator, m_concatenate)//
				.setIncludeInOutConnections(true)//
				.build();

		final var workflowDef = m_wfm.copyToDef(spec, PasswordRedactor.asNull());

		var pasted = m_wfm.paste(workflowDef);

		assertThat(spec.getAnnotationIDs().length, is(pasted.getAnnotationIDs().length));
		assertThat(pasted.getNodeIDs().length, is(2));
		assertThat(spec.getNodeIDs()[0].getPrefix(), is(pasted.getNodeIDs()[0].getPrefix()));
		assertThat(spec.getNodeIDs()[1].getPrefix(), is(pasted.getNodeIDs()[1].getPrefix()));
	}

	@Test
	public void pasteMetaNode() throws Exception {
		final WorkflowCopyContent spec = WorkflowCopyContent.builder()//
				.setNodeIDs(new NodeID[] { m_metanode })//
				.setIncludeInOutConnections(false)//
				.build();

		var workflowDef = m_wfm.copyToDef(spec, PasswordRedactor.asNull());

		var pasted = m_wfm.paste(workflowDef);

		assertThat(spec.getAnnotationIDs().length, is(pasted.getAnnotationIDs().length));
		assertThat(pasted.getNodeIDs().length, is(1));
		assertThat(spec.getNodeIDs()[0].getPrefix(), is(pasted.getNodeIDs()[0].getPrefix()));
	}

	@Test
	public void pasteComponentNode() throws Exception {
		final WorkflowCopyContent spec = WorkflowCopyContent.builder()//
				.setNodeIDs(new NodeID[] { m_component })//
				.setIncludeInOutConnections(false)//
				.build();

		var workflowDef = m_wfm.copyToDef(spec, PasswordRedactor.asNull());

		var pasted = m_wfm.paste(workflowDef);

		assertThat(spec.getAnnotationIDs().length, is(pasted.getAnnotationIDs().length));
		assertThat(pasted.getNodeIDs().length, is(1));
		assertThat(spec.getNodeIDs()[0].getPrefix(), is(pasted.getNodeIDs()[0].getPrefix()));
	}

	@Test
	public void pasteLockedMetanNode() throws Exception {
		final WorkflowCopyContent spec = WorkflowCopyContent.builder()//
				.setNodeIDs(new NodeID[] { m_lockedMetanode })//
				.setIncludeInOutConnections(false)//
				.build();

		var workflowDef = m_wfm.copyToDef(spec, PasswordRedactor.asNull());

		var pasted = m_wfm.paste(workflowDef);

		assertThat(spec.getAnnotationIDs().length, is(pasted.getAnnotationIDs().length));
		assertThat(pasted.getNodeIDs().length, is(1));
		assertThat(spec.getNodeIDs()[0].getPrefix(), is(pasted.getNodeIDs()[0].getPrefix()));			 
	}

	@Test
	public void pasteMultipleNodesWithPartialConnections() throws Exception {
		var nodeIdArray = new NodeID[] { m_tableCreator, m_metanode, m_concatenate, m_component, m_lockedMetanode };
		final WorkflowCopyContent spec = WorkflowCopyContent.builder()//
				.setNodeIDs(nodeIdArray)//
				.setIncludeInOutConnections(false)//
				.build();

		var workflowDef = m_wfm.copyToDef(spec, PasswordRedactor.asNull());

		var pasted = m_wfm.paste(workflowDef);

		assertThat(spec.getAnnotationIDs().length, is(pasted.getAnnotationIDs().length));
		assertThat(pasted.getNodeIDs().length, is(nodeIdArray.length));
		assertThat(spec.getNodeIDs()[0].getPrefix(), is(pasted.getNodeIDs()[0].getPrefix()));
	}

}