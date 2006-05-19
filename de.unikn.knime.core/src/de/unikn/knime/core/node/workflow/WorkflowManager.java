/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import de.unikn.knime.core.eclipseUtil.GlobalClassCreator;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.Node;
import de.unikn.knime.core.node.NodeFactory;
import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.NodeStateListener;
import de.unikn.knime.core.node.NodeStatus;
import de.unikn.knime.core.node.meta.MetaInputNode;
import de.unikn.knime.core.node.meta.MetaInputNodeContainer;
import de.unikn.knime.core.node.meta.MetaInputNodeFactory;
import de.unikn.knime.core.node.meta.MetaNode;
import de.unikn.knime.core.node.meta.MetaNodeContainer;
import de.unikn.knime.core.node.meta.MetaNodeFactory;
import de.unikn.knime.core.node.meta.MetaOutputNode;
import de.unikn.knime.core.node.meta.MetaOutputNodeContainer;
import de.unikn.knime.core.node.meta.MetaOutputNodeFactory;

/**
 * Manager for a workflow holding Nodes and the connecting edge information. The
 * information is stored in a graph based data structure and allows to access
 * predecessors and successors. For performance reasons this implementation is
 * specific to vertices being of type <code>de.unikn.knime.dev.node.Node</code>
 * and (directed) edges connecting ports indicated by indices.
 * 
 * @author M. Berthold, University of Konstanz
 * @author Florian Georg, University of Konstanz
 */
public class WorkflowManager implements NodeStateListener, WorkflowListener {

    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(WorkflowManager.class);

    // quick access to nodes by ID
    private final HashMap<Integer, NodeContainer> m_nodesByID;

    // quick access to IDs via Nodes
    private final HashMap<Node, Integer> m_nodesByNode;

    // quick access to connections
    private final HashMap<Integer, ConnectionContainer> m_connectionsByID;

    // internal variables to allow generation of unique indices
    private int m_runningNodeID = -1;

    private int m_runningConnectionID = -1;

    // change listener support (transient)
    private final transient ArrayList<WorkflowListener> m_eventListeners;

    /**
     * Create new Workflow.
     * 
     */
    public WorkflowManager() {
        m_nodesByID = new HashMap<Integer, NodeContainer>();
        m_nodesByNode = new HashMap<Node, Integer>();
        m_connectionsByID = new HashMap<Integer, ConnectionContainer>();
        m_eventListeners = new ArrayList<WorkflowListener>();
    }

