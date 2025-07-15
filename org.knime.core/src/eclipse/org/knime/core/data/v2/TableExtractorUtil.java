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

package org.knime.core.data.v2;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 * Mechanisms to apply {@link Extractor}s to a {@link BufferedDataTable}.
 *
 * The central method is {@link TableExtractorUtil#extractData} which traverses a given {@link BufferedDataTable} and
 * uses a list of {@link Extractor} to extract data from it. Hereby, various implementations of {@link Extractor} can be
 * used to extract data in a certain way.<b>Example usage</b>:
 *
 * <pre>
 * <code>final var twoColumnsDoubleExtractor =
 *      new MultipleColumnsExtractor<Double>(Double.class,
 *          new int[]{1, 3},
 *         (row, i) -> ((DoubleValue)row.getValue(i)).getDoubleValue()
 *     );
 * final var rowKeyExtractor =
 *     new SingleColumnExtractor<String>(String.class,
 *         (row) -> row.getRowKey().getString()
 *     );
 *
 * extractData(table, new AbstractExtractor[]{ twoColumnsDoubleExtractor, rowKeyExtractor});
 *
 * //holds the content of column 1 and 3 converted to double
 * final Double[][] values = twoColumnsDoubleExtractor.getOutput();
 * //holds the row keys of the table
 * final String[] rowKeys = rowKeyExtractor.getOutput();
 * </code>
 * </pre>
 *
 * This utility class can be used in the data service or the initial data generation of a view.
 *
 * @author Paul Bärnreuther
 * @since 5.1
 * @noextend Not public API, for internal use only.
 * @noreference Not public API, for internal use only.
 */
public final class TableExtractorUtil {

    private TableExtractorUtil() {

    }

    /**
     * This method traverses a table up to a given number of rows and applies extractors to each read row. The
     * extractors are (re)initialised at the start of the process.
     *
     * @param table a {@link BufferedDataTable} with at least {@code numRows}.
     * @param numRows the number of rows traversed. The extraction is aborted if this is a negative number.
     * @param extractors an array of {@link Extractor} constructed in a way that corresponding column indices are
     *            present in the table and their {@code readValue} methods are applicable to a row of the {@code table}
     *            materialised with the union columns.
     */
    public static void extractData(final BufferedDataTable table, final long numRows, final Extractor... extractors) {
        if (numRows < 0) {
            return;
        }
        initializeExtractors(extractors, (int)numRows);
        if (numRows > 0) {
            try (final var cursor = createCursor(table, getAllIndices(extractors), numRows)) {
                try {
                    traverseData(cursor, null, extractors);
                } catch (CanceledExecutionException e) { // NOSONAR
                    // NOSONAR exception never thrown if execution context is null
                }
            }
        }
    }

    /**
     * @param table
     * @param extractors
     *
     * @see TableExtractorUtil#extractData(BufferedDataTable, ExecutionContext, Extractor...)
     * @since 5.6
     */
    public static void extractDataUncancelled(final BufferedDataTable table, final Extractor... extractors) {
        try {
            extractData(table, null, extractors);
        } catch (CanceledExecutionException e) { // NOSONAR
            // NOSONAR exception never thrown if execution context is null
        }
    }

    /**
     * This method traverses a table and applies extractors to each read row. The execution context (and consequently
     * the thread) is checked for cancellation before each next row. The extractors are (re)initialized at the start of
     * the process.
     *
     * @param table a {@link BufferedDataTable} with at least {@code numRows}.
     * @param executionContext the {@link ExecutionContext} to check for cancellation. Use
     *            {@link #extractDataUncancelled(BufferedDataTable, Extractor...)} if no execution context is available.
     *
     * @param extractors an array of {@link Extractor} constructed in a way that corresponding colum indices are present
     *            in the table and their {@code readValue} methods are applicable to a row of the {@code table}
     *            materialised with the union columns.
     * @throws CanceledExecutionException To handle execution cancellation or thread interruption.
     * @since 5.3
     */
    public static void extractData(final BufferedDataTable table, final ExecutionContext executionContext,
        final Extractor... extractors) throws CanceledExecutionException {
        initializeExtractors(extractors, (int)table.size());
        try (final var cursor = createCursor(table, getAllIndices(extractors))) {
            traverseData(cursor, executionContext, extractors);
        }
    }

    private static void initializeExtractors(final Extractor[] extractors, final int size) {
        for (Extractor extractor : extractors) {
            extractor.init(size);
        }
    }

    private static RowCursor createCursor(final BufferedDataTable table, final int[] colIndices, final long numRows) {
        var filter = new TableFilter.Builder() //
            .withToRowIndex(numRows - 1) //
            .withMaterializeColumnIndices(colIndices) //
            .build();
        return table.cursor(filter);
    }

    private static RowCursor createCursor(final BufferedDataTable table, final int[] colIndices) {
        var filter = new TableFilter.Builder() //
            .withMaterializeColumnIndices(colIndices) //
            .build();
        return table.cursor(filter);
    }

    private static int[] getAllIndices(final Extractor[] extractors) {
        return Arrays.asList(extractors).stream() //
            .map(converter -> IntStream.of(converter.getColumnIndices())) //
            .reduce(IntStream::concat) //
            .orElseGet(IntStream::empty) //
            .distinct() //
            .toArray();
    }

    /**
     * Assumes CanceledExecutionException is never thrown if executionContext is null.
     */
    private static void traverseData(final RowCursor cursor, final ExecutionContext executionContext,
        final Extractor[] extractors) throws CanceledExecutionException {
        var i = 0;
        while (cursor.canForward()) {
            if (executionContext != null) {
                executionContext.checkCanceled();
            }
            final var row = cursor.forward();
            for (Extractor extractor : extractors) {
                extractor.readRow(row, i);
            }
            i++;
        }
    }

    /**
     * Restricts a function taking a {@link RowRead} and an arbitrary integer to one selected integer.
     *
     * @param <T> the return type of the function.
     * @param bifunction the function which is to be restricted.
     * @param index to which the function is restricted.
     * @return the restricted function.
     */
    public static <T> Function<RowRead, T> restrictToIndex(final BiFunction<RowRead, Integer, T> bifunction,
        final int index) {
        return (final RowRead row) -> bifunction.apply(row, index);
    }

    /**
     * An interface defining the necessary methods during the extraction of data from a table.
     *
     * @author Paul Bärnreuther
     */
    public interface Extractor {

        /**
         * Initialises the extractor. This gets called once at the start of an extraction process.
         *
         * @param size the number of rows that will be traversed.
         */
        void init(int size);

        /**
         * Gets called once per row and extracts data from it in some way.
         *
         * @param row the {@link RowRead} read from a {@link BufferedDataTable}.
         * @param rowIndex the number how often this function was called since initialisation.
         */
        void readRow(RowRead row, int rowIndex);

        /**
         * The column indices of the table that need to be materialised in order to extract the desired data.
         *
         * @return an integer array of column indices.
         */
        int[] getColumnIndices();

    }
}
