/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   Aug 31, 2006 (wiswedel): created
 */
package org.knime.core.data.container;

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Input Stream that's used to read from stream written by
 * <code>LongUTFDataOutputStream</code>.
 *
 * @see LongUTFDataOutputStream
 * @author wiswedel, University of Konstanz
 */
public class LongUTFDataInputStream extends InputStream
    implements DataInput, Closeable {

    private final DataInputStream m_inputStream;

    private final LongUTFDataInput m_input;

    /**
     * Constructor that wraps a given input stream.
     * @param input The input stream to read from.
     */
    public LongUTFDataInputStream(final DataInputStream input) {
        if (input == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_inputStream = input;
        m_input = new LongUTFDataInput(m_inputStream);
    }

    /** {@inheritDoc} */
    @Override
    public int available() throws IOException {
        return m_inputStream.available();
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        m_inputStream.close();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return m_input.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public void mark(final int readlimit) {
        m_inputStream.mark(readlimit);
    }

    /** {@inheritDoc} */
    @Override
    public boolean markSupported() {
        return m_inputStream.markSupported();
    }

    /** {@inheritDoc} */
    @Override
    public int read() throws IOException {
        return m_inputStream.read();
    }

    /** {@inheritDoc} */
    @Override
    public int read(
            final byte[] b, final int off, final int len) throws IOException {
        return m_inputStream.read(b, off, len);
    }

    /** {@inheritDoc} */
    @Override
    public int read(final byte[] b) throws IOException {
        return m_inputStream.read(b);
    }

    /** {@inheritDoc} */
    @Override
    public boolean readBoolean() throws IOException {
        return m_input.readBoolean();
    }

    /** {@inheritDoc} */
    @Override
    public byte readByte() throws IOException {
        return m_input.readByte();
    }

    /** {@inheritDoc} */
    @Override
    public char readChar() throws IOException {
        return m_input.readChar();
    }

    /** {@inheritDoc} */
    @Override
    public double readDouble() throws IOException {
        return m_input.readDouble();
    }

    /** {@inheritDoc} */
    @Override
    public float readFloat() throws IOException {
        return m_input.readFloat();
    }

    /** {@inheritDoc} */
    @Override
    public void readFully(
            final byte[] b, final int off, final int len) throws IOException {
        m_input.readFully(b, off, len);
    }

    /** {@inheritDoc} */
    @Override
    public void readFully(final byte[] b) throws IOException {
        m_input.readFully(b);
    }

    /** {@inheritDoc} */
    @Override
    public int readInt() throws IOException {
        return m_input.readInt();
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public String readLine() throws IOException {
        return m_input.readLine();
    }

    /** {@inheritDoc} */
    @Override
    public long readLong() throws IOException {
        return m_input.readLong();
    }

    /** {@inheritDoc} */
    @Override
    public short readShort() throws IOException {
        return m_input.readShort();
    }

    /** {@inheritDoc} */
    @Override
    public int readUnsignedByte() throws IOException {
        return m_input.readUnsignedByte();
    }

    /** {@inheritDoc} */
    @Override
    public int readUnsignedShort() throws IOException {
        return m_input.readUnsignedShort();
    }

    /** {@inheritDoc} */
    @Override
    public void reset() throws IOException {
        m_inputStream.reset();
    }

    /** {@inheritDoc} */
    @Override
    public long skip(final long n) throws IOException {
        return m_inputStream.skip(n);
    }

    /** {@inheritDoc} */
    @Override
    public int skipBytes(final int n) throws IOException {
        return m_input.skipBytes(n);
    }

    /** {@inheritDoc} */
    @Override
    public String readUTF() throws IOException {
        return m_input.readUTF();
    }
}
