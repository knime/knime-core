/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   15.04.2005 (cebron): created
 */
package org.knime.base.data.statistics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math.stat.descriptive.moment.Kurtosis;
import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.Skewness;
import org.apache.commons.math.stat.descriptive.moment.Variance;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.BlobWrapperDataCell;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DefaultTable;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.MutableInteger;

/**
 * New statistic table utility class to compute statistical moments, such as mean, variance, column sum, count missing
 * values, min/max values, median, and count occurrences of all possible values.
 *
 * @author Thomas Gabriel, University of Konstanz
 * @author Gabor Bakos
 * @since 2.8
 * @deprecated use the {@link StatisticCalculator} framework instead.
 */
@Deprecated
public class Statistics3Table {
    /** Specification for the stats in the columns. */
    private static final DataTableSpec STATISTICS_SPECIFICATION;
    static {
        DataColumnSpecCreator columnCreator = new DataColumnSpecCreator("Column", StringCell.TYPE);
        DataColumnSpecCreator minCreator = new DataColumnSpecCreator("Min", DoubleCell.TYPE);
        DataColumnSpecCreator maxCreator = new DataColumnSpecCreator("Max", DoubleCell.TYPE);
        DataColumnSpecCreator meanCreator = new DataColumnSpecCreator("Mean", DoubleCell.TYPE);
        DataColumnSpecCreator stdDevCreator = new DataColumnSpecCreator("Std. deviation", DoubleCell.TYPE);
        DataColumnSpecCreator varianceCreator = new DataColumnSpecCreator("Variance", DoubleCell.TYPE);
        DataColumnSpecCreator skewnessCreator = new DataColumnSpecCreator("Skewness", DoubleCell.TYPE);
        DataColumnSpecCreator kurtosisCreator = new DataColumnSpecCreator("Kurtosis", DoubleCell.TYPE);
        DataColumnSpecCreator sumCreator = new DataColumnSpecCreator("Overall sum", DoubleCell.TYPE);
        DataColumnSpecCreator missingCreator = new DataColumnSpecCreator("No. missings", IntCell.TYPE);
        DataColumnSpecCreator nanCreator = new DataColumnSpecCreator("No. NaNs", IntCell.TYPE);
        DataColumnSpecCreator posInfCreator = new DataColumnSpecCreator("No. +\u221Es", IntCell.TYPE);
        DataColumnSpecCreator negInfCreator = new DataColumnSpecCreator("No. -\u221Es", IntCell.TYPE);
        DataColumnSpecCreator medianCreator = new DataColumnSpecCreator("Median", DoubleCell.TYPE);
        DataColumnSpecCreator rowCountCreator = new DataColumnSpecCreator("Row count", IntCell.TYPE);
        STATISTICS_SPECIFICATION =
            new DataTableSpec(columnCreator.createSpec(), minCreator.createSpec(), maxCreator.createSpec(),
                meanCreator.createSpec(), stdDevCreator.createSpec(), varianceCreator.createSpec(),
                skewnessCreator.createSpec(), kurtosisCreator.createSpec(), sumCreator.createSpec(),
                missingCreator.createSpec(), nanCreator.createSpec(), posInfCreator.createSpec(),
                negInfCreator.createSpec(), medianCreator.createSpec(), rowCountCreator.createSpec());
    }

    /** Used to cache the media for each column. */
    private final double[] m_median;

    /** Array of maps containing DataValue value to number of occurrences. */
    private final List<Map<DataCell, Integer>> m_nominalValues;

    /** Used to 'cache' the mean values. */
    private final double[] m_meanValues;

    /** Used to 'cache' the variance values. */
    private final double[] m_varianceValues;

    /** Used to 'cache' the sum of each column. */
    private final double[] m_sum;

    /** Used to 'cache' the number of missing values per columns. In the order of nominalValueColumns. */
    private final int[] m_missingValueCnt;

    /** Used to 'cache' the number of {@link Double#NaN} values per columns. In the order of nominalValueColumns. */
    private final int[] m_nanValueCnt;

    /**
     * Used to 'cache' the number of {@link Double#POSITIVE_INFINITY} values per columns. In the order of
     * nominalValueColumns.
     */
    private final int[] m_posInfinityValueCnt;

    /**
     * Used to 'cache' the number of {@link Double#NEGATIVE_INFINITY} values per columns. In the order of
     * nominalValueColumns.
     */
    private final int[] m_negInfinityValueCnt;

    /** Used to 'cache' the minimum values. */
    private final double[] m_minValues;

    /** Used to 'cache' the maximum values. */
    private final double[] m_maxValues;

    /** Used to 'cache' the minimum values. */
    private final DataCell[] m_minCells;

    /** Used to 'cache' the maximum values. */
    private final DataCell[] m_maxCells;

    /** Used to 'cache' the minimum non-infinite values. */
    private final DataCell[] m_minNonInfValues;

    /** Used to 'cache' the maximum non-infinite values. */
    private final DataCell[] m_maxNonInfValues;

    /** Used to 'cache' the row count. */
    private final int m_rowCount;

    /** Used to 'cache' the skewness. */
    private final double[] m_skewness;

    /** Used to 'cache' the kurtosis. */
    private final double[] m_kurtosis;

    /** Column name from the original data table spec. */
    private final DataTableSpec m_spec;

    /** If not null, a warning has been created during construction. */
    private final String m_warning;

    /**
     * Create new statistic table from an existing one. This constructor calculates all values. It needs to traverse
     * (twice) through the entire specified table. User can cancel action if an execution monitor is passed.
     *
     * @param table table to be wrapped
     * @param computeMedian if the median has to be computed
     * @param numNomValuesOutput number of possible values in output table
     * @param nominalValueColumns columns used to determine all poss. values
     * @param exec an object to check with if user canceled operation
     * @throws CanceledExecutionException if user canceled
     */
    public Statistics3Table(final BufferedDataTable table, final boolean computeMedian, final int numNomValuesOutput,
        final List<String> nominalValueColumns, final ExecutionContext exec) throws CanceledExecutionException {
        this(table, computeMedian, numNomValuesOutput, nominalValueColumns, exec, allApplicableColumns(
            table.getDataTableSpec(), nominalValueColumns));
    }

