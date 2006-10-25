/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 *   09.12.2005 (bw): created
 *   25.10.2006 (tg): cleanup
 */
package org.knime.core.data;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.ConfigRO;

/**
 * Property map that contains (labeled) annotations assigned to a column. This
 * class implements a slim, read only version of java's <code>Properties</code> 
 * class, whereby all related methods delegate to the underlying property
 * object. This class is used by the <code>DataColumnSpec</code>.
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
     * argument. The argument must not be <code>null</code> (but may be empty). 
     * Any subsequent change to the argument is not reflected in this object.
     * @param content  Where to get the properties from.
     * @throws NullPointerException If the argument is <code>null</code>.
     */
    public DataColumnProperties(final Hashtable<String, String> content) {
        m_props = new Properties();
        m_props.putAll(content);
    }

    /**
     * Tests if this properties object contains a given key.
     * 
     * @param key The requested key.
     * @return <code>true</code> if <code>key</code> is contained in this
     *         property object, <code>false</code> otherwise.
     * @throws  NullPointerException  if the key is <code>null</code>.
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
     * @param key Request key.
     * @param defaultValue The value to be returned if <code>key</code> is not
     *            contained in this property object.
     * @return The value to <code>key</code> or <code>defaultValue</code>.
     * @throws NullPointerException If <code>key</code> is <code>null</code>.
     * 
     * @see Properties#getProperty(String, String)
     */
    public String getProperty(final String key, final String defaultValue) {
        return m_props.getProperty(key, defaultValue);
    }

    /**
     * Get the property annotated by <code>key</code> or <code>null</code>
     * if <code>key</code> does not exist.
     * 
     * @param key Request key.
     * @return The value to which key is mapped to or <code>null</code> if
     *         <code>key</code> is not contained.
     * @throws NullPointerException If argument is <code>null</code>.
     *
     * @see Properties#getProperty(String)
     */
    public String getProperty(final String key) {
        return m_props.getProperty(key);
    }
    
    /**
     * Get the number of properties in this object.
     * 
     * @return The number of stored properties.
     *
     * @see Properties#size()
     */
    public int size() {
        return m_props.size();
    }
    
    /**
     * Get an enumeration on all keys in this property object.
     * 
     * @return An enumeration on the keys.
     *
     * @see Properties#propertyNames()
     */
    @SuppressWarnings("unchecked")
    public Enumeration<String> properties() {
        return (Enumeration<String>)m_props.propertyNames();
    }
    
    /**
     * Creates a new instance which carries all properties from this object
     * unless they are overwritten by the argument in which case the argument's
     * properties are used. This serves as a convenient way to add new
     * properties to a <code>DataColumnProperties</code> object.
     * 
     * @param overwrite The new properties to use.
     * @return A (almost) clone of this object with additional properties.
     * @throws NullPointerException If the argument is <code>null</code>.
     */
    public DataColumnProperties cloneAndOverwrite(
            final Hashtable<String, String> overwrite) {
        DataColumnProperties clone = new DataColumnProperties();
        clone.m_props.putAll(m_props);
        clone.m_props.putAll(overwrite);
        return clone;
    }
    
    /** 
     * Returns a string containing key=value pairs, separated by ", ".
     * @see Properties#toString()
     */
    @Override
    public String toString() {
        return m_props.toString();
    }
    
    /**
     * Compares a given object on equality. It will be equal if it is also
     * a <code>DataColumnProperties</code> object and contains the equal key
     * value pairs.
     * @param obj To compare to.
     * @return If the given object is equal to this property object.
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof DataColumnProperties)) {
            return false;
        }
        return m_props.equals(((DataColumnProperties) obj).m_props);
    }
    
    /**
     * Hash code based on underlying <code>java.util.Properties</code> class.
     * @see Properties#hashCode() 
     */
    @Override
    public int hashCode() {
        return m_props.hashCode();
    }
    
    /**
     * Saves all key-value pairs to the given <code>Config</code>.
     * <p>Note: This implementation adds the pairs directly to the argument,
     * make sure to provide an empty subconfig!
     * @param config Write properties into this object.
     */
    public void save(final Config config) {
        assert config.keySet().isEmpty() : "Subconfig must be empty: " 
            +  Arrays.toString(config.keySet().toArray());
        for (Map.Entry<Object, Object> p : m_props.entrySet()) {
            String key = (String)p.getKey();
            String val = (String)p.getValue();
            config.addString(key, val);
        }
    }
    
    /**
     * Reads all properties (key-value pairs) from the given <code>Config</code>
     * and return a new <code>DataColumnProperty</code> object.
     * @param config To read properties from.
     * @return A new property object.
     * @throws InvalidSettingsException If the <i>keys</i> entry is not 
     *         available or a value is not available for a given key.
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
