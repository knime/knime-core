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
 *   14.02.2005 (M. Berthold): created
 *   12.01.2006 (mb): clean up for code review
 */
package de.unikn.knime.core.node.workflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import de.unikn.knime.core.eclipseUtil.GlobalClassCreator;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.Node;
import de.unikn.knime.core.node.NodeDialogPane;
import de.unikn.knime.core.node.NodeInPort;
import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.NodeOutPort;
import de.unikn.knime.core.node.NodeProgressMonitor;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.NodeStateListener;
import de.unikn.knime.core.node.NodeStatus;
import de.unikn.knime.core.node.NodeView;

/**
 * Wrapper for a Node and the surrounding graph information, i.e. successors and
 * predecessors. For each InPort the node can store no more than one
 * predecessor, for each OutPort a list of successors is held. The NodeContainer
 * listens to event coming from the node and sends them to it's own listeners
 * after adding the node's ID.
 * 
 * @author M. Berthold, University of Konstanz
 */
public class NodeContainer implements NodeStateListener {

    // The node logger for the underlying node is used here.
    private final NodeLogger m_logger;

    // remember node itself...
    private final Node m_node;
    // ...it's ID
    private final int m_id;
    // ...for each port a list of successors...
    private final Vector<List<NodeContainer>> m_succ;
    // ...and an array of predecessors (only one per port!)
    private final Vector<NodeContainer> m_pred;

    // Also hold an object storing information about this node's
    // position on the visual representation of this workflow (or
    // other supplemental info) - if available. The NodeContainer
    // itself does not care about it but simply makes sure it is
    // stored and retrieved using the appropriate interface.
    private NodeExtraInfo m_extraInfo;

    /**
     * flags for events modeling information regarding state of node
     */
    /** not waiting for anything. */
    public static final int STATE_IDLE = 0;
    /** is waiting to be excuted once the underlying node is executable. */
    public static final int STATE_WAITING_TO_BE_EXECUTABLE = 1;
    /** can be executed. */
    public static final int STATE_IS_EXECUTABLE = 2;
    /** has been returned as EXECUTABLE and waits for thread to start. */
    public static final int STATE_WAITING_FOR_EXECUTION = 3;
    /** is currently being executed. */
    public static final int STATE_CURRENTLY_EXECUTING = 4;
    /** properties (extra info) have changed. */
    public static final int EVENT_EXTRAINFO_CHANGED = 1000;

    // and also remember the actual state: initialize using default.
    private int m_state = STATE_IDLE;

    // Holds the current <code>NodeContainer</code> status number and message.
    // which are essentially warnings and error messages (note the difference
    // to the node state!). The status is used by e.g. a GUI to display
    // things that went wrong during execution or setup. So it may explain
    // the reasons why a specific state has (or has not) been reached.
    //private NodeStatus m_nodeStatus;

    // for execution of the Node in it's own Thread, hold status
    // information of the execution thread...
    private boolean m_executionRunning;
    private boolean m_executionSuccess;
    private boolean m_executionCanceled;
    // ... and a progress monitor as well as the thread itself.
    private NodeProgressMonitor m_progressMonitor;
    private Thread m_workerThread;

    // store list of listeners - essentially this Container will listen
    // to events coming from it's <code>Node</code>, add the id to the
    // event and forward it.
    private final ArrayList<NodeStateListener> m_eventListeners;

    /** Create new container using an existing Node and a predefined ID.
     * 
     * @param n node to wrap
     * @param id identifier of the node
     */
    public NodeContainer(final Node n, final int id) {
        m_logger = NodeLogger.getLogger(n.getNodeName());
        m_node = n;
        m_id = id;
        m_succ = new Vector<List<NodeContainer>>(m_node.getNrOutPorts());
        m_pred = new Vector<NodeContainer>(m_node.getNrInPorts());
        m_succ.setSize(m_node.getNrOutPorts());
        m_pred.setSize(m_node.getNrInPorts());
        m_extraInfo = null;
        m_eventListeners = new ArrayList<NodeStateListener>();
        m_node.addStateListener(this);
    }

