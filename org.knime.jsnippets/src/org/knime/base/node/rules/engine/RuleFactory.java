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
 * Created on 2013.04.23. by Gabor
 */
package org.knime.base.node.rules.engine;

import java.text.ParseException;
import java.util.Map;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.workflow.FlowVariable;

/**
 * A factory class to generate rules.
 *
 * @author Gabor Bakos
 * @since 2.8
 */
public final class RuleFactory {
    private static final RuleFactory INSTANCE = new RuleFactory(false, true);

    private static final RuleFactory FILTER_INSTANCE = new RuleFactory(true, true);

    private static final RuleFactory VARIABLE_INSTANCE = new RuleFactory(false, false);

    private final boolean m_allowNoOutcome;

    private final boolean m_allowTableReference;

    private RuleFactory(final boolean allowNoOutcome, final boolean allowTableReference) {
        super();
        this.m_allowNoOutcome = allowNoOutcome;
        this.m_allowTableReference = allowTableReference;
    }

    /**
     * Enum to help selecting the proper {@link RuleFactory}.
     *
     * @author Gabor Bakos
     * @since 2.8
     */
    enum RuleSet {
        /** Both outcome and table references are allowed. */
        Full,
        /** The outcome is optional, not used. */
        Filter,
        /**
         * Outcomes are required, but table references are disallowed. (Column references are allowed, but you can pass
         * an empty {@link DataTableSpec} as the parameter.)
         */
        Variable;
    }

    /**
     * Creates a new rule by parsing a rule string.
     *
     * @param rule the rule string
     * @param spec the spec of the table on which the rule will be applied.
     * @param flowVariables the flow variables; please do not modify during this call.
     * @return {@link Rule} representing {@code rule}, can be a comment.
     * @throws ParseException if the rule contains a syntax error
     * @since 2.8
     */
    public Rule parse(final String rule, final DataTableSpec spec, final Map<String, FlowVariable> flowVariables)
            throws ParseException {
        return new SimpleRuleParser(spec, flowVariables, m_allowTableReference).parse(rule, m_allowNoOutcome);
    }

    /**
     * @return the instance for general rules
     */
    public static RuleFactory getInstance() {
        return INSTANCE;
    }

    /**
     * @return the instance for rules with possibly no outcome.
     */
    public static RuleFactory getFilterInstance() {
        return FILTER_INSTANCE;
    }

    /**
     * @return the instance for rules without reference to table properties ({@code $$ROWID$$}, {@code $$ROWINDEX$$},
     *         {@code $$ROWCOUNT$$}).
     */
    public static RuleFactory getVariableInstance() {
        return VARIABLE_INSTANCE;
    }

    /**
     * Selects the proper {@link RuleFactory} instance.
     *
     * @param ruleSet The {@link RuleSet} to select.
     * @return The {@link RuleFactory} parsing that kind of rules.
     */
    static RuleFactory getInstance(final RuleSet ruleSet) {
        switch (ruleSet) {
            case Full:
                return INSTANCE;
            case Filter:
                return FILTER_INSTANCE;
            case Variable:
                return VARIABLE_INSTANCE;
            default:
                throw new UnsupportedOperationException("Unknown ruleset: " + ruleSet);
        }
    }
}
