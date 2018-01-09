/*
 * ------------------------------------------------------------------------
 *
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
 * History
 *   16. Dec. 2015. (Gabor Bakos): created
 */
package org.knime.base.node.rules.engine;

import java.text.ParseException;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import org.knime.base.node.rules.engine.Condition.GenericCondition;
import org.knime.base.node.rules.engine.Expression.ASTType;
import org.knime.base.node.rules.engine.Rule.GenericRule;
import org.knime.base.node.rules.engine.Rule.Outcome;
import org.knime.base.node.rules.engine.Rule.Outcome.GenericOutcome;
import org.knime.base.node.rules.engine.Rule.TableReference;
import org.knime.core.data.DataTableSpec;

/**
 * Some helper methods for supporting streaming execution, like
 * <ul>
 * <li>{@link #isStreamableExpression(Expression) checking for streamable expressions}</li>
 * <li>{@link #isStreamableRule(Rule) checking for streamable rules}</li>
 * </ul>
 *
 * @author Gabor Bakos
 */
public class StreamingUtil {

    /**
     * Hidden constructor.
     */
    private StreamingUtil() {
        super();
    }

    /**
     * Contains row count or not?
     *
     * @param expression An {@link Expression}.
     * @return Whether it can be used in streaming computation or not.
     */
    public static boolean isStreamableExpression(final Expression expression) {
        if (expression.getTreeType() == ASTType.TableRef && expression.toString().equals(TableReference.RowCount.toString())) {
            //row count
            return false;
        }
        return expression.getChildren().stream().allMatch(c -> isStreamableExpression(c));
    }

    /**
     * Contains row index or not?
     *
     * @param expression An {@link Expression}.
     * @return Whether it can be used in distributed computation or not.
     */
    public static boolean isDistributableExpression(final Expression expression) {
        if (expression.getTreeType() == ASTType.TableRef && expression.toString().equals(TableReference.RowIndex.toString())) {
            //row index
            return false;
        }
        return expression.getChildren().stream().allMatch(c -> isDistributableExpression(c));
    }

    /**
     * Contains row count or not?
     *
     * @param rule A {@link Rule}.
     * @return All of its components are streamable or not.
     */
    public static boolean isStreamableRule(final Rule rule) {
        return checkExpressions(rule, StreamingUtil::isStreamableExpression);
    }

    /**
     * Contains row index or not?
     *
     * @param rule A {@link Rule}.
     * @return All of its components are distributable or not.
     */
    public static boolean isDistributableRule(final Rule rule) {
        return checkExpressions(rule, StreamingUtil::isDistributableExpression);
    }

    private static boolean checkExpressions(final Rule rule, final Predicate<Expression> check) {
        if (!rule.getCondition().isEnabled()) {
            //comment
            return true;
        }
        if (rule instanceof GenericRule) {
            final GenericRule gr = (GenericRule)rule;
            final Condition condition = gr.getCondition();
            if (condition instanceof GenericCondition) {
                final GenericCondition gc = (GenericCondition)condition;
                final Expression ce = gc.getExpression();
                final Outcome outcome = gr.getOutcome();
                if (outcome instanceof GenericOutcome) {
                    final GenericOutcome go = (GenericOutcome)outcome;
                    final Expression oe = go.getExpression();
                    return check.test(oe) && check.test(ce);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * @param ruleFactory The appropriate {@link RuleFactory}.
     * @param rules The actual rules.
     * @return Has any non-streamable rules or not?
     */
    public static boolean hasNonStreamableRule(final RuleFactory ruleFactory, final Iterable<String> rules) {
        return checkRules(ruleFactory, rules, StreamingUtil::isStreamableRule);
    }

    /**
     * @param ruleFactory The appropriate {@link RuleFactory}.
     * @param rules The actual rules.
     * @return Has any non-distributable rules or not?
     */
    public static boolean hasNonDistributableRule(final RuleFactory ruleFactory, final Iterable<String> rules) {
        return checkRules(ruleFactory, rules, StreamingUtil::isDistributableRule);
    }
    private static boolean checkRules(final RuleFactory ruleFactory, final Iterable<String> rules, final Predicate<Rule> ruleIsGood) {
        final RuleFactory proper = ruleFactory.cloned();
        proper.disableColumnChecks();
        proper.disableFlowVariableChecks();
        final DataTableSpec empty = new DataTableSpec();
        return StreamSupport.stream(rules.spliterator(), true).anyMatch(ruleAsString -> {
            try {
                return !ruleIsGood.test(proper.parse(ruleAsString, empty, Collections.emptyMap()));
            } catch (ParseException e) {
                return true;
            }
        });
    }
}
