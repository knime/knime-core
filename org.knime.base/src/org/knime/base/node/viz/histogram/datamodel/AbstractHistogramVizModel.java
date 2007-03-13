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
 *    26.02.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.datamodel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.base.node.viz.histogram.util.BinningUtil;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeLogger;

/**
 * This is the basic visualization model for a histogram. It handles bin
 * creation and hilite handling.
 * @author Tobias Koetter, University of Konstanz
 */
public abstract class AbstractHistogramVizModel {
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(AbstractHistogramVizModel.class);

    /** The caption of the bar which holds all missing values. */
    public static final String MISSING_VAL_BAR_CAPTION = "Missing_values";
    
    /** Defines the minimum width of a bar. Should be more than the base line
     * stroke.*/
    public static final int MIN_BIN_WIDTH = 4;

    /** This is the minimum space between two bins. */
    public static final int SPACE_BETWEEN_BINS = 2;
    
    /**
     * The default number of bars which get created if the createBinnedBars
     * method is called with a number smaller then 1.
     */
    public static final int DEFAULT_NO_OF_BINS = 10;

    /**
     * Defines the maximum number of decimal places which are used in the
     * binning method.
     */
    public static final int INTERVAL_DIGITS = 2;

    /**
     * The width of the hilite rectangle in percent of the surrounding
     * rectangle. Should be greater 0 and less than 1. 0.8 = 80%
     */
    public static final double HILITE_RECT_WIDTH_FACTOR = 0.5;

    /**Compare the caption of bins.*/
    protected static final BinDataModelComparator BIN_CAPTION_COMPARATOR = 
        new BinDataModelComparator(BinDataModelComparator.COMPARE_CAPTION);

    private final SortedSet<Color> m_rowColors;

    private boolean m_binNominal = false;

    private AggregationMethod m_aggrMethod;

    private HistogramLayout m_layout;

    private boolean m_showMissingValBin = true;
    
    /**If set to true the plotter paints the grid lines for the y axis values.*/
    private boolean m_showGridLines = true;
  
    /** The current basic width of the bins. */
    private int m_binWidth = Integer.MAX_VALUE;
    
    private int m_maxBinWidth;

    /**
     *The plotter will show all bars empty or not when set to <code>true</code>.
     */
    private boolean m_showEmptyBins = false;

    private final InteractiveBinDataModel m_missingValueBin = new InteractiveBinDataModel(
            AbstractHistogramVizModel.MISSING_VAL_BAR_CAPTION, 0, 0);
    
    private final List<InteractiveBinDataModel> m_bins = new ArrayList<InteractiveBinDataModel>(50);

    private int m_noOfBins = 1;
    
    private int m_maxNoOfBins;

    /**Holds the actual size of the drawing space.*/
    private Dimension m_drawingSpace;

    /** The minimum height of a bar with an aggregation value > 0. */
    public static final int MINIMUM_BAR_HEIGHT = 5;
    
    /**Constructor for class HistogramVizModel.
     * @param rowColors all possible colors the user has defined for a row
     * @param layout the {@link HistogramLayout} to use
     * @param aggrMethod the {@link AggregationMethod} to use
     * @param noOfBins the no of bins to create
     */
    public AbstractHistogramVizModel(final SortedSet<Color> rowColors,
            final AggregationMethod aggrMethod, final HistogramLayout layout, 
            final int noOfBins) {
        if (rowColors == null) {
            throw new IllegalArgumentException(
                    "Bar elements shouldn't be null");
        }
        m_rowColors = rowColors;
        m_aggrMethod = aggrMethod;
        m_layout = layout;
        m_noOfBins = noOfBins;
    }
    
    
    /**
     * @return the drawingSpace
     */
    public Dimension getDrawingSpace() {
        return m_drawingSpace;
    }
    
    
    /**
     * @param drawingSpace the drawingSpace to set
     * @return <code>true</code> if the parameter has changed
     */
    public boolean setDrawingSpace(final Dimension drawingSpace) {
        if (drawingSpace == null) {
            throw new IllegalArgumentException(
                    "Drawing space shouldn't be null");
        }
        if (drawingSpace == null || drawingSpace.equals(m_drawingSpace)) {
            return false;
        }
        m_drawingSpace = drawingSpace;
        return true;
    }
    
