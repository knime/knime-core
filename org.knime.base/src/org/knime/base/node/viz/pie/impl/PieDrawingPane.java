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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.util.List;

import org.knime.base.node.viz.aggregation.AggregationModel;
import org.knime.base.node.viz.aggregation.DrawingUtils;
import org.knime.base.node.viz.pie.datamodel.PieVizModel;
import org.knime.base.node.viz.plotter.AbstractDrawingPane;
import org.knime.core.data.property.ColorAttr;


/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class PieDrawingPane extends AbstractDrawingPane {

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
            for (final AggregationModel<? extends Shape,
                    ? extends Shape> section : m_vizModel.getDrawSections()) {
                final Shape element = section.getShape();
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
            }
        }

        //set the old rendering hints
        g2.setRenderingHints(origHints);
    }

    /**
     * Resets the drawing pane.
     */
    public void reset() {
        m_vizModel = null;
    }

}
