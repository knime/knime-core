/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by
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
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.node.ExecutionMonitor;

/**
 * {@link CellFactory} implementation of the Moving Aggregation node. The factory takes care of caching the rows
 * within the window and the calculation of the selected aggregation values.
 *
 *  @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 *  @since 2.10
 */
public class MovingAggregationCellFactory implements CellFactory {

    private final LinkedList<DataRow> m_window;
    private final int m_windowLength;
    private final int[] m_aggrColIdxs;
    private AggregationOperator[] m_ops;
    private final DataColumnSpec[] m_specs;
    private final Set<String> m_aggregationCols;
    private Set<String> m_retainedCols;
    private boolean m_handleMissings;

    /**
     * @param spec the {@link DataTableSpec} of the table to process
     * @param globalSettings the {@link GlobalSettings} to use for aggregation
     * @param colNamePolicy the {@link ColumnNamePolicy}
     * @param aggregators list with {@link ColumnAggregator}s to use
     * @param windowLength the length of the aggregation window
     * @param handleMissings if true, a smaller window size is used in the beginning to handle missing values.
     * @throws IllegalArgumentException if the selected {@link ColumnAggregator}s and the {@link ColumnNamePolicy}
     * results in duplicate column names
     */
    MovingAggregationCellFactory(final DataTableSpec spec, final GlobalSettings globalSettings,
        final ColumnNamePolicy colNamePolicy, final List<ColumnAggregator> aggregators, final int windowLength,
        final boolean handleMissings) {
        m_window = new LinkedList<>();
        m_windowLength = windowLength;
        m_handleMissings = handleMissings;
        m_aggrColIdxs = new int[aggregators.size()];
        m_ops = new AggregationOperator[aggregators.size()];
        m_specs = new DataColumnSpec[aggregators.size()];
        m_aggregationCols = new HashSet<>(aggregators.size());
        m_retainedCols = new HashSet<>(Arrays.asList(spec.getColumnNames()));
        Set<String> uniqueNames = new HashSet<>(aggregators.size());
        int counter = 0;
        for (final ColumnAggregator colAggr : aggregators) {
            final String colName = colAggr.getOriginalColName();
            m_aggrColIdxs[counter] = spec.findColumnIndex(colName);
            m_ops[counter] = colAggr.getOperator(globalSettings);
            final String aggrColName = colNamePolicy.createColumName(colAggr);
            if (!uniqueNames.add(aggrColName)) {
                throw new IllegalArgumentException("Duplicate column name " + aggrColName);
            }
            m_specs[counter] = colAggr.createColumnSpec(aggrColName, spec.getColumnSpec(m_aggrColIdxs[counter]));
            m_aggregationCols.add(colName);
            m_retainedCols.remove(colName);
            counter++;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell[] getCells(final DataRow row) {
        m_window.add(row);
        final DataCell[] cells = new DataCell[m_ops.length];
        final boolean windowFull = (m_window.size() >= m_windowLength);
        if (windowFull || m_handleMissings) {
            for (int i = 0, length = m_ops.length; i < length; i++) {
                int colIdx = m_aggrColIdxs[i];
                final AggregationOperator op = m_ops[i];
                for (final DataRow windowRow : m_window) {
                    op.compute(windowRow, colIdx);
                }
                final DataCell resultCell = op.getResult();
                op.reset();
                cells[i] = resultCell;
            }
            if (windowFull) {
                //remove the first row only when the window is full
                //not during the missing value handling phase!
                m_window.removeFirst();
            }
        } else {
            //the window is not yet full return missing cells
            Arrays.fill(cells, DataType.getMissingCell());
        }
        return cells;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataColumnSpec[] getColumnSpecs() {
        return m_specs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProgress(final int curRowNr, final int rowCount, final RowKey lastKey, final ExecutionMonitor exec) {
        exec.setProgress(curRowNr / (double) rowCount, "Processing row " + curRowNr + " of " + rowCount);
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
    public Collection<String> getRetainedColNames() {
        return m_retainedCols;
    }
}
