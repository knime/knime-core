/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   07.07.2005 (mb): created
 */
package de.unikn.knime.core.data;

import java.util.Comparator;

/**
 * The comparator used to compare two <code>DataCell</code> objects. An
 * instance of this class is returned by each <code>DataType</code> to compare
 * two cells of its native type. Implementations can safely assume that objects
 * passed to the compareDataCell method are typecastable to that native type of
 * that DataType (that returned the comparator object).
 * 
 * Note: this comparator imposes orderings that are inconsistent with equals.
 * 
 * (That is, the return value zero is not equivalent to equals returning true,
 * it rather says "don't know". Meaning, if two data cells cannot be compared
 * (because they represent completly different value types) the comparator will
 * return zero - and equals will return false.)
 * 
 * @author Michael Berthold, Konstanz University
 */
public abstract class DataCellComparator implements Comparator<DataCell> {

    /**
     * Create a new general <code>DataCell</code> comparator.
     */
    protected DataCellComparator() {

    }

    /**
     * The final implementation checks and handles data cells representing
     * missing values, before it delegates the actual comparison to the derived
     * class, by calling <code>compareDataCells()</code>.
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
     * @see #compareDataCells(DataCell, DataCell)
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
            return compareDataCells(c1, c2);
        }
    }

    /**
     * Do not call this function. Rather call the <code>compare</code> method.
     * The derived class should compare the two passed data cells. It can be
     * safely assumed that both cells are castable to the native type of the
     * <code>DataType</code> that returned this comparator. Also, both cells
     * are not representing missing values.
     * 
     * @param c1 the one cell to compare the other with
     * @param c2 the other cell to compare the one with
     * @return return -1 if c1 is smaller than c2, +1 if c1 is larger than c2,
     *         0 otherwise.
     */
    protected abstract int compareDataCells(final DataCell c1, 
            final DataCell c2);

}
