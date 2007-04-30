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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.core.data.DataCell;

/**
 * This class extends the {@link BarElementDataModel} to support hiliting.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractiveBarElementDataModel extends BarElementDataModel {

    private static final long serialVersionUID = -6612680797726333107L;
    
    private final Set<DataCell> m_rowKeys = new HashSet<DataCell>();
    
    private final Set<DataCell> m_hilitedRowKeys = new HashSet<DataCell>();
    
    private Rectangle m_hilitedRectangle;
    
    /**Constructor for class BarElementDataModel.
     * @param color the color to use for this bar element
     */
    protected InteractiveBarElementDataModel(final Color color) {
        super(color);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void addDataRow(final DataCell rowKey, 
            final DataCell aggrValCell) {
        super.addDataRow(rowKey, aggrValCell);
        m_rowKeys.add(rowKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setElementRectangle(final Rectangle elementRect, 
            final AggregationMethod aggrMethod) {
        super.setElementRectangle(elementRect, aggrMethod);
        calculateHilitedRectangle(aggrMethod);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void updateElementWidth(final int xCoord, final int elementWidth, 
            final AggregationMethod aggrMethod) {
        super.updateElementWidth(xCoord, elementWidth, aggrMethod);
        calculateHilitedRectangle(aggrMethod);
    }
    /**
     * Calculates the hilite rectangle or resets the hilite rectangle to
     * <code>null</code> if no rows are hilited.
     * @param aggrMethod the aggregation method which should be used
     * @param elementRect 
     */
    private void calculateHilitedRectangle(final AggregationMethod aggrMethod) {
        final int noOfHilitedKeys = m_hilitedRowKeys.size();
        final Rectangle elementRect = getElementRectangle();
        if (noOfHilitedKeys < 1 || elementRect == null) {
            //if their are no rows hilited we have no hilite rectangle
            m_hilitedRectangle = null;
            return;
        }
        final int totalWidth = (int)elementRect.getWidth();
        final int hiliteWidth = Math.max((int)(totalWidth 
                * AbstractHistogramVizModel.HILITE_RECT_WIDTH_FACTOR), 
                1);
        final int totalHeight = (int)elementRect.getHeight();
        final double heightPerRow = (double)totalHeight / getRowCount();
        final int hiliteHeight = 
            Math.max((int)(heightPerRow * noOfHilitedKeys), 1);
        final int xCoord = (int)elementRect.getX() 
                            + (totalWidth / 2) - (hiliteWidth / 2);
        final int startY = (int)elementRect.getY();
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
     * @return the {@link Rectangle} the hilited amount of this element 
     * should be drawn on the screen or <code>null</code> if none are hilited
     */
    public Rectangle getHilitedRectangle() {
        return m_hilitedRectangle;
    }

    /**
     * @return the keys of the rows in this element.
     */
    public Set<DataCell> getKeys() {
        return m_rowKeys;
    }

    /**
     * @return <code>true</code> if at least one row of this element is hilited
     */
    public boolean isHilited() {
        return m_hilitedRowKeys.size() > 0;
    }

    /**
     * @return the keys of the hilited rows in this element
     */
    public Set<DataCell> getHilitedKeys() {
        return m_hilitedRowKeys;
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
     * @return <code>true</code> if at least one key has been added
     */
    protected boolean setHilitedKeys(final Collection<DataCell> hilitedKeys, 
            final AggregationMethod aggrMethod) {
        boolean changed = false;
        for (DataCell key : hilitedKeys) {
            if (m_rowKeys.contains(key)) {
                if (m_hilitedRowKeys.add(key)) {
                    changed = true;
                }
            }
        }
        if (changed) {
            calculateHilitedRectangle(aggrMethod);
        }
        return changed;
    }

    /**
     * @param unhilitedKeys the keys which should be unhilited
     * @param aggrMethod the current aggregation method
     * @return <code>true</code> if at least one key has been removed
     */
    protected boolean removeHilitedKeys(
            final Collection<DataCell> unhilitedKeys, 
            final AggregationMethod aggrMethod) {
        final boolean changed = m_hilitedRowKeys.removeAll(unhilitedKeys);
        if (changed) {
            calculateHilitedRectangle(aggrMethod);
        }
        return changed;
    }

    /**
     * @return the number of hilited rows
     */
    public int getHiliteRowCount() {
        return m_hilitedRowKeys.size();
    }

}
