/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   14.04.2014 (koetter): created
 */
package org.knime.timeseries.node.movagg;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.node.preproc.groupby.ColumnNamePolicy;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;

/**
 * {@link CellFactory} implementation of the Moving Aggregation node. The factory takes care of caching the rows
 * within the window and the calculation of the selected aggregation values.
 *
 *  @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 *  @since 2.10
 */
public class MovingAggregationTableFactory {

    private final int m_windowLength;
    private final int[] m_aggrColIdxs;
    private final int[] m_cols2KeepIdxs;
    private AggregationOperator[] m_ops;
    private final DataColumnSpec[] m_specs;
    private final Set<String> m_aggregationCols;
    private Set<String> m_retainedCols;
    private boolean m_handleMissings;
    private final boolean m_cumulativeComp;
    private final boolean m_removeAggregationCols;
    private final boolean m_removeRetainedCols;
    private final DataTableSpec m_inputSpec;
    private final WindowType m_type;

    /**
     * @param spec the {@link DataTableSpec} of the table to process
     * @param globalSettings the {@link GlobalSettings} to use for aggregation
     * @param colNamePolicy the {@link ColumnNamePolicy}
     * @param aggregators list with {@link ColumnAggregator}s to use
     * @param cumulativeComp use cumulative computation instead of window length
     * @param windowLength the length of the aggregation window
     * @param type the {@link WindowType}
     * @param handleMissings if true, a smaller window size is used in the beginning to handle missing values.
     * @param removeRetainedCols <code>true</code> if the retained columns should be removed
     * @param removeAggregationCols <code>true</code> if the aggregation columns should be removed
     * @throws IllegalArgumentException if the selected {@link ColumnAggregator}s and the {@link ColumnNamePolicy}
     * results in duplicate column names
     */
    MovingAggregationTableFactory(final DataTableSpec spec, final GlobalSettings globalSettings,
        final ColumnNamePolicy colNamePolicy, final List<ColumnAggregator> aggregators, final boolean cumulativeComp,
        final WindowType type, final int windowLength, final boolean handleMissings,
        final boolean removeAggregationCols, final boolean removeRetainedCols) {
        m_inputSpec = spec;
        m_cumulativeComp = cumulativeComp;
        m_type = type;
        m_removeAggregationCols = removeAggregationCols;
        m_removeRetainedCols = removeRetainedCols;
        m_windowLength = windowLength;
        m_handleMissings = handleMissings;
        m_aggrColIdxs = new int[aggregators.size()];
        m_ops = new AggregationOperator[aggregators.size()];
        m_specs = new DataColumnSpec[aggregators.size()];
        m_aggregationCols = new HashSet<>(aggregators.size());
        m_retainedCols = new HashSet<>(Arrays.asList(spec.getColumnNames()));
        Set<String> uniqueNames = new HashSet<>(aggregators.size());
        int colIdx = 0;
        for (final ColumnAggregator colAggr : aggregators) {
            final String colName = colAggr.getOriginalColName();
            m_aggrColIdxs[colIdx] = spec.findColumnIndex(colName);
            m_ops[colIdx] = colAggr.getOperator(globalSettings);
            final String aggrColName = colNamePolicy.createColumName(colAggr);
            if (!uniqueNames.add(aggrColName)) {
                throw new IllegalArgumentException("Duplicate column name " + aggrColName);
            }
            m_specs[colIdx] = colAggr.createColumnSpec(aggrColName, spec.getColumnSpec(m_aggrColIdxs[colIdx]));
            m_aggregationCols.add(colName);
            m_retainedCols.remove(colName);
            colIdx++;
        }
        Collection<String> colNames2Keep = new LinkedList<>();
        if (!m_removeRetainedCols) {
            colNames2Keep.addAll(m_retainedCols);
        }
        if (!m_removeAggregationCols) {
            colNames2Keep.addAll(m_aggregationCols);
        }
        m_cols2KeepIdxs = new int[colNames2Keep.size()];
        colIdx = 0;
        for (final String colName : colNames2Keep) {
            m_cols2KeepIdxs[colIdx++] = spec.findColumnIndex(colName);
        }
        //sort the columns to keep ascending
        Arrays.sort(m_cols2KeepIdxs);
    }



    /**
     * @param exec {@link ExecutionContext} to provide progress
     * @param table the {@link BufferedDataTable} to process
     * @return the result table
     * @throws CanceledExecutionException if the user has canceled the operation
     */
    public BufferedDataTable createTable(final ExecutionContext exec, final BufferedDataTable table)
            throws CanceledExecutionException {
        final DataTableSpec resultSpec = createResultSpec();
        final BufferedDataContainer dc = exec.createDataContainer(resultSpec);
        final int rowCount = table.getRowCount();
        if (rowCount == 0) {
            dc.close();
            return dc.getTable();
        }
        if (m_cumulativeComp) {
            return getCumulativeTable(exec, table, dc);
        }
        switch (m_type) {
            case BACKWARD:
                return getBackwardTable(exec, table, dc);
            case CENTER:
                return getCenterTable(exec, table, dc);
            case FORWARD:
                return getForwardTable(exec, table, dc);
        }
        throw new RuntimeException("Unknown window type " + m_type);
    }

