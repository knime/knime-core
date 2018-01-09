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
 * Created on 2013.08.12. by Gabor Bakos
 */
package org.knime.base.node.rules.engine;

import java.util.List;

import org.dmg.pmml.SimpleRuleDocument.SimpleRule;
import org.knime.base.node.rules.engine.pmml.PMMLExpressionFactory;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.BooleanCell;

/**
 * Common interface to create tree for the expressions. Those trees can evaluate the expressions for each row
 * {@link ExpressionFactory} for {@link Expression}s, or create the PMML model {@link PMMLExpressionFactory} for
 * {@link SimpleRule}s.
 *
 * @author Gabor Bakos
 * @param <BoolExpression> The type of the results.
 * @param <ReferenceExpression>
 */
public interface RuleExpressionFactory<BoolExpression, ReferenceExpression> {

    /**
     * Negates the result of an Expression.<br/>
     * No optimization is done for constant {@code BoolExpression}s.
     *
     * @param expressionToNegate A {@link BooleanValue} {@code BoolExpression}. If a non-Boolean and non-missing value
     *            is matched, an {@link IllegalStateException} will be thrown.
     * @return The negated expression.
     */
    public BoolExpression not(final BoolExpression expressionToNegate);

    /**
     * Creates a new {@code BoolExpression} with the conjunction of the arguments. Missing values will cause missing
     * value result, but a {@link BooleanCell#FALSE} will result in a {@value BooleanCell#FALSE} result because of the
     * short-circuit evaluation. When no arguments passed, a {@value Boolean#TRUE} result will be returned. <br/>
     * No optimization is done for constant {@code BoolExpression}s.
     *
     * @param boolExpressions Some {@code BoolExpression}s with {@link BooleanValue} result. If non-missing, non-Boolean
     *            expression results occur, an IllegalStateException will be thrown.
     * @return The conjunction of the arguments as a {@code BoolExpression}.
     */
    public BoolExpression and(final List<BoolExpression> boolExpressions);

    /**
     * Creates a new {@code BoolExpression} with the disjunction of the arguments. Missing values will cause missing
     * value result, but a {@link BooleanCell#TRUE} will result in a {@value BooleanCell#TRUE} result because of the
     * short-circuit evaluation. When no arguments passed, a {@value Boolean#FALSE} result will be returned. <br/>
     * No optimization is done for constant {@code BoolExpression}s.
     *
     * @param boolExpressions Some {@code BoolExpression}s with {@link BooleanValue} result. If non-missing, non-Boolean
     *            expression results occur, an IllegalStateException will be thrown.
     * @return The disjunction of the arguments as a {@code BoolExpression}.
     */
    public BoolExpression or(final List<BoolExpression> boolExpressions);

    /**
     * Creates a new {@code BoolExpression} with the exclusive or of the arguments. Missing values will cause missing
     * value result. When no arguments passed, an {@link IllegalStateException} will be thrown. <br/>
     * No optimization is done for Constant {@code BoolExpression}s.
     *
     * @param boolExpressions At least one {@code BoolExpression}s with {@link BooleanValue} result. If non-missing,
     *            non-Boolean expression results occur, an IllegalStateException will be thrown.
     * @return The conjunction of the arguments as a {@code BoolExpression}.
     * @throws IllegalStateException When no expression is specified.
     */
    public BoolExpression xor(final List<BoolExpression> boolExpressions);

    /**
     * Creates an {@code BoolExpression} for checking for missing values.
     *
     * @param reference Any non-{@code null} {@code ReferenceExpression}.
     * @return A {@code BoolExpression} which result {@link BooleanCell#TRUE} if the wrapped {@code ReferenceExpression}
     *         returns a missing value, and {@link BooleanCell#FALSE} if it is not missing.
     */
    public BoolExpression missing(final ReferenceExpression reference);

    /**
     * Creates an {@code BoolExpression} for checking for missing values.
     *
     * @param reference Any non-{@code null} {@code ReferenceExpression}.
     * @return An {@code BoolExpression} which result {@link BooleanCell#TRUE} if the wrapped {@code ReferenceExpression}
     *         returns a missing value, and {@link BooleanCell#FALSE} if it is not missing.
     */
    public BoolExpression missingBoolean(final ReferenceExpression reference);

