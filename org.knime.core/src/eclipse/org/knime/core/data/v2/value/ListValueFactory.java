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
import org.knime.core.data.v2.value.ValueInterfaces.ListReadValue;
import org.knime.core.data.v2.value.ValueInterfaces.ListWriteValue;
import org.knime.core.table.access.ListAccess.ListReadAccess;
import org.knime.core.table.access.ListAccess.ListWriteAccess;
import org.knime.core.table.access.ReadAccess;
import org.knime.core.table.access.WriteAccess;
import org.knime.core.table.schema.ListDataSpec;
import org.knime.core.table.schema.traits.DataTraits;
import org.knime.core.table.schema.traits.DefaultListDataTraits;

import com.google.common.collect.ImmutableList;

/**
 * {@link ValueFactory} implementation for {@link ListCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 * @noreference This class is not intended to be referenced by clients.
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
    public ListDataSpec getSpec() {
        return new ListDataSpec(m_inner.getSpec());
    }

    @Override
    public ListReadValue createReadValue(final ListReadAccess reader) {
        return new DefaultListReadValue(reader, m_inner, m_elementType);
    }

    @Override
    public ListWriteValue createWriteValue(final ListWriteAccess writer) {
        return new DefaultListWriteValue(writer, m_inner);
    }

    @Override
    public DataTraits getTraits() {
        return new DefaultListDataTraits(m_inner.getTraits());
    }

    @Override
    public ValueFactory<?, ?> getElementValueFactory() {
        return m_inner;
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

        private final ValueFactory<?, ?> m_inner;

        private final DataType m_elementType;

        /**
         * Create a default {@link ListReadValue}.
         *
         * @param reader {@link ListReadAccess} to get the values
         * @param elementType the type of the elements
         */
        DefaultListReadValue(final ListReadAccess reader, final ValueFactory<?, ?> inner,
            final DataType elementType) {
            m_reader = reader;
            m_inner = inner;
            m_elementType = elementType;
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
                return m_inner.createReadValue(m_reader.getAccess(index)).getDataCell();
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

        private final ValueFactory<?, ?> m_inner;

        /**
         * Create a default {@link ListWriteValue}.
         *
         * @param writer {@link ListWriteAccess} to access the values
         */
        DefaultListWriteValue(final ListWriteAccess writer, final ValueFactory<?, ?> inner) {
            m_writer = writer;
            m_inner = inner;
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
            for (int i = 0; i < size; i++) { // NOSONAR
                @SuppressWarnings("unchecked")
                W value = (W)m_inner.createWriteValue(m_writer.getWriteAccess(i));
                setter.accept(i, value);
            }
        }
    }

}