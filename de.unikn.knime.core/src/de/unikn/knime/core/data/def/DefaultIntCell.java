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
import de.unikn.knime.core.data.DoubleValue;
import de.unikn.knime.core.data.FuzzyIntervalValue;
import de.unikn.knime.core.data.FuzzyNumberValue;
import de.unikn.knime.core.data.IntType;
import de.unikn.knime.core.data.IntValue;

/**
 * A data cell implementation holding an integer value by storing this value in
 * a private <code>int</code> member. It provides an int value, a double
 * value, a fuzzy number value, as well as a fuzzy interval value.
 * 
 * @author mb, University of Konstanz
 */
public final class DefaultIntCell extends DataCell implements IntValue,
        DoubleValue, FuzzyNumberValue, FuzzyIntervalValue {

    private final int m_int;

    /**
     * Creates new cell for a generic int value.
     * 
     * @param i The integer value to store.
     */
    public DefaultIntCell(final int i) {
        m_int = i;
    }

    /**
     * @see de.unikn.knime.core.data.DataCell#getType()
     */
    public DataType getType() {
        return IntType.INT_TYPE;
    }

    /**
     * @see de.unikn.knime.core.data.IntValue#getIntValue()
     */
    public int getIntValue() {
        return m_int;
    }

    /**
     * @see de.unikn.knime.core.data.DoubleValue#getDoubleValue()
     */
    public double getDoubleValue() {
        return (double)m_int;
    }

    /**
     * @see de.unikn.knime.core.data.FuzzyNumberValue#getCore()
     */
    public double getCore() {
        return (double)m_int;
    }

    /**
     * @see de.unikn.knime.core.data.FuzzyIntervalValue#getMaxSupport()
     */
    public double getMaxSupport() {
        return (double)m_int;
    }

    /**
     * @see de.unikn.knime.core.data.FuzzyIntervalValue#getMinSupport()
     */
    public double getMinSupport() {
        return (double)m_int;
    }

    /**
     * @see de.unikn.knime.core.data.FuzzyIntervalValue#getMaxCore()
     */
    public double getMaxCore() {
        return (double)m_int;
    }

    /**
     * @see de.unikn.knime.core.data.FuzzyIntervalValue#getMinCore()
     */
    public double getMinCore() {
        return (double)m_int;
    }

    /**
     * @see de.unikn.knime.core.data.FuzzyIntervalValue#getCenterOfGravity()
     */
    public double getCenterOfGravity() {
        return (double)m_int;
    }

    /**
     * @see de.unikn.knime.core.data.DataCell
     *      #equalsDataCell(de.unikn.knime.core.data.DataCell)
     */
    protected boolean equalsDataCell(final DataCell dc) {
        return ((DefaultIntCell)dc).m_int == m_int;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return m_int;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "" + m_int;
    }

}
