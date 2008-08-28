/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.ConfigRO;

import org.knime.base.node.preproc.groupby.GroupByNodeModel;
import org.knime.base.node.preproc.groupby.aggregation.AggregationMethod;
import org.knime.base.node.preproc.groupby.aggregation.ColumnAggregator;

import java.util.ArrayList;
import java.util.Collection;
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
     * @param specs the {@link DataColumnSpec}s of the columns to add
     */
    public void addColumn(final DataColumnSpec... specs) {
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
    public void removeColumn(final int... idxs) {
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
    public void removeColumns(final Collection<String> colNames) {
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
    public void removeAll() {
        m_cols.clear();
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
            return m_cols.get(row).getMethod();
        }
        return m_cols.get(row).getColSpec();
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
            final ColumnAggregator colAggr =
                m_cols.get(row);
            m_cols.set(row, new ColumnAggregator(
                    colAggr.getColSpec(), newMethod));
        }
    }

    /**
     * @param settings the settings object to write to
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        ColumnAggregator.saveColumnAggregators(settings, m_cols);
    }

    /**
     * @param settings the settings object to read from
     * @param spec the input {@link DataTableSpec}
     * @param groupCols the group by columns for compatibility
     */
    public void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec spec, final List<String> groupCols) {
        m_cols.clear();
        try {
            m_cols.addAll(ColumnAggregator.loadColumnAggregators(settings));
        } catch (final InvalidSettingsException e) {
            m_cols.addAll(getColumnMethods(spec, groupCols, settings));
        }
    }

    /**
     * Helper method to get the aggregation methods for the old node settings.
     * @param spec the input {@link DataTableSpec}
     * @param groupByCols the columns that are used for grouping
     * @param config the config object to read from
     * @return the {@link ColumnAggregator}s
     */
    private static Collection<ColumnAggregator> getColumnMethods(
            final DataTableSpec spec, final List<String> groupByCols,
            final ConfigRO config) {
        String numeric = null;
        String nominal = null;
        try {
            numeric =
                config.getString(GroupByNodeModel.OLD_CFG_NUMERIC_COL_METHOD);
            nominal =
                config.getString(GroupByNodeModel.OLD_CFG_NOMINAL_COL_METHOD);
        } catch (final InvalidSettingsException e) {
            numeric = AggregationMethod.getDefaultNumericMethod().getLabel();
            nominal = AggregationMethod.getDefaultNominalMethod().getLabel();
        }
        return GroupByNodeModel.createColumnAggregators(spec, groupByCols,
                numeric, nominal);
    }
}
