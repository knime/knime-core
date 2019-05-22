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
 */
package org.knime.core.data.container.filter.predicate;

import static org.knime.core.data.container.filter.predicate.FilterPredicate.custom;
import static org.knime.core.data.container.filter.predicate.FilterPredicate.equal;
import static org.knime.core.data.container.filter.predicate.FilterPredicate.greater;
import static org.knime.core.data.container.filter.predicate.FilterPredicate.greaterOrEqual;
import static org.knime.core.data.container.filter.predicate.FilterPredicate.isMissing;
import static org.knime.core.data.container.filter.predicate.FilterPredicate.lesser;
import static org.knime.core.data.container.filter.predicate.FilterPredicate.lesserOrEqual;
import static org.knime.core.data.container.filter.predicate.FilterPredicate.notEqual;
import static org.knime.core.data.container.filter.predicate.TypedColumn.boolCol;
import static org.knime.core.data.container.filter.predicate.TypedColumn.doubleCol;
import static org.knime.core.data.container.filter.predicate.TypedColumn.intCol;
import static org.knime.core.data.container.filter.predicate.TypedColumn.longCol;
import static org.knime.core.data.container.filter.predicate.TypedColumn.rowKey;
import static org.knime.core.data.container.filter.predicate.TypedColumn.stringCol;

import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.TableSpecReplacerTable;
import org.knime.core.data.container.filter.predicate.BinaryLogicalPredicate.And;
import org.knime.core.data.container.filter.predicate.BinaryLogicalPredicate.Or;
import org.knime.core.data.container.filter.predicate.ColumnPredicate.CustomPredicate;
import org.knime.core.data.container.filter.predicate.ColumnPredicate.EqualTo;
import org.knime.core.data.container.filter.predicate.ColumnPredicate.GreaterThan;
import org.knime.core.data.container.filter.predicate.ColumnPredicate.GreaterThanOrEqualTo;
import org.knime.core.data.container.filter.predicate.ColumnPredicate.LesserThan;
import org.knime.core.data.container.filter.predicate.ColumnPredicate.LesserThanOrEqualTo;
import org.knime.core.data.container.filter.predicate.ColumnPredicate.MissingValuePredicate;
import org.knime.core.data.container.filter.predicate.ColumnPredicate.NotEqualTo;
import org.knime.core.data.container.filter.predicate.FilterPredicate.Visitor;
import org.knime.core.data.container.filter.predicate.IndexedColumn.BooleanColumn;
import org.knime.core.data.container.filter.predicate.IndexedColumn.DoubleColumn;
import org.knime.core.data.container.filter.predicate.IndexedColumn.IntColumn;
import org.knime.core.data.container.filter.predicate.IndexedColumn.LongColumn;
import org.knime.core.data.container.filter.predicate.IndexedColumn.OrderColumn;
import org.knime.core.data.container.filter.predicate.IndexedColumn.StringColumn;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;

/**
 * Helper class that changes the types of {@link TypedColumn TypedColumns} held by a {@link FilterPredicate}. This is
 * helpful if a table's {@link DataTableSpec} is altered, as for instance in the case of {@link TableSpecReplacerTable
 * TableSpecReplacerTables}.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @since 3.8
 */
public final class FilterPredicateSpecReplacer implements Visitor<FilterPredicate> {

    private final DataTableSpec m_newSpec;

    /**
     * Constructs a new filter predicate spec replacer visitor. Initialized with the new / updated
     * {@link DataTableSpec}. For instance, if the to-be-visited {@link FilterPredicate} tests some condition on an
     * {@link IntColumn} at index 5 and the spec passed as the argument has a {@link DataColumnSpec} of type
     * {@link LongCell#TYPE} at index 5, this visitor will return a {@link FilterPredicate} testing the same condition
     * on a {@link LongColumn} at index 5.
     *
     * @param newSpec the updated spec
     */
    public FilterPredicateSpecReplacer(final DataTableSpec newSpec) {
        m_newSpec = newSpec;
    }

    @Override
    public <T> FilterPredicate visit(final MissingValuePredicate<T> mvp) {
        return mvp.getColumn().accept(new MissingValuePredicateSpecReplacer());
    }

