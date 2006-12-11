/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
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
     * @see BasicComboBoxRenderer#getListCellRendererComponent(
     *      javax.swing.JList, java.lang.Object, int, boolean, boolean)
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
            clipped = format(s, clipped.length() - 3);
        }
        return clipped;
    }

    /*
     * builds strings with the following pattern: if size is smaller than
     * 30, return the last 30 chars in the string; if the size is larger
     * than 30: return the first 12 chars + ... + chars from the end. Size
     * more than 55: the first 28 + ... + rest from the end.
     */
    private String format(final String str, final int size) {
        String result;
        if (str.length() <= size) {
            // short enough - return it unchanged
            return str;
        }
        if (size <= 30) {
            result = "..."
                    + str.substring(str.length() - size + 3, str.length());
        } else if (size <= 55) {
            result = str.substring(0, 12)
                    + "..."
                    + str.subSequence(str.length() - size + 15, str
                            .length());
        } else {
            result = str.substring(0, 28)
                    + "..."
                    + str.subSequence(str.length() - size + 31, str
                            .length());
        }
        return result;
    }
}
