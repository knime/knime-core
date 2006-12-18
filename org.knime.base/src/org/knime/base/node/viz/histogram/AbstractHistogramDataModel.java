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
 * 
 * History
 *   31.07.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.histogram;

import java.util.Hashtable;
import java.util.LinkedHashSet;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.StringCell;

/**
 * Abstract class which holds all information to paint a histogram bar.
 * @author Tobias Koetter, University of Konstanz
 */
public abstract class AbstractHistogramDataModel {

    /**The name of the y axis if the user choose the aggregation method count.*/
    public static final String COL_NAME_COUNT = "Count";

    /** The caption of the bar which holds all missing values. */
    public static final String MISSING_VAL_BAR_CAPTION = "Missing_values";

    /**
     * The default number of bars which get created if the createBinnedBars
     * method is called with a number smaller then 1.
     */
    protected static final int DEFAULT_NO_OF_BARS = 10;

    /**
     * Defines the maximum number of decimal places which are used in the
     * binning method.
     */
    public static final int INTERVAL_DIGITS = 2;
    
    
    /**
     * <code>Hashtable</code> with all <code>AbstractBarDataModel</code> 
     * objects as values and their caption as <code>String</code> as key.
     */
    private Hashtable<String, AbstractBarDataModel> m_bars;
    
    /**
     * <code>AbstractBarDataModel</code> object with all rows which have no 
     * value for the selected x axis column or <code>null</code> if all rows 
     * have a value.
     */
    private AbstractBarDataModel m_missingValueBar = null;


    /** The <code>DataTableSpec</code> of the original data. */
    private final DataTableSpec m_tableSpec;

    private int m_noOfBars;

    /** The index of the column of the x axis. */
    private final int m_xCoordColIdx;

    /** The <code>DataColumnSpec</code> of the x axis column. */
    private final DataColumnSpec m_originalXColSpec;

    /** Flag which indicates if the x axis property is nominal. */
    private final boolean m_xIsNominal;
    
    private final DataValueComparator m_xColComparator;
    
    /** All bar captions in the order they should be displayed. */
    private LinkedHashSet<DataCell> m_barCaptions;

    /** The max value for the x column. */
    private DataCell m_maxVal = null;

    /** The min values for the x column. */
    private DataCell m_minVal = null;

    /**
     * The name of the column to aggregate. Could be <code>null</code> if the
     * aggregation method is count!.
     */
    private String m_aggrColumn;
    
    private int m_aggrColIDx;

    /** The current aggregation method. */
    private AggregationMethod m_aggrMethod = AggregationMethod.COUNT;

    /**
     * The minimum value for the aggregation column without the missing value
     * bar.
     */
    private double m_minAggrValue = Double.NaN;

    /** The maximum for the aggregation column without the missing value bar. */
    private double m_maxAggrValue = Double.NaN;
    
    /**Constructor for class AbstractHistogramDataModel.
     * @param tableSpec the table specification
     * @param xCoordLabel the label of the x coordinate
     * @param noOfBars the number of bars to create if the selected x column 
     * isn't nominal
     * @param aggregationColumn the aggregation method could be 
     * <code>null</code>
     * @param aggrMethod the aggregation method
     */
    public AbstractHistogramDataModel(final DataTableSpec tableSpec, 
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
        m_xCoordColIdx = m_tableSpec.findColumnIndex(xCoordLabel);
        if (m_xCoordColIdx < 0) {
            throw new IllegalArgumentException("No valid column name");
        }
        m_originalXColSpec = m_tableSpec.getColumnSpec(m_xCoordColIdx);
        final DataType colType = m_originalXColSpec.getType();
        m_xColComparator = colType.getComparator();
        if (colType.isCompatible(DoubleValue.class)) {
            m_xIsNominal = false;
        } else {
            m_xIsNominal = true;
        }
        if (aggrMethod == null) {
            throw new IllegalArgumentException("Aggregation method shouldn't"
                    + " be null");
        }
        if (!aggrMethod.equals(AggregationMethod.COUNT) 
                && (aggregationColumn == null 
                        || aggregationColumn.trim().length() < 0)) {
            throw new IllegalArgumentException("Aggregation column shouldn't"
                    + " be null");
        }
        m_aggrColIDx = m_tableSpec.findColumnIndex(aggregationColumn);
        m_bars = new Hashtable<String, AbstractBarDataModel>();
        m_barCaptions = new LinkedHashSet<DataCell>();
        m_aggrColumn = aggregationColumn;
        m_aggrMethod = aggrMethod;
        m_noOfBars = noOfBars;
    }
    /**
     * Adds a new data row to this <code>HistogramData</code> object.
     * 
     * @param row the <code>DataRow</code> to add
     */
    public abstract void addDataRow(final DataRow row);