    /**
     * Finds those columns that have applicable columns.
     *
     * @param spec A {@link DataTableSpec}.
     * @param nominalValueColumns The list of names of nominal values to check.
     * @return The indices (in ascending order) which are {@link DoubleValue}d, or one of the nominal values.
     * @since 2.9
     */
    protected static int[] allApplicableColumns(final DataTableSpec spec, final List<String> nominalValueColumns) {
        final int[] allColumns = allColumns(spec.getNumColumns());
        int toRemove = 0;
        for (int i = allColumns.length; i-- > 0;) {
            final DataColumnSpec colSpec = spec.getColumnSpec(i);
            if (colSpec.getType().isCompatible(DoubleValue.class) || nominalValueColumns.contains(colSpec.getName())) {
                allColumns[i] = i;
            } else {
                allColumns[i] = -1;
                ++toRemove;
            }
        }
        if (toRemove > 0) {
            int[] filtered = new int[allColumns.length - toRemove];
            int j = 0;
            for (int i = 0; i < allColumns.length; ++i) {
                if (allColumns[i] >= 0) {
                    filtered[j++] = allColumns[i];
                }
            }
            return filtered;
        }
        return allColumns;
    }

    /**
     * @param numberOfColumns The number of columns.
     * @return Indices from {@code 0} to {@code numberOfColumns - 1}.
     */
    private static int[] allColumns(final int numberOfColumns) {
        int[] ret = new int[numberOfColumns];
        for (int i = ret.length; i-- > 0;) {
            ret[i] = i;
        }
        return ret;
    }

