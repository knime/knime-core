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
 *   12 Nov 2022 (chaubold): created
 */
package org.knime.core.data.v2.filestore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.Test;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.table.io.ReadableDataInputStream;

/**
 * Tests {@link UnmodifiedLongUTFDataInput} and {@link UnmodifiedLongUTFDataOutput} used to write data either to a
 * {@link FileStore} or to an Arrow binary buffer when using the columnar backend.
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("javadoc")
public class UnmodifiedLongUTFInOutTest {
    private final Random m_random = new Random();

    private static final ByteOrder[] BYTE_ORDERS = new ByteOrder[]{ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN};

    @Test
    public void testLong() throws IOException {
        long testValue = m_random.nextLong();

        test(o -> {
            try {
                o.writeLong(testValue);
            } catch (IOException e) {
                fail();
            }
        }, i -> {
            try {
                return i.readLong();
            } catch (IOException e) {
                fail();
                return 0;
            }
        }, testValue);
    }

    @Test
    public void testInt() throws IOException {
        int testValue = m_random.nextInt();

        test(o -> {
            try {
                o.writeInt(testValue);
            } catch (IOException e) {
                fail();
            }
        }, i -> {
            try {
                return i.readInt();
            } catch (IOException e) {
                fail();
                return 0;
            }
        }, testValue);
    }

    @Test
    public void testShort() throws IOException {
        short testValue = (short)m_random.nextInt();

        test(o -> {
            try {
                o.writeShort(testValue);
            } catch (IOException e) {
                fail();
            }
        }, i -> {
            try {
                return i.readShort();
            } catch (IOException e) {
                fail();
                return 0;
            }
        }, testValue);
    }

    @Test
    public void testChar() throws IOException {
        char testValue = (char)m_random.nextInt();

        test(o -> {
            try {
                o.writeChar(testValue);
            } catch (IOException e) {
                fail();
            }
        }, i -> {
            try {
                return i.readChar();
            } catch (IOException e) {
                fail();
                return 0;
            }
        }, testValue);
    }

    @Test
    public void testSimpleUTFString() throws IOException {
        String testValue = "This is a test String without special chars";

        test(o -> {
            try {
                o.writeUTF(testValue);
            } catch (IOException e) {
                fail();
            }
        }, i -> {
            try {
                return i.readUTF();
            } catch (IOException e) {
                fail();
                return "";
            }
        }, testValue);
    }

    @Test
    public void testUmlautUTFString() throws IOException {
        String testValue = "¿Für welche Späße braucht man UTF Blödsinn?";

        test(o -> {
            try {
                o.writeUTF(testValue);
            } catch (IOException e) {
                fail();
            }
        }, i -> {
            try {
                return i.readUTF();
            } catch (IOException e) {
                fail();
                return "";
            }
        }, testValue);
    }

    private static <V> void test(final Consumer<DataOutput> writer, final Function<DataInput, V> reader, final V expectedValue)
        throws IOException {
        for (var byteOrder : BYTE_ORDERS) {
            final var outStream = new ByteArrayOutputStream();
            try (var dataOut = new DataOutputStream(outStream)) {
                var wrappedStream = new UnmodifiedLongUTFDataOutput(dataOut, byteOrder);
                writer.accept(wrappedStream);
            }

            final var inStream = new ByteArrayInputStream(outStream.toByteArray());
            try (var dataIn = new ReadableDataInputStream(inStream)) {
                var wrappedStream = new UnmodifiedLongUTFReadableDataInput(dataIn, byteOrder);
                V actual = reader.apply(wrappedStream);
                assertEquals(expectedValue, actual);
            }
        }
    }

    @Test
    public void testByteOrderMixupFails() throws IOException {
        long expectedValue = m_random.nextLong();
        final var outStream = new ByteArrayOutputStream();
        try (var dataOut = new DataOutputStream(outStream)) {
            var wrappedStream = new UnmodifiedLongUTFDataOutput(dataOut, ByteOrder.LITTLE_ENDIAN);
            wrappedStream.writeLong(expectedValue);
        }

        final var inStream = new ByteArrayInputStream(outStream.toByteArray());
        try (var dataIn = new ReadableDataInputStream(inStream)) {
            var wrappedStream = new UnmodifiedLongUTFReadableDataInput(dataIn, ByteOrder.BIG_ENDIAN);
            long actual = wrappedStream.readLong();
            assertNotEquals(expectedValue, actual);
        }
    }
}
