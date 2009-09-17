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
 * Contains the operators that are specified in PMML for SimplePredicates.
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 */
public enum PMMLOperator {
    /** = operator. */
    EQUAL("equal", "="),
    /** != operator. */
    NOT_EQUAL("notEqual", "<>"),
    /** < operator. */
    LESS_THAN("lessThan", "<"),
    /** <= operator. */
    LESS_OR_EQUAL("lessOrEqual", "<="),
    /** > operator. */
    GREATER_THAN("greaterThan", ">"),
    /** >= operator. */
    GREATER_OR_EQUAL("greaterOrEqual", ">="),
    /** is missing operator. */
    IS_MISSING("isMissing", "is missing"),
    /** is not missing operator. */
    IS_NOT_MISSING("isNotMissing", "is not missing");

    private final String m_represent;
    private final String m_symbol;

    private PMMLOperator(final String represent, final String symbol) {
        m_represent = represent;
        m_symbol = symbol;
    }

    private static final Map<String, PMMLOperator> LOOKUP =
            new HashMap<String, PMMLOperator>();

    static {
        for (PMMLOperator op : EnumSet.allOf(PMMLOperator.class)) {
            LOOKUP.put(op.toString(), op);
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
     * Returns the corresponding operator for the passed representation.
     *
     * @param represent the representation to find the operator for
     * @return the operator
     * @throws InstantiationException - if no such PMML operator exists
     */
    public static PMMLOperator get(final String represent)
            throws InstantiationException {
        PMMLOperator pmmlOperator = LOOKUP.get(represent);
        if (pmmlOperator == null) {
            throw new InstantiationException("Illegal PMML operator type '"
                    + represent);
        }
        return pmmlOperator;
    }

    /**
     * Returns the symbol for the operator.
     *
     * @return the symbol
     */
    public String getSymbol() {
        return m_symbol;
    }

    /**
     * Evaluates the operator on the passed strings.
     *
     * @param a the first string
     * @param b the second string (only applicable for binary operations,
     *            otherwise ignored)
     * @return the result of the operation
     */
    public boolean evaluate(final String a, final String b) {
        switch (this) {
            case EQUAL:
                return a.equals(b);
            case NOT_EQUAL:
                return !a.equals(b);
            case LESS_THAN:
                return a.compareTo(b) < 0;
            case LESS_OR_EQUAL:
                return a.compareTo(b) <= 0;
            case GREATER_THAN:
                return a.compareTo(b) > 0;
            case GREATER_OR_EQUAL:
                return a.compareTo(b) >= 0;
            case IS_MISSING:
                return a == null;
            case IS_NOT_MISSING:
                return a != null;
        }
        // we should never arrive here
        assert false;
        return false;
    }

    /**
     * Evaluates the operator on the passed double values.
     *
     * @param a the first double value
     * @param b the second double value (only applicable for binary operations,
     *            otherwise ignored)
     * @return the result of the operation
     */
    public boolean evaluate(final Double a, final Double b) {
        switch (this) {
            case EQUAL:
                return a == b;
            case NOT_EQUAL:
                return a != b;
            case LESS_THAN:
                return a < b;
            case LESS_OR_EQUAL:
                return a <= b;
            case GREATER_THAN:
                return a > b;
            case GREATER_OR_EQUAL:
                return a >= b;
            case IS_MISSING:
                return a == null;
            case IS_NOT_MISSING:
                return a != null;
        }
        // we should never arrive here
        assert false;
        return false;
    }



}
