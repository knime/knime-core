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
 *   17 Apr 2019 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.data.container.filter.predicate;

import java.util.function.Function;

import org.knime.core.data.DataRow;
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
 * The visitor that determines whether a {@link DataRow} should be retained or dropped.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @since 3.8
 */
public final class FilterPredicateToDataRowApplier implements Visitor<Boolean> {

    private static final Function<?, Boolean> IS_MISSING = t -> t == null;

    private final DataRow m_dataRow;

    /**
     * Constructor.
     *
     * @param dataRow the row for which this visitor should decide whether to retain or drop it
     */
    public FilterPredicateToDataRowApplier (final DataRow dataRow) {
        m_dataRow = dataRow;
    }

    @Override
    public <T> Boolean visit(final MissingValuePredicate<T> mvp) {
        return mvp.getColumn().accept(new TypedColumnToDataRowApplier(m_dataRow, IS_MISSING));
    }

    @Override
    public <T> Boolean visit(final CustomPredicate<T> udf) {
        final Function<T, Boolean> function = t -> t != null && udf.getPredicate().test(t);
        return udf.getColumn().accept(new TypedColumnToDataRowApplier(m_dataRow, function));
    }

    @Override
    public <T> Boolean visit(final EqualTo<T> eq) {
        final Function<T, Boolean> function = t -> t != null && t.equals(eq.getValue());
        return eq.getColumn().accept(new TypedColumnToDataRowApplier(m_dataRow, function));
    }

    @Override
    public <T> Boolean visit(final NotEqualTo<T> neq) {
        final Function<T, Boolean> function = t -> t != null && !t.equals(neq.getValue());
        return neq.getColumn().accept(new TypedColumnToDataRowApplier(m_dataRow, function));
    }

    @Override
    public <T extends Comparable<T>> Boolean visit(final LesserThan<T> lt) {
        final Function<T, Boolean> function = t -> t != null && t.compareTo(lt.getValue()) < 0;
        return lt.getColumn().accept(new TypedColumnToDataRowApplier(m_dataRow, function));
    }

    @Override
    public <T extends Comparable<T>> Boolean visit(final LesserThanOrEqualTo<T> leq) {
        final Function<T, Boolean> function = t -> t != null && t.compareTo(leq.getValue()) <= 0;
        return leq.getColumn().accept(new TypedColumnToDataRowApplier(m_dataRow, function));
    }

    @Override
    public <T extends Comparable<T>> Boolean visit(final GreaterThan<T> gt) {
        final Function<T, Boolean> function = t -> t != null && t.compareTo(gt.getValue()) > 0;
        return gt.getColumn().accept(new TypedColumnToDataRowApplier(m_dataRow, function));
    }

    @Override
    public <T extends Comparable<T>> Boolean visit(final GreaterThanOrEqualTo<T> geq) {
        final Function<T, Boolean> function = t -> t != null && t.compareTo(geq.getValue()) >= 0;
        return geq.getColumn().accept(new TypedColumnToDataRowApplier(m_dataRow, function));
    }

    @Override
    public Boolean visit(final And and) {
        return and.getLeft().accept(this) && and.getRight().accept(this);
    }

    @Override
    public Boolean visit(final Or or) {
        return or.getLeft().accept(this) || or.getRight().accept(this);
    }

    @Override
    public Boolean visit(final Not not) {
        return !not.getPredicate().accept(this);
    }

    static final class TypedColumnToDataRowApplier
        implements org.knime.core.data.container.filter.predicate.TypedColumn.Visitor<Boolean> {

        private final DataRow m_dataRow;

        private final Function<?, Boolean> m_function;

        TypedColumnToDataRowApplier (final DataRow dataRow, final Function<?, Boolean> function) {
            m_dataRow = dataRow;
            m_function = function;
        }

        @Override
        public Boolean visit(final RowKeyColumn rowKey) {
            final String value = m_dataRow.getKey().getString();
            @SuppressWarnings("unchecked")
            final Function<String, Boolean> function = (Function<String, Boolean>)m_function;
            return function.apply(value);
        }

        @Override
        public Boolean visit(final IntColumn intCol) {
            final Integer value = intCol.getValue(m_dataRow.getCell(intCol.getIndex()));
            @SuppressWarnings("unchecked")
            final Function<Integer, Boolean> function = (Function<Integer, Boolean>)m_function;
            return function.apply(value);
        }

        @Override
        public Boolean visit(final LongColumn longCol) {
            final Long value = longCol.getValue(m_dataRow.getCell(longCol.getIndex()));
            @SuppressWarnings("unchecked")
            final Function<Long, Boolean> function = (Function<Long, Boolean>)m_function;
            return function.apply(value);
        }

        @Override
        public Boolean visit(final DoubleColumn doubleCol) {
            final Double value = doubleCol.getValue(m_dataRow.getCell(doubleCol.getIndex()));
            @SuppressWarnings("unchecked")
            final Function<Double, Boolean> function = (Function<Double, Boolean>)m_function;
            return function.apply(value);
        }

        @Override
        public Boolean visit(final BooleanColumn boolCol) {
            final Boolean value = boolCol.getValue(m_dataRow.getCell(boolCol.getIndex()));
            @SuppressWarnings("unchecked")
            final Function<Boolean, Boolean> function = (Function<Boolean, Boolean>)m_function;
            return function.apply(value);
        }

        @Override
        public Boolean visit(final StringColumn stringCol) {
            final String value = stringCol.getValue(m_dataRow.getCell(stringCol.getIndex()));
            @SuppressWarnings("unchecked")
            final Function<String, Boolean> function = (Function<String, Boolean>)m_function;
            return function.apply(value);
        }

    }

}
