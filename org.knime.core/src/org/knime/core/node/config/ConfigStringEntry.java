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
 * Config entry for String values.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class ConfigStringEntry extends AbstractConfigEntry {
    
    private static final long serialVersionUID = -1651694785295750285L;

    /** The String value. */
    private final String m_string;
    
    /**
     * Creates a new String entry. "null" is interpreted as null pointer.
     * @param key The key for this value.
     * @param value The String value or null.
     */
    ConfigStringEntry(final String key, final String value) {
        super(ConfigEntries.xstring, key);
        m_string = value; 
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
    @Override
    public String toStringValue() {
        return m_string;
    }
    
    /**
     * {@inheritDoc}
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
