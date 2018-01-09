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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.knime.core.data.DataRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;

/**
 * Simple workflow with subnode containing QF nodes. Subnode dialog driven by variables. Also tests
 * loops with subnode, which uses loop variables as config.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class TestSubnode_CopyPasteWithDialogValues extends WorkflowTestCase {

    private NodeID m_subNode8;
    private NodeID m_javaEdit_test1_11;
    private NodeID m_javaEdit_nomail_12;
    private NodeID m_tableView13;
    private NodeID m_loopStartFail15;
    private NodeID m_loopEndFail25;
    private NodeID m_loopStartSuccess17;
    private NodeID m_loopEndSuccess16;
    private NodeID m_diffChecker20;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_subNode8 = new NodeID(baseID, 8);
        m_javaEdit_test1_11 = new NodeID(baseID, 11);
        m_javaEdit_nomail_12 = new NodeID(baseID, 12);
        m_tableView13 = new NodeID(baseID, 13);
        m_loopStartFail15 = new NodeID(baseID, 15);
        m_loopEndFail25 = new NodeID(baseID, 25);
        m_loopStartSuccess17 = new NodeID(baseID, 17);
        m_loopEndSuccess16 = new NodeID(baseID, 16);
        m_diffChecker20 = new NodeID(baseID, 20);
    }

    private void checkTableContent(final String email, final int integer) throws Exception {
        checkState(m_tableView13, InternalNodeContainerState.EXECUTED);
        final NativeNodeContainer nnc = (NativeNodeContainer)getManager().getNodeContainer(m_tableView13);
        BufferedDataTable table = ((BufferedDataTableHolder)nnc.getNodeModel()).getInternalTables()[0];
        assertNotNull(table);
        assertEquals("unexpected row count", 1, table.getRowCount());
        DataRow r = table.iterator().next();
        assertEquals(r.getCell(0), new StringCell(email));
        assertEquals(r.getCell(1), new IntCell(integer));
    }


    /** Just loading and making sure all is green with expected output.
     * @throws Exception ... */
    @Test
    public void testLoadExecutedFlow() throws Exception {
        checkState(m_tableView13, InternalNodeContainerState.EXECUTED);
        checkTableContent("test2@knime.com", 4);

        getManager().resetAndConfigureAll();
        executeAllAndWait();
        checkTableContent("test2@knime.com", 4);
    }

    /** Load workflow and reconnect it to another input that has different, valid data.
     * @throws Exception ... */
    @Test
    public void testConnectToValidInput() throws Exception {
        deleteConnection(m_subNode8, 0);
        checkState(m_subNode8, InternalNodeContainerState.IDLE);
        SubNodeContainer subNC = getManager().getNodeContainer(m_subNode8, SubNodeContainer.class, true);
        assertEquals(NodeMessage.Type.WARNING, subNC.getNodeMessage().getMessageType());
        assertFalse(getManager().canExecuteNode(m_subNode8));

        getManager().addConnection(m_javaEdit_test1_11, 1, m_subNode8, 0);
        assertEquals(NodeMessage.Type.RESET, subNC.getNodeMessage().getMessageType());
        assertTrue(getManager().canExecuteNode(m_subNode8));

        executeAllAndWait();
        checkState(m_subNode8, InternalNodeContainerState.EXECUTED);
        checkTableContent("test1@knime.com", 4);
    }

    /** Load workflow and reconnect it to another input that has different, valid data.
     * @throws Exception ... */
    @Test
    public void testConnectToInvalidInput() throws Exception {
        deleteConnection(m_subNode8, 0);

        getManager().addConnection(m_javaEdit_nomail_12, 1, m_subNode8, 0);
        // TODO better settings validation
//        assertFalse(getManager().canExecuteNode(m_subNode8)); // no valid email address

        executeAllAndWait();
        checkState(m_subNode8, InternalNodeContainerState.IDLE);
    }

    /** Load workflow and delete input connection, exec all. Expected to fail.
     * @throws Exception ... */
    @Test
    public void testNotConnected() throws Exception {
        deleteConnection(m_subNode8, 0);

        // TODO better settings validation
//        assertFalse(getManager().canExecuteNode(m_subNode8)); // no valid email address

        executeAllAndWait();
        checkState(m_subNode8, InternalNodeContainerState.IDLE);
    }


    /** Reconnect to second loop, which is expected to complete with no problem - all data valid.
     * @throws Exception ... */
    @Test
    public void testConnectInLoop_ValidPattern() throws Exception {
        deleteConnection(m_subNode8, 0);
        getManager().addConnection(m_loopStartSuccess17, 1, m_subNode8, 0);
        getManager().addConnection(m_subNode8, 1, m_loopEndSuccess16, 1);
        checkState(m_subNode8, InternalNodeContainerState.IDLE); // variables not populated!
        executeAndWait(m_loopStartSuccess17);
        checkState(m_subNode8, InternalNodeContainerState.CONFIGURED); // variables populated!
        checkState(m_loopEndSuccess16, InternalNodeContainerState.CONFIGURED);
        executeAndWait(m_diffChecker20);

        checkState(m_subNode8, InternalNodeContainerState.EXECUTED);
        checkState(m_loopEndSuccess16, InternalNodeContainerState.EXECUTED);
        checkState(m_diffChecker20, InternalNodeContainerState.EXECUTED);
    }

    /** Reconnect to first loop, which is expected to fail due to invalid email pattern.
     * @throws Exception ... */
    @Test
    public void testConnectInLoop_InvalidPattern() throws Exception {
        deleteConnection(m_subNode8, 0);
        getManager().addConnection(m_loopStartFail15, 1, m_subNode8, 0);
        getManager().addConnection(m_subNode8, 1, m_loopEndFail25, 1);
        checkState(m_subNode8, InternalNodeContainerState.IDLE); // variables not populated!
        executeAndWait(m_loopStartFail15);
        checkState(m_subNode8, InternalNodeContainerState.CONFIGURED); // variables populated!
        checkState(m_loopEndFail25, InternalNodeContainerState.CONFIGURED);
        executeAndWait(m_loopEndFail25);

        // TODO better settings validation
        checkState(m_subNode8, InternalNodeContainerState.IDLE); // failed due to invalid email
    }

}
