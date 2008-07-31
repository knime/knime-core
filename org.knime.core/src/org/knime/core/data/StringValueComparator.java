/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
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

/**
 * Comparator returned by the StringValue interface. 
 * 
 * @see org.knime.core.data.StringValue.StringUtilityFactory
 * @author Michael Berthold, University of Konstanz
 */
public class StringValueComparator extends DataValueComparator {

    /**
     * Compares two {@link StringValue}s based on their lexicographical 
     * order.
     * @see org.knime.core.data.DataValueComparator
     *          #compareDataValues(DataValue, DataValue)
     */
    @Override
    public int compareDataValues(final DataValue v1, final DataValue v2) {
        String s1 = ((StringValue)v1).getStringValue();
        String s2 = ((StringValue)v2).getStringValue();
        return s1.compareTo(s2);
    }

}
