/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.MutableInteger;

import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.data.sort.SortedTable;
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
 *
 * @author Tobias Koetter, University of Konstanz
 */
public abstract class GroupByTable {

    private static final AggregationMethod RETAIN_ORDER_COL_AGGR_METHOD =
        AggregationMethods.getRowOrderMethod();
    private static final String RETAIN_ORDER_COL_NAME = "orig_order_col";
    private final List<String> m_groupCols;
    private final int m_maxUniqueVals;
    private final boolean m_enableHilite;
    private final ColumnNamePolicy m_colNamePolicy;
    private final Map<RowKey, Set<RowKey>> m_hiliteMapping;
    private final Map<String, Collection<String>> m_skippedGroupsByColName =
        new HashMap<String, Collection<String>>();
    private final boolean m_retainOrder;
    private final ColumnAggregator[] m_colAggregators;
    private final BufferedDataTable m_resultTable;
    private final boolean m_sortInMemory;

    /**Constructor for class GroupByTable.
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
     * input table if set to <code>true</code>
     * @param retainOrder <code>true</code> if the original row order should be
     * retained
     * @throws CanceledExecutionException if the user has canceled the execution
     */
    protected GroupByTable(final ExecutionContext exec,
            final BufferedDataTable inDataTable,
            final List<String> groupByCols,
            final ColumnAggregator[] colAggregators, final int maxUniqueValues,
            final boolean sortInMemory, final boolean enableHilite,
            final ColumnNamePolicy colNamePolicy, final boolean retainOrder)
    throws CanceledExecutionException {
        if (inDataTable == null) {
            throw new NullPointerException("DataTable must not be null");
        }
        checkGroupCols(inDataTable.getDataTableSpec(), groupByCols);
        if (maxUniqueValues < 0) {
            throw new IllegalArgumentException(
                    "Maximum unique values must be a positive integer");
        }
        if (exec == null) {
            throw new NullPointerException("Exec must not be null");
        }
        m_sortInMemory = sortInMemory;
        m_enableHilite = enableHilite;
        if (m_enableHilite) {
            m_hiliteMapping = new HashMap<RowKey, Set<RowKey>>();
        } else {
            m_hiliteMapping = null;
        }
        m_groupCols = groupByCols;
        m_maxUniqueVals = maxUniqueValues;
        m_colNamePolicy = colNamePolicy;
        m_retainOrder = retainOrder;
        final Set<String> workingCols =
            getWorkingCols(groupByCols, colAggregators);
        final BufferedDataTable dataTable;
        final ColumnAggregator[] aggrs;
        final ExecutionContext subExec;
        if (m_retainOrder && inDataTable.getRowCount() > 1) {
            exec.setMessage("Memorize row order...");
            final String retainOrderCol = DataTableSpec.getUniqueColumnName(
                    inDataTable.getDataTableSpec(), RETAIN_ORDER_COL_NAME);
            //add the retain order column to the working columns as well
            workingCols.add(retainOrderCol);
            dataTable = appendOrderColumn(exec.createSubExecutionContext(0.1),
                    inDataTable, workingCols, retainOrderCol);
            final DataColumnSpec retainOrderColSpec =
                dataTable.getSpec().getColumnSpec(retainOrderCol);
            aggrs = new ColumnAggregator[colAggregators.length + 1];
            System.arraycopy(colAggregators, 0, aggrs,
                    0, colAggregators.length);
            aggrs[colAggregators.length] = new ColumnAggregator(
                    retainOrderColSpec, RETAIN_ORDER_COL_AGGR_METHOD);
            subExec = exec.createSubExecutionContext(0.5);
        } else {
            subExec = exec;
            final ColumnRearranger columnRearranger =
                new ColumnRearranger(inDataTable.getDataTableSpec());
            columnRearranger.keepOnly(workingCols.toArray(new String[0]));
            dataTable = exec.createColumnRearrangeTable(inDataTable,
                    columnRearranger, exec.createSubExecutionContext(0.01));
            aggrs = colAggregators;
        }
        final DataTableSpec inTableSpec = dataTable.getDataTableSpec();
        m_colAggregators = aggrs;
        final DataTableSpec resultSpec = createGroupByTableSpec(inTableSpec,
                m_groupCols, m_colAggregators, m_colNamePolicy);
        final int[] groupColIdx = new int[m_groupCols.size()];
        int groupColIdxCounter = 0;
        //get the indices of the group by columns
        for (int i = 0, length = inTableSpec.getNumColumns(); i < length;
            i++) {
            final DataColumnSpec colSpec = inTableSpec.getColumnSpec(i);
            if (m_groupCols.contains(colSpec.getName())) {
                groupColIdx[groupColIdxCounter++] = i;
            }
        }
        exec.setMessage("Creating group table...");
        if (dataTable.getRowCount() < 1) {
            //check for an empty table
            final BufferedDataContainer dc =
                exec.createDataContainer(resultSpec);
            dc.close();
            m_resultTable = dc.getTable();
        } else {
            final BufferedDataTable groupTable = createGroupByTable(subExec,
                    dataTable, resultSpec, groupColIdx);
            final BufferedDataTable resultTable;
            if (m_retainOrder) {
                final String origName =
                    m_colAggregators[m_colAggregators.length - 1].getColName();
                final String orderColName = m_colNamePolicy.createColumName(
                        origName, RETAIN_ORDER_COL_AGGR_METHOD);
                //sort the table by the order column
                exec.setMessage("Rebuild original row order...");
                final BufferedDataTable tempTable =
                    sortTable(exec.createSubExecutionContext(0.4), groupTable,
                            Arrays.asList(orderColName), sortInMemory);
                //remove the order column
                final ColumnRearranger rearranger =
                    new ColumnRearranger(tempTable.getSpec());
                rearranger.remove(orderColName);
                resultTable = exec.createColumnRearrangeTable(tempTable,
                        rearranger, exec);
            } else {
                resultTable = groupTable;
            }
            m_resultTable = resultTable;
        }
        exec.setProgress(1.0);
    }