    /**
     * Create new Workflow from NodeSettings.
     * 
     * @param settings Workflow read from.
     * @throws InvalidSettingsException If the settings could not be loaded.
     */
    public WorkflowManager(final NodeSettings settings)
            throws InvalidSettingsException {
        // re-initialize the storage for nodes and connections
        m_nodesByID = new HashMap<Integer, NodeContainer>();
        m_nodesByNode = new HashMap<Node, Integer>();
        m_connectionsByID = new HashMap<Integer, ConnectionContainer>();
        m_eventListeners = new ArrayList<WorkflowListener>();
        // read name
        // read running ids for new nodes and connections
        m_runningNodeID = settings.getInt(KEY_RUNNING_NODE_ID);
        m_runningConnectionID = settings.getInt(KEY_RUNNING_CONN_ID);
        // read nodes

        NodeSettings nodes = settings.getConfig(KEY_NODES); // Node-Subconfig
        // object
        // get all keys in there
        for (String nodeKey : (Set<String>)nodes.keySet()) {
            NodeSettings nodeSetting = null;
            try {
                // retrieve config object for each node
                nodeSetting = nodes.getConfig(nodeKey);
                // create NodeContainer based on NodeSettings object
                NodeContainer newNode = null;

                // get the factory to decide which node to instantiate
                String factoryClassName = nodeSetting
                        .getString(Node.CONFIG_FACTORY_KEY);

                // use global Class Creator utility for Eclipe "compatibility"

                NodeFactory factory = null;
                try {
                    factory = (NodeFactory)((GlobalClassCreator
                            .createClass(factoryClassName)).newInstance());
                } catch (Exception e) {
                    throw new InvalidSettingsException(e.getMessage());
                }
                if (factory instanceof MetaNodeFactory) {
                    newNode = MetaNodeContainer
                            .createNewNodeContainer(nodeSetting);
                } else if (factory instanceof MetaInputNodeFactory) {
                    newNode = MetaInputNodeContainer
                            .createNodeContainer(nodeSetting);
                } else if (factory instanceof MetaOutputNodeFactory) {
                    newNode = MetaOutputNodeContainer
                            .createNodeContainer(nodeSetting);
                } else {
                    newNode = NodeContainer.createNewNodeContainer(nodeSetting);
                }
                // and add it to workflow
                this.addNodeWithID(newNode);
            } catch (InvalidSettingsException ise) {
                LOGGER.warn("Could not create node " + nodeKey + " reason: "
                        + ise.getMessage());
                LOGGER.debug(nodeSetting, ise);
            }
        }
        // read connections
        NodeSettings connections = settings.getConfig(KEY_CONNECTIONS);
        for (String connectionKey : (Set<String>)connections.keySet()) {
            // retrieve config object for next connection
            NodeSettings connectionConfig = connections
                    .getConfig(connectionKey);
            // and add appropriate connection to workflow
            try {
                ConnectionContainer.insertNewConnectionIntoWorkflow(
                        connectionConfig, this);
            } catch (InvalidSettingsException ise) {
                LOGGER.warn("Could not create connection " + connectionKey
                        + " reason: " + ise.getMessage());
                LOGGER.debug(connectionConfig, ise);
            }
        }
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
            final int positionChangeMultiplier) throws InvalidSettingsException {

        NodeSettings nodes = settings.getConfig(KEY_NODES); // Node-Subconfig

        // the new ids to return
        ArrayList<Integer> newIds = new ArrayList<Integer>();

        // the hash map is used to map the old node id to the new one
        HashMap<Integer, Integer> idMap = new HashMap<Integer, Integer>();

        for (String nodeKey : (Set<String>)nodes.keySet()) {
            NodeSettings nodeSetting = null;
            try {
                // retrieve config object for each node
                nodeSetting = nodes.getConfig(nodeKey);
                // create NodeContainer based on NodeSettings object
                NodeContainer newNode = null;

                // get the factory to decide which node to instantiate
                String factoryClassName = nodeSetting
                        .getString(Node.CONFIG_FACTORY_KEY);

                // use global Class Creator utility for Eclipe "compatibility"

                NodeFactory factory = null;
                try {
                    factory = (NodeFactory)((GlobalClassCreator
                            .createClass(factoryClassName)).newInstance());
                } catch (Exception e) {
                    throw new InvalidSettingsException(e.getMessage());
                }
                if (factory instanceof MetaNodeFactory) {
                    newNode = MetaNodeContainer
                            .createNewNodeContainer(nodeSetting);
                } else if (factory instanceof MetaInputNodeFactory) {
                    newNode = MetaInputNodeContainer
                            .createNodeContainer(nodeSetting);
                } else if (factory instanceof MetaOutputNodeFactory) {
                    newNode = MetaOutputNodeContainer
                            .createNodeContainer(nodeSetting);
                } else {
                    newNode = NodeContainer.createNewNodeContainer(nodeSetting);
                }

                // change the id, as this id is already in use (it was copied)
                // first remeber the old id "map(oldId, newId)"

                // remember temporarily the old id
                int oldId = newNode.getID();

                // create new id
                int newId = ++m_runningNodeID;
                idMap.put(new Integer(newNode.getID()), new Integer(newId));
                newNode.changeId(newId);
                // remember the new id for the return value
                newIds.add(new Integer(newId));

                // finaly change the extra info so that the copies are
                // located differently
                newNode.getExtraInfo().changePosition(
                        40 * positionChangeMultiplier);

                // set the user name to the new id if the init name
                // was set before e.g. "Node_44"

                // get set username
                String currentUserNodeName = newNode.getUserName();

                // create temprarily the init user name of the copied node
                // to check wether the current name was changed
                String oldInitName = "Node " + (oldId + 1);
                if (oldInitName.equals(currentUserNodeName)) {
                    newNode.setUserName("Node " + (newId + 1));
                }

                // and add it to workflow
                this.addNodeWithID(newNode);

            } catch (InvalidSettingsException ise) {
                LOGGER.warn("Could not create node " + nodeKey + " reason: "
                        + ise.getMessage());
                LOGGER.debug(nodeSetting, ise);
            }
        }
        // read connections
        NodeSettings connections = settings.getConfig(KEY_CONNECTIONS);
        for (String connectionKey : (Set<String>)connections.keySet()) {
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

                Integer newSourceId = idMap.get(new Integer(oldSourceID));
                Integer newTargetId = idMap.get(new Integer(oldTargetID));

                // check if both (source and target node have been selected
                // if not, the connection is omitted
                if (newSourceId == null || newTargetId == null) {

                    continue;
                }

                // create the new connection
                int newId = ConnectionContainer
                        .insertNewConnectionIntoWorkflow(connectionConfig,
                                this, newSourceId, newTargetId);

                // add the id to the new ids
                newIds.add(new Integer(newId));

            } catch (InvalidSettingsException ise) {
                LOGGER.warn("Could not create connection " + connectionKey
                        + " reason: " + ise.getMessage());
                LOGGER.debug(connectionConfig, ise);
            }
        }

        int[] idArray = new int[newIds.size()];
        int i = 0;
        for (Integer newId : newIds) {
            idArray[i] = newId.intValue();
            i++;
        }

