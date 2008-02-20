/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   14.03.2007 (mb/bw): created
 */
package org.knime.core.node.workflow;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.GenericNodeDialogPane;
import org.knime.core.node.GenericNodeFactory;
import org.knime.core.node.GenericNodeModel;
import org.knime.core.node.GenericNodeView;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.Node;
import org.knime.core.node.NodeInPort;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeOutPort;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.PortObject;
import org.knime.core.node.PortObjectSpec;
import org.knime.core.node.PortType;
import org.knime.core.node.GenericNodeFactory.NodeType;
import org.knime.core.node.workflow.ConnectionContainer.ConnectionType;
import org.knime.core.node.workflow.WorkflowPersistor.ConnectionContainerTemplate;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;

/**
 * Container holding nodes and connections of a (sub) workflow. In contrast
 * to previous implementations, this class will now handle all control, such
 * as transport of data and specs from node to subsequent nodes. That is, nodes
 * do not know their pre- or successors anymore.
 * A WorkflowManager can also play the role of a NodeContainer, thus
 * representing a metanode/subworkflow.
 *
 * @author M. Berthold/B. Wiswedel, University of Konstanz
 */
public final class WorkflowManager extends NodeContainer {

    /** my logger. */
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(WorkflowManager.class);

    /** Name of this workflow (usually displayed at top of the node figure). */
    private String m_name = "Workflow Manager";

    // Nodes held in this workflow:

    /** mapping from NodeID to Nodes. */
    private final TreeMap<NodeID, NodeContainer> m_nodes =
        new TreeMap<NodeID, NodeContainer>();
    
    // Connections (by node, source and destination). Note that meta
    // connections (in- and outgoing of this workflow) are also part
    // of these maps.
    
    /** mapping from source NodeID to set of outgoing connections. */
    private final TreeMap<NodeID, Set<ConnectionContainer>>
        m_connectionsBySource =
        new TreeMap<NodeID, Set<ConnectionContainer>>();
    /** mapping from destination NodeID to set of incoming connections. */
    private final TreeMap<NodeID, Set<ConnectionContainer>>
        m_connectionsByDest =
        new TreeMap<NodeID, Set<ConnectionContainer>>();

    // Ports of the workflow (empty if it is not a subworkflow):

    /** ports of this Metanode (both arrays can have 0 length!). */
    private final WorkflowInPort[] m_inPorts;
    private final WorkflowOutPort[] m_outPorts;
    
    // Misc members:
    
    /** for internal usage, holding output table references. */
    private final HashMap<Integer, ContainerTable> m_globalTableRepository;

    /** Listeners interested in status changes. */
    private final CopyOnWriteArrayList<WorkflowListener> m_wfmListeners;


    /**
     * Semaphore to make sure we never deal with inconsistent nodes within the
     * workflow. Changes to state or outputs (port/data) need to synchronize
     * against this so that nodes collecting input (states/specs/data) can make
     * sure that they look at one consistent "snapshot" of a workflow. This
     * semaphore will be used by all "connected" children of this node. Isolated
     * workflows create a new semaphore.
     */
    private Object m_dirtyWorkflow;

    /** The root of everything, a workflow with no in- or outputs.
     * This workflow holds the top level projects. */
    public static final WorkflowManager ROOT =
        new WorkflowManager(null, NodeID.ROOTID,
                new PortType[0], new PortType[0]);


    ///////////////////////
    // Constructors
    ///////////////////////

    /** Constructor - create new child workflow container with a parent,
     * a new ID, and the number and type of in/outports as specified.
     */
    private WorkflowManager(final WorkflowManager parent, final NodeID id,
            final PortType[] inTypes, final PortType[] outTypes) {
        super(parent, id);
        m_inPorts = new WorkflowInPort[inTypes.length];
        for (int i = 0; i < inTypes.length; i++) {
            m_inPorts[i] = new WorkflowInPort(i, inTypes[i]);
        }
        m_outPorts = new WorkflowOutPort[outTypes.length];
        for (int i = 0; i < outTypes.length; i++) {
            m_outPorts[i] = new WorkflowOutPort(i, outTypes[i]);
        }
        if (m_inPorts.length == 0 && m_outPorts.length == 0) {
            // this workflow is not connected to parent via any ports
            // (it is likely a project)
            // we can start a new table repository since there can not
            // be any dependencies...
            m_globalTableRepository = new HashMap<Integer, ContainerTable>();
            // ...and we do not need to synchronize across unconnected workflows
            m_dirtyWorkflow = new Object();
        } else {
            // otherwise we may have incoming and/or outgoing dependencies...
            m_globalTableRepository = parent.m_globalTableRepository;
            // ...synchronize across border
            m_dirtyWorkflow = parent.m_dirtyWorkflow;
        }
        // add sets for this (meta-) node's in- and output connections
        m_connectionsByDest.put(getID(), new HashSet<ConnectionContainer>());
        m_connectionsBySource.put(getID(), new HashSet<ConnectionContainer>());
        // initialize listener list
        m_wfmListeners = new CopyOnWriteArrayList<WorkflowListener>();
        // done.
        LOGGER.info("Created subworkflow " + this.getID());
    }
    
    /** Constructor - create new workflow from persistor.
     */
    private WorkflowManager(final WorkflowManager parent, final NodeID id,
            final WorkflowPersistor persistor) {
        super(parent, id, persistor.getMetaPersistor());
        m_name = persistor.getName();
        WorkflowInPort[] ins = persistor.getInPorts();
        m_inPorts = new WorkflowInPort[ins.length];
        System.arraycopy(ins, 0, m_inPorts, 0, ins.length);
        WorkflowOutPort[] outs = persistor.getOutPorts();
        m_outPorts = new WorkflowOutPort[outs.length];
        System.arraycopy(outs, 0, m_outPorts, 0, outs.length);
        // add set for this (meta-) node's in- and output connections
        m_connectionsByDest.put(getID(), new HashSet<ConnectionContainer>());
        m_connectionsBySource.put(getID(), new HashSet<ConnectionContainer>());
        m_dirtyWorkflow = m_inPorts.length == 0 
            && m_outPorts.length == 0 ? new Object() : parent.m_dirtyWorkflow;
        m_globalTableRepository = persistor.getGlobalTableRepository();
        m_wfmListeners = new CopyOnWriteArrayList<WorkflowListener>();
        LOGGER.info("Created subworkflow " + this.getID());
    }

    ///////////////////////////////////////
    // Node / Project / Metanode operations
    ///////////////////////////////////////
    
    /** Create new project - which is the same as creating a new subworkflow
     * at this level with no in- or outports.
     *
     * @return newly created workflow
     */
    public WorkflowManager createAndAddProject() {
        WorkflowManager wfm = createAndAddSubWorkflow(new PortType[0],
                new PortType[0]);
        LOGGER.info("Created project " + ((NodeContainer)wfm).getID());
        return wfm;
    }

    /** Remove a project - the same as remove node but we make sure it really
     * looks like a project (i.e. has no in- or outports).
     * 
     * @param id of the project to be removed.
     */
    public void removeProject(final NodeID id) {
        NodeContainer nc = m_nodes.get(id);
        if (nc instanceof WorkflowManager) {
            WorkflowManager wfm = (WorkflowManager)nc;
            if ((wfm.getNrInPorts() == 0) && (wfm.getNrOutPorts() == 0)) {
                // looks like a project, remove it
                removeNode(id);
                return;
            }
        }
        throw new IllegalArgumentException(
                "Node: " + id + " is not a project!");
    }

    /** Uses given Factory to create a new node and then adds new node to the
     * workflow manager. We will automatically find the next available free
     * index for the new node within the given prefix.
     *
     * @param factory NodeFactory used to create the new node
     * @return newly created (unique) NodeID
     */
    // FIXME: I don't like this type cast warning (and the ? for that matter!)
    public NodeID createAndAddNode(final GenericNodeFactory<?> factory) {
        NodeID newID;
        synchronized (m_dirtyWorkflow) {
            // TODO synchronize to avoid messing with running workflows!
            assert factory != null;
            // insert node
            newID = createUniqueID();
            SingleNodeContainer container = new SingleNodeContainer(this,
               new Node((GenericNodeFactory<GenericNodeModel>)factory), newID);
            addNodeContainer(container);
        }
        configure(newID);
        LOGGER.info("Added new node " + newID);
        notifyWorkflowListeners(
                new WorkflowEvent(WorkflowEvent.Type.NODE_ADDED,
                newID, null, null));
        return newID;
    }

    /** Check if specific node can be removed (i.e. is not currently being
     * executed or waiting to be).
     * 
     * @param nodeID id of node to be removed
     * @return true if node can safely be removed.
     */
    public boolean canRemoveNode(final NodeID nodeID) {
        synchronized (m_dirtyWorkflow) {
            // check to make sure we can safely remove this node
            NodeContainer nc = m_nodes.get(nodeID);
            if (nc == null) {
                return false;
            }
            if (nc.getState().executionInProgress()) {
                return false;
            }
        }
        return true;
    }
    
