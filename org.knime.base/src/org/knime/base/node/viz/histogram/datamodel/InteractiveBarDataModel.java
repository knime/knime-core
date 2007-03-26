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
import java.util.SortedSet;

import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.core.data.DataCell;
import org.knime.core.node.NodeLogger;

/**
 * This class holds the information of a histogram bar. Like the color to use
 * and the {@link InteractiveBarElementDataModel} objects of this bar.
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractiveBarDataModel extends BarDataModel {
    
    private static final long serialVersionUID = -5525319030969754880L;
  
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(InteractiveBarDataModel.class);
    
    /**If the different elements of this bar can't be draw because the bar
     * is to small this rectangle is calculated to reflect the proportion
     * of hilited rows in this bar. */
    private Rectangle m_hiliteRectangle;
    
    /**Constructor for class BarDataModel.
     * @param barName the name of this bar
     * @param color the color to use for this bar
     */
    protected InteractiveBarDataModel(final String barName, final Color color) {
        super(barName, color);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BarElementDataModel createElement(final Color color) {
        return new InteractiveBarElementDataModel(color);
    }
    
    /**
     * The proportional hilite rectangle of this bar which
     * could be displayed if the elements of this bar can't be draw.
     * @return the proportional hilite rectangle
     */
    public Rectangle getHiliteRectangle() {
        return m_hiliteRectangle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setBarRectangle(final Rectangle barRect, 
            final AggregationMethod aggrMethod, final HistogramLayout layout, 
            final int baseLine, final SortedSet<Color> barElementColors) {
        super.setBarRectangle(barRect, aggrMethod, layout, baseLine, 
                barElementColors);
        calculateHiliteRectangle(aggrMethod, layout);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void updateBarWidth(final int startX, final int barWidth, 
            final HistogramLayout layout, 
            final SortedSet<Color> barElementColors, 
            final AggregationMethod aggrMethod, final int baseLine) {
        super.updateBarWidth(startX, barWidth, layout, 
                barElementColors, aggrMethod, baseLine);
        calculateHiliteRectangle(aggrMethod, layout);
    }
    
    /**
     * This calculates the proportional hilite rectangle of this bar which
     * could be displayed if the elements of this bar can't be draw.
     * Set the hilite rectangle in the middle of the bar since we
     * @param aggrMethod the current {@link AggregationMethod}
     * @param layout the current {@link HistogramLayout}
     */
    private void calculateHiliteRectangle(final AggregationMethod aggrMethod, 
            final HistogramLayout layout) {
        final Rectangle barRectangle = getBarRectangle();
        if (isPresentable() || barRectangle == null) {
            m_hiliteRectangle = null;
            return;
        }
        final int noOfHilitedRows = getNoOfHilitedRows();
        if (noOfHilitedRows <= 0) {
            m_hiliteRectangle = null;
            return;
        }
        final int barY = (int)barRectangle.getY();
        final int barHeight = (int)barRectangle.getHeight();
        final int barWidth = (int)barRectangle.getWidth();
        final int rowCount = getRowCount();
        final double fraction = noOfHilitedRows / (double)rowCount;
        int hiliteHeight = (int)(barHeight * fraction);
        final int hiliteWidth = Math.max(
                (int)(barWidth 
        * AbstractHistogramVizModel.HILITE_RECT_WIDTH_FACTOR), 
        1);
        final int hiliteX = (int) (barRectangle.getX()
                + (barWidth - hiliteWidth) / 2);
        int hiliteY = barY;
        if (HistogramLayout.SIDE_BY_SIDE.equals(layout) 
                && getMinAggregationValue(aggrMethod, layout) < 0
                && getMaxAggregationValue(aggrMethod, layout) > 0) {
            //set the hilite rectangle in the side by side mode in the middle
            //if the minimum aggregation value is negative and the maximum
            //aggregation value is positive
            final int middleY = (int)(barY + (barHeight / 2.0));
            hiliteY = middleY - (hiliteHeight / 2);
        } else  if (getAggregationValue(aggrMethod) > 0) {
            hiliteY = hiliteY + barHeight - hiliteHeight;
        }
        //check for possible rounding errors
        if (hiliteHeight > barHeight) {
            hiliteHeight = barHeight;
            LOGGER.warn("Hilite rectangle higher than surrounding bar");
        }
        if (hiliteY < barY) {
            hiliteY = barY;
            LOGGER.warn("Hilite rectangle y coordinate above "
                    + "surrounding bar y coordinate");
        }
        m_hiliteRectangle = 
            new Rectangle(hiliteX, hiliteY, hiliteWidth, hiliteHeight);
    }
    
    /**
     * @return <code>true</code> if at least one row of this element is hilited
     */
    public boolean isHilited() {
        for (BarElementDataModel element : getElements()) {
            if (((InteractiveBarElementDataModel)element).isHilited()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the number of hilited rows in this bar.
     */
    public int getNoOfHilitedRows() {
        int noOfHilitedKeys = 0;
        for (BarElementDataModel element : getElements()) {
            noOfHilitedKeys += 
                ((InteractiveBarElementDataModel)element).getHiliteRowCount();
        }
        return noOfHilitedKeys;
    }

    /**
     * @param hilited the row keys to unhilite
     * @param aggrMethod the current {@link AggregationMethod}
     * @param layout the current {@link HistogramLayout}
     * @return if the hilite keys have changed
     */
    public boolean removeHilitedKeys(final Collection<DataCell> hilited, 
            final AggregationMethod aggrMethod, final HistogramLayout layout) {
        boolean changed = false;
        for (BarElementDataModel element : getElements()) {
            changed = 
                ((InteractiveBarElementDataModel)element).removeHilitedKeys(
                        hilited, aggrMethod) || changed;
        }
        if (changed) {
            calculateHiliteRectangle(aggrMethod, layout);
        }
        return changed;
    }

    /**
     * @param hilited the row keys to hilite
     * @param aggrMethod the current {@link AggregationMethod}
     * @param layout the current {@link HistogramLayout}
     * @return if the hilite keys have changed
     */
    public boolean setHilitedKeys(final Collection<DataCell> hilited, 
            final AggregationMethod aggrMethod, final HistogramLayout layout) {
        boolean changed = false;
        for (BarElementDataModel element : getElements()) {
            changed = 
                ((InteractiveBarElementDataModel)element).setHilitedKeys(
                        hilited, aggrMethod) || changed;
        }
        if (changed) {
            calculateHiliteRectangle(aggrMethod, layout);
        }
        return changed;
    }

    /**
     * Clears all hilite information.
     */
    public void clearHilite() {
        for (BarElementDataModel element : getElements()) {
            ((InteractiveBarElementDataModel)element).clearHilite();
        }
        //since no elements are hilited we have to reset the hilite rectangle
        m_hiliteRectangle = null;
    }
}
