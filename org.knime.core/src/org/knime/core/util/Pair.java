/* Created on Jun 27, 2006 3:54:35 PM by thor
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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.core.util;

/**
 * This class is a simple pair of objects.
 * 
 * @param <T> class of the first object
 * @param <M> class of the second object
 * @author Thorsten Meinl, University of Konstanz
 */
public final class Pair<T, M> {
    private final T m_first;
    private final M m_second;
    
    /**
     * Creates a new pair.
     * 
     * @param first the first object
     * @param second the second object
     */
    public Pair(final T first, final M second) {
        m_first = first;
        m_second = second;
    }

    /**
     * Returns the first object.
     * 
     * @return the first object
     */
    public T getFirst() { return m_first; }
    
    /**
     * Returns the second object.
     * 
     * @return the second object
     */
    public M getSecond() { return m_second; }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof Pair)) { return false; }
        
        Pair<?, ?> p = (Pair<?, ?>) o;
        if (!m_first.equals(p.m_first)) { return false; }
        if (!m_second.equals(p.m_second)) { return false; }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_first.hashCode() ^ (m_second.hashCode() << 2); 
    }
}