    /**
     * Create new statistic table from an existing one. This constructor calculates all values. It needs to traverse
     * (twice) through the entire specified table. User can cancel action if an execution monitor is passed.
     *
     * @param table table to be wrapped
     * @param computeMedian if the median has to be computed
     * @param numNomValuesOutput number of possible values in output table
     * @param nominalValueColumns columns used to determine all poss. values
     * @param exec an object to check with if user canceled operation
     * @param selectedColumnIndices The indices of columns to compute the statistics.
     * @throws CanceledExecutionException if user canceled
     */
    public Statistics3Table(final BufferedDataTable table, final boolean computeMedian, final int numNomValuesOutput,
        final List<String> nominalValueColumns, final ExecutionContext exec, final int... selectedColumnIndices)
        throws CanceledExecutionException {
        final int[] colIndices = check(selectedColumnIndices, table.getSpec(), nominalValueColumns);
        int nrCols = table.getDataTableSpec().getNumColumns();
        m_spec = table.getDataTableSpec();
        // initialize cache arrays
        m_meanValues = new double[nrCols];
        //Using Mean and Variance from commons math. Bug: 4286
        Mean[] means = new Mean[nrCols];
        m_varianceValues = new double[nrCols];
        Variance[] variances = new Variance[nrCols];
        for (int i = nrCols; i-- > 0;) {
            means[i] = new Mean();
            variances[i] = new Variance(true);
        }
        m_sum = new double[nrCols];
        m_minValues = new double[nrCols];
        m_maxValues = new double[nrCols];
        m_minCells = new DataCell[nrCols];
        m_maxCells = new DataCell[nrCols];
        m_minNonInfValues = new DataCell[nrCols];
        m_maxNonInfValues = new DataCell[nrCols];
        m_missingValueCnt = new int[nrCols];
        m_nanValueCnt = new int[nrCols];
        m_posInfinityValueCnt = new int[nrCols];
        m_negInfinityValueCnt = new int[nrCols];
        m_median = new double[nrCols];
        m_nominalValues = new ArrayList<Map<DataCell, Integer>>(nominalValueColumns.size());
        for (int i = nrCols; i-- > 0;) {
            m_nominalValues.add(null);
        }
        m_rowCount = table.getRowCount();
        //Using Skewness and Kurtosis from commons math.
        m_skewness = new double[nrCols];
        m_kurtosis = new double[nrCols];
        final Skewness[] skewness = new Skewness[nrCols];
        final Kurtosis[] kurtosis = new Kurtosis[nrCols];
        for (int i = nrCols; i-- > 0;) {
            skewness[i] = new Skewness();
            kurtosis[i] = new Kurtosis();
        }

        Set<String> nominalValueColumnsSet = new HashSet<String>(nominalValueColumns);

        // the number of non-missing cells in each column
        int[] validCount = new int[nrCols];
        double[] sumsquare = new double[nrCols];
        for (int i = 0; i < nrCols; i++) {
            m_missingValueCnt[i] = 0;
            m_meanValues[i] = Double.NaN;
            m_sum[i] = Double.NaN;
            m_varianceValues[i] = Double.NaN;
            m_minCells[i] = DataType.getMissingCell();
            m_maxCells[i] = DataType.getMissingCell();
            m_minNonInfValues[i] = DataType.getMissingCell();
            m_maxNonInfValues[i] = DataType.getMissingCell();
            m_skewness[i] = Double.NaN;
            m_kurtosis[i] = Double.NaN;
            m_median[i] = Double.NaN;
            sumsquare[i] = 0.0;
            validCount[i] = 0;
        }

        // used to store warnings
        final StringBuilder warn = new StringBuilder();

        // temp map used to sort later based in occurrences
        final List<Map<DataCell, MutableInteger>> nominalValues =
            new ArrayList<Map<DataCell, MutableInteger>>(m_nominalValues.size());
        for (int i = m_nominalValues.size(); i-- > 0;) {
            nominalValues.add(null);
        }

        final int rowCnt = table.getRowCount();
        double diffProgress = rowCnt;
        if (computeMedian) {
            for (int i : colIndices) {
                if (m_spec.getColumnSpec(i).getType().isCompatible(DoubleValue.class)) {
                    diffProgress += rowCnt;
                }
            }
        }
        int rowIdx = 0;
        for (RowIterator rowIt = table.iterator(); rowIt.hasNext(); rowIdx++) {
            DataRow row = rowIt.next();
            exec.setProgress(rowIdx / diffProgress, "Calculating statistics, processing row " + (rowIdx + 1) + " (\""
                + row.getKey() + "\")");

            onStatisticComputation(row);

            for (int c : colIndices) {
                exec.checkCanceled();
                DataColumnSpec cspec = m_spec.getColumnSpec(c);
                final DataCell cell = row.getCell(c);
                if (!(cell.isMissing())) {
                    // for double columns we calc the sum (for the mean calc)
                    if (cspec.getType().isCompatible(DoubleValue.class)) {
                        double d = ((DoubleValue)cell).getDoubleValue();
                        means[c].increment(d);
                        variances[c].increment(d);
                        updateMinMax(c, cell, cspec.getType().getComparator());
                        if (d == Double.POSITIVE_INFINITY) {
                            m_posInfinityValueCnt[c]++;
                        }
                        if (d == Double.NEGATIVE_INFINITY) {
                            m_negInfinityValueCnt[c]++;
                        }
                        if (Double.isNaN(d)) {
                            m_nanValueCnt[c]++;
                        }
                        skewness[c].increment(d);
                        kurtosis[c].increment(d);
                        sumsquare[c] += d * d;
                        validCount[c]++;
                    }
                } else {
                    m_missingValueCnt[c]++;
                }
                if (nominalValueColumnsSet.contains(cspec.getName())) {
                    if (nominalValues.get(c) == null || (nominalValues.get(c) != null
                    // list is only empty, when the number of poss.
                    // values exceeded the maximum
                        && nominalValues.get(c).size() > 0)) {
                        if (nominalValues.get(c) == null) {
                            nominalValues.set(c, new LinkedHashMap<DataCell, MutableInteger>());
                        }
                        MutableInteger cnt = nominalValues.get(c).get(cell);
                        if (cnt == null) {
                            nominalValues.get(c).put(cell, new MutableInteger(1));
                        } else {
                            cnt.inc();
                        }
                        if (nominalValues.get(c).size() == numNomValuesOutput + 1) {
                            if (warn.length() == 0) {
                                warn.append("Maximum number of unique possible " + "values (" + numNomValuesOutput
                                    + ") exceeds for column(s): ");
                            } else {
                                warn.append(",");
                            }
                            warn.append("\"" + m_spec.getColumnSpec(c).getName() + "\"");
                            nominalValues.get(c).clear();
                        }
                    }
                }
            }
        }
        //Table is empty, but we should provide the nominal values an empty map.
        if (!table.iterator().hasNext()) {
            for (int c : colIndices) {
                if (nominalValueColumnsSet.contains(m_spec.getColumnSpec(c).getName())) {
                    nominalValues.set(c, Collections.<DataCell, MutableInteger> emptyMap());
                }
            }
        }

        // init warning message
        if (warn.length() > 0) {
            m_warning = warn.toString();
        } else {
            m_warning = null;
        }

        for (int j = 0; j < nrCols; j++) {
            // in case we got an empty table or columns that contain only
            // missing values
            if (validCount[j] == 0) {
                m_minValues[j] = Double.NaN;
                m_maxValues[j] = Double.NaN;
                m_meanValues[j] = Double.NaN;
                m_varianceValues[j] = Double.NaN;
                m_sum[j] = 0.0;
                m_skewness[j] = Double.NaN;
                m_kurtosis[j] = Double.NaN;
            } else {
                m_minValues[j] = m_minCells[j].isMissing() ? Double.NaN : ((DoubleValue) m_minCells[j]).getDoubleValue();
                m_maxValues[j] = m_maxCells[j].isMissing() ? Double.NaN : ((DoubleValue) m_maxCells[j]).getDoubleValue();
                m_meanValues[j] = means[j].getResult();
                m_varianceValues[j] = variances[j].getResult();
                m_sum[j] = means[j].getResult() * means[j].getN();
                m_skewness[j] = skewness[j].getResult();
                m_kurtosis[j] = kurtosis[j].getResult();
                // unreported bug fix: in cases in which a column contains
                // almost only one value (for instance 1.0) but one single
                // 'outlier' whose value is, for instance 0.9999998, we get
                // round-off errors resulting in negative variance values
                if (m_varianceValues[j] < 0.0 && m_varianceValues[j] > -1.0E8) {
                    m_varianceValues[j] = 0.0;
                }
                assert Double.isNaN(m_varianceValues[j]) || m_varianceValues[j] >= 0.0 : "Variance cannot be "
                    + "negative (column \"" + m_spec.getColumnSpec(j).getName() + "\": " + m_varianceValues[j] + ")";
            }
        }

        // copy map and sort each column
        for (int c = 0; c < nominalValues.size(); c++) {
            Map<DataCell, MutableInteger> map = nominalValues.get(c);
            if (map == null) {
                m_nominalValues.set(c, null);
                continue;
            }
            List<Map.Entry<DataCell, MutableInteger>> list =
                new ArrayList<Entry<DataCell, MutableInteger>>(map.entrySet());
            Collections.sort(list, new Comparator<Map.Entry<DataCell, MutableInteger>>() {
                /** {@inheritDoc} */
                @Override
                public int compare(final Entry<DataCell, MutableInteger> o1, final Entry<DataCell, MutableInteger> o2) {
                    return o2.getValue().intValue() - o1.getValue().intValue();
                }
            });
            m_nominalValues.set(c, new LinkedHashMap<DataCell, Integer>(list.size()));
            for (Map.Entry<DataCell, MutableInteger> e : list) {
                m_nominalValues.get(c).put(e.getKey(), e.getValue().intValue());
            }
        }

        // compute median values if desired
        if (computeMedian) {
            final int[] filteredIndices = filter(table.getSpec(), colIndices);
            final MedianTable medianTable = new MedianTable(table, filteredIndices);
            medianTable.setInMemory(table.getRowCount() < Runtime.getRuntime().freeMemory() / Double.SIZE / 2);
            double[] medianValues = medianTable.medianValues(exec);
            for (int i = 0; i < filteredIndices.length; ++i) {
                m_median[filteredIndices[i]] = medianValues[i];
            }
        }
    }

    /**
     * Hook for subclasses to perform additional computations.
     *
     * @param row current row for computation
     * @since 2.9
     */
    protected void onStatisticComputation(final DataRow row) {
        //NOOP
    }

    /**
     * Filters out those indices that are not compatible with {@link DoubleValue}s.
     *
     * @param spec The {@link DataTableSpec}.
     * @param colIndices The column indices to check.
     * @return The subset of column indices that belong to columns compatible with {@link DoubleValue}s.
     */
    private int[] filter(final DataTableSpec spec, final int[] colIndices) {
        List<Integer> indices = new ArrayList<Integer>();
        for (int colIdx : colIndices) {
            if (spec.getColumnSpec(colIdx).getType().isCompatible(DoubleValue.class)) {
                indices.add(colIdx);
            }
        }
        int[] ret = new int[indices.size()];
        int i = 0;
        for (Integer colIdx : indices) {
            ret[i++] = colIdx.intValue();
        }
        return ret;
    }

