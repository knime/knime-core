/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *   23.03.2006 (cebron): created
 *   21.06.06 (bw & po): reviewed
 *   27.02.07 (po): removed DoubleValue implementation
 */
package org.knime.core.data.def;

import java.io.IOException;

import org.knime.core.data.ComplexNumberValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;

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
     * Returns the preferred value class of this cell implementation. This
     * method is called per reflection to determine which is the preferred
     * renderer, comparator, etc.
     * 
     * @return ComplexNumberValue.class
     */
    public static final Class<? extends DataValue> getPreferredValueClass() {
        return ComplexNumberValue.class;
    }

    private static final ComplexNumberSerializer SERIALIZER =
            new ComplexNumberSerializer();

    /**
     * Returns the factory to read/write DataCells of this class from/to a
     * DataInput/DataOutput. This method is called via reflection.
     * 
     * @return A serializer for reading/writing cells of this kind.
     * @see DataCell
     */
    public static final ComplexNumberSerializer getCellSerializer() {
        return SERIALIZER;
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
    public double getRealValue() {
        return m_real;
    }

    /**
     * {@inheritDoc}
     */
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
     * Factory for (de-)serializing a {@link ComplexNumberCell}.
     */
    private static class ComplexNumberSerializer implements
            DataCellSerializer<ComplexNumberCell> {

        /**
         * {@inheritDoc}
         */
        public void serialize(final ComplexNumberCell cell,
                final DataCellDataOutput output) throws IOException {
            output.writeDouble(cell.getRealValue());
            output.writeDouble(cell.getImaginaryValue());
        }

        /**
         * {@inheritDoc}
         */
        public ComplexNumberCell deserialize(final DataCellDataInput input)
                throws IOException {
            double real = input.readDouble();
            double imag = input.readDouble();
            return new ComplexNumberCell(real, imag);
        }
    }
}
