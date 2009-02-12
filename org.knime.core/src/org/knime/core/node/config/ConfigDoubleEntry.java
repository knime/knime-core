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
 * Config entry for double values.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class ConfigDoubleEntry extends AbstractConfigEntry {
    
    /** The double value. */
    private final double m_double;
    
    /**
     * Creates a new Config entry for type double.
     * @param key The key for this value.
     * @param d The double value.
     */
    ConfigDoubleEntry(final String key, final double d) {
        super(ConfigEntries.xdouble, key);
        m_double = d;
    }

    /**
     * Creates a new Config entry for type double.
     * @param key The key for this value.
     * @param d The double value as String.
     */
    ConfigDoubleEntry(final String key, final String d) {
        super(ConfigEntries.xdouble, key);
        m_double = Double.parseDouble(d);
    }
    
    /**
     * @return The double value.
     */
    public double getDouble() {
        return m_double;
    }

    /**
     * @return A String representation of this double value.
     * @see Double#toString(double)
     */
    @Override
    public String toStringValue() {
        return Double.toString(m_double);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasIdenticalValue(final AbstractConfigEntry ace) {
        return 
            Double.compare(((ConfigDoubleEntry) ace).m_double, m_double) == 0;
    }

}
