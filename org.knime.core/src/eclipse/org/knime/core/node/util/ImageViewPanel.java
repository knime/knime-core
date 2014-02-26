/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.core.node.util;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/** Panel that displays a BufferedImage in its center. Different scaling options are available.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 2.8
 */
@SuppressWarnings("serial")
public class ImageViewPanel extends JPanel implements Scrollable {

    /** Scaling options to fit the image into the available panel area. */
    public enum ScaleType {
        /** Don't scale. 1:1. */
        None,
        /** Shrink image (keeping aspect ratio) so that it fits the available area. */
        ShrinkAsNeeded,
        /** Always scale to fit the available area (keeping aspect ratio). */
        ScaleToFit,
    }

    private BufferedImage m_image;
    private ScaleType m_scaleType;

    /** Panel with no current image and scaling {@link ScaleType#ShrinkAsNeeded}. */
    public ImageViewPanel() {
        this(null);
    }

    /** Panel displaying given image and scaling {@link ScaleType#ShrinkAsNeeded}.
     * @param image The image (may be null). */
    public ImageViewPanel(final BufferedImage image) {
        m_image = image;
        m_scaleType = ScaleType.ShrinkAsNeeded;
    }

    /** Set image to draw (or null) and repaint.
     * @param image The image
     */
    public void setImage(final BufferedImage image) {
        m_image = image;
        repaint();
    }

    /** Set scale type, not null.
     * @param scaleType ...
     */
    public void setScaleType(final ScaleType scaleType) {
        if (scaleType == null) {
            throw new IllegalArgumentException("Argument must not be null.");
        }
        m_scaleType = scaleType;
        repaint();
    }

    /** {@inheritDoc} */
    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet() || m_image == null) {
            return super.getPreferredSize();
        } else {
            return new Dimension(m_image.getWidth(), m_image.getHeight());
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        if (m_image != null) {
            Insets insets = getInsets();
            int x = insets.left;
            int y = insets.top;
            int panelWidth = getWidth() - x - insets.right;
            int panelHeight = getHeight() - y - insets.bottom;
            final Rectangle drawingRectangle = new Rectangle(x, y, panelWidth, panelHeight);
            drawInto(g, m_image, m_image.getWidth(), m_image.getHeight(), drawingRectangle, m_scaleType);
        }
    }

    /** Scales and draws the given image into the argument graphics object.
     * @param g to draw into.
     * @param image the image width, e.g. ((BufferedImage)image).getWidth();
     * @param imageWidth the image width, e.g. ((BufferedImage)image).getWidth();
     * @param imageHeight the image height, e.g. ((BufferedImage)image).getHeight();
     * @param drawingRectangle The rectangle in the graphics area to draw into
     * @param scaleType The scaling type
     */
    public static void drawInto(final Graphics g, final Image image, final int imageWidth,
        final int imageHeight, final Rectangle drawingRectangle, final ScaleType scaleType) {
        final int x = drawingRectangle.x;
        final int y = drawingRectangle.y;
        final int panelWidth = drawingRectangle.width;
        final int panelHeight = drawingRectangle.height;
        int drawWidth;
        int drawHeight;
        double scale;
        if (ScaleType.None.equals(scaleType)) {
            scale = 1.0;
        } else {
            scale = Math.max(imageWidth / (double)panelWidth, imageHeight / (double)panelHeight);
            if (scaleType.equals(ScaleType.ShrinkAsNeeded)) {
                scale = Math.max(scale, 1.0);
            }
        }
        if (scale == 1.0) {
            drawWidth = imageWidth;
            drawHeight = imageHeight;
        } else {
            drawWidth = (int)(imageWidth / scale);
            drawHeight = (int)(imageHeight / scale);
        }
        int placeX = Math.max(0, x + ((panelWidth - drawWidth) / 2));
        int placeY = Math.max(0, y + ((panelHeight - drawHeight) / 2));
        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }
        g.drawImage(image, placeX, placeY, drawWidth, drawHeight, null);
    }

    /** {@inheritDoc} */
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        switch (m_scaleType) {
            case None:
                return getPreferredSize();
            default:
                return getPreferredSize();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getScrollableUnitIncrement(final Rectangle visibleRect,
            final int orientation, final int direction) {
        return getScrollableBlockIncrement(visibleRect,
                orientation, direction) / 4;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getScrollableBlockIncrement(final Rectangle visibleRect,
            final int orientation, final int direction) {
        boolean vertical = SwingConstants.VERTICAL == orientation;
        if (vertical) {
            return visibleRect.height / 3;
        } else {
            return visibleRect.width / 3;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getScrollableTracksViewportWidth() {
        if (ScaleType.None.equals(m_scaleType)) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getScrollableTracksViewportHeight() {
        if (ScaleType.None.equals(m_scaleType)) {
            return false;
        }
        return true;
    }
}