    /**
     * Sanity check of the input parameters.
     *
     * @param selectedColumnIndices The column indices where the stats should be computed.
     * @param spec The {@link DataTableSpec}.
     * @param nominalValueColumns The columns where the possible values should be computed.
     * @return The clone of {@code selectedColumnIndices}.
     * @throws IllegalArgumentException When there was an invalid argument.
     */
    private int[] check(final int[] selectedColumnIndices, final DataTableSpec spec,
        final List<String> nominalValueColumns) {
        int numColumns = spec.getNumColumns();
        for (int i : selectedColumnIndices) {
            if (i < 0 || i >= numColumns) {
                throw new IllegalArgumentException("Invalid column index: " + i + " in " + spec);
            }
        }
        Set<Integer> indices = new HashSet<Integer>();
        for (int idx : selectedColumnIndices) {
            if (!indices.add(idx)) {
                throw new IllegalArgumentException("Selected indices are not unique: "
                    + Arrays.toString(selectedColumnIndices) + " duplicate: " + idx);
            }
        }
        for (String colName : nominalValueColumns) {
            final int colIndex = spec.findColumnIndex(colName);
            if (colIndex < 0 || !indices.contains(colIndex)) {
                throw new IllegalArgumentException(
                    "The selected column for nominal values is not among the selected indices.");
            }
        }
        return selectedColumnIndices.clone();
    }

    private static final String[] ROW_HEADER = new String[]{"Minimum", "Maximum", "Mean", "Std. deviation", "Variance",
        "Overall sum", "No. missings", "Median", "Row count", "No. NaNs", "No. +infinities", "No. -infinities"};

    /**
     * Creates a table of statistic moments such as minimum, maximum, mean, standard deviation, variance, overall sum,
     * no. of missing vales, and median.
     *
     * @return a table with one moment in each row across all input columns
     */
    public DataTable createStatisticMomentsTable() {
        DataRow[] data = new DataRow[12];
        data[0] = createRow(ROW_HEADER[0], getMin());
        data[1] = createRow(ROW_HEADER[1], getMax());
        data[2] = createRow(ROW_HEADER[2], getMean());
        data[3] = createRow(ROW_HEADER[3], getStandardDeviation());
        data[4] = createRow(ROW_HEADER[4], getVariance());
        data[5] = createRow(ROW_HEADER[5], getSum());
        data[6] = createRow(ROW_HEADER[6], getNumberMissingValues());
        data[7] = createRow(ROW_HEADER[7], getMedian());
        final double[] rowCounts = new double[m_spec.getNumColumns()];
        Arrays.fill(rowCounts, m_rowCount);
        data[8] = createRow(ROW_HEADER[8], rowCounts);
        data[9] = createRow(ROW_HEADER[9], getNumberNaNValues());
        data[10] = createRow(ROW_HEADER[10], getNumberPositiveInfiniteValues());
        data[11] = createRow(ROW_HEADER[11], getNumberNegativeInfiniteValues());
        return new DefaultTable(data, createOutSpecNumeric(m_spec));
    }

    /**
     * Creates the statistics in transposed compared to the original.
     *
     * @param exec An {@link ExecutionContext}.
     * @return Statistics {@link BufferedDataTable} with skewness and kurtosis in a transposed form.
     * @since 2.9
     */
    public BufferedDataTable createStatisticsInColumnsTable(final ExecutionContext exec) {
        BufferedDataContainer container = exec.createDataContainer(getStatisticsSpecification());
        int colIdx = 0;
        for (DataColumnSpec spec : m_spec) {
            if (spec.getType().isCompatible(DoubleValue.class)) {
                container.addRowToTable(new DefaultRow(spec.getName(), createRow(spec.getName(), colIdx)));
            }
            colIdx++;
        }
        container.close();
        return container.getTable();
    }

    /**
     * Creates a row for the transposed table.
     *
     * @param name The name of the column.
     * @param colIdx The index of column in the computed values.
     * @return The cells according to {@link #STATISTICS_SPECIFICATION}.
     */
    private DataCell[] createRow(final String name, final int colIdx) { final DataCell[] ret = new DataCell[getStatisticsSpecification().getNumColumns()];
        int i = 0;
        ret[i++] = new StringCell(name);
        ret[i++] = m_minCells[colIdx];
        ret[i++] = m_maxCells[colIdx];
        ret[i++] = new DoubleCell(m_meanValues[colIdx]);
        ret[i++] = new DoubleCell(Math.sqrt(m_varianceValues[colIdx]));
        ret[i++] = new DoubleCell(m_varianceValues[colIdx]);
        ret[i++] = new DoubleCell(m_skewness[colIdx]);
        ret[i++] = new DoubleCell(m_kurtosis[colIdx]);
        ret[i++] = new DoubleCell(m_sum[colIdx]);
        ret[i++] = new IntCell(m_missingValueCnt[colIdx]);
        ret[i++] = new IntCell(m_nanValueCnt[colIdx]);
        ret[i++] = new IntCell(m_posInfinityValueCnt[colIdx]);
        ret[i++] = new IntCell(m_negInfinityValueCnt[colIdx]);
        ret[i++] = Double.isNaN(m_median[colIdx]) ? DataType.getMissingCell() : new DoubleCell(m_median[colIdx]);
        ret[i++] = new IntCell(m_rowCount);
        return ret;
    }

    /**
     * Create nominal value table containing all possible values together with their occurrences.
     *
     * @param nominal value output table
     * @return data table with nominal values for each column
     */
    public DataTable createNominalValueTable(final List<String> nominal) {
        DataTableSpec outSpec = createOutSpecNominal(m_spec, nominal);
        @SuppressWarnings("unchecked")
        Iterator<Entry<DataCell, Integer>>[] it = new Iterator[(outSpec.getNumColumns() / 3)];
        long[] totals = new long[it.length];
        for (int i = 0, index = 0; i < m_nominalValues.size(); i++) {
            Map<DataCell, Integer> currentMap = m_nominalValues.get(i);
            if (currentMap != null) {
                it[index] = currentMap.entrySet().iterator();
                totals[index] = currentMap.values().stream().collect(Collectors.summingLong(Integer::valueOf));
                index += 1;
            }
        }
        DataContainer cont = new DataContainer(outSpec, true);
        int rowIndex = 0;
        do {
            boolean addEnd = true;
            DataCell[] cells = new DataCell[3 * it.length];
            for (int i = 0; i < it.length; i++) {
                if (it[i].hasNext()) {
                    Map.Entry<DataCell, Integer> e = it[i].next();
                    cells[3 * i] = e.getKey();
                    int count = e.getValue().intValue();
                    cells[3 * i + 1] = new IntCell(count);
                    cells[3 * i + 2] = new DoubleCell((double)count / totals[i]);
                    addEnd = false;
                } else {
                    cells[3 * i] = DataType.getMissingCell();
                    cells[3 * i + 1] = DataType.getMissingCell();
                    cells[3 * i + 2] = DataType.getMissingCell();
                }
            }
            if (addEnd) {
                break;
            }
            cont.addRowToTable(new DefaultRow(RowKey.createRowKey(rowIndex++), cells));
        } while (true);
        cont.close();
        return cont.getTable();
    }

