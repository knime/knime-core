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
 *   14.05.2007 (Fabian Dill): created
 */
package org.knime.base.node.preproc.columnTrans;

import java.util.regex.Pattern;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class RegExpCellFactory extends AbstractMany2OneCellFactory {
    
    private Pattern m_pattern;

    /**
     * 
     * @param inputSpec input spec of the whole table
     * @param appendedColumnName name of the new column
     * @param includedColsIndices indices of columns to condense
     * @param regExp regular expression to determine matching columns
     */
    public RegExpCellFactory(final DataTableSpec inputSpec, 
            final String appendedColumnName, 
            final int[] includedColsIndices, final String regExp) {
        super(inputSpec, 
                appendedColumnName, includedColsIndices);
        m_pattern = Pattern.compile(regExp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int findColumnIndex(final DataRow row) {
        int columnIndex = -1;
        for (int i : getIncludedColIndices()) {
            DataCell cell = row.getCell(i);
            if (cell.isMissing()) {
                continue;
            }
            if (m_pattern.matcher(
                    cell.toString().trim()).matches()) {
                if (columnIndex >= 0) {
                    throw new IllegalArgumentException(
                            "Multiple columns match in row "
                            + row.getKey().getString());
                }
                columnIndex = i;
            }
        }
        return columnIndex;
    }

}
