/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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

import org.knime.core.data.property.ColorAttr;

import org.knime.base.node.viz.aggregation.AggregationModel;
import org.knime.base.node.viz.aggregation.DrawingUtils;
import org.knime.base.node.viz.aggregation.util.LabelDisplayPolicy;
import org.knime.base.node.viz.pie.datamodel.PieSectionDataModel;
import org.knime.base.node.viz.pie.datamodel.PieSubSectionDataModel;
import org.knime.base.node.viz.pie.datamodel.PieVizModel;
import org.knime.base.node.viz.pie.util.GeometryUtil;
import org.knime.base.node.viz.plotter.AbstractDrawingPane;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;

import javax.swing.ToolTipManager;


/**
 * The drawing pane implementation of the pie chart which handles the
 * real drawing on the screen.
 * @author Tobias Koetter, University of Konstanz
 */
public class PieDrawingPane extends AbstractDrawingPane {

    private static final long serialVersionUID = -2626993487320068781L;

    /** Defines the font of the information message which is displayed. */
    private static final Font INFO_MSG_FONT = new Font("Arial", Font.PLAIN, 16);
    /**The alpha blending value of not selected sections.*/
    private static final float SECTION_ALPHA = 0.6f;
    /**The alpha blending value of not selected sections.*/
    private static final float SELECTED_SECTION_ALPHA = 1.0f;
    /** The extra space between the label and its link.*/
    private static final int SPACE_BETWEEN_LINK_AND_LABEL = 5;

