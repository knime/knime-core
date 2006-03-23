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
 * Comparator returned by the <code>FuzzyNumberType</code> datacell type. 
 *
 * @author Michael Berthold, Konstanz University
 */
public class FuzzyNumberCellComparator extends DataCellComparator {

    /**
     * Compares to <code>DataCell</code> based in their
     * <code>FuzzyNumberValue</code>.
     * 
     * @param c1 the first datacell to compare the other with
     * @param c2 the other datacell to compare the first with
     * @return whatever a comparator is supposed to return.
     * 
     * @throws ClassCastException If one of the cells is not
     *             <code>FuzzyNumberValue</code> type.
     * @see de.unikn.knime.core.data.DataCellComparator
     *      #compareDataCells(de.unikn.knime.core.data.DataCell,
     *                        de.unikn.knime.core.data.DataCell)
     */
    public int compareDataCells(final DataCell c1, final DataCell c2) {
        FuzzyNumberValue fi1 = ((FuzzyNumberValue)c1);
        FuzzyNumberValue fi2 = ((FuzzyNumberValue)c2);
        int core = Double.compare(fi1.getCore(), fi2.getCore());
        if (core != 0) {
            return core;
        }
        int minSupp = Double.compare(fi1.getMinSupport(), fi2.getMinSupport());
        if (minSupp != 0) {
            return minSupp;
        }
        int maxSupp = Double.compare(fi1.getMaxSupport(), fi2.getMaxSupport());
        if (maxSupp != 0) {
            return maxSupp;
        }
        return 0;
    }

}