    /**
     * Creates a new {@code BoolExpression} checking for inclusion in a collection. <br/>
     * It may perform constant optimization.
     *
     * @param left The left {@code ReferenceExpression} is what we are looking for.
     * @param right The right {@code ReferenceExpression} contains the possible values in a {@link CollectionDataValue} (
     *            {@link ListCell}).
     * @return A {@code BoolExpression} returning {@link BooleanCell#TRUE} the left value is contained in the right
     *         values, missing if the right value is missing and {@link BooleanCell#FALSE} if left is not contained in
     *         the right. (Throws {@link IllegalStateException} if the right operand is neither missing nor collection.)
     * @throws IllegalStateException If the right {@code ReferenceExpression} does not return a collection type.
     */
    public BoolExpression in(final ReferenceExpression left, final ReferenceExpression right);

    /**
     * Creates a comparison {@code BoolExpression} with the acceptable outcomes. The matched objects are combined (in
     * left-right order). <br/>
     * No optimization is done for constant {@code ReferenceExpression}s.
     *
     * @param left The {@code ReferenceExpression} representing the left operand.
     * @param right The {@code ReferenceExpression} of the right operand.
     * @param cmp The {@link DataValueComparator} to compare the {@code left} and {@code right} values.
     * @param possibleValues The signum values for the {@code cmp} outcomes: {@code -1} means left is smaller, {@code 0}
     *            - they are the same, {@code 1} - left is larger.
     * @return A {@code BoolExpression} comparing the {@code left} and {@code right} values. If the comparison from the
     *         {@code cmp} results in any of the {@code possibleValues} a {@link BooleanCell#TRUE} will be returned,
     *         else a {@link BooleanCell#FALSE}.
     */
    public BoolExpression compare(final ReferenceExpression left, final ReferenceExpression right,
        final DataValueComparator cmp, final int... possibleValues);

    /**
     * Constructs a wild-card matcher {@code BoolExpression}. <br/>
     * Constant optimization may be done on the {@code right} (pattern) {@code ReferenceExpression}.
     *
     * @param left A {@link StringValue}'d {@code ReferenceExpression}.
     * @param right Another {@link StringValue}'d {@code ReferenceExpression}, but this is for the wild-card pattern.
     * @param key The key for the matched objects. Can be {@code null}.
     * @return A {@code BoolExpression} representing an SQL-like LIKE operator.
     * @throws IllegalStateException When the constant right expression is an invalid pattern. (Most probably never.)
     */
    public BoolExpression like(final ReferenceExpression left, final ReferenceExpression right, final String key)
        throws IllegalStateException;

    /**
     * Constructs a regular expression {@line Matcher#find()}er {@code BoolExpression}. <br/>
     * Constant optimization may be done on the {@code right} (pattern) {@code ReferenceExpression}.
     *
     * @param left A {@link StringValue}'d {@code ReferenceExpression}.
     * @param right Another {@link StringValue}'d {@code ReferenceExpression}, but this is for the regular expression
     *            pattern.
     * @param key The key for the matched objects. Can be {@code null}.
     * @return A {@code BoolExpression} representing a regular expression finding operator.
     * @throws IllegalStateException When the constant right expression is an invalid pattern.
     */
    public BoolExpression contains(final ReferenceExpression left, final ReferenceExpression right, final String key)
        throws IllegalStateException;

    /**
     * Constructs a regular expression {@line Matcher#matches()} {@code BoolExpression}. <br/>
     * Constant optimization may be done on the {@code right} (pattern)
     * {@code ReferenceExpression}.
     *
     * @param left A {@link StringValue}'d {@code ReferenceExpression}.
     * @param right Another {@link StringValue}'d {@code ReferenceExpression}, but this is for the regular expression
     *            pattern.
     * @param key The key for the matched objects. Can be {@code null}.
     * @return A {@code BoolExpression} representing a regular expression matcher operator.
     * @throws IllegalStateException When the constant right expression is an invalid pattern.
     */
    public BoolExpression matches(final ReferenceExpression left, final ReferenceExpression right, final String key)
        throws IllegalStateException;

    /**
     * @return A constant {@code true} {@code BoolExpression}.
     */
    public BoolExpression trueValue();

    /**
     * @return A constant {@code false} {@code BoolExpression}.
     */
    public BoolExpression falseValue();

    /**
     * @param expression A Boolean-valued {@code ReferenceExpression}.
     * @return A {@code BoolExpression} wrapping the argument.
     */
    public BoolExpression boolAsPredicate(ReferenceExpression expression);
}
