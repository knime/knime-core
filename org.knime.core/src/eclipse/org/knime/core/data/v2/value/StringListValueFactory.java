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

import org.knime.core.data.StringValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.value.ListValueFactory.DefaultListReadValue;
import org.knime.core.data.v2.value.ListValueFactory.DefaultListWriteValue;
import org.knime.core.data.v2.value.ListValueFactory.ListReadValue;
import org.knime.core.data.v2.value.ListValueFactory.ListWriteValue;
import org.knime.core.data.v2.value.StringValueFactory.StringWriteValue;
import org.knime.core.data.vector.stringvector.StringVectorValue;
import org.knime.core.table.access.ListAccess.ListReadAccess;
import org.knime.core.table.access.ListAccess.ListWriteAccess;
import org.knime.core.table.access.StringAccess.StringReadAccess;
import org.knime.core.table.schema.ListDataSpec;
import org.knime.core.table.schema.StringDataSpec;
import org.knime.core.table.schema.traits.DataTraits;
import org.knime.core.table.schema.traits.DefaultDataTraits;
import org.knime.core.table.schema.traits.DefaultListDataTraits;

/**
 * {@link ValueFactory} implementation for {@link ListCell} with elements of type {@link StringCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public final class StringListValueFactory implements ValueFactory<ListReadAccess, ListWriteAccess> {

    /** A stateless instance of {@link StringListValueFactory} */
    public static final StringListValueFactory INSTANCE = new StringListValueFactory();

    @Override
    public ListDataSpec getSpec() {
        return new ListDataSpec(StringDataSpec.INSTANCE);
    }

    @Override
    public StringListReadValue createReadValue(final ListReadAccess reader) {
        return new DefaultStringListReadValue(reader);
    }

    @Override
    public StringListWriteValue createWriteValue(final ListWriteAccess writer) {
        return new DefaultStringListWriteValue(writer);
    }

    @Override
    public DataTraits getTraits() {
        return new DefaultListDataTraits(DefaultDataTraits.EMPTY);
    }

    /**
     * {@link ReadValue} equivalent to {@link ListCell} with {@link StringCell} elements.
     *
     * @since 4.3
     */
    public static interface StringListReadValue extends ListReadValue, StringVectorValue {

        /**
         * @param index at which to obtain the returned String
         * @return the String at <b>index</b>
         */
        String getString(int index);

        /**
         *
         * @return the content of the list as array
         */
        String[] getStringArray();

        /**
         * @return an {@link Iterator} over the Strings in the list
         */
        Iterator<String> stringIterator();

    }

    /**
     * {@link WriteValue} equivalent to {@link ListCell} with {@link StringCell} elements.
     *
     * @since 4.3
     */
    public static interface StringListWriteValue extends ListWriteValue {

        /**
         * @param values to set
         */
        void setValue(String[] values);

    }

    private static final class DefaultStringListReadValue extends DefaultListReadValue
        implements StringListReadValue {

        private DefaultStringListReadValue(final ListReadAccess reader) {
            super(reader, StringValueFactory.INSTANCE, StringCell.TYPE);
        }

        @Override
        public String getString(final int index) {
            final StringReadAccess v = m_reader.getAccess(index);
            return v.getStringValue();
        }

        @Override
        public int getLength() {
            return size();
        }

        @Override
        public String getValue(final int index) {
            return getString(index);
        }

        @Override
        public String[] getStringArray() {
            final String[] result = new String[size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = getString(i);
            }
            return result;
        }

        @Override
        public Iterator<String> stringIterator() {
            return Arrays.stream(getStringArray()).iterator();
        }

    }

    private static final class DefaultStringListWriteValue extends DefaultListWriteValue
        implements StringListWriteValue {

        private DefaultStringListWriteValue(final ListWriteAccess writer) {
            super(writer, StringValueFactory.INSTANCE);
        }

        @Override
        public void setValue(final String[] values) {
            this.<StringValue, StringWriteValue> setValue(values.length, (i, v) -> v.setStringValue(values[i]));
        }
    }
}