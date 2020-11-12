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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.OptionalInt;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.MissingValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.SparseListCell;
import org.knime.core.data.collection.SparseListDataValue;
import org.knime.core.data.v2.CollectionValueFactory;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.access.AccessSpec;
import org.knime.core.data.v2.access.IntAccess.IntAccessSpec;
import org.knime.core.data.v2.access.IntAccess.IntReadAccess;
import org.knime.core.data.v2.access.IntAccess.IntWriteAccess;
import org.knime.core.data.v2.access.ListAccess.ListAccessSpec;
import org.knime.core.data.v2.access.ListAccess.ListReadAccess;
import org.knime.core.data.v2.access.ListAccess.ListWriteAccess;
import org.knime.core.data.v2.access.ReadAccess;
import org.knime.core.data.v2.access.StructAccess.StructAccessSpec;
import org.knime.core.data.v2.access.StructAccess.StructReadAccess;
import org.knime.core.data.v2.access.StructAccess.StructWriteAccess;
import org.knime.core.data.v2.access.WriteAccess;
import org.knime.core.data.v2.value.IntListValueFactory.IntListReadValue;
import org.knime.core.data.v2.value.IntListValueFactory.IntListWriteValue;
import org.knime.core.data.v2.value.ListValueFactory.ListReadValue;
import org.knime.core.data.v2.value.ListValueFactory.ListWriteValue;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

