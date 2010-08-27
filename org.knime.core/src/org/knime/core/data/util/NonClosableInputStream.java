/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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

        /** Wraps a given zip input stream.
         * @param zipIn The stream to wrap.
         */
        public Zip(final ZipInputStream zipIn) {
            super(zipIn);
        }
        
        /** Closes the currently open zip entry.
         * {@inheritDoc} */
        @Override
        public void close() throws IOException {
            ((ZipInputStream)getUnderlyingStream()).closeEntry();
        }
    }

}
