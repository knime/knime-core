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
 * ------------------------------------------------------------------------
 *
 * History
 *   30.07.2025 (david): created
 */
package org.knime.core.util.binning.auto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.function.BiPredicate;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntFunction;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.data.extract.ArrayExtractors.MultipleColumnsExtractor;
import org.knime.core.data.v2.TableExtractorUtil;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.port.PortObject;
import org.knime.core.util.binning.auto.AutoBinningSettings.BinBoundary;
import org.knime.core.util.binning.auto.AutoBinningSettings.BinBoundaryExactMatchBehaviour;
import org.knime.core.util.binning.auto.AutoBinningSettings.BinNamingSettings;
import org.knime.core.util.binning.auto.AutoBinningSettings.BinningSettings;
import org.knime.core.util.binning.numeric.NumericBin2;
import org.knime.core.util.binning.numeric.PMMLPreprocDiscretizeTranslatorConfiguration;
import org.knime.core.util.binning.numeric.PMMLPreprocDiscretizeTranslatorConfiguration.ClosureStyle;
import org.knime.core.util.binning.numeric.PMMLPreprocDiscretizeTranslatorConfiguration.Interval;

import com.google.common.collect.Sets;

/**
 * Given various settings, creates bins.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 * @since 5.6
 *
 * @see AutoBinningPMMLApplier
 */
public class AutoBinningPMMLCreator {

    /**
     * Extracts the specified columns from the given data table and returns a map of column names to sorted lists of
     * double values. The values are extracted as doubles, and missing values are skipped.
     *
     * @param table the data table to extract the values from
     * @param exec execution context used for cancellation and progress reporting
     * @param colNames the names of the columns to extract; these must be numeric columns
     * @return a map where the keys are column names and the values are sorted lists of double values
     * @throws CanceledExecutionException if the execution is canceled during the extraction
     */
    public static Map<String, List<Double>> extractDataFromTableAndSort(final BufferedDataTable table,
        final ExecutionContext exec, final String... colNames) throws CanceledExecutionException {
        int[] columnIndices = Arrays.stream(colNames) //
            .mapToInt(table.getDataTableSpec()::findColumnIndex) //
            .toArray();

        var extractor = new MultipleColumnsExtractor<>( //
            Double.class, //
            columnIndices, //
            (row, i) -> row.isMissing(i) //
                ? null //
                : ((DoubleValue)row.getValue(i)).getDoubleValue() //
        );

        TableExtractorUtil.extractData(table, exec, extractor);

        return IntStream.range(0, columnIndices.length) //
            .collect( //
                HashMap<String, List<Double>>::new, // initial map
                (map, nextInt) -> { // accumulator
                    var values =
                        Arrays.stream(extractor.getOutput()[nextInt]).filter(Objects::nonNull).sorted().toList();
                    var columnIndex = columnIndices[nextInt];
                    var columnName = table.getDataTableSpec().getColumnNames()[columnIndex];

                    map.put(columnName, values);
                }, //
                (firstMap, secondMap) -> { // combine any two maps
                    if (!Sets.union(firstMap.keySet(), secondMap.keySet()).isEmpty()) {
                        throw new KNIMEException( //
                            "Column indices in the input data table are not unique. Please ensure that the column names are unique.")
                                .toUnchecked();
                    }
                    firstMap.putAll(secondMap); // merge the two maps
                } //
            );
    }

    /**
     * A simple record to hold the input data set for the binning operation. It contains the input column name, output
     * column name, sorted values for the column, and optional domain bounds. The values are expected to be already
     * sorted in ascending order, and the bounds are optional - if they are not provided, the minimum and maximum values
     * will be inferred from the data.
     *
     * @param inputName the name of the input column
     * @param outputName the name of the output column
     * @param sortedValues the list of sorted values for the column, which should be sorted in ascending order
     * @param lowerDomainBound an optional lower domain bound for the values; if not present, the minimum value from the
     *            sorted values will be used
     * @param upperDomainBound an optional upper domain bound for the values; if not present, the maximum value from the
     *            sorted values will be used
     */
    public static record InputDataSet( //
        String inputName, //
        String outputName, //
        List<Double> sortedValues, //
        OptionalDouble lowerDomainBound, //
        OptionalDouble upperDomainBound //
    ) {

    }

