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
 *   Aug 29, 2022 (Juan Diaz Baquero): created
 */
package org.knime.core.data.statistics;

import static org.knime.core.data.v2.RowReadUtil.readStringValue;
import static org.knime.core.data.v2.TableExtractorUtil.extractData;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.data.statistics.StatisticsExtractors.CentralMomentExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.CountMissingValuesExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.CountUniqueExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.DoubleSumExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.FirstQuartileExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.KurtosisExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.MaximumExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.MeanAbsoluteDeviation;
import org.knime.core.data.statistics.StatisticsExtractors.MeanExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.MedianExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.MinimumExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.QuantileExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.SkewnessExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.StandardDeviationExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.ThirdQuartileExtractor;
import org.knime.core.data.statistics.StatisticsExtractors.VarianceExtractor;
import org.knime.core.data.v2.RowRead;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.Pair;

/**
 * Compute univariate statistics for a given double or string column.
 *
 * @author Juan Diaz Baquero
 * @author Benjamin Moser, KNIME GmbH, Konstanz, Germany
 * @since 5.1
 * @noreference Not public API, for internal use only.
 */
public final class UnivariateStatistics {

    /**
     * Labels used for every column on the view data
     */
    private static final EnumSet<Statistic> DEFAULT_EXCLUDED_STATISTICS =
        EnumSet.of(Statistic.QUANTILE_1, Statistic.QUANTILE_5, Statistic.QUANTILE_10, Statistic.QUANTILE_90,
            Statistic.QUANTILE_95, Statistic.QUANTILE_99, Statistic.VARIANCE, Statistic.SKEWNESS, Statistic.KURTOSIS);

    private String m_name;

    private String m_type;

    private long m_numberUniqueValues;

    private long m_numberMissingValues;

    private String m_firstValue;

    private String m_lastValue;

    private String m_commonValues;

    private Double[] m_quantiles = new Double[9];

    private Double m_min;

    private Double m_max;

    private Optional<Double> m_mean = Optional.empty();

    private Optional<Double> m_meanAbsoluteDeviation = Optional.empty();

    private Optional<Double> m_standardDeviation = Optional.empty();

    private Optional<Double> m_variance = Optional.empty();

    private Optional<Double> m_skewness = Optional.empty();

    private Optional<Double> m_kurtosis = Optional.empty();

    private Optional<Double> m_sum = Optional.empty();

    private UnivariateStatistics() {
        // Utility class
    }

    String getName() {
        return m_name;
    }

    private void setName(final String name) {
        m_name = name;
    }

    String getType() {
        return m_type;
    }

    private void setType(final DataType type) {
        m_type = type.toPrettyString();
    }

    long getNumberUniqueValues() {
        return m_numberUniqueValues;
    }

    private void setNumberUniqueValues(final long numberUniqueValues) {
        m_numberUniqueValues = numberUniqueValues;
    }

    long getNumberMissingValues() {
        return m_numberMissingValues;
    }

    private void setNumberMissingValues(final long numberMissingValues) {
        m_numberMissingValues = numberMissingValues;
    }

    String getFirstValue() {
        return m_firstValue;
    }

    private void setFirstValue(final String firstValue) {
        m_firstValue = firstValue;
    }

    String getLastValue() {
        return m_lastValue;
    }

    private void setLastValue(final String lastValue) {
        m_lastValue = lastValue;
    }

    String getCommonValues() {
        return m_commonValues;
    }

    private void setCommonValues(final String[] items) {
        m_commonValues = items.length == 0 ? null : String.join(", ", items);
    }

    Double[] getQuantiles() {
        return m_quantiles;
    }

    private void setQuantiles(final Double[] quantiles) {
        m_quantiles = quantiles;
    }

    Double getMin() {
        return m_min;
    }

    private void setMin(final Double min) {
        m_min = min;
    }

    Double getMax() {
        return m_max;
    }

    private void setMax(final Double max) {
        m_max = max;
    }

    Optional<Double> getMean() {
        return m_mean;
    }

    private void setMean(final double mean) {
        m_mean = Double.isNaN(mean) ? Optional.empty() : Optional.of(mean);
    }

