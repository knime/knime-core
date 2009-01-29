/*
 * ------------------------------------------------------------------
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
 *   04.10.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.scattermatrix;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.knime.base.node.viz.plotter.scatter.DotInfo;
import org.knime.base.util.coordinate.Coordinate;

/**
 * Represents one matrix element in a scatter plot matrix, 
 * with the upper left corner, the width and height of the surrounding 
 * rectangle, the x and y coordinate of the scatter plot and a list of the 
 * contained 
 * {@link org.knime.base.node.viz.plotter.scatter.DotInfo}s.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class ScatterMatrixElement {
    
    private  Point m_leftUpperCorner;
    
    private int m_width;
    
    private int m_height;
    
    private Coordinate m_xCoordinate;
    
    private Coordinate m_yCoordinate;
    
    private List<DotInfo> m_dots;
    
    /**
     * 
     * @param upperLeftCorner the upper left corner.
     * @param width the width.
     * @param height the height.
     * @param xCoordinate the x coordinate
     * @param yCoordinate the y coordinate
     */
    public ScatterMatrixElement(final Point upperLeftCorner,
            final int width, final int height, final Coordinate xCoordinate,
            final Coordinate yCoordinate) {
        m_leftUpperCorner = upperLeftCorner;
        m_width = width;
        m_height = height;
        m_xCoordinate = xCoordinate;
        m_yCoordinate = yCoordinate;
        m_dots = new ArrayList<DotInfo>();
    }
    
    
    /**
     * 
     * @param dot adds a dot for this matrix element.
     */
    public void addDot(final DotInfo dot) {
        m_dots.add(dot);
    }
    
    /**
     * 
     * @param dots the dots in this matrix element.
     */
    public void setDots(final List<DotInfo> dots) {
        m_dots = dots;
    }
    
    /**
     * 
     * @return the dots of this matrix element
     */
    public List<DotInfo> getDots() {
        return m_dots;
    }
    /**
     * 
     * @return upper left corner
     */
    public Point getCorner() {
        return m_leftUpperCorner;
    }
    
    /**
     * 
     * @return height.
     */
    public int getHeight() {
        return m_height;
    }
    
    /**
     * 
     * @return width.
     */
    public int getWidth() {
        return m_width;
    }
    
    /**
     * 
     * @return the x coordinate.
     */
    public Coordinate getXCoordinate() {
        return m_xCoordinate;
    }
    
    /**
     * 
     * @return y coordinate.
     */
    public Coordinate getYCoordinate() {
        return m_yCoordinate;
    }

}
