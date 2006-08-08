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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   31.07.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.histogram;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;

/**
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public interface HistogramDataModel {

    /**The name of the y axis if the user choose the aggregation method count.*/
    public static final String COL_NAME_COUNT = "Count";

    /** The caption of the bar which holds all missing values. */
    public static final String MISSING_VAL_BAR_CAPTION = "Missing_values";

    /**
     * Adds a new data row to this <code>HistogramData</code> object.
     * 
     * @param row the <code>DataRow</code> to add
     */
    public abstract void addDataRow(final DataRow row);

    /**
     * @return the aggregation method
     */
    public abstract AggregationMethod getAggregationMethod();

    /**
     * Returns the number of bars.
     * 
     * @return the number of bars
     */
    public abstract int getNumberOfBars();

    /**
     * @param caption the caption of the bar we want
     * @return the <code>BarDataModel</code> object with the given caption or
     *         <code>null</code> if no bar exists with the given caption
     */
    public abstract BarDataModel getBar(final String caption);

    /**
     * @return a <code>BarDataModel</code> with all missing value rows or
     *         <code>null</code> if all rows contains a value for the selected
     *         x axis
     */
    public abstract BarDataModel getMissingValueBar();

    /**
     * @return <code>true</code> if the x column is a nominal value
     */
    public abstract boolean isNominal();

    /**
     * @param inclMissingBar if set to <code>true</code> the aggregation value
     *            of the missing value bar is taken into account as well
     * @return the minimum aggregation value of all bars.
     */
    public abstract double getMinAggregationValue(final boolean inclMissingBar);

    /**
     * @param inclMissingBar if set to <code>true</code> the aggregation value
     *            of the missing value bar is taken into account as well
     * @return the maximum aggregation value of all bars
     */
    public abstract double getMaxAggregationValue(final boolean inclMissingBar);

    /**
     * @return the maximum <code>DataCell</code> of all rows for the defined x
     *         axis column
     */
    public abstract DataCell getMaxVal();

    /**
     * @return the minimum <code>DataCell</code> of all rows for the defined x
     *         axis column
     */
    public abstract DataCell getMinVal();

    /**
     * @return <code>true</code> if the a missing value bar is present
     */
    public abstract boolean containsMissingValueBar();

}