    Optional<Double> getMeanAbsoluteDeviation() {
        return m_meanAbsoluteDeviation;
    }

    private void setMeanAbsoluteDeviation(final double meanAbsoluteDeviation) {
        m_meanAbsoluteDeviation =
            Double.isNaN(meanAbsoluteDeviation) ? Optional.empty() : Optional.of(meanAbsoluteDeviation);
    }

    Optional<Double> getStandardDeviation() {
        return m_standardDeviation;
    }

    private void setStandardDeviation(final double standardDeviation) {
        m_standardDeviation = Double.isNaN(standardDeviation) ? Optional.empty() : Optional.of(standardDeviation);

    }

    Optional<Double> getVariance() {
        return m_variance;
    }

    private void setVariance(final double variance) {
        m_variance = Double.isNaN(variance) ? Optional.empty() : Optional.of(variance);
    }

    Optional<Double> getSkewness() {
        return m_skewness;
    }

    private void setSkewness(final double skewness) {
        m_skewness = Double.isNaN(skewness) ? Optional.empty() : Optional.of(skewness);
    }

    Optional<Double> getKurtosis() {
        return m_kurtosis;
    }

    private void setKurtosis(final double kurtosis) {
        m_kurtosis = Double.isNaN(kurtosis) ? Optional.empty() : Optional.of(kurtosis);
    }

    Optional<Double> getSum() {
        return m_sum;
    }

    private void setSum(final double sum) {
        m_sum = Double.isNaN(sum) ? Optional.empty() : Optional.of(sum);
    }

    /**
     * Compute statistics for every column in the input table.
     *
     * @param inputTable The table for whose columns to compute statistics
     * @param exec Execution context
     * @param selectedStatistics The statistics to include
     * @return A table in which each row corresponds to statistics about a column in the input table
     * @throws CanceledExecutionException If cancelled
     */
    public static BufferedDataTable computeStatisticsTable(final BufferedDataTable inputTable,
        final ExecutionContext exec, final Collection<Statistic> selectedStatistics) throws CanceledExecutionException {
        var allColumns = inputTable.getSpec().getColumnNames();
        return computeStatisticsTable(inputTable, allColumns, exec, selectedStatistics);
    }

    /**
     * /** Compute statistics for selected columns in the input table.
     *
     * @param inputTable The table for whose columns to compute statistics
     * @param selectedColumns The column names of the input table for which to compute statistics
     * @param executionContext Execution context
     * @param selectedStatistics The statistics to include
     * @return A table in which each row corresponds to statistics about a selected column in the input table
     * @throws CanceledExecutionException If cancelled
     */
    public static BufferedDataTable computeStatisticsTable(final BufferedDataTable inputTable,
        final String[] selectedColumns, final ExecutionContext executionContext,
        final Collection<Statistic> selectedStatistics)
        throws CanceledExecutionException {
        final var eligibleCols = Arrays.stream(selectedColumns)//
            .filter(name -> {
                var type = inputTable.getDataTableSpec().getColumnSpec(name).getType();
                return type.isCompatible(DoubleValue.class) || type.isCompatible(StringValue.class);
            })//
            .toArray(String[]::new);

        // trivial case -- nothing to do
        final var statisticsTable = executionContext.createDataContainer(getStatisticsTableSpec(selectedStatistics));
        if (eligibleCols.length == 0) {
            statisticsTable.close();
            return statisticsTable.getTable();
        }

        // compute statistics for each column individually
        final var selectedColumnTables =
            StatisticsTableUtil.splitTableByColumnNames(inputTable, eligibleCols, true, true, executionContext);
        final var selectedColumnTablesWithMissingValues =
            StatisticsTableUtil.splitTableByColumnNames(inputTable, eligibleCols, false, true, executionContext);
        for (var columnName : eligibleCols) {
            final var allColumnStatistics = new UnivariateStatistics();
            final var sortedTable =
                BufferedDataTableSorter.sortTable(selectedColumnTables.get(columnName), 0, executionContext);
            final var tableWithMissingValues = selectedColumnTablesWithMissingValues.get(columnName);
            allColumnStatistics.performStatisticsCalculationForAllColumns(sortedTable, executionContext);
            boolean isStringColumn = sortedTable.getSpec().getColumnSpec(0).getType().isCompatible(StringValue.class);
            if (!isStringColumn) {
                allColumnStatistics.performStatisticsCalculationForNumericColumns(sortedTable, executionContext);
            }
            allColumnStatistics.performMissingValuesComputation(tableWithMissingValues, executionContext);
            statisticsTable.addRowToTable(StatisticsTableUtil.createTableRow(allColumnStatistics, selectedStatistics));
        }
        statisticsTable.close();

        return statisticsTable.getTable();
    }

