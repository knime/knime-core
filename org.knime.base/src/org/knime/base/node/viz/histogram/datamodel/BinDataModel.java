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
import java.util.Set;
import java.util.SortedSet;

import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.core.data.DataCell;
import org.knime.core.data.def.StringCell;
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
    
    private final DataCell m_xAxisCaptionCell;
    
    private final Double m_lowerBound;
    
    private final Double m_upperBound;
    
    private final Map<Color, BarDataModel> m_bars = 
        new HashMap<Color, BarDataModel>();
    
    private boolean m_drawBar = true;
    
    private int m_rowCounter = 0;
    
    private boolean m_isSelected = false;
    
    private Rectangle m_binRectangle;
    
    /**Constructor for class BinDataModel.
     * @param xAxisCaption the caption of this bin on the x axis
     * @param lowerBound the lower bound of the bin interval
     * @param upperBound the higher bound of the bin interval
     */
    protected BinDataModel(final String xAxisCaption, final double lowerBound,
            final double upperBound) {
        if (xAxisCaption == null) {
            throw new IllegalArgumentException("Caption shouldn't be null");
        }
        m_xAxisCaption = xAxisCaption;
        m_xAxisCaptionCell = new StringCell(xAxisCaption);
        m_lowerBound = lowerBound;
        m_upperBound = upperBound;
    }

    /**
     * @param id the row id
     * @param rowColor the row color
     * @param columns the {@link ColorColumn} objects in the same order
     * like the aggregation values
     * @param aggrVals the aggregation value in the same order like the
     * columns
     */
    protected void addDataRow(final DataCell id, final Color rowColor, 
            final Collection<ColorColumn> columns, final DataCell... aggrVals) {
//        final DataCell[] aggrVals = row.getAggrVals();
//        final DataCell id = row.getRowKey().getId();
//        final Color rowColor = row.getColor();
        if (columns.size() != aggrVals.length) {
            throw new IllegalArgumentException(
                    "Columns and value should be of equal size");
        }
        int i = 0;
        for (ColorColumn column : columns) {
            final DataCell cell = aggrVals[i++];
            final Color barColor = column.getColor();
            BarDataModel bar = m_bars.get(barColor);
            if (bar == null) {
                bar = new BarDataModel(barColor);
                m_bars.put(barColor, bar);
            }
            bar.addDataRow(rowColor, id, cell);   
        }
        m_rowCounter++;
    }
    
    /**
     * @return the x axis caption
     */
    public String getXAxisCaption() {
        return m_xAxisCaption;
    }
    
    /**
     * @return the x axis caption as {@link DataCell}
     */
    public DataCell getXAxisCaptionCell() {
        return m_xAxisCaptionCell;
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
     * @return the number of bars in this bin
     */
    public int getNoOfBars() {
        return m_bars.size();
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
        for (BarDataModel bar : bars) {
            final int rowCount = bar.getRowCount();
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
     * @return the lower bound of this bin could be <code>null</code>
     */
    public Double getLowerBound() {
        return m_lowerBound;
    }
    
    /**
     * @return the higher bound of this bin could be <code>null</code>
     */
    public Double getUpperBound() {
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
     * @return <code>true</code> if the bars should be drawn
     */
    public boolean isDrawBar() {
        return m_drawBar;
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
     * @param aggrMethod the aggregation method which should be used
     * @param layout the histogram layout
     * @param baseLine the x coordinate of the base line (0) on the screen
     * @param barElementColors all element colors which define the order
     * the elements should be drawn
     * @param aggrColumns the aggregation column array which indicates
     * the order of the bars
     */
    public void setBinRectangle(final Rectangle binRectangle,
            final AggregationMethod aggrMethod, final HistogramLayout layout,
            final int baseLine, final SortedSet<Color> barElementColors, 
            final Collection<ColorColumn> aggrColumns) {
        m_binRectangle = binRectangle;
        setBarRectangle(aggrMethod, layout, baseLine, barElementColors, 
                    aggrColumns);
    }
    
    /**
     * Sets the rectangle for all bars in this bin.
     * @param aggrMethod the aggregation method which should be used
     * @param layout the histogram layout
     * @param baseLine the x coordinate of the base line (0) on the screen
     * @param barElementColors all element colors which define the order
     * the elements should be drawn
     * @param aggrColumns the aggregation column array which indicates
     * the order of the bars
     */
    private void setBarRectangle(final AggregationMethod aggrMethod, 
            final HistogramLayout layout, final int baseLine, 
            final SortedSet<Color> barElementColors, 
            final Collection<ColorColumn> aggrColumns) {
        if (m_binRectangle == null) {
            final Collection<BarDataModel> bars = m_bars.values();
            //also reset the bar rectangle
            for (BarDataModel bar : bars) {
                bar.setBarRectangle(null, aggrMethod, layout, baseLine, 
                        barElementColors);
            }
            return;
        }
        final double totalWidth = m_binRectangle.getWidth();
        final int noOfBars = aggrColumns.size();
        final int barWidth = 
            Math.max((int)totalWidth - (SPACE_BETWEEN_BARS * noOfBars), 1);
        if (noOfBars * barWidth > totalWidth) {
            //the total bin width is not enough to draw all bars so we don't
            //need to calculate any further and reset all previous 
            //bar rectangles
//            final Collection<BarDataModel> bars = m_bars.values();
//            for (BarDataModel bar : bars) {
//                bar.setBarRectangle(null, aggrMethod, layout, baseLine, 
//                        barElementColors);
//            }
            m_drawBar = false;
            return;
        }
        m_drawBar = true;
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
            m_drawBar = false;
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

    /**
     * @param startX new x coordinate
     * @param binWidth new bin width
     * @param layout the {@link HistogramLayout} to use
     * @param barElementColors all element colors which define the order
     * the elements should be drawn
     * @param aggrMethod the {@link AggregationMethod} to use
     * @param aggrColumns the current aggregation columns
     * @param baseLine the base line
     */
    public void updateBinWidth(final int startX, final int binWidth,
            final HistogramLayout layout, 
            final SortedSet<Color> barElementColors, 
            final AggregationMethod aggrMethod, 
            final Collection<ColorColumn> aggrColumns,
            final int baseLine) {
        if (m_binRectangle == null) {
            return;
        }
        final boolean drawBarBefore = m_drawBar;
        final int yCoord = (int)m_binRectangle.getY();
        final int binHeight = (int) m_binRectangle.getHeight();
        m_binRectangle.setBounds(startX, yCoord, binWidth, binHeight);
        
        final int noOfBars = m_bars.size();
        final int barWidth = 
            Math.max(binWidth - (SPACE_BETWEEN_BARS * noOfBars), 1);
        final Collection<BarDataModel> bars = m_bars.values();
        if (noOfBars * barWidth > binWidth) {
            //the total bin width is not enough to draw all bars so we don't
            //need to calculate any further and reset all previous 
            //bar rectangles
//            for (BarDataModel bar : bars) {
//                bar.setBarRectangle(null, aggrMethod, layout, -1, null);
//            }
            m_drawBar = false;
            return;
        }
        m_drawBar = true;
        if (!drawBarBefore) {
            //if the bar couldn't be draw before but now we have to 
            //recalculate them
            setBarRectangle(aggrMethod, layout, baseLine, barElementColors, 
                    aggrColumns);
            return;
        }

        int xCoord = startX + SPACE_BETWEEN_BARS;
        for (BarDataModel bar : bars) {
            bar.updateBarWidth(xCoord, barWidth, layout, 
                    barElementColors, aggrMethod, baseLine);
            xCoord += barWidth + SPACE_BETWEEN_BARS;
        }
    }

    /**
     * Clears all bars and rows from this bin.
     */
    public void clear() {
        m_bars.clear();
        m_rowCounter = 0;
        m_binRectangle = null;
        m_drawBar = false;
    }

    /**
     * @return <code>true</code> if at least one bar is selected
     */
    public boolean isSelected() {
        return m_isSelected;
    }
    
    /**
     * @param selected <code>true</code> to select the given bar otherwise
     * <code>false</code>
     * @return <code>true</code> if the parameter has changed
     */
    public boolean setSelected(final boolean selected) {
        if (m_isSelected == selected) {
            return false;
        }
        m_isSelected = selected;
        for (final BarDataModel bar : getBars()) {
            bar.setSelected(m_isSelected);
        }
        return true;
    }

    /**
     * Selects all elements which contain the given point. 
     * @param point the {@link Point} to check
     */
    public void selectElement(final Point point) {
        if (m_binRectangle != null && m_binRectangle.contains(point)) {
            for (final BarDataModel bar : getBars()) {
               m_isSelected = bar.selectElement(point) || m_isSelected;
            }
        }
    }
    
    /**
     * Selects all elements which intersect with the given rectangle. 
     * @param rect the {@link Rectangle} to check
     */
    public void selectElement(final Rectangle rect) {
        if (m_binRectangle != null && m_binRectangle.intersects(rect)) {
            for (final BarDataModel bar : getBars()) {
               m_isSelected = bar.selectElement(rect) || m_isSelected;
            }
        }
    }
    
    /**
     * @param hilited the row keys to hilite
     * @param aggrMethod the current aggregation method
     * @param layout the current {@link HistogramLayout}
     */
    protected void setHilitedKeys(final Set<DataCell> hilited, 
            final AggregationMethod aggrMethod,
            final HistogramLayout layout) {
        for (final BarDataModel bar : getBars()) {
            bar.setHilitedKeys(hilited, aggrMethod, layout);
        }
    }

    /**
     * @param hilited the row keys to unhilite
     * @param aggrMethod the current aggregation method
     * @param layout the current {@link HistogramLayout}
     */
    protected void removeHilitedKeys(final Set<DataCell> hilited, 
            final AggregationMethod aggrMethod, 
            final HistogramLayout layout) {
        for (final BarDataModel bar : getBars()) {
            bar.removeHilitedKeys(hilited, aggrMethod, layout);
        }
    }

    /**
     * Clears the hilite information.
     */
    public void clearHilite() {
        for (final BarDataModel bar : getBars()) {
            bar.clearHilite();
        }
    }
}
