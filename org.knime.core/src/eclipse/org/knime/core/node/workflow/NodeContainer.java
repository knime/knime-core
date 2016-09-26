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

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

import org.knime.core.api.node.workflow.INodeContainer.NodeLock;
import org.knime.core.api.node.workflow.INodeContainer.NodeLocks;
import org.knime.core.api.node.workflow.NodeAnnotationData;
import org.knime.core.api.node.workflow.NodeContainerState;
import org.knime.core.api.node.workflow.NodeContainerStateObservable;
import org.knime.core.api.node.workflow.NodeStateChangeListener;
import org.knime.core.api.node.workflow.NodeStateEvent;
import org.knime.core.api.node.workflow.NodeUIInformation;
import org.knime.core.api.node.workflow.NodeUIInformationEvent;
import org.knime.core.api.node.workflow.NodeUIInformationListener;
import org.knime.core.api.node.workflow.WorkflowCopyContent;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.filestore.internal.FileStoreHandlerRepository;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialog;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeProgressMonitor;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NodeView;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.exec.ThreadNodeExecutionJobManager;
import org.knime.core.node.interactive.InteractiveView;
import org.knime.core.node.interactive.ViewContent;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.node.util.NodeExecutionJobManagerPool;
import org.knime.core.node.web.WebTemplate;
import org.knime.core.node.workflow.NodeContainer.NodeContainerSettings.SplitType;
import org.knime.core.node.workflow.NodePropertyChangedEvent.NodeProperty;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.action.InteractiveWebViewsResult;
import org.knime.core.node.workflow.execresult.NodeContainerExecutionResult;
import org.knime.core.node.workflow.execresult.NodeContainerExecutionStatus;

