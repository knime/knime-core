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
import java.util.PrimitiveIterator;
import java.util.PrimitiveIterator.OfInt;

import org.knime.core.data.IntValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.access.IntAccess.IntReadAccess;
import org.knime.core.data.v2.access.IntAccess.IntWriteAccess;
import org.knime.core.data.v2.access.ListAccess.ListAccessSpec;
import org.knime.core.data.v2.access.ListAccess.ListReadAccess;
import org.knime.core.data.v2.access.ListAccess.ListWriteAccess;
import org.knime.core.data.v2.value.IntValueFactory.IntReadValue;
import org.knime.core.data.v2.value.IntValueFactory.IntWriteValue;
import org.knime.core.data.v2.value.ListValueFactory.DefaultListReadValue;
import org.knime.core.data.v2.value.ListValueFactory.DefaultListWriteValue;
import org.knime.core.data.v2.value.ListValueFactory.ListReadValue;
import org.knime.core.data.v2.value.ListValueFactory.ListWriteValue;

/**
 * {@link ValueFactory} implementation for {@link ListCell} with elements of type {@link IntCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public final class IntListValueFactory implements ValueFactory<ListReadAccess, ListWriteAccess> {

    /** A stateless instance of {@link IntListValueFactory} */
    public static final IntListValueFactory INSTANCE = new IntListValueFactory();

    @Override
    public ListAccessSpec<IntReadAccess, IntWriteAccess> getSpec() {
        return new ListAccessSpec<>(IntValueFactory.INSTANCE);
    }

    @Override
    public IntListReadValue createReadValue(final ListReadAccess reader) {
        return new DefaultIntListReadValue(reader);
    }

    @Override
    public IntListWriteValue createWriteValue(final ListWriteAccess writer) {
        return new DefaultIntListWriteValue(writer);
    }

    /**
     * {@link ReadValue} equivalent to {@link ListCell} with {@link IntCell} elements.
     *
     * @since 4.3
     */
    public static interface IntListReadValue extends ListReadValue {

        /**
         * @param index the index in the list
         * @return the integer value at the index
         * @throws IllegalStateException if the value at this index is missing
         */
        int getInt(int index);

        /**
         * @return the list as a integer array
         * @throws IllegalStateException if the value at one index is missing
         */
        int[] getIntArray();

        /**
         * @return an iterator over the integer list
         * @throws IllegalStateException if the value at one index is missing
         */
        PrimitiveIterator.OfInt intIterator();
    }

    /**
     * {@link WriteValue} equivalent to {@link ListCell} with {@link IntCell} elements.
     *
     * @since 4.3
     */
    public static interface IntListWriteValue extends ListWriteValue {

        /**
         * Set the value.
         *
         * @param values a array of int values
         */
        void setValue(int[] values);
    }

    private static final class DefaultIntListReadValue extends DefaultListReadValue implements IntListReadValue {

        private DefaultIntListReadValue(final ListReadAccess reader) {
            super(reader, IntCell.TYPE);
        }

        @Override
        public int getInt(final int index) {
            final IntReadValue v = m_reader.getReadValue(index);
            return v.getIntValue();
        }

        @Override
        public int[] getIntArray() {
            final int[] result = new int[size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = getInt(i);
            }
            return result;
        }

        @Override
        public OfInt intIterator() {
            return Arrays.stream(getIntArray()).iterator();
        }
    }

    private static final class DefaultIntListWriteValue extends DefaultListWriteValue implements IntListWriteValue {

        private DefaultIntListWriteValue(final ListWriteAccess writer) {
            super(writer);
        }

        @Override
        public void setValue(final int[] values) {
            this.<IntValue, IntWriteValue> setValue(values.length, (i, v) -> v.setIntValue(values[i]));
        }
    }
}