    private DataRow createRow(final String key, final double[] array) {
        int idx = 0;
        DataTableSpec outSpec = createOutSpecNumeric(m_spec);
        DataCell[] data = new DataCell[outSpec.getNumColumns()];
        for (int i = 0; idx < data.length; i++) {
            if (outSpec.getColumnSpec(idx).getName().equals(m_spec.getColumnSpec(i).getName())) {
                if (Double.isNaN(array[i])) {
                    data[idx] = DataType.getMissingCell();
                } else {
                    data[idx] = new DoubleCell(array[i]);
                }
                idx++;
            }
        }
        return new DefaultRow(key, data);
    }

    private DataRow createRow(final String key, final int[] array) {
        int idx = 0;
        DataTableSpec outSpec = createOutSpecNumeric(m_spec);
        DataCell[] data = new DataCell[outSpec.getNumColumns()];
        for (int i = 0; idx < data.length; i++) {
            if (outSpec.getColumnSpec(idx).getName().equals(m_spec.getColumnSpec(i).getName())) {
                data[idx] = array[i] < 0 ? DataType.getMissingCell() : new DoubleCell(array[i]);
                idx++;
            }
        }
        return new DefaultRow(key, data);
    }

    /**
     * Create spec containing only numeric columns in same order as the input spec.
     *
     * @param inSpec input spec
     * @return a new spec with all numeric columns
     */
    public static DataTableSpec createOutSpecNumeric(final DataTableSpec inSpec) {
        ArrayList<DataColumnSpec> cspecs = new ArrayList<DataColumnSpec>();
        for (int i = 0; i < inSpec.getNumColumns(); i++) {
            DataColumnSpec cspec = inSpec.getColumnSpec(i);
            if (cspec.getType().isCompatible(DoubleValue.class)) {
                cspecs.add(new DataColumnSpecCreator(cspec.getName(), DoubleCell.TYPE).createSpec());
            }
        }
        return new DataTableSpec(cspecs.toArray(new DataColumnSpec[0]));
    }

    /**
     * Create spec containing only nominal columns in same order as the input spec.
     *
     * @param inSpec input spec
     * @param nominalValues used in map of co-occurrences
     * @return a new spec with all nominal columns
     */
    public static DataTableSpec createOutSpecNominal(final DataTableSpec inSpec, final List<String> nominalValues) {
        ArrayList<DataColumnSpec> cspecs = new ArrayList<DataColumnSpec>();
        for (int i = 0; i < inSpec.getNumColumns(); i++) {
            DataColumnSpec cspec = inSpec.getColumnSpec(i);
            if (nominalValues.contains(cspec.getName())) {
                cspecs.add(cspec);
                String countCol = DataTableSpec.getUniqueColumnName(inSpec, cspec.getName() + "_Count");
                cspecs.add(new DataColumnSpecCreator(countCol, IntCell.TYPE).createSpec());
                String percentCol =
                    DataTableSpec.getUniqueColumnName(inSpec, "Relative Frequency (" + cspec.getName() + ")");
                DataColumnSpecCreator percentColSpecCreator = new DataColumnSpecCreator(percentCol, DoubleCell.TYPE);
                percentColSpecCreator
                    .setDomain(new DataColumnDomainCreator(new DoubleCell(0.0), new DoubleCell(1.0)).createDomain());
                cspecs.add(percentColSpecCreator.createSpec());
            }
        }
        return new DataTableSpec(cspecs.toArray(new DataColumnSpec[cspecs.size()]));
    }

    /**
     * Returns an array of valid columns.
     *
     * @param nominalValues The column names to filter (the result might contain less values).
     * @return an array of string column which are valid in in conjunction with the current data spec
     */
    public final String[] extractNominalColumns(final List<String> nominalValues) {
        ArrayList<String> columns = new ArrayList<String>();
        for (int i = 0; i < m_spec.getNumColumns(); i++) {
            DataColumnSpec cspec = m_spec.getColumnSpec(i);
            if (nominalValues.contains(cspec.getName())) {
                columns.add(cspec.getName());
            }
        }
        return columns.toArray(new String[0]);
    }

    /**
     * @return array of column names
     */
    public String[] getColumnNames() {
        String[] names = new String[m_spec.getNumColumns()];
        for (int i = 0; i < names.length; i++) {
            names[i] = m_spec.getColumnSpec(i).getName();
        }
        return names;
    }

    /**
     * Returns the mean for the desired column. Throws an exception if the specified column is not compatible to
     * DoubleValue. Returns {@link Double#NaN} if the specified column contains only missing cells or if the table is
     * empty.
     *
     * @param colIdx the column index for which the mean is calculated
     * @return mean value or {@link Double#NaN}
     */
    public double getMean(final int colIdx) {
        return m_meanValues[colIdx];
    }

    /**
     * Returns the means for all columns. Returns {@link Double#NaN} if the column type is not of type
     * {@link DoubleValue}.
     *
     * @return an array of mean values with an item for each column, which is {@link Double#NaN} if the column type is
     *         not {@link DoubleValue}
     */
    public double[] getMean() {
        double[] result = new double[m_meanValues.length];
        System.arraycopy(m_meanValues, 0, result, 0, result.length);
        return result;
    }

    /**
     * Returns the sum for the desired column. Throws an exception if the specified column is not compatible to
     * DoubleValue. Returns {@link Double#NaN} if the specified column contains only missing cells or if the table is
     * empty.
     *
     * @param colIdx the column index for which the mean is calculated
     * @return sum value or {@link Double#NaN}
     */
    public double getSum(final int colIdx) {
        return m_sum[colIdx];
    }

