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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.base.node.viz.histogram.util.ColorColumn;
import org.knime.core.data.DataCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;

/**
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class BinDataModel implements Serializable {

    private static final long serialVersionUID = -5898246116408854042L;

    
    /**The color of the bar if no aggregation column is selected.*/
    private static final Color NO_AGGR_COL_COLOR = Color.GRAY;

    private final String m_xAxisCaption;
    private final DataCell m_xAxisCaptionCell;
    private final Double m_lowerBound;
    private final Double m_upperBound;
    private final Map<Color, BarDataModel> m_bars = 
        new HashMap<Color, BarDataModel>();
    private int m_rowCounter = 0;

    //visual variables
    private boolean m_drawBar = true;
    private boolean m_isSelected = false;
    
    /**The surrounding rectangle is used to distinguish between the 
        different bins.*/
    private Rectangle m_surroundingRectangle;
    /**The bin rectangle is the main rectangle which contains the bars.*/
    private Rectangle m_binRectangle;

    /**Constructor for class BinDataModel.
     * @param xAxisCaption the caption of this bin on the x axis
     * @param lowerBound the lower bound of the bin interval
     * @param upperBound the higher bound of the bin interval
     */
    public BinDataModel(final String xAxisCaption, final double lowerBound, 
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
     * @param aggrCols the {@link ColorColumn} objects in the same order
     * like the aggregation values
     * @param aggrVals the aggregation value in the same order like the
     * columns
     */
    public void addDataRow(final DataCell id, final Color rowColor, 
            final Collection<ColorColumn> aggrCols, 
            final DataCell... aggrVals) {
    //        final DataCell[] aggrVals = row.getAggrVals();
    //        final DataCell id = row.getRowKey().getId();
    //        final Color rowColor = row.getColor();
    //        if (aggrCols.size() != aggrVals.length) {
    //            throw new IllegalArgumentException(
    //                    "Columns and value should be of equal size");
    //        }
            if (aggrCols == null || aggrCols.size() < 1) {
                //no aggregation column selected create a dummy bar to show
                //at least the count aggregation method
                BarDataModel bar = m_bars.get(NO_AGGR_COL_COLOR);
                if (bar == null) {
                    bar = createBar(NO_AGGR_COL_COLOR);
                    m_bars.put(NO_AGGR_COL_COLOR, bar);
                }
                bar.addDataRow(rowColor, id, new DoubleCell(0));
            } else {
                int i = 0;
                for (ColorColumn column : aggrCols) {
                    final DataCell cell = aggrVals[i++];
                    final Color barColor = column.getColor();
                    BarDataModel bar = m_bars.get(barColor);
                    if (bar == null) {
                        bar = createBar(barColor);
                        m_bars.put(barColor, bar);
                    }
                    bar.addDataRow(rowColor, id, cell);   
                }
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
     * Creates a new {@link BarDataModel} with the given color.
     * @param color the {@link Color} of the bar
     * @return the created bar
     */
    protected BarDataModel createBar(final Color color) {
        return new BarDataModel(color);
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
     * @return the surroundingRectangle to draw to distinguish between the
     * different bins
     */
    public Rectangle getSurroundingRectangle() {
        return m_surroundingRectangle;
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
        m_surroundingRectangle = 
            AbstractHistogramVizModel.calculateSurroundingRectangle(
                    m_binRectangle, baseLine, 
                    AbstractHistogramVizModel.BIN_SURROUNDING_SPACE);
        if (aggrColumns == null || aggrColumns.size() < 1) {
            //no aggregation column selected so we have only one bar the 
            //face bar -> simply set the bin rectangle as bar rectangle
            final BarDataModel bar = m_bars.get(NO_AGGR_COL_COLOR);
            if (bar != null) {
                //no data row was added to this bin so we don't  have a bar
                bar.setBarRectangle(binRectangle, aggrMethod, layout, baseLine, 
                        barElementColors);
            }
        } else {
            setBarRectangle(aggrMethod, layout, baseLine, barElementColors, 
                    aggrColumns);
        }
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
            final Collection<? extends ColorColumn> aggrColumns) {
            if (m_binRectangle == null) {
                final Collection<BarDataModel> bars = m_bars.values();
                //also reset the bar rectangle
                for (BarDataModel bar : bars) {
                    bar.setBarRectangle(null, aggrMethod, layout, baseLine, 
                            barElementColors);
                }
                return;
            }
            final int binWidth = (int)m_binRectangle.getWidth();
            final int noOfBars = aggrColumns.size();
            m_drawBar = elementsFitInBin(noOfBars, binWidth);
            if (!m_drawBar) {
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
            final int binHeight = (int)m_binRectangle.getHeight();
            final double maxAggrVal = Math.max(getMaxAggregationValue(
                    aggrMethod, layout), 0);
            final double minAggrVal = Math.min(getMinAggregationValue(
                    aggrMethod, layout), 0);
            final double valRange = maxAggrVal + Math.abs(minAggrVal);
            if (valRange <= 0) {
                m_drawBar = false;
                return;
            }

            final int barWidth = calculateBarWidth(binWidth, noOfBars);
            final double heightPerVal = binHeight / valRange;
            final int binX = (int) m_binRectangle.getX();
            int xCoord = binX;
            for (ColorColumn aggrColumn : aggrColumns) {
                final BarDataModel bar = 
                    m_bars.get(aggrColumn.getColor());
                if (bar != null) {
                    //set the rectangle only for the bars which are available
                    //in this bin
                    final double barMaxAggrVal = Math.max(
                            bar.getMaxAggregationValue(aggrMethod, layout), 0);
                    final double barMinAggrVal = Math.min(
                        bar.getMinAggregationValue(aggrMethod, layout), 0);
                    final double aggrVal = barMaxAggrVal 
                    + Math.abs(barMinAggrVal);
                    final int yCoord = (int)(baseLine 
                        - (barMaxAggrVal * heightPerVal));
                    final int barHeight = (int)(aggrVal * heightPerVal);
                    final Rectangle barRect = new Rectangle(xCoord, yCoord, 
                            barWidth, barHeight);
                    bar.setBarRectangle(barRect, aggrMethod, layout, baseLine,
                            barElementColors);
                }
                //add the bar width and the space between bars to the current
                //x coordinate
                xCoord += barWidth 
                + AbstractHistogramVizModel.SPACE_BETWEEN_BARS;
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
            m_surroundingRectangle = 
                AbstractHistogramVizModel.calculateSurroundingRectangle(
                        m_binRectangle, baseLine, 
                        AbstractHistogramVizModel.BIN_SURROUNDING_SPACE);
            final int noOfBars = m_bars.size();
            if (noOfBars < 1) {
                return;
            }
            m_drawBar = elementsFitInBin(noOfBars, binWidth);
            if (!m_drawBar) {
                //the total bin width is not enough to draw all bars so we don't
                //need to calculate any further and reset all previous 
                //bar rectangles
    //            for (BarDataModel bar : bars) {
    //                bar.setBarRectangle(null, aggrMethod, layout, -1, null);
    //            }
                return;
            }
            m_drawBar = true;
            if (!drawBarBefore) {
                //if the bar couldn't be draw before but now we have to 
                //recalculate them
                setBinRectangle(m_binRectangle, aggrMethod, layout, baseLine, 
                        barElementColors, aggrColumns);
                return;
            }
            final int barWidth = calculateBarWidth(binWidth, noOfBars);
            int xCoord = startX;
            if (aggrColumns == null || aggrColumns.size() < 1) {
                //the user hasn't selected a aggregation column so we use the
                //dummy bar
                final BarDataModel bar = m_bars.get(NO_AGGR_COL_COLOR);
                bar.updateBarWidth(xCoord, barWidth, layout, 
                        barElementColors, aggrMethod, baseLine);
            } else {
                for (ColorColumn aggrCol : aggrColumns) {
                    final BarDataModel bar = m_bars.get(aggrCol.getColor());
                    bar.updateBarWidth(xCoord, barWidth, layout, 
                            barElementColors, aggrMethod, baseLine);
                    xCoord += barWidth 
                        + AbstractHistogramVizModel.SPACE_BETWEEN_BARS;
                }
            }
        }

    /**
     * @param binWidth the total width of this bin
     * @param noOfBars the number of elements to fit in
     * @return the bar width
     */
    private static int calculateBarWidth(final int binWidth, 
            final int noOfBars) {
        return Math.max((binWidth 
                - (AbstractHistogramVizModel.SPACE_BETWEEN_BARS 
                        * (noOfBars - 1))) / noOfBars, 
                        AbstractHistogramVizModel.MINIMUM_ELEMENT_WIDTH);
    }
    /**
     * Checks if all bars fit in the surrounding bin.
     * @param layout the {@link HistogramLayout}
     * @param noOfBars the number of bars which should fit
     * @param binWidth the width of the bin
     * @return <code>true</code> if the given number of bars fit into
     * the given bin ranges
     */
    private static boolean elementsFitInBin(final int noOfBars, 
            final int binWidth) {
        if (noOfBars > 1) {
            final int barWidth = 
                calculateBarWidth(binWidth, noOfBars);
            if (noOfBars * barWidth 
                    > binWidth 
                    - (AbstractHistogramVizModel.SPACE_BETWEEN_BARS 
                            * (noOfBars - 1))) {
                return false;
            }
        }
        return true;
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
     * @return <code>true</code> if at least one bar of the bin contains
     * the point
     */
    public boolean selectElement(final Point point) {
        if (m_binRectangle != null && m_binRectangle.contains(point)) {
            if (!m_drawBar) {
                for (final BarDataModel bar : getBars()) {
                    bar.setSelected(true);
                }
                m_isSelected = true;
            } else {
                for (final BarDataModel bar : getBars()) {
                   m_isSelected = bar.selectElement(point) || m_isSelected;
                }
            }
        }
        return m_isSelected;
    }

    /**
     * Selects all elements which intersect with the given rectangle. 
     * @param rect the {@link Rectangle} to check
     * @return <code>true</code> if at least one bar of the bin contains
     * the rectangle
     */
    public boolean selectElement(final Rectangle rect) {
        if (m_binRectangle != null && m_binRectangle.intersects(rect)) {
            if (!m_drawBar) {
                for (final BarDataModel bar : getBars()) {
                    bar.setSelected(true);
                }
                m_isSelected = true;
            } else {
                for (final BarDataModel bar : getBars()) {
                   m_isSelected = bar.selectElement(rect) || m_isSelected;
                }
            }
        }
        return m_isSelected;
    }
    
    /**
     * @see java.lang.Object#clone()
     */
    @Override
    protected BinDataModel clone() {
        final BinDataModel clone = 
            new BinDataModel(m_xAxisCaption, m_lowerBound, m_upperBound);
        for (BarDataModel bar : m_bars.values()) {
            final BarDataModel barClone = bar.clone();
            clone.m_bars.put(barClone.getColor(), barClone);
        }
        return clone;
    }
}
