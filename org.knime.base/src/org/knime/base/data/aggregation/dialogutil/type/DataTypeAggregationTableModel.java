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
 *   06.07.2014 (koetter): created
 */
package org.knime.base.data.aggregation.dialogutil.type;

import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.dialogutil.AbstractAggregationTableModel;
import org.knime.core.data.DataType;




/**
 * {@link AbstractAggregationTableModel} that stores {@link DataType}s and the {@link AggregationMethod} to use.
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 * @since 2.11
 */
public class DataTypeAggregationTableModel
    extends AbstractAggregationTableModel<AggregationMethod, DataTypeAggregator> {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     */
    DataTypeAggregationTableModel() {
        super(new String[] {"Data type", "Aggregation (click to change)"},
            new Class[] {DataTypeAggregator.class, DataTypeAggregator.class}, true, AggregationMethods.getInstance());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void setValue(final Object aValue, final int row, final int columnIdx) {
        if (aValue == null) {
            return;
        }
        if (aValue instanceof AggregationMethod) {
            assert columnIdx == 1;
            final AggregationMethod newMethod = (AggregationMethod)aValue;
            updateMethod(row, newMethod);
        }
    }

    /**
     * @param row the row to update
     * @param method the {@link AggregationMethod} to use
     */
    private void updateMethod(final int row, final AggregationMethod method) {
        final DataTypeAggregator old = getRow(row);
        if (old.getMethodTemplate().getId().equals(method.getId())) {
            //check if the method has changed
            return;
        }
        //create a new operator each time it is updated to guarantee that
        //each column has its own operator instance
        final AggregationMethod methodClone = AggregationMethods.getMethod4Id(method.getId());
        final DataTypeAggregator newRow =
                new DataTypeAggregator(old.getDataType(), methodClone, old.inclMissingCells());
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
    protected Object getValueAtRow(final int row, final int columnIndex) {
        final DataTypeAggregator aggregator = getRow(row);
        switch (columnIndex) {
        case 0:
            return aggregator;
        case 1:
            return aggregator;

        default:
            break;
        }
        return null;
    }

}
