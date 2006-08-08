/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   14.02.2005 (M. Berthold): created
 *   12.01.2006 (mb): clean up for code review
 */
package org.knime.core.node.workflow;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Future;

import org.knime.core.eclipseUtil.GlobalClassCreator;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.Node;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeInPort;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeOutPort;
import org.knime.core.node.NodeProgressMonitor;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NodeStateListener;
import org.knime.core.node.NodeStatus;
import org.knime.core.node.NodeView;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.meta.MetaNodeModel;

/**
 * Wrapper for a Node and the surrounding graph information, i.e. successors and
 * predecessors. For each InPort the node can store no more than one
 * predecessor, for each OutPort a list of successors is held. The NodeContainer
 * listens to event coming from the node and sends them to it's own listeners
 * after adding the node's ID.
 * 
 * @author M. Berthold, University of Konstanz
 * @author Thorsten Meinl, University of Konstanz
 */
public class NodeContainer implements NodeStateListener {
    /** Key for this node's user description. */
    private static final String KEY_CUSTOM_DESCRIPTION = "customDescription";

    /** Key for this node's user name. */
    private static final String KEY_CUSTOM_NAME = "customName";

    /** Key for extra info's class name. */
    private static final String KEY_EXTRAINFOCLASS = "extraInfoClassName";

    /** Key for the factory class name, used to load nodes. */
    static final String KEY_FACTORY_NAME = "factory";

    /** Key for this node's internal ID. */
    static final String KEY_ID = "id";

    private static final String KEY_IS_DELETABLE = "isDeletable";

    // The logger for static methods
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(NodeContainer.class);

    /**
     * Creates the <code>NodeExtraInfo</code> from given settings, describing
     * whatever additional information was stored (graphical layout?).
     * 
     * @param sett the setting to construct the extra info from
     * @return new <code>NodeExtraInfo</code> object or null
     * @throws InvalidSettingsException if the settings are invalid
     */
    protected static NodeExtraInfo createExtraInfo(final NodeSettingsRO sett)
            throws InvalidSettingsException {
        NodeExtraInfo extraInfo = null; // null if it doesn't exist
        if (sett.containsKey(NodeContainer.KEY_EXTRAINFOCLASS)) {
            // if it does exist, determine type of extrainfo
            String extraInfoClassName = sett
                    .getString(NodeContainer.KEY_EXTRAINFOCLASS);
            try {
                // use global Class Creator utility for Eclipse "compatibility"
                extraInfo = (NodeExtraInfo)(GlobalClassCreator
                        .createClass(extraInfoClassName).newInstance());
                // and load content of extrainfo
                extraInfo.load(sett);
            } catch (Exception e) {
                LOGGER.warn("ExtraInfoClass could not be loaded "
                        + extraInfoClassName + " reason: " + e, e);
            }
        }
        return extraInfo;
    }

    private static NodeFactory readNodeFactory(final NodeSettingsRO settings)
            throws InvalidSettingsException, InstantiationException,
            IllegalAccessException, ClassNotFoundException {
        // read node factory class name
        String factoryClassName = settings.getString(KEY_FACTORY_NAME);
        // use global Class Creator utility for Eclipse "compatibility"

        try {
            NodeFactory f = (NodeFactory)((GlobalClassCreator
                    .createClass(factoryClassName)).newInstance());
            return f;
        } catch (ClassNotFoundException ex) {
            String[] x = factoryClassName.split("\\.");
            String simpleClassName = x[x.length - 1];

            for (String s : NodeFactory.getLoadedNodeFactories()) {
                if (s.endsWith("." + simpleClassName)) {
                    NodeFactory f = (NodeFactory)((GlobalClassCreator
                            .createClass(s)).newInstance());
                    LOGGER.warn("Substituted '" + f.getClass().getName()
                            + "' for unknown factory '" + factoryClassName
                            + "'");
                    return f;
                }
            }

            throw ex;
        }
    }

    private List<NodeInPort> m_cachedInPorts;

