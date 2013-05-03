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
package org.knime.base.data.aggregation.dialogutil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;

import org.knime.base.data.aggregation.AggregationMethodDecorator;
import org.knime.base.data.aggregation.ColumnAggregator;


/**
 * This {@link DefaultTableModel} holds all aggregation columns and their
 * aggregation method.
 *
 * @author Tobias Koetter, University of Konstanz
 * @param <O> the {@link AggregationMethodDecorator} implementation
 * @since 2.6
 */
public abstract class AbstractAggregationTableModel
    <O extends AggregationMethodDecorator> extends AbstractTableModel
        implements AggregationTableModel<O> {

    /**The name of the settings column in the aggregation panel.*/
    private static final String SETTINGS_COL_NAME = "Parameter";

    private static final long serialVersionUID = 1;

    private final List<O> m_operators = new ArrayList<O>();

    private final int m_missingColIdx;

    private final int m_settingsColIdx;

    private final Class<?>[] m_classes;

    private final String[] m_colNames;

    /**Constructor for class AbstractAggregationTableModel.
     * include/exclude missing cell option should be added
     * @param colNames the column classes without the missing cell option column
     * @param colClasses the column classes without the missing cell option
     * column
     * @param appendMissingCol <code>true</code> if a column to change the
     */
    public AbstractAggregationTableModel(final String[] colNames,
            final Class<?>[] colClasses, final boolean appendMissingCol) {
        if (colClasses == null) {
            throw new NullPointerException("colClasses must not be null");
        }
        if (colNames == null) {
            throw new NullPointerException("colNames must not be null");
        }
        if (colNames.length != colClasses.length) {
            throw new IllegalArgumentException(
                    "Number of column names and classes should be equal");
        }
        if (appendMissingCol) {
            m_missingColIdx = colClasses.length;
            m_settingsColIdx = m_missingColIdx + 1;
            m_colNames = new String[colNames.length + 2];
            System.arraycopy(colNames, 0, m_colNames, 0, colNames.length);
            m_colNames[m_missingColIdx] = "Missing";
            m_colNames[m_settingsColIdx] = SETTINGS_COL_NAME;
            m_classes = new Class<?>[colClasses.length + 2];
            System.arraycopy(colClasses, 0, m_classes, 0, colClasses.length);
            m_classes[m_missingColIdx] = Boolean.class;
            m_classes[m_settingsColIdx] = Boolean.class;
        } else {
            m_missingColIdx = -1;
            m_settingsColIdx = colClasses.length;
            m_colNames = new String[colNames.length + 1];
            System.arraycopy(colNames, 0, m_colNames, 0, colNames.length);
            m_colNames[m_settingsColIdx] = SETTINGS_COL_NAME;
            m_classes = new Class<?>[colClasses.length + 1];
            System.arraycopy(colClasses, 0, m_classes, 0, colClasses.length);
            m_classes[m_settingsColIdx] = Boolean.class;

        }
    }

    /**
     * Initializes the column aggregator table with the given
     * {@link ColumnAggregator}s.
     * @param operators the {@link AggregationMethodDecorator} {@link List}
     */
    @Override
    public void initialize(final List<O> operators) {
        m_operators.clear();
        m_operators.addAll(operators);
    }

    /**
     * Removes all operators.
     */
    @Override
    public void removeAll() {
        m_operators.clear();
        fireTableDataChanged();
    }

    /**
     * Toggles the include missing cell option of all selected rows if they
     * support it.
     * @param selectedRows the index of the rows to change the aggregation
     * method
     */
    @Override
    public void toggleMissingCellOption(final int[] selectedRows) {
        if (selectedRows == null) {
            return;
        }
        for (final int i : selectedRows) {
            if (i < 0) {
                continue;
            }
            toggleMissingCellOption(i);
        }
    }

    /**
     * @param row the index of the row to toggle the missing cell option
     */
    protected void toggleMissingCellOption(final int row) {
        final O operator = getOperator(row);
        if (operator.supportsMissingValueOption()) {
            operator.setInclMissingCells(
                    !operator.inclMissingCells());
            fireTableRowsUpdated(row, row);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValueAt(final Object aValue, final int row,
            final int columnIdx) {
        if (row < 0 || row >= m_operators.size()) {
            //this might happen if the user removes a row that he also
            //edited e.g. changed the name of the result column
            fireTableDataChanged();
            return;
        }
        if (columnIdx == m_missingColIdx) {
            toggleMissingCellOption(row);
        } else {
            setValue(aValue, row, columnIdx);
        }
    }

    /**
     * The method is also responsible to call the appropriate table changed
     * method e.g. {@link #fireTableCellUpdated(int, int)} to notify the
     * table renderer of the changes.
     *
     * @param aValue the value to set
     * @param row the row to update
     * @param columnIdx the column to update
     */
    protected abstract void setValue(final Object aValue, final int row,
            final int columnIdx);

    /**
     * @param row row index to change the method for
     * @param inclMissingVals <code>true</code> if missing cells should be
     * considered during aggregation
     */
    protected void updateInclMissing(final int row,
            final boolean inclMissingVals) {
        final AggregationMethodDecorator operator = getOperator(row);
        if (operator.supportsMissingValueOption()) {
            operator.setInclMissingCells(inclMissingVals);
        }
    }

    /**
     * @param row the index of the row
     * @return the aggregator for the row with the given index
     */
    private O getOperator(final int row) {
        if (row < 0 || m_operators.size() <= row) {
            throw new IllegalArgumentException("Invalid row index: " + row);
        }
        final O operators = m_operators.get(row);
        return operators;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCellEditable(final int row, final int columnIdx) {
        final O operator = getOperator(row);
        if (!operator.isValid()) {
            //the row is not editable if the operator is invalid
            return false;
        }
        if (columnIdx == m_missingColIdx) {
            return operator.supportsMissingValueOption();
        }
        if (columnIdx == m_settingsColIdx) {
            return operator.hasOptionalSettings();
        }
        return isEditable(row, columnIdx);
    }

    /**
     * This method does only need to handle the none missing cell option columns
     * since the include/exclude missing cell column is handled by this
     * abstract class.
     * @param row the row index
     * @param columnIdx the index of the column to check
     * @return <code>true</code> if the column can be edited
     */
    protected abstract boolean isEditable(int row, int columnIdx);

    /**
     * This method does only need to handle the none missing cell option columns
     * since the include/exclude missing cell column is handled by this
     * abstract class.
     * @param row the row index
     * @param columnIndex the column index
     * @return the value at the corresponding position
     */
    protected abstract Object getValueAtRow(int row, int columnIndex);

    /**
     * @return the rows {@link List}
     */
    public List<O> getRows() {
        return Collections.unmodifiableList(m_operators);
    }

    /**
     * {@inheritDoc}
     * @since 2.7
     */
    @Override
    public O getRow(final int row) {
        if (row < 0 || getRowCount() <= row) {
            throw new IllegalArgumentException("Invalid row index: " + row);
        }
        return m_operators.get(row);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCount() {
        return m_operators.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getColumnCount() {
        return m_classes.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnName(final int column) {
        return m_colNames[column];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getColumnClass(final int columnIndex) {
        return m_classes[columnIndex];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValueAt(final int row, final int columnIndex) {
        if (columnIndex == m_missingColIdx) {
            return Boolean.valueOf(getOperator(row).inclMissingCells());
        }
        if (columnIndex == m_settingsColIdx) {
            return Boolean.valueOf(getOperator(row).hasOptionalSettings());
        }
        return getValueAtRow(row, columnIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(final int... idxs) {
        if (idxs == null || idxs.length < 1) {
            return;
        }
        final List<O> ops2remove =
            new LinkedList<O>();
        for (final int idx : idxs) {
            ops2remove.add(m_operators.get(idx));
        }
        remove(ops2remove);
    }

    /**
     * @param ops2Remove the operators to remove
     */
    protected void remove(final Collection<O> ops2Remove) {
        m_operators.removeAll(ops2Remove);
        fireTableDataChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(final List<O> operators) {
        if (operators == null || operators.isEmpty()) {
            return;
        }
        if (m_operators.addAll(operators)) {
            //notify the rest if the rows have changed
            fireTableDataChanged();
        }
    }

    /**
     * @param row the index of the row to replace
     * @param operator the new operator to use
     */
    protected void updateRow(final int row, final O operator) {
        m_operators.set(row, operator);
        fireTableRowsUpdated(row, row);
    }

    /**
     * {@inheritDoc}
     * @since 2.7
     */
    @Override
    public boolean containsSettingsOperator() {
        for (O op : getRows()) {
            if (op.hasOptionalSettings()) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMissingCellOptionColIdx() {
        return m_missingColIdx;
    }

    /**
     * {@inheritDoc}
     * @since 2.7
     */
    @Override
    public int getSettingsButtonColIdx() {
        return m_settingsColIdx;
    }
}
