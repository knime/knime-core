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
 * Represents the missing value strategies as defined in PMML
 * (<a>http://www.dmg.org/v4-0/TreeModel.html#MissValStrategies</a>).
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
public enum PMMLMissingValueStrategy {
    /** lastPrediction strategy as specified in PMML. */
    LAST_PREDICTION("lastPrediction", true),
    /** nullPrediction strategy as specified in PMML. not yet supported */
    NULL_PREDICTION("nullPrediction", false),
    /** defaultChild strategy as specified in PMML. */
    DEFAULT_CHILD("defaultChild", true),
    /** weightedConfidence strategy as specified in PMML. not yet supported */
    WEIGHTED_CONFIDENCE("weightedConfidence", false),
    /** aggregateNodes strategy as specified in PMML. not yet supported */
    AGGREGATE_NODES("aggregateNodes", false),
    /** none strategy as specified in PMML. */
    NONE("none", true);

    private final String m_represent;
    private final boolean m_isSupported;

    private static final HashMap<String, PMMLMissingValueStrategy> LOOKUP =
        new HashMap<String, PMMLMissingValueStrategy>();

    /**
     * Create a new missing value strategy.
     *
     * @param rep the string representation of the strategy
     * @param supported true if the strategy is supported, false otherwise
     */
    private PMMLMissingValueStrategy(final String rep,
            final boolean supported) {
        m_represent = rep;
        m_isSupported = supported;
    }

    static {
        for (PMMLMissingValueStrategy strategy
                : EnumSet.allOf(PMMLMissingValueStrategy.class)) {
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
     * Returns the corresponding missing value strategy to the string
     * representation.
     * @param represent the representation to retrieve the strategy for
     * @return the missing value strategy
     */
    public static PMMLMissingValueStrategy get(final String represent) {
        if (represent == null) {
            return getDefault();
        }

        PMMLMissingValueStrategy strategy = LOOKUP.get(represent);
        if (strategy == null) {
            throw new IllegalArgumentException("Missing value strategy "
                    + represent + " is unknown.");
        } else if (!strategy.m_isSupported) {
            throw new IllegalArgumentException("Missing value strategy "
                    + represent + " is not yet supported.");
        }
        return strategy;
    }

    /**
     * Returns the default missing value strategy.
     * @return the default strategy
     */
    public static PMMLMissingValueStrategy getDefault() {
      return NONE;
    }


}