    /**
     * @return all {@link InteractiveBinDataModel} objects of this histogram including
     * the missing value bin if the showMissingValue bin variable is set to
     * <code>true</code>
     */
    public Collection<InteractiveBinDataModel> getBins() {
        final InteractiveBinDataModel missingValueBin = getMissingValueBin();
        if (missingValueBin != null) {
            if (isShowMissingValBin()) {
                final int missingValBinIdx = m_bins.size() - 1;
                if (m_bins.get(missingValBinIdx) != missingValueBin) {
                    m_bins.add(missingValueBin);
                }
            } else {
                //check the last bin if it's the missing value bin if thats the
                //case remove it from the list
                final int missingValBinIdx = m_bins.size() - 1;
                if (m_bins.get(missingValBinIdx) == missingValueBin) {
                    //if the list contains the missing value bin remove it
                    m_bins.remove(missingValBinIdx);
                }
            }
        }
        return m_bins;
    }

    /**
     * @return the aggregation columns. Could be null!
     */
    public abstract Collection<ColorColumn> getAggrColumns();

    /**
     * @return the x column specification
     */
    public abstract DataColumnSpec getXColumnSpec();

    /**
     * @return the x column name
     */
    public abstract String getXColumnName();

    /**
     * @return all available element colors. This is the color the user has
     * set for one attribute in the ColorManager node.
     */
    public SortedSet<Color> getRowColors() {
        return m_rowColors;
    }

    /**
     * @return the noOfBins without the missing value bin but including the
     * empty bins displayed or not.
     */
    public int getNoOfBins() {
        return m_noOfBins;
    }

    /**
     * @return the maximum number of bins which fit into the 
     * current drawing space
     */
    public int getMaxNoOfBins() {
        calculateMaxNoOfBins();
        return m_maxNoOfBins;
    }

    /**
     * @return the binWidth
     */
    public int getBinWidth() {
        calculateMaxBinWidth();
        checkBinWidth();
        return m_binWidth;
    }
    
    /**
     * Calculates the current preferred width of the bars.
     */
    private void checkBinWidth() {
        if (m_drawingSpace == null) {
            return;
        }
        int binWidth = m_binWidth;
        if (binWidth < 0) {
            // that only occurs at the first call
            //we have to use the getBinCaptions method which checks if the 
            //missing value bin should be included or not and if empty 
            //bins should be displayed
            final int noOfBins = getDisplayedNoOfBins();
            binWidth = (int)(m_drawingSpace.getWidth() / noOfBins)
                    - SPACE_BETWEEN_BINS;
        }
        if (binWidth < MIN_BIN_WIDTH) {
            binWidth = MIN_BIN_WIDTH;
        }
        final int maxBinWidth = getMaxBinWidth();
        if (binWidth > maxBinWidth) {
            // to avoid to wide bars after resizing the window!
            binWidth = maxBinWidth;
        }
        //draw at least a small line
        if (binWidth <= 0) {
            binWidth = 1;
        }
        m_binWidth = binWidth;
    }
    /**
     * Calculates the maximum width per bar for the current display settings.
     */
    private void calculateMaxBinWidth() {
        if (m_drawingSpace == null) {
            return;
        }
        //we have to use the getBinCaptions method which checks if the missing
        //value bin should be included or not and if empty bins should be
        //displayed
        final int noOfBins = getDisplayedNoOfBins();
        //the minimum bin width should be at least 1 pixel
        m_maxBinWidth = Math.max((int)(m_drawingSpace.getWidth() / noOfBins)
                - AbstractHistogramVizModel.SPACE_BETWEEN_BINS, 1);
    }
    
