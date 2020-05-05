/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IDataRepository;
import org.knime.core.data.filestore.FileStorePortObject;
import org.knime.core.data.filestore.FileStoreUtil;
import org.knime.core.node.AbstractNodeView.ViewableModel;
import org.knime.core.node.interactive.InteractiveNode;
import org.knime.core.node.interactive.InteractiveView;
import org.knime.core.node.interactive.ViewContent;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.port.inactive.InactiveBranchConsumer;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.MergeOperator;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortObjectOutput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.streamable.StreamableOperatorInternals;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.CaptureWorkflowStartNode;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.ExecutionEnvironment;
import org.knime.core.node.workflow.FlowCaptureContext;
import org.knime.core.node.workflow.FlowLoopContext;
import org.knime.core.node.workflow.FlowObjectStack;
import org.knime.core.node.workflow.FlowScopeContext;
import org.knime.core.node.workflow.FlowTryCatchContext;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.ICredentials;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.LoopStartNode;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.ScopeStartNode;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.node.workflow.VariableType.DoubleType;
import org.knime.core.node.workflow.VariableType.IntType;
import org.knime.core.node.workflow.VariableType.StringType;


/**
 * Abstract class defining a node's configuration and execution (among others).
 * More specifically, it defines:
 * <ul>
 * <li>Input and output ports (count and types)</li>
 * <li>Settings handling (validation and storage)</li>
 * <li>Configuration (e.g. after new settings are applied or a node
 * is (re)connected</li>
 * <li>Execution</li>
 * <li>Reset</li>
 * <li>Storage of &quot;internals&quot; (e.g. hilite translation and/or
 * information that is shown in node view)
 * </ul>
 * Derived classes must overwrite one of two execute methods and
 * one of two configure methods (depending on their port types):
 * <ol>
 * <li>The {@link #execute(PortObject[], ExecutionContext)} and
 * {@link #configure(PortObjectSpec[])} methods for general
 * port definitions (rarely used) or
 * <li>the {@link #execute(BufferedDataTable[], ExecutionContext)} and
 * {@link #configure(DataTableSpec[])} methods for standard data ports
 * (on both in- and outports).
 * </ol>
 * None of these methods is declared abstract, though one pair of
 * execute/configure must be overridden (if none is overwritten a runtime
 * exception will be thrown upon the node's configuration or execution, resp.).
 *
 * <p>
 * For a detailed description of this class refer to KNIME's extension guide
 * and the various node implementations.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class NodeModel implements ViewableModel {


    /* This code will go into the class header once we introduce
     * NodeConfiguration in replacement for NodeSettings */

//    /* @param <NC> The type of {@link NodeConfiguration} associated with this
//    *         implementation. Most simply models will just use
//    *         <code>NodeConfiguration</code> may wish to use a derived class.
//    */

