/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
                return a == b || a.equals(b);
            case NOT_EQUAL:
                return !a.equals(b);
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
