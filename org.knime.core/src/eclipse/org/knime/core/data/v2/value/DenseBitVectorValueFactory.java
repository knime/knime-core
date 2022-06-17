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
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;
import org.knime.core.table.access.StringAccess.StringReadAccess;
import org.knime.core.table.access.StringAccess.StringWriteAccess;
import org.knime.core.table.schema.StringDataSpec;

/**
 * {@link ValueFactory} implementation for {@link DenseBitVectorCell}.
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @since 4.6
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public final class DenseBitVectorValueFactory implements ValueFactory<StringReadAccess, StringWriteAccess> {

    /** Stateless instance of LongValueFactory */
    public static final DenseBitVectorValueFactory INSTANCE = new DenseBitVectorValueFactory();

    @Override
    public StringDataSpec getSpec() {
        return StringDataSpec.INSTANCE;
    }

    @Override
    public ReadValue createReadValue(final StringReadAccess reader) {
        return new DenseBitVectorReadValue(reader);
    }

    @Override
    public WriteValue<BitVectorValue> createWriteValue(final StringWriteAccess writer) {
        return new DenseBitVectorWriteValue(writer);
    }


    private static final class DenseBitVectorReadValue extends AbstractValue<StringReadAccess> implements ReadValue {

        DenseBitVectorReadValue(final StringReadAccess access) {
            super(access);
        }

        @Override
        public DataCell getDataCell() {
            final var hexString = m_access.getStringValue();
            return new DenseBitVectorCellFactory(hexString).createDataCell();
        }

    }

    private static final class DenseBitVectorWriteValue extends AbstractValue<StringWriteAccess> implements WriteValue<BitVectorValue> {

        DenseBitVectorWriteValue(final StringWriteAccess access) {
            super(access);
        }

        @Override
        public void setValue(final BitVectorValue value) {
            m_access.setStringValue(value.toHexString());
        }

    }
}