    private static final BasicStroke SELECTION_OUTLINE_STROKE =
        new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1,
                new float[]{3}, 0);
    private static final Color SELECTION_OUTLINE_COLOR =
        Color.BLACK;

    private static final Color SECTION_OUTLINE_COLOR =
        Color.BLACK;

    private static final BasicStroke SECTION_OUTLINE_STROKE =
        new BasicStroke(1.0f);

    private static final Color HILITE_OUTLINE_COLOR = ColorAttr.HILITE;

    private static final BasicStroke HILITE_OUTLINE_STROKE =
        new BasicStroke(3f);

    private PieVizModel m_vizModel;

    /**
     * Information message. If not <code>null</code> no bars will be drawn
     * only this message will be displayed.
     */
    private String m_infoMsg = null;


    /**Constructor for class PieDrawingPane.*/
    protected PieDrawingPane() {
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    /**
     * @param vizModel the visualization model to draw
     */
    public void setVizModel(final PieVizModel vizModel) {
        if (vizModel == null) {
            throw new NullPointerException("vizModel must not be null");
        }
        m_vizModel = vizModel;
        repaint();
    }

    /**
     * @return the information message which will be displayed on the screen
     *         instead of the pie sections
     */
    public String getInfoMsg() {
        return m_infoMsg;
    }

    /**
     * If the information message is set no bars will be drawn. Only this
     * message will appear in the plotter.
     *
     * @param infoMsg the information message to display
     */
    public void setInfoMsg(final String infoMsg) {
        m_infoMsg = infoMsg;
    }

    /**
     * Resets the drawing pane.
     */
    public void reset() {
        m_vizModel = null;
        m_infoMsg = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintContent(final Graphics g) {
        final Graphics2D g2 = (Graphics2D)g;
        String msg = m_infoMsg;
        if (m_vizModel == null) {
            //if we have no bins and no info message display a no bars info
            if (msg == null) {
                msg = "No pie data available";
            }
        }
        //check if we have to display an information message
        if (msg != null) {
            DrawingUtils.drawMessage(g2, INFO_MSG_FONT, msg, getBounds());
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
        final double labelLinkSize = m_vizModel.getLabelLinkSize();
        final boolean explode = m_vizModel.explodeSelectedSections();
        final boolean drawOutline = m_vizModel.drawSectionOutline();
//      final AggregationMethod aggrMethod = m_vizModel.getAggregationMethod();
//      final double totalValue = m_vizModel.getAbsAggregationValue();
        final LabelDisplayPolicy labelPolicy =
            m_vizModel.getLabelDisplayPolicy();
        final boolean showDetails = m_vizModel.showDetails();

        for (final PieSectionDataModel section
                : m_vizModel.getSections2Draw()) {
            if (!section.isPresentable()) {
                //skip not presentable sections
                continue;
            }
            //check if we should draw ...
            if (showDetails && section.isSelected()) {
                //... all subsections of this section or...
                final Collection<PieSubSectionDataModel> elements =
                    section.getElements();
                for (final PieSubSectionDataModel subSection : elements) {
                    drawSection(g2, subSection, false);
                    if (LabelDisplayPolicy.ALL.equals(labelPolicy)
                            || (LabelDisplayPolicy.SELECTED.equals(labelPolicy)
                                    && subSection.isSelected())) {
                        final Rectangle2D labelArea;
                        if (explode && section.isSelected()) {
                            labelArea = explodeArea;
                        } else {
                            labelArea = pieArea;
                        }

                        final String label = m_vizModel.createLabel(section,
                                subSection);
                        drawLabel(g2, label, labelArea, labelLinkSize,
                                subSection);
                    }
                }
            } else {
                //...only the main section itself
                drawSection(g2, section, drawOutline);
                if (LabelDisplayPolicy.ALL.equals(labelPolicy)
                        || (LabelDisplayPolicy.SELECTED.equals(labelPolicy)
                                && section.isSelected())) {
                    final Rectangle2D labelArea;
                    if (explode && section.isSelected()) {
                        labelArea = explodeArea;
                    } else {
                        labelArea = pieArea;
                    }

                    final String label = m_vizModel.createLabel(section);
                    drawLabel(g2, label, labelArea, labelLinkSize, section);
                }
            }
        }
        //set the old rendering hints
        g2.setRenderingHints(origHints);

        //draw the rectangles for debugging
//        g2.setStroke(SECTION_OUTLINE_STROKE);
//        g2.setColor(Color.CYAN);
//        g2.draw(m_vizModel.getLabelArea());
//        g2.draw(m_vizModel.getExplodedArea());
//        g2.draw(m_vizModel.getPieArea());
    }

    private void drawLabel(final Graphics2D g2, final String label,
            final Rectangle2D area, final double labelLinkSize,
            final AggregationModel<Arc2D, ? extends Shape> section) {
        final double labelAngle = GeometryUtil.calculateMidAngle(
                section.getShape());
        //draw the label
        drawLabel(g2, label, labelAngle, area, labelLinkSize);
    }

    /**
     * @param g2
     * @param section
     * @param element
     * @param color
     */
    private void drawSection(final Graphics2D g2,
            final AggregationModel<? extends Arc2D, ? extends Arc2D> section,
            final boolean drawOutline) {
        if (!section.isPresentable()) {
            //skip not presentable sections
            return;
        }
        final Color color = section.getColor();
        final Arc2D shape = section.getShape();
        final float alpha;
        if (section.isSelected()) {
            //fill the section
            alpha = SELECTED_SECTION_ALPHA;
        } else {
            alpha = SECTION_ALPHA;
        }
        //fill the section
        DrawingUtils.drawBlock(g2, shape, color, alpha);
        //draw the section outline if desired
        if (drawOutline) {
            DrawingUtils.drawArc(g2, shape,
                    SECTION_OUTLINE_COLOR, SECTION_OUTLINE_STROKE);
        }
        if (section.supportsHiliting() && section.isHilited()
                && section.getHiliteShape() != null) {
            final Arc2D hiliteShape = section.getHiliteShape();
            DrawingUtils.drawArc(g2, hiliteShape, HILITE_OUTLINE_COLOR,
                    HILITE_OUTLINE_STROKE);
        }
        if (section.isSelected()) {
            //fill the section
            DrawingUtils.drawOutline(g2, shape,
                    SELECTION_OUTLINE_COLOR, SELECTION_OUTLINE_STROKE);
        }
    }

    private static void drawLabel(final Graphics2D g2,
            final String label, final double angle,
            final Rectangle2D area, final double labelMargin) {
//        final String label = section.getName() + " "
//            + aggrMethod.getText() + ": " + value;
        final FontMetrics metrics = g2.getFontMetrics();
        final int textWidth = metrics.stringWidth(label);
        final int textHeight = metrics.getHeight();
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
        if (angle >= 90 && angle <= 270) {
            //this is the left side of the pie
            if (angle <= 90 + margin) {
                //this is the top left section
                linkY2 = linkY1 - labelMargin;
            } else if (angle >= 270 - margin) {
                //this is the bottom left section
                linkY2 = linkY1 + labelMargin;
            } else {
                linkY2 = linkY1;
            }
            linkX2 = linkX1 - labelMargin;
            labelX = linkX2  - textWidth - SPACE_BETWEEN_LINK_AND_LABEL;
            labelY = linkY2 + textHeight / 2.0;
        } else {
            //this is the right side of the pie
            if (angle >= 90 - margin && angle <= 90) {
                //this is the top right section
                linkY2 = linkY1 - labelMargin;
            } else if (angle <= 270 + margin && angle >= 270) {
                //this is the bottom right section
                linkY2 = linkY1 + labelMargin;
            } else {
                linkY2 = linkY1;
            }
            linkX2 = linkX1 + labelMargin;
            labelX = linkX2 + SPACE_BETWEEN_LINK_AND_LABEL;
            labelY = linkY2 + textHeight / 2.0;
        }
        g2.drawLine((int)linkX1, (int)linkY1, (int)linkX2, (int)linkY2);
        g2.drawString(label, (float)labelX, (float)labelY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText(final MouseEvent e) {
        final Point p = e.getPoint();
        if (m_vizModel != null && p != null) {
            final PieSectionDataModel section =
                m_vizModel.getSelectedElement(p);
            if (section != null) {
                if (section.isSelected() && m_vizModel.showDetails()) {
                    final PieSubSectionDataModel subSection =
                        section.getSelectedSubElement(p);
                    if (subSection != null) {
                        return m_vizModel.createLabel(section, subSection);
                    }
                }
                return m_vizModel.createLabel(section);
            }
        }
        return null;
    }
}
