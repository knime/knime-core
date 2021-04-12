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
 *   Nov 12, 2020 (Benjamin Wilhelm): created
 */
package org.knime.core.data.v2.value;

import java.util.Arrays;
import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.value.ListValueFactory.DefaultListReadValue;
import org.knime.core.data.v2.value.ListValueFactory.ListReadValue;
import org.knime.core.data.v2.value.ListValueFactory.ListWriteValue;
import org.knime.core.table.access.ListAccess.ListAccessSpec;
import org.knime.core.table.access.ListAccess.ListReadAccess;
import org.knime.core.table.access.ListAccess.ListWriteAccess;
import org.knime.core.table.access.ObjectAccess.ObjectReadAccess;
import org.knime.core.table.access.ObjectAccess.ObjectWriteAccess;

/**
 * Abstract {@link ValueFactory} implementation for {@link ListCell} with object elements. The
 * {@link ObjectListReadValue} and {@link ObjectListWriteValue} allow for direct access to the objects not wrapping them
 * into {@link DataCell} implementations.
 *
 * @param <T> the type of the elements
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public abstract class ObjectListValueFactory<T> implements ValueFactory<ListReadAccess, ListWriteAccess> {

    private final ValueFactory<ObjectReadAccess<T>, ObjectWriteAccess<T>> m_innerValueFactory;

    /**
     * @param innerValueFactory the {@link ValueFactory} to create the element values of the list
     */
    ObjectListValueFactory(final ValueFactory<ObjectReadAccess<T>, ObjectWriteAccess<T>> innerValueFactory) {
        m_innerValueFactory = innerValueFactory;
    }

    @Override
    public ListAccessSpec<ObjectReadAccess<T>, ObjectWriteAccess<T>> getSpec() {
        return new ListAccessSpec<>(m_innerValueFactory.getSpec());
    }

    /**
     * {@link ReadValue} equivalent to {@link ListCell} with elements of type T.
     *
     * @param <T> the type of the elements
     * @since 4.3
     */
    public static interface ObjectListReadValue<T> extends ListReadValue {

        /**
         * @param index the index in the list
         * @return the object value at the index
         * @throws IllegalStateException if the value at this index is missing
         */
        T getValue(int index);

        /**
         * @return the list as a object array
         * @throws IllegalStateException if the value at one index is missing
         */
        T[] getValueArray();

        /**
         * @return an iterator over the object list
         * @throws IllegalStateException if the value at one index is missing
         */
        Iterator<T> valueIterator();
    }

    /**
     * {@link WriteValue} equivalent to {@link ListCell} with elements of type T.
     *
     * @param <T> the type of the elements
     * @since 4.3
     */
    public static interface ObjectListWriteValue<T> extends ListWriteValue {

        /**
         * Set the value.
         *
         * @param values a array of object values
         */
        void setValue(T[] values);
    }

    /**
     * Abstract implementation of {@link ObjectListReadValue}.
     *
     * @param <T> the type of the elements
     */
    abstract static class AbstractObjectListReadValue<T> extends DefaultListReadValue
        implements ObjectListReadValue<T> {

        /**
         * @param reader the access to the list
         * @param type the {@link DataType} of the elements
         */
        protected AbstractObjectListReadValue(final ListReadAccess reader, final ValueFactory<?, ?> inner,
            final DataType type) {
            super(reader, inner, type);
        }

        /**
         * Create an empty array of type T. Exists because it is not possible to create generic arrays.
         *
         * @param size the size of the array
         * @return the array
         */
        protected abstract T[] createObjectArray(final int size);

        @Override
        public T[] getValueArray() {
            final T[] result = createObjectArray(size());
            for (int i = 0; i < result.length; i++) {
                result[i] = getValue(i);
            }
            return result;
        }

        @Override
        public Iterator<T> valueIterator() {
            return Arrays.stream(getValueArray()).iterator();
        }
    }
}