    /**
     * @return This container's underlying <code>Node</code>.
     */
    protected Node getNode() {
        return m_node;
    }

    /** Checks whether the in port given by its index is a PredictorParams port,
     * that is, the port takes information about a predictor model as an input.
     * 
     * @param portNumber the port number
     * 
     * @return true if the port is a PredictorParams port, false otherwise
     */
    public boolean isPredictorInPort(final int portNumber) {
        return m_node.isPredictorInPort(portNumber);
    }

    /** Checks whether the out port given by its index is a PredictorParams
     * port.
     * 
     * @param portNumber the port number
     * 
     * @return true if the port is a PredictorParams port, false otherwise
     */
    public boolean isPredictorOutPort(final int portNumber) {
        return m_node.isPredictorOutPort(portNumber);
    }

    /** Returns the number of views for this node.
     * 
     * @return number views of this node
     */
    public int getNumViews() {
        return m_node.getNumViews();
    }

    /** Opens the node's view. (Which may be empty if node is not executed.)
     * 
     * @param viewIndex the index of the view (=0 if there is only one)
     */
    public void showView(final int viewIndex) {
        m_node.showView(viewIndex);
    }

    /** Returns the node's view.
     * 
     * @param viewIndex the index of the view (if more than one)
     * @return The view, or <code>null</code> in case of an error
     */
    public NodeView getView(final int viewIndex) {
        return m_node.getView(viewIndex);
    }

    /** Returns the view name for the given index.
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

    /** Opens the node's dialog.
     */
    public void showDialog() {
        m_node.showDialog();
    }

    /**
     * @return identifier of this node
     */
    public int getID() {
        return m_id;
    }

    /**
     * @return This node's name.
     */
    public String getNodeName() {
        return m_node.getNodeName();
    }
    
    /**
     * @return If this node is auto executable.
     * @see Node#isAutoExecutable()
     */
    public boolean isAutoExecutable() {
        return m_node.isAutoExecutable();
    }

    /**
     * @return This node's name with id.
     */
    public String getNodeNameWithID() {
        return getNodeName() + " (#" + getID() + ")";
    }

    /**
     * @return a html page containing the node's description
     * @see de.unikn.knime.core.node.Node#getFullHTMLDescription()
     */
    public String getFullHTMLNodeDescription() {
        return m_node.getFullHTMLDescription();
    }

    /**
     * @return a single line containing a brief node description
     * @see de.unikn.knime.core.node.Node#getOneLineDescription
     */
    public String getNodeOneLineDescription() {
        return m_node.getOneLineDescription();
    }

    /**
     * @return extra information object of this node
     */
    public NodeExtraInfo getExtraInfo() {
        return m_extraInfo;
    }

    /** Overwrite <code>ExtraInfo</code> object of this node.
     * 
     * @param ei new extra information object for this node
     */
    public void setExtraInfo(final NodeExtraInfo ei) {
        m_extraInfo = ei;
        // send event notification
        stateChanged(new NodeStatus(NodeStatus.STATUS_EXTRA_INFO_CHANGED),
                m_id);
    }

    /**
     * @return int current state of this node
     */
    int getState() {
        return m_state;
    }

    /** 
     * Set new state of node.
     * @param s new state of this node
     * @throws IllegalArgumentException If the argument is out of range, 
     * i.e. not one of STATE_CURRENTLY_EXECUTING, STATE_IDLE, 
     * STATE_IS_EXECUTABLE, STATE_WAITING_FOR_EXECUTION, or 
     * STATE_WAITING_TO_BE_EXECUTABLE.
     */
    void setState(final int s) {
        switch (s) {
            case STATE_CURRENTLY_EXECUTING:
            case STATE_IDLE:
            case STATE_IS_EXECUTABLE:
            case STATE_WAITING_FOR_EXECUTION:
            case STATE_WAITING_TO_BE_EXECUTABLE: break;
            default: throw new IllegalArgumentException(
                    "Invalid state identifier: " + s);
        }
        m_state = s;
    }

