/* Created on Dec 6, 2006 3:18:53 PM by thor
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 * ------------------------------------------------------------------- * 
 */
package org.knime.core.data.renderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicLabelUI;

/**
 * This code is taken from <a
 * href="http://www.codeguru.com/java/articles/198.shtml">
 * http://www.codeguru.com/java/articles/198.shtml</a>.
 * It is a layouter for {@link JLabel}s that prints multiple lines.
 * 
 * @author Zafir Anjum
 */
public class MultiLineLabelUI extends BasicLabelUI {
    static {
        labelUI = new MultiLineLabelUI();
    }

    private static Dimension computeMultiLineDimension(final FontMetrics fm,
            final String[] strs) {
        int i, c, width = 0;
        for (i = 0, c = strs.length; i < c; i++) {
            width =
                    Math.max(width, SwingUtilities.computeStringWidth(fm,
                            strs[i]));
        }
        return new Dimension(width, fm.getHeight() * strs.length);
    }

    /**
     * Compute and return the location of the icons origin, the location of
     * origin of the text baseline, and a possibly clipped version of the
     * compound labels string. Locations are computed relative to the viewR
     * rectangle. This layoutCompoundLabel() does not know how to handle
     * LEADING/TRAILING values in horizontalTextPosition (they will default to
     * RIGHT) and in horizontalAlignment (they will default to CENTER). Use the
     * other version of layoutCompoundLabel() instead.
     */
    private static String layoutCompoundLabel(final FontMetrics fm,
            final String[] text, final Icon icon, final int verticalAlignment,
            final int horizontalAlignment, final int verticalTextPosition,
            final int horizontalTextPosition, final Rectangle viewR,
            final Rectangle iconR, final Rectangle textR, final int textIconGap) {
        /*
         * Initialize the icon bounds rectangle iconR.
         */

        if (icon != null) {
            iconR.width = icon.getIconWidth();
            iconR.height = icon.getIconHeight();
        } else {
            iconR.width = 0;
            iconR.height = 0;
        }

        /*
         * Initialize the text bounds rectangle textR. If a null or and empty
         * String was specified we substitute "" here and use 0,0,0,0 for textR.
         */

        // Fix for textIsEmpty sent by Paulo Santos
        boolean textIsEmpty =
                (text == null)
                        || (text.length == 0)
                        || (text.length == 1 && ((text[0] == null) || text[0]
                                .equals("")));

        String rettext = "";
        if (textIsEmpty) {
            textR.width = 0;
            textR.height = 0;
        } else {
            Dimension dim = computeMultiLineDimension(fm, text);
            textR.width = dim.width;
            textR.height = dim.height;
        }

        /*
         * Unless both text and icon are non-null, we effectively ignore the
         * value of textIconGap. The code that follows uses the value of gap
         * instead of textIconGap.
         */

        int gap = (textIsEmpty || (icon == null)) ? 0 : textIconGap;

        if (!textIsEmpty) {

            /*
             * If the label text string is too wide to fit within the available
             * space "..." and as many characters as will fit will be displayed
             * instead.
             */

            int availTextWidth;

            if (horizontalTextPosition == SwingConstants.CENTER) {
                availTextWidth = viewR.width;
            } else {
                availTextWidth = viewR.width - (iconR.width + gap);
            }

            if (textR.width > availTextWidth && text.length == 1) {
                String clipString = "...";
                int totalWidth =
                        SwingUtilities.computeStringWidth(fm, clipString);
                int nChars;
                for (nChars = 0; nChars < text[0].length(); nChars++) {
                    totalWidth += fm.charWidth(text[0].charAt(nChars));
                    if (totalWidth > availTextWidth) {
                        break;
                    }
                }
                rettext = text[0].substring(0, nChars) + clipString;
                textR.width = SwingUtilities.computeStringWidth(fm, rettext);
            }
        }

        /*
         * Compute textR.x,y given the verticalTextPosition and
         * horizontalTextPosition properties
         */

        if (verticalTextPosition == SwingConstants.TOP) {
            if (horizontalTextPosition != SwingConstants.CENTER) {
                textR.y = 0;
            } else {
                textR.y = -(textR.height + gap);
            }
        } else if (verticalTextPosition == SwingConstants.CENTER) {
            textR.y = (iconR.height / 2) - (textR.height / 2);
        } else { // (verticalTextPosition == BOTTOM)
            if (horizontalTextPosition != SwingConstants.CENTER) {
                textR.y = iconR.height - textR.height;
            } else {
                textR.y = (iconR.height + gap);
            }
        }

        if (horizontalTextPosition == SwingConstants.LEFT) {
            textR.x = -(textR.width + gap);
        } else if (horizontalTextPosition == SwingConstants.CENTER) {
            textR.x = (iconR.width / 2) - (textR.width / 2);
        } else { // (horizontalTextPosition == RIGHT)
            textR.x = (iconR.width + gap);
        }

        /*
         * labelR is the rectangle that contains iconR and textR. Move it to its
         * proper position given the labelAlignment properties.
         * 
         * To avoid actually allocating a Rectangle, Rectangle.union has been
         * inlined below.
         */
        int labelRX = Math.min(iconR.x, textR.x);
        int labelRWidth =
                Math.max(iconR.x + iconR.width, textR.x + textR.width)
                        - labelRX;
        int labelRY = Math.min(iconR.y, textR.y);
        int labelRHeight =
                Math.max(iconR.y + iconR.height, textR.y + textR.height)
                        - labelRY;

        int dx, dy;

        if (verticalAlignment == SwingConstants.TOP) {
            dy = viewR.y - labelRY;
        } else if (verticalAlignment == SwingConstants.CENTER) {
            dy =
                    (viewR.y + (viewR.height / 2))
                            - (labelRY + (labelRHeight / 2));
        } else { // (verticalAlignment == BOTTOM)
            dy = (viewR.y + viewR.height) - (labelRY + labelRHeight);
        }

        if (horizontalAlignment == SwingConstants.LEFT) {
            dx = viewR.x - labelRX;
        } else if (horizontalAlignment == SwingConstants.RIGHT) {
            dx = (viewR.x + viewR.width) - (labelRX + labelRWidth);
        } else { // (horizontalAlignment == CENTER)
            dx = (viewR.x + (viewR.width / 2)) - (labelRX + (labelRWidth / 2));
        }

        /*
         * Translate textR and glypyR by dx,dy.
         */

        textR.x += dx;
        textR.y += dy;

        iconR.x += dx;
        iconR.y += dy;

        return rettext;
    }

