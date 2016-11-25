/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   07.07.2005 (mb): created
 *   21.06.06 (bw & po): reviewed
 */
package org.knime.core.data.def;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellFactory.FromComplexString;
import org.knime.core.data.DataCellFactory.FromInputStream;
import org.knime.core.data.DataCellFactory.FromReader;
import org.knime.core.data.DataCellFactory.FromSimpleString;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.NominalValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.convert.DataCellFactoryMethod;

/**
 * A data cell implementation holding a string value by storing this value in a
 * private {@link String} member.
 *
 * @author Michael Berthold, University of Konstanz
 */
public final class StringCell extends DataCell
implements StringValue, NominalValue {

    /**
     * Convenience access member for
     * <code>DataType.getType(StringCell.class)</code>.
     *
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE = DataType.getType(StringCell.class);

    /**
     * Returns the factory to read/write DataCells of this class from/to a
     * DataInput/DataOutput. This method is called via reflection.
     *
     * @return A serializer for reading/writing cells of this kind.
     * @see DataCell
     * @deprecated use {@link DataTypeRegistry#getSerializer(Class)} instead
     */
    @Deprecated
    public static final StringSerializer getCellSerializer() {
        return new StringSerializer();
    }

    private final String m_string;

    /**
     * Creates a new String Cell based on the given String value.
     *
     * @param str The String value to store.
     * @throws NullPointerException If the given String value is
     *             <code>null</code>.
     */
    public StringCell(final String str) {
        if (str == null) {
            throw new NullPointerException("String value can't be null.");
        }
        m_string = str;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStringValue() {
        return m_string;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getStringValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return m_string.equals(((StringCell)dc).m_string);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_string.hashCode();
    }

    /**
     * Factory for (de-)serializing a {@link StringCell}s.
     *
     * @noreference This class is not intended to be referenced by clients.
     */
    public static final class StringSerializer implements
            DataCellSerializer<StringCell> {
        /**
         * {@inheritDoc}
         */
        @Override
        public void serialize(final StringCell cell,
                final DataCellDataOutput output) throws IOException {
            output.writeUTF(cell.getStringValue());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public StringCell deserialize(
                final DataCellDataInput input) throws IOException {
            String s = input.readUTF();
            return new StringCell(s);
        }

    }


    /**
     * Factory for {@link StringCell}s.
     *
     * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
     * @since 3.0
     */
    public static final class StringCellFactory
        implements FromSimpleString, FromComplexString, FromReader, FromInputStream {
        /**
         * The data type for the cells created by this factory.
         */
        public static final DataType TYPE = StringCell.TYPE;

        /**
         * {@inheritDoc}
         *
         * Uses {@link Integer#parseInt(String)} to convert the string into an int.
         */
        @DataCellFactoryMethod(name = "String")
        @Override
        public DataCell createCell(final String s) {
            return create(s);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataType getDataType() {
            return TYPE;
        }

        /**
         * {@inheritDoc}
         */
        @DataCellFactoryMethod(name = "InputStream (String)")
        @Override
        public DataCell createCell(final InputStream input) throws IOException {
            return createCell(new InputStreamReader(input, "UTF-8"));
        }

        /**
         * {@inheritDoc}
         */
        @DataCellFactoryMethod(name = "Reader (String)")
        @Override
        public DataCell createCell(final Reader input) throws IOException {
            StringBuilder buf = new StringBuilder(1024);
            char[] c = new char[1024];
            int size;
            while ((size = input.read(c)) != -1) {
                buf.append(c, 0, size);
            }

            return new StringCell(buf.toString());
        }

        /**
         * Creates a new string cell with the given value.
         *
         * @param s any string
         * @return a new data cell
         */
        public static DataCell create(final String s) {
            return new StringCell(s);
        }
    }
}
