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

import java.util.Collections;
import java.util.List;
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
 * {@link #offerMatch(DataRow, long, DataRow, long) addMatch}, {@link #offerLeftOuter(DataRow, long) addLeftOuter}, and
 * {@link #offerRightOuter(DataRow, long) addRightOuter}.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @param <T> the type of the results, either {@link OutputSplit} or {@link OutputCombined}.
 * @since 4.2
 */
public interface JoinResult<T> {

    /**
     * Categories for join results, e.g., matches and unmatched rows.
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

        public static final List<ResultType> MATCHES_AND_OUTER =
            Collections.unmodifiableList(List.of(MATCHES, LEFT_OUTER, RIGHT_OUTER));
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
     * @return the {@link OutputCombined} or {@link OutputSplit} instance providing access to the join results.
     */
    T getResults();

    /** @return whether to keep rows added via {@link #offerMatch(DataRow, long, DataRow, long)}. */
    boolean isRetainMatched();

    /**
     * @param side left input table or right input table.
     * @return whether to keep rows added via {@link #offerLeftOuter(DataRow, long)} or
     *         {@link #offerRightOuter(DataRow, long)}.
     */
    boolean isRetainUnmatched(InputTable side);

    /**
     * @param side left or right input table
     * @return a callback to add unmatched rows from the desired side
     */
    default RowHandler unmatched(final InputTable side) {
        return side.isLeft() ? this::offerLeftOuter : this::offerRightOuter;
    }

    /**
     * Indicate that heap space is running low.
     */
    void lowMemory();


    /**
     * Signal that false positive unmatched rows may occur for the given input side, i.e., unmatched rows from that
     * input side may turn out <b>to be matched later on</b>.<br/>
     *
     * For instance, after a call to {@link #deferUnmatchedRows(InputTable)} with inputSide = {@link InputTable#LEFT} it
     * is valid to first call {@link #offerLeftOuter(DataRow, long)} for a row X from the left input table and afterwards
     * {@link #offerMatch(DataRow, long, DataRow, long)} for that same row X. The implementation will then cancel the
     * unmatched status of X and will not output it as unmatched when calling {@link #getResults()}.
     *
     * All calls to this method after the first call for the same input side have no effect - deferred collection is
     * then already activated.
     *
     * @param side
     */
    void deferUnmatchedRows(final InputTable side);

    /**
     * Enable tracking of matches to reject matches in {@link #offerMatch(DataRow, long, DataRow, long)} if they have
     * been added before.
     */
    void deduplicateMatches();

    /**
     * Returns a mapping from input row keys to output row keys.
     *
     * @param side whether to return the mapping for the rows of the left or right input table
     * @param resultType the mapping
     * @return an empty optional if hiliting is disabled. Otherwise a mapping that associates input rows from the given
     *         side to the rows in the output of the given result type
     */
    Optional<Map<RowKey, Set<RowKey>>> getHiliteMapping(final InputTable side, final ResultType resultType);

    /**
     * Implementation for adding an unmatched row from the right table.
     *
     * @param row same as for {@link #offerRightOuter(DataRow, long)}
     * @param offset same as for {@link #offerRightOuter(DataRow, long)}
     */
    void doAddRightOuter(final DataRow row, final long offset);

    /**
     * Implementation for adding an unmatched row from the left table.
     *
     * @param row same as for {@link #offerLeftOuter(DataRow, long)}
     * @param offset same as for {@link #offerLeftOuter(DataRow, long)}
     */
    void doAddLeftOuter(final DataRow row, final long offset);

    /**
     * Implementation for adding a matching row pair.
     *
     * @param left same as for {@link #offerMatch(DataRow, long, DataRow, long)}
     * @param leftOrder same as for {@link #offerMatch(DataRow, long, DataRow, long)}
     * @param right same as for {@link #offerMatch(DataRow, long, DataRow, long)}
     * @param rightOrder same as for {@link #offerMatch(DataRow, long, DataRow, long)}
     *
     */
    void doAddMatch(final DataRow left, final long leftOrder, final DataRow right, final long rightOrder);

    /**
     * Accepts the given row as a an inner join result if {@link #isRetainMatched()} is true. If
     * {@link #isRetainMatched()} is false, the operation has no effect.
     *
     * @param left a row from the left input table
     * @param leftOrder sort order of the left row, e.g., row offset in the left table
     * @param right a row from the right input table
     * @param rightOrder analogous
     */
    void offerMatch(final DataRow left, final long leftOrder, final DataRow right, final long rightOrder);

    /**
     * Adds this row to the unmatched rows from the left table, if {@link #isRetainUnmatched(InputTable)} for
     * {@link InputTable#LEFT}, otherwise does nothing.
     *
     * @param row a row from the left input table
     * @param offset sort order of the row, e.g., row offset in the table
     */
    void offerLeftOuter(final DataRow row, final long offset);

    /**
     * Adds this row to the unmatched rows from the right table, if {@link #isRetainUnmatched(InputTable)} for
     * {@link InputTable#RIGHT}, otherwise does nothing.
     *
     * @param row a row from the right input table
     * @param offset sort order of the row, e.g., row offset in the table
     */
    void offerRightOuter(final DataRow row, final long offset);

}
