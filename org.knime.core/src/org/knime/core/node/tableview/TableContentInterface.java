/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
