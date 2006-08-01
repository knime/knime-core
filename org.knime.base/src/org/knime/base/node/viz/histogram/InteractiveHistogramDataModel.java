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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 */
package org.knime.base.node.viz.histogram;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.def.StringCell;


/**
 * Class which holds the Histogram data model. The x axis property is fixed but
 * the aggregation property and method is flexible.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractiveHistogramDataModel implements HistogramDataModel {
    /**
     * The default number of bars which get created if the createBinnedBars
     * method is called with a number smaller then 1.
     */
    private static final int DEFAULT_NO_OF_BARS = 10;

    /**
     * Defines the maximum number of decimal places which are used in the
     * binning method.
     */
    protected static final int INTERVAL_DIGITS = 2;

    /** The <code>DataTableSpec</code> of the original data. */
    private final DataTableSpec m_tableSpec;

    /**
     * <code>Hashtable</code> with all <code>BarDataModel</code> objects as
     * values and their caption as <code>String</code> as key.
     */
    private final Hashtable<String, InteractiveBarDataModel> m_bars;

    private int m_noOfBars;
    
    /**
     * <code>BarDataModel</code> object with all rows which have no value for
     * the selected x axis column or <code>null</code> if all rows have a
     * value.
     */
    private InteractiveBarDataModel m_missingValueBar = null;

    /** The index of the column of the x axis. */
    private final int m_xCoordRowIdx;

    /** The <code>DataColumnSpec</code> of the x axis column. */
    private final DataColumnSpec m_originalXColSpec;

    /** Flag which indicates if the x axis property is nominal. */
    private final boolean m_xIsNominal;

    /**
     * The intervals of the bins which are automatically created for none
     * nominal values. Holds the right boundary of the interval in the first
     * dimension and the number of rows in the second dimension.
     */
    private double[][] m_binIntervals;

    /** All bar captions in the order they should be displayed. */
    private final LinkedHashSet<DataCell> m_barCaptions;

    /** The max value for the x column. */
    private DataCell m_maxVal = null;

    /** The min values for the x column. */
    private DataCell m_minVal = null;

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
     * The name of the column to aggregate. Could be <code>null</code> if the
     * aggregation method is count!.
     */
    private String m_aggrColumn;

    /** The current aggregation method. */
    private AggregationMethod m_aggrMethod = AggregationMethod.COUNT;

    /**
     * The minimum value for the aggregation column without the missing value
     * bar.
     */
    private double m_minAggrValue = Double.NaN;

    /** The maximum for the aggregation column without the missing value bar. */
    private double m_maxAggrValue = Double.NaN;

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
        if (tableSpec == null) {
            throw new IllegalArgumentException("tableSpec shouldn't be null.");
        }
        m_tableSpec = tableSpec;
        if (xCoordLabel == null) {
            throw new IllegalArgumentException(
                    "xCoordLabel must be specified.");
        }
        m_xCoordRowIdx = m_tableSpec.findColumnIndex(xCoordLabel);
        if (m_xCoordRowIdx < 0) {
            throw new IllegalArgumentException("No valid column name");
        }
        if (aggrMethod == null) {
            throw new IllegalArgumentException("Aggregation method shouldn't"
                    + " be null");
        }
        m_originalXColSpec = m_tableSpec.getColumnSpec(m_xCoordRowIdx);
        DataType colType = m_originalXColSpec.getType();
        if (colType.isCompatible(DoubleValue.class)) {
            m_xIsNominal = false;
        } else {
            m_xIsNominal = true;
        }
        // get the right comparator for the current selected x column for row
        // sorting
        DataValueComparator cellComparator = colType.getComparator();
        m_rowComparator = new RowByColumnComparator(m_xCoordRowIdx,
                cellComparator);
        // Initialise all internal data structures
        m_bars = new Hashtable<String, InteractiveBarDataModel>();
        m_dataRows = new ArrayList<DataRow>();
        m_barCaptions = new LinkedHashSet<DataCell>();
        m_aggrColumn = aggregationColumn;
        m_aggrMethod = aggrMethod;
        m_noOfBars = noOfBars;
    }
    
    /**
     * Constructor for class HistogramData which uses the default number of 
     * bars for binning of nuerical x columns.
     * 
     * @param tableSpec the <code>DataTableSpec</code> of the data on which
     *            this <code>HistogramData</code> object based on
     * @param xCoordLabel the label of the column for which the bars should be
     *            created
     * @param aggregationColumn column to aggregate on
     * @param aggrMethod the aggregation method
     */
    protected InteractiveHistogramDataModel(final DataTableSpec tableSpec,
            final String xCoordLabel, final String aggregationColumn,
            final AggregationMethod aggrMethod) {
        this(tableSpec, xCoordLabel, DEFAULT_NO_OF_BARS,
                aggregationColumn, aggrMethod);
    }

    /**
     * @see org.knime.dev.node.view.histogram.HistogramDataModel#addDataRow(org.knime.core.data.DataRow)
     */
    public void addDataRow(final DataRow row) {
        if (row == null) {
            throw new IllegalArgumentException("Row shouldn't be null.");
        }
        if (row.getNumCells() < m_xCoordRowIdx) {
            throw new IllegalArgumentException("Row is to short.");
        }
        DataCell cell = row.getCell(m_xCoordRowIdx);
        if (cell == null
                || (!cell.isMissing() && !cell.getType().equals(
                        m_originalXColSpec.getType()))) {
            throw new IllegalArgumentException("Cell null or column type of "
                    + "this row and defined x coordinate column not equal.");
        }
        m_dataRows.add(row);
        m_noOfNotSortedRows++;
        if (m_xIsNominal) {
            if (cell.isMissing()) {
                addRow2MissingValBar(row);
            } else {
                String caption = cell.toString();
                InteractiveBarDataModel bar = m_bars.get(caption);
                if (bar == null) {
                    bar = new InteractiveBarDataModel(caption, 
                            getAggregationColumnIdx(),
                            getAggregationMethod());
                    m_bars.put(caption, bar);
                }
                bar.addRow(row);
            }
        } // if it's a none nominal column the bars are created and filled in
        // the getBars method
        if (!cell.isMissing()) {
            // test the min value
            if (m_minVal == null) {
                m_minVal = cell;
            } else {
                if (m_rowComparator.getBasicComparator()
                        .compare(m_minVal, cell) > 0) {
                    m_minVal = cell;
                }
            }
            // test the max value
            if (m_maxVal == null) {
                m_maxVal = cell;
            } else {
                if (m_rowComparator.getBasicComparator()
                        .compare(m_maxVal, cell) < 0) {
                    m_maxVal = cell;
                }
            }
        }
    }

    /**
     * Adds the given row to the missing value bar. If the bar doesn't exists
     * yet it will be created.
     * 
     * @param row the row to add to the bar
     */
    private void addRow2MissingValBar(final DataRow row) {
        if (m_missingValueBar == null) {
            int aggrIdx = getAggregationColumnIdx();
            m_missingValueBar = new InteractiveBarDataModel(
                    MISSING_VAL_BAR_CAPTION, aggrIdx, m_aggrMethod);
        }
        m_missingValueBar.addRow(row);
    }

    /**
     * @see org.knime.dev.node.view.histogram.HistogramDataModel#getAggregationMethod()
     */
    public AggregationMethod getAggregationMethod() {
        return m_aggrMethod;
    }

    /**
     * Changes the aggregation column and method. If one of them has changed it
     * also calls the <code>calculateAggregationValues</code> method to
     * recalculate the aggregation values.
     * 
     * @param colName the name of the possibly new aggregation column
     * @param aggrMethod the possibly new aggregation method
     * @return <code>true</code> if the value has changed
     */
    protected boolean changeAggregationColumn(final String colName,
            final AggregationMethod aggrMethod) {
        if (!aggrMethod.equals(AggregationMethod.COUNT)
                && (colName == null || colName.length() < 1)) {
            throw new IllegalArgumentException("Column name not valid.");
        }
        if (aggrMethod == null) {
            throw new IllegalArgumentException("Aggregation method not valid.");
        }
        // check if something has changed if not do nothing
        if ((m_aggrColumn == null && colName != null)
                || (m_aggrColumn != null && !m_aggrColumn.equals(colName))
                || (!aggrMethod.equals(m_aggrMethod))) {
            m_aggrColumn = colName;
            m_aggrMethod = aggrMethod;
            calculateAggregationValues();
            return true;
        }
        return false;
    }

    /**
     * @return the <code>LinkedHashSet</code> with the caption of all bars as
     *         <code>DataCell</code> objects in the order they should be
     *         displayed
     */
    protected LinkedHashSet<DataCell> getOrderedBarCaptions() {
        if (m_xIsNominal) {
            return null;
        } else {
            if (m_barCaptions.size() < 1) {
                createBinnedBars(m_noOfBars);
            }
            return m_barCaptions;
        }
    }

    /**
     * @see org.knime.dev.node.view.histogram.HistogramDataModel#getNumberOfBars()
     */
    public int getNumberOfBars() {
        return m_noOfBars;
    }

    /**
     * @param noOfBars sets the number of bins
     * @return <code>true</code> if the value has changed
     */
    protected boolean setNumberOfBars(final int noOfBars) {
        if (noOfBars < 1) {
            throw new IllegalArgumentException("No valid number of bars.");
        }
        if (m_xIsNominal) {
            return false;
        }
        if (m_noOfBars != noOfBars) {
            m_noOfBars = noOfBars;
            createBinnedBars(m_noOfBars);
            return true;
        }
        return false;
    }

    /**
     * @see org.knime.dev.node.view.histogram.HistogramDataModel#getBar(java.lang.String)
     */
    public BarDataModel getBar(final String caption) {
        return getBars().get(caption);
    }

    /**
     * @return <code>Hashtable</code> with all bars of this model. The key is
     *         the caption of the bar as <code>String</code> and the value is
     *         the <code>BarDataModel</code> itself.
     */
    protected Hashtable<String, InteractiveBarDataModel> getBars() {
        if (!m_xIsNominal && (m_bars == null || m_bars.size() < 1)) {
            createBinnedBars(m_noOfBars);
        }
        return m_bars;
    }

    /**
     * @see org.knime.dev.node.view.histogram.HistogramDataModel#getMissingValueBar()
     */
    public BarDataModel getMissingValueBar() {
        if (m_missingValueBar != null && m_bars != null && m_bars.size() > 0
                && m_bars.containsKey(m_missingValueBar.getCaption())) {
            int counter = 1;
            StringBuffer newCaption = new StringBuffer(m_missingValueBar
                    .getCaption());
            newCaption.append("(");
            newCaption.append(counter++);
            newCaption.append(")");
            while (m_bars.containsKey(newCaption.toString())) {
                String caption = newCaption.toString();
                int idx = caption.lastIndexOf('(');
                if (idx < 0) {
                    newCaption = new StringBuffer(caption);
                } else {
                    newCaption = new StringBuffer(caption.substring(0, idx));
                }
                newCaption.append("(");
                newCaption.append(counter++);
                newCaption.append(")");
            }
            m_missingValueBar.setCaption(newCaption.toString());
        }
        return m_missingValueBar;
    }
    
    /**
     * @see org.knime.dev.node.view.histogram.HistogramDataModel#isNominal()
     */
    public boolean isNominal() {
        return m_xIsNominal;
    }

    /**
     * @see org.knime.dev.node.view.histogram.HistogramDataModel#getMinAggregationValue(boolean)
     */
    public double getMinAggregationValue(final boolean inclMissingBar) {
        if (Double.isNaN(m_minAggrValue)) {
            calculateAggregationValues();
        }
        if (inclMissingBar) {
            BarDataModel missingBar = getMissingValueBar();
            if (missingBar != null) {
                double currentVal = missingBar.getAggregationValue();
                // we can't handle negative values yet thats why we use the
                // absolute method
                currentVal = Math.abs(currentVal);
                if (currentVal <= m_minAggrValue
                        || Double.isNaN(m_minAggrValue)) {
                    return currentVal;
                }
            }
        }
        return m_minAggrValue;
    }

    /**
     * @see org.knime.dev.node.view.histogram.HistogramDataModel#getMaxAggregationValue(boolean)
     */
    public double getMaxAggregationValue(final boolean inclMissingBar) {
        if (Double.isNaN(m_maxAggrValue)) {
            calculateAggregationValues();
        }
        if (inclMissingBar) {
            BarDataModel missingBar = getMissingValueBar();
            if (missingBar != null) {
                double currentVal = missingBar.getAggregationValue();
                // we can't handle negative values yet thats why we use the
                // absolute method
                currentVal = Math.abs(currentVal);
                if (currentVal >= m_maxAggrValue
                        || Double.isNaN(m_maxAggrValue)) {
                    return currentVal;
                }
            }
        }
        return m_maxAggrValue;
    }

    /**
     * @see org.knime.dev.node.view.histogram.HistogramDataModel#getMaxVal()
     */
    public DataCell getMaxVal() {
        return m_maxVal;
    }

    /**
     * @see org.knime.dev.node.view.histogram.HistogramDataModel#getMinVal()
     */
    public DataCell getMinVal() {
        return m_minVal;
    }

    /**
     * @return the index of the current aggregation column or -1 if no
     *         aggregation column is defined because it's not needed like for
     *         the count method
     */
    protected int getAggregationColumnIdx() {
        if (m_aggrColumn == null) {
            return -1;
        } else {
            return m_tableSpec.findColumnIndex(getAggregationColumn());
        }
    }

    /**
     * @return the name of the aggregation column. Could be <code>null</code>
     *         if no column is necessary for the aggregation method like count!
     */
    protected String getAggregationColumn() {
        return m_aggrColumn;
    }

    /**
     * @return the index of the defined x axis column.
     */
    protected int getXColumnIdx() {
        return m_xCoordRowIdx;
    }

    /**
     * @return the <code>DataColumnSpec</code> of the defined x axis column.
     */
    protected DataColumnSpec getOriginalXColSpec() {
        return m_originalXColSpec;
    }

    /**
     * @see org.knime.dev.node.view.histogram.HistogramDataModel#toString()
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("X column: ");
        buf.append(m_originalXColSpec.getName());
        buf.append("\n");
        buf.append("Is nominal value: ");
        buf.append(isNominal());
        buf.append("\n");
        buf.append("Aggregation column: ");
        buf.append(m_aggrColumn);
        buf.append("\n");
        buf.append("Number of rows: ");
        buf.append(m_dataRows.size());
        buf.append("\t");
        buf.append("Number of bars: ");
        buf.append(getNumberOfBars());
        buf.append("\n");
        if (m_binIntervals != null) {
            buf.append("Intervals with no of members: ");
            buf.append("\n");
            for (double[] interval : m_binIntervals) {
                buf.append("|");
                buf.append(interval[0]);
                buf.append(":");
                buf.append(interval[1]);
            }
        }
        buf.append("********Bars*********");
        for (Enumeration<String> keys = m_bars.keys(); 
            keys.hasMoreElements();) {
            String caption = keys.nextElement();
            buf.append(m_bars.get(caption).toString());
        }
        return buf.toString();
    }

    // ***********Helper classes********************

    /**
     * Creates the <code>HistogramBar</code> objects for the given bin values
     * and returns the captions of all created bars.
     * 
     * @param numberOfBars the number of bars to create
     */
    private void createBinnedBars(final int numberOfBars) {
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
            maxVal = getNumericValue(maxValCell);
        }
        double minVal = 0;
        if (!minValCell.isMissing()) {
            minVal = getNumericValue(minValCell);
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
        final double binInterval = createBinInterval(maxVal, minVal, noOfBars,
                m_originalXColSpec);
        if (minVal - binInterval < 0) {
            // try to start with 0 as left border to have nicer intervals
            minVal = 0;
        }
        // increase the number of bars to include the max value
        while (minVal + (binInterval * noOfBars) < maxVal) {
            noOfBars++;
        }
        m_binIntervals = new double[noOfBars][2];
        double leftBoundary = myRoundedBorders(minVal, binInterval,
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
            double rightBoundary = myRoundedBorders(leftBoundary + binInterval,
                    binInterval, INTERVAL_DIGITS);
            m_binIntervals[i][0] = rightBoundary;
            String binCaption = createBarName(leftBoundary, rightBoundary);
            bar = m_bars.get(binCaption);
            if (bar == null) {
                bar = new InteractiveBarDataModel(binCaption, aggrColIdx, 
                        aggrMethod);
                m_bars.put(binCaption, bar);
            } else {
                // this should never happen because we clean the m_bars variable
                // before entering the for loop!
                throw new IllegalStateException("Bar with caption "
                        + binCaption + " already exists.");
            }
            m_barCaptions.add(new StringCell(binCaption));
            boolean isLower = true;
            while (isLower && currentRowIdx < rowLength) {
                DataRow row = sortedRows.get(currentRowIdx);
                DataCell cell = row.getCell(m_xCoordRowIdx);
                if (cell == null || cell.isMissing()) {
                    addRow2MissingValBar(row);
                    currentRowIdx++;
                    continue;
                }
                double val = getNumericValue(cell);
                if (val <= rightBoundary) {
                    bar.addRow(row);
                    currentRowIdx++;
                } else {
                    isLower = false;
                }
            }
            // save the number of rows for this interval
            m_binIntervals[i][1] = bar.getNumberOfRows();
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
        //set the actual number of bars
        m_noOfBars = m_bars.size();
    }

    /**
     * @param maxVal the maximum possible value
     * @param minVal the minimum possible value
     * @param noOfBars the number of bars
     * @param colSpec the specification of the column for whom we want the
     *            interval
     * @return the interval for the given min, max value and no of bars
     */
    private static double createBinInterval(final double maxVal,
            final double minVal, final int noOfBars,
            final DataColumnSpec colSpec) {
        double interval = (maxVal - minVal) / noOfBars;
        // if the column is of type integer we don't need to have an interval
        // with decimal places
        if (interval >= 1) {
            if (interval < 10) {
                interval = 
                    myRoundedInterval(interval, INTERVAL_DIGITS, colSpec);
            } else {
                // find the next higher number divided by ten.
                double divider = 10;
                double addition = 2;
                if (interval > 50 && interval <= 100) {
                    addition = 5;
                } else if (interval > 100 && interval <= 1000) {
                    addition = 10;
                } else if (interval > 1000) {
                    divider = 100;
                    addition = 100;
                    while ((interval / 10) > divider) {
                        divider *= 10;
                        addition *= 5;
                    }
                }
                /*
                 * if (interval > 1000) { addition = 100; divider = 100; }
                 */

                while (interval / divider > 1) {
                    divider += addition;
                }
                interval = divider;
            }
        } else {
            interval = myRoundedInterval(interval, INTERVAL_DIGITS, colSpec);
        }
        return interval;
    }

    /**
     * Returns the rounded value which contains the given number of decimal
     * places after the last 0 in the given increment.
     * 
     * @param doubleVal the value to round
     * @param increment the increment which defines the start index of the digit
     *            counter
     * @param noOfDigits the number of decimal places to display
     * @return the rounded value
     */
    private static double myRoundedBorders(final double doubleVal,
            final double increment, final int noOfDigits) {
        char[] incrementString = Double.toString(increment).toCharArray();
        StringBuffer decimalFormatBuf = new StringBuffer();
        boolean digitFound = false;
        int digitCounter = 0;
        int positionCounter = 0;
        for (int length = incrementString.length; positionCounter < length
                && digitCounter <= noOfDigits; positionCounter++) {
            char c = incrementString[positionCounter];
            if (c == '.') {
                decimalFormatBuf.append(".");
            } else {
                if (c != '0' || digitFound) {
                    digitFound = true;
                    digitCounter++;
                }
                if (digitCounter <= noOfDigits) {
                    decimalFormatBuf.append("#");
                }
            }
        }
        DecimalFormat df = new DecimalFormat(decimalFormatBuf.toString());
        String resultString = df.format(doubleVal);
        double result = Double.parseDouble(resultString);
        return result;
    }

    /*
     * Returns the rounded value. If the value is bigger or equal 1 it returns
     * the result of the math.ceil method otherwise it returns the rounded value
     * which contains the given number of decimal places after the last 0.
     * 
     * @param doubleVal the value to round @param noOfDigits the number of
     * decimal places we want for less then 1 values @return the rounded value
     */
    private static double myRoundedInterval(final double doubleVal,
            final int noOfDigits, final DataColumnSpec colSpec) {
        // if the value is >= 1 or an integer return an interval without decimal
        // places
        if (doubleVal >= 1 || colSpec.getType().isCompatible(IntValue.class)) {
            return Math.ceil(doubleVal);
        } else {
            // the given doubleVal is less then zero
            char[] interval = Double.toString(doubleVal).toCharArray();
            StringBuffer decimalFormatBuf = new StringBuffer();
            boolean digitFound = false;
            int digitCounter = 0;
            int positionCounter = 0;
            for (int length = interval.length; positionCounter < length
                    && digitCounter <= noOfDigits; positionCounter++) {
                char c = interval[positionCounter];
                if (c == '.') {
                    decimalFormatBuf.append(".");
                } else {
                    if (c != '0' || digitFound) {
                        digitFound = true;
                        digitCounter++;
                    }
                    if (digitCounter <= noOfDigits) {
                        decimalFormatBuf.append("#");
                    }
                }
            }
            double result = Double.parseDouble(new String(interval, 0,
                    positionCounter));
            DecimalFormat df = new DecimalFormat(decimalFormatBuf.toString());
            String resultString = df.format(result);
            result = Double.parseDouble(resultString);
            return result;
        }
    }

    /** Clears the internal used variables which holds the bar information. */
    private void clearBarInformation() {
        // remove the old bars
        m_bars.clear();
        // remove all captions
        m_barCaptions.clear();
    }

    /**
     * Checks if the given <code>DataCell</code> is a numeric cell and returns
     * the numeric value. If it's not a numeric cell it throws an
     * <code>IllegalargumentException</code>.
     * 
     * @param cell the cell to convert
     * @return the numeric value of the given cell
     */
    private double getNumericValue(final DataCell cell) {
        if (cell == null) {
            throw new IllegalArgumentException("Cell shouldn't be null.");
        }
        if (!(cell.getType().isCompatible(DoubleValue.class))) {
            throw new IllegalArgumentException("Cell type not "
                    + "compatible with numeric data type.");
        }
        double val = ((DoubleValue)cell).getDoubleValue();
        return val;
    }

    /**
     * Creates the name of the bin depending on the given boundaries.
     * 
     * @param leftBoundary the left boundary of the bin
     * @param rightBoundary the right boundary of the bin
     * @return the bin name
     */
    private String createBarName(final double leftBoundary,
            final double rightBoundary) {
        StringBuffer buf = new StringBuffer();
        if ((int)leftBoundary == leftBoundary) {
            buf.append((int)leftBoundary);
        } else {
            buf.append(Double.toString(leftBoundary));
        }
        buf.append(" - ");
        if ((int)rightBoundary == rightBoundary) {
            buf.append((int)rightBoundary);
        } else {
            buf.append(Double.toString(rightBoundary));
        }
        return buf.toString();
    }

    /**
     * Calculates the aggregation values for the current aggregation method and
     * saves the result in the min and max aggregation value variable.
     */
    private void calculateAggregationValues() {
        double minVal = Double.MAX_VALUE;
        double maxVal = Double.MIN_VALUE;
        for (BarDataModel bar : getBars().values()) {
            bar.setAggregationColumn(getAggregationColumnIdx(),
                    getAggregationMethod());
            double currentVal = bar.getAggregationValue();
            // we can't handle negative values yet thats why we use the absolute
            // method
            currentVal = Math.abs(currentVal);
            if (currentVal <= minVal || Double.isNaN(minVal)) {
                minVal = currentVal;
            }
            if (currentVal >= maxVal || Double.isNaN(maxVal)) {
                maxVal = currentVal;
            }
        }
        // calculate the aggregation value for the missing value bar as well
        if (m_missingValueBar != null) {
            m_missingValueBar.setAggregationColumn(getAggregationColumnIdx(),
                    getAggregationMethod());
        }
        m_minAggrValue = minVal;
        m_maxAggrValue = maxVal;
    }

    /**
     * @return all rows sorted by the x axis property in ascending order
     */
    private List<DataRow> getOrderedDataRows() {
        if (m_noOfNotSortedRows > 0) {
            sortDataRows();
        }
        return m_dataRows;
    }

    /**
     * Sorts the rows in the m_orderedDataRows object.
     */
    private void sortDataRows() {
        Collections.sort(m_dataRows, m_rowComparator);
        m_noOfNotSortedRows = 0;
    }

    /**
     * @see org.knime.dev.node.view.histogram.HistogramDataModel#containsMissingValueBar()
     */
    public boolean containsMissingValueBar() {
        return (getMissingValueBar() != null);
    }
}
