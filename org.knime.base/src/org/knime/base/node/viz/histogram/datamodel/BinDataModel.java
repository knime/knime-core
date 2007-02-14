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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.NodeLogger;

/**
 * This class holds the information of a histogram bin. A bin consists of at 
 * least one {@link BarDataModel} object which consists of one or more
 * {@link BarElementDataModel} objects.
 * @author Tobias Koetter, University of Konstanz
 */
public class BinDataModel {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(BinDataModel.class);
    /**
     * The space between to bars in pixel.
     */
    private static final int SPACE_BETWEEN_BARS = 1;

    private final String m_xAxisCaption;
    
    private final double m_lowerBound;
    
    private final double m_upperBound;
    
    private final Map<Color, BarDataModel> m_bars = 
        new HashMap<Color, BarDataModel>();
    
    /**The color of the missing value bar doesn't matter we have to paint it
     * anyway different to ensure the uniqueness.*/
    private BarDataModel m_missingValueBar = 
        new BarDataModel(Color.LIGHT_GRAY);
    
    private int m_rowCounter = 0;
    
    private Rectangle m_binRectangle;
    
    /**Constructor for class BinDataModel.
     * @param xAxisCaption the caption of this bin on the x axis
     * @param lowerBound the lower bound of the bin interval
     * @param upperBound the higher bound of the bin interval
     */
    protected BinDataModel(final String xAxisCaption, final double lowerBound,
            final double upperBound) {
        m_xAxisCaption = xAxisCaption;
        m_lowerBound = lowerBound;
        m_upperBound = upperBound;
    }

    /**
     * @param row the {@link HistogramDataRow} to add
     * @param columns the {@link ColorColumn} objects in the same order
     * like the aggregation values
     */
    protected void addDataRow(final HistogramDataRow row, 
            final List<ColorColumn> columns) {
        final DataCell[] aggrVals = row.getAggrVals();
        final DataCell id = row.getRowKey().getId();
        final Color rowColor = row.getColor();
        for (int i = 0, length = aggrVals.length; i < length; i++) {
            final DataCell cell = aggrVals[i];
            final Color barColor = columns.get(i).getColor();
            BarDataModel bar = m_bars.get(barColor);
            if (bar == null) {
                bar = new BarDataModel(barColor);
                m_bars.put(barColor, bar);
            }
            if (cell.isMissing()) {
              m_missingValueBar.addDataRow(m_missingValueBar.getColor(), id, 
                      0);
              continue;
            } 
            if (!cell.getType().isCompatible(DoubleValue.class)) {
                throw new IllegalArgumentException(
                        "Aggregation values should be of numeric type");
            }
            bar.addDataRow(rowColor, id, ((DoubleValue)cell).getDoubleValue());
        }
        m_rowCounter++;
    }
    
    /**
     * @return the xAxisCaption
     */
    public String getXAxisCaption() {
        return m_xAxisCaption;
    }

    /**
     * @return the missingValueBar
     */
    public BarDataModel getMissingValueBar() {
        if (m_missingValueBar.getRowCount() == 0) {
            return null;
        }
        return m_missingValueBar;
    }
    
    /**
     * @param color the color of the bar of interest
     * @return the bar with the given color or <code>null</code> if no bar
     * exists for the given color
     */
    public BarDataModel getBar(final Color color) {
        return m_bars.get(color);
    }
    
    /**
     * @return all {@link BarDataModel} objects of this bin
     */
    public Collection<BarDataModel> getBars() {
        return m_bars.values();
    }
    
    /**
     * @param showMissingValbar <code>true</code> if the missing value bar
     * should be displayed as well 
     * @return the number of bars in this bin
     */
    public int getNoOfBars(final boolean showMissingValbar) {
        int noOfBars = m_bars.size();
        if (showMissingValbar && m_missingValueBar != null
                && m_missingValueBar.getRowCount() > 0) {
            noOfBars++;
        }
        return noOfBars;
    }

    /**
     * @param method the {@link AggregationMethod} to use
     * @param layout the histogram layout
     * @return the maximum aggregation value
     */
    public double getMaxAggregationValue(final AggregationMethod method,
            final HistogramLayout layout) {
        final Collection<BarDataModel> bars = m_bars.values();
        double maxAggrValue = -Double.MAX_VALUE;
        for (BarDataModel bar : bars) {
            final double value = bar.getMaxAggregationValue(method, layout);
            if (value > maxAggrValue) {
                maxAggrValue = value;
            }
        }
        return maxAggrValue;
    }
    
