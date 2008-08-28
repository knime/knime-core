 
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
 *   Mar 29, 2006 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.BlobDataCell.BlobAddress;
import org.knime.core.data.container.BufferFromFileIteratorVersion20.DataCellStreamReader;
import org.knime.core.data.util.NonClosableInputStream;


/**
 * Class interpreting the file format as written by the {@link Buffer} class.
 * It supports both modes of {@link DataCell} serialization, that is using
 * a {@link DataCellSerializer} and a plain java serialization.
 * 
 * <p>This class is the counterpart to {@link DCObjectOutputVersion2}, see 
 * details on the file format in there.
 * 
 * @author Bernd Wiswedel, University of Konstanz 
 */
final class DCObjectInputVersion2 implements KNIMEStreamConstants {

    /** Escapable stream, returns eof when block ends. Stream we read from. */
    private final BlockableInputStream m_in;

    /** Wrapped stream that is passed to the DataCellSerializer,
     * this stream reads from m_in. */
    private final DCLongUTFDataInputStream m_dataIn;
    
    /** Buffer used to deserialized contained DataCells (when DataCells are
     * wrapped in DataCells). */
    private final DataCellStreamReader m_cellReader;
    
    /** Preferred class loader that is set shortly before a java 
     * de-serialization takes place. May be null. */
    private ClassLoader m_priorityClassLoader;
    
    /**
     * Creates new input stream that reads from <code>in</code>.
     * @param in The stream to read from.
     * @param cellReader The object that helps to read DataCell contained in 
     * DataCell as required by {@link DataCellDataInput#readDataCell()}.
     */
    DCObjectInputVersion2(final InputStream in, 
            final DataCellStreamReader cellReader) {
        m_cellReader = cellReader;
        m_in = new BlockableInputStream(in);
        m_dataIn = new DCLongUTFDataInputStream(new DataInputStream(m_in));
    }
    
    /** Reads a data cell from the stream. 
     * @param serializer The factory that is used to create the cell
     * @return A new data cell instance.
     * @throws IOException If reading fails.
     * @see DataCellSerializer#deserialize(DataCellDataInput)
     */
    DataCell readDataCellPerKNIMESerializer(
            final DataCellSerializer<? extends DataCell> serializer)
        throws IOException {
        return serializer.deserialize(m_dataIn);
    }
    
    
    /** Reads a data cell from the stream using java de-serialization. 
     * @return A new data cell instance.
     * @throws IOException If reading fails (also e.g. 
     * {@link ClassCastException} are wrapped in such IO exceptions). 
     */
    DataCell readDataCellPerJavaSerialization() throws IOException {
        PriorityGlobalObjectInputStream gl = 
            new PriorityGlobalObjectInputStream(
                    new NonClosableInputStream(m_dataIn));
        gl.setCurrentClassLoader(m_priorityClassLoader);
        try {
            return (DataCell)gl.readObject();
        } catch (Exception exception) {
            throw new IOException("Unable to restore data cell (" 
                    + exception.getClass().getSimpleName() + ")" , exception);
        } finally {
            gl.close();
        }
    }
    
    /** Reads a blob address from the stream.
     * @return as read from the stream.
     * @throws IOException If that fails. */
    BlobAddress readBlobAddress() throws IOException {
        return BlobAddress.deserialize(m_dataIn);
    }
    
    /** Reads a row key from the stream.
     * @return A new row key instance.
     * @throws IOException If IO problems occur.
     */
    RowKey readRowKey() throws IOException {
        return new RowKey(m_dataIn.readUTF());
    }
    
    /** Reads a single byte from the stream.
     * @return That byte.
     * @throws IOException If IO problems occur. */
    byte readControlByte() throws IOException {
        return m_dataIn.readByte();
    }
    
    /** Pushes the stream forth until a mark is encountered.
     * @throws IOException If IO problems occur. */
    void endBlock() throws IOException {
        m_in.endBlock();
    }
    
    /** Set the class loader to ask "first" to load classes. Used when 
     * a data cell is deserialized and all its member should be loaded in the
     * context of that class loader.
     * @param l The class loader to use, if <code>null</code> it uses the 
     * globally known class loader (GlobalClassCreator)
     */
    void setCurrentClassLoader(final ClassLoader l) {
        m_priorityClassLoader = l;
    }
    
    /** Closes the underlying streams.
     * @throws IOException If underlying streams fail to close.
     */
    void close() throws IOException {
        m_dataIn.close();
    }
    
    /** Data input stream with functionality to read encapsulated DataCell
     * objects. */
    private final class DCLongUTFDataInputStream 
        extends LongUTFDataInputStream implements DataCellDataInput {

        /** Inherited constructor.
         * @param input Passed to super implementation.
         */
        public DCLongUTFDataInputStream(final DataInputStream input) {
            super(input);
        }

        /** Throws always an exception as reading DataCells is not supported.
         * {@inheritDoc} */
        @Override
        public DataCell readDataCell() throws IOException {
            return m_cellReader.readDataCell(DCObjectInputVersion2.this);
        }
        
    }
    
}
