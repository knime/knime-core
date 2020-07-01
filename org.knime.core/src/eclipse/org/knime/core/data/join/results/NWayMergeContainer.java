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
 *   25.11.2009 (Heiko Hofer): created
 */
package org.knime.core.data.join.results;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToLongFunction;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.join.HybridHashJoin;
import org.knime.core.data.join.JoinImplementation;
import org.knime.core.data.join.JoinSpecification;
import org.knime.core.data.join.JoinSpecification.InputTable;
import org.knime.core.data.join.JoinSpecification.OutputRowOrder;
import org.knime.core.data.join.OrderedRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.CanceledExecutionException.CancelChecker;
import org.knime.core.node.ExecutionContext;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

/**
 * <h1>Usage</h1> A join container for the output row orders {@link OutputRowOrder#DETERMINISTIC} and
 * {@link OutputRowOrder#LEFT_RIGHT} (if the left input table happens to be the probe input table). Collects matches and
 * unmatched data rows for {@link HybridHashJoin}.
 *
 * <h1>Internals</h1> Exploits the partial row order in which matches and unmatched data rows are produced during the
 * hybrid hash join to sort in linear time.
 *
 * <h1>Notes</h1> The result table chunks managed here are somewhat related to the input table chunks managed by the
 * DiskBackedHashPartitions in {@link HybridHashJoin}. However, results can be produced during all three phases of the
 * hybrid hash join. Some output chunks will thus be produced from joining input hash partitions, but additional sorted
 * chunks can be produced during all phases of the hybrid hash join (e.g., unmatched hash rows can be produced during
 * phase 1, when the hash input table is indexed, during phase 2, when the probe input is processed against in-memory
 * indexes, and in phase 3 when partitions on disk are joined). <br/>
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 4.2
 */
public class NWayMergeContainer extends JoinContainer {

    /**
     * Multiple tables containing results in sorted order. {@link #m_chunks}[{@link #MATCHES}] contains the inner join
     * results, {@link #m_chunks}[{@link #LEFT_OUTER}] contains the unmatched rows from the left table, etc.
     */
    private final SortedChunks[] m_chunks;

    /** {@link #m_tables}[{@link #MATCHES}] contains the final result for the inner join, etc. */
    private final BufferedDataTable[] m_tables;

    /** @see NWayMergeContainer#NWayMergeContainer(JoinSpecification, ExecutionContext, boolean) */
    private final boolean m_leftIsProbe;

    /**
     *
     * @param joinImplementation
     * @param exec
     * @param leftIsProbe the row offset of the probe table is used to sort output rows. Since we operate on left and
     *            right tables here, we need to know which is the probe table. If false, the right input is the probe
     *            table.
     * @param deduplicateResults
     * @param deferUnmatchedRows
     */
    public NWayMergeContainer(final JoinImplementation joinImplementation, final ExecutionContext exec,
        final boolean leftIsProbe, final boolean deduplicateResults, final boolean deferUnmatchedRows) {
        super(joinImplementation, deduplicateResults, deferUnmatchedRows);
        m_leftIsProbe = leftIsProbe;

        // create output containers
        m_chunks = Arrays.stream(m_outputSpecs)
                .map(SortedChunks::new)
                .toArray(SortedChunks[]::new);

        m_tables = new BufferedDataTable[3];

    }

    @Override
    public boolean doAddMatch(final DataRow left, final long leftOffset, final DataRow right, final long rightOffset) {
        DataRow match = m_joinSpecification.rowJoin(left, right);
        long probeRowOffset = m_leftIsProbe ? leftOffset : rightOffset;
        m_chunks[MATCHES].addSorted(match, probeRowOffset);
        // TODO hiliting
        //        addHiliteMapping(InputTable.LEFT, ResultType.MATCHES, left.getKey(), match.getKey());
        //        addHiliteMapping(InputTable.RIGHT, ResultType.MATCHES, right.getKey(), match.getKey());
        return true;
    }

    @Override
    public boolean doAddLeftOuter(final DataRow row, final long leftOffset) {
        long probeRowOffset = m_leftIsProbe ? leftOffset : Long.MAX_VALUE;
        m_chunks[LEFT_OUTER].addSorted(m_joinSpecification.rowProjectOuter(InputTable.LEFT, row), probeRowOffset);
        // TODO hiliting
        //        addHiliteMapping(InputTable.LEFT, ResultType.MATCHES, left.getKey(), match.getKey());
        //        addHiliteMapping(InputTable.RIGHT, ResultType.MATCHES, right.getKey(), match.getKey());
        return true;
    }

    @Override
    public boolean doAddRightOuter(final DataRow row, final long rightOffset) {
        long probeRowOffset = m_leftIsProbe ? Long.MAX_VALUE : rightOffset;
        m_chunks[RIGHT_OUTER].addSorted(m_joinSpecification.rowProjectOuter(InputTable.RIGHT, row), probeRowOffset);
        // TODO hiliting
        //        addHiliteMapping(InputTable.LEFT, ResultType.MATCHES, left.getKey(), match.getKey());
        //        addHiliteMapping(InputTable.RIGHT, ResultType.MATCHES, right.getKey(), match.getKey());
        return true;
    }

