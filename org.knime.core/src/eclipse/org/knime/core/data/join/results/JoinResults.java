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

import java.util.function.Consumer;
import java.util.function.ObjLongConsumer;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.join.JoinSpecification;
import org.knime.core.data.join.JoinSpecification.InputTable;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.CanceledExecutionException.CancelChecker;

/**
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 4.2
 */
public interface JoinResults {

    enum OutputMode {OutputCombined, OutputSplit}

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
    public boolean addMatch(DataRow left, long leftOrder, DataRow right, long rightOrder);

    /**
     * Adds this row to the unmatched rows from the left table, if {@link #isRetainUnmatched(InputTable)} for
     * {@link InputTable#LEFT}, otherwise does nothing.
     *
     * @param row a row from the left input table
     * @param offset sort order of the row, e.g., row offset in the table
     * @return whether the row was accepted
     */
    public boolean addLeftOuter(DataRow row, long offset);

    /**
     * Adds this row to the unmatched rows from the right table, if {@link #isRetainUnmatched(InputTable)} for
     * {@link InputTable#RIGHT}, otherwise does nothing.
     *
     * @param row a row from the right input table
     * @param offset sort order of the row, e.g., row offset in the table
     * @return whether the row was accepted
     */
    public boolean addRightOuter(DataRow row, long offset);

    /** @return whether to keep rows added via {@link #addMatch(DataRow, long, DataRow, long)}. */
    public boolean isRetainMatched();

    /**
     * @param side left input table or right input table.
     * @return whether to keep rows added via {@link #addLeftOuter(DataRow, long)} or
     *         {@link #addRightOuter(DataRow, long)}.
     */
    public boolean isRetainUnmatched(InputTable side);

    /** Signals that all results added since the previous call to {@link #sortedChunkEnd()} are in sorted order. */
    default void sortedChunkEnd() {}

    /**
     * @param side left or right input table
     * @return a callback to add unmatched rows from the desired side
     */
    default ObjLongConsumer<DataRow> unmatched(final InputTable side) {
        return side.isLeft() ? this::addLeftOuter : this::addRightOuter;
    }

    /**
     * Safely process each row in the table using a {@link CloseableRowIterator} that eventually frees resources,
     * checking for cancelation in between row processing.
     *
     * @param table provides rows
     * @param consumer processes a row
     * @param canceled provides a way to check whether the execution was canceled
     * @throws CanceledExecutionException escalated from {@link CancelChecker#checkCanceled()}
     */
    static void iterateWithResources(final BufferedDataTable table, final Consumer<DataRow> consumer, final CancelChecker canceled) throws CanceledExecutionException {
        try (CloseableRowIterator it = table.iterator()) {
            while (it.hasNext()) {
                canceled.checkCanceled();
                consumer.accept(it.next());
            }
        }
    }

    /**
     * Safely process each row in the table using a {@link CloseableRowIterator} that eventually frees resources,
     * checking for cancelation in between row processing.
     *
     * @param table provides rows
     * @param consumer consumes the row's offset in the table and the row
     * @param canceled provides a way to check whether the execution was canceled
     * @throws CanceledExecutionException escalated from {@link CancelChecker#checkCanceled()}
     */
    static void enumerateWithResources(final BufferedDataTable table, final ObjLongConsumer<DataRow> consumer,
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
     * @param joinSpecification the join specification used to determine whether to retain unmatched rows, project rows
     *            to their included columns, etc.
     */
    public void setJoinSpecification(JoinSpecification joinSpecification);

    /**
     * @return the join specification used to determine whether to retain unmatched rows, project rows to their included
     *         columns, etc.
     */
    public JoinSpecification getJoinSpecification();

    /**
     * @return if true, only the first call to {@link #addMatch(DataRow, long, DataRow, long)},
     *         {@link #addLeftOuter(DataRow, long)}, and {@link #addRightOuter(DataRow, long)} with a specific row
     *         offset will be effective (subsequent calls will have no effect)
     */
    boolean isDeduplicateResults();

   /**
    *
    * @author Carl Witt, KNIME AG, Zurich, Switzerland
    * @since 4.2
    */
   public interface OutputCombined extends JoinResults {

       /**
        * @return a table containing the matches, left unmatched rows, and right unmatched rows.
        * @throws CanceledExecutionException
        */
       public BufferedDataTable getTable() throws CanceledExecutionException;

   }

   /**
   *
   * @author Carl Witt, KNIME AG, Zurich, Switzerland
   * @since 4.2
   */
  public interface OutputSplit extends JoinResults {

      /**
       * @return The {@link DataTable} which holds the inner joins.
       * @throws CanceledExecutionException
       */
      public BufferedDataTable getMatches() throws CanceledExecutionException;

      /**
       * @return The unmatched rows from the left table.
       * @throws CanceledExecutionException
       */
      public BufferedDataTable getLeftOuter() throws CanceledExecutionException;

      /**
       * @return The unmatched rows from the right table.
       * @throws CanceledExecutionException
       */
      public BufferedDataTable getRightOuter() throws CanceledExecutionException;

  }

}