    /**
     * @param groupByCols the group by column names
     * @param colAggregators the aggregation columns
     * @return {@link Set} with the name of all columns to work with
     */
    private Set<String> getWorkingCols(final List<String> groupByCols,
            final ColumnAggregator[] colAggregators) {
        final Set<String> colNames = new HashSet<String>(groupByCols);
        for (final ColumnAggregator aggr : colAggregators) {
            colNames.add(aggr.getColName());
        }
        return colNames;
    }

    /**
     * @param exec the {@link ExecutionContext}
     * @param dataTable the data table to aggregate
     * @param resultSpec the result {@link DataTableSpec}
     * @param groupColIdx the group column indices
     * @return the aggregated input table
     * @throws CanceledExecutionException if the operation has been canceled
     */
    protected abstract BufferedDataTable createGroupByTable(
            final ExecutionContext exec, final BufferedDataTable dataTable,
            final DataTableSpec resultSpec, final int[] groupColIdx)
    throws CanceledExecutionException;

    /**
     * @return the columns to group by
     */
    public List<String> getGroupCols() {
        return m_groupCols;
    }

    /**
     * @return the maximum number of unique values
     */
    public int getMaxUniqueVals() {
        return m_maxUniqueVals;
    }


    /**
     * @return if a hilite mapping should be maintained
     */
    public boolean isEnableHilite() {
        return m_enableHilite;
    }


    /**
     * @return if sorting should be performed in memory
     */
    public boolean isSortInMemory() {
        return m_sortInMemory;
    }


    /**
     * @return if the input table order should be retained
     */
    public boolean isRetainOrder() {
        return m_retainOrder;
    }

    /**
     * @return the colAggregators
     */
    public ColumnAggregator[] getColAggregators() {
        return m_colAggregators;
    }

    /**
     * @param exec the {@link ExecutionContext}
     * @param dataTable the {@link BufferedDataTable} to add the order column to
     * @param workingCols the names of all columns needed for grouping
     * @param retainOrderCol the name of the order column
     * @return the given table with the appended order column
     * @throws CanceledExecutionException if the operation has been canceled
     */
    public static BufferedDataTable appendOrderColumn(
            final ExecutionContext exec, final BufferedDataTable dataTable,
            final Set<String> workingCols, final String retainOrderCol)
    throws CanceledExecutionException {
        final ColumnRearranger rearranger =
            new ColumnRearranger(dataTable.getSpec());
        rearranger.append(new SingleCellFactory(new DataColumnSpecCreator(
                retainOrderCol, IntCell.TYPE).createSpec()) {
            private int m_id = 0;
            @Override
            public DataCell getCell(final DataRow row) {
                return new IntCell(m_id++);
            }
        });
        rearranger.keepOnly(workingCols.toArray(new String[0]));
        return exec.createColumnRearrangeTable(dataTable, rearranger, exec);
    }

