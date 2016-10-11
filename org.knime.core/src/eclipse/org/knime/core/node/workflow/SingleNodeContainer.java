/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.api.node.workflow.ISingleNodeContainer;
import org.knime.core.api.node.workflow.WorkflowCopyContent;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeConfigureHelper;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.ConfigEditTreeModel;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.workflow.FlowVariable.Scope;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.execresult.NodeContainerExecutionStatus;
import org.w3c.dom.Element;

/**
 * Implementation of {@link NodeContainer} which wraps a node hiding it's
 * internals, such as a node with an underlying implementation or a subnode
 * hiding it's internal workflow.
 *
 * @author M. Berthold/B. Wiswedel, University of Konstanz
 */
public abstract class SingleNodeContainer extends NodeContainer implements ISingleNodeContainer {

    /** my logger. */
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(SingleNodeContainer.class);

    /**
     * Available policy how to handle output data. It might be held in memory or
     * completely on disc. We use an enum here as a boolean may not be
     * sufficient in the future (possibly adding a third option "try to keep in
     * memory").
     */
    public static enum MemoryPolicy {
        /** Hold output in memory. */
        CacheInMemory,
        /**
         * Cache only small tables in memory, i.e. with cell count &lt;= DataContainer.MAX_CELLS_IN_MEMORY.
         */
        CacheSmallInMemory,
        /** Buffer on disc. */
        CacheOnDisc
    }

    /** Config key: What memory policy to use for a node outport. */
    static final String CFG_MEMORY_POLICY = "memory_policy";
    /** The sub settings entry where the model can save its setup. */
    static final String CFG_MODEL = "model";
    /** The sub settings entry containing the flow variable settings. These
     * settings are not available in the derived node model. */
    static final String CFG_VARIABLES = "variables";

    /** Name of the sub-directory containing node-local files. These files
     * manually copied by the user and the node will automatically list those
     * files as node-local flow variables in its configuration dialog.
     */
    public static final String DROP_DIR_NAME = "drop";

    private SingleNodeContainerSettings m_settings =
        new SingleNodeContainerSettings();

    /**
     * @param parent ...
     * @param id ...
     */
    SingleNodeContainer(final WorkflowManager parent, final NodeID id) {
        super(parent, id);
    }


    /**
     * @param parent ...
     * @param id ...
     * @param anno ...
     */
    SingleNodeContainer(final WorkflowManager parent, final NodeID id, final NodeAnnotation anno) {
        super(parent, id, anno);
    }


    /**
     * @param parent ...
     * @param id ...
     * @param persistor ...
     */
    SingleNodeContainer(final WorkflowManager parent, final NodeID id, final NodeContainerMetaPersistor persistor) {
        super(parent, id, persistor);
    }

    /* ------------------ Views ---------------- */

    /**
     * Set a new HiLiteHandler for an incoming connection.
     * @param index index of port
     * @param hdl new HiLiteHandler
     */
    abstract void setInHiLiteHandler(final int index, final HiLiteHandler hdl);

    /**
     * @return ...
     */
    public abstract ExecutionContext createExecutionContext();

    // ////////////////////////////////
    // Handle State Transitions
    // ////////////////////////////////

