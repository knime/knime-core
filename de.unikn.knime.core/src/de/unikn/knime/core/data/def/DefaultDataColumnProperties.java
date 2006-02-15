/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
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
 *   Dec 9, 2005 (wiswedel): created
 */
package de.unikn.knime.core.data.def;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import de.unikn.knime.core.data.DataColumnProperties;

/**
 * Default implementation of the <code>DataColumnProperties</code>. It is built
 * upon a given <code>Hashtable&lt;String, String&gt;</code>.
 * @author wiswedel, University of Konstanz
 */
public class DefaultDataColumnProperties 
    implements Cloneable, DataColumnProperties {
    
    private final Properties m_props;
    
    /** Creates an empty DefaultColumnProperties object. */
    public DefaultDataColumnProperties() {
        this(new Hashtable<String, String>());
    }
    
    /**
     * Creates a properties object containing the (key, value) pairs from the
     * argument. The argument must not be null (but may be empty). Any 
     * subsequent change to the argument is not reflected in this object.
     * @param content  Where to get the properties from
     * @throws NullPointerException If the argument is null.
     */
    public DefaultDataColumnProperties(
            final Hashtable<String, String> content) {
        m_props = new Properties();
        m_props.putAll(content);
    }

    /**
     * @see DataColumnProperties#containsProperty(java.lang.String)
     */
    public boolean containsProperty(final String key) {
        return m_props.containsKey(key);
    }

    /**
     * @see DataColumnProperties#getProperty(String, String)
     */
    public String getProperty(final String key, final String defaultValue) {
        return m_props.getProperty(key, defaultValue);
    }

    /**
     * @see DataColumnProperties#getProperty(String)
     */
    public String getProperty(final String key) {
        return m_props.getProperty(key);
    }
    
    /**
     * @see DataColumnProperties#size()
     */
    public int size() {
        return m_props.size();
    }
    
    /**
     * @see DataColumnProperties#properties()
     */
    @SuppressWarnings("unchecked")
    public Enumeration<String> properties() {
        return (Enumeration<String>)m_props.propertyNames();
    }
    
    /**
     * @see DataColumnProperties#cloneAndOverwrite(Hashtable)
     */
    public DataColumnProperties cloneAndOverwrite(
            final Hashtable<String, String> overwrite) {
        DefaultDataColumnProperties clone = new DefaultDataColumnProperties();
        clone.m_props.putAll(m_props);
        clone.m_props.putAll(overwrite);
        return clone;
    }
    
    /** 
     * Returns a string containing key=value pairs, separated by ", ".
     * @see java.lang.Object#toString()
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
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DefaultDataColumnProperties)) {
            return false;
        }
        DefaultDataColumnProperties other = (DefaultDataColumnProperties)obj;
        return m_props.equals(other.m_props);
    }
    
    /**
     * Hash code based on underlying <code>java.util.Properties</code> class.
     * @see Object#hashCode() 
     */
    @Override
    public int hashCode() {
        return m_props.hashCode();
    }
}