    @Override
    public <T> FilterPredicate visit(final CustomPredicate<T> udf) {
        return udf.getColumn().accept(new CustomPredicateSpecReplacer(udf, udf.getPredicate()));
    }

    @Override
    public <T> FilterPredicate visit(final EqualTo<T> eq) {
        return visitInternal(eq);
    }

    private <T, C extends TypedColumn<T>> FilterPredicate visitInternal(final EqualTo<T> eq) {
        final TypedColumn<T> column = eq.getColumn();
        final T value = eq.getValue();
        try {
            final BiFunction<C, T, FilterPredicate> function = (c, v) -> equal(c, v);
            return column.accept(new ValuePredicateSpecReplacer(eq, function, value));
        } catch (UnsupportedOperationException e) {
            return column.accept(new CustomPredicateSpecReplacer(eq, i -> i.equals(value)));
        }
    }

    @Override
    public <T> FilterPredicate visit(final NotEqualTo<T> neq) {
        return visitInternal(neq);
    }

    private <T, C extends TypedColumn<T>> FilterPredicate visitInternal(final NotEqualTo<T> neq) {
        final TypedColumn<T> column = neq.getColumn();
        final T value = neq.getValue();
        try {
            final BiFunction<C, T, FilterPredicate> function = (c, v) -> notEqual(c, v);
            return column.accept(new ValuePredicateSpecReplacer(neq, function, value));
        } catch (UnsupportedOperationException e) {
            return column.accept(new CustomPredicateSpecReplacer(neq, i -> !i.equals(value)));
        }
    }

    @Override
    public <T extends Comparable<T>> FilterPredicate visit(final LesserThan<T> lt) {
        return visitInternal(lt);
    }

    private <T extends Comparable<T>, C extends OrderColumn<T>> FilterPredicate visitInternal(final LesserThan<T> lt) {
        final TypedColumn<T> column = lt.getColumn();
        final T value = lt.getValue();
        try {
            final BiFunction<C, T, FilterPredicate> function = (c, v) -> lesser(c, v);
            return column.accept(new ValuePredicateSpecReplacer(lt, function, value));
        } catch (UnsupportedOperationException e) {
            final Predicate<T> predicate = i -> i.compareTo(value) < 0;
            return column.accept(new CustomPredicateSpecReplacer(lt, predicate));
        }
    }

    @Override
    public <T extends Comparable<T>> FilterPredicate visit(final LesserThanOrEqualTo<T> leq) {
        return visitInternal(leq);
    }

    private <T extends Comparable<T>, C extends OrderColumn<T>> FilterPredicate
        visitInternal(final LesserThanOrEqualTo<T> leq) {
        final TypedColumn<T> column = leq.getColumn();
        final T value = leq.getValue();
        try {
            final BiFunction<C, T, FilterPredicate> function = (c, v) -> lesserOrEqual(c, v);
            return column.accept(new ValuePredicateSpecReplacer(leq, function, value));
        } catch (UnsupportedOperationException e) {
            final Predicate<T> predicate = i -> i.compareTo(value) <= 0;
            return column.accept(new CustomPredicateSpecReplacer(leq, predicate));
        }
    }

    @Override
    public <T extends Comparable<T>> FilterPredicate visit(final GreaterThan<T> gt) {
        return visitInternal(gt);
    }

    private <T extends Comparable<T>, C extends OrderColumn<T>> FilterPredicate visitInternal(final GreaterThan<T> gt) {
        final TypedColumn<T> column = gt.getColumn();
        final T value = gt.getValue();
        try {
            final BiFunction<C, T, FilterPredicate> function = (c, v) -> greater(c, v);
            return column.accept(new ValuePredicateSpecReplacer(gt, function, value));
        } catch (UnsupportedOperationException e) {
            final Predicate<T> predicate = i -> i.compareTo(value) > 0;
            return column.accept(new CustomPredicateSpecReplacer(gt, predicate));
        }

    }

    @Override
    public <T extends Comparable<T>> FilterPredicate visit(final GreaterThanOrEqualTo<T> geq) {
        return visitInternal(geq);
    }

