/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
 * Config entry for integer values.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class ConfigIntEntry extends AbstractConfigEntry {
    
    /** The int value. */
    private final int m_int;

    /**
     * Creates a new Config entry for an int value.
     * @param key The key for this entry.
     * @param i The int value.
     */
    ConfigIntEntry(final String key, final int i) {
        super(key, ConfigEntries.xint);
        m_int = i;
    }

    /**
     * Creates a new Config entry for an int value.
     * @param key The key for this entry.
     * @param i The int value as String.
     */
    ConfigIntEntry(final String key, final String i) {
        super(key, ConfigEntries.xint);
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
     * @see de.unikn.knime.core.node.config.ConfigurableEntry#toStringValue()
     */
    public String toStringValue() {
        return Integer.toString(m_int);
    }
    
    /**
     * @see AbstractConfigEntry#hasIdenticalValue(AbstractConfigEntry)
     */
    @Override
    protected boolean hasIdenticalValue(final AbstractConfigEntry ace) {
        return ((ConfigIntEntry) ace).m_int == m_int;
    }

}
