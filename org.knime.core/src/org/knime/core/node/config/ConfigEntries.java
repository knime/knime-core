/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