    /**
     * Given some selected statistics, it returns the table specification of the resulting statistics table.
     *
     * @param selectedStatistics The selected statistics to include
     * @return The table specification for the selected statistics columns
     */
    public static DataTableSpec getStatisticsTableSpec(final Collection<Statistic> selectedStatistics) {
        var colSpecs = Arrays.stream(Statistic.values()) //
            .filter(selectedStatistics::contains) // to preserve original order of enum
            .map(statistic -> new DataColumnSpecCreator(statistic.getName(), statistic.getType()).createSpec()) //
            .toArray(DataColumnSpec[]::new);
        return new DataTableSpec(colSpecs);
    }

    /**
     * Given a single-column table of strings, compute a single-column table of their lengths
     *
     * @param stringTable The input strings
     * @param exec The execution context
     * @return A table of string lengths.
     */
    static BufferedDataTable getStringLengths(final BufferedDataTable stringTable, final ExecutionContext exec) {
        final var spec = stringTable.getSpec();
        final var container = exec.createDataContainer(
            new DataTableSpec(new DataColumnSpecCreator(spec.getColumnNames()[0], IntCell.TYPE).createSpec()));
        try (final var readCursor = stringTable.cursor()) {
            while (readCursor.canForward()) {
                final var row = readCursor.forward();
                final var value = ((StringValue)row.getValue(0)).getStringValue();
                container.addRowToTable(new DefaultRow(row.getRowKey().getString(), value.length()));
            }
        }
        container.close();
        return container.getTable();
    }

    /**
     * Given a single-column table of strings, obtain the first and last string in the table.
     *
     * @param sortedTable The input strings.
     * @return A pair of first and last string in the input table.
     */
    static Pair<String, String> getFirstAndLastString(final BufferedDataTable sortedTable) {
        try (final var readCursor = sortedTable.cursor()) {
            String first = null;
            String last = null;
            if (readCursor.canForward()) {
                first = readStringValue(readCursor.forward(), 0);
            }
            RowRead row = null;
            while (readCursor.canForward()) {
                row = readCursor.forward();
            }
            if (row != null) {
                last = readStringValue(row, 0);
            }
            return new Pair<>(first, last);
        }
    }

    /**
     * @return The default statistics
     */
    public static List<Statistic> getDefaultStatistics() {
        return Arrays.stream(Statistic.values()).filter(stat -> !DEFAULT_EXCLUDED_STATISTICS.contains(stat)).toList();
    }

    /**
     * @return The labels of the default statistics
     */
    public static String[] getDefaultStatisticsLabels() {
        return getLabelsFromStatistics(getDefaultStatistics());
    }

    /**
     * @return All available statistics
     */
    public static List<Statistic> getAvailableStatistics() {
        return Arrays.stream(Statistic.values()).toList();
    }

    /**
     * @return The labels of all available statistics
     */
    public static String[] getAvailableStatisticsLabels() {
        return getLabelsFromStatistics(getAvailableStatistics());
    }

    private static String[] getLabelsFromStatistics(final Collection<Statistic> statistics) {
        return statistics.stream().map(Statistic::getName).toArray(String[]::new);
    }

