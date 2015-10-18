/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
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
 *   29.04.2014 (Marcel): created
 */
package org.knime.base.data.statistics;

import static org.knime.core.node.util.CheckUtils.checkSetting;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.mutable.MutableLong;
import org.knime.base.data.statistics.calculation.Mean;
import org.knime.base.data.statistics.calculation.Variance;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultCellIterator;
import org.knime.core.data.sort.ColumnBufferedDataTableSorter;
import org.knime.core.data.sort.SortingConsumer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;

/**
 * A utility class for calculating several statistical moments, such as the {@link Variance}, {@link Mean} or others.
 * The <code>org.knime.base.data.statistics.calculation</code> package contains default statistics. One may also
 * implement custom {@link Statistic}s. <br>
 * <b>Example usage:</b>
 *
 * <pre>
 * Median median = new Median(&quot;col1&quot;, &quot;col2&quot;);
 * StatisticCalculator statisticCalculator = new StatisticCalculator(table.getDataTableSpec(), median);
 * statisticCalculator.evaluate(table, executionContext);
 *
 * DataCell medianOfcol1 = median.getMedian(&quot;col1&quot;);
 * </pre>
 *
 * <b>Default columns usage:</b> Default columns should be used if all statistics should be computed on the same set of
 * columns as shown in the example. Columns which are not compatible to a certain statistic are automatically filtered
 * for them.
 *
 * <pre>
 * Median median = new Median();
 * MinMax minMax = new MinMax();
 * StatisticCalculator statisticCalculator =
 *      new StatisticCalculator(table.getDataTableSpec(), filter.getIncludes(), median, minMax);
 * for(String s : filter.getIncludes()){
 *      median.getMedian(s);
 *      ...
 * }
 *
 * </pre>
 *
 * @author Marcel Hanser
 * @since 2.11
 */
public class StatisticCalculator {
    private final Set<String> m_colToSortOn = new LinkedHashSet<String>();

    private final Statistic[] m_statistics;

    /**
     * @param spec the spec
     * @param statistics to compute
     * @throws InvalidSettingsException if the statistics and the spec are conflicting
     */
    public StatisticCalculator(final DataTableSpec spec, final Statistic... statistics)//
        throws InvalidSettingsException {
        this(spec, new String[0], statistics);
    }

    /**
     * @param spec the spec
     * @param defaultColumns the default columns
     * @param statistics to compute
     * @throws InvalidSettingsException if the statistics and the spec are conflicting
     */
    public StatisticCalculator(final DataTableSpec spec, final String[] defaultColumns, final Statistic... statistics)
        throws InvalidSettingsException {
        checkSetting(!ArrayUtils.contains(statistics, null), "Statistics cannot contain null values.");
        checkSetting(ArrayUtils.isNotEmpty(statistics), "Statistics cannot be empty.");

        for (Statistic stat : statistics) {
            stat.init(spec, defaultColumns);
            if (stat instanceof StatisticSorted) {
                Collections.addAll(m_colToSortOn, stat.getColumns());
            }
        }
        m_statistics = statistics;
    }

    /**
     * @param dataTable actual data table to compute the
     * @param exec execution context
     * @return a potential warnings message or <code>null</code>
     * @throws CanceledExecutionException if the user cancels the execution
     */
    public String evaluate(final BufferedDataTable dataTable, final ExecutionContext exec)
        throws CanceledExecutionException {
        for (Statistic stat : m_statistics) {
            stat.beforeEvaluation(dataTable.size());
        }

        if (!m_colToSortOn.isEmpty()) {

            ColumnBufferedDataTableSorter columnDataTableSorter;
            try {
                columnDataTableSorter =
                    new ColumnBufferedDataTableSorter(dataTable.getDataTableSpec(), dataTable.size(),
                        m_colToSortOn.toArray(new String[m_colToSortOn.size()]));
            } catch (InvalidSettingsException e) {
                throw new RuntimeException("Error on initialize the sorting", e);
            }

            exec.setMessage("Sorting Data.");

            final Iterator<DataRow> it = dataTable.iterator();
            final MutableLong count = new MutableLong();
            final ExecutionContext evalProgress = exec.createSubExecutionContext(0.3);

            final int[] specMapping =
                createSpecMapping(dataTable.getSpec(), m_colToSortOn.toArray(new String[m_colToSortOn.size()]));

            columnDataTableSorter.sort(dataTable, exec.createSubExecutionContext(0.7), new SortingConsumer() {

                @Override
                public void consume(final DataRow defaultRow) {
                    DataRow next = it.next();
                    evalProgress.setProgress(count.longValue() / (double)dataTable.size(),
                        "Processing Row: " + next.getKey());
                    count.increment();
                    for (Statistic stat : m_statistics) {
                        stat.consumeRow(new OverwritingRow(next, defaultRow, specMapping));
                    }
                }
            });
        } else {
            exec.setMessage("Evaluating statistics.");
            long count = 0;
            for (DataRow currRow : dataTable) {
                exec.setProgress(count++ / (double)dataTable.size(), "Processing Row: " + currRow.getKey());
                for (Statistic stat : m_statistics) {
                    stat.consumeRow(currRow);
                }
            }
        }
        StringBuilder warnings = new StringBuilder();
        for (Statistic stat : m_statistics) {
            String warningString = stat.finish();
            if (warningString != null) {
                warnings.append(warningString);
                warnings.append("\n");
            }
        }
        return warnings.length() > 0 ? warnings.toString() : null;
    }

    /**
     * @param spec
     * @param createDataTableSpec
     * @return
     */
    private int[] createSpecMapping(final DataTableSpec first, final String[] strings) {
        int[] indexes = new int[first.getNumColumns()];
        int index = 0;
        for (String s : first.getColumnNames()) {
            indexes[index++] = org.apache.commons.lang.ArrayUtils.indexOf(strings, s);
        }
        return indexes;
    }

    private final class OverwritingRow implements DataRow {

        private final DataRow m_firstRow;

        private final DataRow m_overwritingRow;

        private int[] m_indexes;

        private OverwritingRow(final DataRow next, final DataRow next2, final int[] indexMapping) {
            m_firstRow = next;
            m_overwritingRow = next2;
            m_indexes = indexMapping;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Iterator<DataCell> iterator() {
            return new DefaultCellIterator(this);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getNumCells() {
            return m_firstRow.getNumCells();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public RowKey getKey() {
            return m_firstRow.getKey();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder bu = new StringBuilder();
            for (DataCell c : this) {
                bu.append(c);
                bu.append('\t');
            }
            return bu.toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell getCell(final int index) {
            int i = m_indexes[index];
            if (i < 0) {
                return m_firstRow.getCell(index);
            } else {
                return m_overwritingRow.getCell(i);
            }
        }
    }
}
