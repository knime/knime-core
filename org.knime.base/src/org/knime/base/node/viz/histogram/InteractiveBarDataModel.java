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
package org.knime.base.node.viz.histogram;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;


/**
 * Class which holds all information of a bar in a histogram view and provides
 * methods to retrieve information needed to present this bar like number of
 * members in total or the value of the aggregation.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
final class InteractiveBarDataModel implements BarDataModel {
    /**Used to format the aggregation value for the aggregation method count.*/
    private static final DecimalFormat AGGREGATION_LABEL_FORMATER_COUNT = 
        new DecimalFormat("#");

    /** The caption associated with this bar. */
    private String m_caption;

    /**
     * <code>Hashtable</code> with the <code>RowKey</code> as
     * <code>DataCell</code> as key and the <code>DataRow</code> itself as
     * value.
     */
    private Hashtable<DataCell, DataRow> m_rows = 
        new Hashtable<DataCell, DataRow>();

    /** The method which is used to calculate the aggregation value. */
    private AggregationMethod m_aggrMethod;

    /** The index of the aggregation column in the data rows. */
    private int m_aggrColIdx;

    /**
     * The upper bound for the current set aggregation method and aggregation
     * method.
     */
    private double m_aggrValue = Double.NaN;

    /**
     * Constructor for class HistogramBar.
     * 
     * @param caption the caption of this bar
     * @param aggrColIDx the index of the aggregation column
     * @param aggrMethod the aggregation method
     */
    protected InteractiveBarDataModel(final String caption, final int aggrColIDx,
            final AggregationMethod aggrMethod) {
        if (caption == null) {
            throw new IllegalArgumentException("Caption shouldn't be null.");
        }
        if (aggrMethod == null) {
            throw new IllegalArgumentException("Aggregation method shouldn't "
                    + "be null.");
        }
        m_caption = caption;
        m_aggrMethod = aggrMethod;
        m_aggrColIdx = aggrColIDx;
    }

    /**
     * Adds a new row to this bar.
     * 
     * @param row the <code>DataRow</code> itself
     */
    protected void addRow(final DataRow row) {
        m_rows.put(row.getKey().getId(), row);
    }

    /**
     * @see org.knime.base.node.viz.histogram.BarDataModel#getCaption()
     */
    public String getCaption() {
        return m_caption;
    }

    /**
     * @see org.knime.base.node.viz.histogram.BarDataModel#getLabel()
     */
    public String getLabel() {
        final double aggrVal = getAggregationValue();
        // return Double.toString(aggrVal);
        AggregationMethod method = getAggregationMethod();
        if (method.equals(AggregationMethod.COUNT)) {
            return AGGREGATION_LABEL_FORMATER_COUNT.format(aggrVal);
        } else {
            return myRoundedLabel(aggrVal, 2);
        }
    }

    /**
     * Returns the rounded value. It returns the rounded value which contains
     * the given number of digits after the last 0.
     * 
     * @param doubleVal the value to round
     * @param noOfDigits the number of digits we want for less then 1 values
     * @return the rounded value
     */
    private static String myRoundedLabel(final double doubleVal,
            final int noOfDigits) {
        // the given doubleVal is less then zero
        char[] interval = Double.toString(doubleVal).toCharArray();
        StringBuffer decimalFormatBuf = new StringBuffer();
        boolean digitFound = false;
        int digitCounter = 0;
        int positionCounter = 0;
        boolean dotFound = false;
        for (int length = interval.length; positionCounter < length
                && digitCounter <= noOfDigits; positionCounter++) {
            char c = interval[positionCounter];
            if (c == '.') {
                decimalFormatBuf.append(".");
                dotFound = true;
            } else {
                if (c != '0' || digitFound) {
                    digitFound = true;
                    if (dotFound) {
                        digitCounter++;
                    }
                }
                if (digitCounter <= noOfDigits) {
                    decimalFormatBuf.append("#");
                }
            }
        }
        DecimalFormat df = new DecimalFormat(decimalFormatBuf.toString());
        String resultString = df.format(doubleVal);
        return resultString;
    }

    /**
     * @see org.knime.base.node.viz.histogram.BarDataModel#getNumberOfRows()
     */
    public int getNumberOfRows() {
        return m_rows.size();
    }

    /**
     * @see org.knime.base.node.viz.histogram.BarDataModel#isEmpty()
     */
    public boolean isEmpty() {
        return (getNumberOfRows() < 1);
    }

    /**
     * @see org.knime.base.node.viz.histogram.BarDataModel#setAggregationColumn(int, AggregationMethod)
     */
    public void setAggregationColumn(final int aggrColIdx,
            final AggregationMethod aggrMethod) {
        if (aggrMethod == null) {
            throw new IllegalArgumentException("Aggregation method shouldn't"
                    + " be null.");
        }
        if (m_aggrMethod.equals(aggrMethod)
                && (m_aggrColIdx == aggrColIdx || aggrColIdx < 0)) {
            return;
        } else {
            m_aggrMethod = aggrMethod;
            m_aggrColIdx = aggrColIdx;
            // check if the column type and aggregation method are compatible
            if (!m_aggrMethod.equals(AggregationMethod.COUNT)) {
                Collection<DataRow> rows = m_rows.values();
                if (rows != null && rows.size() > 0) {
                    DataRow row = rows.iterator().next();
                    DataCell cell = row.getCell(m_aggrColIdx);
                    if (!cell.getType().isCompatible(DoubleValue.class)) {
                        throw new IllegalArgumentException("Selected"
                                + " aggregation column and method aren't"
                                + " compatible.");
                    }
                }
            }
            // This forces the recalculation of the aggregation value in the
            // getAggregationValue method.
            m_aggrValue = Double.NaN;
            return;
        }
    }

    /**
     * @see org.knime.base.node.viz.histogram.BarDataModel#getAggregationValue()
     */
    public double getAggregationValue() {
        if (Double.isNaN(m_aggrValue)) {
            calculateAggregationValue();
        }
        return m_aggrValue;
    }

    /**
     * @see org.knime.base.node.viz.histogram.BarDataModel#getAggregationMethod()
     */
    public AggregationMethod getAggregationMethod() {
        return m_aggrMethod;
    }

    /**
     * @see org.knime.base.node.viz.histogram.BarDataModel#getRowKeys()
     */
    public Set<DataCell> getRowKeys() {
        return m_rows.keySet();
    }

    /**
     * Calculates the aggregation value depending on the defined
     * <code>AggregationMethod</code>.
     */
    private void calculateAggregationValue() {
        if (m_aggrMethod.equals(AggregationMethod.COUNT)) {
            m_aggrValue = getNumberOfRows();
        } else {
            // calculate the sum of all cells of the aggregation column first
            // because it is needed for both methods!
            double aggrSum = 0.0;
            for (DataRow row : m_rows.values()) {
                final DataCell cell = row.getCell(m_aggrColIdx);
                if (!cell.isMissing()) {
                    aggrSum += ((DoubleValue)cell).getDoubleValue();
                }
            }
            if (m_aggrMethod.equals(AggregationMethod.SUMMARY)) {
                m_aggrValue = aggrSum;
            } else if (m_aggrMethod.equals(AggregationMethod.AVERAGE)) {
                if (getNumberOfRows() == 0) {
                    m_aggrValue = 0;
                } else {
                    m_aggrValue = aggrSum / getNumberOfRows();
                }
            } else {
                // this should never happen because we check the aggregation
                // method in the set method!
                throw new IllegalArgumentException(
                        "No valid aggregation method");
            }
        }
    }


    /**
     * @see org.knime.base.node.viz.histogram.BarDataModel#setCaption(String)
     */
    public void setCaption(final String caption) {
        if (caption == null) {
            throw new IllegalArgumentException("Caption shouldn't be null.");
        }
        m_caption = caption;
    }
    
    /**
     * @see org.knime.base.node.viz.histogram.BarDataModel#createColorInformation(org.knime.core.data.DataTableSpec)
     */
    public Hashtable<ColorAttr, Collection<RowKey>> 
        createColorInformation(final DataTableSpec tableSpec) {
        Hashtable<ColorAttr, Collection<RowKey>> rowsByColor = 
            new Hashtable<ColorAttr, Collection<RowKey>>();
        for (DataRow row : m_rows.values()) {
            ColorAttr colAtr = tableSpec.getRowColor(row);
            Collection<RowKey> colRows = rowsByColor.get(colAtr);
            if (colRows == null) {
                colRows = new ArrayList<RowKey>();
                rowsByColor.put(colAtr, colRows);
            }
            colRows.add(row.getKey());
        }
        return rowsByColor;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("Caption: ");
        buf.append(m_caption);
        buf.append("\n");
        buf.append("Aggregation method: ");
        buf.append(m_aggrMethod);
        buf.append("\n");
        buf.append("Aggregation Value: ");
        buf.append(m_aggrValue);
        buf.append("\n");
        buf.append("Number of rows: ");
        buf.append(m_rows.size());

        return buf.toString();
    }
}
