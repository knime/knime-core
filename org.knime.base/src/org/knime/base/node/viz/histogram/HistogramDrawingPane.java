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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.text.DecimalFormat;
import java.util.Collection;

import org.knime.base.node.viz.histogram.datamodel.BarDataModel;
import org.knime.base.node.viz.histogram.datamodel.BarElementDataModel;
import org.knime.base.node.viz.histogram.datamodel.BinDataModel;
import org.knime.base.node.viz.histogram.datamodel.HistogramVizModel;
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
    
    /**Used to format the aggregation value for the aggregation method count.*/
    private static final DecimalFormat AGGREGATION_LABEL_FORMATER_COUNT = 
        new DecimalFormat("#");
    
    /**The number of digits to display for a label.*/
    private static final int NO_OF_LABEL_DIGITS = 2;
    
    /**This stroke is used to draw the rectangle around each element.*/
    private static final BasicStroke ELEMENT_OUTLINE_STROKE = 
        new BasicStroke(1f);
    
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
     * Holds the {@link HistogramVizModel} objects to draw.
     */
    private HistogramVizModel m_histoData;

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
     */
    protected HistogramDrawingPane() {
        super();
    }

    
    /**
     * @param histoData the {@link HistogramDataModel} objects to draw
     */
    public void setHistogramData(final HistogramVizModel histoData) {
        m_histoData = histoData;
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
        m_histoData = null;
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
        final Graphics2D g2 = (Graphics2D)g;
        if (m_histoData == null || m_histoData.getBins() == null) {
            //if we have no bins and no info message display a no bars info
            if (m_infoMsg == null) {
                m_infoMsg = "No bars to display";
            }
        }
        //check if we have to display an information message
        if (m_infoMsg != null) {
            //save the original settings
            final Font origFont = g2.getFont();
            g2.setFont(INFO_MSG_FONT);
            final FontMetrics metrics = g2.getFontMetrics();
            final int textWidth = metrics.stringWidth(m_infoMsg);
            final int textHeight = metrics.getHeight();
            //get the basic rectangle we have to draw in
            final Rectangle basicRect = getBounds();
            int textX = (int)basicRect.getCenterX() - (textWidth / 2);
            int textY = (int)basicRect.getCenterY() - (textHeight / 2);
            if (textX < 0) {
                textX = 0;
            }
            if (textY < 0) {
                textY = 0;
            }
            g2.drawString(m_infoMsg, textX, textY);
            //set the original settings
            g2.setFont(origFont);
            return;
        }
//      check if we have to draw the grid lines
        if (m_gridLines != null) {
            for (int gridLine : m_gridLines) {
                paintHorizontalLine(g2, 0, gridLine,
                        (int) getBounds().getWidth(), GRID_LINE_COLOR, 
                        GRID_LINE_STROKE);
            }
        }
        //check if we have to draw the base line
        if (m_baseLine != null) {
            paintHorizontalLine(g2, 0, m_baseLine.intValue(),
                    (int) getBounds().getWidth(), BASE_LINE_COLOR, 
                    BASE_LINE_STROKE);
        }
        final AggregationMethod aggrMethod = m_histoData.getAggregationMethod();
        final HistogramLayout layout = m_histoData.getHistogramLayout();
        final Collection<BinDataModel> bins = m_histoData.getBins();
// loop over all bins and paint them
        for (BinDataModel bin : bins) {
            final Collection<BarDataModel> bars = bin.getBars();
            for (BarDataModel bar : bars) {
                final Collection<BarElementDataModel> elements = 
                    bar.getElements();
                for (BarElementDataModel element : elements) {
                    final Color elementColor = element.getColor();
                    //draw the element itself first
                    final Rectangle elementRect = element.getElementRectangle();
                    drawBlock(g2, elementRect, elementColor);
                    //draw the hilite rectangle
                    final Rectangle hiliteRect = element.getHilitedRectangle();
                    drawBlock(g2, hiliteRect, HILITE_RECT_BGR_COLOR);
                    //always draw the hilite borders to make them visible
                    //even if the bar has the same color like the hilite color
                    drawRectangle(g2, hiliteRect, HILITE_RECT_OUTLINE_COLOR, 
                            HILITE_RECT_OUTLINE_STROKE);
                    //draw the surrounding rectangles at last
                    if (m_showElementOutlines) {
                        drawRectangle(g2, elementRect, 
                                ELEMENT_OUTLINE_COLOR, ELEMENT_OUTLINE_STROKE);
                    }
                    if (element.isSelected()) {
                        drawRectangle(g2, elementRect, 
                                ELEMENT_SELECTED_OUTLINE_COLOR, 
                                ELEMENT_SELECTED_OUTLINE_STROKE);
                    }
                } //end of element loop
                //draw the bar label
                if ((bar.isSelected() 
                        && LabelDisplayPolicy.SELECTED.equals(
                                m_labelDisplayPolicy)) 
                        || LabelDisplayPolicy.ALL.equals(
                                m_labelDisplayPolicy)) {
                    if (HistogramLayout.STACKED.equals(layout)) {
                        final double aggrVal = 
                            bar.getAggregationValue(aggrMethod);
                        paintLabel(g2, bar.getBarRectangle(), aggrVal, 
                                aggrMethod, getBounds(), m_showLabelVertical);
                    } else if (HistogramLayout.SIDE_BY_SIDE.equals(layout)) {
                        //paint a label for each element after painting
                        //the elements itself to have them in the front
                        for (BarElementDataModel element : elements) {
                            if (element.isSelected()
                                    || LabelDisplayPolicy.ALL.equals(
                                            m_labelDisplayPolicy)) {
                                final double aggrVal = 
                                    element.getAggregationValue(aggrMethod);
                                paintLabel(g2, element.getElementRectangle(), 
                                        aggrVal, aggrMethod, getBounds(), 
                                        m_showLabelVertical);
                            }
                        }
                    } else {
                        throw new IllegalArgumentException(
                                "Layout " + layout + " not supported");
                    }
                }
                //draw the outline of the bar to debug in side by side modus
                if (HistogramLayout.SIDE_BY_SIDE.equals(
                        m_histoData.getHistogramLayout())) {
                    final Color barColor = bar.getColor();
                    final Rectangle barRectangle = bar.getBarRectangle();
                    drawRectangle(g2, barRectangle, barColor, 
                            ELEMENT_OUTLINE_STROKE);
                }
                
            } //end of bar loop
            //draw the outline of the bin to debug in multiple 
            //aggregation column mode
            if (m_histoData.getAggrColumns().size() > 1) {
                final Rectangle binRectangle = bin.getBinRectangle();
                drawRectangle(g2, binRectangle, Color.ORANGE, 
                        GRID_LINE_STROKE);
            }
        } // end of the bin loop
        return;
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
     * Draws a filled rectangle without a border.
     * @param g2 the graphic object
     * @param rect the rectangle to fill
     * @param fillingColor the filling color
     */
    private static void drawBlock(final Graphics2D g2, final Rectangle rect,
            final Color fillingColor) {
        if (rect == null) {
            return;
        }
        // save the original settings
        final Stroke origStroke = g2.getStroke();
        final Paint origPaint = g2.getPaint();
        //draw the color block
        g2.setPaint(fillingColor);
        g2.fill(rect);
        //set the old settings
        g2.setStroke(origStroke);
        g2.setPaint(origPaint);
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
        if (aggrVal > 0) {
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
