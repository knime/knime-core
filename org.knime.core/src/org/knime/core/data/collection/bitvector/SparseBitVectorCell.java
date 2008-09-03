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
 *   29.08.2008 (ohl): created
 */
package org.knime.core.data.collection.bitvector;

import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;

/**
 * Stores Zeros and Ones in a vector, i.e. with fixed positions. The vector has
 * a fixed length. <br />
 * Implementation assumes that the vector is only sparsely populated with '1's.
 * It stores the indices of the ones. For densely populated vectors
 * {@link DenseBitVector} is more suitable.<br />
 * The length of the vector is restricted to {@link Long#MAX_VALUE} (i.e.
 * 9223372036854775807). The number of ones that can be stored is limited to
 * {@link Integer#MAX_VALUE} (which is 2147483647), in which case it uses about
 * 16Gbyte of memory.<br />
 *
 * @author ohl, University of Konstanz
 */
public class SparseBitVectorCell extends DataCell implements BitVectorValue {
    /**
     * Convenience access member for
     * <code>DataType.getType(SparseBitVectorCell.class)</code>.
     *
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE =
            DataType.getType(SparseBitVectorCell.class);

    /**
     * Returns the preferred value class of this cell implementation. This
     * method is called per reflection to determine which is the preferred
     * renderer, comparator, etc.
     *
     * @return BitVectorValue.class;
     */
    public static final Class<? extends DataValue> getPreferredValueClass() {
        return BitVectorValue.class;
    }

    private static final DataCellSerializer<SparseBitVectorCell> SERIALIZER =
            new SparseBitVectorSerializer();

    /**
     * Returns the factory to read/write DataCells of this class from/to a
     * DataInput/DataOutput. This method is called via reflection.
     *
     * @return A serializer for reading/writing cells of this kind.
     * @see DataCell
     */
    public static final DataCellSerializer<SparseBitVectorCell> getCellSerializer() {
        return SERIALIZER;
    }

    private final SparseBitVector m_bitVector;

    /**
     * Use the {@link SparseBitVectorCellFactory} to create instances of this
     * cell.
     *
     * @param bitVector the bit vector to store in this cell.
     */
    SparseBitVectorCell(final SparseBitVector bitVector) {
        m_bitVector = new SparseBitVector(bitVector);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return ((SparseBitVectorCell)dc).m_bitVector.equals(m_bitVector);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_bitVector.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_bitVector.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toHexString() {
        return m_bitVector.toHexString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long cardinality() {
        return m_bitVector.cardinality();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean get(final long index) {
        return m_bitVector.get(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return m_bitVector.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long length() {
        return m_bitVector.length();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long nextClearBit(final long startIdx) {
        return m_bitVector.nextClearBit(startIdx);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long nextSetBit(final long startIdx) {
        return m_bitVector.nextSetBit(startIdx);
    }

    /** Factory for (de-)serializing a DenseBitVectorCell. */
    private static class SparseBitVectorSerializer implements
            DataCellSerializer<SparseBitVectorCell> {

        /**
         * {@inheritDoc}
         */
        public void serialize(final SparseBitVectorCell cell,
                final DataCellDataOutput out) throws IOException {

            long[] idx = cell.m_bitVector.getAllOneIndices();
            long length = cell.length();
            out.writeLong(length);
            out.writeInt(idx.length);
            for (int i = 0; i < idx.length; i++) {
                out.writeLong(idx[i]);
            }
        }

        /**
         * {@inheritDoc}
         */
        public SparseBitVectorCell deserialize(final DataCellDataInput input)
                throws IOException {
            long length = input.readLong();
            int arrayLength = input.readInt();
            long[] idx = new long[arrayLength];
            for (int i = 0; i < arrayLength; i++) {
                idx[i] = input.readLong();
            }
            return new SparseBitVectorCell(new SparseBitVector(length, idx));
        }
    }

}
