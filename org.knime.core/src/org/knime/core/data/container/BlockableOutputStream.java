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

import java.io.IOException;
import java.io.OutputStream;

/**
 * Output stream that allows to set block marks. It will scan the byte array on
 * incoming {@link #write(byte[])} or {@link #write(byte[], int, int)}
 * invocations and escape the bytes if necessary.
 * 
 * <p>
 * This class is used to mark the end of a
 * {@link org.knime.core.data.DataCell} in order avoid stream corruption
 * when a {@link org.knime.core.data.DataCell} reads more (or less) than it
 * has written.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
final class BlockableOutputStream 
extends OutputStream implements KNIMEStreamConstants {

    /** The stream to write to. */
    private final OutputStream m_outStream;

    /**
     * Constructor that simply memorizes the stream to write to.
     * 
     * @param outStream to write to, never <code>null</code>
     */
    BlockableOutputStream(final OutputStream outStream) {
        m_outStream = outStream;
    }

    /**
     * Parses the byte[] argument, escapes the bytes if necessary and delegates
     * escaped byte array to underlying stream.
     * {@inheritDoc}
     */
    @Override
    public synchronized void write(final byte[] b, final int off, final int len)
            throws IOException {
        int end = off;
        while (end < len) {
            int start = end;
            loop: for (int i = start; i < len; i++) {
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
                // escape current character and proceed
                escapeAndWrite(b[end++]);
            }
        }
    }

    /**
     * Calls {@link #write(byte[], int, int)}.
     * {@inheritDoc} 
     */
    @Override
    public void write(final byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    /*
     * private buffer that contains a escape byte and whatever needs to be
     * escaped.
     */
    private final byte[] m_buffer = new byte[2];

    /**
     * Checks if the byte to be written needs to be escaped and does so if
     * necessary.
     * {@inheritDoc} 
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

    /* Escapes the byte <code>b</code>. */
    private void escapeAndWrite(final int b) throws IOException {
        m_buffer[0] = TC_ESCAPE;
        m_buffer[1] = (byte)b;
        m_outStream.write(m_buffer, 0, 2);
    }

    /**
     * Delegates to output stream.
     * {@inheritDoc} 
     */
    @Override
    public void close() throws IOException {
        m_outStream.close();
    }

    /**
     * Delegates to output stream.
     * {@inheritDoc} 
     */
    @Override
    public void flush() throws IOException {
        m_outStream.flush();
    }

    /**
     * Writes a terminate character to the underlying stream.
     * 
     * @throws IOException if the write fails
     */
    public void endBlock() throws IOException {
        m_outStream.write(TC_TERMINATE);
    }
}
