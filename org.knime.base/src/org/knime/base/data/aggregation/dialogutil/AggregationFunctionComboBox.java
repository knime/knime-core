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
package org.knime.base.data.aggregation.dialogutil;

import java.util.List;

import javax.swing.JComboBox;
import javax.swing.event.ListDataEvent;

import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.core.data.DataType;
import org.knime.core.node.port.database.aggregation.AggregationFunction;

/**
 * This combo box is used in the aggregation column table to let the user
 * choose from the different compatible aggregation methods per aggregation
 * column.
 *
 * @author Tobias Koetter, KNIME.com, Zurich, Switzerland
 * @since 2.11
 */
public class AggregationFunctionComboBox extends JComboBox<AggregationFunction> {

    private static final long serialVersionUID = 1L;

    private DataType m_type = null;

    private boolean m_selectingItem;

    /**
     * Creates AggregationMethod selection combo box with the allowed
     * methods.
     */
    public AggregationFunctionComboBox() {
        super();
        this.setBackground(this.getBackground());
        this.setRenderer(new AggregationFunctionAndRowListCellRenderer());
    }

    /**
     * Resets the type to force an update aggregation method options the user can select from.
     * @since 2.12
     */
    public void resetType() {
        m_type = null;
    }

    /**
     * @param type the {@link DataType} used to initialize this combobox
     * @param list {@link List} of {@link AggregationMethod}s the user can choose from
     * @param selected the current selected method
     */
    public void update(final DataType type, final List<? extends AggregationFunction> list,
        final AggregationFunction selected) {
        if (m_type == null || !m_type.equals(type)) {
            //recreate the combo box if the type has change
            removeAllItems();
            for (final AggregationFunction method : list) {
                addItem(method);
            }
            //save the current type for comparison
            m_type = type;
        }
        //select the previous selected item
        setSelectedItem(selected);
    }

    @Override
    public void setSelectedItem(final Object anObject) {
        Object objectToSelect = null;
        if (anObject != null && !isEditable()) {
            final AggregationFunction functionToSelect;
            if (anObject instanceof AggregationFunctionRow<?>) {
                functionToSelect = ((AggregationFunctionRow<?>)anObject).getFunction();
            } else if (anObject instanceof AggregationFunction) {
                functionToSelect = (AggregationFunction) anObject;
            } else {
                functionToSelect = null;
            }
            if (functionToSelect != null) {
                // For non editable combo boxes, an invalid selection
                // will be rejected.
                boolean found = false;
                for (int i = 0; i < dataModel.getSize(); i++) {
                    AggregationFunction element = dataModel.getElementAt(i);
                    if (functionToSelect.getId().equals(element.getId())) {
                        found = true;
                        objectToSelect = element;
                        break;
                    }
                }
                if (!found) {
                    return;
                }
            }

            // Must toggle the state of this flag since this method
            // call may result in ListDataEvents being fired.
            m_selectingItem = true;
            dataModel.setSelectedItem(objectToSelect);
            m_selectingItem = false;

            if (selectedItemReminder != dataModel.getSelectedItem()) {
                // in case a users implementation of ComboBoxModel
                // doesn't fire a ListDataEvent when the selection
                // changes.
                selectedItemChanged();
            }
        }
        fireActionEvent();
    }

    @Override
    public void contentsChanged(final ListDataEvent e) {
        Object oldSelection = selectedItemReminder;
        Object newSelection = dataModel.getSelectedItem();
        if (oldSelection == newSelection) {
            return;
        }
        if ((oldSelection == null || oldSelection instanceof AggregationFunction)
                && (newSelection instanceof AggregationFunction)) {
            final AggregationFunction oldFunction = (AggregationFunction)oldSelection;
            final AggregationFunction newFunction = (AggregationFunction)newSelection;
            if (oldFunction == null || !oldFunction.getId().equals(newFunction.getId())) {
                selectedItemChanged();
                if (!m_selectingItem) {
                    fireActionEvent();
                }
            }
        } else {
            super.contentsChanged(e);
        }
    }

    /**
     * @return the selected {@link AggregationMethods}
     */
    public AggregationMethod getSelectedMethod() {
        return (AggregationMethod)getSelectedItem();
    }
}
