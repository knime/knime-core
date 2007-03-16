/*
 * ------------------------------------------------------------------
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
 */
package org.knime.base.node.viz.histogram;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.Collection;

import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel;
import org.knime.base.node.viz.histogram.datamodel.BarDataModel;
import org.knime.base.node.viz.histogram.datamodel.BarElementDataModel;
import org.knime.base.node.viz.histogram.datamodel.BinDataModel;
import org.knime.base.node.viz.histogram.datamodel.ColorColumn;
import org.knime.base.node.viz.histogram.datamodel.InteractiveBarDataModel;
import org.knime.base.node.viz.histogram.datamodel.InteractiveBarElementDataModel;
import org.knime.base.node.viz.plotter.AbstractDrawingPane;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.NodeLogger;

/**
 * The view class of a Histogram visualisation. It simply uses the given
 * information of the <code>BarVisModel</code> <code>Collection</code> to
 * display the bars on the screen.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class HistogramDrawingPane extends AbstractDrawingPane {
    
    private static final long serialVersionUID = 7881989778083295425L;
    
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(HistogramDrawingPane.class);
    
    /**Used to format the aggregation value for the aggregation method count.*/
    private static final DecimalFormat AGGREGATION_LABEL_FORMATER_COUNT = 
        new DecimalFormat("#");
    
    /**The number of digits to display for a label.*/
    private static final int NO_OF_LABEL_DIGITS = 2;
    
    /**Defines the color of the base line.*/
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
        final Rectangle rect = new Rectangle(img.getWidth(), img.getHeight());
        OVERLOADED_ELEMENT_FILLING = new TexturePaint(img, rect);
    }
    
    /**This stroke is used to draw the rectangle around each element.*/
    private static final BasicStroke ELEMENT_OUTLINE_STROKE = 
        new BasicStroke(1f);

