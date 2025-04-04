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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Problems with nested scope nodes and exceptions while dragging connection.
 * 6374: nested scopes of different type with wrong wiring can result in NPE
 * https://bugs.knime.org/show_bug.cgi?id=6374
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class Bug6374_NPE_when_connecting_outside_Scope extends WorkflowTestCase {

    private NodeID m_tableColToVar_8;
    private NodeID m_varToTableCol_7;
    private NodeID m_loopEnd_10;
    private NodeID m_catchErrors_5;


    @BeforeEach
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_varToTableCol_7 = new NodeID(baseID, 7);
        m_tableColToVar_8 = new NodeID(baseID, 8);
        m_loopEnd_10 = new NodeID(baseID, 10);
        m_catchErrors_5 = new NodeID(baseID, 5);
    }

    /** Add connection, all nodes reset. */
    @Test
    public void testAddConnection_ThenExecute() throws Exception {
        WorkflowManager manager = getManager();
        checkState(manager, CONFIGURED);
        manager.addConnection(m_tableColToVar_8, 1, m_varToTableCol_7, 1);
        assertNotNull(findInConnection(m_varToTableCol_7, 1));

        executeAllAndWait();
        checkState(m_loopEnd_10, CONFIGURED);
        NodeMessage msg = manager.getNodeContainer(m_loopEnd_10).getNodeMessage();
        assertNotNull(msg);
        assertEquals(NodeMessage.Type.ERROR, msg.getMessageType(),  "Not an error message on loop end");
    }

    /** Partially execute, then add connection. This used to cause errors. */
    @Test
    public void testPartialExecute_ThenAddConnection() throws Exception {
        WorkflowManager manager = getManager();
        executeAndWait(m_catchErrors_5, m_tableColToVar_8);
        checkStateOfMany(EXECUTED, m_catchErrors_5, m_tableColToVar_8);

        manager.addConnection(m_tableColToVar_8, 1, m_varToTableCol_7, 1);
        assertNotNull(findInConnection(m_varToTableCol_7, 1));

        executeAllAndWait();
        checkState(m_loopEnd_10, CONFIGURED);
        NodeMessage msg = manager.getNodeContainer(m_loopEnd_10).getNodeMessage();
        assertNotNull(msg);
        assertEquals(NodeMessage.Type.ERROR, msg.getMessageType(),  "Not an error message on loop end");
    }

}