    /** Remove node if possible. Throws an exception if node is "busy" and can
     * not be removed at this time.
     *
     * @param nodeID id of node to be removed
     */
    public void removeNode(final NodeID nodeID) {
        synchronized (m_dirtyWorkflow) {
            // check to make sure we can safely remove this node
            if (!canRemoveNode(nodeID)) {
                throw new IllegalStateException("Node can not be removed");
            }
            // remove lists of in- and outgoing connections.
            while (m_connectionsByDest.get(nodeID).size() > 0) {
                ConnectionContainer toDel =
                    m_connectionsByDest.get(nodeID).iterator().next();
                removeConnection(toDel);
            }
            assert m_connectionsByDest.get(nodeID).size() == 0;
            m_connectionsByDest.remove(nodeID);
            while (m_connectionsBySource.get(nodeID).size() > 0) {
                ConnectionContainer toDel =
                    m_connectionsBySource.get(nodeID).iterator().next();
                removeConnection(toDel);
            }
            assert m_connectionsBySource.get(nodeID).size() == 0;
            m_connectionsBySource.remove(nodeID);
            // and finally remove node itself as well.
            m_nodes.remove(nodeID);
        }
        notifyWorkflowListeners(
                new WorkflowEvent(WorkflowEvent.Type.NODE_REMOVED,
                getID(), nodeID, null));
    }

    /** Creates new meta node. We will automatically find the next available
     * free index for the new node within this workflow.
     * @param inPorts types of external inputs (going into this workflow)
     * @param outPorts types of external outputs (exiting this workflow)
     * @return newly created WorflowManager
     */
    public WorkflowManager createAndAddSubWorkflow(final PortType[] inPorts,
            final PortType[] outPorts) {
        if (this == ROOT && (inPorts.length != 0 || outPorts.length != 0)) {
            throw new IllegalStateException("Can't create sub workflow on "
                + "root workflow manager, use createProject() instead");
        }
        NodeID newID;
        WorkflowManager wfm;
        synchronized (m_dirtyWorkflow) {
            newID = createUniqueID();
            wfm = new WorkflowManager(this, newID, inPorts, outPorts);
            addNodeContainer(wfm);
            LOGGER.info("Added new subworkflow " + newID);
        }
        notifyWorkflowListeners(new WorkflowEvent(WorkflowEvent.Type.NODE_ADDED,
                        newID, null, null));
        return wfm;
    }
    
    /** Creates new meta node from a persistor instance. 
     * @param persistor to read from 
     * @param newID new id to be used
     * @return newly created WorflowManager
     */
    WorkflowManager createSubWorkflow(final WorkflowPersistor persistor, 
            final NodeID newID) {
        if (!newID.hasSamePrefix(getID()) || m_nodes.containsKey(newID)) {
            throw new RuntimeException(
                    "Invalid or duplicate ID \"" + newID + "\"");
        }
        return new WorkflowManager(this, newID, persistor);
    }
    
    ////////////////////////////////////////////
    // Helper methods for Node/Workflow creation
    ////////////////////////////////////////////
    
    /**
     * Create a new, unique node ID. Should be run within a synchronized
     * block to avoid duplicates!
     * 
     * @return next available unused index.
     */
    private NodeID createUniqueID() {
        int nextIndex = 1;
        if (!m_nodes.isEmpty()) {
            NodeID lastID = m_nodes.lastKey();
            nextIndex = lastID.getIndex() + 1;
        }
        NodeID newID = new NodeID(this.getID(), nextIndex);
        assert !m_nodes.containsKey(newID);
        return newID;
    }
    
    /** Adds the NodeContainer to m_nodes and adds empty connection sets to
     * m_connectionsBySource and m_connectionsByDest.
     * @param nodeContainer new Container to add.
     */
    private void addNodeContainer(final NodeContainer nodeContainer) {
        if (this == ROOT && !(nodeContainer instanceof WorkflowManager)) {
            throw new IllegalStateException("Can't add ordinary node to root "
                    + "workflow, use createProject() first");
        }
        if (this == ROOT && (nodeContainer.getNrInPorts() != 0 
                || nodeContainer.getNrOutPorts() != 0)) {
            throw new IllegalStateException("Can't add sub workflow to root "
                    + " workflow, use createProject() instead");
        }
        NodeID id = nodeContainer.getID();
        synchronized (m_dirtyWorkflow) {
            assert !m_nodes.containsKey(id) : "\"" 
               + nodeContainer.getNameWithID() + "\" already contained in flow";
            m_nodes.put(id, nodeContainer);
            // and create Sets of in and outgoing connections
            m_connectionsBySource.put(id, new HashSet<ConnectionContainer>());
            m_connectionsByDest.put(id, new HashSet<ConnectionContainer>());
        }
    }

    ///////////////////////////
    // Connection operations
    ///////////////////////////

    /** Add new connection - throw Exception if the same connection
     * already exists.
     *
     * @param source node id
     * @param sourcePort port index at source node
     * @param dest destination node id
     * @param destPort port index at destination node
     * @return newly created Connection object
     * @throws IllegalArgumentException if connection already exists
     */
    public ConnectionContainer addConnection(final NodeID source,
            final int sourcePort, final NodeID dest,
            final int destPort) {
        return addConnection(source, sourcePort, dest, destPort, true);
    }

    /** Add new connection - throw Exception if the same connection
     * already exists.
     *
     * @param source node id
     * @param sourcePort port index at source node
     * @param dest destination node id
     * @param destPort port index at destination node
     * @param configure if true, configure destination node after insertions
     * @return newly created Connection object
     * @throws IllegalArgumentException if connection already exists
     */
    private ConnectionContainer addConnection(final NodeID source,
            final int sourcePort, final NodeID dest,
            final int destPort, final boolean configure) {
        assert source != null;
        assert dest != null;
        assert sourcePort >= 0;
        assert destPort >= 0;
        ConnectionContainer newConn = null;
        ConnectionType newConnType = null;
        NodeContainer sourceNC;
        NodeContainer destNC;
        synchronized (m_dirtyWorkflow) {
            if (!canAddConnection(source, sourcePort, dest, destPort)) {
                throw new IllegalArgumentException("Can not add connection!");
            }
            sourceNC = m_nodes.get(source);
            destNC = m_nodes.get(dest);
            // determine type of new connection:
            if ((sourceNC == null) && (destNC == null)) {
                newConnType = ConnectionType.WFMTHROUGH;
            } else if (sourceNC == null) {
                newConnType = ConnectionType.WFMIN;
            } else if (destNC == null) {
                newConnType = ConnectionType.WFMOUT;
            } else {
                newConnType = ConnectionType.STD;
            }
            // create new connection
            newConn = new ConnectionContainer(source, sourcePort,
                    dest, destPort, newConnType);
            // 1) try to insert it into set of outgoing connections
            Set<ConnectionContainer> outConns =
                m_connectionsBySource.get(source);
            if (!outConns.add(newConn)) {
                throw new IllegalArgumentException(
                "Connection already exists!");
            }
            // 2) insert connection into set of ingoing connections
            Set<ConnectionContainer> inConns = m_connectionsByDest.get(dest);
            if (!inConns.add(newConn)) {
                throw new IllegalArgumentException(
                "Connection already exists (oddly enough only as incoming)!");
            }
            // handle special cases with port reference chains (WFM border
            // crossing connections:
            if ((source.equals(getID())) && (dest.equals(getID()))) {
                // connection goes directly from workflow in to workflow outport
                assert newConnType == ConnectionType.WFMTHROUGH;
                getOutPort(destPort).setUnderlyingPort(
                        getWorkflowIncomingPort(sourcePort));
            } else if ((!dest.equals(getID()))
                    && (destNC instanceof WorkflowManager)) {
                // we are feeding data into a subworkflow
                WorkflowInPort wfmIPort
                        = ((WorkflowManager)destNC).getInPort(destPort);
                wfmIPort.setUnderlyingPort(sourceNC.getOutPort(sourcePort));
            } else if (dest.equals(getID())) {
                // we are feeding data out of the subworkflow
                assert newConnType == ConnectionType.WFMOUT;
                getOutPort(destPort).setUnderlyingPort(
                        sourceNC.getOutPort(sourcePort));
            }
        }
        // if so desired...
        if (configure) {
            // ...make sure the destination node is configured again (and all of
            // its successors if needed):
            configure(dest);
        }
        // and finally notify listeners
        notifyWorkflowListeners(new WorkflowEvent(
                WorkflowEvent.Type.CONNECTION_ADDED, null, null, newConn));
        LOGGER.info("Added new connection from node " + source
                + "(" + sourcePort + ")" + " to node " + dest
                + "(" + destPort + ")");
        return newConn;
    }

