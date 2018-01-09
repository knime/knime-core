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
 * Created on 2013.04.24. by Gabor
 */
package org.knime.base.node.rules.engine;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.base.node.rules.engine.Rule.TableReference;
import org.knime.base.util.WildcardMatcher;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.MissingValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Scope;

/**
 * Creates {@link Expression}s using simple methods.
 *
 * @author Gabor Bakos
 * @since 2.8
 */
public class ExpressionFactory implements RuleExpressionFactory<Expression, Expression>,
    ReferenceExpressionFactory, Cloneable {
    /**
     * {@link Expression} for constants.
     *
     * @author Gabor
     * @since 2.8
     */
    private static final class ConstantExpression extends Expression.Base {
        private final ExpressionValue m_value;

        /**
         * Constructs the {@link ConstantExpression}.
         *
         * @param value The value to return.
         */
        private ConstantExpression(final ExpressionValue value) {
            super();
            this.m_value = value;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<DataType> getInputArgs() {
            return Collections.emptyList();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataType getOutputType() {
            return m_value.getValue().getType();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ExpressionValue evaluate(final DataRow row, final VariableProvider provider) {
            return m_value;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isConstant() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return m_value.getValue().toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ASTType getTreeType() {
            return ASTType.Constant;
        }
    }

    /**
     * An {@link Expression} base class for regular expressions. <br/>
     * {@link Expression#isConstant()} optimization is only done on the right (pattern) {@link Expression}.
     *
     * @author Gabor Bakos
     * @since 2.8
     */
    private abstract class RegExExpression extends Expression.Base {
        private final Expression m_right;

        private final Expression m_left;

        private final String m_opName;

        private final boolean m_match;

        private final String m_key;

        private final Pattern m_pattern;

        private final Map<String, Map<String, String>> m_rightConstantMap;

        /**
         * Constructor for {@link RegExExpression}.
         *
         * @param left The expression to compute the text to match on.
         * @param right The expression to compute the regular expression. Please note that a {@link #transform(String)}
         *            will be used on the result.
         * @param key Insertion key for the maps.
         *
         * @throws ParseException
         */
        private RegExExpression(final Expression left, final Expression right, final String opName,
            final boolean match, final String key) throws IllegalStateException {
            super(left, right);
            this.m_right = right;
            this.m_left = left;
            this.m_opName = opName;
            this.m_match = match;
            this.m_key = key;
            Pattern pattern = null;
            Map<String, Map<String, String>> map = EMPTY_MAP;
            try {
                if (right.isConstant()) {
                    final ExpressionValue expressionVal = right.evaluate(null, null);
                    final DataCell cell = expressionVal.getValue();
                    if (cell instanceof StringValue) {
                        pattern = createPattern((StringValue)cell);
                        map = expressionVal.getMatchedObjects();
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException(right.toString(), e);
            }
            m_pattern = pattern;
            m_rightConstantMap = map;
        }

        /**
         * @param cell A non-{@code null} {@link StringValue}.
         * @return The {@link Pattern} object created from the string value of {@code cell}.
         */
        protected Pattern createPattern(final StringValue cell) {
            return Pattern.compile(transform(cell.getStringValue()));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<DataType> getInputArgs() {
            return Arrays.asList(StringCell.TYPE, StringCell.TYPE);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataType getOutputType() {
            return BooleanCell.TYPE;
        }

        /**
         * Performs a match with possibly constant expressions.
         *
         * @param leftValue The result of the left value.
         * @param rightObjects The objects related to the pattern.
         * @param lString The value of the string to match on.
         * @param pattern A regular expression's compiled {@link Pattern}.
         * @return The expression value with the found groups and a {@link BooleanValue} result for match.
         */
        private ExpressionValue match(final ExpressionValue leftValue,
            final Map<String, Map<String, String>> rightObjects, final StringValue lString, final Pattern pattern) {
            final String l = lString.getStringValue();
            final Matcher matcher = pattern.matcher(l);
            final boolean res = m_match ? matcher.matches() : matcher.find();
            final Map<String, Map<String, String>> mergedObjects =
                Util.mergeObjects(leftValue.getMatchedObjects(), rightObjects);
            for (int i = 1; i <= matcher.groupCount(); ++i) {
                if (res) {
                    if (!mergedObjects.containsKey(m_key)) {
                        mergedObjects.put(m_key, new HashMap<String, String>());
                    }
                    mergedObjects.get(m_key).put(Integer.toString(i), matcher.group(i));
                }
            }
            return new ExpressionValue(BooleanCellFactory.create(res), mergedObjects);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ExpressionValue evaluate(final DataRow row, final VariableProvider provider) {
            ExpressionValue leftValue = m_left.evaluate(row, provider);
            DataCell leftCell = leftValue.getValue();
            if (m_pattern != null) {
                if (leftCell.isMissing()) {
                    return new ExpressionValue(BooleanCell.FALSE, EMPTY_MAP);
                }
                if (leftCell instanceof StringValue) {
                    StringValue lString = (StringValue)leftCell;
                    return match(leftValue, m_rightConstantMap, lString, m_pattern);
                }
                throw new IllegalStateException("Both the m_value and the pattern have to be strings: " + leftCell
                    + " [" + leftCell.getType() + "], " + m_pattern.pattern());
            }
            ExpressionValue rightValue = m_right.evaluate(row, provider);
            DataCell rightCell = rightValue.getValue();
            if (leftCell.isMissing() || rightCell.isMissing()) {
                return new ExpressionValue(BooleanCell.FALSE, EMPTY_MAP);
            }
            if (rightCell instanceof StringValue) {
                StringValue rString = (StringValue)rightCell;
                Pattern pattern = Pattern.compile(transform(rString.getStringValue()));
                if (leftCell instanceof StringValue) {
                    StringValue lString = (StringValue)leftCell;
                    return match(leftValue, rightValue.getMatchedObjects(), lString, pattern);
                }
            }
            throw new IllegalStateException("Both the m_value and the pattern have to be strings: " + leftCell + " ["
                + leftCell.getType() + "], " + rightCell + " [" + rightCell.getType() + "]");
        }

        /**
         * Converts the pattern to a regular expression.
         *
         * @param pattern The pattern that will be the result of the {@link #m_right} expression.
         * @return The transformed regular expression.
         */
        protected abstract String transform(final String pattern);

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isConstant() {
            return m_left.isConstant() && m_right.isConstant();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return m_left + m_opName + m_right;
        }
    }

    private boolean m_missingMatch = true, m_nanMatch = true;

    /** A constant to avoid type inference problems. */
    private static final Map<String, Map<String, String>> EMPTY_MAP = Collections
        .<String, Map<String, String>> emptyMap();

    /** Private constructor. */
    private ExpressionFactory() {
        super();
    }

    /**
     * @return the instance
     */
    public static ExpressionFactory getInstance() {
        return new ExpressionFactory();
    }

    /**
     * @return An {@link ExpressionFactory} that do not match on {@link Double#NaN} values for comparisons (except
     *         equals).
     */
    public ExpressionFactory withNaNsDoNotMatch() {
        ExpressionFactory ret = clone();
        ret.m_nanMatch = false;
        return ret;
    }

    /**
     * @return An {@link ExpressionFactory} that do not match on missing values for comparisons (except equals).
     */
    public ExpressionFactory withMissingsDoNotMatch() {
        ExpressionFactory ret = clone();
        ret.m_missingMatch = false;
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final ExpressionFactory clone() {
        try {
            return (ExpressionFactory)super.clone();
        } catch (CloneNotSupportedException e) {
            ExpressionFactory ret = new ExpressionFactory();
            ret.m_missingMatch = m_missingMatch;
            ret.m_nanMatch = m_nanMatch;
            return ret;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression not(final Expression expressionToNegate) {
        if (!expressionToNegate.getOutputType().isCompatible(BooleanValue.class)) {
            throw new IllegalStateException("Expected a boolean expression, got: " + expressionToNegate);
        }
        return new Expression.Base(expressionToNegate) {

            /**
             * {@inheritDoc}
             */
            @Override
            public DataType getOutputType() {
                return BooleanCell.TYPE;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public List<DataType> getInputArgs() {
                return Collections.singletonList(BooleanCell.TYPE);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ExpressionValue evaluate(final DataRow row, final VariableProvider provider) {
                ExpressionValue v = expressionToNegate.evaluate(row, provider);
                if (v.getValue().isMissing()) {
                    return v;
                }
                DataCell cell = v.getValue();
                if (cell instanceof BooleanValue) {
                    BooleanValue bool = (BooleanValue)cell;
                    return new ExpressionValue(BooleanCellFactory.create(!bool.getBooleanValue()), v.getMatchedObjects());
                }
                throw new IllegalStateException("Not boolean: " + v.getValue());
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isConstant() {
                return expressionToNegate.isConstant();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "not(" + expressionToNegate + ")";
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ASTType getTreeType() {
                return ASTType.Not;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression and(final List<Expression> boolExpressions) {
        final boolean allIsConstant = checkBooleansAndConstant(boolExpressions);
        return new Expression.Base(boolExpressions) {

            /**
             * {@inheritDoc}
             */
            @Override
            public DataType getOutputType() {
                return BooleanCell.TYPE;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public List<DataType> getInputArgs() {
                return Collections.singletonList(BooleanCell.TYPE);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ExpressionValue evaluate(final DataRow row, final VariableProvider provider) {
                DataCell ret = BooleanCell.TRUE;
                Map<String, Map<String, String>> matchedObjects = new HashMap<String, Map<String, String>>();
                for (Expression boolExpression : boolExpressions) {
                    ExpressionValue v = boolExpression.evaluate(row, provider);
                    DataCell cell = v.getValue();
                    assert !cell.isMissing();
                    if (cell instanceof BooleanValue) {
                        BooleanValue bool = (BooleanValue)cell;
                        if (bool.getBooleanValue()) {
                            matchedObjects = Util.mergeObjects(matchedObjects, v.getMatchedObjects());
                        } else {
                            return new ExpressionValue(BooleanCell.FALSE, EMPTY_MAP);
                        }
                    } else if (cell.isMissing()) {
                        ret = DataType.getMissingCell();
                    } else {
                        throw new IllegalStateException("Not boolean: " + v.getValue());
                    }
                }
                return new ExpressionValue(ret, matchedObjects);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isConstant() {
                return allIsConstant;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "and(" + boolExpressions + ")";
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ASTType getTreeType() {
                return ASTType.And;
            }
        };
    }

    /**
     * @param boolExpressions
     * @return
     */
    private boolean checkBooleansAndConstant(final List<Expression> boolExpressions) {
        boolean allIsConstant = true;
        for (Expression expression : boolExpressions) {
            allIsConstant &= expression.isConstant();
            if (!expression.getOutputType().isCompatible(BooleanValue.class)) {
                throw new IllegalStateException("Expected a boolean expression, got: " + expression + " in "
                    + boolExpressions.toString());
            }
        }
        return allIsConstant;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression or(final List<Expression> boolExpressions) {
        final boolean allIsConstant = checkBooleansAndConstant(boolExpressions);
        return new Expression.Base(boolExpressions) {

            /**
             * {@inheritDoc}
             */
            @Override
            public DataType getOutputType() {
                return BooleanCell.TYPE;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public List<DataType> getInputArgs() {
                return Collections.singletonList(BooleanCell.TYPE);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ExpressionValue evaluate(final DataRow row, final VariableProvider provider) {
                DataCell ret = BooleanCell.FALSE;
                Map<String, Map<String, String>> matchedObjects = new HashMap<String, Map<String, String>>();
                for (Expression boolExpression : boolExpressions) {
                    ExpressionValue v = boolExpression.evaluate(row, provider);
                    DataCell cell = v.getValue();
                    assert !cell.isMissing();
                    if (cell instanceof BooleanValue) {
                        BooleanValue bool = (BooleanValue)cell;
                        if (!bool.getBooleanValue()) {
                            matchedObjects = Util.mergeObjects(matchedObjects, v.getMatchedObjects());
                        } else {
                            return new ExpressionValue(BooleanCell.TRUE, matchedObjects);
                        }
                    } else if (cell.isMissing()) {
                        ret = DataType.getMissingCell();
                    } else {
                        throw new IllegalStateException("Not boolean: " + v.getValue());
                    }
                }
                return new ExpressionValue(ret, matchedObjects);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isConstant() {
                return allIsConstant;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "or(" + boolExpressions + ")";
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ASTType getTreeType() {
                return ASTType.Or;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression xor(final List<Expression> boolExpressions) {
        if (boolExpressions.size() < 1) {
            throw new IllegalStateException("xor requires at least one argument.");
        }
        final boolean allIsConstant = checkBooleansAndConstant(boolExpressions);
        return new Expression.Base(boolExpressions) {

            /**
             * {@inheritDoc}
             */
            @Override
            public DataType getOutputType() {
                return BooleanCell.TYPE;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public List<DataType> getInputArgs() {
                return Collections.singletonList(BooleanCell.TYPE);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ExpressionValue evaluate(final DataRow row, final VariableProvider provider) {
                ExpressionValue val = boolExpressions.get(0).evaluate(row, provider);
                DataCell valCell = val.getValue();
                assert !valCell.isMissing();
                if (valCell.isMissing()) {
                    return new ExpressionValue(valCell, EMPTY_MAP);
                } else if (!(valCell instanceof BooleanValue)) {
                    throw new IllegalStateException("Not a boolean value in row '" + row.getKey() + "': "
                        + valCell.getType());
                }

                BooleanCell ret = (BooleanCell)BooleanCellFactory.create(((BooleanValue) valCell).getBooleanValue());
                Map<String, Map<String, String>> matchedObjects = val.getMatchedObjects();
                for (int i = 1; i < boolExpressions.size(); ++i) {
                    Expression boolExpression = boolExpressions.get(i);
                    ExpressionValue v = boolExpression.evaluate(row, provider);
                    DataCell cell = v.getValue();
                    if (cell.isMissing()) {
                        return new ExpressionValue(cell, EMPTY_MAP);
                    } else if (cell instanceof BooleanValue) {
                        BooleanValue bool = (BooleanValue)cell;
                        matchedObjects = Util.mergeObjects(matchedObjects, v.getMatchedObjects());
                        ret = (BooleanCell)BooleanCellFactory.create(ret.getBooleanValue() ^ bool.getBooleanValue());
                    } else {
                        throw new IllegalStateException("Not a boolean value: " + v.getValue());
                    }
                }
                return new ExpressionValue(ret, matchedObjects);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isConstant() {
                return allIsConstant;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "xor(" + boolExpressions + ")";
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ASTType getTreeType() {
                return ASTType.Xor;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression missing(final Expression reference) {
        return new Expression.Base(reference) {

            /**
             * {@inheritDoc}
             */
            @Override
            public DataType getOutputType() {
                return BooleanCell.TYPE;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public List<DataType> getInputArgs() {
                return Collections.singletonList(DataType.getMissingCell().getType());
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ExpressionValue evaluate(final DataRow row, final VariableProvider provider) {
                ExpressionValue val = reference.evaluate(row, provider);
                DataCell valCell = val.getValue();
                return new ExpressionValue(BooleanCellFactory.create(valCell.isMissing()), EMPTY_MAP);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isConstant() {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "missing(" + reference + ")";
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ASTType getTreeType() {
                return ASTType.Missing;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression missingBoolean(final Expression reference) {
        return new Expression.Base(reference) {

            /**
             * {@inheritDoc}
             */
            @Override
            public DataType getOutputType() {
                return BooleanCell.TYPE;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public List<DataType> getInputArgs() {
                return Collections.singletonList(DataType.getMissingCell().getType());
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ExpressionValue evaluate(final DataRow row, final VariableProvider provider) {
                ExpressionValue val = reference.evaluate(row, provider);
                DataCell valCell = val.getValue();
                return new ExpressionValue(BooleanCellFactory.create(valCell.isMissing()), EMPTY_MAP);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isConstant() {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "missing(" + reference + ")";
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ASTType getTreeType() {
                return ASTType.Missing;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression columnRef(final DataTableSpec spec, final String columnRef) {
        return columnRefImpl(spec, columnRef, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression columnRefForMissing(final DataTableSpec spec, final String columnRef) {
        return columnRefImpl(spec, columnRef, true);
    }

    /**
     *
     * @param spec The {@link DataTableSpec}.
     * @param columnRef Name of the column.
     * @param booleanArgumentOfMissing When this value is {@code true} and the value is a boolean missing value, the
     *            result is {@code false}, else missing.
     * @return The {@link Expression} computing the value of the column.
     * @see #columnRef(DataTableSpec, String)
     * @see #columnRefForMissing(DataTableSpec, String)
     */
    private Expression columnRefImpl(final DataTableSpec spec, final String columnRef,
        final boolean booleanArgumentOfMissing) {
        if (!spec.containsName(columnRef)) {
            throw new IllegalStateException("Not a column: " + columnRef);
        }
        final int position = spec.findColumnIndex(columnRef);
        final DataType type = spec.getColumnSpec(position).getType();
        final boolean isBoolean = type.isCompatible(BooleanValue.class);
        assert (!booleanArgumentOfMissing || isBoolean) : type;
        return new Expression.Base() {

            /**
             * {@inheritDoc}
             */
            @Override
            public List<DataType> getInputArgs() {
                return Collections.emptyList();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public DataType getOutputType() {
                return spec.getColumnSpec(columnRef).getType();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ExpressionValue evaluate(final DataRow row, final VariableProvider provider) {
                if (booleanArgumentOfMissing) {
                    return new ExpressionValue(row.getCell(position), EMPTY_MAP);
                }
                final DataCell cell = row.getCell(position);
                if (isBoolean && cell.isMissing()) {
                    return new ExpressionValue(BooleanCell.FALSE, EMPTY_MAP);
                }
                return new ExpressionValue(row.getCell(position), EMPTY_MAP);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isConstant() {
                return false;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "$" + columnRef + "$";
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ASTType getTreeType() {
                return ASTType.ColRef;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression flowVarRef(final Map<String, FlowVariable> flowVariables, final String flowVarRef) {
        if (!flowVariables.containsKey(flowVarRef) || flowVariables.get(flowVarRef) == null) {
            throw new IllegalStateException("Not a valid flow variable: " + flowVarRef + " (" + flowVariables + ")");
        }
        final FlowVariable var = flowVariables.get(flowVarRef);
        final boolean isConstant = var.getScope() != Scope.Local;
        final ExpressionValue constant = isConstant ? Util.readFlowVarToExpressionValue(var) : null;
        return new Expression.Base() {

            /**
             * {@inheritDoc}
             */
            @Override
            public List<DataType> getInputArgs() {
                return Collections.emptyList();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public DataType getOutputType() {
                return Util.toDataType(var.getType());
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ExpressionValue evaluate(final DataRow row, final VariableProvider provider) {
                if (constant != null) {
                    return constant;
                }
                Object variable = provider.readVariable(flowVarRef, Util.flowVarTypeToClass(var.getType()));
                switch (var.getType()) {
                    case DOUBLE:
                        return new ExpressionValue(new DoubleCell(((Double)variable).doubleValue()), EMPTY_MAP);
                    case INTEGER:
                        return new ExpressionValue(new IntCell(((Integer)variable).intValue()), EMPTY_MAP);
                    case STRING:
                        return new ExpressionValue(new StringCell((String)variable), EMPTY_MAP);
                    default:
                        throw new IllegalStateException("Not supported flow variable type: " + var.getType());
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isConstant() {
                return isConstant;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "$$" + flowVarRef + "$$";
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ASTType getTreeType() {
                return ASTType.FlowVarRef;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression constant(final String text, final DataType type) {
        try {
            final Class<? extends DataCell> cellClass = type.getCellClass();
            if (cellClass == null) {
                throw new IllegalStateException("Cannot construct cell with type: " + type);
            }
            final DataCell cell = cellClass.getConstructor(String.class).newInstance(text);
            final ExpressionValue value = new ExpressionValue(cell, EMPTY_MAP);
            return new ConstantExpression(value);
        } catch (InstantiationException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch (SecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression constant(final double real) {
        final DoubleCell cell = new DoubleCell(real);
        final ExpressionValue value = new ExpressionValue(cell, EMPTY_MAP);
        return new ConstantExpression(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression constant(final int integer) {
        final IntCell cell = new IntCell(integer);
        final ExpressionValue value = new ExpressionValue(cell, EMPTY_MAP);
        return new ConstantExpression(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression list(final List<Expression> operands) {
        boolean allIsConstantTmp = true;
        for (Expression expression : operands) {
            allIsConstantTmp &= expression.isConstant();
        }
        final boolean allIsConstant = allIsConstantTmp;
        ExpressionValue constantTmp = null;
        if (allIsConstant) {
            final List<DataCell> cells = new ArrayList<DataCell>(operands.size());
            Map<String, Map<String, String>> matchedObjects = new HashMap<String, Map<String, String>>();
            for (Expression expression : operands) {
                ExpressionValue v = expression.evaluate(null, null);
                cells.add(v.getValue());
                matchedObjects = Util.mergeObjects(matchedObjects, v.getMatchedObjects());
            }
            constantTmp = new ExpressionValue(CollectionCellFactory.createListCell(cells), matchedObjects);
        }
        final ExpressionValue constant = constantTmp;
        return new Expression.Base(operands) {

            /**
             * {@inheritDoc}
             */
            @Override
            public List<DataType> getInputArgs() {
                return Collections.emptyList();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public DataType getOutputType() {
                DataType[] types = new DataType[operands.size()];
                int i = 0;
                for (Expression expression : operands) {
                    types[i] = expression.getOutputType();
                    ++i;
                }
                return ListCell.getCollectionType(CollectionCellFactory.getElementType(types));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ExpressionValue evaluate(final DataRow row, final VariableProvider provider) {
                if (constant != null) {
                    return constant;
                }
                final List<DataCell> cells = new ArrayList<DataCell>(operands.size());
                Map<String, Map<String, String>> matchedObjects = new HashMap<String, Map<String, String>>();
                for (Expression expression : operands) {
                    ExpressionValue v = expression.evaluate(row, provider);
                    cells.add(v.getValue());
                    matchedObjects = Util.mergeObjects(matchedObjects, v.getMatchedObjects());
                }
                return new ExpressionValue(CollectionCellFactory.createListCell(cells), matchedObjects);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isConstant() {
                return allIsConstant;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return operands.toString();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ASTType getTreeType() {
                return ASTType.List;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression in(final Expression left, final Expression right) {
        if (!right.getOutputType().isCollectionType()) {
            throw new IllegalStateException("The m_right operand of operator 'IN' is not a collection: "
                + right.getOutputType());
        }
        ExpressionValue constantTmp = null;
        if (left.isConstant() && right.isConstant()) {
            ExpressionValue leftValue = left.evaluate(null, null);
            ExpressionValue rightValue = right.evaluate(null, null);
            DataCell l = leftValue.getValue();
            final DataCell r = rightValue.getValue();
            if (r.isMissing()) {
                constantTmp = new ExpressionValue(DataType.getMissingCell(), EMPTY_MAP);
            } else if (r instanceof CollectionDataValue) {
                CollectionDataValue rightValues = (CollectionDataValue)r;
                for (DataCell dataCell : rightValues) {
                    DataValueComparator cmp =
                        DataType.getCommonSuperType(l.getType(), dataCell.getType()).getComparator();
                    if (cmp.compare(l, dataCell) == 0) {
                        constantTmp =
                            new ExpressionValue(BooleanCell.TRUE, Util.mergeObjects(leftValue.getMatchedObjects(),
                                rightValue.getMatchedObjects()));
                    }
                }
                if (constantTmp == null) {
                    constantTmp = new ExpressionValue(BooleanCell.FALSE, EMPTY_MAP);
                }
            }
            if (constantTmp == null) {
                throw new IllegalStateException("Right operand of the 'IN' operator is not a collection.");
            }
        }
        final ExpressionValue constant = constantTmp;
        return new Expression.Base(left, right) {

            /**
             * {@inheritDoc}
             */
            @Override
            public List<DataType> getInputArgs() {
                return Arrays.asList(DataType.getMissingCell().getType(),
                    ListCell.getCollectionType(DataType.getMissingCell().getType()));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public DataType getOutputType() {
                return BooleanCell.TYPE;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ExpressionValue evaluate(final DataRow row, final VariableProvider provider) {
                if (constant != null) {
                    return constant;
                }
                ExpressionValue leftValue = left.evaluate(row, provider);
                ExpressionValue rightValue = right.evaluate(row, provider);
                DataCell l = leftValue.getValue();
                final DataCell r = rightValue.getValue();
                if (r.isMissing()) {
                    return new ExpressionValue(DataType.getMissingCell(), EMPTY_MAP);
                }
                if (r instanceof CollectionDataValue) {
                    CollectionDataValue rightValues = (CollectionDataValue)r;
                    for (DataCell dataCell : rightValues) {
                        DataValueComparator cmp =
                            DataType.getCommonSuperType(l.getType(), dataCell.getType()).getComparator();
                        if (cmp.compare(l, dataCell) == 0) {
                            return new ExpressionValue(BooleanCell.TRUE, Util.mergeObjects(
                                leftValue.getMatchedObjects(), rightValue.getMatchedObjects()));
                        }
                    }
                    return new ExpressionValue(BooleanCell.FALSE, EMPTY_MAP);
                }
                throw new IllegalStateException("Right operand of the 'IN' operator is not a collection.");
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isConstant() {
                return left.isConstant() && right.isConstant();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return left + " in " + right;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ASTType getTreeType() {
                return ASTType.In;
            }
        };
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
    public Expression compare(final Expression left, final Expression right, final DataValueComparator cmp,
        final int... possibleValues) {
        return new Expression.Base(left, right) {

            /**
             * {@inheritDoc}
             */
            @Override
            public List<DataType> getInputArgs() {
                return Arrays.asList(left.getOutputType(), right.getOutputType());
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public DataType getOutputType() {
                return BooleanCell.TYPE;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ExpressionValue evaluate(final DataRow row, final VariableProvider provider) {
                ExpressionValue leftValue = left.evaluate(row, provider);
                ExpressionValue rightValue = right.evaluate(row, provider);
                Map<String, Map<String, String>> mergedObjects = Util.mergeObjects(leftValue.getMatchedObjects(), rightValue.getMatchedObjects());
                DataCell leftCell = leftValue.getValue();
                DataCell rightCell = rightValue.getValue();
                final boolean leftMissing = leftCell.isMissing(), rightMissing = rightCell.isMissing();
                //Priority over NaNs
                if (!m_missingMatch && (leftMissing || rightMissing)) {
                    boolean bothMissingAndAllowEquals =
                        leftMissing && rightMissing && Arrays.binarySearch(possibleValues, 0) >= 0;
                    if (bothMissingAndAllowEquals) {
                        if (leftCell instanceof MissingValue) {
                            MissingValue lmc = (MissingValue)leftCell;
                            if (rightCell instanceof MissingValue) {
                                MissingValue rmc = (MissingValue)rightCell;
                                //If the errors differ we do not consider them equal
                                bothMissingAndAllowEquals = lmc.equals(rmc);
                            }
                        }
                    }
                    return new ExpressionValue(BooleanCellFactory.create(bothMissingAndAllowEquals), mergedObjects);
                }
                //No missing values
                final boolean leftNaN = isNaN(leftCell), rightNaN = isNaN(rightCell);
                if (!m_nanMatch && (leftNaN || rightNaN)) {
                    //NaNs are considered equals to each other even if it is not by the IEEE spec.
                    boolean bothNaNAndAllowEquals = leftNaN && rightNaN && Arrays.binarySearch(possibleValues, 0) >= 0;
                    return new ExpressionValue(BooleanCellFactory.create(bothNaNAndAllowEquals), mergedObjects);
                }
                boolean found = false;
                int compareResult = Util.signum(cmp.compare(leftCell, rightCell));
                for (int possibleValue : possibleValues) {
                    found |= possibleValue == compareResult;
                }
                return new ExpressionValue(BooleanCellFactory.create(found), mergedObjects);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isConstant() {
                return left.isConstant() && right.isConstant();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                String rel = "???";
                if (Arrays.equals(possibleValues, LT)) {
                    rel = "<";
                } else if (Arrays.equals(possibleValues, LE)) {
                    rel = "<=";
                } else if (Arrays.equals(possibleValues, GT)) {
                    rel = ">";
                } else if (Arrays.equals(possibleValues, GE)) {
                    rel = ">=";
                } else if (Arrays.equals(possibleValues, EQ)) {
                    rel = "=";
                }
                return left + rel + right;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ASTType getTreeType() {
                if (Arrays.equals(possibleValues, LT)) {
                    return ASTType.Less;
                }
                if (Arrays.equals(possibleValues, LE)) {
                    return ASTType.LessOrEquals;
                }
                if (Arrays.equals(possibleValues, GT)) {
                    return ASTType.Greater;
                }
                if (Arrays.equals(possibleValues, GE)) {
                    return ASTType.GreaterOrEquals;
                }
                if (Arrays.equals(possibleValues, EQ)) {
                    return ASTType.Equals;
                }
                throw new IllegalStateException("" + Arrays.toString(possibleValues));
            }
        };
    }

    /**
     * @param cell A {@link DataCell}.
     * @return {@code true} iff {@code cell} is a {@link DoubleValue} with a {@link Double#NaN} number.
     */
    protected final boolean isNaN(final DataCell cell) {
        if (cell instanceof DoubleValue) {
            DoubleValue dv = (DoubleValue)cell;
            return Double.isNaN(dv.getDoubleValue());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression like(final Expression left, final Expression right, final String key)
        throws IllegalStateException {
        return new RegExExpression(left, right, " like ", true, key) {

            /**
             * {@inheritDoc}
             */
            @Override
            protected String transform(final String string) {
                return WildcardMatcher.wildcardToRegex(string);
            }

            @Override
            protected Pattern createPattern(final StringValue cell) {
                return Pattern.compile(transform(cell.getStringValue()), Pattern.DOTALL | Pattern.MULTILINE);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ASTType getTreeType() {
                return ASTType.Like;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression contains(final Expression left, final Expression right, final String key)
        throws IllegalStateException {
        return new RegExExpression(left, right, " contains ", false, key) {

            /**
             * {@inheritDoc}
             */
            @Override
            protected String transform(final String string) {
                return string;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ASTType getTreeType() {
                throw new UnsupportedOperationException("contains");
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression matches(final Expression left, final Expression right, final String key)
        throws IllegalStateException {
        return new RegExExpression(left, right, " matches ", true, key) {

            /**
             * {@inheritDoc}
             */
            @Override
            protected String transform(final String string) {
                return string;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ASTType getTreeType() {
                return ASTType.Matches;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression tableRef(final TableReference reference) {
        return new Expression.Base() {

            /**
             * {@inheritDoc}
             */
            @Override
            public List<DataType> getInputArgs() {
                return Collections.emptyList();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public DataType getOutputType() {
                return reference.getDataType();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ExpressionValue evaluate(final DataRow row, final VariableProvider provider) {
                switch (reference) {
                    case RowCount:
                        return new ExpressionValue(new LongCell(provider.getRowCountLong()), EMPTY_MAP);
                    case RowIndex:
                        return new ExpressionValue(new LongCell(provider.getRowIndexLong()), EMPTY_MAP);
                    case RowId:
                        return new ExpressionValue(new StringCell(row.getKey().getString()), EMPTY_MAP);
                    default:
                        throw new IllegalStateException("Not supported table reference: " + reference);
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isConstant() {
                //reference == TableReference.RowCount is not ok, because
                //it requires provider
                return false;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return reference.toString();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public ASTType getTreeType() {
                return ASTType.TableRef;
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression trueValue() {
        return booleanValue(true);
    }

    /**
     * @param b
     * @return
     */
    private Expression booleanValue(final boolean b) {
        return new ConstantExpression(new ExpressionValue(BooleanCellFactory.create(b),
            Collections.<String, Map<String, String>> emptyMap()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression falseValue() {
        return booleanValue(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression trueRefValue() {
        return trueValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression falseRefValue() {
        return falseValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression boolAsPredicate(final Expression expression) {
        if (!BooleanCell.TYPE.isASuperTypeOf(expression.getOutputType())) {
            throw new IllegalArgumentException(expression + " is not a boolean");
        }
        return expression;
    }
}