/**
 * Abstract super class for containers holding node or just structural
 * information of a metanode. Also stores additional (optional) information
 * such as coordinates on a workflow layout.
 *
 * @author M. Berthold/B. Wiswedel, University of Konstanz
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class NodeContainer implements NodeProgressListener, NodeContainerStateObservable {

    /** my logger. */
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(NodeContainer.class);

    /** Old and unfortunately public possible status values of a NodeContainer.
     * Needed for backwards compatibility with RMI Execution 2.7 and before.
     * */
    @Deprecated
    public static enum State {
        IDLE,
        CONFIGURED,
        UNCONFIGURED_MARKEDFOREXEC,
        MARKEDFOREXEC,
        QUEUED,
        PREEXECUTE,
        EXECUTING,
        EXECUTINGREMOTELY,
        POSTEXECUTE,
        EXECUTED;

        /** @return Whether this state represents an intermediate state,
         * i.e. where the node is either executing or in some way scheduled
         * for execution.
         */
        public boolean executionInProgress() {
            switch (this) {
            case IDLE:
            case EXECUTED:
            case CONFIGURED: return false;
            default: return true;
            }
        }
    };

    private InternalNodeContainerState m_state;

    private final NodeID m_id;

    private final WorkflowManager m_parent;

    private NodeExecutionJobManager m_jobManager;

    /** The job representing the pending task of executing the node. */
    private NodeExecutionJob m_executionJob;

    /** progress monitor. */
    private final NodeProgressMonitor m_progressMonitor = new DefaultNodeProgressMonitor(this);

    /** execution environment. */
    private ExecutionEnvironment m_executionEnv = null;

    private NodeMessage m_nodeMessage = NodeMessage.NONE;

    /**
     * Object that represents locks set on the node, i.e.
     * whether the node is allowed to be deleted, reseted or configured.
     */
    private NodeLocks m_nodeLocks;

    /** this list will hold FlowObjects of loops in the pipeline which cannot
     * be executed before this one is not done - usually these are loops
     * with "dangling" branches, e.g. a chain of nodes leaving the loop.
     */
    private final ArrayList<FlowLoopContext> m_listOfWaitingLoops = new ArrayList<FlowLoopContext>();

    private String m_customDescription;

    private ReferencedFile m_nodeContainerDirectory;

    private ReferencedFile m_autoSaveDirectory;

    private final NodeAnnotation m_annotation;

    private final NodeTimer m_nodeTimer = new NodeTimer(this);

    /**
     * semaphore to make sure never try to work on inconsistent internal node
     * states. This semaphore will be used by a node alone to synchronize
     * internal changes of status etc.
     */
    protected final Object m_nodeMutex = new Object();

    /*--------- listener administration------------*/

    private final CopyOnWriteArraySet<NodeStateChangeListener>
        m_stateChangeListeners =
            new CopyOnWriteArraySet<NodeStateChangeListener>();

    private final CopyOnWriteArraySet<NodeMessageListener> m_messageListeners =
        new CopyOnWriteArraySet<NodeMessageListener>();

    private final CopyOnWriteArraySet<NodeProgressListener>
        m_progressListeners = new CopyOnWriteArraySet<NodeProgressListener>();

    private final CopyOnWriteArraySet<NodeUIInformationListener> m_uiListeners =
        new CopyOnWriteArraySet<NodeUIInformationListener>();

    private final CopyOnWriteArraySet<NodePropertyChangedListener> m_nodePropertyChangedListeners =
            new CopyOnWriteArraySet<NodePropertyChangedListener>();

    private NodeUIInformation m_uiInformation;


    /**
     * Create new NodeContainer with IDLE state.
     *
     * @param parent the workflowmanager holding this node
     * @param id the nodes identifier
     */
    NodeContainer(final WorkflowManager parent, final NodeID id) {
        m_parent = parent;
        if (m_parent == null) {
            // make sure at least the top node knows how to execute stuff
            m_jobManager = NodeExecutionJobManagerPool.getDefaultJobManagerFactory().getInstance();
        }
        m_id = id;
        m_state = InternalNodeContainerState.IDLE;
        m_nodeLocks = new NodeLocks(false, false, false);
        m_annotation = new NodeAnnotation(NodeAnnotationData.builder().setIsDefault(true).build());
        m_annotation.registerOnNodeContainer(this);
    }

    /**
     * Create new NodeContainer with IDLE state. The provided node annotation will be taken over.
     *
     * @param parent the workflowmanager holding this node
     * @param id the nodes identifier
     * @param nodeAnno the node annotation to be copied from. If <code>null</code> then it's like calling {@link #NodeContainer(WorkflowManager, NodeID)}.
     */
    NodeContainer(final WorkflowManager parent, final NodeID id, final NodeAnnotation nodeAnno) {
        this(parent, id);
        if (nodeAnno != null) {
            m_annotation.setData(NodeAnnotationData.builder(nodeAnno.getData(), true).build());
        }
    }

    NodeContainer(final WorkflowManager parent, final NodeID id,
            final NodeContainerMetaPersistor persistor) {
        this(parent, id);
        assert persistor.getState() != null : "State of node \"" + id
        + "\" in \"" + persistor.getClass().getSimpleName() + "\" is null";
        m_state = persistor.getState();
        m_jobManager = persistor.getExecutionJobManager();
        if (m_parent == null && !(m_jobManager instanceof ThreadNodeExecutionJobManager)) {
            // make sure at least the top node knows how to execute stuff
            m_jobManager = NodeExecutionJobManagerPool.getDefaultJobManagerFactory().getInstance();
        }
        m_customDescription = persistor.getCustomDescription();
        NodeAnnotationData annoData = persistor.getNodeAnnotationData();
        if (annoData != null && !annoData.isDefault()) {
            m_annotation.setData(NodeAnnotationData.builder(annoData, true).build());
        }

        m_uiInformation = persistor.getUIInfo();
        m_nodeLocks = persistor.getNodeLocks();

        setNodeMessage(persistor.getNodeMessage());
        if (!persistor.getLoadHelper().isTemplateFlow()) {
            m_nodeContainerDirectory = persistor.getNodeContainerDirectory();
        }
    }

    /**
     * @return parent workflowmanager holding this node (or null if root).
     */
    public final WorkflowManager getParent() {
        return m_parent;
    }

    /** Returns the {@linkplain #getParent() parent workflow manager}. A {@link WorkflowManager} instance contained
     * in a {@link SubNodeContainer} overrides it to return the subnode (which then is responsible for all the actions).
     * @return the direct node container parent.
     */
    public NodeContainerParent getDirectNCParent() {
        // overridden in wfm
        return getParent();
    }

    /* ----------------- Job Manager ------------------ */

    /**
     * Set a new NodeExecutionJobManager for this node. This also includes
     * all child nodes unless they have their own dedicated job manager.
     * @param je the new job manager.
     */
    void setJobManager(final NodeExecutionJobManager je) {
        synchronized (m_nodeMutex) {
            if (getDirectNCParent() == null && !(je instanceof ThreadNodeExecutionJobManager)) {
                // ROOT and workflow with no parent (inner wfm of subnode) must have the default job manager set
                throw new IllegalArgumentException(String.format("Can only set the default job manager (%s) on a "
                    + "no-parent workflow manager (%s); got %s", ThreadNodeExecutionJobManager.class.getSimpleName(),
                    this.getNameWithID(), je == null ? "<null>" : je.getClass().getSimpleName()));
            }
            if (je != m_jobManager) {
                if (m_jobManager != null) {
                    m_jobManager.closeAllViews();
                }
                m_jobManager = je;
                notifyNodePropertyChangedListener(NodeProperty.JobManager);
            }
        }
    }

    /**
     * @return The job manager associated with this node or null if this
     * node will use the job manager of the parent (or the parent of ...)
     * @see #findJobManager()
     */
    public final NodeExecutionJobManager getJobManager() {
        return m_jobManager;
    }

    /** @return NodeExecutionJobManager responsible for this node and all its children. */
    public final NodeExecutionJobManager findJobManager() {
        if (m_jobManager == null) {
            assert getDirectNCParent() != null : "Root has no associated job manager";
            return getDirectNCParent().findJobManager();
        }
        return m_jobManager;
    }

    /**
     * @param executionJob the executionJob to set
     */
    void setExecutionJob(final NodeExecutionJob executionJob) {
        m_executionJob = executionJob;
    }

    /**
     * @return the executionJob
     */
    NodeExecutionJob getExecutionJob() {
        return m_executionJob;
    }

    public boolean addNodePropertyChangedListener(
            final NodePropertyChangedListener l) {
        return m_nodePropertyChangedListeners.add(l);
    }

    public boolean removeNodePropertyChangedListener(
            final NodePropertyChangedListener l) {
        return m_nodePropertyChangedListeners.remove(l);
    }

    protected void notifyNodePropertyChangedListener(
            final NodeProperty property) {
        NodePropertyChangedEvent e = new NodePropertyChangedEvent(
                getID(), property);
        for (NodePropertyChangedListener l : m_nodePropertyChangedListeners) {
            l.nodePropertyChanged(e);
        }
    }

    /////////////////////////////////////////////////
    // Convenience functions for all derived classes
    /////////////////////////////////////////////////

    /**
     * @return true of this node (or all nodes in this container) are resetable. Please take the
     *         {@link #getNodeLocks()}#hasResetLock() property into account when implementing this method.
     */
    abstract boolean isResetable();

    /**
     * @return true if this node is executed or contains executed nodes. Please take the
     *         {@link #hasResetLock()} into account when implementing this method.
     */
    abstract boolean canPerformReset();

    /** Enable (or disable) queuing of underlying node for execution. This
     * really only changes the state of the node and once all pre-conditions
     * for execution are fulfilled (e.g. configuration succeeded and all
     * ingoing objects are available) the node will be actually queued.
     *
     * @param flag determines if node is marked or unmarked for execution
     * @throws IllegalStateException in case of illegal entry state.
     */
    abstract void markForExecution(final boolean flag);

    /**
     * Change state of marked (for execution) node to queued once it has been assigned to a NodeExecutionJobManager.
     *
     * @param inData the incoming data for the execution
     * @return True if it's now successfully queued, false if it can't be queued as it's still UNCONFIGURED_MARKED.
     * @throws IllegalStateException in case of illegal entry state (not marked).
     */
    boolean queue(final PortObject[] inData) {
        synchronized (m_nodeMutex) {
            if (!performStateTransitionQUEUED()) {
                return false;
            }
            // queue job if state change was successful
            NodeExecutionJobManager jobManager = findJobManager();
            NodeContext.pushContext(this);
            try {
                NodeExecutionJob job = jobManager.submitJob(this, inData);
                setExecutionJob(job);
            } catch (Throwable t) {
                String error = "Failed to submit job to job executor \"" + jobManager + "\": " + t.getMessage();
                setNodeMessage(new NodeMessage(NodeMessage.Type.ERROR, error));
                LOGGER.error(error, t);
                notifyParentPreExecuteStart();
                try {
                    notifyParentExecuteStart();
                } catch (IllegalFlowObjectStackException e) {
                    // ignore, we have something more serious to deal with
                }
                notifyParentPostExecuteStart(NodeContainerExecutionStatus.FAILURE);
                notifyParentExecuteFinished(NodeContainerExecutionStatus.FAILURE);
            } finally {
                NodeContext.removeLastContext();
            }
        }
        return true;
    }

    /** Marks this node as remotely executing. This is necessary if the entire
     * (sub-) flow that this node is part of is executed remotely.
     * @throws IllegalStateException In case of an illegal state transition.
     */
    abstract void mimicRemoteExecuting();

    /** Puts this node (and all its children) into the {@link InternalNodeContainerState#PREEXECUTE}
     * state. This method is used when a workflow is executed remotely.
     * @throws IllegalStateException In case of an illegal state (e.g. a node
     * is already executing).
     */
    abstract void mimicRemotePreExecute();

    /** Puts this node (and all its children) into the {@link InternalNodeContainerState#POSTEXECUTE}
     * state. This method is used when a workflow is executed remotely.
     * @throws IllegalStateException In case of an illegal state.
     */
    abstract void mimicRemotePostExecute();

    /** Put this node into either the {@link InternalNodeContainerState#EXECUTED} or
     * {@link InternalNodeContainerState#IDLE} state depending on the argument. This method is
     * applied recursively on all of this node's children (if a metanode).
     * @param status Where to get the success flag from.
     * @throws IllegalStateException In case of an illegal state.
     */
    abstract void mimicRemoteExecuted(
            final NodeContainerExecutionStatus status);

    /** Called upon load when the node has been saved as remotely executing.
     * @param inData The input data for continued execution.
     * @param settings the reconnect settings.
     * @throws InvalidSettingsException If the settings are invalid
     * @throws NodeExecutionJobReconnectException If that fails for any reason.
     */
    void continueExecutionOnLoad(final PortObject[] inData, final NodeSettingsRO settings)
        throws InvalidSettingsException, NodeExecutionJobReconnectException {
        synchronized (m_nodeMutex) {
            switch (getInternalState()) {
            case EXECUTINGREMOTELY:
                NodeExecutionJobManager jobManager = findJobManager();
                try {
                    NodeExecutionJob job = jobManager.loadFromReconnectSettings(settings, inData, this);
                    setExecutionJob(job);
//                    setState(State.EXECUTINGREMOTELY, false);
                } catch (NodeExecutionJobReconnectException t) {
                    throw t;
                } catch (InvalidSettingsException t) {
                    throw t;
                } catch (Throwable t) {
                    throw new InvalidSettingsException("Failed to continue job on job manager \""
                                                        + jobManager + "\": " + t.getMessage(), t);
                }
                return;
            default:
                throwIllegalStateException();
            }
        }
    }

    /** Called when the workflow is to be disposed. It will cancel this node
     * if it is still running. If this node is being executed remotely (cluster
     * execution) and has been saved, it will just disconnect it. */
    void performShutdown() {
        m_stateChangeListeners.clear();
        m_messageListeners.clear();
        m_progressListeners.clear();
        m_uiListeners.clear();
        m_nodePropertyChangedListeners.clear();
    }

    /** Cancel execution of a marked, queued, or executing node. (Tolerate
     * execute as this may happen throughout cancelation).
     * @throws IllegalStateException
     */
    abstract void cancelExecution();

    void saveNodeExecutionJobReconnectInfo(final NodeSettingsWO settings) {
        assert getInternalState().equals(InternalNodeContainerState.EXECUTINGREMOTELY)
            : "Can't save node execution job, node is not executing remotely but " + getInternalState();
        NodeExecutionJobManager jobManager = findJobManager();
        NodeExecutionJob job = getExecutionJob();
        assert jobManager.canDisconnect(job) : "Execution job can be saved/disconnected";
        jobManager.saveReconnectSettings(job, settings);
        job.setSavedForDisconnect(true);
    }

    /**
     * Invoked by job manager when the execution starts. This method will invoke
     * the {@link WorkflowManager#doBeforePreExecution(NodeContainer)} method in
     * this node's parent. It will then call back on
     * {@link #performStateTransitionPREEXECUTE()} to allow for a synchronized
     * state transition.
     * @return true if the node did an actual state transition. It may abort the
     * state change if the job was cancel (cancel checking is to be done in the
     * synchronized block -- therefore the return value)
     */
    boolean notifyParentPreExecuteStart() {
        return getParent().doBeforePreExecution(this);
    }

    /**
     * Invoked by the job executor immediately before the execution is
     * triggered. It invokes doBeforeExecution on the parent.
     *
     * @throws IllegalFlowObjectStackException in case of wrongly connected
     *             loops, for instance.
     */
    void notifyParentExecuteStart() {
        // this will allow the parent to call state changes etc properly
        // synchronized. The main execution is done asynchronously.
        try {
            getParent().doBeforeExecution(this);
        } catch (IllegalFlowObjectStackException e) {
            LOGGER.warn(e.getMessage(), e);
            setNodeMessage(new NodeMessage(NodeMessage.Type.ERROR, e.getMessage()));
            throw e;
        }
    }

    /**
     * Invoked by job manager when the execution is finishing. This method will
     * invoke the {@link WorkflowManager#doBeforePostExecution(NodeContainer, NodeContainerExecutionStatus)}
     * method in this node's parent. It will then call back on
     * {@link #performStateTransitionPOSTEXECUTE()} to allow for a synchronized
     * state transition.
     * @param status The execution status.
     */
    void notifyParentPostExecuteStart(final NodeContainerExecutionStatus status) {
        getParent().doBeforePostExecution(this, status);
    }

    /**
     * Called immediately after the execution took place in the job executor. It
     * will trigger an doAfterExecution on the parent wfm.
     *
     * @param status Whether the execution was successful.
     */
    final void notifyParentExecuteFinished(
            final NodeContainerExecutionStatus status) {
        // clean up stuff and especially change states synchronized again
        getParent().doAfterExecution(this, status);
    }

    /** Called from {@link #queue(PortObject[])} to set state from marked to queued. It's overwritten in
     * {@link SubNodeContainer} to be more ignorant with state checks.
     * @return true if the state transition was done and the node can now be executed. False if it was marked but
     * it cannot be executed (coming from {@link InternalNodeContainerState#UNCONFIGURED_MARKEDFOREXEC}). The calling
     * code will then clear the mark flags.
     * @throws IllegalStateException If not coming from 'marked' state. */
    boolean performStateTransitionQUEUED() {
        assert Thread.holdsLock(m_nodeMutex);
        // switch state from marked to queued
        switch (getInternalState()) {
        case CONFIGURED_MARKEDFOREXEC:
            setInternalState(InternalNodeContainerState.CONFIGURED_QUEUED);
            break;
        case EXECUTED_MARKEDFOREXEC:
            setInternalState(InternalNodeContainerState.EXECUTED_QUEUED);
            break;
        case UNCONFIGURED_MARKEDFOREXEC:
            return false;
        default:
            throwIllegalStateException();
        }
        return true;
    }

    /** Called when the state of a node should switch from
     * {@link State#QUEUED} to {@link State#PREEXECUTE}. The method is to be
     * called from the node's parent in a synchronized environment.
     * @return whether there was an actual state transition, false if the
     *         execution was canceled (cancel checking to be done in
     *         synchronized block) */
    abstract boolean performStateTransitionPREEXECUTE();

    /**
     * This should be used to change the nodes states correctly (and likely
     * needs to be synchronized with other changes visible to successors of this
     * node as well!) BEFORE the actual execution. The main reason is that the
     * actual execution should be performed unsynchronized!
     */
    abstract void performStateTransitionEXECUTING();

    /** Called when the state of a node should switch from
     * {@link State#EXECUTING} (or {@link State#EXECUTINGREMOTELY}) to
     * {@link State#POSTEXECUTE}. The method is to be called from the node's
     * parent in a synchronized environment. */
    abstract void performStateTransitionPOSTEXECUTE();

    /**
     * This should be used to change the nodes states correctly (and likely
     * needs to be synchronized with other changes visible to successors of this
     * node as well!) AFTER the actual execution. The main reason is that the
     * actual execution should be performed unsynchronized!
     *
     * @param status indicates if execution was successful
     */
    abstract void performStateTransitionEXECUTED(final NodeContainerExecutionStatus status);

    /////////////////////////////////////////////////
    // List Management of Waiting Loop Head Nodes
    /////////////////////////////////////////////////

    /** add a loop to the list of waiting loops.
     *
     * @param slc FlowLoopContext object of the loop.
     */
    public void addWaitingLoop(final FlowLoopContext slc) {
        if (!m_listOfWaitingLoops.contains(slc)) {
            m_listOfWaitingLoops.add(slc);
        }
    }

    /**
     * @return a list of waiting loops (well: their FlowLoopContext objects)
     */
    public List<FlowLoopContext> getWaitingLoops() {
        return m_listOfWaitingLoops;
    }

    /** clears the list of waiting loops.
     */
    public void clearWaitingLoopList() {
        m_listOfWaitingLoops.clear();
    }

    /** Remove element from list of waiting loops.
     *
     * @param so loop to be removed.
     */
    public void removeWaitingLoopHeadNode(final FlowObject so) {
        if (m_listOfWaitingLoops.contains(so)) {
            m_listOfWaitingLoops.remove(so);
        }
    }

    ///////////////////////////////
    // Listener administration
    ////////////////////////////////////


    /* ----------- progress ----------*/

    /**
     * @return the progressMonitor
     */
    NodeProgressMonitor getProgressMonitor() {
        return m_progressMonitor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void progressChanged(final NodeProgressEvent pe) {
        // set our ID as source ID
        NodeProgressEvent event = new NodeProgressEvent(getID(), pe.getNodeProgress());
        // forward the event
        notifyProgressListeners(event);
    }

    /**
    *
    * @param listener listener to the node progress
    * @return true if the listener was not already registered before, false
    *         otherwise
    */
   public boolean addProgressListener(final NodeProgressListener listener) {
       if (listener == null) {
           throw new NullPointerException("Node progress listener must not be null");
       }
       return m_progressListeners.add(listener);
   }


   /**
    *
    * @param listener existing listener to the node progress
    * @return true if the listener was successfully removed, false if it was
    *         not registered
    */
   public boolean removeNodeProgressListener(final NodeProgressListener listener) {
       return m_progressListeners.remove(listener);
   }

   /**
    * Notifies all registered {@link NodeProgressListener}s about the new
    * progress.
    *
    * @param e the new progress event
    */
   protected void notifyProgressListeners(final NodeProgressEvent e) {
       for (NodeProgressListener l : m_progressListeners) {
           l.progressChanged(e);
       }
   }


   /* ------------- message ---------------------*/

   /**
    *
    * @param listener listener to the node messages (warnings and errors)
    * @return true if the listener was not already registered, false otherwise
    */
   public boolean addNodeMessageListener(final NodeMessageListener listener) {
       if (listener == null) {
           throw new NullPointerException("Node message listner must not be null!");
       }
       return m_messageListeners.add(listener);
   }

   /**
    *
    * @param listener listener to the node messages
    * @return true if the listener was successfully removed, false if it was not
    *         registered
    */
   public boolean removeNodeMessageListener(final NodeMessageListener listener) {
       return m_messageListeners.remove(listener);
   }

   /** Get the message to be displayed to the user.
    * @return the node message consisting of type and message, never null. */
   public final NodeMessage getNodeMessage() {
       return m_nodeMessage;
   }

   /**
    * @param newMessage the nodeMessage to set
    */
   public final void setNodeMessage(final NodeMessage newMessage) {
       NodeMessage oldMessage = m_nodeMessage;
       m_nodeMessage = newMessage == null ? NodeMessage.NONE : newMessage;
       if (!m_nodeMessage.equals(oldMessage)) {
           notifyMessageListeners(new NodeMessageEvent(getID(), m_nodeMessage));
       }
   }

   /**
    * Notifies all registered {@link NodeMessageListener}s about the new
    * message.
    *
    * @param e the new message event
    */
   protected final void notifyMessageListeners(final NodeMessageEvent e) {
       for (NodeMessageListener l : m_messageListeners) {
           l.messageChanged(e);
       }
   }

   /* ---------------- UI -----------------*/

   public void addUIInformationListener(final NodeUIInformationListener l) {
       if (l == null) {
           throw new NullPointerException("NodeUIInformationListener must not be null!");
       }
       m_uiListeners.add(l);
   }

   public void removeUIInformationListener(final NodeUIInformationListener l) {
       m_uiListeners.remove(l);
   }

   protected void notifyUIListeners(final NodeUIInformationEvent evt) {
       for (NodeUIInformationListener l : m_uiListeners) {
           l.nodeUIInformationChanged(evt);
       }
   }

   /**
    * Returns the UI information.
    *
    * @return a the node information
    */
   public NodeUIInformation getUIInformation() {
       return m_uiInformation;
   }

   /**
    *
    * @param uiInformation new user interface information of the node such as
    *   coordinates on workbench.
    */
   public void setUIInformation(final NodeUIInformation uiInformation) {
       // ui info is a property of the outer workflow (it just happened
       // to be a field member of this class)
       // there is no reason on settings the dirty flag when changed.
       m_uiInformation = uiInformation;
       notifyUIListeners(new NodeUIInformationEvent(m_id, m_uiInformation, m_customDescription));
   }


    /* ------------------ state ---------------*/

    /**
     * Notifies all registered {@link NodeStateChangeListener}s about the new
     * state.
     *
     * @param e the new state change event
     */
    protected void notifyStateChangeListeners(final NodeStateEvent e) {
        for (NodeStateChangeListener l : m_stateChangeListeners) {
            l.stateChanged(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addNodeStateChangeListener(final NodeStateChangeListener listener) {
        if (listener == null) {
            throw new NullPointerException("Node state change listener must not be null!");
        }
        return m_stateChangeListeners.add(listener);
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeNodeStateChangeListener(final NodeStateChangeListener listener) {
        return m_stateChangeListeners.remove(listener);
    }

    /** {@inheritDoc}
     * @since 2.8 */
    @Override
    public NodeContainerState getNodeContainerState() {
        return getInternalState();
    }

    /**
     * @return the status of this node
     */
    InternalNodeContainerState getInternalState() {
        return m_state;
    }

    /**
     * @return the status of this node
     */
    @Deprecated
    public State getState() {
        return getInternalState().mapToOldStyleState();
    }

    /** Set new status and notify listeners.
     * @param state the new state
     */
    void setInternalState(final InternalNodeContainerState state) {
        setInternalState(state, true);
    }

    /** Set new status and notify listeners.
     * @param state the new state
     * @param setDirty whether to set this node &quot;dirty&quot; (needs save).
     * @return true if state was changed.
     */
    boolean setInternalState(final InternalNodeContainerState state, final boolean setDirty) {
        if (state == null) {
            throw new NullPointerException("State must not be null.");
        }
        boolean changesMade = false;
        synchronized (m_nodeMutex) {
            if (!m_state.equals(state)) {
                m_state = state;
                changesMade = true;
            }
        }
        // TODO: This is sometimes (always?) synchronized on m_nodeMutex as
        // the calling method is sync'ed...
        // I ran into a deadlock (see Email to Michael on 11.4.08)
        if (changesMade) {
            if (setDirty) {
                setDirty();
            }
            notifyStateChangeListeners(new NodeStateEvent(getID(), m_state));
            LOGGER.debug(this.getNameWithID() + " has new state: " + m_state);
        }
        return changesMade;
    }

    /** Throws a new IllegalStateException with a meaningful error message
     * containing node name, current state and method name. This method is used
     * from the different state transition methods in
     * {@link SingleNodeContainer} and {@link WorkflowManager}.
     */
    protected void throwIllegalStateException() {
        String name = getNameWithID();
        String state = "\"" + getInternalState().toString() + "\"";
        String methodName = "<unknown>";
        String clazz = "";
        StackTraceElement[] callStack = Thread.currentThread().getStackTrace();
        // top most element is this method, at index [1] we find the calling
        // method name.
        if (callStack.length > 3) {
            clazz = "\"" + callStack[2].getClassName() + "#";
            methodName = callStack[2].getMethodName() + "\"";
        }
        throw new IllegalStateException("Illegal state " + state
                + " encountered in method " + clazz + methodName
                + " in node " + name);
    }

    /* ------------ dialog -------------- */

    /** Whether the dialog is a {@link org.knime.core.node.DataAwareNodeDialogPane}. If so,
     * the predecessor nodes need to be executed before the dialog is opened.
     * @return that property
     * @since 2.6
     */
    public boolean hasDataAwareDialogPane() {
        return false;
    }

    /** Return a NodeDialogPane for a node which can be embedded into
     * a JFrame oder another GUI element.
     *
     * @return A dialog pane for the corresponding node.
     * @throws NotConfigurableException if node cannot be configured
     */
    public NodeDialogPane getDialogPaneWithSettings()
        throws NotConfigurableException {
        if (!hasDialog()) {
            throw new IllegalStateException(
                    "Node \"" + getName() + "\" has no dialog");
        }
        final int nrInPorts = getNrInPorts();
        final NodeID id = getID();
        PortObjectSpec[] inputSpecs = new PortObjectSpec[nrInPorts];
        PortObject[] inputData = new PortObject[nrInPorts];
        m_parent.assembleInputSpecs(id, inputSpecs);
        m_parent.assembleInputData(id, inputData);
        NodeContext.pushContext(this);
        try {
            return getDialogPaneWithSettings(inputSpecs, inputData);
        } finally {
            NodeContext.removeLastContext();
        }
    }

    /** Called for nodes having {@linkplain org.knime.core.node.DataAwareNodeDialogPane data aware dialogs} in order
     * to check whether to prompt for execution or not.
     * @return true if correctly connected and all inputs have data.
     * @since 2.8
     * @see WorkflowManager#isAllInputDataAvailableToNode(NodeID)
     * @noreference This method is not intended to be referenced by clients.
     */
    public final boolean isAllInputDataAvailable() {
        return getParent().isAllInputDataAvailableToNode(m_id);
    }

    /** Currently called by nodes having {@linkplain org.knime.core.node.DataAwareNodeDialogPane data aware dialogs}
     * in order to test whether upstream nodes are correctly wired and can be executed. It only tests if the direct
     * predecessors are connected -- in the longer term it will check if all predecessors are correctly set up and
     * at least one is executable.
     * @return true if all non-optional ports are connected.
     * @since 2.8
     * @noreference This method is not intended to be referenced by clients.
     */
    public final boolean canExecuteUpToHere() {
        return getParent().isFullyConnected(m_id);
    }

    /** Launch a node dialog in its own JFrame (a JDialog).
     *
     * @throws NotConfigurableException if node cannot be configured
     */
    public void openDialogInJFrame()
    throws NotConfigurableException {
        NodeDialog nd = new NodeDialog(getDialogPaneWithSettings(), this);
        nd.openDialog();
    }

    /** Take settings from the node's dialog and apply them to the model. Throws
     * an exception if the apply fails.
     *
     * @throws InvalidSettingsException if settings are not applicable.
     */
    public void applySettingsFromDialog() throws InvalidSettingsException {
        CheckUtils.checkState(hasDialog(), "Node \"%s\" has no dialog", getName());
        // TODO do we need to reset the node first??
        NodeSettings sett = new NodeSettings("node settings");
        NodeContext.pushContext(this);
        try {
            getDialogPane().finishEditingAndSaveSettingsTo(sett);
            m_parent.loadNodeSettings(getID(), sett);
        } finally {
            NodeContext.removeLastContext();
        }
    }

    public boolean areDialogSettingsValid() {
        CheckUtils.checkState(hasDialog(), "Node \"%s\" has no dialog", getName());
        NodeSettings sett = new NodeSettings("node settings");
        NodeContext.pushContext(this);
        try {
            getDialogPane().finishEditingAndSaveSettingsTo(sett);
            validateSettings(sett);
            return true;
        } catch (InvalidSettingsException nce) {
            return false;
        } finally {
            NodeContext.removeLastContext();
        }
    }

    /* --------------- Dialog handling --------------- */

    public abstract boolean hasDialog();


    abstract NodeDialogPane getDialogPaneWithSettings(
            final PortObjectSpec[] inSpecs, final PortObject[] inData)
            throws NotConfigurableException;

    abstract NodeDialogPane getDialogPane();

    public abstract boolean areDialogAndNodeSettingsEqual();

    void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        /*
         * see #loadCommonSettings
         */
        NodeContainerSettings ncSet = new NodeContainerSettings();
        ncSet.setJobManager(getJobManager());
        ncSet.load(settings);
        // the job manager instance will be the same, if settings permit
        setJobManager(ncSet.getJobManager());
        setDirty();
    }

    void loadCommonSettings(final NodeContainerSettings s) throws InvalidSettingsException {
        /*
         * this is awkward. We don't have a member for NodeContainerSettings.
         * This object is just created to load (and save) the node container
         * settings. Now, we want to preserve the job manager instance, if
         * possible (i.e. if the same type of mgr is set, just with new
         * settings). Thus, we set it in the new NodeContainerSettings instance,
         * that is overloaded then - but it takes care of the mgr set.
         */
        NodeContainerSettings ncSet = new NodeContainerSettings();
        ncSet.setJobManager(getJobManager());
        NodeSettings tempSettings = new NodeSettings("temp");
        s.save(tempSettings);
        ncSet.load(tempSettings);
        // the job manager instance will be the same, if settings permit
        setJobManager(ncSet.getJobManager());
        setDirty();
    }

    void saveSettings(final NodeSettingsWO settings) {
        NodeContainerSettings ncSet = new NodeContainerSettings();
        ncSet.setJobManager(m_jobManager);
        ncSet.save(settings);
    }

    void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new NodeContainerSettings().load(settings);
    }

    SplitType getSplitType() {
        if (this instanceof WorkflowManager) {
            return NodeContainerSettings.SplitType.DISALLOWED;
        }
        if (this instanceof SingleNodeContainer) {
            // TODO: distinguish between ThreadedNodeModel and "simple" node
            return NodeContainerSettings.SplitType.USER;
        }
        // every thing not know is simple node.
        return NodeContainerSettings.SplitType.USER;
    }

    /* ------------- ports --------------- */

    public abstract int getNrInPorts();

    public abstract NodeInPort getInPort(final int index);

    public abstract NodeOutPort getOutPort(final int index);

    public abstract int getNrOutPorts();

    /* -------------- views ---------------- */


    public int getNrViews() {

        int numOfNodeViews = getNrNodeViews();

        // TODO: Assuming that the Default has not views!!!
        if (getJobManager() != null && getJobManager().hasView()) {
            // job managers have only one view
            return numOfNodeViews + 1;
        }

        return numOfNodeViews;
    }

    /**
     * Returns the number of views provided by the node implementation.
     * @return the number of views provided by the node implementation
     */
    public abstract int getNrNodeViews();

    public String getViewName(final int i) {
        if (i < getNrNodeViews()) {
            return getNodeViewName(i);
        } else {
            assert getJobManager() != null : "Job Manager changed: illegal view idx";
            return "Job Manager View";
        }
    }

    public abstract String getNodeViewName(final int i);

    public AbstractNodeView<NodeModel> getView(final int i) {
        if (i < getNrNodeViews()) {
            return getNodeView(i);
        } else {
            NodeContext.pushContext(this);
            try {
                assert getJobManager() != null : "Job Manager changed: No view!!";
                return getJobManager().getView(this);
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    /**
     * Return the view with the specified index provided by the node.
     *
     * @param i the view to create
     * @return a new view instance with index i provided by the node
     */
    public abstract AbstractNodeView<NodeModel> getNodeView(final int i);

    /**
     * Must be called when the node is reset.
     */
    protected void resetJobManagerViews() {
        if (getJobManager() != null && getJobManager().hasView()) {
            getJobManager().resetAllViews();
        }
    }

    protected void closeAllJobManagerViews() {
        if (getJobManager() != null && getJobManager().hasView()) {
            getJobManager().closeAllViews();
        }
    }

    /* ------------- Interactive Views ---------------*/

    /**
     * @return true if node provides an interactive view.
     * @since 2.8
     */
    public abstract boolean hasInteractiveView();

    /** Get the 'interactive web views' provided by this node. That is, views providing a {@link WebTemplate} for an interactive
     * web view. {@link NativeNodeContainer} can have at most one view, {@link SubNodeContainer} may have many (one for
     * each contained view node), {@link WorkflowManager} have none.
     *
     * <p>The model for the view is (currently) a {@link NodeModel} underlying the native node as the view itself
     * lives in the UI code and has a strong dependency to the SWT browser / eclipse code.
     *
     * <p>The name associated with the web view (e.g. JS scatter plot) comes from a node's description (xml).
     * @return An new {@link InteractiveWebViewsResult} with possibly 0 or more views.
     * @since 3.3
     */
    public abstract InteractiveWebViewsResult getInteractiveWebViews();

    /**
     * Returns the name of the interactive view if such a view exists. Otherwise <code>null</code> is returned.
     *
     * @return name of the interactive view or <code>null</code>
     * @since 2.8
     */
    public abstract String getInteractiveViewName();

    /**
     * @return interactive view.
     * @since 2.8
     */
    public abstract <V extends AbstractNodeView<?> & InteractiveView<?,? extends ViewContent, ? extends ViewContent>> V getInteractiveView();

    /* ------------- Misc node info -------------- */

    /** The input stack associated with this node - for ordinary nodes this is the the merged stack of the input
     * (ignoring any variables pushed by the node itself), for workflows this is the workflow variable "stack".
     * @return The stack, usually not null when used in "normal operation" (possible TODO: unset the stack when node
     * is reset).
     * @since 3.1 */
    public abstract FlowObjectStack getFlowObjectStack();

    public abstract URL getIcon();

    public abstract NodeType getType();

    public final NodeID getID() {
        return m_id;
    }

    public abstract String getName();

    public final String getNameWithID() {
        return getName() + " " + getID().toString();
    }

    /** @return Node name with status information.  */
    @Override
    public String toString() {
        return getNameWithID() + " (" + getInternalState() + ")";
    }

    /**
     * @return the display label for {@link NodeView}, {@link OutPortView} and
     * {@link NodeDialog}
     */
    public String getDisplayLabel() {
        String label = getID().toString() + " - "
            + getName();
        // if this node has an annotation add the first line to the label
        String customLabel = getDisplayCustomLine();
        if (!customLabel.isEmpty()) {
            label += " (" + customLabel + ")";
        }
        return label;
    }

    /**
     * @return the first line of the annotation - if the node has any, otherwise
     *         an empty string. At max 33 chars.
     */
    protected String getDisplayCustomLine() {
        String annoLine = getFirstAnnotationLine();
        if (annoLine.length() > 30) {
            return annoLine.substring(0, 30) + "...";
        } else {
            return annoLine;
        }
    }

    /**
     * @return the first line of the annotation (trimmed) - if the node has any,
     *         otherwise an empty string.
     */
    private String getFirstAnnotationLine() {
        String result = "";
        if (m_annotation != null) {
            String annoStr = m_annotation.getText();
            if (annoStr != null && !annoStr.isEmpty()) {
                // extract the (not-empty) first line (or 30 chars max)
                int lf = 0;
                while (lf < annoStr.length()) {
                    if (annoStr.charAt(lf) == '\n'
                            || annoStr.charAt(lf) == '\r'
                            || annoStr.charAt(lf) == ' ') {
                    lf++;
                    } else {
                        break;
                }
                }
                int firstLF = annoStr.indexOf('\n', lf);
                if (firstLF < 0) {
                    // no LF - use entire string.
                    firstLF = annoStr.length();
                }
                result = annoStr.substring(lf, firstLF).trim();
            }
        }
        return result;
    }

    /**
     * For reporting backward compatibility. If no custom name is set the
     * reporting creates new names (depending on the node id). (The preference
     * page prefix is ignored then.)
     *
     * @return the first line of the annotation (which contains in old (pre 2.5)
     *         flows the custom name) or null, if no annotation or no old custom
     *         name is set.
     */
    public String getCustomName() {
        String annoLine = getFirstAnnotationLine();
        if (annoLine.isEmpty()) {
            return null;
        } else {
            return annoLine;
        }
    }

    /**
     * @return the annotation associated with the node, never null.
     */
    public NodeAnnotation getNodeAnnotation() {
        return m_annotation;
    }

    public String getCustomDescription() {
        return m_customDescription;
    }

    public void setCustomDescription(final String customDescription) {
        boolean notify = false;
        synchronized (m_nodeMutex) {
            if (!ConvenienceMethods.areEqual(
                    customDescription, m_customDescription)) {
                m_customDescription = customDescription;
                setDirty();
                notify = true;
            }
        }
        if (notify) {
            notifyUIListeners(new NodeUIInformationEvent(m_id, m_uiInformation,
                    m_customDescription));
        }
    }

    /**
     * @return an object holding information about execution frequency and times.
     * @since 2.12
     */
    public NodeTimer getNodeTimer() {
        return m_nodeTimer;
    }

    /** Is this node a to be locally executed workflow. In contrast to remotely
     * executed workflows, the nodes in the encapsulated workflow will be
     * executed independently (each represented by an own job), whereas remote
     * execution means that the entire workflow execution is one single job.
     * <p>This method returns false for all single node container.
     * @return The above described property.
     */
    protected abstract boolean isLocalWFM();

    /** @param value the isDeletable to set */
    public void setDeletable(final boolean value) {
        if (m_nodeLocks.hasDeleteLock() == value) {
            changeNodeLocks(!value, NodeLock.DELETE);
        }
    }

    /** @return the isDeletable */
    public boolean isDeletable() {
        return !m_nodeLocks.hasDeleteLock();
    }

    /** Method that's called when the node is discarded. The single node
     * container overwrites this method and cleans the outport data of the
     * node (deletes temp files).
     */
    void cleanup() {
        if (m_annotation != null) {
            m_annotation.unregisterFromNodeContainer();
        }
        closeAllJobManagerViews();
        for (int i = 0; i < getNrOutPorts(); i++) {
            NodeOutPort outPort = getOutPort(i);
            outPort.disposePortView();
        }
    }

    /**
     * @return the isDirty
     */
    public final boolean isDirty() {
        final ReferencedFile ncDirectory = getNodeContainerDirectory();
        return ncDirectory == null || ncDirectory.isDirty() || internalIsDirty();
    }

    /**
     * @return If this node is dirty
     * @since 2.10
     */
    protected boolean internalIsDirty() {
        return false;
    }

    /** Sets the dirty flag on the referenced file (node container directory or auto save directory).
     * @param directory either the auto save directory or the node container directory (asserted)
     * @return true if this changed the dirty state; that is, the location was previously marked as clean. It returns
     *         false if the location is not set yet (as it is dirty by definition then)
     */
    boolean setDirty(final ReferencedFile directory) {
        assert directory == null || directory.equals(getAutoSaveDirectory())
                || directory.equals(getNodeContainerDirectory())
                : "Location must be either auto-save or node container directory: " + directory;
        return directory != null && directory.setDirty(true);
    }

    /** Called from persistor when node has been saved. */
    void unsetDirty() {
        final ReferencedFile ncDirectory = getNodeContainerDirectory();
        assert ncDirectory != null : "NC directory must not be null at this point";
        ncDirectory.setDirty(false);
    }

    /**
     * Mark this node container to be changed, that is, it needs to be saved.
     */
    public void setDirty() {
        if (setDirty(getNodeContainerDirectory())) {
            LOGGER.debug("Setting dirty flag on " + getNameWithID());
        }
        setDirty(getAutoSaveDirectory());
        if (m_parent != null) {
            m_parent.setDirty();
        }
    }

    /** Get a new persistor that is used to copy this node (copy&amp;paste action).
     * @param tableRep Table repository of the destination.
     * @param fileStoreHandlerRepository file store handler of destination
     * @param preserveDeletableFlags Whether the "isdeleteable" annotation
     * should be copied also (false when individual nodes are copied
     * but true when an entire metanode is copied).
     * @param isUndoableDeleteCommand If to keep the location of the
     *        node directories (important for undo of delete commands, see
     *        {@link WorkflowManager#copy(boolean, WorkflowCopyContent)}
     *        for details.)
     * @return A new persistor for copying. */
    protected abstract NodeContainerPersistor getCopyPersistor(
            final HashMap<Integer, ContainerTable> tableRep,
            FileStoreHandlerRepository fileStoreHandlerRepository,
            final boolean preserveDeletableFlags,
            final boolean isUndoableDeleteCommand);

    /**
     * @param directory the nodeContainerDirectory to set
     */
    protected void setNodeContainerDirectory(final ReferencedFile directory) {
        if (directory == null || !directory.getFile().isDirectory()) {
            throw new IllegalArgumentException("Not a directory: " + directory);
        }
        m_nodeContainerDirectory = directory;
    }

    /**
     * @param autoSaveDirectory the autoSaveDirectory to set
     * @noreference This method is not intended to be referenced by clients.
     */
    public void setAutoSaveDirectory(final ReferencedFile autoSaveDirectory) {
        m_autoSaveDirectory = autoSaveDirectory;
    }

    /**
     * Returns the directory for this node container. If the node has not been persisted yet, <code>null</code> is
     * returned.
     *
     * @return a directory or <code>null</code>
     * @noreference this is not part of the public API
     */
    public final ReferencedFile getNodeContainerDirectory() {
        return m_nodeContainerDirectory;
    }

    /** @return The directory for auto-saving (or null if not auto-saved yet).
     * @noreference This method is not intended to be referenced by clients. */
    public final ReferencedFile getAutoSaveDirectory() {
        return m_autoSaveDirectory;
    }

    /** Restore content from persistor. This represents the second step
     * when loading a workflow.
     * @param persistor To load from.
     * @param tblRep A table repository to restore BufferedDatTables
     * @param inStack Incoming {@link FlowObjectStack}.
     * @param exec For progress
     * @param loadResult Where to report errors/warnings to
     * @param preserveNodeMessage Whether possible node messages in the
     *        persistor are to be preserved (parameter to configure method
     *        that is called during load).
     * @return The workflow content that was inserted (NodeID's and
     *         annotations), for single node containers the result is null.
     * @throws CanceledExecutionException If canceled.
     */
    abstract WorkflowCopyContent loadContent(
            final NodeContainerPersistor persistor,
            final Map<Integer, BufferedDataTable> tblRep,
            final FlowObjectStack inStack, final ExecutionMonitor exec,
            final LoadResult loadResult, final boolean preserveNodeMessage)
            throws CanceledExecutionException;

    /** Load information from execution result. Subclasses will override this
     * method and will call this implementation as <code>super.loadEx...</code>.
     * @param result The execution result (contains port objects, messages, etc)
     * @param exec For progress information (no cancelation supported)
     * @param loadResult A load result that contains, e.g. error messages.
     */
    public void loadExecutionResult(
            final NodeContainerExecutionResult result,
            final ExecutionMonitor exec, final LoadResult loadResult) {
        /* Ideally this code would go into a separate final method that calls
         * an abstract method .... however, this is risky as subclasses may
         * wish to synchronize the entire load procedure.
         */
        setNodeMessage(result.getNodeMessage());
    }

    /** Saves all internals that are necessary to mimic the computed result
     * into a new execution result object. This method is called on node
     * instances, which are, e.g. executed on a server and later on read back
     * into a true KNIME instance (upon which
     * {@link #loadExecutionResult(NodeContainerExecutionResult, ExecutionMonitor, LoadResult)}
     * is called).
     * @param exec For progress information (this method will copy port
     *        objects).
     * @return A new execution result instance.
     * @throws CanceledExecutionException If canceled.
     */
    public abstract NodeContainerExecutionResult createExecutionResult(
            final ExecutionMonitor exec) throws CanceledExecutionException;


    /** Saves all information that is held in this abstract NodeContainer
     * into the argument.
     * @param result Where to save to.
     */
    protected void saveExecutionResult(
            final NodeContainerExecutionResult result) {
        result.setSuccess(getInternalState().equals(InternalNodeContainerState.EXECUTED));
        result.setMessage(m_nodeMessage);
    }

    /**
     * @return ExecutionEnvironment of this node.
     * @since 2.8
     */
    protected ExecutionEnvironment getExecutionEnvironment() {
        return m_executionEnv;
    }

    /**
     * @param exEnv new ExecutionEnvironment of this node.
     * @since 2.8
     */
    protected void setExecutionEnvironment(final ExecutionEnvironment exEnv) {
        assert exEnv == null || ((m_executionEnv == null) && (exEnv != null))
                : "Execution Environment set on unclean Environment (" + toString() +")";
        m_executionEnv = exEnv;
    }

    /** Helper class that defines load/save routines for general NodeContainer
     * properties. This is currently only the job manager. */
    public static final class NodeContainerSettings {

        public enum SplitType {
            /** Node can't handle it. */
            DISALLOWED,
            /** Node is designed for splitting. */
            SUPPORTED,
            /** May work... */
            USER
        }
        private NodeExecutionJobManager m_jobManager;

        /** @param jobManager the jobManager to set */
        public void setJobManager(final NodeExecutionJobManager jobManager) {
            m_jobManager = jobManager;
        }

        /** @return the jobManager */
        public NodeExecutionJobManager getJobManager() {
            return m_jobManager;
        }

        /** Save all properties (currently only job manager) to argument.
         * @param settings To save to.
         */
        public void save(final NodeSettingsWO settings) {
            if (m_jobManager != null) {
                NodeExecutionJobManagerPool.saveJobManager(
                        m_jobManager, settings.addNodeSettings("job.manager"));
            }
        }

        /**
         * Restores all settings (currently only job manager and its
         * settings) from argument.
         *
         * @param settings To load from.
         * @throws InvalidSettingsException If that's not possible.
         */
        public void load(final NodeSettingsRO settings)
                throws InvalidSettingsException {
            if (settings.containsKey("job.manager")) {
                NodeSettingsRO s = settings.getNodeSettings("job.manager");
                m_jobManager =
                        NodeExecutionJobManagerPool.load(m_jobManager, s);
            } else {
                m_jobManager = null;
            }
        }

    }


    /* ------------- Node Locking -------------- */

    /**
     * Changes the nodes lock status for various actions, i.e. from being deleted, reseted or configured.
     *
     * @param setLock whether the locks should be set (<code>true</code>) or released (<code>false</code>)
     * @param locks the locks to be set or released, e.g. {@link NodeLock#DELETE}, {@link NodeLock#RESET},
     *            {@link NodeLock#CONFIGURE}
     * @since 3.2
     */
    public void changeNodeLocks(final boolean setLock, final NodeLock... locks) {
        boolean deleteLock = m_nodeLocks.hasDeleteLock();
        boolean resetLock = m_nodeLocks.hasResetLock();
        boolean configureLock = m_nodeLocks.hasConfigureLock();
        for (NodeLock nl : locks) {
            switch (nl) {
                case DELETE:
                    deleteLock = setLock;
                    break;
                case RESET:
                    resetLock = setLock;
                    break;
                case CONFIGURE:
                    configureLock = setLock;
                    break;
                case ALL:
                    deleteLock = setLock;
                    resetLock = setLock;
                    configureLock = setLock;
                default:
                    break;
            }
        }
        if (deleteLock != m_nodeLocks.hasDeleteLock() || resetLock != m_nodeLocks.hasResetLock()
            || configureLock != m_nodeLocks.hasConfigureLock()) {
            notifyNodePropertyChangedListener(NodeProperty.LockStatus);
            m_nodeLocks = new NodeLocks(deleteLock, resetLock, configureLock);
        }
    }


    /**
     * Returns the node's lock status, i.e. whether the node is locked from being deleted, reseted or configured.
     *
     * @return the currently set {@link NodeLocks}
     * @since 3.2
     */
    public NodeLocks getNodeLocks() {
        return m_nodeLocks;
    }
}
