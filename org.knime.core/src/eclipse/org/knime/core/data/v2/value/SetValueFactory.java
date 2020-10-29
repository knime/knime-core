package org.knime.core.data.v2.value;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.collection.SetDataValue;
import org.knime.core.data.v2.CollectionValueFactory;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.access.ListAccess.ListAccessSpec;
import org.knime.core.data.v2.access.ListAccess.ListReadAccess;
import org.knime.core.data.v2.access.ListAccess.ListWriteAccess;
import org.knime.core.data.v2.access.ReadAccess;
import org.knime.core.data.v2.access.WriteAccess;
import org.knime.core.data.v2.value.ListValueFactory.ListReadValue;
import org.knime.core.data.v2.value.ListValueFactory.ListWriteValue;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * {@link ValueFactory} implementation for {@link SetCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
public final class SetValueFactory implements CollectionValueFactory<ListReadAccess, ListWriteAccess> {

    private ListValueFactory m_listValueFactory;

    @Override
    public void initialize(final ValueFactory<?, ?> elementValueFactory, final DataType elementType) {
        m_listValueFactory = new ListValueFactory();
        m_listValueFactory.initialize(elementValueFactory, elementType);
    }

    @Override
    public ListAccessSpec<ReadAccess, WriteAccess> getSpec() {
        return m_listValueFactory.getSpec();
    }

    @Override
    public SetReadValue createReadValue(final ListReadAccess reader) {
        return new DefaultSetReadValue<>(reader, m_listValueFactory);
    }

    @Override
    public SetWriteValue createWriteValue(final ListWriteAccess writer) {
        return new DefaultSetWriteValue<>(writer, m_listValueFactory);
    }

    /**
     * {@link ReadValue} equivalent to {@link SetCell}.
     *
     * @since 4.3
     */
    public interface SetReadValue extends ReadValue, SetDataValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link SetCell}.
     *
     * @since 4.3
     */
    public interface SetWriteValue extends WriteValue<SetDataValue> {

        /**
         * @param values the values to set
         */
        void setValue(Collection<DataValue> values);
    }

    /**
     * Default implementation of {@link SetReadValue}. Set elements are of the type {@link DataCell}. Extend this class
     * to add access methods that do not wrap the elements in {@link DataCell}.
     *
     * @param <L> the type of the list in which the elements are saved
     * @since 4.3
     */
    static class DefaultSetReadValue<L extends ListReadValue> implements SetReadValue {

        /** The list in which the elements are saved */
        protected final L m_value;

        /**
         * Create a default {@link SetReadValue}.
         *
         * @param reader the {@link ListReadAccess} to get the values in a list
         * @param listValueFactory the value factory for the storage list
         */
        DefaultSetReadValue(final ListReadAccess reader, final ValueFactory<ListReadAccess, ?> listValueFactory) {
            @SuppressWarnings("unchecked")
            final L value = (L)listValueFactory.createReadValue(reader);
            m_value = value;
        }

        @Override
        public DataType getElementType() {
            return m_value.getElementType();
        }

        @Override
        public int size() {
            return m_value.size();
        }

        @Override
        public boolean containsBlobWrapperCells() {
            return m_value.containsBlobWrapperCells();
        }

        @Override
        public Iterator<DataCell> iterator() {
            return m_value.iterator();
        }

        @Override
        public boolean contains(final DataCell cell) {
            // NB: This is an inefficient implementation of contains
            // However, we would need a much more complex data structure
            // to get a more efficient contains implementation
            for (final DataCell c : this) {
                if (Objects.equal(c, cell)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public DataCell getDataCell() {
            return CollectionCellFactory.createSetCell(ImmutableList.copyOf(iterator()));
        }
    }

    /**
     * Default implementation of {@link SetWriteValue}. Set elements are of the type {@link DataCell}. Extend this class
     * to add setter methods that do not wrap the elements in {@link DataCell}.
     *
     * @param <L> the type of the list in which the elements are saved
     * @param <W> the type of the {@link WriteAccess} for the set elements
     * @since 4.3
     */
    static class DefaultSetWriteValue<L extends ListWriteValue> implements SetWriteValue {

        /** The list in which the elements are saved */
        protected final L m_value;

        /**
         * Create a default {@link SetWriteValue}.
         *
         * @param writer the {@link ListWriteAccess} to set the values in the list
         * @param listValueFactory the value factory for the storage list
         */
        DefaultSetWriteValue(final ListWriteAccess writer, final ValueFactory<?, ListWriteAccess> listValueFactory) {
            @SuppressWarnings("unchecked")
            final L value = (L)listValueFactory.createWriteValue(writer);
            m_value = value;
        }

        @Override
        public void setValue(final SetDataValue value) {
            m_value.setValue(ImmutableList.copyOf(value.iterator()));
        }

        @Override
        public void setValue(final Collection<DataValue> values) {
            final Set<DataValue> set;
            if (values instanceof Set) {
                set = (Set<DataValue>)values;
            } else {
                set = new HashSet<>(values);
            }
            m_value.setValue(Lists.newArrayList(set));
        }
    }
}