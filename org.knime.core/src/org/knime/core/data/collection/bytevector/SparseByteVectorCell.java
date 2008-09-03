/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 *
 * History
 *   03.09.2008 (ohl): created
 */
package org.knime.core.data.collection.bytevector;

import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;

/**
 *
 * @author ohl, University of Konstanz
 */
public class SparseByteVectorCell extends DataCell implements ByteVectorValue {
    /**
     * Convenience access member for
     * <code>DataType.getType(SparseByteVectorCell.class)</code>.
     *
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE =
            DataType.getType(SparseByteVectorCell.class);

    /**
     * Returns the preferred value class of this cell implementation. This
     * method is called per reflection to determine which is the preferred
     * renderer, comparator, etc.
     *
     * @return ByteVectorValue.class;
     */
    public static final Class<? extends DataValue> getPreferredValueClass() {
        return ByteVectorValue.class;
    }

    private static final DataCellSerializer<SparseByteVectorCell> SERIALIZER =
            new SparseByteVectorSerializer();

    /**
     * Returns the factory to read/write DataCells of this class from/to a
     * DataInput/DataOutput. This method is called via reflection.
     *
     * @return A serializer for reading/writing cells of this kind.
     * @see DataCell
     */
    public static final DataCellSerializer<SparseByteVectorCell> getCellSerializer() {
        return SERIALIZER;
    }

    private final SparseByteVector m_byteVector;

    /**
     * Use the {@link SparseBitVectorCellFactory} to create instances of this
     * cell.
     *
     * @param byteVector the byte vector a copy of which is stored in this cell.
     */
    SparseByteVectorCell(final SparseByteVector byteVector) {
        m_byteVector = new SparseByteVector(byteVector);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return ((SparseByteVectorCell)dc).m_byteVector.equals(m_byteVector);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_byteVector.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_byteVector.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int cardinality() {
        return m_byteVector.cardinality();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int get(final long index) {
        return m_byteVector.get(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return m_byteVector.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long length() {
        return m_byteVector.length();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long nextCountIndex(final int startIdx) {
        return m_byteVector.nextCountIndex(startIdx);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long nextZeroIndex(final int startIdx) {
        return m_byteVector.nextZeroIndex(startIdx);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long sumOfAllCounts() {
        return m_byteVector.sumOfAllCounts();
    }

    /** Factory for (de-)serializing a DenseBitVectorCell. */
    private static class SparseByteVectorSerializer implements
            DataCellSerializer<SparseByteVectorCell> {

        /**
         * {@inheritDoc}
         */
        public void serialize(final SparseByteVectorCell cell,
                final DataCellDataOutput out) throws IOException {

            long length = cell.m_byteVector.length();
            long[] idxs = cell.m_byteVector.getAllCountIndices();
            byte[] cnts = cell.m_byteVector.getAllCounts();
            assert idxs.length == cnts.length;
            out.writeLong(length);
            out.writeInt(idxs.length);
            for (int i = 0; i < idxs.length; i++) {
                out.writeLong(idxs[i]);
                out.writeByte(cnts[i]);
            }
        }

        /**
         * {@inheritDoc}
         */
        public SparseByteVectorCell deserialize(final DataCellDataInput input)
                throws IOException {
            long length = input.readLong();
            int arrayLength = input.readInt();
            long[] idxs = new long[arrayLength];
            byte[] cnts = new byte[arrayLength];
            for (int i = 0; i < arrayLength; i++) {
                idxs[i] = input.readLong();
                cnts[i] = input.readByte();
            }
            return new SparseByteVectorCell(new SparseByteVector(length, idxs,
                    cnts));
        }
    }

}
