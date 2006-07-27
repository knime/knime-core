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
package de.unikn.knime.core.data;

/**
 * Comparator returned by the IntValue interface. 
 *
 * @see de.unikn.knime.core.data.IntValue#UTILITY
 * @see de.unikn.knime.core.data.IntValue.IntUtilityFactory
 * @author Michael Berthold, Konstanz University
 */
public class IntValueComparator extends DataValueComparator {

    /**
     * Compares two <code>IntValue</code>s based in their <code>int</code>.
     * @see de.unikn.knime.core.data.DataValueComparator
     *          #compareDataValues(DataValue, DataValue)
     */
    @Override
    public int compareDataValues(final DataValue v1, final DataValue v2) {
        int i1 = ((IntValue)v1).getIntValue();
        int i2 = ((IntValue)v2).getIntValue();
        if (i1 < i2) {
            return -1;
        }
        if (i1 > i2) {
            return 1;
        }
        return 0;
    }

}