/**
 * {@link ValueFactory} implementation for {@link SparseListCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
public final class SparseListValueFactory implements CollectionValueFactory<StructReadAccess, StructWriteAccess> {

    private ValueFactory<ReadAccess, WriteAccess> m_inner;

    private ListValueFactory m_listValueFactory;

    @Override
    public void initialize(final ValueFactory<?, ?> elementValueFactory, final DataType elementType) {
        @SuppressWarnings("unchecked")
        final ValueFactory<ReadAccess, WriteAccess> inner = (ValueFactory<ReadAccess, WriteAccess>)elementValueFactory;
        m_inner = inner;
        m_listValueFactory = new ListValueFactory();
        m_listValueFactory.initialize(elementValueFactory, elementType);
    }

    @Override
    public StructAccessSpec getSpec() {
        final AccessSpec<ReadAccess, WriteAccess> defaultAccessSpec = m_inner.getSpec();
        final IntAccessSpec sizeAccessSpec = IntAccessSpec.INSTANCE;
        final ListAccessSpec<IntReadAccess, IntWriteAccess> indicesAccessSpec =
            new ListAccessSpec<>(IntValueFactory.INSTANCE);
        final ListAccessSpec<ReadAccess, WriteAccess> listAccessSpec = new ListAccessSpec<>(m_inner);
        return new StructAccessSpec(defaultAccessSpec, sizeAccessSpec, indicesAccessSpec, listAccessSpec);
    }

    @Override
    public SparseListReadValue createReadValue(final StructReadAccess reader) {
        return new DefaultSparseListReadValue<>(reader.getInnerReadAccessAt(0), reader.getInnerReadAccessAt(1),
            reader.getInnerReadAccessAt(2), reader.getInnerReadAccessAt(3), m_inner, m_listValueFactory);
    }

    @Override
    public SparseListWriteValue createWriteValue(final StructWriteAccess writer) {
        return new DefaultSparseListWriteValue<>(writer.getWriteAccessAt(0), writer.getWriteAccessAt(1),
            writer.getWriteAccessAt(2), writer.getWriteAccessAt(3), m_inner, m_listValueFactory);
    }

    /**
     * {@link ReadValue} equivalent to {@link SparseListCell}.
     *
     * @since 4.3
     */
    public interface SparseListReadValue extends ReadValue, SparseListDataValue {

        /**
         * @param index the index in the list
         * @return if the value at this index is missing
         */
        boolean isMissing(int index);
    }

    /**
     * {@link WriteValue} equivalent to {@link SparseListCell}.
     *
     * @since 4.3
     */
    public interface SparseListWriteValue extends WriteValue<SparseListDataValue> {

        /**
         * @param values the values to set
         * @param defaultElement the default element which should not be saved multiple times
         */
        void setValue(List<DataValue> values, DataValue defaultElement);
    }

    /**
     * Default implementation of {@link SparseListReadValue}. List elements are of the type {@link DataCell}. Extend
     * this class to add access methods that do not wrap the elements in {@link DataCell}.
     *
     * @param <E> the type of the element {@link ReadValue}
     * @param <L> the type of the list in which the elements are saved
     * @param <R> the type of the {@link ReadAccess} for the list elements
     * @since 4.3
     */
    static class DefaultSparseListReadValue<E extends ReadValue, L extends ListReadValue, R extends ReadAccess>
        implements SparseListReadValue {

        private final R m_defaultAccess;

        /** The value for unset items */
        protected final E m_defaultValue;

        /** The size of the list */
        private final IntReadAccess m_sizeValue;

        /** The indices of the explicitly saved elements in {@link #m_storageList} */
        protected final IntListReadValue m_storageIndices;

        /** The explicitly saved elements which are not the {@link #m_defaultValue} */
        protected final L m_storageList;

        /**
         * Create a default {@link SparseListReadValue}.
         *
         * @param defaultAccess the access for the default value
         * @param sizeAccess the access for the size of the sparse list
         * @param indicesAccess the access for the list of the indices of the explicitly saved elements
         * @param listAccess the access for the list of explicitly saved elements
         * @param valueFactory the value factory for the element values
         * @param listValueFactory the value factory for the storage list
         */
        DefaultSparseListReadValue(final R defaultAccess, final IntReadAccess sizeAccess,
            final ListReadAccess indicesAccess, final ListReadAccess listAccess, final ValueFactory<R, ?> valueFactory,
            final ValueFactory<ListReadAccess, ?> listValueFactory) {

            m_defaultAccess = defaultAccess;
            @SuppressWarnings("unchecked")
            final E defaultValue = (E)valueFactory.createReadValue(defaultAccess);
            m_defaultValue = defaultValue;
            m_sizeValue = sizeAccess;

            m_storageIndices = IntListValueFactory.INSTANCE.createReadValue(indicesAccess);

            @SuppressWarnings("unchecked")
            final L storageList = (L)listValueFactory.createReadValue(listAccess);
            m_storageList = storageList;
        }

        /**
         * @param index the index in the sparse list
         * @return the index in the storage list the value at this location is not the default value
         */
        protected OptionalInt storageIndexForIndex(final int index) {
            // NB: We find the index using binary search
            int left = 0;
            int right = m_storageIndices.size() - 1;

            while (left < right) {
                final int storageIndex = (left + right) / 2;
                final int currentIndex = m_storageIndices.getInt(storageIndex);
                if (currentIndex < index) {
                    left = storageIndex + 1;
                } else if (currentIndex > index) {
                    right = storageIndex - 1;
                } else {
                    return OptionalInt.of(storageIndex);
                }
            }
            return OptionalInt.empty();
        }

        @Override
        public DataCell get(final int index) {
            final OptionalInt storageIndex = storageIndexForIndex(index);
            if (storageIndex.isPresent()) {
                return m_storageList.get(storageIndex.getAsInt());
            } else {
                return m_defaultValue.getDataCell();
            }
        }

        @Override
        public DataCell getDefaultElement() {
            if (!m_defaultAccess.isMissing()) {
                return m_defaultValue.getDataCell();
            } else {
                return DataType.getMissingCell();
            }
        }

        @Override
        public DataType getElementType() {
            return m_storageList.getElementType();
        }

        @Override
        public int size() {
            return m_sizeValue.getIntValue();
        }

        @Override
        public boolean containsBlobWrapperCells() {
            return false;
        }

        @Override
        public Iterator<DataCell> iterator() {
            final DataCell defaultElement = getDefaultElement();
            return new AbstractSparseIterator<DataCell>(size(), m_storageIndices.size(), m_storageIndices::getInt) {

                @Override
                public DataCell next() {
                    final OptionalInt storageIndex = nextStorageIndex();
                    if (storageIndex.isPresent()) {
                        return m_storageList.get(storageIndex.getAsInt());
                    } else {
                        return defaultElement;
                    }
                }
            };
        }

        @Override
        public int[] getAllIndices() {
            return m_storageIndices.getIntArray();
        }

        @Override
        public boolean isMissing(final int index) {
            final OptionalInt storageIndex = storageIndexForIndex(index);
            if (storageIndex.isPresent()) {
                return m_storageList.isMissing(storageIndex.getAsInt());
            } else {
                return m_defaultAccess.isMissing();
            }
        }

        @Override
        public DataCell getDataCell() {
            // TODO(benjamin) There is no API to create a sparse list from an already sparse collection!
            return CollectionCellFactory.createSparseListCell(ImmutableList.copyOf(iterator()), getDefaultElement());
        }
    }

    /**
     * Default implementation of {@link SparseListWriteValue}. List elements are of the type {@link DataCell}. Extend
     * this class to add setter methods that do not wrap the elements in {@link DataCell}.
     *
     * @param <D> the type of the {@link DataValue} of the {@link WriteValue} T
     * @param <E> the type of the element {@link ReadValue}.
     * @param <L> the type of the list in which the elements are saved
     * @param <W> the type of the {@link WriteAccess} for the list elements
     * @since 4.3
     */
    static class DefaultSparseListWriteValue<D extends DataValue, E extends WriteValue<D>, L extends ListWriteValue, W extends WriteAccess>
        implements SparseListWriteValue {

        private final E m_defaultValue;

        private final IntWriteAccess m_sizeValue;

        private final IntListWriteValue m_storageIndices;

        private final L m_storageList;

        /**
         * Create a default {@link SparseListWriteValue}.
         *
         * @param defaultAccess the access for the default value
         * @param sizeAccess the access for the size of the sparse list
         * @param indicesAccess the access for the list of the indices of the explicitly saved elements
         * @param listAccess the access for the list of explicitly saved elements
         * @param valueFactory the value factory for the element values
         * @param listValueFactory the value factory for the storage list
         */
        DefaultSparseListWriteValue(final W defaultAccess, final IntWriteAccess sizeAccess,
            final ListWriteAccess indicesAccess, final ListWriteAccess listAccess,
            final ValueFactory<?, W> valueFactory, final ValueFactory<?, ListWriteAccess> listValueFactory) {

            @SuppressWarnings("unchecked")
            final E defaultValue = (E)valueFactory.createWriteValue(defaultAccess);
            m_defaultValue = defaultValue;
            m_sizeValue = sizeAccess;

            m_storageIndices = IntListValueFactory.INSTANCE.createWriteValue(indicesAccess);

            @SuppressWarnings("unchecked")
            final L createWriteValue = (L)listValueFactory.createWriteValue(listAccess);
            m_storageList = createWriteValue;
        }

        @Override
        public void setValue(final SparseListDataValue value) {
            final int[] indices = value.getAllIndices();
            final List<DataValue> storageList =
                Arrays.stream(indices).mapToObj(value::get).collect(Collectors.toList());
            setValue(value.getDefaultElement(), value.size(), indices, storageList);
        }

        @Override
        public void setValue(final List<DataValue> values, final DataValue defaultElement) {
            final int size = values.size();
            final List<Integer> storageIndices = new ArrayList<>();
            final List<DataValue> storageList = new ArrayList<>();

            for (int i = 0; i < size; i++) {
                final DataValue v = values.get(i);
                if (!Objects.equal(defaultElement, v)) {
                    storageIndices.add(i);
                    storageList.add(v);
                }
            }
            setValue(defaultElement, size, storageIndices.stream().mapToInt(Integer::intValue).toArray(), storageList);

        }

        private void setValue(final DataValue defaultValue, final int size, final int[] storageIndices,
            final List<DataValue> storageList) {
            // Set the default value
            if (!(defaultValue instanceof MissingValue)) {
                @SuppressWarnings("unchecked")
                final D d = (D)defaultValue;
                m_defaultValue.setValue(d);
            }

            // Set the size
            m_sizeValue.setIntValue(size);

            // Set the indices
            m_storageIndices.setValue(storageIndices);

            // Set the implicit values
            m_storageList.setValue(storageList);
        }
    }

    /**
     * An abstract {@link Iterator} which iterates over sparse list consisting of a storage list (for non default
     * values), a list of indices for these values and a default element. Call {@link #nextStorageIndex} to get the
     * index of the next element in the storage list (or empty if the next element is the default value)
     *
     * @param <E> the type of elements returned by this iterator
     */
    protected abstract static class AbstractSparseIterator<E> implements Iterator<E> {

        /** The size of the whole list */
        private final int m_size;

        /** The number of explicity saved elements */
        private final int m_storageSize;

        private final IntUnaryOperator m_indexFromStorage;

        private int m_nextIndex;

        private int m_nextStorageIndex;

        private int m_nextNonDefaultIndex;

        /**
         * Create an abstract {@link Iterator} which iterates over sparse list consisting of a storage list (for non
         * default values), a list of indices for these values and a default element.
         *
         * @param size the size of the sparse list
         * @param storageSize the number of explicitly save elements
         * @param indexFromStorage a operator which maps a storage index to the index in the sparse list
         */
        protected AbstractSparseIterator(final int size, final int storageSize,
            final IntUnaryOperator indexFromStorage) {
            m_size = size;
            m_storageSize = storageSize;
            m_indexFromStorage = indexFromStorage;

            m_nextIndex = 0;
            m_nextStorageIndex = 0;
            m_nextNonDefaultIndex = indexFromStorage.applyAsInt(m_nextStorageIndex);
        }

        @Override
        public boolean hasNext() {
            return m_nextIndex < m_size;
        }

        /**
         * @return the next index in the storage list or empty if the default value is the next value.
         */
        protected OptionalInt nextStorageIndex() {
            if (!hasNext()) {
                throw new NoSuchElementException(
                    "No element with index " + m_nextIndex + " in list of size " + m_size + ".");
            }
            if (m_nextIndex == m_nextNonDefaultIndex) {
                // Get the element from the storage
                final int index = m_nextStorageIndex;
                // Point to next element in the storage
                m_nextStorageIndex++;
                if (m_nextStorageIndex < m_storageSize) {
                    m_nextNonDefaultIndex = m_indexFromStorage.applyAsInt(m_nextStorageIndex);
                }
                return OptionalInt.of(index);
            }
            // Not specifically set in the storage -> Default element
            return OptionalInt.empty();
        }
    }
}