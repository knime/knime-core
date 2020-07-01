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

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.ObjLongConsumer;
import java.util.stream.Stream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.join.HybridHashJoin;
import org.knime.core.data.join.JoinImplementation;
import org.knime.core.data.join.JoinSpecification;
import org.knime.core.data.join.JoinSpecification.InputTable;
import org.knime.core.data.join.OrderedRow;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException.CancelChecker;
import org.knime.core.node.ExecutionContext;

import gnu.trove.set.hash.TLongHashSet;

/**
 * Base class for implementations of the {@link JoinResults} interface.
 * Provides result deduplication capabilities for disjunctive joins.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 4.2
 */
public abstract class JoinContainer implements JoinResults {

    /**
     * Can be used to address arrays of data structures for different result types.
     */
    protected static final int LEFT_OUTER = 0, RIGHT_OUTER = 1, MATCHES = 2;

    /**
     * Handlers for unmatched rows. The handler for unmatched rows from the left input table is at offset
     * {@link Enum#ordinal()} for {@link InputTable#LEFT}.
     */
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
     * Each {@link RowOffsetCombinationSet} checks whether a result with a given combination of row offsets was added
     * before. The array contains one set for each result type, e.g., for {@link #MATCHES}, {@link #LEFT_OUTER}, and
     * {@link #RIGHT_OUTER}.
     */
    protected final RowOffsetCombinationSet[] m_caches;

    /**
     * {@link JoinContainer#JoinContainer(JoinImplementation, boolean, boolean)}
     */
    protected final boolean m_deduplicateResults;

    /**
     * A mapping for each input table that associates the {@link RowKey} of an input row to the {@link RowKey}s of the
     * output rows that involve this row. The output row can be either a match (when it combines the input row with
     * another row) or an unmatched row. If hiliting is disabled, this field is null.
     */
    protected final EnumMap<InputTable, EnumMap<ResultType, Map<RowKey, Set<RowKey>>>> m_hiliteMapping;

    private CancelChecker m_checkCanceled;

    /**
     * @param joinImplementation contains the {@link JoinSpecification} according to which the tables are combined and
     *            additional configuration such as {@link JoinImplementation#isEnableHiliting()}.
     * @param deduplicateResults If true, invocations of {@link #addMatch(DataRow, long, DataRow, long)},
     *            {@link #addLeftOuter(DataRow, long)}, and {@link #addRightOuter(DataRow, long)} will reject the given
     *            row if the method was previously invoked with the same row offsets.
     * @param deferUnmatchedRows If false, output unmatched rows directly. If true, allow calls to
     *            {@link #addMatch(DataRow, long, DataRow, long)} to cancel the unmatched status of a row until
     *            collecting the unmatched rows via {@link DeferredProbeRowHandler#collectUnmatched()}.
     */
    public JoinContainer(final JoinImplementation joinImplementation, final boolean deduplicateResults, final boolean deferUnmatchedRows) {
        m_joinSpecification = joinImplementation.getJoinSpecification();
        m_exec = joinImplementation.getExecutionContext();
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

        if(joinImplementation.isEnableHiliting()) {
            m_hiliteMapping = new EnumMap<>(InputTable.class);

            m_hiliteMapping.put(InputTable.LEFT, new EnumMap<>(ResultType.class));
            m_hiliteMapping.get(InputTable.LEFT).put(ResultType.MATCHES, new HashMap<>());
            m_hiliteMapping.get(InputTable.LEFT).put(ResultType.LEFT_OUTER, new HashMap<>());

            m_hiliteMapping.put(InputTable.RIGHT, new EnumMap<>(ResultType.class));
            m_hiliteMapping.get(InputTable.RIGHT).put(ResultType.MATCHES, new HashMap<>());
            m_hiliteMapping.get(InputTable.RIGHT).put(ResultType.RIGHT_OUTER, new HashMap<>());
        } else {
            m_hiliteMapping = null;
        }

    }

    /**
     * If a {@link HybridHashJoin} can't be performed in memory, it will flush partitions of the input table to disk.
     * If these partitions are still too big to join in memory, the {@link BlockHashJoin} will compute the join in
     * several passes over the probe larger input partition. In this case, the join container will have to defer the
     * handling of unmatched rows from the table (left/right) that is used as probe input until the last pass over the
     * probe input has been completed.
     * @param side for which input table to set
     * @param defer whether to enable deferred collection of unmatched rows
     */
    public void setDeferUnmatchedRows(final InputTable side, final boolean defer) {

        int resultType = side.isLeft() ? LEFT_OUTER : RIGHT_OUTER;
        boolean previouslyDeferred = m_unmatchedRows[resultType] instanceof DeferredProbeRowHandler;
        if (defer && previouslyDeferred) {
            return;
        }
        if (!defer && previouslyDeferred) {
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
        ObjLongConsumer<DataRow> handler = side.isLeft() ? this::doAddLeftOuter : this::doAddRightOuter;
        if (defer) {
            BufferedDataTable table =
                    m_joinSpecification.getSettings(side).getTable().orElseThrow(IllegalStateException::new);
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

        // let the deferred probe row handlers know that both rows are matched (irrespective of keeping the match)
        m_unmatchedRows[LEFT_OUTER].matched(left, leftOrder);
        m_unmatchedRows[RIGHT_OUTER].matched(right, rightOrder);

        if (m_joinSpecification.isRetainMatched() && isFreshValue) {
            return doAddMatch(left, leftOrder, right, rightOrder);
        }
        return false;
    }

    @Override
    public boolean addLeftOuter(final DataRow row, final long offset) {
        boolean isFreshValue = !m_deduplicateResults || m_caches[LEFT_OUTER].put(offset, 0);

        if (m_joinSpecification.isRetainUnmatched(InputTable.LEFT) && isFreshValue) {
            // if the unmatched rows handler is not deferred, this will call #doAddLeftOuter directly, otherwise
            // it will collect the row as a candidate for the left unmatched rows output. The row looses its
            // unmatched status as soon as #addMatch is called with the unmatched candidate row.
            m_unmatchedRows[LEFT_OUTER].unmatched(row, offset);
            return true;
        }
        return false;
    }

    @Override
    public boolean addRightOuter(final DataRow row, final long offset) {
        boolean isFreshValue = !m_deduplicateResults || m_caches[RIGHT_OUTER].put(offset, 0);
        if (m_joinSpecification.isRetainUnmatched(InputTable.RIGHT) && isFreshValue) {
            // if the unmatched rows handler is not deferred, this will call #doAddRightOuter directly, otherwise
            // it will collect the row as a candidate for the right unmatched rows output. The row looses its
            // unmatched status as soon as #addMatch is called with the unmatched candidate row.
            m_unmatchedRows[RIGHT_OUTER].unmatched(row, offset);
            return true;
        }
        return false;
    }

    /**
     * Convert a row from the left input table to the single match table format. <br/>
     * Depending on whether join columns in the right table are merged, fewer missing values are appended.
     *
     * @param leftUnmatched an unmatched row from the left table in the original input format
     * @return a data row that contains missing values for all included columns of the right table.
     */
    protected DataRow leftToSingleTableFormat(final DataRow leftUnmatched) {

        int[] leftCells = m_joinSpecification.getMatchTableIncludeIndices(InputTable.LEFT);
        // this skips merged join columns if merge join columns is on
        int[] rightCells = m_joinSpecification.getMatchTableIncludeIndices(InputTable.RIGHT);

        final DataCell[] dataCells = new DataCell[leftCells.length + rightCells.length];
        int cell = 0;

        for (int i = 0; i < leftCells.length; i++) {
            dataCells[cell++] = leftUnmatched.getCell(leftCells[i]);
        }
        for (int i = 0; i < rightCells.length; i++) {
            dataCells[cell++] = DataType.getMissingCell();
        }

        RowKey newKey = m_joinSpecification.getRowKeyFactory().apply(leftUnmatched, null);
        return new DefaultRow(newKey, dataCells);
    }

    /**
     * Convert a row from the right input table to single match table format. <br/>
     * Depending on whether join columns are to be merged, the merged columns are removed from the row (also, the
     * columns not included are removed). If merge join columns is on, the present values from the right outer row are
     * written to the column of the left table that consumed the join column (can be multiple; if they are all equal,
     * the value will be used, otherwise a missing value is emitted).
     *
     * @param rightUnmatched an unmatched row from the original right input table
     * @return a data row that contains missing values for all included columns of the right table.
     */
    protected DataRow rightToSingleTableFormat(final DataRow rightUnmatched) {

        // getMatchTableIncludeIndices is aware of whether merge join columns is selected
        int[] leftCells = m_joinSpecification.getMatchTableIncludeIndices(InputTable.LEFT);
        int[] rightCells = m_joinSpecification.getMatchTableIncludeIndices(InputTable.RIGHT);

        final DataCell[] dataCells = new DataCell[leftCells.length + rightCells.length];
        int cell = 0;

        if(m_joinSpecification.isMergeJoinColumns()) {

            int[][] lookupColumns = m_joinSpecification.getColumnLeftMergedLocations();

            // put values from merged columns into left table
            for (int i = 0; i < leftCells.length; i++) {

                // in case this is not a join column (no lookup columns) just use a missing value
                DataCell consensus = lookupColumns[i].length == 0 ? DataType.getMissingCell() : null;

                for (int j = 0; j < lookupColumns[i].length; j++) {
                    if(lookupColumns[i][j] == -1) {
                        // if any of the lookup values is missing, deliver a missing value
                        consensus = DataType.getMissingCell();
                        break;
                    } else {
                        DataCell value = rightUnmatched.getCell(lookupColumns[i][j]);
                        // if at least one value does not equal the others, return a missing value
                        if(consensus != null && !value.equals(consensus)) {
                            consensus = DataType.getMissingCell();
                            break;
                        }
                        consensus = value;
                    }
                }
                dataCells[cell++] = consensus;
            }
            // skip the merged join columns
            for (int i = 0; i < rightCells.length; i++) {
                dataCells[cell++] = rightUnmatched.getCell(rightCells[i]);
            }

        } else {
            // just fill all left table columns with missing values
            for (int i = 0; i < leftCells.length; i++) {
                dataCells[cell++] = DataType.getMissingCell();
            }

            // and append everything that has survived projection to right outer format
            for (int i = 0; i < rightCells.length; i++) {
                dataCells[cell++] = rightUnmatched.getCell(rightCells[i]);
            }
        }
        RowKey newKey = m_joinSpecification.getRowKeyFactory().apply(null, rightUnmatched);
        return new DefaultRow(newKey, dataCells);

    }

    /**
     * Associate an input row to a row in the output.
     *
     * @param side whether the input row comes from the left or the right input table
     * @param resultType whether the output row describes a match or an unmatched row
     * @param inputRowKey the ID of the input row
     * @param outputRowKey the ID of the output row
     */
    protected void addHiliteMapping(final InputTable side, final ResultType resultType, final RowKey inputRowKey,
        final RowKey outputRowKey) {
        // if hiliting is not enabled, do nothing
        // otherwise associate the output row's key to the input row's key
        if (m_hiliteMapping != null) {
            Set<RowKey> associatedRowKeys =
                m_hiliteMapping.get(side).get(resultType).computeIfAbsent(outputRowKey, key -> new HashSet<>());
            associatedRowKeys.add(inputRowKey);
        }
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
     * Returns a mapping from input row keys to output row keys.
     *
     * @param side whether to return the mapping for the rows of the left or right input table
     * @param resultType the mapping
     * @return an empty optional if hiliting is disabled. Otherwise a mapping that associates input rows from the given
     *         side to the rows in the output of the given result type
     */
    public Optional<Map<RowKey, Set<RowKey>>> getHiliteMapping(final InputTable side, final ResultType resultType) {

        if (m_hiliteMapping == null) {
            return Optional.empty();
        }

        // combine maps for matches and unmatched rows into a single map
        if (resultType == ResultType.ALL) {
            final Map<RowKey, Set<RowKey>> combinedMap = new HashMap<>();
            // copy all entries from the matches map
            combinedMap.putAll(m_hiliteMapping.get(side).get(ResultType.MATCHES));
            // add all entries of the left/right unmatched output rows
            for (Map<RowKey, Set<RowKey>> unmatchedMap : m_hiliteMapping.get(side).values()) {
                // add each mapping to the combined map
                unmatchedMap.forEach((inputRowKey, outputRowKeys) -> {
                    // the output rows already associated to the given input row
                    Set<RowKey> existingMappings = combinedMap.computeIfAbsent(inputRowKey, key -> new HashSet<>());
                    // extend by the mappings from this result type
                    existingMappings.addAll(outputRowKeys);
                });
            }
            return Optional.of(combinedMap);
        } else {
            return Optional.of(m_hiliteMapping.get(side).get(resultType));
        }
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
