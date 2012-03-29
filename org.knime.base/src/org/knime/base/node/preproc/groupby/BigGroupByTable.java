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
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;
import org.knime.core.util.MutableInteger;
import org.knime.core.util.Pair;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.data.aggregation.GlobalSettings;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
     * @param globalSettings the global settings
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
            final ColumnAggregator[] colAggregators,
            final GlobalSettings globalSettings, final boolean sortInMemory,
            final boolean enableHilite, final ColumnNamePolicy colNamePolicy,
            final boolean retainOrder)
    throws CanceledExecutionException {
        super(exec, inDataTable, groupByCols, colAggregators, globalSettings,
                sortInMemory, enableHilite, colNamePolicy, retainOrder);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected BufferedDataTable createGroupByTable(final ExecutionContext exec,
            final BufferedDataTable table, final DataTableSpec resultSpec,
            final int[] groupColIdx) throws CanceledExecutionException {
        LOGGER.debug("Entering createGroupByTable(exec, table) "
                + "of class BigGroupByTable.");
        final DataTableSpec origSpec = table.getDataTableSpec();
        //sort the data table in order to process the input table chunk wise
        final BufferedDataTable sortedTable;
        final ExecutionContext groupExec;
        final DataValueComparator[] comparators;
        if (groupColIdx.length < 1) {
            sortedTable = table;
            groupExec = exec;
            comparators = new DataValueComparator[0];
        } else {
            final ExecutionContext sortExec =
                exec.createSubExecutionContext(0.6);
            exec.setMessage("Sorting input table...");
            sortedTable = sortTable(sortExec, table,
                    getGroupCols(), isSortInMemory());
            sortExec.setProgress(1.0);
            groupExec = exec.createSubExecutionContext(0.4);
            comparators = new DataValueComparator[groupColIdx.length];
            for (int i = 0, length = groupColIdx.length; i < length; i++) {
                final DataColumnSpec colSpec =
                    origSpec.getColumnSpec(groupColIdx[i]);
                comparators[i] = colSpec.getType().getComparator();
            }
        }
        final BufferedDataContainer dc = exec.createDataContainer(resultSpec);
        final DataCell[] previousGroup = new DataCell[groupColIdx.length];
        final DataCell[] currentGroup = new DataCell[groupColIdx.length];
        final MutableInteger groupCounter = new MutableInteger(0);
        boolean firstRow = true;
        final double progressPerRow = 1.0 / sortedTable.getRowCount();
        int rowCounter = 0;
        //In the rare case that the DataCell comparator return 0 for two
        //data cells that are not equal we have to maintain a map with all
        //rows with equal cells in the group columns per chunk.
        //This variable stores for each chunk these members. A chunk consists
        //of rows which return 0 for the pairwise group value comparison.
        //Usually only equal data cells return 0 when compared with each other
        //but in rare occasions also data cells that are NOT equal return 0 when
        //compared to each other 
        //(such as cells that contain chemical structures).
        //In this rare case this map will contain for each group of data cells
        //that are pairwise equal in the chunk a separate entry.
        final Map<GroupKey, Pair<ColumnAggregator[], Set<RowKey>>>
            chunkMembers = new HashMap<GroupKey,
            Pair<ColumnAggregator[], Set<RowKey>>>(3);
        boolean logUnusualCells = true;
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
            //check if we are still in the same data chunk which contains
            //rows that return 0 for all pairwise comparisons of their
            //group column data cells
            if (!sameChunk(comparators, previousGroup, currentGroup)) {
                createTableRows(dc, chunkMembers, groupCounter);
                //set the current group as previous group
                System.arraycopy(currentGroup, 0, previousGroup, 0,
                        currentGroup.length);
                if (logUnusualCells && chunkMembers.size() > 1) {
                    //log unusual number of chunk members with the classes that
                    //cause the problem
                    if (LOGGER.isEnabledFor(LEVEL.INFO)) {
                        final StringBuilder buf = new StringBuilder();
                        buf.append("Data chunk with ");
                        buf.append(chunkMembers.size());
                        buf.append(" members occured in groupby node. "
                                + "Involved classes are: ");
                        final GroupKey key = 
                            chunkMembers.keySet().iterator().next();
                        for (final DataCell cell : key.getGroupVals()) {
                            buf.append(cell.getClass().getCanonicalName());
                            buf.append(", ");
                        }
                        LOGGER.info(buf.toString());
                    }
                    logUnusualCells = false;
                }
                //reset the chunk members map
                chunkMembers.clear();
            }
            //process the row as one of the members of the current chunk
            Pair<ColumnAggregator[], Set<RowKey>> member =
                chunkMembers.get(new GroupKey(currentGroup));
            if (member == null) {
                Set<RowKey> rowKeys;
                if (isEnableHilite()) {
                    rowKeys = new HashSet<RowKey>();
                } else {
                    rowKeys = Collections.EMPTY_SET;
                }
                member = new Pair<ColumnAggregator[], Set<RowKey>>(
                        cloneColumnAggregators(), rowKeys);
                final DataCell[] groupKeys = new DataCell[currentGroup.length];
                System.arraycopy(currentGroup, 0, groupKeys, 0,
                        currentGroup.length);
                chunkMembers.put(new GroupKey(groupKeys), member);
            }
            //compute the current row values
            for (final ColumnAggregator colAggr : member.getFirst()) {
                final DataCell cell = row.getCell(origSpec.findColumnIndex(
                        colAggr.getOriginalColName()));
                colAggr.getOperator(getGlobalSettings()).compute(cell);
            }
            if (isEnableHilite()) {
                member.getSecond().add(row.getKey());
            }
            groupExec.checkCanceled();
            groupExec.setProgress(progressPerRow * rowCounter++);
        }
        //create the final row for the last chunk after processing the last
        //table row
        createTableRows(dc, chunkMembers, groupCounter);
        dc.close();
        return dc.getTable();
    }

    /**
     * Creates and adds the result rows for the members of a data chunk to the
     * given data container. It also handles the row key mapping if hilite
     * translation is enabled.
     *
     * @param dc the {@link DataContainer} to use
     * @param chunkMembers the members of the current data chunk
     * @param groupCounter the number of groups that have been created
     * so fare
     */
    private void createTableRows(final BufferedDataContainer dc,
            final Map<GroupKey,
                        Pair<ColumnAggregator[], Set<RowKey>>> chunkMembers,
            final MutableInteger groupCounter) {
        if (chunkMembers == null || chunkMembers.isEmpty()) {
            return;
        }
        for (final Entry<GroupKey, Pair<ColumnAggregator[], Set<RowKey>>> e
                : chunkMembers.entrySet()) {
            final DataCell[] groupVals = e.getKey().getGroupVals();
            final ColumnAggregator[] colAggregators = e.getValue().getFirst();
            final RowKey rowKey = RowKey.createRowKey(groupCounter.intValue());
            groupCounter.inc();
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
                    colAggr.getOperator(getGlobalSettings());
                rowVals[valIdx++] = operator.getResult();
                if (operator.isSkipped()) {
                    //add skipped groups and the column that causes the
                    //skipping into the skipped groups map
                    addSkippedGroup(colAggr.getOriginalColName(),
                            operator.getSkipMessage(), groupVals);
                }
            }
            final DataRow newRow = new DefaultRow(rowKey, rowVals);
            dc.addRowToTable(newRow);
            if (isEnableHilite()) {
                final Set<RowKey> oldKeys = e.getValue().getSecond();
                addHiliteMapping(rowKey, oldKeys);
            }
        }
    }

    /**
     * @return a copy of the column aggregators
     */
    private ColumnAggregator[] cloneColumnAggregators() {
        final ColumnAggregator[] origAggregators = getColAggregators();
        final ColumnAggregator[] aggregators =
            new ColumnAggregator[origAggregators.length];
        for (int i = 0, length = origAggregators.length; i < length; i++) {
            aggregators[i] = origAggregators[i].clone();
        }
        return aggregators;
    }

    /**
     * Returns <code>true</code> if both {@link DataCell} groups return
     * 0 for each pairwise comparison of their elements at the same position.
     *
     * @param comparators the {@link DataValueComparator}s to use
     * @param previousGroup the values of the previous group
     * @param currentGroup the values of the current group
     * @return <code>true</code> if both groups return 0 when all pairs are
     * compared using the given {@link DataValueComparator}s
     */
    private boolean sameChunk(final DataValueComparator[] comparators,
            final DataCell[] previousGroup, final DataCell[] currentGroup) {
        if (comparators == null || comparators.length < 1) {
            return true;
        }
        for (int i = 0, length = comparators.length; i < length; i++) {
            if (comparators[i].compare(previousGroup[i], currentGroup[i])
                    != 0) {
                return false;
            }
        }
        return true;
    }
}