    /**
     * @param exec {@link ExecutionContext}
     * @param table2sort the {@link BufferedDataTable} to sort
     * @param sortCols the columns to sort by
     * @param sortInMemory the sort in memory flag
     * @return the sorted {@link BufferedDataTable}
     * @throws CanceledExecutionException if the operation has been canceled
     */
    public static BufferedDataTable sortTable(final ExecutionContext exec,
            final BufferedDataTable table2sort, final List<String> sortCols,
            final boolean sortInMemory) throws CanceledExecutionException {
        if (sortCols.isEmpty()) {
            return table2sort;
        }
        final boolean[] sortOrder = new boolean[sortCols.size()];
        for (int i = 0, length = sortOrder.length; i < length; i++) {
            sortOrder[i] = true;
        }
        final SortedTable sortedTabel =
            new SortedTable(table2sort, sortCols, sortOrder,
                sortInMemory, exec);
        return sortedTabel.getBufferedDataTable();

    }

    /**
     * @param groupVals the group values of the skipped group
     * @return the group name
     */
    public static String createSkippedGroupName(final DataCell[] groupVals) {
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
     * @param newKey the new {@link RowKey}
     * @param oldKeys all old {@link RowKey}s
     */
    protected void addHiliteMapping(final RowKey newKey,
            final Set<RowKey> oldKeys) {
        m_hiliteMapping.put(newKey, oldKeys);
    }

    /**
     * @param colName the name of the column
     * @param groupVals the skipped group values
     */
    protected void addSkippedGroup(final String colName,
            final DataCell[] groupVals) {
        final String groupName = createSkippedGroupName(groupVals);
        Collection<String> groupNames =
            m_skippedGroupsByColName.get(colName);
        if (groupNames == null) {
            groupNames = new ArrayList<String>();
            m_skippedGroupsByColName.put(colName, groupNames);
        }
        groupNames.add(groupName);
    }

    /**
     * @param spec the original {@link DataTableSpec}
     * @param groupColNames the name of all columns to group by
     * @param columnAggregators the aggregation columns with the
     * aggregation method to use in the order the columns should be appear
     * in the result table
     * @param colNamePolicy the {@link ColumnNamePolicy} for the aggregation
     * columns
     * @return the result {@link DataTableSpec}
     */
    public static final DataTableSpec createGroupByTableSpec(
            final DataTableSpec spec, final List<String> groupColNames,
            final ColumnAggregator[] columnAggregators,
            final ColumnNamePolicy colNamePolicy) {
        if (spec == null) {
            throw new NullPointerException("DataTableSpec must not be null");
        }
        if (groupColNames == null) {
            throw new IllegalArgumentException(
                    "groupColNames must not be null");
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
            final String colName =
                colNamePolicy.createColumName(origSpec.getName(),
                        aggrCol.getMethod());
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
                buf.append("=");
                final Collection<String> groupNames =
                    m_skippedGroupsByColName.get(colName);
                groupCounter = 0;
                buf.append("\"");
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
                buf.append("\"");
            }
            return buf.toString();
        }
        return null;
    }

    /**
     * @param spec the <code>DataTableSpec</code> to check
     * @param groupCols the group by column name <code>List</code>
     * @throws IllegalArgumentException if one of the group by columns doesn't
     * exists in the given <code>DataTableSpec</code>
     */
    public static void checkGroupCols(final DataTableSpec spec,
            final List<String> groupCols)
            throws IllegalArgumentException {
        if (groupCols == null) {
            throw new IllegalArgumentException(
                    "Group columns must not be null");
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

    /**
     * @return the aggregated {@link BufferedDataTable}
     */
    public BufferedDataTable getBufferedTable() {
        return m_resultTable;
    }
}
