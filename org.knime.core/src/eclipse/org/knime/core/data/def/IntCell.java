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
 *   27.02.07 (po): implements ComplexNumberValue now
 */
package org.knime.core.data.def;

import java.io.IOException;

import org.knime.core.data.BoundedValue;
import org.knime.core.data.ComplexNumberValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellFactory.FromComplexString;
import org.knime.core.data.DataCellFactory.FromSimpleString;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.FuzzyIntervalValue;
import org.knime.core.data.FuzzyNumberValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.convert.DataCellFactoryMethod;

/**
 * A data cell implementation holding an integer value by storing this value in
 * a private <code>int</code> member. It provides an int value, a double
 * value, a fuzzy number value, as well as a fuzzy interval value.
 *
 * @author Michael Berthold, University of Konstanz
 */
public class IntCell extends DataCell implements IntValue, LongValue,
    DoubleValue, ComplexNumberValue, FuzzyNumberValue,
        FuzzyIntervalValue, BoundedValue {

    /**
     * Convenience access member for
     * <code>DataType.getType(IntCell.class)</code>.
     *
     * @see DataType#getType(Class)
     */
    public static final DataType TYPE = DataType.getType(IntCell.class);

    /**
     * Returns the factory to read/write DataCells of this class from/to a
     * DataInput/DataOutput. This method is called via reflection.
     *
     * @return A serializer for reading/writing cells of this kind.
     * @see DataCell
     * @deprecated use {@link DataTypeRegistry#getSerializer(Class)} instead
     */
    @Deprecated
    public static final DataCellSerializer<IntCell> getCellSerializer() {
        return new IntCellSerializer();
    }

    private final int m_int;

    /**
     * Creates new cell for a generic int value.
     *
     * @param i The integer value to store.
     */
    public IntCell(final int i) {
        m_int = i;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIntValue() {
        return m_int;
    }

    /** {@inheritDoc} */
    @Override
    public long getLongValue() {
        return m_int;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDoubleValue() {
        return m_int;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCore() {
        return m_int;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getMaxSupport() {
        return m_int;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getMinSupport() {
        return m_int;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getMaxCore() {
        return m_int;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getMinCore() {
        return m_int;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCenterOfGravity() {
        return m_int;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getImaginaryValue() {
        return 0.0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getRealValue() {
        return m_int;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return ((IntCell)dc).m_int == m_int;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_int;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Integer.toString(m_int);
    }

    /**
     * Serializer for an {@link IntCell}.
     *
     * @noreference This class is not intended to be referenced by clients.
     * @since 3.0
     */
    public static final class IntCellSerializer implements DataCellSerializer<IntCell> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void serialize(final IntCell cell,
                final DataCellDataOutput output) throws IOException {
            output.writeInt(cell.m_int);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public IntCell deserialize(
                final DataCellDataInput input) throws IOException {
            int i = input.readInt();
            return new IntCell(i);
        }
    }


    /**
     * Factory for {@link IntCell}s.
     *
     * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
     * @since 3.0
     */
    public static final class IntCellFactory implements FromSimpleString, FromComplexString {
        /**
         * The data type for the cells created by this factory.
         */
        public static final DataType TYPE = IntCell.TYPE;

        /**
         * {@inheritDoc}
         *
         * Uses {@link Integer#parseInt(String)} to convert the string into an int.
         */
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
         * Creates a new int cell by parsing the given string with {@link Integer#parseInt(String)}.
         *
         * @param s a string
         * @return a new int cell
         * @throws NumberFormatException if the string is not a valid integer number
         */
        public static DataCell create(final String s) {
            String trimmed = s.trim();
            if (trimmed.isEmpty()) {
                return DataType.getMissingCell();
            }

            // this is a feature of the parseInt method: it bails on '+'
            if (trimmed.charAt(0) == '+') {
                trimmed = trimmed.substring(1);
                if (trimmed.isEmpty()) {
                    throw new NumberFormatException("Invalid number format, got '+' for an integer.");
                }
            }

            return new IntCell(Integer.parseInt(trimmed));
        }

        /**
         * Creates a new int cell with the given value.
         *
         * @param i any int value
         * @return a new data cell
         */
        @DataCellFactoryMethod(name = "Integer")
        public static DataCell create(final int i) {
            return new IntCell(i);
        }
    }
}
