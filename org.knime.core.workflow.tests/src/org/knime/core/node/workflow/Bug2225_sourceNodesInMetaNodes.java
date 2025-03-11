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

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.testing.node.executioncount.ExecutionCountNodeModel;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class Bug2225_sourceNodesInMetaNodes extends WorkflowTestCase {

    private NodeID m_loopStart;
    private NodeID m_loopEnd;
    private NodeID m_counterInLoop;
    private NodeID m_counterOutLoop;
    private NodeID m_tblView;

    @BeforeEach
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_loopStart = new NodeID(baseID, 2);
        m_loopEnd = new NodeID(baseID, 3);
        NodeID meta = new NodeID(baseID, 5);
        m_counterInLoop = new NodeID(meta, 2);
        m_counterOutLoop = new NodeID(meta, 4);
        m_tblView = new NodeID(baseID, 6);
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
        assert inCount == 1 : "Expected one execution of source node: " + inCount;
        int outCount = counterNodes.get(m_counterOutLoop).getCounter();

        checkState(m_counterOutLoop, InternalNodeContainerState.CONFIGURED);
        assert outCount == 0 : "Expected no execution of unconnected node: "
            + outCount;

        executeAndWait(m_tblView);
        checkState(m_tblView, InternalNodeContainerState.EXECUTED);

        outCount = counterNodes.get(m_counterOutLoop).getCounter();
        checkState(m_counterOutLoop, InternalNodeContainerState.EXECUTED);
        assert outCount == 1 : "Expected no execution of unconnected node: "
            + outCount;

    }

    /** {@inheritDoc} */
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

