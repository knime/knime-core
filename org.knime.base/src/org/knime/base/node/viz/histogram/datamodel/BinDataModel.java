/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *    13.03.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.datamodel;

import org.knime.core.data.DataCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.base.node.viz.histogram.datamodel.AbstractHistogramVizModel.HistogramHiliteCalculator;
import org.knime.base.node.viz.histogram.util.ColorColumn;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents one bin in the histogram. A bin represents a value of
 * the selected x column and contains one or more {@link BarDataModel} objects
 * depending on the number of aggregation columns.
 * @author Tobias Koetter, University of Konstanz
 */
public class BinDataModel implements Serializable {

    private static final long serialVersionUID = -5898246116408854042L;


    /**The color of the bar if no aggregation column is selected.*/
    private static final Color NO_AGGR_COL_COLOR = Color.GRAY;

    private static final String CFG_X_CAPTION = "xCaption";
    private static final String CFG_BARS = "bars";
    private static final String CFG_BAR = "bar_";
    private static final String CFG_LOWER_BOUND = "lowerBound";
    private static final String CFG_UPPER_BOUND = "upperBound";
    private static final String CFG_BAR_COUNTER = "barCounter";
    private static final String CFG_HAS_BOUNDARIES = "hasBoundaries";


    private final String m_xAxisCaption;
    private final DataCell m_xAxisCaptionCell;
    private final Double m_lowerBound;
    private final Double m_upperBound;
    private final Map<Color, BarDataModel> m_bars;
    private int m_rowCounter = 0;

    //visual variables
    private boolean m_presentable = true;
    private boolean m_isSelected = false;

    /**The surrounding rectangle is used to distinguish between the
        different bins.*/
    private Rectangle2D m_surroundingRectangle;
    /**The bin rectangle is the main rectangle which contains the bars.*/
    private Rectangle m_binRectangle;

    /**Constructor for class BinDataModel.
     * @param xAxisCaption the caption of this bin on the x axis
     * @param lowerBound the lower bound of the bin interval
     * @param upperBound the higher bound of the bin interval
     */
    public BinDataModel(final String xAxisCaption, final double lowerBound,
            final double upperBound) {
        this(xAxisCaption, new Double(lowerBound), new Double(upperBound),
                new HashMap<Color, BarDataModel>());
    }

    /**Constructor for class BinDataModel.
     * @param xAxisCaption the caption of this bin on the x axis
     * @param lowerBound the lower bound of the bin interval
     * @param upperBound the higher bound of the bin interval
     * @param bars the bar data models of this bin
     */
    private BinDataModel(final String xAxisCaption, final Double lowerBound,
            final Double upperBound, final Map<Color, BarDataModel> bars) {
        if (xAxisCaption == null) {
            throw new IllegalArgumentException("Caption must not be null");
        }
        m_xAxisCaption = xAxisCaption;
        m_xAxisCaptionCell = new StringCell(xAxisCaption);
        m_lowerBound = lowerBound;
        m_upperBound = upperBound;
        m_bars = bars;
    }

