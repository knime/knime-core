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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.container.BlobDataCell.BlobAddress;


/**
 * <code>ObjectOutputStream</code> that is used to serialize or write 
 * <code>DataCell</code> objects to a stream. <code>DataCell</code> objects
 * whose type does not support customized writing will be serialized using
 * java serialization. If the type supports customized writing/reading
 * (meaning that the type implements <code>DataCellSerializer</code> are saved
 * using the <code>writeDataCell</code> method. It will use the type to write
 * a customized stream.
 * 
 * <p>This class uses an internal <code>ObjectOutputStream</code> to which it
 * delegates all incoming write requests (like for instance the writeObject()
 * method). If a <code>DataCellSerializer</code> writes to this stream it uses
 * an internal blockable stream to mark the end of the DataCell. This internal
 * stream - in the end - writes also to the underlying 
 * <code>ObjectOutputStream</code>. The following figures summarizes the 
 * dependencies.
 * 
 * <p>
 * <center>
 *   <img src="doc-files/objectoutput.png" alt="Streams" align="middle">
 * </center>
 * @author wiswedel, University of Konstanz
 */
class DCObjectOutputStream extends ObjectOutputStream {
    
    /** The stream as passed in the constructor, m_objectOut writes to it.*/
    private final OutputStream m_underylingOut;
    /** Underlying stream to write to. */
    private final ObjectOutputStream m_objectOut;
    /** Used to mark the end of a DataCell when written using the 
     * writeDataCell method. It will write to m_objectOut */
    private final BlockableOutputStream m_out;
    /** This stream writes to m_out and is passed to the DataCellSerializer. */
    private final LongUTFDataOutputStream m_dataOut;

    
    /**
     * Constructs new output stream, inits all internal writers.
     * @param out The stream to write to.
     * @throws IOException If the stream can't be written.
     */
    public DCObjectOutputStream(final OutputStream out) throws IOException {
        m_underylingOut = out;
        m_objectOut = new ObjectOutputStream(m_underylingOut);
        m_out = new BlockableOutputStream(m_objectOut);
        m_dataOut = new LongUTFDataOutputStream(new DataOutputStream(m_out));
    }

    /** Get reference to underlying output stream. Remember to flush this
     * stream before you do nasty things with the underlying stream!
     * @return The underyling stream as passed in the constructor.
     */
    OutputStream getUnderylingStream() {
        return m_underylingOut;
    }
    
    /** Writes a data cell using the serializer. It will write one block.
     * @param serializer The factory being used to write the cell.
     * @param cell The cell to be written.
     * @throws IOException If that fails.
     */
    public void writeDataCell(final DataCellSerializer<DataCell> serializer, 
            final DataCell cell) throws IOException {
        try {
            serializer.serialize(cell, m_dataOut);
        } finally {
            m_out.endBlock();
        }
    }
    
    /** Writes a given blob address to the stream and marks the end by a 
     * block character.
     * @param address to write out.
     * @throws IOException if that fails
     */
    public void writeBlobAddress(final BlobAddress address) throws IOException {
        try {
            address.serialize(m_dataOut);
        } finally {
            m_out.endBlock();
        }
    }
    
    /* The following methods all delegate to the internal ObjectOutputStream */
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        m_objectOut.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void defaultWriteObject() throws IOException {
        m_objectOut.defaultWriteObject();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws IOException {
        m_objectOut.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PutField putFields() throws IOException {
        return m_objectOut.putFields();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() throws IOException {
        m_objectOut.reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_objectOut.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void useProtocolVersion(final int version) throws IOException {
        m_objectOut.useProtocolVersion(version);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final byte[] buf, final int off, final int len) 
        throws IOException {
        m_objectOut.write(buf, off, len);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final byte[] buf) throws IOException {
        m_objectOut.write(buf);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final int val) throws IOException {
        m_objectOut.write(val);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBoolean(final boolean val) throws IOException {
        m_objectOut.writeBoolean(val);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeByte(final int val) throws IOException {
        m_objectOut.writeByte(val);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeBytes(final String str) throws IOException {
        m_objectOut.writeBytes(str);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChar(final int val) throws IOException {
        m_objectOut.writeChar(val);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChars(final String str) throws IOException {
        m_objectOut.writeChars(str);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeDouble(final double val) throws IOException {
        m_objectOut.writeDouble(val);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeFields() throws IOException {
        m_objectOut.writeFields();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeFloat(final float val) throws IOException {
        m_objectOut.writeFloat(val);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeInt(final int val) throws IOException {
        m_objectOut.writeInt(val);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeLong(final long val) throws IOException {
        m_objectOut.writeLong(val);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeObjectOverride(final Object obj) throws IOException {
        m_objectOut.writeObject(obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeShort(final int val) throws IOException {
        m_objectOut.writeShort(val);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeUnshared(final Object obj) throws IOException {
        m_objectOut.writeUnshared(obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeUTF(final String str) throws IOException {
        m_objectOut.writeUTF(str);
    }
}
