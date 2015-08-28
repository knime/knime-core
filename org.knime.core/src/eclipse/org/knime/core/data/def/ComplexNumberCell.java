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
 *   23.03.2006 (cebron): created
 *   21.06.06 (bw & po): reviewed
 *   27.02.07 (po): removed DoubleValue implementation
 */
package org.knime.core.data.def;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.core.data.ComplexNumberValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellFactory.FromComplexString;
import org.knime.core.data.DataCellFactory.FromSimpleString;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;

/**
 * A data cell implementation holding a complex number value by storing this
 * value in two double member variables. It provides access to the complex
 * number value.
 *
 * @author ciobaca, University of Konstanz
 */
public final class ComplexNumberCell extends DataCell implements
        ComplexNumberValue {

    /**
     * Convenience access method for DataType.getType(ComplexNumberCell.class).
     */
    public static final DataType TYPE =
            DataType.getType(ComplexNumberCell.class);

    /**
     * Returns the factory to read/write DataCells of this class from/to a
     * DataInput/DataOutput. This method is called via reflection.
     *
     * @return A serializer for reading/writing cells of this kind.
     * @see DataCell
     * @deprecated use {@link DataTypeRegistry#getSerializer(Class)} instead
     */
    @Deprecated
    public static final ComplexNumberSerializer getCellSerializer() {
        return new ComplexNumberSerializer();
    }

    /** real part of the complex number. */
    private final double m_real;

    /** imaginary part of the complex number. */
    private final double m_imag;

    /**
     * Creates a new cell for a complex number.
     *
     * @param real The double value.
     * @param imag The imaginary value.
     */
    public ComplexNumberCell(final double real, final double imag) {
        m_real = real;
        m_imag = imag;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getRealValue() {
        return m_real;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getImaginaryValue() {
        return m_imag;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        double oreal = ((ComplexNumberCell) dc).m_real;
        double oimag = ((ComplexNumberCell)dc).m_imag;
        if (Double.isNaN(oreal) && Double.isNaN(oimag)
                && Double.isNaN(m_real) && Double.isNaN(m_imag)) {
            return true;
        }
        return (oreal == m_real && oimag == m_imag);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        long bitsReal = Double.doubleToLongBits(m_real);
        long bitsImag = Double.doubleToLongBits(m_imag);
        long bits = bitsReal ^ (bitsImag >> 1);
        return (int)(bits ^ (bits >>> 32));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String result = "" + m_real;
        if (m_imag < 0) {
            return result + " - i*" + Math.abs(m_imag);
        } else {
            return result + " + i*" + m_imag;
        }
    }

    /**
     * Factory for {@link ComplexNumberCell}s.
     *
     * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
     * @since 3.0
     */
    public static final class ComplexNumberCellFactory implements FromSimpleString, FromComplexString {
        /**
         * The data type for the cells created by this factory.
         */
        public static final DataType TYPE = ComplexNumberCell.TYPE;

        private static final Pattern SPLIT_PATTERN = Pattern.compile("^(.+?)(?:\\s*([\\+\\-])(?:\\s*i\\s*\\*(.+)|(.*)i))?$");

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
        @Override
        public DataCell createCell(final String input) {
            return create(input);
        }

        /**
         * Creates a new complex number cell by parsing the given string. The expected format is
         * <tt>real "+" img"i"</tt>, e.g <tt>3 + 2i</tt>, <tt>0.5 - 0.3i</tt>, <tt>0.1</tt>, <tt>-i</tt>.
         *
         * @param s a string denoting a complex number
         * @return a new complex number cell
         */
        public static DataCell create(final String s) {
            Matcher m = SPLIT_PATTERN.matcher(s);
            if (m.matches()) {
                double real = 0, img = 0;

                if ("i".equals(m.group(1))) {
                    img = 1;
                } else if ("-i".equals(m.group(1))) {
                    img = -1;
                } else {
                    real = Double.parseDouble(m.group(1));
                    double sign = "-".equals(m.group(2)) ? -1 : 1;
                    if (m.group(3) != null) {
                        img = sign * Double.parseDouble(m.group(3));
                    } else if (m.group(4) != null) {
                        img = sign * Double.parseDouble(m.group(4));
                    }
                }
                return new ComplexNumberCell(real, img);
            } else {
                throw new NumberFormatException("'" + s + "_ is not a valid complex number, expected something that "
                    + "matches '" + SPLIT_PATTERN.pattern() + "'");
            }
        }

        /**
         * Creates a new complex number cell from the real and imaginary parts.
         *
         * @param real the real part
         * @param img the imaginary part
         * @return a new data cell
         */
        public static DataCell create(final double real, final double img) {
            return new ComplexNumberCell(real, img);
        }
    }

    /**
     * Factory for (de-)serializing a {@link ComplexNumberCell}.
     *
     * @noreference This class is not intended to be referenced by clients.
     */
    public static final class ComplexNumberSerializer implements
            DataCellSerializer<ComplexNumberCell> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void serialize(final ComplexNumberCell cell,
                final DataCellDataOutput output) throws IOException {
            output.writeDouble(cell.getRealValue());
            output.writeDouble(cell.getImaginaryValue());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ComplexNumberCell deserialize(final DataCellDataInput input)
                throws IOException {
            double real = input.readDouble();
            double imag = input.readDouble();
            return new ComplexNumberCell(real, imag);
        }
    }
}