    private <T extends Comparable<T>, C extends OrderColumn<T>> FilterPredicate
        visitInternal(final GreaterThanOrEqualTo<T> geq) {
        final TypedColumn<T> column = geq.getColumn();
        final T value = geq.getValue();
        try {
            final BiFunction<C, T, FilterPredicate> function = (c, v) -> greaterOrEqual(c, v);
            return column.accept(new ValuePredicateSpecReplacer(geq, function, value));
        } catch (UnsupportedOperationException e) {
            final Predicate<T> predicate = i -> i.compareTo(value) >= 0;
            return column.accept(new CustomPredicateSpecReplacer(geq, predicate));
        }
    }

    @Override
    public FilterPredicate visit(final And and) {
        return and.getLeft().accept(this).and(and.getRight().accept(this));
    }

    @Override
    public FilterPredicate visit(final Or or) {
        return or.getLeft().accept(this).or(or.getRight().accept(this));
    }

    @Override
    public FilterPredicate visit(final Not not) {
        return not.getPredicate().accept(this).negate();
    }

    private final class CustomPredicateSpecReplacer
        implements org.knime.core.data.container.filter.predicate.TypedColumn.Visitor<FilterPredicate> {

        private final FilterPredicate m_original;

        private final Predicate<?> m_predicate;

        CustomPredicateSpecReplacer(final FilterPredicate original, final Predicate<?> predicate) {
            m_original = original;
            m_predicate = predicate;
        }

        @Override
        public FilterPredicate visit(final RowKeyColumn rowKey) {
            throw new UnsupportedOperationException("Custom filter predicates are not allowed on row keys.");
        }

        @Override
        public FilterPredicate visit(final IntColumn intCol) {
            final int index = intCol.getIndex();
            final DataType newType = m_newSpec.getColumnSpec(index).getType();

            if (newType.equals(IntCell.TYPE)) {
                return m_original;
            }

            @SuppressWarnings("unchecked")
            final Predicate<Integer> predicate = (Predicate<Integer>)m_predicate;
            if (newType.equals(BooleanCell.TYPE)) {
                return custom(boolCol(index), i -> predicate.test(i ? 1 : 0));
            }

            throw new UnsupportedOperationException(
                "Cannot convert filter predicate of type integer to type " + newType + ".");
        }

        @Override
        public FilterPredicate visit(final LongColumn longCol) {
            final int index = longCol.getIndex();
            final DataType newType = m_newSpec.getColumnSpec(index).getType();
            if (newType.equals(LongCell.TYPE)) {
                return m_original;
            }

            @SuppressWarnings("unchecked")
            final Predicate<Long> predicate = (Predicate<Long>)m_predicate;
            if (newType.equals(IntCell.TYPE)) {
                return custom(intCol(index), i -> predicate.test(new Long(i)));
            } else if (newType.equals(BooleanCell.TYPE)) {
                return custom(boolCol(index), i -> predicate.test(i ? 1L : 0L));
            }

            throw new UnsupportedOperationException(
                "Cannot convert filter predicate of type long to type " + newType + ".");
        }

        @Override
        public FilterPredicate visit(final DoubleColumn doubleCol) {
            final int index = doubleCol.getIndex();
            final DataType newType = m_newSpec.getColumnSpec(index).getType();
            if (newType.equals(DoubleCell.TYPE)) {
                return m_original;
            }

            @SuppressWarnings("unchecked")
            final Predicate<Double> predicate = (Predicate<Double>)m_predicate;
            if (newType.equals(LongCell.TYPE)) {
                return custom(longCol(index), i -> predicate.test(new Double(i)));
            } else if (newType.equals(IntCell.TYPE)) {
                return custom(intCol(index), i -> predicate.test(new Double(i)));
            } else if (newType.equals(BooleanCell.TYPE)) {
                return custom(boolCol(index), i -> predicate.test(i ? 1d : 0d));
            }

            throw new UnsupportedOperationException(
                "Cannot convert filter predicate of type double to type " + newType + ".");
        }

        @Override
        public FilterPredicate visit(final BooleanColumn boolCol) {
            final int index = boolCol.getIndex();
            final DataType newType = m_newSpec.getColumnSpec(index).getType();
            if (newType.equals(BooleanCell.TYPE)) {
                return m_original;
            }

            throw new UnsupportedOperationException(
                "Cannot convert filter predicate of type boolean to type " + newType + ".");
        }

        @Override
        public FilterPredicate visit(final StringColumn stringCol) {
            final int index = stringCol.getIndex();
            final DataType newType = m_newSpec.getColumnSpec(index).getType();
            if (newType.equals(BooleanCell.TYPE)) {
                return m_original;
            }

            throw new UnsupportedOperationException(
                "Cannot convert filter predicate of type String to type " + newType + ".");
        }

    }

