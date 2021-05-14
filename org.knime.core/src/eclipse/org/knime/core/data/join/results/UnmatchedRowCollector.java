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
 * A handler for unmatched rows. In the simplest case, it directly outputs all rows passed to
 * {@link #unmatched(DataRow, long)}. See {@link DeferredUnmatchedRowCollector} for a row collector that implements deferred
 * collection of unmatched rows.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public interface UnmatchedRowCollector {

    /**
     * @param matchedProbeRowOffset this is the row's offset in the partition table, no need store or extract row
     *            offsets from the super table.
     */
    void matched(long matchedProbeRowOffset);

    /**
     * Suggest that a given row is unmatched. If the same has been marked as {@link #matched(long)} before, this will
     * have no effect. If in the future the same row is marked as {@link #matched(long)}, the row will not be output
     * during {@link #collectUnmatched(RowHandler)}.
     *
     * Several calls to this method with identical rows will <b>not</b> add the row several times, since this just marks
     * a given row as unmatched.
     *
     * This is useful when joining against partial indexes of the hash input (due to insufficient heap space) in which
     * case a probe row can be matched during a future pass over the probe input with a different index. Also useful
     * during match any, when the result is collected after performing several match all joins, in which case the
     * unmatched rows can only be collected after the last join.
     *
     * @param unmatchedProbeRow a row that doesn't have a matching row in the other table
     * @param unmatchedProbeRowOffset the row's offset in the table it is contained in
     * @return false if the row has been previously marked as {@link #matched(long)}, true otherwise.
     */
    boolean unmatched(DataRow unmatchedProbeRow, long unmatchedProbeRowOffset);

    /**
     * Suggest freeing up heap space.
     */
    void lowMemory();

    /**
     * Processes unmatched rows using the given handler. This method is meant to be called only once, i.e., it is
     * assumed that unmatched rows are produced and collected in the end, NOT produced, collected, produced, ...
     *
     * @param handler process each row that was previously marked with {@link #unmatched(DataRow, long)} and not
     *            subsequently unmarked with {@link #matched(long)}. No guarantees on the processing order.
     *
     * @throws CanceledExecutionException if the {@link CancelChecker} passed at construction signaled that execution is
     *             canceled
     */
    void collectUnmatched(RowHandler handler) throws CanceledExecutionException;

    /**
     * @return a default row collector
     */
    static UnmatchedRowCollector passThrough() {
        return new UnmatchedRowCollector() {

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
            public void collectUnmatched(final RowHandler handler) {
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