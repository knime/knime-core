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
import java.util.Hashtable;
import java.util.Map;
import java.util.SortedSet;

import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.core.data.DataCell;
import org.knime.core.node.NodeLogger;

/**
 * This class holds the information of a histogram bar. Like the color to use
 * and the {@link BarElementDataModel} objects of this bar.
 * @author Tobias Koetter, University of Konstanz
 */
public class BarDataModel {
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(BarDataModel.class);
    /**
     * The space between to elements in the {@link HistogramLayout.SIDE_BY_SIDE}
     * layout in  pixel.
     */
    private static final int SPACE_BETWEEN_ELEMENTS = 1;
    
    private final Color m_color;
    
    private final Map<Color, BarElementDataModel> m_elements =
        new Hashtable<Color, BarElementDataModel>();
    
    private int m_rowCounter = 0;
    
    private double m_aggrSum = 0;
    
    private Rectangle m_barRectangle;
    
    /**Constructor for class BarDataModel.
     * @param color the color to use for this bar
     */
    protected BarDataModel(final Color color) {
        m_color = color;
    }
    
    /**
     * Adds a new row to this bar.
     * @param color the color of the data row
     * @param rowKey the row key
     * @param aggrVal the aggregation value
     */
    protected void addDataRow(final Color color, final DataCell rowKey, 
            final double aggrVal) {
        BarElementDataModel element = m_elements.get(color);
        if (element == null) {
            element = new BarElementDataModel(color);
            m_elements.put(color, element);
        }
        element.addDataRow(rowKey, aggrVal);
        m_aggrSum += aggrVal;
        m_rowCounter++;
    }

    /**
     * @return the color to use
     */
    public Color getColor() {
        return m_color;
    }
    
    /**
     * @param color the color of the element
     * @return the element with the given color or <code>null</code> if none
     * element with the given color exists
     */
    public BarElementDataModel getElement(final Color color) {
        return m_elements.get(color);
    }

    /**
     * @return all {@link BarElementDataModel} objects of this bar
     */
    public Collection<BarElementDataModel> getElements() {
        return m_elements.values();
    }
    
    /**
     * @return the number of elements
     */
    public int getNoOfElements() {
        return m_elements.size();
    }
    
    /**
     * @return the number of rows of this bar
     */
    public int getRowCount() {
        return m_rowCounter;
    }
    
    /**
     * @param method the {@link AggregationMethod} to use
     * @return the aggregation value of this bar
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
     * @param method the {@link AggregationMethod} to use
     * @param layout the histogram layout
     * @return the maximum aggregation value
     */
    public double getMaxAggregationValue(final AggregationMethod method, 
            final HistogramLayout layout) {
        if (HistogramLayout.STACKED.equals(layout)) {
            return getAggregationValue(method);
        } else if (HistogramLayout.SIDE_BY_SIDE.equals(layout)) {
            final Collection<BarElementDataModel> elements = getElements();
            double maxAggrValue = -Double.MAX_VALUE;
            for (BarElementDataModel element : elements) {
                final double value = element.getAggregationValue(method);
                if (value > maxAggrValue) {
                    maxAggrValue = value;
                }   
            }
            return maxAggrValue;
        } else {
            throw new IllegalArgumentException(
                    "Layout " + layout + " not supported");
        }
    }
    
    /**
     * @param method the {@link AggregationMethod} to use
     * @param layout the histogram layout
     * @return the minimum aggregation value
     */
    public double getMinAggregationValue(final AggregationMethod method, 
            final HistogramLayout layout) {
        if (HistogramLayout.STACKED.equals(layout)) {
            return getAggregationValue(method);
        } else if (HistogramLayout.SIDE_BY_SIDE.equals(layout)) {
            final Collection<BarElementDataModel> elements = getElements();
            double minAggrValue = Double.MAX_VALUE;
            for (BarElementDataModel element : elements) {
                final double value = element.getAggregationValue(method);
                if (value < minAggrValue) {
                    minAggrValue = value;
                }   
            }
            return minAggrValue;
        } else {
            throw new IllegalArgumentException(
                    "Layout " + layout + " not supported");
        }
    }