    //
    // TODO: remove the next two functions - no deep access to NodePorts!
    //
    
    /** Returns all in ports of this node.
     * 
     * @return the in ports of this node.
     */
    public Collection<NodeInPort> getNodeInPorts() {
        List<NodeInPort> inPorts = new ArrayList<NodeInPort>();
        for (int i = 0; i < m_node.getNrInPorts(); i++) {
            inPorts.add(m_node.getNodeInPort(i));
        }
        return inPorts;
    }

    /** Returns all out ports of this node.
     * 
     * @return the out ports of this node.
     */
    public Collection<NodeOutPort> getNodeOutPorts() {
        List<NodeOutPort> outPorts = new ArrayList<NodeOutPort>();
        for (int i = 0; i < m_node.getNrOutPorts(); i++) {
            outPorts.add(m_node.getNodeOutPort(i));
        }
        return outPorts;
    }

    
    /** Return The dialog pane which holds all the settings' components. In
     * addition this method loads the settings from the model into the
     * dialog pane. The pane might be <code>null</code> if no dialog
     * is available.
     *
     * @return dialog pane
     */
    public NodeDialogPane getDialogPane() {
        return m_node.getDialogPane();
    }

    /** Opens the port view of this node for the given port.
     * 
     * @param index the index of the port to open the view for
     */
    public void openPortView(final int index) {
        NodeOutPort port = m_node.getNodeOutPort(index);
        port.openPortView(m_node.getNodeName() + " (#" + index + ")");
    }

    /** Returns the number of output ports of the underlying node.
     * 
     * @return the number of outports
     */
    public int getNrOutPorts() {
        return m_node.getNrOutPorts();
    }

    /** Returns the number of input ports of the underlying node.
     * 
     * @return the number of inports
     */
    public int getNrInPorts() {
        return m_node.getNrInPorts();
    }

    /** Delegation method to nodes's <code>getOutputDescription</code> method.
     * 
     * @param port The port id of interest.
     * @return The description to that port.
     * @throws IndexOutOfBoundsException If argument is out of range.
     */
    public String getOutputDescription(final int port) {
        return m_node.getOutputPortDescription(port);
    }

    /** Delegation method to the node's <code>getInputDescription</code>
     * method.
     * 
     * @param port The port id of interest
     * @return The description to that port
     */
    public String getInputDescription(final int port) {
        return m_node.getInputPortDescription(port);
    }

    /**
     * @return the node's <code>toString</code> description
     */
    public String nodeToString() {
        return m_node.toString();
    }

    /** Check if node can be executed - this is also true if all nodes
     * leading up to this node can be executed. In a GUI this would mean
     * that this node will show up "yellow".
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
            NodeContainer[] pred = this.getPredecessors();
            for (int p = 0; p < pred.length; p++) {
                if (pred[p] == null) {
                    isExectuableUpToHere = false;
                    break;
                }
                if (pred[p].isExecuted()) {
                    continue;
                }
                if (!pred[p].isExecutableUpToHere()) {
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

    /** Resets the node. Will reset the entire flow from here by propagating
     * this through the output ports.
     */
    public void resetNode() {
        m_node.resetNode();
    }

    /** Adds an outgoing connection to a specified port. Note that more than one
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

    /** Adds an incoming connection to a specified port. Only one incoming
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

    /** Deletes an incoming connection.
     * 
     * @param port index of port to be disconnected
     */
    void removeIncomingConnection(final int port) {
        m_pred.set(port, null);
    }

    /** Remove an outgoing connection.
     * 
     * @param port index of outgoing port
     * @param node node the connection to be deleted points to
     */
    void removeOutgoingConnection(final int port, final NodeContainer node) {
        List<NodeContainer> list = m_succ.get(port);
        list.remove(node);
    }

