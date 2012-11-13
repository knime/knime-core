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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.sort.SortedTable;
import org.knime.base.node.preproc.sorter.SorterNodeDialogPanel2;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.MutableInteger;
import org.knime.core.util.Pair;

/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
public abstract class GroupByTable {

    private static final AggregationMethod RETAIN_ORDER_COL_AGGR_METHOD =
        AggregationMethods.getRowOrderMethod();
    private static final String RETAIN_ORDER_COL_NAME = "orig_order_col";
    private final List<String> m_groupCols;
    private final GlobalSettings m_globalSettings;
    private final boolean m_enableHilite;
    private final ColumnNamePolicy m_colNamePolicy;
    private final Map<RowKey, Set<RowKey>> m_hiliteMapping;
    private final Map<String, Collection<Pair<String, String>>>
        m_skippedGroupsByColName =
        new HashMap<String, Collection<Pair<String, String>>>();
    private final boolean m_retainOrder;
    private final ColumnAggregator[] m_colAggregators;
    private final BufferedDataTable m_resultTable;

    /**Constructor for class GroupByTable.
     * @param exec the <code>ExecutionContext</code>
     * @param inDataTable the table to aggregate
     * @param groupByCols the name of all columns to group by
     * @param colAggregators the aggregation columns with the aggregation method
     * to use in the order the columns should be appear in the result table
     * numerical columns
     * @param globalSettings the global settings
     * @param enableHilite <code>true</code> if a row key map should be
     * maintained to enable hiliting
     * @param colNamePolicy the {@link ColumnNamePolicy} for the
     * aggregation columns
     * input table if set to <code>true</code>
     * @param retainOrder <code>true</code> if the original row order should be
     * retained
     * @throws CanceledExecutionException if the user has canceled the execution
     * @since 2.6
     */
    protected GroupByTable(final ExecutionContext exec,
            final BufferedDataTable inDataTable,
            final List<String> groupByCols,
            final ColumnAggregator[] colAggregators,
            final GlobalSettings globalSettings, final boolean enableHilite,
            final ColumnNamePolicy colNamePolicy, final boolean retainOrder)
    throws CanceledExecutionException {
        this(exec, inDataTable, groupByCols, colAggregators, globalSettings,
                false, enableHilite, colNamePolicy, retainOrder);
    }