    private BufferedDataTable get(final int resultType) throws CanceledExecutionException {

        if (m_tables[resultType] == null) {
            if (resultType != MATCHES) {
                m_unmatchedRows[resultType].collectUnmatched();
            }
            // perform n-way merge of the sorted chunks, remove the auxiliary offset column
            m_tables[resultType] = m_chunks[resultType].getSortedTable();
        }

        return m_tables[resultType];
    }

//    @Override
//    public BufferedDataTable getMatches() throws CanceledExecutionException {
//        return get(MATCHES);
//    }
//
//    @Override
//    public BufferedDataTable getLeftOuter() throws CanceledExecutionException {
//        return get(LEFT_OUTER);
//    }
//
//    /**
//     * The unmatched rows of the right input table, in order of appearance in the right input table.
//     */
//    @Override
//    public BufferedDataTable getRightOuter() throws CanceledExecutionException {
//        return get(RIGHT_OUTER);
//    }

    @Override
    public void sortedChunkEnd() {
        for (int i = 0; i < 3; i++) {
            if (m_chunks[i] != null) {
                m_chunks[i].sortedChunkEnd();
            }
        }
    }

//    /**
//     * Returns the concatenation of matched rows, left unmatched rows, and right unmatched rows. Which rows are included
//     * depends on the join specification.<br/>
//     * The spec of the returned table is the same as that of {@link #getMatches()}. Pads left unmatched and right
//     * unmatched rows with missing values to fill the missing columns.
//     *
//     * @return matched and unmatched rows concatenated in one table with {@link JoinSpecification#specForMatchTable()}.
//     * @throws CanceledExecutionException
//     */
//    @Override
//    public BufferedDataTable getSingleTable() throws CanceledExecutionException {
//
//        // if only the matches are needed, we can use the output table directly
//        if (m_joinSpecification.isRetainMatched()
//                && !m_joinSpecification.isRetainUnmatched(InputTable.LEFT)
//                && !m_joinSpecification.isRetainUnmatched(InputTable.RIGHT)) {
//            return getMatches();
//        } else { // otherwise we need to pad and concat the unmatched rows
//
//            BufferedDataContainer container = m_exec.createDataContainer(m_joinSpecification.specForMatchTable());
//            CancelChecker checkCanceled = CancelChecker.checkCanceledPeriodically(m_exec);
//
//            if (m_joinSpecification.isRetainMatched()) {
//                JoinResults.iterateWithResources(getMatches(), container::addRowToTable, checkCanceled);
//            }
//            if (m_joinSpecification.isRetainUnmatched(InputTable.LEFT)) {
//                JoinResults.iterateWithResources(getLeftOuter(),
//                    row -> container.addRowToTable(leftToSingleTableFormat(row)), checkCanceled);
//            }
//            if (m_joinSpecification.isRetainUnmatched(InputTable.RIGHT)) {
//                JoinResults.iterateWithResources(getRightOuter(),
//                    row -> container.addRowToTable(rightToSingleTableFormat(row)), checkCanceled);
//            }
//            container.close();
//            return container.getTable();
//        }
//
//    }

    /**
     * This is an abstraction for collecting similar rows that are produced in sorted chunks. The idea is to get a
     * faster sort using an n-way merge without having to care too much about the internals. This class is used by
     * repeatedly calling {@link #addSorted(DataRow)}. All rows added between two {@link #sortedChunkEnd()} calls must
     * be in natural join order.
     */
    private class SortedChunks {

        /**
         * The format of the result table, as returned by {@link #getSortedTable()}. If this object is used for sorting
         * matching rows, this spec will be identical with {@link NWayMergeContainer#getMatches()}, and likewise for
         * {@link NWayMergeContainer#getLeftOuter()} and {@link NWayMergeContainer#getRightOuter()}
         */
        final DataTableSpec m_resultSpec;

        /** The intermediate table format used for sorting. Adds one long column to store the probe row offset. */
        final DataTableSpec m_workingSpec;

        BufferedDataContainer m_currentSortedChunk;

        final List<BufferedDataTable> m_sortedChunks = new ArrayList<>();

        long m_rowsAdded = 0;

        SortedChunks(final DataTableSpec resultSpec) {
            m_resultSpec = resultSpec;
            // add one long column for the probe row offset to each output spec for n-way merge
            m_workingSpec = OrderedRow.withOffset(m_resultSpec);
        }

        /**
         * Adds a row in ascending probeRowOffset order, i.e., between consecutive calls to {@link #sortedChunkEnd()},
         * every probeRowOffset must be larger than probeRowOffset passed to the previous call of
         * {@link #addSorted(DataRow, long)}.
         *
         * @param row a join result, either match or unmatched row
         * @param probeRowOffset the offset of the probe row involved in the join result (Long.MAX_VALUE for a unmatched
         *            hash row)
         */
        public void addSorted(final DataRow row, final long probeRowOffset) {
            if (m_currentSortedChunk == null) {
                m_currentSortedChunk = m_exec.createDataContainer(m_workingSpec);
            }
            m_currentSortedChunk.addRowToTable(OrderedRow.withOffset(row, probeRowOffset));
            m_rowsAdded++;
        }

