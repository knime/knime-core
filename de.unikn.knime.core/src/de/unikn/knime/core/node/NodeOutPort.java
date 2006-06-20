/* --------------------------------------------------------------------- *
 *   This source code, its documentation and all appendant files         *
 *   are protected by copyright law. All rights reserved.                *
 *                                                                       *
 *   Copyright, 2003 - 2006                                              *
 *   Universitaet Konstanz, Germany.                                     *
 *   Lehrstuhl fuer Angewandte Informatik                                *
 *   Prof. Dr. Michael R. Berthold                                       *
 *                                                                       *
 *   You may not modify, publish, transmit, transfer or sell, reproduce, *
 *   create derivative works from, distribute, perform, display, or in   *
 *   any way exploit any of the content, in whole or in part, except as  *
 *   otherwise expressly permitted in writing by the copyright owner.    *
 * --------------------------------------------------------------------- *
 * 
 * History
 *   04.05.2006(sieb, ohl): reviewed 
 */
package de.unikn.knime.core.node;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Class implements a node's output port. A variable number of input ports can
 * be connected to it (which are part of the next nodes in the workflow).
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class NodeOutPort extends NodePort {
    /**
     * Keeps all connected input ports of the next nodes.
     */
    private final Set<NodeInPort> m_connInPorts;

    /**
     * The inspector view of this port. Could be null if not opened yet.
     */
    private NodeOutPortView m_portView;

    /**
     * Creates a new output port with a fixed and ID (should unique to all other
     * output ports of this node) for the given node.
     * 
     * @param portID This port ID.
     * @param node the node this port belongs to
     */
    protected NodeOutPort(final int portID, final Node node) {
        super(portID, node);
        m_connInPorts = Collections
                .synchronizedSet(new LinkedHashSet<NodeInPort>());
        m_portView = null;
    }

    /**
     * Sets a port view for this port. The port view can only be set once.
     * 
     * @param portView The port view to set.
     * @throws NullPointerException If the port view is null.
     * @throws IllegalStateException If the port view was already set.
     * @see #getPortView()
     */
    protected final void setPortView(final NodeOutPortView portView) {
        if (portView == null) {
            throw new NullPointerException("Can't set port view to null");
        }
        if (m_portView != null) {
            throw new IllegalStateException("Port View can only set once.");
        }
        m_portView = portView;
    }

    /**
     * Returns the port view for this output port which can be null.
     * 
     * @return The port view or null.
     * @see #setPortView(NodeOutPortView)
     */
    protected final NodeOutPortView getPortView() {
        return m_portView;
    }

    /**
     * Return an unmodifiable set of connected <code>NodeInPort</code>
     * objects. These inports belong to successor nodes this port is connected
     * to.
     * 
     * @return Set of connected input ports.
     */
    final Set<NodeInPort> getConnectedInPorts() {
        return Collections.unmodifiableSet(m_connInPorts);
    }

    /**
     * Returns <b>true</b> if at least one connection to another inport has
     * been established.
     * 
     * @return If a connection is available.
     */
    @Override
    public boolean isConnected() {
        return (m_connInPorts.size() > 0);
    }

    /**
     * Adds the given input port to the set of connected ports. Do not
     * use this to connect nodes. Rather call the NodeInPort.connectPort method.
     * 
     * @param connInPort A new input port to add.
     * 
     * @throws NullPointerException If the connected input port is null.
     * @throws IllegalArgumentException If this given port is not of the same
     *             type as this port.
     * @see NodeInPort#connectPort(NodeOutPort)
     */
    final void addInPort(final NodeInPort connInPort) {
        if (connInPort == null) {
            throw new NullPointerException("Can't add null to inports.");
        }
        if (this instanceof NodePort.DataPort
                && !(connInPort instanceof NodePort.DataPort)) {
            throw new IllegalArgumentException("Port types don't match.");
        }
        if (this instanceof NodePort.PredictorParamsPort
                && !(connInPort instanceof NodePort.PredictorParamsPort)) {
            throw new IllegalArgumentException("Port types don't match.");
        }
        m_connInPorts.add(connInPort);
    }

    /**
     * Removes the given input port from the set of connected ports.
     * 
     * @param connInPort Input port to remove.
     * @throws NullPointerException If the connected input port is null.
     * @throws IllegalArgumentException If this given port is not of the same
     *             type as this port.
     * 
     */
    final void removePort(final NodeInPort connInPort) {
        if (connInPort == null) {
            throw new NullPointerException("Can't remove null from inports.");
        }
        if (this instanceof NodePort.DataPort
                && !(connInPort instanceof NodePort.DataPort)) {
            throw new IllegalArgumentException("Port types don't match.");
        }
        if (this instanceof NodePort.PredictorParamsPort
                && !(connInPort instanceof NodePort.PredictorParamsPort)) {
            throw new IllegalArgumentException("Port types don't match.");
        }
        m_connInPorts.remove(connInPort);
    }

    /**
     * Removes all connected input ports from the set of connected ports.
     */
    final void removeAllPorts() {
        // avoid concurrent modification exception
        // (disconnectPort removed port form m_connInPorts)
        NodeInPort[] clone = m_connInPorts.toArray(new NodeInPort[0]);
        for (int i = clone.length - 1; i >= 0; --i) {
            NodeInPort inPort = clone[i];
            inPort.disconnectPort();
        }
    }

    /**
     * Opens the port view for this port with the given name.
     * 
     * @param name The name of the port view.
     */
    public abstract void openPortView(final String name);

    /**
     * Call this when the port is not used anymore, certainly when you've opened
     * a view before. All port views will be closed and disposed.
     */
    protected void disposePortView() {
        if (m_portView != null) {
            m_portView.setVisible(false);
            m_portView.dispose();
            m_portView = null;
        }
    }

    /**
     * TODO (tg) This method is not guaranteed to be called!
     * 
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        // make sure to blow away the port view
        disposePortView();
        super.finalize();
    }

} // NodeOutPort
