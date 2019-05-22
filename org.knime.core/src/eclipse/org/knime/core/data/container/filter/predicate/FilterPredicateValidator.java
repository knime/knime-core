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

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
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
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;

/**
 * Helper class that can be used to validate {@link FilterPredicate FilterPredicates} against a given
 * {@link DataTableSpec}. Note that it is not sufficient for {@link TypedColumn Columns} in the predicate to be
 * compatible (in the sense of {@link DataType#isCompatible(Class)}) to the type specified in the {@link DataTableSpec}
 * for that column. The types have to be actually equal. The reason for this is that {@link FilterPredicate
 * FilterPredicates} are intended to be pushed down to the underlying table stores and applied to stored values before
 * {@link DataCell DataCells} and {@link DataRow DataRows} are actually materialized.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @since 3.8
 */
public final class FilterPredicateValidator implements Visitor<Void> {

    private final ColumnValidator m_columnValidator;

    /**
     * Constructs a new validator visitor for validating {@link FilterPredicate FilterPredicates} against a
     * {@link DataTableSpec}.
     *
     * @param spec the table spec to validate against
     */
    public FilterPredicateValidator(final DataTableSpec spec) {
        m_columnValidator = new ColumnValidator(spec);
    }

    @Override
    public <T> Void visit(final MissingValuePredicate<T> mvp) {
        mvp.getColumn().accept(m_columnValidator);
        return null;
    }

    @Override
    public <T> Void visit(final CustomPredicate<T> udf) {
        udf.getColumn().accept(m_columnValidator);
        return null;
    }

    @Override
    public <T> Void visit(final EqualTo<T> eq) {
        eq.getColumn().accept(m_columnValidator);
        return null;
    }

    @Override
    public <T> Void visit(final NotEqualTo<T> neq) {
        neq.getColumn().accept(m_columnValidator);
        return null;
    }

    @Override
    public <T extends Comparable<T>> Void visit(final LesserThan<T> lt) {
        lt.getColumn().accept(m_columnValidator);
        return null;
    }

    @Override
    public <T extends Comparable<T>> Void visit(final LesserThanOrEqualTo<T> leq) {
        leq.getColumn().accept(m_columnValidator);
        return null;
    }

    @Override
    public <T extends Comparable<T>> Void visit(final GreaterThan<T> gt) {
        gt.getColumn().accept(m_columnValidator);
        return null;
    }

    @Override
    public <T extends Comparable<T>> Void visit(final GreaterThanOrEqualTo<T> geq) {
        geq.getColumn().accept(m_columnValidator);
        return null;
    }

    @Override
    public Void visit(final And and) {
        and.getLeft().accept(this);
        and.getRight().accept(this);
        return null;
    }

    @Override
    public Void visit(final Or or) {
        or.getLeft().accept(this);
        or.getRight().accept(this);
        return null;
    }

    @Override
    public Void visit(final Not not) {
        not.getPredicate().accept(this);
        return null;
    }

    /**
     * Helper class used by a {@link FilterPredicateValidator} to validate {@link TypedColumn Columns} against a given
     * {@link DataTableSpec}.
     */
    private static final class ColumnValidator
        implements org.knime.core.data.container.filter.predicate.TypedColumn.Visitor<Void> {

        private final DataTableSpec m_spec;

        ColumnValidator(final DataTableSpec spec) {
            m_spec = spec;
        }

        private void checkIndexOOB(final int index) {
            if (index < 0 || index >= m_spec.getNumColumns()) {
                throw new IndexOutOfBoundsException("Column index out of range: " + index);
            }
        }

        private void checkTypesEqual(final int index, final DataType expected) {
            if (!m_spec.getColumnSpec(index).getType().equals(expected)) {
                throw new IllegalArgumentException(
                    "Column at index " + index + " is of type " + m_spec.getColumnSpec(index).getType().getName()
                        + ", which is incompatible to " + expected.getName() + ".");
            }
        }

        @Override
        public Void visit(final RowKeyColumn rowKey) {
            return null;
        }

        @Override
        public Void visit(final IntColumn intCol) {
            final int index = intCol.getIndex();
            checkIndexOOB(index);
            checkTypesEqual(index, IntCell.TYPE);
            return null;
        }

        @Override
        public Void visit(final LongColumn longCol) {
            final int index = longCol.getIndex();
            checkIndexOOB(index);
            checkTypesEqual(index, LongCell.TYPE);
            return null;
        }

        @Override
        public Void visit(final DoubleColumn doubleCol) {
            final int index = doubleCol.getIndex();
            checkIndexOOB(index);
            checkTypesEqual(index, DoubleCell.TYPE);
            return null;
        }

        @Override
        public Void visit(final BooleanColumn boolCol) {
            final int index = boolCol.getIndex();
            checkIndexOOB(index);
            checkTypesEqual(index, BooleanCell.TYPE);
            return null;
        }

        @Override
        public Void visit(final StringColumn stringCol) {
            final int index = stringCol.getIndex();
            checkIndexOOB(index);
            checkTypesEqual(index, StringCell.TYPE);
            return null;
        }

    }

}
