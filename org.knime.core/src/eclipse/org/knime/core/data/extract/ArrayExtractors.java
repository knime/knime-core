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
 */

package org.knime.core.data.extract;

import static org.knime.core.data.v2.RowReadUtil.readPrimitiveDoubleValue;
import static org.knime.core.data.v2.RowReadUtil.readPrimitiveIntValue;
import static org.knime.core.data.v2.RowReadUtil.readStringValue;
import static org.knime.core.data.v2.TableExtractorUtil.restrictToIndex;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.knime.core.data.v2.RowRead;
import org.knime.core.data.v2.TableExtractorUtil.Extractor;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.util.Pair;

/**
 * This class serves as a collection of classes implementing {@link Extractor} which extract an array from one ore
 * multiple columns of a {@link BufferedDataTable}. This is the first step of the serialisation of the data towards the
 * frontend.
 *
 * @author Paul Bärnreuther
 *
 * @since 5.6
 */
public final class ArrayExtractors {

    /**
     * This extractor depends on zero or one column of the table and creates one object for every row.
     *
     * @author Paul Bärnreuther
     * @param <T> the type of the output objects.
     */
    public static final class SingleColumnExtractor<T> implements Extractor {
        final Function<RowRead, T> m_readValueCallback;

        private T[] m_output;

        private final Class<T> m_class;

        private final Integer m_colIndex;

        /**
         * This constructor is to be called if the given callback does not depend on the materialization of any column
         * of the traversed table
         *
         * @param c the class with which the output array will be created.
         * @param readValueCallback a function extracting an instance of {@code T} from a {@link RowRead}.
         */
        public SingleColumnExtractor(final Class<T> c, final Function<RowRead, T> readValueCallback) {
            this(c, null, readValueCallback);
        }

        /**
         * This constructor is to be called if the given callback depend on the materialization of one column of the
         * traversed table
         *
         * @param c the class with which the output array will be created.
         * @param colIndex the index of the column which the callback should be applied to.
         * @param readValueCallback a function extracting an instance of {@code T} from a {@link RowRead} and a given
         *            index
         */
        public SingleColumnExtractor(final Class<T> c, final int colIndex,
            final BiFunction<RowRead, Integer, T> readValueCallback) {
            this(c, colIndex, restrictToIndex(readValueCallback, colIndex));
        }

        private SingleColumnExtractor(final Class<T> c, final Integer colIndex,
            final Function<RowRead, T> readValueCallback) {
            m_readValueCallback = readValueCallback;
            m_class = c;
            m_colIndex = colIndex;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void init(final int size) {
            m_output = (T[])Array.newInstance(m_class, size);
        }

        @Override
        public void readRow(final RowRead row, final int rowIndex) {
            m_output[rowIndex] = m_readValueCallback.apply(row);
        }

        @Override
        public int[] getColumnIndices() {
            if (m_colIndex == null) {
                return new int[0];
            } else {
                return new int[]{m_colIndex};
            }
        }

        /**
         * @return the output of the extractor. It is null until the initialisation at the start of an extraction. After
         *         an extraction, every entry of the output comes from a single row of the traversed table.
         */
        public T[] getOutput() {
            return m_output;
        }
    }

    /**
     * This extractor depends on multiple columns of the table and creates an array of object for every row. However,
     * for every column the same callback function is applied.
     *
     * @author Paul Bärnreuther
     * @param <T> the type of the output objects.
     */
    public static final class MultipleColumnsExtractor<T> implements Extractor {

        private T[][] m_output;

        private final int[] m_colIndices;

        private final BiFunction<RowRead, Integer, T> m_readValueCallback;

        private final Class<T> m_class;

        /**
         * Creates an extractor with a list of indices to which the same callback is to be applied.
         *
         * @param c the class with which the output array will be created.
         * @param colIndices the column indices to which the callback is applied.
         * @param readValueCallback a callback extracting an instance of T from a given {@link RowRead} and a column.
         *            index.
         */
        public MultipleColumnsExtractor(final Class<T> c, final int[] colIndices,
            final BiFunction<RowRead, Integer, T> readValueCallback) {
            m_class = c;
            m_colIndices = colIndices;
            m_readValueCallback = readValueCallback;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void init(final int size) {
            m_output = (T[][])Array.newInstance(m_class, m_colIndices.length, size);
        }

        @Override
        public void readRow(final RowRead row, final int rowIndex) {
            for (int i = 0; i < m_colIndices.length; i++) {
                m_output[i][rowIndex] = m_readValueCallback.apply(row, m_colIndices[i]);
            }
        }

        /**
         * @return the output of the extractor. It is null until the initialization at the start of an extraction. After
         *         an extraction, the entries of the output correspond to the columns given to the extractor and every
         *         entry of one such entry comes from a single row of the traversed table.
         */
        public T[][] getOutput() {
            return m_output;
        }

        @Override
        public int[] getColumnIndices() {
            return m_colIndices;
        }

    }