        return idArray;

    }

    /**
     * Creates a new node from the given factory, adds the node to the workflow
     * and returns the corresponding <code>NodeContainer</code>.
     * 
     * @param factory the factory to create the node
     * @return the <code>NodeContainer</code> representing the created node
     */
    public NodeContainer createNode(final NodeFactory factory) {
        Node node;

        if (factory instanceof MetaInputNodeFactory) {
            node = new MetaInputNode(factory);
            LOGGER.debug("adding meta input node '" + node
                    + "' to the workflow...");
            int id = addNode(node);
            LOGGER.debug("done, ID=" + id);
        } else if (factory instanceof MetaOutputNodeFactory) {
            node = new MetaOutputNode(factory);
            LOGGER.debug("adding meta output node '" + node
                    + "' to the workflow...");
            int id = addNode(node);
            LOGGER.debug("done, ID=" + id);
        } else if (factory instanceof MetaNodeFactory) {
            node = new MetaNode(factory);
            LOGGER.debug("adding meta node '" + node + "' to the workflow...");
            int id = addNode(node);
            LOGGER.debug("done, ID=" + id);
        } else {
            node = new Node(factory);
            LOGGER.debug("adding node '" + node + "' to the workflow...");
            int id = addNode(node);
            LOGGER.debug("done, ID=" + id);
        }

        return getNodeContainer(node);
    }

    /**
     * adds a new node to the workflow.
     * 
     * @param n node to be added
     * @return internal, unique identifier
     */
    int addNode(final Node n) {
        if (m_nodesByNode.containsKey(n)) {
            throw new IllegalArgumentException(
                    "Node already managed by this workflow, "
                            + "can't add multiple times: " + n);
        }
        // create new ID
        m_runningNodeID++;
        assert (!m_nodesByID.containsKey(m_runningNodeID));
        // create new wrapper for this node

        NodeContainer newNode;
        if (n instanceof MetaNode) {
            newNode = new MetaNodeContainer(n, m_runningNodeID);
        } else if (n instanceof MetaInputNode) {
            newNode = new MetaInputNodeContainer(n, m_runningNodeID);
        } else if (n instanceof MetaOutputNode) {
            newNode = new MetaOutputNodeContainer(n, m_runningNodeID);
        } else {
            newNode = new NodeContainer(n, m_runningNodeID);
        }

        // add WorkflowManager as listener for state change events
        newNode.addListener(this);
        // and add it to our hashmap of nodes.
        m_nodesByID.put(m_runningNodeID, newNode);
        m_nodesByNode.put(n, m_runningNodeID);

        // notify listeners
        LOGGER.debug("Added " + newNode.getNodeNameWithID());
        fireWorkflowEvent(WorkflowEvent.NODE_ADDED, m_runningNodeID, null,
                newNode);

        return m_runningNodeID;
    }

    /**
     * removes a node from the workflow - note that this will require to node to
     * be disconnected completely.
     * 
     * @param container node to be removed
     */
    public synchronized void removeNode(final NodeContainer container) {

        Node n = container.getNode();
        Integer id = getNodeID(n);
        // tell node that it has been disconnected (close views...)
        n.detachNode();

        if (id != null) {
            NodeContainer nodeCont = m_nodesByID.get(id);

            // FG: First remove all connections
            disconnectNodeContainer(nodeCont);

            if (nodeCont != null) {
                nodeCont.removeAllListeners();

                m_nodesByID.remove(id);
                m_nodesByNode.remove(nodeCont.getNode());

                // notify listeners
                LOGGER.debug("Removed: " + nodeCont.getNodeNameWithID());
                fireWorkflowEvent(WorkflowEvent.NODE_REMOVED, ((Integer)id)
                        .intValue(), nodeCont, null);
            } else {
                LOGGER.error("Could not find (and remove): "
                        + nodeCont.getNodeNameWithID());
            }
        } else {
            LOGGER.error("Could not find (and remove): " + n.getNodeName());
            throw new IllegalArgumentException(
                    "Node not managed by this workflow: " + n);
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
        ArrayList<ConnectionContainer> connections = new ArrayList<ConnectionContainer>();
        // collect incoming connections
        for (int i = 0; i < numIn; i++) {
            ConnectionContainer c = getIncomingConnectionAt(nodeCont, i);
            if (c != null) {
                connections.add(c);
            }
        }
        // collect outgoing connections
        for (int i = 0; i < numOut; i++) {
            ConnectionContainer[] cArr = getOutgoingConnectionsAt(nodeCont, i);
            if (cArr != null) {
                connections.addAll(Arrays.asList(cArr));
            }
        }

        // remove all collected connections
        for (ConnectionContainer container : connections) {
            removeConnectionIfExists(container);
        }

    }

    /**
     * adds a new node to the workflow using the predefined identifier-int. If
     * the identifier is already in use an exception will be thrown.
     * 
     * FG: Do we really need this? Internal id manipulation should not be
     * exposed as public I think. MB: Let's leave it private then for now...
     * 
     * @param n node to be added
     * @param id predefined ID for the node - needs to be unique
     * @param ei The extra info to set
     * @throws IllegalArgumentException when the id already exists
     */
    private void addNodeWithID(final NodeContainer nc) {
        Integer id = new Integer(nc.getID());
        if (m_nodesByID.containsKey(nc.getNode())) {
            throw new IllegalArgumentException("duplicate ID");
        }
        if (m_nodesByNode.containsKey(nc.getNode())) {
            throw new IllegalArgumentException("duplicate/illegal node");
        }
        nc.addListener(this);
        m_nodesByID.put(id, nc);
        m_nodesByNode.put(nc.getNode(), id);

        // notify listeners
        LOGGER.debug("Added " + nc.getNodeNameWithID());
        fireWorkflowEvent(WorkflowEvent.NODE_ADDED, nc.getID(), null, nc);
    }

    /**
     * return ID of a node.
     * 
     * @param n node to retrieve id for
     * @return identifier of node or null if it does not exist
     */
    private Integer getNodeID(final Node n) {
        return m_nodesByNode.get(n);
    }

    /**
     * return a node for a specific identifier. Returns null if ID is not found,
     * throws an exception if something odd happens.
     * 
     * @param id the identifier of the node request
     * @return node matching the given ID
     */
    Node getNode(final int id) {
        NodeContainer nodeObj = m_nodesByID.get(id);
        if (nodeObj == null) {
            return null;
        }
        return nodeObj.getNode();
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
     * @throws IllegalStateException if port indices are invalid or the
     *             connection can't be added for some reason
     */
    public ConnectionContainer addConnection(final int idOut,
            final int portOut, final int idIn, final int portIn) {

        // find nodes in our tables
        NodeContainer nodeOut = m_nodesByID.get(idOut);
        NodeContainer nodeIn = m_nodesByID.get(idIn);
        if (nodeOut == null || nodeIn == null) {
            if (nodeOut != null) {
                throw new IllegalStateException("Node with id #" + idIn
                        + " not found.");
            }
            if (nodeIn != null) {
                throw new IllegalStateException("Node with id #" + idOut
                        + " not found.");
            }
            throw new IllegalStateException("Nodes with ids #" + idIn
                    + " and #" + idOut + " not found.");
        }

        // // check indices
        // if (!this.canAddConnection(idOut, portOut, idIn, portIn)) {
        // throw new InvalidSettingsException(
        // "Connection can't be added, check with"
        // + " 'canAddConnection' first");
        // }

        assert (idOut == nodeOut.getID());
        assert (idIn == nodeIn.getID());
        // add outgoing edge
        nodeOut.addOutgoingConnection(portOut, nodeIn);
        // add incoming edge
        nodeIn.addIncomingConnection(portIn, nodeOut);
        nodeIn.getNode().getNodeInPort(portIn).connectPort(
                nodeOut.getNode().getNodeOutPort(portOut));
        // create new ID
        m_runningConnectionID++;
        assert (!m_connectionsByID.containsKey(m_runningConnectionID));
        // create new connection, add it to list and return it
        ConnectionContainer newConnection = new ConnectionContainer(
                m_runningConnectionID, nodeOut, portOut, nodeIn, portIn);
        // add this manager as listener for workflow event
        newConnection.addWorkflowListener(this);
        m_connectionsByID.put(m_runningConnectionID, newConnection);

        // notify listeners
        LOGGER.debug("Added connection (from node id:" + idIn + ", port:"
                + portIn + " to node id:" + idOut + ", port:" + portOut + ")");
        fireWorkflowEvent(WorkflowEvent.CONNECTION_ADDED, -1, null,
                newConnection);

        return newConnection;
    }

    /**
     * deletes a connection between two nodes.
     * 
     * @param connection to be deleted
     */
    public synchronized void removeConnectionIfExists(
            final ConnectionContainer connection) {
        // if connection does not exist simply return
        if (!(m_connectionsByID.containsKey(connection.getID()))) {
            return;
        }

        // retrieve source and target node
        NodeContainer nodeOut = connection.getSource();
        int portOut = connection.getSourcePort();
        NodeContainer nodeIn = connection.getTarget();
        int portIn = connection.getTargetPort();
        // remove outgoing edge
        nodeOut.removeOutgoingConnection(portOut, nodeIn);
        // remove incoming edge
        nodeIn.removeIncomingConnection(portIn);
        // also disconnect the two underlying Nodes.
        nodeIn.getNode().getNodeInPort(portIn).disconnectPort();
        // finally remove connection from internal list
        m_connectionsByID.remove(connection.getID());

        // notify listeners
        LOGGER.info("Removed connection (from node id:" + nodeIn.getID()
                + ", port:" + portIn + " to node id:" + nodeOut.getID()
                + ", port:" + portOut + ")");
        fireWorkflowEvent(WorkflowEvent.CONNECTION_REMOVED, -1, connection,
                null);
    }

    // ////////////////////////
    // Routines for Execution
    // ////////////////////////

    /**
     * Mark all nodes that are not yet executed as "to be executed" and the ones
     * that are actually executable (all predecessors data is available) as
     * "runnable". The Workflow will then fire events when a status of a node
     * changes until all nodes that are/were runnable have been executed.
     */
    public synchronized void prepareForExecAllNodes() {
        int newExecutables = 0;
        for (NodeContainer thisNodeC : m_nodesByID.values()) {
            if (!(thisNodeC.getNode().isExecuted())
                    && (thisNodeC.getState() != NodeContainer.STATE_WAITING_FOR_EXECUTION)
                    && (thisNodeC.getState() != NodeContainer.STATE_CURRENTLY_EXECUTING)) {
                if (thisNodeC.getNode().isExecutable()) {
                    thisNodeC.setState(NodeContainer.STATE_IS_EXECUTABLE);
                    newExecutables++;
                } else {
                    thisNodeC
                            .setState(NodeContainer.STATE_WAITING_TO_BE_EXECUTABLE);
                }
            }
        }
        if (newExecutables > 0) {
            fireWorkflowEvent(WorkflowEvent.EXEC_POOL_CHANGED, -1, null, null);
        } else {
            fireWorkflowEvent(WorkflowEvent.EXEC_POOL_DONE, -1, null, null);
        }
    }

    /**
     * Mark all nodes that are neceesary for the specificed nodes to be
     * excecuted as "to be executed" - the remaining behaviour is similar to
     * <code>prepareForExecAllNodes()</code>.
     * 
     * @param nodeID the node's ID which is ultimately to be executed.
     */
    public synchronized void prepareForExecUpToNode(final int nodeID) {
        NodeContainer thisNodeC = m_nodesByID.get(nodeID);
        if (prepareForExecUpToNode(thisNodeC)) {
            fireWorkflowEvent(WorkflowEvent.EXEC_POOL_CHANGED, -1, null, null);
        } else {
            fireWorkflowEvent(WorkflowEvent.EXEC_POOL_DONE, -1, null, null);
        }
    }

    /*
     * private routine to mark this NodeContainer and all predecessors requiring
     * execution as EXECUTABLE or WAITING_TO_BE_EXECUTABLE. Calls itself
     * recursively until all preceeding nodes are either executed or their state
     * is set appropriately.
     * 
     * @return true if at least one node's EXECUTABLE flag was set
     */
    private synchronized boolean prepareForExecUpToNode(final NodeContainer n) {
        boolean foundExecutable = false;
        if (!(n.getNode().isExecuted())
                && (n.getState() != NodeContainer.STATE_WAITING_FOR_EXECUTION)
                && (n.getState() != NodeContainer.STATE_CURRENTLY_EXECUTING)) {
            // node is not already executed (or waiting to be) - set flag
            // according to the underlying Node's "isExecutable" status.
            if (n.getNode().isExecutable()) {
                n.setState(NodeContainer.STATE_IS_EXECUTABLE);
                foundExecutable = true;
            } else {
                n.setState(NodeContainer.STATE_WAITING_TO_BE_EXECUTABLE);
            }
            // process all predecessors (only if this node was not executed!)
            NodeContainer[] preNodes = n.getPredecessors();
            for (int i = 0; i < preNodes.length; i++) {
                if (preNodes[i] != null) {
                    foundExecutable = foundExecutable
                            | prepareForExecUpToNode(preNodes[i]);
                } else {
                    LOGGER.error(n.getNodeNameWithID()
                            + " is not executable: check connections");
                }
            }
        }
        return foundExecutable;
    }

    /*
     * find nodes that are now runnable (for example after some other node
     * terminated execution
     * 
     * @return int number of newly detected runnables
     */
    private synchronized int checkForNewExecutables() {
        int newExecutable = 0;
        for (NodeContainer thisNodeC : m_nodesByID.values()) {
            if ((thisNodeC.getState() == NodeContainer.STATE_WAITING_TO_BE_EXECUTABLE)
                    && (thisNodeC.getNode().isExecutable())) {
                thisNodeC.setState(NodeContainer.STATE_IS_EXECUTABLE);
                newExecutable++;
            }
            if ((thisNodeC.isAutoExecutable())
                    && (thisNodeC.getState() == NodeContainer.STATE_IDLE)
                    && (thisNodeC.getNode().isExecutable())) {
                thisNodeC.setState(NodeContainer.STATE_IS_EXECUTABLE);
                newExecutable++;
            }
        }
        return newExecutable;
    }

    /**
     * Cancel execution of all remaining nodes. Note that this is not a
     * guarantee that all remaining nodes RIGHT NOW will be canceled but all of
     * them will be asked to terminate. This routine requires the goodwill of
     * the implementations of the individual execute-routines.
     */
    public synchronized void cancelExecutionAllRemainingNodes() {
        int canceledNodes = 0; // how many could we cancel
        int currentlyInQueue = 0; // how many are already in the queue?
        int currentlyExecuting = 0; // how many are already executing?
        for (NodeContainer thisNodeC : m_nodesByID.values()) {
            if (thisNodeC.getState() == NodeContainer.STATE_IS_EXECUTABLE) {
                // node has not yet been started, simlpy change back to IDLE
                thisNodeC.setState(NodeContainer.STATE_IDLE);
                canceledNodes++;
            }
            if (thisNodeC.getState() == NodeContainer.STATE_WAITING_TO_BE_EXECUTABLE) {
                // node has not yet been started, simlpy change back to IDLE
                thisNodeC.setState(NodeContainer.STATE_IDLE);
                canceledNodes++;
            }
            if (thisNodeC.getState() == NodeContainer.STATE_WAITING_FOR_EXECUTION) {
                // these ones we need to cancel, since they have already
                // been returned a requester as "executable"
                thisNodeC.cancelExecution();
                currentlyInQueue++;
            }
            if (thisNodeC.getState() == NodeContainer.STATE_CURRENTLY_EXECUTING) {
                // these nodes are currently being executed, try to cancel.
                thisNodeC.cancelExecution();
                currentlyExecuting++;
            }
        }
        if ((currentlyExecuting == 0) && (currentlyInQueue == 0)) {
            // done. Otherwise we'll be done once the remaining ones return
            // however we made sure no new nodes are going to be executed.
            fireWorkflowEvent(WorkflowEvent.EXEC_POOL_DONE, -1, null, null);
        }
    }

    /**
     * Cancel execution of all remaining nodes after the specified node.
     * 
     * @param nodeID the node's ID after which excution is to be canceled.
     */
    public synchronized void cancelExecutionAfterNode(final int nodeID) {
        NodeContainer thisNodeC = m_nodesByID.get(nodeID);
        cancelExecutionAfterNode(thisNodeC);
        // check if any other nodes are either in the queue or already
        // executing (= not idle)
        int activeNodes = 0;
        for (NodeContainer nodeC : m_nodesByID.values()) {
            if (nodeC.getState() != NodeContainer.STATE_IDLE) {
                activeNodes++;
            }
        }
        if (activeNodes == 0) {
            // all nodes are idle, fire event that workflow pool is empty
            fireWorkflowEvent(WorkflowEvent.EXEC_POOL_DONE, -1, null, null);
        }
    }

    private synchronized void cancelExecutionAfterNode(final NodeContainer n) {
        // try to cancel this node
        if ((n.getState() == NodeContainer.STATE_WAITING_TO_BE_EXECUTABLE)) {
            // ok, we can simply change the node's flag
            n.setState(NodeContainer.STATE_IDLE);
        }
        if ((n.getState() == NodeContainer.STATE_IS_EXECUTABLE)) {
            // ok, we can simply change the node's flag
            n.setState(NodeContainer.STATE_IDLE);
        }
        if ((n.getState() == NodeContainer.STATE_WAITING_FOR_EXECUTION)) {
            // more complicated, we need to notify the node's progress monitor
            // that we would like to cancel execution
            n.cancelExecution();
        }
        if ((n.getState() == NodeContainer.STATE_CURRENTLY_EXECUTING)) {
            // more complicated, we need to notify the node's progress monitor
            // that we would like to cancel execution
            n.cancelExecution();
        }
        // and also try to cancel all successors
        NodeContainer[][] nodes = n.getSuccessors();
        for (int i = 0; i < nodes.length; i++) {
            NodeContainer[] portNodes = nodes[i];
            for (int j = 0; j < portNodes.length; j++) {
                cancelExecutionAfterNode(portNodes[j]);
            }
        }
    }

    /**
     * Return next available Node which needs to be executed. In theory at some
     * point in time this may incorporate some clever sorting mechanism, right
     * now it returns runnable nodes in some rather arbitrary, non-deterministic
     * order. Note that a return value of null only means that right now there
     * is no runnable node - there may be one later on when another node sends
     * an event indicating that it is done executing. The final end of execution
     * is indicated by the appropriate workflow event.
     * 
     * TODO: right now an executable node is only returned once even if it is
     * never actually executed! We need a way to have a watchdog timer that
     * resets these flags if nothing has happened for "too long"...
     * 
     * @return next runnable node or null of none is currently (!) available.
     */
    public synchronized NodeContainer getNextExecutableNode() {
        // right now just look for next runnable node from start every time
        for (NodeContainer thisNodeC : m_nodesByID.values()) {
            if ((thisNodeC.getState() == NodeContainer.STATE_IS_EXECUTABLE)
                    && (thisNodeC.getNode().isExecutable())) {
                thisNodeC.setState(NodeContainer.STATE_WAITING_FOR_EXECUTION);
                LOGGER.debug("returning node id=" + thisNodeC.getID()
                        + " as next available executable.");
                return thisNodeC;
            }
        }
        // didn't find any runnable node: return null.
        return null;
    }

    /**
     * Callback for NodeContainer state-changed events. Update pool of
     * executable nodes if we are in "workflow execution" mode.
     * 
     * @param state The node state event type.
     * @param nodeID The node's ID.
     */
    public synchronized void stateChanged(final NodeStatus state,
            final int nodeID) {
        // first reset action status of this node
        NodeContainer changedNode = m_nodesByID.get(nodeID);
        int stateId = state.getStatusId();
        assert (changedNode != null);
        // if this is an event indicating the start of a node's execution:
        if (stateId == NodeStatus.START_EXECUTE) {
            // change state from WAITING_FOR_EXECUTION to CURRENTLY_EXECUTING
            assert (changedNode.getState() == NodeContainer.STATE_WAITING_FOR_EXECUTION);
            changedNode.setState(NodeContainer.STATE_CURRENTLY_EXECUTING);
        }
        // if this is an event indicating the end of a node's execution:
        if (stateId == NodeStatus.END_EXECUTE) {
            // change state from CURRENTLY_EXECUTING to IDLE
            // assert (changedNode.getState() ==
            // NodeContainer.STATE_CURRENTLY_EXECUTING);
            changedNode.setState(NodeContainer.STATE_IDLE);
            // now check if there are any new nodes that need to be run:
            if (checkForNewExecutables() > 0) {
                fireWorkflowEvent(WorkflowEvent.EXEC_POOL_CHANGED, -1, null,
                        null);
                return; // new nodes detected
            }
            // if not, check if there are some remaining that need to be run:
            for (NodeContainer thisNodeC : m_nodesByID.values()) {
                if ((thisNodeC.getState() == NodeContainer.STATE_CURRENTLY_EXECUTING)
                        || (thisNodeC.getState() == NodeContainer.STATE_WAITING_FOR_EXECUTION)) {
                    return; // still some active or executable nodes available
                }
            }
            // did not find any remaining nodes with an active action code: done
            fireWorkflowEvent(WorkflowEvent.EXEC_POOL_DONE, -1, null, null);
            LOGGER.info("Workflow Pool done.");
            // reset all flags to IDLE (just in case we missed some)
            for (NodeContainer thisNodeC : m_nodesByID.values()) {
                thisNodeC.setState(NodeContainer.STATE_IDLE);
            }
        }
        if (stateId == NodeStatus.RESET) {
            fireWorkflowEvent(WorkflowEvent.NODE_RESET, nodeID, null, null);
        }
        if (stateId == NodeStatus.CONFIGURED) {
            fireWorkflowEvent(WorkflowEvent.NODE_CONFIGURED, nodeID, null, null);
        }
        if (stateId == NodeStatus.STATUS_EXTRA_INFO_CHANGED) {
            fireWorkflowEvent(WorkflowEvent.NODE_EXTRAINFO_CHANGED, nodeID,
                    null, null);
        }
    }

    /**
     * Callback for Workflow events.
     * 
     * @param event The thrown event
     */
    public void workflowChanged(final WorkflowEvent event) {
        if (event.getEventType() == WorkflowEvent.CONNECTION_EXTRAINFO_CHANGED) {
            // just forward the event
            fireWorkflowEvent(event.getEventType(), event.getID(), event
                    .getOldValue(), event.getNewValue());
        }
    }

    // ////////////////////////
    // Save/Load of Workflows
    // ////////////////////////

    // list of keywords to save&load settings
    // FG: made public to enable others to use WFM config

    /** Key for current running node id. */
    public static final String KEY_RUNNING_NODE_ID = "runningNodeID";

    /** Key for current running connection id. */
    public static final String KEY_RUNNING_CONN_ID = "runningConnectionID";

    /** Key for nodes. */
    public static final String KEY_NODES = "nodes";

    /** Key for connections. */
    public static final String KEY_CONNECTIONS = "connections";

    /**
     * Stores all workflow information into the given configuration. Note that
     * we have to store both nodes and connections and re-create them in that
     * order since connections reference IDs of nodes. Nodes themselves will not
     * store their predecessors or successors.
     * 
     * @param settings the configuration the current settings are written to.
     * @see #load
     */
    public void save(final NodeSettings settings) {

        // save name
        // save current running ids
        settings.addInt(KEY_RUNNING_NODE_ID, m_runningNodeID);
        settings.addInt(KEY_RUNNING_CONN_ID, m_runningConnectionID);
        // save nodes in an own sub-config object as a series of configs
        NodeSettings nodes = settings.addConfig(KEY_NODES);
        for (NodeContainer nextNode : m_nodesByID.values()) {
            // and save it to it's own config object
            NodeSettings nextNodeConfig = nodes.addConfig("node_"
                    + nextNode.getID());
            nextNode.save(nextNodeConfig);
            // TODO notify about node settings saved ????
        }
        // save connections in an own sub-config object as a series of configs
        NodeSettings connections = settings.addConfig(KEY_CONNECTIONS);
        for (ConnectionContainer nextConnection : m_connectionsByID.values()) {
            // and save it to it's own config object
            NodeSettings nextConnectionConfig = connections
                    .addConfig("connection_" + nextConnection.getID());
            nextConnection.save(nextConnectionConfig);
            // TODO notify about connection settings saved ????
        }
    }

    /**
     * Read workflow setup from configuration object. Nodes will be read first
     * and then used to reconstruct the connections between them since the nodes
     * do not store their connections.
     * 
     * @param settings the configuration object
     * @return A new Workflow object initialized by the given NodeSettings.
     * @throws InvalidSettingsException when a key is missing
     */
    public static WorkflowManager load(final NodeSettings settings)
            throws InvalidSettingsException {
        return new WorkflowManager(settings);
    }

    /**
     * Returns the amount of nodes currently managed by this instance.
     * 
     * @return Number of managed nodes.
     */
    public int getNumNodes() {
        return m_nodesByID.keySet().size();
    }

    /**
     * Returns all nodes currently managed by this instance.
     * 
     * @return All the managed node containers.
     */
    public NodeContainer[] getNodes() {
        return m_nodesByID.values().toArray(
                new NodeContainer[this.getNumNodes()]);
    }

    /**
     * Returns the node container that is handled by the manager for the given
     * node.
     * 
     * @param node The node
     * @return The container that wraps the node
     */
    NodeContainer getNodeContainer(final Node node) {
        NodeContainer cont = m_nodesByID.get(this.getNodeID(node));
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
        NodeContainer cont = m_nodesByID.get(new Integer(id));
        return cont;
    }

    /**
     * Returns the connection given by the ID.
     * 
     * @param id The id of the connection object to lookup
     * @return The connection container object, or <code>null</code>
     */
    public ConnectionContainer getConnectionByID(final int id) {
        return m_connectionsByID.get(id);
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
    public ConnectionContainer[] getOutgoingConnectionsAt(
            final NodeContainer container, final int portNum) {
        ArrayList<ConnectionContainer> foundConnections = new ArrayList<ConnectionContainer>();

        // If the node is contained, process it
        if (container != null) {
            // Find all outgoing connections for the given node
            for (ConnectionContainer conn : m_connectionsByID.values()) {
                // check if this connection affects the right node and port
                if ((conn.getSource().equals(container))
                        && (conn.getSourcePort() == portNum)) {
                    foundConnections.add(conn);
                }

            }

        } else {
            throw new IllegalArgumentException(
                    "The node is not contained in the workflow");
        }

        return foundConnections
                .toArray(new ConnectionContainer[foundConnections.size()]);

    }

    /**
     * Returns the incoming connection that exist at some in-port on some node.
     * 
     * @param container The node in the workflow
     * @param portNum Index of the in-port
     * @return Connection that is attached to the given in-port
     * @throws IllegalArgumentException If either nodeID or portNum is invalid.
     */
    public ConnectionContainer getIncomingConnectionAt(
            final NodeContainer container, final int portNum) {

        // If the node is contained, process it
        if (container != null) {
            // Find all outgoing connections for the given node
            for (ConnectionContainer conn : m_connectionsByID.values()) {
                // check if this connection affects the right node and port
                // if so, return the connection
                if ((conn.getTarget().equals(container))
                        && (conn.getTargetPort() == portNum)) {
                    return conn;
                }

            }

        } else {
            throw new IllegalArgumentException(
                    "The node is not contained in the workflow");
        }

        return null;

    }

    /**
     * Returns whether a connection can be added between the given nodes and
     * ports. This may return <code>false</code> if:
     * <ul>
     * <li> Some of the nodeIDs are invalid
     * <li> Some of the port-numbers are invalid
     * <li> There's already a connection that ends at the given in-port
     * <li> or (new) this connection would create a loop in the workflow
     * </ul>
     * 
     * @param sourceNode ID of the source node
     * @param outPort Index of the outgoing port
     * @param targetNode ID of the target node
     * @param inPort Index of the incoming port
     * @return <code>true</code> if a connection can be added,
     *         <code>false</code> otherwise.
     */
    public boolean canAddConnection(final int sourceNode, final int outPort,
            final int targetNode, final int inPort) {

        if ((sourceNode < 0) || (outPort < 0) || (targetNode < 0)
                || (inPort < 0)) {
            // easy sanity check failed - return false;
            return false;
        }

        Node src = this.getNode(sourceNode);
        Node targ = this.getNode(targetNode);

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

        ConnectionContainer conn = this.getIncomingConnectionAt(
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

    // /////////////////////////
    // Event Listener Handling
    // /////////////////////////

    /**
     * Adds a listener to the workflow, has no effekt if the listener is already
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
     * Removes a listener from the worklflow, has no effekt if the listener was
     * not registered before.
     * 
     * @param listener The listener to remove
     */
    public void removeListener(final WorkflowListener listener) {
        if (m_eventListeners.contains(listener)) {
            m_eventListeners.remove(listener);
        }

    }

    /**
     * Removes all listeners. For convenience.
     */
    public void removeAllListeners() {
        m_eventListeners.clear();

    }

    /**
     * Notifes all registered listeners of the event.
     * 
     */
    private void fireWorkflowEvent(final int type, final int id,
            final Object oldValue, final Object newValue) {
        for (int i = 0; i < m_eventListeners.size(); i++) {
            WorkflowListener listener = m_eventListeners.get(i);
            listener.workflowChanged(new WorkflowEvent(type, id, oldValue,
                    newValue));

        }
    }

    /**
     * @return The current running Connection ID
     */
    public int getRunningConnectionID() {
        return m_runningConnectionID;
    }

    /**
     * @return The current running Node ID
     */
    public int getRunningNodeID() {
        return m_runningNodeID;
    }

    /**
     * @return All connections that are defined inside the workflow
     */
    public ConnectionContainer[] getAllConnections() {
        return m_connectionsByID.values().toArray(
                new ConnectionContainer[m_connectionsByID.values().size()]);
    }
}
