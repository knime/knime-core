/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * -------------------------------------------------------------------
 */

package org.knime.base.node.preproc.groupby;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.ColumnAggregator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * A data table that groups a given input table by the given columns
 * and calculates the aggregation values of the remaining rows. Call the
 * {@link #getBufferedTable()} method after instance creation to get the
 * grouped table. If the enableHilite flag was set to <code>true</code> call
 * the {@link #getHiliteMapping()} method to get the row key translation
 * <code>Map</code>. Call the {@link #getSkippedGroupsByColName()} method
 * to get a <code>Map</code> with all skipped groups or the
 * {@link #getSkippedGroupsMessage(int, int)} for a appropriate warning message.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class BigGroupByTable extends
GroupByTable {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(BigGroupByTable.class);

    /**Constructor for class BigGroupByTable.
     * @param exec the <code>ExecutionContext</code>
     * @param inDataTable the table to aggregate
     * @param groupByCols the name of all columns to group by
     * @param colAggregators the aggregation columns with the aggregation method
     * to use in the order the columns should be appear in the result table
     * numerical columns
     * @param maxUniqueValues the maximum number of unique values
     * @param sortInMemory <code>true</code> if the table should be sorted in
     * the memory
     * @param enableHilite <code>true</code> if a row key map should be
     * maintained to enable hiliting
     * @param colNamePolicy the {@link ColumnNamePolicy} for the
     * aggregation columns
     * @param retainOrder returns the row of the table in the same order as the
     * input table if set to <code>true</code>
     * @throws CanceledExecutionException if the user has canceled the execution
     */
    public BigGroupByTable(final ExecutionContext exec,
            final BufferedDataTable inDataTable,
            final List<String> groupByCols,
            final ColumnAggregator[] colAggregators, final int maxUniqueValues,
            final boolean sortInMemory, final boolean enableHilite,
            final ColumnNamePolicy colNamePolicy, final boolean retainOrder)
    throws CanceledExecutionException {
        super(exec, inDataTable, groupByCols, colAggregators, maxUniqueValues,
                sortInMemory, enableHilite, colNamePolicy, retainOrder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable createGroupByTable(final ExecutionContext exec,
            final BufferedDataTable table, final DataTableSpec resultSpec,
            final int[] groupColIdx) throws CanceledExecutionException {
        LOGGER.debug("Entering createGroupByTable(exec, table) "
                + "of class BigGroupByTable.");
        final BufferedDataTable sortedTable;
        final ExecutionContext groupExec;
        if (groupColIdx.length < 1) {
            sortedTable = table;
            groupExec = exec;
        } else {
            final ExecutionContext sortExec =
                exec.createSubExecutionContext(0.6);
            exec.setMessage("Sorting input table...");
            sortedTable = sortTable(sortExec, table,
                    getGroupCols(), isSortInMemory());
            sortExec.setProgress(1.0);
            groupExec = exec.createSubExecutionContext(0.4);
        }
        final BufferedDataContainer dc = exec.createDataContainer(resultSpec);
        final DataTableSpec origSpec = table.getDataTableSpec();
        final DataCell[] previousGroup = new DataCell[groupColIdx.length];
        final DataCell[] currentGroup = new DataCell[groupColIdx.length];
        Set<RowKey> rowKeys = new HashSet<RowKey>();
        int groupCounter = 0;
        boolean firstRow = true;
        final double progressPerRow = 1.0 / sortedTable.getRowCount();
        int rowCounter = 0;
        for (final DataRow row : sortedTable) {
            //fetch the current group column values
            for (int i = 0, length = groupColIdx.length; i < length; i++) {
                currentGroup[i] = row.getCell(groupColIdx[i]);
            }
            if (firstRow) {
                System.arraycopy(currentGroup, 0, previousGroup, 0,
                        currentGroup.length);
                firstRow = false;
            }
            //check if we are still in the same group
            if (!Arrays.equals(previousGroup, currentGroup)) {
                //this is a new group
                final DataRow newRow = createTableRow(previousGroup,
                        groupCounter);
                dc.addRowToTable(newRow);
                //increase the group counter
                groupCounter++;
                //set the current group as previous group
                System.arraycopy(currentGroup, 0, previousGroup, 0,
                        currentGroup.length);

                //add the row keys to the hilite map if the flag is true
                if (isEnableHilite()) {
                    addHiliteMapping(newRow.getKey(), rowKeys);
                    rowKeys = new HashSet<RowKey>();
                }
            }
            //compute the current row values
            for (final ColumnAggregator colAggr : getColAggregators()) {
                final DataCell cell = row.getCell(origSpec.findColumnIndex(
                        colAggr.getColName()));
                colAggr.getOperator(getMaxUniqueVals()).compute(cell);
            }
            if (isEnableHilite()) {
                rowKeys.add(row.getKey());
            }
            groupExec.checkCanceled();
            groupExec.setProgress(progressPerRow * rowCounter++);
        }
        //create the final row for the last group after processing the last
        //table row
        final DataRow newRow = createTableRow(previousGroup, groupCounter);
        dc.addRowToTable(newRow);
        //add the row keys to the hilite map if the flag is true
        if (isEnableHilite()) {
            addHiliteMapping(newRow.getKey(), rowKeys);
        }
        dc.close();
        return dc.getTable();
    }

    private DataRow createTableRow(final DataCell[] groupVals,
            final int groupCounter) {
        final RowKey rowKey = RowKey.createRowKey(groupCounter);
        final ColumnAggregator[] colAggregators = getColAggregators();
        final DataCell[] rowVals =
            new DataCell[groupVals.length + colAggregators.length];
        //add the group values first
        int valIdx = 0;
        for (final DataCell groupCell : groupVals) {
            rowVals[valIdx++] = groupCell;
        }
        //add the aggregation values
        for (final ColumnAggregator colAggr : colAggregators) {
            final AggregationOperator operator =
                colAggr.getOperator(getMaxUniqueVals());
            rowVals[valIdx++] = operator.getResult();
            if (operator.isSkipped()) {
                //add skipped groups and the column that causes the skipping
                //into the skipped groups map
                addSkippedGroup(colAggr.getColName(), groupVals);
            }
            //reset the operator for the next group
            operator.reset();
        }
        final DataRow newRow = new DefaultRow(rowKey, rowVals);
        return newRow;
    }
}
