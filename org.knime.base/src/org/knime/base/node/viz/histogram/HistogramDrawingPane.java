/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.knime.base.node.viz.plotter2D.AbstractDrawingPane;
import org.knime.core.data.DataCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.property.hilite.HiLiteHandler;

/**
 * The view class of a Histogram visualisation. It simply uses the given
 * information of the <code>BarVisModel</code> <code>Collection</code> to
 * display the bars on the screen.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class HistogramDrawingPane extends AbstractDrawingPane {

    private static final long serialVersionUID = 7881989778083295425L;

    /**This stroke is used to draw the rectangle around each group of 
     * unhighlighted rows per color.*/
    private static final BasicStroke BLOCK_UNHILITE_STROKE = 
        new BasicStroke(1f);

    /**This stroke is used to draw the rectangle around each group of 
     * highlighted rows per color.*/
    private static final BasicStroke BLOCK_HILITE_STROKE = 
        new BasicStroke(2f);
    
    /** Defines the stroke of the rectangle which surrounds the bar label. */
    private static final BasicStroke VALUE_RECT_STROKE = new BasicStroke(1f);

    /** Defines the color of the stroke of the rectangle which surrounds 
     * the bar label. */
    private static final Color VALUE_RECT_STROKE_COLOR = Color.LIGHT_GRAY;
    
    /**Defines the background color of the rectangle which surrounds 
     * the bar label. */
    private static final Color VALUE_RECT_BGR_COLOR = Color.WHITE;
    
    /** Defines the font of the bar labels. */
    private static final Font BAR_VALUE_FONT = 
        new Font("Arial", Font.PLAIN, 12);

    /**
     * Defines the space between the bottom of the plotter and the aggregation
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
    
    /** Defines the font of the information message which is displayed. */
    private static final Font INFO_MSG_FONT = new Font("Arial", Font.PLAIN, 16);

    /**
     * Holds the <code>BarVisModel</code> objects to draw on the screen the
     * key is the caption of the bar and the value the bar itself.
     */
    private Hashtable<String, BarVisModel> m_bars;

    private HiLiteHandler m_hiLiteHandler;

    /**
     * Information message. If not <code>null</code> no bars will be drawn
     * only this message will be displayed.
     */
    private String m_infoMsg = null;
    
    /**If set the base line is drawn at this screen position.*/
    private Integer m_baseLine = null;
    
    /**
     * Constructor for class HistogramDrawingPane.
     * 
     * @param handler the {@link HiLiteHandler} to use
     */
    protected HistogramDrawingPane(final HiLiteHandler handler) {
        super();
        this.m_hiLiteHandler = handler;
    }

    /**
     * @param visBars the <code>BarVisModel</code> objects which represent the
     *            Histogram.
     */
    public void setVisBars(final Hashtable<String, BarVisModel> visBars) {
        m_bars = visBars;
    }

    /**
     * @return the <code>BarVisModel</code> <code>Hashtable</code> with the
     *         caption of the bar as key and the bar itself as value
     */
    protected Hashtable<String, BarVisModel> getVisBars() {
        return m_bars;
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
     * @param hiLiteHandler the hiLiteHandler to set
     */
    public void setHiLiteHandler(final HiLiteHandler hiLiteHandler) {
        m_hiLiteHandler = hiLiteHandler;
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
     * Resets the internal values of the histogram drawing pane to their default
     * values.
     */
    public void reset() {
        m_baseLine = null;
        m_bars = null;
        m_infoMsg = null;
        m_hiLiteHandler = null;
    }
    // **********************************************
    /*--------- the drawing methods ----------------*/
    // **********************************************
    /**
     * @see org.knime.base.node.viz.plotter2D.AbstractDrawingPane
     *      #paintPlotDrawingPane(java.awt.Graphics)
     */
    @Override
    protected void paintPlotDrawingPane(final Graphics g) {
        if (m_bars == null) {
            return;
        }
        final Graphics2D g2 = (Graphics2D)g;
        //check if we have to display an information message
        if (m_infoMsg != null) {
            //save the original settings
            final Font origFont = g2.getFont();
            g2.setFont(INFO_MSG_FONT);
            final FontMetrics metrics = g2.getFontMetrics();
            final int textWidth = metrics.stringWidth(m_infoMsg);
            final int textHeight = metrics.getHeight();
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
        //check if we have to draw the base line
        if (m_baseLine != null) {
            paintBaseLine(g2, m_baseLine.intValue(), 
                    (int) getBounds().getWidth());
        }
// loop over all bars and paint them
        for (BarVisModel bar : m_bars.values()) {
            final Rectangle barRect = bar.getRectangle();
            int noOfRows = bar.getNumberOfRows();
            double heightPerRow = 0;
            if (noOfRows > 0) {
                heightPerRow = barRect.getHeight() / noOfRows;
            }
            //set the start point for the y direction of the current bar
            int startY = (int)(barRect.getY());
            // Loop through all available colors of this bar to calculate
            //and draw the blocks per color
            for (ColorAttr colorAttr : bar.getSortedColors()) {
                Collection<RowKey> rowKeys = bar.getRowsByColorAttr(colorAttr);
                int noOfHiLite = 0;
                int noOfNotHiLite = 0;
                // loop through all rows of the current color to count the
                // highlight and unhighlight rows to calculate the height for
                // both separate blocks
                for (RowKey rowKey : rowKeys) {
                    if (m_hiLiteHandler != null
                            && m_hiLiteHandler.isHiLit(rowKey.getId())) {
                        noOfHiLite++;
                    } else {
                        noOfNotHiLite++;
                    }
                }
                if (noOfHiLite > 0) {
                    startY = paintColorBlock(g2, bar, heightPerRow, startY, 
                            colorAttr, noOfHiLite, BLOCK_HILITE_STROKE,
                            true);
                }
                if (noOfNotHiLite > 0) {
                    startY = paintColorBlock(g2, bar, heightPerRow, startY, 
                            colorAttr, noOfNotHiLite, BLOCK_UNHILITE_STROKE,
                            false);
                }
            } // end of the color loop for one bar 

            if (bar.isSelected()) {
                paintBarValue(g2, bar);
            }
            
        } // end of the bar loop
        return;
    }

    /**
     * Draws the base line.
     * @param g2 the graphics object to use
     * @param yOffset the y offset of the line
     * @param screenWidth the width of the line
     */
    private static void paintBaseLine(final Graphics2D g2, final int yOffset,
            final int screenWidth) {
        // save the original settings
        final Stroke origStroke = g2.getStroke();
        final Color origColor = g2.getColor();
        g2.setColor(BASE_LINE_COLOR);
        g2.setStroke(BASE_LINE_STROKE);
        g2.drawLine(0, yOffset, screenWidth, yOffset);
        //set the original settings
        g2.setStroke(origStroke);
        g2.setColor(origColor);
    }

    /**
     * Calculates the size of a bar block and draws it.
     * <p>
     * Each bar consists of one or more blocks. A block is the summary of rows
     * which have the same color attribute <b>and</b> highlight status.
     * @param g2 the graphics object to use
     * @param bar the bar to paint
     * @param heightPerRow the height per row
     * @param startY the y coordinate
     * @param colorAttr the color attribute to use
     * @param noOfRows number of rows to paint
     */
    private static int paintColorBlock(final Graphics2D g2, 
            final BarVisModel bar, final double heightPerRow, final int startY, 
            final ColorAttr colorAttr, final int noOfRows, 
            final BasicStroke stroke, final boolean isHilite) {
        // save the original settings
        final Stroke origStroke = g2.getStroke();
        final Paint origPaint = g2.getPaint();
        final Rectangle barRect = bar.getRectangle();
        final Rectangle fillRect = calculateFillingRect(
                heightPerRow, noOfRows, barRect, startY);
        //first fill the rectangle...
        g2.setStroke(stroke);
        g2.setPaint(colorAttr.getColor(bar.isSelected(), isHilite));
        g2.fill(fillRect);
        //... and then draw the border
        final Rectangle borderRect = 
            calculateBorderRect(fillRect, stroke);
        final Color borderColor = 
            colorAttr.getBorderColor(bar.isSelected(), isHilite);
        g2.setPaint(borderColor);
        g2.draw(borderRect);
        int newStartY = (int)(startY + fillRect.getHeight());
        //set the old settings
        g2.setStroke(origStroke);
        g2.setPaint(origPaint);
        return newStartY;
    }

    /**
     * Paints the actual value (count, average, ...) of the bar.
     * @param g2 the graphics object to use
     * @param bar the bar which value should be paint
     */
    private static void paintBarValue(final Graphics2D g2, 
            final BarVisModel bar) {
        // save the original settings
        final AffineTransform origTransform = g2.getTransform();
        final Font origFont = g2.getFont();
        final Paint origPaint = g2.getPaint();
        final Stroke origStroke = g2.getStroke();
        final Rectangle barRect = bar.getRectangle();
        final AffineTransform verticalTrans = new AffineTransform();
        // if the bar is selected show the aggregation value
        final String label = bar.getLabel();
        g2.setFont(BAR_VALUE_FONT);
        final FontMetrics metrics = g2.getFontMetrics();
        int textX = (int)(barRect.getX() + (barRect.getWidth() / 2));
        int textWidth = metrics.stringWidth(label);
        textX -= textWidth / 2;
        // avoid drawing the aggregation value outside of the display
        if (textX < 1) {
            textWidth += Math.abs(textX - 1);
            textX = 1;
        }
        int textY = 0;
        if (bar.getAggregationValue() > 0) {
            textY =  (int)(barRect.getY() + barRect.getHeight() 
                    - textWidth / 2) - AGGR_VAL_LABEL_SPACER;
        } else {
            textY = (int)(barRect.getY() + textWidth / 2) 
            + AGGR_VAL_LABEL_SPACER;
        }
        final int textHeight = metrics.getHeight();
        // calculate the text background rectangle
        final int bgX = textX - AGGR_VAL_BG_SPACER;
        final int bgY = textY - textHeight - AGGR_VAL_BG_SPACER;
        final Rectangle textBackGroundRec = new Rectangle(bgX, bgY, 
                textWidth + 2 * AGGR_VAL_BG_SPACER, 
                textHeight + 2 * AGGR_VAL_BG_SPACER);
        // rotate 90 degree around the center of the text rectangle
        verticalTrans.rotate(Math.toRadians(90), textBackGroundRec
                .getCenterX(), textBackGroundRec.getCenterY());
        g2.transform(verticalTrans);
        g2.setPaint(VALUE_RECT_BGR_COLOR);
        g2.fill(textBackGroundRec);
        g2.setPaint(VALUE_RECT_STROKE_COLOR);
        g2.setStroke(VALUE_RECT_STROKE);
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
     * Calculates the size of the rectangle to fill.
     * @param heightPerRow the height per row
     * @param noOfRows the number of rows
     * @param maxSize the maximum size of the rectangle
     * @param startY the start coordinate on the Y axis.
     * @return the {@link Rectangle} to fill
     */
    private static Rectangle calculateFillingRect(final double heightPerRow, 
            final int noOfRows, final Rectangle maxSize, final int startY) {
        int height = (int)Math.ceil(heightPerRow * noOfRows);
        // avoid rounding errors
        if (startY + height 
                > maxSize.getY() + maxSize.getHeight()) {
            final int dif = (startY + height)
                    - (int)(maxSize.getY() + maxSize.getHeight());
            height = height - dif;
        }
        final Rectangle colorRect = new Rectangle(
                (int)maxSize.getX(), startY, (int)maxSize.getWidth(), 
                height);
        return colorRect;
    }
    
    /**
     * Calculates the size of the rectangle with the given stroke.
     * @param rect the original size of the rectangle
     * @param stroke the stroke which will be used to draw the rectangle
     * @return the {@link Rectangle} to draw
     */
    private static Rectangle calculateBorderRect(final Rectangle rect, 
            final BasicStroke stroke) {
        final int width = (int)stroke.getLineWidth();
        final int halfWidth = width / 2;
        final Rectangle strokeRect = new Rectangle((int) rect.getX() 
                + halfWidth, (int) rect.getY() + halfWidth, 
                (int) rect.getWidth() - width, (int) rect.getHeight() - width);
        return strokeRect;
    }
    
    /**
     * @return the width of the stroke used to outline the bars in the
     *         visualisation
     */
    protected static double getBarStrokeWidth() {
        return BLOCK_UNHILITE_STROKE.getLineWidth();
        /*return Math.max(BAR_OUTLINE_BASIC_STROKE.getLineWidth(),
                BAR_OUTLINE_NEGATIVE_STROKE.getLineWidth());*/
    }

    // **********************************************
    // *******Selection methods ***********************
    // **********************************************
    /**
     * @return all row keys of rows which are belong to a selected bar
     */
    protected Set<DataCell> getKeys4SelectedBars() {
        if (m_bars == null) {
            return null;
        }
        Set<DataCell> selectedRows = new HashSet<DataCell>();
        for (BarVisModel bar : m_bars.values()) {
            if (bar.isSelected()) {
                selectedRows.addAll(bar.getRowKeys());
            }
        }
        return selectedRows;
    }

    /**
     * @see org.knime.base.node.viz.plotter2D.AbstractDrawingPane
     *      #clearSelection()
     */
    @Override
    protected void clearSelection() {
        if (m_bars == null) {
            return;
        }
        for (BarVisModel bar : m_bars.values()) {
            bar.setSelected(false);
        }
    }

    /**
     * @see org.knime.base.node.viz.plotter2D.AbstractDrawingPane
     *      #selectElementsInDragTangle(int, int, int, int)
     */
    @Override
    protected void selectElementsInDragTangle(final int mouseDownX,
            final int mouseDownY, final int mouseUpX, final int mouseUpY) {
        if (m_bars == null) {
            return;
        }
        int x = mouseDownX;
        int y = mouseDownY;
        int width = mouseUpX - mouseDownX;
        int height = mouseUpY - mouseDownY;
        if (mouseDownX > mouseUpX) {
            x = mouseUpX;
            width = mouseDownX - mouseUpX;
        }
        if (mouseDownY > mouseUpY) {
            y = mouseUpY;
            height = mouseDownY - mouseUpY;
        }
        Rectangle rect = new Rectangle(x, y, width, height);
        for (BarVisModel bar : m_bars.values()) {
            if (bar.screenRectOverlapping(rect)) {
                bar.setSelected(!bar.isSelected());
            } else {
                bar.setSelected(false);
            }
        }
        return;
    }

    /**
     * @see org.knime.base.node.viz.plotter2D.AbstractDrawingPane
     *      #toggleSelectionAt(int, int)
     */
    @Override
    protected void toggleSelectionAt(final int x, final int y) {
        if (m_bars == null) {
            return;
        }
        for (BarVisModel bar : m_bars.values()) {
            if (bar.getRectangle().contains(x, y)) {
                bar.setSelected(!bar.isSelected());
                break;
            }
        }
    }

    /**
     * @see org.knime.base.node.viz.plotter2D.AbstractDrawingPane
     *      #getNumberSelectedElements()
     */
    @Override
    public int getNumberSelectedElements() {
        if (m_bars == null) {
            return 0;
        }
        int noOfSelected = 0;
        for (BarVisModel bar : m_bars.values()) {
            if (bar.isSelected()) {
                noOfSelected += bar.getNumberOfRows();
            }
        }
        return noOfSelected;
    }
}
