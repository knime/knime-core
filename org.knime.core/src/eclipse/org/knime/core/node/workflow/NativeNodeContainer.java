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
 * ---------------------------------------------------------------------
 *
 * Created on Sep 30, 2013 by Berthold
 */
package org.knime.core.node.workflow;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.filestore.internal.EmptyFileStoreHandler;
import org.knime.core.data.filestore.internal.IFileStoreHandler;
import org.knime.core.data.filestore.internal.ILoopStartWriteFileStoreHandler;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.data.filestore.internal.LoopEndWriteFileStoreHandler;
import org.knime.core.data.filestore.internal.LoopStartWriteFileStoreHandler;
import org.knime.core.data.filestore.internal.NestedLoopStartWriteFileStoreHandler;
import org.knime.core.data.filestore.internal.ReferenceWriteFileStoreHandler;
import org.knime.core.data.filestore.internal.WriteFileStoreHandler;
import org.knime.core.data.util.memory.InstanceCounter;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DataAwareNodeDialogPane;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.FileNodePersistor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeAndBundleInformationPersistor;
import org.knime.core.node.NodeConfigureHelper;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeProgressMonitor;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.interactive.InteractiveView;
import org.knime.core.node.interactive.ViewContent;
import org.knime.core.node.missing.MissingNodeModel;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.NodeExecutionJobManagerPool;
import org.knime.core.node.workflow.CredentialsStore.CredentialsNode;
import org.knime.core.node.workflow.FlowVariable.Scope;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.action.InteractiveWebViewsResult;
import org.knime.core.node.workflow.action.InteractiveWebViewsResult.Builder;
import org.knime.core.node.workflow.execresult.NativeNodeContainerExecutionResult;
import org.knime.core.node.workflow.execresult.NodeContainerExecutionResult;
import org.knime.core.node.workflow.execresult.NodeContainerExecutionStatus;
import org.knime.core.node.workflow.execresult.NodeExecutionResult;
import org.knime.core.node.workflow.virtual.parchunk.FlowVirtualScopeContext;
import org.knime.core.node.workflow.virtual.subnode.VirtualSubNodeInOut;
import org.knime.core.node.workflow.virtual.subnode.VirtualSubNodeInputNodeModel;
import org.knime.core.node.workflow.virtual.subnode.VirtualSubNodeOutputNodeModel;
import org.knime.shared.workflow.def.BaseNodeDef;
import org.knime.shared.workflow.def.NativeNodeDef;
import org.w3c.dom.Element;

/**
 * Implementation of {@link SingleNodeContainer} for a natively implemented KNIME Node relying
 * on a {@link NodeModel}.
 *
 * @author B. Wiswedel &amp; M.Berthold
 * @since 2.9
 */
public class NativeNodeContainer extends SingleNodeContainer {

    /** my logger. */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(NativeNodeContainer.class);

    /**
     * Instance counter used by the application health checkers etc. Not public API.
     * @noreference This field is not intended to be referenced by clients.set
     * @since 5.5
     */
    public static final InstanceCounter<NativeNodeContainer> INSTANCE_COUNTER =
        InstanceCounter.register(NativeNodeContainer.class);

    /** underlying node. */
    private final Node m_node;

    private NodeContainerOutPort[] m_outputPorts = null;


    private NodeInPort[] m_inputPorts = null;

    /** The bundle info with which the node was last executed with. Usually it's the latest installed
     * version but when a flow was executed, saved and loaded it's possibly "older". Used to address
     * bug 5207. This field is set when status changes to EXECUTED and set to null when reset. */
    private NodeAndBundleInformationPersistor m_nodeAndBundleInformation;

    private LoopStatusChangeHandler m_loopStatusChangeHandler;

    /** Used to exclude virtual nodes from copy operations. */
    public static final Predicate<NodeContainer> IS_VIRTUAL_IN_OUT_NODE = nc -> {
        if (nc instanceof NativeNodeContainer) {
            var nnc = (NativeNodeContainer)nc;
            return nnc.isModelCompatibleTo(VirtualSubNodeInputNodeModel.class)
                    || nnc.isModelCompatibleTo(VirtualSubNodeOutputNodeModel.class);
        }
        return false;
    };

    /**
     * Create new SingleNodeContainer based on existing Node.
     *
     * @param parent the workflow manager holding this node
     * @param n the underlying node
     * @param id the unique identifier
     */
    NativeNodeContainer(final WorkflowManager parent, final Node n, final NodeID id) {
        super(parent, id);
        m_node = n;
        postConstruct();
    }

    /**
     * Create new SingleNodeContainer from persistor.
     *
     * @param parent the workflow manager holding this node
     * @param id the identifier
     * @param persistor to read from
     */
    NativeNodeContainer(final WorkflowManager parent, final NodeID id, final NativeNodeContainerPersistor persistor) {
        super(parent, id, persistor.getMetaPersistor());
        m_node = persistor.getNode();
        if (getInternalState().isExecuted()) {
            m_nodeAndBundleInformation = persistor.getNodeAndBundleInformation();
        }
        assert m_node != null : persistor.getClass().getSimpleName()
                + " did not provide Node instance for "
                + getClass().getSimpleName() + " with id \"" + id + "\"";
        postConstruct();
    }

    /**
     * Create new NativeNode from def.
     *
     * @param parent the workflow manager holding this node
     * @param id the identifier
     * @param def native node definition
     * @param node node instance
     */
    NativeNodeContainer(final WorkflowManager parent, final NodeID id, final NativeNodeDef def, final Node node) {
        super(parent, id, def);
        m_node = node;
        CheckUtils.checkNotNull(m_node, "%s did not provide Node instance for %s with id \"%s\"",
            def.getNodeName(), getClass().getSimpleName(), id);
        postConstruct();
    }

