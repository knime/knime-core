package org.knime.core.node.workflow;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.isNull;


import org.junit.Before;
import org.junit.Test;
import org.knime.shared.workflow.def.ComponentNodeDef;
import org.knime.shared.workflow.def.NativeNodeDef;
import org.knime.shared.workflow.def.WorkflowDef;
import org.knime.shared.workflow.storage.text.util.ObjectMapperUtil;
import org.knime.shared.workflow.storage.util.PasswordRedactor;
;

public class BugAP19479_Component_Encryption extends WorkflowTestCase{
	
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
	 * Test copying the joiner node (#3) without dangling connections.
	 */
	@Test
	public void copyLockedComponent() throws Exception {
		final WorkflowCopyContent spec = WorkflowCopyContent.builder()//
			.setNodeIDs(m_lockedComponent)//
			.setIncludeInOutConnections(false)//
			.build();
		
		WorkflowDef workflow = m_wfm.copyToDef(spec, PasswordRedactor.asNull()).getPayload();
		var defClipboardContent = m_wfm.copyToDef(spec, PasswordRedactor.asNull());
        var mapper = ObjectMapperUtil.getInstance().getObjectMapper();
        var serializedContent = mapper.writeValueAsString(defClipboardContent);
        assertThat(serializedContent.contains("superSecretName"), is(false));
		// copied one node
		assertThat(workflow.getNodes().size(), is(1));
		
		var componentNodeDef = (ComponentNodeDef) workflow.getNodes().get("4");
		assertThat(componentNodeDef.getCipher(), isNotNull());
	}	
	
	/**
	 * Test copying the joiner node (#3) without dangling connections.
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
