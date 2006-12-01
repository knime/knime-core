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
import java.io.OutputStream;
import java.util.zip.ZipOutputStream;


/** Class that overrides the close method and just calls closeEntry on the
 * reference zip output. All other methods delegate directly.
 */
class NonClosableZipOutputStream extends OutputStream {
    private final ZipOutputStream m_zipOut;
    
    /** Inits object, references argument.
     * @param zipOut The reference.
     */
    public NonClosableZipOutputStream(final ZipOutputStream zipOut) {
        m_zipOut = zipOut;
    }
    /**
     * @see java.io.OutputStream#close()
     */
    @Override
    public void close() throws IOException {
        m_zipOut.closeEntry();
    }
    /**
     * @see java.io.OutputStream#flush()
     */
    @Override
    public void flush() throws IOException {
        m_zipOut.flush();
    }
    /**
     * @see java.io.OutputStream#write(byte[], int, int)
     */
    @Override
    public void write(final byte[] b, final int off, final int len) 
        throws IOException {
        m_zipOut.write(b, off, len);
    }
    /**
     * @see java.io.OutputStream#write(byte[])
     */
    @Override
    public void write(final byte[] b) throws IOException {
        m_zipOut.write(b);
    }
    /**
     * @see java.io.OutputStream#write(int)
     */
    @Override
    public void write(final int b) throws IOException {
        m_zipOut.write(b);
    }
}
