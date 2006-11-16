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
 *   12.10.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.dendrogram;

import java.awt.Point;
import java.util.LinkedHashSet;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.data.property.ShapeFactory;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class DendrogramPoint {
    
    private final Point m_point;
    
    private Set<DataCell>m_containedRows;
    
    private boolean m_isSelected;
    
    private boolean m_isHilite;
    
    private double m_relSize = 0.0;
    
    private ShapeFactory.Shape m_shape = ShapeFactory.getShape(
            ShapeFactory.DEFAULT);
    
    private ColorAttr m_color = ColorAttr.DEFAULT;
    
    // store the original distance for tooptip
    private final double m_dist;
    
    
    /**
     * 
     * @param p the mapped point.
     * @param dist the distance of the represented cluster node.
     */
    public DendrogramPoint(final Point p, final double dist) {
        m_point = p;
        m_dist = dist;
        m_containedRows = new LinkedHashSet<DataCell>();
    }
    
    /**
     * 
     * @param rowIds adds the row ids to the contained row ids.
     */
    public void addRow(final DataCell... rowIds) {
        for (DataCell id : rowIds) {
            m_containedRows.add(id);
        }
    }
    
    /**
     * 
     * @param ids adds the row ids to the contained row ids.
     */
    public void addRows(final Set<DataCell> ids) {
        m_containedRows.addAll(ids);
    }
    
    /**
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
    public Set<DataCell> getRows() {
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
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object o) {
        DendrogramPoint p = (DendrogramPoint)o;
        return m_containedRows.equals(p.m_containedRows);
    }
    
    /**
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return m_containedRows.hashCode();
    }
}