        public void sortedChunkEnd() {
            if (m_currentSortedChunk != null) {
                m_currentSortedChunk.close();
                m_sortedChunks.add(m_currentSortedChunk.getTable());
                m_currentSortedChunk = null;
            }
        }

        /**
         * Performs an n-way merge that merges the sorted chunks into a sorted table. <br/>
         * O(n (b log b)) ~ O(n) complexity where b is the number of chunks (which is a constant in the hybrid hash join
         * case).
         *
         * @return the sorted concatenation of the sorted chunks
         * @throws CanceledExecutionException
         */
        private BufferedDataTable nWayMerge() throws CanceledExecutionException {

            final BufferedDataContainer sorted = m_exec.createDataContainer(m_workingSpec);

            // create iterators that wrap the table iterators with a peek operation,
            // needed for sorting tables according to output row order of their first row
            @SuppressWarnings("unchecked")
            final PeekingIterator<DataRow>[] iterators = m_sortedChunks.stream()
                .map(BufferedDataTable::iterator)
                .map(Iterators::peekingIterator)
                .toArray(PeekingIterator[]::new);

            // we want to sort iterators by probe row offset, create an according comparator
            // when an iterator is depleted, it technically isn't comparable anymore, but we just sort it to the end
            ToLongFunction<PeekingIterator<DataRow>> getRowOffset = iterator -> {
                if (iterator.hasNext()) {
//                    System.out.println(String.format("OFFSET_EXTRACTOR(iterator.peek())=%s", OrderedRow.OFFSET_EXTRACTOR.applyAsLong(iterator.peek())));
                    return OrderedRow.OFFSET_EXTRACTOR.applyAsLong(iterator.peek());
                } else {
//                    System.out.println(String.format("OFFSET_EXTRACTOR(iterator.peek())=%s", Long.MAX_VALUE));
                    return Long.MAX_VALUE;
                }
            };
            Comparator<PeekingIterator<DataRow>> probeRowOffset = Comparator.comparingLong(getRowOffset);

            CancelChecker periodicCheck = CancelChecker.checkCanceledPeriodically(m_exec);

            // true if at least one of the iterators isn't depleted
            boolean pendingRows;
            do {

                periodicCheck.checkCanceled();

                // take rows from table with smallest probe row offset
                Arrays.sort(iterators, probeRowOffset);
                pendingRows = false;
                for (int i = 0; i < iterators.length; i++) {

                    // all empty iterators are sorted to the back of the array, only non-empty iterators from now
                    if (!iterators[i].hasNext()) {
                        break;
                    }

                    // Take rows from current chunk as long as the probe row offsets are contiguous
                    // This automatically sorts by hash row offset, because for each probe row, matches are produced
                    // one after each other, in hash row order (afterwards, no more matches for that probe row can occur,
                    // because each probe row is joined only once, with the only matching partition).
                    // Thus, we don't have to worry about non-deterministic effects due to different in-memory partitions.
                    long lastProbeRowOffset = getRowOffset.applyAsLong(iterators[i]);
                    long nextProbeRowOffset = lastProbeRowOffset;
                    do {
                        // add the result row, remove the offset column
                        // TODO what's faster? filter row? copy all but first? use column rearranger?
                        DataRow nextRow = iterators[i].next();
//                        System.out.println(String.format(" adding nextRow=%s", nextRow));
                        sorted.addRowToTable(nextRow);
                        lastProbeRowOffset = nextProbeRowOffset;
                        nextProbeRowOffset = getRowOffset.applyAsLong(iterators[i]);

                    } while (nextProbeRowOffset == lastProbeRowOffset + 1);
                    pendingRows = pendingRows || iterators[i].hasNext();
                }
            } while (pendingRows);
            sorted.close();

            // remove auxiliary column for sorting
            final ColumnRearranger cr = OrderedRow.removeOffset(m_workingSpec);
            return m_exec.createColumnRearrangeTable(sorted.getTable(), cr, m_exec);
        }

        /**
         * @return the rows, sorted in ascending order, as provided by {@link OrderedRow#OFFSET_EXTRACTOR}
         * @throws CanceledExecutionException
         */
        public BufferedDataTable getSortedTable() throws CanceledExecutionException {
            // if no rows have been added, return an empty table
            if (m_rowsAdded == 0) {
                BufferedDataContainer empty = m_exec.createDataContainer(m_workingSpec);
                empty.close();
                return empty.getTable();
            }

            //cleanup: close current chunk if it is open
            if (m_currentSortedChunk != null) {
                sortedChunkEnd();
            }
            return nWayMerge();
        }

    }

}