    /**
     * Checks if the cell contains a value if so it check for min and max value.
     * 
     * @param cell the cell with the value to check/add
     */
    protected void add2MinMaxValue(final DataCell cell) {
        if (!cell.isMissing()) {
            // test the min value
            if (m_minVal == null) {
                m_minVal = cell;
            } else {
                if (m_xColComparator.compare(m_minVal, cell) > 0) {
                    m_minVal = cell;
                }
            }
            // test the max value
            if (m_maxVal == null) {
                m_maxVal = cell;
            } else {
                if (m_xColComparator.compare(m_maxVal, cell) < 0) {
                    m_maxVal = cell;
                }
            }
        }
    }

    
    /**
     * @return the aggregation method
     */
    public AggregationMethod getAggregationMethod() {
        return m_aggrMethod;
    }
    
    /**
     * Changes the aggregation method.
     * 
     * @param aggrMethod the new aggregation method 
     * @return <code>true</code> if the value has changed
     */
    protected boolean changeAggregationMethod(
            final AggregationMethod aggrMethod) {
        if (aggrMethod == null) {
            throw new IllegalArgumentException("Aggregation method not valid.");
        }        
        if (!aggrMethod.equals(m_aggrMethod)) {
            m_aggrMethod = aggrMethod;
            resetAggregationvalues();
            return true;
        }
        return false;
    }
    
    /**
     * Changes the aggregation column. If the column has changed it resets
     * the aggregation values.
     * 
     * @param colName the name of the possibly new aggregation column
     * @return <code>true</code> if the value has changed
     */
    public boolean changeAggregationColumn(final String colName) {
        // check if something has changed if not do nothing
        if ((m_aggrColumn == null && colName != null)
                || (m_aggrColumn != null && !m_aggrColumn.equals(colName))) {
            m_aggrColIDx = m_tableSpec.findColumnIndex(colName);
            if (m_aggrColIDx < 0 && colName != null) {
                throw new IllegalArgumentException("Couldn't find aggregation "
                        + "column name in table specification.");
            }
            m_aggrColumn = colName;
            resetAggregationvalues();
            return true;
        }
        return false;
    }
    
    private void resetAggregationvalues() {
        m_maxAggrValue = Double.NaN;
        m_minAggrValue = Double.NaN;    
    }
    
    /**
     * Returns the number of bars.
     * 
     * @return the number of bars
     */
    public int getNumberOfBars() {
        return m_noOfBars;
    }

    /**
     * @param caption the caption of the bar we want
     * @return the <code>AbstractBarDataModel</code> object with the given 
     * caption or <code>null</code> if no bar exists with the given caption
     */
    public AbstractBarDataModel getBar(final String caption) {
        return m_bars.get(caption);
    }

    /**
     * @param bar the bar to add to the model
     */
    public void addBar(final AbstractBarDataModel bar) {
        if (bar == null) {
            return;
        }
        m_bars.put(bar.getCaption(), bar);
        m_barCaptions.add(new StringCell(bar.getCaption()));
        m_noOfBars = m_bars.size();
    }
    /**
     * @return the bars
     */
    protected Hashtable<String, AbstractBarDataModel> getBars() {
        return m_bars;
    }

    /**
     * @param bars the bars to set
     */
    protected void setBars(final Hashtable<String, AbstractBarDataModel> bars) {
        m_bars = bars;
    }
    
    /**
     * @return the barCaptions
     */
    protected LinkedHashSet<DataCell> getBarCaptions() {
        return m_barCaptions;
    }
    
    /**
     * @param barCaptions the barCaptions to set
     */
    protected void setBarCaptions(final LinkedHashSet<DataCell> barCaptions) {
        m_barCaptions = barCaptions;
    }
    
    /** Clears the internal used variables which holds the bar information. */
    protected void clearBarInformation() {
        // remove the old bars
        m_bars.clear();
        // remove all captions
        m_barCaptions.clear();
    }
    
