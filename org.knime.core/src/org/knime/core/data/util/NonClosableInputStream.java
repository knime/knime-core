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
 *   Nov 15, 2006 (wiswedel): created
 */
package org.knime.core.data.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

/**
 * InputStream that delegates to an underlying {@link InputStream} but ignores
 * calls of {@link #close()}.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class NonClosableInputStream extends InputStream {

    private final InputStream m_in;
    
    /**
     * Creates new input stream.
     * @param inStream To wrap.
     * @throws NullPointerException If the argument is null.
     */
    public NonClosableInputStream(final InputStream inStream) {
        if (inStream == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_in = inStream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int available() throws IOException {
        return m_in.available();
    }

    /** Does NOT delegate to wrapped input stream, ignores call.
    * {@inheritDoc} */
    @Override
    public void close() throws IOException {
    }

    /** {@inheritDoc} */
    @Override
    public void mark(final int readlimit) {
        m_in.mark(readlimit);
    }

    /** {@inheritDoc} */
    @Override
    public boolean markSupported() {
        return m_in.markSupported();
    }

    /** {@inheritDoc} */
    @Override
    public int read() throws IOException {
        return m_in.read();
    }

    /** {@inheritDoc} */
    @Override
    public int read(final byte[] b, final int off, final int len) 
        throws IOException {
        return m_in.read(b, off, len);
    }

    /** {@inheritDoc} */
    @Override
    public int read(final byte[] b) throws IOException {
        return m_in.read(b);
    }

    /** {@inheritDoc} */
    @Override
    public void reset() throws IOException {
        m_in.reset();
    }

    /** {@inheritDoc} */
    @Override
    public long skip(final long n) throws IOException {
        return m_in.skip(n);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return m_in.toString();
    }
    
    /** Get reference to underlying stream.
     * @return The delegated stream. */
    public final InputStream getUnderlyingStream() {
        return m_in;
    }
    
    /** Special implementation that wraps {@link ZipInputStream} objects
     * and calls {@link ZipInputStream#closeEntry()} when the stream is closed.
     */
    public static final class Zip extends NonClosableInputStream {

        /** Wraps a given zip output stream.
         * @param zipOut The stream to wrap.
         */
        public Zip(final ZipInputStream zipOut) {
            super(zipOut);
        }
        
        /** Closes the currently open zip entry.
         * {@inheritDoc} */
        @Override
        public void close() throws IOException {
            ((ZipInputStream)getUnderlyingStream()).closeEntry();
        }
    }

}
