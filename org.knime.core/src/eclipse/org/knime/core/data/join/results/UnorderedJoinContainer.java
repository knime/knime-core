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

import org.knime.core.data.DataRow;
import org.knime.core.data.join.JoinSpecification;
import org.knime.core.data.join.JoinSpecification.InputTable;
import org.knime.core.data.join.JoinSpecification.OutputRowOrder;
import org.knime.core.data.join.results.JoinResults.OutputCombined;
import org.knime.core.data.join.results.JoinResults.OutputMode;
import org.knime.core.data.join.results.JoinResults.OutputSplit;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 * A minimal container for join results that can be output in any order, i.e., when the {@link JoinSpecification}
 * specifies {@link OutputRowOrder#ARBITRARY}. <br/>
 * This is the fastest way to collect join results, because we can avoid sorting and creating additional objects to
 * store row order.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 4.2
 */
public interface UnorderedJoinContainer {

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


    /**
     * Split output version of the unordered join container. Maintains separate tables for all result types. Unmatched
     * rows are projected to the included columns of the left/right table and unaffected by the merge join columns
     * option. Only the matches are affected by this option.
     */
    public static class Split extends JoinContainer implements OutputSplit {

        /**
         * The containers that hold the {@link DataRow}s that become the {@link #m_splitOutputResults}.
         */
        private final BufferedDataContainer[] m_splitOutputContainers;

        /**
         * Caches the results returned by {@link #get(int)}, i.e., the three split output tables for matches, left
         * unmatched, and right unmatched rows.
         */
        private final BufferedDataTable[] m_splitOutputResults;

        /**
         * @param joinSpecification as in
         *            {@link JoinContainer#JoinContainer(JoinSpecification, ExecutionContext, boolean, boolean)}
         * @param exec as in {@link JoinContainer#JoinContainer(JoinSpecification, ExecutionContext, boolean, boolean)}
         * @param deduplicateResults as in
         *            {@link JoinContainer#JoinContainer(JoinSpecification, ExecutionContext, boolean, boolean)}
         * @param deferUnmatchedRows
         */
        public Split(final JoinSpecification joinSpecification, final ExecutionContext exec,
            final boolean deduplicateResults, final boolean deferUnmatchedRows) {
            super(joinSpecification, exec, deduplicateResults, deferUnmatchedRows);
            m_splitOutputContainers =
                Arrays.stream(m_outputSpecs).map(exec::createDataContainer).toArray(BufferedDataContainer[]::new);
            m_splitOutputResults = new BufferedDataTable[3];
        }

        @Override
        public boolean doAddMatch(final DataRow left, final long leftOffset, final DataRow right,
            final long rightOffset) {
            DataRow match = m_joinSpecification.rowJoin(left, right);
            final DataRow row = match;
            m_splitOutputContainers[MATCHES].addRowToTable(row);
            return true;
        }

        @Override
        public boolean doAddLeftOuter(final DataRow row, final long offset) {
            DataRow leftOuter = m_joinSpecification.rowProjectOuter(InputTable.LEFT, row);
            m_splitOutputContainers[LEFT_OUTER].addRowToTable(leftOuter);
            return true;
        }

        @Override
        public boolean doAddRightOuter(final DataRow row, final long offset) {
            DataRow rightOuter = m_joinSpecification.rowProjectOuter(InputTable.RIGHT, row);
            m_splitOutputContainers[RIGHT_OUTER].addRowToTable(rightOuter);
            return true;
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

        private BufferedDataTable get(final int resultType) throws CanceledExecutionException {
            if (m_splitOutputResults[resultType] == null) {
                if (resultType != MATCHES) {
                    m_unmatchedRows[resultType].collectUnmatched();
                }
                m_splitOutputContainers[resultType].close();
                m_splitOutputResults[resultType] = m_splitOutputContainers[resultType].getTable();
            }
            return m_splitOutputResults[resultType];
        }
    }

    /**
     * Single table version of the unordered join container.
     * Since unmatched rows are included along with matched rows, they need to be padded with missing values and
     * are also affected by the merge join columns option.
     */
    public static class Combined extends JoinContainer implements OutputCombined {

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
            m_singleTableContainer = exec.createDataContainer(m_outputSpecs[MATCHES]);
        }

        @Override
        protected boolean doAddMatch(final DataRow left, final long leftOrder, final DataRow right,
            final long rightOrder) {
            DataRow match = m_joinSpecification.rowJoin(left, right);
            m_singleTableContainer.addRowToTable(match);
            return true;
        }

        @Override
        protected boolean doAddLeftOuter(final DataRow row, final long offset) {
            DataRow paddedMerged = leftToSingleTableFormat(row);
            m_singleTableContainer.addRowToTable(paddedMerged);
            return true;
        }

        @Override
        protected boolean doAddRightOuter(final DataRow row, final long offset) {
            DataRow paddedMerged = rightToSingleTableFormat(row);
            m_singleTableContainer.addRowToTable(paddedMerged);
            return true;
        }

        @Override
        public BufferedDataTable getTable() throws CanceledExecutionException {
            if (m_singleTableResult == null) {
                m_unmatchedRows[LEFT_OUTER].collectUnmatched();
                m_unmatchedRows[RIGHT_OUTER].collectUnmatched();

                m_singleTableContainer.close();
                m_singleTableResult = m_singleTableContainer.getTable();
            }
            return m_singleTableResult;
        }
    }


}