    /**
     * Configure underlying node and update state accordingly.
     *
     * @param inObjectSpecs input table specifications
     * @param keepNodeMessage If true the previous message is kept (unless an error needs to be shown)
     * @return true if output specs have changed.
     * @throws IllegalStateException in case of illegal entry state.
     */
    final boolean configure(final PortObjectSpec[] inObjectSpecs, final boolean keepNodeMessage) {
        synchronized (m_nodeMutex) {
            final NodeMessage oldMessage = getNodeMessage();
            // remember old specs
            PortObjectSpec[] prevSpecs = new PortObjectSpec[getNrOutPorts()];
            for (int i = 0; i < prevSpecs.length; i++) {
                prevSpecs[i] = getOutPort(i).getPortObjectSpec();
            }
            boolean prevInactivity = isInactive();
            // perform action
            switch (getInternalState()) {
            case IDLE:
                if (callNodeConfigure(inObjectSpecs, keepNodeMessage)) {
                    setInternalState(InternalNodeContainerState.CONFIGURED);
                } else {
                    setInternalState(InternalNodeContainerState.IDLE);
                }
                break;
            case UNCONFIGURED_MARKEDFOREXEC:
                if (callNodeConfigure(inObjectSpecs, keepNodeMessage)) {
                    setInternalState(InternalNodeContainerState.CONFIGURED_MARKEDFOREXEC);
                } else {
                    setInternalState(InternalNodeContainerState.UNCONFIGURED_MARKEDFOREXEC);
                }
                break;
            case CONFIGURED:
                // m_node.reset();
                boolean success = callNodeConfigure(inObjectSpecs, keepNodeMessage);
                if (success) {
                    setInternalState(InternalNodeContainerState.CONFIGURED);
                } else {
                    // m_node.reset();
                    setInternalState(InternalNodeContainerState.IDLE);
                }
                break;
            case CONFIGURED_MARKEDFOREXEC:
                // these are dangerous - otherwise re-queued loop-ends are
                // reset!
                // m_node.reset();
                success = callNodeConfigure(inObjectSpecs, keepNodeMessage);
                if (success) {
                    setInternalState(InternalNodeContainerState.CONFIGURED_MARKEDFOREXEC);
                } else {
                    // m_node.reset();
                    setInternalState(InternalNodeContainerState.UNCONFIGURED_MARKEDFOREXEC);
                }
                break;
            case EXECUTINGREMOTELY: // this should only happen during load
                success = callNodeConfigure(inObjectSpecs, keepNodeMessage);
                if (!success) {
                    setInternalState(InternalNodeContainerState.IDLE);
                }
                break;
            default:
                throwIllegalStateException();
            }
            if (keepNodeMessage) {
                setNodeMessage(NodeMessage.merge(oldMessage, getNodeMessage()));
            }
            // if state stayed the same but inactivity of node changed
            // fake a state change to make sure it's displayed properly
            if (prevInactivity != isInactive()) {
                InternalNodeContainerState oldSt = this.getInternalState();
                setInternalState(InternalNodeContainerState.IDLE.equals(oldSt) ? InternalNodeContainerState.CONFIGURED
                        : InternalNodeContainerState.IDLE);
                setInternalState(oldSt);
                return true;
            }
            // compare old and new specs
            for (int i = 0; i < prevSpecs.length; i++) {
                PortObjectSpec newSpec = getOutPort(i).getPortObjectSpec();
                if (newSpec != null) {
                    if (!newSpec.equals(prevSpecs[i])) {
                        return true;
                    }
                } else if (prevSpecs[i] != null) {
                    return true; // newSpec is null!
                }
            }
            return false; // all specs & inactivity stayed the same!
        }
    }

    /**
     * Calls configure in the node, whereby it also updates the settings in case the node is driven by flow variables.
     * It also allows the current job manager to modify the output specs according to its  settings
     * (in case it modifies the node's output).
     *
     * <p>This method is KNIME private API and is called from the framework and the streaming executor (which is why
     * it has public scope).
     *
     * @param inSpecs the input specs to node configure
     * @param keepNodeMessage see {@link SingleNodeContainer#configure(PortObjectSpec[], boolean)}
     * @return true of configure succeeded.
     * @since 2.12
     * @noreference This method is not intended to be referenced by clients.
     */
    public final boolean callNodeConfigure(final PortObjectSpec[] inSpecs, final boolean keepNodeMessage) {
        final NodeExecutionJobManager jobMgr = findJobManager();
        NodeConfigureHelper nch = new NodeConfigureHelper() {

            private Map<String, FlowVariable> m_exportedSettingsVariables;

            /** {@inheritDoc} */
            @Override
            public void preConfigure() throws InvalidSettingsException {
                m_exportedSettingsVariables = applySettingsUsingFlowObjectStack();
            }

            /** {inheritDoc} */
            @Override
            public PortObjectSpec[] postConfigure(final PortObjectSpec[] inObjSpecs,
                final PortObjectSpec[] nodeModelOutSpecs) throws InvalidSettingsException {
                if (!m_exportedSettingsVariables.isEmpty()) {
                    FlowObjectStack outgoingFlowObjectStack = getOutgoingFlowObjectStack();
                    ArrayList<FlowVariable> reverseOrder =
                            new ArrayList<FlowVariable>(m_exportedSettingsVariables.values());
                    Collections.reverse(reverseOrder);
                    for (FlowVariable v : reverseOrder) {
                        outgoingFlowObjectStack.push(v);
                    }
                }
                return jobMgr.configure(inObjSpecs, nodeModelOutSpecs);
            }
        };
        NodeContext.pushContext(this);
        try {
            return performConfigure(inSpecs, nch, keepNodeMessage);
        } finally {
            NodeContext.removeLastContext();
        }
    }

    /** All preparations done: pass configure down to derived classes.
     *
     * @param inSpecs ...
     * @param nch ...
     * @param keepNodeMessage as per {@link SingleNodeContainer#configure(PortObjectSpec[], boolean)}
     *        (sub nodes need this to forward the property to the contained wfm).
     * @return true if configure succeeded.
     */
    abstract boolean performConfigure(final PortObjectSpec[] inSpecs, final NodeConfigureHelper nch,
        final boolean keepNodeMessage);

