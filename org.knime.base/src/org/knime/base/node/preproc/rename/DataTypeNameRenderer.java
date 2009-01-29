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
 * History
 *   Feb 2, 2006 (wiswedel): created
 */
package org.knime.base.node.preproc.rename;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.UIManager;

import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValue.UtilityFactory;


/**
 * Renderer used in the combo box where the user can pick different
 * {@link org.knime.core.data.DataValue} class. This renderer will show the
 * name of the {@link org.knime.core.data.DataValue} class along with the
 * icon assigned to the value class as defined by the
 * {@link DataType#getUtilityFor(Class)} method.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class DataTypeNameRenderer extends DefaultListCellRenderer {
    /**
     * {@inheritDoc}
     */
    @Override
    public Component getListCellRendererComponent(final JList list,
            final Object value, final int index, final boolean isSelected,
            final boolean cellHasFocus) {
        /* Almost all has been copied from the super implementation */
        setComponentOrientation(list.getComponentOrientation());
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        if (value instanceof Class
                && DataValue.class.isAssignableFrom((Class)value)) {
            Class<? extends DataValue> type = (Class<? extends DataValue>)value;
            String s = type.getName();
            int dot = s.lastIndexOf('.');
            if (dot >= 0 && dot < s.length() - 1) {
                s = s.substring(dot + 1);
            }
            s = s.trim();
            UtilityFactory fac = DataType.getUtilityFor(type);
            Icon icon = fac.getIcon();
            setIcon(icon);
            setText(s);
        } else {
            if (value instanceof Icon) {
                setIcon((Icon)value);
                setText("");
            } else {
                setIcon(null);
                setText((value == null) ? "" : value.toString());
            }
        }

        setEnabled(list.isEnabled());
        setFont(list.getFont());
        setBorder((cellHasFocus) ? UIManager
                .getBorder("List.focusCellHighlightBorder") : noFocusBorder);
        return this;
    }
}
