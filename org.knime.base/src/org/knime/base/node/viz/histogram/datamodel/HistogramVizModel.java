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
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeLogger;

/**
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public abstract class HistogramVizModel {
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(HistogramVizModel.class);

    /** The caption of the bar which holds all missing values. */
    public static final String MISSING_VAL_BAR_CAPTION = "Missing_values";

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
     * The space between to bins in pixels.
     */
    public static final int SPACE_BETWEEN_BINS = 2;

    /**
     * The width of the hilite rectangle in percent of the surrounding
     * rectangle. Should be greater 0 and less than 1. 0.8 = 80%
     */
    public static final double HILITE_RECTANGLE_WIDTH_FACTOR = 0.5;

    /**Compare the caption of bins.*/
    protected static final BinDataModelComparator BIN_CAPTION_COMPARATOR = 
        new BinDataModelComparator(BinDataModelComparator.COMPARE_CAPTION);

    private final SortedSet<Color> m_rowColors;

    private boolean m_binNominal = false;

    private AggregationMethod m_aggrMethod;

    private HistogramLayout m_layout;

    private boolean m_showMissingValBin = true;

    /**
     *The plotter will show all bars empty or not when set to <code>true</code>.
     */
    private boolean m_showEmptyBins = false;

    private final BinDataModel m_missingValueBin = new BinDataModel(
            HistogramVizModel.MISSING_VAL_BAR_CAPTION, 0, 0);
    
    private final List<BinDataModel> m_bins = new ArrayList<BinDataModel>(50);

    private int m_noOfBins;

    /**Constructor for class HistogramVizModel.
     * @param rowColors all possible colors the user has defined for a row
     * @param layout the {@link HistogramLayout} to use
     * @param aggrMethod the {@link AggregationMethod} to use
     * @param noOfBins 
     */
    public HistogramVizModel(final SortedSet<Color> rowColors,
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
     * @return all {@link BinDataModel} objects of this histogram including
     * the missing value bin if the showMissingValue bin variable is set to
     * <code>true</code>
     */
    public Collection<BinDataModel> getBins(){
        final BinDataModel missingValueBin = getMissingValueBin();
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
     * @return the aggregation columns
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
     * @return the noOfBins without the missing value bin
     */
    public int getNoOfBins() {
        return m_noOfBins;
    }


    /**
     * @param noOfBins the new number of bins to create
     * @return <code>true</code> if the number of bins has changed
     */
    public boolean setNoOfBins(final int noOfBins) {
        if (isBinNominal()) {
            throw new IllegalArgumentException(
                    "Not possible for nominal binning");
        }
        if (m_noOfBins == noOfBins) {
            return false;
        }
        m_noOfBins = noOfBins;
        createBins();
        return true;
    }

    /**
     * @param caption the caption of the bin of interest
     * @return the bin with the given caption or <code>null</code> if no bin
     * with the given caption exists
     */
    public BinDataModel getBin(final String caption) {
        for (final BinDataModel bin : getBins()) {
            if (bin.getXAxisCaption().equals(caption)) {
                return bin;
            }
        }
        return null;
    }
    
    /**
     * @param idx the index of the bin
     * @return the {@link BinDataModel} at the given index
     */
    public BinDataModel getBin(final int idx) {
        return m_bins.get(idx);
    }

    /**
     * @return all bin captions in the order they should be displayed
     */
    public Set<DataCell> getBinCaptions() {
        final Collection<BinDataModel> bins = getBins();
        final LinkedHashSet<DataCell> captions = 
            new LinkedHashSet<DataCell>(bins.size());
        for (final BinDataModel bin : bins) {
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
        for (final BinDataModel bin : getBins()) {
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
        for (final BinDataModel bin : getBins()) {
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
    public BinDataModel getMissingValueBin() {
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
        for (final BinDataModel bin : getBins()) {
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
     * @return all keys of hilited rows
     */
    public Set<DataCell> getHilitedKeys() {
        final Set<DataCell> keys = new HashSet<DataCell>();
        for (final BinDataModel bin : getBins()) {
            final Collection<BarDataModel> bars = bin.getBars();
            for (final BarDataModel bar : bars) {
                if (bar.isSelected()) {
                    final Collection<BarElementDataModel> elements = bar
                            .getElements();
                    for (final BarElementDataModel element : elements) {
                        keys.addAll(element.getHilitedKeys());
                    }
                }
            }
        }
        return keys;
    }

    /**
     * @param hilite <code>true</code> if the selected elements should be 
     * hilited or <code>false</code> if they should be unhilited
     * @return all keys of the selected elements
     */
    public Set<DataCell> getSelectedKeys(final boolean hilite) {
        final Set<DataCell> keys = new HashSet<DataCell>();
        for (final BinDataModel bin : getBins()) {
            if (bin.isSelected()) {
                final Collection<BarDataModel> bars = bin.getBars();
                for (final BarDataModel bar : bars) {
                    if (bar.isSelected()) {
                        final Collection<BarElementDataModel> elements = bar
                                .getElements();
                        for (final BarElementDataModel element : elements) {
                            if (element.isSelected()) {
                                keys.addAll(element.getKeys());
                                if (hilite) {
                                    element.setHilitedKeys(keys, m_aggrMethod);
                                } else {
                                    element.clearHilite();
                                }
                            }
                        }
                    }
                }
            }
        }
        return keys;
    }

    /**
     * Selects the element which contains the given point.
     * @param point the point on the screen to select
     */
    public void selectElement(final Point point) {
        for (final BinDataModel bin : getBins()) {
            final Rectangle binRectangle = bin.getBinRectangle();
            if (binRectangle != null && binRectangle.contains(point)) {
                final Collection<BarDataModel> bars = bin.getBars();
                for (final BarDataModel bar : bars) {
                    final Rectangle barRectangle = bar.getBarRectangle();
                    if (barRectangle != null && barRectangle.contains(point)) {
                        bar.setSelected(true);
                        final Collection<BarElementDataModel> elements = bar
                                .getElements();
                        for (final BarElementDataModel element : elements) {
                            final Rectangle elementRectangle = element
                                    .getElementRectangle();
                            //if the bar is to small to draw the different
                            //elements we have to select all elements 
                            //of this bar
                            if (!bar.isDrawElements()
                                    || (elementRectangle != null 
                                        && elementRectangle.contains(point))) {
                                element.setSelected(true);
                                return;
                            }
                        }
                    }
                }
            }
        }
        return;
    }

    /**
     * Selects all elements which are touched by the given rectangle.
     * @param rect the rectangle on the screen select
     */
    public void selectElement(final Rectangle rect) {
        for (final BinDataModel bin : getBins()) {
            final Rectangle binRectangle = bin.getBinRectangle();
            if (binRectangle != null && binRectangle.intersects(rect)) {
                final Collection<BarDataModel> bars = bin.getBars();
                for (final BarDataModel bar : bars) {
                    final Rectangle barRectangle = bar.getBarRectangle();
                    if (barRectangle != null && barRectangle.intersects(rect)) {
                        bar.setSelected(true);
                        final Collection<BarElementDataModel> elements = bar
                                .getElements();
                        for (final BarElementDataModel element : elements) {
                            final Rectangle elementRectangle = element
                                    .getElementRectangle();
                            //if the bar is to small to draw the different
                            //elements we have to select all elements 
                            //of this bar
                            if (!bar.isDrawElements()
                                    || (elementRectangle != null 
                                        && elementRectangle.intersects(rect))) {
                                element.setSelected(true);
                            }
                        }
                    }
                }
            }
        }
        return;
    }

    /**
     * Clears all selections.
     */
    public void clearSelection() {
        for (final BinDataModel bin : getBins()) {
            final Collection<BarDataModel> bars = bin.getBars();
            for (final BarDataModel bar : bars) {
                bar.setSelected(false);
                final Collection<BarElementDataModel> elements = bar
                        .getElements();
                for (final BarElementDataModel element : elements) {
                    element.setSelected(false);
                }
            }
        }
    }

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
        for (final BinDataModel bin : getBins()) {
            final Collection<BarDataModel> bars = bin.getBars();
            for (final BarDataModel bar : bars) {
                final Collection<BarElementDataModel> elements = bar
                        .getElements();
                for (final BarElementDataModel element : elements) {
                    if (hilite) {
                        element.setHilitedKeys(hilited, m_aggrMethod);
                    } else {
                        element.removeHilitedKeys(hilited, m_aggrMethod);
                    }
                }
            }
        }
    }

    /**
     * Unhilites all rows.
     */
    public void unHiliteAll() {
        for (final BinDataModel bin : getBins()) {
            final Collection<BarDataModel> bars = bin.getBars();
            for (final BarDataModel bar : bars) {
                final Collection<BarElementDataModel> elements = bar
                        .getElements();
                for (final BarElementDataModel element : elements) {
                    element.clearHilite();
                }
            }
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
    
    protected abstract void addRows2Bins();

    /**
     * Creates the bins for the currently set binning information
     * and adds all data rows to the corresponding bin.
     */
    protected void createBins() {
        LOGGER.debug("Entering createBins() of class HistogramVizModel.");
        final long startBinTimer = System.currentTimeMillis();
        
        if (isBinNominal()) {
            createNominalBins();
        } else {
            //create the new bins
            createIntervalBins();
        }
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
        // remove the old bar information
        m_bins.clear();
        final Set<DataCell> values = xColSpec.getDomain().getValues();
        for (DataCell value : values) {
            m_bins.add(new BinDataModel(value.toString(), 0, 0));
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
        
        // remove the old bar information
        m_bins.clear();
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
                    firstBar, leftBoundary, rightBoundary);
            firstBar = false;
            final BinDataModel bin = 
                new BinDataModel(binCaption, leftBoundary, rightBoundary);
            m_bins.add(bin);
            // set the left boundary of the next bar to the current right
            // boundary
            leftBoundary = rightBoundary;
        }
    }

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
            getMissingValueBin().addDataRow(id, color, aggrColumns, aggrVals);
            return startBin;
        }
        if (isBinNominal()) {
            return addDataRow2NominalBin(startBin, xVal, color, id, 
                    aggrColumns, aggrVals);
        }
        if (!xVal.getType().isCompatible(DoubleValue.class)) {
            throw new IllegalStateException("X value is not a valid number");
        }
        return addDataRow2NoneNominalBin(startBin, (DoubleValue)xVal, color, 
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
            final BinDataModel bin = getBin(binIdx);
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
    private int addDataRow2NoneNominalBin(final int startBin, 
            final DoubleValue xVal, final Color color, final DataCell id, 
            final Collection<ColorColumn> aggrColumns, 
            final DataCell[] aggrVals) {
        final double value = xVal.getDoubleValue();
         for (int binIdx = startBin, length = getBins().size(); 
             binIdx < length; binIdx++) {
            final BinDataModel bin = getBin(binIdx);
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
