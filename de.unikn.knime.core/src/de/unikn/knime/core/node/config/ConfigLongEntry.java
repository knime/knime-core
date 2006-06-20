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
 */
package de.unikn.knime.core.node.config;

/**
 * Config entry for <code>long</code> values.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class ConfigLongEntry extends AbstractConfigEntry {
    
    /** The long value. */
    private final long m_long;

    /**
     * Creates a new Config entry for an long value.
     * @param l The long value.
     */
    ConfigLongEntry(final long l) {
        super(ConfigEntries.xlong);
        m_long = l;
    }

    /**
     * Creates a new Config entry for a long value.
     * @param l The long value as String.
     */
    ConfigLongEntry(final String l) {
        super(ConfigEntries.xlong);
        m_long = Long.parseLong(l);
    }

    /**
     * @return The long value.
     */
    public long getLong() {
        return m_long;
    }
    
    /**
     * @return A String representation of this long.
     * @see Long#toString(long)
     */
    public String toStringValue() {
        return Long.toString(m_long);
    }
    
    /**
     * @see AbstractConfigEntry#hasIdenticalValue(AbstractConfigEntry)
     */
    @Override
    protected boolean hasIdenticalValue(final AbstractConfigEntry ace) {
        return ((ConfigLongEntry) ace).m_long == m_long;
    }

}
