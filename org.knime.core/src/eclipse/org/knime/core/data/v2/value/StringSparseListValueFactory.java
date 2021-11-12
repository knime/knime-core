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

import org.knime.core.data.StringValue;
import org.knime.core.data.collection.SparseListCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.value.SparseListValueFactory.AbstractSparseIterator;
import org.knime.core.data.v2.value.SparseListValueFactory.DefaultSparseListReadValue;
import org.knime.core.data.v2.value.SparseListValueFactory.DefaultSparseListWriteValue;
import org.knime.core.data.v2.value.ValueInterfaces.StringListReadValue;
import org.knime.core.data.v2.value.ValueInterfaces.StringListWriteValue;
import org.knime.core.data.v2.value.ValueInterfaces.StringReadValue;
import org.knime.core.data.v2.value.ValueInterfaces.StringSparseListReadValue;
import org.knime.core.data.v2.value.ValueInterfaces.StringSparseListWriteValue;
import org.knime.core.data.v2.value.ValueInterfaces.StringWriteValue;
import org.knime.core.table.access.IntAccess.IntReadAccess;
import org.knime.core.table.access.IntAccess.IntWriteAccess;
import org.knime.core.table.access.ListAccess.ListReadAccess;
import org.knime.core.table.access.ListAccess.ListWriteAccess;
import org.knime.core.table.access.StringAccess.StringReadAccess;
import org.knime.core.table.access.StringAccess.StringWriteAccess;
import org.knime.core.table.access.StructAccess.StructReadAccess;
import org.knime.core.table.access.StructAccess.StructWriteAccess;
import org.knime.core.table.schema.DataSpec;
import org.knime.core.table.schema.IntDataSpec;
import org.knime.core.table.schema.ListDataSpec;
import org.knime.core.table.schema.StringDataSpec;
import org.knime.core.table.schema.StructDataSpec;
import org.knime.core.table.schema.traits.DataTraits;
import org.knime.core.table.schema.traits.DefaultDataTraits;
import org.knime.core.table.schema.traits.DefaultListDataTraits;
import org.knime.core.table.schema.traits.DefaultStructDataTraits;

import com.google.common.base.Objects;

/**
 * {@link ValueFactory} implementation for {@link SparseListCell} with elements of type {@link StringCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public class StringSparseListValueFactory implements ValueFactory<StructReadAccess, StructWriteAccess> {

    /** A stateless instance of {@link StringSparseListValueFactory} */
    public static final StringSparseListValueFactory INSTANCE = new StringSparseListValueFactory();

    @Override
    public DataSpec getSpec() {
        final StringDataSpec defaultDataSpec = StringDataSpec.INSTANCE;
        final IntDataSpec sizeDataSpec = IntDataSpec.INSTANCE;
        final var indicesDataSpec = new ListDataSpec(IntDataSpec.INSTANCE);
        final var listDataSpec = new ListDataSpec(StringDataSpec.INSTANCE);
        return new StructDataSpec(defaultDataSpec, sizeDataSpec, indicesDataSpec, listDataSpec);
    }

    @Override
    public StringSparseListReadValue createReadValue(final StructReadAccess reader) {
        return new DefaultStringSparseListReadValue(reader.getAccess(0), reader.getAccess(1),
            reader.getAccess(2), reader.getAccess(3));
    }

    @Override
    public StringSparseListWriteValue createWriteValue(final StructWriteAccess writer) {
        return new DefaultStringSparseListWriteValue(writer.getWriteAccess(0), writer.getWriteAccess(1),
            writer.getWriteAccess(2), writer.getWriteAccess(3));
    }

    @Override
    public DataTraits getTraits() {
        return new DefaultStructDataTraits(DefaultDataTraits.EMPTY, DefaultDataTraits.EMPTY,
            new DefaultListDataTraits(DefaultDataTraits.EMPTY),
            new DefaultListDataTraits(DefaultDataTraits.EMPTY));
    }


    private static final class DefaultStringSparseListReadValue
        extends DefaultSparseListReadValue<StringReadValue, StringListReadValue, StringReadAccess>
        implements StringSparseListReadValue {

        private DefaultStringSparseListReadValue(final StringReadAccess defaultAccess, final IntReadAccess sizeAccess,
            final ListReadAccess indicesAccess, final ListReadAccess listAccess) {
            super(defaultAccess, sizeAccess, indicesAccess, listAccess, StringValueFactory.INSTANCE,
                StringListValueFactory.INSTANCE);
        }

        private String getStringFromStorage(final int storageIndex) {
            return m_storageList.getValue(storageIndex);
        }

        @Override
        public String getString(final int index) {
            final OptionalInt storageIndex = storageIndexForIndex(index);
            if (storageIndex.isPresent()) {
                return getStringFromStorage(storageIndex.getAsInt());
            } else {
                return m_defaultValue.getStringValue();
            }
        }

        @Override
        public String[] getStringArray() {
            final var values = new String[size()];
            final Iterator<String> iterator = stringIterator();
            for (int i = 0; i < values.length; i++) { // NOSONAR
                values[i] = iterator.next();
            }
            return values;
        }

        @Override
        public Iterator<String> stringIterator() {
            final var defaultElement = m_defaultValue.getStringValue();
            return new AbstractSparseIterator<String>(size(), m_storageIndices.size(), m_storageIndices::getInt) {

                @Override
                public String next() { // NOSONAR: The common 'NoSuchElementException' will not be thrown, as we have another default behavior: returning a default element
                    final OptionalInt storageIndex = nextStorageIndex();
                    if (storageIndex.isPresent()) {
                        return getStringFromStorage(storageIndex.getAsInt());
                    } else {
                        return defaultElement;
                    }
                }
            };
        }

        @Override
        public int getLength() {
            return size();
        }

        @Override
        public String getValue(final int index) {
            return getString(index);
        }

    }

    private static final class DefaultStringSparseListWriteValue
        extends DefaultSparseListWriteValue<StringValue, StringWriteValue, StringListWriteValue, StringWriteAccess>
        implements StringSparseListWriteValue {

        @SuppressWarnings("deprecation")
        protected DefaultStringSparseListWriteValue(final StringWriteAccess defaultAccess,
            final IntWriteAccess sizeAccess, final ListWriteAccess indicesAccess, final ListWriteAccess listAccess) {
            super(defaultAccess, sizeAccess, indicesAccess, listAccess, StringValueFactory.INSTANCE,
                StringListValueFactory.INSTANCE);
        }

        @Override
        public void setValue(final String[] values, final String defaultElement) {
            final List<Integer> storageIndices = new ArrayList<>();
            final List<String> storageList = new ArrayList<>();

            for (int i = 0; i < values.length; i++) { // NOSONAR
                final String v = values[i];
                if (!Objects.equal(v, defaultElement)) {
                    storageIndices.add(i);
                    storageList.add(v);
                }
            }

            m_defaultValue.setStringValue(defaultElement);
            m_sizeValue.setIntValue(storageList.size());
            m_storageIndices.setValue(storageIndices.stream().mapToInt(Integer::intValue).toArray());
            m_storageList.setValue(storageList.stream().toArray(String[]::new));
        }
    }
}