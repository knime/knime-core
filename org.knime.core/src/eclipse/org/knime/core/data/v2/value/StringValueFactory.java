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

import org.knime.core.data.NominalValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.table.access.StringAccess.StringReadAccess;
import org.knime.core.table.access.StringAccess.StringWriteAccess;
import org.knime.core.table.schema.StringDataSpec;

/**
 * {@link ValueFactory} implementation for {@link StringCell}.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public class StringValueFactory implements ValueFactory<StringReadAccess, StringWriteAccess> {

    /**
     * Stateless instance of StringValueFactory.
     */
    public static final StringValueFactory INSTANCE = new StringValueFactory();

    private static final StringDataSpec SPEC_INSTANCE = StringDataSpec.INSTANCE;

    @Override
    public StringDataSpec getSpec() {
        return SPEC_INSTANCE;
    }

    @Override
    public StringReadValue createReadValue(final StringReadAccess reader) {
        return new DefaultStringReadValue(reader);
    }

    @Override
    public StringWriteValue createWriteValue(final StringWriteAccess writer) {
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

    private static final class DefaultStringWriteValue implements StringWriteValue {
        private final StringWriteAccess m_access;

        private DefaultStringWriteValue(final StringWriteAccess access) {
            m_access = access;
        }

        @Override
        public void setValue(final StringValue value) {
            m_access.setStringValue(value.getStringValue());
        }

        @Override
        public void setStringValue(final String value) {
            m_access.setStringValue(value);
        }

    }

    private static final class DefaultStringReadValue implements StringReadValue {
        private final StringReadAccess m_access;

        private DefaultStringReadValue(final StringReadAccess access) {
            m_access = access;
        }

        @Override
        public String getStringValue() {
            return m_access.getStringValue();
        }

        @Override
        public StringCell getDataCell() {
            return new StringCell(m_access.getStringValue());
        }
    }
}
