/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 *
 * History
 *    29.06.2007 (Tobias Koetter): created
 */

package org.knime.base.node.preproc.groupby;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.MutableInteger;

import org.knime.base.data.sort.SortedTable;
import org.knime.base.node.preproc.groupby.aggregation.AggregationOperator;
import org.knime.base.node.preproc.groupby.aggregation.ColumnAggregator;
import org.knime.base.node.preproc.sorter.SorterNodeDialogPanel2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
public class GroupByTable {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(GroupByTable.class);

    private final List<String> m_groupCols;

    private final int m_maxUniqueVals;

    private final boolean m_enableHilite;

    private final boolean m_keepColName;

    private final Map<RowKey, Set<RowKey>> m_hiliteMapping;

    private final BufferedDataTable m_resultTable;

    private final Map<String, Collection<String>> m_skippedGroupsByColName =
        new HashMap<String, Collection<String>>();

    private final ColumnAggregator[] m_colAggregators;

    /**Constructor for class GroupByTable.
     * @param exec the <code>ExecutionContext</code>
     * @param dataTable the table to aggregate
     * @param groupByCols the name of all columns to group by
     * @param colAggregators the aggregation columns with the aggregation method
     * to use in the order the columns should be appear in the result table
     * numerical columns
     * @param maxUniqueValues the maximum number of unique values
     * @param sortInMemory <code>true</code> if the table should be sorted in
     * the memory
     * @param enableHilite <code>true</code> if a row key map should be
     * maintained to enable hiliting
     * @param keepColName <code>true</code> if the original column name should
     * be kept
     * @throws CanceledExecutionException if the user has canceled the execution
     */
    public GroupByTable(final ExecutionContext exec,
            final BufferedDataTable dataTable,
            final List<String> groupByCols,
            final ColumnAggregator[] colAggregators, final int maxUniqueValues,
            final boolean sortInMemory, final boolean enableHilite,
            final boolean keepColName)
    throws CanceledExecutionException {
        LOGGER.debug("Entering GroupByTable() of class GroupByTable.");
        if (dataTable == null) {
            throw new NullPointerException("DataTable must not be null");
        }
        checkGroupCols(dataTable.getDataTableSpec(), groupByCols);
        if (maxUniqueValues < 0) {
            throw new IllegalArgumentException(
                    "Maximum unique values must be a positive integer");
        }
        if (exec == null) {
            throw new NullPointerException("Exec must not be null");
        }
        m_enableHilite = enableHilite;
        if (m_enableHilite) {
            m_hiliteMapping = new HashMap<RowKey, Set<RowKey>>();
        } else {
            m_hiliteMapping = null;
        }

        m_groupCols = groupByCols;
        m_colAggregators = colAggregators;
        m_maxUniqueVals = maxUniqueValues;
        m_keepColName = keepColName;
        exec.setMessage("Sorting input table...");
        ExecutionContext subExec = exec.createSubExecutionContext(0.6);
        final SortedTable sortedTable =
            sortInputTable(subExec, dataTable, m_groupCols, sortInMemory);
        subExec.setProgress(1.0);
        exec.setMessage("Writing group table...");
        subExec = exec.createSubExecutionContext(0.4);
        m_resultTable = createGroupByTable(subExec, sortedTable);
        exec.setProgress(1.0);
        LOGGER.debug("Exiting GroupByTable() of class GroupByTable.");
    }

    private static SortedTable sortInputTable(final ExecutionContext exec,
            final BufferedDataTable table2sort, final List<String> groupCols,
            final boolean sortInMemory)
    throws CanceledExecutionException {
        LOGGER.debug("Entering sortInputTable(exec, table2sort, inclList, "
                + "sortInMemory) of class GroupByTable.");
        final boolean[] sortOrder = new boolean[groupCols.size()];
        for (int i = 0, length = sortOrder.length; i < length; i++) {
            sortOrder[i] = true;
        }
        final SortedTable sortedTabel =
            new SortedTable(table2sort, groupCols, sortOrder,
                sortInMemory, exec);
        LOGGER.debug("Exiting sortInputTable(exec, table2sort, inclList, "
                + "sortInMemory) of class GroupByTable.");
        return sortedTabel;

    }

