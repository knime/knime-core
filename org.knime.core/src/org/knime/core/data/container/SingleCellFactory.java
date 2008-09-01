/* 
 * 
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
 *   Jun 20, 2006 (wiswedel): created
 */
package org.knime.core.data.container;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;

/**
 * Convenience implementation of a cell factory with one new column.
 * @author Bernd Wiswedel, University of Konstanz
 */
public abstract class SingleCellFactory extends DefaultCellFactory {
    
    /** Create new cell factory that provides one column given by newColSpec. 
     * @param newColSpec The spec of the new column.
     */
    public SingleCellFactory(final DataColumnSpec newColSpec) {
        super(newColSpec);
    }
    
    /** {@inheritDoc} */
    @Override
    public DataCell[] getCells(final DataRow row) {
        return new DataCell[]{getCell(row)};
    }

    /**
     * Called from getCells. Return the single cell to be returned.
     * @param row The reference row.
     * @return The new cell.
     */
    public abstract DataCell getCell(final DataRow row);
    
}
