package org.knime.core.node.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.knime.shared.workflow.storage.clipboard.SystemClipboardFormat;
import org.knime.shared.workflow.storage.util.PasswordRedactor;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Test for the regression that allowed to unlock locked components via simple
 * copy and paste (sic!). See {@link #copyLockedComponent()} and
 * {@link #copyLockedMetanode()}.
 * 
 * Test for the regression that when copying locked components or metanodes, a
 * more or less plain text description would appear in the system clipboard that
 * would also allow to paste an unlocked version by simple erasure of the
 * ciphers and alteration of the payload identifier. See
 * {@link #systemClipboardRepresentationIsObfuscated()}.
 * 
 * Test workflow has the following structure
 * 
 * <pre>
├── Unlocked Component #4
│   └── Outer Locked #3
│       └── Inner Locked #4
│           └── Table Creator #1, outputs single row with one String cell with value "secret"
├── Unlocked Metanode #5
│   └── Outer Locked #3
│       └── Inner Locked #4
│           └── Table Creator #1, outputs single row with one String cell with value "secret" 
└── KNIME Server Connector #6, uses "secret" as password
 * </pre>
 */
public class BugAP19479_Component_Encryption extends WorkflowTestCase {

	private WorkflowManager m_wfm;

	private NodeID m_topComponentId, m_topMetanodeId, m_serverConnectorId;

	private WorkflowAnnotationID m_sourceAnnotationID;

	@Before
	public void beforeEach() throws Exception {
		NodeID baseID = loadAndSetWorkflow();
		m_wfm = getManager();
		m_topComponentId = new NodeID(baseID, 4);
		m_topMetanodeId = new NodeID(baseID, 5);
		m_serverConnectorId = new NodeID(baseID, 6);
	}

	/**
	 * Generate a string representation of a locked component and a locked metanode
	 * as would be stored in the system clipboard. Make sure that this
	 * representation is obfuscated
	 */
	@Test
	public void systemClipboardRepresentationIsObfuscated() throws Exception {
		final WorkflowCopyContent spec = WorkflowCopyContent.builder()//
				.setNodeIDs(m_topComponentId, m_topMetanodeId, m_serverConnectorId)//
				.setIncludeInOutConnections(false)//
				.build();

		// remove passwords during copy operation
		var defClipboardContent = m_wfm.copyToDef(spec, PasswordRedactor.asNull());

		// obfuscate to hide locked components and metanodes
		String toCopy = SystemClipboardFormat.serialize(defClipboardContent);

		// should not contain, e.g., node settings in plain text
		assertFalse(
				"System clipboard representation is unsafe. Must not contain the plain text string \"secret\" but is "
						+ toCopy,
				toCopy.contains("secret"));
	}

	/**
	 * Test serializing workflow content for the system clipboard and deserializing
	 * it.
	 */
	@Test
	public void systemClipboardRepresentationRoundtrip() throws Exception {
		final WorkflowCopyContent spec = WorkflowCopyContent.builder()//
				.setNodeIDs(m_topComponentId, m_topMetanodeId)//
				.setIncludeInOutConnections(false)//
				.build();

		// remove passwords during copy operation
		var defClipboardContent = m_wfm.copyToDef(spec, PasswordRedactor.asNull());

		// prepare for copying into system clipboard (obfuscate)
		String toCopy = SystemClipboardFormat.serialize(defClipboardContent);
		// restore
		var deserialized = SystemClipboardFormat.deserialize(toCopy);

		// make sure everything is still intact after one roundtrip
		assertEquals(
				"System clipboard representation is broken. Deserialized payload identifier must be equal to serialized content.",
				defClipboardContent.getPayloadIdentifier(), deserialized.getPayloadIdentifier());

		assertEquals(
				"System clipboard representation is broken. Deserialized version must be equal to serialized content.",
				defClipboardContent.getVersion(), deserialized.getVersion());

		// payload comparison via representation. Equals as in defClipboardContent.equals(deserialized) currently does
		// not work because for instance the MetanodeToDefAdapter contained in defClipboardContent does not override 
		// equals and the overriden equals of DefaultMetaNodeDef works only when comparing to other DefaultMetaNodeDef
		// instances (and the copy constructor of a DefaultMetaNodeDef doesn't make a deep copy).
		var mapper = mapperForComparison();
		var expectedRepresentation = mapper.writerWithDefaultPrettyPrinter()
				.writeValueAsString(defClipboardContent.getPayload());
		var actualRepresentation = mapper.writerWithDefaultPrettyPrinter()
				.writeValueAsString(deserialized.getPayload());

		assertEquals(
				"System clipboard representation is broken. Deserialized payload must be equal to serialized content.",
				expectedRepresentation, actualRepresentation);

		assertEquals(
				"System clipboard representation is broken. Deserialized payload must be equal to serialized content.",
				expectedRepresentation, actualRepresentation);
	}

	/**
	 * Copy and paste a locked metanode. The pasted node should also be locked.
	 */
	@Test
	public void copyLockedMetanode() throws Exception {
		var pastedMetanodeId = copyPaste(m_topMetanodeId);
		// the top metanode is unlocked
		var pastedMetanode = m_wfm.getNodeContainer(pastedMetanodeId);
		// it's only child node should be locked
		var pastedLockedMetanode = ((WorkflowManager) pastedMetanode).getNodeContainer(pastedMetanodeId.createChild(3));
		// hashed password is still the same (to make sure the cipher is still intact)
		assertEquals(
				"Locked metanode seems to have been unlocked during copy and paste operation. "
						+ "The cipher was broken through alteration of the password digest.",
				"C3499C2729730A7F807EFB8676A92DCB6F8A3F8F",
				((WorkflowManager) pastedLockedMetanode).getWorkflowCipher().toDef().getPasswordDigest());
	}

	/**
	 * Copy and paste a locked component. The pasted node should also be locked.
	 */
	@Test
	public void copyLockedComponent() throws Exception {
		var pastedComponentId = copyPaste(m_topComponentId);
		// the top component is unlocked
		SubNodeContainer pastedComponent = (SubNodeContainer) m_wfm.getNodeContainer(pastedComponentId);
		// it's only child node should be locked
		SubNodeContainer pastedLockedComponent = (SubNodeContainer) pastedComponent.getWorkflowManager()
				.getNodeContainer(pastedComponentId.createChild(0).createChild(3));
		// by (confusing) convention, the locked component does not define the cipher
		// itself, but a null cipher...
		assertTrue("Pasted locked component should not have a cipher (its contained workflow should).",
				pastedLockedComponent.getWorkflowCipher().isNullCipher());
		// ...and the workflow manager in the locked component defines the cipher
		assertEquals(
				"Locked component seems to have been unlocked during copy and paste operation. "
						+ "The cipher was broken through alteration of the password digest.",
				"C3499C2729730A7F807EFB8676A92DCB6F8A3F8F",
				pastedLockedComponent.getWorkflowManager().getWorkflowCipher().toDef().getPasswordDigest());
	}

	/**
	 * Copy the given node from {@link #m_wfm} and paste it into {@link #m_wfm}.
	 * 
	 * @return the node id of the pasted node
	 */
	private NodeID copyPaste(NodeID nodeId) {
		final WorkflowCopyContent spec = WorkflowCopyContent.builder()//
				.setNodeIDs(nodeId)//
				.setIncludeInOutConnections(false)//
				.build();
		var defClipboardContent = m_wfm.copyToDef(spec, PasswordRedactor.asNull());

		var pasteSpec = m_wfm.paste(defClipboardContent);
		return pasteSpec.getNodeIDs()[0];
	}

	/**
	 * @return an object mapper that sorts properties and map entries in order to be
	 *         able to compare two representations
	 */
	private JsonMapper mapperForComparison() {
		var mapper = // ObjectMapperUtil.getInstance().getObjectMapper();
				JsonMapper.builder().configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true).build();
		mapper.registerModule(new Jdk8Module());
		mapper.registerModule(new JavaTimeModule());
		mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
		return mapper;
	}
	
}
