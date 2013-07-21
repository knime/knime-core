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
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Scope;

/**
 * Creates {@link Expression}s using simple methods.
 *
 * @author Gabor Bakos
 * @since 2.8
 */
public class ExpressionFactory {
    /**
     * {@link Expression} for constants.
     *
     * @author Gabor
     * @since 2.8
     */
    private static final class ConstantExpression implements Expression {
        private final ExpressionValue m_value;

        /**
         * Constructs the {@link ConstantExpression}.
         *
         * @param value The value to return.
         */
        private ConstantExpression(final ExpressionValue value) {
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
    }

    /**
     * An {@link Expression} base class for regular expressions. <br/>
     * {@link Expression#isConstant()} optimization is only done on the right (pattern) {@link Expression}.
     *
     * @author Gabor Bakos
     * @since 2.8
     */
    private abstract class RegExExpression implements Expression {
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
                        pattern = Pattern.compile(transform(((StringValue)cell).getStringValue()));
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
                if (!mergedObjects.containsKey(m_key)) {
                    mergedObjects.put(m_key, new HashMap<String, String>());
                }
                mergedObjects.get(m_key).put(Integer.toString(i), matcher.group(i));
            }
            return new ExpressionValue(BooleanCell.get(res), mergedObjects);
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

    /** A constant to avoid type inference problems. */
    private static final Map<String, Map<String, String>> EMPTY_MAP = Collections
        .<String, Map<String, String>> emptyMap();

    /** Static singleton instance. */
    private static final ExpressionFactory INSTANCE = new ExpressionFactory();

    /** Private constructor. */
    private ExpressionFactory() {
        super();
    }

    /**
     * @return the instance
     */
    public static ExpressionFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Negates the result of an {@link Expression}. For missing values it will return missing value.<br/>
     * No optimization is done for {@link Expression#isConstant()} {@link Expression}.
     *
     * @param expressionToNegate A {@link BooleanValue} {@link Expression}. If a non-Boolean and non-missing value is
     *            matched, and {@link IllegalStateException} will be thrown.
     * @return The negated expression.
     */
    public Expression not(final Expression expressionToNegate) {
        if (!expressionToNegate.getOutputType().isCompatible(BooleanValue.class)) {
            throw new IllegalStateException("Expected a boolean expression, got: " + expressionToNegate);
        }
        return new Expression() {

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
                    return new ExpressionValue(BooleanCell.get(!bool.getBooleanValue()), v.getMatchedObjects());
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
        };
    }

    /**
     * Creates a new {@link Expression} with the conjunction of the arguments. Missing values will cause missing value
     * result, but a {@link BooleanCell#FALSE} will result in a {@value BooleanCell#FALSE} result because of the
     * short-circuit evaluation. When no arguments passed, a {@value Boolean#TRUE} result will be returned. <br/>
     * No optimization is done for {@link Expression#isConstant()} {@link Expression}s.
     *
     * @param boolExpressions Some {@link Expression}s with {@link BooleanValue} result. If non-missing, non-Boolean
     *            expression results occur, an IllegalStateException will be thrown.
     * @return The conjunction of the arguments as an {@link Expression}.
     */
    public Expression and(final Expression... boolExpressions) {
        final boolean allIsConstant = checkBooleansAndConstant(boolExpressions);
        return new Expression() {

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
                return "and(" + Arrays.toString(boolExpressions) + ")";
            }
        };
    }

    /**
     * @param boolExpressions
     * @return
     */
    private boolean checkBooleansAndConstant(final Expression... boolExpressions) {
        boolean allIsConstant = true;
        for (Expression expression : boolExpressions) {
            allIsConstant &= expression.isConstant();
            if (!expression.getOutputType().isCompatible(BooleanValue.class)) {
                throw new IllegalStateException("Expected a boolean expression, got: " + expression + " in "
                    + Arrays.toString(boolExpressions));
            }
        }
        return allIsConstant;
    }

