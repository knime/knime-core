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

import org.knime.core.data.DataRow;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.CanceledExecutionException.CancelChecker;

/**
 * When joining against incomplete indexes, we have to wait until we can decide whether a row is unmatched, since a row
 * might be matched in a different partition (in the disjunctive case, rows go to several partitions) or a future pass
 * over the probe input in {@link BlockHashJoin#join(JoinResult, MatchStrategy, MatchStrategy)}. This unmatched row
 * handler stores the offsets of the rows for which a match was found. {@link MatchStrategy#collectUnmatched()} is used
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
@SuppressWarnings("javadoc")
public class DeferredUnmatchedRowCollector implements UnmatchedRowCollector {

    /**
     * The i-th bit is set if the i-th row in the probe input was matched to a row in a partial hash index at least
     * once, e.g., during a {@link BlockHashJoin#singlePass(BufferedDataTable, HashIndex)}.
     */
    private final BitSet m_wasMatched = new BitSet();

    private final CancelChecker m_checkCanceled;

    private final BufferedDataTable m_probeInput;

    /**
     * Keeps the unmatched rows in memory. If {@link #lowMemory()} is called, this data structure is discarded and the
     * unmatched rows need to be collected in an additional pass over the input table {@link #m_probeInput}, collecting
     * a row every time its offset is not contained in {@link #m_wasMatched}.
     *
     * A {@link TreeMap} is used to output the candidates in sorted order (according to their offset in the containing
     * table. This is for instance useful when joining with multiple passes over an input table, which will result in
     * calls to {@link #unmatched(DataRow, long)} with non-sorted row offset parameters.
     */
    private TreeMap<Long, DataRow> m_candidates = new TreeMap<>();

    /**
     * @param probeInput the table used as probe input, as processed with {@link MatchStrategy#matched(DataRow, long, DataRow, long)}
     *            and {@link #unmatchedLeft(DataRow, long)}
     * @param checkCanceled a way to check whether execution was aborted
     */
    public DeferredUnmatchedRowCollector(final BufferedDataTable probeInput, final CancelChecker checkCanceled) {
        m_probeInput = probeInput;
        m_checkCanceled = checkCanceled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void matched(final long matchedProbeRowOffset) {
        if (m_candidates != null) {
            m_candidates.remove(matchedProbeRowOffset);
        }
        m_wasMatched.set((int)matchedProbeRowOffset);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean unmatched(final DataRow unmatchedProbeRow, final long unmatchedProbeRowOffset) {
        if (m_candidates != null) {
            // if the row was matched before, it was removed from the data structure afterwards
            boolean previouslyRemoved = m_wasMatched.get(((int)unmatchedProbeRowOffset));
            // don't put the row in the data structure another time
            if (!previouslyRemoved) {
                // this will deduplicate multiple additions to the collector (deduplication by comparing row offsets)
                m_candidates.putIfAbsent(unmatchedProbeRowOffset, unmatchedProbeRow);
            }
        }
        // signal not to add the row to the results, will be added upon collectUnmatched
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void lowMemory() {
        m_candidates = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void collectUnmatched(final RowHandler handler) throws CanceledExecutionException {

        // use the cached rows to produce output
        if (m_candidates != null) {
            while (!m_candidates.isEmpty()) {
                m_checkCanceled.checkCanceled();
                Entry<Long, DataRow> unmatchedRow = m_candidates.pollFirstEntry();
                handler.accept(unmatchedRow.getValue(), unmatchedRow.getKey());
            }
            m_candidates.clear();
        } else {
            // do a single pass over the probe input, output every unmatched row into the join results
            RowHandler outputIfUnmatched = (datarow, originalRowOffset) -> {
                if (!m_wasMatched.get((int)originalRowOffset)) {
                    handler.accept(datarow, originalRowOffset);
                }
            };
            JoinResult.enumerateWithResources(m_probeInput, outputIfUnmatched, m_checkCanceled);
        }
        m_wasMatched.clear();
    }

}