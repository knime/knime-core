 
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