    /**
     * @return a <code>AbstractBarDataModel</code> with all missing value rows 
     * or <code>null</code> if all rows contains a value for the selected
     * x axis
     */
    public AbstractBarDataModel getMissingValueBar() {
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
     * @param missingValueBar the missingValueBar to set
     */
    protected void setMissingValueBar(
            final AbstractBarDataModel missingValueBar) {
        m_missingValueBar = missingValueBar;
    }

    /**
     * @return <code>true</code> if the x column is a nominal value
     */
    public boolean isNominal() {
        return m_xIsNominal;
    }

    /**
     * @return the index of the defined x axis column.
     */
    protected int getXColumnIdx() {
        return m_xCoordColIdx;
    }


    /**
     * @return the index of the current aggregation column or -1 if no
     *         aggregation column is defined because it's not needed like for
     *         the count method
     */
    protected int getAggregationColumnIdx() {
        return m_aggrColIDx;
    }

    /**
     * @param inclMissingBar if set to <code>true</code> the aggregation value
     *            of the missing value bar is taken into account as well
     * @return the minimum aggregation value of all bars.
     */
    public double getMinAggregationValue(final boolean inclMissingBar) {
        if (Double.isNaN(m_minAggrValue)) {
            calculateAggregationValues();
        }
        if (inclMissingBar) {
            AbstractBarDataModel missingBar = getMissingValueBar();
            if (missingBar != null) {
                missingBar.setAggregationMethod(getAggregationMethod());
                missingBar.setAggregationColumn(getAggregationColumnIdx());
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
     * @param inclMissingBar if set to <code>true</code> the aggregation value
     *            of the missing value bar is taken into account as well
     * @return the maximum aggregation value of all bars
     */
    public double getMaxAggregationValue(final boolean inclMissingBar) {
        if (Double.isNaN(m_maxAggrValue)) {
            calculateAggregationValues();
        }
        if (inclMissingBar) {
            AbstractBarDataModel missingBar = getMissingValueBar();
            if (missingBar != null) {
                missingBar.setAggregationMethod(getAggregationMethod());
                missingBar.setAggregationColumn(getAggregationColumnIdx());
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
     * This method calculates and sets the min and max aggregation value
     * variable.
     */

    /**
     * Calculates the aggregation values for the current aggregation method and
     * saves the result in the min and max aggregation value variable.
     */
    protected void calculateAggregationValues() {
        if (m_bars == null || m_bars.size() < 1) {
            m_minAggrValue = 0;
            m_maxAggrValue = 0;
            return;
        }
        double minVal = Double.NaN;
        double maxVal = Double.NaN;
        for (AbstractBarDataModel bar : m_bars.values()) {
            bar.setAggregationMethod(getAggregationMethod());
            bar.setAggregationColumn(getAggregationColumnIdx());
            double currentVal = bar.getAggregationValue();
            // we can't handle negative values yet thats why we use the absolute
            // method
            //currentVal = Math.abs(currentVal);
            if (currentVal <= minVal || Double.isNaN(minVal)) {
                minVal = currentVal;
            }
            if (currentVal >= maxVal || Double.isNaN(maxVal)) {
                maxVal = currentVal;
            }
        }
        m_minAggrValue = minVal;
        m_maxAggrValue = maxVal;
    }
    
    /**
     * @return the maximum <code>DataCell</code> of all rows for the defined x
     *         axis column
     */
    public DataCell getMaxVal() {
        return m_maxVal;
    }

    /**
     * @param maxVal the maxVal to set
     */
    protected void setMaxVal(DataCell maxVal) {
        m_maxVal = maxVal;
    }
    
    /**
     * @return the minimum <code>DataCell</code> of all rows for the defined x
     *         axis column
     */
    public DataCell getMinVal() {
        return m_minVal;
    }

    /**
     * @param minVal the minVal to set
     */
    protected void setMinVal(DataCell minVal) {
        m_minVal = minVal;
    }
    
    /**
     * @return <code>true</code> if the a missing value bar is present
     */
    public boolean containsMissingValueBar() {
        return (getMissingValueBar() != null);
    }

    /**
     * @return <code>true</code> if the at least one empty bar is present
     */
    public boolean containsEmptyValueBars() {
        for (AbstractBarDataModel bar : m_bars.values()) {
            if (bar.isEmpty()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Sets the number of bars to create.
     * 
     * @param noOfBars the number of bars
     * @return <code>true</code> if the number has changed
     */
    public boolean setNumberOfBars(final int noOfBars) {
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
     * Creates the <code>HistogramBar</code> objects for the given bin values
     * and returns the captions of all created bars.
     * 
     * @param noOfBars the number of bars to create
     */
    protected abstract void createBinnedBars(final int noOfBars);
    
    /**
     * Returns the name of the selected aggregation column.
     * 
     * @return name of the aggregation column
     */
    public String getAggregationColumn() {
        return m_aggrColumn;
    }
    
    /**
     * Return the name of the selected x column.
     * @return name of the x column
     */
    public String getXColumn() {
        return m_originalXColSpec.getName();
    }

    /**
     * @return the column specification from the given table specification
     */
    public DataColumnSpec getOriginalXColSpec() {
        return m_originalXColSpec;
    }

    /**
     * @return the <code>LinkedHashSet</code> with the caption of all bars as
     *         <code>DataCell</code> objects in the order they should be
     *         displayed
     */
    public LinkedHashSet<DataCell> getOrderedBarCaptions() {
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
     * @return the xColComparator
     */
    protected DataValueComparator getXColComparator() {
        return m_xColComparator;
    }

    /**
     * @return the tableSpec
     */
    protected DataTableSpec getTableSpec() {
        return m_tableSpec;
    }
    
    /**
     * @see java.lang.Object#clone()
     */
    @Override
    public abstract Object clone();
}
