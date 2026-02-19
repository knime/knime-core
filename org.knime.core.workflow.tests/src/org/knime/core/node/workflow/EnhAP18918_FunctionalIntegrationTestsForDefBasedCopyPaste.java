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
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.shared.workflow.def.ConfigMapDef;
import org.knime.shared.workflow.def.ConfigValuePasswordDef;
import org.knime.shared.workflow.def.NativeNodeDef;
import org.knime.shared.workflow.storage.clipboard.DefClipboardContent;
import org.knime.shared.workflow.storage.util.PasswordRedactor;

/**
 * Test the new copy-paste (that uses the *Def classes) by ensuring no
 * properties are lost when copy-pasting
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
public class EnhAP18918_FunctionalIntegrationTestsForDefBasedCopyPaste extends WorkflowTestCase {

	private NodeID id_wf, id_tableCreator, id_outerComp, id_outerMeta, id_concat, id_shuf, id_pie, id_diff1,
			id_credconfig, id_credvalidate, id_linkedmeta, id_diff2, id_gen;

	/**
	 * Initialise all top-level node IDs
	 * 
	 * @throws Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
		id_wf = loadAndSetWorkflow();

		id_tableCreator = new NodeID(id_wf, 1);
		id_outerComp = new NodeID(id_wf, 8);
		id_outerMeta = new NodeID(id_wf, 9);
		id_concat = new NodeID(id_wf, 10);
		id_shuf = new NodeID(id_wf, 12);
		id_pie = new NodeID(id_wf, 13);
		id_diff1 = new NodeID(id_wf, 11);
		id_credconfig = new NodeID(id_wf, 15);
		id_credvalidate = new NodeID(id_wf, 14);
		id_linkedmeta = new NodeID(id_wf, 17);
		id_diff2 = new NodeID(id_wf, 18);
		id_gen = new NodeID(id_wf, 19);
	}

	/**
	 * Check that the resulting table doesn't change when copying a few connected
	 * nodes, including nested components/metaNodes.
	 * 
	 * @throws Exception
	 */
	@Test
	public void copyPasteWorkflow() throws Exception {
		var cc = WorkflowCopyContent.builder()
				.setNodeIDs(id_tableCreator, id_outerComp, id_outerMeta, id_concat, id_shuf, id_pie)
				.setAnnotationIDs(new WorkflowAnnotationID(id_wf, 0)).setIncludeInOutConnections(false).build();
		var pasted = getManager().paste(getManager().copyToDef(cc, PasswordRedactor.asNull()));

		assertThat(getManager().getNodeContainers(), hasSize(12 + 6));
		assertThat(getManager().getAnnotationCount(), is(4 + 1));
		assertThat(getManager().getConnectionContainers(), hasSize(15));

		var id_pastedShuf = findNodeInCC(pasted, "Shuffle");
		getManager().addConnection(id_pastedShuf, 1, id_diff1, 2);
		executeAllAndWait();

		// The table difference checker will only execute, if the results are the same,
		// so at least nothing went terribly wrong.
		checkState(id_diff1, InternalNodeContainerState.EXECUTED);
	}

	/**
	 * Copy-paste a workflow annotation and ensure its appearance doesn't change
	 */
	@Test
	public void copyPasteWorkflowAnnotation() {
		var anno0 = new WorkflowAnnotationID(id_wf, 0);
		var cc = WorkflowCopyContent.builder().setAnnotationIDs(anno0).build();
		var pasted = getManager().paste(getManager().copyToDef(cc, PasswordRedactor.asNull()));
		assertThat(getManager().getAnnotationCount(), is(4 + 1));
		var pastedAnno0 = getManager().getWorkflowAnnotations(pasted.getAnnotationIDs())[0];
		assertThat(pastedAnno0.getText(), startsWith("Workflow Annotation: James, while John"));
		assertThat(pastedAnno0.getBorderColor(), is(3433892));
		assertThat(pastedAnno0.getStyleRanges(), arrayWithSize(6));
	}

	/**
	 * * Copy-paste component contents to the root workflow: This shows that we can
	 * copy-paste from across workflows
	 * 
	 * @throws Exception
	 */
	@Test
	public void copyPasteComponentContents() throws Exception {
		var id_innerComp = NodeID.fromString(id_outerComp + ":0:7");
		var id_innerTableCreator = NodeID.fromString(id_outerComp + ":0:2");
		// Copy table creator and inner component from outer component to root workflow
		var cc = WorkflowCopyContent.builder().setNodeIDs(id_innerComp, id_innerTableCreator)
				.setIncludeInOutConnections(false).build();
		var outerCompContainer = (SubNodeContainer) getManager().getNodeContainer(id_outerComp);

		var pasted = getManager()
				.paste(outerCompContainer.getWorkflowManager().copyToDef(cc, PasswordRedactor.asNull()));

		assertThat(getManager().getNodeContainers(), hasSize(12 + 2));
		executeAllAndWait();

		var id_pastedInnerComp = findNodeInCC(pasted, "Inner Component");
		checkState(id_pastedInnerComp, InternalNodeContainerState.IDLE);

		getManager().addConnection(id_tableCreator, 1, id_pastedInnerComp, 2);
		checkState(id_pastedInnerComp, InternalNodeContainerState.CONFIGURED);
		var diff = newTableDiffCheckerInstance(id_pastedInnerComp, 1, id_outerComp, 1);
		executeAllAndWait();
		checkState(diff, InternalNodeContainerState.EXECUTED);
	}

	/**
	 * Copy-paste a sink node (only incoming connections) and ensure that the
	 * connection is not lost
	 * 
	 * @throws Exception
	 */
	@Test
	public void copyPasteSinkNodeWithConnections() throws Exception {
		var cc = WorkflowCopyContent.builder().setNodeIDs(id_pie).setIncludeInOutConnections(true).build();
		var pasted = getManager().paste(getManager().copyToDef(cc, PasswordRedactor.asNull()));

		executeAndWait(pasted.getNodeIDs()[0]);

		checkState(pasted.getNodeIDs()[0], InternalNodeContainerState.EXECUTED);
	}

	/**
	 * Copy-paste a node that is contained within two components and check that its
	 * settings are properly carried over
	 * 
	 * @throws InvalidSettingsException
	 */
	@Test
	public void copyPasteDoubleNestedNativeNode() throws InvalidSettingsException {
		// WFM of most nested metanode contents
		var innerMetaWFM = (WorkflowManager) ((WorkflowManager) getManager().getNodeContainer(id_outerMeta))
				.getNodeContainer(NodeID.fromString(id_wf + ":9:6"));
		var cc = WorkflowCopyContent.builder().setNodeIDs(NodeID.fromString(id_wf + ":9:6:5"))
				.setIncludeInOutConnections(false).build();
		var pasted = getManager().paste(innerMetaWFM.copyToDef(cc, PasswordRedactor.asNull()));
		var pastedConcat = findNodeContainer(pasted.getNodeIDs()[0]);
		assertThat(pastedConcat.getName(), is("Concatenate"));
		assertThat(pastedConcat.getNodeSettings().getConfigBase("model").getString("suffix"), is("_dup"));
	}

	/**
	 * Check that the node model is not lost when copying by checking the generated
	 * values of a pasted table creator
	 * 
	 * @throws InvalidSettingsException
	 */
	@Test
	public void copyPasteNodeWithModel() throws InvalidSettingsException {
		var cc = WorkflowCopyContent.builder().setNodeIDs(id_tableCreator).setIncludeInOutConnections(false).build();
		var pastedTableCreator = findNodeContainer(
				getManager().paste(getManager().copyToDef(cc, PasswordRedactor.asNull())).getNodeIDs()[0]);
		var nodeModel = pastedTableCreator.getNodeSettings().getConfigBase("model");
		assertThat(nodeModel.getStringArray("values"), is(new String[] { "Bernd", "Carl", "Dionysios" }));
	}

	/**
	 * Copy nested annotations and paste them in the root WF
	 */
	@Test
	public void copyPasteNestedMetaNodeAnnotations() {
		var cc = WorkflowCopyContent.builder().setNodeIDs(id_outerMeta).setIncludeInOutConnections(false).build();
		var pastedOuterMeta = (WorkflowManager) findNodeContainer(
				getManager().paste(getManager().copyToDef(cc, PasswordRedactor.asNull())).getNodeIDs()[0]);
		var pastedOuterMetaWF = pastedOuterMeta.getWorkflow();
		var pastedInnerMeta = (WorkflowManager) pastedOuterMetaWF.getNode(new NodeID(pastedOuterMetaWF.getID(), 6));

		assertThat(pastedOuterMeta.getAnnotationCount(), is(1));
		assertThat(pastedInnerMeta.getAnnotationCount(), is(1));

		var outerAnno = pastedOuterMeta.getWorkflowAnnotations().iterator().next();
		var innerAnno = pastedInnerMeta.getWorkflowAnnotations().iterator().next();

		assertThat(outerAnno.getText(), is("Outer Meta Annotation"));
		assertThat(innerAnno.getText(), is("Inner Meta Annotation"));
	}

	/**
	 * Copy a linked metaNode and ensure that the Link information is preserved
	 * 
	 * @throws Exception
	 */
	@Test
	public void copyPasteLinkedMetanode() throws Exception {
		var cc = WorkflowCopyContent.builder().setNodeIDs(id_linkedmeta).setIncludeInOutConnections(false).build();
		var pastedLinkedMeta = getManager().paste(getManager().copyToDef(cc, PasswordRedactor.asNull()))
				.getNodeIDs()[0];

		assertThat(getManager().getLinkedMetaNodes(false), hasSize(2));
		var templateInfo = ((WorkflowManager) findNodeContainer(pastedLinkedMeta)).getTemplateInformation();

		assertThat(templateInfo.getRole(), is(MetaNodeTemplateInformation.Role.Link));
		assertThat(templateInfo.getSourceURI().toString(), is("knime://LOCAL/Linked_Metanode"));

		getManager().addConnection(pastedLinkedMeta, 0, id_diff2, 2);
		executeAndWait(id_diff2);
		checkState(id_diff2, InternalNodeContainerState.EXECUTED);
	}

	/**
	 * Copy-Paste nodes that 1. have flow variable ports, 2. use passwords.
	 * 
	 * @throws Exception
	 */
	@Test
	public void copyPasteCredentialConfig() throws Exception {
		var cc = WorkflowCopyContent.builder().setNodeIDs(id_credconfig).setIncludeInOutConnections(false).build();

		// looks spooky, but just extracts encrypted password from copied node and
		// checks its value
		var copiedUnsafe = getManager().copyToDef(cc, PasswordRedactor.unsafe());
		Function<DefClipboardContent, ConfigValuePasswordDef> passwordDef = dcc -> 
			((ConfigValuePasswordDef) ((ConfigMapDef) ((NativeNodeDef) dcc.getPayload().getNodes()
				.get("15")).getModelSettings().getChildren().get("defaultValue"))
				.getChildren().get("passwordEncrypted")); 
		assertThat(passwordDef.apply(copiedUnsafe).getValue(),
				is("02BAAAAGMq_QxveL1vZ0EBj9UiWy7_u1C6"));

		// same, but here we require the password value to be null
		var copiedAsNull = getManager().copyToDef(cc, PasswordRedactor.asNull());
		assertThat(passwordDef.apply(copiedAsNull).getValue(),
				nullValue());

		// when pasting the redacted version, the unredacted version should be available and pasted instead
		var pastedConfig = getManager().paste(copiedAsNull).getNodeIDs()[0];
		getManager().removeConnection(findInConnection(id_credvalidate, 1));
		getManager().addConnection(pastedConfig, 1, id_credvalidate, 1);
		executeAndWait(id_credvalidate);
		checkState(id_credvalidate, InternalNodeContainerState.EXECUTED);
	}

	/**
	 * Copy-Paste a node with a custom description, validate that it is still there
	 * after pasting
	 */
	@Test
	public void copyPasteNativeNodeWithCustomDescription() {
		var cc = WorkflowCopyContent.builder().setNodeIDs(id_tableCreator).setIncludeInOutConnections(false).build();
		var pastedCreator = findNodeContainer(
				getManager().paste(getManager().copyToDef(cc, PasswordRedactor.asNull())).getNodeIDs()[0]);
		assertThat(pastedCreator.getCustomDescription(), is("creates a table"));
	}

	/**
	 * Copy-Paste a node with a node annotation (that even has a style range)
	 */
	@Test
	public void copyPasteComponentWithNodeAnnotation() {
		var cc = WorkflowCopyContent.builder().setNodeIDs(id_outerComp).setIncludeInOutConnections(false).build();
		var pastedComponent = findNodeContainer(
				getManager().paste(getManager().copyToDef(cc, PasswordRedactor.asNull())).getNodeIDs()[0]);
		var anno = pastedComponent.getNodeAnnotation();
		assertThat(anno.getText(), is("outer_component_bold_nodeannotation"));
		assertThat(anno.getBgColor(), is(16777215));
		assertThat(anno.getStyleRanges(), arrayWithSize(1));
	}

	/**
	 * Copy-Paste a node that has a non-default Job Manager
	 */
	@Test
	public void copyPasteJobManager() {
		var cc = WorkflowCopyContent.builder().setNodeIDs(id_gen).setIncludeInOutConnections(false).build();
		var pastedGen = findNodeContainer(
				getManager().paste(getManager().copyToDef(cc, PasswordRedactor.asNull())).getNodeIDs()[0]);
		assertThat(pastedGen.getJobManager().getID(),
				is("org.knime.testing.streaming.testexecutor.StreamingTestNodeExecutionJobManagerFactory"));
		// how to check that pastedGen.getJobManager().m_numChunks == 42? API does not
		// expose field
	}

	/**
	 * Copy-Paste nodes to check that the memory policy is transferred correctly
	 */
	@Test
	public void copyPasteMemoryPolicy() {
		var cc = WorkflowCopyContent.builder().setNodeIDs(id_tableCreator, id_gen).setIncludeInOutConnections(false)
				.build();
		var pastedCC = getManager().paste(getManager().copyToDef(cc, PasswordRedactor.asNull()));

        final var pastedTableCreator = (SingleNodeContainer) findNodeContainer(
                findNodeInCC(pastedCC, "Table Creator (deprecated)"));
		assertThat(pastedTableCreator.getOutDataMemoryPolicy(),
				is(SingleNodeContainer.MemoryPolicy.CacheSmallInMemory));

		var pastedDataGenerator = (SingleNodeContainer) findNodeContainer(findNodeInCC(pastedCC, "Data Generator"));
		assertThat(pastedDataGenerator.getOutDataMemoryPolicy(), is(SingleNodeContainer.MemoryPolicy.CacheOnDisc));
	}

	/**
	 * Helper function that extracts a node ID from {@link WorkflowCopyContent}
	 * based on its name
	 * 
	 * @param cc
	 * @param nodeType
	 * @return
	 */
	private NodeID findNodeInCC(final WorkflowCopyContent cc, final String nodeType) {
		for (var nid : cc.getNodeIDs()) {
			var nc = getManager().getNodeContainer(nid);
			var nt = nc.getName();
			if (nt.equals(nodeType)) {
				return nid;
			}
		}
		return null;
	}

	/**
	 * Create a new table checker instance (by copying node 11) and connect two
	 * nodes to it
	 * 
	 * @param nodeA
	 * @param portA
	 * @param nodeB
	 * @param portB
	 * @return the ID of the created table checker
	 */
	private NodeID newTableDiffCheckerInstance(NodeID nodeA, int portA, NodeID nodeB, int portB) {
		var cc = WorkflowCopyContent.builder().setNodeIDs(id_diff1).setIncludeInOutConnections(false).build();
		var pasted_diff = getManager().paste(getManager().copyToDef(cc, PasswordRedactor.asNull())).getNodeIDs()[0];
		getManager().addConnection(nodeA, portA, pasted_diff, 1);
		getManager().addConnection(nodeB, portB, pasted_diff, 2);
		return pasted_diff;
	}

}