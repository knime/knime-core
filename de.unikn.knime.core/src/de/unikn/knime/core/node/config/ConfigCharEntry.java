/* 
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
 *   17.01.2006(sieb, ohl): reviewed 
 */
package de.unikn.knime.core.node.config;

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
     * @param c The char value.
     */
    ConfigCharEntry(final char c) {
        super(ConfigEntries.xchar);
        m_char = c;
    }

    /**
     * Creates a new char entry.
     * @param c The char value as String.
     */
    ConfigCharEntry(final String c) {
        super(ConfigEntries.xchar);
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
    public String toStringValue() {
        return Character.toString(m_char);
    }
    
    /**
     * @see AbstractConfigEntry#hasIdenticalValue(AbstractConfigEntry)
     */
    @Override
    protected boolean hasIdenticalValue(final AbstractConfigEntry ace) {
        return ((ConfigCharEntry) ace).m_char == m_char;
    }
    
}