    private BufferedDataTable createGroupByTable(final ExecutionContext exec,
            final SortedTable table)
    throws CanceledExecutionException {
        LOGGER.debug("Entering createGroupByTable(exec, table) "
                + "of class GroupByTable.");
        final DataTableSpec origSpec = table.getDataTableSpec();
        final DataTableSpec resultSpec = createGroupByTableSpec(origSpec,
                m_groupCols, m_colAggregators, m_keepColName);
        final BufferedDataContainer dc = exec.createDataContainer(resultSpec);
        //check for an empty table
        if (table.getRowCount() < 1) {
            dc.close();
            return dc.getTable();
        }

        final int[] groupColIdx = new int[m_groupCols.size()];
        int groupColIdxCounter = 0;
        //get the indices of the group by columns
        for (int i = 0, length = origSpec.getNumColumns(); i < length;
            i++) {
            final DataColumnSpec colSpec = origSpec.getColumnSpec(i);
            if (m_groupCols.contains(colSpec.getName())) {
                groupColIdx[groupColIdxCounter++] = i;
            }
        }

        final DataCell[] previousGroup = new DataCell[groupColIdx.length];
        final DataCell[] currentGroup = new DataCell[groupColIdx.length];
        Set<RowKey> rowKeys = new HashSet<RowKey>();
        int groupCounter = 0;
        boolean firstRow = true;
        final double progressPerRow = 1.0 / table.getRowCount();
        int rowCounter = 0;
        for (final DataRow row : table) {
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
                if (m_enableHilite) {
                    m_hiliteMapping.put(newRow.getKey(), rowKeys);
                    rowKeys = new HashSet<RowKey>();
                }
            }
            //compute the current row values
            for (final ColumnAggregator colAggr : m_colAggregators) {
                final DataCell cell = row.getCell(origSpec.findColumnIndex(
                        colAggr.getColName()));
                colAggr.getOperator(m_maxUniqueVals).compute(cell);
            }
            if (m_enableHilite) {
                rowKeys.add(row.getKey());
            }
            exec.checkCanceled();
            exec.setProgress(progressPerRow * rowCounter++);
        }
        //create the final row for the last group after processing the last
        //table row
        final DataRow newRow = createTableRow(previousGroup, groupCounter);
        dc.addRowToTable(newRow);
        //add the row keys to the hilite map if the flag is true
        if (m_enableHilite) {
            m_hiliteMapping.put(newRow.getKey(), rowKeys);
        }
        dc.close();
        return dc.getTable();
    }

    private DataRow createTableRow(final DataCell[] groupVals,
            final int groupCounter) {
        final RowKey rowKey = RowKey.createRowKey(groupCounter);
        final DataCell[] rowVals =
            new DataCell[groupVals.length + m_colAggregators.length];
        //add the group values first
        int valIdx = 0;
        for (final DataCell groupCell : groupVals) {
            rowVals[valIdx++] = groupCell;
        }
        //add the aggregation values
        for (final ColumnAggregator colAggr : m_colAggregators) {
            final AggregationOperator operator =
                colAggr.getOperator(m_maxUniqueVals);
            rowVals[valIdx++] = operator.getResult();
            if (operator.isSkipped()) {
                //add skipped groups and the column that causes the skipping
                //into the skipped groups map
                final String colName = colAggr.getColName();
                final String groupName = createGroupName(groupVals);
                Collection<String> groupNames =
                    m_skippedGroupsByColName.get(colName);
                if (groupNames == null) {
                    groupNames = new ArrayList<String>();
                    m_skippedGroupsByColName.put(colName, groupNames);
                }
                groupNames.add(groupName);
            }
            //reset the operator for the next group
            operator.reset();
        }
        final DataRow newRow = new DefaultRow(rowKey, rowVals);
        return newRow;
    }

    private static String createGroupName(final DataCell[] groupVals) {
        if (groupVals == null || groupVals.length == 0) {
            return "";
        }
        if (groupVals.length == 1) {
            return groupVals[0].toString();
        }
        final StringBuilder buf = new StringBuilder();
        buf.append("[");
        for (int i = 0, length = groupVals.length; i < length; i++) {
            if (i != 0) {
            buf.append(",");
            }
            buf.append(groupVals[i].toString());
        }
        buf.append("]");
        return buf.toString();
    }

    /**
     * @return the aggregated {@link BufferedDataTable}
     */
    public BufferedDataTable getBufferedTable() {
        return m_resultTable;
    }


    /**
     * the hilite translation <code>Map</code> or <code>null</code> if
     * the enableHilte flag in the constructor was set to <code>false</code>.
     * The key of the <code>Map</code> is the row key of the new group row and
     * the corresponding value is the <code>Collection</code> with all old row
     * keys which belong to this group.
     * @return the hilite translation <code>Map</code> or <code>null</code> if
     * the enableHilte flag in the constructor was set to <code>false</code>.
     */
    public Map<RowKey, Set<RowKey>> getHiliteMapping() {
        return m_hiliteMapping;
    }


    /**
     * Returns a <code>Map</code> with all skipped groups. The key of the
     * <code>Map</code> is the name of the column and the value is a
     * <code>Collection</code> with all skipped groups.
     * @return a <code>Map</code> with all skipped groups
     */
    public Map<String, Collection<String>> getSkippedGroupsByColName() {
        return m_skippedGroupsByColName;
    }

    /**
     * @param maxGroups the maximum number of skipped groups to display
     * @param maxCols the maximum number of columns to display per group
     * @return <code>String</code> message with the skipped groups per column
     * or <code>null</code> if no groups where skipped
     */
    public String getSkippedGroupsMessage(final int maxGroups,
            final int maxCols) {
        if (m_skippedGroupsByColName != null
                && m_skippedGroupsByColName.size() > 0) {
            final StringBuilder buf = new StringBuilder();
            buf.append("Skipped group(s) per column by group value(s): ");
            final Set<String> columnNames = m_skippedGroupsByColName.keySet();
            int columnCounter = 0;
            int groupCounter = 0;
            for (final String colName : columnNames) {
                if (columnCounter != 0) {
                    buf.append("; ");
                }
                if (columnCounter++ >= maxCols) {
                    buf.append("...");
                    break;
                }
                buf.append(colName);
                buf.append(": ");
                final Collection<String> groupNames =
                    m_skippedGroupsByColName.get(colName);
                groupCounter = 0;
                for (final String groupName : groupNames) {
                    if (groupCounter != 0) {
                        buf.append(", ");
                    }
                    if (groupCounter++ >= maxGroups) {
                        buf.append("...");
                        break;
                    }
                    buf.append(groupName);
                }
            }
            return buf.toString();
        }
        return null;
    }

    /**
     * @param spec the original {@link DataTableSpec}
     * @param groupColNames the name of all columns to group by
     * @param columnAggregators the aggregation columns with the
     * aggregation method to use in the order the columns should be appear
     * in the result table
     * @param keepColName <code>true</code> if the original column names should
     * be kept
     * @return the result {@link DataTableSpec}
     */
    public static final DataTableSpec createGroupByTableSpec(
            final DataTableSpec spec, final List<String> groupColNames,
            final ColumnAggregator[] columnAggregators,
            final boolean keepColName) {
        if (spec == null) {
            throw new NullPointerException("DataTableSpec must not be null");
        }
        if (groupColNames == null || groupColNames.isEmpty()) {
            throw new IllegalArgumentException("inclList must not be empty");
        }
        if (columnAggregators == null) {
            throw new NullPointerException("colMethods must not be null");
        }

        final int noOfCols = groupColNames.size() + columnAggregators.length;
        final DataColumnSpec[] colSpecs =
            new DataColumnSpec[noOfCols];
        int colIdx = 0;
        //add the group columns first

        final Map<String, MutableInteger> colNameCount =
            new HashMap<String, MutableInteger>(noOfCols);
        for (final String colName : groupColNames) {
            final DataColumnSpec colSpec = spec.getColumnSpec(colName);
            if (colSpec == null) {
                throw new IllegalArgumentException(
                        "No column spec found for name: " + colName);
            }
            colSpecs[colIdx++] = colSpec;
            colNameCount.put(colName, new MutableInteger(1));
        }
        //add the aggregation columns
        for (final ColumnAggregator aggrCol : columnAggregators) {
            final DataColumnSpec origSpec =
                spec.getColumnSpec(aggrCol.getColName());
            if (origSpec == null) {
                throw new IllegalArgumentException(
                        "No column spec found for name: "
                        + aggrCol.getColName());
            }
            final String colName;
            if (keepColName) {
                colName = origSpec.getName();
            } else {
                colName =
                    aggrCol.getMethod().createColumnName(origSpec.getName());
            }
            //since a column could be used several times create a unique name
            final String uniqueName;
            if (colNameCount.containsKey(colName)) {
                final MutableInteger counter = colNameCount.get(colName);
                uniqueName = colName + "_" + counter.intValue();
                counter.inc();
            } else {
                colNameCount.put(colName, new MutableInteger(1));
                uniqueName = colName;
            }
            final DataColumnSpec newSpec =
                aggrCol.getMethod().createColumnSpec(uniqueName, origSpec);
            colSpecs[colIdx++] = newSpec;
        }

        return new DataTableSpec(colSpecs);
    }

    /**
     * @param spec the <code>DataTableSpec</code> to check
     * @param groupCols the group by column name <code>List</code>
     * @throws IllegalArgumentException if one of the group by columns doesn't
     * exists in the given <code>DataTableSpec</code>
     */
    public static void checkGroupCols(final DataTableSpec spec,
            final List<String> groupCols) throws IllegalArgumentException {
        if (groupCols == null || groupCols.isEmpty()) {
            throw new IllegalArgumentException(
                    "No columns selected to group by");
        }
        // check if all group by columns exist in the DataTableSpec

        for (final String ic : groupCols) {
            if (ic == null) {
                throw new IllegalArgumentException(
                        "Column name must not be null");
            }
            if (!ic.equals(SorterNodeDialogPanel2.NOSORT.getName())) {
                if ((spec.findColumnIndex(ic) == -1)) {
                    throw new IllegalArgumentException("Column " + ic
                            + " not in spec.");
                }
            }
        }
    }
}
