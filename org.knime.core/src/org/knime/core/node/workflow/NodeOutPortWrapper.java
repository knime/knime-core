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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Apr 1, 2008 (mb/bw): created
 */
package org.knime.core.node.workflow;

import java.util.HashSet;
import java.util.Set;

import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * 
 * @author B. Wiswedel, M. Berthold, University of Konstanz
 */
public class NodeOutPortWrapper extends NodePortAdaptor implements NodeOutPort {
    
    private final Set<NodeStateChangeListener>m_listener;
    
    /** if connected, reference the outport this node is connected to.
     */
    private NodeOutPort m_underlyingPort;
    
    /**
     * Creates a new output port with a fixed type and index (should unique 
     * to all other output ports of this node) for the given node.
     * 
     * @param pType The port's type
     * @param portIndex This port index
     */
    NodeOutPortWrapper(final int portIndex, final PortType pType) {
        super(portIndex, pType);
        m_listener = new HashSet<NodeStateChangeListener>();
    }

    /**
     * Set a new underlying port - used when the connection inside this
     * workflow to this outgoing port changes. The argument is the new
     * NodeOutPort connected to the outgoing port of the WFM.
     * 
     * @param p new port
     */
    void setUnderlyingPort(final NodeOutPort p) {
        if (m_underlyingPort != null) {
            m_underlyingPort.removeNodeStateChangeListener(this);
        }
        m_underlyingPort = p;
        if (m_underlyingPort != null) {
            // this fixes bug #1499
            // we use some prefix here and do not simply copy the name since
            // the default (null) name is "Outport 0", for instance. If we then
            // connect a WFM to an outer WFM, whereby the inner WFM is not 
            // connected (i.e. its output would show "Output X"), we would end
            // up copying whatever the WFM decides (the port could show 
            // "Outport 3" although the port is "0"
            // besides all that it's reasonable to show that the port only
            // forwards data, it does not hold it.
            String prefix = "Connected to: ";
            String name = m_underlyingPort.getPortName();
            if (name != null && !name.startsWith(prefix)) {
                name = prefix + name;
            }
            setPortName(name);
            m_underlyingPort.addNodeStateChangeListener(this);
            // if not null -> query state and throw event
            notifyNodeStateChangeListener(new NodeStateEvent(
                    new NodeID(0), m_underlyingPort.getNodeState()));
        } else {
            setPortName(null);
            // if null: disconnected set state -> idle
            notifyNodeStateChangeListener(new NodeStateEvent(
                    new NodeID(0), NodeContainer.State.IDLE));
        }
    }

    /**
     * @return underlying NodeOutPort
     */
    NodeOutPort getUnderlyingPort() {
        return m_underlyingPort;
    }

    /**
     * {@inheritDoc}
     */
    public HiLiteHandler getHiLiteHandler() {
        if (m_underlyingPort == null) {
            return null;
        }
        return m_underlyingPort.getHiLiteHandler();
    }
    
    /**
     * {@inheritDoc}
     */
    public ScopeObjectStack getScopeContextStackContainer() {
        if (m_underlyingPort == null) {
            return null;
        }
        return m_underlyingPort.getScopeContextStackContainer();
    }
    
    /**
     * {@inheritDoc}
     */
    public PortObject getPortObject() {
        if (m_underlyingPort == null) {
            return null;
        }
        return m_underlyingPort.getPortObject();
    }
    
    /** {@inheritDoc} */
    @Override
    public String getPortSummary() {
        if (m_underlyingPort == null) {
            return null;
        }
        return m_underlyingPort.getPortSummary();
    }
    
    /**
     * {@inheritDoc}
     */
    public NodeContainer.State getNodeState() {
        if (m_underlyingPort == null) {
            /* 
             * Return the IDLE state if the underlying port is null.
             * This may happen during loading of meta nodes and
             * if the port is not connected. The port is anyway intersted in 
             * the state and it is displayed as "no spec/node data".
             * 
             * TODO: when necessary (e.g. if a "not connected" state for the 
             * port should be displayed an additional event type has to be 
             * implemented (NodeOutPort.State = {Not_CONNECTED, NO_SPEC_NO_DATA,
             * SPEC_AVAILABLE, DATA_AVAILABEL}. Then the SingleNodeContainer 
             * has to cvonvert between the NodeContainer.State and the 
             * NodeOutPort.State. The NodeContainer.State can then be moved to 
             * the SingleNodeContainer.
             * Meanwhile return IDLE. 
             */
            return NodeContainer.State.IDLE;
        }
        return m_underlyingPort.getNodeState();
    }
    
    /**
     * {@inheritDoc}
     */
    public PortObjectSpec getPortObjectSpec() {
        if (m_underlyingPort == null) {
            return null;
        }
        return m_underlyingPort.getPortObjectSpec();
    }


    /**
     * {@inheritDoc}
     */
    public void openPortView(final String name) {
        if (m_underlyingPort == null) {
            return;
        }
        m_underlyingPort.openPortView(name);
    }
    
    /** {@inheritDoc} */
    @Override
    public void disposePortView() {
        // it's task of the underlying port to dispose views.
    }
    
    ///////////////////////////
    // Equals/HashCode/ToString
    ///////////////////////////
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (m_underlyingPort == null) {
            return this == obj;
        }
        return m_underlyingPort.equals(obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        if (m_underlyingPort == null) {
            return System.identityHashCode(this);
        }
        return m_underlyingPort.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (m_underlyingPort == null) {
            return "<<not connected>>";
        }
        return m_underlyingPort.toString();
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
        }
    }

}
