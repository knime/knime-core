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
 * Config entry for short values.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class ConfigShortEntry extends AbstractConfigEntry {
    
    /** The short value. */
    private final short m_short;
    
    /**
     * Creates a new short entry.
     * @param key The key for this value.
     * @param s The short value.
     */
    ConfigShortEntry(final String key, final short s) {
        super(ConfigEntries.xshort, key);
        m_short = s;
    }

    /**
     * Creates a new short entry.
     * @param key The key for this value.
     * @param s The short value as String.
     */
    ConfigShortEntry(final String key, final String s) {
        super(ConfigEntries.xshort, key);
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
     * @see Short#toString(short)
     */
    @Override
    public String toStringValue() {
        return Short.toString(m_short);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasIdenticalValue(final AbstractConfigEntry ace) {
        return ((ConfigShortEntry) ace).m_short == m_short;
    }

}
