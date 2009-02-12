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
 *   12.10.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.dendrogram;

import java.awt.Point;
import java.util.LinkedHashSet;
import java.util.Set;

import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.data.property.ShapeFactory;

/**
 * Represents a point in the dendrogram with the contained rows, the distance of
 * the cluster, the point in the drawing pane and the visual properties 
 * hilited, selected, relative size, color and shape. 
 * A {@link org.knime.base.node.viz.plotter.dendrogram.BinaryTree} of 
 * <code>DendrogramPoint</code>s is passed from the
 * {@link org.knime.base.node.viz.plotter.dendrogram.DendrogramPlotter} to the 
 * {@link org.knime.base.node.viz.plotter.dendrogram.DendrogramDrawingPane}.
 * 
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class DendrogramPoint {
    
    /** The point in 2d space. */
    private final Point m_point;
    
    /**
     * the data rows contained in the referring cluster node or the one that is
     * contained in the leaf.
     */
    private Set<RowKey> m_containedRows;
    
    /** Flag whether it is selected. */
    private boolean m_isSelected;
    
    /** Flag whether it is hilited. */
    private boolean m_isHilite;
    
    /** The relative size of the leaf point (as defined by the size 
     * property handler). */
    private double m_relSize = 0.0;
    
    /** Shape of the leaf data point defined by the shape handler. */
    private ShapeFactory.Shape m_shape = ShapeFactory.getShape(
            ShapeFactory.DEFAULT);
    
    /** Color of the leaf data point defined by the color handler. */
    private ColorAttr m_color = ColorAttr.DEFAULT;
    
    /** Store the original distance for tooptip. */
    private final double m_dist;
    
    
    /**
     * Creates a <code>DendrogramPoint</code> with a mapped point and the 
     * original distance.
     * 
     * @param p the mapped point.
     * @param dist the distance of the represented cluster node.
     */
    public DendrogramPoint(final Point p, final double dist) {
        m_point = p;
        m_dist = dist;
        m_containedRows = new LinkedHashSet<RowKey>();
    }
    
    /**
     * Adds the rows to this <code>DendrogramPoint</code>.
     * 
     * @param rowIds adds the row ids to the contained row ids.
     */
    public void addRow(final RowKey... rowIds) {
        for (RowKey id : rowIds) {
            m_containedRows.add(id);
        }
    }
    
    /**
     * Adds the rows to thsi <code>DendrogramPoint</code>.
     * @param ids adds the row ids to the contained row ids.
     */
    public void addRows(final Set<RowKey> ids) {
        m_containedRows.addAll(ids);
    }
    
    /**
     * Returns the mapped point, where to draw this 
     * <code>DendrogramPoint</code>.
     * 
     * @return the point where the cluster is located in the dendrogram.
     */
    public Point getPoint() {
        return m_point;
    }
    
    /**
     * 
     * @return the original distance value of the represented cluster node.
     */
    public double getDistance() {
        return m_dist;
    }
    /**
     * 
     * @return row ids of all contained rows.
     */
    public Set<RowKey> getRows() {
        return m_containedRows;
    }
    
    /**
     * 
     * @return true if the point is selected.
     */
    public boolean isSelected() {
        return m_isSelected;
    }
    
    /**
     * 
     * @param selected true if the point is selected
     */
    public void setSelected(final boolean selected) {
        m_isSelected = selected;
    }
    
    /**
     * 
     * @return true if the point is hilited
     */
    public boolean isHilite() {
        return m_isHilite;
    }

    /**
     * 
     * @param hilite true if the point is hilited.
     */
    public void setHilite(final boolean hilite) {
        m_isHilite = hilite;
    }
    
    /** 
     * If a datapoint is represented this is its original color attr.
     * @param color the original color attr.
     */
    public void setColor(final ColorAttr color) {
        m_color = color;
    }
    
    /**
     * 
     * @return Default for cluster nodes and for data points the original color.
     */
    public ColorAttr getColor() {
        return m_color;
    }
    
    /**
     * 
     * @param relSize the relative size defined by the size manager, or 0.0 for 
     * cluster points and default.
     */
    public void setRelativeSize(final double relSize) {
        m_relSize = relSize;
    }
    
    /**
     * 
     * @return the relative size defined by the size manager, or 0.0 for 
     * cluster points and default.
     */
    public double getRelativeSize() {
        return m_relSize;
    }
    
    /**
     * 
     * @param shape the shape defined by the shape manager or default otherwise.
     */
    public void setShape(final ShapeFactory.Shape shape) {
        m_shape = shape;
    }
    
    /**
     * 
     * @return the shape defined by the shape manager or default otherwise.
     */
    public ShapeFactory.Shape getShape() {
        return m_shape;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object o) {
        DendrogramPoint p = (DendrogramPoint)o;
        return m_containedRows.equals(p.m_containedRows);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_containedRows.hashCode();
    }
}
