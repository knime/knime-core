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
 *   Jun 17, 2020 (carlwitt): created
 */
package org.knime.core.data.join.implementation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.function.Function;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.LongValue;
import org.knime.core.data.join.JoinSpecification;
import org.knime.core.data.join.JoinSpecification.InputTable;
import org.knime.core.data.join.JoinSpecification.OutputRowOrder;
import org.knime.core.data.join.JoinTableSettings;
import org.knime.core.data.join.results.JoinResult;
import org.knime.core.data.join.results.RowHandlerCancelable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.CanceledExecutionException.CancelChecker;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.custom_hash.TObjectIntCustomHashMap;
import gnu.trove.map.hash.TCustomHashMap;
import gnu.trove.strategy.HashingStrategy;

/**
 * Index for rows in a table. Provides fast lookup of join partners of a row by accessing the values in its join
 * columns.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
class HashIndex {

    /**
     * Creates a new list of rows in case there's none already associated to a certain combination of join column values
     * in {@link #addHashRow(JoinTuple, DataRow, long)}. Must produce a new instance of the type the {@link #m_index}
     * map to (=hold as values).
     */
    private static final Function<DataCell[], List<DataRow>> newRowList = k -> new ArrayList<DataRow>();

    /** Puts the join results here. */
    final JoinResult<?> m_joinContainer;

    /**
     * The hash rows in order of their addition to the index (which is hash input row order). <br/>
     * This is used to flush hash rows to disk in the row order of the hash input, which allows us to do an n-way merge
     * later on. This is also used to access unmatched hash rows via their offset in {@link #m_matched}.
     */
    private final List<DataRow> m_rows = new ArrayList<>();

    /**
     * The offset of the i-th data row in {@link #m_rows}.
     */
    private final TLongArrayList m_rowOffsets = new TLongArrayList();

    /**
     * Makes hash input rows accessible via join column value combinations.
     */
    private final TCustomHashMap<DataCell[], List<DataRow>> m_index;

    /**
     * Whether to remember which hash rows have had join partners in the probe table to be able to output unmatched hash
     * rows.
     */
    private final boolean m_trackMatchedHashRows;

    /**
     * The i-th bit stores whether the i-th entry of m_rows has had a join partner in the probe table so far, as found
     * during a previous call to {@link #joinSingleRow(JoinTuple, DataRow, JoinResult)}.
     *
     * Requires about 1 MB space per 10M rows.
     */
    private final BitSet m_matched;

    /**
     * Maps a hash row to its offset in {@link #m_rows}. This internal offset is in turn used to mark the rows that have
     * been matched to a probe row, by setting the corresponding bit in {@link #m_matched}. This avoids wrapping
     * DataRows in another object that holds the data row's offset in its source table. Alternatively, one could also
     * introduce another map from DataRow to long but these internal offsets are also convenient for addressing in
     * {@link #m_matched} as they are more compact.
     */
    private final TObjectIntCustomHashMap<DataRow> m_hashrowInternalOffsets;

    private final JoinTableSettings m_probeSettings;

    private final JoinSpecification m_joinSpecification;

    /**
     * Evaluated to check whether the execution was canceled. The supplier might ignore every n-th evaluation for
     * performance reasons.
     */
    private final CancelChecker m_checkCanceled;

    private InputTable m_hashSide;

    /**
     *
     * @param joinSpecification
     * @param joinContainer
     * @param unmatched
     * @param comprehensiveIndex if false, indicates that this is a non-comprehensive index, e.g., in the disjunctive
     *            case. A row being unmatched is then not necessarily unmatched (could be matched against another
     *            partition's index or the next iteration's index)
     * @param hashSide
     * @param checkCanceled
     */
    /**
     *
     * @param joinSpecification tells the hash index whether we're interested in left/right unmatched rows, how to
     *            extract join tuples from data rows and whether it's a conjunctive or a disjunctive join.
     * @param joinContainer to receive matched and unmatched rows
     * @param hashSide the join specification talks about left and right tables. hashside tells the index which side is
     *            being indexed and which side is used as probe
     * @param checkCanceled to enable interrupting expensive operations, such as
     *            {@link #forUnmatchedHashRows(RowHandlerCancelable)} and (in extreme cases)
     *            {@link #joinSingleRow(DataRow, long)}
     */
    @SuppressWarnings("serial")
    HashIndex(final JoinSpecification joinSpecification, final JoinResult<?> joinContainer,
        final JoinSpecification.InputTable hashSide, final CancelChecker checkCanceled) {

        m_joinSpecification = joinSpecification;
        m_hashSide = hashSide;
        m_joinContainer = joinContainer;
        m_checkCanceled = checkCanceled;

        // whether to compare data cells based on value and type, on their string representations, etc.
        HashingStrategy<DataCell[]> comparisonMode;
        switch(joinSpecification.getDataCellComparisonMode()) {
            case STRICT:
                comparisonMode = new HashStrict();
                break;
            case AS_STRING:
                comparisonMode = new HashAsString();
                break;
            case NUMERIC_AS_LONG:
                comparisonMode = new HashNumericAsLong();
                break;
            default:
                throw new IllegalStateException("No implementation for the data cell comparison mode "
                    + joinSpecification.getDataCellComparisonMode());
        }
        m_index = new TCustomHashMap<>(comparisonMode);

        // probe/hash row settings
        InputTable probeSide = hashSide.other();
        m_trackMatchedHashRows = m_joinSpecification.getSettings(hashSide).isRetainUnmatched();
        m_probeSettings = m_joinSpecification.getSettings(probeSide);

        // row offsets and unmatched rows
        m_hashrowInternalOffsets = new TObjectIntCustomHashMap<>(new HashingStrategy<DataRow>() {
            @Override
            public int computeHashCode(final DataRow object) {
                return object.getKey().hashCode();
            }

            @Override
            public boolean equals(final DataRow o1, final DataRow o2) {
                return o1 == o2;
            }
        });
        m_matched = m_trackMatchedHashRows ? new BitSet() : null;

    }

    /**
     *
     * @param joinTuple data cells holding the values of the columns appearing in the join clauses. Can be null to
     *            indicate that one of the join columns contains a missing value. The row will then be treated as
     *            unmatched rows (missing value equals nothing).
     * @param row
     * @param offset
     */
    public void addHashRow(final DataCell[] joinTuple, final DataRow row, final long offset) {

        if (joinTuple == null) {
            // do not add to index structure. can't be matched by anything
            m_joinContainer.unmatched(m_hashSide).accept(row, offset);
        } else {
            List<DataRow> rowList = m_index.computeIfAbsent(joinTuple, newRowList);
            rowList.add(row);
            // add to index structure
            m_hashrowInternalOffsets.put(row, m_rows.size());
            m_rows.add(row);
            m_rowOffsets.add(offset);
        }

    }

    /**
     * Retrieves the rows from this index that have the same values in the join columns.
     * Offers each pair of probeRow and a matching row to the join container.
     * @param probeRow the row that provides the join column values for which we search join partners
     * @param probeRowOffset the offset of the probe row in its source table (for sorting)
     * @throws CanceledExecutionException if the user cancels the join, this exception is propagated
     */
    public void joinSingleRow(final DataRow probeRow, final long probeRowOffset) throws CanceledExecutionException {

        DataCell[] key = m_probeSettings.get(probeRow);

        // null if no matches exist. Otherwise, a list of matching rows in the order they were inserted
        // using #addHashRow(JoinTuple, DataRow, long)
        List<DataRow> matching = m_index.get(key);

        // no indexed row has the same values in the join columns as the probe row
        if (matching == null) {
            // the probe row is potentially unmatched (depends on whether the index is comprehensive)
            m_joinContainer.unmatched(m_probeSettings.getSide()).accept(probeRow, probeRowOffset);
        } else {
            // these rows have the same values in the join columns as the probe row
            for (DataRow hashRow : matching) {
                // could be quite a few rows that match
                m_checkCanceled.checkCanceled();
                processMatch(probeRow, probeRowOffset, hashRow);
            }
        }
    }

    /**
     * @param probeRow a query row defining the values in the join columns to look up in this index
     * @param probeRowOffset the position of the row in its containing table
     * @param hashRow a row retrieved frmo this index with matching values in the join columns
     */
    private void processMatch(final DataRow probeRow, final long probeRowOffset, final DataRow hashRow) {
        // mark hash row as matched if keeping track
        int internalOffset = m_hashrowInternalOffsets.get(hashRow);
        if (m_trackMatchedHashRows) {
            m_matched.set(internalOffset);
        }

        DataRow left = m_probeSettings.getSide().isLeft() ? probeRow : hashRow;
        DataRow right = m_probeSettings.getSide().isLeft() ? hashRow : probeRow;

        // retrieve the offset of the hash row in the hash input table
        long hashRowOrder = m_rowOffsets.get(internalOffset);

        long leftOrder = m_probeSettings.getSide().isLeft() ? probeRowOffset : hashRowOrder;
        long rightOrder = m_probeSettings.getSide().isLeft() ? hashRowOrder : probeRowOffset;

        m_joinContainer.offerMatch(left, leftOrder, right, rightOrder);
    }

    /**
     * Process the hash rows that have not been matched to a probe row during {@link #joinSingleRow(DataRow, long)}
     *
     * @param handler processes the hash row and its row offset
     * @throws CanceledExecutionException
     *
     */
    @SuppressWarnings("javadoc")
    public void forUnmatchedHashRows(final RowHandlerCancelable handler) throws CanceledExecutionException {

        if (!m_trackMatchedHashRows) {
            return;
        }

        int nextUnmatchedOffset = m_matched.nextClearBit(0);

        while (nextUnmatchedOffset < m_rows.size()) {
            m_checkCanceled.checkCanceled();
            handler.accept(m_rows.get(nextUnmatchedOffset), m_rowOffsets.get(nextUnmatchedOffset));
            nextUnmatchedOffset = m_matched.nextClearBit(nextUnmatchedOffset + 1);
        }

    }

    public int numAddedRows() {
        return m_rows.size();
    }

    /**
     * Hash strategy that tests whether two rows match by comparing the content AND data types of the values in the join
     * columns, e.g., a value in an integer column will never match a value in a long column.
     */
    @SuppressWarnings("serial")
    private static class HashStrict implements HashingStrategy<DataCell[]> {
        @Override
        public int computeHashCode(final DataCell[] joinClauseSides) {
            return Arrays.hashCode(joinClauseSides);
        }

        @Override
        public boolean equals(final DataCell[] o1, final DataCell[] o2) {
            for (int i = 0; i < o1.length; i++) {
                if (o1[i].isMissing() || o2[i].isMissing()) {
                    return false;
                }
                // compare the data cells considering their value and type
                if (!o1[i].equals(o2[i])) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Hashing strategy that tests whether two rows match by comparing the string representations of the values in the
     * join columns.
     */
    @SuppressWarnings("serial")
    private static class HashAsString implements HashingStrategy<DataCell[]> {
        @Override
        public int computeHashCode(final DataCell[] joinClauseSides) {
            if (joinClauseSides == null) {
                return 0;
            }

            int result = 1;
            for (Object element : joinClauseSides) {
                result = 31 * result + (element == null ? 0 : element.toString().hashCode());
            }
            return result;
        }

        @Override
        public boolean equals(final DataCell[] o1, final DataCell[] o2) {
            for (int i = 0; i < o1.length; i++) {
                if (o1[i].isMissing() || o2[i].isMissing()) {
                    return false;
                }
                // compare the data cells based on their string representations
                if (!o1[i].toString().equals(o2[i].toString())) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Hashing strategy that tests whether two rows match by treating values of integer join columns as long values.
     * Without this special strategy, their hash codes will differ and thus not match.
     */
    @SuppressWarnings("serial")
    private static class HashNumericAsLong implements HashingStrategy<DataCell[]> {

        @Override
        public int computeHashCode(final DataCell[] joinClauseSides) {
            if (joinClauseSides == null) {
                return 0;
            }

            int result = 1;
            for (DataCell element : joinClauseSides) {
                if (element instanceof LongValue) {
                    long value = ((LongValue)element).getLongValue();
                    result = 31 * result + (int)(value ^ (value >>> 32));
                } else {
                    result = 31 * result + (element == null ? 0 : element.hashCode());
                }
            }
            return result;
        }

        @Override
        public boolean equals(final DataCell[] o1, final DataCell[] o2) {
            for (int i = 0; i < o1.length; i++) {
                if (o1[i].isMissing() || o2[i].isMissing()) {
                    return false;
                }
                // compare the data cells using their long value if possible
                if (o1[i] instanceof LongValue && o2[i] instanceof LongValue) {
                    if (((LongValue)o1[i]).getLongValue() != ((LongValue)o2[i]).getLongValue()) {
                        return false;
                    }
                } else {
                    if (!o1[i].equals(o2[i])) {
                        return false;
                    }
                }
            }
            return true;
        }
    }




    /**
     * @return the input table with the (expected) larger memory footprint as measured by the number of materialized
     *         cells. In case the join specification holds only data table specs and no data tables yet, returns the
     *         left table (which may also speed up sorting according to {@link OutputRowOrder#LEFT_RIGHT})
     */
    static InputTable biggerTable(final JoinSpecification js) {
        Long leftMaterializedCells = js.getSettings(InputTable.LEFT).getMaterializedCells().orElse(0L);
        Long rightMaterializedCells = js.getSettings(InputTable.RIGHT).getMaterializedCells().orElse(0L);
        return leftMaterializedCells >= rightMaterializedCells ? InputTable.LEFT : InputTable.RIGHT;
    }

    /**
     * @return the opposite of what {@link #biggerTable(JoinSpecification)} returns
     */
    static InputTable smallerTable(final JoinSpecification joinSpecification) {
        return biggerTable(joinSpecification).other();
    }

    /** @return the number of rows added to this index */
    long size() {
        return m_rows.size();
    }

}