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
import org.knime.core.node.exec.ThreadNodeExecutionJobManager;
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
public final class SingleNodeContainer extends NodeContainer implements
        NodeMessageListener, NodeProgressListener {

    /** my logger. */
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(SingleNodeContainer.class);

    /** underlying node. */
    private final Node m_node;

    /**
     * remember ID of the job when this node is submitted to a
     * NodeExecutionJobManager.
     */
    private NodeExecutionJob m_executionJob;

    /** progress monitor. */
    private final NodeProgressMonitor m_progressMonitor =
            new DefaultNodeProgressMonitor(this);

    private SingleNodeContainerSettings m_settings =
        new SingleNodeContainerSettings();

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
        m_node.addMessageListener(this);
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
        m_node.addMessageListener(this);
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

    /* ------------------ Views ---------------- */

    /**
     * Set a new HiLiteHandler for an incoming connection.
     *
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
    public void setJobExecutor(final NodeExecutionJobManager je) {
        if (getState().equals(State.EXECUTING)
                || getState().equals(State.QUEUED)) {
            throw new IllegalStateException("Illegal state " + getState()
                    + " in setJobExecutor - can not change a running node.");

        }
        super.setJobExecutor(je);
    }

    private ExecutionContext createExecutionContext() {
        m_progressMonitor.reset();
        return new ExecutionContext(m_progressMonitor, getNode(), getParent()
                .getGlobalTableRepository());
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
    @Override
    boolean configureAsNodeContainer(final PortObjectSpec[] inObjectSpecs) {
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
    boolean isResetableAsNodeContainer() {
        return (getState().equals(State.EXECUTED)
                || getState().equals(State.MARKEDFOREXEC)
                || getState().equals(State.CONFIGURED) || getState().equals(
                State.UNCONFIGURED_MARKEDFOREXEC));
    }

    /** {@inheritDoc} */
    @Override
    void resetAsNodeContainer() {
        synchronized (m_nodeMutex) {
            switch (getState()) {
            case EXECUTED:
                m_node.reset(true);
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
                m_node.reset(true);
                setState(State.IDLE);
                return;
            default:
                throw new IllegalStateException("Illegal state " + getState()
                        + " encountered in resetNode().");
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    void markForExecutionAsNodeContainer(final boolean flag) {
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
    void queueAsNodeContainer(final PortObject[] inData) {
        synchronized (m_nodeMutex) {
            switch (getState()) {
            case MARKEDFOREXEC:
                setState(State.QUEUED);
                ExecutionContext execCon = createExecutionContext();
                m_executionJob = findJobExecutor().submitJob(
                        this, inData, execCon);
                return;
            default:
                throw new IllegalStateException("Illegal state " + getState()
                        + " encountered in queueNode(). Node "
                        + getNameWithID());
            }
        }
    }


    /** {@inheritDoc} */
    @Override
    void cancelExecutionAsNodeContainer() {
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
                m_executionJob.cancel();
                setState(State.CONFIGURED);
                break;
            case EXECUTING:
                // future is running in thread pool, use ordinary cancel policy
                m_progressMonitor.setExecuteCanceled();
                m_executionJob.cancel();
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
                m_node.reset(false);  // we need to clean up remaining nonsense
                m_node.clearLoopStatus();  // ...and the loop status
                // but node will not be reconfigured!
                // (configure does not prepare execute but only tells us what
                //  output execute() may create hence we do not need it here)
                setState(State.CONFIGURED);
            }
            m_executionJob = null;
        }
    }
    
    /**
     * Invoked by the job executor immediately before the execution is
     * triggered. It invokes doBeforeExecution on the parent.
     * @throws IllegalContextStackObjectException in case of wrongly
     *         connected loops, for instance.
     */
    void performBeforeExecuteNode() {
        // this will allow the parent to call state changes etc properly
        // synchronized. The main execution is done asynchronously.
        try {
            getParent().doBeforeExecution(SingleNodeContainer.this);
        } catch (IllegalContextStackObjectException e) {
            LOGGER.warn(e.getMessage());
            m_node.notifyMessageListeners(
                    new NodeMessage(NodeMessage.Type.ERROR, e.getMessage()));
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
            m_node.notifyMessageListeners(new NodeMessage(
                    NodeMessage.Type.WARNING, errorString));
            success = false;
        }
        // execute node outside any synchronization!
        success = success && m_node.execute(inObjects, ec);
        if (success) {
            // output tables are made publicly available (for blobs)
            putOutputTablesIntoGlobalRepository(ec);
        }
        return success;
    }

    /** Called immediately after the execution took place in the job executor.
     * It will trigger an doAfterExecution on the parent wfm.
     * @param success Whether the execution was successful.
     */
    void performAfterExecuteNode(final boolean success) {
        // clean up stuff and especially change states synchronized again
        getParent().doAfterExecution(SingleNodeContainer.this, success);
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

    // //////////////////////////////////////
    // Save & Load Settings and Content
    // //////////////////////////////////////

    /** {@inheritDoc} */
    @Override
    void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        synchronized (m_nodeMutex) {
            m_node.loadSettingsFrom(settings);
            m_settings = new SingleNodeContainerSettings(settings);
            setDirty();
        }
    }

    /** {@inheritDoc} */
    @Override
    LoadResult loadContent(final NodeContainerPersistor nodePersistor,
            final Map<Integer, BufferedDataTable> tblRep,
            final ScopeObjectStack inStack, final ExecutionMonitor exec)
            throws CanceledExecutionException {
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
            return new LoadResult();
        }
    }

    /** {@inheritDoc} */
    @Override
    void saveSettings(final NodeSettingsWO settings)
    throws InvalidSettingsException {
        m_node.saveSettingsTo(settings);
        m_settings.save(settings);
    }

    /** {@inheritDoc} */
    @Override
    boolean areSettingsValid(final NodeSettingsRO settings) {
        try {
            new SingleNodeContainerSettings(settings);
        } catch (InvalidSettingsException ise) {
            return false;
        }
        return m_node.areSettingsValid(settings);
    }

    /**
     * Returns the settings for the job manager. This could be an empty settings
     * object (generated by this class) - it won't be null.
     *
     * @return the settings for the currently set job manager
     */
    public NodeSettingsRO getJobManagerSettings() {
        return m_settings.getjobManagerSettings();
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

    /**
     * @return Node name with status information.
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return m_node.getName() + "(" + getID() + ")" + ";status:" + getState();
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
        return m_node.getDialogPaneWithSettings(inSpecs, stack);
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
    void loadSettingsFromDialog() throws InvalidSettingsException {
        synchronized (m_nodeMutex) {
            m_node.loadSettingsFromDialog();
        }
    }

    /** {@inheritDoc} */
    @Override
    public NodeMessage getNodeMessage() {
        return m_node.getNodeMessage();
    }

    /** {@inheritDoc} */
    public void messageChanged(final NodeMessageEvent messageEvent) {
        notifyMessageListeners(messageEvent);
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

        private static final String CFG_SNC_SETTINGS =
                "internal_single_node_container_settings";

        private static final String CFG_JOB_MANAGER_ID = "JobManagerID";

        private static final String CFG_JOB_MANAGER_SETTINGS =
                "JobManagerSettings";

        // the threaded job manager is the default
        private String m_jobManagerID =
            ThreadNodeExecutionJobManager.INSTANCE.getID();

        // the default manager has no settings
        private NodeSettingsRO m_jobManagerSettings =
                new NodeSettings("empty");

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

            // since this settings object exists only since 2.0, we stay with
            // the default if there are no node container settings.
            if (settings.containsKey(CFG_SNC_SETTINGS)) {
                NodeSettingsRO sncSettings =
                        settings.getNodeSettings(CFG_SNC_SETTINGS);
                m_jobManagerID = sncSettings.getString(CFG_JOB_MANAGER_ID);
                m_jobManagerSettings =
                        sncSettings.getNodeSettings(CFG_JOB_MANAGER_SETTINGS);
            }

        }

        /**
         * Writes the current settings values into the passed argument.
         *
         * @param settings the object to write the settings into.
         */
        public void save(final NodeSettingsWO settings) {
            NodeSettingsWO sncSettings =
                    settings.addNodeSettings(CFG_SNC_SETTINGS);
            sncSettings.addString(CFG_JOB_MANAGER_ID, m_jobManagerID);

            NodeSettingsWO foo =
                sncSettings.addNodeSettings(CFG_JOB_MANAGER_SETTINGS);
            m_jobManagerSettings.copyTo(foo);
        }

        /**
         * Returns the ID of the job manager.
         *
         * @return the ID of the job manager
         */
        public String getJobManagerID() {
            return m_jobManagerID;
        }

        /**
         * Returns the settings for the job manager. This could be an empty
         * settings object (generated by this class) - it won't be null.
         *
         * @return the settings for the job manager
         */
        public NodeSettingsRO getjobManagerSettings() {
            return m_jobManagerSettings;
        }

        /**
         * Stores a new selected job manager with its settings.
         *
         * @param id the id of the new job manager
         * @param jobManagerSettings the settings for the specified job manager.
         *            Could be null, if the manager doesn't need any settings.
         *
         */
        public void setJobManager(final String id,
                final NodeSettingsRO jobManagerSettings) {
            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("Job manager ID can't "
                        + "be null or empty");
            }
            NodeSettingsRO settings = jobManagerSettings;
            if (settings == null) {
                settings = new NodeSettings(id);
            }
            m_jobManagerID = id;
            m_jobManagerSettings = settings;
        }

    }

}
