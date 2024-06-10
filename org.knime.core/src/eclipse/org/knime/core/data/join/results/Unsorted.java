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

import java.util.EnumMap;

import org.knime.core.data.DataRow;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.data.join.JoinSpecification;
import org.knime.core.data.join.JoinSpecification.InputTable;
import org.knime.core.data.join.JoinSpecification.OutputRowOrder;
import org.knime.core.data.join.implementation.JoinImplementation;
import org.knime.core.data.join.results.JoinResult.OutputCombined;
import org.knime.core.data.join.results.JoinResult.OutputSplit;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;

/**
 * A minimal container for join results that can be output in any order, i.e., when the {@link JoinSpecification}
 * specifies {@link OutputRowOrder#ARBITRARY}. <br/>
 * This is the fastest way to collect join results, because we can avoid sorting and creating additional objects to
 * store row order.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 4.2
 */
public final class Unsorted {

    /**
     * @param joinImplementation an implementation of a join algorithm (provides join specification and implementation
     *            details, e.g., whether hiliting is on).
     * @return a join container that returns results sorted by left row offset, then right row offset.
     */
    public static JoinResult<OutputSplit> createSplit(final JoinImplementation joinImplementation) {
        return new Split(joinImplementation);
    }

    /**
     * @param joinImplementation an implementation of a join algorithm (provides join specification and implementation
     *            details, e.g., whether hiliting is on).
     * @return a join container that returns results as a combined table
     */
    public static JoinResult<OutputCombined> createCombined(final JoinImplementation joinImplementation) {
        return new Combined(joinImplementation);
    }

    /**
     * Split output version of the unordered join container. Maintains separate tables for all result types. Unmatched
     * rows are projected to the included columns of the left/right table and unaffected by the merge join columns
     * option. Only the matches are affected by this option.
     */
    private static class Split extends JoinContainer<OutputSplit> {

        /**
         * The containers that hold the {@link DataRow}s that become the {@link #m_splitOutputResults}.
         */
        private final EnumMap<ResultType, BufferedDataContainer> m_splitOutputContainers =
            new EnumMap<>(ResultType.class);

        /**
         * Caches the results returned by {@link #get(int)}, i.e., the three split output tables for matches, left
         * unmatched, and right unmatched rows.
         */
        private final EnumMap<ResultType, BufferedDataTable> m_splitOutputResults = new EnumMap<>(ResultType.class);

        private final OutputSplit m_outputSplit = new OutputSplit() {
            @Override
            public BufferedDataTable getMatches() throws CanceledExecutionException {
                return get(ResultType.MATCHES);
            }

            @Override
            public BufferedDataTable getLeftOuter() throws CanceledExecutionException {
                return get(ResultType.LEFT_OUTER);
            }

            @Override
            public BufferedDataTable getRightOuter() throws CanceledExecutionException {
                return get(ResultType.RIGHT_OUTER);
            }
        };

        /**
         * @param joinImplementation as in {@link JoinContainer#JoinContainer(JoinImplementation)}
         */
        Split(final JoinImplementation joinImplementation) {
            super(joinImplementation);
            // create working specs and output containers
            for (ResultType rt : ResultType.MATCHES_AND_OUTER) {
                final var containerSettings = DataContainerSettings.builder().withCheckDuplicateRowKeys(
                    !joinImplementation.getJoinSpecification().isRowKeyFactoryCreatesUniqueKeys()).build();

                m_splitOutputContainers.put(rt, joinImplementation.getExecutionContext()
                    .createDataContainer(m_outputSpecs.get(rt), containerSettings));
            }
        }

        @Override
        public void doAddMatch(final DataRow left, final long leftOffset, final DataRow right,
            final long rightOffset) {
            final DataRow match = m_joinSpecification.rowJoin(left, right);
            m_splitOutputContainers.get(ResultType.MATCHES).addRowToTable(match);
            addHiliteMapping(ResultType.MATCHES, match.getKey(), InputTable.LEFT, left.getKey());
            addHiliteMapping(ResultType.MATCHES, match.getKey(), InputTable.RIGHT, right.getKey());
        }

