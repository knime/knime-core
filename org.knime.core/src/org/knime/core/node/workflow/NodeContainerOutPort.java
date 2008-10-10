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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Mar 20, 2008 (mb): created from NodeOutPort (now an interface)
 */
package org.knime.core.node.workflow;

import java.util.HashSet;
import java.util.Set;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.NodeContainer.State;

/**
 * The implementation of an OutPort of a SingleNodeContainer - e.g. a "real"
 * node.
 * 
 * @author M. Berthold, University of Konstanz
 */
public class NodeContainerOutPort extends NodePortAdaptor
    implements NodeOutPort {
    
    private final Set<NodeStateChangeListener>m_listener;

    /**
     * The underlying Node.
     */
    private final SingleNodeContainer m_snc;
    
    /**
     * The inspector view of this port. Could be null if not opened yet.
     */
    private OutPortView m_portView;

    /**
     * Creates a new output port with a fixed and ID (should unique to all other
     * output ports of this node) for the given node.
     *
     * @param snc the underlying SingleNodeContainer.
     * @param portIndex the (output) port index.
     */
    public NodeContainerOutPort(final SingleNodeContainer snc,
            final int portIndex) {
        super(portIndex, snc.getNode().getOutputType(portIndex));
        m_snc = snc;
        m_portView = null;
        // TODO register this object as listener to spec/object... changes
        //   with Node!!
        m_snc.addNodeStateChangeListener(this);
        m_listener = new HashSet<NodeStateChangeListener>();
    }

    /**
     * {@inheritDoc}
     */
    public PortObjectSpec getPortObjectSpec() {
        return m_snc.getNode().getOutputSpec(getPortIndex());
    }

    /**
     * {@inheritDoc}
     */
    public PortObject getPortObject() {
        // the following test allows SingleNodeContainers/WFMs to hide
        // the PortObjects after a Node.execute() until the state of the
        // SNC/WFM has been adjusted to "EXECUTED"
        return m_snc.getState().equals(State.EXECUTED)
                      ? m_snc.getNode().getOutputObject(getPortIndex()) : null;
    }
    
    /** {@inheritDoc} */
    @Override
    public String getPortSummary() {
        return m_snc.getState().equals(State.EXECUTED)
            ? m_snc.getNode().getOutputObjectSummary(getPortIndex()) : null;
    }
    
    /**
     * {@inheritDoc}
     */
    public State getNodeState() {
        return m_snc.getState();
    }
    
    /**
     * {@inheritDoc}
     */
    public HiLiteHandler getHiLiteHandler() {
        return m_snc.getNode().getOutputHiLiteHandler(getPortIndex());
    }

    /**
     * {@inheritDoc}
     */
    public ScopeObjectStack getScopeContextStackContainer() {
        return m_snc.getNode().getScopeContextStackContainer();
    }

    /**
     * Sets a port view for this port. The port view can only be set once.
     *
     * @param portView The port view to set.
     * @throws NullPointerException If the port view is null.
     * @throws IllegalStateException If the port view was already set.
     * @see #getPortView()
     */
    private final void setPortView(final OutPortView portView) {
        if (portView == null) {
            throw new NullPointerException("Can't set port view to null");
        }
        if (m_portView != null) {
            throw new IllegalStateException("Port View can only be set once.");
        }
        m_portView = portView;
    }

    /**
     * Returns the port view for this output port which can be null.
     *
     * @return The port view or null.
     */
    protected final OutPortView getPortView() {
        return m_portView;
    }

    /**
     * {@inheritDoc}
     */
    // TODO: return component with convenience method for Frame construction.
    public void openPortView(final String name) {
        if (m_portView == null) {
            setPortView(new OutPortView(m_snc.getName() + " #" 
                    + m_snc.getID().getIndex(), getPortName()));
        }
        m_portView.openView();
        m_portView.update(getPortObject(), getPortObjectSpec());
    }

    /** {@inheritDoc} */
    @Override
    public void disposePortView() {
        if (m_portView == null) {
            return;
        }
        Runnable run = new Runnable() {
            /** {@inheritDoc} */
            @Override
            public void run() {
                if (m_portView != null) {
                    m_portView.setVisible(false);
                    m_portView.dispose();
                    m_portView = null;
                }
            }
        };
        ViewUtils.runOrInvokeLaterInEDT(run);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void finalize() throws Throwable {
        disposePortView();
        super.finalize();
    }

    ///////////////////////////////////////////////
    ///         State Listener methods
    //////////////////////////////////////////////
    
    /**
     * 
     * {@inheritDoc}
     */
    public boolean addNodeStateChangeListener(
            final NodeStateChangeListener listener) {
        return m_listener.add(listener);
    }

    /**
     * 
     * {@inheritDoc}
     */
    public void notifyNodeStateChangeListener(final NodeStateEvent e) {
        for (NodeStateChangeListener l : m_listener) {
            l.stateChanged(e);
        }
    }

    /**
     * 
     * {@inheritDoc}
     */
    public boolean removeNodeStateChangeListener(
            final NodeStateChangeListener listener) {
        return m_listener.remove(listener);
    }

    /**
     * 
     * {@inheritDoc}
     */
    public void stateChanged(final NodeStateEvent state) {
        if (state.getState().equals(NodeContainer.State.IDLE)
                || state.getState().equals(NodeContainer.State.CONFIGURED)
                || state.getState().equals(NodeContainer.State.EXECUTED)) {
            notifyNodeStateChangeListener(state);
            if (m_portView != null) {
                try {
                    m_portView.update(getPortObject(), getPortObjectSpec());
                } catch (Exception e) {
                    NodeLogger.getLogger(getClass()).error(
                            "Failed to update port view.", e);
                }
            }
        }
        
    }

}
