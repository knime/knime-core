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
 *   13.11.2006 (sieb): created
 */
package org.knime.base.node.preproc.discretization.caim2.modelcreator;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;

import org.knime.base.node.viz.plotter.AbstractDrawingPane;

/**
 * The actual drawing pain, rendering the binned column model. The binned
 * columns are rendered as horizontal lines with ticks at the bin boarders.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class BinModelDrawingPane extends AbstractDrawingPane {

    private static final int TICK_SIZE = 4;

    private int m_hMargin = 50;

    private BinRuler[] m_binningSchemes;

    /**
     * Sets the background color to white.
     */
    public BinModelDrawingPane() {
        super.setBackground(Color.white);
    }

    /**
     * Sets the horizontal margin.
     * 
     * @param hMargin sets the horizontal margin
     */
    public void setHorizontalMargin(final int hMargin) {
        m_hMargin = hMargin;
        assert m_hMargin == hMargin;
    }

    /**
     * Sets the binning schemes for all bins that schould be painted.
     * 
     * @param schemes sets the binning schemes for all bins that schould be
     *            painted.
     */
    public void setBinningSchemes(final BinRuler[] schemes) {

        m_binningSchemes = schemes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintContent(final Graphics g) {
        if (m_binningSchemes == null) {
            return;
        }
        // set text antialias on
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // set the preferred size to the needed height
        int width = getPreferredSize().width;
        int height =
                m_binningSchemes[m_binningSchemes.length - 1]
                        .getLeftStartPoint().y + 40;
        setPreferredSize(new Dimension(width, height));

        if (m_binningSchemes == null) {
            return;
        }

        for (BinRuler scheme : m_binningSchemes) {

            // first draw the line of the ruler
            int x = scheme.getLeftStartPoint().x;
            int y = scheme.getLeftStartPoint().y;
            int xR = x + scheme.getWidth();
            g.drawLine(x, y, xR, y);

            // then draw the ticks...
            int[] tickXPositions = scheme.getBinPositions();
            for (int tickXPos : tickXPositions) {
                g.drawLine(tickXPos + m_hMargin, y, tickXPos + m_hMargin, y
                        + TICK_SIZE);
            }

            // ...and the corresponding labels
            String[] labels = scheme.getBinPosValue();
            int yPos = y + TICK_SIZE + 12;
            g.drawString(labels[0], x, yPos);
            for (int i = 1; i < labels.length; i++) {
                int fontHeight = g.getFontMetrics().getHeight();
                int yPosAlternate = yPos + (i % 2 * fontHeight);

                int tickX;
                // if this is the last tick
                if (i == labels.length - 1) {
                    tickX =
                            tickXPositions[i]
                                    - g.getFontMetrics().stringWidth(labels[i]);
                } else {
                    int fontWidthHalf =
                            g.getFontMetrics().stringWidth(labels[i]) / 2;
                    tickX = tickXPositions[i] - fontWidthHalf;
                }
                g.drawString(labels[i], tickX + m_hMargin, yPosAlternate);
            }
            // draw the column name above the ruler
            Font tempFont = g.getFont();
            g.setFont(new Font("arial", Font.BOLD, 12));
            g.drawString(scheme.getName(), x, y
                    - g.getFontMetrics().getHeight());
            g.setFont(tempFont);
        }


    }


    /**
     * No tool tips yet.
     * 
     * @see javax.swing.JComponent#getToolTipText(java.awt.event.MouseEvent)
     */
    @Override
    public String getToolTipText(final MouseEvent e) {

        // no tool tips yet
        return null;
    }

}
