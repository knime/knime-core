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
 */
package org.knime.base.node.viz.histogram.impl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collection;

import javax.swing.ToolTipManager;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.aggregation.DrawingUtils;
import org.knime.base.node.viz.aggregation.util.GUIUtils;
import org.knime.base.node.viz.aggregation.util.LabelDisplayPolicy;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel;
import org.knime.base.node.viz.histogram.datamodel.BarDataModel;
import org.knime.base.node.viz.histogram.datamodel.BarElementDataModel;
import org.knime.base.node.viz.histogram.datamodel.BinDataModel;
import org.knime.base.node.viz.histogram.datamodel.InteractiveBarDataModel;
import org.knime.base.node.viz.histogram.datamodel.InteractiveBarElementDataModel;
import org.knime.base.node.viz.histogram.datamodel.InteractiveBinDataModel;
import org.knime.base.node.viz.plotter.AbstractDrawingPane;
import org.knime.core.data.property.ColorAttr;

/**
 * The view class of a Histogram visualisation. It simply uses the given
 * information of the <code>BarVisModel</code> <code>Collection</code> to
 * display the bars on the screen.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class HistogramDrawingPane extends AbstractDrawingPane {

        private static final long serialVersionUID = 7881989778083295425L;

    /**The number of digits to display for a label.*/
    private static final int NO_OF_LABEL_DIGITS = 2;

    /**Defines the color of overloaded elements.*/
    private static final TexturePaint OVERLOADED_ELEMENT_FILLING;
    static {
//      draw 2D rounded rectangle with a buffered background
        final BufferedImage img =
            new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        // obtain Graphics2D from bufferImage and draw on it
        final Graphics2D g2 = img.createGraphics();
        g2.setColor(Color.LIGHT_GRAY);
        g2.fillRect(0, 0, 4, 4);
        g2.setColor(Color.GRAY);
        g2.fillRect(0, 0, 1, 1);
        g2.fillRect(1, 1, 1, 1);
        final Rectangle2D rect = new Rectangle(img.getWidth(), img.getHeight());
        OVERLOADED_ELEMENT_FILLING = new TexturePaint(img, rect);
    }

    /**The alpha value of the overloaded element block.*/
    private static final float OVERLOADED_ELEMENT_ALPHA = 0.8f;

    /**This stroke is used to draw the rectangle around each element.*/
    private static final BasicStroke ELEMENT_OUTLINE_STROKE =
        new BasicStroke(1f);

    private static final float[] BIN_SURROUNDING_DACH = {10.0f};
    /** Defines the stroke of the bin surrounding rectangle. */
    private static final BasicStroke BIN_SURROUNDING_STROKE =
        new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10.0f, BIN_SURROUNDING_DACH, 0.0f);

    /**The color of the bin surrounding rectangle.*/
    private static final Color BIN_SURROUNDING_COLOR = Color.LIGHT_GRAY;

    /**The alpha value of the bar surrounding block in the side by side layout.
     * */
    private static final float BAR_SIDE_BY_SIDE_SURROUNDING_ALPHA = 0.2f;
    /**The alpha value of the bar surrounding block in the stacked layout.*/
    private static final float BAR_STACKED_SURROUNDING_ALPHA = 0.7f;

    /**The color of the element outline.*/
    private static final Color ELEMENT_OUTLINE_COLOR = Color.BLACK;

    /**This stroke is used to draw the rectangle around each element.*/
    private static final BasicStroke ELEMENT_SELECTED_OUTLINE_STROKE =
        new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1,
                new float[]{3}, 0);

    /**The color of the element outline.*/
    private static final Color ELEMENT_SELECTED_OUTLINE_COLOR =
        Color.BLACK;

    /**This color is used to fill the hilite rectangle.*/
    private static final Color HILITE_RECT_BGR_COLOR = ColorAttr.HILITE;

    /**This stroke is used to draw the rectangle around the hilite rectangle.*/
    private static final BasicStroke HILITE_RECT_OUTLINE_STROKE =
        new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    /**The color of the hilite rectangle outline.*/
    private static final Color HILITE_RECT_OUTLINE_COLOR = Color.BLACK;

    /** Defines the stroke of the rectangle which surrounds the bar label. */
    private static final BasicStroke LABEL_RECT_STROKE = new BasicStroke(1f);

    /** Defines the color of the stroke of the rectangle which surrounds
     * the bar label. */
    private static final Color LABEL_RECT_STROKE_COLOR = Color.LIGHT_GRAY;

    /**Defines the background color of the label rectangle. */
    private static final Color LABEL_RECT_BGR_COLOR = Color.WHITE;

    /** Defines the font of the bar labels. */
    private static final Font BAR_VALUE_FONT =
        new Font("Arial", Font.PLAIN, 12);

    /**
     * Defines the space between the bottom of the bar and the aggregation
     * value in pixel.
     */
    private static final int AGGR_VAL_LABEL_SPACER = 20;

    /**
     * Defines how much space should be between the value and the border of the
     * background. Should be smaller than the AGGR_VAL_LABEL_SPACER value.
     */
    private static final int AGGR_VAL_BG_SPACER = 2;

    /**Defines the color of the base line.*/
    private static final Color BASE_LINE_COLOR = Color.BLACK;

    /** Defines the stroke of the base line. */
    private static final BasicStroke BASE_LINE_STROKE = new BasicStroke(2f);

    /**Defines the color of the base line.*/
    private static final Color GRID_LINE_COLOR = Color.LIGHT_GRAY;

    /** Defines the stroke of the base line. */
    private static final BasicStroke GRID_LINE_STROKE = new BasicStroke(1f);

    /** Defines the font of the information message which is displayed. */
    private static final Font INFO_MSG_FONT = new Font("Arial", Font.PLAIN, 16);

    /**
     * Holds the {@link AbstractHistogramVizModel} objects to draw.
     */
    private AbstractHistogramVizModel m_vizModel;

    private final AbstractHistogramProperties m_properties;

    private boolean m_updatePropertiesPanel = false;

    /**
     * Information message. If not <code>null</code> no bars will be drawn
     * only this message will be displayed.
     */
    private String m_infoMsg = null;

    /**If set the base line is drawn at this screen position.*/
    private Integer m_baseLine = null;

    /**If set the grid lines are drawn at the given positions.*/
    private int[] m_gridLines;


    /**
     * Constructor for class HistogramDrawingPane.
     * @param props the {@link AbstractHistogramProperties} panel
     */
    protected HistogramDrawingPane(final AbstractHistogramProperties props) {
        super();
        m_properties = props;
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    /**
     * Call this method to indicates that the properties panel should be
     * update the next time the {@link #paintComponent(Graphics)} method
     * is called.
     */
    public void updateProperties() {
        m_updatePropertiesPanel = true;
    }

    /**
     * @param histoData the {@link AbstractHistogramVizModel} objects to draw
     * @param updatPropertiesPanel set to <code>true</code> if the
     * properties panel should be updated as well
     */
    public void setHistogramVizModel(final AbstractHistogramVizModel histoData,
            final boolean updatPropertiesPanel) {
        m_vizModel = histoData;
        m_updatePropertiesPanel = updatPropertiesPanel;
        repaint();
    }

    /**
     * @return the information message which will be displayed on the screen
     *         instead of the bars
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
     * Indicates if a base line should be drawn. Set to <code>null</code> if
     * none base line should be drawn.
     * @param baseLine the Y coordinate of the baseline on the screen
     */
    public void setBaseLine(final Integer baseLine) {
        m_baseLine = baseLine;
    }

    /**
     * Indicates if the grid lines should be drawn. Set to <code>null</code> if
     * none grid lines should be drawn
     * @param gridLines the Y coordinate of each grid line on the screen
     */
    public void setGridLines(final int[] gridLines) {
        m_gridLines = gridLines;
    }

    /**
     * @return the width of the stroke used to outline the bars in the
     *         visualisation
     */
    protected static double getBarStrokeWidth() {
        return ELEMENT_OUTLINE_STROKE.getLineWidth();
        /*return Math.max(BAR_OUTLINE_BASIC_STROKE.getLineWidth(),
                BAR_OUTLINE_NEGATIVE_STROKE.getLineWidth());*/
    }

    /**
     * Resets the internal values of the histogram drawing pane to their default
     * values.
     */
    public void reset() {
        m_baseLine = null;
        m_gridLines = null;
        m_vizModel = null;
        m_infoMsg = null;
    }
    // **********************************************
    /*--------- the drawing methods ----------------*/
    // **********************************************

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintContent(final Graphics g) {
        final Graphics2D g2 = (Graphics2D)g;
        final Rectangle2D bounds = getBounds();
        String msg = m_infoMsg;
        final AbstractHistogramVizModel vizModel = m_vizModel;
        if (vizModel == null || vizModel.getBins() == null) {
            //if we have no bins and no info message display a no bars info
            if (msg == null) {
                msg = "No bins to display";
            }
        }
        //check if we have to display an information message
        if (msg != null) {
            DrawingUtils.drawMessage(g2, INFO_MSG_FONT, msg, bounds);
            return;
        }
        if (m_updatePropertiesPanel && m_properties != null) {
            m_properties.updateHistogramSettings(vizModel);
            m_updatePropertiesPanel = false;
        }
//      check if we have to draw the grid lines
        if (vizModel.isShowGridLines() && m_gridLines != null) {
            for (final int gridLine : m_gridLines) {
                DrawingUtils.paintHorizontalLine(g2, 0, gridLine,
                        (int) bounds.getWidth(), GRID_LINE_COLOR,
                        GRID_LINE_STROKE);
            }
        }
        //get all variables which are needed multiple times
        final AggregationMethod aggrMethod = vizModel.getAggregationMethod();
//        final Collection<ColorColumn> aggrColumns =
//            vizModel.getAggrColumns();
        final HistogramLayout layout = vizModel.getHistogramLayout();
        //if the user has selected more then one aggregation column we have to
        //draw the bar outline to how him which bar belongs to which aggregation
        //column
        final boolean drawBinOutline = vizModel.isShowBinOutline();
        final boolean drawBarOutline = vizModel.isShowBarOutline();
//            (aggrColumns != null
//            && aggrColumns.size() > 1)
//            || HistogramLayout.SIDE_BY_SIDE.equals(
//                m_vizModel.getHistogramLayout());
        final boolean showElementOutline = vizModel.isShowElementOutline();
        final LabelDisplayPolicy labelDisplayPolicy =
            vizModel.getLabelDisplayPolicy();
        final boolean showLabelVertical = vizModel.isShowLabelVertical();
        final float barOutlineAlpha;
        if (HistogramLayout.SIDE_BY_SIDE.equals(
                vizModel.getHistogramLayout())) {
            barOutlineAlpha = BAR_SIDE_BY_SIDE_SURROUNDING_ALPHA;
        } else {
            barOutlineAlpha = BAR_STACKED_SURROUNDING_ALPHA;
        }
        // loop over all bins and paint them
        for (final BinDataModel bin : vizModel.getBins()) {
            if (drawBinOutline) {
                DrawingUtils.drawRectangle(g2, bin.getSurroundingRectangle(),
                        BIN_SURROUNDING_COLOR , BIN_SURROUNDING_STROKE);
            }
            if (!bin.isPresentable()) {
                //the bars doen't fit in this bin so we have to
                //fill the complete bin in black to show it to the user
                DrawingUtils.drawBlock(g2, bin.getBinRectangle(),
                        OVERLOADED_ELEMENT_FILLING, OVERLOADED_ELEMENT_ALPHA);
                if (bin.isSelected()) {
                    DrawingUtils.drawRectangle(g2, bin.getBinRectangle(),
                        ELEMENT_SELECTED_OUTLINE_COLOR,
                        ELEMENT_SELECTED_OUTLINE_STROKE);
                }
                if (bin instanceof InteractiveBinDataModel) {
                    final InteractiveBinDataModel interactiveBin =
                        (InteractiveBinDataModel)bin;
                    drawHiliteRect(g2, interactiveBin.getHiliteRectangle());
                }
                continue;
            }
            final Collection<BarDataModel> bars = bin.getBars();
            for (final BarDataModel bar : bars) {
                if (drawBarOutline) {
                    //draw the outline of the bar if we have multiple
                    //aggregation columns
                    DrawingUtils.drawBlock(g2, bar.getSurroundingRectangle(),
                            bar.getColor(), barOutlineAlpha);
                }
                if (bar.isPresentable()) {
                    drawElements(g2, bar.getElements(), showElementOutline);
                } else {
                    //the elements doen't fit in this bar so we have to
                    //fill the complete bar to show it to the user
                    final Rectangle2D barRectangle = bar.getShape();
                    DrawingUtils.drawBlock(g2, barRectangle,
                            OVERLOADED_ELEMENT_FILLING,
                            OVERLOADED_ELEMENT_ALPHA);
                    if (bar.isSelected()) {
                        DrawingUtils.drawRectangle(g2, barRectangle,
                            ELEMENT_SELECTED_OUTLINE_COLOR,
                            ELEMENT_SELECTED_OUTLINE_STROKE);
                    }
                    if (bar instanceof InteractiveBarDataModel) {
                        final InteractiveBarDataModel interactiveBar =
                            (InteractiveBarDataModel)bar;
                        drawHiliteRect(g2, interactiveBar.getHiliteShape());
                    }
                }
                //draw the bar label at last to have them on top
                drawLabels(g2, bar, aggrMethod, layout, bounds,
                        labelDisplayPolicy, showLabelVertical);
            } //end of bar loop
        } // end of the bin loop
//      check if we have to draw the base line
        if (m_baseLine != null) {
            DrawingUtils.paintHorizontalLine(g2, 0, m_baseLine.intValue(),
                    (int) bounds.getWidth(), BASE_LINE_COLOR,
                    BASE_LINE_STROKE);
        }
        return;
    }


    /**
     * Handles the label drawing.
     * @param g2 the graphics object
     * @param bar the bar for which the label(s) should be drawn
     * @param aggrMethod the aggregation method to get the right label
     * @param layout the current layout to decide if the label is per
     * element or per bar
     * @param bounds the surrounding pane on which to draw
     * @param displayPolicy the {@link LabelDisplayPolicy}
     * @param showVertical if set to <code>true</code> the labels are
     * painted vertical otherwise horizontal
     */
    private void drawLabels(final Graphics2D g2, final BarDataModel bar,
            final AggregationMethod aggrMethod, final HistogramLayout layout,
            final Rectangle2D bounds, final LabelDisplayPolicy displayPolicy,
            final boolean showVertical) {
        if (LabelDisplayPolicy.ALL.equals(
                displayPolicy)
                || (LabelDisplayPolicy.SELECTED.equals(
                        displayPolicy) && bar.isSelected())) {
            if (HistogramLayout.STACKED.equals(layout)
                    || !bar.isPresentable()) {
                paintLabel(g2, bar.getShape(),
                        bar.getAggregationValue(aggrMethod), aggrMethod,
                        bounds, showVertical);
            } else if (HistogramLayout.SIDE_BY_SIDE.equals(layout)) {
                //paint a label for each element after painting
                //the elements itself to have them in the front
                for (final BarElementDataModel element : bar.getElements()) {
                    if (element.isSelected()
                            || LabelDisplayPolicy.ALL.equals(
                                    displayPolicy)) {
                        final double aggrVal =
                            element.getAggregationValue(aggrMethod);
                        paintLabel(g2, element.getElementRectangle(),
                                aggrVal, aggrMethod, bounds,
                                showVertical);
                    }
                }
            } else {
                throw new IllegalArgumentException(
                        "Layout " + layout + " not supported");
            }
        }
    }


    /**
     * Draws the given elements on the screen.
     * @param g2 the graphics object
     * @param elements the elements to draw
     * @param showElementOutlines if the outline of each element
     * should be drawn
     */
    private static void drawElements(final Graphics2D g2,
            final Collection<BarElementDataModel> elements,
            final boolean showElementOutlines) {
        for (final BarElementDataModel element : elements) {
            final Color elementColor = element.getColor();
            //draw the element itself first
            final Rectangle2D elementRect =
                element.getElementRectangle();
//            drawBlock(g2, elementRect, elementColor);
//          draw the surrounding rectangles after the block
            if (element.isSelected()) {
                DrawingUtils.drawBlock(g2, elementRect, elementColor);
                DrawingUtils.drawRectangle(g2, elementRect,
                        ELEMENT_SELECTED_OUTLINE_COLOR,
                        ELEMENT_SELECTED_OUTLINE_STROKE);
            } else {
                DrawingUtils.drawBlock(g2, elementRect, elementColor, 0.8f);
                if (showElementOutlines) {
                    DrawingUtils.drawRectangle(g2, elementRect,
                            ELEMENT_OUTLINE_COLOR,
                            ELEMENT_OUTLINE_STROKE);
                }
            }
            if (element instanceof InteractiveBarElementDataModel) {
                final InteractiveBarElementDataModel interactiveElement =
                    (InteractiveBarElementDataModel)element;
//              draw the hilite rectangle
                final Rectangle2D hiliteRect =
                    interactiveElement.getHiliteShape();
                drawHiliteRect(g2, hiliteRect);
            }

        } //end of element loop
    }

    /**
     * @param g2 the graphics object
     * @param hiliteRect the rectangle to draw. If null no rectangle
     * is drawn.
     */
    private static void drawHiliteRect(final Graphics2D g2,
            final Rectangle2D hiliteRect) {
        if (hiliteRect != null) {
            DrawingUtils.drawBlock(g2, hiliteRect, HILITE_RECT_BGR_COLOR);
            //always draw the hilite borders to make them visible
            //even if the bar has the same color like the hilite color
            //but only if the complete rectangle is wider than the
            //stroke
            if (hiliteRect.getWidth()
                    > HILITE_RECT_OUTLINE_STROKE.getLineWidth()) {
                DrawingUtils.drawRectangle(g2, hiliteRect,
                        HILITE_RECT_OUTLINE_COLOR,
                        HILITE_RECT_OUTLINE_STROKE);
            }
        }
    }

   /**
     * Draws a the given aggregation value as label inside of the given border
     * rectangle.
     * @param g2 the graphics object
     * @param borderRect the rectangle we wont to label
     * @param aggrVal the label value
     * @param aggrMethod the aggregation method to know how to round the value
     * @param drawingSpace the drawing space itself to avoid drawing outside
     * @param showVertical set to <code>true</code> if the label should be
     * painted vertical otherwise it is drawn horizontal
     */
    private static void paintLabel(final Graphics2D g2,
            final Rectangle2D borderRect, final double aggrVal,
            final AggregationMethod aggrMethod, final Rectangle2D drawingSpace,
            final boolean showVertical) {
        if (borderRect == null) {
            return;
        }
        final String label = GUIUtils.createLabel(aggrVal, NO_OF_LABEL_DIGITS,
                aggrMethod);
        // save the original settings
        final AffineTransform origTransform = g2.getTransform();
        final Font origFont = g2.getFont();
        final Paint origPaint = g2.getPaint();
        final Stroke origStroke = g2.getStroke();
        final AffineTransform verticalTrans = new AffineTransform();
        g2.setFont(BAR_VALUE_FONT);
        final FontMetrics metrics = g2.getFontMetrics();
        int textX = (int)(borderRect.getX() + (borderRect.getWidth() / 2));
        int textWidth = metrics.stringWidth(label);
        //I always divide by 2 because the text is drawn at the specified
        //position and then rotated by the center!!!
        textX -= textWidth / 2;
        // avoid drawing the aggregation value outside of the display
        if (textX < 1) {
            textWidth += Math.abs(textX - 1);
            textX = 1;
        }
        int textY = 0;
        if (aggrVal >= 0) {
            textY =  (int)(borderRect.getY() + borderRect.getHeight()
                    - textWidth / 2.0) - AGGR_VAL_LABEL_SPACER;
        } else {
            textY = (int)(borderRect.getY() + textWidth / 2.0)
                + AGGR_VAL_LABEL_SPACER;
        }
        final double screenHeight = drawingSpace.getHeight();
        //check if the label is outside of the drawing space
        if (textY + textWidth / 2 > screenHeight) {
            textY = (int)(screenHeight - textWidth / 2.0 - AGGR_VAL_LABEL_SPACER);
        } else if (textY < 0) {
            textY = textWidth / 2 + AGGR_VAL_LABEL_SPACER;
        }
        final int textHeight = metrics.getHeight();
        // calculate the text background rectangle
        final int bgX = textX - AGGR_VAL_BG_SPACER;
        final int bgY = textY - textHeight - AGGR_VAL_BG_SPACER;
        final Rectangle textBackGroundRec = new Rectangle(bgX, bgY,
                textWidth + 2 * AGGR_VAL_BG_SPACER,
                textHeight + 2 * AGGR_VAL_BG_SPACER);
        if (showVertical) {
            // rotate 90 degree around the center of the text rectangle
            verticalTrans.rotate(Math.toRadians(90), textBackGroundRec
                    .getCenterX(), textBackGroundRec.getCenterY());
            g2.transform(verticalTrans);
        }
        g2.setPaint(LABEL_RECT_BGR_COLOR);
        g2.fill(textBackGroundRec);
        g2.setPaint(LABEL_RECT_STROKE_COLOR);
        g2.setStroke(LABEL_RECT_STROKE);
        g2.draw(textBackGroundRec);
        //g2.setPaint(rectColor);
        g2.drawString(label, textX, textY);
        // set the original settings
        g2.setTransform(origTransform);
        g2.setFont(origFont);
        g2.setPaint(origPaint);
        g2.setStroke(origStroke);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText(final MouseEvent e) {
//        final Point p = e.getPoint();
//        if (m_vizModel != null && p != null) {
//            final BarDataModel bar =
//                m_vizModel.getSelectedElement(p);
//            if (bar != null) {
//                final AggregationMethod aggrMethod =
//                    m_vizModel.getAggregationMethod();
//                final double aggrVal;
////                if (LabelDisplayPolicy.ALL.equals(
////                        m_vizModel.getLabelDisplayPolicy())) {
//                    final BarElementDataModel element =
//                        m_vizModel.getSelectedSubElements(p, bar);
//                    if (element != null) {
//                        aggrVal = element.getAggregationValue(aggrMethod);
//                    } else {
//                        aggrVal = bar.getAggregationValue(aggrMethod);
//                    }
////                } else {
////                    aggrVal = bar.getAggregationValue(aggrMethod);
////                }
//                return GUIUtils.createLabel(aggrVal, NO_OF_LABEL_DIGITS,
//                        aggrMethod);
//            }
//        }
        return null;
    }
}
