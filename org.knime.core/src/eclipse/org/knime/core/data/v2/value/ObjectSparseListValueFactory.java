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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.data.collection.SparseListCell;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.value.ObjectListValueFactory.ObjectListReadValue;
import org.knime.core.data.v2.value.ObjectListValueFactory.ObjectListWriteValue;
import org.knime.core.data.v2.value.SparseListValueFactory.AbstractSparseIterator;
import org.knime.core.data.v2.value.SparseListValueFactory.DefaultSparseListReadValue;
import org.knime.core.data.v2.value.SparseListValueFactory.DefaultSparseListWriteValue;
import org.knime.core.data.v2.value.SparseListValueFactory.SparseListReadValue;
import org.knime.core.data.v2.value.SparseListValueFactory.SparseListWriteValue;
import org.knime.core.table.access.AccessSpec;
import org.knime.core.table.access.IntAccess.IntAccessSpec;
import org.knime.core.table.access.IntAccess.IntReadAccess;
import org.knime.core.table.access.IntAccess.IntWriteAccess;
import org.knime.core.table.access.ListAccess.ListAccessSpec;
import org.knime.core.table.access.ListAccess.ListReadAccess;
import org.knime.core.table.access.ListAccess.ListWriteAccess;
import org.knime.core.table.access.ObjectAccess.ObjectReadAccess;
import org.knime.core.table.access.ObjectAccess.ObjectWriteAccess;
import org.knime.core.table.access.StructAccess.StructAccessSpec;
import org.knime.core.table.access.StructAccess.StructReadAccess;
import org.knime.core.table.access.StructAccess.StructWriteAccess;

import com.google.common.base.Objects;

