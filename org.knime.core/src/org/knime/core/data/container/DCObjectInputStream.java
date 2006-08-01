/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
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
import java.io.InvalidObjectException;
import java.io.NotActiveException;
import java.io.ObjectInputStream;
import java.io.ObjectInputValidation;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.eclipseUtil.GlobalObjectInputStream;


/**
 * Input stream that is used by the Buffer to read (java.io.-)serialized  
 * <code>DataCell</code> (the ones whose type does not support customized 
 * reading/writing) and also <code>DataCell</code> objects that have been
 * written using a <code>DataCellSerializer</code>. The class extends 
 * ObjectInputStream but delegates incoming <code>readObject()</code> requests
 * to a private <code>ObjectInputStream</code>. 
 * 
 * <p>Reading <code>DataCell</code> using a <code>DataCellSerializer</code>
 * is done using the <code>readDataCell()</code> method. It will use another
 * input stream that delegates itself to the private ObjectInputStream but 
 * uses blocks to determine the end of a <code>DataCell</code>. An attempt to
 * summarize the different streams is made in the following figure (for the 
 * output stream though).
 * 
 * <p>
 * <center>
 *   <img src="doc-files/objectoutput.png" alt="Streams" align="middle">
 * </center>
 */
final class DCObjectInputStream extends ObjectInputStream {

    /** The streams that is being written to. */
    private final GlobalObjectInputStream m_inObject;
    /** Wrapped stream that is passed to the DataCellSerializer,
     * this stream reads from m_in. */
    private final DataInputStream m_dataInStream;
    /** Escapable stream, returns eof when block ends. */
    private final BlockableInputStream m_in;
    
    /**
     * Creates new input stream that reads from <code>in</code>.
     * @param in The stream to read from.
     * @throws IOException If the init of the stream reading fails.
     */
    DCObjectInputStream(final InputStream in) throws IOException {
        m_inObject = new GlobalObjectInputStream(in);
        m_in = new BlockableInputStream(m_inObject);
        m_dataInStream = new DataInputStream(m_in);
    }
    
    /** Reads a data cell from the stream and pushes the stream forward to 
     * the end of the block.
     * @param serializer The factory that is used to create the cell
     * @return A new data cell instance.
     * @throws IOException If reading fails.
     * @see DataCellSerializer#deserialize(java.io.DataInput)
     */
    public DataCell readDataCell(
            final DataCellSerializer serializer) throws IOException {
        try {
            return serializer.deserialize(m_dataInStream);
        } finally {
            m_in.endBlock();
        }
    }
    
    /* The following methods all delegate to the underlying m_inObject stream.
     */

    /**
     * @see java.io.ObjectInputStream#available()
     */
    @Override
    public int available() throws IOException {
        return m_inObject.available();
    }

    /**
     * @see java.io.ObjectInputStream#close()
     */
    @Override
    public void close() throws IOException {
        m_inObject.close();
    }

    /**
     * @see java.io.ObjectInputStream#defaultReadObject()
     */
    @Override
    public void defaultReadObject() throws IOException, ClassNotFoundException {
        m_inObject.defaultReadObject();
    }

    /**
     * @see java.io.InputStream#mark(int)
     */
    @Override
    public void mark(final int readlimit) {
        m_inObject.mark(readlimit);
    }

    /**
     * @see java.io.InputStream#markSupported()
     */
    @Override
    public boolean markSupported() {
        return m_inObject.markSupported();
    }

    /**
     * @see java.io.ObjectInputStream#read()
     */
    @Override
    public int read() throws IOException {
        return m_inObject.read();
    }

    /**
     * @see java.io.ObjectInputStream#read(byte[], int, int)
     */
    @Override
    public int read(final byte[] buf, final int off, final int len) 
        throws IOException {
        return m_inObject.read(buf, off, len);
    }

