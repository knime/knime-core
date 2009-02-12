/*
 * ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   14.05.2007 (Fabian Dill): created
 */
package org.knime.base.node.preproc.columnTrans;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class MinMaxCellFactory extends AbstractMany2OneCellFactory {

    private final boolean m_max;
    
    
    /**
     * @param inputSpec input spec of the whole table
     * @param appendedColumnName name of the new column
     * @param includedColsIndices indices of columns to condense
     * @param max true if maximum value leads to inclusion 
     */
    public MinMaxCellFactory(final DataTableSpec inputSpec,
            final String appendedColumnName, 
            final int[] includedColsIndices, 
            final boolean max) {
        super(inputSpec, 
                appendedColumnName, includedColsIndices);
        m_max = max;
    }


    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public int findColumnIndex(final DataRow row) {
        if (m_max) {
            return findMaximumColumn(row);
        }
        return findMinimumColumn(row);
    }
    
    private int findMaximumColumn(final DataRow row) {
        boolean multipleValue = false;
        // current min value
        double maxValue = Integer.MIN_VALUE;
        // list of matching column indices
        int columnIndex = -1;
        for (int i : getIncludedColIndices()) {
            if (row.getCell(i).isMissing()) {
                continue;
            }
            double currentValue = ((DoubleValue)row.getCell(i))
                .getDoubleValue();
            if (maxValue == currentValue) {
                multipleValue = true;
            }
            if (maxValue < currentValue) {
                multipleValue = false;
                maxValue = currentValue;
                columnIndex = i;
            }
        }
        if (multipleValue) {
            throw new IllegalArgumentException(
                    "Multiple columns match in row "
                    + row.getKey().getString());
        }
        return columnIndex;
    }
    
    private int findMinimumColumn(final DataRow row) {
        boolean multipleValue = false;
        // current min value
        double minValue = Integer.MAX_VALUE;
        // list of matching column indices
        int columnIndex = -1;
        for (int i : getIncludedColIndices()) {
            if (row.getCell(i).isMissing()) {
                continue;
            }
            double currentValue = ((DoubleValue)row.getCell(i))
                .getDoubleValue();
            if (minValue == currentValue) {
                multipleValue = true;
            }            
            if (minValue > currentValue) {
                multipleValue = false;
                minValue = currentValue;
                columnIndex = i;
            }
        }
        if (multipleValue) {
            throw new IllegalArgumentException(
                    "Multiple columns match in row "
                    + row.getKey().getString());
        }
        return columnIndex;
    }

    
}
