/*
 * ------------------------------------------------------------------ *
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
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeProgressMonitor;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NodeView;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.w3c.dom.Element;

/**
 * Holds a node in addition to some status information.
 *
 * @author M. Berthold/B. Wiswedel, University of Konstanz
 */
public final class SingleNodeContainer extends NodeContainer
    implements NodeProgressListener {

    /** my logger. */
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(SingleNodeContainer.class);

    /** underlying node. */
    private final Node m_node;

    /** progress monitor. */
    private final NodeProgressMonitor m_progressMonitor =
            new DefaultNodeProgressMonitor(this);

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
     * The memory policy for the data outports, i.e. keep in memory or hold on
     * disc. Default is to keep small tables in memory
     */
    private MemoryPolicy m_outDataPortsMemoryPolicy =
            MemoryPolicy.CacheSmallInMemory;

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
        return m_outDataPortsMemoryPolicy;
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
    public NodeView<NodeModel> getView(final int i) {
        String title = getNameWithID() + " (" + getViewName(i) + ")";
        if (getCustomName() != null) {
            title += " - " + getCustomName();
        }
        return (NodeView<NodeModel>)m_node.getView(i, title);
    }

    /** {@inheritDoc} */
    @Override
    public String getViewName(final int i) {
        return m_node.getViewName(i);
    }

    /** {@inheritDoc} */
    @Override
    public int getNrViews() {
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
            if (getState().equals(State.EXECUTING)
                    || getState().equals(State.QUEUED)) {
                throw new IllegalStateException("Illegal state " + getState()
                        + " in setJobExecutor - " 
                        + "can not change a running node.");
                
            }
            super.setJobManager(je);
        }
    }

    private ExecutionContext createExecutionContext() {
        m_progressMonitor.reset();
        return new ExecutionContext(m_progressMonitor, getNode(),
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
                if (m_node.configure(inObjectSpecs)) {
                    setState(State.CONFIGURED);
                } else {
                    setState(State.IDLE);
                }
                break;
            case UNCONFIGURED_MARKEDFOREXEC:
                if (m_node.configure(inObjectSpecs)) {
                    setState(State.MARKEDFOREXEC);
                } else {
                    setState(State.UNCONFIGURED_MARKEDFOREXEC);
                }
                break;
            case CONFIGURED:
                // m_node.reset();
                boolean success = m_node.configure(inObjectSpecs);
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
                success = m_node.configure(inObjectSpecs);
                if (success) {
                    setState(State.MARKEDFOREXEC);
                } else {
                    // m_node.reset();
                    setState(State.UNCONFIGURED_MARKEDFOREXEC);
                }
                break;
            default:
                throw new IllegalStateException("Illegal state " + getState()
                        + " encountered in configureNode(), node " + getID());
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
     * check if node can be safely reset.
     *
     * @return if node can be reset.
     */
    @Override
    boolean isResetable() {
        return (getState().equals(State.EXECUTED)
                || getState().equals(State.MARKEDFOREXEC)
                || getState().equals(State.CONFIGURED) || getState().equals(
                State.UNCONFIGURED_MARKEDFOREXEC));
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
                throw new IllegalStateException("Illegal state " + getState()
                        + " encountered in resetNode().");
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
                    throw new IllegalStateException("Illegal state "
                            + getState()
                            + " encountered in markForExecution(true).");
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
                    throw new IllegalStateException("Illegal state "
                            + getState()
                            + " encountered in markForExecution(false).");
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
                throw new IllegalStateException("Illegal state " + getState()
                        + " encountered in enableReQueuing().");
            }
        }
    }
    
    /**
     * Change state of marked (for execution) node to queued once it has been
     * assigned to a NodeExecutionJobManager.
     *
     * @param inData the incoming data for the execution
     * @throws IllegalStateException in case of illegal entry state.
     */
    void queue(final PortObject[] inData) {
        synchronized (m_nodeMutex) {
            switch (getState()) {
            case MARKEDFOREXEC:
                setState(State.QUEUED);
                NodeExecutionJobManager jobManager = findJobManager();
                try {
                    ExecutionContext execCon = createExecutionContext();
                    NodeExecutionJob job = 
                        jobManager.submitJob(this, inData, execCon);
                    setExecutionJob(job);
                } catch (Throwable t) {
                    String error = "Failed to submit job to job executor \"" 
                        + jobManager + "\": " + t.getMessage();
                    setNodeMessage(new NodeMessage(
                            NodeMessage.Type.ERROR, error));
                    LOGGER.error(error, t);
                    try {
                        performBeforeExecuteNode();
                    } catch (IllegalContextStackObjectException e) {
                        // have something more serious to deal with
                    }
                    performAfterExecuteNode(false);
                }
                return;
            default:
                throw new IllegalStateException("Illegal state " + getState()
                        + " encountered in queueNode(). Node "
                        + getNameWithID());
            }
        }
    }


    /** Cancel execution of a marked, queued, or executing node. (Tolerate
     * execute as this may happen throughout cancelation).
     *
     * @throws IllegalStateException
     */
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
                m_progressMonitor.setExecuteCanceled();
                NodeExecutionJob job = getExecutionJob();
                assert job != null : "node is queued but no job represents "
                    + "the execution task (is null)";
                job.cancel();
                setState(State.CONFIGURED);
                break;
            case EXECUTING:
                // future is running in thread pool, use ordinary cancel policy
                m_progressMonitor.setExecuteCanceled();
                job = getExecutionJob();
                assert job != null : "node is queued but no job represents "
                    + "the execution task (is null)";
                job.cancel();
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

    /**
     * This should be used to change the nodes states correctly (and likely
     * needs to be synchronized with other changes visible to successors of this
     * node as well!) BEFORE the actual execution. The main reason is that the
     * actual execution should be performed unsychronized!
     */
    void preExecuteNode() {
        synchronized (m_nodeMutex) {
            switch (getState()) {
            case QUEUED:
                // clear loop status
                m_node.clearLoopStatus();
                // change state to avoid more than one executor
                setState(State.EXECUTING);
                break;
            default:
                throw new IllegalStateException("Illegal state " + getState()
                        + " encountered in executeNode(), node: " + getID());
            }
        }
    }

    /**
     * This should be used to change the nodes states correctly (and likely
     * needs to be synchronized with other changes visible to successors of this
     * node as well!) AFTER the actual execution. The main reason is that the
     * actual execution should be performed unsychronized!
     *
     * @param success indicates if execution was successful
     */
    void postExecuteNode(final boolean success) {
        synchronized (m_nodeMutex) {
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
        }
    }
    
    /**
     * Invoked by the job executor immediately before the execution is
     * triggered. It invokes doBeforeExecution on the parent.
     *
     * @throws IllegalContextStackObjectException in case of wrongly connected
     *             loops, for instance.
     */
    void performBeforeExecuteNode() {
        // this will allow the parent to call state changes etc properly
        // synchronized. The main execution is done asynchronously.
        try {
            getParent().doBeforeExecution(SingleNodeContainer.this);
        } catch (IllegalContextStackObjectException e) {
            LOGGER.warn(e.getMessage());
            setNodeMessage(new NodeMessage(
                    NodeMessage.Type.ERROR, e.getMessage()));
            throw e;
        }
    }
    
    /**
     * Execute underlying Node asynchronously. Make sure to give Workflow-
     * Manager a chance to call pre- and postExecuteNode() appropriately and
     * synchronize those parts (since they changes states!).
     *
     * @param inObjects input data
     * @param ec The execution context for progress, e.g.
     * @return whether execution was successful. 
     * @throws IllegalStateException in case of illegal entry state.
     */
    public boolean performExecuteNode(final PortObject[] inObjects,
            final ExecutionContext ec) {
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
            if (!m_node.configure(specs)) {
                LOGGER.error("Configure failed after Execute failed!");
            }
            // TODO don't remove the stack conflict message (if any)
            setNodeMessage(orgMessage);
        }
        return success;
    }
    
    /**
     * Called immediately after the execution took place in the job executor. It
     * will trigger an doAfterExecution on the parent wfm.
     *
     * @param success Whether the execution was successful.
     */
    void performAfterExecuteNode(final boolean success) {
        // clean up stuff and especially change states synchronized again
        getParent().doAfterExecution(SingleNodeContainer.this, success);
    }
    
    /** Hook to insert new port objects into this node. This method is used,
     * for instance in grid execution in order to load the results into the
     * node instance. 
     * @param outData The new output data.
     * @return If that's successful (false if for instance, elements are null
     * or incompatible)
     */
    public boolean loadExecutionResult(final PortObject[] outData) {
        return m_node.loadExecutionResult(outData, false);
    }
    
    /** Set a node message on this node. This method should be used when
     * the node is executed remotely, i.e. the local Node instance is not
     * used for the calculation but should represent a calculation result.
     * @param message A message that should be shown at the node. Must not be
     * null (use a reset message instead).
     * @throws NullPointerException If the argument is null 
     */ 
    public void loadNodeMessage(final NodeMessage message) {
        m_node.loadNodeMessage(message);
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
            try {
                loadSNCSettings(persistor.getSNCSettings());
            } catch (InvalidSettingsException e) {
                String error = "Error loading SNC settings: " + e.getMessage();
                LOGGER.warn(error, e);
                result.addError(error);
            }
        }
        return result;
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

    ////////////////////////
    // Progress forwarding
    ////////////////////////

    /**
     * {@inheritDoc}
     */
    public void progressChanged(final NodeProgressEvent pe) {
        // set our ID as source ID
        NodeProgressEvent event =
                new NodeProgressEvent(getID(), pe.getNodeProgress());
        // forward the event
        notifyProgressListeners(event);
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
        return m_node.areDialogAndNodeSettingsEqual();
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
    public static class SingleNodeContainerSettings {

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
