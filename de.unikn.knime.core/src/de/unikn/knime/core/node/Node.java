/* --------------------------------------------------------------------- *
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
 * 
 * History
 *   17.01.2006(sieb, ohl): reviewed 
 */
package de.unikn.knime.core.node;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.UIManager;

import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.NodeFactory.NodeType;
import de.unikn.knime.core.node.interrupt.InterruptibleNodeModel;
import de.unikn.knime.core.node.meta.MetaInputModel;
import de.unikn.knime.core.node.meta.MetaNodeModel;
import de.unikn.knime.core.node.property.hilite.HiLiteHandler;
import de.unikn.knime.core.node.workflow.WorkflowManager;
import de.unikn.knime.core.util.FileUtil;

/**
 * Implementation of a node as basic processing unit within the workflow. A
 * Node object is the place where the data flow starts, ends, or intersects. 
 * Thus a Node can be connected with predecessors and successors through its 
 * input and output ports, {@link de.unikn.knime.core.node.NodeInPort} and
 * {@link de.unikn.knime.core.node.NodeOutPort}, respectively. There are data
 * ports for exchanging data tables, and prediction model ports for transfering
 * computed data models. <br />
 * A node must contain a {@link NodeModel} and may contain {@link NodeView}s
 * and a {@link NodeDialogPane} implementing the Model-View-Controller paradigm.
 * The node manages the interactions between these components and handles all
 * internal and external data flows. Incoming data is passed to the
 * {@link NodeModel} and forwarded from there to the node's ports. <br />
 * The <code>Node</code> is the part within a workflow holding and managing
 * the user specific {@link NodeModel}, {@link NodeDialogPane}, and possibly
 * {@link NodeView}, thus, it is not intended to extend this class. A
 * {@link NodeFactory} is used to bundle model, view and dialog. This factory is
 * passed to the node constructor to create a node of that specific type.
 * 
 * @author Thomas Gabriel, University of Konstanz
 * 
 */
public final class Node {

    /** The node logger for this class. */
    private final NodeLogger m_logger;

    /** The node's name. */
    private String m_name;

    /** The factory used to create model, dialog, and views. */
    private final NodeFactory m_factory;

    /** The node's assigned node model. */
    private final NodeModel m_model;

    /** The node's dialog or <code>null</code> if not available. */
    private NodeDialogPane m_dialogPane;

    /** Holds the current node status number and message. */
    private NodeStatus m_status;

    /** Keeps fixed array of input ports for data. */
    private final DataInPort[] m_inDataPorts;

    /** Keeps fixed array of output ports for data. */
    private final DataOutPort[] m_outDataPorts;

    /** Keeps fixed array of input ports for models. */
    private final ModelContentInPort[] m_inModelPorts;

    /** Keeps fixed array of output ports for models. */
    private final ModelContentOutPort[] m_outModelPorts;

    /** The listeners that are interested in node state changes. */
    private final Set<NodeStateListener> m_stateListeners;
    
    /** Node settings XML file name. */
    public static final String SETTINGS_FILE_NAME = "settings.xml";

    /** Data config files, it contains the paths to the data files. */
    public static final String DATA_FILE_NAME = "data.xml";
    
    /** Directory name to save and load node internals. */
    private static final String INTERN_FILE_DIR = "internal";
    
    /** Directory name to save and load the data. */
    private static final String DATA_FILE_DIR = "data";
    
    private File m_nodeDir = null;

    /** Store when the current output data has been stored (to avoid 
     * uneccesary re-save). Will be set when saved, will be unset upon reset.
     */
    private boolean m_isCurrentlySaved = false;

