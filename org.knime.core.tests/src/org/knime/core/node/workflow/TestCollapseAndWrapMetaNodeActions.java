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
 *
 * History
 *   Mar 13, 2014 ("Patrick Winter"): created
 */
package org.knime.core.node.workflow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.workflow.action.CollapseIntoMetaNodeResult;
import org.knime.core.node.workflow.action.MetaNodeToSubNodeResult;
import org.knime.core.node.workflow.action.SubNodeToMetaNodeResult;

/**
 * A couple nodes collapse into a meta node, then wrapped into subnode, undo, redo.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class TestCollapseAndWrapMetaNodeActions extends WorkflowTestCase {

    private NodeID m_columnFilter_2;
    private NodeID m_columnFilter_3;
    private NodeID m_columnSplitter_4;
    private NodeID m_javaEdit_7;
    private NodeID m_tableView_6;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_columnFilter_2 = new NodeID(baseID, 2);
        m_columnFilter_3 = new NodeID(baseID, 3);
        m_columnSplitter_4 = new NodeID(baseID, 4);
        m_javaEdit_7 = new NodeID(baseID, 7);
        m_tableView_6 = new NodeID(baseID, 6);
    }

    /** Collect nodes, collapse them, undo. */
    @Test
    public void testCollapseIntoMetaNodeThenUndo() throws Exception {
        WorkflowManager mgr = getManager();
        executeAllAndWait();
        checkState(mgr, EXECUTED);

        mgr.resetAndConfigureAll();
        // there is only one in the wfm
        WorkflowAnnotation annotation = mgr.getWorkflowAnnotations().stream().findFirst().get();
        final NodeID[] nodes = new NodeID[] {m_columnFilter_2, m_columnFilter_3, m_columnSplitter_4};
        CollapseIntoMetaNodeResult collapseResult = mgr.collapseIntoMetaNode(nodes,
            new WorkflowAnnotation[] {annotation}, "Test-Meta/Wrap Node");
        NodeID metaSubID = collapseResult.getCollapsedMetanodeID();
        mgr.getNodeContainer(metaSubID, WorkflowManager.class, true);
        assertFalse("Should have removed node: " + m_columnFilter_2, mgr.containsNodeContainer(m_columnFilter_2));
        assertTrue("No annotation expected", mgr.getWorkflowAnnotations().isEmpty());

        executeAllAndWait();
        checkState(mgr, EXECUTED);
        mgr.resetAndConfigureNode(metaSubID);
        assertTrue("Should be able to undo collapse", collapseResult.canUndo());
        collapseResult.undo();
        assertTrue("Should have restored node: " + m_columnFilter_2, mgr.containsNodeContainer(m_columnFilter_2));
        assertFalse("Annotation expected", mgr.getWorkflowAnnotations().isEmpty());

    }

    /** Collect nodes, collapse them, convert to meta node and wrap/unwrap. */
    @Test
    public void testCollapseIntoMetaNodeThenWrapUnwrap() throws Exception {
        WorkflowManager mgr = getManager();
        executeAllAndWait();
        checkState(mgr, EXECUTED);

        mgr.resetAndConfigureAll();
        // there is only one in the wfm
        WorkflowAnnotation annotation = mgr.getWorkflowAnnotations().stream().findFirst().get();
        final NodeID[] nodes = new NodeID[] {m_columnFilter_2, m_columnFilter_3, m_columnSplitter_4};
        CollapseIntoMetaNodeResult collapseResult = mgr.collapseIntoMetaNode(nodes,
            new WorkflowAnnotation[] {annotation}, "Test-Meta/Wrap Node");
        WorkflowManager metaNode = mgr.getNodeContainer(
            collapseResult.getCollapsedMetanodeID(), WorkflowManager.class, true);
        NodeID metaSubID = metaNode.getID();
        assertFalse("Should have removed node: " + m_columnFilter_2, mgr.containsNodeContainer(m_columnFilter_2));
        assertTrue("No annotation expected", mgr.getWorkflowAnnotations().isEmpty());
        mgr.getNodeContainer(metaSubID, WorkflowManager.class, true);

        executeAllAndWait();
        checkState(mgr, EXECUTED);
        mgr.resetAndConfigureNode(metaSubID);

        MetaNodeToSubNodeResult convertObject = mgr.convertMetaNodeToSubNode(metaSubID);
        mgr.getNodeContainer(metaSubID, SubNodeContainer.class, true);
        executeAllAndWait();
        checkState(mgr, EXECUTED);
        mgr.resetAndConfigureNode(metaSubID);


        assertTrue("Should be undo-able", convertObject.canUndo());
        convertObject.undo();
        mgr.getNodeContainer(metaSubID, WorkflowManager.class, true);
        executeAllAndWait();
        checkState(mgr, EXECUTED);
        mgr.resetAndConfigureNode(metaSubID);

        convertObject = mgr.convertMetaNodeToSubNode(metaSubID);
        mgr.addConnection(m_javaEdit_7, 1, metaSubID, 0); // no flow var ports at '0' for meta nodes
        mgr.addConnection(metaSubID, 0, m_tableView_6, 0); // no flow var ports at '0' for meta nodes

        assertEquals("wrong number inputs", 3, mgr.getIncomingConnectionsFor(metaSubID).size());
        assertEquals("wrong number outputs", 3, mgr.getOutgoingConnectionsFor(metaSubID).size());

        executeAllAndWait();
        checkState(mgr, EXECUTED);
        mgr.resetAndConfigureNode(metaSubID);

        SubNodeToMetaNodeResult convertSubNodeToMetaNodeResult = mgr.convertSubNodeToMetaNode(metaSubID);
        mgr.getNodeContainer(metaSubID, WorkflowManager.class, true);
        assertEquals("wrong number inputs", 2, mgr.getIncomingConnectionsFor(metaSubID).size());
        assertEquals("wrong number outputs", 2, mgr.getOutgoingConnectionsFor(metaSubID).size());
        executeAllAndWait();
        checkState(mgr, EXECUTED);

        convertSubNodeToMetaNodeResult.undo();
        mgr.getNodeContainer(metaSubID, SubNodeContainer.class, true);
        executeAllAndWait();
        checkState(mgr, EXECUTED);

    }

}
