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
 * Implementation stores the bits in a collection of longs (64 bit words). Thus
 * it can be used for well populated vectors. Its length is restricted to
 * ({@link Integer#MAX_VALUE} - 1) * 64 (i.e. 137438953344, in which case it
 * uses around 16GigaByte of memory).<br />
 *
 * @author ohl, University of Konstanz
 */
public class DenseBitVectorCell extends DataCell implements BitVectorValue {

    /**
     * Convenience access member for
     * <code>DataType.getType(DenseBitVectorCell.class)</code>.
     *
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE =
            DataType.getType(DenseBitVectorCell.class);

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

    private static final DataCellSerializer<DenseBitVectorCell> SERIALIZER =
            new DenseBitVectorSerializer();

    /**
     * Returns the factory to read/write DataCells of this class from/to a
     * DataInput/DataOutput. This method is called via reflection.
     *
     * @return A serializer for reading/writing cells of this kind.
     * @see DataCell
     */
    public static final DataCellSerializer<DenseBitVectorCell> getCellSerializer() {
        return SERIALIZER;
    }

    private final DenseBitVector m_bitVector;

    /**
     * Use the {@link DenseBitVectorCellFactory} to create instances of this
     * cell.
     *
     * @param bitVector the bit vector to store in this cell.
     */
    DenseBitVectorCell(final DenseBitVector bitVector) {
        m_bitVector = new DenseBitVector(bitVector);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return ((DenseBitVectorCell)dc).m_bitVector.equals(m_bitVector);
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
    private static class DenseBitVectorSerializer implements
            DataCellSerializer<DenseBitVectorCell> {

        /**
         * {@inheritDoc}
         */
        public void serialize(final DenseBitVectorCell cell,
                final DataCellDataOutput out) throws IOException {
            long[] bits = cell.m_bitVector.getAllBits();
            long length = cell.length();
            out.writeLong(length);
            out.writeInt(bits.length);
            for (int i = 0; i < bits.length; i++) {
                out.writeLong(bits[i]);
            }
        }

        /**
         * {@inheritDoc}
         */
        public DenseBitVectorCell deserialize(final DataCellDataInput input)
                throws IOException {
            long length = input.readLong();
            int arrayLength = input.readInt();
            long[] bits = new long[arrayLength];
            for (int i = 0; i < arrayLength; i++) {
                bits[i] = input.readLong();
            }
            return new DenseBitVectorCell(new DenseBitVector(bits, length));
        }
    }

}