    /**
     * Returns the sum values for all columns. Returns {@link Double#NaN} if the column type is not of type
     * {@link DoubleValue}.
     *
     * @return an array of sum values with an item for each column, which is {@link Double#NaN} if the column type is
     *         not {@link DoubleValue}
     */
    public double[] getSum() {
        double[] result = new double[m_sum.length];
        System.arraycopy(m_sum, 0, result, 0, result.length);
        return result;
    }

    /**
     * Returns an array of the number of missing values for each dimension.
     *
     * @return number missing values for each dimensions
     */
    public int[] getNumberMissingValues() {
        int[] result = new int[m_missingValueCnt.length];
        System.arraycopy(m_missingValueCnt, 0, result, 0, result.length);
        return result;
    }

    /**
     * Returns the number of missing values for the given column index.
     *
     * @param colIdx column index to consider
     * @return number of missing values in this columns
     */
    public double getNumberMissingValues(final int colIdx) {
        return m_missingValueCnt[colIdx];
    }

    /**
     * Returns an array of the number of {@link Double#NaN} values for each dimension.
     *
     * @return number of {@link Double#NaN} values for each dimensions
     * @since 2.9
     */
    public int[] getNumberNaNValues() {
        return m_nanValueCnt.clone();
    }

    /**
     * Returns the number of {@link Double#NaN} values for the given column index.
     *
     * @param colIdx column index to consider
     * @return number of {@link Double#NaN} values in this column
     * @since 2.9
     */
    public int getNumberNaNValues(final int colIdx) {
        return m_nanValueCnt[colIdx];
    }

    /**
     * Returns an array of the number of {@link Double#POSITIVE_INFINITY} values for each dimension.
     *
     * @return number of {@link Double#POSITIVE_INFINITY} values for each dimensions
     * @since 2.9
     */
    public int[] getNumberPositiveInfiniteValues() {
        return m_posInfinityValueCnt.clone();
    }

    /**
     * Returns the number of {@link Double#POSITIVE_INFINITY} values for the given column index.
     *
     * @param colIdx column index to consider
     * @return number of {@link Double#POSITIVE_INFINITY} values in this column
     * @since 2.9
     */
    public int getNumberPositiveInfiniteValues(final int colIdx) {
        return m_posInfinityValueCnt[colIdx];
    }

    /**
     * Returns an array of the number of {@link Double#NEGATIVE_INFINITY} values for each dimension.
     *
     * @return number of {@link Double#NEGATIVE_INFINITY} values for each dimensions
     * @since 2.9
     */
    public int[] getNumberNegativeInfiniteValues() {
        return m_negInfinityValueCnt.clone();
    }

    /**
     * Returns the number of {@link Double#NEGATIVE_INFINITY} values for the given column index.
     *
     * @param colIdx column index to consider
     * @return number of {@link Double#NEGATIVE_INFINITY} values in this column
     * @since 2.9
     */
    public int getNumberNegativeInfiniteValues(final int colIdx) {
        return m_negInfinityValueCnt[colIdx];
    }

    /**
     * Returns the variance for the desired column. Throws an exception if the specified column is not compatible to
     * {@link DoubleValue}. Returns {@link Double#NaN} if the specified column contains only missing cells or if the
     * table is empty.
     *
     * @param colIdx the column index for which the variance is calculated
     * @return variance or {@link Double#NaN}
     */
    public double getVariance(final int colIdx) {
        return m_varianceValues[colIdx];
    }

    /**
     * Returns the variance for all columns. Returns {@link Double#NaN} if the column type is not of type
     * {@link DoubleValue}, if the entire column contains missing cells, or if the table is empty.
     *
     * @return variance values
     */
    public double[] getVariance() {
        double[] result = new double[m_varianceValues.length];
        System.arraycopy(m_varianceValues, 0, result, 0, result.length);
        return result;
    }

    /**
     * Calculates the standard deviation for the desired column. Throws an exception if the column type is not
     * compatible to {@link DoubleValue}. Will return zero if the column contains only missing cells or the table was
     * empty.
     *
     * @param colIdx the index of the column for which the standard deviation is to be calculated
     * @return standard deviation or zero if its a column of missing values of the table is empty
     */
    public double getStandardDeviation(final int colIdx) {
        return Math.sqrt(m_varianceValues[colIdx]);
    }

    /**
     * Returns the standard deviation for all columns. The returned array contains no valid value (i.e.
     * {@link Double#NaN}) for column that are not compatible to {@link DoubleValue}.
     *
     * @return standard deviation values
     */
    public double[] getStandardDeviation() {
        double[] temp = getVariance();
        for (int i = 0; i < temp.length; i++) {
            if (!Double.isNaN(temp[i])) {
                temp[i] = Math.sqrt(temp[i]);
            }
        }
        return temp;
    }

    /**
     * Returns the minimum for all columns. Will be {@link Double#NaN} for columns that only contain missing cells or
     * for empty data tables.<br>
     * Consider using {@link #getMinCells()} instead because this gives you the correct type and can distinguish between
     * NaN and missing cells.
     *
     * @return the minimum values
     */
    public double[] getMin() {
        return m_minValues.clone();
    }

    /**
     * Returns the minimum for all columns. Will be a missing cell for columns that only contain missing cells or
     * for empty data tables.
     *
     * @return the minimum values as {@link DataCell}s
     * @since 2.9
     */
    public DataCell[] getMinCells() {
        return m_minCells.clone();
    }

    /**
     * Returns the maximum for all columns. Will be {@link Double#NaN} for columns that only contain missing cells or
     * for empty data tables.<br>
     * Consider using {@link #getMinCells()} instead because this gives you the correct type and can distinguish between
     * NaN and missing cells.
     *
     * @return the maximum values
     */
    public double[] getMax() {
        return m_maxValues.clone();
    }

    /**
     * Returns the maximum for all columns. Will be a missing cell for columns that only contain missing cells or
     * for empty data tables.
     *
     * @return the maximum values as {@link DataCell}s
     * @since 2.9
     */
    public DataCell[] getMaxCells() {
        return m_maxCells.clone();
    }


    /**
     * Returns the minimum (not infinite) value selected column. Will be {@link Double#NaN} for columns that only
     * contain missing cells/{@link Double#NaN}/infinite values or for empty data tables.
     * <br>
     * Can be infinity if the node was not executed and an old array is loaded.
     *
     * @param col The column index.
     * @return the minimum value
     * @since 2.10
     */
    public DataCell getNonInfMin(final int col) {
        return m_minNonInfValues[col];
    }

