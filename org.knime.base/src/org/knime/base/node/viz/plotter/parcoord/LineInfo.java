/*
 * ------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   24.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.parcoord;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.data.property.ShapeFactory;

/**
 * Represents a line (one row) in the parallel coordinates plot. Stores the 
 * mapped points, the domain values (for tooltip information), flags whether this
 * line is selected and/or hilited, the {@link org.knime.core.data.RowKey} and the
 * visual properties size, color and shape (for the dots).
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class LineInfo {
    
    private List<Point> m_points;
    
    private List<DataCell> m_domainValues;
    
    private boolean m_isSelected;
    
    private boolean m_isHilite;
    
    private ColorAttr m_color;
    
    private final RowKey m_rowKey;
    
    private double m_size;
    
    private ShapeFactory.Shape m_shape;
    
    private static final int CLICK_TOLERANCE = 5;
    
    
    /**
     * Creates a line.
     * @param points the points making up the line (mapped).
     * @param domainValues the domain values
     * @param isSelected true if the line / row is selected.
     * @param isHilite true if the row/line is hilite
     * @param color the row's color attribute
     * @param size the rows size attribute
     * @param rowKey the referring row key of the line
     */
    public LineInfo(final List<Point> points, final List<DataCell> domainValues,
            final boolean isSelected,
            final boolean isHilite, final ColorAttr color, final double size, 
            final RowKey rowKey) {
        m_points = points;
        m_domainValues = domainValues;
        m_isSelected = isSelected;
        m_isHilite = isHilite;
        m_color = color;
        m_size = size;
        m_rowKey = rowKey;
        m_shape = ShapeFactory.getShape(ShapeFactory.DEFAULT);
    }
    
    /**
     * 
     * @param points the mapped points making up the line
     */
    public void setPoints(final List<Point> points) {
        m_points = points;
    }
    
    /**
     * 
     * @return the mapped points making up the line
     */
    public List<Point>getPoints() {
        return m_points;
    }
    
    /**
     * 
     * @param shape the shape for the datapoints of this line.
     */
    public void setShape(final ShapeFactory.Shape shape) {
        m_shape = shape;
    }
    
    /**
     * 
     * @return the shape for the data points of this line.
     */
    public ShapeFactory.Shape getShape() {
        return m_shape;
    }
    
    /**
     * 
     * @param domainValues the domain values of the line.
     */
    public void setDomainValues(final List<DataCell> domainValues) {
        m_domainValues = domainValues;
    }
    
    /**
     * 
     * @return the domain values of the line.
     */
    public List<DataCell> getDomainValues() {
        return m_domainValues;
    }
    
    /**
     * 
     * @param isSelected true if the line / row is selected.
     */
    public void setSelected(final boolean isSelected) {
        m_isSelected = isSelected;
    }
    
    /**
     * 
     * @return true if the row / line is selected.
     */
    public boolean isSelected() {
        return m_isSelected;
    }
    
    /**
     * 
     * @param hilite true if the row / line is hilite.
     */
    public void setHilite(final boolean hilite) {
        m_isHilite  = hilite;
    }
    
    /**
     * 
     * @return true if the row / line is hilite.
     */
    public boolean isHilite() {
        return m_isHilite;
    }
    
    /**
     * 
     * @param color the color of this row / line.
     */
    public void setColor(final ColorAttr color) {
        m_color = color;
    }
    
    /**
     * 
     * @return the color of this row / line.
     */
    public ColorAttr getColor() {
        return m_color;
    }
    
    /**
     * 
     * @param size the row's size attribute
     */
    public void setSize(final double size) {
        m_size = size;
    }
    
    /**
     * 
     * @return the relative size
     */
    public double getSize() {
        return m_size;
    }
    
    /**
     * 
     * @return the referring row key of this line.
     */
    public RowKey getRowKey() {
        return m_rowKey;
    }
    
    /**
     * Adds a point to the line.
     * @param point the point to be added
     */
    public void addPoint(final Point point) {
        m_points.add(point);
    }
    
    
    /**
     * Returns true if any point of this line is contained in the rectangle.
     * @param rectangle the dragged rectangle
     * @return true if any point of the line lies within the rectangle.
     */
    public boolean isContainedIn(final Rectangle rectangle) {
        for (Point p : m_points) {
            if (p.x > rectangle.x 
                    && p.x < (rectangle.x + rectangle.width)
                    && p.y > rectangle.y
                    && p.y < rectangle.y + rectangle.height) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Determines if the line was clicked.
     * @param clicked the clicked point
     * @param curve flag if the line was drawn as a curve.
     * @return true if the either one point of the line was clicked or if 
     * the line connecting the points was clicked 
     * (both with a defined tolerance).
     * 
     */
    public boolean wasClicked(final Point clicked, final boolean curve) {
        // check whether they were clicked
        for (int i = 0; i < getPoints().size() - 1; i++) {
            // calculate the linear interpolation between any pair of points
            Point p1 = getPoints().get(i);
            Point p2 = getPoints().get(i + 1);
            // check if one of the points was clicked
            if (pointWasClicked(clicked, p1) 
                    || pointWasClicked(clicked, p2)) {
                return true;
            }
            if (curve) {
                return false;
            }
            // first find the x range
            if (p1.x < clicked.x && clicked.x < p2.x) {
                // check if clicked lies between the two points on the yaxis
                if ((clicked.y <= p1.y && clicked.y >= p2.y) 
                        || (clicked.y >= p1.y && clicked.y <= p2.y)) {
                    // no interpolate the line
                double m = ((p2.getY() - p1.getY()) 
                        / (p2.getX() - p1.getX()));
                // get the y value
                double y = (m * clicked.getX()) - (m * p1.getX()) 
                    + p1.getY();
                // if the point lies on the interpolated line
                if (clicked.y < (int)(y + CLICK_TOLERANCE) 
                        && clicked.y > (int)(y - CLICK_TOLERANCE)) {
                    return true;
                }
                }
            }
        }
        return false;
    }
    
    private boolean pointWasClicked(final Point clicked, final Point p1) {
        if (clicked.x <= (p1.x + CLICK_TOLERANCE / 2) 
                && clicked.x >= (p1.x - CLICK_TOLERANCE / 2)
                && clicked.y <= (p1.y + CLICK_TOLERANCE / 2)
                && clicked.y >= (p1.y - CLICK_TOLERANCE / 2)) {
            return true;
        }
        return false;
    }
    
    
}
