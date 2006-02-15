/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
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
 * Comparator returned by the <code>FuzzyIntervalType</code> datacell type.
 *  
 * @author Michael Berthold, Konstanz University
 */
public class FuzzyIntervalCellComparator extends DataCellComparator {

    /**
     * The compare function called by the abstract DataCellComparator class.
     * The comparison is based on the border values returned by
     * <code>FuzzyIntervalCell.get{Min,Max}{Core,Support}()</code> methods.
     * Note that comparing fuzzy intervals is far from trivial - we base the
     * comparison used here on the center of gravities of the fuzzy sets. Do not
     * call this method directly. Use <code>DataCell.compareTo</code> instead.
     * 
     * @see de.unikn.knime.core.data.DataCellComparator
     *      #compareDataCells(de.unikn.knime.core.data.DataCell,
     *      de.unikn.knime.core.data.DataCell)
     */
    protected int compareDataCells(final DataCell c1, final DataCell c2) {

        FuzzyIntervalValue f1 = (FuzzyIntervalValue)c1;
        FuzzyIntervalValue f2 = (FuzzyIntervalValue)c2;

        // compute center of gravities of both trapezoid
        double f1CoG = f1.getCenterOfGravity();
        double f2CoG = f2.getCenterOfGravity();
        // perform actual comparison
        if (f1CoG > f2CoG) {
            return +1;
        }
        if (f1CoG == f2CoG) {
            return 0;
        }
        return -1;
    }

}