    /**
     * Returns the maximum (not infinite) value selected column. Will be {@link Double#NaN} for columns that only
     * contain missing cells/{@link Double#NaN}/infinite values or for empty data tables.
     * <br>
     * Can be infinity if the node was not executed and an old array is loaded.
     *
     * @param col The column index.
     * @return the maximum value
     * @since 2.10
     */
    public DataCell getNonInfMax(final int col) {
        return m_maxNonInfValues[col];
    }

    /**
     * Returns the median for the desired column.
     *
     * @param colIdx the column index for which the median is calculated
     * @return median value
     */
    public double getMedian(final int colIdx) {
        return getValueOrNaN(m_median, colIdx);
    }

    /**
     * Returns the median for all columns.
     *
     * @return an array of median values with an item for each column
     */
    public double[] getMedian() {
        double[] result = new double[m_median.length];
        System.arraycopy(m_median, 0, result, 0, result.length);
        return result;
    }

    /**
     * Returns the skewness for the desired column.
     *
     * @param colIdx the column index for which the skewness is calculated
     * @return skewness value or {@link Double#NaN} if not yet computed.
     * @since 2.9
     */
    public double getSkewness(final int colIdx) {
        return getValueOrNaN(m_skewness, colIdx);
    }

    /**
     * @param array A double array.
     * @param colIdx index within the array.
     * @return the value in the {@code array} at {@code colIdx} position or {@link Double#NaN} if {@code array} is
     *         {@code null}.
     */
    private static double getValueOrNaN(final double[] array, final int colIdx) {
        if (array == null) {
            return Double.NaN;
        }
        return array[colIdx];
    }

    /**
     * Returns the skewness for all columns.
     *
     * @return an array of skewness values with an item for each column
     * @since 2.9
     */
    public double[] getSkewness() {
        return m_skewness.clone();
    }

    /**
     * Returns the kurtosis for the desired column.
     *
     * @param colIdx the column index for which the kurtosis is calculated
     * @return kurtosis value or {@link Double#NaN} if not yet computed.
     * @since 2.9
     */
    public double getKurtosis(final int colIdx) {
        return getValueOrNaN(m_kurtosis, colIdx);
    }

    /**
     * Returns the kurtosis for all columns.
     *
     * @return an array of kurtosis values with an item for each column
     * @since 2.9
     */
    public double[] getKurtosis() {
        return m_kurtosis.clone();
    }

    /**
     * @return number of rows in the original data table
     * @since 2.7
     */
    public int getRowCount() {
        return m_rowCount;
    }

    /**
     * Returns a map containing DataCell value to number of occurrences.
     *
     * @param colIdx column index to return map for
     * @return map of DataCell values to occurrences
     */
    public Map<DataCell, Integer> getNominalValues(final int colIdx) {
        if (m_nominalValues.get(colIdx) == null) {
            return null;
        }
        return Collections.unmodifiableMap(m_nominalValues.get(colIdx));
    }

    /**
     * Returns an array (for each column) of mappings containing DataCell value to number of occurrences.
     *
     * @return array of mappings of occurrences
     */
    public List<Map<DataCell, Integer>> getNominalValues() {
        List<Map<DataCell, Integer>> result = new ArrayList<Map<DataCell, Integer>>(m_nominalValues.size());
        for (int i = 0; i < m_nominalValues.size(); i++) {
            result.add(getNominalValues(i));
        }
        return result;
    }

    private Statistics3Table(final DataTableSpec spec, final DataCell[] minCells, final double[] minValues,
        final DataCell[] minNonInfValues, final DataCell[] maxCells, final double[] maxValues,
        final DataCell[] maxNonInfValues, final double[] meanValues, final double[] median, final double[] varianceValues,
        final double[] sum, final int[] missings, final int[] nans, final int[] posInfs, final int[] negInfs,
        final List<Map<DataCell, Integer>> nomValues, final double[] skewness, final double[] kurtosis,
        final int rowCount) {
        m_spec = spec;
        m_minCells = minCells;
        m_minValues = minValues;
        m_maxCells = maxCells;
        m_maxValues = maxValues;
        m_minNonInfValues = minNonInfValues;
        m_maxNonInfValues = maxNonInfValues;
        m_meanValues = meanValues;
        m_median = median;
        m_varianceValues = varianceValues;
        m_sum = sum;
        m_missingValueCnt = missings;
        m_nanValueCnt = nans;
        m_posInfinityValueCnt = posInfs;
        m_negInfinityValueCnt = negInfs;
        m_nominalValues = nomValues;
        m_warning = null;
        m_skewness = skewness;
        m_kurtosis = kurtosis;
        m_rowCount = rowCount;
    }

    /**
     * Returns warning message if number of possible values exceeds predefined maximum.
     *
     * @return null or a warning issued during construction time
     */
    public String getWarning() {
        return m_warning;
    }

