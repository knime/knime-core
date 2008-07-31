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
 * An enumeration holding all possible types that can be written to and read
 * from a Config. All entries have to be defined inside the corresponding DTD. 
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
         * @param key The key for this value. 
         * @param value The String value.
         * @return A new Config entry.
         */
        @Override
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
         * @param key The key for this value. 
         * @param value The String value as int.
         * @return A new Config entry.
         */
        @Override
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
         * @param key The key for this value. 
         * @param value The String value as double.
         * @return A new Config entry.
         */
        @Override
        ConfigDoubleEntry createEntry(final String key, final String value) {
            return new ConfigDoubleEntry(key, value);
        }
    },
    
    /**
     * Entry of type float.
     */
    xfloat {
        /**
         * Returns a new entry for float objects.
         * @param key The key for this value. 
         * @param value The String value as float.
         * @return A new Config entry.
         */
        @Override
        ConfigFloatEntry createEntry(final String key, final String value) {
            return new ConfigFloatEntry(key, value);
        }
    },
    
    /**
     * Entry of type long.
     */
    xlong {
        /**
         * Returns a new entry for long objects.
         * @param key The key for this value. 
         * @param value The String value as long.
         * @return A new Config entry.
         */
        @Override
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
         * @param key The key for this value. 
         * @param value The String value as short.
         * @return A new Config entry.
         */
        @Override
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
         * @param key The key for this value. 
         * @param value The String value as byte.
         * @return A new Config entry.
         */
        @Override
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
         * @param key The key for this value. 
         * @param value The String value as char.
         * @return A new Config entry.
         */
        @Override
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
         * @param key The key for this value. 
         * @param value The String value as boolean.
         * @return A new Config entry.
         */
        @Override
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
         * @param key The key for this value.
         * @param value The String value as boolean.
         * @see Config#addConfig(String)
         * @return nothing, as it throws an exception
         * @throws UnsupportedOperationException always.
         */
        @Override
        Config createEntry(final String key, final String value) {
            throw new UnsupportedOperationException("Do not call this method" 
                    + " on sub config entries"); 
        }
    };

    /**
     * Creates a new Config entry from String.
     * @param key The key for this value.
     * @param value The value.
     * @return A new Config entry.
     */
    abstract AbstractConfigEntry createEntry(String key, String value);
}