    /**
     * Compute and return the location of the icons origin, the location of
     * origin of the text baseline, and a possibly clipped version of the
     * compound labels string. Locations are computed relative to the viewR
     * rectangle. The JComponents orientation (LEADING/TRAILING) will also be
     * taken into account and translated into LEFT/RIGHT values accordingly.
     */
    private static String layoutCompoundLabel(final JComponent c,
            final FontMetrics fm, final String[] text, final Icon icon,
            final int verticalAlignment, final int horizontalAlignment,
            final int verticalTextPosition, final int horizontalTextPosition,
            final Rectangle viewR, final Rectangle iconR,
            final Rectangle textR, final int textIconGap) {
        boolean orientationIsLeftToRight = true;
        int hAlign = horizontalAlignment;
        int hTextPos = horizontalTextPosition;

        if (c != null) {
            if (!(c.getComponentOrientation().isLeftToRight())) {
                orientationIsLeftToRight = false;
            }
        }

        // Translate LEADING/TRAILING values in horizontalAlignment
        // to LEFT/RIGHT values depending on the components orientation
        switch (horizontalAlignment) {
        case SwingConstants.LEADING:
            hAlign =
                    (orientationIsLeftToRight) ? SwingConstants.LEFT
                            : SwingConstants.RIGHT;
            break;
        case SwingConstants.TRAILING:
            hAlign =
                    (orientationIsLeftToRight) ? SwingConstants.RIGHT
                            : SwingConstants.LEFT;
            break;
        }

        // Translate LEADING/TRAILING values in horizontalTextPosition
        // to LEFT/RIGHT values depending on the components orientation
        switch (horizontalTextPosition) {
        case SwingConstants.LEADING:
            hTextPos =
                    (orientationIsLeftToRight) ? SwingConstants.LEFT
                            : SwingConstants.RIGHT;
            break;
        case SwingConstants.TRAILING:
            hTextPos =
                    (orientationIsLeftToRight) ? SwingConstants.RIGHT
                            : SwingConstants.LEFT;
            break;
        }

        return layoutCompoundLabel(fm, text, icon, verticalAlignment, hAlign,
                verticalTextPosition, hTextPos, viewR, iconR, textR,
                textIconGap);
    }

