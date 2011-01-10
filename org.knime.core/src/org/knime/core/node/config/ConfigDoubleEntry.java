/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
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
