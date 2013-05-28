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

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.knime.base.node.rules.engine.Rule.GenericRule;
import org.knime.base.node.rules.engine.Rule.Operators;
import org.knime.base.node.rules.engine.Rule.Outcome;
import org.knime.base.node.rules.engine.Rule.Outcome.NoOutcome;
import org.knime.base.node.rules.engine.Rule.TableReference;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.workflow.FlowVariable;

/**
 * A simple implementation to parse {@link Rule}s.
 *
 * @author Gabor Bakos
 * @since 2.8
 */
public class SimpleRuleParser {

    private final DataTableSpec m_spec;

    private final Map<String, FlowVariable> m_flowVariables;

    private final ExpressionFactory m_factory;

    private final boolean m_allowTableReferences;

    /**
     * The current state used to parse rules. It also contains helper methods.
     *
     * @author Gabor Bakos
     * @since 2.8
     */
    public static class ParseState implements Serializable {
        private static final long serialVersionUID = 1564553985597559764L;

        private String m_text;

        private int m_position;

        private final int m_end;

        //        private Stack<List<Expression>> expressions = new Stack<List<Expression>>();

        /**
         * Initializes the {@link ParseState} with {@code text}.
         *
         * @param text The {@link String} to parse.
         */
        public ParseState(final String text) {
            super();
            this.m_text = text;
            this.m_position = 0;
            this.m_end = text.length();
        }

        /**
         * @return Is end of text reached?
         */
        public boolean isEnd() {
            return m_position >= m_end;
        }

        /**
         * Skips the white spaces.
         */
        public void skipWS() {
            while (!isEnd()) {
                if (Character.isWhitespace(m_text.charAt(m_position))) {
                    ++m_position;
                } else {
                    break;
                }
            }
        }

        /**
         * Reads a string.
         *
         * @return The string without escapes.
         * @throws ParseException Not a string comes.
         */
        public String readString() throws ParseException {
            final int startPosition = m_position;
            expect('"');
            consume();
            final String collected = collectTill('"', startPosition/*, '\\'*/);
            expect('"');
            consume();
            return collected;
        }

        /**
         * Collects the characters till it not finds the {@code find} character. The {@code escapeChars} are not
         * included the result (unless it was preceded with an escape char) and a {@code find} character after an escape
         * char does not considered a terminator. <br/>
         * This method consumes characters.
         *
         * @param find The character to find.
         * @param startPosition The initial position where the problem begins. This will be used in the error message.
         * @param escapeChars The escape characters. If no specified, all characters interpreted as-is.
         * @return The unescaped text without the {@code find} character at the end.
         * @throws ParseException Reached end without finding unescaped {@code find}.
         */
        private String collectTill(final char find, final int startPosition, final char... escapeChars)
                throws ParseException {
            StringBuilder sb = new StringBuilder();
            while (!isEnd()) {
                char ch = consume();
                if (ch == find) {
                    --m_position;
                    return sb.toString();
                }
                for (char c : escapeChars) {
                    if (ch == c) {
                        ch = consume();
                        break;
                    }
                }
                sb.append(ch);
            }
            throw new ParseException("No matching char found: '" + find + "'.", startPosition);
        }

        /**
         * Peeks whether the selected character ({@code ch}) comes next or not.
         *
         * @param ch The character to check for.
         * @throws ParseException Not that character found, or end of text.
         */
        public void expect(final char ch) throws ParseException {
            if (isEnd() || m_text.charAt(m_position) != ch) {
                throw new ParseException("Expected: " + ch + " got: "
                        + (isEnd() ? "end of rule" : m_text.charAt(m_position)), m_position);
            }
        }

        /**
         * Checks the current character, do not move forward.
         *
         * @return The current character.
         * @throws ParseException End is reached.
         */
        public char peekChar() throws ParseException {
            if (isEnd()) {
                throw new ParseException("Reached end of rule", m_position);
            }
            return m_text.charAt(m_position);
        }

        /**
         * Moves one character forward.
         *
         * @return The current character.
         * @throws ParseException End is reached.
         */
        public char consume() throws ParseException {
            char ch = peekChar();
            ++m_position;
            return ch;
        }

        /**
         * Checks the next character, do not move forward.
         *
         * @return The next character.
         * @throws ParseException End is reached.
         */
        public char peekNext() throws ParseException {
            if (m_position + 1 >= m_end) {
                throw new ParseException("Reached end of file", m_end);
            }
            return m_text.charAt(m_position + 1);
        }

