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

import java.util.List;

import org.knime.core.data.StringValue;
import org.knime.core.data.collection.SparseListCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.access.IntAccess.IntReadAccess;
import org.knime.core.data.v2.access.IntAccess.IntWriteAccess;
import org.knime.core.data.v2.access.ListAccess.ListReadAccess;
import org.knime.core.data.v2.access.ListAccess.ListWriteAccess;
import org.knime.core.data.v2.access.ObjectAccess.ObjectReadAccess;
import org.knime.core.data.v2.access.ObjectAccess.ObjectWriteAccess;
import org.knime.core.data.v2.access.StructAccess.StructReadAccess;
import org.knime.core.data.v2.access.StructAccess.StructWriteAccess;
import org.knime.core.data.v2.value.StringListValueFactory.StringListReadValue;
import org.knime.core.data.v2.value.StringValueFactory.StringReadValue;
import org.knime.core.data.v2.value.StringValueFactory.StringWriteValue;

/**
 * {@link ValueFactory} implementation for {@link SparseListCell} with elements of type {@link StringCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public final class StringSparseListValueFactory extends ObjectSparseListValueFactory<String> {

    /** A stateless instance of {@link StringSparseListValueFactory} */
    public static final StringSparseListValueFactory INSTANCE = new StringSparseListValueFactory();

    /**
     * This constructor is not intended to be called directly. Use the stateless instance
     * {@link StringSparseListValueFactory#INSTANCE}.
     *
     * @noreference This constructor is not intended to be referenced by clients.
     */
    public StringSparseListValueFactory() {
        super(StringValueFactory.INSTANCE);
    }

    @Override
    public StringSparseListReadValue createReadValue(final StructReadAccess reader) {
        return new DefaultStringSparseListReadValue(reader.getInnerReadAccessAt(0), reader.getInnerReadAccessAt(1),
            reader.getInnerReadAccessAt(2), reader.getInnerReadAccessAt(3));
    }

    @Override
    public StringSparseListWriteValue createWriteValue(final StructWriteAccess writer) {
        return new DefaultStringSparseListWriteValue(writer.getWriteAccessAt(0), writer.getWriteAccessAt(1),
            writer.getWriteAccessAt(2), writer.getWriteAccessAt(3));
    }

    /**
     * {@link ReadValue} equivalent to {@link SparseListCell} with {@link StringCell} elements.
     *
     * @since 4.3
     */
    public static interface StringSparseListReadValue extends ObjectSparseListReadValue<String>, StringListReadValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link SparseListCell} with {@link StringCell} elements.
     *
     * @since 4.3
     */
    public static interface StringSparseListWriteValue extends ObjectSparseListWriteValue<String> {
    }

    private static final class DefaultStringSparseListReadValue
        extends AbstractObjectSparseListReadValue<StringReadValue, String> implements StringSparseListReadValue {

        private DefaultStringSparseListReadValue(final ObjectReadAccess<String> defaultAccess,
            final IntReadAccess sizeAccess, final ListReadAccess indicesAccess, final ListReadAccess listAccess) {
            super(defaultAccess, sizeAccess, indicesAccess, listAccess, StringValueFactory.INSTANCE,
                StringListValueFactory.INSTANCE);
        }

        @Override
        public int getLength() {
            return size();
        }

        @Override
        protected String getDefaultValue() {
            return m_defaultValue.getStringValue();
        }

        @Override
        protected String[] createObjectArray(final int size) {
            return new String[size];
        }
    }

    private static final class DefaultStringSparseListWriteValue
        extends AbstractObjectSparseListWriteValue<StringValue, StringWriteValue, String>
        implements StringSparseListWriteValue {

        protected DefaultStringSparseListWriteValue(final ObjectWriteAccess<String> defaultAccess,
            final IntWriteAccess sizeAccess, final ListWriteAccess indicesAccess, final ListWriteAccess listAccess) {
            super(defaultAccess, sizeAccess, indicesAccess, listAccess, StringValueFactory.INSTANCE,
                StringListValueFactory.INSTANCE);
        }

        @Override
        protected void setDefaultValue(final String value) {
            m_defaultValue.setStringValue(value);
        }

        @Override
        protected void setStorageList(final List<String> values) {
            m_storageList.setValue(values.toArray(new String[0]));
        }
    }
}