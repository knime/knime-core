/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.EmptyNodeDialogPane;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.Node;
import org.knime.core.node.Node.LoopRole;
import org.knime.core.node.NodeCreationContext;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NodeView;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.exec.ThreadNodeExecutionJobManager;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.node.util.NodeExecutionJobManagerPool;
import org.knime.core.node.workflow.ConnectionContainer.ConnectionType;
import org.knime.core.node.workflow.FlowLoopContext.RestoredFlowLoopContext;
import org.knime.core.node.workflow.NodeContainer.NodeContainerSettings.SplitType;
import org.knime.core.node.workflow.NodeMessage.Type;
import org.knime.core.node.workflow.SingleNodeContainer.LoopStatus;
import org.knime.core.node.workflow.SingleNodeContainer.SingleNodeContainerSettings;
import org.knime.core.node.workflow.Workflow.NodeAndInports;
import org.knime.core.node.workflow.WorkflowPersistor.ConnectionContainerTemplate;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowPortTemplate;
import org.knime.core.node.workflow.execresult.NodeContainerExecutionResult;
import org.knime.core.node.workflow.execresult.NodeContainerExecutionStatus;
import org.knime.core.node.workflow.execresult.WorkflowExecutionResult;
import org.knime.core.node.workflow.virtual.ParallelizedChunkContent;
import org.knime.core.node.workflow.virtual.ParallelizedChunkContentMaster;
import org.knime.core.node.workflow.virtual.VirtualNodeInput;
import org.knime.core.node.workflow.virtual.VirtualPortObjectInNodeFactory;
import org.knime.core.node.workflow.virtual.VirtualPortObjectInNodeModel;
import org.knime.core.node.workflow.virtual.VirtualPortObjectOutNodeFactory;
import org.knime.core.util.FileUtil;
import org.knime.core.util.LockFailedException;
import org.knime.core.util.Pair;

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
public final class WorkflowManager extends NodeContainer implements NodeUIInformationListener {

    /** my logger. */
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(WorkflowManager.class);

    /** Name of this workflow (usually displayed at top of the node figure).
     * May be null to use name of workflow directory. */
    private String m_name;

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