    /**
     * @return the maximum bin width
     */
    public int getMaxBinWidth() {
        calculateMaxBinWidth();
        return m_maxBinWidth;
    }

    /**
     * @param binWidth the binWidth to set
     * @return <code>true</code> if the with has changed
     */
    public boolean setBinWidth(final int binWidth) {
        if (m_binWidth == binWidth) {
            return false;
        }
        if (binWidth < 0) {
            m_binWidth = 0;
        } else {
            m_binWidth = binWidth;
        }
        return true;
    }
    
    /**
     * @param noOfBins the new number of bins to create
     * @return <code>true</code> if the number of bins has changed
     */
    public boolean setNoOfBins(final int noOfBins) {
        int noOf = Math.min(noOfBins, getMaxNoOfBins());
        if (m_noOfBins == noOf) {
            return false;
        }
        m_noOfBins = noOf;
        createBins();
        return true;
    }

    /**
     * @param noOfBins updates the number of bins but doesn't check if the
     * number has changed and thus doesn't recreate the bins if the 
     * number has changed.
     */
    protected void updateNoOfBins(final int noOfBins) {
        m_noOfBins = Math.min(noOfBins, getMaxNoOfBins());
    }

    /**
     * Calculates the maximum number of bars which could be displayed.
     */
    private void calculateMaxNoOfBins() {
        //handle nominal binning special
        if (isBinNominal()) {
            final DataColumnSpec xColSpec = getXColumnSpec();
            final DataColumnDomain domain = xColSpec.getDomain();
            if (domain == null) {
                throw new IllegalStateException(
                        "X column domain shouldn't be null");
            }
            final Set<DataCell> values = domain.getValues();
            if (values == null) {
                throw new IllegalStateException(
                        "Values of x column domain shouldn't be null");
            }
            m_maxNoOfBins = values.size();
            return;
        }
        if (m_drawingSpace == null) {
            //if no drawing space is defined we set the maximum number
            //of bins to the current number of bins
            m_maxNoOfBins = m_bins.size();
            return;
        }
        int maxNoOfBins = (int)(m_drawingSpace.getWidth() 
                / (AbstractHistogramVizModel.MIN_BIN_WIDTH 
                        + AbstractHistogramVizModel.SPACE_BETWEEN_BINS));
        if (isShowMissingValBin() && containsMissingValueBin()) {
            maxNoOfBins--;
        }
        //handle integer values special
        final DataColumnSpec xColSpec = getXColumnSpec();
        if (xColSpec != null) {
            final boolean isInteger = 
                xColSpec.getType().isCompatible(IntValue.class);
            if (isInteger) {
                final DataColumnDomain domain = xColSpec.getDomain();
                if (domain != null) {
                    final IntCell lowerBound = 
                        (IntCell)domain.getLowerBound();
                    final IntCell upperBound = 
                        (IntCell)domain.getUpperBound();
                    final int range = 
                        upperBound.getIntValue() - lowerBound.getIntValue()
                        + 1;
                    if (maxNoOfBins > range) {
                        maxNoOfBins = range;
                    }
                }
            }
        }
        // avoid rounding errors and display at least one bar
        if (maxNoOfBins < 1) {
            maxNoOfBins = 1;
        }
        m_maxNoOfBins = maxNoOfBins;
    }

    /**
     * @return the number of bins which are displayed.
     */
    public int getDisplayedNoOfBins() {
        return getBinCaptions().size();
    }
    
    /**
     * @param caption the caption of the bin of interest
     * @return the bin with the given caption or <code>null</code> if no bin
     * with the given caption exists
     */
    public InteractiveBinDataModel getBin(final String caption) {
        for (final InteractiveBinDataModel bin : getBins()) {
            if (bin.getXAxisCaption().equals(caption)) {
                return bin;
            }
        }
        return null;
    }
    