    /**Constructor for class GroupByTable.
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
     * input table if set to <code>true</code>
     * @param retainOrder <code>true</code> if the original row order should be
     * retained
     * @throws CanceledExecutionException if the user has canceled the execution
     * @deprecated sortInMemory option is no longer required
     * @see #GroupByTable(ExecutionContext, BufferedDataTable, List,
     * ColumnAggregator[], GlobalSettings, boolean, ColumnNamePolicy, boolean)
     */
    @Deprecated
    protected GroupByTable(final ExecutionContext exec,
            final BufferedDataTable inDataTable,
            final List<String> groupByCols,
            final ColumnAggregator[] colAggregators,
            final GlobalSettings globalSettings, final boolean sortInMemory,
            final boolean enableHilite, final ColumnNamePolicy colNamePolicy,
            final boolean retainOrder)
    throws CanceledExecutionException {
        if (inDataTable == null) {
            throw new NullPointerException("DataTable must not be null");
        }
        checkGroupCols(inDataTable.getDataTableSpec(), groupByCols);
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
        m_colNamePolicy = colNamePolicy;
        //retain the row order only if the input table contains more than 1 row
        m_retainOrder = retainOrder && inDataTable.getRowCount() > 1;
        final Set<String> workingCols =
            getWorkingCols(globalSettings, groupByCols, colAggregators);
        final BufferedDataTable dataTable;
        final ColumnAggregator[] aggrs;
        final ExecutionContext subExec;
        if (m_retainOrder) {
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
                    retainOrderColSpec, RETAIN_ORDER_COL_AGGR_METHOD, true);
            subExec = exec.createSubExecutionContext(0.5);
        } else {
            subExec = exec;
            final ColumnRearranger columnRearranger =
                new ColumnRearranger(inDataTable.getDataTableSpec());
            final String[] workingColsArray =
                workingCols.toArray(new String[0]);
            columnRearranger.keepOnly(workingColsArray);
            columnRearranger.permute(workingColsArray);
            dataTable = exec.createColumnRearrangeTable(inDataTable,
                    columnRearranger, exec.createSubExecutionContext(0.01));
            aggrs = colAggregators;
        }
        final DataTableSpec dataTableSpec = dataTable.getDataTableSpec();
        //set the DataTableSpec of the filtered table that is processed instead
        //of the original table in the GlobalSettings object
        m_globalSettings = new GlobalSettings(dataTableSpec, globalSettings);
        m_colAggregators = aggrs;
        final DataTableSpec resultSpec = createGroupByTableSpec(dataTableSpec,
                m_groupCols, m_colAggregators, m_colNamePolicy);
        final int[] groupColIdx = new int[m_groupCols.size()];
        int groupColIdxCounter = 0;
        //get the indices of the group by columns
        for (int i = 0, length = dataTableSpec.getNumColumns(); i < length;
            i++) {
            final DataColumnSpec colSpec = dataTableSpec.getColumnSpec(i);
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
                final DataColumnSpec origColSpec =
                    m_colAggregators[m_colAggregators.length - 1]
                                     .getOriginalColSpec();
                final String orderColName = m_colNamePolicy.createColumName(
                        new ColumnAggregator(origColSpec,
                                RETAIN_ORDER_COL_AGGR_METHOD, true));
                //sort the table by the order column
                exec.setMessage("Rebuild original row order...");
                final BufferedDataTable tempTable =
                    sortTable(exec.createSubExecutionContext(0.4), groupTable,
                            Arrays.asList(orderColName));
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
     * @param globalSettings the {@link GlobalSettings}
     * @param groupByCols the group by column names
     * @param colAggregators the aggregation columns
     * @return {@link Set} with the name of all columns to work with
     */
    private Set<String> getWorkingCols(final GlobalSettings globalSettings,
       final List<String> groupByCols, final ColumnAggregator[] colAggregators) {
        final Set<String> colNames = new LinkedHashSet<String>(groupByCols);
        for (final ColumnAggregator aggr : colAggregators) {
            colNames.add(aggr.getOriginalColName());
            final Collection<String> addColNames =
                aggr.getOperator(globalSettings).getAdditionalColumnNames();
            if (addColNames != null && !addColNames.isEmpty()) {
                colNames.addAll(addColNames);
            }
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
     * @return the global settings
     */
    public GlobalSettings getGlobalSettings() {
        return m_globalSettings;
    }


    /**
     * @return if a hilite mapping should be maintained
     */
    public boolean isEnableHilite() {
        return m_enableHilite;
    }


    /**
     * @return if sorting should be performed in memory
     * @deprecated this option is no longer required and always returns
     * <code>false</code>
     */
    @Deprecated
    public boolean isSortInMemory() {
        return false;
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
        final String[] workingColsArray = workingCols.toArray(new String[0]);
        rearranger.keepOnly(workingColsArray);
        rearranger.permute(workingColsArray);
        return exec.createColumnRearrangeTable(dataTable, rearranger, exec);
    }

    /**
     * @param exec {@link ExecutionContext}
     * @param table2sort the {@link BufferedDataTable} to sort
     * @param sortCols the columns to sort by
     * @param sortInMemory the sort in memory flag
     * @return the sorted {@link BufferedDataTable}
     * @throws CanceledExecutionException if the operation has been canceled
     * @deprecated the sortInMemory option is no longer required
     * @see #sortTable(ExecutionContext, BufferedDataTable, List)
     */
    @Deprecated
    public static BufferedDataTable sortTable(final ExecutionContext exec,
            final BufferedDataTable table2sort, final List<String> sortCols,
            final boolean sortInMemory) throws CanceledExecutionException {
        return sortTable(exec, table2sort, sortCols);
    }

    /**
     * @param exec {@link ExecutionContext}
     * @param table2sort the {@link BufferedDataTable} to sort
     * @param sortCols the columns to sort by
     * @return the sorted {@link BufferedDataTable}
     * @throws CanceledExecutionException if the operation has been canceled
     * @since 2.7
     */
    public static BufferedDataTable sortTable(final ExecutionContext exec,
            final BufferedDataTable table2sort, final List<String> sortCols)
    throws CanceledExecutionException {
        if (sortCols.isEmpty()) {
            return table2sort;
        }
        final boolean[] sortOrder = new boolean[sortCols.size()];
        for (int i = 0, length = sortOrder.length; i < length; i++) {
            sortOrder[i] = true;
        }
        final SortedTable sortedTabel =
            new SortedTable(table2sort, sortCols, sortOrder, exec);
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
     * @param skipMsg the skip message to display
     * @param groupVals the skipped group values
     */
    protected void addSkippedGroup(final String colName,
            final String skipMsg, final DataCell[] groupVals) {
        final String groupName = createSkippedGroupName(groupVals);
        Collection<Pair<String, String>> groupNames =
            m_skippedGroupsByColName.get(colName);
        if (groupNames == null) {
            groupNames = new ArrayList<Pair<String, String>>();
            m_skippedGroupsByColName.put(colName, groupNames);
        }
        groupNames.add(new Pair<String, String>(groupName, skipMsg));
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
                spec.getColumnSpec(aggrCol.getOriginalColName());
            if (origSpec == null) {
                throw new IllegalArgumentException(
                        "No column spec found for name: "
                        + aggrCol.getOriginalColName());
            }
            final String colName =
                colNamePolicy.createColumName(aggrCol);
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
                aggrCol.getMethodTemplate().createColumnSpec(uniqueName,
                        origSpec);
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
     * <code>Collection</code> with {@link Pair} objects with the
     * group name as first and the corresponding skip message as second object.
     * @return a <code>Map</code> with all skipped groups
     */
    public Map<String, Collection<Pair<String, String>>>
        getSkippedGroupsByColName() {
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
            buf.append("Skipped group(s): ");
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
                final Collection<Pair<String, String>> groupNameMsgs =
                    m_skippedGroupsByColName.get(colName);
                final LinkedHashSet<String> causes =
                    new LinkedHashSet<String>();
                if (groupNameMsgs != null && !groupNameMsgs.isEmpty()) {
                    groupCounter = 0;
                    for (final Pair<String, String> groupNameMsg
                            : groupNameMsgs) {
                        final String name = groupNameMsg.getFirst();
                        final String cause = groupNameMsg.getSecond();
                        if (cause != null && !cause.isEmpty()) {
                            causes.add(cause);
                        }
                        if (name == null || name.isEmpty()) {
                            //skip empty group names
                            continue;
                        }
                        if (groupCounter == 0) {
                            buf.append(" groups: ");
                            buf.append("\"");
                        } else {
                            buf.append(", ");
                        }
                        if (groupCounter++ >= maxGroups) {
                            buf.append("...");
                            break;
                        }
                        buf.append(name);
                    }
                    if (groupCounter > 0) {
                        buf.append("\"");
                    }
                }
                if (!causes.isEmpty()) {
                    buf.append(" cause: ");
                    groupCounter = 0;
                    buf.append("\"");
                    for (final String cause : causes) {
                        if (groupCounter != 0) {
                            buf.append(", ");
                        }
                        if (groupCounter++ >= maxGroups) {
                            buf.append("...");
                            break;
                        }
                        buf.append(cause);
                    }
                    buf.append("\"");
                }
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
