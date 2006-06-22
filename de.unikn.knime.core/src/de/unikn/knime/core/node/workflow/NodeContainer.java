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
 *   14.02.2005 (M. Berthold): created
 *   12.01.2006 (mb): clean up for code review
 */
package de.unikn.knime.core.node.workflow;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Future;

import de.unikn.knime.core.eclipseUtil.GlobalClassCreator;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.KNIMEConstants;
import de.unikn.knime.core.node.Node;
import de.unikn.knime.core.node.NodeDialogPane;
import de.unikn.knime.core.node.NodeFactory;
import de.unikn.knime.core.node.NodeInPort;
import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeOutPort;
import de.unikn.knime.core.node.NodeProgressMonitor;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.NodeStateListener;
import de.unikn.knime.core.node.NodeStatus;
import de.unikn.knime.core.node.NodeView;
import de.unikn.knime.core.node.NotConfigurableException;

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
    protected static final String KEY_CUSTOM_DESCRIPTION = "customDescription";

    /** Key for this node's user name. */
    protected static final String KEY_CUSTOM_NAME = "customName";

    /** Key for extra info's class name. */
    protected static final String KEY_EXTRAINFOCLASS = "extraInfoClassName";

    /** Key for this node's internal ID. */
    protected static final String KEY_ID = "id";
    
    /** Key for the factory class name, used to load nodes. */
    protected static final String KEY_FACTORY_NAME = "factory";

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
    protected static NodeExtraInfo createExtraInfo(final NodeSettings sett)
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
                LOGGER.warn("ExtraInfoClass could not " + "be loaded "
                        + extraInfoClassName + " reason: " + e.getMessage());
            }
        }
        return extraInfo;
    }

    /** A userspecified name for this node. */
    private String m_customName;

    /** A userspecified description for this node. */
    private String m_description;

    // store list of listeners - essentially this Container will listen
    // to events coming from its <code>Node</code>, add the id to the
    // event and forward it.
    private final List<NodeStateListener> m_eventListeners;
    
    private boolean m_executionCanceled;

    // for execution of the Node in its own Thread, hold status
    // information of the execution thread...
    private boolean m_executionRunning;

    private boolean m_executionSuccess;

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

    // ... and a progress monitor as well as the thread itself.
    private NodeProgressMonitor m_progressMonitor;

    // ...for each port a list of successors...
    private final Vector<List<NodeContainer>> m_succ;

    
    private List<NodeInPort> m_cachedInPorts;
    private List<NodeOutPort> m_cachedOutPorts; 
    
    /**
     * Create new container using an existing Node and a predefined ID.
     * 
     * @param n node to wrap
     * @param id identifier of the node
     */
    public NodeContainer(final Node n, final int id) {
        m_node = n;
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
    
    private static NodeFactory readNodeFactory(final NodeSettings settings)
            throws InvalidSettingsException {
        // read node factory class name
        String factoryClassName = settings.getString(KEY_FACTORY_NAME);
        try {
            // use global Class Creator utility for Eclipse "compatibility"
            return (NodeFactory)((GlobalClassCreator
                    .createClass(factoryClassName)).newInstance());
        } catch (Exception e) {
            throw new InvalidSettingsException("NodeFactory could not be "
                    + "loaded: " + factoryClassName, e);
        }
    }
        
    /**
     * Creates a new NodeContainer and reads it's status and information from
     * the NodeSettings object. Note that the list of predecessors and
     * successors will NOT be initalized correctly. The Workflow manager is
     * required to take care of re-initializing the connections.
     * 
     * @param settings Retrieve the data from.
     * @param wfm the workflowmanager that is responsible for this node
     * @throws InvalidSettingsException If the required keys are not available
     *             in the NodeSettings.
     * 
     * @see #save(NodeSettings)
     */
    public NodeContainer(
            final NodeSettings settings,
            final WorkflowManager wfm)
            throws InvalidSettingsException {
        this(new Node(readNodeFactory(settings), wfm), settings.getInt(KEY_ID));
        
        setExtraInfo(createExtraInfo(settings));

        try {
            // read custom name
            String name = settings.getString(KEY_CUSTOM_NAME);

            // if there was no user node name defined than keep the default name
            if (name != null) {
                setCustomName(name);
            }
        } catch (InvalidSettingsException ise) {
            LOGGER.warn("In the settings of node <id:" + getID()
                    + "|type:" + getName()
                    + "> is no user name specified");
        }

        try {
            // read custom description
            String description = settings.getString(KEY_CUSTOM_DESCRIPTION);
            setDescription(description);
        } catch (InvalidSettingsException ise) {
            LOGGER.warn("In the settings of node <id:" + getID()
                    + "|type:" + getName()
                    + "> is no user description specified");
        }
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
            throw new NullPointerException("Outgoing connection at port #"
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
     * attempt to cancel execution by setting corresponding flag in.
     * ExecutionMonitor
     */
    public void cancelExecution() {
        if (m_progressMonitor != null) {
            m_progressMonitor.setExecuteCanceled();
            // TODO needs to come back from Monitor not just set here!
            // but we need Node to tell us that execution was cancelled...
            m_executionCanceled = true;
        }
    }

    /**
     * @return Returns whether the execution terminated successfully
     */
    public synchronized boolean executionSucceeded() {
        return m_executionSuccess;
    }

    /**
     * @return Returns whether the execution was canceled prematurely
     */
    public synchronized boolean executionWasCanceled() {
        return m_executionCanceled;
    }

    /**
     * @return user specified name of this node
     */
    public String getCustomName() {
        return m_customName;
    }

    /**
     * @return user specified description of this node
     */
    public String getDescription() {
        return m_description;
    }

    /**
     * Return The dialog pane which holds all the settings' components. In
     * addition this method loads the settings from the model into the dialog
     * pane. The pane might be <code>null</code> if no dialog is available.
     * 
     * @return dialog pane
     * @throws NotConfigurableException if the dialog cannot be opened because
     * of real invalid settings or if any predconditions are not fulfilled, e.g.
     * no predecessor node, no nominal column in input table, etc.
     */
    public NodeDialogPane getDialogPane() throws NotConfigurableException {
        return m_node.getDialogPane();
    }

    /** 
     * Returns the embedded workflow manager, if the underlying node contains
     * a meta node model.
     * 
     * @return the embedded workflow manager or <code>null</code> if the
     * underlying node model is not a meta node model
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
     * @return a html page containing the node's description
     * @see de.unikn.knime.core.node.Node#getFullHTMLDescription()
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
     * @return identifier of this node
     */
    public int getID() {
        return m_id;
    }

    /**
     * Returns an unmodifieable list of all in ports of this node.
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
     * Delegation method to the node's <code>getInputDescription</code>
     * method.
     * 
     * @param port The port id of interest
     * @return The description to that port
     */
    public String getInputDescription(final int port) {
        return m_node.getInputPortDescription(port);
    }

    /**
     * Returns the concrete class of this node's model.
     * 
     * @return the model's class
     */
    public Class<? extends NodeModel> getModelClass() {
        return m_node.getModelClass();
    }

    //
    // TODO: remove the next two functions - no deep access to NodePorts!
    //

    /**
     * @return This node's name.
     */
    public String getName() {
        return m_node.getName();
    }

    /**
     * @return This node's name with id.
     */
    public String getNameWithID() {
        return getName() + " (#" + getID() + ")";
    }

    /**
     * @return This container's underlying <code>Node</code>.
     */
    protected Node getNode() {
        return m_node;
    }

    /**
     * @return The number of data output ports.
     */
    public int getNrDataInPorts() {
        return m_node.getNrDataInPorts();
    }

    /**
     * @return The number of data output ports.
     */
    public int getNrDataOutPorts() {
        return m_node.getNrDataOutPorts();
    }

    /**
     * Returns the number of input ports of the underlying node.
     * 
     * @return the number of inports
     */
    public int getNrInPorts() {
        return m_node.getNrInPorts();
    }

    /**
     * Returns the number of output ports of the underlying node.
     * 
     * @return the number of outports
     */
    public int getNrOutPorts() {
        return m_node.getNrOutPorts();
    }

    /**
     * @return The number of <code>PredictorParams</code> output ports.
     */
    public int getNrPredictorInPorts() {
        return m_node.getNrPredictorInPorts();
    }

    /**
     * @return The number of <code>PredictorParams</code> output ports.
     */
    public int getNrPredictorOutPorts() {
        return m_node.getNrPredictorOutPorts();
    }

    /**
     * Returns the number of views for this node.
     * 
     * @return number views of this node
     */
    public int getNumViews() {
        return m_node.getNumViews();
    }

    /**
     * @return a single line containing a brief node description
     * @see de.unikn.knime.core.node.Node#getOneLineDescription
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
     * Delegation method to nodes's <code>getOutputDescription</code> method.
     * 
     * @param port The port id of interest.
     * @return The description to that port.
     * @throws IndexOutOfBoundsException If argument is out of range.
     */
    public String getOutputDescription(final int port) {
        return m_node.getOutputPortDescription(port);
    }

    /**
     * Return array of predecessors of this node.
     * 
     * @return an array of NodeContainers, one entry for each InPort.
     */
    public Collection<NodeContainer> getPredecessors() {
        return Collections.unmodifiableCollection(m_pred);
    }

    /**
     * Returns the node status object of this <code>Node</code>.
     * 
     * @return the node status
     */
    public NodeStatus getStatus() {
        return m_node.getStatus();
    }

    /**
     * Return array of successors of this node.
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
     * TODO Thorsten Meinl
     * @return
     */
    public Collection<NodeContainer> getAllSuccessors() {
        ArrayList<NodeContainer> succ = new ArrayList<NodeContainer>();
        for (List<NodeContainer> ncl : m_succ) {
            if (ncl != null) {
                for (NodeContainer nc : ncl) {
                    if (!succ.contains(nc)) { succ.add(nc); }
                }
            }
        }
        
        for (int i = 0; i < succ.size(); i++) {
            for (List<NodeContainer> ncl : succ.get(i).m_succ) {
                if (ncl != null) {
                    for (NodeContainer nc : ncl) {
                        if (!succ.contains(nc)) { succ.add(nc); }
                    }
                }
            }            
        }
        
        return succ;
    }
    
    /**
     * Returns the node's view.
     * 
     * @param viewIndex the index of the view (if more than one)
     * @return The view, or <code>null</code> in case of an error
     */
    public NodeView getView(final int viewIndex) {
        return m_node.getView(viewIndex);
    }

    /**
     * Returns the view name for the given index.
     * 
     * @param viewIndex the index of the view (if more than one)
     * @return the name of the given view
     */
    public String getViewName(final int viewIndex) {
        return m_node.getViewName(viewIndex);
    }

    /**
     * @return flag indicating if this node has a dialog.
     */
    public boolean hasDialog() {
        return m_node.hasDialog();
    }

    /**
     * @return If this node is auto executable.
     * @see Node#isAutoExecutable()
     */
    public boolean isAutoExecutable() {
        return m_node.isAutoExecutable();
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
     * @return true if node has successfully been executed
     */
    public boolean isExecuted() {
        return m_node.isExecuted();
    }

    // ////////////////////////
    // NodeContainer save&load
    // ////////////////////////

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
     * Checks whether the in port given by its index is a PredictorParams port,
     * that is, the port takes information about a predictor model as an input.
     * 
     * @param portNumber the port number
     * 
     * @return true if the port is a PredictorParams port, false otherwise
     */
    public boolean isPredictorInPort(final int portNumber) {
        return m_node.isPredictorInPort(portNumber);
    }

    /**
     * Checks whether the out port given by its index is a PredictorParams port.
     * 
     * @param portNumber the port number
     * 
     * @return true if the port is a PredictorParams port, false otherwise
     */
    public boolean isPredictorOutPort(final int portNumber) {
        return m_node.isPredictorOutPort(portNumber);
    }

    /**
     * @return the node's <code>toString</code> description
     */
    public String nodeToString() {
        return m_node.toString();
    }

    /**
     * Notifies all state listeners that the state of this
     * <code>NodeContainer</code> has changed. Protected so that
     * <code>MetaNodeContainer</code> can use it.
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
        port.openPortView(m_node.getName() + " (#" + index + ")");
    }

    // ///////////////////
    // Execution Handling
    // ///////////////////

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
     * Resets the node. Will reset the entire flow from here by propagating this
     * through the output ports.
     */
    void resetAndConfigure() {
        m_node.resetAndConfigure();
    }

    // ////////////////////////
    // Event Listener handling
    // ////////////////////////

    /**
     * Write node container settings which are factory name, node id, customer
     * name and description, and extra info (optional).
     * @param settings To write settings to.
     */
    public void save(final NodeSettings settings) {
        // save node factory
        settings.addString(KEY_FACTORY_NAME, 
                m_node.getFactory().getClass().getName());
        // save id
        settings.addInt(KEY_ID, m_id);
        // save name
        settings.addString(KEY_CUSTOM_NAME, m_customName);
        // save description
        settings.addString(KEY_CUSTOM_DESCRIPTION, m_description);
        // save type of extrainfo and also it's content - but only if it exists
        if (m_extraInfo != null) {
            settings.addString(KEY_EXTRAINFOCLASS, m_extraInfo.getClass()
                    .getName());
            m_extraInfo.save(settings);
        }
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
     * Opens the node's dialog.
     */
    public void showDialog() {
        m_node.showDialog();
    }

    /**
     * Opens the node's view. (Which may be empty if node is not executed.)
     * 
     * @param viewIndex the index of the view (=0 if there is only one)
     */
    public void showView(final int viewIndex) {
        m_node.showView(viewIndex);
    }
    
    /**
     * Closes all views.
     *
     */
    public void closeAllViews() {
        m_node.closeAllViews();
    }
    
    /**
     * Starts the execution. The node must not be already started and has to be
     * in executable state.
     * 
     * @param pm The progress monitor (for cancelation and progress updates)
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

        // so far execution was unsuccessful
        m_executionSuccess = false;
        // ...and not canceled
        m_executionCanceled = false;
        // create the actual worker thread and remember Progress Monitor
        m_progressMonitor = pm;
        Runnable r = new Runnable() {
            public void run() {
                try {
                    // executeNode() should return as soon as possible if
                    // canceled - or after it has been finished of course
                    // NOTE: the return from this call may happen AFTER
                    // the state-changed event has already been processed!
                    m_executionSuccess = m_node
                            .execute(new ExecutionMonitor(pm));
                } catch (Exception e) {
                    // some other error - this should never happen!
                    m_executionSuccess = false;
                    m_logger.fatal("Fatal exception", e);
                } catch (AssertionError ae) {
                    m_executionSuccess = false;
                    m_logger.assertLog(false, ae.getMessage(), ae);
                } catch (Error e) {
                    // some other error - should never happen!
                    m_executionSuccess = false;
                    m_logger.fatal("Fatal error", e);
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
     * @return <code>true</code> if this node's model is a interruptible model.
     */
    public boolean isInterruptible() {
        return m_node.isInterruptible();
    }
    
    /**
     * @return Node name and id.
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getNode().getName() + "(#" + m_id + ")";
    }
}
