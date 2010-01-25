/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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

    /** Delegates to {@link #getPreferredSize(JComponent)}.
     * @see BasicLabelUI#getMaximumSize(JComponent)
     */
    @Override
    public Dimension getMaximumSize(final JComponent c) {
        return getPreferredSize(c);
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
        Font font = label.getFont();

        int dx = insets.left + insets.right;
        int dy = insets.top + insets.bottom;

        if ((text == null) || ((text != null) && (font == null))) {
                return new Dimension(dx, dy);
        } else {
            FontMetrics fm = label.getFontMetrics(font);
            String[] splittedString = splitStrings(text);
            int height = dy + splittedString.length * fm.getHeight();
            int width = 0;
            for (String part : splittedString) {
                int curWidth = SwingUtilities.computeStringWidth(fm, part);
                width = Math.max(width, curWidth);
            }
            width += dx;
            return new Dimension(width, height);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paint(final Graphics g, final JComponent c) {
        JLabel label = (JLabel)c;
        String text = label.getText();

        if (text == null) {
            return;
        }

        FontMetrics fm = label.getFontMetrics(label.getFont()); 
        Insets insets = c.getInsets(m_viewInsets);

        m_paintViewR.x = insets.left;
        m_paintViewR.y = insets.top;
        m_paintViewR.width = c.getWidth() - (insets.left + insets.right);
        m_paintViewR.height = c.getHeight() - (insets.top + insets.bottom);

        m_paintIconR.x = 0;
        m_paintIconR.y = 0;
        m_paintIconR.width = 0;
        m_paintIconR.height = 0;
        m_paintTextR.x = 0;
        m_paintTextR.y = 0;
        m_paintTextR.width = 0;
        m_paintTextR.height = 0;
        
        String[] splits = splitStrings(text);
        for (int i = 0; i < splits.length; i++) {
            String s = splits[i];
            String clippedText = layoutCL(label, fm, s, null, m_paintViewR,
                    m_paintIconR, m_paintTextR);
            int textX = m_paintTextR.x;
            int textY = m_paintTextR.y + fm.getAscent() + i * fm.getHeight();

            if (label.isEnabled()) {
                paintEnabledText(label, g, clippedText, textX, textY);
            } else {
                paintDisabledText(label, g, clippedText, textX, textY);
            }
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

}
