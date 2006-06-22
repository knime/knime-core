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
     * @param workflowFile The location of the workflow file,
     *            <code>WORKFLOW_FILE</code>.
     * @throws InvalidSettingsException If settings can not be read.
     * @throws CanceledExecutionException If loading was canceled.
     * @throws IOException If the workflow file can not be found or files to
     *             load node interna.
     */
    public WorkflowManager(final File workflowFile)
            throws InvalidSettingsException, CanceledExecutionException, 
                   IOException {
        this();

        if (!workflowFile.isFile()
                || !workflowFile.getName().equals(WORKFLOW_FILE)) {
            throw new IOException("File must be named: \"" + WORKFLOW_FILE
                    + "\": " + workflowFile);
        }

        // load workflow topology
        NodeSettings settings = NodeSettings.loadFromXML(new FileInputStream(
                workflowFile));
        load(settings);

        ExecutionMonitor exec = new ExecutionMonitor(
                new DefaultNodeProgressMonitor());
        
        File parentDir = workflowFile.getParentFile();
        
        // get all keys in there
        for (NodeContainer newNode : analyzeTopology()) {
            try {
                NodeSettings nodeSetting = settings.getConfig(KEY_NODES)
                    .getConfig("node_" + newNode.getID());
                String nodeFileName = 
                    nodeSetting.getString(KEY_NODE_SETTINGS_FILE);
                File nodeFile = new File(parentDir, nodeFileName);
                newNode.getNode().load(nodeFile, exec);
            } catch (IOException ioe) {
                LOGGER.warn("Unable to load node internals for: "
                        + newNode.getNameWithID());
                newNode.getNode().resetAndConfigure();
            } catch (InvalidSettingsException ise) {
                LOGGER.warn("Unable to load settings for: " 
                        + newNode.getNameWithID());
                newNode.getNode().resetAndConfigure();
            }
        }
    }

    /**
     * Only load internal workflow manager settings, init nodes and connections.
     * No NodeSettings, DataTableSpec, DataTable, or PredictiveParams are
     * loaded.
     * 
     * @param settings Read settings from.
     * @throws InvalidSettingsException If an error occurs during reading.
     */
    public void load(final NodeSettings settings)
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

    private Collection<NodeContainer> analyzeTopology() {
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
        analyzerHelp(termList, list);
        return list;
    }

    private void analyzerHelp(final Collection<NodeContainer> current,
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
                analyzerHelp(con.getPredecessors(), result);
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
     * @throws IllegalArgumentException if port indices are invalid
     */
    public ConnectionContainer addConnection(final int idOut,
            final int portOut, final int idIn, final int portIn) {
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

        return addConnection(m_nodeContainerByID.get(idOut), portOut,
                m_nodeContainerByID.get(idIn), portIn);
    }

    /**
     * add a connection between two nodes. The port indices have to be within
     * their valid ranges.
     * 
     * @param outNode source node
     * @param portOut index of port on source node
     * @param inNode target node (sink)
     * @param portIn index of port on target
     * @return newly create edge
     */
    public ConnectionContainer addConnection(final NodeContainer outNode,
            final int portOut, final NodeContainer inNode, final int portIn) {
        if (m_parent == null) {
            if (!m_idsByNode.containsKey(outNode.getNode())) {
                throw new IllegalArgumentException("The output node container"
                        + " is not handled by this workflow manager");
            }
            if (!m_idsByNode.containsKey(inNode.getNode())) {
                throw new IllegalArgumentException("The input node container"
                        + " is not handled by this workflow manager");
            }
        }

        ConnectionContainer newConnection = new ConnectionContainer(
                ++m_runningConnectionID, outNode, portOut, inNode, portIn);
        addConnection(newConnection);
        return newConnection;
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
        NodeSettings nodes = settings.getConfig(KEY_NODES); // Node-Subconfig

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

                // change the id, as this id is already in use (it was copied)
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
    }

    
    
    /**
     * Removes all connections (incoming and outgoing) from a node container.
     * Note that this results in a bunch of workflow events !
     * 
     * @param nodeCont The container which should be completely disconnected
     */
    public void disconnectNodeContainer(final NodeContainer nodeCont) {
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
     */
    public void removeConnection(final ConnectionContainer connection) {
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
     */
    public void removeNode(final NodeContainer container) {
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


    /**
     * Removes all nodes and connection from the workflow.
     */
    public synchronized void clear() {
        List<NodeContainer> containers = new ArrayList<NodeContainer>(
                m_nodeContainerByID.values());
        for (NodeContainer nc : containers) {
            removeNode(nc);
        }

        assert (m_nodeContainerByID.size() == 0);
        assert (m_connectionsByID.size() == 0);
        assert (m_idsByNode.size() == 0);
        m_runningConnectionID = -1;
        m_runningNodeID = -1;
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
     */
    public void save(final File workflowFile) throws IOException,
            CanceledExecutionException {
        save(workflowFile, null);
    }
    
    /**
     * TODO Thorsten, please check docu.
     * Saves this workflow manager settings including nodes and connections
     * into the given file. In additon, all nodes' internal structures are
     * stored - if available, depending on the current node state, reset, 
     * configured, or executed. For each node a directory is created (at
     * the workflow file's parent path) to save the node internals.
     * @param workflowFile To write workflow manager settings to.
     * @throws IOException If the workflow file can't be found.
     * @throws CanceledExecutionException If the saving process has been 
     *         canceled.
     */
    public void save(final File workflowFile, 
            final Collection<NodeContainer> omitNodes) throws IOException,
            CanceledExecutionException {
        synchronized (m_modificationLock) {
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
    
    
    private class WorkflowExecutor extends Thread implements NodeStateListener {
        private final List<NodeContainer> m_waitingNodes =
            new LinkedList<NodeContainer>();
        private final List<NodeContainer> m_runningNodes =
            new LinkedList<NodeContainer>();
        /**
         * TODO 
         */
        public WorkflowExecutor() {
            super("KNIME Workflow Executor");
            setDaemon(true);
            start();
        }
        
        @Override
        public void run() {
            while (!isInterrupted()) {
                synchronized (m_waitingNodes) {
                    Iterator<NodeContainer> it = m_waitingNodes.iterator();
                    while (it.hasNext()) {
                        NodeContainer nc = it.next();
                        if (nc.getNode().isExecutable()) {
                            m_runningNodes.add(nc);
                            it.remove();
                            
                            DefaultNodeProgressMonitor pm =
                                new DefaultNodeProgressMonitor();
                            fireWorkflowEvent(new WorkflowEvent.NodeStarted(
                                    nc.getID(), nc, pm));
                            nc.addListener(this);
                            nc.startExecution(pm);
                        } else if (nc.isExecuted()) {
                            it.remove();
                        }
                    }
                    

                    boolean finished = false;
                    synchronized (m_runningNodes) {
                        finished = (m_runningNodes.size() == 0);
                    }
                    if (finished) {
                        synchronized (this) {
                            notifyAll();
                        }
                    }

                    
                    try {
                        m_waitingNodes.wait();
                    } catch (InterruptedException ex) {
                        // that's ok
                    }
                }
            }
        }
        
        
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
                if (change) { m_waitingNodes.notifyAll(); }
            }
        }
        
        
        public void waitUntilFinished() {            
            synchronized (this) {
                if ((m_runningNodes.size() > 0)
                        || (m_waitingNodes.size() != 0)) {
                    try {
                        wait();
                        // wake up all other waiting threads
                        notifyAll();
                    } catch (InterruptedException ex) {
                        // that's ok
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
                synchronized (m_runningNodes) {
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
                }
                
                
                synchronized (m_waitingNodes) {
                    m_waitingNodes.notifyAll();
                }
            }
        }
        
        public void cancelExecution() {
            synchronized (m_waitingNodes) {
                m_waitingNodes.clear();
                m_waitingNodes.notifyAll();
            }
        }

        public void cancelExecution(final Collection<NodeContainer> nodes) {
            synchronized (m_waitingNodes) {
                for (NodeContainer nc : nodes) {
                    m_waitingNodes.remove(nc);
                }
                m_waitingNodes.notifyAll();
            }
        }
        
        public boolean executionInProgress() {
            return (m_runningNodes.size() > 0 || m_waitingNodes.size() > 0);
        }
    }
    
    public void executeAll(final boolean block) {
        Collection<NodeContainer> nodes = new ArrayList<NodeContainer>();
        findExecutableNodes(nodes);
        m_executor.addWaitingNodes(nodes);
        if (block) {
            m_executor.waitUntilFinished();
        }
    }
    
    
    public void waitUntilFinished() {
        m_executor.waitUntilFinished();
    }
    
    
    

    private void findExecutableNodes(final Collection<NodeContainer> nodes,
            final NodeContainer beforeNode) {
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
            
            m_parent.findExecutableNodes(nodes, myNodeContainer);
        }
    }

    
    
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
            
            m_parent.findExecutableNodes(nodes, myNodeContainer);
        }
    }
    
    
    
    public void executeUpToNode(final int nodeID, final boolean block) {
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
        findExecutableNodes(nodes, nc);
        
        m_executor.addWaitingNodes(nodes);
        if (block) { m_executor.waitUntilFinished(); }
    }
    
    
    public void cancelExecution() {
        m_executor.cancelExecution();
    }
    
    
    public void cancelExecutionAfterNode(final int nodeID) {
        NodeContainer nodeCont = m_nodeContainerByID.get(nodeID);
        m_executor.cancelExecution(nodeCont.getAllSuccessors());
    }
    
    
    public void resetAndConfigureNode(final int nodeID) {
        if (m_executor.executionInProgress()) {
            throw new IllegalStateException("Node cannot be reset while"
                    + " execution is in progress.");
        }
        
        NodeContainer nodeCont = m_nodeContainerByID.get(nodeID);
        nodeCont.resetAndConfigure();
        for (NodeContainer nc : nodeCont.getAllSuccessors()) {
            nc.resetAndConfigure();
        }
    }

    public void resetAndConfigureAll() {
        if (m_executor.executionInProgress()) {
            throw new IllegalStateException("Node cannot be reset while"
                    + " execution is in progress.");
        }
        
        for (NodeContainer nc : m_nodeContainerByID.values()) {
            nc.resetAndConfigure();
        }
    }

    
    public void resetAndConfigureAfterNode(final int nodeID) {
        if (m_executor.executionInProgress()) {
            throw new IllegalStateException("Node cannot be reset while"
                    + " execution is in progress.");
        }
        
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

//    
//    
//    /**
//     * Cancel execution of all remaining nodes after the specified node.
//     * 
//     * @param nodeID the node's ID after which excution is to be canceled.
//     */
//    public synchronized void cancelExecutionAfterNode(final int nodeID) {
//        NodeContainer thisNodeC = m_nodeContainerByID.get(nodeID);
//        cancelExecutionAfterNode(thisNodeC);
//        // check if any other nodes are either in the queue or already
//        // executing (= not idle)
//        int activeNodes = 0;
//        for (NodeContainer nodeC : m_nodeContainerByID.values()) {
//            if (nodeC.getState() != NodeContainer.State.Idle) {
//                activeNodes++;
//            }
//        }
//        if (activeNodes == 0) {
//            // all nodes are idle, fire event that workflow pool is empty
//            fireWorkflowEvent(new WorkflowEvent.ExecPoolDone(-1, null, null));
//        }
//    }
//
//    private synchronized void cancelExecutionAfterNode(final NodeContainer n) {
//        // try to cancel this node
//        if ((n.getState() == NodeContainer.State.WaitingToBeExecutable)) {
//            // ok, we can simply change the node's flag
//            n.setState(NodeContainer.State.Idle);
//        }
//        if ((n.getState() == NodeContainer.State.IsExecutable)) {
//            // ok, we can simply change the node's flag
//            n.setState(NodeContainer.State.Idle);
//        }
//        if ((n.getState() == NodeContainer.State.WaitingForExecution)) {
//            // more complicated, we need to notify the node's progress monitor
//            // that we would like to cancel execution
//            n.cancelExecution();
//        }
//        if ((n.getState() == NodeContainer.State.CurrentlyExecuting)) {
//            // more complicated, we need to notify the node's progress monitor
//            // that we would like to cancel execution
//            n.cancelExecution();
//        }
//        // and also try to cancel all successors
//        NodeContainer[][] nodes = n.getSuccessors();
//        for (int i = 0; i < nodes.length; i++) {
//            NodeContainer[] portNodes = nodes[i];
//            for (int j = 0; j < portNodes.length; j++) {
//                cancelExecutionAfterNode(portNodes[j]);
//            }
//        }
//    }
//
//    // ////////////////////////
//    // Routines for Execution
//    // ////////////////////////
//
//    /**
//     * Cancel execution of all remaining nodes. Note that this is not a
//     * guarantee that all remaining nodes RIGHT NOW will be canceled but all of
//     * them will be asked to terminate. This routine requires the goodwill of
//     * the implementations of the individual execute-routines.
//     */
//    public synchronized void cancelExecutionAllRemainingNodes() {
//        int canceledNodes = 0; // how many could we cancel
//        int currentlyInQueue = 0; // how many are already in the queue?
//        int currentlyExecuting = 0; // how many are already executing?
//        for (NodeContainer nc : m_nodeContainerByID.values()) {
//            if (nc.getState() == NodeContainer.State.IsExecutable) {
//                // node has not yet been started, simlpy change back to IDLE
//                nc.setState(NodeContainer.State.Idle);
//                canceledNodes++;
//            }
//            if (nc.getState() == NodeContainer.State.WaitingToBeExecutable) {
//                // node has not yet been started, simlpy change back to IDLE
//                nc.setState(NodeContainer.State.Idle);
//                canceledNodes++;
//            }
//            if (nc.getState() == NodeContainer.State.WaitingForExecution) {
//                // these ones we need to cancel, since they have already
//                // been returned a requester as "executable"
//                nc.cancelExecution();
//                currentlyInQueue++;
//            }
//            if (nc.getState() == NodeContainer.State.CurrentlyExecuting) {
//                // these nodes are currently being executed, try to cancel.
//                nc.cancelExecution();
//                currentlyExecuting++;
//            }
//        }
//        if ((currentlyExecuting == 0) && (currentlyInQueue == 0)) {
//            // done. Otherwise we'll be done once the remaining ones return
//            // however we made sure no new nodes are going to be executed.
//            fireWorkflowEvent(new WorkflowEvent.ExecPoolDone(-1, null, null));
//        }
//    }
//
//
//
//    /**
//     * Return next available Node which needs to be executed. In theory at some
//     * point in time this may incorporate some clever sorting mechanism, right
//     * now it returns runnable nodes in some rather arbitrary, non-deterministic
//     * order. Note that a return value of null only means that right now there
//     * is no runnable node - there may be one later on when another node sends
//     * an event indicating that it is done executing. The final end of execution
//     * is indicated by the appropriate workflow event.
//     * 
//     * TODO: right now an executable node is only returned once even if it is
//     * never actually executed! We need a way to have a watchdog timer that
//     * resets these flags if nothing has happened for "too long"...
//     * 
//     * @return next runnable node or <code>null</code> of none is currently
//     *         (!) available.
//     */
//    public synchronized NodeContainer getNextExecutableNode() {
//        // right now just look for next runnable node from start every time
//        for (NodeContainer nc : m_nodeContainerByID.values()) {
//            if ((nc.getState() == NodeContainer.State.IsExecutable)
//                    && (nc.getNode().isExecutable())) {
//                nc.setState(NodeContainer.State.WaitingForExecution);
//                LOGGER.debug("returning node id=" + nc.getID()
//                        + " as next available executable.");
//                return nc;
//            }
//        }
//
//        if (m_parent != null) {
//            return m_parent.getNextExecutableNode();
//        }
//        // didn't find any runnable node: return null.
//        return null;
//    }
//
//    /**
//     * Starts the execution of a workflow (or parts of it) by sending events. No
//     * node will be directly executed by this method.
//     */
//    public void startExecution() {
//        checkForExecutableNodes();
//    }
//
//
//    /**
//     * Mark all nodes that are not yet executed as "to be executed" and the ones
//     * that are actually executable (all predecessors data is available) as
//     * "runnable". If no executable nodes have been found, a
//     * {@link WorkflowEvent.ExecPoolDone} is fired.
//     */
//    public synchronized void prepareForExecAllNodes() {
//        for (NodeContainer nc : m_nodeContainerByID.values()) {
//            if (!(nc.getNode().isExecuted())
//                && (nc.getState() != NodeContainer.State.WaitingForExecution)
//                && (nc.getState() != NodeContainer.State.CurrentlyExecuting)) {
//
//                if (nc.getNode().isExecutable()) {
//                    nc.setState(NodeContainer.State.IsExecutable);
//                } else {
//                    nc.setState(NodeContainer.State.WaitingToBeExecutable);
//                }
//            }
//        }
//
//        if (m_parent != null) {
//            for (ConnectionContainer cc : m_connectionsByID.values()) {
//                if (!m_nodeContainerByID.containsValue(cc.getSource())) {
//                    m_parent.prepareForExecUpToNode(cc.getSource());
//                }
//            }
//        }
//    }
//
//    /**
//     * Mark all nodes that are neceesary for the specificed node to be excecuted
//     * as "to be executed" - the remaining behaviour is similar to
//     * {@link #prepareForExecAllNodes()}.
//     * 
//     * @param nodeID the node's ID which is ultimately to be executed.
//     */
//    public synchronized void prepareForExecUpToNode(final int nodeID) {
//        NodeContainer nc = m_nodeContainerByID.get(nodeID);
//        prepareForExecUpToNode(nc);
//    }
//
//    /**
//     * Private routine to mark this NodeContainer and all predecessors requiring
//     * execution as EXECUTABLE or WAITING_TO_BE_EXECUTABLE. Calls itself
//     * recursively until all preceeding nodes are either executed or their state
//     * is set appropriately.
//     * 
//     * @param n a node container
//     */
//    private synchronized void prepareForExecUpToNode(final NodeContainer n) {
//        if (!(n.getNode().isExecuted())
//                && (n.getState() != NodeContainer.State.WaitingForExecution)
//                && (n.getState() != NodeContainer.State.CurrentlyExecuting)) {
//            // node is not already executed (or waiting to be) - set flag
//            // according to the underlying Node's "isExecutable" status.
//            if (n.getNode().isExecutable()) {
//                n.setState(NodeContainer.State.IsExecutable);
//            } else {
//                n.setState(NodeContainer.State.WaitingToBeExecutable);
//            }
//
//            // process all predecessors (only if this node was not executed!)
//            for (NodeContainer pred : n.getPredecessors()) {
//                if (pred != null) {
//                    if (m_idsByNode.containsKey(pred.getNode())) {
//                        prepareForExecUpToNode(pred);
//                    } else if (m_parent != null) {
//                        m_parent.prepareForExecUpToNode(pred);
//                    } else {
//                        throw new IllegalStateException("The node #"
//                                + pred.getID() + "(" + pred.nodeToString()
//                                + ")"
//                                + " is not part of this workflow manager or"
//                                + " its parent manager");
//                    }
//                } else {
//                    LOGGER.error(n.getNameWithID()
//                            + " is not executable: check connections");
//                }
//            }
//        }
//    }
//
//
//
//
//
//    private boolean checkForExecutableNodes() {
//        // check if there are any new nodes that need to be run:
//        int newExecutables = 0, runningNodes = 0;
//        for (NodeContainer nc : m_nodeContainerByID.values()) {
//            NodeContainer.State s = nc.getState();
//            
//            if ((s == NodeContainer.State.WaitingToBeExecutable)
//                    && (nc.getNode().isExecutable())) {
//                nc.setState(NodeContainer.State.IsExecutable);
//                newExecutables++;
//            } else if ((nc.isAutoExecutable())
//                    && (s == NodeContainer.State.Idle)
//                    && (nc.getNode().isExecutable())) {
//                nc.setState(NodeContainer.State.IsExecutable);
//                newExecutables++;
//            } else if (s == NodeContainer.State.IsExecutable) {
//                newExecutables++;
//            } else if ((s == NodeContainer.State.CurrentlyExecuting)
//                    || (s == NodeContainer.State.WaitingForExecution)) {
//                runningNodes++;
//            }
//        }
//
//        if (newExecutables > 0) {
//            fireWorkflowEvent(
//                    new WorkflowEvent.ExecPoolChanged(-1, null, null));
//        } else if (runningNodes == 0) {
//            if ((m_parent == null) || !m_parent.checkForExecutableNodes()) {
//                LOGGER.info("Workflow Pool done.");
//                // reset all flags to IDLE (just in case we missed some)
//                for (NodeContainer nc : m_nodeContainerByID.values()) {
//                    nc.setState(NodeContainer.State.Idle);
//                }            
//                fireWorkflowEvent(new WorkflowEvent.ExecPoolDone(-1, null,
//                        null));
//                return false;
//            }
//        }
//
//        return true;
//    }
//
//
//    /**
//     * Returns the one and only executor for this workflow.
//     * 
//     * @return a workflow executor
//     */
//    public WorkflowExecutor getExecutor() {
//        if (m_executor == null) {
//            m_executor = new DefaultWorkflowExecutor();
//        }
//        return m_executor;
//    }
//
//    /**
//     * Default implementation of a workflow executor. Please note that there
//     * must not be more than one registered workflow executor per workflow.
//     * Otherwise the end of execution might not be detected correctly and the
//     * thread that called {@link #executeAll()} or {@link #executeUpToNode(int)}
//     * might be blocked.
//     * 
//     * @author M. Berthold, University of Konstanz
//     * @author Thorsten Meinl, University of Konstanz
//     */
//    private class DefaultWorkflowExecutor implements WorkflowExecutor,
//            WorkflowListener, NodeStateListener {
//        private CountDownLatch m_execDone = new CountDownLatch(0);
//
//        private final AtomicInteger m_runningNodes = new AtomicInteger();
//
//        private NodeProgressMonitor m_progressMonitor = 
//            new DefaultNodeProgressMonitor();
//
//        /**
//         * Create executor class and register as listener for events.
//         */
//        public DefaultWorkflowExecutor() {
//            WorkflowManager.this.addListener(this);
//        }
//
//        /**
//         * Execute all nodes in workflow - return when all nodes are executed
//         * (or at least Workflow claims to be done).
//         */
//        public synchronized void executeAll() {
//            m_execDone = new CountDownLatch(1);
//            prepareForExecAllNodes();
//            startExecution();
//            try {
//                m_execDone.await();
//            } catch (InterruptedException ex) {
//                // nothing to do
//            }
//            System.out.println();
//        }
//
//        /**
//         * Execute all nodes in workflow leading to a certain node. Return when
//         * all nodes are executed (or at least Workflow claims to be done).
//         * 
//         * @param nodeID id of node to be executed.
//         */
//        public synchronized void executeUpToNode(final int nodeID) {
//            m_execDone = new CountDownLatch(1);
//            prepareForExecUpToNode(nodeID);
//            startExecution();
//            try {
//                m_execDone.await();
//            } catch (InterruptedException ex) {
//                // nothing to do
//            }
//        }
//
//        /**
//         * Starts additional nodes when workflow has changed, stops execution
//         * when workflow is done.
//         * 
//         * @param event the WorkflowEvent to be handled
//         */
//        public void workflowChanged(final WorkflowEvent event) {
//            if (event instanceof WorkflowEvent.ExecPoolChanged) {
//                NodeContainer nextNode = null;
//                while ((nextNode = getNextExecutableNode()) != null) {
//                    nextNode.addListener(this);
//                    m_runningNodes.incrementAndGet();
//                    nextNode.startExecution(m_progressMonitor);
//                }
//            }
//        }
//
//        /**
//         * @see de.unikn.knime.core.node.NodeStateListener
//         *      #stateChanged(de.unikn.knime.core.node.NodeStatus, int)
//         */
//        public void stateChanged(final NodeStatus state, final int id) {
//            if (state instanceof NodeStatus.EndExecute) {
//                getNodeContainerById(id).removeListener(this);
//                if (m_runningNodes.decrementAndGet() <= 0) {
//                    m_execDone.countDown();
//                }
//            }
//        }
//
//        /**
//         * @see de.unikn.knime.core.node.workflow.WorkflowExecutor
//         *      #setProgressMonitor(NodeProgressMonitor)
//         */
//        public void setProgressMonitor(final NodeProgressMonitor monitor) {
//            m_progressMonitor = monitor;
//        }
//    }
}
