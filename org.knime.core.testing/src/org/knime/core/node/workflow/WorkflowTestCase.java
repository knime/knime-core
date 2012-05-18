/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import junit.framework.TestCase;

import org.eclipse.core.runtime.FileLocator;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public abstract class WorkflowTestCase extends TestCase {

    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    private WorkflowManager m_manager;
    private ReentrantLock m_lock;

    /**
     *
     */
    public WorkflowTestCase() {
        m_lock = new ReentrantLock();
    }

    protected NodeID loadAndSetWorkflow() throws Exception {
        ClassLoader l = getClass().getClassLoader();
        String workflowDirString = getClass().getPackage().getName();
        URL workflowURL = l.getResource(workflowDirString.replace('.', '/'));
        if (workflowURL == null) {
            throw new Exception("Can't load workflow that's expected to be "
                    + "in package " + workflowDirString);
        }

        if (!"file".equals(workflowURL.getProtocol())) {
            workflowURL = FileLocator.resolve(workflowURL);
        }

        File workflowDir = new File(workflowURL.getFile());
        if (!workflowDir.isDirectory()) {
            throw new Exception("Can't load workflow directory: "
                    + workflowDir);
        }
        WorkflowLoadResult loadResult = WorkflowManager.ROOT.load(
                workflowDir, new ExecutionMonitor(),
                WorkflowLoadHelper.INSTANCE, false);
        WorkflowManager m = loadResult.getWorkflowManager();
        if (m == null) {
            throw new Exception("Errors reading workflow: "
                    + loadResult.getFilteredError("", LoadResultEntryType.Ok));
        } else {
            switch (loadResult.getType()) {
            case Ok:
                break;
            default:
                m_logger.info("Errors reading workflow (proceeding anyway): ");
                dumpLineBreakStringToLog(loadResult.getFilteredError("",
                    LoadResultEntryType.Warning));
            }
        }
        setManager(m);
        return m.getID();
    }

    /**
     * @param manager the manager to set
     */
    protected void setManager(final WorkflowManager manager) {
        m_manager = manager;
    }

    /**
     * @return the manager
     */
    protected WorkflowManager getManager() {
        return m_manager;
    }

    protected void checkState(final NodeID id,
            final State... expected) throws Exception {
        NodeContainer nc = findNodeContainer(id);
        checkState(nc, expected);
    }

    protected void checkState(final NodeContainer nc,
            final State... expected) throws Exception {
        State actual = nc.getState();
        boolean matches = false;
        for (State s : expected) {
            if (actual.equals(s)) {
                matches = true;
            }
        }
        if (!matches) {
            String error = "node " + nc.getNameWithID() + " has wrong state; "
            + "expected (any of) " + Arrays.toString(expected) + ", actual "
            + actual + " (dump follows)";
            m_logger.info("Test failed: " + error);
            dumpWorkflowToLog();
            fail(error);
        }
    }

    protected void checkMetaOutState(final NodeID metaID, final int portIndex,
            final State... expected) throws Exception {
        NodeContainer nc = findNodeContainer(metaID);
        if (!(nc instanceof WorkflowManager)) {
            throw new IllegalArgumentException("Node with ID " + metaID
                    + " is not a meta node: " + nc.getNameWithID());
        }
        WorkflowOutPort p = ((WorkflowManager)nc).getOutPort(portIndex);
        State actual = p.getNodeState();
        boolean matches = false;
        for (State s : expected) {
            if (actual.equals(s)) {
                matches = true;
            }
        }
        if (!matches) {
            String error = "Workflow outport " + portIndex + " of WFM "
                + nc.getNameWithID() + " has wrong state; expected (any of) "
                + Arrays.toString(expected) + ", actual " + actual
                + " (dump follows)";
            m_logger.info("Test failed: " + error);
            dumpWorkflowToLog();
            fail(error);
        }
    }

    protected WorkflowManager findParent(final NodeID id) {
        if (m_manager == null) {
            throw new NullPointerException("WorkflowManager not set.");
        }
        if (!id.hasPrefix(m_manager.getID())) {
            throw new IllegalArgumentException("NodeID " + id + " has not "
                    + "same prefix as WorkflowManager: " + m_manager.getID());
        }
        if (!id.hasSamePrefix(m_manager.getID())) {
            WorkflowManager myParent = findParent(id.getPrefix());
            NodeContainer current = myParent.getNodeContainer(id.getPrefix());
            if (!(current instanceof WorkflowManager)) {
                throw new IllegalArgumentException("Parent is not a WFM: "
                        + current.getNameWithID());
            }
            return (WorkflowManager)current;
        }
        return m_manager;
    }

    protected NodeContainer findNodeContainer(final NodeID id)
    throws Exception {
        WorkflowManager parent = findParent(id);
        return parent.getNodeContainer(id);
    }

    protected ConnectionContainer findInConnection(final NodeID id,
            final int port)
        throws Exception {
        WorkflowManager parent = findParent(id);
        for (ConnectionContainer cc : parent.getConnectionContainers()) {
            if (cc.getDest().equals(id) && cc.getDestPort() == port) {
                return cc;
            }
        }
        return null;
    }

    protected ConnectionContainer findLeavingWorkflowConnection(final NodeID id,
            final int port) throws Exception {
        NodeContainer nc = findNodeContainer(id);
        if (!(nc instanceof WorkflowManager)) {
            throw new IllegalArgumentException("Node " + id
                    + " is not a workflow manager");
        }
        for (ConnectionContainer cc
                : ((WorkflowManager)nc).getConnectionContainers()) {
            if (cc.getDest().equals(id) && cc.getDestPort() == port) {
                return cc;
            }
        }
        return null;
    }

    protected void executeAllAndWait() throws Exception {
        m_manager.getParent().executeUpToHere(m_manager.getID());
        waitWhileInExecution();
    }

    protected void executeAndWait(final NodeID... ids)
        throws Exception {
        NodeID prefix = null;
        WorkflowManager parent = null;
        for (NodeID id : ids) {
            if (prefix == null) {
                prefix = id.getPrefix();
                parent = findParent(id);
            } else if (!prefix.equals(id.getPrefix())) {
                throw new IllegalArgumentException("Mixing NodeIDs of "
                        + "different levels " + Arrays.toString(ids));
            }
        }
        if (parent != null) {
            parent.executeUpToHere(ids);
        }
        m_lock.lock();
        try {
            for (NodeID id : ids) {
                waitWhileNodeInExecution(id);
            }
        } finally {
            m_lock.unlock();
        }
    }

    protected void reset(final NodeID... ids) {
        for (NodeID id : ids) {
            WorkflowManager parent = findParent(id);
            parent.resetAndConfigureNode(id);
        }
    }

    protected void deleteConnection(final NodeID destination, final int destPort) {
        WorkflowManager parent = findParent(destination);
        final ConnectionContainer inConn =
            parent.getIncomingConnectionFor(destination, destPort);
        parent.removeConnection(inConn);
    }

    protected void waitWhileInExecution() throws Exception {
        waitWhileNodeInExecution(m_manager);
    }

    protected void waitWhileNodeInExecution(final NodeID id) throws Exception {
        waitWhileNodeInExecution(findNodeContainer(id));
    }

    protected void waitWhileNodeInExecution(final NodeContainer node)
    throws Exception {
        waitWhile(node, new Hold() {
            @Override
            protected boolean shouldHold() {
                State s = node.getState();
                return s.executionInProgress();
            }
        });
    }

    protected void waitWhile(final NodeContainer nc,
            final Hold hold) throws Exception {
        if (!hold.shouldHold()) {
            return;
        }
        final Condition condition = m_lock.newCondition();
        NodeStateChangeListener l = new NodeStateChangeListener() {
            /** {@inheritDoc} */
            @Override
            public void stateChanged(final NodeStateEvent state) {
                m_lock.lock();
                try {
                    m_logger.info("Received " + state);
                    condition.signalAll();
                } finally {
                    m_lock.unlock();
                }
            }
        };
        nc.addNodeStateChangeListener(l);
        m_lock.lock();
        try {
            while (hold.shouldHold()) {
                int secToWait = hold.getSecondsToWaitAtMost();
                if (secToWait > 0) {
                    if (!condition.await(secToWait, TimeUnit.SECONDS)) {
                        m_logger.warn(
                                "Timeout elapsed before condition was true");
                        break;
                    }
                } else {
                    condition.await(5, TimeUnit.SECONDS);
                }
            }
        } finally {
            m_lock.unlock();
            nc.removeNodeStateChangeListener(l);
        }
    }


    /** {@inheritDoc} */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (m_manager != null) {
            // in most cases we wait for individual nodes to finish. This
            // does not mean that the workflow state is also updated, give
            // it a second to finish its cleanup
            waitWhile(m_manager, new Hold() {
                /** {@inheritDoc} */
                @Override
                protected boolean shouldHold() {
                    return m_manager.getState().executionInProgress();
                }
                /** {@inheritDoc} */
                @Override
                protected int getSecondsToWaitAtMost() {
                    return 2;
                }
            });
            if (!WorkflowManager.ROOT.canRemoveNode(m_manager.getID())) {
                String error = "Cannot remove workflow, dump follows";
                m_logger.error(error);
                dumpWorkflowToLog();
                fail(error);
            }
            WorkflowManager.ROOT.removeProject(m_manager.getID());
            setManager(null);
        }
    }

    protected int getNrTablesInGlobalRepository() {
        return m_manager.getGlobalTableRepository().size();
    }

    protected void dumpWorkflowToLog() throws IOException {
        String toString = m_manager.printNodeSummary(m_manager.getID(), 0);
        dumpLineBreakStringToLog(toString);
    }

    protected void dumpLineBreakStringToLog(final String s) throws IOException {
        BufferedReader r = new BufferedReader(new StringReader(s));
        String line;
        while ((line = r.readLine()) != null) {
            m_logger.info(line);
        }
        r.close();
    }

    protected abstract class Hold {
        protected abstract boolean shouldHold();
        protected int getSecondsToWaitAtMost() {
            return -1;
        }
    }

}
