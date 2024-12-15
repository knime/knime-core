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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTING;

import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.shared.workflow.storage.clipboard.DefClipboardContent;
import org.knime.shared.workflow.storage.util.PasswordRedactor;
import org.knime.testing.node.blocking.BlockingRepository;
import org.knime.testing.node.blocking.BlockingRepository.LockedMethod;

/**
 * Tests manipulation of a component during and after execution.
 * 
 * @author Bernd Wiswedel
 */
public class BugAP2124_ComponentModificationWhileDownstreamExecution extends WorkflowTestCase {

	private static final String BLOCK_ID_OUTER = "AP-21243_Lock_Outer";
	private static final String BLOCK_ID_INNER = "AP-21243_Lock_Inner";

	private NodeID m_tableCreator_1;
	private NodeID m_tableToVar_8;
	private NodeID m_blockOuter_10;
	private NodeID m_componentOuter_11;
	private NodeID m_componentInner_11_0_9_7;
    private NodeID m_blockInner_11_0_9_7_0_5;
    private NodeID m_variableEdit_11_0_9_7_0_6;
	private ReentrantLock m_inComponentLock;
	private ReentrantLock m_outComponentLock;

    @BeforeEach
    public void setUp() throws Exception {
        NodeID baseID = loadAndSetWorkflow();
        m_tableCreator_1 = new NodeID(baseID, 1);
        m_tableToVar_8 = new NodeID(baseID, 8);
        m_blockOuter_10 = new NodeID(baseID, 10);
        m_componentOuter_11 = new NodeID(baseID, 11);
        m_componentInner_11_0_9_7 = m_componentOuter_11.createChild(0).createChild(9).createChild(7);
		m_blockInner_11_0_9_7_0_5 = m_componentInner_11_0_9_7.createChild(0).createChild(5);
		m_variableEdit_11_0_9_7_0_6 = m_componentInner_11_0_9_7.createChild(0).createChild(6);
		m_inComponentLock = new ReentrantLock();
		m_outComponentLock = new ReentrantLock();
		BlockingRepository.put(BLOCK_ID_INNER, LockedMethod.EXECUTE, m_inComponentLock);
		BlockingRepository.put(BLOCK_ID_OUTER, LockedMethod.EXECUTE, m_outComponentLock);
    }

    @Test
    public void testRegularExecute() throws Exception {
    	checkState(m_tableCreator_1, CONFIGURED);
    	executeAllAndWait();
    	checkState(m_tableToVar_8, EXECUTED);
    	checkState(getManager(), EXECUTED);
    }

    /** Keep inner component running, then try to manipulate the component while it's running (operations allowed). */
    @Test
    public void testModificationWhileComponentIsExecuting() throws Exception {
    	checkState(m_tableCreator_1, CONFIGURED);
    	m_inComponentLock.lock();
    	try {
    		executeDontWait(m_tableToVar_8, m_blockOuter_10);
    		waitWhile(m_blockInner_11_0_9_7_0_5, nc -> nc.getInternalState() != EXECUTING, 3);
    		waitWhileNodeInExecution(m_variableEdit_11_0_9_7_0_6);
    		checkState(m_blockInner_11_0_9_7_0_5, EXECUTING);
    		checkState(m_componentInner_11_0_9_7, EXECUTING);
    		final NodeContainer varEditNode = findNodeContainer(m_variableEdit_11_0_9_7_0_6);
			final WorkflowManager innerMostComponentWFM = varEditNode.getParent();
			// component is currently executing - it's allowed to drop new nodes and run them
			final NodeID copiedNode = innerMostComponentWFM.copyFromAndPasteHere(innerMostComponentWFM, 
					WorkflowCopyContent.builder().setNodeIDs(m_variableEdit_11_0_9_7_0_6).build()) //
				.getNodeIDs()[0];
			checkState(copiedNode, CONFIGURED);
			executeAndWait(copiedNode);

			// can also reset stuff inside the component (but don't because then it won't complete)
			assertThat("can reset inside of executing component", innerMostComponentWFM.canResetNode(copiedNode));
    	} finally {
    		m_inComponentLock.unlock();
    	}
    	waitWhileInExecution();
    	checkState(getManager(), EXECUTED);
    }

    /** Execute inner component, but keep downstream nodes running, then try to manipulate the component 
     * (operations not allowed). */
    @Test
    public void testModificationAfterComponentHasExecuted() throws Exception {
    	checkState(m_tableCreator_1, CONFIGURED);
    	m_outComponentLock.lock();
    	try {
    		executeDontWait(m_tableToVar_8, m_blockOuter_10);
    		waitWhile(m_blockOuter_10, nc -> nc.getInternalState() != EXECUTING, 3);
    		waitWhileNodeInExecution(m_tableToVar_8);
    		checkState(m_componentOuter_11, EXECUTED);

    		final NodeContainer varEditNode = findNodeContainer(m_variableEdit_11_0_9_7_0_6);
    		final WorkflowManager innerMostComponentWFM = varEditNode.getParent();

    		// component is currently executing - it's allowed to drop new nodes and run them
			final DefClipboardContent def = innerMostComponentWFM.copyToDef(
					WorkflowCopyContent.builder().setNodeIDs(m_variableEdit_11_0_9_7_0_6).build(),
					PasswordRedactor.asNull());

			assertThrows(IllegalStateException.class, () -> innerMostComponentWFM.paste(def),
					"Workflow manipuation not allowed");
			assertFalse(innerMostComponentWFM.canRemoveConnection(findInConnection(m_blockInner_11_0_9_7_0_5, 1)),
					"cannot remove connection");
			assertFalse(innerMostComponentWFM.canAddConnection(m_blockInner_11_0_9_7_0_5, 0,
					m_variableEdit_11_0_9_7_0_6, 0), "cannot add connection");
			assertThat("not resetable", !innerMostComponentWFM.canResetNode(m_variableEdit_11_0_9_7_0_6));
    	} finally {
    		m_outComponentLock.unlock();
    	}
    	waitWhileInExecution();
    	checkState(getManager(), EXECUTED);
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
    	BlockingRepository.removeAll(BLOCK_ID_INNER);
    	BlockingRepository.removeAll(BLOCK_ID_OUTER);
    	super.tearDown();
    }

}