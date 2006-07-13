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
 *   17.01.2006(sieb, ohl): reviewed 
 */
package de.unikn.knime.core.node;

/**
 * Implements a node's input port. Internally it keeps a reference to its
 * connected <code>NodeOutPort</code> if available and to its node. The node
 * gets notified, whenever the connection changes.
 * 
 * @author Thomas Gabriel, University of Konstanz
 * 
 * @see NodeOutPort
 */
public abstract class NodeInPort extends NodePort {
    /**
     * The output port of the predecessor which is connected to this port, can
     * be <code>null</code>, if no connection exists.
     */
    private NodeOutPort m_connOutPort;

    /**
     * Creates a new input port with the ID assigned from the node.
     * 
     * @param portId the ID of this port.
     * @param node The port's node.
     * @throws NullPointerException If the node is null.
     */
    NodeInPort(final int portId, final Node node) {
        super(portId, node);
        if (node == null) {
            throw new NullPointerException("Ports can't belong to null nodes.");
        }
        m_connOutPort = null;
    }

    /**
     * Connects the given output port to this port, and returns any previously
     * connected port, (which could be <code>null</code> if no connection
     * existed before).
     * 
     * @param connPort The new output port to connect.
     * @return The last outport connected to this one which can be
     *         <code>null</code> if this port was not connected before.
     * 
     * @throws IllegalArgumentException If the given port is not of the same
     *             type as this port.
     * @throws NullPointerException If the port to connect to is null.
     */
    public final NodeOutPort connectPort(final NodeOutPort connPort) {
        if (connPort == null) {
            throw new NullPointerException(
                    "The port to connect can't be null.");
        }
        if (this instanceof NodePort.DataPort
                && !(connPort instanceof NodePort.DataPort)) {
            throw new IllegalArgumentException("Port types don't match.");
        }
        if (this instanceof NodePort.ModelContentPort
                && !(connPort instanceof NodePort.ModelContentPort)) {
            throw new IllegalArgumentException("Port types don't match.");
        }
        NodeOutPort tmp = m_connOutPort;
        m_connOutPort = connPort;
        m_connOutPort.addInPort(this);
        getNode().inportHasNewConnection(getPortID());
        return tmp;
    }

    /**
     * Diconnects this port. It will return the currently connected port, or
     * <code>null</code>, if not connected.
     * 
     * @return The currently connected port which can be <code>null</code> if
     *         this port is not connected.
     */
    public final NodeOutPort disconnectPort() {
        // see if we are connected
        if (m_connOutPort != null) {            
            m_connOutPort.removePort(this);
            NodeOutPort tmp = m_connOutPort;
            m_connOutPort = null;
            getNode().inportWasDisconnected(getPortID());
            return tmp;
        } else {
            // nothing to do - we are not connected
            return null;
        }
    }

    /**
     * Returns <code>true</code> if this port is connected to an output
     * port, otherwise <code>false</code>.
     * 
     * @return <code>true</code> if connected, otherwise <code>false</code>.
     */
    @Override
    public final boolean isConnected() {
        return (m_connOutPort != null);
    }

    /**
     * Returns the output port connected to this port.
     * 
     * @return NodeOutPort or <code>null</code> if not connected.
     */
    public final NodeOutPort getConnectedPort() {
        return m_connOutPort;
    }
} // NodeInPort
