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
 *   09.12.2005 (bw): created
 *   25.10.2006 (tg): cleanup
 *   02.11.2006 (tm, cs): reviewed
 */
package org.knime.core.data;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

/**
 * Property map that contains annotations assigned to a column. This class
 * implements a slim, read only version of java's {@link Properties} class,
 * whereby all related methods delegate to the underlying property object. This
 * class is used by the {@link DataColumnSpec}.
 * 
 * @see java.util.Properties
 * @see org.knime.core.data.DataColumnSpec#getProperties()
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class DataColumnProperties implements Cloneable {

    /** Keeps a map of data column properties. */
    private final Properties m_props;

    /**
     * Creates an empty DataColumnProperties object.
     */
    public DataColumnProperties() {
        this(new Hashtable<String, String>());
    }

    /**
     * Creates a properties object containing the (key, value) pairs from the
     * argument. The argument must not be <code>null</code> (but may be
     * empty). Any subsequent change to the argument is not reflected in this
     * object.
     * 
     * @param content a map with key-value pairs
     * @throws NullPointerException if the argument is <code>null</code>
     */
    public DataColumnProperties(final Map<String, String> content) {
        m_props = new Properties();
        m_props.putAll(content);
    }

    /**
     * Tests if this properties object contains a given key.
     * 
     * @param key the key to check
     * @return <code>true</code> if <code>key</code> is contained in this
     *         property object, <code>false</code> otherwise
     * @throws NullPointerException if the key is <code>null</code>
     * 
     * @see java.util.Properties#containsKey(Object)
     */
    public boolean containsProperty(final String key) {
        return m_props.containsKey(key);
    }

    /**
     * Get the property assigned to <code>key</code> or - if this property
     * does not exist - the <code>defaultValue</code>.
     * 
     * @param key request-key
     * @param defaultValue the value to be returned if <code>key</code> is not
     *            contained in this property object
     * @return the property value for the given <code>key</code> or the
     *         <code>defaultValue</code>
     * @throws NullPointerException if <code>key</code> is <code>null</code>
     * 
     * @see Properties#getProperty(String, String)
     */
    public String getProperty(final String key, final String defaultValue) {
        return m_props.getProperty(key, defaultValue);
    }

    /**
     * Get the property value for the given <code>key</code> or
     * <code>null</code> if <code>key</code> does not exist.
     * 
     * @param key request key
     * @return the value which is mapped to the given key or <code>null</code>
     *         if <code>key</code> is not contained
     * @throws NullPointerException if argument is <code>null</code>
     * 
     * @see Properties#getProperty(String)
     */
    public String getProperty(final String key) {
        return m_props.getProperty(key);
    }

    /**
     * Get the number of properties in this object.
     * 
     * @return the number of stored properties
     * 
     * @see Properties#size()
     */
    public int size() {
        return m_props.size();
    }

    /**
     * Get an enumeration on all keys in this property object.
     * 
     * @return an enumeration on the keys
     * 
     * @see Properties#propertyNames()
     */
    @SuppressWarnings("unchecked")
    public Enumeration<String> properties() {
        return (Enumeration<String>)m_props.propertyNames();
    }

    /**
     * Creates a new instance which carries all properties from this object and
     * adds the <code>newProperties</code>. If there is a key conflict the
     * newProperties overwrite the old ones. This serves as a convenient way to
     * add new properties to a <code>DataColumnProperties</code> object.
     * 
     * @param newProperties the new properties to add
     * @return a (almost) clone of this object with additional properties
     * @throws NullPointerException if the argument is <code>null</code>
     */
    public DataColumnProperties cloneAndOverwrite(
            final Map<String, String> newProperties) {
        DataColumnProperties clone = new DataColumnProperties();
        clone.m_props.putAll(m_props);
        clone.m_props.putAll(newProperties);
        return clone;
    }

    /**
     * Returns a string containing key=value pairs, separated by ", ".
     * 
     * @see Properties#toString()
     */
    @Override
    public String toString() {
        return m_props.toString();
    }

    /**
     * Compares a given object on equality. It will be equal if it is also a
     * <code>DataColumnProperties</code> object and contains the equal key
     * value pairs.
     * 
     * @param obj to compare to
     * @return <code>true</code> if the given object is equal to this property
     *         object
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof DataColumnProperties)) {
            return false;
        }
        return m_props.equals(((DataColumnProperties)obj).m_props);
    }

    /**
     * Hash code based on underlying {@link Properties} class.
     * 
     * @see Properties#hashCode()
     */
    @Override
    public int hashCode() {
        return m_props.hashCode();
    }

    /**
     * Saves all key-value pairs to the given {@link ConfigWO}.
     * <p>
     * Note: This implementation adds the pairs directly to the argument, make
     * sure to provide an empty subconfig!
     * 
     * @param config write properties into this object
     */
    public void save(final ConfigWO config) {
      
        for (Map.Entry<Object, Object> p : m_props.entrySet()) {
            String key = (String)p.getKey();
            String val = (String)p.getValue();
            config.addString(key, val);
        }
    }

    /**
     * Reads all properties (key-value pairs) from the given {@link ConfigRO}
     * and returns a new <code>DataColumnProperties</code> object.
     * 
     * @param config to read properties from
     * @return a new property object
     * @throws InvalidSettingsException if the <i>keys</i> entry is not
     *             available or a value is not available for a given key
     */
    public static DataColumnProperties load(final ConfigRO config)
            throws InvalidSettingsException {
        Hashtable<String, String> table = new Hashtable<String, String>();
        for (String key : config) {
            table.put(key, config.getString(key));
        }
        return new DataColumnProperties(table);
    }
}