    /** Executor for asynchronous invocation of checkForNodeStateChanges
     * in an unconnected parent.
     * If a checkForNodeStateChanges-Thread is already waiting, additional
     * ones will be discarded. */
    private static final Executor PARENT_NOTIFIER =
        new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(2), new ThreadFactory() {
                    /** {@inheritDoc} */
                    @Override
                    public Thread newThread(final Runnable r) {
                        Thread t = new Thread(r, "KNIME-WFM-Parent-Notifier");
                        return t;
                    }
                }, new ThreadPoolExecutor.DiscardPolicy());

    // Nodes and edges forming this workflow:
    private final Workflow m_workflow;

    // Ports of the workflow (empty if it is not a subworkflow):

    /** ports of this Metanode (both arrays can have 0 length!). */
    private final WorkflowInPort[] m_inPorts;
    private UIInformation m_inPortsBarUIInfo;
    private final WorkflowOutPort[] m_outPorts;
    private UIInformation m_outPortsBarUIInfo;

    /** Vector holding workflow specific variables. */
    private Vector<FlowVariable> m_workflowVariables;

    private final Vector<WorkflowAnnotation> m_annotations =
        new Vector<WorkflowAnnotation>();

    // Misc members:

    /** for internal usage, holding output table references. */
    private final HashMap<Integer, ContainerTable> m_globalTableRepository;

    /** Password store. This object is associated with each meta-node
     * (contained meta nodes have their own password store). */
    private final CredentialsStore m_credentialsStore;

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
        m_credentialsStore = new CredentialsStore(this);
        // initialize listener list
        m_wfmListeners = new CopyOnWriteArrayList<WorkflowListener>();
        checkForNodeStateChanges(false); // get default state right
        // done.
        LOGGER.debug("Created subworkflow " + this.getID());
    }

    /** Constructor - create new workflow from persistor.
     */
    private WorkflowManager(final WorkflowManager parent, final NodeID id,
            final WorkflowPersistor persistor) {
        super(parent, id, persistor.getMetaPersistor());
        ReferencedFile ncDir = super.getNodeContainerDirectory();
        if (ncDir != null) {
            if (!ncDir.fileLockRootForVM()) {
                throw new IllegalStateException("Root directory to workflow \""
                        + ncDir + "\" can't be locked although it should have "
                        + "been locked by the load routines");
            }
        }
        m_workflow = new Workflow(id);
        m_name = persistor.getName();
        m_loadVersion = persistor.getLoadVersionString();
        m_workflowVariables =
            new Vector<FlowVariable>(persistor.getWorkflowVariables());
        m_credentialsStore =
            new CredentialsStore(this, persistor.getCredentials());
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

    /**
     * @return workflow
     */
    Workflow getWorkflow() {
        return m_workflow;
    }

    ///////////////////////////////////////
    // Node / Project / Metanode operations
    ///////////////////////////////////////

    /** Create new project - which is the same as creating a new subworkflow
     * at this level with no in- or outports.
     * @param name The name of the workflow (null value is ok)
     * @return newly created workflow
     */
    public WorkflowManager createAndAddProject(final String name) {
        WorkflowManager wfm = createAndAddSubWorkflow(new PortType[0],
                new PortType[0], name);
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
        return internalAddNewNode(factory, null);
    }

    /**
     * @param factory
     * @param context
     * @return the node id of the created node.
     */
    public NodeID addNodeAndApplyContext(final NodeFactory<?> factory,
            final NodeCreationContext context) {
        return internalAddNewNode(factory, context);
    }

    private NodeID internalAddNewNode(final NodeFactory<?> factory,
            final NodeCreationContext context) {
        NodeID newID;
        synchronized (m_workflowMutex) {
            // TODO synchronize to avoid messing with running workflows!
            assert factory != null;
            // insert node
            newID = createUniqueID();
            SingleNodeContainer container = new SingleNodeContainer(this,
               new Node((NodeFactory<NodeModel>)factory, context), newID);
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
     * not be removed at this time. If the node does not exist, this method
     * returns without exception.
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
            checkForNodeStateChanges(true);
        }
        setDirty();
        notifyWorkflowListeners(
                new WorkflowEvent(WorkflowEvent.Type.NODE_REMOVED,
                getID(), nc, null));
    }

    /** Creates new meta node. We will automatically find the next available
     * free index for the new node within this workflow.
     * @param inPorts types of external inputs (going into this workflow)
     * @param outPorts types of external outputs (exiting this workflow)
     * @param name Name of the workflow (null values will be handled)
     * @return newly created <code>WorkflowManager</code>
     */
    public WorkflowManager createAndAddSubWorkflow(final PortType[] inPorts,
            final PortType[] outPorts, final String name) {
        if (this == ROOT && (inPorts.length != 0 || outPorts.length != 0)) {
            throw new IllegalStateException("Can't create sub workflow on "
                + "root workflow manager, use createProject() instead");
        }
        NodeID newID;
        WorkflowManager wfm;
        synchronized (m_workflowMutex) {
            newID = createUniqueID();
            wfm = new WorkflowManager(this, newID, inPorts, outPorts);
            if (name != null) {
                wfm.m_name = name;
            }
            addNodeContainer(wfm, true);
            LOGGER.debug("Added new subworkflow " + newID);
        }
        setDirty();
        return wfm;
    }

    /** Creates new meta node from a persistor instance.
     * @param persistor to read from
     * @param newID new id to be used
     * @return newly created <code>WorkflowManager</code>
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
                    + "workflow, use createAndAddProject() first");
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
        return addConnection(source, sourcePort, dest, destPort, false);
    }

    /** Add new connection - throw Exception if the same connection
     * already exists.
     *
     * @param source node id
     * @param sourcePort port index at source node
     * @param dest destination node id
     * @param destPort port index at destination node
     * @param currentlyLoadingFlow True if the flow is currently loading
     *        its content, it will then skip the configuration of the
     *        destination node and allow node insertion in case the dest
     *        node is currently (remotely!) executing.
     * @return newly created Connection object
     * @throws IllegalArgumentException if connection already exists
     */
    private ConnectionContainer addConnection(final NodeID source,
            final int sourcePort, final NodeID dest,
            final int destPort, final boolean currentlyLoadingFlow) {
        assert source != null;
        assert dest != null;
        assert sourcePort >= 0;
        assert destPort >= 0;
        ConnectionContainer newConn = null;
        ConnectionType newConnType = null;
        NodeContainer sourceNC;
        NodeContainer destNC;
        synchronized (m_workflowMutex) {
            if (!canAddConnection(
                    source, sourcePort, dest, destPort, currentlyLoadingFlow)) {
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
        if (!currentlyLoadingFlow) { // user adds connnection -> configure
            if (newConn.getType().isLeavingWorkflow()) {
                assert !m_workflow.containsNodeKey(dest);
                // if the destination was the WFM itself, only configure its
                // successors one layer up!
                getParent().configureNodeAndSuccessors(dest, false);
                checkForNodeStateChanges(true);
            } else if (destNC instanceof WorkflowManager) {
                // connection enters a meta node
                // (can't have optional ins -- no reset required)
                WorkflowManager destWFM = (WorkflowManager)destNC;
                destWFM.configureNodesConnectedToPortInWFM(destPort);
                Set<Integer> outPorts =
                    destWFM.getWorkflow().connectedOutPorts(destPort);
                configureNodeAndPortSuccessors(dest, outPorts,
                        /* do not configure dest itself */false, true);
            } else {
                assert m_workflow.containsNodeKey(dest);
                // ...make sure the destination node is configured again (and
                // all of its successors if needed)
                // (reset required if optional input is connected)
                resetAndConfigureNode(dest);
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
        return canAddConnection(source, sourcePort, dest, destPort, false);
    }

    /** see {@link #canAddConnection(NodeID, int, NodeID, int)}. If the flag
     * is set it will skip the check whether the destination node is
     * executing (the node may be executing remotely during load)
     */
    private boolean canAddConnection(final NodeID source,
            final int sourcePort, final NodeID dest,
            final int destPort, final boolean currentlyLoadingFlow) {
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
        if (destNode != null) { // ordinary node
            if (destNode.getNrInPorts() <= destPort) {
                return false;  // dest Node index exists
            }
            // omit execution checks during loading (dest node may
            // be executing remotely -- SGE execution)
            if (!currentlyLoadingFlow) {
                // destination node may have optional inputs
                if (hasSuccessorInProgress(dest)) {
                    return false;
                }
                if (destNode.getState().executionInProgress()) {
                    return false;
                }
            }
        } else { // leaving workflow connection
            assert dest.equals(getID());
            if (this.getNrOutPorts() <= destPort) {
                return false;  // WFM outport index exists
            }
            // node may be executing during load (remote cluster execution)
            if (!currentlyLoadingFlow) {
                // nodes with optional inputs may have executing successors
                // note it is ok if the WFM itself is executing...
                if (getParent().hasSuccessorInProgress(getID())) {
                    return false;
                }
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
        /* ports can be connected in two cases (one exception below):
         * - the destination type is a super type or the same type
         *   of the source port (usual case) or
         * - if the source port is a super type of the destination port,
         *   for instance a reader node that reads a general PMML objects,
         *   validity is checked using the runtime class of the actual
         *   port object then
         * if one port is a BDT and the other is not, no connection is allowed
         * */
        Class<? extends PortObject> sourceCl = sourceType.getPortObjectClass();
        Class<? extends PortObject> destCl = destType.getPortObjectClass();
        if (BufferedDataTable.class.equals(sourceCl)
                && !BufferedDataTable.class.equals(destCl)) {
            return false;
        } else if (BufferedDataTable.class.equals(destCl)
                && !BufferedDataTable.class.equals(sourceCl)) {
            return false;
        } else if (!destCl.isAssignableFrom(sourceCl)
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
        if (cc == null || !cc.isDeletable()) {
            return false;
        }
        NodeID destID = cc.getDest();
        NodeID sourceID = cc.getSource();
        // make sure both nodes (well, their connection lists) exist
        if (m_workflow.getConnectionsByDest(destID) == null) {
            return false;
        }
        if (m_workflow.getConnectionsBySource(sourceID) == null) {
            return false;
        }
        // make sure connection between those two nodes exists
        if (!m_workflow.getConnectionsByDest(destID).contains(cc)) {
            return false;
        }
        if (!m_workflow.getConnectionsBySource(sourceID).contains(cc)) {
            return false;
        }
        if (destID.equals(getID())) { // wfm out connection
            // note it is ok if the WFM itself is executing...
            if (getParent().hasSuccessorInProgress(getID())) {
                return false;
            }
        } else {
            if (hasSuccessorInProgress(destID)) {
                return false;
            }
            if (m_workflow.getNode(destID).getState().executionInProgress()) {
                return false;
            }
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
                "Connection did not exist (it did exist as outcoming conn.)!");
            }
            // handle special cases with port reference chains (WFM border
            // crossing connections:
            int destPort = cc.getDestPort();
            if ((source.equals(getID()))
                && (dest.equals(getID()))) {
                // connection goes directly from workflow in to workflow outport
                assert cc.getType() == ConnectionType.WFMTHROUGH;
                getOutPort(destPort).setUnderlyingPort(null);
            } else if ((!dest.equals(getID()))
                    && (destNC instanceof WorkflowManager)) {
                // we are feeding data into a subworkflow
                WorkflowInPort wfmIPort
                        = ((WorkflowManager)destNC).getInPort(destPort);
                wfmIPort.setUnderlyingPort(null);
            } else if (dest.equals(getID())) {
                // we are feeding data out of the subworkflow
                assert cc.getType() == ConnectionType.WFMOUT;
                getOutPort(destPort).setUnderlyingPort(null);
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
            } else if (destNC instanceof WorkflowManager) {
                // connection entered a meta node
                WorkflowManager destWFM = (WorkflowManager)destNC;
                destWFM.resetNodesInWFMConnectedToInPorts(
                        Collections.singleton(destPort));
                destWFM.configureNodesConnectedToPortInWFM(destPort);
                Set<Integer> outPorts =
                    destWFM.getWorkflow().connectedOutPorts(destPort);
                for (int i : outPorts) {
                    resetSuccessors(dest, i);
                }
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

    /** Get all outgoing connections for a node.
     * @param id The requested node
     * @return All current outgoing connections in a new set.
     * @throws IllegalArgumentException If the node is unknown or null.
     */
    public Set<ConnectionContainer> getOutgoingConnectionsFor(final NodeID id) {
        synchronized (m_workflowMutex) {
            getNodeContainer(id); // for exception handling
            return new LinkedHashSet<ConnectionContainer>(
                    m_workflow.getConnectionsBySource(id));
        }
    }

    /**
     * Returns the incoming connection of the node with the passed node id at
     * the specified port.
     * @param id id of the node of interest
     * @param portIdx port index
     * @return incoming connection at that port of the given node or null if it
     *     doesn't exist
     */
    public ConnectionContainer getIncomingConnectionFor(final NodeID id,
            final int portIdx) {
        synchronized (m_workflowMutex) {
            Set<ConnectionContainer>inConns =
                m_workflow.getConnectionsByDest(id);
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

    /** Get all incoming connections for a node.
     * @param id The requested node
     * @return All current incoming connections in a new set.
     * @throws IllegalArgumentException If the node is unknown or null.
     */
    public Set<ConnectionContainer> getIncomingConnectionsFor(final NodeID id) {
        synchronized (m_workflowMutex) {
            getNodeContainer(id); // for exception handling
            return new LinkedHashSet<ConnectionContainer>(
                    m_workflow.getConnectionsByDest(id));
        }
    }

    /**
     * Gets a connection by id.
     * @param id of the connection to return
     * @return the connection with the specified id
     */
    public ConnectionContainer getConnection(final ConnectionID id) {
        synchronized (m_workflowMutex) {
            return getIncomingConnectionFor(id.getDestinationNode(),
                    id.getDestinationPort());
        }
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
        synchronized (m_workflowMutex) {
            NodeContainer nc = getNodeContainer(id);
            if (!nc.getState().executionInProgress()
                    && !hasSuccessorInProgress(id)) {
                resetSuccessors(id);
                if (nc.isResetable()) {
                    if (nc instanceof SingleNodeContainer) {
                        ((SingleNodeContainer)nc).reset();
                    } else {
                        ((WorkflowManager)nc).resetAllNodesInWFM();
                    }
                }
                nc.loadSettings(settings);
                // bug fix 2593: can't simply call configureNodeAndSuccessor
                // with meta node as argument: will miss contained source nodes
                if (nc instanceof SingleNodeContainer) {
                    configureNodeAndSuccessors(id, true);
                } else {
                    ((WorkflowManager)nc).reconfigureAllNodesOnlyInThisWFM();
                    configureNodeAndSuccessors(id, false);
                }
            } else {
                throw new IllegalStateException(
                        "Cannot load settings into node; it is executing or "
                        + "has executing successors");
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
     * will be queued automatically (usually triggered by doAfterExecution
     * of the predecessors).
     * This does NOT affect any predecessors of this workflow!
     *
     * @param flag mark nodes if true, otherwise try to erase marks
     */
    private void markForExecutionAllNodesInWorkflow(final boolean flag) {
        synchronized (m_workflowMutex) {
            boolean changed = false; // will be true in case of state changes
            for (NodeContainer nc : m_workflow.getNodeValues()) {
                if (nc instanceof SingleNodeContainer) {
                    SingleNodeContainer snc = (SingleNodeContainer)nc;
                    if (flag) {
                        switch (nc.getState()) {
                        case CONFIGURED:
                        case IDLE:
                            changed = true;
                            snc.markForExecution(true);
                            break;
                        default:
                            // either executed or to-be-executed
                        }

                    } else {
                        switch (nc.getState()) {
                        case MARKEDFOREXEC:
                        case UNCONFIGURED_MARKEDFOREXEC:
                            changed = true;
                            snc.markForExecution(false);
                            break;
                        default:
                            // ignore all other
                        }
                    }
                } else {
                    WorkflowManager wfm = ((WorkflowManager)nc);
                    // does not need to set "changed" flag here as this child
                    // will propagate state changes by calling
                    // call checkForNodeStateChanges (possibly too often)
                    wfm.markForExecutionAllNodesInWorkflow(flag);
                }
            }
            if (changed) {
                checkForNodeStateChanges(true);
            }
        }
    }

    /**
     * Mark all nodes in this workflow that are connected to the given
     * inports.
     * Note that this routine will NOT trigger any actions connected to
     * possible outports of this WFM.
     *
     * @param inPorts set of port indices of the WFM.
     * @param markExecutedNodes if true also (re)mark executed nodes.
     */
    void markForExecutionNodesInWFMConnectedToInPorts(
            final Set<Integer> inPorts,
            final boolean markExecutedNodes) {
        synchronized (m_workflowMutex) {
            boolean changed = false; // will be true in case of state changes
            ArrayList<NodeAndInports> nodes
                          = m_workflow.findAllConnectedNodes(inPorts);
            for (NodeAndInports nai : nodes) {
                NodeContainer nc = m_workflow.getNode(nai.getID());
                if (nc instanceof SingleNodeContainer) {
                    SingleNodeContainer snc = (SingleNodeContainer)nc;
                    switch (nc.getState()) {
                    case CONFIGURED:
                    case IDLE:
                        changed = true;
                        snc.markForExecution(true);
                        break;
                    case EXECUTED:
                        if (markExecutedNodes) {
                            changed = true;
                            snc.markForReExecutionInLoop();
                            break;
                        }
                    default:
                        // either executed or to-be-executed
                    }
                } else {
                    WorkflowManager wfm = ((WorkflowManager)nc);
                    assert nc instanceof WorkflowManager;
                    // does not need to set "changed" flag here as child
                    // will propagate state changes by calling
                    // call checkForNodeStateChanges (likely too often)
                    wfm.markForExecutionNodesInWFMConnectedToInPorts(
                                                       nai.getInports(),
                                                       markExecutedNodes);
                }
            }
            if (changed) {
                checkForNodeStateChanges(true);
            }
        }
    }

    /** mark all nodes connected to the specified outport(!)
     * in this workflow (and all subworkflows!) for execution
     * (if they are not executed already). Also go back up to the
     * predecessors of this wfm if there are connections of interest.
     *
     * @param outPortIndex indicates which outport is affected
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
                if (thisID.equals(getID())) {
                    continue; // skip WFM
                }
                NodeContainer thisNode = m_workflow.getNode(thisID);
                if (thisNode instanceof SingleNodeContainer) {
                    SingleNodeContainer snc = (SingleNodeContainer)thisNode;
                    switch (snc.getState()) {
                    case IDLE:
                    case CONFIGURED:
                        if (!markAndQueueNodeAndPredecessors(snc.getID(), -1)) {
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
                if (nc instanceof SingleNodeContainer) {
                    ((SingleNodeContainer)nc).markForExecution(false);
                } else {
                    ((WorkflowManager)nc).markForExecutionAllNodesInWorkflow(
                            false);
                }
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
            assert getID().equals(id);
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
     * Also re-configure not executed nodes the same way to make sure that
     * new workflow variables are spread accordingly.
     */
//    public void resetAll() {
//        synchronized (m_workflowMutex) {
//            for (NodeID id : m_workflow.getNodeIDs()) {
//                boolean hasNonParentPredecessors = false;
//                for (ConnectionContainer cc
//                        : m_workflow.getConnectionsByDest(id)) {
//                    if (!cc.getSource().equals(this.getID())) {
//                        hasNonParentPredecessors = true;
//                        break;
//                    }
//                }
//                if (!hasNonParentPredecessors) {
//                    if (getNodeContainer(id).isResetable()) {
//                        // reset nodes which are green - will configure
//                        // them afterwards anyway.
//                        resetAndConfigureNode(id);
//                    } else {
//                        // but make sure to re-configure yellow nodes so
//                        // that new variables are available in those
//                        // pipeline branches!
//                        configureNodeAndSuccessors(id, true);
//                    }
//                }
//            }
//        }
//    }

    /**
     * Re-configure all configured (NOT executed) nodes in this workflow
     * to make sure that new workflow variables are spread accordingly.
     * Note that this does NOT affect any successors of this workflow
     * manager but touches all nodes inside this wfm and its kids.
     */
    void reconfigureAllNodesOnlyInThisWFM() {
        synchronized (m_workflowMutex) {
            // do not worry about pipelines, just process all nodes "left
            // to right" and make sure we touch all of them (also yellow
            // nodes in a metanode that is connected to red nodes only)...
            for (NodeID sortedID : m_workflow.createBreadthFirstSortedList(
                     m_workflow.getNodeIDs(), true).keySet()) {
                NodeContainer nc = getNodeContainer(sortedID);
                if (nc instanceof SingleNodeContainer) {
                    // reconfigure yellow AND red nodes - it could be that
                    // the reason for the red state were the variables!
                    if (nc.getState().equals(State.CONFIGURED)
                            || nc.getState().equals(State.IDLE)) {
                        configureSingleNodeContainer(
                            (SingleNodeContainer)nc,
                            /* keepNodemessage=*/ false);
                    }
                } else {
                    assert nc instanceof WorkflowManager;
                    ((WorkflowManager)nc).reconfigureAllNodesOnlyInThisWFM();
                }
            }
            checkForNodeStateChanges(true);
        }
    }

    /**
     * Reset all executed nodes in this workflow to make sure that new
     * workflow variables are spread accordingly. If a node is already
     * reset (or we just reset it), also configure it.
     * Note that this does NOT affect any successors of this workflow
     * manager but touches all nodes inside this wfm and its kids.
     *
     * TODO Maybe redundant: call resetAllNodes... and then configure them
     *  (only called when WorkflowVariables are set)
     */
    void resetAndReconfigureAllNodesInWFM() {
        synchronized (m_workflowMutex) {
            // do not worry about pipelines, just process all nodes "left
            // to right" and make sure we touch all of them (also yellow/green
            // nodes in a metanode that is connected to red nodes only)...
            for (NodeID sortedID : m_workflow.createBreadthFirstSortedList(
                     m_workflow.getNodeIDs(), true).keySet()) {
                // TODO reset all nodes in reverse order first
                NodeContainer nc = getNodeContainer(sortedID);
                if (nc instanceof SingleNodeContainer) {
                    // (reset) and configure yellow AND red nodes - it could be
                    // that the reason for the red state were the variables!
                    if (nc.isResetable()) {
                        ((SingleNodeContainer)nc).reset();
                    }
                    if (nc.getState().equals(State.CONFIGURED)
                            || nc.getState().equals(State.IDLE)) {
                        // re-configure if node was yellow or we just reset it.
                        // note that there still may be metanodes
                        // connected to this one which contain green
                        // nodes! (hence the brute force left-to-right approach
                        configureSingleNodeContainer(
                                (SingleNodeContainer)nc,
                                /* keepNodemessage=*/ false);
                    }
                } else {
                    assert nc instanceof WorkflowManager;
                    ((WorkflowManager)nc).resetAndReconfigureAllNodesInWFM();
                }
            }
            checkForNodeStateChanges(true);
        }
    }

    /** Resets and freshly configures all nodes in this workflow.
     * @deprecated Use {@link #resetAndConfigureAll()} instead
     */
    @Deprecated
    public void resetAll() {
        resetAndConfigureAll();
    }

    /** Resets and freshly configures all nodes in this workflow. */
    public void resetAndConfigureAll() {
        // TODO this does not reset connected outports (which it should as this
        // is a public methods. (see resetAndReconfigureAllNodesInWFM)
        resetAndReconfigureAllNodesInWFM();
    }

    /**
     * Reset all nodes in this workflow.
     * Note that this routine will NOT trigger any resets connected to
     * possible outports of this WFM.
     */
    void resetAllNodesInWFM() {
        synchronized (m_workflowMutex) {
            if (!isResetable()) {
                // only attempt to do this if possible.
                return;
            }
            for (NodeID id : m_workflow.getNodeIDs()) {
                // we don't need to worry about the correct order since
                // we will reset everything in here anyway.
                NodeContainer nc = m_workflow.getNode(id);
                if (nc.isResetable()) {
                    if (nc instanceof SingleNodeContainer) {
                        ((SingleNodeContainer)nc).reset();
                    } else {
                        assert nc instanceof WorkflowManager;
                        ((WorkflowManager)nc).resetAllNodesInWFM();
                    }
                }
            }
            // TODO Michael: this can be replaced by checkForNodeState...
            //
            // don't let the WFM decide on the state himself - for example,
            // if there is only one WFMTHROUGH connection contained, it will
            // produce wrong states! Force it to be idle.
            setState(State.IDLE);
        }
    }

    /**
     * Reset all nodes in this workflow that are connected to the given
     * inports. The reset is performed in the correct order, that is last
     * nodes are reset first.
     * Note that this routine will NOT trigger any resets connected to
     * possible outports of this WFM.
     *
     * @param inPorts set of port indices of the WFM.
     */
    void resetNodesInWFMConnectedToInPorts(final Set<Integer> inPorts) {
        synchronized (m_workflowMutex) {
            if (!isResetable()) {
                // only attempt to do this if possible.
                return;
            }
            ArrayList<NodeAndInports> nodes
              = m_workflow.findAllConnectedNodes(inPorts);
            ListIterator<NodeAndInports> li = nodes.listIterator(nodes.size());
            while (li.hasPrevious()) {
                NodeAndInports nai = li.previous();
                NodeContainer nc = m_workflow.getNode(nai.getID());
                if (nc.isResetable()) {
                    if (nc instanceof SingleNodeContainer) {
                        ((SingleNodeContainer)nc).reset();
                    } else {
                        assert nc instanceof WorkflowManager;
                        ((WorkflowManager)nc)
                              .resetNodesInWFMConnectedToInPorts(
                                                             nai.getInports());
                    }
                }
            }
            // TODO Michael: this can be replaced by checkForNodeState...
            //
            // don't let the WFM decide on the state himself - for example,
            // if there is only one WFMTHROUGH connection contained, it will
            // produce wrong states! Force it to be idle.
            setState(State.IDLE);
        }
    }

    /**
     * Clean outports of nodes connected to set of input ports. Used while
     * restarting the loop, whereby the loop body is not to be reset (special
     * option in start nodes). Clearing is done in correct order: downstream
     * nodes first.
     * @param inPorts set of port indices of the WFM.
     */
    void cleanOutputPortsInWFMConnectedToInPorts(final Set<Integer> inPorts) {
        synchronized (m_workflowMutex) {
            ArrayList<NodeAndInports> nodes =
                m_workflow.findAllConnectedNodes(inPorts);
            ListIterator<NodeAndInports> li = nodes.listIterator(nodes.size());
            while (li.hasPrevious()) {
                NodeAndInports nai = li.previous();
                NodeContainer nc = m_workflow.getNode(nai.getID());
                if (nc.isResetable()) {
                    if (nc instanceof SingleNodeContainer) {
                        ((SingleNodeContainer)nc).cleanOutPorts();
                    } else {
                        assert nc instanceof WorkflowManager;
                        ((WorkflowManager)nc)
                            .cleanOutputPortsInWFMConnectedToInPorts(
                                nai.getInports());
                    }
                }
            }
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
                NodeContainer nc = getNodeContainer(id);
                if (nc instanceof SingleNodeContainer) {
                    markAndQueueNodeAndPredecessors(id, -1);
                } else if (nc.isLocalWFM()) {
                    // if the execute option on a meta node is selected, run
                    // all nodes in it, not just the ones that are connected
                    // to the outports
                    // this will also trigger an execute on predecessors
                    ((WorkflowManager)nc).executeAll();
                } else {
                    markAndQueueNodeAndPredecessors(id, -1);
                }
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
            NodeContainer nc = getNodeContainer(id);
            // first check some basic facts about this node:
            // 1) executed? - done (and happy)
            if (nc.getState().equals(State.EXECUTED)) {
                // everything fine: found "source" of chain executed
                // Note that we can not assume that an executing metanode
                // is also a good thing: the port this one is connected
                // to may still be idle! So we test for "executing" later
                // for SNC's only (step 3)
                return true;
            }
            // 2) its a to-be-locally-executed WFM:
            if (nc.isLocalWFM()) {
                // hand over control to the sub workflow (who will hand
                // back up to this level if there are (implicit or explicit)
                // through connections to follow:
                WorkflowManager wfm = (WorkflowManager)nc;
                return wfm.markForExecutionAllAffectedNodes(outPortIndex);
            }
            // 3) executing SingleNodeContainer? - done (and happy)
            if (nc.getState().executionInProgress()) {
                // everything fine: found "source" of chain in execution
                return true;
            }
            // 4) now we check if we are dealing with a source (there is no
            //   need to traverse further up then and we can cancel the
            //   operation if the source is in a non-executable condition.
            Set<ConnectionContainer> predConn =
                m_workflow.getConnectionsByDest(id);
            if (nc.getNrInPorts() == 0) {
                assert predConn.size() == 0;
                if (canExecuteNode(nc.getID())) {
                    nc.markForExecution(true);
                    assert nc.getState().equals(State.MARKEDFOREXEC)
                        : "NodeContainer " + nc + " in unexpected state:"
                        + nc.getState();
                    nc.queue(new PortObject[0]);
                    // we are now executing one of the sources of the chain
                    return true;
                } else {
                    // could not find executable predecessor!
                    return false;
                }
            }
            // 5) we fail on nodes which are not fully connected
            //    (whereby unconnected optional inputs are ok)
            NodeOutPort[] predPorts = assemblePredecessorOutPorts(id);
            for (int i = 0; i < predPorts.length; i++) {
                if (predPorts[i] == null
                        && !nc.getInPort(i).getPortType().isOptional()) {
                    return false;
                }
            }
            // 6) now let's see if we can mark the predecessors of this node
            //  (and this way trigger the backwards traversal)
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
            boolean canBeMarked = true;
            for (NodeOutPort portIt : predPorts) {
                if (portIt != null
                    && (!portIt.getNodeState().executionInProgress()
                        && portIt.getPortObject() == null)) {
                    canBeMarked = false;
                }
            }
            if (canBeMarked) {
                nc.markForExecution(true);
                queueIfQueuable(nc);
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
        if (nc.isLocalWFM()) {
            return false;
        }
        if (!isLocalWFM()) {
            switch (getState()) {
            case MARKEDFOREXEC:
            case UNCONFIGURED_MARKEDFOREXEC:
                return getParent().queueIfQueuable(this);
            default:
                return false;
            }
        }
        assert Thread.holdsLock(m_workflowMutex);
        switch (nc.getState()) {
            case UNCONFIGURED_MARKEDFOREXEC:
            case MARKEDFOREXEC:
                break;
            default:
                assert false : "Queuing of " + nc.getNameWithID()
                    + " not possible, node is " + nc.getState();
                return false;
        }
        NodeOutPort[] ports = assemblePredecessorOutPorts(nc.getID());
        PortObject[] inData = new PortObject[ports.length];
        boolean allDataAvailable = true;
        for (int i = 0; i < ports.length; i++) {
            if (ports[i] != null) {
                inData[i] = ports[i].getPortObject();
                allDataAvailable &= inData[i] != null;
            } else if (nc.getInPort(i).getPortType().isOptional()) {
                // unconnected optional input - ignore
            } else {
                allDataAvailable = false;
            }
        }
        if (allDataAvailable) {
            switch (nc.getState()) {
            case MARKEDFOREXEC:
                nc.queue(inData);
                return true;
            case UNCONFIGURED_MARKEDFOREXEC:
                disableNodeForExecution(nc.getID());
                checkForNodeStateChanges(true);
                return false;
            default:
                assert false : "Invalid state " + nc.getState()
                    + ", case should have handeled above";
            }
        }
        return false;
    }

    /* -------------- State changing actions and testers ----------- */

    /**
     * Callback from NodeContainer to request a safe transition into the
     * {@link NodeContainer.State#PREEXECUTE} state. This method is mostly
     * only called with {@link SingleNodeContainer} as argument but may also be
     * called with a remotely executed meta node.
     * @param nc node whose execution is about to start
     * @return whether there was an actual state transition, false if the
     *         execution was canceled (cancel checking to be done in
     *         synchronized block)
     */
    boolean doBeforePreExecution(final NodeContainer nc) {
        assert !nc.isLocalWFM() : "No execution of local meta nodes";
        synchronized (m_workflowMutex) {
            LOGGER.debug(nc.getNameWithID() + " doBeforePreExecution");
            if (nc.performStateTransitionPREEXECUTE()) {
                checkForNodeStateChanges(true);
                return true;
            }
            return false;
        }
    }

    /**
     * Callback from NodeContainer to request a safe transition into the
     * {@link NodeContainer.State#POSTEXECUTE} state. This method is mostly
     * only called with {@link SingleNodeContainer} as argument but may also be
     * called with a remotely executed meta node.
     * @param nc node whose execution is ending (and is now copying
     *   result data, e.g.)
     */
    void doBeforePostExecution(final NodeContainer nc) {
        assert !nc.isLocalWFM() : "No execution of local meta nodes";
        synchronized (m_workflowMutex) {
            LOGGER.debug(nc.getNameWithID() + " doBeforePostExecution");
            nc.performStateTransitionPOSTEXECUTE();
            checkForNodeStateChanges(true);
        }
    }

    /** Call-back from NodeContainer called before node is actually executed.
     * The argument node is in usually a {@link SingleNodeContainer}, although
     * it can also be a meta node (i.e. a <code>WorkflowManager</code>), which
     * is executed remotely (execution takes place as a single operation).
     *
     * @param nc node whose execution is about to start
     * @throws IllegalFlowObjectStackException If loop end nodes have
     * problems identifying their start node
     */
    void doBeforeExecution(final NodeContainer nc) {
        assert !nc.getID().equals(this.getID());
        assert !nc.isLocalWFM() : "No execution of local meta nodes";
        synchronized (m_workflowMutex) {
            LOGGER.debug(nc.getNameWithID() + " doBeforeExecution");
            // allow SNC to update states etc
            if (nc instanceof SingleNodeContainer) {
                SingleNodeContainer snc = (SingleNodeContainer)nc;
                if (LoopRole.END.equals(snc.getLoopRole())) {
                    // if this is an END to a loop, make sure it knows its head
                    FlowObjectStack flowObjectStack = snc.getFlowObjectStack();
                    FlowLoopContext slc =
                        flowObjectStack.peek(FlowLoopContext.class);
                    if (slc == null) {
                        LOGGER.debug("Incoming flow object stack for "
                                + snc.getNameWithID() + ":\n"
                                + flowObjectStack.toDeepString());
                        throw new IllegalFlowObjectStackException(
                                "Encountered loop-end without "
                                + "corresponding head!");
                    } else if (slc instanceof RestoredFlowLoopContext) {
                        throw new IllegalFlowObjectStackException(
                                "Can't continue loop as the workflow was "
                                + "restored with the loop being partially "
                                + "executed. Reset loop start and execute "
                                + "entire loop again.");
                    }
                    NodeContainer headNode = m_workflow.getNode(slc.getOwner());
                    if (headNode == null) {
                        throw new IllegalFlowObjectStackException(
                                "Loop start and end node must be in the same "
                                + "workflow");
                    }
                    snc.getNode().setLoopStartNode(
                            ((SingleNodeContainer)headNode).getNode());
                } else if (LoopRole.BEGIN.equals(snc.getLoopRole())) {
                    snc.getNode().getOutgoingFlowObjectStack().push(
                            new InnerFlowLoopContext());
//                    snc.getNode().getFlowObjectStack().push(
//                            new InnerFlowLoopContext());
                } else {
                    // or not if it's any other type of node
                    snc.getNode().setLoopStartNode(null);
                }
            }
            nc.performStateTransitionEXECUTING();
            checkForNodeStateChanges(true);
        }
    }

    /** Cleanup a node after execution. This will also permit the argument node
     * to change its state in {@link NodeContainer#
     * performStateTransitionEXECUTED(NodeContainerExecutionStatus)}.
     * This method also takes care of restarting loops, if there are any to be
     * continued.
     *
     * <p>As in {@link #doBeforeExecution(NodeContainer)} the argument node is
     * usually a {@link SingleNodeContainer} but can also be a remotely executed
     * <code>WorkflowManager</code>.
     *
     * @param nc node which just finished execution
     * @param status indicates if node execution was finished successfully
     *    (note that this does not imply State=EXECUTED e.g. for loop ends)
     */
    void doAfterExecution(final NodeContainer nc,
            final NodeContainerExecutionStatus status) {
        assert isLocalWFM() : "doAfterExecute not allowed for "
            + "remotely executing workflows";
        assert !nc.getID().equals(this.getID());
        boolean success = status.isSuccess();
        synchronized (m_workflowMutex) {
            String st = success ? " - success" : " - failure";
            LOGGER.debug(nc.getNameWithID() + " doAfterExecute" + st);
            if (!success) {
                // execution failed - clean up successors' execution-marks
                disableNodeForExecution(nc.getID());
            }
            // switch state from POSTEXECUTE to new state: EXECUTED/CONFIGURED
            // in case of success (w/ or w/out loop) or IDLE in case of error.
            nc.performStateTransitionEXECUTED(status);
            boolean canConfigureSuccessors = true;
            if (nc instanceof SingleNodeContainer) {
                SingleNodeContainer snc = (SingleNodeContainer)nc;
                // remember previous message in case loop restart fails...
                NodeMessage latestNodeMessage = snc.getNodeMessage();
                if (success) {
                    Node node = snc.getNode();
                    // process start of bundle of parallel chunks
                    if (node.getNodeModel()
                    		instanceof LoopStartParallelizeNode) {
                        try {
                            parallelizeLoop(nc.getID());
                        } catch (Exception e) {
                            latestNodeMessage = new NodeMessage(
                                    NodeMessage.Type.ERROR,
                                    "Parallel Branch Start Failure! ("
                                    + e.getMessage() +")");
                            success = false;
                        }
                    }
                    // process loop context for "real" nodes:
                    if (snc.getLoopRole().equals(LoopRole.BEGIN)) {
                        // if this was BEGIN, it's not anymore (until we do not
                        // restart it explicitly!)
                        node.setLoopEndNode(null);
                    }
                    if (node.getLoopContext() != null) {
                        assert snc.getLoopRole() == LoopRole.END;
                        // we are supposed to execute this loop again!
                        // first retrieve FlowLoopContext object
                        FlowLoopContext slc = node.getLoopContext();
                        // first check if the loop is properly configured:
                        if (m_workflow.getNode(slc.getOwner()) == null) {
                            // obviously not: origin of loop is not in this WFM!
                            // nothing else to do: NC stays configured
                            assert nc.getState()
                                        == NodeContainer.State.CONFIGURED;
                            // and choke
                            latestNodeMessage = new NodeMessage(
                                    NodeMessage.Type.ERROR,
                                    "Loop nodes are not in the same workflow!");
                            success = false;
                        } else {
                            // make sure the end of the loop is properly
                            // configured:
                            slc.setTailNode(nc.getID());
                            // and try to restart loop
                            try {
                                if (!snc.getNode().getPauseLoopExecution()) {
                                    restartLoop(slc);
                                } else {
                                    // do nothing - leave successors marked...
//                                    disableNodeForExecution(snc.getID());
                                    // and leave flag for now (will be reset
                                    // when execution is resumed).
//                                    snc.getNode().setPauseLoopExecution(false);
                                }
                            } catch (IllegalLoopException ile) {
                                latestNodeMessage = new NodeMessage(
                                        NodeMessage.Type.ERROR,
                                        ile.getMessage());
                                success = false;
                            }
                            // make sure we do not accidentally configure the
                            // remainder of this node since we are not yet done
                            // with the loop
                            canConfigureSuccessors = false;
                        }
                        if (!success) {
                            // make sure any marks are removed off (only for loop ends!)
                            disableNodeForExecution(snc.getID());
                            snc.getNode().clearLoopContext();
                        }
                    }
                }
                // not this is NOT the else of the if above - success can
                // be modified...
                if (!success) {
                    // clean up node interna and status (but keep org. message!)
                    // switch from IDLE to CONFIGURED if possible!
                    configureSingleNodeContainer(snc, /*keepNodeMessage=*/false);
                    snc.setNodeMessage(latestNodeMessage);
                }
            }
            // now handle non success for all types of nodes:
            if (!success) {
                // clean loops which were waiting for this one to be executed.
                for (FlowLoopContext flc : nc.getWaitingLoops()) {
                    disableNodeForExecution(flc.getTailNode());
                }
                nc.clearWaitingLoopList();
            }
            if (nc.getWaitingLoops().size() >= 1) {
                // looks as if some loops were waiting for this node to
                // finish! Let's try to restart them:
                for (FlowLoopContext slc : nc.getWaitingLoops()) {
                    try {
                        restartLoop(slc);
                    } catch (IllegalLoopException ile) {
                        // set error message in LoopEnd node not this one!
                        NodeMessage nm = new NodeMessage(
                                NodeMessage.Type.ERROR,
                                ile.getMessage());
                        getNodeContainer(slc.getTailNode()).setNodeMessage(nm);
                    }
                }
                nc.clearWaitingLoopList();
            }
            if (canConfigureSuccessors) {
                // may be SingleNodeContainer or WFM contained within this
                // one but then it can be treated like a SNC
                configureNodeAndSuccessors(nc.getID(), false);
            }
            checkForNodeStateChanges(true);
        }
    }

    /** Restart execution of a loop if possible. Can delay restart if
     * we are still waiting for some node in the loop body (or any
     * dangling loop branches) to finish execution
     *
     * @param sc FlowLoopContext of the actual loop
     */
    private void restartLoop(final FlowLoopContext slc)
    throws IllegalLoopException {
        NodeContainer tailNode = m_workflow.getNode(slc.getTailNode());
        NodeContainer headNode = m_workflow.getNode(slc.getOwner());
        if ((tailNode == null) || (headNode == null)) {
            throw new IllegalLoopException("Loop Nodes must both"
                    + " be in the same workflow!");
        }
        if (!(tailNode instanceof SingleNodeContainer)
                || !(headNode instanceof SingleNodeContainer)) {
            throw new IllegalLoopException("Loop Nodes must both"
                    + " be SingleNodeContainers!");
        }
        // (1) find all intermediate node, the loop's "body"
        ArrayList<NodeAndInports> loopBodyNodes
                                  = m_workflow.findAllNodesConnectedToLoopBody(
                                            headNode.getID(), tailNode.getID());
        // (2) check if any of those nodes are still waiting to be
        //     executed or currently executing
        for (NodeAndInports nai : loopBodyNodes) {
            NodeID id = nai.getID();
            NodeContainer currNode = m_workflow.getNode(id);
            if (currNode.getState().executionInProgress()) {
                // stop right here - loop can not yet be restarted!
                currNode.addWaitingLoop(slc);
                return;
            }
            if (this.canExecuteNode(id)) {
                // we missed some nodes during the initial marking - because
                // these are part of untouched branches which
                // were not selected to be executed initially.
                // Mark them now and make sure they are already executed
                // during the first iteration (later executions will
                // then execute them automatically).
                // FIXME: (Bug 2292)
                //  - when only 1 iteration is run, this node never executes
                if (currNode instanceof WorkflowManager) {
                    // FIXME: also here we need to execute...?
                } else {
                    assert currNode instanceof SingleNodeContainer;
                    this.markAndQueueNodeAndPredecessors(id, -1);
                    currNode.addWaitingLoop(slc);
                    return;
                }
            }
        }
        // (3) mark the origin of the loop to be executed again
        //     do this now so that we have an executing node in this WFM
        //     and an intermediate state does not suggest everything is done.
        //     (this used to happen before (9))
        // NOTE: if we ever queue nodes asynchronosly this might cause problems.
        SingleNodeContainer headSNC = ((SingleNodeContainer)headNode);
        assert headSNC.getLoopRole().equals(LoopRole.BEGIN);
        headSNC.markForReExecutionInLoop();
        // clean up all newly added objects on FlowVariable Stack
        // (otherwise we will push the same variables many times...
        // push ISLC back onto the stack is done in doBeforeExecute()!
        headSNC.getOutgoingFlowObjectStack().pop(InnerFlowLoopContext.class);
        // (4-7) reset/configure loop body - or not...
        if (headSNC.resetAndConfigureLoopBody()) {
            // (4a) reset the nodes in the body (only those -
            //     make sure end of loop is NOT reset). Make sure reset()
            //     is performed in the correct order (last nodes first!)
            ListIterator<NodeAndInports> li = loopBodyNodes.listIterator(
                    loopBodyNodes.size());
            while (li.hasPrevious()) {
                NodeAndInports nai = li.previous();
                NodeID id = nai.getID();
                NodeContainer nc = m_workflow.getNode(id);
                if (nc == null) {
                    throw new IllegalLoopException("Node in loop body not in"
                        + " same workflow as head&tail!");
                } else if (!nc.isResetable()) {
                    LOGGER.warn("Node " + nc.getNameWithID() + " not resetable "
                            + "during loop run, it is " + nc.getState());
                    continue;
                }
                if (nc instanceof SingleNodeContainer) {
                    ((SingleNodeContainer)nc).reset();
                } else {
                    assert nc instanceof WorkflowManager;
                    // only reset the nodes connected to relevant ports.
                    // See also bug 2225
                    ((WorkflowManager)nc).resetNodesInWFMConnectedToInPorts(
                                                              nai.getInports());
                }
            }
            // clean outports of start but do not call reset
            headSNC.cleanOutPorts();
            // (5a) configure the nodes from start to rest (it's not
            //     so important if we configure more than the body)
            //     do NOT configure start of loop because otherwise
            //     we will re-create the FlowObjectStack and
            //     remove the loop-object as well!
            configureNodeAndSuccessors(headNode.getID(), false);
            // the tail node may have thrown an exception inside
            // configure, so we have to check here if the node
            // is really configured before. (Failing configures in
            // loop body nodes do NOT affect the state of the tailNode.)
            if (tailNode.getState().equals(State.MARKEDFOREXEC)) {
                // (6a) ... we enable the body to be queued again.
                for (NodeAndInports nai : loopBodyNodes) {
                    NodeID id = nai.getID();
                    NodeContainer nc = m_workflow.getNode(id);
                    if (nc instanceof SingleNodeContainer) {
                        // make sure it's not already done...
                        if (nc.getState().equals(State.IDLE)
                                || nc.getState().equals(State.CONFIGURED)) {
                            ((SingleNodeContainer)nc).markForExecution(true);
                        }
                    } else {
                        // Mark only idle or configured nodes for re-execution
                        // which are part of the flow.
                        ((WorkflowManager)nc)
                                .markForExecutionNodesInWFMConnectedToInPorts(
                                                 nai.getInports(), false);
                    }
                }
//                // and (7a) mark end of loop for re-execution
                // not needed anymore: end-of-loop state _is_ MARKEDFOREXEC!
//                ((SingleNodeContainer)tailNode).markForExecution(true);
            } else {
                // configure of tailNode failed! Abort execution of loop:
                throw new IllegalLoopException("Loop end node could not"
                    + " be executed. Aborting Loop execution.");

            }
        } else {
            // (4b-5b) skip reset/configure... just clean outports
            ListIterator<NodeAndInports> li = loopBodyNodes.listIterator(
                    loopBodyNodes.size());
            while (li.hasPrevious()) {
                NodeAndInports nai = li.previous();
                NodeID id = nai.getID();
                NodeContainer nc = m_workflow.getNode(id);
                if (nc == null) {
                    throw new IllegalLoopException("Node in loop body not in"
                        + " same workflow as head&tail!");
                }
                if (nc instanceof SingleNodeContainer) {
                    ((SingleNodeContainer)nc).cleanOutPorts();
                } else {
                    WorkflowManager wm = (WorkflowManager)nc;
                    wm.cleanOutputPortsInWFMConnectedToInPorts(nai.getInports());
                }
            }
            // clean outports of start but do not call reset
            headSNC.cleanOutPorts();
            // (6b) ...only re-"mark" loop body (tail is already marked)
            for (NodeAndInports nai : loopBodyNodes) {
                NodeID id = nai.getID();
                NodeContainer nc = m_workflow.getNode(id);
                if (nc instanceof SingleNodeContainer) {
                    // make sure it's not already done...
                    if (nc.getState().equals(State.EXECUTED)) {
                        ((SingleNodeContainer)nc).markForReExecutionInLoop();
                    }
                } else {
                    // Mark executed nodes for re-execution (will also mark
                    // queuded and idle nodes but those don't exist)
                    ((WorkflowManager)nc)
                            .markForExecutionNodesInWFMConnectedToInPorts(
                                             nai.getInports(), true);
                }
            }
            // and (7b) mark end of loop for re-execution
//            assert tailNode.getState().equals(State.CONFIGURED);
//            ((SingleNodeContainer)tailNode).markForExecution(true);
            // see above - state is ok
            assert tailNode.getState().equals(State.MARKEDFOREXEC);
        }
        // (8) allow access to tail node
        ((SingleNodeContainer)headNode).getNode().setLoopEndNode(
                ((SingleNodeContainer)tailNode).getNode());
        // (9) and finally try to queue the head of this loop!
        assert headNode.getState().equals(State.MARKEDFOREXEC);
        queueIfQueuable(headNode);
    }

    /* Parallelize this "loop": create appropriate number of parallel
     * branches executing the matching chunks.
     */
    private void parallelizeLoop(final NodeID startID)
    throws IllegalLoopException {
    	synchronized (m_workflowMutex) {
    		final NodeID endID = m_workflow.getMatchingLoopEnd(startID);
            LoopEndParallelizeNode endNode;
            LoopStartParallelizeNode startNode;
    		try {
    			// just for validation
    			startNode = castNodeModel(startID, LoopStartParallelizeNode.class);
    			endNode = castNodeModel(endID, LoopEndParallelizeNode.class);
    		} catch (IllegalArgumentException iae) {
    			throw new IllegalLoopException(iae.getMessage(), iae);
    		}

    		final ArrayList<NodeAndInports> loopBody =
    			m_workflow.findAllNodesConnectedToLoopBody(startID, endID);
    		NodeID[] loopNodes = new NodeID[loopBody.size()];
    		loopNodes[0] = startID;
    		for (int i = 0; i < loopBody.size(); i++) {
    			loopNodes[i] = loopBody.get(i).getID();
    		}
    		// creating matching sub workflow node holding all chunks
    		Set<Pair<NodeID, Integer>> exposedInports =
    		    findNodesWithExternalSources(startID, loopNodes);
    		HashMap<Pair<NodeID, Integer>, Integer> extInConnections
    		    = new HashMap<Pair<NodeID, Integer>, Integer>();
            PortType[] exposedInportTypes = new PortType[exposedInports.size() + 1];
            // the first port is the variable port
            exposedInportTypes[0] = FlowVariablePortObject.TYPE;
            // the remaining ones will cover the exposed inports of the loop body
            int index = 1;
            for (Pair<NodeID, Integer> npi : exposedInports) {
                NodeContainer nc = getNodeContainer(npi.getFirst());
                int portIndex = npi.getSecond();
                exposedInportTypes[index] = nc.getInPort(portIndex).getPortType();
                extInConnections.put(npi, index);
                index++;
            }
            WorkflowManager subwfm = createAndAddSubWorkflow(
                    exposedInportTypes, new PortType[0], "Parallel Chunks");
            UIInformation startUIPlain = getNodeContainer(startID).getUIInformation();
            if (startUIPlain instanceof NodeUIInformation) {
                NodeUIInformation startUI = ((NodeUIInformation)startUIPlain).
                createNewWithOffsetPosition(new int[]{60, -60, 0, 0});
                subwfm.setUIInformation(startUI);
            }
            // connect outside(!) nodes to new sub metanote
            for (Pair<NodeID, Integer>npi : extInConnections.keySet()) {
                int metanodeindex = extInConnections.get(npi);
                if (metanodeindex >= 0) {  // ignore variable port!
                    // we need to find the source again (since our list
                    // only holds the destination...)
                    ConnectionContainer cc = this.getIncomingConnectionFor(
                            npi.getFirst(), npi.getSecond());
                    this.addConnection(cc.getSource(), cc.getSourcePort(),
                            subwfm.getID(), metanodeindex);
                }
            }
    		ParallelizedChunkContentMaster pccm
    		    = new ParallelizedChunkContentMaster(subwfm, endNode,
    		                startNode.getNrRemoteChunks());
    		for (int i = 0; i < startNode.getNrRemoteChunks(); i++) {
    			ParallelizedChunkContent copiedNodes =
    			    duplicateLoopBodyInSubWFMandAttach(
    			            subwfm, extInConnections,
    			    		startID, endID, loopNodes, i);
                copiedNodes.executeChunk();
    			pccm.addParallelChunk(i, copiedNodes);
    		}
            // make sure head knows his chunk master (for potential cleanup)
            startNode.setChunkMaster(pccm);
		}
    }

    /*
     * Identify all nodes that have incoming connections which are not part
     * of a given set of nodes.
     *
     * @param startID id of first node (don't include)
     * @param ids NodeIDs of set of nodes
     * @return set of NodeIDs and inport indices that have outside conn.
     */
    private Set<Pair<NodeID, Integer>> findNodesWithExternalSources(
            final NodeID startID,
            final NodeID[] ids) {
        // for quick search:
        HashSet<NodeID> orgIDsHash = new HashSet<NodeID>(Arrays.asList(ids));
        // result
        HashSet<Pair<NodeID, Integer>> exposedInports =
            new HashSet<Pair<NodeID, Integer>>();
        for (NodeID id : ids) {
            if (m_workflow.getConnectionsByDest(id) != null)
            for (ConnectionContainer cc : m_workflow.getConnectionsByDest(id)) {
                if (   (!orgIDsHash.contains(cc.getSource()))
                    && (!cc.getSource().equals(startID))) {
                    Pair<NodeID, Integer> npi
                            = new Pair<NodeID, Integer>(cc.getDest(),
                                                        cc.getDestPort());
                    if (!exposedInports.contains(npi)) {
                        exposedInports.add(npi);
                    }
                }
            }
        }
        return exposedInports;
    }

    /*
     * ...
     * @param subWFM already prepared subworkflow with appropriate
     *   inports. If subWFM==this then the subworkflows are simply
     *   added to the same workflow.
     * @param extInConnections map of incoming connections
     *   (NodeID + PortIndex) => WFM-Inport. Can be null if subWFM==this.
     * ...
     */
    private ParallelizedChunkContent duplicateLoopBodyInSubWFMandAttach(
            final WorkflowManager subWFM,
            final HashMap<Pair<NodeID, Integer>, Integer> extInConnections,
            final NodeID startID, final NodeID endID, final NodeID[] oldIDs,
            final int chunkIndex) {
        assert Thread.holdsLock(m_workflowMutex);
        // compute offset for new nodes (shifted in case of same
        // workflow, otherwise just underneath each other)
        final int[] moveUIDist;
        if (subWFM == this) {
            moveUIDist = new int[]{(chunkIndex + 1) * 10,
                    (chunkIndex + 1) * 80, 0, 0};
        } else {
            moveUIDist = new int[]{(chunkIndex + 1) * 0,
                    (chunkIndex + 1) * 150, 0, 0};
        }
        // create virtual start node
        NodeContainer startNode = getNodeContainer(startID);
        // find port types (ignore Variable Port "ear")
        PortType[] outTypes = new PortType[startNode.getNrOutPorts() - 1];
        for (int i = 0; i < outTypes.length; i++) {
            outTypes[i] = startNode.getOutPort(i + 1).getPortType();
        }
        NodeID virtualStartID = subWFM.createAndAddNode(
                new VirtualPortObjectInNodeFactory(outTypes));
        UIInformation startUIPlain = startNode.getUIInformation();
        if (startUIPlain instanceof NodeUIInformation) {
            NodeUIInformation startUI = ((NodeUIInformation)startUIPlain).
            createNewWithOffsetPosition(moveUIDist);
            subWFM.getNodeContainer(virtualStartID).setUIInformation(startUI);
        }
        // create virtual end node
        NodeContainer endNode = getNodeContainer(endID);
        assert endNode instanceof SingleNodeContainer;
        // find port types (ignore Variable Port "ear")
        PortType[] realInTypes = new PortType[endNode.getNrInPorts() - 1];
        for (int i = 0; i < realInTypes.length; i++) {
            realInTypes[i] = endNode.getInPort(i + 1).getPortType();
        }
        NodeID virtualEndID = subWFM.createAndAddNode(
                new VirtualPortObjectOutNodeFactory(realInTypes));
        UIInformation endUIPlain = endNode.getUIInformation();
        if (endUIPlain instanceof NodeUIInformation) {
            NodeUIInformation endUI = ((NodeUIInformation)endUIPlain).
            createNewWithOffsetPosition(moveUIDist);
            subWFM.getNodeContainer(virtualEndID).setUIInformation(endUI);
        }
        // copy nodes in loop body
        WorkflowCopyContent copyContent = new WorkflowCopyContent();
        copyContent.setNodeIDs(oldIDs);
        WorkflowCopyContent newBody
            = subWFM.copyFromAndPasteHere(this, copyContent);
        NodeID[] newIDs = newBody.getNodeIDs();
        Map<NodeID, NodeID> oldIDsHash = new HashMap<NodeID, NodeID>();
        for (int i = 0; i < oldIDs.length; i++) {
            oldIDsHash.put(oldIDs[i], newIDs[i]);
            NodeContainer nc = subWFM.getNodeContainer(newIDs[i]);
            UIInformation uiInfo = nc.getUIInformation();
            if (uiInfo instanceof NodeUIInformation) {
                NodeUIInformation ui = (NodeUIInformation) uiInfo;
                nc.setUIInformation(ui.createNewWithOffsetPosition(moveUIDist));
            }
        }
        // restore connections to nodes outside the loop body (only incoming)
        for (int i = 0; i < newIDs.length; i++) {
            NodeContainer oldNode = getNodeContainer(oldIDs[i]);
            for (int p = 0; p < oldNode.getNrInPorts(); p++) {
                ConnectionContainer c = getIncomingConnectionFor(oldIDs[i], p);
                if (c == null) {
                    // ignore: no incoming connection
                } else if (oldIDsHash.containsKey(c.getSource())) {
                    // ignore: connection already retained by paste persistor
                } else if (c.getSource().equals(startID)) {
                    // used to connect to start node, connect to virtual in now
                    subWFM.addConnection(virtualStartID, c.getSourcePort(),
                            newIDs[i], c.getDestPort());
                } else {
                    // source node not part of loop:
                    if (subWFM == this) {
                        addConnection(c.getSource(), c.getSourcePort(),
                                newIDs[i], c.getDestPort());
                    } else {
                        // find new replacement port
                        int subWFMportIndex = extInConnections.get(
                                new Pair<NodeID, Integer>(c.getDest(),
                                                          c.getDestPort()));
                        subWFM.addConnection(subWFM.getID(), subWFMportIndex,
                                newIDs[i], c.getDestPort());
                    }
                }
            }
        }
        // attach incoming connections of new Virtual End Node
        for (int p = 0; p < endNode.getNrInPorts(); p++) {
            ConnectionContainer c = getIncomingConnectionFor(endID, p);
            if (c == null) {
                // ignore: no incoming connection
            } else if (oldIDsHash.containsKey(c.getSource())) {
                // connects to node in loop - connect to copy
                NodeID source = oldIDsHash.get(c.getSource());
                subWFM.addConnection(source, c.getSourcePort(),
                        virtualEndID, c.getDestPort());
            } else if (c.getSource().equals(startID)) {
                // used to connect to start node, connect to virtual in now
                subWFM.addConnection(virtualStartID, c.getSourcePort(),
                        virtualEndID, c.getDestPort());
            } else {
                // source node not part of loop
                if (subWFM == this) {
                    addConnection(c.getSource(), c.getSourcePort(),
                            virtualEndID, c.getDestPort());
                } else {
                    // find new replacement port
                    int subWFMportIndex = extInConnections.get(
                            new Pair<NodeID, Integer>(c.getSource(),
                                                      c.getSourcePort()));
                    subWFM.addConnection(this.getID(), subWFMportIndex,
                            virtualEndID, c.getDestPort());
                }
            }
        }
        if (subWFM == this) {
            // connect start node var port with virtual start node
            addConnection(startID, 0, virtualStartID, 0);
        } else {
            // add variable connection to port 0 of WFM!
            if (this.canAddConnection(startID, 0, subWFM.getID(), 0)) {
                // only add this one the first time...
                this.addConnection(startID, 0, subWFM.getID(), 0);
            }
            subWFM.addConnection(subWFM.getID(), 0, virtualStartID, 0);
        }
        // set chunk of table to be processed in new virtual start node
        LoopStartParallelizeNode startModel =
            castNodeModel(startID, LoopStartParallelizeNode.class);
        VirtualNodeInput data = startModel.getVirtualNodeInput(chunkIndex);
        VirtualPortObjectInNodeModel virtualInModel =
            subWFM.castNodeModel(virtualStartID, VirtualPortObjectInNodeModel.class);
        virtualInModel.setVirtualNodeInput(data);
        return new ParallelizedChunkContent(subWFM, virtualStartID,
                virtualEndID, newIDs);
    }

    /** Check if we can expand the selected metanode into a set of nodes in
     * this WFM.
     * This essentially checks if the nodes can be moved (=deleted from
     * the original WFM) or if they are executed
     *
     * @param orgID the id of the metanode to be expanded
     * @return null of ok otherwise reason (String) why not
     */
    public String canExpandMetaNode(final NodeID wfmID) {
        if (!(getNodeContainer(wfmID) instanceof WorkflowManager)) {
            // wrong type of node!
            return "Can not expand "
                    + "selected node (not a metanode).";
        }
        if (!canRemoveNode(wfmID)) {
            // we can not - bail!
            return "Can not move all "
                    + "selected nodes (successor executing?).";
        }
        return null;
    }

    /** Expand the selected metanode into a set of nodes in
     * this WFM and remove the old metanode.
     *
     * @param orgID the id of the metanode to be expanded
     * @throws IllegalArgumentException if expand can not be done
     */
    public void expandMetaNode(final NodeID wfmID)
    throws IllegalArgumentException {
        synchronized (m_workflowMutex) {
            // check again, to be sure...
            String res = canExpandMetaNode(wfmID);
            if (res != null) {
                throw new IllegalArgumentException(res);
            }
            //
            WorkflowManager subWFM = (WorkflowManager)getNodeContainer(wfmID);
            // retrieve all nodes from metanode
            Collection<NodeContainer> ncs = subWFM.getNodeContainers();
            NodeID[] orgIDs = new NodeID[ncs.size()];
            int i = 0;
            for (NodeContainer nc : ncs) {
                orgIDs[i] = nc.getID();
                i++;
            }
            // copy the nodes from the sub workflow manager:
            WorkflowCopyContent orgContent = new WorkflowCopyContent();
            orgContent.setNodeIDs(orgIDs);
            WorkflowCopyContent newContent
                    = this.copyFromAndPasteHere(subWFM, orgContent);
            NodeID[] newIDs = newContent.getNodeIDs();
            Map<NodeID, NodeID> oldIDsHash = new HashMap<NodeID, NodeID>();
            for (i = 0; i < orgIDs.length; i++) {
                oldIDsHash.put(orgIDs[i], newIDs[i]);
            }
            // connect connections TO the sub workflow:
            for (ConnectionContainer cc :
                        m_workflow.getConnectionsByDest(subWFM.getID())) {
                int destPortIndex = cc.getDestPort();
                for (ConnectionContainer subCC :
                        subWFM.m_workflow.getConnectionsBySource(subWFM.getID())) {
                    if (subCC.getSourcePort() == destPortIndex) {
                        // reconnect
                        NodeID newID = oldIDsHash.get(subCC.getDest());
                        this.addConnection(cc.getSource(), cc.getSourcePort(),
                                newID, subCC.getDestPort());
                    }
                }
            }
            // connect connection FROM the sub workflow
            for (ConnectionContainer cc :
                        getOutgoingConnectionsFor(subWFM.getID())) {
                int sourcePortIndex = cc.getSourcePort();
                ConnectionContainer subCC = subWFM.getIncomingConnectionFor(
                        subWFM.getID(), sourcePortIndex);
                if (subCC != null) {
                    // delete existing connection from Metanode to Node
                    // reconnect
                    NodeID newID = oldIDsHash.get(subCC.getSource());
                    this.addConnection(newID, subCC.getSourcePort(),
                            cc.getDest(), cc.getDestPort());
                }
            }
            // move nodes so that their center lies on the position of
            // the old metanode!
            // ATTENTION: if you change this make sure it is (correctly)
            // revertable by collapseToMetaNodes (undo-redo!)
            int xmin = Integer.MAX_VALUE;
            int ymin = Integer.MAX_VALUE;
            int xmax = Integer.MIN_VALUE;
            int ymax = Integer.MIN_VALUE;
            for (i = 0; i < newIDs.length; i++) {
                NodeContainer nc = getNodeContainer(newIDs[i]);
                UIInformation uii = nc.getUIInformation();
                if (uii instanceof NodeUIInformation) {
                    int[] bounds = ((NodeUIInformation)uii).getBounds();
                    if (bounds.length >= 2) {
                        xmin = Math.min(bounds[0], xmin);
                        ymin = Math.min(bounds[1], ymin);
                        xmax = Math.max(bounds[0], xmax);
                        ymax = Math.max(bounds[1], ymax);
                    }
                }
            }
            UIInformation uii = subWFM.getUIInformation();
            if (uii instanceof NodeUIInformation) {
                int[] metaBounds = ((NodeUIInformation)uii).getBounds();
                int xShift = metaBounds[0] - (xmin + xmax) / 2;
                int yShift = metaBounds[1] - (ymin + ymax) / 2;
                for (i = 0; i < newIDs.length; i++) {
                    NodeContainer nc = getNodeContainer(newIDs[i]);
                    uii = nc.getUIInformation();
                    if (uii instanceof NodeUIInformation) {
                        NodeUIInformation newUii
                           = ((NodeUIInformation)uii).createNewWithOffsetPosition(
                                   new int[]{xShift, yShift});
                        nc.setUIInformation(newUii);
                    }
                }
            }
            // and finally remove old sub workflow
            this.removeNode(wfmID);
        }
    }

    /** Check if we can collapse selected set of nodes into a metanode.
     * This essentially checks if the nodes can be moved (=deleted from
     * the original WFM), if they are executed, or if moving them would
     * result in cycles in the original WFM (outgoing connections fed
     * back into inports of the new Metanode).
     *
     * @param orgIDs the ids of the nodes to be moved to the new metanode.
     * @return null or reason why this can not be done as string.
     */
    public String canCollapseNodesIntoMetaNode(final NodeID[] orgIDs) {
        synchronized (m_workflowMutex) {
            // for quick search:
            HashSet<NodeID> orgIDsHash = new HashSet<NodeID>(Arrays.asList(orgIDs));
            // Check if we are allowed to move (=delete) all those nodes
            for (NodeID id : orgIDs) {
                if (!canRemoveNode(id)) {
                    // we can not - bail!
                    return "Can not move all "
                            + "selected nodes (successor executing?).";
                }
            }
            // Check if any of those nodes are executed
            for (NodeID id : orgIDs) {
                NodeContainer nc = getNodeContainer(id);
                if (State.EXECUTED.equals(nc.getState())) {
                    // we can not - bail!
                    return"Can not move executed nodes (reset first).";
                }
            }
            // Check if move will create loops in WFM connected to new Metanode
            // a) first find set of nodes connected to the selected ones and not
            //    part of the list
            HashSet<NodeID> ncNodes = new HashSet<NodeID>();
            for (NodeID id : orgIDs) {
                for (ConnectionContainer cc
                        : m_workflow.getConnectionsBySource(id)) {
                    NodeID destID = cc.getDest();
                    if ((!this.getID().equals(destID))
                            && (!orgIDsHash.contains(destID))) {
                        // successor which is not part of list - remember it!
                        ncNodes.add(destID);
                    }
                }
            }
            // b) check if any successor of those nodes is IN our list!
            while (!ncNodes.isEmpty()) {
                NodeID thisID = ncNodes.iterator().next();
                ncNodes.remove(thisID);
                for (ConnectionContainer cc
                        : m_workflow.getConnectionsBySource(thisID)) {
                    NodeID destID = cc.getDest();
                    if (!this.getID().equals(destID)) {
                        if (orgIDsHash.contains(destID)) {
                            // successor is in our original list - bail!
                            return "Can not move "
                                    + "nodes - selected set is not closed!";
                        }
                        ncNodes.add(destID);
                    }
                }
            }
        }
        return null;
    }

    /** Collapse selected set of nodes into a metanode. Make sure connections
     * from and two nodes not contained in this set are passed through
     * appropriate ports of the new metanode.
     *
     * @param orgIDs the ids of the nodes to be moved to the new metanode.
     * @param name of the new metanode
     * @return newly create metanode
     * @throws IllegalArgumentException if collapse can not be done
     */
    public WorkflowManager collapseNodesIntoMetaNode(final NodeID[] orgIDs,
            final String name)
    throws IllegalArgumentException {
        synchronized (m_workflowMutex) {
            // make sure this is still true:
            String res = canCollapseNodesIntoMetaNode(orgIDs);
            if (res != null) {
                throw new IllegalArgumentException(res);
            }
            // for quick search:
            HashSet<NodeID> orgIDsHash = new HashSet<NodeID>(Arrays.asList(orgIDs));
            // find Nodes/Ports that have incoming connections from the outside.
            // Map will hold DestNodeID/PortIndex + Index of new MetanodeInport.
            HashMap<Pair<NodeID, Integer>, Integer> exposedInports =
                new HashMap<Pair<NodeID, Integer>, Integer>();
            int inMNindex = 0;
            for (NodeID id : orgIDs) {
                if (m_workflow.getConnectionsByDest(id) != null)
                for (ConnectionContainer cc : m_workflow.getConnectionsByDest(id)) {
                    if (!orgIDsHash.contains(cc.getSource())) {
                        Pair<NodeID, Integer> npi
                                = new Pair<NodeID, Integer>(cc.getDest(),
                                                            cc.getDestPort());
                        if (!exposedInports.containsKey(npi)) {
                            exposedInports.put(npi, inMNindex);
                            inMNindex++;
                        }
                    }
                }
            }
            // find Nodes/Ports that have outgoing connections to the outside.
            // Map will hold SourceNodeID/PortIndex + Index of new MetanodeOutport.
            HashMap<Pair<NodeID, Integer>, Integer> exposedOutports =
                new HashMap<Pair<NodeID, Integer>, Integer>();
            int outMNindex = 0;
            for (NodeID id : orgIDs) {
                for (ConnectionContainer cc : m_workflow.getConnectionsBySource(id)) {
                    if (!orgIDsHash.contains(cc.getDest())) {
                        Pair<NodeID, Integer> npi
                                = new Pair<NodeID, Integer>(cc.getSource(),
                                                            cc.getSourcePort());
                        if (!exposedOutports.containsKey(npi)) {
                            exposedOutports.put(npi, outMNindex);
                            outMNindex++;
                        }
                    }
                }
            }
            // determine types of new Metanode in- and outports:
            // (note that we reach directly into the Node to get the port type
            //  so we need to correct the index for the - then missing - var
            //  port.)
            PortType[] exposedInportTypes = new PortType[exposedInports.size()];
            for (Pair<NodeID, Integer> npi : exposedInports.keySet()) {
                int index = exposedInports.get(npi);
                NodeContainer nc = getNodeContainer(npi.getFirst());
                int portIndex = npi.getSecond();
                exposedInportTypes[index] = nc.getInPort(portIndex).getPortType();
            }
            PortType[] exposedOutportTypes = new PortType[exposedOutports.size()];
            for (Pair<NodeID, Integer> npi : exposedOutports.keySet()) {
                int index = exposedOutports.get(npi);
                NodeContainer nc = getNodeContainer(npi.getFirst());
                int portIndex = npi.getSecond();
              exposedOutportTypes[index]
                                  = nc.getOutPort(portIndex).getPortType();
            }
            // create the new Metanode
            WorkflowManager newWFM = createAndAddSubWorkflow(exposedInportTypes,
                    exposedOutportTypes, name);
            // move into center of nodes this one replaces...
            int x = 0;
            int y = 0;
            int count = 0;
            if (orgIDs.length >=1 ) {
                for (int i = 0; i < orgIDs.length; i++) {
                    NodeContainer nc = getNodeContainer(orgIDs[i]);
                    UIInformation uii = nc.getUIInformation();
                    if (uii instanceof NodeUIInformation) {
                        int[] bounds = ((NodeUIInformation)uii).getBounds();
                        if (bounds.length >= 2) {
                            x += bounds[0];
                            y += bounds[1];
                            count++;
                        }
                    }
                }
            }
            if (count >= 1) {
                NodeUIInformation newUii = new NodeUIInformation(x/count, y/count,
                        -1, -1, true);
                newWFM.setUIInformation(newUii);
            }
            // copy the nodes into the newly create WFM:
            WorkflowCopyContent orgContent = new WorkflowCopyContent();
            orgContent.setNodeIDs(orgIDs);
            WorkflowCopyContent newContent
                    = newWFM.copyFromAndPasteHere(this, orgContent);
            NodeID[] newIDs = newContent.getNodeIDs();
            Map<NodeID, NodeID> oldIDsHash = new HashMap<NodeID, NodeID>();
            for (int i = 0; i < orgIDs.length; i++) {
                oldIDsHash.put(orgIDs[i], newIDs[i]);
            }
            // move subworkflows into upper left corner but keep
            // original layout (important for undo!)
            // ATTENTION: if you change this, make sure it is revertable
            // by extractMetanode (and correctly so!).
            int xmin = Integer.MAX_VALUE;
            int ymin = Integer.MAX_VALUE;
            if (newIDs.length >=1 ) {
                for (int i = 0; i < newIDs.length; i++) {
                    NodeContainer nc = newWFM.getNodeContainer(newIDs[i]);
                    UIInformation uii = nc.getUIInformation();
                    if (uii instanceof NodeUIInformation) {
                        int[] bounds = ((NodeUIInformation)uii).getBounds();
                        if (bounds.length >= 2) {
                            xmin = Math.min(bounds[0], xmin);
                            ymin = Math.min(bounds[1], ymin);
                        }
                    }
                }
                int xshift = 150 - Math.max(xmin, 70);
                int yshift = 120 - Math.max(ymin, 20);
                for (int i = 0; i < newIDs.length; i++) {
                    NodeContainer nc = newWFM.getNodeContainer(newIDs[i]);
                    UIInformation uii = nc.getUIInformation();
                    if (uii instanceof NodeUIInformation) {
                        NodeUIInformation newUii
                           = ((NodeUIInformation)uii).createNewWithOffsetPosition(
                                   new int[]{xshift, yshift});
                        nc.setUIInformation(newUii);
                    }
                }
            }
            // create connections INSIDE the new workflow
            for (Pair<NodeID, Integer> npi : exposedInports.keySet()) {
                int index = exposedInports.get(npi);
                NodeID newID = oldIDsHash.get(npi.getFirst());
                newWFM.addConnection(newWFM.getID(), index, newID, npi.getSecond());
            }
            for (Pair<NodeID, Integer> npi : exposedOutports.keySet()) {
                int index = exposedOutports.get(npi);
                NodeID newID = oldIDsHash.get(npi.getFirst());
                newWFM.addConnection(newID, npi.getSecond(), newWFM.getID(), index);
            }
            // create OUTSIDE connections to and from the new workflow
            for (NodeID id : orgIDs) {
                // convert to a seperate array so we can delete connections!
                ConnectionContainer[] cca = new ConnectionContainer[0];
                cca = m_workflow.getConnectionsByDest(id).toArray(cca);
                for (ConnectionContainer cc : cca) {
                    if (!orgIDsHash.contains(cc.getSource())) {
                        Pair<NodeID, Integer> npi
                                = new Pair<NodeID, Integer>(cc.getDest(),
                                                            cc.getDestPort());
                        int newPort = exposedInports.get(npi);
                        this.addConnection(cc.getSource(), cc.getSourcePort(),
                                newWFM.getID(), newPort);
                        this.removeConnection(cc);
                    }
                }
            }
            for (NodeID id : orgIDs) {
                // convert to a seperate array so we can delete connections!
                ConnectionContainer[] cca = new ConnectionContainer[0];
                cca = m_workflow.getConnectionsBySource(id).toArray(cca);
                for (ConnectionContainer cc : cca) {
                    if (!orgIDsHash.contains(cc.getDest())) {
                        Pair<NodeID, Integer> npi
                                = new Pair<NodeID, Integer>(cc.getSource(),
                                                            cc.getSourcePort());
                        int newPort = exposedOutports.get(npi);
                        this.removeConnection(cc);
                        this.addConnection(newWFM.getID(), newPort,
                                cc.getDest(), cc.getDestPort());
                    }
                }
            }
            // and finally: delete the original nodes.
            for (NodeID id : orgIDs) {
                this.removeNode(id);
            }
            return newWFM;
        }
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

    /** {@inheritDoc} */
    @Override
    boolean canPerformReset() {
        // check for at least one executed and resetable node!
        for (NodeContainer nc : m_workflow.getNodeValues()) {
            if (nc.getState().executionInProgress()) {
                return false;
            }
            if (nc.canPerformReset()) {
                return true;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    void markForExecution(final boolean flag) {
        assert !isLocalWFM() : "Setting execution mark on meta node not allowed"
            + " for locally executing (sub-)flows";
        if (getState().executionInProgress()) {
            throw new IllegalStateException("Execution of (sub-)flow already "
                    + "in progress, current state is " + getState());
        }
        markForExecutionAllNodesInWorkflow(flag);
        setState(State.MARKEDFOREXEC);
    }

    /** {@inheritDoc} */
    @Override
    void mimicRemoteExecuting() {
        synchronized (m_workflowMutex) {
            for (NodeContainer nc : m_workflow.getNodeValues()) {
                nc.mimicRemoteExecuting();
            }
            // do not propagate -- this method is called from parent
            checkForNodeStateChanges(false);
        }
    }

    /** {@inheritDoc} */
    @Override
    void mimicRemotePreExecute() {
        synchronized (m_workflowMutex) {
            for (NodeContainer nc : m_workflow.getNodeValues()) {
                nc.mimicRemotePreExecute();
            }
            // do not propagate -- this method is called from parent
            checkForNodeStateChanges(false);
        }
    }

    /** {@inheritDoc} */
    @Override
    void mimicRemotePostExecute() {
        synchronized (m_workflowMutex) {
            for (NodeContainer nc : m_workflow.getNodeValues()) {
                nc.mimicRemotePostExecute();
            }
            // do not propagate -- this method is called from parent
            checkForNodeStateChanges(false);
        }
    }

    /** {@inheritDoc} */
    @Override
    void mimicRemoteExecuted(final NodeContainerExecutionStatus status) {
        synchronized (m_workflowMutex) {
            for (NodeContainer nc : m_workflow.getNodeValues()) {
                int i = nc.getID().getIndex();
                NodeContainerExecutionStatus sub = status.getChildStatus(i);
                if (sub == null) {
                    assert false : "Execution status is null for child " + i;
                    sub = NodeContainerExecutionStatus.FAILURE;
                }
                // will be ignored on already executed nodes
                // (think of an executed file reader in a meta node that is
                // submitted onto a cluster in the executed state already)
                nc.mimicRemoteExecuted(sub);
            }
            // do not propagate -- method is (indirectly) called from parent
            checkForNodeStateChanges(false);
        }
    }

    /** {@inheritDoc} */
    @Override
    boolean performStateTransitionPREEXECUTE() {
        assert !isLocalWFM() : "Execution of meta node not allowed"
            + " for locally executing (sub-)flows";
        synchronized (m_nodeMutex) {
            if (getState().executionInProgress()) {
                for (NodeContainer nc : m_workflow.getNodeValues()) {
                    nc.mimicRemotePreExecute();
                }
                // method is called from parent, don't propagate state changes
                checkForNodeStateChanges(false);
                return true;
            } else {
                // node may not be executinInProgress when previously queued
                // but then canceled upon user request; this method is called
                // from a worker thread, which does not know about cancelation
                // yet (it is interrupted when run as local job ... but this
                // isn't a local job)
                return false;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    void performStateTransitionEXECUTING() {
        assert !isLocalWFM() : "Execution of meta node not allowed"
            + " for locally executing (sub-)flows";
        synchronized (m_nodeMutex) {
            for (NodeContainer nc : m_workflow.getNodeValues()) {
                nc.mimicRemoteExecuting();
            }
            // method is called from parent, don't propagate state changes
            checkForNodeStateChanges(false);
        }
    }

    /** {@inheritDoc} */
    @Override
    void performStateTransitionPOSTEXECUTE() {
        assert !isLocalWFM() : "Execution of meta node not allowed"
            + " for locally executing (sub-)flows";
        synchronized (m_nodeMutex) {
            for (NodeContainer nc : m_workflow.getNodeValues()) {
                nc.mimicRemotePostExecute();
            }
            // method is called from parent, don't propagate state changes
            checkForNodeStateChanges(false);
        }
    }

    /** {@inheritDoc} */
    @Override
    void performStateTransitionEXECUTED(
            final NodeContainerExecutionStatus status) {
        assert !isLocalWFM() : "Execution of meta node not allowed"
            + " for locally executing (sub-)flows";
        synchronized (m_workflowMutex) {
            mimicRemoteExecuted(status);
            String stateList = printNodeSummary(getID(), 0);
            // this method is called from the parent's doAfterExecute
            // we don't propagate state changes (i.e. argument flag is false)
            // since the check for state changes in the parent will happen next
            if (!sweep(false)) {
                LOGGER.debug("Some states were invalid, old states are:");
                LOGGER.debug(stateList);
                LOGGER.debug("The new (corrected) states are: ");
                LOGGER.debug(printNodeSummary(getID(), 0));
            }
            // allow failed nodes (IDLE) to be configured
            configureAllNodesInWFM(/*keepNodeMessage=*/true);
        }
    }

    /* ------------- node commands -------------- */

    /**
     * Check if a node can be reset, meaning that it is executed and all of
     * its successors are idle or executed as well. We do not want to mess
     * with executing chains.
     *
     * @param nodeID the id of the node
     * @return true if the node can safely be reset.
     */
    public boolean canResetNode(final NodeID nodeID) {
        synchronized (m_workflowMutex) {
            NodeContainer nc = m_workflow.getNode(nodeID);
            if (nc == null) {
                return false;
            }
            // (a) this node is resetable
            // (b) no successors is running or queued.
            return (nc.canPerformReset()
                    && (!hasSuccessorInProgress(nodeID)));
        }
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
            ((WorkflowManager)nc).resetAllNodesInWFM();
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
                        WorkflowManager wfm = (WorkflowManager)nc;
                        // this is ok, since we will never call this again
                        // while traversing a flow - this is the main entry
                        // point from the outside and should reset all children
                        // (resetSuccessors() follows ports and will be
                        // called throughout subsequent calls...)

                        // TODO this configures the meta node already, which
                        // is done 2nd time two lines below
                        wfm.resetAndReconfigureAllNodesInWFM();
                    }
                    nc.resetJobManagerViews();
                    // and launch configure starting with this node
                    configureNodeAndSuccessors(id, true);
                } else if (nc.getState().equals(State.IDLE)) {
                    // the node is IDLE: we don't need to reset it but we
                    // should remove its node message! (This for instance
                    // matters when we disconnect the inport of this node
                    // and it showed an error due to conflicting stacks!)
                    nc.setNodeMessage(null);
                    // But maybe the node is configurable?
                    configureNodeAndSuccessors(id, true);
                }
            } else {
                throw new IllegalStateException(
                "Can not reset node (wrong state of node or successors) " + id);
            }
        }
        // TODO this should go into the synchronized block?
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
            // don't allow individual execution of nodes in a remote exec flow
            if (!isLocalWFM()) {
                return false;
            }
            // check for WorkflowManager - which we handle differently
            if (nc instanceof WorkflowManager) {
                return ((WorkflowManager)nc).hasExecutableNode();
            }
            return nc.getState().equals(State.CONFIGURED);
        }
    }

    /** Check if a node can be cancelled individually.
    *
    * @param nodeID id of node
    * @return true if node can be cancelled
    *
    */
   public boolean canCancelNode(final NodeID nodeID) {
       synchronized (m_workflowMutex) {
           NodeContainer nc = m_workflow.getNode(nodeID);
           if (nc == null) {
               return false;
           }
           // don't allow individual cancellation of nodes in a remote exec flow
           if (!isLocalWFM()) {
               return false;
           }
           if (!nc.getState().executionInProgress()) {
               return false;
           }
           return true;
       }
   }

    /** @return true if any node contained in this workflow is executable,
     * that is configured.
     */
    private boolean hasExecutableNode() {
        for (NodeContainer nc : m_workflow.getNodeValues()) {
            if (nc instanceof SingleNodeContainer) {
                if (nc.getState().equals(State.CONFIGURED)) {
                    return true;
                }
            } else {
                if (((WorkflowManager)nc).hasExecutableNode()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    void cancelExecution() {
        synchronized (m_workflowMutex) {
            NodeExecutionJob job = getExecutionJob();
            if (job != null) {
                // this is a remotely executed workflow, cancel its execution
                // and let the execution job take care of a state updates of
                // the contained nodes.
                assert !isLocalWFM();
                job.cancel();
            } else {
                for (NodeContainer nc : m_workflow.getNodeValues()) {
                    nc.cancelExecution();
                }
                checkForNodeStateChanges(true);
            }
        }
    }

    /**
     * Cancel execution of the given NodeContainer.
     *
     * @param nc node to be canceled
     */
    public void cancelExecution(final NodeContainer nc) {
        synchronized (m_workflowMutex) {
            disableNodeForExecution(nc.getID());
            if (nc.getState().executionInProgress()) {
                nc.cancelExecution();
            }
            checkForNodeStateChanges(true);
        }
    }

    /**
     * Pause loop execution of the given NodeContainer (=loop end).
     *
     * @param nc node to be canceled
     */
    public void pauseLoopExecution(final NodeContainer nc) {
        if (nc instanceof SingleNodeContainer) {
            SingleNodeContainer snc = (SingleNodeContainer)nc;
            if (snc.getNodeModel() instanceof LoopEndNode) {
                synchronized (m_workflowMutex) {
                    if (snc.getLoopStatus().equals(LoopStatus.RUNNING)) {
                        // currently running
                        snc.pauseLoopExecution(true);
                    }
                    checkForNodeStateChanges(true);
                }
            }
        }
    }

    /** Resume operation of a paused loop. Depending on the flag we
     * either step (= run only one iteration and pause again) or run
     * until the loop is finished.
     *
     * @param nc
     */
    public void resumeLoopExecution(final NodeContainer nc,
            final boolean oneStep) {
        if (nc instanceof SingleNodeContainer) {
            SingleNodeContainer snc = (SingleNodeContainer)nc;
            if (snc.getNodeModel() instanceof LoopEndNode) {
                synchronized (m_workflowMutex) {
                    if (snc.getLoopStatus().equals(LoopStatus.PAUSED)) {
                        // currently paused - ok!
                        FlowLoopContext flc = snc.getNode().getLoopContext();
                        try {
                            if (!oneStep) {
                                snc.pauseLoopExecution(false);
                            }
                            restartLoop(flc);
                        } catch (IllegalLoopException ile) {
                            nc.setNodeMessage(new NodeMessage(
                                    NodeMessage.Type.ERROR, ile.getMessage()));
                        }
                    }
                }
            }
        }
    }

    /** Is the node with the given ID ready to take a new job manager. This
     * is generally true if the node is currently not executing.
     * @param nodeID The node in question.
     * @return Whether it's save to invoke the
     * {@link #setJobManager(NodeID, NodeExecutionJobManager)} method.
     */
    public boolean canSetJobManager(final NodeID nodeID) {
        synchronized (m_workflowMutex) {
            if (!m_workflow.containsNodeKey(nodeID)) {
                return false;
            }
            NodeContainer nc = getNodeContainer(nodeID);
            switch (nc.getState()) {
            case QUEUED:
            case PREEXECUTE:
            case EXECUTING:
            case EXECUTINGREMOTELY:
            case POSTEXECUTE:
                return false;
            default:
                return true;
            }
        }
    }

    /** Sets a new job manager on the node with the given ID.
     * @param nodeID The node in question.
     * @param jobMgr The new job manager (may be null to use parent's one).
     * @throws IllegalStateException If the node is not ready
     * @throws IllegalArgumentException If the node is unknown
     * @see #canSetJobManager(NodeID)
     */
    public void setJobManager(
            final NodeID nodeID, final NodeExecutionJobManager jobMgr) {
        synchronized (m_workflowMutex) {
            NodeContainer nc = getNodeContainer(nodeID);
            nc.setJobManager(jobMgr);
        }
    }

    /** Attempts to cancel or running nodes in preparation for a removal of
     * this node (or its parent) from the root. Executing nodes, which can be
     * disconnected from the execution (e.g. remote cluster execution) are
     * disconnected if their status has been saved before.
     */
    public void shutdown() {
        performShutdown();
    }

    /** {@inheritDoc} */
    @Override
    void performShutdown() {
        synchronized (m_workflowMutex) {
            NodeExecutionJob job = getExecutionJob();
            if (job != null) {
                if (job.isSavedForDisconnect()) {
                    findJobManager().disconnect(job);
                } else {
                    cancelExecution();
                }
            } else {
                for (NodeContainer nc : m_workflow.getNodeValues()) {
                    disableNodeForExecution(nc.getID());
                    nc.performShutdown();
                }
                checkForNodeStateChanges(false);
            }
            m_wfmListeners.clear();
            super.performShutdown();
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
        // let parent execute this node (important if job manager is assigned)
        // see also bug 2217
        getParent().executeUpToHere(getID());
        try {
            waitWhileInExecution(-1, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOGGER.warn(
                    "Thread interrupted while waiting for finishing execution");
        }
        return this.getState().equals(State.EXECUTED);
    }

    /** Causes the current thread to wait until the the workflow has reached
     * a non-executing state unless a given timeout elapsed.
     * @param time the maximum time to wait
     *       (0 or negative for waiting infinitely)
     * @param unit the time unit of the {@code time} argument
     * @return {@code false} if the waiting time detectably elapsed
     *         before return from the method, else {@code true}. It returns
     *         {@code true} if the time argument is 0 or negative.
     * @throws InterruptedException if the current thread is interrupted
     */
    public boolean waitWhileInExecution(final long time, final TimeUnit unit)
        throws InterruptedException {
        // lock supporting timeout
        final ReentrantLock lock = new ReentrantLock();
        final Condition condition = lock.newCondition();
        NodeStateChangeListener listener = new NodeStateChangeListener() {
            @Override
            public void stateChanged(final NodeStateEvent stateEvent) {
                lock.lock();
                try {
                    if (!stateEvent.getState().executionInProgress()) {
                        condition.signalAll();
                    }
                } finally {
                    lock.unlock();
                }
            }
        };
        lock.lockInterruptibly();
        addNodeStateChangeListener(listener);
        try {
        	State state = getState();
        	if (!state.executionInProgress()) {
        		return true;
        	}
            if (time > 0) {
                return condition.await(time, unit);
            } else {
                condition.await();
                return true;
            }
        } finally {
            lock.unlock();
            removeNodeStateChangeListener(listener);
        }
    }

    /** Convenience method: (Try to) Execute all nodes in the workflow.
     * This method returns immediately, leaving it to the associated
     * executor to do the job. */
    public void executeAll() {
        synchronized (m_workflowMutex) {
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
                if (nc.isLocalWFM()) {
                    assert nc instanceof WorkflowManager;
                    ((WorkflowManager)nc).executeAll();
                } else {
                    executeUpToHere(thisID);
                }
                // and finally move the current node to the other list
                executedNodes.add(thisID);
            }
        }
    }

    boolean continueExecutionOnLoad(final NodeContainer nc,
            final NodeContainerPersistor persistor)
        throws InvalidSettingsException,  NodeExecutionJobReconnectException {
        NodeContainerMetaPersistor metaPers = persistor.getMetaPersistor();
        NodeSettingsRO execJobSettings = metaPers.getExecutionJobSettings();
        NodeOutPort[] ports = assemblePredecessorOutPorts(nc.getID());
        PortObject[] inData = new PortObject[ports.length];
        boolean allDataAvailable = true;
        for (int i = 0; i < ports.length; i++) {
            if (ports[i] != null) {
                inData[i] = ports[i].getPortObject();
                // if connected but no data, set to false
                if (inData[i] == null) {
                    allDataAvailable = false;
                }
            } else if (!nc.getInPort(i).getPortType().isOptional()) {
                // unconnected non-optional port ... abort
                allDataAvailable = false;
            }
        }
        if (allDataAvailable && nc.getState().equals(State.EXECUTINGREMOTELY)) {
            nc.continueExecutionOnLoad(inData, execJobSettings);
            return true;
        }
        return false;
    }

    /////////////////////////////////////////////////////////
    // WFM as NodeContainer: Dialog related implementations
    /////////////////////////////////////////////////////////

    private NodeDialogPane m_nodeDialogPane;

    /** {@inheritDoc} */
    @Override
    public boolean hasDialog() {
        int c = NodeExecutionJobManagerPool.getNumberOfJobManagersFactories();
        return c > 1;
    }

    /** {@inheritDoc} */
    @Override
    NodeDialogPane getDialogPaneWithSettings(
            final PortObjectSpec[] inSpecs) throws NotConfigurableException {
        NodeDialogPane dialogPane = getDialogPane();
        NodeSettings settings = new NodeSettings("wfm_settings");
        saveSettings(settings);
        dialogPane.internalLoadSettingsFrom(
                settings, inSpecs, new FlowObjectStack(getID()),
                        new CredentialsProvider(this, m_credentialsStore));
        return dialogPane;
    }

    /** {@inheritDoc} */
    @Override
    NodeDialogPane getDialogPane() {
        if (m_nodeDialogPane == null) {
            if (hasDialog()) {
                m_nodeDialogPane = new EmptyNodeDialogPane();
                // workflow manager jobs can't be split
                m_nodeDialogPane.addJobMgrTab(SplitType.DISALLOWED);
            } else {
                throw new IllegalStateException("Workflow has no dialog");
            }
        }
        return m_nodeDialogPane;
    }

    /** {@inheritDoc} */
    @Override
    public boolean areDialogAndNodeSettingsEqual() {
        if (!hasDialog()) {
            return true;
        }
        String defName = "wfm_settings";
        NodeSettings origSettings = new NodeSettings(defName);
        saveSettings(origSettings);
        NodeSettings dialogSettings = new NodeSettings(defName);
        try {
            getDialogPane().finishEditingAndSaveSettingsTo(dialogSettings);
        } catch (InvalidSettingsException e) {
            return false;
        }
        return dialogSettings.equals(origSettings);
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
        // have through connections)!
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
                    switch (nop.getNodeState()) {
                    case IDLE:
                    case UNCONFIGURED_MARKEDFOREXEC:
                        inportState = State.IDLE;
                        break;
                    default:
                        inportState = State.CONFIGURED;
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
                // all executed but some through connections!
                newState = inportState;
            }
        } else if (nrNodesInState[State.CONFIGURED.ordinal()] == nrNodes) {
            // all (>=1) configured
            if (allOutPortsConnected) {
                newState = State.CONFIGURED;
            } else {
                newState = State.IDLE;
            }
        } else if (nrNodesInState[State.EXECUTED.ordinal()]
                                  + nrNodesInState[State.CONFIGURED.ordinal()]
                                                   == nrNodes) {
            newState = State.CONFIGURED;
        } else if (nrNodesInState[State.EXECUTING.ordinal()] >= 1) {
            newState = State.EXECUTING;
        } else if (nrNodesInState[State.EXECUTINGREMOTELY.ordinal()] >= 1) {
            newState = State.EXECUTINGREMOTELY;
        } else if (nrNodesInState[State.PREEXECUTE.ordinal()] >= 1) {
            newState = State.EXECUTING;
        } else if (nrNodesInState[State.POSTEXECUTE.ordinal()] >= 1) {
            newState = State.EXECUTING;
        } else if (nrNodesInState[State.QUEUED.ordinal()] >= 1) {
            newState = State.EXECUTING;
        } else if (nrNodesInState[State.UNCONFIGURED_MARKEDFOREXEC.ordinal()]
                                  >= 1) {
            newState = State.UNCONFIGURED_MARKEDFOREXEC;
        } else if (nrNodesInState[State.MARKEDFOREXEC.ordinal()] >= 1) {
            newState = State.MARKEDFOREXEC;
        }
        State oldState = this.getState();
        this.setState(newState, propagateChanges);
        boolean wasExecuting = oldState.equals(State.EXECUTINGREMOTELY)
            || oldState.equals(State.EXECUTING);
        if (wasExecuting) {
            boolean isExecuting = newState.executionInProgress();
            if (newState.equals(State.EXECUTED)) {
                // we just successfully executed this WFM: check if any
                // loops were waiting for this one in the parent workflow!
                if (getWaitingLoops().size() >= 1) {
                    // looks as if some loops were waiting for this node to
                    // finish! Let's try to restart them:
                    for (FlowLoopContext flc : getWaitingLoops()) {
                        try {
                            getParent().restartLoop(flc);
                        } catch (IllegalLoopException ile) {
                            // set error message in LoopEnd node not this one!
                            NodeMessage nm = new NodeMessage(
                                    NodeMessage.Type.ERROR,
                                    ile.getMessage());
                            getParent().getNodeContainer(flc.getTailNode())
                                .setNodeMessage(nm);
                            getParent().disableNodeForExecution(
                                    flc.getTailNode());
                        }
                    }
                    clearWaitingLoopList();
                }
            } else if (!isExecuting) {
                // if something went wrong and any others loops were waiting
                // for this node: clean them up!
                // (most likely this is just an IDLE node, however, which
                // had other flows that were not executed (such as ROOT!)
                for (FlowLoopContext flc : getWaitingLoops()) {
                    getParent().disableNodeForExecution(flc.getTailNode());
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
            if (m_workflowMutex.equals(getParent().m_workflowMutex)
                    // simple: mutexes are the same which means that we have
                    // either in- or outgoing connections (or both). No need
                    // to add an synchronize on the parent-mutex.
                    || Thread.holdsLock(getParent().m_workflowMutex)) {
                    // the second case is less simple: we don't have the same
                    // mutex but we already hold it: in this case, do not
                    // make this change asynchronously because we obviously
                    // called this from outside the "disconnected" metanode
                    // and want to keep the state change sync'ed.
                    // this fixes a problem with metanodes containing cluster
                    // (sub) workflows which were started but the state of
                    // the metanode/project was changed too late.
                getParent().checkForNodeStateChanges(propagateChanges);
            } else {
                // Different mutexes, that is this workflowmanager is a
                // project and the state check in the parent has do be
                // done asynchronosly to avoid deadlocks.
                // Locking the parent here would be exactly what we do not want
                // to do: Never lock a child (e.g. node) first and then its
                // parent (e.g. wfm) - see also bug #1755!
                PARENT_NOTIFIER.execute(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (getParent().m_workflowMutex) {
                                getParent().checkForNodeStateChanges(
                                        propagateChanges);
                        }
                    }
                  });
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
        NodeOutPort[] predOutPorts = assemblePredecessorOutPorts(id);
        for (int i = 0; i < predOutPorts.length; i++) {
            NodeOutPort p = predOutPorts[i];
            if (p == null) { // unconnected port
                // accept only if inport is optional
                if (!nc.getInPort(i).getPortType().isOptional()) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Returns true if the argument represents a source node in the workflow.
     * A source node is a node, which has (i) no predecessors and (ii) only
     * optional inputs.
     * @param id The node to test -- must exist in workflow
     * @return If argument is a source node (configure can start from there)
     */
    private boolean isSourceNode(final NodeID id) {
        NodeContainer nc = getNodeContainer(id);
        NodeOutPort[] predPorts = assemblePredecessorOutPorts(id);
        for (int i = 0; i < predPorts.length; i++) {
            NodeInPort inPort = nc.getInPort(i);
            boolean isOptional = inPort.getPortType().isOptional();
            if (predPorts[i] != null) { // has connected predecessor
                return false;
            } else if (!isOptional) {   // not connected but required
                return false;
            }
        }
        return true;
    }

    /** Attempts to configure all nodes in the workflow. It will also try to
     * configure nodes whose predecessors did not change their output specs.
     * This method checks the new state of the meta node but
     * does not propagate it (since called recursively).
     * @param keepNodeMessage Whether to retain the previously set node message.
     */
    private void configureAllNodesInWFM(final boolean keepNodeMessage) {
        assert Thread.holdsLock(m_workflowMutex);
        Set<NodeID> bfsSortedSet = m_workflow.createBreadthFirstSortedList(
                m_workflow.getNodeIDs(), true).keySet();
        for (NodeID id : bfsSortedSet) {
            NodeContainer nc = getNodeContainer(id);
            if (nc instanceof SingleNodeContainer) {
                switch (nc.getState()) {
                case EXECUTED:
                    break;
                default:
                    configureSingleNodeContainer(
                            (SingleNodeContainer)nc, keepNodeMessage);
                }
            } else {
                ((WorkflowManager)nc).configureAllNodesInWFM(keepNodeMessage);
            }
        }
        checkForNodeStateChanges(false);
    }

    /** Configure a SingleNodeContainer.
     *
     * @param snc node to be configured
     * @param keepNodeMessage Whether to keep previously set node messages
     *        (important during load sometimes)
     * @return true if the configuration did change something.
     */
    private boolean configureSingleNodeContainer(
            final SingleNodeContainer snc, final boolean keepNodeMessage) {
        boolean configurationChanged = false;
        synchronized (m_workflowMutex) {
            NodeMessage oldMessage = snc.getNodeMessage();
            final int inCount = snc.getNrInPorts();
            NodeID sncID = snc.getID();
            NodeOutPort[] predPorts = assemblePredecessorOutPorts(sncID);
            final PortObjectSpec[] inSpecs = new PortObjectSpec[inCount];
            final FlowObjectStack[] sos = new FlowObjectStack[inCount];
            final HiLiteHandler[] hiliteHdls = new HiLiteHandler[inCount];
            // check for presence of input specs and collects inport
            // TableSpecs, FlowObjectStacks and HiLiteHandlers
            boolean allSpecsExists = true;
            for (int i = 0; i < predPorts.length; i++) {
                if (predPorts[i] != null) {
                    inSpecs[i] = predPorts[i].getPortObjectSpec();
                    sos[i] = predPorts[i].getFlowObjectStack();
                    hiliteHdls[i] = predPorts[i].getHiLiteHandler();
                    allSpecsExists &= inSpecs[i] != null;
                } else if (snc.getInPort(i).getPortType().isOptional()) {
                    // optional input, which is not connected ... ignore
                } else {
                    allSpecsExists = false;
                }
            }
            if (!allSpecsExists) {
                // only configure nodes with all Input Specs present
                // (NodeMessage did not change -- can exit here)
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
                // nodes can be EXECUTINGREMOTELY when loaded (reconnect to a
                // grid/server) -- also these nodes will be configured() on load
            case EXECUTINGREMOTELY:
                // the stack that previously would have been propagated,
                // used to track changes
                FlowObjectStack oldFOS = snc.createOutFlowObjectStack();
                // create new FlowObjectStack
                FlowObjectStack scsc;
                FlowObjectStack nodeOutgoingStack = new FlowObjectStack(sncID);
                boolean flowStackConflict = false;
                if (isSourceNode(sncID)) {
                    // no input ports - create new stack, prefilled with
                    // Workflow variables:
                    scsc = new FlowObjectStack(sncID,
                            getWorkflowVariableStack());
                } else {
                    try {
                        scsc = new FlowObjectStack(sncID, sos);
                    } catch (IllegalFlowObjectStackException e) {
                        LOGGER.warn("Unable to merge flow object stacks: "
                                + e.getMessage(), e);
                        scsc = new FlowObjectStack(sncID);
                        flowStackConflict = true;
                    }
                }
                if (snc.getLoopRole().equals(LoopRole.BEGIN)) {
                    // the stack will automatically add the ID of the
                    // head of the loop (the owner!)
                    FlowLoopContext slc = new FlowLoopContext();
                    nodeOutgoingStack.push(slc);
                }
                snc.setFlowObjectStack(scsc, nodeOutgoingStack);
                snc.setCredentialsStore(m_credentialsStore);
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
                if (flowStackConflict) {
                    // can't configured due to stack clash
                    // make sure execution from here on is canceled
                    disableNodeForExecution(sncID);
                    // and reset node if it's not reset already
                    // (ought to be red with this type of error!)
                    if (!snc.getState().equals(State.IDLE)) {
                        // if not already idle make sure it is!
                        snc.reset();
                    }
                    // report the problem
                    snc.setNodeMessage(new NodeMessage(Type.ERROR,
                            "Can't merge FlowVariable Stacks! (likely "
                            + "a loop problem.)"));
                    // different outputs - empty ports!
                    outputSpecsChanged = true;
                } else {
                    outputSpecsChanged = snc.configure(inSpecs);
                }
                // NOTE:
                // no need to clean stacks of LoopEnd nodes - done automagically
                // inside the getFlowObjectStack of the ports of LoopEnd
                // Nodes.

                // check if FlowObjectStacks have changed
                boolean stackChanged = false;
                FlowObjectStack newFOS = snc.createOutFlowObjectStack();
                stackChanged = !newFOS.equals(oldFOS);
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
// FIXME: report errors again, once configure follows only ports, not nodes.
                LOGGER.debug("configure found EXECUTED node: "
                        + snc.getNameWithID());
                break;
            case PREEXECUTE:
            case POSTEXECUTE:
            case EXECUTING:
                // should not happen but could if reset has worked on slightly
                // different nodes than configure, for instance.
                LOGGER.debug("configure found " + snc.getState() + " node: "
                        + snc.getNameWithID());
                break;
            case QUEUED:
                // should not happen but could if reset has worked on slightly
                // different nodes than configure, for instance.
                LOGGER.debug("configure found QUEUED node: "
                        + snc.getNameWithID());
                break;
            default:
                LOGGER.error("configure found weird state (" + snc.getState()
                        + "): " + snc.getNameWithID());
            }
            if (keepNodeMessage) {
                NodeMessage newMessage = snc.getNodeMessage();
                if (!oldMessage.equals(newMessage))  {
                    newMessage = NodeMessage.merge(oldMessage, newMessage);
                    snc.setNodeMessage(newMessage);
                }
            }
        }
        return configurationChanged;
        // we have a problem here. Subsequent metanodes with through connections
        // need to be configured no matter what - they can change their state
        // because 3 nodes before in the pipeline the execute state changed...
//        return configurationChanged == configurationChanged;
    }

    /** Configure the nodes in WorkflowManager, connected to a specific port.
     * If index == -1, configure all nodes.
     * Note that this routine does NOT configure any nodes connected in
     * the parent WFM.
     *
     * @param wfm the WorkflowManager
     * @param inportIndex index of incoming port (or -1 if not known)
     */
    private void configureNodesConnectedToPortInWFM(final int inportIndex) {
        synchronized (m_workflowMutex) {
            // configure node only if it's not yet completely executed.
            // we can not avoid this: WFM with only WFM_THROUGH connections
            // will act as "EXECUTED" after reset and hence not configure.
//            if (this.getState().equals(State.EXECUTED)) {
//                return;
//            }
            // TODO: we can put our own
            // objects onto the stack here (to clean up later)?
            LOGGER.debug("Attempting to configure meta node " + this.getID()
                    + " port " + inportIndex + " successors.");
            LOGGER.debug("List=" + m_workflow.getConnectionsBySource(getID()));
            for (ConnectionContainer cc
                                : m_workflow.getConnectionsBySource(getID())) {
                if ((inportIndex < 0) || (cc.getSourcePort() == inportIndex)) {
                    NodeID succNode = cc.getDest();
                    if (!cc.getType().isLeavingWorkflow()) {
                        LOGGER.debug("Attempting to configure node "
                                + succNode);
                        configureNodeAndPortSuccessors(succNode, null, true, false);
                    }
                }
            }
            // and finalize stuff
            checkForNodeStateChanges(true);
            // TODO: clean up flow object stack after we leave WFM?
        }
    }

    /**
     * Configure node and, if this node's output specs have changed
     * also configure its successors.
     *
     * @param id of node to configure
     * @param configureMyself true if the node itself is to be configured
     */
    private void configureNodeAndSuccessors(final NodeID nodeId,
            final boolean configureMyself) {
        configureNodeAndPortSuccessors(nodeId, null, configureMyself, true);
    }

    /**
     * Configure node (depending on flag) and successors of specific ports
     * of that node.
     *
     * @param id of node to configure
     * @param ports indices of output port successors are connected to (null if
     *   all are to be used).
     * @param configureMyself true if the node itself is to be configured
     * @param configurePartent true also the parent is configured (if affected)
     */
    private void configureNodeAndPortSuccessors(final NodeID nodeId,
            final Set<Integer> ports,
            final boolean configureMyself,
            final boolean configureParent) {
        // ensure we always configured ALL successors if we configure the node
        assert (!configureMyself) || (ports == null);

        // FIXME: actually consider port index!!

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
                    if (configureSingleNodeContainer((SingleNodeContainer)nc,
                            /*keepNodeMessage=*/false)) {
                        freshlyConfiguredNodes.add(nc.getID());
                    }
                } else {
                    assert nc instanceof WorkflowManager;
                    ((WorkflowManager)nc).configureNodesConnectedToPortInWFM(-1);
                    freshlyConfiguredNodes.add(nc.getID());
                }
            }
        }
        // make sure internal status changes are properly reflected
        checkForNodeStateChanges(true);
        // And finally clean up: if the WFM was part of the list of nodes
        // make sure we only configure nodes actually connected to ports
        // which are connected to nodes which we did configure!
        if (configureParent && wfmIsPartOfList) {
            Set<Integer> portsToConf = new LinkedHashSet<Integer>();
            for (int i = 0; i < getNrWorkflowOutgoingPorts(); i++) {
                for (ConnectionContainer cc : m_workflow.getConnectionsByDest(
                        this.getID())) {
                    assert cc.getType().isLeavingWorkflow();
                    if ((cc.getDestPort() == i)
                        && (freshlyConfiguredNodes.contains(cc.getSource()))) {
                        portsToConf.add(i);
                    }
                }
            }
            if (!portsToConf.isEmpty()) {
                getParent().configureNodeAndPortSuccessors(this.getID(),
                        portsToConf, false, configureParent);
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
                        build.append(nc.toString());
                        if (nc.isDirty()) {
                            build.append("*");
                        }
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

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "(WFM) " + super.toString();
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

    /** Does the workflow contain a node with the argument id?
     * @param id The id in question.
     * @return true if there is node with the given id, false otherwise.
     */
    public boolean containsNodeContainer(final NodeID id) {
        return m_workflow.getNode(id) != null;
    }

    /**
     * @return list of errors messages (list empty if none exist).
     */
    public List<NodeMessage> getNodeErrorMessages() {
        ArrayList<NodeMessage> result = new ArrayList<NodeMessage>();
        for (NodeContainer nc : m_workflow.getNodeValues()) {
            if (nc instanceof SingleNodeContainer) {
                if (nc.getNodeMessage().getMessageType()
                        .equals(NodeMessage.Type.ERROR)) {
                    result.add(nc.getNodeMessage());
                }
            } else {
                assert nc instanceof WorkflowManager;
                List<NodeMessage> subResult
                        = ((WorkflowManager)nc).getNodeErrorMessages();
                result.addAll(subResult);
            }
        }
        return result;
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

    // bug fix 1810, nofify children about possible job manager changes
    /** {@inheritDoc} */
    @Override
    protected void notifyJobManagerChangedListener() {
        super.notifyJobManagerChangedListener();
        // TODO protect for intermediate changes to the node list
        // TODO only notify affected children
        for (NodeContainer nc : getNodeContainers()) {
            nc.notifyJobManagerChangedListener();
        }
    }

    /** {@inheritDoc} */
    @Override
    protected NodeContainerPersistor getCopyPersistor(
            final HashMap<Integer, ContainerTable> tableRep,
            final boolean preserveDeletableFlags,
            final boolean isUndoableDeleteCommand) {
        return new CopyWorkflowPersistor(this, tableRep,
                preserveDeletableFlags, isUndoableDeleteCommand);
    }

    //////////////////////////////////////
    // copy & paste
    //////////////////////////////////////

    /** Copies the nodes with the given ids from the argument workflow manager
     * into this wfm instance. All nodes wil be reset (and configured id
     * possible). Connections among the nodes are kept.
     * @param sourceManager The wfm to copy from
     * @param content The content to copy (must exist in sourceManager)
     * @return Inserted NodeIDs and annotations.
     */
    public WorkflowCopyContent copyFromAndPasteHere(
            final WorkflowManager sourceManager,
            final WorkflowCopyContent content) {
        WorkflowPersistor copyPersistor = sourceManager.copy(content);
        return paste(copyPersistor);
    }

    /** Copy the given content.
     * @param content The content to copy (must exist).
     * @return A workflow persistor hosting the node templates, ready to be
     * used in the {@link #paste(WorkflowPersistor)} method.
     */
    public WorkflowPersistor copy(final WorkflowCopyContent content) {
        return copy(false, content);
    }

    /** Copy the nodes with the given ids.
     * @param isUndoableDeleteCommand True if the returned persistor is used
     * in the delete command (which supports undo). This has two effects:
     * <ol>
     *   <li>It keeps the locations of the node's directories (e.g.
     *   &lt;workflow>/File Reader (#xy)/). This is true if the copy serves
     *   as backup of an undoable delete command (undoable = undo enabled).
     *   If it is undone, the directories must not be cleared before the
     *   next save (in order to keep the drop folder)
     *   </li>
     *   <li>The returned persistor will insert a reference to the contained
     *   workflow annotations instead of copying them (enables undo on previous
     *   move or edit commands.
     *   </li>
     * </ol>
     * @param content The content to copy (must exist).
     * @return A workflow persistor hosting the node templates, ready to be
     * used in the {@link #paste(WorkflowPersistor)} method.
     */
    public WorkflowPersistor copy(final boolean isUndoableDeleteCommand,
            final WorkflowCopyContent content) {
        NodeID[] nodeIDs = content.getNodeIDs();
        HashSet<NodeID> idsHashed = new HashSet<NodeID>(Arrays.asList(nodeIDs));
        if (idsHashed.size() != nodeIDs.length) {
            throw new IllegalArgumentException(
                    "argument list contains duplicates");
        }
        Map<Integer, NodeContainerPersistor> loaderMap =
            new LinkedHashMap<Integer, NodeContainerPersistor>();
        Set<ConnectionContainerTemplate> connTemplates =
            new HashSet<ConnectionContainerTemplate>();
        synchronized (m_workflowMutex) {
            for (int i = 0; i < nodeIDs.length; i++) {
                // throws exception if not present in workflow
                NodeContainer cont = getNodeContainer(nodeIDs[i]);
                loaderMap.put(cont.getID().getIndex(), cont.getCopyPersistor(
                      m_globalTableRepository, false, isUndoableDeleteCommand));
                for (ConnectionContainer out
                        : m_workflow.getConnectionsBySource(nodeIDs[i])) {
                    if (idsHashed.contains(out.getDest())) {
                        connTemplates.add(
                                new ConnectionContainerTemplate(out, false));
                    }
                }
            }
            return new PasteWorkflowContentPersistor(loaderMap, connTemplates,
                    content.getAnnotations(), isUndoableDeleteCommand);
        }
    }

    /** Pastes the contents of the argument persistor into this wfm.
     * @param persistor The persistor created with
     * {@link #copy(WorkflowCopyContent)} method.
     * @return The new node ids of the inserted nodes and the annotations in a
     *         dedicated object.
     */
    public WorkflowCopyContent paste(final WorkflowPersistor persistor) {
        synchronized (m_workflowMutex) {
            try {
                return loadContent(persistor,
                        new HashMap<Integer, BufferedDataTable>(),
                        new FlowObjectStack(getID()), new ExecutionMonitor(),
                        new LoadResult("Paste into Workflow"), false);
            } catch (CanceledExecutionException e) {
                throw new IllegalStateException("Cancelation although no access"
                        + " on execution monitor");
            }
        }
    }

    ///////////////////////////////
    ///////// LOAD & SAVE /////////
    ///////////////////////////////


    /** Get working folder associated with this WFM. May be null if
     * not saved yet.
     * @return working directory.
     */
    public ReferencedFile getWorkingDir() {
        return getNodeContainerDirectory();
    }

    /** Workflow version, indicates the "oldest"
      * version that is compatible to the current workflow format. */
    static final String CFG_VERSION = "version";
    /** Version of KNIME that has written the workflow. */
    static final String CFG_CREATED_BY = "created_by";

    public static WorkflowLoadResult loadProject(final File directory,
            final ExecutionMonitor exec, final WorkflowLoadHelper loadHelper)
            throws IOException, InvalidSettingsException,
            CanceledExecutionException, UnsupportedWorkflowVersionException,
            LockFailedException {
        return ROOT.load(directory, exec, loadHelper, false);
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowExecutionResult createExecutionResult(
            final ExecutionMonitor exec) throws CanceledExecutionException {
        synchronized (m_nodeMutex) {
            WorkflowExecutionResult result =
                new WorkflowExecutionResult(getID());
            super.saveExecutionResult(result);
            Set<NodeID> bfsSortedSet = m_workflow.createBreadthFirstSortedList(
                    m_workflow.getNodeIDs(), true).keySet();
            boolean success = false;
            for (NodeID id : bfsSortedSet) {
                NodeContainer nc = getNodeContainer(id);
                exec.setMessage(nc.getNameWithID());
                ExecutionMonitor subExec = exec.createSubProgress(
                        1.0 / bfsSortedSet.size());
                NodeContainerExecutionResult subResult =
                    getNodeContainer(id).createExecutionResult(subExec);
                if (subResult.isSuccess()) {
                    success = true;
                }
                result.addNodeExecutionResult(id, subResult);
            }
            // if at least one child was an success, this is also a success for
            // this node; force it -- otherwise take old success flag
            // (important for no-child workflows)
            if (success) {
                result.setSuccess(true);
            }
            return result;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void loadExecutionResult(
            final NodeContainerExecutionResult result,
            final ExecutionMonitor exec, final LoadResult loadResult) {
        if (result == null) {
            throw new IllegalArgumentException(
            "Workflow result must not be null");
        } else if (!(result instanceof WorkflowExecutionResult)) {
            throw new IllegalArgumentException("Argument must be instance "
                    + "of \"" + WorkflowExecutionResult.class.getSimpleName()
                    + "\": " + result.getClass().getSimpleName());
        }
        WorkflowExecutionResult r = (WorkflowExecutionResult)result;
        synchronized (m_workflowMutex) {
            super.loadExecutionResult(result, exec, loadResult);
            Map<NodeID, NodeContainerExecutionResult> map =
                r.getExecutionResultMap();
            final int count = map.size();
            // contains the corrected NodeID in this workflow (the node ids in
            // the execution result refer to the base id of the remote workflow)
            Map<NodeID, NodeID> transMap = new HashMap<NodeID, NodeID>();
            NodeID otherIDPrefix = r.getBaseID();
            for (NodeID otherID : map.keySet()) {
                assert otherID.hasSamePrefix(otherIDPrefix);
                transMap.put(new NodeID(getID(), otherID.getIndex()), otherID);
            }
            for (NodeID id : m_workflow.createBreadthFirstSortedList(
                    transMap.keySet(), true).keySet()) {
                NodeID otherID = transMap.get(id);
                NodeContainer nc = m_workflow.getNode(id);
                NodeContainerExecutionResult exResult = map.get(otherID);
                if (exResult == null) {
                    loadResult.addError("No execution result for node "
                            + nc.getNameWithID());
                    continue;
                }
                exec.setMessage(nc.getNameWithID());
                ExecutionMonitor subExec = exec.createSubProgress(1.0 / count);
                nc.loadExecutionResult(exResult, subExec, loadResult);
                subExec.setProgress(1.0);
            }
        }
    }

    public static WorkflowPersistorVersion1xx createLoadPersistor(
            final File directory, final WorkflowLoadHelper loadHelper)
            throws IOException, UnsupportedWorkflowVersionException {
        if (directory == null) {
            throw new NullPointerException("Arguments must not be null.");
        }
        if (!directory.isDirectory() || !directory.canRead()) {
            throw new IOException("Can't read directory " + directory);
        }

        ReferencedFile workflowknimeRef = new ReferencedFile(
                new ReferencedFile(directory), WorkflowPersistor.WORKFLOW_FILE);
        File workflowknime = workflowknimeRef.getFile();
        if (!workflowknime.isFile()) {
            throw new IOException("No \"" + WorkflowPersistor.WORKFLOW_FILE
                    + "\" file in directory \"" + directory.getAbsolutePath()
                    + "\"");
        }
        if (!directory.canWrite()) {
            throw new IOException("Can't load workflow from a directory "
                    + "without write permissions. (Required to lock workflow.) "
                    + "Location: " + directory.getAbsolutePath());
        }

        WorkflowLoadHelper lh = loadHelper != null
        ? loadHelper : WorkflowLoadHelper.INSTANCE;
        NodeSettingsRO settings =
            NodeSettings.loadFromXML(new BufferedInputStream(
                    new FileInputStream(workflowknime)));
        // CeBIT 2006 version did not contain a version string.
        String version;
        String versionString;
        if (settings.containsKey(CFG_VERSION)) {
            try {
                version = settings.getString(CFG_VERSION);
            } catch (InvalidSettingsException e) {
                throw new IOException("Can't read version number from \""
                        + workflowknime.getAbsolutePath() + "\"", e);
            }
            versionString = version;
        } else {
            version = "0.9.0";
            versionString = "<unknown>";
        }

        if (version == null) {
            throw new UnsupportedWorkflowVersionException(
            "Refuse to load workflow: Workflow version not available.");
        }
        WorkflowPersistorVersion1xx persistor;
        // TODO only create new hash map if workflow is a project?
        HashMap<Integer, ContainerTable> tableRep =
            new HashMap<Integer, ContainerTable>();
        if (WorkflowPersistorVersion200.canReadVersion(version)) {
            persistor = new WorkflowPersistorVersion200(
                    tableRep, workflowknimeRef, lh, version);
        } else if (WorkflowPersistorVersion1xx.canReadVersion(version)) {
            LOGGER.warn("The current KNIME version (" + KNIMEConstants.VERSION
                    + ") is different from the one that created the"
                    + " workflow (" + version
                    + ") you are trying to load. In some rare cases, it"
                    + " might not be possible to load all data"
                    + " or some nodes can't be configured."
                    + " Please re-configure and/or re-execute these nodes.");
            persistor = new WorkflowPersistorVersion1xx(
                    tableRep, workflowknimeRef, lh, version);
        } else {
            StringBuilder versionDetails = new StringBuilder(versionString);
            String createdBy = settings.getString(CFG_CREATED_BY, null);
            if (createdBy != null) {
                versionDetails.append(" (created by KNIME ");
                versionDetails.append(createdBy).append(")");
            }
            String v = versionDetails.toString();
            switch (lh.getUnknownKNIMEVersionLoadPolicy(v)) {
            case Abort:
                throw new UnsupportedWorkflowVersionException(
                        "Unable to load workflow, version string \"" + v
                        + "\" is unknown");
            default:
                version = WorkflowPersistorVersion200.VERSION_LATEST;
                persistor = new WorkflowPersistorVersion200(
                        tableRep, workflowknimeRef, lh, version);
            }
        }
        return persistor;
    }

    /**
     * Loads the workflow contained in the directory as node into this workflow
     * instance. Loading a whole new project is usually done using
     * {@link WorkflowManager#loadProject(File, ExecutionMonitor, WorkflowLoadHelper)}
     * .
     *
     * @param directory to load from
     * @param exec For progress/cancellation (currently not supported)
     * @param loadHelper callback to load credentials and such (if available)
     *            during load of the underlying <code>SingleNodeContainer</code>
     *            (may be null).
     * @param keepNodeMessages Whether to keep the messages that are associated
     *            with the nodes in the loaded workflow (mostly false but true
     *            when remotely computed results are loaded).
     * @return A workflow load result, which also contains the loaded workflow.
     * @throws IOException If errors reading the "important" files fails due to
     *             I/O problems (file not present, e.g.)
     * @throws InvalidSettingsException If parsing the "important" files fails.
     * @throws CanceledExecutionException If canceled.
     * @throws UnsupportedWorkflowVersionException If the version of the
     *             workflow is unknown (future version)
     * @throws LockFailedException if the flow can't be locked for opening
     */
    public WorkflowLoadResult load(final File directory,
            final ExecutionMonitor exec, final WorkflowLoadHelper loadHelper,
            final boolean keepNodeMessages) throws IOException,
            InvalidSettingsException, CanceledExecutionException,
            UnsupportedWorkflowVersionException, LockFailedException {
        ReferencedFile rootFile = new ReferencedFile(directory);
        boolean isTemplate =
                    (loadHelper != null && loadHelper.isTemplateFlow());
        if (!isTemplate) {
            // don't lock read-only templates (as we don't have r/o locks yet)
            if (!rootFile.fileLockRootForVM()) {
                throw new LockFailedException("Unable to lock workflow from \""
                        + rootFile
                        + "\". It is in use by another user/instance.");
            }
        }
        try {
            WorkflowPersistorVersion1xx persistor =
                createLoadPersistor(directory, loadHelper);
            return load(persistor, exec, keepNodeMessages);
        } finally {
            if (!isTemplate) {
                rootFile.fileUnlockRootForVM();
            }
        }
    }

    /**
     * Loads the workflow contained in the directory as node into this workflow
     * instance. Loading a whole new project is usually done using
     * {@link WorkflowManager#loadProject(File, ExecutionMonitor, WorkflowLoadHelper)}
     * .
     *
     * @param directory to load from
     * @param exec For progress/cancellation (currently not supported)
     * @param loadHelper callback to load credentials and such (if available)
     *            during load of the underlying <code>SingleNodeContainer</code>
     *            (may be null).
     * @param keepNodeMessages Whether to keep the messages that are associated
     *            with the nodes in the loaded workflow (mostly false but true
     *            when remotely computed results are loaded).
     * @return A workflow load result, which also contains the loaded workflow.
     * @throws IOException If errors reading the "important" files fails due to
     *             I/O problems (file not present, e.g.)
     * @throws InvalidSettingsException If parsing the "important" files fails.
     * @throws CanceledExecutionException If canceled.
     * @throws UnsupportedWorkflowVersionException If the version of the
     *             workflow is unknown (future version)
     */

    public WorkflowLoadResult load(final WorkflowPersistorVersion1xx persistor,
            final ExecutionMonitor exec, final boolean keepNodeMessages)
    throws IOException, InvalidSettingsException, CanceledExecutionException,
            UnsupportedWorkflowVersionException {
        final ReferencedFile refDirectory =
                persistor.getMetaPersistor().getNodeContainerDirectory();
        File directory = refDirectory.getFile();
        final String dirName = directory.getName();
        exec.setMessage("Loading workflow structure from \""
                + refDirectory + "\"");

        String versionString = persistor.getLoadVersionString();
        LOGGER.debug("Loading workflow from \"" + refDirectory
                + "\" (version \"" + versionString + "\" with loader class \""
                + persistor.getClass().getSimpleName() + "\")");
        // data files are loaded using a repository of reference tables;
        Map<Integer, BufferedDataTable> tblRep =
            new HashMap<Integer, BufferedDataTable>();
        WorkflowLoadResult result = new WorkflowLoadResult(dirName);
        persistor.preLoadNodeContainer(null, result);
        WorkflowManager manager = null;
        boolean fixDataLoadProblems = false;
        boolean isIsolatedProject =
            persistor.getInPortTemplates().length == 0
            && persistor.getOutPortTemplates().length == 0;
        InsertWorkflowPersistor insertPersistor =
            new InsertWorkflowPersistor(persistor);
        Object mutex = isIsolatedProject ? new Object() : m_workflowMutex;
        synchronized (mutex) {
            m_loadVersion = persistor.getLoadVersionString();
            NodeID[] newIDs =
                loadContent(insertPersistor, tblRep, null, exec, result,
                        keepNodeMessages).getNodeIDs();
            if (newIDs.length != 1) {
                throw new InvalidSettingsException("Loading workflow failed, "
                        + "couldn't identify child sub flow (typically "
                        + "a project)");
            }
            manager = (WorkflowManager)getNodeContainer(newIDs[0]);
            // if all errors during the load process are related to data loading
            // it might be that the flow is ex/imported without data;
            // check for it and silently overwrite the workflow
            switch (result.getType()) {
            case DataLoadError:
                if (!persistor.mustWarnOnDataLoadError()) {
                    LOGGER.debug("Workflow was apparently ex/imported without "
                        + "data, silently fixing states and writing changes");
                    try {
                        manager.save(directory, new ExecutionMonitor(), true);
                        fixDataLoadProblems = true;
                    } catch (Throwable t) {
                        LOGGER.warn("Failed in an attempt to write workflow to "
                                + "file (workflow was ex/imported without "
                                + "data; could not write the "
                                + "\"corrected\" flow.)", t);
                    }
                }
                break;
            default:
                // errors are handled elsewhere
            }
        }
        exec.setProgress(1.0);
        result.setWorkflowManager(manager);
        result.setGUIMustReportDataLoadErrors(persistor
                .mustWarnOnDataLoadError());
        StringBuilder message = new StringBuilder("Loaded workflow from \"");
        message.append(directory.getAbsolutePath()).append("\" ");
        switch (result.getType()) {
        case Ok:
            message.append(" with no errors");
            break;
        case Warning:
            message.append(" with warnings");
            break;
        case DataLoadError:
            message.append(" with errors during data load. ");
            if (fixDataLoadProblems) {
                message.append("Problems were fixed and (silently) saved.");
            } else {
                message.append("Problems were fixed but not saved!");
            }
            break;
        case Error:
            message.append(" with errors");
            break;
        default:
            message.append("with ").append(result.getType());
        }
        LOGGER.debug(message.toString());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    WorkflowCopyContent loadContent(final NodeContainerPersistor nodePersistor,
            final Map<Integer, BufferedDataTable> tblRep,
            final FlowObjectStack ignoredStack, final ExecutionMonitor exec,
            final LoadResult loadResult, final boolean preserveNodeMessage)
        throws CanceledExecutionException {
        if (!(nodePersistor instanceof WorkflowPersistor)) {
            throw new IllegalStateException("Expected "
                    + WorkflowPersistor.class.getSimpleName()
                    + " persistor object, got "
                    + nodePersistor.getClass().getSimpleName());
        }
        WorkflowPersistor persistor = (WorkflowPersistor)nodePersistor;
        assert this != ROOT || persistor.getConnectionSet().isEmpty()
        : "ROOT workflow has no connections: " + persistor.getConnectionSet();
        LinkedHashMap<NodeID, NodeContainerPersistor> persistorMap =
            new LinkedHashMap<NodeID, NodeContainerPersistor>();
        Map<Integer, NodeContainerPersistor> nodeLoaderMap =
            persistor.getNodeLoaderMap();
        exec.setMessage("annotations");
        List<WorkflowAnnotation> annos = persistor.getWorkflowAnnotations();
        for (WorkflowAnnotation w : annos) {
            addWorkflowAnnotationInternal(w);
        }
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
        postLoad(persistorMap, tblRep, persistor.mustWarnOnDataLoadError(),
                exec, loadResult, preserveNodeMessage);
        // set dirty if this wm should be reset (for instance when the state
        // of the workflow can't be properly read from the workflow.knime)
        if (persistor.needsResetAfterLoad() || persistor.isDirtyAfterLoad()) {
            setDirty();
        }
        m_deletedNodesFileLocations.addAll(
                persistor.getObsoleteNodeDirectories());
        Collection<NodeID> resultColl = persistorMap.keySet();
        NodeID[] newIDs = resultColl.toArray(new NodeID[resultColl.size()]);
        WorkflowAnnotation[] newAnnotations =
            annos.toArray(new WorkflowAnnotation[annos.size()]);
        WorkflowCopyContent result = new WorkflowCopyContent();
        result.setAnnotation(newAnnotations);
        result.setNodeIDs(newIDs);
        return result;
    }

    private void postLoad(
            final Map<NodeID, NodeContainerPersistor> persistorMap,
            final Map<Integer, BufferedDataTable> tblRep,
            final boolean mustWarnOnDataLoadError, final ExecutionMonitor exec,
            final LoadResult loadResult, final boolean keepNodeMessage)
    throws CanceledExecutionException {
        // linked set because we need reverse order later on
        Collection<NodeID> failedNodes = new LinkedHashSet<NodeID>();
        boolean isStateChangePredictable = false;
        for (NodeID bfsID : m_workflow.createBreadthFirstSortedList(
                        persistorMap.keySet(), true).keySet()) {
            NodeContainer cont = getNodeContainer(bfsID);
            // initialize node container with CredentialsStore
            if (cont instanceof SingleNodeContainer) {
                SingleNodeContainer snc = (SingleNodeContainer) cont;
                snc.setCredentialsStore(m_credentialsStore);
            }
            LoadResult subResult = new LoadResult(cont.getNameWithID());
            boolean isFullyConnected = isFullyConnected(bfsID);
            boolean needsReset;
            switch (cont.getState()) {
            case IDLE:
            case UNCONFIGURED_MARKEDFOREXEC:
                needsReset = false;
                break;
            default:
                // we reset everything which is not fully connected
                needsReset = !isFullyConnected;
            break;
            }
            NodeOutPort[] predPorts = assemblePredecessorOutPorts(bfsID);
            final int predCount = predPorts.length;
            PortObject[] portObjects = new PortObject[predCount];
            boolean inPortsContainNull = false;
            FlowObjectStack[] predStacks = new FlowObjectStack[predCount];
            for (int i = 0; i < predCount; i++) {
                NodeOutPort p = predPorts[i];
                if (cont instanceof SingleNodeContainer && p != null) {
                    SingleNodeContainer snc = (SingleNodeContainer) cont;
                    snc.setInHiLiteHandler(i, p.getHiLiteHandler());
                }
                if (p != null) {
                    predStacks[i] = p.getFlowObjectStack();
                    portObjects[i] = p.getPortObject();
                    inPortsContainNull &= portObjects[i] == null;
                }
            }
            FlowObjectStack inStack;
            try {
                if (isSourceNode(bfsID)) {
                    predStacks =
                        new FlowObjectStack[]{getWorkflowVariableStack()};
                }
                inStack = new FlowObjectStack(cont.getID(), predStacks);
            } catch (IllegalFlowObjectStackException ex) {
                subResult.addError("Errors creating flow object stack for "
                        + "node \"" + cont.getNameWithID() + "\", (resetting "
                        + "flow variables): " + ex.getMessage());
                needsReset = true;
                inStack = new FlowObjectStack(cont.getID());
            }
            NodeContainerPersistor persistor = persistorMap.get(bfsID);
            State loadState = persistor.getMetaPersistor().getState();
            exec.setMessage(cont.getNameWithID());
            // two steps below: loadNodeContainer and loadContent
            ExecutionMonitor sub1 =
                exec.createSubProgress(1.0 / (2 * m_workflow.getNrNodes()));
            ExecutionMonitor sub2 =
                exec.createSubProgress(1.0 / (2 * m_workflow.getNrNodes()));
            try {
                persistor.loadNodeContainer(tblRep, sub1, subResult);
            } catch (CanceledExecutionException e) {
                throw e;
            } catch (Exception e) {
                if (!(e instanceof InvalidSettingsException)
                        && !(e instanceof IOException)) {
                    LOGGER.error("Caught unexpected \""
                            + e.getClass().getSimpleName()
                            + "\" during node loading", e);
                }
                subResult.addError("Errors loading, skipping it: "
                        + e.getMessage());
                needsReset = true;
            }
            sub1.setProgress(1.0);
            // if cont == isolated meta nodes, then we need to block that meta
            // node as well (that is being asserted in methods which get called
            // indirectly)
            Object mutex = cont instanceof WorkflowManager
                ? ((WorkflowManager)cont).m_workflowMutex : m_workflowMutex;
            synchronized (mutex) {
                cont.loadContent(persistor, tblRep, inStack,
                        sub2, subResult, keepNodeMessage);
            }
            sub2.setProgress(1.0);
            if (persistor.isDirtyAfterLoad()) {
                cont.setDirty();
            }
            boolean hasPredecessorFailed = false;
            for (ConnectionContainer cc
                    : m_workflow.getConnectionsByDest(bfsID)) {
                NodeID s = cc.getSource();
                if (s.equals(getID())) {
                    continue; // don't consider WFM_IN connections
                }
                if (failedNodes.contains(s)) {
                    hasPredecessorFailed = true;
                }
            }
            needsReset |= persistor.needsResetAfterLoad();
            needsReset |= hasPredecessorFailed;
            boolean isExecuted = cont.getState().equals(State.EXECUTED);
            boolean remoteExec = persistor.getMetaPersistor()
                .getExecutionJobSettings() != null;

            // if node is executed and some input data is missing we need
            // to reset that node as there is obviously a conflict (e.g.
            // predecessors has been loaded as IDLE
            if (!needsReset && isExecuted && inPortsContainNull) {
                needsReset = true;
                subResult.addError("Predecessor ports have no data", true);
            }
            if (needsReset && cont instanceof SingleNodeContainer
                    && cont.isResetable()) {
                // we don't care for successors because they are not loaded yet
                invokeResetOnSingleNodeContainer((SingleNodeContainer)cont);
                isExecuted = false;
            }
            if (needsReset) {
                failedNodes.add(bfsID);
            }
            if (!isExecuted && cont instanceof SingleNodeContainer) {
                configureSingleNodeContainer((SingleNodeContainer)cont,
                        keepNodeMessage);
            }
            if (persistor.mustComplainIfStateDoesNotMatch()
                    && !cont.getState().equals(loadState)
                    && !hasPredecessorFailed) {
                isStateChangePredictable = true;
                String warning = "State has changed from "
                    + loadState + " to " + cont.getState();
                switch (subResult.getType()) {
                case DataLoadError:
                    // data load errors cause state changes
                    subResult.addError(warning, true);
                    break;
                default:
                    subResult.addWarning(warning);
                }
                cont.setDirty();
            }
            // saved in executing state (e.g. grid job), request to reconnect
            if (remoteExec) {
                if (needsReset) {
                    subResult.addError("Can't continue execution "
                            + "due to load errors");
                }
                if (inPortsContainNull) {
                    subResult.addError(
                            "Can't continue execution; no data in inport");
                }
                if (!cont.getState().equals(State.EXECUTINGREMOTELY)) {
                    subResult.addError("Can't continue execution; node is not "
                            + "configured but " + cont.getState());
                }
                try {
                    if (!continueExecutionOnLoad(cont, persistor)) {
                        cont.cancelExecution();
                        cont.setDirty();
                        subResult.addError(
                                "Can't continue execution; unknown reason");
                    }
                } catch (Exception exc) {
                    StringBuilder error = new StringBuilder(
                            "Can't continue execution");
                    if (exc instanceof NodeExecutionJobReconnectException
                            || exc instanceof InvalidSettingsException) {
                        error.append(": ").append(exc.getMessage());
                    } else {
                        error.append(" due to ");
                        error.append(exc.getClass().getSimpleName());
                        error.append(": ").append(exc.getMessage());
                    }
                    LOGGER.error(error, exc);
                    cont.cancelExecution();
                    cont.setDirty();
                    subResult.addError(error.toString());
                }
            }
            loadResult.addChildError(subResult);
            // set warning message on node if we have loading errors
            // do this only if these are critical errors or data-load errors,
            // which must be reported.
            switch (subResult.getType()) {
            case Ok:
            case Warning:
                break;
            case DataLoadError:
                if (!mustWarnOnDataLoadError) {
                    break;
                }
            default:
                NodeMessage oldMessage = cont.getNodeMessage();
                StringBuilder messageBuilder =
                    new StringBuilder(oldMessage.getMessage());
                if (messageBuilder.length() != 0) {
                    messageBuilder.append("\n");
                }
                NodeMessage.Type type;
                switch (oldMessage.getMessageType()) {
                case RESET:
                case WARNING:
                    type = NodeMessage.Type.WARNING;
                    break;
                default:
                    type = NodeMessage.Type.ERROR;
                }
                messageBuilder.append(subResult.getFilteredError(
                        "", LoadResultEntryType.Warning));
                cont.setNodeMessage(
                        new NodeMessage(type, messageBuilder.toString()));
            }
        }
        if (!sweep(false) && !isStateChangePredictable) {
            loadResult.addError("Some node states were invalid");
        }
    }

    private Map<Integer, NodeID> loadNodesAndConnections(
            final Map<Integer, NodeContainerPersistor> loaderMap,
            final Set<ConnectionContainerTemplate> connections,
            final LoadResult loadResult) {
        // id suffix are made unique by using the entries in this map
        Map<Integer, NodeID> translationMap =
            new LinkedHashMap<Integer, NodeID>();

        for (Map.Entry<Integer, NodeContainerPersistor> nodeEntry
                : loaderMap.entrySet()) {
            int suffix = nodeEntry.getKey();
            NodeID subId = new NodeID(getID(), suffix);
            // the mutex may be already held here. It is not held if we load
            // a completely new project (for performance reasons when loading
            // 100+ workflows simultaneously in a cluster environment)
            synchronized (m_workflowMutex) {
                if (m_workflow.containsNodeKey(subId)) {
                    subId = createUniqueID();
                }
                NodeContainerPersistor pers = nodeEntry.getValue();
                translationMap.put(suffix, subId);
                NodeContainer container = pers.getNodeContainer(this, subId);
                NodeContainerMetaPersistor metaPersistor =
                    pers.getMetaPersistor();
                ReferencedFile ncRefDir =
                    metaPersistor.getNodeContainerDirectory();
                if (ncRefDir != null) {
                    // the nc dir is in the deleted locations list if the node
                    // was deleted and is now restored (undo)
                    m_deletedNodesFileLocations.remove(ncRefDir);
                }
                addNodeContainer(container, false);
                if (pers.isDirtyAfterLoad()) {
                    container.setDirty();
                }
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
                    source, c.getSourcePort(), dest, c.getDestPort(), true)) {
                String warn = "Unable to insert connection \"" + c + "\"";
                LOGGER.warn(warn);
                loadResult.addError(warn);
                continue;
            }
            ConnectionContainer cc = addConnection(
                    source, c.getSourcePort(), dest, c.getDestPort(), true);
            cc.setUIInfo(c.getUiInfo());
            cc.setDeletable(c.isDeletable());
            assert cc.getType().equals(type);
        }
        return translationMap;
    }

    public void save(final File directory, final ExecutionMonitor exec,
            final boolean isSaveData)
        throws IOException, CanceledExecutionException, LockFailedException {
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
                final String saveVersion =
                    WorkflowPersistorVersion200.VERSION_LATEST;
                if (m_loadVersion != null
                        && !m_loadVersion.equals(saveVersion)) {
                    LOGGER.info("Workflow was created with a previous version "
                            + "of KNIME (workflow version " + m_loadVersion
                            + "), converting to current version. This may "
                            + "take some time.");
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
                    m_loadVersion = saveVersion;
                }
                WorkflowPersistorVersion200.save(
                        this, workflowDirRef, exec, isSaveData);
            } finally {
                workflowDirRef.unlock();
            }
        }
    }

    /** Delete directories of removed nodes. This is part of the save routine to
     * commit the changes. Called from the saving persistor class */
    void deleteObsoleteNodeDirs() {
        for (ReferencedFile deletedNodeDir
                : m_deletedNodesFileLocations) {
            File f = deletedNodeDir.getFile();
            if (f.exists()) {
                if (FileUtil.deleteRecursively(f)) {
                    LOGGER.debug(
                            "Deleted obsolete node directory \""
                            + f.getAbsolutePath() + "\"");
                } else {
                    LOGGER.warn(
                            "Deletion of obsolete node directory \""
                            + f.getAbsolutePath() + "\" failed");
                }
            }
        }
        // bug fix 1857: this list must be cleared upon save
        m_deletedNodesFileLocations.clear();
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
                for (int pi = 0; pi < predPorts.length; pi++) {
                    NodeOutPort predOutPort = predPorts[pi];
                    NodeInPort inport = nc.getInPort(pi);
                    State predOutPortState;
                    if (predOutPort == null) { // unconnected
                        if (inport.getPortType().isOptional()) {
                            // optional inport -- imitate executed predecessor
                            predOutPortState = State.EXECUTED;
                        } else {
                            predOutPortState = State.IDLE;
                        }
                    } else {
                        predOutPortState = predOutPort.getNodeState();
                    }
                    switch (predOutPortState) {
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
                    case PREEXECUTE:
                    case EXECUTING:
                    case POSTEXECUTE:
                        allowedStates.retainAll(Arrays.asList(State.IDLE,
                                State.UNCONFIGURED_MARKEDFOREXEC,
                                State.CONFIGURED, State.MARKEDFOREXEC));
                        break;
                    case EXECUTINGREMOTELY:
                        // be more flexible than in the EXECUTING case
                        // EXECUTINGREMOTELY is used in meta nodes,
                        // which are executed elsewhere -- they set all nodes
                        // of their internal flow to EXECUTINGREMOTELY
                        allowedStates.retainAll(Arrays.asList(State.IDLE,
                                State.UNCONFIGURED_MARKEDFOREXEC,
                                State.CONFIGURED, State.MARKEDFOREXEC,
                                State.EXECUTINGREMOTELY));
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
                    case EXECUTINGREMOTELY:
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

    /** {@inheritDoc} */
    @Override
    protected boolean isLocalWFM() {
        return findJobManager() instanceof ThreadNodeExecutionJobManager;
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

    /** Set new name of this workflow or null to reset the name (will then
     * return the workflow directory in {@link #getName()} or null if this flow
     * has not been saved yet).
     * @param name The new name or null
     */
    public void setName(final String name) {
        if (!ConvenienceMethods.areEqual(m_name, name)) {
            setDirty();
            m_name = name;
        }
    }

    /** Renames the underlying workflow directory to the new name.
     * @param newName The name of the directory.
     * @return Whether that was successful.
     * @throws IllegalStateException If the workflow has not been saved yet
     * (has no corresponding node directory).
     */
    public boolean renameWorkflowDirectory(final String newName) {
        ReferencedFile file = getNodeContainerDirectory();
        if (file == null) {
            throw new IllegalStateException("Workflow has not been saved yet.");
        }
        return file.rename(newName);
    }

    /** Get reference to credentials store used to persist name/passwords.
     * @return password store associated with this workflow/meta-node.
     */
    public CredentialsStore getCredentialsStore() {
        return m_credentialsStore;
    }

    /** Update user/password fields in the credentials store assigned to the
     * workflow and update the node configuration.
     * @param credentialsList the list of credentials to be updated. It will
     *  find matching crendentials in this workflow and update their fields.
     * @throws IllegalArgumentException If any of the credentials is unknown
     */
    public void updateCredentials(final Credentials... credentialsList) {
        synchronized (m_workflowMutex) {
            if (getCredentialsStore().update(credentialsList)) {
                configureAllNodesInWFM(false);
            }
        }
    }

    /** Get the name of the workflow. If none has been set, a name is derived
     * from the workflow directory name. If no directory has been set, a static
     * string is returned. This method never returns null.
     * {@inheritDoc} */
    @Override
    public String getName() {
        if (m_name != null) {
            return m_name;
        }
        ReferencedFile refFile = getNodeContainerDirectory();
        if (refFile != null) {
            File file = refFile.getFile();
            String dirName = file.getName();
            if (dirName != null) {
                return dirName;
            }
        }
        return "Workflow Manager";
    }

    /** @return the name set in the constructor or via {@link #setName(String)}.
     * In comparison to {@link #getName()} this method does not use the workflow
     * directory name if no other name is set.
     */
    String getNameField() {
        return m_name;
    }

    /** {@inheritDoc} */
    @Override
    public int getNrNodeViews() {
        return 0;  // workflow managers don't have views (yet?)!
    }

    /** {@inheritDoc} */
    @Override
    public NodeView<NodeModel> getNodeView(final int i) {
        throw new IndexOutOfBoundsException("WFM don't have views.");
    }

    /** {@inheritDoc} */
    @Override
    public String getNodeViewName(final int i) {
        throw new IndexOutOfBoundsException("WFM don't have views.");
    }

    /** {@inheritDoc} */
    @Override
    void loadSettings(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        super.loadSettings(settings);
    }

    /** {@inheritDoc} */
    @Override
    void saveSettings(final NodeSettingsWO settings) {
        super.saveSettings(settings);

        // TODO: as we don't have a node model associated with the wfm, we must
        // do the same thing a dialog does when saving settings (it assumes
        // existance of a node).
//        Node.SettingsLoaderAndWriter l = new Node.SettingsLoaderAndWriter();
//        NodeSettings model = new NodeSettings("field_ignored");
//        NodeSettings variables;
//        l.setModelSettings(model);
//        l.setVariablesSettings(variables);
//        l.save(settings);
        settings.addNodeSettings("model");
        SingleNodeContainerSettings s = new SingleNodeContainerSettings();
        NodeContainerSettings ncSet = new NodeContainerSettings();
        ncSet.save(settings);
        s.save(settings);

    }

    /** {@inheritDoc} */
    @Override
    boolean areSettingsValid(final NodeSettingsRO settings) {
        return super.areSettingsValid(settings);
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
            super.cleanup();
            ReferencedFile ncDir = getNodeContainerDirectory();
            if (ncDir != null) {
                ncDir.fileUnlockRootForVM();
            }
            for (NodeContainer nc : m_workflow.getNodeValues()) {
                nc.cleanup();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setNodeContainerDirectory(final ReferencedFile directory) {
        ReferencedFile ncDir = getNodeContainerDirectory();
        if (ncDir != null) {
            ncDir.fileUnlockRootForVM();
        }
        if (!directory.fileLockRootForVM()) {
            throw new IllegalStateException("Workflow root directory \""
                    + directory + "\" can't be locked although it should have "
                    + "been locked by the save routines");
        }
        super.setNodeContainerDirectory(directory);
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

    /////////////////////////////
    // Workflow Variable handling
    /////////////////////////////

    /* Private routine which assembles a stack of workflow variables all
     * the way to the top of the workflow hierarchy.
     */
    private void pushWorkflowVariablesOnStack(final FlowObjectStack sos) {
        if (getID().equals(ROOT.getID())) {
            // reach top of tree, return
            return;
        }
        // otherwise push variables of parent...
        getParent().pushWorkflowVariablesOnStack(sos);
        // ... and then our own
        if (m_workflowVariables != null) {
            // if we have some vars, put them on stack
            for (FlowVariable sv : m_workflowVariables) {
                sos.push(sv.clone());
            }
        }
        return;
    }

    /** Get read-only access on the current workflow variables.
     * @return the current workflow variables, never null.
     */
    @SuppressWarnings("unchecked")
    public List<FlowVariable> getWorkflowVariables() {
        return m_workflowVariables == null ? Collections.EMPTY_LIST
                : Collections.unmodifiableList(m_workflowVariables);
    }

    /* @return stack of workflow variables. */
    private FlowObjectStack getWorkflowVariableStack() {
        // assemble new stack
        FlowObjectStack sos = new FlowObjectStack(getID());
        // push own variables and the ones of the parent(s):
        pushWorkflowVariablesOnStack(sos);
        return sos;
    }

    /** Set new workflow variables. All nodes within
     * this workflow will have access to these variables.
     * The method may change in future versions or removed entirely (bug 1937).
     *
     * @param newVars new variables to be set
     * @param skipReset if false the workflow will be re-configured
     */
    public void addWorkflowVariables(final boolean skipReset,
            final FlowVariable... newVars) {
        // meta node variables not supported for two reasons
        // (1) missing configure propagation and (2) meta-node variables need
        // to be hidden in outer workflow
        assert (getNrInPorts() == 0 && getNrOutPorts() == 0)
            : "Workflow variables can't be set on meta nodes";
        synchronized (m_workflowMutex) {
            if (m_workflowVariables == null) {
                // create new set of vars if none exists
                m_workflowVariables = new Vector<FlowVariable>();
            }
            for (FlowVariable sv : newVars) {
                // make sure old variables of the same name are removed first
                removeWorkflowVariable(sv.getName());
                m_workflowVariables.add(sv);
            }
            if (!skipReset) {
                // usually one needs to reset the Workflow to make sure the
                // new variable settings are used by all nodes!
                // Note that resetAll also needs to configure non-executed
                // nodes in order to spread those new variables correctly!
                resetAndReconfigureAllNodesInWFM();
            } else {
                // otherwise only configure already configured nodes. This
                // is required to make sure they rebuild their
                // FlowObjectStack!
                reconfigureAllNodesOnlyInThisWFM();
            }
            setDirty();
        }
    }

    /* -- Workflow Annotations ---------------------------------------------*/

    /** @return read-only collection of all currently registered annotations. */
    public Collection<WorkflowAnnotation> getWorkflowAnnotations() {
        return Collections.unmodifiableList(m_annotations);
    }

    /** Add new workflow annotation, fire events.
     * @param annotation to add
     * @throws IllegalArgumentException If annotation already registered. */
    /** Add new workflow annotation, fire events.
     * @param annotation to add
     * @throws IllegalArgumentException If annotation already registered. */
    public void addWorkflowAnnotation(final WorkflowAnnotation annotation) {
        addWorkflowAnnotationInternal(annotation);
        setDirty();
    }

    /** Adds annotation as in #addWorkf but does not fire dirty event. */
    private void addWorkflowAnnotationInternal(
            final WorkflowAnnotation annotation) {
        if (m_annotations.contains(annotation)) {
            throw new IllegalArgumentException("Annotation \"" + annotation
                    + "\" already exists");
        }
        m_annotations.add(annotation);
        annotation.addUIInformationListener(this);
        notifyWorkflowListeners(new WorkflowEvent(
                WorkflowEvent.Type.ANNOTATION_ADDED, null, null, annotation));
    }

    /** Remove workflow annotation, fire events.
     * @param annotation to remove
     * @throws IllegalArgumentException If annotation is not registered. */
    public void removeAnnotation(final WorkflowAnnotation annotation) {
        if (!m_annotations.remove(annotation)) {
            throw new IllegalArgumentException("Annotation \"" + annotation
                    + "\" does not exists");
        }
        annotation.removeUIInformationListener(this);
        notifyWorkflowListeners(new WorkflowEvent(
                WorkflowEvent.Type.ANNOTATION_REMOVED, null, annotation, null));
        setDirty();
    }

    /** Listener to annotations, etc; sets content dirty.
     * @param evt Change event. */
    @Override
    public void nodeUIInformationChanged(final NodeUIInformationEvent evt) {
        setDirty();
    }


    /* -------------------------------------------------------------------*/


    /**
     * Retrieves the node with the given ID, fetches the underlying
     * {@link NodeModel} and casts it to the argument class.
     * @param id The node of interest
     * @param cl The class object the underlying NodeModel needs to implement
     * @param <T> The type the class
     * @return The casted node model.
     * @throws IllegalArgumentException If the node does not exist, is not
     *         a {@link SingleNodeContainer} or the model does not implement the
     *         requested type.
     */
    public <T> T castNodeModel(final NodeID id, final Class<T> cl) {
		NodeContainer nc = getNodeContainer(id);
		if (!(nc instanceof SingleNodeContainer)) {
			throw new IllegalArgumentException("Node \"" + nc
					+ "\" not a single node container");
		}
		NodeModel model = ((SingleNodeContainer)nc).getNodeModel();
		if (!cl.isInstance(model)) {
			throw new IllegalArgumentException("Node \"" + nc
					+ "\" not instance of " + cl.getSimpleName());
		}
		return cl.cast(model);
    }

    /** Find all nodes in this workflow, whose underlying {@link NodeModel} is
     * of the requested type. Intended purpose is to allow certain extensions
     * (reporting, web service, ...) access to specialized nodes.
     * @param <T> Specific NodeModel derivation or another interface
     *            implemented by NodeModel instances.
     * @param nodeModelClass The class of interest
     * @param recurse Whether to recurse into contained meta nodes.
     * @return A (unsorted) list of nodes matching the class criterion
     */
    public <T> Map<NodeID, T> findNodes(
            final Class<T> nodeModelClass, final boolean recurse) {
        Map<NodeID, T> result = new LinkedHashMap<NodeID, T>();
        for (NodeContainer nc : m_workflow.getNodeValues()) {
            if (nc instanceof SingleNodeContainer) {
                SingleNodeContainer snc = (SingleNodeContainer)nc;
                NodeModel model = snc.getNode().getNodeModel();
                if (nodeModelClass.isAssignableFrom(model.getClass())) {
                    result.put(snc.getID(), nodeModelClass.cast(model));
                }
            }
        }
        if (recurse) { // do separately to maintain some sort of order
            for (NodeContainer nc : m_workflow.getNodeValues()) {
                if (nc instanceof WorkflowManager) {
                    result.putAll(((WorkflowManager)nc).findNodes(
                            nodeModelClass, true));
                }
            }
        }
        return result;
    }


    /** Remove workflow variable of given name.
     * The method may change in future versions or removed entirely (bug 1937).
     *
     * @param name of variable to be removed.
     */
    public void removeWorkflowVariable(final String name) {
        for (int i = 0; i < m_workflowVariables.size(); i++) {
            FlowVariable sv = m_workflowVariables.elementAt(i);
            if (sv.getName().equals(name)) {
                m_workflowVariables.remove(i);
            }
        }
    }

}