    /**
     * Creates a new {@link Expression} with the disjunction of the arguments. Missing values will cause missing value
     * result, but a {@link BooleanCell#TRUE} will result in a {@value BooleanCell#TRUE} result because of the
     * short-circuit evaluation. When no arguments passed, a {@value Boolean#FALSE} result will be returned. <br/>
     * No optimization is done for {@link Expression#isConstant()} {@link Expression}s.
     *
     * @param boolExpressions Some {@link Expression}s with {@link BooleanValue} result. If non-missing, non-Boolean
     *            expression results occur, an IllegalStateException will be thrown.
     * @return The disjunction of the arguments as an {@link Expression}.
     */
    public Expression or(final Expression... boolExpressions) {
        final boolean allIsConstant = checkBooleansAndConstant(boolExpressions);
        return new Expression() {

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
                return "or(" + Arrays.toString(boolExpressions) + ")";
            }
        };
    }

    /**
     * Creates a new {@link Expression} with the exclusive or of the arguments. Missing values will cause missing value
     * result. When no arguments passed, an {@link IllegalStateException} will be thrown. <br/>
     * No optimization is done for {@link Expression#isConstant()} {@link Expression}s.
     *
     * @param boolExpressions At least one {@link Expression}s with {@link BooleanValue} result. If non-missing,
     *            non-Boolean expression results occur, an IllegalStateException will be thrown.
     * @return The conjunction of the arguments as an {@link Expression}.
     * @throws IllegalStateException When no expression is specified.
     */
    public Expression xor(final Expression... boolExpressions) {
        if (boolExpressions.length < 1) {
            throw new IllegalStateException("xor requires at least one argument.");
        }
        final boolean allIsConstant = checkBooleansAndConstant(boolExpressions);
        return new Expression() {

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
                ExpressionValue val = boolExpressions[0].evaluate(row, provider);
                DataCell valCell = val.getValue();
                if (valCell.isMissing()) {
                    return new ExpressionValue(valCell, EMPTY_MAP);
                }
                if (valCell.getType() != BooleanCell.TYPE) {
                    throw new IllegalStateException("Not a boolean m_value: " + valCell.getType());
                }
                BooleanCell ret = (BooleanCell)valCell;
                Map<String, Map<String, String>> matchedObjects = val.getMatchedObjects();
                for (int i = 1; i < boolExpressions.length; ++i) {
                    Expression boolExpression = boolExpressions[i];
                    ExpressionValue v = boolExpression.evaluate(row, provider);
                    DataCell cell = v.getValue();
                    if (cell.isMissing()) {
                        return new ExpressionValue(ret, EMPTY_MAP);
                    }
                    if (cell instanceof BooleanValue) {
                        BooleanValue bool = (BooleanValue)cell;
                        matchedObjects = Util.mergeObjects(matchedObjects, v.getMatchedObjects());
                        ret = BooleanCell.get(ret.getBooleanValue() ^ bool.getBooleanValue());
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
                return "xor(" + Arrays.toString(boolExpressions) + ")";
            }
        };
    }

    /**
     * Creates an {@link Expression} for checking for missing values.
     *
     * @param reference Any non-{@code null} {@link Expression}.
     * @return An {@link Expression} which result {@link BooleanCell#TRUE} if the wrapped {@link Expression} returns a
     *         missing value, and {@link BooleanCell#FALSE} if it is not missing.
     */
    public Expression missing(final Expression reference) {
        return new Expression() {

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
                return new ExpressionValue(BooleanCell.get(valCell.isMissing()), EMPTY_MAP);
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
        };
    }

    /**
     * Creates a reference {@link Expression} to a column in a {@link DataTable}.
     *
     * @param spec The {@link DataTableSpec}.
     * @param columnRef Name of the column in the {@code spec}. (As-is, no escapes.)
     * @return The {@link Expression} computing the value of the column.
     * @throws IllegalStateException If the {@code columnRef} is not present in {@code spec}.
     */
    public Expression columnRef(final DataTableSpec spec, final String columnRef) {
        if (!spec.containsName(columnRef)) {
            throw new IllegalStateException("Not a column: " + columnRef);
        }
        final int position = spec.findColumnIndex(columnRef);
        return new Expression() {

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
        };
    }

    /**
     * Creates a reference {@link Expression} to a flow variable.<br/>
     * It performs {@link Expression#isConstant()} optimization.
     *
     * @param flowVariables All available flow variables.
     * @param flowVarRef The name of the flow variable. (As-is, no escapes.)
     * @return An Expression computing the flow variable's value.
     * @throws IllegalStateException If the {@code flowVarRef} is not present in the {@code flowVariables}.
     */
    public Expression flowVarRef(final Map<String, FlowVariable> flowVariables, final String flowVarRef) {
        if (!flowVariables.containsKey(flowVarRef) || flowVariables.get(flowVarRef) == null) {
            throw new IllegalStateException("Not a valid flow variable: " + flowVarRef + " (" + flowVariables + ")");
        }
        final FlowVariable var = flowVariables.get(flowVarRef);
        final boolean isConstant = var.getScope() != Scope.Local;
        final ExpressionValue constant = isConstant ? Util.readFlowVarToExpressionValue(var) : null;
        return new Expression() {

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
        };
    }

    /**
     * Creates a constant {@link Expression} from a {@link String} using the {@link DataType}'s single argument (
     * {@link String}) constructor. Can be used to create xml or svg cells too.
     *
     * @param text The constant text of the result.
     * @param type The DataType of the result.
     * @return An {@link Expression} always returning the same text.
     * @throws IllegalStateException if cannot create the specified {@link DataCell}. (Should not happen with
     *             {@link StringCell}s.)
     */
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
     * Creates a constant expression for a double value.
     *
     * @param real The value to return.
     * @return A new constant expression always returning {@code real}.
     */
    public Expression constant(final double real) {
        final DoubleCell cell = new DoubleCell(real);
        final ExpressionValue value = new ExpressionValue(cell, EMPTY_MAP);
        return new ConstantExpression(value);
    }

    /**
     * Creates a constant expression for an int value.
     *
     * @param integer The value to return.
     * @return A new constant expression always returning {@code integer}.
     */
    public Expression constant(final int integer) {
        final IntCell cell = new IntCell(integer);
        final ExpressionValue value = new ExpressionValue(cell, EMPTY_MAP);
        return new ConstantExpression(value);
    }

    /**
     * Creates an {@link Expression} generating {@link ListCell} values. The matched objects get merged. <br/>
     * It performs {@link Expression#isConstant()} optimization.
     *
     * @param operands The expressions.
     * @return An {@link Expression} generating {@link ListCell}s from the {@code operands}' results.
     */
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
        return new Expression() {

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
        };
    }

