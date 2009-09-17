/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
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
 *   Sep 11, 2009 (morent): created
 */
package org.knime.base.node.mine.decisiontree2;

import java.util.EnumSet;
import java.util.HashMap;

/**
 * Represents the no true child strategies as defined in PMML
 * (<a>http://www.dmg.org/v4-0/TreeModel.html#NoTrueChildStrategies</a>).
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
public enum PMMLNoTrueChildStrategy {
    /** returnNullPrediction no-true child strategy. */
    RETURN_NULL_PREDICTION("returnNullPrediction"),
    /** returnLastPrediction no-true child strategy. */
    RETURN_LAST_PREDICTION("returnLastPrediction");

    private final String m_represent;
    private static final HashMap<String, PMMLNoTrueChildStrategy> LOOKUP
            = new HashMap<String, PMMLNoTrueChildStrategy>();

    private PMMLNoTrueChildStrategy(final String represent) {
        m_represent = represent;
    }

    static {
        for (PMMLNoTrueChildStrategy strategy
                : EnumSet.allOf(PMMLNoTrueChildStrategy.class)) {
            LOOKUP.put(strategy.toString(), strategy);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_represent;
    }

    /**
     * Returns the corresponding no true child strategy to the representation.
     *
     * @param represent the representation to retrieve the strategy for
     * @return the strategy
     * @see java.util.HashMap#get(java.lang.Object)
     */
    public static PMMLNoTrueChildStrategy get(final String represent) {
        if (represent == null) {
            return getDefault();
        } else {
            return LOOKUP.get(represent);
        }
    }

    /**
     * Returns the default no true child strategy.
     *
     * @return the default strategy
     */
    public static PMMLNoTrueChildStrategy getDefault() {
        return RETURN_NULL_PREDICTION;
    }
}
