/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * --------------------------------------------------------------------- *
 *
 * History
 *   17.01.2006(sieb, ohl): reviewed
 */
package org.knime.core.node;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.UIManager;

import org.apache.commons.io.output.DeferredFileOutputStream;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainerException;
import org.knime.core.data.filestore.internal.IFileStoreHandler;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.NodePersistor.LoadNodeModelSettingsFailPolicy;
import org.knime.core.node.config.ConfigEditTreeModel;
import org.knime.core.node.interactive.AbstractInteractiveNodeView;
import org.knime.core.node.interactive.InteractiveNode;
import org.knime.core.node.interrupt.InterruptibleNodeModel;
import org.knime.core.node.missing.MissingNodeModel;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectSpecZipInputStream;
import org.knime.core.node.port.PortObjectSpecZipOutputStream;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortUtil;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec;
import org.knime.core.node.port.inactive.InactiveBranchConsumer;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.util.NodeExecutionJobManagerPool;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.FlowLoopContext;
import org.knime.core.node.workflow.FlowObjectStack;
import org.knime.core.node.workflow.FlowScopeContext;
import org.knime.core.node.workflow.FlowTryCatchContext;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.LoopStartNode;
import org.knime.core.node.workflow.NodeContainer.NodeContainerSettings.SplitType;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.NodeMessageEvent;
import org.knime.core.node.workflow.NodeMessageListener;
import org.knime.core.node.workflow.ScopeEndNode;
import org.knime.core.node.workflow.ScopeStartNode;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.execresult.NodeExecutionResult;
import org.knime.core.util.FileUtil;
import org.w3c.dom.Element;

/**
 * Implementation of a node as basic processing unit within the workflow. A Node
 * object is the place where the data flow starts, ends, or intersects. Thus a
 * Node can be connected with predecessors and successors through its input and
 * output ports, {@link org.knime.core.node.workflow.NodeInPort} and
 * {@link org.knime.core.node.workflow.NodeOutPort}, respectively. There are
 * data ports for exchanging data tables, and prediction model ports for
 * transferring computed data models. <br />
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
    /** The sub settings entry containing the flow variable settings. These
     * settings are not available in the derived node model. */
    public static final String CFG_VARIABLES = "variables";

    static final boolean DIALOG_IN_EDT =
        Boolean.getBoolean(KNIMEConstants.PROPERTY_DIALOG_IN_EDT);

    /** The node's name. */
    private String m_name;

    /** The factory used to create model, dialog, and views. */
    private final NodeFactory<NodeModel> m_factory;

    /** The node's assigned node model. */
    private final NodeModel m_model;

    /** The node's dialog or <code>null</code> if not available. */
    private NodeDialogPane m_dialogPane;

    private NodeSettings m_variablesSettings;

    private boolean m_forceSychronousIO;

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
        Input(final String n, final PortType t) {
            m_name = n;
            m_type = t;
        }
        private final String m_name;
        private final PortType m_type;
        public String getName() { return m_name; }
        public PortType getType() { return m_type; }
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

    /** File store handler that is non null during and after execution.
     * Set null on reset. */
    private IFileStoreHandler m_fileStoreHandler;

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
        } catch (Throwable e) {
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
        this(nodeFactory, null);
    }

    public Node(final NodeFactory<NodeModel> nodeFactory,
            final NodeCreationContext context) {

        if (nodeFactory == null) {
            throw new IllegalArgumentException("NodeFactory must not be null.");
        }
        m_factory = nodeFactory;
        m_name = m_factory.getNodeName().intern();
        m_model = m_factory.callCreateNodeModel(context);
        m_model.addWarningListener(this);
        m_logger = NodeLogger.getLogger(m_name);
        m_messageListeners = new CopyOnWriteArraySet<NodeMessageListener>();
        // create an extra input port (index: 0) for the optional variables.
        m_inputs = new Input[m_model.getNrInPorts() + 1];
        m_inputs[0] = new Input("Variable Inport",
                new PortType(FlowVariablePortObject.class, true));
        for (int i = 1; i < m_inputs.length; i++) {
            m_inputs[i] = new Input(m_factory.getInportName(i - 1),
                    m_model.getInPortType(i - 1));
        }

        // create an extra output port (index: 0) for the variables.
        m_outputs = new Output[m_model.getNrOutPorts() + 1];
        m_outputs[0] = new Output();
        m_outputs[0].type = new PortType(FlowVariablePortObject.class, true);
        m_outputs[0].name = "Variable Outport";
        m_outputs[0].spec = null;
        m_outputs[0].object = null;
        m_outputs[0].summary = null;
        m_outputs[0].hiliteHdl = null;
        for (int i = 1; i < m_outputs.length; i++) {
            m_outputs[i] = new Output();
            m_outputs[i].type = m_model.getOutPortType(i - 1);
            m_outputs[i].name = m_factory.getOutportName(i - 1);
            m_outputs[i].spec = null;
            m_outputs[i].object = null;
            m_outputs[i].summary = null;
            m_outputs[i].hiliteHdl = m_model.getOutHiLiteHandler(i - 1);
        }
        m_localTempTables = new HashSet<ContainerTable>();
        setForceSynchronousIO(false); // may set to true if this is a loop end
    }

    /** Create a persistor that is used to paste a copy of this node into
     * the same or a different workflow. (Used by copy&paste actions and
     * undo operations)
     * @return A new copy persistor that clones this node's settings. The copy
     * has a non-executed state (ports and internals are not copied).
     */
    public CopyNodePersistor createCopyPersistor() {
        return new CopyNodePersistor(this);
    }

    /** Load settings and data + internals from a loader instance.
     * @param loader To load from
     * @param exec For progress information/cancelation
     * @param loadResult Where to report errors/warnings to
     * @throws CanceledExecutionException If canceled.
     */
    void load(final NodePersistor loader, final ExecutionMonitor exec,
            final LoadResult loadResult) throws CanceledExecutionException {
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
                loadResult.addError(error);
                createErrorMessageAndNotify(error, e);
                loader.setNeedsResetAfterLoad();
                break;
            case WARN:
                createWarningMessageAndNotify(error, e);
                loadResult.addWarning(error);
                loader.setDirtyAfterLoad();
                break;
            }
        }
        loadDataAndInternals(loader, exec, loadResult);
        exec.setProgress(1.0);
    }

    /**
     * Creates an execution result containing all calculated values in a
     * execution. The returned value is suitable to be used in
     * {@link #loadDataAndInternals(
     * NodeContentPersistor, ExecutionMonitor, LoadResult)}.
     * If this node is not executed, it will assign null values to the fields
     * in the returned execution result.
     * @param exec For progress information.
     * @return A new execution result containing the values being calculated.
     * @throws CanceledExecutionException If canceled
     */
    public NodeExecutionResult createNodeExecutionResult(
            final ExecutionMonitor exec) throws CanceledExecutionException {
        NodeExecutionResult result = new NodeExecutionResult();
        result.setWarningMessage(m_model.getWarningMessage());
        if (hasContent()) {
            File internTempDir;
            try {
                internTempDir = FileUtil.createTempDir("knime_node_internDir");
                exec.setMessage("Saving internals");
                saveInternals(internTempDir, exec.createSubProgress(0.0));
                result.setNodeInternDir(new ReferencedFile(internTempDir));
            } catch (IOException ioe) {
                m_logger.error("Unable to save internals", ioe);
            }
        }
        if (m_internalHeldTables != null) {
            BufferedDataTable[] internalHeldTables = Arrays.copyOf(
                    m_internalHeldTables, m_internalHeldTables.length);
            result.setInternalHeldTables(internalHeldTables);
        }
        PortObject[] pos = new PortObject[getNrOutPorts()];
        PortObjectSpec[] poSpecs = new PortObjectSpec[getNrOutPorts()];
        for (int i = 0; i < pos.length; i++) {
            PortObject po = getOutputObject(i);
            if (po != null) {
                pos[i] = po;
                poSpecs[i] = po.getSpec();
            }
        }
        result.setPortObjects(pos);
        result.setPortObjectSpecs(poSpecs);
        return result;
    }

    /** Check class of the spec instance.
     * @param spec The spec
     * @param portIdx the port
     * @return <code>true</code> if the spec is valid (null, correct class
     *         or inactive port object spec)
     */
    private boolean checkPortObjectSpecClass(
            final PortObjectSpec spec, final int portIdx) {
        if (spec == null) {
            return true;
        } else if (spec instanceof InactiveBranchPortObjectSpec) {
            return true;
        } else {
            Class<? extends PortObjectSpec> specClass =
                m_outputs[portIdx].type.getPortObjectSpecClass();
            return specClass.isInstance(spec);
        }
    }

    /** Check class of the object instance.
     * @param spec The port object
     * @param portIdx the port
     * @return <code>true</code> if the object is valid (null, correct class
     *         or inactive port object)
     */
    private boolean checkPortObjectClass(
            final PortObject obj, final int portIdx) {
        if (obj == null) {
            return true;
        } else if (obj instanceof InactiveBranchPortObject) {
            return true;
        } else {
            Class<? extends PortObject> objClass =
                m_outputs[portIdx].type.getPortObjectClass();
            return objClass.isInstance(obj);
        }
    }

    /** Loads data from an argument persistor.
     * @param loader To load from.
     * @param exec For progress.
     * @param loadResult to add errors and warnings to (if any)
     * @noreference This method is not intended to be referenced by clients.
     */
    public void loadDataAndInternals(final NodeContentPersistor loader,
            final ExecutionMonitor exec, final LoadResult loadResult) {
        boolean hasContent = loader.hasContent();
        m_model.setHasContent(hasContent);
        m_fileStoreHandler = loader.getFileStoreHandler();
        for (int i = 0; i < getNrOutPorts(); i++) {
            PortObjectSpec spec = loader.getPortObjectSpec(i);
            if (checkPortObjectSpecClass(spec, i)) {
                m_outputs[i].spec = spec;
            } else {
                Class<? extends PortObjectSpec> specClass =
                    m_outputs[i].type.getPortObjectSpecClass();
                loadResult.addError("Loaded PortObjectSpec of class \""
                        + spec.getClass().getSimpleName() + ", expected "
                        + specClass.getSimpleName());
                loader.setNeedsResetAfterLoad();
            }

            PortObject obj = loader.getPortObject(i);
            if (checkPortObjectClass(obj, i)) {
                m_outputs[i].object = obj;
                m_outputs[i].summary = loader.getPortObjectSummary(i);
            } else {
                Class<? extends PortObject> objClass =
                    m_outputs[i].type.getPortObjectClass();
                loadResult.addError("Loaded PortObject of class \""
                        + obj.getClass().getSimpleName() + ", expected "
                        + objClass.getSimpleName());
                loader.setNeedsResetAfterLoad();
            }
            if (m_outputs[i].object != null) {
                // overwrites the spec that is read few rows above
                spec = m_outputs[i].object.getSpec();
                m_outputs[i].spec = spec;
                m_outputs[i].hiliteHdl =
                    (i == 0) ? null : m_model.getOutHiLiteHandler(i - 1);
            }
        }
        m_model.restoreWarningMessage(loader.getWarningMessage());
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
                loadResult.addError(error, true);
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
    }

