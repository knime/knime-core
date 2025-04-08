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
 *   11 Nov 2022 (chaubold): created
 */
package org.knime.core.data.v2.filestore;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.knime.core.data.container.LongUTFDataOutputStream;
import org.knime.core.table.io.ReadableDataInput;
import org.knime.core.table.util.StringEncoder;

/**
 * Combination of ReadableDataInput with unmodified UTF serialization so it can be read from Arrow tables by non-Java
 * readers
 *
 * In contrast to the {@link LongUTFDataOutputStream}, this UTF representation does not diverge from the standard.
 *
 * Also, this stream allows to define the endianness in which the data is read, which defaults to
 * {@link ByteOrder#LITTLE_ENDIAN}.
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @since 5.1
 */
public final class UnmodifiedLongUTFReadableDataInput implements ReadableDataInput {

    private final ReadableDataInput m_input;

    private final ByteOrder m_byteOrder;

    public UnmodifiedLongUTFReadableDataInput(final ReadableDataInput input, final ByteOrder order) {
        m_input = input;
        m_byteOrder = order;
    }

    public UnmodifiedLongUTFReadableDataInput(final ReadableDataInput input) {
        this(input, ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public void readFully(final byte[] b) throws IOException {
        m_input.readFully(b);
    }

    @Override
    public void readFully(final byte[] b, final int off, final int len) throws IOException {
        m_input.readFully(b, off, len);
    }

    @Override
    public int skipBytes(final int n) throws IOException {
        return m_input.skipBytes(n);
    }

    @Override
    public boolean readBoolean() throws IOException {
        return m_input.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return m_input.readByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return m_input.readUnsignedByte();
    }

    @Override
    public short readShort() throws IOException {
        return readByteBuf(new byte[Short.BYTES]).getShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        var bytes = new byte[Integer.BYTES];
        if (m_byteOrder == ByteOrder.LITTLE_ENDIAN) {
            m_input.read(bytes, 2, 2);
        } else {
            m_input.read(bytes, 0, 2);
        }
        var buffer = ByteBuffer.wrap(bytes);
        buffer.order(m_byteOrder);
        return buffer.getInt();
    }

    @Override
    public char readChar() throws IOException {
        return readByteBuf(new byte[Character.BYTES]).getChar();
    }

    @Override
    public int readInt() throws IOException {
        return readByteBuf(new byte[Integer.BYTES]).getInt();
    }

    @Override
    public long readLong() throws IOException {
        return readByteBuf(new byte[Long.BYTES]).getLong();
    }

    @Override
    public float readFloat() throws IOException {
        return readByteBuf(new byte[Float.BYTES]).getFloat();
    }

    private ByteBuffer readByteBuf(final byte[] bytes) throws IOException {
        m_input.readFully(bytes);
        var buffer = ByteBuffer.wrap(bytes);
        buffer.order(m_byteOrder);
        return buffer;
    }

    @Override
    public double readDouble() throws IOException {
        return readByteBuf(new byte[Double.BYTES]).getDouble();
    }

    @Override
    public String readLine() throws IOException {
        return m_input.readLine();
    }

    @Override
    public String readUTF() throws IOException {
        var length = readLong();
        if (length > Integer.MAX_VALUE) {
            throw new IOException("Found a UTF string longer than 2GB, cannot read that yet");
            // TODO: allow string lengths > MAX_INTEGER https://knime-com.atlassian.net/browse/AP-19712
        }
        var bytes = new byte[(int)length];
        m_input.readFully(bytes);
        return StringEncoder.decode(bytes);
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return m_input.read(b, off, len);
    }

    @Override
    public byte[] readBytes() throws IOException {
        return m_input.readBytes();
    }
}
