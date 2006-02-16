/*
 * @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * --------------------------------------------------------------------- *
 *   This source code, its documentation and all appendant files         *
 *   are protected by copyright law. All rights reserved.                *
 *                                                                       *
 *   Copyright, 2003 - 2006                                              *
 *   Universitaet Konstanz, Germany.                                     *
 *   Lehrstuhl fuer Angewandte Informatik                                *
 *   Prof. Dr. Michael R. Berthold                                       *
 *                                                                       *
 *   You may not modify, publish, transmit, transfer or sell, reproduce, *
 *   create derivative works from, distribute, perform, display, or in   *
 *   any way exploit any of the content, in whole or in part, except as  *
 *   otherwise expressly permitted in writing by the copyright owner.    *
 * --------------------------------------------------------------------- *
 */
package de.unikn.knime.core.node;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.UIManager;

import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.eclipseUtil.GlobalClassCreator;
import de.unikn.knime.core.node.property.hilite.HiLiteHandler;

/**
 * Implementation of a node as basic processing unit within the workflow. A
 * <code>Node</code> object is the place where the data flow starts, ends, or
 * intersects. Thus a <code>Node</code> can be connected with predecessor and
 * successors through its input and output ports, <code>NodeInPort</code>
 * resp. <code>NodeOutPort</code>. There are data ports for exchanging data
 * tables, and prediction model ports for transfering computed data models.
 * <p>
 * A node contains a <code>NodeModel</code>, <code>NodeView</code>, and
 * <code>NodeDialogPane</code> implementing the Model-View-Controller
 * paradigm. The node manages the interactions between these components and
 * handles all internal and external data flows. Incoming data is passed to the
 * <code>NodeModel</code> and forwarded from there to the node's ports.
 * <p>
 * The Node is the part within a workflow holding and managing the user specific
 * NodeModel, NodeDialogPane, and possibly NodeView, thus, it is not intended to
 * extend this class. A <code>NodeFactory</code> is used to bundle nodemodel,
 * view and dialogpane. This factory is passed to the node constructor to create
 * a node of that specific type.
 * <p>
 * 
 * @author Thomas Gabriel, University of Konstanz
 * 
 * @see NodeFactory
 * @see NodeModel
 * @see NodeView
 * @see NodeDialogPane
 */
public class Node {

    /** The key under which the factory name is stored in the config. */
    public static final String CONFIG_FACTORY_KEY = "factory";

    /** The node logger for this class. */
    private final NodeLogger m_logger;

    /** The node's name. */
    private String m_nodeName;

    /** The factory used to create model, dialog, and views. */
    private final NodeFactory m_nodeFactory;

    /** The node's assigned node model. */
    private final NodeModel m_nodeModel;

    /** The node's dialog or <code>null</code> if not available. */
    private NodeDialogPane m_nodeDialogPane;

    /** Holds the current node status number and message. */
    private NodeStatus m_nodeStatus;

    /** Keeps fixed array of input ports for data. */
    private final DataInPort[] m_inDataPorts;

    /** Keeps fixed array of output ports for data. */
    private final DataOutPort[] m_outDataPorts;

    /** Keeps fixed array of input ports for models. */
    private final PredictorInPort[] m_inModelPorts;

    /** Keeps fixed array of output ports for models. */
    private final PredictorOutPort[] m_outModelPorts;

    /** The listeners that are interested in node state changes. */
    private final Set<NodeStateListener> m_stateListeners;

