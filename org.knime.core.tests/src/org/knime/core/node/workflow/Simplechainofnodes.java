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
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class Simplechainofnodes extends WorkflowTestCase {

    private NodeID m_dataGen;
    private NodeID m_colFilter;
    private NodeID m_rowFilter;
    private NodeID m_tblView;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_dataGen = new NodeID(baseID, 1);
        m_colFilter = new NodeID(baseID, 2);
        m_rowFilter = new NodeID(baseID, 3);
        m_tblView = new NodeID(baseID, 4);
    }

    @Test
    public void testExecuteOneByOne() throws Exception {
        checkState(m_dataGen, InternalNodeContainerState.CONFIGURED);
        checkState(m_colFilter, InternalNodeContainerState.CONFIGURED);
        checkState(m_rowFilter, InternalNodeContainerState.CONFIGURED);
        checkState(m_tblView, InternalNodeContainerState.CONFIGURED);

        executeAndWait(m_dataGen);
        checkState(m_dataGen, InternalNodeContainerState.EXECUTED);
        checkState(m_colFilter, InternalNodeContainerState.CONFIGURED);
        checkState(m_rowFilter, InternalNodeContainerState.CONFIGURED);
        checkState(m_tblView, InternalNodeContainerState.CONFIGURED);

        executeAndWait(m_colFilter);
        checkState(m_dataGen, InternalNodeContainerState.EXECUTED);
        checkState(m_colFilter, InternalNodeContainerState.EXECUTED);
        checkState(m_rowFilter, InternalNodeContainerState.CONFIGURED);
        checkState(m_tblView, InternalNodeContainerState.CONFIGURED);

        executeAndWait(m_rowFilter);
        checkState(m_dataGen, InternalNodeContainerState.EXECUTED);
        checkState(m_colFilter, InternalNodeContainerState.EXECUTED);
        checkState(m_rowFilter, InternalNodeContainerState.EXECUTED);
        checkState(m_tblView, InternalNodeContainerState.CONFIGURED);

        executeAndWait(m_tblView);
        checkState(m_dataGen, InternalNodeContainerState.EXECUTED);
        checkState(m_colFilter, InternalNodeContainerState.EXECUTED);
        checkState(m_rowFilter, InternalNodeContainerState.EXECUTED);
        checkState(m_tblView, InternalNodeContainerState.EXECUTED);

    }

    @Test
    public void testExecuteLast() throws Exception {
        executeAndWait(m_tblView);
        checkState(m_dataGen, InternalNodeContainerState.EXECUTED);
        checkState(m_colFilter, InternalNodeContainerState.EXECUTED);
        checkState(m_rowFilter, InternalNodeContainerState.EXECUTED);
        checkState(m_tblView, InternalNodeContainerState.EXECUTED);
    }

    @Test
    public void testExecuteAll() throws Exception {
        getManager().executeAllAndWaitUntilDone();
        checkState(getManager(), InternalNodeContainerState.EXECUTED);
        checkState(m_dataGen, InternalNodeContainerState.EXECUTED);
        checkState(m_colFilter, InternalNodeContainerState.EXECUTED);
        checkState(m_rowFilter, InternalNodeContainerState.EXECUTED);
        checkState(m_tblView, InternalNodeContainerState.EXECUTED);
    }

    @Test
    public void testRandomExecuteAndReset() throws Exception {
        executeAndWait(m_rowFilter);
        checkState(m_dataGen, InternalNodeContainerState.EXECUTED);
        checkState(m_colFilter, InternalNodeContainerState.EXECUTED);
        checkState(m_rowFilter, InternalNodeContainerState.EXECUTED);
        checkState(m_tblView, InternalNodeContainerState.CONFIGURED);

        assertTrue(getManager().canResetNode(m_colFilter));
        getManager().resetAndConfigureNode(m_colFilter);
        checkState(m_dataGen, InternalNodeContainerState.EXECUTED);
        checkState(m_colFilter, InternalNodeContainerState.CONFIGURED);
        checkState(m_rowFilter, InternalNodeContainerState.CONFIGURED);
        checkState(m_tblView, InternalNodeContainerState.CONFIGURED);

        executeAndWait(m_tblView);
        checkState(m_tblView, InternalNodeContainerState.EXECUTED);

        for (int i = 0; i < 10; i++) {
            getManager().resetAndConfigureNode(m_dataGen);
            checkState(m_dataGen, InternalNodeContainerState.CONFIGURED);
            checkState(m_colFilter, InternalNodeContainerState.CONFIGURED);
            checkState(m_rowFilter, InternalNodeContainerState.CONFIGURED);
            checkState(m_tblView, InternalNodeContainerState.CONFIGURED);
            executeAndWait(m_tblView);
            checkState(m_dataGen, InternalNodeContainerState.EXECUTED);
            checkState(m_colFilter, InternalNodeContainerState.EXECUTED);
            checkState(m_rowFilter, InternalNodeContainerState.EXECUTED);
            checkState(m_tblView, InternalNodeContainerState.EXECUTED);
        }
    }

    @Test
    public void testDeleteConnectionTryExecuteInsertAgain() throws Exception {
        WorkflowManager m = getManager();
        ConnectionContainer connection = findInConnection(m_rowFilter, 1);
        assertNotNull(connection);
        // although the connection exists, we can replace it. This is heavily
        // used when old connections are overwritten.
        assertTrue(m.canAddConnection(m_colFilter, 1, m_rowFilter, 1));
        assertTrue(m.canRemoveConnection(connection));
        m.removeConnection(connection);

        checkState(m_rowFilter, InternalNodeContainerState.IDLE);
        assertFalse(m.canExecuteNode(m_rowFilter));
        assertFalse(m.canExecuteNode(m_tblView));
        assertTrue(m.canExecuteNode(m_colFilter));

        executeAndWait(m_colFilter);
        checkState(m_colFilter, InternalNodeContainerState.EXECUTED);

        executeAndWait(m_rowFilter);
        checkState(m_rowFilter, InternalNodeContainerState.IDLE);

        m.addConnection(m_colFilter, 1, m_rowFilter, 1);
        checkState(m_rowFilter, InternalNodeContainerState.CONFIGURED);
        checkState(m_tblView, InternalNodeContainerState.CONFIGURED);

        executeAndWait(m_tblView);
        checkState(m_tblView, InternalNodeContainerState.EXECUTED);
    }

    @Test
    public void testExecuteDeleteConnection() throws Exception {
        WorkflowManager m = getManager();
        executeAndWait(m_tblView);
        ConnectionContainer connection = findInConnection(m_rowFilter, 1);
        assertTrue(m.canRemoveConnection(connection));
        m.removeConnection(connection);

        checkState(m_rowFilter, InternalNodeContainerState.IDLE);
        assertFalse(m.canExecuteNode(m_rowFilter));
        assertFalse(m.canExecuteNode(m_tblView));
    }

    @Test
    public void testDeleteNodeExecute() throws Exception {
        WorkflowManager m = getManager();
        assertTrue(m.canRemoveNode(m_rowFilter));
        m.removeNode(m_rowFilter);

        checkState(m_tblView, InternalNodeContainerState.IDLE);
        assertFalse(m.canExecuteNode(m_tblView));
    }

    @Test
    public void testExecuteDeleteNode() throws Exception {
        WorkflowManager m = getManager();
        executeAndWait(m_tblView);
        assertTrue(m.canRemoveNode(m_rowFilter));
        m.removeNode(m_rowFilter);

        checkState(m_tblView, InternalNodeContainerState.IDLE);
        assertFalse(m.canExecuteNode(m_tblView));
    }

}
