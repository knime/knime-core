/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *    12.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.pie.impl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.aggregation.AggregationModel;
import org.knime.base.node.viz.aggregation.DrawingUtils;
import org.knime.base.node.viz.pie.datamodel.PieSectionDataModel;
import org.knime.base.node.viz.pie.datamodel.PieVizModel;
import org.knime.base.node.viz.pie.util.GeometryUtil;
import org.knime.base.node.viz.plotter.AbstractDrawingPane;
import org.knime.core.data.property.ColorAttr;


/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class PieDrawingPane extends AbstractDrawingPane {

    /**The number of digits to display for a label.*/
    private static final int NO_OF_LABEL_DIGITS = 2;
    /**This stroke is used to draw the rectangle around the hilite rectangle.*/
    private static final BasicStroke SELECTION_OUTLINE_STROKE =
        new BasicStroke(2f);
    private static final Color SELECTION_OUTLINE_COLOR =
        ColorAttr.SELECTED;

    private static final Color SECTION_OUTLINE_COLOR =
        Color.BLACK;
    private static final BasicStroke SECTION_OUTLINE_STROKE =
        new BasicStroke(1f);

    /**The alpha value of the overloaded element block.*/
    private static final float HILITE_FILLING_ALPHA = 1.0f;
    /**Defines the filing of the hilite sections.*/
    private static final TexturePaint HILITE_FILLING;
    static {
//      draw 2D rounded rectangle with a buffered background
        final BufferedImage img =
            new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        // obtain Graphics2D from bufferImage and draw on it
        final Graphics2D g2 = img.createGraphics();
        g2.setColor(ColorAttr.HILITE);
        g2.fillRect(0, 0, 4, 4);
        g2.setColor(Color.GRAY);
        g2.fillRect(0, 0, 1, 1);
        g2.fillRect(1, 1, 1, 1);
        final Rectangle rect = new Rectangle(img.getWidth(), img.getHeight());
        HILITE_FILLING = new TexturePaint(img, rect);
    }

    private PieVizModel m_vizModel;

    /**
     * @param vizModel the visualization model to draw
     */
    public void setVizModel(final PieVizModel vizModel) {
        m_vizModel = vizModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintContent(final Graphics g) {
        final Graphics2D g2 = (Graphics2D)g;
        if (m_vizModel == null) {
            g2.drawLine(0, 0, 500, 500);
            return;
        }
        final RenderingHints origHints = g2.getRenderingHints();
        if (m_vizModel.drawAntialias()) {
            // Enable antialiasing for shapes
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
         // Disable antialiasing for shapes
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_OFF);
        }
        final Rectangle2D explodeArea = m_vizModel.getExplodedArea();
        final Rectangle2D pieArea = m_vizModel.getPieArea();
        final double labelMargin = m_vizModel.getLabelLinkSize();
        final AggregationMethod aggrMethod = m_vizModel.getAggregationMethod();
        final double totalValue = m_vizModel.getAggregationValue();
        if (m_vizModel.showDetails()) {
            final List<? extends AggregationModel<? extends Shape,
                    ? extends Shape>> drawSubSections =
                        m_vizModel.getDrawSubSections();
            for (final AggregationModel<? extends Shape,
                    ? extends Shape> subSection : drawSubSections) {
                final Shape element = subSection.getShape();
                final Color color = subSection.getColor();
                DrawingUtils.drawBlock(g2, element, color);
                if (subSection.isSelected()) {
                    DrawingUtils.drawOutline(g2, element,
                            SELECTION_OUTLINE_COLOR, SELECTION_OUTLINE_STROKE);
                }
                if (subSection.isHilited()
                        && subSection.getHiliteShape() != null) {
                    final Shape hiliteElement = subSection.getHiliteShape();
                    DrawingUtils.drawBlock(g2, hiliteElement, HILITE_FILLING,
                            HILITE_FILLING_ALPHA);

                }
            }
            for (final AggregationModel<? extends Shape,
                    ? extends Shape> section : m_vizModel.getDrawSections()) {
                final Shape element = section.getShape();
                DrawingUtils.drawOutline(g2, element, section.getColor(),
                        SECTION_OUTLINE_STROKE);
            }
        } else {
            for (final PieSectionDataModel section : m_vizModel.getSections()) {
                final double value = section.getAggregationValue(aggrMethod);
                final Arc2D element = section.getShape();
                final Color color = section.getColor();
                DrawingUtils.drawBlock(g2, element, color);
                if (section.isSelected()) {
                    DrawingUtils.drawOutline(g2, element,
                            SELECTION_OUTLINE_COLOR, SELECTION_OUTLINE_STROKE);
                } else if (m_vizModel.drawSectionOutline()) {
                    DrawingUtils.drawOutline(g2, element,
                            SECTION_OUTLINE_COLOR, SECTION_OUTLINE_STROKE);
                }
                if (section.supportsHiliting() && section.isHilited()
                        && section.getHiliteShape() != null) {
                    final Shape hiliteElement = section.getHiliteShape();
                    DrawingUtils.drawBlock(g2, hiliteElement, HILITE_FILLING,
                            HILITE_FILLING_ALPHA);

                }
                final double labelAngle = GeometryUtil.calculateMidAngle(
                        element, totalValue, value);
                final String label = createLabel(section.getName(), aggrMethod,
                        value);
                //draw the label
                if (section.isSelected()) {
                    drawLabel(g2, label, labelAngle, explodeArea, labelMargin);
                } else {
                    drawLabel(g2, label, labelAngle, pieArea, labelMargin);
                }
            }
        }
        //set the old rendering hints
        g2.setRenderingHints(origHints);

        //draw the rectangles for debugging
        g2.setStroke(SECTION_OUTLINE_STROKE);
        g2.setColor(Color.CYAN);
        g2.draw(m_vizModel.getLabelArea());
        g2.draw(m_vizModel.getExplodedArea());
        g2.draw(m_vizModel.getPieArea());
    }

    private static String createLabel(final String name,
            final AggregationMethod aggrMethod, final double value) {
        final String valuePart = aggrMethod.createLabel(value,
                NO_OF_LABEL_DIGITS);
        if (name != null) {
            return name + " " + aggrMethod.getText() + ": " + valuePart;
        }
        return valuePart;

    }

    /**
     * @param g2
     * @param section
     * @param totalValue
     * @param aggrMethod
     * @param labelArea
     */
    private static void drawLabel(final Graphics2D g2,
            final String label, final double angle,
            final Rectangle2D area, final double labelMargin) {
//        final String label = section.getName() + " "
//            + aggrMethod.getText() + ": " + value;
        final FontMetrics metrics = g2.getFontMetrics();
        final int textWidth = metrics.stringWidth(label);
//        final int textHeight = metrics.getHeight();
        final double borderXend = Math.cos(Math.toRadians(angle))
                                    * (area.getWidth() / 2);
        final double borderYend = -Math.sin(Math.toRadians(angle))
                                    * (area.getWidth() / 2);
        final double linkX1 = area.getCenterX() + borderXend;
        final double linkY1 = area.getCenterY() + borderYend;
        final double linkX2;
        final double linkY2;
        final double labelX;
        final double labelY;
        final int margin = 30;
        if (angle > 90 && angle < 270) {
            //this is the left side of the pie
            if (angle < 90 + margin) {
                //this is the top left section
                linkY2 = linkY1 - labelMargin;
            } else if (angle > 270 - margin) {
                //this is the bottom left section
                linkY2 = linkY1 + labelMargin;
            } else {
                linkY2 = linkY1;
            }
            linkX2 = linkX1 - labelMargin;
            labelX = linkX2  - textWidth;
            labelY = linkY2;
        } else {
            //this is the right side of the pie
            if (angle > 90 - margin && angle < 90) {
                //this is the top right section
                linkY2 = linkY1 - labelMargin;
            } else if (angle < 270 + margin && angle > 270) {
                //this is the bottom right section
                linkY2 = linkY1 + labelMargin;
            } else {
                linkY2 = linkY1;
            }
            linkX2 = linkX1 + labelMargin;
            labelX = linkX2;
            labelY = linkY2;
        }
        g2.drawLine((int)linkX1, (int)linkY1, (int)linkX2, (int)linkY2);
        g2.drawString(label, (int)labelX, (int)labelY);
    }

    /**
     * Resets the drawing pane.
     */
    public void reset() {
        m_vizModel = null;
    }

}
