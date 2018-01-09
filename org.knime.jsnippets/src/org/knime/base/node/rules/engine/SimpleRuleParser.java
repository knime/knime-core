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
import java.util.Map;
import java.util.Set;

import org.knime.base.node.rules.engine.Rule.GenericRule;
import org.knime.base.node.rules.engine.Rule.Operators;
import org.knime.base.node.rules.engine.Rule.Outcome;
import org.knime.base.node.rules.engine.Rule.Outcome.NoOutcome;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.workflow.FlowVariable;

/**
 * A simple implementation to parse {@link Rule}s.
 * <p>
 * Rule Engine rules:
 *
 * <pre>
 * LINE := RULE | '//' [^\n]*
 * RULE := BEXPR '=&gt;' (SINGLE | BOOLCONST)
 * SINGLE := CONST | REF
 * BEXPR := BEXPR3
 * BEXPR3 := BEXPR2 ('OR' BEXPR2)*
 * BEXPR2 := BEXPR1 ('XOR' BEXPR1)*
 * BEXPR1 := BEXPR0 ('AND' BEXPR0)*
 * BEXPR0 := '(' BEXPR ')' |
 *           'NOT' BEXPR0 |
 *           'MISSING' COL |
 *           BOOLCONST |
 *           BOOLCOL |
 *           PREDEXPR
 * PREDEXPR := SINGLE OP SINGLE |
 *          SINGLE LOP (LIST | COL)
 * OP := '&gt;' | '&lt;' | '&gt;=' | '&lt;=' |
 *       '=' | 'LIKE' | 'MATCHES'
 * LOP := 'IN'
 * STRING := NON_QUOTE_STRING | PERL_QUOTE_STRING
 * NON_QUOTE_STRING = '&quot;' [^&quot;]* '&quot;'
 * PERL_QUOTE_STRING = '/' ([^\\/](\\/)?)* '/'
 * NUMBER := '-'? POSITIVE
 * POSITIVE := ([0-9]*\.[0-9]*('E''-'?[1-9][0-9]*)?) |
 *             [0-9]+('E''-'?[1-9][0-9]*)? | 'Infinity'
 * COL := '$' [^$]+ '$'
 * BOOLCOL := COL
 * REF := COL | FLOWVAR | TABLEREF
 * FLOWVAR := '$${' ('D' | 'I' | 'S') [^}]+ '}$$'
 * TABLEREF := '$$ROWID$$' | '$$ROWINDEX$$' | '$$ROWCOUNT$$'
 * CONST := STRING | NUMBER
 * BOOLCONST := 'TRUE' | 'FALSE'
 * LIST := '(' SINGLE (',' SINGLE)* ')'
 * </pre>
 *
 * </p>
 * <p>
 * Rule Filter rules:
 *
 * <pre>
 * LINE := RULE | '//' [^\n]*
 * RULE := BEXPR '=&gt;' BOOLCONST
 * SINGLE := CONST | REF
 * BEXPR := BEXPR3
 * BEXPR3 := BEXPR2 ('OR' BEXPR2)*
 * BEXPR2 := BEXPR1 ('XOR' BEXPR1)*
 * BEXPR1 := BEXPR0 ('AND' BEXPR0)*
 * BEXPR0 := '(' BEXPR ')' |
 *           'NOT' BEXPR0 |
 *           'MISSING' COL |
 *           BOOLCONST |
 *           BOOLCOL |
 *           PREDEXPR
 * PREDEXPR := SINGLE OP SINGLE |
 *          SINGLE LOP (LIST | COL)
 * OP := '&gt;' | '&lt;' | '&gt;=' | '&lt;=' |
 *       '=' | 'LIKE' | 'MATCHES'
 * LOP := 'IN'
 * STRING := '&quot;' [^&quot;]* '&quot;'
 * NUMBER := '-'? POSITIVE
 * POSITIVE := ([0-9]*\.[0-9]*('E''-'?[1-9][0-9]*)?) |
 *             [0-9]+('E''-'?[1-9][0-9]*)? | 'Infinity'
 * COL := '$' [^$]+ '$'
 * BOOLCOL := COL
 * REF := COL | FLOWVAR | TABLEREF
 * FLOWVAR := '$${' ('D' | 'I' | 'S') [^}]+ '}$$'
 * TABLEREF := '$$ROWID$$' | '$$ROWINDEX$$' | '$$ROWCOUNT$$'
 * CONST := STRING | NUMBER
 * BOOLCONST := 'TRUE' | 'FALSE'
 * LIST := '(' SINGLE (',' SINGLE)* ')'
 * </pre>
 *
 * </p>
 * <p>
 * Variable rules:
 *
 * <pre>
 * LINE := RULE | '//' [^\n]*
 * RULE := BEXPR '=&gt;' FLOWVAR
 * SINGLE := CONST | FLOWVAR
 * BEXPR := BEXPR3
 * BEXPR3 := BEXPR2 ('OR' BEXPR2)*
 * BEXPR2 := BEXPR1 ('XOR' BEXPR1)*
 * BEXPR1 := BEXPR0 ('AND' BEXPR0)*
 * BEXPR0 := '(' BEXPR ')' |
 *           'NOT' BEXPR0 |
 *           BOOLCONST |
 *           PREDEXPR
 * PREDEXPR := SINGLE OP SINGLE |
 *          SINGLE LOP LIST
 * OP := '&gt;' | '&lt;' | '&gt;=' | '&lt;=' |
 *       '=' | 'LIKE' | 'MATCHES'
 * LOP := 'IN'
 * STRING := '&quot;' [^&quot;]* '&quot;'
 * NUMBER := '-'? POSITIVE
 * POSITIVE := ([0-9]*\.[0-9]*('E''-'?[1-9][0-9]*)?) |
 *             [0-9]+('E''-'?[1-9][0-9]*)? | 'Infinity'
 * COL := '$' [^$]+ '$'
 * FLOWVAR := '$${' ('D' | 'I' | 'S') [^}]+ '}$$'
 * CONST := STRING | NUMBER
 * BOOLCONST := 'TRUE' | 'FALSE'
 * LIST := '(' SINGLE (',' SINGLE)* ')'
 * </pre>
 *
 * </p>
 *
 * @author Gabor Bakos
 * @since 2.8
 */
