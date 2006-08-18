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

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;

/**
 * Interface of a histogram bar.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public abstract class AbstractBarDataModel {
    /**Used to format the aggregation value for the aggregation method count.*/
    private static final DecimalFormat AGGREGATION_LABEL_FORMATER_COUNT = 
        new DecimalFormat("#");

    /** The caption associated with this bar. */
    private String m_caption;

    private final int m_xCoordColIdx;

    /** The method which is used to calculate the aggregation value. */
    private AggregationMethod m_aggrMethod;

    /** The index of the aggregation column in the data rows. */
    private int m_aggrColIdx;

    /**Constructor for class AbstractBarDataModel.
     * 
     * @param caption the caption of the bar
     * @param xCoordColIDx the column index of the x coordinate
     * @param aggrColIDx the column index of the aggregation column
     * @param aggrMethod the aggregation method
     */
    protected AbstractBarDataModel(final String caption, final int xCoordColIDx,
            final int aggrColIDx, final AggregationMethod aggrMethod) {
        if (caption == null) {
            throw new IllegalArgumentException("Caption shouldn't be null.");
        }
        if (aggrMethod == null) {
            throw new IllegalArgumentException("Aggregation method shouldn't "
                    + "be null.");
        }
        m_caption = caption;
        m_xCoordColIdx = xCoordColIDx;
        m_aggrMethod = aggrMethod;
        m_aggrColIdx = aggrColIDx;
    }

    /**
     * @return the caption of the x axis of this bar
     */
    public String getCaption() {
        return this.m_caption;
    }

    /**
     * @return the aggregation value label of this bar
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
     * The number of rows in this bar.
     * 
     * @return number of rows in this bar
     */
    public abstract int getNumberOfRows();

    /**
     * @return <code>true</code> if this bar contains no rows
     */
    public boolean isEmpty() {
        return (getNumberOfRows() < 1);
    }

    /**
     * @return the highest value of this bar for the current aggregation method
     */
    public abstract double getAggregationValue();

    /**
     * @return the aggrColIdx
     */
    protected int getAggregationColIdx() {
        return m_aggrColIdx;
    }
    
    /**
     * @param idx the new aggregation column index
     */
    protected void setAggregationColumn(final int idx) {
        m_aggrColIdx = idx;
    }

    /**
     * @return the xCoordColIdx
     */
    protected int getXCoordColIdx() {
        return m_xCoordColIdx;
    }

    /**
     * @return the <code>AggregationMethod</code> which is used in this bar
     */
    public AggregationMethod getAggregationMethod() {
        return m_aggrMethod;
    }

    /**
     * @param aggrMethod the new aggregation method
     */
    public void setAggregationMethod(final AggregationMethod aggrMethod) {
        if (aggrMethod == null) {
            throw new IllegalArgumentException("Aggregation method shouldn't"
                    + " be null.");
        }
        m_aggrMethod = aggrMethod;
    }

    /**
     * @return the row key of all rows which belong to this bar
     */
    public abstract Set<DataCell> getRowKeys();

    /**
     * @param caption the new caption of this bar
     */
    public void setCaption(final String caption) {
        if (caption == null) {
            throw new IllegalArgumentException("Caption shouldn't be null.");
        }
        m_caption = caption;
    }

    /**
     * Returns a <code>Hashtable</code> with a <code>ColorAttr</code> object
     * as key and a <code>Collection</code> of the associated 
     * <code>DataRow</code> objects.
     * @param tableSpec the table specification which contains the color data
     * @return <code>Hashtable</code> with a <code>ColorAttr</code> object
     * as key and a <code>Collection</code> of the associated 
     * <code>DataRow</code> objects
     */
    public abstract Hashtable<ColorAttr, Collection<RowKey>> 
    createColorInformation(final DataTableSpec tableSpec);

    /**
     * Returns the rounded value. It returns the rounded value which contains
     * the given number of digits after the last 0.
     * 
     * @param doubleVal the value to round
     * @param noOfDigits the number of digits we want for less then 1 values
     * @return the rounded value
     */
    protected static String myRoundedLabel(final double doubleVal,
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
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("Caption: ");
        buf.append(getCaption());
        buf.append("\n");
        buf.append("Aggregation method: ");
        buf.append(getAggregationMethod());
        buf.append("\n");
        buf.append("Aggregation Value: ");
        buf.append(getAggregationValue());
        buf.append("\n");
        buf.append("Number of rows: ");
        buf.append(getNumberOfRows());
        return buf.toString();
    }
}