    /**
     * @param idx the index of the bin
     * @return the {@link InteractiveBinDataModel} at the given index
     */
    public InteractiveBinDataModel getBin(final int idx) {
        return m_bins.get(idx);
    }

    /**
     * @return all bin captions in the order they should be displayed
     */
    public Set<DataCell> getBinCaptions() {
        final Collection<InteractiveBinDataModel> bins = getBins();
        final LinkedHashSet<DataCell> captions = 
            new LinkedHashSet<DataCell>(bins.size());
        for (final InteractiveBinDataModel bin : bins) {
            if (m_showEmptyBins || bin.getMaxBarRowCount() > 0) {
                captions.add(new StringCell(bin.getXAxisCaption()));
            }
        }
        if (m_showMissingValBin && m_missingValueBin.getMaxBarRowCount() > 0) {
            captions.add(new StringCell(m_missingValueBin.getXAxisCaption()));
        }
        return captions;
    }

    /**
     * @return <code>true</code> if the bins are nominal or
     * <code>false</code> if the bins are intervals
     */
    public boolean isBinNominal() {
        return m_binNominal;
    }

    /**
     * @return the maximum aggregation value
     */
    public double getMaxAggregationValue() {
        double maxAggrValue = Double.MIN_VALUE;
        for (final InteractiveBinDataModel bin : getBins()) {
            final double value = bin.getMaxAggregationValue(m_aggrMethod,
                    m_layout);
            if (value > maxAggrValue) {
                maxAggrValue = value;
            }
        }
        return maxAggrValue;
    }

    /**
     * @return the minimum aggregation value
     */
    public double getMinAggregationValue() {
        double minAggrValue = Double.MAX_VALUE;
        for (final InteractiveBinDataModel bin : getBins()) {
            final double value = bin.getMinAggregationValue(m_aggrMethod,
                    m_layout);
            if (value < minAggrValue) {
                minAggrValue = value;
            }
        }
        return minAggrValue;
    }

    /**
     * @return the missingValueBin or <code>null</code> if the selected
     * x column contains no missing values
     */
    public InteractiveBinDataModel getMissingValueBin() {
        if (m_missingValueBin.getMaxBarRowCount() == 0) {
            return null;
        }
        return m_missingValueBin;
    }

    /**
     * @return <code>true</code> if this model contains a missing value bin
     */
    public boolean containsMissingValueBin() {
        return (getMissingValueBin() != null);
    }

