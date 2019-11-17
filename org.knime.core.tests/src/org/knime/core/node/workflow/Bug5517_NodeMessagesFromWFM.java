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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.workflow.NodeMessage.Type;
import org.knime.core.util.Pair;


/** 5517: New API method to query node messages from workflow instance (needed in KNIME server)
 * http://bimbug.inf.uni-konstanz.de/show_bug.cgi?id=5517
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class Bug5517_NodeMessagesFromWFM extends WorkflowTestCase {

    private NodeID m_tableCreator_1;
    private NodeID m_rowFilterEmpty_2;
    private NodeID m_metaNodeFail_3;
    private NodeID m_rowFilterNone_4;
    private NodeID m_failInMeta_3_1;
    private NodeID m_subNodeFail_6;
    private NodeID m_failInSub_6_1;
    private NodeID m_rowFilterNone_5;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_tableCreator_1 = new NodeID(baseID, 1);
        m_rowFilterEmpty_2 = new NodeID(baseID, 2);
        m_metaNodeFail_3 = new NodeID(baseID, 3);
        m_rowFilterNone_4 = new NodeID(baseID, 4);
        m_failInMeta_3_1 = new NodeID(m_metaNodeFail_3, 1);
        m_subNodeFail_6 = new NodeID(baseID, 6);
        m_failInSub_6_1 = new NodeID(new NodeID(m_subNodeFail_6, 0), 1);
        m_rowFilterNone_5 = new NodeID(baseID, 5);
    }

    @Test
    public void testMessageAfterExecute() throws Exception {
        WorkflowManager manager = getManager();
        List<Pair<String, NodeMessage>> messages = manager.getNodeMessages(Type.WARNING, Type.ERROR);
        Assert.assertNotNull(messages);
        removeSubnodeWarningNoExecutedPredecessor(messages);
        Assert.assertTrue("Non empty: " + messages, messages.isEmpty());
        checkStateOfMany(InternalNodeContainerState.CONFIGURED, m_tableCreator_1, m_rowFilterEmpty_2, m_metaNodeFail_3,
            m_rowFilterNone_4, m_failInMeta_3_1, m_subNodeFail_6, m_failInSub_6_1, m_rowFilterNone_5);
        executeAllAndWait();
        checkStateOfMany(InternalNodeContainerState.EXECUTED, m_tableCreator_1, m_rowFilterEmpty_2);
        List<Pair<String, NodeMessage>> warnMsgs = manager.getNodeMessages(NodeMessage.Type.WARNING);
        List<Pair<String, NodeMessage>> errorMsgs = manager.getNodeMessages(NodeMessage.Type.ERROR);
        List<Pair<String, NodeMessage>> warnErrorMsgs =
                manager.getNodeMessages(NodeMessage.Type.WARNING, NodeMessage.Type.ERROR);

        Assert.assertEquals(1, warnMsgs.size());
        Assert.assertEquals(2, errorMsgs.size());
        Assert.assertEquals(3, warnErrorMsgs.size());

        Pair<String, NodeMessage> pair = warnMsgs.get(0);
        Assert.assertTrue(pair.getFirst().contains(manager.getNodeContainer(m_rowFilterEmpty_2).getName()));
        Assert.assertTrue(pair.getSecond().getMessage().contains("empty"));

        List<NodeID> expectedErrorIDs = Arrays.asList(m_failInMeta_3_1, m_failInSub_6_1);
        for (Pair<String, NodeMessage> p : errorMsgs) {
            String nodeName = p.getFirst();
            boolean found = false;
            for (NodeID id : expectedErrorIDs) {
                if (nodeName.contains(findNodeContainer(id).getNameWithID())) {
                    found = true;
                    break;
                }
            }
            Assert.assertTrue("Corresponding node with error msg not found: " + nodeName, found);
            // "This node fails on each execution"
            Assert.assertTrue(p.getSecond().getMessage().contains("fail"));
        }
        reset(m_tableCreator_1);
        messages = manager.getNodeMessages(Type.WARNING, Type.ERROR);
        Assert.assertNotNull(messages);
        removeSubnodeWarningNoExecutedPredecessor(messages);
        Assert.assertTrue("Non empty: " + messages, messages.isEmpty());
    }

    /** Remove first warning message associated with "subnode has no data" message. Workaround that we remove
     * when we find a better solution to the problem that subnodes need full input data before executable.
     * @param messages list that gets modified.
     */
    private void removeSubnodeWarningNoExecutedPredecessor(final List<Pair<String, NodeMessage>> messages) {
        for (Iterator<Pair<String, NodeMessage>> it = messages.iterator(); it.hasNext(); ) {
            Pair<String, NodeMessage> m = it.next();
            NodeMessage msg = m.getSecond();
            if (NodeMessage.Type.WARNING.equals(msg.getMessageType())
                    && msg.getMessage().contains("Component does not have input data, execute upstream nodes first")) {
                it.remove();
                break;
            }
        }
    }


}
