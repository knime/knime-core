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

import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Consumer;

import org.knime.core.data.container.LongUTFDataOutputStream;
import org.knime.core.table.util.StringEncoder;

/**
 * Combination of {@link DataOutput} with unmodified UTF serialization so it can be read from Arrow tables by non-Java
 * readers.
 *
 * In contrast to the {@link LongUTFDataOutputStream}, this UTF representation does not diverge from the standard.
 *
 * Also, this stream allows to define the endianness in which the data is written, which defaults to
 * {@link ByteOrder#LITTLE_ENDIAN}.
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @since 5.1
 */
public final class UnmodifiedLongUTFDataOutput implements DataOutput {
    private final DataOutput m_output;

    private final ByteOrder m_byteOrder;

    private ByteBuffer m_byteBuffer;

    public UnmodifiedLongUTFDataOutput(final DataOutput output, final ByteOrder order) {
        m_output = output;
        m_byteOrder = order;
    }

    public UnmodifiedLongUTFDataOutput(final DataOutput output) {
        this(output, ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public void write(final int b) throws IOException {
        m_output.write(b);
    }

    @Override
    public void write(final byte[] b) throws IOException {
        m_output.write(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        m_output.write(b, off, len);
    }

    @Override
    public void writeBoolean(final boolean v) throws IOException {
        m_output.writeBoolean(v);
    }

    @Override
    public void writeByte(final int v) throws IOException {
        m_output.writeByte(v);
    }

    @Override
    public void writeShort(final int v) throws IOException {
        writeByteBuf(Short.BYTES, b -> b.putShort((short)v));
    }

    @Override
    public void writeChar(final int v) throws IOException {
        writeByteBuf(Character.BYTES, b -> b.putChar((char)v));
    }

    @Override
    public void writeInt(final int v) throws IOException {
        writeByteBuf(Integer.BYTES, b -> b.putInt(v));
    }

    @Override
    public void writeLong(final long v) throws IOException {
        writeByteBuf(Long.BYTES, b -> b.putLong(v));
    }

    @Override
    public void writeFloat(final float v) throws IOException {
        writeByteBuf(Float.BYTES, b -> b.putFloat(v));
    }

    private void writeByteBuf(final int capacity, final Consumer<ByteBuffer> writeMethod) throws IOException {
        if (m_byteBuffer == null) {
            m_byteBuffer = ByteBuffer.allocate(8); // max bytes for all our use cases is 8
            m_byteBuffer.order(m_byteOrder);
        } else {
            m_byteBuffer.rewind();
        }
        writeMethod.accept(m_byteBuffer);
        m_output.write(m_byteBuffer.array(), 0, capacity);
    }

    @Override
    public void writeDouble(final double v) throws IOException {
        writeByteBuf(Double.BYTES, b -> b.putDouble(v));
    }

    @Override
    public void writeBytes(final String s) throws IOException {
        m_output.writeBytes(s);
    }

    @Override
    public void writeChars(final String s) throws IOException {
        int len = s.length();
        var b = ByteBuffer.allocate(len * 2);
        b.order(m_byteOrder);
        for (var i = 0; i < len; i++) {
            b.putChar(s.charAt(i));
        }
        m_output.write(b.array());
    }

    @Override
    public void writeUTF(final String s) throws IOException {
        var b = StringEncoder.encode(s);
        // TODO: allow string lengths > MAX_INTEGER https://knime-com.atlassian.net/browse/AP-19712
        final int length = b.limit();
        writeLong(length);
        m_output.write(b.array(), 0, length);
    }

}
