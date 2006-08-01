/*
 * ------------------------------------------------------------------
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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   Mar 28, 2006 (meinl): created
 *   11.05.2006 (wiswedel, ohl): reviewed
 */
package org.knime.core.util;

/**
 * This class is essentially an integer whose value can be changed. The hash
 * code and therefore also the equals change dynamically with the value stored.
 * Be patient not to use this object as a key in a hashtable and such.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public final class MutableInteger extends Number {
    private static final long serialVersionUID = -1576835000083239940L;
    private volatile int m_i;
    
    /**
     * Creates a new mutable integer.
     * @param i the start value
     */
    public MutableInteger(final int i) {
        m_i = i;
    }

    /**
     * @see java.lang.Number#intValue()
     */
    @Override
    public int intValue() {
        return m_i;
    }

    /**
     * @see java.lang.Number#longValue()
     */
    @Override
    public long longValue() {
        return m_i;
    }

    /**
     * @see java.lang.Number#floatValue()
     */
    @Override
    public float floatValue() {
        return m_i;
    }

    /**
     * @see java.lang.Number#doubleValue()
     */
    @Override
    public double doubleValue() {
        return m_i;
    }

    /**
     * Sets the value of this integer.
     * @param newValue the new value
     */
    public void setValue(final int newValue) {
        m_i = newValue;
    }
    
    
    /**
     * Increments this integer by one.
     * @return the new value
     */
    public int inc() {
        return ++m_i;
    }
    
    
    /**
     * Decrements this integer by one.
     * @return the new value
     */
    public int dec() {
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
        return ((Number) obj).intValue() == m_i;
    }

    /** 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return m_i;
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return Integer.toString(m_i);
    }
}


