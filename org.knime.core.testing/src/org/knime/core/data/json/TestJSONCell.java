/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   9 Sept. 2014 (Gabor): created
 */
package org.knime.core.data.json;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;

/**
 * Tests the {@link JSONCell} class.
 *
 * @author Gabor Bakos
 */
public class TestJSONCell {

    /**
     * Test method for {@link org.knime.core.data.json.JSONCell#getCellSerializer()}.
     *
     * @throws IOException
     */
    @Test
    public void testGetCellSerializer() throws IOException {
        DataCellSerializer<JSONCell> serializer = JSONCell.getCellSerializer();
        for (final String input : new String[]{"[ ]", "[{\"foo\": \"bar\"},{\"foo\": \"biz\"}]", "42", /*"null",*/
        "\"\"", "true", "{}", "{\"foo\": {\"key\": 32}}", "{\"\": []}"}) {
            DataCellDataOutput output = new DataCellDataOutputImplementation(input);
            serializer.serialize((JSONCell)JSONCellFactory.create(input, false), output);
            JSONCell jsonCell = serializer.deserialize(new DataCellDataInputImplementation(input));
            assertEquals(input, norm(JSONCellFactory.create(input, false).toString()), norm(jsonCell.toString()));
            assertEquals(input, JSONCellFactory.create(input, false), jsonCell);
        }
    }

    /**
     * Nulls are not supported for deserialization either.
     *
     * @throws IOException
     */
    @Test(expected = NullPointerException.class)
    public void testNull() throws IOException {
        DataCellSerializer<JSONCell> serializer = JSONCell.getCellSerializer();
        serializer.deserialize(new DataCellDataInputImplementation("null"));
    }

    /**
     * @param input
     * @return
     */
    static String norm(final String input) {
        return input.replaceAll("\\s+", "");
    }

    /**
     * Test method for {@link org.knime.core.data.json.JSONCell#getPreferredValueClass()}.
     */
    @Test
    public void testGetPreferredValueClass() {
        assertEquals(JSONValue.class, JSONCell.getPreferredValueClass());
    }

    /**
     *
     */
    private static final class DataCellDataInputImplementation implements DataCellDataInput {
        /**
         *
         */
        private final String m_input;

        /**
         * @param input
         */
        private DataCellDataInputImplementation(final String input) {
            this.m_input = input;
        }

        @Override
        public int skipBytes(final int arg0) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int readUnsignedShort() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int readUnsignedByte() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String readUTF() throws IOException {
            return ((JSONCell)JSONCellFactory.create(m_input, false)).getStringValue();
        }

        @Override
        public short readShort() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public long readLong() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String readLine() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int readInt() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void readFully(final byte[] arg0, final int arg1, final int arg2) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void readFully(final byte[] arg0) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public float readFloat() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public double readDouble() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public char readChar() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte readByte() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean readBoolean() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public DataCell readDataCell() throws IOException {
            throw new UnsupportedOperationException();
        }
    }

    /**
     *
     */
    private static final class DataCellDataOutputImplementation implements DataCellDataOutput {
        /**
         *
         */
        private final String m_input;

        /**
         * @param input
         */
        private DataCellDataOutputImplementation(final String input) {
            this.m_input = input;
        }

        @Override
        public void writeShort(final int v) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void writeLong(final long v) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void writeInt(final int v) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void writeFloat(final float v) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void writeDouble(final double arg0) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void writeChars(final String arg0) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void writeChar(final int arg0) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void writeBytes(final String arg0) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void writeByte(final int arg0) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void writeBoolean(final boolean arg0) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void write(final byte[] arg0, final int arg1, final int arg2) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void write(final byte[] arg0) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void write(final int arg0) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void writeUTF(final String s) throws IOException {
            assertEquals(norm(m_input), norm(s));
        }

        @Override
        public void writeDataCell(final DataCell cell) throws IOException {
            throw new UnsupportedOperationException();
        }
    }

}