    private final class MissingValuePredicateSpecReplacer
        implements org.knime.core.data.container.filter.predicate.TypedColumn.Visitor<FilterPredicate> {

        private FilterPredicate convertTo(final int index) {
            final DataType newType = m_newSpec.getColumnSpec(index).getType();
            if (newType.equals(IntCell.TYPE)) {
                return isMissing(intCol(index));
            } else if (newType.equals(LongCell.TYPE)) {
                return isMissing(longCol(index));
            } else if (newType.equals(DoubleCell.TYPE)) {
                return isMissing(doubleCol(index));
            } else if (newType.equals(BooleanCell.TYPE)) {
                return isMissing(boolCol(index));
            } else if (newType.equals(StringCell.TYPE)) {
                return isMissing(stringCol(index));
            } else {
                throw new UnsupportedOperationException(
                    "Cannot convert missing filter predicate to type " + newType + ".");
            }
        }

        @Override
        public FilterPredicate visit(final RowKeyColumn rowKey) {
            throw new UnsupportedOperationException("Missing value filter predicates are not allowed on row keys.");
        }

        @Override
        public FilterPredicate visit(final IntColumn intCol) {
            return convertTo(intCol.getIndex());
        }

        @Override
        public FilterPredicate visit(final LongColumn longCol) {
            return convertTo(longCol.getIndex());
        }

        @Override
        public FilterPredicate visit(final DoubleColumn doubleCol) {
            return convertTo(doubleCol.getIndex());
        }

        @Override
        public FilterPredicate visit(final BooleanColumn boolCol) {
            return convertTo(boolCol.getIndex());
        }

        @Override
        public FilterPredicate visit(final StringColumn stringCol) {
            return convertTo(stringCol.getIndex());
        }

    }

