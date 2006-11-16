/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   05.09.2006 (Fabian Dill): created
 */
package org.knime.exp.node.view.plotter.basic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.util.LinkedList;
import java.util.List;

/**
 * A drawing element consists of points which are already mapped to the 
 * drawing pane's dimension.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public abstract class BasicDrawingElement {
    
   private List<Point> m_points;
    
    private Color m_color;
    
    private Stroke m_stroke;
    
    private static final Color DEFAULT_COLOR = Color.BLACK;
    
    private static final Stroke DEFAULT_STROKE = new BasicStroke(1);
    
    private List<DataCellPoint> m_domainValues;
    
    /**
     * Creates an empty line with default color(black) and default stroke.
     *
     */
    public BasicDrawingElement() {
        m_color = DEFAULT_COLOR;
        m_stroke = DEFAULT_STROKE;
    }
    
    
    /**
     * Adds a point of the path.
     * @param p one point of the path.
     */
    public void addPoint(final Point p) {
        if (m_points == null) {
            m_points = new LinkedList<Point>();
        }
        m_points.add(p);
    }
    
    /**
     * 
     * @param domainValue the domain value.
     */
    public void addDomainValue(final DataCellPoint domainValue) {
        if (m_domainValues == null) {
            m_domainValues = new LinkedList<DataCellPoint>();
        }
        m_domainValues.add(domainValue);
    }
    
    /**
     * Should be in same order as referring mapped points.
     * @param domainValues the domain values.
     */
    public void setDomainValues(final List<DataCellPoint>domainValues) {
        m_domainValues = domainValues;
    }
    
    /**
     * 
     * @param points domain points.
     */
    public void setDomainValues(final DataCellPoint...points) {
        m_domainValues = new LinkedList<DataCellPoint>();
        for (DataCellPoint p : points) {
            m_domainValues.add(p);
        }
    }
    
    /**
     * 
     * @return the domain values.
     */
    public List<DataCellPoint> getDomainValues() {
        return m_domainValues;
    }
    

    /**
     * 
     * @param points the mapped points making up this drawing element.
     */
    public void setPoints(final List<Point> points) {
        m_points = points;
    }
    
    /**
     * 
     * @param points mapped points.
     */
    public void setPoints(final Point... points) {
        m_points = new LinkedList<Point>();
        for (Point p : points) {
            m_points.add(p);
        }
    }
    

    /**
     * 
     * @return the mapped points making up this drawing element.
     */
    public List<Point>getPoints() {
        return m_points;
    }
    
    /**
     * 
     * @return the color of this element.
     */
    public Color getColor() {
        return m_color;
    }
    
    /**
     * 
     * @param color the color of this element.
     */
    public void setColor(final Color color) {
        m_color = color;
    }
    
    /**
     * 
     * @param stroke the stroke of this element.
     */
    public void setStroke(final Stroke stroke) {
        m_stroke = stroke;
    }
    
    /**
     * 
     * @return the stroke of this element.
     */
    public Stroke getStroke() {
        return m_stroke;
    }
    
    /**
     * The method which "knows" how to paint it.
     * @param g2 the graphics object.
     */
    public abstract void paint(final Graphics2D g2); 
    

}
