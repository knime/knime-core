/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *   Dec 21, 2006 (wiswedel): created
 */
package org.knime.core.data.renderer;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicLabelUI;

/**
 * Label UI that respects the linebreaks in the label to be rendered. This
 * UI does not support an icon or respects in any kind the alignment of the
 * label.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class MultiLineBasicLabelUI extends BasicLabelUI {

    private boolean m_isWrapLongLines = false;

    /** Delegates to {@link #getPreferredSize(JComponent)}.
     * @see BasicLabelUI#getMaximumSize(JComponent)
     */
    @Override
    public Dimension getMaximumSize(final JComponent c) {
        return getPreferredSize(c);
    }

    /** Enable floating text, line breaks are added as needed.
     * @param value to set (default is false)
     * @since 2.8
     */
    public void setWrapLongLines(final boolean value) {
        m_isWrapLongLines = value;
    }

    /** Delegates to {@link #getPreferredSize(JComponent)}.
     * @see BasicLabelUI#getMinimumSize(JComponent)
     */
    @Override
    public Dimension getMinimumSize(final JComponent c) {
        return getPreferredSize(c);
    }

    /* These rectangles/insets are allocated once for this shared LabelUI
     * implementation.  Re-using rectangles rather than allocating
     * them in each getPreferredSize call sped up the method substantially.
     */
    private Insets m_viewInsets = new Insets(0, 0, 0, 0);
    private Rectangle m_paintIconR = new Rectangle();
    private Rectangle m_paintTextR = new Rectangle();
    private Rectangle m_paintViewR = new Rectangle();
    private String m_string;
    private String[] m_splittedStrings;

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getPreferredSize(final JComponent c) {
        JLabel label = (JLabel)c;
        String text = label.getText();
        Insets insets = label.getInsets(m_viewInsets);
        Icon icon = label.isEnabled() ? label.getIcon() : label.getDisabledIcon();
        Font font = label.getFont();

        int dx = insets.left + insets.right;
        int dy = insets.top + insets.bottom;

        if ((text == null) || (font == null)) {
            int width = dx;
            int height = dy;
            if (icon != null) {
                width += icon.getIconWidth();
                height += icon.getIconHeight();
            }
            return new Dimension(width, height);
        } else {
            FontMetrics fm = label.getFontMetrics(font);
            String[] splittedString = splitStrings(text);
            int height = splittedString.length * fm.getHeight();
            int width = 0;
            for (String part : splittedString) {
                int curWidth = SwingUtilities.computeStringWidth(fm, part);
                width = Math.max(width, curWidth);
            }
            if (icon != null) {
                width += icon.getIconWidth() + label.getIconTextGap();
                height = Math.max(height, icon.getIconHeight());
            }
            width += dx;
            height += dy;
            return new Dimension(width, height);
        }
    }

    /** Get the width of the text. The size of {@link #getPreferredSize(JComponent)}, which is occupied by text
     * (no icon, no icon-text gap, no insets).
     * @param c ...
     * @return ...
     * @since 2.8
     * @noreference This method is not intended to be referenced by clients.
     */
    public final int getPreferredTextWidth(final JComponent c) {
        JLabel label = (JLabel)c;
        String text = label.getText();
        Font font = label.getFont();
        if (text == null || font == null) {
            return 0;
        }
        FontMetrics fm = label.getFontMetrics(font);
        String[] splittedString = splitStrings(text);
        int width = 0;
        for (String part : splittedString) {
            int curWidth = SwingUtilities.computeStringWidth(fm, part);
            width = Math.max(width, curWidth);
        }
        return width;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paint(final Graphics g, final JComponent c) {
        JLabel label = (JLabel)c;
        String text = label.getText();
        Icon icon = label.isEnabled() ? label.getIcon() : label.getDisabledIcon();

        if (text == null && icon == null) {
            return;
        }

        FontMetrics fm = label.getFontMetrics(label.getFont());
        Insets insets = c.getInsets(m_viewInsets);

        m_paintViewR.x = insets.left;
        m_paintViewR.y = insets.top;
        m_paintViewR.width = c.getWidth() - (insets.left + insets.right);
        m_paintViewR.height = c.getHeight() - (insets.top + insets.bottom);

        // all assigments only temporary - modified by layoutCL down below
        m_paintIconR.x = 0;
        m_paintIconR.y = 0;
        m_paintIconR.width = icon != null ? icon.getIconWidth() : 0;
        m_paintIconR.height = icon != null ? icon.getIconHeight() : 0;
        m_paintTextR.x = 0;
        m_paintTextR.y = 0;
        int maxTextWidth = m_paintViewR.width - (icon != null ? icon.getIconWidth() + label.getIconTextGap() : 0);
        m_paintTextR.width = maxTextWidth;
        m_paintTextR.height = m_paintViewR.height;

        String[] splits = splitStrings(text);
        if (icon != null) {
            icon.paintIcon(c, g, m_paintViewR.x, m_paintViewR.y);
        }
        int yOffset = m_paintViewR.y + fm.getAscent();
        for (int i = 0; i < splits.length; i++) {
            String currentLine = splits[i];
            String s;
            int startChar = 0;
            boolean oneMoreIteration;
            do {
                if (m_isWrapLongLines) {
                    // is there enough space for another row after drawing this line?
                    if (yOffset + fm.getHeight() <= m_paintViewR.height) {
                        // cut text and avoid clipping ("...")
                        int end = autoSplit(currentLine, fm, startChar, Math.max(maxTextWidth, 0));
                        s = currentLine.substring(startChar, end);
                        startChar = end;
                        oneMoreIteration = startChar < currentLine.length();
                    } else {
                        // take remainder of text, will likely clip and add '...'
                        s = currentLine.substring(startChar, currentLine.length());
                        oneMoreIteration = false;
                    }
                } else {
                    s = currentLine;
                    oneMoreIteration = false;
                }
                // this modifies the rectangle arguments
                String clippedText = layoutCL(label, fm, s, icon, m_paintViewR, m_paintIconR, m_paintTextR);
                int textX = m_paintTextR.x;
                int textY = yOffset;

                if (label.isEnabled()) {
                    paintEnabledText(label, g, clippedText, textX, textY);
                } else {
                    paintDisabledText(label, g, clippedText, textX, textY);
                }
                yOffset += fm.getHeight();
            } while (oneMoreIteration);
        }
    }

    private String[] splitStrings(final String s) {
        if (s == null) {
            return new String[]{};
        }
        if (!s.equals(m_string)) {
            m_splittedStrings = s.split("\n");
            m_string = s;
        }
        return m_splittedStrings;
    }

    /** Determines split position in string argument so that the first half can be represented in less than maxWidth
     * pixels. Does some magic to find good line breaks
     * @param s The string
     * @param fm The font
     * @param startIndex Where to start (called in a loop)
     * @param maxWidth The text area max width
     * @return the end index of the sub string that can be fully drawn in a line.
     */
    private int autoSplit(final String s, final FontMetrics fm, final int startIndex, final int maxWidth) {
        final int sEnd = s.length();
        int newEnd = sEnd;
        // This is the naive way. Serious performance hit when run on 20k columns with long names (and then resizing)
        // the column header.
        /* while (SwingUtilities.computeStringWidth(fm, s.substring(startIndex, newEnd)) > maxWidth) {
         *     newEnd -= 1;
         * } */

        // some pseudo binary search to find a good index.
        for (int step = (newEnd - startIndex) / 2; step > 0; step = step / 2) {
            if (SwingUtilities.computeStringWidth(fm, s.substring(startIndex, newEnd)) < maxWidth) {
                if (newEnd >= sEnd) {
                    return sEnd;
                }
                newEnd = Math.min(sEnd, newEnd + step);
            } else {
                newEnd = Math.max(startIndex, newEnd - step);
            }
        }
        // above for loop ended with modifying "newEnd". Bound it again.
        while (SwingUtilities.computeStringWidth(fm, s.substring(startIndex, newEnd)) > maxWidth) {
            newEnd = Math.max(startIndex, newEnd - 1);
        }
        // if there is a space char near the calculated split point use that one instead
        if (newEnd - startIndex > 6) {
            for (int i = newEnd - 1; i > newEnd - 3; i--) {
                switch (s.charAt(i)) {
                    case ' ':
                    case '\t':
                        newEnd = i;
                        break;
                    default:
                }
            }
        }
        return Math.max(startIndex + 1, newEnd);
    }

}
