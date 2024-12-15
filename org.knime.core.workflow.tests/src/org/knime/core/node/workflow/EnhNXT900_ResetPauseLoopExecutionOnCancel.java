/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.core.node.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Benjamin Moser, KNIME GmbH, Konstanz, Germany
 */
public class EnhNXT900_ResetPauseLoopExecutionOnCancel extends WorkflowTestCase {

	private WorkflowManager m_wfm;

	private NativeNodeContainer m_tail;

	private NativeNodeContainer m_wait;

	@BeforeEach
	public void beforeEach() throws Exception {
		NodeID baseID = loadAndSetWorkflow();
		var tailID = new NodeID(baseID, 2);
		var waitID = new NodeID(baseID, 3);
		m_wfm = getManager();
		m_tail = m_wfm.getNodeContainer(tailID, NativeNodeContainer.class, true);
		m_wait = m_wfm.getNodeContainer(waitID, NativeNodeContainer.class, true);
		m_wfm.resetAndConfigureAll();
		checkStateOfAll(InternalNodeContainerState.CONFIGURED);
	}

	/**
	 * Check that the pauseLoopExecution flag is cleared on node cancel
	 */
	@Test
	public void resetFlagOnCancel() throws Exception {
		triggerStepLoop(m_tail);
		assertTrue(m_tail.getNode().getPauseLoopExecution());
		m_wfm.cancelExecution(m_tail);
		waitWhileInExecution();
		checkState(m_tail.getID(), InternalNodeContainerState.CONFIGURED);
		assertFalse(m_tail.getNode().getPauseLoopExecution());
	}

	/**
	 * Check that loop is fully executed after tail is cancelled, then executed
	 * @throws Exception
	 */
	@Test
	public void loopExecutesFullyAfterCancelExecute() throws Exception {
		triggerStepLoop(m_tail);
		m_wfm.cancelExecution(m_tail);
		executeAndWait(m_tail.getID());
		checkStateOfAll(InternalNodeContainerState.EXECUTED);
		checkIterationIndex(m_wait, 9);
	}

	/**
	 * Based on StepLoopAction#runOnNodes
	 */
	private void triggerStepLoop(NativeNodeContainer tail) {
		if (tail.isModelCompatibleTo(LoopEndNode.class)
				&& tail.getLoopStatus().equals(NativeNodeContainer.LoopStatus.PAUSED)) {
			m_wfm.resumeLoopExecution(tail, /* oneStep= */true);
		} else if (m_wfm.canExecuteNodeDirectly(tail.getID())) {
			m_wfm.executeUpToHere(tail.getID());
			m_wfm.pauseLoopExecution(tail);
		}
	}

	private void checkIterationIndex(NodeContainer nc, int expectedIndex) {
		assertEquals("Unexpected iteration index", expectedIndex,
				nc.getFlowObjectStack().peek(FlowLoopContext.class).getIterationIndex());
	}

	private void checkStateOfAll(InternalNodeContainerState targetState) throws Exception {
		checkStateOfMany(targetState, m_wfm.getWorkflow().getNodeIDs().toArray(NodeID[]::new));
	}
}