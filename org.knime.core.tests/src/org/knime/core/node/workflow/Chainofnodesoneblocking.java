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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED_MARKEDFOREXEC;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED_QUEUED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTING;
import static org.knime.core.node.workflow.InternalNodeContainerState.PREEXECUTE;

import java.util.concurrent.locks.ReentrantLock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.testing.node.blocking.BlockingRepository;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class Chainofnodesoneblocking extends WorkflowTestCase {

    private static final String LOCK_ID = "chainofnodesoneblocking";
    private NodeID m_dataGen;
    private NodeID m_blocker;
    private NodeID m_colFilter;
    private NodeID m_tblView;

    @Before
    public void setUp() throws Exception {
        // the id is used here and in the workflow (part of the settings)
        BlockingRepository.put(LOCK_ID, new ReentrantLock());
        NodeID baseID = loadAndSetWorkflow();
        m_dataGen = new NodeID(baseID, 1);
        m_blocker = new NodeID(baseID, 2);
        m_colFilter = new NodeID(baseID, 3);
        m_tblView = new NodeID(baseID, 4);
    }

    @Test
    public void testExecuteNoBlocking() throws Exception {
        checkState(m_dataGen, CONFIGURED);
        checkState(m_blocker, CONFIGURED);
        checkState(m_colFilter, CONFIGURED);
        checkState(m_tblView, CONFIGURED);

        executeAndWait(m_tblView);
        checkState(m_tblView, EXECUTED);
    }

    @Test
    public void testBlockingStates() throws Exception {
        WorkflowManager m = getManager();
        ReentrantLock execLock = BlockingRepository.get(LOCK_ID);
        execLock.lock();
        try {
            m.executeUpToHere(m_tblView);
            // can't tell about the data generator, but the remaining three
            // should be in some executing state
            checkState(m_blocker, CONFIGURED_MARKEDFOREXEC,
                    CONFIGURED_QUEUED, PREEXECUTE, EXECUTING);
            checkState(m_colFilter, CONFIGURED_MARKEDFOREXEC);
            checkState(m_tblView, CONFIGURED_MARKEDFOREXEC);
            assertTrue(m.getNodeContainerState().isExecutionInProgress());
        } finally {
            execLock.unlock();
        }
        // give the workflow manager time to wrap up
        waitWhileNodeInExecution(m_tblView);
    }

    @Test
    public void testPropertiesOfExecutingNode() throws Exception {
        WorkflowManager m = getManager();
        ReentrantLock execLock = BlockingRepository.get(LOCK_ID);
        execLock.lock();
        try {
            m.executeUpToHere(m_tblView);
            checkState(m_blocker, CONFIGURED_MARKEDFOREXEC, EXECUTING);
            final NodeContainer blockerNC = m.getNodeContainer(m_blocker);
            waitWhile(blockerNC, new Hold() {
                @Override
                protected boolean shouldHold() {
                    return blockerNC.getInternalState().equals(CONFIGURED_MARKEDFOREXEC)
                    || blockerNC.getInternalState().equals(CONFIGURED_QUEUED)
                    || blockerNC.getInternalState().equals(PREEXECUTE);
                }
            });
            checkState(m_blocker, EXECUTING);

            // test reset node
            assertFalse(m.canResetNode(m_blocker));
            try {
                m.resetAndConfigureNode(m_blocker);
                fail();
            } catch (IllegalStateException ise) {
                // expected
            }

            // test delete node
            assertFalse(m.canRemoveNode(m_blocker));
            try {
                m.removeNode(m_blocker);
                fail();
            } catch (IllegalStateException ise) {
            }

            // test outgoing connection delete
            ConnectionContainer cc = findInConnection(m_colFilter, 1);
            assertNotNull(cc);
            assertFalse(m.canRemoveConnection(cc));
            try {
                m.removeConnection(cc);
                fail();
            } catch (IllegalStateException ise) {
            }

            // test cancel node
            m.cancelExecution(blockerNC);
            waitWhile(blockerNC, new Hold() {
                @Override
                protected boolean shouldHold() {
                    return !blockerNC.getInternalState().equals(CONFIGURED);
                }
            });
            checkState(m_blocker, CONFIGURED);
            checkState(m_colFilter, CONFIGURED);
            checkState(m_tblView, CONFIGURED);
            assertTrue(m.canRemoveConnection(cc));
        } finally {
            execLock.unlock();
        }
        // give the workflow manager time to wrap up
    }

    @Test
    public void testPropertiesOfMarkedNode() throws Exception {
        WorkflowManager m = getManager();
        ReentrantLock execLock = BlockingRepository.get(LOCK_ID);
        execLock.lock();
        try {
            m.executeUpToHere(m_tblView);

            checkState(m_colFilter, CONFIGURED_MARKEDFOREXEC);
            checkState(m_tblView, CONFIGURED_MARKEDFOREXEC);

            // test reset node
            assertFalse(m.canResetNode(m_colFilter));
            try {
                m.resetAndConfigureNode(m_colFilter);
                fail();
            } catch (IllegalStateException ise) {
                // expected
            }

            // test delete node
            assertFalse(m.canRemoveNode(m_colFilter));
            try {
                m.removeNode(m_colFilter);
                fail();
            } catch (IllegalStateException ise) {
            }

            // test outgoing connection delete
            ConnectionContainer inConnection = findInConnection(m_blocker, 1);
            ConnectionContainer outConnection = findInConnection(m_tblView, 1);
            assertNotNull(inConnection);
            assertNotNull(outConnection);
            assertFalse(m.canRemoveConnection(inConnection));
            assertFalse(m.canRemoveConnection(outConnection));
            try {
                m.removeConnection(inConnection);
                fail();
            } catch (IllegalStateException ise) {
            }
            try {
                m.removeConnection(outConnection);
                fail();
            } catch (IllegalStateException ise) {
            }

            m.cancelExecution(m.getNodeContainer(m_colFilter));
            checkState(m_colFilter, CONFIGURED);
            checkState(m_tblView, CONFIGURED);

            checkState(m_blocker, CONFIGURED_MARKEDFOREXEC,
                    CONFIGURED_QUEUED, PREEXECUTE, EXECUTING);
        } finally {
            execLock.unlock();
        }
        // give the workflow manager time to wrap up
        waitWhileNodeInExecution(m_blocker);
    }

    /** {@inheritDoc} */
    @Override
    @After
    public void tearDown() throws Exception {
        BlockingRepository.remove(LOCK_ID);
        super.tearDown();
    }

}
