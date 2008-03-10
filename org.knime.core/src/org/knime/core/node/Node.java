/*
 * ------------------------------------------------------------------
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   17.01.2006(sieb, ohl): reviewed 
 */
package org.knime.core.node;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.UIManager;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.node.GenericNodeDialogPane.MiscNodeDialogPane;
import org.knime.core.node.GenericNodeFactory.NodeType;
import org.knime.core.node.interrupt.InterruptibleNodeModel;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.NodeMessageEvent;
import org.knime.core.node.workflow.NodeMessageListener;
import org.knime.core.node.workflow.ScopeContext;
import org.knime.core.node.workflow.ScopeObjectStack;
import org.knime.core.util.FileUtil;
import org.w3c.dom.Element;

/**
 * Implementation of a node as basic processing unit within the workflow. A Node
 * object is the place where the data flow starts, ends, or intersects. Thus a
 * Node can be connected with predecessors and successors through its input and
 * output ports, {@link org.knime.core.node.NodeInPort} and
 * {@link org.knime.core.node.NodeOutPort}, respectively. There are data ports
 * for exchanging data tables, and prediction model ports for transferring
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

    /** The sub settings entry where the model can save its setup. */
    public static final String CFG_MODEL = "model";

    /** The node's name. */
    private String m_name;

    /** The factory used to create model, dialog, and views. */
    private final GenericNodeFactory<GenericNodeModel> m_factory;

    /** The node's assigned node model. */
    private final GenericNodeModel m_model;

    /** The node's dialog or <code>null</code> if not available. */
    private GenericNodeDialogPane m_dialogPane;

    /** Holds the current node message. */
    private NodeMessage m_message;

    /** Keeps fixed array of output ports. */
    private final NodeOutPort[] m_outPorts;

    /** Keeps fixed array of output ports. */
    private final NodeInPort[] m_inPorts;

    /**
     * The memory policy for the data outports, i.e. keep in memory or hold on
     * disc.
     */
    private MemoryPolicy m_outDataPortsMemoryPolicy;

    /** The listeners that are interested in node state changes. */
    private final CopyOnWriteArraySet<NodeMessageListener> m_messageListeners;

    /** Config key: What memory policy to use for a node outport. */
    static final String CFG_MEMORY_POLICY = "memory_policy";

    /**
     * Available policy how to handle output data. It might be held in memory or
     * completely on disc. We use an enum here as a boolean may not be
     * sufficient in the future (possibly adding a third option "try to keep in
     * memory").
     */
    static enum MemoryPolicy {
        /** Hold output in memory. */
        CacheInMemory,
        /**
         * Cache only small tables in memory, i.e. with cell count <=
         * DataContainer.MAX_CELLS_IN_MEMORY.
         */
        CacheSmallInMemory,
        /** Buffer on disc. */
        CacheOnDisc
    }

    /**
     * Contains the set of tables that have been created during the execute
     * using the ExecutionContext. This set does not contain the outport tables.
     * For a threaded node (chunk-wise processing), this set will contain the
     * temporary chunk containers. Ideally, we will clear theses tables when the
     * execution finishes but some implementations (for instance a scatterplot,
     * scorer) do create tables that they keep a reference on (for displaying in
     * the view).
     */
    private final Set<ContainerTable> m_localTempTables;

    // lock that prevents a possible deadlock if a node is currently configuring
    // (e.g. because inportHasNodeModelContent has been called)
    // and the WFM is asking if the node isExecutable(), which it is in most
    // cases then
    private final Object m_configureLock = new Object();

    static {
        try {
            String sysLaF = UIManager.getSystemLookAndFeelClassName();
            // The GTK L&F has apparently some serious problems. Weka dialogs
            // cannot be opened (NPE) and in 1.6.0 there were problems with
            // "Xlib: sequence lost" ... resulting in KNIME going down.
            if (sysLaF.equals("com.sun.java.swing.plaf.gtk.GTKLookAndFeel")) {
                sysLaF = UIManager.getCrossPlatformLookAndFeelClassName();
            }
            UIManager.setLookAndFeel(sysLaF);
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
     * @param wfm the workflow manager that is responsible for this node; maybe
     *            <code>null</code>
     * @throws IllegalArgumentException If the <i>nodeFactory</i> is
     *             <code>null</code>.
     */
    public Node(final GenericNodeFactory<GenericNodeModel> nodeFactory) {
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

        // init state listener array
        m_messageListeners = new CopyOnWriteArraySet<NodeMessageListener>();

        // init input ports
        m_inPorts = new NodeInPort[m_model.getNrInPorts()];
        for (int i = 0; i < m_inPorts.length; i++) {
            m_inPorts[i] = new NodeInPort(i, m_model.getInPortType(i));
            m_inPorts[i].setPortName(m_factory.getInportDataName(i));
        }

        // init output ports
        m_outPorts = new NodeOutPort[m_model.getNrOutPorts()];
        // default option: keep small tables in mem (can be changed in dialog)
        m_outDataPortsMemoryPolicy = MemoryPolicy.CacheSmallInMemory;
        for (int i = 0; i < m_outPorts.length; i++) {
            m_outPorts[i] = new NodeOutPort(i, m_model.getOutPortType(i));
            m_outPorts[i].setPortName(m_factory.getOutportDataName(i));
            m_outPorts[i].setPortObjectSpec(null);
            m_outPorts[i].setPortObject(null, this);
            m_outPorts[i].setHiLiteHandler(m_model.getOutHiLiteHandler(i));
        }

        m_localTempTables = new HashSet<ContainerTable>();
        // TODO (tg,po)
        // let the model create its 'default' table specs
        // configure(null);
    }

    /**
     * Creates a copy of the passed node.
     * 
     * @param node the node that should be copied
     */
    public Node(final Node node) {
        this(node.getFactory());
        NodeSettings settings = new NodeSettings("settings");
        node.saveSettingsTo(settings);
        try {
            loadSettingsFrom(settings);
        } catch (InvalidSettingsException ise) {
            m_logger.error("Could not copy node, reason: " + ise.getMessage());
        } finally {
            reset();
        }
    }

    void load(final NodePersistor loader, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        if (m_model.isAutoExecutable()) {
            // will be executed by workflow manager
            m_model.setHasContent(false);
        } else {
            boolean wasExecuted = loader.isExecuted();
            m_model.setHasContent(wasExecuted);
        }
        m_message = loader.getNodeMessage();
        m_outDataPortsMemoryPolicy = loader.getMemoryPolicy();
        if (m_outDataPortsMemoryPolicy == null) {
            m_outDataPortsMemoryPolicy = MemoryPolicy.CacheSmallInMemory;
        }
        try {
            m_model.loadSettingsFrom(loader.getNodeModelSettings());
        } catch (Exception e) {
            m_logger.error("Loading model settings failed", e);
        }
        for (int i = 0; i < getNrOutPorts(); i++) {
            NodeOutPort port = getOutPort(i);
            port.setPortObjectSpec(loader.getPortObjectSpec(i));
            port.setPortObject(loader.getPortObject(i), /* owner= */this);
        }
        loadInternals(loader.getNodeInternDirectory(), exec);
        if (m_message != null) {
            notifyMessageListeners(m_message);
        }
    }

    /**
     * Loads the settings (but not the data) from the given settings object.
     * 
     * @param settings a settings object
     * @throws InvalidSettingsException if an expected setting is missing
     */
    public void loadSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        SettingsLoaderAndWriter l = SettingsLoaderAndWriter.load(settings);
        try {
            NodeSettingsRO modelSettings = l.getModelSettings();
            m_model.validateSettings(modelSettings);
            m_model.loadSettingsFrom(modelSettings);
        } catch (InvalidSettingsException e) {
            throw e;
        } catch (Throwable t) {
            m_logger.error("Loading model settings failed", t);
        }
        if (getNrOutPorts() > 0) {
            // ensured to return non-null value
            m_outDataPortsMemoryPolicy = l.getMemoryPolicy();
        }
    }

    /**
     * Validates the argument settings.
     * 
     * @param settings a settings object
     * @return if valid.
     */
    public boolean areSettingsValid(final NodeSettingsRO settings) {
        try {
            SettingsLoaderAndWriter l = SettingsLoaderAndWriter.load(settings);
            NodeSettingsRO modelSettings = l.getModelSettings();
            m_model.validateSettings(modelSettings);
        } catch (InvalidSettingsException e) {
            return false;
        } catch (Throwable t) {
            m_logger.error("Validating settings failed", t);
            return false;
        }
        return true;
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
     * @deprecated Use the <code>NodeFactoryHTMLCreator</code> in connection
     *             with the {@link #getXMLDescription()}.
     * @return A html page containing the node's detailed description.
     * @see org.knime.core.node.NodeFactory#getXMLDescription
     */
    @Deprecated
    public String getFullHTMLDescription() {
        return m_factory.getNodeFullHTMLDescription();
    }

    /**
     * A short description of this node.
     * 
     * @deprecated Use the <code>NodeFactoryHTMLCreator</code> in connection
     *             with the {@link #getXMLDescription()}.
     * @return A single line containing a brief node description.
     * @see org.knime.core.node.NodeFactory#getXMLDescription
     */
    @Deprecated
    public String getOneLineDescription() {
        return m_factory.getNodeOneLineDescription();
    }

    /**
     * The XML description can be used with the
     * <code>NodeFactoryHTMLCreator</code> in order to get a converted HTML
     * description of it, which fits the overall KNIME HTML style.
     * 
     * @return XML description of the node
     * @see org.knime.core.node.NodeFactory#getXMLDescription()
     */
    public Element getXMLDescription() {
        return m_factory.getXMLDescription();
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
     * @return The total number of input ports.
     */
    public int getNrInPorts() {
        return m_model.getNrInPorts();
    }

    /**
     * @return The total number of output ports.
     */
    public int getNrOutPorts() {
        return m_model.getNrOutPorts();
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
    public NodeOutPort getOutPort(final int index) {
        return m_outPorts[index];
    }

    /**
     * 
     * @param index
     * @return
     */
    public NodeInPort getInPort(final int index) {
        return m_inPorts[index];
    }

    /**
     * Delegation method to the inport. {@link NodePort#getPortName()} method.
     * 
     * @param portID The port id of interest
     * @return The description to that port
     * @throws IndexOutOfBoundsException If argument is out of range.
     */
    public String getInportName(final int portID) {
        return m_inPorts[portID].getPortName();
    }

    /**
     * Delegation method to the outport. {@link NodePort#getPortName()} method.
     * 
     * @param portID The port id of interest.
     * @return The description to that port.
     * @throws IndexOutOfBoundsException If argument is out of range.
     */
    public String getOutportName(final int portID) {
        return m_outPorts[portID].getPortName();
    }

    /**
     * Get the policy for the data outports, that is, keep the output in main
     * memory or write it to disc. This method is used from within the
     * ExecutionContext when the derived NodeModel is executing.
     * 
     * @return The memory policy to use.
     */
    final MemoryPolicy getOutDataMemoryPolicy() {
        return m_outDataPortsMemoryPolicy;
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
     * Delegate method to node model.
     * 
     * @return Whether the node model has (potentially) content to be displayed.
     * @see GenericNodeModel#hasContent()
     */
    boolean hasContent() {
        return m_model.hasContent();
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
     * @param inData the datatables from the successors.
     * @return <code>true</code> if execution was successful otherwise
     *         <code>false</code>.
     * @see NodeModel#isConfigured()
     * @see NodeModel#execute(BufferedDataTable[],ExecutionContext)
     */
    public boolean execute(final PortObject[] inData,
            final ExecutionContext exec) {
        // start message and keep start time
        final long time = System.currentTimeMillis();
        m_logger.info("Start execute");
        // reset the message object
        m_message = null;
        // notify state listeners
        // TODO: NEWWFM State Event
        // notifyStateListeners(new NodeStateChangedEvent(
        // NodeStateChangedEvent.Type.START_EXECUTE));

        // 
        // EXECUTE the underlying node's model
        //
        // check for existence of all input tables
        // TODO allow for optional inputs
        for (int i = 0; i < inData.length; i++) {
            if (inData[i] == null) {
                m_logger.assertLog(false,
                        "Couldn't get data from predecessor (Port No." + i
                                + "). Is it executed?");
                m_logger.error("failed execute");
                // TODO NEWWFM state event
                m_message =
                        new NodeMessage(NodeMessage.Type.ERROR,
                                "Couldn't get data from predecessor (Port No."
                                        + i + "). Is it executed?");
                // TODO: also notify message/progress listeners
                notifyMessageListeners(m_message);
                // notifyStateListeners(new NodeStateChangedEvent.EndExecute());
                return false;
            }
        }

        // check for compatible input PortObjects
        for (int i = 0; i < inData.length; i++) {
            PortType thisType = m_model.getInPortType(i);
            if (!(thisType.getPortObjectClass().isInstance(inData[i]))) {
                m_logger.error("Connection Error: Mismatch"
                        + " of input port types (port " + i + ").");
                m_logger.error("  (Wanted: "
                        + thisType.getPortObjectClass().getName() + ", "
                        + "actual: " + inData[i].getClass().getName() + ")");
                m_message =
                        new NodeMessage(NodeMessage.Type.ERROR,
                                "Connection Error: Mismatch"
                                        + " of input port types (port " + i
                                        + ").");
                // TODO: return here???
            }
        }

        PortObject[] newOutData; // the new DTs from the model
        try {
            // INVOKE MODEL'S EXECUTE
            newOutData = m_model.executeModel(inData, exec);
            processModelWarnings();
        } catch (CanceledExecutionException cee) {
            // execution was canceled
            m_logger.info("execute canceled");
            reset();
            m_message =
                    new NodeMessage(NodeMessage.Type.WARNING,
                            "Execution canceled");
            return false;
        } catch (AssertionError ae) {
            m_logger.assertLog(false, ae.getMessage(), ae);
            reset();
            m_message =
                    new NodeMessage(NodeMessage.Type.ERROR, "Execute failed: "
                            + ae.getMessage());
            return false;
        } catch (Error e) {
            // some other error - should never happen!
            m_logger.fatal("Fatal error", e);
            reset();
            m_message =
                    new NodeMessage(NodeMessage.Type.ERROR, "Execute failed: "
                            + e.getMessage());
            return false;
        } catch (Exception e) {
            // execution failed
            m_logger.error("Execute failed", e);
            reset();
            m_message =
                    new NodeMessage(NodeMessage.Type.ERROR, "Execute failed: "
                            + e.getMessage());
            return false;
        } finally {
            if (m_message != null) {
                notifyMessageListeners(m_message);
            }
        }

        boolean continuesLoop =
                m_model.getScopeContextStackContainer().getLoopStatus() != null;
        // check for compatible output PortObjects
        for (int i = 0; i < newOutData.length; i++) {
            PortType thisType = m_model.getOutPortType(i);
            assert newOutData[i] != null || continuesLoop : "Null output from non-loopterminate node";
            if ((newOutData[i] != null)
                    && !thisType.getPortObjectClass().isInstance(newOutData[i])) {
                m_logger.error("Connection Error: Mismatch"
                        + " of output port types (port " + i + ").");
                m_logger
                        .error("  (Wanted: "
                                + thisType.getPortObjectClass().getName()
                                + ", " + "actual: "
                                + newOutData[i].getClass().getName() + ")");

                // TODO: is this redunant double checking?
                m_message =
                        new NodeMessage(NodeMessage.Type.ERROR,
                                "Connection Error: Mismatch"
                                        + " of output port types (port " + i
                                        + ").");
                notifyMessageListeners(m_message);
                return false;
            }
        }

        // spread the newly available PortObjects to the successors
        for (int p = 0; p < getNrOutPorts(); p++) {
            if (newOutData[p] instanceof BufferedDataTable) {
                BufferedDataTable thisTable = (BufferedDataTable)newOutData[p];
                DataTableSpec portSpec =
                        (DataTableSpec)(m_outPorts[p].getPortObjectSpec());
                DataTableSpec newPortSpec = thisTable.getDataTableSpec();
                if (portSpec != null) {
                    if (!portSpec.equalStructure(newPortSpec)) {
                        m_logger.coding("DataSpec generated by configure does"
                                + " not match spec after execution.");
                    }
                }
                BufferedDataTable t = thisTable;
                t.setOwnerRecursively(this);
                m_outPorts[p].setPortObject(t, this);
                m_outPorts[p].setPortObjectSpec(newPortSpec);
            } else {
                // TODO save them, don't simply hand them over!
                m_outPorts[p].setPortObject(newOutData[p], this);
                if (newOutData[p] != null) {
                    assert !continuesLoop;
                    m_outPorts[p].setPortObjectSpec(newOutData[p].getSpec());
                }
            }
            m_outPorts[p].setHiLiteHandler(m_model.getOutHiLiteHandler(p));
            m_outPorts[p]
                    .setScopeContextStackContainer(getScopeContextStackContainer());
        }
        m_logger.info("End execute (" + (System.currentTimeMillis() - time)
                / 100 / 10.0 + " sec)");
        return true;
    } // executeNode(ExecutionMonitor)

    /**
     * Checks the warnings in the model and notifies registered listeners.
     * 
     */
    private void processModelWarnings() {

        // get the warning message if available and create a message object
        // also notify all listeners
        String warningMessage = m_model.getWarningMessage();
        if (warningMessage != null) {

            m_logger.warn("Model warning message: " + warningMessage);
            m_message =
                    new NodeMessage(NodeMessage.Type.WARNING, "Warning: "
                            + warningMessage);
            notifyMessageListeners(m_message);
            // reset the warning message
            m_model.setWarningMessage(null);
        }
    }

    /**
     * Resets this node with out re-configuring it. All connected nodes will be
     * reset, as well as the <code>DataTable</code>s and
     * <code>PredictParams</code> at the outports.
     * 
     */
    public void reset() {
        m_logger.info("reset");
        // if reset had no exception, reset node message
        m_message = null;
        m_model.resetModel();
        // and make sure output ports are empty as well
        cleanOutPorts();
        // clear temporary tables that have been created during execute
        for (ContainerTable t : m_localTempTables) {
            t.clear();
        }
        m_localTempTables.clear();
    }

    public void cleanOutPorts() {
        m_logger.info("clean output ports.");
        // blow away our data tables in the port
        for (int p = 0; p < getNrOutPorts(); p++) {
            m_outPorts[p].setPortObjectSpec(null);
            m_outPorts[p].setPortObject(null, this);
        }
    }

    /**
     * closes, unregisters and disposes all node views and output port views.
     */
    public void disposeAllViews() {

        // close and unregister all node views
        NodeView[] views = m_model.getViews().toArray(new NodeView[0]);
        for (NodeView view : views) {
            // unregisters and closes the view
            view.closeView();
        }

        // close port views
        for (int o = 0; o < m_outPorts.length; o++) {
            m_outPorts[o].disposePortView(this);
        }

    }

    /**
     * Adds the argument set of tables to the set of temporary tables in this
     * node. Called after execute.
     * 
     * @param tempTables Tables to add, not <code>null</code>.
     */
    public void addToTemporaryTables(final Set<ContainerTable> tempTables) {
        m_localTempTables.addAll(tempTables);
    }

    /**
     * Enumerates the output tables and puts them into the global worflow
     * repository of tables. This method is basically delegates from the
     * NodeContainer class to access a package-scope method in
     * BufferedDataTable.
     * 
     * @param rep The global repository.
     */
    public void putOutputTablesIntoGlobalRepository(
            final HashMap<Integer, ContainerTable> rep) {
        for (NodeOutPort p : m_outPorts) {
            PortObject portObject = p.getPortObject();
            if (portObject instanceof BufferedDataTable) {
                BufferedDataTable t = (BufferedDataTable)portObject;
                if (t != null) {
                    t.putIntoTableRepository(rep);
                }
            }
        }
    }

    /**
     * Delegate method to allow access to the (package scope) method
     * {@link ExecutionContext#getLocalTableRepository()}. Called after
     * execution has finished to clean up temporary tables.
     * 
     * @param c To access.
     * @return Its local table repository.
     */
    public static HashMap<Integer, ContainerTable> getLocalTableRepositoryFromContext(
            final ExecutionContext c) {
        return c.getLocalTableRepository();
    }

    /**
     * Deletes any temporary resources associated with this node.
     */
    public void cleanup() {
        for (NodeOutPort p : m_outPorts) {
            PortObject portObject = p.getPortObject();
            if (portObject instanceof BufferedDataTable) {
                BufferedDataTable t = (BufferedDataTable)portObject;
                if (t != null) {
                    t.clear(this);
                }
            }
        }
    }

    // /**
    // * Notification method, called by an input port to tell the node about a
    // * disconnected outport from a predecessor. The notification is done, as
    // the
    // * node itself should be responsible to cause the required actions. The
    // node
    // * is reset and newly configured.
    // *
    // * @param inPortID The port id that just got disconnected.
    // */
    // void wasDisconnectedXXX(final int inPortID) {
    // boundInPort(inPortID);
    // // call reset
    // reset();
    //
    // if (isDataInPort(inPortID)) {
    // // reset hilite handler in this node.
    // // This triggers hilite handler propagation through the output ports
    // setHiLiteHandler(inPortID, null);
    // // re-create out table specs, as incoming table specs/models are
    // // gone.
    // configure(null);
    // } else { // then this is a ModelContent port
    // /*
    // * reset the ModelContent of this inport, previously pushed in and
    // * stored in this node.
    // */
    // int realId = inPortID - getNrDataInPorts();
    // try {
    // m_model.loadModelContent(realId, null);
    // // re-create out table specs, as incoming table specs/models are
    // // gone.
    // configure(null);
    // } catch (NullPointerException e) {
    // /*
    // * if the nodemodel implementation of the loadModelContent is
    // * correct we will not end up here.
    // */
    // String message =
    // "Incorrect implementation of "
    // + "method NodeModel.loadModelContent(): "
    // + "It must handle null parameters";
    //
    // m_logger.coding(message);
    //
    // m_status = new NodeStatus.Error(message);
    // notifyStateListeners(m_status);
    // } catch (Throwable t) {
    // /*
    // * if the nodemodel implementation of the loadModelContent is
    // * correct we will not end up here.
    // */
    // String message =
    // "Implementation error: NodeModel.loadModelContent()"
    // + " must handle null parameters";
    //
    // m_logger.coding(message, t);
    //
    // m_status = new NodeStatus.Error(message);
    // notifyStateListeners(m_status);
    // }
    // }
    // }

    // /**
    // * Notification method, called by an input port to tell the node about a
    // new
    // * available predictor model from a predecessor outport. The notification
    // is
    // * done, as the node itself should be responsible to cause the required
    // * actions. The predictor model is loaded into the model and the node is
    // * configured afterwards.
    // *
    // * @param inPortID The port ID that has a new predictor model spec
    // * available.
    // * @param predParams the new model content at the port
    // */
    // public void newModelContentAtPort(final int inPortID,
    // final ModelContentRO predParams) {
    // // Predictor params are propagated through model ports only
    // boundModelContentInPort(inPortID);
    // if (isExecuted()) {
    // reset();
    // }
    // try {
    // int realId = inPortID - getNrDataInPorts();
    // m_model.loadModelContent(realId, predParams);
    //
    // // NOTE: configure was previously invoked at the end of the method
    // // as this is not necessary and also would reset state messages
    // // set in the catch blocks the configure is only invoked,
    // // if the model could be properly set
    // configure(null);
    //
    // } catch (InvalidSettingsException ise) {
    // // NOTE: this reset was added to ensure that the node is reset
    // // (not configured - see the above NOTE) after an exception has
    // // been thrown during model load
    // reset();
    // m_logger.warn("Unable to load ModelContent: " + ise.getMessage());
    // m_status =
    // new NodeStatus.Error("Could not load ModelContent: "
    // + ise.getMessage());
    // notifyStateListeners(m_status);
    //
    // } catch (NullPointerException npe) {
    // reset();
    //
    // m_status =
    // new NodeStatus.Error(
    // "Could not load ModelContent due to null argument.");
    // notifyStateListeners(m_status);
    // } catch (Throwable e) {
    // reset();
    // m_logger.coding("Error occurred: ", e);
    // m_status =
    // new NodeStatus.Error(
    // "Could not load ModelContent due to an error: "
    // + e.getMessage());
    // notifyStateListeners(m_status);
    // }
    //
    // }

    /**
     * Sets all (new) incoming <code>DataTableSpec</code> elements in the
     * model, calls the model to create output table specs and propagates these
     * new specs to the connected successors.
     * 
     * @param inSpecs the tablespecs from the predecessors
     * @return flag indicating success of configure
     */
    public boolean configure(final PortObjectSpec[] inSpecs) {
        boolean success = false;
        synchronized (m_configureLock) {
            // reset message object
            m_message = null;

            NodeMessage localMessage = null;
            // need to init here as there may be an exception being thrown and
            // then we copy the null elements of this array to their destination
            PortObjectSpec[] newOutSpec = new PortObjectSpec[getNrOutPorts()];
            try {
                // check the inspecs against null
                for (int i = 0; i < inSpecs.length; i++) {
                    if (BufferedDataTable.class.isAssignableFrom(getInPort(i)
                            .getPortType().getPortObjectClass())
                            && (inSpecs[i] == null)) {
                        return false;
                        // TODO: did we really need a warning here??
                        // throw new InvalidSettingsException(
                        // "Node is not executable until all predecessors "
                        // + "are configured and/or executed.");
                    }
                }

                // call configure model to create output table specs
                // guaranteed to return non-null, correct-length array
                newOutSpec = m_model.configureModel(inSpecs);
                success = true;
            } catch (InvalidSettingsException ise) {
                m_logger.warn("Configure failed: " + ise.getMessage());
                localMessage =
                        new NodeMessage(NodeMessage.Type.WARNING, "Warning: "
                                + ise.getMessage());
            } catch (Exception e) {
                m_logger.error("Configure failed", e);
            } catch (Error e) {
                m_logger.fatal("Configure failed", e);
            } finally {
                /*
                 * set the new specs in the output ports, which will propagate
                 * them to connected successor nodes
                 */
                for (int p = 0; p < newOutSpec.length; p++) {
                    // update data table spec
                    m_outPorts[p].setPortObjectSpec(newOutSpec[p]);
                    m_outPorts[p]
                            .setScopeContextStackContainer(getScopeContextStackContainer());
                }

                processModelWarnings();
                if (localMessage != null) {
                    m_message = localMessage;
                    notifyMessageListeners(localMessage);
                }
            }
        }
        if (success) {
            m_logger.info("Configure succeeded. (" + this.getName() + ")");
        }
        return success;
    }

    /**
     * @return The number of available views.
     */
    public int getNrViews() {
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
     * @param viewIndex The view index to show.
     */
    public void showView(final int viewIndex) {
        showView(viewIndex, getName());
    }

    /**
     * Opens the node's view.
     * 
     * @param viewIndex The view's index to show.
     * @param nodeName The underlying node's name.
     */
    public void showView(final int viewIndex, final String nodeName) {
        try {
            getView(viewIndex, nodeName).openView();
        } catch (Exception e) {
            m_logger.error("Show view failed", e);
            m_message =
                    new NodeMessage(NodeMessage.Type.ERROR,
                            "View could not be opened, reason: "
                                    + e.getMessage());
            notifyMessageListeners(m_message);
        } catch (Error e) {
            m_logger.fatal("Show view failed", e);
            m_message =
                    new NodeMessage(NodeMessage.Type.ERROR,
                            "View could not be opened, reason: "
                                    + e.getMessage());
            notifyMessageListeners(m_message);
        }
    }

    /**
     * Return a new instance of the node's view (without opening it).
     * 
     * @param viewIndex The view's index to show up.
     * @param title the displayed view title.
     * @return The node view with the specified index.
     * @throws ArrayIndexOutOfBoundsException If the view index is out of range.
     */
    public GenericNodeView<?> getView(final int viewIndex, final String title) {
        GenericNodeView<?> view = m_factory.createNodeView(viewIndex, m_model);
        view.setViewTitle(title);
        return view;
    }

    /**
     * Closes all views.
     */
    public void closeAllViews() {
        Set<GenericNodeView<?>> views =
                new HashSet<GenericNodeView<?>>(m_model.getViews());
        for (GenericNodeView<?> view : views) {
            view.closeView();
        }
    }

    /**
     * Closes all output port views (data and model port views).
     */
    public void closeAllPortViews() {
        for (NodeOutPort port : m_outPorts) {
            port.disposePortView(this);
        }
    }

    /**
     * 
     * @return <code>true</code> if a dialog is available or the number of
     *         data outports is greater than zero.
     */
    public boolean hasDialog() {
        return m_factory.hasDialog() || (getNrOutPorts() > 0);
    }

    /**
     * Shows this node's dialog with the name of this node as title.
     * 
     * @see #showDialog(String)
     */
    // OBSOLETE
    // public void showDialog(final DataTableSpec[] specs) {
    // showDialog("Dialog - " + getName(), specs);
    // }
    /**
     * Opens the node's dialog and loads the current settings from the model
     * into the dialog.
     * 
     * @param title The title for the dialog to open.
     */
    // OBSOLETE
    // public void showDialog(final String title, final DataTableSpec[] specs) {
    // try {
    // if (hasDialog()) {
    // NodeDialog dlg = new NodeDialog(getDialogPane(specs), this);
    // dlg.openDialog();
    // }
    // } catch (Exception e) {
    // m_logger.error("show dialog failed", e);
    // m_status =
    // new NodeStatus.Error("Dialog could not be opend properly: "
    // + e.getMessage());
    // notifyStateListeners(m_status);
    // } catch (Error e) {
    // m_logger.fatal("show dialog failed", e);
    // m_status =
    // new NodeStatus.Error("Dialog could not be opend properly: "
    // + e.getMessage());
    // notifyStateListeners(m_status);
    // }
    // }
    /**
     * @param inSpecs The input specs, which will be forwarded to the dialog's
     *            {@link GenericNodeDialogPane# loadSettingsFrom(NodeSettingsRO,
     *            PortObjectSpec[])}.
     * @return The dialog pane which holds all the settings' components. In
     *         addition this method loads the settings from the model into the
     *         dialog pane.
     * @throws NotConfigurableException if the dialog cannot be opened because
     *             of real invalid settings or if any preconditions are not
     *             fulfilled, e.g. no predecessor node, no nominal column in
     *             input table, etc.
     * @throws IllegalStateException If node has no dialog.
     * @see #hasDialog()
     */
    public GenericNodeDialogPane getDialogPaneWithSettings(
            final PortObjectSpec[] inSpecs) throws NotConfigurableException {
        GenericNodeDialogPane dialogPane = getDialogPane();
        NodeSettingsRO settings = getSettingsFromNode();
        dialogPane.internalLoadSettingsFrom(settings, inSpecs);
        return dialogPane;
    }

    /**
     * Get reference to the node dialog instance. Used to get the user settings
     * from the dialog without overwriting them as in in
     * {@link #getDialogPaneWithSettings(PortObjectSpec[])}
     * 
     * @return Reference to dialog pane.
     * @throws IllegalStateException If node has no dialog.
     * @see #hasDialog()
     */
    public GenericNodeDialogPane getDialogPane() {
        if (m_dialogPane == null) {
            if (hasDialog()) {
                if (m_factory.hasDialog()) {
                    m_dialogPane = m_factory.createNodeDialogPane();
                } else {
                    assert (getNrOutPorts() > 0);
                    m_dialogPane = new MiscNodeDialogPane();
                }
                if (getNrOutPorts() > 0) {
                    m_dialogPane.addMiscTab();
                }
            } else {
                throw new IllegalStateException(
                        "Can't return dialog pane, node has no dialog!");
            }
        }
        return m_dialogPane;
    }

    public boolean areDialogAndNodeSettingsEqual() {
        if (m_dialogPane == null) {
            assert false : "No dialog available or not created yet";
            return true;
        }
        NodeSettingsRO dialogSettings;
        try {
            dialogSettings = getSettingsFromDialog();
        } catch (InvalidSettingsException ise) {
            return false;
        }
        NodeSettingsRO nodeSettings = getSettingsFromNode();
        return nodeSettings.equals(dialogSettings);
    }

    public void loadSettingsFromDialog() throws InvalidSettingsException {
        if (m_dialogPane == null) {
            assert false : "No dialog available or not created yet";
            return;
        }
        NodeSettingsRO dialogSettings = getSettingsFromDialog();
        loadSettingsFrom(dialogSettings);
    }

    private NodeSettingsRO getSettingsFromNode() {
        NodeSettings settings = new NodeSettings(getName());
        saveSettingsTo(settings);
        return settings;
    }

    private NodeSettingsRO getSettingsFromDialog()
            throws InvalidSettingsException {
        if (m_dialogPane == null) {
            assert false : "Dialog has not been instantiated";
            // fall back settings (not changed)
            return getSettingsFromNode();
        }
        NodeSettings settings = new NodeSettings(getName());
        m_dialogPane.finishEditingAndSaveSettingsTo(settings);
        return settings;
    }

    /**
     * Saves the settings (but not the data).
     * 
     * @param settings a settings object
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        SettingsLoaderAndWriter l = new SettingsLoaderAndWriter();
        l.setMemoryPolicy(m_outDataPortsMemoryPolicy);
        final NodeSettings model = new NodeSettings("field_ignored");
        try {
            m_model.saveSettingsTo(model);
        } catch (Exception e) {
            m_logger.error("Could not save model", e);
        } catch (Error e) {
            m_logger.fatal("Could not save model", e);
        }
        l.setModelSettings(model);
        l.save(settings);
    }

    void saveInternals(final File internDir, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        if (internDir.exists()) {
            FileUtil.deleteRecursively(internDir);
        }
        internDir.mkdir();
        if (internDir.canWrite()) {
            try {
                m_model.saveInternals(internDir, exec);
                processModelWarnings();
            } catch (IOException ioe) {
                m_message =
                        new NodeMessage(NodeMessage.Type.ERROR,
                                "Unable to save " + "internals: "
                                        + ioe.getMessage());
                m_logger.debug("saveInternals() failed with " + "IOException",
                        ioe);
                notifyMessageListeners(m_message);
            } catch (CanceledExecutionException e) {
                throw e;
            } catch (Exception e) {
                m_logger.coding("saveInternals() "
                        + "should only cause IOException.", e);
                m_message =
                        new NodeMessage(NodeMessage.Type.ERROR,
                                "Unable to save " + "internals: "
                                        + e.getMessage());
                notifyMessageListeners(m_message);
            }
        } else {
            String errorMessage =
                    "Unable to write directory: " + internDir.getAbsolutePath();
            m_logger.error(errorMessage);
            m_message = new NodeMessage(NodeMessage.Type.ERROR, errorMessage);
            notifyMessageListeners(m_message);
        }
    }

    void loadInternals(final File internDir, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        if (m_model.hasContent()) {
            try {
                m_model.loadInternals(internDir, exec);
                processModelWarnings();
            } catch (IOException ioe) {
                m_message =
                        new NodeMessage(NodeMessage.Type.ERROR,
                                "Unable to load internals: " + ioe.getMessage());
                m_logger.debug("loadInternals() failed with IOException", ioe);
                notifyMessageListeners(m_message);
            } catch (CanceledExecutionException e) {
                throw e;
            } catch (Exception e) {
                m_logger.coding("loadInternals() "
                        + "should only cause IOException.", e);
                m_message =
                        new NodeMessage(NodeMessage.Type.ERROR,
                                "Unable to load internals: " + e.getMessage());
                notifyMessageListeners(m_message);
            }
        }
    }

    /**
     * Validates the settings inside the model.
     * 
     * @throws InvalidSettingsException If not valid.
     */
    // OBSOLETE
    // public void validateModelSettingsFromDialog()
    // throws InvalidSettingsException {
    // // save new dialog's config into new object
    // NodeSettings newSettings = new NodeSettings(this.getName());
    // m_dialogPane.finishEditingAndSaveSettingsTo(newSettings);
    // m_model.validateSettings(newSettings.getNodeSettings(CFG_MODEL));
    // // validate settings
    // loadMiscSettingsFrom(newSettings.getNodeSettings(CFG_MISC_SETTINGS),
    // false);
    // }
    /**
     * Reads the current settings from the dialog and writes them into the
     * model.
     * 
     * @throws InvalidSettingsException If the settings are not valid for the
     *             underlying model.
     */
    // OBSOLETE, will be fixed by MICHAEL
    // public void loadModelSettingsFromDialog() throws InvalidSettingsException
    // {
    // // save new dialog's config into new object
    // NodeSettings newSettings = new NodeSettings(this.getName());
    // m_dialogPane.finishEditingAndSaveSettingsTo(newSettings);
    // // and apply it to the model
    // m_model.loadSettingsFrom(newSettings.getNodeSettings(CFG_MODEL));
    // // if this method is called, the dialog was at least open, so the
    // // the misc information is present (note: there were no misc infos
    // // before KNIME 1.2.0)
    // loadMiscSettingsFrom(newSettings.getNodeSettings(CFG_MISC_SETTINGS),
    // true);
    // }
    /**
     * Compares the current settings from the dialog with the settings from the
     * model.
     * 
     * @return true if the settings are equal
     */
    // OBSOLETE here
    // public boolean isModelAndDialogSettingsEqual() {
    // try {
    // // save new dialog's config into new object
    // NodeSettings dialogSettings = new NodeSettings("Compare");
    // m_dialogPane.finishEditingAndSaveSettingsTo(dialogSettings);
    // NodeSettings modelSettings = new NodeSettings("Compare");
    // NodeSettingsWO miscSettings =
    // modelSettings.addNodeSettings(CFG_MISC_SETTINGS);
    // saveMiscSettingsTo(miscSettings);
    // m_model.saveSettingsTo(modelSettings.addNodeSettings(CFG_MODEL));
    // // check for equality
    // return dialogSettings.isIdentical(modelSettings);
    // } catch (InvalidSettingsException ise) {
    // // if there are invalid settings it is assumed that the settings
    // // are not equal
    // return false;
    // }
    // }
    /**
     * Reads the current settings from the model and load them into the dialog
     * pane.
     * 
     * @throws NotConfigurableException if the dialog cannot be opened because
     *             of real invalid settings or if any preconditions are not
     *             fulfilled, e.g. no predecessor node, no nominal column in
     *             input table, etc.
     */
    // OBSOLETE
    // private void loadDialogSettingsFromModel(final PortObjectSpec[] specs)
    // throws NotConfigurableException {
    // // get the model's current settings ...
    // NodeSettings currSettings = new NodeSettings(this.getName());
    // NodeSettingsWO modelSettings = currSettings.addNodeSettings(CFG_MODEL);
    // m_model.saveSettingsTo(modelSettings);
    // NodeSettingsWO miscSettings =
    // currSettings.addNodeSettings(CFG_MISC_SETTINGS);
    // saveMiscSettingsTo(miscSettings);
    // // ... to init the dialog
    // m_dialogPane.internalLoadSettingsFrom(currSettings, specs);
    // }
    /**
     * Adds a state listener to this node. Ignored, if the listener is already
     * registered.
     * 
     * @param listener The listener to add.
     */
    public void addMessageListener(final NodeMessageListener listener) {
        if (listener == null) {
            throw new NullPointerException(
                    "Node message listener must not be null!");
        }
        m_messageListeners.add(listener);
    }

    /**
     * Removes a state listener from this node. Ignored, if the listener is not
     * registered.
     * 
     * @param listener The listener to remove.
     */
    public void removeMessageListener(final NodeMessageListener listener) {
        if (!m_messageListeners.remove(listener)) {
            m_logger.debug("listener was not registered: " + listener);
        }
    }

    /**
     * Notifies all state listeners that the state of this node has changed.
     * 
     * @param message The message object.
     */
    public void notifyMessageListeners(final NodeMessage message) {
        for (NodeMessageListener listener : m_messageListeners) {
            try {
                listener.messageChanged(new NodeMessageEvent(new NodeID(0),
                        message));
            } catch (Throwable t) {
                m_logger.error("Exception while notifying node listeners", t);
            }
        }
    }

    /**
     * Returns a string summary of this node.
     * 
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Node @" + hashCode() + " [" + m_name + ";in="
                + m_model.getNrInPorts() + ";out=" + m_outPorts.length + "]";
    }

    /**
     * @return The message object of this <code>Node</code>.
     */
    public NodeMessage getNodeMessage() {
        return m_message;
    }

    /**
     * @return the <code>NodeFactory</code> that constructed this node.
     */
    public GenericNodeFactory<GenericNodeModel> getFactory() {
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

    // //////////////////////
    // ScopeContext handling
    // //////////////////////

    public void setScopeContextStackContainer(final ScopeObjectStack scsc) {
        m_model.setScopeContextStackContainer(scsc);
    }

    public ScopeObjectStack getScopeContextStackContainer() {
        return m_model.getScopeContextStackContainer();
    }

    public void clearLoopStatus() {
        getScopeContextStackContainer().clearLoopStatus();
    }

    public ScopeContext getLoopStatus() {
        return getScopeContextStackContainer().getLoopStatus();
    }

    static class SettingsLoaderAndWriter {
        /**
         * Config for misc settings which are shown in the dialog. So far it
         * only contains a subsetting for the memory policy of the data
         * outports.
         */
        static final String CFG_MISC_SETTINGS = "internal_node_subsettings";

        private MemoryPolicy m_memoryPolicy = MemoryPolicy.CacheSmallInMemory;

        private NodeSettings m_modelSettings;

        /**
         * @return the memoryPolicy
         */
        MemoryPolicy getMemoryPolicy() {
            return m_memoryPolicy;
        }

        /**
         * @param memoryPolicy the memoryPolicy to set
         */
        final void setMemoryPolicy(final MemoryPolicy memoryPolicy) {
            if (memoryPolicy == null) {
                throw new NullPointerException("Memory Policy can't be null");
            }
            m_memoryPolicy = memoryPolicy;
        }

        /**
         * @return the modelSettings
         */
        NodeSettings getModelSettings() {
            return m_modelSettings;
        }

        /**
         * @param modelSettings the modelSettings to set
         */
        void setModelSettings(final NodeSettings modelSettings) {
            m_modelSettings = modelSettings;
        }

        static SettingsLoaderAndWriter load(final NodeSettingsRO settings)
                throws InvalidSettingsException {
            SettingsLoaderAndWriter result = new SettingsLoaderAndWriter();
            // in versions before KNIME 1.2.0, there were no misc settings
            // in the dialog, we must use caution here: if they are not present
            // we use the default, i.e. small data are kept in memory
            if (settings.containsKey(CFG_MISC_SETTINGS)
                    && settings.getNodeSettings(CFG_MISC_SETTINGS).containsKey(
                            Node.CFG_MEMORY_POLICY)) {
                NodeSettingsRO sub =
                        settings.getNodeSettings(CFG_MISC_SETTINGS);
                String memoryPolicy =
                        sub.getString(Node.CFG_MEMORY_POLICY,
                                MemoryPolicy.CacheSmallInMemory.toString());
                if (memoryPolicy == null) {
                    throw new InvalidSettingsException(
                            "Can't use null memory policy.");
                }
                MemoryPolicy p;
                try {
                    p = MemoryPolicy.valueOf(memoryPolicy);
                } catch (IllegalArgumentException iae) {
                    throw new InvalidSettingsException(
                            "Invalid memory policy: " + memoryPolicy);
                }
                result.m_memoryPolicy = p;
            } else {
                result.m_memoryPolicy = MemoryPolicy.CacheSmallInMemory;
            }
            result.m_modelSettings =
                    (NodeSettings)settings.getNodeSettings(CFG_MODEL);
            return result;
        }

        void save(final NodeSettingsWO settings) {
            if (m_memoryPolicy != null) {
                NodeSettingsWO sub =
                        settings.addNodeSettings(CFG_MISC_SETTINGS);
                sub.addString(CFG_MEMORY_POLICY, m_memoryPolicy.toString());
            }
            NodeSettingsWO model = settings.addNodeSettings(CFG_MODEL);
            m_modelSettings.copyTo(model);
        }
    }
}
