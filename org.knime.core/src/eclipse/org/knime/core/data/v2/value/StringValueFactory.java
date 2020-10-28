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
 *   Oct 7, 2020 (dietzc): created
 */
package org.knime.core.data.v2.value;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.commons.codec.StringEncoder;
import org.knime.core.data.NominalValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.access.ObjectAccess.ObjectAccessSpec;
import org.knime.core.data.v2.access.ObjectAccess.ObjectReadAccess;
import org.knime.core.data.v2.access.ObjectAccess.ObjectSerializer;
import org.knime.core.data.v2.access.ObjectAccess.ObjectWriteAccess;

/**
 * {@link ValueFactory} implementation for {@link StringCell}.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
public class StringValueFactory implements ValueFactory<ObjectReadAccess<String>, ObjectWriteAccess<String>> {

    /**
     * Stateless instance of StringValueFactory.
     */
    public static final StringValueFactory INSTANCE = new StringValueFactory();

    @Override
    public StringAccessSpec getSpec() {
        return StringAccessSpec.SPEC_INSTANCE;
    }

    /* StringAccessSpec for StringValueFactories  */
    private static final class StringAccessSpec implements ObjectAccessSpec<String> {

        private final static StringAccessSpec SPEC_INSTANCE = new StringAccessSpec();

        @Override
        public ObjectSerializer<String> getSerializer() {
            return new StringObjectSerializer();
        }
    }

    @Override
    public StringReadValue createReadValue(final ObjectReadAccess<String> reader) {
        return new DefaultStringReadValue(reader);
    }

    @Override
    public StringWriteValue createWriteValue(final ObjectWriteAccess<String> writer) {
        return new DefaultStringWriteValue(writer);
    }

    /**
     * {@link ReadValue} equivalent to {@link StringCell}.
     *
     * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
     * @since 4.3
     */
    public interface StringReadValue extends //
        StringValue, //
        NominalValue, //
        ReadValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link StringCell}.
     *
     * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
     * @since 4.3
     */
    public interface StringWriteValue extends WriteValue<StringValue> {
        /**
         * @param value the string value to set
         */
        void setStringValue(String value);
    }

    /**
     * {@link ObjectSerializer} for Strings.
     *
     * NB: not a singleton as we want to create a new {@link StringEncoder} per instance.
     *
     * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
     * @since 4.3
     */
    static final class StringObjectSerializer implements ObjectSerializer<String> {

        @Override
        public String deserialize(final DataInput access) throws IOException {
            return access.readUTF();
        }

        @Override
        public void serialize(final String object, final DataOutput access) throws IOException {
            access.writeUTF(object);
        }

    }

    private static final class DefaultStringWriteValue implements StringWriteValue {
        private final ObjectWriteAccess<String> m_access;

        private DefaultStringWriteValue(final ObjectWriteAccess<String> access) {
            m_access = access;
        }

        @Override
        public void setValue(final StringValue value) {
            m_access.setObject(value.getStringValue());
        }

        @Override
        public void setStringValue(final String value) {
            m_access.setObject(value);
        }

    }

    private static final class DefaultStringReadValue implements StringReadValue {
        private final ObjectReadAccess<String> m_access;

        private DefaultStringReadValue(final ObjectReadAccess<String> access) {
            m_access = access;
        }

        @Override
        public String getStringValue() {
            return m_access.getObject();
        }

        @Override
        public StringCell getDataCell() {
            return new StringCell(m_access.getObject());
        }
    }
}
