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

import java.util.function.Function;

import org.knime.core.data.container.RearrangeColumnsTable;
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
import org.knime.core.data.container.filter.predicate.IndexedColumn.StringColumn;

/**
 * Helper class that maps the indices of all {@link ColumnPredicate ColumnPredicates} held by a {@link FilterPredicate}
 * to new values. This is helpful if columns are rearranged and the indices held by the predicate have to be rearranged
 * as well, as for instance in the case of {@link RearrangeColumnsTable RearrangeColumnsTables}.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @since 3.8
 */
public final class FilterPredicateIndexMapper implements Visitor<FilterPredicate> {

    private final int[] m_map;

    /**
     * Constructs a new index mapper visitor. Initialized with an array that maps the old index values onto their new
     * positions. For instance, an index map of [0, 2, 1] entails that the column at the current index 1 is mapped onto
     * the new index 2 and vice versa.
     *
     * @param map an array that maps the old indices onto their new positions
     */
    public FilterPredicateIndexMapper(final int[] map) {
        m_map = map;
    }

    @Override
    public <T> FilterPredicate visit(final MissingValuePredicate<T> mvp) {
        final Function<TypedColumn<T>, FilterPredicate> function = MissingValuePredicate::new;
        return mvp.getColumn().accept(new ColumnIndexMapper(function, m_map));
    }

    @Override
    public <T> FilterPredicate visit(final CustomPredicate<T> udf) {
        final Function<TypedColumn<T>, FilterPredicate> function = c -> new CustomPredicate<T>(c, udf.getPredicate());
        return udf.getColumn().accept(new ColumnIndexMapper(function, m_map));
    }

    @Override
    public <T> FilterPredicate visit(final EqualTo<T> eq) {
        final Function<TypedColumn<T>, FilterPredicate> function = c -> new EqualTo<T>(c, eq.getValue());
        return eq.getColumn().accept(new ColumnIndexMapper(function, m_map));
    }

    @Override
    public <T> FilterPredicate visit(final NotEqualTo<T> neq) {
        final Function<TypedColumn<T>, FilterPredicate> function = c -> new NotEqualTo<T>(c, neq.getValue());
        return neq.getColumn().accept(new ColumnIndexMapper(function, m_map));
    }

    @Override
    public <T extends Comparable<T>> FilterPredicate visit(final LesserThan<T> lt) {
        final Function<TypedColumn<T>, FilterPredicate> function = c -> new LesserThan<T>(c, lt.getValue());
        return lt.getColumn().accept(new ColumnIndexMapper(function, m_map));
    }

    @Override
    public <T extends Comparable<T>> FilterPredicate visit(final LesserThanOrEqualTo<T> leq) {
        final Function<TypedColumn<T>, FilterPredicate> function = c -> new LesserThanOrEqualTo<T>(c, leq.getValue());
        return leq.getColumn().accept(new ColumnIndexMapper(function, m_map));
    }

    @Override
    public <T extends Comparable<T>> FilterPredicate visit(final GreaterThan<T> gt) {
        final Function<TypedColumn<T>, FilterPredicate> function = c -> new GreaterThan<T>(c, gt.getValue());
        return gt.getColumn().accept(new ColumnIndexMapper(function, m_map));
    }

    @Override
    public <T extends Comparable<T>> FilterPredicate visit(final GreaterThanOrEqualTo<T> geq) {
        final Function<TypedColumn<T>, FilterPredicate> function = c -> new GreaterThanOrEqualTo<T>(c, geq.getValue());
        return geq.getColumn().accept(new ColumnIndexMapper(function, m_map));
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

    private static final class ColumnIndexMapper
        implements org.knime.core.data.container.filter.predicate.TypedColumn.Visitor<FilterPredicate> {

        private final Function<?, FilterPredicate> m_function;

        private final int[] m_map;

        ColumnIndexMapper(final Function<?, FilterPredicate> function, final int[] map) {
            m_function = function;
            m_map = map;
        }

        @Override
        public FilterPredicate visit(final RowKeyColumn rowKey) {
            @SuppressWarnings("unchecked")
            final Function<RowKeyColumn, FilterPredicate> func = (Function<RowKeyColumn, FilterPredicate>)m_function;
            return func.apply(new RowKeyColumn());
        }

        @Override
        public FilterPredicate visit(final IntColumn intCol) {
            @SuppressWarnings("unchecked")
            final Function<IntColumn, FilterPredicate> func = (Function<IntColumn, FilterPredicate>)m_function;
            return func.apply(new IntColumn(m_map[intCol.getIndex()]));
        }

        @Override
        public FilterPredicate visit(final LongColumn longCol) {
            @SuppressWarnings("unchecked")
            final Function<LongColumn, FilterPredicate> func = (Function<LongColumn, FilterPredicate>)m_function;
            return func.apply(new LongColumn(m_map[longCol.getIndex()]));
        }

        @Override
        public FilterPredicate visit(final DoubleColumn doubleCol) {
            @SuppressWarnings("unchecked")
            final Function<DoubleColumn, FilterPredicate> func = (Function<DoubleColumn, FilterPredicate>)m_function;
            return func.apply(new DoubleColumn(m_map[doubleCol.getIndex()]));
        }

        @Override
        public FilterPredicate visit(final BooleanColumn boolCol) {
            @SuppressWarnings("unchecked")
            final Function<BooleanColumn, FilterPredicate> func = (Function<BooleanColumn, FilterPredicate>)m_function;
            return func.apply(new BooleanColumn(m_map[boolCol.getIndex()]));
        }

        @Override
        public FilterPredicate visit(final StringColumn stringCol) {
            @SuppressWarnings("unchecked")
            final Function<StringColumn, FilterPredicate> func = (Function<StringColumn, FilterPredicate>)m_function;
            return func.apply(new StringColumn(m_map[stringCol.getIndex()]));
        }

    }

}
