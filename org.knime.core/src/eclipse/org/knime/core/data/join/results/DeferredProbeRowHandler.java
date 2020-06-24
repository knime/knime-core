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
 *   Jun 22, 2020 (carlwitt): created
 */
package org.knime.core.data.join.results;

import java.util.BitSet;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.ObjLongConsumer;

import org.knime.core.data.DataRow;
import org.knime.core.data.join.HybridHashJoin;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.CanceledExecutionException.CancelChecker;

/**
 * A handler for unmatched rows. In the simplest case, it just outputs a row to {@link JoinResults} when passing a row
 * to {@link #unmatched(DataRow, long)}. If a row can be unmatched in one situation and matched in another (e.g., row is
 * joined against an incomoplete index or joined in multiple partitions), this handler provides a
 * {@link #matched(DataRow, long)} that cancels a previous {@link #unmatched(DataRow, long)} call. This will also make
 * all future {@link #unmatched(DataRow, long)} calls on the same row ineffective. <br/>
 * <br/>
 * If a row is send to disk the subsequent {@link DiskBucket#join(DiskBucket, JoinResults)} will perform a
 * {@link BlockHashJoin#join(JoinResults, UnmatchedRows, UnmatchedRows)} that may generate
 * {@link #matched(DataRow, long)} events, such that unmatched probe and hash rows can only be collected after
 * {@link HybridHashJoin#phase3}. This is done via {@link #collectUnmatched()}.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
interface UnmatchedRows {

    public void matched(DataRow matchedProbeRow, long matchedProbeRowOffset);

    public void unmatched(DataRow unmatchedProbeRow, long unmatchedProbeRowOffset);

//    public void deferred(DataRow maybeMatchedProbeRow, long maybeMatchedProbeRowOffset);

    /**
     * @throws CanceledExecutionException if execution is canceled during collection of the rows
     */
    public void collectUnmatched() throws CanceledExecutionException;

    /**
     * Indicates that heap space is running low and we should save heap space if possible.
     */
    public void lowMemory();

    /**
     * An default unmatched row handler for the conjunctive join case.
     *
     * @param unmatched what to do with unmatched probe rows, e.g., put them in left/right unmatched rows of
     *            {@link JoinResults}
     *
     * @return a default row handler used in {@link HybridHashJoin} in a conjunctive join. This is the simple case where
     *         hash indexes are comprehensive in the sense that unmatched probe rows can't turn out matched later on.
     *         Similarly, unmatched hash rows can't be part of another partition's hash index that will cause them to be
     *         matched later on. In this case, all unmatched rows can go straight to the join container.
     */
    public static UnmatchedRows completeIndex(final ObjLongConsumer<DataRow> unmatched) {
        return new UnmatchedRows() {

            @Override
            public void unmatched(final DataRow unmatchedProbeRow, final long unmatchedProbeRowOffset) {
                unmatched.accept(unmatchedProbeRow, unmatchedProbeRowOffset);
            }

            @Override
            public void matched(final DataRow matchedProbeRow, final long matchedProbeRowOffset) {
                /**
                 * By default, finding a match for the probe row requires no special action other than reporting the
                 * join results to {@link JoinResults}. However, in a {@link BlockHashJoin}, we record which rows have
                 * been matched in at least one pass to output the unmatched rows in the end.
                 */
            }

//            @Override
//            public void deferred(final DataRow maybeMatchedProbeRow, final long maybeMatchedProbeRowOffset) {
//                /**
//                 * If a probe row goes to disk, we will see whether it is unmatched when joining the disk buckets
//                 */
//            }

            @Override
            public void collectUnmatched() {
                /**
                 * If probe rows are output directly, we don't need to collect them in the end.
                 */
            }

            @Override
            public void lowMemory() {
                /**
                 * This requires no additional memory and thus no action is needed.
                 */
            }
        };
    }

}

/**
 * When joining against incomplete indexes, we have to wait until we can decide whether a row is unmatched, since a row
 * might be matched in a different partition (in the disjunctive case, rows go to several partitions) or a future pass
 * over the probe input in {@link BlockHashJoin#join(JoinResults, UnmatchedRows, UnmatchedRows)}. This unmatched row
 * handler stores the offsets of the rows for which a match was found. {@link UnmatchedRows#collectUnmatched()} is used
 * in the end to retrieve the unmatched rows.
 *
 * <h1>Internals</h1>
 *
 * If memory runs low, another pass over the probe input is used to retrieve the unmatched probe rows by comparing their
 * offsets to the offsets for which no match was found. Until memory runs low, as indicated by {@link #lowMemory()}
 * unmatched probe rows are cached, allowing us to save an additional pass over the probe input, if {@link #lowMemory()}
 * doesn't occur until the end.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
class DeferredProbeRowHandler implements UnmatchedRows {

    /**
     * The i-th bit is set if the i-th row in the probe input was matched to a row in a partial hash index at least
     * once, e.g., during a {@link BlockHashJoin#singlePass(BufferedDataTable, HashIndex)}.
     */
    BitSet m_wasMatched = new BitSet();
