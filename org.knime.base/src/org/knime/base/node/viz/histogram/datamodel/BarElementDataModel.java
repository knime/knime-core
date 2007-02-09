/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *    01.01.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.datamodel;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Set;

import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.core.data.DataCell;

/**
 * This class holds all information for a bar element of a histogram bar. A
 * bar element is a rectangle section of a histogram bar.
 * @author Tobias Koetter, University of Konstanz
 */
public class BarElementDataModel {
    
    /**
     * The width of the hilite rectangle in percent of the surrounding
     * rectangle. Should be greater 0 and less than 1. 0.8 = 80%
     */
    private static final double HILITE_RECTANGLE_WIDTH_FACTOR = 0.5;

    private final Color m_color;
    
    private final Set<DataCell> m_rowKeys = new HashSet<DataCell>();
    
    private final Set<DataCell> m_hilitedRowKeys = new HashSet<DataCell>();
    
    private double m_aggrSum = 0;
    
    private int m_rowCounter = 0;
    
    private boolean m_isSelected = false;
    
    private Rectangle m_elementRectangle;
    
    private Rectangle m_hilitedRectangle;
    
    /**
     * @return <code>true</code> if the element is selected
     */
    public boolean isSelected() {
        return m_isSelected;
    }

    /**
     * @param isSelected set to <code>true</code> if the element is selected
     */
    protected void setSelected(final boolean isSelected) {
        this.m_isSelected = isSelected;
    }

    /**Constructor for class BarElementDataModel.
     * @param color the color to use for this bar element
     */
    protected BarElementDataModel(final Color color) {
        m_color = color;
    }
    
    /**
     * Adds the given values to the bar element.
     * @param rowKey the rowkey of the row to add
     * @param aggrVal the value of the aggregation column of this bar
     */
    protected void addDataRow(final DataCell rowKey, final double aggrVal) {
        m_rowKeys.add(rowKey);
        m_aggrSum += aggrVal;
        m_rowCounter++;
    }
    
    /**
     * @return the keys of the rows in this element.
     */
    public Set<DataCell> getKeys() {
        return m_rowKeys;
    }
    
    /**
     * Clears the hilite counter.
     */
    protected void clearHilite() {
        m_hilitedRowKeys.clear();
        m_hilitedRectangle = null;
    }
    
    /**
     * @param hilitedKeys the hilited keys
     * @param aggrMethod the current aggregation method
     * 
     */
    protected void setHilitedKeys(final Set<DataCell> hilitedKeys, 
            final AggregationMethod aggrMethod) {
        for (DataCell key : hilitedKeys) {
            if (m_rowKeys.contains(key)) {
                m_hilitedRowKeys.add(key);
                calculateHilitedRectangle(aggrMethod);
            }
        }
    }
    
    /**
     * @param unhilitedKeys the keys which should be unhilited
     * @param aggrMethod the current aggregation method
     */
    protected void removeHilitedKeys(final Set<DataCell> unhilitedKeys,
            final AggregationMethod aggrMethod) {
        if (m_hilitedRowKeys.removeAll(unhilitedKeys)) {
            calculateHilitedRectangle(aggrMethod);
        }
    }
    
    /**
     * @return the number of rows
     */
    public int getRowCount() {
        return m_rowCounter;
    }
    
    /**
     * @return the summary of the aggregation values of all added rows
     */
    public double getAggregationSum() {
        return m_aggrSum;
    }
    
    /**
     * @param method the {@link AggregationMethod} to use
     * @return the aggregation value of this bar element
     */
    public double getAggregationValue(final AggregationMethod method) {
        if (AggregationMethod.COUNT.equals(method)) {
            return m_rowCounter;
        } else if (AggregationMethod.SUM.equals(method)) {
            return m_aggrSum;
        } else if (AggregationMethod.AVERAGE.equals(method)) {
            if (m_rowCounter == 0) {
                //avoid division by 0
                return 0;
            }
            return m_aggrSum / m_rowCounter;
        }
       throw new IllegalArgumentException("Aggregation method "
               + method + " not supported.");
    }
    /**
     * @return the color to use for this bar element
     */
    public Color getColor() {
        return m_color;
    }

    /**
     * @return the {@link Rectangle} the element should be drawn on the 
     * screen
     */
    public Rectangle getElementRectangle() {
        return m_elementRectangle;
    }

    /**
     * @param elementRect the {@link Rectangle} the element should be drawn
     * on the screen
     * @param aggrMethod the aggregation method which should be used
     */
    protected void setElementRectangle(final Rectangle elementRect,
            final AggregationMethod aggrMethod) {
        if (elementRect == null) {
            m_elementRectangle = null;
            m_hilitedRectangle = null;
        } else if (m_elementRectangle == null 
                || !m_elementRectangle.equals(elementRect)) {
            m_elementRectangle = elementRect;
            calculateHilitedRectangle(aggrMethod);
        }
    }
    
    /**
     * @return the {@link Rectangle} the hilited amount of this element 
     * should be drawn on the screen or <code>null</code> if none are hilited
     */
    public Rectangle getHilitedRectangle() {
        return m_hilitedRectangle;
    }
    
    /**
     * Calculates the hilite rectangle or resets the hilite rectangle to
     * <code>null</code> if no rows are hilited.
     * @param aggrMethod the aggregation method which should be used
     */
    private void calculateHilitedRectangle(final AggregationMethod aggrMethod) {
        final int noOfHilitedKeys = m_hilitedRowKeys.size();
        if (noOfHilitedKeys < 1) {
            //if their are no rows hilited we have no hilite rectangle
            m_hilitedRectangle = null;
            return;
        }
        final int totalWidth = (int)m_elementRectangle.getWidth();
        final int hiliteWidth = Math.max(
                (int)(totalWidth * HILITE_RECTANGLE_WIDTH_FACTOR), 1);
        final int totalHeight = (int)m_elementRectangle.getHeight();
        final double heightPerRow = (double)totalHeight / getRowCount();
        final int hiliteHeight = 
            Math.max((int)(heightPerRow * noOfHilitedKeys), 1);
        final int xCoord = (int)m_elementRectangle.getX() 
                            + (totalWidth / 2) - (hiliteWidth / 2);
        final int startY = (int)m_elementRectangle.getY();
        final double aggrVal = getAggregationValue(aggrMethod);
        int yCoord = 0;
        if (aggrVal >= 0) {
            //if it's a positive value we draw the hilite rectangle from
            //bottom to top of the bar
            yCoord = startY + (totalHeight - hiliteHeight);
        } else {
            //if it's a negative value we draw the hilite rectangle from
            //top to bottom of the bar
            yCoord = startY;
        }
        final Rectangle hiliteRect = 
            new Rectangle(xCoord, yCoord, hiliteWidth, hiliteHeight);
        m_hilitedRectangle = hiliteRect;
    }
    
    /**
     * @see java.lang.Object#clone()
     */
    @Override
    public BarElementDataModel clone() {
        final BarElementDataModel clone = new BarElementDataModel(m_color);
        clone.m_aggrSum = m_aggrSum;
        clone.m_elementRectangle = m_elementRectangle;
        clone.m_hilitedRectangle = m_hilitedRectangle;
        clone.m_hilitedRowKeys.addAll(m_hilitedRowKeys);
        clone.m_isSelected = m_isSelected;
        clone.m_rowCounter = m_rowCounter;
        clone.m_rowKeys.addAll(m_rowKeys);
        return clone;
    }
}