    /**
     * This extractor generates an Object array for every row whose entries are the result of several given callback
     * functions.
     *
     * @author Paul Bärnreuther
     */
    public static final class ExtractorWithMultipleCallbacks implements Extractor {

        private Object[][] m_output;

        private final List<Function<RowRead, Object>> m_readValueCallbacks;

        private final int m_numCallbacks;

        private final int m_outputRowSize;

        private final int[] m_colIndices;

        /**
         * @param colIndices the indices of the columns that need to be materialised in order to apply the
         *            {@code readValueCallbacks}.
         * @param readValueCallbacks a list of callback functions applied to each row. The results form the elements of
         *            each entry of the output.
         */
        public ExtractorWithMultipleCallbacks(final int[] colIndices,
            final List<Function<RowRead, Object>> readValueCallbacks) {
            m_readValueCallbacks = readValueCallbacks;
            m_numCallbacks = m_readValueCallbacks.size();
            m_outputRowSize = readValueCallbacks.size();
            m_colIndices = colIndices;
        }

        @Override
        public void init(final int size) {
            m_output = new Object[size][m_outputRowSize];
        }

        @Override
        public void readRow(final RowRead row, final int rowIndex) {
            IntStream.range(0, m_numCallbacks)
                .forEach(i -> m_output[rowIndex][i] = m_readValueCallbacks.get(i).apply(row));
        }

        /**
         * @return the output given by a list of extracted objects for every read row. It is null until the
         *         initialisation at the start of an extraction.
         */
        public Object[][] getOutput() {
            return m_output;
        }

        @Override
        public int[] getColumnIndices() {
            return m_colIndices;
        }
    }

    /**
     * Extracts an array of primitive double values from a column. If values are missing, the extraction throws an
     * error.
     *
     * @author Paul Bärnreuther
     */
    public static class PrimitiveDoubleExtractor implements Extractor {

        private double[] m_output;

        private final int m_colIndex;

        /**
         * @param colIndex the index of the column from which the data is extracted
         */
        public PrimitiveDoubleExtractor(final int colIndex) {
            m_colIndex = colIndex;
        }

        @Override
        public void init(final int size) {
            m_output = new double[size];
        }

        @Override
        public void readRow(final RowRead row, final int rowIndex) {
            m_output[rowIndex] = readPrimitiveDoubleValue(row, m_colIndex);
        }

        @Override
        public int[] getColumnIndices() {
            return new int[]{m_colIndex};
        }

        /**
         * @return the output of the extractor. It is null until the initialisation at the start of an extraction. After
         *         an extraction, every entry of the output comes from a single row of the traversed table.
         */
        public double[] getOutput() {
            return m_output;
        }
    }

    /**
     * Additionally to extracting the double values of a column, the affiliations of these values (rows) to categories
     * in a given condition column is stored in separate arrays per category. The entry of such an array is zero if the
     * associated row does not contain the corresponding category. Otherwise it is 1 divided by the total number of
     * observations such that the total sum of all these arrays is one.
     *
     * @author Paul Bärnreuther
     */
    public static final class PrimitiveDoubleExtractorWithWeightsPerCategory extends PrimitiveDoubleExtractor {

        private final int m_catIndex;

        private Map<String, double[]> m_weights;

        private int m_size;

        private double m_singleWeight;

        /**
         * @param catIndex the index of the nominal condition column.
         * @param freqIndex the index of the numerical column which is to be filtered.
         */
        public PrimitiveDoubleExtractorWithWeightsPerCategory(final int catIndex, final int freqIndex) {
            super(freqIndex);
            m_catIndex = catIndex;
        }

        @Override
        public void init(final int size) {
            super.init(size);
            m_weights = new LinkedHashMap<>();
            m_size = size;
            m_singleWeight = 1d / size;
        }

        @Override
        public void readRow(final RowRead row, final int rowIndex) {
            super.readRow(row, rowIndex);
            final var cat = readStringValue(row, m_catIndex);
            m_weights.computeIfAbsent(cat, key -> new double[m_size]);
            m_weights.get(cat)[rowIndex] = m_singleWeight;
        }

        @Override
        public int[] getColumnIndices() {
            return new int[]{super.getColumnIndices()[0], m_catIndex};
        }

