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
 *   29.03.2007 (mb): created
 */
package org.knime.core.node.workflow;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialog;
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
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.node.util.NodeExecutionJobManagerPool;
import org.knime.core.node.workflow.NodeContainer.NodeContainerSettings.SplitType;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.execresult.NodeContainerExecutionResult;
import org.knime.core.node.workflow.execresult.NodeContainerExecutionStatus;

/**
 * Abstract super class for containers holding node or just structural
 * information of a meta node. Also stores additional (optional) information
 * such as coordinates on a workflow layout.
 *
 * @author M. Berthold/B. Wiswedel, University of Konstanz
 */
public abstract class NodeContainer implements NodeProgressListener {

    /** my logger. */
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(NodeContainer.class);

    /** possible status values of a NodeContainer. */
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

    private State m_state;

    private final NodeID m_id;

    private final WorkflowManager m_parent;

    private NodeExecutionJobManager m_jobManager;

    /** The job representing the pending task of executing the node. */
    private NodeExecutionJob m_executionJob;

    /** progress monitor. */
    private final NodeProgressMonitor m_progressMonitor =
            new DefaultNodeProgressMonitor(this);

    private NodeMessage m_nodeMessage = NodeMessage.NONE;

    private boolean m_isDeletable;

    /** this list will hold ScopeObjects of loops in the pipeline which can not
     * be executed before this one is not done - usually these are loops
     * with "dangling" branches, e.g. a chain of nodes leaving the loop.
     */
    private ArrayList<ScopeLoopContext> m_listOfWaitingLoops
                                        = new ArrayList<ScopeLoopContext>();

    private String m_customName;

    private String m_customDescription;

    private ReferencedFile m_nodeContainerDirectory;

    private boolean m_isDirty;

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

    private final CopyOnWriteArraySet<JobManagerChangedListener> m_jobManagerListeners =
            new CopyOnWriteArraySet<JobManagerChangedListener>();

