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

import org.junit.Before;
import org.junit.Test;
import org.knime.core.data.util.memory.MemoryAlertSystem;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class Bug4385_FileStoresWithLowMemory extends WorkflowTestCase {

    private NodeID m_dataGenStart1;
    private NodeID m_testAfterLoop2;
    private NodeID m_testSingle8;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_dataGenStart1 = new NodeID(baseID, 1);
        m_testAfterLoop2 = new NodeID(baseID, 2);
        m_testSingle8 = new NodeID(baseID, 8);
    }

    @Test
    public void testExecuteFlow() throws Exception {
        checkState(InternalNodeContainerState.CONFIGURED);
        executeAllAndWait();
        for (NodeContainer nc : getManager().getNodeContainers()) {
            assertEquals("Node " + nc.getNameWithID() + ": " + nc.getNodeMessage(),
                NodeMessage.Type.RESET, nc.getNodeMessage().getMessageType());
        }
        MemoryAlertSystem.getInstance().sendMemoryAlert();
        checkState(InternalNodeContainerState.EXECUTED);
        reset(m_testAfterLoop2, m_testSingle8);
        executeAndWait(m_testAfterLoop2, m_testSingle8);
        checkState(InternalNodeContainerState.EXECUTED);
        // interesting part (where the error used to occur) happens in shutdown.
        // it threw an exception as documented in the bug report
    }

    private void checkState(final InternalNodeContainerState state) throws Exception {
        checkState(m_dataGenStart1, state);
        checkState(m_testAfterLoop2, state);
        checkState(m_testSingle8, state);
    }

}
