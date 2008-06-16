/*
 * 
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * --------------------------------------------------------------------- *
 * 
 * 10-02-2007 Rosaria Silipo (created)
 */
package org.knime.timeseries.node.display;

import org.knime.core.data.DataCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;

/**
 * This is a helper class for BarChartPlotter and CandlestickPlotter. 
 * It keeps info (like the screen coordinates and color, the high, low, 
 * close and open price) for each data point that is plotted.
 * 
 * @author Rosaria Silipo
 */

public class FinancialDotInfo {

    // data used to plot that thing. Could change with user input.
    private ColorAttr m_color; // retrieved from the color handler

    private int m_xCoord; // the screen coordinates calculated depending on

    private int m_openPrice; 
    private int m_closePrice; 
    private int m_highPrice; 
    private int m_lowPrice; 
    
    private DataCell m_xDomainValue;
    
    private DataCell m_openPriceDomainValue;
    private DataCell m_closePriceDomainValue;
    private DataCell m_highPriceDomainValue;
    private DataCell m_lowPriceDomainValue;

    /**
     * Dot size, retrieved from the size handler of the data table. The dot size
     * is given in percent.
     */
    private double m_size; // the size attribute of the corresponding row
    
    private FinancialShapeFactory.Shape m_shape; // the shape attribute

    private boolean m_hilit; // the hilite status

    private RowKey m_rowID; // the associated row

    private int m_rowIndex; // the associated row index

    /**
     * Creates a new object storing the characteristics of one dot.
     * 
     * @param x the x coordinates of the point
     * @param openPrice 
     * @param closePrice 
     * @param highPrice 
     * @param lowPrice 
     * @param rowKey of the row represented by this dotInfo, containing rowID
     *            and color attributes
     * @param hilit true if this dot is hilited
     * @param colorAttr the color of this dot
     * @param size the relative size of this dot
     * @param rowIndex the indes of the row this dot was created from important
     *            to map back to the rows in the data container
     */
    public FinancialDotInfo(final int x, 
            final int openPrice, final int closePrice, 
            final int highPrice, final int lowPrice,
            final RowKey rowKey,
            final boolean hilit, final ColorAttr colorAttr, final double size,
            final int rowIndex) {
        m_xCoord = x;
        
        m_openPrice = openPrice;
        m_closePrice = closePrice;
        m_highPrice = highPrice;
        m_lowPrice = lowPrice;
        
        m_rowID = rowKey;
        m_color = colorAttr;
        m_size = size;
        m_hilit = hilit;
        m_rowIndex = rowIndex;
        m_shape = FinancialShapeFactory.getShape(FinancialShapeFactory.VERTICAL_BAR);
    }

    /**
     * @return the DataCell containing the row key of the row this data point is
     *         extracted from.
     */
    public RowKey getRowID() {
        return m_rowID;
    }

    /**
     * @return the X Coordinate in the drawing pane. If negative dot shouldn't
     *         be painted.
     */
    public int getXCoord() {
        return m_xCoord;
    }

    /**
     * @param x the new screen X coordinate for this point. If negative the
     *            paintDot() method will return false.
     */
    public void setXCoord(final int x) {
        m_xCoord = x;
    }

    /**
    * @return the Y Coordinate for the open price in the drawing pane. 
     * If negative dot shouldn't be painted.
     */
    public int getOpenPrice() {
        return m_openPrice;
    }

    /**
     * @return the Y Coordinate for the close price in the drawing pane. 
     * If negative dot shouldn't be painted.
     */
    public int getClosePrice() {
        return m_closePrice;
    }

    /**
     * @return the Y Coordinate for the low price in the drawing pane. 
     * If negative dot shouldn't be painted.
     */
    public int getLowPrice() {
        return m_lowPrice;
    }

    /**
    * @return the Y Coordinate for the high price in the drawing pane. 
     * If negative dot shouldn't be painted.
     */
    public int getHighPrice() {
        return m_highPrice;
    }

    /**
     * @param y the new screen Y coordinate for the open price. If negative the
     *            paintDot() method will return false.
     */
    public void setOpenPrice(final int y) {
        m_openPrice = y;
    }

    /**
     * @param y the new screen Y coordinate for the close price. If negative the
     *            paintDot() method will return false.
     */
    public void setClosePrice(final int y) {
        m_closePrice = y;
    }

    /**
     * @param y the new screen Y coordinate for the high price. If negative the
     *            paintDot() method will return false.
     */
    public void setHighPrice(final int y) {
        m_highPrice = y;
    }

