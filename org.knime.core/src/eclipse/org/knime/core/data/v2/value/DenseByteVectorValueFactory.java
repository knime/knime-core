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

import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.data.vector.bytevector.DenseByteVector;
import org.knime.core.data.vector.bytevector.DenseByteVectorCell;
import org.knime.core.data.vector.bytevector.DenseByteVectorCellFactory;
import org.knime.core.table.access.VarBinaryAccess.VarBinaryReadAccess;
import org.knime.core.table.access.VarBinaryAccess.VarBinaryWriteAccess;
import org.knime.core.table.schema.VarBinaryDataSpec;

/**
 * {@link ValueFactory} implementation for {@link DenseByteVectorCell}.
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @since 4.6
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public final class DenseByteVectorValueFactory implements ValueFactory<VarBinaryReadAccess, VarBinaryWriteAccess> {

    /** Stateless instance of LongValueFactory */
    public static final DenseByteVectorValueFactory INSTANCE = new DenseByteVectorValueFactory();

    @Override
    public VarBinaryDataSpec getSpec() {
        return VarBinaryDataSpec.INSTANCE;
    }

    @Override
    public ReadValue createReadValue(final VarBinaryReadAccess reader) {
        return new DenseByteVectorReadValue(reader);
    }

    @Override
    public WriteValue<ByteVectorValue> createWriteValue(final VarBinaryWriteAccess writer) {
        return new DenseByteVectorWriteValue(writer);
    }

    private static final class DenseByteVectorReadValue extends AbstractValue<VarBinaryReadAccess>
        implements ReadValue, ByteVectorValue {

        DenseByteVectorReadValue(final VarBinaryReadAccess access) {
            super(access);
        }

        @Override
        public DenseByteVectorCell getDataCell() {
            // Note that we cannot cache the data cell value here because
            // the underlying access will be moved to the next row and this
            // DenseByteVectorReadValue will be reused and queried again.
            // So if someone accesses the ByteVectorValue interface of this ReadValue
            // directly, the performance will be bad.
            return m_access.getObject(in -> {
                final var bytes = in.readBytes();
                return new DenseByteVectorCellFactory(new DenseByteVector(bytes)).createDataCell();
            });
        }

        @Override
        public long length() {
            return getDataCell().length();
        }

        @Override
        public long sumOfAllCounts() {
            return getDataCell().sumOfAllCounts();
        }

        @Override
        public int cardinality() {
            return getDataCell().cardinality();
        }

        @Override
        public int get(final long index) {
            return getDataCell().get(index);
        }

        @Override
        public boolean isEmpty() {
            return getDataCell().isEmpty();
        }

        @Override
        public long nextZeroIndex(final long startIdx) {
            return getDataCell().nextZeroIndex(startIdx);
        }

        @Override
        public long nextCountIndex(final long startIdx) {
            return getDataCell().nextCountIndex(startIdx);
        }
    }

    private static final class DenseByteVectorWriteValue extends AbstractValue<VarBinaryWriteAccess>
        implements WriteValue<ByteVectorValue> {

        DenseByteVectorWriteValue(final VarBinaryWriteAccess access) {
            super(access);
        }

        @Override
        public void setValue(final ByteVectorValue value) {
            DenseByteVectorCell cell;
            if (value instanceof DenseByteVectorReadValue) {
                cell = ((DenseByteVectorReadValue)value).getDataCell();
            } else if (value instanceof DenseByteVectorCell) {
                cell = (DenseByteVectorCell)value;
            } else {
                throw new IllegalStateException("Expected a DenseByteVectorCell or DenseByteVectorReadValue, got "
                    + value.getClass().getName() + ". This is an implementation error.");
            }

            m_access.setObject(cell, (out, v) -> {
                final var length = v.length();

                for (long byteIdx = 0; byteIdx < length; byteIdx++) {
                    out.writeByte(v.get(byteIdx));
                }
            });
        }

    }
}