    /**
     * Creates the node output for the given data table based on the specified settings. The returned object is an array
     * of {@link PortObject} where the first element is a data table containing the binned values, and the second
     * element is a PMML port object containing the binning configuration. The return result can be directly returned
     * from the {@link NodeModel} execute method.
     *
     * @param sortedValues the input data sets, each one (usually) representing one column
     * @param exec the execution context to report progress and handle cancellation
     * @param settings the settings for the binning operation, including the columns to bin, the binning method, and
     *            other parameters like bounds and bin naming.
     * @return an array of {@link PortObject} where the first element is a data table containing the binned values, and
     *         the second element is a PMML port object containing the binning configuration.
     * @throws CanceledExecutionException if the execution is canceled during the binning process
     * @throws InvalidSettingsException if the settings are not valid for the input data table, such as if the input
     *             columns are not numeric.
     */
    public static PMMLPreprocDiscretizeTranslatorConfiguration createPMMLTranslatorConfiguration( //
        final List<InputDataSet> sortedValues, //
        final ExecutionContext exec, //
        final AutoBinningSettings settings //
    ) throws InvalidSettingsException, CanceledExecutionException {

        Map<String, List<BinBoundary>> edgesMap = new HashMap<>();

        for (var input : sortedValues) {
            List<BinBoundary> edges = createEdgesForInput(input, settings, exec);
            edgesMap.put(input.inputName(), edges);
        }

        var inputNames = sortedValues.stream() //
            .map(InputDataSet::inputName) //
            .toList();

        var outputNames = sortedValues.stream() //
            .map(InputDataSet::outputName) //
            .toList();

        return new PMMLPreprocDiscretizeTranslatorConfiguration( //
            inputNames, //
            outputNames, //
            createBins(edgesMap, settings.binNaming()) //
        ) //
            .withLowerOutlierBin(settings.boundsSettings().binNameForValuesOutsideLowerBound()) //
            .withUpperOutlierBin(settings.boundsSettings().binNameForValuesOutsideUpperBound());
    }

    private static List<BinBoundary> createEdgesForInput(final InputDataSet input, final AutoBinningSettings settings,
        final ExecutionContext exec) throws InvalidSettingsException, CanceledExecutionException {

        if (settings.binning() instanceof BinningSettings.EqualWidth ew) {
            var lowerBound = settings.boundsSettings().optLowerBound() //
                .orElse(input.lowerDomainBound() //
                    .orElseGet(() -> {
                        if (input.sortedValues().isEmpty()) {
                            throw new KNIMEException( //
                                "Cannot create equal width bins for empty input column: " + input.inputName()
                                    + ". Either add data or specify the bounds manually.").toUnchecked();
                        }
                        return input.sortedValues().get(0);
                    }));

            var upperBound = settings.boundsSettings().optUpperBound() //
                .orElse(input.upperDomainBound() //
                    .orElseGet(() -> {
                        if (input.sortedValues().isEmpty()) {
                            throw new KNIMEException( //
                                "Cannot create bins equal for empty input column: " + input.inputName()
                                    + ". Either add data or specify the bounds manually.").toUnchecked();
                        }
                        return input.sortedValues().get(input.sortedValues().size() - 1);
                    }));

            return createEdgesForEqualWidth( //
                lowerBound, //
                upperBound, //
                settings.integerBounds(), //
                ew.numBins() //
            );
        } else if (settings.binning() instanceof BinningSettings.EqualCount ec) {
            if (input.sortedValues().isEmpty()) {
                throw new KNIMEException( //
                    "Cannot create equal count bins for empty input column: " + input.inputName()).toUnchecked();
            }

            return createEdgesForEqualCount( //
                input.sortedValues(), //
                exec.createSubExecutionContext(0.1), //
                ec.numBins(), //
                settings.boundsSettings().optLowerBound(), //
                settings.boundsSettings().optUpperBound(), //
                settings.integerBounds() //
            );
        } else if (settings.binning() instanceof BinningSettings.FixedQuantiles fc) {
            if (input.sortedValues().isEmpty()) {
                throw new KNIMEException( //
                    "Cannot create quantile bins for empty input column: " + input.inputName()).toUnchecked();
            }

            return createEdgesFromQuantiles( //
                input.sortedValues(), //
                exec.createSilentSubExecutionContext(0.1), //
                Arrays.asList(fc.quantiles()) //
            );
        } else if (settings.binning() instanceof BinningSettings.FixedBoundaries fb) {
            return Arrays.asList(fb.boundaries());
        } else {
            throw new InvalidSettingsException("Unknown binning settings: " + settings.binning());
        }
    }

