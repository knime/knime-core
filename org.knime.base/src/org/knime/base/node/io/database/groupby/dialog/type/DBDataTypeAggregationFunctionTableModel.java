/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 */

package org.knime.base.node.io.database.groupby.dialog.type;

import java.util.Collection;
import java.util.LinkedList;

import javax.swing.table.DefaultTableModel;

import org.knime.base.data.aggregation.dialogutil.AbstractAggregationTableModel;
import org.knime.base.data.aggregation.dialogutil.AggregationFunctionRow;
import org.knime.core.data.DataType;
import org.knime.core.node.port.database.aggregation.AggregationFunction;
import org.knime.core.node.port.database.aggregation.AggregationFunctionProvider;
import org.knime.core.node.port.database.aggregation.DBAggregationFunction;


/**
 * This {@link DefaultTableModel} holds all aggregation columns and their
 * aggregation method.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class DBDataTypeAggregationFunctionTableModel
    extends AbstractAggregationTableModel<DBAggregationFunction, DBDataTypeAggregationFunctionRow> {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     */
    public DBDataTypeAggregationFunctionTableModel() {
        this(null);
    }
    /**Constructor for class AggregationColumnTableModel.
     * @param provider {@link AggregationFunctionProvider}
     */
    public DBDataTypeAggregationFunctionTableModel(
        final AggregationFunctionProvider<DBAggregationFunction> provider) {
        super(new String[] {"Data type", "Aggregation (click to change)"},
            new Class<?>[] {DBDataTypeAggregationFunctionRow.class, DBDataTypeAggregationFunctionRow.class},
            false, provider);
    }

    /**
     * @param type the type to check for compatibility
     * @return indices of all rows that are compatible with the given type
     * or an empty collection if none is compatible
     */
    public Collection<Integer> getCompatibleRowIdxs(final DataType type) {
        return getRowIdxs(true, type);
    }

    /**
     * @param type the type to check for compatibility
     * @return indices of all rows that are not compatible with the given type
     * or an empty collection if all are compatible
     */
    public Collection<Integer> getNotCompatibleRowIdxs(final DataType type) {
        return getRowIdxs(false, type);
    }

    private Collection<Integer> getRowIdxs(final boolean compatible, final DataType type) {
        final Collection<Integer> result = new LinkedList<>();
        for (int i = 0, length = getRowCount(); i < length; i++) {
            if ((compatible && isCompatible(i, type)) || (!compatible && !isCompatible(i, type))) {
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
    public boolean isCompatible(final int row, final DataType type) {
        final DBDataTypeAggregationFunctionRow colAggr = getRow(row);
        return type.isASuperTypeOf(colAggr.getDataType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(final Object aValue, final int rowIdx, final int columnIdx) {
        if (aValue == null) {
            return;
        }
        if (columnIdx == 1) {
            if (aValue instanceof AggregationFunction) {
                updateFunction(rowIdx, (AggregationFunction)aValue);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object getValueAtRow(final int row, final int columnIndex) {
        return getRow(row);
    }

    /**
     * @param selectedRows the rows to update
     * @param method the {@link DBAggregationFunction} to use
     */
    protected void setAggregationFunction(final int[] selectedRows, final AggregationFunction method) {
        for (final int row : selectedRows) {
            updateFunction(row, method);
        }
    }

    /**
     * @param row the row to update
     * @param function the {@link DBDataTypeAggregationFunctionRow} to use
     */
    private void updateFunction(final int row, final AggregationFunction function) {
        final DBDataTypeAggregationFunctionRow old = getRow(row);
        if (old.getFunction().getId().equals(function.getId())) {
            //check if the method has changed
            return;
        }
        //create a new operator each time it is updated to guarantee that
        //each column has its own operator instance
        final DBAggregationFunction cloneFunction = getAggregationFunctionProvider().getFunction(function.getId());
        final DBDataTypeAggregationFunctionRow newRow =
                new DBDataTypeAggregationFunctionRow(old.getDataType(), cloneFunction);
        newRow.setValid(old.isValid());
        updateRow(row, newRow);
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
    protected void toggleMissingCellOption(final int row) {
        final AggregationFunctionRow<DBAggregationFunction> funcRow = getRow(row);
        funcRow.setInclMissingCells(!funcRow.inclMissingCells());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateInclMissing(final int row, final boolean inclMissingVals) {
        final AggregationFunctionRow<DBAggregationFunction> funcRow = getRow(row);
        funcRow.setInclMissingCells(inclMissingVals);
    }
}
