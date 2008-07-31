/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
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
 * -------------------------------------------------------------------
 * 
 * History
 *   Dec 1, 2006 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

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
        // generally, blob addresses are only assigned once (by the first
        // buffer that a cell is added to. It may be reassigned if it was
        // assigned to a buffer writing to a stream directly (for instance
        // to a zip file using the table writer node).
        if (m_blobAddress != null && m_blobAddress.getBufferID() != -1) {
            throw new IllegalStateException("BlobAddress already assigned.");
        }
        m_blobAddress = blobAddress;
    }
    
    /** Utility class that holds information where the blob is located. 
     * This contains: bufferID, column index, index of blob in the column. */
    static final class BlobAddress {
        
        private final int m_bufferID;
        private final int m_column;
        private int m_indexOfBlobInColumn;
        private boolean m_isUseCompression;
        
        /**
         * Create new address object.
         * @param bufferID ID of corresponding buffer.
         * @param column The column index
         * @param isUseCompression Whether or not the file is to be compressed.
         */
        BlobAddress(final int bufferID, final int column, 
                final boolean isUseCompression) {
            m_bufferID = bufferID;
            m_column = column;
            m_indexOfBlobInColumn = -1;
            m_isUseCompression = isUseCompression;
        }
        
        /** Set the corresponding address.
         * @param indexOfBlobInColumn The index of blob in the column.
         */
        public void setIndexOfBlobInColumn(final int indexOfBlobInColumn) {
            m_indexOfBlobInColumn = indexOfBlobInColumn;
        }
        
        /**
         * @return The ID of the Buffer which takes responsibility to 
         * serialize out this object
         */
        int getBufferID() {
            return m_bufferID;
        }
        
        /** @return the column */
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
        void serialize(final DataOutput output) throws IOException {
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
        static BlobAddress deserialize(final DataInput input) 
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
                    && a.m_indexOfBlobInColumn == m_indexOfBlobInColumn;
            }
            return false;
        }
        
        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return m_bufferID ^ (m_column << 8) 
                ^ (m_indexOfBlobInColumn << 16);
        }
    }
}
