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
import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DefaultCellIterator;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.join.JoinSpecification;
import org.knime.core.data.join.JoinSpecification.InputTable;
import org.knime.core.data.join.JoinSpecification.OutputRowOrder;
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

        // add two long columns to each output spec for storing the left and right row offset
        m_workingSpecs = Arrays.stream(m_outputSpecs)
                .map(tableSpec -> LeftRightOrderedRow.withOffsets(m_joinSpecification, tableSpec))
                .toArray(DataTableSpec[]::new);

        // create output containers
        m_containers = Arrays.stream(m_workingSpecs)
                .map(exec::createDataContainer)
                .toArray(BufferedDataContainer[]::new);

        m_tables = new BufferedDataTable[3];
    }

    private void add(final int rowType, final LeftRightOrderedRow row) {
        m_containers[rowType].addRowToTable(row);
    }

    @Override
    public boolean doAddMatch(final DataRow left, final long leftOffset, final DataRow right, final long rightOffset) {
        add(MATCHES, new LeftRightOrderedRow(m_joinSpecification.rowJoin(left, right), leftOffset, rightOffset));
        return true;
    }

    @Override
    public boolean doAddLeftOuter(final DataRow row, final long offset) {
        add(LEFT_OUTER, new LeftRightOrderedRow(m_joinSpecification.rowProject(InputTable.LEFT, row), offset, -1));
        return true;
    }

    @Override
    public boolean doAddRightOuter(final DataRow row, final long offset) {
        add(RIGHT_OUTER, new LeftRightOrderedRow(m_joinSpecification.rowProject(InputTable.RIGHT, row), -1, offset));
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
            if(resultType != MATCHES) {
                m_unmatchedRows[resultType].collectUnmatched();
            }

            m_containers[resultType].close();

            BufferedDataTable unsorted = m_containers[resultType].getTable();

            // sort by the first two columns
            String[] sortColumns = new String[] {
                // left row offset
                unsorted.getDataTableSpec().getColumnSpec(0).getName(),
                // right row offset
                unsorted.getDataTableSpec().getColumnSpec(1).getName()
            };
            boolean[] sortBothAscending = new boolean[]{true, true};
            BufferedDataTable sorted =
                new BufferedDataTableSorter(unsorted, Arrays.asList(sortColumns), sortBothAscending).sort(m_exec);

            // remove sort columns
            final ColumnRearranger workingSpecToFinalSpec =
                LeftRightOrderedRow.removeSortColumns(m_workingSpecs[resultType]);
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
/**
 * Adds the offset of the row in its source table to the row. This is used for persisting partitions of an input table
 * (either hash or probe). The row offsets are later on needed to sort the join results if the {@link JoinSpecification}
 * specifies {@link OutputRowOrder#DETERMINISTIC} or {@link OutputRowOrder#LEFT_RIGHT}.
 *
 * @see NWayMergeContainer.SortedChunks#nWayMerge
 * @see LeftRightSortedJoinContainer#getSingleTable()
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("javadoc")
class LeftRightOrderedRow implements DataRow, Comparable<LeftRightOrderedRow> {

    private final LongCell m_leftOrder;
    private final LongCell m_rightOrder;
    final RowKey m_rowKey;
    final DataCell[] m_cells;

    private static final Comparator<LeftRightOrderedRow> COMPARATOR =
        Comparator.comparingLong(LeftRightOrderedRow::getLeftOrder).thenComparing(LeftRightOrderedRow::getRightOrder);

    public LeftRightOrderedRow(final DataRow original, final long leftOrder, final long rightOrder) {

        m_rowKey = original.getKey();

        m_leftOrder = new LongCell(leftOrder);
        m_rightOrder = new LongCell(rightOrder);

        m_cells = new DataCell[original.getNumCells() + 2];

        m_cells[0] = m_leftOrder;
        m_cells[1] = m_rightOrder;

        for (int cell=0; cell < original.getNumCells(); cell++) {
            m_cells[cell+2] = original.getCell(cell);
        }

    }

    /**
     * @param workingSpec a working table spec generated with {@link #withOffsets(JoinSpecification, DataTableSpec)}
     * @return the original data table spec, without the auxiliary column for sorting
     */
    public static ColumnRearranger removeSortColumns(final DataTableSpec workingSpec) {
        ColumnRearranger workingSpecToFinalSpec = new ColumnRearranger(workingSpec);
        workingSpecToFinalSpec.remove(0, 1);
        return workingSpecToFinalSpec;
    }

    /**
     * Prepends two long columns to the given table specification. They hold the row offset of the left/right row that
     * contributed to a row in the join results. Row offset denotes the position of the row in its source table (left or
     * right input table). Can be -1 if it's a left/right unmatched row.
     */
    static DataTableSpec withOffsets(final JoinSpecification joinSpec, final DataTableSpec spec) {

        DataColumnSpec leftRowOffset = new DataColumnSpecCreator("Left Row Offset", LongCell.TYPE).createSpec();
        DataColumnSpec rightRowOffset = new DataColumnSpecCreator("Right Row Offset", LongCell.TYPE).createSpec();
        // make sure the column names are unique; however, as long as they don't clash, the names don't matter
        DataColumnSpec leftRowOffsetSafe = joinSpec.columnDisambiguate(leftRowOffset, spec::containsName);
        DataColumnSpec rightRowOffsetSafe = joinSpec.columnDisambiguate(rightRowOffset, spec::containsName);

        // prepend columns to get a working table spec that can be sorted by the first two columns
        return new DataTableSpecCreator()
                .addColumns(leftRowOffsetSafe, rightRowOffsetSafe)
                .addColumns(spec)
                .createSpec();
    }

    public long getLeftOrder() {
        return m_leftOrder.getLongValue();
    }

    public long getRightOrder() {
        return m_rightOrder.getLongValue();
    }


    @Override
    public Iterator<DataCell> iterator() {
        return new DefaultCellIterator(this);
    }

    @Override
    public int getNumCells() {
        return m_cells.length;
    }

    @Override
    public RowKey getKey() {
        return m_rowKey;
    }

    @Override
    public DataCell getCell(final int index) {
        return m_cells[index];
    }

    @Override
    public int compareTo(final LeftRightOrderedRow o) {
        return COMPARATOR.compare(this, o);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_leftOrder == null) ? 0 : m_leftOrder.hashCode());
        result = prime * result + ((m_rightOrder == null) ? 0 : m_rightOrder.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LeftRightOrderedRow other = (LeftRightOrderedRow)obj;
        if (m_leftOrder == null) {
            if (other.m_leftOrder != null) {
                return false;
            }
        } else if (!m_leftOrder.equals(other.m_leftOrder)) {
            return false;
        }
        if (m_rightOrder == null) {
            if (other.m_rightOrder != null) {
                return false;
            }
        } else if (!m_rightOrder.equals(other.m_rightOrder)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("(%s, %s) %s: %s", m_leftOrder, m_rightOrder, m_rowKey, Arrays.toString(m_cells));
    }

}