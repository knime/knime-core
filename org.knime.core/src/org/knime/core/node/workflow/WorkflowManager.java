/*
 * ------------------------------------------------------------------ *
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.Node;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NodeView;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.Node.LoopRole;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.node.workflow.ConnectionContainer.ConnectionType;
import org.knime.core.node.workflow.ScopeLoopContext.RestoredScopeLoopContext;
import org.knime.core.node.workflow.WorkflowPersistor.ConnectionContainerTemplate;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowPortTemplate;
import org.knime.core.util.FileUtil;

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
    
    /** Executor for asynchronous event notification. */
    private static final Executor WORKFLOW_NOTIFIER =
        Executors.newSingleThreadExecutor(new ThreadFactory() {
            /** {@inheritDoc} */
            @Override
            public Thread newThread(final Runnable r) {
                Thread t = new Thread(r, "KNIME-Workflow-Notifier");
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            }
        });
    
    // Nodes and edges forming this workflow:
    final Workflow m_workflow;

    // Ports of the workflow (empty if it is not a subworkflow):

    /** ports of this Metanode (both arrays can have 0 length!). */
    private final WorkflowInPort[] m_inPorts;
    private UIInformation m_inPortsBarUIInfo;
    private final WorkflowOutPort[] m_outPorts;
    private UIInformation m_outPortsBarUIInfo;

    // Misc members:

    /** for internal usage, holding output table references. */
    private final HashMap<Integer, ContainerTable> m_globalTableRepository;

    private final List<ReferencedFile> m_deletedNodesFileLocations
        = new ArrayList<ReferencedFile>();

    /** The version string as read from workflow.knime file during load
     * (or null if not loaded but newly created). This field is used to
     * determine whether the workflow needs to be converted to any newer version
     * upon save. */
    private String m_loadVersion;

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
    private Object m_workflowMutex;

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
        m_workflow = new Workflow(id);
        m_inPorts = new WorkflowInPort[inTypes.length];
        for (int i = 0; i < inTypes.length; i++) {
            m_inPorts[i] = new WorkflowInPort(i, inTypes[i]);
        }
        m_outPorts = new WorkflowOutPort[outTypes.length];
        for (int i = 0; i < outTypes.length; i++) {
            m_outPorts[i] = new WorkflowOutPort(this, i, outTypes[i]);
        }
        if (m_inPorts.length == 0 && m_outPorts.length == 0) {
            // this workflow is not connected to parent via any ports
            // (it is likely a project)
            // we can start a new table repository since there can not
            // be any dependencies...
            m_globalTableRepository = new HashMap<Integer, ContainerTable>();
            // ...and we do not need to synchronize across unconnected workflows
            m_workflowMutex = new Object();
        } else {
            // otherwise we may have incoming and/or outgoing dependencies...
            m_globalTableRepository = parent.m_globalTableRepository;
            // ...synchronize across border
            m_workflowMutex = parent.m_workflowMutex;
        }
        // initialize listener list
        m_wfmListeners = new CopyOnWriteArrayList<WorkflowListener>();
        // done.
        LOGGER.debug("Created subworkflow " + this.getID());
    }

    /** Constructor - create new workflow from persistor.
     */
    private WorkflowManager(final WorkflowManager parent, final NodeID id,
            final WorkflowPersistor persistor) {
        super(parent, id, persistor.getMetaPersistor());
        m_workflow = new Workflow(id);
        m_name = persistor.getName();
        WorkflowPortTemplate[] inPortTemplates = persistor.getInPortTemplates();
        m_inPorts = new WorkflowInPort[inPortTemplates.length];
        for (int i = 0; i < inPortTemplates.length; i++) {
            WorkflowPortTemplate t = inPortTemplates[i];
            m_inPorts[i] = new WorkflowInPort(
                    t.getPortIndex(), t.getPortType());
            m_inPorts[i].setPortName(t.getPortName());
        }
        WorkflowPortTemplate[] outPortTemplates =
            persistor.getOutPortTemplates();
        m_outPorts = new WorkflowOutPort[outPortTemplates.length];
        for (int i = 0; i < outPortTemplates.length; i++) {
            WorkflowPortTemplate t = outPortTemplates[i];
            m_outPorts[i] = new WorkflowOutPort(
                    this, t.getPortIndex(), t.getPortType());
            m_outPorts[i].setPortName(t.getPortName());
        }
        m_workflowMutex = m_inPorts.length == 0
            && m_outPorts.length == 0 ? new Object() : parent.m_workflowMutex;
        m_globalTableRepository = persistor.getGlobalTableRepository();
        m_wfmListeners = new CopyOnWriteArrayList<WorkflowListener>();
        LOGGER.debug("Created subworkflow " + this.getID());
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
        LOGGER.debug("Created project " + ((NodeContainer)wfm).getID());
        return wfm;
    }

    /** Remove a project - the same as remove node but we make sure it really
     * looks like a project (i.e. has no in- or outports).
     *
     * @param id of the project to be removed.
     */
    public void removeProject(final NodeID id) {
        NodeContainer nc = m_workflow.getNode(id);
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
    public NodeID createAndAddNode(final NodeFactory<?> factory) {
        NodeID newID;
        synchronized (m_workflowMutex) {
            // TODO synchronize to avoid messing with running workflows!
            assert factory != null;
            // insert node
            newID = createUniqueID();
            SingleNodeContainer container = new SingleNodeContainer(this,
               new Node((NodeFactory<NodeModel>)factory), newID);
            addNodeContainer(container, true);
        }
        configureNodeAndSuccessors(newID, true);
        LOGGER.debug("Added new node " + newID);
        setDirty();
        return newID;
    }

    /** Check if specific node can be removed (i.e. is not currently being
     * executed or waiting to be).
     *
     * @param nodeID id of node to be removed
     * @return true if node can safely be removed.
     */
    public boolean canRemoveNode(final NodeID nodeID) {
        synchronized (m_workflowMutex) {
            // check to make sure we can safely remove this node
            NodeContainer nc = m_workflow.getNode(nodeID);
            if (nc == null) {
                return false;
            }
            if (nc.getState().executionInProgress()) {
                return false;
            }
            if (!nc.isDeletable()) {
                return false;
            }
            for (ConnectionContainer c : m_workflow.getConnectionsByDest(nodeID)) {
                if (!c.isDeletable()) {
                    return false;
                }
            }
            for (ConnectionContainer c : m_workflow.getConnectionsBySource(nodeID)) {
                if (!c.isDeletable()) {
                    return false;
                }
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
        NodeContainer nc;
        synchronized (m_workflowMutex) {
            // if node does not exist, simply return
            if (m_workflow.getNode(nodeID) == null) {
                return;
            }
            // check to make sure we can safely remove this node
            if (!canRemoveNode(nodeID)) {
                throw new IllegalStateException("Node can not be removed");
            }
            // remove lists of in- and outgoing connections.
            while (!m_workflow.getConnectionsByDest(nodeID).isEmpty()) {
                ConnectionContainer toDel =
                    m_workflow.getConnectionsByDest(nodeID).iterator().next();
                removeConnection(toDel);
            }
            while (!m_workflow.getConnectionsBySource(nodeID).isEmpty()) {
                ConnectionContainer toDel =
                    m_workflow.getConnectionsBySource(nodeID).iterator().next();
                removeConnection(toDel);
            }
            // and finally remove node itself as well.
            nc = m_workflow.removeNode(nodeID);
            nc.cleanup();
            ReferencedFile ncDir = nc.getNodeContainerDirectory();
            // update list of obsolete node directories for non-root wfm
            if (this != ROOT && ncDir != null) {
                m_deletedNodesFileLocations.add(ncDir);
            }
        }
        setDirty();
        notifyWorkflowListeners(
                new WorkflowEvent(WorkflowEvent.Type.NODE_REMOVED,
                getID(), nc, null));
        checkForNodeStateChanges(true);
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
        synchronized (m_workflowMutex) {
            newID = createUniqueID();
            wfm = new WorkflowManager(this, newID, inPorts, outPorts);
            addNodeContainer(wfm, true);
            LOGGER.debug("Added new subworkflow " + newID);
        }
        setDirty();
        return wfm;
    }

    /** Creates new meta node from a persistor instance.
     * @param persistor to read from
     * @param newID new id to be used
     * @return newly created WorflowManager
     */
    WorkflowManager createSubWorkflow(final WorkflowPersistor persistor,
            final NodeID newID) {
        if (!newID.hasSamePrefix(getID()) || m_workflow.containsNodeKey(newID)) {
            throw new RuntimeException(
                    "Invalid or duplicate ID \"" + newID + "\"");
        }
        WorkflowManager wfm = new WorkflowManager(this, newID, persistor);
        return wfm;
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
        if (m_workflow.getNrNodes() > 0) {
            NodeID lastID = m_workflow.m_nodes.lastKey();
            nextIndex = lastID.getIndex() + 1;
        }
        NodeID newID = new NodeID(this.getID(), nextIndex);
        assert !m_workflow.containsNodeKey(newID);
        return newID;
    }

    /** Adds the NodeContainer to m_nodes and adds empty connection sets to
     * m_connectionsBySource and m_connectionsByDest.
     * @param nodeContainer new Container to add.
     * @param propagateChanges Whether to also check workflow state
     * (this is always true unless called from the load routines)
     */
    private void addNodeContainer(final NodeContainer nodeContainer, 
            final boolean propagateChanges) {
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
        synchronized (m_workflowMutex) {
            assert !m_workflow.containsNodeKey(id) : "\""
               + nodeContainer.getNameWithID() + "\" already contained in flow";
            m_workflow.putNode(id, nodeContainer);
        }
        notifyWorkflowListeners(
                new WorkflowEvent(WorkflowEvent.Type.NODE_ADDED,
                id, null, nodeContainer));
        checkForNodeStateChanges(propagateChanges);
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
        // must not set it in the private method (see below), as the private
        // one is also called from the load routine
        setDirty();
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
        synchronized (m_workflowMutex) {
            if (!canAddConnection(source, sourcePort, dest, destPort)) {
                canAddConnection(source, sourcePort, dest, destPort);
                throw new IllegalArgumentException("Can not add connection!");
            }
            // check for existence of a connection to the destNode/Port
            Set<ConnectionContainer> scc = m_workflow.getConnectionsByDest(dest);
            ConnectionContainer removeCCfirst = null;
            for (ConnectionContainer cc : scc) {
                if (cc.getDestPort() == destPort) {
                    removeCCfirst = cc;
                }
            }
            if (removeCCfirst != null) {
                removeConnection(removeCCfirst);
            }
            // cleaned up - now add new connection
            sourceNC = m_workflow.getNode(source);
            destNC = m_workflow.getNode(dest);
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
                m_workflow.getConnectionsBySource(source);
            if (!outConns.add(newConn)) {
                throw new IllegalArgumentException(
                "Connection already exists!");
            }
            // 2) insert connection into set of ingoing connections
            Set<ConnectionContainer> inConns = m_workflow.getConnectionsByDest(dest);
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
                NodeOutPort underlyingPort;
                if (sourceNC != null) {
                    underlyingPort = sourceNC.getOutPort(sourcePort);
                } else {
                    assert source.equals(getID());
                    underlyingPort = getWorkflowIncomingPort(sourcePort);
                }
                wfmIPort.setUnderlyingPort(underlyingPort);
            } else if (dest.equals(getID())) {
                // we are feeding data out of the subworkflow
                assert newConnType == ConnectionType.WFMOUT;
                getOutPort(destPort).setUnderlyingPort(
                        sourceNC.getOutPort(sourcePort));
            }
        }
        // if so desired...
        if (configure) {
            if (newConn.getType().isLeavingWorkflow()) {
                assert !m_workflow.containsNodeKey(dest);
                // if the destination was the WFM itself, only configure its
                // successors one layer up!
                getParent().configureNodeAndSuccessors(dest, false);
                checkForNodeStateChanges(true);
            } else {
                assert m_workflow.containsNodeKey(dest);
                // ...make sure the destination node is configured again (and
                // all of its successors if needed):
                configureNodeAndSuccessors(dest, true);
            }
        }
        // and finally notify listeners
        notifyWorkflowListeners(new WorkflowEvent(
                WorkflowEvent.Type.CONNECTION_ADDED, null, null, newConn));
        LOGGER.debug("Added new connection from node " + source
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
        if (source == null || dest == null) {
            return false;
        }
        // get NodeContainer for source/dest - can be null for WFM-connections!
        NodeContainer sourceNode = m_workflow.getNode(source);
        NodeContainer destNode = m_workflow.getNode(dest);
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
        // check if we are about to replace an existing connection
        for (ConnectionContainer cc : m_workflow.getConnectionsByDest(dest)) {
            if (cc.getDestPort() == destPort) {
                // if that connection is not removable: fail
                if (!canRemoveConnection(cc)) {
                    return false;
                }
            }
        }
        // check type compatibility
        PortType sourceType = (sourceNode != null
            ? sourceNode.getOutPort(sourcePort).getPortType()
            : this.getInPort(sourcePort).getPortType());
        PortType destType = (destNode != null
            ? destNode.getInPort(destPort).getPortType()
            : this.getOutPort(destPort).getPortType());
        /* ports can be connected in two cases: 
         * - the destination type is a super type or the same type 
         *   of the source port (usual case) or 
         * - if the source port is a super type of the destination port, 
         *   for instance a reader node that reads a general PMML objects,
         *   validity is checked using the runtime class of the actual 
         *   port object then */ 
        Class<? extends PortObject> sourceCl = sourceType.getPortObjectClass();
        Class<? extends PortObject> destCl = destType.getPortObjectClass();
        if (!destCl.isAssignableFrom(sourceCl) 
            && !sourceCl.isAssignableFrom(destCl)) {
            return false;
        }
        // and finally check if we are threatening to close a loop (if we
        // are not trying to leave this metanode, of course).
        if (!dest.equals(this.getID())
                && !source.equals(this.getID())) {
            Set<NodeID> sNodes
                = m_workflow.getBreadthFirstListOfNodeAndSuccessors(
                        dest, true).keySet();
            if (sNodes.contains(source)) {
                return false;
            }
        }
        // no reason to say no found - return true.
        return true;
    }

    /** Check if a connection can safely be removed.
     *
     * @param cc connection
     * @return true if connection cc is removable.
     */
    public boolean canRemoveConnection(final ConnectionContainer cc) {
        if (!cc.isDeletable()) {
            return false;
        }
        // make sure both nodes (well, their connection lists) exist
        if (m_workflow.getConnectionsByDest(cc.getDest()) == null) {
            return false;
        }
        if (m_workflow.getConnectionsBySource(cc.getSource()) == null) {
            return false;
        }
        // make sure connection between those two nodes exists
        if (!m_workflow.getConnectionsByDest(cc.getDest()).contains(cc)) {
            return false;
        }
        if (!m_workflow.getConnectionsBySource(cc.getSource()).contains(cc)) {
            return false;
        }
        // retrieve state of destination NodeContainer (could be WFM)
        NodeContainer.State destState = cc.getDest().equals(this.getID())
            ? this.getState()
            : m_workflow.getNode(cc.getDest()).getState();
        // make sure destination is "in use"...
        if (!(destState.equals(NodeContainer.State.IDLE)
              || destState.equals(NodeContainer.State.CONFIGURED)
              || destState.equals(NodeContainer.State.EXECUTED))) {
              return false;
          }
        return true;
    }

    /** Remove connection.
     *
     * @param cc connection
     */
    public void removeConnection(final ConnectionContainer cc) {
        synchronized (m_workflowMutex) {
            // make sure both nodes (well, their connection lists) exist
            if (m_workflow.getConnectionsByDest(cc.getDest()) == null) {
                return;
            }
            if (m_workflow.getConnectionsBySource(cc.getSource()) == null) {
                return;
            }
            // make sure connection exists
            if ((!m_workflow.getConnectionsByDest(cc.getDest()).contains(cc))) {
                if ((!m_workflow.getConnectionsBySource(cc.getSource()).contains(cc))) {
                    // if connection doesn't exist anywhere, we are fine
                    return;
                } else {
                    // this should never happen - only one direction exists
                    assert false;
                    throw new IllegalArgumentException(
                    "Can not remove partially existing connection!");
                }
            }
            // now check if other reasons forbit to delete this connection:
            if (!canRemoveConnection(cc)) {
                throw new IllegalStateException("Can not remove connection!");
            }
            // check type and underlying nodes
            NodeID source = cc.getSource();
            NodeID dest = cc.getDest();
            NodeContainer sourceNC = m_workflow.getNode(source);
            NodeContainer destNC = m_workflow.getNode(dest);
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
                m_workflow.getConnectionsBySource(source);
            if (!outConns.remove(cc)) {
                throw new IllegalArgumentException(
                "Connection does not exist!");
            }
            // 2) remove connection from set of ingoing connections
            Set<ConnectionContainer> inConns = m_workflow.getConnectionsByDest(dest);
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
            // and finally reset the destination node - since it has incomplete
            // incoming connections now...
            if (cc.getType().isLeavingWorkflow()) {
                // in case of WFM being disconnected make sure outside
                // successors are reset
                this.getParent().resetSuccessors(this.getID());
                // reconfigure successors as well (of still existing conns)
                this.getParent().configureNodeAndSuccessors(this.getID(),
                        false);
                // make sure to reflect state changes
                checkForNodeStateChanges(true);
            } else {
                // otherwise just reset successor, rest will be handled by WFM
                resetAndConfigureNode(cc.getDest());
            }
        }
        setDirty();
        notifyWorkflowListeners(
                new WorkflowEvent(WorkflowEvent.Type.CONNECTION_REMOVED,
                        null, cc, null));
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
        synchronized (m_workflowMutex) {
            Set<ConnectionContainer> outConnections
                = m_workflow.getConnectionsBySource(id);
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
        synchronized (m_workflowMutex) {
            Set<ConnectionContainer>inConns = m_workflow.getConnectionsByDest(id);
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
        synchronized (m_workflowMutex) {
            NodeContainer nc = getNodeContainer(id);
            if (!nc.getState().executionInProgress()
                    && !hasSuccessorInProgress(id)) {
                resetSuccessors(id);
                if (nc.getState().equals(State.EXECUTED)) {
                    if (nc instanceof SingleNodeContainer) {
                        ((SingleNodeContainer)nc).reset();
                    } else {
                        assert nc instanceof WorkflowManager;
                        assert false;
                    }
                }
                nc.loadSettings(settings);
                configureNodeAndSuccessors(id, true);
            } else {
                throw new IllegalStateException(
                        "Cannot load settings into running node.");
            }
        }
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
     * will be queued "automatically" (usually triggered by doAfterExecution
     * of the predecessors).
     * This does NOT affect any predecessors of this workflow!
     * 
     * @param flag mark nodes if true, otherwise try to erase marks
     */
    private void markForExecutionAllNodesInWorkflow(final boolean flag) {
        synchronized (m_workflowMutex) {
            for (NodeID id : m_workflow.getNodeIDs()) {
                NodeContainer nc = m_workflow.getNode(id);
                if (nc instanceof SingleNodeContainer) {
                    ((SingleNodeContainer)nc).markForExecution(flag);
                } else {
                    assert nc instanceof WorkflowManager;
                    ((WorkflowManager)nc).
                            markForExecutionAllNodesInWorkflow(flag);
                }
            }
        }
    }

    /** mark all nodes connected to the specified outport(!)
     * in this workflow (and all subworkflows!) for execution
     * (if they are not executed already). Also go back up to the
     * predecessors of this wfm if there are connections of interest.
     *
     * @param int outPortIndex indicates which outport is affected
     *   (-1 for all outports)
     * @return true if all nodes in chain were markable
     */
    private boolean markForExecutionAllAffectedNodes(final int outPortIndex) {
        synchronized (m_workflowMutex) {
            HashSet<Integer> p = new HashSet<Integer>();
            if (outPortIndex >= 0) {
                p.add(outPortIndex);
            } else {
                // if all ports are to be used, add them explicitly
                for (int o = 0; o < this.getNrOutPorts(); o++) {
                    p.add(o);
                }
            }
            LinkedHashMap<NodeID, Set<Integer>> sortedNodes =
                m_workflow.createBackwardsBreadthFirstSortedList(p);
            for (NodeID thisID : sortedNodes.keySet()) {
                if (!thisID.equals(getID())) { // skip WFM
                    NodeContainer thisNode = m_workflow.getNode(thisID);
                    if (thisNode instanceof SingleNodeContainer) {
                        SingleNodeContainer snc = (SingleNodeContainer)thisNode;
                        switch (snc.getState()) {
                        case IDLE:
                        case CONFIGURED:
                            if (!markAndQueueNodeAndPredecessors(
                                    snc.getID(), -1)) {
                                return false;
                            }
                            break;
                        case MARKEDFOREXEC:
                        case UNCONFIGURED_MARKEDFOREXEC:
                            // tolerate those states - nodes are already marked.
                            break;
                        default: // already running
                            // TODO other states. Any reason to bomb?
                        }
                    } else {
                        assert thisNode instanceof WorkflowManager;
                        Set<Integer> outPortIndicces = sortedNodes.get(thisID);
                        for (Integer i : outPortIndicces) {
                            if (!((WorkflowManager)thisNode).
                                    markForExecutionAllAffectedNodes(i)) {
                                return false;
                            }
                        }
                    }
                }
            } // endfor all nodes in sorted list
            checkForNodeStateChanges(true);
            if (sortedNodes.containsKey(this.getID())) {
                // list contained WFM, go up one level
                Set<Integer> is = sortedNodes.get(this.getID());
                for (Integer i : is) {
                    getParent().markAndQueuePredecessors(this.getID(), i);
                }
            }
        }
        return true;
    }

    /**
     * Reset the mark when nodes have been set to be executed. Skip
     * nodes which are already queued or executing.
     *
     * @param snc
     */
    private void disableNodeForExecution(final NodeID id) {
        NodeContainer nc = m_workflow.getNode(id);
        if (nc != null) {
            switch (nc.getState()) {
            case IDLE:
            case CONFIGURED:
                // nothing needs to be done - also with the successors!
                return;
            case MARKEDFOREXEC:
            case UNCONFIGURED_MARKEDFOREXEC:
                assert nc instanceof SingleNodeContainer;
                ((SingleNodeContainer)nc).markForExecution(false);
            default:
                // ignore all other states (but touch successors)
            }
            for (ConnectionContainer cc
                    : m_workflow.getConnectionsBySource(id)) {
                NodeID succId = cc.getDest();
                if (succId.equals(this.getID())) {
                    // unmark successors of this metanode
                    getParent().disableNodeForExecution(this.getID());
                } else {
                    // handle normal node
                    disableNodeForExecution(succId);
                }
            }
        } else { // WFM
            this.markForExecutionAllNodesInWorkflow(false);
            // unmark successors of this metanode
            getParent().disableNodeForExecution(this.getID());
        }
    }


    /**
     * Reset all nodes in this workflow. Make sure the reset is propagated
     * in the right order, that is, only actively reset the "left most"
     * nodes in the workflow or the ones connected to meta node input
     * ports. The will also trigger resets of subsequent nodes.
     */
    public void resetAll() {
        synchronized (m_workflowMutex) {            
            for (NodeID id : m_workflow.getNodeIDs()) {
                boolean hasNonParentPredecessors = false;
                for (ConnectionContainer cc
                        : m_workflow.getConnectionsByDest(id)) {
                    if (!cc.getSource().equals(this.getID())) {
                        hasNonParentPredecessors = true;
                        break;
                    }
                }
                if (!hasNonParentPredecessors) {
                    resetAndConfigureNode(id);
                }
            }
        }
    }

    /**
     * Reset all nodes in this workflow.
     * Note that this routine will NOT trigger any resets connected to
     * possible outports of this WFM.
     */
    void resetAllNodesInWFM() {
        synchronized (m_workflowMutex) {
            if (!this.isResetable()) {
                // only attempt to do this if possible.
                return;
            }
            for (NodeID id : m_workflow.getNodeIDs()) {
                // we don't need to worry about the correct order since
                // we will reset everything in here anyway.
                NodeContainer nc = m_workflow.getNode(id);
                assert nc != null;
                if (nc instanceof SingleNodeContainer) {
                    ((SingleNodeContainer)nc).reset();
                } else {
                    assert nc instanceof WorkflowManager;
                    ((WorkflowManager)nc).resetAllNodesInWFM();
                }
            }
            // don't let the WFM decide on the state himself - for example,
            // if there is only one WFMTHROUGH connection contained, it will
            // produce wrong states! Force it to be idle.
            setState(State.IDLE);
        }
    }
    
    /** mark these nodes and all not-yet-executed predecessors for execution.
     * They will be marked first, queued when all inputs are available and
     * finally executed.
     *
     * @param ids node ids to mark
     */
    public void executeUpToHere(final NodeID... ids) {
        synchronized (m_workflowMutex) {
            for (NodeID id : ids) {
                markAndQueueNodeAndPredecessors(id, -1);
            }
            checkForNodeStateChanges(true);
        }
    }

    /** Find all nodes which are connected to a specific inport of a node
     * and try to mark/queue them.
     * 
     * @param id of node
     * @param inPortIndex index of inport
     * @return true of the marking was successful.
     */
    private boolean markAndQueuePredecessors(final NodeID id,
            final int inPortIndex) {
        assert m_workflow.getNode(id) != null;
        Set<ConnectionContainer> predConn = m_workflow.getConnectionsByDest(id);
        for (ConnectionContainer cc : predConn) {
            NodeID predID = cc.getSource();
            if (cc.getDestPort() == inPortIndex) {
                // those are the nodes we are interested in: us as destination
                // and our inport is connected to their outport
                if (predID.equals(this.getID())) {
                    // we are leaving this workflow!
                    if (!getParent().markAndQueuePredecessors(predID,
                            cc.getSourcePort())) {
                        // give up if this "branch" fails
                        return false;
                    }
                } else {
                    assert m_workflow.getNode(predID) != null;
                    // a "normal" node in this Workflow, mark it and its
                    // predecessors
                    if (!markAndQueueNodeAndPredecessors(predID,
                            cc.getSourcePort())) {
                        // give up if this "branch" fails
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    /** Recursively iterates the predecessors and marks them for execution
     * before marking (and possibly queuing) this node itself.
     * 
     * @param id The node whose predecessors are to marked for execution.
     * @param outPortIndex index of output port this request arrived from
     *   (or -1 if all ports are to be considered)
     *   
     * @return true if we found executable predecessor
     */
    private boolean markAndQueueNodeAndPredecessors(final NodeID id,
            final int outPortIndex) {
        assert !id.equals(this.getID());
        synchronized (m_workflowMutex) {
            NodeContainer thisNode = getNodeContainer(id);
            // first check some basic facts about this node:
            // 1) executed or executing? - done (and happy)
            if (thisNode.getState().equals(State.EXECUTED)
                    || thisNode.getState().executionInProgress()) {
                // everything fine: found "source" of chain in execution
                return true;
            }
            // 2) its a WFM:
            if (thisNode instanceof WorkflowManager) {
                // hand over control to the sub workflow (who will hand
                // back up to this level if there are (implicit or explicit)
                // through connections to follow:
                WorkflowManager wfm = (WorkflowManager)thisNode;
                return wfm.markForExecutionAllAffectedNodes(outPortIndex);
            }
            assert thisNode instanceof SingleNodeContainer;
            SingleNodeContainer snc = (SingleNodeContainer)thisNode;
            // 3) now we check if we are dealing with a source (there is no
            //   need to traverse further up then and we can cancel the
            //   operation if the source is in a non-executable condition.
            Set<ConnectionContainer> predConn
                            = m_workflow.getConnectionsByDest(id);
            if (thisNode.getNrInPorts() == 0) {
                assert predConn.size() == 0;
                State thisState = thisNode.getState();
                if (thisState.equals(State.CONFIGURED)) {
                    snc.markForExecution(true);
                    assert snc.getState().equals(State.MARKEDFOREXEC)
                        : "SingleNodeContainer " + snc
                        + " in unexpected state:" + snc.getState();
                    snc.queue(new PortObject[0]);
                    // we are now executing one of the sources of the chain
                    return true;
                } else {
                    // could not find executable predecessor!
                    return false;
                }
            }
            assert thisNode.getNrInPorts() > 0;
            // 4) we fail on nodes which are not fully connected:
            if (predConn.size() < thisNode.getNrInPorts()) {
                // do not deal with incompletely connected nodes!
                return false;
            }
            // 5) now let's see if we can mark the predecessors of this node
            //  (and this way trigger the backwards traversal)
            assert predConn.size() > 0;
            assert predConn.size() == thisNode.getNrInPorts();
            // handle nodes which are in the middle of a pipeline
            // (A) recurse up to all predecessors of this node (mark/queue them)
            for (ConnectionContainer cc : predConn) {
                NodeID predID = cc.getSource();
                if (predID.equals(getID())) {
                    // connection coming from outside this WFM
                    assert cc.getType().equals(
                            ConnectionContainer.ConnectionType.WFMIN);
                    NodeOutPort realPort = getInPort(cc.getSourcePort())
                                .getUnderlyingPort();
                    if (!realPort.getNodeState().equals(State.EXECUTED)
                            && !realPort.getNodeState().executionInProgress()) {
                        // the real predecessor node is not already marked/done:
                        // we have to mark the predecessor in the parent flow
                        if (!getParent().markAndQueuePredecessors(predID,
                                cc.getSourcePort())) {
                            return false;
                        }
                    }
                } else {
                    if (!markAndQueueNodeAndPredecessors(predID,
                            cc.getSourcePort())) {
                        return false;
                    }
                }
            }
            // (B) check if this node is markable and mark it!
            NodeOutPort[] predPorts = assemblePredecessorOutPorts(id);
            boolean canBeMarked = true;
            for (NodeOutPort portIt : predPorts) {
                if (portIt == null
                    || (!portIt.getNodeState().executionInProgress()
                        && portIt.getPortObject() == null)) {
                    canBeMarked = false;
                }
            }
            if (canBeMarked) {
                assert thisNode instanceof SingleNodeContainer;
                ((SingleNodeContainer)thisNode).markForExecution(true);
                queueIfQueuable(thisNode);
                return true;
            }
        }
        return false;
    }

    /** Queues the argument NC if possible. Does nothing if argument is
     * not marked. Resets marks if node is queuable (all predecessors are done)
     * but its state is still unconfigured (and marked for execution). This
     * will never change again so we can forget about executing also the rest.
     *
     * @param nc To queue if possible
     * @return whether successfully queued.
     */
    private boolean queueIfQueuable(final NodeContainer nc) {
        if (nc instanceof WorkflowManager) {
            return false;
        }
        assert Thread.holdsLock(m_workflowMutex);
        switch (nc.getState()) {
            case UNCONFIGURED_MARKEDFOREXEC:
            case MARKEDFOREXEC:
                break;
            default:
                assert false : "Queuing of " + nc.getNameWithID()
                    + "not possible, node is " + nc.getState();
                return false;
        }
        NodeOutPort[] ports = assemblePredecessorOutPorts(nc.getID());
        PortObject[] inData = new PortObject[ports.length];
        boolean allDataAvailable = true;
        for (int i = 0; i < ports.length; i++) {
            if (ports[i] != null) {
                inData[i] = ports[i].getPortObject();
            }
            allDataAvailable &= inData[i] != null;
        }
        if (allDataAvailable) {
            if (nc.getState().equals(State.MARKEDFOREXEC)) {
                assert nc instanceof SingleNodeContainer;
                SingleNodeContainer snc = (SingleNodeContainer)nc;
                snc.queue(inData);
                return true;
            } else {
                disableNodeForExecution(nc.getID());
                checkForNodeStateChanges(true);
                return false;
            }
        }
        return false;
    }

    /* -------------- State changing actions and testers ----------- */

    /** call-back from SingleNodeContainer called before node is actually
     * executed.
     *
     * @param snc SingleNodeContainer which finished execution in a JobExecutor
     * @throws IllegalContextStackObjectException If loop end nodes have 
     * problems identifying their start node
     */
    void doBeforeExecution(final SingleNodeContainer snc) {
        assert !snc.getID().equals(this.getID());
        synchronized (m_workflowMutex) {
            LOGGER.debug(snc.getNameWithID() + " doBeforeExecute");
            // allow SNC to update states etc
                if (snc.getLoopRole().equals(LoopRole.END)) {
                    // if this is an END to a loop, make sure it knows its head
                    ScopeLoopContext slc = snc.getNode().
                               getScopeContextStackContainer().peek(
                                       ScopeLoopContext.class);
                    if (slc == null) {
                        LOGGER.debug("Incoming scope object stack for "
                                + snc.getNameWithID() + ":\n"
                                + snc.getScopeObjectStack().toDeepString());
                        throw new IllegalContextStackObjectException(
                                "Encountered loop-end without " +
                                "corresponding head!");
                    } else if (slc instanceof RestoredScopeLoopContext) {
                        throw new IllegalContextStackObjectException(
                                "Can't continue loop as the workflow was "
                                + "restored with the loop being partially " 
                                + "executed. Reset loop start and execute "
                                + "entire loop again.");
                    }
                    NodeContainer headNode = m_workflow.getNode(slc.getOwner());
                    if (headNode == null) {
                        throw new IllegalContextStackObjectException(
                                "Loop start and end node must be in the same " 
                                + "workflow");
                    }
                    snc.getNode().setLoopStartNode(
                            ((SingleNodeContainer)headNode).getNode());
                } else {
                    // or not if it's any other type of node
                    snc.getNode().setLoopStartNode(null);
                }
                snc.preExecuteNode();
            checkForNodeStateChanges(true);
        }
    }

    /** cleanup a node after execution.
     *
     * @param snc SingleNodeContainer which just finished execution
     * @param success indicates if node execution was finished successfully
     *    (note that this does not imply State=EXECUTED e.g. for loop ends)
     */
    void doAfterExecution(final SingleNodeContainer snc,
            final boolean success) {
        assert !snc.getID().equals(this.getID());
        synchronized (m_workflowMutex) {
            String st = success ? " - success" : " - failure";
            LOGGER.debug(snc.getNameWithID() + " doAfterExecute" + st);
            if (!success) {
                // execution failed - clean up successors' execution-marks
                disableNodeForExecution(snc.getID());
                // and also any nodes which were waiting for this one to
                // be executed.
                for (ScopeObject so : snc.getWaitingLoops()) {
                    disableNodeForExecution(so.getOwner());
                }
                snc.clearWaitingLoopList();
            }
            // allow SNC to update states etc
            snc.postExecuteNode(success);
            boolean canConfigureSuccessors = true;
            // process loop context - only for "real" nodes:
            if (snc.getLoopRole().equals(LoopRole.BEGIN)) {
                // if this was BEGIN, it's not anymore (until we do not
                // restart it explicitly!)
                snc.getNode().setLoopEndNode(null);
            }
            if (snc.getLoopRole().equals(LoopRole.END)) {
                // no matter what happened, try to clean up the stack.
                ScopeLoopContext slc =
                    snc.getScopeObjectStack().pop(ScopeLoopContext.class);
                assert !success || slc != null 
                    : "No Loop start for this Loop End. This should have been "
                        + "caught in doBeforeExecution";
                assert !success || !(slc instanceof RestoredScopeLoopContext) 
                    : "Can't continue loop as the workflow was restored " 
                            + "with the loop being partially executed. This " 
                            + "should have been caught in doBeforeExecution";
            }
            Node node = snc.getNode();
            if (node.getLoopStatus() != null) {
                // we are supposed to execute this loop again!
                // first retrieve ScopeContext
                ScopeLoopContext slc = node.getLoopStatus();
                // first check if the loop is properly configured:
                if (m_workflow.getNode(slc.getOwner()) == null) {
                    // obviously not: origin of the loop is not in this WFM!
                    // nothing else to do: NC stays configured
                    assert snc.getState() == NodeContainer.State.CONFIGURED;
                    // and choke
                    throw new IllegalContextStackObjectException(
                            "Loop nodes are not in the same workflow!");
                } else {
                    // make sure the end of the loop is properly
                    // configured:
                    slc.setTailNode(snc.getID());
                    // and try to restart loop
                    restartLoop(slc);
                    // clear stack (= loop context)
                    node.clearLoopStatus();
                    // and make sure we do not accidentally configure the
                    // remainder of this node since we are not yet done
                    // with the loop
                    canConfigureSuccessors = false;
                }
            }
            if (snc.getWaitingLoops().size() >= 1) {
                // looks as if some loops were waiting for this node to
                // finish! Let's try to restart them:
                for (ScopeLoopContext slc : snc.getWaitingLoops()) {
                    restartLoop(slc);
                }
                snc.clearWaitingLoopList();
            }
            if (canConfigureSuccessors) {
                // may be SingleNodeContainer or WFM contained within this
                // one but then it can be treated like a SNC
                configureNodeAndSuccessors(snc.getID(), false);
            }
            checkForNodeStateChanges(true);
        }
    }

    /** Restart execution of a loop if possible. Can delay restart if
     * we are still waiting for some node in the loop body (or any
     * dangling loop branches) to finish execution
     *
     * @param sc ScopeObject of the actual loop
     */
    private void restartLoop(final ScopeLoopContext slc) {
        NodeContainer tailNode = m_workflow.getNode(slc.getTailNode());
        NodeContainer headNode = m_workflow.getNode(slc.getOwner());
        if ((tailNode == null) || (headNode == null)) {
            throw new IllegalStateException("Loop Nodes must both"
                    + " be in the same workflow!");
        }
        if (!(tailNode instanceof SingleNodeContainer)
                || !(headNode instanceof SingleNodeContainer)) {
            throw new IllegalStateException("Loop Nodes must both"
                    + " be SingleNodeContainers!");
        }
        // (1) find all intermediate node, the loop's "body"
        List<NodeID> loopBodyNodes = findAllNodesConnectedToLoopBody(
                headNode.getID(), tailNode.getID());
        // (2) check if any of those nodes are still waiting to be
        //     executed or currently executing
        for (NodeID id : loopBodyNodes) {
            NodeContainer currNode = m_workflow.getNode(id);
            if (currNode.getState().executionInProgress()) {
                // stop right here - loop can not yet be restarted!
                currNode.addWaitingLoop(slc);
                return;
            }
            if (this.canExecuteNode(id)) {
                // we missed some nodes during the initial marking - most
                // likely because these are in an untouched branch. Mark
                // them now and return for now.
                this.markAndQueueNodeAndPredecessors(id, -1);
                currNode.addWaitingLoop(slc);
                return;
            }
        }
        // (3) reset the nodes in the body (only those -
        //     make sure end of loop is NOT reset)
        for (NodeID id : loopBodyNodes) {
            NodeContainer nc = m_workflow.getNode(id);
            if (nc == null) {
                throw new IllegalStateException("Node in loop body not in"
                    + " same workflow as head&tail!");
            }
            if (nc instanceof SingleNodeContainer) {
                ((SingleNodeContainer)nc).reset();
            } else {
                assert nc instanceof WorkflowManager;
                // FIXME: only reset the nodes connected to relevant ports
                ((WorkflowManager)nc).resetAllNodesInWFM();
            }
        }
        // (4) configure the nodes from start to rest (it's not
        //     so important if we configure more than the body)
        //     do NOT configure start of loop because otherwise
        //     we will re-create the ScopeContextStack and
        //     remove the loop-object as well!
        configureNodeAndSuccessors(headNode.getID(), false);
        // the current node may have thrown an exception inside
        // configure, so we have to check here if the node
        // is really configured before...
        if (tailNode.getState().equals(State.CONFIGURED)) {
            // (5) ... we enable the body to be queued again.
            for (NodeID id : loopBodyNodes) {
                NodeContainer nc = m_workflow.getNode(id);
                if (nc instanceof SingleNodeContainer) {
                    ((SingleNodeContainer)nc).markForExecution(true);
                } else {
                    // FIXME - check ports
                    ((WorkflowManager)nc).markForExecutionAllNodesInWorkflow(true);
                }
            }
            // and (6) mark end of loop for re-execution
            ((SingleNodeContainer)tailNode).markForExecution(true);
        }
        // (7) allow access to tail node
        ((SingleNodeContainer)headNode).getNode().setLoopEndNode(
                ((SingleNodeContainer)tailNode).getNode());
        // (8) mark the origin of the loop to be executed again
        ((SingleNodeContainer)headNode).enableReQueuing();
        // (9) try to queue the head of this loop!
        assert headNode.getState().equals(State.MARKEDFOREXEC);
        queueIfQueuable(headNode);
    }

    /** Create list of nodes (id)s that are part of a loop body. Note that
     * this also includes any dangling branches which leave the loop but
     * do not connect back to the end-node. Used to re-execute all nodes
     * of a loop.
     * The list does not contain the start node or end node.
     *
     * @param startNode id of head of loop
     * @param endNode if of tail of loop
     * @return list of nodes within loop body & any dangling branches
     */
    private List<NodeID> findAllNodesConnectedToLoopBody(final NodeID startNode,
            final NodeID endNode) {
        ArrayList<NodeID> matchingNodes = new ArrayList<NodeID>();
        if (startNode.equals(endNode)) {
            // silly case
            return matchingNodes;
        }
        matchingNodes.add(startNode);
        int currIndex = 0;
        while (currIndex < matchingNodes.size()) {
            NodeID currID = matchingNodes.get(currIndex);
            for (ConnectionContainer cc : m_workflow.getConnectionsBySource(currID)) {
                assert (cc.getSource().equals(currID));
                NodeID succID = cc.getDest();
                if (succID.equals(this.getID())) {
                    // if any branch leaves this WFM, complain!
                    throw new IllegalContextStackObjectException(
                            "Loops are not permitted to leave workflows!");
                }
                if ((!succID.equals(endNode))
                        && (!matchingNodes.contains(succID))) {
                    matchingNodes.add(succID);
                }
            }
            currIndex += 1;
        }
        matchingNodes.remove(startNode);
        return matchingNodes;
    }

    /** check if node can be safely reset. In case of a WFM we will check
     * if one of the internal nodes can be reset and none of the nodes
     * are "in progress".
     * 
     * @return if all internal nodes can be reset.
     */
    @Override
    boolean isResetable() {
        // first check if there is a node in execution
        for (NodeContainer nc : m_workflow.getNodeValues()) {
            if (nc.getState().executionInProgress()) {
                return false;
            }
        }
        // check for through connection
        for (ConnectionContainer cc
                : m_workflow.getConnectionsBySource(getID())) {
            if (cc.getType().equals(
                    ConnectionContainer.ConnectionType.WFMTHROUGH)) {
                return true;
            }
        }
        // check for at least one resetable node!
        for (NodeContainer nc : m_workflow.getNodeValues()) {
            if (nc.isResetable()) {
                return true;
            }
        }
        // nothing of the above: false.
        return false;
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
        NodeContainer nc = m_workflow.getNode(nodeID);
        if (nc == null) {
            return false;
        }
        // (a) this node is resetable
        // (b) no successors is running or queued.
        return (nc.isResetable()
               && (!hasSuccessorInProgress(nodeID)));
    }

    // TODO: This function needs to go!
    // replaced by invokeResetOnSNC and invokeResetOnPortSuccessors...
    @Deprecated
    private void invokeResetOnNode(final NodeID nodeID) {
        assert Thread.holdsLock(m_workflowMutex);
        NodeContainer nc = getNodeContainer(nodeID);
        if (nc instanceof SingleNodeContainer) {
            ((SingleNodeContainer)nc).reset();
        } else {
            // TODO - this case should never happen but can not yet be
            // guaranteed since Bernd's persistor grap calls it left and right.
            assert nc instanceof WorkflowManager;
            ((WorkflowManager)nc).resetAll();
        }
    }

    /** Reset node and notify listeners. */
    private void invokeResetOnSingleNodeContainer(
            final SingleNodeContainer snc) {
        assert Thread.holdsLock(m_workflowMutex);
        snc.reset();
    }

    /** Reset those nodes which are connected to a specific workflow
     * incoming port.
     * 
     * @param inportIndex index of port.
     */
    void invokeResetOnPortSuccessors(final int inportIndex) {
        synchronized (m_workflowMutex) {
            // will contain source nodes (meta input nodes) connected to
            // correct port
            HashMap<NodeContainer, Integer> sourceNodes
                    = new HashMap<NodeContainer, Integer>();
            // find all nodes that are directly connected to this meta nodes
            // input port
            for (ConnectionContainer cc
                          : m_workflow.getConnectionsBySource(getID())) {
                NodeID id = cc.getDest();
                NodeContainer nc = m_workflow.getNode(id);
                if ((cc.getType().equals(ConnectionType.WFMIN))
                        &&    ((inportIndex < 0)
                            || (cc.getSourcePort() == inportIndex))) {
                    // avoid WFMTHROUGH connections
                    sourceNodes.put(nc, cc.getDestPort());
                }
            }
            // and then reset those nodes (all of their successors will be
            // reset automatically)
            for (NodeContainer nc : sourceNodes.keySet()) {
                assert !this.getID().equals(nc.getID());
                if (nc.isResetable()) {
                    this.resetSuccessors(nc.getID());
                    if (nc instanceof SingleNodeContainer) {
                        invokeResetOnSingleNodeContainer(
                                (SingleNodeContainer)nc);
                    } else {
                        assert nc instanceof WorkflowManager;
                        int portIndex = sourceNodes.get(nc);
                        ((WorkflowManager)nc).invokeResetOnPortSuccessors(
                                portIndex);
                    }
                }
            }
        }
        checkForNodeStateChanges(true);
    }

    /**
     * Test if successors of a node are currently executing.
     *
     * @param nodeID id of node
     * @return true if at least one successors is currently in progress.
     */
    private boolean hasSuccessorInProgress(final NodeID nodeID) {
        NodeContainer nc = m_workflow.getNode(nodeID);
        if (nc == null) {  // we are talking about this WFM
            assert nodeID.equals(this.getID());
            return getParent().hasSuccessorInProgress(nodeID);
        }
        // else it's a node inside the WFM
        for (ConnectionContainer cc : m_workflow.getConnectionsBySource(nodeID)) {
            switch (cc.getType()) {
            case WFMIN:
            case WFMTHROUGH:
                assert false : "Outgoing connection can't be of type " +
                    cc.getType();
            case STD:
                NodeID succID = cc.getDest();
                NodeContainer succNC = getNodeContainer(succID);
                if (succNC.getState().executionInProgress()
                        || hasSuccessorInProgress(succID)) {
                    return true;
                }
                break;
            case WFMOUT:
                // TODO check only nodes connection to the specific WF outport
                if (getParent().hasSuccessorInProgress(getID())) {
                    return true;
                }
            }
        }
        return false;
    }

    //////////////////////////////////////////////////////////
    // NodeContainer implementations (WFM acts as meta node)
    //////////////////////////////////////////////////////////

    /** Reset node and all executed successors of a specific node.
    *
    * @param id of first node in chain to be reset.
    */
    public void resetAndConfigureNode(final NodeID id) {
        synchronized (m_workflowMutex) {
            NodeContainer nc = getNodeContainer(id);
            if (!hasSuccessorInProgress(id)) {
                // if that's not the case: problem!
                if (nc.isResetable()) {
                    // only then it makes sense to reset/configure this
                    // Node and also its successors. Otherwise stop.
                    //
                    // Reset all successors first
                    resetSuccessors(id);
                    // and then reset node itself
                    if (nc instanceof SingleNodeContainer) {
                        invokeResetOnSingleNodeContainer(
                                (SingleNodeContainer)nc);
                    } else {
                        assert nc instanceof WorkflowManager;
                        // this is ok, since we will never call this again
                        // while traversing a flow - this is the main entry
                        // point from the outside and should reset all kids.
                        // (resetSuccessors() follows ports and will be
                        // called throughout subsequent calls...)
                        ((WorkflowManager)nc).resetAll();
                    }
                    // and launch configure starting with this node
                    configureNodeAndSuccessors(id, true);
                }
            } else {
                throw new IllegalStateException(
                "Can not reset node (wrong state of node or successors) " + id);
            }
        }
        checkForNodeStateChanges(true);
    }

    /**
     * Reset successors of a node. Do not reset node itself since it
     * may be a metanode from within we started this.
     *
     * @param id of node
     */
    private void resetSuccessors(final NodeID id) {
        resetSuccessors(id, -1);
    }

    /*
     * Reset successors of a node connected to a specific port. If id == -1
     * reset successors connected to all nodes.
     */
    private void resetSuccessors(final NodeID id, final int portID) {
        assert Thread.holdsLock(m_workflowMutex);
        Set<ConnectionContainer> succs = m_workflow.getConnectionsBySource(id);
        for (ConnectionContainer conn : succs) {
            NodeID currID = conn.getDest();
            if ((conn.getSourcePort() == portID)
                || (portID < 0)) {
                // only reset successors if they are connected to the
                // correct port or we don't care (id==-1)
                if (!conn.getType().isLeavingWorkflow()) {
                    assert m_workflow.getNode(currID) != null;
                    // normal connection to another node within this workflow
                    // first check if it is already reset
                    NodeContainer nc = m_workflow.getNode(currID);
                    assert nc != null;
                    if (nc.isResetable()) {
                        // first reset successors of successor
                        if (nc instanceof SingleNodeContainer) {
                            // for a normal node, ports don't matter
                            this.resetSuccessors(currID, -1);
                            // ..then reset immediate successor itself
                            invokeResetOnSingleNodeContainer(
                                (SingleNodeContainer)nc);
                        } else {
                            assert nc instanceof WorkflowManager;
                            WorkflowManager wfm = (WorkflowManager)nc;
                            // first reset all nodes which are connected
                            // to the outports of interest of this WFM...
                            Set<Integer> outcomingPorts
                                = wfm.m_workflow.connectedOutPorts(
                                         conn.getDestPort());
                            for (Integer i : outcomingPorts) {
                                this.resetSuccessors(currID, i);
                            }
                            // ...then reset nodes inside WFM.
                            ((WorkflowManager)nc).invokeResetOnPortSuccessors(
                                    conn.getDestPort());
                        }
                    }
                } else {
                    assert this.getID().equals(currID);
                    // connection goes to a meta outport!
                    // Only reset nodes which are connected to the currently
                    // interesting port.
                    int outGoingPortID = conn.getDestPort();
                    getParent().resetSuccessors(this.getID(), outGoingPortID);
                }
            }
        }
        checkForNodeStateChanges(true);
    }

    /** Check if a node can be executed directly.
     *
     * @param nodeID id of node
     * @return true of node is configured and all immediate predecessors are
     *              executed.
     */
    public boolean canExecuteNode(final NodeID nodeID) {
        synchronized (m_workflowMutex) {
            NodeContainer nc = m_workflow.getNode(nodeID);
            if (nc == null) {
                return false;
            }
            // check for WorkflowManager - which we handle differently
            if (nc instanceof WorkflowManager) {
                // simply check if there is ANY excutable node in this
                // WFM. If yes: return true.
                WorkflowManager wfm = (WorkflowManager)nc;
                for (NodeID id : wfm.m_workflow.getNodeIDs()) {
                    if (wfm.canExecuteNode(id)) {
                        return true;
                    }
                }
            } else {
                assert nc instanceof SingleNodeContainer;
                // node itself needs to be configured.
                if (nc.getState().equals(NodeContainer.State.CONFIGURED)) {
                    return true;
                }
            }
        }
        // all other cases: not executable!
        return false;
    }

    /**
     * Cancel execution of all children.
     */
    void cancelExecutionOfAllChildren() {
        for (NodeContainer nc : m_workflow.getNodeValues()) {
            // TODO may need to be sorted last-first.
            if (nc instanceof SingleNodeContainer) {
                ((SingleNodeContainer)nc).cancelExecution();
            } else {
                assert nc instanceof WorkflowManager;
                ((WorkflowManager)nc).cancelExecutionOfAllChildren();
            }
        }
    }
    
    /**
     * Cancel execution of the given NodeContainer.
     *
     * @param nc node to be canceled
     */
    public void cancelExecution(final NodeContainer nc) {
        assert nc != null;
        disableNodeForExecution(nc.getID());
        synchronized (m_workflowMutex) {
            if (nc.getState().executionInProgress()) {
                if (nc instanceof SingleNodeContainer) {
                    ((SingleNodeContainer)nc).cancelExecution();
                } else {
                    assert nc instanceof WorkflowManager;
                    ((WorkflowManager)nc).cancelExecutionOfAllChildren();
                }
            }
        }
    }

   /**
    * Convenience method: execute all and wait for execution to be done.
    *
    * @return true if execution was successful
    */
    public boolean executeAllAndWaitUntilDone() {
        if (this == ROOT) {
            throw new IllegalStateException("Can't execute ROOT workflow");
        }
        final Object mySemaphore = new Object();
        this.addNodeStateChangeListener(new NodeStateChangeListener() {
            /** {@inheritDoc} */
            public void stateChanged(final NodeStateEvent state) {
                synchronized (mySemaphore) {
                    mySemaphore.notifyAll();
                }
            }
        });
        executeAll();
        synchronized (mySemaphore) {
            while (getState().executionInProgress()) {
                try {
                    mySemaphore.wait();
                } catch (InterruptedException ie) {
                    cancelExecutionOfAllChildren();
                    return false;
                }
            }
        }
        return this.getState().equals(State.EXECUTED);
    }

    /** Convenience method: (Try to) Execute all nodes in the workflow.
     * This method returns immediately, leaving it to the associated
     * executor to do the job. */
    public void executeAll() {
        Set<NodeID> endNodes = new HashSet<NodeID>();
        for (NodeID id : m_workflow.getNodeIDs()) {
            boolean hasNonParentSuccessors = false;
            for (ConnectionContainer cc
                    : m_workflow.getConnectionsBySource(id)) {
                if (!cc.getDest().equals(this.getID())) {
                    hasNonParentSuccessors = true;
                    break;
                }
            }
            if (!hasNonParentSuccessors) {
                endNodes.add(id);
            }
        }
        // now use these "end nodes" to start executing all until we
        // reach the beginning. Do NOT leave the workflow, though.
        Set<NodeID> executedNodes = new HashSet<NodeID>();
        while (endNodes.size() > 0) {
            NodeID thisID = endNodes.iterator().next();
            endNodes.remove(thisID);
            // move all of the predecessors to the "end nodes"
            for (ConnectionContainer cc
                    : m_workflow.getConnectionsByDest(thisID)) {
                NodeID nextID = cc.getSource();
                if (!endNodes.contains(nextID)
                        && !executedNodes.contains(nextID)
                        && !nextID.equals(this.getID())) {
                    endNodes.add(nextID);
                }
            }
            // try to execute the current node
            NodeContainer nc = m_workflow.getNode(thisID);
            if (nc instanceof SingleNodeContainer) {
                executeUpToHere(thisID);
            } else {
                assert nc instanceof WorkflowManager;
                ((WorkflowManager)nc).executeAll();
            }
            // and finally move the current node to the other list
            executedNodes.add(thisID);
        }
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
    NodeDialogPane getDialogPaneWithSettings(
            final PortObjectSpec[] inSpecs) throws NotConfigurableException {
        throw new IllegalStateException("Workflow has no dialog (yet)");
    }

    /** {@inheritDoc} */
    @Override
    NodeDialogPane getDialogPane() {
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
     * @return global table repository for this WFM.
     */
    HashMap<Integer, ContainerTable> getGlobalTableRepository() {
        return m_globalTableRepository;
    }

    /**
     * Check if any internal nodes have changed state which might mean that
     * this WFM also needs to change its state...
     * @param propagateChanges Whether to also inform this wfm's parent if done
     * (true always except for loading)
     */
    private void checkForNodeStateChanges(final boolean propagateChanges) {
        // TODO enable this assertion
//        assert Thread.holdsLock(m_workflowMutex);
        int[] nrNodesInState = new int[State.values().length];
        int nrNodes = 0;
        boolean internalNodeHasError = false;
        for (NodeContainer ncIt : m_workflow.getNodeValues()) {
            nrNodesInState[ncIt.getState().ordinal()]++;
            nrNodes++;
            if ((ncIt.getNodeMessage() != null)
                    && (ncIt.getNodeMessage().getMessageType().equals(
                            NodeMessage.Type.ERROR))) {
                internalNodeHasError = true;
            }
        }
        // set summarization message if any of the internal nodes has an error
        if (internalNodeHasError) {
            setNodeMessage(new NodeMessage(
                    NodeMessage.Type.ERROR, "Error in sub flow."));
        } else {
            setNodeMessage(NodeMessage.NONE);
        }
        //
        assert nrNodes == m_workflow.getNrNodes();
        NodeContainer.State newState = State.IDLE;
        // check if all outports are connected
        boolean allOutPortsConnected =
            getNrOutPorts() == m_workflow.getConnectionsByDest(
                                                    this.getID()).size();
        // check if we have complete Objects on outports
        boolean allPopulated = false;
        // ...and at the same time find the "smallest" common state of
        // all inports (useful when all internal nodes are green but we
        // have through connections!
        State inportState = State.EXECUTED;
        if (allOutPortsConnected) {
            allPopulated = true;
            for (int i = 0; i < getNrOutPorts(); i++) {
                NodeOutPort nop = getOutPort(i).getUnderlyingPort();
                if (nop == null) {
                    allPopulated = false;
                    inportState = State.IDLE;
                } else if (nop.getPortObject() == null) {
                    allPopulated = false;
                    inportState = State.CONFIGURED;
                    if (nop.getNodeState().equals(State.IDLE)
                            || nop.getNodeState().equals(
                                    State.UNCONFIGURED_MARKEDFOREXEC)) {
                        inportState = State.IDLE;
                    }
                }
            }
        }
        if (nrNodes == 0) {
            // special case: zero nodes!
            if (allOutPortsConnected) {
                newState = allPopulated ? State.EXECUTED : State.CONFIGURED;
            } else {
                newState = State.IDLE;
            }
        } else if (nrNodesInState[State.EXECUTED.ordinal()] == nrNodes) {
            // WFM is executed only if all (>=1) nodes are executed and
            // all output ports are connected and contain their
            // portobjects.
            if (allPopulated) {
                // all nodes in WFM done and all ports populated!
                newState = State.EXECUTED;
            } else {
                // all executed but some though connections!
                newState = inportState;
            }
        } else if (nrNodesInState[State.CONFIGURED.ordinal()] == nrNodes) {
            // all (>=1) configured
            if (allOutPortsConnected) {
                newState = State.CONFIGURED;
            } else {
                newState = State.IDLE;
            }
        } else if (nrNodesInState[State.EXECUTING.ordinal()] >= 1) {
            newState = State.EXECUTING;
        } else if (nrNodesInState[State.EXECUTED.ordinal()]
                   + nrNodesInState[State.CONFIGURED.ordinal()]
                   == nrNodes) {
            newState = State.CONFIGURED;
        } else if ((nrNodesInState[State.QUEUED.ordinal()] >= 1)
                || (nrNodesInState[State.EXECUTING.ordinal()] >= 1)) {
            newState = State.EXECUTING;
        } else if (nrNodesInState[State.UNCONFIGURED_MARKEDFOREXEC.ordinal()]
                                  >= 1) {
            newState = State.UNCONFIGURED_MARKEDFOREXEC;
        } else if (nrNodesInState[State.MARKEDFOREXEC.ordinal()] >= 1) {
            newState = State.MARKEDFOREXEC;
        }
        State oldState = this.getState();
        this.setState(newState, propagateChanges);
        if (oldState.equals(State.EXECUTING)) {
            if (newState.equals(State.EXECUTED)) {
                // we just successfully executed this WFM: check if any
                // loops were waiting for this one in the parent workflow!
                if (getWaitingLoops().size() >= 1) {
                    // looks as if some loops were waiting for this node to
                    // finish! Let's try to restart them:
                    for (ScopeLoopContext slc : getWaitingLoops()) {
                        getParent().restartLoop(slc);
                    }
                    clearWaitingLoopList();
                }
            } else if (!newState.equals(State.EXECUTING)) {
                // something went wrong - if other any loops were waiting
                // for this node: clean them up!
                for (ScopeObject so : getWaitingLoops()) {
                    getParent().disableNodeForExecution(so.getOwner());
                }
                clearWaitingLoopList();
            }
        }
        if (inportState.equals(State.IDLE)) {
            newState = State.IDLE;
        }
        if (inportState.equals(State.CONFIGURED)
                && (!newState.equals(State.IDLE))
                && (!newState.equals(State.UNCONFIGURED_MARKEDFOREXEC))) {
            newState = State.CONFIGURED;
        }
        if ((!oldState.equals(newState))
                && (getParent() != null) && propagateChanges) {
            // make sure parent WFM reflects state changes
            getParent().checkForNodeStateChanges(propagateChanges);
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
        NodeContainer nc = getNodeContainer(id);
        int nrIns = nc.getNrInPorts();
        NodeOutPort[] result = new NodeOutPort[nrIns];
        Set<ConnectionContainer> incoming = m_workflow.getConnectionsByDest(id);
        for (ConnectionContainer conn : incoming) {
            assert conn.getDest().equals(id);
            // get info about destination
            int destPortIndex = conn.getDestPort();
            int portIndex = conn.getSourcePort();
            if (conn.getSource() != this.getID()) {
                // connected to another node inside this WFM
                assert conn.getType() == ConnectionType.STD;
                result[destPortIndex] =
                    m_workflow.getNode(conn.getSource()).getOutPort(portIndex);
            } else {
                // connected to a WorkflowInport
                assert conn.getType() == ConnectionType.WFMIN;
                result[destPortIndex] = getWorkflowIncomingPort(portIndex);
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
        }
        NodeContainer nc = getNodeContainer(id);
        // Note that this enforces FULLY connected nodes
        return m_workflow.getConnectionsByDest(id).size() == nc.getNrInPorts();
    }

    /*
     * Configure successors of this workflow connected to a specific port
     * (avoids duplicate configure calls)
     */
    private void configureWorkFlowPortSuccessors(final int port) {
        LOGGER.debug("Attempting to configure meta node " + this.getID()
                + " port " + port + " successors.");
        LOGGER.debug("List=" + m_workflow.getConnectionsBySource(getID()));
        synchronized (m_workflowMutex) {
            for (ConnectionContainer cc
                                : m_workflow.getConnectionsBySource(getID())) {
                if ((port < 0) || (cc.getSourcePort() == port)) {
                    NodeID succNode = cc.getDest();
                    if (cc.getType().isLeavingWorkflow()) {
                        assert succNode.equals(getID());
                        assert cc.getType()
                         .equals(ConnectionContainer.ConnectionType.WFMTHROUGH);
                        getParent().configureNodeSuccessors(this.getID(),
                                cc.getDestPort());
                    } else {
                        LOGGER.debug("Attempting to configure node "
                                + succNode);
                        configureNodeAndSuccessors(succNode, true);
                    }
                }
            }
            checkForNodeStateChanges(true);
        }
    }

    /** Configure a SingleNodeContainer.
     * 
     * @param snc node to be configured
     * @return true if the configuration did change something.
     */
    private boolean configureSingleNodeContainer(
            final SingleNodeContainer snc) {
        boolean configurationChanged = false;
        synchronized (m_workflowMutex) {
            final int inCount = snc.getNrInPorts();
            NodeOutPort[] predPorts = assemblePredecessorOutPorts(snc.getID());
            final PortObjectSpec[] inSpecs = new PortObjectSpec[inCount];
            final ScopeObjectStack[] scscs = new ScopeObjectStack[inCount];
            final HiLiteHandler[] hiliteHdls = new HiLiteHandler[inCount];
            // check for presence of input specs
            boolean allSpecsExists = true;
            for (int i = 0; i < predPorts.length; i++) {
                if (predPorts[i] != null) {
                    inSpecs[i] = predPorts[i].getPortObjectSpec();
                    scscs[i] = predPorts[i].getScopeContextStackContainer();
                    hiliteHdls[i] = predPorts[i].getHiLiteHandler();
                }
                allSpecsExists &= inSpecs[i] != null;
            }
            if (!allSpecsExists) {
                // only configure nodes with all Input Specs present
                return false;
            }
            // configure node only if it's not yet running, queued or done.
            // This can happen if the WFM queues a node which has more than
            // one predecessor with populated output ports but one of the
            // nodes still has not called the "doAfterExecution()" routine
            // which might attempt to configure an already queued node again
            switch (snc.getState()) {
            case IDLE:
            case CONFIGURED:
            case UNCONFIGURED_MARKEDFOREXEC:
            case MARKEDFOREXEC:
                // create new ScopeContextStack
                ScopeObjectStack oldSOS = null;
                oldSOS = snc.getScopeObjectStack();
                ScopeObjectStack scsc;
                boolean scopeStackConflict = false;
                try {
                    scsc = new ScopeObjectStack(snc.getID(), scscs);
                } catch (IllegalContextStackObjectException e) {
                    LOGGER.warn("Unable to merge scope object stacks: " 
                            + e.getMessage(), e);
                    scsc = new ScopeObjectStack(snc.getID());
                    scopeStackConflict = true;
                }
                if (snc.getLoopRole().equals(LoopRole.BEGIN)) {
                    // the stack will automatically add the ID of the
                    // head of the loop (the owner!)
                    ScopeLoopContext slc = new ScopeLoopContext();
                    scsc.push(slc);
                }
                // TODO: the stack is invalid if if a predecessor 
                // is a loop end node, need to pop...()
                snc.setScopeObjectStack(scsc);
                // update HiLiteHandlers on inports of SNC only
                // TODO think about it... happens magically
                for (int i = 0; i < inCount; i++) {
                    snc.setInHiLiteHandler(i, hiliteHdls[i]);
                }
                // remember HiLiteHandler on OUTPORTS of all nodes!
                HiLiteHandler[] oldHdl =
                    new HiLiteHandler[snc.getNrOutPorts()];
                for (int i = 0; i < oldHdl.length; i++) {
                    oldHdl[i] = snc.getOutPort(i).getHiLiteHandler();
                }
                // configure node itself
                boolean outputSpecsChanged = false;
                if (scopeStackConflict && !snc.getState().equals(State.IDLE)) {
                    // FIXME how to invalidate the configure state of the node?
                    snc.reset(); // can't configured due to stack clash
                    // different output if any output spec was non-null
                    for (PortObjectSpec s : inSpecs) {
                        if (s != null) {
                            outputSpecsChanged = true;
                        }
                    }
                } else {
                    outputSpecsChanged = snc.configure(inSpecs);
                }

                // check if ScopeContextStacks have changed
                boolean stackChanged = false;
                // TODO: once SOS.equals() actually works...
                stackChanged = !snc.getScopeObjectStack().isEmpty()
                  || (oldSOS != null && !oldSOS.isEmpty());
//                    stackChanged = snc.getScopeObjectStack().equals(old_sos);
                // check if HiLiteHandlers have changed
                boolean hiLiteHdlsChanged = false;
                for (int i = 0; i < oldHdl.length; i++) {
                    HiLiteHandler hdl = snc.getOutPort(i).getHiLiteHandler();
                    hiLiteHdlsChanged |= (hdl != oldHdl[i]);
                }
                configurationChanged = (outputSpecsChanged || stackChanged
                        || hiLiteHdlsChanged);
                // and finally check if we can queue this node!
                if (snc.getState().equals(State.UNCONFIGURED_MARKEDFOREXEC)
                        || snc.getState().equals(State.MARKEDFOREXEC)) {
                    queueIfQueuable(snc);
                }
                break;
            case EXECUTED:
                // should not happen but could if reset has worked on slightly
                // different nodes than configure, for instance.
                LOGGER.error("configure found EXECUTED node! Graph traversal?");
                break;
            default:

            }
        }
//        return configurationChanged;
        // we have a problem here - subsequent metanodes with through connections
        // need to be configured no matter what - they can change their state
        // because 3 nodes before in the pipeline the execute state changed...
        return configurationChanged == configurationChanged;
    }

    /** Configure the nodes in WorkflowManager, connected to a specific port.
     * If not are given, configure all.
     * 
     * @param wfm the WorkflowManager
     * @param inportIndex index of incoming port (or -1 if not known)
     */
    private void configureNodesConnectedToPortInWFM(final WorkflowManager wfm,
            final int inportIndex) {
        synchronized (m_workflowMutex) {
            // configure node only if it's not yet completely executed.
            if (wfm.getState().equals(State.EXECUTED)) {
                return;
            }
            // TODO: we can put our own
            // objects onto the stack here (to clean up later)?
            wfm.configureWorkFlowPortSuccessors(inportIndex);            
            // and finalize stuff
            checkForNodeStateChanges(true);
            // TODO: clean up scope object stack after we leave WFM?
        }
    }
    
    /**
     * Configure node and, if this node's output specs have changed
     * also configure it's successors.
     *
     * @param id of node to configure
     * @param configureMyself true if the node itself is to be configured
     */
    private void configureNodeAndSuccessors(final NodeID nodeId,
            final boolean configureMyself) {
        // create list of properly ordered nodes (each one appears only once!)
        LinkedHashMap<NodeID, Set<Integer>> nodes
             = m_workflow.getBreadthFirstListOfNodeAndSuccessors(nodeId, false);
        // remember which ones we did configure to avoid useless configurations
        // (this list does not contain nodes where configure() didn't change
        // the specs/handlers/stacks.
        HashSet<NodeID> freshlyConfiguredNodes = new HashSet<NodeID>();
        // if not desired, don't configure origin
        if (!configureMyself) {
            // don't configure origin...
            nodes.remove(nodeId);
            // ...but pretend we did configure it
            freshlyConfiguredNodes.add(nodeId);
        }
        // Don't configure WFM itself but make sure the nodes connected
        // to this WFM are configured when we are done here...
        boolean wfmIsPartOfList = nodes.containsKey(this.getID());
        if (wfmIsPartOfList) {
            nodes.remove(this.getID());
        }
        // now iterate over the remaining nodes
        for (NodeID currNode : nodes.keySet()) {
            boolean needsConfiguration = currNode.equals(nodeId);
            for (ConnectionContainer cc
                             : m_workflow.getConnectionsByDest(currNode)) {
                if (freshlyConfiguredNodes.contains(cc.getSource())) {
                    needsConfiguration = true;
                }
            }
            if (!needsConfiguration) {
                continue;
            }
            final NodeContainer nc = getNodeContainer(currNode);
            synchronized (m_workflowMutex) {
                if (nc instanceof SingleNodeContainer) {
                    if (configureSingleNodeContainer((SingleNodeContainer)nc)) {
                        freshlyConfiguredNodes.add(nc.getID());
                    }
                } else {
                    assert nc instanceof WorkflowManager;
                    configureNodesConnectedToPortInWFM((WorkflowManager)nc, -1);
                    freshlyConfiguredNodes.add(nc.getID());
                }
            }
        }
        // make sure internal status changes are properly reflected
        checkForNodeStateChanges(true);
        // And finally clean up: if the WFM was part of the list of nodes
        // make sure we only configure nodes actually connected to ports
        // which are connected to nodes which we did configure!
        if (wfmIsPartOfList) {
            for (int i = 0; i < getNrWorkflowOutgoingPorts(); i++) {
                boolean portNeedsConfiguration = false;
                for (ConnectionContainer cc : m_workflow.getConnectionsByDest(
                        this.getID())) {
                    assert cc.getType().isLeavingWorkflow();
                    if ((cc.getDestPort() == i)
                        && (freshlyConfiguredNodes.contains(cc.getSource()))) {
                        portNeedsConfiguration = true;
                    }
                }
                if (portNeedsConfiguration) {
                    getParent().configureNodeSuccessors(this.getID(), i);
                }
            }
        }
    }
    
    /**
     * Configure successors of a specific port of a node.
     *
     * @param id of node to configure
     * @param index of output port successors are connected to (-1 if
     *   all are to be used).
     */
    private void configureNodeSuccessors(final NodeID nodeId,
            final int portIndex) {
        // FIXME: actually consider port index!!
        
        // create list of properly ordered nodes (each one appears only once!)
        LinkedHashMap<NodeID, Set<Integer>> nodes
             = m_workflow.getBreadthFirstListOfNodeAndSuccessors(nodeId, false);
        // remember which ones we did configure to avoid useless configurations
        // (this list does not contain nodes where configure() didn't change
        // the specs/handlers/stacks.
        HashSet<NodeID> freshlyConfiguredNodes = new HashSet<NodeID>();
        // if not desired, don't configure origin
        // don't configure origin...
        nodes.remove(nodeId);
        // ...but pretend we did configure it
        freshlyConfiguredNodes.add(nodeId);
        // Don't configure WFM itself but make sure the nodes connected
        // to this WFM are configured when we are done here...
        boolean wfmIsPartOfList = nodes.containsKey(this.getID());
        if (wfmIsPartOfList) {
            nodes.remove(this.getID());
        }
        // now iterate over the remaining nodes
        for (NodeID currNode : nodes.keySet()) {
            boolean needsConfiguration = currNode.equals(nodeId);
            for (ConnectionContainer cc
                             : m_workflow.getConnectionsByDest(currNode)) {
                if (freshlyConfiguredNodes.contains(cc.getSource())) {
                    needsConfiguration = true;
                }
            }
            if (!needsConfiguration) {
                continue;
            }
            final NodeContainer nc = getNodeContainer(currNode);
            synchronized (m_workflowMutex) {
                if (nc instanceof SingleNodeContainer) {
                    if (configureSingleNodeContainer((SingleNodeContainer)nc)) {
                        freshlyConfiguredNodes.add(nc.getID());
                    }
                } else {
                    assert nc instanceof WorkflowManager;
                    configureNodesConnectedToPortInWFM((WorkflowManager)nc, -1);
                    freshlyConfiguredNodes.add(nc.getID());
                }
            }
        }
        // make sure internal status changes are properly reflected
        checkForNodeStateChanges(true);
        // And finally clean up: if the WFM was part of the list of nodes
        // make sure we only configure nodes actually connected to ports
        // which are connected to nodes which we did configure!
        if (wfmIsPartOfList) {
            for (int i = 0; i < getNrWorkflowOutgoingPorts(); i++) {
                boolean portNeedsConfiguration = false;
                for (ConnectionContainer cc : m_workflow.getConnectionsByDest(
                        this.getID())) {
                    assert cc.getType().isLeavingWorkflow();
                    if ((cc.getDestPort() == i)
                        && (freshlyConfiguredNodes.contains(cc.getSource()))) {
                        portNeedsConfiguration = true;
                    }
                }
                if (portNeedsConfiguration) {
                    getParent().configureNodeAndSuccessors(this.getID(), false);
                }
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
        synchronized (m_workflowMutex) {
            build.append(getNameWithID());
            build.append(": " + getState() + " (start)\n");
            for (Map.Entry<NodeID, NodeContainer> it
                    : m_workflow.m_nodes.tailMap(prefix).entrySet()) {
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
        }
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
        return Collections.unmodifiableCollection(m_workflow.getNodeValues());
    }

    /**
     * @return collection of ConnectionContainer in this WFM
     */
    public Collection<ConnectionContainer> getConnectionContainers() {
        Set<ConnectionContainer> result =
            new LinkedHashSet<ConnectionContainer>();
        for (Set<ConnectionContainer> s
                : m_workflow.getConnectionsBySourceValues()) {
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
        NodeContainer nc = m_workflow.getNode(id);
        if (nc == null) {
            throw new IllegalArgumentException("No such node ID: " + id);
        }
        return nc;
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
        if (m_wfmListeners.isEmpty()) {
            return;
        }
        // the iterator is based on the current(!) set of listeners
        // (problem was: during load the addNodeContainer method fired an event
        // by using this method - the event got delivered at a point where
        // the workflow editor was registered and marked the flow as being dirty
        // although it was freshly loaded)
        final Iterator<WorkflowListener> it = m_wfmListeners.iterator(); 
        WORKFLOW_NOTIFIER.execute(new Runnable() {
            /** {@inheritDoc} */
            @Override
            public void run() {
                while (it.hasNext()) {
                    it.next().workflowChanged(evt);
                }
            }
        });
    }

    //////////////////////////////////////
    // copy & paste & collapse & expand
    //////////////////////////////////////
    
    public NodeID[] copy(final WorkflowManager sourceManager, 
            final NodeID[] nodeIDs) {
        HashSet<NodeID> idsHashed = new HashSet<NodeID>(Arrays.asList(nodeIDs));
        if (idsHashed.size() != nodeIDs.length) {
            throw new IllegalArgumentException(
                    "argument list contains duplicates");
        }
        Map<Integer, NodeContainerPersistor> loaderMap =
            new TreeMap<Integer, NodeContainerPersistor>();
        Set<ConnectionContainerTemplate> connTemplates =
            new HashSet<ConnectionContainerTemplate>();
        synchronized (sourceManager.m_workflowMutex) {
            for (int i = 0; i < nodeIDs.length; i++) {
                // throws exception if not present in workflow
                NodeContainer cont = sourceManager.getNodeContainer(nodeIDs[i]);
                loaderMap.put(cont.getID().getIndex(), 
                        cont.getCopyPersistor(m_globalTableRepository, false));
                for (ConnectionContainer out 
                        : sourceManager.m_workflow.getConnectionsBySource(nodeIDs[i])) {
                    if (idsHashed.contains(out.getDest())) {
                        connTemplates.add(new ConnectionContainerTemplate(out));
                    }
                }
            }
        }
        synchronized (m_workflowMutex) {
            Map<Integer, NodeID> resultIDs = loadNodesAndConnections(
                    loaderMap, connTemplates, new LoadResult());
            Map<NodeID, NodeContainerPersistor> newLoaderMap =
                new TreeMap<NodeID, NodeContainerPersistor>();
            NodeID[] result = new NodeID[nodeIDs.length];
            for (int i = 0; i < nodeIDs.length; i++) {
                int oldSuffix = nodeIDs[i].getIndex();
                result[i] = resultIDs.get(oldSuffix);
                newLoaderMap.put(result[i], loaderMap.get(oldSuffix));
                assert result[i] != null 
                    : "Deficient map, no entry for suffix " + oldSuffix;
            }
            try {
                postLoad(newLoaderMap, 
                        new HashMap<Integer, BufferedDataTable>(), 
                        new ExecutionMonitor());
            } catch (CanceledExecutionException e) {
                LOGGER.fatal("Unexpected exception", e);
            }
            return result;
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected NodeContainerPersistor getCopyPersistor(
            final HashMap<Integer, ContainerTable> tableRep, 
            final boolean preserveDeletableFlags) {
        return new CopyWorkflowPersistor(
                this, tableRep, preserveDeletableFlags);
    }
    
    ///////////////////////////////
    ///////// LOAD & SAVE /////////
    ///////////////////////////////

    /** Workflow version. */
    static final String CFG_VERSION = "version";

    public static WorkflowLoadResult loadProject(File directory,
            final ExecutionMonitor exec) throws IOException,
            InvalidSettingsException, CanceledExecutionException {
        return ROOT.load(directory, exec);
    }

    public WorkflowLoadResult load(File directory, final ExecutionMonitor exec) 
        throws IOException, InvalidSettingsException, 
        CanceledExecutionException {
        if (directory == null || exec == null) {
            throw new NullPointerException("Arguments must not be null.");
        }
        if (!directory.isDirectory() || !directory.canRead()) {
            throw new IOException("Can't read directory " + directory);
        }
        exec.setMessage("Loading workflow structure from \""
                + directory.getAbsolutePath() + "\"");
        ReferencedFile workflowDirRef = new ReferencedFile(directory);
        ReferencedFile workflowknimeRef =
            new ReferencedFile(workflowDirRef, WorkflowPersistor.WORKFLOW_FILE);
        File workflowknime = workflowknimeRef.getFile();
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
        WorkflowPersistorVersion1xx persistor;
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
        Map<Integer, BufferedDataTable> tblRep =
            new HashMap<Integer, BufferedDataTable>();
        WorkflowLoadResult result = new WorkflowLoadResult();
        result.addError(persistor.preLoadNodeContainer(
                workflowknimeRef, settings));
        ExecutionMonitor contentExec = exec.createSubProgress(0.1);
        ExecutionMonitor loadExec = exec.createSubProgress(0.9);
        exec.setMessage("Loading content");
        result.addError(persistor.loadNodeContainer(tblRep, contentExec));
        contentExec.setProgress(1.0);
        WorkflowManager manager;
        exec.setMessage("Creating workflow instance");
        boolean fixDataLoadProblems = false;
        synchronized (m_workflowMutex) {
            NodeID newID = createUniqueID();
            manager = createSubWorkflow(persistor, newID);
            addNodeContainer(manager, false);
            synchronized (manager.m_workflowMutex) {
                result.addError(manager.loadContent(
                        persistor, tblRep, null, loadExec));
            }
            // if all errors during the load process are related to data loading
            // it might be that the flow is ex/imported without data;
            // check for it and silently overwrite the workflow
            if (result.hasErrors() && (!result.hasErrorDuringNonDataLoad()
                    && !persistor.mustWarnOnDataLoadError())) {
                LOGGER.debug("Workflow was apparently ex/imported without "
                        + "data, silently fixing states and writing changes");
                try {
                    manager.save(directory, new ExecutionMonitor(), true);
                    fixDataLoadProblems = true;
                } catch (Throwable t) {
                    LOGGER.warn("Failed in an attempt to write workflow to "
                            + "file (workflow was ex/imported without data; "
                            + "could not write the \"corrected\" flow.)", t);
                }
            }
        }
        exec.setProgress(1.0);
        result.setWorkflowManager(manager);
        String message;
        if (result.hasErrors() && (result.hasErrorDuringNonDataLoad()
                || persistor.mustWarnOnDataLoadError())) {
            message = "Loaded workflow from \"" + directory.getAbsolutePath()
                + "\" with errors";
            LOGGER.debug(result.getErrors());
            result.setGUIMustReportError(true);
        } else if (result.hasErrors() && !result.hasErrorDuringNonDataLoad()) {
            message = "Loaded workflow from \"" + directory.getAbsolutePath()
                + "\" with errors during data load. ";
            if (fixDataLoadProblems) {
                message += "Problems were fixed and (silently) saved.";
            } else {
                message += "Problems were fixed but could not be saved.";
            }
        } else {
            message = "Successfully loaded workflow from \""
                + directory.getAbsolutePath() + "\"";
        }
        LOGGER.debug(message);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    LoadResult loadContent(final NodeContainerPersistor nodePersistor,
            final Map<Integer, BufferedDataTable> tblRep,
            final ScopeObjectStack ignoredStack, final ExecutionMonitor exec)
        throws CanceledExecutionException {
        if (!(nodePersistor instanceof WorkflowPersistor)) {
            throw new IllegalStateException("Expected "
                    + WorkflowPersistor.class.getSimpleName()
                    + " persistor object, got "
                    + nodePersistor.getClass().getSimpleName());
        }
        WorkflowPersistor persistor = (WorkflowPersistor)nodePersistor;
        LoadResult loadResult = new LoadResult();
        Map<NodeID, NodeContainerPersistor> persistorMap =
            new HashMap<NodeID, NodeContainerPersistor>();
        Map<Integer, NodeContainerPersistor> nodeLoaderMap =
            persistor.getNodeLoaderMap();
        m_loadVersion = persistor.getLoadVersion();
        exec.setMessage("node & connection information");
        Map<Integer, NodeID> translationMap = 
            loadNodesAndConnections(nodeLoaderMap,
                    persistor.getConnectionSet(), loadResult);
        for (Map.Entry<Integer, NodeID> e : translationMap.entrySet()) {
            NodeID id = e.getValue();
            NodeContainerPersistor p = nodeLoaderMap.get(e.getKey());
            assert p != null : "Deficient translation map";
            persistorMap.put(id, p);
        }

        m_inPortsBarUIInfo = persistor.getInPortsBarUIInfo();
        m_outPortsBarUIInfo = persistor.getOutPortsBarUIInfo();
        LoadResult postLoadResult = postLoad(persistorMap, tblRep, exec);
        if (postLoadResult.hasErrors()) {
            loadResult.addError(postLoadResult);
        }
        // set dirty if this wm should be reset (for instance when the state
        // of the workflow can't be properly read from the workflow.knime)
        if (persistor.needsResetAfterLoad() || persistor.isDirtyAfterLoad()) {
            setDirty();
        }
        return loadResult;
    }
    
    private LoadResult postLoad(
            final Map<NodeID, NodeContainerPersistor> persistorMap, 
            final Map<Integer, BufferedDataTable> tblRep,
            final ExecutionMonitor exec) 
        throws CanceledExecutionException {
        LoadResult loadResult = new LoadResult();
        // linked set because we need reverse order later on
        Collection<NodeID> failedNodes = new LinkedHashSet<NodeID>();
        Set<NodeID> needConfigurationNodes = new HashSet<NodeID>();
        for (NodeID bfsID : m_workflow.createBreadthFirstSortedList(
                        persistorMap.keySet(), true).keySet()) {
            NodeContainer cont = getNodeContainer(bfsID);
            boolean needsReset;
            switch (cont.getState()) {
            case IDLE:
            case UNCONFIGURED_MARKEDFOREXEC:
                needsReset = false;
                break;
            default:
                // we reset everything which is not fully connected
                needsReset = !isFullyConnected(bfsID);
            break;
            }
            NodeOutPort[] predPorts = assemblePredecessorOutPorts(bfsID);
            final int predCount = predPorts.length;
            PortObject[] portObjects = new PortObject[predCount];
            ScopeObjectStack[] predStacks = new ScopeObjectStack[predCount];
            for (int i = 0; i < predCount; i++) {
                NodeOutPort p = predPorts[i];
                if (cont instanceof SingleNodeContainer && p != null) {
                    SingleNodeContainer snc = (SingleNodeContainer)cont;
                    snc.setInHiLiteHandler(i, p.getHiLiteHandler());
                }
                if (p != null) {
                    predStacks[i] = p.getScopeContextStackContainer();
                    portObjects[i] = p.getPortObject();
                }
            }
            ScopeObjectStack inStack;
            try {
                inStack = new ScopeObjectStack(cont.getID(), predStacks);
            } catch (IllegalContextStackObjectException ex) {
                loadResult.addError("Errors creating scope object stack for " 
                        + "node \"" + cont.getNameWithID() + "\", (resetting "
                        + "scope variables): " + ex.getMessage());
                needsReset = true;
                inStack = new ScopeObjectStack(cont.getID());
            }
            NodeContainerPersistor containerPersistor = persistorMap.get(bfsID);
            exec.setMessage(cont.getNameWithID());
            // two steps below: loadNodeContainer and loadContent
            ExecutionMonitor sub1 =
                exec.createSubProgress(1.0 / (2 * m_workflow.getNrNodes()));
            ExecutionMonitor sub2 =
                exec.createSubProgress(1.0 / (2 * m_workflow.getNrNodes()));
            LoadResult subResult = new LoadResult();
            try {
                subResult.addError(
                        containerPersistor.loadNodeContainer(tblRep, sub1));
            } catch (CanceledExecutionException e) {
                throw e;
            } catch (Exception e) {
                if (!(e instanceof InvalidSettingsException)
                        && !(e instanceof IOException)) {
                    LOGGER.error("Caught unexpected \""
                            + e.getClass().getSimpleName()
                            + "\" during node loading", e);
                }
                loadResult.addError("Errors reading node \""
                        + cont.getNameWithID() + "\", skipping it: "
                        + e.getMessage());
                needsReset = true;
            }
            sub1.setProgress(1.0);
            subResult.addError(cont.loadContent(
                    containerPersistor, tblRep, inStack, sub2));
            sub2.setProgress(1.0);
            if (containerPersistor.isDirtyAfterLoad()) {
                cont.setDirty();
            }
            boolean hasPredecessorFailed = false;
            for (ConnectionContainer cc 
                    : m_workflow.getConnectionsByDest(bfsID)) {
                if (failedNodes.contains(cc.getSource())) {
                    hasPredecessorFailed = true;
                }
            }
            needsReset |= containerPersistor.needsResetAfterLoad();
            needsReset |= hasPredecessorFailed;
            boolean isExecuted = cont.getState().equals(State.EXECUTED);
            // if node is executed and some input data is missing we need
            // to reset that node as there is obviously a conflict (e.g.
            // predecessors has been loaded as IDLE
            if (!needsReset && isExecuted 
                    && Arrays.asList(portObjects).contains(null)) {
                needsReset = true;
                subResult.addError("Predecessor ports have no data", true);
            }
            if (needsReset) {
                failedNodes.add(bfsID);
            }
            if (!hasPredecessorFailed && needsReset) {
                needConfigurationNodes.add(bfsID);
            }
            if (subResult.hasErrors()) {
                loadResult.addError("Errors reading node \""
                        + cont.getNameWithID() + "\":", subResult);
            }
        }
        // switching to list interface here in order to reverse the ordering
        failedNodes = new ArrayList<NodeID>(failedNodes);
        Collections.reverse((List<NodeID>)failedNodes);
        for (NodeID failed : failedNodes) {
            NodeContainer nc = getNodeContainer(failed);
            if (nc instanceof SingleNodeContainer) {
                SingleNodeContainer snc = (SingleNodeContainer)nc;
                if (snc.isResetable()) {
                    invokeResetOnNode(nc.getID());
                }
            } else {
                assert nc instanceof WorkflowManager;
                WorkflowManager wfm = (WorkflowManager)nc;
                if (wfm.isResetable()) {
                    wfm.resetAllNodesInWFM();
                }
            }
            // make sure it's marked as dirty (meta nodes may not be resetable
            // and hence don't get the dirty flag set)
            nc.setDirty();
        }
        for (NodeID id : needConfigurationNodes) {
            // don't push the configure outside the workflow manager here
            // (i.e. last flag in method call is false).
            // FIXME: Why? impossible now, argument was removed because its useless
            configureNodeAndSuccessors(id, true);
        }
        if (!sweep(false)) {
            loadResult.addError("Some node states were invalid");
        }
        return loadResult;
    }
    
    private Map<Integer, NodeID> loadNodesAndConnections(
            final Map<Integer, NodeContainerPersistor> loaderMap, 
            final Set<ConnectionContainerTemplate> connections,
            final LoadResult loadResult) {
        // id suffix are made unique by using the entries in this map
        Map<Integer, NodeID> translationMap = new HashMap<Integer, NodeID>();

        for (Map.Entry<Integer, NodeContainerPersistor> nodeEntry 
                : loaderMap.entrySet()) {
            int suffix = nodeEntry.getKey();
            NodeID subId = new NodeID(getID(), suffix);
            if (m_workflow.containsNodeKey(subId)) {
                subId = createUniqueID();
            }
            NodeContainerPersistor pers = nodeEntry.getValue();
            translationMap.put(suffix, subId);
            NodeContainer container = pers.getNodeContainer(this, subId);
            addNodeContainer(container, false);
            if (pers.isDirtyAfterLoad()) {
                container.setDirty();
            }
        }

        for (ConnectionContainerTemplate c : connections) {
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
            if (!canAddConnection(
                    source, c.getSourcePort(), dest, c.getDestPort())) {
                String warn = "Unable to insert connection \"" + c + "\"";
                LOGGER.warn(warn);
                loadResult.addError(warn);
                continue;
            }
            ConnectionContainer cc = addConnection(
                    source, c.getSourcePort(), dest, c.getDestPort(), false);
            cc.setUIInfo(c.getUiInfo());
            cc.setDeletable(c.isDeletable());
            assert cc.getType().equals(type);
        }
        return translationMap;
    }

    public void save(final File directory, final ExecutionMonitor exec,
            final boolean isSaveData)
        throws IOException, CanceledExecutionException {
        if (this == ROOT) {
            throw new IOException("Can't save root workflow");
        }
        // TODO GUI must only provide directory
        synchronized (m_workflowMutex) {
            ReferencedFile workflowDirRef = new ReferencedFile(directory);
            // if it's the location associated with the workflow we will
            // use same reference since a lock will be acquired
            if (workflowDirRef.equals(getNodeContainerDirectory())) {
                workflowDirRef = getNodeContainerDirectory();
            }
            workflowDirRef.lock();
            try {
                final boolean isWorkingDirectory = 
                    workflowDirRef.equals(getNodeContainerDirectory());
                WorkflowPersistorVersion200 persistor =
                    new WorkflowPersistorVersion200(null);
                if (m_loadVersion != null
                        && !m_loadVersion.equals(persistor.getSaveVersion())) {
                    LOGGER.info("Workflow was created with a previous version "
                            + "of KNIME (" + m_loadVersion + "), converting to "
                            + "current version " + persistor.getSaveVersion()
                            + ". This may take some time.");
                    setDirtyAll();
                    if (isWorkingDirectory) {
                        for (NodeContainer nc : m_workflow.getNodeValues()) {
                            ReferencedFile ncDir = 
                                nc.getNodeContainerDirectory();
                            if (ncDir != null && ncDir.getFile().exists()) {
                                FileUtil.deleteRecursively(ncDir.getFile());
                            }
                        }
                    }
                }
                if (isWorkingDirectory) {
                    for (ReferencedFile deletedNodeDir
                            : m_deletedNodesFileLocations) {
                        File f = deletedNodeDir.getFile();
                        if (f.exists() && !FileUtil.deleteRecursively(f)) {
                            LOGGER.warn("Deletion of obsolete node directory \""
                                    + f.getAbsolutePath() + "\" failed");
                        }
                    }
                    m_loadVersion = persistor.getSaveVersion();
                }
                new WorkflowPersistorVersion200(null).save(
                        this, workflowDirRef, exec, isSaveData);
            } finally {
                workflowDirRef.unlock();
            }
        }
    }

    /** Performs sanity check on workflow. This is necessary upon load.
     * @param propagate Whether to also reflect state changes in our parent
     * @return Whether everything was clean before (if false is returned,
     * something was wrong).
     */
    boolean sweep(final boolean propagate) {
        boolean wasClean = true;
        synchronized (m_workflowMutex) {
            for (NodeID id : m_workflow.createBreadthFirstSortedList(
                         m_workflow.getNodeIDs(), true).keySet()) {
                NodeContainer nc = getNodeContainer(id);
                Set<State> allowedStates =
                    new HashSet<State>(Arrays.asList(State.values()));
                NodeOutPort[] predPorts = assemblePredecessorOutPorts(id);
                for (NodeOutPort p : predPorts) {
                    if (p == null) {
                        allowedStates.retainAll(
                                Collections.singleton(State.IDLE));
                        continue;
                    }
                    switch (p.getNodeState()) {
                    case IDLE:
                        allowedStates.retainAll(Arrays.asList(
                                State.IDLE));
                        break;
                    case CONFIGURED:
                        allowedStates.retainAll(Arrays.asList(
                                State.CONFIGURED, State.IDLE));
                        break;
                    case UNCONFIGURED_MARKEDFOREXEC:
                        allowedStates.retainAll(Arrays.asList(State.IDLE,
                                State.UNCONFIGURED_MARKEDFOREXEC));
                        break;
                    case MARKEDFOREXEC:
                    case QUEUED:
                    case EXECUTING:
                        allowedStates.retainAll(Arrays.asList(State.IDLE,
                                State.UNCONFIGURED_MARKEDFOREXEC,
                                State.CONFIGURED, State.MARKEDFOREXEC));
                        break;
                    case EXECUTED:
                    }
                }
                if (!allowedStates.contains(nc.getState())) {
                    wasClean = false;
                    switch (nc.getState()) {
                    case EXECUTED:
                        resetSuccessors(nc.getID());
                        invokeResetOnNode(nc.getID());
                        break;
                    case EXECUTING:
                    case QUEUED:
                    case MARKEDFOREXEC:
                    case UNCONFIGURED_MARKEDFOREXEC:
                        assert nc instanceof SingleNodeContainer;
                        ((SingleNodeContainer)nc).cancelExecution();
                        break;
                    default:
                    }
                    if (!allowedStates.contains(State.CONFIGURED)) {
                        nc.setState(State.IDLE);
                    }
                }
                boolean hasData = true;
                // meta nodes don't need to provide output data and can still
                // be executed.
                if (nc instanceof SingleNodeContainer) {
                    for (int i = 0; i < nc.getNrOutPorts(); i++) {
                        NodeOutPort p = nc.getOutPort(i);
                        hasData &= p != null && p.getPortObject() != null
                            && p.getPortObjectSpec() != null;
                    }
                }
                if (!hasData && nc.getState().equals(State.EXECUTED)) {
                    wasClean = false;
                    resetSuccessors(nc.getID());
                    invokeResetOnNode(nc.getID());
                }
            }
        }
        checkForNodeStateChanges(propagate);
        return wasClean;
    }

    private void setDirtyAll() {
        setDirty();
        for (NodeContainer nc : m_workflow.getNodeValues()) {
            if (nc instanceof WorkflowManager) {
                ((WorkflowManager)nc).setDirtyAll();
            } else {
                nc.setDirty();
            }
        }
        for (ContainerTable t : m_globalTableRepository.values()) {
            t.ensureOpen();
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public void setDirty() {
        boolean sendEvent = !isDirty();
        super.setDirty();
        if (sendEvent) {
            notifyWorkflowListeners(new WorkflowEvent(
                    WorkflowEvent.Type.WORKFLOW_DIRTY, getID(), null, null));
        }
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
        if (!m_name.equals(name)) {
            setDirty();
            m_name = name;
        }
    }
    
    /** Renames the underlying workflow directory to the new name.
     * @param newName The name of the directory.
     * @return Whether that was successful.
     * @throws IllegalStateException If the worklow has not been saved yet 
     * (has no corresponding node directory). 
     */
    public boolean renameWorkflowDirectory(final String newName) {
        ReferencedFile file = getNodeContainerDirectory();
        if (file == null) {
            throw new IllegalStateException("Worklow has not yet been saved");
        }
        return file.rename(newName);
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
    public NodeView<NodeModel> getView(final int i) {
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

    /** {@inheritDoc} */
    @Override
    void cleanup() {
        synchronized (m_workflowMutex) {
            for (NodeContainer nc : m_workflow.getNodeValues()) {
                nc.cleanup();
            }
        }
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

    /** Set UI information for workflow's input ports
     * (typically aligned as a bar).
     * @param inPortsBarUIInfo The new UI info.
     */
    public void setInPortsBarUIInfo(final UIInformation inPortsBarUIInfo) {
        if (!ConvenienceMethods.areEqual(m_inPortsBarUIInfo, inPortsBarUIInfo)) {
            m_inPortsBarUIInfo = inPortsBarUIInfo;
            setDirty();
        }
    }

    /** Set UI information for workflow's output ports
     * (typically aligned as a bar).
     * @param outPortsBarUIInfo The new UI info.
     */
    public void setOutPortsBarUIInfo(final UIInformation outPortsBarUIInfo) {
        if (!ConvenienceMethods.areEqual(
                m_outPortsBarUIInfo, outPortsBarUIInfo)) {
            m_outPortsBarUIInfo = outPortsBarUIInfo;
            setDirty();
        }
    }

    /** Get UI information for workflow input ports.
     * @return the ui info or null if not set.
     * @see #setInPortsBarUIInfo(UIInformation)
     */
    public UIInformation getInPortsBarUIInfo() {
        return m_inPortsBarUIInfo;
    }

    /** Get UI information for workflow output ports.
     * @return the ui info or null if not set.
     * @see #setOutPortsBarUIInfo(UIInformation)
     */
    public UIInformation getOutPortsBarUIInfo() {
        return m_outPortsBarUIInfo;
    }
}