public class SimpleRuleParser extends BaseRuleParser<Expression> {

    /**
     * Constructs the parser with the default {@link ExpressionFactory}.
     *
     * @param spec The {@link DataTableSpec}
     * @param flowVariables The {@link FlowVariable}s.
     * @see SimpleRuleParser#SimpleRuleParser(DataTableSpec, Map, RuleExpressionFactory, ReferenceExpressionFactory)
     */
    public SimpleRuleParser(final DataTableSpec spec, final Map<String, FlowVariable> flowVariables) {
        this(spec, flowVariables, ExpressionFactory.getInstance(), ExpressionFactory.getInstance());
    }

    /**
     * Constructs the parser with the specified {@link ExpressionFactory}.
     *
     * @param spec The {@link DataTableSpec}
     * @param flowVariables The {@link FlowVariable}s.
     * @param factory The {@link RuleExpressionFactory} to create Boolean-valued {@link Expression}s.
     * @param refFactory The {@link ReferenceExpressionFactory} to create {@link Expression}s.
     * @see SimpleRuleParser#SimpleRuleParser(DataTableSpec, Map, RuleExpressionFactory, ReferenceExpressionFactory, boolean, Set)
     */
    public SimpleRuleParser(final DataTableSpec spec, final Map<String, FlowVariable> flowVariables,
        final RuleExpressionFactory<Expression, Expression> factory, final ReferenceExpressionFactory refFactory) {
        this(spec, flowVariables, factory, refFactory, true, RuleNodeSettings.RuleEngine.supportedOperators());
    }

    /**
     * Constructs the parser with the specified {@link ExpressionFactory}.
     *
     * @param spec The {@link DataTableSpec}
     * @param flowVariables The {@link FlowVariable}s.
     * @param factory The {@link RuleExpressionFactory} to create Boolean-valued {@link Expression}s.
     * @param refFactory The {@link ReferenceExpressionFactory} to create {@link Expression}s.
     * @param allowTableReferences Enable to parse {@code $$ROWINDEX$$}, {@code $$ROWID$$} or {@code $$ROWCOUNT$$}.
     * @param operators The allowed operators.
     */
    public SimpleRuleParser(final DataTableSpec spec, final Map<String, FlowVariable> flowVariables,
        final RuleExpressionFactory<Expression, Expression> factory, final ReferenceExpressionFactory refFactory, final boolean allowTableReferences, final Set<Operators> operators) {
        super(spec, flowVariables, factory, refFactory, allowTableReferences, operators);
    }

    /**
     * Parses a whole {@link Rule}.
     *
     * @param rule A line representing a rule (possibly comment).
     * @param booleanOutcome Should the rule include only a boolean outcome ({@code true}), or it cannot ({@code false}
     *            ), or it can be either boolean or non-boolean ({@code null})?
     * @return The parsed {@link Rule}.
     * @throws ParseException Problem during parsing.
     */
    public Rule parse(final String rule, final Boolean booleanOutcome) throws ParseException {
        if (RuleSupport.isComment(rule)) {
            return new GenericRule(rule, new Condition.Comment(rule), NoOutcome.getInstance());
        }
        final ParseState state = new ParseState(rule);
        final Condition condition = parseCondition(state);
        if (!state.isEnd()) {
            final Outcome outcome = parseOutcome(state, booleanOutcome);
            return new GenericRule(rule, condition, outcome);
        }
        throw new ParseException("No outcome specified.", state.getPosition());
    }

    /**
     * Parses a whole {@link Rule} with a mandatory {@link Outcome}.
     *
     * @param rule A line representing a rule (possibly comment).
     * @return The parsed {@link Rule}.
     * @throws ParseException Problem during parsing.
     * @see SimpleRuleParser#parse(String, Boolean)
     */
    public Rule parse(final String rule) throws ParseException {
        return parse(rule, null);
    }

    /**
     * Parses the outcome from a {@link BaseRuleParser.ParseState}, although if it is not required, it might return a {@link NoOutcome}
     * instance.
     *
     * @param state The {@link BaseRuleParser.ParseState}.
     * @param booleanOutcome Only booleans in {@link Outcome} of the rule? ({@code null} means both booleans and
     *            non-booleans are allowed.)
     * @return An {@link Outcome}, can be {@link NoOutcome} if it is missing and not required.
     * @throws ParseException Problem during parsing.
     */
    public Outcome parseOutcome(final ParseState state, final Boolean booleanOutcome) throws ParseException {
        state.skipWS();
        state.consumeText("=>");
        state.skipWS();
        if (state.isEnd()) {
            throw new ParseException("No outcome specified", state.getPosition());
        }
        return parseOutcomeAfterArrow(state, booleanOutcome);
    }

    /**
     * Reads a {@link Condition} and creates the proper {@link Expression} of it.
     *
     * @param state The {@link BaseRuleParser.ParseState}.
     * @return The {@link Condition} parsed.
     * @throws ParseException Problem during parsing.
     */
    public Condition parseCondition(final ParseState state) throws ParseException {
        Expression expression = parseBooleanExpression(state);
        String text = state.toString();
        text = text.substring(0, text.indexOf('\n'));
        return new Condition.GenericCondition(text, text.substring(0, state.getPosition()), true,
            expression);
    }
}
