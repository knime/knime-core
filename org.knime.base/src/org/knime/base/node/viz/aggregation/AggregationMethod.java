/*
 *
 * -------------------------------------------------------------------
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
 */
package org.knime.base.node.viz.aggregation;

import java.util.ArrayList;
import java.util.List;

import org.knime.core.node.util.ButtonGroupEnumInterface;

/**
 * Enumerates all possible aggregation methods of the Histogram visualisation.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public enum AggregationMethod implements ButtonGroupEnumInterface {
    /** The average of the selected y column. */
    AVERAGE("Average", "Calculates the average"),
    /** The summary of the selected y column. */
    SUM("Sum", "Calculates the sum"),
    /** The number of rows. */
    COUNT("Row count", "Counts the number of rows"),
    /** The number of values without missing values. */
    VALUE_COUNT("Row count (w/o missing values)",
            "Counts the number of rows (excl. missing values)");


    private final String m_name;

    private final String m_tooltip;


    /**Constructor for class AggregationMethod.
     *@param name the name to display
     *@param tooltip the tool tip to display
     */
    AggregationMethod(final String name, final String tooltip) {
        m_name = name;
        m_tooltip = tooltip;
    }

    /**
     * Returns the enumeration fields as a String list of their names.
     *
     * @return the enumeration fields as a String list of their names
     */
    public static List<String> asStringList() {
        final Enum<AggregationMethod>[] values = values();
        final List<String> list = new ArrayList<String>();
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
     * @param action the action command to check
     * @return the aggregation method with the given name
     */
    public static AggregationMethod getMethod4Command(final String action) {
        if (action == null || action.length() < 1) {
            return getDefaultMethod();
        }
        for (final AggregationMethod value : AggregationMethod.values()) {
            if (value.name().equals(action)) {
                return value;
            }
        }
        return getDefaultMethod();
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
        for (final AggregationMethod value : AggregationMethod.values()) {
            if (value.name().equals(aggrMethod)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public String getActionCommand() {
        return name();
    }

    /**
     * {@inheritDoc}
     */
    public String getText() {
        return m_name;
    }

    /**
     * {@inheritDoc}
     */
    public String getToolTip() {
        return m_tooltip;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDefault() {
        return this.equals(AggregationMethod.getDefaultMethod());
    }
}
