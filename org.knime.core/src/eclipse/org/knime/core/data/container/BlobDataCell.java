/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   Dec 1, 2006 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import org.knime.core.data.DataCell;


/**
 * Abstract <b>B</b>inary <b>l</b>arge <b>ob</b>ject cell used to handle
 * potentially large cells.
 *
 * <p>Cell implementations extending from this class will be separately written
 * when a table is buffered on disc (each cell into a separate file).
 * This has two advantages:
 * <ul>
 * <li>Cells of this class will be only written once, i.e. any subsequent node
 * that references this cell object will not buffer a copy of it again
 * but rather reference to this single cell. (In comparison to ordinary cells
 * such as {@link org.knime.core.data.def.DoubleCell} or
 * {@link org.knime.core.data.def.StringCell},
 * which get copied when buffered multiple times.)
 * </li>
 * <li>
 * Objects of this class will be read from disc as late as possible, more
 * precisely, they get read when the
 * {@link org.knime.core.data.DataRow#getCell(int)} method is invoked. (Usually,
 * cells get deserialized when the
 * {@link org.knime.core.data.RowIterator#next()} method is called. This may
 * save much time when the cell is not used as for instance in a row filter,
 * row sampler or sorter node.
 * </li>
 * <li> Once deserialized, the cell is held in a
 * {@link java.lang.ref.SoftReference}, allowing for garbage collection when
 * memory gets limited (unless cell is otherwise referenced).
 * </li>
 * </ul>
 *
 * <p>In comparison to ordinary cell implementation, objects of this class
 * take slightly longer to (de)serialize as they get written to a separate
 * blobs directory.
 *
 * <p>Implementation note: The content of a <code>DataContainer</code> is
 * usually written to a file <code>knime_container_<i>date</i>_xxx.bin.gz</code>
 * file in the temp directory. <code>BlobDataCell</code> objects are treated
 * differently, they are written to a separate directory called
 * <code>knime_container_<i>date</i></code>, each blob cell into a separate
 * (compressed) file.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public abstract class BlobDataCell extends DataCell {

    /**
     * Shall blob cells be compressed when saved to blob file. This
     * field is <code>true</code>, which means the file is
     * compressed using the gzip compression format.
     *
     * <p>This field is accessed on the concrete implementation of
     * <code>BlobDataCell</code> using reflection. If objects of the derived
     * class shall not be compressed, define a static field with the same
     * name/type/scope, which returns <code>false</code>.
     */
    public static final boolean USE_COMPRESSION = true;

    private transient BlobAddress m_blobAddress;

    /** Get the location of the blob or <code>null</code>
     * if it hasn't been stored just yet.
     * @return This blob's address.
     */
    BlobAddress getBlobAddress() {
        return m_blobAddress;
    }

    /**
     * Set Blob address (which can't be <code>null</code>).
     * @param blobAddress The blob address object.
     */
    void setBlobAddress(final BlobAddress blobAddress) {
        if (blobAddress == null) {
            throw new NullPointerException("BlobAddress must not be null.");
        }
        if (m_blobAddress != null) {
            if (m_blobAddress.getBufferID() != -1) {
                // generally, blob addresses are only assigned once (by the
                // first buffer that a cell is added to. It may be reassigned
                // if it was assigned to a buffer writing to a stream directly
                // (for instance to a zip file using the table writer node).
            } else if (m_blobAddress.equals(blobAddress)) {
                // ignore setting the same blob address
            } else {
                throw new IllegalStateException(
                        "BlobAddress already assigned.");
            }
        }
        m_blobAddress = blobAddress;
    }

    /** Utility class that holds information where the blob is located.
     * This contains: bufferID, column index, index of blob in the column.
     * @noreference This class is not intended to be referenced by clients. */
    public static final class BlobAddress implements Serializable {

        private static final long serialVersionUID = -6278793794618726020L;

        private final int m_bufferID;
        private final int m_column;
        private int m_indexOfBlobInColumn;
        private boolean m_isUseCompression;

        private final String m_id;

        /**
         * Create new address object.
         *
         * @param bufferID ID of corresponding buffer.
         * @param column The column index
         * @param isUseCompression Whether or not the file is to be compressed.
         */
        BlobAddress(final int bufferID, final int column, final boolean isUseCompression) {
            this(bufferID, column, isUseCompression, null);
        }

        /**
         * Create new address object.
         *
         * @param bufferID ID of corresponding buffer.
         * @param column The column index
         * @param isUseCompression Whether or not the file is to be compressed.
         * @param an internal, unique id.
         */
        BlobAddress(final int bufferID, final int column, final boolean isUseCompression, final String id) {
            m_bufferID = bufferID;
            m_column = column;
            m_indexOfBlobInColumn = -1;
            m_isUseCompression = isUseCompression;
            m_id = id == null ? toString() : id;
        }

        /**
         * @param id internal id used in new columnar table backend. ID is required to be JVM unique.
         */
        BlobAddress(final String id) {
            this(-1, -1, USE_COMPRESSION, id);
        }

        /**
         * @param indexOfBlobInColumn The index of blob in the column.
         */
        public void setIndexOfBlobInColumn(final int indexOfBlobInColumn) {
            m_indexOfBlobInColumn = indexOfBlobInColumn;
        }

        /**
         * @return The ID of the Buffer which takes responsibility to
         * serialize out this object
         */
        public int getBufferID() {
            return m_bufferID;
        }

        /**
         * Get the index of the column in which the blob is defined
         *
         * @return the column index
         */
        int getColumn() {
            return m_column;
        }

        /** @return Whether or not the blob is (to be) compressed.
         */
        public boolean isUseCompression() {
            return m_isUseCompression;
        }

        /** Get the index of the blob in the columns (if a column only
         * contains blobs, this method returns the row index).
         *
         * @return The blob row address
         */
        int getIndexOfBlobInColumn() {
            return m_indexOfBlobInColumn;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "Buffer " + m_bufferID + ", col " + m_column
                + ", index " + m_indexOfBlobInColumn;
        }

        /** Writes address to a stream.
         * @param output To write to.
         * @throws IOException If that fails for any reason.
         */
        public void serialize(final DataOutput output) throws IOException {
            output.writeInt(m_bufferID);
            output.writeInt(m_column);
            output.writeInt(m_indexOfBlobInColumn);
            output.writeBoolean(m_isUseCompression);
        }

        /**
         * Reads blob address from stream.
         * @param input To read from.
         * @return A new blob instance.
         * @throws IOException If that fails.
         */
        public static BlobAddress deserialize(final DataInput input)
            throws IOException {
            int bufferID = input.readInt();
            int column = input.readInt();
            int indexOfBlobInColumn = input.readInt();
            boolean isCompress = input.readBoolean();
            BlobAddress address = new BlobAddress(bufferID, column, isCompress);
            address.setIndexOfBlobInColumn(indexOfBlobInColumn);
            return address;
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof BlobAddress) {
                BlobAddress a = (BlobAddress)obj;
                return a.m_bufferID == m_bufferID && a.m_column == m_column
                    && a.m_indexOfBlobInColumn == m_indexOfBlobInColumn && a.m_id == m_id;
            }
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return m_bufferID ^ (m_column << 8)
                ^ (m_indexOfBlobInColumn << 16);
        }

        /**
         * Return {@link BlobAddress} from {@link BlobDataCell}.
         *
         * @param cell the cell to extract the address from
         * @return {@link BlobAddress} of the blobCell in question
         *
         * @noreference This method is not intended to be referenced by clients.
         */
        public static BlobAddress getAddress(final BlobDataCell cell) {
            return cell.m_blobAddress;
        }

        /**
         * Set the {@link BlobAddress} of the provided {@link BlobDataCell}. The {@link BlobAddress} will be the id.
         *
         * @param cell the BlobDataCell to set the address for
         * @param id the id used as the address.
         *
         * @noreference This method is not intended to be referenced by clients.
         */
        public static void setAddress(final BlobDataCell cell, final String id) {
            cell.m_blobAddress = new BlobAddress(id);
        }
    }
}
