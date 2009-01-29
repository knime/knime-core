/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   17.01.2006(sieb, ohl): reviewed 
 */
package org.knime.core.node.config;

/**
 * Config entry for integer values.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class ConfigIntEntry extends AbstractConfigEntry {
    
    /** The int value. */
    private final int m_int;

    /**
     * Creates a new Config entry for an int value.
     * @param key The key for this value.
     * @param i The int value.
     */
    ConfigIntEntry(final String key, final int i) {
        super(ConfigEntries.xint, key);
        m_int = i;
    }

    /**
     * Creates a new Config entry for an int value.
     * @param key The key for this value.
     * @param i The int value as String.
     */
    ConfigIntEntry(final String key, final String i) {
        super(ConfigEntries.xint, key);
        m_int = Integer.parseInt(i);
    }

    /**
     * @return The int value.
     */
    public int getInt() {
        return m_int;
    }
    
    /**
     * @return A String representation of this int.
     * @see Integer#toString(int)
     */
    @Override
    public String toStringValue() {
        return Integer.toString(m_int);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasIdenticalValue(final AbstractConfigEntry ace) {
        return ((ConfigIntEntry) ace).m_int == m_int;
    }

}
