/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jul 29, 2020 (moritz): created
 */
package org.knime.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;

/**
 * Wrapper for {@link URLConnection#getInputStream()} and disconnects in case it is an {@link HttpURLConnection}.
 *
 * @author Moritz Heine, KNIME GmbH, Konstanz, Germany.
 */
final class WrappedURLInputStream extends InputStream {

    private final InputStream m_inputStream;

    private final URLConnection m_connection;

    /**
     * Creates a new {@link WrappedURLInputStream} that disconnects from the {@link URLConnection} as soon as the stream
     * is closed.
     *
     * @param connection the connection.
     * @throws IOException if an error occurs while creating the output stream.
     */
    WrappedURLInputStream(final URLConnection connection) throws IOException {
        m_connection = connection;

        try {
            m_inputStream = connection.getInputStream();
        } catch (IOException e) {
            if (m_connection instanceof HttpURLConnection) {
                ((HttpURLConnection)m_connection).disconnect();
            }

            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read() throws IOException {
        return m_inputStream.read();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(final byte[] b) throws IOException {
        return m_inputStream.read(b);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return m_inputStream.read(b, off, len);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int available() throws IOException {
        return m_inputStream.available();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void mark(final int readlimit) {
        m_inputStream.mark(readlimit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean markSupported() {
        return m_inputStream.markSupported();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void reset() throws IOException {
        m_inputStream.reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long skip(final long n) throws IOException {
        return m_inputStream.skip(n);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        m_inputStream.close();

        if (m_connection instanceof HttpURLConnection) {
            ((HttpURLConnection)m_connection).disconnect();
        }
    }
}
