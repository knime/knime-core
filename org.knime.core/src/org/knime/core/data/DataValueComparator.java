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
 *   07.07.2005 (mb): created
 *   21.06.06 (bw & po): reviewed
 */
package org.knime.core.data;

import java.util.Comparator;

/**
 * The comparator used to compare two <code>DataValue</code> objects. An
 * instance of this class is returned by each
 * <code>DataValue#UtilityFactory</code> implementation to compare two values.
 * Implementations can safely assume that objects passed to the
 * <code>compareDataValue</code> method are typecastable to that type of that
 * DataValue returning this comparator.
 * 
 * <p>
 * Note: this comparator imposes orderings that are inconsistent with equals.
 * (That is, the return value zero is not equivalent to equals returning true,
 * it rather says &quot;don't know&quot;. Meaning, if two data values cannot be
 * compared (because they represent completly different value types) the
 * comparator will return zero - and equals will return false.)
 * 
 * @see org.knime.core.data.DataValue.UtilityFactory#getComparator()
 * @author Michael Berthold, University of Konstanz
 */
public abstract class DataValueComparator implements Comparator<DataCell> {

    /**
     * Create a new general <code>DataValue</code> comparator.
     */
    protected DataValueComparator() {

    }

    /**
     * The final implementation checks and handles data cells representing
     * missing values, before it delegates the actual comparison to the derived
     * class, by calling <code>compareDataValues()</code>.
     * 
     * @param c1 the first datacell to compare the other with
     * @param c2 the other datacell to compare the first with
     * @return a negative number if o1 is smaller than o2, a positive number if
     *         o1 is larger than o2, otherwise zero. Note: return value zero is
     *         not equivalent to o1 equals o2.
     * 
     * @throws ClassCastException If <code>o1</code> or <code>o2</code> not
     *             of type <code>DataCell</code>.
     * @throws NullPointerException If one or the other object is
     *             <code>null</code>.
     * 
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     * @see #compareDataValues(DataValue, DataValue)
     */
    public final int compare(final DataCell c1, final DataCell c2) {
        if (c1.isMissing()) {
            if (c2.isMissing()) {
                return 0;
            } else {
                return -1;
            }
        } else {
            if (c2.isMissing()) {
                return +1;
            }
            // both cells are not missing
            return compareDataValues(c1, c2);
        }
    }

    /**
     * Do not call this function. Rather call the <code>compare</code> method.
     * The derived class should compare the two passed data values. It can be
     * safely assumed that both values are castable to the specific type of the
     * <code>DataValue</code> that returned this comparator. 
     * 
     * @param v1 the one value to compare the other with.
     * @param v2 the other value to compare the one with.
     * @return return -1 if v1 is smaller than v2, +1 if v1 is larger than v2,
     *         0 otherwise.
     */
    protected abstract int compareDataValues(
            final DataValue v1, final DataValue v2);

}
