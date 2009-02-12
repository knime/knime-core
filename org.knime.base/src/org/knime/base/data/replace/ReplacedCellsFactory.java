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
 */
package org.knime.base.data.replace;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;

/**
 * Factory that is used to compute replacing cells in a DataTable that modifies
 * one or more columns of a given input table.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface ReplacedCellsFactory {
    /**
     * Computes the data cells that should replace the <code>column</code>-th
     * column in the given row. The replacing procedure itself is done in the
     * calling class.
     * 
     * @param row the row carrying the "obsolete" cells
     * @param columns the columns that are to be replaced
     * @return the values that serve as replacement
     * @throws IndexOutOfBoundsException if an int argument is out of range
     * @throws NullPointerException if row is <code>null</code>
     */
    DataCell[] getReplacement(final DataRow row, final int[] columns);
}
