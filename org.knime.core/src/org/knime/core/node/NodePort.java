/*
 * ------------------------------------------------------------------
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
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 * 
 * History
 *   02.05.2006(sieb, ohl): reviewed 
 */
package org.knime.core.node;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * Abstract node port implementation which keeps a unique id and a port name.
 * The inner classes can be used to distinguish between <code>DataPort</code>
 * and <code>ModelContentPort</code> objects.
 * 
 * @see NodeInPort
 * @see NodeOutPort
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class NodePort {
    /**
     * The node this port is input for. Needs to be notified of (dis)connect
     * actions and new data available at the connected counter part (outport).
     */
    private final Node m_node;

    /** This ports ID assigned from the underlying node. */
    private final int m_portID;

    /** The port name which can be used for displaying purposes. */
    private String m_portName;

    /**
     * Creates a new node port with an ID assigned from the underlying node. The
     * default port name is "Port [portID]" and can be changed via
     * <code>#setPortName(String)</code>.
     * 
     * @param portID the port's id, greater or equal zero
     * @param node the node this port belongs to
     * 
     * @see #setPortName(String)
     */
    NodePort(final int portID, final Node node) {
        assert (portID >= 0);
        m_portID = portID;
        m_node = node;
        setPortName(null);
    }

    /**
     * @return The port name.
     */
    public final String getPortName() {
        return m_portName;
    }

    /**
     * Sets a new name for this port. If null or an empty string is passed, the
     * default name will be generated: "Port [" + portID + "]".
     * 
     * @param portName The new name for this port. If null is passed, the
     *            default name will be generated.
     */
    final void setPortName(final String portName) {
        if (portName == null || portName.trim().length() == 0) {
            if (this instanceof NodeInPort) {
                m_portName = "Inport " + m_portID;
            } else {
                m_portName = "Outport " + m_portID;
            }
        } else {
            m_portName = portName.trim();
        }
    }

    /**
     * @return This port's id.
     */
    public final int getPortID() {
        return m_portID;
    }

    /**
     * Returns <code>true</code> if this port has a connection to another
     * port.
     * 
     * @return <code>true</code> If a connection exists otherwise
     *         <code>false</code>.
     */
    public abstract boolean isConnected();

    /**
     * Interface to identify <code>DataPort</code> objects wich can return
     * <code>DataTable</code>, <code>DataTableSpec</code>,
     * <code>HiLiteHandler</code> objects.
     */
    interface DataPort {
        /**
         * @return The node port's <code>DataTable</code>.
         */
        BufferedDataTable getBufferedDataTable();

        /**
         * @return The node port's <code>DataTableSpec</code>.
         */
        DataTableSpec getDataTableSpec();

        /**
         * @return The node port's <code>HiLiteHandler</code>.
         */
        HiLiteHandler getHiLiteHandler();
    }

    /**
     * Interface to identify <code>PredcitorParamsPort</code> objects which
     * returns <code>ModelContent</code> objects.
     */
    interface ModelContentPort {
        /**
         * @return The node port's <code>ModelContent</code> object.
         */
        ModelContentRO getModelContent();
    }

    /**
     * @return The node this port belongs to.
     */
    final Node getNode() {
        return m_node;
    }
} // NodePort