    /** Used before configure, to apply the variable mask to the nodesettings,
     * that is to change individual node settings to reflect the current values
     * of the variables (if any).
     * @return a map containing the exposed variables (which are visible to
     * downstream nodes. These variables are put onto the node's
     * {@link FlowObjectStack}.
     */
    private Map<String, FlowVariable> applySettingsUsingFlowObjectStack() throws InvalidSettingsException {
        NodeSettingsRO variablesSettings = m_settings.getVariablesSettings();
        if (variablesSettings == null) {
            return Collections.emptyMap();
        }
        NodeSettings fromModel = m_settings.getModelSettingsClone();
        ConfigEditTreeModel configEditor;
        try {
            configEditor = ConfigEditTreeModel.create(fromModel, variablesSettings);
        } catch (final InvalidSettingsException e) {
            throw new InvalidSettingsException("Errors reading flow variables: " + e.getMessage(), e);
        }
        Map<String, FlowVariable> flowVariablesMap = getFlowObjectStack().getAvailableFlowVariables();
        List<FlowVariable> newVariableList;
        try {
            newVariableList = configEditor.overwriteSettings(fromModel, flowVariablesMap);
        } catch (InvalidSettingsException e) {
            throw new InvalidSettingsException("Errors overwriting node settings with flow variables: "
                + e.getMessage(), e);
        }

        NodeContext.pushContext(this);
        try {
            performValidateSettings(fromModel);
            performLoadModelSettingsFrom(fromModel);
        } catch (InvalidSettingsException e) {
            throw new InvalidSettingsException("Errors loading flow variables into node : " + e.getMessage(), e);
        } finally {
            NodeContext.removeLastContext();
        }
        Map<String, FlowVariable> newVariableHash = new LinkedHashMap<String, FlowVariable>();
        for (FlowVariable v : newVariableList) {
            if (newVariableHash.put(v.getName(), v) != null) {
                LOGGER.warn("Duplicate variable assignment for key \"" + v.getName() + "\")");
            }
        }
        return newVariableHash;
    }

