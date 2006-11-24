/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   23.03.2006 (cebron): created
 *   21.06.06 (bw & po): reviewed
 */
package org.knime.core.data.def;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.knime.core.data.ComplexNumberValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;

/**
 * A data cell implementation holding a complex number value by storing this
 * value in two double member variables. It provides access to the complex
 * number value.
 * 
 * @author ciobaca, University of Konstanz
 */
public final class ComplexNumberCell extends DataCell implements
        ComplexNumberValue, DoubleValue {

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
     * @see org.knime.core.data.ComplexNumberValue#getRealValue()
     */
    public double getRealValue() {
        return m_real;
    }

    /**
     * @see org.knime.core.data.ComplexNumberValue#getRealValue()
     */
    public double getImaginaryValue() {
        return m_imag;
    }

    /**
     * @see org.knime.core.data.DataCell
     *      #equalsDataCell(org.knime.core.data.DataCell)
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return ((ComplexNumberCell)dc).m_real == m_real
                && ((ComplexNumberCell)dc).m_imag == m_imag;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        long bitsReal = Double.doubleToLongBits(m_real);
        long bitsImag = Double.doubleToLongBits(m_imag);
        long bits = bitsReal ^ (bitsImag >> 1);
        return (int)(bits ^ (bits >>> 32));
    }

    /**
     * @see java.lang.Object#toString()
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
     * Implements the getter method of the {@link DoubleValue} interface.
     * 
     * @return The magnitude of the complex number.
     */
    public double getDoubleValue() {
        return Math.sqrt(m_real * m_real + m_imag * m_imag);
    }

    /** 
     * Factory for (de-)serializing a {@link ComplexNumberCell}. 
     * */
    private static class ComplexNumberSerializer implements
            DataCellSerializer<ComplexNumberCell> {

        /**
         * @see DataCellSerializer#serialize(DataCell, DataOutput)
         */
        public void serialize(final ComplexNumberCell cell,
                final DataOutput output) throws IOException {
            output.writeDouble(cell.getRealValue());
            output.writeDouble(cell.getImaginaryValue());
        }

        /**
         * @see DataCellSerializer#deserialize(DataInput)
         */
        public ComplexNumberCell deserialize(final DataInput input)
                throws IOException {
            double real = input.readDouble();
            double imag = input.readDouble();
            return new ComplexNumberCell(real, imag);
        }
    }
}