        /**
         * Checks whether the characters in {@code token} match the next characters in the original text. Do not move
         * forward.
         *
         * @param token A {@link String}.
         * @return {@code true} iff there was a match.
         */
        public boolean peekText(final String token) {
            int i = m_position, tokenPos = 0;
            while (i < m_end && tokenPos < token.length()) {
                if (m_text.charAt(i) != token.charAt(tokenPos)) {
                    return false;
                }
                ++i;
                ++tokenPos;
            }
            return tokenPos == token.length();
        }

        /**
         * Checks whether the characters in {@code token} match the next characters in the original text. Consumes the
         * {@code token}.
         *
         * @param token A {@link String}.
         * @return {@code true} iff there was a match.
         * @throws ParseException The next characters do not match {@code token}.
         */
        public String consumeText(final String token) throws ParseException {
            if (!peekText(token)) {
                throw new ParseException("Expected: " + token, m_position);
            }
            m_position += token.length();
            return token;
        }

        /**
         * @return {@code true} iff next characters are {@code $$ROWCOUNT$$}, {@code $$ROWID$$} or {@code $$ROWINDEX$$}.
         */
        public boolean isTablePropertyReference() {
            for (String string : new String[]{org.knime.ext.sun.nodes.script.expression.Expression.ROWCOUNT,
                    org.knime.ext.sun.nodes.script.expression.Expression.ROWID,
                    org.knime.ext.sun.nodes.script.expression.Expression.ROWINDEX}) {
                if (peekText("$$" + string + "$$")) {
                    return true;
                }
            }
            return false;
        }

