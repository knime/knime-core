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
package org.knime.core.util.binning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.BiPredicate;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntFunction;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.binning.BinningSettings.BinBoundary;
import org.knime.core.util.binning.BinningSettings.BinBoundary.BinBoundaryExactMatchBehaviour;
import org.knime.core.util.binning.BinningSettings.BinNamingScheme;
import org.knime.core.util.binning.BinningSettings.BinningMethod;
import org.knime.core.util.binning.BinningSettings.DataBounds;
import org.knime.core.util.binning.numeric.NumericBin;

/**
 * Given various settings, creates bins.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 * @since 5.8
 *
 * @see BinningPMMLApplyUtil
 */
public final class BinningUtil {

    private BinningUtil() {
        // Utility class, no instances allowed
    }

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
        final var extractedValues = extractValues(table, colNames, exec);
        return indexAndSortValues(extractedValues, colNames, exec);
    }

    private static List<List<Double>> extractValues(final BufferedDataTable table, final String[] colNames,
        final ExecutionContext exec) throws CanceledExecutionException {
        int[] columnIndices = Arrays.stream(colNames) //
            .mapToInt(table.getDataTableSpec()::findColumnIndex) //
            .toArray();
        List<List<Double>> extractedValues = IntStream.range(0, columnIndices.length) //
            .mapToObj(i -> (List<Double>)new ArrayList<Double>()) //
            .toList();

        try (var cursor = table.cursor()) {
            while (cursor.canForward()) {
                exec.checkCanceled();
                final var row = cursor.forward();
                for (int i = 0; i < columnIndices.length; i++) {
                    final var colIdx = columnIndices[i];
                    if (row.isMissing(colIdx)) {
                        continue;
                    }
                    final var doubleValue = ((DoubleValue)row.getValue(colIdx)).getDoubleValue();
                    extractedValues.get(i).add(doubleValue);
                }
            }
        }
        return extractedValues;
    }

    private static Map<String, List<Double>> indexAndSortValues(final List<List<Double>> extractedValues,
        final String[] colNames, final ExecutionContext exec) throws CanceledExecutionException {
        final var map = new HashMap<String, List<Double>>(colNames.length);
        for (int i = 0; i < colNames.length; i++) {
            exec.checkCanceled();
            final var sortedValues = extractedValues.get(i).stream().sorted().toList();
            map.put(colNames[i], sortedValues);
        }
        return map;
    }

    /**
     * Creates bins from the sorted values for a single column, using the specified settings. The sorted values
     *
     * @param sortedValues the sorted values for the column, which should be sorted in ascending order. If the list does
     *            not yield at least two edges, an exception is thrown.
     * @param exec the execution context to report progress and handle cancellation
     * @param binningMethod the binning settings, which define how the bins should be created, e.g. equal width or a
     *            custom list of quantiles (amongst others).
     * @param binNamingScheme the naming scheme for the bins, e.g. numbered, by borders or midpoints, along with how any
     *            doubles in the bin names should be formatted.
     * @param dataBoundsSettings the settings for the data bounds, e.g. if there should be some fixed bounds beyond
     *            which
     * @param domainBounds an optional lower and/or upper domain bound for the values; Used in case the binning settings
     *            require bounds. Only used when the respective bound in dataBoundsSettings is not specified.
     * @return a list of {@link NumericBin} representing the bins created from the sorted values.
     * @throws IllegalArgumentException if the sorted values are empty or if they do not allow for creating bins.
     * @throws CanceledExecutionException if the execution is canceled during the binning process.
     */
    public static List<NumericBin> createBinsFromSortedValues(final List<Double> sortedValues, //
        final ExecutionContext exec, //
        final BinningMethod binningMethod, //
        final BinNamingScheme binNamingScheme, //
        final DataBounds dataBoundsSettings, //
        final DataBounds domainBounds //
    ) throws IllegalArgumentException, CanceledExecutionException {
        if (sortedValues.isEmpty() && !(binningMethod instanceof BinningMethod.FixedBoundaries)) {
            throw new IllegalArgumentException("Cannot create bins for empty input values");
        }
        List<BinBoundary> edges =
            createEdgesForInput(sortedValues, domainBounds, binningMethod, dataBoundsSettings, exec);
        if (edges.size() < 2) {
            // with less than 2 edges we can't make any bins
            throw new IllegalArgumentException("Cannot create bins with less than 2 edges.");
        }
        return createBins(edges, binNamingScheme);
    }

    private static List<BinBoundary> createEdgesForInput(final List<Double> sortedValues, final DataBounds domainBounds,
        final BinningMethod binningMethod, final DataBounds dataBoundsSettings, final ExecutionContext exec)
        throws CanceledExecutionException {
        if (sortedValues.isEmpty()) {
            throw new IllegalArgumentException("Cannot create bins for empty input values.");
        }

        if (binningMethod instanceof BinningMethod.EqualWidth ew) {

            validateLowerUpperBounds(dataBoundsSettings, domainBounds, sortedValues);

            var lowerBound = dataBoundsSettings.optLowerBound() //
                .orElseGet(() -> domainBounds.optLowerBound() //
                    .orElseGet(() -> sortedValues.get(0)));

            final var upperBound = dataBoundsSettings.optUpperBound() //
                .orElseGet(() -> domainBounds.optUpperBound() //
                    .orElseGet(() -> sortedValues.get(sortedValues.size() - 1)));

            return createEdgesForEqualWidth( //
                lowerBound, //
                upperBound, //
                ew.integerBounds(), //
                ew.numBins() //
            );
        } else if (binningMethod instanceof BinningMethod.EqualCount ec) {
            return createEdgesForEqualCount( //
                sortedValues, //
                exec.createSubExecutionContext(0.1), //
                ec.numBins(), //
                dataBoundsSettings, ec.integerBounds() //
            );
        } else if (binningMethod instanceof BinningMethod.FixedQuantiles fc) {

            return createEdgesFromQuantiles( //
                sortedValues, //
                exec.createSilentSubExecutionContext(0.1), //
                Arrays.asList(fc.quantiles()), //
                dataBoundsSettings, fc.integerBounds());
        } else if (binningMethod instanceof BinningMethod.FixedBoundaries fb) {
            return Arrays.asList(fb.boundaries());
        } else {
            throw new IllegalStateException("Unknown binning settings: " + binningMethod);
        }
    }

    /**
     * That when both min and max are provided in dataBoundsSettings the max is greater or equal to the min can be
     * checked in a static validation beforehand already. It is included here for completeness. But if only one of them
     * is provided, we also need to make sure that it fits to the domain bounds or data bounds, i.e. a provided min must
     * not be greater than the maximum of the domain/data and vice versa.
     *
     * @param dataBoundsSettings
     * @param domainBounds
     * @param sortedValues
     */
    private static void validateLowerUpperBounds(final DataBounds dataBoundsSettings, final DataBounds domainBounds,
        final List<Double> sortedValues) {

        final var minValue = dataBoundsSettings.optLowerBound();
        final var maxValue = dataBoundsSettings.optUpperBound();
        if (minValue.isPresent() && maxValue.isPresent()) {
            CheckUtils.checkArgument(minValue.getAsDouble() <= maxValue.getAsDouble(),
                "The provided upper bound (%f) must be greater than or equal to the lower bound (%f).", //
                maxValue.getAsDouble(), minValue.getAsDouble());
        }
        if (minValue.isPresent()) {
            final var maxFromData = domainBounds.optUpperBound() //
                .orElseGet(() -> sortedValues.get(sortedValues.size() - 1));
            CheckUtils.checkArgument(minValue.getAsDouble() <= maxFromData,
                "The fixed lower bound (%f) must be greater than or equal to the lower bound of the data (%f).", //
                minValue.getAsDouble(), domainBounds.optLowerBound().getAsDouble());
        }
        if (maxValue.isPresent()) {
            final var minFromData = domainBounds.optLowerBound() //
                .orElseGet(() -> sortedValues.get(0));
            CheckUtils.checkArgument(maxValue.getAsDouble() >= minFromData,
                "The fixed upper bound (%f) must be less than or equal to the upper bound of the data(%f).", //
                maxValue.getAsDouble(), domainBounds.optUpperBound().getAsDouble());
        }

    }

    /**
     * Calculates the bounds for a fixed-width binning. If integer bounds are requested, the bounds will be rounded to
     * integer values (specifically, the first bin boundary will be floored, and all others will be ceiled).
     *
     * Package scoped for testing purposes.
     *
     * @param min the lower bound to use for the first bin boundary; if not present, the minimum value from the column
     *            domain
     * @param max the upper bound to use for the last bin boundary; if not present, the maximum value from the column
     *            domain
     * @param integerBounds if true, the bin boundaries will be rounded to integer values (the data type is still
     *            double)
     * @param numBins the number of bins to create
     * @return a list of bin boundaries, each with an exact match behaviour describing how to handle values that exactly
     *         match the boundary value.
     * @since 5.10
     */
    public static List<BinBoundary> createEdgesForEqualWidth( //
        final double min, //
        final double max, //
        final boolean integerBounds, //
        final int numBins //
    ) {

        CheckUtils.checkState(max >= min,
            "The provided maximum value (%f) must be greater than or equal to the minimum value (%f) for creating equal width bins.", //
            max, min);
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
     * @param dataBounds to filter the data by before calculating the edges.
     * @param integerBounds whether the computed edges should be rounded to integer values. If true, this can lead to
     *            less than the requested number of edges, as some edges may be rounded to the same value.
     * @return a list of bin boundaries, each with an exact match behaviour describing how to handle values that exactly
     *         match the boundary value.
     * @throws CanceledExecutionException if the execution is canceled during the calculation
     */
    static List<BinBoundary> createEdgesFromQuantiles(List<Double> sortedColumn, final ExecutionContext exec,
        final List<BinBoundary> sampleBoundaries, final DataBounds dataBounds, final boolean integerBounds)
        throws CanceledExecutionException {

        final var minValue = dataBounds.optLowerBound();
        final var maxValue = dataBounds.optUpperBound();

        sortedColumn = filterColumnValues(sortedColumn, minValue, maxValue);

        var edges = new ArrayList<Double>(sampleBoundaries.size());
        for (int i = 0; i < sampleBoundaries.size(); i++) {
            edges.add(Double.NaN); // placeholder for the edge value
        }

        DoubleUnaryOperator maybeCeil = integerBounds //
            ? Math::ceil //
            : DoubleUnaryOperator.identity();

        DoubleUnaryOperator maybeFloor = integerBounds //
            ? Math::floor //
            : DoubleUnaryOperator.identity();

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
            if (edgeIndex == 0) {
                quantile = maybeFloor.applyAsDouble(quantile); // first edge is floored
            } else {
                quantile = maybeCeil.applyAsDouble(quantile); // all others are ceiled
            }
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
        List<Double> sortedColumn, //
        final ExecutionContext exec, //
        final int binCount, //
        final DataBounds dataBounds, // NOSONAR optional is fine
        final boolean integerBounds //
    ) throws CanceledExecutionException {

        final var minValue = dataBounds.optLowerBound();
        final var maxValue = dataBounds.optUpperBound();

        sortedColumn = filterColumnValues(sortedColumn, minValue, maxValue);

        DoubleUnaryOperator maybeCeil = integerBounds //
            ? Math::ceil //
            : DoubleUnaryOperator.identity();

        DoubleUnaryOperator maybeFloor = integerBounds //
            ? Math::floor //
            : DoubleUnaryOperator.identity();

        // this algorithm tries to find bin boundaries that give equal counts of values in each bin, but it prioritises
        // not splitting identical values across different bins. This can result in a fewer finite-width bins than
        // expected (and a bunch of zero-width bins at the end to keep the amount of boundaries as expected).
        var countPerBin = Math.toIntExact(Math.round(sortedColumn.size() / (double)binCount));
        var edges = new double[binCount + 1];
        edges[0] = minValue.orElse(maybeFloor.applyAsDouble(sortedColumn.get(0)));
        edges[edges.length - 1] = maxValue.orElse(maybeCeil.applyAsDouble(sortedColumn.get(sortedColumn.size() - 1)));

        int lowestPossibleNextBoundIndex = 0;
        int guessForNextBoundIndex = countPerBin - 1;
        for (int i = 1; i < edges.length - 1; i++) {
            exec.checkCanceled();

            if (guessForNextBoundIndex < sortedColumn.size()) {
                // find endpoints of the streak
                var endpoints = findStreakEndpoints(sortedColumn, guessForNextBoundIndex, integerBounds);
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
                edges[i] = maybeCeil.applyAsDouble(sortedColumn.get(guessForNextBoundIndex));
                lowestPossibleNextBoundIndex = guessForNextBoundIndex + 1;
                guessForNextBoundIndex += countPerBin;
            } else {
                edges[i] = edges[i - 1];
            }
        }

        // assign sensible match behaviour to the edges and return
        return addMatchBehaviourToEdges(edges);
    }

    private static List<Double> filterColumnValues(final List<Double> sortedColumn, final OptionalDouble minValue,
        final OptionalDouble maxValue) {
        // we need to filter the table values to remove anything outside the specified range
        final var filteredValues = sortedColumn.stream() //
            .filter(val -> minValue.isEmpty() || val >= minValue.getAsDouble()) //
            .filter(val -> maxValue.isEmpty() || val <= maxValue.getAsDouble()) //
            .toList();

        if (filteredValues.isEmpty()) {
            throw new IllegalArgumentException("No values to bin remaining after applying fixed upper/lower bound.");
        }
        return filteredValues;
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
     * @param edges a map where keys are column names and values are lists of bin boundaries
     * @param binNamingSettings the settings for naming the bins
     * @return a map where keys are column names and values are lists of bins created from the edges
     * @since 5.10
     */
    public static List<NumericBin> createBins( //
        final List<BinBoundary> edges, //
        final BinNamingScheme binNamingSettings) {

        final List<NumericBin> bins = new ArrayList<>();
        for (int i = 0; i < edges.size() - 1; ++i) {
            var leftEdge = edges.get(i);
            var rightEdge = edges.get(i + 1);

            if (isEmptyInterval(leftEdge, rightEdge)) {
                // skip empty intervals, i.e. intervals where the left edge is equal to the right edge
                continue;
            }

            bins.add(new NumericBin(binNamingSettings.binNaming().computedName(bins.size(), leftEdge, rightEdge), //
                leftEdge.exactMatchBehaviour() == BinBoundaryExactMatchBehaviour.TO_LOWER_BIN, //
                leftEdge.value(), //
                rightEdge.exactMatchBehaviour() == BinBoundaryExactMatchBehaviour.TO_UPPER_BIN, //
                rightEdge.value()));
        }
        addLowerOutlierBin(bins, binNamingSettings.binNameForLowerOutliers());
        addUpperOutlierBin(bins, binNamingSettings.binNameForUpperOutliers());
        return bins;
    }

    /**
     * Checks if the interval defined by the left and right edges is empty, i.e. if the left edge is equal to the right
     * edge and at least one edge is not included
     *
     * @param leftEdge the left edge of the interval
     * @param rightEdge the right edge of the interval
     * @return true if the interval is empty, false otherwise
     */
    private static boolean isEmptyInterval(final BinBoundary leftEdge, final BinBoundary rightEdge) {
        final var leftOrRightOpen = leftEdge.exactMatchBehaviour() == BinBoundaryExactMatchBehaviour.TO_LOWER_BIN
            || rightEdge.exactMatchBehaviour() == BinBoundaryExactMatchBehaviour.TO_UPPER_BIN;
        return leftEdge.value() == rightEdge.value() && leftOrRightOpen;
    }

    /**
     * Sets the lower outlier bin name for all columns in this configuration. This is the bin that will contain any
     * value that is below the lower bound of the first bin.
     *
     * @param bins the list of bins to which the lower outlier bin should be added
     * @param name the name of the lower outlier bin to be added
     */
    private static void addLowerOutlierBin(final List<NumericBin> bins, final String name) {
        var firstNonOutlierBin = bins.get(0);
        bins.add(0, new NumericBin( //
            name, //
            false, //
            Double.NEGATIVE_INFINITY, //
            !firstNonOutlierBin.isLeftOpen(), //
            firstNonOutlierBin.getLeftValue() //
        ));

    }

    /**
     * Sets the upper outlier bin name for all columns in this configuration. This is the bin that will contain any
     * value that is above the upper bound of the last bin.
     *
     * @param name the name of the upper outlier bin to be added
     * @param bins the list of bins to which the upper outlier bin should be added
     */
    private static void addUpperOutlierBin(final List<NumericBin> bins, final String name) {
        var lastNonOutlierBin = bins.get(bins.size() - 1);
        bins.add(new NumericBin( //
            name, //
            !lastNonOutlierBin.isRightOpen(), //
            lastNonOutlierBin.getRightValue(), //
            false, //
            Double.POSITIVE_INFINITY //
        ));
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
