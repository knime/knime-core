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
 *   May 26, 2020 (carlwitt): created
 */
package org.knime.core.data.join.results;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.join.JoinSpecification.InputTable;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.CanceledExecutionException.CancelChecker;

/**
 * Defines methods for collectors of join output rows. For collecting results, it offers
 * {@link #addMatch(DataRow, long, DataRow, long) addMatch}, {@link #addLeftOuter(DataRow, long) addLeftOuter}, and
 * {@link #addRightOuter(DataRow, long) addRightOuter}.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @param <T> the type of the results, either {@link OutputSplit} or {@link OutputCombined}.
 * @since 4.2
 */
public interface JoinResult<T> {

    /**
     * Categories for join results, e.g., matches and unmatched rows. In some situations it is appropriate to refer to
     * all result types, e.g., for {@link OutputMode#COMBINED}.
     */
    enum ResultType {
            /** rows that join rows from the left and right input tables */
            MATCHES,
            /** unmatched rows from the left input table */
            LEFT_OUTER,
            /** unmatched rows from the right table */
            RIGHT_OUTER,
            /** {@link #MATCHES}, {@link #LEFT_OUTER}, and {@link #RIGHT_OUTER} */
            ALL;

        private static final ResultType[] MATCHES_AND_OUTER = new ResultType[]{MATCHES, LEFT_OUTER, RIGHT_OUTER};

        static ResultType[] matchesAndOuter() {
            return MATCHES_AND_OUTER;
        }
    }

    /**
     * Supertype for {@link OutputCombined} and {@link OutputSplit}, just for bounding generics, to make sure either of
     * the two types is used.
     */
    static interface Output {}

    /**
     * A {@link JoinResult} that offers {@link #getTable() getTable} for retrieving results.
     *
     * @author Carl Witt, KNIME AG, Zurich, Switzerland
     * @since 4.2
     */
    public static interface OutputCombined extends Output {

        /**
         * @return a table containing the matches, left unmatched rows, and right unmatched rows.
         * @throws CanceledExecutionException
         */
        BufferedDataTable getTable() throws CanceledExecutionException;

    }

    /**
     * A {@link JoinResult} that offers {@link #getMatches() getMatches}, {@link #getLeftOuter() getLeftOuter}, and
     * {@link #getRightOuter() getRightOuter} for retrieving results.
     *
     * @author Carl Witt, KNIME AG, Zurich, Switzerland
     * @since 4.2
     */
    public static interface OutputSplit extends Output {

        /**
         * @return The {@link DataTable} which holds the inner joins.
         * @throws CanceledExecutionException
         */
        BufferedDataTable getMatches() throws CanceledExecutionException;

        /**
         * @return The unmatched rows from the left table.
         * @throws CanceledExecutionException
         */
        BufferedDataTable getLeftOuter() throws CanceledExecutionException;

        /**
         * @return The unmatched rows from the right table.
         * @throws CanceledExecutionException
         */
        BufferedDataTable getRightOuter() throws CanceledExecutionException;

    }

    /**
     * Processes a {@link DataRow} along with its offset in the {@link BufferedDataTable} that contains the row. In case
     * the operation needs a long time, it is allowed to throw a {@link CanceledExecutionException}.
     */
    @FunctionalInterface
    static interface RowHandlerCancelable {
        /**
         * @param row the data row to process, e.g., index a hash row, look up a probe row, or output an unmatched row.
         * @param offset the position of the row in the input table it comes from, e.g., for sorting outputs or marking
         *            rows as matched in a bitset.
         * @throws CanceledExecutionException
         */
        void accept(DataRow row, long offset) throws CanceledExecutionException;
    }

    /**
     * Processes a {@link DataRow} along with its offset in the {@link BufferedDataTable} that contains the row.
     */
    @FunctionalInterface
    static interface RowHandler extends RowHandlerCancelable {
        @Override
        void accept(DataRow row, long offset);
    }

