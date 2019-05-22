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

import org.knime.core.data.DataType;
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
 * A class for specifying a column on which a {@link ColumnPredicate} shall be applied. The class {@link TypedColumn}
 * implements the Visitor design pattern, i.e., the functionality of the class can be extended by implementing the
 * {@link Visitor} interface.
 *
 * @param <T> the type of values which this column holds
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @since 3.8
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface TypedColumn<T> {

    /**
     * A {@link TypedColumn} must accept a {@link Visitor}, as required by the Visitor design pattern. Non-abstract
     * classes implementing this interface should invoke the visitor's visit method on themselves when overriding this
     * method.
     *
     * @param v the visitor that intends to visit this column
     * @return a return value of some type specified by the visitor implementation
     */
    <R> R accept(Visitor<R> v);

    /**
     * Static factory method for creating a new {@link RowKeyColumn}.
     *
     * @return a new {@link RowKeyColumn}
     */
    static RowKeyColumn rowKey() {
        return new RowKeyColumn();
    }

    /**
     * Static factory method for creating a new {@link IntColumn} at a given index, i.e., a column of {@link DataType}
     * {@link IntCell#TYPE}.
     *
     * @param index this column's index
     * @return a new {@link IntColumn}
     */
    static IntColumn intCol(final int index) {
        return new IntColumn(index);
    }

    /**
     * Static factory method for creating a new {@link LongColumn} at a given index, i.e., a column of {@link DataType}
     * {@link LongCell#TYPE}.
     *
     * @param index this column's index
     * @return a new {@link LongColumn}
     */
    static LongColumn longCol(final int index) {
        return new LongColumn(index);
    }

    /**
     * Static factory method for creating a new {@link DoubleColumn} at a given index, i.e., a column of
     * {@link DataType} {@link DoubleCell#TYPE}.
     *
     * @param index this column's index
     * @return a new {@link DoubleColumn}
     */
    static DoubleColumn doubleCol(final int index) {
        return new DoubleColumn(index);
    }

    /**
     * Static factory method for creating a new {@link BooleanColumn} at a given index, i.e., a column of
     * {@link DataType} {@link BooleanCell#TYPE}.
     *
     * @param index this column's index
     * @return a new {@link BooleanColumn}
     */
    static BooleanColumn boolCol(final int index) {
        return new BooleanColumn(index);
    }

    /**
     * Static factory method for creating a new {@link StringColumn} at a given index, i.e., a column of
     * {@link DataType} {@link StringCell#TYPE}.
     *
     * @param index this column's index
     * @return a new {@link StringColumn}
     */
    static StringColumn stringCol(final int index) {
        return new StringColumn(index);
    }

//    /**
//     * Not-yet-implemented draft for custom column support. Could be invoked like this: <code>
//     * FilterPredicate.equal(TypedColumn.customCol(0), LocalDateCellFactory.create(LocalDate.of(1997, 8, 4)))
//     * </code> Allows for the definition of {@link FilterPredicate FilterPredicates} over arbitrary types (e.g., type
//     * XMLValue or LocalDateCell). Note that to make this work {@link Visitor#visit(CustomColumn)} will have to be
//     * overridden for classes extending {@link Visitor}. See also IndexedColumn.CustomColumn.
//     *
//     * @param index this column's index
//     * @return a new {@link CustomColumn}
//     */
//    static CustomColumn customCol(final int index) {
//        return new CustomColumn(index);
//    }

    /**
     * Implementation of the visitor design pattern for the {@link TypedColumn} class. Enables the introduction of new
     * operations on {@link TypedColumn Columns} without modifying the existing object structure.
     *
     * @param <R> the return type of the visitor
     * @noextend This interface is not intended to be extended by clients.
     * @noimplement This interface is not intended to be implemented by clients.
     * @noreference This interface is not intended to be referenced by clients.
     */
    public interface Visitor<R> {

        /**
         * The method invoked by a {@link RowKeyColumn}, when that column accepts this {@link Visitor}.
         *
         * @param rowKey the column that accepts (and invokes) this visitor's visit
         * @return a return value obtained during the visit
         */
        R visit(final RowKeyColumn rowKey);

        /**
         * The method invoked by a {@link IntColumn}, when that column accepts this {@link Visitor}.
         *
         * @param intCol the column that accepts (and invokes) this visitor's visit
         * @return a return value obtained during the visit
         */
        R visit(final IntColumn intCol);

        /**
         * The method invoked by a {@link LongColumn}, when that column accepts this {@link Visitor}.
         *
         * @param longCol the column that accepts (and invokes) this visitor's visit
         * @return a return value obtained during the visit
         */
        R visit(final LongColumn longCol);

        /**
         * The method invoked by a {@link DoubleColumn}, when that column accepts this {@link Visitor}.
         *
         * @param doubleCol the column that accepts (and invokes) this visitor's visit
         * @return a return value obtained during the visit
         */
        R visit(final DoubleColumn doubleCol);

        /**
         * The method invoked by a {@link BooleanColumn}, when that column accepts this {@link Visitor}.
         *
         * @param boolCol the column that accepts (and invokes) this visitor's visit
         * @return a return value obtained during the visit
         */
        R visit(final BooleanColumn boolCol);

        /**
         * The method invoked by a {@link StringColumn}, when that column accepts this {@link Visitor}.
         *
         * @param stringCol the column that accepts (and invokes) this visitor's visit
         * @return a return value obtained during the visit
         */
        R visit(final StringColumn stringCol);

//        /**
//         * The method invoked by a {@link CustomColumn}, when that column accepts this {@link Visitor}.
//         *
//         * @param customCol the column that accepts (and invokes) this visitor's visit
//         * @return a return value obtained during the visit
//         */
//        default R visit(final CustomColumn customCol) {
//            throw new UnsupportedOperationException("Custom columns not yet supported.");
//        }

    }

}
