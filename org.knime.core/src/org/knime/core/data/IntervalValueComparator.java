/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 */
package org.knime.core.data;

/**
 * Comparator returned by the {@link IntervalValue} datacell type.
 *  
 * @see org.knime.core.data.IntervalValue.IntervalUtilityFactory
 * @author Thomas Gabriel, University of Konstanz
 */
public class IntervalValueComparator extends DataValueComparator {

    /**
     * The compare function called by the abstract {@link DataValueComparator}
     * class. The comparison is based on the center of 
     * <code>IntervalValue.getMin()</code> and 
     * <code>IntervalValue.getMax()</code> methods' values.
     * Do not call this method directly. Use
     * {@link DataValueComparator#compare(DataCell, DataCell)} instead.
     * 
     * @see org.knime.core.data.DataValueComparator
     *      #compareDataValues(DataValue, DataValue)
     */
    @Override
    protected int compareDataValues(final DataValue v1, final DataValue v2) {

        IntervalValue f1 = (IntervalValue) v1;
        IntervalValue f2 = (IntervalValue) v2;

        // compute mean
        double d1 = (f1.getRightBound() + f1.getLeftBound()) / 2.0;
        double d2 = (f2.getRightBound() + f2.getLeftBound()) / 2.0;
        // perform actual comparison
        if (d1 > d2) {
            return +1;
        }
        if (d1 == d2) {
            return 0;
        }
        return -1;
    }

}
