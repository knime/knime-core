/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *   03.09.2008 (ohl): created
 */
package org.knime.core.data.vector.bytevector;

import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.vector.bitvector.SparseBitVectorCellFactory;

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
    public long nextCountIndex(final long startIdx) {
        return m_byteVector.nextCountIndex(startIdx);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long nextZeroIndex(final long startIdx) {
        return m_byteVector.nextZeroIndex(startIdx);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long sumOfAllCounts() {
        return m_byteVector.sumOfAllCounts();
    }

    /**
     * Returns a clone of the internal sparse byte vector.
     * @return a copy of the internal sparse byte vector.
     */
    public SparseByteVector getByteVectorCopy() {
        return new SparseByteVector(m_byteVector);
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
