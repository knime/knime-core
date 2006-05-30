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

/**
 * An enumeration holding all possible types that can be written to and read
 * from a Config. All entries have to be defined inside the corrensponding DTD. 
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
    
enum ConfigEntries {
    /**
     * Entry of type String.
     */
    xstring {
        /**
         * Returns a new entry for String objects. 
         * @param key The key for the entry.
         * @param value The String value.
         * @return A new Config entry.
         */
        ConfigStringEntry createEntry(final String key, final String value) {
            return new ConfigStringEntry(key, value);
        }
    },
    
    /**
     * Entry of type int.
     */
    xint {
        /**
         * Returns a new entry for int objects. 
         * @param key The key for the entry.
         * @param value The String value as int.
         * @return A new Config entry.
         */
        ConfigIntEntry createEntry(final String key, final String value) {
            return new ConfigIntEntry(key, value);
        }
    },
    
    /**
     * Entry of type double.
     */
    xdouble {
        /**
         * Returns a new entry for double objects. 
         * @param key The key for the entry.
         * @param value The String value as double.
         * @return A new Config entry.
         */
        ConfigDoubleEntry createEntry(final String key, final String value) {
            return new ConfigDoubleEntry(key, value);
        }
    },
    
    /**
     * Entry of type long.
     */
    xlong {
        /**
         * Returns a new entry for long objects. 
         * @param key The key for the entry.
         * @param value The String value as long.
         * @return A new Config entry.
         */
        ConfigLongEntry createEntry(final String key, final String value) {
            return new ConfigLongEntry(key, value);
        }
    },

    /**
     * Entry of type short.
     */
    xshort {
        /**
         * Returns a new entry for short objects. 
         * @param key The key for the entry.
         * @param value The String value as short.
         * @return A new Config entry.
         */
        ConfigShortEntry createEntry(final String key, final String value) {
            return new ConfigShortEntry(key, value);
        }
    },
    
    /**
     * Entry of type byte.
     */
    xbyte {
        /**
         * Returns a new entry for byte objects. 
         * @param key The key for the entry.
         * @param value The String value as byte.
         * @return A new Config entry.
         */
        ConfigByteEntry createEntry(final String key, final String value) {
            return new ConfigByteEntry(key, value);
        }
    },
    
    /**
     * Entry of type char.
     */
    xchar {
        /**
         * Returns a new entry for char objects. 
         * @param key The key for the entry.
         * @param value The String value as char.
         * @return A new Config entry.
         */
        ConfigCharEntry createEntry(final String key, final String value) {
            return new ConfigCharEntry(key, value);
        }
    },
    
    /**
     * Entry of type boolean.
     */
    xboolean {
        /**
         * Returns a new entry for boolean objects. 
         * @param key The key for the entry.
         * @param value The String value as boolean.
         * @return A new Config entry.
         */
        ConfigBooleanEntry createEntry(final String key, final String value) {
            return new ConfigBooleanEntry(key, value);
        }
    },
    
    /**
     * Entry of type sub config.
     */
    config {
        /**
         * Do not create a new entry through this object. Rather use the 
         * parent's config <code>addConfig</code> method.
         * @see Config#addConfig(String)
         * @param key The key for the entry.
         * @param value The String value as boolean.
         * @return nothing, as it throws an exception
         * @throws UnsupportedOperationException always.
         */
        Config createEntry(final String key, final String value) {
            throw new UnsupportedOperationException("Do not call this method" 
                    + " on sub config entries"); 
        }
    };

    /**
     * Creates a new Config entry from String.
     * @param key The key.
     * @param value The value.
     * @return A new Config entry.
     */
    abstract AbstractConfigEntry createEntry(
            final String key, final String value);

}