    private BufferedDataTable getCumulativeTable(final ExecutionContext exec, final BufferedDataTable table,
        final BufferedDataContainer dc) throws CanceledExecutionException {
        final int rowCount = table.getRowCount();
        int rowIdx = 0;
        for (final DataRow row : table) {
            exec.setProgress(rowIdx / (double) rowCount, "Processing row " + rowIdx + " of " + rowCount);
            exec.checkCanceled();
            final DataCell[] cells = new DataCell[m_ops.length + m_cols2KeepIdxs.length];
            int idx = 0;
            //handle the retained columns
            for (final int colIdx : m_cols2KeepIdxs) {
                cells[idx++] = row.getCell(colIdx);
            }
            for (int i = 0, length = m_ops.length; i < length; i++) {
                final int colIdx = m_aggrColIdxs[i];
                final AggregationOperator op = m_ops[i];
                op.compute(row, colIdx);
                cells[idx++] = op.getResult();
            }
            dc.addRowToTable(new DefaultRow(row.getKey(), cells));
        }
        dc.close();
        return dc.getTable();
    }



    private BufferedDataTable getForwardTable(final ExecutionMonitor exec, final BufferedDataTable table,
        final BufferedDataContainer dc) throws CanceledExecutionException {
        final int rowCount = table.getRowCount();
        final LinkedList<DataRow> window = new LinkedList<>();
        int rowIdx = 0;
        for (final DataRow row : table) {
            exec.setProgress(rowIdx / (double) rowCount, "Processing row " + rowIdx++ + " of " + rowCount);
            exec.checkCanceled();
            window.add(row);
            final boolean windowFull = window.size() >= m_windowLength;
            if (!windowFull) {
                //we have to fill the window first
                continue;
            }
            final DataCell[] cells = new DataCell[m_ops.length + m_cols2KeepIdxs.length];
            final DataRow firstRow = window.getFirst();
            int idx = 0;
            //handle the retained columns
            for (final int colIdx : m_cols2KeepIdxs) {
                cells[idx++] = firstRow.getCell(colIdx);
            }
            for (int i = 0, length = m_ops.length; i < length; i++) {
                final int colIdx = m_aggrColIdxs[i];
                final AggregationOperator op = m_ops[i];
                for (final DataRow windowRow : window) {
                    op.compute(windowRow, colIdx);
                }
                cells[idx++] = op.getResult();
                op.reset();
            }
            dc.addRowToTable(new DefaultRow(firstRow.getKey(), cells));
            //remove the first row only when the window is full
            //not during the missing value handling phase!
            window.removeFirst();
        }
        //we have to handle the remaining rows in the window
        while (!window.isEmpty()) {
            exec.checkCanceled();
            final DataCell[] cells = new DataCell[m_ops.length + m_cols2KeepIdxs.length];
            int idx = 0;
            final DataRow firstRow = window.getFirst();
            //handle the retained columns
            for (final int colIdx : m_cols2KeepIdxs) {
                cells[idx++] = firstRow.getCell(colIdx);
            }
            for (int i = 0, length = m_ops.length; i < length; i++) {
                if (m_handleMissings) {
                    final int colIdx = m_aggrColIdxs[i];
                    final AggregationOperator op = m_ops[i];
                    for (final DataRow windowRow : window) {
                        op.compute(windowRow, colIdx);
                    }
                    cells[idx++] = op.getResult();
                    op.reset();
                } else {
                    //the window is not yet full return missing cells
                    cells[idx++] = DataType.getMissingCell();
                }
            }
            window.removeFirst();
            dc.addRowToTable(new DefaultRow(firstRow.getKey(), cells));
        }
        dc.close();
        return dc.getTable();
    }
    private BufferedDataTable getCenterTable(final ExecutionMonitor exec, final BufferedDataTable table,
        final BufferedDataContainer dc) throws CanceledExecutionException {
        final int rowCount = table.getRowCount();
        final LinkedList<DataRow> window = new LinkedList<>();
        int rowIdx = 0;
        int centerIdx = -1;
        for (final DataRow row : table) {
            exec.setProgress(rowIdx / (double) rowCount, "Processing row " + rowIdx++ + " of " + rowCount);
            exec.checkCanceled();
            window.add(row);
            //we have to subtract 1 since the indexing of the array starts with 0
            centerIdx = window.size() - m_windowLength / 2 - 1;
            if (centerIdx < 0) {
                //we have to fill the window first
                continue;
            }
            final DataCell[] cells = new DataCell[m_ops.length + m_cols2KeepIdxs.length];
            final DataRow centerRow = window.get(centerIdx);
            int idx = 0;
            //handle the retained columns
            for (final int colIdx : m_cols2KeepIdxs) {
                cells[idx++] = centerRow.getCell(colIdx);
            }
            final boolean windowFull = window.size() >= m_windowLength;
            for (int i = 0, length = m_ops.length; i < length; i++) {
                if (windowFull || m_handleMissings) {
                    final int colIdx = m_aggrColIdxs[i];
                    final AggregationOperator op = m_ops[i];
                    for (final DataRow windowRow : window) {
                        op.compute(windowRow, colIdx);
                    }
                    cells[idx++] = op.getResult();
                    op.reset();
                } else {
                    //the window is not yet full return missing cells
                    cells[idx++] = DataType.getMissingCell();
                }
            }
            dc.addRowToTable(new DefaultRow(centerRow.getKey(), cells));
            //remove the first row only when the window is full
            //not during the missing value handling phase!
            if (windowFull) {
                window.removeFirst();
            }
        }
        //we have to handle the remaining rows in the window
        while (window.size() > centerIdx) {
            exec.checkCanceled();
            final DataCell[] cells = new DataCell[m_ops.length + m_cols2KeepIdxs.length];
            int idx = 0;
            final DataRow centerRow = window.get(centerIdx);
            //handle the retained columns
            for (final int colIdx : m_cols2KeepIdxs) {
                cells[idx++] = centerRow.getCell(colIdx);
            }
            for (int i = 0, length = m_ops.length; i < length; i++) {
                if (m_handleMissings) {
                    final int colIdx = m_aggrColIdxs[i];
                    final AggregationOperator op = m_ops[i];
                    for (final DataRow windowRow : window) {
                        op.compute(windowRow, colIdx);
                    }
                    cells[idx++] = op.getResult();
                    op.reset();
                } else {
                    //the window is not yet full return missing cells
                    cells[idx++] = DataType.getMissingCell();
                }
            }
            window.removeFirst();
            dc.addRowToTable(new DefaultRow(centerRow.getKey(), cells));
        }
        dc.close();
        return dc.getTable();
    }



