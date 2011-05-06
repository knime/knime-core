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
 * History
 *    27.08.2008 (Tobias Koetter): created
 */

package org.knime.base.data.aggregation.dialogutil;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue;

import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.ColumnAggregator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.table.DefaultTableModel;


/**
 * This {@link DefaultTableModel} holds all aggregation columns and their
 * aggregation method.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class AggregationColumnTableModel extends DefaultTableModel {

    private static final long serialVersionUID = 7331177164907480373L;

    private final List<ColumnAggregator> m_cols =
        new ArrayList<ColumnAggregator>();

    /**
     * Initializes the column aggregator table with the given
     * {@link ColumnAggregator}s.
     * @param colAggrs the {@link List} of {@link ColumnAggregator}s
     */
    protected void initialize(final List<ColumnAggregator> colAggrs) {
        m_cols.clear();
        m_cols.addAll(colAggrs);
    }

    /**
     * @param specs the {@link DataColumnSpec}s of the columns to add
     */
    protected void addColumn(final DataColumnSpec... specs) {
        if (specs == null || specs.length < 1) {
            return;
        }
        for (final DataColumnSpec spec : specs) {
            final AggregationMethod defaultMethod =
                AggregationMethods.getDefaultMethod(spec);
            m_cols.add(new ColumnAggregator(spec,
                    defaultMethod, defaultMethod.inclMissingCells()));
        }
        fireTableDataChanged();
    }

    /**
     * @param idxs the indices of the columns to remove
     */
    protected void removeColumn(final int... idxs) {
        if (idxs == null || idxs.length < 1) {
            return;
        }
        final Collection<ColumnAggregator> aggr =
            new LinkedList<ColumnAggregator>();
        for (final int idx : idxs) {
            aggr.add(m_cols.get(idx));
        }
        m_cols.removeAll(aggr);
        fireTableDataChanged();
    }

    /**
     * @param colNames the names of the columns to remove
     */
    protected void removeColumns(final Collection<String> colNames) {
        if (colNames == null || colNames.isEmpty()) {
            return;
        }
        final Set<String> colNameSet = new HashSet<String>(colNames);
        final Collection<ColumnAggregator> colAggr2Remove =
            new LinkedList<ColumnAggregator>();
        for (final ColumnAggregator colAggr : m_cols) {
            if (colNameSet.contains(colAggr.getColName())) {
                colAggr2Remove.add(colAggr);
            }
        }
        m_cols.removeAll(colAggr2Remove);
        fireTableDataChanged();
    }

    /**
     * Removes all aggregation column.
     */
    protected void removeAll() {
        m_cols.clear();
        fireTableDataChanged();
    }

    /**
     * @param selectedRows the index of the rows to change the aggregation
     * method
     * @param method the aggregation method to use
     */
    protected void setAggregationMethod(final int[] selectedRows,
            final AggregationMethod method) {
        if (selectedRows == null) {
            return;
        }
        for (final int i : selectedRows) {
            if (i < 0) {
                continue;
            }
            updateMethod(i, method);
        }
        fireTableDataChanged();
    }

    /**
     * Toggles the include missing cell option of all selected rows if they
     * support it.
     * @param selectedRows the index of the rows to change the aggregation
     * method
     */
    public void toggleMissingCellOption(final int[] selectedRows) {
        if (selectedRows == null) {
            return;
        }
        for (final int i : selectedRows) {
            if (i < 0) {
                continue;
            }
            final ColumnAggregator aggregator = getColumnAggregator(i);
            if (aggregator.supportsMissingValueOption()) {
                aggregator.setinclMissingCells(
                        !aggregator.inclMissingCells());
            }
        }
        fireTableDataChanged();
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnName(final int columnIdx) {
        switch (columnIdx) {
            case 0:
                return "Column";
            case 1:
                return "Aggregation (click to change)";
            default:
                return "missing";
        }
    }

    /**
     * @param type the type to check for compatibility
     * @return indices of all rows that are compatible with the given type
     * or an empty collection if none is compatible
     */
    public Collection<Integer> getCompatibleRowIdxs(
            final Class<? extends DataValue> type) {
        final Collection<Integer> result = new LinkedList<Integer>();
        for (int i = 0, length = m_cols.size(); i < length; i++) {
            if (isCompatible(i, type)) {
                result.add(Integer.valueOf(i));
            }
        }
        return result;
    }

    /**
     * @param row the index of the row to check
     * @param type the type to check for compatibility
     * @return <code>true</code> if the row contains a numerical column
     */
    public boolean isCompatible(final int row,
            final Class<? extends DataValue> type) {
        final ColumnAggregator colAggr = getColumnAggregator(row);
        return colAggr.isCompatible(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValueAt(final Object aValue, final int row,
            final int columnIdx) {
        if (aValue == null) {
            return;
        }
        if (aValue instanceof AggregationMethod) {
            final AggregationMethod newMethod =
                (AggregationMethod)aValue;
            assert columnIdx == 1;
            updateMethod(row, newMethod);
        }
        if (aValue instanceof Boolean) {
            final Boolean inclMissing = (Boolean) aValue;
            assert columnIdx == 2;
            updateInclMissing(row, inclMissing.booleanValue());
        }
    }

    /**
     * @param row row index to change the method for
     * @param inclMissingVals <code>true</code> if missing cells should be
     * considered during aggregation
     */
    private void updateInclMissing(final int row,
            final boolean inclMissingVals) {
        final ColumnAggregator colAggr = getColumnAggregator(row);
        colAggr.setinclMissingCells(inclMissingVals);
    }

    /**
     * @param row row index to change the method for
     * @param method the new aggregation method
     */
    private void updateMethod(final int row, final AggregationMethod method) {
        final ColumnAggregator colAggr = getColumnAggregator(row);
        m_cols.set(row, new ColumnAggregator(
                colAggr.getColSpec(), method, method.inclMissingCells()));
        fireTableCellUpdated(row, 2);
    }

    /**
     * @param row the index of the row
     * @return the aggregator for the row with the given index
     */
    public ColumnAggregator getColumnAggregator(final int row) {
        if (row < 0 || m_cols.size() <= row) {
            throw new IllegalArgumentException("Invalid row index: " + row);
        }
        final ColumnAggregator colAggr = m_cols.get(row);
        return colAggr;
    }


    /**
     * @return the {@link ColumnAggregator} {@link List}
     */
    public List<ColumnAggregator> getColumnAggregators() {
        return Collections.unmodifiableList(m_cols);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCellEditable(final int row, final int columnIdx) {
        switch (columnIdx) {
            case 1:
                return true;
            case 2:
                final ColumnAggregator aggregator = getColumnAggregator(row);
                return aggregator.supportsMissingValueOption();
            default:
                return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCount() {
        if (m_cols == null) {
            return 0;
        }
        return m_cols.size();
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public int getColumnCount() {
        return 3;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getColumnClass(final int columnIndex) {
        switch (columnIndex) {
        case 1:
            return AggregationMethod.class;
        case 2:
            return Boolean.class;
        default:
            return DataColumnSpec.class;
        }
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValueAt(final int row, final int columnIndex) {
        switch (columnIndex) {
        case 1:
            return getColumnAggregator(row).getMethod();
        case 2:
            return Boolean.valueOf(
                    getColumnAggregator(row).inclMissingCells());
        default:
            return getColumnAggregator(row).getColSpec();
        }
    }
}
