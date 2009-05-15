/*
 * ------------------------------------------------------------------ *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   14.03.2007 (mb): created
 */
package org.knime.core.node.workflow;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodePostConfigure;
import org.knime.core.node.NodeProgressMonitor;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NodeView;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.exec.ThreadNodeExecutionJobManager;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.execresult.NodeContainerExecutionResult;
import org.knime.core.node.workflow.execresult.NodeExecutionResult;
import org.knime.core.node.workflow.execresult.SingleNodeContainerExecutionResult;
import org.w3c.dom.Element;

/**
 * Holds a node in addition to some status information.
 *
 * @author M. Berthold/B. Wiswedel, University of Konstanz
 */
public final class SingleNodeContainer extends NodeContainer {

    /** my logger. */
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(SingleNodeContainer.class);

    /** underlying node. */
    private final Node m_node;

    private SingleNodeContainerSettings m_settings =
        new SingleNodeContainerSettings();

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
         * Cache only small tables in memory, i.e. with cell count <=
         * DataContainer.MAX_CELLS_IN_MEMORY.
         */
        CacheSmallInMemory,
        /** Buffer on disc. */
        CacheOnDisc
    }

    /** Config key: What memory policy to use for a node outport. */
    static final String CFG_MEMORY_POLICY = "memory_policy";

    /**
     * Create new SingleNodeContainer based on existing Node.
     *
     * @param parent the workflow manager holding this node
     * @param n the underlying node
     * @param id the unique identifier
     */
    SingleNodeContainer(final WorkflowManager parent, final Node n,
            final NodeID id) {
        super(parent, id);
        m_node = n;
        setPortNames();
        m_node.addMessageListener(new UnderlyingNodeMessageListener());
    }

    /**
     * Create new SingleNodeContainer from persistor.
     *
     * @param parent the workflow manager holding this node
     * @param id the identifier
     * @param persistor to read from
     */
    SingleNodeContainer(final WorkflowManager parent, final NodeID id,
            final SingleNodeContainerPersistor persistor) {
        super(parent, id, persistor.getMetaPersistor());
        m_node = persistor.getNode();
        assert m_node != null : persistor.getClass().getSimpleName()
                + " did not provide Node instance for "
                + getClass().getSimpleName() + " with id \"" + id + "\"";
        setPortNames();
        m_node.addMessageListener(new UnderlyingNodeMessageListener());
    }

    private void setPortNames() {
        for (int i = 0; i < getNrOutPorts(); i++) {
            getOutPort(i).setPortName(m_node.getFactory().getOutportName(i));
        }
        for (int i = 0; i < getNrInPorts(); i++) {
            getInPort(i).setPortName(m_node.getFactory().getInportName(i));
        }
    }

    /**
     * @return the underlying Node
     */
    Node getNode() {
        return m_node;
    }

    /* ------------------ Port Handling ------------- */

    /** {@inheritDoc} */
    @Override
    public int getNrOutPorts() {
        return m_node.getNrOutPorts();
    }

    /** {@inheritDoc} */
    @Override
    public int getNrInPorts() {
        return m_node.getNrInPorts();
    }

    private NodeContainerOutPort[] m_outputPorts = null;
    /**
     * Returns the output port for the given <code>portID</code>. This port
     * is essentially a container for the underlying Node and the index and will
     * retrieve all interesting data from the Node.
     *
     * @param index The output port's ID.
     * @return Output port with the specified ID.
     * @throws IndexOutOfBoundsException If the index is out of range.
     */
    @Override
    public NodeOutPort getOutPort(final int index) {
        if (m_outputPorts == null) {
            m_outputPorts = new NodeContainerOutPort[getNrOutPorts()];
        }
        if (m_outputPorts[index] == null) {
            m_outputPorts[index] = new NodeContainerOutPort(this, index);
        }
        return m_outputPorts[index];
    }

    private NodeInPort[] m_inputPorts = null;
    /**
     * Return a port, which for the inputs really only holds the type and some
     * other static information.
     *
     * @param index the index of the input port
     * @return port
     */
    @Override
    public NodeInPort getInPort(final int index) {
        if (m_inputPorts == null) {
            m_inputPorts = new NodeInPort[getNrInPorts()];
        }
        if (m_inputPorts[index] == null) {
            m_inputPorts[index] =
                    new NodeInPort(index, m_node.getInputType(index));
        }
        return m_inputPorts[index];
    }

    /**
     * Get the policy for the data outports, that is, keep the output in main
     * memory or write it to disc. This method is used from within the
     * ExecutionContext when the derived NodeModel is executing.
     *
     * @return The memory policy to use.
     */
    final MemoryPolicy getOutDataMemoryPolicy() {
        return m_settings.getMemoryPolicy();
    }

    /* ------------------ Views ---------------- */

    /**
     * Set a new HiLiteHandler for an incoming connection.
     * @param index index of port
     * @param hdl new HiLiteHandler
     */
    void setInHiLiteHandler(final int index, final HiLiteHandler hdl) {
        m_node.setInHiLiteHandler(index, hdl);
    }

    /** {@inheritDoc} */
    @Override
    public NodeView<NodeModel> getNodeView(final int i) {
        String title = getNameWithID() + " (" + getViewName(i) + ")";
        if (getCustomName() != null) {
            title += " - " + getCustomName();
        }
        return (NodeView<NodeModel>)m_node.getView(i, title);
    }

    /** {@inheritDoc} */
    @Override
    public String getNodeViewName(final int i) {
        return m_node.getViewName(i);
    }

    /** {@inheritDoc} */
    @Override
    public int getNrNodeViews() {
        return m_node.getNrViews();
    }

    /** {@inheritDoc} */
    @Override
    void cleanup() {
        super.cleanup();
        m_node.cleanup();
        if (m_outputPorts != null) {
            for (NodeOutPort p : m_outputPorts) {
                if (p != null) {
                    p.disposePortView();
                }
            }
        }
    }

    /**
     * Set a new NodeExecutionJobManager for this node but before check for
     * valid state.
     *
     * @param je the new NodeExecutionJobManager.
     */
    @Override
    public void setJobManager(final NodeExecutionJobManager je) {
        synchronized (m_nodeMutex) {
            switch (getState()) {
            case QUEUED:
            case PREEXECUTE:
            case EXECUTING:
            case EXECUTINGREMOTELY:
            case POSTEXECUTE:
                throwIllegalStateException();
            default:
            }
            super.setJobManager(je);
        }
    }

    public ExecutionContext createExecutionContext() {
        NodeProgressMonitor progressMonitor = getProgressMonitor();
        progressMonitor.reset();
        return new ExecutionContext(progressMonitor, getNode(),
                getOutDataMemoryPolicy(),
                getParent().getGlobalTableRepository());
    }

    // ////////////////////////////////
    // Handle State Transitions
    // ////////////////////////////////

    /**
     * Configure underlying node and update state accordingly.
     *
     * @param inObjectSpecs input table specifications
     * @return true if output specs have changed.
     * @throws IllegalStateException in case of illegal entry state.
     */
    boolean configure(final PortObjectSpec[] inObjectSpecs) {
        synchronized (m_nodeMutex) {
            // remember old specs
            PortObjectSpec[] prevSpecs = new PortObjectSpec[getNrOutPorts()];
            for (int i = 0; i < prevSpecs.length; i++) {
                prevSpecs[i] = getOutPort(i).getPortObjectSpec();
            }
            // perform action
            switch (getState()) {
            case IDLE:
                if (nodeConfigure(inObjectSpecs)) {
                    setState(State.CONFIGURED);
                } else {
                    setState(State.IDLE);
                }
                break;
            case UNCONFIGURED_MARKEDFOREXEC:
                if (nodeConfigure(inObjectSpecs)) {
                    setState(State.MARKEDFOREXEC);
                } else {
                    setState(State.UNCONFIGURED_MARKEDFOREXEC);
                }
                break;
            case CONFIGURED:
                // m_node.reset();
                boolean success = nodeConfigure(inObjectSpecs);
                if (success) {
                    setState(State.CONFIGURED);
                } else {
                    // m_node.reset();
                    setState(State.IDLE);
                }
                break;
            case MARKEDFOREXEC:
                // these are dangerous - otherwise re-queued loop-ends are
                // reset!
                // m_node.reset();
                success = nodeConfigure(inObjectSpecs);
                if (success) {
                    setState(State.MARKEDFOREXEC);
                } else {
                    // m_node.reset();
                    setState(State.UNCONFIGURED_MARKEDFOREXEC);
                }
                break;
            case EXECUTINGREMOTELY: // this should only happen during load
                success = nodeConfigure(inObjectSpecs);
                if (!success) {
                    setState(State.IDLE);
                }
                break;
            default:
                throwIllegalStateException();
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
            return false; // all specs stayed the same!
        }
    }

    /**
     * Calls configure in the node, allowing the current job manager to modify
     * the output specs according to its settings (in case it modifies the
     * node's output).
     *
     * @param inSpecs the input specs to node configure
     */
    private boolean nodeConfigure(final PortObjectSpec[] inSpecs) {

        final NodeExecutionJobManager jobMgr = findJobManager();

        NodePostConfigure npc = new NodePostConfigure() {
            public PortObjectSpec[] configure(final PortObjectSpec[] inObjSpecs,
                    final PortObjectSpec[] nodeModelOutSpecs)
                    throws InvalidSettingsException {
                return jobMgr.configure(inObjSpecs, nodeModelOutSpecs);
            }
        };
        return m_node.configure(inSpecs, npc);
    }

    /**
     * check if node can be safely reset.
     *
     * @return if node can be reset.
     */
    @Override
    boolean isResetable() {
        switch (getState()) {
        case EXECUTED:
        case MARKEDFOREXEC:
        case UNCONFIGURED_MARKEDFOREXEC:
        case CONFIGURED:
            return true;
        default:
            return false;
        }
    }

    /** Reset underlying node and update state accordingly.
     * @throws IllegalStateException in case of illegal entry state.
     */
    void reset() {
        synchronized (m_nodeMutex) {
            switch (getState()) {
            case EXECUTED:
                removeOutputTablesFromGlobalRepository();
                m_node.reset();
                // After reset we need explicit configure!
                setState(State.IDLE);
                return;
            case MARKEDFOREXEC:
                setState(State.CONFIGURED);
                return;
            case UNCONFIGURED_MARKEDFOREXEC:
                setState(State.IDLE);
                return;
            case CONFIGURED:
                m_node.reset();
                setState(State.IDLE);
                return;
            default:
                throwIllegalStateException();
            }
        }
    }

    /** Enable (or disable) queuing of underlying node for execution. This
     * really only changes the state of the node and once all pre-conditions
     * for execution are fulfilled (e.g. configuration succeeded and all
     * ingoing objects are available) the node will be actually queued.
     *
     * @param flag determines if node is marked or unmarked for execution
     * @throws IllegalStateException in case of illegal entry state.
     */
    @Override
    void markForExecution(final boolean flag) {
        synchronized (m_nodeMutex) {
            if (flag) {  // we want to mark the node for execution!
                switch (getState()) {
                case CONFIGURED:
                    setState(State.MARKEDFOREXEC);
                    return;
                case IDLE:
                    setState(State.UNCONFIGURED_MARKEDFOREXEC);
                    return;
                default:
                    throwIllegalStateException();
                }
            } else {  // we want to remove the mark for execution
                switch (getState()) {
                case MARKEDFOREXEC:
                    setState(State.CONFIGURED);
                    return;
                case UNCONFIGURED_MARKEDFOREXEC:
                    setState(State.IDLE);
                    return;
                default:
                    throwIllegalStateException();
                }
            }
        }
    }

    /**
     * Queue underlying node for re-execution (= update state accordingly).
     *
     * @throws IllegalStateException in case of illegal entry state.
     */
    void enableReQueuing() {
        synchronized (m_nodeMutex) {
            switch (getState()) {
            case EXECUTED:
                m_node.cleanOutPorts();
                setState(State.MARKEDFOREXEC);
                return;
            default:
                throwIllegalStateException();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    void cancelExecution() {
        synchronized (m_nodeMutex) {
            switch (getState()) {
            case UNCONFIGURED_MARKEDFOREXEC:
                setState(State.IDLE);
                break;
            case MARKEDFOREXEC:
                setState(State.CONFIGURED);
                break;
            case QUEUED:
                // m_executionFuture has not yet started or if it has started,
                // it will not hand off to node implementation (otherwise it
                // would be executing)
                getProgressMonitor().setExecuteCanceled();
                NodeExecutionJob job = getExecutionJob();
                assert job != null : "node is queued but no job represents "
                    + "the execution task (is null)";
                job.cancel();
                setState(State.CONFIGURED);
                break;
            case EXECUTING:
                // future is running in thread pool, use ordinary cancel policy
                getProgressMonitor().setExecuteCanceled();
                job = getExecutionJob();
                assert job != null : "node is executing but no job represents "
                    + "the execution task (is null)";
                job.cancel();
                break;
            case PREEXECUTE:   // locally executing nodes are not really in
            case POSTEXECUTE:  // one of these two states
            case EXECUTINGREMOTELY:
                // execute remotely can be both truly executing remotely
                // (job will be non-null) or marked as executing remotely
                // (e.g. node is part of meta node which is remote executed
                // -- the job will be null). We tolerate both cases here.
                job = getExecutionJob();
                if (job == null) {
                    // we can't decide on whether this node is now IDLE or
                    // CONFIGURED -- we rely on the parent to call configure
                    // (pessimistic guess here that node was not configured)
                    setState(State.IDLE);
                } else {
                    getProgressMonitor().setExecuteCanceled();
                    job.cancel();
                }
                break;
            case EXECUTED:
                // Too late - do nothing.
                break;
            default:
                LOGGER.warn("Strange state " + getState()
                        + " encountered in cancelExecution().");
            }
        }
    }


    //////////////////////////////////////
    //  internal state change actions
    //////////////////////////////////////

    /** {@inheritDoc} */
    @Override
    void mimicRemotePreExecute() {
        synchronized (m_nodeMutex) {
            switch (getState()) {
            case MARKEDFOREXEC:
            case UNCONFIGURED_MARKEDFOREXEC:
                setState(State.PREEXECUTE);
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
            switch (getState()) {
            case PREEXECUTE:
                setState(State.EXECUTINGREMOTELY);
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
            switch (getState()) {
            case EXECUTINGREMOTELY:
                setState(State.POSTEXECUTE);
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
    void performStateTransitionPREEXECUTE() {
        synchronized (m_nodeMutex) {
            switch (getState()) {
            case QUEUED:
                setState(State.PREEXECUTE);
                break;
            default:
                throwIllegalStateException();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    void performStateTransitionEXECUTING() {
        synchronized (m_nodeMutex) {
            switch (getState()) {
            case PREEXECUTE:
                m_node.clearLoopStatus();
                if (findJobManager() instanceof ThreadNodeExecutionJobManager) {
                    setState(State.EXECUTING);
                } else {
                    setState(State.EXECUTINGREMOTELY);
                }
                break;
            default:
                throwIllegalStateException();
            }
        }
    }
    
    /** {@inheritDoc} */
    @Override
    void performStateTransitionPOSTEXECUTE() {
        synchronized (m_nodeMutex) {
            switch (getState()) {
            case EXECUTING:
            case EXECUTINGREMOTELY:
                setState(State.POSTEXECUTE);
                break;
            default:
                throwIllegalStateException();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    void performStateTransitionEXECUTED(final boolean success) {
        synchronized (m_nodeMutex) {
            switch (getState()) {
            case POSTEXECUTE:
                if (success) {
                    if (m_node.getLoopStatus() == null) {
                        setState(State.EXECUTED);
                        
                    } else {
                        // loop not yet done - "stay" configured until done.
                        setState(State.CONFIGURED);
                    }
                } else {
                    // also clean loop status:
                    m_node.clearLoopStatus();
                    // note was already reset/configured in doAfterExecute.
                    // in theory this should always be the correct state...
                    // TODO do better - also handle catastrophes
                    setState(State.CONFIGURED);
                }
                setExecutionJob(null);
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
    public boolean performExecuteNode(final PortObject[] inObjects) {
        ExecutionContext ec = createExecutionContext();
        boolean success;
        try {
            ec.checkCanceled();
            success = true;
        } catch (CanceledExecutionException e) {
            String errorString = "Execution canceled";
            LOGGER.warn(errorString);
            setNodeMessage(new NodeMessage(
                    NodeMessage.Type.WARNING, errorString));
            success = false;
        }
        // execute node outside any synchronization!
        success = success && m_node.execute(inObjects, ec);
        if (success) {
            // output tables are made publicly available (for blobs)
            putOutputTablesIntoGlobalRepository(ec);
        } else {
            // something went wrong: reset and configure node to reach
            // a solid state again - but remember original node message!
            NodeMessage orgMessage = getNodeMessage();
            m_node.reset();
            PortObjectSpec[] specs = new PortObjectSpec[m_node.getNrInPorts()];
            for (int i = 0; i < specs.length; i++) {
                specs[i] = inObjects[i].getSpec();
            }
            if (!nodeConfigure(specs)) {
                LOGGER.error("Configure failed after Execute failed!");
            }
            // TODO don't remove the stack conflict message (if any)
            setNodeMessage(orgMessage);
        }
        return success;
    }

    /**
     * Enumerates the output tables and puts them into the workflow global
     * repository of tables. All other (temporary) tables that were created in
     * the given execution context, will be put in a set of temporary tables in
     * the node.
     *
     * @param c The execution context containing the (so far) local tables.
     */
    private void putOutputTablesIntoGlobalRepository(final ExecutionContext c) {
        HashMap<Integer, ContainerTable> globalRep =
            getParent().getGlobalTableRepository();
        m_node.putOutputTablesIntoGlobalRepository(globalRep);
        HashMap<Integer, ContainerTable> localRep =
                Node.getLocalTableRepositoryFromContext(c);
        Set<ContainerTable> localTables = new HashSet<ContainerTable>();
        for (Map.Entry<Integer, ContainerTable> t : localRep.entrySet()) {
            ContainerTable fromGlob = globalRep.get(t.getKey());
            if (fromGlob == null) {
                // not used globally
                localTables.add(t.getValue());
            } else {
                assert fromGlob == t.getValue();
            }
        }
        m_node.addToTemporaryTables(localTables);
    }

    /** Removes all tables that were created by this node from the global
     * table repository. */
    private void removeOutputTablesFromGlobalRepository() {
        HashMap<Integer, ContainerTable> globalRep =
            getParent().getGlobalTableRepository();
        m_node.removeOutputTablesFromGlobalRepository(globalRep);
    }

    // //////////////////////////////////////
    // Save & Load Settings and Content
    // //////////////////////////////////////

    /** {@inheritDoc} */
    @Override
    void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        synchronized (m_nodeMutex) {
            super.loadSettings(settings);
            m_node.loadSettingsFrom(settings);
            loadSNCSettings(settings);
            setDirty();
        }
    }

    /** {@inheritDoc} */
    @Override
    LoadResult loadContent(final NodeContainerPersistor nodePersistor,
            final Map<Integer, BufferedDataTable> tblRep,
            final ScopeObjectStack inStack, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        LoadResult result = new LoadResult();
        synchronized (m_nodeMutex) {
            if (!(nodePersistor instanceof SingleNodeContainerPersistor)) {
                throw new IllegalStateException("Expected "
                        + SingleNodeContainerPersistor.class.getSimpleName()
                        + " persistor object, got "
                        + nodePersistor.getClass().getSimpleName());
            }
            SingleNodeContainerPersistor persistor =
                (SingleNodeContainerPersistor)nodePersistor;
            State state = persistor.getMetaPersistor().getState();
            setState(state, false);
            if (state.equals(State.EXECUTED)) {
                m_node.putOutputTablesIntoGlobalRepository(getParent()
                        .getGlobalTableRepository());
            }
            for (ScopeObject s : persistor.getScopeObjects()) {
                inStack.push(s);
            }
            setScopeObjectStack(inStack);
            SingleNodeContainerSettings sncSettings =
                persistor.getSNCSettings();
            if (sncSettings == null) {
                LOGGER.coding(
                        "SNC settings from persistor are null, using default");
                sncSettings = new SingleNodeContainerSettings();
            }
            m_settings = sncSettings;
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public LoadResult loadExecutionResult(
            final NodeContainerExecutionResult execResult,
            final ExecutionMonitor exec) {
        synchronized (m_nodeMutex) {
            if (!(execResult instanceof SingleNodeContainerExecutionResult)) {
                throw new IllegalArgumentException("Argument must be instance "
                        + "of \"" + SingleNodeContainerExecutionResult.
                        class.getSimpleName() + "\": "
                        + execResult.getClass().getSimpleName());
            }
            LoadResult errors = super.loadExecutionResult(execResult, exec);
            SingleNodeContainerExecutionResult sncExecResult =
                (SingleNodeContainerExecutionResult)execResult;
            NodeExecutionResult nodeExecResult =
                sncExecResult.getNodeExecutionResult();
            LoadResult nodeErrors = m_node.loadDataAndInternals(
                    nodeExecResult, new ExecutionMonitor());
            if (nodeErrors.hasEntries()) {
                errors.addError(nodeErrors);
            }
            boolean needsReset = nodeExecResult.needsResetAfterLoad();
            if (!needsReset && State.EXECUTED.equals(
                    sncExecResult.getState())) {
                for (int i = 0; i < getNrOutPorts(); i++) {
                    if (m_node.getOutputObject(i) == null) {
                        errors.addError(
                                "Output object at port " + i + " is null");
                        needsReset = true;
                    }
                }
            }
            if (needsReset) {
                execResult.setNeedsResetAfterLoad();
            }
            return errors;
        }
    }

    /** {@inheritDoc} */
    @Override
    public SingleNodeContainerExecutionResult createExecutionResult(
            final ExecutionMonitor exec) throws CanceledExecutionException {
        synchronized (m_nodeMutex) {
            SingleNodeContainerExecutionResult result =
                new SingleNodeContainerExecutionResult();
            super.saveExecutionResult(result);
            result.setNodeExecutionResult(
                    m_node.createNodeExecutionResult(exec));
            return result;
        }
    }

    /** {@inheritDoc} */
    @Override
    void saveSettings(final NodeSettingsWO settings) {
        super.saveSettings(settings);
        m_node.saveSettingsTo(settings);
        saveSNCSettings(settings);
    }

    /**
     * Saves the SingleNodeContainer settings such as the job executor to the
     * argument node settings object.
     *
     * @param settings To save to.
     * @see #loadSNCSettings(NodeSettingsRO)
     */
    void saveSNCSettings(final NodeSettingsWO settings) {
        m_settings.save(settings);
    }

    /**
     * Loads the SingleNodeContainer settings from the argument. This is the
     * reverse operation to {@link #saveSNCSettings(NodeSettingsWO)}.
     *
     * @param settings To load from.
     * @throws InvalidSettingsException If settings are invalid.
     */
    void loadSNCSettings(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        synchronized (m_nodeMutex) {
            m_settings = new SingleNodeContainerSettings(settings);
        }
    }

    /** @return reference to internally used settings (contains information for
     * memory policy, e.g.) */
    SingleNodeContainerSettings getSingleNodeContainerSettings() {
        return m_settings;
    }

    /** {@inheritDoc} */
    @Override
    boolean areSettingsValid(final NodeSettingsRO settings) {
        if (!super.areSettingsValid(settings)) {
            return false;
        }
        try {
            new SingleNodeContainerSettings(settings);
        } catch (InvalidSettingsException ise) {
            return false;
        }
        return m_node.areSettingsValid(settings);
    }

    ////////////////////////////////////
    // ScopeObjectStack handling
    ////////////////////////////////////

    /**
     * Set ScopeObjectStack.
     *
     * @param st new stack
     */
    void setScopeObjectStack(final ScopeObjectStack st) {
        synchronized (m_nodeMutex) {
            m_node.setScopeContextStackContainer(st);
        }
    }

    /**
     * @return current ScopeObjectStack
     */
    ScopeObjectStack getScopeObjectStack() {
        synchronized (m_nodeMutex) {
            return m_node.getScopeContextStackContainer();
        }
    }

    /**
     * @return role of node within a loop
     */
    Node.LoopRole getLoopRole() {
        return getNode().getLoopRole();
    }

    ///////////////////////////////////
    // NodeContainer->Node forwarding
    ///////////////////////////////////

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return m_node.getName();
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasDialog() {
        return m_node.hasDialog();
    }

    /** {@inheritDoc} */
    @Override
    NodeDialogPane getDialogPaneWithSettings(final PortObjectSpec[] inSpecs)
            throws NotConfigurableException {
        ScopeObjectStack stack = getScopeObjectStack();
        NodeSettings settings = new NodeSettings(getName());
        saveSettings(settings);
        return m_node.getDialogPaneWithSettings(inSpecs, stack, settings);
    }

    /** {@inheritDoc} */
    @Override
    NodeDialogPane getDialogPane() {
        return m_node.getDialogPane();
    }

    /** {@inheritDoc} */
    @Override
    public boolean areDialogAndNodeSettingsEqual() {
        final String key = "snc_settings";
        NodeSettingsWO nodeSettings = new NodeSettings(key);
        saveSettings(nodeSettings);
        NodeSettingsWO dlgSettings = new NodeSettings(key);
        try {
            m_node.getDialogPane().finishEditingAndSaveSettingsTo(dlgSettings);
        } catch (InvalidSettingsException e) {
            return false;
        }
        return dlgSettings.equals(nodeSettings);
    }

    /** {@inheritDoc} */
    @Override
    public NodeType getType() {
        return m_node.getType();
    }

    /** {@inheritDoc} */
    @Override
    public URL getIcon() {
        return m_node.getFactory().getIcon();
    }

    /**
     * @return the XML description of the node for the NodeDescription view
     */
    public Element getXMLDescription() {
        return m_node.getXMLDescription();
    }

    /** {@inheritDoc} */
    @Override
    protected boolean isLocalWFM() {
        return false;
    }

    /**
     * Overridden to also ensure that outport tables are "open" (node directory
     * is deleted upon save() - so the tables are better copied into temp).
     * {@inheritDoc}
     */
    @Override
    public void setDirty() {
        /*
         * Ensures that any port object in the associated node is read from its
         * saved location. Especially BufferedDataTable objects are read as late
         * as possible (in order to reduce start-up time), this method makes
         * sure that they are read (and either copied into TMP or into memory),
         * so the underlying node directory can be savely deleted.
         */
        m_node.ensureOutputDataIsRead();
        super.setDirty();
    }

    /** {@inheritDoc} */
    @Override
    protected NodeContainerPersistor getCopyPersistor(
            final HashMap<Integer, ContainerTable> tableRep,
            final boolean preserveDeletableFlags) {
        return new CopySingleNodeContainerPersistor(this,
                preserveDeletableFlags);
    }

    // /////////////////////////////////////////////////////////////////////
    // Settings loading and saving (single node container settings only)
    // /////////////////////////////////////////////////////////////////////

    /**
     * Handles the settings specific to a SingleNodeContainer. Reads and writes
     * them from and into a NodeSettings object.
     */
    public static class SingleNodeContainerSettings implements Cloneable {

        private MemoryPolicy m_memoryPolicy = MemoryPolicy.CacheSmallInMemory;
        /**
         * Creates a settings object with default values.
         */
        public SingleNodeContainerSettings() {
            // stay with the default values
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
        public SingleNodeContainerSettings(final NodeSettingsRO settings)
                throws InvalidSettingsException {
            NodeSettingsRO sncSettings =
                    settings.getNodeSettings(Node.CFG_MISC_SETTINGS);
            if (sncSettings.containsKey(CFG_MEMORY_POLICY)) {
                String memPolStr = sncSettings.getString(CFG_MEMORY_POLICY);
                try {
                m_memoryPolicy = MemoryPolicy.valueOf(memPolStr);
                } catch (IllegalArgumentException iae) {
                    throw new InvalidSettingsException(
                            "Invalid memory policy: " + memPolStr);
                }
            }

        }

        /**
         * Writes the current settings values into the passed argument.
         *
         * @param settings the object to write the settings into.
         */
        public void save(final NodeSettingsWO settings) {
            NodeSettingsWO sncSettings =
                    settings.addNodeSettings(Node.CFG_MISC_SETTINGS);
            sncSettings.addString(CFG_MEMORY_POLICY, m_memoryPolicy.name());
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

        /** {@inheritDoc} */
        @Override
        protected SingleNodeContainerSettings clone() {
            try {
                return (SingleNodeContainerSettings)super.clone();
            } catch (CloneNotSupportedException e) {
                LOGGER.coding("clone() threw exception although class "
                        + "implements Clonable", e);
                return new SingleNodeContainerSettings();
            }
        }

    }

    /** The message listener that is added the Node and listens for messages
     * that are set by failing execute methods are by the user
     * (setWarningMessage()).
     */
    private final class UnderlyingNodeMessageListener
        implements NodeMessageListener {
        /** {@inheritDoc} */
        @Override
        public void messageChanged(final NodeMessageEvent messageEvent) {
            SingleNodeContainer.this.setNodeMessage(messageEvent.getMessage());
        }
    }

}
