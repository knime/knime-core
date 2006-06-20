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
 */
package de.unikn.knime.core.node.config;

import java.io.Serializable;

/**
 * Abstract Config entry holding only a Config entry type. Deriving classes must
 * store the corresponding value and implement the toStringValue() method.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
abstract class AbstractConfigEntry implements Serializable {
    
    /** The type of the stored value. */
    private final ConfigEntries m_type;
    
    /**
     * Creates a new Config entry by the given key and type.
     * @param type This Config's type.
     * @throws IllegalArgumentException If the type is null.
     */
    AbstractConfigEntry(final ConfigEntries type) {
        if (type == null) {
            throw new IllegalArgumentException(
                    "Config entry type can't be null");
        }
        m_type = type;
    }
    
    /*
     * Reviewers (PO and CS): The current key should be renamed to "identifier"
     * or something similar. A new getKey method should return a key for the
     * hashmap, which must be a combination of the type and the identifier.
     * This will resolve the problem of replacing entries with the same 
     * identifier but different keys. The equals ans hashCode methods must be 
     * adopted accordingly.
     */
    
    /**
     * @return This Config's type.
     */
    final ConfigEntries getType() {
        return m_type;
    }
    
    /**
     * A String represation including key, type and value.
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "type=" + m_type + ",value=" + toStringValue();
    }
    
    /**
     * Returns a String representation for this Config entry which is the used 
     * to re-load this Config entry.
     * @return A String representing this Config entry which can be null.
     */
    abstract String toStringValue();
    
    /**
     * Config entries are equal if the key and type match. The value will not
     * be considered.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public final boolean equals(final Object o) {
        if (o == null) {
            return false;
        }
        if (o.getClass() != this.getClass()) {
            return false;
        }
        AbstractConfigEntry e = (AbstractConfigEntry) o;
        return m_type.name() == e.m_type.name();
    }

    /**
     * Checks the identity of two config enties. They are identical if they are
     * equal and contain equal values. Equality of the values is checked by
     * <code>hasIdenticalValue</code>, implemented in the derived classes.
     * @param ace Entry to check if identical.
     * @return true, if both are equal and have identical values.
     */
    public final boolean isIdentical(final AbstractConfigEntry ace) {
        if (!this.equals(ace)) {
            return false;
        }
        // because objects are equal it is safe to typecast
        return hasIdenticalValue((AbstractConfigEntry) ace);
    }
    
    /**
     * Derived classes must compare their value with the value in the passed
     * argument (on equality). They can safely assume that the specified object
     * has the same java class, the same type and key.
     * 
     * @param ace the argument to compare the value with
     * @return true if the specified argument stores the same value as this.
     */
    abstract boolean hasIdenticalValue(AbstractConfigEntry ace);
    
    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public final int hashCode() {
        return m_type.hashCode();
    }
    
}