    /**
     * String-format a list of pairs of data value and absolute frequencies.
     *
     * @param mostFrequentValues A list of pairs of some unique data values and their respective absolute frequencies.
     * @param type The type of the data values.
     * @param totalNumValues The total number of unique values
     * @return A string of shape "<value> (<absolute-count>; <percentage-count>)"
     */
    static String[] formatMostFrequentValues(final List<Pair<DataValue, Long>> mostFrequentValues, final DataType type,
        final long totalNumValues) {
        Function<DataValue, String> reader;
        if (type.isCompatible(DoubleValue.class)) {
            reader = dv -> DataValueRendererUtils.formatDouble(((DoubleValue)dv).getDoubleValue());
        } else if (type.isCompatible(StringValue.class)) {
            reader = dv -> ((StringValue)dv).getStringValue();
        } else if (type.isCompatible(CollectionDataValue.class)) {
            reader = dv -> ((CollectionDataValue)dv).stream().map(DataCell::toString)
                .collect(Collectors.joining(", ", "[", "]"));
        } else {
            reader = Object::toString;
        }

        return mostFrequentValues.stream().map(pair -> {
            var value = pair.getFirst();
            var valueReadable = value == null ? "?" : reader.apply(value);
            var absCount = pair.getSecond();
            return String.format("%s (%s; %s)", valueReadable, absCount,
                DataValueRendererUtils.formatPercentage(absCount / (double)totalNumValues));
        }).toArray(String[]::new);
    }

    private void performMissingValuesComputation(final BufferedDataTable inputColumnTable,
        final ExecutionContext executionContext) throws CanceledExecutionException {
        var columnIndex = 0;

        final var missingValuesExtractor = new CountMissingValuesExtractor(columnIndex);
        extractData(inputColumnTable, executionContext, missingValuesExtractor);
        setNumberMissingValues(missingValuesExtractor.getOutput());
    }

    /**
     * Compute statistics for the given input column that will be computed for every type.
     *
     * @param inputColumnTable A table containing exactly one column of either {@link DoubleValue} or
     *            {@link StringValue}.
     * @param executionContext The current execution context
     */
    private void performStatisticsCalculationForAllColumns(final BufferedDataTable inputColumnTable,
        final ExecutionContext executionContext) throws CanceledExecutionException {
        var columnIndex = 0;
        var type = inputColumnTable.getSpec().getColumnSpec(columnIndex).getType();
        setName(inputColumnTable.getSpec().getColumnSpec(columnIndex).getName());
        setType(type);

        // for unique values, always consider raw values
        final var countUniqueExtractor = new CountUniqueExtractor();
        extractData(inputColumnTable, executionContext, countUniqueExtractor);
        setNumberUniqueValues(countUniqueExtractor.getNumberOfUniqueValues());
        setCommonValues(formatMostFrequentValues(countUniqueExtractor.getMostFrequentValues(10),
            inputColumnTable.getSpec().getColumnSpec(columnIndex).getType(), inputColumnTable.size()));
    }