    /**
     * @see java.io.InputStream#read(byte[])
     */
    @Override
    public int read(final byte[] b) throws IOException {
        return m_inObject.read(b);
    }

    /**
     * @see java.io.ObjectInputStream#readBoolean()
     */
    @Override
    public boolean readBoolean() throws IOException {
        return m_inObject.readBoolean();
    }

    /**
     * @see java.io.ObjectInputStream#readByte()
     */
    @Override
    public byte readByte() throws IOException {
        return m_inObject.readByte();
    }

    /**
     * @see java.io.ObjectInputStream#readChar()
     */
    @Override
    public char readChar() throws IOException {
        return m_inObject.readChar();
    }

    /**
     * @see java.io.ObjectInputStream#readDouble()
     */
    @Override
    public double readDouble() throws IOException {
        return m_inObject.readDouble();
    }

    /**
     * @see java.io.ObjectInputStream#readFields()
     */
    @Override
    public GetField readFields() throws IOException, ClassNotFoundException {
        return m_inObject.readFields();
    }

    /**
     * @see java.io.ObjectInputStream#readFloat()
     */
    @Override
    public float readFloat() throws IOException {
        return m_inObject.readFloat();
    }

    /**
     * @see java.io.ObjectInputStream#readFully(byte[], int, int)
     */
    @Override
    public void readFully(final byte[] buf, final int off, final int len)
        throws IOException {
        m_inObject.readFully(buf, off, len);
    }

    /**
     * @see java.io.ObjectInputStream#readFully(byte[])
     */
    @Override
    public void readFully(final byte[] buf) throws IOException {
        m_inObject.readFully(buf);
    }

    /**
     * @see java.io.ObjectInputStream#readInt()
     */
    @Override
    public int readInt() throws IOException {
        return m_inObject.readInt();
    }

    /**
     * @see java.io.ObjectInputStream#readLong()
     */
    @Override
    public long readLong() throws IOException {
        return m_inObject.readLong();
    }

    /**
     * @see java.io.ObjectInput#readObject()
     */
    @Override
    protected Object readObjectOverride() 
        throws IOException, ClassNotFoundException {
        return m_inObject.readObject();
    }
    /**
     * @see java.io.ObjectInputStream#readShort()
     */
    @Override
    public short readShort() throws IOException {
        return m_inObject.readShort();
    }

    /**
     * @see java.io.ObjectInputStream#readUnshared()
     */
    @Override
    public Object readUnshared() throws IOException, ClassNotFoundException {
        return m_inObject.readUnshared();
    }

    /**
     * @see java.io.ObjectInputStream#readUnsignedByte()
     */
    @Override
    public int readUnsignedByte() throws IOException {
        return m_inObject.readUnsignedByte();
    }

    /**
     * @see java.io.ObjectInputStream#readUnsignedShort()
     */
    @Override
    public int readUnsignedShort() throws IOException {
        return m_inObject.readUnsignedShort();
    }

    /**
     * @see java.io.ObjectInputStream#readUTF()
     */
    @Override
    public String readUTF() throws IOException {
        return m_inObject.readUTF();
    }

    /**
     * @see ObjectInputStream#registerValidation(ObjectInputValidation, int)
     */
    @Override
    public void registerValidation(
            final ObjectInputValidation obj, final int prio) 
        throws NotActiveException, InvalidObjectException {
        m_inObject.registerValidation(obj, prio);
    }

    /**
     * @see java.io.InputStream#reset()
     */
    @Override
    public void reset() throws IOException {
        m_inObject.reset();
    }

    /**
     * @see java.io.InputStream#skip(long)
     */
    @Override
    public long skip(final long n) throws IOException {
        return m_inObject.skip(n);
    }

    /**
     * @see java.io.ObjectInputStream#skipBytes(int)
     */
    @Override
    public int skipBytes(final int len) throws IOException {
        return m_inObject.skipBytes(len);
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return m_inObject.toString();
    }
}
