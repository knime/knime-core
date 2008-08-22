/*
 * ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 *
 * History
 *   30.07.2007 (thor): created
 */
package org.knime.base.node.preproc.joiner;

import java.awt.Component;
import java.awt.Font;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

import org.knime.core.data.DataColumnSpec;

/**
 * This class renders a list with {@link DataColumnSpec}s and strings. Column
 * specs get a nice icon describing the type in front of the column name,
 * string columns are rendered italic.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class ColumnSpecListRenderer extends DefaultListCellRenderer {
    private static final Font RK_FONT;

    private static final Font CS_FONT;

    static {
        JLabel l = new JLabel();
        RK_FONT =
                new Font(l.getFont().getName(), Font.ITALIC, l.getFont()
                        .getSize());
        CS_FONT = l.getFont();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getListCellRendererComponent(final JList list,
            final Object value, final int index, final boolean isSelected,
            final boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index,
                        isSelected, cellHasFocus);

        if (value instanceof String) { // this is the Row Key
            setFont(RK_FONT);
            setText(value.toString());
        } else if (value instanceof DataColumnSpec) {
            setFont(CS_FONT);
            setText(((DataColumnSpec)value).getName());
            setIcon(((DataColumnSpec)value).getType().getIcon());
        } else {
            setFont(CS_FONT);
            setText(value.toString());
        }

        return this;
    }
}
