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

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;

/**
 * Abstract class for {@link TypedColumn Columns} that can be accessed by their index.
 *
 * @param <T> the type of values which this column holds
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @since 3.8
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public abstract class IndexedColumn<T> implements TypedColumn<T> {
    private final int m_index;

    IndexedColumn(final int index) {
        m_index = index;
    }

    T getValue(final DataCell cell) {
        return cell.isMissing() ? null : getValueInternal(cell);
    }

    abstract T getValueInternal(DataCell cell);

    /**
     * A method for obtaining this {@link IndexedColumn IndexedColumn's} index.
     *
     * @return this column's index
     */
    public int getIndex() {
        return m_index;
    }

//    public static final class CustomColumn extends IndexedColumn<DataCell> {
//        CustomColumn(final int index) {
//            super(index);
//        }
//
//        @Override
//        public <R> R accept(final Visitor<R> v) {
//            return v.visit(this);
//        }
//
//        @Override
//        DataCell getValueInternal(final DataCell cell) {
//            return cell;
//        }
//    }

    /**
     * An {@link IndexedColumn} that holds values of {@link DataType} {@link BooleanCell#TYPE}.
     */
    // Note that {@link BooleanColumn} does not implement {@link OrderColumn} even though {@link Boolean} implements
    // {@link Comparable}. The reason for this is that the {@link OrderPredicate} makes little sense for boolean values.
    public static final class BooleanColumn extends IndexedColumn<Boolean> {

        BooleanColumn(final int index) {
            super(index);
        }

        @Override
        Boolean getValueInternal(final DataCell cell) {
            return ((BooleanValue)cell).getBooleanValue();
        }

        @Override
        public <R> R accept(final Visitor<R> v) {
            return v.visit(this);
        }

        @Override
        public String toString() {
            return "boolean column at index " + getIndex();
        }

    }

    /**
     * Interface for {@link IndexedColumn IndexedColumns} that holds comparable values, i.e., values which over which a
     * partial ordering can be imposed.
     *
     * @param <T> the comparable type of values which this column holds
     * @noextend This interface is not intended to be extended by clients.
     * @noimplement This interface is not intended to be implemented by clients.
     */
    public interface OrderColumn<T extends Comparable<T>> extends TypedColumn<T> {
    }

    /**
     * An {@link OrderColumn} that holds values of {@link DataType} {@link IntCell#TYPE}.
     */
    public static final class IntColumn extends IndexedColumn<Integer> implements OrderColumn<Integer> {

        IntColumn(final int index) {
            super(index);
        }

        @Override
        Integer getValueInternal(final DataCell cell) {
            return ((IntValue)cell).getIntValue();
        }

        @Override
        public <R> R accept(final Visitor<R> v) {
            return v.visit(this);
        }

        @Override
        public String toString() {
            return "integer column at index " + getIndex();
        }

    }

    /**
     * An {@link OrderColumn} that holds values of {@link DataType} {@link LongCell#TYPE}.
     */
    public static final class LongColumn extends IndexedColumn<Long> implements OrderColumn<Long> {

        LongColumn(final int index) {
            super(index);
        }

        @Override
        Long getValueInternal(final DataCell cell) {
            return ((LongValue)cell).getLongValue();
        }

        @Override
        public <R> R accept(final Visitor<R> v) {
            return v.visit(this);
        }

        @Override
        public String toString() {
            return "long column at index " + getIndex();
        }

    }

    /**
     * An {@link OrderColumn} that holds values of {@link DataType} {@link DoubleCell#TYPE}.
     */
    public static final class DoubleColumn extends IndexedColumn<Double> implements OrderColumn<Double> {

        DoubleColumn(final int index) {
            super(index);
        }

        @Override
        Double getValueInternal(final DataCell cell) {
            return ((DoubleValue)cell).getDoubleValue();
        }

        @Override
        public <R> R accept(final Visitor<R> v) {
            return v.visit(this);
        }

        @Override
        public String toString() {
            return "double column at index " + getIndex();
        }

    }

    /**
     * An {@link OrderColumn} that holds values of {@link DataType} {@link StringCell#TYPE}.
     */
    public static final class StringColumn extends IndexedColumn<String> implements OrderColumn<String> {

        StringColumn(final int index) {
            super(index);
        }

        @Override
        String getValueInternal(final DataCell cell) {
            return ((StringValue)cell).getStringValue();
        }

        @Override
        public <R> R accept(final Visitor<R> v) {
            return v.visit(this);
        }

        @Override
        public String toString() {
            return "String column at index " + getIndex();
        }

    }

}
