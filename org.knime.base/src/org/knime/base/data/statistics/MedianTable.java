/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * Created on 2013.06.13. by Gabor
 */
package org.knime.base.data.statistics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.mutable.MutableLong;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.sort.ColumnBufferedDataTableSorter;
import org.knime.core.data.sort.SortingConsumer;
import org.knime.core.data.sort.SortingDescription;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;

/**
 * Finds the median for selected ({@link DoubleValue}d) columns.
 *
 * @author Gabor Bakos
 * @since 2.8
 */
class MedianTable {

    private final BufferedDataTable m_table;

    private final int[] m_indices;

    private final boolean m_includeNaNs;

    private final boolean m_includeMissingValues;

    private double[] m_medians;

    /**
     * @param table The input table.
     * @param indices The unique column indices denoting only {@link DoubleValue}d columns within {@code table}.
     */
    public MedianTable(final BufferedDataTable table, final int[] indices) {
        this(table, indices, true, false);
    }

    /**
     * @param table The input table.
     * @param colNames The unique column names denoting only {@link DoubleValue}d columns within {@code table}.
     * @param includeNaNs Include the {@link Double#NaN} values to the possible values?
     * @param includeMissingValues Include the missing values to the number of values?
     */
    public MedianTable(final BufferedDataTable table, final List<String> colNames, final boolean includeNaNs,
        final boolean includeMissingValues) {
        this(table, findIndices(table, colNames), includeNaNs, includeMissingValues);
    }

    /**
     * @param table The input table.
     * @param colNames The unique column names denoting only {@link DoubleValue}d columns within {@code table}.
     * @return The column indices of {@code colNames} within {@code table}.
     */
    private static int[] findIndices(final BufferedDataTable table, final List<String> colNames) {
        final DataTableSpec spec = table.getSpec();
        if (new HashSet<String>(colNames).size() != colNames.size()) {
            throw new IllegalArgumentException("Same column name multiple times: " + colNames);
        }
        int[] ret = new int[colNames.size()];
        for (int i = colNames.size(); i-- > 0;) {
            ret[i] = spec.findColumnIndex(colNames.get(i));
        }
        return ret;
    }

    /**
     * @param table The input table.
     * @param indices The unique column indices denoting only {@link DoubleValue}d columns within {@code table}.
     * @param includeNaNs Include the {@link Double#NaN} values to the possible values?
     * @param includeMissingValues Include the missing values to the number of values?
     */
    public MedianTable(final BufferedDataTable table, final int[] indices, final boolean includeNaNs,
        final boolean includeMissingValues) {
        this.m_table = table;
        this.m_indices = indices.clone();
        this.m_includeNaNs = includeNaNs;
        this.m_includeMissingValues = includeMissingValues;
    }

    /**
     * @param context An {@link ExecutionContext}
     * @return The median values for the columns in the order of the columns specified in the constructor. The values
     *         can be {@link Double#NaN}s in certain circumstances.
     * @throws CanceledExecutionException When cancelled.
     */
    public synchronized double[] medianValues(final ExecutionContext context) throws CanceledExecutionException {
        if (m_medians == null) {
            m_medians = new double[m_indices.length];
            int[] validCount = new int[m_indices.length];
            for (DataRow row : m_table) {
                context.checkCanceled();
                for (int i = 0; i < m_indices.length; ++i) {
                    int col = m_indices[i];
                    final DataCell cell = row.getCell(col);
                    if (cell.isMissing()) {
                        if (m_includeMissingValues) {
                            validCount[i]++;
                        }
                    } else if (cell instanceof DoubleValue) {
                        DoubleValue dv = (DoubleValue)cell;
                        if (m_includeNaNs) {
                            validCount[i]++;
                        } else if (!Double.isNaN(dv.getDoubleValue())) {
                            validCount[i]++;
                        }
                    } else {
                        throw new IllegalStateException("Not a double value: " + cell + " in column: "
                            + m_table.getSpec().getColumnSpec(col).getName());
                    }
                }
            }
            List<String> incList = new ArrayList<String>(m_indices.length);
            final String[] columnNames = m_table.getSpec().getColumnNames();
            for (int i : m_indices) {
                incList.add(columnNames[i]);
            }

            // two indices per column that denote the lower and upper index of the median value (or both the same)
            long[][] k = new long[2][m_indices.length];
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < m_indices.length; j++) {
                    k[i][j] = validCount[j] > 0 ? (validCount[j] - 1 + i) / 2 : 0;
                }
            }
            sortOnDisk(context, k);
        }
        return m_medians.clone();
    }

    /**
     * Sorts the data on the disk, it moves the missing values to the end.
     *
     * @param context An {@link ExecutionContext}.
     * @param k The indices to read from the different columns
     *        (first dim: length 2 (above & below median indices), second dim: columns)
     * @throws CanceledExecutionException Execution was cancelled.
     */
    private void sortOnDisk(final ExecutionContext context, final long[][] k) throws CanceledExecutionException {
        final SortingDescription[] sorting = new SortingDescription[m_indices.length];
        final DataTableSpec spec = m_table.getSpec();
        for (int i = 0; i < m_indices.length; i++) {
            final DataColumnSpec columnSpec = spec.getColumnSpec(m_indices[i]);
            final DataValueComparator comparator = columnSpec.getType().getComparator();
            sorting[i] = new SortingDescription(columnSpec.getName()) {

                @Override
                public int compare(final DataRow o1, final DataRow o2) {
                    // Move missing values to the end.
                    final DataCell c1 = o1.getCell(0);
                    final DataCell c2 = o2.getCell(0);
                    if (c1.isMissing()) {
                        return c2.isMissing() ? 0 : 1;
                    }
                    if (c2.isMissing()) {
                        return -1;
                    }
                    return comparator.compare(c1, c2);
                }
            };
        }
        final ColumnBufferedDataTableSorter tableSorter;
        try {
            tableSorter = new ColumnBufferedDataTableSorter(m_table.getSpec(), m_table.size(), sorting);
        } catch (InvalidSettingsException e) {
            throw new IllegalStateException(e);
        }
        final MutableLong counter = new MutableLong();
        final DoubleValue[][] cells = new DoubleValue[2][m_indices.length];
        tableSorter.sort(m_table, context, new SortingConsumer() {
            @Override
            public void consume(final DataRow row) {
                for (int kindex = 0; kindex < 2; kindex++) {
                    for (int i = 0; i < m_indices.length; i++) {
                        if (counter.longValue() == k[kindex][i]) {
                            DataCell cell = row.getCell(i);
                            if (cell instanceof DoubleValue) {
                                DoubleValue dv = (DoubleValue)cell;
                                cells[kindex][i] = dv;
                            } else {
                                cells[kindex][i] = new DoubleCell(Double.NaN);
                            }
                        }
                    }
                }
                counter.increment();
            }
        });
        for (int index = m_indices.length; index-- > 0;) {
            if (cells[0][index] == null || cells[1][index] == null) {
                //No non-missing rows
                m_medians[index] = Double.NaN;
            } else {
                m_medians[index] = (cells[0][index].getDoubleValue() + cells[1][index].getDoubleValue()) / 2;
            }
        }
    }

    /**
     * @param inMemory the inMemory to set
     * @deprecated No longer in use - KNIME decided when to swap to disc
     */
    @Deprecated
    public synchronized void setInMemory(final boolean inMemory) {
    }
}
