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
import de.unikn.knime.core.data.DataColumnSpec;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.RowKey;
import de.unikn.knime.core.node.ExecutionMonitor;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public abstract class SingleCellFactory implements CellFactory {
    
    private final DataColumnSpec[] m_colSpec;
    
    public SingleCellFactory(final DataColumnSpec newColSpec) {
        m_colSpec = new DataColumnSpec[]{newColSpec};
    }
    
    public DataColumnSpec[] getColumnSpecs() {
        return m_colSpec;
    }

    /**
     * @see CellFactory#getCells(DataRow)
     */
    public DataCell[] getCells(final DataRow row) {
        return new DataCell[]{getCell(row)};
    }
    
    public abstract DataCell getCell(final DataRow row);
    
    /**
     * @see CellFactory#setProgress(int, int, RowKey, ExecutionMonitor)
     */
    public void setProgress(final int curRowNr, final int rowCount, 
            final RowKey lastKey, final ExecutionMonitor exec) {
        exec.setProgress(curRowNr / (double)rowCount, "Processed row " 
                + curRowNr + " (\"" + lastKey + "\")");
    }

}
