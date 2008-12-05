/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
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
 *   11.04.2008 (thor): created
 */
package org.knime.base.node.rules;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;

/**
 * A rule consists of a condition part (antecedant) and an outcome (consequent).
 * The condition may consists of arithmetic or string comparisons combined by
 * boolean operators. The outcome is a simple string. The grammar for a rule is
 * as follow:
 *
 * <pre>
 * RULE := BEXPR '=&gt;' STRING
 * BEXPR := '(' BEXPR ')' |
 *          'NOT' BEXPR |
 *          'MISSING' COL |
 *          AEXPR (BINOP BEXPR)?
 * AEXPR := COL OP COL |
 *          NUMBER OP COL |
 *          COL OP NUMBER |
 *          STRING OP COL |
 *          COL OP STRING |
 *          COL LOP STRINGLIST
 * BOP := 'AND' | 'OR' | 'XOR'
 * OP := '&gt;' | '&lt;' | '&gt;=' | '&lt;=' | '=' | 'LIKE'
 * LOP := 'IN'
 * STRING := '&quot;' [&circ;&quot;]* '&quot;'
 * NUMBER := [1-9][0-9]*(\.[0-9]+)?
 * COL := '$' [&circ;$]+ '$'
 * STRINGLIST := '(' STRING (',' STRING)* ')'
 * </pre>
 *
 * The operators should be self-describing, if not look them up in SQL ;-)
 * <br />
 * While parsing a rule, the parser also checks if the arithmetic operations are
 * in fact done with numerical columns and throws an exception if not. Therefore
 * the constructor needs the spec of the table on which the rule will lateron be
 * used.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class Rule {
    /**
     * Enumeration for all possible operators used in a rule.
     *
     * @author Thorsten Meinl, University of Konstanz
     */
    public enum Operators {
        /** Numeric greater than. */
        GT(">"),
        /** Numeric lower than. */
        LT("<"),
        /** Numeric greater than or equal. */
        GE(">="),
        /** Numeric lower than or equal. */
        LE("<="),
        /** Numeric or string equal. */
        EQ("="),
        /** Boolean AND. */
        AND,
        /** Boolean OR. */
        OR,
        /** Boolean XOR. */
        XOR,
        /** Boolean NOT. */
        NOT,
        /** Test for missing value. */
        MISSING,
        /** Wildcard matching (* and ? as wildcards). */
        LIKE,
        /** Set matching. */
        IN;

        private final String m_represent;

        private Operators(final String represent) {
            m_represent = represent;
        }

        private Operators() {
            m_represent = null;
        }

        /**
         *
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            if (m_represent != null) {
                return m_represent;
            }
            return super.toString();
        }
    }

    private final String m_outcome;

    private final String m_condition;

    private final RuleNode m_root;

    private char[] s;

    private int p;

    /**
     * Creates a new rule by parsing a rule string.
     *
     * @param rule the rule string
     * @param spec the spec of the table on which the rule will be applied.
     * @throws ParseException if the rule contains a syntax error
     */
    public Rule(final String rule, final DataTableSpec spec)
            throws ParseException {
        s = rule.toCharArray();

        try {
            m_root = parseBooleanExpression(spec);
            m_condition = rule.substring(0, p);
            skipWS();
            expect('=');
            expect('>');
            skipWS();

            m_outcome = parseString();
            if (p < s.length) {
                throw new ParseException("Garbage at end of rule detected", p);
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            ParseException e =
                    new ParseException("Unexpected end of rule string", p);
            e.setStackTrace(ex.getStackTrace());
            throw e;
        }
    }

    /**
     * Parses a boolean expression (<tt>BEXPR</tt>).
     *
     * @param spec the table spec
     * @return a rule node for the parsed expression
     * @throws ParseException if rule contains a syntax error
     */
    private RuleNode parseBooleanExpression(final DataTableSpec spec)
            throws ParseException {
        skipWS();
        RuleNode leftNode;
        if (s[p] == '(') {
            p++;
            leftNode = parseBooleanExpression(spec);
            skipWS();
            expect(')');
        } else if (s[p] == 'N') {
            p++;
            expect('O');
            expect('T');
            leftNode = parseBooleanExpression(spec);
        } else if (s[p] == 'M') {
            p++;
            expect('I');
            expect('S');
            expect('S');
            expect('I');
            expect('N');
            expect('G');
            skipWS();
            int col = parseColumn(spec);
            return RuleNodeFactory.missing(col);
        } else {
            leftNode = parseArithmeticExpression(spec);
        }

        skipWS();
        if (s[p] == 'A') {
            p++;
            expect('N');
            expect('D');
            RuleNode rightNode = parseBooleanExpression(spec);
            return RuleNodeFactory.and(leftNode, rightNode);
        } else if (s[p] == 'O') {
            p++;
            expect('R');
            RuleNode rightNode = parseBooleanExpression(spec);
            return RuleNodeFactory.or(leftNode, rightNode);
        } else if (s[p] == 'X') {
            p++;
            expect('O');
            expect('R');
            RuleNode rightNode = parseBooleanExpression(spec);
            return RuleNodeFactory.xor(leftNode, rightNode);
        } else {
            return leftNode;
        }
    }

    /**
     * Parses an arithmetic expression (<tt>AEXPR</tt>).
     *
     * @param spec the table spec
     * @return a rule node for the parsed expression
     * @throws ParseException if rule contains a syntax error
     */
    private RuleNode parseArithmeticExpression(final DataTableSpec spec)
            throws ParseException {
        skipWS();
        if (s[p] == '$') {
            int start = p;
            int leftColIndex = parseColumn(spec);
            Operators op = parseOperator();
            skipWS();
            if (op == Operators.IN) {
                return RuleNodeFactory.in(leftColIndex, parseStringList());
            } else if (s[p] == '$') {
                int rightColIndex = parseColumn(spec);

                DataColumnSpec leftSpec = spec.getColumnSpec(leftColIndex);
                DataColumnSpec rightSpec = spec.getColumnSpec(rightColIndex);
                DataType commonType =
                        DataType.getCommonSuperType(leftSpec.getType(),
                                rightSpec.getType());
                DataValueComparator comp = commonType.getComparator();

                switch (op) {
                    case EQ:
                        return RuleNodeFactory.eq(leftColIndex, rightColIndex);
                    case GE:
                        return RuleNodeFactory.ge(leftColIndex, rightColIndex,
                                comp);
                    case GT:
                        return RuleNodeFactory.gt(leftColIndex, rightColIndex,
                                comp);
                    case LE:
                        return RuleNodeFactory.le(leftColIndex, rightColIndex,
                                comp);
                    case LT:
                        return RuleNodeFactory.lt(leftColIndex, rightColIndex,
                                comp);
                    default:
                        throw new IllegalStateException("Unhandeled operator "
                                + op);
                }
            } else if (s[p] == '"') {
                String t = parseString();
                switch (op) {
                    case EQ:
                        return RuleNodeFactory.eq(leftColIndex, t);
                    case GE:
                        return RuleNodeFactory.ge(leftColIndex, t);
                    case GT:
                        return RuleNodeFactory.gt(leftColIndex, t);
                    case LE:
                        return RuleNodeFactory.le(leftColIndex, t);
                    case LT:
                        return RuleNodeFactory.lt(leftColIndex, t);
                    case LIKE:
                        return RuleNodeFactory.like(leftColIndex, t);
                    default:
                        throw new IllegalStateException("Unhandeled operator "
                                + op);
                }
            } else {
                Number n = parseNumber();
                DataType leftType = spec.getColumnSpec(leftColIndex).getType();
                if (!leftType.isCompatible(DoubleValue.class)) {
                    throw new ParseException(spec.getColumnSpec(leftColIndex)
                            .getName()
                            + " is not a numeric column", start);
                }

                if ((n instanceof Integer)
                        && !leftType.isCompatible(IntValue.class)) {
                    n = new Double(n.doubleValue());
                }
                switch (op) {
                    case EQ:
                        return RuleNodeFactory.eq(leftColIndex, n);
                    case GE:
                        return RuleNodeFactory.ge(leftColIndex, n);
                    case GT:
                        return RuleNodeFactory.gt(leftColIndex, n);
                    case LE:
                        return RuleNodeFactory.le(leftColIndex, n);
                    case LT:
                        return RuleNodeFactory.lt(leftColIndex, n);
                    default:
                        throw new IllegalStateException("Unhandeled operator "
                                + op);
                }
            }
        } else if (s[p] == '"') {
            String t = parseString();
            Operators op = parseOperator();
            int rightColIndex = parseColumn(spec);
            switch (op) {
                case EQ:
                    return RuleNodeFactory.eq(rightColIndex, t);
                case GE:
                    return RuleNodeFactory.lt(rightColIndex, t);
                case GT:
                    return RuleNodeFactory.le(rightColIndex, t);
                case LE:
                    return RuleNodeFactory.gt(rightColIndex, t);
                case LT:
                    return RuleNodeFactory.ge(rightColIndex, t);
                case LIKE:
                    return RuleNodeFactory.like(t, rightColIndex);
                default:
                    throw new IllegalStateException("Unhandeled operator " + op);
            }
        } else if ((s[p] >= '0') && (s[p] <= '9')) {
            Number n = parseNumber();
            Operators op = parseOperator();
            int start = p;
            int rightColIndex = parseColumn(spec);

            DataType rightType = spec.getColumnSpec(rightColIndex).getType();
            if (!rightType.isCompatible(DoubleValue.class)) {
                throw new ParseException(spec.getColumnSpec(rightColIndex)
                        .getName()
                        + " is not a numeric column", start);
            }

            if ((n instanceof Integer)
                    && !rightType.isCompatible(IntValue.class)) {
                n = new Double(n.doubleValue());
            }

            switch (op) {
                case EQ:
                    return RuleNodeFactory.eq(rightColIndex, n);
                case GE:
                    return RuleNodeFactory.lt(rightColIndex, n);
                case GT:
                    return RuleNodeFactory.le(rightColIndex, n);
                case LE:
                    return RuleNodeFactory.gt(rightColIndex, n);
                case LT:
                    return RuleNodeFactory.ge(rightColIndex, n);
                default:
                    throw new IllegalStateException("Unhandeled operator " + op);
            }
        } else {
            throw new ParseException(
                    "Expected a column name, a string or a number", p);
        }
    }

    /**
     * Parses a decimal number (<tt>NUMBER</tt>).
     *
     * @return the number
     * @throws ParseException if a syntax error has been found
     */
    private Number parseNumber() throws ParseException {
        skipWS();
        int n = 0;
        int sign = 1;
        if (s[p] == '-') {
            p++;
            sign = -1;
        }
        while ((s[p] >= '0') && (s[p] <= '9')) {
            n *= 10;
            n += s[p] - '0';
            p++;
        }

        if (s[p] == '.') {
            p++;
            int f = 0;
            long digits = 1;
            while ((s[p] >= '0') && (s[p] <= '9')) {
                f *= 10;
                f += s[p] - '0';
                p++;
                digits *= 10;
            }
            return new Double(sign * (n + f / (double)digits));
        } else {
            return new Integer(sign * n);
        }
    }

    /**
     * Parses any of the operators (<tt>OP</tt> <tt>BOP</tt>, <tt>LOP</tt>).
     *
     * @return an operator
     * @throws ParseException if a syntax error has been found
     */
    private Operators parseOperator() throws ParseException {
        skipWS();
        int start = p;
        if (s[p] == '=') {
            p++;
            return Operators.EQ;
        } else if (s[p] == '>') {
            p++;
            if (s[p] == '=') {
                p++;
                return Operators.GE;
            } else {
                return Operators.GT;
            }
        } else if (s[p] == '<') {
            p++;
            if (s[p] == '=') {
                p++;
                return Operators.LE;
            } else {
                return Operators.LT;
            }
        } else if (s[p] == 'L') {
            p++;
            expect('I');
            expect('K');
            expect('E');
            return Operators.LIKE;
        } else if (s[p] == 'I') {
            p++;
            expect('N');
            return Operators.IN;
        } else {
            throw new ParseException("Expected one of [<, >, <=, >=, =]", start);
        }
    }

    /**
     * Parses a column reference (<tt>COL</tt>).
     *
     * @param spec the table spec
     * @return a rule node for the parsed expression
     * @throws ParseException if a syntax error has been found
     */

    private int parseColumn(final DataTableSpec spec) throws ParseException {
        expect('$');
        int start = p;
        while (s[p] != '$') {
            p++;
        }
        String colName = new String(s, start, p++ - start);
        int colIndex = spec.findColumnIndex(colName);
        if (colIndex == -1) {
            throw new ParseException("Column '" + colName + "' does not exist",
                    start);
        }
        return colIndex;
    }

    /**
     * Parses a list of strings (<tt>STRINGLIST</tt>).
     *
     * @return a list of strings
     *
     * @throws ParseException if a syntax error has been found
     */
    private List<String> parseStringList() throws ParseException {
        ArrayList<String> list = new ArrayList<String>();
        skipWS();
        expect('(');
        while (true) {
            skipWS();
            list.add(parseString());
            skipWS();
            if (s[p] != ',') {
                break;
            }
            p++;
        }
        expect(')');
        return list;
    }

    /**
     * Skips whitespaces in the input.
     */
    private void skipWS() {
        while ((s[p] == ' ') || (s[p] == '\n') || (s[p] == '\r')
                || (s[p] == '\t')) {
            p++;
        }
    }

    /**
     * Checks if the next character is any of the ones in the list and eats it
     * up. If not, an exception is thrown.
     *
     * @param cs a list of expected characters
     * @return the character
     * @throws ParseException if none of the expected character matched
     */
    private char expect(final char... cs) throws ParseException {
        for (char c : cs) {
            if (s[p] == c) {
                return s[p++];
            }
        }
        if (cs.length == 1) {
            throw new ParseException("Expected '" + cs[0] + "' but found '"
                    + s[p] + "'", p);
        } else {
            throw new ParseException("Expected one of " + Arrays.toString(cs)
                    + " but found '" + s[p] + "'", p);
        }
    }

    /**
     * Parses a string (<tt>STRING</tt>).
     *
     * @return a string
     * @throws ParseException if a syntax error has been found.
     */
    private String parseString() throws ParseException {
        expect('"');
        int start = p;
        while (s[p] != '"') {
            p++;
        }
        return new String(s, start, p++ - start);
    }

    /**
     * Returns if this rules matches the given row.
     *
     * @param row a data row
     * @return <code>true</code> if the rule matches, <code>false</code>
     *         otherwise
     */
    public boolean matches(final DataRow row) {
        return m_root.evaluate(row);
    }

    /**
     * Returns the rule's outcome (consequent).
     *
     * @return a string
     */
    public String getOutcome() {
        return m_outcome;
    }

    /**
     * Returns the rule's condition (antecedant).
     *
     * @return the condition
     */
    public String getCondition() {
        return m_condition;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_condition + " => \"" + m_outcome + "\"";
    }

    /**
     * Returns the parsed rule in string representation. This representation
     * is not completely identical to the input rule string, but the logical
     * structure should match.
     *
     * @return a string representation of this rule
     */
    public String serialize() {
        return m_root.toString() + " => \"" + m_outcome + "\"";
    }

    /**
     * Zum Testen...
     *
     * @param args Pieps
     * @throws Exception Tröt
     */
    public static void main(final String[] args) throws Exception {
        DataColumnSpec[] colSpecs =
                {
                        new DataColumnSpecCreator("A", IntCell.TYPE)
                                .createSpec(),
                        new DataColumnSpecCreator("B", IntCell.TYPE)
                                .createSpec(),
                        new DataColumnSpecCreator("C", IntCell.TYPE)
                                .createSpec(),
                        new DataColumnSpecCreator("S", StringCell.TYPE)
                                .createSpec(),
                        new DataColumnSpecCreator("X", DoubleCell.TYPE)
                                .createSpec(),
                        new DataColumnSpecCreator("Y", DoubleCell.TYPE)
                                .createSpec(),
                        new DataColumnSpecCreator("Z", DoubleCell.TYPE)
                                .createSpec()};
        DataTableSpec ts = new DataTableSpec(colSpecs);

        BufferedReader in =
                new BufferedReader(new InputStreamReader(System.in));

        String line;
        while ((line = in.readLine()) != null) {
            try {
                Rule r = new Rule(line, ts);
                System.out.println(r.toString());
            } catch (ParseException ex) {
                ex.printStackTrace();
            }
        }
    }
}
