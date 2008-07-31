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
 */
package org.knime.core.node.config;

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
     * @param key The key for this value.
     * @param l The long value.
     */
    ConfigLongEntry(final String key, final long l) {
        super(ConfigEntries.xlong, key);
        m_long = l;
    }

    /**
     * Creates a new Config entry for a long value.
     * @param key The key for this value.
     * @param l The long value as String.
     */
    ConfigLongEntry(final String key, final String l) {
        super(ConfigEntries.xlong, key);
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
    @Override
    public String toStringValue() {
        return Long.toString(m_long);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasIdenticalValue(final AbstractConfigEntry ace) {
        return ((ConfigLongEntry) ace).m_long == m_long;
    }

}