    /**
     * Calculates the bounds for a fixed-width binning. If integer bounds are requested, the bounds will be rounded to
     * integer values (specifically, the first bin boundary will be floored, and all others will be ceiled).
     *
     * Package scoped for testing purposes.
     *
     * @param singleColumnSpec the column spec for which to calculate the bounds. It should have a domain with bounds.
     * @param min the lower bound to use for the first bin boundary; if not present, the minimum value from the column
     *            domain
     * @param max the upper bound to use for the last bin boundary; if not present, the maximum value from the column
     *            domain
     * @param integerBounds if true, the bin boundaries will be rounded to integer values (the data type is still
     *            double)
     * @param numBins the number of bins to create
     * @return a list of bin boundaries, each with an exact match behaviour describing how to handle values that exactly
     *         match the boundary value.
     */
    static List<BinBoundary> createEdgesForEqualWidth( //
        final double min, //
        final double max, //
        final boolean integerBounds, //
        final int numBins //
    ) {
        // naive equally spaced boundaries
        var unroundedEdges = DoubleStream.concat( //
            IntStream.range(0, numBins).mapToDouble(i -> min + i / (double)numBins * (max - min)), //
            DoubleStream.of(max) //
        ).boxed().toList();

        IntFunction<DoubleUnaryOperator> roundingFunction = i -> i == 0 //
            ? Math::floor // first bin boundary is floored
            : Math::ceil; // all others are ceiled

        List<Double> edges = integerBounds //
            ? IntStream.range(0, unroundedEdges.size()) //
                .mapToObj(i -> roundingFunction.apply(i).applyAsDouble(unroundedEdges.get(i))) //
                .toList() //
            : unroundedEdges;

        return addMatchBehaviourToEdges(edges);
    }

    /**
     * This method is based on the 7th quantile algorithm in R, equivalent to the scipy <code>quantiles</code> method
     * with (a,b)=(1,1). See <a
     * href="https://en.wikipedia.org/wiki/Quantile#Estimating_quantiles_from_a_sample"}>WP:Quantile</a>.
     *
     * Package scoped for testing purposes.
     *
     * @param sortedColumn the sorted single-column data table to find the edges for, sorted ascending
     * @param exec the execution context to report progress and handle cancellation
     * @param sampleBoundaries the list of sample quantile bin boundaries to create edges for
     * @return a list of bin boundaries, each with an exact match behaviour describing how to handle values that exactly
     *         match the boundary value.
     * @throws CanceledExecutionException if the execution is canceled during the calculation
     */
    static List<BinBoundary> createEdgesFromQuantiles(final List<Double> sortedColumn, final ExecutionContext exec,
        final List<BinBoundary> sampleBoundaries) throws CanceledExecutionException {

        var edges = new ArrayList<Double>(sampleBoundaries.size());
        for (int i = 0; i < sampleBoundaries.size(); i++) {
            edges.add(Double.NaN); // placeholder for the edge value
        }

        long numRows = sortedColumn.size();
        long rowIndex = 0;
        int edgeIndex = 0;
        var iter = sortedColumn.iterator();
        Double rowQ = null;
        Double rowQ1 = null;

        if (iter.hasNext()) {
            rowQ1 = iter.next();
            rowQ = rowQ1;
        }

        for (var quantileBound : sampleBoundaries) {
            // Named h and h_floor to follow the notation in the WP article.
            double h = (numRows - 1) * quantileBound.value() + 1;
            int h_floor = (int)Math.floor(h); // NOSONAR naming convention is fine

            while ((1.0 == quantileBound.value() || rowIndex < h_floor) && iter.hasNext()) { // NOSONAR
                rowQ = rowQ1;
                rowQ1 = iter.next();
                rowIndex++;
                exec.setProgress(rowIndex / (double)numRows);
                exec.checkCanceled();
            }
            rowQ = 1.0 != quantileBound.value() ? rowQ : rowQ1; // NOSONAR
            // for quantile calculation see also
            // http://en.wikipedia.org/wiki/
            //                Quantile#Estimating_the_quantiles_of_a_population.
            // this implements R-7
            double xq = rowQ.doubleValue();
            double xq1 = rowQ1.doubleValue();
            double quantile = xq + (h - h_floor) * (xq1 - xq);
            edges.set(edgeIndex, quantile);
            edgeIndex++;
        }

        return IntStream.range(0, edges.size()) //
            .mapToObj(i -> new BinBoundary(edges.get(i), sampleBoundaries.get(i).exactMatchBehaviour())) //
            .toList();
    }

