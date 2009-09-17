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

/**
 * Contains the boolean operators that are specified in PMML for
 * CompoundPrediates and SimpleSetPredicates.
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
public enum PMMLBooleanOperator {
    /** or operator. */
    OR("or"),
    /** and operator. */
    AND("and"),
    /** xor operator. */
    XOR("xor"),
    /**
     * Surrogate operator. Used for cases where a missing value appears in the
     * evaluation. An expression <code>surrogate(a,b)</code> is equivalent to
     * <code>if not unknown(a) then a else b</code>.
     */
    SURROGATE("surrogate");

    private final String m_represent;

    private PMMLBooleanOperator(final String represent) {
        m_represent = represent;
    }

    private PMMLBooleanOperator() {
        m_represent = null;
    }

    private static final Map<String, PMMLBooleanOperator> LOOKUP =
            new HashMap<String, PMMLBooleanOperator>();

    static {
        for (PMMLBooleanOperator op
                : EnumSet.allOf(PMMLBooleanOperator.class)) {
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
     * @throws InstantiationException  - if no such PMML operator exists
     */
    public static PMMLBooleanOperator get(final String represent)
            throws InstantiationException {
        PMMLBooleanOperator pmmlBooleanOperator = LOOKUP.get(represent);
        if (pmmlBooleanOperator == null) {
            throw new InstantiationException("Illegal PMML boolean operator "
                    + "type '" + represent);
        }

        return pmmlBooleanOperator;
    }
}
