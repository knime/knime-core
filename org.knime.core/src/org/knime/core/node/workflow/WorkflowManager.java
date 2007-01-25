/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * -------------------------------------------------------------------
 * 
 * History
 *   13.02.2005 (M. Berthold): created
 */
package org.knime.core.node.workflow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeProgressMonitor;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NodeStateListener;
import org.knime.core.node.NodeStatus;
import org.knime.core.util.MutableInteger;

/**
 * Manager for a workflow holding Nodes and the connecting edge information. The
 * information is stored in a graph based data structure and allows to access
 * predecessors and successors. For performance reasons this implementation is
 * specific to vertices being of type <code>org.knime.dev.node.Node</code> and
 * (directed) edges connecting ports indicated by indices.
 * 
 * @author M. Berthold, University of Konstanz
 * @author Florian Georg, University of Konstanz
 * @author Thorsten Meinl, University of Konstanz
 */
public class WorkflowManager implements WorkflowListener {
    private class MyNodeStateListener implements NodeStateListener {
        /**
         * Callback for NodeContainer state-changed events. Update pool of
         * executable nodes if we are in "workflow execution" mode.
         * 
         * @param state The node state event type.
         * @param nodeID The node's ID.
         */
        public synchronized void stateChanged(final NodeStatus state,
                final int nodeID) {
            NodeContainer changedNode = m_nodesByID.get(nodeID);
            assert (changedNode != null);

            // if this is an event indicating the start of a node's execution:
            if (state instanceof NodeStatus.Reset) {
                fireWorkflowEvent(new WorkflowEvent.NodeReset(nodeID, null,
                        null));
            } else if (state instanceof NodeStatus.Configured) {
                fireWorkflowEvent(new WorkflowEvent.NodeConfigured(nodeID,
                        null, null));
            } else if (state instanceof NodeStatus.ExtrainfoChanged) {
                fireWorkflowEvent(new WorkflowEvent.NodeExtrainfoChanged(
                        nodeID, null, null));
            }
        }
    }

    /**
     * This class is the executor for the workflow. Note that there is only one
     * executor for a workflow and all its children.
     * 
     * @author Thorsten Meinl, University of Konstanz
     */
    private static class WorkflowExecutor implements NodeStateListener {
        private class MyNodePM extends DefaultNodeProgressMonitor {
            private final NodeContainer m_node;

            /**
             * Creates a new internal progress monitor for the workflow
             * executor.
             * 
             * @param node the node
             */
            public MyNodePM(final NodeContainer node) {
                m_node = node;
            }

            @Override
            public synchronized void setExecuteCanceled() {
                super.setExecuteCanceled();
                if (m_runningNodes.containsKey(m_node)) {
                    for (NodeContainer succ : m_node.getAllSuccessors()) {
                        if (m_waitingNodes.remove(succ) != null) {
                            succ.getWorkflowManager().fireWorkflowEvent(
                                    new WorkflowEvent.NodeFinished(
                                            succ.getID(), null, null));
                        }
                    }
                }

            }
        }

        private final Map<NodeContainer, NodeProgressMonitor> m_runningNodes =
                new ConcurrentHashMap<NodeContainer, NodeProgressMonitor>();

        private final Map<NodeContainer, NodeProgressMonitor> m_waitingNodes =
                new ConcurrentHashMap<NodeContainer, NodeProgressMonitor>();

        private final Map<NodeContainer, CountDownLatch> m_waitLocks =
                new WeakHashMap<NodeContainer, CountDownLatch>();

        private final Object m_addLock = new Object(),
                m_transferLock = new Object(), m_finishLock = new Object();

        /**
         * Create new empty workflow executer.
         */
        WorkflowExecutor() {
        }

        /**
         * Adds new nodes to the list of nodes that are waiting for execution.
         * 
         * @param nodes a list of nodes that should be executed
         */
        public void addWaitingNodes(final Collection<NodeContainer> nodes) {
            boolean change = false;
            for (final NodeContainer nc : nodes) {
                boolean b;
                // avoid that a node is removed from m_waitingNodes but not yet
                // inserted into m_runningNodes
                synchronized (m_transferLock) {
                    b =
                            m_waitingNodes.containsKey(nc)
                                    || m_runningNodes.containsKey(nc);
                }
                if (!b) {
                    MyNodePM pm = new MyNodePM(nc);
                    synchronized (m_addLock) {
                        m_waitingNodes.put(nc, pm);
                        CountDownLatch old =
                                m_waitLocks.put(nc, new CountDownLatch(1));
                        if (old != null) {
                            old.countDown();
                        }
                    }

                    // inform the node that it is queued now
                    nc.queuedForExecution();
                    nc.getWorkflowManager().fireWorkflowEvent(
                            new WorkflowEvent.NodeWaiting(nc.getID(), nc, pm));

                    change = true;
                }
            }
            if (change) {
                startNewNodes(false);
            }
        }

        /**
         * Cancel execution of all nodes. Waiting nodes are just removed from
         * the waiting list, running nodes are sent a signal (via the progress
         * monitor) that they should terminate.
         */
        public void cancelExecution() {
            for (NodeProgressMonitor pm : m_runningNodes.values()) {
                pm.setExecuteCanceled();
            }
            // avoid that a node is added to m_waitingNodes after the finish
            // events have been sent and before the map is cleared;
            // we will miss the finish event for this node otherwise
            synchronized (m_addLock) {
                Set<NodeContainer> temp =
                        new HashSet<NodeContainer>(m_waitingNodes.keySet());
                m_waitingNodes.clear();
                for (NodeContainer nc : temp) {
                    nc.getWorkflowManager().fireWorkflowEvent(
                            new WorkflowEvent.NodeFinished(nc.getID(), null,
                                    null));
                }
                for (CountDownLatch cdl : m_waitLocks.values()) {
                    cdl.countDown();
                }
                m_waitLocks.clear();
            }
        }

        /**
         * Cancel execution of the passed nodes. Waiting nodes are just removed
         * from the waiting list, running nodes are sent a signal (via the
         * progress monitor) that they should terminate.
         * 
         * @param nodes a list of nodes that should be canceled
         */
        public void cancelExecution(final Collection<NodeContainer> nodes) {
            Set<NodeContainer> cancelNodes = new HashSet<NodeContainer>();
            cancelNodes.addAll(nodes);

            synchronized (m_transferLock) {
                for (NodeContainer nc : nodes) {
                    if (m_runningNodes.containsKey(nc)) {
                        m_runningNodes.get(nc).setExecuteCanceled();
                        cancelNodes.addAll(nc.getAllSuccessors());
                        if (nc.getEmbeddedWorkflowManager() != null) {
                            cancelExecution(nc.getEmbeddedWorkflowManager()
                                    .getNodes());
                        }
                    }
                }

                for (Iterator<NodeContainer> it = cancelNodes.iterator(); it
                        .hasNext();) {
                    NodeContainer cont = it.next();
                    if (m_waitingNodes.remove(cont) == null) {
                        it.remove();
                    }
                    CountDownLatch cdl = m_waitLocks.get(cont);
                    if (cdl != null) {
                        cdl.countDown();
                    }
                }
            }

            for (NodeContainer nc : cancelNodes) {
                nc.stateChanged(
                        new NodeStatus.Configured("Removed from queue"), nc
                                .getID());
                nc.getWorkflowManager().fireWorkflowEvent(
                        new WorkflowEvent.NodeFinished(nc.getID(), null, null));
            }
        }

