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
 *   07.07.2005 (mb): created
 *   21.06.06 (bw & po): reviewed
 */
package org.knime.core.data;

import java.util.Comparator;

/**
 * The comparator used to compare two {@link DataValue} objects. An
 * instance of this class is returned by each
 * {@link DataValue.UtilityFactory} implementation.
 * Implementations can safely assume that objects passed to the
 * {@link #compareDataValues(DataValue, DataValue)} method support a type cast 
 * to that type of the <code>DataValue</code> returning this comparator.
 * 
 * <p>
 * Note: this comparator imposes orderings that are inconsistent with equals.
 * (That is, the return value zero is not equivalent to equals returning true,
 * it rather says &quot;don't know&quot;. Meaning, if two data values cannot be
 * compared (because they represent completely different value types) the
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
     * This final implementation checks and handles <code>DataCell</code>s 
     * representing missing values, before it delegates the actual comparison 
     * to the derived class, by calling <code>compareDataValues()</code>.
     * Missing cells are considered to be smaller than any other non-missing 
     * <code>DataCell</code>, and two missing cells are not comparable 
     * (returns 0).
     * 
     * @param c1 the first <code>DataCell</code> to compare the other with
     * @param c2 the other <code>DataCell</code> to compare the first with
     * @return a negative number if c1 is smaller than c2, a positive number if
     *         c1 is larger than c2, otherwise zero. Note: return value zero is
     *         not equivalent to c1 equals c2, rather they are not comparable 
     *         at all
     * 
     * @throws NullPointerException if any of the objects is <code>null</code>
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
            } else {
                // both cells are not missing
                return compareDataValues(c1, c2);
            }
        }
    }

    /**
     * Do not call this function, rather call the <code>compare</code> method, 
     * which handles missing cells.
     * The derived class should compare the two passed data values. It can be
     * safely assumed that both values are castable to the specific type of the
     * <code>DataValue</code> that returned this comparator. 
     * 
     * @param v1 the first value to compare the other with
     * @param v2 the other value to compare the first with
     * @return return -1 if v1 is smaller than v2, +1 if v1 is larger than v2,
     *         0 otherwise
     */
    protected abstract int compareDataValues(
            final DataValue v1, final DataValue v2);

}