    /** Check if a new connection can be added.
     *
     * @param source node id
     * @param sourcePort port index at source node
     * @param dest destination node id
     * @param destPort port index at destination node
     * @return true if connection can be added.
     */
    public boolean canAddConnection(final NodeID source,
            final int sourcePort, final NodeID dest,
            final int destPort) {
        // get NodeContainer for source/dest - can be null for WFM-connections!
        NodeContainer sourceNode = m_nodes.get(source);
        NodeContainer destNode = m_nodes.get(dest);
        // sanity checks (index/null)
        if (!(source.equals(this.getID()) || (sourceNode != null))) {
            return false;  // source node exists or is WFM itself
        }
        if (!(dest.equals(this.getID()) || (destNode != null))) {
            return false;  // dest node exists or is WFM itself
        }
        if ((sourcePort < 0) || (destPort < 0)) {
            return false;  // port indices are >= 0
        }
        if (sourceNode != null) {
            if (sourceNode.getNrOutPorts() <= sourcePort) {
                return false;  // source Node index exists
            }
        } else {
            if (this.getNrInPorts() <= sourcePort) {
                return false;  // WFM inport index exists
            }
        }
        if (destNode != null) {
            if (destNode.getNrInPorts() <= destPort) {
                return false;  // dest Node index exists
            }
        } else {
            if (this.getNrOutPorts() <= destPort) {
                return false;  // WFM outport index exists
            }
        }
        // check for existence of a connection to the destNode/Port
        Set<ConnectionContainer> scc = m_connectionsByDest.get(dest);
        for (ConnectionContainer cc : scc) {
            if (cc.getDestPort() == destPort) {
                return false;
            }
        }
        // check type compatibility
        PortType sourceType = (sourceNode != null
            ? sourceNode.getOutPort(sourcePort).getPortType()
            : this.getInPort(sourcePort).getPortType());
        PortType destType = (destNode != null
            ? destNode.getInPort(destPort).getPortType()
            : this.getOutPort(destPort).getPortType());
        return destType.getPortObjectClass().isAssignableFrom(
                sourceType.getPortObjectClass());
    }

    /** Check if a connection can safely be removed.
     *
     * @param cc connection
     * @return true if connection cc is removable.
     */
    public boolean canRemoveConnection(final ConnectionContainer cc) {
        // make sure connection exists
        if (!m_connectionsByDest.get(cc.getDest()).contains(cc)) {
            return false;
        }
        if (!m_connectionsBySource.get(cc.getSource()).contains(cc)) {
            return false;
        }
        // retrieve state of source and destination NodeContainer (could be WFM)
        NodeContainer.State sourceState = cc.getSource().equals(this.getID())
            ? this.getState()
            : m_nodes.get(cc.getSource()).getState();
        NodeContainer.State destState = cc.getDest().equals(this.getID())
            ? this.getState()
            : m_nodes.get(cc.getDest()).getState();
        // make sure neither source nor destination are "in use"...
        if (!(sourceState.equals(NodeContainer.State.IDLE)
              || sourceState.equals(NodeContainer.State.CONFIGURED)
              || sourceState.equals(NodeContainer.State.EXECUTED))) {
            return false;
        }
        if (!(destState.equals(NodeContainer.State.IDLE)
              || destState.equals(NodeContainer.State.CONFIGURED)
              || destState.equals(NodeContainer.State.EXECUTED))) {
              return false;
          }
        // that's it, folks.
        return true;
    }

    /** Remove connection.
     *
     * @param cc connection
     */
    public void removeConnection(final ConnectionContainer cc) {
        synchronized (m_dirtyWorkflow) {
            if (!canRemoveConnection(cc)) {
                throw new IllegalArgumentException(
                        "Can not remove connection!");
            }
            // check type and underlying nodes
            NodeID source = cc.getSource();
            NodeID dest = cc.getDest();
            NodeContainer sourceNC = m_nodes.get(source);
            NodeContainer destNC = m_nodes.get(dest);
            assert (source.equals(this.getID())) || (sourceNC != null);
            assert (dest.equals(this.getID())) || (destNC != null);
            if ((sourceNC == null) && (destNC == null)) {
                assert cc.getType() == ConnectionType.WFMTHROUGH;
            } else if (sourceNC == null) {
                assert cc.getType() == ConnectionType.WFMIN;
            } else if (destNC == null) {
                assert cc.getType() == ConnectionType.WFMOUT;
            } else {
                assert cc.getType() == ConnectionType.STD;
            }
            // 1) try to delete it from set of outgoing connections
            Set<ConnectionContainer> outConns =
                m_connectionsBySource.get(source);
            if (!outConns.remove(cc)) {
                throw new IllegalArgumentException(
                "Connection does not exist!");
            }
            // 2) remove connection from set of ingoing connections
            Set<ConnectionContainer> inConns = m_connectionsByDest.get(dest);
            if (!inConns.remove(cc)) {
                throw new IllegalArgumentException(
                "Connection did not exist (it did exist as incoming conn.)!");
            }
            // handle special cases with port reference chains (WFM border
            // crossing connections:
            if ((source.equals(getID()))
                && (dest.equals(getID()))) {
                // connection goes directly from workflow in to workflow outport
                assert cc.getType() == ConnectionType.WFMTHROUGH;
                getOutPort(cc.getDestPort()).setUnderlyingPort(null);
            } else if ((!dest.equals(getID()))
                    && (destNC instanceof WorkflowManager)) {
                // we are feeding data into a subworkflow
                WorkflowInPort wfmIPort
                        = ((WorkflowManager)destNC).getInPort(cc.getDestPort());
                wfmIPort.setUnderlyingPort(null);
            } else if (dest.equals(getID())) {
                // we are feeding data out of the subworkflow
                assert cc.getType() == ConnectionType.WFMOUT;
                getOutPort(cc.getDestPort()).setUnderlyingPort(null);
            }
        }
        // and finally reset the destination node - since it has incomplete
        // incoming connections now...
        if (cc.getDest().equals(this.getID())) {
            // in case of WFM being disconnected make sure outside
            // successors are reset
            this.getParent().resetAndConfigureNode(this.getID());
        } else {
            // otherwise just reset successor, rest will be handled by WFM
            resetAndConfigureNode(cc.getDest());
        }
        notifyWorkflowListeners(
                new WorkflowEvent(WorkflowEvent.Type.CONNECTION_REMOVED,
                        null, null, cc));
    }

    /////////////////////////////////
    // Utility Connection Functions
    /////////////////////////////////
    
    /**
     * Returns the set of outgoing connections for the node with the passed id
     * at the specified port.
     *
     * @param id id of the node of interest
     * @param portIdx port index of that node
     * @return all outgoing connections for the passed node at the specified
     *  port
     */
    public Set<ConnectionContainer> getOutgoingConnectionsFor(final NodeID id,
            final int portIdx) {
        synchronized (m_dirtyWorkflow) {
            Set<ConnectionContainer> outConnections
                = m_connectionsBySource.get(id);
            Set<ConnectionContainer> outConsForPort
                = new HashSet<ConnectionContainer>();
            if (outConnections == null) {
                return outConsForPort;
            }
            for (ConnectionContainer cont : outConnections) {
                if (cont.getSourcePort() == portIdx) {
                    outConsForPort.add(cont);
                }
            }
            return outConsForPort;
        }
    }

    /**
     * Returns the incoming connection of the node with the passed node id at
     * the specified port.
     * @param id id of the node of interest
     * @param portIdx port index
     * @return incoming connection at that port of the given node
     */
    public ConnectionContainer getIncomingConnectionFor(final NodeID id,
            final int portIdx) {
        synchronized (m_dirtyWorkflow) {
            Set<ConnectionContainer>inConns = m_connectionsByDest.get(id);
            if (inConns != null) {
                for (ConnectionContainer cont : inConns) {
                    if (cont.getDestPort() == portIdx) {
                        return cont;
                    }
                }
            }
        }
        return null;
    }

    /////////////////////
    // Node Settings
    /////////////////////

    /** Load Settings into specified node.
     *
     * @param id of node
     * @param settings to be load by node
     * @throws InvalidSettingsException if settings are wrong
     * @throws IllegalArgumentException if node does not exist
     */
    public void loadNodeSettings(final NodeID id, final NodeSettingsRO settings)
    throws InvalidSettingsException {
        // TODO load setting for MetaNodeContainer/WorkflowManager-nodes
        // TODO synchronize
        NodeContainer nc = getNodeContainer(id);
        // TODO propagate reset to parent (what if "this" is sub flow)
        resetAndConfigureNode(id);
        nc.loadSettings(settings);
        configure(id);
    }

    /**
     * write node settings into Settings object.
     * 
     * @param id of node
     * @param settings to be saved to
     * @throws InvalidSettingsException thrown if nonsense is written
     */
    public void saveNodeSettings(final NodeID id, final NodeSettingsWO settings)
    throws InvalidSettingsException {
        NodeContainer nc = getNodeContainer(id);
        nc.saveSettings(settings);
    }
    
    ////////////////////////////
    // Execution of nodes
    ////////////////////////////

    /** (un)mark all nodes in the workflow (and all subworkflows!)
     * for execution (if they are not executed already). Once they are
     * configured (if not already) and all inputs are available they
     * will be queued "automatically" (right now: whenever
     * checkForQueuableNodes() is called).
     * 
     * @param flag indicate if we want to set or reset the flag
     */
    public void prepareForExecutionAll(final boolean flag) {
        synchronized (m_dirtyWorkflow) {
            for (NodeContainer nc : m_nodes.values()) {
                switch (nc.getState()) {
                case IDLE:
                case CONFIGURED:
                    nc.markForExecution(true);
                    break;
                case MARKEDFOREXEC:
                case UNCONFIGURED_MARKEDFOREXEC:
                    nc.markForExecution(false);
                default: // already running
                    // TODO other states - not really reason for warning?
                }
            }
        }
        checkForQueuableNodes();
    }