        /**
         * Returns if any of the nodes inside the passed workflow manager is
         * currently executing or waiting for execution. A parent workflow
         * manager additionally asks all of its children.
         * 
         * @param wfm a workflow manager
         * @return <code>true</code> if an execution is in progress,
         *         <code>false</code> otherwise
         */
        public boolean executionInProgress(final WorkflowManager wfm) {
            // check if any of the nodes in the lists are from the
            // passed WFM

            synchronized (m_transferLock) {
                for (NodeContainer nc : m_runningNodes.keySet()) {
                    if (wfm.m_nodesByID.values().contains(nc)) {
                        LOGGER.debug("Node " + nc + " is still running");
                        return true;
                    }
                }

                for (NodeContainer nc : m_waitingNodes.keySet()) {
                    if (wfm.m_nodesByID.values().contains(nc)) {
                        LOGGER.debug("Node " + nc + " is still waiting");
                        return true;
                    }
                }
            }

            for (WeakReference<WorkflowManager> wr : wfm.m_children) {
                if (wr.get() != null) {
                    if (executionInProgress(wr.get())) {
                        // this may happen if (parts of) a metaworkflow is/are
                        // excuted by the user without actually executing the
                        // meta node itself
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Checks if new nodes are executable now and if so, starts them.
         * Threads waiting on m_waitingNodes are awakened if no nodes are
         * running any more and no new executable node has been found.
         */
        private void startNewNodes(final boolean watchdog) {
            synchronized (m_finishLock) {
                Iterator<Map.Entry<NodeContainer, NodeProgressMonitor>> it =
                        m_waitingNodes.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<NodeContainer, NodeProgressMonitor> e = it.next();
                    if (e.getKey().isExecutable()) {
                        NodeContainer nc = e.getKey();
                        NodeProgressMonitor pm = e.getValue();

                        synchronized (m_transferLock) {
                            it.remove();
                            m_runningNodes.put(nc, pm);
                        }

                        try {
                            nc.getWorkflowManager().fireWorkflowEvent(
                                    new WorkflowEvent.NodeStarted(nc.getID(),
                                            nc, pm));
                            nc.addListener(this);
                            nc.startExecution(pm);
                        } catch (Exception ex) {
                            // remove from running nodes due to an error
                            m_runningNodes.remove(nc);
                        }
                    } else if (e.getKey().isExecuted()) {
                        it.remove();
                    } else if (!checkForDeadlock(e.getKey())) {
                        LOGGER.warn("A predecessor of " + e.getKey() + " is "
                                + " neither running nor waiting or executed. "
                                + "This is a deadlock, I will thus cancel "
                                + e.getKey() + ".");
                        cancelExecution(Collections.singletonList(e.getKey()));
                    }
                }

                if (m_runningNodes.size() == 0) {
                    if (m_waitingNodes.size() > 0) {
                        if (watchdog) {
                            LOGGER.error("Whoa there, the watchdog found a "
                                    + "possible deadlock situation! Some nodes "
                                    + "are still waiting, but none is running"
                                    + m_waitingNodes);
                        } else {
                            LOGGER.warn("Some nodes were still waiting but "
                                    + "none is running: " + m_waitingNodes);
                        }
                    }

                    for (NodeContainer nc : m_waitingNodes.keySet()) {
                        nc.getWorkflowManager().fireWorkflowEvent(
                                new WorkflowEvent.NodeFinished(nc.getID(),
                                        null, null));
                    }
                    m_waitingNodes.clear();

                    m_finishLock.notifyAll();
                }
            }
        }

        private boolean checkForDeadlock(final NodeContainer cont) {
            if (!cont.isExecutable() && !cont.isExecuted()) {
                boolean predStillWaiting = false;
                for (NodeContainer pred : cont.getPredecessors()) {
                    if (m_waitingNodes.containsKey(pred)
                            || m_runningNodes.containsKey(pred)) {
                        predStillWaiting = true;
                        break;
                    }
                }

                // If a meta workflow is executed by hand and the meta node
                // has not a valid data table yet the meta input node of the
                // meta flow is not executable and a non-existing deadlock is
                // detected. The following check prevents this.
                if (cont.getInPorts().size() == 0) {
                    WorkflowManager parent = cont.getWorkflowManager().m_parent;
                    if ((parent != null) && executionInProgress(parent)) {
                        return true;
                    }
                }

                if (!predStillWaiting) {
                    LOGGER.warn("The node " + cont + " is not executable but "
                            + "waiting for execution and "
                            + "all its predecessor are already executed.");
                    return false;
                }

                for (NodeContainer pred : cont.getPredecessors()) {
                    if (!checkForDeadlock(pred)) {
                        return false;
                    }
                }
            }

            return true;
        }

        /**
         * @see org.knime.core.node.NodeStateListener
         *      #stateChanged(org.knime.core.node.NodeStatus, int)
         */
        public void stateChanged(final NodeStatus state, final int id) {
            if ((state instanceof NodeStatus.EndExecute)
                    || (state instanceof NodeStatus.ExecutionCanceled)) {
                Iterator<Map.Entry<NodeContainer, NodeProgressMonitor>> it =
                        m_runningNodes.entrySet().iterator();
                while (it.hasNext()) {
                    NodeContainer nc = it.next().getKey();
                    if (nc.getID() == id) {
                        nc.removeListener(this);

                        synchronized (m_finishLock) {
                            it.remove();
                        }

                        CountDownLatch cdl = m_waitLocks.get(nc);
                        if (cdl != null) {
                            cdl.countDown();
                        }

                        nc.getWorkflowManager().fireWorkflowEvent(
                                new WorkflowEvent.NodeFinished(nc.getID(), nc,
                                        nc));

                        if (state instanceof NodeStatus.ExecutionCanceled) {
                            cancelExecution(nc.getAllSuccessors());
                        }

                        if (nc.getStatus() instanceof NodeStatus.Error) {
                            cancelExecution(nc.getAllSuccessors());
                        } else if (nc.isExecuted()) {
                            List<NodeContainer> autoNodes =
                                    new ArrayList<NodeContainer>();
                            // check if auto-executable nodes are following
                            for (NodeContainer[] row : nc.getSuccessors()) {
                                for (NodeContainer cont : row) {
                                    if (cont.isAutoExecutable()
                                            && cont.isExecutable()) {
                                        autoNodes.add(cont);
                                    }
                                }
                            }
                            addWaitingNodes(autoNodes);
                        }
                    }
                }

                startNewNodes(false);
                synchronized (m_finishLock) {
                    m_finishLock.notifyAll();
                }
            }
        }

        /**
         * Blocks until the passed node has finished execution.
         * 
         * @param cont the NodeContainer that should be waited for
         */
        public void waitUntilFinished(final NodeContainer cont) {
            CountDownLatch cdl = m_waitLocks.get(cont);
            if (cdl != null) {
                try {
                    cdl.await();
                } catch (InterruptedException ex) {
                    LOGGER.info(ex.getMessage(), ex);
                }
            }
        }

        /**
         * Blocks until all nodes that the passed workflow manager is
         * responsible are finished.
         * 
         * @param wfm a workflow manager
         */
        public void waitUntilFinished(final WorkflowManager wfm) {
            synchronized (m_finishLock) {
                while ((m_runningNodes.size() > 0)
                        || (m_waitingNodes.size() > 0)) {
                    // check if any of the nodes in the lists are from the
                    // passed WFM
                    boolean interesting = false;
                    for (NodeContainer nc : m_runningNodes.keySet()) {
                        if (wfm.m_nodesByID.values().contains(nc)) {
                            interesting = true;
                            break;
                        }
                    }

                    if (!interesting) {
                        for (NodeContainer nc : m_waitingNodes.keySet()) {
                            if (wfm.m_nodesByID.values().contains(nc)) {
                                interesting = true;
                                break;
                            }
                        }
                    }

                    if (interesting) {
                        try {
                            // in order to prevent deadlocks due to possible
                            // programming errors, re-check every 5 secons
                            m_finishLock.wait(5000);
                        } catch (InterruptedException ex) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
        }

        /**
         * Checks if it is allowed to reset the given node container. A
         * container may not be reset if it is currently executing or if any of
         * its successor nodes is waiting for execution or currently executing.
         * 
         * @param nc any node container
         * @return <code>true</code> if it can be reset, <code>false</code>
         *         otherwise
         */
        public boolean canBeReset(final NodeContainer nc) {
            if (m_runningNodes.containsKey(nc)) {
                return false;
            }
            for (NodeContainer succ : nc.getAllSuccessors()) {
                if (m_runningNodes.containsKey(succ)
                        || m_waitingNodes.containsKey(succ)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Checks if the given node can be deleted safely from the workflow. A
         * node can only be deleted if it is not queued or running and none of
         * its successor are queued or running.
         * 
         * @param cont the node that should be deleted
         * @return <code>true</code> if the node can be deleted
         *         <code>false</code> otherwise
         */
        public boolean canBeDeleted(final NodeContainer cont) {
            if (m_runningNodes.containsKey(cont)
                    || m_waitingNodes.containsKey(cont)) {
                return false;
            }

            for (NodeContainer succ : cont.getAllSuccessors()) {
                if (m_runningNodes.containsKey(succ)
                        || m_waitingNodes.containsKey(succ)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Returns if the given node is currently queued and waiting for
         * execution.
         * 
         * @param cont any node container
         * @return <code>true</code> if the node is queued, <code>false</code>
         *         otherwise
         */
        public boolean isQueued(final NodeContainer cont) {
            return m_waitingNodes.containsKey(cont);
        }
    }

    /** Key for connections. */
    public static final String KEY_CONNECTIONS = "connections";

    /** Links the node settings file name. */
    public static final String KEY_NODE_SETTINGS_FILE = "node_settings_file";

    /** Key for nodes. */
    public static final String KEY_NODES = "nodes";

    /** Key for current running connection id. */
    private static final String KEY_RUNNING_CONN_ID = "runningConnectionID";

    /** Key for current running node id. */
    private static final String KEY_RUNNING_NODE_ID = "runningNodeID";

    /** The node logger for this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(WorkflowManager.class);

    /** Workflow version. */
    private static final String CFG_VERSION = "version";

    private String m_loadedVersion = KNIMEConstants.VERSION;

    /**
     * Identifier for KNIME workflows.
     */
    public static final String WORKFLOW_FILE = "workflow.knime";

    private final List<WeakReference<WorkflowManager>> m_children =
            new ArrayList<WeakReference<WorkflowManager>>();

    // quick access to connections
    private final Map<Integer, ConnectionContainer> m_connectionsByID =
            new HashMap<Integer, ConnectionContainer>();

    // change listener support
    private final CopyOnWriteArrayList<WorkflowListener> m_eventListeners =
            new CopyOnWriteArrayList<WorkflowListener>();

    private final WorkflowExecutor m_executor;

    // quick access to IDs via Nodes
    private final Map<NodeContainer, Integer> m_idsByNode =
            new HashMap<NodeContainer, Integer>();

    // quick access to nodes by ID
    private final Map<Integer, NodeContainer> m_nodesByID =
            new LinkedHashMap<Integer, NodeContainer>();

    private final MyNodeStateListener m_nodeStateListener =
            new MyNodeStateListener();

    private final WorkflowManager m_parent;

    private final MutableInteger m_runningConnectionID;

    // internal variables to allow generation of unique indices
    private final MutableInteger m_runningNodeID;

    private final List<NodeContainer> m_detachedNodes =
            new ArrayList<NodeContainer>();
    
    /** Table repository, important for blob (de)serialization. A sub workflow
     * manager will use the map of its parent WFM. */
    private final HashMap<Integer, ContainerTable> m_tableRepository;

    /**
     * Create new Workflow.
     */
    public WorkflowManager() {
        m_parent = null;
        // this is a local workaround: we ran into problems with workflows
        // that contain (saved) nodes, which have been written with 1.1.x.
        // Their output tables generally have an id of -1 - which is invalid.
        m_tableRepository = new HashMap<Integer, ContainerTable>() {
            @Override
            public ContainerTable put(
                    final Integer key, final ContainerTable value) {
                if (key < 0) {
                    LOGGER.debug("Table has an invalid ID! " 
                        + "(This message can be ignored if the flow " 
                        + "was written with a version prior to 1.2.0.");
                    return null;
                }
                return super.put(key, value);
            }
        };
        m_executor = new WorkflowExecutor();
        m_runningConnectionID = new MutableInteger(-1);
        m_runningNodeID = new MutableInteger(0);
    }

    /**
     * Create new WorkflowManager by a given <code>WORKFLOW_FILE</code>. All
     * nodes and connection are initialzied, and - if available -
     * <code>NodeSettings</code>, <code>DataTableSpec</code>,
     * <code>DataTable</code>, and <code>ModelContent</code> are loaded
     * into each node.<br />
     * This constructor does essentially nothing else than calling
     * {@link #WorkflowManager()} and {@link #load(File, NodeProgressMonitor)}.
     * The "problem" with this constructor is, that once an error occurs while
     * loading the workflow you won't have the manager at hand afterwards.
     * 
     * @param workflowFile the location of the workflow file,
     *            {@link #WORKFLOW_FILE}
     * @param progMon a progress monitor that is used to report progress while
     *            loading the workflow
     * @throws InvalidSettingsException if settings cannot be read
     * @throws CanceledExecutionException if loading was canceled
     * @throws IOException if the workflow file can not be found or files to
     *             load node internals
     * @throws WorkflowException if an exception occurs while loading the
     *             workflow structure
     */
    public WorkflowManager(final File workflowFile,
            final NodeProgressMonitor progMon) throws InvalidSettingsException,
            CanceledExecutionException, IOException, WorkflowException {
        this();
        try {
            load(workflowFile, progMon);
        } catch (WorkflowInExecutionException ex) {
            // this is not possible
        }
    }

    /**
     * Creates a new sub workflow manager with the given parent manager.
     * 
     * @param parent the parent workflow manager
     */
    protected WorkflowManager(final WorkflowManager parent) {
        m_parent = parent;
        m_parent.m_children.add(new WeakReference<WorkflowManager>(this));
        m_executor = m_parent.m_executor;
        m_runningConnectionID = parent.m_runningConnectionID;
        m_runningNodeID = parent.m_runningNodeID;
        m_tableRepository = m_parent.m_tableRepository;
    }

    /**
     * Returns the current KNIME workflow file version loaded.
     * 
     * @return Workflow file version.
     */
    public String getWorkflowVersion() {
        return m_loadedVersion;
    }

    private void addConnection(final ConnectionContainer cc) {
        if (m_connectionsByID.containsKey(cc.getID())) {
            throw new IllegalArgumentException("A connection with id #"
                    + cc.getID() + " already exists in the workflow.");
        }

        NodeContainer outNode = cc.getSource();
        NodeContainer inNode = cc.getTarget();
        int outPort = cc.getSourcePortID();
        int inPort = cc.getTargetPortID();

        // check if the outport can be connected to the inport
        inNode.checkConnectPorts(inPort, outNode, outPort);

        m_connectionsByID.put(cc.getID(), cc);

        // add outgoing edge
        outNode.addOutgoingConnection(outPort, inNode);

        // add incoming edge
        inNode.addIncomingConnection(inPort, outNode);
        inNode.connectPorts(inPort, outNode, outPort);

        // add this manager as listener for workflow event
        cc.addWorkflowListener(this);

        // notify listeners
        LOGGER.debug("Added connection (from node id:" + outNode.getID()
                + ", port:" + outPort + " to node id:" + inNode.getID()
                + ", port:" + inPort + ")");
        fireWorkflowEvent(new WorkflowEvent.ConnectionAdded(-1, null, cc));
    }

    /**
     * add a connection between two nodes. The port indices have to be within
     * their valid ranges.
     * 
     * @param idOut identifier of source node
     * @param portOut index of port on source node
     * @param idIn identifier of target node (sink)
     * @param portIn index of port on target
     * @return newly create edge
     * @throws IllegalArgumentException if port indices are invalid
     */
    public synchronized ConnectionContainer addConnection(final int idOut,
            final int portOut, final int idIn, final int portIn) {
        // checkForRunningNodes();

        NodeContainer nodeOut = m_nodesByID.get(idOut);
        NodeContainer nodeIn = m_nodesByID.get(idIn);

        if (nodeOut == null) {
            throw new IllegalArgumentException("Node with id #" + idOut
                    + " does not exist.");
        }
        if (nodeIn == null) {
            throw new IllegalArgumentException("Node with id #" + idIn
                    + " does not exist.");
        }

        ConnectionContainer newConnection =
                new ConnectionContainer(m_runningConnectionID.inc(), nodeOut,
                        portOut, nodeIn, portIn);
        addConnection(newConnection);
        return newConnection;
    }

    /**
     * Adds a listener to the workflow, has no effect if the listener is already
     * registered.
     * 
     * @param listener The listener to add
     */
    public void addListener(final WorkflowListener listener) {
        if (!m_eventListeners.contains(listener)) {
            m_eventListeners.add(listener);
        }
    }

    /**
     * Creates a new node from the given factory, adds the node to the workflow
     * and returns the corresponding <code>NodeContainer</code>.
     * 
     * @param factory the factory to create the node
     * @return the <code>NodeContainer</code> representing the created node
     */
    public synchronized NodeContainer addNewNode(final NodeFactory factory) {
        final int newNodeID = m_runningNodeID.inc();
        assert (!m_nodesByID.containsKey(newNodeID));
        NodeContainer newNode = new NodeContainer(factory, this, newNodeID);
        LOGGER.debug("adding new node '" + newNode + "' to the workflow...");

        // add WorkflowManager as listener for state change events
        newNode.addListener(m_nodeStateListener);
        // and add it to our hashmap of nodes.
        m_nodesByID.put(newNodeID, newNode);
        m_idsByNode.put(newNode, newNodeID);

        // notify listeners
        LOGGER.debug("Added " + newNode.getNameWithID());
        fireWorkflowEvent(
                new WorkflowEvent.NodeAdded(newNodeID, null, newNode));

        LOGGER.debug("done, ID=" + newNodeID);
        return newNode;
    }

    /**
     * adds a new node to the workflow using the predefined identifier-int. If
     * the identifier is already in use an exception will be thrown.
     * 
     * FG: Do we really need this? Internal id manipulation should not be
     * exposed as public I think. MB: Let's leave it private then for now...
     * 
     * @param nc node to be added
     * @throws IllegalArgumentException when the id already exists
     */
    private void addNodeWithID(final NodeContainer nc) {
        Integer id = new Integer(nc.getID());
        if (m_nodesByID.containsKey(id)) {
            throw new IllegalArgumentException("duplicate ID");
        }
        if (m_idsByNode.containsKey(nc)) {
            throw new IllegalArgumentException("duplicate/illegal node");
        }
        nc.addListener(m_nodeStateListener);
        m_nodesByID.put(id, nc);
        m_idsByNode.put(nc, id);

        // notify listeners
        LOGGER.debug("Added " + nc.getNameWithID());
        fireWorkflowEvent(new WorkflowEvent.NodeAdded(id, null, nc));
    }

    /**
     * Returns whether a connection can be added between the given nodes and
     * ports. This may return <code>false</code> if:
     * <ul>
     * <li>Some of the nodeIDs are invalid,</li>
     * <li>some of the port-numbers are invalid,</li>
     * <li>there's already a connection that ends at the given in-port,</li>
     * <li>or (new) this connection would create a loop in the workflow</li>
     * </ul>
     * 
     * @param sourceNode ID of the source node
     * @param outPort Index of the outgoing port
     * @param targetNode ID of the target node
     * @param inPort Index of the incoming port
     * @return <code>true</code> if a connection can be added,
     *         <code>false</code> otherwise
     * @deprecated use the method {@link WorkflowManager
     *             #checkAddConnection(int, int, int, int)} as this method
     *             throws an exception with detailed information instead just a
     *             boolean value.
     */
    @Deprecated
    public boolean canAddConnection(final int sourceNode, final int outPort,
            final int targetNode, final int inPort) {

        // applies the new checkAddConnection method to emulate this old
        // deprecated method
        try {
            checkAddConnection(sourceNode, outPort, targetNode, inPort);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    /**
     * Checks if the given node can be deleted safely from the workflow. A node
     * can only be deleted if it is not queued or running and none of its
     * successor are queued or running.
     * 
     * @param cont the node that should be deleted
     * @return <code>true</code> if the node can be deleted <code>false</code>
     *         otherwise
     */
    public boolean canBeDeleted(final NodeContainer cont) {
        return m_executor.canBeDeleted(cont);
    }

    /**
     * Checks if the given node can be reset safely. A node can only be reset if
     * it is not running and none of its successor are queued or running.
     * 
     * @param cont the node that should be deleted
     * @return <code>true</code> if the node can be deleted <code>false</code>
     *         otherwise
     */
    public boolean canBeReset(final NodeContainer cont) {
        return m_executor.canBeReset(cont);
    }

    /**
     * Checks if a connection can be added between the given nodes. If not, an
     * exception is thrown to deliver detailed information about the reason:
     * <ul>
     * <li>Some of the nodeIDs are invalid,</li>
     * <li>some of the port-numbers are invalid,</li>
     * <li>there's already a connection that ends at the given in-port,</li>
     * <li>or (new) this connection would create a loop in the workflow</li>
     * </ul>
     * 
     * @param sourceNode ID of the source node
     * @param outPort Index of the outgoing port
     * @param targetNode ID of the target node
     * @param inPort Index of the incoming port
     * @throws Exception if the two nodes can not be connected
     */
    public void checkAddConnection(final int sourceNode, final int outPort,
            final int targetNode, final int inPort) throws Exception {

        if ((sourceNode < 0) || (outPort < 0) || (targetNode < 0)
                || (inPort < 0)) {
            // easy sanity check failed - return false;
            throw new IndexOutOfBoundsException("Invalid node/port indices: "
                    + "Source node: " + sourceNode + " Target node: "
                    + targetNode + " Outport: " + outPort + " Inport: "
                    + inPort);
        }

        NodeContainer src = m_nodesByID.get(sourceNode);
        NodeContainer targ = m_nodesByID.get(targetNode);

        if ((src == null) || (targ == null)) {
            // Nodes don't exist (whyever) - return failure
            String message =
                    "WFM: checking for connection between non existing"
                            + " nodes!";
            throw new IllegalArgumentException(message);
        }

        boolean portNumsValid =
                (src.getNrOutPorts() > outPort)
                        && (targ.getNrInPorts() > inPort) && (outPort >= 0)
                        && (inPort >= 0);
        if (!portNumsValid) {
            // port numbers don't exist - return failure
            String message =
                    "WFM: checking for connection for non existing" + " ports!";
            throw new IllegalArgumentException(message);
        }

        ConnectionContainer conn = getIncomingConnectionAt(targ, inPort);
        boolean hasConnection = (conn != null);
        if (hasConnection) {
            // input port already has a connection - return failure
            String message = "WFM: Input port already has a connection.";
            throw new IllegalArgumentException(message);
        }

        boolean isDataConn =
                targ.isDataInPort(inPort) && src.isDataOutPort(outPort);
        boolean isModelConn =
                !targ.isDataInPort(inPort) && !src.isDataOutPort(outPort);
        if (!isDataConn && !isModelConn) {
            // trying to connect data to model port - return failure
            String message =
                    "WFM: Data port can not be connected to a model port.";
            throw new IllegalArgumentException(message);
        }

        // check for loops
        boolean loop = targ.isFollowedBy(src);
        if (loop) {

            String message =
                    "Attempt to create loop (from node id:" + sourceNode
                            + ", port:" + inPort + " to node id:" + targetNode
                            + ", port:" + outPort + ")";
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Cancels the execution of all nodes in the workflow.
     */
    public void cancelExecution() {
        m_executor.cancelExecution();
    }

    // /**
    // * Cancels the execution of the workflow after the passed node.
    // *
    // * @param nodeID the id of the node after which the execution should be
    // * canceled
    // */
    // public void cancelExecutionAfterNode(final int nodeID) {
    // NodeContainer nodeCont = m_nodesByID.get(nodeID);
    // m_executor.cancelExecution(nodeCont.getAllSuccessors());
    // }

    /**
     * Cancels execution of the given node and all its sucessor nodes.
     * 
     * @param node a node
     */
    public void cancelExecution(final NodeContainer node) {
        Collection<NodeContainer> l = node.getAllSuccessors();
        l.add(node);
        m_executor.cancelExecution(l);
    }

    /*
     * checks if any running nodes are in the workflow and throws an exception
     * if this is the case
     */
    private void checkForRunningNodes(final String msg)
            throws WorkflowInExecutionException {
        if (m_executor.executionInProgress(this)) {
            throw new WorkflowInExecutionException(msg
                    + " while execution is in progress");
        }
    }

    /**
     * Removes all nodes and connection from the workflow.
     * 
     * @throws WorkflowInExecutionException if the workflow is currently being
     *             executed
     */
    public synchronized void clear() throws WorkflowInExecutionException {
        checkForRunningNodes("Workflow cannot be cleared");

        List<NodeContainer> containers =
                new ArrayList<NodeContainer>(m_nodesByID.values());
        for (NodeContainer nc : containers) {
            removeNode(nc);
        }

        assert (m_nodesByID.size() == 0);
        assert (m_connectionsByID.size() == 0);
        assert (m_idsByNode.size() == 0);
        if (m_parent == null) {
            m_runningConnectionID.setValue(-1);
            m_runningNodeID.setValue(0);
        }
    }

    /**
     * Configures the passed node but does not reset it. The caller has to
     * ensure that the node is resetted and not executed beforehand.
     * 
     * @param nodeID the node that should be configured
     */
    public void configureNode(final int nodeID) {
        NodeContainer nodeCont = m_nodesByID.get(nodeID);
        nodeCont.configure();
    }

    /**
     * Creates and returns a new workflowmanager that handles a workflow that is
     * contained in the workflow that this manager handles.
     * 
     * @return a subordinate workflow manager
     */
    public WorkflowManager createSubManager() {
        return new WorkflowManager(this);
    }

    /**
     * Creates copies of the node and connection containers passed as arguments.
     * If connection should be copied whose endpoints have not been copied, they
     * are silently ignored.
     * 
     * @param nodeIDs the ids of the nodes that should be copied
     * @param connectionIDs the ids of the connections that should be copied
     * 
     * @return the ids of the newly created containers, the first array element
     *         being the array of node container ids, the second being the array
     *         of connection containers
     * @throws CloneNotSupportedException if the {@link NodeExtraInfo} object of
     *             a node container could not be cloned
     */
    public synchronized int[][] createSubWorkflow(final int[] nodeIDs,
            final int[] connectionIDs) throws CloneNotSupportedException {
        // the new ids to return
        ArrayList<Integer> newNodeIDs = new ArrayList<Integer>();
        ArrayList<Integer> newConnectionIDs = new ArrayList<Integer>();

        // the map is used to map the old node id to the new one
        Map<Integer, Integer> idMap = new HashMap<Integer, Integer>();

        for (int nodeID : nodeIDs) {
            final NodeContainer nc = m_nodesByID.get(nodeID);
            if (nc == null) {
                throw new IllegalArgumentException("A node with id " + nodeID
                        + " is not handled by this workflow manager");
            }

            // create NodeContainer based on NodeSettings object
            final int newId = m_runningNodeID.inc();
            final NodeContainer newNode = new NodeContainer(nc, newId);

            idMap.put(nc.getID(), newId);
            // remember the new id for the return value
            newNodeIDs.add(newId);

            // set the user name to the new id if the init name
            // was set before e.g. "Node_44"
            // get set username
            String currentUserNodeName = newNode.getCustomName();

            // create temprarily the init user name of the copied node
            // to check wether the current name was changed
            String oldInitName = "Node " + nc.getID();
            if (oldInitName.equals(currentUserNodeName)) {
                newNode.setCustomName("Node " + newId);
            }

            // and add it to workflow
            addNodeWithID(newNode);
        }

        for (int connID : connectionIDs) {
            final ConnectionContainer cc = m_connectionsByID.get(connID);
            if (cc == null) {
                throw new IllegalArgumentException("A connection with id "
                        + connID + " is not handled by this workflow manager");
            }

            int oldSourceID = cc.getSource().getID();
            int oldTargetID = cc.getTarget().getID();

            // check if both (source and target node have been selected
            // if not, the connection is omitted
            if (!idMap.containsKey(oldSourceID)
                    || !idMap.containsKey(oldTargetID)) {
                continue;
            }

            ConnectionContainer newConn =
                    new ConnectionContainer(m_runningConnectionID.inc(),
                            m_nodesByID.get(idMap.get(oldSourceID)), cc
                                    .getSourcePortID(), m_nodesByID.get(idMap
                                    .get(oldTargetID)), cc.getTargetPortID());
            addConnection(newConn);
            // add the id to the new ids
            newConnectionIDs.add(newConn.getID());
        }

        int[] nids = new int[newNodeIDs.size()];
        int i = 0;
        for (Integer newId : newNodeIDs) {
            nids[i++] = newId;
        }

        int[] cids = new int[newConnectionIDs.size()];
        i = 0;
        for (Integer newId : newConnectionIDs) {
            cids[i++] = newId;
        }

        return new int[][]{nids, cids};
    }

    /**
     * Creates additional nodes and optional connections between those specified
     * in the settings object.
     * 
     * @param settings the <code>NodeSettings</code> object describing the sub
     *            workflow to add to this workflow manager
     * 
     * @return the ids of the newly created containers, the first array element
     *         being the array of node container ids, the second being the array
     *         of connection containers
     * 
     * @throws InvalidSettingsException thrown if the passed settings are not
     *             valid
     * @throws ClassNotFoundException if a node class cannot be found
     * @throws IllegalAccessException if a node class is not accessible
     * @throws InstantiationException if a node cannot be instantiated
     */
    public int[][] createSubWorkflow(final NodeSettings settings)
            throws InvalidSettingsException, InstantiationException,
            IllegalAccessException, ClassNotFoundException {
        NodeSettings nodes = settings.getNodeSettings(KEY_NODES);

        ArrayList<Integer> newNodeIDs = new ArrayList<Integer>();
        ArrayList<Integer> newConnectionIDs = new ArrayList<Integer>();

        // the map is used to map the old node id to the new one
        Map<Integer, Integer> idMap = new HashMap<Integer, Integer>();

        for (String nodeKey : nodes.keySet()) {
            NodeSettings nodeSetting = new NodeSettings(nodeKey);
            try {
                // retrieve config object for each node
                nodes.getNodeSettings(nodeKey).copyTo(nodeSetting);
                // create NodeContainer based on NodeSettings object

                // remember temporarily the old id
                final int oldId = nodeSetting.getInt(NodeContainer.KEY_ID);
                final int newId = m_runningNodeID.inc();
                nodeSetting.addInt(NodeContainer.KEY_ID, newId);
                final NodeContainer newNode =
                        new NodeContainer(nodeSetting, this);
                try {
                    newNode.loadSettings(nodeSetting);
                } catch (InvalidSettingsException ex) {
                    LOGGER.debug("Could not load settings for node " + nodeKey
                            + ": " + ex.getMessage());
                }

                // adapt custom name in case it has still the node id string
                String customName = newNode.getCustomName();
                if (customName.equals("Node " + oldId)) {
                    newNode.setCustomName("Node " + newId);
                }

                newNode.resetAndConfigure();

                // change the id, as this id is already in use (it was copied)
                // first remeber the old id "map(oldId, newId)"

                idMap.put(oldId, newId);
                // remember the new id for the return value
                newNodeIDs.add(newId);

                // and add it to workflow
                addNodeWithID(newNode);
            } catch (InvalidSettingsException ise) {
                LOGGER.warn("Could not create node " + nodeKey + " reason: "
                        + ise.getMessage());
                LOGGER.debug(nodeSetting, ise);
            }
        }
        // read connections
        NodeSettings connections = settings.getNodeSettings(KEY_CONNECTIONS);
        for (String connectionKey : connections.keySet()) {
            // retrieve config object for next connection
            NodeSettings connectionConfig =
                    connections.getNodeSettings(connectionKey);
            // and add appropriate connection to workflow
            try {
                // get the new id from the map
                // read ids and port indices
                int oldSourceID =
                        ConnectionContainer
                                .getSourceIdFromConfig(connectionConfig);
                int oldTargetID =
                        ConnectionContainer
                                .getTargetIdFromConfig(connectionConfig);

                // check if both (source and target node have been selected
                // if not, the connection is omitted
                if (idMap.get(oldSourceID) == null
                        || idMap.get(oldTargetID) == null) {
                    continue;
                }

                ConnectionContainer cc =
                        new ConnectionContainer(m_runningConnectionID.inc(),
                                connectionConfig, this, idMap);
                addConnection(cc);
                // add the id to the new ids
                newConnectionIDs.add(cc.getID());
            } catch (InvalidSettingsException ise) {
                LOGGER.warn("Could not create connection " + connectionKey
                        + " reason: " + ise.getMessage());
                LOGGER.debug(connectionConfig, ise);
            }
        }

        int[] nids = new int[newNodeIDs.size()];
        int i = 0;
        for (Integer newId : newNodeIDs) {
            nids[i++] = newId;
        }

        int[] cids = new int[newConnectionIDs.size()];
        i = 0;
        for (Integer newId : newConnectionIDs) {
            cids[i++] = newId;
        }

        return new int[][]{nids, cids};
    }

    /**
     * Removes all connections (incoming and outgoing) from a node container.
     * Note that this results in a bunch of workflow events !
     * 
     * @param nodeCont the container which should be completely disconnected
     * @throws WorkflowInExecutionException if the workflow is currently being
     *             executed
     */
    public synchronized void disconnectNodeContainer(
            final NodeContainer nodeCont) throws WorkflowInExecutionException {
        if (!canBeDeleted(nodeCont)) {
            throw new WorkflowInExecutionException(
                    "Node cannot be disconnected"
                            + ", because it is part of a running workflow.");
        }

        int numIn = nodeCont.getNrInPorts();
        int numOut = nodeCont.getNrOutPorts();

        List<ConnectionContainer> connections =
                new ArrayList<ConnectionContainer>();
        // collect incoming connections
        for (int i = 0; i < numIn; i++) {
            ConnectionContainer c = getIncomingConnectionAt(nodeCont, i);
            if (c != null) {
                connections.add(c);
            }
        }
        // collect outgoing connections
        for (int i = 0; i < numOut; i++) {
            List<ConnectionContainer> cArr =
                    getOutgoingConnectionsAt(nodeCont, i);
            if (cArr != null) {
                connections.addAll(cArr);
            }
        }

        // remove all collected connections
        for (ConnectionContainer container : connections) {
            removeConnection(container);
        }
    }

    /**
     * Executes all nodes in this workflow.
     * 
     * @param block <code>true</code> if the method should block until the
     *            execution has been finished
     */
    public void executeAll(final boolean block) {
        synchronized (this) {
            Collection<NodeContainer> nodes = new ArrayList<NodeContainer>();

            List<NodeContainer> topSortedNodes = topSortNodes();
            for (NodeContainer nc : topSortedNodes) {
                // we also add unconfigured nodes here because they may get
                // configured if their predecessor(s) are executed
                if (!nc.isExecuted() && nc.isFullyConnected()) {
                    nodes.add(nc);
                }
            }

            for (NodeContainer nc : topSortedNodes) {
                if (!nc.isFullyConnected()) {
                    nodes.removeAll(nc.getAllSuccessors());
                }
            }

            if (m_parent != null) {
                NodeContainer myNodeContainer = null;
                for (NodeContainer nc : m_parent.m_nodesByID.values()) {
                    if (nc.getEmbeddedWorkflowManager() == this) {
                        myNodeContainer = nc;
                        break;
                    }
                }
                assert (myNodeContainer != null) : "Did not find my "
                        + "node container";

                m_parent.findExecutableNodes(myNodeContainer, nodes);
            }

            m_executor.addWaitingNodes(nodes);
        }
        if (block) {
            m_executor.waitUntilFinished(this);
        }
    }

    /**
     * Executes the workflow up to and including the passed node. If desired the
     * method blocks until the execution has been finished.
     * 
     * @param nodeID the id of the node up to which the workflow should be
     *            executed
     * @param block <code>true</code> if the method should block,
     *            <code>false</code> otherwise
     * @throws IllegalArgumentException if the passed node is not configured or
     *             already executed
     */
    public void executeUpToNode(final int nodeID, final boolean block) {
        final NodeContainer nc;
        List<NodeContainer> nodes;
        synchronized (this) {
            nc = m_nodesByID.get(nodeID);
            if (!nc.isConfigured()) {
                throw new IllegalArgumentException("The given node is not"
                        + " configured and cannot be executed");
            }
            if (nc.isExecuted()) {
                throw new IllegalArgumentException("The given node is already"
                        + " executed");
            }

            nodes = new ArrayList<NodeContainer>();
            nodes.add(nc);
            findExecutableNodes(nc, nodes);
        }
        // queue the nodes in reverse order, i.e. the first executing nodes
        // gets queued first, so that its progress bar comes first
        Collections.reverse(nodes);

        m_executor.addWaitingNodes(nodes);
        if (block) {
            m_executor.waitUntilFinished(nc);
        }
    }

    /**
     * Executes exactly one node. The node must be in the executable state. No
     * predecessors will be executed in order to get this node into the
     * executable state. No auto-executable successor nodes will be executed
     * afterwards.
     * 
     * @param nodeID the id of the node to execute.
     * @param block <code>true</code> if the method should block,
     *            <code>false</code> otherwise
     * @throws IllegalArgumentException if the passed node is not configured or
     *             already executed
     */
    public void executeOneNode(final int nodeID, final boolean block) {
        NodeContainer nc;
        synchronized (this) {
            nc = m_nodesByID.get(nodeID);
            if (!nc.isExecutable()) {
                throw new IllegalArgumentException("The given node is not"
                        + " executable, thus can't be executed");
            }
            if (nc.isExecuted()) {
                throw new IllegalArgumentException("The given node is already"
                        + " executed");
            }

            List<NodeContainer> nodes = new LinkedList<NodeContainer>();
            nodes.add(nc);
            m_executor.addWaitingNodes(nodes);
        }
        if (block) {
            m_executor.waitUntilFinished(nc);
        }
    }

    /**
     * Searches for potentially executable nodes that are before the passed node
     * container in the flow.
     * 
     * @param beforeNode the node up to which (but not including) executable
     *            nodes should be searched
     * @param nodes a collection to which the executable nodes are added
     */
    private void findExecutableNodes(final NodeContainer beforeNode,
            final Collection<NodeContainer> nodes) {
        LinkedList<NodeContainer> pred = new LinkedList<NodeContainer>();
        pred.add(beforeNode);
        while (!pred.isEmpty()) {
            NodeContainer nodeCont = pred.removeFirst();
            for (NodeContainer nc : nodeCont.getPredecessors()) {
                if (nc.isConfigured() && !nc.isExecuted() 
                        && !pred.contains(nc)) {
                    pred.add(nc);
                    nodes.add(nc);
                }
            }

            // check for auto-excutable nodes
            LinkedList<NodeContainer> succ = new LinkedList<NodeContainer>();
            succ.add(nodeCont);

            while (succ.size() > 0) {
                NodeContainer nc = succ.removeFirst();
                for (NodeContainer[] nca : nc.getSuccessors()) {
                    for (NodeContainer autoexec : nca) {
                        if (autoexec.isAutoExecutable()
                                && autoexec.isConfigured()
                                && !autoexec.isExecuted()
                                && !nodes.contains(autoexec)) {
                            nodes.add(autoexec);
                            succ.add(autoexec);
                        }
                    }
                }
            }
        }

        if (m_parent != null) {
            NodeContainer myNodeContainer = null;
            for (NodeContainer nc : m_parent.m_nodesByID.values()) {
                if (nc.getEmbeddedWorkflowManager() == this) {
                    myNodeContainer = nc;
                    break;
                }
            }
            assert (myNodeContainer != null) : "Did not find my node container";

            m_parent.findExecutableNodes(myNodeContainer, nodes);
        }
    }

    /*
     * Notifes all registered listeners of the event.
     */
    private void fireWorkflowEvent(final WorkflowEvent event) {
        for (WorkflowListener l : m_eventListeners) {
            try {
                l.workflowChanged(event);
            } catch (Throwable t) {
                LOGGER.error("Exception while notifying workflow listeners", t);
            }
        }
    }

    /**
     * Returns the incoming connection that exist at some in-port on some node.
     * 
     * @param container a node in the workflow
     * @param portNum index of the in-port
     * @return the connection that is attached to the given in-port or
     *         <code>null</code> if no such connection exists
     * @throws IllegalArgumentException If either nodeID or portNum is invalid.
     */
    public ConnectionContainer getIncomingConnectionAt(
            final NodeContainer container, final int portNum) {
        if (container == null) {
            throw new NullPointerException("container must not be null");
        }
        // Find all outgoing connections for the given node
        for (ConnectionContainer conn : m_connectionsByID.values()) {
            // check if this connection affects the right node and port
            // if so, return the connection
            if ((conn.getTarget().equals(container))
                    && (conn.getTargetPortID() == portNum)) {
                return conn;
            }
        }

        return null;
    }

    /**
     * Returns the node container that is handled by the manager for the given
     * id.
     * 
     * @param id The id of the <code>Node</code> whose
     *            <code>NodeContainer</code> should be returned
     * @return The container that wraps the node of the given id
     */
    public NodeContainer getNodeContainerById(final int id) {
        NodeContainer cont = m_nodesByID.get(new Integer(id));
        return cont;
    }

    /**
     * Returns all nodes currently managed by this instance.
     * 
     * @return All the managed node containers.
     */
    public Collection<NodeContainer> getNodes() {
        return Collections.unmodifiableCollection(m_nodesByID.values());
    }

    /**
     * Returns the outgoing connections that exist at some out-port on some
     * node.
     * 
     * @param container The container in the workflow.
     * @param portNum Index of the out-port
     * @return Array containing the connection container objects that are
     *         associated to the given out-port on the node
     * @throws IllegalArgumentException If either nodeID or portNum is invalid.
     */
    public List<ConnectionContainer> getOutgoingConnectionsAt(
            final NodeContainer container, final int portNum) {
        List<ConnectionContainer> foundConnections =
                new ArrayList<ConnectionContainer>();

        // If the node is contained, process it
        if (container != null) {
            // Find all outgoing connections for the given node
            for (ConnectionContainer conn : m_connectionsByID.values()) {
                // check if this connection affects the right node and port
                if ((conn.getSource().equals(container))
                        && (conn.getSourcePortID() == portNum)) {
                    foundConnections.add(conn);
                }
            }
        } else {
            throw new IllegalArgumentException(
                    "The node is not contained in the workflow");
        }

        return foundConnections;
    }

    /**
     * Loads the complete workflow from the given file.
     * 
     * @param workflowFile the workflow file
     * @param progMon a node progres monitor for reporting progress
     * @throws IOException if the workflow file can not be found or files to
     *             load node internals
     * @throws InvalidSettingsException if settings cannot be read
     * @throws CanceledExecutionException if loading was canceled
     * @throws WorkflowInExecutionException if the workflow is currently being
     *             executed
     * @throws WorkflowException if an exception occurs while loading the
     *             workflow structure
     */
    public synchronized void load(final File workflowFile,
            final NodeProgressMonitor progMon) throws IOException,
            InvalidSettingsException, CanceledExecutionException,
            WorkflowInExecutionException, WorkflowException {
        checkForRunningNodes("Workflow cannot be loaded");

        if (!workflowFile.isFile()
                || !workflowFile.getName().equals(WORKFLOW_FILE)) {
            throw new IOException("File must be named: \"" + WORKFLOW_FILE
                    + "\": " + workflowFile);
        }

        // ==================================================================
        // FIXME The following lines and the ones in the finally-block
        // are just hacks to omit warnings messages during loading the flow.
        // When the WFM is redesigned we need a proper way to do this.
        if (m_parent == null) { // meta nodes must not do anything here!
            NodeLogger.setIgnoreConfigureWarning(true);
        }
        // ==================================================================
        try {
            // load workflow topology
            NodeSettingsRO settings =
                    NodeSettings.loadFromXML(new FileInputStream(workflowFile));
            if (settings.containsKey(CFG_VERSION)) {
                m_loadedVersion = settings.getString(CFG_VERSION);
                if (m_loadedVersion == null) {
                    throw new WorkflowException(
                        "Refuse to load workflow: Workflow version not available.");
                }
                // first version was only labeled with 1.0 instead of 1.0.0 
                if (m_loadedVersion.equals("1.0")) {
                    m_loadedVersion = "1.0.0";
                }
            } else {
                m_loadedVersion = "0.9.0"; // CeBIT 2006 version without version id
            }
            LOGGER.debug("Trying to parse version: " + m_loadedVersion);
            String[] versionStrArray = m_loadedVersion.split("\\.");
            int[] versionIntArray = new int[]{
                    KNIMEConstants.MAJOR, KNIMEConstants.MINOR, KNIMEConstants.REV};
            if (versionStrArray.length != versionIntArray.length) {
                throw new WorkflowException("Refuse to load workflow: Unknown"
                        + " workflow version \"" + m_loadedVersion + "\".");
            }
            for (int i = 0; i < versionIntArray.length; i++) {
                int value = -1;
                try {
                    value = Integer.parseInt(versionStrArray[i]);
                } catch (NumberFormatException nfe) {
                    throw new WorkflowException(
                            "Refuse to load workflow: Unknown workflow version "
                            + "\"" + m_loadedVersion + "\".");
                }
                if (value < versionIntArray[i]) {
                    break;
                } else if (value > versionIntArray[i]) {
                    throw new WorkflowException(
                            "Refuse to load workflow: "
                            + "The current KNIME version (" + KNIMEConstants.VERSION
                            + ") is older than the workflow (" + m_loadedVersion 
                            + ") you are trying to load.\n"
                            + "Please get a newer version of KNIME.");
                }
            }
            if (!KNIMEConstants.VERSION.equalsIgnoreCase(m_loadedVersion)) {
                if (m_parent == null) {
                    LOGGER.warn(
                            "The current KNIME version (" + KNIMEConstants.VERSION 
                            + ") is different from the one that created the"
                            + " workflow (" + m_loadedVersion 
                            + ") you are trying to load. In some rare cases, it"
                            + " might not be possible to load all data"
                            + " or some nodes can't be configured."
                            + " Please re-configure and/or re-execute these"
                            + " nodes.");
                }
            }
            
            try {
                load(settings);
            } finally {
    
                File parentDir = workflowFile.getParentFile();
        
                // data files are loaded using a repository of reference tables;
                // these lines serves to init the repository so nodes can put their data
                // into this map, the repository is deleted when the loading is done
        
                // meta workflows must use their grand*-parent editor's id
                // and only the grand-parent may initialize the repository with the id
                WorkflowManager wfm = this;
                while (wfm.m_parent != null) {
                    wfm = wfm.m_parent;
                }
                int loadID = System.identityHashCode(wfm);
                if (wfm == this) {
                    BufferedDataTable.initRepository(loadID);
                }
                ArrayList<NodeContainer> failedNodes = new ArrayList<NodeContainer>();
                // get all keys in there
                try {
                    double nodeCounter = 1.0;
                    ExecutionMonitor execMon = new ExecutionMonitor(progMon);
                    for (int i = 0; i < topSortNodes().size(); i++) {
                        NodeContainer newNode = topSortNodes().get(i);
                        execMon.checkCanceled();
                        execMon.setMessage("Loading node: " + newNode.getNameWithID());
                        try {
                            NodeSettingsRO nodeSetting =
                                    settings.getNodeSettings(KEY_NODES)
                                            .getNodeSettings("node_" + newNode.getID());
                            String nodeFileName =
                                    nodeSetting.getString(KEY_NODE_SETTINGS_FILE);
                            File nodeFile = new File(parentDir, nodeFileName);
                            NodeProgressMonitor subProgMon =
                                    execMon.createSubProgress(
                                            1.0 / topSortNodes().size())
                                            .getProgressMonitor();
                            newNode.load(loadID, nodeFile, subProgMon);
                        } catch (IOException ioe) {
                            String msg =
                                    "Unable to load node: " + newNode.getNameWithID()
                                            + " -> reset and configure.";
                            LOGGER.error(msg, ioe);
                            failedNodes.add(newNode);
                        } catch (InvalidSettingsException ise) {
                            String msg =
                                    "Unable to load node: " + newNode.getNameWithID()
                                            + " -> reset and configure.";
                            LOGGER.error(msg, ise);
                            failedNodes.add(newNode);
                        } catch (Throwable e) {
                            String msg =
                                    "Unable to load node: " + newNode.getNameWithID()
                                            + " -> reset and configure.";
                            LOGGER.error(msg, e);
                            failedNodes.add(newNode);
                        }
                        progMon.setProgress(nodeCounter / topSortNodes().size());
                        // progMon.setMessage("Prog: " + nodeCounter
                        // / topSortNodes().size());
                        nodeCounter += 1.0;
                    }
                } finally {
                    // put into a finally block because that may release much of memory
        
                    // only the wfm that create the repos may clear it, otherwise
                    // the meta workflow clears it and not-yet-loaded nodes
                    // in the parent cannot be loaded
                    if (wfm == this) {
                        BufferedDataTable.clearRepository(loadID);
                    }
                }
                for (NodeContainer newNode : failedNodes) {
                    resetAndConfigureNode(newNode.getID());
                }
            }
        } finally {
            // ===============================================================
            // FIXME The following linesand the one line above are just hacks
            // to omit warnings messages during loading the flow.
            // When the WFM is redesigned we need a proper way to do this.
            if (m_parent == null) { // meta nodes must not do anything here!
                NodeLogger.setIgnoreConfigureWarning(false);
            }
            // ===============================================================
        }
    }

    /**
     * Only load internal workflow manager settings, init nodes and connections.
     * No NodeSettings, DataTableSpec, DataTable, or ModelContent are loaded.
     * 
     * @param settings read settings from
     * @throws InvalidSettingsException if an error occurs during reading
     * @throws WorkflowException if an exception occurs while loading the
     *             workflow
     */
    private void load(final NodeSettingsRO settings)
            throws InvalidSettingsException, WorkflowException {
        // read running ids for new nodes and connections
        if (m_parent == null) {
            m_runningNodeID.setValue(settings.getInt(KEY_RUNNING_NODE_ID));
            m_runningConnectionID
                    .setValue(settings.getInt(KEY_RUNNING_CONN_ID));
        }

        final WorkflowException workflowException =
                new WorkflowException("Error while loading workflow: ");
        WorkflowException lastEx = workflowException;

        // Node-Subconfig
        NodeSettingsRO nodes = settings.getNodeSettings(KEY_NODES);
        // get all keys in there
        for (String nodeKey : nodes.keySet()) {
            NodeSettingsRO nodeSetting = null;
            // retrieve config object for each node
            nodeSetting = nodes.getNodeSettings(nodeKey);
            // create NodeContainer based on NodeSettings object

            try {
                NodeContainer newNode = new NodeContainer(nodeSetting, this);
                addNodeWithID(newNode);
            } catch (InstantiationException ex) {
                lastEx =
                        new WorkflowException("Error while loading node: "
                                + ex.getMessage(), lastEx, ex);
                LOGGER.error("Could not create factory object of type "
                        + nodeSetting.getString(NodeContainer.KEY_FACTORY_NAME,
                                "??") + " for node " + nodeKey, ex);
            } catch (IllegalAccessException ex) {
                lastEx =
                        new WorkflowException("Error while loading node: "
                                + ex.getMessage(), lastEx, ex);
                LOGGER.error("Could not access factory class "
                        + nodeSetting.getString(NodeContainer.KEY_FACTORY_NAME,
                                "??") + " for node " + nodeKey, ex);
            } catch (ClassNotFoundException ex) {
                lastEx =
                        new WorkflowException("Error while loading node: "
                                + ex.getMessage(), lastEx, ex);
                LOGGER.error("Could not find factory class "
                        + nodeSetting.getString(NodeContainer.KEY_FACTORY_NAME,
                                "??") + " for node " + nodeKey, ex);
            } catch (Throwable t) {
                LOGGER.error(t.getMessage(), t);
                lastEx =
                        new WorkflowException("Error while loading node: "
                                + t.getMessage(), lastEx, t);
            }
        }

        // read connections
        NodeSettingsRO connections = settings.getNodeSettings(KEY_CONNECTIONS);
        for (String connectionKey : connections.keySet()) {
            // retrieve config object for next connection
            NodeSettingsRO connectionConfig =
                    connections.getNodeSettings(connectionKey);
            // and add appropriate connection to workflow
            ConnectionContainer cc = null;
            try {
                cc =
                        new ConnectionContainer(m_runningConnectionID.inc(),
                                connectionConfig, this);
                addConnection(cc);
            } catch (Exception ex) {
                lastEx =
                        new WorkflowException("Error while adding connection: ",
                                lastEx, ex);
                LOGGER.error("Could not create connection: " + connectionKey
                        + " reason: " + ex.getMessage());
                LOGGER.debug(connectionConfig, ex);
            }
        }

        if (lastEx != workflowException) {
            throw workflowException;
        }
    }

    /**
     * Deletes a connection between two nodes.
     * 
     * @param connection to be deleted
     * @throws WorkflowInExecutionException if the workflow is currently being
     *             executed
     */
    public synchronized void removeConnection(
            final ConnectionContainer connection)
            throws WorkflowInExecutionException {
        if (!canBeDeleted(connection.getTarget())) {
            throw new WorkflowInExecutionException("Connection cannot be "
                    + "removed, because it is part of a running workflow.");
        }

        // if connection does not exist simply return
        if (!(m_connectionsByID.containsKey(connection.getID()))) {
            return;
        }

        // retrieve source and target node
        NodeContainer sourceNode = connection.getSource();
        int portOut = connection.getSourcePortID();
        NodeContainer targetNode = connection.getTarget();
        int portIn = connection.getTargetPortID();
        // remove outgoing edge
        sourceNode.removeOutgoingConnection(portOut, targetNode);
        // remove incoming edge
        targetNode.removeIncomingConnection(portIn);

        // cancel the disconnected node and all its sucessors
        // (this will only remove them from the queue as they are not executed;
        // this is caught in checkForRunningNodes)
        ArrayList<NodeContainer> cancelNodes = new ArrayList<NodeContainer>();
        cancelNodes.add(targetNode);
        cancelNodes.addAll(targetNode.getAllSuccessors());
        m_executor.cancelExecution(cancelNodes);

        // also disconnect the two underlying Nodes.
        targetNode.disconnectPort(portIn);
        // finally remove connection from internal list
        m_connectionsByID.remove(connection.getID());

        resetAndConfigureNode(targetNode.getID());

        // notify listeners
        LOGGER.info("Removed connection (from node id:" + sourceNode.getID()
                + ", port:" + portOut + " to node id:" + targetNode.getID()
                + ", port:" + portIn + ")");
        fireWorkflowEvent(new WorkflowEvent.ConnectionRemoved(-1, connection,
                null));
    }

    /**
     * Removes a listener from the worklflow, has no effekt if the listener was
     * not registered before.
     * 
     * @param listener The listener to remove
     */
    public void removeListener(final WorkflowListener listener) {
        m_eventListeners.remove(listener);
    }

    /**
     * Removes a node from the workflow including all its connections.
     * 
     * @param container node to be removed
     * @throws WorkflowInExecutionException if the workflow is currently being
     *             executed
     */
    public synchronized void removeNode(final NodeContainer container)
            throws WorkflowInExecutionException {
        if (!canBeDeleted(container)) {
            throw new WorkflowInExecutionException("Node cannot be removed, "
                    + "because it is part of a running workflow.");
        }

        Integer id = m_idsByNode.get(container);
        if (id != null) {
            // tell node that it has been disconnected (close views...)
            try {
                container.detach();
                resetAndConfigureAfterNode(id);
                disconnectNodeContainer(container);
                m_detachedNodes.add(container);
            } catch (Exception ex) {
                LOGGER.error("Error while removing node: ", ex);
            }

            container.removeAllListeners();

            m_nodesByID.remove(id);
            m_idsByNode.remove(container);

            LOGGER.debug("Removed: " + container.getNameWithID());
            fireWorkflowEvent(
                    new WorkflowEvent.NodeRemoved(id, container, null));
        } else {
            LOGGER.error("Could not find (and remove): " + container);
            throw new IllegalArgumentException(
                    "Node not managed by this workflow: " + container);
        }
    }

    /**
     * Resets and configures all nodes after the passed node.
     * 
     * @param nodeID the node id
     * @throws WorkflowInExecutionException if the workflow is currently
     *             executing
     */
    public synchronized void resetAndConfigureAfterNode(final int nodeID)
            throws WorkflowInExecutionException {
        NodeContainer nodeCont = m_nodesByID.get(nodeID);
        if (!canBeReset(nodeCont)) {
            throw new WorkflowInExecutionException("Node cannot be reset, "
                    + "because it is part of a running workflow.");
        }

        for (NodeContainer nc : nodeCont.getAllSuccessors()) {
            nc.resetAndConfigure();
        }
    }

    /**
     * Resets and configures all nodes.
     * 
     * @throws WorkflowInExecutionException if the workflow is currently
     *             executing
     */
    public synchronized void resetAndConfigureAll()
            throws WorkflowInExecutionException {
        checkForRunningNodes("Nodes cannot be reset");

        for (NodeContainer nc : m_nodesByID.values()) {
            nc.resetAndConfigure();
        }
        if (!m_tableRepository.isEmpty()) {
            LOGGER.debug("Table repository is not empty after workflow " 
                    + "is reset (" + m_tableRepository.size() + " elements)");
        }
    }

    /**
     * Resets and configures the passed node and all its sucessors.
     * 
     * @param nodeID the node id
     * @throws WorkflowInExecutionException if the workflow is currently
     *             executing
     */
    public synchronized void resetAndConfigureNode(final int nodeID)
            throws WorkflowInExecutionException {
        NodeContainer nodeCont = m_nodesByID.get(nodeID);
        if (!canBeReset(nodeCont)) {
            throw new WorkflowInExecutionException("Node cannot be reset, "
                    + "because it is part of a running workflow.");
        }

        nodeCont.resetAndConfigure();
        for (NodeContainer nc : nodeCont.getAllSuccessors()) {
            nc.resetAndConfigure();
        }
    }

    /**
     * Saves this workflow manager settings including nodes and connections into
     * the given file. In additon, all nodes' internal structures are stored -
     * if available, depending on the current node state, reset, configured, or
     * executed. For each node a directory is created (at the workflow file's
     * parent path) to save the node internals.
     * 
     * @param workflowFile To write workflow manager settings to.
     * @param progMon The monitor for the workflow saving progress.
     * @throws IOException If the workflow file can't be found.
     * @throws CanceledExecutionException If the saving process has been
     *             canceled.
     * @throws WorkflowInExecutionException if the workflow is currently being
     *             executed
     */
    public synchronized void save(final File workflowFile,
            final NodeProgressMonitor progMon) throws IOException,
            CanceledExecutionException, WorkflowInExecutionException {
        checkForRunningNodes("Workflow cannot be saved");

        if (workflowFile.isDirectory()
                || !workflowFile.getName().equals(WORKFLOW_FILE)) {
            throw new IOException("File must be named: \"" + WORKFLOW_FILE
                    + "\": " + workflowFile);
        }

        // remove internals of all detached nodes first
        for (NodeContainer cont : m_detachedNodes) {
            cont.removeInternals();
        }
        m_detachedNodes.clear();

        File parentDir = workflowFile.getParentFile();
        // workflow settings
        NodeSettings settings = new NodeSettings(WORKFLOW_FILE);
        // save workflow version
        settings.addString(CFG_VERSION, KNIMEConstants.VERSION);
        // save current running id
        settings.addInt(KEY_RUNNING_NODE_ID, m_runningNodeID.intValue());
        // save current connection id
        settings.addInt(KEY_RUNNING_CONN_ID, m_runningConnectionID.intValue());
        // save nodes in an own sub-config object as a series of configs
        NodeSettingsWO nodes = settings.addNodeSettings(KEY_NODES);
        int nodeNum = 0;

        ExecutionMonitor execMon = new ExecutionMonitor(progMon);
        for (NodeContainer nextNode : m_nodesByID.values()) {

            progMon.setMessage("Saving node: " + nextNode.getNameWithID());
            // create node directory based on the nodes name and id
            // all chars which are not letter or number are replaced by '_'
            String nodeDirID =
                    nextNode.getName().replaceAll("[^a-zA-Z0-9 ]", "_") + " (#"
                            + nextNode.getID() + ")";
            // and save it to it's own config object
            NodeSettingsWO nextNodeConfig =
                    nodes.addNodeSettings("node_" + nextNode.getID());
            String nodeFileName = nodeDirID + "/" + Node.SETTINGS_FILE_NAME;
            nextNodeConfig.addString(KEY_NODE_SETTINGS_FILE, nodeFileName);
            File nodeFile = new File(parentDir, nodeFileName);
            File nodeDir = nodeFile.getParentFile();
            if (!nodeDir.isDirectory() && !nodeDir.mkdir()) {
                throw new IOException("Unable to create dir: " + nodeDir);
            }

            NodeProgressMonitor subProgMon =
                    execMon.createSubProgress(1.0 / m_nodesByID.size())
                            .getProgressMonitor();
            nextNode.save(nextNodeConfig, nodeFile, subProgMon);
            nodeNum++;
            progMon.setProgress((double)nodeNum / m_nodesByID.size());
        }

        NodeSettingsWO connections = settings.addNodeSettings(KEY_CONNECTIONS);
        for (ConnectionContainer cc : m_connectionsByID.values()) {
            // and save it to it's own config object
            NodeSettingsWO nextConnectionConfig =
                    connections.addNodeSettings("connection_" + cc.getID());
            cc.save(nextConnectionConfig);
        }
        settings.saveToXML(new FileOutputStream(workflowFile));
    }

    private void topSortHelp(final Collection<NodeContainer> current,
            final ArrayList<NodeContainer> result) {
        for (NodeContainer con : current) {
            if (con != null) {
                Collection<NodeContainer> pred = con.getPredecessors();
                for (NodeContainer preCon : pred) {
                    if (preCon != null && !result.contains(preCon)) {
                        NodeContainer[][] succ = preCon.getSuccessors();
                        boolean notContained = true;
                        for (int i = 0; i < succ.length; i++) {
                            for (int j = 0; j < succ[i].length; j++) {
                                if (!result.contains(succ[i][j])) {
                                    notContained = false;
                                }
                            }
                        }
                        if (notContained) {
                            result.add(0, preCon);
                        }
                    }
                }
            }
        }
        for (NodeContainer con : current) {
            if (con != null) {
                topSortHelp(con.getPredecessors(), result);
            }
        }
    }

    /* Topological sorting of all nodes inthe workflow */
    private List<NodeContainer> topSortNodes() {
        Collection<NodeContainer> termList = new ArrayList<NodeContainer>();
        for (Integer nodeKey : m_nodesByID.keySet()) {
            NodeContainer newNode = m_nodesByID.get(nodeKey);
            NodeContainer[][] succ = newNode.getSuccessors();
            boolean term = true;
            for (int i = 0; i < succ.length; i++) {
                if (succ[i].length > 0) {
                    term = false;
                    break;
                }
            }
            if (term) {
                termList.add(newNode);
            }
        }
        ArrayList<NodeContainer> list = new ArrayList<NodeContainer>(termList);
        topSortHelp(termList, list);
        return list;
    }

    /**
     * Waits until the execution in this workflow has been finished.
     */
    public void waitUntilFinished() {
        m_executor.waitUntilFinished(this);
    }

    /**
     * Callback for Workflow events.
     * 
     * @param event The thrown event
     */
    public void workflowChanged(final WorkflowEvent event) {
        if (event instanceof WorkflowEvent.ConnectionExtrainfoChanged) {
            // just forward the event
            fireWorkflowEvent(event);
        }
    }

    /**
     * Loads the settings from the passed node container's dialog into its
     * model, resets and configures this node and all its sucessors.
     * 
     * @param nodeCont a node container
     * @throws WorkflowInExecutionException if the settings cannot be applied
     *             because the workflow is in execution
     * @throws InvalidSettingsException if the settings are invalid
     */
    synchronized void applyDialogSettings(final NodeContainer nodeCont)
            throws WorkflowInExecutionException, InvalidSettingsException {
        if (!canBeReset(nodeCont)) {
            throw new WorkflowInExecutionException(
                    "Dialog settings cannot be applied, because the node is "
                            + "part of a running workflow.");
        }

        nodeCont.loadModelSettingsFromDialog();
        nodeCont.resetAndConfigure();
        for (NodeContainer nc : nodeCont.getAllSuccessors()) {
            nc.resetAndConfigure();
        }
    }

    /**
     * Checks if at least one node is executing.
     * 
     * @return <code>true</code> if at least one node is executing,
     *         <code>false</code> otherwise
     */
    public boolean executionInProgress() {
        return m_executor.executionInProgress(this);
    }

    /**
     * Shuts the workflow manager down, i.e. stops all nodes, closes all views
     * and cleans up all temporary data.
     */
    public void shutdown() {
        m_executor.cancelExecution();

        Iterator<Map.Entry<NodeContainer, Integer>> nodeContaierEntries =
                m_idsByNode.entrySet().iterator();

        while (nodeContaierEntries.hasNext()) {
            Map.Entry<NodeContainer, Integer> entry =
                    nodeContaierEntries.next();

            entry.getKey().closeAllViews();
            entry.getKey().closeAllPortViews();
        }

        List<NodeContainer> sortedNodes = topSortNodes();

        for (int i = sortedNodes.size() - 1; i >= 0; i--) {
            sortedNodes.get(i).cleanup();
        }
    }

    /**
     * Returns if the given node is currently queued and waiting for execution.
     * 
     * @param cont any node container
     * @return <code>true</code> if the node is queued, <code>false</code>
     *         otherwise
     */
    public boolean isQueued(final NodeContainer cont) {
        return m_executor.isQueued(cont);
    }

    /**
     * @return The bufferRepositoryMap associated with this workflow. This
     * is used for blob (de)serialization.
     */
    HashMap<Integer, ContainerTable> getTableRepository() {
        return m_tableRepository;
    }
}
