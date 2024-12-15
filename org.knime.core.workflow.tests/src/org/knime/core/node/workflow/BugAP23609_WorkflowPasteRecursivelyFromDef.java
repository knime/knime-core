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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.knime.core.node.ExecutionMonitor;
import org.knime.shared.workflow.storage.clipboard.DefClipboardContent;
import org.knime.shared.workflow.storage.util.PasswordRedactor;
import java.nio.file.Path;

/**
 * Tests the bug described in AP-23609 for components (i.e. {@link SubNodeContainer}s)
 * and metanodes (i.e.{@link WorkflowManager}s).
 * 
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
public class BugAP23609_WorkflowPasteRecursivelyFromDef extends WorkflowTestCase { //NOSONAR

	// nodes the the component test case
	private NodeID m_topComponentID;

	// nodes for the metanode test cast
	private NodeID m_topMetanodeID;

	@TempDir
	Path m_tempFolder;

	@BeforeEach
	public void beforeEach() throws Exception {
		File copiedFolder = m_tempFolder.resolve(BugAP23609_WorkflowPasteRecursivelyFromDef.class.getSimpleName()).toFile();
		FileUtils.copyDirectory(getDefaultWorkflowDirectory(), copiedFolder);
		NodeID baseID = loadAndSetWorkflow(copiedFolder);
		m_topComponentID = new NodeID(baseID, 4);
		m_topMetanodeID = new NodeID(baseID, 5);
	}

	/** Copy the top most component and paste it into itself and once again into the newly pasted component. */
	@Test
	public void testRecursivePasteOfOriginalComponent() throws Exception {
		final WorkflowManager wfm = getManager();
		wfm.removeNode(m_topMetanodeID); // this is the component test, remove metanode.

		final WorkflowCopyContent copyContent = WorkflowCopyContent.builder() //
				.setNodeIDs(m_topComponentID) //
				.setIncludeInOutConnections(false) //
				.build();

		final DefClipboardContent clipboardContent = wfm.copyToDef(copyContent, PasswordRedactor.asNull());
		final WorkflowManager topComponentWFM = 
				((SubNodeContainer)wfm.findNodeContainer(m_topComponentID)).getWorkflowManager();

		WorkflowCopyContent pastedOuterContent = topComponentWFM.paste(clipboardContent);
		final NodeID[] pastedOuterNodeIDs = pastedOuterContent.getNodeIDs();
		assertThat(pastedOuterNodeIDs.length, is(1));
		final NodeID pastedOuterNodeID = pastedOuterNodeIDs[0];
		final SubNodeContainer pastedOuterSubNode = topComponentWFM.getNodeContainer(pastedOuterNodeID,
				SubNodeContainer.class, true);

		final WorkflowManager pastedOuterSubNodeWFM = (pastedOuterSubNode).getWorkflowManager();
		WorkflowCopyContent pastedInnerContent = pastedOuterSubNodeWFM.paste(clipboardContent);
		final NodeID[] pastedInnerNodeIDs = pastedInnerContent.getNodeIDs();
		assertThat(pastedInnerNodeIDs.length, is(1));
		final NodeID pastedInnerNodeID = pastedInnerNodeIDs[0];
		final SubNodeContainer pastedInnerSubNode = pastedOuterSubNodeWFM.getNodeContainer(pastedInnerNodeID,
				SubNodeContainer.class, true);

		assertThat(pastedInnerSubNode, is(notNullValue()));

		final long tableContainerNodeCount = countTableCreatorsInWorkflow(wfm);
		assertThat("Number of Table Creator nodes after pasting", tableContainerNodeCount, is(3L));
		wfm.save(wfm.getNodeContainerDirectory().getFile(), new ExecutionMonitor(), true);
	}

	/** Copy the top most metanode and paste it into itself and once again into the newly pasted component. */
	@Test
	public void testRecursivePasteOfOriginalMetanode() throws Exception {
		final WorkflowManager wfm = getManager();
		wfm.removeNode(m_topComponentID); // this is the metanode test, remove component.

		final WorkflowCopyContent copyContent = WorkflowCopyContent.builder() //
				.setNodeIDs(m_topMetanodeID) //
				.setIncludeInOutConnections(false) //
				.build();

		final DefClipboardContent clipboardContent = wfm.copyToDef(copyContent, PasswordRedactor.asNull());
		final WorkflowManager topMetanodeWFM = (WorkflowManager)wfm.findNodeContainer(m_topMetanodeID);

		WorkflowCopyContent pastedOuterContent = topMetanodeWFM.paste(clipboardContent);
		final NodeID[] pastedOuterNodeIDs = pastedOuterContent.getNodeIDs();
		assertThat(pastedOuterNodeIDs.length, is(1));
		final NodeID pastedOuterNodeID = pastedOuterNodeIDs[0];
		final WorkflowManager pastedOuterMetaNode = topMetanodeWFM.getNodeContainer(pastedOuterNodeID,
				WorkflowManager.class, true);

		WorkflowCopyContent pastedInnerContent = pastedOuterMetaNode.paste(clipboardContent);
		final NodeID[] pastedInnerNodeIDs = pastedInnerContent.getNodeIDs();
		assertThat(pastedInnerNodeIDs.length, is(1));
		final NodeID pastedInnerNodeID = pastedInnerNodeIDs[0];
		final WorkflowManager pastedInnerMetaNode = pastedOuterMetaNode.getNodeContainer(pastedInnerNodeID,
				WorkflowManager.class, true);

		assertThat(pastedInnerMetaNode, is(notNullValue()));

		final long tableContainerNodeCount = countTableCreatorsInWorkflow(wfm);
		assertThat("Number of Table Creator nodes after pasting", tableContainerNodeCount, is(3L));
		wfm.save(wfm.getNodeContainerDirectory().getFile(), new ExecutionMonitor(), true);
	}

	/** Copy the top most component and paste it into itself, then copy top-most compenent again, and paste it into the
	 * previously inserted component. */
	@Test
	public void testRecursivePasteOfModifiedComponent() throws Exception {
		final WorkflowManager wfm = getManager();
		wfm.removeNode(m_topMetanodeID); // this is the component test, remove metanode.

		final WorkflowCopyContent copyContent = WorkflowCopyContent.builder() //
				.setNodeIDs(m_topComponentID) //
				.setIncludeInOutConnections(false) //
				.build();

		final DefClipboardContent clipboardContent = wfm.copyToDef(copyContent, PasswordRedactor.asNull());
		final WorkflowManager topComponentWFM = 
				((SubNodeContainer)wfm.findNodeContainer(m_topComponentID)).getWorkflowManager();

		WorkflowCopyContent pastedOuterContent = topComponentWFM.paste(clipboardContent);
		final NodeID[] pastedOuterNodeIDs = pastedOuterContent.getNodeIDs();
		assertThat(pastedOuterNodeIDs.length, is(1));
		final NodeID pastedOuterNodeID = pastedOuterNodeIDs[0];
		final SubNodeContainer pastedOuterSubNode = topComponentWFM.getNodeContainer(pastedOuterNodeID,
				SubNodeContainer.class, true);

		final WorkflowCopyContent copyContentNew = WorkflowCopyContent.builder() //
				.setNodeIDs(m_topComponentID) //
				.setIncludeInOutConnections(false) //
				.build();
		final DefClipboardContent clipboardContentNew = wfm.copyToDef(copyContentNew, PasswordRedactor.asNull());

		final WorkflowManager pastedOuterSubNodeWFM = (pastedOuterSubNode).getWorkflowManager();
		WorkflowCopyContent pastedInnerContent = pastedOuterSubNodeWFM.paste(clipboardContentNew);
		final NodeID[] pastedInnerNodeIDs = pastedInnerContent.getNodeIDs();
		assertThat(pastedInnerNodeIDs.length, is(1));
		final NodeID pastedInnerNodeID = pastedInnerNodeIDs[0];
		final SubNodeContainer pastedInnerSubNode = pastedOuterSubNodeWFM.getNodeContainer(pastedInnerNodeID,
				SubNodeContainer.class, true);

		assertThat(pastedInnerSubNode, is(notNullValue()));

		final long tableContainerNodeCount = countTableCreatorsInWorkflow(wfm);
		assertThat("Number of Table Creator nodes after pasting", tableContainerNodeCount, is(4L));
		wfm.save(wfm.getNodeContainerDirectory().getFile(), new ExecutionMonitor(), true);
	}

	/** Copy the top most component and paste it into itself, then copy top-most compenent again, and paste it into the
	 * previously inserted component. */
	@Test
	public void testRecursivePasteOfModifiedMetanode() throws Exception {
		final WorkflowManager wfm = getManager();
		wfm.removeNode(m_topComponentID); // this is the metanode test, remove component.

		final WorkflowCopyContent copyContent = WorkflowCopyContent.builder() //
				.setNodeIDs(m_topMetanodeID) //
				.setIncludeInOutConnections(false) //
				.build();

		final DefClipboardContent clipboardContent = wfm.copyToDef(copyContent, PasswordRedactor.asNull());
		final WorkflowManager topMetanodeWFM = (WorkflowManager)wfm.findNodeContainer(m_topMetanodeID);

		WorkflowCopyContent pastedOuterContent = topMetanodeWFM.paste(clipboardContent);
		final NodeID[] pastedOuterNodeIDs = pastedOuterContent.getNodeIDs();
		assertThat(pastedOuterNodeIDs.length, is(1));
		final NodeID pastedOuterNodeID = pastedOuterNodeIDs[0];
		final WorkflowManager pastedOuterMetaNode = topMetanodeWFM.getNodeContainer(pastedOuterNodeID,
				WorkflowManager.class, true);

		final WorkflowCopyContent copyContentNew = WorkflowCopyContent.builder() //
				.setNodeIDs(m_topMetanodeID) //
				.setIncludeInOutConnections(false) //
				.build();
		final DefClipboardContent clipboardContentNew = wfm.copyToDef(copyContentNew, PasswordRedactor.asNull());

		WorkflowCopyContent pastedInnerContent = pastedOuterMetaNode.paste(clipboardContentNew);
		final NodeID[] pastedInnerNodeIDs = pastedInnerContent.getNodeIDs();
		assertThat(pastedInnerNodeIDs.length, is(1));
		final NodeID pastedInnerNodeID = pastedInnerNodeIDs[0];
		final WorkflowManager pastedInnerMetaNode = pastedOuterMetaNode.getNodeContainer(pastedInnerNodeID,
				WorkflowManager.class, true);

		assertThat(pastedInnerMetaNode, is(notNullValue()));

		final long tableContainerNodeCount = countTableCreatorsInWorkflow(wfm);
		assertThat("Number of Table Creator nodes after pasting", tableContainerNodeCount, is(4L));
		wfm.save(wfm.getNodeContainerDirectory().getFile(), new ExecutionMonitor(), true);
	}

	private static int countTableCreatorsInWorkflow(final NodeContainerTemplate wfm) {
		int count = 0;
		for (NodeContainer nc : wfm.getNodeContainers()) {
			if (nc instanceof NativeNodeContainer) {
				if (nc.getName().contains("Creator")) {
					count++;
				}
			} else if (nc instanceof NodeContainerTemplate template) {
				count += countTableCreatorsInWorkflow(template);
			}
		}
		return count;
	}

}