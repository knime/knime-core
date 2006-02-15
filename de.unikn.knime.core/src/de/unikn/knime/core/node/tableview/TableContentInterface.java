/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
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
 *   Nov 16, 2005 (wiswedel): created
 */
package de.unikn.knime.core.node.tableview;

import javax.swing.event.TableModelListener;

import de.unikn.knime.core.data.RowKey;

/** 
 * Interface used by the row header view of a table. It allows to retrieve 
 * information regarding the row keys in a table and their hilite status.
 * @author wiswedel, University of Konstanz
 */
public interface TableContentInterface {

    /** 
     * Get the number of rows that are in the table.
     * @return The number of rows in the table.
     * @see javax.swing.table.TableModel#getRowCount() 
     */
    int getRowCount();

    /**
     * Get the row key for a given row index. The row key will be displayed
     * in a separate (row header) table. 
     * @param row The row index.
     * @return The key of that row.
     * @throws IndexOutOfBoundsException If the index is out of range.
     */
    RowKey getRowKey(final int row);

    /** 
     * Is the Row <code>row</code> highlighted? The row with the index will
     * be cached (if it hasn't been in there) and the highlight status of the
     * row is returned. This method may change the current cache content since
     * it ensures <code>row</code> is in the cache 
     * @param row The row index of interest
     * @return <code>true</code> If that index is currently highlighted
     */
    boolean isHiLit(final int row);

    /**
     * Adds a listener to the list that is notified each time a change
     * to the data model occurs.
     *
     * @param   l       the TableModelListener
     */
    public void addTableModelListener(final TableModelListener l);

    /**
     * Removes a listener from the list that is notified each time a
     * change to the data model occurs.
     *
     * @param   l       the TableModelListener
     */
    public void removeTableModelListener(final TableModelListener l);

}
