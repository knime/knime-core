/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *
 */
package org.knime.base.node.preproc.pivot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.data.sort.SortedTable;
import org.knime.base.node.preproc.groupby.GroupByNodeModel;
import org.knime.base.node.preproc.groupby.GroupByTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * The {@link NodeModel} implementation of the pivot node which uses
 * the {@link GroupByNodeModel} class implementations to create an intermediate
 * group-by table from which the pivoting table is extracted.
 *
 * @author Thomas Gabriel, KNIME.com AG, Switzerland
 */
public class Pivot2NodeModel extends GroupByNodeModel {

    /** Configuration key of the selected group by columns.*/
    protected static final String CFG_PIVOT_COLUMNS = "pivotColumns";

    private final SettingsModelFilterString m_pivotCols =
        new SettingsModelFilterString(CFG_PIVOT_COLUMNS);

    private final SettingsModelBoolean m_ignoreMissValues =
        Pivot2NodeDialog.createSettingsMissingValues();

    private final SettingsModelBoolean m_totalAggregation =
        Pivot2NodeDialog.createSettingsTotal();

    private final SettingsModelBoolean m_ignoreDomain =
        Pivot2NodeDialog.createSettingsIgnoreDomain();

    private static final String PIVOT_COLUMN_DELIMITER = "_";
    private static final String PIVOT_AGGREGATION_DELIMITER = "+";

    private final HiLiteHandler m_totalGroupsHilite = new HiLiteHandler();

    /** Create a new pivot node model. */
    public Pivot2NodeModel() {
        super(1, 3);
    }

    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        final List<String> pivotCols = m_pivotCols.getIncludeList();
        if (pivotCols.isEmpty()) {
            throw new InvalidSettingsException("No pivot columns selected.");
        }
        final List<String> groupCols = getGroupByColumns();
        for (final String piv : pivotCols) {
            if (groupCols.contains(piv)) {
                throw new InvalidSettingsException(
                        "Ambigious group/pivot column selection.");
            }
        }
        final List<String> groupAndPivotCols = createAllColumns();
        final DataTableSpec origSpec = (DataTableSpec) inSpecs[0];
        final DataTableSpec groupSpec = createGroupBySpec(origSpec,
                groupAndPivotCols);
        if (groupSpec.getNumColumns() == groupAndPivotCols.size()) {
            throw new InvalidSettingsException(
                    "No aggregation columns selected.");
        }

