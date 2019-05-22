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

import java.util.function.Predicate;

import org.knime.core.data.DataRow;
import org.knime.core.data.MissingValue;
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
import org.knime.core.data.container.filter.predicate.ColumnPredicate.ValuePredicate;
import org.knime.core.data.container.filter.predicate.IndexedColumn.OrderColumn;

/**
 * A class that defines criteria for which {@link DataRow DataRows} to keep when iterating over a collection of rows.
 * The predicate can be expressed as composite logical operations on columns. An example of such an expression is to
 * <i>keep all rows with a value greater than 5 in column 2 and a string matching some regular expression in column
 * 4</i>. The class {@link FilterPredicate} implements the Visitor design pattern, i.e., the functionality of the class
 * can be extended by implementing the {@link Visitor} interface.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @since 3.8
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface FilterPredicate {

    /**
     * A {@link FilterPredicate} must accept a {@link Visitor}, as required by the Visitor design pattern. Non-abstract
     * classes implementing this interface should invoke the visitor's visit method on themselves when overriding this
     * method.
     *
     * @param v the visitor that intends to visit this predicate
     * @return a return value of some type specified by the visitor implementation
     * @noreference This method is not intended to be referenced by clients.
     */
    <R> R accept(Visitor<R> v);

    /**
     * Negates this {@link FilterPredicate} by wrapping it in a {@link Not}.
     *
     * @return the negation of this predicate
     */
    default FilterPredicate negate() {
        return new Not(this);
    }

    /**
     * Builds the logical disjunction ({@link Or}) of this and another {@link FilterPredicate}.
     *
     * @param other another filter predicate
     * @return the logical <b>or</b> of this predicate and the other predicate
     */
    default FilterPredicate or(final FilterPredicate other) {
        return new Or(this, other);
    }

    /**
     * Builds the logical conjunction ({@link And}) of this and another {@link FilterPredicate}.
     *
     * @param other another filter predicate
     * @return the logical <b>and</b> of this predicate and the other predicate
     */
    default FilterPredicate and(final FilterPredicate other) {
        return new And(this, other);
    }

    /**
     * Static factory method for creating a new {@link MissingValuePredicate}, i.e., a {@link ColumnPredicate} on an
     * {@link IndexedColumn} that retains only {@link DataRow DataRows} with a {@link MissingValue} in that column.
     *
     * @param column the column which this predicate shall be applied to
     * @return a new {@link MissingValuePredicate}
     */
    static <T, C extends IndexedColumn<T>> FilterPredicate isMissing(final C column) {
        return new MissingValuePredicate<T>(column);
    }

    /**
     * Static factory method for creating a new {@link CustomPredicate}, i.e., a {@link ColumnPredicate} on an
     * {@link IndexedColumn} that retains only {@link DataRow DataRows} that evaluate to true in that column for a given
     * {@link Predicate}.
     *
     * @param column the column which this predicate shall be applied to
     * @param predicate the custom predicate to translate into a filter predicate
     * @return a new {@link CustomPredicate}
     */
    static <T, C extends IndexedColumn<T>> FilterPredicate custom(final C column, final Predicate<T> predicate) {
        return new CustomPredicate<T>(column, predicate);
    }

    /**
     * Static factory method for creating a new {@link EqualTo}, i.e., a {@link ValuePredicate} on a {@link TypedColumn}
     * that retains only {@link DataRow DataRows} with a value equal to a given value in that column.
     *
     * @param column the column which this predicate shall be applied to
     * @param value the value that another value must be equal to for this predicate to evaluate to true
     * @return a new {@link EqualTo} predicate
     */
    static <T, C extends TypedColumn<T>> FilterPredicate equal(final C column, final T value) {
        return new EqualTo<T>(column, value);
    }

    /**
     * Static factory method for creating a new {@link NotEqualTo}, i.e., a {@link ValuePredicate} on a
     * {@link TypedColumn} that retains only {@link DataRow DataRows} with a value not equal to a given value in that
     * column.
     *
     * @param column the column which this predicate shall be applied to
     * @param value the value that another value must not be equal to for this predicate to evaluate to true
     * @return a new {@link NotEqualTo} predicate
     */
    static <T, C extends TypedColumn<T>> FilterPredicate notEqual(final C column, final T value) {
        return new NotEqualTo<T>(column, value);
    }

    /**
     * Static factory method for creating a new {@link LesserThan}, i.e., a {@link ValuePredicate} on an
     * {@link OrderColumn} that retains only {@link DataRow DataRows} with a value lesser than a given value in that
     * column.
     *
     * @param column the column which this predicate shall be applied to
     * @param value the value that another value must be lesser than for this predicate to evaluate to true
     * @return a new {@link LesserThan} predicate
     */
    static <T extends Comparable<T>, C extends OrderColumn<T>> FilterPredicate lesser(final C column, final T value) {
        return new LesserThan<T>(column, value);
    }

    /**
     * Static factory method for creating a new {@link LesserThanOrEqualTo}, i.e., a {@link ValuePredicate} on an
     * {@link OrderColumn} that retains only {@link DataRow DataRows} with a value lesser than or equal to a given value
     * in that column.
     *
     * @param column the column which this predicate shall be applied to
     * @param value the value that another value must be lesser than or equal to for this predicate to evaluate to true
     * @return a new {@link LesserThanOrEqualTo} predicate
     */
    static <T extends Comparable<T>, C extends OrderColumn<T>> FilterPredicate lesserOrEqual(final C column,
        final T value) {
        return new LesserThanOrEqualTo<T>(column, value);
    }

    /**
     * Static factory method for creating a new {@link GreaterThan}, i.e., a {@link ValuePredicate} on an
     * {@link OrderColumn} that retains only {@link DataRow DataRows} with a value greater than a given value in that
     * column.
     *
     * @param column the column which this predicate shall be applied to
     * @param value the value that another value must be greater than for this predicate to evaluate to true
     * @return a new {@link GreaterThan} predicate
     */
    static <T extends Comparable<T>, C extends OrderColumn<T>> FilterPredicate greater(final C column, final T value) {
        return new GreaterThan<T>(column, value);
    }

    /**
     * Static factory method for creating a new {@link GreaterThanOrEqualTo}, i.e., a {@link ValuePredicate} on an
     * {@link OrderColumn} that retains only {@link DataRow DataRows} with a value greater than or equal to a given
     * value in that column.
     *
     * @param column the column which this predicate shall be applied to
     * @param value the value that another value must be greater than or equal to for this predicate to evaluate to true
     * @return a new {@link GreaterThanOrEqualTo} predicate
     */
    static <T extends Comparable<T>, C extends OrderColumn<T>> FilterPredicate greaterOrEqual(final C column,
        final T value) {
        return new GreaterThanOrEqualTo<T>(column, value);
    }

    /**
     * Implementation of the visitor design pattern for the {@link FilterPredicate} class. Enables the introduction of
     * new operations on {@link FilterPredicate FilterPredicates} without modifying the existing object structure.
     *
     * @param <R> the return type of the visitor
     * @noextend This interface is not intended to be extended by clients.
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This interface is not intended to be referenced by clients.
     */
    public interface Visitor<R> {

        /**
         * The method invoked by a {@link MissingValuePredicate}, when that predicate accepts this {@link Visitor}.
         *
         * @param mvp the predicate that accepts (and invokes) this visitor's visit
         * @return a return value obtained during the visit
         */
        <T> R visit(final MissingValuePredicate<T> mvp);

        /**
         * The method invoked by a {@link CustomPredicate}, when that predicate accepts this {@link Visitor}.
         *
         * @param udf the predicate that accepts (and invokes) this visitor's visit
         * @return a return value obtained during the visit
         */
        <T> R visit(final CustomPredicate<T> udf);

        /**
         * The method invoked by an {@link EqualTo}, when that predicate accepts this {@link Visitor}.
         *
         * @param eq the predicate that accepts (and invokes) this visitor's visit
         * @return a return value obtained during the visit
         */
        <T> R visit(final EqualTo<T> eq);

        /**
         * The method invoked by a {@link NotEqualTo}, when that predicate accepts this {@link Visitor}.
         *
         * @param neq the predicate that accepts (and invokes) this visitor's visit
         * @return a return value obtained during the visit
         */
        <T> R visit(final NotEqualTo<T> neq);

        /**
         * The method invoked by a {@link LesserThan}, when that predicate accepts this {@link Visitor}.
         *
         * @param lt the predicate that accepts (and invokes) this visitor's visit
         * @return a return value obtained during the visit
         */
        <T extends Comparable<T>> R visit(final LesserThan<T> lt);

        /**
         * The method invoked by a {@link LesserThanOrEqualTo}, when that predicate accepts this {@link Visitor}.
         *
         * @param leq the predicate that accepts (and invokes) this visitor's visit
         * @return a return value obtained during the visit
         */
        <T extends Comparable<T>> R visit(final LesserThanOrEqualTo<T> leq);

        /**
         * The method invoked by a {@link GreaterThan}, when that predicate accepts this {@link Visitor}.
         *
         * @param gt the predicate that accepts (and invokes) this visitor's visit
         * @return a return value obtained during the visit
         */
        <T extends Comparable<T>> R visit(final GreaterThan<T> gt);

        /**
         * The method invoked by a {@link GreaterThanOrEqualTo}, when that predicate accepts this {@link Visitor}.
         *
         * @param geq the predicate that accepts (and invokes) this visitor's visit
         * @return a return value obtained during the visit
         */
        <T extends Comparable<T>> R visit(final GreaterThanOrEqualTo<T> geq);

        /**
         * The method invoked by an {@link And}, when that predicate accepts this {@link Visitor}.
         *
         * @param and the predicate that accepts (and invokes) this visitor's visit
         * @return a return value obtained during the visit
         */
        public R visit(final And and);

        /**
         * The method invoked by an {@link Or}, when that predicate accepts this {@link Visitor}.
         *
         * @param or the predicate that accepts (and invokes) this visitor's visit
         * @return a return value obtained during the visit
         */
        public R visit(final Or or);

        /**
         * The method invoked by a {@link Not}, when that predicate accepts this {@link Visitor}.
         *
         * @param not the predicate that accepts (and invokes) this visitor's visit
         * @return a return value obtained during the visit
         */
        public R visit(final Not not);

    }

}