    /**
     * Load a new statistic table by the given settings object.
     *
     * @param sett to load this table from
     * @return a new statistic table
     * @throws InvalidSettingsException if the settings are corrupt
     */
    public static Statistics3Table load(final NodeSettingsRO sett) throws InvalidSettingsException {
        DataTableSpec spec = DataTableSpec.load(sett.getConfig("spec"));
        List<Map<DataCell, Integer>> nominalValues = new ArrayList<Map<DataCell, Integer>>(spec.getNumColumns());
        for (int c = 0; c < spec.getNumColumns(); c++) {
            String name = spec.getColumnSpec(c).getName();
            if (!sett.containsKey(name)) {
                nominalValues.add(null);
            } else {
                nominalValues.add(c, new LinkedHashMap<DataCell, Integer>());
                NodeSettingsRO subSett = sett.getNodeSettings(name);
                for (String key : subSett.keySet()) {
                    NodeSettingsRO nomSett = subSett.getNodeSettings(key);
                    nominalValues.get(c).put(nomSett.getDataCell("key"), nomSett.getInt("value"));
                }
            }
        }
        DataCell[] minCells;
        double[] min;
        if (sett.containsKey("minimumCells")) { // since 2.9
            minCells = sett.getDataCellArray("minimumCells");
            min = new double[minCells.length];
            for (int i = 0; i < minCells.length; i++) {
                if (minCells[i].isMissing()) {
                    min[i] = Double.NaN;
                } else {
                    min[i] = ((DoubleValue) minCells[i]).getDoubleValue();
                }
            }
        } else { // until 2.8
            min = sett.getDoubleArray("minimum");
            minCells = new DataCell[min.length];
            for (int i = 0; i < min.length; i++) {
                minCells[i] = new DoubleCell(min[i]);
            }
        }

        DataCell[] maxCells;
        double[] max;
        if (sett.containsKey("maximumCells")) { // since 2.9
            maxCells = sett.getDataCellArray("maximumCells");
            max = new double[maxCells.length];
            for (int i = 0; i < maxCells.length; i++) {
                if (maxCells[i].isMissing()) {
                    max[i] = Double.NaN;
                } else {
                    max[i] = ((DoubleValue) maxCells[i]).getDoubleValue();
                }
            }
        } else { // until 2.8
            max = sett.getDoubleArray("maximum");
            maxCells = new DataCell[max.length];
            for (int i = 0; i < max.length; i++) {
                maxCells[i] = new DoubleCell(max[i]);
            }
        }


        //Use min and max to load compatible values
        DataCell[] minNonInf = sett.getDataCellArray("minimumNonInf", minCells);
        DataCell[] maxNonInf = sett.getDataCellArray("maximumNonInf", maxCells);
        double[] mean = sett.getDoubleArray("mean");
        double[] var = sett.getDoubleArray("variance");
        double[] median = sett.getDoubleArray("median");
        int[] missings = sett.getIntArray("missings");
        int[] unknownNumber = new int[min.length];
        Arrays.fill(unknownNumber, -1);
        int[] nans = sett.getIntArray("nans", unknownNumber);
        int[] posInfs = sett.getIntArray("posInfs", unknownNumber);
        int[] negInfs = sett.getIntArray("negInfs", unknownNumber);
        double[] sums = sett.getDoubleArray("sums");
        double[] unknownDouble = new double[min.length];
        Arrays.fill(unknownDouble, Double.NaN);
        double[] skewness = sett.getDoubleArray("skewness", unknownDouble);
        double[] kurtosis = sett.getDoubleArray("kurtosis", unknownDouble);
        // added with 2.7, fallback -1
        int rowCount = sett.getInt("row_count", -1);
        return new Statistics3Table(spec, minCells, min, minNonInf, maxCells, max, maxNonInf, mean, median, var, sums,
            missings, nans, posInfs, negInfs, nominalValues, skewness, kurtosis, rowCount);
    }

    /**
     * Saves this object to the given settings object.
     *
     * @param sett this object is saved to
     */
    public void save(final NodeSettingsWO sett) {
        m_spec.save(sett.addConfig("spec"));
        sett.addDataCellArray("minimumCells", m_minCells);
        sett.addDataCellArray("maximumCells", m_maxCells);
        sett.addDataCellArray("minimumNonInf", m_minNonInfValues);
        sett.addDataCellArray("maximumNonInf", m_maxNonInfValues);
        sett.addDoubleArray("mean", m_meanValues);
        sett.addDoubleArray("variance", m_varianceValues);
        sett.addDoubleArray("median", m_median);
        sett.addIntArray("missings", m_missingValueCnt);
        sett.addIntArray("nans", m_nanValueCnt);
        sett.addIntArray("posInfs", m_posInfinityValueCnt);
        sett.addIntArray("negInfs", m_negInfinityValueCnt);
        sett.addDoubleArray("sums", m_sum);
        sett.addDoubleArray("skewness", m_skewness);
        sett.addDoubleArray("kurtosis", m_kurtosis);
        sett.addInt("row_count", m_rowCount);
        for (int c = 0; c < m_nominalValues.size(); c++) {
            if (m_nominalValues.get(c) != null) {
                NodeSettingsWO subSett = sett.addNodeSettings(m_spec.getColumnSpec(c).getName());
                for (Map.Entry<DataCell, Integer> e : m_nominalValues.get(c).entrySet()) {
                    NodeSettingsWO nomSett = subSett.addNodeSettings(e.getKey().toString());
                    nomSett.addDataCell("key", e.getKey());
                    nomSett.addInt("value", e.getValue());
                }
            }
        }
    }

    /**
     * @return the statisticsSpecification
     * @since 2.9
     */
    public static DataTableSpec getStatisticsSpecification() {
        return STATISTICS_SPECIFICATION;
    }


    /** Updates the min and max value for an respective column. This method
     * does nothing if the min and max values don't need to be stored, e.g.
     * the column at hand contains string values.
     * @param col The column of interest.
     * @param cell The new value to check.
     */
    private void updateMinMax(final int col, final DataCell cell, final DataValueComparator comparator) {
        if (cell.isMissing()) {
            return;
        }
        DataCell value = handleNaN(cell instanceof BlobWrapperDataCell ? ((BlobWrapperDataCell)cell).getCell() : cell);
        if (value.isMissing()) {
            return;
        }

        if (m_minCells[col].isMissing() || (comparator.compare(value, m_minCells[col]) < 0)) {
            m_minCells[col] = value;
        }
        if (m_maxCells[col].isMissing() || (comparator.compare(value, m_maxCells[col]) > 0)) {
            m_maxCells[col] = value;
        }


        if (m_minNonInfValues[col].isMissing() || (!Double.isInfinite(((DoubleValue) value).getDoubleValue())
                && (comparator.compare(value, m_minNonInfValues[col]) < 0))) {
            m_minNonInfValues[col] = value;
        }
        if (m_maxNonInfValues[col].isMissing() || (!Double.isInfinite(((DoubleValue) value).getDoubleValue())
                && (comparator.compare(value, m_maxNonInfValues[col]) > 0))) {
            m_maxNonInfValues[col] = value;
        }
    }

    /*
     * Returns
     * - the cell if it is not a DoubleValue
     * - the cell if it is not NaN
     * - a missing cell if it is NaN
     */
    private static DataCell handleNaN(final DataCell cell) {
        if (cell.getType().isCompatible(DoubleValue.class)) {
            if (Double.isNaN(((DoubleValue) cell).getDoubleValue())) {
                return DataType.getMissingCell();
            } else {
                return cell;
            }
        } else {
            return cell;
        }
    }

    /**
     * @return the spec
     * @since 2.10
     */
    public DataTableSpec getSpec() {
        return m_spec;
    }
}
