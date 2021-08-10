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
import java.util.PrimitiveIterator.OfInt;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.data.collection.SetCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.value.IntListValueFactory.IntListReadValue;
import org.knime.core.data.v2.value.IntListValueFactory.IntListWriteValue;
import org.knime.core.data.v2.value.SetValueFactory.DefaultSetReadValue;
import org.knime.core.data.v2.value.SetValueFactory.DefaultSetWriteValue;
import org.knime.core.data.v2.value.SetValueFactory.SetReadValue;
import org.knime.core.data.v2.value.SetValueFactory.SetWriteValue;
import org.knime.core.table.access.ListAccess.ListReadAccess;
import org.knime.core.table.access.ListAccess.ListWriteAccess;
import org.knime.core.table.schema.IntDataSpec;
import org.knime.core.table.schema.ListDataSpec;
import org.knime.core.table.schema.traits.DataTraits;
import org.knime.core.table.schema.traits.DefaultDataTraits;
import org.knime.core.table.schema.traits.DefaultListDataTraits;

/**
 * {@link ValueFactory} implementation for {@link SetCell} with elements of type {@link IntCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public final class IntSetValueFactory implements ValueFactory<ListReadAccess, ListWriteAccess> {

    /** A stateless instance of {@link IntSetValueFactory} */
    public static final IntSetValueFactory INSTANCE = new IntSetValueFactory();

    @Override
    public ListDataSpec getSpec() {
        return new ListDataSpec(IntDataSpec.INSTANCE);
    }

    @Override
    public IntSetReadValue createReadValue(final ListReadAccess reader) {
        return new DefaultIntSetReadValue(reader);
    }

    @Override
    public IntSetWriteValue createWriteValue(final ListWriteAccess writer) {
        return new DefaultIntSetWriteValue(writer);
    }

    @Override
    public DataTraits getTraits() {
        return new DefaultListDataTraits(DefaultDataTraits.EMPTY);
    }

    /**
     * {@link ReadValue} equivalent to {@link SetCell} with {@link IntCell} elements.
     *
     * @since 4.3
     */
    public interface IntSetReadValue extends SetReadValue {

        /**
         * @param value a double value
         * @return true if the set contains the value
         */
        boolean contains(int value);

        /**
         * @return a {@link Set} containing the {@link Integer} values
         */
        Set<Integer> getIntSet();

        /**
         * @return an iterator of the double set
         * @throws IllegalStateException if the set contains a missing value
         */
        PrimitiveIterator.OfInt intIterator();
    }

    /**
     * {@link WriteValue} equivalent to {@link SetCell} with {@link IntCell} elements.
     *
     * @since 4.3
     */
    public interface IntSetWriteValue extends SetWriteValue {

        /**
         * Set the value.
         *
         * @param values a collection of double values
         */
        void setIntColletionValue(Collection<Integer> values);
    }

    private static final class DefaultIntSetReadValue extends DefaultSetReadValue<IntListReadValue>
        implements IntSetReadValue {

        protected DefaultIntSetReadValue(final ListReadAccess reader) {
            super(reader, IntListValueFactory.INSTANCE);
        }

        @Override
        public boolean contains(final int value) {
            // TODO(benjamin) we can save the values sorted and do binary search
            final int[] values = m_value.getIntArray();
            for (int i = 0; i < values.length; i++) {
                if (value == values[i]) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Set<Integer> getIntSet() {
            return Arrays.stream(m_value.getIntArray()).boxed().collect(Collectors.toSet());
        }

        @Override
        public OfInt intIterator() {
            return m_value.intIterator();
        }
    }

    private static final class DefaultIntSetWriteValue extends DefaultSetWriteValue<IntListWriteValue>
        implements IntSetWriteValue {

        protected DefaultIntSetWriteValue(final ListWriteAccess writer) {
            super(writer, IntListValueFactory.INSTANCE);
        }

        @Override
        public void setIntColletionValue(final Collection<Integer> values) {
            m_value.setValue(values.stream().mapToInt(Integer::intValue).distinct().toArray());
        }
    }
}