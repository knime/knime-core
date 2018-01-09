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

import java.util.Collections;
import java.util.Map;

import org.knime.base.node.rules.engine.Condition.MatchOutcome.CommentOutcome;
import org.knime.base.node.rules.engine.Condition.MatchOutcome.MatchState;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;

/**
 * Condition for a rule.
 *
 * @author Gabor Bakos
 * @since 2.8
 */
public interface Condition extends Line {
    /**
     * The possible outcome for a match on a condition for a rule.
     *
     * @author Gabor Bakos
     * @since 2.8
     */
    interface MatchOutcome {
        /**
         * After a match what should happen. Currently only {@value #matchedAndStop} and {@value #nonMatched} are used
         * for regular {@link MatchOutcome}s and {@value #skipped} for comments.
         *
         * @author Gabor Bakos
         * @since 2.8
         */
        public static enum MatchState {
            /** We have a match no need to check other rules. */
            matchedAndStop,
            /** Match, but the outcome is probably just a logging. */
            matchedAndContinue,
            /** Comment, or nonfatal exception. */
            skipped,
            /** This rule did not match. */
            nonMatched,
            /** Error, but can go to next input. */
            stopProcessingLine,
            /** Error, should stop processing lines. */
            stopProcessing;
        }

        /**
         * @return The result after a match attempted.
         */
        MatchState getOutcome();

        /**
         * The {@link Expression}s might generate objects based on the rule and the other inputs. This method provides
         * the result of it.
         *
         * @return The matched objects.
         */
        Map<String, Map<String, String>> getMatchedObjects();

        /**
         * For comments this is a default implementation, you can get the singleton instance with {@link #getInstance()}
         * .
         *
         * @author Gabor Bakos
         * @since 2.8
         */
        class CommentOutcome implements MatchOutcome {
            private static final CommentOutcome INSTANCE = new CommentOutcome();

            private CommentOutcome() {
                super();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public MatchState getOutcome() {
                return MatchState.skipped;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Map<String, Map<String, String>> getMatchedObjects() {
                return Collections.emptyMap();
            }

            /**
             * @return the singleton instance
             */
            public static CommentOutcome getInstance() {
                return INSTANCE;
            }
        }

        /**
         * {@link MatchOutcome} basic implementation.
         *
         * @author Gabor Bakos
         * @since 2.8
         */
        class GenericMatchOutcome implements MatchOutcome {
            private final MatchState m_state;

            private final Map<String, Map<String, String>> m_matchedObjects;

            /**
             * Constructs {@link GenericMatchOutcome}.
             *
             * @param state The resulting {@link MatchState}.
             * @param matchedObjects The matched objects.
             */
            public GenericMatchOutcome(final MatchState state, final Map<String, Map<String, String>> matchedObjects) {
                super();
                this.m_state = state;
                this.m_matchedObjects =
                    matchedObjects.isEmpty() ? Collections.<String, Map<String, String>> emptyMap() : Collections
                        .unmodifiableMap(Util.clone(matchedObjects));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public MatchState getOutcome() {
                return m_state;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Map<String, Map<String, String>> getMatchedObjects() {
                return m_matchedObjects;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "GenericMatchOutcome [m_state=" + m_state + ", m_matchedObjects=" + m_matchedObjects + "]";
            }
        }
    }

    /**
     * Evaluates whether the current row and the variables match the {@link Condition} or not.
     *
     * @param row The current {@link DataRow}.
     * @param provider A {@link VariableProvider}.
     * @return The result of the match. It can include regular expression match groups and signals to terminate the
     *         whole execution.
     */
    MatchOutcome matches(DataRow row, VariableProvider provider);

    /**
     * @return The part of {@link #getLine()} that was used to create the {@link Condition}.
     */
    String getText();

    /**
     * Default implementation of {@link Condition} for comments.
     *
     * @author Gabor Bakos
     * @since 2.8
     */
    class Comment implements Condition {
        private final String m_line;

        /**
         * A rule which is comment.
         *
         * @param line A line with starting {@code //}.
         */
        public Comment(final String line) {
            this.m_line = line;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isEnabled() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return m_line;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public MatchOutcome matches(final DataRow row, final VariableProvider provider) {
            return CommentOutcome.getInstance();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getLine() {
            return m_line;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getText() {
            return m_line;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isCatchAll() {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isConstantFalse() {
            return false;
        }
    }

    /**
     * Default implementation of {@link Condition}.
     *
     * @author Gabor Bakos
     * @since 2.8
     */
    class GenericCondition implements Condition {
        private final String m_line;

        private final String m_text;

        private final boolean m_enabled;

        private final Expression m_expression;

        /**
         * Constructs a {@link GenericCondition}.
         *
         * @param line The line of the rule.
         * @param text The text that the condition is made from.
         * @param enabled Whether this is enabled (for comments this should be {@code false}, and {@link Comment} is
         *            preferred).
         * @param expression The {@link Expression} that should be used to evaluate the match. It should return
         *            {@link BooleanValue} results in {@link ExpressionValue#getValue()}.
         */
        public GenericCondition(final String line, final String text, final boolean enabled, final Expression expression) {
            this.m_line = line;
            this.m_text = text;
            this.m_enabled = enabled;
            this.m_expression = expression;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isEnabled() {
            return m_enabled;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getLine() {
            return m_line;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getText() {
            return m_text;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public MatchOutcome matches(final DataRow row, final VariableProvider provider) {
            final ExpressionValue value = m_expression.evaluate(row, provider);
            if (value.getValue().isMissing()) {
                return new MatchOutcome.GenericMatchOutcome(MatchState.skipped,
                    Collections.<String, Map<String, String>> emptyMap());
            }
            final DataCell cell = value.getValue();
            if (cell instanceof BooleanValue) {
                final BooleanValue bv = (BooleanValue)cell;
                if (bv.getBooleanValue()) {
                    return new MatchOutcome.GenericMatchOutcome(MatchState.matchedAndStop, value.getMatchedObjects());
                }
                return new MatchOutcome.GenericMatchOutcome(MatchState.nonMatched,
                    Collections.<String, Map<String, String>> emptyMap());
            }
            return new MatchOutcome.GenericMatchOutcome(MatchState.stopProcessing, value.getMatchedObjects());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isCatchAll() {
            return checkValue(true);
        }

        /**
         * @return The <em>constant</em> value is equals to {@code value}, else false.
         * @see #isCatchAll()
         * @see #isConstantFalse()
         */
        private boolean checkValue(final boolean value) {
            if (m_expression.isConstant()) {
                DataCell rawValue = m_expression.evaluate(null, null).getValue();
                if (rawValue instanceof BooleanValue) {
                    BooleanValue bv = (BooleanValue)rawValue;
                    return bv.getBooleanValue() == value;
                }
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isConstantFalse() {
            return checkValue(false);
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

    /**
     * @return {@code true}, iff the condition is known to be a constant true.
     */
    boolean isCatchAll();

    /**
     * @return {@code true}, iff the condition is known to be a constant false.
     */
    boolean isConstantFalse();
}
