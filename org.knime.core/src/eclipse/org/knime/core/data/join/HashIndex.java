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
package org.knime.core.data.join;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Function;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.join.JoinSpecification.InputTable;
import org.knime.core.data.join.JoinSpecification.OutputRowOrder;
import org.knime.core.data.join.results.JoinResult;
import org.knime.core.data.join.results.JoinResult.RowHandlerCancelable;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.CanceledExecutionException.CancelChecker;

import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.custom_hash.TObjectIntCustomHashMap;
import gnu.trove.map.hash.TCustomHashMap;
import gnu.trove.strategy.HashingStrategy;

/**
 * Index for rows in a table. Provides fast lookup of join partners via
<<<<<<< HEAD
 * {@link #joinSingleRow(JoinTuple, DataRow, long, JoinResult)}. Can be flushed to disk using {@link #toDisk()}. This
 * does not serialize the index structure, it just flushes to disk the rows stored in the index using a
 * {@link BufferedDataTable}.
=======
 * {@link #joinSingleRow(JoinTuple, DataRow, long, JoinResults)}. Can be flushed to disk using {@link #toDisk()}.
>>>>>>> 65ee5f4050365315c38ca0aab8cc21d67f282d75
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
class HashIndex {

    /**
     * Creates a new list of rows in case there's none already associated to a certain combination of join column values
     * in {@link #addHashRow(JoinTuple, DataRow, long)}. Must produce a new instance of the type the {@link #m_indexes}
     * map to (=hold as values).
     */
    private static final Function<DataCell[], List<DataRow>> newRowList = k -> new ArrayList<DataRow>();

    /** Puts the join results here. */
    final JoinResult m_joinContainer;

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
     * Makes hash input rows accessible via join column value combinations. For each disjunctive clause, a separate
     * index is needed. The number of indexes is 1 if the join is conjunctive {@link JoinSpecification#isConjunctive()},
     * or disjunctive with a single conjunctive clause (i.e., user selects match any with a single column pair A=X).
     */
    private final List<TCustomHashMap<DataCell[], List<DataRow>>> m_indexes;

    /**
     * Whether to remember which hash rows have had join partners in the probe table to be able to output unmatched hash
     * rows.
     */
    private final boolean m_trackMatchedHashRows;

    /**
     * The i-th bit stores whether the i-th entry of m_rows has had a join partner in the probe table so far, as found
     * during a previous call to {@link #joinSingleRow(JoinTuple, DataRow, JoinResult)}
     */
    private final BitSet m_matched;

    /**
     * Maps a hash row to its offset in {@link #m_rows}. This internal offset is in turn used to mark the rows that have
     * been matched to a probe row, by setting the corresponding bit in {@link #m_matched}.
     * This avoids wrapping DataRows in another object that holds the data row's offset in its source table.
     * Alternatively, one could also introduce another map from DataRow to long but these internal offsets are also
     * convenient for addressing in {@link #m_matched} as they are more compact.
     */
    private final TObjectIntCustomHashMap<DataRow> m_hashrowInternalOffsets;

    private final JoinTableSettings m_probeSettings;

    private final JoinSpecification m_joinSpecification;

    /**
     * Evaluated to check whether the execution was canceled. The supplier might ignore every n-th evaluation for
     * performance reasons.
     */
    private final CancelChecker m_checkCanceled;

    private final Comparator<DataRow> m_compareByRowOffset;

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
    HashIndex(final JoinSpecification joinSpecification, final JoinResult joinContainer,
        final JoinSpecification.InputTable hashSide, final CancelChecker checkCanceled) {

        m_joinSpecification = joinSpecification;
        m_hashSide = hashSide;
        m_joinContainer = joinContainer;
        m_checkCanceled = checkCanceled;

        // probe/hash row settings
        InputTable probeSide = hashSide.other();
        m_trackMatchedHashRows = m_joinSpecification.getSettings(hashSide).m_retainUnmatched;
        m_probeSettings = m_joinSpecification.getSettings(probeSide);

        // row offsets and unmatched rows
        m_hashrowInternalOffsets = new TObjectIntCustomHashMap<>(new HashingStrategy<DataRow>() {
            @Override public int computeHashCode(final DataRow object) { return object.getKey().hashCode(); }
            @Override public boolean equals(final DataRow o1, final DataRow o2) { return o1 == o2; }
        });
        m_matched = m_trackMatchedHashRows ? new BitSet() : null;

        // index building
        int numIndexes = m_joinSpecification.numConjunctiveGroups();
        m_indexes = new ArrayList<>(numIndexes);
        if(m_joinSpecification.isConjunctive()) {
            m_indexes.add(new TCustomHashMap<>(JoinTuple.hashConjunctive()));
        } else {
            // add one lookup index for every conjunctive clause
            for (int i = 0; i < numIndexes; i++) {
                m_indexes.add(new TCustomHashMap<>(JoinTuple.hashDisjunctiveClause(i)));
            }
        }

        // disjunctive join predicates require duplicate elimination across the m_indexes.
        // this is used for merging the results from every index by eliminating duplicates and sorting the result rows
        // according to the order they were inserted (by calling addHashRow on this object)
        m_compareByRowOffset = (hashRow1, hashRow2) -> {
            long hashRow1Offset = m_rowOffsets.get(m_hashrowInternalOffsets.get(hashRow1));
            long hashRow2Offset = m_rowOffsets.get(m_hashrowInternalOffsets.get(hashRow2));
            return Long.compare(hashRow1Offset, hashRow2Offset);
        };

    }

//    /**
//     * Provides a default parameter for the unmatchedProbeRows parameter. Typically, the unmatched rows from the probe
//     * input are handled by {@link JoinResults#addLeftOuter(DataRow, long)} or
//     * {@link JoinResults#addRightOuter(DataRow, long)}. However, in a nested loop join, several passes over the probe
//     * input are made and unmatched rows must be output only once, which requires a special handler that can be passed
//     * to the full constructor.
//     * @param joinSpecification
//     * @param results
//     * @param hashSide
//     * @param checkCanceled
//     * @throws InvalidSettingsException
//     *
//     * @see #HashIndex(JoinSpecification, JoinResults, TObjectLongProcedure, InputTable, BooleanSupplier)
//     */
//    public HashIndex(final JoinSpecification joinSpecification, final JoinResults results, final InputTable hashSide,
//        final CancelChecker checkCanceled) {
//        this(joinSpecification, results, results.unmatched(hashSide.other()), hashSide, checkCanceled);
//    }

    /**
     *
     * @param joinTuple data cells holding the values of the columns appearing in the join clauses. can be null to
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
            if (m_joinSpecification.isConjunctive()) {
                List<DataRow> rowList = m_indexes.get(0).computeIfAbsent(joinTuple, newRowList);
                rowList.add(row);
            } else {
                // add the row to every clause index
                for (int clause = 0; clause < m_indexes.size(); clause++) {
                    // contains the rows that have a specific combination of values in the join columns of the i-th clause
                    List<DataRow> clauseRowList = m_indexes.get(clause).computeIfAbsent(joinTuple, newRowList);
                    // the values corresponding to the clause are extracted by the clause index's hashing strategy
                    // collisions are avoided via that hashing strategy's equals implementation
                    clauseRowList.add(row);
                }
            }
            // add to index structure
            m_hashrowInternalOffsets.put(row, m_rows.size());
            m_rows.add(row);
            m_rowOffsets.add(offset);
        }

    }

    /**
     * @param probeRow the row that provides the join column values for which we search join partners
     * @param probeRowOffset the offset of the probe row in its source table (for sorting)
     * @return true iff the execution was canceled
     */
    public void joinSingleRow(final DataRow probeRow, final long probeRowOffset) throws CanceledExecutionException {

        List<DataRow> matching =
            m_joinSpecification.isConjunctive() ? matchConjunctive(probeRow) : matchDisjunctive(probeRow);

        // no indexed row has the same values in the join columns as the probe row
        if (matching == null) {
            // the probe row is potentially unmatched (depends on whether the index is comprehensive)
            m_joinContainer.unmatched(m_probeSettings.getSide()).accept(probeRow, probeRowOffset);
        } else {
            // these rows have the same values in the join columns as the probe row
            for (DataRow hashRow : matching) {

                // could be quite a few rows that match
                m_checkCanceled.checkCanceled();

                // mark hash row as matched if keeping track
                int internalOffset = m_hashrowInternalOffsets.get(hashRow);
                if (m_trackMatchedHashRows) {
                    m_matched.set(internalOffset);
                }

                // retrieve the offset of the hash row in the hash input table
                long hashRowOrder = m_rowOffsets.get(internalOffset);

                // even if we don't retain matches, we can't skip this since the join container may needs to cancel
                // the unmatched status of a probe row
                DataRow left = m_probeSettings.getSide().isLeft() ? probeRow : hashRow;
                DataRow right = m_probeSettings.getSide().isLeft() ? hashRow : probeRow;

                long leftOrder = m_probeSettings.getSide().isLeft() ? probeRowOffset : hashRowOrder;
                long rightOrder = m_probeSettings.getSide().isLeft() ? hashRowOrder : probeRowOffset;

                m_joinContainer.addMatch(left, leftOrder, right, rightOrder);
            }
        }
    }

    /**
     * Find all matching rows among the previously added rows.
     *
     * @param probeRow The row to extract the join predicate values from.
     * @return null if none found. Otherwise, a list of matching rows in order of their insertion with
     *         {@link #addHashRow(JoinTuple, DataRow, long)}.
     */
    private List<DataRow> matchConjunctive(final DataRow probeRow) {
        DataCell[] key = JoinTuple.get(m_probeSettings, probeRow);
        return m_indexes.get(0).get(key);
    }

    /**
     * Find all matching rows among the previously added rows.
     * A hash row matches if it matches on any of the join column groups.
     * <h1>Internals</h1>
     *
     * @param probeRow The row to extract the join predicate values from.
     * @return null if none found. Otherwise, a list of matching rows in order of their insertion with
     *         {@link #addHashRow(JoinTuple, DataRow, long)}.
     */
    private List<DataRow> matchDisjunctive(final DataRow probeRow) {

        TreeSet<DataRow> matches = new TreeSet<>(m_compareByRowOffset);

        DataCell[] asArray = JoinTuple.get(m_probeSettings, probeRow);
        for (int i = 0; i < m_indexes.size(); i++) {
            TCustomHashMap<DataCell[], List<DataRow>> tCustomHashMap = m_indexes.get(i);
            List<DataRow> joinPartners = tCustomHashMap.get(asArray);
            // join partners is null if the index has no entry for the join tuple
            if (joinPartners != null) {
                matches.addAll(joinPartners);
            }
        }

        return matches.size() == 0 ? null : new ArrayList<>(matches);
    }

    /**
     * Process the hash rows that have not been matched to a probe row during {@link #joinSingleRow(DataRow, long)}
     * @param handler processes the hash row and its row offset
     * @throws CanceledExecutionException
     *
     */
    public void forUnmatchedHashRows(final RowHandlerCancelable handler)
        throws CanceledExecutionException {

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

//    /**
//     * Only hash buckets are migrated to disk, probe rows are put into DiskBuckets directly. This does not serialize the
//     * index structure, it just flushes to disk the rows stored in the index using a {@link BufferedDataTable}.
//     */
//    DiskBucket toDisk(final DiskBackedHashPartitions partitioner) {
//
//        // release memory
//        m_indexes.clear();
//
//        DiskBucket result = partitioner.new DiskBucket(m_hashSettings);
//
//        for (int i = m_rows.size(); i-- > 0;) {
//            result.add(m_rows.get(i), m_rowOffsets.get(i));
//            // release memory
//            m_rows.set(i, null);
//        }
//
//        return result;
//    }

    public int numAddedRows() {
        return m_rows.size();
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

}