/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 *
 * History
 *   29.03.2007 (mb): created
 */
package org.knime.core.node.workflow;

import java.net.URL;
import java.util.concurrent.CopyOnWriteArraySet;

import org.knime.core.node.GenericNodeDialogPane;
import org.knime.core.node.GenericNodeModel;
import org.knime.core.node.GenericNodeView;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialog;
import org.knime.core.node.NodeInPort;
import org.knime.core.node.NodeOutPort;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.PortObject;
import org.knime.core.node.PortObjectSpec;
import org.knime.core.node.GenericNodeFactory.NodeType;

/**
 * Abstract super class for containers holding node or just structural
 * information of a meta node. Also stores additional (optional) information
 * such as coordinates on a workflow layout.
 *
 * @author M. Berthold/B. Wiswedel, University of Konstanz
 */
public abstract class NodeContainer {

    /** possible status values of a NodeContainer. */
    public static enum State {
        IDLE,
        CONFIGURED,
        UNCONFIGURED_MARKEDFOREXEC,
        MARKEDFOREXEC,
        QUEUED,
        EXECUTING,
        EXECUTED
    };

    private State m_state;

    private final NodeID m_id;

    private final WorkflowManager m_parent;

    private JobExecutor m_jobExecutor;

    private String m_customName;

    private String m_customDescription;

    /**
     * semaphore to make sure never try to work on inconsistent internal node
     * states. This semaphore will be used by a node alone to synchronize
     * internal changes of status etc.
     */
    final protected Object m_dirtyNode = new Object();

    /*--------- listener administration------------*/


    private final CopyOnWriteArraySet<NodeStateChangeListener> m_stateChangeListeners =
            new CopyOnWriteArraySet<NodeStateChangeListener>();

    private final CopyOnWriteArraySet<NodeMessageListener> m_messageListeners =
        new CopyOnWriteArraySet<NodeMessageListener>();

    private final CopyOnWriteArraySet<NodeProgressListener> m_progressListeners =
        new CopyOnWriteArraySet<NodeProgressListener>();

