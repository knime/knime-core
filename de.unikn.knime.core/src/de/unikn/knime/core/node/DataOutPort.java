/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   24.10.2005 (gabriel): created
 *   08.05.2006(sieb, ohl): reviewed 
 */
package de.unikn.knime.core.node;

import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.property.hilite.HiLiteHandler;

/**
 * Represents a data ouput port of a node. It is used by the node to store
 * the output <code>DataTable</code>, <code>DataTableSpec</code>, and
 * <code>HiLiteHandler</code> objects. It provides methods for the connected
 * successor ports to retrieve these objects and it also notifies connected
 * ports if new objects are available. <br>
 * It's also possible to open a port view, which displays the objects currently
 * stored in the port.
 *  
 * @author Peter Ohl, University of Konstanz
 */
public final class DataOutPort extends NodeOutPort 
        implements NodePort.DataPort {
    
    /**
     * The data table for this port - if any.
     */
    private BufferedDataTable m_dataTable;

    /**
     * The table spec for this port.
     */
    private DataTableSpec m_tableSpec;
    
    /**
     * The hilite handler of this port.
     */
    private HiLiteHandler m_hiliteHdl;

    /**
     * Creates a new output data port. 
     * @param portId The port's ID.
     * @param node the node this port belongs to
     */
    public DataOutPort(final int portId, final Node node) {
        super(portId, node);
        m_tableSpec = null;
        m_dataTable = null;
        m_hiliteHdl = null;
    }
    
    /**
     * Sets a new data table spec in this port. The port will notify all
     * connected input ports and port views.
     * 
     * @param tSpec The new data table spec for this port or null.
     */
    void setDataTableSpec(final DataTableSpec tSpec) {
        m_tableSpec = tSpec;
        for (NodeInPort inPort : super.getConnectedInPorts()) {
            ((DataInPort) inPort).newTableSpecAvailable();
        }
        if (getPortView() != null) {
            ((DataOutPortView) getPortView()).updateDataTableSpec(m_tableSpec);
        }
    }

    /**
     * Returns the <code>DataTableSpec</code> or null if not available.
     * 
     * @return The <code>DataTableSpec</code> for this port.
     */
    public DataTableSpec getDataTableSpec() {
        return m_tableSpec;
    }
    
    /**
     * Sets a new data table in this port. The port will notify all connected
     * input ports and its view.
     * 
     * @param dataTable The new data table for this port or null.
     */
    void setDataTable(final BufferedDataTable dataTable) {
        //  (tg) also set the new spec here
        if (dataTable != null) {
            setDataTableSpec(dataTable.getDataTableSpec());
        }
        m_dataTable = dataTable;
        for (NodeInPort inPort : super.getConnectedInPorts()) {
            ((DataInPort) inPort).newDataTableAvailable();
        }
        if (getPortView() != null) {
            ((DataOutPortView) getPortView()).updateDataTable(m_dataTable);
        }
    }

    /**
     * Returns the DataTable for this port, as set by the node this port is
     * output for.
     * 
     * @return DataTable the DataTable for this port. Could be null.
     */
    public BufferedDataTable getBufferedDataTable() {
        return m_dataTable;
    }
    
    /**
     * Returns the hilite handler for this port as set by the node this port is
     * output for.
     * 
     * @return The HiLiteHandler for this port or null.
     */
    public HiLiteHandler getHiLiteHandler() {
        return m_hiliteHdl;
    }
    
    /**
     * Sets a new hilight handler in the port. The port will notify all
     * connected input ports of a really new (i.e. not equal) hightlight handler
     * available.
     * 
     * @param hiLiteHdl The new HiLiteHandler for this port.
     */
    void setHiLiteHandler(final HiLiteHandler hiLiteHdl) {

        boolean equal;
        if (hiLiteHdl == null) {
            equal = (m_hiliteHdl == null);
        } else {
            equal = hiLiteHdl.equals(m_hiliteHdl);
        }

        if (!equal) {
            m_hiliteHdl = hiLiteHdl;
            for (NodeInPort inPort : super.getConnectedInPorts()) {
                ((DataInPort) inPort).newHiLitHandlerAvailable();
            }
            if (getPortView() != null) {
                ((DataOutPortView) getPortView()).updateHiliteHandler(
                        m_hiliteHdl);
            }
        }
    }
    
    /**
     * Opens a view for this port displaying the current data table, data table
     * spec, and hilite handler settings.
     * @param nodeName The name of the node this port belongs to. 
     */
    @Override
    public void openPortView(final String nodeName) {
        if (getPortView() == null) {
            super.setPortView(new DataOutPortView(nodeName, getPortName()));
            ((DataOutPortView) getPortView()).updateDataTable(m_dataTable);
            ((DataOutPortView) getPortView()).updateDataTableSpec(m_tableSpec);
            ((DataOutPortView) getPortView()).updateHiliteHandler(m_hiliteHdl);
        }
        getPortView().openView();
    }
    
}
