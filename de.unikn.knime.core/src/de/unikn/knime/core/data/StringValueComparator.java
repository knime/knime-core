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
 *   21.06.06 (bw & po): reviewed
 */
package de.unikn.knime.core.data;

/**
 * Comparator returned by the StringValue interface. 
 * 
 * @see de.unikn.knime.core.data.StringValue.StringUtilityFactory
 * @author Michael Berthold, Konstanz University
 */
public class StringValueComparator extends DataValueComparator {

    /**
     * Compares to <code>StringValue</code> based on their lexicographical 
     * order.
     * @see de.unikn.knime.core.data.DataValueComparator
     *          #compareDataValues(DataValue, DataValue)
     */
    @Override
    public int compareDataValues(final DataValue v1, final DataValue v2) {
        String s1 = ((StringValue)v1).getStringValue();
        String s2 = ((StringValue)v2).getStringValue();
        return s1.compareTo(s2);
    }

}
