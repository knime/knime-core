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
 *   22.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.parcoord;

import java.awt.Rectangle;

import org.knime.base.util.coordinate.Coordinate;
import org.knime.base.util.coordinate.NumericCoordinate;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;

/**
 * Represents a parallel axis in a parallel coordinates plot with an x position,
 * a height, a {@link org.knime.base.util.coordinate.Coordinate}, a name and a
 * flag, whether this <code>ParallelAxis</code> is selected.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public abstract class ParallelAxis {
    
//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            ParallelAxis.class);
    
    private int m_xPos;
    
    private int m_height;
    
    private Coordinate m_coordinate;
    
    private String m_name;
    
    private boolean m_selected;
    
    
    /**
     * Factory method to get an instance of a <code>ParallelAxis</code>. 
     * Determines whether a 
     * {@link org.knime.base.node.viz.plotter.parcoord.NumericParallelAxis} or 
     * a {@link org.knime.base.node.viz.plotter.parcoord.NominalParallelAxis}
     * should be returned, based on the passed 
     * {@link org.knime.core.data.DataColumnSpec}.
     * 
     * @param colSpec the column spec for this parallel axis.
     * @return either a nominal or a numeric parallel axis based on the column 
     * spec.
     * @see Coordinate
     */
    public static ParallelAxis createParallelAxis(
            final DataColumnSpec colSpec) {
        Coordinate coordinate = Coordinate.createCoordinate(colSpec);
        ParallelAxis axis;
        if (coordinate instanceof NumericCoordinate) {
            axis = new NumericParallelAxis();
        } else {
            axis = new NominalParallelAxis();
        }
        axis.setName(colSpec.getName());
        axis.setCoordinate(coordinate);
        return axis;
    }

    /**
     * 
     * @return the underlying coordinate
     */
    protected Coordinate getCoordinate() {
        return m_coordinate;
    }
    
    /**
     * 
     * @param coordinate the underlying coordinate
     */
    protected void setCoordinate(final Coordinate coordinate) {
        m_coordinate = coordinate;
    }
    
    /**
     * 
     * @return the referring column name
     */
    public String getName() {
        return m_name;
    }
    
    /**
     * 
     * @param name the referring column name
     */
    public void setName(final String name) {
        m_name = name;
    }
    
    /**
     * Sets the height for all parallel axes.
     * @param height height
     */
    public void setHeight(final int height) {
        m_height = height;
    }
    
    /**
     * 
     * @return the length of the axis.
     */
    public int getHeight() {
        return m_height;
    }
    
    /**
     * 
     * @return the x position where it should be painted.
     */
    public int getXPosition() {
        return m_xPos;
    }
    
    /**
     * 
     * @param xPos the mapped x position
     */
    public void setXPosition(final int xPos) {
        m_xPos = xPos;
    }
    
    /**
     * 
     * @param cell the value
     * @return the mapped point for the axis
     */
    public double getMappedValue(final DataCell cell) {
        if (cell.isMissing()) {
            return ParallelCoordinatesPlotter.MISSING;
        }
        return getCoordinate().calculateMappedValue(cell,
                m_height, true);
    }
    
    /**
     * 
     * @return true if the axis is nominal, false otherwise.
     */
    public boolean isNominal() {
        return m_coordinate.isNominal();
    }
    
    /**
     * 
     * @param rectangle a dragged selection rectangle
     * @return true if the axis lies within the rectangle or intersects it.
     */
    public boolean isContainedIn(final Rectangle rectangle) {
        if (m_xPos >= rectangle.x 
                && m_xPos <= (rectangle.x + rectangle.width)
                && ((rectangle.y + rectangle.height) 
                        > ParallelCoordinateDrawingPane.TOP_SPACE
                || (rectangle.y + rectangle.height) 
                <= (m_height - ParallelCoordinateDrawingPane.BOTTOM_SPACE))) {
            return true;
        }
        return false;
    }
    
    /**
     * 
     * @param selected true if the line should be selected.
     */
    public void setSelected(final boolean selected) {
        m_selected = selected;
    }
    
    /**
     * 
     * @return true if the line is selected.
     */
    public boolean isSelected() {
        return m_selected;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_name + "@" + m_xPos;
    }
    
}