    private List<NodeOutPort> m_cachedOutPorts;

    // A user-specified name for this node
    private String m_customName;

    // A user-specified description for this node
    private String m_description;

    // store list of listeners - essentially this Container will listen
    // to events coming from its <code>Node</code>, add the id to the
    // event and forward it.
    private final List<NodeStateListener> m_eventListeners;

    // for execution of the Node in its own Thread, hold status
    // information of the execution thread...
    private boolean m_executionRunning;

    // Also hold an object storing information about this node's
    // position on the visual representation of this workflow (or
    // other supplemental info) - if available. The NodeContainer
    // itself does not care about it but simply makes sure it is
    // stored and retrieved using the appropriate interface.
    private NodeExtraInfo m_extraInfo;

    // ...its ID
    private final int m_id;

    // The node logger for the underlying node is used here.
    private final NodeLogger m_logger;

    // remember node itself...
    private final Node m_node;

    // ...and an array of predecessors (only one per port!)
    private final Vector<NodeContainer> m_pred;

    // ...for each port a list of successors...
    private final Vector<List<NodeContainer>> m_succ;

    private boolean m_deletable = true;

    private final WorkflowManager m_wfm;

    /**
     * Create new container using a node factory and a predefined ID.
     * 
     * @param f a node factroy
     * @param wfm the workflow manager for this node container
     * @param id identifier of the node
     */
    public NodeContainer(final NodeFactory f, final WorkflowManager wfm,
            final int id) {
        this(new Node(f, wfm), wfm, id);
    }

    private NodeContainer(final Node node, final WorkflowManager wfm,
            final int id) {
        m_node = node;
        m_wfm = wfm;
        m_id = id;
        m_logger = NodeLogger.getLogger(getNameWithID());
        m_customName = "Node " + id; // initial name is the node id
        m_description = null; // no initial description
        m_succ = new Vector<List<NodeContainer>>(m_node.getNrOutPorts());
        m_pred = new Vector<NodeContainer>(m_node.getNrInPorts());
        m_succ.setSize(m_node.getNrOutPorts());
        m_pred.setSize(m_node.getNrInPorts());
        m_extraInfo = null;
        m_eventListeners = new ArrayList<NodeStateListener>();
        m_node.addStateListener(this);
    }

    /**
     * Creates a copy of the passed node container with a new id.
     * 
     * @param template the node container to copy
     * @param id the new id
     * @throws CloneNotSupportedException if the {@link NodeExtraInfo} of the
     *             template could not be cloned
     * 
     */
    NodeContainer(final NodeContainer template, final int id)
            throws CloneNotSupportedException {
        this(new Node(template.m_node, template.m_wfm), template.m_wfm, id);

        setExtraInfo((NodeExtraInfo)template.getExtraInfo().clone());
        if (template.m_customName != null) {
            setCustomName(template.m_customName);
        }
        setDescription(template.m_description);
    }

    /**
     * Creates a new NodeContainer and reads it's status and information from
     * the NodeSettings object. Note that the list of predecessors and
     * successors will NOT be initalized correctly. The Workflow manager is
     * required to take care of re-initializing the connections.
     * 
     * @param setts retrieve the data from
     * @param wfm the workflowmanager that is responsible for this node
     * @throws InvalidSettingsException if the required keys are not available
     *             in the NodeSettings
     * @throws ClassNotFoundException if the factory class in the settings could
     *             not be found
     * @throws IllegalAccessException if the factory class is not acessible
     * @throws InstantiationException if a factory object could not be created
     * 
     * @see #save(NodeSettingsWO, File, NodeProgressMonitor)
     */
    public NodeContainer(final NodeSettingsRO setts, final WorkflowManager wfm)
            throws InvalidSettingsException, InstantiationException,
            IllegalAccessException, ClassNotFoundException {
        this(readNodeFactory(setts), wfm, setts.getInt(KEY_ID));

        setExtraInfo(createExtraInfo(setts));

        try {
            // read custom name
            String name = setts.getString(KEY_CUSTOM_NAME);

            // if there was no user node name defined than keep the default name
            if (name != null) {
                setCustomName(name);
            }
        } catch (InvalidSettingsException ise) {
            LOGGER.warn("In the settings of node <id:" + getID() + "|type:"
                    + getName() + "> is no user name specified");
        }

        try {
            // read custom description
            String description = setts.getString(KEY_CUSTOM_DESCRIPTION);
            setDescription(description);
        } catch (InvalidSettingsException ise) {
            LOGGER.warn("In the settings of node <id:" + getID() + "|type:"
                    + getName() + "> is no user description specified");
        }

        m_deletable = setts.getBoolean(KEY_IS_DELETABLE, true);
    }