    private UIInformation m_uiInformation;


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
            m_jobManager =
                    NodeExecutionJobManagerPool.getDefaultJobManagerFactory()
                            .getInstance();
        }
        m_id = id;
        m_state = State.IDLE;
        m_isDeletable = true;
    }

    NodeContainer(final WorkflowManager parent, final NodeID id,
            final NodeContainerMetaPersistor persistor) {
        this(parent, id);
        assert parent != null;
        assert persistor.getState() != null : "State of node \"" + id
        + "\" in \"" + persistor.getClass().getSimpleName() + "\" is null";
        m_state = persistor.getState();
        m_jobManager = persistor.getExecutionJobManager();
        m_customDescription = persistor.getCustomDescription();
        m_customName = persistor.getCustomName();
        m_uiInformation = persistor.getUIInfo();
        m_isDeletable = persistor.isDeletable();
        setNodeMessage(persistor.getNodeMessage());
        m_nodeContainerDirectory = persistor.getNodeContainerDirectory();
    }

    /**
     * @return parent workflowmanager holding this node (or null if root).
     */
    public final WorkflowManager getParent() {
        return m_parent;
    }

    /* ----------------- Job Manager ------------------ */

    /**
     * Set a new NodeExecutionJobManager for this node. This also includes
     * all child nodes unless they have their own dedicated job manager.
     * @param je the new job manager.
     */
    void setJobManager(final NodeExecutionJobManager je) {
        synchronized (m_nodeMutex) {
            if (m_parent == null && je == null) {
                throw new NullPointerException(
                "Root workflow manager must have a job manager.");
            }
            if (je != m_jobManager) {
                if (m_jobManager != null) {
                    m_jobManager.closeAllViews();
                }
                m_jobManager = je;
                notifyJobManagerChangedListener();
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

    /**
     * @return NodeExecutionJobManager
     * responsible for this node and all its children.
     */
    public final NodeExecutionJobManager findJobManager() {
        if (m_jobManager == null) {
            assert m_parent != null : "Root has no associated job manager";
            return m_parent.findJobManager();
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

    public boolean addJobManagerChangedListener(
            final JobManagerChangedListener l) {
        return m_jobManagerListeners.add(l);
    }

    public boolean removeJobManagerChangedListener(
            final JobManagerChangedListener l) {
        return m_jobManagerListeners.remove(l);
    }

    protected void notifyJobManagerChangedListener() {
        JobManagerChangedEvent e = new JobManagerChangedEvent(getID());
        for (JobManagerChangedListener l : m_jobManagerListeners) {
            l.jobManagerChanged(e);
        }
    }

    /////////////////////////////////////////////////
    // Convenience functions for all derived classes
    /////////////////////////////////////////////////

    /**
     * @return true of this node (or all nodes in this container) are
     *   resetable.
     */
    abstract boolean isResetable();

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
     * Change state of marked (for execution) node to queued once it has been
     * assigned to a NodeExecutionJobManager.
     *
     * @param inData the incoming data for the execution
     * @throws IllegalStateException in case of illegal entry state.
     */
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
                    NodeExecutionJob job = jobManager.submitJob(this, inData);
                    setExecutionJob(job);
                } catch (Throwable t) {
                    String error = "Failed to submit job to job executor \""
                        + jobManager + "\": " + t.getMessage();
                    setNodeMessage(new NodeMessage(
                            NodeMessage.Type.ERROR, error));
                    LOGGER.error(error, t);
                    notifyParentPreExecuteStart();
                    try {
                        notifyParentExecuteStart();
                    } catch (IllegalContextStackObjectException e) {
                        // ignore, we have something more serious to deal with
                    }
                    notifyParentPostExecuteStart();
                    notifyParentExecuteFinished(
                            NodeContainerExecutionStatus.FAILURE);
                }
                return;
            default:
                throwIllegalStateException();
            }
        }
    }

    /** Marks this node as remotely executing. This is necessary if the entire
     * (sub-) flow that this node is part of is executed remotely.
     * @throws IllegalStateException In case of an illegal state transition.
     */
    abstract void mimicRemoteExecuting();

    /** Puts this node (and all its children) into the {@link State#PREEXECUTE}
     * state. This method is used when a workflow is executed remotely.
     * @throws IllegalStateException In case of an illegal state (e.g. a node
     * is already executing).
     */
    abstract void mimicRemotePreExecute();

    /** Puts this node (and all its children) into the {@link State#POSTEXECUTE}
     * state. This method is used when a workflow is executed remotely.
     * @throws IllegalStateException In case of an illegal state.
     */
    abstract void mimicRemotePostExecute();

    /** Put this node into either the {@link State#EXECUTED} or
     * {@link State#IDLE} state depending on the argument. This method is
     * applied recursively on all of this node's children (if a meta node).
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
    void continueExecutionOnLoad(final PortObject[] inData,
            final NodeSettingsRO settings)
        throws InvalidSettingsException, NodeExecutionJobReconnectException {
        synchronized (m_nodeMutex) {
            switch (getState()) {
            case EXECUTINGREMOTELY:
                NodeExecutionJobManager jobManager = findJobManager();
                try {
                    NodeExecutionJob job = jobManager.loadFromReconnectSettings(
                            settings, inData, this);
                    setExecutionJob(job);
//                    setState(State.EXECUTINGREMOTELY, false);
                } catch (NodeExecutionJobReconnectException t) {
                    throw t;
                } catch (InvalidSettingsException t) {
                    throw t;
                } catch (Throwable t) {
                    throw new InvalidSettingsException(
                            "Failed to continue job on job manager \""
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
    abstract void performShutdown();

    /** Cancel execution of a marked, queued, or executing node. (Tolerate
     * execute as this may happen throughout cancelation).
     * @throws IllegalStateException
     */
    abstract void cancelExecution();

    void saveNodeExecutionJobReconnectInfo(final NodeSettingsWO settings) {
        assert getState().equals(State.EXECUTINGREMOTELY)
        : "Can't save node execution job, node is not executing "
            + "remotely but " + getState();
        NodeExecutionJobManager jobManager = findJobManager();
        NodeExecutionJob job = getExecutionJob();
        assert jobManager.canDisconnect(job)
        : "Execution job can be saved/disconnected";
        jobManager.saveReconnectSettings(job, settings);
        job.setSavedForDisconnect(true);
    }

    /**
     * Invoked by job manager when the execution starts. This method will invoke
     * the {@link WorkflowManager#doBeforePreExecution(NodeContainer)} method in
     * this node's parent. It will then call back on
     * {@link #performStateTransitionPREEXECUTE()} to allow for a synchronized
     * state transition.
     */
    void notifyParentPreExecuteStart() {
        getParent().doBeforePreExecution(this);
    }

    /**
     * Invoked by the job executor immediately before the execution is
     * triggered. It invokes doBeforeExecution on the parent.
     *
     * @throws IllegalContextStackObjectException in case of wrongly connected
     *             loops, for instance.
     */
    void notifyParentExecuteStart() {
        // this will allow the parent to call state changes etc properly
        // synchronized. The main execution is done asynchronously.
        try {
            getParent().doBeforeExecution(this);
        } catch (IllegalContextStackObjectException e) {
            LOGGER.warn(e.getMessage(), e);
            setNodeMessage(new NodeMessage(
                    NodeMessage.Type.ERROR, e.getMessage()));
            throw e;
        }
    }

    /**
     * Invoked by job manager when the execution is finishing. This method will
     * invoke the {@link WorkflowManager#doBeforePostExecution(NodeContainer)}
     * method in this node's parent. It will then call back on
     * {@link #performStateTransitionPOSTEXECUTE()} to allow for a synchronized
     * state transition.
     */
    void notifyParentPostExecuteStart() {
        getParent().doBeforePostExecution(this);
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

    /** Called when the state of a node should switch from
     * {@link State#QUEUED} to {@link State#PREEXECUTE}. The method is to be
     * called from the node's parent in a synchronized environment. */
    abstract void performStateTransitionPREEXECUTE();

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
    abstract void performStateTransitionEXECUTED(
            final NodeContainerExecutionStatus status);

    /////////////////////////////////////////////////
    // List Management of Waiting Loop Head Nodes
    /////////////////////////////////////////////////

    /** add a loop to the list of waiting loops.
     *
     * @param slc ScopeObject of the loop.
     */
    public void addWaitingLoop(final ScopeLoopContext slc) {
        if (!m_listOfWaitingLoops.contains(slc)) {
            m_listOfWaitingLoops.add(slc);
        }
    }

    /**
     * @return a list of waiting loops (well: their ScopeObjects)
     */
    public List<ScopeLoopContext> getWaitingLoops() {
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
    public void removeWaitingLoopHeadNode(final ScopeObject so) {
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
        NodeProgressEvent event =
                new NodeProgressEvent(getID(), pe.getNodeProgress());
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
           throw new NullPointerException(
                   "Node progress listener must not be null");
       }
       return m_progressListeners.add(listener);
   }


   /**
    *
    * @param listener existing listener to the node progress
    * @return true if the listener was successfully removed, false if it was
    *         not registered
    */
   public boolean removeNodeProgressListener(
           final NodeProgressListener listener) {
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
           throw new NullPointerException(
                   "Node message listner must not be null!");
       }
       return m_messageListeners.add(listener);
   }

   /**
    *
    * @param listener listener to the node messages
    * @return true if the listener was successfully removed, false if it was not
    *         registered
    */
   public boolean removeNodeMessageListener(
           final NodeMessageListener listener) {
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
           throw new NullPointerException(
                   "NodeUIInformationListener must not be null!");
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
   public UIInformation getUIInformation() {
           return m_uiInformation;
   }

   /**
    *
    * @param uiInformation new user interface information of the node such as
    *   coordinates on workbench and custom name.
    */
   public void setUIInformation(final UIInformation uiInformation) {
       // ui info is a property of the outer workflow (it just happened
       // to be a field member of this class)
       // there is no reason on settings the dirty flag when changed.
       m_uiInformation = uiInformation;
       notifyUIListeners(new NodeUIInformationEvent(m_id, m_uiInformation,
               m_customName, m_customDescription));
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

    /**
    *
    * @param listener listener to the node's state
    * @return true if the listener was not already registered, false otherwise
    */
   public boolean addNodeStateChangeListener(
           final NodeStateChangeListener listener) {
       if (listener == null) {
           throw new NullPointerException(
                   "Node state change listener must not be null!");
       }
       return m_stateChangeListeners.add(listener);
   }

   /**
    *
    * @param listener listener to the node's state.
    * @return true if the listener was successfully removed, false if the
    *         listener was not registered
    */
   public boolean removeNodeStateChangeListener(
           final NodeStateChangeListener listener) {
       return m_stateChangeListeners.remove(listener);
   }

    /**
     * @return the status of this node
     */
    public State getState() {
        return m_state;
    }

    /** Set new status and notify listeners.
     * @param state the new state
     */
    protected void setState(final State state) {
        setState(state, true);
    }

    /** Set new status and notify listeners.
     * @param state the new state
     * @param setDirty whether to set this node &quot;dirty&quot; (needs save).
     * @return true if change was changed.
     */
    protected boolean setState(final State state, final boolean setDirty) {
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
        }
        LOGGER.debug(this.getNameWithID() + " has new state: " + m_state);
        return changesMade;
    }

    /** Throws a new IllegalStateException with a meaningful error message
     * containing node name, current state and method name. This method is used
     * from the different state transition methods in
     * {@link SingleNodeContainer} and {@link WorkflowManager}.
     */
    protected void throwIllegalStateException() {
        String name = getNameWithID();
        String state = "\"" + getState().toString() + "\"";
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

    /** Return a NodeDialogPane for a node which can be embedded into
     * a JFrame oder another GUI element.
     *
     * @return A dialog pane for the corresponding node.
     * @throws NotConfigurableException if node can not be configured
     */
    public NodeDialogPane getDialogPaneWithSettings()
        throws NotConfigurableException {
        if (!hasDialog()) {
            throw new IllegalStateException(
                    "Node \"" + getName() + "\" has no dialog");
        }
        PortObjectSpec[] inputSpecs = new PortObjectSpec[getNrInPorts()];
        m_parent.assembleInputSpecs(getID(), inputSpecs);
        return getDialogPaneWithSettings(inputSpecs);
    }

    /** Launch a node dialog in its own JFrame (a JDialog).
     *
     * @throws NotConfigurableException if node can not be configured
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
        if (!hasDialog()) {
            throw new IllegalStateException(
                    "Node \"" + getName() + "\" has no dialog");
        }
        // TODO do we need to reset the node first??
        NodeSettings sett = new NodeSettings("node settings");
        getDialogPane().finishEditingAndSaveSettingsTo(sett);
        m_parent.loadNodeSettings(getID(), sett);
    }

    public boolean areDialogSettingsValid() {
        if (!hasDialog()) {
            throw new IllegalStateException(
                    "Node \"" + getName() + "\" has no dialog");
        }
        NodeSettings sett = new NodeSettings("node settings");
        try {
            getDialogPane().finishEditingAndSaveSettingsTo(sett);
            return areSettingsValid(sett);
        } catch (InvalidSettingsException nce) {
            return false;
        }
    }

    /* --------------- Dialog handling --------------- */

    public abstract boolean hasDialog();

    abstract NodeDialogPane getDialogPaneWithSettings(final PortObjectSpec[] inSpecs)
            throws NotConfigurableException;

    abstract NodeDialogPane getDialogPane();

    public abstract boolean areDialogAndNodeSettingsEqual();

    void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
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
        ncSet.load(settings);
        // the job manager instance will be the same, if settings permit
        setJobManager(ncSet.getJobManager());
        setDirty();
    }

    void saveSettings(final NodeSettingsWO settings) {
        NodeContainerSettings ncSet = new NodeContainerSettings();
        ncSet.setJobManager(m_jobManager);
        ncSet.save(settings);
    }

    boolean areSettingsValid(final NodeSettingsRO settings) {
        try {
            new NodeContainerSettings().load(settings);
        } catch (InvalidSettingsException ise) {
            return false;
        }
        return true;
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

    public NodeView<NodeModel> getView(final int i) {
        if (i < getNrNodeViews()) {
            return getNodeView(i);
        } else {
            assert getJobManager() != null : "Job Manager changed: No view!!";
            return getJobManager().getView(this);
        }
    }

    /**
     * Return the view with the specified index provided by the node.
     *
     * @param i the view to create
     * @return a new view instance with index i provided by the node
     */
    public abstract NodeView<NodeModel> getNodeView(final int i);

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

    /* ------------- Misc node info -------------- */

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
        return getNameWithID() + " (" + getState() + ")";
    }

    /**
     * @return the display label for {@link NodeView}, {@link OutPortView} and
     * {@link NodeDialog}
     */
    public String getDisplayLabel() {
        String label = getID().getIDWithoutRoot() + " - "
            + getName();
        String customName = getCustomName();
        if (customName != null && customName.trim().length() > 0) {
            label += " (" + customName + ")";
        }
        return label;
    }

    public String getCustomName() {
        return m_customName;
    }

    public void setCustomName(final String customName) {
        boolean notify = false;
        synchronized (m_nodeMutex) {
            if (!ConvenienceMethods.areEqual(customName, m_customName)) {
                m_customName = customName;
                setDirty();
                notify = true;
            }
        }
        if (notify) {
            notifyUIListeners(new NodeUIInformationEvent(m_id, m_uiInformation,
                    m_customName, m_customDescription));
        }
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
                    m_customName, m_customDescription));
        }
    }

    /** Is this node a to be locally executed workflow. In contrast to remotely
     * executed workflows, the nodes in the encapsulated workflow will be
     * executed independently (each represented by an own job), whereas remote
     * execution means that the entire workflow execution is one single job.
     * <p>This method returns false for all single node container.
     * @return The above described property.
     */
    protected abstract boolean isLocalWFM();

    /**
     * @return the isDeletable
     */
    public boolean isDeletable() {
        return m_isDeletable;
    }

    /** Method that's called when the node is discarded. The single node
     * container overwrites this method and cleans the outport data of the
     * node (deletes temp files).
     */
    void cleanup() {
        closeAllJobManagerViews();
    }

    /**
     * @return the isDirty
     */
    public final boolean isDirty() {
        return m_isDirty;
    }

    /**
     * Mark this node container to be changed, that is, it needs to be saved.
     */
    public void setDirty() {
        if (!m_isDirty) {
            LOGGER.debug("Setting dirty flag on " + getNameWithID());
        }
        m_isDirty = true;
        if (m_parent != null) {
            m_parent.setDirty();
        }
    }

    /** Called from persistor when node has been saved. */
    void unsetDirty() {
        m_isDirty = false;
    }

    /** Get a new persistor that is used to copy this node (copy& paste action).
     * @param tableRep Table repository of the destination.
     * @param preserveDeletableFlags Whether the "isdeleteable" annotation
     * should be copied also (false when individual nodes are copied
     * but true when an entire meta node is copied).
     * @return A new persistor for copying. */
    protected abstract NodeContainerPersistor getCopyPersistor(
            final HashMap<Integer, ContainerTable> tableRep,
            final boolean preserveDeletableFlags);

    /**
     * @param directory the nodeContainerDirectory to set
     */
    protected final void setNodeContainerDirectory(
            final ReferencedFile directory) {
        if (directory == null || !directory.getFile().isDirectory()) {
            throw new IllegalArgumentException("Not a directory: " + directory);
        }
        m_nodeContainerDirectory = directory;
    }

    /**
     * @return the nodeContainerDirectory
     */
    protected final ReferencedFile getNodeContainerDirectory() {
        return m_nodeContainerDirectory;
    }

    /** Restore content from persistor. This represents the second step
     * when loading a workflow.
     * @param persistor To load from.
     * @param tblRep A table repository to restore BufferedDatTables
     * @param inStack Incoming scope object stack.
     * @param exec For progress
     * @param loadResult Where to report errors/warnings to
     * @param preserveNodeMessage Whether possible node messages in the 
     *        persistor are to be preserved (parameter to configure method
     *        that is called during load).
     * @throws CanceledExecutionException If canceled.
     */
    abstract void loadContent(final NodeContainerPersistor persistor,
            final Map<Integer, BufferedDataTable> tblRep,
            final ScopeObjectStack inStack, final ExecutionMonitor exec,
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
     * into a true KNIME instance (upon which {@link #loadExecutionResult(
     * NodeContainerExecutionResult, ExecutionMonitor, LoadResult) is called).
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
        result.setSuccess(getState().equals(State.EXECUTED));
        result.setMessage(m_nodeMessage);
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

}
