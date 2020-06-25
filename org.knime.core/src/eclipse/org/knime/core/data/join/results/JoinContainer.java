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
 *   Jun 20, 2020 (carlwitt): created
 */
package org.knime.core.data.join.results;

import java.util.function.ObjLongConsumer;
import java.util.stream.Stream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.join.HybridHashJoin;
import org.knime.core.data.join.JoinSpecification;
import org.knime.core.data.join.JoinSpecification.InputTable;
import org.knime.core.data.join.OrderedRow;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.CanceledExecutionException.CancelChecker;
import org.knime.core.node.ExecutionContext;

import gnu.trove.set.hash.TLongHashSet;

/**
 * Base class for implementations of the {@link JoinResults} interface.
 * Provides result deduplication capabilities for disjunctive joins.
 * TODO hiliting
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 4.2
 */
public abstract class JoinContainer implements JoinResults {

    /**
     * Can be used to address arrays of data structures for different result types.
     */
    protected static final int LEFT_OUTER = 0, RIGHT_OUTER = 1, MATCHES = 2;

    protected final UnmatchedRows[] m_unmatchedRows;

    /** The i-th element of this array contains the table format of the i-th element in {@link #m_tables}. */
    protected final DataTableSpec[] m_outputSpecs = new DataTableSpec[3];

    /**
     * Used to determine the output table specs and perform the actual join operation between two matching rows as well
     * as padding unmatched rows with missing values according to the match table spec.
     *
     * @see JoinSpecification#specForMatchTable()
     */
    protected JoinSpecification m_joinSpecification;

    /**
     * For creating tables.
     */
    protected final ExecutionContext m_exec;

    /**
     * Used to check whether a result with a given combination of row offsets was added before.
     */
    protected final RowOffsetCombinationSet[] m_caches;

    /**
     * {@link JoinContainer#JoinContainer(JoinSpecification, ExecutionContext, boolean)}
     */
    protected final boolean m_deduplicateResults;

    private CancelChecker m_checkCanceled;

    /**
     * @param joinSpecification which results to keep, e.g., matches, left unmatched rows, right unmatched rows
     * @param exec for creating {@link BufferedDataTable}s
     * @param deduplicateResults If true, invocations of {@link #addMatch(DataRow, long, DataRow, long)},
     *            {@link #addLeftOuter(DataRow, long)}, and {@link #addRightOuter(DataRow, long)} will reject the given
     *            row if the method was previously invoked with the same row offsets.
     * @param deferUnmatchedRows
     */
    public JoinContainer(final JoinSpecification joinSpecification, final ExecutionContext exec, final boolean deduplicateResults, final boolean deferUnmatchedRows) {
        m_joinSpecification = joinSpecification;
        m_exec = exec;
        m_deduplicateResults = deduplicateResults;
        m_checkCanceled = CancelChecker.checkCanceledPeriodically(m_exec);

        m_outputSpecs[LEFT_OUTER] = m_joinSpecification.specForUnmatched(InputTable.LEFT);
        m_outputSpecs[RIGHT_OUTER] = m_joinSpecification.specForUnmatched(InputTable.RIGHT);
        m_outputSpecs[MATCHES] = m_joinSpecification.specForMatchTable();

        if (m_deduplicateResults) {
            m_caches = Stream.generate(RowOffsetCombinationSet::new).limit(3).toArray(RowOffsetCombinationSet[]::new);
        } else {
            m_caches = null;
        }

        m_unmatchedRows = new UnmatchedRows[2];
        m_unmatchedRows[LEFT_OUTER] = unmatchedRowHandler(InputTable.LEFT, deferUnmatchedRows);
        m_unmatchedRows[RIGHT_OUTER] = unmatchedRowHandler(InputTable.RIGHT, deferUnmatchedRows);

    }

    /**
     * If a {@link HybridHashJoin} can't be performed in memory, it will flush partitions of the input table to disk.
     * If these partitions are still too big to join in memory, the {@link BlockHashJoin} will compute the join in
     * several passes over the probe larger input partition. In this case, the join container will have to defer the
     * handling of unmatched rows from the table (left/right) that is used as probe input until the last pass over the
     * probe input has been completed.
     * @param side for which input table to set
     * @param defer whether to enable deferred collection of unmatched rows
     * @throws CanceledExecutionException
     */
    public void setDeferUnmatchedRows(final InputTable side, final boolean defer) throws CanceledExecutionException {

//        System.out.println(String.format("JoinContainer.setDeferUnmatchedRows(%s) to %s", side, defer));

        int resultType = side.isLeft() ? LEFT_OUTER : RIGHT_OUTER;
        boolean previouslyDeferred = m_unmatchedRows[resultType] instanceof DeferredProbeRowHandler;
        if (defer && previouslyDeferred) {
            return;
        }
        if (!defer && previouslyDeferred) {
            // TODO maybe just do m_unmatchedRows[resultType].collectUnmatched();
            throw new IllegalStateException("Can not enable defered unmatched row collection again. "
                + "This would discard the previously collected results.");
        }

        m_unmatchedRows[resultType] = unmatchedRowHandler(side, defer);
    }

