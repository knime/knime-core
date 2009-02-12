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
 *   02.08.2005 (bernd): created
 */
package org.knime.core.node.util;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;


/**
 * Renderer that checks if the value being rendered is of type
 * <code>StringIconEnumInterface</code> if so it will render the icon if
 * available and the label. If not, the passed value's toString() method
 * is used for rendering.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class StringIconListCellRenderer extends DefaultListCellRenderer {

    private static final long serialVersionUID = 6102484208185201127L;

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
        final Component c =  super.getListCellRendererComponent(list, value,
                index, isSelected, cellHasFocus);
        assert (c == this);
        if (value instanceof StringIconOption) {
            final StringIconOption stringIcon =
                (StringIconOption) value;
            setText(stringIcon.getText());
            if (stringIcon.getIcon() != null) {
                setIcon(stringIcon.getIcon());
            }
        }
        return this;
    }
}
