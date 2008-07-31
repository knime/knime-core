/*
 * ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   16.05.2007 (Fabian Dill): created
 */
package org.knime.base.node.preproc.columnTrans;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;

/**
 * Cell with value = 1 matches, others don't.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class BinaryCellFactory extends AbstractMany2OneCellFactory {
    
    /**
     * 
     * @param inputSpec input spec of the whole table
     * @param appendedColumnName name of the new column
     * @param includedColsIndices indices of columns to condense
     */
    public BinaryCellFactory(final DataTableSpec inputSpec, 
            final String appendedColumnName, final int[] includedColsIndices) {
        super(inputSpec, appendedColumnName, includedColsIndices);
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public int findColumnIndex(final DataRow row) {
        int colIndex = -1;
        for (int i : getIncludedColIndices()) {
            if (row.getCell(i).isMissing()) {
                continue;
            }
            double currentValue = ((DoubleValue)row.getCell(i))
                .getDoubleValue();
            if (currentValue == 1) {
                if (colIndex >= 0) {
                    throw new IllegalArgumentException(
                            "Multiple columns match in row "
                            + row.getKey().getString());
                }
                colIndex = i;
            }
        }
        return colIndex;
    }

}
