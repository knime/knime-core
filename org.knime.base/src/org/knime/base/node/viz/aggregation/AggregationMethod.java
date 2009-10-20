/*
 *
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