//    /** Calls {@link NodeFactory#createNodeConfiguration(ConfigRegistry)} and
//     * returns it.
//     * @return The config as defined by the factory.
//     */
//    // to be made private
//    NodeConfiguration createNewEmptyNodeConfiguration() {
//        ConfigRegistry configReg = ConfigRegistry.internalCreateNew("node");
//        NodeConfiguration nc;
//        try {
//            nc = m_factory.createNodeConfiguration(configReg);
//        } catch (final Exception e) {
//            m_logger.coding("Failed to create node configuration "
//                    + "from factory, node will have no settings", e);
//            nc = new NodeConfiguration(configReg);
//        }
//        configReg.setDisallowElementAdding();
//        // nc might be null for old obsolete node settings based nodes.
//        return nc;
//    }

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
        return m_model.getNrInPorts() + 1;
    }

    /**
     * @return The total number of output ports.
     */
    public int getNrOutPorts() {
        return m_model.getNrOutPorts() + 1;
    }

    /**
     * Return name of input connector.
     *
     * @param index of the connector
     * @return The description
     * @throws IndexOutOfBoundsException If argument is out of range.
     */
    public String getInportName(final int index) {
        return m_inputs[index].getName();
    }

    /**
     * Return the name of the port as specified by the node factory (which
     * takes it from the node description).
     * @param index the port index
     * @return the name of the port as specified by the node factory.
     */
    public String getInportDescriptionName(final int index) {
        if (index <= 0) {
            return "Variable Inport";
        }
        return m_factory.getInportName(index - 1);
    }

    /**
     * Return type of input connector.
     *
     * @param index of the connector
     * @return The type
     * @throws IndexOutOfBoundsException If argument is out of range.
     */
    public PortType getInputType(final int index) {
        return m_inputs[index].getType();
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
     * Return the name of the port as specified by the node factory (which
     * takes it from the node description).
     * @param index the port index
     * @return the name of the port as specified by the node factory.
     */
    public String getOutportDescriptionName(final int index) {
        if (index <= 0) {
            return "Variable Outport";
        }
        return m_factory.getOutportName(index - 1);
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
        assert 0 <= index && index < getNrInPorts();
        if (index > 0) {
            // ignore HiLiteHandler on optional variable input port
            m_model.setNewInHiLiteHandler(index - 1, hdl);
        }
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
     * @return true if configure or execute were skipped because nodes is
     *   part of an inactive branch.
     * @see InactiveBranchPortObjectSpec
     */
    public boolean isInactive() {
        return m_outputs[0].spec == null ? false
                : m_outputs[0].spec instanceof InactiveBranchPortObjectSpec;
    }

    /** Returns true if the contained model is an instance of
     * {@link InactiveBranchConsumer}.
     * @return Such a property.
     */
    public boolean isInactiveBranchConsumer() {
        return m_model instanceof InactiveBranchConsumer;
    }

    /** Iterates the argument array and returns true if any element
     * is instance of {@link InactiveBranchPortObject}.
     * @param ins The input data
     * @return true if any input is inactive.
     */
    public static boolean containsInactiveObjects(final PortObject[] ins) {
        for (PortObject portObject : ins) {
            if (portObject instanceof InactiveBranchPortObject) {
                return true;
            }
        }
        return false;
    }

   /**
    * @param rawData the data from the predecessor.
    * @param reExecute is true when node is reExecuted
    * @param exec The execution monitor.
    * @return <code>true</code> if execution was successful otherwise
    *         <code>false</code>.
    * @see Node#execute(BufferedDataTable[],ExecutionContext)
    */
    @Deprecated
    public boolean execute(final PortObject[] rawData, final ExecutionContext exec) {
        return execute(rawData, false, exec);
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
     * @param rawData the data from the predecessor.
     * @param reExecute is true when node is reExecuted
     * @param exec The execution monitor.
     * @return <code>true</code> if execution was successful otherwise
     *         <code>false</code>.
     * @see NodeModel#execute(BufferedDataTable[],ExecutionContext)
     * @noreference This method is not intended to be referenced by clients.
     * @since 2.8
     */
    public boolean execute(final PortObject[] rawData, final boolean reExecute, final ExecutionContext exec) {
        // clear the message object
        clearNodeMessageAndNotify();

        // loops that override the resetAndConfigureLoopBody (returning true)
        // will not call reset between successive executions
        // => force a clear of the model's content here
        m_model.setHasContent(false);

        // check if the node is part of a skipped branch and return
        // appropriate objects without actually configuring the node.
        // We also need to make sure that we don't run InactiveBranchConsumers
        // if they are in the middle of an inactive scope or loop so this
        // check is not trivial...
        boolean isInactive = false;
        // are we not a consumer and any of the incoming branches are inactive?
        isInactive = isInactive || (!isInactiveBranchConsumer() && containsInactiveObjects(rawData));
        // are we a consumer but in the middle of an inactive scope?
        FlowObjectStack inStack = getFlowObjectStack();
        FlowScopeContext peekfsc = inStack.peek(FlowScopeContext.class);
        if (peekfsc != null) {
            isInactive = isInactive || peekfsc.isInactiveScope();
        }
        if (isInactive) {
            if (m_model instanceof ScopeStartNode) {
                // inactive scope start node must indicate to their scope
                // end node that they were inactive...
                FlowScopeContext fsc = getOutgoingFlowObjectStack().peek(FlowScopeContext.class);
                assert fsc != null;
                fsc.inactiveScope(true);
            }
            if (m_model instanceof ScopeEndNode) {
                // scope end nodes can be inactive if and only if their scope start node is
                // inactive as well (which we should see in the scope context object).
                if (peekfsc == null) {
                    createErrorMessageAndNotify("Missing Scope Start Node in inactive branch.");
                    return false;
                }
                if (!peekfsc.isInactiveScope()) {
                    // we can not handle this case: the End scope node needs
                    // to trigger re-execution which it won't in an inactive
                    // branch
                    createErrorMessageAndNotify("Active Scope End node in inactive branch not allowed.");
                    return false;
                } else {
                    // also the scope start node is inactive, so the entire
                    // loop is inactive.
                    // Pop Scope object
                    // => this is done in configure, not needed here! (MB: Hittisau 2013)
//                    getOutgoingFlowObjectStack().pop(FlowScopeContext.class);
                }
            }
            // just a normal node: skip execution and fill output ports with inactive markers
            PortObject[] outs = new PortObject[getNrOutPorts()];
            Arrays.fill(outs, InactiveBranchPortObject.INSTANCE);
            setOutPortObjects(outs, false);
            assert !m_model.hasContent();
            return true;
        }

        // copy input port objects, ignoring the 0-variable port:
        PortObject[] data = Arrays.copyOfRange(rawData, 1, rawData.length);

        PortObject[] inData = new PortObject[data.length];
        // check for existence of all input tables
        for (int i = 0; i < data.length; i++) {
            if (data[i] == null && !m_inputs[i + 1].getType().isOptional()) {
                m_logger.error("execute failed, input contains null");
                // TODO NEWWFM state event
                // TODO: also notify message/progress listeners
                createErrorMessageAndNotify(
                        "Couldn't get data from predecessor (Port No." + i + ").");
                // notifyStateListeners(new NodeStateChangedEvent.EndExecute());
                return false;
            }
            if (data[i] == null) { // optional input
                inData[i] = null;  // (checked above)
            } else if (data[i] instanceof BufferedDataTable) {
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

        PortObject[] rawOutData; // the new DTs from the model
        try {
            // INVOKE MODEL'S EXECUTE
            // (warnings will now be processed "automatically" - we listen)
            rawOutData = invokeNodeModelExecute(exec, inData, reExecute);
        } catch (Throwable th) {
            boolean isCanceled = th instanceof CanceledExecutionException;
            isCanceled = isCanceled || th instanceof InterruptedException;
            // writing to a buffer is done asynchronously -- if this thread
            // is interrupted while waiting for the IO thread to flush we take
            // it as a graceful exit
            isCanceled = isCanceled || (th instanceof DataContainerException
                    && th.getCause() instanceof InterruptedException);
            if (isCanceled) {
                // clear the flag so that the ThreadPool does not kill the
                // thread
                Thread.interrupted();

                reset();
                createWarningMessageAndNotify("Execution canceled");
                return false;
            } else {
                // check if we are inside a try-catch block (only if it was a real
                // error - not when canceled!)
                FlowObjectStack flowObjectStack = getFlowObjectStack();
                FlowTryCatchContext tcslc = flowObjectStack.peek(FlowTryCatchContext.class);
                if ((tcslc != null) && (!tcslc.isInactiveScope())) {
                    // failure inside an active try-catch:
                    // make node inactive but preserve error message.
                    reset();
                    PortObject[] outs = new PortObject[getNrOutPorts()];
                    Arrays.fill(outs, InactiveBranchPortObject.INSTANCE);
                    setOutPortObjects(outs, false);
                    createErrorMessageAndNotify("Execution failed in Try-Catch block: " + th.getMessage());
                    // and push information onto stack so catch-node can report it:
                    FlowObjectStack fos = getNodeModel().getOutgoingFlowObjectStack();
                    fos.push(new FlowVariable(FlowTryCatchContext.ERROR_FLAG, 1));
                    fos.push(new FlowVariable(FlowTryCatchContext.ERROR_NODE, getName()));
                    fos.push(new FlowVariable(FlowTryCatchContext.ERROR_REASON, th.getMessage()));
                    StringWriter thstack = new StringWriter();
                    th.printStackTrace(new PrintWriter(thstack));
                    fos.push(new FlowVariable(FlowTryCatchContext.ERROR_STACKTRACE, thstack.toString()));
                    return true;
                }
            }
            String message = "Execute failed: ";
            if (th.getMessage() != null && th.getMessage().length() >= 5) {
                message = message.concat(th.getMessage());
            } else {
                message = message.concat("(\"" + th.getClass().getSimpleName()
                        + "\"): " + th.getMessage());
            }
            reset();
            createErrorMessageAndNotify(message, th);
            return false;
        }
        // add variable port at index 0
        PortObject[] newOutData = new PortObject[rawOutData.length + 1];
        System.arraycopy(rawOutData, 0, newOutData, 1, rawOutData.length);
        newOutData[0] = FlowVariablePortObject.INSTANCE;

        // check if we see a loop status in the NodeModel
        FlowLoopContext slc = m_model.getLoopContext();
        boolean continuesLoop = (slc != null);
        if (!setOutPortObjects(newOutData, continuesLoop)) {
            return false;
        }
        for (int p = 1; p < getNrOutPorts(); p++) {
            m_outputs[p].hiliteHdl = m_model.getOutHiLiteHandler(p - 1);
        }

        // there might be previous internal tables in a loop
        // (tables are kept between loop iterations)
        // they can be discarded if no longer needed (i.e. they are not part of
        // the internal tables after this execution)
        // (for instance used in group by loop start node)
        BufferedDataTable[] previousInternalHeldTables = m_internalHeldTables;
        if (previousInternalHeldTables != null
                && !this.isModelCompatibleTo(LoopStartNode.class)) {
            m_logger.coding("Found internal tables for non loop "
                    + "start node: " + getName());
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
        // see comment at variable declaration on what is done here
        if (previousInternalHeldTables != null) {
            Set<BufferedDataTable> disposableTables =
                collectTableAndReferences(previousInternalHeldTables);
            disposableTables.removeAll(
                    collectTableAndReferences(m_internalHeldTables));
            disposableTables.removeAll(collectTableAndReferences(newOutData));
            for (BufferedDataTable t : disposableTables) {
                t.clearSingle(this);
            }
        }
        return true;
    } // execute

    /**
     * @param exec The execution context.
     * @param inData The input data to the node (excluding flow var port)
     * @return The output of node
     * @throws Exception An exception thrown by the client.
     * @see invokeNodeModelExecute
     * @since 2.6
     */
    @Deprecated
    public PortObject[] invokeNodeModelExecute(final ExecutionContext exec,
              final PortObject[] inData) throws Exception {
        return invokeNodeModelExecute(exec, inData, false);
    }

    /** Invokes protected method {@link NodeModel#executeModel(
     * PortObject[], ExecutionContext)}. Isolated in a separate method call
     * as it may be (ab)used by other executors.
     * @param exec The execution context.
     * @param inData The input data to the node (excluding flow var port)
     * @return The output of node
     * @throws Exception An exception thrown by the client.
     * @since 2.8
     */
    public PortObject[] invokeNodeModelExecute(final ExecutionContext exec,
            final PortObject[] inData, final boolean reExecute) throws Exception {
        return m_model.executeModel(inData, reExecute, exec);
    }

    /** Called after execute in order to put the computed result into the
     * outports. It will do a sequence of sanity checks whether the argument
     * is valid (non-null, correct type, etc.)
     * @param newOutData The computed output data
     * @param continuesLoop Whether the loop continue mask is set (permits
     *        null values)
     * @return Whether that is successful (false in case of incompatible port
     *         objects)
     */
    private boolean setOutPortObjects(final PortObject[] newOutData,
            final boolean continuesLoop) {
        if (newOutData == null) {
            throw new NullPointerException("Port object array is null");
        }
        if (newOutData.length != getNrOutPorts()) {
            throw new IndexOutOfBoundsException("Array is expected to be of "
                    + "length " + getNrOutPorts() + ": " + newOutData.length);
        }
        // check for compatible output PortObjects
        for (int i = 0; i < newOutData.length; i++) {
            PortType thisType = m_outputs[i].type;
            if (newOutData[i] == null && !continuesLoop) {
                createErrorMessageAndNotify("Output at port " + i + " is null");
                return false;
            }
            if (newOutData[i] != null) {
                if (newOutData[i] instanceof InactiveBranchPortObject) {
                    // allow PO coming from inactive branch
                    // TODO ensure model was skipped during configure?
                } else if (!thisType.getPortObjectClass().isInstance(newOutData[i])) {
                    createErrorMessageAndNotify("Invalid output port object "
                            + "at port " + i);
                    m_logger.error("  (Wanted: "
                            + thisType.getPortObjectClass().getName()
                            + ", " + "actual: "
                            + newOutData[i].getClass().getName() + ")");
                    return false;
                }
                PortObjectSpec spec;
                try {
                    spec = newOutData[i].getSpec();
                } catch (Throwable t) {
                    createErrorMessageAndNotify("PortObject \""
                            + newOutData[i].getClass().getName()
                            + "\" threw " + t.getClass().getSimpleName()
                            + " on #getSpec() ", t);
                    return false;
                }
                if (spec == null) {
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
                    m_outputs[p].spec = newOutData[p].getSpec();
                    m_outputs[p].summary = newOutData[p].getSummary();
                } else {
                    m_outputs[p].summary = null;
                }
            }
        }
        return true;
    }

    /** Sets new file store handler, disposes old one first.
     * @param fileStoreHandler new handler (possibly null to unset)
     * @noreference This method is not intended to be referenced by clients.
     */
    public void setFileStoreHandler(final IFileStoreHandler fileStoreHandler) {
        m_fileStoreHandler = fileStoreHandler;
    }

    /**
     * @return the file store handler for the current execution. Throws
     * exception when no file store handler is set (i.e. not set because node
     * is reset or executing with 3rd party executor).
     * @since 2.6
     * @noreference This method is not intended to be referenced by clients.  */
    public IFileStoreHandler getFileStoreHandler() {
        return m_fileStoreHandler;
    }

    /** Copies the PortObject so that the copy can be given to the node model
     * implementation (and potentially modified). The copy is carried out by
     * means of the respective serializer (via streams).
     *
     * <p> Note that this method is meant to be used by the framework only.
     * @param portObject The object to be copied.
     * @param exec For progress/cancel
     * @return The (deep) copy.
     * @throws IOException In case of exceptions while accessing the stream or
     * if the argument is an instance of {@link BufferedDataTable}.
     * @throws CanceledExecutionException If canceled.*/
    public static PortObject copyPortObject(final PortObject portObject,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        if (portObject instanceof BufferedDataTable) {
            throw new IOException("Can't copy BufferedDataTable objects");
        }

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

        DeferredFileOutputStream deferredOutputStream = new DeferredFileOutputStream(
                /* 10 MB */10 * 1024 * 1024, "knime-portobject-copy-", ".bin",
                new File(KNIMEConstants.getKNIMETempDir()));
        PortObject.PortObjectSerializer obSer =
            PortUtil.getPortObjectSerializer(portObject.getClass());
        PortObjectZipOutputStream objOut = PortUtil.getPortObjectZipOutputStream(deferredOutputStream);
        objOut.setLevel(0);
        obSer.savePortObject(portObject, objOut, exec);
        objOut.close();
        InputStream inStream;
        if (deferredOutputStream.isInMemory()) {
            inStream = new ByteArrayInputStream(deferredOutputStream.getData());
        } else {
            inStream = new BufferedInputStream(new FileInputStream(deferredOutputStream.getFile()));
        }
        PortObjectZipInputStream objIn = PortUtil.getPortObjectZipInputStream(inStream);
        PortObject result = obSer.loadPortObject(objIn, specCopy, exec);
        objIn.close();
        if (!deferredOutputStream.isInMemory()) {
            deferredOutputStream.getFile().delete();
        }
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
    private void clearNodeMessageAndNotify() {
        notifyMessageListeners(NodeMessage.NONE);
    }

    /**
     * Is called, when a warning message is set in the {@link NodeModel}.
     * Forwards it to registered {@link NodeMessageListener}s.
     *
     * @param warningMessage the new message in the node model.
     */
    @Override
    public void warningChanged(final String warningMessage) {

        // get the warning message if available and create a message object
        // also notify all listeners
        if (warningMessage != null) {
            createWarningMessageAndNotify(warningMessage);
        } else {
            clearNodeMessageAndNotify();
        }
    }

    /** Getter for the currently set node warning message in the corresponding
     * NodeModel.
     * @return The currently set warning message (may be null).
     */
    public String getWarningMessageFromModel() {
        return m_model.getWarningMessage();
    }

    /**
     * Resets this node without re-configuring it.
     */
    public void reset() {
        m_logger.debug("reset");
        clearLoopContext();
        setPauseLoopExecution(false);
        m_model.resetModel();
        clearNodeMessageAndNotify();
        cleanOutPorts(false);
    }

    /** Sets output objects to null, disposes tables that are created by
     * this node.
     * @deprecated Framework code should call {@link #cleanOutPorts(boolean)}
     * @noreference This method is not intended to be referenced by clients.
     */
    @Deprecated
    public void cleanOutPorts() {
        cleanOutPorts(false);
    }


    /** Sets output objects to null.
     * @param isLoopRestart If true, does not clear tables that are part
     * of the internally held tables (loop start nodes implements the
     * {@link BufferedDataTableHolder} interface). This can only be true
     * between two loop iterations.
     * @noreference This method is not intended to be referenced by clients.
     */
    public void cleanOutPorts(final boolean isLoopRestart) {
        if (isLoopRestart) { // just as an assertion
            FlowObjectStack inStack = getFlowObjectStack();
            FlowLoopContext flc = inStack.peek(FlowLoopContext.class);
            if (flc != null && flc.isInactiveScope()) {
                m_logger.coding("Encountered an inactive FlowLoopContext in a loop restart.");
                // continue with historically "correct" solution:
                flc = inStack.peekScopeContext(FlowLoopContext.class, false);
            }
            if (flc == null && !this.isModelCompatibleTo(LoopStartNode.class)) {
                m_logger.coding("Encountered a loop restart action but there is"
                        + " no loop context on the flow object stack (node "
                        + getName() + ")");
            }
        }
        m_logger.debug("clean output ports.");
        Set<BufferedDataTable> disposableTables =
            new LinkedHashSet<BufferedDataTable>();
        for (int i = 0; i < m_outputs.length; i++) {
            PortObject portObject = m_outputs[i].object;
            if (portObject instanceof BufferedDataTable) {
                final BufferedDataTable table = (BufferedDataTable)portObject;
                table.collectTableAndReferencesOwnedBy(this, disposableTables);
            }
            m_outputs[i].spec = null;
            m_outputs[i].object = null;
            m_outputs[i].summary = null;
        }

        if (m_internalHeldTables != null) {
            Set<BufferedDataTable> internalTableSet =
                collectTableAndReferences(m_internalHeldTables);
            // internal tables are also used by loop start implementations to
            // keep temporary tables between two loop iterations (e.g. the
            // the group loop start first sorts the table and then puts parts
            // of the table into the loop -- the sorted table is kept as an
            // internal table reference that must not be cleared).
            if (isLoopRestart) {
                disposableTables.removeAll(internalTableSet);
            } else {
                disposableTables.addAll(internalTableSet);
                m_internalHeldTables = null;
            }
        }
        for (BufferedDataTable disposable : disposableTables) {
            disposable.clearSingle(this);
        }
        // clear temporary tables that have been created during execute
        for (ContainerTable t : m_localTempTables) {
            t.clear();
        }
        m_localTempTables.clear();
    }

    private Set<BufferedDataTable> collectTableAndReferences(
            final PortObject[] objects) {
        if (objects == null || objects.length == 0) {
            return Collections.emptySet();
        }
        Set<BufferedDataTable> result = new LinkedHashSet<BufferedDataTable>();
        for (PortObject t : objects) {
            if (t instanceof BufferedDataTable) {
                BufferedDataTable table = (BufferedDataTable)t;
                table.collectTableAndReferencesOwnedBy(this, result);
            }
        }
        return result;
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

    /** Reverse operation to
     * {@link #putOutputTablesIntoGlobalRepository(HashMap)}. It will remove
     * all output tables and its delegates from the global table repository.
     * @param rep The global table rep.
     * @return The number of tables effectively removed, used for assertions.
     */
    public int removeOutputTablesFromGlobalRepository(
            final HashMap<Integer, ContainerTable> rep) {
        int result = 0;
        for (int i = 0; i < m_outputs.length; i++) {
            PortObject portObject = m_outputs[i].object;
            if (portObject instanceof BufferedDataTable) {
                BufferedDataTable t = (BufferedDataTable)portObject;
                result += t.removeFromTableRepository(rep, this);
            }
        }
        return result;
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

    /** Force any tables written by the associated execution context to be
     * written synchronously. This will be the default for loop end nodes
     * (they always write synchronously) but can also be forced by calling this
     * method. Calling this method with value = false doesn't mean tables
     * get written asynchronously.
     *
     * @param value The value to set.
     * @since 2.6
     */
    public void setForceSynchronousIO(final boolean value) {
        m_forceSychronousIO = value || this.isModelCompatibleTo(LoopEndNode.class);
    }

    /** Getter for {@link #setForceSynchronousIO(boolean)}.
     * @return the forceSychronousIO
     * @since 2.6 */
    public boolean isForceSychronousIO() {
        return m_forceSychronousIO;
    }

    /**
     * Deletes any temporary resources associated with this node.
     */
    public void cleanup() {
        m_model.unregisterAllViews();
        try {
            m_model.onDispose();
        } catch (Throwable t) {
            m_logger.error(t.getClass().getSimpleName() + " during cleanup of node: " + t.getMessage(), t);
        }
        cleanOutPorts(false);
    }

    /** Used before configure, to apply the variable mask to the nodesettings,
     * that is to change individual node settings to reflect the current values
     * of the variables (if any).
     * @return a map containing the exposed variables (which are visible to
     * downstream nodes. These variables are put onto the node's
     * {@link FlowObjectStack}.
     */
    private Map<String, FlowVariable> applySettingsUsingFlowObjectStack()
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
            String message = "Failed to apply flow variables; "
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
        Map<String, FlowVariable> flowVariablesMap =
            getFlowObjectStack().getAvailableFlowVariables();
        List<FlowVariable> newVariableList;
        try {
            newVariableList = configEditor.overwriteSettings(
                    fromModel, flowVariablesMap);
        } catch (InvalidSettingsException e) {
            throw new InvalidSettingsException(
                    "Errors overwriting node settings with flow variables: "
                    + e.getMessage(), e);
        }
        try {
            m_model.validateSettings(fromModel);
        } catch (final Throwable e) {
            String message = "Validation of node settings failed";
            if (!(e instanceof InvalidSettingsException)) {
                message = message + " with " + e.getClass().getSimpleName() + ": " + e.getMessage();
                m_logger.error(message, e);
            } else {
                message = message + ": " + e.getMessage();
            }
            throw new InvalidSettingsException(message, e);
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
        Map<String, FlowVariable> newVariableHash =
            new LinkedHashMap<String, FlowVariable>();
        for (FlowVariable v : newVariableList) {
            if (newVariableHash.put(v.getName(), v) != null) {
                m_logger.warn("Duplicate variable assignment for key \""
                        + v.getName() + "\")");
            }
        }
        return newVariableHash;
    }

    private void addOutgoingFlowVariables(
            final Map<String, FlowVariable> newVars) {
        FlowObjectStack outgoingFlowObjectStack = getOutgoingFlowObjectStack();
        ArrayList<FlowVariable> reverseOrder =
            new ArrayList<FlowVariable>(newVars.values());
        Collections.reverse(reverseOrder);
        for (FlowVariable v : reverseOrder) {
            outgoingFlowObjectStack.push(v);
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
        return configure(inSpecs, null);
    }

    /**
     * Allows passing an object that may modify the specs created by the
     * {@link NodeModel}, for example in case the node is wrapped and the
     * output is modified.
     *
     * @param rawInSpecs table specs from the predecessors
     * @param postConfigure object called after node model calculated output
     *            specs
     * @return true if configure finished successfully.
     */
    public boolean configure(final PortObjectSpec[] rawInSpecs,
            final NodePostConfigure postConfigure) {
        boolean success = false;
        synchronized (m_configureLock) {
            // reset message object
            clearNodeMessageAndNotify();
            // copy input port object specs, ignoring the 0-variable port:
            PortObjectSpec[] inSpecs =
                Arrays.copyOfRange(rawInSpecs, 1, rawInSpecs.length);

            // clean output spec
            for (int p = 0; p < m_outputs.length; p++) {
                // update data table spec
                m_outputs[p].spec = null;
            }

            PortObjectSpec[] newOutSpec =
                new PortObjectSpec[getNrOutPorts() - 1];
            Map<String, FlowVariable> newVariables = Collections.emptyMap();
            try {
                // check the inspecs against null
                for (int i = 0; i < inSpecs.length; i++) {
                    if (inSpecs[i] == null) {
                        if (m_inputs[i + 1].getType().isOptional()) {
                            // ignore, unconnected optional input
                        } else {
                            return false;
                        }
                        // TODO: did we really need a warning here??
                        // throw new InvalidSettingsException(
                        // "Node is not executable until all predecessors "
                        // + "are configured and/or executed.");
                    }
                }

                // check if the node is part of a skipped branch and return
                // appropriate specs without actually configuring the node.
                // Note that we must also check the incoming variable port!
                boolean isInactive = false;
                if (!isInactiveBranchConsumer()) {
                    for (int i = 0; i < rawInSpecs.length; i++) {
                        if (rawInSpecs[i] instanceof InactiveBranchPortObjectSpec) {
                            isInactive = true;
                            break;
                        }
                    }
                } else {
                    FlowLoopContext flc = getFlowObjectStack().peek(FlowLoopContext.class);
                    if (flc != null && flc.isInactiveScope()) {
                        isInactive = true;
                    }
                }
                if (isInactive) {
                    for (int j = 0; j < m_outputs.length; j++) {
                        m_outputs[j].spec =
                            InactiveBranchPortObjectSpec.INSTANCE;
                    }
                    if (success) {
                        m_logger.debug("Configure skipped. (" + getName() + " in inactive branch.)");
                    }
                    return true;
                }
                if (m_variablesSettings != null) {
                    newVariables = applySettingsUsingFlowObjectStack();
                }

                // call configure model to create output table specs
                // guaranteed to return non-null, correct-length array
                newOutSpec = invokeNodeModelConfigure(inSpecs);
                if (postConfigure != null) {
                    newOutSpec = postConfigure.configure(inSpecs, newOutSpec);
                }
                addOutgoingFlowVariables(newVariables);
                for (int p = 0; p < newOutSpec.length; p++) {
                    // update data table spec
                    m_outputs[p + 1].spec = newOutSpec[p];
                }
                m_outputs[0].spec = FlowVariablePortObjectSpec.INSTANCE;
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
            }
        }
        if (success) {
            m_logger.debug("Configure succeeded. (" + this.getName() + ")");
        }
        return success;
    }

    /** Invokes protected method NodeModel#configureModel. Isolated in a
     * separate method call as it may be (ab)used by other executors.
     * @param inSpecs The input data to the node (excluding flow var port)
     * @return The output of node
     * @throws InvalidSettingsException An exception thrown by the client.
     * @since 2.6 */
    public PortObjectSpec[] invokeNodeModelConfigure(
            final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        PortObjectSpec[] newOutSpec;
        newOutSpec = m_model.configureModel(inSpecs);
        return newOutSpec;
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
    public AbstractNodeView<?> getView(final int viewIndex, final String title) {
        try {
            return m_factory.createAbstractNodeView(viewIndex, m_model);
        } catch (Throwable e) {
            String errorMsg = "View instantiation failed: " + e.getMessage();
            m_logger.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * Returns true if this node can show an interactive view.
     * @return <code>true</code> if interactive view is available.
     * @since 2.8
     */
    public boolean hasInteractiveView() {
        if (!(m_model instanceof InteractiveNode)) {
            return false;
        }
        return m_factory.hasInteractiveView();
    }

    /**
     * Returns the name for the interactive node's view.
     *
     * @return The view's name.
     * @since 2.8
     */
    public String getInteractiveViewName() {
        return m_factory.getInteractiveViewName();
    }

    /**
     * Return a new instance of the interactive node's view (without opening it).
     *
     * @param title the displayed view title.
     * @return The node view with the specified index.
     * @since 2.8
     */
    public AbstractInteractiveNodeView<?> getInteractiveView(final String title) {
        try {
            return m_factory.createInteractiveView(m_model);
        } catch (Throwable e) {
            String errorMsg = "Interactive View instantiation failed: " + e.getMessage();
            m_logger.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
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
        // if there is only one, it will be the thread pool executor.
        if (NodeExecutionJobManagerPool.getNumberOfJobManagersFactories() > 1) {
            // framework creates a dialog for job manager selection
            return true;
        }
        // The default job manager (ThreadPool) has no settings. No need to
        // create a dialog if it is the only job manager.
        return false;
    }

    /**
     * @param inSpecs The input specs, which will be forwarded to the dialog's
     *        {@link NodeDialogPane#loadSettingsFrom(NodeSettingsRO,
     *        PortObjectSpec[])}.
     * @param settings The current settings of this node. The settings object
     *        will also contain the settings of the outer SNC.
     * @param isWriteProtected Whether write protected, see
     *        {@link WorkflowManager#isWriteProtected()}.
     * @return The dialog pane which holds all the settings' components. In
     *         addition this method loads the settings from the model into the
     *         dialog pane.
     * @throws NotConfigurableException if the dialog cannot be opened because
     *             of real invalid settings or if any preconditions are not
     *             fulfilled, e.g. no predecessor node, no nominal column in
     *             input table, etc.
     * @throws IllegalStateException If node has no dialog.
     * @see #hasDialog()
     * @since 2.6
     */
    public NodeDialogPane getDialogPaneWithSettings(
            final PortObjectSpec[] inSpecs, final PortObject[] inData,
            final NodeSettingsRO settings, final boolean isWriteProtected)
        throws NotConfigurableException {
        NodeDialogPane dialogPane = getDialogPane();
        PortObjectSpec[] corrInSpecs = new PortObjectSpec[inSpecs.length - 1];
        PortObject[] corrInData = new PortObject[inData.length - 1];
        for (int i = 1; i < inSpecs.length; i++) {
            if (inSpecs[i] instanceof InactiveBranchPortObjectSpec) {
                if (!isInactiveBranchConsumer()) {
                    throw new NotConfigurableException("Can not configure"
                            + " nodes in inactive branches.");
                }
            }
            PortType t = getInputType(i);
            if (!t.acceptsPortObjectSpec(inSpecs[i])
                    && !(inSpecs[i] instanceof InactiveBranchPortObjectSpec)) {
                // wrong type and not a consumer of inactive branches either
                // (which is the only exception for a type mismatch)
                throw new IllegalArgumentException(
                        "Invalid incoming port object spec \""
                                + inSpecs[i].getClass().getSimpleName()
                                + "\", expected \""
                                + t.getPortObjectSpecClass().getSimpleName()
                                + "\"");
            } else if (inSpecs[i] == null && BufferedDataTable.TYPE.equals(t)
                    && !t.isOptional()) {
                corrInSpecs[i - 1] = new DataTableSpec();
            } else {
                corrInSpecs[i - 1] = inSpecs[i];
                corrInData[i - 1] = inData[i];
            }
        }
        dialogPane.internalLoadSettingsFrom(settings, corrInSpecs, corrInData,
                getFlowObjectStack(), getCredentialsProvider(),
                isWriteProtected);
        return dialogPane;
    }

    /**
     * Get reference to the node dialog instance. Used to get the user settings
     * from the dialog without overwriting them as in
     * {@link #getDialogPaneWithSettings(PortObjectSpec[],
     * PortObject[], NodeSettingsRO, boolean)}.
     *
     * @return Reference to dialog pane.
     * @throws IllegalStateException If node has no dialog.
     * @see #hasDialog()
     */
    public NodeDialogPane getDialogPane() {
        if (m_dialogPane == null) {
            if (hasDialog()) {
                if (m_factory.hasDialog()) {
                    final AtomicReference<Throwable> exRef =
                        new AtomicReference<Throwable>();
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                m_dialogPane = m_factory.createNodeDialogPane();
                            } catch (Throwable ex) {
                                exRef.set(ex);
                            }
                        }
                    };

                    ViewUtils.invokeAndWaitInEDT(r);
                    if (exRef.get() instanceof Error) {
                        throw (Error)exRef.get();
                    } else if (exRef.get() instanceof RuntimeException) {
                        NodeLogger.getLogger(Node.class).error("Error while creating node dialog for '"
                                                                       + m_factory.getNodeName() + "': "
                                                                       + exRef.get().getMessage(), exRef.get());
                        throw (RuntimeException) exRef.get();
                    } else {
                    	// not possible since createNodeDialogPane does not throw Exceptions
                    }
                } else {
                    m_dialogPane = new EmptyNodeDialogPane();
                }
                if (getNrOutPorts() > 0) {
                    m_dialogPane.addMiscTab();
                }
                if (NodeExecutionJobManagerPool
                        .getNumberOfJobManagersFactories() > 1) {
                    // TODO: set the splittype depending on the nodemodel
                    SplitType splitType = SplitType.USER;
                    m_dialogPane.addJobMgrTab(splitType);
                }

            } else {
                throw new IllegalStateException(
                        "Can't return dialog pane, node has no dialog!");
            }
        }
        return m_dialogPane;
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

    /** Call {@link NodeModel#saveInternals(File, ExecutionMonitor)} and handles errors by logging to the NodeLogger
     * or setting a warning message at the node.
     * @param internDir ...
     * @param exec ...
     * @throws CanceledExecutionException ...
     * @noreference This method is not intended to be referenced by clients.
     */
    // Called by 3rd party executor
    public void saveInternals(final File internDir, final ExecutionMonitor exec)
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

    /** Call {@link NodeModel#loadInternals(File, ExecutionMonitor)} and handles errors by logging to the NodeLogger
     * or setting a warning message at the node.
     * @param internDir ...
     * @param exec ...
     * @throws CanceledExecutionException ...
     * @noreference This method is not intended to be referenced by clients.
     */
    // Called by 3rd party executor
    public void loadInternals(final File internDir, final ExecutionMonitor exec)
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
    private void notifyMessageListeners(final NodeMessage message) {
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
     * Returns a string summary of this node.
     *
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Node @" + hashCode() + " [" + m_name + ";in="
                + m_inputs.length + ";out=" + m_outputs.length + "]";
    }

    /**
     * NOTE: it is not recommended to call this method anywhere else than in the
     * core. The port indices in the factory don't encounter for the implicit
     * flow variable ports!
     *
     * @return the <code>NodeFactory</code> that constructed this node.
     * @noreference This method is not intended to be referenced by clients.
     */
    public NodeFactory<NodeModel> getFactory() {
        return m_factory;
    }

    /** @return non-null meta bundle and node information to this instance.
     * @since 2.6
     * @noreference This method is not intended to be referenced by clients. */
    public NodeAndBundleInformation getNodeAndBundleInformation() {
        if (m_model instanceof MissingNodeModel) {
            return ((MissingNodeModel)m_model).getNodeAndBundleInformation();
        }
        return new NodeAndBundleInformation(this);
    }

    /** @return the underlying node model. */
    public NodeModel getNodeModel() {
        return m_model;
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

    /** Widens scope of {@link AbstractNodeView#openView(String)} method so it
     * can be called from UI framework components. This method is not meant for
     * public use and may change in future versions.
     * @param view The view to call the method on.
     * @param title The title for the view (method argument).
     */
    public static void invokeOpenView(final AbstractNodeView<?> view,
            final String title) {
        view.openView(title);
    }

    /** Widens scope of internalLoadSettingsFrom method in
     * {@link NodeDialogPane}. Framework method, not to be called by node
     * implementations.
     * @param pane forwarded
     * @param settings forwarded
     * @param specs forwarded
     * @param data forwarded
     * @param foStack forwarded
     * @param credentialsProvider forwarded
     * @param isWriteProtected forwarded
     * @throws NotConfigurableException forwarded
     * @since 2.6
     */
    public static void invokeDialogInternalLoad(final NodeDialogPane pane,
            final NodeSettingsRO settings, final PortObjectSpec[] specs,
            final PortObject[] data, final FlowObjectStack foStack,
            final CredentialsProvider credentialsProvider,
            final boolean isWriteProtected) throws NotConfigurableException {
        pane.internalLoadSettingsFrom(settings, specs, data, foStack,
                credentialsProvider, isWriteProtected);
    }

    /** Widens scope of {@link AbstractNodeView#closeView()} method so it
     * can be called from UI framework components. This method is not meant for
     * public use and may change in future versions.
     * @param view The view to call the method on.
     */
    public static void invokeCloseView(final AbstractNodeView<?> view) {
        view.closeView();
    }

    // ////////////////////////
    // FlowObjectStack handling
    // ////////////////////////

    public void setFlowObjectStack(final FlowObjectStack scsc,
            final FlowObjectStack outgoingFlowObjectStack) {
        m_model.setFlowObjectStack(scsc, outgoingFlowObjectStack);
    }

    public FlowObjectStack getFlowObjectStack() {
        return m_model.getFlowObjectStack();
    }

    /** Get list of flow variables that are added by NodeModel implementation.
     * @return The stack of flow variables that the node added in its client
     *         code (configure & execute).
     */
    public FlowObjectStack getOutgoingFlowObjectStack() {
        return m_model.getOutgoingFlowObjectStack();
    }

    /**
     * @param nodeModelClass class or interface to check for.
     * @return return true if underlying NodeModel implements the given class/interface
     * @since 2.8
     */
    public boolean isModelCompatibleTo(final Class<?> nodeModelClass) {
        return nodeModelClass.isAssignableFrom(this.getNodeModel().getClass());
    }

    /** Possible roles of loop roles. */
    @Deprecated
    public static enum LoopRole { BEGIN, END, NONE }

    /**
     * @return role of loop node.
     */
    @Deprecated
    public final LoopRole getLoopRole() {
        if (isModelCompatibleTo(LoopStartNode.class)) {
            return LoopRole.BEGIN;
        } else if (isModelCompatibleTo(LoopEndNode.class)) {
            return LoopRole.END;
        } else {
            return LoopRole.NONE;
        }
    }

    /** Clear loop context member of NodeModel.
     */
    public void clearLoopContext() {
        m_model.clearLoopContext();
    }

    /**
     * @return loop context members of NodeModel.
     */
    public FlowLoopContext getLoopContext() {
        return m_model.getLoopContext();
    }

    /**
     * @return initial FlowLoopContext object to be put on stack.
     * @since 2.8
     */
    public FlowScopeContext getInitialScopeContext() {
        return m_model.getInitialScopeContext();
    }

    /**
     * @see NodeModel#getPauseLoopExecution
     *
     * @return true if loop execution was paused.
     */
    public boolean getPauseLoopExecution() {
        return m_model.getPauseLoopExecution();
    }

    /**
     * @see NodeModel#setPauseLoopExecution
     *
     * @param ple new state.
     */
    public void setPauseLoopExecution(final boolean ple) {
        m_model.setPauseLoopExecution(ple);
    }

    /** Make model aware of corresponding LoopEndNode.
     *
     * @param tail the node.
     */
    public void setLoopEndNode(final Node tail) {
        if (tail == null) {
            m_model.setLoopEndNode(null);
        } else {
            if (!(tail.m_model instanceof LoopEndNode)) {
                throw new ClassCastException("Node.setLoopEndNode called with"
                        + "wrong argument. Not a LoopEndNode!");
            }
            m_model.setLoopEndNode((LoopEndNode)tail.m_model);
        }
    }

    /** Make model aware of corresponding LoopStartNode.
     *
     * @param head the node.
     */
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

    /**
     * @see NodeModel#getLoopStartNode()
     * @return corresponding loop start node.
     * @since 2.6
     */
    public LoopStartNode getLoopStartNode() {
    	return m_model.getLoopStartNode();
    }

    /**
     * @return corresponding loop end node.
     * @since 2.6
     */
    public LoopEndNode getLoopEndNode() {
    	return m_model.getLoopEndNode();
    }

    /**
     * @see NodeModel#resetAndConfigureLoopBody()
     * @return true (default) if loop body nodes have to be reset/configure during each iteration.
     */
    public boolean resetAndConfigureLoopBody() {
        return getNodeModel().resetAndConfigureLoopBody();
    }


    // ////////////////////////
    // Credentials handling
    // ////////////////////////

    /** Sets credentials in model.
     * @param provider provider to set.
     */
    public void setCredentialsProvider(final CredentialsProvider provider) {
        m_model.setCredentialsProvider(provider);
    }

    /** @return The credentials as set in the model. */
    public CredentialsProvider getCredentialsProvider() {
        return m_model.getCredentialsProvider();
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