    /**
     * Adds an incoming connection to a specified port. Only one incoming
     * connection is allowed per port - if this port is already connected it
     * will simply overwrite the previous connection.
     * 
     * @param port index of incoming port
     * @param nc NodeContainer this connection originates at.
     * 
     */
    void addIncomingConnection(final int port, final NodeContainer nc) {
        // sanity checks:
        if (nc == null) {
            throw new NullPointerException("Incoming connection at port #"
                    + port + " has no source node defined (null).");
        }
        if (port < 0 || port > getNrInPorts()) {
            throw new IndexOutOfBoundsException("Port index out of bounds: "
                    + port);
        }
        if (m_pred.get(port) != null) {
            throw new IllegalStateException("Could not create connection "
                    + "at port #" + port + ". Port is already connected.");
        }
        // add connection only on this side.
        m_pred.set(port, nc);
    }

    /**
     * Adds a listener, has no effect if the listener is already registered.
     * 
     * @param listener The listener to add
     */
    public void addListener(final NodeStateListener listener) {
        if (!m_eventListeners.contains(listener)) {
            m_eventListeners.add(listener);
        }
    }

    /**
     * Adds an outgoing connection to a specified port. Note that more than one
     * outgoing connection can exist for each port.
     * 
     * @param port index of outgoing port
     * @param nc NodeContainer this connection points to (sink).
     */
    void addOutgoingConnection(final int port, final NodeContainer nc) {
        // sanity checks:
        if (nc == null) {
            throw new IllegalArgumentException("Outgoing connection at port #"
                    + port + " has no target node defined (null).");
        }
        if (port < 0 || port > getNrOutPorts()) {
            throw new IndexOutOfBoundsException("Port index out of bounds: "
                    + port);
        }
        // add connection also on the other side (the sink).
        if (m_succ.get(port) == null) {
            m_succ.set(port, new ArrayList<NodeContainer>());
        }
        m_succ.get(port).add(nc);
    }

    /**
     * @see Node#closeAllViews()
     */
    public void closeAllViews() {
        m_node.closeAllViews();
    }

    /**
     * @see Node#closeAllPortViews()
     */
    public void closeAllPortViews() {
        m_node.closeAllPortViews();
    }

    /**
     * @see Node#configure()
     */
    void configure() {
        m_node.configure();
    }

    /**
     * Connects an inport of this noe with an outport of another node.
     * 
     * @param inPort the index of the inport
     * @param outNode the predecessor node
     * @param outPort the index of the output port
     */
    void connectPorts(final int inPort, final NodeContainer outNode,
            final int outPort) {
        m_node.getInPort(inPort)
                .connectPort(outNode.m_node.getOutPort(outPort));
    }

    /**
     * @see Node#removeInternals()
     */
    void removeInternals() {
        m_node.removeInternals();
    }

    /**
     * @see Node#detach()
     */
    void detach() {
        m_node.detach();
    }

    /**
     * @see Node#cleanup()
     */
    void cleanup() {
        m_node.cleanup();
    }

    /**
     * Disconnets the inport with the given id from its predecessor.
     * 
     * @param inPort the index of the inport
     */
    void disconnectPort(final int inPort) {
        m_node.getInPort(inPort).disconnectPort();
    }