        @Override
        public void doAddLeftOuter(final DataRow row, final long offset) {
            DataRow leftOuter = m_joinSpecification.rowProjectOuter(InputTable.LEFT, row);
            m_splitOutputContainers.get(ResultType.LEFT_OUTER).addRowToTable(leftOuter);
            addHiliteMapping(ResultType.LEFT_OUTER, leftOuter.getKey(), InputTable.LEFT, row.getKey());
        }

        @Override
        public void doAddRightOuter(final DataRow row, final long offset) {
            DataRow rightOuter = m_joinSpecification.rowProjectOuter(InputTable.RIGHT, row);
            m_splitOutputContainers.get(ResultType.RIGHT_OUTER).addRowToTable(rightOuter);
            addHiliteMapping(ResultType.RIGHT_OUTER, rightOuter.getKey(), InputTable.RIGHT, row.getKey());
        }

        BufferedDataTable get(final ResultType resultType) throws CanceledExecutionException {

            if (m_splitOutputResults.get(resultType) == null) { // NOSONAR computeIfAbsent: code may throw exception
                collectUnmatchedRows(resultType);
                m_splitOutputContainers.get(resultType).close();
                m_splitOutputResults.put(resultType, m_splitOutputContainers.get(resultType).getTable());
            }
            return m_splitOutputResults.get(resultType);
        }

        @Override
        public OutputSplit getResults() {
            return m_outputSplit;
        }
    }

    /**
     * Single table version of the unordered join container. Since unmatched rows are included along with matched rows,
     * they need to be padded with missing values and are also affected by the merge join columns option.
     */
    private static class Combined extends JoinContainer<OutputCombined> {

        private final BufferedDataContainer m_singleTableContainer;

        private final OutputCombined m_outputCombined = new OutputCombined() {
            @Override
            public BufferedDataTable getTable() throws CanceledExecutionException {
                if (m_singleTableResult == null) {
                    collectUnmatchedRows(ResultType.ALL);

                    m_singleTableContainer.close();
                    m_singleTableResult = m_singleTableContainer.getTable();
                }
                return m_singleTableResult;
            }
        };

        /**
         * Caches result returned by {@link #getTable()}.
         */
        private BufferedDataTable m_singleTableResult;

        /**
         * @param joinImplementation
         */
        Combined(final JoinImplementation joinImplementation) {
            super(joinImplementation);
            final var containerSettings = DataContainerSettings.builder().withCheckDuplicateRowKeys(
                !joinImplementation.getJoinSpecification().isRowKeyFactoryCreatesUniqueKeys()).build();

            m_singleTableContainer = joinImplementation.getExecutionContext()
                .createDataContainer(m_outputSpecs.get(ResultType.MATCHES), containerSettings);
        }

        @Override
        public void doAddMatch(final DataRow left, final long leftOrder, final DataRow right,
            final long rightOrder) {
            DataRow match = m_joinSpecification.rowJoin(left, right);
            m_singleTableContainer.addRowToTable(match);
            addHiliteMapping(ResultType.MATCHES, match.getKey(), InputTable.LEFT, left.getKey());
            addHiliteMapping(ResultType.MATCHES, match.getKey(), InputTable.RIGHT, right.getKey());
        }

        @Override
        public void doAddLeftOuter(final DataRow row, final long offset) {
            DataRow paddedMerged = m_joinSpecification.leftToSingleTableFormat(row);
            m_singleTableContainer.addRowToTable(paddedMerged);
            addHiliteMapping(ResultType.LEFT_OUTER, paddedMerged.getKey(), InputTable.LEFT, row.getKey());
        }

        @Override
        public void doAddRightOuter(final DataRow row, final long offset) {
            DataRow paddedMerged = m_joinSpecification.rightToSingleTableFormat(row);
            m_singleTableContainer.addRowToTable(paddedMerged);
            addHiliteMapping(ResultType.RIGHT_OUTER, paddedMerged.getKey(), InputTable.RIGHT, row.getKey());
        }

        @Override
        public OutputCombined getResults() {
            return m_outputCombined;
        }

    }

    private Unsorted() {
        // utility class
    }

}