    /** Return array of predecessors of this node.
     * 
     * @return an array of NodeContainers, one entry for each InPort.
     */
    public NodeContainer[] getPredecessors() {
        return m_pred.toArray(new NodeContainer[0]);
    }

    /** Return array of successors of this node.
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

    /** Test if a given node is among the (direct or indirect) successors of
     * this node.
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

    //////////////////////////
    // NodeContainer save&load
    //////////////////////////

    /** Key for extra info's class name. */
    protected static final String KEY_EXTRAINFOCLASS = "extraInfoClassName";
    /** Key for this node's internal ID. */
    protected static final String KEY_ID = "id";

    /** Stores all information into the given configuration.
     * 
     * @param config The configuration to write to current settings into.
     * @see #createNewNodeContainer
     */
    public void save(final NodeSettings config) {
        // save configuration of underlying node
        m_node.saveConfigTo(config);
        // save id
        config.addInt(KEY_ID, m_id);
        // save type of extrainfo and also it's content - but only if it exists
        if (m_extraInfo != null) {
            config.addString(KEY_EXTRAINFOCLASS, m_extraInfo.getClass()
                    .getName());
            m_extraInfo.save(config);
        }
    }

    /** Creates a new NodeContainer and reads it's status and information from
     * the NodeSettings object. Note that the list of predecessors and
     * successors will NOT be initalized correctly. The Workflow manager is
     * required to take care of re-initializing the connections.
     * 
     * @param sett Retrieve the data from.
     * @return new NodeContainer
     * @throws InvalidSettingsException If the required keys are not available
     *             in the NodeSettings.
     * 
     * @see #save
     */
    public static NodeContainer createNewNodeContainer(final NodeSettings sett)
            throws InvalidSettingsException {
        // create new Node based on configuration
        Node newNode = Node.createNode(sett);
        // read id
        int newID = sett.getInt(NodeContainer.KEY_ID);
        // create new NodeContainer and return it
        NodeContainer newNC = new NodeContainer(newNode, newID);
        newNC.setExtraInfo(createExtraInfo(sett));
        return newNC;
    }

    /** Creates the <code>NodeExtraInfo</code> from given settings, describing
     * whatever additional information was stored (graphical layout?).
     * 
     * @param sett the setting to construct the extra info from
     * @return new <code>NodeExtraInfo</code> object or null
     * @throws InvalidSettingsException if the settings are invalid
     */
    protected static NodeExtraInfo createExtraInfo(final NodeSettings sett)
            throws InvalidSettingsException {
        NodeExtraInfo extraInfo = null;  // null if it doesn't exist
        if (sett.containsKey(NodeContainer.KEY_EXTRAINFOCLASS)) {
            // if it does exist, determine type of extrainfo
            String extraInfoClassName = sett
                    .getString(NodeContainer.KEY_EXTRAINFOCLASS);
            try {
                // use global Class Creator utility for Eclipse "compatibility"
                extraInfo = (NodeExtraInfo)(GlobalClassCreator
                        .createClass(extraInfoClassName).newInstance());
            } catch (Exception e) {
                throw new InvalidSettingsException("ExtraInfoClass could not "
                        + "be loaded " + extraInfoClassName + " reason: "
                        + e.getMessage());
            }
            // and load content of extrainfo
            extraInfo.load(sett);
        }
        return extraInfo;
    }

    /**
     * Loads new <code>NodeSettings</code> into the underlying
     * <code>Node</code>.
     * 
     * @param settings the settings to load
     */
    public void loadConfigFrom(final NodeSettings settings) {
        try {
            m_node.loadConfigFrom(settings);
        } catch (InvalidSettingsException ise) {
            m_logger.error("Settings could not be loaded. " + ise.getMessage());
        }
    }

    /////////////////////
    // Execution Handling
    /////////////////////
    