    private void postConstruct() {
        getInternalState().incrementInStateCount();
        setPortNames();
        INSTANCE_COUNTER.track(this); // NOSONAR
        m_node.addMessageListener(new UnderlyingNodeMessageListener());
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
            NativeNodeContainer.this.setNodeMessage(messageEvent.getMessage());
        }
    }

    @Override
    boolean setInternalState(final InternalNodeContainerState state, final boolean setDirty) {
        getInternalState().decrementInStateCount();
        state.incrementInStateCount();
        return super.setInternalState(state, setDirty);
    }

    /** Get the underlying node.
     * @return the underlying Node
     */
    public Node getNode() {
        return m_node;
    }

    /** @return reference to underlying node's model. */
    public NodeModel getNodeModel() {
        return getNode().getNodeModel();
    }

    /* ------------------ Port Handling ------------- */

    /* */
    private void setPortNames() {
        for (int i = 0; i < getNrOutPorts(); i++) {
            getOutPort(i).setPortName(m_node.getOutportDescriptionName(i));
        }
        for (int i = 0; i < getNrInPorts(); i++) {
            getInPort(i).setPortName(m_node.getInportDescriptionName(i));
        }
    }

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
            m_inputPorts[index] = new NodeInPort(index, m_node.getInputType(index));
        }
        return m_inputPorts[index];
    }

    /* ------------------ Views ---------------- */

    /**
     * {@inheritDoc}
     */
    @Override
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
        NodeContext.pushContext(this);
        try {
            return (AbstractNodeView<NodeModel>)m_node.getView(i, title);
        } finally {
            NodeContext.removeLastContext();
        }
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

    @Override
    public InteractiveWebViewsResult getInteractiveWebViews() {
        Builder builder = InteractiveWebViewsResult.newBuilder();
        if (m_node.hasWizardView()) {
            builder.add(this);
        }
        return builder.build();
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasInteractiveView() {
        return m_node.hasInteractiveView();
    }

    /** {@inheritDoc} */
    @Override
    public String getInteractiveViewName() {
        return m_node.getInteractiveViewName();
    }

    /** {@inheritDoc} */
    @Override
    public <V extends AbstractNodeView<?> & InteractiveView<?, ? extends ViewContent, ? extends ViewContent>> V getInteractiveView() {
        NodeContext.pushContext(this);
        try {
            V ainv = m_node.getNodeModel().getInteractiveNodeView();
            if (ainv == null) {
                String name = getInteractiveViewName();
                if (name == null) {
                    name = "TITLE MISSING";
                }
                String title = getNameWithID() + " (" + name + ")";
                String customName = getDisplayCustomLine();
                if (!customName.isEmpty()) {
                    title += " - " + customName;
                }
                ainv = m_node.getInteractiveView(title);
                ainv.setWorkflowManagerAndNodeID(getParent(), getID());
            }
            return ainv;
        } finally {
            NodeContext.removeLastContext();
        }
    }


    /* ------------------ Job Handling ---------------- */

    /** {@inheritDoc} */
    @Override
    void cleanup() {
        NodeContext.pushContext(this);
        try {
            m_node.cleanup();
            m_node.setFlowObjectStack(null, null); // flow objects (scopes, loops) might have refs to WFM
            clearFileStoreHandler();
        } finally {
            NodeContext.removeLastContext();
        }
        getInternalState().decrementInStateCount();
        // call after node has been cleaned up, (DB) extensions rely on credentials provider (AP-23380)
        super.cleanup();
    }

    /** {@inheritDoc} */
    @Override
    void setJobManager(final NodeExecutionJobManager je) {
        synchronized (m_nodeMutex) {
            switch (getInternalState()) {
            case CONFIGURED_QUEUED:
            case EXECUTED_QUEUED:
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

    /** {@inheritDoc} */
    @Override
    public ExecutionContext createExecutionContext() {
        NodeProgressMonitor progressMonitor = getProgressMonitor();
        return new ExecutionContext(progressMonitor, getNode(), getOutDataMemoryPolicy(),
            getParent().getWorkflowDataRepository());
    }

    /* ---------------- Configuration/Execution ----------------- */

    /** {@inheritDoc} */
    @Override
    boolean performStateTransitionPREEXECUTE() {
        synchronized (m_nodeMutex) {
            getProgressMonitor().reset();
            switch (getInternalState()) {
            case EXECUTED_QUEUED:
            case CONFIGURED_QUEUED:
                setInternalState(InternalNodeContainerState.PREEXECUTE);
                return true;
            default:
                // ignore any other state: other states indicate that the node
                // was canceled before it is actually run
                // (this method is called from a worker thread, whereas cancel
                // typically from the UI thread)
                if (!Thread.currentThread().isInterrupted()) {
                    LOGGER.debug("Execution of node " + getNameWithID()
                            + " was probably canceled (node is " + getInternalState()
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
            switch (getInternalState()) {
            case PREEXECUTE:
                this.getNode().clearLoopContext();
                if (NodeExecutionJobManagerPool.isThreaded(findJobManager())) {
                    setInternalState(InternalNodeContainerState.EXECUTING);
                } else {
                    setInternalState(InternalNodeContainerState.EXECUTINGREMOTELY);
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
            switch (getInternalState()) {
            case PREEXECUTE: // in case of errors, e.g. flow stack problems
                             // encountered during doBeforeExecution
            case EXECUTING:
            case EXECUTINGREMOTELY:
                setInternalState(InternalNodeContainerState.POSTEXECUTE);
                break;
            default:
                throwIllegalStateException();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    void mimicRemotePreExecute() {
        synchronized (m_nodeMutex) {
            getProgressMonitor().reset();
            switch (getInternalState()) {
            case EXECUTED_MARKEDFOREXEC:
            case CONFIGURED_MARKEDFOREXEC:
            case UNCONFIGURED_MARKEDFOREXEC:
                // ideally opening the file store handler would be done in "mimicRemoteExecuting" (consistently to
                // performStateTransitionEXECUTING) but remote execution isn't split up that nicely - there is only
                // pre-execute and executed
                initLocalFileStoreHandler();
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

    /**
     * {@inheritDoc}
     */
    @Override
    void mimicRemoteExecuted(final NodeContainerExecutionStatus status) {
        synchronized (m_nodeMutex) {
            super.mimicRemoteExecuted(status);
            setExecutionEnvironment(null);
            closeFileStoreHandlerAfterExecute(status.isSuccess());
        }

    }

    /** {@inheritDoc} */
    @Override
    void performStateTransitionEXECUTED(
            final NodeContainerExecutionStatus status) {
        synchronized (m_nodeMutex) {
            switch (getInternalState()) {
            case POSTEXECUTE:
                closeFileStoreHandlerAfterExecute(status.isSuccess());
                if (status.isSuccess()) {
                    if (this.getNode().getLoopContext() != null) {
                        // loop not yet done - "stay" configured until done.
                        assert this.getLoopStatus().equals(LoopStatus.RUNNING);
                        setInternalState(InternalNodeContainerState.CONFIGURED_MARKEDFOREXEC);
                    } else {
                        setInternalState(InternalNodeContainerState.EXECUTED);
                        m_nodeAndBundleInformation = NodeAndBundleInformationPersistor.create(m_node);
                        setExecutionEnvironment(null);
                    }
                } else {
                    // node will be configured in doAfterExecute.
                    // for now we assume complete failure and clean up (reset)
                    // We do keep the message, though.
                    NodeMessage oldMessage = getNodeMessage();
                    NodeContext.pushContext(this);
                    try {
                        performReset();
                    } finally {
                        NodeContext.removeLastContext();
                    }
                    this.clearFileStoreHandler();
                    setNodeMessage(oldMessage);
                    setInternalState(InternalNodeContainerState.IDLE);
                    setExecutionEnvironment(null);
                }
                setExecutionJob(null);
                break;
            default:
                throwIllegalStateException();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    boolean performConfigure(final PortObjectSpec[] inSpecs, final NodeConfigureHelper nch,
        final boolean keepNodeMessage) {
        return m_node.configure(inSpecs, nch);
    }

    /** {@inheritDoc} */
    @Override
    void performLoadModelSettingsFrom(final NodeSettingsRO modelSettings) throws InvalidSettingsException {
        m_node.loadModelSettingsFrom(modelSettings);
    }

    /** {@inheritDoc} */
    @Override
    public NodeContainerExecutionStatus performExecuteNode(final PortObject[] inObjects) {
        ExecutionContext ec = createExecutionContext();
        m_node.openFileStoreHandler(ec);

        ExecutionEnvironment ev = getExecutionEnvironment();
        boolean success;
        try {
            ec.checkCanceled();
            success = true;
        } catch (CanceledExecutionException e) {
            String errorString = "Execution canceled";
            LOGGER.warn(errorString);
            setNodeMessage(new NodeMessage(NodeMessage.Type.WARNING, errorString));
            success = false;
        }
        NodeContext.pushContext(this);
        try {
            // execute node outside any synchronization!
            success = success && m_node.execute(inObjects, ev, ec);
        } finally {
            NodeContext.removeLastContext();
        }
        if (success) {
            // output tables are made publicly available (for blobs)
            putOutputTablesIntoGlobalRepository(ec);
        } else {
            // something went wrong: reset and configure node to reach
            // a solid state again will be done by WorkflowManager (in
            // doAfterExecute().
        }
        return success ? NodeContainerExecutionStatus.SUCCESS : NodeContainerExecutionStatus.FAILURE;
    }


    /* ----------- Reset and Port handling ------------- */

    /** {@inheritDoc} */
    @Override
    void performReset() {
        m_node.reset();
        m_nodeAndBundleInformation = null;
        cleanOutPorts(false);
    }

    /** {@inheritDoc} */
    @Override
    void cleanOutPorts(final boolean isLoopRestart) {
        m_node.cleanOutPorts(isLoopRestart, getParent().getWorkflowDataRepository());
        if (!isLoopRestart) {
            // this should have no affect as m_node.cleanOutPorts() will remove
            // all tables already
            int nrRemovedTables = removeOutputTablesFromGlobalRepository();
            assert nrRemovedTables == 0 : nrRemovedTables + " tables in global "
                + "repository after node cleared outports (expected 0)";
        }
    }

    /**
     * Enumerates the output tables and puts them into the workflow global repository of tables. All other (temporary)
     * tables that were created in the given execution context, will be put in a set of temporary tables in the node.
     *
     * <p>
     * This method is only to be called by the framework and by other external executor after execution.
     *
     * @param c The execution context containing the (so far) local tables.
     * @noreference This method is not intended to be referenced by clients.
     */
    public final void putOutputTablesIntoGlobalRepository(final ExecutionContext c) {
        WorkflowDataRepository dataRepository = getParent().getWorkflowDataRepository();
        m_node.putOutputTablesIntoGlobalRepository(dataRepository);
        Map<Integer, ContainerTable> localRep =
                Node.getLocalTableRepositoryFromContext(c);
        Set<ContainerTable> localTables = new LinkedHashSet<>();
        for (Map.Entry<Integer, ContainerTable> t : localRep.entrySet()) {
            Optional<ContainerTable> fromGlob = dataRepository.getTable(t.getKey());
            if (fromGlob.isPresent()) {
                assert fromGlob.get() == t.getValue();
            } else {
                // not used globally
                localTables.add(t.getValue());
            }
        }
        m_node.addToTemporaryTables(localTables);
    }

    /** Removes all tables that were created by this node from the global
     * table repository. */
    private int removeOutputTablesFromGlobalRepository() {
        WorkflowDataRepository dataRepository = getParent().getWorkflowDataRepository();
        return m_node.removeOutputTablesFromGlobalRepository(dataRepository);
    }

    /* --------------- File Store Handling ------------- */

    /** ...
     *
     * @param success ...
     */
    void closeFileStoreHandlerAfterExecute(final boolean success) {
        IFileStoreHandler fsh = m_node.getFileStoreHandler();
        if (fsh instanceof IWriteFileStoreHandler) {
            ((IWriteFileStoreHandler)fsh).close();
        } else {
            // * can be null if run through 3rd party executor
            // * might be not an IWriteFileStoreHandler if restored loop is executed
            // * can be an EmptyFileStoreHandler if node is in an inactive branch
            // (this will result in a failure before model#execute is called)
            assert !success || fsh == null || fsh instanceof EmptyFileStoreHandler : String
                .format("Must not be \"%s\" in execute, node \"%s\"", fsh.getClass().getSimpleName(), getNameWithID());
            LOGGER.debug("Can't close file store handler, not writable: "
                + (fsh == null ? "<null>" : fsh.getClass().getSimpleName()));
        }
    }

    /**
     * Initializes and sets the {@link IFileStoreHandler} of this node by indirectly forwarding/using/referencing the
     * file store handler of the passed node. I.e. where the respective files are written to is determined by the file
     * store handler of the given node (not this node).
     *
     * Note: the provided node need to have an already initialized file store handler of type
     * {@link IWriteFileStoreHandler}.
     *
     * @param nnc the node to which file store handler to reference
     * @since 4.1
     */
    public void initFileStoreHandlerReference(final NativeNodeContainer nnc) {
        IWriteFileStoreHandler targetFSHandler = (IWriteFileStoreHandler)nnc.getNode().getFileStoreHandler();
        m_node.setFileStoreHandler(initWriteFileStoreHandlerReference(targetFSHandler));
    }

    private IWriteFileStoreHandler initWriteFileStoreHandlerReference(final IWriteFileStoreHandler targetFSHandler) {
        IFileStoreHandler oldFSHandler = m_node.getFileStoreHandler();
        if (oldFSHandler instanceof IWriteFileStoreHandler) {
            oldFSHandler.clearAndDispose();
        }
        return new ReferenceWriteFileStoreHandler(targetFSHandler, getID());
    }

    /**
     * Initializes and sets a local {@link IFileStoreHandler} for this node if needed. Local file store handlers write
     * the respective files into a dedicated local directory associated with this node.
     *
     * Needs to be done explicitly, e.g., by an {@link NodeExecutionJob} that takes care of the node execution.
     *
     * @since 4.1
     */
    public void initLocalFileStoreHandler() {
        FlowLoopContext upstreamFLC = getUpstreamFlowLoopContext();
        FlowLoopContext innerFLC = getInnerFlowLoopContext();
        IWriteFileStoreHandler currentFSHandler;

        // special handling if node is part of a loop and _not_ in the first iteration
        currentFSHandler = initFileStoreHandlerInLoopIteration(upstreamFLC, innerFLC);

        if (currentFSHandler == null) {
            // node not in loop or in a loop's iteration (but not the first one)
            // -> initialize new file store handler for the first time

            // check for virtual scope
            // note: virtual scope is always the outermost scope
            IWriteFileStoreHandler virtualScopeFSHandler = initVirtualScopeFileStoreHandler();

            if (virtualScopeFSHandler == null && upstreamFLC == null && innerFLC == null) {
                // node neither part of a virtual scope nor a loop scope
                currentFSHandler = initDefaultFileStoreHandler();
            } else if (upstreamFLC != null || innerFLC != null) {
                // in case node is part of a loop (or nested loop ...)
                // (return null if not)
                currentFSHandler = initLoopScopeFileStoreHandler(upstreamFLC, innerFLC, virtualScopeFSHandler);
            }
            if (currentFSHandler == null && virtualScopeFSHandler != null) {
                currentFSHandler = virtualScopeFSHandler;
           }
        }

        if (currentFSHandler != null) {
            WorkflowDataRepository dataRepository = getParent().getWorkflowDataRepository();
            currentFSHandler.addToRepository(dataRepository);
            m_node.setFileStoreHandler(currentFSHandler);
        }
    }

    private IWriteFileStoreHandler initFileStoreHandlerInLoopIteration(final FlowLoopContext upstreamFLC,
        final FlowLoopContext innerFLC) {
        if (innerFLC != null && innerFLC.getIterationIndex() > 0) {
            // it's a loop start in its n-th iteration (n > 0)
            assert m_node.getFileStoreHandler() instanceof IWriteFileStoreHandler : "Loop Start " + getNameWithID()
                + " must have file store handler in iteration " + innerFLC.getIterationIndex();
            // just keep the existing file store handler
            return (IWriteFileStoreHandler)m_node.getFileStoreHandler();
        } else if (innerFLC == null && upstreamFLC != null && upstreamFLC.getIterationIndex() > 0) {
            if (this.isModelCompatibleTo(LoopEndNode.class)) {
                // it's a loop end in its n-th iteration (n > 0)
                assert m_node.getFileStoreHandler() instanceof IWriteFileStoreHandler : "Node " + getNameWithID()
                    + " must have file store handler in iteration " + upstreamFLC.getIterationIndex();
                // just keep the existing file store handler
                return (IWriteFileStoreHandler)m_node.getFileStoreHandler();
            } else {
                // it's an ordinary node in the n-th iteration (n > 0) of the loop it's part of
                return new ReferenceWriteFileStoreHandler(upstreamFLC.getFileStoreHandler());
            }
        }
        return null;
    }

    /**
     * Inits the default file store handler for a node, i.e. file stores are stored with the node itself.
     *
     * @return the file store handler, never <code>null</code>
     */
    private IWriteFileStoreHandler initDefaultFileStoreHandler() {
        // node is not a start node and not contained in a loop
        IFileStoreHandler oldFSHandler = m_node.getFileStoreHandler();
        if (oldFSHandler instanceof IWriteFileStoreHandler) {
            clearFileStoreHandler();
            /*assert false : "Node " + getNameWithID() + " must not have file store handler at this point (not a "
            + "loop start and not contained in loop), disposing old handler";*/
        }
        IWriteFileStoreHandler newFSHandler = new WriteFileStoreHandler(getNameWithID(), UUID.randomUUID());
        return newFSHandler;
    }

    /**
     * Inits the file store handler if node is part of a loop scope (including start and end).
     *
     * @param upstreamFLC optional loop context this node might be part of
     * @param innerFLC optional loop context beginning at this node if this node is a loop start
     * @param targetFSH file store handler to be referenced if given
     * @return the file store handler or <code>null</code> if not a loop scope
     */
    private IWriteFileStoreHandler initLoopScopeFileStoreHandler(final FlowLoopContext upstreamFLC,
        final FlowLoopContext innerFLC, final IWriteFileStoreHandler targetFSH) {
        IFileStoreHandler oldFSHandler = m_node.getFileStoreHandler();
        IWriteFileStoreHandler newFSHandler = null;

        if (innerFLC != null) {
            // node is a loop start node
            assert innerFLC.getIterationIndex() == 0;
            if (oldFSHandler instanceof IWriteFileStoreHandler) {
                assert false : "Loop Start " + getNameWithID() + " must not have file store handler at this point "
                    + "(no iteration ran), disposing old handler";
                clearFileStoreHandler();
            }
            if (upstreamFLC != null) {
                ILoopStartWriteFileStoreHandler upStreamFSHandler = upstreamFLC.getFileStoreHandler();
                newFSHandler = new NestedLoopStartWriteFileStoreHandler(upStreamFSHandler, innerFLC);
            } else if (targetFSH != null) {
                // there is another target file store handler
                // -> loop start file store handler references it
                newFSHandler = new LoopStartWriteFileStoreHandler(this, targetFSH, innerFLC);
            } else {
                // create entirely new loop start file store handler (without referencing another handler)
                newFSHandler = new LoopStartWriteFileStoreHandler(this, UUID.randomUUID(), innerFLC);
            }
            innerFLC.setFileStoreHandler((ILoopStartWriteFileStoreHandler)newFSHandler);
        } else {
            // ordinary node contained in loop
            assert upstreamFLC != null;
            assert upstreamFLC.getIterationIndex() == 0;
            ILoopStartWriteFileStoreHandler upStreamFSHandler = upstreamFLC.getFileStoreHandler();
            if (upStreamFSHandler != null) {
                if (this.isModelCompatibleTo(LoopEndNode.class)) {
                    newFSHandler = new LoopEndWriteFileStoreHandler(upStreamFSHandler);
                } else {
                    newFSHandler = new ReferenceWriteFileStoreHandler(upStreamFSHandler);
                }
            } else {
                // in a loop but no file store handler set ... must be an inactive loop or a loop in an inactive context
                assert upstreamFLC.isInactiveScope()
                    || getFlowObjectStack().peekScopeContext(FlowScopeContext.class, true) != null;
            }
        }
        return newFSHandler;
    }

    /**
     * @return the flow loop context this node is part of or <code>null</code> if not part of a loop
     */
    private FlowLoopContext getUpstreamFlowLoopContext() {
        final FlowObjectStack flowObjectStack = getFlowObjectStack();
        FlowLoopContext upstreamFLC = getFlowScopeContextFromHierarchy(FlowLoopContext.class, flowObjectStack);

        NodeID outerStartNodeID = upstreamFLC == null ? null : upstreamFLC.getHeadNode();
        // loop start nodes will put their loop context on the outgoing flow object stack
        assert !getID().equals(outerStartNodeID) : "Loop start on incoming flow stack can't be node itself";

        return upstreamFLC;
    }

    /**
     * @return if this node is a loop start, the loop start's loop inner loop context, otherwise <code>null</code>
     */
    private FlowLoopContext getInnerFlowLoopContext() {
        final FlowLoopContext innerFLC = getOutgoingFlowObjectStack().peek(FlowLoopContext.class);
        NodeID innerStartNodeID = innerFLC == null ? null : innerFLC.getHeadNode();
        // if there is a loop context on this node's stack, this node must be the start
        assert !(this.isModelCompatibleTo(LoopStartNode.class)) || getID().equals(innerStartNodeID);

        return innerFLC;
    }

    /**
     * Gets the flow scope context of the given class, also crossing 'subnode-boundaries' if this node is part of a
     * subnode/component.
     *
     * @param contextClass the class of the {@link FlowScopeContext} to get
     * @param flowObjectStack the stack to get the flow-scope-context from
     * @param <C>
     *
     * @return the context or <code>null</node> if there is none of the given class
     */
    static <C extends FlowScopeContext> C getFlowScopeContextFromHierarchy(final Class<C> contextClass,
        final FlowObjectStack flowObjectStack) {
        C context = flowObjectStack.peek(contextClass);
        if (context == null) {
            // if node is contained in subnode check if the subnode is in a loop (see AP-5667)
            final FlowSubnodeScopeContext subnodeSC = flowObjectStack.peek(FlowSubnodeScopeContext.class);
            if (subnodeSC != null) {
                context = subnodeSC.getOuterFlowScopeContext(contextClass);
            }
        }
        return context;
    }

    /**
     * Inits file store handler for nodes that are part of a {@link FlowVirtualScopeContext}.
     *
     * @return the file store handler or <code>null</code> if not a virtual scope
     */
    private IWriteFileStoreHandler initVirtualScopeFileStoreHandler() {
        FlowVirtualScopeContext virtualScope =
            getFlowScopeContextFromHierarchy(FlowVirtualScopeContext.class, getFlowObjectStack());
        var hasHostNode = virtualScope != null && virtualScope.hasHostNode();
        if (hasHostNode) {
            var fsh = virtualScope.createFileStoreHandler().orElse(null);
            if (fsh instanceof IWriteFileStoreHandler) {
                return initWriteFileStoreHandlerReference((IWriteFileStoreHandler)fsh);
            } else {
                throw new IllegalStateException(
                    "No write file store handler given. Most likely an implementation error");
            }
        } else {
            return null;
        }
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

    /* --------------- Loop Stuff ----------------- */

    /** Possible loop states. */
    public static enum LoopStatus { NONE, RUNNING, PAUSED, FINISHED }
    /**
     * @return status of loop (determined from NodeState and LoopContext)
     */
    public LoopStatus getLoopStatus() {
        if (this.isModelCompatibleTo(LoopEndNode.class)) {
            if ((getNode().getLoopContext() != null)
                    || (getInternalState().isExecutionInProgress())) {
                if ((getNode().getPauseLoopExecution())
                        && (getInternalState().equals(InternalNodeContainerState.CONFIGURED_MARKEDFOREXEC))) {
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
     * @return always the same the {@link LoopStatusChangeHandler}-instance or an empty optional if this node is not a
     *         loop end
     *
     * @noreference This method is not intended to be referenced by clients.
     *
     * @since 4.6
     */
    public Optional<LoopStatusChangeHandler> getLoopStatusChangeHandler() {
        if (this.isModelCompatibleTo(LoopEndNode.class) && m_loopStatusChangeHandler == null) {
            m_loopStatusChangeHandler = new LoopStatusChangeHandler();
        }
        return Optional.ofNullable(m_loopStatusChangeHandler);
    }

    /**
     * @see NodeModel#resetAndConfigureLoopBody()
     */
    boolean resetAndConfigureLoopBody() {
        NodeContext.pushContext(this);
        try {
            return getNode().resetAndConfigureLoopBody();
        } finally {
            NodeContext.removeLastContext();
        }
    }

    /** enable (or disable) that after the next execution of this loop end node
     * the execution will be halted. This can also be called on a paused node
     * to trigger a "single step" execution.
     *
     * @param enablePausing if true, pause is enabled. Otherwise disabled.
     */
    void pauseLoopExecution(final boolean enablePausing) {
        if (getInternalState().isExecutionInProgress()) {
            getNode().setPauseLoopExecution(enablePausing);
            getLoopStatusChangeHandler().ifPresent(h -> h.notifyLoopStatusChangeListener(LoopStatus.PAUSED));
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isModelCompatibleTo(final Class<?> nodeModelClass) {
        return this.getNode().isModelCompatibleTo(nodeModelClass);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInactive() {
        return m_node.isInactive();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean setInactive() {
        //TODO so far only called when a single node container
        //is set inactive after a failure in a try-catch-scope.
        //For native nodes these caught failures are currently handled
        //in the the Node.execute(...)-method -> should eventually be
        //moved into NodeExecutionJob.internalRun(...) where
        //caught failures (i.e. try-catch-scope) are handled for components, too
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInactiveBranchConsumer() {
        return m_node.isInactiveBranchConsumer();
    }

    /* ------------------- Load & Save ---------------- */

    @Override
    void loadContent(final BaseNodeDef nodeDef, final ExecutionMonitor exec, final LoadResult loadResult)
        throws CanceledExecutionException {
        super.loadContent(nodeDef, exec, loadResult);
        var context = NodeContext.getContext();
        // inform the virtual input/output node about the containing component
        if (context != null) { // this is going to be refactored as part of AP-18959
            var subnodeContainerOptional = context.getContextObjectForClass(SubNodeContainer.class);
            if (getNodeModel() instanceof VirtualSubNodeInOut && subnodeContainerOptional.isPresent()) {
                ((VirtualSubNodeInOut)getNodeModel()).setSubNodeContainer(subnodeContainerOptional.get());
            }
        }

        var ms =
            Optional.ofNullable(getSingleNodeContainerSettings()).map(SingleNodeContainerSettings::getModelSettings);
        if (ms.isPresent()) { // null if the node never had settings - no reason to load them
            var modelSettings = ms.get();
            NodeContext.pushContext(this);
            try {
                m_node.validateModelSettings(modelSettings);
                m_node.loadModelSettingsFrom(modelSettings);
            } catch (InvalidSettingsException ex) {
                final var msg = String.format("Can't load the node model settings: %s", ex.getMessage());
                LOGGER.error(msg, ex);
                loadResult.addError(msg);
            } finally {
                NodeContext.removeLastContext();
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    WorkflowCopyContent performLoadContent(final SingleNodeContainerPersistor nodePersistor,
        final Map<Integer, BufferedDataTable> tblRep, final FlowObjectStack inStack, final ExecutionMonitor exec,
        final LoadResult loadResult, final boolean preserveNodeMessage) throws CanceledExecutionException {
        boolean isExecuted = nodePersistor.getMetaPersistor().getState().equals(InternalNodeContainerState.EXECUTED);

        if (nodePersistor instanceof FileNativeNodeContainerPersistor) {
            FileNativeNodeContainerPersistor fileNativeNCPersistor = (FileNativeNodeContainerPersistor)nodePersistor;
            exec.setMessage("Loading settings into node instance");
            FileNodePersistor innerNodePersistor = fileNativeNCPersistor.getNodePersistor();
            m_node.load(innerNodePersistor, exec, loadResult);
            if (innerNodePersistor.needsResetAfterLoad()) {
                fileNativeNCPersistor.setNeedsResetAfterLoad();
            }
            String status;
            switch (loadResult.getType()) {
                case Ok:
                    status = " without errors";
                    break;
                case DataLoadError:
                    status = " with data errors";
                    break;
                case Error:
                    status = " with errors";
                    break;
                case Warning:
                    status = " with warnings";
                    break;
                default:
                    status = " with " + loadResult.getType();
            }
            String message = "Loaded node " + getNameWithID() + status;
            exec.setProgress(1.0, message);

            if (m_node.isModelCompatibleTo(CredentialsNode.class)) {
                CredentialsNode credNode = (CredentialsNode)m_node.getNodeModel();
                credNode.doAfterLoadFromDisc(fileNativeNCPersistor.getLoadHelper(),
                    getCredentialsProvider(), isExecuted, isInactive());
                saveModelSettingsToDefault();
            }
        }

        if (isExecuted) {
            m_node.putOutputTablesIntoGlobalRepository(getParent().getWorkflowDataRepository());
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void loadExecutionResult(
            final NodeContainerExecutionResult execResult,
            final ExecutionMonitor exec, final LoadResult loadResult) {
        synchronized (m_nodeMutex) {
            if (InternalNodeContainerState.EXECUTED.equals(getInternalState())) {
                LOGGER.debug(getNameWithID()
                        + " is already executed; won't load execution result");
                return;
            }
            if (!(execResult instanceof NativeNodeContainerExecutionResult)) {
                throw new IllegalArgumentException("Argument must be instance "
                        + "of \"" + NativeNodeContainerExecutionResult.
                        class.getSimpleName() + "\": "
                        + execResult.getClass().getSimpleName());
            }
            super.loadExecutionResult(execResult, exec, loadResult);
            NativeNodeContainerExecutionResult sncExecResult = (NativeNodeContainerExecutionResult)execResult;
            NodeExecutionResult nodeExecResult = sncExecResult.getNodeExecutionResult();
            boolean success = sncExecResult.isSuccess();
            if (success) {
                NodeContext.pushContext(this);
                try {
                    m_node.loadExecutionResult(nodeExecResult, new ExecutionMonitor(), loadResult);
                    m_node.putOutputTablesIntoGlobalRepository(getParent().getWorkflowDataRepository());
                } finally {
                    NodeContext.removeLastContext();
                }
            }
            boolean needsReset = nodeExecResult.needsResetAfterLoad();
            if (!needsReset && success) {
                for (int i = 0; i < getNrOutPorts(); i++) {
                    if (m_node.getOutputObject(i) == null) {
                        loadResult.addError("Output object at port " + i + " is null");
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
    public NativeNodeContainerExecutionResult createExecutionResult(
            final ExecutionMonitor exec) throws CanceledExecutionException {
        synchronized (m_nodeMutex) {
            NativeNodeContainerExecutionResult result = new NativeNodeContainerExecutionResult();
            super.saveExecutionResult(result);
            NodeContext.pushContext(this);
            try {
                result.setNodeExecutionResult(m_node.createNodeExecutionResult(exec));
            } finally {
                NodeContext.removeLastContext();
            }
            return result;
        }
    }

    /** {@inheritDoc} */
    @Override
    void performSaveModelSettingsTo(final NodeSettings modelSettings) {
        getNode().saveModelSettingsTo(modelSettings);
    }

    /**
     * {@inheritDoc}
     *
     * This method is also called for loaded nodes with view settings for which no view settings were stored on the last
     * save of the workflow. This is only the case for nodes with a bundle version before 5.2. In this case, if the node
     * was is executed initially, the bundle version is available via the NodeContext
     */
    @Override
    void performSaveDefaultViewSettingsTo(final NodeSettings viewSettings) {
        getNode().saveDefaultViewSettingsTo(viewSettings);
    }

    /** {@inheritDoc} */
    @Override
    void performValidateSettings(final NodeSettingsRO modelSettings) throws InvalidSettingsException {
        getNode().validateModelSettings(modelSettings);
    }

    /** {@inheritDoc} */
    @Override
    protected void performValidateViewSettings(final NodeSettingsRO viewSettings) throws InvalidSettingsException {
        getNode().validateViewSettings(viewSettings);
    }

    /* ------------ Stacks and Co --------------- */

    /** {@inheritDoc} */
    @Override
    void setFlowObjectStack(final FlowObjectStack st, final FlowObjectStack outgoingStack) {
        synchronized (m_nodeMutex) {
            pushNodeDropDirURLsOntoStack(st);
            m_node.setFlowObjectStack(st, outgoingStack);
        }
    }

    /** Support old-style drop dir mechanism - replaced since 2.8 with relative mountpoints,
     * e.g. knime://knime.workflow */
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

    /** {@inheritDoc} */
    @Override
    public FlowObjectStack getFlowObjectStack() {
        return m_node.getFlowObjectStack();
    }

    /** {@inheritDoc} */
    @Override
    public FlowObjectStack getOutgoingFlowObjectStack() {
        return m_node.getOutgoingFlowObjectStack();
    }

    /** {@inheritDoc} */
    @Override
    void performSetCredentialsProvider(final CredentialsProvider cp) {
        m_node.setCredentialsProvider(cp);
    }

    /** {@inheritDoc} */
    @Override
    CredentialsProvider getCredentialsProvider() {
        return m_node.getCredentialsProvider();
    }


    /** Get the tables/portobjects kept by the underlying node. The return value is null if (a) the underlying node is
     * not a {@link org.knime.core.node.BufferedDataTableHolder} or {@link org.knime.core.node.port.PortObjectHolder}
     * or (b) the node is not executed.
     * @return The internally held tables.
     * @see Node#getInternalHeldPortObjects()
     */
    public PortObject[] getInternalHeldPortObjects() {
        return getNode().getInternalHeldPortObjects();
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
         * so the underlying node directory can be safely deleted.
         */
        // if-statement fixes bug 1777: ensureOpen can cause trouble if there
        // is a deep hierarchy of BDTs
        if (!isDirty()) {
            NodeContext.pushContext(this);
            try { // only for node context push
                try {
                    m_node.ensureOutputDataIsRead();
                } catch (Exception e) {
                    LOGGER.error("Unable to read output data", e);
                }
                IFileStoreHandler fileStoreHandler = m_node.getFileStoreHandler();
                if (fileStoreHandler instanceof IWriteFileStoreHandler) {
                    try {
                        ((IWriteFileStoreHandler)fileStoreHandler).ensureOpenAfterLoad();
                    } catch (IOException e) {
                        LOGGER.error("Unable to open file store handler " + fileStoreHandler, e);
                    }
                }
            } finally {
                NodeContext.removeLastContext();
            }
        }
        super.setDirty();
    }

    @Override
    protected NodeContainerPersistor getCopyPersistor(final boolean preserveDeletableFlags,
        final boolean isUndoableDeleteCommand) {
        return new CopyNativeNodeContainerPersistor(this, preserveDeletableFlags, isUndoableDeleteCommand);
    }

    /* ------------------ Node Properties ---------------- */

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return m_node.getName();
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

    /** {@inheritDoc} */
    @Override
    public Element getXMLDescription() {
        return m_node.getXMLDescription();
    }

    /** @return non-null meta bundle and node information to this instance. For executed nodes this will
     * return the information to the bundle as of time of execution (also for loaded workflows).
     * @since 2.10
     * @noreference This method is not intended to be referenced by clients. */
    public NodeAndBundleInformationPersistor getNodeAndBundleInformation() {
        if (m_nodeAndBundleInformation != null) {
            return m_nodeAndBundleInformation;
        }
        final NodeModel model = getNodeModel();
        if (model instanceof MissingNodeModel) {
            return ((MissingNodeModel)model).getNodeAndBundleInformation();
        }
        return NodeAndBundleInformationPersistor.create(getNode());
    }

    /* ------------------ Dialog ------------- */

    /** {@inheritDoc} */
    @Override
    public boolean hasDialog() {
        return m_node.hasDialog();
    }

    /** {@inheritDoc} */
    @Override
    public final boolean hasDataAwareDialogPane() {
        NodeContext.pushContext(this);
        try {
            return m_node.hasDialog() && (m_node.getDialogPane() instanceof DataAwareNodeDialogPane);
        } finally {
            NodeContext.removeLastContext();
        }
    }

    /** {@inheritDoc} */
    @Override
    NodeDialogPane getDialogPaneWithSettings(final PortObjectSpec[] inSpecs,
            final PortObject[] inData) throws NotConfigurableException {
        NodeSettings settings = new NodeSettings(getName());
        saveSettings(settings, true, true);
        NodeContext.pushContext(this);
        try {
            return m_node.getDialogPaneWithSettings(inSpecs, inData, settings, getParent().isWriteProtected());
        } finally {
            NodeContext.removeLastContext();
        }
    }

    /** {@inheritDoc} */
    @Override
    NodeDialogPane getDialogPane() {
        NodeContext.pushContext(this);
        try {
            return m_node.getDialogPane();
        } finally {
            NodeContext.removeLastContext();
        }
    }



    /* --------------- Output Port Information ---------------- */

    /**
     * {@inheritDoc}
     */
    @Override
    public PortType getOutputType(final int portIndex) {
        return getNode().getOutputType(portIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObjectSpec getOutputSpec(final int portIndex) {
        return getNode().getOutputSpec(portIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObject getOutputObject(final int portIndex) {
        return getNode().getOutputObject(portIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOutputObjectSummary(final int portIndex) {
        return getNode().getOutputObjectSummary(portIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HiLiteHandler getOutputHiLiteHandler(final int portIndex) {
        return getNode().getOutputHiLiteHandler(portIndex);
    }
}
