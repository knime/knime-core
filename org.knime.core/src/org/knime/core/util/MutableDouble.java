/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
     * {@inheritDoc}
     */
    @Override
    public int intValue() {
        return (int)m_i;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long longValue() {
        return (long)m_i;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float floatValue() {
        return (float)m_i;
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Number)) {
            return false;
        }
        return ((Number)obj).doubleValue() == m_i;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(m_i);
        return (int)(bits ^ (bits >>> 32));
    }

    /**
     * {@inheritDoc}
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
