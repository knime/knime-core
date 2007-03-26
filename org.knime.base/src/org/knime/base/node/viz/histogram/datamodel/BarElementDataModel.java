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
 *    13.03.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.datamodel;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;

import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;

/**
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class BarElementDataModel implements Serializable {

    private static final long serialVersionUID = 2537898631338523620L;

    private final Color m_color;
    
    private double m_aggrSum = 0;
    /**The number of values without missing values!*/
    private int m_valueCounter = 0;
    /**The number of rows including empty value rows.*/
    private int m_rowCounter = 0;
    
    //visual variables
    private boolean m_isSelected = false;
    private Rectangle m_elementRectangle;

    /**Constructor for class BarElementDataModel.
     * @param color the color to use for this bar element
     */
    public BarElementDataModel(final Color color) {
        m_color = color;
    }

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

    /**
     * Adds the given values to the bar element.
     * @param rowKey the rowkey of the row to add
     * @param aggrValCell the value cell of the aggregation column of this bar
     */
    protected void addDataRow(final DataCell rowKey, 
            final DataCell aggrValCell) {
        if (!aggrValCell.isMissing()) {
            m_aggrSum += ((DoubleValue)aggrValCell).getDoubleValue();
            m_valueCounter++;
        }
        m_rowCounter++;
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
            if (m_valueCounter == 0) {
                //avoid division by 0
                return 0;
            }
            return m_aggrSum / m_valueCounter;
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
        } else if (m_elementRectangle == null 
                || !m_elementRectangle.equals(elementRect)) {
            m_elementRectangle = elementRect;
        }
    }

    /**
     * @param xCoord the new x coordinate
     * @param elementWidth the new element width
     * @param aggrMethod the {@link AggregationMethod} to use
     */
    public void updateElementWidth(final int xCoord, final int elementWidth, 
            final AggregationMethod aggrMethod) {
        if (m_elementRectangle == null) {
            return;
        }
        final int yCoord = (int)m_elementRectangle.getY();
        final int elementHeight = (int)m_elementRectangle.getHeight();
        m_elementRectangle.setBounds(xCoord, yCoord, 
                elementWidth, elementHeight);
    }

    /**
     * Selects this element if the element rectangle contains the given 
     * point.
     * @param point the {@link Point} to check
     * @return <code>true</code> if the element contains the point
     */
    public boolean selectElement(final Point point) {
        if (m_elementRectangle != null 
                    && m_elementRectangle.contains(point)) {
            setSelected(true);
            return true;
        }
        return false;
    }

    /**
     * Selects this element if the element rectangle intersect the given 
     * rectangle.
     * @param rect the {@link Rectangle} to check
     * @return <code>true</code> if the element intersects the rectangle
     */
    public boolean selectElement(final Rectangle rect) {
        if (m_elementRectangle != null 
                && m_elementRectangle.intersects(rect)) {
        setSelected(true);
        return true;
    }
    return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BarElementDataModel clone() {
        final BarElementDataModel clone = new BarElementDataModel(m_color);
        clone.m_aggrSum = m_aggrSum;
        clone.m_rowCounter = m_rowCounter;
        clone.m_valueCounter = m_valueCounter;
        return clone;
    }
}
