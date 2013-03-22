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
 *   14.03.2007 (mb): created
 */
package org.knime.core.node.workflow;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.filestore.internal.FileStoreHandlerRepository;
import org.knime.core.data.filestore.internal.IFileStoreHandler;
import org.knime.core.data.filestore.internal.ILoopStartWriteFileStoreHandler;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.filestore.internal.LoopEndWriteFileStoreHandler;
import org.knime.core.data.filestore.internal.LoopStartReferenceWriteFileStoreHandler;
import org.knime.core.data.filestore.internal.LoopStartWritableFileStoreHandler;
import org.knime.core.data.filestore.internal.ReferenceWriteFileStoreHandler;
import org.knime.core.data.filestore.internal.WorkflowFileStoreHandlerRepository;
import org.knime.core.data.filestore.internal.WriteFileStoreHandler;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DataAwareNodeDialogPane;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodePostConfigure;
import org.knime.core.node.NodeProgressMonitor;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.exec.ThreadNodeExecutionJobManager;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.workflow.FlowVariable.Scope;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.execresult.NodeContainerExecutionResult;
import org.knime.core.node.workflow.execresult.NodeContainerExecutionStatus;
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

    /** Name of the sub-directory containing node-local files. These files
     * manually copied by the user and the node will automatically list those
     * files as node-local flow variables in its configuration dialog.
     */
    public static final String DROP_DIR_NAME = "drop";

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
            getOutPort(i).setPortName(m_node.getOutportDescriptionName(i));
        }
        for (int i = 0; i < getNrInPorts(); i++) {
            getInPort(i).setPortName(m_node.getInportDescriptionName(i));
        }
    }

    /** Get the underlying node.
     * @return the underlying Node
     */
    public Node getNode() {
        return m_node;
    }

    /**
     * @return reference to underlying node.
     * @deprecated Method is going to be removed in future versions. Use
     * {@link #getNode()} instead.
     * Currently used to enable workaround for bug #2136 (see also bug #2137)
     */
    @Deprecated
    public Node getNodeReferenceBug2136() {
        return getNode();
    }

    /** @return reference to underlying node's model. */
    public NodeModel getNodeModel() {
        return getNode().getNodeModel();
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
     * @noreference This method is not intended to be referenced by clients.
     */
    public final MemoryPolicy getOutDataMemoryPolicy() {
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
    public AbstractNodeView<NodeModel> getNodeView(final int i) {
        String title = getNameWithID() + " (" + getViewName(i) + ")";
        String customName = getDisplayCustomLine();
        if (!customName.isEmpty()) {
            title += " - " + customName;
        }
        return (AbstractNodeView<NodeModel>)m_node.getView(i, title);
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
        clearFileStoreHandler();
        if (m_outputPorts != null) {
            for (NodeOutPort p : m_outputPorts) {
                if (p != null) {
                    p.disposePortView();
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    void setJobManager(final NodeExecutionJobManager je) {
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
            boolean prevInactivity = isInactive();
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
            // if state stayed the same but inactivity of node changed
            // fake a state change to make sure it's displayed properly
            if (prevInactivity != isInactive()) {
                State oldSt = this.getState();
                setState(State.IDLE.equals(oldSt) ? State.CONFIGURED
                        : State.IDLE);
                setState(oldSt);
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
     * Calls configure in the node, allowing the current job manager to modify
     * the output specs according to its settings (in case it modifies the
     * node's output).
     *
     * @param inSpecs the input specs to node configure
     * @return true of configure succeeded.
     */
    private boolean nodeConfigure(final PortObjectSpec[] inSpecs) {

        final NodeExecutionJobManager jobMgr = findJobManager();

        NodePostConfigure npc = new NodePostConfigure() {
            /** {inheritDoc} */
            @Override
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

    /** {@inheritDoc} */
    @Override
    boolean canPerformReset() {
        synchronized (m_nodeMutex) {
            switch (getState()) {
            case EXECUTED:
                return true;
            default:
                return false;
            }
        }
    }

    /** Reset underlying node and update state accordingly.
     * @throws IllegalStateException in case of illegal entry state.
     */
    void reset() {
        synchronized (m_nodeMutex) {
            switch (getState()) {
            case EXECUTED:
                m_node.reset();
                clearFileStoreHandler();
                cleanOutPorts(false);
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
                /*
                 * Also configured nodes must be reset in order to handle
                 * nodes subsequent to meta nodes with through-connections.
                 */
                m_node.reset();
                clearFileStoreHandler();
                setState(State.IDLE);
                return;
            default:
                throwIllegalStateException();
            }
        }
    }

    /** Cleans outports, i.e. sets fields to null, calls clear() on BDT.
     * Usually happens as part of a reset() (except for loops that have
     * their body not reset between iterations.
     * @param isLoopRestart See {@link Node#cleanOutPorts(boolean)}. */
    void cleanOutPorts(final boolean isLoopRestart) {
        m_node.cleanOutPorts(isLoopRestart);
        if (!isLoopRestart) {
            // this should have no affect as m_node.cleanOutPorts() will remove
            // all tables already
            int nrRemovedTables = removeOutputTablesFromGlobalRepository();
            assert nrRemovedTables == 0 : nrRemovedTables + " tables in global "
                + "repository after node cleared outports (expected 0)";
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
     * Mark underlying, executed node so that it can be re-executed
     * (= update state accordingly). Used in loops to execute start
     * more than once and when reset/configure is skipped in loop body.
     *
     * @throws IllegalStateException in case of illegal entry state.
     */
    void markForReExecutionInLoop() {
        synchronized (m_nodeMutex) {
            switch (getState()) {
            case EXECUTED:
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

    /** {@inheritDoc} */
    @Override
    void performShutdown() {
        synchronized (m_nodeMutex) {
            NodeExecutionJob job = getExecutionJob();
            if (job != null && job.isSavedForDisconnect()) {
                assert getState().executionInProgress();
                findJobManager().disconnect(job);
            } else if (getState().executionInProgress()) {
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
            case PREEXECUTE: // in case of errors, e.g. flow stack problems
                             // encountered during doBeforeExecution
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
    void mimicRemoteExecuted(final NodeContainerExecutionStatus status) {
        boolean success = status.isSuccess();
        synchronized (m_nodeMutex) {
            switch (getState()) {
            case POSTEXECUTE:
                setState(success ? State.EXECUTED : State.IDLE);
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
    boolean performStateTransitionPREEXECUTE() {
        synchronized (m_nodeMutex) {
            getProgressMonitor().reset();
            switch (getState()) {
            case QUEUED:
                setState(State.PREEXECUTE);
                return true;
            default:
                // ignore any other state: other states indicate that the node
                // was canceled before it is actually run
                // (this method is called from a worker thread, whereas cancel
                // typically from the UI thread)
                if (!Thread.currentThread().isInterrupted()) {
                    LOGGER.debug("Execution of node " + getNameWithID()
                            + " was probably canceled (node is " + getState()
                            + " during 'preexecute') but calling thread is not"
                            + " interrupted");
                }
                return false;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    void performStateTransitionEXECUTING() {
        synchronized (m_nodeMutex) {
            switch (getState()) {
            case PREEXECUTE:
                m_node.clearLoopContext();
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
            case PREEXECUTE: // in case of errors, e.g. flow stack problems
                             // encountered during doBeforeExecution
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
    void performStateTransitionEXECUTED(
            final NodeContainerExecutionStatus status) {
        synchronized (m_nodeMutex) {
            switch (getState()) {
            case POSTEXECUTE:
                IFileStoreHandler fsh = m_node.getFileStoreHandler();
                if (fsh instanceof IWriteFileStoreHandler) {
                    ((IWriteFileStoreHandler)fsh).close();
                } else {
                    // can be null if run through 3rd party executor
                    // might be not an IWriteFileStoreHandler if restored loop is executed
                    // (this will result in a failure before model#execute is called)
                    assert !status.isSuccess() || fsh == null
                    : "must not be " + fsh.getClass().getSimpleName() + " in execute";
                    LOGGER.debug("Can't close file store handler, not writable: "
                            + (fsh == null ? "<null>" : fsh.getClass().getSimpleName()));
                }
                if (status.isSuccess()) {
                    if (m_node.getLoopContext() == null) {
                        setState(State.EXECUTED);
                    } else {
                        // loop not yet done - "stay" configured until done.
                        assert getLoopStatus().equals(LoopStatus.RUNNING);
                        setState(State.MARKEDFOREXEC);
                    }
                } else {
                    // node will be configured in doAfterExecute.
                    // for now we assume complete failure and clean up (reset)
                    // We do keep the message, though.
                    NodeMessage oldMessage = getNodeMessage();
                    m_node.reset();
                    clearFileStoreHandler();
                    setNodeMessage(oldMessage);
                    setState(State.IDLE);
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
    public NodeContainerExecutionStatus performExecuteNode(
            final PortObject[] inObjects) {
        IWriteFileStoreHandler fsh =
            initFileStore(getParent().getFileStoreHandlerRepository());
        m_node.setFileStoreHandler(fsh);
        // this call requires the FSH to be set on the node (ideally would take
        // it as an argument but createExecutionContext became API unfortunately)
        ExecutionContext ec = createExecutionContext();
        fsh.open(ec);

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
            // a solid state again will be done by WorkflowManager (in
            // doAfterExecute().
        }
        return success ? NodeContainerExecutionStatus.SUCCESS
                : NodeContainerExecutionStatus.FAILURE;
    }

    private IWriteFileStoreHandler initFileStore(
            final WorkflowFileStoreHandlerRepository fileStoreHandlerRepository) {
        FlowLoopContext upstreamFLC = getFlowObjectStack().peekScopeContext(FlowLoopContext.class, false);
        NodeID outerStartNodeID = upstreamFLC == null ? null : upstreamFLC.getHeadNode();
        // loop start nodes will put their loop context on the outgoing flow object stack
        assert !getID().equals(outerStartNodeID) : "Loop start on incoming flow stack can't be node itself";

        FlowLoopContext innerFLC = getOutgoingFlowObjectStack().peekScopeContext(FlowLoopContext.class, false);
        NodeID innerStartNodeID = innerFLC == null ? null : innerFLC.getHeadNode();
        // if there is a loop context on this node's stack, this node must be the start
        assert !(this.isModelCompatibleTo(LoopStartNode.class)) || getID().equals(innerStartNodeID);

        IFileStoreHandler oldFSHandler = m_node.getFileStoreHandler();
        IWriteFileStoreHandler newFSHandler;

        if (innerFLC == null && upstreamFLC == null) {
            // node is not a start node and not contained in a loop
            if (oldFSHandler instanceof IWriteFileStoreHandler) {
                clearFileStoreHandler();
                assert false : "Node " + getNameWithID() + " must not have file store handler at this point (not a "
                    + "loop start and not contained in loop), disposing old handler";
            }
            newFSHandler = new WriteFileStoreHandler(getNameWithID(), UUID.randomUUID());
            newFSHandler.addToRepository(fileStoreHandlerRepository);
        } else if (innerFLC != null) {
            // node is a loop start node
            int loopIteration = innerFLC.getIterationIndex();
            if (loopIteration == 0) {
                if (oldFSHandler instanceof IWriteFileStoreHandler) {
                    assert false : "Loop Start " + getNameWithID() + " must not have file store handler at this point "
                        + "(no iteration ran), disposing old handler";
                    clearFileStoreHandler();
                }
                if (upstreamFLC != null) {
                    ILoopStartWriteFileStoreHandler upStreamFSHandler = upstreamFLC.getFileStoreHandler();
                    newFSHandler = new LoopStartReferenceWriteFileStoreHandler(upStreamFSHandler, innerFLC);
                } else {
                    newFSHandler = new LoopStartWritableFileStoreHandler(getNameWithID(), UUID.randomUUID(), innerFLC);
                }
                newFSHandler.addToRepository(fileStoreHandlerRepository);
                innerFLC.setFileStoreHandler((ILoopStartWriteFileStoreHandler)newFSHandler);
            } else {
                assert oldFSHandler instanceof IWriteFileStoreHandler : "Loop Start " + getNameWithID()
                    + " must have file store handler in iteration " + loopIteration;
                newFSHandler = (IWriteFileStoreHandler)oldFSHandler;
                // keep the old one
            }
        } else {
            // ordinary node contained in loop
            assert innerFLC == null && upstreamFLC != null;
            ILoopStartWriteFileStoreHandler upStreamFSHandler = upstreamFLC.getFileStoreHandler();
            if (this.isModelCompatibleTo(LoopEndNode.class)) {
                if (upstreamFLC.getIterationIndex() > 0) {
                    newFSHandler = (IWriteFileStoreHandler)oldFSHandler;
                } else {
                    newFSHandler = new LoopEndWriteFileStoreHandler(upStreamFSHandler);
                    newFSHandler.addToRepository(fileStoreHandlerRepository);
                }
            } else {
                newFSHandler = new ReferenceWriteFileStoreHandler(upStreamFSHandler);
                newFSHandler.addToRepository(fileStoreHandlerRepository);
            }
        }
        return newFSHandler;
    }

    /** Disposes file store handler (if set) and sets it to null. Called from reset and cleanup.
     * @noreference This method is not intended to be referenced by clients. */
    public void clearFileStoreHandler() {
        IFileStoreHandler fileStoreHandler = m_node.getFileStoreHandler();
        if (fileStoreHandler != null) {
            fileStoreHandler.clearAndDispose();
            m_node.setFileStoreHandler(null);
        }
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
    private int removeOutputTablesFromGlobalRepository() {
        HashMap<Integer, ContainerTable> globalRep =
            getParent().getGlobalTableRepository();
        return m_node.removeOutputTablesFromGlobalRepository(globalRep);
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
            SingleNodeContainerPersistor persistor =
                (SingleNodeContainerPersistor)nodePersistor;
            State state = persistor.getMetaPersistor().getState();
            setState(state, false);
            if (state.equals(State.EXECUTED)) {
                m_node.putOutputTablesIntoGlobalRepository(getParent()
                        .getGlobalTableRepository());
            }
            final FlowObjectStack outgoingStack = new FlowObjectStack(getID());
            for (FlowObject s : persistor.getFlowObjects()) {
                outgoingStack.push(s);
            }
            setFlowObjectStack(inStack, outgoingStack);
            SingleNodeContainerSettings sncSettings =
                persistor.getSNCSettings();
            if (sncSettings == null) {
                LOGGER.coding(
                        "SNC settings from persistor are null, using default");
                sncSettings = new SingleNodeContainerSettings();
            }
            m_settings = sncSettings;
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void loadExecutionResult(
            final NodeContainerExecutionResult execResult,
            final ExecutionMonitor exec, final LoadResult loadResult) {
        synchronized (m_nodeMutex) {
            if (State.EXECUTED.equals(getState())) {
                LOGGER.debug(getNameWithID()
                        + " is alredy executed; won't load execution result");
                return;
            }
            if (!(execResult instanceof SingleNodeContainerExecutionResult)) {
                throw new IllegalArgumentException("Argument must be instance "
                        + "of \"" + SingleNodeContainerExecutionResult.
                        class.getSimpleName() + "\": "
                        + execResult.getClass().getSimpleName());
            }
            super.loadExecutionResult(execResult, exec, loadResult);
            SingleNodeContainerExecutionResult sncExecResult =
                (SingleNodeContainerExecutionResult)execResult;
            NodeExecutionResult nodeExecResult =
                sncExecResult.getNodeExecutionResult();
            boolean success = sncExecResult.isSuccess();
            if (success) {
                m_node.loadDataAndInternals(
                        nodeExecResult, new ExecutionMonitor(), loadResult);
            }
            boolean needsReset = nodeExecResult.needsResetAfterLoad();
            if (!needsReset && success) {
                for (int i = 0; i < getNrOutPorts(); i++) {
                    if (m_node.getOutputObject(i) == null) {
                        loadResult.addError(
                                "Output object at port " + i + " is null");
                        needsReset = true;
                    }
                }
            }
            if (needsReset) {
                execResult.setNeedsResetAfterLoad();
            }
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
    // Credentials handling
    ////////////////////////////////////

    /** Set credentials store on this node. It will clear usage history to
     * previously accessed credentials (client usage in credentials, see
     * {@link Credentials}) and set a new provider on the underlying node, which
     * has this node as client.
     * @param store The new store to set.
     * @see Node#setCredentialsProvider(CredentialsProvider)
     * @throws NullPointerException If the argument is null.
     */
    void setCredentialsStore(final CredentialsStore store) {
        CredentialsProvider oldProvider = m_node.getCredentialsProvider();
        if (oldProvider != null) {
            oldProvider.clearClientHistory();
            if (oldProvider.getClient() == this
                    && oldProvider.getStore() == store) {
                // no change
                return;
            }
        }
        m_node.setCredentialsProvider(new CredentialsProvider(this, store));
    }

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
    void setFlowObjectStack(final FlowObjectStack st,
            final FlowObjectStack outgoingStack) {
        synchronized (m_nodeMutex) {
            pushNodeDropDirURLsOntoStack(st);
            m_node.setFlowObjectStack(st, outgoingStack);
        }
    }

    private void pushNodeDropDirURLsOntoStack(final FlowObjectStack st) {
        ReferencedFile refDir = getNodeContainerDirectory();
        ReferencedFile dropFolder = refDir == null ? null
                : new ReferencedFile(refDir, DROP_DIR_NAME);
        if (dropFolder == null) {
            return;
        }
        dropFolder.lock();
        try {
            File directory = dropFolder.getFile();
            if (!directory.exists()) {
                return;
            }
            String[] files = directory.list();
            if (files != null) {
                StringBuilder debug = new StringBuilder(
                        "Found " + files.length + " node local file(s) to "
                        + getNameWithID() + ": ");
                debug.append(Arrays.toString(Arrays.copyOf(files, Math.max(3, files.length))));
                for (String f : files) {
                    File child = new File(directory, f);
                    try {
                        st.push(new FlowVariable(
                                Scope.Local.getPrefix() + "(drop) " + f,
//                                child.getAbsolutePath(), Scope.Local));
                                child.toURI().toURL().toString(), Scope.Local));
//                    } catch (Exception mue) {
                    } catch (MalformedURLException mue) {
                        LOGGER.warn("Unable to process drop file", mue);
                    }
                }
            }
        } finally {
            dropFolder.unlock();
        }
    }

    /**
     * @return current FlowObjectStack
     */
    FlowObjectStack getFlowObjectStack() {
        synchronized (m_nodeMutex) {
            return m_node.getFlowObjectStack();
        }
    }


    /** Delegates to node to get flow variables that are added or modified
     * by the node.
     * @return The list of outgoing flow variables.
     * @see org.knime.core.node.Node#getOutgoingFlowObjectStack()
     */
    public FlowObjectStack getOutgoingFlowObjectStack() {
        synchronized (m_nodeMutex) {
            return m_node.getOutgoingFlowObjectStack();
        }
    }

    /** Check if the given node is part of a scope (loop, try/catch...).
     *
     * @return true if node is part of a scope context.
     */
    public boolean isMemberOfScope() {
        synchronized (m_nodeMutex) {
            // we need to check if either a FlowScopeObject is on the stack or of
            // the node is the end of a Scope (since those don't have their own
            // scope object on the outgoing stack anymore.
            FlowObjectStack fos = m_node.getFlowObjectStack();
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
     * @param nodeModelClass
     * @return return true if underlying NodeModel implements the given class/interface
     */
    public boolean isModelCompatibleTo(final Class<?> nodeModelClass) {
        return this.getNode().isModelCompatibleTo(nodeModelClass);
    }

    /**
     * @see NodeModel#resetAndConfigureLoopBody()
     */
    boolean resetAndConfigureLoopBody() {
        return getNode().resetAndConfigureLoopBody();
    }

    /** enable (or disable) that after the next execution of this loop end node
     * the execution will be halted. This can also be called on a paused node
     * to trigger a "single step" execution.
     *
     * @param enablePausing if true, pause is enabled. Otherwise disabled.
     */
    void pauseLoopExecution(final boolean enablePausing) {
        if (getState().executionInProgress()) {
            getNode().setPauseLoopExecution(enablePausing);
        }
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
    public final boolean hasDataAwareDialogPane() {
        return m_node.hasDialog()
        && m_node.getDialogPane() instanceof DataAwareNodeDialogPane;
    }

    /** {@inheritDoc} */
    @Override
    NodeDialogPane getDialogPaneWithSettings(final PortObjectSpec[] inSpecs,
            final PortObject[] inData) throws NotConfigurableException {
        NodeSettings settings = new NodeSettings(getName());
        saveSettings(settings);
        return m_node.getDialogPaneWithSettings(inSpecs, inData, settings,
                getParent().isWriteProtected());
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

    /** Get the tables that are kept by the underlying node. The return value
     * is null if (a) the underlying node is not a
     * {@link org.knime.core.node.BufferedDataTableHolder} or (b) the node
     * is not executed.
     * @return The internally held tables.
     * @see org.knime.core.node.BufferedDataTableHolder
     * @see Node#getInternalHeldTables()
     */
    public BufferedDataTable[] getInternalHeldTables() {
        return getNode().getInternalHeldTables();
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
     * @return true if configure or execute were skipped because node is
     *   part of an inactive branch.
     * @see Node#isInactive()
     */
    public boolean isInactive() {
        return m_node.isInactive();
    }

    /** @return <code>true</code> if the underlying node is able to consume
     * inactive objects (implements
     * {@link org.knime.core.node.port.inactive.InactiveBranchConsumer}).
     * @see {@link Node#isInactiveBranchConsumer()}
     */
    public boolean isInactiveBranchConsumer() {
        return m_node.isInactiveBranchConsumer();
    }

    /** Possible loop states. */
    public static enum LoopStatus { NONE, RUNNING, PAUSED, FINISHED };
    /**
     * @return status of loop (determined from NodeState and LoopContext)
     */
    public LoopStatus getLoopStatus() {
        if (this.isModelCompatibleTo(LoopEndNode.class)) {
            if ((getNode().getLoopContext() != null)
                    || (getState().executionInProgress())) {
                if ((getNode().getPauseLoopExecution())
                        && (getState().equals(State.MARKEDFOREXEC))) {
                    return LoopStatus.PAUSED;
                } else {
                    return LoopStatus.RUNNING;
                }
            } else {
                return LoopStatus.FINISHED;
            }
        }
        return LoopStatus.NONE;
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
        // if-statement fixes bug 1777: ensureOpen can cause trouble if there
        // is a deep hierarchy of BDTs
        if (!isDirty()) {
            try {
                m_node.ensureOutputDataIsRead();
            } catch (Exception e) {
                LOGGER.error("Unable to read output data", e);
            }
        }
        super.setDirty();
    }

    /** {@inheritDoc} */
    @Override
    protected NodeContainerPersistor getCopyPersistor(
            final HashMap<Integer, ContainerTable> tableRep,
            final FileStoreHandlerRepository fileStoreHandlerRepository,
            final boolean preserveDeletableFlags,
            final boolean isUndoableDeleteCommand) {
        return new CopySingleNodeContainerPersistor(this,
                preserveDeletableFlags, isUndoableDeleteCommand);
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
