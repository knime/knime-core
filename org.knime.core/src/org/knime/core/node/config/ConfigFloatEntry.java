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
 * Config entry for float values.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class ConfigFloatEntry extends AbstractConfigEntry {
    
    /** The float value. */
    private final float m_float;
    
    /**
     * Creates a new Config entry for type float.
     * @param key The key for this value.
     * @param f The float value.
     */
    ConfigFloatEntry(final String key, final float f) {
        super(ConfigEntries.xfloat, key);
        m_float = f;
    }

    /**
     * Creates a new Config entry for type float.
     * @param key The key for this value.
     * @param d The float value as String.
     */
    ConfigFloatEntry(final String key, final String d) {
        super(ConfigEntries.xfloat, key);
        m_float = Float.parseFloat(d);
    }
    
    /**
     * @return The float value.
     */
    public float getFloat() {
        return m_float;
    }

    /**
     * @return A String representation of this float value.
     * @see Float#toString(float)
     */
    @Override
    public String toStringValue() {
        return Double.toString(m_float);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasIdenticalValue(final AbstractConfigEntry ace) {
        return Float.compare(((ConfigFloatEntry) ace).m_float, m_float) == 0;
    }

}