    private BufferedDataTable getBackwardTable(final ExecutionMonitor exec, final BufferedDataTable table,
        final BufferedDataContainer dc) throws CanceledExecutionException {
        final int rowCount = table.getRowCount();
        final LinkedList<DataRow> window = new LinkedList<>();
        int rowIdx = 0;
        for (final DataRow row : table) {
            exec.setProgress(rowIdx / (double) rowCount, "Processing row " + rowIdx++ + " of " + rowCount);
            exec.checkCanceled();
            window.add(row);
            final DataCell[] cells = new DataCell[m_ops.length + m_cols2KeepIdxs.length];
            int idx = 0;
            //handle the retained columns
            for (final int colIdx : m_cols2KeepIdxs) {
                cells[idx++] = row.getCell(colIdx);
            }
            final boolean windowFull = window.size() >= m_windowLength;
            for (int i = 0, length = m_ops.length; i < length; i++) {
                if (windowFull || m_handleMissings) {
                    final int colIdx = m_aggrColIdxs[i];
                    final AggregationOperator op = m_ops[i];

                    for (final DataRow windowRow : window) {
                        op.compute(windowRow, colIdx);
                    }
                    cells[idx++] = op.getResult();
                    op.reset();
                } else {
                    //the window is not yet full return missing cells
                    cells[idx++] = DataType.getMissingCell();
                }
            }
            if (windowFull) {
                //remove the first row only when the window is full
                //not during the missing value handling phase!
                window.removeFirst();
            }
            dc.addRowToTable(new DefaultRow(row.getKey(), cells));
        }
        dc.close();
        return dc.getTable();
    }



    /**
     * @return the name of all columns from the input table that are aggregated
     */
    public Collection<String> getAggregationColNames() {
        return m_aggregationCols;
    }

    /**
     * @return the name of all columns from the input table that are retained
     */
    private Collection<String> getRetainedColNames() {
        return m_retainedCols;
    }

    /**
     * @return the {@link DataTableSpec} of the result table
     */
    public DataTableSpec createResultSpec() {
        final ColumnRearranger colRearranger = new ColumnRearranger(m_inputSpec);
        if (m_removeAggregationCols) {
            colRearranger.remove(getAggregationColNames().toArray(new String[0]));
        }
        if (m_removeRetainedCols) {
            colRearranger.remove(getRetainedColNames().toArray(new String[0]));
        }
        DataTableSpec createSpec = colRearranger.createSpec();
        return new DataTableSpec(createSpec, new DataTableSpec(getColumnSpecs()));
    }

    /**
     * @return the {@link DataColumnSpec} array with the specs of the columns that get appended
     */
    public DataColumnSpec[] getColumnSpecs() {
        return m_specs;
    }
}
