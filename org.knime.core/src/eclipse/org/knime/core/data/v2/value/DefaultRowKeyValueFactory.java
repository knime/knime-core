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

import org.knime.core.data.RowKey;
import org.knime.core.data.RowKeyValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.RowKeyReadValue;
import org.knime.core.data.v2.RowKeyValueFactory;
import org.knime.core.data.v2.RowKeyWriteValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.access.ObjectAccess.ObjectReadAccess;
import org.knime.core.data.v2.access.ObjectAccess.ObjectWriteAccess;
import org.knime.core.data.v2.access.ObjectAccess.StringAccessSpec;

/**
 * {@link ValueFactory} implementation for custom {@link RowKeyValue} and {@link RowKeyWriteValue}. 'Custom' means, that
 * the user can define the {@link RowKey} as needed as String.
 *
 * NB: RowKeys must are unique.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public final class DefaultRowKeyValueFactory
    implements RowKeyValueFactory<ObjectReadAccess<String>, ObjectWriteAccess<String>> {

    /**
     * Stateless instance.
     */
    public static final DefaultRowKeyValueFactory INSTANCE = new DefaultRowKeyValueFactory();

    @Override
    public StringAccessSpec getSpec() {
        return StringAccessSpec.INSTANCE;
    }

    @Override
    public RowKeyReadValue createReadValue(final ObjectReadAccess<String> reader) {
        return new DefaultRowKeyReadValue(reader);
    }

    @Override
    public RowKeyWriteValue createWriteValue(final ObjectWriteAccess<String> writer) {
        return new DefaultRowKeyWriteValue(writer);
    }

    /* Simple CustomRowKeyWriteValue */
    private final class DefaultRowKeyWriteValue implements RowKeyWriteValue {

        private final ObjectWriteAccess<String> m_access;

        public DefaultRowKeyWriteValue(final ObjectWriteAccess<String> access) {
            m_access = access;
        }

        @Override
        public void setMissing() {
            m_access.setMissing();
        }

        @Override
        public void setRowKey(final String key) {
            m_access.setObject(key);

        }

        @Override
        public void setRowKey(final RowKeyValue key) {
            m_access.setObject(key.getString());
        }

        @Override
        public void setValue(final RowKeyReadValue value) {
            setRowKey(value.getStringValue());
        }
    }

    /* Simple CustomRowKeyReadValue */
    private final class DefaultRowKeyReadValue implements RowKeyReadValue {

        private final ObjectReadAccess<String> m_access;

        public DefaultRowKeyReadValue(final ObjectReadAccess<String> access) {
            m_access = access;
        }

        @Override
        public boolean isMissing() {
            return m_access.isMissing();
        }

        @Override
        public StringCell getDataCell() {
            return new StringCell(m_access.getObject());
        }

        @Override
        public String getString() {
            return m_access.getObject();
        }
    }
}
