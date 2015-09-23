/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
import org.knime.core.data.DataCellFactory.FromComplexString;
import org.knime.core.data.DataCellFactory.FromSimpleString;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.DataValue;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;

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
     * Returns the factory to read/write DataCells of this class from/to a
     * DataInput/DataOutput. This method is called via reflection.
     *
     * @return A serializer for reading/writing cells of this kind.
     * @see DataCell
     * @deprecated use {@link DataTypeRegistry#getSerializer(Class)} instead
     */
    @Deprecated
    public static final DataCellSerializer<DenseByteVectorCell> getCellSerializer() {
        return new DenseByteVectorSerializer();
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
    protected boolean equalContent(final DataValue otherValue) {
        return ByteVectorValue.equalContent(this, (ByteVectorValue) otherValue);
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

    /**
     * Factory for {@link DenseByteVectorCell}s.
     *
     * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
     * @since 3.0
     */
    public static final class Factory implements FromSimpleString, FromComplexString {
        // TODO make this a ConfigurableDataCellFactory
        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell createCell(final String input) {
            byte[] bytes = new byte[(input.length()  + 1) / 2];

            int index = 0;
            for (int i = 0; i < input.length(); i += 2) {
                byte b = (byte) ((input.charAt(i) - '0') << 4);

                if (i < input.length() - 1) {
                    b += (byte) (input.charAt(i + 1) - '0');
                }
                bytes[index++] = b;
            }

            return new DenseByteVectorCell(new DenseByteVector(bytes));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataType getDataType() {
            return DenseByteVectorCell.TYPE;
        }
    }


    /**
     * Serializer {@link DenseByteVectorCell}s.
     *
     * @noreference This class is not intended to be referenced by clients.
     */
    public static final class DenseByteVectorSerializer implements DataCellSerializer<DenseByteVectorCell> {
        /**
         * {@inheritDoc}
         */
        @Override
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
        @Override
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