    /**
     * A simple record to hold a pair of integers, used for finding streak endpoints in the list of values.
     *
     * Package scoped for testing purposes.
     */
    record IntPair(int first, int second) {
    }

    /**
     * Finds the endpoints of a streak of values in the list, starting at the specified index.
     *
     * Package scoped for testing purposes.
     *
     * @param values the list of values to search through
     * @param beginning the index to start searching from
     * @param ceil if true, uses ceiling for comparisons; if false, uses exact equality. This is useful for integer
     *            bounds in bin creation.
     * @return a pair of indices representing the left and right endpoints of the streak. These are inclusive, so [0, 1,
     *         1, 1, 2] would give the endpoints [1, 3].
     */
    static IntPair findStreakEndpoints(final List<Double> values, final int beginning, final boolean ceil) {
        BiPredicate<Double, Double> maybeCeilEquals = ceil //
            ? (a, b) -> Math.ceil(a) == Math.ceil(b) // NOSONAR
            : (a, b) -> a.doubleValue() == b.doubleValue(); // NOSONAR

        var streakValue = values.get(beginning);

        int leftIndex = beginning;
        while (leftIndex > 0 && maybeCeilEquals.test(streakValue, values.get(leftIndex - 1))) {
            leftIndex--;
        }

        int rightIndex = beginning;
        while (rightIndex < values.size() - 1 && maybeCeilEquals.test(values.get(rightIndex + 1), streakValue)) {
            rightIndex++;
        }

        return new IntPair(leftIndex, rightIndex);
    }

    /**
     * Extract the given column to a list of doubles. Missing values will be skipped.
     *
     * Package scoped for testing purposes.
     *
     * @param table a table containing the column that should be extracted
     * @param colIndex the index of the column to extract
     * @param exec the execution context to report progress and handle cancellation
     * @return a list of double values extracted from the specified column
     * @throws CanceledExecutionException if the execution is canceled during the extraction
     */
    static List<Double> extractColumnValues(final BufferedDataTable table, final int colIndex,
        final ExecutionMonitor exec) throws CanceledExecutionException {
        List<Double> tableValues = new ArrayList<>();
        try (var it = table.iterator()) {
            while (it.hasNext()) {
                var cell = it.next().getCell(colIndex);

                if (cell.isMissing()) {
                    continue; // skip missing values
                }

                tableValues.add(((DoubleValue)cell).getDoubleValue());
                exec.checkCanceled();
            }
        }
        return tableValues;
    }