//    /**This stroke is used to draw the rectangle around each aggregation
//     * column bar.*/
//    private static final BasicStroke AGGR_COLUM_OUTLINE_STROKE = 
//        new BasicStroke(1f);
    /**The color of the element outline.*/
    private static final Color ELEMENT_OUTLINE_COLOR = Color.BLACK;

    /**This stroke is used to draw the rectangle around each element.*/
    private static final BasicStroke ELEMENT_SELECTED_OUTLINE_STROKE = 
        new BasicStroke(3f);
    
    /**The color of the element outline.*/
    private static final Color ELEMENT_SELECTED_OUTLINE_COLOR = 
        ColorAttr.SELECTED;
    
    /**This color is used to fill the hilite rectangle.*/
    private static final Color HILITE_RECT_BGR_COLOR = ColorAttr.HILITE;
    
    /**This stroke is used to draw the rectangle around the hilite rectangle.*/
    private static final BasicStroke HILITE_RECT_OUTLINE_STROKE = 
        new BasicStroke(1f);
    
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
    

    /**If set to true the plotter paints the outline of the bars. The outline
     * is always painted for highlighted blocks!.*/
    private boolean m_showElementOutlines = false;

    /**If set to <code>true</code> the bar labels are displayed vertical 
     * otherwise they are displayed horizontal.*/
    private boolean m_showLabelVertical = true;

    /**The label display policy defines for which bars the labels should be
     * displayed.*/
    private LabelDisplayPolicy m_labelDisplayPolicy = 
        LabelDisplayPolicy.getDefaultOption();
    
    /**
     * Constructor for class HistogramDrawingPane.
     * @param props the {@link AbstractHistogramProperties} panel
     */
    protected HistogramDrawingPane(final AbstractHistogramProperties props) {
        super();
        m_properties = props;
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
     * @return <code>true</code> if the bar outline should be also shown for
     * none highlighted blocks
     */
    public boolean isShowElementOutline() {
        return m_showElementOutlines;
    }
    
    /**
     * @param showBarOutline set to <code>true</code> if the outline of the
     * bars should be also shown for not highlighted bars
     */
    public void setShowElementOutline(final boolean showBarOutline) {
        if (showBarOutline != m_showElementOutlines) {
            m_showElementOutlines = showBarOutline;
            repaint();
        }
    }
    

    /**
     * @param showLabelVertical if <code>true</code> the bar labels are 
     * displayed vertical otherwise horizontal.
     */
    public void setShowLabelVertical(final boolean showLabelVertical) {
        if (m_showLabelVertical  != showLabelVertical) {
            m_showLabelVertical = showLabelVertical;
            repaint();
        }
    }

    /**
     * @param labelDisplayPolicy the display policy
     */
    public void setLabelDisplayPolicy(
            final LabelDisplayPolicy labelDisplayPolicy) {
        if (m_labelDisplayPolicy != labelDisplayPolicy) {
            m_labelDisplayPolicy = labelDisplayPolicy;
            repaint();
        }
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
        m_labelDisplayPolicy = LabelDisplayPolicy.getDefaultOption();
        m_showElementOutlines = false;
        m_showLabelVertical = true;
        m_baseLine = null;
        m_gridLines = null;
        m_vizModel = null;
        m_infoMsg = null;
    }
    // **********************************************
    /*--------- the drawing methods ----------------*/
    // **********************************************

    /**
     * @see AbstractDrawingPane#paintContent(java.awt.Graphics)
     */
    @Override
    public void paintContent(final Graphics g) {
        LOGGER.debug("Entering paintContent(g) of class HistogramDrawingPane.");
        final Graphics2D g2 = (Graphics2D)g;
        final Rectangle bounds = getBounds();
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
            drawMessage(g2, msg, bounds);
            return;
        }
        if (m_updatePropertiesPanel && m_properties != null) {
            m_properties.updateHistogramSettings(vizModel);
            m_updatePropertiesPanel = false;
        }
//      check if we have to draw the grid lines
        if (m_gridLines != null) {
            for (int gridLine : m_gridLines) {
                paintHorizontalLine(g2, 0, gridLine,
                        (int) bounds.getWidth(), GRID_LINE_COLOR, 
                        GRID_LINE_STROKE);
            }
        }
        //get all variables which are needed multiple times
        final AggregationMethod aggrMethod = vizModel.getAggregationMethod();
        final Collection<ColorColumn> aggrColumns = 
            vizModel.getAggrColumns();
        final HistogramLayout layout = vizModel.getHistogramLayout();
        //if the user has selected more then one aggregation column we have to
        //draw the bar outline to how him which bar belongs to which aggregation
        //column
        final boolean drawBarOutline = (aggrColumns != null 
            && aggrColumns.size() > 1)
            || HistogramLayout.SIDE_BY_SIDE.equals(
                m_vizModel.getHistogramLayout());
        
        // loop over all bins and paint them
        for (BinDataModel bin : vizModel.getBins()) {
            if (!bin.isDrawBar()) {
                //the bars doen't fit in this bin so we have to 
                //fill the complete bin in black to show it to the user
                drawBlock(g2, bin.getBinRectangle(), 
                        OVERLOADED_ELEMENT_FILLING, 0.8f);
                continue;
            }
            final Collection<BarDataModel> bars = bin.getBars();
            for (BarDataModel bar : bars) {
                if (drawBarOutline) {
                    //draw the outline of the bar if we have multiple
                    //aggregation columns
                    drawBlock(g2, bar.getBarRectangle(), 
                            bar.getColor(), 0.2f);
                }
                if (bar.isDrawElements()) {
                    drawElements(g2, bar.getElements(), m_showElementOutlines);
                } else {
                    //the elements doen't fit in this bar so we have to 
                    //fill the complete bar in black to show it to the user
                    final Rectangle barRectangle = bar.getBarRectangle();
                    drawBlock(g2, barRectangle, 
                            OVERLOADED_ELEMENT_FILLING, 0.8f);
                    if (bar instanceof InteractiveBarDataModel) {
                        InteractiveBarDataModel interactiveBar = 
                            (InteractiveBarDataModel)bar;
                        drawHiliteRect(g2, interactiveBar.getHiliteRectangle());
                    }
                    if (bar.isSelected()) {
                        drawRectangle(g2, barRectangle, 
                            ELEMENT_SELECTED_OUTLINE_COLOR, 
                            ELEMENT_SELECTED_OUTLINE_STROKE);
                    }
                }
                //draw the bar label at last to have them on top
                drawLabels(g2, bar, aggrMethod, layout, bounds);
            } //end of bar loop
            //draw the outline of the bin to debug in multiple 
            //aggregation column mode
            if (aggrColumns != null && aggrColumns.size() > 1) {
                drawRectangle(g2, bin.getBinRectangle(), Color.ORANGE, 
                        GRID_LINE_STROKE);
            }
        } // end of the bin loop
//      check if we have to draw the base line
        if (m_baseLine != null) {
            paintHorizontalLine(g2, 0, m_baseLine.intValue(),
                    (int) bounds.getWidth(), BASE_LINE_COLOR, 
                    BASE_LINE_STROKE);
        }
        LOGGER.debug("Exiting paintContent(g) of class HistogramDrawingPane.");
        return;
    }


    /**
     * Draws the given message in the center of the given rectangle.
     * @param g2
     * @param msg
     * @param the size of the panel to draw the message on
     */
    private static void drawMessage(final Graphics2D g2, final String msg,
            final Rectangle bounds) {
        //save the original settings
        final Font origFont = g2.getFont();
        g2.setFont(INFO_MSG_FONT);
        final FontMetrics metrics = g2.getFontMetrics();
        final int textWidth = metrics.stringWidth(msg);
        final int textHeight = metrics.getHeight();
        //get the basic rectangle we have to draw in
        int textX = (int)bounds.getCenterX() - (textWidth / 2);
        int textY = (int)bounds.getCenterY() - (textHeight / 2);
        if (textX < 0) {
            textX = 0;
        }
        if (textY < 0) {
            textY = 0;
        }
        g2.drawString(msg, textX, textY);
        //set the original settings
        g2.setFont(origFont);
    }

    /**
     * Handles the label drawing.
     * @param g2 the graphics object
     * @param bar the bar for which the label(s) should be drawn
     * @param aggrMethod the aggregation method to get the right label
     * @param layout the current layout to decide if the label is per
     * element or per bar
     * @param bounds the surrounding pane on which to draw
     */
    private void drawLabels(final Graphics2D g2, final BarDataModel bar, 
            final AggregationMethod aggrMethod, final HistogramLayout layout,
            final Rectangle bounds) {
        if (LabelDisplayPolicy.ALL.equals(
                m_labelDisplayPolicy)
                || (LabelDisplayPolicy.SELECTED.equals(
                        m_labelDisplayPolicy) && bar.isSelected())) {
            if (HistogramLayout.STACKED.equals(layout) 
                    || !bar.isDrawElements()) {
                paintLabel(g2, bar.getBarRectangle(), 
                        bar.getAggregationValue(aggrMethod), aggrMethod, 
                        bounds, m_showLabelVertical);
            } else if (HistogramLayout.SIDE_BY_SIDE.equals(layout)) {
                //paint a label for each element after painting
                //the elements itself to have them in the front
                for (BarElementDataModel element : bar.getElements()) {
                    if (element.isSelected()
                            || LabelDisplayPolicy.ALL.equals(
                                    m_labelDisplayPolicy)) {
                        final double aggrVal = 
                            element.getAggregationValue(aggrMethod);
                        paintLabel(g2, element.getElementRectangle(), 
                                aggrVal, aggrMethod, bounds, 
                                m_showLabelVertical);
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
        for (BarElementDataModel element : elements) {
            final Color elementColor = element.getColor();
            //draw the element itself first
            final Rectangle elementRect = 
                element.getElementRectangle();
//            drawBlock(g2, elementRect, elementColor);
//          draw the surrounding rectangles after the block
            if (element.isSelected()) {
                drawBlock(g2, elementRect, elementColor);
                drawRectangle(g2, elementRect, 
                        ELEMENT_SELECTED_OUTLINE_COLOR, 
                        ELEMENT_SELECTED_OUTLINE_STROKE);
            } else {
                drawBlock(g2, elementRect, elementColor, 0.8f);
                if (showElementOutlines) {
                    drawRectangle(g2, elementRect, 
                            ELEMENT_OUTLINE_COLOR, 
                            ELEMENT_OUTLINE_STROKE);
                }
            }
            if (element instanceof InteractiveBarElementDataModel) {
                InteractiveBarElementDataModel interactiveElement = 
                    (InteractiveBarElementDataModel)element;
//              draw the hilite rectangle
                final Rectangle hiliteRect = 
                    interactiveElement.getHilitedRectangle();
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
            final Rectangle hiliteRect) {
        if (hiliteRect != null) {
            drawBlock(g2, hiliteRect, HILITE_RECT_BGR_COLOR);
            //always draw the hilite borders to make them visible
            //even if the bar has the same color like the hilite color
            //but only if the complete rectangle is wider than the 
            //stroke
            if (hiliteRect.getWidth() 
                    > HILITE_RECT_OUTLINE_STROKE.getLineWidth()) {
                drawRectangle(g2, hiliteRect, 
                        HILITE_RECT_OUTLINE_COLOR, 
                        HILITE_RECT_OUTLINE_STROKE);
            }
        }
    }

   /**
     * Draws a horizontal line starting at the given x/y offset with 
     * the given length.
     * @param g2 the graphics object to use
     * @param xOffset the x offset of the line
     * @param yOffset the y offset of the line
     * @param lineWidth the width of the line
     * @param color the drawing color
     * @param stroke the stroke to use
     */
    private static void paintHorizontalLine(final Graphics2D g2, 
            final int xOffset, final int yOffset, final int lineWidth, 
            final Color color, final BasicStroke stroke) {
        // save the original settings
        final Stroke origStroke = g2.getStroke();
        final Color origColor = g2.getColor();
        g2.setColor(color);
        g2.setStroke(stroke);
        g2.drawLine(xOffset, yOffset, lineWidth, yOffset);
        //set the original settings
        g2.setStroke(origStroke);
        g2.setColor(origColor);
    }
    /**
     * Draws a filled rectangle without a border and default transparency.
     * @param g2 the graphic object
     * @param rect the rectangle to fill
     * @param paint the filling color or TexturePaint
     */
    private static void drawBlock(final Graphics2D g2, final Rectangle rect,
            final Paint paint) {
        drawBlock(g2, rect, paint, 1.0f);
    }
    
    /**
     * Draws a filled rectangle without a border.
     * @param g2 the graphic object
     * @param rect the rectangle to fill
     * @param paint the filling color or TexturePaint
     * @param alpha the transparency
     */
    private static void drawBlock(final Graphics2D g2, final Rectangle rect,
            final Paint paint, final float alpha) {
        if (rect == null) {
            return;
        }
        // save the original settings
        final Paint origPaint = g2.getPaint();
        final Composite originalComposite = g2.getComposite();
        //draw the color block
        g2.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, alpha));
        g2.setPaint(paint);
        g2.fill(rect);
        //set the old settings
        g2.setPaint(origPaint);
        g2.setComposite(originalComposite);
    }

    /**
     * Draws an empty rectangle.
     * 
     * @param g2 the graphics object
     * @param rect the rectangle to draw
     * @param color the {@link Color} of the rectangle border
     * @param stroke the {@link BasicStroke} to use
     */
    private static void drawRectangle(final Graphics2D g2, 
            final Rectangle rect, final Color color, 
            final BasicStroke stroke) {
        if (rect == null) {
            return;
        }
        final Stroke origStroke = g2.getStroke();
        final Paint origPaint = g2.getPaint();
        final Rectangle borderRect = 
            calculateBorderRect(rect, stroke);
        g2.setStroke(stroke);
        g2.setPaint(color);
        g2.draw(borderRect);
        //set the old settings
        g2.setStroke(origStroke);
        g2.setPaint(origPaint);
    }
    
    /**
     * Calculates the size of the rectangle with the given stroke.
     * @param rect the original size of the rectangle
     * @param stroke the stroke which will be used to draw the rectangle
     * @return the {@link Rectangle} to draw
     */
    private static Rectangle calculateBorderRect(final Rectangle rect, 
            final BasicStroke stroke) {
        final int strokeWidth = (int)stroke.getLineWidth();
        final int halfStrokeWidth = strokeWidth / 2;
        final int newX = (int) rect.getX() + halfStrokeWidth;
        final int newY = (int) rect.getY() + halfStrokeWidth;
        int newWidth = (int) rect.getWidth() - strokeWidth;
        int newHeight = (int) rect.getHeight() - strokeWidth;
        //check for negative values
        if (newWidth <= 0) {
            newWidth = 1;
        }
        if (newHeight <= 0) {
            newHeight = 1;
        }
        final Rectangle strokeRect = 
            new Rectangle(newX, newY, newWidth, newHeight);
        return strokeRect;
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
            final Rectangle borderRect, final double aggrVal, 
            final AggregationMethod aggrMethod, final Rectangle drawingSpace,
            final boolean showVertical) {
        if (borderRect == null) {
            return;
        }
        final String label = 
            createLabel(aggrVal, aggrMethod);
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
                    - textWidth / 2) - AGGR_VAL_LABEL_SPACER;
        } else {
            textY = (int)(borderRect.getY() + textWidth / 2) 
            + AGGR_VAL_LABEL_SPACER;
        }
        final double screenHeight = drawingSpace.getHeight();
        //check if the label is outside of the drawing space
        if (textY + textWidth / 2 > screenHeight) {
            textY = (int)(screenHeight - textWidth / 2 - AGGR_VAL_LABEL_SPACER);
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
     * @param aggrVal the value to use as label
     * @param aggrMethod the method used to get the aggregation value
     * @return the rounded aggregation value as <code>String</code> label
     */
    private static String createLabel(final double aggrVal, 
            final AggregationMethod aggrMethod) {
        // return Double.toString(aggrVal);
        if (aggrMethod.equals(AggregationMethod.COUNT)) {
            return AGGREGATION_LABEL_FORMATER_COUNT.format(aggrVal);
        } 
        // the given doubleVal is less then zero
        char[] interval = Double.toString(aggrVal).toCharArray();
        StringBuffer decimalFormatBuf = new StringBuffer();
        boolean digitFound = false;
        int digitCounter = 0;
        int positionCounter = 0;
        boolean dotFound = false;
        for (int length = interval.length; positionCounter < length
                && digitCounter <= NO_OF_LABEL_DIGITS; positionCounter++) {
            char c = interval[positionCounter];
            if (c == '.') {
                decimalFormatBuf.append(".");
                dotFound = true;
            } else {
                if (c != '0' || digitFound) {
                    digitFound = true;
                    if (dotFound) {
                        digitCounter++;
                    }
                }
                if (digitCounter <= NO_OF_LABEL_DIGITS) {
                    decimalFormatBuf.append("#");
                }
            }
        }
        DecimalFormat df = new DecimalFormat(decimalFormatBuf.toString());
        String resultString = df.format(aggrVal);
        return resultString;
    }
}
