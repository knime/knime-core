/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