    /**
     * Returns all successor node containers of this node container.
     * 
     * @return a collection of all successor nodes
     */
    public Collection<NodeContainer> getAllSuccessors() {
        ArrayList<NodeContainer> succ = new ArrayList<NodeContainer>();
        for (List<NodeContainer> ncl : m_succ) {
            if (ncl != null) {
                for (NodeContainer nc : ncl) {
                    if (!succ.contains(nc)) {
                        succ.add(nc);
                    }
                }
            }
        }

        for (int i = 0; i < succ.size(); i++) {
            for (List<NodeContainer> ncl : succ.get(i).m_succ) {
                if (ncl != null) {
                    for (NodeContainer nc : ncl) {
                        if (!succ.contains(nc)) {
                            succ.add(nc);
                        }
                    }
                }
            }
        }

        return succ;
    }

    /**
     * Returns the custom name for this node container.
     * 
     * @return the user specified name
     */
    public String getCustomName() {
        return m_customName;
    }

    /**
     * Returns the user-specified description for this node container.
     * 
     * @return the description
     */
    public String getDescription() {
        return m_description;
    }

    /**
     * @see Node#getDialogPane()
     * @throws NotConfigurableException If dialog is not configurable.
     */
    public NodeDialogPane getDialogPane() throws NotConfigurableException {
        return m_node.getDialogPane();
    }

    /**
     * Returns the embedded workflow manager, if the underlying node contains a
     * meta node model.
     * 
     * @return the embedded workflow manager or <code>null</code> if the
     *         underlying node model is not a meta node model
     */
    public WorkflowManager getEmbeddedWorkflowManager() {
        return m_node.getEmbeddedWorkflowManager();
    }

    /**
     * @return extra information object of this node
     */
    public NodeExtraInfo getExtraInfo() {
        return m_extraInfo;
    }

    /**
     * @see org.knime.core.node.Node#getFullHTMLDescription()
     */
    public String getFullHTMLNodeDescription() {
        return m_node.getFullHTMLDescription();
    }

    /**
     * @return the icon associated with this node
     */
    public URL getIcon() {
        return m_node.getFactory().getIcon();
    }

    /**
     * Returns the unique id of this node container.
     * 
     * @return identifier of this node
     */
    public int getID() {
        return m_id;
    }

    /**
     * Returns an unmodifieable list of all inports of this node.
     * 
     * @return the in ports of this node.
     */
    public List<NodeInPort> getInPorts() {
        if (m_cachedInPorts == null) {
            List<NodeInPort> inPorts = new ArrayList<NodeInPort>();
            for (int i = 0; i < m_node.getNrInPorts(); i++) {
                inPorts.add(m_node.getInPort(i));
            }
            m_cachedInPorts = Collections.unmodifiableList(inPorts);
        }
        return m_cachedInPorts;
    }

    /**
     * @see Node#getInportName(int)
     */
    public String getInportName(final int port) {
        return m_node.getInportName(port);
    }

    /**
     * @see Node#getModelClass()
     */
    public Class<? extends NodeModel> getModelClass() {
        return m_node.getModelClass();
    }

    /**
     * @see Node#getName()
     */
    public String getName() {
        return m_node.getName();
    }

    /**
     * @see Node#getType()
     */
    public NodeType getType() {
        return m_node.getType();
    }

    /**
     * @return This node's name with id.
     */
    public String getNameWithID() {
        return getName() + " (#" + getID() + ")";
    }

    /**
     * @see Node#getNrDataInPorts()
     */
    public int getNrDataInPorts() {
        return m_node.getNrDataInPorts();
    }

    /**
     * @see Node#getNrDataOutPorts()
     */
    public int getNrDataOutPorts() {
        return m_node.getNrDataOutPorts();
    }

    /**
     * @see Node#getNrInPorts()
     */
    public int getNrInPorts() {
        return m_node.getNrInPorts();
    }

    /**
     * @see Node#getNrOutPorts()
     */
    public int getNrOutPorts() {
        return m_node.getNrOutPorts();
    }

