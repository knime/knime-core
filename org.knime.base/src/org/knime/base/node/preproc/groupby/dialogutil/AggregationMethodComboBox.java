/*
 * ------------------------------------------------------------------
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
 */
package org.knime.base.node.preproc.groupby.dialogutil;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;

import org.knime.base.node.preproc.groupby.aggregation.AggregationMethod;

import java.util.List;

import javax.swing.JComboBox;

/**
 * This combo box is used in the aggregation column table to let the user
 * choose from the different compatible aggregation methods per aggregation
 * column.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class AggregationMethodComboBox extends JComboBox {

    private static final long serialVersionUID = -8712817491828316484L;

    private DataType m_type = null;

    /**
     * Creates AggregationMethod selection combo box with the allowed
     * methods.
     */
    public AggregationMethodComboBox() {
        super();
        this.setBackground(this.getBackground());
        this.setRenderer(new AggregationMethodListCellRenderer());
    }

    /**
     * @param spec the {@link DataColumnSpec} used to initialize this combobox
     * @param selectedMethod the current selected method
     */
    public void update(final DataColumnSpec spec,
            final AggregationMethod selectedMethod) {
        if (m_type == null || !m_type.equals(spec.getType())) {
            //recreate the combo box if the type has change
            removeAllItems();
            final List<AggregationMethod> compatibleMethods =
                AggregationMethod.getCompatibleMethods(spec);
            for (final AggregationMethod method : compatibleMethods) {
                addItem(method);
            }
            //save the current type for comparison
            m_type = spec.getType();
        }
        //select the previous selected item
        setSelectedItem(selectedMethod);
    }

    /**
     * @return the selected {@link AggregationMethod}
     */
    public AggregationMethod getSelectedMethod() {
        return (AggregationMethod)getSelectedItem();
    }

}