        final DataTableSpec groupRowsSpec =
            createGroupBySpec(origSpec, groupCols);
        if (m_ignoreMissValues.getBooleanValue()) {
            final Set<String>[] combPivots = createCombinedPivots(groupSpec,
                    pivotCols);
            for (final Set<String> combPivot : combPivots) {
                if (combPivot == null) {
                    return new DataTableSpec[] {null, groupRowsSpec, null};
                }
            }
            final DataTableSpec outSpec = createOutSpec(groupSpec, combPivots,
                /* ignored */ new HashMap<String, Integer>(), null);
            if (m_totalAggregation.getBooleanValue()) {
                @SuppressWarnings("unchecked")
                final
                DataTableSpec totalGroupSpec = createGroupBySpec(origSpec,
                        Collections.EMPTY_LIST);
                final DataColumnSpec[] pivotRowsSpec =
                    new DataColumnSpec[outSpec.getNumColumns()
                                       + totalGroupSpec.getNumColumns()];
                for (int i = 0; i < outSpec.getNumColumns(); i++) {
                    pivotRowsSpec[i] = outSpec.getColumnSpec(i);
                }
                final int totalOffset = outSpec.getNumColumns();
                for (int i = 0; i < totalGroupSpec.getNumColumns(); i++) {
                    pivotRowsSpec[i + totalOffset] =
                        totalGroupSpec.getColumnSpec(i);
                }
                return new DataTableSpec[] {outSpec, groupRowsSpec,
                        new DataTableSpec(pivotRowsSpec)};
            } else {
                return new DataTableSpec[] {outSpec, groupRowsSpec, outSpec};
            }
        } else {
            return new DataTableSpec[] {null, groupRowsSpec, null};
        }
    }

    private Set<String>[] createCombinedPivots(final DataTableSpec groupSpec,
            final List<String> pivotCols) {
        final int[] pivotIdx = new int[pivotCols.size()];
        @SuppressWarnings("unchecked")
        final
        Set<String>[] combPivots = new Set[pivotIdx.length];
        for (int i = 0; i < pivotIdx.length; i++) {
            pivotIdx[i] = groupSpec.findColumnIndex(pivotCols.get(i));
        }
        for (int i = 0; i < pivotIdx.length; i++) {
            final DataColumnSpec cspec = groupSpec.getColumnSpec(pivotIdx[i]);
            final DataColumnDomain domain = cspec.getDomain();
            if (!m_ignoreDomain.getBooleanValue() && domain.hasValues()) {
                combPivots[i] = new LinkedHashSet<String>();
                final Set<DataCell> values = domain.getValues();
                for (final DataCell pivotValue : values) {
                    combPivots[i].add(pivotValue.toString());
                }
                if (!m_ignoreMissValues.getBooleanValue()) {
                    combPivots[i].add("?");
                }
            }
        }
        return combPivots;
    }

    private List<String> createAllColumns() {
        final List<String> all = new ArrayList<String>(getGroupByColumns());
        final List<String> pivotCols = m_pivotCols.getIncludeList();
        all.removeAll(pivotCols);
        all.addAll(pivotCols);
        return all;
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        final BufferedDataTable table = (BufferedDataTable) inData[0];
        final List<String> groupAndPivotCols = createAllColumns();
        final BufferedDataTable groupTable;
        final String orderPivotColumnName;

        ExecutionContext groupAndPivotExec = 
            exec.createSubExecutionContext(0.5);
        ExecutionContext groupExec = exec.createSubExecutionContext(0.25);
        ExecutionContext pivotExec = exec.createSubExecutionContext(0.25);

        double progMainTotal = 0.0;
        double progMainTableAppendIndexForSort =
            isProcessInMemory() || isRetainOrder() ? 1.0 : 0.0;
        progMainTotal += progMainTableAppendIndexForSort;
        double progMainTableGroup = 5.0;
        progMainTotal += progMainTableGroup;
        double progMainTableInMemSort = isProcessInMemory() ? 3.0 : 0.0;
        progMainTotal += progMainTableInMemSort;
        double progMainTableGetPivots = 1.0;
        progMainTotal += progMainTableGetPivots;
        double progMainTableFillPivots = 1.0;
        progMainTotal += progMainTableFillPivots;
        double progMainTableRestoreSort =
            isProcessInMemory() || isRetainOrder() ? 1.0 : 0.0;
        progMainTotal += progMainTableRestoreSort;
        double progMainTableReplaceRowKey =
            isProcessInMemory() ? 1.0 : 0.0;
        progMainTotal += progMainTableReplaceRowKey;

        if (isProcessInMemory() || isRetainOrder()) {
            exec.setMessage("Keeping row order");
            final String retainOrderCol = DataTableSpec.getUniqueColumnName(
                    table.getDataTableSpec(), "#pivot_order#");
            // append temp. id column with minimum-aggregation method
            final ColumnAggregator[] colAggregators =
                getColumnAggregators().toArray(new ColumnAggregator[0]);
            final Set<String> workingCols = new LinkedHashSet<String>();
            workingCols.addAll(groupAndPivotCols);
            for (final ColumnAggregator ca : colAggregators) {
                workingCols.add(ca.getOriginalColName());
            }
            workingCols.add(retainOrderCol);
            final BufferedDataTable appTable = GroupByTable.appendOrderColumn(
                    groupAndPivotExec.createSubExecutionContext(
                            progMainTableAppendIndexForSort / progMainTotal),
                    table, workingCols, retainOrderCol);
            final DataColumnSpec retainOrderColSpec =
                    appTable.getSpec().getColumnSpec(retainOrderCol);
            final ColumnAggregator[] aggrs =
                    new ColumnAggregator[colAggregators.length + 1];
            System.arraycopy(colAggregators, 0, aggrs,
                    0, colAggregators.length);
            aggrs[colAggregators.length] = new ColumnAggregator(
                    retainOrderColSpec,
                    AggregationMethods.getRowOrderMethod(), true);
            orderPivotColumnName = getColumnNamePolicy().createColumName(
                    aggrs[colAggregators.length]);
            exec.setMessage("Grouping main table");
            final GroupByTable groupByTable = createGroupByTable(
                    groupAndPivotExec.createSubExecutionContext(
                            progMainTableGroup / progMainTotal),
                    appTable, groupAndPivotCols, isProcessInMemory(), 
                    false /* retain order always false; handled by pivoting */, 
                    Arrays.asList(aggrs));
            // table is not sorted by group&pivot columns; if process in memory
            // true then sort table by group&pivot columns
            if (isProcessInMemory()) { 
                exec.setMessage("Sorting group table");
                final SortedTable sortedGroupByTable = new SortedTable(
                        groupByTable.getBufferedTable(), groupAndPivotCols,
                        new boolean[groupAndPivotCols.size()],
                        groupAndPivotExec.createSubExecutionContext(
                                progMainTableInMemSort / progMainTotal));
                groupTable = sortedGroupByTable.getBufferedDataTable();
            } else {
                groupTable = groupByTable.getBufferedTable();
            }
        } else {
            exec.setMessage("Grouping main table");
            final GroupByTable groupByTable = createGroupByTable(
                    groupAndPivotExec.createSubExecutionContext(
                            progMainTableGroup / progMainTotal),
                    table, groupAndPivotCols,
                    isProcessInMemory(), false, getColumnAggregators());
            groupTable = groupByTable.getBufferedTable();
            orderPivotColumnName = null;
        }
        final List<String> pivotCols = m_pivotCols.getIncludeList();
        final int[] pivotIdx = new int[pivotCols.size()];
        final DataTableSpec groupSpec = groupTable.getSpec();
        final Set<String>[] combPivots =
            createCombinedPivots(groupSpec, pivotCols);
        for (int i = 0; i < pivotIdx.length; i++) {
            pivotIdx[i] = groupSpec.findColumnIndex(pivotCols.get(i));
        }
        exec.setProgress("Determining pivots...");
        ExecutionContext fillExec = groupAndPivotExec.createSubExecutionContext(
                progMainTableGetPivots / progMainTotal);
        final int groupTableSize = groupTable.getRowCount();
        int groupIndex = 0;
        for (final DataRow row : groupTable) {
            for (int i = 0; i < pivotIdx.length; i++) {
                if (combPivots[i] == null) {
                    combPivots[i] = new LinkedHashSet<String>();
                }
                final DataCell cell = row.getCell(pivotIdx[i]);
                if (cell.isMissing()) {
                    if (!m_ignoreMissValues.getBooleanValue()) {
                        combPivots[i].add(cell.toString());
                    }
                } else {
                    combPivots[i].add(cell.toString());
                }
            }
            fillExec.setProgress(
                    groupIndex++ / (double)groupTableSize,
                    String.format("Group \"%s\" (%d/%d)" ,
                            row.getKey(), groupIndex, groupTableSize));
            fillExec.checkCanceled();
        }

        final Map<String, Integer> pivotStarts =
            new LinkedHashMap<String, Integer>();
        final DataTableSpec outSpec = createOutSpec(groupSpec, combPivots,
                pivotStarts, orderPivotColumnName);
        exec.setProgress("Filling pivot table");
        BufferedDataTable pivotTable = fillPivotTable(
                groupTable, outSpec, pivotStarts,
                groupAndPivotExec.createSubExecutionContext(
                        progMainTableFillPivots / progMainTotal), 
                        orderPivotColumnName);

        if (orderPivotColumnName != null) {
            exec.setMessage("Restoring row order");
            final SortedTable sortedPivotTable = new SortedTable(pivotTable,
               Arrays.asList(new String[]{orderPivotColumnName}),
               new boolean[]{true}, groupAndPivotExec.createSubExecutionContext(
                       progMainTableRestoreSort / progMainTotal));
            pivotTable = sortedPivotTable.getBufferedDataTable();
            final ColumnRearranger colre =
                new ColumnRearranger(pivotTable.getSpec());
            colre.remove(orderPivotColumnName);
            pivotTable = exec.createColumnRearrangeTable(pivotTable, colre,
                    exec.createSilentSubProgress(0.0));
        }
        // temp fix for bug 3286
        if (isProcessInMemory()) {
            // if process in memory is true, RowKey's needs to be re-computed
            final BufferedDataContainer rowkeyBuf = 
                groupAndPivotExec.createSubExecutionContext(
                        progMainTableReplaceRowKey / progMainTotal).
                        createDataContainer(pivotTable.getSpec());
            int rowIndex = 0;
            for (DataRow row : pivotTable) {
                rowkeyBuf.addRowToTable(new DefaultRow(
                        RowKey.createRowKey(rowIndex++), row));
            }
            rowkeyBuf.close();
            pivotTable = rowkeyBuf.getTable();
        }
        groupAndPivotExec.setProgress(1.0);

        /* Fill the 3rd port */
        exec.setMessage("Determining pivot totals");
        double progPivotTotal = 0.0;
        double progPivotGroup = 5.0;
        progPivotTotal += progPivotGroup;
        double progPivotFillMissing = 1.0;
        progPivotTotal += progPivotFillMissing;
        double progPivotFillPivots = 1.0;
        progPivotTotal += progPivotFillPivots;
        double progPivotOverallTotals =
            m_totalAggregation.getBooleanValue() ? 5.0 : 0.0;
        progPivotTotal += progPivotOverallTotals;

        // create pivot table only on pivot columns (for grouping)
        // perform pivoting: result in single line
        final GroupByTable rowGroup = createGroupByTable(
                pivotExec.createSubExecutionContext(
                        progPivotGroup / progPivotTotal), table,
                m_pivotCols.getIncludeList(), isProcessInMemory(),
                isRetainOrder(), getColumnAggregators());
        final BufferedDataTable rowGroupTable = rowGroup.getBufferedTable();
        // fill group columns with missing cells
        final ColumnRearranger colre = new ColumnRearranger(
                rowGroupTable.getDataTableSpec());
        for (int i = 0; i < getGroupByColumns().size(); i++) {
            final DataColumnSpec cspec = outSpec.getColumnSpec(i);
            final CellFactory factory = new SingleCellFactory(cspec) {
                /** {@inheritDoc} */
                @Override
                public DataCell getCell(final DataRow row) {
                    return DataType.getMissingCell();
                }
            };
            colre.insertAt(i, factory);
        }
        final BufferedDataTable groupedRowTable =
            exec.createColumnRearrangeTable(rowGroupTable, colre,
                    pivotExec.createSubExecutionContext(
                            progPivotFillMissing / progPivotTotal));
        BufferedDataTable pivotRowsTable = fillPivotTable(groupedRowTable,
                outSpec, pivotStarts, pivotExec.createSubExecutionContext(
                        progPivotFillPivots / progPivotTotal), null);
        if (orderPivotColumnName != null) {
            final ColumnRearranger colre2 = new ColumnRearranger(
                    pivotRowsTable.getSpec());
            colre2.remove(orderPivotColumnName);
            pivotRowsTable = exec.createColumnRearrangeTable(pivotRowsTable,
                    colre2, exec.createSilentSubProgress(0.0));
        }

        // total aggregation without grouping
        if (m_totalAggregation.getBooleanValue()) {
            @SuppressWarnings("unchecked")
            final GroupByTable totalGroup = createGroupByTable(
                    pivotExec.createSubExecutionContext(
                            progPivotOverallTotals / progPivotTotal), table,
                    Collections.EMPTY_LIST, isProcessInMemory(),
                    isRetainOrder(), getColumnAggregators());
            final BufferedDataTable totalGroupTable =
                totalGroup.getBufferedTable();

            final DataTableSpec pivotsRowsSpec = pivotRowsTable.getSpec();
            final DataTableSpec totalGroupSpec = totalGroupTable.getSpec();
            final DataTableSpec overallTotalSpec =
                    new DataTableSpec(pivotsRowsSpec, totalGroupSpec);
            final BufferedDataContainer buf = exec.createDataContainer(
                    overallTotalSpec);
            if (pivotRowsTable.getRowCount() > 0) {
                final List<DataCell> pivotTotalsCells =
                    new ArrayList<DataCell>();
                final DataRow pivotsRow = pivotRowsTable.iterator().next();
                for (final DataCell cell : pivotsRow) {
                    pivotTotalsCells.add(cell);
                }
                final DataRow totalGroupRow = totalGroupTable.iterator().next();
                for (final DataCell cell : totalGroupRow) {
                    pivotTotalsCells.add(cell);
                }
                buf.addRowToTable(new DefaultRow(new RowKey("Totals"),
                        pivotTotalsCells));
            }
            buf.close();
            pivotRowsTable = buf.getTable();
        }
        pivotExec.setProgress(1.0);
        
        /* Fill the 2nd port: important to create this last since it will create
         * the final hilite handler (mapping) for port #1 AND #2 (bug 3270) */
        exec.setMessage("Creating group totals");
        // create group table only on group columns; no pivoting
        final BufferedDataTable columnGroupTable = createGroupByTable(
                groupExec, table,
                getGroupByColumns()).getBufferedTable();

        return new PortObject[] {
                // pivot table
                pivotTable,
                // group totals
                columnGroupTable,
                // pivot and overall totals
                pivotRowsTable};
    }

    private DataTableSpec createOutSpec(final DataTableSpec groupSpec,
            final Set<String>[] combPivots,
            final Map<String, Integer> pivotStarts,
            final String orderPivotColumnName) {
        final List<String> groupCols = getGroupByColumns();
        final List<String> groupAndPivotCols = createAllColumns();
        final List<String> pivots = new ArrayList<String>();
        createPivotColumns(combPivots, pivots, 0);
        final List<DataColumnSpec> cspecs = new ArrayList<DataColumnSpec>();
        for (final DataColumnSpec cspec : groupSpec) {
            if (groupCols.contains(cspec.getName())) {
                cspecs.add(cspec);
            }
        }

        // all pivots combined with agg. methods
        for (final String p : pivots) {
            pivotStarts.put(p, cspecs.size());
            for (final DataColumnSpec cspec : groupSpec) {
                if (orderPivotColumnName != null
                        && cspec.getName().equals(orderPivotColumnName)) {
                    continue;
                }
                final String name = cspec.getName();
                if (!groupAndPivotCols.contains(name)) {
                    final DataColumnSpec pivotCSpec = new DataColumnSpecCreator(
                            p + PIVOT_AGGREGATION_DELIMITER + name,
                            cspec.getType()).createSpec();
                    cspecs.add(pivotCSpec);
                }
            }
        }

        // append pivot order column
        if (orderPivotColumnName != null) {
            cspecs.add(groupSpec.getColumnSpec(orderPivotColumnName));
        }
        return new DataTableSpec(cspecs.toArray(new DataColumnSpec[0]));
    }

    private BufferedDataTable fillPivotTable(final BufferedDataTable groupTable,
            final DataTableSpec pivotSpec,
            final Map<String, Integer> pivotStarts,
            final ExecutionContext exec,
            final String orderPivotColumnName)
        throws CanceledExecutionException {
        final BufferedDataContainer buf = exec.createDataContainer(pivotSpec);
        final List<String> pivotCols = m_pivotCols.getIncludeList();
        final int pivotCount = pivotCols.size();
        final List<String> groupCols =
            new ArrayList<String>(getGroupByColumns());
        groupCols.removeAll(pivotCols);
        final int groupCount = groupCols.size();
        final DataTableSpec groupSpec = groupTable.getSpec();
        final int colCount = groupSpec.getNumColumns();
        final DataCell[] outcells = new DataCell[pivotSpec.getNumColumns()];
        final int totalRowCount = groupTable.getRowCount();
        int rowIndex = 0;
        for (final DataRow row : groupTable) {
            final RowKey origRowKey = row.getKey();
            String pivotColumn = null;
            for (int i = 0; i < colCount; i++) {
                final DataCell cell = row.getCell(i);
                // is a group column
                if (i < groupCount) {
                    // diff group found: write out current group and cont.
                    if (outcells[i] != null && !cell.equals(outcells[i])) {
                        // write row to out table
                        write(buf, outcells);
                        // reset pivot column name and out data row
                        pivotColumn = null;
                        for (int j = i + 1; j < outcells.length; j++) {
                            outcells[j] = null;
                        }
                    }
                    outcells[i] = cell;
                // is pivot column
                } else if (i < (groupCount + pivotCount)) {
                    // check for missing pivots
                    if (m_ignoreMissValues.getBooleanValue()
                            && cell.isMissing()) {
                        for (int j = 0; j < outcells.length; j++) {
                            outcells[j] = null;
                        }
                        break;
                    }
                    // create pivot column
                    if (pivotColumn == null) {
                        pivotColumn = cell.toString();
                    } else {
                        pivotColumn += PIVOT_COLUMN_DELIMITER + cell.toString();
                    }
                // is a aggregation column
                } else {
                    final int idx = pivotStarts.get(pivotColumn);
                    final int pivotIndex = i - pivotCount - groupCount;
                    final int pivotCellIndex = idx + pivotIndex;
                    if (orderPivotColumnName == null // if retain order is off
                            || !groupSpec.getColumnSpec(i).getName().equals(
                                    orderPivotColumnName)) {
                        outcells[pivotCellIndex] = cell;
                    } else { // temp retain column (type:IntCell)
                        final int retainIndex = outcells.length - 1;
                        if (outcells[retainIndex] == null) {
                            outcells[retainIndex] = cell;
                        } else {
                            final DataValueComparator comp =
                                pivotSpec.getColumnSpec(
                                    retainIndex).getType().getComparator();
                            if (comp.compare(outcells[retainIndex], cell) > 0) {
                                outcells[retainIndex] = cell;
                            }
                        }
                    }
                }
            }
            exec.setProgress(rowIndex++ / (double)totalRowCount,
                    String.format("Group \"%s\" (%d/%d)" ,
                            origRowKey, rowIndex, totalRowCount));
            exec.checkCanceled();
        }
        // write last group - if any.
        if (outcells[0] != null) {
            write(buf, outcells);
        }
        buf.close();
        return buf.getTable();
    }

    private void write(final BufferedDataContainer buf,
            final DataCell[] outcells) {
        for (int j = 0; j < outcells.length; j++) {
            if (outcells[j] == null) {
                outcells[j] = DataType.getMissingCell();
            }
        }
        final RowKey key = RowKey.createRowKey(buf.size());
        final DefaultRow outrow = new DefaultRow(key, outcells);
        buf.addRowToTable(outrow);
    }

    private void createPivotColumns(final Set<String>[] combs,
            final List<String> pivots, final int index) {
        if (index == combs.length || combs[index] == null) {
            return;
        }
        if (pivots.isEmpty()) {
            pivots.addAll(combs[index]);
        } else {
            final List<String> copy = new ArrayList<String>(pivots);
            pivots.clear();
            for (final String s : combs[index]) {
                for (final String p : copy) {
                    pivots.add(p + PIVOT_COLUMN_DELIMITER + s);
                }
            }
        }
        createPivotColumns(combs, pivots, index + 1);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_pivotCols.saveSettingsTo(settings);
        m_ignoreMissValues.saveSettingsTo(settings);
        m_totalAggregation.saveSettingsTo(settings);
        m_ignoreDomain.saveSettingsTo(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.validateSettings(settings);
        m_pivotCols.validateSettings(settings);
        m_ignoreMissValues.validateSettings(settings);
        m_totalAggregation.validateSettings(settings);
        m_ignoreDomain.validateSettings(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_pivotCols.loadSettingsFrom(settings);
        m_ignoreMissValues.loadSettingsFrom(settings);
        m_totalAggregation.loadSettingsFrom(settings);
        m_ignoreDomain.loadSettingsFrom(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        // bugfix 3055: wrong hilite handler passed to #2/#3 out-port
        if (outIndex == 0 || outIndex == 1) {
            return super.getOutHiLiteHandler(0);
        } else {
            return m_totalGroupsHilite;
        }
    }

}
