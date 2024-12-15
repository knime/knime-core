MISSINGpackage org.knime.core.node.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BugAP21872_ResetMetanodes extends WorkflowTestCase {

    private NodeID m_sourceNode;

    private NodeID m_throughSucessor;

    private NodeID m_viaSuccessor;

    private NodeID m_metaNode;

    private NodeID m_baseId;

    @BeforeEach
    public void setUp() throws Exception {
        m_baseId = loadAndSetWorkflow();
        m_sourceNode = new NodeID(m_baseId, 1);
        m_metaNode = new NodeID(m_baseId, 6);

        // successor of source via connections "through" the metanode
        m_throughSucessor = new NodeID(m_baseId, 4);
        // successor of source "via" metanode (disregarding the workflow inside the metanode, as if the metanode were
        //  a simple single node.
        m_viaSuccessor = new NodeID(m_baseId, 5);
        executeAllAndWait();

        getManager().resetAndConfigureNode(m_sourceNode, true);
    }

    @Test
    public void testSuccessorsAreReset() throws Exception {
        assertConfigured(m_throughSucessor, m_viaSuccessor);
    }

    /**
     * Nothing should have changed here with this ticket
     */
    @Test
    public void testAllInsideMetanodeAreReset() throws Exception {
        var metaNodeWfm = (WorkflowManager)getManager().getNodeContainer(m_metaNode);
        var connectedToWfInAndOut = metaNodeWfm.getNodeContainer(m_metaNode.createChild(2));
        var connectedtoWfOutWithSuccessorOutside = metaNodeWfm.getNodeContainer(m_metaNode.createChild(3));
        var connectedtoWfOutWithoutSuccessorOutside = metaNodeWfm.getNodeContainer(m_metaNode.createChild(4));
        assertConfigured(connectedToWfInAndOut, connectedtoWfOutWithSuccessorOutside,
            connectedtoWfOutWithoutSuccessorOutside);
    }

    @Test
    public void checkMetaOutState() throws Exception {
        checkMetaOutState(m_metaNode, 0, InternalNodeContainerState.CONFIGURED);
        checkMetaOutState(m_metaNode, 1, InternalNodeContainerState.CONFIGURED);
        checkMetaOutState(m_metaNode, 2, InternalNodeContainerState.CONFIGURED);
    }

    private void assertConfigured(final NodeID... nodes) throws Exception {
        for (NodeID node : nodes) {
            checkState(node, InternalNodeContainerState.CONFIGURED);
        }
    }

    private void assertConfigured(final NodeContainer... nodes) throws Exception {
        for (NodeContainer node : nodes) {
            checkState(node, InternalNodeContainerState.CONFIGURED);
        }
    }

}