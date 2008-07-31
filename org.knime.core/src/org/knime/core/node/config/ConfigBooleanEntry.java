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
 * Config entry for boolean objects.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class ConfigBooleanEntry extends AbstractConfigEntry {
   
    /** The boolean value. */
    private final boolean m_boolean;
    
    /**
     * Creates a new entry for boolean objects. 
     * @param key The key for this value.
     * @param b The boolean value.
     */
    ConfigBooleanEntry(final String key, final boolean b) {
        super(ConfigEntries.xboolean, key);
        m_boolean = b;
    }

    /**
     * Creates a new entry for boolean objects. 
     * @param key The key for this value.
     * @param b The boolean value as String.
     */
    ConfigBooleanEntry(final String key, final String b) {
        super(ConfigEntries.xboolean, key);
        m_boolean = Boolean.parseBoolean(b);
    }
    
    /**
     * @return The boolean value.
     */
    public boolean getBoolean() {
        return m_boolean;
    }
    
    /**
     * @return A String representation of this boolean.
     * @see java.lang.Boolean#toString(boolean)
     */
    @Override
    public String toStringValue() {
        return Boolean.toString(m_boolean);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasIdenticalValue(final AbstractConfigEntry ace) {
        return ((ConfigBooleanEntry)ace).m_boolean == m_boolean;
    }
 
}
