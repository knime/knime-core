/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * --------------------------------------------------------------------- *
 * History
 *   14.09.2004 (ohl): created
 */
package org.knime.base.node.viz.plotter2D;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.JComponent;

import org.knime.base.util.coordinate.Coordinate;
import org.knime.base.util.coordinate.CoordinateMapping;

/**
 * Implements a header for a scrollable area. Can be used as horizontal (row) or
 * vertical (columns) header. It tries to find "useful" increments between the
 * ticks. There are various parameters you can play with to get a nice display -
 * but I admit it's hard and only sometimes successful...
 * 
 * @author ohl, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 */
public class Header extends JComponent {

    /**
     * Constant for the horizontal enumeration value.
     */
    public static final int HORIZONTAL = 0;

    /**
     * Constant for the vertical enumeration value.
     */
    public static final int VERTICAL = 1;

    /**
     * the "thickness" of the header.
     */
    public static final int SIZE = 50;

    /** the length of the tick line. */
    private static final int TICKLENGTH = 7;

    // /** the length of the label. */
    // private static final int NUM_CHARS = 8;

    // /** the pattern for the DecimalFormat class. */
    // private static final DecimalFormat EXP_FORMATTER = new DecimalFormat(
    // "0.00E0");

    /** the size of the font we draw with. */
    private static final int FONTSIZE = 10;

    /** the font used to label the ruler. */
    private static final Font RULER_FONT = new Font("SansSerif", Font.PLAIN,
            FONTSIZE);

    /** the offset between adjacent labels in horizontal headers. */
    private static final int HORIZ_OFFSET = FONTSIZE;

    // /** internal constants. */
    // private static final double LOW = -1.0 * Math.pow(10, NUM_CHARS - 1);
    //
    // private static final double HIGH = 1.0 * Math.pow(10, NUM_CHARS);
    //
    // private static final double NEGMAX = -1.0 * Math.pow(10, -(NUM_CHARS -
    // 5));
    //
    // private static final double POSMIN = 1.0 * Math.pow(10, -(NUM_CHARS -
    // 4));

    /** holding the orientation of this instance. */
    private final boolean m_horizontal;

    /** the full length of the ruler (in pixels). */
    private int m_fullLength;

    /**
     * The underlying coordinate for this axis header.
     */
    private Coordinate m_coordinate;

    /**
     * This is the offset to start the ticks from. This is neccessary, i.e. for
     * the scatter plotter which would set half the current dotsize, which must
     * be painted below its actual value.
     */
    private int m_startTickOffset;

    /**
     * Creates a new ruler in either horizontal or vertical orientation.
     * 
     * @param orientation specifies the orientation of this instance. Use
     *            Header.HORIZONTAL, or Header.VERTICAL
     * @param length the initial entire length of the ruler in pixels.
     */
    public Header(final int orientation, final int length) {
        if ((orientation != HORIZONTAL) && (orientation != VERTICAL)) {
            throw new IllegalArgumentException("Argument 'orientation' must"
                    + " be either Header.HORIZONTAL or Header.VERTICAL.");
        }
        m_fullLength = length;

        m_horizontal = (orientation == HORIZONTAL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);

        if (g.getClipBounds() == null) {
            return;
        }

        int x = 0;
        int y = 0;
        int width;
        int height;
        if (m_horizontal) {
            width = m_fullLength;
            height = SIZE;
        } else {
            width = SIZE;
            height = m_fullLength;
        }
        // Do the ruler labels in a small font that's black.
        g.setFont(RULER_FONT);
        g.setColor(Color.black);

        // the length we draw the ruler in
        int length;
        if (m_horizontal) {
            length = width;
        } else {
            length = height;
        }
        if (length <= 5) {
            // no use in drawing in such a small area
            return;
        }

        // draw the axis
        if (m_horizontal) {
            g.drawLine(x, y + 2, x + width, y + 2);
        } else {
            g.drawLine(x + width - 1, y, x + width - 1, y + height);
        }

        // draw the ticks and the labels
        // the offset must be subtracted 2 times
        if (m_coordinate == null) {
            return;
        }
        CoordinateMapping[] mappings = m_coordinate.getTickPositions(
                m_fullLength - 2 * m_startTickOffset, true);

        // this offset is for the labeldrawing in the horizontal header
        // it puts the lables alternatively up and down to have more
        // space for the labeling
        boolean useOffset = false;

        // draw all ticks except the last
        for (int i = 0; i < mappings.length; i++) {
            CoordinateMapping mapping = mappings[i];

            // String label;
            // if (incr < (m_start / 1000.0)) {
            // // if it's too small to show with our limited precision,
            // // just print the increment
            // label = "+" + format(incr);
            // } else {
            // label = format(first);
            // }
            drawTick(g, (long)mapping.getMappingValue() + m_startTickOffset,
                    mapping.getDomainValueAsString(), useOffset);

            useOffset = !useOffset;
        }
    }

