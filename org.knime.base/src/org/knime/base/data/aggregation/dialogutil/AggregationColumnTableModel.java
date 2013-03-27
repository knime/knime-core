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
 * History
 *    27.08.2008 (Tobias Koetter): created
 */

package org.knime.base.data.aggregation.dialogutil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.table.DefaultTableModel;

import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;


/**
 * This {@link DefaultTableModel} holds all aggregation columns and their
 * aggregation method.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class AggregationColumnTableModel
    extends AbstractAggregationTableModel<ColumnAggregator> {

    /**Constructor for class AggregationColumnTableModel.
     */
    public AggregationColumnTableModel() {
        super(new String[] {"Column", "Aggregation (click to change)"},
                new Class<?>[] {DataColumnSpec.class, AggregationMethod.class},
                true);
    }
    private static final long serialVersionUID = 7331177164907480373L;

    /**
     * @param type the type to check for compatibility
     * @return indices of all rows that are compatible with the given type
     * or an empty collection if none is compatible
     */
    public Collection<Integer> getCompatibleRowIdxs(
            final Class<? extends DataValue> type) {
        return getRowIdxs(true, type);
    }

    /**
     * @param type the type to check for compatibility
     * @return indices of all rows that are not compatible with the given type
     * or an empty collection if all are compatible
     * @since 2.6
     */
    public Collection<Integer> getNotCompatibleRowIdxs(
            final Class<? extends DoubleValue> type) {
        return getRowIdxs(false, type);
    }

    private Collection<Integer> getRowIdxs(final boolean compatible,
            final Class<? extends DataValue> type) {
        final Collection<Integer> result = new LinkedList<Integer>();
        for (int i = 0, length = getRowCount(); i < length; i++) {
            if ((compatible && isCompatible(i, type))
                    || (!compatible && !isCompatible(i, type))) {
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
        final ColumnAggregator colAggr = getRow(row);
        return colAggr.isCompatible(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(final Object aValue, final int row,
            final int columnIdx) {
        if (aValue == null) {
            return;
        }
        if (aValue instanceof AggregationMethod) {
            assert columnIdx == 1;
            final AggregationMethod newMethod =
                (AggregationMethod)aValue;
            updateMethod(row, newMethod);
        }
    }

    /**
     * @param selectedRows the rows to update
     * @param method the {@link AggregationMethod} to use
     */
    protected void setAggregationMethod(final int[] selectedRows,
            final AggregationMethod method) {
        for (final int row : selectedRows) {
            updateMethod(row, method);
        }
    }

    /**
     * @param row the row to update
     * @param method the {@link AggregationMethod} to use
     */
    private void updateMethod(final int row, final AggregationMethod method) {
        final ColumnAggregator old = getRow(row);
        if (old.getMethodTemplate().equals(method)) {
            //check if the method has changed
            return;
        }
        //create a new operator each time it is updated to guarantee that
        //each column has its own operator instance
        AggregationMethod methodClone =
            AggregationMethods.getMethod4Id(method.getId());
        updateRow(row, new ColumnAggregator(old.getOriginalColSpec(),
                methodClone, old.inclMissingCells()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isEditable(final int row, final int columnIdx) {
        switch (columnIdx) {
            case 1:
                return true;
            default:
                return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object getValueAtRow(final int row, final int columnIndex) {
        final ColumnAggregator columnAggregator = getRow(row);
        switch (columnIndex) {
        case 0:
            return columnAggregator.getOriginalColSpec();
        case 1:
            return columnAggregator.getMethodTemplate();

        default:
            break;
        }
        return null;
    }


    /**
     * @param row the index of the row
     * @return the aggregator for the row with the given index
     * @see #getRow(int)
     */
    @Deprecated
    public ColumnAggregator getColumnAggregator(final int row) {
        return getRow(row);
    }

    /**
     * @param colNames the names of the columns to remove
     */
    @Deprecated
    protected void removeColumns(final Collection<String> colNames) {
        if (colNames == null || colNames.isEmpty()) {
            return;
        }
        final Set<String> colNameSet = new HashSet<String>(colNames);
        final Collection<ColumnAggregator> colAggr2Remove =
            new LinkedList<ColumnAggregator>();
        for (final ColumnAggregator colAggr : getRows()) {
            if (colNameSet.contains(colAggr.getOriginalColName())) {
                colAggr2Remove.add(colAggr);
            }
        }
        remove(colAggr2Remove);
    }

    /**
     * @param idxs the indices of the columns to remove
     * @see AggregationColumnTableModel#remove(int...)
     */
    @Deprecated
    protected void removeColumn(final int... idxs) {
        remove(idxs);
    }

    /**
     * @return the {@link ColumnAggregator} {@link List}
     * @see #getRows()
     */
    @Deprecated
    public List<ColumnAggregator> getColumnAggregators() {
        return getRows();
    }

    /**
     * @param specs the {@link DataColumnSpec}s of the columns to add
     * @see #add(List)
     */
    @Deprecated
    protected void addColumn(final DataColumnSpec... specs) {
        if (specs == null || specs.length < 1) {
            return;
        }
        final List<ColumnAggregator> aggregators =
            new ArrayList<ColumnAggregator>(specs.length);
        for (final DataColumnSpec spec : specs) {
            final AggregationMethod defaultMethod =
                AggregationMethods.getDefaultMethod(spec);
            aggregators.add(new ColumnAggregator(spec, defaultMethod));
        }
        add(aggregators);
    }
}
