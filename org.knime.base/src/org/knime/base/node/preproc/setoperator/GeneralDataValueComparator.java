/*
 * -------------------------------------------------------------------
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
 *    23.11.2007 (Tobias Koetter): created
 */

package org.knime.base.node.preproc.setoperator;

import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparator;


/**
 * Compares to {@link DataValue} using the general available
 * {@link Object#toString()} method.
 * @author Tobias Koetter, University of Konstanz
 */
public final class GeneralDataValueComparator extends DataValueComparator {

    private static DataValueComparator instance;

    private GeneralDataValueComparator() {
        //prevent instances
    }

    /**
     * @return the only living instance
     */
    public static DataValueComparator getInstance() {
        if (instance == null) {
            instance = new GeneralDataValueComparator();
        }
        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int compareDataValues(final DataValue v1, final DataValue v2) {
        if (v1 == v2) {
            return 0;
        }
        if (v1 == null) {
            return 1;
        }
        if (v2 == null) {
            return -1;
        }
        final String s1 = v1.toString();
        final String s2 = v2.toString();
        if (s1 == s2) {
            return 0;
        }
        if (s1 == null) {
            return 1;
        }
        if (s2 == null) {
            return -1;
        }
        return s1.compareTo(s2);
    }

}