    /**
     * @param id the row id
     * @param rowColor the row color
     * @param aggrCols the {@link ColorColumn} objects in the same order
     * like the aggregation values
     * @param aggrVals the aggregation value in the same order like the
     * columns
     */
    public void addDataRow(final RowKey id, final Color rowColor,
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
                    bar = createBar("", NO_AGGR_COL_COLOR);
                    m_bars.put(NO_AGGR_COL_COLOR, bar);
                }
                bar.addDataRow(rowColor, id, new DoubleCell(0));
            } else {
                int i = 0;
                for (final ColorColumn column : aggrCols) {
                    final DataCell cell = aggrVals[i++];
                    final Color barColor = column.getColor();
                    BarDataModel bar = m_bars.get(barColor);
                    if (bar == null) {
                        bar = createBar(column.getColumnName(), barColor);
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
     * @param barName the name of this bar
     * @param color the {@link Color} of the bar
     * @return the created bar
     */
    protected BarDataModel createBar(final String barName, final Color color) {
        return new BarDataModel(barName, color);
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
     * @return all selected bars of this bin
     */
    public List<BarDataModel> getSelectedBars() {
        final Collection<BarDataModel> bars = getBars();
        final List<BarDataModel> selectedBars =
            new ArrayList<BarDataModel>(bars.size());
        for (final BarDataModel bar : bars) {
            if (bar.isSelected()) {
                selectedBars.add(bar);
            }
        }
        return selectedBars;
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
        for (final BarDataModel bar : bars) {
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
        for (final BarDataModel bar : bars) {
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
        for (final BarDataModel bar : bars) {
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
        for (final BarDataModel model : bars) {
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
     * @return <code>true</code> if the number of bars fit in the bin
     */
    public boolean isPresentable() {
        return m_presentable;
    }

    /**
     * @return the {@link Rectangle} the bin should be drawn on the
     * screen
     */
    public Rectangle2D getBinRectangle() {
        return m_binRectangle;
    }


    /**
     * @return the surroundingRectangle to draw to distinguish between the
     * different bins
     */
    public Rectangle2D getSurroundingRectangle() {
        return m_surroundingRectangle;
    }

    /**
     * THE HEIGHT OF THE RECTANGLE SHOULD BE CALCULATED USING THE MIN AND
     * MAX AGGREGATION VALUE TO HANDLES BINS WITH POSITIVE AND NEGATIVE BARS!!!
     * @param binRectangle the {@link Rectangle} the bin should be drawn on the
     * screen
     * @param baseLine the x coordinate of the base line (0) on the screen
     * @param barElementColors all element colors which define the order
     * the elements should be drawn
     * @param aggrColumns the aggregation column array which indicates
     * the order of the bars
     * @param calculator the hilite shape calculator
     */
    public void setBinRectangle(final Rectangle binRectangle,
            final int baseLine,
            final List<Color> barElementColors,
            final Collection<ColorColumn> aggrColumns,
            final HistogramHiliteCalculator calculator) {
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
                bar.setBarRectangle(binRectangle, baseLine,
                        barElementColors, calculator);
                m_presentable = true;
            }
        } else {
            setBarRectangle(baseLine, barElementColors,
                    aggrColumns, calculator);
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
    private void setBarRectangle(final int baseLine,
            final List<Color> barElementColors,
            final Collection<? extends ColorColumn> aggrColumns,
            final HistogramHiliteCalculator calculator) {
            if (m_binRectangle == null) {
                final Collection<BarDataModel> bars = m_bars.values();
                //also reset the bar rectangle
                for (final BarDataModel bar : bars) {
                    bar.setBarRectangle(null, baseLine, barElementColors,
                            calculator);
                }
                return;
            }
            final AggregationMethod aggrMethod = calculator.getAggrMethod();
            final HistogramLayout layout = calculator.getLayout();
            final int binWidth = (int)m_binRectangle.getWidth();
            final int noOfBars = aggrColumns.size();
            m_presentable = elementsFitInBin(noOfBars, binWidth);
            if (!m_presentable) {
                return;
            }
            //calculate the height
            final int binHeight = (int)m_binRectangle.getHeight();
            final double maxAggrVal = Math.max(getMaxAggregationValue(
                    aggrMethod, layout), 0);
            final double minAggrVal = Math.min(getMinAggregationValue(
                    aggrMethod, layout), 0);
            final double valRange = maxAggrVal + Math.abs(minAggrVal);
            if (valRange <= 0) {
                m_presentable = false;
                return;
            }

            final int barWidth = calculateBarWidth(binWidth, noOfBars);
            final double heightPerVal = binHeight / valRange;
            final int binX = (int) m_binRectangle.getX();
            int xCoord = binX;
            for (final ColorColumn aggrColumn : aggrColumns) {
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
                    bar.setBarRectangle(barRect, baseLine, barElementColors,
                            calculator);
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
     * @param barElementColors all element colors which define the order
     * the elements should be drawn
     * @param aggrColumns the current aggregation columns
     * @param baseLine the base line
     * @param calculator the hilite shape calculator
     */
    public void updateBinWidth(final int startX, final int binWidth,
            final List<Color> barElementColors,
            final Collection<ColorColumn> aggrColumns,
            final int baseLine,
            final HistogramHiliteCalculator calculator) {
            if (m_binRectangle == null) {
                return;
            }
            final boolean drawBarBefore = m_presentable;
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
            m_presentable = elementsFitInBin(noOfBars, binWidth);
            if (!m_presentable) {
                //the total bin width is not enough to draw all bars so we don't
                //need to calculate any further and reset all previous
                //bar rectangles
    //            for (BarDataModel bar : bars) {
    //                bar.setBarRectangle(null, aggrMethod, layout, -1, null);
    //            }
                return;
            }
            if (!drawBarBefore) {
                //if the bar couldn't be draw before but now we have to
                //recalculate them
                setBinRectangle(m_binRectangle, baseLine, barElementColors,
                        aggrColumns, calculator);
                return;
            }
            final int barWidth = calculateBarWidth(binWidth, noOfBars);
            int xCoord = startX;
            if (aggrColumns == null || aggrColumns.size() < 1) {
                //the user hasn't selected a aggregation column so we use the
                //dummy bar
                final BarDataModel bar = m_bars.get(NO_AGGR_COL_COLOR);
                bar.updateBarWidth(xCoord, barWidth, barElementColors,
                        baseLine, calculator);
            } else {
                for (final ColorColumn aggrCol : aggrColumns) {
                    final BarDataModel bar = m_bars.get(aggrCol.getColor());
                    bar.updateBarWidth(xCoord, barWidth, barElementColors,
                            baseLine, calculator);
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
        m_presentable = false;
        m_isSelected = false;
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
            if (!m_presentable) {
                for (final BarDataModel bar : getBars()) {
                    bar.setSelected(true);
                }
                m_isSelected = true;
            } else {
                for (final BarDataModel bar : getBars()) {
                   m_isSelected = bar.selectElement(point, true)
                   || m_isSelected;
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
            if (!m_presentable) {
                for (final BarDataModel bar : getBars()) {
                    bar.setSelected(true);
                }
                m_isSelected = true;
            } else {
                for (final BarDataModel bar : getBars()) {
                   m_isSelected = bar.selectElement(rect, true)
                   || m_isSelected;
                }
            }
        }
        return m_isSelected;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BinDataModel clone() {
        final BinDataModel clone =
            new BinDataModel(m_xAxisCaption, m_lowerBound.doubleValue(),
                    m_upperBound.doubleValue());
        for (final BarDataModel bar : m_bars.values()) {
            final BarDataModel barClone = bar.clone();
            clone.m_bars.put(barClone.getColor(), barClone);
        }
        return clone;
    }

    /**
     * @param config the config object to use
     * @param exec the {@link ExecutionMonitor} to provide progress messages
     * @throws CanceledExecutionException if the operation is canceled
     */
    public void save2File(final ConfigWO config,
            final ExecutionMonitor exec) throws CanceledExecutionException {
        config.addString(CFG_X_CAPTION, getXAxisCaption());
        if (getLowerBound() == null || getUpperBound() == null) {
            config.addBoolean(CFG_HAS_BOUNDARIES, false);
        } else {
            config.addBoolean(CFG_HAS_BOUNDARIES, true);
            config.addDouble(CFG_LOWER_BOUND, getLowerBound().doubleValue());
            config.addDouble(CFG_UPPER_BOUND, getUpperBound().doubleValue());
        }
        final ConfigWO barsConf = config.addConfig(CFG_BARS);
        final Collection<BarDataModel> bars = getBars();
        barsConf.addInt(CFG_BAR_COUNTER, bars.size());
        int idx = 0;
        for (final BarDataModel bar : bars) {
            final ConfigWO barConfig = barsConf.addConfig(CFG_BAR + idx++);
            bar.save2File(barConfig, exec);
        }
        exec.checkCanceled();
    }

    /**
     * @param config the config object to use
     * @param exec the {@link ExecutionMonitor} to provide progress messages
     * @return the {@link ColorColumn}
     * @throws CanceledExecutionException if the operation is canceled
     * @throws InvalidSettingsException if the config object is invalid
     */
    public static BinDataModel loadFromFile(final ConfigRO config,
            final ExecutionMonitor exec) throws CanceledExecutionException,
            InvalidSettingsException {
        final String caption = config.getString(CFG_X_CAPTION);
        final Double lowerBound;
        final Double upperBound;
        if (config.getBoolean(CFG_HAS_BOUNDARIES)) {
            lowerBound = new Double(config.getDouble(CFG_LOWER_BOUND));
            upperBound = new Double(config.getDouble(CFG_UPPER_BOUND));
        } else {
            lowerBound = null;
            upperBound = null;
        }
        final ConfigRO barsConf = config.getConfig(CFG_BARS);
        final int barCounter = barsConf.getInt(CFG_BAR_COUNTER);
        final Map<Color, BarDataModel> bars =
            new HashMap<Color, BarDataModel>(barCounter);
        for (int i = 0; i < barCounter; i++) {
            final Config binConf = barsConf.getConfig(CFG_BAR + i);
            final BarDataModel bar = BarDataModel.loadFromFile(binConf, exec);
            bars.put(bar.getColor(), bar);
        }
        exec.checkCanceled();
        return new BinDataModel(caption, lowerBound, upperBound, bars);
    }
}
