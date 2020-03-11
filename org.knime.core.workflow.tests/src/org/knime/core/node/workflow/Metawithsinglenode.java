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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class Metawithsinglenode extends WorkflowTestCase {

    private NodeID m_dataGen;
    private NodeID m_colFilterInMeta;
    private NodeID m_meta;
    private NodeID m_tblView;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_dataGen = new NodeID(baseID, 1);
        m_meta = new NodeID(baseID, 2);
        m_colFilterInMeta = new NodeID(m_meta, 1);
        m_tblView = new NodeID(baseID, 3);
    }

    @Test
    public void testExecuteOneByOne() throws Exception {
        checkState(m_dataGen, InternalNodeContainerState.CONFIGURED);
        checkState(m_colFilterInMeta, InternalNodeContainerState.CONFIGURED);
        checkMetaOutState(m_meta, 0, InternalNodeContainerState.CONFIGURED);
        checkState(m_tblView, InternalNodeContainerState.CONFIGURED);

        executeAndWait(m_dataGen);
        checkState(m_dataGen, InternalNodeContainerState.EXECUTED);
        checkState(m_colFilterInMeta, InternalNodeContainerState.CONFIGURED);
        checkMetaOutState(m_meta, 0, InternalNodeContainerState.CONFIGURED);
        checkState(m_tblView, InternalNodeContainerState.CONFIGURED);

        executeAndWait(m_colFilterInMeta);
        checkState(m_dataGen, InternalNodeContainerState.EXECUTED);
        checkState(m_colFilterInMeta, InternalNodeContainerState.EXECUTED);
        checkMetaOutState(m_meta, 0, InternalNodeContainerState.EXECUTED);
        checkState(m_tblView, InternalNodeContainerState.CONFIGURED);

        executeAndWait(m_tblView);
        checkState(m_dataGen, InternalNodeContainerState.EXECUTED);
        checkState(m_colFilterInMeta, InternalNodeContainerState.EXECUTED);
        checkMetaOutState(m_meta, 0, InternalNodeContainerState.EXECUTED);
        checkState(m_tblView, InternalNodeContainerState.EXECUTED);

        // state may not have propagated to workflow
        waitWhileInExecution();
        checkState(m_meta, InternalNodeContainerState.EXECUTED);

    }

    @Test
    public void testExecuteLast() throws Exception {
        executeAndWait(m_tblView);
        checkState(m_dataGen, InternalNodeContainerState.EXECUTED);
        checkState(m_colFilterInMeta, InternalNodeContainerState.EXECUTED);
        checkMetaOutState(m_meta, 0, InternalNodeContainerState.EXECUTED);
        checkState(m_tblView, InternalNodeContainerState.EXECUTED);
    }

    @Test
    public void testExecuteInMeta() throws Exception {
        executeAndWait(m_colFilterInMeta);
        checkState(m_dataGen, InternalNodeContainerState.EXECUTED);
        checkState(m_colFilterInMeta, InternalNodeContainerState.EXECUTED);
        checkMetaOutState(m_meta, 0, InternalNodeContainerState.EXECUTED);
        checkState(m_tblView, InternalNodeContainerState.CONFIGURED);
    }

    @Test
    public void testExecuteAll() throws Exception {
        getManager().executeAllAndWaitUntilDone();
        checkState(m_dataGen, InternalNodeContainerState.EXECUTED);
        checkState(m_colFilterInMeta, InternalNodeContainerState.EXECUTED);
        checkMetaOutState(m_meta, 0, InternalNodeContainerState.EXECUTED);
        checkState(m_tblView, InternalNodeContainerState.EXECUTED);
    }

    @Test
    public void testRandomExecuteAndReset() throws Exception {
        executeAndWait(m_colFilterInMeta);
        checkState(m_dataGen, InternalNodeContainerState.EXECUTED);
        checkState(m_colFilterInMeta, InternalNodeContainerState.EXECUTED);
        checkMetaOutState(m_meta, 0, InternalNodeContainerState.EXECUTED);
        checkState(m_tblView, InternalNodeContainerState.CONFIGURED);

        assertTrue(getManager().canResetNode(m_meta));
        getManager().resetAndConfigureNode(m_meta);
        checkState(m_dataGen, InternalNodeContainerState.EXECUTED);
        checkState(m_colFilterInMeta, InternalNodeContainerState.CONFIGURED);
        checkMetaOutState(m_meta, 0, InternalNodeContainerState.CONFIGURED);
        checkState(m_tblView, InternalNodeContainerState.CONFIGURED);

        executeAndWait(m_tblView);
        checkState(m_tblView, InternalNodeContainerState.EXECUTED);

        for (int i = 0; i < 10; i++) {
            getManager().resetAndConfigureNode(m_dataGen);
            checkState(m_dataGen, InternalNodeContainerState.CONFIGURED);
            checkState(m_colFilterInMeta, InternalNodeContainerState.CONFIGURED);
            checkMetaOutState(m_meta, 0, InternalNodeContainerState.CONFIGURED);
            checkState(m_tblView, InternalNodeContainerState.CONFIGURED);

            executeAndWait(m_tblView);
            checkState(m_dataGen, InternalNodeContainerState.EXECUTED);
            checkState(m_colFilterInMeta, InternalNodeContainerState.EXECUTED);
            checkMetaOutState(m_meta, 0, InternalNodeContainerState.EXECUTED);
            checkState(m_tblView, InternalNodeContainerState.EXECUTED);
        }
    }

    @Test
    public void testDeleteOuterConnectionTryExecuteInsertAgain()
        throws Exception {
        WorkflowManager m = getManager();
        ConnectionContainer c = findInConnection(m_meta, 0);
        assertNotNull(c);

        assertTrue(m.canRemoveConnection(c));
        m.removeConnection(c);
        assertNull(findInConnection(m_meta, 0));

        checkState(m_colFilterInMeta, InternalNodeContainerState.IDLE);
        checkMetaOutState(m_meta, 0, InternalNodeContainerState.IDLE);
        assertFalse(findParent(
                m_colFilterInMeta).canExecuteNode(m_colFilterInMeta));
        assertFalse(m.canExecuteNode(m_tblView));
        assertFalse(m.canExecuteNode(m_meta));

        executeAndWait(m_colFilterInMeta);
        checkState(m_meta, InternalNodeContainerState.IDLE);

        m.addConnection(c.getSource(), c.getSourcePort(),
                c.getDest(), c.getDestPort());
        checkState(m_colFilterInMeta, InternalNodeContainerState.CONFIGURED);
        checkMetaOutState(m_meta, 0, InternalNodeContainerState.CONFIGURED);
        checkState(m_tblView, InternalNodeContainerState.CONFIGURED);

        executeAndWait(m_tblView);
        checkState(m_tblView, InternalNodeContainerState.EXECUTED);
        checkMetaOutState(m_meta, 0, InternalNodeContainerState.EXECUTED);
    }

    @Test
    public void testDeleteInnerConnectionTryExecuteInsertAgain() throws Exception {
        WorkflowManager m = getManager();
        WorkflowManager meta = findParent(m_colFilterInMeta);
        ConnectionContainer c = findInConnection(m_colFilterInMeta, 1);
        assertNotNull(c);

        assertTrue(meta.canRemoveConnection(c));
        meta.removeConnection(c);
        assertNull(findInConnection(m_colFilterInMeta, 1));

        checkState(m_colFilterInMeta, InternalNodeContainerState.IDLE);
        checkMetaOutState(m_meta, 0, InternalNodeContainerState.IDLE);
        assertFalse(m.canExecuteNode(m_colFilterInMeta));
         assertFalse(m.canExecuteNode(m_tblView));
         // temporarily disabled, see
         // 4776: Metanodes with only idle (unconnected) nodes can still be executed if an upstream node is executable
         // http://bimbug.inf.uni-konstanz.de/show_bug.cgi?id=4776
//        assertFalse(m.canExecuteNode(m_meta));

        executeAndWait(m_colFilterInMeta);
        checkState(m_meta, InternalNodeContainerState.IDLE);

        meta.addConnection(c.getSource(), c.getSourcePort(),
                c.getDest(), c.getDestPort());
        checkState(m_colFilterInMeta, InternalNodeContainerState.CONFIGURED);
        checkMetaOutState(m_meta, 0, InternalNodeContainerState.CONFIGURED);
        checkState(m_tblView, InternalNodeContainerState.CONFIGURED);

        executeAndWait(m_tblView);
        checkState(m_tblView, InternalNodeContainerState.EXECUTED);
        checkMetaOutState(m_meta, 0, InternalNodeContainerState.EXECUTED);
    }

    @Test
    public void testExecuteDeleteOuterConnection() throws Exception {
        WorkflowManager m = getManager();
        WorkflowManager meta = findParent(m_colFilterInMeta);
        executeAndWait(m_tblView);
        ConnectionContainer connection = findInConnection(m_meta, 0);
        assertTrue(m.canRemoveConnection(connection));
        m.removeConnection(connection);

        checkState(m_colFilterInMeta, InternalNodeContainerState.IDLE);
        checkMetaOutState(m_meta, 0, InternalNodeContainerState.IDLE);
        assertFalse(meta.canExecuteNode(m_colFilterInMeta));
        assertFalse(m.canExecuteNode(m_tblView));
    }

    @Test
    public void testExecuteDeleteInnerConnection() throws Exception {
        WorkflowManager m = getManager();
        WorkflowManager meta = findParent(m_colFilterInMeta);
        executeAndWait(m_tblView);
        ConnectionContainer connection = findInConnection(m_colFilterInMeta, 1);
        assertTrue(meta.canRemoveConnection(connection));
        meta.removeConnection(connection);

        checkState(m_colFilterInMeta, InternalNodeContainerState.IDLE);
        checkMetaOutState(m_meta, 0, InternalNodeContainerState.IDLE);
        assertFalse(meta.canExecuteNode(m_colFilterInMeta));
        assertFalse(m.canExecuteNode(m_tblView));
    }

    @Test
    public void testExecuteDeleteNode() throws Exception {
        WorkflowManager m = getManager();
        WorkflowManager meta = findParent(m_colFilterInMeta);
        executeAndWait(m_tblView);
        assertTrue(meta.canRemoveNode(m_colFilterInMeta));
        meta.removeNode(m_colFilterInMeta);

        checkState(m_tblView, InternalNodeContainerState.IDLE);
        assertFalse(m.canExecuteNode(m_tblView));
    }
}
