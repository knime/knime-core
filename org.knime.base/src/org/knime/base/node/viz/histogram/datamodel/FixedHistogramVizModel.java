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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.base.node.viz.histogram.util.BinningUtil;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.node.NodeLogger;


/**
 * This class holds all visualization data of a histogram. 
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedHistogramVizModel extends 
HistogramVizModel {
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(FixedHistogramVizModel.class);

    private final FixedHistogramDataModel m_model;
    
    private final List<FixedHistogramDataRow> m_dataRows;

    private final List<ColorColumn> m_aggrColumns;
    
    private final DataColumnSpec m_xColSpec;
    
    /**
     * Constructor for class HistogramVizModel.
     * @param model the {@link FixedHistogramDataModel} to use
     * @param noOfBins the number of bins to create
     * @param aggrMethod the {@link AggregationMethod} to use
     * @param layout {@link HistogramLayout} to use
     */
    public FixedHistogramVizModel(final FixedHistogramDataModel model,
            final int noOfBins, final AggregationMethod aggrMethod,
            final HistogramLayout layout) {
        super(model.getBarElementColors());
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
    @Override
    public String getXColumnName() {
        return m_xColSpec.getName();
    }

    /**
     * @return the x column specification
     */
    @Override
    public DataColumnSpec getXColumnSpec() {
        return m_xColSpec;
    }
    /**
     * @return the aggrColumns
     */
    @Override
    public List<ColorColumn> getAggrColumns() {
        return m_aggrColumns;
    }

    /**
     * @return all {@link BinDataModel} objects of this histogram including
     * the missing value bin if the showMissingValue bin variable is set to
     * <code>true</code>
     */
    @Override
    public Collection<BinDataModel> getBins() {
        final BinDataModel missingValueBin = getMissingValueBin();
        if (missingValueBin != null) {
            if (m_showMissingValBin) {
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
     * Creates the bins for the currently set binning information
     * and adds all data rows to the corresponding bin.
     */
    @Override
    protected void createBins() {
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
        for (FixedHistogramDataRow row : m_dataRows) {
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
        //sort the bins by their caption
        Collections.sort(m_bins, BIN_CAPTION_COMPARATOR);
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
            m_noOfBins = FixedHistogramVizModel.DEFAULT_NO_OF_BINS;
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
     * @param startBin the index of the bin to start looking
     * @param row the data row to add
     * @return the index of the bin where the row was added
     */
    private int addDataRow2Bin(final int startBin, final FixedHistogramDataRow row) {
        final DataCell xVal = row.getXVal();
        if (xVal.isMissing()) {
            m_missingValueBin.addDataRow(row.getRowKey().getId(), 
                    row.getColor(), m_aggrColumns, row.getAggrVals());
            return startBin;
        }
        if (startBin >= m_bins.size()) {
            throw new IllegalArgumentException("Start bin shouldn't be bigger "
                    + "than number of bins");
        }
        if (m_binNominal) {
            return addDataRow2NominalBin(startBin, row, xVal);
        }
        if (!xVal.getType().isCompatible(DoubleValue.class)) {
            throw new IllegalStateException("X value is not a valid number");
        }
        return addDataRow2NoneNominalBin(startBin, row, (DoubleValue)xVal);
    }

    /**
     * Adds the given row to a nominal bin by checking if the given x 
     * value.toString is equal to the current bin caption.
     * @param startBin the bin to start looking
     * @param row the row to add
     * @param xVal the x value of the row
     * @return the index of the bin where the row was added
     */
    private int addDataRow2NominalBin(final int startBin, 
            final FixedHistogramDataRow row, final DataCell xVal) {
        final String xValString = xVal.toString();
         for (int binIdx = startBin, length = m_bins.size(); binIdx < length; 
            binIdx++) {
            final BinDataModel bin = m_bins.get(binIdx);
            if (bin.getXAxisCaption().equals(xValString)) {
                bin.addDataRow(row.getRowKey().getId(), row.getColor(), 
                        m_aggrColumns, row.getAggrVals());
                return startBin;
            }
         }
        throw new IllegalArgumentException("No bin found for x value:" 
                + row.getXVal().toString());
    }
    
    /**
     * Adds the given row to a none nominal bin by checking if the given
     * x value is in the range of the lower and upper bound of a bin.
     * @param startBin the bin to start looking
     * @param row the row to add
     * @param xVal the x value of the row
     * @return the index of the bin where the row was added
     */
    private int addDataRow2NoneNominalBin(final int startBin, 
            final FixedHistogramDataRow row, final DoubleValue xVal) {
        final double value = xVal.getDoubleValue();
         for (int binIdx = startBin, length = m_bins.size(); binIdx < length; 
            binIdx++) {
            final BinDataModel bin = m_bins.get(binIdx);
            final double lowerBound = bin.getLowerBound();
            final double higherBound = bin.getUpperBound();
            boolean add2Bin = false;
            if (binIdx == 0) {
                add2Bin = (value >= lowerBound && value <= higherBound);
            } else {
                add2Bin = (value > lowerBound && value <= higherBound);
            }
            if (add2Bin) {
                bin.addDataRow(row.getRowKey().getId(), row.getColor(), 
                        m_aggrColumns, row.getAggrVals());
                return binIdx;
            }
         }
        throw new IllegalArgumentException("No bin found for x value:" 
                + row.getXVal().toString());
    }
}
