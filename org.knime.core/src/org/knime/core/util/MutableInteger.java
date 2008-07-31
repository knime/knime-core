/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
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
     * 
     * @param i the start value
     */
    public MutableInteger(final int i) {
        m_i = i;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int intValue() {
        return m_i;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long longValue() {
        return m_i;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float floatValue() {
        return m_i;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double doubleValue() {
        return m_i;
    }

    /**
     * Sets the value of this integer.
     * 
     * @param newValue the new value
     */
    public void setValue(final int newValue) {
        m_i = newValue;
    }

    /**
     * Increments this integer by one.
     * 
     * @return the new value
     */
    public int inc() {
        return ++m_i;
    }

    /**
     * Adds the given value to this mutable integer.
     * 
     * @param value the value to add
     */
    public void add(final int value) {
        m_i += value;
    }

    /**
     * Decrements this integer by one.
     * 
     * @return the new value
     */
    public int dec() {
        return --m_i;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Number)) {
            return false;
        }
        return ((Number)obj).intValue() == m_i;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_i;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Integer.toString(m_i);
    }
}