    /**
     * @return the number of rows in this bin
     */
    public int getBinRowCount() {
        return m_rowCounter;
    }
    
    /**
     * @return the maximum row count of all bars in this bin
     */
    public int getMaxBarRowCount() {
        final Collection<BarDataModel> bars = m_bars.values();
        int maxRowCount = 0;
        for (BarDataModel model : bars) {
            final int rowCount = model.getRowCount();
            if (rowCount > maxRowCount) {
                maxRowCount = rowCount;
            }
        }
        return maxRowCount;
    }
    

    /**
     * @param method the {@link AggregationMethod} to use
     * @param layout the histogram layout
     * @return the minimum aggregation value
     */
    public double getMinAggregationValue(final AggregationMethod method,
            final HistogramLayout layout) {
        final Collection<BarDataModel> bars = m_bars.values();
        double minAggrValue = Double.MAX_VALUE;
        for (BarDataModel bar : bars) {
            final double value = bar.getMinAggregationValue(method, layout);
            if (value < minAggrValue) {
                minAggrValue = value;
            }
        }
        return minAggrValue;
    }
    

    /**
     * @return the minimum row count of all bars in this bin
     */
    public int getMinRowCount() {
        final Collection<BarDataModel> bars = m_bars.values();
        int minRowCount = 0;
        for (BarDataModel model : bars) {
            final int rowCount = model.getRowCount();
            if (rowCount < minRowCount) {
                minRowCount = rowCount;
            }
        }
        return minRowCount;
    }
    
    /**
     * @return the lower bound of this bin
     */
    public double getLowerBound() {
        return m_lowerBound;
    }
    
    /**
     * @return the higher bound of this bin
     */
    public double getUpperBound() {
        return m_upperBound;
    }
    
//    /**
//     * @param value the value to check
//     * @return <code>true</code> if the given value is between or equal
//     *  the lower and higher bound of this bin
//     */
//    public boolean isInBoundaries(final double value) {
//        return (value > m_lowerBound && value <= m_higherBound);
//    }

