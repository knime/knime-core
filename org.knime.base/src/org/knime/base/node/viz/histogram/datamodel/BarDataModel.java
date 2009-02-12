/*
 * -------------------------------------------------------------------
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
 *    13.03.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.datamodel;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.aggregation.AggregationValModel;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel.HistogramHiliteCalculator;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

/**
 * This class represents one bar in the histogram. A bar corresponds to one
 * selected aggregation column and belongs to one
 * {@link org.knime.base.node.viz.histogram.datamodel.BinDataModel}.
 * It contains one or more {@link BarElementDataModel}.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class BarDataModel extends AggregationValModel<BarElementDataModel,
Rectangle2D, Rectangle2D>
implements Serializable {

    private static final long serialVersionUID = 2839475106700548682L;

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(BarDataModel.class);

    private static final String CFG_ELEMENT_COUNT = "elementCount";

    private static final String CFG_BAR_ELEMENT = "barElement_";

    /**The surrounding rectangle is used to distinguish between multiple
     * selected aggregation columns.*/
    private Rectangle2D m_surroundingRectangle;
//    /**The bar rectangle is the main rectangle which contains the elements.*/
//    private Rectangle m_barRectangle;

    /**Constructor for class BarDataModel.
     * @param barName the name of this bar
     * @param color the color to use for this bar
     */
    protected BarDataModel(final String barName, final Color color) {
        super(barName, color, false);
    }

    /**Constructor for class BarDataModel.
     * @param barName the name of this bar
     * @param color the color to use for this bar
     * @param supportHiliting if hiliting should be supported
     */
    protected BarDataModel(final String barName, final Color color,
            final boolean supportHiliting) {
        super(barName, color, supportHiliting);
    }

    /**Constructor for class BarDataModel (used for cloning).
     * @param barName
     * @param color
     * @param elements
     * @param rowCounter
     * @param valueCounter
     * @param aggrSum
     */
    private BarDataModel(final String barName, final Color color,
            final Map<Color, BarElementDataModel> elements,
            final int rowCounter, final int valueCounter,
            final double aggrSum, final boolean supportHiliting) {
        super(barName, color, elements, rowCounter, valueCounter, aggrSum,
                supportHiliting);
    }

    private BarDataModel(final ConfigRO config,
            final ExecutionMonitor exec)
    throws InvalidSettingsException, CanceledExecutionException {
        super(config, exec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BarElementDataModel createElement(final Color color) {
        return new BarElementDataModel(color, false);
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
                    > barWidth
                    - (AbstractHistogramVizModel.SPACE_BETWEEN_ELEMENTS
                            * (noOfColors - 1))) {
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

    /**
     * @return the {@link Rectangle} the aggregation color of this bar
     *  should be drawn on the screen
     */
    public Rectangle2D getSurroundingRectangle() {
        return m_surroundingRectangle;
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
            final Collection<BarElementDataModel> elements =
                    getElements();
            double minAggrValue = Double.MAX_VALUE;
            for (final BarElementDataModel element : elements) {
                final double value = element.getAggregationValue(method);
                if (value < minAggrValue) {
                    minAggrValue = value;
                }
            }
            return minAggrValue;
        } else {
            throw new IllegalArgumentException("Layout " + layout
                    + " not supported");
        }
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
            final Collection<BarElementDataModel> elements =
                    getElements();
            double maxAggrValue = -Double.MAX_VALUE;
            for (final BarElementDataModel element : elements) {
                final double value = element.getAggregationValue(method);
                if (value > maxAggrValue) {
                    maxAggrValue = value;
                }
            }
            return maxAggrValue;
        } else {
            throw new IllegalArgumentException("Layout " + layout
                    + " not supported");
        }
    }

    /**
     * @param barRect the {@link Rectangle} the bar should be drawn on the
     * screen
     * @param baseLine the x coordinate of the base line (0) on the screen
     * @param barElementColors all element colors which define the order
     * the elements should be drawn
     * @param calculator the hilite shape calculator
     */
    protected void setBarRectangle(final Rectangle2D barRect,
            final int baseLine, final SortedSet<Color> barElementColors,
            final HistogramHiliteCalculator calculator) {
        setRectangle(barRect, baseLine, calculator);
        setElementRectangle(baseLine, barElementColors, calculator);
    }

    private void setRectangle(final Rectangle2D barRect, final int baseLine,
            final HistogramHiliteCalculator calculator) {
        setShape(barRect, calculator);
        m_surroundingRectangle =
            AbstractHistogramVizModel.calculateSurroundingRectangle(barRect,
                    baseLine, AbstractHistogramVizModel.BAR_SURROUNDING_SPACE);
    }

    /**
     * @param aggrMethod the aggregation method which should be used
     * @param layout the histogram layout
     * @param baseLine the x coordinate of the base line (0) on the screen
     * @param barElementColors all element colors which define the order
     * the elements should be drawn
     */
    private void setElementRectangle(final int baseLine,
            final SortedSet<Color> barElementColors,
            final HistogramHiliteCalculator calculator) {
        final Rectangle2D barRectangle = getShape();
        if (barRectangle == null) {
            //also reset the element rectangles
            final Collection<BarElementDataModel> elements =
                getElements();
            for (final BarElementDataModel element : elements) {
                element.setRectangle(null, calculator);
            }
            return;
        }
        final AggregationMethod aggrMethod = calculator.getAggrMethod();
        final HistogramLayout layout = calculator.getLayout();
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
        final int barHeight = (int)barRectangle.getHeight();
        final int barWidth = (int)barRectangle.getWidth();
        final int noOfElements = getNoOfElements();
        setPresentable(elementsFitInBar(layout, barElementColors,
                noOfElements, barWidth, barHeight), calculator);
        if (!isPresentable()) {
            return;
        }
        if (HistogramLayout.STACKED.equals(layout)) {
            setStackedRectangles(barRectangle, barElementColors,
                    valRange, aggrMethod, calculator);
        } else if (HistogramLayout.SIDE_BY_SIDE.equals(layout)) {
            setSideBySideRectangles(barRectangle, barElementColors,
                    valRange, aggrMethod, baseLine, calculator);
        } else {
            throw new IllegalArgumentException(
                    "Layout " + layout + " not supported");
        }
        return;
    }

    private void setSideBySideRectangles(final Rectangle2D bounds,
            final SortedSet<Color> barElementColors, final double valRange,
            final AggregationMethod aggrMethod, final int baseLine,
            final HistogramHiliteCalculator calculator) {
        LOGGER.debug("Entering setSideBySideRectangles"
                + "(bounds, barElementColors, valRange, aggrMethod, baseLine) "
                + "of class BarDataModel.");
        final int barHeight = (int)bounds.getHeight();
        //check if all elements fit side by side
        final double heightPerVal = barHeight / valRange;
        final int barX = (int)bounds.getX();
        final int barY = (int)bounds.getY();
        final int barWidth = (int)bounds.getWidth();
        final int noOfBars = barElementColors.size();
        final int elementWidth =
            calculateSideBySideElementWidth(barElementColors, barWidth);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Bar values (x,height,width, totalNoOf): "
                    + barX + ", "
                    + barHeight + ", "
                    + barWidth
                    + noOfBars);
            LOGGER.debug("Value range: " + valRange
                    + " height per value:" + heightPerVal);
        }
        //the user wants the elements next to each other
        //so we have to change the x coordinate
        int xCoord = barX;
        for (final Color elementColor : barElementColors) {
            final BarElementDataModel element =
                getElement(elementColor);
            if (element != null) {
                //the user wants the elements next to each other;
                final double aggrVal =
                    element.getAggregationValue(aggrMethod);
                final Rectangle elementRect =
                    BarDataModel.calculateBarRectangle(baseLine,
                            barHeight, barY, heightPerVal, aggrVal, xCoord,
                            elementWidth);
                element.setRectangle(elementRect, calculator);
            }
            //add the bar width and the space between bars to the current
            //x coordinate
            xCoord += elementWidth
                + AbstractHistogramVizModel.SPACE_BETWEEN_ELEMENTS;
        }
        LOGGER.debug("Exiting setSideBySideRectangles"
                + "(bounds, barElementColors, valRange, aggrMethod, baseLine) "
                + "of class BarDataModel.");
    }

    private void setStackedRectangles(final Rectangle2D bounds,
            final SortedSet<Color> barElementColors, final double valRange,
            final AggregationMethod aggrMethod,
            final HistogramHiliteCalculator calculator) {
            //the user wants the elements on top of each other
            final int startX = (int)bounds.getX();
            final int startY = (int)bounds.getY();
            final int barHeight = (int)bounds.getHeight();
            final int barWidth = (int)bounds.getWidth();
            final double barAggrVal = getAggregationValue(aggrMethod);
//            if (LOGGER.isDebugEnabled()) {
//                LOGGER.debug("Bar values (x,y,height,width,aggrVal): "
//                        + startX + ", "
//                        + startY + ", "
//                        + barHeight + ", "
//                        + barWidth + ", "
//                        + barAggrVal);
//            }
            //we have to be care full with the value range in stacked layout
            //because of the mixture of positive and negatives
            double stackedValRange = valRange;
            if ((AggregationMethod.AVERAGE.equals(aggrMethod)
                    || AggregationMethod.SUM.equals(aggrMethod))) {
                stackedValRange = 0;
//                if (LOGGER.isDebugEnabled()) {
//                    LOGGER.debug(
//                            "Calculating stacked value range.Starting with: "
//                            + stackedValRange);
//                }
                for (final BarElementDataModel element : getElements()) {
                    stackedValRange +=
                        Math.abs(element.getAggregationValue(aggrMethod));
                }
//                if (LOGGER.isDebugEnabled()) {
//                    LOGGER.debug("Calculating stacked bin height "
//                            + "using stackedValRange: " + stackedValRange);
//                }
            }
            final double heightPerAbsVal = bounds.getHeight() / stackedValRange;
            int yCoord = startY;
            double elementHeightSum = 0;
            int elementCounter = 0;
            final int noOfElements = getNoOfElements();
//            if (LOGGER.isDebugEnabled()) {
//                LOGGER.debug("Stacked valRange: " + stackedValRange
//                        + " height per absVal: " + heightPerAbsVal
//                        + " noOfElements: " + noOfElements);
//            }
            for (final Color elementColor : barElementColors) {
                final BarElementDataModel element =
                    getElement(elementColor);
                if (element == null) {
                    continue;
                }
                elementCounter++;
                //the user wants the elements next to each other;
                final double aggrVal = element.getAggregationValue(aggrMethod);

                final double elementAbsVal = Math.abs(aggrVal);
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
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(
                                    "++++++++Height diff. for bar " + barAggrVal
                                    + " in last element: " + diff
                                    + ". Bar height: " + barHeight
                                    + " Height sum without adjustment: "
                                    + elementHeightSum
                                    + " No of elements: " + getNoOfElements());
                        }
                        if (elementHeight < 1) {
                            LOGGER.info(
                                    "******Unable to correct height diff. for "
                                    + "bar " + barAggrVal
                                    + ". Last element to low for height "
                                    + "adjustment.");
                        }
                    }
                }
