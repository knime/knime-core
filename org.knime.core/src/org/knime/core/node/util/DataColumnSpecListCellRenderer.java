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
 *   02.08.2005 (bernd): created
 */
package org.knime.core.node.util;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import org.knime.core.data.DataColumnSpec;


/**
 * Renderer that checks if the value being renderer is of type 
 * <code>DataColumnSpec</code> if so it will renderer the name of the column
 * spec and also the type's icon. If not, the passed value's toString() method
 * is used for rendering.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class DataColumnSpecListCellRenderer extends DefaultListCellRenderer {
    private static final long serialVersionUID = 1156595670217009312L;

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getListCellRendererComponent(
            final JList list, final Object value, final int index, 
            final boolean isSelected, final boolean cellHasFocus) {
        // The super method will reset the icon if we call this method 
        // last. So we let super do its job first and then we take care
        // that everything is properly set.
        Component c =  super.getListCellRendererComponent(list, value, index,
                isSelected, cellHasFocus);
        assert (c == this);
        if (value instanceof DataColumnSpec) {
            setText(((DataColumnSpec)value).getName().toString());
            setIcon(((DataColumnSpec)value).getType().getIcon());
        }
        return this;
    }
}
