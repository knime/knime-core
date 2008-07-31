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
 * -------------------------------------------------------------------
 * 
 * History
 *   21.08.2006 (Fabian Dill): created
 */
package org.knime.core.node.util;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Insets;

import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class ConvenientComboBoxRenderer extends BasicComboBoxRenderer {
    /**
     * {@inheritDoc}
     */
    @Override
    public Component getListCellRendererComponent(final JList list,
            final Object value, final int index, final boolean isSelected,
            final boolean cellHasFocus) {
        if ((index > -1) && (value != null)) {
            list.setToolTipText(value.toString());
        } else {
            list.setToolTipText(null);
        }
        return super.getListCellRendererComponent(list, value, index,
                isSelected, cellHasFocus);
    }

    /**
     * Does the clipping automatically, clips off characters from the middle
     * of the string.
     * 
     * @see BasicComboBoxRenderer#getText()
     */
    @Override
    public String getText() {
        Insets ins = getInsets();
        int width = getWidth() - ins.left - ins.right;
        String s = super.getText();
        FontMetrics fm = getFontMetrics(getFont());
        String clipped = s;
        while (clipped.length() > 5 && fm.stringWidth(clipped) > width) {
            clipped = StringFormat.formatPath(s, clipped.length() - 3);
        }
        return clipped;
    }

}
