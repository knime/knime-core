/* -------------------------------------------------------------------
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
package de.unikn.knime.core.data;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Property map that contains (labeled) annotations assigned to a column. This
 * interface implements a slim, read only version of java's
 * <code>Properties</code> class. This interface is used by the
 * <code>DataColumnSpec</code>.
 * 
 * @see java.util.Properties
 * @see de.unikn.knime.core.data.DataColumnSpec#getProperties()
 * @author wiswedel, University of Konstanz
 */
public interface DataColumnProperties extends Serializable {

    /**
     * Tests if this properties object contains a given key.
     * 
     * @param key The requested key.
     * @return <code>true</code> if <code>key</code> is contained in this
     *         property object, <code>false</code> otherwise.
     */
    boolean containsProperty(final String key);

    /**
     * Get the property assigned to <code>key</code> or - if this property
     * does not exist - the <code>defaultValue</code>.
     * 
     * @param key Request key.
     * @param defaultValue The value to be returned if <code>key</code> is not
     *            contained in this property object.
     * @return The value to <code>key</code> or <code>defaultValue</code>.
     * @throws NullPointerException If <code>key</code> is <code>null</code>.
     */
    String getProperty(final String key, final String defaultValue);

    /**
     * Get the property annotated by <code>key</code> or <code>null</code>
     * if <code>key</code> does not exist.
     * 
     * @param key Request key.
     * @return The value to which key is mapped to or <code>null</code> if
     *         <code>key</code> is not contained.
     * @throws NullPointerException If argument is <code>null</code>.
     */
    String getProperty(final String key);

    /**
     * Get the number of properties in this object.
     * 
     * @return The number of stored properties.
     */
    int size();

    /**
     * Get an enumeration on all keys in this property object.
     * 
     * @return An enumeration on the keys.
     */
    Enumeration<String> properties();

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
    DataColumnProperties cloneAndOverwrite(
            final Hashtable<String, String> overwrite);

}
