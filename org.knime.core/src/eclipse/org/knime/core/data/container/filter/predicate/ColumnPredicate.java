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

import org.knime.core.data.MissingValue;

/**
 * Abstract class for {@link FilterPredicate FilterPredicates} expressing logical operations on {@link TypedColumn
 * Columns}.
 *
 * @param <T> the type of the column on which this predicate shall be applied
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @since 3.8
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public abstract class ColumnPredicate<T> implements FilterPredicate {

    private final TypedColumn<T> m_column;

    ColumnPredicate(final TypedColumn<T> column) {
        m_column = column;
    }

    /**
     * A method for obtaining the {@link TypedColumn} this {@link ColumnPredicate} is defined on.
     *
     * @return the column of this predicate
     */
    public TypedColumn<T> getColumn() {
        return m_column;
    }

    /**
     * A {@link ColumnPredicate} that checks if the value in a given {@link TypedColumn Column} is a
     * {@link MissingValue}.
     *
     * @param <T> the type of the column on which this predicate shall be applied
     */
    public static final class MissingValuePredicate<T> extends ColumnPredicate<T> {

        MissingValuePredicate(final TypedColumn<T> column) {
            super(column);
        }

        @Override
        public <R> R accept(final Visitor<R> v) {
            return v.visit(this);
        }

        @Override
        public String toString() {
            return getColumn().toString() + " is missing";
        }

    }

    /**
     * A {@link ColumnPredicate} that checks if the value in a given {@link TypedColumn Column} evaluates to true in
     * that column for a given {@link Predicate}.
     *
     * @param <T> the type of the column on which this predicate shall be applied
     */
    public static final class CustomPredicate<T> extends ColumnPredicate<T> {

        private final Predicate<T> m_predicate;

        CustomPredicate(final TypedColumn<T> column, final Predicate<T> predicate) {
            super(column);
            m_predicate = predicate;
        }

        @Override
        public <R> R accept(final Visitor<R> v) {
            return v.visit(this);
        }

        /**
         * Method for obtaining the {@link Predicate} that is applied by this {@link ColumnPredicate}.
         *
         * @return the custom predicate
         */
        public Predicate<T> getPredicate() {
            return m_predicate;
        }

        @Override
        public String toString() {
            return getColumn().toString() + " with custom predicate";
        }

    }

    /**
     * Abstract class for {@link ColumnPredicate ColumnPredicates} expressing logical operations that compare the value
     * in a given {@link TypedColumn Column} against some value.
     *
     * @param <T> the type of the column on which this predicate shall be applied
     * @noextend This interface is not intended to be extended by clients.
     * @noimplement This interface is not intended to be implemented by clients.
     */
    public static abstract class ValuePredicate<T> extends ColumnPredicate<T> {

        private final T m_value;

        private ValuePredicate(final TypedColumn<T> column, final T value) {
            super(column);
            m_value = value;
        }

        /**
         * Method for obtaining the value that is compared against by this {@link ValuePredicate}.
         *
         * @return this predicate's value
         */
        public T getValue() {
            return m_value;
        }
    }

    static abstract class EquivalencePredicate<T> extends ValuePredicate<T> {
        EquivalencePredicate(final TypedColumn<T> column, final T value) {
            super(column, value);
        }
    }

    /**
     * A {@link ValuePredicate} that checks if the value in a given {@link TypedColumn Column} is equal to some value.
     *
     * @param <T> the type of the column on which this predicate shall be applied
     */
    public static final class EqualTo<T> extends EquivalencePredicate<T> {

        EqualTo(final TypedColumn<T> column, final T value) {
            super(column, value);
        }

        @Override
        public <R> R accept(final Visitor<R> v) {
            return v.visit(this);
        }

        @Override
        public String toString() {
            return getColumn().toString() + " equal to " + getValue();
        }

    }

    /**
     * A {@link ValuePredicate} that checks if the value in a given {@link TypedColumn Column} is not equal to some
     * value.
     *
     * @param <T> the type of the column on which this predicate shall be applied
     */
    public static final class NotEqualTo<T> extends EquivalencePredicate<T> {

        NotEqualTo(final TypedColumn<T> column, final T value) {
            super(column, value);
        }

        @Override
        public <R> R accept(final Visitor<R> v) {
            return v.visit(this);
        }

        @Override
        public String toString() {
            return getColumn().toString() + " not equal to " + getValue();
        }

    }

    static abstract class OrderPredicate<T extends Comparable<T>> extends ValuePredicate<T> {
        OrderPredicate(final TypedColumn<T> column, final T value) {
            super(column, value);
        }
    }

    /**
     * A {@link ValuePredicate} that checks if the value in a given {@link TypedColumn Column} is lesser than some
     * value.
     *
     * @param <T> the comparable type of the column on which this predicate shall be applied
     */
    public static final class LesserThan<T extends Comparable<T>> extends OrderPredicate<T> {

        LesserThan(final TypedColumn<T> column, final T value) {
            super(column, value);
        }

        @Override
        public <R> R accept(final Visitor<R> v) {
            return v.visit(this);
        }

        @Override
        public String toString() {
            return getColumn().toString() + " lesser than " + getValue();
        }

    }

    /**
     * A {@link ValuePredicate} that checks if the value in a given {@link TypedColumn Column} is lesser than or equal
     * to some value.
     *
     * @param <T> the comparable type of the column on which this predicate shall be applied
     */
    public static final class LesserThanOrEqualTo<T extends Comparable<T>> extends OrderPredicate<T> {

        LesserThanOrEqualTo(final TypedColumn<T> column, final T value) {
            super(column, value);
        }

        @Override
        public <R> R accept(final Visitor<R> v) {
            return v.visit(this);
        }

        @Override
        public String toString() {
            return getColumn().toString() + " lesser than or equal to " + getValue();
        }

    }

    /**
     * A {@link ValuePredicate} that checks if the value in a given {@link TypedColumn Column} is greater than some
     * value.
     *
     * @param <T> the comparable type of the column on which this predicate shall be applied
     */
    public static final class GreaterThan<T extends Comparable<T>> extends OrderPredicate<T> {

        GreaterThan(final TypedColumn<T> column, final T value) {
            super(column, value);
        }

        @Override
        public <R> R accept(final Visitor<R> v) {
            return v.visit(this);
        }

        @Override
        public String toString() {
            return getColumn().toString() + " greater than " + getValue();
        }

    }

    /**
     * A {@link ValuePredicate} that checks if the value in a given {@link TypedColumn Column} is greater than or equal
     * to some value.
     *
     * @param <T> the comparable type of the column on which this predicate shall be applied
     */
    public static final class GreaterThanOrEqualTo<T extends Comparable<T>> extends OrderPredicate<T> {

        GreaterThanOrEqualTo(final TypedColumn<T> column, final T value) {
            super(column, value);
        }

        @Override
        public <R> R accept(final Visitor<R> v) {
            return v.visit(this);
        }

        @Override
        public String toString() {
            return getColumn().toString() + " greater than or equal to " + getValue();
        }

    }

}
