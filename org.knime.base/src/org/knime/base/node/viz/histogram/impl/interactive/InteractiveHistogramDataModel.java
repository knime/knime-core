/*
 * ------------------------------------------------------------------
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
package org.knime.base.node.viz.histogram.impl.interactive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;

import org.knime.base.node.viz.histogram.AbstractBarDataModel;
import org.knime.base.node.viz.histogram.AbstractHistogramDataModel;
import org.knime.base.node.viz.histogram.AggregationMethod;
import org.knime.base.node.viz.histogram.util.BinningUtil;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.def.StringCell;


/**
 * Class which holds the Histogram data model. The x axis property is fixed but
 * the aggregation property and method is flexible.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractiveHistogramDataModel extends AbstractHistogramDataModel {
    /**
     * Saves all data rows which are added to this class. To get them ordered
     * use the getOrderedDataRows() method to access them!
     */
    private final List<DataRow> m_dataRows;

    /**
     * The <code>Comparator</code> used to sort the added data rows by the
     * defined x axis property.
     */
    private final RowByColumnComparator m_rowComparator;

    /**
     * The number of added rows after the last sorting. If greater then 0 the
     * rows get sorted if you call the <code>getOrderedDataRows</code> method
     */
    private int m_noOfNotSortedRows = 0;

    /**
     * Constructor for class HistogramData.
     * 
     * @param tableSpec the <code>DataTableSpec</code> of the data on which
     *            this <code>HistogramData</code> object based on
     * @param xCoordLabel the label of the column for which the bars should be
     *            created
     * @param noOfBars the number of bars to create if the x column is numerical
     * @param aggregationColumn column to aggregate on
     * @param aggrMethod the aggregation method
     */
    protected InteractiveHistogramDataModel(final DataTableSpec tableSpec,
            final String xCoordLabel, final int noOfBars,
            final String aggregationColumn, 
            final AggregationMethod aggrMethod) {
        super(tableSpec, xCoordLabel, noOfBars, aggregationColumn, aggrMethod);
        m_rowComparator = new RowByColumnComparator(getXColumnIdx(), 
                getXColComparator());
        m_dataRows = new ArrayList<DataRow>();
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
    public InteractiveHistogramDataModel(final DataTableSpec tableSpec,
            final String xCoordLabel, final String aggregationColumn,
            final AggregationMethod aggrMethod) {
        this(tableSpec, xCoordLabel, DEFAULT_NO_OF_BARS,
                aggregationColumn, aggrMethod);
    }

    /**
     * @see org.knime.base.node.viz.histogram.AbstractHistogramDataModel
     * #addDataRow(org.knime.core.data.DataRow)
     */
    @Override
    public void addDataRow(final DataRow row) {
        if (row == null) {
            throw new IllegalArgumentException("Row shouldn't be null.");
        }
        if (row.getNumCells() < getXColumnIdx()) {
            throw new IllegalArgumentException("Row is to short.");
        }
        DataCell xCell = row.getCell(getXColumnIdx());
        if (xCell == null
                || (!xCell.isMissing() && !xCell.getType().equals(
                        getOriginalXColSpec().getType()))) {
            throw new IllegalArgumentException("Cell null or column type of "
                    + "this row and defined x coordinate column not equal.");
        }
        m_dataRows.add(row);
        m_noOfNotSortedRows++;
        if (isNominal()) {
            if (xCell.isMissing()) {
                addRow2MissingValBar(row);
            } else {
                String caption = xCell.toString();
                AbstractBarDataModel bar = getBar(caption);
                if (bar == null) {
                    bar = new InteractiveBarDataModel(caption, 
                            getAggregationColumnIdx(),
                            getAggregationMethod());
                    addBar(bar);
                }
                ((InteractiveBarDataModel)bar).addRow(row);
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
    private void addRow2MissingValBar(final DataRow row) {
        InteractiveBarDataModel missingValBar = 
            (InteractiveBarDataModel)getMissingValueBar();
        if (missingValBar == null) {
            missingValBar = new InteractiveBarDataModel(
                    MISSING_VAL_BAR_CAPTION, getAggregationColumnIdx(), 
                    getAggregationMethod());
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
//      check if we have some data rows
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
        final double binInterval = BinningUtil.createBinInterval(maxVal, 
                minVal, noOfBars, 
                getOriginalXColSpec().getType().isCompatible(IntValue.class));
        minVal = BinningUtil.createBinStart(minVal, binInterval);
        // increase the number of bars to include the max value
        while (minVal + (binInterval * noOfBars) < maxVal) {
            noOfBars++;
        }
        // TK_TODO: Improve the binning especially the integer binning

        double leftBoundary = BinningUtil.myRoundedBorders(minVal, binInterval,
                INTERVAL_DIGITS);
        List<DataRow> sortedRows = getOrderedDataRows();
        int rowLength = sortedRows.size();
        InteractiveBarDataModel bar = null;
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
            String binCaption = BinningUtil.createBarName(leftBoundary, 
                    rightBoundary);
            bar = (InteractiveBarDataModel)getBar(binCaption);
            if (bar == null) {
                bar = new InteractiveBarDataModel(binCaption, aggrColIdx, 
                        aggrMethod);
                addBar(bar);
            } else {
                // this should never happen because we clean the m_bars variable
                // before entering the for loop!
                throw new IllegalStateException("Bar with caption "
                        + binCaption + " already exists.");
            }
            boolean isLower = true;
            while (isLower && currentRowIdx < rowLength) {
                DataRow row = sortedRows.get(currentRowIdx);
                DataCell cell = row.getCell(getXColumnIdx());
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

                DataRow row = sortedRows.get(currentRowIdx++);
                bar.addRow(row);
            }
        }
        // recalculate the aggregation values
        calculateAggregationValues();
    }

    /**
     * @return all <code>DataRows</code> on which this model based on
     */
    protected Collection<DataRow> getDataRow() {
        return m_dataRows;
    }

    /**
     * @return all rows sorted by the x axis property in ascending order
     */
    private List<DataRow> getOrderedDataRows() {
        if (m_noOfNotSortedRows > 0) {
            Collections.sort(m_dataRows, m_rowComparator);
            m_noOfNotSortedRows = 0;
        }
        return m_dataRows;
    }

    /**
     * @see org.knime.base.node.viz.histogram.AbstractHistogramDataModel#clone()
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        final InteractiveHistogramDataModel model = 
            new InteractiveHistogramDataModel(getTableSpec(), getXColumn(), 
                getNumberOfBars(), getAggregationColumn(), 
                getAggregationMethod());
        model.m_dataRows.addAll(m_dataRows);
        model.m_noOfNotSortedRows = m_noOfNotSortedRows;
        final LinkedHashSet<DataCell> barCaptions = getBarCaptions();
        final LinkedHashSet<DataCell> captionCopy = 
            new LinkedHashSet<DataCell>(barCaptions.size());
        for (DataCell cell : barCaptions) {
            captionCopy.add(new StringCell(cell.toString()));
        }
        model.setBarCaptions(captionCopy);
        final Hashtable<String, AbstractBarDataModel> bars = getBars();
        final Hashtable<String, AbstractBarDataModel> barsCopy =  
            new Hashtable<String, AbstractBarDataModel>(bars.size());
        final Enumeration<String> barKeys = bars.keys();
        while (barKeys.hasMoreElements()) {
            String barKey = barKeys.nextElement();
            final AbstractBarDataModel barModel = 
                (AbstractBarDataModel)bars.get(barKey).clone();
            barsCopy.put(barKey, barModel);
        }
        model.setBars(barsCopy);
        model.setMissingValueBar(getMissingValueBar());
        model.setMinVal(getMinVal());
        model.setMaxVal(getMaxVal());
        model.calculateAggregationValues();
        return model;
    }
}
