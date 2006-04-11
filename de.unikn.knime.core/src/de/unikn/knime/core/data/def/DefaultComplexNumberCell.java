/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   23.03.2006 (cebron): created
 */
package de.unikn.knime.core.data.def;

import de.unikn.knime.core.data.ComplexNumberType;
import de.unikn.knime.core.data.ComplexNumberValue;
import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.DoubleValue;

/**
 * A data cell implementation holding a complex number value by storing this 
 * value in two double member variables. It provides a complex number value.
 * 
 * @author ciobaca, University of Konstanz
 */
public final class DefaultComplexNumberCell extends DataCell 
                        implements ComplexNumberValue, DoubleValue {

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
    public DefaultComplexNumberCell(final double real, final double imag) {
        m_real = real;
        m_imag = imag;
    }

    /**
     * @see de.unikn.knime.core.data.DataCell#getType()
     */
    public DataType getType() {
        return ComplexNumberType.COMPLEX_NUMBER_TYPE;
    }

    /**
     * @see de.unikn.knime.core.data.ComplexNumberValue#getRealValue()
     */
    public double getRealValue() {
        return m_real;
    }

    /**
     * @see de.unikn.knime.core.data.ComplexNumberValue#getRealValue()
     */
    public double getImaginaryValue() {
        return m_imag;
    }

    /**
     * @see de.unikn.knime.core.data.DataCell
     *      #equalsDataCell(de.unikn.knime.core.data.DataCell)
     */
    protected boolean equalsDataCell(final DataCell dc) {
        return ((DefaultComplexNumberCell)dc).m_real == m_real 
               && ((DefaultComplexNumberCell)dc).m_imag == m_imag;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        // FIXME: take into account the imaginary value as well
        long bits = Double.doubleToLongBits(m_real);
        return (int)(bits ^ (bits >>> 32));
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        String result = "" + m_real;
        if (m_imag < 0) {
            return result + " - i*" + Math.abs(m_imag);
        } else {
            return result + " + i*" + m_imag; 
        }
    }

    /**
     * 
     * @return the real part of the complex number.
     */
    public double getDoubleValue() {
        return m_real;
    }
}