//    BitSet m_deferred = new BitSet();

    private TreeMap<Long, DataRow> m_candidates = new TreeMap<>();

    private final CancelChecker m_checkCanceled;

    private final ObjLongConsumer<DataRow> m_unmatched;

    private final BufferedDataTable m_probeInput;

    /**
     * @param probeInput the table used as probe input, as processed with {@link UnmatchedRows#matched(DataRow, long)} and
     *            {@link #unmatched(DataRow, long)}
     * @param unmatched what to do with the unmatched probe rows
     * @param checkCanceled a way to check whether execution was aborted
     */
    public DeferredProbeRowHandler(
        final BufferedDataTable probeInput, final ObjLongConsumer<DataRow> unmatched,
        final CancelChecker checkCanceled) {
        m_probeInput = probeInput;
        m_unmatched = unmatched;
        m_checkCanceled = checkCanceled;
    }

    /**
     * @param matchedProbeRowOffset this is the row's offset in the partition table, no need store or extract row
     *            offsets from the super table.
     */
    @Override
    public void matched(final DataRow matchedProbeRow, final long matchedProbeRowOffset) {
        if (m_candidates != null) {
            m_candidates.remove(matchedProbeRowOffset);
        }
        m_wasMatched.set((int)matchedProbeRowOffset);
//        System.out.println("[DFR] match: " + matchedProbeRow + " offset " + matchedProbeRowOffset);
    }

    /**
     * Since we're joining against partial indexes of the hash input, not finding a match isn't informative since the
     * probe row could be matched during a future pass over the probe input, when different rows of the hash input have
     * been indexed.
     */
    @Override
    public void unmatched(final DataRow unmatchedProbeRow, final long unmatchedProbeRowOffset) {
        if (m_candidates != null) {
            // if the row was matched before, it was removed from the data structure afterwards
            boolean previouslyRemoved = m_wasMatched.get(((int)unmatchedProbeRowOffset));
            // don't put the row in the data structure another time
            if (!previouslyRemoved) {
                // TODO the candidate can be put in the map twice, but shouldn't be a problem
                m_candidates.put(unmatchedProbeRowOffset, unmatchedProbeRow);
//                System.out.println("[DFR] candidate: " + unmatchedProbeRow + " offset " + unmatchedProbeRowOffset);
            }
        }
    }

    @Override public void lowMemory() {
        m_candidates = null;
    }

    /**
     * Outputs unmatched rows into the handler {@link #m_unmatched} passed during construction.
     * This method is meant to be called only once. It is assumed that unmatched rows are produced and collected in the
     * end, NOT produced, collected, produced, ...
     * This clears all data structures.
     * @throws CanceledExecutionException if the {@link CancelChecker} passed at construction signaled that execution is
     *             canceled
     */
    @Override
    public void collectUnmatched() throws CanceledExecutionException {
//        System.out.println(String.format("Deferred unmatched probe row collection%n candidates %s%n wasMatched %s", m_candidates, m_wasMatched)); // %ndeferred %s , m_deferred

        if (m_candidates != null) {
            while (!m_candidates.isEmpty()) {
                m_checkCanceled.checkCanceled();
                Entry<Long, DataRow> unmatchedRow = m_candidates.pollFirstEntry();
                m_unmatched.accept(unmatchedRow.getValue(), unmatchedRow.getKey());
            }
            m_candidates.clear();
        } else {
            // do a single pass over the probe input, output every unmatched row into the join results
            ObjLongConsumer<DataRow> outputIfUnmatched = (datarow, originalRowOffset) -> {
                if (!m_wasMatched.get((int)originalRowOffset)) { // && !m_deferred.get((int)originalRowOffset)
                    System.out.println("[DFR] deferred collection " + datarow + " offset " + originalRowOffset);
                    m_unmatched.accept(datarow, originalRowOffset);
                }
            };
            JoinResults.enumerateWithResources(m_probeInput, outputIfUnmatched, m_checkCanceled);
        }
        m_wasMatched.clear();
    }

}