    /**
     * @see Node#getNrModelContentInPorts()
     */
    public int getNrModelContentInPorts() {
        return m_node.getNrModelContentInPorts();
    }

    /**
     * @see Node#getNrModelContentOutPorts()
     */
    public int getNrModelContentOutPorts() {
        return m_node.getNrModelContentOutPorts();
    }

    /**
     * @see Node#getNumViews()
     */
    public int getNumViews() {
        return m_node.getNumViews();
    }

    /**
     * @see Node#getOneLineDescription()
     */
    public String getOneLineDescription() {
        return m_node.getOneLineDescription();
    }

    /**
     * Returns all out ports of this node.
     * 
     * @return the out ports of this node.
     */
    public List<NodeOutPort> getOutPorts() {
        if (m_cachedOutPorts == null) {
            List<NodeOutPort> outPorts = new ArrayList<NodeOutPort>();
            for (int i = 0; i < m_node.getNrOutPorts(); i++) {
                outPorts.add(m_node.getOutPort(i));
            }
            m_cachedOutPorts = Collections.unmodifiableList(outPorts);
        }
        return m_cachedOutPorts;
    }

    /**
     * @param port the port index to retrieve the name for.
     * 
     * @return the name of the specified outport.
     */
    public String getOutportName(final int port) {
        return m_node.getOutportName(port);
    }

    /**
     * Returns an array of direct predecessors of this node.
     * 
     * @return an array of NodeContainers
     */
    public Collection<NodeContainer> getPredecessors() {
        return Collections.unmodifiableCollection(m_pred);
    }

    /**
     * @see Node#getStatus()
     */
    public NodeStatus getStatus() {
        return m_node.getStatus();
    }

    /**
     * Returns a matrix of all successors of this node.
     * 
     * @return a matrix of NodeContainers, one row for each OutPort.
     */
    public NodeContainer[][] getSuccessors() {
        // prepare array
        NodeContainer[][] result = new NodeContainer[m_succ.size()][];
        for (int i = 0; i < m_succ.size(); i++) {
            if (m_succ.get(i) != null) {
                result[i] = m_succ.get(i).toArray(new NodeContainer[]{});
            } else {
                result[i] = new NodeContainer[0];
            }
        }
        return result;
    }

    /**
     * @see Node#getView(int, String)
     */
    public NodeView getView(final int viewIndex) {
        return m_node.getView(viewIndex, getNameWithID());
    }

    /**
     * @see Node#getViewName(int)
     */
    public String getViewName(final int viewIndex) {
        return m_node.getViewName(viewIndex);
    }

    /**
     * @see Node#hasDialog()
     */
    public boolean hasDialog() {
        return m_node.hasDialog();
    }

    /**
     * @see Node#isAutoExecutable()
     */
    public boolean isAutoExecutable() {
        return m_node.isAutoExecutable();
    }

    /**
     * @see Node#isConfigured()
     */
    public boolean isConfigured() {
        return m_node.isConfigured();
    }

    /**
     * @see Node#isDataInPort(int)
     */
    public boolean isDataInPort(final int inPort) {
        return m_node.isDataInPort(inPort);
    }

    /**
     * @see Node#isDataOutPort(int)
     */
    public boolean isDataOutPort(final int outPort) {
        return m_node.isDataOutPort(outPort);
    }

    /**
     * @see Node#isExecutable()
     */
    public boolean isExecutable() {
        return m_node.isExecutable();
    }