//                if (LOGGER.isDebugEnabled()) {
//                    LOGGER.debug("Element aggrVal: " + aggrVal
//                            + " element absVal: " + elementAbsVal
//                            + " xCoord: " + startX
//                            + " yCoord: " + yCoord
//                            + " elementWidth: " + barWidth
//                            + " rawElementHeight: " + rawElementHeight
//                            + " adjusted elementHeight: " + elementHeight);
//                }

                final Rectangle elementRect =
                    new Rectangle(startX, yCoord, barWidth, elementHeight);
                element.setRectangle(elementRect, calculator);
                //add the bar height to the current y coordinate to draw
                //the next element below the current one
                yCoord += elementHeight;
            }
        }

    /**
     * @param startX the x coordinate
     * @param newWidth the new bar width
     * @param barElementColors all element colors which define the order
     * the elements should be drawn
     * @param baseLine the base line
     * @param calculator the hilite shape calculator
     */
    public void updateBarWidth(final int startX, final int newWidth,
            final SortedSet<Color> barElementColors, final int baseLine,
            final HistogramHiliteCalculator calculator) {
        final Rectangle2D barRectangle = getShape();
        if (barRectangle == null) {
            return;
        }
        final HistogramLayout layout = calculator.getLayout();
        final boolean drawElementsBefore = isPresentable();
        final int yCoord = (int)barRectangle.getY();
        final int barHeight = (int)barRectangle.getHeight();
        final Rectangle rect = new Rectangle(startX, yCoord,
                newWidth, barHeight);
        setRectangle(rect, baseLine, calculator);
        final int barWidth = (int)barRectangle.getWidth();
        final int barX = (int)barRectangle.getX();
        final int noOfElements = getNoOfElements();
        setPresentable(elementsFitInBar(layout, barElementColors,
                noOfElements, barWidth, barHeight), calculator);
        if (!isPresentable()) {
            //if the elements doesn't fit any way return here
            return;
        }
        if (!drawElementsBefore) {
            //if the elements couldn't be draw before but now recalculate
            //them
            setElementRectangle(baseLine, barElementColors, calculator);
            return;
        }

        if (HistogramLayout.STACKED.equals(layout)) {
            for (final Color elementColor : barElementColors) {
                final BarElementDataModel element =
                    getElement(elementColor);
                if (element != null) {
                    element.updateElementWidth(barX,
                            barWidth, calculator);
                }
            }
        } else if (HistogramLayout.SIDE_BY_SIDE.equals(layout)) {
            int xCoord = barX;
            final int elementWidth =
                calculateSideBySideElementWidth(barElementColors, barWidth);
            for (final Color elementColor : barElementColors) {
                final BarElementDataModel element =
                    getElement(elementColor);
                if (element != null) {
                    element.updateElementWidth(xCoord,
                            elementWidth, calculator);
                }
//              add the bar width and the space between bars to the current
                //x coordinate
                xCoord += elementWidth
                    + AbstractHistogramVizModel.SPACE_BETWEEN_ELEMENTS;
            }
        } else {
            throw new IllegalArgumentException(
                    "Layout " + layout + " not supported");
        }
        return;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<BarElementDataModel> getSelectedElements() {
        final Collection<BarElementDataModel> elements = getElements();
        final List<BarElementDataModel> selectedElements =
            new ArrayList<BarElementDataModel>(elements.size());
        for (final BarElementDataModel element : elements) {
            if (element.isSelected()) {
                selectedElements.add(element);
            }
        }
        return selectedElements;
    }

    private static int calculateSideBySideElementWidth(
            final SortedSet<Color> barElementColors, final int barWidth) {
        final int noOfColors = barElementColors.size();
        return Math.max((barWidth
                - (AbstractHistogramVizModel.SPACE_BETWEEN_ELEMENTS
                        * (noOfColors - 1))) / noOfColors,
                        AbstractHistogramVizModel.MINIMUM_ELEMENT_WIDTH);
    }

    /**
     * Calculates the rectangle which fits in the given surrounding
     * rectangle.
     * @param baseLine the y coordinate of the base line
     * @param surroundingHeight the height of the surrounding rectangle
     * @param surroundingY the y coordinate of the surrounding rectangle
     * @param heightPerVal the height per aggregation value
     * @param aggrVal the aggregation value of this bar
     * @param xCoord the x coordinate of this bar
     * @param barWidth the width of the bar
     * @return the calculated bar
     */
    private static Rectangle calculateBarRectangle(final int baseLine,
            final int surroundingHeight, final int surroundingY,
            final double heightPerVal, final double aggrVal,
            final int xCoord, final int barWidth) {
        int barHeight = Math.max((int)(
                heightPerVal * Math.abs(aggrVal)),
                AbstractHistogramVizModel.MINIMUM_BAR_HEIGHT);
        final int totalHeight;
        if (aggrVal < 0) {
            totalHeight = surroundingHeight + surroundingY - baseLine;
        } else {
            totalHeight = baseLine - surroundingY;
        }
        if (barHeight > totalHeight) {
            final int diff = barHeight - totalHeight;
            barHeight -= diff;
            LOGGER.debug("Height diff. bar higher than bin: "
                    + diff);
        }
//                  calculate the position of the y coordinate
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
        final Rectangle barRect =
            new Rectangle(xCoord, yCoord, barWidth, barHeight);
        return barRect;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BarDataModel clone() {
        final Collection<BarElementDataModel> elements = getElements();
        final Map<Color, BarElementDataModel> elementClones =
            new HashMap<Color, BarElementDataModel>(
                    elements.size());
        for (final BarElementDataModel element : elements) {
            final BarElementDataModel elementClone = element.clone();
            elementClones.put(elementClone.getColor(), elementClone);
        }
        final BarDataModel clone = new BarDataModel(getName(), getColor(),
                elementClones, getRowCount(), getValueCount(),
                getAggregationSum(), supportsHiliting());
        return clone;
    }

    /**
     * @param config the config object to use
     * @param exec the {@link ExecutionMonitor} to provide progress messages
     * @return the {@link BarDataModel}
     * @throws CanceledExecutionException if the operation is canceled
     * @throws InvalidSettingsException if the config object is invalid
     */
    public static BarDataModel loadFromFile(final Config config,
            final ExecutionMonitor exec) throws CanceledExecutionException,
            InvalidSettingsException {
        return new BarDataModel(config, exec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveElements(final Collection<BarElementDataModel> elements,
            final ConfigWO config, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        config.addInt(CFG_ELEMENT_COUNT, elements.size());
        int idx = 0;
        for (final BarElementDataModel element : elements) {
            final ConfigWO elementConfig =
                config.addConfig(CFG_BAR_ELEMENT + idx++);
            element.save2File(elementConfig, exec);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Collection<BarElementDataModel> loadElements(
            final ConfigRO config,
            final ExecutionMonitor exec) throws CanceledExecutionException,
            InvalidSettingsException {
        final int counter = config.getInt(CFG_ELEMENT_COUNT);
        final Collection<BarElementDataModel> elements =
            new ArrayList<BarElementDataModel>(counter);
        for (int i = 0; i < counter; i++) {
            final Config binConf = config.getConfig(CFG_BAR_ELEMENT + i);
                elements.add(BarElementDataModel.loadFromFile(binConf, exec));
        }
        exec.checkCanceled();
        return elements;
    }
}
