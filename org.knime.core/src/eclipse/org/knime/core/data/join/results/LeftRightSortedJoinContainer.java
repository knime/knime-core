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
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.CanceledExecutionException.CancelChecker;
import org.knime.core.node.ExecutionContext;

/**
 * A container for join results that need to be sorted according to {@link OutputRowOrder#LEFT_RIGHT}.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 4.2
 */
public class LeftRightSortedJoinContainer extends JoinContainer {

    private final BufferedDataContainer[] m_containers;

    private final BufferedDataTable[] m_tables;

    private final DataTableSpec[] m_workingSpecs;

    /**
     * @param joinSpecification
     * @param exec
     * @param deduplicateResults
     * @param deferUnmatchedRows
     */
    public LeftRightSortedJoinContainer(final JoinSpecification joinSpecification, final ExecutionContext exec,
        final boolean deduplicateResults, final boolean deferUnmatchedRows) {
        super(joinSpecification, exec, deduplicateResults, deferUnmatchedRows);

        // add a long column to each output spec for storing the combined left and right row offset
        m_workingSpecs = Arrays.stream(m_outputSpecs)
                .map(OrderedRow::withOffset)
                .toArray(DataTableSpec[]::new);

        // create output containers
        m_containers = Arrays.stream(m_workingSpecs)
                .map(exec::createDataContainer)
                .toArray(BufferedDataContainer[]::new);

        m_tables = new BufferedDataTable[3];
    }

    private void add(final int rowType, final DataRow row) {
        m_containers[rowType].addRowToTable(row);
    }

    @Override
    public boolean doAddMatch(final DataRow left, final long leftOffset, final DataRow right, final long rightOffset) {
        DataRow joinedProjected = m_joinSpecification.rowJoin(left, right);
        add(MATCHES, OrderedRow.withCombinedOffset(joinedProjected, leftOffset, rightOffset));
        return true;
    }

    @Override
    public boolean doAddLeftOuter(final DataRow row, final long offset) {
        DataRow projected = m_joinSpecification.rowProject(InputTable.LEFT, row);
        add(LEFT_OUTER, OrderedRow.withCombinedOffset(projected, offset, -1));
        return true;
    }

    @Override
    public boolean doAddRightOuter(final DataRow row, final long offset) {
        DataRow projected = m_joinSpecification.rowProject(InputTable.RIGHT, row);
        add(RIGHT_OUTER, OrderedRow.withCombinedOffset(projected, -1, offset));
        return true;
    }

    /**
     * Closes the result container and sorts the rows according to the left and then right row offset. The row offsets
     * are stored in the first two columns, see {@link #withOffset(DataTableSpec)}. The actual row offset values are
     * added to a result row by wrapping it in a {@link LeftRightOrderedRow} which prepends the specified row offsets as
     * two additional long cells. These are removed after sorting to comply with
     * {@link JoinSpecification#specForMatchTable()}, {@link JoinSpecification#leftUnmatchedTableSpec()}, or
     * {@link JoinSpecification#rightUnmatchedTableSpec()}, depending on the join result row type.
     *
     * @param resultType match rows, left unmatched rows, or right unmatched rows
     * @return sorted table
     * @throws CanceledExecutionException
     * @see LeftRightOrderedRow#LeftRightOrderedRow(DataRow, long, long)
     */
    private BufferedDataTable get(final int resultType) throws CanceledExecutionException {
        if (m_tables[resultType] == null) {
            // in case unmatched rows are collected deferred, this is the last possibility to collect them
            if(resultType != MATCHES) {
                m_unmatchedRows[resultType].collectUnmatched();
            }

            m_containers[resultType].close();

            BufferedDataTable unsorted = m_containers[resultType].getTable();

            // sort by the combined offsets
            BufferedDataTable sorted =
                new BufferedDataTableSorter(unsorted, Comparator.comparingLong(OrderedRow.OFFSET_EXTRACTOR))
                    .sort(m_exec);

            // remove sort columns
            final ColumnRearranger workingSpecToFinalSpec = OrderedRow.removeOffset(m_workingSpecs[resultType]);
            m_tables[resultType] = m_exec.createColumnRearrangeTable(sorted, workingSpecToFinalSpec, m_exec);
        }
        return m_tables[resultType];
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

    @Override
    public BufferedDataTable getSingleTable() throws CanceledExecutionException {

        BufferedDataContainer result = m_exec.createDataContainer(m_outputSpecs[MATCHES]);
        CancelChecker checkCanceled = CancelChecker.checkCanceledPeriodically(m_exec);

        if (m_joinSpecification.isRetainMatched()) {
            JoinResults.iterateWithResources(getMatches(), result::addRowToTable, checkCanceled);
        }

        // add left unmatched rows
        if (m_joinSpecification.isRetainUnmatched(InputTable.LEFT)) {
            JoinResults.iterateWithResources(getLeftOuter(), row -> result.addRowToTable(padRightWithMissing(row)),
                checkCanceled);
        }

        // add right unmatched rows
        if (m_joinSpecification.isRetainUnmatched(InputTable.RIGHT)) {
            JoinResults.iterateWithResources(getRightOuter(), row -> result.addRowToTable(padLeftWithMissing(row)),
                checkCanceled);
        }

        result.close();

        return result.getTable();
    }

}