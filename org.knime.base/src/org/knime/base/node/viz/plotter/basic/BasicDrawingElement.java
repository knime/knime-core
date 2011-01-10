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
 *   05.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.basic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * A drawing element consists of points which are already mapped to the 
 * drawing pane's dimension. The <code>BasicDrawingElement</code>s can be added
 * to the {@link org.knime.base.node.viz.plotter.basic.BasicPlotter} with 
 * {@link org.knime.base.node.viz.plotter.basic.BasicPlotter
 * #addBasicDrawingElement(BasicDrawingElement)}. The 
 * <code>BasicDrawingElement</code>s should be used if only the domain data of 
 * the shape is available, since they are automatically mapped to the drawing 
 * pane's dimension. There some ready-to-use implementations of the 
 * <code>BasicDrawingElement</code>: 
 * {@link org.knime.base.node.viz.plotter.basic.BasicLine}, 
 * {@link org.knime.base.node.viz.plotter.basic.BasicEllipse},
 * {@link org.knime.base.node.viz.plotter.basic.BasicRectangle},
 * {@link org.knime.base.node.viz.plotter.basic.BasicText}.
 * Each of these implementations restore the original stroke and color of the 
 * graphics object. New implementations should keep this behavior.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public abstract class BasicDrawingElement {
   
    /** The mapped points. */
    private List<Point> m_points;
    
    /** The color of the shape. */
    private Color m_color;
    
    /** The stroke the shaped is painted with. */
    private Stroke m_stroke;
    
    /** Default color is black. */
    private static final Color DEFAULT_COLOR = Color.BLACK;
    
    /** Default storke. */
    private static final Stroke DEFAULT_STROKE = new BasicStroke(1);
    
    /** Original (domain) values. */
    private List<DataCellPoint> m_domainValues = new ArrayList<DataCellPoint>();
    
    /**
     * Creates an empty shape with default color(black) and default stroke.
     *
     */
    public BasicDrawingElement() {
        m_color = DEFAULT_COLOR;
        m_stroke = DEFAULT_STROKE;
    }
    
    
    /**
     * Adds a (mapped) point of the path. This method should be used by the 
     * {@link org.knime.base.node.viz.plotter.basic.BasicPlotter#updateSize()}
     * only.
     * 
     * @param p one point of the path
     */
    public void addPoint(final Point p) {
        if (m_points == null) {
            m_points = new LinkedList<Point>();
        }
        m_points.add(p);
    }
    
    /**
     * Adds a domain value to the set of domain values that are mapped to the 
     * DrawingPane's dimension.
     * 
     * @param domainValue the domain value
     */
    public void addDomainValue(final DataCellPoint domainValue) {
        if (m_domainValues == null) {
            m_domainValues = new LinkedList<DataCellPoint>();
        }
        m_domainValues.add(domainValue);
    }
    
    /**
     * Adds a list of domain values that should be in same order as 
     * referring mapped points.
     * 
     * @param domainValues the domain values
     */
    public void setDomainValues(final List<DataCellPoint>domainValues) {
        m_domainValues = domainValues;
    }
    
    /**
     * Adds a list of domain values that should be in same order as 
     * referring mapped points.
     * 
     * @param points domain points
     */
    public void setDomainValues(final DataCellPoint...points) {
        m_domainValues = new LinkedList<DataCellPoint>();
        for (DataCellPoint p : points) {
            m_domainValues.add(p);
        }
    }
    
    /**
     * 
     * @return the domain values
     */
    public List<DataCellPoint> getDomainValues() {
        return m_domainValues;
    }
    

    /**
     * Sets the mapped points. Should be used by the 
     * {@link org.knime.base.node.viz.plotter.basic.BasicPlotter} only.
     * 
     * @param points the mapped points making up this drawing element
     */
    public void setPoints(final List<Point> points) {
        m_points = points;
    }
    
    /**
     * Sets the mapped points. Should be used by the 
     * {@link org.knime.base.node.viz.plotter.basic.BasicPlotter} only.
     * 
     * @param points mapped points
     */
    public void setPoints(final Point... points) {
        m_points = new LinkedList<Point>();
        for (Point p : points) {
            m_points.add(p);
        }
    }
    

    /**
     * 
     * @return the mapped points making up this drawing element
     */
    public List<Point>getPoints() {
        return m_points;
    }
    
    /**
     * 
     * @return the color of this element
     */
    public Color getColor() {
        return m_color;
    }
    
    /**
     * 
     * @param color the color of this element
     */
    public void setColor(final Color color) {
        m_color = color;
    }
    
    /**
     * 
     * @param stroke the stroke of this element
     */
    public void setStroke(final Stroke stroke) {
        m_stroke = stroke;
    }
    
    /**
     * 
     * @return the stroke of this element
     */
    public Stroke getStroke() {
        return m_stroke;
    }
    
    /**
     * The method which "knows" how to paint it.
     * 
     * @param g2 the graphics object
     */
    public abstract void paint(final Graphics2D g2); 
    

}
