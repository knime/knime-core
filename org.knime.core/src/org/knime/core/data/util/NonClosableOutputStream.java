/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
import java.io.OutputStream;
import java.util.zip.ZipOutputStream;


/** Class that overrides the close method and flushes the stream instead of
 * closing it.
 * @author Bernd Wiswedel, University of Konstanz 
 */
public class NonClosableOutputStream extends OutputStream {
   
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
