/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 *    27.08.2008 (Tobias Koetter): created
 */

package org.knime.base.node.preproc.groupby.dialogutil;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;

import org.knime.base.node.preproc.groupby.aggregation.AggregationMethod;
import org.knime.base.node.preproc.groupby.aggregation.ColumnAggregator;

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
            m_cols.add(new ColumnAggregator(spec,
                    AggregationMethod.getDefaultMethod(spec)));
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
     * {@inheritDoc}
     */
    @Override
    public String getColumnName(final int columnIdx) {
        if (columnIdx == 0) {
            return "Column";
        }
        return "Aggregation (click to change)";

    }

    /**
     * @param rows2check the index of the rows to check
     * @return <code>true</code> if all rows with the given index are numerical
     */
    protected boolean onlyNumerical(final int[] rows2check) {
        if (rows2check == null) {
            return false;
        }
        for (final int idx : rows2check) {
            if (!isNumerical(idx)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return the row indices of all numerical rows
     */
    protected Collection<Integer> getNumericalRowIdxs() {
        final Collection<Integer> result = new LinkedList<Integer>();
        for (int i = 0, length = m_cols.size(); i < length; i++) {
            if (isNumerical(i)) {
                result.add(new Integer(i));
            }
        }
        return result;
    }

    /**
     * @return the row indices of all none numerical rows
     */
    protected Collection<Integer> getNoneNumericalRowIdxs() {
        final Collection<Integer> result = new LinkedList<Integer>();
        for (int i = 0, length = m_cols.size(); i < length; i++) {
            if (!isNumerical(i)) {
                result.add(new Integer(i));
            }
        }
        return result;
    }

    /**
     * @param row the index of the row to check
     * @return <code>true</code> if the row contains a numerical column
     */
    protected boolean isNumerical(final int row) {
        final ColumnAggregator colAggr = getColumnAggregator(row);
        return colAggr.getDataType().isCompatible(DoubleValue.class);
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
    }

    /**
     * @param row row index to change the method for
     * @param method the new aggregation method
     */
    private void updateMethod(final int row, final AggregationMethod method) {
        final ColumnAggregator colAggr = getColumnAggregator(row);
        m_cols.set(row, new ColumnAggregator(
                colAggr.getColSpec(), method));
    }

    /**
     * @param row the index of the row
     * @return the aggregator for the row with the given index
     */
    private ColumnAggregator getColumnAggregator(final int row) {
        if (row < 0 || m_cols.size() <= row) {
            throw new IllegalArgumentException("Invalid row index");
        }
        final ColumnAggregator colAggr =
            m_cols.get(row);
        return colAggr;
    }


    /**
     * @return the {@link ColumnAggregator} {@link List}
     */
    protected List<ColumnAggregator> getColumnAggregators() {
        return Collections.unmodifiableList(m_cols);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCellEditable(final int row, final int columnIdx) {
        return (columnIdx == 1);
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
        return 2;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getColumnClass(final int columnIndex) {
        if (columnIndex == 1) {
            return AggregationMethod.class;
        }
        return DataColumnSpec.class;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValueAt(final int row, final int columnIndex) {
        if (columnIndex == 1) {
            return getColumnAggregator(row).getMethod();
        }
        return getColumnAggregator(row).getColSpec();
    }
}