    // private String format(final double d) {
    //
    // if ((d <= LOW) || (d >= HIGH) || ((d > 0) && (d < POSMIN))
    // || ((d < 0) && (d > NEGMAX))) {
    // // if string rep of d needs more than NUM_CHARS digits:
    // return EXP_FORMATTER.format(d);
    // } else {
    // // use the normal string rep - but round it.
    // // TODO: do the right amount of rounding
    // return Double.toString(Math.round(d * 1000.0) / 1000.0);
    // }
    // }

    /*
     * draws a tick and writes a label at the corresponding place. Takes into
     * account the orientation of the header.
     */
    private void drawTick(final Graphics g, final long at, final String label,
            final boolean useOffset) {

        int width;
        int height;
        if (m_horizontal) {
            width = m_fullLength;
            height = SIZE;
        } else {
            width = SIZE;
            height = m_fullLength;
        }

        if (m_horizontal) {
            if (at <= width) {
                int x = (int)at;
                g.drawLine(x, 2, x, TICKLENGTH + 2);

                // for the label we adjust the coordinates
                int lablePixelLength = g.getFontMetrics().stringWidth(label);

                // place the lable in the midle of a tick
                x -= lablePixelLength / 2;

                // if this would mean that the lable is not displayed
                // at the left border set x to 0
                if (x < 0) {
                    x = 0;
                } else if (x + lablePixelLength > m_fullLength) {
                    // if the lable woulb be printed beyond the right border
                    x = m_fullLength - lablePixelLength - 1;
                }

                int labelY = height - TICKLENGTH - 3;
                if (useOffset) {
                    labelY -= HORIZ_OFFSET;
                }
                if (labelY < FONTSIZE) {
                    labelY = FONTSIZE;
                }
                g.drawString(label, x, labelY);
            }
        } else {
            if (at <= height) {
                // int y = (int) at;
                // have the origin sitting in the lower left

                int y = height - (int)at;

                g.drawLine(SIZE - TICKLENGTH - 1, y, SIZE - 1, y);

                // for the label we adjust the coordinates
                int lablePixelHeight = g.getFontMetrics().getHeight();

                // if this would mean that the lable is not displayed
                // at the left border set x to 0
                if (y < lablePixelHeight) {
                    y = lablePixelHeight;
                } else if (y + lablePixelHeight > m_fullLength) {
                    // if the lable woulb be printed beyond the border
                    // y = m_fullLength - lablePixelHeight - 1;
                }

                g.drawString(label, 1, y);
            }
        }
    }

    /*
     * Calculates the distance between the labels of the header. The distance is
     * the value between the numbers diplayed - not pixels; assuming that the
     * entire range (between start and end) will be displayed over length
     * pixels. The distance between labels will be then more than PIXDIST
     * pixels. The value returned will be either 1, .5, .25, or their The method
     * may return 0.0 if start equals end, or the length is less than PIXDIST!
     * 
     * 
     */
    // private double getTickDistance(final double start, final double end,
    // final int length) {
    // double min;
    // double max;
    // double sign;
    // if ((start == end) || (length < TICKDIST)) {
    // return 0.0;
    // }
    //
    // if (start < end) {
    // min = start;
    // max = end;
    // sign = 1.0;
    // } else {
    // min = end;
    // max = start;
    // sign = -1.0;
    // }
    //
    // // the min. increment to keep the minimum pixel distance.
    // double minIncr = (max - min) * TICKDIST / (double)length;
    //
    // // get the next pow10 greater than the minimum distance
    // double incr = Math
    // .pow(10, Math.floor(Math.log(minIncr) / Math.log(10)));
    //
    // // make sure we return .1eX, .25eX or .5eX
    // if (incr < minIncr) {
    // incr = incr * 2.5;
    // }
    // if (incr < minIncr) {
    // incr = incr * 2;
    // }
    // if (incr < minIncr) {
    // incr = incr * 2;
    // }
    // // now it should really be greater than minIncr. But you never know.
    // // incr = minIncr;
    // assert incr >= minIncr;
    //
    // return Math.round(sign * incr * 1000.0) / 1000.0;
    //
    // }
    /**
     * Sets the prefered size of the component. Depending on the orientation,
     * the parameter <code>l</code> will either specifiy the width or height.
     * The values set (start and end) will be evenly spreaded over the entire
     * length.
     * 
     * @param l the length of the ruler
     */
    public void setPreferredLength(final int l) {
        m_fullLength = l;
        if (m_horizontal) {
            setPreferredSize(new Dimension(l, SIZE));
        } else {
            setPreferredSize(new Dimension(SIZE, l));
        }
    }

    /**
     * Sets the underlying coordinate for this header.
     * 
     * @param coordinate the coordinate to set
     */
    public void setCoordinate(final Coordinate coordinate) {
        m_coordinate = coordinate;
        repaint();
    }

    /**
     * Set the offset to start the tick-painting from. This is neccessary, i.e.
     * for the scatter plotter which would set half the current dotsize, which
     * must be painted below its actual value.
     * 
     * @param dotSizeOffset the offset
     */
    public void setStartTickOffset(final int dotSizeOffset) {
        m_startTickOffset = dotSizeOffset;
    }

    /**
     * @return the underlying coordinate
     */
    public Coordinate getCoordinate() {
        return m_coordinate;
    }
}
