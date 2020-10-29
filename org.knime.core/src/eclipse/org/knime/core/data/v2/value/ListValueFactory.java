package org.knime.core.data.v2.value;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.MissingValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.collection.ListDataValue;
import org.knime.core.data.v2.CollectionValueFactory;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.access.ListAccess.ListAccessSpec;
import org.knime.core.data.v2.access.ListAccess.ListReadAccess;
import org.knime.core.data.v2.access.ListAccess.ListWriteAccess;
import org.knime.core.data.v2.access.ReadAccess;
import org.knime.core.data.v2.access.WriteAccess;

import com.google.common.collect.ImmutableList;

/**
 * {@link ValueFactory} implementation for {@link ListCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
public final class ListValueFactory implements CollectionValueFactory<ListReadAccess, ListWriteAccess> {

    private ValueFactory<ReadAccess, WriteAccess> m_inner;

    private DataType m_elementType;

    @Override
    public void initialize(final ValueFactory<?, ?> elementValueFactory, final DataType elementType) {
        @SuppressWarnings("unchecked")
        final ValueFactory<ReadAccess, WriteAccess> inner = (ValueFactory<ReadAccess, WriteAccess>)elementValueFactory;
        m_inner = inner;
        m_elementType = elementType;
    }

    @Override
    public ListAccessSpec<ReadAccess, WriteAccess> getSpec() {
        return new ListAccessSpec<>(m_inner);
    }

    @Override
    public ListReadValue createReadValue(final ListReadAccess reader) {
        return new DefaultListReadValue(reader, m_elementType);
    }

    @Override
    public ListWriteValue createWriteValue(final ListWriteAccess writer) {
        return new DefaultListWriteValue(writer);
    }

    /**
     * {@link ReadValue} equivalent to {@link ListCell}.
     *
     * @since 4.3
     */
    public static interface ListReadValue extends ReadValue, ListDataValue {

        /**
         * @param index the index in the list
         * @return if the value at this index is missing
         */
        boolean isMissing(int index);
    }

    /**
     * {@link WriteValue} equivalent to {@link ListCell}.
     *
     * @since 4.3
     */
    public static interface ListWriteValue extends WriteValue<ListDataValue> {

        /**
         * @param values the values to set
         */
        void setValue(List<DataValue> values);
    }

    /**
     * Default implementation of {@link ListReadValue}. List elements are of the type {@link DataCell}. Extend this
     * class to add access methods that do not wrap the elements in {@link DataCell}.
     *
     * @since 4.3
     */
    static class DefaultListReadValue implements ListReadValue {

        /** The access to the list. */
        protected final ListReadAccess m_reader;

        private final DataType m_elementType;

        /**
         * Create a default {@link ListReadValue}.
         *
         * @param reader {@link ListReadAccess} to get the values
         * @param elementType the type of the elements
         */
        DefaultListReadValue(final ListReadAccess reader, final DataType elementType) {
            m_elementType = elementType;
            m_reader = reader;
        }

        @Override
        public DataType getElementType() {
            return m_elementType;
        }

        @Override
        public int size() {
            return m_reader.size();
        }

        @Override
        public boolean containsBlobWrapperCells() {
            return false;
        }

        @Override
        public Iterator<DataCell> iterator() {
            final int size = size();
            return new Iterator<DataCell>() {
                private int m_nextIndex = 0;

                @Override
                public boolean hasNext() {
                    return m_nextIndex < size;
                }

                @Override
                public DataCell next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException(
                            "No element with index " + m_nextIndex + " in list of size " + size + ".");
                    }
                    final DataCell cell = get(m_nextIndex);
                    m_nextIndex++;
                    return cell;
                }
            };
        }

        @Override
        public DataCell get(final int index) {
            if (!isMissing(index)) {
                return m_reader.getReadValue(index).getDataCell();
            } else {
                return DataType.getMissingCell();
            }
        }

        @Override
        public boolean isMissing(final int index) {
            return m_reader.isMissing(index);
        }

        @Override
        public DataCell getDataCell() {
            return CollectionCellFactory.createListCell(ImmutableList.copyOf(iterator()));
        }
    }

    /**
     * Default implementation of {@link ListWriteValue}. List elements are of the type {@link DataCell}. Extend this
     * class to add setter methods that do not wrap the elements in {@link DataCell}.
     *
     * @since 4.3
     */
    static class DefaultListWriteValue implements ListWriteValue {

        private final ListWriteAccess m_writer;

        /**
         * Create a default {@link ListWriteValue}.
         *
         * @param writer {@link ListWriteAccess} to access the values
         */
        DefaultListWriteValue(final ListWriteAccess writer) {
            m_writer = writer;
        }

        @Override
        public void setValue(final ListDataValue value) {
            setValue(value.size(), (i, v) -> {
                final DataCell cell = value.get(i);
                if (!cell.isMissing()) {
                    v.setValue(cell);
                }
            });
        }

        @Override
        public void setValue(final List<DataValue> values) {
            setValue(values.size(), (i, v) -> {
                final DataValue value = values.get(i);
                if (!(value instanceof MissingValue)) {
                    v.setValue(value);
                }
            });
        }

        /**
         * Set the list value by accessing the value directly in the setter.
         *
         * @param <D> the type of the {@link DataValue} written by the {@link WriteValue} of type W
         * @param <W> the type of the {@link WriteValue}
         * @param size the size of the list
         * @param setter a {@link BiConsumer} which gets the index of the value and the {@link WriteValue} of type W and
         *            is supposed to set W to the expected value.
         */
        protected <D extends DataValue, W extends WriteValue<D>> void setValue(final int size,
            final BiConsumer<Integer, W> setter) {
            m_writer.create(size);
            for (int i = 0; i < size; i++) {
                setter.accept(i, m_writer.getWriteValue(i));
            }
        }
    }
}