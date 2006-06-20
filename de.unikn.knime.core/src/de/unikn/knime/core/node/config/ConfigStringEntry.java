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
 * Config entry for String values.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class ConfigStringEntry extends AbstractConfigEntry {

    /** The String value. */
    private final String m_string;
    
    /**
     * Creates a new String entry. "null" is interpreted as null pointer.
     * @param value The String value or null.
     */
    ConfigStringEntry(final String value) {
        super(ConfigEntries.xstring);
        m_string = (value == null ? null : value.intern());   
    }
    
    /**
     * @return The String value.
     */
    public String getString() {
        return m_string;
    }
    
    /**
     * @return The String value.
     */
    public String toStringValue() {
        return m_string;
    }
    
    /**
     * @see AbstractConfigEntry#hasIdenticalValue(AbstractConfigEntry)
     */
    @Override
    protected boolean hasIdenticalValue(final AbstractConfigEntry ace) {
        ConfigStringEntry e = (ConfigStringEntry) ace;
        if (m_string == e.m_string) {
            return true;
        }
        return (m_string != null && m_string.equals(e.m_string));
    }

}
