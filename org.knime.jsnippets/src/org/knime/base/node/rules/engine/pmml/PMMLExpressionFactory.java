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
 * Created on 2013.08.10. by Gabor Bakos
 */
package org.knime.base.node.rules.engine.pmml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.knime.base.node.mine.decisiontree2.PMMLArrayType;
import org.knime.base.node.mine.decisiontree2.PMMLBooleanOperator;
import org.knime.base.node.mine.decisiontree2.PMMLCompoundPredicate;
import org.knime.base.node.mine.decisiontree2.PMMLFalsePredicate;
import org.knime.base.node.mine.decisiontree2.PMMLOperator;
import org.knime.base.node.mine.decisiontree2.PMMLPredicate;
import org.knime.base.node.mine.decisiontree2.PMMLSetOperator;
import org.knime.base.node.mine.decisiontree2.PMMLSimplePredicate;
import org.knime.base.node.mine.decisiontree2.PMMLSimpleSetPredicate;
import org.knime.base.node.mine.decisiontree2.PMMLTruePredicate;
import org.knime.base.node.rules.engine.Expression;
import org.knime.base.node.rules.engine.Expression.ASTType;
import org.knime.base.node.rules.engine.RuleEngineNodeModel;
import org.knime.base.node.rules.engine.RuleExpressionFactory;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;

/**
 * A {@link RuleExpressionFactory} for PMML expressions.
 *
 * @author Gabor Bakos
 */
public class PMMLExpressionFactory implements RuleExpressionFactory<PMMLPredicate, Expression> {
    private Set<String> m_usedColumns = new LinkedHashSet<String>();

