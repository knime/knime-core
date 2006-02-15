/*
 * @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * --------------------------------------------------------------------- *
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
 */
package de.unikn.knime.core.node;

/**
 * This class implements a node's input port which is either connected or not.
 * Internally it keeps a reference to its assigned <code>NodeOutPort</code> if
 * available which is asked for data input (<code>DataTable</code>),
 * <code>DataTableSpec</code>, and <code>HiLiteHandler</code>.
 * 
 * @author Thomas Gabriel, University of Konstanz
 * 
 * @see NodeOutPort
 */
public abstract class NodeInPort extends NodePort {

    /**
     * The node this port is input for. Needs to be notified of (dis)connect
     * actions and new data available at the connected counter part (outport).
     */
    private final Node m_node;

    /**
     * The output port of the predecessor which is connected to this port, can
     * be <code>null</code>, if no connection exists.
     */
    private NodeOutPort m_connPort;

    /**
     * Creates a new input port with the unique ID assigned from the node.
     * 
     * @param portId Unique ID.
     * @param node The port's node.
     * @throws NullPointerException If the node is null.
     */
    NodeInPort(final int portId, final Node node) {
        super(portId);
        if (node == null) {
            throw new NullPointerException();
        }
        m_node = node;
        m_connPort = null;
    }

    /**
     * Connects the given output port to this port, and returns any previously
     * connected port, (which could be <code>null</code> if no connection
     * existed before).
     * 
     * @param connPort The new connected output port.
     * @return The last outport connected with this one which can be
     *         <code>null</code> if this port was not connected before.
     * 
     * @throws IllegalArgumentException If this given port is not of the same
     *             type as this port.
     * @throws NullPointerException If the port to connect to is null.
     */
    public final NodeOutPort connectPort(final NodeOutPort connPort) {
        if (connPort == null) {
            throw new NullPointerException();
        }
        if (this instanceof NodePort.DataPort
                && !(connPort instanceof NodePort.DataPort)) {
            throw new IllegalArgumentException("Port types does not match.");
        }
        if (this instanceof NodePort.PredictorParamsPort
                && !(connPort instanceof NodePort.PredictorParamsPort)) {
            throw new IllegalArgumentException("Port types does not match.");
        }
        NodeOutPort tmp = m_connPort;
        m_connPort = connPort;
        m_connPort.addInPort(this);
        getNode().inportHasNewConnection(getPortID());
        return tmp;
    }

    /**
     * This port's underlying <code>Node</code>.
     * 
     * @return The node this port is input for.
     */
    final Node getNode() {
        return m_node;
    }

    /**
     * Diconnects this port. It will return the last connected port or null,
     * if not connected.
     * 
     * @return The last outport connected to this port which can be
     *         <code>null</code> if this port was not connected before.
     */
    public final NodeOutPort disconnectPort() {
        // see if we are connected
        if (m_connPort != null) {
            getNode().inportWasDisconnected(getPortID());
            m_connPort.removePort(this);
            NodeOutPort tmp = m_connPort;
            m_connPort = null;
            return tmp;
        } else {
            // nothing to do - we were not connected before
            return null;
        }
    }

    /**
     * Returns <code>true</code> if this port is connected with an output
     * port, otherwise <code>false</code>.
     * 
     * @return <code>true</code> if connected, otherwise <code>false</code>.
     */
    public final boolean isConnected() {
        return (m_connPort != null);
    }

    /**
     * Returns the output port connected with this port.
     * 
     * @return NodeOutPort or <code>null</code> if not connected.
     */
    public final NodeOutPort getConnectedPort() {
        return m_connPort;
    }

    /**
     * Invoked to reset the node of this input port.
     */
    final void resetNode() {
        getNode().inportResetsNode(getPortID());
    }

} // NodeInPort