/**
 * Abstract {@link ValueFactory} implementation for {@link SparseListCell} with object elements. The
 * {@link ObjectSparseListReadValue} and {@link ObjectSparseListWriteValue} allow for direct access to the objects not
 * wrapping them into {@link DataCell} implementations.
 *
 * @param <T> the type of the elements
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public abstract class ObjectSparseListValueFactory<T> implements ValueFactory<StructReadAccess, StructWriteAccess> {

    private final ValueFactory<ObjectReadAccess<T>, ObjectWriteAccess<T>> m_innerValueFactory;

    /**
     * @param innerValueFactory the {@link ValueFactory} to create the element values of the list
     */
    public ObjectSparseListValueFactory(
        final ValueFactory<ObjectReadAccess<T>, ObjectWriteAccess<T>> innerValueFactory) {
        m_innerValueFactory = innerValueFactory;
    }

    @Override
    public AccessSpec<StructReadAccess, StructWriteAccess> getSpec() {
        final AccessSpec<ObjectReadAccess<T>, ObjectWriteAccess<T>> defaultAccessSpec = m_innerValueFactory.getSpec();
        final IntAccessSpec sizeAccessSpec = IntAccessSpec.INSTANCE;
        final ListAccessSpec<IntReadAccess, IntWriteAccess> indicesAccessSpec =
            new ListAccessSpec<>(IntAccessSpec.INSTANCE);
        final ListAccessSpec<ObjectReadAccess<T>, ObjectWriteAccess<T>> listAccessSpec =
            new ListAccessSpec<>(m_innerValueFactory.getSpec());
        return new StructAccessSpec(defaultAccessSpec, sizeAccessSpec, indicesAccessSpec, listAccessSpec);
    }

    /**
     * {@link ReadValue} equivalent to {@link SparseListCell} with elements of type T.
     *
     * @param <T> the type of the elements
     * @since 4.3
     */
    public static interface ObjectSparseListReadValue<T> extends SparseListReadValue, ObjectListReadValue<T> {
    }

    /**
     * {@link WriteValue} equivalent to {@link SparseListCell} with elements of type T.
     *
     * @param <T> the type of the elements
     * @since 4.3
     */
    public static interface ObjectSparseListWriteValue<T> extends SparseListWriteValue {

        /**
         * @param values the values to set
         * @param defaultElement the default element which should not be saved multiple times
         */
        void setValue(T[] values, T defaultElement);
    }

    /**
     * Abstract implementation of {@link ObjectListReadValue}.
     *
     * @param <E> the type of the element {@link ReadValue}
     * @param <T> the type of the elements
     */
    abstract static class AbstractObjectSparseListReadValue<E extends ReadValue, T>
        extends DefaultSparseListReadValue<E, ObjectListReadValue<T>, ObjectReadAccess<T>>
        implements ObjectSparseListReadValue<T> {

        /**
         * Create an abstract {@link ObjectSparseListReadValue}.
         *
         * @param defaultAccess the access for the default value
         * @param sizeAccess the access for the size of the sparse list
         * @param indicesAccess the access for the list of the indices of the explicitly saved elements
         * @param listAccess the access for the list of explicitly saved elements
         * @param valueFactory the value factory for the element values
         * @param listValueFactory the value factory for the storage list
         */
        protected AbstractObjectSparseListReadValue(final ObjectReadAccess<T> defaultAccess,
            final IntReadAccess sizeAccess, final ListReadAccess indicesAccess, final ListReadAccess listAccess,
            final ValueFactory<ObjectReadAccess<T>, ?> valueFactory,
            final ValueFactory<ListReadAccess, ?> listValueFactory) {
            super(defaultAccess, sizeAccess, indicesAccess, listAccess, valueFactory, listValueFactory);
        }

        /**
         * @return the default value (use #m_defaultValue)
         */
        protected abstract T getDefaultValue();

        /**
         * Create an empty array of type T. Exists because it is not possible to create generic arrays.
         *
         * @param size the size of the array
         * @return the array
         */
        protected abstract T[] createObjectArray(final int size);

        private T getValueFromStorage(final int storageIndex) {
            return m_storageList.getValue(storageIndex);
        }

        @Override
        public T getValue(final int index) {
            final OptionalInt storageIndex = storageIndexForIndex(index);
            if (storageIndex.isPresent()) {
                return getValueFromStorage(storageIndex.getAsInt());
            } else {
                return getDefaultValue();
            }
        }

        @Override
        public T[] getValueArray() {
            final T[] values = createObjectArray(size());
            final Iterator<T> iterator = valueIterator();
            for (int i = 0; i < values.length; i++) {
                values[i] = iterator.next();
            }
            return values;
        }

        @Override
        public Iterator<T> valueIterator() {
            final T defaultElement = getDefaultValue();
            return new AbstractSparseIterator<T>(size(), m_storageIndices.size(), m_storageIndices::getInt) {

                @Override
                public T next() {
                    final OptionalInt storageIndex = nextStorageIndex();
                    if (storageIndex.isPresent()) {
                        return getValueFromStorage(storageIndex.getAsInt());
                    } else {
                        return defaultElement;
                    }
                }
            };
        }
    }

    /**
     * Abstract implementation of {@link ObjectListWriteValue}.
     *
     * @param <D> the type of the {@link DataValue} of the {@link WriteValue} E
     * @param <E> the type of the element {@link WriteValue}.
     * @param <T> the type of the elements
     */
    abstract static class AbstractObjectSparseListWriteValue<D extends DataValue, E extends WriteValue<D>, T>
        extends DefaultSparseListWriteValue<D, E, ObjectListWriteValue<T>, ObjectWriteAccess<T>>
        implements ObjectSparseListWriteValue<T> {

        /**
         * Create an abstract {@link ObjectSparseListWriteValue}.
         *
         * @param defaultAccess the access for the default value
         * @param sizeAccess the access for the size of the sparse list
         * @param indicesAccess the access for the list of the indices of the explicitly saved elements
         * @param listAccess the access for the list of explicitly saved elements
         * @param valueFactory the value factory for the element values
         * @param listValueFactory the value factory for the storage list
         */
        protected AbstractObjectSparseListWriteValue(final ObjectWriteAccess<T> defaultAccess,
            final IntWriteAccess sizeAccess, final ListWriteAccess indicesAccess, final ListWriteAccess listAccess,
            final ValueFactory<?, ObjectWriteAccess<T>> valueFactory,
            final ValueFactory<?, ListWriteAccess> listValueFactory) {
            super(defaultAccess, sizeAccess, indicesAccess, listAccess, valueFactory, listValueFactory);
        }

        /**
         * Set the default value #m_defaultValue.
         *
         * @param value the value
         */
        protected abstract void setDefaultValue(T value);

        /**
         * Set the storage list #m_storageList.
         *
         * @param values the values
         */
        protected abstract void setStorageList(List<T> values);

        @Override
        public void setValue(final T[] values, final T defaultElement) {
            final List<Integer> storageIndices = new ArrayList<>();
            final List<T> storageList = new ArrayList<>();

            for (int i = 0; i < values.length; i++) {
                final T v = values[i];
                if (Objects.equal(v, defaultElement)) {
                    storageIndices.add(i);
                    storageList.add(v);
                }
            }

            setDefaultValue(defaultElement);
            m_sizeValue.setIntValue(storageList.size());
            m_storageIndices.setValue(storageIndices.stream().mapToInt(Integer::intValue).toArray());
            setStorageList(storageList);
        }
    }
}