    /**
     * Check if node can be executed - this is also true if all nodes leading up
     * to this node can be executed. In a GUI this would mean that this node
     * will show up "yellow".
     * 
     * @return true if node can be used to initiate an execution up to here.
     */
    public boolean isExecutableUpToHere() {
        // check first if node is executed
        if (isExecuted()) {
            return false;
        }
        // update internal flag so we know if we can actually
        // initiate an "execution up to here" from this node or not
        boolean isExectuableUpToHere = false;
        if (m_node.isConfigured()) {
            isExectuableUpToHere = true;
            // if this node is executable in principle (= is configured),
            // verify that all predecessors are executable-up-to-here.
            for (NodeContainer pred : getPredecessors()) {
                if (pred == null) {
                    isExectuableUpToHere = false;
                    break;
                } else if (pred.isExecuted()) {
                    continue;
                } else if (!pred.isExecutableUpToHere()) {
                    isExectuableUpToHere = false;
                    break;
                }
            }
        }
        return isExectuableUpToHere;
    }

    /**
     * @see Node#isExecuted()
     */
    public boolean isExecuted() {
        return m_node.isExecuted();
    }

    /**
     * Test if a given node is among the (direct or indirect) successors of this
     * node.
     * 
     * @param target node to be searched for
     * @return true if target is a successor somewhere down the line
     */
    protected boolean isFollowedBy(final NodeContainer target) {
        // check for recursion target
        if (this == target) {
            return true;
        }
        // test all successors and recursively their successors
        boolean hasReached = false;
        NodeContainer[][] nextNodes = this.getSuccessors();
        for (int i = 0; i < nextNodes.length; i++) {
            if (nextNodes[i] != null) {
                for (int j = 0; j < nextNodes[i].length; j++) {
                    if (nextNodes[i][j] != null) {
                        hasReached = hasReached
                                || nextNodes[i][j].isFollowedBy(target);
                    }
                }
            }
        }
        return hasReached;
    }

    /**
     * @see Node#isInterruptible()
     */
    public boolean isInterruptible() {
        return m_node.isInterruptible();
    }

    /**
     * @see Node#isModelContentInPort(int)
     */
    public boolean isPredictorInPort(final int portNumber) {
        return m_node.isModelContentInPort(portNumber);
    }

    /**
     * @see Node#isModelContentOutPort(int)
     */
    public boolean isPredictorOutPort(final int portNumber) {
        return m_node.isModelContentOutPort(portNumber);
    }

    /**
     * Loads the settings (but not any data) from the given settings. They are
     * also passed to the underlying node.
     * 
     * @param settings the settings
     * @throws InvalidSettingsException if an expected setting is missing
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_customName = settings.getString(KEY_CUSTOM_NAME);
        m_description = settings.getString(KEY_CUSTOM_DESCRIPTION);
        m_deletable = settings.getBoolean(KEY_IS_DELETABLE);

        setExtraInfo(createExtraInfo(settings));
        m_node.loadSettings(settings);
    }

    /**
     * Loads the node settings and internal structures from the given location,
     * depending on the node's state, configured or executed.
     * 
     * @param loadID Forwared to the node. This id serves as loading id, it
     *            helps to distinguish between two workflows being loaded at the
     *            same time. This id is passed on to the
     *            {@link org.knime.core.node.BufferedDataTable#getDataTable(
     *            int, Integer)}.
     * @param nodeFile The node settings location.
     * @param progMon The monitor reporting progress during reading structure.
     * @throws IOException If the node settings file can't be found or read.
     * @throws InvalidSettingsException If the settings are wrong.
     * @throws CanceledExecutionException If loading was canceled.
     */
    public void load(final int loadID, final File nodeFile,
            final NodeProgressMonitor progMon) throws IOException,
            InvalidSettingsException, CanceledExecutionException {
        ExecutionContext context = new ExecutionContext(progMon, m_node);
        m_node.load(loadID, nodeFile, context);
    }

    /**
     * @return the node's <code>toString</code> description
     */
    public String nodeToString() {
        return m_node.toString();
    }

    /**
     * Notifies all state listeners that the state of this
     * <code>NodeContainer</code> has changed.
     * 
     * @param state <code>NodeStateListener</code>
     */
    protected void notifyStateListeners(final NodeStatus state) {
        for (NodeStateListener listener : m_eventListeners) {
            listener.stateChanged(state, m_id);
        }
    }