    static {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // use the default look and feel then.
        }
    }
    
    /**
     * Creates a new node by retrieving the model, dialog, and views, from the
     * specified <code>NodeFactory</code>. Also inits the input and output
     * ports for the given number of data and model port. This node is
     * configured after initialization.
     * 
     * @param nodeFactory the node's factory for the creation of model, view,
     *            and dialog
     * @throws NullPointerException if the node factory is <code>null</code>
     */
    public Node(final NodeFactory nodeFactory) {
        this(nodeFactory, (WorkflowManager) null);
    }
    
    /**
     * Creates a new node by retrieving the model, dialog, and views, from the
     * specified <code>NodeFactory</code>. Also inits the input and output
     * ports for the given number of data and model port. This node is
     * configured after initialization.
     * 
     * @param nodeFactory the node's factory for the creation of model, view,
     *            and dialog
     * @param wfm the workflow manager that is responsible for this node;
     * maybe <code>null</code>
     * @throws IllegalArgumentException 
     *         If the <i>nodeFactory</i> is <code>null</code>.
     */
    public Node(final NodeFactory nodeFactory, final WorkflowManager wfm) {

        if (nodeFactory == null) {
            throw new IllegalArgumentException("NodeFactory must not be null.");
        }
        // remember NodeFactory
        m_factory = nodeFactory;

        // keep node name
        m_name = m_factory.getNodeName().intern();

        // keep node model
        m_model = m_factory.callCreateNodeModel();

        // register logger
        m_logger = NodeLogger.getLogger(m_name);

        // do not instanciate dialog pane at the beginning
        m_dialogPane = null;

        // init state listener array
        m_stateListeners = Collections
                .synchronizedSet(new HashSet<NodeStateListener>());

        // init data input ports
        m_inDataPorts = new DataInPort[m_model.getNrDataIns()];
        for (int i = 0; i < m_inDataPorts.length; i++) {
            m_inDataPorts[i] = new DataInPort(i, this);
            m_inDataPorts[i].setPortName(m_factory.getInportDataName(i));
        }

        // init model input ports
        m_inModelPorts = new ModelContentInPort[m_model.getNrModelIns()];
        for (int i = 0; i < m_inModelPorts.length; i++) {
            m_inModelPorts[i] = new ModelContentInPort(i + getNrDataInPorts(),
                    this);
            m_inModelPorts[i].setPortName(m_factory.getInportModelName(i));
        }

        // init data output ports
        m_outDataPorts = new DataOutPort[m_model.getNrDataOuts()];
        for (int i = 0; i < m_outDataPorts.length; i++) {
            m_outDataPorts[i] = new DataOutPort(i, this);
            m_outDataPorts[i].setPortName(m_factory.getOutportDataName(i));
            m_outDataPorts[i].setDataTable(null);
            m_outDataPorts[i].setHiLiteHandler(m_model.getOutHiLiteHandler(i));
            // DataTableSpecs will be set later through #configure()
        }

        // init model output ports
        m_outModelPorts = new ModelContentOutPort[m_model.getNrModelOuts()];
        for (int i = 0; i < m_outModelPorts.length; i++) {
            m_outModelPorts[i] = 
                new ModelContentOutPort(i + getNrDataOutPorts(), this);
            m_outModelPorts[i].setPortName(m_factory.getOutportModelName(i));
            m_outModelPorts[i].setModelContent(null);
        }

        if (m_model instanceof SpecialNodeModel) {
            ((SpecialNodeModel) m_model).setNode(this);
            ((SpecialNodeModel) m_model).setInternalWFM(wfm.createSubManager());
        }
        
        // let the model create its 'default' table specs
        configure();
    }
    
    
    
    /**
     * Creates a copy of the passed node.
     * 
     * @param node the node that should be copied
     * @param wfm the workflow manager that is responsible for this node;
     * maybe <code>null</code>
     */
    public Node(final Node node, final WorkflowManager wfm) {
        this (node.m_factory, wfm);
        NodeSettings modelSettings = new NodeSettings("modelSettings");
        node.m_model.saveSettingsTo(modelSettings);
        try {
            this.m_model.loadSettingsFrom(modelSettings);
        } catch (InvalidSettingsException ise) {
            m_logger.error("Could not copy node, reason: " + ise.getMessage());
        } finally {
            resetAndConfigure();            
        }
    }

 
    /**
     * Loads the settings (but not the data) from the given settings object.
     * 
     * @param settings a settings object
     * @throws InvalidSettingsException if an expected setting is missing
     */
    public void loadSettings(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        m_name = settings.getString(CFG_NAME);

        NodeSettingsRO modelSettings = settings.getNodeSettings(CFG_MODEL);
        if (m_model instanceof SpecialNodeModel) {
            ((SpecialNodeModel) m_model).loadSettingsFrom(null, modelSettings,
                    null);
        } else {
            m_model.loadSettingsFrom(modelSettings);
        }            
    }
    
    
    /**
     * Loads the node settings and internal structures from the given location,
     * depending on the node's state, configured or executed.
     * @param loadID Forwared to the node. This id serves as loading id, 
     * it helps to distinguish between two workflows being loaded at the same
     * time. This id is passed on to the 
     * {@link de.unikn.knime.core.node.BufferedDataTable#getDataTable(
     * int, Integer)}.
     * @param nodeFile The node settings location.
     * @param exec The execution monitor reporting progress during reading
     *        structure.
     * @throws IOException If the node settings file can't be found or read.
     * @throws InvalidSettingsException If the settings are wrong.
     * @throws CanceledExecutionException If loading was canceled. 
     */
    public void load(final int loadID, final File nodeFile, 
            final ExecutionContext exec) throws IOException, 
            InvalidSettingsException, CanceledExecutionException {
        assert exec != null;
        m_status = null;
        if (!nodeFile.isFile() || !nodeFile.canRead()) {
            m_model.setExecuted(false);
            throw new IOException(SETTINGS_FILE_NAME + " can't be read: " 
                    + nodeFile);
        }

        // load node settings
        NodeSettingsRO settings = 
            NodeSettings.loadFromXML(new FileInputStream(nodeFile));

        // read node name
        m_name = settings.getString(CFG_NAME);

        // read configured flag
        boolean wasConfigured = settings.getBoolean(CFG_ISCONFIGURED);
        m_model.setConfigured(wasConfigured);
        // read executed flag
        boolean wasExecuted = settings.getBoolean(CFG_ISEXECUTED);
        if (m_model.isAutoExecutable()) {
            m_model.setExecuted(false);
        } else {
            m_model.setExecuted(wasExecuted);
        }

        // read model and load settings
        try {
            NodeSettingsRO modelSettings = settings.getNodeSettings(CFG_MODEL);
            if (m_model instanceof SpecialNodeModel) {
                ((SpecialNodeModel) m_model).loadSettingsFrom(nodeFile,
                        modelSettings, exec);
            } else {
                m_model.loadSettingsFrom(modelSettings);
            }            
        } catch (InvalidSettingsException ise) {
            m_logger.warn("Unable to load settings: " + ise.getMessage());
            m_status = new NodeStatus.Warning(
                    "Unable to load settings: " + ise.getMessage());
            notifyStateListeners(m_status);
        } catch (Exception e) {
            throw new InvalidSettingsException(e);
        } catch (Error e) {
            throw new InvalidSettingsException(e);
        }

        m_nodeDir = nodeFile.getParentFile();
        
        // if node was configured
        if (isConfigured()) {
            NodeSettingsRO spec = settings.getNodeSettings(CFG_SPEC_FILES);
            for (int i = 0; i < m_outDataPorts.length; i++) {
                String specName = spec.getString(CFG_OUTPUT_PREFIX + i);
                File targetFile = new File(m_nodeDir, specName);
                DataTableSpec outSpec = null;
                if (targetFile.exists()) {
                    NodeSettingsRO settingsSpec = NodeSettings.loadFromXML(
                        new BufferedInputStream(
                                new FileInputStream(targetFile)));
                    outSpec = DataTableSpec.load(settingsSpec);
                }
                m_outDataPorts[i].setDataTableSpec(outSpec);
            }
        }
        // load data if node was executed
        if (isExecuted()) {
            File internDir = new File(m_nodeDir, INTERN_FILE_DIR);
            try {
                m_model.loadInternals(internDir, exec);
                processModelWarnings();
            } catch (IOException ioe) {
                m_status = new NodeStatus.Error(
                    "Unable to load internals: " + ioe.getMessage());
                this.notifyStateListeners(m_status);
            }
            // load data
            if (getNrDataOutPorts() > 0) {
                String dataConfigFileName = settings.getString(CFG_DATA_FILE);
                File dataConfigFile = new File(m_nodeDir, dataConfigFileName);
                loadData(loadID, dataConfigFile, exec);
            }
            // load models
            NodeSettingsRO model = settings.getNodeSettings(CFG_MODEL_FILES);
            for (int i = 0; i < m_outModelPorts.length; i++) {
                String modelName = model.getString(CFG_OUTPUT_PREFIX + i);
                File targetFile = new File(m_nodeDir, modelName);
                BufferedInputStream in = 
                    new BufferedInputStream(new FileInputStream(targetFile));
                ModelContentRO pred = ModelContent.loadFromXML(in);
                m_outModelPorts[i].setModelContent(pred);
            }
            m_isCurrentlySaved = true;
        } else {
            if (wasExecuted && m_model.isAutoExecutable()) {
                execute(exec);
            }
        }
    }

    /**
     * Returns the name for this node.
     * 
     * @return The node's name.
     */
    public String getName() {
        return m_name;
    }
    
    /**
     * Returns the type for this node.
     * 
     * @return The node's type.
     */
    public NodeType getType() {
        return m_factory.getType();
    }

    /**
     * A detailed description of this node as html.
     * 
     * @return A html page containing the node's detailed description.
     * @see de.unikn.knime.core.node.NodeFactory#getNodeFullHTMLDescription
     */
    public String getFullHTMLDescription() {
        return m_factory.getNodeFullHTMLDescription();
    }

    /**
     * A short description of this node.
     * 
     * @return A single line containing a brief node description.
     * @see de.unikn.knime.core.node.NodeFactory#getNodeOneLineDescription
     */
    public String getOneLineDescription() {
        return m_factory.getNodeOneLineDescription();
    }

    /**
     * Sets a new name for this node.
     * 
     * @param nodeName The node's new name.
     * @throws NullPointerException If the name is <code>null</code>.
     */
    public void setName(final String nodeName) {
        m_name = nodeName.intern();
    }

    /**
     * @return The total number of input ports (data + model ports).
     */
    public int getNrInPorts() {
        return getNrDataInPorts() + getNrModelContentInPorts();
    }

    /**
     * @return The total number of output ports (data + model ports).
     */
    public int getNrOutPorts() {
        return getNrDataOutPorts() + getNrModelContentOutPorts();
    }

    /**
     * @return The number of input ports for data.
     */
    public int getNrDataInPorts() {
        return m_inDataPorts.length;
    }

    /**
     * @return The number of input ports for <code>ModelContent</code>.
     */
    public int getNrModelContentInPorts() {
        return m_inModelPorts.length;
    }

    /**
     * Returns the input port for the given ID.
     * 
     * @param inPortID The input port's ID.
     * @return Input port for the specified ID.
     * @throws IndexOutOfBoundsException If the ID is out of range.
     */
    public NodeInPort getInPort(final int inPortID) {
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
    public int getNrDataOutPorts() {
        return m_outDataPorts.length;
    }

    /**
     * @return The number of <code>ModelContent</code> output ports.
     */
    public int getNrModelContentOutPorts() {
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
     * Checks if the port with the specified ID is a ModelContent port.
     * 
     * @param id Port ID.
     * @return <code>true</code> if the port with the specified ID is a
     *         ModelContent port.
     */
    public boolean isModelContentInPort(final int id) {
        if ((getNrDataInPorts() <= id) && (id < getNrInPorts())) {
            return true;
        }
        return false;
    }

    /**
     * Checks if the port with the specified ID is a ModelContent port.
     * 
     * @param id Port ID.
     * @return <code>true</code> if the port with the specified ID is a
     *         ModelContent port.
     */
    public boolean isModelContentOutPort(final int id) {
        if ((getNrDataOutPorts() <= id) && (id < getNrOutPorts())) {
            return true;
        }
        return false;
    }

    /**
     * Returns the output port for the given <code>outPortID</code>. The
     * first ports are by definition for data the following ports
     * <code>ModelContent</code>.
     * 
     * @param outPortID The output port's ID.
     * @return Output port with the specified ID.
     * @throws IndexOutOfBoundsException If the index is out of range.
     */
    public NodeOutPort getOutPort(final int outPortID) {
        boundOutPort(outPortID);
        if (outPortID < m_outDataPorts.length) {
            return m_outDataPorts[outPortID];
        } else {
            return m_outModelPorts[outPortID - m_outDataPorts.length];
        }
    }

    /**
     * Delegation method to the inport.
     * {@link NodePort#getPortName()} method.
     * 
     * @param portID The port id of interest
     * @return The description to that port
     * @throws IndexOutOfBoundsException If argument is out of range.
     */
    public String getInportName(final int portID) {
        boundInPort(portID);
        if (isDataInPort(portID)) {
            return m_inDataPorts[portID].getPortName();
        } else {
            int newPortID = portID - getNrDataInPorts();
            return m_inModelPorts[newPortID].getPortName();
        }
    }

    /**
     * Delegation method to the outport.
     * {@link NodePort#getPortName()} method.
     * 
     * @param portID The port id of interest.
     * @return The description to that port.
     * @throws IndexOutOfBoundsException If argument is out of range.
     */
    public String getOutportName(final int portID) {
        boundOutPort(portID);
        if (isDataOutPort(portID)) {
            return m_outDataPorts[portID].getPortName();
        } else {
            int newPortID = portID - getNrDataOutPorts();
            return m_outModelPorts[newPortID].getPortName();
        }
    }

    /**
     * Delegate method to the model's <code>isAutoExecutable()</code> method.
     * 
     * @return If the the underlying node model should be immediately executed
     *         when possible.
     */
    public boolean isAutoExecutable() {
        return m_model.isAutoExecutable();
    }

    /**
     * Returns <code>true</code> if this node instance has been configured,
     * otherwise <code>false</code>. A node is configured if it has all its
     * settings required for execution.
     * 
     * @return <code>true</code> if node is configured otherwise
     *         <code>false</code>.
     * 
     * @see #execute()
     * @see #resetAndConfigure()
     */
    public boolean isConfigured() {
        return m_model.isConfigured();
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
            if (inPort.getBufferedDataTable() == null) {
                return false;
            }
        }
        for (ModelContentInPort inPort : m_inModelPorts) {
            if (!inPort.isConnected()) {
                return false;
            }
            if (inPort.getModelContent() == null) {
                return false;
            }
        }
        
        if (m_model instanceof MetaInputModel) {
            if (!((MetaInputModel) m_model).canBeExecuted()) {
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
        return m_model.isExecuted();
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
     * @see #execute(ExecutionContext)
     * @return <code>true</code> if execution was successful otherwise
     *         <code>false</code>.
     */
    public boolean execute() {
        ExecutionContext exe = new ExecutionContext(null, this);
        NodeProgressMonitorView progView = new NodeProgressMonitorView(null,
                exe.getProgressMonitor());
        progView.setVisible(true);
        boolean ret = execute(exe);
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
     * @see NodeModel#execute(BufferedDataTable[],ExecutionContext)
     */
    public boolean execute(final ExecutionContext exec) {
        // start message and keep start time
        final long time = System.currentTimeMillis();
        m_logger.info("Start execute");
        // reset the status object
        m_status = null;
        // notify state listeners
        notifyStateListeners(new NodeStatus.StartExecute());
        //
        // CHECK PRECONDITIONS
        //
        // if this node has already been executed
        if (isExecuted()) {
            m_logger.assertLog(false, "Is executed");
            notifyStateListeners(new NodeStatus.EndExecute());
            return true;
        }
        // if NOT fully connected to predecessors
        if (!isFullyConnected()) {
            m_logger.assertLog(false, "Is not fully connected");
            notifyStateListeners(new NodeStatus.EndExecute());
            return false;
        }
        // if not configured
        if (!isConfigured()) {
            m_logger.assertLog(false, "Is not correctly configured");
            notifyStateListeners(new NodeStatus.EndExecute());
            return false;
        }

        // 
        // EXECUTE the underlying node's model
        //
        // retrieve all input tables
        BufferedDataTable[] inData = new BufferedDataTable[getNrDataInPorts()];
        for (int i = 0; i < inData.length; i++) {
            DataInPort inPort = m_inDataPorts[i];
            inData[i] = inPort.getBufferedDataTable();
            if (inData[i] == null) {
                m_logger.assertLog(false,
                        "Couldn't get data from predecessor (Port No." + i
                                + "). Is it not executed?!?");
                m_logger.error("failed execute");
                m_status = new NodeStatus.Error(
                        "Couldn't get data from predecessor (Port No." + i 
                        + "). Is it not executed?!?");
                notifyStateListeners(m_status);
                notifyStateListeners(
                        new NodeStatus.EndExecute());
                return false;
            }
        }
        BufferedDataTable[] newOutData; // the new DTs from the model
        ModelContent[] predParams; // the new output models
        try {
            // INVOKE MODEL'S EXECUTE
            newOutData = m_model.executeModel(inData, exec);
            processModelWarnings();
        } catch (CanceledExecutionException cee) {
            // execution was canceled
            m_logger.info("execute canceled");
            this.resetAndConfigure();
            m_status = new NodeStatus.Warning("Execution canceled!");
            return false;
        } catch (AssertionError ae) {
            m_logger.assertLog(false, ae.getMessage(), ae);
            this.resetAndConfigure();
            m_status = new NodeStatus.Error(
                    "Execute failed: " + ae.getMessage());
            this.notifyStateListeners(m_status);
            return false;
        } catch (Error e) {
            // some other error - should never happen!
            m_logger.fatal("Fatal error", e);
            this.resetAndConfigure();
            m_status = new NodeStatus.Error(
                    "Execute failed: " + e.getMessage());
            this.notifyStateListeners(m_status);
            return false;
        } catch (Exception e) {
            // execution failed
            m_logger.error("Execute failed", e);
            this.resetAndConfigure();
            m_status = new NodeStatus.Error(
                    "Execute failed: " + e.getMessage());
            this.notifyStateListeners(m_status);
            return false;
        }

        // check created predictor models (if any)
        predParams = new ModelContent[getNrModelContentOutPorts()];
        for (int p = 0; p < predParams.length; p++) {
            // create ModelContent to write into
            predParams[p] = new ModelContent("predictor");
            try {
                m_model.saveModelContent(p, predParams[p]);
            } catch (Exception e) {
                m_logger.error("Predictor model couldn't be saved at port #"
                        + p, e);
                m_status = new NodeStatus.Error("ModelContent couldn't " 
                        + "be saved at port #" + p + e.getMessage());
                notifyStateListeners(m_status);
                notifyStateListeners(
                        new NodeStatus.EndExecute());
                return false;
            }
        }
        // spread the newly available DataTable, Specs, and prediction models
        // to the successors
        for (int p = 0; p < getNrDataOutPorts(); p++) {
            DataTableSpec portSpec = m_outDataPorts[p].getDataTableSpec();
            if (portSpec != null) {
                DataTableSpec newSpec = newOutData[p].getDataTableSpec();
                if (!portSpec.equals(newSpec)) {
                    m_logger.coding("DataSpec generated by configure does not"
                            + " match spec after execution.");
                }
            }
            BufferedDataTable t = newOutData[p];
            t.setOwnerRecursively(this);
            m_outDataPorts[p].setDataTable(t);
        }
        for (int p = 0; p < getNrModelContentOutPorts(); p++) {
            m_outModelPorts[p].setModelContent(predParams[p]);
        }
        m_logger.info("End execute (" + (System.currentTimeMillis() - time)
                / 100 / 10.0 + " sec)");
        m_isCurrentlySaved = false;
        notifyStateListeners(new NodeStatus.EndExecute());
        return true;
    } // executeNode(ExecutionMonitor)

    /**
     * Checks the warnings in the model and notifies registered listeners.
     * 
     */
    private void processModelWarnings() {

        // get the warning message if available and create a status
        // also notify all listeners
        String warningMessage = m_model.getWarningMessage();
        if (warningMessage != null) {

            m_logger.warn("Model warning message: " + warningMessage);
            m_status = new NodeStatus.Warning("Warning: " + warningMessage);

            this.notifyStateListeners(m_status);
            // reset the warning message
            m_model.setWarningMessage(null);
        }
    }

    /**
     * Resets this node. The method will first reset all connected successors
     * (recursively) and then call the model's <code>#resetModel()</code> in
     * order to reset the underlying model. The <code>#isExecuted()</code>
     * method will return <code>false</code> after this call.
     * 
     * @see #reset()
     * @see #configure()
     */
    public void resetAndConfigure() {
        // reset
        try {
            reset();
        } catch (Exception e) {
            m_logger.error("Reset failed", e);
            m_status = new NodeStatus.Warning(
                    "Reset failed: " + e.getMessage());
        } catch (Error e) {
            m_logger.fatal("Reset failed", e);
            m_status = new NodeStatus.Warning(
                    "Reset failed: " + e.getMessage());
        } finally {
            notifyStateListeners(new NodeStatus.Reset("Not configured."));
            notifyStateListeners(m_status);
        }
        // configure
        try {
            configure();
        } catch (Exception e) {
            m_logger.error("Configure failed", e);
            m_status = new NodeStatus.Warning(
                    "Configure failed: " + e.getMessage());
        } catch (Error e) {
            m_logger.fatal("Configure failed", e);
            m_status = new NodeStatus.Warning(
                    "Configure failed: " + e.getMessage());
        } finally {
            notifyStateListeners(new NodeStatus.Configured("Configured."));
            notifyStateListeners(m_status);
        }
    }

    /**
     * Resets this node with out re-configuring it. All connected nodes will be
     * reset, as well as the <code>DataTable</code>s and
     * <code>PredictParams</code> at the outports.
     */
    private void reset() {
        m_logger.info("reset");
        // if reset had no exception, reset node status
        m_status = null;
        // after reset, the node is not saved anymore
        m_isCurrentlySaved = false;
        // reset the model
        m_model.resetModel();

        // blow away our data tables in the port
        for (int p = 0; p < getNrDataOutPorts(); p++) {
            // m_outDataPorts[p].setDataTableSpec(null);
            m_outDataPorts[p].setDataTable(null);
        }

        // blow away our pred models in the out ports
        for (int p = 0; p < getNrModelContentOutPorts(); p++) {
            try {
                m_outModelPorts[p].setModelContent(null);
            } catch (NullPointerException npe) {
                m_logger.coding("loadModelContent() does not check for null"
                        + " argument.");
                m_status = new NodeStatus.Warning("Node does not check for "
                        + "null argument in loadPreddictorParams().");
            }
        }

        // load ModelContent from all available InPorts again
        for (int p = 0; p < getNrModelContentInPorts(); p++) {
            try {
                m_model.loadModelContent(p, m_inModelPorts[p]
                        .getModelContent());
            } catch (NullPointerException npe) {
                m_logger.coding("loadModelContent() does not check for null"
                        + " argument.");
                m_status = new NodeStatus.Warning("Node does not check for "
                        + "null argument in loadModelContent().");
            } catch (InvalidSettingsException ise) {
                m_status = new NodeStatus.Warning("Could not load model"
                        + " settings into the node.");
            }
        }
        
        // notify about the new status or send warning if something failed 
        notifyStateListeners(new NodeStatus.Reset());
        if (m_status != null) {
            notifyStateListeners(m_status);
        }
    }

    /**
     * Prepares the deletion of this node by calling <code>#resetNode()</code>,
     * closing all views, disconnect all input and output ports, and all state
     * listeners are removed.
     */
    public void detach() {
        // reset this node first
        reset();
        // close and unregister all views
        NodeView[] views = m_model.getViews().toArray(new NodeView[0]);
        for (NodeView view : views) {
            // unregisters and closes the view
            view.closeView();
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
        // delete all node file with the node's directory
        if (m_nodeDir != null) {
            boolean b = FileUtil.deleteRecursively(m_nodeDir);
            if (!b) {
                m_logger.warn("Unable to delete dir: \"" + m_nodeDir + "\"");
            }
        }
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
     * @see Node#inportHasNewModelContent
     */
    void inportHasNewConnection(final int inPortID) {
        boundInPort(inPortID);
        if (m_model instanceof SpecialNodeModel) {
            ((SpecialNodeModel) m_model).inportHasNewConnection(inPortID);
        }
        // resetNodeWithoutConfigure();
        if (inPortID < getNrDataInPorts()) {
            // set hilitehandler in this node using the one from the predecessor
            inportHasNewHiLiteHandler(inPortID);
            // also get a new DataTableSpec, if available
            inportHasNewTableSpec(inPortID);
        } else {
            inportHasNewModelContent(inPortID);
        }
    }

    /**
     * Notification method, called by an input port to tell the node about a
     * disconnected outport from a predecessor. The notification is done, as the
     * node itself should be responsible to cause the required actions. The node
     * is reset and newly configured.
     * 
     * @param inPortID The port id that just got disconnected.
     */
    void inportWasDisconnected(final int inPortID) {
        boundInPort(inPortID);
        // call reset
        reset();

        if (isDataInPort(inPortID)) {
            // reset hilite handler in this node.
            // This triggers hilite handler propagation through the output ports
            setHiLiteHandler(inPortID, null);
        } else { // then this is a ModelContent port
            /*
             * reset the ModelContent of this inport, previously pushed in
             * and stored in this node.
             */
            int realId = inPortID - getNrDataInPorts();
            try {
                m_model.loadModelContent(realId, null);
            } catch (NullPointerException e) {
                /*
                 * if the nodemodel implementation of the loadPredictorParams is
                 * correct we will not end up here.
                 */
                m_logger.coding("Incorrect implementation of "
                        + "method NodeModel.loadModelConten(): "
                        + "It must handle null parameters");
            } catch (InvalidSettingsException ise) {
                // ignore, since we tried to load null settings.
            }
        }
        // re-create out table specs, as incoming table specs/models are gone.
        configure();
        
        if (m_model instanceof SpecialNodeModel) {
            ((SpecialNodeModel) m_model).inportWasDisconnected(inPortID);
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
        NodeInPort port = getInPort(inPortID);
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
        if (m_model instanceof SpecialNodeModel) {
            ((SpecialNodeModel) m_model).inportHasNewDataTable(
                    m_inDataPorts[inPortID].getBufferedDataTable(), inPortID);
        }
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
        if (isExecuted()) {
            reset();
        }

        if (m_model instanceof SpecialNodeModel) {
            ((SpecialNodeModel) m_model).inportHasNewTableSpec(
                    m_inDataPorts[inPortID].getDataTableSpec(), inPortID);
        }
        configure();
        
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
    void inportHasNewModelContent(final int inPortID) {
        // Predictor params are propagated through model ports only
        boundModelContentInPort(inPortID);
        if (isExecuted()) {
            reset();
        }
        try {
            int realId = inPortID - getNrDataInPorts();
            ModelContentRO params = m_inModelPorts[realId]
                    .getModelContent();
            m_model.loadModelContent(realId, params);
        } catch (InvalidSettingsException ise) {
            m_logger.warn("Unable to load ModelContent: "
                    + ise.getMessage());
            m_status = new NodeStatus.Error(
                    "Could not load ModelContent: " + ise.getMessage());
            this.notifyStateListeners(m_status);
        } catch (NullPointerException npe) {
            m_logger.coding("Model need to check for null argument.");
            m_status = new NodeStatus.Error(
                    "Could not load ModelContent due to null argument.");
            this.notifyStateListeners(m_status);
        }
        configure();

    }

    /**
     * Returns the <code>HiLiteHandler</code> for the given output port by
     * retrieving it from the node's <code>NodeModel</code>.
     * 
     * @param outPortID Output port's ID.
     * @return The hilite handler for the given output port.
     */
    HiLiteHandler getOutHiLiteHandler(final int outPortID) {
        // hilite handler propagate through data ports only.
        boundDataOutPort(outPortID);
        return m_model.getOutHiLiteHandler(outPortID);
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
        // hilite handler are propagated through data ports only.
        boundDataInPort(inPortID);
        m_model.setInHiLiteHandler(hiLiteHdl, inPortID);
        for (int i = 0; i < m_outDataPorts.length; i++) {
            m_outDataPorts[i].setHiLiteHandler(m_model
                    .getOutHiLiteHandler(i));
        }
    }

    /**
     * Sets all (new) incoming <code>DataTableSpec</code> elements in the
     * model, calls the model to create output table specs and propagates these
     * new specs to the connected successors.
     */
    public void configure() {
        if (isExecuted()) {
            throw new IllegalStateException("Must not call configure when"
                    + " executed.");
        }

        // reset status object to clean previous status messages.
        m_status = null;

        // configure
        try {
            String errorMsg = "";
            // get inspecs and check them against null
            DataTableSpec[] inSpecs = new DataTableSpec[getNrDataInPorts()];
            for (int i = 0; i < inSpecs.length; i++) {
                inSpecs[i] = m_inDataPorts[i].getDataTableSpec();
                if (inSpecs[i] == null) {
                    if (errorMsg.length() > 0) {
                        errorMsg += ",";
                    }
                    errorMsg += i;
                }
            }
            // if an in spec is null
            if (errorMsg.length() > 0) {
                throw new InvalidSettingsException(
                    "Node can't be configured due to missing input spec(s): " 
                        + errorMsg);
            }

            // call configure model to create output table specs
            DataTableSpec[] newSpecs = m_model.configureModel(inSpecs);
            // notify state listeners before the new specs are propagated
            this.notifyStateListeners(new NodeStatus.Configured("Configured"));
            /*
             * set the new specs in the output ports, which will propagate them
             * to connected successor nodes
             */
            for (int p = 0; p < newSpecs.length; p++) {
                // update data table spec
                m_outDataPorts[p].setDataTableSpec(newSpecs[p]);
            }
        } catch (InvalidSettingsException ise) {
            m_logger.warn("Configure failed: " + ise.getMessage());
            reset();
        } catch (Exception e) {
            m_logger.error("Configure failed", e);
            reset();
        } catch (Error e) {
            m_logger.fatal("Configure failed", e);
            reset();
        } finally {
            processModelWarnings();
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
        return m_factory.getNrNodeViews();
    }

    /**
     * Returns the name for this node's view at the given index.
     * 
     * @param viewIndex The view index.
     * @return The view's name.
     * @throws ArrayIndexOutOfBoundsException If the view index is out of range.
     */
    public String getViewName(final int viewIndex) {
        return m_factory.getNodeViewName(viewIndex);
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
            m_status = new NodeStatus.Error(
                    "View could not be opened, reason: " + e.getMessage());
            this.notifyStateListeners(m_status);
        } catch (Error e) {
            m_logger.fatal("Show view failed", e);
            m_status = new NodeStatus.Error(
                    "View could not be opened, reason: " + e.getMessage());
            this.notifyStateListeners(m_status);
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
        NodeView view = m_factory.createNodeView(viewIndex, m_model);
        return view;
    }
    
    /**
     * Closes all views.
     */
    public void closeAllViews() {
        for (NodeView view : m_model.getViews()) {
            view.closeView();
        }
    }
    
    /**
     * Closes all output port views (data and model port views).
     */
    public void closeAllPortViews() {
        for (DataOutPort port : m_outDataPorts) {
            port.disposePortView();
        }
        for (ModelContentOutPort port : m_outModelPorts) {
            port.disposePortView();
        }
    }
    
    /**
     * @return <code>true</code> if a dialog is available.
     */
    public boolean hasDialog() {
        return m_factory.hasDialog();
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
            m_status = new NodeStatus.Error(
                    "Dialog could not be opend properly: " + e.getMessage());
            this.notifyStateListeners(m_status);
        } catch (Error e) {
            m_logger.fatal("show dialog failed", e);
            m_status = new NodeStatus.Error(
                    "Dialog could not be opend properly: " + e.getMessage());
            this.notifyStateListeners(m_status);
        }
    }

    /**
     * @return The dialog pane which holds all the settings' components. In
     *         addition this method loads the settings from the model into the
     *         dialog pane. The pane might be <code>null</code> if no dialog
     *         is available.
     * @throws NotConfigurableException if the dialog cannot be opened because
     * of real invalid settings or if any predconditions are not fulfilled, e.g.
     * no predecessor node, no nominal column in input table, etc.
     * @see #hasDialog()
     */
    public NodeDialogPane getDialogPane() throws NotConfigurableException {
        if (hasDialog()) {
            // init dialog pane, if not done yet.
            if (m_dialogPane == null) {
                m_dialogPane = m_factory.createNodeDialogPane();
                m_dialogPane.setNode(this);
            }
            // load settings into the dialog
            loadDialogSettingsFromModel();
            // return the dialog pane
            return m_dialogPane;
        }
        assert false : "Can't return dialog pane, node has no dialog!";
        return null;
    }
    
    private static final String CFG_NAME = "name";
    private static final String CFG_ISCONFIGURED = "isConfigured";
    private static final String CFG_ISEXECUTED = "isExecuted";
    private static final String CFG_MODEL = "model";
    private static final String CFG_SPEC_FILES = "spec_files";
    private static final String SPEC_FILE_PREFIX = "spec_";
    private static final String CFG_DATA_FILE = "data_meta_file";
    private static final String CFG_DATA_FILE_DIR = "data_files_directory";
    
    private static final String DATA_FILE_PREFIX = "data_";
    private static final String CFG_MODEL_FILES = "model_files";
    private static final String MODEL_FILE_PREFIX = "model_";
    private static final String CFG_OUTPUT_PREFIX = "output_";
    
    private static String createSpecFileName(final int index) {
        return SPEC_FILE_PREFIX + index + ".xml";
    }
    
    private static String createDataFileDirName(final int index) {
        return DATA_FILE_PREFIX + index;
    }
    
    private static String createModelFileName(final int index) {
        return MODEL_FILE_PREFIX + index + ".pmml.gz";
    }
    
    
    /**
     * Saves the settings (but not the data).
     * 
     * @param settings a settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        // write node name
        settings.addString(CFG_NAME, m_name);
        // write configured flag
        settings.addBoolean(CFG_ISCONFIGURED, isConfigured());
        // write executed flag
        settings.addBoolean(CFG_ISEXECUTED, isExecuted());
     
        // write model
        final NodeSettingsWO model = settings.addNodeSettings(CFG_MODEL);
        try {
            m_model.saveSettingsTo(model);
        } catch (Exception e) {
            m_logger.error("Could not save model", e);
        } catch (Error e) {
            m_logger.fatal("Could not save model", e);
        }        
    }
    
    /**
     * Saves the node, node settings, and all internal structures, spec, data,
     * and models, to the given node directory (located at the node file).
     * @param nodeFile To write node settings to.
     * @param exec Used to report progress during saving.
     * @throws IOException If the node file can't be found or read.
     * @throws CanceledExecutionException If the saving has been canceled.
     */
    public void save(final File nodeFile, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        NodeSettings settings = new NodeSettings(SETTINGS_FILE_NAME);
        saveSettings(settings);
    
        m_nodeDir = nodeFile.getParentFile();
        
        if (m_model instanceof SpecialNodeModel) {
            try {
                NodeSettings model = settings.getNodeSettings(CFG_MODEL);
                ((SpecialNodeModel) m_model).saveSettingsTo(m_nodeDir, model,
                        exec);
            } catch (InvalidSettingsException ex) {
                // this should never happen as we have added the model
                // in saveSettings(...) above
                m_logger.error("Internal error while saving a meta node", ex);
            }            
        }

        
        
        if (isConfigured()) {
            NodeSettingsWO specs = settings.addNodeSettings(CFG_SPEC_FILES);
            for (int i = 0; i < m_outDataPorts.length; i++) {
                String specName = createSpecFileName(i);
                specs.addString(CFG_OUTPUT_PREFIX + i, specName);
                if (!(m_isCurrentlySaved && isExecuted())) {
                    DataTableSpec outSpec = 
                        m_outDataPorts[i].getDataTableSpec();
                    if (outSpec != null) {
                        NodeSettings specSettings = new NodeSettings(specName);
                        outSpec.save(specSettings);
                        File targetFile = new File(m_nodeDir, specName);
                        specSettings.saveToXML(new BufferedOutputStream(
                                new FileOutputStream(targetFile)));
                    }
                }
            }
        } else {
            for (int i = 0; i < m_outDataPorts.length; i++) {
                File specFile = new File(m_nodeDir, createSpecFileName(i));
                specFile.delete();
            }
        }
        if (!m_isCurrentlySaved) {
            if (isExecuted()) {
                if (!isAutoExecutable()) {
                    File internDir = new File(m_nodeDir, INTERN_FILE_DIR);
                    // may exist from previous savings, clean up
                    if (internDir.exists()) {
                        FileUtil.deleteRecursively(internDir);
                    }
                    internDir.mkdir();
                    if (internDir.canWrite()) {
                        try {
                            m_model.saveInternals(internDir, exec);
                            processModelWarnings();
                        } catch (IOException ioe) {
                            m_status = new NodeStatus.Error("Unable to save " 
                                    + "internals: " + ioe.getMessage());
                            this.notifyStateListeners(m_status);
                        }
                    }
                }
                if (getNrDataOutPorts() > 0) {
                    settings.addString(CFG_DATA_FILE, DATA_FILE_NAME);
                    File dataSettingsFile = new File(m_nodeDir, DATA_FILE_NAME);
                    saveData(dataSettingsFile, exec);
                }
                NodeSettingsWO models = 
                    settings.addNodeSettings(CFG_MODEL_FILES);
                for (int i = 0; i < m_outModelPorts.length; i++) {
                    String specName = createModelFileName(i);
                    models.addString(CFG_OUTPUT_PREFIX + i, specName);
                    ModelContentRO pred = 
                        m_outModelPorts[i].getModelContent();
                    File targetFile = new File(m_nodeDir, specName);
                    BufferedOutputStream out = new BufferedOutputStream(
                            new FileOutputStream(targetFile));
                    pred.saveToXML(out);
                }
                m_isCurrentlySaved = true;
            } else {
                File internDir = new File(m_nodeDir, INTERN_FILE_DIR);
                FileUtil.deleteRecursively(internDir);
                File dataDir = new File(m_nodeDir, DATA_FILE_DIR);
                FileUtil.deleteRecursively(dataDir);
                new File(m_nodeDir, DATA_FILE_NAME).delete();
                for (int i = 0; i < m_outModelPorts.length; i++) {
                    String modelFile = createModelFileName(i);
                    File targetFile = new File(m_nodeDir, modelFile);
                    targetFile.delete();            
                }
            }
        } else {
             if (isExecuted()) {
                 if (getNrDataOutPorts() > 0) {
                     settings.addString(CFG_DATA_FILE, DATA_FILE_NAME);
                 }
                 NodeSettingsWO models = 
                     settings.addNodeSettings(CFG_MODEL_FILES);
                 for (int i = 0; i < m_outModelPorts.length; i++) {
                     String specName = createModelFileName(i);
                     models.addString(CFG_OUTPUT_PREFIX + i, specName);
                 }
             } else {
                 m_logger.assertLog(
                         false, "Saved flag is set but node is not executed.");
             }
        }
        settings.saveToXML(new BufferedOutputStream(
                new FileOutputStream(nodeFile)));
    } // save(File, ExecutionMonitor)

    private void saveData(final File configFile, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        NodeSettings settings = new NodeSettings("data_files_information");
        File dataDir = new File(configFile.getParentFile(), DATA_FILE_DIR);
        if (dataDir.exists()) {
            FileUtil.deleteRecursively(dataDir);
        }
        dataDir.mkdir();
        if (!dataDir.isDirectory() || !dataDir.canWrite()) {
            throw new IOException("Can not write directory " 
                    + dataDir.getAbsolutePath());
        }
        settings.addString(CFG_DATA_FILE_DIR, DATA_FILE_DIR);
        for (int i = 0; i < m_outDataPorts.length; i++) {
            NodeSettingsWO portSettings = 
                settings.addNodeSettings(CFG_OUTPUT_PREFIX + i);
            String dataName = createDataFileDirName(i);
            File dir = new File(dataDir, dataName);
            dir.mkdir();
            if (!(dir.isDirectory() && dir.canWrite())) {
                throw new IOException("Can not write directory " 
                        + dir.getAbsolutePath());
            }
            portSettings.addString(CFG_DATA_FILE_DIR, dataName);
            BufferedDataTable outTable = 
                m_outDataPorts[i].getBufferedDataTable();
            outTable.save(dir, portSettings, exec);
        }
        settings.saveToXML(new BufferedOutputStream(
                new FileOutputStream(configFile)));
    }
    
    private void loadData(final int loadID, final File configfile, 
            final ExecutionMonitor exec) throws IOException, 
            CanceledExecutionException, InvalidSettingsException {
        NodeSettingsRO settings = NodeSettings.loadFromXML(
                new BufferedInputStream(new FileInputStream(configfile)));
        String dataPath = settings.getString(CFG_DATA_FILE_DIR);
        File dataDir = new File(m_nodeDir, dataPath);
        if (!dataDir.exists() || !dataDir.canRead()) {
            throw new IOException("Can not read directory " 
                    + dataDir.getAbsolutePath());
        }
        for (int i = 0; i < m_outDataPorts.length; i++) {
            NodeSettingsRO portSettings = 
                settings.getNodeSettings(CFG_OUTPUT_PREFIX + i);
            String dataName = portSettings.getString(CFG_DATA_FILE_DIR);
            File dir = new File(dataDir, dataName);
            if (!dir.isDirectory() || !dir.canRead()) {
                throw new IOException("Can not read directory " 
                        + dir.getAbsolutePath());
            }
            BufferedDataTable t = 
                BufferedDataTable.loadFromFile(dir, portSettings, exec, loadID);
            // take ownership for any newly created files (successor nodes
            // don't store this table, they just reference on us.)
            t.setOwnerRecursively(this);
            assert (t != null);
            m_outDataPorts[i].setDataTable(t);
        }
    }
    
    /**
     * Validates the settings inside the model.
     * 
     * @throws InvalidSettingsException If not valid.
     */
    void validateModelSettingsFromDialog() throws InvalidSettingsException {
        // save new dialog's config into new object
        NodeSettings newSettings = new NodeSettings(this.getName());
        m_dialogPane.finishEditingAndSaveSettingsTo(newSettings);
        // validate settings
        m_model.validateSettings(newSettings);
    }

    /**
     * Reads the current settings from the dialog and writes them into the
     * model.
     * @throws InvalidSettingsException If the settings are not valid for the
     *             underlying model.
     */
    public void loadModelSettingsFromDialog() throws InvalidSettingsException {
        // save new dialog's config into new object
        NodeSettings newSettings = new NodeSettings(this.getName());
        m_dialogPane.finishEditingAndSaveSettingsTo(newSettings);
        // and apply it to the model
        m_model.loadSettingsFrom(newSettings);
    }

    /**
     * Compares the current settings from the dialog with the settings from the
     * model.
     * 
     * @return true if the settings are equal
     */
    boolean isModelAndDialogSettingsEqual() {
        try {
            // save new dialog's config into new object
            NodeSettings dialogSettings = new NodeSettings("Compare");
            NodeSettings modelSettings = new NodeSettings("Compare");
            m_dialogPane.finishEditingAndSaveSettingsTo(dialogSettings);
            m_model.saveSettingsTo(modelSettings);
            // check for equality
            return dialogSettings.isIdentical(modelSettings);
        } catch (InvalidSettingsException ise) {
            // if there are invalid settings it is assumed that the settings
            // are not equal
            return false;
        }
    }

    /**
     * Reads the current settings from the model and load them into the dialog
     * pane.
     * @throws NotConfigurableException if the dialog cannot be opened because
     * of real invalid settings or if any predconditions are not fulfilled, e.g.
     * no predecessor node, no nominal column in input table, etc.
     */
    private void loadDialogSettingsFromModel() throws NotConfigurableException {
        // get the model's current settings ...
        NodeSettings currSettings = new NodeSettings(this.getName());
        m_model.saveSettingsTo(currSettings);
        // ... to init the dialog
        m_dialogPane.loadSettingsFrom(currSettings, getInDataTableSpecs());
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

    /**
     * Checks range for given input port ID.
     * 
     * @param inPortID The input port ID to check.
     * @return Given input port ID if in range.
     * @throws IndexOutOfBoundsException if the specified ID is not a pred
     *             params input port ID.
     */
    private int boundModelContentInPort(final int inPortID) {
        // predictor params port ids are the indecies above the data port ids
        if ((inPortID < getNrDataInPorts())
                || (inPortID >= getNrModelContentInPorts() 
                        + getNrDataInPorts())) {
            throw new IndexOutOfBoundsException(
                    "Invalid ModelContent input-port number: "
                            + inPortID
                            + " (valid range: ["
                            + getNrDataInPorts()
                            + "..."
                            + (getNrModelContentInPorts() 
                                    + getNrDataInPorts() - 1)
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
    synchronized void notifyStateListeners(final NodeStatus state) {
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
    @Override
    public String toString() {
        return "Node @" + hashCode() + " [" + m_name + ";in="
                + m_inDataPorts.length + ";out=" + m_outDataPorts.length
                + ";fullyConnected=" + isFullyConnected() + ";configured="
                + isConfigured() + ";executable=" + isExecutable()
                + ";executed=" + isExecuted() + "]";
    }

    /**
     * @return The status object of this <code>Node</code>.
     */
    public NodeStatus getStatus() {
        return m_status;
    }

    /**
     * @return the <code>NodeFactory</code> that constructed this node.
     */
    public NodeFactory getFactory() {
        return m_factory;
    }

    /**
     * @return true if this node's model is a interruptible model
     */
    public boolean isInterruptible() {
        if (m_model instanceof InterruptibleNodeModel) {
            return true;
        }

        return false;
    }

    /**
     * Returns the concrete class of this node's model.
     * 
     * @return the model's class
     */
    public Class<? extends NodeModel> getModelClass() {
        return m_model.getClass();
    }

    /**
     * Returns the embedded workflow manager if this node is a meta node which
     * is just a container for a subworkflow.
     * 
     * @return the embedded workflowmanager or <code>null</code> if this node
     * does not represent a meta node
     */
    public WorkflowManager getEmbeddedWorkflowManager() {
        if (m_model instanceof SpecialNodeModel) {
            return ((SpecialNodeModel) m_model).internalWFM();
        } else {
            return null;
        }
    }
    
    
    /**
     * Transfers the model of this node into the meta node model by calling
     * {@link MetaNodeModel#receiveModel(NodeModel)} with the model as argument.
     * 
     * @param metaModel a meta node model
     */
    public void retrieveModel(final MetaNodeModel metaModel) {
        metaModel.receiveModel(m_model);        
    }
}
