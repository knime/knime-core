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
 *    19.10.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.aggregation.util;

import java.util.Comparator;

import org.knime.base.node.viz.aggregation.AggregationValModel;


/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
@SuppressWarnings("unchecked")
public class AggrValModelComparator implements Comparator
<AggregationValModel> {

    private boolean m_sortNumerical = true;

    private int m_lower = -1;

    private int m_upper = 1;


    /**Constructor for class AggrValModelComparator.
     * @param ascending <code>true</code> if the sections should be sorted in
     * ascending order
     */
    public AggrValModelComparator(final boolean sortNumerical,
            final boolean ascending) {
        m_sortNumerical = sortNumerical;
        setSortAscending(ascending);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public int compare(
            final AggregationValModel o1,
            final AggregationValModel o2) {
        if (m_sortNumerical) {
            double v1 = 0;
            double v2 = 0;
            try {
                v1 = Double.parseDouble(o1.getName());
                v2 = Double.parseDouble(o2.getName());
                final int result = Double.compare(v1, v2);
                if (result < 0) {
                    return m_lower;
                }
                if (result > 0) {
                    return m_upper;
                }
                return 0;
            } catch (final NumberFormatException e) {
                //if the number conversion failed sort it by name
            }
        }
        if (o1 == null && o2 != null) {
            return m_lower;
        }
        if (o1 != null && o2 == null) {
            return m_upper;
        }
        if (o1 == null && o2 == null) {
            return 0;
        }
        final int result = o1.getName().compareTo(o2.getName());
        if (result < 0) {
            return m_lower;
        }
        if (result > 0) {
            return m_upper;
        }
        return 0;
    }

    /**
     * @param ascending <code>true</code> if the sections should be sorted in
     * ascending order
     */
    public void setSortAscending(final boolean ascending) {
        if (ascending) {
            m_lower = -1;
            m_upper = 1;
        } else {
            m_lower = 1;
            m_upper = -1;
        }
    }

    /**
     * @param sortNumerical <code>true</code> if the name is a number
     */
    public void setBinNumerical(final boolean sortNumerical) {
        m_sortNumerical = sortNumerical;
    }
}
