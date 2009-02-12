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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   11.10.2006 (gabriel): created
 */
package org.knime.core.data.property;

import javax.swing.table.DefaultTableCellRenderer;

import org.knime.core.data.property.ShapeFactory.Shape;

/**
 * Overrides the <code>DefaultTableCellRenderer</code> and sets icon and
 * text to display the Shape properties.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class ShapeSelectionComboBoxRenderer extends DefaultTableCellRenderer {
    
    private final ShapeSelectionComboBox m_combo = new ShapeSelectionComboBox();

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setValue(final Object value) {
        m_combo.setSelectedItem(value);
        Shape shape = (Shape) value;
        super.setIcon(shape.getIcon());
        super.setText(value.toString());
    }
    
}
