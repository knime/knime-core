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

import java.io.DataOutput;
import java.io.IOException;

import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;
import org.knime.core.table.access.VarBinaryAccess.VarBinaryReadAccess;
import org.knime.core.table.access.VarBinaryAccess.VarBinaryWriteAccess;
import org.knime.core.table.io.ReadableDataInput;
import org.knime.core.table.schema.VarBinaryDataSpec;

/**
 * {@link ValueFactory} implementation for {@link DenseBitVectorCell}.
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @since 4.7
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public final class DenseBitVectorValueFactory implements ValueFactory<VarBinaryReadAccess, VarBinaryWriteAccess> {

    /** Stateless instance of LongValueFactory */
    public static final DenseBitVectorValueFactory INSTANCE = new DenseBitVectorValueFactory();

    @Override
    public VarBinaryDataSpec getSpec() {
        return VarBinaryDataSpec.INSTANCE;
    }

    @Override
    public ReadValue createReadValue(final VarBinaryReadAccess reader) {
        return new DenseBitVectorReadValue(reader);
    }

    @Override
    public WriteValue<BitVectorValue> createWriteValue(final VarBinaryWriteAccess writer) {
        return new DenseBitVectorWriteValue(writer);
    }

    private static final class DenseBitVectorReadValue extends AbstractValue<VarBinaryReadAccess>
        implements ReadValue, BitVectorValue {

        DenseBitVectorReadValue(final VarBinaryReadAccess access) {
            super(access);
        }

        private static DenseBitVectorCell deserialize(final ReadableDataInput in) {
            try {
                final var length = in.readLong();
                final var v = new DenseBitVector(length);
                short localBitIdx = 0;
                byte storage = in.readByte();
                for (long bitIdx = 0; bitIdx < length; bitIdx++) {
                    if (localBitIdx >= 8) {
                        localBitIdx = 0;
                        storage = in.readByte();
                    }

                    if (0 < (storage & (1 << localBitIdx))) { // NOSONAR: bit shift cannot go out of the byte range
                        v.set(bitIdx);
                    }

                    localBitIdx++;
                }

                return new DenseBitVectorCellFactory(v).createDataCell();
            } catch (IOException e) {
                throw new IllegalStateException("Error when deserializing DenseBitVector", e);
            }
        }

        @Override
        public DenseBitVectorCell getDataCell() {
            // Note that we cannot cache the data cell value here because
            // the underlying access will be moved to the next row and this
            // DenseBitVectorReadValue will be reused and queried again.
            // But thanks to VarBinary data being cached by the ObjectCache,
            // repeated accesses for the same row should only call deserialize()
            // once.
            return m_access.getObject(DenseBitVectorReadValue::deserialize);
        }

        @Override
        public long length() {
            return getDataCell().length();
        }

        @Override
        public long cardinality() {
            return getDataCell().cardinality();
        }

        @Override
        public boolean get(final long index) {
            return getDataCell().get(index);
        }

        @Override
        public boolean isEmpty() {
            return getDataCell().isEmpty();
        }

        @Override
        public long nextClearBit(final long startIdx) {
            return getDataCell().nextClearBit(startIdx);
        }

        @Override
        public long nextSetBit(final long startIdx) {
            return getDataCell().nextSetBit(startIdx);
        }

        @Override
        public String toHexString() {
            return getDataCell().toHexString();
        }

        @Override
        public String toBinaryString() {
            return getDataCell().toBinaryString();
        }

    }

    private static final class DenseBitVectorWriteValue extends AbstractValue<VarBinaryWriteAccess>
        implements WriteValue<BitVectorValue> {

        DenseBitVectorWriteValue(final VarBinaryWriteAccess access) {
            super(access);
        }

        private static void serialize(final DataOutput out, final BitVectorValue v) {
            try {
                final var length = v.length();
                out.writeLong(length);

                short localBitIdx = 0; // addresses the bit in the storage byte
                byte storage = 0;
                for (long bitIdx = 0; bitIdx < length; bitIdx++) {
                    if (v.get(bitIdx)) {
                        storage |= (1 << localBitIdx);
                    }
                    localBitIdx++;
                    if (localBitIdx == 8) {
                        out.writeByte(storage);
                        localBitIdx = 0;
                        storage = 0;
                    }
                }
                if (localBitIdx > 0) {
                    // write the last unfilled byte
                    out.writeByte(storage);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Value " + v + " could not be serialized", e);
            }
        }

        @Override
        public void setValue(final BitVectorValue value) {
            DenseBitVectorCell cell;
            if (value instanceof DenseBitVectorReadValue) {
                cell = ((DenseBitVectorReadValue)value).getDataCell();
            } else if (value instanceof DenseBitVectorCell) {
                cell = (DenseBitVectorCell)value;
            } else {
                throw new IllegalStateException("Expected DenseBitVectorCell or DenseBitVectorReadValue, but got "
                    + value.getClass().getName() + ". This is an implementation error.");
            }

            m_access.setObject(cell, DenseBitVectorWriteValue::serialize);
        }

    }
}
