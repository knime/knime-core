/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   Mar 29, 2006 (wiswedel): created
 */
package de.unikn.knime.base.data.container;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataCellSerializer;

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
    
    /** Underlying stream to write to. */
    private final ObjectOutputStream m_objectOut;
    /** Used to mark the end of a DataCell when written using the 
     * writeDataCell method. It will write to m_objectOut */
    private final BlockableOutputStream m_out;
    /** This stream writes to m_out and is passed to the DataCellSerializer. */
    private final DataOutputStream m_dataOut;

    
    /**
     * Constructs new output stream, inits all internal writers.
     * @param out The stream to write to.
     * @throws IOException If the stream can't be written.
     */
    public DCObjectOutputStream(final OutputStream out) throws IOException {
        m_objectOut = new ObjectOutputStream(out);
        m_out = new BlockableOutputStream(m_objectOut);
        m_dataOut = new DataOutputStream(m_out);
    }
    
    /** Writes a data cell using the serializer. It will write one block.
     * @param serializer The factory being used to write the cell.
     * @param cell The cell to be written.
     * @throws IOException If that fails.
     */
    public void writeDataCell(final DataCellSerializer serializer, 
            final DataCell cell) throws IOException {
        try {
            serializer.serialize(cell, m_dataOut);
        } finally {
            m_out.endBlock();
        }
    }

    
    /* The following methods all delegate to the internal ObjectOutputStream */
    
    /**
     * @see ObjectOutputStream#close()
     */
    public void close() throws IOException {
        m_objectOut.close();
    }

    /**
     * @see ObjectOutputStream#defaultWriteObject()
     */
    public void defaultWriteObject() throws IOException {
        m_objectOut.defaultWriteObject();
    }

    /**
     * @see ObjectOutputStream#flush()
     */
    public void flush() throws IOException {
        m_objectOut.flush();
    }

    /**
     * @see ObjectOutputStream#putFields()
     */
    public PutField putFields() throws IOException {
        return m_objectOut.putFields();
    }

    /**
     * @see ObjectOutputStream#reset()
     */
    public void reset() throws IOException {
        m_objectOut.reset();
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return m_objectOut.toString();
    }

    /**
     * @see ObjectOutputStream#useProtocolVersion(int)
     */
    public void useProtocolVersion(final int version) throws IOException {
        m_objectOut.useProtocolVersion(version);
    }

    /**
     * @see ObjectOutputStream#write(byte[], int, int)
     */
    public void write(final byte[] buf, final int off, final int len) 
        throws IOException {
        m_objectOut.write(buf, off, len);
    }

    /**
     * @see ObjectOutputStream#write(byte[])
     */
    public void write(final byte[] buf) throws IOException {
        m_objectOut.write(buf);
    }

    /**
     * @see ObjectOutputStream#write(int)
     */
    public void write(final int val) throws IOException {
        m_objectOut.write(val);
    }

    /**
     * @see ObjectOutputStream#writeBoolean(boolean)
     */
    public void writeBoolean(final boolean val) throws IOException {
        m_objectOut.writeBoolean(val);
    }

    /**
     * @see ObjectOutputStream#writeByte(int)
     */
    public void writeByte(final int val) throws IOException {
        m_objectOut.writeByte(val);
    }

    /**
     * @see ObjectOutputStream#writeBytes(java.lang.String)
     */
    public void writeBytes(final String str) throws IOException {
        m_objectOut.writeBytes(str);
    }

    /**
     * @see ObjectOutputStream#writeChar(int)
     */
    public void writeChar(final int val) throws IOException {
        m_objectOut.writeChar(val);
    }

    /**
     * @see ObjectOutputStream#writeChars(java.lang.String)
     */
    public void writeChars(final String str) throws IOException {
        m_objectOut.writeChars(str);
    }

    /**
     * @see ObjectOutputStream#writeDouble(double)
     */
    public void writeDouble(final double val) throws IOException {
        m_objectOut.writeDouble(val);
    }

    /**
     * @see ObjectOutputStream#writeFields()
     */
    public void writeFields() throws IOException {
        m_objectOut.writeFields();
    }

    /**
     * @see ObjectOutputStream#writeFloat(float)
     */
    public void writeFloat(final float val) throws IOException {
        m_objectOut.writeFloat(val);
    }

    /**
     * @see ObjectOutputStream#writeInt(int)
     */
    public void writeInt(final int val) throws IOException {
        m_objectOut.writeInt(val);
    }

    /**
     * @see ObjectOutputStream#writeLong(long)
     */
    public void writeLong(final long val) throws IOException {
        m_objectOut.writeLong(val);
    }

    /**
     * @see ObjectOutputStream#writeObjectOverride(Object)
     */
    public void writeObjectOverride(final Object obj) throws IOException {
        m_objectOut.writeObject(obj);
    }

    /**
     * @see ObjectOutputStream#writeShort(int)
     */
    public void writeShort(final int val) throws IOException {
        m_objectOut.writeShort(val);
    }

    /**
     * @see ObjectOutputStream#writeUnshared(java.lang.Object)
     */
    public void writeUnshared(final Object obj) throws IOException {
        m_objectOut.writeUnshared(obj);
    }

    /**
     * @see ObjectOutputStream#writeUTF(java.lang.String)
     */
    public void writeUTF(final String str) throws IOException {
        m_objectOut.writeUTF(str);
    }


}
