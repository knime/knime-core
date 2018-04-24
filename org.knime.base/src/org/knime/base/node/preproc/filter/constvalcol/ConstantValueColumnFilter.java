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
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
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

    /*
     * The option to filter all constant value columns.
     */
    private final boolean m_filterAll;

    /*
     * The option to filter columns with a specific constant numeric value.
     */
    private final boolean m_filterNumeric;

    /*
     * The specific numeric value that is to be looked for in filtering.
     */
    private final double m_filterNumericValue;

    /*
     * The option to filter columns with a specific constant String value.
     */
    private final boolean m_filterString;

    /*
     * The specific String value that is to be looked for in filtering.
     */
    private final String m_filterStringValue;

    /*
     * The option to filter columns containing only missing values.
     */
    private final boolean m_filterMissing;

    /*
     * The minimum number of rows a table must have to be considered for filtering.
     */
    private final long m_rowThreshold;

    /*
     * Constructor for the Constant Value Column Filter class.
     */
    private ConstantValueColumnFilter(final boolean filterAll, final boolean filterNumeric,
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
        /*
         * If the table is smaller than the specified threshold (which should never be smaller than 1), none of the
         * selected columns are removed.
         */
        if (inputTable.size() < Math.max(m_rowThreshold, 1)) {
            return new String[0];
        }

        /*
         * Assemble a map of filter candidates that maps the indices of columns that potentially contain only duplicate
         * values to a column checker object that can be used to check whether further values in that column are
         * specified in the dialog pane and constant (i.e., identical to the first observed value).
         */
        Set<String> colNamesToFilterSet = new HashSet<>(Arrays.asList(colNamesToFilter));
        String[] allColNames = inputTable.getDataTableSpec().getColumnNames();
        RowIterator rowIt = inputTable.iterator();
        DataRow firstRow = rowIt.next();
        Map<Integer, ConstantChecker> columnCheckers = new HashMap<>();
        for (int i = 0; i < allColNames.length; i++) {
            if (colNamesToFilterSet.contains(allColNames[i])) {
                DataCell firstCell = firstRow.getCell(i);
                DataType type = inputTable.getDataTableSpec().getColumnSpec(i).getType();
                ConstantChecker checker = createConstantChecker(type, firstCell);
                if (checker.isCellSpecified(firstCell)) {
                    columnCheckers.put(i, checker);
                }
            }
        }

        /*
         * Across all filter candidates, check if there is any cell whose value differs from the first cell's value.
         * When found, this column is not constant and, thus, should be removed from the filter candidates. This method
         * has a low memory footprint and operates in linear runtime. When the option to filter only constant columns
         * with specific values is selected, columns should also be removed when they are found to contain a value other
         * than any of the specified values.
         */
        final long finalSize = inputTable.size();
        for (long i = 1; i < finalSize; i++) {
            final long finalI = i;
            final DataRow currentRow = rowIt.next();
            exec.checkCanceled();
            exec.setProgress(i / (double)inputTable.size(),
                () -> String.format("Row %,d/%,d (%s)", finalI + 1, finalSize, currentRow.getKey()));

            for (Iterator<Entry<Integer, ConstantChecker>> it = columnCheckers.entrySet().iterator(); it.hasNext();) {
                Entry<Integer, ConstantChecker> entry = it.next();
                DataCell cell = currentRow.getCell(entry.getKey());
                ConstantChecker checker = entry.getValue();

                /*
                 * Columns are removed from the filter candidates, when (a) the current cell has a value other than the
                 * specified / allowed values or (b) it differs from the first cell in this column (i.e., this column is
                 * not constant).
                 */
                if (!(checker.isCellSpecified(cell) && checker.isCellConstant(cell))) {
                    it.remove();
                }
            }
        }

        /*
         * Obtain the names of to-be-filtered columns from the filter candidates map.
         */
        String[] colNamesToRemove = columnCheckers.keySet().stream().map(i -> allColNames[i]).toArray(String[]::new);

        return colNamesToRemove;
    }

    /*
     * A method that creates a new constant checker based on the data type and first cell of a column.
     */
    private ConstantChecker createConstantChecker(final DataType type, final DataCell firstCell) {
        if (firstCell.isMissing()) {
            return new ConstantMissingChecker();
        } else {

            if (type.isCompatible(DoubleValue.class)) {
                return new ConstantDoubleValueChecker(firstCell);
            } else if (type.isCompatible(StringValue.class)) {
                return new ConstantStringValueChecker(firstCell);
            }
            return new ConstantCellChecker(firstCell);

        }
    }

    /*
     * A little helper class that checks if a cell is both constant (i.e., similar to the first observed cell from the
     * same column) and specified by the user.
     */
    abstract class ConstantChecker {
        abstract boolean isCellConstant(final DataCell cell);

        boolean isCellSpecified(final DataCell cell) {
            return m_filterAll || (m_filterMissing && cell.isMissing());
        }
    }

    final class ConstantMissingChecker extends ConstantChecker {
        @Override
        boolean isCellConstant(final DataCell cell) {
            return cell.isMissing();
        }
    }

    final class ConstantCellChecker extends ConstantChecker {
        private final DataCell m_firstCell;

        ConstantCellChecker(final DataCell firstCell) {
            m_firstCell = firstCell;
        }

        @Override
        protected boolean isCellConstant(final DataCell cell) {
            /*
             * This equality check is not quite ideal. Consider a table with a single column containing only integers of
             * value 3 and another table with a single column containing only doubles of value 3. If these tables are
             * concatenated, the resulting table's single column will not be considered constant due to how the equality
             * check of data cells is implemented. Having said that, there currently does not seem to be a better
             * alternative in KNIME's API.
             */
            return m_firstCell.equals(cell);
        }
    }

    final class ConstantDoubleValueChecker extends ConstantChecker {
        private final double m_firstCellValue;

        ConstantDoubleValueChecker(final DataCell firstCell) {
            m_firstCellValue = ((DoubleValue)firstCell).getDoubleValue();
        }

        @Override
        boolean isCellConstant(final DataCell cell) {
            return !cell.isMissing() && ((DoubleValue)cell).getDoubleValue() == m_firstCellValue;
        }

        @Override
        boolean isCellSpecified(final DataCell cell) {
            return (m_filterNumeric && !cell.isMissing()
                && ((DoubleValue)cell).getDoubleValue() == m_filterNumericValue) || super.isCellSpecified(cell);
        }
    }

    final class ConstantStringValueChecker extends ConstantChecker {
        private final String m_firstCellValue;

        ConstantStringValueChecker(final DataCell firstCell) {
            m_firstCellValue = ((StringValue)firstCell).getStringValue();
        }

        @Override
        boolean isCellConstant(final DataCell cell) {
            return !cell.isMissing() && ((StringValue)cell).getStringValue().equals(m_firstCellValue);
        }

        @Override
        boolean isCellSpecified(final DataCell cell) {
            return (m_filterString && !cell.isMissing()
                && ((StringValue)cell).getStringValue().equals(m_filterStringValue)) || super.isCellSpecified(cell);
        }
    }

    /**
     * Builder class used to build new {@link ConstantValueColumnFilter} objects.
     *
     * @author Marc Bux, KNIME AG, Zurich, Switzerland
     */
    public static class ConstantValueColumnFilterBuilder {
        private boolean m_nestedFilterAll = true;

        private boolean m_nestedFilterNumeric = false;

        private double m_nestedFilterNumericValue = 0d;

        private boolean m_nestedFilterString = false;

        private String m_nestedFilterStringValue = "";

        private boolean m_nestedFilterMissing = false;

        private long m_nestedRowThreshold = 1l;

        /**
         * Method for setting the option to filter all constant value columns for the to-be-built
         * {@link ConstantValueColumnFilter} object.
         *
         * @param filterAll the option to filter all constant value columns
         * @return this {@link ConstantValueColumnFilterBuilder} object
         */
        public ConstantValueColumnFilterBuilder filterAll(final boolean filterAll) {
            m_nestedFilterAll = filterAll;
            return this;
        }

        /**
         * Method for setting the option to filter columns with a specific constant numeric value for the to-be-built
         * {@link ConstantValueColumnFilter} object.
         *
         * @param filterNumeric the option to filter columns with a specific constant numeric value
         * @return this {@link ConstantValueColumnFilterBuilder} object
         */
        public ConstantValueColumnFilterBuilder filterNumeric(final boolean filterNumeric) {
            m_nestedFilterNumeric = filterNumeric;
            return this;
        }

        /**
         * Method for setting the specific numeric value that is to be looked for in filtering for the to-be-built
         * {@link ConstantValueColumnFilter} object.
         *
         * @param filterNumericValue the specific numeric value that is to be looked for in filtering
         * @return this {@link ConstantValueColumnFilterBuilder} object
         */
        public ConstantValueColumnFilterBuilder filterNumericValue(final double filterNumericValue) {
            m_nestedFilterNumericValue = filterNumericValue;
            return this;
        }

        /**
         * Method for setting the option to filter columns with a specific constant String value for the to-be-built
         * {@link ConstantValueColumnFilter} object.
         *
         * @param filterString the option to filter columns with a specific constant String value
         * @return this {@link ConstantValueColumnFilterBuilder} object
         */
        public ConstantValueColumnFilterBuilder filterString(final boolean filterString) {
            m_nestedFilterString = filterString;
            return this;
        }

        /**
         * Method for setting the specific String value that is to be looked for in filtering for the to-be-built
         * {@link ConstantValueColumnFilter} object.
         *
         * @param filterStringValue the specific String value that is to be looked for in filtering
         * @return this {@link ConstantValueColumnFilterBuilder} object
         */
        public ConstantValueColumnFilterBuilder filterStringValue(final String filterStringValue) {
            m_nestedFilterStringValue = filterStringValue;
            return this;
        }

        /**
         * Method for setting the option to filter columns containing only missing values for the to-be-built
         * {@link ConstantValueColumnFilter} object.
         *
         * @param filterMissing the option to filter columns containing only missing values
         * @return this {@link ConstantValueColumnFilterBuilder} object
         */
        public ConstantValueColumnFilterBuilder filterMissing(final boolean filterMissing) {
            m_nestedFilterMissing = filterMissing;
            return this;
        }

        /**
         * Method for setting the minimum number of rows a table must have to be considered for filtering for the
         * to-be-built {@link ConstantValueColumnFilter} object.
         *
         * @param rowThreshold the minimum number of rows a table must have to be considered for filtering
         * @return this {@link ConstantValueColumnFilterBuilder} object
         */
        public ConstantValueColumnFilterBuilder rowThreshold(final long rowThreshold) {
            m_nestedRowThreshold = rowThreshold;
            return this;
        }

        /**
         * Method for building new {@link ConstantValueColumnFilter} objects.
         *
         * @return a new {@link ConstantValueColumnFilter} object
         */
        public ConstantValueColumnFilter createConstantValueColumnFilter() {
            return new ConstantValueColumnFilter(m_nestedFilterAll, m_nestedFilterNumeric, m_nestedFilterNumericValue,
                m_nestedFilterString, m_nestedFilterStringValue, m_nestedFilterMissing, m_nestedRowThreshold);
        }
    }
}