    private final CopyOnWriteArraySet<NodeUIInformationListener> m_uiListeners =
        new CopyOnWriteArraySet<NodeUIInformationListener>();

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
            // TODO: better default choice??
            m_jobExecutor = new ThreadedJobExecutor(16);
        }
        m_id = id;
        m_state = State.IDLE;
    }

    NodeContainer(final WorkflowManager parent, final NodeID id,
            final NodeContainerMetaPersistor persistor) {
        this(parent, id);
        assert persistor.getState() != null : "State of node \"" + id
        + "\" in \"" + persistor.getClass().getSimpleName() + "\" is null";
        m_state = persistor.getState();
        m_customDescription = persistor.getCustomDescription();
        m_customName = persistor.getCustomName();
        m_uiInformation = persistor.getUIInfo();
    }

    /**
     * @return parent workflowmanager holding this node (or null if root).
     */
    final WorkflowManager getParent() {
        return m_parent;
    }

    /**
     * Set a new JobExecutor for this node and all it's children.
     *
     * @param je the new JobExecutor.
     */
    public void setJobExecutor(final JobExecutor je) {
        if (je == null) {
            throw new NullPointerException("JobExecutor must not be null.");
        }
        m_jobExecutor = je;
    }

    /**
     * @return JobExecutor responsible for this node and all its children.
     */
    protected final JobExecutor findJobExecutor() {
        if (m_jobExecutor == null) {
            assert m_parent != null;
            return ((NodeContainer)m_parent).findJobExecutor();
        }
        return m_jobExecutor;
    }

    ///////////////////////////////
    // Listener administration
    ////////////////////////////////////


    /* ----------- progress ----------*/

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

   /** Get the message to be displayed to the user or null if nothing is set
    * currently.
    * @return the node message consisting of type and message */
   public abstract NodeMessage getNodeMessage();

   /**
    * Notifies all registered {@link NodeMessageListener}s about the new
    * message.
    *
    * @param e the new message event
    */
   protected void notifyMessageListeners(final NodeMessageEvent e) {
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
    * @param nodeUIInfo new user interface information of the node such as
    *   coordinates on workbench and custom name.
    */
   public void setUIInformation(final UIInformation nodeUIInfo) {
       m_uiInformation = nodeUIInfo;
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

    /**
     * Set new status and notify listeners.
     *
     * @param state the new state
     */
    protected void setNewState(final State state) {
        boolean changesMade = false;
        synchronized (m_dirtyNode) {
            if (m_state != state) {
                m_state = state;
                changesMade = true;
            }
        }
        if (changesMade) {
            // notify state listeners
            notifyStateChangeListeners(new NodeStateEvent(getID(), m_state));
        }
    }

    /* ---------- State changing actions ------------ */

    /** Configure underlying node.
     *
     * @param specs input port object specifications
     * @return true if configuration resulted in NEW output specs (meaning
     *   that successors should probably be configured as well)
     * @throws IllegalStateException in case of illegal entry state.
     */
    abstract boolean configureNode(final PortObjectSpec[] specs)
    throws IllegalStateException;

    /** Enable queuing of underlying node for execution. This really only
     * changes the state of the node and once all pre-conditions for
     * execution are fulfilled (e.g. configuration succeeded and all
     * ingoing objects are available) the node will be actually queued.
     *
     * @throws IllegalStateException in case of illegal entry state.
     */
    abstract void enableQueuing()
    throws IllegalStateException;

    /** Actually queue the node for execution together with ingoing data.
     * The outgoing data is held in the nodes {@link NodeOutPort}s.
     *
     * @param inData ingoing objects.
     * @throws IllegalStateException in case of illegal entry state.
     */
    abstract void queueNode(final PortObject[] inData)
    throws IllegalStateException;

    /** Reset underlying node and update state accordingly.
     * @throws IllegalStateException in case of illegal entry state.
     */
    abstract void resetNode()
    throws IllegalStateException;

    /* ------------ dialog -------------- */

    /** Return a NodeDialogPane for a node which can be embedded into
     * a JFrame oder another GUI element.
     *
     * @return A dialog pane for the corresponding node.
     * @throws NotConfigurableException if node can not be configured
     */
    public GenericNodeDialogPane getDialogPaneWithSettings()
        throws NotConfigurableException {
        if (!hasDialog()) {
            throw new IllegalStateException(
                    "Node \"" + getName() + "\" has no dialog");
        }
        PortObjectSpec[] inputSpecs = new PortObjectSpec[getNrInPorts()];
        m_parent.assembleInputSpecs(getID(), inputSpecs);
        return getDialogPaneWithSettings(inputSpecs);
    }

    /** Launch a node dialog in it's own JFrame (actually a JDialog).
     *
     * @param id node ID
     * @throws NotConfigurableException if node can not be configured
     */
    public void openDialogInJFrame(final NodeID id)
    throws NotConfigurableException {
        NodeDialog nd = new NodeDialog(
                getDialogPaneWithSettings(), m_parent, getID());
        nd.openDialog();
    }

    /** Take settings from the node's dialog and apply them to the model. Throws
     * an exception if the apply fails.
     *
     * @throws InvalidSettingsException if settings are not applicable.
     */
    public void applySettingsFromDialog()
    throws InvalidSettingsException {
        if (!hasDialog()) {
            throw new IllegalStateException(
                    "Node \"" + getName() + "\" has no dialog");
        }
        // TODO do we need to reset the node first??
        NodeSettings sett = new NodeSettings("node settings");
        getDialogPane().internalSaveSettingsTo(sett);
        m_parent.loadNodeSettings(getID(), sett);
    }

    public boolean areDialogSettingsValid() {
        if (!hasDialog()) {
            throw new IllegalStateException(
                    "Node \"" + getName() + "\" has no dialog");
        }
        NodeSettings sett = new NodeSettings("node settings");
        try {
            getDialogPane().internalSaveSettingsTo(sett);
            return areSettingsValid(sett);
        } catch (InvalidSettingsException nce) {
            return false;
        }
    }

    /* --------------- Dialog handling --------------- */

    public abstract boolean hasDialog();

    abstract GenericNodeDialogPane getDialogPaneWithSettings(final PortObjectSpec[] inSpecs)
            throws NotConfigurableException;

    abstract GenericNodeDialogPane getDialogPane();

    public abstract boolean areDialogAndNodeSettingsEqual();

    abstract void loadSettingsFromDialog() throws InvalidSettingsException;

    abstract void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException;

    abstract void saveSettings(final NodeSettingsWO settings)
    throws InvalidSettingsException;


    abstract boolean areSettingsValid(final NodeSettingsRO settings);


    /* ------------- ports --------------- */

    public abstract int getNrInPorts();

    public abstract NodeInPort getInPort(final int index);

    public abstract NodeOutPort getOutPort(final int index);

    public abstract int getNrOutPorts();

    /* -------------- views ---------------- */

    public abstract int getNrViews();

    public abstract String getViewName(final int i);

    public abstract GenericNodeView<GenericNodeModel> getView(final int i);



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

    public String getCustomName() {
        return m_customName;
    }

    public void setCustomName(final String customName) {
        m_customName = customName;
        notifyUIListeners(new NodeUIInformationEvent(m_id, m_uiInformation,
                m_customName, m_customDescription));
    }

    public String getCustomDescription() {
        return m_customDescription;
    }

    public void setCustomDescription(final String customDescription) {
        m_customDescription = customDescription;
        notifyUIListeners(new NodeUIInformationEvent(m_id, m_uiInformation,
                m_customName, m_customDescription));
    }
    
    abstract void loadContent(
            final NodeContainerPersistor persistor, final int loadID);



}
