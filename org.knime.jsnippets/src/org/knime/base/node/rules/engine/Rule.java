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

import org.knime.base.node.rules.engine.Rule.Outcome.NoOutcome;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;

/**
 * An interface representing the rules in the rules engine.
 *
 * @author Gabor Bakos
 * @since 2.8
 */
public interface Rule {
    /**
     * @return The {@link Condition} of the {@link Rule}.
     */
    Condition getCondition();

    /**
     * @return The {@link Outcome} of rule application.
     */
    Outcome getOutcome();

    /**
     * This interface specifies how the outcome can be computed for a {@link Rule}.
     *
     * @author Gabor Bakos
     * @since 2.8
     */
    interface Outcome {
        /**
         * Computes the resulting cell using the {@code row} and the {@code provider}.
         *
         * @param row The {@link DataRow}.
         * @param provider The {@link VariableProvider}.
         * @return The computed result.
         */
        DataValue getComputedResult(DataRow row, VariableProvider provider);

        /**
         * @return Type of result that will be returned by {@link #getComputedResult(DataRow, VariableProvider)}.
         */
        DataType getType();

        /**
         * This is the default implementation when no outcome is requested.
         * @see #getInstance()
         *
         * @author Gabor Bakos
         * @since 2.8
         */
        final class NoOutcome implements Outcome {
            private static final NoOutcome INSTANCE = new NoOutcome();

            /**
             * Hide the constructor.
             */
            private NoOutcome() {
                super();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public DataValue getComputedResult(final DataRow row, final VariableProvider provider) {
                return DataType.getMissingCell();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public DataType getType() {
                return DataType.getMissingCell().getType();
            }

            /**
             * @return the instance
             */
            public static NoOutcome getInstance() {
                return INSTANCE;
            }
        }

        /**
         * A basic implementation of {@link Outcome} based on {@link Expression}.
         *
         * @author Gabor Bakos
         * @since 2.8
         */
        static class GenericOutcome implements Outcome {
            private final Expression m_expression;

            /**
             * Creates a new {@link GenericOutcome}.
             *
             * @param expression The {@link Expression} computing the result.
             */
            public GenericOutcome(final Expression expression) {
                super();
                m_expression = expression;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public DataValue getComputedResult(final DataRow row, final VariableProvider provider) {
                return m_expression.evaluate(row, provider).getValue();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public DataType getType() {
                return m_expression.getOutputType();
            }

            /**
             * @return the expression
             * @since 3.2
             */
            Expression getExpression() {
                return m_expression;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return m_expression.toString();
            }

        }

    }

    /**
     * Reference to something with a type.
     *
     * @author Gabor Bakos
     * @since 2.8
     */
    public interface Reference {
        /**
         * @return The type of data referenced.
         */
        DataType getDataType();
    }

    /**
     * Reference to a table property.
     *
     * @author Gabor Bakos
     * @since 2.8
     */
    static enum TableReference implements Reference {
        /** Refers to the row index (the first row is {@code 0L}). */
        RowIndex(org.knime.ext.sun.nodes.script.expression.Expression.ROWINDEX, LongCell.TYPE),
        /** The row key's string representation. */
        RowId(org.knime.ext.sun.nodes.script.expression.Expression.ROWID, StringCell.TYPE),
        /** All rows in the table. */
        RowCount(org.knime.ext.sun.nodes.script.expression.Expression.ROWCOUNT, LongCell.TYPE);
        private final String m_name;

        private final DataType m_dataType;

        private TableReference(final String name, final DataType type) {
            this.m_name = name;
            this.m_dataType = type;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "$$" + m_name + "$$";
        }

        /**
         * @return the name
         */
        public String getName() {
            return m_name;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataType getDataType() {
            return m_dataType;
        }
    }

    /**
     * Default implementation of the {@link Rule} interface.
     *
     * @author Gabor Bakos
     * @since 2.8
     */
    static class GenericRule implements Rule {
        private final String m_text;

        private final Condition m_condition;

        private final Outcome m_outcome;

        /**
         * Constructs a rule with the text of the rule, the {@link Condition} and the {@link Outcome} without
         * {@link SideEffect}s.
         *
         * @param text The text from which the rule was parsed.
         * @param condition The parsed {@link Condition}.
         * @param outcome The parsed {@link Outcome}.
         */
        GenericRule(final String text, final Condition condition, final Outcome outcome) {
            super();
            this.m_text = text;
            this.m_condition = condition;
            this.m_outcome = outcome == null ? NoOutcome.getInstance() : outcome;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Condition getCondition() {
            return m_condition;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Outcome getOutcome() {
            return m_outcome;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return getCondition() + " => " + getOutcome() + "\nRaw: " + m_text;
        }
    }

    /**
     * Enumeration for all possible operators used in a rule.
     *
     * @author Thorsten Meinl, University of Konstanz
     * @author Gabor Bakos
     * @since 2.8
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
        /** Regex matches. */
        MATCHES,
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
    /**
     * The boolean constants for rules.
     *
     * @since 2.9
     */
    public enum BooleanConstants {
        /** The representation of {@code false}. */
        FALSE,
        /** The representation of {@code true}. */
        TRUE;
    }
}
