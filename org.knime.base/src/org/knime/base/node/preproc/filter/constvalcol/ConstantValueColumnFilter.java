/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   11 Apr 2018 (Marc Bux, KNIME AG, Zurich, Switzerland): created
 */
package org.knime.base.node.preproc.filter.constvalcol;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowIterator;
import org.knime.core.data.StringValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 * A class for determining columns that only contain only (duplicates of) the same value.
 *
 * @author Marc Bux, KNIME AG, Zurich, Switzerland
 * @since 3.6
 */
public class ConstantValueColumnFilter {

    /**
     * The option to filter all constant value columns.
     */
    private final boolean m_filterAll;

    /**
     * The option to filter columns with a specific constant numeric value.
     */
    private final boolean m_filterNumeric;

    /**
     * The specific numeric value that is to be looked for in filtering.
     */
    private final double m_filterNumericValue;

    /**
     * The option to filter columns with a specific constant String value.
     */
    private final boolean m_filterString;

    /**
     * The specific String value that is to be looked for in filtering.
     */
    private final String m_filterStringValue;

    /**
     * The option to filter columns containing only missing values.
     */
    private final boolean m_filterMissing;

    /**
     * The minimum number of rows a table must have to be considered for filtering.
     */
    private final long m_rowThreshold;

    /**
     * Constructor for the Constant Value Column Filter class.
     *
     * @param filterAll the option to filter all constant value columns
     * @param filterNumeric the option to filter columns with a specific constant numeric value
     * @param filterNumericValue the specific numeric value that is to be looked for in filtering
     * @param filterString the option to filter columns with a specific constant String value
     * @param filterStringValue the specific String value that is to be looked for in filtering
     * @param filterMissing the option to filter columns containing only missing values
     * @param rowThreshold the minimum number of rows a table must have to be considered for filtering
     */
    public ConstantValueColumnFilter(final boolean filterAll, final boolean filterNumeric,
        final double filterNumericValue, final boolean filterString, final String filterStringValue,
        final boolean filterMissing, final long rowThreshold) {
        m_filterAll = filterAll;
        m_filterNumeric = filterNumeric;
        m_filterNumericValue = filterNumericValue;
        m_filterString = filterString;
        m_filterStringValue = filterStringValue;
        m_filterMissing = filterMissing;
        m_rowThreshold = rowThreshold;
    }

    /**
     * A method that, from a selection of columns, determines the columns that contain only the same (duplicate /
     * constant) value over and over.
     *
     * @param inputTable the input table that is to be investigated for columns with constant values
     * @param colNamesToFilter the names of columns that potentially contain constant values only
     * @param exec execution context for updating progress and cancelation checking
     * @return the names of columns that provably contain constant values only
     * @throws CanceledExecutionException
     */
    public String[] determineConstantValueColumns(final BufferedDataTable inputTable, final String[] colNamesToFilter,
        final ExecutionContext exec) throws CanceledExecutionException {
        // If the table is smaller than the specified threshold, none of the selected columns are removed
        if (inputTable.size() < m_rowThreshold) {
            return new String[0];
        }
        // If the table contains no data, all selected columns are considered constant and removed
        if (inputTable.size() < 1) {
            return new String[0];
        }

        // Assemble a map of filter candidates that maps the indices of columns that potentially contain only duplicate
        // values to their last observed value.
        Set<String> colNamesToFilterSet = new HashSet<>(Arrays.asList(colNamesToFilter));
        String[] allColNames = inputTable.getDataTableSpec().getColumnNames();
        // TODO: change to map int -> FilterObject, where the latter depends on the type of column
        // TODO: change from last observed to first value
        Map<Integer, DataCell> filterColsLastObsVals = new HashMap<>();
        for (int i = 0; i < allColNames.length; i++) {
            if (colNamesToFilterSet.contains(allColNames[i])) {
                // We have not observed any values yet, so the last observed value is null.
                filterColsLastObsVals.put(i, null);
            }
        }

        // Across all filter candidates, check if there are two (vertically) successive cells with different values.
        // When found, this column is not constant and, thus, should be removed from the filter candidates. This method
        // has a low memory footprint and operates in linear runtime. When the option to filter only constant columns
        // with specific values is selected, columns should also be removed when they are found to contain a value
        // other than any of the specified values.
        for (RowIterator rowIt = inputTable.iterator(); rowIt.hasNext();) {
            DataRow currentRow = rowIt.next();
            exec.checkCanceled();
            // TODO set progress mit supplier
            //            exec.setProgress(0.2, "Checking row " + rowIndex);
            for (Iterator<Entry<Integer, DataCell>> entryIt = filterColsLastObsVals.entrySet().iterator(); entryIt
                .hasNext();) {
                Entry<Integer, DataCell> filterColsLastObsVal = entryIt.next();
                // currentCell can't be null; lastCell can be null (in the first row).
                DataCell currentCell = currentRow.getCell(filterColsLastObsVal.getKey());
                DataCell lastCell = filterColsLastObsVal.getValue();
                filterColsLastObsVal.setValue(currentCell);

                // Columns are removed from the filter candidates, when
                // (a) the currentCell has a value other than the specified / allowed values or
                // (b) it differs from the last observed cell in this column (i.e., this column is not constant).
                if (!isValueSpecified(currentCell) || (lastCell != null && !currentCell.equals(lastCell))) {
                    entryIt.remove();
                }
            }
        }

        // Obtain the names of to-be-filtered columns from the filter candidates map
        String[] colNamesToRemove =
            filterColsLastObsVals.keySet().stream().map(i -> allColNames[i]).toArray(String[]::new);

        return colNamesToRemove;
    }

    /**
     * A function that determines whether the value of a given DataCell has been specified in the dialog pane to be
     * filtered.
     *
     * @param cell the cell whose value is to be checked
     * @return <code>true</code>, if and only if the cell's value qualifies for being filtered
     */
    private boolean isValueSpecified(final DataCell cell) {
        if (m_filterAll) {
            return true;
        }
        if (m_filterNumeric && cell instanceof DoubleValue
            && ((DoubleValue)cell).getDoubleValue() == m_filterNumericValue) {
            return true;
        }
        if (m_filterString && cell instanceof StringValue
            && ((StringValue)cell).getStringValue().equals(m_filterStringValue)) {
            return true;
        }
        if (m_filterMissing && cell instanceof MissingCell) {
            return true;
        }
        return false;
    }

}
