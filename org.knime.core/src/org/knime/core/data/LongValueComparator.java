/* ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 *
 * History
 *   21.01.2009 (meinl): created
 */
package org.knime.core.data;

/**
 * Comparator returned by the {@link LongValue} interface.
 *
 * @see org.knime.core.data.LongValue#UTILITY
 * @see org.knime.core.data.LongValue.LongUtilityFactory
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class LongValueComparator extends DataValueComparator {
    /**
     * {@inheritDoc}
     */
    @Override
    protected int compareDataValues(final DataValue v1, final DataValue v2) {
        long l1 = ((LongValue)v1).getLongValue();
        long l2 = ((LongValue)v2).getLongValue();
        if (l1 < l2) {
            return -1;
        }
        if (l1 > l2) {
            return 1;
        }
        return 0;
    }
}
