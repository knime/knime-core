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
 *   Sep 4, 2009 (morent): created
 */
package org.knime.base.node.mine.decisiontree2;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Contains the set operators that are specified in PMML for
 * SimpleSetPredicates.
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
public enum PMMLSetOperator {
    /** is missing operator. */
    IS_IN("isIn"),
    /** is not missing operator. */
    IS_NOT_IN("isNotIn");

    private final String m_represent;

    private PMMLSetOperator(final String represent) {
        m_represent = represent;
    }

    private PMMLSetOperator() {
        m_represent = null;
    }

    private static final Map<String, PMMLSetOperator> LOOKUP =
            new HashMap<String, PMMLSetOperator>();

    static {
        for (PMMLSetOperator op : EnumSet.allOf(PMMLSetOperator.class)) {
            LOOKUP.put(op.toString(), op);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (m_represent != null) {
            return m_represent;
        }
        return super.toString();
    }

    /**
     * Returns the corresponding operator for the passed representation.
     *
     * @param represent the representation to find the operator for
     * @return the operator
     * @throws InstantiationException - if no such PMML operator exists
     */
    public static PMMLSetOperator get(final String represent)
            throws InstantiationException {
        PMMLSetOperator pmmlSetOperator = LOOKUP.get(represent);
        if (pmmlSetOperator == null) {
            throw new InstantiationException("Illegal PMML set operator type '"
                    + represent);
        }
        return pmmlSetOperator;
    }

    /**
     * Evaluates the operator on the passed value.
     * @param <T> The type of the parameters to be evaluated.
     *
     * @param a the value to check
     * @param set the set to compare against
     * @return the result of the operation
     */
    public <T> boolean evaluate(final T a, final Set<T> set) {
        if (a == null || set == null) {
            return false;
        }
        switch (this) {
            case IS_IN:
                return set.contains(a);
            case IS_NOT_IN:
                return !set.contains(a);
        }
        // we should never arrive here
        assert false;
        return false;
    }

}