    /**
     * {@inheritDoc}
     */
    @Override
    public PMMLPredicate not(final PMMLPredicate expressionToNegate) {
        return xor(Arrays.asList(trueValue(), expressionToNegate));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PMMLPredicate and(final List<PMMLPredicate> boolExpressions) {
        return createConnective(boolExpressions, PMMLBooleanOperator.AND);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PMMLPredicate or(final List<PMMLPredicate> boolExpressions) {
        return createConnective(boolExpressions, PMMLBooleanOperator.OR);
    }

    /**
     * Creates a "predicate" from logical connectives.
     *
     * @param boolExpressions The expressions to combine.
     * @param op The operator.
     * @return The {@link PMMLRuleCompoundPredicate} representing the arguments.
     */
    private PMMLPredicate createConnective(final List<PMMLPredicate> boolExpressions, final PMMLBooleanOperator op) {
        final PMMLCompoundPredicate ret = new PMMLCompoundPredicate(op);
        ret.setPredicates(new LinkedList<PMMLPredicate>(boolExpressions));
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PMMLPredicate xor(final List<PMMLPredicate> boolExpressions) {
        return createConnective(boolExpressions, PMMLBooleanOperator.XOR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PMMLPredicate missing(final Expression reference) {
        m_usedColumns.add(expressionToString(reference));
        return new PMMLSimplePredicate(expressionToString(reference), PMMLOperator.IS_MISSING, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PMMLPredicate missingBoolean(final Expression reference) {
        return missing(reference);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PMMLPredicate in(final Expression left, final Expression right) {
        PMMLSimpleSetPredicate setIn = new PMMLSimpleSetPredicate(expressionToString(left), PMMLSetOperator.IS_IN);
        if (left.getTreeType() == ASTType.ColRef) {
            m_usedColumns.add(expressionToString(left));
        } else {
            throw new UnsupportedOperationException("PMML 4.1 supports only columns before IN.");
        }
        if (!right.isConstant()) {
            throw new UnsupportedOperationException("PMML 4.1 supports only constants in arguments.");
        }
        List<Expression> children = right.getChildren();
        List<String> values = new ArrayList<String>(children.size());
        List<DataType> types = new ArrayList<DataType>(children.size());
        for (Expression child : children) {
            values.add(expressionToString(child));
            types.add(child.getOutputType());
        }
        DataType outputType = RuleEngineNodeModel.computeOutputType(types, false);
        if (outputType.isCompatible(IntValue.class)) {
            setIn.setArrayType(PMMLArrayType.INT);
        } else if (outputType.isCompatible(DoubleValue.class)) {
            setIn.setArrayType(PMMLArrayType.REAL);
        } else {
            setIn.setArrayType(PMMLArrayType.STRING);
        }
        setIn.setValues(values);
        return setIn;
    }

    private static final int[] LT = new int[]{-1};

    private static final int[] LE = new int[]{-1, 0};

    private static final int[] GT = new int[]{1};

    private static final int[] GE = new int[]{0, 1};

    private static final int[] EQ = new int[]{0};

    /**
     * {@inheritDoc}
     */
    @Override
    public PMMLPredicate compare(final Expression left, final Expression right, final DataValueComparator cmp,
        final int... possibleValues) {
        final PMMLOperator op;
        if (Arrays.equals(possibleValues, LT)) {
            op = PMMLOperator.LESS_THAN;
        } else if (Arrays.equals(possibleValues, LE)) {
            op = PMMLOperator.LESS_OR_EQUAL;
        } else if (Arrays.equals(possibleValues, GT)) {
            op = PMMLOperator.GREATER_THAN;
        } else if (Arrays.equals(possibleValues, GE)) {
            op = PMMLOperator.GREATER_OR_EQUAL;
        } else if (Arrays.equals(possibleValues, EQ)) {
            op = PMMLOperator.EQUAL;
        } else {
            throw new IllegalStateException("" + Arrays.toString(possibleValues));
        }
        handleSameType(left, right);
        if (left.getTreeType() == ASTType.ColRef) {
            m_usedColumns.add(expressionToString(left));
        }
        if (right.getTreeType() == ASTType.ColRef) {
            m_usedColumns.add(expressionToString(right));
            final PMMLOperator opposite;
            switch (op) {
                case EQUAL:
                    opposite = op;
                    break;
                case GREATER_OR_EQUAL:
                    opposite = PMMLOperator.LESS_OR_EQUAL;
                    break;
                case GREATER_THAN:
                    opposite = PMMLOperator.LESS_THAN;
                    break;
                case LESS_OR_EQUAL:
                    opposite = PMMLOperator.GREATER_OR_EQUAL;
                    break;
                case LESS_THAN:
                    opposite = PMMLOperator.GREATER_THAN;
                    break;
                    default:
                        throw new UnsupportedOperationException("Not supported: " + op);
            }
            return new PMMLSimplePredicate(expressionToString(right), opposite, expressionToString(left));
        }
        return new PMMLSimplePredicate(expressionToString(left), op, expressionToString(right));
    }

    /**
     * When the types are the same, the PMML specification do not allow it.
     *
     * @param left An {@link Expression}.
     * @param right An {@link Expression}.
     */
    private void handleSameType(final Expression left, final Expression right) {
        if (left.getTreeType() == right.getTreeType()) {
            throw new UnsupportedOperationException("Cannot compare these expressions in PMML 4.1: " + left + ", " + right);
        }
    }

    /**
     * Converts an {@link Expression} to {@link String}. Strips the {@code $}s around column references.
     *
     * @param expr An {@link Expression}.
     * @return The {@link String} representation of {@code expr}.
     */
    private String expressionToString(final Expression expr) {
        if (expr.getTreeType() == ASTType.ColRef) {
            String s = expr.toString();
            String colName = s.substring(1, s.lastIndexOf('$'));
            m_usedColumns.add(colName);
            return colName;
        }
        return expr.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PMMLPredicate like(final Expression left, final Expression right, final String key)
        throws IllegalStateException {
        throw new UnsupportedOperationException("PMML 4.1 do not support regular expressions in rules.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PMMLPredicate contains(final Expression left, final Expression right, final String key)
        throws IllegalStateException {
        throw new UnsupportedOperationException("PMML 4.1 do not support regular expressions in rules.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PMMLPredicate matches(final Expression left, final Expression right, final String key)
        throws IllegalStateException {
        throw new UnsupportedOperationException("PMML 4.1 do not support regular expressions in rules.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PMMLPredicate boolAsPredicate(final Expression expression) {
        if (expression.isConstant()) {
            DataCell value = expression.evaluate(null, null).getValue();
            if (value instanceof BooleanValue) {
                BooleanValue b = (BooleanValue)value;
                return b.getBooleanValue() ? new PMMLTruePredicate() : new PMMLFalsePredicate();
            }
        }
        if (expression.getTreeType() == ASTType.ColRef) {
            m_usedColumns.add(expressionToString(expression));
        }
        return new PMMLSimplePredicate(expressionToString(expression), PMMLOperator.EQUAL, "true");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PMMLPredicate trueValue() {
        return new PMMLTruePredicate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PMMLPredicate falseValue() {
        return new PMMLFalsePredicate();
    }

    /**
     * @return the usedColumns.
     */
    public Set<String> getUsedColumns() {
        return Collections.unmodifiableSet(m_usedColumns);
    }
}
