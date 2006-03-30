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
package de.unikn.knime.dev.data.container;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Output stream that allows to set block marks. It will scan the byte array on 
 * incoming write(byte[] .... ) invocations and escape the bytes if necessary. 
 *
 * <p>This class is used to mark the end of a <code>DataCell</code> in order
 * avoid stream corruption when a <code>DataCell</code> reads more (or less) 
 * than it has written.
 * @author wiswedel, University of Konstanz
 */
final class BlockableOutputStream extends OutputStream {

    /** The byte being used as block terminate.*/
    static final byte TC_TERMINATE = (byte)0x61;

    /** The byte being used to escape the next byte. The next byte will 
     * therefore neither be considered as terminate nor as escape byte.
     */
    static final byte TC_ESCAPE = (byte)0x62;
    
    /** The stream to write to. */
    private final OutputStream m_outStream;

    /** Constructor that simply memorizes the stream to write to.
     * @param outStream To write to, never <code>null</code>.
     */
    BlockableOutputStream(final OutputStream outStream) {
        m_outStream = outStream;
    }
    
    /**
     * Parses the byte[] argument, escapes the bytes if necessary and delegates
     * escaped byte array to underlying stream.
     * @see java.io.OutputStream#write(byte[], int, int)
     */
    @Override
    public synchronized void write(
            final byte[] b, final int off, final int len)
            throws IOException {
        int end = off;
        while (end < len) {
            int start = end;
            loop : for (int i = start; i < len; i++) {
                switch (b[i]) {
                case TC_TERMINATE:
                case TC_ESCAPE:
                    end = i;
                    break loop;
                default:
                // do nothing
                }
                end++;
            }
            int newLength = end - start;
            if (newLength > 0) {
                m_outStream.write(b, start, newLength);
            }
            if (end != len) {
                // escape current character and procceed
                escapeAndWrite(b[end++]);
            }
        }
    }
    
    /**
     * Calles <code>write(b, 0, b.length);</code>.
     * @see java.io.OutputStream#write(byte[])
     */
    @Override
    public void write(final byte[] b) throws IOException {
        write(b, 0, b.length);
    }
    
    /** private buffer that contains a escape byte and whatever needs to
     * be escaped. */
    private final byte[] m_buffer = new byte[2];

    /**
     * Checks if the byte to be written needs to be escaped and does so 
     * if necessary.
     * @see java.io.OutputStream#write(int)
     */
    @Override
    public synchronized void write(final int b) throws IOException {
        switch (b) {
        case TC_TERMINATE:
        case TC_ESCAPE:
            escapeAndWrite(b);
            break;
        default:
            m_outStream.write(b);
        }
    }
    
    /** Escapes the byte <code>b</code>. */
    private void escapeAndWrite(final int b) throws IOException {
        m_buffer[0] = TC_ESCAPE;
        m_buffer[1] = (byte)b;
        m_outStream.write(m_buffer, 0, 2);
    }
    
    /**
     * Delegates to output stream.
     * @see java.io.Closeable#close()
     */
    public void close() throws IOException {
        m_outStream.close();
    }

    /**
     * Delegates to output stream.
     * @see java.io.Flushable#flush()
     */
    public void flush() throws IOException {
        m_outStream.flush();
    }

    /** Writes a terminate character to the underlying stream.
     * @throws IOException If the write fails.
     */
    public void endBlock() throws IOException {
        m_outStream.write(TC_TERMINATE);
    }
}
