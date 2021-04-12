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
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.value.ObjectListValueFactory.ObjectListReadValue;
import org.knime.core.data.v2.value.ObjectListValueFactory.ObjectListWriteValue;
import org.knime.core.data.v2.value.SetValueFactory.DefaultSetReadValue;
import org.knime.core.data.v2.value.SetValueFactory.DefaultSetWriteValue;
import org.knime.core.data.v2.value.SetValueFactory.SetReadValue;
import org.knime.core.data.v2.value.SetValueFactory.SetWriteValue;
import org.knime.core.table.access.AccessSpec;
import org.knime.core.table.access.ListAccess.ListReadAccess;
import org.knime.core.table.access.ListAccess.ListWriteAccess;

import com.google.common.base.Objects;

/**
 * Abstract {@link ValueFactory} implementation for {@link SetCell} with object elements. The {@link ObjectSetReadValue}
 * and {@link ObjectSetWriteValue} allow for direct access to the objects not wrapping them into {@link DataCell}
 * implementations.
 *
 * @param <T> the type of the elements
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public abstract class ObjectSetValueFactory<T> implements ValueFactory<ListReadAccess, ListWriteAccess> {

    private final ValueFactory<ListReadAccess, ListWriteAccess> m_listValueFactory;

    /**
     * @param listValueFactory a {@link ValueFactory} to create a list in which the Set is saved
     */
    ObjectSetValueFactory(final ValueFactory<ListReadAccess, ListWriteAccess> listValueFactory) {
        m_listValueFactory = listValueFactory;
    }

    @Override
    public ObjectSetReadValue<T> createReadValue(final ListReadAccess access) {
        return new DefaultObjectSetReadValue<>(access, m_listValueFactory);
    }

    @Override
    public ObjectSetWriteValue<T> createWriteValue(final ListWriteAccess access) {
        return new DefaultObjectSetWriteValue<>(access, m_listValueFactory);
    }

    @Override
    public AccessSpec<ListReadAccess, ListWriteAccess> getSpec() {
        return m_listValueFactory.getSpec();
    }

    /**
     * {@link ReadValue} equivalent to {@link SetCell} with elements of type T.
     *
     * @param <T> the type of the elements
     * @since 4.3
     */
    public interface ObjectSetReadValue<T> extends SetReadValue {

        /**
         * @param value an object value
         * @return true if the set contains the value
         */
        boolean contains(T value);

        /**
         * @return a {@link Set} containing the object values
         */
        Set<T> getValueSet();

        /**
         * @return an iterator of the object set
         * @throws IllegalStateException if the set contains a missing value
         */
        Iterator<T> valueIterator();
    }

    /**
     * {@link WriteValue} equivalent to {@link SetCell} with elements of type T.
     *
     * @param <T> the type of the elements
     * @since 4.3
     */
    public interface ObjectSetWriteValue<T> extends SetWriteValue {

        /**
         * Set the value.
         *
         * @param values a collection of String values
         */
        void setColletionValue(Collection<T> values);
    }

    private static final class DefaultObjectSetReadValue<T> extends DefaultSetReadValue<ObjectListReadValue<T>>
        implements ObjectSetReadValue<T> {

        protected DefaultObjectSetReadValue(final ListReadAccess reader,
            final ValueFactory<ListReadAccess, ?> listValueFactory) {
            super(reader, listValueFactory);
        }

        @Override
        public boolean contains(final T value) {
            final T[] values = m_value.getValueArray();
            for (int i = 0; i < values.length; i++) {
                if (Objects.equal(value, values[i])) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Set<T> getValueSet() {
            return Arrays.stream(m_value.getValueArray()).collect(Collectors.toSet());
        }

        @Override
        public Iterator<T> valueIterator() {
            return m_value.valueIterator();
        }
    }

    private static final class DefaultObjectSetWriteValue<T> extends DefaultSetWriteValue<ObjectListWriteValue<T>>
        implements ObjectSetWriteValue<T> {

        protected DefaultObjectSetWriteValue(final ListWriteAccess writer,
            final ValueFactory<?, ListWriteAccess> listValueFactory) {
            super(writer, listValueFactory);
        }

        @Override
        public void setColletionValue(final Collection<T> values) {
            @SuppressWarnings("unchecked")
            final T[] array = (T[])values.stream().distinct().toArray();
            m_value.setValue(array);
        }
    }
}