    /**
     * Opens the port view of this node for the given port.
     * 
     * @param index the index of the port to open the view for
     */
    public void openPortView(final int index) {
        NodeOutPort port = m_node.getOutPort(index);
        port.openPortView(getNameWithID());
    }

    /**
     * Removes all listeners. For convenience.
     */
    public void removeAllListeners() {
        m_eventListeners.clear();
    }

    /**
     * Deletes an incoming connection.
     * 
     * @param port index of port to be disconnected
     */
    void removeIncomingConnection(final int port) {
        m_pred.set(port, null);
    }

    /**
     * Removes a listener, has no effect if the listener was not registered.
     * 
     * @param listener The listener to remove
     */
    public void removeListener(final NodeStateListener listener) {
        m_eventListeners.remove(listener);
    }

    /**
     * Remove an outgoing connection.
     * 
     * @param port index of outgoing port
     * @param node node the connection to be deleted points to
     */
    void removeOutgoingConnection(final int port, final NodeContainer node) {
        List<NodeContainer> list = m_succ.get(port);
        list.remove(node);
    }

    /**
     * @see Node#resetAndConfigure()
     */
    void resetAndConfigure() {
        m_node.resetAndConfigure();
    }

    /**
     * Saves only the settings (including the ones from the underlying node) but
     * not its data.
     * 
     * @param settings a settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString(KEY_FACTORY_NAME, m_node.getFactory().getClass()
                .getName());
        settings.addInt(KEY_ID, m_id);
        settings.addString(KEY_CUSTOM_NAME, m_customName);
        settings.addString(KEY_CUSTOM_DESCRIPTION, m_description);
        settings.addBoolean(KEY_IS_DELETABLE, m_deletable);

        if (m_extraInfo != null) {
            settings.addString(KEY_EXTRAINFOCLASS, m_extraInfo.getClass()
                    .getName());
            m_extraInfo.save(settings);
        }
        m_node.saveSettings(settings);
    }

    /**
     * Write node container and node settings.
     * 
     * @param settings To write settings to.
     * @param nodeFile To write node settings to.
     * @param progMon Used to report progress during saving.
     * @throws IOException If the node file can't be found or read.
     * @throws CanceledExecutionException If the saving has been canceled.
     */
    public void save(final NodeSettingsWO settings, final File nodeFile,
            final NodeProgressMonitor progMon) throws IOException,
            CanceledExecutionException {

        ExecutionContext exec = new ExecutionContext(progMon, m_node);
        saveSettings(settings);
        m_node.save(nodeFile, exec);
    }

    /**
     * Sets a user name for this node.
     * 
     * @param name the user name to set for this node
     */
    public void setCustomName(final String name) {
        m_customName = name;
        notifyStateListeners(new NodeStatus.CustomName());
    }

    /**
     * Sets a user description for this node.
     * 
     * @param description the user name to set for this node
     */
    public void setDescription(final String description) {
        m_description = description;
        notifyStateListeners(new NodeStatus.CustomDescription());
    }

    /**
     * Overwrite <code>ExtraInfo</code> object of this node.
     * 
     * @param ei new extra information object for this node
     */
    public void setExtraInfo(final NodeExtraInfo ei) {
        m_extraInfo = ei;
        notifyStateListeners(new NodeStatus.ExtrainfoChanged());
    }

    /**
     * @see Node#showDialog()
     */
    public void showDialog() {
        m_node.showDialog("Dialog - " + getNameWithID());
    }

    /**
     * Opens the NodeView for the given index. Views for each index can be
     * opened multiple times.
     * 
     * @param viewIndex The view's index.
     * @see Node#showView(int)
     */
    public void showView(final int viewIndex) {
        m_node.showView(viewIndex, getNameWithID());
    }