    /**
     * Starts the execution. The node must not be already started and has to be
     * in executable state.
     * 
     * @param pm The progress monitor (for cancelation and progress updates)
     */
    public synchronized void startExecution(final NodeProgressMonitor pm) {

        if (!m_node.isExecutable()) {
            
            throw new IllegalStateException("Node is not in executable state");
        }
        // make sure node is not already executing (should not happen)
        if (m_executionRunning) {
            m_logger.error("Node is already/still running, new execute"
                    + " is not allowed. (" + this.getID() + ")");
            return;
        }
        m_executionRunning = true;
        // ok, let's start execution:
        
        // so far execution was unsuccessful
        m_executionSuccess = false;
        // ...and not canceled
        m_executionCanceled = false;
        // create the actual worker thread and remember Progress Monitor
        m_progressMonitor = pm;
        m_workerThread = new Thread(new Runnable() {
            public void run() {
                try {
                    // executeNode() should return as soon as possible if
                    // canceled - or after it has been finished of course
                    // NOTE: the return from this call may happen AFTER
                    // the state-changed event has already been processed!
                    m_executionSuccess = m_node
                            .executeNode(new ExecutionMonitor(pm));
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
                    m_workerThread = null;

                    // Do not forgot to notify all listeners. Note that this
                    // replaces the simple forwarding of the event arriving
                    // from Node.execute itself to avoid racing conditions
                    // (event arrives before m_executionSuccess flag is set
                    // correctly.
                    for (int i = 0; i < m_eventListeners.size(); i++) {
                        NodeStateListener listener = m_eventListeners.get(i);
                        listener.stateChanged(new NodeStatus(
                                NodeStatus.END_EXECUTE), m_id);
                    }
                }

            }

        });
        // and finally: GO!
        m_workerThread.start();
    }

    /**
     * attempt to cancel execution by setting corresponding flag in.
     * ExecutionMonitor
     */
    public void cancelExecution() {
        if (m_progressMonitor != null) {
            m_progressMonitor.setExecuteCanceled();
            // TODO needs to come back from Monitor not just set here!
            //   but we need Node to tell us that execution was cancelled...
            m_executionCanceled = true;
        }
    }

    /**
     * @return Returns whether the node is currently executed
     */
    public synchronized boolean isExecuting() {
        return m_executionRunning;
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

    //////////////////////////
    // Event Listener handling
    //////////////////////////

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
     * Removes a listener, has no effect if the listener was not registered.
     * 
     * @param listener The listener to remove
     */
    public void removeListener(final NodeStateListener listener) {
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
     * Callback from node (this <code>NodeContainer</code> has registered
     * itself as a listener to the underlying <code>Node</code>), indicating
     * that the underlying node has changed its state. Fire new event to all
     * listeners with the node-ID added.
     * 
     * @param state Indicates the type of status change of this node.
     * @param id identifier from <code>Node</code>: will be overwritten with
     *   NodeContainer ID
     */
    public synchronized void stateChanged(final NodeStatus state,
            final int id) {

        if (state.getStatusId() == NodeStatus.END_EXECUTE) {
            // do not immediately forward this event. We will generate a
            // new event after we have actually set all internal flags
            // correctly. Otherwise this event may overtake the
            // processing of the return value of Node.execute()!
        } else {
            // forward all other events immediately
            notifyStateListeners(state);
        }

    }

    /**
     * Notifies all state listeners that the state of this
     * <code>NodeContainer</code> has changed.
     * Protected so that <code>MetaNodeContainer</code> can use it.
     * 
     * @param state <code>NodeStateListener</code>
     */
    protected void notifyStateListeners(final NodeStatus state) {
        // for all listeners
        for (int i = 0; i < m_eventListeners.size(); i++) {
            NodeStateListener listener = m_eventListeners.get(i);
            listener.stateChanged(state, m_id);
        }
    }
    
    /**
     * Returns the node status object of this <code>Node</code>.
     * 
     * @return the node status
     */
    public NodeStatus getNodeStatus() {

        return m_node.getNodeStatus();
    }
}
