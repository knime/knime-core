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
 * Comparator returned by the <code>DoubleType</code> datacell type. 
 * 
 * @author Michael Berthold, Konstanz University
 */
public class DoubleCellComparator extends DataCellComparator {

    /**
     * Compares to <code>DataCell</code> based on their
     * <code>DoubleValue</code>.
     * 
     * @param c1 the first datacell to compare the other with
     * @param c2 the other datacell to compare the first with
     * @return what a comparator is supposed to return.
     * 
     * @throws ClassCastException If one of the cells is not
     *             <code>DoubleValue</code> type.
     * 
     * @see java.lang.Double#compare(double,double)
     * @see de.unikn.knime.core.data.DataCellComparator
     *      #compareDataCells(de.unikn.knime.core.data.DataCell,
     *                        de.unikn.knime.core.data.DataCell)
     */
    public int compareDataCells(final DataCell c1, final DataCell c2) {
        double d1 = ((DoubleValue)c1).getDoubleValue();
        double d2 = ((DoubleValue)c2).getDoubleValue();
        return Double.compare(d1, d2);
    }

}