    private String m_str;

    private String[] m_strs;

    private void drawString(final Graphics g, final String s,
            final int accChar, final int textX, final int textY) {
        if (s.indexOf('\n') == -1) {
            BasicGraphicsUtils.drawString(g, s, accChar, textX, textY);
        } else {
            String[] strs = splitStringByLines(s);
            int height = g.getFontMetrics().getHeight();
            // Only the first line can have the accel char
            BasicGraphicsUtils.drawString(g, strs[0], accChar, textX, textY);
            for (int i = 1; i < strs.length; i++) {
                g.drawString(strs[i], textX, textY + (height * i));
            }
        }
    }

    /**
     * @see javax.swing.plaf.basic.BasicLabelUI#layoutCL(javax.swing.JLabel,
     *      java.awt.FontMetrics, java.lang.String, javax.swing.Icon,
     *      java.awt.Rectangle, java.awt.Rectangle, java.awt.Rectangle)
     */
    @Override
    protected String layoutCL(final JLabel label,
            final FontMetrics fontMetrics, final String text, final Icon icon,
            final Rectangle viewR, final Rectangle iconR, final Rectangle textR) {
        String s =
                layoutCompoundLabel(label, fontMetrics,
                        splitStringByLines(text), icon, label
                                .getVerticalAlignment(), label
                                .getHorizontalAlignment(), label
                                .getVerticalTextPosition(), label
                                .getHorizontalTextPosition(), viewR, iconR,
                        textR, label.getIconTextGap());

        if (s.equals("")) {
            return text;
        }
        return s;
    }

    /**
     * @see javax.swing.plaf.basic.BasicLabelUI
     *      #paintDisabledText(javax.swing.JLabel, java.awt.Graphics,
     *      java.lang.String, int, int)
     */
    @Override
    protected void paintDisabledText(final JLabel l, final Graphics g,
            final String s, final int textX, final int textY) {
        int accChar = l.getDisplayedMnemonicIndex();
        Color background = l.getBackground();
        g.setColor(background.brighter());
        drawString(g, s, accChar, textX + 1, textY + 1);

        g.setColor(background.darker());
        drawString(g, s, accChar, textX, textY);
    }

    /**
     * @see javax.swing.plaf.basic.BasicLabelUI
     *      #paintEnabledText(javax.swing.JLabel, java.awt.Graphics,
     *      java.lang.String, int, int)
     */
    @Override
    protected void paintEnabledText(final JLabel l, final Graphics g,
            final String s, final int textX, final int textY) {
        int mnemIndex = l.getDisplayedMnemonicIndex();
        g.setColor(l.getForeground());
        drawString(g, s, mnemIndex, textX, textY);
    }

    private String[] splitStringByLines(final String str) {
        if (str.equals(m_str)) {
            return m_strs;
        }

        m_strs = str.split("\n");
        return m_strs;
    }
}
