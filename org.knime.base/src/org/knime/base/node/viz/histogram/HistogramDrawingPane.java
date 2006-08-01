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
 * If you have any quesions please contact the copyright holder:
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
import java.awt.Rectangle;
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
    /** Defines the basic stroke which is used for the border of each bar. */
    private static final BasicStroke BAR_OUTLINE_BASIC_STROKE = new BasicStroke(
            2f);

    /**
     * Defines the color of the outline rectangle with a positive aggregation
     * value.
     */
    private static final Color BAR_OUTLINE_BASIC_COLOR = Color.BLACK;

    /**
     * Defines the stroke which is used for the border of bars with a negative
     * aggregation value.
     */
    private static final BasicStroke BAR_OUTLINE_NEGATIVE_STROKE = new BasicStroke(
            6f);

    /**
     * Defines the color of the outline rectangle with a negative aggregation
     * value.
     */
    private static final Color BAR_OUTLINE_NEGATIVE_COLOR = Color.RED;

    /** Defines the stroke of the rectangle which surrounds the bar label. */
    private static final BasicStroke LABEL_RECT_STROKE = new BasicStroke(1f);

    /** Defines the color of the rectangle which surrounds the bar label. */
    private static final Color LABEL_RECT_COLOR = Color.LIGHT_GRAY;

    /** Defines the font of the bar labels. */
    private static final Font BAR_LABEL_FONT = new Font("Arial", Font.PLAIN, 12);

    /** Defines the font of the info message which is displayed. */
    private static final Font INFO_MSG_FONT = new Font("Arial", Font.PLAIN, 16);

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

    /**
     * Constructor for class HistogramDrawingPane.
     * 
     * @param handler
     */
    protected HistogramDrawingPane(final HiLiteHandler handler) {
        super();
        this.m_hiLiteHandler = handler;
    }

    /**
     * @param visBars the <code>BarVisModel</code> objects which represent the
     *            Histogram.
     */
    protected void setVisBars(final Hashtable<String, BarVisModel> visBars) {
        m_bars = visBars;
    }

    /**
     * @return the <code>BarVisModel</code> <code>Hashtable</code> with the
     *         caption of the bar as key and the bar itself as value
     */
    protected Hashtable<String, BarVisModel> getVisBars() {
        return m_bars;
    }

    // **********************************************
    /*--------- the drawing methods ----------------*/
    // **********************************************
    /**
     * @see org.knime.dev.node.view.plotter2D.AbstractDrawingPane
     *      #paintPlotDrawingPane(java.awt.Graphics)
     */
    @Override
    protected void paintPlotDrawingPane(final Graphics g) {
        if (m_bars == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D)g;
        if (m_infoMsg != null) {
            g2.setFont(INFO_MSG_FONT);
            FontMetrics metrics = g2.getFontMetrics();
            int textWidth = metrics.stringWidth(m_infoMsg);
            int textHeight = metrics.getHeight();
            Rectangle basicRect = getBounds();
            int textX = (int)basicRect.getCenterX() - (textWidth / 2);
            int textY = (int)basicRect.getCenterY() - (textHeight / 2);
            if (textX < 0) {
                textX = 0;
            }
            if (textY < 0) {
                textY = 0;
            }
            g2.drawString(m_infoMsg, textX, textY);
            return;
        }
        // loop over all bars and draw them
        for (BarVisModel bar : m_bars.values()) {
            BasicStroke rectStroke = BAR_OUTLINE_BASIC_STROKE;
            Color rectColor = BAR_OUTLINE_BASIC_COLOR;
            // check for negative values
            if (bar.getAggregationValue() < 0) {
                rectStroke = BAR_OUTLINE_NEGATIVE_STROKE;
                rectColor = BAR_OUTLINE_NEGATIVE_COLOR;
            }
            // I have to remove the stroke width from the bar dimensions.
            Rectangle origBarRect = bar.getRectangle();
            final int strokeWidth = (int)rectStroke.getLineWidth();
            Rectangle barRect = new Rectangle(origBarRect.x + strokeWidth,
                    origBarRect.y + strokeWidth, origBarRect.width
                            - strokeWidth, origBarRect.height - strokeWidth);
            int noOfRows = bar.getNumberOfRows();
            double heightPerRow = barRect.getHeight() / noOfRows;
            // paint the shape of the bar first
            g2.setStroke(rectStroke);
            g2.setPaint(rectColor);
            g2.draw(barRect);
            // fill the bar with the color from the color manager or with the
            // default color
            int startY = (int)(barRect.getY()); // + (rectStroke.getLineWidth()
            // / 2));
            int startX = (int)(barRect.getX()); // + (rectStroke.getLineWidth()
            // / 2));
            int colorWidth = (int)(barRect.getWidth());
            // - (rectStroke.getLineWidth() / 2));
            // Loop through all available colors of this bar to display them
            // inside the drawing rectangle
            for (ColorAttr colorAttr : bar.getSortedColors()) {
                Collection<RowKey> rowKeys = bar.getRowsByColorAttr(colorAttr);
                int noOfHiLite = 0;
                int noOfNotHiLite = 0;
                // loop through all rows of the current color to count the
                // highlight and unhighlight rows to calculate the height for
                // both
                for (RowKey rowKey : rowKeys) {
                    if (m_hiLiteHandler != null
                            && m_hiLiteHandler.isHiLit(rowKey.getId())) {
                        noOfHiLite++;
                    } else {
                        noOfNotHiLite++;
                    }
                }
                if (noOfHiLite > 0) {
                    // draw the highlight rows in a rectangle
                    g2.setPaint(colorAttr.getColor(bar.isSelected(), true));
                    int height = (int)Math.round(heightPerRow * noOfHiLite);
                    // avoid rounding errors
                    if (startY + height > barRect.getY() + barRect.getHeight()) {
                        int dif = (startY + height)
                                - (int)(barRect.getY() + barRect.getHeight());
                        height = height - dif;
                    }
                    Rectangle colorRect = new Rectangle(startX, startY,
                            colorWidth, height);
                    g2.fill(colorRect);
                    startY += height;
                }
                if (noOfNotHiLite > 0) {
                    // draw the unhighlight rows in a rectangle
                    g2.setPaint(colorAttr.getColor(bar.isSelected(), false));
                    int height = (int)Math.ceil(heightPerRow * noOfNotHiLite);
                    // avoid rounding errors
                    if (startY + height > barRect.getY() + barRect.getHeight()) {
                        int dif = (startY + height)
                                - (int)(barRect.getY() + barRect.getHeight());
                        height = height - dif;
                    }
                    Rectangle colorRect = new Rectangle(startX, startY,
                            colorWidth, height);
                    g2.fill(colorRect);
                    startY += height;
                }

            } // end of the color loop for one bar

            if (bar.isSelected()) {
                // save the original transformation
                AffineTransform origTransform = g2.getTransform();
                AffineTransform verticalTrans = new AffineTransform();
                // if the bar is selected show the aggregation value
                String label = bar.getLabel();
                g2.setFont(BAR_LABEL_FONT);
                FontMetrics metrics = g2.getFontMetrics();
                int textX = (int)(barRect.getX() + (barRect.getWidth() / 2));
                int textWidth = metrics.stringWidth(label);
                textX -= textWidth / 2;
                // avoid drawing the aggregation value outside of the display
                if (textX < 1) {
                    textWidth += Math.abs(textX - 1);
                    textX = 1;
                }
                int textY = (int)(barRect.getY() + barRect.getHeight() - textWidth / 2)
                        - AGGR_VAL_LABEL_SPACER;
                int textHeight = metrics.getHeight();
                // calculate the text background rectangle
                int bgX = textX - AGGR_VAL_BG_SPACER;
                int bgY = textY - textHeight - AGGR_VAL_BG_SPACER;
                Rectangle textBackGroundRec = new Rectangle(bgX, bgY, textWidth
                        + 2 * AGGR_VAL_BG_SPACER, textHeight + 2
                        * AGGR_VAL_BG_SPACER);
                // rotate 90 degree around the center of the text rectangle
                verticalTrans.rotate(Math.toRadians(90), textBackGroundRec
                        .getCenterX(), textBackGroundRec.getCenterY());
                g2.transform(verticalTrans);
                g2.setPaint(Color.WHITE);
                g2.fill(textBackGroundRec);
                g2.setPaint(LABEL_RECT_COLOR);
                g2.setStroke(LABEL_RECT_STROKE);
                g2.draw(textBackGroundRec);
                g2.setPaint(rectColor);
                g2.drawString(label, textX, textY);
                // set the original transformation
                g2.setTransform(origTransform);
            }
        } // end of the bar loop
        return;
    }

    /**
     * @return the width of the maximum stroke used to outline the bars in the
     *         visualisation
     */
    protected static double getMaxStrokeWidth() {
        return Math.max(BAR_OUTLINE_BASIC_STROKE.getLineWidth(),
                BAR_OUTLINE_NEGATIVE_STROKE.getLineWidth());
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
     * @see org.knime.dev.node.view.plotter2D.AbstractDrawingPane
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
     * @see org.knime.dev.node.view.plotter2D.AbstractDrawingPane
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
     * @see org.knime.dev.node.view.plotter2D.AbstractDrawingPane
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
     * @see org.knime.dev.node.view.plotter2D.AbstractDrawingPane
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
     * Resets the internal values of the histogram drawing pane to their default
     * values.
     */
    public void reset() {
        m_bars = null;
        m_infoMsg = null;
        m_hiLiteHandler = null;
    }
}
