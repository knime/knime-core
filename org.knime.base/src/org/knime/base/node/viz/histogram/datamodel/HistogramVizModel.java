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
 *    12.02.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.datamodel;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
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
 * This class holds all visualization data of a histogram. 
 * @author Tobias Koetter, University of Konstanz
 */
public class HistogramVizModel {
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
    
    private final HistogramDataModel m_model;
    
    private final List<HistogramDataRow> m_dataRows;

    private final List<ColorColumn> m_aggrColumns;
    
    private final DataColumnSpec m_xColSpec;
    
    private final SortedSet<Color> m_barElementColors;
    
    private final List<BinDataModel> m_bins = 
        new ArrayList<BinDataModel>();
    
    private int m_noOfBins;
    
    private boolean m_binNominal = false;
    
    private AggregationMethod m_aggrMethod;
    
    private HistogramLayout m_layout;
    
    private boolean m_inclMissingValBin;
    
    private BinDataModel m_missingValueBin = 
        new BinDataModel(HistogramVizModel.MISSING_VAL_BAR_CAPTION, 0, 0);
        
    /**
     * Constructor for class HistogramVizModel.
     * @param model the {@link HistogramDataModel} to use
     * @param noOfBins the number of bins to create
     * @param aggrMethod the {@link AggregationMethod} to use
     * @param layout {@link HistogramLayout} to use
     */
    public HistogramVizModel(final HistogramDataModel model,
            final int noOfBins, final AggregationMethod aggrMethod,
            final HistogramLayout layout) {
        if (aggrMethod == null) {
            throw new IllegalArgumentException("No aggregation method defined");
        }
        if (model == null) {
            throw new IllegalArgumentException("Model shouldn't ba null");
        }
        if (layout == null) {
            throw new IllegalArgumentException("No layout defined");
        }
        m_model = model;
        m_aggrColumns = m_model.getAggrColumns();
        m_dataRows = m_model.getSortedRows();
        m_xColSpec = m_model.getXColumnSpec();
        m_barElementColors = m_model.getBarElementColors();
        m_noOfBins = noOfBins;
        m_aggrMethod = aggrMethod;
        m_layout = layout;
        
        if (m_model.getXColumnSpec().getType().isCompatible(
                DoubleValue.class)) {
            m_binNominal = false;
//             createIntervalBins();
        } else {
            m_binNominal = true;
//            createNominalBins();
        }
        createBins();
    }

    /**
     * @return the x column name
     */
    public String getXColumnName() {
        return m_xColSpec.getName();
    }

    /**
     * @return the x column specification
     */
    public DataColumnSpec getXColumnSpec() {
        return m_xColSpec;
    }
    /**
     * @return the aggrColumns
     */
    public List<ColorColumn> getAggrColumns() {
        return m_aggrColumns;
    }

    /**
     * @return all available element colors. This is the color the user has
     * set for one attribute in the ColorManager node.
     */
    public SortedSet<Color> getBarElementColors() {
        return m_barElementColors;
    }
    
    /**
     * @return the noOfBins
     */
    public int getNoOfBins() {
        return m_noOfBins;
    }