    /**
     * @return <code>true</code> if the histogram contains at least one
     * bin with no rows in it.
     */
    public boolean containsEmptyBins() {
        for (final InteractiveBinDataModel bin : getBins()) {
            if (bin.getBinRowCount() < 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return <code>true</code> if the empty bins should be displayed
     */
    public boolean isShowEmptyBins() {
        return m_showEmptyBins;
    }

    /**
     * @param showEmptyBins set to <code>true</code> if also the empty bins 
     * should be shown
     * @return <code>true</code> if the variable has changed
     */
    public boolean setShowEmptyBins(final boolean showEmptyBins) {
        if (showEmptyBins != m_showEmptyBins) {
            m_showEmptyBins = showEmptyBins;
            return true;
        }
        return false;
    }

    /**
     * @return the aggregation method which is used to calculate the 
     * aggregation value
     */
    public AggregationMethod getAggregationMethod() {
        return m_aggrMethod;
    }

    /**
     * @param aggrMethod the aggregation method to use to calculate
     * the aggregation value
     * @return <code>true</code> if the aggregation method has changed
     */
    public boolean setAggregationMethod(final AggregationMethod aggrMethod) {
        if (m_aggrMethod.equals(aggrMethod)) {
            return false;
        }
        m_aggrMethod = aggrMethod;
        return true;
    }

    /**
     * @return the layout
     */
    public HistogramLayout getHistogramLayout() {
        return m_layout;
    }

    /**
     * @param layout the layout to set
     * @return <code>true</code> if the layout has changed
     */
    public boolean setHistogramLayout(final HistogramLayout layout) {
        if (layout != null && !m_layout.equals(layout)) {
            m_layout = layout;
            return true;
        }
        return false;
    }

    /**
     * @return the inclMissingValBin
     */
    public boolean isShowMissingValBin() {
        return m_showMissingValBin;
    }

    /**
     * @param inclMissingValBin the inclMissingValBin to set
     * @return <code>true</code> if the parameter has changed
     */
    public boolean setShowMissingValBin(final boolean inclMissingValBin) {
        if (m_showMissingValBin == inclMissingValBin) {
            return false;
        }
        m_showMissingValBin = inclMissingValBin;
        return true;
    }
    
    /**
     * @return the showGridLines
     */
    public boolean isShowGridLines() {
        return m_showGridLines;
    }
    
    
    /**
     * @param showGridLines the showGridLines to set
     * @return <code>true</code> if the parameter has changed
     */
    public boolean setShowGridLines(final boolean showGridLines) {
        if (m_showGridLines != showGridLines) {
            m_showGridLines = showGridLines;
            return true;
        }
        return false;
    }
    /**
     * @return all keys of hilited rows
     */
    public abstract Set<DataCell> getHilitedKeys();

    /**
     * @return all keys of the selected elements
     */
    public abstract Set<DataCell> getSelectedKeys();

    /**
     * Selects the element which contains the given point.
     * @param point the point on the screen to select
     */
    public abstract void selectElement(final Point point);

    /**
     * Selects all elements which are touched by the given rectangle.
     * @param rect the rectangle on the screen select
     */
    public abstract void selectElement(final Rectangle rect);

    /**
     * Clears all selections.
     */
    public abstract void clearSelection();
    
    /**
     * This method un/hilites all rows with the given key.
     * @param hilited the rowKeys of the rows to un/hilite
     * @param hilite if the given keys should be hilited <code>true</code> 
     * or unhilited <code>false</code>
     */
    public void updateHiliteInfo(final Set<DataCell> hilited,
            final boolean hilite) {
        if (hilited == null || hilited.size() < 1) {
            return;
        }
        for (final InteractiveBinDataModel bin : getBins()) {
            if (hilite) {
                bin.setHilitedKeys(hilited, m_aggrMethod, m_layout);
            } else {
                bin.removeHilitedKeys(hilited, m_aggrMethod, m_layout);
            }
        }
    }

    /**
     * Unhilites all rows.
     */
    public void unHiliteAll() {
        for (final InteractiveBinDataModel bin : getBins()) {
            bin.clearHilite();
        }
    }

    /**
     * @return a HTML <code>String</code> which contains details information
     * about the current selected elements
     */
    public String getHTMLDetailData() {
        final StringBuilder buf = new StringBuilder();
        buf.append("<h2>Details data</h2>");
        buf.append("Nothing selected");
        return buf.toString();
    }

    /**
     * @param nominal set to <code>true</code> if the nominal binning method
     * should be used.
     */
    protected void setBinNominal(final boolean nominal) {
        m_binNominal = nominal;
    }

    /**
     * Clears all information from the missing value bin.
     */
    protected void clearMissingValueBin() {
        if (m_missingValueBin.getBinRowCount() > 0) {
            m_missingValueBin.clear();
        }
    }
    
    /**
     * Creates the bins for the currently set binning information
     * and adds all data rows to the corresponding bin.
     */
    protected void createBins() {
        LOGGER.debug("Entering createBins() of class HistogramVizModel.");
        final long startBinTimer = System.currentTimeMillis();
        // remove the old bar information
        m_bins.clear();
        //clear all information from the missing value bin
        //before recreating the bins
        clearMissingValueBin();
        if (isBinNominal()) {
            createNominalBins();
        } else {
            //create the new bins
            createIntervalBins();
        }
        updateNoOfBins(m_bins.size());
        final long startAddRowTimer = System.currentTimeMillis();
        addRows2Bins();
        final long end = System.currentTimeMillis();
        LOGGER.debug(" Total time to create " + m_noOfBins + " bins: " 
                + (end - startBinTimer) + " in ms.\n"
                + "Time to create bins: " + (startAddRowTimer - startBinTimer)
                + " in ms.\n"
                + "Time to add rows: " + (end - startAddRowTimer) + " in ms.");
        LOGGER.debug("Exiting createBins() of class HistogramVizModel.");
    }
    
    private void createNominalBins() {
        //check if we have the values
        final DataColumnSpec xColSpec = getXColumnSpec();
        if (xColSpec.getDomain().getValues() == null) {
            throw new IllegalArgumentException(
                    "No domain values defined for nominal binning column. " 
                    + "Please use DomainCalculator or ColumnFilter node "
                    + "to set the domain values.");
        }
        final Set<DataCell> values = xColSpec.getDomain().getValues();
        for (DataCell value : values) {
            m_bins.add(new InteractiveBinDataModel(value.toString(), 0, 0));
        }
        //sort the bins by their caption
        Collections.sort(m_bins, BIN_CAPTION_COMPARATOR);
    }
    
    private void createIntervalBins() {
        //set the bounds for binning
        final DataColumnSpec xColSpec = getXColumnSpec();
        final DataColumnDomain domain = xColSpec.getDomain();
        final DataCell lowerBoundCell = domain.getLowerBound();
        if (lowerBoundCell == null || lowerBoundCell.isMissing()
                || !lowerBoundCell.getType().isCompatible(
                        DoubleValue.class)) {
            throw new IllegalArgumentException(
            "The lower bound of the x column domain should be defined");
        }
        double lowerBound = ((DoubleValue)lowerBoundCell).getDoubleValue();
        final DataCell upperBoundCell = domain.getUpperBound();
        if (upperBoundCell == null || upperBoundCell.isMissing()
                || !upperBoundCell.getType().isCompatible(
                        DoubleValue.class)) {
            throw new IllegalArgumentException(
            "The upper bound of the x column domain should be defined");
        }
        final double upperBound = 
            ((DoubleValue)upperBoundCell).getDoubleValue();
        //start the binning
        if (m_noOfBins < 1) {
            m_noOfBins = FixedHistogramVizModel.DEFAULT_NO_OF_BINS;
        }
        if ((lowerBound - upperBound) == 0) {
            m_noOfBins = 1;
        }
        final boolean isInteger = 
            xColSpec.getType().isCompatible(IntValue.class);
        final double binInterval = BinningUtil.createBinInterval(upperBound, 
                lowerBound, m_noOfBins, isInteger);
        lowerBound = BinningUtil.createBinStart(lowerBound, binInterval);
        // increase the number of bars to include the max value
        while (lowerBound + (binInterval * m_noOfBins) < upperBound) {
            m_noOfBins++;
        }
        double leftBoundary = BinningUtil.myRoundedBorders(lowerBound, 
                binInterval, FixedHistogramVizModel.INTERVAL_DIGITS);
        boolean firstBar = true;
        for (int i = 0; i < m_noOfBins; i++) {
            // I have to use this rounding method to avoid problems with very
            // small intervals. If the interval is very small it could happen
            // that we get the same boundaries for several bars by rounding the
            // borders
            double rightBoundary = BinningUtil.myRoundedBorders(
                    leftBoundary + binInterval, binInterval, INTERVAL_DIGITS);
            final String binCaption = BinningUtil.createBarName(
                    firstBar, leftBoundary, rightBoundary, isInteger);
            firstBar = false;
            final InteractiveBinDataModel bin = 
                new InteractiveBinDataModel(binCaption, leftBoundary, rightBoundary);
            m_bins.add(bin);
            // set the left boundary of the next bar to the current right
            // boundary
            leftBoundary = rightBoundary;
        }
    }

    /**
     *This method should loop through all data rows and should add each row
     *to the corresponding bin by calling the 
     *{@link #addDataRow2Bin(int, DataCell, Color, DataCell, 
     *Collection, DataCell[])} method.
     */
    protected abstract void addRows2Bins();
    
    /**
     * Adds the given data row to the corresponding bin.
     * @param startBin the index of the bin to start with
     * @param xVal the x axis value oft this row
     * @param color the color of this row
     * @param id the id of this row
     * @param aggrColumns the selected aggregation columns
     * @param aggrVals the aggregation values
     * @return the index of the bin to whom this row was added
     */
    protected int addDataRow2Bin(final int startBin, final DataCell xVal, 
            final Color color, final DataCell id, 
            final Collection<ColorColumn> aggrColumns, 
            final DataCell[] aggrVals) {
        
        if (xVal.isMissing()) {
            m_missingValueBin.addDataRow(id, color, aggrColumns, aggrVals);
            return startBin;
        }
        if (isBinNominal()) {
            return addDataRow2NominalBin(startBin, xVal, color, id, 
                    aggrColumns, aggrVals);
        }
        if (!xVal.getType().isCompatible(DoubleValue.class)) {
            throw new IllegalStateException("X value is not a valid number");
        }
        return addDataRow2IntervalBin(startBin, (DoubleValue)xVal, color, 
                id, aggrColumns, aggrVals);
    }

    /**
     * Adds the given row to a nominal bin by checking if the given x 
     * value.toString is equal to the current bin caption.
     * @param startBin the index of the bin to start with
     * @param xVal the x value of this row
     * @param color the color of this row
     * @param id the id of this row
     * @param aggrColumns the selected aggregation columns
     * @param aggrVals the aggregation values
     * @return the index of the bin this row was added
     */
    private int addDataRow2NominalBin(final int startBin, final DataCell xVal, 
            final Color color, final DataCell id, 
            final Collection<ColorColumn> aggrColumns, 
            final DataCell[] aggrVals) {
        final String xValString = xVal.toString();
         for (int binIdx = startBin, length = getBins().size(); 
             binIdx < length; binIdx++) {
            final InteractiveBinDataModel bin = getBin(binIdx);
            if (bin.getXAxisCaption().equals(xValString)) {
                bin.addDataRow(id, color, aggrColumns, aggrVals);
                return startBin;
            }
         }
        throw new IllegalArgumentException("No bin found for x value:" 
                + xValString);
    }
    
    /**
     * Adds the given row to a none nominal bin by checking if the given
     * x value is in the range of the lower and upper bound of a bin.
     * @param startBin the index of the bin to start with
     * @param xVal the x value of this row
     * @param color the color of this row
     * @param id the id of this row
     * @param aggrColumns the selected aggregation columns
     * @param aggrVals the aggregation values
     * @return the index of the bin this row was added
     */
    private int addDataRow2IntervalBin(final int startBin, 
            final DoubleValue xVal, final Color color, final DataCell id, 
            final Collection<ColorColumn> aggrColumns, 
            final DataCell[] aggrVals) {
        final double value = xVal.getDoubleValue();
         for (int binIdx = startBin, length = getBins().size(); 
             binIdx < length; binIdx++) {
            final InteractiveBinDataModel bin = getBin(binIdx);
            final Double lowerBoundObj = bin.getLowerBound();
            final Double upperBoundObj = bin.getUpperBound();
            if (lowerBoundObj == null || upperBoundObj == null) {
                continue;
            }
            final double lowerBound = lowerBoundObj.doubleValue();
            final double upperBound = upperBoundObj.doubleValue();
            boolean add2Bin = false;
            if (binIdx == 0) {
                add2Bin = (value >= lowerBound && value <= upperBound);
            } else {
                add2Bin = (value > lowerBound && value <= upperBound);
            }
            if (add2Bin) {
                bin.addDataRow(id, color, aggrColumns, aggrVals);
                return binIdx;
            }
         }
        throw new IllegalArgumentException("No bin found for x value:" 
                + xVal.toString());
    }
}