//    /** The node configuration associated with this model. This field will
//     * be assigned immediately after construction.
//     */
//    private NC m_nodeConfiguration;
//    /** @param nodeConfiguration the nodeConfiguration to set */
//    void setNodeConfiguration(final NC nodeConfiguration) {
//        try {
//            onNewNodeConfiguration(nodeConfiguration);
//        } catch (final Throwable e) {
//            m_logger.coding("Throwable while notifying node about "
//                    + "new configuration", e);
//        }
//        m_nodeConfiguration = nodeConfiguration;
//    }
//
//    /** Subclass hook to react on new configuration objects. This method is
//     * called before the new configuration object is set in the abstract
//     * NodeModel class.
//     * @param nodeConfiguration The node configuration object that is going
//     * to be set into the model after this method returns (whether normally
//     * or abnormally.) The argument is never <code>null</code>.
//     */
//    protected void onNewNodeConfiguration(final NC nodeConfiguration) {
//        // subclass hook.
//    }
//
//    /**
//     * Get the node configuration set on this model. Subclasses may simply use
//     * this configuration and assume it is properly setup (i.e. there was at
//     * least an attempt to auto-guess defaults for newly created nodes, the
//     * configuration was loaded as part of the workflow loading for stored
//     * workflows or the configuration has been filled in the node's dialog.)
//     *
//     * <p>
//     * Subclasses should verify consistency with the input data in their
//     * {@link #configure(DataTableSpec[]) configure} method, possibly throwing
//     * and {@link InvalidSettingsException}.
//     *
//     * <p>
//     * Derived classes should always use this method to get the current node
//     * configuration rather than keeping the returned value as class field (as
//     * the underlying configuration object changes when updated through the
//     * dialog or other controls).
//     *
//     * <p>
//     * The returned object should be considered read-only, i.e. derived classes
//     * should always only read from the configuration but not modify it.
//     *
//     * @return The currently active node configuration. This method does not
//     *         return null unless it is (indirectly) called from the NodeModel
//     *         constructor.
//     */
//    protected NC getNodeConfiguration() {
//        return m_nodeConfiguration;
//    }

    /**
     * The node logger for this class; do not make static to make sure the right
     * class name is printed in messages.
     */
    private final NodeLogger m_logger;

    /** Hold in and output port types. */
    private final PortType[] m_inPortTypes;
    private final PortType[] m_outPortTypes;

    /** Holds the input hilite handler for each input. */
    private final HiLiteHandler[] m_inHiLiteHdls;

    /** Hilite adapter returned in
     * {@link NodeModel#getInHiLiteHandler(int)} when the current in-port
     * hilite handler is <code>null</code>, e.g. the node is not fully
     * connected.
     */
    private static final HiLiteHandler HILITE_ADAPTER = new HiLiteHandler();

    /** Keeps a list of registered views. */
    private final CopyOnWriteArrayList<AbstractNodeView<?>> m_views;

    /** Flag for the hasContent state. */
    private boolean m_hasContent;

    /**
     * Optional warning message to be set during / after execution. Enables
     * higher levels to display the given message.
     */
    private String m_warningMessage = null;

    /** The listeners that are interested in changes of the model warning. */
    private final CopyOnWriteArraySet<NodeModelWarningListener>
                                                         m_warningListeners;

    /**
     * Creates a new model with the given number of input and
     * output data ports.
     * @param nrInDataPorts number of input data ports
     * @param nrOutDataPorts number of output data ports
     * @throws NegativeArraySizeException If the number of in- or outputs is
     *             smaller than zero.
     */
    protected NodeModel(final int nrInDataPorts,
            final int nrOutDataPorts) {
        this(createPOs(nrInDataPorts), createPOs(nrOutDataPorts));
    }

    private static PortType[] createPOs(final int nrDataPorts) {
        PortType[] portTypes = new PortType[nrDataPorts];
        Arrays.fill(portTypes, BufferedDataTable.TYPE);
        return portTypes;
    }

    /**
     * Creates a new model with the given number (and types!) of input and
     * output types.
     * @param inPortTypes an array of non-null in-port types
     * @param outPortTypes an array of non-null out-port types
     */
    protected NodeModel(final PortType[] inPortTypes,
            final PortType[] outPortTypes) {
        // create logger
        m_logger = NodeLogger.getLogger(this.getClass());

        // init message listener array
        m_warningListeners =
                       new CopyOnWriteArraySet<NodeModelWarningListener>();

        // check port types of validity and store them
        if (inPortTypes == null) {
            m_inPortTypes = new PortType[0];
        } else {
            m_inPortTypes = new PortType[inPortTypes.length];
            for (int i = 0; i < inPortTypes.length; i++) {
                PortType portType = inPortTypes[i];
                CheckUtils.checkArgumentNotNull(portType, "InPortType[%d] must not be null!", i);
                m_inPortTypes[i] =
                    PortTypeRegistry.getInstance().getPortType(portType.getPortObjectClass(), portType.isOptional());
            }
        }
        if (outPortTypes == null) {
            m_outPortTypes = new PortType[0];
        } else {
            m_outPortTypes = new PortType[outPortTypes.length];
            for (int i = 0; i < outPortTypes.length; i++) {
                PortType portType = outPortTypes[i];
                CheckUtils.checkArgumentNotNull(portType, "OutPortType[%d] must not be null!", i);
                m_outPortTypes[i] =
                    PortTypeRegistry.getInstance().getPortType(portType.getPortObjectClass(), portType.isOptional());
            }
        }

        m_hasContent = false;

        // set initial array of HiLiteHandlers
        if (getNrInPorts() == 0) {
            // initialize a new one if no input exists
            m_inHiLiteHdls = new HiLiteHandler[1];
        } else {
            // otherwise create a spot handlers - one for each input.
            m_inHiLiteHdls = new HiLiteHandler[getNrInPorts()];
        }

        // keeps set of registered views in the order they are added
        m_views = new CopyOnWriteArrayList<AbstractNodeView<?>>();
    }

    /**
     * Load internals into the derived <code>NodeModel</code>. This method is
     * only called if the <code>Node</code> was executed. Read all your
     * internal structures from the given file directory to create your internal
     * data structure which is necessary to provide all node functionalities
     * after the workflow is loaded, e.g. view content and/or hilite mapping.
     * <br>
     *
     * @param nodeInternDir The directory to read from.
     * @param exec Used to report progress and to cancel the load process.
     * @throws IOException If an error occurs during reading from this dir.
     * @throws CanceledExecutionException If the loading has been canceled.
     * @see #saveInternals(File,ExecutionMonitor)
     */
    protected abstract void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException;

    /**
     * Save internals of the derived <code>NodeModel</code>. This method is
     * only called if the <code>Node</code> is executed. Write all your
     * internal structures into the given file directory which are necessary to
     * recreate this model when the workflow is loaded, e.g. view content and/or
     * hilite mapping.<br>
     *
     * @param nodeInternDir The directory to write into.
     * @param exec Used to report progress and to cancel the save process.
     * @throws IOException If an error occurs during writing to this dir.
     * @throws CanceledExecutionException If the saving has been canceled.
     * @see #loadInternals(File,ExecutionMonitor)
     */
    protected abstract void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException;


    /**
     * Registers the given view at the model to receive change events of the
     * underlying model. Note that no change event is fired.
     *
     * @param view The view to register.
     */
    final void registerView(final AbstractNodeView<?> view) {
        assert view != null;
        m_views.add(view);
        m_logger.debug("Registering view at model (total count " + m_views.size() + ")");
    }

    /**
     * @param <V> the concrete interactive view type
     * @return interactive node view or null if it does not (yet) exist.
     * @since 2.8
     */
    @SuppressWarnings("unchecked")
    public final <V extends AbstractNodeView<?> & InteractiveView<?, ? extends ViewContent, ? extends ViewContent>> V getInteractiveNodeView() {
        for (AbstractNodeView<?> abv : m_views) {
            if (abv instanceof InteractiveView) {
                return (V)abv;
            }
        }
        return null;
    }

    /**
     * Unregisters the given view.
     *
     * @param view The view to unregister.
     */
    final void unregisterView(final AbstractNodeView<?> view) {
        assert view != null;
        boolean success = m_views.remove(view);
        if (success) {
            m_logger.debug("Unregistering view from model (" + m_views.size() + " remaining).");
        } else {
            m_logger.debug("Can't remove view from model, not registered.");
        }
    }

    /**
     * Unregisters all views from the model.
     */
    final void unregisterAllViews() {
        m_logger.debug("Removing all (" + m_views.size() + ") views from model.");
        for (AbstractNodeView<?> view : m_views) {
            view.closeView();
        }
        m_views.clear();
    }

    /**
     * Returns the overall number of inputs.
     *
     * @return Number of inputs.
     */
    protected final int getNrInPorts() {
        return m_inPortTypes.length;
    }

    /**
     * Returns the overall number of outputs.
     *
     * @return Number of outputs.
     */
    protected final int getNrOutPorts() {
        return m_outPortTypes.length;
    }

    /** Port type as specified in constructor.
     * @param index Index of inport
     * @return Type of port as specified in constructor.
     * @throws IndexOutOfBoundsException If index is invalid.
     * @since 2.12
     * @throws IndexOutOfBoundsException ...
     * @since 2.12
     */
    protected final PortType getInPortType(final int index) {
        return m_inPortTypes[index];
    }

    /** Port type as specified in constructor.
     * @param index Index of outport
     * @return Type of port as specified in constructor.
     * @throws IndexOutOfBoundsException If index is invalid.
     * @since 2.12
     * @throws IndexOutOfBoundsException ...
     * @since 2.12
     */
    protected final PortType getOutPortType(final int index) {
        return m_outPortTypes[index];
    }

    /**
     * Validates the specified settings in the model and then loads them into
     * it.
     *
     * @param settings the settings to read
     * @throws InvalidSettingsException if the settings are not valid or cannot
     *      be loaded into the model
     */
    final void loadSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // validate the settings before loading them
        validateSettings(settings);
        // load settings into the model
        loadValidatedSettingsFrom(settings);
    }

    /**
     * Adds to the given <code>NodeSettings</code> the model specific
     * settings. The settings don't need to be complete or consistent. If, right
     * after startup, no valid settings are available this method can write
     * either nothing or invalid settings.
     * <p>
     * Method is called by the <code>Node</code> if the current settings need
     * to be saved or transfered to the node's dialog.
     *
     * @param settings The object to write settings into.
     *
     * @see #loadValidatedSettingsFrom(NodeSettingsRO)
     * @see #validateSettings(NodeSettingsRO)
     */
    protected abstract void saveSettingsTo(final NodeSettingsWO settings);

    /**
     * Validates the settings in the passed <code>NodeSettings</code> object.
     * The specified settings should be checked for completeness and
     * consistency. It must be possible to load a settings object validated
     * here without any exception in the
     * <code>#loadValidatedSettings(NodeSettings)</code> method. The method
     * must not change the current settings in the model - it is supposed to
     * just check them. If some settings are missing, invalid, inconsistent, or
     * just not right throw an exception with a message useful to the user.
     *
     * @param settings The settings to validate.
     * @throws InvalidSettingsException If the validation of the settings
     *             failed.
     * @see #saveSettingsTo(NodeSettingsWO)
     * @see #loadValidatedSettingsFrom(NodeSettingsRO)
     */
    protected abstract void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException;

    /**
     * Sets new settings from the passed object in the model. You can safely
     * assume that the object passed has been successfully validated by the
     * <code>#validateSettings(NodeSettings)</code> method. The model must set
     * its internal configuration according to the settings object passed.
     *
     * @param settings The settings to read.
     *
     * @throws InvalidSettingsException If a property is not available.
     *
     * @see #saveSettingsTo(NodeSettingsWO)
     * @see #validateSettings(NodeSettingsRO)
     */
    protected abstract void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Invokes the abstract <code>#execute()</code> method of this model. In
     * addition, this method notifies all assigned views of the model about the
     * changes.
     *
     * @param rawData An array of <code>PortObject</code> objects holding the data
     *            from the inputs (includes flow variable port).
     * @param exEnv The execution environment used for execution of this model.
     * @param exec The execution monitor which is passed to the execute method
     *            of this model.
     * @return The result of the execution in form of an array with
     *         <code>DataTable</code> elements, as many as the node has
     *         outputs.
     * @throws Exception any exception or error that is fired in the derived
     *             model will be just forwarded. It may throw an
     *             CanceledExecutionException if the user pressed cancel during
     *             execution. Even if the derived model doesn't check, the
     *             result will be discarded and the exception thrown.
     * @throws IllegalStateException If the number of <code>PortObject</code>
     *             objects returned by the derived <code>NodeModel</code>
     *             does not match the number of outputs. Or if any of them is
     *             null.
     * @see #execute(PortObject[],ExecutionContext)
     * @since 2.8
     * @noreference This method is not intended to be referenced by clients
     *              (use Node class instead)
     */
    PortObject[] executeModel(final PortObject[] rawData, final ExecutionEnvironment exEnv,
            final ExecutionContext exec) throws Exception {
        final PortObject[] data = ArrayUtils.remove(rawData, 0);
        assert (data != null && data.length == getNrInPorts());
        assert (exec != null);

        setWarningMessage(null);

        // check for compatible input PortObjects
        for (int i = 0; i < data.length; i++) {
            PortType thisType = getInPortType(i);
            if (thisType.isOptional() && data[i] == null) {
                // ignore non-populated optional input
            } else if (data[i] instanceof InactiveBranchPortObject) {
                assert this instanceof InactiveBranchConsumer;
                // allow Inactive POs at InactiveBranchConsumer
            } else if (!(thisType.getPortObjectClass().isInstance(data[i]))) {
                m_logger.error("  (Wanted: "
                        + thisType.getPortObjectClass().getName() + ", "
                        + "actual: " + data[i].getClass().getName() + ")");
                throw new IllegalStateException("Connection Error: Mismatch"
                        + " of input port types (port " + (i) + ").");
            }
        }

        // temporary storage for result of derived model.
        // EXECUTE DERIVED MODEL
        PortObject[] outData;
        try {
            if (!exEnv.reExecute()) {
                outData = execute(data, exec);
            } else {
                //FIXME: implement reexecution with loading view content and execute
                if (this instanceof InteractiveNode) {
                    InteractiveNode iThis = (InteractiveNode)this;
                    ViewContent viewContent = exEnv.getPreExecuteViewContent();
                    iThis.loadViewValue(viewContent, exEnv.getUseAsDefault());
                    outData = execute(data, exec);
                } else if (this instanceof LoopStartNode) {
                    outData = execute(data, exec);
                } else {
                    m_logger.coding("Cannot re-execute non interactive node. Using normal execute instead.");
                    outData = execute(data, exec);
                }
            }

            // if execution was canceled without exception flying return false
            if (exec.isCanceled()) {
                throw new CanceledExecutionException("Result discarded due to user cancel");
            }
        } catch (Exception e) {
            // clear local tables (which otherwise would continue to block resources)
            exec.onCancel();
            throw e;
        }

        if (outData == null) {
            outData = new PortObject[getNrOutPorts()];
        }

        /* Cleanup operation for nodes that just pass on their input
         * data table. We need to wrap those here so that the framework
         * explicitly references them (instead of copying) */
        for (int i = 0; i < outData.length; i++) {
            if (outData[i] instanceof BufferedDataTable) {
                for (int j = 0; j < data.length; j++) {
                    if (outData[i] == data[j]) {
                        outData[i] = exec.createWrappedTable((BufferedDataTable)data[j]);
                    }
                }
            } else if (outData[i] instanceof FileStorePortObject) {
                // file stores can be 'external', e.g. when a model reader node reads an external model file
                FileStorePortObject fsPO = (FileStorePortObject)outData[i];
                IDataRepository expectedRep = exec.getDataRepository();
                IDataRepository actualRep = FileStoreUtil.getFileStores(fsPO).stream()
                        .map(FileStoreUtil::getFileStoreHandler).map(h -> h.getDataRepository())
                        .findFirst().orElse(expectedRep);
                if (actualRep != expectedRep) {
                    outData[i] = Node.copyPortObject(fsPO, exec);
                }
            }
        }

        // TODO: check outgoing types! (inNode!)

        // if number of out tables does not match: fail
        if (outData.length != getNrOutPorts()) {
            throw new IllegalStateException(
                    "Invalid result. Execution failed. "
                            + "Reason: Incorrect implementation; the execute"
                            + " method in " + this.getClass().getSimpleName()
                            + " returned null or an incorrect number of output"
                            + " tables.");
        }

        // check the result, data tables must not be null
        for (int i = 0; i < outData.length; i++) {
            // do not check for null output tables if this is the end node
            // of a loop and another loop iteration is requested
            if ((getLoopContext() == null) && (outData[i] == null)) {
                m_logger.error("Execution failed: Incorrect implementation;"
                        + " the execute method in "
                        + this.getClass().getSimpleName()
                        + "returned a null data table at port: " + i);
                throw new IllegalStateException("Invalid result. "
                        + "Execution failed, reason: data at output " + i
                        + " is null.");
            }
        }
        // check meaningfulness of result and warn,
        // - only if the execute didn't issue a warning already
        if ((m_warningMessage == null) || (m_warningMessage.length() == 0)) {
            boolean hasData = false;
            int bdtPortCount = 0; // number of BDT ports
            for (int i = 0; i < outData.length; i++) {
                if (outData[i] instanceof BufferedDataTable) {
                    // do some sanity checks on PortObjects holding data tables
                    bdtPortCount += 1;
                    BufferedDataTable outDataTable =
                        (BufferedDataTable)outData[i];
                    if (outDataTable.size() > 0) {
                        hasData = true;
                    } else {
                        m_logger.info("The result table at port " + i
                                + " contains no rows");
                    }
                }
            }
            if (!hasData && bdtPortCount > 0) {
                if (bdtPortCount == 1) {
                    setWarningMessage("Node created an empty data table.");
                } else {
                    setWarningMessage(
                            "Node created empty data tables on all out-ports.");
                }
            }
        }

        setHasContent(true);
        PortObject[] rawOutData = new PortObject[getNrOutPorts() + 1];
        rawOutData[0] = FlowVariablePortObject.INSTANCE;
        System.arraycopy(outData, 0, rawOutData, 1, outData.length);
        return rawOutData;
    } // executeModel(PortObject[],ExecutionMonitor)

    /**
     * Sets the hasContent flag and fires a state change event.
     * @param hasContent Flag if this node is configured be executed or not.
     */
    final void setHasContent(final boolean hasContent) {
        if (hasContent != hasContent()) {
            m_hasContent = hasContent;
            // and inform all views about the new model
            stateChanged();
        }
    }

    /**
     * @return <code>true</code> if this model has been executed and therefore
     * possibly has content that can be displayed in a view,
     * <code>false</code> otherwise.
     */
    final boolean hasContent() {
        return m_hasContent;
    }

    /**
     * Execute method for general port types. The argument <code>inObjects</code> represent the input objects and the
     * returned array represents the output objects. The elements in the argument array are generally guaranteed to be
     * not null and subclasses of the {@link PortObject PortObject classes} that are defined through the
     * {@link PortType PortTypes} given in the {@link NodeModel#NodeModel(PortType[], PortType[]) constructor}.
     * Similarly, the returned output objects need to comply with their port types object class (otherwise an error is
     * reported by the framework) and must not be null. There are few exceptions to these rules:
     * <ul>
     * <li>Nodes with optional inputs (as specified in the constructor) may find null elements in the array.</li>
     * <li>Node that implement {@link InactiveBranchConsumer} may find instances of {@link InactiveBranchPortObject} in
     * case the corresponding input is inactive.</li>
     * <li>All nodes are allowed to return {@link InactiveBranchPortObject} elements in case the output should be
     * inactivated.</li>
     * <li>Loop end nodes may return null in case they restart the loop by setting the {@link #continueLoop()
     * corresponding flags}.</li>
     * </ul>
     *
     * <p>
     * For a general description of the execute method refer to the description of the specialized
     * {@link #execute(BufferedDataTable[], ExecutionContext)} methods as it addresses more use cases.
     *
     * @param inObjects The input objects.
     * @param exec For {@link BufferedDataTable} creation and progress.
     * @return The output objects.
     * @throws Exception If the node execution fails for any reason.
     */
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {

        // default implementation: the standard version needs to hold: all
        // ports are data ports!

        // (1) cast PortObjects to BufferedDataTable
        BufferedDataTable[] inTables =
            toBDTArray(inObjects, "Input port", "Likely reason: wrong version of NodeModel.execute() overwritten!");
        // (2) call old-fashioned, data-only execute
        BufferedDataTable[] outData = execute(inTables, exec);
        // (3) return new POs (upcast from BDT automatic)
        return outData;
    }

    /** Type casts the elements in the argument array to BDT and returns an array of it.
     * @param inObjects The objects to type cast, all expected to be null or BDT.
     * @param typeOfInputForError error message start, e.g. "Input port" or "Internal held object index"
     * @param endErrorString appended to the error, like "wrong version of NodeModel.execute() overwritten!"
     * @return ...
     * @throws IOException If any element is not a BDT
     */
    static BufferedDataTable[] toBDTArray(final PortObject[] inObjects, final String typeOfInputForError,
        final String endErrorString) throws IOException {
        BufferedDataTable[] inTables = new BufferedDataTable[inObjects.length];
        for (int i = 0; i < inObjects.length; i++) {
            try {
                inTables[i] = (BufferedDataTable)inObjects[i];
            } catch (ClassCastException cce) {
                throw new IOException(String.format("%s %d does not hold data table but %s; %s",
                    typeOfInputForError, i, inObjects[i].getClass().getSimpleName(), endErrorString));
            }
        }
        return inTables;
    }

    /**
     * This function is invoked by the <code>Node#executeNode()</code> method of the node (through the
     * <code>#executeModel(BufferedDataTable[],ExecutionMonitor)</code> method) only after all predecessor nodes have
     * been successfully executed and all data is therefore available at the input ports. Implement this function with
     * your task in the derived model.
     * <p>
     * The input data is available in the given array argument <code>inData</code> and is ensured to be neither
     * <code>null</code> nor contain <code>null</code> elements (with few non-standard exception, which are described in
     * more detail in {@link #execute(PortObject[], ExecutionContext)}).
     *
     * <p>
     * In order to create output data, you need to create objects of class <code>BufferedDataTable</code>. Use the
     * execution context argument to create <code>BufferedDataTable</code>.
     *
     * @param inData An array holding <code>DataTable</code> elements, one for each input.
     * @param exec The execution monitor for this execute method. It provides us with means to create new
     *            <code>BufferedDataTable</code>. Additionally, it should be asked frequently if the execution should be
     *            interrupted and throws an exception then. This exception might me caught, and then after closing all
     *            data streams, been thrown again. Also, if you can tell the progress of your task, just set it in this
     *            monitor.
     * @return An array of non- <code>null</code> DataTable elements with the size of the number of outputs. The result
     *         of this execution.
     * @throws Exception If you must fail the execution. Try to provide a meaningful error message in the exception as
     *             it will be displayed to the user.<STRONG>Please</STRONG> be advised to check frequently the canceled
     *             status by invoking <code>ExecutionMonitor#checkCanceled</code> which will throw an
     *             <code>CanceledExcecutionException</code> and abort the execution.
     */
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        throw new IOException("NodeModel.execute() implementation missing!");
    }

    /**
     * Invokes the abstract <code>#reset()</code> method of the derived model.
     * In addition, this method resets all hilite handlers, and notifies all
     * views about the changes.
     */
    final void resetModel() {
        try {
            setWarningMessage(null);
            // reset in derived model
            reset();
        } catch (Throwable t) {
            String name = t.getClass().getSimpleName();
            m_logger.coding("Reset failed due to a " + name, t);
        } finally {
            // reset these property handlers
            resetHiLiteHandlers();
            setHasContent(false); // also fires stateChanged()
        }
    }

    /**
     * Override this function in the derived model and reset your
     * <code>NodeModel</code>. All components should unregister themselves
     * from any observables (at least from the hilite handler right now). All
     * internally stored data structures should be released. User settings
     * should not be deleted/reset though.
     */
    protected abstract void reset();

    /** Called by the framework when the node is disposed, for instance if
     * the workflow is closed or the node is deleted by the user. Subclasses may
     * override this method to do further cleanup. This method is called
     * independent of whether this node is or has ever been executed.
     */
    protected void onDispose() {
        // empty, potentially overridden in sub-classes
    }

    /**
     * Notifies all registered views of a change of the underlying model. It is
     * called by functions of the abstract class that modify the model (like
     * <code>#executeModel()</code> and <code>#resetModel()</code> ).
     */
    protected final void stateChanged() {
        for (AbstractNodeView<?> view : m_views) {
            try {
                view.callModelChanged();
            } catch (Exception e) {
                String msg =
                    "View [" + view.getViewName() + "] caused an error while displaying new contents: "
                        + e.getMessage();
                setWarningMessage(msg);
                m_logger.debug(msg, e);
            }
        }
    }

    /**
     * This method can be called from the derived model in order to inform the
     * views about changes of the settings or during execution, if you want the
     * views to show the progress, and if they can display models half way
     * through the execution. In the view
     * <code>NodeView#updateModel(Object)</code> is called and needs to
     * be overridden.
     *
     * @param arg The argument you want to pass.
     */
    protected final void notifyViews(final Object arg) {
        m_logger.assertLog(NodeContext.getContext() != null,
                "No node context available, please check call hierarchy and fix it");

        for (AbstractNodeView<?> view : m_views) {
            view.updateModel(arg);
        }
    }

    /**
     * This implementation is empty. Subclasses may override this method
     * in order to be informed when the hilite handler changes at the inport,
     * e.g. when the node (or an preceding node) is newly connected.
     *
     * @param inIndex The index of the input.
     * @param hiLiteHdl The <code>HiLiteHandler</code> at input index.
     *         May be <code>null</code> when not available, i.e. not properly
     *         connected.
     * @throws IndexOutOfBoundsException If the <code>inIndex</code> is not in
     *          the range of inputs.
     */
    protected void setInHiLiteHandler(final int inIndex,
            final HiLiteHandler hiLiteHdl) {
        assert inIndex >= 0;
        assert hiLiteHdl == hiLiteHdl;
    }

    /**
     * Sets a new <code>HiLiteHandler</code> for the given input.
     *
     * This method is called by the corresponding <code>Node</code> in order
     * to set the <code>HiLiteHandler</code> for the given input port.
     *
     * @param in The input index.
     * @param hdl The new <code>HiLiteHandler</code>.
     *
     * @see #setInHiLiteHandler(int, HiLiteHandler)
     */
    final void setNewInHiLiteHandler(final int in, final HiLiteHandler hdl) {
        if (m_inHiLiteHdls[in] == hdl) {
            // only do something (calls notification and state change!)
            // if there really is a new HiLiteHandler.
            return;
        }
        m_inHiLiteHdls[in] = hdl;
        int simulatedPortIndex = getSimulatedHiliteHandlerPortIndex(in);
        if (simulatedPortIndex >= 0) {
            setInHiLiteHandler(simulatedPortIndex, hdl);
        }
        stateChanged();
    }

    /**
     * Returns the <code>HiLiteHandler</code> for the given input index, if the
     * current in-port hilite handler is <code>null</code> an
     * <code>HiLiteHandlerAdapter</code> is created and returned.
     *
     * @param inIndex in-port index
     * @return <code>HiLiteHandler</code> for the given input index
     * @throws IndexOutOfBoundsException if the <code>inIndex</code> is not in
     *             the range of inputs
     */
    public final HiLiteHandler getInHiLiteHandler(final int inIndex) {
        int correctIndex = getTrueHiliteHandlerPortIndex(inIndex);
        if (m_inHiLiteHdls[correctIndex] == null) {
            return HILITE_ADAPTER;
        }
        return m_inHiLiteHdls[correctIndex];
    }

    /** Returns the argument. This method is overridden in class
     * {@link NodeModel} to handle incoming model ports appropriately
     * (no hilite handlers at deprecated model ports).
     * @param portIndex The simulated port index
     * @return The true port index
     */
    int getTrueHiliteHandlerPortIndex(final int portIndex) {
        return portIndex;
    }

    /** Returns the argument. This method is overridden in class
     * {@link NodeModel} to handle incoming model ports appropriately
     * (no hilite handlers at deprecated model ports).
     * @param portIndex The true port index
     * @return The simulated port index
     */
    int getSimulatedHiliteHandlerPortIndex(final int portIndex) {
        return portIndex;
    }

    /**
     * Returns the <code>HiLiteHandler</code> for the given output index. This
     * default implementation simply passes on the handler of input port 0 or
     * generates a new one if this node has no inputs. <br>
     * <br>
     * This method is intended to be overridden
     *
     * @param outIndex The output index.
     * @return <code>HiLiteHandler</code> for the given output port.
     * @throws IndexOutOfBoundsException If the index is not in output's range.
     */
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        if (outIndex < 0 || outIndex >= getNrOutPorts()) {
            throw new IndexOutOfBoundsException("index=" + outIndex);
        }
        // if we have no inputs we create a new instance (but only one...)
        if (getNrInPorts() == 0) {
            if (m_inHiLiteHdls[0] == null) {
                m_inHiLiteHdls[0] = new HiLiteHandler();
            }
            return m_inHiLiteHdls[0];
        }
        int firstBDTPort = 0;
        for (int i = 0; i < getNrInPorts(); i++) {
            if (getInPortType(i).equals(BufferedDataTable.TYPE)) {
                firstBDTPort = i;
                break;
            }
        }
        return getInHiLiteHandler(firstBDTPort);
    }

    /**
     * Resets all internal <code>HiLiteHandler</code> objects if the number of
     * inputs is zero.
     */
    private void resetHiLiteHandlers() {
        // if we have no inputs we have created a new instance.
        // we need to reset it here then.
        if (getNrInPorts() == 0) {
            for (int i = 0; i < m_inHiLiteHdls.length; i++) {
                if (m_inHiLiteHdls[i] != null) {
                    m_inHiLiteHdls[i].fireClearHiLiteEvent();
                }
            }
        }
    }

    /**
     * This function is called when something changes that could affect the
     * output <code>DataTableSpec</code> elements. E.g. after a reset,
     * execute, (dis-)connect, and object creation (model instantiation).
     * <p>
     * The function calls <code>#configure()</code> to receive the updated
     * output DataTableSpecs, if the model is not executed yet. After execution
     * the DataTableSpecs are simply taken from the output DataTables.
     *
     * @param inSpecs An array of input <code>DataTableSpec</code> elements,
     *            either the array or each of its elements can be
     *            <code>null</code>.
     * @return An array where each element indicates if the outport has changed.
     * @throws InvalidSettingsException if the current settings don't go along
     *             with the table specs
     */
    final PortObjectSpec[] configureModel(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        assert inSpecs.length == getNrInPorts();

        setWarningMessage(null);

        PortObjectSpec[] copyInSpecs = new PortObjectSpec[getNrInPorts()];
        PortObjectSpec[] newOutSpecs;

        System.arraycopy(inSpecs, 0, copyInSpecs, 0, inSpecs.length);
        // make sure we conveniently have TableSpecs.
        // Rather empty ones than null
        for (int i = 0; i < copyInSpecs.length; i++) {
            if (copyInSpecs[i] == null
                    && BufferedDataTable.TYPE.equals(m_inPortTypes[i])
                    && !m_inPortTypes[i].isOptional()) {
                // only mimic empty table for real table connections
                copyInSpecs[i] = new DataTableSpec();
            }
            // only weak port compatibility check during connect
            // (model reader can be connected to any model port)
            // complain if actual types are incompatible.
            Class<? extends PortObjectSpec> expected =
                m_inPortTypes[i].getPortObjectSpecClass();
            if (copyInSpecs[i] != null  // i.e. skip only "optional and not connected"
                    && !expected.isAssignableFrom(copyInSpecs[i].getClass())
                    && !(copyInSpecs[i] instanceof InactiveBranchPortObjectSpec)) {
                StringBuilder b = new StringBuilder("Incompatible port spec");
                if (copyInSpecs.length > 1) {
                    b.append(" at port ").append(i);
                }
                b.append(", expected: ").append(expected.getSimpleName());
                b.append(", actual: ").append(
                        copyInSpecs[i].getClass().getSimpleName());
                throw new InvalidSettingsException(b.toString());
            }
        }

        // CALL CONFIGURE
        newOutSpecs = configure(copyInSpecs);
        if (newOutSpecs == null) {
            newOutSpecs = new PortObjectSpec[getNrOutPorts()];
        }
        // check output object spec length
        if (newOutSpecs.length != getNrOutPorts()) {
            m_logger.error("Output spec-array length invalid: "
                    + newOutSpecs.length + " <> " + getNrOutPorts());
            newOutSpecs = new PortObjectSpec[getNrOutPorts()];
        }
        return newOutSpecs;
    }

    /**
     * Configure method for general port types. The argument specs represent the input object specs and are guaranteed
     * to be subclasses of the {@link PortObjectSpec PortObjectSpecs} that are defined through the {@link PortType
     * PortTypes} given in the {@link NodeModel#NodeModel(PortType[], PortType[]) constructor} unless this model is an
     * {@link InactiveBranchConsumer} (most nodes are not). Similarly, the returned output specs need to comply with
     * their port types spec class (otherwise an error is reported by the framework). They may also be null (out spec
     * not known at time of configuration) or
     * {@linkplain org.knime.core.node.port.flowvariable.FlowVariablePortObjectSpec inactive} (output and downstream
     * nodes are inactive).
     *
     * <p>
     * For a general description of the configure method refer to the description of the specialized
     * {@link #configure(DataTableSpec[])} methods as it addresses more use cases.
     *
     * @param inSpecs The input data table specs. Items of the array could be null if no spec is available from the
     *            corresponding input port (i.e. not connected or upstream node does not produce an output spec). If a
     *            port is of type {@link BufferedDataTable#TYPE} and no spec is available the framework will replace
     *            null by an empty {@link DataTableSpec} (no columns) unless the port is marked as optional as per
     *            constructor.
     * @return The output objects specs or null.
     * @throws InvalidSettingsException If this node can't be configured.
     */
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {

        // default implementation: the standard version needs to hold: all
        // ports are data ports!

        // (1) cast PortObjectSpecs to DataTableSpecs
        DataTableSpec[] inDataSpecs = new DataTableSpec[inSpecs.length];
        for (int i = 0; i < inSpecs.length; i++) {
            try {
                inDataSpecs[i] = (DataTableSpec)inSpecs[i];
            } catch (ClassCastException cce) {
                throw new InvalidSettingsException("Input Port " + i + " does not hold data table specs. "
                    + "Likely reason: wrong version" + " of NodeModel.configure() overwritten!");
            }
        }
        // (2) call old-fashioned, data-only configure
        DataTableSpec[] outDataSpecs = configure(inDataSpecs);
        // (3) return new POs (upcast from DataSpecs automatic)
        return outDataSpecs;
    }

    /**
     * This function is called whenever the derived model should re-configure and generate the expected output specs.
     * Based on the given input data table spec(s) and the current model's settings, the derived model has to calculate
     * the output data table spec and return them.
     * <p>
     * For ordinary(*) nodes the passed DataTableSpec elements are never <code>null</code> but can be empty. The model
     * may return <code>null</code> data table spec(s) for the outputs. Note, after the model has been executed this
     * function will not be called anymore, as the output DataTableSpecs are then being pulled from the output
     * DataTables. A derived <code>NodeModel</code> that cannot provide any DataTableSpecs at its outputs before
     * execution (because the table structure is unknown at this point) can return an array containing just
     * <code>null</code> elements. As an example consider a "Transpose" node that flips columns to rows -- there is no
     * way to determine the table spec at time of configuration as the number of rows (which is the number of new
     * columns at the output) is unknown though the node is still executable.
     *
     * <p>
     * (*)For nodes that support optional inputs or may have inactive outputs it's better to override
     * {@link #configure(PortObjectSpec[])}.
     * <p>
     * Implementation note: This method is called from the {@link #configure(PortObjectSpec[])} method unless that
     * method is overwritten.
     *
     * @param inSpecs The input data table specs (as many as this model has inputs). Do NOT modify the contents of this
     *            array. If no spec is available for any given port (because the port is not connected or the previous
     *            node does not produce a spec) the framework will pass an empty {@link DataTableSpec} (no columns)
     *            unless the port is marked as {@link PortType#isOptional() optional} (in which case the array element
     *            is null).
     * @return An array of DataTableSpecs (as many as this model has outputs) They will be propagated to connected
     *         successor nodes. <code>null</code> DataTableSpec elements are changed to empty once.
     *
     * @throws InvalidSettingsException if the <code>#configure()</code> failed, that is, the settings are inconsistent
     *             with given DataTableSpec elements.
     */
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        throw new InvalidSettingsException("NodeModel.configure()" + " implementation missing!");
    }

    /////////////////////////
    // Warning handling
    /////////////////////////

    /** Method being called when node is restored. It does not notify listeners.
     * @param warningMessage The message as written to the workflow file
     */
    final void restoreWarningMessage(final String warningMessage) {
        m_warningMessage = warningMessage;
    }

    /**
     * Sets an optional warning message by the implementing node model.
     *
     * @param warningMessage the warning message to set
     */
    protected final void setWarningMessage(final String warningMessage) {
        // message changed, set new one and notify listeners.
        m_warningMessage = warningMessage;
        notifyWarningListeners(m_warningMessage);
    }

    /**
     * Get the most recently set warning message.
     * @return the warningMessage that is currently set (or null)
     */
    protected final String getWarningMessage() {
        return m_warningMessage;
    }

    /**
     * Adds a warning listener to this node. Ignored if the listener is already
     * registered.
     *
     * @param listener The listener to add.
     */
    public void addWarningListener(final NodeModelWarningListener listener) {
        if (listener == null) {
            throw new NullPointerException(
                    "NodeModel message listener must not be null!");
        }
        m_warningListeners.add(listener);
    }

    /**
     * Removes a warning listener from this node. Ignored if the listener is
     * not registered.
     *
     * @param listener The listener to remove.
     */
    public void removeWarningListener(
            final NodeModelWarningListener listener) {
        if (!m_warningListeners.remove(listener)) {
            m_logger.debug("listener was not registered: " + listener);
        }
    }

    /**
     * Notifies all listeners that the warning of this node has changed.
     * @param warning message
     */
    public void notifyWarningListeners(final String warning) {
        for (NodeModelWarningListener listener : m_warningListeners) {
            try {
                listener.warningChanged(warning);
            } catch (Throwable t) {
                m_logger.error("Exception while notifying NodeModel listeners",
                        t);
            }
        }
    }

    /** Credentials provider, set by the NodeContainer (via) Node before
     * configure/execute. */
    private CredentialsProvider m_credentialsProvider;

    /** Framework method to update credentials provider before
     * configure/execute.
     * @param provider The provider for credentials. */
    final void setCredentialsProvider(final CredentialsProvider provider) {
        m_credentialsProvider = provider;
    }

    /** Accessor to credentials defined on a workflow. The method will return
     * a non-null provider during {@link #configure(DataTableSpec[])} and
     * {@link #execute(BufferedDataTable[], ExecutionContext)} (or their
     * respective {@link PortObject}-typed counterparts). Sub-classes can
     * read out credentials here. Any invocation of the
     * {@link CredentialsProvider#get(String)} method will register this node
     * as client to the requested credentials, indicating that theses
     * credentials need to be available upon load or in a remote execution of
     * this node (e.g. cluster or server execution). The credentials identifier
     * (its {@link ICredentials#getName()} should be part of the configuration,
     * i.e. chosen by the user in the configuration dialog.
     *
     * @return A provider for credentials available in this workflow.
     */
    protected final CredentialsProvider getCredentialsProvider() {
        return m_credentialsProvider;
    }

    /** Holds the {@link FlowObjectStack} of this node. */
    private FlowObjectStack m_flowObjectStack;

    /** Variables that were added by the node model. This is to fix bug 1771
     * (flow object stack contains obsolete items). These elements will be
     * pushed on the real node stack after execute. */
    private FlowObjectStack m_outgoingFlowObjectStack;

    /** Get the value of the String variable with the given name leaving the
     * flow variable stack unmodified.
     * @param name Name of the variable
     * @return The value of the string variable
     * @throws NullPointerException If the argument is null
     * @throws NoSuchElementException If no such variable with the correct
     * type is available.
     * @since 2.8
     */
    public final String peekFlowVariableString(final String name) {
        try {
            return m_outgoingFlowObjectStack.peekFlowVariable(
                    name, FlowVariable.Type.STRING).getStringValue();
        } catch (NoSuchElementException e) {
            return m_flowObjectStack.peekFlowVariable(
                    name, FlowVariable.Type.STRING).getStringValue();
        }
    }

    /** Put a new variable of type double onto the stack. If such variable
     * already exists, its value will be (virtually) overwritten.
     * @param name The name of the variable.
     * @param value The assignment value for the variable
     * @throws NullPointerException If the name argument is null.
     */
    protected final void pushFlowVariableDouble(final String name, final double value) {
        pushFlowVariable(new FlowVariable(name, value));
    }

    /**
     * Put a new {@link FlowVariable} of any {@link VariableType} onto the stack. If such variable already exists, its
     * value will be (virtually) overwritten.
     *
     * @param name the name of the variable
     * @param type the {@link VariableType} of the variable
     * @param value the simple value of the variable
     * @param <T> the simple value type of the variable
     * @throws NullPointerException if any argument is null
     *
     * @since 4.1
     */
    protected final <T> void pushFlowVariable(final String name, final VariableType<T> type, final T value) {
        CheckUtils.checkArgumentNotNull(name, "Variable name must not be null.");
        CheckUtils.checkArgumentNotNull(type, "Variable type must not be null.");
        CheckUtils.checkArgumentNotNull(value, "Variable value must not be null.");
        pushFlowVariable(new FlowVariable(name, type, value));
    }

    /** Get the value of the double variable with the given name leaving the
     * variable stack unmodified.
     * @param name Name of the variable
     * @return The assignment value of the variable
     * @throws NullPointerException If the argument is null
     * @throws NoSuchElementException If no such variable with the correct
     * type is available.
     * @since 2.8
     */
    public final double peekFlowVariableDouble(final String name) {
        try {
            return m_outgoingFlowObjectStack.peekFlowVariable(
                    name, FlowVariable.Type.DOUBLE).getDoubleValue();
        } catch (NoSuchElementException e) {
            return m_flowObjectStack.peekFlowVariable(
                    name, FlowVariable.Type.DOUBLE).getDoubleValue();
        }
    }

    /**
     * Get the value of the top-most (output or input) {@link FlowVariable} with a certain name and {@link VariableType}
     * from the stack, leaving the stack unmodified.
     *
     * @param name the name of the variable
     * @param type the {@link VariableType} of the variable
     * @param <T> the simple value type of the variable
     * @return the simple non-null value of the top-most variable with the argument name and type
     * @throws NullPointerException if any argument is null
     * @throws NoSuchElementException if no variable with the correct name and type is available.
     * @since 4.1
     * @see FlowObjectStack#peekFlowVariable(String, VariableType)
     */
    public final <T> T peekFlowVariable(final String name, final VariableType<T> type) {
        CheckUtils.checkArgumentNotNull(name, "Variable name must not be null.");
        CheckUtils.checkArgumentNotNull(type, "Variable type must not be null.");
        CheckUtils.checkArgumentNotNull(m_outgoingFlowObjectStack, "Outgoing flow object stack must not be null.");
        if (m_outgoingFlowObjectStack != null) {
            final Optional<FlowVariable> out = m_outgoingFlowObjectStack.peekFlowVariable(name, type);
            if (out.isPresent()) {
                return out.get().getValue(type);
            }
        }
        if (m_flowObjectStack != null) {
            final Optional<FlowVariable> out = m_flowObjectStack.peekFlowVariable(name, type);
            if (out.isPresent()) {
                return out.get().getValue(type);
            }
        }
        throw new NoSuchElementException("No such variable \"" + name + "\" of type " + type);
    }

    /** Get the FlowScopeContext on top leaving the variable stack unmodified.
     * @return The FlowScopeContext
     * @throws NoSuchElementException If no FlowScopeContext exists
     */
    final FlowScopeContext peekFlowScopeContext() {
        FlowScopeContext fsc = null;
        fsc = m_outgoingFlowObjectStack.peek(FlowScopeContext.class);
        if (fsc == null) {
            fsc = m_flowObjectStack.peek(FlowScopeContext.class);
        }
        if (fsc == null) {
            throw new NoSuchElementException();
        }
        return fsc;
    }

    /** Put a new variable of type integer onto the stack. If such variable
     * already exists, its value will be (virtually) overwritten.
     * @param name The name of the variable.
     * @param value The assignment value for the variable
     * @throws NullPointerException If the name argument is null.
     */
    protected final void pushFlowVariableInt(final String name, final int value) {
        pushFlowVariable(new FlowVariable(name, value));
    }

    final void pushFlowVariable(final FlowVariable variable) {
        m_outgoingFlowObjectStack.push(variable);
    }

    /** Get the value of the integer variable with the given name leaving the
     * variable stack unmodified.
     * @param name Name of the variable
     * @return The value of the integer variable
     * @throws NullPointerException If the argument is null
     * @throws NoSuchElementException If no such variable with the correct
     * type is available.
     * @since 2.8
     */
    public final int peekFlowVariableInt(final String name) {
        try {
            return m_outgoingFlowObjectStack.peekFlowVariable(
                    name, FlowVariable.Type.INTEGER).getIntValue();
        } catch (NoSuchElementException e) {
            return m_flowObjectStack.peekFlowVariable(
                    name, FlowVariable.Type.INTEGER).getIntValue();
        }
    }

    /** Put a new variable of type String onto the stack. If such variable
     * already exists, its value will be (virtually) overwritten.
     * @param name The name of the variable.
     * @param value The assignment value for the variable
     * @throws NullPointerException If the name argument is null.
     */
    protected final void pushFlowVariableString(
            final String name, final String value) {
        pushFlowVariable(new FlowVariable(name, value));
    }

    FlowObjectStack getFlowObjectStack() {
        return m_flowObjectStack;
    }

    void setFlowObjectStack(final FlowObjectStack scsc,
            final FlowObjectStack outgoingFlowObjectStack) {
        m_flowObjectStack = scsc;
        m_outgoingFlowObjectStack = outgoingFlowObjectStack;
    }

    /**
     * @return list of added flow variables in this node.
     */
    FlowObjectStack getOutgoingFlowObjectStack() {
        return m_outgoingFlowObjectStack;
    }

    /**
     * Get all flow variables of types {@link StringType}, {@link DoubleType}, and {@link IntType} currently available
     * at this node. The keys of the returned map will be the identifiers of the flow variables and the values the flow
     * variables itself. The map is non-modifiable.
     *
     * <p>
     * The map contains all flow variables, i.e. the variables that are provided as part of (all) input connections and
     * the variables that were pushed onto the stack by this node itself (which is different from the
     * {@link NodeDialogPane#getAvailableFlowVariables()} method, which only returns variables provided at the input.
     *
     * @return A new map of available flow variables in a non-modifiable map (never null).
     * @since 2.3.3
     * @deprecated Use {@link #getAvailableFlowVariables(VariableType[])} instead.
     */
    @Deprecated
    public final Map<String, FlowVariable> getAvailableFlowVariables() {
        Map<String, FlowVariable> result = new LinkedHashMap<String, FlowVariable>();
        if (m_flowObjectStack != null) {
            result.putAll(m_flowObjectStack.getAvailableFlowVariables());
            result.putAll(m_outgoingFlowObjectStack.getAvailableFlowVariables());
        }
        return Collections.unmodifiableMap(result);
    }

    /** Get all flow variables if they are one of the given types. Keys are the names, values are the flow variable
     * itself.
     *
     * <p>The map contains all flow variables, i.e. the variables that are provided as part of (all) input connections
     * and the variables that were pushed onto the stack by this node itself (which is different from the
     * {@link NodeDialogPane#getAvailableFlowVariables()} method, which only returns variables provided at the input.
     *
     * <p>The class {@link FlowVariable} is subject to change and hence the use of this method is discouraged. In most
     * cases it's sufficient to retrieve primitive variables using {@link #getAvailableFlowVariables()}.

     * @param types The type list - a variable whose type is in the argument list is filtered (= part of the result)
     * @return A new map of available flow variables in a non-modifiable map (never null).
     * @since 3.1
     * @deprecated Use {@link #getAvailableFlowVariables(VariableType[])} instead.
     */
    @Deprecated
    final Map<String, FlowVariable> getAvailableFlowVariables(final FlowVariable.Type... types) {
        Map<String, FlowVariable> result = new LinkedHashMap<String, FlowVariable>();
        if (m_flowObjectStack != null) {
            result.putAll(m_flowObjectStack.getAvailableFlowVariables(types));
            result.putAll(m_outgoingFlowObjectStack.getAvailableFlowVariables(types));
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Get a map of all (input and output) {@link FlowVariable FlowVariables} whose {@link VariableType} is equal to any
     * of the arguments.
     *
     * @param types any number of {@link VariableType} singletons; the list of valid types is defined in class
     *            {@link VariableType} (non-API) and may change between versions of KNIME.
     * @return The non-null read-only map of flow variable name -&gt; {@link FlowVariable}
     * @see FlowObjectStack#getAvailableFlowVariables(VariableType[])
     * @since 4.1
     */
    public final Map<String, FlowVariable> getAvailableFlowVariables(final VariableType<?>[] types) {
        return Collections.unmodifiableMap(Stream.concat(//
            Optional.ofNullable(m_flowObjectStack)//
            .map(s -> s.getAvailableFlowVariables(types).entrySet().stream()).orElseGet(Stream::empty),
            Optional.ofNullable(m_outgoingFlowObjectStack)//
            .map(s -> s.getAvailableFlowVariables(types).entrySet().stream()).orElseGet(Stream::empty))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1, LinkedHashMap::new)));
    }

    /**
     * Get a map of all (input and output) {@link FlowVariable FlowVariables} whose {@link VariableType} is equal to any
     * of the arguments.
     *
     * @param type a {@link VariableType} singleton
     * @param otherTypes any number of additional {@link VariableType} singletons
     * @return The non-null read-only map of flow variable name -&gt; {@link FlowVariable}
     * @see FlowObjectStack#getAvailableFlowVariables(VariableType[])
     * @since 4.1
     */
    public Map<String, FlowVariable> getAvailableFlowVariables(final VariableType<?> type,
        final VariableType<?>... otherTypes) {
        return getAvailableFlowVariables(ArrayUtils.add(otherTypes, type));
    }

    /**
     * Get all flow variables of types {@link StringType}, {@link DoubleType}, and {@link IntType} currently available
     * at the input of this node. The keys of the returned map will be the identifiers of the flow variables and the
     * values the flow variables itself. The map is non-modifiable.
     *
     * <p>
     * The map contains only flow variables that are provided as part of (all) input connections (which is same as the
     * list returned in the dialog by calling {@link NodeDialogPane#getAvailableFlowVariables()}).
     *
     * @return A new map of input flow variables in a non-modifiable map (never null).
     * @since 2.6
     * @deprecated Use {@link #getAvailableInputFlowVariables(VariableType[])} instead.
     */
    @Deprecated
    public final Map<String, FlowVariable> getAvailableInputFlowVariables() {
        Map<String, FlowVariable> result =
            new LinkedHashMap<String, FlowVariable>();
        if (m_flowObjectStack != null) {
            result.putAll(m_flowObjectStack.getAvailableFlowVariables());
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Get a map of all input {@link FlowVariable FlowVariables} whose {@link VariableType} is equal to any of the
     * arguments.
     *
     * @param types any number of {@link VariableType} singletons; the list of valid types is defined in class
     *            {@link VariableType} (non-API) and may change between versions of KNIME.
     * @return the non-null read-only map of flow variable name -&gt; {@link FlowVariable}
     * @see FlowObjectStack#getAvailableFlowVariables(VariableType[])
     * @since 4.1
     */
    public final Map<String, FlowVariable> getAvailableInputFlowVariables(final VariableType<?>[] types) {
        return m_flowObjectStack != null ? m_flowObjectStack.getAvailableFlowVariables(types) : Collections.emptyMap();
    }

    /**
     * Get a map of all input {@link FlowVariable FlowVariables} whose {@link VariableType} is equal to any of the
     * arguments.
     *
     * @param type a {@link VariableType} singleton
     * @param otherTypes any number of additional {@link VariableType} singletons
     * @return the non-null read-only map of flow variable name -&gt; {@link FlowVariable}
     * @see FlowObjectStack#getAvailableFlowVariables(VariableType[])
     * @since 4.1
     */
    public Map<String, FlowVariable> getAvailableInputFlowVariables(final VariableType<?> type,
        final VariableType<?>... otherTypes) {
        return getAvailableInputFlowVariables(ArrayUtils.add(otherTypes, type));
    }

    //////////////////////////////////////////
    // Loop Support...
    //
    // TODO: maybe all of this should be moved into an adaptor class
    // "LoopNodeModelAdapter" which keeps the node's role and all of
    // the loop specific stuff? Later...
    //
    //////////////////////////////////////////

    /** Informs WorkflowManager after execute to continue the loop.
     * Call by the end of the loop! This will result in both
     * this Node as well as the creator of the FlowLoopContext to be
     * queued for execution once again. In this case the node can return
     * an empty table after execution.
     *
     * Called on LoopTail Node's only.
     */
    protected final void continueLoop() {
        if (!(this instanceof LoopEndNode)) {
            throw new IllegalStateException(
                "continueLoop called from non-end node (Coding Error)!");
        }
        FlowLoopContext slc = m_flowObjectStack.peek(FlowLoopContext.class);
        if (slc != null && slc.isInactiveScope()) {
            m_logger.coding("Encountered an inactive FlowLoopContext in continueLoop.");
            // continue with historically "correct" solution:
            slc = m_flowObjectStack.peekScopeContext(FlowLoopContext.class, false);
        }
        if (slc == null) {
            // wrong wiring of the pipeline: head seems to be missing!
            throw new IllegalStateException(
                    "Missing Loop Start in Pipeline!");
        }
        m_loopContext = slc;
        // note that the WFM will set the tail ID so we can retrieve it
        // in the head node!
    }

    /** Informs WorkflowManager if the nodes inside the loop (= the loop
     * body) have to be reset &amp; configured inbetween iterations. Default
     * behavior is to reset/configure everytime.
     */
    protected boolean resetAndConfigureLoopBody() {
        return true;
    }

    private FlowLoopContext m_loopContext;

    final FlowLoopContext getLoopContext() {
        return m_loopContext;
    }

    /**
     * Return appropriate FlowLoopContext object depending on the type of ScopeStartNode.
     *
     * @return initial FlowLoopContext object to be put on stack.
     */
    final FlowScopeContext getInitialScopeContext() {
        if (this instanceof LoopStartNode) {
            return new FlowLoopContext();
        }
        if (this instanceof CaptureWorkflowStartNode) {
            return new FlowCaptureContext();
        }
        if (this instanceof ScopeStartNode) {
            return new FlowTryCatchContext();
        }
        return null;
    }

    final void clearLoopContext() {
        m_loopContext = null;
    }

    private boolean m_pauseAfterNextExecution = false;

    final void setPauseLoopExecution(final boolean ple) {
        m_pauseAfterNextExecution = ple;
    }

    final boolean getPauseLoopExecution() {
        return m_pauseAfterNextExecution;
    }

    private LoopEndNode m_loopEndNode = null;

    /** Access method for loop start nodes to access their respective
     * loop end. This method returns null if this node is not a loop start or
     * the loop is not correctly closed by the user.
     * @return The loop end node or null. Clients typically test and cast to
     * an expected loop end instance.
     * @see #getLoopStartNode()
     */
    protected final LoopEndNode getLoopEndNode() {
        return m_loopEndNode;
    }

    /** Setter used by framework to update loop end node.
     * @param end The end node of the loop (if this is a start node). */
    void setLoopEndNode(final LoopEndNode end) {
        m_loopEndNode = end;
    }

    private ScopeStartNode<?> m_scopeStartNode = null;

    /**
     * Access method for scope end nodes to access their respective scope start.
     *
     * @param startNodeType the expected type of the scope start node
     * @return the scope start of the given type or an empty optional if either the type doesn't match, the node is not
     *         a scope end or the scope is not correctly closed by the user
     * @since 4.2
     */
    protected final <T extends ScopeStartNode<?>> Optional<T> getScopeStartNode(final Class<T> startNodeType) {
        if (m_scopeStartNode != null && startNodeType.isAssignableFrom(m_scopeStartNode.getClass())) {
            return Optional.of(startNodeType.cast(m_scopeStartNode));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Setter used by framework to update the scope start node.
     *
     * @param start the start node of the scope (if this is a scope end node).
     */
    void setScopeStartNode(final ScopeStartNode<?> start) {
        m_scopeStartNode = start;
    }

    /** Access method for loop end nodes to access their respective loop start.
     * This method returns null if this node is not a loop end or the loop is
     * not correctly closed by the user.
     * @return The loop start node or null. Clients typically test and cast to
     * an expected loop start instance.
     * @see #getLoopEndNode()
     */
    protected final LoopStartNode getLoopStartNode() {
        return getScopeStartNode(LoopStartNode.class).orElse(null);
    }

    /** Setter used by framework to update loop start node.
     * @param start The start node of the loop (if this is a end node). */
    void setLoopStartNode(final LoopStartNode start) {
        setScopeStartNode(start);
    }

    //////////////////////////////////////////
    // Streamable support
    //////////////////////////////////////////

    /**
     * Streaming API (pending):
     * Defines properties on the input ports when used in a streamed and/or
     * distributed fashion.
     *
     * <p>
     * A data input is streamed when the node implementation only needs to see
     * each data record once (no iterative access), otherwise it's non-streamed.
     * If a port is streamed the
     * {@link StreamableOperator#runFinal(PortInput[], PortOutput[], ExecutionContext)}
     * method will provide the input as a {@link RowInput} object, to which the
     * client implementation can safely type-cast to. For non-streamed ports the
     * input is represented by an instance of {@link PortObjectInput}. Non-data
     * ports (not {@link BufferedDataTable}) are always non-streamed.
     *
     * <p>
     * An data input may be distributable (= parallelizable), in which case the
     * data is processed in paralleled (possibly scattered in the cloud).
     * Non-data ports are always non-distributable (but the execution may still
     * take place in a distributed fashion if another port is distributed -- any
     * non-distributable port is then simply duplicated as required).
     *
     * @return An array with the port role for each input port. Null elements
     *         are not allowed. The default implementation marks each input as
     *         {@link InputPortRole#NONDISTRIBUTED_NONSTREAMABLE}.
     * @since 2.6
     * @noreference This method is not intended to be referenced by clients.
     * @nooverride This method is currently not intended to be overwritten
     *             as it describes pending API.
     */
    public InputPortRole[] getInputPortRoles() {
        InputPortRole[] result = new InputPortRole[getNrInPorts()];
        Arrays.fill(result, InputPortRole.NONDISTRIBUTED_NONSTREAMABLE);
        return result;
    }

    /**
     * Streaming API (pending):
     * Similar to {@link #getInputPortRoles()} describes the role of the output.
     * An output is distributable when the (distributed!) input directly maps to
     * the output without any further merge or reduction step (which is
     * otherwise described by the {@link #createMergeOperator()}). Only data
     * outputs can be distributable, any other (model) output is always
     * non-distributable.
     *
     * The input- and output roles define the place where the output data is
     * generated:
     *
     * <ul>
     * <li>If the input data (and hence also the output data) is
     * non-distributable, the output will be generated by the
     * {@link StreamableOperator#runFinal(PortInput[], PortOutput[], ExecutionContext)}
     * method. Only one instance of the operator is used.
     * <li>If both the input and output data is distributable the output is
     * created by the
     * {@link StreamableOperator#runFinal(PortInput[], PortOutput[], ExecutionContext)}
     * method, too. Note that in this case there are several instances of a
     * {@link StreamableOperator} (either representing different threads in the
     * same JVM or distributed in a compute cluster).
     * <li>If the input is distributable but the output is non-distributable,
     * the output is created in the
     * {@link NodeModel#finishStreamableExecution(StreamableOperatorInternals, ExecutionContext, PortOutput[])}
     * implementation. The client implementation must also overwrite the
     * {@link NodeModel#createMergeOperator()} method. The implementation of
     * {@link StreamableOperator#runFinal(PortInput[], PortOutput[], ExecutionContext)}
     * must not return or push any result into the {@link PortOutput} representations.
     * </ul>
     *
     * In case a node has "mixed" outputs (e.g. a distributable data output and
     * a non-distributable model output) the output objects are produced at two
     * different locations: The data in the {@link StreamableOperator} instances
     * and the model after the merge in the NodeModel.
     *
     * @return The output roles for each of the output ports, never null.
     * The default implementation returns {@link OutputPortRole#NONDISTRIBUTED}.
     * @since 2.6
     * @noreference This method is not intended to be referenced by clients.
     * @nooverride This method is currently not intended to be overwritten
     *             as it describes pending API.
     */
    public OutputPortRole[] getOutputPortRoles() {
        OutputPortRole[] result = new OutputPortRole[getNrOutPorts()];
        Arrays.fill(result, OutputPortRole.NONDISTRIBUTED);
        return result;
    }

    // isDistributable or (isStreamable xor isBuffered)

    /**
     * Streaming API (pending):
     * Factory method for a streamable operator that is used to execute this
     * node. The default implementation returns a standard operator that wraps
     * the {@link #execute(PortObject[], ExecutionContext)} method. Subclasses
     * may override it to return a new operator that follows the data handling
     * described by the {@link #getInputPortRoles()} and
     * {@link #getOutputPortRoles()} methods.
     *
     * <p>
     * This method is called by the node executor once or multiple times
     * depending on the input roles. If any input is distributable, the method
     * is called multiple times (for each partition once), possibly on different
     * (remote) clones of this NodeModel.
     *
     * @param partitionInfo The partition info describing the chunk (if
     *            distributable).
     * @param inSpecs The port object specs of the input ports. These are
     *            identical to the specs that
     *            {@link #configure(PortObjectSpec[])} was last called with
     *            (also on the remote side).
     * @return A new operator for the (chunk) execution.
     * @throws InvalidSettingsException Usually not thrown in the client but
     *             still part of the method signature as implementations often
     *             run the same methods as during configure. (This method is not
     *             being called when configure fails.)
     * @since 2.6
     * @noreference This method is not intended to be referenced by clients.
     * @nooverride This method is currently not intended to be overwritten as it
     *             describes pending API.
     */
    public StreamableOperator createStreamableOperator(
            final PartitionInfo partitionInfo, final PortObjectSpec[] inSpecs)
        throws InvalidSettingsException {
        final int partitionIndex = partitionInfo.getPartitionIndex();
        if (partitionIndex != 0) {
            throw new IllegalStateException("Default implementation of a"
                    + "streaming execution should not be distributed (this "
                    + "appears to be partition " + partitionIndex + ")");
        }
        return new StreamableOperator() {

            @Override
            public void runFinal(final PortInput[] inputs,
                    final PortOutput[] outputs,
                    final ExecutionContext ctx) throws Exception {
                PortObject[] inObjects = new PortObject[inputs.length];
                for (int i = 0; i < inputs.length; i++) {
                    if(inputs[i] != null) {
                        inObjects[i] = ((PortObjectInput)inputs[i]).getPortObject();
                    }
                }
                // add flow variable port and remove it later from result - executeModel expects it
                PortObject[] extendedInData = ArrayUtils.add(inObjects, 0, FlowVariablePortObject.INSTANCE);
                PortObject[] extendedOutData = executeModel(extendedInData, ExecutionEnvironment.DEFAULT, ctx);
                PortObject[] outObjects = ArrayUtils.remove(extendedOutData, 0);

                for (int i = 0; i < outputs.length; i++) {
                    if (outObjects[i] != null) { //port objects happen to be null for instance at the loop end-node when the iteration is continued
                        if (outObjects[i] instanceof InactiveBranchPortObject) {
                            outputs[i].setInactive();
                        } else if (getOutPortType(i).equals(BufferedDataTable.TYPE)) {
                            ((RowOutput)outputs[i]).setFully((BufferedDataTable)outObjects[i]);
                        } else {
                            ((PortObjectOutput)outputs[i]).setPortObject(outObjects[i]);
                        }
                    }
                }
            }
        };
    }

    /**
     * Streaming API (pending):
     * Used to initialize an iterative streamable execution. If the result is
     * non-null and {@link #iterate(StreamableOperatorInternals)} returns true,
     * the object will be used to initialize {@link StreamableOperator} on the
     * remote side, which are then run and merged using the
     * {@link MergeOperator}.
     *
     * <p>
     * This method only needs to be implemented if this node needs iterative
     * access on the data (e.g. k-means clustering, which iteratively
     * synchronizes cluster prototypes between remote streamable operators).
     *
     * @return Internals used to bootstrap the streamable operator. The
     * default implementation returns <code>null</code>.
     * @since 2.6
     * @noreference This method is not intended to be referenced by clients.
     * @nooverride This method is currently not intended to be overwritten as it
     *             describes pending API.
     */
    public StreamableOperatorInternals
        createInitialStreamableOperatorInternals() {
        return null;
    }

    /**
     * Streaming API (pending):
     * Called to determine whether the node requires an(other) iteration on the
     * data before the final results are computed. If <code>true</code> the
     * argument internals are transferred to the remote side and loaded into a
     * {@link StreamableOperator} on which {@link StreamableOperator#runIntermediate(PortInput[], ExecutionContext)} is
     * called.
     *
     * <p>This implementation returns <code>false</code>.
     * @since 2.6
     * @param internals the internals. Before the first iteration it will
     * be the result of {@link #createInitialStreamableOperatorInternals()},
     * any subsequent invocation will be called with a merged internals
     * (creating using {@link MergeOperator#mergeIntermediate(StreamableOperatorInternals[])}).
     * @return If another iteration on {@link StreamableOperator#runIntermediate(PortInput[], ExecutionContext)} is to
     * be run.
     */
    public boolean iterate(final StreamableOperatorInternals internals) {
        return false;
    }

    /**
     * Streaming API (pending): Finally determines the specification of the output (tables). In most cases
     * implementations just return the result of {@link #configure(PortObjectSpec[])} here (which is also the default
     * implementation). In some cases the node needs access on the data to determine the output spec (e.g. for nodes
     * such as "Transpose" or "One-to-Many"), which is iterated as described in
     * {@link #iterate(StreamableOperatorInternals)}.
     *
     * This method is called after {@linkplain #iterate(StreamableOperatorInternals) iterate} returns <code>false</code>
     * to prepare the {@link RowOutput}. It will then call the
     * {@link StreamableOperator#runFinal( PortInput[], PortOutput[], ExecutionContext)} method to compute the final
     * output.
     *
     * @param internals The internals of the last "intermediate" merge.
     * @param inSpecs The input specs.
     * @return The specs describing the outputs that are computed by
     *         {@link StreamableOperator#runFinal( PortInput[], PortOutput[], ExecutionContext)} and
     *         {@link #finishStreamableExecution( StreamableOperatorInternals, ExecutionContext, PortOutput[])}, never
     *         null (elements). In the default implementation (i.e. for non-streamable or non-distributed nodes), this
     *         might sometimes return <code>null</code> (e.g. Transpose-node). In that case (potential) result table can
     *         only be transfered by {@link RowOutput#setFully(BufferedDataTable)} to the respective RowOutput. The
     *         implementation of the RowOutput passed in the
     *         {@link StreamableOperator#runFinal(PortInput[], PortOutput[], ExecutionContext)} has to take care of that
     *         in order to comply with the streaming-API default implementation.
     * @throws InvalidSettingsException As described in the configure method.
     * @since 2.6
     */
    public PortObjectSpec[] computeFinalOutputSpecs(final StreamableOperatorInternals internals,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return configure(inSpecs);
    }

    /**
     * Streaming API (pending):
     * Factory method to create a merge operator that combines results created
     * in different {@link StreamableOperator} objects. This method must be
     * overwritten if the input is distributable but the output is not
     * (as it needs to prepare the final output that is then published by the
     * {@link #finishStreamableExecution(StreamableOperatorInternals,
     * ExecutionContext, PortOutput[])} method). It may be overwritten if
     * the output is distributable but some work needs to be done after
     * all operators have finished (e.g. setting some internals or
     * warning message).
     *
     * <p>
     * The default implementation returns <code>null</code> because input and
     * output are non distributable.
     *
     * @return A new merge operator or <code>null</code>.
     * @since 2.6
     * @noreference This method is not intended to be referenced by clients.
     * @nooverride This method is currently not intended to be overwritten as it
     *             describes pending API.
     */
    public MergeOperator createMergeOperator() {
        return null;
    }

    /**
     * Streaming API (pending):
     * Called by the executor if the data is processed in a distributed fashion
     * to create the final output result or update node internals (for instance
     * a warning message or view content). This method is called on the local
     * side. See also the API description for {@link #getOutputPortRoles()}.
     *
     * @param internals The merged internals of the streamable operators that
     *            processed the data. The internals object is created by one or
     *            multiple {@link MergeOperator} (as created by
     *            {@link #createMergeOperator()}).
     *
     * @param exec For progress reporting, cancelation, output creation.
     * @param output The array of the output representations. This method must
     *            only write to the slots that it is responsible for
     *            (non-distributed output).
     * @throws Exception Any exception to indicate an error, including
     *             cancelation.
     * @since 2.6
     * @noreference This method is not intended to be referenced by clients.
     * @nooverride This method is currently not intended to be overwritten as it
     *             describes pending API.
     */
    public void finishStreamableExecution(
            final StreamableOperatorInternals internals,
            final ExecutionContext exec,
            final PortOutput[] output) throws Exception {
        throw new IllegalStateException("Method must be implemented as a"
                + " merge operator was created.");
    }

    /**
     * Returns the logger for this node.
     *
     * @return a node logger
     * @since 2.8
     */
    protected final NodeLogger getLogger() {
        return m_logger;
    }
}