        /**
         * @return a pair of the categories of the condition column and the associated weights. A weight is zero if the
         *         associated row did not have the corresponding category and 1 divided by the total number of
         *         observations otherwise.
         */
        public Pair<List<String>, List<double[]>> getCategoriesAndWeights() {
            final var categories = new ArrayList<String>();
            final var weights = new ArrayList<double[]>();
            for (final var entry : m_weights.entrySet()) {
                categories.add(entry.getKey());
                weights.add(entry.getValue());
            }
            return new Pair<>(categories, weights);
        }
    }

    /**
     * This extractor reverts the bin naming strategy BinNaming.numbered of the {@link AutoBinner} by parsing the bin
     * name "Bin i" to the index i-1 and returning the binned frequencies as an array of integers.
     *
     * @author Paul Bärnreuther
     */
    public static class BinningResultsParser implements Extractor {

        private static final Pattern keepOnlyDigits = Pattern.compile("[^0-9]");

        private int m_indexCol;

        private int m_valueCol;

        private int[] m_output;

        /**
         * @param binNameColIndex the index of the column containing the numbered bin names ("Bin 1", "Bin 2", ...)
         * @param binValueColIndex the index of the column containing the aggregated frequencies of the bins
         * @param numBins the number of bins
         */
        public BinningResultsParser(final int binNameColIndex, final int binValueColIndex, final int numBins) {
            m_indexCol = binNameColIndex;
            m_valueCol = binValueColIndex;
            m_output = new int[numBins];
        }

        @Override
        public void init(final int size) {
            // Nothing to be initialized
        }

        @Override
        public void readRow(final RowRead row, final int rowIndex) {
            final var binName = getBinName(row);
            if (binName == null) {
                return;
            }
            m_output[parseToIndex(binName)] = getValue(row);
        }

        /**
         * @param row
         * @return the bin value of the row
         */
        protected int getValue(final RowRead row) {
            return readPrimitiveIntValue(row, m_valueCol);
        }

        /**
         * @param row
         * @return the bin name of the row
         */
        protected String getBinName(final RowRead row) {
            return readStringValue(row, m_indexCol);
        }

        /**
         * @param binName
         * @return the index left by the binName after keeping only the digits.
         */
        protected int parseToIndex(final String binName) {
            return Integer.parseInt(keepOnlyDigits.matcher(binName).replaceAll("")) - 1;
        }

        @Override
        public int[] getColumnIndices() {
            return new int[]{m_indexCol, m_valueCol};
        }

        /**
         * @return the array of integers, where the i'th entry is the value in the row with the string in the bin name
         *         column obtained by String.format("Bin %s", i+1)
         */
        public int[] getOutput() {
            return m_output;
        }
    }

    /**
     * This extractor extends {@link BinningResultsParser} by additionally grouping the results by a given condition
     * category
     *
     * @author Paul Bärnreuther
     */
    public static final class GroupByBinningResultsParser extends BinningResultsParser {

        private int m_conditionCol;

        private Map<String, int[]> m_catToFreq;

        private int m_numBins;

        /**
         * @param conditionColIndex the index of the column with respect to the results are grouped
         * @param binNameColIndex the index of the column containing the numbered bin names ("Bin 1", "Bin 2", ...)
         * @param binValueColIndex the index of the column containing the aggregated frequencies of the bins
         * @param numBins the number of bins
         */
        public GroupByBinningResultsParser(final int conditionColIndex, final int binNameColIndex,
            final int binValueColIndex, final int numBins) {
            super(binNameColIndex, binValueColIndex, 0);
            m_catToFreq = new LinkedHashMap<>();
            m_numBins = numBins;
            m_conditionCol = conditionColIndex;
        }

        @Override
        public void readRow(final RowRead row, final int rowIndex) {
            final var binName = getBinName(row);
            if (binName == null) {
                return;
            }
            final var category = readStringValue(row, m_conditionCol);
            m_catToFreq.computeIfAbsent(category, key -> new int[m_numBins]);
            m_catToFreq.get(category)[parseToIndex(binName)] = getValue(row);
        }

        @Override
        public int[] getColumnIndices() {
            return new int[]{super.getColumnIndices()[0], super.getColumnIndices()[1], m_conditionCol};
        }

        /**
         * @return a pair of the categories in the condition column and the corresponding frequencies of the bins.
         */
        public Pair<List<String>, List<int[]>> getCategoriesAndFrequencies() {
            final var categories = new ArrayList<String>();
            final var frequencies = new ArrayList<int[]>();
            for (final var entry : m_catToFreq.entrySet()) {
                categories.add(entry.getKey());
                frequencies.add(entry.getValue());
            }
            return new Pair<>(categories, frequencies);
        }
    }
}