    /**
     * Safely process each row in the table using a {@link CloseableRowIterator} that eventually frees resources,
     * checking for cancellation in between row processing.
     *
     * @param table provides rows
     * @param consumer processes a row
     * @param canceled provides a way to check whether the execution was canceled
     * @throws CanceledExecutionException escalated from {@link CancelChecker#checkCanceled()}
     */
    static void iterateWithResources(final BufferedDataTable table, final Consumer<DataRow> consumer,
        final CancelChecker canceled) throws CanceledExecutionException {
        try (CloseableRowIterator it = table.iterator()) {
            while (it.hasNext()) {
                canceled.checkCanceled();
                consumer.accept(it.next());
            }
        }
    }

    /**
     * Safely process each row in the table using a {@link CloseableRowIterator} that eventually frees resources,
     * checking for cancellation in between row processing.
     *
     * @param table provides rows
     * @param consumer consumes the row's offset in the table and the row
     * @param canceled provides a way to check whether the execution was canceled
     * @throws CanceledExecutionException escalated from {@link CancelChecker#checkCanceled()}
     */
    static <T extends RowHandlerCancelable> void enumerateWithResources(final BufferedDataTable table, final T consumer,
        final CancelChecker canceled) throws CanceledExecutionException {
        long offset = 0;
        try (CloseableRowIterator it = table.iterator()) {
            while (it.hasNext()) {
                canceled.checkCanceled();
                consumer.accept(it.next(), offset);
                offset++;
            }
        }
    }

    /**
     * Accepts the given row as a an inner join result if {@link #isRetainMatched()} is true. If
     * {@link #isRetainMatched()} is false, the operation has no effect.
     *
     * @param left a row from the left input table
     * @param leftOrder sort order of the left row, e.g., row offset in the left table
     * @param right a row from the right input table
     * @param rightOrder analogous
     * @return whether the match was accepted.
     */
    boolean addMatch(DataRow left, long leftOrder, DataRow right, long rightOrder);

    /**
     * Adds this row to the unmatched rows from the left table, if {@link #isRetainUnmatched(InputTable)} for
     * {@link InputTable#LEFT}, otherwise does nothing.
     *
     * @param row a row from the left input table
     * @param offset sort order of the row, e.g., row offset in the table
     * @return whether the row was accepted
     */
    boolean addLeftOuter(DataRow row, long offset);

    /**
     * Adds this row to the unmatched rows from the right table, if {@link #isRetainUnmatched(InputTable)} for
     * {@link InputTable#RIGHT}, otherwise does nothing.
     *
     * @param row a row from the right input table
     * @param offset sort order of the row, e.g., row offset in the table
     * @return whether the row was accepted
     */
    boolean addRightOuter(DataRow row, long offset);

    /**
     * @return the {@link OutputCombined} or {@link OutputSplit} instance providing access to the join results.
     */
    T getResults();

    /** @return whether to keep rows added via {@link #addMatch(DataRow, long, DataRow, long)}. */
    boolean isRetainMatched();

    /**
     * @param side left input table or right input table.
     * @return whether to keep rows added via {@link #addLeftOuter(DataRow, long)} or
     *         {@link #addRightOuter(DataRow, long)}.
     */
    boolean isRetainUnmatched(InputTable side);

    /**
     * @param side left or right input table
     * @return a callback to add unmatched rows from the desired side
     */
    default RowHandler unmatched(final InputTable side) {
        return side.isLeft() ? this::addLeftOuter : this::addRightOuter;
    }

    /**
     * Indicate that heap space is running low.
     */
    void lowMemory();

    /**
     * If to-be-joined data is too big to join in memory, the join will be split in several passes. In this case, the
     * handling of unmatched rows from the table (left/right) that is used as probe input has to be deferred until the
     * last pass over the probe input has been completed.
     *
     * @param side for which input table to set
     * @param defer whether to enable deferred collection of unmatched rows
     */
    void setDeferUnmatchedRows(final InputTable side, final boolean defer);

    /**
     * Returns a mapping from input row keys to output row keys.
     *
     * @param side whether to return the mapping for the rows of the left or right input table
     * @param resultType the mapping
     * @return an empty optional if hiliting is disabled. Otherwise a mapping that associates input rows from the given
     *         side to the rows in the output of the given result type
     */
    Optional<Map<RowKey, Set<RowKey>>> getHiliteMapping(final InputTable side, final ResultType resultType);

}
