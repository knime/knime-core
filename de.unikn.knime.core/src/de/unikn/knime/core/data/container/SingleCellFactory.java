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
 *   Jun 20, 2006 (wiswedel): created
 */
package de.unikn.knime.core.data.container;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataRow;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public abstract class SingleCellFactory implements CellFactory {

    /**
     * @see CellFactory#getCells(DataRow)
     */
    public DataCell[] getCells(final DataRow row) {
        return new DataCell[]{getCell(row)};
    }
    
    public abstract DataCell getCell(final DataRow row);

}
