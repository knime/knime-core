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
 *   Jun 12, 2020 (carlwitt): created
 */
package org.knime.core.data.join.implementation;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.join.JoinSpecification;
import org.knime.core.data.join.JoinSpecification.OutputRowOrder;
import org.knime.core.data.join.JoinTableSettings;
import org.knime.core.data.join.results.LeftRightSorted;
import org.knime.core.data.join.results.RowHandlerCancelable;

/**
 * Tools to add a long column to tables. This is used to annotate the offset of a row in its source table to the row.
 * This is necessary for
 * <ul>
 * <li>persisting partitions of an input table (either hash or probe) in {@link DiskBackedHashPartitions}</li>
 * <li>for disk-based sorting, as in {@link NWayMergeContainer}</li>
 * </ul>
 * The row offsets are later on needed to sort the join results if the {@link JoinSpecification} specifies
 * {@link OutputRowOrder#DETERMINISTIC} or {@link OutputRowOrder#LEFT_RIGHT}.
 *
 * @see NWayMergeContainer.SortedChunks#nWayMerge
 * @see LeftRightSorted#getTable()
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 4.2
 */
@SuppressWarnings("javadoc")
public final class OrderedRow { // implements DataRow, Comparable<OrderedRow>

    /**
     * Extracts the row order (= offset in containing table) from a persisted row.
     */
    static long getOffset(final DataRow row) {
        return ((LongCell)row.getCell(0)).getLongValue();
    }

    public static int compareUnsignedOffsets(final DataRow row1, final DataRow row2){
        return Long.compareUnsigned(OrderedRow.getOffset(row1), OrderedRow.getOffset(row2));
    }

    /**
     * Creates a new table spec with an additional long column. The column is used to hold the offset of the probe input
     * row that contributed to this join result. Can be -1 if it's an unmatched row from the hash input. Use
     * {@link #withOffset(DataRow, long)} to create a {@link DataRow} with some long materialized to a {@link LongCell}
     * according to the spec returned here.
     *
     * @param spec spec to copy and change
     * @return base table spec to copy and add a column to
     */
    public static DataTableSpec withOffset(final DataTableSpec spec) {

        DataColumnSpec rowOffset = new DataColumnSpecCreator("__row_offset__", LongCell.TYPE).createSpec();
        // make sure the column names are unique; however, as long as they don't clash, the names don't matter
        DataColumnSpec rowOffsetSafe =
            JoinSpecification.columnDisambiguate(rowOffset, spec::containsName, s -> s.concat("_"));

        // prepend columns to get a working table spec that can be sorted by the first two columns
        return new DataTableSpecCreator().addColumns(rowOffsetSafe).addColumns(spec).createSpec();
    }

    /**
     * Creates a new row with the given long value added as a {@link LongCell}.
     *
     * @param row
     * @param rowOffset
     * @return
     */
    public static DataRow withOffset(final DataRow row, final long rowOffset) {
        DataCell[] cells = new DataCell[row.getNumCells() + 1];
        int cell = 0;
        // add long row in the correct position
        cells[cell] = new LongCell(rowOffset);
        cell++;
        // copy row contents
        for (int i = 0; i < row.getNumCells(); i++) {
            cells[cell] = row.getCell(i);
            cell++;
        }
        return new DefaultRow(row.getKey(), cells);
    }

    /**
     * Revert the {@link #withOffset(DataTableSpec)} operation
     *
     * <pre>
     * {@code
     * // usage:
     * DataTableSpec workingSpec = OrderedRow.withOffsets(join settings, input table spec);
     * final ColumnRearranger cr = OrderedRow.removeSortColumns(workingSpec);
     * BufferedDataTable result = m_exec.createColumnRearrangeTable(sorted, cr, m_exec);
     * }
     *
     * </pre>
     *
     * @param workingSpec a working table spec generated with {@link #withOffset(DataTableSpec)}
     * @return the original data table spec, without the auxiliary column for sorting
     */
    public static ColumnRearranger removeOffset(final DataTableSpec workingSpec) {
        ColumnRearranger workingSpecToFinalSpec = new ColumnRearranger(workingSpec);
        workingSpecToFinalSpec.remove(0);
        return workingSpecToFinalSpec;
    }

    /**
     * Revert the {@link #withOffset(DataRow, long)} operation
     *
     * @param row a data row with offset information
     * @return the data row without the offset information
     */
    static DataRow removeOffset(final DataRow row) {
        DataCell[] cells = new DataCell[row.getNumCells() - 1];
        for (int i = 1; i < row.getNumCells(); i++) {
            cells[i - 1] = row.getCell(i);
        }
        return new DefaultRow(row.getKey(), cells);
    }

    /**
     * Transform the data row into condensed table format.
     *
     * @param joinTable provides the include and join columns
     * @param row a row from the table that joinTable describes
     * @param rowOffset an optional offset
     * @param storeRowOffsets whether to store rowOffset as an additional column according to
     *            {@link #withOffset(DataRow, long)}
     * @return data row with only the columns needed for joining: join column and include columns and an optional row
     *         offset
     * @see JoinTableSettings#condensed(DataRow, long, boolean)
     */
    public static DataRow materialize(final JoinTableSettings joinTable, final DataRow row, final long rowOffset,
        final boolean storeOffset) {

        int[] copyCellIndices = joinTable.getMaterializeColumnIndices();
        int numCells = copyCellIndices.length + (storeOffset ? 1 : 0);
        DataCell[] cells = new DataCell[numCells];
        int cell = 0;
        if (storeOffset) {
            cells[cell] = new LongCell(rowOffset);
            cell++;
        }
        // keep only join columns and include columns
        for (int i = 0; i < copyCellIndices.length; i++) {
            cells[cell] = row.getCell(copyCellIndices[i]);
            cell++;
        }
        // optionally store row offset

        return new DefaultRow(row.getKey(), cells);
    }

    /**
     * Transforms a handler for a {@link DataRow} <i>r</i> and its offset <i>o</i> such that the given offset <i>o</i>
     * is replaced with the offset extracted from the row using {@link OrderedRow#OFFSET_EXTRACTOR}
     *
     * @param rowHandler the original row handler
     * @return the rowHandler function composed with an offset extractor and removal step.
     */
    static <T extends RowHandlerCancelable> RowHandlerCancelable extractOffsets(final T rowHandler) {
        return (row, offset) -> rowHandler.accept(row, OrderedRow.getOffset(row));
    }

    /**
     * Combine two offsets into one long such that sorting the combined values first sorts according to left value then
     * according to right value.
     *
     * @param left a 32-bit unsigned integer, i.e., at most ~4G
     * @param right a 32-bit unsigned integer, i.e., at most ~4G
     * @return a 64-bit unsigned long containing the left value in the upper 32 bits and the right in the lower 32 bits
     */
    public static long combinedOffsets(final long left, final long right) {
        // make some space for shifting
        long result = left;
        // move left offset to highest order bits
        result <<= 32;
        // add right offset in the lower order bits
        result += right;
        return result;
    }

    private OrderedRow() {
        // utility class
    }

}