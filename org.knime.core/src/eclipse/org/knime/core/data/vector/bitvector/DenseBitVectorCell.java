/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *   29.08.2008 (ohl): created
 */
package org.knime.core.data.vector.bitvector;

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
 * it can be used for well populated vectors. Its length is restricted to ({@link Integer#MAX_VALUE} -
 * 1) * 64 (i.e. 137438953344, in which case it uses around 16GigaByte of
 * memory).<br />
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
        return m_bitVector.toBinaryString();
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
    public String toBinaryString() {
        return m_bitVector.toBinaryString();
    }

    /**
     * Returns a clone of the internal dense bit vector.
     *
     * @return a copy of the internal dense bit vector.
     */
    public DenseBitVector getBitVectorCopy() {
        return new DenseBitVector(m_bitVector);
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