        /**
         * @return {@code true} iff next characters look like the start of a flow variable reference.
         */
        public boolean isFlowVariableRef() {
            for (Character c : Util.getFlowVarTypePrefixChars()) {
                if (peekText("$${" + c)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * @return {@code true} iff next characters look like the start of a column reference.
         */
        public boolean isColumnRef() {
            try {
                return peekChar() == '$' && peekNext() != '$' && !isFlowVariableRef() && !isTablePropertyReference();
            } catch (ParseException e) {
                return false;
            }
        }

        /**
         * @return An unescaped variable name, without the prefix and suffix.
         * @throws ParseException Not a flow variable was parsed.
         * @see #isFlowVariableRef()
         */
        public String readFlowVariable() throws ParseException {
            int startPos = m_position;
            if (!isFlowVariableRef()) {
                throw new ParseException("Expected a flow variable", startPos);
            }
            consumeText("$${");
            final String flowVarName = collectTill('}', startPos);
            consumeText("}$$");
            if (flowVarName.length() < 2) {
                throw new ParseException("Invalid flow variable name: " + flowVarName, startPos);
            }
            return flowVarName.substring(1);
        }

        /**
         * @return The table property reference text.
         * @throws ParseException Not a table reference parsed.
         * @see #isTablePropertyReference()
         */
        public String readTablePropertyReference() throws ParseException {
            int startPos = m_position;
            if (!isTablePropertyReference()) {
                throw new ParseException("Expected a $$ROWINDEX$$, $$ROWKEY$$ or $$ROWID$$", startPos);
            }
            consumeText("$$");
            final String reference = collectTill('$', startPos);
            consumeText("$$");
            return reference;
        }

        /**
         * @return An unescaped column reference without the prefix and the suffix.
         * @throws ParseException Not a column reference was parsed.
         * @see #isColumnRef()
         */
        public String readColumnRef() throws ParseException {
            int startPos = m_position;
            if (!isColumnRef()) {
                throw new ParseException("Expected a column reference", startPos);
            }
            expect('$');
            consume();
            final String columnName = collectTill('$', startPos);
            expect('$');
            consume();
            return columnName;
        }

        /**
         * @return the position
         */
        public int getPosition() {
            return m_position;
        }

        /**
         * @param position the position to set
         */
        public void setPosition(final int position) {
            this.m_position = position;
        }

        /**
         * @return A {@link Double} number as a {@link String} (cannot be {@link Double#isNaN() NaN}, uses {@code .} as
         *         the decimal separator).
         * @throws ParseException Not a double number.
         */
        public String parseNumber() throws ParseException {
            final String infinityString = "Infinity";
            int len = 1;
            if (isEnd()) {
                throw new ParseException("Reached end when expecting a number", m_position);
            }
            try {
                if (m_text.length() > m_position + len - 1 && m_text.charAt(m_position + len - 1) == '-') {
                    ++len;
                }
                if (m_text.length() > m_position + len - 1 && m_text.charAt(m_position + len - 1) == '.') {
                    ++len;
                }
                if (m_text.length() >= m_position + len + infinityString.length() - 1
                        && infinityString.equals(m_text.substring(m_position + len - 1, m_position + len - 1
                                + infinityString.length()))) {
                    len += infinityString.length();
                    len--;
                    String ret = m_text.substring(m_position, m_position + len);
                    m_position += len;
                    return ret;
                }
                while (m_position + len < m_end) {
                    String substring = m_text.substring(m_position, m_position + len);
                    if (substring.toLowerCase().endsWith("e") && len > 1) {
                        final char ch = substring.charAt(len - 2);
                        if (ch == '.' || Character.toLowerCase(ch) == 'e') {
                            throw new ParseException("Not a number", m_position);
                        }
                        len++;
                        continue;
                    }
                    if (substring.charAt(len - 1) == '-' && len > 1
                            && Character.toLowerCase(substring.charAt(len - 2)) == 'e') {
                        ++len;
                        continue;
                    }
                    Double.parseDouble(substring);
                    ++len;
                }
            } catch (NumberFormatException e) {
                --len;
            }
            if (m_position + len > m_text.length()) {
                --len;
            }
            while (len > 0 && !parses(m_text.substring(m_position, m_position + len))) {
                --len;
            }
            if (len == 0) {
                throw new ParseException("Not a number", m_position);
            }
            String ret = m_text.substring(m_position, m_position + len);
            m_position += len;
            return ret;
        }

        /**
         * @param string A {@link String}.
         * @return Checks whether it can be {@link Double#parseDouble(String) parsed} as a {@link Double}.
         */
        private boolean parses(final String string) {
            try {
                if (string.length() == 0) {
                    return false;
                }
                char last = string.charAt(string.length() - 1);
                switch (last) {
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                    case '.':
                    case 'E':
                    case 'y':
                        break;
                        default:
                            throw new NumberFormatException(string);
                }
                Double.parseDouble(string);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(m_text);
            sb.append('\n');
            for (int i = m_position; i-- > 0;) {
                sb.append(' ');
            }
            sb.append('|');
            return sb.toString();
        }

        /**
         * @return An {@link Operators}.
         * @throws ParseException If not an {@link Operators} comes.
         */
        public Operators parseOperator() throws ParseException {
            Operators longest = null;
            for (Operators operator : Operators.values()) {
                if (peekText(operator.toString())) {
                    if (longest == null || longest.toString().length() < operator.toString().length()) {
                        longest = operator;
                    }
                }
            }
            if (longest == null) {
                throw new ParseException("Expected an operator", m_position);
            }
            m_position += longest.toString().length();
            return longest;
        }

        /**
         * Expects the next character to be a white space.
         *
         * @throws ParseException The next character is not a white space.
         */
        public void expectWS() throws ParseException {
            if (!Character.isWhitespace(peekChar())) {
                throw new ParseException("Please separate them with spaces", m_position);
            }
        }
    }

    /**
     * Constructs the parser with the default {@link ExpressionFactory}.
     *
     * @param spec The {@link DataTableSpec}
     * @param flowVariables The {@link FlowVariable}s.
     * @see SimpleRuleParser#SimpleRuleParser(DataTableSpec, Map, ExpressionFactory)
     */
    public SimpleRuleParser(final DataTableSpec spec, final Map<String, FlowVariable> flowVariables) {
        this(spec, flowVariables, ExpressionFactory.getInstance());
    }

    /**
     * Constructs the parser with the specified {@link ExpressionFactory}.
     *
     * @param spec The {@link DataTableSpec}
     * @param flowVariables The {@link FlowVariable}s.
     * @param factory The {@link ExpressionFactory} to create {@link Expression}s.
     * @see SimpleRuleParser#SimpleRuleParser(DataTableSpec, Map, ExpressionFactory)
     */
    public SimpleRuleParser(final DataTableSpec spec, final Map<String, FlowVariable> flowVariables,
                     final ExpressionFactory factory) {
        this(spec, flowVariables, factory, true);
    }

    /**
     * Constructs the parser with the default {@link ExpressionFactory}.
     *
     * @param spec The {@link DataTableSpec}
     * @param flowVariables The {@link FlowVariable}s.
     * @param allowTableReferences Enable to parse {@code $$ROWINDEX$$}, {@code $$ROWID$$} or {@code $$ROWCOUNT$$}.
     * @see SimpleRuleParser#SimpleRuleParser(DataTableSpec, Map, ExpressionFactory)
     */
    public SimpleRuleParser(final DataTableSpec spec, final Map<String, FlowVariable> flowVariables,
                            final boolean allowTableReferences) {
        this(spec, flowVariables, ExpressionFactory.getInstance(), allowTableReferences);
    }

    /**
     * Constructs the parser with the specified {@link ExpressionFactory}.
     *
     * @param spec The {@link DataTableSpec}
     * @param flowVariables The {@link FlowVariable}s.
     * @param factory The {@link ExpressionFactory} to create {@link Expression}s.
     * @param allowTableReferences Enable to parse {@code $$ROWINDEX$$}, {@code $$ROWID$$} or {@code $$ROWCOUNT$$}.
     * @see SimpleRuleParser#SimpleRuleParser(DataTableSpec, Map, ExpressionFactory)
     */
    public SimpleRuleParser(final DataTableSpec spec, final Map<String, FlowVariable> flowVariables,
                            final ExpressionFactory factory, final boolean allowTableReferences) {
        super();
        this.m_spec = spec;
        this.m_flowVariables = flowVariables;
        this.m_factory = factory;
        this.m_allowTableReferences = allowTableReferences;
    }

    /**
     * Parses a whole {@link Rule}.
     *
     * @param rule A line representing a rule (possibly comment).
     * @param allowNoOutcome Should the rule include an outcome, or it is optional?
     * @return The parsed {@link Rule}.
     * @throws ParseException Problem during parsing.
     */
    public Rule parse(final String rule, final boolean allowNoOutcome) throws ParseException {
        if (RuleSupport.isComment(rule)) {
            return new GenericRule(rule, new Condition.Comment(rule), NoOutcome.getInstance());
        }
        final ParseState state = new ParseState(rule);
        final Condition condition = parseCondition(state);
        if (!state.isEnd()) {
            final Outcome outcome = parseOutcome(state, allowNoOutcome);
            return new GenericRule(rule, condition, outcome);
        }
        if (!allowNoOutcome) {
            throw new ParseException("No outcome specified.", state.getPosition());
        }
        return new GenericRule(rule, condition, NoOutcome.getInstance());
    }

    /**
     * Parses a whole {@link Rule} with a mandatory {@link Outcome}.
     *
     * @param rule A line representing a rule (possibly comment).
     * @return The parsed {@link Rule}.
     * @throws ParseException Problem during parsing.
     * @see SimpleRuleParser#parse(String, boolean)
     */
    public Rule parse(final String rule) throws ParseException {
        return parse(rule, false);
    }

    /**
     * Parses only the outcome from a {@link ParseState}.
     *
     * @param state The {@link ParseState}.
     * @return The parsed {@link Outcome}.
     * @throws ParseException Problem during parsing.
     * @see SimpleRuleParser#parseOutcome(ParseState, boolean)
     */
    public Outcome parseOutcome(final ParseState state) throws ParseException {
        return parseOutcome(state, false);
    }

    /**
     * Parses the outcome from a {@link ParseState}, although if it is not required, it might return a {@link NoOutcome}
     * instance.
     *
     * @param state The {@link ParseState}.
     * @param allowNoOutcome Allow missing {@link Outcome} in the rule?
     * @return An {@link Outcome}, can be {@link NoOutcome} if it is missing and not required.
     * @throws ParseException Problem during parsing.
     */
    public Outcome parseOutcome(final ParseState state, final boolean allowNoOutcome) throws ParseException {
        state.skipWS();
        state.consumeText("=>");
        state.skipWS();
        if (state.isEnd()) {
            if (allowNoOutcome) {
                return Outcome.NoOutcome.getInstance();
            }
            throw new ParseException("No outcome specified", state.getPosition());
        }
        Expression parseOperand = parseOperand(state);
        state.skipWS();
        if (!state.isEnd()) {
            throw new ParseException("Garbage at end of rule detected", state.getPosition());
        }
        return new Outcome.GenericOutcome(parseOperand);
    }

    /**
     * Reads a {@link Condition} and creates the proper {@link Expression} of it.
     *
     * @param state The {@link ParseState}.
     * @return The {@link Condition} parsed.
     * @throws ParseException Problem during parsing.
     */
    public Condition parseCondition(final ParseState state) throws ParseException {
        Expression expression = parseBooleanExpression(state);
        return new Condition.GenericCondition(state.m_text, state.m_text.substring(0, state.getPosition()), true,
                expression);
    }

    /**
     * Reads a boolean connective. The {@code AND}, {@code OR}, {@code XOR} connectives are associative, so no
     * parenthesis is required within homogeneous conditions. The {@code NOT} operator is prefix, the other connectives
     * are parsed in infix form.
     *
     * @param state The {@link ParseState}.
     * @return The Boolean (inputs, output) {@link Expression} parsed.
     * @throws ParseException Problem during parsing.
     */
    private Expression parseBooleanExpression(final ParseState state) throws ParseException {
        state.skipWS();
        char ch = state.peekChar();
        Expression left = parseNonConnectiveBooleanExpression(state, ch);
        state.skipWS();
        //Check for infix logical connectives
        if (state.peekText(Operators.AND.name())) {
            List<Expression> arguments = collectInfixArguments(state, left, Operators.AND.name());
            return m_factory.and(arguments.toArray(new Expression[0]));
        } else if (state.peekText(Operators.OR.name())) {
            List<Expression> arguments = collectInfixArguments(state, left, Operators.OR.name());
            return m_factory.or(arguments.toArray(new Expression[0]));
        } else if (state.peekText(Operators.XOR.name())) {
            List<Expression> arguments = collectInfixArguments(state, left, Operators.XOR.name());
            return m_factory.xor(arguments.toArray(new Expression[0]));
        }
        return left;
    }

    /**
     * Reads a boolean expression without infix logical connectives ({@code AND}, {@code OR}, {@code XOR}) are not
     * parsed during this call.
     *
     * @param state The {@link ParseState}.
     * @param ch The current character.
     * @return The Boolean (inputs, output) {@link Expression} parsed.
     * @throws ParseException Problem during parsing.
     */
    private Expression parseNonConnectiveBooleanExpression(final ParseState state, final char ch) throws ParseException {
        Expression left;
        if (ch == '(') { //parenthesis
            state.consume();
            left = parseBooleanExpression(state);
            state.expect(')');
            state.consume();
        } else if (state.peekText(Operators.NOT.name())) { //NOT
            state.consumeText(Operators.NOT.name());
            state.expectWS();
            state.skipWS();
            Expression expressionToNegate = parseBooleanExpression(state);
            left = m_factory.not(expressionToNegate);
        } else if (state.peekText(Operators.MISSING.name())) { //MISSING
            state.consumeText(Operators.MISSING.name());
            String operator = "MISSING ";
            state.expectWS();
            Expression reference = parseReference(state, operator);
            left = m_factory.missing(reference);
        } else { //infix relation
            left = parseRelation(state);
        }
        return left;
    }

    /**
     * Reads a {@link FlowVariable} reference, or a {@link TableReference} or a column reference.
     *
     * @param state The {@link ParseState}.
     * @param operator The text referring to the operator (for error messages).
     * @return The {@link Expression} parsed representing a column.
     * @throws ParseException Problem during parsing.
     */
    private Expression parseReference(final ParseState state, final String operator) throws ParseException {
        state.skipWS();
        Expression reference;
        if (state.isFlowVariableRef()) {
            reference = parseFlowVariableExpression(state);
        } else if (state.isTablePropertyReference() && m_allowTableReferences) {
            reference = parseTablePropertyReference(state);
        } else if (state.isColumnRef()) {
            reference = parseColumnExpression(state);
        } else {
            throw new ParseException(operator + "operator requires either a column"
                    + (m_allowTableReferences ? ", a table property" : "") + " or a flow variable", state.getPosition());
        }
        return reference;
    }

    /**
     * Reads the infix operators arguments.
     *
     * @param state The {@link ParseState}.
     * @param left The already parsed left {@link Expression}.
     * @param name The name of the operator.
     * @return The {@link Expression} parsed representing a infix operator ({@code name}).
     * @throws ParseException Problem during parsing.
     */
    private List<Expression> collectInfixArguments(final ParseState state, final Expression left, final String name)
            throws ParseException {
        state.skipWS();
        List<Expression> arguments = new ArrayList<Expression>();
        arguments.add(left);
        while (state.peekText(name)) {
            state.consumeText(name);
            state.expectWS();
            state.skipWS();
            arguments.add(parseNonConnectiveBooleanExpression(state, state.peekChar()));
            state.skipWS();
        }
        return arguments;
    }

    /**
     * Parses the <strong>infix</strong> {@link Operators} with their arguments.
     *
     * @param state The {@link ParseState}.
     * @return The Boolean (output) {@link Expression} parsed.
     * @throws ParseException Problem during parsing.
     */
    private Expression parseRelation(final ParseState state) throws ParseException {
        int beforeLeft = state.getPosition();
        Expression left = parseOperand(state);
        state.skipWS();
        int beforeOperator = state.getPosition();
        Operators op = state.parseOperator();
        switch (op) {
            case IN:
                Expression right = parseList(state);
                return m_factory.in(left, right);
            case MISSING:
                throw new ParseException("Invalid logical predicate: " + op, beforeOperator);
            case NOT:
            case AND:
            case OR:
            case XOR:
                throw new ParseException("Invalid logical connective" + op, beforeOperator);
            case CONTAINS:
            case EQ:
            case GE:
            case GT:
            case LE:
            case LIKE:
            case LT:
            case MATCHES:
                // single argument
                break;
            default:
                throw new ParseException("Not supported operator: " + op, beforeOperator);
        }
        switch (op) {
            case CONTAINS:
            case LIKE:
            case MATCHES:
                state.expectWS();
                break;

            default:
                break;
        }
        int beforeRight = state.getPosition();
        state.skipWS();
        Expression right = parseOperand(state);
        DataValueComparator cmp =
                DataType.getCommonSuperType(left.getOutputType(), right.getOutputType()).getComparator();
        switch (op) {
            case IN:
            case MISSING:
            case NOT:
            case AND:
            case OR:
            case XOR:
                throw new IllegalStateException("Already handled: " + op);
            case EQ:
                return m_factory.compare(left, right, cmp, 0);
            case LE:
                return m_factory.compare(left, right, cmp, -1, 0);
            case LT:
                return m_factory.compare(left, right, cmp, -1);
            case GE:
                return m_factory.compare(left, right, cmp, 0, 1);
            case GT:
                return m_factory.compare(left, right, cmp, 1);
            case LIKE:
                reportNotString(beforeLeft, left, op, beforeRight, right);
                return m_factory.like(left, right, null); //add keys when parsed
            case CONTAINS:
                reportNotString(beforeLeft, left, op, beforeRight, right);
                return m_factory.contains(left, right, null); //add keys when parsed
            case MATCHES:
                reportNotString(beforeLeft, left, op, beforeRight, right);
                return m_factory.matches(left, right, null); //add keys when parsed
            default:
                throw new ParseException("Not supported operator: " + op, beforeOperator);
        }
    }

    /**
     * Reports an error when one of the left or right argument has not a {@link StringCell} valued output.
     *
     * @param beforeLeft The position before parsed the {@code left} expression.
     * @param left The left {@link Expression}.
     * @param op The operator read.
     * @param beforeRight The position before parsed the {@code righ expression}.
     * @param right The right {@link Expression}.
     * @throws ParseException The type of the output is not a {@link StringValue}.
     */
    private void reportNotString(final int beforeLeft, final Expression left, final Operators op,
                                 final int beforeRight, final Expression right) throws ParseException {
        if (!StringCell.TYPE.isASuperTypeOf(left.getOutputType())) {
            throw new ParseException("Expression before '" + op + "' is not a string.", beforeLeft);
        }
        if (!StringCell.TYPE.isASuperTypeOf(right.getOutputType())) {
            throw new ParseException("Expression after '" + op + "' is not a string.", beforeRight);
        }
    }

    /**
     * Creates an {@link Expression} to compute a list. The format of the list is specified as: {@code (}operand (
     * {@code ,} operand) {@code )}
     *
     * @param state The {@link ParseState}.
     * @return The {@link Expression} parsed representing a list.
     * @throws ParseException Problem during parsing.
     */
    private Expression parseList(final ParseState state) throws ParseException {
        state.skipWS();
        List<Expression> operands = new ArrayList<Expression>();
        state.expect('(');
        state.consume();
        while (true) {
            operands.add(parseOperand(state));
            state.skipWS();
            if (state.peekChar() != ',') {
                break;
            }
            state.consume();
        }
        state.expect(')');
        state.consume();
        return m_factory.list(operands);
    }

    /**
     * Creates an {@link Expression} to compute a column, a {@link FlowVariable}, a {@link TableReference}, a number or
     * a {@link String}.
     *
     * @param state The {@link ParseState}.
     * @return The {@link Expression} parsed representing a column.
     * @throws ParseException Problem during parsing.
     */
    private Expression parseOperand(final ParseState state) throws ParseException {
        state.skipWS();
        Expression left;
        if (state.isColumnRef() || state.isFlowVariableRef() || state.isTablePropertyReference()) {
            left = parseReference(state, "");
        } else {
            final char ch = state.peekChar();
            if (ch == '"') {
                String text = state.readString();
                left = m_factory.constant(text, StringCell.TYPE);
            } else if (ch == '-' || (ch >= '0' && ch <= '9') || ch == '.'
                    || state.peekText(Double.toString(Double.POSITIVE_INFINITY))) {
                String text = state.parseNumber();
                try {
                    int integer = Integer.parseInt(text);
                    left = m_factory.constant(integer);
                } catch (NumberFormatException e) {
                    double real = Double.parseDouble(text);
                    left = m_factory.constant(real);
                }
            } else {
                throw new ParseException("Expected a number, string, column"
                        + (m_allowTableReferences ? ", a table property" : "") + " or flow variable reference.",
                        state.getPosition());
            }
        }
        return left;
    }

    /**
     * Creates an {@link Expression} to compute a column.
     *
     * @param state The {@link ParseState}.
     * @return The {@link Expression} parsed representing a column.
     * @throws ParseException Problem during parsing.
     */
    private Expression parseColumnExpression(final ParseState state) throws ParseException {
        state.skipWS();
        int startPos = state.getPosition();
        if (state.isColumnRef()) {
            String columnRef = state.readColumnRef();
            try {
                return m_factory.columnRef(m_spec, columnRef);
            } catch (IllegalStateException e) {
                throw new ParseException(e.getMessage(), startPos);
            }
        }
        throw new ParseException("Expected a column reference", state.getPosition());
    }

    /**
     * Creates an {@link Expression} to compute a {@link FlowVariable}.
     *
     * @param state The {@link ParseState}.
     * @return The {@link Expression} parsed representing a {@link FlowVariable}.
     * @throws ParseException Problem during parsing.
     */
    private Expression parseFlowVariableExpression(final ParseState state) throws ParseException {
        state.skipWS();
        int startPos = state.getPosition();
        if (state.isFlowVariableRef()) {
            String flowVarRef = state.readFlowVariable();
            try {
                return m_factory.flowVarRef(m_flowVariables, flowVarRef);
            } catch (IllegalStateException e) {
                throw new ParseException(e.getMessage(), startPos);
            }
        }
        throw new ParseException("Expected a flow variable reference", state.getPosition());
    }

    /**
     * Creates an {@link Expression} to compute a {@link TableReference}.
     *
     * @param state The {@link ParseState}.
     * @return The {@link Expression} parsed representing a {@link TableReference}.
     * @throws ParseException Problem during parsing.
     */
    private Expression parseTablePropertyReference(final ParseState state) throws ParseException {
        state.skipWS();
        String reference = state.readTablePropertyReference();
        if (org.knime.ext.sun.nodes.script.expression.Expression.ROWCOUNT.equals(reference)) {
            return m_factory.tableRef(TableReference.RowCount);
        }
        if (org.knime.ext.sun.nodes.script.expression.Expression.ROWINDEX.equals(reference)) {
            return m_factory.tableRef(TableReference.RowIndex);
        }
        if (org.knime.ext.sun.nodes.script.expression.Expression.ROWID.equals(reference)) {
            return m_factory.tableRef(TableReference.RowId);
        }
        throw new ParseException("Expected a table property reference", state.getPosition());
    }
}
