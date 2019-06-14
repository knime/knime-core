/*
 * ------------------------------------------------------------------------
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

import static org.hamcrest.CoreMatchers.is;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTING;
import static org.knime.core.node.workflow.InternalNodeContainerState.IDLE;

import java.util.concurrent.locks.ReentrantLock;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.knime.testing.node.blocking.BlockingRepository;

/** A simple test using a subnode where the subnode is executing (blocked) and then canceled. The bug would then
 * potentially result in a connected downstream node to remain in a weird (executing state) as the post-execute action
 * of the subnode would fail with the error reported in the ticket (https://knime-com.atlassian.net/browse/AP-11710).
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class BugAP11710_Subnode_Cancel extends WorkflowTestCase {

    private static final String LOCK_ID = "AP-11710";
    private NodeID m_dataGenerator_1;
    private NodeID m_subnode_4;
    private NodeID m_outputInSubnode_4_4;
    private NodeID m_varToTableRow_3;
    private NodeID m_blocking_4_5;

    @Before
    public void setUp() throws Exception {
        // the id is used here and in the workflow (part of the settings)
        BlockingRepository.put(LOCK_ID, new ReentrantLock());
        NodeID baseID = loadAndSetWorkflow();
        m_dataGenerator_1 = baseID.createChild(1);
        m_subnode_4 = baseID.createChild(4);
        m_outputInSubnode_4_4 = m_subnode_4.createChild(0).createChild(4);
        m_blocking_4_5 = m_subnode_4.createChild(0).createChild(5);
        m_varToTableRow_3 = baseID.createChild(3);
    }

    /** Execute workflow, don't let subnode finish. Then cancel it and check state of downstream node. */
    // note that bug was not 100% reproducible (only 30-50 %)
    @Test
    public void testMain() throws Exception {
        WorkflowManager m = getManager();
        checkState(m, IDLE);
        executeAndWait(m_dataGenerator_1);
        checkState(m_dataGenerator_1, EXECUTED);
        checkState(m_subnode_4, IDLE);
        SubNodeContainer subnode = m.getNodeContainer(m_subnode_4, SubNodeContainer.class, true);
        ReentrantLock execLock = BlockingRepository.get(LOCK_ID);
        execLock.lock();
        try {
            m.executeUpToHere(m_varToTableRow_3);
            waitWhileNodeInExecution(findNodeContainer(m_outputInSubnode_4_4));
            NodeContainer blockingNode = findNodeContainer(m_blocking_4_5);
            waitWhile(blockingNode, new Hold() {
                @Override
                protected boolean shouldHold() {
                    return !blockingNode.getInternalState().equals(EXECUTING);
                }
            });
            InternalNodeContainerState subnodeState = subnode.getInternalState();
            Assert.assertThat("subnode in some executing state: " + subnodeState,
                subnodeState.isExecutionInProgress(), is(true));
            checkState(m_varToTableRow_3, InternalNodeContainerState.UNCONFIGURED_MARKEDFOREXEC);
            m.cancelExecution(subnode);
            Assert.assertThat("subnode in some executing state: " + subnodeState,
                subnodeState.isExecutionInProgress(), is(true));
        } finally {
            execLock.unlock();
        }
        waitWhileNodeInExecution(subnode);
        checkState(m_subnode_4, EXECUTED, CONFIGURED); // may actually complete and 'cancel' was reacted on too late
        checkState(m_varToTableRow_3, IDLE, CONFIGURED);
        checkState(m, CONFIGURED);
    }

    /** {@inheritDoc} */
    @Override
    @After
    public void tearDown() throws Exception {
        BlockingRepository.remove(LOCK_ID);
        super.tearDown();
    }

}
