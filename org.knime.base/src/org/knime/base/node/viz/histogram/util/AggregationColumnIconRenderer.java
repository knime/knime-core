/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   16.03.2007 (koetter): created
 */
package org.knime.base.node.viz.histogram.util;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

/**
 * Constructs a new column values renderer with is specific Color.
 */
class AggregationColumnIconRenderer extends DefaultListCellRenderer {

    private static final long serialVersionUID = -3291637282301055630L;

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getListCellRendererComponent(final JList list,
            final Object value, final int index, final boolean isSelected,
            final boolean cellHasFocus) {
        if (list == null || value == null) {
            return super.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);
        }
        if (!(value instanceof AggregationColumnIcon)) {
            throw new IllegalArgumentException("No valid value object");
        }
        AggregationColumnIcon icon = (AggregationColumnIcon)value;
        Component comp = super.getListCellRendererComponent(list, value, index,
                isSelected, cellHasFocus);
        super.setIcon(icon);
        super.setText(icon.getText().toString());
        return comp;
    }
}