    /**
     * Reset all nodes in this workflow. Make sure the reset is propagated
     * in the right order, that is, only actively reset the "left most"
     * nodes in the workflow or the ones connected to meta node input
     * ports only(!).
     */
    public void resetAll() {
        synchronized (m_dirtyWorkflow) {
            // will contain source nodes (meta input nodes) or 0-input nodes
            ArrayList<NodeContainer> sourceNodes
                    = new ArrayList<NodeContainer>();
    
            // find all nodes that are directly connected to this meta nodes
            // input ports
            Set<ConnectionContainer> cons = m_connectionsByDest.get(getID());
    
            for (ConnectionContainer c : cons) {
                NodeID id = c.getDest();
                NodeContainer nc = m_nodes.get(id);
                if (nc != null) {
                    sourceNodes.add(nc);
                }
            }
            // look for nodes with no predecessor
            for (NodeContainer nc : m_nodes.values()) {
                if (nc.getNrInPorts() == 0) {
                    sourceNodes.add(nc);
                }
            }
            // and finally reset those nodes (all of their successors will be
            // reset automatically)
            for (NodeContainer nc : sourceNodes) {
                switch (nc.getState()) {
                case EXECUTED:
                    // TODO clean this up - resetNode() reconfigures for every node!
                    // it would be better to use the internal reset() instead.
                    nc.resetNode();
                    break;
                case MARKEDFOREXEC:
                case UNCONFIGURED_MARKEDFOREXEC:
                    nc.markForExecution(false);
                default: // nothing to do with "yellow" nodes
                }
            }
            // finally adjust state of WFM
            setNewState(State.CONFIGURED);
        }
    }

    /** mark this node and all not-yet-executed predecessors for execution.
     * They will be marked first, queued when all inputs are available and
     * finally executed.
     *
     * @param id node id
     */
    public void executeUpToHere(final NodeID id) {
        NodeContainer nc = getNodeContainer(id);
        if (!State.CONFIGURED.equals(nc.getState())) {
            throw new IllegalStateException("Can't execute " + getNodeString(nc)
                    + ", not in configured state, but " + nc.getState());
        }
        enableQueuingOfPredecessors(id);
        nc.markForExecution(true);
        // this could change some of the queuable states, so check:
        checkForQueuableNodes();
    }

    /** Recursively iterates the predecessors and marks them for execution.
     * @param id The node whose predecessors are to marked for execution.
     */
    private void enableQueuingOfPredecessors(final NodeID id) {
        Set<ConnectionContainer> predConn = m_connectionsByDest.get(id);
        for (ConnectionContainer c : predConn) {
            NodeID predID = c.getSource();
            NodeContainer predNC = m_nodes.get(predID);
            if (predNC == null) {
                assert c.getType().equals(
                        ConnectionContainer.ConnectionType.WFMIN);
                assert predID.equals(getID());
                getParent().enableQueuingOfPredecessors(getID());
            } else {
                switch (predNC.getState()) {
                case IDLE: throw new IllegalStateException("Can't execute \""
                        + predNC.getNameWithID() + "\", state is IDLE");
                case CONFIGURED:
                    enableQueuingOfPredecessors(predID);
                    predNC.markForExecution(true);
                    break;
                default: // already run or to be run
                }
            }
        }
    }

    /** Returns a description of a node container,
     * e.g. '"File Reader" (id "2.3.4")'.
     * @return Such a string to be used in error messages.
     */
    private String getNodeString(final NodeContainer nc) {
        return "\"" + nc.getName() + "\" (id \"" + nc.getID() + "\"";
    }

    /* -------------- State changing actions and testers ----------- */

    /** {@inheritDoc} */
    @Override
    boolean configureNode(final PortObjectSpec[] specs) {
        // remember old specs
        PortObjectSpec[] prevSpecs =
                new PortObjectSpec[getNrOutPorts()];
        for (int i = 0; i < prevSpecs.length; i++) {
            prevSpecs[i] = getOutPort(i).getPortObjectSpec();
        }
        // configure all "successors" of myself, that is, every node which
        // is connected to one of the metanode's inports.
        this.configureSuccessors(this.getID());
        // note that we do NOT need to configure nodes without ingoing
        // connections explicitly. Even though they will not be configured
        // as successors of our inports they have been configured before.

        // make sure we reflect any state changes inside this workflow also
        // in our own state:
        checkForNodeStateChanges();
        
        // compare old and new specs
        for (int i = 0; i < prevSpecs.length; i++) {
            PortObjectSpec newSpec =
                    getOutPort(i).getPortObjectSpec();
            if (newSpec != null) {
                if (!newSpec.equals(prevSpecs[i])) {
                    return true;
                }
            } else if (prevSpecs[i] != null) {
                return true; // newSpec is null!
            }
        }
        return false; // all specs stayed the same!
    }