    static {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // use the default look and feel then.
        }
    }

    /**
     * Creates a new node by retrieving the model, dialog, and views, from the
     * specified nodefactory. Also inits the input and output ports for the
     * given number of data and model port. This node is configured after
     * initialization.
     * 
     * @param nodeFactory The node's factory for the creation of model, view,
     *            and dialog.
     * @throws NullPointerException If the node factory is <code>null</code>.
     */
    public Node(final NodeFactory nodeFactory) {

        if (nodeFactory == null) {
            throw new NullPointerException("Specified Node Factory must not "
                    + "be null.");
        }
        // remember NodeFactory
        m_nodeFactory = nodeFactory;

        // keep node name
        m_nodeName = m_nodeFactory.getNodeName().intern();

        // keep node model
        m_nodeModel = m_nodeFactory.createNodeModel();

        // register logger
        m_logger = NodeLogger.getLogger(m_nodeName);

        // do not instanciate dialog pane at the beginning
        m_nodeDialogPane = null;

        // init state listener array
        m_stateListeners = Collections
                .synchronizedSet(new HashSet<NodeStateListener>());

        // init data input ports
        m_inDataPorts = new DataInPort[m_nodeModel.getNrDataIns()];
        for (int i = 0; i < m_inDataPorts.length; i++) {
            m_inDataPorts[i] = new DataInPort(i, this);
        }

        // init model input ports
        m_inModelPorts = new PredictorInPort[m_nodeModel.getNrModelIns()];
        for (int i = 0; i < m_inModelPorts.length; i++) {
            m_inModelPorts[i] = new PredictorInPort(i + getNrDataInPorts(),
                    this);
        }

        // init data output ports
        m_outDataPorts = new DataOutPort[m_nodeModel.getNrDataOuts()];
        for (int i = 0; i < m_outDataPorts.length; i++) {
            m_outDataPorts[i] = new DataOutPort(i);
            m_outDataPorts[i].setDataTable(null);
            m_outDataPorts[i].setHiLiteHandler(m_nodeModel
                    .getOutHiLiteHandler(i));
            // DataTableSpecs will be set later through 'configureNode()'
        }

        // init model output ports
        m_outModelPorts = new PredictorOutPort[m_nodeModel.getNrModelOuts()];
        for (int i = 0; i < m_outModelPorts.length; i++) {
            m_outModelPorts[i] = new PredictorOutPort(i + getNrDataOutPorts());
            m_outModelPorts[i].setPredictorParams(null);
        }

        // let the model create its 'default' table specs
        try {
            configureNode();
        } catch (InvalidSettingsException ise) {
            // do nothing
        }

    } // Node(NodeFactory)

    /**
     * Returns the name for this node.
     * 
     * @return The node's name.
     */
    public String getNodeName() {
        return m_nodeName;
    }

    /**
     * A detailed description of this node as html.
     * 
     * @return A html page containing the node's detailed description.
     * @see de.unikn.knime.core.node.NodeFactory#getNodeFullHTMLDescription
     */
    public String getFullHTMLDescription() {
        return m_nodeFactory.getNodeFullHTMLDescription();
    }

    /**
     * A short description of this node.
     * 
     * @return A single line containing a brief node description.
     * @see de.unikn.knime.core.node.NodeFactory#getNodeOneLineDescription
     */
    public String getOneLineDescription() {
        return m_nodeFactory.getNodeOneLineDescription();
    }

    /**
     * Sets a new name for this node.
     * 
     * @param nodeName The node's new name.
     * @throws NullPointerException If the name is <code>null</code>.
     */
    public void setNodeName(final String nodeName) {
        m_nodeName = nodeName.intern();
    }

    /**
     * @return The total number of input ports (data + model ports).
     */
    public int getNrInPorts() {
        return getNrDataInPorts() + getNrPredictorInPorts();
    }

    /**
     * @return The total number of output ports (data + model ports).
     */
    public int getNrOutPorts() {
        return getNrDataOutPorts() + getNrPredictorOutPorts();
    }

    /**
     * @return The number of input ports for data.
     */
    private int getNrDataInPorts() {
        return m_inDataPorts.length;
    }

    /**
     * @return The number of input ports for <code>PredictorParams</code>.
     */
    private int getNrPredictorInPorts() {
        return m_inModelPorts.length;
    }

    /**
     * Returns the input port for the given ID.
     * 
     * @param inPortID The input port's ID.
     * @return Input port for the specified ID.
     * @throws IndexOutOfBoundsException If the ID is out of range.
     */
    public NodeInPort getNodeInPort(final int inPortID) {
        boundInPort(inPortID);
        if (inPortID < m_inDataPorts.length) {
            return m_inDataPorts[inPortID];
        } else {
            return m_inModelPorts[inPortID - m_inDataPorts.length];
        }
    }

    /**
     * @return The number of data output ports.
     */
    private int getNrDataOutPorts() {
        return m_outDataPorts.length;
    }

    /**
     * @return The number of <code>PredictorParams</code> output ports.
     */
    private int getNrPredictorOutPorts() {
        return m_outModelPorts.length;
    }

    /**
     * Checks if the port with the specified ID is a data port.
     * 
     * @param id Port ID.
     * @return <code>true</code> if the port with the specified ID is a data
     *         port.
     */
    public boolean isDataInPort(final int id) {
        if ((0 <= id) && (id < getNrDataInPorts())) {
            return true;
        }
        return false;
    }

    /**
     * Checks if the port with the specified ID is a data port.
     * 
     * @param id Port ID.
     * @return <code>true</code> if the port with the specified ID is a data
     *         port.
     */
    public boolean isDataOutPort(final int id) {
        if ((0 <= id) && (id < getNrDataOutPorts())) {
            return true;
        }
        return false;
    }

    /**
     * Checks if the port with the specified ID is a PredictorParams port.
     * 
     * @param id Port ID.
     * @return <code>true</code> if the port with the specified ID is a
     *         PredictorParams port.
     */
    public boolean isPredictorInPort(final int id) {
        if ((getNrDataInPorts() <= id) && (id < getNrInPorts())) {
            return true;
        }
        return false;
    }

    /**
     * Checks if the port with the specified ID is a PredictorParams port.
     * 
     * @param id Port ID.
     * @return <code>true</code> if the port with the specified ID is a
     *         PredictorParams port.
     */
    public boolean isPredictorOutPort(final int id) {
        if ((getNrDataOutPorts() <= id) && (id < getNrOutPorts())) {
            return true;
        }
        return false;
    }

    /**
     * Returns the output port for the given <code>outPortID</code>. The
     * first ports are by definition for data the following ports
     * <code>PredictorParams</code>.
     * 
     * @param outPortID The output port's ID.
     * @return Output port with the specified ID.
     * @throws IndexOutOfBoundsException If the index is out of range.
     */
    public NodeOutPort getNodeOutPort(final int outPortID) {
        boundOutPort(outPortID);
        if (outPortID < m_outDataPorts.length) {
            return m_outDataPorts[outPortID];
        } else {
            return m_outModelPorts[outPortID - m_outDataPorts.length];
        }
    }

    /**
     * Delegation method to the model's <code>getInputDescription()</code>
     * method.
     * 
     * @param portID The port id of interest
     * @return The description to that port
     * @throws IndexOutOfBoundsException If argument is out of range.
     * @see NodeModel#getInputDescription(int)
     */
    public String getInputPortDescription(final int portID) {
        boundInPort(portID);
        return m_nodeModel.getInputDescription(portID);
    }

    /**
     * Delegation method to model's <code>getOutputDescription()</code>
     * method.
     * 
     * @param portID The port id of interest.
     * @return The description to that port.
     * @throws IndexOutOfBoundsException If argument is out of range.
     * @see NodeModel#getOutputDescription(int)
     */
    public String getOutputPortDescription(final int portID) {
        boundOutPort(portID);
        return m_nodeModel.getOutputDescription(portID);
    }

    /**
     * Returns <code>true</code> if this node instance has been configured,
     * otherwise <code>false</code>. A node is configured if it has all its
     * settings required for execution.
     * 
     * @return <code>true</code> if node is configured otherwise
     *         <code>false</code>.
     * 
     * @see #executeNode()
     * @see #resetNode()
     */
    public boolean isConfigured() {
        return m_nodeModel.isConfigured();
    }

    /**
     * Returns <code>true</code> if the node is executable, i.e. all ports
     * have predecessors connected and data tables available. And, this node
     * must be configured correctly. And, it must not be executed already.
     * 
     * @return <code>true</code> if the node is executable, otherwise
     *         <code>false</code>.
     */
    public boolean isExecutable() {
        if (isExecuted()) {
            return false;
        }
        for (DataInPort inPort : m_inDataPorts) {
            if (!inPort.isConnected()) {
                return false;
            }
            if (inPort.getDataTable() == null) {
                return false;
            }
        }
        for (PredictorInPort inPort : m_inModelPorts) {
            if (!inPort.isConnected()) {
                return false;
            }
            if (inPort.getPredictorParams() == null) {
                return false;
            }
        }
        return isConfigured();
    }

    /**
     * @return <code>true</code> if the underlying <code>NodeModel</code>
     *         has been executed.
     * @see NodeModel#isExecuted()
     */
    public boolean isExecuted() {
        return m_nodeModel.isExecuted();
    }

    /**
     * Returns <code>true</code> if all input ports have a connected output
     * port from a previous node, otherwise <code>false</code>.
     * 
     * @return <code>true</code> if all input ports are connected, otherwise
     *         <code>false</code>.
     */
    public boolean isFullyConnected() {
        for (NodeInPort inPort : m_inDataPorts) {
            if (!inPort.isConnected()) {
                return false;
            }
        }
        for (NodeInPort inPort : m_inModelPorts) {
            if (!inPort.isConnected()) {
                return false;
            }
        }
        return true;
    }

    /**
     * @see #executeNode(ExecutionMonitor)
     * @return <code>true</code> if execution was successful otherwise
     *         <code>false</code>.
     */
    public boolean executeNode() {
        ExecutionMonitor exe = new ExecutionMonitor();
        NodeProgressMonitorView progView = new NodeProgressMonitorView(null,
                exe.getProgressMonitor());
        progView.setVisible(true);
        boolean ret = executeNode(exe);
        progView.setVisible(false);
        progView.dispose();
        return ret;
    }

    /**
     * Starts executing this node. If the node has been executed already, it
     * does nothing - just returns <code>true</code>.
     * 
     * 
     * Otherwise, the procedure starts executing all predecessor nodes connected
     * to an input port (which in turn recursively trigger their predecessors)
     * and calls the function <code>#execute()</code> in the model after all
     * connected nodes return successfully. If a port is not connected this
     * function returns false without executing itself (it may have executed
     * some predecessor nodes though). If a predecessor node returns false this
     * method also returns false without executing this node or any further
     * connected node.
     * 
     * @param exec The execution monitor.
     * @return <code>true</code> if execution was successful otherwise
     *         <code>false</code>.
     * @see NodeModel#isConfigured()
     * @see NodeModel#execute(DataTable[],ExecutionMonitor)
     */
    public boolean executeNode(final ExecutionMonitor exec) {

        // start message and keep start time
        final long time = System.currentTimeMillis();
        m_logger.info("Start execute");

        // reset the status object
        m_nodeStatus = null;

        // notify state listeners
        this.notifyStateListeners(new NodeStatus(NodeStatus.START_EXECUTE));

        //
        // CHECK PRECONDITIONS
        //

        // if this node has already been executed
        if (isExecuted()) {
            m_logger.assertLog(false, "Is executed");
            this.notifyStateListeners(new NodeStatus(NodeStatus.END_EXECUTE));
            return true;
        }

        // if NOT fully connected to predecessors
        if (!isFullyConnected()) {
            m_logger.assertLog(false, "Is not fully connected");
            this.notifyStateListeners(new NodeStatus(NodeStatus.END_EXECUTE));
            return false;
        }

        // if not configured
        if (!isConfigured()) {
            m_logger.assertLog(false, "Is not correctly configured");
            this.notifyStateListeners(new NodeStatus(NodeStatus.END_EXECUTE));
            return false;
        }

        // 
        // EXECUTE the underlying node's model
        //

        // retrieve all input tables
        DataTable[] inData = new DataTable[getNrDataInPorts()];
        for (int i = 0; i < inData.length; i++) {
            DataInPort inPort = m_inDataPorts[i];
            inData[i] = inPort.getDataTable();
            if (inData[i] == null) {
                m_logger.assertLog(false,
                        "Couldn't get data from predecessor (Port No." + i
                                + "). Is it not executed?!?");
                m_logger.error("failed execute");
                m_nodeStatus = new NodeStatus(NodeStatus.ERROR,
                        "Couldn't get data from predecessor (Port No." + i
                                + "). Is it not executed?!?");
                notifyStateListeners(m_nodeStatus);
                notifyStateListeners(new NodeStatus(NodeStatus.END_EXECUTE));
                return false;
            }
        }

        DataTable[] newOutData; // the new DTs from the model
        PredictorParams[] predParams; // the new output models
        try {
            // INVOKE MODEL'S EXECUTE
            newOutData = m_nodeModel.executeModel(inData, exec);

            processModelWarnings();

        } catch (CanceledExecutionException cee) {
            // execution was canceled
            m_logger.info("execute canceled");
            notifyStateListeners(new NodeStatus(NodeStatus.END_EXECUTE));
            return false;
        } catch (Exception e) {
            // execution failed
            m_logger.error("Execute failed", e);
            m_nodeStatus = new NodeStatus(NodeStatus.ERROR, "Execute failed: "
                    + e.getMessage());
            this.notifyStateListeners(m_nodeStatus);
            notifyStateListeners(new NodeStatus(NodeStatus.END_EXECUTE));
            return false;
        }

        //
        // CHECK EXECUTION RESULTS
        //

        // check the returned DataTables
        if (newOutData == null) {
            m_logger.error("Does not return data");
            m_nodeStatus = new NodeStatus(NodeStatus.ERROR,
                    "Does not return data");
            this.notifyStateListeners(m_nodeStatus);
            notifyStateListeners(new NodeStatus(NodeStatus.END_EXECUTE));
            return false;
        }
        for (int p = 0; p < getNrDataOutPorts(); p++) {
            if (newOutData[p] == null) {
                m_logger.error("Does not return data at port #" + p);
                m_nodeStatus = new NodeStatus(NodeStatus.ERROR,
                        "Does not return data at port #" + p);
                this.notifyStateListeners(m_nodeStatus);
                notifyStateListeners(new NodeStatus(NodeStatus.END_EXECUTE));
                return false;
            }
        }

        // check created predictor models (if any)
        predParams = new PredictorParams[getNrPredictorOutPorts()];
        for (int p = 0; p < predParams.length; p++) {
            // create PredictorParams to write into
            predParams[p] = new PredictorParams("predictor");
            try {
                m_nodeModel.savePredictorParams(p, predParams[p]);
            } catch (InvalidSettingsException ise) {
                m_logger.error("Predictor model couldn't be saved at port #"
                        + p, ise);
                m_nodeStatus = new NodeStatus(NodeStatus.ERROR,
                        "Predictor model couldn't be saved at port #" + p
                                + ise.getMessage());
                this.notifyStateListeners(m_nodeStatus);
                notifyStateListeners(new NodeStatus(NodeStatus.END_EXECUTE));
                return false;
            }
        }

        // print some success information
        m_logger.info("End execute (" + (System.currentTimeMillis() - time)
                / 100 / 10.0 + " sec)");

        // spread the newly available DataTable, Specs, and prediction models
        // to the successors
        for (int p = 0; p < getNrDataOutPorts(); p++) {
            m_outDataPorts[p].setDataTable(newOutData[p]);
            m_outDataPorts[p]
                    .setDataTableSpec(newOutData[p].getDataTableSpec());
        }
        for (int p = 0; p < getNrPredictorOutPorts(); p++) {
            m_outModelPorts[p].setPredictorParams(predParams[p]);
        }

        this.notifyStateListeners(new NodeStatus(NodeStatus.END_EXECUTE));
        return true;
    } // executeNode(ExecutionMonitor)

    /**
     * Checks the warnings in the model and notifies registered listeners.
     *
     */
    private void processModelWarnings() {
        
        // get the warning message if available and create a status
        // also notify all listeners
        String warningMessage = m_nodeModel.getWarningMessage();
        if (warningMessage != null) {

            m_logger.warn("Model warning message: " + warningMessage);
            m_nodeStatus = new NodeStatus(NodeStatus.WARNING, "Warning: "
                    + warningMessage);

            this.notifyStateListeners(m_nodeStatus);
            // reset the warning message
            m_nodeModel.setWarningMessage(null);
        }
    }

    /**
     * Resets this node. The method will first reset all connected successors
     * (recursively) and then call the model's <code>#resetModel()</code> in
     * order to reset the underlying model. The <code>#isExecuted()</code>
     * method will return <code>false</code> after this call.
     * 
     * @see #resetNodeWithoutConfigure()
     */
    public void resetNode() {
        m_logger.info("reset");
        try {
            resetNodeWithoutConfigure();
        } catch (Exception e) {
            m_logger.error("Reset failed", e);
            m_nodeStatus = new NodeStatus(NodeStatus.WARNING, "Reset failed: "
                    + e.getMessage());
            this.notifyStateListeners(m_nodeStatus);
        } catch (Error e) {
            m_logger.fatal("Reset failed", e);
            m_nodeStatus = new NodeStatus(NodeStatus.WARNING, "Reset failed: "
                    + e.getMessage());
            this.notifyStateListeners(m_nodeStatus);
        } finally {
            try {
                configureNode();
            } catch (InvalidSettingsException ise) {
                m_nodeStatus = new NodeStatus(NodeStatus.WARNING,
                        "Invalid settings: " + ise.getMessage());
                this.notifyStateListeners(m_nodeStatus);
            }
        }
    }

    /**
     * Resets this node with out re-configuring it. All connected nodes will be
     * reset, as well as the <code>DataTable</code>s and
     * <code>PredictParams</code> at the outports.
     */
    void resetNodeWithoutConfigure() {
        m_logger.debug("resetting without configure");
        // reset the model
        try {
            m_nodeModel.resetModel();
        } catch (Exception e) {
            m_logger
                    .error("Node model could not be reseted: " + e.getMessage());
            m_nodeStatus = new NodeStatus(NodeStatus.ERROR,
                    "Node model could not be reseted: " + e.getMessage());
            this.notifyStateListeners(m_nodeStatus);
        }
        this.notifyStateListeners(new NodeStatus(NodeStatus.RESET,
                "Not configured."));
        // first, reset the rest of the flow, starting from the leafs
        for (int p = 0; p < getNrDataOutPorts(); p++) {
            m_outDataPorts[p].resetConnected();
        }

        // blow away our data tables in the port
        for (int p = 0; p < getNrDataOutPorts(); p++) {
            // m_outDataPorts[p].setDataTableSpec(null);
            m_outDataPorts[p].setDataTable(null);
        }

        // blow away our pred models in the port
        for (int p = 0; p < getNrPredictorOutPorts(); p++) {
            m_outModelPorts[p].setPredictorParams(null);
        }

        // load PredictorParams from all available InPorts again
        for (int p = 0; p < getNrPredictorInPorts(); p++) {
            try {
                m_nodeModel.loadPredictorParams(p, m_inModelPorts[p]
                        .getPredictorParams());
            } catch (InvalidSettingsException ise) {
                // TODO (cs) do we need to throw this to the "outside"?
                m_nodeStatus = new NodeStatus(NodeStatus.ERROR,
                        "PredictorParams could not be loaded: "
                                + ise.getMessage());
                this.notifyStateListeners(m_nodeStatus);
            }
        }

        // if reset had no exception reset also the node status
        m_nodeStatus = null;
        this.notifyStateListeners(new NodeStatus(NodeStatus.STATUS_CHANGED));
    }

    /**
     * Prepares the deletion of this node by calling <code>#resetNode()</code>,
     * closing all views, disconnect all input and output ports, and all state
     * listeners are removed.
     */
    public void detachNode() {
        // reset this node first
        resetNodeWithoutConfigure();
        // close and unregister all views
        for (NodeView view : m_nodeModel.getViews()) {
            // unregisters and closes the view
            view.closeView();
            view = null;
        }
        // disconnect output ports
        for (int o = 0; o < m_outDataPorts.length; o++) {
            m_outDataPorts[o].removeAllPorts();
            m_outDataPorts[o].disposePortView();
        }
        // disconnect input ports
        for (int i = 0; i < m_inDataPorts.length; i++) {
            m_inDataPorts[i].disconnectPort();
        }
        // remove all state listeners
        m_stateListeners.clear();
    }

    /**
     * Notification method, called by an input port to tell the node about a new
     * connected outport from a predecessor. The notification is done, as the
     * node itself should be responsible to cause the required actions. In case
     * of a data in-port, the hilite handler, as well as the table specs are
     * loaded into this node. In case of a predictor model in-port the predictor
     * params are loaded into this node.
     * 
     * @param inPortID The port id that has been connected.
     * 
     * @see Node#inportHasNewHiLiteHandler
     * @see Node#inportHasNewTableSpec
     * @see Node#inportHasNewPredictorParams
     */
    void inportHasNewConnection(final int inPortID) {
        boundInPort(inPortID);
        // resetNodeWithoutConfigure();
        if (inPortID < getNrDataInPorts()) {
            // set hilitehandler in this node using the one from the predecessor
            inportHasNewHiLiteHandler(inPortID);
            // also get a new DataTableSpec, if available
            inportHasNewTableSpec(inPortID);
        } else {
            inportHasNewPredictorParams(inPortID);
        }
    }

    /**
     * Notification method, called by an input port to tell the node about a
     * disconnected outport from a predecessor. The notification is done, as the
     * node itself should be responsible to cause the required actions. The node
     * is reseted and newly configured.
     * 
     * @param inPortID The port id that just got disconnected.
     */
    void inportWasDisconnected(final int inPortID) {
        boundInPort(inPortID);
        // call reset
        resetNodeWithoutConfigure();

        if (isDataInPort(inPortID)) {
            // reset hilite handler in this node.
            // This triggers hilite handler propagation through the output ports
            setHiLiteHandler(inPortID, null);
        } else { // then this is a PredictorParams port
            /*
             * reset the PredictorParams of this inport, previously pushed in
             * and stored in this node.
             */
            int realId = inPortID - getNrDataInPorts();
            try {
                m_nodeModel.loadPredictorParams(realId, null);
            } catch (InvalidSettingsException ise) {
                /*
                 * if the nodemodel implementation of the loadPredictorParams is
                 * correct we will not end up here.
                 */
                throw new IllegalStateException("Incorrect implementation of "
                        + "method NodeModel.loadPredictorParams. "
                        + "It must handle null parameters");
            }
        }
        // re-create out table specs, as incoming table specs/models are gone.
        try {
            configureNode();
        } catch (InvalidSettingsException ise) {
            m_nodeStatus = new NodeStatus(NodeStatus.ERROR,
                    "Configure failed. Invalid settings: " + ise.getMessage());
            this.notifyStateListeners(m_nodeStatus);
        }
    }

    /**
     * Notification method, called by an input port to tell the node about a new
     * available hilte handler from a predecessor outport. The notification is
     * done, as the node itself should be responsible to cause the required
     * actions. Only in case of a data in-port the new hilite handler is set for
     * this node.
     * 
     * @param inPortID the number of the port that has a new hilite handler
     *            available.
     */
    void inportHasNewHiLiteHandler(final int inPortID) {
        // hilite handler are propagated through data ports only.
        boundDataInPort(inPortID);
        NodeInPort port = getNodeInPort(inPortID);
        setHiLiteHandler(inPortID, ((DataInPort)port).getHiLiteHandler());
    }

    /**
     * Notification method, called by an input port to tell the node about a new
     * available data table from a predecessor outport. The notification is
     * done, as the node itself should be responsible to cause the required
     * actions. At the moment nothing is done here, as the data table is loaded
     * in the <code>executeNode</code> method.
     * 
     * @param inPortID The port id that has a new data table available.
     */
    void inportHasNewDataTable(final int inPortID) {
        // data tables are propagated through data ports only
        boundDataInPort(inPortID);

    }

    /**
     * Notification method, called by an input port to tell the node about a new
     * available table spec from a predecessor outport. The notification is
     * done, as the node itself should be responsible to cause the required
     * actions. The table spec is loaded via the <code>configureNode</code>
     * method.
     * 
     * @param inPortID The port ID that has a new data table spec available.
     */
    void inportHasNewTableSpec(final int inPortID) {
        // data tables specs are propagated through data ports only
        boundDataInPort(inPortID);
        try {
            configureNode();
        } catch (InvalidSettingsException ise) {
            m_nodeStatus = new NodeStatus(NodeStatus.ERROR,
                    "Configure failed. Invalid settings: " + ise.getMessage());
            this.notifyStateListeners(m_nodeStatus);
        }
    }

    /**
     * Notification method, called by an input port to tell the node about a new
     * available predictor model from a predecessor outport. The notification is
     * done, as the node itself should be responsible to cause the required
     * actions. The predictor model is loaded into the model and the node is
     * configured afterwards.
     * 
     * @param inPortID The port ID that has a new predictor model spec
     *            available.
     */
    void inportHasNewPredictorParams(final int inPortID) {
        // Predictor params are propagated through model ports only
        boundPredParamsInPort(inPortID);
        try {
            int realId = inPortID - getNrDataInPorts();
            PredictorParams params = m_inModelPorts[realId]
                    .getPredictorParams();
            m_nodeModel.loadPredictorParams(realId, params);
        } catch (InvalidSettingsException ise) {
            m_logger.error("loadPredictorParams() failed: ", ise);
            m_nodeStatus = new NodeStatus(NodeStatus.ERROR,
                    "Could not load PredictorParams: " + ise.getMessage());
            this.notifyStateListeners(m_nodeStatus);
        }
        try {
            configureNode();
        } catch (InvalidSettingsException ise) {
            m_nodeStatus = new NodeStatus(NodeStatus.ERROR,
                    "Configure failed. Invalid settings: " + ise.getMessage());
            this.notifyStateListeners(m_nodeStatus);
        }
    }

    /**
     * Notification method, called by an input port to tell the node about a
     * reset request from a predecessor outport. The notification is done, as
     * the node itself should be responsible to cause the required actions. The
     * node is reset without calling configureNode (as the reset initiator will
     * trigger a configure later).
     * 
     * @param inPortID The port ID that has a new predictor model spec
     *            available.
     */
    void inportResetsNode(final int inPortID) {
        boundInPort(inPortID);
        resetNodeWithoutConfigure();
    }

    /**
     * Returns the <code>HiLiteHandler</code> for the given output port by
     * retrieving it from the node's <code>NodeModel</code>.
     * 
     * @param outPortID Output port's ID.
     * @return The hilite handler for the given output port.
     */
    HiLiteHandler getOutHiLiteHandler(final int outPortID) {
        // highlight handler propagate through data ports only.
        boundDataOutPort(outPortID);
        return m_nodeModel.getOutHiLiteHandler(outPortID);
    }

    /**
     * Propagates the <code>HiLiteHandler</code> from the predecessor
     * connected to the given input port to the node's model. It will then push
     * the node's HiLiteHandlers (retrieved from the model) to all connected
     * successors.
     * 
     * @param inPortID The input port index.
     * @param hiLiteHdl The hilite handler.
     */
    void setHiLiteHandler(final int inPortID, final HiLiteHandler hiLiteHdl) {
        // highlight handler are propagated through data ports only.
        boundDataInPort(inPortID);
        m_nodeModel.setInHiLiteHandler(hiLiteHdl, inPortID);
        for (int i = 0; i < m_outDataPorts.length; i++) {
            m_outDataPorts[i].setHiLiteHandler(m_nodeModel
                    .getOutHiLiteHandler(i));
        }
    }

    /**
     * Sets all (new) incoming <code>DataTableSpec</code> elements in the
     * model, calls the model to create output table specs and propagates these
     * new specs to the connected successors.
     * 
     * @throws InvalidSettingsException If configure failed due to wrong
     *             settings.
     */
    protected void configureNode() throws InvalidSettingsException {

        m_logger.info("configure");

        // reset status object to clean previous status messages.
        m_nodeStatus = null;

        // only call for re-configuration if we are not executed
        if (isExecuted()) {
            // this happens if a previous node in a row of executed ones
            // in configured, all other nodes will be re-configured
            m_logger.warn("is executed: resetting");
            resetNodeWithoutConfigure();
        }

        // configure
        try {
            // get inspecs
            DataTableSpec[] inSpecs = getInDataTableSpecs();

            //
            // call model to create output table specs
            //
            DataTableSpec[] newSpecs = m_nodeModel.configureModel(inSpecs);
            // notify state listeners
            this.notifyStateListeners(new NodeStatus(NodeStatus.CONFIGURED,
                    "Configured"));
            /*
             * set the new specs in the output ports, which will propagate them
             * to connected successor nodes
             */
            for (int p = 0; p < newSpecs.length; p++) {
                // update data table spec
                m_outDataPorts[p].setDataTableSpec(newSpecs[p]);
            }
            
            // check for model warnings
            processModelWarnings();
        } catch (InvalidSettingsException ise) {
            m_logger.debug("Configure failed: " + ise.getMessage());
            notConfigured();
            throw ise;
        } catch (Exception e) {
            m_logger.error("Configure failed", e);
            notConfigured();
            throw new InvalidSettingsException(e.getMessage());
        } catch (Error e) {
            m_logger.fatal("Configure failed", e);
            notConfigured();
            throw new InvalidSettingsException(e.getMessage());
        }
    }

    /**
     * Called if configured failed. Send state changed event to all listeners
     * and propagates <code>null</code> data table specs.
     */
    private void notConfigured() {
        // notify state listeners
        this.notifyStateListeners(new NodeStatus(NodeStatus.CONFIGURED));
        // set null specs in the output ports (they will propagate it to
        // connected successor nodes)
        for (int p = 0; p < getNrDataOutPorts(); p++) {
            // update data table spec
            m_outDataPorts[p].setDataTableSpec(null);
        }
    }

    /**
     * Returns the in data table specs for this node. If incoming table spec is
     * null, an empty spec will be created and returned.
     * 
     * @return An array of non-null DataTableSpec objects.
     */
    DataTableSpec[] getInDataTableSpecs() {
        DataTableSpec[] specs = new DataTableSpec[getNrDataInPorts()];
        for (int i = 0; i < specs.length; i++) {
            specs[i] = m_inDataPorts[i].getDataTableSpec();
            if (specs[i] == null) {
                specs[i] = new DataTableSpec();
            }
        }
        return specs;
    }

    /**
     * @return The number of available views.
     */
    public int getNumViews() {
        return m_nodeFactory.getNrNodeViews();
    }

    /**
     * Returns the name for this node's view at the given index.
     * 
     * @param viewIndex The view index.
     * @return The view's name.
     * @throws ArrayIndexOutOfBoundsException If the view index is out of range.
     */
    public String getViewName(final int viewIndex) {
        return m_nodeFactory.getNodeViewName(viewIndex);
    }

    /**
     * Opens the node's view.
     * 
     * @param viewIndex The view's index to show.
     */
    public void showView(final int viewIndex) {
        try {
            getView(viewIndex).openView();
        } catch (Exception e) {
            m_logger.error("Show view failed", e);
            m_nodeStatus = new NodeStatus(NodeStatus.ERROR,
                    "View could not be opened, reason: " + e.getMessage());
            this.notifyStateListeners(m_nodeStatus);
        } catch (Error e) {
            m_logger.fatal("Show view failed", e);
            m_nodeStatus = new NodeStatus(NodeStatus.ERROR,
                    "View could not be opened, reason: " + e.getMessage());
            this.notifyStateListeners(m_nodeStatus);
        }
    }

    /**
     * Return a new instance of the node's view (without opening it).
     * 
     * @param viewIndex The view's index to show up.
     * @return The node view with the specified index.
     * @throws ArrayIndexOutOfBoundsException If the view index is out of range.
     */
    public NodeView getView(final int viewIndex) {
        NodeView view = m_nodeFactory.createNodeView(viewIndex, m_nodeModel);
        return view;
    }

    /**
     * @return <code>true</code> if a dialog is available.
     */
    public boolean hasDialog() {
        return m_nodeFactory.hasDialog();
    }

    /**
     * Opens the node's dialog and loads the current settings from the model
     * into the dialog.
     */
    public void showDialog() {
        try {
            if (hasDialog()) {
                NodeDialog dlg = new NodeDialog(getDialogPane());
                dlg.openDialog();
            }
        } catch (Exception e) {
            m_logger.error("show dialog failed", e);
            m_nodeStatus = new NodeStatus(NodeStatus.ERROR,
                    "Dialog could not be opend properly: " + e.getMessage());
            this.notifyStateListeners(m_nodeStatus);
        } catch (Error e) {
            m_logger.fatal("show dialog failed", e);
            m_nodeStatus = new NodeStatus(NodeStatus.ERROR,
                    "Dialog could not be opend properly: " + e.getMessage());
            this.notifyStateListeners(m_nodeStatus);
        }
    }

    /**
     * @return The dialog pane which holds all the settings' components. In
     *         addition this method loads the settings from the model into the
     *         dialog pane. The pane might be <code>null</code> if no dialog
     *         is available.
     * @see #hasDialog()
     */
    public NodeDialogPane getDialogPane() {
        if (hasDialog()) {
            // init dialog pane, if not done yet.
            if (m_nodeDialogPane == null) {
                m_nodeDialogPane = m_nodeFactory.createNodeDialogPane();
                m_nodeDialogPane.setNode(this);
            }
            // load settings into the dialog
            loadDialogSettingsFromModel();
            // return the dialog pane
            return m_nodeDialogPane;
        }
        assert false : "Can't return dialog pane, node has no dialog!";
        return null;
    }

    /**
     * Writes the current <code>Node</code> and <code>NodeModel</code>
     * settings into the given <code>NodeSettings</code> object. These are the
     * node name, and in- and out port names. Also calls the
     * <code>#saveConfig()</code> method inside the <code>NodeModel</code>
     * to append the model's settings to the given <code>NodeSettings</code>
     * object.
     * 
     * @param settings The object to write the node's settings into.
     */
    public void saveConfigTo(final NodeSettings settings) {
        // write node factory's class name (including package)
        settings.addString(CONFIG_FACTORY_KEY, m_nodeFactory.getClass()
                .getName());
        // write node name
        settings.addString("name", m_nodeName);
        // write configuration settings
        settings.addBoolean("isConfigured", this.isConfigured());
        // write inports
        final NodeSettings inport = settings.addConfig("inports");
        for (int i = 0; i < m_inDataPorts.length; i++) {
            inport.addString("" + i, m_inDataPorts[i].getPortName());
        }
        // write outports
        final NodeSettings outport = settings.addConfig("outports");
        for (int i = 0; i < m_outDataPorts.length; i++) {
            outport.addString("" + i, m_outDataPorts[i].getPortName());
        }
        // write model
        final NodeSettings model = settings.addConfig("model");
        try {
            m_nodeModel.saveSettingsTo(model);
        } catch (Exception e) {
            m_logger.error("Could not save model", e);
        } catch (Error e) {
            m_logger.fatal("Could not save model", e);
        }
    }

    /**
     * Loads specific <code>NodeSettings</code> and inits this
     * <code>Node</code> with ports, model, and in- and outports. Also calls
     * the model's <code>#loadConfig()</code> method to load the settings into
     * the model. A configure will be executed, if the node was configured
     * before.
     * 
     * @param settings The object the read the node's settings from.
     * 
     * @throws InvalidSettingsException If one property is not available.
     */
    public void loadConfigFrom(final NodeSettings settings)
            throws InvalidSettingsException {
        // read node name
        m_nodeName = settings.getString("name");
        final NodeSettings inport = settings.getConfig("inports");
        // read inports
        for (int i = 0; i < m_inDataPorts.length; i++) {
            m_inDataPorts[i].setPortName(inport.getString("" + i));
        }
        // read outports
        final NodeSettings outport = settings.getConfig("outports");
        for (int i = 0; i < m_outDataPorts.length; i++) {
            m_outDataPorts[i].setPortName(outport.getString("" + i));
        }
        // read model and load settings
        try {
            // try to load configuration as well
            m_nodeModel.loadSettingsFrom(settings.getConfig("model"));
        } catch (InvalidSettingsException ise) {
            // read configuration settings
            boolean wasConfigured = settings.getBoolean("isConfigured", false);
            // TODO (mb) can be removed at some point when flag is standard
            if (wasConfigured) { // only pass exception is note was
                // configured
                m_logger.warn("could not load settings: " + ise.getMessage());
            }
        } catch (Exception e) {
            m_logger.error("could not load settings", e);
            throw new InvalidSettingsException(e.getMessage());
        } catch (Error e) {
            m_logger.fatal("could not load settings", e);
            throw new InvalidSettingsException(e.getMessage());
        } finally {
            // update data table specs
            try {
                configureNode();
            } catch (InvalidSettingsException ise) {
                m_nodeStatus = new NodeStatus(NodeStatus.ERROR, ise
                        .getMessage());
                this.notifyStateListeners(m_nodeStatus);
            }
        }
    }

    /**
     * Static method to create a new <code>Node</code> based on a
     * <code>NodeSettings</code>.
     * 
     * @param settings The object to read the node's settings from.
     * @return Newly created <code>Node</code>, initialized using the
     *         settings object.
     * @throws InvalidSettingsException If one property is not available.
     */
    public static Node createNode(final NodeSettings settings)
            throws InvalidSettingsException {
        Node newNode = new Node(createNodeFactory(settings)); // create new
        // node
        newNode.loadConfigFrom(settings); // load remaining settings
        return newNode; // return fully initialized Node
    }

    /**
     * Creates a new <code>NodeFactory</code> from the given settings object.
     * 
     * @param settings The settings describing a factory.
     * @return The constructed factory.
     * @throws InvalidSettingsException if the settings are invalid
     */
    protected static NodeFactory createNodeFactory(final NodeSettings settings)
            throws InvalidSettingsException {

        // read node factory class name
        String factoryClassName = settings.getString(CONFIG_FACTORY_KEY);
        // and try to call it's constructor
        NodeFactory newFactory;
        try {
            // use global Class Creator utility for Eclipse "compatibility"
            newFactory = (NodeFactory)((GlobalClassCreator
                    .createClass(factoryClassName)).newInstance());
        } catch (Exception e) {
            throw new InvalidSettingsException("Factory could not be loaded "
                    + factoryClassName + " reason: " + e.getMessage());
        }

        return newFactory;
    }

    /**
     * Validates the settings inside the model.
     * 
     * @throws InvalidSettingsException If not valid.
     */
    void validateModelSettingsFromDialog() throws InvalidSettingsException {
        // save new dialog's config into new object
        NodeSettings newSettings = new NodeSettings(this.getNodeName());
        m_nodeDialogPane.saveSettingsTo(newSettings);
        // validate settings
        m_nodeModel.validateSettings(newSettings);
    }

    /**
     * Reads the current settings from the dialog and writes them into the
     * model.
     * 
     * @throws InvalidSettingsException If the settings are not valid for the
     *             underlying model.
     */
    void loadModelSettingsFromDialog() throws InvalidSettingsException {
        // save new dialog's config into new object
        NodeSettings newSettings = new NodeSettings(this.getNodeName());
        m_nodeDialogPane.saveSettingsTo(newSettings);
        // and apply it to the model
        m_nodeModel.loadSettingsFrom(newSettings);
        // update the output data table specs
        configureNode();
    }

    /**
     * Reads the current settings from the model and load them into the dialog
     * pane.
     */
    private void loadDialogSettingsFromModel() {
        // get the model's current settings ...
        NodeSettings currSettings = new NodeSettings(this.getNodeName());
        m_nodeModel.saveSettingsTo(currSettings);
        // ... to init the dialog
        m_nodeDialogPane.loadSettingsFrom(currSettings, getInDataTableSpecs());
    }

    /**
     * Checks range for given input port ID.
     * 
     * @param inPortID The input port ID to check.
     * @return Given input port ID if in range.
     * @throws IndexOutOfBoundsException if the specified ID is not an input
     *             port ID.
     */
    private int boundInPort(final int inPortID) {
        if ((inPortID < 0) || (inPortID >= getNrInPorts())) {
            throw new IndexOutOfBoundsException("Invalid input-port number: "
                    + inPortID + " (valid range: [0..." + (getNrInPorts() - 1)
                    + "])");
        }
        return inPortID;
    }

    /**
     * Checks range for given output port ID.
     * 
     * @param outPortID The output port ID to check.
     * @return Given output port ID if in range.
     * @throws IndexOutOfBoundsException if the specified ID is not an output
     *             port ID.
     */
    private int boundOutPort(final int outPortID) {
        if ((outPortID < 0) || (outPortID >= getNrOutPorts())) {
            throw new IndexOutOfBoundsException("Invalid output-port number: "
                    + outPortID + " (valid range: [0..."
                    + (getNrOutPorts() - 1) + "])");
        }
        return outPortID;
    }

    /**
     * Checks range for given input data port ID.
     * 
     * @param inDataPortID The input port ID to check.
     * @return Given input port ID if in range.
     * @throws IndexOutOfBoundsException if the specified ID is not a data input
     *             port ID.
     */
    private int boundDataInPort(final int inDataPortID) {
        if ((inDataPortID < 0) || (inDataPortID >= getNrDataInPorts())) {
            throw new IndexOutOfBoundsException("Invalid input-port number: "
                    + inDataPortID + " (valid range: [0..."
                    + (getNrDataInPorts() - 1) + "])");
        }
        return inDataPortID;
    }

    /**
     * Checks range for given input data port ID.
     * 
     * @param outDataPortID The output port ID to check.
     * @return Given output port ID if in range.
     * @throws IndexOutOfBoundsException if the specified ID is not a data
     *             output port ID.
     */
    private int boundDataOutPort(final int outDataPortID) {
        if ((outDataPortID < 0) || (outDataPortID >= getNrDataOutPorts())) {
            throw new IndexOutOfBoundsException("Invalid output-port number: "
                    + outDataPortID + " (valid range: [0..."
                    + (getNrDataOutPorts() - 1) + "])");
        }
        return outDataPortID;
    }

    // /**
    // * Checks range for given output port ID.
    // *
    // * @param outPortID The output port ID to check.
    // * @return Given params output port ID if in range.
    // * @throws IndexOutOfBoundsException if the specified ID is not a pred
    // * params output port ID.
    // */
    // private int boundPredParamsOutPort(final int outPortID) {
    // if ((outPortID < getNrDataOutPorts())
    // || (outPortID >= getNrPredictorOutPorts() + getNrDataOutPorts())) {
    //
    // throw new IndexOutOfBoundsException(
    // "Invalid predictor params output-port number: "
    // + outPortID
    // + " (valid range: ["
    // + getNrDataOutPorts()
    // + "..."
    // + (getNrPredictorOutPorts()
    // + getNrDataOutPorts() - 1)
    // + "])");
    // }
    // return outPortID;
    // }

    /**
     * Checks range for given input port ID.
     * 
     * @param The input port ID to check.
     * @return Given input port ID if in range.
     * @throws IndexOutOfBoundsException if the specified ID is not a pred
     *             params input port ID.
     */
    private int boundPredParamsInPort(final int inPortID) {
        // predictor params port ids are the indecies above the data port ids
        if ((inPortID < getNrDataInPorts())
                || (inPortID >= getNrPredictorInPorts() + getNrDataInPorts())) {
            throw new IndexOutOfBoundsException(
                    "Invalid predictor params input-port number: "
                            + inPortID
                            + " (valid range: ["
                            + getNrDataInPorts()
                            + "..."
                            + (getNrPredictorInPorts() + getNrDataInPorts() - 1)
                            + "])");
        }
        return inPortID;
    }

    /**
     * Adds a state listener to this node. Ignored, if the listener is already
     * registered.
     * 
     * @param listener The listener to add.
     */
    public void addStateListener(final NodeStateListener listener) {
        if (!m_stateListeners.add(listener)) {
            m_logger.debug("listener already registered: " + listener);
        }
    }

    /**
     * Removes a state listener from this node. Ignored, if the listener is not
     * registered.
     * 
     * @param listener The listener to remove.
     */
    public void removeStateListener(final NodeStateListener listener) {
        if (!m_stateListeners.remove(listener)) {
            m_logger.debug("listener was not registered: " + listener);
        }
    }

    /**
     * Notifies all state listeners that the state of this node has changed.
     * 
     * @param state The status object.
     */
    protected synchronized void notifyStateListeners(final NodeStatus state) {
        synchronized (m_stateListeners) {
            for (NodeStateListener listener : m_stateListeners) {
                listener.stateChanged(state, -1);
            }
        }
    }

    /**
     * Returns a string summary of this node.
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "Node @" + hashCode() + " [" + m_nodeName + ";in="
                + m_inDataPorts.length + ";out=" + m_outDataPorts.length
                + ";fullyConnected=" + isFullyConnected() + ";configured="
                + isConfigured() + ";executable=" + isExecutable()
                + ";executed=" + isExecuted() + "]";
    }

    /**
     * @return The status object of this <code>Node</code>.
     */
    public NodeStatus getNodeStatus() {
        return m_nodeStatus;
    }

    /**
     * @return the <code>NodeFactory</code> that constructed this node.
     */
    public NodeFactory getNodeFactory() {
        return m_nodeFactory;
    }

    /**
     * @return returns the node model of this node
     */
    protected NodeModel getNodeModel() {
        return m_nodeModel;
    }

} // Node
