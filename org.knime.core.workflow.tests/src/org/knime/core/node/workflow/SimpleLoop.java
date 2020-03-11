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

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.knime.testing.node.executioncount.ExecutionCountNodeModel;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class SimpleLoop extends WorkflowTestCase {

    private NodeID m_loopStart;
    private NodeID m_loopEnd;
    private NodeID m_counterInLoop;
    private NodeID m_counterOutSourceLoop;
    private NodeID m_counterOutSinkLoop;
    private NodeID m_tblView;

    @Before
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_loopStart = new NodeID(baseID, 2);
        m_loopEnd = new NodeID(baseID, 3);
        m_counterInLoop = new NodeID(baseID, 4);
        m_counterOutSourceLoop = new NodeID(baseID, 8);
        m_counterOutSinkLoop = new NodeID(baseID, 9);
        m_tblView = new NodeID(baseID, 5);
    }

    @Test
    public void testExecuteFlow() throws Exception {
        checkState(m_loopStart, InternalNodeContainerState.CONFIGURED);
        checkState(m_tblView, InternalNodeContainerState.CONFIGURED);
        executeAndWait(m_loopEnd);
        waitWhileInExecution();
        checkState(m_loopEnd, InternalNodeContainerState.EXECUTED);
        Map<NodeID, ExecutionCountNodeModel> counterNodes =
            getManager().findNodes(ExecutionCountNodeModel.class, true);
        int inCount = counterNodes.get(m_counterInLoop).getCounter();

        checkState(m_counterInLoop, InternalNodeContainerState.EXECUTED);
        assertEquals("Expected 10 executions of node in loop", 10, inCount);

        int outCount = counterNodes.get(m_counterOutSourceLoop).getCounter();
        checkState(m_counterOutSourceLoop, InternalNodeContainerState.EXECUTED);
        assertEquals(
                "Expected one execution of source nodes in loop", 1, outCount);

        int outCountSink = counterNodes.get(m_counterOutSinkLoop).getCounter();
        checkState(m_counterOutSinkLoop, InternalNodeContainerState.EXECUTED);
        assertEquals("Expected 10 executions of sink nodes in loop",
                10, outCountSink);

        executeAndWait(m_tblView);
        checkState(m_tblView, InternalNodeContainerState.EXECUTED);

        getManager().resetAndConfigureAll();
        assertEquals(getNrTablesInGlobalRepository(), 0);
    }

}
