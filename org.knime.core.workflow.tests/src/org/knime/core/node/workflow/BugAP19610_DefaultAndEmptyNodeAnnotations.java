MISSINGpackage org.knime.core.node.workflow;

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
                "Copying a node with empty node annotation should paste a node with empty node annotation.",
                "", pastedNode.getNodeAnnotation().getText());
    }

    /**
     * Copy and paste a node with default node annotation ("Node " + NodeID, e.g., "Node 3"). The pasted
     * node should also have a node annotation following the default schema (e.g., "Node 4").
     */
    @Test
    public void copyNodeWithDefaultAnnotation() throws Exception {
        var pastedNodeId = copyPaste(m_withDefault);
        var pastedNode = m_wfm.getNodeContainer(pastedNodeId);

        assertTrue("Copying a node with default node annotation should paste a node with a default node annotation.",
                pastedNode.getNodeAnnotation().getData().isDefault());
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
        assertEquals("Copying a node with default node annotation should paste a node with a default node annotation.",
                expected, pastedNode.getNodeAnnotation().getText());
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