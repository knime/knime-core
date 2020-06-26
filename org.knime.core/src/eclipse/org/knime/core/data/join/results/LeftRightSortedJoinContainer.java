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
 *   Jun 3, 2020 (carlwitt): created
 */
package org.knime.core.data.join.results;

import java.util.Arrays;
import java.util.Comparator;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.join.JoinSpecification;
import org.knime.core.data.join.JoinSpecification.InputTable;
import org.knime.core.data.join.JoinSpecification.OutputRowOrder;
import org.knime.core.data.join.OrderedRow;
import org.knime.core.data.join.results.JoinResults.OutputCombined;
import org.knime.core.data.join.results.JoinResults.OutputMode;
import org.knime.core.data.join.results.JoinResults.OutputSplit;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 * A container for join results that need to be sorted according to {@link OutputRowOrder#LEFT_RIGHT}.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 4.2
 */
public abstract class LeftRightSortedJoinContainer {

    /**
     * @param outputMode
     * @param joinSpecification
     * @param exec
     * @param deduplicateResults
     * @param deferUnmatchedRows
     * @return
     */
    public static JoinContainer create(final OutputMode outputMode, final JoinSpecification joinSpecification, final ExecutionContext exec,
        final boolean deduplicateResults, final boolean deferUnmatchedRows) {
        switch(outputMode) {
            case OutputCombined:
                return new Combined(joinSpecification, exec, deduplicateResults, deferUnmatchedRows);
            case OutputSplit:
                return new Split(joinSpecification, exec, deduplicateResults, deferUnmatchedRows);
        }
        throw new IllegalStateException("Output mode not implemented: " + outputMode);
    }

    private static final Comparator<DataRow> COMPARE_COMBINED_OFFSETS = (c1, c2) -> Long
        .compareUnsigned(OrderedRow.OFFSET_EXTRACTOR.applyAsLong(c1), OrderedRow.OFFSET_EXTRACTOR.applyAsLong(c2));

    private LeftRightSortedJoinContainer() {
        throw new IllegalStateException(
            "Instantiate LeftRightSortedJoinContainer.Split or LeftRightSortedJoinContainer.Combined instead.");
    }

    /**
     *
     *
     * @author Carl Witt, KNIME AG, Zurich, Switzerland
     */
    public static class Split extends JoinContainer implements OutputSplit {

        private final BufferedDataContainer[] m_containers;

        private final BufferedDataTable[] m_tables;

        private final DataTableSpec[] m_workingSpecs;

        /**
         * @param joinSpecification
         * @param exec
         * @param deduplicateResults
         * @param deferUnmatchedRows
         */
        protected Split(final JoinSpecification joinSpecification, final ExecutionContext exec,
            final boolean deduplicateResults, final boolean deferUnmatchedRows) {
            super(joinSpecification, exec, deduplicateResults, deferUnmatchedRows);
            // add a long column to each output spec for storing the combined left and right row offset
            m_workingSpecs = Arrays.stream(m_outputSpecs).map(OrderedRow::withOffset).toArray(DataTableSpec[]::new);

            // create output containers
            m_containers =
                Arrays.stream(m_workingSpecs).map(exec::createDataContainer).toArray(BufferedDataContainer[]::new);

            m_tables = new BufferedDataTable[3];
        }

        @Override
        protected boolean doAddMatch(final DataRow left, final long leftOffset, final DataRow right,
            final long rightOffset) {
            DataRow joinedProjected = m_joinSpecification.rowJoin(left, right);
            add(MATCHES, OrderedRow.withOffset(joinedProjected, OrderedRow.combinedOffsets(leftOffset, rightOffset)));
            return true;
        }

        @Override
        protected boolean doAddLeftOuter(final DataRow row, final long offset) {
            // use rowProjectOuter to avoid merging join columns
            DataRow projected = m_joinSpecification.rowProjectOuter(InputTable.LEFT, row);
            // no combined offset necessary, since they live in their own table and are sorted separately
            // (their offsets have their own "scope")
            add(LEFT_OUTER, OrderedRow.withOffset(projected, offset));
            return true;
        }

        @Override
        protected boolean doAddRightOuter(final DataRow row, final long offset) {
            // use rowProjectOuter to avoid merging join columns
            DataRow projected = m_joinSpecification.rowProjectOuter(InputTable.RIGHT, row);
            // no combined offset necessary, since they live in their own table and are sorted separately
            // (their offsets have their own "scope")
            add(RIGHT_OUTER, OrderedRow.withOffset(projected, offset));
            return true;
        }

        private void add(final int rowType, final DataRow row) {
            m_containers[rowType].addRowToTable(row);
        }

        @Override
        public BufferedDataTable getMatches() throws CanceledExecutionException {
            return get(MATCHES);
        }

        @Override
        public BufferedDataTable getLeftOuter() throws CanceledExecutionException {
            return get(LEFT_OUTER);
        }

        @Override
        public BufferedDataTable getRightOuter() throws CanceledExecutionException {
            return get(RIGHT_OUTER);
        }

        /**
         * Closes the result container and sorts the rows according to the left and then right row offset. The row
         * offsets are stored in the first two columns, see {@link #withOffset(DataTableSpec)}. The actual row offset
         * values are added to a result row by wrapping it in a {@link LeftRightOrderedRow} which prepends the specified
         * row offsets as two additional long cells. These are removed after sorting to comply with
         * {@link JoinSpecification#specForMatchTable()}, {@link JoinSpecification#leftUnmatchedTableSpec()}, or
         * {@link JoinSpecification#rightUnmatchedTableSpec()}, depending on the join result row type.
         *
         * @param resultType match rows, left unmatched rows, or right unmatched rows
         * @return sorted table
         * @throws CanceledExecutionException
         * @see LeftRightOrderedRow#LeftRightOrderedRow(DataRow, long, long)
         */
        @SuppressWarnings("javadoc")
        protected BufferedDataTable get(final int resultType) throws CanceledExecutionException {
            if (m_tables[resultType] == null) {
                // in case unmatched rows are collected deferred, this is the last possibility to collect them
                if (resultType != MATCHES) {
                    m_unmatchedRows[resultType].collectUnmatched();
                }

                m_containers[resultType].close();

                BufferedDataTable unsorted = m_containers[resultType].getTable();

                // sort by the combined offsets
                BufferedDataTable sorted = new BufferedDataTableSorter(unsorted, COMPARE_COMBINED_OFFSETS).sort(m_exec);

                // remove sort columns
                final ColumnRearranger workingSpecToFinalSpec = OrderedRow.removeOffset(m_workingSpecs[resultType]);
                m_tables[resultType] = m_exec.createColumnRearrangeTable(sorted, workingSpecToFinalSpec, m_exec);
            }
            return m_tables[resultType];
        }
    }
    /**
     *
     * @author Carl Witt, KNIME AG, Zurich, Switzerland
     */
    public static class Combined extends JoinContainer implements OutputCombined {

        /**
         * To make sure the left outer matches are listed at the bottom (for legacy compatibility), we force them to be
         * larger than all left offsets by reserving another 1G offsets for the left outer matches.
         *
         * Say we have an unigned 8 bit number and want to combine a left and a right unsigned 4 bit number such that
         * when sorting by the combined number the left order dominates. What we can do:
         * <ol>
         * <li>reserve the highest order bit to get the outer matches past the matches. It can be set by
         * {@code | 0b1000_0000}</li>
         * <li>reserve the highest left outer row offset {@code 0b1111_0000} to get them past the left outer matches.
         * Their offsets are stored in the four lower bits.</li>
         * </ol>
         * So the left offset of a match can be between 0 {@code 0b0000_0000} and 7 {@code 0b0111_0000} and the left
         * offset of a unmatched row can be 0 {@code 0b1000_0000} to 6 {@code 0b1110_0000}. <br/>
         * The right offset of a match can be 0 {@code 0b0000_0000} to 15 {@code 0b0000_1111} and the right offset of a
         * unmatched row can also be 0 {@code 0b1111_0000} to 15 {@code 0b1111_1111}. <br/>
         * Since we're making full use of the 64 bits, we have to use {@link Long#compareUnsigned(long, long)} in
         * {@link LeftRightSortedJoinContainer#COMPARE_COMBINED_OFFSETS}.
         *
         */
        private static final long OUTER_ROW_BIT = 0b1000000000000000000000000000000000000000000000000000000000000000L;
        private static final long RIGHT_OUTER_BITS = 0b1111111111111111111111111111111100000000000000000000000000000000L;

        private final DataTableSpec m_workingSpec;

        private final BufferedDataContainer m_singleTableContainer;

        /**
         * Caches result returned by {@link #getTable()}.
         */
        private BufferedDataTable m_singleTableResult;

        /**
         * @param joinSpecification
         * @param exec
         * @param deduplicateResults
         * @param deferUnmatchedRows
         */

        public Combined(final JoinSpecification joinSpecification, final ExecutionContext exec,
            final boolean deduplicateResults, final boolean deferUnmatchedRows) {
            super(joinSpecification, exec, deduplicateResults, deferUnmatchedRows);

            // add a long column to each output spec for storing the combined left and right row offset
            m_workingSpec = OrderedRow.withOffset(m_outputSpecs[MATCHES]);
            m_singleTableContainer = exec.createDataContainer(m_workingSpec);

        }

        @Override
        protected boolean doAddMatch(final DataRow left, final long leftOffset, final DataRow right,
            final long rightOffset) {
            DataRow joinedProjected = m_joinSpecification.rowJoin(left, right);
            DataRow withOffsets = OrderedRow.withOffset(joinedProjected, OrderedRow.combinedOffsets(leftOffset, rightOffset));
            m_singleTableContainer.addRowToTable(withOffsets);
            return true;
        }

        @Override
        protected boolean doAddLeftOuter(final DataRow row, final long offset) {
            // use leftToSingleTableFormat to create single table row layout
            DataRow paddedMerged = leftToSingleTableFormat(row);
            DataRow withOffset = OrderedRow.withOffset(paddedMerged, (offset << 32) | OUTER_ROW_BIT);
            m_singleTableContainer.addRowToTable(withOffset);
            return true;
        }

        @Override
        protected boolean doAddRightOuter(final DataRow row, final long offset) {
            // use leftToSingleTableFormat to create single table row layout
            DataRow paddedMerged = rightToSingleTableFormat(row);
            DataRow withOffset = OrderedRow.withOffset(paddedMerged, offset | RIGHT_OUTER_BITS);
            m_singleTableContainer.addRowToTable(withOffset);
            return true;
        }

        @Override
        public BufferedDataTable getTable() throws CanceledExecutionException {
            if (m_singleTableResult == null) {
                m_unmatchedRows[LEFT_OUTER].collectUnmatched();
                m_unmatchedRows[RIGHT_OUTER].collectUnmatched();

                m_singleTableContainer.close();
                BufferedDataTable unsorted = m_singleTableContainer.getTable();

                // sort by the combined offsets
                BufferedDataTable sorter = new BufferedDataTableSorter(unsorted, COMPARE_COMBINED_OFFSETS).sort(m_exec);

                // remove sort columns
                final ColumnRearranger workingSpecToFinalSpec = OrderedRow.removeOffset(m_workingSpec);
                m_singleTableResult = m_exec.createColumnRearrangeTable(sorter, workingSpecToFinalSpec, m_exec);
            }
            return m_singleTableResult;
        }
    }
}