    /**
     * Compute statistics for the given numeric input column.
     *
     * @param sortedInputColumnTable A table containing exactly one numeric column compatible with type
     *            {@link DoubleValue}.
     * @param executionContext The current execution context
     */
    private void performStatisticsCalculationForNumericColumns(final BufferedDataTable sortedInputColumnTable,
        final ExecutionContext executionContext) throws CanceledExecutionException {
        var columnIndex = 0;

        final var minExtractor = new MinimumExtractor(columnIndex);
        final var maxExtractor = new MaximumExtractor(columnIndex);
        final var qExtractors = new QuantileExtractor[9];
        qExtractors[0] = new QuantileExtractor(columnIndex, 1, 100); // 1%
        qExtractors[1] = new QuantileExtractor(columnIndex, 1, 20); // 5%
        qExtractors[2] = new QuantileExtractor(columnIndex, 1, 10); // 10%
        qExtractors[3] = new FirstQuartileExtractor(columnIndex); // 25%
        qExtractors[4] = new MedianExtractor(columnIndex); // 50%
        qExtractors[5] = new ThirdQuartileExtractor(columnIndex); // 75%
        qExtractors[6] = new QuantileExtractor(columnIndex, 9, 10); // 90%
        qExtractors[7] = new QuantileExtractor(columnIndex, 19, 20); // 95%
        qExtractors[8] = new QuantileExtractor(columnIndex, 99, 100); // 99%
        final var meanExtractor = new MeanExtractor(columnIndex);
        final var sumExtractor = new DoubleSumExtractor(columnIndex);

        // apply extractors
        extractData(sortedInputColumnTable, executionContext, minExtractor, maxExtractor, meanExtractor, sumExtractor,
            qExtractors[0], qExtractors[1], qExtractors[2], qExtractors[3], qExtractors[4], qExtractors[5],
            qExtractors[6], qExtractors[7], qExtractors[8]);

        final var mean = meanExtractor.getOutput();
        setMean(mean);
        setSum(sortedInputColumnTable.size() == 0 ? Double.NaN : sumExtractor.getOutput());
        setQuantiles(Stream.of(qExtractors).map(QuantileExtractor::getOutput).toArray(Double[]::new));

        setMin(minExtractor.getOutput());
        setMax(maxExtractor.getOutput());

        // further extractors whose initialisation depends on previous results
        final var meanAbsoluteDeviationExtractor = new MeanAbsoluteDeviation(columnIndex, mean);
        final var standardDeviationExtractor = new StandardDeviationExtractor(columnIndex, mean);
        final var biasedVariance = new CentralMomentExtractor(0, mean, 2);
        final var varianceExtractor = new VarianceExtractor(columnIndex, mean);

        extractData(sortedInputColumnTable, executionContext, meanAbsoluteDeviationExtractor,
            standardDeviationExtractor, varianceExtractor, biasedVariance);

        final var stdDeviation = standardDeviationExtractor.getOutput();
        setMeanAbsoluteDeviation(meanAbsoluteDeviationExtractor.getOutput());
        setStandardDeviation(stdDeviation);
        setVariance(varianceExtractor.getOutput());

        final var skewnessExtractor = new SkewnessExtractor(columnIndex, mean, stdDeviation);
        final var kurtosisExtractor = new KurtosisExtractor(columnIndex, mean, biasedVariance.getOutput());

        extractData(sortedInputColumnTable, executionContext, skewnessExtractor, kurtosisExtractor);

        setSkewness(skewnessExtractor.getOutput());
        setKurtosis(kurtosisExtractor.getOutput());
    }

    public enum Statistic {
            NAME("Name", StringCell.TYPE), TYPE("Type", StringCell.TYPE),
            NUMBER_MISSING_VALUES("# Missing values", LongCell.TYPE),
            NUMBER_UNIQUE_VALUES("# Unique values", LongCell.TYPE), MINIMUM("Minimum", DoubleCell.TYPE),
            MAXIMUM("Maximum", DoubleCell.TYPE), QUANTILE_1("1% Quantile", DoubleCell.TYPE),
            QUANTILE_5("5% Quantile", DoubleCell.TYPE), QUANTILE_10("10% Quantile", DoubleCell.TYPE),
            QUANTILE_25("25% Quantile", DoubleCell.TYPE), QUANTILE_50("50% Quantile (Median)", DoubleCell.TYPE),
            QUANTILE_75("75% Quantile", DoubleCell.TYPE), QUANTILE_90("90% Quantile", DoubleCell.TYPE),
            QUANTILE_95("95% Quantile", DoubleCell.TYPE), QUANTILE_99("99% Quantile", DoubleCell.TYPE),
            MEAN("Mean", DoubleCell.TYPE), MEAN_ABSOLUTE_DEVIATION("Mean Absolute Deviation", DoubleCell.TYPE),
            STD_DEVIATION("Standard Deviation", DoubleCell.TYPE), SUM("Sum", DoubleCell.TYPE),
            VARIANCE("Variance", DoubleCell.TYPE), K_MOST_COMMON("10 most common values", StringCell.TYPE),
            SKEWNESS("Skewness", DoubleCell.TYPE), KURTOSIS("Kurtosis", DoubleCell.TYPE);

        private final String m_name;

        private final DataType m_type;

        Statistic(final String name, final DataType type) {
            this.m_name = name;
            this.m_type = type;
        }

        public String getName() {
            return m_name;
        }

        public DataType getType() {
            return m_type;
        }
    }
}
