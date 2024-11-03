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
package org.knime.core.node.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

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
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.def.DefToCoreUtil;
import org.knime.core.node.workflow.execresult.NodeContainerExecutionStatus;
import org.knime.shared.workflow.def.BaseNodeDef;
import org.knime.shared.workflow.def.ConfigurableNodeDef;
import org.w3c.dom.Element;

/**
 * Implementation of {@link NodeContainer} which wraps a node hiding it's
 * internals, such as a node with an underlying implementation or a subnode
 * hiding it's internal workflow.
 *
 * @author M. Berthold/B. Wiswedel, University of Konstanz
 */
public abstract class SingleNodeContainer extends NodeContainer {

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
    /** Sub settings entry containing the node-view settings. */
    static final String CFG_VIEW = "view";
    static final String CFG_VIEW_VARIABLES = "view_variables";

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

    SingleNodeContainer(final WorkflowManager parent, final NodeID id, final ConfigurableNodeDef def) {
        super(parent, id, def);
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

    /**
     * @return a set of names of model settings that are overwritten by a variable
     * @throws InvalidSettingsException if settings can't be parsed
     * @since 5.3
     */
    public Set<String> getVariableControlledModelSettings() throws InvalidSettingsException {
        NodeSettings fromModel = m_settings.getModelSettingsClone();
        return getVariableControlledSettings(fromModel, m_settings.getVariablesSettings());
    }

    /**
     * @return a set of names of model view settings that are overwritten by a variable
     * @throws InvalidSettingsException if settings can't be parsed
     * @since 5.3
     */
    public Set<String> getVariableControlledViewSettings() throws InvalidSettingsException {
        NodeSettings fromViewModel = m_settings.getViewSettingsClone();
        return getVariableControlledSettings(fromViewModel, m_settings.getVariablesSettings());
    }

    private static Set<String> getVariableControlledSettings(final NodeSettings settings,
        final NodeSettingsRO variableSettings) throws InvalidSettingsException {
        if (variableSettings == null) {
            return Collections.emptySet();
        }
        ConfigEditTreeModel configEditor;
        try {
            configEditor = ConfigEditTreeModel.create(settings, variableSettings);
            return configEditor.getVariableControlledParameters();
        } catch (final InvalidSettingsException e) {
            throw new InvalidSettingsException("Errors reading flow variables: " + e.getMessage(), e);
        }
    }

    /** Used before configure, to apply the variable mask to the nodesettings,
     * that is to change individual node settings to reflect the current values
     * of the variables (if any).
     * @return a map containing the exposed variables (which are visible to
     * downstream nodes. These variables are put onto the node's
     * {@link FlowObjectStack}.
     */
    private Map<String, FlowVariable> applySettingsUsingFlowObjectStack() throws InvalidSettingsException {
        NodeSettingsRO viewVariableSettings = m_settings.getViewVariablesSettings();
        NodeSettingsRO variableSettings = m_settings.getVariablesSettings();
        if (variableSettings == null && viewVariableSettings == null) {
            return Collections.emptyMap();
        }

        List<FlowVariable> newVariableList = new ArrayList<>();
        try {
            NodeContext.pushContext(this);
            if (viewVariableSettings != null) {
                applyViewSettingsUsingFlowObjectStack(viewVariableSettings, newVariableList);
            }

            if (variableSettings != null) {
                applyModelSettingsUsingFlowObjectStack(variableSettings, newVariableList);
            }
        } catch (InvalidSettingsException e) {
            throw new InvalidSettingsException("Errors loading flow variables into node : " + e.getMessage(), e);
        } finally {
            NodeContext.removeLastContext();
        }

        Map<String, FlowVariable> newVariableMap = new LinkedHashMap<String, FlowVariable>();
        for (FlowVariable v : newVariableList) {
            if (newVariableMap.put(v.getName(), v) != null) {
                LOGGER.warn("Duplicate variable assignment for key \"" + v.getName() + "\")");
            }
        }
        return newVariableMap;
    }

    private void applyModelSettingsUsingFlowObjectStack(final NodeSettingsRO variableSettings,
        final List<FlowVariable> newVariableList) throws InvalidSettingsException {
        NodeSettings fromModel = m_settings.getModelSettingsClone();
        List<FlowVariable> newModelVariableList = overwriteSettingsWithFlowVariables(fromModel, variableSettings,
            getAllFlowVariables());
        newVariableList.addAll(newModelVariableList);

        performValidateSettings(fromModel);
        performLoadModelSettingsFrom(fromModel);
    }

    private void applyViewSettingsUsingFlowObjectStack(final NodeSettingsRO viewVariableSettings,
        final List<FlowVariable> newVariableList) throws InvalidSettingsException {
        NodeSettings fromViewModel = m_settings.getViewSettingsClone();

        List<FlowVariable> newViewVariableList =
            overwriteSettingsWithFlowVariables(fromViewModel, viewVariableSettings, getAllFlowVariables());
        newVariableList.addAll(newViewVariableList);

        performValidateViewSettings(fromViewModel);
    }

    private Map<String, FlowVariable> getAllFlowVariables() {
        return getFlowObjectStack().getAvailableFlowVariables(VariableType.getAllTypes());
    }

    private static List<FlowVariable> overwriteSettingsWithFlowVariables(final NodeSettings settingsToOverwrite,
        final NodeSettingsRO variablesSettings, final Map<String, FlowVariable> flowVariablesMap)
        throws InvalidSettingsException {
        if (variablesSettings == null) {
            return Collections.emptyList();
        }
        ConfigEditTreeModel configEditor;
        try {
            configEditor = ConfigEditTreeModel.create(settingsToOverwrite, variablesSettings);
        } catch (final InvalidSettingsException e) {
            throw new InvalidSettingsException("Errors reading flow variables: " + e.getMessage(), e);
        }
        List<FlowVariable> newVariableList;
        try {
            newVariableList = configEditor.overwriteSettings(settingsToOverwrite, flowVariablesMap);
        } catch (InvalidSettingsException e) {
            throw new InvalidSettingsException(
                "Errors overwriting node settings with flow variables: " + e.getMessage(), e);
        }
        return newVariableList;
    }

    /**
     * Extracts and returns the model settings of a node. The (sub-)settings that are controlled by flow variables are
     * replaced by the flow variable value.
     *
     * NOTE: this method can fail if the set flow variables are not available in the flow object stack (mainly because
     * upstream nodes aren't executed, yet)
     *
     * @return the node's 'model' settings, with setting values replaced with the value of the controlling flow variable
     *         (if so)
     * @throws InvalidSettingsException
     *
     * @since 4.2
     */
    public NodeSettings getModelSettingsUsingFlowObjectStack() throws InvalidSettingsException {
        if (m_settings.getModelSettings() == null) {
            //model settings haven't been initialized, yet (freshly added node)
            saveModelSettingsToDefault();
        }
        NodeSettings modelSettings = m_settings.getModelSettingsClone();
        overwriteSettingsWithFlowVariables(modelSettings, m_settings.getVariablesSettings(),
            getAllFlowVariables());
        return modelSettings;
    }

    /**
     * Extracts and returns the view settings of a node. The (sub-)settings that are controlled by flow variables are
     * replaced by the flow variable value. An empty optional is returned when either the node has no view settings or
     * it is a freshly added node and the node container provides no default view settings.
     *
     * NOTE: this method can fail if the set flow variables are not available in the flow object stack (mainly because
     * upstream nodes aren't executed, yet)
     *
     * @return the node's 'view' settings, with setting values replaced with the value of the controlling flow variable
     *         (if so); or an empty optional if there are no view-settings
     * @throws InvalidSettingsException
     *
     * @since 4.6
     */
    public Optional<NodeSettings> getViewSettingsUsingFlowObjectStack() throws InvalidSettingsException {
        saveViewSettingsToDefault();
        if (m_settings.getViewSettings() == null) {
            return Optional.empty();
        }
        var viewSettings = m_settings.getViewSettingsClone();
        overwriteSettingsWithFlowVariables(viewSettings, m_settings.getViewVariablesSettings(), getAllFlowVariables());
        return Optional.of(viewSettings);
    }

    /**
     * Load cleaned and "variable adjusted" into underlying implementation. Throws exception if validation of settings
     * fails or other problems occur.
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
            return getInternalState().isResetable();
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
                // temporary debug output to diagnose problem on instrumentation server, to be removed (BW - 2018-11-26)
                Supplier<String> statePrinter = () -> {
                    WorkflowManager projectWFM = getParent().getProjectWFM();
                    return projectWFM.printNodeSummary(projectWFM.getID(), 0);
                };
                CheckUtils.checkState(job != null,
                        "Exec Job on node '%s' is null, job manager on node '%s', on parent '%s'; state of project: ",
                        getNameWithID(), getJobManager(), findJobManager(), statePrinter.get());
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

    /**
     * Replaces the node's view settings with the provided ones.
     *
     * @param settings
     */
    void loadViewSettings(final NodeSettingsRO settings) {
        m_settings.m_viewSettings = settings;
        setDirty();
    }

    /**
     * Replaces the node's view settings with the provided ones.
     *
     * @param variableSettings
     */
    void loadViewVariableSettings(final NodeSettingsRO variableSettings) {
        m_settings.m_viewVariablesSettings = variableSettings;
        setDirty();
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
            NodeContext.pushContext(this);
            try {
                return performLoadContent(persistor, tblRep, inStack, exec, loadResult, preserveNodeMessage);
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    @Override
    void loadContent(final BaseNodeDef nodeDef, final ExecutionMonitor exec, final LoadResult loadResult)
        throws CanceledExecutionException {
        synchronized (m_nodeMutex) {
            if (!(nodeDef instanceof ConfigurableNodeDef)) {
                throw new IllegalStateException("Expected " + ConfigurableNodeDef.class.getSimpleName()
                    + " persistor object, got " + nodeDef.getClass().getSimpleName());
            }
            exec.checkCanceled();
            var singleNodeDef = (ConfigurableNodeDef)nodeDef;
            setInternalState(InternalNodeContainerState.IDLE, false);
            SingleNodeContainerSettings sncSettings = null;
            try {
                var internalSettings = DefToCoreUtil.toNodeSettings(singleNodeDef.getInternalNodeSubSettings());
                sncSettings = new SingleNodeContainerSettings(internalSettings);
            } catch (InvalidSettingsException ex) {
                final String msg = String.format("Can't create the single node container settings: %s", ex.getMessage());
                LOGGER.error(msg, ex);
                loadResult.addError(msg);
            }
            if (sncSettings == null) {
                LOGGER.coding("SNC settings from def are null, using default");
                sncSettings = new SingleNodeContainerSettings();
            }
            m_settings = sncSettings;
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
        saveSettings(settings, false, false);
    }

    /** {@inheritDoc} */
    @Override
    public NodeSettings getNodeSettings() {
        NodeSettings settings = new NodeSettings("configuration");
        saveSettings(settings, true, false);
        return settings;
    }

    /**
     * Saves config from super NodeContainer (job manager) and the view and model settings and variable settings.
     *
     * @param settings To save to.
     * @param initDefaultSettings If true and the model or view settings are not yet assigned (node freshly dragged onto
     *            workflow) the NodeModel's saveSettingsTo or saveDefaultViewSettingsTo method is called to init
     *            fallback settings.
     * @param wash previous versions of KNIME (2.7 and before) kept the model settings only in the node;
     *            NodeModel#saveSettingsTo was always called before the dialog was opened (some dialog implementations
     *            rely on the exact structure of the NodeSettings ... which may change between versions). We wash the
     *            settings through the node so that the model settings are updated when opening the dialog (only
     *            relevant for the legacy swing-based dialogs)
     */
    void saveSettings(final NodeSettingsWO settings, final boolean initDefaultSettings, final boolean wash) {
        super.saveSettings(settings);
        saveSNCSettings(settings, initDefaultSettings, wash);
    }

    /** Implementation of {@link WorkflowManager#saveNodeSettingsToDefault(NodeID)}. */
    void saveModelSettingsToDefault() {
        saveModelSettingsTo(m_settings);
    }

    void saveViewSettingsToDefault() {
        if (m_settings.getViewSettings() == null) {
            saveDefaultViewSettingsTo(m_settings);
        }
    }

    /**
     * Saves the {@link SingleNodeContainerSettings}.
     * @param settings To save to.
     * @param initDefaultSettings If true and the view or model settings are not yet assigned (node freshly dragged onto
     * workflow) the NodeModel's saveSettingsTo or saveDefaultViewSettingsTo method is called to init fallback settings.
     */
    void saveSNCSettings(final NodeSettingsWO settings, final boolean initDefaultSettings) {
        saveSNCSettings(settings, initDefaultSettings, false);
    }

    private void saveSNCSettings(final NodeSettingsWO settings, final boolean initDefaultSettings, final boolean wash) {
        SingleNodeContainerSettings sncSettings = m_settings;
        final var initDefaultModelSettings = initDefaultSettings && m_settings.getModelSettings() == null;
        final var initDefaultViewSettings = initDefaultSettings && m_settings.getViewSettings() == null;
        final var washModelSettings = wash && sncSettings.getModelSettings() != null;
        if (initDefaultModelSettings || initDefaultViewSettings || wash) {
            sncSettings = m_settings.clone();
            if (washModelSettings) {
                saveWashedModelSettingsTo(sncSettings);
            } else if (initDefaultModelSettings) {
                saveModelSettingsTo(sncSettings);
            }
            if (initDefaultViewSettings) {
                saveDefaultViewSettingsTo(sncSettings);
            }
        }
        sncSettings.save(settings);
    }

    /**
     * previous versions of KNIME (2.7 and before) kept the model settings only in the node; NodeModel#saveSettingsTo
     * was always called before the dialog was opened (some dialog implementations rely on the exact structure of the
     * NodeSettings ... which may change between versions). We wash the settings through the node so that the model
     * settings are updated when opening the dialog (only relevant for the legacy swing-based dialogs)
     *
     * We need to temporarily load the current model settings of the container, since we cannot rely on any previously
     * loaded settings being the same. In fact, they are not the same, if a model setting is overwritten by flow
     * variable (since we do want to wash only the non-overwritten flow variables here).
     *
     * In order to not alter the node models state, we clean up afterwards by remembering the previously present
     * settings and loading them again.
     *
     * @param sncSettings to save the washed model settings to.
     */
    private void saveWashedModelSettingsTo(final SingleNodeContainerSettings sncSettings) {
        SingleNodeContainerSettings previousSncSettings = new SingleNodeContainerSettings();
        saveModelSettingsTo(previousSncSettings);
        /**
         * We check here whether washing is even necessary to not alter the node models state unnecessarily. This can
         * only not be the case in two (not necessarily exclusive) situations:
         *
         * 1. There exist variable settings (so that the previousSncSettings have overwritten model settings)
         *
         * 2. The sncSettings model settings are outdated and washing is necessary
         *
         * Since both 1. and 2. are non-standard situations, we avoid washing in most cases.
         */
        if (previousSncSettings.getModelSettings().equals(sncSettings.getModelSettings())) {
            return;
        }
        try {
            performValidateSettings(sncSettings.getModelSettings());
            performLoadModelSettingsFrom(sncSettings.getModelSettings());
        } catch (InvalidSettingsException ex) {
            LOGGER.debug("Unable to wash settings.", ex);
            /**
             * If loading the settings fails here, we do not want to continue with the washing.
             */
            return;
        }
        saveModelSettingsTo(sncSettings);
        try {
            performValidateSettings(previousSncSettings.getModelSettings());
            performLoadModelSettingsFrom(previousSncSettings.getModelSettings());
        } catch (InvalidSettingsException ex) {
            /**
             * Under the assumption that settings that were successfully loaded before can be saved and successfully
             * loaded again, this should not happen. If it does, we continue leaving the node model in a state where the
             * valid model settings from the sncSettings were loaded.
             */
            LOGGER.debug("Unable to reapply settings after washing.", ex);
        }
    }

    private void saveModelSettingsTo(final SingleNodeContainerSettings sncSettings) {
        NodeSettings modelSettings = new NodeSettings("model");
        NodeContext.pushContext(this);
        try {
            performSaveModelSettingsTo(modelSettings);
        } finally {
            NodeContext.removeLastContext();
        }
        sncSettings.setModelSettings(modelSettings);
    }

    private void saveDefaultViewSettingsTo(final SingleNodeContainerSettings sncSettings) {
        NodeSettings viewSettings = new NodeSettings("view");
        NodeContext.pushContext(this);
        try {
            performSaveDefaultViewSettingsTo(viewSettings);
        } finally {
            NodeContext.removeLastContext();
        }
        if (!viewSettings.keySet().isEmpty()) {
            sncSettings.setViewSettings(viewSettings);
        }
    }

    /** Save settings of specific implementation.
     * The NodeContext is available when this method is called.
     *
     * @param modelSettings ...
     */
    abstract void performSaveModelSettingsTo(final NodeSettings modelSettings);

    /**
     * Save default view settings of specific implementation. This is called when first requesting view settings on a
     * newly loaded view. The NodeContext is available when this method is called.
     *
     * @param viewSettings ...
     */
    abstract void performSaveDefaultViewSettingsTo(final NodeSettings viewSettings);

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

    /**
     * Validate settings of specific implementation.
     *
     * @param modelSettings ...
     * @throws InvalidSettingsException ...
     */
    abstract void performValidateSettings(final NodeSettingsRO modelSettings) throws InvalidSettingsException;

    /**
     * Validate view settings of specific implementation. Throws exception if validation of settings fails or other
     * problems occur.
     *
     * @param viewSettings ...
     * @throws InvalidSettingsException ...
     * @since 5.2
     */
    protected void performValidateViewSettings(final NodeSettingsRO viewSettings) throws InvalidSettingsException {
    }

    ////////////////////////////////////
    // Credentials handling
    ////////////////////////////////////

    /** {@inheritDoc} */
    @Override
    public boolean areDialogAndNodeSettingsEqual() {
        final String key = "snc_settings";
        NodeSettingsWO nodeSettings = new NodeSettings(key);
        saveSettings(nodeSettings, true, false);
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
            oldProvider.cleanup();
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
            // Scope end and scope start nodes are members of the scope
            if (isModelCompatibleTo(ScopeEndNode.class) || isModelCompatibleTo(ScopeStartNode.class)) {
                return true;
            }
            // Check if a FlowScopeContext is on the stack
            // and make sure it is not a FlowSubnodeScopeContext: Components are not part of a scope
            final FlowObjectStack fos = getFlowObjectStack();
            if (fos == null) {
                return false;
            }
            final FlowScopeContext fsc = fos.peek(FlowScopeContext.class);
            return fsc != null && !(fsc instanceof FlowSubnodeScopeContext);
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
            final NodeID ownerID = getID();
            final FlowObjectStack st = getFlowObjectStack();
            final FlowObjectStack outgoingStack = getOutgoingFlowObjectStack();
            final boolean isPopTopmostScopeContext = isModelCompatibleTo(ScopeEndNode.class);
            return FlowObjectStack.createOutgoingFlowObjectStack(ownerID, st, outgoingStack, isPopTopmostScopeContext);
        }
    }

    /**
     * @param nodeModelClass ...
     * @return return true if underlying NodeModel (if it exists) implements the given class/interface
     * @since 2.8
     */
    public abstract boolean isModelCompatibleTo(final Class<?> nodeModelClass);

    /**
     * Sets the node inactive.
     * @return <code>true</code> if the inactivation was successful
     */
    abstract boolean setInactive();

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
        private NodeSettingsRO m_viewSettings;
        private NodeSettingsRO m_viewVariablesSettings;

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
            m_variablesSettings = settings.containsKey(CFG_VARIABLES) ? settings.getNodeSettings(CFG_VARIABLES) : null;

            // viewVariablesSettings, modelSettings and viewSettings are optional fields, as well
            m_viewVariablesSettings =
                settings.containsKey(CFG_VIEW_VARIABLES) ? settings.getNodeSettings(CFG_VIEW_VARIABLES) : null;
            m_modelSettings = settings.containsKey(CFG_MODEL) ? settings.getNodeSettings(CFG_MODEL) : null;
            m_viewSettings = settings.containsKey(CFG_VIEW) ? settings.getNodeSettings(CFG_VIEW) : null;
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
            if (m_viewSettings != null) {
                NodeSettingsWO view = settings.addNodeSettings(CFG_VIEW);
                m_viewSettings.copyTo(view);
            }
            if (m_viewVariablesSettings != null) {
                NodeSettingsWO viewVariables = settings.addNodeSettings(CFG_VIEW_VARIABLES);
                m_viewVariablesSettings.copyTo(viewVariables);
            }
        }

        /**
         * Store a new memory policy in this settings object. The memory policy must not be null.
         *
         * @param memPolicy the new policy to set
         */
        public void setMemoryPolicy(final MemoryPolicy memPolicy) {
            CheckUtils.checkNotNull(memPolicy,
                "Could not set MemoryPolicy to model SingleContainerSettings: policy cannot be null!");
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

        /**
         * @return the viewSettings
         */
        public NodeSettings getViewSettingsClone() {
            NodeSettings s = new NodeSettings("ignored");
            if (m_viewSettings != null) {
                m_viewSettings.copyTo(s);
            }
            return s;
        }

        /**
         * @return the viewSettings
         */
        public NodeSettingsRO getViewSettings() {
            return m_viewSettings;
        }

        /**
         * @param viewSettings the viewSettings to set
         */
        public void setViewSettings(final NodeSettingsRO viewSettings) {
            m_viewSettings = viewSettings;
        }

        /**
         * @return the viewVariablesSettings
         */
        public NodeSettingsRO getViewVariablesSettings() {
            return m_viewVariablesSettings;
        }

        /**
         * @param viewVariablesSettings the viewVariablesSettings to set
         */
        public void setViewVariablesSettings(final NodeSettings viewVariablesSettings) {
            m_viewVariablesSettings = viewVariablesSettings;
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
