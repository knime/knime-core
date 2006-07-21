/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   13.02.2005 (M. Berthold): created
 */
package de.unikn.knime.core.node.workflow;

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

import de.unikn.knime.core.node.BufferedDataTable;
import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.DefaultNodeProgressMonitor;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.Node;
import de.unikn.knime.core.node.NodeFactory;
import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.NodeProgressMonitor;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.NodeSettingsRO;
import de.unikn.knime.core.node.NodeSettingsWO;
import de.unikn.knime.core.node.NodeStateListener;
import de.unikn.knime.core.node.NodeStatus;
import de.unikn.knime.core.util.MutableInteger;

/**
 * Manager for a workflow holding Nodes and the connecting edge information. The
 * information is stored in a graph based data structure and allows to access
 * predecessors and successors. For performance reasons this implementation is
 * specific to vertices being of type <code>de.unikn.knime.dev.node.Node</code>
 * and (directed) edges connecting ports indicated by indices.
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
    private class WorkflowExecutor implements NodeStateListener {
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
                            fireWorkflowEvent(new WorkflowEvent.NodeFinished(
                                    succ.getID(), null, null));
                        }
                    }
                }

            }
        }

        private final Map<NodeContainer, NodeProgressMonitor> m_runningNodes = new HashMap<NodeContainer, NodeProgressMonitor>(),
                m_waitingNodes = new HashMap<NodeContainer, NodeProgressMonitor>();

        /**
         * Adds new nodes to the list of nodes that are waiting for execution.
         * 
         * @param nodes a list of nodes that should be executed
         */
        public void addWaitingNodes(final Collection<NodeContainer> nodes) {
            synchronized (m_waitingNodes) {
                boolean change = false;
                for (final NodeContainer nc : nodes) {
                    if (!m_waitingNodes.containsKey(nc)
                            && !m_runningNodes.containsKey(nc)) {
                        MyNodePM pm = new MyNodePM(nc);
                        m_waitingNodes.put(nc, pm);
                        fireWorkflowEvent(new WorkflowEvent.NodeWaiting(nc
                                .getID(), nc, pm));

                        change = true;
                    }
                }
                if (change) {
                    startNewNodes();
                }
            }
        }

        /**
         * Cancel execution of all nodes. Waiting nodes are just removed from
         * the waiting list, running nodes are sent a signal (via the progress
         * monitor) that they should terminate.
         */
        public void cancelExecution() {
            synchronized (m_waitingNodes) {
                for (NodeContainer nc : m_waitingNodes.keySet()) {
                    fireWorkflowEvent(new WorkflowEvent.NodeFinished(
                            nc.getID(), null, null));
                }

                m_waitingNodes.clear();
            }
            for (NodeProgressMonitor pm : m_runningNodes.values()) {
                pm.setExecuteCanceled();
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
            synchronized (m_waitingNodes) {
                Set<NodeContainer> cancelNodes = new HashSet<NodeContainer>();
                cancelNodes.addAll(nodes);

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

                for (NodeContainer nc : cancelNodes) {
                    if (m_waitingNodes.remove(nc) != null) {
                        fireWorkflowEvent(new WorkflowEvent.NodeFinished(nc
                                .getID(), null, null));
                    }
                }
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
            synchronized (m_waitingNodes) {
                // check if any of the nodes in the lists are from the
                // passed WFM

                for (NodeContainer nc : m_runningNodes.keySet()) {
                    if (wfm.m_nodesByID.values().contains(nc)) {

                        return true;
                    }
                }

                for (NodeContainer nc : m_waitingNodes.keySet()) {
                    if (wfm.m_nodesByID.values().contains(nc)) {
                        return true;
                    }
                }

                for (WeakReference<WorkflowManager> wr : wfm.m_children) {
                    if (wr.get() != null) {
                        if (executionInProgress(wr.get())) {
                            return true;
                        }
                    }
                }
                return false;
            }
        }

        /**
         * Checks if new nodes are executable now and if so, starts them.
         * Threads waiting on m_waitingNodes are awakened if no nodes are
         * running any more and no new executable node has been found.
         */
        private void startNewNodes() {
            synchronized (m_waitingNodes) {
                Iterator<Map.Entry<NodeContainer, NodeProgressMonitor>> it = m_waitingNodes
                        .entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<NodeContainer, NodeProgressMonitor> e = it.next();
                    if (e.getKey().isExecutable()) {
                        it.remove();
                        NodeContainer nc = e.getKey();
                        NodeProgressMonitor pm = e.getValue();

                        m_runningNodes.put(nc, pm);

                        try {
                            fireWorkflowEvent(new WorkflowEvent.NodeStarted(nc
                                    .getID(), nc, pm));
                            nc.addListener(this);
                            nc.startExecution(pm);
                        } catch (Exception ex) {
                            // remove from running nodes due to an error
                            m_runningNodes.remove(nc);
                        }

                    } else if (e.getKey().isExecuted()) {
                        it.remove();
                    }
                }

                if (m_runningNodes.size() == 0) {
                    if (m_waitingNodes.size() > 0) {
                        LOGGER.warn("Some nodes were still waiting but none is"
                                + " running: " + m_waitingNodes);
                    }

                    for (NodeContainer nc : m_waitingNodes.keySet()) {
                        fireWorkflowEvent(new WorkflowEvent.NodeFinished(nc
                                .getID(), null, null));
                    }
                    m_waitingNodes.clear();

                    m_waitingNodes.notifyAll();
                }
            }
        }

        /**
         * @see de.unikn.knime.core.node.NodeStateListener
         *      #stateChanged(de.unikn.knime.core.node.NodeStatus, int)
         */
        public void stateChanged(final NodeStatus state, final int id) {
            if ((state instanceof NodeStatus.EndExecute)
                    || (state instanceof NodeStatus.ExecutionCanceled)) {
                synchronized (m_waitingNodes) {
                    Iterator<Map.Entry<NodeContainer, NodeProgressMonitor>> it = m_runningNodes
                            .entrySet().iterator();
                    while (it.hasNext()) {
                        NodeContainer nc = it.next().getKey();
                        if (nc.getID() == id) {
                            nc.removeListener(this);
                            it.remove();

                            fireWorkflowEvent(new WorkflowEvent.NodeFinished(nc
                                    .getID(), null, null));
                            if (state instanceof NodeStatus.ExecutionCanceled) {
                                cancelExecution(nc.getAllSuccessors());
                            }
                        }
                    }

                    startNewNodes();
                    m_waitingNodes.notifyAll();
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
            synchronized (m_waitingNodes) {
                while (m_runningNodes.size() > 0) {
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
                            m_waitingNodes.wait();
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
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(WorkflowManager.class);

    /**
     * Identifier for KNIME workflows.
     */
    public static final String WORKFLOW_FILE = "workflow.knime";

    private final List<WeakReference<WorkflowManager>> m_children = new ArrayList<WeakReference<WorkflowManager>>();

    // quick access to connections
    private final Map<Integer, ConnectionContainer> m_connectionsByID = new HashMap<Integer, ConnectionContainer>();

    // change listener support (transient) <= why? (tm)
    private final transient ArrayList<WorkflowListener> m_eventListeners = new ArrayList<WorkflowListener>();

    private final WorkflowExecutor m_executor;

    // quick access to IDs via Nodes
    private final Map<NodeContainer, Integer> m_idsByNode = new HashMap<NodeContainer, Integer>();

    // quick access to nodes by ID
    private final Map<Integer, NodeContainer> m_nodesByID = new LinkedHashMap<Integer, NodeContainer>();

    private final MyNodeStateListener m_nodeStateListener = new MyNodeStateListener();

    private final WorkflowManager m_parent;

    private final MutableInteger m_runningConnectionID;

    // internal variables to allow generation of unique indices
    private final MutableInteger m_runningNodeID;

    /**
     * Create new Workflow.
     */
    public WorkflowManager() {
        m_parent = null;
        m_executor = new WorkflowExecutor();
        m_runningConnectionID = new MutableInteger(-1);
        m_runningNodeID = new MutableInteger(0);
    }

    /**
     * Create new WorkflowManager by a given <code>WORKFLOW_FILE</code>. All
     * nodes and connection are initialzied, and - if available -
     * <code>NodeSettings</code>, <code>DataTableSpec</code>,
     * <code>DataTable</code>, and <code>ModelContent</code> are loaded
     * into each node.
     * 
     * @param workflowFile the location of the workflow file,
     *            <code>WORKFLOW_FILE</code>
     * @throws InvalidSettingsException if settings cannot be read
     * @throws CanceledExecutionException if loading was canceled
     * @throws IOException if the workflow file can not be found or files to
     *             load node internals
     */
    public WorkflowManager(final File workflowFile)
            throws InvalidSettingsException, CanceledExecutionException,
            IOException {
        this();
        try {
            load(workflowFile);
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

        ConnectionContainer newConnection = new ConnectionContainer(
                m_runningConnectionID.inc(), nodeOut, portOut, nodeIn, portIn);
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
        fireWorkflowEvent(new WorkflowEvent.NodeAdded(newNodeID, null, newNode));

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
     */
    public boolean canAddConnection(final int sourceNode, final int outPort,
            final int targetNode, final int inPort) {
        if ((sourceNode < 0) || (outPort < 0) || (targetNode < 0)
                || (inPort < 0)) {
            // easy sanity check failed - return false;
            return false;
        }

        NodeContainer src = m_nodesByID.get(sourceNode);
        NodeContainer targ = m_nodesByID.get(targetNode);

        boolean nodesValid = (src != null) && (targ != null);
        if (!nodesValid) {
            // Nodes don't exist (whyever) - return failure
            LOGGER.error("WFM: checking for connection between non existing"
                    + " nodes!");
            return false;
        }

        boolean portNumsValid = (src.getNrOutPorts() > outPort)
                && (targ.getNrInPorts() > inPort) && (outPort >= 0)
                && (inPort >= 0);
        if (!portNumsValid) {
            // port numbers don't exist - return failure
            LOGGER.error("WFM: checking for connection for non existing"
                    + " ports!");
            return false;
        }

        ConnectionContainer conn = getIncomingConnectionAt(targ, inPort);
        boolean hasConnection = (conn != null);
        if (hasConnection) {
            // input port already has a connection - return failure
            return false;
        }

        boolean isDataConn = targ.isDataInPort(inPort)
                && src.isDataOutPort(outPort);
        boolean isModelConn = !targ.isDataInPort(inPort)
                && !src.isDataOutPort(outPort);
        if (!isDataConn && !isModelConn) {
            // trying to connect data to model port - return failure
            return false;
        }

        boolean loop = targ.isFollowedBy(src);
        // if (loop) {
        // LOGGER.warn("Attempt to create loop (from node id:" + srcC.getID()
        // + ", port:" + inPort + " to node id:" + targC.getID()
        // + ", port:" + outPort + ")");
        // }
        return !loop;
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

    /*
     * check sif the given node can be reset
     */
    private void checkForRunningNodes(final String msg, final NodeContainer nc)
            throws WorkflowInExecutionException {
        if (!m_executor.canBeReset(nc)) {
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

        List<NodeContainer> containers = new ArrayList<NodeContainer>(
                m_nodesByID.values());
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

            ConnectionContainer newConn = new ConnectionContainer(
                    m_runningConnectionID.inc(), m_nodesByID.get(idMap
                            .get(oldSourceID)), cc.getSourcePortID(),
                    m_nodesByID.get(idMap.get(oldTargetID)), cc
                            .getTargetPortID());
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
            NodeSettings nodeSetting = null;
            try {
                // retrieve config object for each node
                nodeSetting = nodes.getNodeSettings(nodeKey);
                // create NodeContainer based on NodeSettings object

                // remember temporarily the old id
                final int oldId = nodeSetting.getInt(NodeContainer.KEY_ID);
                final int newId = m_runningNodeID.inc();
                nodeSetting.addInt(NodeContainer.KEY_ID, newId);
                final NodeContainer newNode = new NodeContainer(nodeSetting,
                        this);
                newNode.loadSettings(nodeSetting);
                newNode.resetAndConfigure();

                // change the id, as this id is already in use (it was copied)
                // first remeber the old id "map(oldId, newId)"

                idMap.put(oldId, newId);
                // remember the new id for the return value
                newNodeIDs.add(newId);

                // set the user name to the new id if the init name
                // was set before e.g. "Node_44"
                // get set username
                String currentUserNodeName = newNode.getCustomName();

                // create temprarily the init user name of the copied node
                // to check wether the current name was changed
                String oldInitName = "Node " + (oldId + 1);
                if (oldInitName.equals(currentUserNodeName)) {
                    newNode.setCustomName("Node " + (newId + 1));
                }

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
            NodeSettings connectionConfig = connections
                    .getNodeSettings(connectionKey);
            // and add appropriate connection to workflow
            try {
                // get the new id from the map
                // read ids and port indices
                int oldSourceID = ConnectionContainer
                        .getSourceIdFromConfig(connectionConfig);
                int oldTargetID = ConnectionContainer
                        .getTargetIdFromConfig(connectionConfig);

                // check if both (source and target node have been selected
                // if not, the connection is omitted
                if (idMap.get(oldSourceID) == null
                        || idMap.get(oldTargetID) == null) {
                    continue;
                }

                ConnectionContainer cc = new ConnectionContainer(
                        m_runningConnectionID.inc(), connectionConfig, this,
                        idMap);
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
        checkForRunningNodes("Node cannot be disconnected");

        int numIn = nodeCont.getNrInPorts();
        int numOut = nodeCont.getNrOutPorts();

        List<ConnectionContainer> connections = new ArrayList<ConnectionContainer>();
        // collect incoming connections
        for (int i = 0; i < numIn; i++) {
            ConnectionContainer c = getIncomingConnectionAt(nodeCont, i);
            if (c != null) {
                connections.add(c);
            }
        }
        // collect outgoing connections
        for (int i = 0; i < numOut; i++) {
            List<ConnectionContainer> cArr = getOutgoingConnectionsAt(nodeCont,
                    i);
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

            for (NodeContainer nc : topSortNodes()) {
                // we also add unconfigured nodes here because they may get
                // configured if their predecessor(s) are executed
                if (!nc.isExecuted() && nc.isFullyConnected()) {
                    nodes.add(nc);
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
        synchronized (this) {
            NodeContainer nc = m_nodesByID.get(nodeID);
            if (!nc.isConfigured()) {
                throw new IllegalArgumentException("The given node is not"
                        + " configured and cannot be executed");
            }
            if (nc.isExecuted()) {
                throw new IllegalArgumentException("The given node is already"
                        + " executed");
            }

            List<NodeContainer> nodes = new ArrayList<NodeContainer>();
            nodes.add(nc);
            findExecutableNodes(nc, nodes);

            // queue the nodes in reverse order, i.e. the first executing nodes
            // gets queued first, so that its progress bar comes first
            Collections.reverse(nodes);
            m_executor.addWaitingNodes(nodes);
        }
        if (block) {
            m_executor.waitUntilFinished(this);
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
                if (nc.isConfigured() && !nc.isExecuted() && !pred.contains(nc)) {
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
    @SuppressWarnings("unchecked")
    private void fireWorkflowEvent(final WorkflowEvent event) {
        // we make a copy here because a listener can add or remove
        // itself or another listener during handling the event
        // this will then cause a ConcurrentModificationException
        ArrayList<WorkflowListener> temp = (ArrayList<WorkflowListener>)m_eventListeners
                .clone();
        for (WorkflowListener l : temp) {
            l.workflowChanged(event);
        }

        // if (m_parent != null) {
        // m_parent.fireWorkflowEvent(event);
        // }
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
        List<ConnectionContainer> foundConnections = new ArrayList<ConnectionContainer>();

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
     * @throws IOException if the workflow file can not be found or files to
     *             load node internals
     * @throws InvalidSettingsException if settings cannot be read
     * @throws CanceledExecutionException if loading was canceled
     * @throws WorkflowInExecutionException if the workflow is currently being
     *             executed
     */
    public synchronized void load(final File workflowFile) throws IOException,
            InvalidSettingsException, CanceledExecutionException,
            WorkflowInExecutionException {
        checkForRunningNodes("Workflow cannot be loaded");

        if (!workflowFile.isFile()
                || !workflowFile.getName().equals(WORKFLOW_FILE)) {
            throw new IOException("File must be named: \"" + WORKFLOW_FILE
                    + "\": " + workflowFile);
        }

        // load workflow topology
        NodeSettingsRO settings = NodeSettings.loadFromXML(new FileInputStream(
                workflowFile));
        load(settings);

        NodeProgressMonitor progMon = new DefaultNodeProgressMonitor();

        File parentDir = workflowFile.getParentFile();

        // data files are loaded using a repository of reference tables;
        // these lines serves to init the repository so nodes can put their data
        // into this map, the repository is deleted when the loading is done
        int loadID = System.identityHashCode(this);
        BufferedDataTable.initRepository(loadID);
        ArrayList<NodeContainer> failedNodes = new ArrayList<NodeContainer>();
        // get all keys in there
        try {
            for (NodeContainer newNode : topSortNodes()) {
                try {
                    NodeSettingsRO nodeSetting = settings.getNodeSettings(
                            KEY_NODES).getNodeSettings(
                            "node_" + newNode.getID());
                    String nodeFileName = nodeSetting
                            .getString(KEY_NODE_SETTINGS_FILE);
                    File nodeFile = new File(parentDir, nodeFileName);
                    newNode.load(loadID, nodeFile, progMon);
                } catch (IOException ioe) {
                    String msg = "Unable to load node: "
                            + newNode.getNameWithID()
                            + " -> reset and configure.";
                    LOGGER.error(msg);
                    LOGGER.debug("", ioe);
                    failedNodes.add(newNode);
                } catch (InvalidSettingsException ise) {
                    String msg = "Unable to load node: "
                            + newNode.getNameWithID()
                            + " -> reset and configure.";
                    LOGGER.error(msg);
                    LOGGER.debug("", ise);
                    failedNodes.add(newNode);
                }
            }
        } finally {
            // put into a finally block because that may release much of memory
            BufferedDataTable.clearRepository(loadID);
        }
        for (NodeContainer newNode : failedNodes) {
            resetAndConfigureNode(newNode.getID());
        }
    }

    /**
     * Only load internal workflow manager settings, init nodes and connections.
     * No NodeSettings, DataTableSpec, DataTable, or ModelContent are loaded.
     * 
     * @param settings read settings from
     * @throws InvalidSettingsException if an error occurs during reading
     */
    private void load(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // read running ids for new nodes and connections
        if (m_parent == null) {
            m_runningNodeID.setValue(settings.getInt(KEY_RUNNING_NODE_ID));
            m_runningConnectionID
                    .setValue(settings.getInt(KEY_RUNNING_CONN_ID));
        }

        // read nodes
        NodeSettingsRO nodes = settings.getNodeSettings(KEY_NODES); // Node-Subconfig
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
                LOGGER.error("Could not create factory object of type "
                        + nodeSetting.getString(NodeContainer.KEY_FACTORY_NAME,
                                "??") + " for node " + nodeKey, ex);
            } catch (IllegalAccessException ex) {
                LOGGER.error("Could not access factory class "
                        + nodeSetting.getString(NodeContainer.KEY_FACTORY_NAME,
                                "??") + " for node " + nodeKey, ex);
            } catch (ClassNotFoundException ex) {
                LOGGER.error("Could not find factory class "
                        + nodeSetting.getString(NodeContainer.KEY_FACTORY_NAME,
                                "??") + " for node " + nodeKey, ex);
            }
        }

        // read connections
        NodeSettingsRO connections = settings.getNodeSettings(KEY_CONNECTIONS);
        for (String connectionKey : connections.keySet()) {
            // retrieve config object for next connection
            NodeSettingsRO connectionConfig = connections
                    .getNodeSettings(connectionKey);
            // and add appropriate connection to workflow
            ConnectionContainer cc = null;
            try {
                cc = new ConnectionContainer(m_runningConnectionID.inc(),
                        connectionConfig, this);
            } catch (InvalidSettingsException ise) {
                LOGGER.warn("Could not create connection: " + connectionKey
                        + " reason: " + ise.getMessage());
                LOGGER.debug(connectionConfig, ise);
            }
            try {
                addConnection(cc);
            } catch (Exception ex) {
                LOGGER.error("Could not add connection", ex);
            }
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
        checkForRunningNodes("Connection cannot be removed", connection
                .getTarget());

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
        checkForRunningNodes("Node cannot be removed");

        Integer id = m_idsByNode.get(container);
        if (id != null) {
            // tell node that it has been disconnected (close views...)
            try {
                container.detach();
                resetAndConfigureAfterNode(id);
                disconnectNodeContainer(container);
            } catch (Exception ex) {
                LOGGER.error("Error while removing node", ex);
            }

            container.removeAllListeners();

            m_nodesByID.remove(id);
            m_idsByNode.remove(container);

            LOGGER.debug("Removed: " + container.getNameWithID());
            fireWorkflowEvent(new WorkflowEvent.NodeRemoved(id, container, null));
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
        checkForRunningNodes("Node cannot be reset", nodeCont);

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
        checkForRunningNodes("Node cannot be reset", nodeCont);

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
     * @param exec The execution monitor for the workflow saving progress
     * @throws IOException If the workflow file can't be found.
     * @throws CanceledExecutionException If the saving process has been
     *             canceled.
     * @throws WorkflowInExecutionException if the workflow is currently being
     *             executed
     */
    public synchronized void save(final File workflowFile,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException, WorkflowInExecutionException {
        checkForRunningNodes("Workflow cannot be saved");

        if (!workflowFile.isFile()
                || !workflowFile.getName().equals(WORKFLOW_FILE)) {
            throw new IOException("File must be named: \"" + WORKFLOW_FILE
                    + "\": " + workflowFile);
        }

        File parentDir = workflowFile.getParentFile();
        // workflow settings
        NodeSettings settings = new NodeSettings(WORKFLOW_FILE);
        // save current running id
        settings.addInt(KEY_RUNNING_NODE_ID, m_runningNodeID.intValue());
        // save current connection id
        settings.addInt(KEY_RUNNING_CONN_ID, m_runningConnectionID.intValue());
        // save nodes in an own sub-config object as a series of configs
        NodeSettingsWO nodes = settings.addNodeSettings(KEY_NODES);
        int nodeNum = 0;
        for (NodeContainer nextNode : m_nodesByID.values()) {

            nodeNum++;
            exec.setProgress((double)nodeNum / m_nodesByID.size());

            // create node directory based on the nodes name and id
            // all chars which are not letter or number are replaced by '_'
            String nodeDirID = nextNode.getName().replaceAll("[^a-zA-Z0-9 ]",
                    "_")
                    + " (#" + nextNode.getID() + ")";
            // and save it to it's own config object
            NodeSettingsWO nextNodeConfig = nodes.addNodeSettings("node_"
                    + nextNode.getID());
            String nodeFileName = nodeDirID + "/" + Node.SETTINGS_FILE_NAME;
            nextNodeConfig.addString(KEY_NODE_SETTINGS_FILE, nodeFileName);
            File nodeFile = new File(parentDir, nodeFileName);
            File nodeDir = nodeFile.getParentFile();
            if (!nodeDir.isDirectory() && !nodeDir.mkdir()) {
                throw new IOException("Unable to create dir: " + nodeDir);
            }
            nextNode.save(nextNodeConfig, nodeFile, exec);
        }

        NodeSettingsWO connections = settings.addNodeSettings(KEY_CONNECTIONS);
        for (ConnectionContainer cc : m_connectionsByID.values()) {
            // and save it to it's own config object
            NodeSettingsWO nextConnectionConfig = connections
                    .addNodeSettings("connection_" + cc.getID());
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
        checkForRunningNodes("Dialog settings cannot be applied", nodeCont);

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

        Iterator<Map.Entry<NodeContainer, Integer>> nodeContaierEntries = m_idsByNode
                .entrySet().iterator();

        while (nodeContaierEntries.hasNext()) {
            Map.Entry<NodeContainer, Integer> entry = nodeContaierEntries
                    .next();

            entry.getKey().closeAllViews();
            entry.getKey().closeAllPortViews();
        }
        
        List<NodeContainer> sortedNodes = topSortNodes();
        
        for (int i = sortedNodes.size() - 1; i >= 0; i--) {
            sortedNodes.get(i).cleanup();
        }
    }
}
