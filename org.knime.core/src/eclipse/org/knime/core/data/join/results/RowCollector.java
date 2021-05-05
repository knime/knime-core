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
 *   Apr 30, 2021 (carlwitt): created
 */
package org.knime.core.data.join.results;

import org.knime.core.data.DataRow;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.CanceledExecutionException.CancelChecker;

/**
 * A handler for unmatched rows. In the simplest case, it just outputs a row to {@link JoinResult} when passing a row to
 * {@link #unmatchedLeft(DataRow, long)}. If a row can be unmatched in one situation and matched in another (e.g., row
 * is joined against an incomoplete index or joined in multiple partitions), this handler provides a
 * {@link #matched(DataRow, long, DataRow, long)} that cancels a previous {@link #unmatchedLeft(DataRow, long)} call.
 * This will also make all future {@link #unmatchedLeft(DataRow, long)} calls on the same row ineffective. <br/>
 * <br/>
 * If a row is send to disk the subsequent {@link DiskBucket#join(DiskBucket, JoinResult)} will perform a
 * {@link BlockHashJoin#join(JoinResult, UnmatchedRows, UnmatchedRows)} that may generate
 * {@link #matched(DataRow, long, DataRow, long)} events, such that unmatched probe and hash rows can only be collected
 * after {@link HybridHashJoin#phase3}. This is done via {@link #collectUnmatched()}.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public interface RowCollector {

    /**
     * @param matchedProbeRowOffset this is the row's offset in the partition table, no need store or extract row
     *            offsets from the super table.
     */
    void matched(long matchedProbeRowOffset);

    /**
     *
     * Several calls to this method with identical input will <b>not</b> add the row several times, since this just
     * marks a given row as unmatched.
     *
     * Since we're joining against partial indexes of the hash input, not finding a match isn't informative since the
     * probe row could be matched during a future pass over the probe input, when different rows of the hash input have
     * been indexed.
     */
    boolean unmatched(DataRow unmatchedProbeRow, long unmatchedProbeRowOffset);

    void lowMemory();

    /**
     * Outputs unmatched rows into the handler {@link #m_unmatched} passed during construction. This method is meant to
     * be called only once. It is assumed that unmatched rows are produced and collected in the end, NOT produced,
     * collected, produced, ... This clears all data structures.
     *
     * @throws CanceledExecutionException if the {@link CancelChecker} passed at construction signaled that execution is
     *             canceled
     *
     * TODO change contract to something that gets a handler?
     */
    void collectUnmatched() throws CanceledExecutionException;

    static RowCollector passThrough() {
        return new RowCollector() {

            @Override
            public boolean unmatched(final DataRow unmatchedProbeRow, final long unmatchedProbeRowOffset) {
                return true;
            }

            @Override
            public void matched(final long matchedProbeRowOffset) {
                /**
                 * By default, finding a match for the probe row requires no special action other than reporting the
                 * join results to {@link JoinResults}. However, in a {@link BlockHashJoin}, we record which rows have
                 * been matched in at least one pass to output the unmatched rows in the end.
                 */
            }

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