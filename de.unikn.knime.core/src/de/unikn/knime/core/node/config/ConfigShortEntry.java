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
 * Config entry for short values.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class ConfigShortEntry extends AbstractConfigEntry {
    
    /** The short value. */
    private final short m_short;
    
    /**
     * Creates a new short entry.
     * @param key The key of this entry.
     * @param s The short value.
     */
    ConfigShortEntry(final String key, final short s) {
        super(key, ConfigEntries.xshort);
        m_short = s;
    }

    /**
     * Creates a new short entry.
     * @param key The key of this entry.
     * @param s The short value as String.
     */
    ConfigShortEntry(final String key, final String s) {
        super(key, ConfigEntries.xshort);
        m_short = Short.parseShort(s);
    }
    
    /**
     * @return The short value.
     */
    public short getShort() {
        return m_short;
    }
    
    /**
     * @return A Sring representation of this short value.
     * @see de.unikn.knime.core.node.config.ConfigurableEntry#toStringValue()
     */
    public String toStringValue() {
        return Short.toString(m_short);
    }

    /**
     * @see AbstractConfigEntry#hasIdenticalValue(AbstractConfigEntry)
     */
    @Override
    protected boolean hasIdenticalValue(final AbstractConfigEntry ace) {
        return ((ConfigShortEntry) ace).m_short == m_short;
    }

}
