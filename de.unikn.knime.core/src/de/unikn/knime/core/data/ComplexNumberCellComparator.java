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
 *   23.03.2006 (cebron): created
 */
package de.unikn.knime.core.data;

/**
 * Comparator returned by the <code>ComplexNumberType</code> datacell type. 
 * 
 * @author ciobaca, Konstanz University
 */
public class ComplexNumberCellComparator extends DataCellComparator {

    /**
     * Compares to <code>DataCell</code> based on their
     * <code>ComplexNumberValue</code>.
     * 
     * @param c1 the first datacell to compare the other with
     * @param c2 the other datacell to compare the first with
     * @return what a comparator is supposed to return.
     * 
     * @throws ClassCastException If one of the cells is not
     *             <code>ComplexNumberValue</code> type.
     * 
     * @see java.lang.Double#compare(double,double)
     * @see de.unikn.knime.core.data.DataCellComparator
     *      #compareDataCells(de.unikn.knime.core.data.DataCell,
     *                        de.unikn.knime.core.data.DataCell)
     */
    public int compareDataCells(final DataCell c1, final DataCell c2) {
        double real1 = ((ComplexNumberValue)c1).getRealValue();
        double real2 = ((ComplexNumberValue)c2).getRealValue();
        return Double.compare(real1, real2);
    }

}

