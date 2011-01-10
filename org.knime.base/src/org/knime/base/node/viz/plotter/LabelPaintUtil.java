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
 * -------------------------------------------------------------------
 *
 * History
 *   08.10.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.List;

import org.knime.base.util.coordinate.CoordinateMapping;

/**
 * Util class providing methods for drawing labels, which can be rotated if the
 * available space is to small. Since the rotation depends on the position,
 * where the labels is drawn, a value of the
 * {@link org.knime.base.node.viz.plotter.LabelPaintUtil.Position} must be
 * passed to the {@link #drawLabel(String, Graphics2D, Rectangle,
 * org.knime.base.node.viz.plotter.LabelPaintUtil.Position, boolean)} method. If
 * the labels must be rotated due to the lack of space can also be determined
 * with the {@link #rotateLabels(CoordinateMapping[], int, FontMetrics)} method.
 *
 * @author Fabian Dill, University of Konstanz
 */
public final class LabelPaintUtil {

    private static final String DOTS = "..";

    private LabelPaintUtil() {

    }

    /**
     * Possible positions of the label.
     *
     * @author Fabian Dill, University of Konstanz
     */
    public enum Position {
        /** Label on the left side of the coordinate. */
        LEFT,
        /** Label on the bottom of the coordinate. */
        BOTTOM,
        /** Label on the right side of the coordinate. */
        TOP,
        /** Label on top of the coordinate. */
        RIGHT;
    }

    /**
     * Rounds the passed double value by the rounding factor and returns a
     * string representation of it. Typical usage for tooltips.
     *
     * @param value the value to be rounded
     * @param roundingFactor the rounding factor use 100 for two numbers after
     *            the comma, 1000 for three numbers after the comma and so on.
     * @return the string representation of the ronded double
     */
    public static String getDoubleAsString(final double value,
            final double roundingFactor) {
        long newVal = Math.round(value * roundingFactor);
        return "" + (newVal / roundingFactor);
    }

    /**
     * Draws the label, and if the available space is to small it rotates the
     * label depending on the
     * {@link org.knime.base.node.viz.plotter.LabelPaintUtil.Position} where to
     * draw it. If it is still too large it cuts the label with the
     * {@link #cutLabel(String, int, FontMetrics)} method. The Rectangle
     * available space gives information about the available space, depending on
     * the position the labels is placed inside this rectangle.
     *
     * @param label the label to display
     * @param g the graphics object
     * @param availableSpace the available space
     * @param position the position
     * @param rotate whether labels are rotated or not
     */
    public static void drawLabel(final String label, final Graphics2D g,
            final Rectangle availableSpace, final Position position,
            final boolean rotate) {
        if (position.equals(Position.LEFT)) {
            drawLeftLabel(label, g, availableSpace, rotate);
            return;
        }
        if (position.equals(Position.BOTTOM)) {
            drawBottomLabel(label, g, availableSpace, rotate);
            return;
        }
        if (position.equals(Position.RIGHT)) {
            drawRightLabel(label, g, availableSpace, rotate);
            return;
        }
        if (position.equals(Position.TOP)) {
            drawTopLabel(label, g, availableSpace, rotate);
        }

    }

    /**
     * Cuts the passed label until it fits into the desired length by cutting
     * out the middle of the label. The first three and the most possible part
     * of the length are retained. If some charaters are removed "..." is
     * inserted.
     *
     * @param label the label to cut
     * @param desiredLength the desired length
     * @param fm the font metrics
     * @return a cutted label which fits into the desired length
     */
    public static String cutLabel(final String label, final int desiredLength,
            final FontMetrics fm) {
        String newLabel = label;
        if (fm.stringWidth(label) > desiredLength) {
            // check if the first 3 and dots and the last 4 is still
            // too long
            if (label.length() > 4) {
                newLabel =
                        label.substring(0, 3)
                                + DOTS
                                + label.substring(label.length() - 4, label
                                        .length());
            } else {
                return DOTS;
            }
            if (fm.stringWidth(newLabel) > desiredLength) {
                // if so check if the last 4 + dots is too long
                newLabel =
                        DOTS
                                + label.substring(label.length() - 4, label
                                        .length());
                if (fm.stringWidth(newLabel) > desiredLength) {
                    // if so return dots
                    return DOTS;
                } else {
                    return newLabel;
                }
            } else if (fm.stringWidth(newLabel) < desiredLength) {
                // if more space than the first 3 and the last 4 is
                // available, increase the end until the string fills out the
                // available length
                int lastIndex = label.length() - 4;
                int firstIndex = 3 + DOTS.length();
                int cutPos = 4;
                while (fm.stringWidth(newLabel) < desiredLength
                        && lastIndex > firstIndex) {
                    cutPos++;
                    lastIndex = label.length() - cutPos;
                    newLabel =
                            label.substring(0, 3)
                                    + DOTS
                                    + label.substring(label.length() - cutPos,
                                            label.length());
                }
                return newLabel;
            }
            return newLabel;
        }
        return label;
    }