    /** {@inheritDoc}
     *
     * This essentially only invokes {@link #markForExecution()} on all
     * underlying nodes.
     */
    @Override
    void markForExecution(final boolean flag) {
        if (flag) {  // mark for execution
            switch (getState()) {
            case EXECUTED:
                throw new IllegalStateException(
                     "Workflow is already completely executed.");
            case CONFIGURED:
                prepareForExecutionAll(true);
                setNewState(State.MARKEDFOREXEC);
            case IDLE:
                prepareForExecutionAll(true);
                setNewState(State.UNCONFIGURED_MARKEDFOREXEC);
            default:
                throw new IllegalStateException("Wrong WFM state "
                        + getState() + " in markForExecution(true)");
            }
        } else {
            switch (getState()) {
            case MARKEDFOREXEC:
                prepareForExecutionAll(false);
                setNewState(State.CONFIGURED);
            case UNCONFIGURED_MARKEDFOREXEC:
                prepareForExecutionAll(false);
                setNewState(State.IDLE);
            default:
                throw new IllegalStateException("Wrong WFM state "
                        + getState() + " in markForExecution(false)");
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    void queueNode(final PortObject[] inData) {
        switch (getState()) {
        case MARKEDFOREXEC:
            setNewState(State.QUEUED);
            break;
        default: throw new IllegalStateException(
                "State change to " + State.QUEUED + " not allowed, currently "
                + getState());
        }
    }

    /** call-back from NodeContainer called before node is actually
     * executed.
     *
     * @param nc node container which just finished execution in a JobExecutor
     */
    void doBeforeExecution(final NodeContainer nc) {
        synchronized (m_dirtyWorkflow) {
            if (nc instanceof WorkflowManager) {
                LOGGER.info(nc.getNameWithID() + " doBeforeExecute (WFM)");
            } else {
                LOGGER.info(nc.getNameWithID() + " doBeforeExecute (NC)");
            }
            switch (getState()) {
            case QUEUED:  // if the WorkflowManager itself was queued:
                // some nodes IN this WFM are executing
                setNewState(State.EXECUTING); 
                // disable outports (to avoid access to PortObject when
                // any inside Node.execute has written them but the State flag
                // of the WFM has not yet been updated
                for (WorkflowOutPort port : m_outPorts) {
                    port.hidePortObject(false);
                }
                if (allInternalNodesFinished()) {
                    doAfterExecution(this);
                }
            default:
                checkForNodeStateChanges();
            }
        }
    }

    /**
     * @return true if all internal nodes have finished execution, either
     * successfully.   
     */
    private boolean allInternalNodesFinished() {
        assert getState().equals(NodeContainer.State.EXECUTING);
        for (NodeContainer nc : m_nodes.values()) {
            if (nc.getState().executionInProgress()) {
                return false;
            }
        }
        return true;
    }

    /** cleanup a node after execution.
     *
     * @param nc NodeContainer which just finished execution
     */
    void doAfterExecution(final NodeContainer nc) {
        synchronized (m_dirtyWorkflow) {
            if (nc instanceof WorkflowManager) {
                LOGGER.info(nc.getNameWithID() + " doAfterExecute (WFM)");
            } else {
                LOGGER.info(nc.getNameWithID() + " doAfterExecute (NC)");
            }
            boolean canConfigureSuccessors = true;
            if (nc instanceof SingleNodeContainer) {
                SingleNodeContainer snc = (SingleNodeContainer)nc;
                Node node = snc.getNode();
                if (node.getLoopStatus() != null) {
                    // we are supposed to execute this loop again!
                    // first check if the loop is properly configured:
                    if (m_nodes.get(node.getLoopStatus().getOriginatingNode())
                            == null) {
                        // obviously not: origin of the loop is not in this WFM!
                        // FIXME: error from WFM!!!
                        // nothing else to do: NC stays configured
                        assert nc.getState() == NodeContainer.State.CONFIGURED;
                    } else {
                        // (1) clear stack (= loop context)
                        ScopeContext sc = node.getLoopStatus();
                        node.clearLoopStatus();
                        // (2) find all intermediate node, the loop's "body"
                        Set<NodeID> loopBodyNodes = findExecutedNodesInLoopBody(
                                sc.getOriginatingNode(), nc.getID());
                        // (3) reset the nodes in the body
                        for (NodeID id : loopBodyNodes) {
                            m_nodes.get(id).resetNode();
                        }
                        // (4) mark the origin of the loop to be executed again
                        NodeContainer origin 
                                    = m_nodes.get(sc.getOriginatingNode());
                        assert origin instanceof SingleNodeContainer;
                        ((SingleNodeContainer)origin).enableReQueuing();
                        // (5) enable the body to be queued as well.
                        for (NodeID id : loopBodyNodes) {
                            configure(id);
                            m_nodes.get(id).markForExecution(true);
                        }
                        // and finally (6) mark end of loop for re-execution
                        nc.markForExecution(true);
                        // make sure we do not accidentially configure the
                        // remainder of this node since we not yet done with
                        // the loop
                        canConfigureSuccessors = false;
                    }
                }
            }
            if (canConfigureSuccessors) {
                // standard behaviour - configure successors since this node
                // is done (either executed or failed) and has new specs (maybe)
                // Do not reconfigure this node if it failed - we will delete
                // any warnings/errors otherwise!
                configureSuccessors(nc.getID());
            }
            switch (getState()) {
            case EXECUTING:
                if (allInternalNodesFinished()) {
                    LOGGER.info("MetaNode " + this.getID() + " doAfterExecute");
                    // this ought to be an atomic operation:
                    // 1) set state to executed
                    setNewState(State.EXECUTED);
                    // 2) switch output ports "on" (to avoid access to
                    // PortObject when any inside Node.execute has written them
                    // but the State flag of the WFM has not yet been updated
                    for (WorkflowOutPort port : m_outPorts) {
                        port.hidePortObject(true);
                    }
                    // and finally run after execution stuff for this as node
                    if (getParent() != null) {
                        getParent().doAfterExecution(this);
                    }
                }
            default:
            }
        }
        if (nc instanceof SingleNodeContainer) {
            checkForQueuableNodes();
            checkForNodeStateChanges();
        }
    }

    /** {@inheritDoc} */
    @Override
    void resetNode() {
        resetAll();
    }

    /* ------------- node commands -------------- */

    /**
     * Check if a node can be reset, meaning that it is executed and all of
     * it's successors are idle or executed as well. We do not want to mess
     * with executing chains.
     * 
     * @param nodeID the id of the node
     * @return true if the node can safely be reset.
     */
    public boolean canResetNode(final NodeID nodeID) {
        // (a) this node is executed
        // (b) no successors is running or queued.
        return (this.getState() == NodeContainer.State.EXECUTED)
               && (successorsResetable(nodeID));
    }

    /**
     * Test if successors of a node are resetable (i.e. not currently
     * executing).
     * 
     * @param nodeID id of node
     * @return true of all successors are resetable
     */
    private boolean successorsResetable(final NodeID nodeID) {
        NodeContainer nc = m_nodes.get(nodeID);
        if (nc == null) {  // we are talking about this WFM
            assert nodeID == this.getID();
            return getParent().successorsResetable(nodeID);
        }
        // else it's a node inside the WFM
        for (ConnectionContainer cc : m_connectionsBySource.get(nodeID)) {
            NodeID succID = cc.getSource();
            NodeContainer succNC = m_nodes.get(succID);
            State succState;
            if (succNC == null) {
                succState = this.getState();
            } else {
                succState = succNC.getState();
            }
            if ((succState == State.CONFIGURED) || (succState == State.EXECUTED)
                    || (succState == State.IDLE)) {
                return successorsResetable(succID);
            }
        }
        return true;
    }

    //////////////////////////////////////////////////////////
    // NodeContainer implementations (WFM acts as meta node)
    //////////////////////////////////////////////////////////
    
    /** Reset node and all executed successors of a specific node.
    *
    * @param id of first node in chain to be reset.
    */
    public void resetAndConfigureNode(final NodeID id) {
        NodeContainer nc = m_nodes.get(id);
        assert nc != null;
        switch (nc.getState()) {
        case EXECUTING:
            throw new IllegalStateException(
                    "Can not reset executing node " + id);
        case QUEUED:
            throw new IllegalStateException("Can not reset queued node " + id);
        case EXECUTED:
            // reset all successors first
            this.resetSuccessors(id);
            // and then reset node itself
            m_nodes.get(id).resetNode();
            // and launch configure starting with this node
            configure(id);
        default: // ignore all other states (IDLE, CONFIGURED...)
        }
        checkForNodeStateChanges();
    }

    /** 
     * Reset successors of all nodes. Do not reset node itself since it
     * may be a metanode from within we started this.
     * 
     * @param id of node
     */
    private void resetSuccessors(final NodeID id) {
        Set<ConnectionContainer> succs = m_connectionsBySource.get(id);
        for (ConnectionContainer conn : succs) {
            NodeID currID = conn.getDest();
            if (currID != this.getID()) {
                // normal connection to another node within this workflow
                // first reset successors of successor
                this.resetSuccessors(currID);
                // ..then immediate successor itself
                m_nodes.get(currID).resetNode();
            } else {
                // connection goes to a meta outport!
                assert conn.getType()
                    == ConnectionContainer.ConnectionType.WFMOUT;
                // Note: this also resets node which are connected to OTHER
                // outports of this metanode!
                getParent().resetSuccessors(this.getID());
            }
        }
    }

    /** Check if a node can be executed directly.
     *
     * @param nodeID id of node
     * @return true of node is configured and all immediate predecessors are
     *              executed.
     */
    public boolean canExecuteNode(final NodeID nodeID) {
        synchronized (m_dirtyWorkflow) {
            NodeContainer nc = m_nodes.get(nodeID);
            if (nc == null) {
                return false;
            }
            // node itself needs to be configured.
            if (nc.getState() != NodeContainer.State.CONFIGURED) {
                return false;
            }
            // all immediate predecessors must be executed.
            Set<ConnectionContainer> predNodes
                         = m_connectionsByDest.get(nodeID);
            for (ConnectionContainer conn : predNodes) {
                assert conn.getDest() == nodeID;
                NodeID predNodeID = conn.getSource();
                NodeContainer predNode = m_nodes.get(predNodeID);
                // TODO fix workaround for incoming meta connections
                if (predNode != null && predNode.getState() 
                        != NodeContainer.State.EXECUTED) {
                    return false;
                }
                if (predNode == null) {
                    assert predNodeID == this.getID();
                    return getParent().canExecuteNode(predNodeID);
                }
            }
        }
        return true;
    }

    /** Execute one individual node (all predecessors must be executed
     * already!). This only queues the node for execution, others may be
     * executed before this one.
     *
     * @param nodeID id of node.
     */
    public void executeNode(final NodeID nodeID) {
        assert m_nodes.get(nodeID) != null;
        synchronized (m_dirtyWorkflow) {
            if (canExecuteNode(nodeID)) {
                m_nodes.get(nodeID).markForExecution(true);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public NodeMessage getNodeMessage() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    void cancelExecution() {
        assert false;
    }

    /////////////////////////////////////////////////////////
    // WFM as NodeContainer: Dialog related implementations
    /////////////////////////////////////////////////////////
    
    /** {@inheritDoc} */
    @Override
    public boolean hasDialog() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    GenericNodeDialogPane getDialogPaneWithSettings(
            final PortObjectSpec[] inSpecs) throws NotConfigurableException {
        throw new IllegalStateException("Workflow has no dialog (yet)");
    }

    /** {@inheritDoc} */
    @Override
    GenericNodeDialogPane getDialogPane() {
        throw new IllegalStateException("Workflow has no dialog (yet)");
    }

    /** {@inheritDoc} */
    @Override
    public boolean areDialogAndNodeSettingsEqual() {
        assert false : "No dialog available for workflow";
        return true; // be positive
    }

    /** {@inheritDoc} */
    @Override
    void loadSettingsFromDialog() throws InvalidSettingsException {
        assert false : "No dialog available for workflow";
    }

    /////////////////////////////////
    // Private helper functions
    /////////////////////////////////

    /**
     * @return global table respository for this WFM.
     */
    HashMap<Integer, ContainerTable> getGlobalTableRepository() {
        return m_globalTableRepository;
    }

    /** Create list of executed node (id)s between two nodes. Used to re-execute
     * all nodes within a loop.
     */
    private Set<NodeID> findExecutedNodesInLoopBody(final NodeID startNode,
            final NodeID endNode) {
        Set<NodeID> matchingNodes = new HashSet<NodeID>();
        doesChainOfExecutedNodesReachNode(startNode, endNode, matchingNodes);
        matchingNodes.remove(startNode);
        matchingNodes.remove(endNode);
        return matchingNodes;
    }

    /**
     * Check if there is one chain of nodes inbetween two nodes that are
     * all executed. Produce set of nodes inbetween the two nodes as well.
     * 
     * @param hereIam start node
     * @param endNode end node
     * @param resultSet resulting, executed nodes.
     * @return true if they are all executed.
     */
    private boolean doesChainOfExecutedNodesReachNode(final NodeID hereIam,
            final NodeID endNode, final Set<NodeID> resultSet) {
        if (hereIam == endNode) {
            return true;
        }
        if (resultSet.contains(hereIam)) {
            return true;
        }
        boolean atLeastOneChainDoes = false;
        Set<ConnectionContainer> succs = m_connectionsBySource.get(hereIam);
        if (succs != null) {
            for (ConnectionContainer conn : succs) {
                NodeID currID = conn.getDest();
                if (doesChainOfExecutedNodesReachNode(currID, endNode,
                                        resultSet)) {
                    assert     (currID == endNode)
                            || (m_nodes.get(currID).getState()
                                        == NodeContainer.State.EXECUTED);
                    atLeastOneChainDoes = true;
                }
            }
        }
        if (atLeastOneChainDoes) {
            resultSet.add(hereIam);
        }
        return atLeastOneChainDoes;
    }

    /** Return list of nodes, sorted by traversing the graph breadth first.
     * 
     * @return list of nodes
     */
    Iterable<NodeID> getBreathFirstListOfNodes() {
        // first create list of nodes without predecessor or only the WFM
        // itself (i.e. connected to outside "world" only.
        ArrayList<NodeID> bfsSortedNodes = new ArrayList<NodeID>();
        for (NodeID thisNode : m_nodes.keySet()) {
            Set<ConnectionContainer> incomingConns
                       = m_connectionsByDest.get(thisNode);
            boolean onlyWFMorNothing = true;
            for (ConnectionContainer thisConn : incomingConns) {
                if (!thisConn.getSource().equals(this.getID())) {
                    onlyWFMorNothing = false;
                }
            }
            if (onlyWFMorNothing) {
                bfsSortedNodes.add(thisNode);
            }
        }
        // now keep adding nodes until we can't find new ones anymore
        for (int i = 0; i < bfsSortedNodes.size(); i++) {
            NodeID currNode = bfsSortedNodes.get(i);
            // look at all successors of this node
            for (ConnectionContainer cc : m_connectionsBySource.get(currNode)) {
                NodeID succNode = cc.getDest();
                // this node is not subject to bf traversal
                if (succNode.equals(getID())) {
                    continue;
                }
                // and make sure all predecessors of this successor are already
                // in the list
                boolean allContained = true;
                for (ConnectionContainer cc2
                               : m_connectionsByDest.get(succNode)) {
                    if (!(bfsSortedNodes.contains(cc2.getSource()))) {
                        allContained = false;
                    }
                }
                if (allContained) {
                    bfsSortedNodes.add(succNode);
                }
            }
        }
        return bfsSortedNodes;
    }

    /** semaphore to avoid multiple checks for newly executable nodes
     * to interfere / interleave with each other.
     */
    private final Object m_currentlychecking = new Object();

    private void checkForQueuableNodes() {
        boolean remainingNodes;
        do {
            remainingNodes = false;
            for (NodeContainer ncIt : m_nodes.values()) {
                if (ncIt.getState()
                        == NodeContainer.State.MARKEDFOREXEC) {
                    final PortObject[] inData =
                        new PortObject[ncIt.getNrInPorts()];
                    synchronized (m_dirtyWorkflow) {
                        assembleInputData(ncIt.getID(), inData);
                    }
                    boolean dataAvailable = true;
                    for (int i = 0; i < inData.length; i++) {
                        if (inData[i] == null) {
                            dataAvailable = false;
                        }
                    }
                    // Check if all data is available. Important, QUEUED
                    // does not mean the node is already executing!
                    if (dataAvailable) {
                        ncIt.queueNode(inData);
                        remainingNodes = true;
                    }
                }
            }
        } while (remainingNodes);
    }

    /**
     * Cancel execution of the given NodeContainer.
     * 
     * @param nc node to be canceled
     */
    public void cancelExecution(final NodeContainer nc) {
        assert nc != null;
        nc.cancelExecution();
    }

    /**
     * Check if any internal nodes have changed state which might mean that
     * this WFM also needs to change its state...
     */
    private void checkForNodeStateChanges() {
        synchronized (m_currentlychecking) {
            int[] nrNodesInState = new int[State.values().length];
            int nrNodes = 0;
            for (NodeContainer ncIt : m_nodes.values()) {
                nrNodesInState[ncIt.getState().ordinal()]++;
                nrNodes++;
            }
            assert nrNodes == m_nodes.size();
            NodeContainer.State newState = State.IDLE;
            if (nrNodesInState[State.EXECUTED.ordinal()] == nrNodes) {
                // WFM is executed only if all nodes are executed and
                // all ports (input and output) are populated.
                boolean allPortObjectsExist = true;
                for (int i = 0; i < getNrOutPorts(); i++) {
                    if (getOutPort(i).getPortObject() == null) {
                        allPortObjectsExist = false;
                    }
                }
                for (int i = 0; i < getNrInPorts(); i++) {
                    if (getWorkflowIncomingPort(i).getPortObject() == null) {
                        allPortObjectsExist = false;
                    }
                }
                if (allPortObjectsExist) {
                    doAfterExecution(this);
                    return;
                }
            } else if (nrNodesInState[State.CONFIGURED.ordinal()] == nrNodes) {
                newState = State.CONFIGURED;
            } else if (nrNodesInState[State.EXECUTING.ordinal()] >= 1) {
                newState = State.EXECUTING;
            }
            this.setNewState(newState);
        }
    }

    
    /** Assemble array of all data for the data input of a given node. The
     * underlying NodeContainer will make sure the nodes are actually
     * executed before it provides data != null.
     *
     * @param id of node
     * @param array of data tables
     */
    private void assembleInputData(final NodeID id,
            final PortObject[] inData) {
        synchronized (m_dirtyWorkflow) {
            NodeOutPort[] ports = assemblePredecessorOutPorts(id);
            assert inData.length == ports.length;
            for (int i = 0; i < inData.length; i++) {
                inData[i] = null;
                if (ports[i] != null) {
                    inData[i] = ports[i].getPortObject();
                } else {
                    inData[i] = null;
                }
            }
        }
    }

    /** Assemble array of all NodeOutPorts connected to the input
     * ports of a given node. This routine will make sure to skip intermediate
     * metanode "bridges".
     *
     * @param id of node
     * @return array of NodeOutPorts connected to this node
     */
    private NodeOutPort[] assemblePredecessorOutPorts(final NodeID id) {
        NodeContainer nc = m_nodes.get(id);
        int nrIns = nc.getNrInPorts();
        NodeOutPort[] result = new NodeOutPort[nrIns];
        for (int i = 0; i < nrIns; i++) {
            result[i] = null;
            Set<ConnectionContainer> incoming = m_connectionsByDest.get(id);
            for (ConnectionContainer conn : incoming) {
                assert conn.getDest().equals(id);
                // get info about destination
                int destPortIndex = conn.getDestPort();
                if (destPortIndex == i) {  // found connection to correct port
                    int portIndex = conn.getSourcePort();
                    if (conn.getSource() != this.getID()) {
                        // connected to another node inside this WFM
                        assert conn.getType() == ConnectionType.STD;
                        result[i] =
                            m_nodes.get(conn.getSource()).getOutPort(portIndex);
                    } else {
                        // connected to a WorkflowInport
                        assert conn.getType() == ConnectionType.WFMIN;
                        result[i] = getWorkflowIncomingPort(portIndex);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Check if a node has fully connected incoming ports.
     * 
     * @param id of Node
     * @return true if all input ports are connected.
     */
    private boolean isFullyConnected(final NodeID id) {
        if (id.equals(this.getID())) {
            return getParent().isFullyConnected(id);
        } else {
            // get node
            NodeContainer nc = m_nodes.get(id);
            // get incoming connections
            Set<ConnectionContainer> incoming = m_connectionsByDest.get(id);
            if (incoming.size() < nc.getNrInPorts()) {
                // Note that this enforces FULLY connected nodes
                return false;
            }
        }
        return true;
    }
    
    /**
     * Configure node and, if this node's output specs have changed
     * also configure it's successors.
     *
     * @param id of node to configure
     */
    private void configure(final NodeID id) {
        if (!isFullyConnected(id)) {
            return;
        }
        // get node
        NodeContainer nc = m_nodes.get(id);
        if (nc == null) {
            synchronized (m_dirtyWorkflow) {
                // looks like we are trying to configure ourselves - nothing
                // to do - wrapping ports already do the trick.
                // this WFM itself is configured from the outside!
            }
            return;
        }
        synchronized (m_dirtyWorkflow) {
            PortObjectSpec[] inSpecs =
                new PortObjectSpec[nc.getNrInPorts()];
            assembleInputSpecs(id, inSpecs);
            // create new ScopeContextStack if this is a "real" node
            if (nc instanceof SingleNodeContainer) {
                SingleNodeContainer snc = (SingleNodeContainer)nc;
                ScopeObjectStack[] scscs =
                    new ScopeObjectStack[snc.getNrInPorts()];
                assembleSCStackContainer(id, scscs);
                ScopeObjectStack  scsc =
                    new ScopeObjectStack(id, scscs);
                snc.setScopeObjectStack(scsc);
            }
            // configure node only if it's not yet running or queued or done.
            // This can happen if the WFM queues a node which has more than
            // one predecessor with populated output ports but one of the
            // nodes still has not called the "doAfterExecution()" routine
            // which might attempt to configure the already queued node again.
            NodeContainer.State ncState = nc.getState();
            switch (ncState) {
            case EXECUTING:
            case EXECUTED:
            case QUEUED:
                break;
            default:
                boolean outputSpecsChanged = nc.configureNode(inSpecs);
                if (outputSpecsChanged) {  // and configure successors if needed
                    configureSuccessors(id);
                }
            }
        }
    }

    /** Configure successors of this node and propagate this further until
     * specs stop to change or end of pipeline is reached.
     *
     * @param id of "parent" node.
     */
    private void configureSuccessors(final NodeID id) {
        Set<ConnectionContainer> outgoing = m_connectionsBySource.get(id);
        for (ConnectionContainer conn : outgoing) {
            NodeID currID = conn.getDest();
            configure(currID);
            if (conn.getType()
                    .equals(ConnectionContainer.ConnectionType.WFMOUT)) {
                getParent().configureSuccessors(this.getID());
            }
        }
    }

    /**
     * Fill array holding all input specs for the given node.
     *
     * @param id of node
     * @param inSpecs return array for specs of all predecessors
     */
    void assembleInputSpecs(final NodeID id,
            final PortObjectSpec[] inSpecs) {
        NodeOutPort[] ports = assemblePredecessorOutPorts(id);
        assert inSpecs.length == ports.length;
        for (int i = 0; i < inSpecs.length; i++) {
            if (ports[i] != null) {
                inSpecs[i] = ports[i].getPortObjectSpec();
            } else {
                inSpecs[i] = null;
            }
        }
    }

    /**
     * Fill array holding all ScopeStackContainers for the given node.
     *
     * @param id of node
     * @param scscs return array for scopestacks of all predecessors
     */
    private void assembleSCStackContainer(final NodeID id,
            final ScopeObjectStack[] scscs) {
        NodeOutPort[] ports = assemblePredecessorOutPorts(id);
        assert scscs.length == ports.length;
        for (int i = 0; i < scscs.length; i++) {
            if (ports[i] != null) {
                scscs[i] = ports[i].getScopeContextStackContainer();
            }
        }
    }

    /** Produce summary of node.
     *
     * @param prefix if containing node/workflow
     * @param indent number of leading spaces
     * @return string
     */
    public String printNodeSummary(final NodeID prefix, final int indent) {
        char[] indentChars = new char[indent];
        Arrays.fill(indentChars, ' ');
        String indentString = new String(indentChars);
        StringBuilder build = new StringBuilder(indentString);
        build.append(getNameWithID());
        build.append(": " + getState() + " (start)\n");
        for (Map.Entry<NodeID, NodeContainer> it
                : m_nodes.tailMap(prefix).entrySet()) {
            NodeID id = it.getKey();
            if (id.hasPrefix(prefix)) {
                NodeContainer nc = it.getValue();
                if (nc instanceof WorkflowManager) {
                    build.append(((WorkflowManager)nc).printNodeSummary(
                            nc.getID(), indent + 2));
                } else {
                    build.append(indentString);
                    build.append("  ");
                    build.append(nc.getNameWithID());
                    build.append(": ");
                    build.append(nc.getState());
                    build.append("\n");
                }
            } else {    // skip remaining nodes with wrong prefix
                break;
            }
        }
        build.append(indentString);
        build.append(getNameWithID());
        build.append("(end)\n");
        return build.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return printNodeSummary(getID(), 0);
    }

    ////////////////////////
    // WFM functionality
    ////////////////////////
    

    /**
     * @return collection of NodeContainers in this WFM
     */
    public Collection<NodeContainer> getNodeContainers() {
        return Collections.unmodifiableCollection(m_nodes.values());
    }

    /**
     * @return collection of ConnectionContainer in this WFM
     */
    public Collection<ConnectionContainer> getConnectionContainers() {
        Set<ConnectionContainer> result =
            new LinkedHashSet<ConnectionContainer>();
        for (Set<ConnectionContainer> s : m_connectionsBySource.values()) {
            if (s != null) {
                result.addAll(s);
            }
        }
        return result;
    }

    /**
     * @param id node ID
     * @return NodeContainer for given ID
     */
    public NodeContainer getNodeContainer(final NodeID id) {
        NodeContainer nc = m_nodes.get(id);
        if (nc == null) {
            throw new IllegalArgumentException("No such node ID: " + id);
        }
        return m_nodes.get(id);
    }
    
    /** Create list of nodes in this WFM in a breath-first order.
     * 
     * @return all nodes in this workflow, breath first sorted.
     */
    public Iterable<NodeContainer> getNodeContainerBreadthFirstSearch() {
        // list of all nodes used to assemble final result
        List<NodeContainer> resultList = new ArrayList<NodeContainer>();
        // hash set of growing list to allow fast duplicate check
        HashSet<NodeID> duplicateCheck = new HashSet<NodeID>();
        // first add all source nodes, i.e. nodes which have no (connected)
        // input, or are only connected to this WFM's input ports.
        for (NodeContainer nc : m_nodes.values()) {
            Set<ConnectionContainer> inConns 
                = m_connectionsByDest.get(nc.getID());
            boolean hasPredecessor = false;
            for (ConnectionContainer cc : inConns) {
                NodeID sourceID = cc.getSource();
                if (!sourceID.equals(getID())) {
                    hasPredecessor = true;
                }
            }
            if (!hasPredecessor) {
                resultList.add(nc);
                duplicateCheck.add(nc.getID());
            }
        }
        // add the successors of the elements in resultList
        // do not use list's iterator as the list is modified
        for (int i = 0; i < resultList.size(); i++) {
            NodeContainer nc = resultList.get(i);
            Set<ConnectionContainer> outConns = 
                m_connectionsBySource.get(nc.getID());
            for (ConnectionContainer cc : outConns) {
                NodeID candidate = cc.getDest();
                if (!duplicateCheck.contains(candidate) &&
                        (!candidate.equals(this.getID()))) {
                    boolean hasUnvisitedPredecessor = false;
                    for (ConnectionContainer ccBack :
                              m_connectionsByDest.get(candidate)) {
                        NodeID sourceID = ccBack.getSource();
                        if (!duplicateCheck.contains(sourceID) 
                                && !sourceID.equals(getID())) {
                            hasUnvisitedPredecessor = true;
                        }
                    }
                    if (!hasUnvisitedPredecessor) {
                        // all predecessors are already in our list - otherwise
                        // we will add this one later anyway.
                        resultList.add(m_nodes.get(candidate));
                        duplicateCheck.add(candidate);
                    }
                }
            }
        }
        assert resultList.size() == m_nodes.size() : "Did not vist all nodes";
        return resultList;
    }

    ///////////////////////////////////
    // Listener for Workflow Events
    ///////////////////////////////////

    /**
     * Add listener to list.
     * 
     * @param listener new listener
     */
    public void addListener(final WorkflowListener listener) {
        if (!m_wfmListeners.contains(listener)) {
            m_wfmListeners.add(listener);
        }
    }

    /**
     * Remove listener.
     * @param listener listener to be removed
     */
    public void removeListener(final WorkflowListener listener) {
        m_wfmListeners.remove(listener);
    }

    /**
     * Fire event to all listeners.
     * @param evt event
     */
    private final void notifyWorkflowListeners(final WorkflowEvent evt) {
        for (WorkflowListener listener : m_wfmListeners) {
            listener.workflowChanged(evt);
        }
    }

    ///////////////////////////////
    ///////// LOAD & SAVE /////////
    ///////////////////////////////

    /** Workflow version. */
    static final String CFG_VERSION = "version";


    public static WorkflowManager load(File directory,
            final ExecutionMonitor exec) throws IOException,
            InvalidSettingsException, CanceledExecutionException {
        if (directory == null || exec == null) {
            throw new NullPointerException("Arguments must not be null.");
        }
        // TODO GUI only needs to provide directory path
        directory = directory.getParentFile();
        if (!directory.isDirectory() || !directory.canRead()) {
            throw new IOException("Can't read directory " + directory);
        }
        File workflowknime = 
            new File(directory, WorkflowPersistor.WORKFLOW_FILE);
        if (!workflowknime.isFile()) {
            throw new IOException("No \"" + WorkflowPersistor.WORKFLOW_FILE 
                    + "\" file in directory \"" + directory.getAbsolutePath()
                    + "\"");
        }
        NodeSettingsRO settings = NodeSettings.loadFromXML(
                new BufferedInputStream(new FileInputStream(workflowknime)));
        // CeBIT 2006 version did not contain a version string.
        String version;
        if (settings.containsKey(CFG_VERSION)) {
            version = settings.getString(CFG_VERSION);
        } else {
            version = "0.9.0";
        }
        if (version == null) {
            throw new InvalidSettingsException(
                "Refuse to load workflow: Workflow version not available.");
        }
        WorkflowPersistor persistor;
        if (WorkflowPersistorVersion200.canReadVersion(version)) {
            persistor = new WorkflowPersistorVersion200(
                    new HashMap<Integer, ContainerTable>());
        } else if (WorkflowPersistorVersion1xx.canReadVersion(version)) {
            LOGGER.warn(
                    "The current KNIME version (" + KNIMEConstants.VERSION 
                    + ") is different from the one that created the"
                    + " workflow (" + version 
                    + ") you are trying to load. In some rare cases, it"
                    + " might not be possible to load all data"
                    + " or some nodes can't be configured."
                    + " Please re-configure and/or re-execute these"
                    + " nodes.");
            persistor = new WorkflowPersistorVersion1xx(
                    new HashMap<Integer, ContainerTable>());
        } else {
            throw new InvalidSettingsException("Unable to load workflow, "
                    + "version string \"" + version + "\" is unknown");
        }
        LOGGER.debug("Loading workflow from \"" + directory.getAbsolutePath()
                + "\" (version \"" + version + "\" with loader class \""
                + persistor.getClass().getSimpleName() + "\")");
        // data files are loaded using a repository of reference tables;
        // these lines serves to init the repository so nodes can put their data
        // into this map, the repository is deleted when the loading is done
        int loadID = System.identityHashCode(persistor);
        BufferedDataTable.initRepository(loadID);
        LoadResult loadResult = 
            persistor.preLoadNodeContainer(workflowknime, exec, settings);
        loadResult.addError(persistor.loadNodeContainer(loadID, exec));
        if (loadResult.hasErrors()) {
            LOGGER.warn(loadResult.getErrors());
        }
        WorkflowManager result;
        synchronized (ROOT.m_dirtyWorkflow) {
            NodeID newID = ROOT.createUniqueID();
            result = ROOT.createSubWorkflow(persistor, newID);
            result.loadContent(persistor, loadID);
            ROOT.addNodeContainer(result);
        }
        BufferedDataTable.clearRepository(loadID);
        LOGGER.debug("Successfully loaded content from \"" 
                + directory.getAbsolutePath() + "\"  into workflow manager "
                + "instance " + result.getNameWithID());
        return result;
    }
    
    /** {@inheritDoc} */
    @Override
    void loadContent(final NodeContainerPersistor nodePersistor, 
            final int loadID) {
        if (!(nodePersistor instanceof WorkflowPersistor)) {
            throw new IllegalStateException("Expected " 
                    + WorkflowPersistor.class.getSimpleName() 
                    + " persistor object, got " 
                    + nodePersistor.getClass().getSimpleName());
        }
        WorkflowPersistor persistor = (WorkflowPersistor)nodePersistor;
        // id suffix are made unique by using the entries in this map
        Map<Integer, NodeID> translationMap = new HashMap<Integer, NodeID>();
        Map<NodeID, NodeContainerPersistor> persistorMap = 
            new HashMap<NodeID, NodeContainerPersistor>();

        for (Map.Entry<Integer, NodeContainerPersistor> nodeEntry
                : persistor.getNodeLoaderMap().entrySet()) {
            int suffix = nodeEntry.getKey();
            NodeID subId = new NodeID(getID(), suffix);
            if (m_nodes.containsKey(subId)) {
                subId = createUniqueID();
            }
            NodeContainerPersistor pers = nodeEntry.getValue();
            translationMap.put(suffix, subId);
            persistorMap.put(subId, pers);
            NodeContainer container = pers.getNodeContainer(this, subId);
            addNodeContainer(container);
        }

        for (ConnectionContainerTemplate c : persistor.getConnectionSet()) {
            int sourceSuffix = c.getSourceSuffix();
            int destSuffix = c.getDestSuffix();
            assert sourceSuffix == -1 || sourceSuffix != destSuffix
                : "Can't insert connection, source and destination are equal";
            ConnectionType type = ConnectionType.STD;
            NodeID source;
            NodeID dest;
            if ((sourceSuffix == -1) && (destSuffix == -1)) {
                source = getID();
                dest = getID();
                type = ConnectionType.WFMTHROUGH;
            } else if (sourceSuffix == -1) {
                source = getID(); 
                dest = translationMap.get(destSuffix);
                type = ConnectionType.WFMIN;
            } else if (destSuffix == -1) {
                dest = getID();
                source = translationMap.get(sourceSuffix);
                type = ConnectionType.WFMOUT;
            } else {
                dest = translationMap.get(destSuffix);
                source = translationMap.get(sourceSuffix);
            }
            if (source == null || dest == null) {
                LOGGER.warn("Unable to insert connection \"" + c
                        + "\", one of the nodes does not exist in the flow");
                continue;
            }
            ConnectionContainer cc = addConnection(
                    source, c.getSourcePort(), dest, c.getDestPort(), false);
            assert cc.getType() == type;
        }
        Set<NodeID> failedNodes = new HashSet<NodeID>();
        Set<NodeID> needConfigurationNodes = new HashSet<NodeID>();
        LoadResult loadResult = new LoadResult();
        for (NodeID bfsID : getBreathFirstListOfNodes()) {
            // failed nodes may not even be in m_nodes - we reset everything
            // which is not fully connected
            boolean needsReset = !isFullyConnected(bfsID);
            NodeContainerPersistor containerPersistor = persistorMap.get(bfsID);
            NodeContainer cont = m_nodes.get(bfsID);
            try {
                LoadResult temp = containerPersistor.loadNodeContainer(
                        loadID, new ExecutionMonitor());
                if (temp.hasErrors()) {
                    loadResult.addError("Errors reading node \"" 
                            + cont.getNameWithID() + "\":", temp);
                }
            } catch (CanceledExecutionException e) {
            } catch (Exception e) {
                if (!(e instanceof InvalidSettingsException)
                        && !(e instanceof IOException)) {
                    LOGGER.error("Caught unexpected \"" 
                            + e.getClass().getSimpleName()
                            + "\" during node loading", e);
                }
                needsReset = true;
            }
            cont.loadContent(containerPersistor, loadID);

            boolean hasPredecessorFailed = false;
            for (ConnectionContainer cc : m_connectionsByDest.get(bfsID)) {
                if (failedNodes.contains(cc.getSource())) {
                    hasPredecessorFailed = true;
                }
            }
            needsReset |= containerPersistor.needsResetAfterLoad();
            needsReset |= hasPredecessorFailed;
            if (needsReset) {
                failedNodes.add(bfsID);
            }
            if (!hasPredecessorFailed && needsReset) {
                needConfigurationNodes.add(bfsID);
            }
        }
        if (loadResult.hasErrors()) {
            LOGGER.warn(loadResult);
        }
        for (NodeID id : needConfigurationNodes) {
            resetAndConfigureNode(id);
        }
        for (NodeID id : needConfigurationNodes) {
            configure(id);
        }
    }

    public void save(final File directory, final ExecutionMonitor exec,
            final boolean isSaveData)
        throws IOException, CanceledExecutionException {
        // TODO GUI must only provide directory
        File workflowFile = directory.getParentFile();
        new WorkflowPersistorVersion200(null).save(
                this, workflowFile, exec, isSaveData);
    }

    //////////////////////////////////////
    // NodeContainer implementations
    // (WorkflowManager acts as meta node)
    //////////////////////////////////////

    /** {@inheritDoc} */
    @Override
    public int getNrInPorts() {
        return m_inPorts.length;
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowInPort getInPort(final int index) {
        return m_inPorts[index];
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowOutPort getOutPort(final int index) {
        return m_outPorts[index];
    }

    /** {@inheritDoc} */
    @Override
    public int getNrOutPorts() {
        return m_outPorts.length;
    }
    
    /** Set new name of this workflow.
     * @param name The new name.
     * @throws NullPointerException If argument is null.
     */
    public void setName(final String name) {
        if (name == null) {
            throw new NullPointerException("Name must not be null.");
        }
        m_name = name;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return m_name;
    }

    /** {@inheritDoc} */
    @Override
    public int getNrViews() {
        return 0;  // workflow managers don't have views (yet?)!
    }

    /** {@inheritDoc} */
    @Override
    public GenericNodeView<GenericNodeModel> getView(final int i) {
        throw new IndexOutOfBoundsException("WFM don't have views.");
    }

    /** {@inheritDoc} */
    @Override
    public String getViewName(final int i) {
        throw new IndexOutOfBoundsException("WFM don't have views.");
    }

    /** {@inheritDoc} */
    @Override
    void loadSettings(final NodeSettingsRO settings)
        throws InvalidSettingsException {
    }

    /** {@inheritDoc} */
    @Override
    void saveSettings(final NodeSettingsWO settings)
    throws InvalidSettingsException {
    }

    /** {@inheritDoc} */
    @Override
    boolean areSettingsValid(final NodeSettingsRO settings) {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public NodeType getType() {
        return NodeType.Meta;
    }

    /** {@inheritDoc} */
    @Override
    public URL getIcon() {
        return null;
    }

    ///////////////////////////
    // Workflow port handling
    /////////////////////////
    
    public int getNrWorkflowIncomingPorts() {
        return getNrInPorts();
    }
    
    public int getNrWorkflowOutgoingPorts() {
        return getNrOutPorts();
    }
    
    public NodeOutPort getWorkflowIncomingPort(final int i) {
        return m_inPorts[i].getUnderlyingPort();
    }
    
    public NodeInPort getWorkflowOutgoingPort(final int i) {
        return m_outPorts[i].getSimulatedInPort();
    }
}
