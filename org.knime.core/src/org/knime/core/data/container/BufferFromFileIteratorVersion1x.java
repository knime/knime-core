/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   Aug 8, 2008 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.BlobDataCell.BlobAddress;
import org.knime.core.node.NodeLogger;

/**
 * (Obsolete) File iterator to read files written by a {@link Buffer}. This
 * class is used for backward compatibility, it reads all stream written with
 * KNIME 1.x or the TechPreview of 2.0.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
final class BufferFromFileIteratorVersion1x extends Buffer.FromFileIterator {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(BufferFromFileIteratorVersion1x.class);
    
    /** Associated buffer. */
    private final Buffer m_buffer;
    
    /** Row pointer. */
    private int m_pointer;
    
    /** If an exception has been thrown while reading from this buffer (only
     * if it has been written to disc). If so, further error messages are
     * only written to debug output in order to reduce message spam on the
     * console. */
    private boolean m_hasThrownReadException;

    /** Stream reading from the binary file. */
    private DCObjectInputStream m_inStream;

    /** Inits the iterator by opening the input stream.
     * @param buffer Associated buffer.
     * @throws IOException If stream can't be opened. */
    BufferFromFileIteratorVersion1x(final Buffer buffer) throws IOException {
        m_pointer = 0;
        if (buffer.getBinFile() == null) {
            throw new IOException("Unable to read table from file, "
                    + "table has been cleared.");
        }
        m_buffer = buffer;
        BufferedInputStream bufferedStream =
            new BufferedInputStream(new FileInputStream(buffer.getBinFile()));
        InputStream in;
        // stream was not zipped in KNIME 1.1.x
        if (!buffer.isBinFileGZipped() || buffer.getReadVersion() < 3) {
            in = bufferedStream;
        } else {
            in = new GZIPInputStream(bufferedStream);
            // buffering is important when reading gzip streams
            in = new BufferedInputStream(in);
        }
        m_inStream = new DCObjectInputStream(in);
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasNext() {
        boolean hasNext = m_pointer < m_buffer.size();
        if (!hasNext && (m_inStream != null)) {
            close();
        }
        return hasNext;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized BlobSupportDataRow next() {
        if (!hasNext()) {
            throw new NoSuchElementException("Iterator at end");
        }
        final DCObjectInputStream inStream = m_inStream;
        RowKey key;
        try {
            key = readRowKey(inStream);
        } catch (Throwable throwable) {
            handleReadThrowable(throwable);
            // can't ensure that we generate a unique key but it should
            // cover 99.9% of all cases
            String keyS = "Read_failed__auto_generated_key_" + m_pointer;
            key = new RowKey(keyS);
        }
        int colCount = m_buffer.getTableSpec().getNumColumns();
        DataCell[] cells = new DataCell[colCount];
        for (int i = 0; i < colCount; i++) {
            DataCell nextCell;
            try {
                nextCell = readDataCell(m_inStream);
            } catch (final Throwable e) {
                handleReadThrowable(e);
                nextCell = DataType.getMissingCell();
            }
            cells[i] = nextCell;
        }
        try {
            if (m_buffer.getReadVersion() >= 5) {
                byte eoRow = inStream.readByte();
                if (eoRow != BYTE_ROW_SEPARATOR) {
                    throw new IOException("Expected end of row byte, "
                        + "got '" + eoRow + "', (byte " + (int)eoRow + ")");
                }
            } else {
                char eoRow = inStream.readChar();
                if (eoRow != '\n') {
                    throw new IOException("Expected end of row character, "
                        + "got '" + eoRow + "', (char " + (int)eoRow + ")");
                }
            }
        } catch (IOException ioe) {
            handleReadThrowable(ioe);
        } finally {
            m_pointer++;
        }
        return new BlobSupportDataRow(key, cells);
    }
    
    /** Reads a row key from the stream. If the buffer is set to not persist
     * row keys (the {@link NoKeyBuffer}, it will a static row key.
     * @param inStream To read from
     * @return The row key as read right from the stream.
     * @throws IOException If reading fails for IO problems.
     */
    private RowKey readRowKey(final DCObjectInputStream inStream) 
        throws IOException {
        if (m_buffer.shouldSkipRowKey()) {
            return DUMMY_ROW_KEY;
        }
        String key;
        // < 5 is version 1.3.x and before, see above
        if (m_buffer.getReadVersion() >= 5) {
            key = inStream.readUTF();
        } else {
            key = readDataCell(inStream).toString();
        }
        return new RowKey(key);
    }

    /** Reads a datacell from inStream, does no exception handling. */
    private DataCell readDataCell(final DCObjectInputStream inStream)
            throws IOException {
        if (m_buffer.getReadVersion() == 1) {
            return readDataCellVersion1(inStream);
        }
        inStream.setCurrentClassLoader(null);
        byte identifier = inStream.readByte();
        if (identifier == BYTE_TYPE_MISSING) {
            return DataType.getMissingCell();
        }
        final boolean isSerialized = identifier == BYTE_TYPE_SERIALIZATION;
        if (isSerialized) {
            identifier = inStream.readByte();
        }
        CellClassInfo type = m_buffer.getTypeForChar(identifier);
        Class<? extends DataCell> cellClass = type.getCellClass();
        boolean isBlob = 
            BlobDataCell.class.isAssignableFrom(cellClass);
        if (isBlob) {
            BlobAddress address = inStream.readBlobAddress();
            Buffer blobBuffer = m_buffer;
            if (address.getBufferID() != m_buffer.getBufferID()) {
                ContainerTable cnTbl =
                    m_buffer.getGlobalRepository().get(address.getBufferID());
                if (cnTbl == null) {
                    throw new IOException(
                    "Unable to retrieve table that owns the blob cell");
                }
                blobBuffer = cnTbl.getBuffer();
            }
            return new BlobWrapperDataCell(blobBuffer, address, type);
        }
        if (isSerialized) {
            try {
                ClassLoader cellLoader = cellClass.getClassLoader();
                inStream.setCurrentClassLoader(cellLoader);
                return (DataCell)inStream.readObject();
            } catch (ClassNotFoundException cnfe) {
                IOException ioe = new IOException(cnfe.getMessage());
                ioe.initCause(cnfe);
                throw ioe;
            }
        } else {
            DataCellSerializer<? extends DataCell> serializer =
                type.getSerializer();
            assert serializer != null;
            return inStream.readDataCell(serializer);
        }
    }

    /** Backward compatibility: DataCells that are (java-) serialized are
     * not annotated with a byte identifying their type. We need that in the
     * future to make sure we use the right class loader.
     * @param inStream To read from.
     * @return The cell.
     * @throws IOException If fails.
     */
    private DataCell readDataCellVersion1(final DCObjectInputStream inStream)
        throws IOException {
        byte identifier = inStream.readByte();
        if (identifier == BYTE_TYPE_MISSING) {
            return DataType.getMissingCell();
        }
        if (identifier == BYTE_TYPE_SERIALIZATION) {
            try {
                return (DataCell)inStream.readObject();
            } catch (ClassNotFoundException cnfe) {
                IOException ioe = new IOException(cnfe.getMessage());
                ioe.initCause(cnfe);
                throw ioe;
            }
        } else {
            CellClassInfo type = m_buffer.getTypeForChar(identifier);
            DataCellSerializer<? extends DataCell> serializer =
                type.getSerializer();
            assert serializer != null;
            try {
                return inStream.readDataCell(serializer);
            } catch (IOException ioe) {
                LOGGER.debug("Unable to read cell from file.", ioe);
                return DataType.getMissingCell();
            }
        }
    } // readDataCellVersion1(DCObjectInputStream)

    private void handleReadThrowable(final Throwable throwable) {
        String warnMessage = "Errors while reading row " + (m_pointer + 1) 
            + " from file \"" + m_buffer.getBinFile().getName() + "\": " 
            + throwable.getMessage();
        if (!m_hasThrownReadException) {
            warnMessage = warnMessage.concat(
                    "; Suppressing further warnings.");
            LOGGER.error(warnMessage, throwable);
        } else {
            LOGGER.debug(warnMessage, throwable);
        }
        if (!(throwable instanceof IOException)) {
            String messageCoding = throwable.getClass().getSimpleName()
            + " caught, implementation may only throw IOException.";
            if (!m_hasThrownReadException) {
                LOGGER.coding(messageCoding);
            } else {
                LOGGER.debug(messageCoding);
            }
        }
        m_hasThrownReadException = true;
    }
    
    /** {@inheritDoc} */
    @Override
    boolean performClose() throws IOException {
        m_pointer = m_buffer.size(); // mark it as end of file
        // already closed (clear has been called before)
        if (m_inStream == null) {
            return false;
        }
        InputStream in = m_inStream;
        m_inStream = null;
        in.close();
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        m_buffer.clearIteratorInstance(this, true);
    }

    /** {@inheritDoc} */
    @Override
    protected void finalize() throws Throwable {
        /* This all relates much to bug #63: The temp files are not
         * deleted under windows. It seems that there are open streams
         * when the VM closes. */
        close();
    }
    
    /** Reads the blob from the given blob address.
     * @param buffer The owning buffer.
     * @param blobAddress The address to read from.
     * @param cl The expected class.
     * @return The blob cell being read.
     * @throws IOException If that fails.
     */
    static BlobDataCell readBlobDataCell(final Buffer buffer, 
            final BlobAddress blobAddress,
            final CellClassInfo cl) throws IOException {
        assert buffer.getBufferID() == blobAddress.getBufferID() 
            : "Buffer IDs don't match: " + buffer.getBufferID() + " vs. " 
            + blobAddress.getBufferID();
        int column = blobAddress.getColumn();
        int indexInColumn = blobAddress.getIndexOfBlobInColumn();
        boolean isCompress = blobAddress.isUseCompression();
        File inFile = buffer.getBlobFile(
                indexInColumn, column, false, isCompress);
        InputStream in = new BufferedInputStream(
                new FileInputStream(inFile));
        if (isCompress) {
            in = new GZIPInputStream(in);
        }
        Class<? extends DataCell> cellClass = cl.getCellClass();
        DataCellSerializer<? extends DataCell> ser = cl.getSerializer();
        InputStream inStream = null;
        BlobDataCell result;
        try {
            if (ser != null) {
                inStream = new DataInputStream(in);
                ObsoleteDCDataInputStream input = 
                    new ObsoleteDCDataInputStream((DataInputStream)inStream);
                // the DataType class will reject Serializer that do not 
                // have the appropriate return type
                result = (BlobDataCell)ser.deserialize(input);
                result.setBlobAddress(blobAddress);
            } else {
                inStream = new PriorityGlobalObjectInputStream(in);
                ((PriorityGlobalObjectInputStream)inStream).
                    setCurrentClassLoader(cellClass.getClassLoader());
                try {
                    result = (BlobDataCell)
                        ((ObjectInputStream)inStream).readObject();
                    result.setBlobAddress(blobAddress);
                } catch (ClassNotFoundException cnfe) {
                    IOException e =
                        new IOException("Unable to restore blob cell");
                    e.initCause(cnfe);
                    throw e;
                }
            }
            return result;
        } finally {
            // do the best to minimize the number of open streams.
            if (inStream != null) {
                inStream.close();
            }
        }
    }
    
    private static final class ObsoleteDCDataInputStream 
        extends LongUTFDataInputStream implements DataCellDataInput {

        /** Inherited constructor.
         * @param input Passed to super implementation.
         */
        public ObsoleteDCDataInputStream(final DataInputStream input) {
            super(input);
        }

        /** Throws always an exception as reading DataCells is not supported.
         * {@inheritDoc} */
        @Override
        public DataCell readDataCell() throws IOException {
            throw new IOException("The stream was written with a version that "
                    + "does not support reading/writing of encapsulated " 
                    + "DataCells");
        }
        
    }
}
