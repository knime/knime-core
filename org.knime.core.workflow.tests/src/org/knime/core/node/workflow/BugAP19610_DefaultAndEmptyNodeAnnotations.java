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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.shared.workflow.storage.util.PasswordRedactor;

/**
 * Test that copy and paste leaves empty annotations empty
 * 
 * Regression AP-19610 describes that starting from 4.6.?, pasted nodes always
 * get the default node annotation prefix plus the node ID as annotation.
 * 
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class BugAP19610_DefaultAndEmptyNodeAnnotations extends WorkflowTestCase { // NOSONAR this naming is our standard

    private WorkflowManager m_wfm;

    /**
     * Three Row Filter nodes, one with empty annotation ("") and one with non-default annotation ("Yak") and one with
     * default annotation ("Node 3").
     */
    private NodeID m_without, m_withNonDefault, m_withDefault;

    @BeforeEach
    public void beforeEach() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_wfm = getManager();
        m_without = new NodeID(baseID, 1);
        m_withNonDefault = new NodeID(baseID, 2);
        m_withDefault = new NodeID(baseID, 3);
    }

    /**
     * Copy and paste a node with an empty annotation (empty string). The pasted
     * node should also have an empty string as node annotation.
     */
    @Test
    public void copyNodeWithEmptyAnnotation() throws Exception {
        var pastedNodeId = copyPaste(m_without);
        var pastedNode = m_wfm.getNodeContainer(pastedNodeId);

        // hashed password is still the same (to make sure the cipher is still intact)
        assertEquals(
                "",
                pastedNode.getNodeAnnotation().getText(), "Copying a node with empty node annotation should paste a node with empty node annotation.");
    }

    /**
     * Copy and paste a node with default node annotation ("Node " + NodeID, e.g., "Node 3"). The pasted
     * node should also have a node annotation following the default schema (e.g., "Node 4").
     */
    @Test
    public void copyNodeWithDefaultAnnotation() throws Exception {
        var pastedNodeId = copyPaste(m_withDefault);
        var pastedNode = m_wfm.getNodeContainer(pastedNodeId);

        assertTrue(pastedNode.getNodeAnnotation().getData().isDefault(),
                "Copying a node with default node annotation should paste a node with a default node annotation.");
    }

    /**
     * Copy and paste a node with custom node annotation should simply copy the annotation.
     */
    @Test
    public void copyNodeWithNonDefaultAnnotation() throws Exception {
        var pastedNodeId = copyPaste(m_withNonDefault);
        var pastedNode = m_wfm.getNodeContainer(pastedNodeId);

        // The new node should have ID 4 and the node annotation should be "Node 4"
        final var expected = m_wfm.getNodeContainer(m_withNonDefault).getNodeAnnotation().getText();
        assertEquals(expected,
                pastedNode.getNodeAnnotation().getText(), "Copying a node with default node annotation should paste a node with a default node annotation.");
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

}