    /**
     * Finds the bin boundaries for a given number of bins, where each bin contains approximately the same number of
     * values. Note that the amounts of values in the bins may differ slightly, depending on the data - for example, the
     * method tries to avoid splitting identical values across different bins, and if integer cutoffs are enforced, the
     * number of values in the bins may vary even more.
     *
     * Package scoped for testing purposes.
     *
     * @param sortedSingleColumn the single-column data table to find the edges for. Should be sorted ascending.
     * @param exec the execution context to report progress and handle cancellation
     * @param binCount the number of bins to create
     * @param minValue if present, the minimum value to use for the first bin boundary; if not present, the minimum is
     *            calculated from the data.
     * @param maxValue if present, the maximum value to use for the last bin boundary; if not present, the maximum is
     *            calculated from the data.
     * @param integerBounds if true, the bin boundaries will be rounded to integer values (the data type is still
     *            double, but they will represent integers).
     * @return the list of bin boundaries, each with an exact match behaviour describing how to handle values that
     *         exactly match the boundary value.
     * @throws CanceledExecutionException if the execution is canceled during the calculation
     */
    static List<BinBoundary> createEdgesForEqualCount( //
        List<Double> tableValues, //
        final ExecutionContext exec, //
        final int binCount, //
        final OptionalDouble minValue, // NOSONAR optional is fine
        final OptionalDouble maxValue, // NOSONAR optional is fine
        final boolean integerBounds //
    ) throws CanceledExecutionException {

        // we need to filter the table values to remove anything outside the specified range
        tableValues = tableValues.stream() //
            .filter(val -> minValue.isEmpty() || val >= minValue.getAsDouble()) //
            .filter(val -> maxValue.isEmpty() || val <= maxValue.getAsDouble()) //
            .toList();

        DoubleUnaryOperator maybeCeil = integerBounds //
            ? Math::ceil //
            : DoubleUnaryOperator.identity();

        DoubleUnaryOperator maybeFloor = integerBounds //
            ? Math::floor //
            : DoubleUnaryOperator.identity();

        // this algorithm tries to find bin boundaries that give equal counts of values in each bin, but it prioritises
        // not splitting identical values across different bins. This can result in a fewer finite-width bins than
        // expected (and a bunch of zero-width bins at the end to keep the amount of boundaries as expected).
        var countPerBin = Math.toIntExact(Math.round(tableValues.size() / (double)binCount));
        var edges = new double[binCount + 1];
        edges[0] = minValue.orElse(maybeFloor.applyAsDouble(tableValues.get(0)));
        edges[edges.length - 1] = maxValue.orElse(maybeCeil.applyAsDouble(tableValues.get(tableValues.size() - 1)));

        int lowestPossibleNextBoundIndex = 0;
        int guessForNextBoundIndex = countPerBin - 1;
        for (int i = 1; i < edges.length - 1; i++) {
            exec.checkCanceled();

            if (guessForNextBoundIndex < tableValues.size()) {
                // find endpoints of the streak
                var endpoints = findStreakEndpoints(tableValues, guessForNextBoundIndex, integerBounds);
                int lowerIndex = Math.max(endpoints.first(), lowestPossibleNextBoundIndex) - 1;
                int higherIndex = endpoints.second();

                // shift the guess for the next bin boundary to the left or right, whichever
                // requires less shifting, to avoid splitting identical values across bins.
                int lowerDiff = -1 * (lowerIndex - lowestPossibleNextBoundIndex + 1 - countPerBin);
                int higherDiff = higherIndex - lowestPossibleNextBoundIndex + 1 - countPerBin;
                if (lowerIndex >= lowestPossibleNextBoundIndex && lowerDiff <= higherDiff) {
                    guessForNextBoundIndex = lowerIndex;
                } else {
                    guessForNextBoundIndex = higherIndex;
                }
                edges[i] = maybeCeil.applyAsDouble(tableValues.get(guessForNextBoundIndex));
                lowestPossibleNextBoundIndex = guessForNextBoundIndex + 1;
                guessForNextBoundIndex += countPerBin;
            } else {
                edges[i] = edges[i - 1];
            }
        }

        // assign sensible match behaviour to the edges and return
        return addMatchBehaviourToEdges(edges);
    }

    private static List<BinBoundary> addMatchBehaviourToEdges(final List<Double> edges) {
        return IntStream.range(0, edges.size()) //
            .mapToObj(i -> new BinBoundary(edges.get(i), //
                i == 0 //
                    ? BinBoundaryExactMatchBehaviour.TO_UPPER_BIN //
                    : BinBoundaryExactMatchBehaviour.TO_LOWER_BIN)) //
            .toList();
    }

    private static List<BinBoundary> addMatchBehaviourToEdges(final double[] edges) {
        return addMatchBehaviourToEdges(Arrays.stream(edges).boxed().toList());
    }

    /**
     * A simple record to hold a pair of double values, used for finding the minimum and maximum values for each column
     * in a data table.
     *
     * Package scoped for testing purposes.
     */
    static record DoublePair(double first, double second) {

        DoublePair withFirst(final double newFfirst) {
            return new DoublePair(newFfirst, this.second);
        }

        DoublePair withSecond(final double newSecond) {
            return new DoublePair(this.first, newSecond);
        }
    }

