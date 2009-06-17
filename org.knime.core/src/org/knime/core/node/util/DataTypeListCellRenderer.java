/*
 * -------------------------------------------------------------------
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
package org.knime.core.node.util;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import org.knime.core.data.DataType;


/**
 * Renderer that checks if the value being renderer is a
 * <code>DataType</code> to render the name of the type and
 * its icon. If not, the passed value's toString() method
 * is used for rendering.
 * @author Nicolas Cebron, University of Konstanz
 */
public class DataTypeListCellRenderer extends DefaultListCellRenderer {

    private static final long serialVersionUID = -3238164216976500254L;

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
        if (value instanceof DataType) {
            setText(((DataType)value).toString());
            setIcon(((DataType)value).getIcon());
        }
        return this;
    }
}
