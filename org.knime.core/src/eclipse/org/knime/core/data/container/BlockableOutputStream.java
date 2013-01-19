/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
