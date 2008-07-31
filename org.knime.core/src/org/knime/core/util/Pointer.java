/*
 * -------------------------------------------------------------------
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
 * ------------------------------------------------------------------- * 
 */
package org.knime.core.util;

/**
 * This class is a little helper that just adds another indirection to and
 * object. This can be useful if you want to pass a value from an anonymous
 * inner class method to the enclosing outer method.
 * 
 * @author Thorsten Meinl, University of Konstanz
 * @param <T> any type
 */
public class Pointer<T> {
    private T m_p;
    
    /**
     * Returns the value of the pointer.
     * 
     * @return the pointer's value
     */
    public T get() {
        return m_p;
    }

    /**
     * Sets the pointer's value.
     * 
     * @param v the new value
     */
    public void set(final T v) {
        m_p = v;
    }
}