    /**
     * @param side
     * @param defer
     * @param resultType
     */
    private UnmatchedRows unmatchedRowHandler(final InputTable side, final boolean defer) {
        BufferedDataTable table =
            m_joinSpecification.getSettings(side).getTable().orElseThrow(IllegalStateException::new);
        ObjLongConsumer<DataRow> handler = side.isLeft() ? this::doAddLeftOuter : this::doAddRightOuter;
        if (defer) {
            return new DeferredProbeRowHandler(table, handler, m_checkCanceled);
        } else {
            return UnmatchedRows.completeIndex(handler);
        }
    }

    /**
     * Implementation for adding a matching row pair.
     *
     * @param left same as for {@link #addMatch(DataRow, long, DataRow, long)}
     * @param leftOrder same as for {@link #addMatch(DataRow, long, DataRow, long)}
     * @param right same as for {@link #addMatch(DataRow, long, DataRow, long)}
     * @param rightOrder same as for {@link #addMatch(DataRow, long, DataRow, long)}
     * @return same as for {@link #addMatch(DataRow, long, DataRow, long)}
     *
     */
    protected abstract boolean doAddMatch(final DataRow left, final long leftOrder, final DataRow right, final long rightOrder);

    /**
     * Implementation for adding an unmatched row from the left table.
     *
     * @param row same as for {@link #addLeftOuter(DataRow, long)}
     * @param offset same as for {@link #addLeftOuter(DataRow, long)}
     * @return same as for {@link #addLeftOuter(DataRow, long)}
     */
    protected abstract boolean doAddLeftOuter(final DataRow row, final long offset);

    /**
     * Implementation for adding an unmatched row from the right table.
     *
     * @param row same as for {@link #addRightOuter(DataRow, long)}
     * @param offset same as for {@link #addRightOuter(DataRow, long)}
     * @return same as for {@link #addRightOuter(DataRow, long)}
     */
    protected abstract boolean doAddRightOuter(final DataRow row, final long offset);

    @Override
    public boolean addMatch(final DataRow left, final long leftOrder, final DataRow right, final long rightOrder) {
        boolean isFreshValue = !m_deduplicateResults || m_caches[MATCHES].put(leftOrder, rightOrder);

        // let the deferred probe row handlers now that both rows are not unmatched
        m_unmatchedRows[LEFT_OUTER].matched(left, leftOrder);
        m_unmatchedRows[RIGHT_OUTER].matched(right, rightOrder);

        if (m_joinSpecification.isRetainMatched() && isFreshValue) {
//            System.out.println(String.format(" +accepted match for offsets %s|%s %s + %s", leftOrder, rightOrder, left, right));
            return doAddMatch(left, leftOrder, right, rightOrder);
        }
        if(m_deduplicateResults && !isFreshValue) {
//            System.out.println(String.format(" -rejected stale match for offsets %s|%s %s + %s", leftOrder, rightOrder, left, right));
        }
        return false;
    }

    @Override
    public boolean addLeftOuter(final DataRow row, final long offset) {
        boolean isFreshValue = !m_deduplicateResults || m_caches[LEFT_OUTER].put(offset, 0);
        if (m_joinSpecification.isRetainUnmatched(InputTable.LEFT) && isFreshValue) {
//            System.out.println(String.format(" +accepted left unmatched row %s (offset %s)", row, offset));
            m_unmatchedRows[LEFT_OUTER].unmatched(row, offset);
            return true;
//            return doAddLeftOuter(row, offset);
        }
        if(m_deduplicateResults && !isFreshValue) {
//            System.out.println(String.format(" -rejected stale left unmatched row %s (offset %s)", row, offset));
        }
        if(!m_joinSpecification.isRetainUnmatched(InputTable.LEFT)) {
//            System.out.println("Not interested in left unmatched.");
        }

        return false;
    }

