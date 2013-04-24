/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2013
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 *
 * History
 *   01.11.2008 (wiswedel): created
 */
package org.knime.core.node.workflow;

import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED_MARKEDFOREXEC;
import static org.knime.core.node.workflow.InternalNodeContainerState.CONFIGURED_QUEUED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTED;
import static org.knime.core.node.workflow.InternalNodeContainerState.EXECUTING;
import static org.knime.core.node.workflow.InternalNodeContainerState.PREEXECUTE;

import java.util.concurrent.locks.ReentrantLock;

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

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // the id is used here and in the workflow (part of the settings)
        BlockingRepository.put(LOCK_ID, new ReentrantLock());
        NodeID baseID = loadAndSetWorkflow();
        m_dataGen = new NodeID(baseID, 1);
        m_blocker = new NodeID(baseID, 2);
        m_colFilter = new NodeID(baseID, 3);
        m_tblView = new NodeID(baseID, 4);
    }

    public void testExecuteNoBlocking() throws Exception {
        checkState(m_dataGen, CONFIGURED);
        checkState(m_blocker, CONFIGURED);
        checkState(m_colFilter, CONFIGURED);
        checkState(m_tblView, CONFIGURED);

        executeAndWait(m_tblView);
        checkState(m_tblView, EXECUTED);
    }

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
    protected void tearDown() throws Exception {
        BlockingRepository.remove(LOCK_ID);
        super.tearDown();
    }

}