    /**
     * @return the {@link Rectangle} the bar should be drawn on the 
     * screen 
     */
    public Rectangle getBarRectangle() {
        return m_barRectangle;
    }

    /**
     * @param barRect the {@link Rectangle} the bar should be drawn on the 
     * screen
     * @param aggrMethod the aggregation method which should be used
     * @param layout the histogram layout
     * @param baseLine the x coordinate of the base line (0) on the screen 
     * @param barElementColors all element colors which define the order
     * the elements should be drawn
     */
    protected void setBarRectangle(final Rectangle barRect,
            final AggregationMethod aggrMethod, final HistogramLayout layout,
            final int baseLine, final SortedSet<Color> barElementColors) {
        m_barRectangle = barRect;
        if (barRect == null) {
            //also reset the element rectangles
            final Collection<BarElementDataModel> elements = 
                m_elements.values();
            for (BarElementDataModel element : elements) {
                element.setElementRectangle(null, aggrMethod);
            }
            return;
        }
        setElementRectangle(aggrMethod, layout, baseLine, barElementColors);
    }
    
    /**
     * @param aggrMethod the aggregation method which should be used
     * @param layout the histogram layout
     * @param baseLine the x coordinate of the base line (0) on the screen
     * @param barElementColors all element colors which define the order
     * the elements should be drawn
     */
    private void setElementRectangle(final AggregationMethod aggrMethod, 
            final HistogramLayout layout, final int baseLine, 
            final SortedSet<Color> barElementColors) {
        final double totalWidth = m_barRectangle.getWidth();
        int barWidth = (int)totalWidth;
        if (HistogramLayout.SIDE_BY_SIDE.equals(layout)) {
            final int noOfBars = barElementColors.size();
            barWidth = Math.max((int)(totalWidth 
                    - (SPACE_BETWEEN_ELEMENTS * noOfBars)) / noOfBars, 1);
            if (noOfBars * barWidth 
                    > totalWidth - (SPACE_BETWEEN_ELEMENTS * noOfBars)) {
                //the total bar width is not enough to draw all elements so we 
                //don't need to calculate any further instead we set have
                //to set all rectangles
                final Collection<BarElementDataModel> elements = 
                    m_elements.values();
                for (BarElementDataModel element : elements) {
                    element.setElementRectangle(null, aggrMethod);
                }
                return;
            }
        }
        //now we know the width of each bar lets calculate the height factor
        final int totalHeight = (int)m_barRectangle.getHeight();
        final double maxAggrVal = getMaxAggregationValue(aggrMethod, layout);
        final double minAggrVal = getMinAggregationValue(aggrMethod, layout);
        double valRange = Math.max(Math.abs(maxAggrVal), Math.abs(minAggrVal));
        if (minAggrVal < 0 && maxAggrVal > 0) {
            valRange = maxAggrVal + Math.abs(minAggrVal);
        }
        if (HistogramLayout.STACKED.equals(layout)) {
            //if the current layout is staked we have to be care full with
            //the value range
            if (AggregationMethod.AVERAGE.equals(aggrMethod)
                    || AggregationMethod.SUM.equals(aggrMethod)) {
                //if the current aggregation method is average we have to 
                //calculate the height proportional to the sum of all 
                //averages
                //if the method is sum we have to handle the 
                //negative values
                valRange = 0;
                for (BarElementDataModel element : m_elements.values()) {
                    valRange += 
                        Math.abs(element.getAggregationValue(aggrMethod));
                }
            }  
        }
        
        final double heightPerVal = totalHeight / valRange;
        final int startX = (int)m_barRectangle.getX();
        final int startY = (int)m_barRectangle.getY();
        if (HistogramLayout.STACKED.equals(layout)) {
            //the user wants the elements on top of each other
            //so we have to change the y coordinate
            int yCoord = startY;
            double elementHeightSum = 0;
            int elementCounter = 0;
            int noOfElements = m_elements.size();
            for (Color elementColor : barElementColors) {
                final BarElementDataModel element = 
                    m_elements.get(elementColor);
                if (element == null) {
                    continue;
                }
                elementCounter++;
                //the user wants the elements next to each other;
                final double aggrVal = element.getAggregationValue(aggrMethod);
                double elementVal = Math.abs(aggrVal);
                if (minAggrVal < 0 && aggrVal >= 0) {
                    elementVal += Math.abs(minAggrVal);
                }
                //add the minimum aggregation value to the real value if it's 
                //negative
                int elementHeight = 
                    (int)Math.round(Math.max((heightPerVal * elementVal), 1.0));
                elementHeightSum += elementHeight;
                if (elementCounter == noOfElements) {
                    //this is the last element of this bar handle 
                    //possible rounding errors
                    if (elementHeightSum < totalHeight 
                            || elementHeightSum > totalHeight) {
                        final double diff = totalHeight - elementHeightSum;
                        elementHeight = 
                            (int)Math.round(elementHeight + diff);
                        LOGGER.debug("Height diff. on last element in "
                                + " stacked visualization because "
                                + "of rounding errors: " 
                                + diff);
                        LOGGER.debug("Bar height: " + totalHeight
                                + " Height sum without adjustment: " 
                                + elementHeightSum 
                                + " No of elements: " + m_elements.size());
                    }
                }
                final Rectangle elementRect =  
                    new Rectangle(startX, yCoord, barWidth, elementHeight);
                element.setElementRectangle(elementRect, aggrMethod);
                //add the bar height to the current y coordinate to draw
                //the next element below the current one
                yCoord += elementHeight;
            }
        } else if (HistogramLayout.SIDE_BY_SIDE.equals(layout)) {
            //the user wants the elements next to each other
            //so we have to change the x coordinate
            int xCoord = startX + SPACE_BETWEEN_ELEMENTS;
            for (Color elementColor : barElementColors) {
                final BarElementDataModel element = 
                    m_elements.get(elementColor);
                if (element != null) {
                    //the user wants the elements next to each other;
                    final double aggrVal = 
                        element.getAggregationValue(aggrMethod);
                    //calculate the bar height
                    int barHeight = Math.max((int)(
                            heightPerVal * Math.abs(aggrVal)), 1);
                    if (barHeight > totalHeight) {
                        final int diff = barHeight - totalHeight;
                        barHeight -= diff;
                        LOGGER.debug("Height diff. in side-by-side layout."
                                + " Element(Bar) higher than surrounding bar: " 
                                + diff);
                    }
                    //calculate the position of the y coordinate
                    int yCoord = 0;
                    if (aggrVal >= 0) {
                        //if it's a positive value the start point is the
                        //baseline minus the height of the bar
                        yCoord = baseLine - barHeight;
                    } else {
                        //if it's a negative value the top left corner start 
                        //point is the base line
                        yCoord = baseLine;
                    }
                    final Rectangle elementRect =  
                        new Rectangle(xCoord, yCoord, barWidth, barHeight);
                    element.setElementRectangle(elementRect, aggrMethod);
                }
                //add the bar width and the space between bars to the current
                //x coordinate
                xCoord += barWidth + SPACE_BETWEEN_ELEMENTS;
            }
        } else {
            throw new IllegalArgumentException(
                    "Layout " + layout + " not supported");
        }
        return;
    }

    /**
     * @return <code>true</code> if one of the elements of this bar is selected
     */
    public boolean isSelected() {
        final Collection<BarElementDataModel> name = m_elements.values();
        for (BarElementDataModel element : name) {
            if (element.isSelected()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * @see java.lang.Object#clone()
     */
    @Override
    public BarDataModel clone() {
        final BarDataModel clone = new BarDataModel(m_color);
        clone.m_barRectangle = m_barRectangle;
        clone.m_aggrSum = m_aggrSum;
        clone.m_rowCounter = m_rowCounter;
        final Collection<BarElementDataModel> elements = m_elements.values();
        for (BarElementDataModel element : elements) {
            clone.m_elements.put(element.getColor(), element.clone());
        }
        return clone;
    }
}