    /**
     * Returns true if any label is too long to be displayed in the available
     * space.
     *
     * @param mappings the set of labels to be displayed.
     * @param availableSize the available width
     * @param fm the font metrics
     * @return true if labels should be rotated, false otherwise
     */
    public static boolean rotateLabels(final CoordinateMapping[] mappings,
            final int availableSize, final FontMetrics fm) {
        for (int i = 0; i < mappings.length; i++) {
            String label = mappings[i].getDomainValueAsString();
            if (fm.stringWidth(label) > availableSize) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if any label is too long to be displayed in the available
     * space.
     *
     * @param labels the set of labels to be displayed.
     * @param availableSize the available width
     * @param fm the font metrics
     * @return true if labels should be rotated, false otherwise
     */
    public static boolean rotateLabels(final List<String> labels,
            final int availableSize, final FontMetrics fm) {
        for (String label : labels) {
            if (fm.stringWidth(label) > availableSize) {
                return true;
            }
        }
        return false;
    }

    /**
     * Draws a label left, that is it tries to put the end of the label
     * rightmost in the rectangle and rotate it it with the end at the bottom.
     *
     * @param label the label to draw
     * @param g the graphics object
     * @param availableSpace the available space to draw in
     * @param rotate flag whether to rotate the label or not
     */
    private static void drawLeftLabel(final String label, final Graphics2D g,
            final Rectangle availableSpace, final boolean rotate) {
        // TODO: paint as left as possible if the string is short!
        String drawLabel = label;
        // if not rotate simply draw the label at the x, y + height position
        if (!rotate) {
            // check if space is enough
            int xPos = availableSpace.x;
            if (g.getFontMetrics().stringWidth(label) > availableSpace.width) {
                drawLabel =
                        cutLabel(label, availableSpace.width, g
                                .getFontMetrics());
            } else {
                xPos =
                        (availableSpace.x + availableSpace.width)
                                - g.getFontMetrics().stringWidth(drawLabel);
            }
            // draw it
            g.drawString(drawLabel, xPos, availableSpace.y
                    + availableSpace.height);
        } else {
            int alpha = 45;
            double a = availableSpace.width;
            // the hypothenuse (since labels are rotate 45 degrees)
            // -> available length for rotated strings
            double hypo = Math.floor(a / Math.cos(Math.toRadians(alpha)));
            hypo -= g.getFontMetrics().getHeight();
            if (g.getFontMetrics().stringWidth(label) > hypo) {
                drawLabel =
                        cutLabel(label, (int)Math.floor(hypo), g
                                .getFontMetrics());
            }

            // int newX = (availableSpace.x + availableSpace.width)
            // - g.getFontMetrics().stringWidth(drawLabel)
            // + g.getFontMetrics().getHeight() / 2;
            double b =
                    Math.sin(Math.toRadians(alpha))
                            * g.getFontMetrics().stringWidth(drawLabel);
            int newX =
                    (int)(availableSpace.width - b)
                            - g.getFontMetrics().getDescent();
            int newY = availableSpace.y + (int)(availableSpace.getHeight() - b);
            // now rotate it
            AffineTransform at = g.getTransform();
            g.rotate(Math.toRadians(alpha), newX, newY);
            g.drawString(drawLabel, newX, newY);
            g.setTransform(at);
        }

    }

    private static void drawBottomLabel(final String label, final Graphics2D g,
            final Rectangle availableSpace, final boolean rotate) {
        String drawLabel = label;
        // if not rotate simply draw the label at the x, y + height position
        if (!rotate) {
            // check if space is enough
            if (g.getFontMetrics().stringWidth(label) > availableSpace.width) {
                drawLabel =
                        cutLabel(label, availableSpace.width, g
                                .getFontMetrics());
            }
            // draw it
            g.drawString(drawLabel, availableSpace.x, availableSpace.y);
        } else {
            // check if it has to be cutted
            int alpha = 45;
            double b = availableSpace.height;
            // the hypothenuse (since labels are rotate 45 degrees)
            // -> available length for rotated strings
            int hypo = (int)Math.floor(b / Math.cos(Math.toRadians(alpha)));
            hypo -= g.getFontMetrics().getHeight();
            if (g.getFontMetrics().stringWidth(label) > hypo
                    - g.getFontMetrics().getHeight()) {
                drawLabel = cutLabel(label, hypo, g.getFontMetrics());
            }
            // now rotate it
            AffineTransform at = g.getTransform();
            g.rotate(Math.toRadians(alpha), availableSpace.x, availableSpace.y);
            g.drawString(drawLabel, availableSpace.x, availableSpace.y);
            g.setTransform(at);
        }

    }

    private static void drawRightLabel(final String label, final Graphics2D g,
            final Rectangle availableSpace, final boolean rotate) {
        // TODO: top y position is still wrong
        String drawLabel = label;
        // if not rotate simply draw the label at the x, y + height position
        if (!rotate) {
            // check if space is enough
            if (g.getFontMetrics().stringWidth(label) > availableSpace.width) {
                drawLabel =
                        cutLabel(label, availableSpace.width, g
                                .getFontMetrics());
            }
            // draw it
            g.drawString(drawLabel, availableSpace.x, availableSpace.y
                    + availableSpace.height);
        } else {
            int alpha = -45;
            // check if it has to be cutted
            int a = availableSpace.width;
            // the hypothenuse (since labels are rotate 45 degrees)
            // -> available length for rotated strings
            int hypo = (int)Math.floor(a / Math.cos(Math.toRadians(alpha)));
            hypo -= g.getFontMetrics().getHeight();
            if (g.getFontMetrics().stringWidth(label) > hypo) {
                drawLabel = cutLabel(label, hypo, g.getFontMetrics());
            }
            // now rotate it
            AffineTransform at = g.getTransform();
            g.rotate(Math.toRadians(alpha), availableSpace.x, availableSpace.y
                    + availableSpace.height);
            g.drawString(drawLabel, availableSpace.x, availableSpace.y
                    + availableSpace.height);
            g.setTransform(at);
        }
    }

    private static void drawTopLabel(final String label, final Graphics2D g,
            final Rectangle availableSpace, final boolean rotate) {
        // TODO: top y position is still wrong
        String drawLabel = label;
        // if not rotate simply draw the label at the x, y + height position
        if (!rotate) {
            // check if space is enough
            if (g.getFontMetrics().stringWidth(label) > availableSpace.width) {
                drawLabel =
                        cutLabel(label, availableSpace.width, g
                                .getFontMetrics());
            }
            // draw it
            g.drawString(drawLabel, availableSpace.x, availableSpace.y
                    + availableSpace.height);
        } else {
            int alpha = -45;
            // hypthenuse = a / sin(alpha)
            // a = availableSpace.height
            // check if it has to be cutted
            double a = availableSpace.height;
            // the hypothenuse (since labels are rotate 45 degrees)
            // -> available length for rotated strings
            int hypo =
                    (int)Math.abs(Math.floor(a
                            / Math.sin(Math.toRadians(alpha))));
            hypo -= g.getFontMetrics().getHeight();
            if (g.getFontMetrics().stringWidth(label) > hypo) {
                drawLabel = cutLabel(label, hypo, g.getFontMetrics());
            }
            // now rotate it
            AffineTransform at = g.getTransform();
            g.rotate(Math.toRadians(alpha), availableSpace.x, availableSpace.y
                    + availableSpace.height);
            g.drawString(drawLabel, availableSpace.x, availableSpace.y
                    + availableSpace.height);
            g.setTransform(at);
        }
    }

}
