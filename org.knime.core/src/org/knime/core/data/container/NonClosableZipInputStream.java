/* 
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
 *   Nov 15, 2006 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * InputStream that delegates to an underlying {@link ZipInputStream} but
 * only closes the current entry on {@link #close()}.
 * @author Bernd Wiswedel, University of Konstanz
 */
class NonClosableZipInputStream extends InputStream {

    private final ZipInputStream m_zipIn;
    
    /**
     * Creates new input stream.
     * @param inStream To wrap.
     * @throws NullPointerException If the argument is null.
     */
    public NonClosableZipInputStream(final ZipInputStream inStream) {
        if (inStream == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_zipIn = inStream;
    }

    /**
     * @see InputStream#available()
     */
    @Override
    public int available() throws IOException {
        return m_zipIn.available();
    }

    /**
     * Does NOT delegate to wrapped zip input stream but rather calls
     * closeEntry().
     * @see ZipInputStream#closeEntry()
     */
    @Override
    public void close() throws IOException {
        m_zipIn.closeEntry();
    }

    /**
     * @see ZipInputStream#closeEntry()
     * @throws IOException Delegates only.
     */
    public void closeEntry() throws IOException {
        m_zipIn.closeEntry();
    }

    /**
     * @return Delegates only.
     * @see ZipInputStream#getNextEntry()
     * @throws IOException Delegates only.
     */
    public ZipEntry getNextEntry() throws IOException {
        return m_zipIn.getNextEntry();
    }

    /**
     * @see InputStream#mark(int)
     */
    @Override
    public void mark(final int readlimit) {
        m_zipIn.mark(readlimit);
    }

    /**
     * @see InputStream#markSupported()
     */
    @Override
    public boolean markSupported() {
        return m_zipIn.markSupported();
    }

    /**
     * @see InputStream#read()
     */
    @Override
    public int read() throws IOException {
        return m_zipIn.read();
    }

    /**
     * @see InputStream#read(byte[], int, int)
     */
    @Override
    public int read(final byte[] b, final int off, final int len) 
        throws IOException {
        return m_zipIn.read(b, off, len);
    }

    /**
     * @see InputStream#read(byte[])
     */
    @Override
    public int read(final byte[] b) throws IOException {
        return m_zipIn.read(b);
    }

    /**
     * @see InputStream#reset()
     */
    @Override
    public void reset() throws IOException {
        m_zipIn.reset();
    }

    /**
     * @see InputStream#skip(long)
     */
    @Override
    public long skip(final long n) throws IOException {
        return m_zipIn.skip(n);
    }

    /**
     * @return Delegates only.
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return m_zipIn.toString();
    }

}
