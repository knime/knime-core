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
 * Config entry for byte objects.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class ConfigByteEntry extends AbstractConfigEntry {
    
    /** The byte value. */
    private final byte m_byte;
    
    /**
     * Creates a new config entry for bytes.
     * @param b The byte value.
     */
    ConfigByteEntry(final byte b) {
        super(ConfigEntries.xbyte);
        m_byte = b;
    }

    /**
     * Creates a new config entry for bytes.
     * @param b The byte value as String.
     */
    ConfigByteEntry(final String b) {
        super(ConfigEntries.xbyte);
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
     * @see AbstractConfigEntry#hasIdenticalValue(AbstractConfigEntry)
     */
    @Override
    protected boolean hasIdenticalValue(final AbstractConfigEntry ace) {
        return ((ConfigByteEntry) ace).m_byte == m_byte;
    }

}
