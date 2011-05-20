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
import org.knime.core.node.ExecutionMonitor;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.data.aggregation.GlobalSettings;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class MemoryGroupByTable extends GroupByTable {

    private class GroupKey {
        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(m_groupVals);
            return result;
        }

        /**
         * {@inheritDoc}
         */
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
            final GroupKey other = (GroupKey)obj;
            if (!Arrays.equals(m_groupVals, other.m_groupVals)) {
                return false;
            }
            return true;
        }

        private final DataCell[] m_groupVals;

        public GroupKey(final DataCell[] groupVals) {
            m_groupVals = groupVals;
        }

        /**
         * @return the number of group values
         */
        public int size() {
            return m_groupVals.length;
        }

        /**
         * @return the group values array
         */
        public DataCell[] getGroupVals() {
            return m_groupVals;
        }

    }

    private Map<GroupKey, Set<RowKey>> m_rowKeys;
    private Map<GroupKey, ColumnAggregator[]> m_vals;

    /**Constructor for class MemoryGroupByTable.
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
     */
    protected MemoryGroupByTable(final ExecutionContext exec,
            final BufferedDataTable inDataTable, final List<String> groupByCols,
            final ColumnAggregator[] colAggregators,
            final GlobalSettings globalSettings, final boolean sortInMemory,
            final boolean enableHilite, final ColumnNamePolicy colNamePolicy,
            final boolean retainOrder)
            throws CanceledExecutionException {
        //retainOrder is always false since it is automatically maintained
        //in this class by the chosen Map implementation
        super(exec, inDataTable, groupByCols, colAggregators, globalSettings,
                sortInMemory, enableHilite, colNamePolicy, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable createGroupByTable(final ExecutionContext exec,
            final BufferedDataTable dataTable, final DataTableSpec resultSpec,
            final int[] groupColIdx) throws CanceledExecutionException {
        m_rowKeys = new HashMap<GroupKey, Set<RowKey>>();
        m_vals = new LinkedHashMap<GroupKey, ColumnAggregator[]>();
        final ExecutionMonitor groupExec = exec.createSubProgress(0.7);
        final DataTableSpec origSpec = dataTable.getDataTableSpec();
        final int rowCount = dataTable.getRowCount();
        int rowCounter = 0;
        for (final DataRow row : dataTable) {
            groupExec.checkCanceled();
            groupExec.setProgress(rowCounter++ / (double) rowCount,
                    "Analyzing row " + rowCounter + " of " + rowCount);
            final DataCell[] currentGroup = new DataCell[groupColIdx.length];
            //fetch the current group column values
            for (int i = 0, length = groupColIdx.length; i < length; i++) {
                currentGroup[i] = row.getCell(groupColIdx[i]);
            }
            final GroupKey groupKey = new GroupKey(currentGroup);
            addRowKey(groupKey, row.getKey());
            addRow(origSpec, groupKey, row);
        }
        return createResultTable(exec.createSubExecutionContext(0.3),
                resultSpec);
    }

    private BufferedDataTable createResultTable(final ExecutionContext exec,
            final DataTableSpec resultSpec) throws CanceledExecutionException {
        final BufferedDataContainer dc = exec.createDataContainer(resultSpec);
        int groupCounter = 0;
        final int size = m_vals.size();
        for (final Entry<GroupKey, ColumnAggregator[]> entry
                : m_vals.entrySet()) {
            exec.checkCanceled();
            exec.setProgress(groupCounter / (double)size,
                    "Writing group " + groupCounter + " of " + size);
            final GroupKey groupVals = entry.getKey();
            final ColumnAggregator[] colAggregators = entry.getValue();
            final RowKey rowKey = RowKey.createRowKey(groupCounter++);
            final DataCell[] rowVals =
                new DataCell[groupVals.size() + colAggregators.length];
            //add the group values first
            int valIdx = 0;
            for (final DataCell groupCell : groupVals.getGroupVals()) {
                rowVals[valIdx++] = groupCell;
            }
            //add the aggregation values
            for (final ColumnAggregator colAggr : colAggregators) {
                final AggregationOperator operator =
                    colAggr.getOperator(getGlobalSettings());
                rowVals[valIdx++] = operator.getResult();
                if (operator.isSkipped()) {
                    //add skipped groups and the column that causes the skipping
                    //into the skipped groups map
                    addSkippedGroup(colAggr.getOriginalColName(),
                            groupVals.getGroupVals());
                }
                //reset the operator for the next group
                operator.reset();
            }
            final DataRow newRow = new DefaultRow(rowKey, rowVals);
            dc.addRowToTable(newRow);
            //add hilite mappings if enabled
            if (isEnableHilite()) {
                final Set<RowKey> oldKeys = m_rowKeys.get(groupVals);
                addHiliteMapping(rowKey, oldKeys);
            }
        }
        dc.close();
        return dc.getTable();
    }

    private void addRow(final DataTableSpec origSpec,
            final GroupKey groupKey, final DataRow row) {
        ColumnAggregator[] aggregators = m_vals.get(groupKey);
        if (aggregators == null) {
            final ColumnAggregator[] origAggregators = getColAggregators();
            aggregators = new ColumnAggregator[origAggregators.length];
            for (int i = 0, length = origAggregators.length; i < length; i++) {
                aggregators[i] = origAggregators[i].clone();
            }
            m_vals.put(groupKey, aggregators);
        }
        for (final ColumnAggregator aggregator : aggregators) {
            final int colIdx =
                origSpec.findColumnIndex(aggregator.getOriginalColName());
            final DataCell cell = row.getCell(colIdx);
            aggregator.getOperator(getGlobalSettings()).compute(cell);
        }
    }

    private void addRowKey(final GroupKey groupKey, final RowKey key) {
        if (isEnableHilite()) {
            Set<RowKey> keySet = m_rowKeys.get(groupKey);
            if (keySet == null) {
                keySet = new HashSet<RowKey>();
                m_rowKeys.put(groupKey, keySet);
            }
            keySet.add(key);
        }
    }
}
