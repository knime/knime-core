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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   08.05.2006(sieb, ohl): reviewed 
 */
package org.knime.core.node;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * Represents a data input port of a node. It is used by the node to retrieve
 * the <code>DataTable</code>, <code>DataTableSpec</code>, and
 * <code>HiLiteHandler objects</code> from the predecessor connected to this
 * port.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class DataInPort extends NodeInPort 
    implements NodePort.DataPort {

    /**
     * Creates new input data port.
     * 
     * @param portId This port's ID.
     * @param node This port's node.
     */
    public DataInPort(final int portId, final Node node) {
        super(portId, node);
    }

    /**
     * Returns the <code>DataTable</code> for this port by retrieving it from
     * the connected output port or throws an exception if the port is not
     * connected.
     * 
     * @return DataTable from the connected output port or null if not
     *         available.
     * @throws IllegalStateException If the port is not connected.
     * @see #isConnected()
     */
    public BufferedDataTable getBufferedDataTable() {
        if (!isConnected()) {
            throw new IllegalStateException("Cannot get DataTable from "
                    + "unconnected input port (" + getPortID() + ")");
        }
        return ((DataOutPort)super.getConnectedPort()).getBufferedDataTable();
    }

    /**
     * Called by the connected output port to notify its counterparts of a new
     * datatable available.
     */
    void newDataTableAvailable() {
        getNode().inportHasNewDataTable(getPortID());
    }

    /**
     * Returns the <code>DataTableSpec</code> for this port by retrieving it
     * from the connected output port.
     * 
     * @return DataTableSpec from the connected output port or <code>null</code>
     *         if not connected.
     * 
     * @see #isConnected()
     */
    public DataTableSpec getDataTableSpec() {
        if (!isConnected()) {
            return null;
        }
        return ((DataOutPort)super.getConnectedPort()).getDataTableSpec();
    }

    /**
     * Called by the connected output port to notify its counterparts of a new
     * data table spec available.
     */
    void newTableSpecAvailable() {
        super.getNode().inportHasNewTableSpec(getPortID());
    }

    /**
     * Returns the <code>HiLiteHandler</code> for this port retrieving it from
     * its connected port. Returns null if the port is not connected.
     * 
     * @return The HiLiteHandler from the connected output port or null.
     */
    public HiLiteHandler getHiLiteHandler() {
        if (!isConnected()) {
            return null;
        }
        return ((DataOutPort)super.getConnectedPort()).getHiLiteHandler();
    }

    /**
     * Invoked by the connected output port to notify its counterparts of a new
     * hilite handler available.
     */
    void newHiLitHandlerAvailable() {
        getNode().inportHasNewHiLiteHandler(getPortID());
    }

}
