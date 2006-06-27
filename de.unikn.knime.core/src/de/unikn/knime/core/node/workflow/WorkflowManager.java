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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.DefaultNodeProgressMonitor;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.Node;
import de.unikn.knime.core.node.NodeFactory;
import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.NodeStateListener;
import de.unikn.knime.core.node.NodeStatus;

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
    /** Key for connections. */
    public static final String KEY_CONNECTIONS = "connections";

    /** Key for nodes. */
    public static final String KEY_NODES = "nodes";

    /** Links the node settings file name. */
    public static final String KEY_NODE_SETTINGS_FILE = "node_settings_file";

    /** Key for current running connection id. */
    private static final String KEY_RUNNING_CONN_ID = "runningConnectionID";

    /** Key for current running node id. */
    private static final String KEY_RUNNING_NODE_ID = "runningNodeID";

    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(WorkflowManager.class);

    // quick access to connections
    private final Map<Integer, ConnectionContainer> m_connectionsByID = 
        new HashMap<Integer, ConnectionContainer>();

    // change listener support (transient) <= why? (tm)
    private final transient ArrayList<WorkflowListener> m_eventListeners = 
        new ArrayList<WorkflowListener>();

    // quick access to IDs via Nodes
    private final Map<Node, Integer> m_idsByNode = new HashMap<Node, Integer>();

    // quick access to nodes by ID
    private final Map<Integer, NodeContainer> m_nodeContainerByID = 
        new HashMap<Integer, NodeContainer>();

    private final WorkflowManager m_parent;

    private final List<WeakReference<WorkflowManager>> m_children = 
        new ArrayList<WeakReference<WorkflowManager>>();

    private volatile int m_runningConnectionID = -1;

    // internal variables to allow generation of unique indices
    private volatile int m_runningNodeID = 0;

    private final Object m_modificationLock = new Object();

    /**
     * Identifier for KNIME workflows.
     */
    public static final String WORKFLOW_FILE = "workflow.knime";

    /**
     * Create new Workflow.
     */
    public WorkflowManager() {
        m_parent = null;
        m_executor = new WorkflowExecutor();
    }

    /**
     * Create new WorkflowManager by a given <code>WORKFLOW_FILE</code>. All
     * nodes and connection are initialzied, and - if available -
     * <code>NodeSettings</code>, <code>DataTableSpec</code>,
     * <code>DataTable</code>, and <code>PredictiveParams</code> are loaded
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
     * Loads the complete workflow from the given file.
     * 
     * @param workflowFile the workflow file
     * @throws IOException if the workflow file can not be found or files to
     *             load node internals
     * @throws InvalidSettingsException if settings cannot be read
     * @throws CanceledExecutionException if loading was canceled
     * @throws WorkflowInExecutionException if the workflow is currently being
     * executed
     */
    public void load(final File workflowFile)
    throws IOException, InvalidSettingsException, CanceledExecutionException,
    WorkflowInExecutionException {
        synchronized (m_modificationLock) {
            checkForRunningNodes();
            
            if (!workflowFile.isFile()
                    || !workflowFile.getName().equals(WORKFLOW_FILE)) {
                throw new IOException("File must be named: \"" + WORKFLOW_FILE
                        + "\": " + workflowFile);
            }
    
            // load workflow topology
            NodeSettings settings = NodeSettings.loadFromXML(
                    new FileInputStream(workflowFile));
            load(settings);
    
            ExecutionMonitor exec = new ExecutionMonitor(
                    new DefaultNodeProgressMonitor());
            
            File parentDir = workflowFile.getParentFile();
            
            // get all keys in there
            for (NodeContainer newNode : topSortNodes()) {
                try {
                    NodeSettings nodeSetting = settings.getConfig(KEY_NODES)
                        .getConfig("node_" + newNode.getID());
                    String nodeFileName = 
                        nodeSetting.getString(KEY_NODE_SETTINGS_FILE);
                    File nodeFile = new File(parentDir, nodeFileName);
                    newNode.getNode().load(nodeFile, exec);
                } catch (IOException ioe) {
                    LOGGER.warn("Unable to load node internals for: "
                            + newNode.getNameWithID(), ioe);
                    newNode.getNode().resetAndConfigure();
                } catch (InvalidSettingsException ise) {
                    LOGGER.warn("Unable to load settings for: " 
                            + newNode.getNameWithID(), ise);
                    newNode.getNode().resetAndConfigure();
                }
            }
        }
    }
    
    /**
     * Only load internal workflow manager settings, init nodes and connections.
     * No NodeSettings, DataTableSpec, DataTable, or PredictiveParams are
     * loaded.
     * 
     * @param settings read settings from
     * @throws InvalidSettingsException if an error occurs during reading
     */
    private void load(final NodeSettings settings)
        throws InvalidSettingsException {
        // read running ids for new nodes and connections
        m_runningNodeID = settings.getInt(KEY_RUNNING_NODE_ID);
        m_runningConnectionID = settings.getInt(KEY_RUNNING_CONN_ID);

        // read nodes
        NodeSettings nodes = settings.getConfig(KEY_NODES); // Node-Subconfig
        // get all keys in there
        for (String nodeKey : nodes.keySet()) {
            NodeSettings nodeSetting = null;
            try {
                // retrieve config object for each node
                nodeSetting = nodes.getConfig(nodeKey);
                // create NodeContainer based on NodeSettings object
                NodeContainer newNode = new NodeContainer(nodeSetting, this);
                // and add it to workflow
                addNodeWithID(newNode);
            } catch (InvalidSettingsException ise) {
                LOGGER.warn("Could not create node " + nodeKey + " reason: "
                        + ise.getMessage());
                LOGGER.debug(nodeSetting, ise);
            }
        }

        // read connections
        NodeSettings connections = settings.getConfig(KEY_CONNECTIONS);
        for (String connectionKey : connections.keySet()) {
            // retrieve config object for next connection
            NodeSettings connectionConfig = connections
                    .getConfig(connectionKey);
            // and add appropriate connection to workflow
            ConnectionContainer cc = null;
            try {
                // TODO (tg) what happends with the connection id if this fails?
                cc = new ConnectionContainer(
                        ++m_runningConnectionID, connectionConfig, this);
            } catch (InvalidSettingsException ise) {
                LOGGER.warn("Could not create connection: " + connectionKey
                        + " reason: " + ise.getMessage());
                LOGGER.debug(connectionConfig, ise);
            }
            addConnection(cc);
        }
    }

    /* Topological sorting of all nodes inthe workflow */
    private Collection<NodeContainer> topSortNodes() {
        Collection<NodeContainer> termList = new ArrayList<NodeContainer>();
        for (Integer nodeKey : m_nodeContainerByID.keySet()) {
            NodeContainer newNode = m_nodeContainerByID.get(nodeKey);
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

    /**
     * Creates a new sub workflow manager with the given parent manager.
     * 
     * @param parent the parent workflow manager
     */
    protected WorkflowManager(final WorkflowManager parent) {
        m_parent = parent;
        m_parent.m_children.add(new WeakReference<WorkflowManager>(this));
        m_executor = m_parent.m_executor;
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
     * @throws WorkflowInExecutionException 
     * @throws IllegalArgumentException if port indices are invalid
     * @throws WorkflowInExecutionException if the workflow is currently being
     * executed
     */
    public ConnectionContainer addConnection(final int idOut,
            final int portOut, final int idIn, final int portIn)
    throws WorkflowInExecutionException {
        synchronized (m_modificationLock) {
            checkForRunningNodes();
            
            NodeContainer nodeOut = m_nodeContainerByID.get(idOut);
            NodeContainer nodeIn = m_nodeContainerByID.get(idIn);
    
            if (nodeOut == null) {
                throw new IllegalArgumentException("Node with id #" + idOut
                        + " does not exist.");
            }
            if (nodeIn == null) {
                throw new IllegalArgumentException("Node with id #" + idIn
                        + " does not exist.");
            }
    
            ConnectionContainer newConnection = new ConnectionContainer(
                    ++m_runningConnectionID, nodeOut, portOut, nodeIn, portIn);
            addConnection(newConnection);
            return newConnection;
        }
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
        inNode.getNode().getInPort(inPort).connectPort(
                outNode.getNode().getOutPort(outPort));

        // add this manager as listener for workflow event
        cc.addWorkflowListener(this);

        // notify listeners
        LOGGER.debug("Added connection (from node id:" + outNode.getID()
                + ", port:" + outPort + " to node id:" + inNode.getID()
                + ", port:" + inPort + ")");
        fireWorkflowEvent(new WorkflowEvent.ConnectionAdded(-1, null, cc));
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
    public NodeContainer addNewNode(final NodeFactory factory) {
        synchronized (m_modificationLock) {
            Node node = new Node(factory, this);
            LOGGER.debug("adding node '" + node + "' to the workflow...");
            
            if (m_idsByNode.containsKey(node)) {
                throw new IllegalArgumentException(
                        "Node already managed by this workflow, "
                                + "can't add multiple times: " + node);
            }
            // create new ID
            final int newNodeID = ++m_runningNodeID;
            assert (!m_nodeContainerByID.containsKey(newNodeID));
            // create new wrapper for this node
    
            NodeContainer newNode = new NodeContainer(node, newNodeID);
    
            // add WorkflowManager as listener for state change events
            newNode.addListener(m_nodeStateListener);
            // and add it to our hashmap of nodes.
            m_nodeContainerByID.put(newNodeID, newNode);
            m_idsByNode.put(node, newNodeID);
    
            // notify listeners
            LOGGER.debug("Added " + newNode.getNameWithID());
            fireWorkflowEvent(new WorkflowEvent.NodeAdded(
                    newNodeID, null, newNode));
    
            
            LOGGER.debug("done, ID=" + newNodeID);
            return getNodeContainer(node);
        }
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
        if (m_nodeContainerByID.containsKey(nc.getNode())) {
            throw new IllegalArgumentException("duplicate ID");
        }
        if (m_idsByNode.containsKey(nc.getNode())) {
            throw new IllegalArgumentException("duplicate/illegal node");
        }
        nc.addListener(m_nodeStateListener);
        m_nodeContainerByID.put(id, nc);
        m_idsByNode.put(nc.getNode(), id);

        // notify listeners
        LOGGER.debug("Added " + nc.getNameWithID());
        fireWorkflowEvent(new WorkflowEvent.NodeAdded(nc.getID(), null, nc));
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

        Node src = getNode(sourceNode);
        Node targ = getNode(targetNode);

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

        ConnectionContainer conn = getIncomingConnectionAt(
                getNodeContainer(targ), inPort);
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

        // the easy tests all succeeded, now check if we are creating a loop
        NodeContainer targC = getNodeContainer(targ);
        assert targC.getID() == targetNode;
        NodeContainer srcC = getNodeContainer(src);
        assert srcC.getID() == sourceNode;
        boolean loop = targC.isFollowedBy(srcC);
        // if (loop) {
        // LOGGER.warn("Attempt to create loop (from node id:" + srcC.getID()
        // + ", port:" + inPort + " to node id:" + targC.getID()
        // + ", port:" + outPort + ")");
        // }
        return !loop;
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
     * Creates additional nodes and optional connections between those specified
     * in the settings object.
     * 
     * @param settings the <code>NodeSettings</code> object describing the sub
     *            workflow to add to this workflow manager
     * @param positionChangeMultiplier factor to determine the change of the
     *            position of a copied object
     * 
     * @return the ids of the newly created containers
     * 
     * @throws InvalidSettingsException thrown if the passed settings are not
     *             valid
     */
    public int[] createSubWorkflow(final NodeSettings settings,
            final int positionChangeMultiplier) 
            throws InvalidSettingsException {
        synchronized (m_modificationLock) {
            NodeSettings nodes = settings.getConfig(KEY_NODES);
    
            // the new ids to return
            ArrayList<Integer> newIDs = new ArrayList<Integer>();
    
            // the map is used to map the old node id to the new one
            Map<Integer, Integer> idMap = new HashMap<Integer, Integer>();
    
            for (String nodeKey : nodes.keySet()) {
                NodeSettings nodeSetting = null;
                try {
                    // retrieve config object for each node
                    nodeSetting = nodes.getConfig(nodeKey);
                    // create NodeContainer based on NodeSettings object
                    final int newId = ++m_runningNodeID;
                    nodeSetting.addInt(NodeContainer.KEY_ID, newId);
                    final NodeContainer newNode = new NodeContainer(nodeSetting,
                            this);
    
                    // change the id, as this id is already in use
                    // first remeber the old id "map(oldId, newId)"
    
                    // remember temporarily the old id
                    final int oldId = newNode.getID();
    
    
                    idMap.put(newNode.getID(), newId);
                    // remember the new id for the return value
                    newIDs.add(newId);
    
                    // finaly change the extra info so that the copies are
                    // located differently (if not null)
                    NodeExtraInfo extraInfo = newNode.getExtraInfo();
                    if (extraInfo != null) {
                        extraInfo.changePosition(40 * positionChangeMultiplier);
                    }
    
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
            NodeSettings connections = settings.getConfig(KEY_CONNECTIONS);
            for (String connectionKey : connections.keySet()) {
                // retrieve config object for next connection
                NodeSettings connectionConfig = connections
                        .getConfig(connectionKey);
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
                        ++m_runningConnectionID, connectionConfig, this, idMap);
                    addConnection(cc);
                    // add the id to the new ids
                    newIDs.add(cc.getID());
                } catch (InvalidSettingsException ise) {
                    LOGGER.warn("Could not create connection " + connectionKey
                            + " reason: " + ise.getMessage());
                    LOGGER.debug(connectionConfig, ise);
                }
            }
    
            int[] idArray = new int[newIDs.size()];
            int i = 0;
            for (Integer newId : newIDs) {
                idArray[i++] = newId;
            }
    
            return idArray;
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
        ArrayList<WorkflowListener> temp = 
            (ArrayList<WorkflowListener>)m_eventListeners.clone();
        for (WorkflowListener l : temp) {
            l.workflowChanged(event);
        }
        
        if (m_parent != null) {
            m_parent.fireWorkflowEvent(event);
        }
    }

    
    
    /**
     * Removes all connections (incoming and outgoing) from a node container.
     * Note that this results in a bunch of workflow events !
     * 
     * @param nodeCont the container which should be completely disconnected
     * @throws WorkflowInExecutionException if the workflow is currently being
     * executed 
     */
    public void disconnectNodeContainer(final NodeContainer nodeCont)
    throws WorkflowInExecutionException {
        synchronized (m_modificationLock) {
            checkForRunningNodes();
            
            int numIn = nodeCont.getNode().getNrInPorts();
            int numOut = nodeCont.getNode().getNrOutPorts();
    
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
     * return a node for a specific identifier. Returns null if ID is not found,
     * throws an exception if something odd happens.
     * 
     * @param id the identifier of the node request
     * @return node matching the given ID
     */
    private Node getNode(final int id) {
        NodeContainer nodeObj = m_nodeContainerByID.get(id);
        if (nodeObj == null) {
            return null;
        }
        return nodeObj.getNode();
    }

    /**
     * Returns the node container that is handled by the manager for the given
     * node.
     * 
     * @param node The node
     * @return The container that wraps the node
     */
    public NodeContainer getNodeContainer(final Node node) {
        Integer id = m_idsByNode.get(node);
        if (id == null) {
            if (m_parent != null) {
                return m_parent.getNodeContainer(node);
            } else {
                return null;
            }
        }

        NodeContainer cont = m_nodeContainerByID.get(id);
        return cont;
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
        NodeContainer cont = m_nodeContainerByID.get(new Integer(id));
        return cont;
    }

    /**
     * Returns all nodes currently managed by this instance.
     * 
     * @return All the managed node containers.
     */
    public Collection<NodeContainer> getNodes() {
        return Collections.unmodifiableCollection(m_nodeContainerByID.values());
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
     * Deletes a connection between two nodes.
     * 
     * @param connection to be deleted
     * @throws WorkflowInExecutionException if the workflow is currently being
     * executed
     */
    public void removeConnection(final ConnectionContainer connection)
    throws WorkflowInExecutionException {
        synchronized (m_modificationLock) {
            checkForRunningNodes();
            
            // if connection does not exist simply return
            if (!(m_connectionsByID.containsKey(connection.getID()))) {
                return;
            }
    
            // retrieve source and target node
            NodeContainer nodeOut = connection.getSource();
            int portOut = connection.getSourcePortID();
            NodeContainer nodeIn = connection.getTarget();
            int portIn = connection.getTargetPortID();
            // remove outgoing edge
            nodeOut.removeOutgoingConnection(portOut, nodeIn);
            // remove incoming edge
            nodeIn.removeIncomingConnection(portIn);
            // also disconnect the two underlying Nodes.
            nodeIn.getNode().getInPort(portIn).disconnectPort();
            // finally remove connection from internal list
            m_connectionsByID.remove(connection.getID());
    
            // notify listeners
            LOGGER.info("Removed connection (from node id:" + nodeOut.getID()
                    + ", port:" + portOut + " to node id:" + nodeIn.getID()
                    + ", port:" + portIn + ")");
            fireWorkflowEvent(new WorkflowEvent.ConnectionRemoved(-1,
                    connection, null));
        }
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
     * executed
     */
    public void removeNode(final NodeContainer container)
        throws WorkflowInExecutionException {
        synchronized (m_modificationLock) {
            checkForRunningNodes();
            
            Node node = container.getNode();
            Integer id = m_idsByNode.get(node);
            if (id != null) {
                // tell node that it has been disconnected (close views...)
                node.detach();
    
                resetAndConfigureAfterNode(id);
                
                disconnectNodeContainer(container);
    
                container.removeAllListeners();
    
                m_nodeContainerByID.remove(id);
                m_idsByNode.remove(container.getNode());            
                
                LOGGER.debug("Removed: " + container.getNameWithID());
                fireWorkflowEvent(
                        new WorkflowEvent.NodeRemoved(id, container, null));
            } else {
                LOGGER.error("Could not find (and remove): " + node.getName());
                throw new IllegalArgumentException(
                        "Node not managed by this workflow: " + node);
            }
        }
    }


    /**
     * Removes all nodes and connection from the workflow.
     * 
     * @throws WorkflowInExecutionException if the workflow is currently being
     * executed
     */
    public void clear() throws WorkflowInExecutionException {
        synchronized (m_modificationLock) {
            checkForRunningNodes();
            
            List<NodeContainer> containers = new ArrayList<NodeContainer>(
                    m_nodeContainerByID.values());
            for (NodeContainer nc : containers) {
                removeNode(nc);
            }
    
            assert (m_nodeContainerByID.size() == 0);
            assert (m_connectionsByID.size() == 0);
            assert (m_idsByNode.size() == 0);
            m_runningConnectionID = -1;
            m_runningNodeID = 0;
        }
    }

    
    /**
     * Saves this workflow manager settings including nodes and connections
     * into the given file. In additon, all nodes' internal structures are
     * stored - if available, depending on the current node state, reset, 
     * configured, or executed. For each node a directory is created (at
     * the workflow file's parent path) to save the node internals.
     * @param workflowFile To write workflow manager settings to.
     * @throws IOException If the workflow file can't be found.
     * @throws CanceledExecutionException If the saving process has been 
     *         canceled.
     * @throws WorkflowInExecutionException if the workflow is currently being
     * executed
     */
    public void save(final File workflowFile) throws IOException,
            CanceledExecutionException, WorkflowInExecutionException {
        save(workflowFile, null);
    }
    
    /**
     * TODO Thorsten, please check docu.
     * Saves this workflow manager settings including nodes and connections
     * into the given file. In additon, all nodes' internal structures are
     * stored - if available, depending on the current node state, reset, 
     * configured, or executed. For each node a directory is created (at
     * the workflow file's parent path) to save the node internals.
     * 
     * @param workflowFile To write workflow manager settings to.
     * @param omitNodes an optional collection of node containers that shall
     * <b>not</b> be saved; maybe <code>null</code>
     * @throws IOException If the workflow file can't be found.
     * @throws CanceledExecutionException If the saving process has been 
     *         canceled.
     * @throws WorkflowInExecutionException if the workflow is currently being
     * executed
     */
    public void save(final File workflowFile, 
            final Collection<NodeContainer> omitNodes) throws IOException,
            CanceledExecutionException, WorkflowInExecutionException {
        synchronized (m_modificationLock) {
            checkForRunningNodes();
            
            if (!workflowFile.isFile()
                    || !workflowFile.getName().equals(WORKFLOW_FILE)) {
                throw new IOException("File must be named: \"" + WORKFLOW_FILE
                        + "\": " + workflowFile);
            }
            
            ExecutionMonitor exec = new ExecutionMonitor();
            
            File parentDir = workflowFile.getParentFile();
            // workflow settings
            NodeSettings settings = new NodeSettings(WORKFLOW_FILE);
            // save current running id
            settings.addInt(KEY_RUNNING_NODE_ID, m_runningNodeID);
            // save current connection id
            settings.addInt(KEY_RUNNING_CONN_ID, m_runningConnectionID);
            // save nodes in an own sub-config object as a series of configs
            NodeSettings nodes = settings.addConfig(KEY_NODES);
            for (NodeContainer nextNode : m_nodeContainerByID.values()) {
                if ((omitNodes != null) && omitNodes.contains(nextNode)) {
                    continue;
                }
                // create node directory based on the nodes name and id
                // all chars which are not letter or number are replaced by '_'
                String nodeDirID = nextNode.getName().replaceAll(
                        "[^a-zA-Z0-9 ]", "_") + " (#" + nextNode.getID() + ")";
                // and save it to it's own config object
                NodeSettings nextNodeConfig = nodes.addConfig(
                        "node_" + nextNode.getID());
                String nodeFileName = nodeDirID + "/" + Node.SETTINGS_FILE_NAME;
                nextNodeConfig.addString(KEY_NODE_SETTINGS_FILE, nodeFileName);
                File nodeFile = new File(parentDir, nodeFileName);
                File nodeDir = nodeFile.getParentFile();
                if (!nodeDir.isDirectory() && !nodeDir.mkdir()) {
                    throw new IOException("Unable to create dir: " + nodeDir);
                }
                nextNode.save(nextNodeConfig);
                nextNode.getNode().save(nodeFile, exec);
            }
            
            NodeSettings connections = settings.addConfig(KEY_CONNECTIONS);
            for (ConnectionContainer cc : m_connectionsByID.values()) {
                if ((omitNodes != null) && (omitNodes.contains(cc.getTarget())
                        || omitNodes.contains(cc.getSource()))) {
                    continue;
                }
                
                // and save it to it's own config object
                NodeSettings nextConnectionConfig = connections
                        .addConfig("connection_" + cc.getID());
                cc.save(nextConnectionConfig);
            }
            settings.saveToXML(new FileOutputStream(workflowFile));
        }
    }

    private final MyNodeStateListener m_nodeStateListener =
        new MyNodeStateListener();

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
            NodeContainer changedNode = m_nodeContainerByID.get(nodeID);
            assert (changedNode != null);

            // if this is an event indicating the start of a node's execution:
            if (state instanceof NodeStatus.Reset) {
                fireWorkflowEvent(new WorkflowEvent.NodeReset(nodeID, null,
                        null));
            } else if (state instanceof NodeStatus.Configured) {
                fireWorkflowEvent(new WorkflowEvent.NodeConfigured(nodeID, null,
                        null));
            } else if (state instanceof NodeStatus.ExtrainfoChanged) {
                fireWorkflowEvent(new WorkflowEvent.NodeExtrainfoChanged(nodeID,
                        null, null));
            }
        }        
    }
    

    
    // ===================================================
    
    private final WorkflowExecutor m_executor;
    
    /**
     * This class is the executor for the workflow. Note that there is only
     * one executor for a workflow and all its children.
     * 
     * @author Thorsten Meinl, University of Konstanz
     */
    private class WorkflowExecutor implements NodeStateListener {
        private final List<NodeContainer> m_waitingNodes =
            new LinkedList<NodeContainer>();
        private final List<NodeContainer> m_runningNodes =
            new LinkedList<NodeContainer>();
        
        /**
         * Checks if new nodes are executable now and if so, starts them.
         * Threads waiting on m_waitingNodes are awakened if no nodes are
         * running any more and no new executable node has been found.
         */
        private void startNewNodes() {
            synchronized (m_waitingNodes) {
                Iterator<NodeContainer> it = m_waitingNodes.iterator();
                while (it.hasNext()) {
                    NodeContainer nc = it.next();
                    if (nc.getNode().isExecutable()) {                        
                        it.remove();
                        
                        DefaultNodeProgressMonitor pm =
                            new DefaultNodeProgressMonitor();
                        fireWorkflowEvent(new WorkflowEvent.NodeStarted(
                                nc.getID(), nc, pm));
                        nc.addListener(this);
                        nc.startExecution(pm);
                        m_runningNodes.add(nc);
                    } else if (nc.isExecuted()) {
                        it.remove();
                    }
                }
                

                if (m_runningNodes.size() == 0) {
                    if (m_waitingNodes.size() > 0) {
                        LOGGER.warn("Some nodes were still waiting but none is"
                                + " running: " + m_waitingNodes);
                    }
                    m_waitingNodes.clear();
                    m_waitingNodes.notifyAll();
                }
            }            
        }
        
        
        /**
         * Adds new nodes to the list of nodes that are waiting for execution.
         * 
         * @param nodes a list of nodes that should be executed
         */
        public void addWaitingNodes(final Collection<NodeContainer> nodes) {
            synchronized (m_waitingNodes) {
                boolean change = false;
                for (NodeContainer nc : nodes) {
                    if (!m_waitingNodes.contains(nc)
                            && !m_runningNodes.contains(nc)) {
                        m_waitingNodes.add(nc);
                        change = true;
                    }
                }
                if (change) { startNewNodes(); }
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
                    for (NodeContainer nc : m_runningNodes) {
                        if (wfm.m_nodeContainerByID.values().contains(nc)) {
                            interesting = true;
                            break;
                        }
                    }

                    if (!interesting) {
                        for (NodeContainer nc : m_waitingNodes) {
                            if (wfm.m_nodeContainerByID.values().contains(nc)) {
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
         * @see de.unikn.knime.core.node.NodeStateListener
         *  #stateChanged(de.unikn.knime.core.node.NodeStatus, int)
         */
        public void stateChanged(final NodeStatus state, final int id) {
            if (state instanceof NodeStatus.EndExecute) {
                synchronized (m_waitingNodes) {
                    Iterator<NodeContainer> it = m_runningNodes.iterator();
                    while (it.hasNext()) {
                        NodeContainer nc = it.next();
                        if (nc.getID() == id) {
                            nc.removeListener(this);
                            it.remove();

                            fireWorkflowEvent(new WorkflowEvent.NodeFinished(
                                    nc.getID(), null, null));
                        }
                    }

                    startNewNodes();
                    m_waitingNodes.notifyAll();
                }
            }
        }
        
        
        /**
         * Cancel execution of all waiting nodes. Already running nodes are not
         * affected.
         * FIXME shouldn't we stop them?
         */
        public void cancelExecution() {
            synchronized (m_waitingNodes) {
                m_waitingNodes.clear();
            }
        }

        
        /**
         * Cancel the execution of the passed nodes if they are on the waiting
         * list. Running nodes are not affected.
         * 
         * @param nodes a list of nodes that should be canceled
         */
        public void cancelExecution(final Collection<NodeContainer> nodes) {
            synchronized (m_waitingNodes) {
                for (NodeContainer nc : nodes) {
                    m_waitingNodes.remove(nc);
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
         * <code>false</code> otherwise
         */
        public boolean executionInProgress(final WorkflowManager wfm) {
            synchronized (m_waitingNodes) {
                // check if any of the nodes in the lists are from the
                // passed WFM
                for (NodeContainer nc : m_runningNodes) {
                    if (wfm.m_nodeContainerByID.values().contains(nc)) {
                        return true;
                    }
                }

                for (NodeContainer nc : m_waitingNodes) {
                    if (wfm.m_nodeContainerByID.values().contains(nc)) {
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
    }
    
    
    /**
     * Executes all nodes in this workflow.
     * 
     * @param block <code>true</code> if the method should block until the
     * execution has been finished
     */
    public void executeAll(final boolean block) {
        synchronized (m_modificationLock) {
            Collection<NodeContainer> nodes = new ArrayList<NodeContainer>();
            findExecutableNodes(nodes);
            m_executor.addWaitingNodes(nodes);
        }
        if (block) {
            m_executor.waitUntilFinished(this);
        }
    }
    
    
    /**
     * Waits until the execution in this workflow has been finished.
     */
    public void waitUntilFinished() {
        m_executor.waitUntilFinished(this);
    }
    

    /**
     * Searches for potentially executable nodes that are before the passed
     * node container in the flow.
     * 
     * @param beforeNode the node up to which (but not including) executable
     * nodes should be searched
     * @param nodes a collection to which the executable nodes are added
     */
    private void findExecutableNodes(final NodeContainer beforeNode,
            final Collection<NodeContainer> nodes) {
        LinkedList<NodeContainer> pred = new LinkedList<NodeContainer>();
        pred.add(beforeNode);
        while (!pred.isEmpty()) {
            NodeContainer nodeCont = pred.removeFirst();
            for (NodeContainer nc : nodeCont.getPredecessors()) {
                if (nc.getNode().isConfigured() && !nc.getNode().isExecuted()
                        && !pred.contains(nc)) {
                    pred.add(nc);
                    nodes.add(nc);                    
                }
            }
            
            // check for auto-excutable nodes
            LinkedList<NodeContainer> succ =
                new LinkedList<NodeContainer>();
            succ.add(nodeCont);
            
            while (succ.size() > 0) {
                NodeContainer nc = succ.removeFirst();
                for (NodeContainer[] nca : nc.getSuccessors()) {
                    for (NodeContainer autoexec : nca) {
                        if (autoexec.isAutoExecutable()
                                && autoexec.getNode().isConfigured()
                                && !autoexec.getNode().isExecuted()
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
            for (NodeContainer nc : m_parent.m_nodeContainerByID.values()) {
                if (nc.getEmbeddedWorkflowManager() == this) {
                    myNodeContainer = nc;
                    break;
                }
            }
            assert (myNodeContainer != null) : "Did not find my node container";
            
            m_parent.findExecutableNodes(myNodeContainer, nodes);
        }
    }

    
    
    /**
     * Searches for potentially executable nodes in this workflow.
     * 
     * @param nodes a collection to which the executable nodes are added
     */
    private void findExecutableNodes(final Collection<NodeContainer> nodes) {
        // collect all configured nodes in this workflow manager
        for (Map.Entry<Node, Integer> e : m_idsByNode.entrySet()) {
            if (e.getKey().isConfigured() && !e.getKey().isExecuted()) {
                nodes.add(m_nodeContainerByID.get(e.getValue()));
            }
        }
        
        if (m_parent != null) {
            NodeContainer myNodeContainer = null;
            for (NodeContainer nc : m_parent.m_nodeContainerByID.values()) {
                if (nc.getEmbeddedWorkflowManager() == this) {
                    myNodeContainer = nc;
                    break;
                }
            }
            assert (myNodeContainer != null) : "Did not find my node container";
            
            m_parent.findExecutableNodes(myNodeContainer, nodes);
        }
    }
    
    
    /**
     * Executes the workflow up to and including the passed node. If desired the
     * method blocks until the execution has been finished.
     * 
     * @param nodeID the id of the node up to which the workflow should be
     * executed
     * @param block <code>true</code> if the method should block,
     * <code>false</code> otherwise
     */
    public void executeUpToNode(final int nodeID, final boolean block) {
        synchronized (m_modificationLock) {
            NodeContainer nc = m_nodeContainerByID.get(nodeID);
            if (!nc.getNode().isConfigured()) {
                throw new IllegalArgumentException("The given node is not"
                        + " configured and cannot be executed");
            }
            if (nc.getNode().isExecuted()) {
                throw new IllegalArgumentException("The given node is already"
                        + " executed");
            }
    
            Collection<NodeContainer> nodes = new ArrayList<NodeContainer>();
            nodes.add(nc);
            findExecutableNodes(nc, nodes);
            
            m_executor.addWaitingNodes(nodes);
        }
        if (block) {
            m_executor.waitUntilFinished(this);
        }
    }
    
    
    /**
     * Cancels the execution of waiting nodes in the workflow. Already running
     * nodes are not affected.
     * FIXME should we cancel running nodes?
     */
    public void cancelExecution() {
        m_executor.cancelExecution();
    }
    
    
    /**
     * Cancels the execution of the workflow after the passed node. Already
     * running nodes are not affected.
     * 
     * @param nodeID the id of the node after which the execution should be
     * canceled
     */
    public void cancelExecutionAfterNode(final int nodeID) {
        NodeContainer nodeCont = m_nodeContainerByID.get(nodeID);
        m_executor.cancelExecution(nodeCont.getAllSuccessors());
    }
    
    
    /**
     * Resets and configures the passed node and all its sucessors.
     * 
     * @param nodeID the node id
     * @throws WorkflowInExecutionException if the workflow is currently
     * executing
     */
    public void resetAndConfigureNode(final int nodeID)
    throws WorkflowInExecutionException {
        checkForRunningNodes();
        
        NodeContainer nodeCont = m_nodeContainerByID.get(nodeID);
        nodeCont.resetAndConfigure();
        for (NodeContainer nc : nodeCont.getAllSuccessors()) {
            nc.resetAndConfigure();
        }
    }

    
    /**
     * Configures the passed node but does not reset it. The caller has to
     * ensure that the node is resetted and not executed beforehand.
     * 
     * @param nodeID the node that should be configured
     */
    public void configureNode(final int nodeID) {
        NodeContainer nodeCont = m_nodeContainerByID.get(nodeID);
        nodeCont.configure();
    }
    
    
    /**
     * Resets and configures all nodes.
     * 
     * @throws WorkflowInExecutionException if the workflow is currently
     * executing
     */
    public void resetAndConfigureAll() throws WorkflowInExecutionException {
        checkForRunningNodes();
        
        for (NodeContainer nc : m_nodeContainerByID.values()) {
            nc.resetAndConfigure();
        }
    }

    
    /**
     * Resets and configures all nodes after the passed node.
     * 
     * @param nodeID the node id
     * @throws WorkflowInExecutionException if the workflow is currently
     * executing
     */
    public void resetAndConfigureAfterNode(final int nodeID)
    throws WorkflowInExecutionException {
        checkForRunningNodes();
        
        NodeContainer nodeCont = m_nodeContainerByID.get(nodeID);
        for (NodeContainer nc : nodeCont.getAllSuccessors()) {
            nc.resetAndConfigure();
        }        
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
    
    private void checkForRunningNodes() throws WorkflowInExecutionException {
        if (m_executor.executionInProgress(this)) {
            throw new WorkflowInExecutionException("Node cannot be reset while"
                    + " execution is in progress.");
        }        
    }
}
