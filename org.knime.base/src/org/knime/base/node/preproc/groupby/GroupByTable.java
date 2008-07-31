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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.base.data.sort.SortedTable;
import org.knime.base.node.preproc.sorter.SorterNodeDialogPanel2;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;


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

    private static final String ROW_KEY_PREFIX = "Row";

    private final List<String> m_inclList;

    private final AggregationMethod m_numericColMethod;

    private final AggregationMethod m_noneNumericColMethod;

    private final int m_maxUniqueVals;

    private final boolean m_enableHilite;

    private final boolean m_moveGroupCols2Front;

    private final boolean m_keepColName;

    private final Map<RowKey, Set<RowKey>> m_hiliteMapping;

    private final BufferedDataTable m_resultTable;

    private final Map<String, Collection<String>> m_skippedGroupsByColName =
        new HashMap<String, Collection<String>>();

    /**Constructor for class GroupByTable.
     * @param dataTable the table to aggregate
     * @param groupByCols the name of all columns to group by
     * @param numericColMethod aggregation method for numerical columns
     * @param noneNumericColMethod aggregation method for none
     * numerical columns
     * @param maxUniqueValues the maximum number of unique values
     * @param sortInMemory <code>true</code> if the table should be sorted in
     * the memory
     * @param enableHilite <code>true</code> if a row key map should be
     * maintained to enable hiliting
     * @param moveGroupCols2Front <code>true</code> if the group by columns
     * should be moved to the front of the result table
     * @param keepColName <code>true</code> if the original column name should
     * be kept
     * @param exec the <code>ExecutionContext</code>
     * @throws CanceledExecutionException if the user has canceled the execution
     */
    public GroupByTable(final BufferedDataTable dataTable,
            final List<String> groupByCols,
            final AggregationMethod numericColMethod,
            final AggregationMethod noneNumericColMethod,
            final int maxUniqueValues, final boolean sortInMemory,
            final boolean enableHilite, final boolean moveGroupCols2Front,
            final boolean keepColName, final ExecutionContext exec)
    throws CanceledExecutionException {
        LOGGER.debug("Entering GroupByTable() of class GroupByTable.");
        if (dataTable == null) {
            throw new NullPointerException("DataTable must not be null");
        }
        checkIncludeList(dataTable.getDataTableSpec(), groupByCols);
        if (numericColMethod == null) {
            throw new NullPointerException("Numeric method must not be null");
        }
        if (noneNumericColMethod == null) {
            throw new NullPointerException(
                    "None numeric method must not be null");
        }
        if (noneNumericColMethod.isNumerical()) {
            throw new IllegalArgumentException("Invalid none numeric method");
        }
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

        m_inclList = groupByCols;
        m_numericColMethod = numericColMethod;
        m_noneNumericColMethod = noneNumericColMethod;
        m_maxUniqueVals = maxUniqueValues;
        m_moveGroupCols2Front = moveGroupCols2Front;
        m_keepColName = keepColName;
        exec.setMessage("Sorting input table...");
        ExecutionContext subExec = exec.createSubExecutionContext(0.5);
        final SortedTable sortedTable =
            sortInputTable(subExec, dataTable, m_inclList, sortInMemory);
        subExec.setProgress(1.0);
        exec.setMessage("Writing group table...");
        subExec = exec.createSubExecutionContext(0.5);
        m_resultTable = createGroupByTable(subExec, sortedTable);
        exec.setProgress(1.0);
        LOGGER.debug("Exiting GroupByTable() of class GroupByTable.");
    }

    private static SortedTable sortInputTable(final ExecutionContext exec,
            final BufferedDataTable table2sort, final List<String> inclList,
            final boolean sortInMemory)
    throws CanceledExecutionException {
        LOGGER.debug("Entering sortInputTable(exec, table2sort, inclList, "
                + "sortInMemory) of class GroupByTable.");
        final boolean[] sortOrder = new boolean[inclList.size()];
        for (int i = 0, length = sortOrder.length; i < length; i++) {
            sortOrder[i] = true;
        }
        final SortedTable sortedTabel =
            new SortedTable(table2sort, inclList, sortOrder,
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
        final DataTableSpec resultSpec = createGroupByTableSpec(
                origSpec, m_inclList, m_numericColMethod,
                m_noneNumericColMethod, m_moveGroupCols2Front, m_keepColName);
        assert (origSpec.getNumColumns() == resultSpec.getNumColumns())
            : "The number of columns must be the same";
        final BufferedDataContainer dc = exec.createDataContainer(resultSpec);
        //check for an empty table
        if (table.getRowCount() < 1) {
            dc.close();
            return dc.getTable();
        }
        final int[] groupColIdx = new int[m_inclList.size()];
        final int[] otherColIdx =
            new int[resultSpec.getNumColumns() - groupColIdx.length];
        final AggregationOperator[] ops =
            new AggregationOperator[otherColIdx.length];
        int groupIdxCounter = 0;
        int otherIdxCounter = 0;
        //get the indices of the group by columns
        for (int i = 0, length = origSpec.getNumColumns(); i < length;
            i++) {
            final DataColumnSpec colSpec = origSpec.getColumnSpec(i);
            if (m_inclList.contains(colSpec.getName())) {
                groupColIdx[groupIdxCounter++] = i;
            } else {
                otherColIdx[otherIdxCounter] = i;
                final AggregationMethod method =
                    getAggregationMethod(colSpec);
                    ops[otherIdxCounter] =
                        method.getOperator(m_maxUniqueVals);
                    otherIdxCounter++;
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
                final DataRow newRow = createTableRow(origSpec, groupColIdx,
                        otherColIdx, ops, previousGroup, groupCounter);
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
            for (int i = 0, length = otherColIdx.length; i < length; i++) {
                final DataCell cell = row.getCell(otherColIdx[i]);
                ops[i].compute(cell);
            }
            if (m_enableHilite) {
                rowKeys.add(row.getKey());
            }
            exec.checkCanceled();
            exec.setProgress(progressPerRow * rowCounter++);
        }
        //create the final row for the last group after processing the last
        //table row
        final DataRow newRow = createTableRow(origSpec, groupColIdx,
                otherColIdx, ops, previousGroup, groupCounter);
        dc.addRowToTable(newRow);
        //add the row keys to the hilite map if the flag is true
        if (m_enableHilite) {
            m_hiliteMapping.put(newRow.getKey(), rowKeys);
        }
        dc.close();
        LOGGER.debug("Exiting createGroupByTable(exec, table) "
                + "of class GroupByTable.");
        return dc.getTable();
    }

    private DataRow createTableRow(final DataTableSpec spec,
            final int[] groupColIdx, final int[] otherColIdx,
            final AggregationOperator[] ops, final DataCell[] groupVals,
            final int groupCounter) {
        LOGGER.debug("Entering createTableRow(spec, groupColIdx, otherColIdx, "
                + "ops, groupVals, groupCounter) of class GroupByTable.");
        //the current row belongs to the next group
        final String rowKey = ROW_KEY_PREFIX + groupCounter;
        DataCell[] rowVals =
            new DataCell[groupColIdx.length + otherColIdx.length];
        //add the group values first
        for (int i = 0, length = groupColIdx.length; i < length; i++) {
            rowVals[groupColIdx[i]] = groupVals[i];
        }
        //add the aggregation values
        for (int i = 0, length = otherColIdx.length; i < length; i++) {
            final AggregationOperator operator = ops[i];
            rowVals[otherColIdx[i]] = operator.getResult();
            //check for skipped
            if (operator.isSkipped()) {
                final String colName =
                    spec.getColumnSpec(otherColIdx[i]).getName();
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

        if (m_moveGroupCols2Front) {
            //change the order of the array elements
            final DataCell[] result = new DataCell[rowVals.length];
            int resultGroupIdx = 0;
            boolean groupVal;
            for (int i = 0, length = rowVals.length; i < length; i++) {
                groupVal = false;
                for (final int groupIdx : groupColIdx) {
                    if (i == groupIdx) {
                        result[resultGroupIdx++] = rowVals[i];
                        groupVal = true;
                        continue;
                    }
                }
                if (!groupVal) {
                    //count the number of group values with bigger
                    //index
                    int offset = 0;
                    for (final int groupIdx : groupColIdx) {
                        if (i < groupIdx) {
                            offset++;
                        }
                    }
                    result[i + offset] = rowVals[i];
                }
            }
            rowVals = result;
        }
        final DataRow newRow = new DefaultRow(rowKey, rowVals);
        LOGGER.debug("Exiting createTableRow(spec, groupColIdx, otherColIdx, "
                + "ops, groupVals, groupCounter) of class GroupByTable.");
        return newRow;
    }

    /**
     * @param colSpec the {@link DataColumnSpec} to check
     * @return the {@link AggregationMethod} to use
     */
    private AggregationMethod getAggregationMethod(
            final DataColumnSpec colSpec) {
        return getAggregationMethod(colSpec, m_numericColMethod,
                m_noneNumericColMethod);
    }

    private static String createGroupName(final DataCell[] groupVals) {
        LOGGER.debug("Entering createGroupName(groupVals) "
                + "of class GroupByTable.");
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
        LOGGER.debug("Exiting createGroupName(groupVals) "
                + "of class GroupByTable.");
        return buf.toString();
    }

    /**
     * @param colSpec the {@link DataColumnSpec} to check
     * @param numericColMethod the {@link AggregationMethod} for
     * numerical columns
     * @param nominalColMethod the {@link AggregationMethod} for none
     * numerical columns
     * @return the {@link AggregationMethod} to use
     */
    private static AggregationMethod getAggregationMethod(
            final DataColumnSpec colSpec,
            final AggregationMethod numericColMethod,
            final AggregationMethod nominalColMethod) {
        if (colSpec.getType().isCompatible(DoubleValue.class)) {
            return numericColMethod;
        }
        return nominalColMethod;
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
        LOGGER.debug("Entering getSkippedGroupsMessage(maxGroups, maxCols) "
                + "of class GroupByTable.");
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
            LOGGER.debug("Exiting getSkippedGroupsMessage(maxGroups, maxCols) "
                    + "of class GroupByTable.");
            return buf.toString();
        }
        LOGGER.debug("Exiting getSkippedGroupsMessage(maxGroups, maxCols) "
                + "of class GroupByTable.");
        return null;
    }

    /**
     * @param spec the original {@link DataTableSpec}
     * @param inclList the name of all columns to group by
     * @param numericalColMethod the numerical column aggregation method
     * @param nominalColMethod the none numerical column aggregation
     * method
     * @param moveGroupCols2Front <code>true</code> if the group by columns
     * should be moved to the front of the result table
     * @param keepColName <code>true</code> if the original colum names should
     * be kept
     * @return the result {@link DataTableSpec}
     */
    static final DataTableSpec createGroupByTableSpec(
            final DataTableSpec spec, final List<String> inclList,
            final AggregationMethod numericalColMethod,
            final AggregationMethod nominalColMethod,
            final boolean moveGroupCols2Front, final boolean keepColName) {
        LOGGER.debug("Entering createGroupByTableSpec() "
                + "of class GroupByTable.");
        if (spec == null) {
            throw new NullPointerException("DataTableSpec must not be null");
        }
        if (numericalColMethod == null) {
            throw new NullPointerException(
                    "Numerical aggregation method must not be null");
        }
        if (nominalColMethod == null) {
            throw new NullPointerException(
                    "None numerical aggregation method must not be null");
        }
        if (nominalColMethod.isNumerical()) {
            throw new IllegalArgumentException(
                    "Invalid none numerical aggregation method");
        }
        final DataColumnSpec[] colSpecs =
            new DataColumnSpec[spec.getNumColumns()];
        int otherIdx = 0;
        for (int i = 0, length = spec.getNumColumns(); i < length; i++) {
            final DataColumnSpec origSpec = spec.getColumnSpec(i);
            final int idx;
            final int groupIdx = inclList.indexOf(origSpec.getName());
            final DataColumnSpec newSpec;
            if (groupIdx >= 0) {
                if (moveGroupCols2Front) {
                    idx = groupIdx;
                } else {
                    idx = i;
                }
                newSpec = origSpec;
            } else {
                final AggregationMethod method = getAggregationMethod(origSpec,
                        numericalColMethod, nominalColMethod);
                newSpec = method.createColumnSpec(origSpec, keepColName);
                if (moveGroupCols2Front) {
                    idx = inclList.size() + otherIdx++;
                } else {
                    idx = i;
                }
            }
            colSpecs[idx] = newSpec;
        }
        LOGGER.debug("Exiting createGroupByTableSpec() of class GroupByTable.");
        return new DataTableSpec(colSpecs);
    }

    /**
     * @param spec the <code>DataTableSpec</code> to check
     * @param includeList the group by column name <code>List</code>
     * @throws IllegalArgumentException if one of the group by columns doesn't
     * exists in the given <code>DataTableSpec</code>
     */
    public static void checkIncludeList(final DataTableSpec spec,
            final List<String> includeList) throws IllegalArgumentException {
        LOGGER.debug("Entering checkIncludeList() of class GroupByTable.");
        if (includeList == null || includeList.isEmpty()) {
            throw new IllegalArgumentException(
                    "No columns selected to group by");
        }
        // check if all group by columns exist in the DataTableSpec

        for (final String ic : includeList) {
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
        LOGGER.debug("Exiting checkIncludeList() of class GroupByTable.");
    }
}