    /**
     * Starts the execution. The node must not be already started and has to be
     * in executable state.
     * 
     * @param pm the progress monitor (for cancelation and progress updates)
     * @return the future that has been created for the node
     */
    public synchronized Future<?> startExecution(final NodeProgressMonitor pm) {
        if (!m_node.isExecutable()) {
            throw new IllegalStateException("Node is not in executable state");
        }
        // make sure node is not already executing (should not happen)
        if (m_executionRunning) {
            m_logger.error("Node is already/still running, new execute"
                    + " is not allowed. (" + this.getID() + ")");
            return null;
        }
        m_executionRunning = true;
        // ok, let's start execution:

        Runnable r = new Runnable() {
            public void run() {
                try {
                    pm.setMessage("Preparing...");
                    // executeNode() should return as soon as possible if
                    // canceled - or after it has been finished of course
                    // NOTE: the return from this call may happen AFTER
                    // the state-changed event has already been processed!
                    m_node.execute(new ExecutionContext(pm, m_node));
                } finally {
                    // and always clean up, no matter how we got out of here
                    m_executionRunning = false;

                    // Do not forgot to notify all listeners. Note that this
                    // replaces the simple forwarding of the event arriving
                    // from Node.execute itself to avoid racing conditions
                    // (event arrives before m_executionSuccess flag is set
                    // correctly.
                    notifyStateListeners(new NodeStatus.EndExecute());
                }
            }
        };
        if (pm != null) {
            pm.setMessage("Scheduled for execution...");
        }
        pm.setProgress(0.0);
        return KNIMEConstants.GLOBAL_THREAD_POOL.enqueue(r);
    }

    /**
     * Callback from node (this <code>NodeContainer</code> has registered
     * itself as a listener to the underlying <code>Node</code>), indicating
     * that the underlying node has changed its state. Fire new event to all
     * listeners with the node-ID added.
     * 
     * @param st Indicates the type of status change of this node.
     * @param id identifier from <code>Node</code>: will be overwritten with
     *            NodeContainer ID
     */
    public synchronized void stateChanged(final NodeStatus st, final int id) {
        // TODO (tg) id is never used?
        if (st instanceof NodeStatus.EndExecute) {
            // do not immediately forward this event. We will generate a
            // new event after we have actually set all internal flags
            // correctly. Otherwise this event may overtake the
            // processing of the return value of Node.execute()!
        } else {
            // forward all other events immediately
            notifyStateListeners(st);
        }
    }

    /**
     * @return Node name and id.
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return m_node.getName() + "(#" + m_id + ")";
    }

    /**
     * Loads the settings from the dialog into the model.
     * 
     * @throws InvalidSettingsException if the settings are invalid
     */
    void loadModelSettingsFromDialog() throws InvalidSettingsException {
        m_node.loadModelSettingsFromDialog();
    }

    /**
     * Loads the dialog settings into the model, resets and configures the node
     * and configures all its sucessor nodes.
     * 
     * @throws WorkflowInExecutionException if settings cannot be applied
     *             because the workflow is currently executed
     * @throws InvalidSettingsException if the settings are invalid
     */
    public void applyDialogSettings() throws WorkflowInExecutionException,
            InvalidSettingsException {
        m_wfm.applyDialogSettings(this);
    }

    /**
     * Returns if this node can be delete or not.
     * 
     * @return <code>true</code> if it can be deleted, <code>false</code>
     *         otherwise
     */
    public boolean isDeletable() {
        return m_deletable;
    }

    /**
     * Sets if this node can be deleted or not.
     * 
     * @param deletable <code>true</code> if it can be deleted,
     *            <code>false</code> otherwise
     */
    public void setDeletable(final boolean deletable) {
        m_deletable = deletable;
    }

    /**
     * @see Node#retrieveModel(MetaNodeModel)
     */
    public void retrieveModel(final MetaNodeModel metaModel) {
        m_node.retrieveModel(metaModel);
    }

    /**
     * @see Node#isFullyConnected()
     */
    public boolean isFullyConnected() {
        return m_node.isFullyConnected();
    }
    
    /**
     * @see Node#providesProgress()
     */
    public boolean providesProgress() {
        return m_node.providesProgress();
    }
}
