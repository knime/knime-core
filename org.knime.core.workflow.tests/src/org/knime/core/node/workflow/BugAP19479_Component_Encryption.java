package org.knime.core.node.workflow;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.isNull;

import org.junit.Before;
import org.junit.Test;
import org.knime.shared.workflow.def.ComponentNodeDef;
import org.knime.shared.workflow.def.NativeNodeDef;
import org.knime.shared.workflow.def.WorkflowDef;
import org.knime.shared.workflow.storage.clipboard.DefClipboard;
import org.knime.shared.workflow.storage.clipboard.DefClipboardContent;
import org.knime.shared.workflow.storage.text.util.ObjectMapperUtil;
import org.knime.shared.workflow.storage.util.PasswordRedactor;

/**
 * The test workflow contains one locked component and one locked metanode. Both
 * contain one native node (Credentials Configuration) that contains a secret
 * string in the node settings.
 */
public class BugAP19479_Component_Encryption extends WorkflowTestCase {

	private WorkflowManager m_wfm;

	private NodeID m_lockedComponent, m_lockedMetanode;

	private WorkflowAnnotationID m_sourceAnnotationID;

	@Before
	public void beforeEach() throws Exception {
		NodeID baseID = loadAndSetWorkflow();
		m_wfm = getManager();
		m_lockedComponent = new NodeID(baseID, 4);
		m_lockedMetanode = new NodeID(baseID, 3);
	}

	/**
	 * Copy a locked component into the {@link DefClipboard}. Serialize the
	 * {@link DefClipboard} content and check that it doesn't contain the content in
	 * plain text.
	 */
	@Test
	public void copyLockedComponent() throws Exception {
		final WorkflowCopyContent spec = WorkflowCopyContent.builder()//
				.setNodeIDs(m_lockedComponent)//
				.setIncludeInOutConnections(false)//
				.build();

		// copy
		WorkflowDef workflow = m_wfm.copyToDef(spec, PasswordRedactor.asNull()).getPayload();
		var defClipboardContent = m_wfm.copyToDef(spec, PasswordRedactor.asNull());

		// defclipboard content to string
		var mapper = ObjectMapperUtil.getInstance().getObjectMapper();
		var serializedContent = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(defClipboardContent);

		System.out.println(serializedContent);
		
		// should not contain, e.g., node settings in plain text
		assertThat("Locked component content exposed.", serializedContent.contains("superSecretName"), is(false));

		var deserialized = DefClipboardContent.valueOf(serializedContent);
		assertNotNull("Cannot deserialize clipboard content.", deserialized.orElse(null));

		// make sure that the deserialized content equals the content that was serialized
		assertEquals("Deserialized locked component definition differs from original locked component definition.",
				deserialized.get().getPayload().getNodes().get("4"), defClipboardContent.getPayload().getNodes().get("4"));

	}

	/**
	 * TODO
	 */
	@Test
	public void copyLockedMetanode() throws Exception {
		final WorkflowCopyContent spec = WorkflowCopyContent.builder()//
				.setNodeIDs(m_lockedMetanode)//
				.setIncludeInOutConnections(false)//
				.build();

		WorkflowDef workflow = m_wfm.copyToDef(spec, PasswordRedactor.asNull()).getPayload();
		var defClipboardContent = m_wfm.copyToDef(spec, PasswordRedactor.asNull());
		var mapper = ObjectMapperUtil.getInstance().getObjectMapper();
		var serializedContent = mapper.writeValueAsString(defClipboardContent);
		assertThat(serializedContent.contains("superSecretName"), is(false));
		// copied one node
		assertThat(workflow.getNodes().size(), is(1));

		var componentNodeDef = (ComponentNodeDef) workflow.getNodes().get("3");
		assertThat(componentNodeDef.getCipher(), isNotNull());
	}

}
