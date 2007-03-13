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
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.NodeLogger;

/**
 * This class holds the information of a histogram bar. Like the color to use
 * and the {@link InteractiveBarElementDataModel} objects of this bar.
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractiveBarDataModel {
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(InteractiveBarDataModel.class);
    /**
     * The space between to elements in the {@link HistogramLayout.SIDE_BY_SIDE}
     * layout in  pixel.
     */
    private static final int SPACE_BETWEEN_ELEMENTS = 1;
    
    private final Color m_color;
    
    private final Map<Color, InteractiveBarElementDataModel> m_elements =
        new HashMap<Color, InteractiveBarElementDataModel>();
    
    /**The number of rows including empty value rows.*/
    private int m_rowCounter = 0;
    
    /**The number of values without missing values!*/
    private int m_valueCount = 0;
    
    private double m_aggrSum = 0;
    
    private boolean m_drawElements = true;
    
    private Rectangle m_barRectangle;
    
    /**If the different elements of this bar can't be draw because the bar
     * is to small this rectangle is calculated to reflect the proportion
     * of hilited rows in this bar. */
    private Rectangle m_hiliteRectangle;
    
    private boolean m_isSelected = false;
    
    /**Constructor for class BarDataModel.
     * @param color the color to use for this bar
     */
    protected InteractiveBarDataModel(final Color color) {
        m_color = color;
    }
    
    /**
     * Adds a new row to this bar.
     * @param color the color of the data row
     * @param rowKey the row key
     * @param cell the aggregation value cell
     */
    protected void addDataRow(final Color color, final DataCell rowKey, 
            final DataCell cell) {
        InteractiveBarElementDataModel element = m_elements.get(color);
        if (element == null) {
            element = new InteractiveBarElementDataModel(color);
            m_elements.put(color, element);
        }
        if (!cell.isMissing()) {
            if (!cell.getType().isCompatible(DoubleValue.class)) {
                throw new IllegalArgumentException(
                        "DataCell should be numeric");
            }
            m_aggrSum += ((DoubleValue)cell).getDoubleValue();
            m_valueCount++;
        }
        element.addDataRow(rowKey, cell);
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
    public InteractiveBarElementDataModel getElement(final Color color) {
        return m_elements.get(color);
    }

    /**
     * @return all {@link InteractiveBarElementDataModel} objects of this bar
     */
    public Collection<InteractiveBarElementDataModel> getElements() {
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
            if (m_valueCount == 0) {
                //avoid division by 0
                return 0;
            }
            return m_aggrSum / m_valueCount;
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
            final Collection<InteractiveBarElementDataModel> elements = getElements();
            double maxAggrValue = -Double.MAX_VALUE;
            for (InteractiveBarElementDataModel element : elements) {
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
            final Collection<InteractiveBarElementDataModel> elements = getElements();
            double minAggrValue = Double.MAX_VALUE;
            for (InteractiveBarElementDataModel element : elements) {
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
     * The proportional hilite rectangle of this bar which
     * could be displayed if the elements of this bar can't be draw.
     * @return the proportional hilite rectangle
     */
    public Rectangle getHiliteRectangle() {
        return m_hiliteRectangle;
    }
    
    /**
     * This calculates the proportional hilite rectangle of this bar which
     * could be displayed if the elements of this bar can't be draw.
     * Set the hilite rectangle in the middle of the bar since we
     * @param layout the current {@link HistogramLayout}
     */
    private void calculateHiliteRectangle(final AggregationMethod aggrMethod,
            final HistogramLayout layout) {
        if (m_drawElements || m_barRectangle == null) {
            m_hiliteRectangle = null;
            return;
        }
        final int noOfHilitedRows = getNoOfHilitedRows();
        if (noOfHilitedRows <= 0) {
            m_hiliteRectangle = null;
            return;
        }
        final int barY = (int)m_barRectangle.getY();
        final int barHeight = (int)m_barRectangle.getHeight();
        final int barWidth = (int)m_barRectangle.getWidth();
        final int rowCount = getRowCount();
        final double fraction = noOfHilitedRows / (double)rowCount;
        int hiliteHeight = (int)(barHeight * fraction);
        final int hiliteWidth = Math.max(
                (int)(barWidth 
        * AbstractHistogramVizModel.HILITE_RECT_WIDTH_FACTOR), 
        1);
        final int hiliteX = (int) (m_barRectangle.getX()
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
        setElementRectangle(aggrMethod, layout, baseLine, barElementColors);
        calculateHiliteRectangle(aggrMethod, layout);
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
        if (m_barRectangle == null) {
            //also reset the element rectangles
            final Collection<InteractiveBarElementDataModel> elements = 
                m_elements.values();
            for (InteractiveBarElementDataModel element : elements) {
                element.setElementRectangle(null, aggrMethod);
            }
            return;
        }
        final double maxAggrVal = getMaxAggregationValue(aggrMethod, layout);
        final double minAggrVal = getMinAggregationValue(aggrMethod, layout);
        double valRange;
        if (minAggrVal < 0 && maxAggrVal > 0) {
            //if the bar contains negative and positive elements
            //we have to add the min and max aggregation value
            //to get the full range
            valRange = maxAggrVal + Math.abs(minAggrVal);
        } else {
            //if the bar contains either negative or positive elements
            //simply take the maximum since one of them is zero
            valRange = Math.max(Math.abs(maxAggrVal), Math.abs(minAggrVal));
        }
        final int barHeight = (int)m_barRectangle.getHeight();
        final int barWidth = (int)m_barRectangle.getWidth();
        final int noOfElements = m_elements.size();
        m_drawElements = elementsFitInBar(layout, barElementColors, 
                noOfElements, barWidth, barHeight);
        if (!m_drawElements) {
            return;
        }
        if (HistogramLayout.STACKED.equals(layout)) {
            setStackedRectangles(m_barRectangle, barElementColors, 
                    valRange, aggrMethod, minAggrVal);
        } else if (HistogramLayout.SIDE_BY_SIDE.equals(layout)) {
            setSideBySideRectangles(m_barRectangle, barElementColors, 
                    valRange, aggrMethod, baseLine);
        } else {
            throw new IllegalArgumentException(
                    "Layout " + layout + " not supported");
        }
        return;
    }

    private void setSideBySideRectangles(final Rectangle bounds, 
            final SortedSet<Color> barElementColors, 
            final double valRange, final AggregationMethod aggrMethod, 
            final int baseLine) {
        LOGGER.debug("Entering setSideBySideRectangles"
                + "(bounds, barElementColors, valRange, aggrMethod, baseLine) "
                + "of class BarDataModel.");
        final int barHeight = (int)bounds.getHeight();
        //check if all elements fit side by side
        final double heightPerVal = barHeight / valRange;
        final int startX = (int)bounds.getX();
        final int barWidth = (int)m_barRectangle.getWidth();
        final int noOfBars = barElementColors.size();
        final int elementWidth = 
            calculateSideBySideElementWidth(barElementColors, barWidth);
        LOGGER.debug("Bar values (x,height,width, totalNoOf): " 
                + startX + ", "
                + barHeight + ", "
                + barWidth 
                + noOfBars);
        LOGGER.debug("Value range: " + valRange
                + " height per value:" + heightPerVal);
        //the user wants the elements next to each other
        //so we have to change the x coordinate
        int xCoord = startX + SPACE_BETWEEN_ELEMENTS;
        for (Color elementColor : barElementColors) {
            final InteractiveBarElementDataModel element = 
                m_elements.get(elementColor);
            if (element != null) {
                //the user wants the elements next to each other;
                final double aggrVal = 
                    element.getAggregationValue(aggrMethod);
                //calculate the bar height
                int elementHeight = Math.max((int)(
                        heightPerVal * Math.abs(aggrVal)), 
                        AbstractHistogramVizModel.MINIMUM_BAR_HEIGHT);
                if (elementHeight > barHeight) {
                    final int diff = elementHeight - barHeight;
                    elementHeight -= diff;
                    LOGGER.debug("Height diff. in side-by-side layout."
                            + " Element(Bar) higher than surrounding bar: " 
                            + diff);
                }
                //calculate the position of the y coordinate
                int yCoord = 0;
                if (aggrVal >= 0) {
                    //if it's a positive value the start point is the
                    //baseline minus the height of the bar
                    yCoord = baseLine - elementHeight;
                } else {
                    //if it's a negative value the top left corner start 
                    //point is the base line
                    yCoord = baseLine;
                }
                LOGGER.debug("xCoord: " + xCoord 
                        + " yCoord: " + yCoord
                        + " elementAggrVal: " + aggrVal
                        + " elementWidth:" + elementWidth
                        + " elementHeight:" + elementHeight);
                final Rectangle elementRect =  
                    new Rectangle(xCoord, yCoord, elementWidth, elementHeight);
                element.setElementRectangle(elementRect, aggrMethod);
            }
            //add the bar width and the space between bars to the current
            //x coordinate
            xCoord += elementWidth + SPACE_BETWEEN_ELEMENTS;
        }
        LOGGER.debug("Exiting setSideBySideRectangles"
                + "(bounds, barElementColors, valRange, aggrMethod, baseLine) "
                + "of class BarDataModel.");
    }

    private void setStackedRectangles(final Rectangle bounds, 
            final SortedSet<Color> barElementColors, final double valRange, 
            final AggregationMethod aggrMethod, final double minAggrVal) {
        LOGGER.debug("Entering setStackedRectangles("
                + "bounds, barElementColors, valRange, aggrMethod, minAggrVal) "
                + "of class BarDataModel.");
        //the user wants the elements on top of each other
        final int startX = (int)bounds.getX();
        final int startY = (int)bounds.getY();
        final int barHeight = (int)bounds.getHeight();
        final int barWidth = (int)bounds.getWidth();
        final double barAggrVal = getAggregationValue(aggrMethod);
        LOGGER.debug("Bar values (x,y,height,width,aggrVal): " 
                + startX + ", "
                + startY + ", "
                + barHeight + ", "
                + barWidth + ", "
                + barAggrVal);
        //we have to be care full with the value range in stacked layout
        //because of the mixture of positive and negatives
        double stackedValRange = valRange;
        if ((AggregationMethod.AVERAGE.equals(aggrMethod)
                || AggregationMethod.SUM.equals(aggrMethod))) {
            //if the current aggregation method is average or sum 
            //we have to handle the negative values as positives
//            if (minAggrVal < 0) {
//                stackedValRange = Math.abs(minAggrVal);
//            } else {
//                stackedValRange = 0;
//            }
            stackedValRange = 0;
            LOGGER.debug("Calculating stacked value range.Starting with: "
                    + stackedValRange);
            for (InteractiveBarElementDataModel element : m_elements.values()) {
                stackedValRange += 
                    Math.abs(element.getAggregationValue(aggrMethod));
            }
            LOGGER.debug("Calculating stacked bin height "
                    + "using stackedValRange: " + stackedValRange);
        }
        final double heightPerAbsVal = bounds.getHeight() / stackedValRange;
        int yCoord = startY;
        double elementHeightSum = 0;
        int elementCounter = 0;
        int noOfElements = m_elements.size();
        LOGGER.debug("Stacked valRange: " + stackedValRange
                + " height per absVal: " + heightPerAbsVal
                + " noOfElements: " + noOfElements);
        for (Color elementColor : barElementColors) {
            final InteractiveBarElementDataModel element = 
                m_elements.get(elementColor);
            if (element == null) {
                continue;
            }
            elementCounter++;
            //the user wants the elements next to each other;
            final double aggrVal = element.getAggregationValue(aggrMethod);
            
            double elementAbsVal = Math.abs(aggrVal);
//            if (minAggrVal < 0 && aggrVal > 0) {
//                elementAbsVal += Math.abs(minAggrVal);
//            }
            //add the minimum aggregation value to the real value if it's 
            //negative
            final double rawElementHeight = 
                Math.max((heightPerAbsVal * elementAbsVal), 1.0);
            int elementHeight = (int)Math.round(rawElementHeight);
            elementHeightSum += elementHeight;
            if (elementCounter == noOfElements) {
                //this is the last element of this bar handle 
                //possible rounding errors
                if (elementHeightSum < barHeight 
                        || elementHeightSum > barHeight) {
                    final double diff = barHeight - elementHeightSum;
                    elementHeight = 
                        (int)Math.round(elementHeight + diff);
                    if (elementHeight < 1) {
                        LOGGER.warn("******Unable to correct height diff. for "
                                + "bar " + barAggrVal
                                + ". Last element to low for height "
                                + "adjustment.");
                    }
                    LOGGER.warn("++++++++Height diff. for bar " + barAggrVal 
                            + " in last element: " + diff 
                            + ". Bar height: " + barHeight
                            + " Height sum without adjustment: " 
                            + elementHeightSum 
                            + " No of elements: " + m_elements.size());
                }
            }
            LOGGER.debug("Element aggrVal: " + aggrVal
                    + " element absVal: " + elementAbsVal
                    + " xCoord: " + startX
                    + " yCoord: " + yCoord
                    + " elementWidth: " + barWidth
                    + " rawElementHeight: " + rawElementHeight
                    + " adjusted elementHeight: " + elementHeight);
            
            final Rectangle elementRect =  
                new Rectangle(startX, yCoord, barWidth, elementHeight);
            element.setElementRectangle(elementRect, aggrMethod);
            //add the bar height to the current y coordinate to draw
            //the next element below the current one
            yCoord += elementHeight;
        }
        LOGGER.debug("Exiting setStackedRectangles("
                + "bounds, barElementColors, valRange, aggrMethod, minAggrVal) "
                + "of class BarDataModel.");
    }

    /**
     * @param startX the x coordinate
     * @param barWidth the new bar width
     * @param layout the current {@link HistogramLayout}
     * @param barElementColors all element colors which define the order
     * the elements should be drawn
     * @param aggrMethod the {@link AggregationMethod} to use
     * @param baseLine the base line
     */
    public void updateBarWidth(final int startX, final int barWidth,
            final HistogramLayout layout, 
            final SortedSet<Color> barElementColors,
            final AggregationMethod aggrMethod, final int baseLine) {
        if (m_barRectangle == null) {
            return;
        }
        final boolean drawElementsBefore = m_drawElements;
        final int yCoord = (int)m_barRectangle.getY();
        final int barHeight = (int)m_barRectangle.getHeight();
        m_barRectangle.setBounds(startX, yCoord, barWidth, barHeight);
        final int totalNoOfElements = barElementColors.size();
        final int noOfElements = m_elements.size();
        calculateHiliteRectangle(aggrMethod, layout);
        m_drawElements = elementsFitInBar(layout, barElementColors,
                noOfElements, barWidth, barHeight);
        if (!m_drawElements) {
            //if the elements doesn't fit any way return here
            return;
        }
        if (!drawElementsBefore) {
            //if the elements couldn't be draw before but now recalculate 
            //them
            setElementRectangle(aggrMethod, layout, baseLine, barElementColors);
            return;
        }
        
        if (HistogramLayout.STACKED.equals(layout)) {
            for (Color elementColor : barElementColors) {
                final InteractiveBarElementDataModel element = 
                    m_elements.get(elementColor);
                if (element != null) {
                    element.updateElementWidth(startX, 
                            barWidth, aggrMethod);
                }
            }
        } else if (HistogramLayout.SIDE_BY_SIDE.equals(layout)) {
            int xCoord = startX + SPACE_BETWEEN_ELEMENTS;
            final int elementWidth = Math.max((barWidth 
                    - (SPACE_BETWEEN_ELEMENTS * totalNoOfElements)) 
                    / totalNoOfElements, 1);
            for (Color elementColor : barElementColors) {
                final InteractiveBarElementDataModel element = 
                    m_elements.get(elementColor);
                if (element != null) {
                    element.updateElementWidth(xCoord, 
                            elementWidth, aggrMethod);
                }
//              add the bar width and the space between bars to the current
                //x coordinate
                xCoord += elementWidth + SPACE_BETWEEN_ELEMENTS;
            }
        } else {
            throw new IllegalArgumentException(
                    "Layout " + layout + " not supported");
        }
        return;
    }
    
    /**
     * Checks if all elements fit in the surrounding bar.
     * @param layout the {@link HistogramLayout}
     * @param barElementColors the total number of bars in the 
     * side by side layout
     * @param noOfElements the number of elements which should fit
     * @param barWidth the width of the bar
     * @param barHeight the height of the bar
     * @param elementWidth the width of each element
     * @return <code>true</code> if the given number of elements fit into
     * the given bar ranges for the given layout 
     */
    private static boolean elementsFitInBar(final HistogramLayout layout, 
            final SortedSet<Color> barElementColors, final int noOfElements, 
            final int barWidth, final int barHeight) {
        if (HistogramLayout.SIDE_BY_SIDE.equals(layout)) {
            final int noOfColors = barElementColors.size();
            final int elementWidth = 
                calculateSideBySideElementWidth(barElementColors, barWidth);
            if (noOfColors * elementWidth 
                    > barWidth - (SPACE_BETWEEN_ELEMENTS * noOfColors)) {
                return false;
            }
        } else if (HistogramLayout.STACKED.equals(layout)) {
            if (noOfElements > barHeight) {
                //we have more elements than pixel
                return false;
            }
        }
        return true;
    }

    private static int calculateSideBySideElementWidth(
            final SortedSet<Color> barElementColors, 
            final int barWidth) {
        final int noOfBars = barElementColors.size();
        return Math.max((barWidth 
                - (SPACE_BETWEEN_ELEMENTS * noOfBars)) 
                    / noOfBars, 1);
    }
    
    /**
     * @return <code>true</code> if the elements should be drawn
     */
    public boolean isDrawElements() {
        return m_drawElements;
    }
    
    /**
     * @return <code>true</code> if one of the elements of this bar is selected
     */
    public boolean isSelected() {
        return m_isSelected;
    }

    /**
     * @param selected <code>true</code> if this bar is selected
     * @return <code>true</code> if the parameter has changed
     */
    protected boolean setSelected(final boolean selected) {
        if (m_isSelected == selected) {
            return false;
        }
        m_isSelected = selected;
        for (InteractiveBarElementDataModel element : getElements()) {
            element.setSelected(selected);
        } return true;
    }

    /**
     * @return the number of hilited rows in this bar.
     */
    public int getNoOfHilitedRows() {
        int noOfHilitedKeys = 0;
        for (InteractiveBarElementDataModel element : getElements()) {
            noOfHilitedKeys += element.getHiliteRowCount();
        }
        return noOfHilitedKeys;
    }
    
    /**
     * @return <code>true</code> if at least one row of this element is hilited
     */
    public boolean isHilited() {
        for (InteractiveBarElementDataModel element : getElements()) {
            if (element.isHilited()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * @param hilited the row keys to unhilite
     * @param aggrMethod the current {@link AggregationMethod}
     * @param layout the current {@link HistogramLayout}
     */
    public void removeHilitedKeys(final Collection<DataCell> hilited, 
            final AggregationMethod aggrMethod, final HistogramLayout layout) {
        boolean changed = false;
        for (InteractiveBarElementDataModel element : getElements()) {
            changed = element.removeHilitedKeys(hilited, aggrMethod) || changed;
        }
        if (changed) {
            calculateHiliteRectangle(aggrMethod, layout);
        }
    }

    /**
     * @param hilited the row keys to hilite
     * @param aggrMethod the current {@link AggregationMethod}
     * @param layout the current {@link HistogramLayout}
     */
    public void setHilitedKeys(final Collection<DataCell> hilited, 
            final AggregationMethod aggrMethod, final HistogramLayout layout) {
        boolean changed = false;
        for (InteractiveBarElementDataModel element : getElements()) {
            changed = element.setHilitedKeys(hilited, aggrMethod) || changed;
        }
        if (changed) {
            calculateHiliteRectangle(aggrMethod, layout);
        }
    }

    /**
     * Clears all hilite information.
     */
    public void clearHilite() {
        for (InteractiveBarElementDataModel element : getElements()) {
            element.clearHilite();
        }
        //since no elements are hilited we have to reset the hilite rectangle
        m_hiliteRectangle = null;
    }

    /**
     * @param point the {@link Point} to check
     * @return <code>true</code> if at least one element of the bar contains
     * the point
     */
    public boolean selectElement(final Point point) {
        if (m_barRectangle != null && m_barRectangle.contains(point)) {
//          if the bar is to small to draw the different
            //elements we have to select all elements 
            //of this bar
            if (!m_drawElements) {
                for (final InteractiveBarElementDataModel element : getElements()) {
                    element.setSelected(true);
                }
                m_isSelected = true;
            } else {
                for (final InteractiveBarElementDataModel element : getElements()) {
                    m_isSelected = element.selectElement(point) || m_isSelected;
                }
            }
        } else {
            setSelected(false);
        }
        return m_isSelected;
    }

    /**
     * Selects all element of this bar which intersect the given 
     * rectangle.
     * @param rect the {@link Rectangle} to check
     * @return <code>true</code> if at least one element of the bar 
     * intersects the rectangle
     */
    public boolean selectElement(final Rectangle rect) {
        if (m_barRectangle != null && m_barRectangle.intersects(rect)) {
//          if the bar is to small to draw the different
            //elements we have to select all elements 
            //of this bar
            if (!m_drawElements) {
                for (final InteractiveBarElementDataModel element : getElements()) {
                    element.setSelected(true);
                }
                m_isSelected = true;
            } else {
                for (final InteractiveBarElementDataModel element : getElements()) {
                    m_isSelected = element.selectElement(rect) || m_isSelected;
                }
            }
        } else {
            setSelected(false);
        }
        return m_isSelected;
    }
}
