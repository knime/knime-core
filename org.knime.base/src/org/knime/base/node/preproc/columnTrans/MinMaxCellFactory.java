/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
