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
 * ------------------------------------------------------------------- * 
 */
package org.knime.core.util;

/**
 * This class is essentially a double whose value can be changed. The hash code
 * and therefore also the equals change dynamically with the value stored. Be
 * patient not to use this object as a key in a hashtable and such.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public final class MutableDouble extends Number {
    private static final long serialVersionUID = -1576835056083239940L;

    private volatile double m_i;

    /**
     * Creates a new mutable double.
     * 
     * @param i the start value
     */
    public MutableDouble(final double i) {
        m_i = i;
    }

    /**
     * @see java.lang.Number#intValue()
     */
    @Override
    public int intValue() {
        return (int)m_i;
    }

    /**
     * @see java.lang.Number#longValue()
     */
    @Override
    public long longValue() {
        return (long)m_i;
    }

    /**
     * @see java.lang.Number#floatValue()
     */
    @Override
    public float floatValue() {
        return (float)m_i;
    }

    /**
     * @see java.lang.Number#doubleValue()
     */
    @Override
    public double doubleValue() {
        return m_i;
    }

    /**
     * Sets the value of this double.
     * 
     * @param newValue the new value
     */
    public void setValue(final double newValue) {
        m_i = newValue;
    }

    /**
     * Increments this double by one.
     * 
     * @return the new value
     */
    public double inc() {
        return ++m_i;
    }

    /**
     * Decrements this double by one.
     * 
     * @return the new value
     */
    public double dec() {
        return --m_i;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Number)) {
            return false;
        }
        return ((Number)obj).doubleValue() == m_i;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(m_i);
        return (int)(bits ^ (bits >>> 32));
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return Double.toString(m_i);
    }
    
    /**
     * Adds a value to this object.
     * 
     * @param d a double value
     */
    public void add(final double d) {
        m_i += d;
    }
}
