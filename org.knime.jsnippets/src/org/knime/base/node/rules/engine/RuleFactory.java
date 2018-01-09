/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.knime.base.node.rules.engine.BaseRuleParser.ParseState;
import org.knime.base.node.rules.engine.Rule.Operators;
import org.knime.base.node.rules.engine.pmml.PMMLRuleParser;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.workflow.FlowVariable;

/**
 * A factory class to generate rules.
 *
 * @author Gabor Bakos
 * @since 2.8
 */
public class RuleFactory implements Cloneable {
    private static final RuleFactory INSTANCE = new RuleFactory(null, true,
        RuleNodeSettings.RuleEngine.supportedOperators());

    private static final RuleFactory FILTER_INSTANCE = new RuleFactory(true, true,
        RuleNodeSettings.RuleFilter.supportedOperators());

    private static final RuleFactory VARIABLE_INSTANCE = new RuleFactory(false, false,
        RuleNodeSettings.VariableRule.supportedOperators());

    private static final RuleFactory PMML_INSTANCE = new RuleFactory(null, false,
        RuleNodeSettings.PMMLRule.supportedOperators()) {
        @Override
        public Rule parse(final String rule, final DataTableSpec spec,
            final java.util.Map<String, FlowVariable> flowVariables) throws ParseException {
            if (!RuleSupport.isComment(rule)) {
                ParseState state = new ParseState(rule);
                PMMLRuleParser pmmlRuleParser = new PMMLRuleParser(spec, Collections.<String, FlowVariable> emptyMap());
                if (!super.m_checkColumns) {
                    pmmlRuleParser.disableColumnCheck();
                }
                pmmlRuleParser.parseBooleanExpression(state);
                state.skipWS();
                state.consumeText("=>");
                pmmlRuleParser.parseOutcomeOperand(state, null);
            }
            return super.parse(rule, spec, flowVariables);
        }
    };

    private final Boolean m_booleanOutcome;

    private final boolean m_allowTableReference;

    private final Set<Operators> m_operators;

    private boolean m_checkColumns = true;

    private boolean m_checkFlowVars = true;

    private boolean m_missingMatch = true, m_nanMatch = true;

    private RuleFactory(final Boolean booleanOutcome, final boolean allowTableReference, final Set<Operators> operators) {
        super();
        this.m_booleanOutcome = booleanOutcome;
        this.m_allowTableReference = allowTableReference;
        this.m_operators = operators;
    }

    /**
     * Disables the check for the column names. <br/>
     * Be careful with this method, as the instance of this class might be shared across different callers, so please
     * consider {@link #cloned() cloning} before calling this method.
     *
     * @see #cloned()
     */
    public void disableColumnChecks() {
        m_checkColumns = false;
    }

    /**
     * Disables the check for the flow variable names. <br/>
     * Be careful with this method, as the instance of this class might be shared across different callers, so please
     * consider {@link #cloned() cloning} before calling this method.
     *
     * @see #cloned()
     */
    public void disableFlowVariableChecks() {
        m_checkFlowVars = false;
    }

    /**
     * Disables the comparisons to match on {@link DataCell#isMissing() missing values}, except when both are missing
     * and the comparison is {@code =} .
     *
     * Be careful with this method, as the instance of this class might be shared across different callers, so please
     * consider {@link #cloned() cloning} before calling this method.
     *
     * @see #cloned()
     */
    public void disableMissingComparisons() {
        m_missingMatch = false;
    }

    /**
     * Disables the comparisons to match on {@link Double#NaN} values, except when both are {@link Double#NaN}s and the
     * comparison is {@code =}.
     *
     * Be careful with this method, as the instance of this class might be shared across different callers, so please
     * consider {@link #cloned() cloning} before calling this method.
     *
     * @see #cloned()
     */
    public void disableNaNComparisons() {
        m_nanMatch = false;
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
        ExpressionFactory expFactory = ExpressionFactory.getInstance();
        if (!m_missingMatch) {
            expFactory = expFactory.withMissingsDoNotMatch();
        }
        if (!m_nanMatch) {
            expFactory = expFactory.withNaNsDoNotMatch();
        }
        SimpleRuleParser parser = new SimpleRuleParser(spec, flowVariables, expFactory,
                expFactory, m_allowTableReference, m_operators);
        if (!m_checkColumns) {
            parser.disableColumnCheck();
        }
        if (!m_checkFlowVars) {
            parser.disableFlowVariableCheck();
        }
        return parser.parse(rule, m_booleanOutcome);
    }

    /**
     * @param nodeType The {@link RuleNodeSettings}.
     * @return The instance belonging the the node.
     * @since 2.9
     */
    public static RuleFactory getInstance(final RuleNodeSettings nodeType) {
        switch (nodeType) {
            case PMMLRule:
                return PMML_INSTANCE;
            case RuleEngine:
                return INSTANCE;
            case RuleFilter:
            case RuleSplitter:
                return FILTER_INSTANCE;
            case VariableRule:
                return VARIABLE_INSTANCE;
            default:
                throw new UnsupportedOperationException("Not supported: " + nodeType);
        }
    }

    /**
     * @return A clone of the current instance.
     * @since 2.9
     */
    public RuleFactory cloned() {
        try {
            return (RuleFactory)clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }
}
