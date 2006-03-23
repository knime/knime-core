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

/**
 * Comparator returned by the StringType datacell type. 
 * 
 * @author Michael Berthold, Konstanz University
 */
public class StringCellComparator extends DataCellComparator {

    /**
     * Compares to <code>DataCell</code> based in their
     * <code>StringValue</code>.
     * 
     * @param c1 Cell 1.
     * @param c2 Cell 2.
     * @return the value <code>0</code> if the two strings are equal; a value
     *         less than <code>0</code> if the string value of c1 is
     *         lexicographically less than the string of c2; and a value greater
     *         than <code>0</code> if c1's string is lexicographically greater
     *         than the string of c2.
     * @throws ClassCastException If one of the cells is not
     *             <code>StringValue</code> type.
     * 
     * @see String#compareTo(java.lang.String)
     * @see de.unikn.knime.core.data.DataCellComparator
     *          #compareDataCells(de.unikn.knime.core.data.DataCell,
     *                            de.unikn.knime.core.data.DataCell)
     */
    public int compareDataCells(final DataCell c1, final DataCell c2) {
        String s1 = ((StringValue)c1).getStringValue();
        String s2 = ((StringValue)c2).getStringValue();
        return s1.compareTo(s2);
    }

}