    @Override
    public boolean addRightOuter(final DataRow row, final long offset) {
        boolean isFreshValue = !m_deduplicateResults || m_caches[RIGHT_OUTER].put(offset, 0);
        if (m_joinSpecification.isRetainUnmatched(InputTable.RIGHT) && isFreshValue) {
//            System.out.println(String.format(" +accepted right unmatched row %s (offset %s)", row, offset));
            m_unmatchedRows[RIGHT_OUTER].unmatched(row, offset);
            return true;
//            return doAddRightOuter(row, offset);
        }
        if(m_deduplicateResults && !isFreshValue) {
//            System.out.println(String.format(" -rejected stale right unmatched row %s (offset %s)", row, offset));
        }
        if(!m_joinSpecification.isRetainUnmatched(InputTable.LEFT)) {
//            System.out.println("Not interested in right unmatched.");
        }
        return false;
    }

    /**
     * @param rightUnmatchedRowProjected an unmatched row from the right table that has been projected down to included
     *            columns already.
     * @return a data row that contains missing values for all included columns of the right table.
     */
    protected DataRow padLeftWithMissing(final DataRow rightUnmatchedRowProjected) {

        int[] includes = m_joinSpecification.columnIncludeIndices(InputTable.LEFT);
        int padCells = includes.length;

        final DataCell[] dataCells = new DataCell[padCells + rightUnmatchedRowProjected.getNumCells()];
        int cell = 0;

        for (int i = 0; i < padCells; i++) {
            dataCells[cell++] = DataType.getMissingCell();
        }
        for (int i = 0; i < rightUnmatchedRowProjected.getNumCells(); i++) {
            dataCells[cell++] = rightUnmatchedRowProjected.getCell(i);
        }

        RowKey newKey = m_joinSpecification.getRowKeyFactory().apply(null, rightUnmatchedRowProjected);
        return new DefaultRow(newKey, dataCells);
    }

    /**
     * @param leftUnmatchedRowProjected an unmatched row from the left table that has been projected down to included
     *            columns already.
     * @return a data row that contains missing values for all included columns of the right table.
     */
    protected DataRow padRightWithMissing(final DataRow leftUnmatchedRowProjected) {

        int[] includes = m_joinSpecification.columnIncludeIndices(InputTable.RIGHT);
        int padCells = includes.length;

        final DataCell[] dataCells = new DataCell[leftUnmatchedRowProjected.getNumCells() + padCells];
        int cell = 0;

        for (int i = 0; i < leftUnmatchedRowProjected.getNumCells(); i++) {
            dataCells[cell++] = leftUnmatchedRowProjected.getCell(i);
        }
        for (int i = 0; i < padCells; i++) {
            dataCells[cell++] = DataType.getMissingCell();
        }

        RowKey newKey = m_joinSpecification.getRowKeyFactory().apply(leftUnmatchedRowProjected, null);
        return new DefaultRow(newKey, dataCells);
    }

    @Override
    public boolean isRetainMatched() {
        return m_joinSpecification.isRetainMatched();
    }

    @Override
    public boolean isRetainUnmatched(final InputTable side) {
        return m_joinSpecification.isRetainUnmatched(side);
    }

    @Override public boolean isDeduplicateResults() {
        return m_deduplicateResults;
    }

    @Override
    public JoinSpecification getJoinSpecification() {
        return m_joinSpecification;
    }

    @Override
    public void setJoinSpecification(final JoinSpecification joinSpecification) {
        m_joinSpecification = joinSpecification;
    }

    /**
     * Stores whether a given row offset combination was seen before. Is used to determine whether there was a previous
     * invocation of {@link JoinContainer#addMatch(DataRow, long, DataRow, long)},
     * {@link JoinContainer#addLeftOuter(DataRow, long)}, or {@link JoinContainer#addRightOuter(DataRow, long)}. In case
     * there was a previous invocation with the same row offsets and {@link JoinContainer#isDeduplicateResults()}, the
     * join result is rejected.
     *
     * @author Carl Witt, KNIME AG, Zurich, Switzerland
     */
    protected class RowOffsetCombinationSet {

        TLongHashSet m_set = new TLongHashSet();

        /**
         * @param leftRowOffset a 32-bit unsigned integer, i.e., at most ~4G
         * @param rightRowOffset a 32-bit unsigned integer, i.e., at most ~4G
         * @return whether the combination of row offsets is new, i.e., hasn't been put before
         */
        public boolean put(final long leftRowOffset, final long rightRowOffset) {
            return m_set.add(OrderedRow.combinedOffsets(leftRowOffset, rightRowOffset));
        }

    }

}
