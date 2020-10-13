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
 */
package org.knime.core.data.v2.value;

import org.knime.core.data.DataCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.RowKeyValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.RowKeyReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.access.ObjectAccess.ObjectAccessSpec;
import org.knime.core.data.v2.access.ObjectAccess.ObjectReadAccess;
import org.knime.core.data.v2.access.ObjectAccess.ObjectSerializer;
import org.knime.core.data.v2.access.ObjectAccess.ObjectWriteAccess;
import org.knime.core.data.v2.value.StringValueFactory.StringObjectSerializer;

/**
 * {@link ValueFactory} implementation for custom {@link RowKeyReadValue} and {@link CustomRowKeyWriteValue}. 'Custom'
 * means, that the user can define the {@link RowKey} as needed as String (in contrast to no RowKey or Auto-generated
 * RowKeys).
 *
 * NB: RowKeys must be unique.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 */
public final class CustomRowKeyValueFactory
    implements ValueFactory<ObjectReadAccess<String>, ObjectWriteAccess<String>> {

    /**
     * Stateless instance of CustomRowKeyValueFactory.
     */
    public final static CustomRowKeyValueFactory INSTANCE = new CustomRowKeyValueFactory();

    private CustomRowKeyValueFactory() {
    }

    @Override
    public ObjectAccessSpec<String> getSpec() {
        return CustomRowKeyAccessSpec.SPEC_INSTANCE;
    }

    @Override
    public RowKeyReadValue createReadValue(final ObjectReadAccess<String> reader) {
        return new CustomRowKeyReadValue(reader);
    }

    @Override
    public CustomRowKeyWriteValue createWriteValue(final ObjectWriteAccess<String> writer) {
        return new DefaultCustomRowKeyWriteValue(writer);
    }

    /**
     * {@link WriteValue} to write (custom-) row keys.
     *
     * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
     * @since 4.3
     *
     * @apiNote API still experimental. It might change in future releases of KNIME Analytics Platform.
     */
    public interface CustomRowKeyWriteValue extends WriteValue<RowKeyReadValue> {
        /**
         * Set a unique row key.
         *
         * @param key the row key
         */
        void setRowKey(String key);

        /**
         * Set a unique row key.
         *
         * @param key the row key
         */
        void setRowKey(RowKeyValue key);
    }

    /* ObjectAccessSpec for CustomRowKeyValueFactories  */
    private static final class CustomRowKeyAccessSpec implements ObjectAccessSpec<String> {

        private final static CustomRowKeyAccessSpec SPEC_INSTANCE = new CustomRowKeyAccessSpec();

        @Override
        public ObjectSerializer<String> getSerializer() {
            return new StringObjectSerializer();
        }
    }

    /* Simple CustomRowKeyWriteValue */
    private final class DefaultCustomRowKeyWriteValue implements CustomRowKeyWriteValue {

        private final ObjectWriteAccess<String> m_access;

        public DefaultCustomRowKeyWriteValue(final ObjectWriteAccess<String> access) {
            m_access = access;
        }

        @Override
        public void setValue(final RowKeyReadValue value) {
            m_access.setObject(value.getString());
        }

        @Override
        public void setRowKey(final String key) {
            m_access.setObject(key);

        }

        @Override
        public void setRowKey(final RowKeyValue key) {
            m_access.setObject(key.getString());
        }

    }

    /* Simple CustomRowKeyReadValue */
    private final class CustomRowKeyReadValue implements RowKeyReadValue {

        private final ObjectReadAccess<String> m_access;

        public CustomRowKeyReadValue(final ObjectReadAccess<String> access) {
            m_access = access;
        }

        @Override
        public DataCell getDataCell() {
            return new StringCell(m_access.getObject());
        }

        @Override
        public String getString() {
            return m_access.getObject();
        }
    }
}
