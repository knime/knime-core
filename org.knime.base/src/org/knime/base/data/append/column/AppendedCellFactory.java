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
 */
package org.knime.base.data.append.column;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;

/**
 * Factory for an
 * {@link AppendedColumnTable} that
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
