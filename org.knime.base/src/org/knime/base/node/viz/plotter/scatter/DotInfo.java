/*
 * 
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.viz.plotter.scatter;

import org.knime.core.data.DataCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.data.property.ShapeFactory;

/**
 * This is a helper class for the ScatterPlotView. It keeps info (like the
 * screen coordinates and color) for each data point that is plotted.
 * 
 * @author Christoph Sieb, University of Konstanz
 * @author Peter Ohl, University of Konstanz
 */

public class DotInfo {

    // data used to plot that thing. Could change with user input.
    private ColorAttr m_color; // retrieved from the color handler

    private int m_xCoord; // the screen coordinates calculated depending on

    private int m_yCoord; // screen size, zoom factor, etc...
    
    private DataCell m_xDomainValue;
    
    private DataCell m_yDomainValue;

    /**
     * Dot size, retrieved from the size handler of the data table. The dot size
     * is given in percent.
     */
    private double m_size; // the size attribute of the corresponding row
    
    private ShapeFactory.Shape m_shape; // the shape attriute

    private boolean m_hilit; // the hilite status

    private RowKey m_rowID; // the associated row

    private int m_rowIndex; // the associated row index

    /**
     * Creates a new object storing the characteristics of one dot.
     * 
     * @param x the x coordinates of the point
     * @param y the y coordinates of the point
     * @param rowKey of the row represented by this dotInfo, containing rowID
     *            and color attributes
     * @param hilit true if this dot is hilited
     * @param colorAttr the color of this dot
     * @param size the relative size of this dot
     * @param rowIndex the indes of the row this dot was created from important
     *            to map back to the rows in the data container
     */
    public DotInfo(final int x, final int y, final RowKey rowKey,
            final boolean hilit, final ColorAttr colorAttr, final double size,
            final int rowIndex) {
        m_xCoord = x;
        m_yCoord = y;
        m_rowID = rowKey;
        m_color = colorAttr;
        m_size = size;
        m_hilit = hilit;
        m_rowIndex = rowIndex;
        m_shape = ShapeFactory.getShape(ShapeFactory.RECTANGLE);
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
     * @return the Y Coordinate in the drawing pane. If negative dot shouldn't
     *         be painted.
     */
    public int getYCoord() {
        return m_yCoord;
    }

    /**
     * @param y the new screen Y coordinate for this point. If negative the
     *            paintDot() method will return false.
     */
    public void setYCoord(final int y) {
        m_yCoord = y;
    }

    /**
     * @return false if the dot shouldn't be painted. Negative screen
     *         coordinates are used as flag for this.
     */
    public boolean paintDot() {
        return (m_xCoord >= 0) && (m_yCoord >= 0);
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
    public void setShape(final ShapeFactory.Shape shape) {
        m_shape = shape;
    }
    
    /**
     * 
     * @return a new instance of the associated shape of this dot.
     */
    public ShapeFactory.Shape getShape() {
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
     * @param yDomainValue the original y value.
     */
    public void setYDomainValue(final DataCell yDomainValue) {
        m_yDomainValue = yDomainValue;
    }
    
    /**
     * 
     * @return the original y value.
     */
    public DataCell getYDomainValue() {
        return m_yDomainValue;
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
        result.append(m_yCoord);
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
     * Computes the euclidian distance between given dot and itself.
     * 
     * @param dot Given dot to compute distance with.
     * @return The euclidian distance between given dot and itself.
     */
    public float distance(final DotInfo dot) {
        return (float)Math.sqrt(this.distanceSq(dot));
    }

    /**
     * Computes the squared euclidian distance between given dot and itself.
     * 
     * @param dot Given dot to compute distance with
     * @return The squared euclidian distance between given dot and itself
     */
    public float distanceSq(final DotInfo dot) {
        float dx = dot.getXCoord() - this.m_xCoord;
        float dy = dot.getYCoord() - this.m_yCoord;
        return (dx * dx + dy * dy);
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
