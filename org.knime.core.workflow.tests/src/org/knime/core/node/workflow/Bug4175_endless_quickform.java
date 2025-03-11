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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.quickform.in.QuickFormInputNode;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class Bug4175_endless_quickform extends WorkflowTestCase {

    private NodeID m_quickFormBoolean;
    private NodeID m_tableViewEnd;

    @BeforeEach
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_quickFormBoolean = new NodeID(baseID, 1);
        m_tableViewEnd = new NodeID(baseID, 14);
    }

    @Test
    public void testExecuteFlow() throws Exception {
        checkState(m_quickFormBoolean, InternalNodeContainerState.CONFIGURED);
        checkState(m_tableViewEnd, InternalNodeContainerState.IDLE);
        executeAndWait(m_tableViewEnd);
        checkState(m_tableViewEnd, InternalNodeContainerState.EXECUTED);
    }

    @Test
    public void testStepExecute() throws Exception {
        WorkflowManager m = getManager();
        m.stepExecutionUpToNodeType(QuickFormInputNode.class, QuickFormInputNode.NOT_HIDDEN_FILTER);
        m.waitWhileInExecution(5, TimeUnit.SECONDS);
        WorkflowManager waitingWFM = m.findNextWaitingWorkflowManager(
                QuickFormInputNode.class, QuickFormInputNode.NOT_HIDDEN_FILTER);
        assertSame(m, waitingWFM);
        Map<NodeID, QuickFormInputNode> waitingNodes =
                waitingWFM.findWaitingNodes(QuickFormInputNode.class, QuickFormInputNode.NOT_HIDDEN_FILTER);
        assertEquals(waitingNodes.size(), 1);
        QuickFormInputNode booleanIn = waitingNodes.get(m_quickFormBoolean);
        assertNotNull(booleanIn);
        waitingWFM.executeUpToHere(m_quickFormBoolean);
        m.waitWhileInExecution(5, TimeUnit.SECONDS);
        checkState(m_quickFormBoolean, InternalNodeContainerState.EXECUTED);
        checkState(m_tableViewEnd, InternalNodeContainerState.IDLE);
        m.stepExecutionUpToNodeType(QuickFormInputNode.class, QuickFormInputNode.NOT_HIDDEN_FILTER);
        m.waitWhileInExecution(5, TimeUnit.SECONDS);
        checkState(m_tableViewEnd, InternalNodeContainerState.EXECUTED);
    }

    @Test
    public void testStepExecuteAfterExecuteAll() throws Exception {
        long start = System.currentTimeMillis();
        executeAllAndWait();
        checkState(m_tableViewEnd, InternalNodeContainerState.EXECUTED);
        WorkflowManager m = getManager();
        m.stepExecutionUpToNodeType(QuickFormInputNode.class, QuickFormInputNode.NOT_HIDDEN_FILTER);
        checkState(m_tableViewEnd, InternalNodeContainerState.EXECUTED);
        long delay = System.currentTimeMillis() - start;
        if (delay > 2000L) {
            fail("took too long to execute: " + delay / 1000L + "s");
        }
    }

    /** {@inheritDoc} */
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

}