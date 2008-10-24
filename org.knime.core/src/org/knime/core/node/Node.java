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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.UIManager;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.NodeDialogPane.MiscNodeDialogPane;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.NodePersistor.LoadNodeModelSettingsFailPolicy;
import org.knime.core.node.config.ConfigEditTreeModel;
import org.knime.core.node.interrupt.InterruptibleNodeModel;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectSpecZipInputStream;
import org.knime.core.node.port.PortObjectSpecZipOutputStream;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortUtil;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.util.NodeExecutionJobManagerPool;
import org.knime.core.node.util.StringFormat;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.LoopStartNode;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.NodeMessageEvent;
import org.knime.core.node.workflow.NodeMessageListener;
import org.knime.core.node.workflow.ScopeLoopContext;
import org.knime.core.node.workflow.ScopeObjectStack;
import org.knime.core.node.workflow.ScopeVariable;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.util.FileUtil;
import org.w3c.dom.Element;

/**
 * Implementation of a node as basic processing unit within the workflow. A Node
 * object is the place where the data flow starts, ends, or intersects. Thus a
 * Node can be connected with predecessors and successors through its input and
 * output ports, {@link org.knime.core.node.workflow.NodeInPort} and
 * {@link org.knime.core.node.workflow.NodeOutPort}, respectively. There are data ports
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
public final class Node implements NodeModelWarningListener {

    /** The node logger for this class. */
    private final NodeLogger m_logger;
    /**
     * Config for node (and node container) settings which are shown in the
     * dialog.
     */
    public static final String CFG_MISC_SETTINGS = "internal_node_subsettings";

    /** The sub settings entry where the model can save its setup. */
    public static final String CFG_MODEL = "model";
    /** The sub settings entry containing the scope variable settings. These
     * settings are not available in the derived node model. */
    public static final String CFG_VARIABLES = "variables";

    /** The node's name. */
    private String m_name;

    /** The factory used to create model, dialog, and views. */
    private final NodeFactory<NodeModel> m_factory;

    /** The node's assigned node model. */
    private final NodeModel m_model;

    /** the last fired message (or null if none available). */
    private NodeMessage m_message;

    /** The node's dialog or <code>null</code> if not available. */
    private NodeDialogPane m_dialogPane;

    private NodeSettings m_variablesSettings;

    /** Keeps outgoing information (specs, objects, HiLiteHandlers...). */
    static class Output {
        String name;
        PortType type;
        PortObjectSpec spec;
        PortObject object;
        HiLiteHandler hiliteHdl;
        String summary;
    }
    private final Output[] m_outputs;

    /** Keeps information about incoming connectors (type and name). */
    static class Input {
        String name;
        PortType type;
    }
    private final Input[] m_inputs;

    /** The array of BDTs, that has been given by the interface
     * {@link BufferedDataTableHolder}. In most cases this is null. */
    private BufferedDataTable[] m_internalHeldTables;

    /** The listeners that are interested in node state changes. */
    private final CopyOnWriteArraySet<NodeMessageListener> m_messageListeners;

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
     * specified <code>NodeFactory</code>. Also initializes the input and output
     * ports for the given number of data and model port. This node is
     * configured after initialization.
     *
     * @param nodeFactory the node's factory for the creation of model, view,
     *            and dialog
     * @throws IllegalArgumentException If the <i>nodeFactory</i> is
     *             <code>null</code>.
     */
    public Node(final NodeFactory<NodeModel> nodeFactory) {
        if (nodeFactory == null) {
            throw new IllegalArgumentException("NodeFactory must not be null.");
        }
        m_factory = nodeFactory;
        m_name = m_factory.getNodeName().intern();
        m_model = m_factory.callCreateNodeModel();
        m_model.addWarningListener(this);
        m_logger = NodeLogger.getLogger(m_name);
        m_messageListeners = new CopyOnWriteArraySet<NodeMessageListener>();
        m_inputs = new Input[m_model.getNrInPorts()];
        for (int i = 0; i < m_inputs.length; i++) {
            m_inputs[i] = new Input();
            m_inputs[i].type = m_model.getInPortType(i);
            m_inputs[i].name = m_factory.getInportName(i);
        }

        m_outputs = new Output[m_model.getNrOutPorts()];
        for (int i = 0; i < m_outputs.length; i++) {
            m_outputs[i] = new Output();
            m_outputs[i].type = m_model.getOutPortType(i);
            m_outputs[i].name = m_factory.getOutportName(i);
            m_outputs[i].spec = null;
            m_outputs[i].object = null;
            m_outputs[i].summary = null;
            m_outputs[i].hiliteHdl = m_model.getOutHiLiteHandler(i);
        }

        m_localTempTables = new HashSet<ContainerTable>();
        // TODO (tg,po)
        // let the model create its 'default' table specs
        // configure(null);
    }

    /** Constructor used to copy the node.
     * @param original To copy from.
     */
    public Node(final Node original) {
        this(original.getFactory());
        NodeSettings settings = new NodeSettings("copy");
        try {
            original.saveSettingsTo(settings);
            loadSettingsFrom(settings);
        } catch (InvalidSettingsException e) {
            m_logger.debug("Unable to copy node settings", e);
            // silently ignore here
        }
    }

    LoadResult load(final NodePersistor loader, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        LoadResult result = new LoadResult();
        boolean hasContent = loader.hasContent();
        m_model.setHasContent(hasContent);
        try {
            // this also validates the settings
            loadSettingsFrom(loader.getSettings());
        } catch (Throwable e) {
            final String error;
            if (e instanceof InvalidSettingsException) {
                error = "Loading model settings failed: " + e.getMessage();
            } else {
                error = "Caught \"" + e.getClass().getSimpleName() + "\", "
                    + "Loading model settings failed: " + e.getMessage();
            }
            LoadNodeModelSettingsFailPolicy pol =
                loader.getModelSettingsFailPolicy();
            if (pol == null) {
                if (!loader.isConfigured()) {
                    pol = LoadNodeModelSettingsFailPolicy.IGNORE;
                } else if (loader.isExecuted()) {
                    pol = LoadNodeModelSettingsFailPolicy.WARN;
                } else {
                    pol = LoadNodeModelSettingsFailPolicy.FAIL;
                }
            }
            switch (pol) {
            case IGNORE:
                if (!(e instanceof InvalidSettingsException)) {
                    m_logger.coding(error, e);
                }
                break;
            case FAIL:
                result.addError(error);
                createErrorMessageAndNotify(error, e);
                loader.setNeedsResetAfterLoad();
                break;
            case WARN:
                createWarningMessageAndNotify(error, e);
                break;
            }
        }
        result.addError(loadDataAndInternals(loader, exec));
        exec.setProgress(1.0);
        return result;
    }
    
    /** Called after execute in order to put the computed result into the 
     * outports. This method is also used from the SNC to put computed results
     * from, e.g. grid execution into the node.
     * @param newOutData The computed output data
     * @param continuesLoop Whether the loop continue mask is set (permits
     *        null values)
     * @return Whether that is successful (false in case of incompatible port
     *         objects)
     */
    public boolean loadOutPortObjects(final PortObject[] newOutData, 
            final boolean continuesLoop) {
        // the following exception will not be thrown in a local execution
        // (i.e. from execute(...)); need to check anyway as this is a public
        // method
        if (newOutData == null) {
            throw new NullPointerException("Port object array must is null");
        }
        if (newOutData.length != getNrOutPorts()) {
            throw new IndexOutOfBoundsException("Array is expected to be of " 
                    + "length " + getNrOutPorts() + ": " + newOutData.length);
        }
        // check for compatible output PortObjects
        for (int i = 0; i < newOutData.length; i++) {
            PortType thisType = m_model.getOutPortType(i);
            if (newOutData[i] == null && !continuesLoop) {
                createErrorMessageAndNotify("Output at port " + i + " is null");
                return false;
            }
            if (newOutData[i] != null) {
                if (!thisType.getPortObjectClass().isInstance(newOutData[i])) {
                    createErrorMessageAndNotify("Invalid output port object " 
                            + "at port " + i);
                    m_logger.error("  (Wanted: "
                            + thisType.getPortObjectClass().getName()
                            + ", " + "actual: "
                            + newOutData[i].getClass().getName() + ")");
                    return false;
                }
                if (newOutData[i].getSpec() == null) {
                    createErrorMessageAndNotify("Implementation Error: "
                            + "PortObject \""
                            + newOutData[i].getClass().getName() + "\" must not"
                            + " have null spec (output port " + i + ").");
                    return false;
                }
            }
        }
        for (int p = 0; p < getNrOutPorts(); p++) {
            if (newOutData[p] instanceof BufferedDataTable) {
                BufferedDataTable thisTable = (BufferedDataTable)newOutData[p];
                DataTableSpec portSpec = (DataTableSpec)(m_outputs[p].spec);
                DataTableSpec newPortSpec = thisTable.getDataTableSpec();
                if (portSpec != null) {
                    if (!portSpec.equalStructure(newPortSpec)) {
                        String errorMsg = "DataSpec generated by configure does"
                            + " not match spec after execution.";
                        if (getNrOutPorts() > 1) {
                            errorMsg = errorMsg.concat(" (Port " + p + ")");
                        }
                        m_logger.coding(errorMsg);
                        createErrorMessageAndNotify(errorMsg);
                    }
                }
                BufferedDataTable t = thisTable;
                t.setOwnerRecursively(this);
                m_outputs[p].object = t;
                m_outputs[p].summary = t.getSummary();
                m_outputs[p].spec = newPortSpec;
            } else {
                m_outputs[p].object = newOutData[p];
                if (newOutData[p] != null) {
                    assert !continuesLoop;
                    m_outputs[p].spec = newOutData[p].getSpec();
                    m_outputs[p].summary = newOutData[p].getSummary();
                } else {
                    m_outputs[p].summary = null;
                }
            }
        }
        return true;
    }
    
    public LoadResult loadDataAndInternals(
            final NodeContentPersistor loader, final ExecutionMonitor exec) {
        LoadResult result = new LoadResult();
        NodeMessage nodeMessage = loader.getNodeMessage();
        if (nodeMessage != null) {
            notifyMessageListeners(nodeMessage);
        }
        for (int i = 0; i < getNrOutPorts(); i++) {
            Class<? extends PortObjectSpec> specClass =
                m_outputs[i].type.getPortObjectSpecClass();
            PortObjectSpec spec = loader.getPortObjectSpec(i);
            if (spec != null && !specClass.isInstance(spec)) {
                result.addError("Loaded PortObjectSpec of class \""
                        + spec.getClass().getSimpleName() + ", expected "
                        + specClass.getSimpleName());
                loader.setNeedsResetAfterLoad();
            } else {
                m_outputs[i].spec = spec;
            }
            
            Class<? extends PortObject> objClass =
                m_outputs[i].type.getPortObjectClass();
            PortObject obj = loader.getPortObject(i);
            if (obj != null && !objClass.isInstance(obj)) {
                result.addError("Loaded PortObject of class \""
                        + obj.getClass().getSimpleName() + ", expected "
                        + objClass.getSimpleName());
                loader.setNeedsResetAfterLoad();
            } else {
                m_outputs[i].object = obj;
                m_outputs[i].summary = loader.getPortObjectSummary(i);
            }
            if (m_outputs[i].object != null) {
                m_outputs[i].hiliteHdl = m_model.getOutHiLiteHandler(i);
            }
        }
        ReferencedFile internDirRef = loader.getNodeInternDirectory();
        if (internDirRef != null) {
            internDirRef.lock();
            try {
                exec.setMessage("Loading internals");
                m_model.loadInternals(internDirRef.getFile(), exec);
            } catch (Throwable e) {
                String error;
                if (e instanceof IOException) {
                    error = "Loading model internals failed: " + e.getMessage();
                    if (loader.mustWarnOnDataLoadError()) {
                        m_logger.debug(error, e);
                    } else {
                        m_logger.debug(error);
                    }
                } else {
                    error = "Caught \"" + e.getClass().getSimpleName() + "\", "
                        + "Loading model internals failed: " + e.getMessage();
                    m_logger.coding(error, e);
                }
                result.addError(error, true);
            } finally {
                internDirRef.unlock();
            }
        }
        if (m_model instanceof BufferedDataTableHolder) {
            m_internalHeldTables = loader.getInternalHeldTables();
            if (m_internalHeldTables != null) {
                BufferedDataTable[] copy = Arrays.copyOf(
                        m_internalHeldTables, m_internalHeldTables.length);
                ((BufferedDataTableHolder)m_model).setInternalTables(copy);
            }
        }
        return result;
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
        m_variablesSettings = l.getVariablesSettings();
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
     * Return name of input connector.
     *
     * @param index of the connector
     * @return The description
     * @throws IndexOutOfBoundsException If argument is out of range.
     */
    public String getInportName(final int index) {
        return m_inputs[index].name;
    }

    /**
     * Return type of input connector.
     *
     * @param index of the connector
     * @return The type
     * @throws IndexOutOfBoundsException If argument is out of range.
     */
    public PortType getInputType(final int index) {
        return m_inputs[index].type;
    }

    /**
     * Return name of output connector.
     *
     * @param index of the connector
     * @return The description to that port.
     * @throws IndexOutOfBoundsException If argument is out of range.
     */
    public String getOutportName(final int index) {
        return m_outputs[index].name;
    }

    /**
     * Return type of output connector.
     *
     * @param index of the connector
     * @return The type
     * @throws IndexOutOfBoundsException If argument is out of range.
     */
    public PortType getOutputType(final int index) {
        return m_outputs[index].type;
    }

    public PortObjectSpec getOutputSpec(final int index) {
        return m_outputs[index].spec;
    }

    public PortObject getOutputObject(final int index) {
        return m_outputs[index].object;
    }

    public String getOutputObjectSummary(final int index) {
        return m_outputs[index].summary;
    }

    public HiLiteHandler getOutputHiLiteHandler(final int index) {
        return m_outputs[index].hiliteHdl;
    }

    /** Get the current set of tables internally held by a NodeModel that
     * implements {@link BufferedDataTableHolder}. It may be null or contain
     * null elements. This array is modified upon load, execute and reset.
     * @return that array.
     */
    public BufferedDataTable[] getInternalHeldTables() {
        return m_internalHeldTables;
    }

    public void setInHiLiteHandler(final int index, final HiLiteHandler hdl) {
        m_model.setNewInHiLiteHandler(index, hdl);
    }

    /**
     * Delegate method to node model.
     *
     * @return Whether the node model has (potentially) content to be displayed.
     * @see NodeModel#hasContent()
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
     * @param data the data from the predecessor.
     * @return <code>true</code> if execution was successful otherwise
     *         <code>false</code>.
     * @see NodeModel#execute(BufferedDataTable[],ExecutionContext)
     */
    public boolean execute(final PortObject[] data,
            final ExecutionContext exec) {
        // start message and keep start time
        final long time = System.currentTimeMillis();
        m_logger.debug("Start execute");
        // reset the message object
        createResetMessageAndNotify();
        // notify state listeners
        // TODO: NEWWFM State Event
        // notifyStateListeners(new NodeStateChangedEvent(
        // NodeStateChangedEvent.Type.START_EXECUTE));

        PortObject[] inData = new PortObject[data.length];
        // check for existence of all input tables
        // TODO allow for optional inputs
        for (int i = 0; i < data.length; i++) {
            if (data[i] == null) {
                m_logger.error("execute failed, input contains null");
                // TODO NEWWFM state event
                // TODO: also notify message/progress listeners
                createErrorMessageAndNotify(
                        "Couldn't get data from predecessor (Port No."
                        + i + ").");
                // notifyStateListeners(new NodeStateChangedEvent.EndExecute());
                return false;
            }
            if (data[i] instanceof BufferedDataTable) {
                inData[i] = data[i];
            } else {
                exec.setMessage("Copying input object at port " +  i);
                ExecutionMonitor subExec = exec.createSubProgress(0.0);
                try {
                    inData[i] = copyPortObject(data[i], subExec);
                } catch (CanceledExecutionException e) {
                    createWarningMessageAndNotify("Execution canceled");
                    return false;
                } catch (Throwable e) {
                    createErrorMessageAndNotify("Unable to clone input data "
                            + "at port " + i + ": " + e.getMessage(), e);
                    return false;
                }
            }
        }

        // check for compatible input PortObjects
        for (int i = 0; i < inData.length; i++) {
            PortType thisType = m_model.getInPortType(i);
            if (!(thisType.getPortObjectClass().isInstance(inData[i]))) {
                createErrorMessageAndNotify("Connection Error: Mismatch"
                                        + " of input port types (port " + i
                                        + ").");
                m_logger.error("  (Wanted: "
                        + thisType.getPortObjectClass().getName() + ", "
                        + "actual: " + inData[i].getClass().getName() + ")");
                return false;
            }
        }

        PortObject[] newOutData; // the new DTs from the model
        try {
            // INVOKE MODEL'S EXECUTE
            // (warnings will now be processed "automatically" - we listen)
            newOutData = m_model.executeModel(inData, exec);
        } catch (CanceledExecutionException cee) {
            // execution was canceled
            reset(true);
            createWarningMessageAndNotify("Execution canceled");
            return false;
        } catch (Throwable th) {
            String message = "Execute failed: ";
            if (th.getMessage() != null && th.getMessage().length() >= 5) {
                message = message.concat(th.getMessage());
            } else {
                message = message.concat("(\"" + th.getClass().getSimpleName()
                        + "\"): " + th.getMessage());
            }
            reset(true);
            createErrorMessageAndNotify(message, th);
            return false;
        }
        // check if we see a loop status in the NodeModel
        ScopeLoopContext slc = m_model.getLoopStatus();
        boolean continuesLoop = (slc != null);
        if (!loadOutPortObjects(newOutData, continuesLoop)) {
            return false;
        }
        for (int p = 0; p < getNrOutPorts(); p++) {
            m_outputs[p].hiliteHdl = m_model.getOutHiLiteHandler(p);
        }

        if (m_model instanceof BufferedDataTableHolder) {
            // copy the table array to prevent later modification by the user
            BufferedDataTable[] internalTbls =
                ((BufferedDataTableHolder)m_model).getInternalTables();
            if (internalTbls != null) {
                m_internalHeldTables =
                    new BufferedDataTable[internalTbls.length];
                for (int i = 0; i < internalTbls.length; i++) {
                    BufferedDataTable t = internalTbls[i];
                    if (t != null) {
                        // if table is one of the input tables, wrap it in
                        // WrappedTable (otherwise table get's copied)
                        for (int in = 0; in < inData.length; in++) {
                            if (t == inData[in]) {
                                t = exec.createWrappedTable(t);
                                break;
                            }
                        }
                    }
                    m_internalHeldTables[i] = t;
                }
            } else {
                m_internalHeldTables = null;
            }
        }
        String elapsed = StringFormat.formatElapsedTime(
                System.currentTimeMillis() - time);
        m_logger.info("End execute (" + elapsed + ")");
        return true;
    } // executeNode(ExecutionMonitor)

    /** Copies the PortObject so that the copy can be given to the node model
     * implementation (and potentially modified). */
    private PortObject copyPortObject(final PortObject portObject,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        assert !(portObject instanceof BufferedDataTable) : "Must not copy BDT";

        // first copy the spec, then copy the object
        final PortObjectSpec s = portObject.getSpec();
        PortObjectSpec.PortObjectSpecSerializer ser =
            PortUtil.getPortObjectSpecSerializer(s.getClass());
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream(10 * 1024);
        PortObjectSpecZipOutputStream specOut =
            PortUtil.getPortObjectSpecZipOutputStream(byteOut);
        specOut.setLevel(0);
        ser.savePortObjectSpec(s, specOut);
        specOut.close();
        ByteArrayInputStream byteIn =
            new ByteArrayInputStream(byteOut.toByteArray());
        PortObjectSpecZipInputStream specIn =
            PortUtil.getPortObjectSpecZipInputStream(byteIn);
        PortObjectSpec specCopy = ser.loadPortObjectSpec(specIn);
        specIn.close();

        PortObject.PortObjectSerializer obSer =
            PortUtil.getPortObjectSerializer(portObject.getClass());
        byteOut.reset();
        PortObjectZipOutputStream objOut =
            PortUtil.getPortObjectZipOutputStream(byteOut);
        specOut.setLevel(0);
        obSer.savePortObject(portObject, objOut, exec);
        objOut.close();

        byteIn = new ByteArrayInputStream(byteOut.toByteArray());
        PortObjectZipInputStream objIn =
            PortUtil.getPortObjectZipInputStream(byteIn);
        PortObject result = obSer.loadPortObject(
                objIn, specCopy, exec);
        objIn.close();
        return result;
    }


    /**
     * Creates a new {@link NodeMessage} object of type warning and notifies
     * registered {@link NodeMessageListener}s. Also logs a warning message.
     *
     * @param warningMessage the new warning message
     */
    private void createWarningMessageAndNotify(final String warningMessage) {
        createWarningMessageAndNotify(warningMessage, null);
    }

    /**
     * Creates a new {@link NodeMessage} object of type warning and notifies
     * registered {@link NodeMessageListener}s. Also logs a warning message.
     * If a throwable is provided its stacktrace is logged at debug level.
     *
     * @param warningMessage the new warning message
     * @param t its stacktrace is logged at debug level.
     */
    private void createWarningMessageAndNotify(final String warningMessage,
            final Throwable t) {
        m_logger.warn(warningMessage);
        if (t != null) {
            m_logger.debug(warningMessage, t);
        }
        notifyMessageListeners(new NodeMessage(NodeMessage.Type.WARNING,
                warningMessage));
    }

    /**
     * Creates a new {@link NodeMessage} object of type error and notifies
     * registered {@link NodeMessageListener}s. Also logs an error message.
     *
     * @param errorMessage the new error message
     */
    private void createErrorMessageAndNotify(final String errorMessage) {
        createErrorMessageAndNotify(errorMessage, null);
    }

    /**
     * Creates a new {@link NodeMessage} object of type error and notifies
     * registered {@link NodeMessageListener}s. Also logs an error message.
     * If a throwable is provided its stacktrace is logged at debug level.
     *
     * @param errorMessage the new error message
     * @param t its stacktrace is logged at debug level.
     */
    private void createErrorMessageAndNotify(final String errorMessage, final
            Throwable t) {
        m_logger.error(errorMessage);
        if (t != null) {
            m_logger.debug(errorMessage, t);
        }
        notifyMessageListeners(new NodeMessage(NodeMessage.Type.ERROR,
                errorMessage));
    }

    /**
     * Notifies all registered {@link NodeMessageListener}s that the node's
     * message is cleared.
     */
    private void createResetMessageAndNotify() {
        notifyMessageListeners(NodeMessage.NONE);
    }

    /**
     * Is called, when a warning message is set in the {@link NodeModel}.
     * Forwards it to registered {@link NodeMessageListener}s.
     *
     * @param warningMessage the new message in the node model.
     */
    public void warningChanged(final String warningMessage) {

        // get the warning message if available and create a message object
        // also notify all listeners
        if (warningMessage != null) {
            createWarningMessageAndNotify(warningMessage);
        } else {
            createResetMessageAndNotify();
        }
    }

    /**
     * Resets this node without re-configuring it.
     * @param cleanMessages Whether to clear the node message, mostly true
     * but false if an execution has failed and we want to give the node model
     * a chance to clear its intermediate results.
     */
    public void reset(final boolean cleanMessages) {
        m_logger.debug("reset");
        // if reset had no exception, reset node message
        m_model.resetModel();
        if (cleanMessages) {
            createResetMessageAndNotify();
        }
        // and make sure output ports are empty as well
        cleanOutPorts();
        // clear temporary tables that have been created during execute
        for (ContainerTable t : m_localTempTables) {
            t.clear();
        }
        m_localTempTables.clear();
    }

    public void cleanOutPorts() {
        m_logger.debug("clean output ports.");
        // blow away our data tables in the port
        for (int i = 0; i < m_outputs.length; i++) {
            PortObject portObject = m_outputs[i].object;
            if (portObject instanceof BufferedDataTable) {
                ((BufferedDataTable)portObject).clear(this);
            }
            m_outputs[i].spec = null;
            m_outputs[i].object = null;
            m_outputs[i].summary = null;
        }
        if (m_internalHeldTables != null) {
            for (BufferedDataTable t : m_internalHeldTables) {
                if (t != null) {
                    t.clear(this);
                }
            }
        }
        m_internalHeldTables = null;
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
     * Enumerates the output tables and puts them into the global workflow
     * repository of tables. This method delegates from the NodeContainer class
     * to access a package-scope method in BufferedDataTable.
     *
     * @param rep The global repository.
     */
    public void putOutputTablesIntoGlobalRepository(
            final HashMap<Integer, ContainerTable> rep) {
        for (int i = 0; i < m_outputs.length; i++) {
            PortObject portObject = m_outputs[i].object;
            if (portObject instanceof BufferedDataTable) {
                BufferedDataTable t = (BufferedDataTable)portObject;
                t.putIntoTableRepository(rep);
            }
        }
        if (m_internalHeldTables != null) {
            // note: theoretically we don't need to put those into table rep
            // as they are either also part of m_outputs or not
            // available to downstream nodes. We do it anyway as we want to
            // treat both m_outputs and the internal tables as similar as
            // possible (particular during load)
            for (BufferedDataTable t : m_internalHeldTables) {
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
        cleanOutPorts();
        closeAllViews();
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

    /** Used before configure, to apply the variable mask to the nodesettings,
     * that is to change individual node settings to reflect the current values
     * of the variables (if any).
     * @return a map containing the exposed variables (which are visible to
     * downstream nodes. These variables are put onto the node's
     * {@link ScopeObjectStack}.
     */
    private Map<String, ScopeVariable> applySettingsUsingScopeStack()
        throws InvalidSettingsException {
        if (m_variablesSettings == null) {
            return Collections.emptyMap();
        }
        NodeSettings fromModel = new NodeSettings("model");
        try {
            m_model.saveSettingsTo(fromModel);
        } catch (Throwable e) {
            m_logger.error("Saving of model settings failed with "
                    + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
            String message = "Failed to apply scope variables; "
                + "model failed to save its settings";
            throw new InvalidSettingsException(message, e);
        }
        ConfigEditTreeModel configEditor;
        try {
            configEditor =
                ConfigEditTreeModel.create(fromModel, m_variablesSettings);
        } catch (final InvalidSettingsException e) {
            throw new InvalidSettingsException("Errors reading flow variables: "
                    + e.getMessage(), e);
        }
        Map<String, ScopeVariable> scopeVariablesMap =
            getScopeContextStackContainer().getAvailableVariables();
        List<ScopeVariable> newVariableList;
        try {
            newVariableList = configEditor.overwriteSettings(
                    fromModel, scopeVariablesMap);
        } catch (InvalidSettingsException e) {
            throw new InvalidSettingsException(
                    "Errors overwriting node settings with flow variables: "
                    + e.getMessage(), e);
        }
        try {
            m_model.validateSettings(fromModel);
        } catch (final Throwable e) {
            if (!(e instanceof InvalidSettingsException)) {
                m_logger.error("Validation of node settings failed with "
                        + e.getClass().getSimpleName(), e);
            }
            throw new InvalidSettingsException(
                    "Errors loading flow variables into node : "
                    + e.getMessage(), e);
        }
        try {
            m_model.loadValidatedSettingsFrom(fromModel);
        } catch (Throwable e) {
            if (!(e instanceof InvalidSettingsException)) {
                m_logger.error("Loading of node settings failed with "
                        + e.getClass().getSimpleName(), e);
            }
            m_logger.error("loadSettings failed after validation succeeded.");
            throw new InvalidSettingsException(
                    "Errors loading flow variables into node : "
                    + e.getMessage(), e);
        }
        Map<String, ScopeVariable> newVariableHash =
            new LinkedHashMap<String, ScopeVariable>();
        for (ScopeVariable v : newVariableList) {
            if (newVariableHash.put(v.getName(), v) != null) {
                m_logger.warn("Duplicate variable assignment for key \""
                        + v.getName() + "\")");
            }
        }
        return newVariableHash;
    }

    private void pushOntoStack(final Map<String, ScopeVariable> newVars) {
        ScopeObjectStack stack = getScopeContextStackContainer();
        ArrayList<ScopeVariable> reverseOrder =
            new ArrayList<ScopeVariable>(newVars.values());
        Collections.reverse(reverseOrder);
        for (ScopeVariable v : reverseOrder) {
            stack.push(v);
        }
    }

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
            createResetMessageAndNotify();
            // need to init here as there may be an exception being thrown and
            // then we copy the null elements of this array to their destination
            PortObjectSpec[] newOutSpec = new PortObjectSpec[getNrOutPorts()];
            Map<String, ScopeVariable> newVariables = Collections.emptyMap();
            try {
                if (m_variablesSettings != null) {
                    newVariables = applySettingsUsingScopeStack();
                }
                // check the inspecs against null
                for (int i = 0; i < inSpecs.length; i++) {
                    if (BufferedDataTable.class.isAssignableFrom(
                            m_inputs[i].type.getPortObjectClass())
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
                pushOntoStack(newVariables);
                success = true;
            } catch (InvalidSettingsException ise) {
                Throwable cause = ise.getCause();
                if (cause == null) {
                    createWarningMessageAndNotify(ise.getMessage());
                } else {
                    createWarningMessageAndNotify(ise.getMessage(), ise);
                }
            } catch (Throwable t) {
                String error = "Configure failed ("
                    + t.getClass().getSimpleName() + "): "
                    + t.getMessage();
                createErrorMessageAndNotify(error, t);
            } finally {
                for (int p = 0; p < newOutSpec.length; p++) {
                    // update data table spec
                    m_outputs[p].spec = newOutSpec[p];
                }
            }
        }
        if (success) {
            m_logger.debug("Configure succeeded. (" + this.getName() + ")");
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
     * Return a new instance of the node's view (without opening it).
     *
     * @param viewIndex The view's index to show up.
     * @param title the displayed view title.
     * @return The node view with the specified index.
     * @throws ArrayIndexOutOfBoundsException If the view index is out of range.
     */
    public NodeView<?> getView(final int viewIndex, final String title) {
        NodeView<?> view;
        try {
            view = m_factory.createNodeView(viewIndex, m_model);
        } catch (Throwable e) {
            m_logger.error("View instantiation failed", e);
            throw new RuntimeException(e.getMessage(), e);
        }
        return view;
    }

    /**
     * Closes all views.
     */
    public void closeAllViews() {
        Set<NodeView<?>> views =
                new HashSet<NodeView<?>>(m_model.getViews());
        for (NodeView<?> view : views) {
            view.closeView();
        }
    }

    /**
     * Returns true if this node can show a dialog. This is the case either,
     * if the node implementation provides a dialog, if the node has output
     * ports, or if more than one job managers are available.
     * @return <code>true</code> if a dialog is available.
     */
    public boolean hasDialog() {
        if (m_factory.hasDialog()) {
            return true;
        }
        if (getNrOutPorts() > 0) {
            // the framework creates a dialog for memory policy settings
            return true;
        }
        if (NodeExecutionJobManagerPool.getNumberOfJobManagers() > 1) {
            // framework creates a dialog for job manager selection
            return true;
        }
        // The default job manager (ThreadPool) has no settings. No need to
        // create a dialog if it is the only job manager.
        return false;
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
     *            {@link NodeDialogPane# loadSettingsFrom(NodeSettingsRO,
     *            PortObjectSpec[])}.
     * @param scopeStack The stack of variables.
     * @return The dialog pane which holds all the settings' components. In
     *         addition this method loads the settings from the model into the
     *         dialog pane.
     * @param settings The current settings of this node. The settings object
     *        will also contain the settings of the outer SNC.
     * @throws NotConfigurableException if the dialog cannot be opened because
     *             of real invalid settings or if any preconditions are not
     *             fulfilled, e.g. no predecessor node, no nominal column in
     *             input table, etc.
     * @throws IllegalStateException If node has no dialog.
     * @see #hasDialog()
     */
    public NodeDialogPane getDialogPaneWithSettings(
            final PortObjectSpec[] inSpecs, final ScopeObjectStack scopeStack,
            final NodeSettings settings) throws NotConfigurableException {
        NodeDialogPane dialogPane = getDialogPane();
        PortType[] inTypes = new PortType[getNrInPorts()];
        for (int i = 0; i < inTypes.length; i++) {
            PortType t = getInputType(i);
            if (!t.acceptsPortObjectSpec(inSpecs[i])) {
                throw new IllegalArgumentException(
                        "Invalid incoming port object spec \""
                                + inSpecs[i].getClass().getSimpleName()
                                + "\", expected \""
                                + t.getPortObjectSpecClass().getSimpleName()
                                + "\"");
            }
            inTypes[i] = t;
        }
        dialogPane.internalLoadSettingsFrom(
                settings, inTypes, inSpecs, scopeStack);
        return dialogPane;
    }

    /**
     * Get reference to the node dialog instance. Used to get the user settings
     * from the dialog without overwriting them as in in
     * {@link #getDialogPaneWithSettings(
     * PortObjectSpec[], ScopeObjectStack, NodeSettings)}
     *
     * @return Reference to dialog pane.
     * @throws IllegalStateException If node has no dialog.
     * @see #hasDialog()
     */
    public NodeDialogPane getDialogPane() {
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
                if (NodeExecutionJobManagerPool.getNumberOfJobManagers() > 1) {
                    m_dialogPane.addJobMgrTab();
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
        final NodeSettings model = new NodeSettings("field_ignored");
        try {
            m_model.saveSettingsTo(model);
        } catch (Exception e) {
            m_logger.error("Could not save model", e);
        } catch (Throwable t) {
            m_logger.fatal("Could not save model", t);
        }
        l.setModelSettings(model);
        l.setVariablesSettings(m_variablesSettings);
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
            } catch (CanceledExecutionException e) {
                throw e;
            } catch (Throwable t) {
                String details = "<no details available>";
                if (t.getMessage() != null && t.getMessage().length() > 0) {
                    details = t.getMessage();
                }

                String errMsg;
                if (t instanceof IOException) {
                    errMsg = "I/O error while saving internals: " + details;
                } else {
                    errMsg = "Unable to save internals: " + details;
                    m_logger.coding("saveInternals() "
                            + "should only cause IOException.", t);
                }
                createErrorMessageAndNotify(errMsg, t);
            }
        } else {
            String errorMessage =
                    "Unable to write directory: " + internDir.getAbsolutePath();
            createErrorMessageAndNotify(errorMessage);
        }
    }

    void loadInternals(final File internDir, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        if (m_model.hasContent()) {
            try {
                m_model.loadInternals(internDir, exec);
            } catch (CanceledExecutionException e) {
                throw e;
            } catch (Throwable e) {
                String details = "<no details available>";
                if (e.getMessage() != null && e.getMessage().length() > 0) {
                    details = e.getMessage();
                }
                createErrorMessageAndNotify("Unable to load internals: "
                        + details, e);
                if (!(e instanceof IOException)) {
                    m_logger.coding("loadInternals() "
                            + "should only cause IOException.", e);
                }
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
     * Notifies all state listeners that the message of this node has changed.
     *
     * @param message The message object.
     */
    public void notifyMessageListeners(final NodeMessage message) {
        // remember old message (in case we want to store status of this node)
        m_message = message;
        // fire event to all listeners
        for (NodeMessageListener listener : m_messageListeners) {
            try {
                listener.messageChanged(new NodeMessageEvent(
                        new NodeID(0), message));
            } catch (Throwable t) {
                m_logger.error("Exception while notifying node listeners", t);
            }
        }
    }

    /**
     * @return Last fired message.
     */
    public NodeMessage getNodeMessage() {
        return m_message;
    }

    /**
     * Returns a string summary of this node.
     *
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Node @" + hashCode() + " [" + m_name + ";in="
                + m_model.getNrInPorts() + ";out=" + m_outputs.length + "]";
    }

    /**
     * @return the <code>NodeFactory</code> that constructed this node.
     */
    public NodeFactory<NodeModel> getFactory() {
        return m_factory;
    }

    /**
     * @return true if this node's model is a interruptible model
     */
    public boolean isInterruptible() {
        return (m_model instanceof InterruptibleNodeModel);
    }

    /** Ensures that any port object is read for later saving with a
     * newer version. */
    public void ensureOutputDataIsRead() {
        for (Output p : m_outputs) {
            if (p.object instanceof BufferedDataTable) {
                ((BufferedDataTable)p.object).ensureOpen();
            }
        }
        for (ContainerTable t : m_localTempTables) {
            t.ensureOpen();
        }
    }

    /** Exposes {@link BufferedDataTable#ensureOpen()} as public method. This
     * method has been added here in order to keep the scope of the above method
     * at a minimum.
     * @param table To invoke this method on.
     */
    public static void invokeEnsureOpen(final BufferedDataTable table) {
        table.ensureOpen();
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
        m_model.clearLoopStatus();
    }

    public ScopeLoopContext getLoopStatus() {
        return m_model.getLoopStatus();
    }

    public static enum LoopRole { BEGIN, END, NONE };

    public final LoopRole getLoopRole() {
        if (m_model instanceof LoopStartNode) {
            return LoopRole.BEGIN;
        } else if (m_model instanceof LoopEndNode) {
            return LoopRole.END;
        } else {
            return LoopRole.NONE;
        }
    }

    public void setLoopEndNode(final Node tail) {
        if (tail == null) {
            m_model.setLoopEndNode(null);
        } else {
            m_model.setLoopEndNode(tail.m_model);
        }
    }

    public void setLoopStartNode(final Node head) {
        if (head == null) {
            m_model.setLoopStartNode(null);
        } else {
            if (!(head.m_model instanceof LoopStartNode)) {
                throw new ClassCastException("Node.setLoopStartNode called with"
                    + "wrong argument. Not a LoopStartNode!");
            }
            m_model.setLoopStartNode((LoopStartNode)head.m_model);
        }
    }

    static class SettingsLoaderAndWriter {

        private NodeSettings m_variablesSettings =
            new NodeSettings("variables");
        private NodeSettings m_modelSettings;

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

        /**
         * @return the variableSettings
         */
        NodeSettings getVariablesSettings() {
            return m_variablesSettings;
        }

        /**
         * @param variablesSettings the variablesSettings to set
         */
        void setVariablesSettings(final NodeSettings variablesSettings) {
            m_variablesSettings = variablesSettings;
        }

        static SettingsLoaderAndWriter load(final NodeSettingsRO settings)
                throws InvalidSettingsException {
            SettingsLoaderAndWriter result = new SettingsLoaderAndWriter();
            // in versions before KNIME 1.2.0, there were no misc settings
            // in the dialog, we must use caution here: if they are not present
            // we use the default.
            if (settings.containsKey(CFG_VARIABLES)) {
                result.m_variablesSettings =
                    (NodeSettings)settings.getNodeSettings(CFG_VARIABLES);
            } else {
                result.m_variablesSettings = null;
            }
            result.m_modelSettings =
                (NodeSettings)settings.getNodeSettings(CFG_MODEL);
            return result;
        }

        void save(final NodeSettingsWO settings) {
            NodeSettingsWO model = settings.addNodeSettings(CFG_MODEL);
            m_modelSettings.copyTo(model);
            if (m_variablesSettings != null) {
                NodeSettingsWO variables =
                    settings.addNodeSettings(CFG_VARIABLES);
                m_variablesSettings.copyTo(variables);
            }
        }
    }
}
