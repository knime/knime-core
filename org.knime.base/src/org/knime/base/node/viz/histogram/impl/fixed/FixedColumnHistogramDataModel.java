/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 */
package org.knime.base.node.viz.histogram.impl.fixed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.knime.base.node.viz.histogram.AbstractHistogramDataModel;
import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.util.BinningUtil;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.property.ColorAttr;


/**
 * Class which holds the Histogram data model. The x axis property is fixed but
 * the aggregation property and method is flexible.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedColumnHistogramDataModel extends AbstractHistogramDataModel {
    
    /**
     * Saves all data rows which are added to this class. To get them ordered
     * use the getOrderedDataRows() method to access them!
     */
    private final List<HistogramDataRow> m_dataRows;

    private final HistogramRowComparator m_rowComparator;
    
    /**
     * The number of added rows after the last sorting. If greater then 0 the
     * rows get sorted if you call the <code>getOrderedDataRows</code> method
     */
    private int m_noOfNotSortedRows = 0;
    

    /**
     * Constructor for class FixedColumnHistogramDataModel.
     * 
     * @param tableSpec the <code>DataTableSpec</code> of the data on which
     *            this <code>HistogramData</code> object based on
     * @param xCoordLabel the label of the column for which the bars should be
     *            created
     * @param noOfBars the number of bars to create if the x column is numerical
     * @param aggregationColumn column to aggregate on
     * @param aggrMethod the aggregation method
     */
    protected FixedColumnHistogramDataModel(final DataTableSpec tableSpec,
            final String xCoordLabel, final int noOfBars,
            final String aggregationColumn, 
            final AggregationMethod aggrMethod) {
        super(tableSpec, xCoordLabel, noOfBars, aggregationColumn, aggrMethod);
        m_rowComparator = new HistogramRowComparator(getXColComparator());
        m_dataRows = new ArrayList<HistogramDataRow>();
    }
    
    /**
     * Constructor for class HistogramData which uses the default number of 
     * bars for binning of numerical x columns.
     * 
     * @param tableSpec the <code>DataTableSpec</code> of the data on which
     *            this <code>HistogramData</code> object based on
     * @param xCoordLabel the label of the column for which the bars should be
     *            created
     * @param aggregationColumn column to aggregate on
     * @param aggrMethod the aggregation method
     */
    protected FixedColumnHistogramDataModel(final DataTableSpec tableSpec,
            final String xCoordLabel, final String aggregationColumn,
            final AggregationMethod aggrMethod) {
        this(tableSpec, xCoordLabel, DEFAULT_NO_OF_BARS,
                aggregationColumn, aggrMethod);
    }

    /**
     * @see org.knime.base.node.viz.histogram.AbstractHistogramDataModel
     *  #addDataRow(org.knime.core.data.DataRow)
     */
    @Override
    public void addDataRow(final DataRow row) {
        if (row == null) {
            throw new IllegalArgumentException("Row shouldn't be null.");
        }
        if (row.getNumCells() < getXColumnIdx() 
                || row.getNumCells() < getAggregationColumnIdx()) {
            throw new IllegalArgumentException("Row is to short.");
        }
        final DataCell xCell = row.getCell(getXColumnIdx());
        final DataCell aggrCell = row.getCell(getAggregationColumnIdx());
        if (xCell == null
                || (!xCell.isMissing() && !xCell.getType().equals(
                        getOriginalXColSpec().getType()))) {
            throw new IllegalArgumentException("X cell null or column type of "
                    + "this row and defined x coordinate column not equal.");
        }
        final ColorAttr colorAttr = getTableSpec().getRowColor(row);
        final HistogramDataRow histoRow = new HistogramDataRow(row.getKey(), 
                colorAttr, xCell, aggrCell);
        m_dataRows.add(histoRow);
        m_noOfNotSortedRows++;
        if (isNominal()) {
            if (xCell.isMissing()) {
                addRow2MissingValBar(histoRow);
            } else {
                String caption = xCell.toString();
                FixedColumnBarDataModel bar = 
                    (FixedColumnBarDataModel) getBar(caption);
                if (bar == null) {
                    bar = new FixedColumnBarDataModel(caption, getXColumnIdx(),
                            getAggregationColumnIdx(),
                            getAggregationMethod());
                    addBar(bar);
                }
                bar.addRow(histoRow);
            }
        } // if it's a none nominal column the bars are created and filled in
        // the getBars method
        add2MinMaxValue(xCell);
    }

    /**
     * Adds the given row to the missing value bar. If the bar doesn't exists
     * yet it will be created.
     * 
     * @param row the row to add to the bar
     */
    private void addRow2MissingValBar(final HistogramDataRow row) {
        FixedColumnBarDataModel missingValBar = 
            (FixedColumnBarDataModel)getMissingValueBar();
        if (missingValBar == null) {
            missingValBar = new FixedColumnBarDataModel(
                    MISSING_VAL_BAR_CAPTION, getXColumnIdx(), 
                    getAggregationColumnIdx(), getAggregationMethod());
            setMissingValueBar(missingValBar);
        }
        missingValBar.addRow(row);
    }

    // ***********Helper classes********************

    /**
     * Creates the <code>HistogramBar</code> objects for the given bin values
     * and returns the captions of all created bars.
     * 
     * @param numberOfBars the number of bars to create
     */
    @Override
    protected void createBinnedBars(final int numberOfBars) {
        //check if we have some data rows
        if (m_dataRows == null || m_dataRows.size() < 1) {
            return;
        }
        // remove the old bar information
        clearBarInformation();
        DataCell maxValCell = getMaxVal();
        DataCell minValCell = getMinVal();
        if (maxValCell == null || minValCell == null) {
            throw new IllegalArgumentException(
                    "Min and max cell shouldn't be null.");
        }
        double maxVal = 0;
        if (!maxValCell.isMissing()) {
            maxVal = BinningUtil.getNumericValue(maxValCell);
        }
        double minVal = 0;
        if (!minValCell.isMissing()) {
            minVal = BinningUtil.getNumericValue(minValCell);
        }
        /*
         * if (minVal > 0) { //start with 0 as left border to have nicer
         * intervals minVal = 0; }
         */
        int noOfBars = numberOfBars;
        if (noOfBars < 1) {
            noOfBars = DEFAULT_NO_OF_BARS;
        }
        if ((maxVal - minVal) == 0) {
            noOfBars = 1;
        }
        final boolean isInteger = 
            getOriginalXColSpec().getType().isCompatible(IntValue.class);
        final double binInterval = BinningUtil.createBinInterval(maxVal, 
                minVal, noOfBars, isInteger);
        minVal = BinningUtil.createBinStart(minVal, binInterval);
        // increase the number of bars to include the max value
        while (minVal + (binInterval * noOfBars) < maxVal) {
            noOfBars++;
        }
        double leftBoundary = BinningUtil.myRoundedBorders(minVal, binInterval,
                INTERVAL_DIGITS);
        List<HistogramDataRow> sortedRows = getOrderedDataRows();
        int rowLength = sortedRows.size();
        FixedColumnBarDataModel bar = null;
        final int xCoordColIdx = getXColumnIdx();
        final int aggrColIdx = getAggregationColumnIdx();
        final AggregationMethod aggrMethod = getAggregationMethod();
        int currentRowIdx = 0;
        for (int i = 0; i < noOfBars; i++) {
            // I have to use this rounding method to avoid problems with very
            // small intervals. If the interval is very small it could happen
            // that we get the same boundaries for several bars by rounding the
            // borders
            double rightBoundary = BinningUtil.myRoundedBorders(
                    leftBoundary + binInterval, binInterval, INTERVAL_DIGITS);
            String binCaption = 
                BinningUtil.createBarName(leftBoundary, rightBoundary);
            bar = (FixedColumnBarDataModel) getBar(binCaption);
            if (bar == null) {
                bar = new FixedColumnBarDataModel(binCaption, xCoordColIdx, 
                        aggrColIdx, aggrMethod);
                addBar(bar);
            } else {
                // this should never happen because we clean the m_bars variable
                // before entering the for loop!
                throw new IllegalStateException("Bar with caption "
                        + binCaption + " already exists.");
            }
            boolean isLower = true;
            while (isLower && currentRowIdx < rowLength) {
                final HistogramDataRow row = sortedRows.get(currentRowIdx);
                final DataCell cell = row.getXVal();
                if (cell == null || cell.isMissing()) {
                    addRow2MissingValBar(row);
                    currentRowIdx++;
                    continue;
                }
                double val = BinningUtil.getNumericValue(cell);
                if (val <= rightBoundary) {
                    bar.addRow(row);
                    currentRowIdx++;
                } else {
                    isLower = false;
                }
            }
            // set the left boundary of the next bar to the current right
            // boundary
            leftBoundary = rightBoundary;
        }
        if (currentRowIdx <= rowLength) {
            // check if their are some rows left
            // if so add them to the last bar to whom they belong because
            // of the sorting
            if (bar == null) {
                throw new IllegalStateException("Internal exception: "
                        + "Bar shouldn't be null.");
            }
            while (currentRowIdx < rowLength) {

                HistogramDataRow row = sortedRows.get(currentRowIdx++);
                bar.addRow(row);
            }
        }
        // recalculate the aggregation values
        calculateAggregationValues();
    }

    /**
     * @return all rows sorted by the x axis property in ascending order
     */
    private List<HistogramDataRow> getOrderedDataRows() {
        if (m_noOfNotSortedRows > 0) {
            Collections.sort(m_dataRows, m_rowComparator);
            m_noOfNotSortedRows = 0;
        }
        return m_dataRows;
    }
}
