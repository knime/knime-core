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
 *   07.07.2005 (mb): created
 */
package de.unikn.knime.core.data.def;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.DoubleType;
import de.unikn.knime.core.data.DoubleValue;
import de.unikn.knime.core.data.FuzzyIntervalValue;
import de.unikn.knime.core.data.FuzzyNumberValue;

/**
 * A data cell implementation holding a double value by storing this value in a
 * private <code>double</code> member. It provides a double value and a fuzzy
 * number value, as well as a fuzzy interval value.
 * 
 * @author mb, University of Konstanz
 */
public final class DefaultDoubleCell extends DataCell implements DoubleValue,
        FuzzyNumberValue, FuzzyIntervalValue {

    private final double m_double;

    /**
     * Creates a new cell for a generic double value. Also acting as
     * FuzzyNumberCell and FuzzyIntervalCell.
     * 
     * @param d The double value.
     */
    public DefaultDoubleCell(final double d) {
        m_double = d;
    }

    /**
     * @see de.unikn.knime.core.data.DataCell#getType()
     */
    public DataType getType() {
        return DoubleType.DOUBLE_TYPE;
    }

    /**
     * @see de.unikn.knime.core.data.DoubleValue#getDoubleValue()
     */
    public double getDoubleValue() {
        return m_double;
    }

    /**
     * @see de.unikn.knime.core.data.FuzzyNumberValue#getCore()
     */
    public double getCore() {
        return m_double;
    }

    /**
     * @see de.unikn.knime.core.data.FuzzyNumberValue#getMaxSupport()
     */
    public double getMaxSupport() {
        return m_double;
    }

    /**
     * @see de.unikn.knime.core.data.FuzzyNumberValue#getMinSupport()
     */
    public double getMinSupport() {
        return m_double;
    }

    /**
     * @see de.unikn.knime.core.data.FuzzyIntervalValue#getMaxCore()
     */
    public double getMaxCore() {
        return m_double;
    }

    /**
     * @see de.unikn.knime.core.data.FuzzyIntervalValue#getMinCore()
     */
    public double getMinCore() {
        return m_double;
    }

    /**
     * @see de.unikn.knime.core.data.FuzzyIntervalValue#getCenterOfGravity()
     */
    public double getCenterOfGravity() {
        return m_double;
    }

    /**
     * @see de.unikn.knime.core.data.DataCell
     *      #equalsDataCell(de.unikn.knime.core.data.DataCell)
     */
    protected boolean equalsDataCell(final DataCell dc) {
        return ((DefaultDoubleCell)dc).m_double == m_double;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        long bits = Double.doubleToLongBits(m_double);
        return (int)(bits ^ (bits >>> 32));
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "" + m_double;
    }

}
