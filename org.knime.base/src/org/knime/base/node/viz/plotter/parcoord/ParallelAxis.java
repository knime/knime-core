/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