    /**
     * @return <code>true</code> if at least one bar is selected
     */
    public boolean isSelected() {
        final Collection<BarDataModel> bars = m_bars.values();
        for (BarDataModel bar : bars) {
            if (bar.isSelected()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * @return the {@link Rectangle} the bin should be drawn on the 
     * screen 
     */
    public Rectangle getBinRectangle() {
        return m_binRectangle;
    }

    /**
     * THE HIGHT OF THE RECTANGLE SHOULD BE CALCULATED USING THE MIN AND
     * MAX AGGREGATION VALUE TO HANDLES BINS WITH POSITIVE AND NEGATIVE BARS!!!
     * @param binRectangle the {@link Rectangle} the bin should be drawn on the 
     * screen 
     * @param showMissingValBar <code>true</code> if the missing value bar
     * should be displayed
     * @param aggrMethod the aggregation method which should be used
     * @param layout the histogram layout
     * @param baseLine the x coordinate of the base line (0) on the screen
     * @param barElementColors all element colors which define the order
     * the elements should be drawn
     * @param aggrColumns the aggregation column array which indicates
     * the order of the bars
     */
    public void setBinRectangle(final Rectangle binRectangle, 
            final boolean showMissingValBar, 
            final AggregationMethod aggrMethod, final HistogramLayout layout,
            final int baseLine, final SortedSet<Color> barElementColors, 
            final List<ColorColumn> aggrColumns) {
        m_binRectangle = binRectangle;
        setBarRectangle(showMissingValBar, aggrMethod, layout, baseLine,
                barElementColors, aggrColumns);
    }
    
    /**
     * Sets the rectangle for all bars in this bin.
     * @param showMissingValBar <code>true</code> if the missing value bar
     * should be displayed
     * @param aggrMethod the aggregation method which should be used
     * @param layout the histogram layout
     * @param baseLine the x coordinate of the base line (0) on the screen
     * @param barElementColors all element colors which define the order
     * the elements should be drawn
     * @param aggrColumns the aggregation column array which indicates
     * the order of the bars
     */
    private void setBarRectangle(final boolean showMissingValBar,
            final AggregationMethod aggrMethod, final HistogramLayout layout,
            final int baseLine, final SortedSet<Color> barElementColors, 
            final List<ColorColumn> aggrColumns) {
        final double totalWidth = m_binRectangle.getWidth();
        int noOfBars = aggrColumns.size();
        if (showMissingValBar && m_missingValueBar != null
                && m_missingValueBar.getRowCount() > 0) {
            noOfBars++;
        }
        final int barWidth = 
            Math.max((int)totalWidth - (SPACE_BETWEEN_BARS * noOfBars), 1);
        if (noOfBars * barWidth > totalWidth) {
            //the total bin width is not enough to draw all bars so we don't
            //need to calculate any further and reset all previous 
            //bar rectangles
            final Collection<BarDataModel> bars = m_bars.values();
            for (BarDataModel bar : bars) {
                bar.setBarRectangle(null, aggrMethod, layout, baseLine, 
                        barElementColors);
            }
            return;
        }
        //calculate the height
        final int totalHeight = (int)m_binRectangle.getHeight();
        final double maxBinAggrVal = getMaxAggregationValue(aggrMethod, layout);
        final double minBinAggrVal = getMinAggregationValue(aggrMethod, layout);
        double valRange = 
            Math.max(Math.abs(maxBinAggrVal), Math.abs(minBinAggrVal));
        if (minBinAggrVal < 0 && maxBinAggrVal > 0) {
            valRange = maxBinAggrVal + Math.abs(minBinAggrVal);
        }
        
        if (valRange <= 0) {
            return;
        }
        final double heightPerVal = totalHeight / valRange;
        final int startX = (int) m_binRectangle.getX();
        final int startY = (int)m_binRectangle.getY();
        int xCoord = startX + SPACE_BETWEEN_BARS;
        for (ColorColumn aggrColumn : aggrColumns) {
            final BarDataModel bar = m_bars.get(aggrColumn.getColor());
            if (bar != null) {
                final double maxBarAggrVal = 
                    bar.getMaxAggregationValue(aggrMethod, layout);
                final double minBarAggrVal = 
                    bar.getMinAggregationValue(aggrMethod, layout);
                double barVal = 
                    Math.max(Math.abs(maxBarAggrVal), Math.abs(minBarAggrVal));
                if (minBinAggrVal < 0 && maxBarAggrVal >= 0) {
                    //add the other aggregation value to the real value
                    //if the min value is negative and the max is positive
                    if (Math.abs(minBarAggrVal) > maxBarAggrVal) {
                        barVal += maxBinAggrVal;
                    } else {
                        barVal += Math.abs(minBinAggrVal);
                    }
                }
                int barMaxHeight = 
                    Math.max((int)(heightPerVal * barVal), 1);
                final int yCoord = startY + (totalHeight - barMaxHeight);
                if (barMaxHeight > totalHeight) {
                    final int diff = barMaxHeight - totalHeight;
                    barMaxHeight -= diff;
                    LOGGER.debug("Height diff. bar higher than bin: " 
                            + diff);
                }
                final Rectangle barRect = 
                    new Rectangle(xCoord, yCoord, barWidth, barMaxHeight);
                bar.setBarRectangle(barRect, aggrMethod, layout, baseLine,
                        barElementColors);
            }
            //add the bar width and the space between bars to the current
            //x coordinate
            xCoord += barWidth + SPACE_BETWEEN_BARS;
        }
    }
//    
//    /**
//     * @see java.lang.Object#clone()
//     */
//    @Override
//    public BinDataModel clone() {
//        final BinDataModel clone = new BinDataModel(m_xAxisCaption, 
//                m_lowerBound, m_upperBound);
//        clone.m_binRectangle = m_binRectangle;
//        clone.m_missingValueBar = m_missingValueBar;
//        clone.m_rowCounter = m_rowCounter;
//        final Collection<BarDataModel> bars = m_bars.values();
//        for (BarDataModel bar : bars) {
//            clone.m_bars.put(bar.getColor(), bar.clone());            
//        }
//        
//        return clone;
//    }
}
