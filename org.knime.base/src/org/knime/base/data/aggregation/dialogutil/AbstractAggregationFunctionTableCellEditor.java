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
 *   17.07.2014 (koetter): created
 */
package org.knime.base.data.aggregation.dialogutil;

import java.awt.Component;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;

import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.core.data.DataType;
import org.knime.core.node.port.database.aggregation.AggregationFunction;
import org.knime.core.node.port.database.aggregation.AggregationFunctionProvider;

/**
 * This class extends the {@link DefaultCellEditor} class to provide a combobox which allows the user to choose
 * one of the available {@link AggregationFunction}s.
 *
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 * @param <F> {@link AggregationFunction} implementation to work with
 * @param <R> {@link AggregationFunctionRow} implementation to work with
 * @since 2.11
 */
public abstract class AbstractAggregationFunctionTableCellEditor<F extends AggregationFunction,
    R extends AggregationFunctionRow<F>> extends DefaultCellEditor {
    private static final long serialVersionUID = 1L;
    private AggregationFunctionProvider<F> m_provider;

    /**Constructor for class AggregationMethodTableCellEditor.
     * @param provider {@link AggregationFunctionProvider}
     */
    public AbstractAggregationFunctionTableCellEditor(final AggregationFunctionProvider<F> provider) {
        super(new AggregationFunctionComboBox());
        m_provider = provider;
    }


    /**
     * @param provider {@link AggregationFunctionProvider}
     */
    public void setAggregationFunctionProvider(final AggregationFunctionProvider<F> provider) {
        if (!provider.equals(m_provider)) {
        	//reset the provider if it has changed. This might happen when the user changes the db connector
            m_provider = provider;
            //also reset the type in the method combobox to force the method reloading
            getBox().resetType();
        }
    }

    /**
     * @return the {@link AggregationFunctionProvider} might be <code>null</code>
     */
    protected AggregationFunctionProvider<F> getAggregationFunctionProvider() {
        return m_provider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected,
        final int rowIdx, final int column) {
        if (value instanceof AggregationFunctionRow<?>) {
            @SuppressWarnings("unchecked")
            R row = (R)value;
            final DataType type = getDataType(table, row, isSelected, rowIdx, column);
            final AggregationFunction selectedMethod =
                    getSelectedAggregationMethod(table, row, isSelected, rowIdx, column);
            final List<? extends AggregationFunction> compatibleMethods = getCompatibleMethods(type);
            if (type != null) {
                getBox().update(type, compatibleMethods, selectedMethod);
            }
        }
        return super.getTableCellEditorComponent(table, value, isSelected, rowIdx, column);
    }

    /**
     * @param type the {@link DataType} to get the methods for
     * @return {@link List} of {@link AggregationMethod}s that are compatible
     */
    public List<F> getCompatibleMethods(final DataType type) {
        return m_provider.getCompatibleFunctions(type, true);
    }

    /**
     * @param   table           the <code>JTable</code> that is asking the
     *                          editor to edit; can be <code>null</code>
     * @param   value           the value of the cell to be edited; it is
     *                          up to the specific editor to interpret
     *                          and draw the value.  For example, if value is
     *                          the string "true", it could be rendered as a
     *                          string or it could be rendered as a check
     *                          box that is checked.  <code>null</code>
     *                          is a valid value
     * @param   isSelected      true if the cell is to be rendered with
     *                          highlighting
     * @param   row             the row of the cell being edited
     * @param   column          the column of the cell being edited
     * @return the {@link AggregationMethod} of the selected value or <code>null</code> if it cannot be retrieved
     */
    protected abstract AggregationFunction getSelectedAggregationMethod(JTable table, R value, boolean isSelected,
        int row, int column);

    /**
     * @param   table           the <code>JTable</code> that is asking the
     *                          editor to edit; can be <code>null</code>
     * @param   value           the value of the cell to be edited; it is
     *                          up to the specific editor to interpret
     *                          and draw the value.  For example, if value is
     *                          the string "true", it could be rendered as a
     *                          string or it could be rendered as a check
     *                          box that is checked.  <code>null</code>
     *                          is a valid value
     * @param   isSelected      true if the cell is to be rendered with
     *                          highlighting
     * @param   row             the row of the cell being edited
     * @param   column          the column of the cell being edited
     * @return the {@link DataType} of the row to determine the supported {@link AggregationMethod}s that are displayed
     * in the drop down box or <code>null</code> if it cannot be retrieved
     */
    protected abstract DataType getDataType(JTable table, R value, boolean isSelected, int row, int column);

    /**
     * @return the {@link AggregationFunctionComboBox}
     */
    protected AggregationFunctionComboBox getBox() {
        return (AggregationFunctionComboBox)getComponent();
    }
}