    /**
     * @param noOfBins the new number of bins to create
     * @return <code>true</code> if the number of bins has changed
     */
    public boolean setNoOfBins(final int noOfBins) {
        if (m_binNominal) {
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
        for (BinDataModel bin : m_bins) {
            if (bin.getXAxisCaption().equals(caption)) {
                return bin;
            }
        }
        return null;
    }
    
    /**
     * @return all {@link BinDataModel} objects of this histogram
     */
    public Collection<BinDataModel> getBins() {
        return m_bins;
    }
    
    /**
     * @param inclEmptyBins set to <code>true</code> to include the caption of
     * empty bins as well
     * @param inclMissingValBin set to <code>true</code> if the missing 
     * value bar should be included as well if it contains at least one row 
     * @return all bin captions in the order they should be displayed
     */
    public LinkedHashSet<DataCell> getBinCaptions(final boolean inclEmptyBins, 
            final boolean inclMissingValBin) {
        LinkedHashSet<DataCell> captions = 
            new LinkedHashSet<DataCell>(m_bins.size() + 1);
        for (BinDataModel bin : m_bins) {
            if (inclEmptyBins || bin.getMaxBarRowCount() > 0) {
                captions.add(new StringCell(bin.getXAxisCaption()));
            }
        }
        if (inclMissingValBin && m_missingValueBin.getMaxBarRowCount() > 0) {
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
        for (BinDataModel bin : m_bins) {
            final double value = 
                bin.getMaxAggregationValue(m_aggrMethod, m_layout);
            if (value > maxAggrValue) {
                maxAggrValue = value;
            }
        }
        if (m_inclMissingValBin 
                && m_missingValueBin.getMaxAggregationValue(m_aggrMethod, 
                        m_layout) 
                < maxAggrValue) {
            maxAggrValue = 
                m_missingValueBin.getMaxAggregationValue(m_aggrMethod, 
                        m_layout);
        }
        return maxAggrValue;
    }

    /**
     * @return the minimum aggregation value
     */
    public double getMinAggregationValue() {
        double minAggrValue = Double.MAX_VALUE;
        for (BinDataModel bin : m_bins) {
            final double value = 
                bin.getMinAggregationValue(m_aggrMethod, m_layout);
            if (value < minAggrValue) {
                minAggrValue = value;
            }
        }
        if (m_inclMissingValBin 
                && m_missingValueBin.getMinAggregationValue(m_aggrMethod, 
                        m_layout) 
                < minAggrValue) {
            minAggrValue = 
                m_missingValueBin.getMinAggregationValue(m_aggrMethod,
                        m_layout);
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
        for (BinDataModel bin : m_bins) {
            if (bin.getBinRowCount() < 1) {
                return true;
            }
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
    public boolean isInclMissingValBin() {
        return m_inclMissingValBin;
    }

    /**
     * @param inclMissingValBin the inclMissingValBin to set
     * @return <code>true</code> if the parameter has changed
     */
    public boolean setInclMissingValBin(final boolean inclMissingValBin) {
        if (m_inclMissingValBin == inclMissingValBin) {
            return false;
        }
        m_inclMissingValBin = inclMissingValBin;
        return true;
    }
    
    /**
     * @return all keys of hilited rows
     */
    public Set<DataCell> getHilitedKeys() {
        Set<DataCell> keys = new HashSet<DataCell>();
        for (BinDataModel bin : m_bins) {            
            final Collection<BarDataModel> bars = bin.getBars();
            for (BarDataModel bar : bars) {
                if (bar.isSelected()) {
                    final Collection<BarElementDataModel> elements = 
                        bar.getElements();
                    for (BarElementDataModel element : elements) {
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
        Set<DataCell> keys = new HashSet<DataCell>();
        for (BinDataModel bin : m_bins) {
            if (bin.isSelected()) {
                final Collection<BarDataModel> bars = bin.getBars();
                for (BarDataModel bar : bars) {
                    if (bar.isSelected()) {
                        final Collection<BarElementDataModel> elements = 
                            bar.getElements();
                        for (BarElementDataModel element : elements) {
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
        for (BinDataModel bin : m_bins) {
            final Rectangle binRectangle = bin.getBinRectangle();
            if (binRectangle != null && binRectangle.contains(point)) {
                final Collection<BarDataModel> bars = bin.getBars();
                for (BarDataModel bar : bars) {
                    final Rectangle barRectangle = bar.getBarRectangle();
                    if (barRectangle != null && barRectangle.contains(point)) {
                        final Collection<BarElementDataModel> elements = 
                            bar.getElements();
                        for (BarElementDataModel element : elements) {
                            final Rectangle elementRectangle = 
                                element.getElementRectangle();
                            if (elementRectangle != null
                                    && elementRectangle.contains(point)) {
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
        for (BinDataModel bin : m_bins) {
            final Rectangle binRectangle = bin.getBinRectangle();
            if (binRectangle != null && binRectangle.intersects(rect)) {
                final Collection<BarDataModel> bars = bin.getBars();
                for (BarDataModel bar : bars) {
                    final Rectangle barRectangle = bar.getBarRectangle();
                    if (barRectangle != null && barRectangle.intersects(rect)) {
                        final Collection<BarElementDataModel> elements = 
                            bar.getElements();
                        for (BarElementDataModel element : elements) {
                            final Rectangle elementRectangle = 
                                element.getElementRectangle();
                            if (elementRectangle != null
                                    && elementRectangle.intersects(rect)) {
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
        for (BinDataModel bin : m_bins) {
            final Collection<BarDataModel> bars = bin.getBars();
            for (BarDataModel bar : bars) {
                final Collection<BarElementDataModel> elements = bar
                        .getElements();
                for (BarElementDataModel element : elements) {
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
        for (BinDataModel bin : m_bins) {
            final Collection<BarDataModel> bars = bin.getBars();
            for (BarDataModel bar : bars) {
                final Collection<BarElementDataModel> elements = 
                    bar.getElements();
                for (BarElementDataModel element : elements) {
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
        for (BinDataModel bin : m_bins) {
            final Collection<BarDataModel> bars = bin.getBars();
            for (BarDataModel bar : bars) {
                final Collection<BarElementDataModel> elements = bar
                        .getElements();
                for (BarElementDataModel element : elements) {
                    element.clearHilite();
                }
            }
        }        
    }
//*******************Binning functions**********************************

    /**
     * Creates the bins for the currently set binning information
     * and adds all data rows to the corresponding bin.
     */
    private void createBins() {
        LOGGER.debug("Entering createBins() of class HistogramVizModel.");
        final long startBinTimer = System.currentTimeMillis();
        
        if (m_binNominal) {
            createNominalBins();
        } else {
            //create the new bins
            createIntervalBins();
        }
        final long startAddRowTimer = System.currentTimeMillis();
        //add the data rows to the new bins
        int startBin = 0;
        for (HistogramDataRow row : m_dataRows) {
            startBin = addDataRow2Bin(startBin, row);
        }
        final long end = System.currentTimeMillis();
        LOGGER.debug(" Total time to create " + m_noOfBins + " bins for " 
                + m_dataRows.size() + " rows: " 
                + (end - startBinTimer) + " in ms.\n"
                + "Time to create bins: " + (startAddRowTimer - startBinTimer)
                + " in ms.\n"
                + "Time to add rows: " + (end - startAddRowTimer) + " in ms.");
        LOGGER.debug("Exiting createBins() of class HistogramVizModel.");
    }
    
    private void createNominalBins() {
        //check if we have the values
        if (m_xColSpec.getDomain().getValues() == null) {
            throw new IllegalArgumentException(
                    "No domain values defined for nominal binning column. " 
                    + "Please use DomainCalculator or ColumnFilter node "
                    + "to set the domain values.");
        }
        // remove the old bar information
        m_bins.clear();
        final Set<DataCell> values = m_xColSpec.getDomain().getValues();
        for (DataCell value : values) {
            m_bins.add(new BinDataModel(value.toString(), 0, 0));
        }
    }
    
    private void createIntervalBins() {
        //set the bounds for binning
        final DataColumnDomain domain = m_xColSpec.getDomain();
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
            m_noOfBins = HistogramVizModel.DEFAULT_NO_OF_BINS;
        }
        if ((lowerBound - upperBound) == 0) {
            m_noOfBins = 1;
        }
        final boolean isInteger = 
            m_xColSpec.getType().isCompatible(IntValue.class);
        final double binInterval = BinningUtil.createBinInterval(upperBound, 
                lowerBound, m_noOfBins, isInteger);
        lowerBound = BinningUtil.createBinStart(lowerBound, binInterval);
        // increase the number of bars to include the max value
        while (lowerBound + (binInterval * m_noOfBins) < upperBound) {
            m_noOfBins++;
        }
        double leftBoundary = BinningUtil.myRoundedBorders(lowerBound, 
                binInterval, HistogramVizModel.INTERVAL_DIGITS);
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
     * @param row the data row to add
     */
    private int addDataRow2Bin(final int startBin, final HistogramDataRow row) {
        for (int binIdx = 0, length = m_bins.size(); binIdx < length; 
            binIdx++) {
            final DataCell xVal = row.getXVal();
            if (xVal.isMissing()) {
                m_missingValueBin.addDataRow(row, m_aggrColumns);
                return startBin;
            }
            final BinDataModel bin = m_bins.get(binIdx);
            final double lowerBound = bin.getLowerBound();
            final double higherBound = bin.getUpperBound();
            if (!xVal.getType().isCompatible(DoubleValue.class)) {
                //it's a nominal x value
                if (bin.getXAxisCaption().equals(
                        xVal.toString())) {
                    bin.addDataRow(row, m_aggrColumns);
                    return startBin;
                }
            } else {
                boolean add2Bin = false;
                final double value = ((DoubleValue)xVal).getDoubleValue();
                if (binIdx == 0) {
                    add2Bin = (value >= lowerBound && value <= higherBound);
                } else {
                    add2Bin = (value >= lowerBound && value <= higherBound);
                }
                if (add2Bin) {
                    bin.addDataRow(row, m_aggrColumns);
                    return binIdx;
                }
            }
        }
        throw new IllegalArgumentException("No bin found for x value:" 
                + row.getXVal().toString());
    }
}
