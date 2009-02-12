/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 * Comparator returned by the {@link FuzzyNumberValue} datacell type. 
 *
 * @see org.knime.core.data.FuzzyNumberValue.FuzzyNumberUtilityFactory
 * @author Michael Berthold, University of Konstanz
 */
public class FuzzyNumberValueComparator extends DataValueComparator {

    /**
     * Compares two {@link FuzzyNumberValue}s based in their
     * core value, min support value, or their max support value (in this 
     * order if the comparison returns 0).
     * @see org.knime.core.data.DataValueComparator
     *      #compareDataValues(DataValue, DataValue)
     */
    @Override
    public int compareDataValues(final DataValue v1, final DataValue v2) {
        FuzzyNumberValue fi1 = ((FuzzyNumberValue)v1);
        FuzzyNumberValue fi2 = ((FuzzyNumberValue)v2);
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