    /**
     * Creates a new {@link Expression} checking for inclusion in a collection. <br/>
     * It performs {@link Expression#isConstant()} optimization.
     *
     * @param left The left {@link Expression} is what we are looking for.
     * @param right The right {@link Expression} contains the possible values in a {@link CollectionDataValue} (
     *            {@link ListCell}).
     * @return An {@link Expression} returning {@link BooleanCell#TRUE} the left value is contained in the right values,
     *         missing if the right value is missing and {@link BooleanCell#FALSE} if left is not contained in the
     *         right. (Throws {@link IllegalStateException} if the right operand is neither missing nor collection.)
     * @throws IllegalStateException If the right {@link Expression} does not returns a collection type.
     */
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
        return new Expression() {

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
        };
    }

    private static final int[] lt = new int[]{-1};

    private static final int[] le = new int[]{-1, 0};

    private static final int[] gt = new int[]{1};

    private static final int[] ge = new int[]{0, 1};

    private static final int[] eq = new int[]{0};

    /**
     * Creates a comparison {@link Expression} with the acceptable outcomes. The matched objects are combined (in
     * left-right order). <br/>
     * No optimization is done for {@link Expression#isConstant()} {@link Expression}s.
     *
     * @param left The {@link Expression} representing the left operand.
     * @param right The {@link Expression} of the right operand.
     * @param cmp The {@link DataValueComparator} to compare the {@code left} and {@code right} values.
     * @param possibleValues The signum values for the {@code cmp} outcomes: {@code -1} means left is smaller, {@code 0}
     *            - they are the same, {@code 1} - left is larger.
     * @return An {@link Expression} comparing the {@code left} and {@code right} values. If the comparison from the
     *         {@code cmp} results in any of the {@code possibleValues} a {@link BooleanCell#TRUE} will be returned,
     *         else a {@link BooleanCell#FALSE}.
     */
    public Expression compare(final Expression left, final Expression right, final DataValueComparator cmp,
        final int... possibleValues) {
        return new Expression() {

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
                DataCell leftCell = leftValue.getValue();
                DataCell rightCell = rightValue.getValue();
                boolean found = false;
                int compareResult = Util.signum(cmp.compare(leftCell, rightCell));
                for (int possibleValue : possibleValues) {
                    found |= possibleValue == compareResult;
                }
                return new ExpressionValue(BooleanCell.get(found), Util.mergeObjects(leftValue.getMatchedObjects(),
                    rightValue.getMatchedObjects()));
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
                if (Arrays.equals(possibleValues, lt)) {
                    rel = "<";
                } else if (Arrays.equals(possibleValues, le)) {
                    rel = "<=";
                } else if (Arrays.equals(possibleValues, gt)) {
                    rel = ">";
                } else if (Arrays.equals(possibleValues, ge)) {
                    rel = ">=";
                } else if (Arrays.equals(possibleValues, eq)) {
                    rel = "=";
                }
                return left + rel + right;
            }
        };
    }

    /**
     * Constructs a wild-card matcher {@link Expression}. <br/>
     * {@link Expression#isConstant()} optimization is only done on the {@code right} (pattern) {@link Expression}.
     *
     * @param left A {@link StringValue}'d {@link Expression}.
     * @param right Another {@link StringValue}'d {@link Expression}, but this is for the wild-card pattern.
     * @param key The key for the matched objects. Can be {@code null}.
     * @return An {@link Expression} representing an SQL-like LIKE operator.
     * @throws IllegalStateException When the constant right expression is an invalid pattern. (Most probably never.)
     */
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

        };
    }

    /**
     * Constructs a regular expression {@line Matcher#find()}er {@link Expression}. <br/>
     * {@link Expression#isConstant()} optimization is only done on the {@code right} (pattern) {@link Expression}.
     *
     * @param left A {@link StringValue}'d {@link Expression}.
     * @param right Another {@link StringValue}'d {@link Expression}, but this is for the regular expression pattern.
     * @param key The key for the matched objects. Can be {@code null}.
     * @return An {@link Expression} representing a regular expression finding operator.
     * @throws IllegalStateException When the constant right expression is an invalid pattern.
     */
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

        };
    }

    /**
     * Constructs a regular expression {@line Matcher#matches()()} {@link Expression}. <br/>
     * {@link Expression#isConstant()} optimization is only done on the {@code right} (pattern) {@link Expression}.
     *
     * @param left A {@link StringValue}'d {@link Expression}.
     * @param right Another {@link StringValue}'d {@link Expression}, but this is for the regular expression pattern.
     * @param key The key for the matched objects. Can be {@code null}.
     * @return An {@link Expression} representing a regular expression matcher operator.
     * @throws IllegalStateException When the constant right expression is an invalid pattern.
     */
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

        };
    }

    /**
     * Constructs an {@link Expression} for the table specific expressions.<br/>
     * No optimization is done for {@link Expression#isConstant()} {@link Expression}.
     *
     * @param reference A {@link TableReference} enum.
     * @return An {@link Expression} to compute the value referred by {@code reference}.
     */
    public Expression tableRef(final TableReference reference) {
        return new Expression() {

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
                        return new ExpressionValue(new IntCell(provider.getRowCount()), EMPTY_MAP);
                    case RowIndex:
                        return new ExpressionValue(new IntCell(provider.getRowIndex()), EMPTY_MAP);
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
        };
    }
}
