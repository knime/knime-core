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
 * Config entry for byte objects.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class ConfigByteEntry extends AbstractConfigEntry {
    
    /** The byte value. */
    private final byte m_byte;
    
    /**
     * Creates a new config entry for bytes.
     * @param key The key for this value.
     * @param b The byte value.
     */
    ConfigByteEntry(final String key, final byte b) {
        super(ConfigEntries.xbyte, key);
        m_byte = b;
    }

    /**
     * Creates a new config entry for bytes.
     * @param key The key for this value.
     * @param b The byte value as String.
     */
    ConfigByteEntry(final String key, final String b) {
        super(ConfigEntries.xbyte, key);
        m_byte = Byte.parseByte(b);
    }

    /**
     * @return The byte value.
     */
    public byte getByte() {
        return m_byte;
    }
    
    /**
     * @return The byte value as String representation.
     * @see Byte#toString(byte)
     */
    @Override
    public String toStringValue() {
        return Byte.toString(m_byte);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasIdenticalValue(final AbstractConfigEntry ace) {
        return ((ConfigByteEntry) ace).m_byte == m_byte;
    }

}
