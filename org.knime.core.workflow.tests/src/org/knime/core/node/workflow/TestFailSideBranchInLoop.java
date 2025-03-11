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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class TestFailSideBranchInLoop extends WorkflowTestCase {

    private NodeID m_dataGen2;
    private NodeID m_loopStart3;
    private NodeID m_csvWriterInLoop13;
    private NodeID m_loopEnd4;

    @BeforeEach
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_dataGen2 = new NodeID(baseID, 2);
        m_loopStart3 = new NodeID(baseID, 3);
        m_csvWriterInLoop13 = new NodeID(baseID, 13);
        m_loopEnd4 = new NodeID(baseID, 4);
    }

    @Test
    @Disabled("Disabled - outstanding hidden problems")
    public void bug3292testExecuteFlowWithUnconfiguredCSVWriter() throws Exception {
        checkState(m_dataGen2, InternalNodeContainerState.CONFIGURED);
        checkState(m_loopEnd4, InternalNodeContainerState.CONFIGURED);
        checkState(m_csvWriterInLoop13, InternalNodeContainerState.IDLE);
        getManager().executeUpToHere(m_loopEnd4);
        waitWhile(m_loopEnd4, nc -> nc.getNodeContainerState().isExecutionInProgress(), 2);
        checkState(m_loopEnd4, InternalNodeContainerState.CONFIGURED);
        checkState(m_loopStart3, InternalNodeContainerState.EXECUTED);
    }

    @Test
    public void testExecuteFlowNoCSVWriter() throws Exception {
        checkState(m_dataGen2, InternalNodeContainerState.CONFIGURED);
        checkState(m_loopEnd4, InternalNodeContainerState.CONFIGURED);
        deleteConnection(m_csvWriterInLoop13, 1);
        checkState(m_csvWriterInLoop13, InternalNodeContainerState.IDLE);
        executeAllAndWait();
        checkState(m_loopEnd4, InternalNodeContainerState.EXECUTED);
        checkState(m_loopStart3, InternalNodeContainerState.EXECUTED);
    }

}