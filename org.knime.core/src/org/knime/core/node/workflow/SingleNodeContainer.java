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
 *   14.03.2007 (mb): created
 */
package org.knime.core.node.workflow;

import java.net.URL;

import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.GenericNodeDialogPane;
import org.knime.core.node.GenericNodeModel;
import org.knime.core.node.GenericNodeView;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeInPort;
import org.knime.core.node.NodeOutPort;
import org.knime.core.node.NodeProgressMonitor;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.PortObject;
import org.knime.core.node.PortObjectSpec;
import org.knime.core.node.GenericNodeFactory.NodeType;
import org.w3c.dom.Element;

/**
 * Holds a node in addition to some status information.
 *
 * @author M. Berthold/B. Wiswedel, University of Konstanz
 */
public final class SingleNodeContainer extends NodeContainer
    implements NodeMessageListener, NodeProgressListener {

    private final Node m_node;

    private final NodeProgressMonitor m_progressMonitor =
            new DefaultNodeProgressMonitor(this);

    /**
     * Create new NodeContainer based on existing Node.
     *
     * @param parent the workflowmanager holding this node
     * @param n the underlying node
     * @param id the unique identifier
     */
    SingleNodeContainer(final WorkflowManager parent, final Node n,
            final NodeID id) {
        super(parent, id);
        m_node = n;
        m_node.addMessageListener(this);
    }

    SingleNodeContainer(final WorkflowManager parent, final NodeID id,
            final SingleNodeContainerPersistor persistor) {
        super(parent, id, persistor.getMetaPersistor());
        m_node = persistor.getNode();
        assert m_node != null : persistor.getClass().getSimpleName()
            + " did not provide Node instance for " + getClass().getSimpleName()
            + " with id \"" + id + "\"";
        m_node.addMessageListener(this);

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
    public int getNrInPorts() {
        return m_node.getNrInPorts();
    }

    /** {@inheritDoc} */
    @Override
    public int getNrOutPorts() {
        return m_node.getNrOutPorts();
    }

    /** {@inheritDoc} */
    @Override
    public NodeOutPort getOutPort(final int i) {
        return m_node.getOutPort(i);
    }

    /** {@inheritDoc} */
    @Override
    public NodeInPort getInPort(final int i) {
        return m_node.getInPort(i);
    }

    /* ------------------ Views ---------------- */

    /** {@inheritDoc} */
    @Override
    public GenericNodeView<GenericNodeModel> getView(final int i) {
        String title = getNameWithID() + " (" + getViewName(i) + ")";
        if (getCustomName() != null) {
            title += " - " + getCustomName();
        }
        return (GenericNodeView<GenericNodeModel>)m_node.getView(i, title);
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

    /**
     * Set a new JobExecutor for this node but before check for valid state.
     *
     * @param je the new JobExecutor.
     */
    @Override
    public void setJobExecutor(final JobExecutor je) {
        if (getState().equals(State.EXECUTING)
                || getState().equals(State.QUEUED)) {
            throw new IllegalStateException("Illegal state " + getState()
                    + " in setJobExecutor - can not change a running node.");

        }
        super.setJobExecutor(je);
    }

    private ExecutionContext createExecutionContext() {
        return new ExecutionContext(m_progressMonitor, getNode(),
                getParent().getGlobalTableRepository());
    }

    // ////////////////////////////////
    // State transations
    // ////////////////////////////////

    /**
     * Configure underlying node and update state accordingly.
     *
     * @param inSpecs input table specifications
     * @return true if output specs have changed.
     * @throws IllegalStateException in case of illegal entry state.
     */
    @Override
    boolean configureNode(final PortObjectSpec[] inObjectSpecs)
            throws IllegalStateException {
        synchronized (m_dirtyNode) {
            // remember old specs
            PortObjectSpec[] prevSpecs =
                    new PortObjectSpec[getNrOutPorts()];
            for (int i = 0; i < prevSpecs.length; i++) {
                prevSpecs[i] = getOutPort(i).getPortObjectSpec();
            }
            // perform action
            switch (getState()) {
            case IDLE:
                if (m_node.configure(inObjectSpecs)) {
                    setNewState(State.CONFIGURED);
                } else {
                    setNewState(State.IDLE);
                }
                break;
            case UNCONFIGURED_MARKEDFOREXEC:
                if (m_node.configure(inObjectSpecs)) {
                    setNewState(State.MARKEDFOREXEC);
                } else {
                    setNewState(State.UNCONFIGURED_MARKEDFOREXEC);
                }
                break;
            case CONFIGURED:
                // m_node.reset();
                boolean success = m_node.configure(inObjectSpecs);
                if (success) {
                    setNewState(State.CONFIGURED);
                } else {
                    // m_node.reset();
                    setNewState(State.IDLE);
                }
                break;
            case MARKEDFOREXEC:
                // these are dangerous - otherwise re-queued loop-ends are
                // reset!
                // m_node.reset();
                success = m_node.configure(inObjectSpecs);
                if (success) {
                    setNewState(State.MARKEDFOREXEC);
                } else {
                    // m_node.reset();
                    setNewState(State.UNCONFIGURED_MARKEDFOREXEC);
                }
                break;
            default:
                throw new IllegalStateException("Illegal state " + getState()
                        + " encountered in configureNode(), node " + getID());
            }
            // compare old and new specs
            for (int i = 0; i < prevSpecs.length; i++) {
                PortObjectSpec newSpec =
                        getOutPort(i).getPortObjectSpec();
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

    /** {@inheritDoc} */
    @Override
    void enableQueuing() throws IllegalStateException {
        synchronized (m_dirtyNode) {
            switch (getState()) {
            case CONFIGURED:
                setNewState(State.MARKEDFOREXEC);
                return;
            case IDLE:
                setNewState(State.UNCONFIGURED_MARKEDFOREXEC);
                return;
            default:
                throw new IllegalStateException("Illegal state " + getState()
                        + " encountered in enableQueuing().");
            }
        }
    }

    /**
     * Queue underlying node for re-execution (= update state accordingly).
     *
     * @throws IllegalStateException in case of illegal entry state.
     */
    void enableReQueuing() throws IllegalStateException {
        synchronized (m_dirtyNode) {
            switch (getState()) {
            case EXECUTED:
                m_node.cleanOutPorts();
                setNewState(State.MARKEDFOREXEC);
                return;
            default:
                throw new IllegalStateException("Illegal state " + getState()
                        + " encountered in enableReQueuing().");
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    void resetNode() throws IllegalStateException {
        synchronized (m_dirtyNode) {
            switch (getState()) {
            case EXECUTED:
                m_node.reset();
                if (m_node.isConfigured()) {
                    setNewState(State.CONFIGURED);
                } else {
                    setNewState(State.IDLE);
                }
                return;
            case MARKEDFOREXEC:
                setNewState(State.CONFIGURED);
                return;
            case UNCONFIGURED_MARKEDFOREXEC:
                setNewState(State.IDLE);
                return;
            default:
                throw new IllegalStateException("Illegal state " + getState()
                        + " encountered in resetNode().");
            }
        }
    }

    /**
     * Change state of marked (for execution) node to queued once it has been
     * assigned to a JobExecutor.
     *
     * @throws IllegalStateException in case of illegal entry state.
     */
    @Override
    void queueNode(final PortObject[] inData) {
        synchronized (m_dirtyNode) {
            switch (getState()) {
            case MARKEDFOREXEC:
                setNewState(State.QUEUED);
                ExecutionContext execCon = createExecutionContext();
                findJobExecutor().submitJob(new JobRunnable(execCon) {
                    @Override
                    public void run(final ExecutionContext ec) {
                        executeNode(inData, ec);
                    }
                });
                return;
            default:
                throw new IllegalStateException("Illegal state " + getState()
                        + " encountered in queueNode().");
            }
        }
    }

    /* ------------- internal state change actions ------------ */

    /**
     * Execute underlying Node and update state accordingly.
     *
     * @param inTables input parameters
     * @throws IllegalStateException in case of illegal entry state.
     */
    private void executeNode(final PortObject[] inObjects,
            final ExecutionContext ec) throws IllegalStateException {
        getParent().doBeforeExecution(SingleNodeContainer.this);
        synchronized (m_dirtyNode) {
            switch (getState()) {
            case QUEUED:
                m_node.clearLoopStatus();
                setNewState(State.EXECUTING);
                // TODO: the progress monitor should not be accessible from the
                // public world.
                ec.getProgressMonitor().reset();
                boolean success = m_node.execute(inObjects, ec);
                if (success) {
                    if (m_node.getLoopStatus() == null) {
                        setNewState(State.EXECUTED);
                    } else {
                        // loop not yet done - "stay" configured until done.
                        setNewState(State.CONFIGURED);
                    }
                } else {
                    m_node.reset();
                    // TODO move this into Node.reset() ?
                    m_node.clearLoopStatus();
                    // TODO reconfigure node!
                    setNewState(State.CONFIGURED);
                }
                break;
            default:
                throw new IllegalStateException("Illegal state " + getState()
                        + " encountered in executeNode(), node: " + getID());
            }
        }
        // the following triggers checkforqueueablenodes
        getParent().doAfterExecution(SingleNodeContainer.this);
    }


    // //////////////////////////////////////
    // forwarding methods to underlying Node
    // //////////////////////////////////////

    @Override
    void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        synchronized (m_dirtyNode) {
            m_node.loadSettingsFrom(settings);
        }
    }
    
    void loadContent(final NodeContainerPersistor nodePersistor, final int loadID) {
        if (!(nodePersistor instanceof SingleNodeContainerPersistor)) {
            throw new IllegalStateException("Expected " 
                    + SingleNodeContainerPersistor.class.getSimpleName() 
                    + " persistor object, got " 
                    + nodePersistor.getClass().getSimpleName());
        }
        SingleNodeContainerPersistor persistor = 
            (SingleNodeContainerPersistor)nodePersistor;
        setNewState(persistor.getMetaPersistor().getState());
        setScopeObjectStack(new ScopeObjectStack(getID()));
    }

    @Override
    void saveSettings(final NodeSettingsWO settings)
    throws InvalidSettingsException {
        m_node.saveSettingsTo(settings);
    }

    @Override
    boolean areSettingsValid(final NodeSettingsRO settings) {
        return m_node.areSettingsValid(settings);
    }

    /** @return name of this node */
    @Override
    public String getName() {
        return m_node.getName();
    }

    /**
     * @see org.knime.core.node.NodeFactory#getNodeFullHTMLDescription
     */
    public String getFullHTMLDescription() {
        return m_node.getFullHTMLDescription();
    }

    /**
     * @see org.knime.core.node.NodeFactory#getNodeOneLineDescription
     */
    public String getOneLineDescription() {
        return m_node.getOneLineDescription();
    }


    void setScopeObjectStack(final ScopeObjectStack st) {
        synchronized (m_dirtyNode) {
            m_node.setScopeContextStackContainer(st);
        }
    }

    ScopeObjectStack getScopeObjectStack() {
        synchronized (m_dirtyNode) {
            return m_node.getScopeContextStackContainer();
        }
    }

    /**
     * @return Node name with status information.
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return m_node.getName() + "(" + getID() + ")" + ";status:" + getState();
    }

    /* ---------------- progress forwarding methods ------------------- */
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

    // dummy methods -----------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public boolean hasDialog() {
        return m_node.hasDialog();
    }

    /** {@inheritDoc} */
    @Override
    GenericNodeDialogPane getDialogPaneWithSettings(
            final PortObjectSpec[] inSpecs) throws NotConfigurableException {
        return m_node.getDialogPaneWithSettings(inSpecs);
    }

    /** {@inheritDoc} */
    @Override
    GenericNodeDialogPane getDialogPane() {
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
        synchronized (m_dirtyNode) {
            m_node.loadSettingsFromDialog();
        }
    }

    // --------------------------

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
        return null;
    }

    /**
     * @return the XML description of the node for the NodeDescription view
     */
    public Element getXMLDescription() {
        return m_node.getXMLDescription();
    }

}


