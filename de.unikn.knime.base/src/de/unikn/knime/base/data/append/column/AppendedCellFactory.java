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
package de.unikn.knime.base.data.append.column;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataRow;

/**
 * Factory for an
 * {@link de.unikn.knime.base.data.append.column.AppendedColumnTable} that
 * serves to generate the cells of the new columns.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface AppendedCellFactory {

    /**
     * Get the new cells for a given row. These cells are appended to the
     * existing row.
     * 
     * @param row The row of interest.
     * @return The appended cells to that row.
     * @throws IllegalArgumentException If there is no mapping available.
     */
    DataCell[] getAppendedCell(final DataRow row);
}
