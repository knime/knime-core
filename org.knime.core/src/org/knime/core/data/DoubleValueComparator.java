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
 * History
 *   07.07.2005 (mb): created
 *   21.06.06 (bw & po): reviewed.
 */
package org.knime.core.data;

/**
 * Comparator returned by the {@link DoubleValue} interface.
 * 
 * @see org.knime.core.data.DoubleValue#UTILITY
 * @author Michael Berthold, University of Konstanz
 */
public class DoubleValueComparator extends DataValueComparator {

    /**
     * Compares two {@link DoubleValue}s based on their generic 
     * <code>double</code>.
     * 
     * @param v1 the first {@link DoubleValue} to compare the other with
     * @param v2 the other {@link DoubleValue} to compare the first with
     * @return what a comparator is supposed to return.
     * 
     * @throws ClassCastException If one of the arguments is 
     *          not <code>DoubleValue</code> type.
     * @throws NullPointerException If any argument is <code>null</code>.
     * 
     * @see java.lang.Double#compare(double,double)
     * @see org.knime.core.data.DataValueComparator
     *      #compareDataValues(DataValue, DataValue)
     */
    @Override
    public int compareDataValues(final DataValue v1, final DataValue v2) {
        double d1 = ((DoubleValue)v1).getDoubleValue();
        double d2 = ((DoubleValue)v2).getDoubleValue();
        return Double.compare(d1, d2);
    }

}
