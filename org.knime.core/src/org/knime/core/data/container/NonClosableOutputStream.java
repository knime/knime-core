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
package org.knime.core.data.container;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipOutputStream;


/** Class that overrides the close method and flushes the stream instead of
 * closing it.
 * @author Bernd Wiswedel, University of Konstanz 
 */
class NonClosableOutputStream extends OutputStream {
   
    private final OutputStream m_out;
    
    /** Inits object, references argument.
     * @param out The reference.
     */
    public NonClosableOutputStream(final OutputStream out) {
        m_out = out;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        m_out.flush();
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws IOException {
        m_out.flush();
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final byte[] b, final int off, final int len) 
        throws IOException {
        m_out.write(b, off, len);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final byte[] b) throws IOException {
        m_out.write(b);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final int b) throws IOException {
        m_out.write(b);
    }
    
    /** Get reference to underlying stream.
     * @return The delegated stream. */
    final OutputStream getUnderlyingStream() {
        return m_out;
    }
    
    /** Special implementation that wraps {@link ZipOutputStream} objects
     * and calls {@link ZipOutputStream#closeEntry()} when the stream is closed.
     */
    public static final class Zip extends NonClosableOutputStream {

        /** Wraps a given zip output stream.
         * @param zipOut The stream to wrap.
         */
        public Zip(final ZipOutputStream zipOut) {
            super(zipOut);
        }
        
        /** Closes the currently open zip entry.
         * {@inheritDoc} */
        @Override
        public void close() throws IOException {
            ((ZipOutputStream)getUnderlyingStream()).closeEntry();
        }
    }
}
