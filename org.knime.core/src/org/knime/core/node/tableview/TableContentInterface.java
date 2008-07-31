/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Nov 16, 2005 (wiswedel): created
 * 2006-06-08 (tm): reviewed   
 */
package org.knime.core.node.tableview;

import javax.swing.event.TableModelListener;

import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;


/** 
 * Interface used by the row header view of a table. It allows to retrieve 
 * information regarding the row keys in a table and their hilite status.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface TableContentInterface {

    /** 
     * Get the number of rows that are in the table.
     * 
     * @return The number of rows in the table.
     * @see javax.swing.table.TableModel#getRowCount() 
     */
    int getRowCount();

    /**
     * Get the row key for a given row index. The row key will be displayed
     * in a separate (row header) table.
     * 
     * @param row The row index.
     * @return The key of that row.
     * @throws IndexOutOfBoundsException If the index is out of range.
     */
    RowKey getRowKey(final int row);
    
    /**
     * Get the color of a requested row, The color is shown as icon in front
     * of the row key.
     * 
     * @param row The row index.
     * @return The color attribute for this row.
     * @throws IndexOutOfBoundsException If the index is out of range.
     */
    ColorAttr getColorAttr(final int row);

    /** 
     * Is the Row <code>row</code> hilited? The row with the index will
     * be cached (if it hasn't been in there) and the hilite status of the
     * row is returned. This method may change the current cache content since
     * it ensures <code>row</code> is in the cache.
     * 
     * @param row The row index of interest
     * @return <code>true</code> If that index is currently hilited
     */
    boolean isHiLit(final int row);

    /**
     * Adds a listener to the list that is notified each time a change
     * to the data model occurs.
     *
     * @param l the TableModelListener
     */
    public void addTableModelListener(final TableModelListener l);

    /**
     * Removes a listener from the list that is notified each time a
     * change to the data model occurs.
     *
     * @param l the TableModelListener
     */
    public void removeTableModelListener(final TableModelListener l);
}
