/* 
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 */
package org.knime.base.node.viz.histogram;

import java.util.ArrayList;
import java.util.List;

/**
 * Enumerates all possible aggregation methods of the Histogram visualisation.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public enum AggregationMethod {
    /** The average of the selected y column. */
    AVERAGE,
    /** The summary of the selected y column. */
    SUM,
    /** The number of rows. */
    COUNT;

    /**
     * Returns the enumeration fields as a String list of their names.
     * 
     * @return the enumeration fields as a String list of their names
     */
    public static List<String> asStringList() {
        Enum<AggregationMethod>[] values = values();
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < values.length; i++) {
            list.add(values[i].name());
        }
        return list;
    }

    /**
     * Returns the aggregation method for the given name. If the name is
     * <code>null</code> or has length zero the method returns the default
     * aggregation method.
     * 
     * @param name the name to check
     * @return the aggregation method with the given name
     */
    public static AggregationMethod getMethod4String(final String name) {
        if (name == null || name.length() < 1) {
            return getDefaultMethod();
        }
        for (AggregationMethod value : AggregationMethod.values()) {
            if (value.name().equals(name)) {
                return value;
            }
        }
        return AggregationMethod.COUNT;
    }

    /**
     * @return the default aggregation method
     */
    public static AggregationMethod getDefaultMethod() {
        return AggregationMethod.COUNT;
    }

    /**
     * @param aggrMethod the name of method to check
     * @return <code>true</code> if it's a valid aggregation method otherwise
     *         it returns <code>false</code>.
     */
    public static boolean valid(final String aggrMethod) {
        for (AggregationMethod value : AggregationMethod.values()) {
            if (value.name().equals(aggrMethod)) {
                return true;
            }
        }
        return false;
    }
}
