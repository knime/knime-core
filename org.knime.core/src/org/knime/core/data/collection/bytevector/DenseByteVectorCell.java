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
import org.knime.core.data.collection.bitvector.DenseBitVectorCellFactory;

/**
 *
 * @author ohl, University of Konstanz
 */
public class DenseByteVectorCell extends DataCell implements ByteVectorValue {
    /**
     * Convenience access member for
     * <code>DataType.getType(DenseByteVectorCell.class)</code>.
     *
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE =
            DataType.getType(DenseByteVectorCell.class);

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

    private static final DataCellSerializer<DenseByteVectorCell> SERIALIZER =
            new DenseByteVectorSerializer();

    /**
     * Returns the factory to read/write DataCells of this class from/to a
     * DataInput/DataOutput. This method is called via reflection.
     *
     * @return A serializer for reading/writing cells of this kind.
     * @see DataCell
     */
    public static final DataCellSerializer<DenseByteVectorCell> getCellSerializer() {
        return SERIALIZER;
    }

    private final DenseByteVector m_byteVector;

    /**
     * Use the {@link DenseBitVectorCellFactory} to create instances of this
     * cell.
     *
     * @param byteVector the byte vector a copy of which is stored in this cell.
     */
    DenseByteVectorCell(final DenseByteVector byteVector) {
        m_byteVector = new DenseByteVector(byteVector);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return ((DenseByteVectorCell)dc).m_byteVector.equals(m_byteVector);
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
        if (index > Integer.MAX_VALUE) {
            throw new ArrayIndexOutOfBoundsException("Index too large.");
        }
        return m_byteVector.get((int)index);
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
    public long nextCountIndex(final long startIdx) {
        if (startIdx > Integer.MAX_VALUE) {
            return -1;
        }
        return m_byteVector.nextCountIndex((int)startIdx);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long nextZeroIndex(final long startIdx) {
        if (startIdx > Integer.MAX_VALUE) {
            return -1;
        }
        return m_byteVector.nextZeroIndex((int)startIdx);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long sumOfAllCounts() {
        return m_byteVector.sumOfAllCounts();
    }

    /**
     * Returns a clone of the internal dense byte vector.
     * @return a copy of the internal dense byte vector.
     */
    public DenseByteVector getByteVectorCopy() {
        return new DenseByteVector(m_byteVector);
    }

    /** Factory for (de-)serializing a DenseBitVectorCell. */
    private static class DenseByteVectorSerializer implements
            DataCellSerializer<DenseByteVectorCell> {

        /**
         * {@inheritDoc}
         */
        public void serialize(final DenseByteVectorCell cell,
                final DataCellDataOutput out) throws IOException {

            byte[] cnts = cell.m_byteVector.getAllCountsAsBytes();
            out.writeInt(cnts.length);
            for (int i = 0; i < cnts.length; i++) {
                out.writeByte(cnts[i]);
            }
        }

        /**
         * {@inheritDoc}
         */
        public DenseByteVectorCell deserialize(final DataCellDataInput input)
                throws IOException {
            int arrayLength = input.readInt();
            byte[] cnts = new byte[arrayLength];
            for (int i = 0; i < arrayLength; i++) {
                cnts[i] = input.readByte();
            }
            return new DenseByteVectorCell(new DenseByteVector(cnts));
        }
    }

}
