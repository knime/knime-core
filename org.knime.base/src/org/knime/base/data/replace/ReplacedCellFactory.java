/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *   16.01.2006 (cebron): created
 */
package org.knime.base.data.replace;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;

/**
 * Convenience class that should be used if your
 * {@link ReplacedCellsFactory} replaces only
 * one column.
 * 
 * @author Nicolas Cebron, University of Konstanz
 */
public abstract class ReplacedCellFactory implements ReplacedCellsFactory {
    /**
     * Returns an array of length 1 containing the replacement of the data cell
     * at the position given in the first element of the columns array.
     * 
     * @see ReplacedCellsFactory
     *      #getReplacement(org.knime.core.data.DataRow, int[])
     */
    public final DataCell[] getReplacement(final DataRow row,
            final int[] columns) {
        return new DataCell[]{getReplacement(row, columns[0])};
    }

    /**
     * Computes the data cell that should replace the <code>column</code>-th
     * column in the given row. The replacing procedure itself is done in the
     * calling class.
     * 
     * @param row the row carrying the "obsolete" cell
     * @param column the column that is to be replaced
     * @return the value that the serves as replacement
     * @throws IndexOutOfBoundsException if int argument is out of range
     * @throws NullPointerException if row is <code>null</code>
     */
    public abstract DataCell getReplacement(final DataRow row, 
                final int column);
}
