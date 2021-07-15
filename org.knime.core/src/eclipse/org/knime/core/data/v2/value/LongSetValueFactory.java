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
import java.util.PrimitiveIterator;
import java.util.PrimitiveIterator.OfLong;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.data.collection.SetCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.value.LongListValueFactory.LongListReadValue;
import org.knime.core.data.v2.value.LongListValueFactory.LongListWriteValue;
import org.knime.core.data.v2.value.SetValueFactory.DefaultSetReadValue;
import org.knime.core.data.v2.value.SetValueFactory.DefaultSetWriteValue;
import org.knime.core.data.v2.value.SetValueFactory.SetReadValue;
import org.knime.core.data.v2.value.SetValueFactory.SetWriteValue;
import org.knime.core.table.access.ListAccess.ListReadAccess;
import org.knime.core.table.access.ListAccess.ListWriteAccess;
import org.knime.core.table.schema.ListDataSpec;
import org.knime.core.table.schema.LongDataSpec;
import org.knime.core.table.schema.traits.DataTraits;
import org.knime.core.table.schema.traits.DefaultDataTraits;

/**
 * {@link ValueFactory} implementation for {@link SetCell} with elements of type {@link LongCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public final class LongSetValueFactory implements ValueFactory<ListReadAccess, ListWriteAccess> {

    /** A stateless instance of {@link LongSetValueFactory} */
    public static final LongSetValueFactory INSTANCE = new LongSetValueFactory();

    @Override
    public ListDataSpec getSpec() {
        return new ListDataSpec(LongDataSpec.INSTANCE);
    }

    @Override
    public LongSetReadValue createReadValue(final ListReadAccess reader) {
        return new DefaultLongSetReadValue(reader);
    }

    @Override
    public LongSetWriteValue createWriteValue(final ListWriteAccess writer) {
        return new DefaultLongSetWriteValue(writer);
    }

    @Override
    public DataTraits getTraits() {
        return DefaultDataTraits.EMPTY;
    }

    /**
     * {@link ReadValue} equivalent to {@link SetCell} with {@link LongCell} elements.
     *
     * @since 4.3
     */
    public interface LongSetReadValue extends SetReadValue {

        /**
         * @param value a long value
         * @return true if the set contains the value
         */
        boolean contains(long value);

        /**
         * @return a {@link Set} containing the {@link Long} values
         */
        Set<Long> getLongSet();

        /**
         * @return an iterator of the long set
         * @throws IllegalStateException if the set contains a missing value
         */
        PrimitiveIterator.OfLong longIterator();
    }

    /**
     * {@link WriteValue} equivalent to {@link SetCell} with {@link LongCell} elements.
     *
     * @since 4.3
     */
    public interface LongSetWriteValue extends SetWriteValue {

        /**
         * Set the value.
         *
         * @param values a collection of long values
         */
        void setLongColletionValue(Collection<Long> values);
    }

    private static final class DefaultLongSetReadValue extends DefaultSetReadValue<LongListReadValue>
        implements LongSetReadValue {

        protected DefaultLongSetReadValue(final ListReadAccess reader) {
            super(reader, LongListValueFactory.INSTANCE);
        }

        @Override
        public boolean contains(final long value) {
            // TODO(benjamin) we can save the values sorted and do binary search
            final long[] values = m_value.getLongArray();
            for (int i = 0; i < values.length; i++) {
                if (value == values[i]) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Set<Long> getLongSet() {
            return Arrays.stream(m_value.getLongArray()).boxed().collect(Collectors.toSet());
        }

        @Override
        public OfLong longIterator() {
            return m_value.longIterator();
        }
    }

    private static final class DefaultLongSetWriteValue extends DefaultSetWriteValue<LongListWriteValue>
        implements LongSetWriteValue {

        protected DefaultLongSetWriteValue(final ListWriteAccess writer) {
            super(writer, LongListValueFactory.INSTANCE);
        }

        @Override
        public void setLongColletionValue(final Collection<Long> values) {
            m_value.setValue(values.stream().mapToLong(Long::longValue).distinct().toArray());
        }
    }
}