    /**
     * Creates intervals based on the specified edges.
     *
     * Package scoped for testing purposes.
     *
     * @param edgesMap a map where keys are column names and values are lists of bin boundaries
     * @param binNamingSettings the settings for naming the bins
     * @return a map where keys are column names and values are lists of bins created from the edges
     * @throws InvalidSettingsException if the edges map contains less than 2 edges for any column
     */
    static Map<String, List<NumericBin2>> createBins( //
        final Map<String, List<BinBoundary>> edgesMap, //
        final BinNamingSettings binNamingSettings //
    ) throws InvalidSettingsException {
        var discretizationsByColumnName = new HashMap<String, List<NumericBin2>>();

        for (var entry : edgesMap.entrySet()) {
            var targetColumn = entry.getKey();
            var edges = entry.getValue();

            if (edges.size() < 2) {
                // with less than 2 edges we can't make any bins
                throw new InvalidSettingsException( //
                    "Cannot create bins for column \"" + targetColumn + "\" with less than 2 edges.");
            }

            var bins = new ArrayList<NumericBin2>();

            for (int i = 0; i < edges.size() - 1; ++i) {
                var leftEdge = edges.get(i);
                var rightEdge = edges.get(i + 1);

                var closure = PMMLPreprocDiscretizeTranslatorConfiguration.ClosureStyle.from( //
                    leftEdge.exactMatchBehaviour() == BinBoundaryExactMatchBehaviour.TO_UPPER_BIN, //
                    rightEdge.exactMatchBehaviour() == BinBoundaryExactMatchBehaviour.TO_LOWER_BIN //
                );
                var binInterval = new PMMLPreprocDiscretizeTranslatorConfiguration.Interval( //
                    leftEdge.value(), rightEdge.value(), closure //
                );

                bins.add(binInterval.toNumericBin(binNamingSettings.computedName(i, leftEdge, rightEdge)));
            }

            discretizationsByColumnName.put(targetColumn, bins);
        }

        return discretizationsByColumnName;
    }

    /**
     * A {@link SingleCellFactory} that creates a cell for each row in the data table, containing the bin for each row
     * in the specified target column.
     */
    static class BinningCellFactory extends SingleCellFactory {

        private final int m_targetColumnIndex;

        private final List<NumericBin2> m_bins;

        private final String m_binNameForBelow;

        private final String m_binNameForAbove;

        BinningCellFactory( //
            final String newColName, //
            final int targetColumnIndex, //
            final List<NumericBin2> bins, //
            final String binNameForBelow, //
            final String binNameForAbove //
        ) {
            super(new DataColumnSpecCreator(newColName, StringCell.TYPE).createSpec());

            m_targetColumnIndex = targetColumnIndex;
            m_bins = bins;

            m_binNameForBelow = binNameForBelow;
            m_binNameForAbove = binNameForAbove;
        }

        @Override
        public DataCell getCell(final DataRow row) {
            var value = row.getCell(m_targetColumnIndex);

            if (value.isMissing()) {
                return value;
            }

            var valueAsDouble = ((DoubleValue)value).getDoubleValue();

            var firstMatchingBin = m_bins.stream() //
                .filter(bin -> bin.covers(value)) //
                .findFirst();

            if (firstMatchingBin.isEmpty()) {
                // find min left boundary of all bins, and max right boundary of all bins. Value should be outside
                // one or the other.

                var minNonOutlierInterval = m_bins.stream() //
                    .sorted(Comparator.comparingDouble(i -> i.getLeftValue())) //
                    .findFirst() //
                    .orElseThrow();

                var outlierLeft = new Interval( //
                    Double.NEGATIVE_INFINITY, minNonOutlierInterval.getLeftValue(), //
                    ClosureStyle.from(true, minNonOutlierInterval.isLeftOpen()) //
                );

                var maxNonOutlierInterval = m_bins.stream() //
                    .sorted(Comparator.comparingDouble(i -> -i.getRightValue())) //
                    .findFirst() // get the last one, which is the largest
                    .orElseThrow();

                var outlierRight = new Interval( //
                    maxNonOutlierInterval.getRightValue(), Double.POSITIVE_INFINITY, //
                    ClosureStyle.from(maxNonOutlierInterval.isRightOpen(), true) //
                );

                if (outlierLeft.covers(valueAsDouble)) {
                    return StringCellFactory.create(m_binNameForBelow);
                } else if (outlierRight.covers(valueAsDouble)) {
                    return StringCellFactory.create(m_binNameForAbove);
                } else {
                    throw new IllegalStateException("No bin found for value " + value + " in column "
                        + m_targetColumnIndex + ". " + "This is an implementation bug. Note: the outlier bins are "
                        + outlierRight + " and " + outlierLeft + ".");
                }
            } else {
                var bin = firstMatchingBin.get();
                return StringCellFactory.create(bin.getBinName());
            }
        }
    }

    /**
     * Checks if the given column specification is compatible with binning. Currently, only columns that are compatible
     * with {@link DoubleValue} are considered compatible for binning.
     *
     * @param colSpec the column specification to check for binning compatibility
     * @return true if the column can be binned, false otherwise
     */
    public static boolean columnCanBeBinned(final DataColumnSpec colSpec) {
        return colSpec.getType().isCompatible(DoubleValue.class);
    }
}
