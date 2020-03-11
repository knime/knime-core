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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.node.workflow.action.CollapseIntoMetaNodeResult;

/**
 * Bug 6336: Premature state notification in case workflow contains loops (problems on server)
 * https://bugs.knime.org/show_bug.cgi?id=6336
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class Bug6336_LoopsInServer_States extends WorkflowTestCase {

    private NodeID m_dataGenerator1;
    private NodeID m_tableView12;
    private NodeID m_loopStart3;
    private NodeID m_loopEnd4;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_dataGenerator1 = new NodeID(baseID, 1);
        m_loopStart3 = new NodeID(baseID, 3);
        m_loopEnd4 = new NodeID(baseID, 4);
        m_tableView12 = new NodeID(baseID, 12);
    }

    /** Loads workflow and executes as is - expects certain event count. */
    @Test
    public void testExecutePlain() throws Exception {
        final WorkflowManager manager = getManager();
        checkState(manager, InternalNodeContainerState.IDLE);
        checkListenerAndStateAfterExecAll(manager);
    }

    /** Loads workflow and collapses almost all nodes, then executes - expects certain event count. */
    @Test
    public void testExecuteAfterCollapse() throws Exception {
        Assume.assumeTrue("Disabled - occassionally failing, see AP-5572", false);
        final WorkflowManager manager = getManager();
        checkState(manager, InternalNodeContainerState.IDLE);
        WorkflowManager metaNode = collapseToMetaNode(manager);
        checkListenerAndStateAfterExecAll(manager);

        manager.resetAndConfigureAll();
        checkState(manager, InternalNodeContainerState.IDLE);

        manager.convertMetaNodeToSubNode(metaNode.getID());
        checkListenerAndStateAfterExecAll(manager);
    }

    /** Collapse to meta node, then copy and connect to its original instance, then exec all. */
    @Test
    public void testExecuteAfterCollapseAndClone() throws Exception {
        Assume.assumeTrue("Disabled - occassionally failing, see AP-5572", false);
        final WorkflowManager manager = getManager();
        WorkflowManager metaNode = collapseToMetaNode(manager);
        WorkflowCopyContent.Builder copyContent = WorkflowCopyContent.builder();
        copyContent.setNodeIDs(metaNode.getID());
        WorkflowCopyContent pasteContent = manager.copyFromAndPasteHere(manager, copyContent.build());
        NodeID copyID = pasteContent.getNodeIDs()[0];
        manager.addConnection(metaNode.getID(), 0, copyID, 0);
        checkListenerAndStateAfterExecAll(manager);
        manager.resetAndConfigureAll();
        manager.expandMetaNode(copyID);
        checkListenerAndStateAfterExecAll(manager);
    }

    /**
     * @param manager
     * @return
     */
    private WorkflowManager collapseToMetaNode(final WorkflowManager manager) {
        Set<NodeID> ids = new LinkedHashSet<>(manager.getWorkflow().getNodeIDs());
        assertTrue(ids.remove(m_dataGenerator1));
        assertTrue(ids.remove(m_tableView12));
        final CollapseIntoMetaNodeResult collapseResult = manager.collapseIntoMetaNode(
            ids.toArray(new NodeID[0]), new WorkflowAnnotation[0], "Collapsed Content");
        WorkflowManager metaNode = manager.getNodeContainer(
            collapseResult.getCollapsedMetanodeID(), WorkflowManager.class, true);
        // remaining: data gen, table view, and new meta node
        assertEquals("Node count after collapse incorrect", 3, manager.getWorkflow().getNrNodes());
        assertTrue(manager.containsNodeContainer(m_dataGenerator1));
        assertTrue(manager.containsNodeContainer(m_tableView12));
        assertFalse(manager.containsNodeContainer(m_loopStart3));
        assertFalse(manager.containsNodeContainer(m_loopEnd4));
        return metaNode;
    }

    /**
     * @param manager
     * @throws Exception
     */
    private void checkListenerAndStateAfterExecAll(final WorkflowManager manager) throws Exception {
        final StringBuilder msg = new StringBuilder();
        final List<InternalNodeContainerState> eventList = new ArrayList<InternalNodeContainerState>();
        manager.addNodeStateChangeListener(new NodeStateChangeListener() {
            @Override
            public void stateChanged(final NodeStateEvent state) {
                final InternalNodeContainerState curState = manager.getInternalState();
                final int count = eventList.size();
                eventList.add(curState);
                switch (count) {
                    case 0:
                        if (!curState.equals(InternalNodeContainerState.EXECUTING)) {
                            msg.append(msg.length() == 0 ? "" : "\n");
                            msg.append("First event should be EXECUTING but is ").append(curState);
                        }
                        break;
                    case 1:
                        if (!curState.equals(InternalNodeContainerState.EXECUTED)) {
                            msg.append(msg.length() == 0 ? "" : "\n");
                            msg.append("Second event should be EXECUTED but is ").append(curState);
                        }
                        break;
                    default:
                }
            }
        });
        executeAllAndWait();
        if (eventList.size() != 2) {
            msg.append(msg.length() == 0 ? "" : "\n");
            msg.append("Too many events received: ").append(eventList.size()).append(" -- ").append(
                ConvenienceMethods.getShortStringFrom(eventList, 10));
        }
        checkState(manager, InternalNodeContainerState.EXECUTED);
        if (msg.length() > 0) {
            fail(msg.toString());
        }
    }

}
