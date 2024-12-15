MISSINGpackage org.knime.core.node.workflow;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.shared.workflow.def.WorkflowDef;
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