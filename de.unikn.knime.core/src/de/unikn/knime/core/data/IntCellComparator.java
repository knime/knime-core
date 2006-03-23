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
 * Comparator returned by the IntType datacell type. 
 *
 * @author Michael Berthold, Konstanz University
 */
public class IntCellComparator extends DataCellComparator {

    /**
     * Compares to <code>DataCell</code> based in their <code>IntValue</code>.
     * 
     * @param c1 the first datacell to compare the other with
     * @param c2 the other datacell to compare the first with
     * @return whatever a comparator is supposed to return.

     * @throws ClassCastException If one of the cells is not
     *             <code>IntValue</code> type.
     * 
     * @see de.unikn.knime.core.data.DataCellComparator
     *          #compareDataCells(de.unikn.knime.core.data.DataCell,
     *                            de.unikn.knime.core.data.DataCell)
     */
    public int compareDataCells(final DataCell c1, final DataCell c2) {
        int i1 = ((IntValue)c1).getIntValue();
        int i2 = ((IntValue)c2).getIntValue();
        if (i1 < i2) {
            return -1;
        }
        if (i1 > i2) {
            return 1;
        }
        return 0;
    }

}
