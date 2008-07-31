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
 * -------------------------------------------------------------------
 * 
 * History
 *   17.01.2006(sieb, ohl): reviewed 
 */
package org.knime.core.node.config;

/**
 * Config entry for char objects.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class ConfigCharEntry extends AbstractConfigEntry {
    
    /** The char value. */
    private final char m_char;
    
    /**
     * Creates a new char entry.
     * @param key The key for this value.
     * @param c The char value.
     */
    ConfigCharEntry(final String key, final char c) {
        super(ConfigEntries.xchar, key);
        m_char = c;
    }

    /**
     * Creates a new char entry.
     * @param key The key for this value.
     * @param c The char value as String.
     */
    ConfigCharEntry(final String key, final String c) {
        super(ConfigEntries.xchar, key);
        if (c.length() > 1) {
            throw new IllegalArgumentException("ConfigCharEntry only takes " 
                    + "strings of length one.");
        }
        m_char = c.charAt(0);
    }
    
    /**
     * @return The char value.
     */
    public char getChar() {
        return m_char;
    }
    
    /**
     * @return A String representation of this char value.
     * @see Character#toString(char)
     */
    @Override
    public String toStringValue() {
        return Character.toString(m_char);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasIdenticalValue(final AbstractConfigEntry ace) {
        return ((ConfigCharEntry) ace).m_char == m_char;
    }
    
}
