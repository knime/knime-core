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
 */
package de.unikn.knime.base.node.io.filereader;

import java.awt.event.MouseEvent;

import javax.swing.table.JTableHeader;

import de.unikn.knime.core.node.tableview.TableContentView;

/**
 * Extension of an KNIME table view that reacts on mouse events in the header.
 * It will sent a property change event to anyone interested in whenever the
 * column header in the table is clicked on. It will sent the column index with
 * the event as "new value" (last argument), the "old value" (middle argument)
 * will always be <code>null</code>.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class PreviewTableContentView extends TableContentView {

    /** Property event ID when table spec has changed. */
    static final String PROPERTY_SPEC_CHANGED = "TableSpecChanged";

    /**
     * Disallows reordering.
     * 
     * @see javax.swing.JTable#setTableHeader(javax.swing.table.JTableHeader)
     */
    @Override
    public void setTableHeader(final JTableHeader newTableHeader) {
        if (newTableHeader != null) {
            newTableHeader.setReorderingAllowed(false);
        }
        super.setTableHeader(newTableHeader);
    }

    /**
     * @see TableContentView#onMouseClickInHeader(MouseEvent)
     */
    @Override
    protected void onMouseClickInHeader(final MouseEvent e) {
        JTableHeader header = getTableHeader();
        // get column in which event occured
        int column = header.columnAtPoint(e.getPoint());
        if (column < 0) {
            return;
        }
        int modelIndex = convertColumnIndexToModel(column);
        firePropertyChange(PROPERTY_SPEC_CHANGED, null, modelIndex);
    }
}
