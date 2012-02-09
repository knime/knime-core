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
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;
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
        List<String> pivotCols = m_pivotCols.getIncludeList();
        if (pivotCols.isEmpty()) {
            throw new InvalidSettingsException("No pivot columns selected.");
        }
        List<String> groupCols = getGroupByColumns();
        for (String piv : pivotCols) {
            if (groupCols.contains(piv)) {
                throw new InvalidSettingsException(
                        "Ambigious group/pivot column selection.");
            }
        }
        List<String> groupAndPivotCols = createAllColumns();
        DataTableSpec origSpec = (DataTableSpec) inSpecs[0];
        DataTableSpec groupSpec = createGroupBySpec(origSpec,
                groupAndPivotCols);
        if (groupSpec.getNumColumns() == groupAndPivotCols.size()) {
            throw new InvalidSettingsException(
                    "No aggregation columns selected.");
        }

        DataTableSpec groupRowsSpec = createGroupBySpec(origSpec, groupCols);
        if (m_ignoreMissValues.getBooleanValue()) {
            Set<String>[] combPivots = createCombinedPivots(groupSpec,
                    pivotCols);
            for (int i = 0; i < combPivots.length; i++) {
                if (combPivots[i] == null) {
                    return new DataTableSpec[] {null, groupRowsSpec, null};
                }
            }
            DataTableSpec outSpec = createOutSpec(groupSpec, combPivots,
                /* ignored */ new HashMap<String, Integer>(), null);
            if (m_totalAggregation.getBooleanValue()) {
                @SuppressWarnings("unchecked")
                DataTableSpec totalGroupSpec = createGroupBySpec(origSpec,
                        Collections.EMPTY_LIST);
                DataColumnSpec[] pivotRowsSpec =
                    new DataColumnSpec[outSpec.getNumColumns()
                                       + totalGroupSpec.getNumColumns()];
                for (int i = 0; i < outSpec.getNumColumns(); i++) {
                    pivotRowsSpec[i] = outSpec.getColumnSpec(i);
                }
                int totalOffset = outSpec.getNumColumns();
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
        int[] pivotIdx = new int[pivotCols.size()];
        @SuppressWarnings("unchecked")
        Set<String>[] combPivots = new Set[pivotIdx.length];
        for (int i = 0; i < pivotIdx.length; i++) {
            pivotIdx[i] = groupSpec.findColumnIndex(pivotCols.get(i));
        }
        for (int i = 0; i < pivotIdx.length; i++) {
            DataColumnSpec cspec = groupSpec.getColumnSpec(pivotIdx[i]);
            DataColumnDomain domain = cspec.getDomain();
            if (!m_ignoreDomain.getBooleanValue() && domain.hasValues()) {
                combPivots[i] = new LinkedHashSet<String>();
                Set<DataCell> values = domain.getValues();
                for (DataCell pivotValue : values) {
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
        List<String> all = new ArrayList<String>(getGroupByColumns());
        List<String> pivotCols = m_pivotCols.getIncludeList();
        all.removeAll(pivotCols);
        all.addAll(pivotCols);
        return all;
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        final BufferedDataTable table = (BufferedDataTable) inData[0];
        List<String> groupAndPivotCols = createAllColumns();
        final BufferedDataTable groupTable;
        final Map<RowKey, Set<RowKey>> mapper;
        final String orderPivotColumnName;
        if (isProcessInMemory() || isRetainOrder()) {
            exec.setMessage("Memorize row order...");
            final String retainOrderCol = DataTableSpec.getUniqueColumnName(
                    table.getDataTableSpec(), "#pivot_order#");
            // append temp. id column with minimum-aggregation method
            ColumnAggregator[] colAggregators = getColumnAggregators().toArray(
                    new ColumnAggregator[0]);
            Set<String> workingCols = new LinkedHashSet<String>();
            workingCols.addAll(groupAndPivotCols);
            for (ColumnAggregator ca : colAggregators) {
                workingCols.add(ca.getOriginalColName());
            }
            workingCols.add(retainOrderCol);
            final BufferedDataTable appTable = GroupByTable.appendOrderColumn(
                    exec.createSubExecutionContext(0.1), table, workingCols,
                    retainOrderCol);
            final DataColumnSpec retainOrderColSpec =
                    appTable.getSpec().getColumnSpec(retainOrderCol);
            ColumnAggregator[] aggrs = 
                    new ColumnAggregator[colAggregators.length + 1];
            System.arraycopy(colAggregators, 0, aggrs,
                    0, colAggregators.length);
            aggrs[colAggregators.length] = new ColumnAggregator(
                    retainOrderColSpec, 
                    AggregationMethods.getRowOrderMethod(), true);
            orderPivotColumnName = getColumnNamePolicy().createColumName(
                    aggrs[colAggregators.length]);
            GroupByTable groupByTable = createGroupByTable(
                    exec.createSubExecutionContext(0.5), appTable, 
                    groupAndPivotCols, isProcessInMemory(), isSortInMemory(), 
                    false, Arrays.asList(aggrs));
            mapper = groupByTable.isEnableHilite() 
                    ? groupByTable.getHiliteMapping() : null;
            if (isProcessInMemory()) {
                // sort group table by group/pivot columns
                SortedTable sortedGroupByTable = new SortedTable(
                        groupByTable.getBufferedTable(), groupAndPivotCols, 
                        new boolean[groupAndPivotCols.size()],
                        exec.createSubExecutionContext(0.1));
                groupTable = sortedGroupByTable.getBufferedDataTable();
            } else {
                groupTable = groupByTable.getBufferedTable();
            }
        } else {
            GroupByTable groupByTable = createGroupByTable(
                 exec.createSubExecutionContext(0.6), table, groupAndPivotCols,
                 isProcessInMemory(), isSortInMemory(), false,
                 getColumnAggregators());
            mapper = groupByTable.isEnableHilite() 
                    ? groupByTable.getHiliteMapping() : null;
            groupTable = groupByTable.getBufferedTable();
            orderPivotColumnName = null;
        }
        
        List<String> pivotCols = m_pivotCols.getIncludeList();
        int[] pivotIdx = new int[pivotCols.size()];
        DataTableSpec groupSpec = groupTable.getSpec();
        Set<String>[] combPivots = createCombinedPivots(groupSpec, pivotCols);
        for (int i = 0; i < pivotIdx.length; i++) {
            pivotIdx[i] = groupSpec.findColumnIndex(pivotCols.get(i));
        }
        for (DataRow row : groupTable) {
            for (int i = 0; i < pivotIdx.length; i++) {
                if (combPivots[i] == null) {
                    combPivots[i] = new LinkedHashSet<String>();
                }
                DataCell cell = row.getCell(pivotIdx[i]);
                if (cell.isMissing()) {
                    if (!m_ignoreMissValues.getBooleanValue()) {
                        combPivots[i].add(cell.toString());
                    }
                } else {
                    combPivots[i].add(cell.toString());
                }
            }
        }

        Map<String, Integer> pivotStarts = new LinkedHashMap<String, Integer>();
        DataTableSpec outSpec = createOutSpec(groupSpec, combPivots,
                pivotStarts, orderPivotColumnName);
        BufferedDataTable pivotTable = fillPivotTable(
                groupTable, outSpec, pivotStarts,
                exec.createSubExecutionContext(0.2),
                mapper, orderPivotColumnName);
        
        if (orderPivotColumnName != null) {
            SortedTable sortedPivotTable = new SortedTable(pivotTable, 
               Arrays.asList(new String[]{orderPivotColumnName}), 
               new boolean[]{true}, exec.createSubExecutionContext(0.1));
            pivotTable = sortedPivotTable.getBufferedDataTable();
            ColumnRearranger colre = new ColumnRearranger(pivotTable.getSpec());
            colre.remove(orderPivotColumnName);
            pivotTable = exec.createColumnRearrangeTable(pivotTable, colre, 
                    exec.createSilentSubProgress(0.01));
        }

        // create pivot table only on pivot columns (for grouping)
        // perform pivoting: result in single line
        GroupByTable rowGroup = createGroupByTable(
                exec.createSubExecutionContext(0.2), table,
                m_pivotCols.getIncludeList(), isProcessInMemory(), 
                isSortInMemory(), isRetainOrder(), getColumnAggregators());
        BufferedDataTable rowGroupTable = rowGroup.getBufferedTable();
        // fill group columns with missing cells
        ColumnRearranger colre = new ColumnRearranger(
                rowGroupTable.getDataTableSpec());
        for (int i = 0; i < getGroupByColumns().size(); i++) {
            final DataColumnSpec cspec = outSpec.getColumnSpec(i);
            CellFactory factory = new SingleCellFactory(cspec) {
                /** {@inheritDoc} */
                @Override
                public DataCell getCell(final DataRow row) {
                    return DataType.getMissingCell();
                }
            };
            colre.insertAt(i, factory);
        }
        BufferedDataTable groupedRowTable = exec.createColumnRearrangeTable(
                rowGroupTable, colre, exec);
        BufferedDataTable pivotRowsTable = fillPivotTable(groupedRowTable,
                outSpec, pivotStarts, exec, mapper, null);
        if (orderPivotColumnName != null) {
            ColumnRearranger colre2 = new ColumnRearranger(
                    pivotRowsTable.getSpec());
            colre2.remove(orderPivotColumnName);
            pivotRowsTable = exec.createColumnRearrangeTable(pivotRowsTable, 
                    colre2, exec.createSilentSubProgress(0.01));
        }

        // total aggregation without grouping
        if (m_totalAggregation.getBooleanValue()) {
            @SuppressWarnings("unchecked")
            GroupByTable totalGroup = createGroupByTable(exec, table,
                    Collections.EMPTY_LIST, isProcessInMemory(), 
                    isSortInMemory(), isRetainOrder(), getColumnAggregators());
            BufferedDataTable totalGroupTable = totalGroup.getBufferedTable();

            DataTableSpec pivotsRowsSpec = pivotRowsTable.getSpec();
            DataTableSpec totalGroupSpec = totalGroupTable.getSpec();
            DataTableSpec overallTotalSpec =
                    new DataTableSpec(pivotsRowsSpec, totalGroupSpec);
            BufferedDataContainer buf = exec.createDataContainer(
                    overallTotalSpec);
            if (pivotRowsTable.getRowCount() > 0) {
                List<DataCell> pivotTotalsCells = new ArrayList<DataCell>();
                final DataRow pivotsRow = pivotRowsTable.iterator().next();
                for (DataCell cell : pivotsRow) {
                    pivotTotalsCells.add(cell);
                }
                final DataRow totalGroupRow = totalGroupTable.iterator().next();
                for (DataCell cell : totalGroupRow) {
                    pivotTotalsCells.add(cell);
                }
                    buf.addRowToTable(new DefaultRow(new RowKey("Totals"),
                        pivotTotalsCells));
            }
            buf.close();
            pivotRowsTable = buf.getTable();
        }

        // create group table only on group columns; no pivoting
        BufferedDataTable columnGroupTable = createGroupByTable(
                exec.createSubExecutionContext(0.2), table,
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
        List<String> groupCols = getGroupByColumns();
        List<String> groupAndPivotCols = createAllColumns();
        List<String> pivots = new ArrayList<String>();
        createPivotColumns(combPivots, pivots, 0);
        List<DataColumnSpec> cspecs = new ArrayList<DataColumnSpec>();
        for (DataColumnSpec cspec : groupSpec) {
            if (groupCols.contains(cspec.getName())) {
                cspecs.add(cspec);
            }
        }

        // all pivots combined with agg. methods
        for (String p : pivots) {
            pivotStarts.put(p, cspecs.size());
            for (DataColumnSpec cspec : groupSpec) {
                if (orderPivotColumnName != null 
                        && cspec.getName().equals(orderPivotColumnName)) {
                    continue;
                }
                final String name = cspec.getName();
                if (!groupAndPivotCols.contains(name)) {
                    DataColumnSpec pivotCSpec = new DataColumnSpecCreator(
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
            final Map<RowKey, Set<RowKey>> hiliteMapping,
            final String orderPivotColumnName) {
        BufferedDataContainer buf = exec.createDataContainer(pivotSpec);
        List<String> pivotCols = m_pivotCols.getIncludeList();
        int pivotCount = pivotCols.size();
        List<String> groupCols = new ArrayList<String>(getGroupByColumns());
        groupCols.removeAll(pivotCols);
        int groupCount = groupCols.size();
        DataTableSpec groupSpec = groupTable.getSpec();
        int colCount = groupSpec.getNumColumns();
        final DataCell[] outcells = new DataCell[pivotSpec.getNumColumns()];
        Map<RowKey, Set<RowKey>> map = new LinkedHashMap<RowKey, Set<RowKey>>();
        Set<RowKey> groupKeys = new LinkedHashSet<RowKey>();
        for (DataRow row : groupTable) {
            String pivotColumn = null;
            for (int i = 0; i < colCount; i++) {
                final DataCell cell = row.getCell(i);
                // is a group column
                if (i < groupCount) {
                    // diff group found: write out current group and cont.
                    if (outcells[i] != null && !cell.equals(outcells[i])) {
                        // write row to out table
                        RowKey key = write(buf, outcells);
                        map.put(key, groupKeys);
                        groupKeys = new LinkedHashSet<RowKey>();
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
                            DataValueComparator comp = pivotSpec.getColumnSpec(
                                    retainIndex).getType().getComparator();
                            if (comp.compare(outcells[retainIndex], cell) > 0) {
                                outcells[retainIndex] = cell;
                            }
                        }
                    }
                }
            }
            if (hiliteMapping != null) {
                groupKeys.addAll(hiliteMapping.get(row.getKey()));
            }
        }
        // write last group - if any.
        if (outcells[0] != null) {
            RowKey key = write(buf, outcells);
            map.put(key, groupKeys);
            groupKeys = new LinkedHashSet<RowKey>();
        }
        buf.close();
        if (hiliteMapping != null) {
            setHiliteMapping(new DefaultHiLiteMapper(map));
        }
        return buf.getTable();
    }

    private RowKey write(final BufferedDataContainer buf,
            final DataCell[] outcells) {
        for (int j = 0; j < outcells.length; j++) {
            if (outcells[j] == null) {
                outcells[j] = DataType.getMissingCell();
            }
        }
        RowKey key = RowKey.createRowKey(buf.size());
        DefaultRow outrow = new DefaultRow(key, outcells);
        buf.addRowToTable(outrow);
        return key;
    }

    private void createPivotColumns(final Set<String>[] combs,
            final List<String> pivots, final int index) {
        if (index == combs.length || combs[index] == null) {
            return;
        }
        if (pivots.isEmpty()) {
            pivots.addAll(combs[index]);
        } else {
            List<String> copy = new ArrayList<String>(pivots);
            pivots.clear();
            for (String s : combs[index]) {
                for (String p : copy) {
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
        if (outIndex == 0 || outIndex == 2) {
            return super.getOutHiLiteHandler(0);
        } else {
            return m_totalGroupsHilite;
        }
    }

}