    /** Load cleaned and "variable adjusted" into underlying implementation.  Throws exception
     * if validation of settings fails or other problems occur.
     *
     * @param settings ...
     * @throws InvalidSettingsException ...
     */
    abstract void performLoadModelSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Check if node can be safely reset.
     *
     * @return if node can be reset.
     * @since 3.2
     */
    @Override
    public boolean isResetable() {
        if (getNodeLocks().hasResetLock()) {
            return false;
        } else {
            switch (getInternalState()) {
                case EXECUTED:
                case EXECUTED_MARKEDFOREXEC:
                case CONFIGURED_MARKEDFOREXEC:
                case UNCONFIGURED_MARKEDFOREXEC:
                case CONFIGURED:
                    return true;
                default:
                    return false;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    boolean canPerformReset() {
        if (getNodeLocks().hasResetLock()) {
            return false;
        } else {
            synchronized (m_nodeMutex) {
                switch (getInternalState()) {
                    case EXECUTED:
                        return true;
                    default:
                        return false;
                }
            }
        }
    }

    /** Reset underlying node and update state accordingly.
     * @throws IllegalStateException in case of illegal entry state.
     */
    void rawReset() {
        // TODO move copies into Native/SubNodecontainer?
        synchronized (m_nodeMutex) {
            switch (getInternalState()) {
            case EXECUTED:
            case EXECUTED_MARKEDFOREXEC:
                NodeContext.pushContext(this);
                try {
                    performReset();
                } finally {
                    NodeContext.removeLastContext();
                }
                if (this instanceof NativeNodeContainer) {
                    ((NativeNodeContainer)this).clearFileStoreHandler();
                }
                // After reset we need explicit configure!
                setInternalState(InternalNodeContainerState.IDLE);
                return;
            case CONFIGURED_MARKEDFOREXEC:
                setInternalState(InternalNodeContainerState.CONFIGURED);
                return;
            case UNCONFIGURED_MARKEDFOREXEC:
                setInternalState(InternalNodeContainerState.IDLE);
                return;
            case CONFIGURED:
                /*
                 * Also configured nodes must be reset in order to handle
                 * nodes subsequent to metanodes with through-connections.
                 */
                NodeContext.pushContext(this);
                try {
                    performReset();
                } finally {
                    NodeContext.removeLastContext();
                }
                if (this instanceof NativeNodeContainer) {
                    ((NativeNodeContainer)this).clearFileStoreHandler();
                }
                setInternalState(InternalNodeContainerState.IDLE);
                return;
            default:
                if (isResetable()) { // a subnode container is resetable even when IDLE
                    NodeContext.pushContext(this);
                    try {
                        performReset();
                    } finally {
                        NodeContext.removeLastContext();
                    }
                } else  {
                    throwIllegalStateException();
                }
            }
        }
    }

    /** Reset model in underlying implementation.
     */
    abstract void performReset();

    /** Cleans outports, i.e. sets fields to null, calls clear() on BDT.
     * Usually happens as part of a reset() (except for loops that have
     * their body not reset between iterations.
     * @param isLoopRestart See {@link Node#cleanOutPorts(boolean)}. */
    abstract void cleanOutPorts(final boolean isLoopRestart);

    /** Enable (or disable) queuing of underlying node for execution. This
     * really only changes the state of the node. If flag==true and when all
     * pre-conditions for execution are fulfilled (e.g. configuration succeeded
     * and all ingoing objects are available) the node will be actually queued.
     *
     * @param flag determines if node is marked or unmarked for execution
     * @throws IllegalStateException in case of illegal entry state.
     */
    @Override
    void markForExecution(final boolean flag) {
        synchronized (m_nodeMutex) {
            if (flag) {  // we want to mark the node for execution!
                switch (getInternalState()) {
                case CONFIGURED:
                    setInternalState(InternalNodeContainerState.CONFIGURED_MARKEDFOREXEC);
                    break;
                case IDLE:
                    setInternalState(InternalNodeContainerState.UNCONFIGURED_MARKEDFOREXEC);
                    break;
                default:
                    throwIllegalStateException();
                }
                setExecutionEnvironment(new ExecutionEnvironment());
            } else {  // we want to remove the mark for execution
                switch (getInternalState()) {
                case CONFIGURED_MARKEDFOREXEC:
                    setInternalState(InternalNodeContainerState.CONFIGURED);
                    break;
                case UNCONFIGURED_MARKEDFOREXEC:
                    setInternalState(InternalNodeContainerState.IDLE);
                    break;
                default:
                    throwIllegalStateException();
                }
                setExecutionEnvironment(null);
            }
        }
    }

    /**
     * Mark underlying, executed node so that it can be re-executed (= update state accordingly).
     * - Used in loops to execute start more than once and when reset/configure is skipped in loop body.
     * - Used for re-execution of interactive nodes.
     *
     * @param exEnv the execution environment
     * @throws IllegalStateException in case of illegal entry state.
     */
    void markForReExecution(final ExecutionEnvironment exEnv) {
        assert exEnv.reExecute();
        synchronized (m_nodeMutex) {
            switch (getInternalState()) {
            case EXECUTED:
                setInternalState(InternalNodeContainerState.EXECUTED_MARKEDFOREXEC);
                break;
            default:
                throwIllegalStateException();
            }
            setExecutionEnvironment(exEnv);
        }
    }

    /** {@inheritDoc} */
    @Override
    void cancelExecution() {
        synchronized (m_nodeMutex) {
            switch (getInternalState()) {
            case UNCONFIGURED_MARKEDFOREXEC:
                setInternalState(InternalNodeContainerState.IDLE);
                break;
            case CONFIGURED_MARKEDFOREXEC:
                setInternalState(InternalNodeContainerState.CONFIGURED);
                break;
            case EXECUTED_MARKEDFOREXEC:
                setInternalState(InternalNodeContainerState.EXECUTED);
                break;
            case CONFIGURED_QUEUED:
            case EXECUTED_QUEUED:
                // m_executionFuture has not yet started or if it has started,
                // it will not hand of to node implementation (otherwise it
                // would be executing)
                getProgressMonitor().setExecuteCanceled();
                NodeExecutionJob job = getExecutionJob();
                assert job != null : "node is queued but no job represents the execution task (is null)";
                job.cancel();
                if (InternalNodeContainerState.CONFIGURED_QUEUED.equals(getInternalState())) {
                    setInternalState(InternalNodeContainerState.CONFIGURED);
                } else {
                    setInternalState(InternalNodeContainerState.EXECUTED);
                }
                break;
            case EXECUTING:
                // future is running in thread pool, use ordinary cancel policy
                getProgressMonitor().setExecuteCanceled();
                job = getExecutionJob();
                assert job != null : "node is executing but no job represents the execution task (is null)";
                job.cancel();
                break;
            case PREEXECUTE:   // locally executing nodes are not really in
            case POSTEXECUTE:  // one of these two states
            case EXECUTINGREMOTELY:
                // execute remotely can be both truly executing remotely
                // (job will be non-null) or marked as executing remotely
                // (e.g. node is part of metanode which is remote executed
                // -- the job will be null). We tolerate both cases here.
                job = getExecutionJob();
                if (job == null) {
                    // we can't decide on whether this node is now IDLE or
                    // CONFIGURED -- we rely on the parent to call configure
                    // (pessimistic guess here that node was not configured)
                    setInternalState(InternalNodeContainerState.IDLE);
                } else {
                    getProgressMonitor().setExecuteCanceled();
                    job.cancel();
                }
                break;
            case EXECUTED:
                // Too late - do nothing and bail.
                return;
            default:
                // warn, ignore, and bail.
                LOGGER.warn("Strange state " + getInternalState() + " encountered in cancelExecution().");
                return;
            }
            // clean up execution environment.
            setExecutionEnvironment(null);
        }
    }

    /** {@inheritDoc} */
    @Override
    void performShutdown() {
        synchronized (m_nodeMutex) {
            NodeExecutionJob job = getExecutionJob();
            if (job != null && job.isSavedForDisconnect()) {
                assert getInternalState().isExecutionInProgress();
                findJobManager().disconnect(job);
            } else if (getInternalState().isExecutionInProgress()) {
                cancelExecution();
            }
            super.performShutdown();
        }
    }


    //////////////////////////////////////
    //  internal state change actions
    //////////////////////////////////////

    /** {@inheritDoc} */
    @Override
    void mimicRemotePreExecute() {
        synchronized (m_nodeMutex) {
            getProgressMonitor().reset();
            switch (getInternalState()) {
            case EXECUTED_MARKEDFOREXEC:
            case CONFIGURED_MARKEDFOREXEC:
            case UNCONFIGURED_MARKEDFOREXEC:
                setInternalState(InternalNodeContainerState.PREEXECUTE);
                break;
            case EXECUTED:
                // ignore executed nodes
                break;
            default:
                throwIllegalStateException();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    void mimicRemoteExecuting() {
        synchronized (m_nodeMutex) {
            switch (getInternalState()) {
            case PREEXECUTE:
                setInternalState(InternalNodeContainerState.EXECUTINGREMOTELY);
                break;
            case EXECUTED:
                // ignore executed nodes
                break;
            default:
                throwIllegalStateException();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    void mimicRemotePostExecute() {
        synchronized (m_nodeMutex) {
            switch (getInternalState()) {
            case PREEXECUTE: // in case of errors, e.g. flow stack problems
                             // encountered during doBeforeExecution
            case EXECUTINGREMOTELY:
                setInternalState(InternalNodeContainerState.POSTEXECUTE);
                break;
            case EXECUTED:
                // ignore executed nodes
                break;
            default:
                throwIllegalStateException();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    void mimicRemoteExecuted(final NodeContainerExecutionStatus status) {
        boolean success = status.isSuccess();
        synchronized (m_nodeMutex) {
            switch (getInternalState()) {
            case POSTEXECUTE:
                setInternalState(success ? InternalNodeContainerState.EXECUTED : InternalNodeContainerState.IDLE);
                break;
            case EXECUTED:
                // ignore executed nodes
                break;
            default:
                throwIllegalStateException();
            }
        }
    }

    /**
     * Execute underlying Node asynchronously. Make sure to give Workflow-
     * Manager a chance to call pre- and postExecuteNode() appropriately and
     * synchronize those parts (since they changes states!).
     *
     * @param inObjects input data
     * @return whether execution was successful.
     * @throws IllegalStateException in case of illegal entry state.
     */
    public abstract NodeContainerExecutionStatus performExecuteNode(final PortObject[] inObjects);


    // //////////////////////////////////////
    // Save & Load Settings and Content
    // //////////////////////////////////////

    /** {@inheritDoc} */
    @Override
    void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        synchronized (m_nodeMutex) {
            super.loadSettings(settings);
            // assign to temp variable first, then load (which may fail during validation), then assign to class member
            SingleNodeContainerSettings tempSettings = new SingleNodeContainerSettings(settings);
            NodeContext.pushContext(this);
            try {
                final NodeSettingsRO modelSettings = tempSettings.getModelSettings();
                if (modelSettings != null) {
                    performLoadModelSettingsFrom(modelSettings);
                }
            } finally {
                NodeContext.removeLastContext();
            }
            m_settings = tempSettings;
            setDirty();
        }
    }

    /** {@inheritDoc} */
    @Override
    WorkflowCopyContent loadContent(final NodeContainerPersistor nodePersistor,
            final Map<Integer, BufferedDataTable> tblRep,
            final FlowObjectStack inStack, final ExecutionMonitor exec,
            final LoadResult loadResult, final boolean preserveNodeMessage)
            throws CanceledExecutionException {
        synchronized (m_nodeMutex) {
            if (!(nodePersistor instanceof SingleNodeContainerPersistor)) {
                throw new IllegalStateException("Expected "
                        + SingleNodeContainerPersistor.class.getSimpleName()
                        + " persistor object, got "
                        + nodePersistor.getClass().getSimpleName());
            }
            exec.checkCanceled();
            SingleNodeContainerPersistor persistor = (SingleNodeContainerPersistor)nodePersistor;
            InternalNodeContainerState state = persistor.getMetaPersistor().getState();
            setInternalState(state, false);
            final FlowObjectStack outgoingStack = new FlowObjectStack(getID());
            for (FlowObject s : persistor.getFlowObjects()) {
                outgoingStack.push(s);
            }
            setFlowObjectStack(inStack, outgoingStack);
            SingleNodeContainerSettings sncSettings = persistor.getSNCSettings();
            if (sncSettings == null) {
                LOGGER.coding("SNC settings from persistor are null, using default");
                sncSettings = new SingleNodeContainerSettings();
            }
            m_settings = sncSettings;
            WorkflowCopyContent result = performLoadContent(
                persistor, tblRep, inStack, exec, loadResult, preserveNodeMessage);
            return result;
        }
    }

    /** Called by {@link #loadContent(NodeContainerPersistor, Map, FlowObjectStack, ExecutionMonitor,
     * LoadResult, boolean)} to allow subclasses to load their content (heavy for subnode).
     * @param nodePersistor ...
     * @param tblRep ...
     * @param inStack ...
     * @param exec ...
     * @param loadResult ..
     * @param preserveNodeMessage ...
     * @return ...
     * @throws CanceledExecutionException ...
     */
    abstract WorkflowCopyContent performLoadContent(final SingleNodeContainerPersistor nodePersistor,
            final Map<Integer, BufferedDataTable> tblRep,
            final FlowObjectStack inStack, final ExecutionMonitor exec,
            final LoadResult loadResult, final boolean preserveNodeMessage) throws CanceledExecutionException;


    /** {@inheritDoc} */
    @Override
    void saveSettings(final NodeSettingsWO settings) {
        saveSettings(settings, false);
    }

    /** Saves config from super NodeContainer (job manager) and the model settings and the variable settings.
     * @param settings To save to.
     * @param initDefaultModelSettings If true and the model settings are not yet assigned (node freshly dragged onto
     * workflow) the NodeModel's saveSettings method is called to init fallback settings.
     */
    void saveSettings(final NodeSettingsWO settings, final boolean initDefaultModelSettings) {
        super.saveSettings(settings);
        saveSNCSettings(settings, initDefaultModelSettings);
    }

    /** Implementation of {@link WorkflowManager#saveNodeSettingsToDefault(NodeID)}. */
    void saveNodeSettingsToDefault() {
        NodeSettings modelSettings = new NodeSettings("model");
        NodeContext.pushContext(this);
        try {
            performSaveModelSettingsTo(modelSettings);
        } finally {
            NodeContext.removeLastContext();
        }
        m_settings.setModelSettings(modelSettings);
    }

    /** Save settings of specific implementation.
     *
     * @param modelSettings ...
     */
    abstract void performSaveModelSettingsTo(final NodeSettings modelSettings);

    /**
     * Saves the {@link SingleNodeContainerSettings}.
     * @param settings To save to.
     * @param initDefaultModelSettings If true and the model settings are not yet assigned (node freshly dragged onto
     * workflow) the NodeModel's saveSettings method is called to init fallback settings.
     */
    void saveSNCSettings(final NodeSettingsWO settings, final boolean initDefaultModelSettings) {
        SingleNodeContainerSettings sncSettings = m_settings;
        if (initDefaultModelSettings && m_settings.getModelSettings() == null) {
            sncSettings = m_settings.clone();
            NodeSettings modelSettings = new NodeSettings("model");
            NodeContext.pushContext(this);
            try {
                performSaveModelSettingsTo(modelSettings);
            } finally {
                NodeContext.removeLastContext();
            }
            sncSettings.setModelSettings(modelSettings);
        }
        sncSettings.save(settings);
    }

    /** @return reference to internally used settings (contains information for
     * memory policy, e.g.) */
    SingleNodeContainerSettings getSingleNodeContainerSettings() {
        return m_settings;
    }

    /** {@inheritDoc} */
    @Override
    void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        final SingleNodeContainerSettings sncSettings = new SingleNodeContainerSettings(settings);
        NodeSettingsRO modelSettings = sncSettings.getModelSettings();
        if (modelSettings == null) {
            modelSettings = new NodeSettings("empty");
        }
        NodeContext.pushContext(this);
        try {
            performValidateSettings(modelSettings);
        } finally {
            NodeContext.removeLastContext();
        }
    }

    /** Validate settings of specific implementation.
     * @param modelSettings ...
     * @throws InvalidSettingsException ...
     */
   abstract void performValidateSettings(final NodeSettingsRO modelSettings) throws InvalidSettingsException;


    ////////////////////////////////////
    // Credentials handling
    ////////////////////////////////////

    /** {@inheritDoc} */
    @Override
    public boolean areDialogAndNodeSettingsEqual() {
        final String key = "snc_settings";
        NodeSettingsWO nodeSettings = new NodeSettings(key);
        saveSettings(nodeSettings, true);
        NodeSettingsWO dlgSettings = new NodeSettings(key);
        NodeContext.pushContext(this);
        try {
            getDialogPane().finishEditingAndSaveSettingsTo(dlgSettings);
        } catch (InvalidSettingsException e) {
            return false;
        } finally {
            NodeContext.removeLastContext();
        }
        return dlgSettings.equals(nodeSettings);
    }


    /** Set credentials store on this node. It will clear usage history to
     * previously accessed credentials (client usage in credentials, see
     * {@link Credentials}) and set a new provider on the underlying node, which
     * has this node as client.
     * @param store The new store to set.
     * @see Node#setCredentialsProvider(CredentialsProvider)
     * @throws NullPointerException If the argument is null.
     */
    void setCredentialsStore(final CredentialsStore store) {
        CredentialsProvider oldProvider = getCredentialsProvider();
        if (oldProvider != null) {
            oldProvider.clearClientHistory();
            if (oldProvider.getClient() == this && oldProvider.getStore() == store) {
                // no change
                return;
            }
        }
        performSetCredentialsProvider(new CredentialsProvider(this, store));
    }

    /** Set/overwrite CredentialsStore.
     *
     * @param cp ...
     */
    abstract void performSetCredentialsProvider(final CredentialsProvider cp);

    /**
     * @return CredentialsProvider
     */
    abstract CredentialsProvider getCredentialsProvider();

    ////////////////////////////////////
    // FlowObjectStack handling
    ////////////////////////////////////

    /**
     * Set {@link FlowObjectStack}.
     *
     * @param st new stack
     * @param outgoingStack a node-local stack containing the items that
     *        were pushed by the node (this stack will be empty unless this
     *        node is a loop start node)
     */
    abstract void setFlowObjectStack(final FlowObjectStack st, final FlowObjectStack outgoingStack);

    /** Delegates to node to get flow variables that are added or modified
     * by the node.
     * @return The list of outgoing flow variables.
     * @see org.knime.core.node.Node#getOutgoingFlowObjectStack()
     */
    public abstract FlowObjectStack getOutgoingFlowObjectStack();

    /** Check if the given node is part of a scope (loop, try/catch...).
     *
     * @return true if node is part of a scope context.
     * @since 2.8
     */
    public boolean isMemberOfScope() {
        synchronized (m_nodeMutex) {
            // we need to check if either a FlowScopeObject is on the stack or of
            // the node is the end of a Scope (since those don't have their own
            // scope object on the outgoing stack anymore.
            FlowObjectStack fos = getFlowObjectStack();
            if (fos == null) {
                return false;
            }
            return (this.isModelCompatibleTo(ScopeEndNode.class)
                    || this.isModelCompatibleTo(ScopeStartNode.class)
                    || (null != fos.peek(FlowScopeContext.class)));
        }
    }

    /** Creates a copy of the stack held by the Node and modifies the copy
     * by pushing all outgoing flow variables onto it. If the node represents
     * a scope end node, it will also pop the corresponding scope context
     * (and thereby all variables added in the scope's body).
     *
     * @return Such a (new!) stack. */
    public FlowObjectStack createOutFlowObjectStack() {
        synchronized (m_nodeMutex) {
            FlowObjectStack st = getFlowObjectStack();
            FlowObjectStack finalStack = new FlowObjectStack(getID(), st);
            if (this.isModelCompatibleTo(ScopeEndNode.class)) {
                finalStack.pop(FlowScopeContext.class);
            }
            FlowObjectStack outgoingStack = getOutgoingFlowObjectStack();
            List<FlowObject> flowObjectsOwnedByThis;
            if (outgoingStack == null) { // not configured -> no stack
                flowObjectsOwnedByThis = Collections.emptyList();
            } else {
                flowObjectsOwnedByThis = outgoingStack.getFlowObjectsOwnedBy(getID(), Scope.Local);
            }
            for (FlowObject v : flowObjectsOwnedByThis) {
                finalStack.push(v);
            }
            return finalStack;
        }
    }

    /**
     * @param nodeModelClass ...
     * @return return true if underlying NodeModel (if it exists) implements the given class/interface
     * @since 2.8
     */
    public abstract boolean isModelCompatibleTo(final Class<?> nodeModelClass);



    /**
     * @return true if configure or execute were skipped because node is
     *   part of an inactive branch.
     * @see Node#isInactive()
     */
    public abstract boolean isInactive();

    /** @return <code>true</code> if the underlying node is able to consume
     * inactive objects (implements
     * {@link org.knime.core.node.port.inactive.InactiveBranchConsumer}).
     * @see Node#isInactiveBranchConsumer()
     */
    public abstract boolean isInactiveBranchConsumer();

    /**
     * @return the XML description of the node for the NodeDescription view
     */
    public abstract Element getXMLDescription();

    /** {@inheritDoc} */
    @Override
    protected final boolean isLocalWFM() {
        return false;
    }


    // /////////////////////////////////////////////////////////////////////
    // Settings loading and saving (single node container settings only)
    // /////////////////////////////////////////////////////////////////////

    /**
     * Handles the settings specific to a SingleNodeContainer. Reads and writes
     * them from and into a NodeSettings object.
     */
    public static final class SingleNodeContainerSettings implements Cloneable {

        private MemoryPolicy m_memoryPolicy = MemoryPolicy.CacheSmallInMemory;
        private NodeSettingsRO m_modelSettings;
        private NodeSettingsRO m_variablesSettings;

        /**
         * Creates a settings object with default values.
         */
        public SingleNodeContainerSettings() {
        }

        /**
         * Creates a new instance holding the settings contained in the
         * specified object. The settings object must be one this class has
         * saved itself into (and not a job manager settings object).
         *
         * @param settings the object with the settings to read
         * @throws InvalidSettingsException if the settings in the argument are
         *             invalid
         */
        public SingleNodeContainerSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
            NodeSettingsRO sncSettings = settings.getNodeSettings(Node.CFG_MISC_SETTINGS);
            if (sncSettings.containsKey(CFG_MEMORY_POLICY)) {
                String memPolStr = sncSettings.getString(CFG_MEMORY_POLICY);
                try {
                    m_memoryPolicy = MemoryPolicy.valueOf(memPolStr);
                } catch (IllegalArgumentException iae) {
                    throw new InvalidSettingsException("Invalid memory policy: " + memPolStr);
                }
            }
            // in versions before KNIME 1.2.0, there were no misc settings
            // in the dialog, we must use caution here: if they are not present
            // we use the default.
            if (settings.containsKey(CFG_VARIABLES)) {
                m_variablesSettings = settings.getNodeSettings(CFG_VARIABLES);
            } else {
                m_variablesSettings = null;
            }
            m_modelSettings = settings.getNodeSettings(CFG_MODEL);
        }

        /**
         * Writes the current settings values into the passed argument.
         *
         * @param settings the object to write the settings into.
         */
        public void save(final NodeSettingsWO settings) {
            NodeSettingsWO sncSettings = settings.addNodeSettings(Node.CFG_MISC_SETTINGS);
            sncSettings.addString(CFG_MEMORY_POLICY, m_memoryPolicy.name());
            if (m_modelSettings != null) {
                NodeSettingsWO model = settings.addNodeSettings(CFG_MODEL);
                m_modelSettings.copyTo(model);
            }
            if (m_variablesSettings != null) {
                NodeSettingsWO variables = settings.addNodeSettings(CFG_VARIABLES);
                m_variablesSettings.copyTo(variables);
            }

        }

        /**
         * Store a new memory policy in this settings object.
         *
         * @param memPolicy the new policy to set
         */
        public void setMemoryPolicy(final MemoryPolicy memPolicy) {
            m_memoryPolicy = memPolicy;
        }

        /**
         * Returns the memory policy currently stored in this settings object.
         *
         * @return the memory policy currently stored in this settings object.
         */
        public MemoryPolicy getMemoryPolicy() {
            return m_memoryPolicy;
        }

        /**
         * @return the modelSettings
         */
        public NodeSettings getModelSettingsClone() {
            NodeSettings s = new NodeSettings("ignored");
            if (m_modelSettings != null) {
                m_modelSettings.copyTo(s);
            }
            return s;
        }

        /**
         * @return the modelSettings
         */
        public NodeSettingsRO getModelSettings() {
            return m_modelSettings;
        }

        /**
         * @param modelSettings the modelSettings to set
         */
        public void setModelSettings(final NodeSettingsRO modelSettings) {
            m_modelSettings = modelSettings;
        }

        /**
         * @return the variableSettings
         */
        public NodeSettingsRO getVariablesSettings() {
            return m_variablesSettings;
        }

        /**
         * @param variablesSettings the variablesSettings to set
         */
        public void setVariablesSettings(final NodeSettings variablesSettings) {
            m_variablesSettings = variablesSettings;
        }

        /** {@inheritDoc} */
        @Override
        protected SingleNodeContainerSettings clone() {
            try {
                return (SingleNodeContainerSettings)super.clone();
            } catch (CloneNotSupportedException e) {
                LOGGER.coding("clone() threw exception although class implements Clonable", e);
                return new SingleNodeContainerSettings();
            }
        }

    }

    /**
     * Get the policy for the data outports, that is, keep the output in main
     * memory or write it to disc. This method is used from within the
     * ExecutionContext when the derived NodeModel is executing.
     *
     * @return The memory policy to use.
     * @noreference This method is not intended to be referenced by clients.
     */
    public final MemoryPolicy getOutDataMemoryPolicy() {
        return m_settings.getMemoryPolicy();
    }

    /* ------------------ Port Information ------------------- */

    /**
     * @param portIndex ...
     * @return ...
     * @since 2.9
     */
    public abstract PortType getOutputType(final int portIndex);

    /**
     * @param portIndex ...
     * @return ...
     * @since 2.9
     */
    public abstract PortObjectSpec getOutputSpec(final int portIndex);

    /**
     * @param portIndex ...
     * @return ...
     * @since 2.9
     */
    public abstract PortObject getOutputObject(final int portIndex);

    /**
     * @param portIndex ...
     * @return ...
     * @since 2.9
     */
    public abstract String getOutputObjectSummary(final int portIndex);

    /**
     * @param portIndex ...
     * @return ...
     * @since 2.9
     */
    public abstract HiLiteHandler getOutputHiLiteHandler(final int portIndex);

}