    /**
     * @param y the new screen Y coordinate for the low price. If negative the
     *            paintDot() method will return false.
     */
    public void setLowPrice(final int y) {
        m_lowPrice = y;
    }

    /**
     * @return false if the dot shouldn't be painted. Negative screen
     *         coordinates are used as flag for this.
     */
    public boolean paintDot() {
        return (m_xCoord >= 0) && (m_openPrice >= 0)
                && (m_closePrice >= 0) && (m_lowPrice >= 0)
                && (m_highPrice >= 0);
    }

    /**
     * @return the <code>Color</code> this dot is supposed to be drawn with
     */
    public ColorAttr getColor() {
        return m_color;
    }

    /**
     * @return the status if the highlighting of this dot true if hilited,
     *         false if not.
     */
    public boolean isHiLit() {
        return m_hilit;
    }

    /**
     * @param h true if the dot should be hilited, false if not.
     */
    public void setHiLit(final boolean h) {
        m_hilit = h;
    }
    
    /**
     * 
     * @param shape the shape for this dot.
     */
    public void setShape(final FinancialShapeFactory.Shape shape) {
        m_shape = shape;
    }
    
    /**
     * 
     * @return a new instance of the associated shape of this dot.
     */
    public FinancialShapeFactory.Shape getShape() {
        return m_shape;
    }
    
    /**
     * 
     * @param xDomainValue the original x value.
     */
    public void setXDomainValue(final DataCell xDomainValue) {
        m_xDomainValue = xDomainValue;
    }
    
    /**
     * 
     * @return the original x value.
     */
    public DataCell getXDomainValue() {
        return m_xDomainValue;
    }
    
    /**
     * 
     * @param yDomainValue the original open price value.
     */
    public void setOpenPriceDomainValue(final DataCell yDomainValue) {
        m_openPriceDomainValue = yDomainValue;
    }
    
    /**
     * 
     * @return the original open price value.
     */
    public DataCell getOpenPriceDomainValue() {
        return m_openPriceDomainValue;
    }

    /**
     * 
     * @param yDomainValue the original close price value.
     */
    public void setClosePriceDomainValue(final DataCell yDomainValue) {
        m_closePriceDomainValue = yDomainValue;
    }
    
    /**
     * 
     * @return the original close price value.
     */
    public DataCell getClosePriceDomainValue() {
        return m_closePriceDomainValue;
    }

    /**
     * 
     * @param yDomainValue the original high price value.
     */
    public void setHighPriceDomainValue(final DataCell yDomainValue) {
        m_highPriceDomainValue = yDomainValue;
    }
    
    /**
     * 
     * @return the original high price value.
     */
    public DataCell getHighPriceDomainValue() {
        return m_highPriceDomainValue;
    }

    /**
     * 
     * @param yDomainValue the original low price value.
     */
    public void setLowPriceDomainValue(final DataCell yDomainValue) {
        m_lowPriceDomainValue = yDomainValue;
    }
    
    /**
     * 
     * @return the original low price value.
     */
    public DataCell getLowPriceDomainValue() {
        return m_lowPriceDomainValue;
    }

    /**
     * Returns a string. Containing an open brace the RowKey in single quotes, a
     * '@' sign, the x and y coordinates, comma separated together in
     * parantheses, and either "Col:" (if not hilighted) or "HLT:" (if
     * hilited) followed by rXXXgXXXbXXX, with xxx being the value of the
     * corresponding color component (r=red, g=green, and b=blue). The whole
     * thing will be terminated by a closing brace.
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("{'");
        result.append(m_rowID.toString());
        result.append("'@(");
        result.append(m_xCoord);
        result.append(",");
        result.append(m_openPrice);
        result.append(",");
        result.append(m_closePrice);
        result.append(",");
        result.append(m_highPrice);
        result.append(",");
        result.append(m_lowPrice);
        result.append(")");
        if (m_hilit) {
            result.append(",HLT:");
        } else {
            result.append(",Col:");
        }
        if (m_color == null) {
            result.append("(null)");
        } else {
            result.append("r" + m_color.getColor(false, false).getRed());
            result.append("g" + m_color.getColor(false, false).getGreen());
            result.append("b" + m_color.getColor(false, false).getBlue());
        }
        result.append("}");
        return result.toString();
    }

    /**
     * @return returns the size of a dot
     */
    public double getSize() {
        return m_size;
    }

    /**
     * @return the index of the corresponding data row
     */
    public int getRowIndex() {
        return m_rowIndex;
    }
}
