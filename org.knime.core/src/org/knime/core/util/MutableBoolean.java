/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   23.08.2006 (ohl): created
 */
package org.knime.core.util;

/**
 * A Boolean object whose value can be changed after construction.
 * 
 * @author ohl, University of Konstanz
 */
public class MutableBoolean {

    private boolean m_value;

    /**
     * Constructor setting its initial value from the boolean value specified.
     * 
     * @param val the initial value.
     */
    public MutableBoolean(final boolean val) {
        m_value = val;
    }

    /**
     * Constructor deriving its initial value from the specified string. The
     * value of the new object is set <code>true</code>, if and only if the
     * string is not null and equals (ignoring case) "true".
     * 
     * @param s the string to be converted to a the initial value.
     */
    public MutableBoolean(final String s) {
        this(Boolean.parseBoolean(s));
    }

    /**
     * Sets the value of this MutableBoolean.
     * 
     * @param val the new value.
     */
    public void setValue(final boolean val) {
        m_value = val;
    }

    /**
     * Returns the value of this object as a boolean primitive.
     * 
     * @return the primitive <code>boolean</code> value of this object.
     */
    public boolean booleanValue() {
        return m_value;
    }

    /**
     * Returns a <tt>String</tt> representing this objects value. If this
     * object represents the value <code>true</code>, a string equal to
     * <code>"true"</code> is returned. Otherwise, a string equal to
     * <code>"false"</code> is returned.
     * 
     * @return a string representation of this object.
     */
    @Override
    public String toString() {
        return m_value ? "true" : "false";
    }

}