    private final class ValuePredicateSpecReplacer
        implements org.knime.core.data.container.filter.predicate.TypedColumn.Visitor<FilterPredicate> {

        private final FilterPredicate m_original;

        private final BiFunction<?, ?, FilterPredicate> m_function;

        private final Object m_value;

        ValuePredicateSpecReplacer(final FilterPredicate original, final BiFunction<?, ?, FilterPredicate> function,
            final Object value) {
            m_original = original;
            m_function = function;
            m_value = value;
        }

        @Override
        public FilterPredicate visit(final RowKeyColumn rowKey) {
            @SuppressWarnings("unchecked")
            final BiFunction<RowKeyColumn, String, FilterPredicate> func =
                (BiFunction<RowKeyColumn, String, FilterPredicate>)m_function;
            final String value = (String)m_value;

            return func.apply(rowKey(), value);
        }

        @Override
        public FilterPredicate visit(final IntColumn intCol) {
            final int index = intCol.getIndex();
            final DataType newType = m_newSpec.getColumnSpec(index).getType();
            if (newType.equals(IntCell.TYPE)) {
                return m_original;
            }

            final Integer value = (Integer)m_value;
            if (newType.equals(LongCell.TYPE)) {
                @SuppressWarnings("unchecked")
                final BiFunction<TypedColumn<Long>, Long, FilterPredicate> func =
                    (BiFunction<TypedColumn<Long>, Long, FilterPredicate>)m_function;
                return func.apply(longCol(index), new Long(value));
            } else if (newType.equals(DoubleCell.TYPE)) {
                @SuppressWarnings("unchecked")
                final BiFunction<TypedColumn<Double>, Double, FilterPredicate> func =
                    (BiFunction<TypedColumn<Double>, Double, FilterPredicate>)m_function;
                return func.apply(doubleCol(index), new Double(value));
            } else if (newType.equals(BooleanCell.TYPE)) {
                @SuppressWarnings("unchecked")
                final BiFunction<TypedColumn<Boolean>, Boolean, FilterPredicate> func =
                    (BiFunction<TypedColumn<Boolean>, Boolean, FilterPredicate>)m_function;
                return func.apply(boolCol(index), value != 0);
            }

            throw new UnsupportedOperationException(
                "Cannot convert filter predicate of type integer to type " + newType + ".");
        }

        @Override
        public FilterPredicate visit(final LongColumn longCol) {
            final int index = longCol.getIndex();
            final DataType newType = m_newSpec.getColumnSpec(index).getType();
            if (newType.equals(LongCell.TYPE)) {
                return m_original;
            }

            final Long value = (Long)m_value;
            if (newType.equals(DoubleCell.TYPE)) {
                @SuppressWarnings("unchecked")
                final BiFunction<TypedColumn<Double>, Double, FilterPredicate> func =
                    (BiFunction<TypedColumn<Double>, Double, FilterPredicate>)m_function;
                return func.apply(doubleCol(index), new Double(value));
            } else if (newType.equals(BooleanCell.TYPE)) {
                @SuppressWarnings("unchecked")
                final BiFunction<TypedColumn<Boolean>, Boolean, FilterPredicate> func =
                    (BiFunction<TypedColumn<Boolean>, Boolean, FilterPredicate>)m_function;
                return func.apply(boolCol(index), value != 0L);
            }

            throw new UnsupportedOperationException(
                "Cannot convert filter predicate of type long to type " + newType + ".");
        }

        @Override
        public FilterPredicate visit(final DoubleColumn doubleCol) {
            final int index = doubleCol.getIndex();
            final DataType newType = m_newSpec.getColumnSpec(index).getType();
            if (newType.equals(DoubleCell.TYPE)) {
                return m_original;
            }

            final Double value = (Double)m_value;
            if (newType.equals(BooleanCell.TYPE)) {
                @SuppressWarnings("unchecked")
                final BiFunction<TypedColumn<Boolean>, Boolean, FilterPredicate> func =
                    (BiFunction<TypedColumn<Boolean>, Boolean, FilterPredicate>)m_function;
                return func.apply(boolCol(index), value != 0d);
            }

            throw new UnsupportedOperationException(
                "Cannot convert filter predicate of type long to type " + newType + ".");
        }

        @Override
        public FilterPredicate visit(final BooleanColumn boolCol) {
            final int index = boolCol.getIndex();
            final DataType newType = m_newSpec.getColumnSpec(index).getType();
            if (newType.equals(BooleanCell.TYPE)) {
                return m_original;
            }

            final Boolean value = (Boolean)m_value;
            if (newType.equals(IntCell.TYPE)) {
                @SuppressWarnings("unchecked")
                final BiFunction<TypedColumn<Integer>, Integer, FilterPredicate> func =
                    (BiFunction<TypedColumn<Integer>, Integer, FilterPredicate>)m_function;
                return func.apply(intCol(index), value ? 1 : 0);
            } else if (newType.equals(LongCell.TYPE)) {
                @SuppressWarnings("unchecked")
                final BiFunction<TypedColumn<Long>, Long, FilterPredicate> func =
                    (BiFunction<TypedColumn<Long>, Long, FilterPredicate>)m_function;
                return func.apply(longCol(index), value ? 1L : 0L);
            } else if (newType.equals(DoubleCell.TYPE)) {
                @SuppressWarnings("unchecked")
                final BiFunction<TypedColumn<Double>, Double, FilterPredicate> func =
                    (BiFunction<TypedColumn<Double>, Double, FilterPredicate>)m_function;
                return func.apply(doubleCol(index), value ? 1d : 0d);
            }

            throw new UnsupportedOperationException(
                "Cannot convert filter predicate of type boolean to type " + newType + ".");
        }

        @Override
        public FilterPredicate visit(final StringColumn stringCol) {
            final int index = stringCol.getIndex();
            final DataType newType = m_newSpec.getColumnSpec(index).getType();
            if (newType.equals(StringCell.TYPE)) {
                return m_original;
            }

            throw new UnsupportedOperationException(
                "Cannot convert filter predicate of type string to type " + newType + ".");
        }

    }

}
