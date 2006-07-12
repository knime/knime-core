/* 
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
 */
package de.unikn.knime.core.data.container;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataColumnSpec;
import de.unikn.knime.core.data.DataRow;

/**
 * Factory for a AppendedColummTable that serves to generate the cells of the
 * new columns.
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface CellFactory {

    /**
     * Get the new cells for a given row. These cells are appended to the 
     * existing row.
     * @param row The row of interest.
     * @return The appended cells to that row.
     * @throws IllegalArgumentException  If there is no mapping available.
     */
    DataCell[] getCells(final DataRow row);
    
    /**
     * The column specs for the cells that are generated in the getCells()
     * method. This method is only called once, there is no need to cache
     * the return value. The length of the returned array must match the 
     * length of the array returned by the getCells(DataRow) method and also
     * the types must match, i.e. the type of the respective DataColumnSpec
     * must be of the same type or a syper type of the cell as returned
     * by getCells(DataRow).
     * @return The specs to the newly created cells.
     */
    DataColumnSpec[] getColumnSpecs();
    
}
